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
from jrm_release import inventory, promote
from jrm_release.ctx import StepError

pytestmark = pytest.mark.usefixtures("clean_env")


def test_mirror_wait_pauses_after_30_minutes(make_ctx):
    sleeps = []
    ctx = make_ctx(http_ok=lambda url: False)
    ctx.sleep = sleeps.append
    with pytest.raises(PauseExc) as e:
        promote.compose_announce_email(ctx)
    assert sleeps == [25] * 72
    assert e.value.done is False and ctx.resume_hint in e.value.message and "mirrors" in e.value.message


def test_mirror_wait_notes_progress_on_the_row(make_ctx):
    notes = []
    ctx = make_ctx(http_ok=lambda url: False)
    ctx.sleep = lambda s: None
    ctx.current_step = lambda: type("Row", (), {"detail": staticmethod(notes.append)})()
    with pytest.raises(PauseExc):
        promote.compose_announce_email(ctx)
    assert len(notes) == promote.MIRROR_POLLS
    assert notes[0] == "waiting for downloads.apache.org (1/72, ~30:00 left)"
    assert notes[-1] == "waiting for downloads.apache.org (72/72, ~00:25 left)"


def test_github_release_url_falls_back_to_the_tag_page(make_ctx):
    ctx = make_ctx(state={"vote": {"outcome": "passed"}})
    ctx.fake.script("gh", "release", "create", out="")
    out = promote.github_release(ctx)
    url = "https://github.com/apache/juneau/releases/tag/juneau-9.2.1"
    assert out == url and ctx.rf.get("githubReleaseUrl") == url
    gh = next(c for c in ctx.fake.calls if c["argv"][0] == "gh")
    assert gh.get("capture") is True


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


def test_followup_checklist_is_trimmed_to_what_applies(make_ctx):
    ctx = make_ctx()
    promote.followup_checklist(ctx)
    text = (ctx.run_dir / "followup.md").read_text()
    assert "badges" not in text and "milestone" not in text and "dist/release" not in text
    assert "stage deploy" in text and "prod promote" in text
    assert "Jira" in text and "9.2.1" in text and "previous version" in text


def test_announce_pause_asks_for_plain_text_and_the_eml_is_plain(make_ctx):
    ctx = make_ctx()
    with pytest.raises(PauseExc) as e:
        promote.compose_announce_email(ctx)
    assert "PLAIN-TEXT" in e.value.message and "announce@" in e.value.message
    eml = (ctx.run_dir / "emails" / "announce.eml.txt").read_text()
    assert "text/html" not in eml.lower() and "<html" not in eml.lower() and "Content-Type" not in eml


def test_reporter_step_pauses_with_the_link_then_resumes(make_ctx):
    opened = []
    ctx = make_ctx()
    ctx.opener = opened.append
    with pytest.raises(PauseExc) as e:
        promote.reporter_release(ctx)
    assert e.value.done is False and "addrelease.html?juneau" in e.value.message and "9.2.1" in e.value.message
    assert opened == [inventory.REPORTER_ADD] and ctx.rf.get("emailPending") == "reporter"
    assert "entered" in promote.reporter_release(ctx) and ctx.rf.get("emailPending") is None


def test_reporter_step_dry_run_does_not_pause_or_open(make_ctx):
    opened = []
    ctx = make_ctx(dry_run=True)
    ctx.opener = opened.append
    assert promote.reporter_release(ctx).startswith("skipped (dry run)") and not opened


def _listing(ctx, top, binaries=None, source=None):
    ls = {inventory.DIST_RELEASE + "/": top, inventory.DIST_RELEASE + "/binaries/": binaries,
          inventory.DIST_RELEASE + "/source/": source}
    ctx.svn.ls = lambda url, **k: ls.get(url)


def test_cleanup_old_layout_removes_everything_but_new_version_and_keys(make_ctx):
    seen = []
    ctx = make_ctx(http_ok=lambda url: seen.append(url) or True)
    _listing(ctx, ["9.2.1/", "KEYS", "binaries/", "source/"],
             ["juneau-9.1.0/", "juneau-9.2.0/"], ["juneau-9.1.0/", "juneau-9.2.0/"])
    out = promote.dist_cleanup(ctx)
    mucc = ctx.fake.ran("svnmucc")
    assert len(mucc) == 1 and "removed 2" in out
    assert len(seen) == 4 and inventory.ARCHIVE + "/binaries/juneau-9.1.0/" in seen
    argv = mucc[0]
    removed = [argv[i + 1].removeprefix(inventory.DIST_RELEASE + "/") for i, a in enumerate(argv) if a == "rm"]
    assert sorted(removed) == ["binaries", "source"]
    assert "9.2.1" not in " ".join(removed) and "KEYS" not in removed


def test_cleanup_keeps_a_subdir_that_still_holds_the_new_version(make_ctx):
    ctx = make_ctx(http_ok=lambda url: True)
    _listing(ctx, ["KEYS", "binaries/"], ["juneau-9.2.1/", "juneau-9.2.0/"])
    promote.dist_cleanup(ctx)
    argv = ctx.fake.ran("svnmucc")[0]
    assert [a for a in argv if a.endswith("juneau-9.2.0")] and not [a for a in argv if a.endswith("/binaries")]


def test_cleanup_top_level_layout_and_archive_urls_checked(make_ctx):
    seen = []
    ctx = make_ctx(http_ok=lambda url: seen.append(url) or True)
    _listing(ctx, ["9.2.1/", "9.2.0/", "KEYS"])
    promote.dist_cleanup(ctx)
    assert seen == [inventory.ARCHIVE + "/9.2.0/"] and len(ctx.fake.ran("svnmucc")) == 1


def test_cleanup_aborts_when_any_candidate_is_missing_from_the_archive(make_ctx):
    ctx = make_ctx(http_ok=lambda url: "9.1.0" not in url)
    _listing(ctx, ["9.2.1/", "9.1.0/", "9.2.0/", "KEYS"])
    with pytest.raises(StepError) as e:
        promote.dist_cleanup(ctx)
    assert "9.1.0" in str(e.value) and not ctx.fake.ran("svnmucc")


def test_cleanup_dry_run_lists_only(make_ctx):
    ctx = make_ctx(dry_run=True, http_ok=lambda url: True)
    _listing(ctx, ["9.2.1/", "9.2.0/", "KEYS"])
    assert "would remove 1" in promote.dist_cleanup(ctx)
    assert not ctx.fake.ran("svnmucc")


def test_cleanup_with_nothing_old_is_a_no_op(make_ctx):
    ctx = make_ctx()
    _listing(ctx, ["9.2.1/", "KEYS"])
    assert "nothing to remove" in promote.dist_cleanup(ctx) and not ctx.fake.ran("svnmucc")


def test_revert_has_no_cleanup_row():
    from jrm_release import revert
    assert not [r for r in revert.ROWS if "cleanup" in r[0] or "reporter" in r[0]]
