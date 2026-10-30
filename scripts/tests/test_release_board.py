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

from pathlib import Path
from types import SimpleNamespace

import pytest

import release
from conftest import FakeRunner
from jrm_release import board, inventory, rc, revert
from jrm_release.runner import Runner

pytestmark = pytest.mark.usefixtures("clean_env")


def test_row_labels_are_zero_padded_unique_and_cover_all_25_steps():
    labels = [inventory.row_label(s) for s in inventory.STEPS]
    assert len(labels) == 25 == len(set(labels))
    assert labels[0] == "01 Prerequisites"
    assert labels[4] == "05 Build verify"
    assert labels[15] == "16 Vote wait (72h)"
    assert labels[24] == "25 Finalize"


def test_longest_label_is_18_characters():
    # The plan and spec say 17 ("23 Announce email"); "08 Deploy snapshot", "11 Release perform", "14 Verify dist/dev"
    # and "16 Vote wait (72h)" are 18.
    assert max(len(inventory.row_label(s)) for s in inventory.STEPS) == 18


def test_num_of_resolves_names_and_retired_steps():
    assert inventory.num_of("workspace") == 4
    assert inventory.num_of("test-workspace-verify") == 6        # RETIRED maps to the step before the removed one
    assert inventory.num_of("nope") is None


def make_runner():
    fake = FakeRunner()
    return Runner(fake), fake


def test_run_passes_parent_when_a_step_is_open():
    runner, fake = make_runner()
    runner.step_id = "build-verify"
    runner.run(["mvn", "verify"], title="Build verify", mutating=True, tool="maven")
    assert fake.calls[0]["parent"] == "build-verify"


def test_run_passes_no_parent_outside_a_step():
    runner, fake = make_runner()
    runner.run(["git", "status"], title="Status", mutating=False)
    assert fake.calls[0]["parent"] is None


def test_run_passes_bar_only_when_given():
    runner, fake = make_runner()
    runner.run(["mvn", "verify"], title="Build verify", mutating=True, tool="maven", bar="reactor")
    runner.run(["mvn", "verify"], title="Build verify", mutating=True, tool="maven")
    assert fake.calls[0]["bar"] == "reactor"
    assert fake.calls[1]["bar"] is None


def test_noting_pause_sets_and_clears_the_step_id(make_ctx):
    import release
    ctx = make_ctx()
    seen, paused = [], []

    def fn(c):
        seen.append(c.runner.step_id)

    release._noting_pause("workspace", fn, paused)(ctx)
    assert seen == ["workspace"]
    assert ctx.runner.step_id is None

    def pauses(c):
        raise c.pause("stop")

    with pytest.raises(type(ctx.pause("x"))):
        release._noting_pause("compose-vote-email", pauses, paused)(ctx)
    assert paused == ["compose-vote-email"]
    assert ctx.runner.step_id is None


def test_release_prepare_tag_check_runs_quietly(make_ctx):
    ctx = make_ctx()
    ctx.ws.mkdir(parents=True, exist_ok=True)
    (ctx.ws / "pom.xml").write_text("<project><version>9.2.1-SNAPSHOT</version></project>")
    rc.release_prepare(ctx)
    tag_calls = [c for c in ctx.fake.calls if c["argv"][3:5] == ["tag", "--list"]]
    assert tag_calls and all(c["quiet"] is True for c in tag_calls)


def test_gpg_unlock_prime_runs_quietly(make_ctx):
    ctx = make_ctx()
    rc.gpg_unlock(ctx)
    gpg = [c for c in ctx.fake.calls if c["argv"][0] == "gpg"]
    assert gpg and all(c["quiet"] is True for c in gpg)
    assert gpg[0]["input"] == b"s3cr3t-gpg\n"


# ---- feature detection, pause table, run number --------------------------------------------------------------------


def args(**kw):
    base = dict(resume=False, start_step=None, dry_run=False, revert=False)
    return SimpleNamespace(**{**base, **kw})


class FakeRf:
    def __init__(self, last=None, attempts=0, pending=None):
        self.data = {"attempts": [{}] * attempts, "emailPending": pending}
        self._last = last

    def last(self):
        return self._last

    def get(self, key):
        return self.data.get(key)


def states(dividers):
    return {d[0]: d[2] for d in dividers}


