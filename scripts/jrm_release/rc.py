# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
"""RC phase steps (inventory.STEPS 1-16).  Each step is fn(ctx) -> optional summary str; FUNCS maps name -> fn."""
import hashlib
import json
import os
from datetime import timedelta

from . import details as details_mod
from . import diffcheck, emails, inventory, prereqs
from .ctx import StepError

FUNCS = {}


def step(fn=None, *, name=None):
    def register(f):
        FUNCS[name or f.__name__.replace("_", "-")] = f
        return f
    return register(fn) if fn else register


@step
def prerequisites(ctx):
    return prereqs.step(ctx)


def release_checks(ctx):
    """The Release checks row's checks: a replay of the preflight's repo group (branch, RC tag, pom, published key), one
    bar cell each.  A run that did not run the preflight itself (a restart at this step) runs that group now."""
    from . import preflight
    from juneau_terminal_scripts import checks as ck
    results = [r for r in ctx.preflight_results or [] if r[0] == "repo"] or preflight.run_checks(ctx, ("repo",))
    cells = []
    for _, name, res in results:
        verdict = ck.PASS if res.status == preflight.PASS else ck.Verdict(
            "fail", res.reason, ("run release.py --check for the whole table",))
        cells.append(ck.Check(name, lambda v=verdict: v))
    return cells


@step
def prechecks(ctx):
    """release_checks as a plain step: a ✔/❌ line each (the console row of release.py's registry shows a bar instead)."""
    from juneau_terminal_scripts import checks as ck
    found = release_checks(ctx)
    failed = []
    for c in found:
        v = ck.evaluate(c)
        ctx.say(f"{'✔' if v.status == 'pass' else '❌'} {c.name}", "info" if v.status == "pass" else "error")
        if v.status != "pass":
            failed.append(f"{c.name}: {v.reason} ({v.hint[0]})")
    if failed:
        raise StepError("; ".join(failed))
    return f"{len(found)} checks passed"


# Steps that release.py's registry runs as a bar of checks (name -> fn(ctx) returning the checks); the FUNCS entry
# of the same name is the plain version, for a run without a console.
CHECKS = {"prerequisites": prereqs.checks, "prechecks": release_checks}


@step
def compose_propose_email(ctx):
    to, sub, body = emails.propose(ctx.d)
    return emails.send(ctx, "propose", to, sub, body)


@step
def workspace(ctx):
    mark = len(ctx.runner.skipped)
    d, ws = ctx.d, str(ctx.ws)
    R = ctx.runner.run
    if os.path.isdir(os.path.join(ws, ".git")):
        # --progress: git reports progress only to a terminal otherwise.
        R(["git", "-C", ws, "fetch", "--progress", "--tags", "origin"], title="Fetch", mutating=True, tool="git")
        R(["git", "-C", ws, "checkout", d["branch"]], title="Checkout", mutating=True)
        R(["git", "-C", ws, "pull", "--progress", "--ff-only", "origin", d["branch"]], title="Pull", mutating=True, tool="git")
    else:
        # A fresh clone already has every tag.
        R(["git", "clone", "--progress", "--branch", d["branch"], inventory.GIT_URL, ws], title="Clone", mutating=True,
          tool="git")
    # The Apache identity that release:prepare commits and tags with.  Bookkeeping, so no board row.
    for key, cred in (("user.name", "credential.apache.availid"), ("user.email", "credential.apache.email")):
        argv = ["git", "-C", ws, "config", key, ctx.cred(cred)]
        if ctx.runner.gate(" ".join(argv)) and not ctx.runner.quiet(argv, title=key).ok:
            ctx.fail(f"could not set {key} in {ws}")
    return ctx.runner.skipped_note(mark, f"workspace on {d['branch']}")


def _bar(ctx):
    """bar="reactor" for a Maven step that runs tests, when the toolkit has the reactor bar (otherwise today's default)."""
    return "reactor" if "reactor-bar" in getattr(ctx, "features", ()) else None


def _mvn(ctx, title, *args, tty=False, env=None, capture=None, bar=None):
    r = ctx.runner.run(["mvn", "-B", *args], title=title, mutating=True, tool="maven", cwd=ctx.ws, tty=tty, env=env,
                       capture=capture, bar=bar)
    if not r.ok:
        raise StepError(f"{title} failed (exit {r.code})")
    return r


