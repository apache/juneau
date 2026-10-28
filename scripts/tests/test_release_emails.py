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
from conftest import PauseExc

from jrm_release import emails, inventory, promote, rc

pytestmark = pytest.mark.usefixtures("clean_env")

D = dict(version="9.2.1", rc=1, branch="b", summary="Bug fixes.", highlights="", knownIssues="",
         acknowledgements="")
FACTS = dict(src_sha="aaa", bin_sha="bbb", commit="abc123", repo="orgapachejuneau-1002",
             deadline="22-Oct-2026 2:00PM EDT")


def test_vote_body_matches_juneau_text():
    to, sub, body = emails.vote(D, FACTS)
    assert to == "dev@juneau.apache.org"
    assert sub == "[VOTE] Release Apache Juneau 9.2.1 RC1"
    for line in ["I am pleased to be calling this vote for the source release of Apache Juneau 9.2.1 RC1.",
                 "https://dist.apache.org/repos/dist/dev/juneau/binaries/juneau-9.2.1-RC1/",
                 "https://dist.apache.org/repos/dist/dev/juneau/source/juneau-9.2.1-RC1/",
                 "apache-juneau-9.2.1-src.zip: \naaa", "apache-juneau-9.2.1-bin.zip: \nbbb",
                 "mvn clean install", "https://dist.apache.org/repos/dist/release/juneau/KEYS",
                 "https://repository.apache.org/content/repositories/orgapachejuneau-1002/",
                 "https://gitbox.apache.org/repos/asf?p=juneau.git;a=commit;h=abc123",
                 "This vote will be open until 22-Oct-2026 2:00PM EDT and passes if a majority of at least "
                 "three +1 Apache Juneau PMC votes are cast.\n(needs to be at least 72 weekday hours)",
                 "[ ] +1 Release this package", "[ ] -1 Do not release this package because...",
                 "Anyone can participate in testing and voting, not just committers"]:
        assert line in body, line


def test_optional_sections_omitted():
    _, _, plain = emails.vote(D, FACTS)
    assert "Highlights" not in plain
    assert "Known issues" not in plain
    _, _, rich = emails.vote({**D, "highlights": "Faster.", "knownIssues": "None much."}, FACTS)
    first_para_end = rich.index("RC1.") + 4
    assert rich.index("Highlights") > first_para_end
    assert rich.index("Known issues") > rich.index("Highlights")


def test_mailto_drops_long_body():
    assert emails.mailto_url("a@b", "s", "x" * 100).endswith("x" * 100)
    long = emails.mailto_url("a@b", "s", "x" * 3000)
    assert "body=" not in long
    assert len(long) < 1800


def test_first_pass_writes_the_mail_and_pauses(make_ctx):
    ctx = make_ctx()
    with pytest.raises(PauseExc) as e:
        rc.compose_propose_email(ctx)
    f = ctx.run_dir / "emails" / "propose.eml.txt"
    assert f.read_text().startswith(f"To: {inventory.DEV_LIST}\nSubject: [PROPOSE] Release Apache Juneau 9.2.1\n\n")
    assert e.value.done is False
    assert f.as_uri() in e.value.message and ctx.resume_hint in e.value.message
    assert ctx.rf.get("emailPending") == "propose"
    pause = ctx.rf.get("pause")                                    # persisted with emailPending, before the toolkit sees the Pause
    assert pause["step"] == "compose-propose-email" and pause["message"] == e.value.message
    assert any("mailto:" in s for s in ctx.fake.said)            # the mailto link is printed, not stored in the pause


def test_resume_pass_records_the_mail_as_sent(make_ctx):
    ctx = make_ctx()
    with pytest.raises(PauseExc):
        rc.compose_propose_email(ctx)
    assert rc.compose_propose_email(ctx) == "propose email sent"
    assert "propose" in ctx.rf.get("emails")
    assert ctx.rf.get("emailPending") is None


def test_cc_is_written_for_the_announce_mail(make_ctx):
    ctx = make_ctx(state={"githubReleaseUrl": "https://example/r"}, http_ok=lambda u: True)
    with pytest.raises(PauseExc):
        promote.compose_announce_email(ctx)
    assert f"Cc: {inventory.DEV_LIST}\n" in (ctx.run_dir / "emails" / "announce.eml.txt").read_text()


def test_dry_run_writes_nothing_and_does_not_pause(make_ctx):
    ctx = make_ctx(dry_run=True)
    assert rc.compose_propose_email(ctx).startswith("skipped (dry run)")
    assert not (ctx.run_dir / "emails").exists()


def test_vote_resume_keeps_the_emailed_deadline(make_ctx):
    from datetime import timedelta
    ctx = make_ctx(state={"nexusRepoId": "orgapachejuneau-1002"})
    with pytest.raises(PauseExc):
        rc.compose_vote_email(ctx)
    emailed = ctx.rf.get("voteDeadline")
    first_now = ctx.now
    ctx.now = lambda: first_now() + timedelta(hours=5)       # the resume comes hours later
    assert rc.compose_vote_email(ctx) == "vote email sent"
    assert ctx.rf.get("voteDeadline") == emailed


def test_attempt_start_after_a_kill_keeps_the_mail_visibly_pending(make_ctx):
    # Crash case: send() persisted emailPending + pause, the process died before _execute recorded anything.
    ctx = make_ctx()
    with pytest.raises(PauseExc):
        rc.compose_propose_email(ctx)
    persisted = ctx.rf.get("pause")
    assert persisted and ctx.rf.get("emailPending") == "propose"
    # a --resume then acknowledges the mail as sent and the pause is cleared by the next attempt_start (Task 8)
    assert rc.compose_propose_email(ctx) == "propose email sent"


def test_reset_leaves_another_mails_pending_mark(make_ctx):
    ctx = make_ctx(state={"emailPending": "propose", "emails": {"vote": "x"}})
    emails.reset(ctx, "vote")
    assert ctx.rf.get("emailPending") == "propose" and ctx.rf.get("emails") is None


def test_reset_clears_the_pending_and_sent_marks(make_ctx):
    ctx = make_ctx(state={"emailPending": "vote", "emails": {"vote": "x", "propose": "y"}})
    emails.reset(ctx, "vote")
    assert ctx.rf.get("emailPending") is None and ctx.rf.get("emails") == {"propose": "y"}


def test_result_and_announce_text():
    to, sub, body = emails.result(D, "passed", "Binding +1: 3 (A, B, C)")
    assert sub == "[RESULT][VOTE] Release Apache Juneau 9.2.1 RC1"
    assert "has passed" in body
    to, sub, body = emails.announce({**D, "acknowledgements": "Thanks all."}, "https://github.com/x/y")
    assert to == "announce@apache.org"
    assert sub == "[ANNOUNCE] Apache Juneau 9.2.1 released"
    assert "https://downloads.apache.org/juneau/9.2.1/" in body or "dist/release/juneau/9.2.1/" in body
    assert "https://github.com/x/y" in body
    assert "Thanks all." in body