def test_features_come_from_the_toolkit_and_default_to_none():
    assert board.board_features(SimpleNamespace(run=SimpleNamespace())) == frozenset()
    assert board.board_features(SimpleNamespace(run=SimpleNamespace(BOARD_FEATURES=frozenset({"dividers"})))) == {"dividers"}


def test_fresh_run_is_run_1_and_stops_at_the_proposal_email():
    n, dividers = board.divider_plan(args(), FakeRf())
    assert n == 1
    assert states(dividers)["compose-propose-email"] == "stops"
    assert {s for k, s in states(dividers).items() if k != "compose-propose-email"} == {"later"}
    assert [d[0] for d in dividers] == ["compose-propose-email", "compose-vote-email", "vote-gate",
                                        "compose-result-email", "compose-announce-email"]


def test_resume_after_the_proposal_pause_marks_it_passed_and_numbers_the_run():
    n, dividers = board.divider_plan(args(resume=True), FakeRf(last="prechecks", attempts=2, pending="propose"))
    assert n == 3
    assert states(dividers)["compose-propose-email"] == "passed"
    assert states(dividers)["compose-vote-email"] == "stops"


def test_start_step_marks_earlier_dividers_passed():
    n, dividers = board.divider_plan(args(start_step="release-perform"), FakeRf(attempts=1))
    assert states(dividers)["compose-propose-email"] == "passed"
    assert states(dividers)["compose-vote-email"] == "stops"


def test_dry_run_passes_every_divider_and_predicts_no_stop():
    n, dividers = board.divider_plan(args(dry_run=True), FakeRf())
    assert set(states(dividers).values()) == {"passed"}


def test_start_step_at_a_pause_step_reruns_its_pause():
    n, dividers = board.divider_plan(args(start_step="compose-vote-email"), FakeRf(attempts=1))
    assert states(dividers)["compose-vote-email"] == "stops"


# ---- session keywords reach the toolkit (old toolkit, new toolkit, revert) ------------------------------------------


class _Session:
    code = 0

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False


class StubRun(FakeRunner):
    """A toolkit stand-in: records the keywords session() is given.  features=None leaves BOARD_FEATURES off entirely."""

    def __init__(self, features):
        super().__init__()
        self.kwargs = {}
        if features is not None:
            self.BOARD_FEATURES = frozenset(features)

    def session(self, title, **kw):
        self.kwargs = kw
        return _Session()


def recorded_session_kwargs(home, monkeypatch, *, features, revert=False, extra_argv=()):
    """Drive release.run_release for a new (or --revert) run against a stub toolkit and return session()'s keywords."""
    stub = StubRun(features)
    deps = release.Deps(run=stub, steps=SimpleNamespace(Pause=RuntimeError), lock=None)
    monkeypatch.setattr(release, "_drive", lambda *a, **k: 0)
    monkeypatch.setattr(release.params, "require", lambda a: Path("params.yml"))
    active = {"details": {"version": "9.2.1", "rc": 1}, "phase": "rc"} if revert else {}
    argv = ["--home", str(home), *(["--revert"] if revert else []), *extra_argv]
    a = release.parse(argv)
    release.run_release(a, deps, home / "runs" / "9.2.1" / "run.json", active)
    return stub.kwargs


def test_new_toolkit_gets_numbered_plan_run_and_dividers(home, monkeypatch):
    kw = recorded_session_kwargs(home, monkeypatch, features={"dividers"})
    assert kw["plan"][0] == ("prerequisites", "01 Prerequisites")
    assert len(kw["plan"]) == 25
    assert kw["run"] == 1
    assert [d[0] for d in kw["dividers"]][0] == "compose-propose-email"
    assert "history_alias" not in kw


def test_old_toolkit_gets_no_new_keyword(home, monkeypatch):
    kw = recorded_session_kwargs(home, monkeypatch, features=None)        # the stub has no BOARD_FEATURES attribute
    assert "run" not in kw and "dividers" not in kw and "history_alias" not in kw
    assert kw["plan"][0][1] == "01 Prerequisites"


def test_toolkit_without_the_dividers_feature_gets_no_run_or_dividers(home, monkeypatch):
    kw = recorded_session_kwargs(home, monkeypatch, features={"count"})
    assert "run" not in kw and "dividers" not in kw