@step
def build_verify(ctx):
    mark = len(ctx.runner.skipped)
    _mvn(ctx, "Build verify", "clean", "verify", bar=_bar(ctx))
    return ctx.runner.skipped_note(mark, "build verified")


@step
def javadoc_verify(ctx):
    mark = len(ctx.runner.skipped)
    r = _mvn(ctx, "Javadoc", "javadoc:aggregate", capture=True)
    note = ctx.runner.skipped_note(mark, "javadoc built")
    if ctx.dry_run:
        return note
    warnings = [ln for ln in (r.out or "").splitlines() if "warning" in ln.lower()]
    path = ctx.run_dir / "javadoc-report.txt"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(f"Javadoc index: {_javadoc_index(ctx)}\nWarnings: {len(warnings)}\n\n" + "\n".join(warnings) + "\n",
                    encoding="utf-8")
    ctx.say(f"Javadoc report: {path.as_uri()}  (open {_javadoc_index(ctx)} to skim the site)")
    return f"{note}; report {path.as_uri()}"


def _javadoc_index(ctx):
    """Where javadoc:aggregate puts the combined site: the plugin's default, target/site/apidocs under the workspace
    (the pom sets no output directory)."""
    for sub in ("site/apidocs", "apidocs"):
        if (ctx.ws / "target" / sub).is_dir():
            return ctx.ws / "target" / sub / "index.html"
    return ctx.ws / "target" / "site" / "apidocs" / "index.html"


@step
def gpg_unlock(ctx):
    mark = len(ctx.runner.skipped)
    pw = ctx.cred("credential.gpg.passphrase", secret=True)
    key = ctx.cred("credential.gpg.keyid")
    tmp = ctx.run_dir / "gpg-prime.txt"
    if not ctx.dry_run:
        tmp.parent.mkdir(parents=True, exist_ok=True)
        tmp.write_text("prime")
    argv = ["gpg", "--batch", "--yes", "--pinentry-mode", "loopback", "--passphrase-fd", "0", "--local-user", key,
            "--armor", "--detach-sign", "--output", str(tmp) + ".asc", str(tmp)]
    if ctx.runner.gate("prime gpg-agent") and not ctx.runner.quiet(argv, title="Prime gpg-agent",
                                                                   input=(pw + "\n").encode()).ok:
        raise StepError("gpg could not sign with that key and passphrase")
    return ctx.runner.skipped_note(mark, "gpg agent primed")


@step
def deploy_snapshot(ctx):
    mark = len(ctx.runner.skipped)
    _mvn(ctx, "Deploy snapshot", "deploy", inventory.MVN_CHECKSUMS, bar=_bar(ctx))
    return ctx.runner.skipped_note(mark, "snapshot deployed")


def git_env(ctx):
    """The environment for the one child that pushes: git asks the helper, which reads the secret from here."""
    here = os.path.dirname(os.path.abspath(__file__))
    return {"GIT_ASKPASS": os.path.join(here, "askpass.py"), "GIT_TERMINAL_PROMPT": "0",
            "JRM_ASKPASS_USER": ctx.cred("credential.apache.availid"),
            "JRM_ASKPASS_PASS": ctx.cred("credential.apache.password", secret=True)}


@step
def release_prepare(ctx):
    mark = len(ctx.runner.skipped)
    pom = ctx.ws / "pom.xml"
    if pom.exists():
        cur = details_mod.pom_version(pom.read_text())
        if not (cur or "").endswith("-SNAPSHOT"):
            raise StepError(f"pom version is {cur}; release:prepare needs a -SNAPSHOT version")
    tags = ctx.runner.quiet(["git", "-C", str(ctx.ws), "tag", "--list", ctx.rc_tag], title="Tag check")
    if ctx.rc_tag in tags.out.split():
        raise StepError(f"tag {ctx.rc_tag} already exists; bump rc or run --revert")
    d = ctx.d
    _mvn(ctx, "Release prepare", "release:prepare", "-DautoVersionSubmodules=true",
         f"-DreleaseVersion={d['version']}", f"-Dtag={ctx.rc_tag}", f"-DdevelopmentVersion={d['developmentVersion']}",
         env=git_env(ctx))
    return ctx.runner.skipped_note(mark, f"prepared {ctx.rc_tag}")


