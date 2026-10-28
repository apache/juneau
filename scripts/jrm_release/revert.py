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
"""--revert: undo the current release candidate.  Each row checks remote state first, so a rerun is harmless."""
from .ctx import StepError
from .rc import dev_dir, git_env


def revert_check(ctx):
    if ctx.rf.get("phase") in ("promote", "released"):
        raise StepError("the release is public (phase promote/released); revert is refused")
    return f"reverting RC{ctx.d['rc']}"


def revert_nexus(ctx):
    rid = ctx.rf.get("nexusRepoId")
    if not rid:
        return "no staging repository recorded"
    state = ctx.nexus.repo(rid).get("state")
    if state in ("open", "closed"):
        ctx.nexus.drop(rid)
        return f"dropped {rid}"
    return f"{rid} already {state}"


def revert_dist(ctx):
    ops = [("rm", dev_dir(ctx, k)) for k in ("source", "binaries") if ctx.svn.ls(dev_dir(ctx, k) + "/") is not None]
    if not ops:
        return "nothing on dist/dev"
    ctx.svn.mucc(f"Remove {ctx.rc_tag}", ops, title="Remove RC from dist/dev")
    return f"removed {len(ops)} dirs"


def revert_tag(ctx):
    ws, tag = str(ctx.ws), ctx.rc_tag
    R = ctx.runner.run
    local = R(["git", "-C", ws, "tag", "--list", tag], title="Local tag", mutating=False).out.strip()
    if local:
        R(["git", "-C", ws, "tag", "-d", tag], title="Delete local tag", mutating=True)
    remote = R(["git", "-C", ws, "ls-remote", "--tags", "origin", f"refs/tags/{tag}"], title="Remote tag",
               mutating=False).out.strip()
    if remote:
        R(["git", "-C", ws, "push", "origin", f":refs/tags/{tag}"], title="Delete remote tag", mutating=True,
          env=git_env(ctx))
    return "tag removed" if (local or remote) else "tag already absent"


def revert_versions(ctx):
    ws, d = str(ctx.ws), ctx.d
    ctx.runner.run(["mvn", "-B", "release:update-versions", "-DautoVersionSubmodules=true",
                    f"-DdevelopmentVersion={d['version']}-SNAPSHOT"], title="Reset versions", mutating=True,
                   tool="maven", cwd=ctx.ws)
    dirty = ctx.runner.run(["git", "-C", ws, "status", "--porcelain"], title="Status", mutating=False).out.strip()
    if not dirty:
        return "versions already reset"
    ctx.runner.run(["git", "-C", ws, "commit", "-a", "-m", f"Reset versions to {d['version']}-SNAPSHOT"],
                   title="Commit", mutating=True)
    ctx.runner.run(["git", "-C", ws, "push", "origin", d["branch"]], title="Push", mutating=True, env=git_env(ctx))
    return "versions reset and pushed"


def revert_record(ctx):
    d = dict(ctx.d)
    hist = list(ctx.rf.get("rcHistory") or [])
    hist.append({"rc": d["rc"], "nexusRepoId": ctx.rf.get("nexusRepoId"), "commit": ctx.rf.get("rcCommit")})
    d["rc"] += 1
    ctx.rf.update({"details": d, "rcHistory": hist, "last": None, "nexusRepoId": None, "rcCommit": None,
                   "voteDeadline": None, "vote": None, "phase": "rc"})
    return f"ready for RC{d['rc']}"


# (row name, title, console label); the registry in release.py runs them in this order.
ROWS = [("revert-check", "Check the revert is allowed", "Revert"), ("revert-nexus", "Drop the Nexus staging repository", "Nexus"),
        ("revert-dist", "Remove the RC from dist/dev", "SVN"), ("revert-tag", "Delete the RC tag", "Git"),
        ("revert-versions", "Reset the pom versions", "Maven"), ("revert-record", "Record the revert", "Done")]
FUNCS = {"revert-check": revert_check, "revert-nexus": revert_nexus, "revert-dist": revert_dist,
         "revert-tag": revert_tag, "revert-versions": revert_versions, "revert-record": revert_record}


def run_all(ctx):
    """Every row after the check, in order (the Registry runs them one by one in a real revert)."""
    for name, _, _ in ROWS[1:]:
        ctx.say(f"{name}: {FUNCS[name](ctx)}")