def test_revert_plan_is_the_six_rows_with_no_dividers_or_run(home, monkeypatch):
    kw = recorded_session_kwargs(home, monkeypatch, features={"dividers", "history-alias"}, revert=True)
    assert [n for n, _ in kw["plan"]] == [n for n, _, _ in revert.ROWS]
    assert "run" not in kw and "dividers" not in kw and "history_alias" not in kw


def test_history_disabled_still_records_the_same_keywords(home, monkeypatch):
    monkeypatch.setenv("JUNEAU_TERMINAL_HISTORY", "0")
    kw = recorded_session_kwargs(home, monkeypatch, features={"dividers"})
    assert kw["run"] == 1 and len(kw["plan"]) == 25


# ---- gated Maven bar, count and history alias ----------------------------------------------------------------------


@pytest.mark.parametrize("step", [rc.build_verify, rc.deploy_snapshot])
def test_maven_steps_pass_bar_reactor_only_with_the_feature(make_ctx, step):
    for features, expected in ((frozenset({"reactor-bar"}), "reactor"), (frozenset(), None)):
        ctx = make_ctx(features=features)
        step(ctx)
        assert ctx.fake.calls[-1]["bar"] == expected


def test_release_perform_passes_bar_reactor_only_with_the_feature(make_ctx):
    from conftest import FakeNexus
    for features, expected in ((frozenset({"reactor-bar"}), "reactor"), (frozenset(), None)):
        ctx = make_ctx(features=features, nexus=FakeNexus(), state={"nexusRepoId": "r-1"})
        try:
            rc.release_perform(ctx)
        except Exception:   # the step's tail (finding the new staging repository) is not under test
            pass
        mvn = [c for c in ctx.fake.calls if c["argv"][:3] == ["mvn", "-B", "release:perform"]]
        assert mvn and mvn[0]["bar"] == expected


def test_javadoc_and_release_prepare_never_pass_a_bar(make_ctx):
    ctx = make_ctx(features=frozenset({"reactor-bar"}))
    ctx.ws.mkdir(parents=True, exist_ok=True)
    (ctx.ws / "pom.xml").write_text("<project><version>9.2.1-SNAPSHOT</version></project>")
    rc.javadoc_verify(ctx)
    rc.release_prepare(ctx)
    assert [c["bar"] for c in ctx.fake.calls if c["argv"][0] == "mvn"] == [None, None]


WANTED = ["juneau-9.2.1-source-release.zip", "juneau-9.2.1-source-release.zip.asc",
          "juneau-9.2.1-source-release.zip.sha512", "juneau-distrib-9.2.1-bin.zip",
          "juneau-distrib-9.2.1-bin.zip.asc", "juneau-distrib-9.2.1-bin.zip.sha512"]


def _staged(ctx):
    ctx.nexus.files = {n: f"https://repository.apache.org/content/repositories/r-1/org/apache/juneau/x/{n}"
                       for n in WANTED}


def test_dist_stage_counts_downloads_only_with_the_feature(make_ctx):
    ctx = make_ctx(state={"nexusRepoId": "r-1"}, features=frozenset({"count"}))
    _staged(ctx)
    rc.dist_stage(ctx)
    assert ctx.fake.counts == [(i, 6) for i in range(0, 7)]
    old = make_ctx(state={"nexusRepoId": "r-1"})
    _staged(old)
    rc.dist_stage(old)
    assert old.fake.counts == []


def test_dist_stage_dry_run_downloads_and_counts_nothing(make_ctx):
    ctx = make_ctx(dry_run=True, state={"nexusRepoId": rc.DRY_REPO}, features=frozenset({"count"}))
    rc.dist_stage(ctx)
    assert ctx.fake.counts == []


def test_history_alias_covers_steps_5_8_11_and_uses_the_expanded_checkout():
    deps = SimpleNamespace(run=SimpleNamespace(repo_key=lambda p: "key-of-" + p))
    alias = board.history_alias(deps, "master")
    assert set(alias) == {"build-verify", "deploy-snapshot", "release-perform"}
    key, script, step, tool = alias["build-verify"]
    assert (script, step, tool) == ("test", "tests", "maven")
    assert key.startswith("key-of-/") and "~" not in key


