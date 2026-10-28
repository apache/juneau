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
"""The vote phase: the tally comes from the params file's vote: block, and nexus-release needs no confirmation."""
from datetime import datetime, timedelta

import pytest
from conftest import FakeNexus, PauseExc
from jrm_release import promote, vote
from jrm_release.ctx import StepError

yaml = pytest.importorskip("yaml")

pytestmark = pytest.mark.usefixtures("clean_env")


def iso(delta_hours):
    return (datetime.now().astimezone() + timedelta(hours=delta_hours)).isoformat()


def params_file(tmp_path, block):
    p = tmp_path / "9.2.1.yml"
    p.write_text(block)
    return p


GOOD = "vote:\n  plus1: {p}\n  zero: 0\n  minus1: {m}\n  binding: [Jane, John, Pat]\n"


def test_open_vote_pauses_with_the_deadline(make_ctx):
    ctx = make_ctx(state={"voteDeadline": iso(5)})
    with pytest.raises(PauseExc) as e:
        vote.vote_tally(ctx)
    assert e.value.done is False
    assert ctx.rf.get("voteDeadline") in e.value.message and ctx.resume_hint in e.value.message


@pytest.mark.parametrize("plus1,minus1,outcome", [(3, 0, "passed"), (2, 0, "failed"), (3, 3, "failed"), (4, 3, "passed")])
def test_tally_outcomes(make_ctx, tmp_path, plus1, minus1, outcome):
    p = params_file(tmp_path, GOOD.format(p=plus1, m=minus1))
    ctx = make_ctx(state={"voteDeadline": iso(-1), "paramsPath": str(p)})
    assert vote.vote_tally(ctx) == f"vote {outcome}"
    assert ctx.rf.get("vote") == {"plus1": plus1, "zero": 0, "minus1": minus1,
                                  "binding": ["Jane", "John", "Pat"], "outcome": outcome}


def test_tally_prints_the_counts_before_recording(make_ctx, tmp_path):
    ctx = make_ctx(state={"voteDeadline": iso(-1), "paramsPath": str(params_file(tmp_path, GOOD.format(p=3, m=0)))})
    vote.vote_tally(ctx)
    assert any("+1 3" in s and "Jane" in s for s in ctx.fake.said)


@pytest.mark.parametrize("block", ["", "vote:\n  plus1: 3\n", "vote:\n  plus1: x\n  binding: [A]\n"])
def test_missing_or_bad_block_pauses_naming_what_to_fill_in(make_ctx, tmp_path, block):
    p = params_file(tmp_path, block)
    ctx = make_ctx(state={"voteDeadline": iso(-1), "paramsPath": str(p)})
    with pytest.raises(PauseExc) as e:
        vote.vote_tally(ctx)
    assert e.value.done is False and str(p) in e.value.message and "vote" in e.value.message
    assert ctx.rf.get("vote") is None


def test_falls_back_to_the_conventional_params_path(make_ctx, home):
    (home / "params").mkdir(parents=True)
    (home / "params" / "9.2.1.yml").write_text(GOOD.format(p=3, m=0))
    ctx = make_ctx(state={"voteDeadline": iso(-1)})
    assert vote.vote_tally(ctx) == "vote passed"


def test_result_email_accepts_the_old_free_text_binding(make_ctx):
    ctx = make_ctx(state={"vote": {"plus1": 3, "zero": 0, "minus1": 0, "binding": "A, B, C", "outcome": "passed"}})
    with pytest.raises(PauseExc):
        vote.compose_result_email(ctx)       # an in-flight run recorded a string before this change
    assert "(A, B, C)" in (ctx.run_dir / "emails" / "result.eml.txt").read_text()


@pytest.mark.parametrize("bad", ["not-a-date", "2026-10-28T10:00:00", 12])
def test_unparseable_or_naive_deadline_pauses_instead_of_crashing(make_ctx, bad):
    ctx = make_ctx(state={"voteDeadline": bad})
    if bad == "2026-10-28T10:00:00":                 # naive: comparing with the aware now() raises TypeError
        ctx.now = lambda: datetime.now().astimezone()
    with pytest.raises(PauseExc) as e:
        vote.vote_tally(ctx)
    assert e.value.done is False and "voteDeadline" in e.value.message and ctx.resume_hint in e.value.message


def test_dry_run_tally_records_nothing(make_ctx, tmp_path):
    ctx = make_ctx(dry_run=True, state={"voteDeadline": iso(-1),
                                        "paramsPath": str(params_file(tmp_path, GOOD.format(p=3, m=0)))})
    assert vote.vote_tally(ctx).startswith("skipped (dry run)")
    assert ctx.rf.get("vote") is None


def test_result_email_joins_the_binding_names(make_ctx):
    ctx = make_ctx(state={"vote": {"plus1": 3, "zero": 0, "minus1": 0, "binding": ["A", "B", "C"], "outcome": "passed"}})
    with pytest.raises(PauseExc):
        vote.compose_result_email(ctx)
    assert "(A, B, C)" in (ctx.run_dir / "emails" / "result.eml.txt").read_text()


def test_nexus_release_needs_no_confirmation(make_ctx):
    nx = FakeNexus()
    ctx = make_ctx(nexus=nx, state={"nexusRepoId": "r-1", "vote": {"outcome": "passed"}})
    assert promote.nexus_release(ctx) == "r-1 released"
    assert nx.posts == [("promote", "r-1")]


def test_nexus_release_refuses_without_a_passed_vote(make_ctx):
    nx = FakeNexus()
    for state in ({"nexusRepoId": "r-1"}, {"nexusRepoId": "r-1", "vote": {"outcome": "failed"}}):
        with pytest.raises(StepError, match="vote has not passed"):
            promote.nexus_release(make_ctx(nexus=nx, state=state))
    assert nx.posts == []
