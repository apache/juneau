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
from . import emails, inventory
from .ctx import StepError
from .rc import dev_dir, git_env, wanted_files

MIRROR_POLLS = 72   # 25 s each: 30 minutes
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
        if polls == MIRROR_POLLS:
            raise ctx.pause(f"The release is not on the mirrors yet ({url}).  Run: {ctx.resume_hint} later.", done=False)
        polls += 1
        ctx.sleep(25)
    to, sub, body = emails.announce(ctx.d, ctx.rf.get("githubReleaseUrl"))
    return emails.send(ctx, "announce", to, sub, body, cc=(inventory.DEV_LIST,))


@step
def followup_checklist(ctx):
    items = ["Update the release notes / website (juneau-docs).", "Update the Docker/Maven badges if any.",
             "Close the release milestone / issues.", "Remove old releases from dist/release if policy requires."]
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