def _git(ctx, *args, title):
    """A read-only git probe in the workspace.  Quiet: a release diff is hundreds of lines, and the step prints only
    what is unexpected."""
    return ctx.runner.quiet(["git", "-C", str(ctx.ws), *args], title=title)


def release_commits(ctx):
    """Check the release commit and the plugin's follow-up (its first descendant on the branch) with diffcheck."""
    tag = ctx.rc_tag
    rel = _git(ctx, "diff", "-U0", f"{tag}^", tag, title="Release diff")
    subj = _git(ctx, "log", "-1", "--format=%s", tag, title="Release subject").out.strip()
    follow = _git(ctx, "log", "--reverse", "--ancestry-path", "--format=%H", f"{tag}..HEAD", title="Follow-up commit")
    commit = follow.out.split()[0] if follow.out.split() else None
    if commit:
        nxt = _git(ctx, "diff", "-U0", f"{tag}..{commit}", title="Next-iteration diff").out
        nsubj = _git(ctx, "log", "-1", "--format=%s", commit, title="Next-iteration subject").out.strip()
    else:
        nxt = nsubj = ""
    def read(path):
        r = _git(ctx, "show", f"{tag}^:{path}", title=f"Read {path}")
        return r.out if r.ok else None
    snap = diffcheck.reactor_snapshot_poms(read, ctx.version)
    res = diffcheck.check_release(version=ctx.version, dev_version=ctx.d["developmentVersion"], rc_tag=tag,
                                  release_diff=rel.out, release_subject=subj, next_diff=nxt, next_subject=nsubj,
                                  snapshot_poms=snap, old_tag=diffcheck.scm_tag(read("pom.xml")))
    if not commit:
        res.problems.append(f"[next-iteration commit] none found after {tag} on this branch")
    res.diffs = f"## {tag}^..{tag}\n{rel.out}\n## {tag}..{commit or '(none)'}\n{nxt}\n"
    return res


@step
def release_diff_review(ctx):
    if ctx.dry_run:   # release:prepare did not run, so there is no RC tag to diff
        return "skipped (dry run): no release commit to diff"
    res = release_commits(ctx)
    path = ctx.run_dir / "release-diff.txt"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(("Problems:\n" + "\n".join(res.problems) + "\n\n" if res.problems else "No problems.\n\n") + res.diffs,
                    encoding="utf-8")
    if res.clean:
        return f"diff clean: {res.summary}"
    ctx.say("Unexpected in the release commits:\n" + "\n".join(res.problems), "warn")
    raise StepError(f"the release commits hold unexpected changes (see {path.as_uri()}).  Fix them, or undo this "
                    f"candidate with --revert and start a new rc; a plain resume repeats this check")


def _listing(ctx):
    """The staging profile's repositories; a dry run has no password, so an unauthorised listing is empty."""
    try:
        return ctx.nexus.repositories()
    except Exception as e:   # noqa: BLE001
        if ctx.dry_run and getattr(e, "status", None) in (401, 403):
            return []
        raise


def recorded_repo_ids(ctx):
    """nexusRepoId of every run under <home>/runs, or None when any run.json cannot be read (nothing may then be dropped)."""
    ids = set()
    for f in sorted((ctx.home / "runs").glob("*/run.json")):
        try:
            data = json.loads(f.read_text())
        except (OSError, ValueError) as e:
            ctx.say(f"cannot read {f} ({e}); not dropping any staging repository", "warn")
            return None
        if isinstance(data, dict) and data.get("nexusRepoId"):
            ids.add(data["nexusRepoId"])
    return ids


def drop_stale(ctx, listing):
    """Drop every open or closed staging repository that no run records as its nexusRepoId."""
    recorded = recorded_repo_ids(ctx)
    if recorded is None:
        return
    for repo in listing:
        rid = repo["repositoryId"]
        if repo.get("state") in ("open", "closed") and rid not in recorded:
            ctx.say(f"dropping stale staging repository {rid} (created {repo.get('created', '?')})")
            ctx.nexus.drop(rid)


