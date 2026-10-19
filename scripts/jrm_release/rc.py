"""RC phase steps (spec 6.3 rows 1-15).  Each step is fn(ctx) -> optional summary str; FUNCS maps name -> fn."""
import hashlib
import os
from datetime import timedelta

from . import details as details_mod
from . import emails, gpgkeys, inventory, prereqs
from .ctx import LABELS, StepError

FUNCS = {}


def step(fn=None, *, name=None):
    def register(f):
        FUNCS[name or f.__name__.replace("_", "-")] = f
        return f
    return register(fn) if fn else register


@step
def prerequisites(ctx):
    return prereqs.step(ctx)


def ensure_keyid(ctx):
    """The GPG key id: offered from the keyring (read-only listing) when exactly one or several usable keys match
    the Apache address, else the plain prompt."""
    key = "credential.gpg.keyid"
    if key in ctx.creds:
        return ctx.creds[key]
    email = ctx.creds.get("credential.apache.email") or (
        f"{ctx.creds['credential.apache.availid']}@apache.org" if ctx.creds.get("credential.apache.availid") else None)
    keys = []
    if email:
        r = ctx.runner.run(["gpg", "--list-secret-keys", "--with-colons", "--keyid-format", "long"],
                           title="List GPG keys", mutating=False)
        keys = gpgkeys.candidates(gpgkeys.parse(r.out), email, ctx.now().timestamp()) if r.ok else []
    label = LABELS[key]
    if len(keys) > 1:
        options = [(k.keyid, f"{k.keyid}  {k.uids[0]}") for k in keys]
        value = ctx.prompt.choice(label, options, default=keys[0].keyid, key=key)
    else:
        value = ctx.prompt.text(label, default=keys[0].keyid if keys else None, key=key)
    if value is None:
        raise KeyboardInterrupt
    ctx.creds[key] = value
    _warn_unpublished(ctx, next((k for k in keys if k.keyid == value), None))
    return value


def _warn_unpublished(ctx, chosen):
    """Warn (never fail) when the chosen key is not in the published KEYS file.  Network, so never in a dry run."""
    if chosen is None or ctx.dry_run:
        return
    text = ctx.http_text(inventory.DOWNLOADS + "/KEYS")
    if text is not None and not gpgkeys.in_keys_file(text, chosen):
        ctx.say(f"⚠ key {chosen.keyid} is not in {inventory.DOWNLOADS}/KEYS; add it before the vote", "warn")


@step
def prechecks(ctx):
    cur = ctx.d
    ctx.cred("credential.apache.availid")
    ctx.cred("credential.apache.email")
    ensure_keyid(ctx)
    tag = ctx.rc_tag

    def remote(ref):
        return ctx.runner.run(["git", "ls-remote", inventory.GIT_URL, ref], title=f"ls-remote {ref}", mutating=False)

    checks = [
        ("branch exists", lambda c: (bool(remote(f"refs/heads/{cur['branch']}").out.strip()),
                                     f"branch {cur['branch']} not on gitbox", "check the branch name")),
        ("rc tag absent", lambda c: (not remote(f"refs/tags/{tag}").out.strip(),
                                     f"tag {tag} already exists", "bump rc, or run --revert")),
    ]
    failed = []
    for name, fn in checks:
        ok, reason, hint = fn(ctx)
        ctx.say(f"{'✔' if ok else '❌'} {name}", "info" if ok else "error")
        if not ok:
            failed.append(f"{name}: {reason} ({hint})")
    if failed:
        raise StepError("; ".join(failed))
    return f"{len(checks)} checks passed"


@step
def compose_propose_email(ctx):
    to, sub, body = emails.propose(ctx.d)
    return emails.send(ctx, "propose", to, sub, body)


