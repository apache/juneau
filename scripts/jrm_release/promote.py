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
"""Promote phase (spec 6.3 rows 19-25).  Irreversible from nexus-release on."""
from . import emails, inventory, runfile
from .ctx import StepError
from .rc import dev_dir, git_env, wanted_files

MIRROR_POLLS = 72   # 25 s each: 30 minutes
MIRROR_POLL_SECONDS = 25
FUNCS = {}


def step(fn):
    FUNCS[fn.__name__.replace("_", "-")] = fn
    return fn


@step
def nexus_release(ctx):
    rid = ctx.rf.get("nexusRepoId")
    if (ctx.rf.get("vote") or {}).get("outcome") != "passed":
        raise StepError("the vote has not passed")
    ctx.nexus.promote(rid)
    ctx.rf.set("phase", "promote")
    return f"{rid} released"


@step
def dist_promote(ctx):
    v = ctx.version
    tags = ctx.runner.run(["git", "-C", str(ctx.ws), "tag", "--list", "juneau-*"], title="Tags",
                          mutating=False).out.split()
    prior = inventory.prior_on_line(tags, v)
    have = [e.rstrip("/") for e in ctx.svn.ls(inventory.DIST_RELEASE + "/") or []]
    ops = []
    if prior and prior in have:
        ops.append(("rm", f"{inventory.DIST_RELEASE}/{prior}"))
    ops.append(("mkdir", f"{inventory.DIST_RELEASE}/{v}"))
    for new in wanted_files(v).values():
        kind = "source" if "-src." in new else "binaries"
        ops.append(("cp", "HEAD", f"{dev_dir(ctx, kind)}/{new}", f"{inventory.DIST_RELEASE}/{v}/{new}"))
    ctx.svn.mucc(f"Release Apache Juneau {v}", ops, title="Promote on dist/release")
    return f"promoted {v}" + (f", removed {prior}" if prior and prior in have else "")


@step
def final_tag(ctx):
    tag, ws = f"juneau-{ctx.version}", str(ctx.ws)
    present = ctx.runner.run(["git", "-C", ws, "ls-remote", "--tags", "origin", f"refs/tags/{tag}"],
                             title="Tag check", mutating=False)
    if present.out.strip():
        return f"{tag} already exists; skipped"
    ctx.runner.run(["git", "-C", ws, "tag", "-a", tag, ctx.rf.get("rcCommit") or "RC_COMMIT", "-m",
                    f"Apache Juneau {ctx.version}"], title="Tag", mutating=True)
    r = ctx.runner.run(["git", "-C", ws, "push", "origin", tag], title="Push tag", mutating=True,
                       env=git_env(ctx), tty=False)
    if not r.ok:
        raise StepError("pushing the final tag failed")
    return f"tagged {tag} on the voted RC commit"


def _release_tags(ctx):
    """Non-RC juneau-x.y.z tags in the workspace, as version tuples."""
    out = ctx.runner.run(["git", "-C", str(ctx.ws), "tag", "--list", "juneau-*"], title="Tags", mutating=False).out
    versions = []
    for t in out.split():
        parsed = inventory.parse_version(t.removeprefix("juneau-"))
        if parsed and "-" not in t.removeprefix("juneau-"):
            versions.append(parsed)
    return versions


@step
def github_release(ctx):
    v, tag = ctx.version, f"juneau-{ctx.version}"
    argv = ["gh", "release", "create", tag, "--repo", inventory.GH_SLUG, "--verify-tag", "--title",
            f"Apache Juneau {v}", "--notes", ctx.d["summary"]]
    if any(t > inventory.parse_version(v) for t in _release_tags(ctx)):
        argv.append("--latest=false")    # a patch on an older line must not become "Latest"
    r = ctx.runner.run(argv, title="GitHub release", mutating=True, tool="github", capture=True,
                       env={"GH_TOKEN": ctx.cred("credential.github.token", secret=True)})
    if not r.ok:
        raise StepError("gh release create failed")
    lines = (r.out or "").strip().splitlines()
    url = lines[-1].strip() if lines and lines[-1].strip().startswith("http") else None
    url = url or f"https://github.com/{inventory.GH_SLUG}/releases/tag/{tag}"
    ctx.rf.set("githubReleaseUrl", url)
    return url


@step
def compose_announce_email(ctx):
    url = f"{inventory.DOWNLOADS}/{ctx.version}/"
    polls = 0
    while not ctx.http_ok(url):
        if ctx.dry_run:
            ctx.say(f"dry run: {url} is not published; the real run waits for it")
            break
        if polls == MIRROR_POLLS:
            raise ctx.pause(f"The release is not on the mirrors yet ({url}).  Run: {ctx.resume_hint} later.", done=False)
        polls += 1
        left = (MIRROR_POLLS - polls + 1) * MIRROR_POLL_SECONDS
        ctx.note(f"waiting for downloads.apache.org ({polls}/{MIRROR_POLLS}, ~{left // 60:02d}:{left % 60:02d} left)")
        ctx.sleep(MIRROR_POLL_SECONDS)
    to, sub, body = emails.announce(ctx.d, ctx.rf.get("githubReleaseUrl"))
    return emails.send(ctx, "announce", to, sub, body, cc=(inventory.DEV_LIST,))


