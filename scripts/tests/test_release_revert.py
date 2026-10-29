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
import pytest

from conftest import FakeNexus
from jrm_release import revert
from jrm_release.ctx import StepError

pytestmark = pytest.mark.usefixtures("clean_env")

RC = {"phase": "rc", "last": "nexus-close", "nexusRepoId": "r-1", "rcCommit": "abc123"}


def test_revert_refused_in_promote(make_ctx):
    for phase in ("promote", "released"):
        ctx = make_ctx(state={**RC, "phase": phase})
        with pytest.raises(StepError, match="public"):
            revert.revert_check(ctx)


def test_revert_check_keeps_the_phase_guard_and_asks_nothing(make_ctx):
    ctx = make_ctx(state=RC)
    assert revert.revert_check(ctx) == "reverting RC1"


def test_revert_after_a_pause_needs_no_typed_confirmation(make_ctx):
    # CLI --revert is the confirmation; a pending mail and its pause do not block it
    ctx = make_ctx(state={**RC, "emailPending": "propose",
                          "pause": {"step": "compose-propose-email", "message": "m", "at": "t"}})
    assert revert.revert_check(ctx) == "reverting RC1"


def test_revert_skips_absent_remote_state(make_ctx):
    nx = FakeNexus()                    # no staging repo
    ctx = make_ctx(nexus=nx, state={**RC, "nexusRepoId": None})
    ctx.fake.script("svn", "ls", ok=False, out="svn: E170000: URL doesn't exist\n")                              # dist/dev dirs absent
    ctx.fake.script("git", "-C", str(ctx.ws), "ls-remote", out="")      # tag absent
    ctx.fake.script("git", "-C", str(ctx.ws), "status", out="")
    revert.run_all(ctx)
    assert nx.posts == []
    assert not ctx.fake.ran("svnmucc")
    assert not [a for a in ctx.fake.argvs() if "push" in a]


def test_version_reset_commits_and_pushes_without_asking(make_ctx):
    ctx = make_ctx(state=RC)
    ctx.fake.script("git", "-C", str(ctx.ws), "status", out=" M pom.xml\n")
    assert revert.revert_versions(ctx) == "versions reset and pushed"
    argvs = ctx.fake.argvs()
    assert any(a[:3] == ["mvn", "-B", "release:update-versions"] and "-DdevelopmentVersion=9.2.1-SNAPSHOT" in a
               for a in argvs)
    assert any("commit" in a for a in argvs)
    assert any(a[-3:] == ["push", "origin", "juneau-9.2.1-branch"] for a in argvs)


def test_revert_record_bumps_rc_and_resets_last(make_ctx):
    ctx = make_ctx(state={**RC, "rcHistory": []})
    revert.revert_record(ctx)
    assert ctx.rf.get("details")["rc"] == 2
    assert ctx.rf.get("last") is None
    assert ctx.rf.get("nexusRepoId") is None
    assert ctx.rf.get("rcHistory")[-1]["rc"] == 1
    assert ctx.rf.get("phase") == "rc"


def test_revert_rerun_idempotent(make_ctx):
    ctx = make_ctx(state=RC)
    nx = ctx.nexus
    nx.repos = [{"repositoryId": "r-1", "state": "open"}]
    ctx.fake.script("svn", "ls", ok=False, out="svn: E170000: URL doesn't exist\n")
    ctx.fake.script("git", "-C", str(ctx.ws), "ls-remote", out="")
    ctx.fake.script("git", "-C", str(ctx.ws), "status", out="")
    revert.run_all(ctx)
    ctx.rf.set("nexusRepoId", "r-1")             # as if the first run died before the record row
    revert.run_all(ctx)                          # second run: the repository is already gone, no error
    assert nx.posts == [("drop", "r-1")]


def test_revert_removes_dev_dirs_and_remote_tag_when_present(make_ctx):
    ctx = make_ctx(state=RC)
    ctx.fake.script("svn", "ls", out="x\n")
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.1-RC1\n")
    ctx.fake.script("git", "-C", str(ctx.ws), "ls-remote", out="abc\trefs/tags/juneau-9.2.1-RC1\n")
    assert revert.revert_dist(ctx) == "removed 2 dirs"
    assert revert.revert_tag(ctx) == "tag removed"
    argvs = ctx.fake.argvs()
    assert ["git", "-C", str(ctx.ws), "tag", "-d", "juneau-9.2.1-RC1"] in argvs
    assert ["git", "-C", str(ctx.ws), "push", "origin", ":refs/tags/juneau-9.2.1-RC1"] in argvs


def test_revert_dist_probes_quietly_and_fails_when_svn_cannot_tell(make_ctx):
    ctx = make_ctx(state=RC)
    ctx.fake.script("svn", "ls", ok=False, out="svn: warning: W160013: path not found\n")
    assert revert.revert_dist(ctx) == "nothing on dist/dev"
    assert all(c["quiet"] for c in ctx.fake.calls if c["argv"][:2] == ["svn", "ls"])
    ctx.fake.script("svn", "ls", ok=False, out="svn: E175002: Connection refused\n")
    with pytest.raises(StepError, match="could not check dist/dev"):
        revert.revert_dist(ctx)