@step
def workspace(ctx):
    d, ws = ctx.d, str(ctx.ws)
    R = ctx.runner.run
    if os.path.isdir(os.path.join(ws, ".git")):
        R(["git", "-C", ws, "fetch", "--tags", "origin"], title="Fetch", mutating=True)
        R(["git", "-C", ws, "checkout", d["branch"]], title="Checkout", mutating=True)
        R(["git", "-C", ws, "pull", "--ff-only", "origin", d["branch"]], title="Pull", mutating=True)
    else:
        R(["git", "clone", "--branch", d["branch"], inventory.GIT_URL, ws], title="Clone", mutating=True)
        R(["git", "-C", ws, "fetch", "--tags", "origin"], title="Fetch tags", mutating=True)
    R(["git", "-C", ws, "config", "user.name", ctx.cred("credential.apache.availid")], title="user.name",
      mutating=True)
    R(["git", "-C", ws, "config", "user.email", ctx.cred("credential.apache.email")], title="user.email",
      mutating=True)
    return f"workspace on {d['branch']}"


def _mvn(ctx, title, *args, tty=False, env=None):
    r = ctx.runner.run(["mvn", "-B", *args], title=title, mutating=True, tool="maven", cwd=ctx.ws, tty=tty, env=env)
    if not r.ok:
        raise StepError(f"{title} failed (exit {r.code})")
    return r


@step
def build_verify(ctx):
    _mvn(ctx, "Build verify", "clean", "verify")
    return "build verified"


@step
def javadoc_verify(ctx):
    _mvn(ctx, "Javadoc", "javadoc:aggregate")
    if ctx.prompt.confirm("Does the javadoc output look clean?", key="review.javadoc") is not True:
        raise StepError("javadoc not accepted")
    return "javadoc accepted"


ZIPS = (("juneau-examples/juneau-examples-core", "juneau-examples-core"),
        ("juneau-examples/juneau-examples-mcp", "juneau-examples-mcp"),
        ("juneau-petstore/juneau-petstore-jetty", "juneau-petstore-jetty"),
        ("juneau-sc/juneau-sc-server", "juneau-sc-server"))


@step(name="test-workspace-verify")
def verify_test_workspaces(ctx):
    snap = f"{ctx.version}-SNAPSHOT"
    made = 0
    for module, art in ZIPS:
        zip_path = ctx.ws / module / "target" / f"{art}-{snap}-bin.zip"
        if not ctx.dry_run and not zip_path.exists():
            ctx.say(f"skip {art}: {zip_path.name} not built on this branch")
            continue
        ctx.runner.run(["unzip", "-o", str(zip_path), "-d", str(ctx.ws / "target" / "workspace" / art)],
                       title=f"Unzip {art}", mutating=True)
        made += 1
    if not made and not ctx.dry_run:
        raise StepError("no workspace zips were built; run build-verify first")
    if ctx.prompt.confirm("Do the workspaces import cleanly into Eclipse?", key="review.workspaces") is not True:
        raise StepError("workspaces not accepted")
    return f"{made} workspaces checked"


@step
def gpg_unlock(ctx):
    pw = ctx.cred("credential.gpg.passphrase", secret=True)
    key = ctx.cred("credential.gpg.keyid")
    tmp = ctx.run_dir / "gpg-prime.txt"
    if not ctx.dry_run:
        tmp.parent.mkdir(parents=True, exist_ok=True)
        tmp.write_text("prime")
    r = ctx.runner.run(["gpg", "--batch", "--yes", "--pinentry-mode", "loopback", "--passphrase-fd", "0",
                        "--local-user", key, "--armor", "--detach-sign", "--output", str(tmp) + ".asc", str(tmp)],
                       title="Prime gpg-agent", mutating=True, tool="gpg", input=(pw + "\n").encode())
    if not r.ok:
        raise StepError("gpg could not sign with that key and passphrase")
    return "gpg agent primed"


@step
def deploy_snapshot(ctx):
    _mvn(ctx, "Deploy snapshot", "deploy", inventory.MVN_CHECKSUMS)
    return "snapshot deployed"


def git_env(ctx):
    """The environment for the one child that pushes: git asks the helper, which reads the secret from here."""
    here = os.path.dirname(os.path.abspath(__file__))
    return {"GIT_ASKPASS": os.path.join(here, "askpass.py"), "GIT_TERMINAL_PROMPT": "0",
            "JRM_ASKPASS_USER": ctx.cred("credential.apache.availid"),
            "JRM_ASKPASS_PASS": ctx.cred("credential.apache.password", secret=True)}


