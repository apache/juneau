"""Promote phase (spec 6.3 rows 19-25).  Irreversible from nexus-release on."""
from . import emails, inventory
from .ctx import StepError
from .rc import dev_dir, git_env, wanted_files

FUNCS = {}


def step(fn):
    FUNCS[fn.__name__.replace("_", "-")] = fn
    return fn


@step
def nexus_release(ctx):
    v, rid = ctx.version, ctx.rf.get("nexusRepoId")
    if (ctx.rf.get("vote") or {}).get("outcome") != "passed":
        raise StepError("the vote has not passed")
    ok = ctx.prompt.confirm(f"Release Apache Juneau {v} to Maven Central?  This cannot be undone.  Type {v} to confirm.",
                            key="confirm.nexus-release", expect=v, danger=True)
    if ok is not True:
        raise StepError("nexus-release not confirmed")
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
    r = ctx.runner.run(argv, title="GitHub release", mutating=True, tool="github",
                       env={"GH_TOKEN": ctx.cred("credential.github.token", secret=True)})
    if not r.ok:
        raise StepError("gh release create failed")
    url = (r.out or "").strip().splitlines()[-1] if (r.out or "").strip() else None
    ctx.rf.set("githubReleaseUrl", url)
    return url or "release created"


@step
def compose_announce_email(ctx):
    url = f"{inventory.DOWNLOADS}/{ctx.version}/"
    polls = 0
    while not ctx.http_ok(url):
        if ctx.dry_run:
            ctx.say(f"dry run: {url} is not published; the real run waits for it")
            break
        polls += 1
        if polls % 12 == 0 and ctx.prompt.confirm(
                "The release is not on downloads.apache.org yet.  Wait another 5 minutes?",
                key="announce.wait") is not True:
            raise ctx.pause("Announce later: re-run with --resume once downloads.apache.org has it.", done=False)
        ctx.sleep(25)
    to, sub, body = emails.announce(ctx.d, ctx.rf.get("githubReleaseUrl"))
    return emails.send(ctx, "announce", to, sub, body, cc=(inventory.DEV_LIST,))


@step
def followup_checklist(ctx):
    items = ["Update the release notes / website (juneau-docs).", "Update the Docker/Maven badges if any.",
             "Close the release milestone / issues.", "Remove old releases from dist/release if policy requires."]
    ctx.say("Follow-up checklist:\n- " + "\n- ".join(items))
    if ctx.review("Have you done these follow-ups?", key="confirm.followup", look_at="The checklist above:") is not True:
        raise ctx.pause("Finish the follow-ups, then re-run with --resume.", done=False)
    ctx.rf.set("followup", {"done": True})
    return "skipped (dry run): nothing to review" if ctx.dry_run else "follow-ups done"


@step
def finalize(ctx):
    ctx.rf.update({"phase": "released", "last": "finalize"})
    return f"Apache Juneau {ctx.version} released"
