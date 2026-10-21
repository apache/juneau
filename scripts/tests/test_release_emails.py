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

from jrm_release import emails

pytestmark = pytest.mark.usefixtures("clean_env")

D = dict(version="9.2.1", rc=1, branch="b", summary="Bug fixes.", highlights="", knownIssues="",
         acknowledgements="")
FACTS = dict(src_sha="aaa", bin_sha="bbb", commit="abc123", repo="orgapachejuneau-1002",
             deadline="22-Oct-2026 2:00PM EDT")


def test_vote_body_matches_juneau_text():
    to, sub, body = emails.vote(D, FACTS)
    assert to == "dev@juneau.apache.org" and sub == "[VOTE] Release Apache Juneau 9.2.1 RC1"
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
    assert "Highlights" not in plain and "Known issues" not in plain
    _, _, rich = emails.vote({**D, "highlights": "Faster.", "knownIssues": "None much."}, FACTS)
    first_para_end = rich.index("RC1.") + 4
    assert rich.index("Highlights") > first_para_end and rich.index("Known issues") > rich.index("Highlights")


def test_mailto_drops_long_body():
    assert emails.mailto_url("a@b", "s", "x" * 100).endswith("x" * 100)
    long = emails.mailto_url("a@b", "s", "x" * 3000)
    assert "body=" not in long and len(long) < 1800


def test_not_yet_pauses_without_recording(make_ctx):
    ctx = make_ctx(answers={"email.propose": False})
    with pytest.raises(ctx.pause) as e:
        emails.send(ctx, "propose", "dev@juneau.apache.org", "s", "b")
    assert e.value.done is False and not ctx.rf.get("emails")


def test_sent_is_recorded(make_ctx):
    ctx = make_ctx(answers={"email.propose": True})
    assert emails.send(ctx, "propose", "dev@juneau.apache.org", "s", "b") == "propose email sent"
    assert "propose" in ctx.rf.get("emails")


def test_cancelled_email_is_ctrl_c(make_ctx):
    ctx = make_ctx(answers={"email.propose": None})
    with pytest.raises(KeyboardInterrupt):
        emails.send(ctx, "propose", "dev@juneau.apache.org", "s", "b")


def test_result_and_announce_text():
    to, sub, body = emails.result(D, "passed", "Binding +1: 3 (A, B, C)")
    assert sub == "[RESULT][VOTE] Release Apache Juneau 9.2.1 RC1" and "has passed" in body
    to, sub, body = emails.announce({**D, "acknowledgements": "Thanks all."}, "https://github.com/x/y")
    assert to == "announce@apache.org" and sub == "[ANNOUNCE] Apache Juneau 9.2.1 released"
    assert "https://downloads.apache.org/juneau/9.2.1/" in body or "dist/release/juneau/9.2.1/" in body
    assert "https://github.com/x/y" in body and "Thanks all." in body