@step
def release_prepare(ctx):
    pom = ctx.ws / "pom.xml"
    if pom.exists():
        cur = details_mod.pom_version(pom.read_text())
        if not (cur or "").endswith("-SNAPSHOT"):
            raise StepError(f"pom version is {cur}; release:prepare needs a -SNAPSHOT version")
    tags = ctx.runner.run(["git", "-C", str(ctx.ws), "tag", "--list", ctx.rc_tag], title="Tag check", mutating=False)
    if ctx.rc_tag in tags.out.split():
        raise StepError(f"tag {ctx.rc_tag} already exists; bump rc or run --revert")
    d = ctx.d
    _mvn(ctx, "Release prepare", "release:prepare", "-DautoVersionSubmodules=true",
         f"-DreleaseVersion={d['version']}", f"-Dtag={ctx.rc_tag}", f"-DdevelopmentVersion={d['developmentVersion']}",
         env=git_env(ctx))
    return f"prepared {ctx.rc_tag}"


@step
def release_diff_review(ctx):
    r = ctx.runner.run(["git", "-C", str(ctx.ws), "diff", f"{ctx.rc_tag}^", ctx.rc_tag], title="Release diff",
                       mutating=False)
    ctx.say(r.out)
    if ctx.prompt.confirm("Does the release diff look right?", key="review.diff") is not True:
        raise StepError("release diff not accepted")
    return "diff accepted"


def _listing(ctx):
    """The staging profile's repositories; a dry run has no password, so an unauthorised listing is empty."""
    try:
        return ctx.nexus.repositories()
    except Exception as e:   # noqa: BLE001
        if ctx.dry_run and getattr(e, "status", None) in (401, 403):
            return []
        raise


@step
def release_perform(ctx):
    listing = _listing(ctx)
    for repo in listing:
        if repo.get("state") in ("open", "closed"):
            q = f"Stale staging repository {repo['repositoryId']} ({repo.get('created', '?')}) is still there.  Drop it?"
            if ctx.prompt.confirm(q, key="confirm.drop-stale") is True:
                ctx.nexus.drop(repo["repositoryId"])
    before = {r["repositoryId"] for r in listing}
    _mvn(ctx, "Release perform", "release:perform", inventory.MVN_CHECKSUMS, env=git_env(ctx))
    if ctx.dry_run:
        ctx.rf.set("nexusRepoId", DRY_REPO)   # a stand-in, so the later rows can print what they would do
        return "performed (dry run)"
    new = [r for r in ctx.nexus.repositories() if r["repositoryId"] not in before]
    if not new:
        raise StepError("no new Nexus staging repository after release:perform")
    repo = max(new, key=lambda r: r.get("created", ""))["repositoryId"]
    ctx.rf.set("nexusRepoId", repo)
    return f"staged in {repo}"


DRY_REPO = "orgapachejuneau-DRYRUN"


@step
def nexus_close(ctx):
    rid = ctx.rf.get("nexusRepoId")
    if not rid:
        raise StepError("no Nexus staging repository recorded; run release-perform first")
    ctx.nexus.close(rid)
    if ctx.dry_run:
        return f"would close {rid}"
    for _ in range(60):
        r = ctx.nexus.repo(rid)
        if r["state"] == "closed":
            return f"{rid} closed"
        if r.get("notifications"):
            raise StepError("close failed: " + "; ".join(r["notifications"]))
        ctx.sleep(5)
    raise StepError(f"{rid} still not closed after 5 minutes")


def dev_dir(ctx, kind):
    return f"{inventory.DIST_DEV}/{kind}/{ctx.rc_tag}"


def renamed(v):
    return {f"juneau-{v}-source-release.zip": f"apache-juneau-{v}-src.zip",
            f"juneau-distrib-{v}-bin.zip": f"apache-juneau-{v}-bin.zip"}


def wanted_files(v):
    """{Nexus file name: dist file name} for the two archives and their .asc / .sha512."""
    out = {}
    for old, new in renamed(v).items():
        for suffix in ("", ".asc", ".sha512"):
            out[old + suffix] = new + suffix
    return out


