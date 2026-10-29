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
from . import details as details_mod
from .ctx import StepError
from .diffcheck import NEXT_SUBJECT, RELEASE_SUBJECT
from .rc import dev_dir, git_env

PLUGIN = "[maven-release-plugin]"


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
    # exists() is quiet, so an absent dir (the usual case) draws no failed row; None means svn could not tell
    found = {k: ctx.svn.exists(dev_dir(ctx, k) + "/") for k in ("source", "binaries")}
    unknown = [k for k, v in found.items() if v is None]
    if unknown:
        raise StepError(f"could not check dist/dev ({', '.join(unknown)}); rerun --revert")
    ops = [("rm", dev_dir(ctx, k)) for k, v in found.items() if v]
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


def _clean_release_state(ws):
    """release:clean's job, done by hand: a leftover release.properties makes release:prepare resume a stale run."""
    gone = [f for f in [ws / "release.properties", *ws.rglob("*.releaseBackup")] if f.is_file()]
    for f in gone:
        f.unlink()
    return len(gone)


def _plugin_poms(ctx, ws, tag):
    """(release commit, poms the two plugin commits changed), or (None, []) when prepare never ran."""
    R = ctx.runner.run
    log = ["git", "-C", ws, "log", "--fixed-strings", "--format=%H"]
    rel = R(log + ["-n1", f"--grep={PLUGIN} {RELEASE_SUBJECT} {tag}"], title="Release commit", mutating=False).out.strip()
    if not rel:
        return None, []
    nxt = R(log + ["--reverse", f"--grep={PLUGIN} {NEXT_SUBJECT}", f"{rel}..HEAD"], title="Next-iteration commit",
            mutating=False).out.split()
    nxt = nxt[0] if nxt else ""
    poms = set()
    for c, which in ((rel, "release commit"), (nxt, "next-iteration commit")):
        if c:
            poms.update(R(["git", "-C", ws, "diff-tree", "--no-commit-id", "--name-only", "-r", "--diff-filter=M", c],
                          title=f"Poms changed ({which})", mutating=False).out.split())
    return rel, sorted(poms)


def _verify_versions(ctx):
    """The check that matters: the root pom is back at <v>-SNAPSHOT and no pom still names the next dev version."""
    want, nxt = f"{ctx.d['version']}-SNAPSHOT", ctx.d.get("developmentVersion")
    root = ctx.ws / "pom.xml"
    got = details_mod.pom_version(root.read_text(encoding="utf-8")) if root.is_file() else None
    if got != want:
        raise StepError(f"root pom is at {got}, expected {want}")
    stale = [str(p.relative_to(ctx.ws)) for p in sorted(ctx.ws.rglob("pom.xml"))
             if ".git" not in p.parts and "target" not in p.parts and nxt and nxt in p.read_text(encoding="utf-8")]
    if stale:
        raise StepError(f"{nxt} is still in {', '.join(stale[:5])}{' ...' if len(stale) > 5 else ''}")


def revert_versions(ctx):
    """Undo the release plugin's own pom changes (versions, <juneau.version>-style properties, <scm><tag>) by restoring
    the poms its two commits touched to their content before the release commit; then verify."""
    ws, d = str(ctx.ws), ctx.d
    R = ctx.runner.run
    cleaned = 0 if ctx.dry_run else _clean_release_state(ctx.ws)
    rel, poms = _plugin_poms(ctx, ws, ctx.rc_tag)
    if rel and poms:
        R(["git", "-C", ws, "checkout", f"{rel}^", "--", *poms], title="Restore poms", mutating=True)
    dirty = R(["git", "-C", ws, "status", "--porcelain", "--untracked-files=no"], title="Status",
              mutating=False).out.strip()
    if dirty:
        R(["git", "-C", ws, "commit", "-a", "-m", f"Reset versions to {d['version']}-SNAPSHOT"], title="Commit",
          mutating=True)
        R(["git", "-C", ws, "push", "origin", d["branch"]], title="Push", mutating=True, env=git_env(ctx))
    if not ctx.dry_run:
        _verify_versions(ctx)
    if dirty:
        return f"versions reset and pushed ({len(poms)} poms restored)"
    note = f"; removed {cleaned} release-plugin files" if cleaned else ""
    return ("versions already reset" if rel else "no release commit found; cleanup only") + note


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
