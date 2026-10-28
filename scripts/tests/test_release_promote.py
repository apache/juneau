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
"""The promote phase's waiting steps: the mirror wait pauses instead of asking, and the follow-up checklist is a file."""
import pytest
from conftest import PauseExc
from jrm_release import promote

pytestmark = pytest.mark.usefixtures("clean_env")


def test_mirror_wait_pauses_after_30_minutes(make_ctx):
    sleeps = []
    ctx = make_ctx(http_ok=lambda url: False)
    ctx.sleep = sleeps.append
    with pytest.raises(PauseExc) as e:
        promote.compose_announce_email(ctx)
    assert sleeps == [25] * 72
    assert e.value.done is False and ctx.resume_hint in e.value.message and "mirrors" in e.value.message


def test_mirror_wait_proceeds_when_the_release_appears(make_ctx):
    sleeps, calls = [], []

    def seen(url):
        calls.append(url)
        return len(calls) > 3
    ctx = make_ctx(http_ok=seen)
    ctx.sleep = sleeps.append
    with pytest.raises(PauseExc):                      # then the announce email itself pauses
        promote.compose_announce_email(ctx)
    assert sleeps == [25] * 3 and ctx.rf.get("emailPending") == "announce"


def test_followup_checklist_is_written_not_asked(make_ctx):
    ctx = make_ctx()
    out = promote.followup_checklist(ctx)
    f = ctx.run_dir / "followup.md"
    assert f.read_text().startswith("# Follow-ups for Apache Juneau 9.2.1\n\n- [ ] ")
    assert ctx.rf.get("followup") == {"written": True}
    assert f.as_uri() in out


def test_followup_dry_run_writes_nothing(make_ctx):
    ctx = make_ctx(dry_run=True)
    assert promote.followup_checklist(ctx).startswith("skipped (dry run)")
    assert not (ctx.run_dir / "followup.md").exists()
