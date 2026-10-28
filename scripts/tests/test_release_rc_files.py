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
"""The RC steps that used to ask for a review and now write a file: the javadoc report, the release diff and the stale-repo drop."""
import json

import pytest
from conftest import FakeNexus
from jrm_release import rc
from jrm_release.ctx import StepError
from test_release_steps import _release_ctx

pytestmark = pytest.mark.usefixtures("clean_env")


def test_javadoc_writes_a_report_and_never_pauses(make_ctx):
    ctx = make_ctx()
    ctx.fake.script("mvn", "-B", "javadoc:aggregate", out="[WARNING] Foo.java:3: warning: no @param x\nBUILD SUCCESS\n")
    out = rc.javadoc_verify(ctx)
    report = (ctx.run_dir / "javadoc-report.txt").read_text()
    assert str(ctx.ws / "target" / "site" / "apidocs" / "index.html") in report
    assert "warning: no @param x" in report and "BUILD SUCCESS" not in report
    assert (ctx.run_dir / "javadoc-report.txt").as_uri() in out


def test_javadoc_dry_run_writes_nothing(make_ctx):
    ctx = make_ctx(dry_run=True)
    rc.javadoc_verify(ctx)
    assert not (ctx.run_dir / "javadoc-report.txt").exists()


def test_clean_diff_passes_and_writes_the_file(make_ctx):
    ctx = _release_ctx(make_ctx)
    out = rc.release_diff_review(ctx)
    assert out.startswith("diff clean")
    assert (ctx.run_dir / "release-diff.txt").exists()


def test_dirty_diff_fails_naming_the_file(make_ctx):
    ctx = make_ctx()          # every probe answers empty, so the check finds problems
    with pytest.raises(StepError) as e:
        rc.release_diff_review(ctx)
    f = ctx.run_dir / "release-diff.txt"
    assert f.as_uri() in str(e.value) and "--revert" in str(e.value)
    assert "problems" in f.read_text().lower()


def test_diff_review_dry_run_skips(make_ctx):
    ctx = make_ctx(dry_run=True)
    assert rc.release_diff_review(ctx).startswith("skipped (dry run)")


def _run_json(home, version, **data):
    p = home / "runs" / version / "run.json"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps({"schema": 1, **data}))
    return p


def test_drop_stale_drops_unrecorded_open_and_closed_repos(make_ctx, home):
    nx = FakeNexus()
    ctx = make_ctx(nexus=nx)
    _run_json(home, "9.2.0", nexusRepoId="orgapachejuneau-1000")           # another version's repo
    listing = [{"repositoryId": "orgapachejuneau-1000", "state": "closed", "created": "2026-09-01"},
               {"repositoryId": "orgapachejuneau-1001", "state": "open", "created": "2026-10-01"},
               {"repositoryId": "orgapachejuneau-1002", "state": "closed", "created": "2026-10-02"},
               {"repositoryId": "orgapachejuneau-1003", "state": "released", "created": "2026-10-03"}]
    rc.drop_stale(ctx, listing)
    assert nx.posts == [("drop", "orgapachejuneau-1001"), ("drop", "orgapachejuneau-1002")]
    assert any("orgapachejuneau-1001" in s and "2026-10-01" in s for s in ctx.fake.said)


def test_drop_stale_drops_nothing_when_a_run_file_is_unreadable(make_ctx, home):
    nx = FakeNexus()
    ctx = make_ctx(nexus=nx)
    p = home / "runs" / "9.2.0" / "run.json"
    p.parent.mkdir(parents=True)
    p.write_text("{not json")
    rc.drop_stale(ctx, [{"repositoryId": "r-1", "state": "open", "created": "x"}])
    assert nx.posts == [] and any("not dropping" in s for s in ctx.fake.said)


def test_drop_stale_under_dry_run_posts_nothing(make_ctx):
    from conftest import FakeHttp
    from jrm_release.nexus import Nexus
    http = FakeHttp({("POST", "/bulk/drop"): (200, b"")})
    ctx = make_ctx(dry_run=True)
    ctx.nexus = Nexus(ctx.runner, lambda: ("u", "p"), opener=http)   # the real client: Runner.gate skips the POST
    rc.drop_stale(ctx, [{"repositoryId": "r-1", "state": "open", "created": "x"}])
    assert http.requests == []