@step
def reporter_release(ctx):
    """reporter.apache.org/addrelease.html sits behind ASF committer basic auth and has no documented API, so this is a
    pausing step (like the email steps): open the page, show the values to enter, acknowledge on --resume."""
    done = emails.acknowledge(ctx, "reporter")
    if done and not ctx.dry_run:
        return "release entered on reporter.apache.org"
    date = ctx.now().date().isoformat() if ctx.now else "today"
    text = f"Add release {ctx.version} (date {date}) at {inventory.REPORTER_ADD}"
    if ctx.dry_run:
        ctx.say(f"(dry run: nothing is opened) {text}")
        return "skipped (dry run): Reporter pause not entered"
    message = f"{text}, then run: {ctx.resume_hint}"
    ctx.rf.update({"emailPending": "reporter", "pause": {"step": "reporter-release", "message": message,
                                                         "at": runfile.stamp()}})
    ctx.say(message)
    try:
        ctx.opener(inventory.REPORTER_ADD)
    except Exception as e:   # noqa: BLE001 - opening is a convenience; it must never change the step
        ctx.say(f"could not open {inventory.REPORTER_ADD}: {e}", "warn")
    raise ctx.pause(message, done=False)


def old_release_paths(ctx):
    """(paths to remove, paths to verify on the archive).  Paths under dist/release/juneau to remove (relative, no trailing slash).  Keeps KEYS and the new version, in the
    top-level <version>/ layout and in the older binaries/juneau-x/ + source/juneau-x/ one.  A binaries/ or source/
    that would end up empty is removed itself."""
    v, root = ctx.version, inventory.DIST_RELEASE
    top = ctx.svn.ls(root + "/")
    if top is None:
        raise StepError(f"cannot list {root}")
    paths, verify = [], []
    for e in top:
        name = e.rstrip("/")
        if name in ("KEYS", v, "binaries", "source"):
            continue
        paths.append(name)
        verify.append(name)
    for sub in ("binaries", "source"):
        if not any(e.rstrip("/") == sub for e in top):
            continue
        inner = ctx.svn.ls(f"{root}/{sub}/")
        if inner is None:
            raise StepError(f"cannot list {root}/{sub}")
        names = [e.rstrip("/") for e in inner]
        old = [f"{sub}/{n}" for n in names if n != f"juneau-{v}"]
        verify += old
        paths += old if len(old) < len(names) else [sub]
    return paths, verify


@step
def dist_cleanup(ctx):
    """Policy: only the current release stays on dist/release; archive.apache.org keeps the rest.  Nothing is removed
    unless every candidate is on the archive.  Not undone by --revert."""
    paths, verify = old_release_paths(ctx)
    if not paths:
        return f"nothing to remove; only {ctx.version} and KEYS are on dist/release"
    # an emptied binaries/ or source/ has no archive copy of its own; its children are what get verified
    missing = []
    for p in verify:
        if not ctx.http_ok(f"{inventory.ARCHIVE}/{p}/"):
            missing.append(p)
    if missing:
        raise StepError("not on archive.apache.org, nothing removed: " + ", ".join(missing))
    ctx.say("Old releases to remove from dist/release:\n- " + "\n- ".join(paths))
    if ctx.dry_run:
        return f"dry run: would remove {len(paths)} old entries (kept {ctx.version})"
    ctx.svn.mucc(f"Remove old Apache Juneau releases; kept {ctx.version}",
                 [("rm", f"{inventory.DIST_RELEASE}/{p}") for p in paths], title="Remove old releases")
    return f"removed {len(paths)} old entries from dist/release; kept {ctx.version}"


@step
def followup_checklist(ctx):
    items = ["Update the release notes / website (juneau-docs); the docs need a stage deploy, then a prod promote.",
             f"Optional: in Jira (JUNEAU > Releases) mark {ctx.version} released with its date, and the previous "
             "version too if it is still unreleased."]
    ctx.say("Follow-up checklist:\n- " + "\n- ".join(items))
    if ctx.dry_run:
        return "skipped (dry run): checklist not written"
    path = ctx.run_dir / "followup.md"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(f"# Follow-ups for Apache Juneau {ctx.version}\n\n" + "\n".join(f"- [ ] {i}" for i in items) + "\n",
                    encoding="utf-8")
    ctx.rf.set("followup", {"written": True})
    return f"follow-up checklist written to {path.as_uri()}"


@step
def finalize(ctx):
    ctx.rf.update({"phase": "released", "last": "finalize"})
    return f"Apache Juneau {ctx.version} released"