@step
def dist_stage(ctx):
    v, rid = ctx.version, ctx.rf.get("nexusRepoId")
    want = wanted_files(v)
    out = ctx.run_dir / "dist"
    if ctx.dry_run and rid == DRY_REPO:   # nothing was staged for real, so there is nothing to download
        ctx.say("dry run: skipping the staging-repository listing and download")
    else:
        available = ctx.nexus.walk(rid)
        missing = [n for n in want if n not in available]
        if missing:
            raise StepError("staging repository lacks: " + ", ".join(missing))
        out.mkdir(parents=True, exist_ok=True)
        for old, new in want.items():
            ctx.nexus.download(available[old], out / new)       # explicit names; sha1/md5 never fetched
    line = ".".join(v.split(".")[:2]) + "."
    ops = []
    for kind in ("source", "binaries"):
        for entry in ctx.svn.ls(f"{inventory.DIST_DEV}/{kind}/") or []:
            name = entry.rstrip("/")
            if name.startswith(f"juneau-{line}") and not name.startswith("."):
                ctx.say(f"remove old {kind}/{name}")
                ops.append(("rm", f"{inventory.DIST_DEV}/{kind}/{name}"))
    ops += [("mkdir", dev_dir(ctx, "source")), ("mkdir", dev_dir(ctx, "binaries"))]
    for new in want.values():
        kind = "source" if "-src." in new else "binaries"
        ops.append(("put", str(out / new), f"{dev_dir(ctx, kind)}/{new}"))
    ctx.svn.mucc(ctx.rc_tag, ops, title="Stage on dist/dev")
    return f"staged {ctx.rc_tag}"


def _digest_recorded(text):
    """The hex digest in a .sha512 file, whether it holds just the digest or 'digest  name' or a path then lines."""
    lines = text.splitlines()
    return "".join(lines[1:]) if len(lines) > 1 else (lines[0].split()[0] if lines and lines[0].split() else "")


@step
def dist_verify(ctx):
    out, v = ctx.run_dir / "dist", ctx.version
    for kind, name in (("source", f"apache-juneau-{v}-src.zip"), ("binaries", f"apache-juneau-{v}-bin.zip")):
        listing = [e.rstrip("/") for e in ctx.svn.ls(dev_dir(ctx, kind) + "/") or []]
        for suffix in ("", ".asc", ".sha512"):
            if (name + suffix) not in listing and not ctx.dry_run:
                raise StepError(f"dist/dev {kind} lacks {name + suffix}")
        local = out / name
        if local.exists():
            digest = hashlib.sha512(local.read_bytes()).hexdigest()
            if digest != _digest_recorded((out / (name + ".sha512")).read_text()):
                raise StepError(f"{name}: SHA-512 does not match its .sha512 file")
            r = ctx.runner.run(["gpg", "--batch", "--verify", str(out / (name + ".asc")), str(local)],
                               title=f"Verify {name} signature", mutating=False, tool="gpg")
            if not r.ok:
                raise StepError(f"{name}: signature does not verify")
    return "dist/dev verified"


@step
def compose_vote_email(ctx):
    v = ctx.version
    out = ctx.run_dir / "dist"

    def sha(name):
        f = out / (name + ".sha512")
        return (_digest_recorded(f.read_text()) if f.exists() else "") or "SHA512_NOT_FOUND"
    r = ctx.runner.run(["git", "-C", str(ctx.ws), "rev-parse", f"{ctx.rc_tag}^{{commit}}"], title="RC commit",
                       mutating=False)
    commit = r.out.strip() or "COMMIT_HASH_NOT_FOUND"
    deadline = ctx.now() + timedelta(hours=72)
    ctx.rf.update({"rcCommit": commit, "voteDeadline": deadline.isoformat()})
    facts = dict(src_sha=sha(f"apache-juneau-{v}-src.zip"), bin_sha=sha(f"apache-juneau-{v}-bin.zip"), commit=commit,
                 repo=ctx.rf.get("nexusRepoId") or "REPO_NOT_FOUND",
                 deadline=deadline.astimezone().strftime("%d-%b-%Y %-I:%M%p %Z"))
    to, sub, body = emails.vote(ctx.d, facts)
    return emails.send(ctx, "vote", to, sub, body)
