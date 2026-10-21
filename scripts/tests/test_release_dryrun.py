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

pytestmark = pytest.mark.usefixtures("clean_env")


def test_dry_run_runs_read_only_only(make_ctx):
    ctx = make_ctx(dry_run=True)
    ctx.runner.run(["git", "ls-remote", "origin"], title="r", mutating=False)
    ctx.runner.run(["git", "push", "origin", "x"], title="w", mutating=True)
    assert ctx.fake.argvs() == [["git", "ls-remote", "origin"]]


def test_dry_run_prints_every_mutation(make_ctx):
    ctx = make_ctx(dry_run=True)
    r = ctx.runner.run(["mvn", "-B", "deploy"], title="w", mutating=True, tool="maven")
    assert r.ok
    assert r.code == 0
    assert any(s == "would run: mvn -B deploy" for s in ctx.fake.said)


def test_dry_run_never_writes_run_json(make_ctx, home):
    ctx = make_ctx(dry_run=True)
    ctx.rf.set("last", "workspace")
    ctx.rf.update({"nexusRepoId": "r-1"})
    assert not ctx.rf.path.exists()
    assert ctx.rf.get("last") == "workspace"


def test_dry_run_masks_secrets(make_ctx):
    ctx = make_ctx(dry_run=True)
    ctx.creds["credential.apache.password"] = "real-pw"          # as if a real run had cached it
    ctx.runner.secrets = lambda: ["real-pw"]
    ctx.runner.run(["svnmucc", "--password", "real-pw"], title="x", mutating=True)
    assert all("real-pw" not in s for s in ctx.fake.said)
    assert any("********" in s for s in ctx.fake.said)
    asked = [a[1] for a in ctx.prompt.asked]
    assert "credential.apache.password" not in asked
    assert "credential.gpg.passphrase" not in asked


def test_dry_run_does_not_ask_for_secrets(make_ctx):
    from conftest import FakePrompts
    ctx = make_ctx(dry_run=True)
    ctx.creds.clear()
    ctx.prompt = FakePrompts({"credential.apache.availid": "jb"})        # a secret prompt would raise
    assert ctx.cred("credential.apache.password", secret=True) == "********"
    assert ctx.cred("credential.apache.availid") == "jb"


def test_dry_run_stops_at_vote_gate(make_ctx):
    import pytest
    from jrm_release import vote
    ctx = make_ctx(dry_run=True)
    with pytest.raises(ctx.pause) as e:
        vote.vote_gate(ctx)
    assert ctx.rf.get("phase") == "vote"
    assert not ctx.rf.path.exists()
    assert e.value.done is True


def test_nexus_post_is_gated_by_dry_run(make_ctx):
    from conftest import FakeHttp
    from jrm_release.nexus import Nexus
    http = FakeHttp({("POST", "/bulk/promote"): (201, b"")})
    ctx = make_ctx(dry_run=True)
    Nexus(ctx.runner, lambda: ("u", "p"), opener=http).promote("r-1")
    assert http.requests == []
    assert any(s.startswith("would run: POST") for s in ctx.fake.said)
    live = make_ctx()
    Nexus(live.runner, lambda: ("u", "p"), opener=http).promote("r-1")
    assert [r[0] for r in http.requests] == ["POST"]
    assert http.requests[0][3]["Authorization"].startswith("Basic ")