def test_history_alias_is_empty_when_the_toolkit_has_no_key():
    assert board.history_alias(SimpleNamespace(run=SimpleNamespace(repo_key=lambda p: None)), "master") == {}
    assert board.history_alias(SimpleNamespace(run=SimpleNamespace()), "master") == {}


def test_history_alias_is_empty_off_master():
    deps = SimpleNamespace(run=SimpleNamespace(repo_key=lambda p: "key"))
    assert board.history_alias(deps, "juneau-9.2.1-branch") == {}
    assert board.history_alias(deps, None) == {}


def test_history_alias_reaches_the_session_only_with_the_feature(home, monkeypatch):
    stub_key = lambda p: "key"   # noqa: E731
    monkeypatch.setattr(StubRun, "repo_key", staticmethod(stub_key), raising=False)
    monkeypatch.setattr(release.params, "load", lambda p: SimpleNamespace(version="10.0.0", details={"branch": "master"}))
    kw = recorded_session_kwargs(home, monkeypatch, features={"dividers", "history-alias"})
    assert set(kw["history_alias"]) == {"build-verify", "deploy-snapshot", "release-perform"}
    kw = recorded_session_kwargs(home, monkeypatch, features={"dividers"})
    assert "history_alias" not in kw


def test_history_alias_is_not_sent_for_a_release_branch(home, monkeypatch):
    monkeypatch.setattr(StubRun, "repo_key", staticmethod(lambda p: "key"), raising=False)
    monkeypatch.setattr(release.params, "load",
                        lambda p: SimpleNamespace(version="9.2.1", details={"branch": "juneau-9.2.1-branch"}))
    kw = recorded_session_kwargs(home, monkeypatch, features={"dividers", "history-alias"})
    assert "history_alias" not in kw


def test_ctx_carries_the_toolkit_features(home):
    deps = release.Deps(run=StubRun({"count"}), steps=SimpleNamespace(Pause=RuntimeError), lock=None)
    ctx = release.build_ctx(release.parse(["--home", str(home)]), deps, FakeRf())
    assert ctx.features == {"count"}


# ---- the remaining divider cases ----------------------------------------------------------------------------------


class StubRegistry:
    """The toolkit's Registry, as far as release.registry uses it: every step is registered, whatever --skip-step says."""

    def __init__(self):
        self.names, self._labels = [], {}

    def step(self, name, title, label=None):
        self.names.append(name)
        self._labels[name] = label
        return lambda fn: fn

    def checks(self, name, title, fn, label=None):
        self.step(name, title, label)


def test_skip_step_keeps_25_rows_and_25_plan_entries(home, monkeypatch):
    deps = release.Deps(run=FakeRunner(), steps=SimpleNamespace(Registry=StubRegistry), lock=None)
    reg = release.registry(deps, release.impl_map())
    assert len(reg.names) == 25
    assert reg._labels["build-verify"] == "05 Build verify"
    kw = recorded_session_kwargs(home, monkeypatch, features={"dividers"}, extra_argv=["--skip-step", "javadoc-verify"])
    assert len(kw["plan"]) == 25


def test_resume_with_a_vote_email_pending_passes_it_and_stops_at_the_vote_gate():
    n, dividers = board.divider_plan(args(resume=True), FakeRf(last="dist-verify", attempts=3, pending="vote"))
    assert n == 4
    assert states(dividers) == {"compose-propose-email": "passed", "compose-vote-email": "passed",
                                "vote-gate": "stops", "compose-result-email": "later",
                                "compose-announce-email": "later"}


def test_start_step_at_the_announce_email_passes_all_earlier_dividers():
    n, dividers = board.divider_plan(args(start_step="compose-announce-email"), FakeRf(attempts=5))
    assert states(dividers) == {"compose-propose-email": "passed", "compose-vote-email": "passed",
                                "vote-gate": "passed", "compose-result-email": "passed",
                                "compose-announce-email": "stops"}


def test_start_step_after_every_pause_has_no_stop():
    n, dividers = board.divider_plan(args(start_step="followup-checklist"), FakeRf(attempts=6))
    assert set(states(dividers).values()) == {"passed"}


def test_pause_dividers_name_real_steps():
    assert all(inventory.num_of(name) for name in inventory.PAUSE_DIVIDERS)