@step
def release_perform(ctx):
    mark = len(ctx.runner.skipped)
    listing = _listing(ctx)
    drop_stale(ctx, listing)
    before = {r["repositoryId"] for r in listing}
    _mvn(ctx, "Release perform", "release:perform", inventory.MVN_CHECKSUMS, env=git_env(ctx), bar=_bar(ctx))
    if ctx.dry_run:
        ctx.rf.set("nexusRepoId", DRY_REPO)   # a stand-in, so the later rows can print what they would do
        return ctx.runner.skipped_note(mark, "performed (dry run)")
    new = [r for r in ctx.nexus.repositories() if r["repositoryId"] not in before]
    if not new:
        raise StepError("no new Nexus staging repository after release:perform")
    repo = max(new, key=lambda r: r.get("created", ""))["repositoryId"]
    ctx.rf.set("nexusRepoId", repo)
    return f"staged in {repo}"


DRY_REPO = "orgapachejuneau-DRYRUN"


@step
def nexus_close(ctx):
    mark = len(ctx.runner.skipped)
    rid = ctx.rf.get("nexusRepoId")
    if not rid:
        raise StepError("no Nexus staging repository recorded; run release-perform first")
    ctx.nexus.close(rid)
    if ctx.dry_run:
        return ctx.runner.skipped_note(mark, f"would close {rid}")
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
        for suffix in ("", ".asc", ".sha512"):  # NOSONAR python:S1192 - file suffixes read more clearly inline
            out[old + suffix] = new + suffix
    return out


@step
def dist_stage(ctx):  # NOSONAR python:S3776 - one linear staging sequence; splitting it would scatter the steps
    mark = len(ctx.runner.skipped)
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
        for done, (old, new) in enumerate(want.items(), 1):
            ctx.nexus.download(available[old], out / new)       # explicit names; sha1/md5 never fetched
            if "count" in getattr(ctx, "features", ()):
                ctx.run.count(done, len(want))
    line = ".".join(v.split(".")[:2]) + "."
    ops = []
    for kind in ("source", "binaries"):
        for entry in ctx.svn.ls(f"{inventory.DIST_DEV}/{kind}/", quiet=True) or []:
            name = entry.rstrip("/")
            if name.startswith(f"juneau-{line}") and not name.startswith("."):
                ctx.say(f"remove old {kind}/{name}")
                ops.append(("rm", f"{inventory.DIST_DEV}/{kind}/{name}"))
    ops += [("mkdir", dev_dir(ctx, "source")), ("mkdir", dev_dir(ctx, "binaries"))]
    for new in want.values():
        kind = "source" if "-src." in new else "binaries"
        ops.append(("put", str(out / new), f"{dev_dir(ctx, kind)}/{new}"))
    ctx.svn.mucc(ctx.rc_tag, ops, title="Stage on dist/dev")
    return ctx.runner.skipped_note(mark, f"staged {ctx.rc_tag}")


def _digest_recorded(text):
    """The hex digest in a .sha512 file, whether it holds just the digest or 'digest  name' or a path then lines."""
    lines = text.splitlines()
    if len(lines) > 1:
        return "".join(lines[1:])
    return lines[0].split()[0] if lines and lines[0].split() else ""


@step
def dist_verify(ctx):  # NOSONAR python:S3776 - one check per staged file; splitting it would scatter the checks
    if ctx.dry_run:   # dist_stage committed nothing, so there is nothing on dist/dev to list or check
        return "skipped (dry run): nothing was staged to verify"
    out, v = ctx.run_dir / "dist", ctx.version
    for kind, name in (("source", f"apache-juneau-{v}-src.zip"), ("binaries", f"apache-juneau-{v}-bin.zip")):
        listing = [e.rstrip("/") for e in ctx.svn.ls(dev_dir(ctx, kind) + "/") or []]
        for suffix in ("", ".asc", ".sha512"):
            if (name + suffix) not in listing:
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
    done = emails.acknowledge(ctx, "vote")
    if done:
        return done
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
    facts = {"src_sha": sha(f"apache-juneau-{v}-src.zip"), "bin_sha": sha(f"apache-juneau-{v}-bin.zip"), "commit": commit,
             "repo": ctx.rf.get("nexusRepoId") or "REPO_NOT_FOUND",
             "deadline": deadline.astimezone().strftime("%d-%b-%Y %-I:%M%p %Z")}
    to, sub, body = emails.vote(ctx.d, facts)
    return emails.send(ctx, "vote", to, sub, body)
