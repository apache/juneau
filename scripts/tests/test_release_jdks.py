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
"""The multi-JDK gate: resolution, the jdk-verify step, the --check row, the params keys and the vote email line."""
import os

import pytest

from jrm_release import emails, jdks, preflight, rc
from jrm_release.ctx import StepError
from jrm_release.params import ParamsError
from jrm_release.preflight import FAIL, PASS

pytestmark = pytest.mark.usefixtures("clean_env")


def make_jdk(path, bundle=False):
    home = path / "Contents" / "Home" if bundle else path
    (home / "bin").mkdir(parents=True)
    (home / "bin" / "java").write_text("#!/bin/sh\n")
    return home


@pytest.fixture
def root(tmp_path, monkeypatch):
    r = tmp_path / "jdk"
    r.mkdir()
    monkeypatch.setattr(jdks, "JDK_ROOT", str(r))
    return r


# ---- resolve -------------------------------------------------------------------------------------------------------

def test_a_number_resolves_to_jdk_dash_n_first(root):
    make_jdk(root / "jdk-28")
    make_jdk(root / "other-28-thing")
    assert jdks.resolve(28) == str(root / "jdk-28")


def test_a_number_falls_back_to_the_single_glob_match_and_normalises_a_bundle(root):
    make_jdk(root / "sfdc-jdk-zulu-25.0.1.jdk", bundle=True)
    assert jdks.resolve("25") == str(root / "sfdc-jdk-zulu-25.0.1.jdk" / "Contents" / "Home")


def test_an_ambiguous_or_missing_number_is_an_error(root):
    make_jdk(root / "a-17-x")
    make_jdk(root / "b-17-y")
    with pytest.raises(jdks.JdkError, match="ambiguous"):
        jdks.resolve(17)
    with pytest.raises(jdks.JdkError, match="nothing matching"):
        jdks.resolve(99)


def test_a_path_is_used_as_given_and_must_be_a_jdk(tmp_path, root):
    home = make_jdk(tmp_path / "elsewhere" / "x.jdk", bundle=True)
    assert jdks.resolve(str(tmp_path / "elsewhere" / "x.jdk")) == str(home)
    (tmp_path / "empty").mkdir()
    with pytest.raises(jdks.JdkError, match="not a JDK home"):
        jdks.resolve(str(tmp_path / "empty"))
    with pytest.raises(jdks.JdkError, match="no such directory"):
        jdks.resolve(str(tmp_path / "nope"))


# ---- the step ------------------------------------------------------------------------------------------------------

def ctx_with(make_ctx, root, required=(17, 21), experimental=(28), **kw):
    for n in {*required, *([experimental] if experimental else [])}:
        make_jdk(root / f"jdk-{n}")
    ctx = make_ctx(**kw)
    ctx.rf.set("details", {**ctx.d, "jdks": [str(j) for j in required],
                           "jdksExperimental": [str(experimental)] if experimental else []})
    return ctx


def mvn_calls(ctx):
    return [c for c in ctx.fake.calls if c["argv"][:1] == ["mvn"]]


def test_each_jdk_runs_mvn_verify_with_its_own_java_home(make_ctx, root):
    ctx = ctx_with(make_ctx, root)
    note = rc.jdk_verify(ctx)
    calls = mvn_calls(ctx)
    assert [c["argv"] for c in calls] == [["mvn", "-B", "clean", "verify"]] * 3
    assert [c["env"]["JAVA_HOME"] for c in calls] == [str(root / f"jdk-{n}") for n in (17, 21, 28)]
    assert all(c["env"]["PATH"].startswith(c["env"]["JAVA_HOME"] + os.sep + "bin") for c in calls)
    assert all(c["cwd"] == ctx.ws for c in calls)
    assert "verified on JDK 17, 21, 28" in note
    assert {k: v["status"] for k, v in ctx.rf.get("jdkResults").items()} == {"17": "pass", "21": "pass", "28": "pass"}


def test_a_required_failure_fails_the_step_after_running_the_rest(make_ctx, root):
    ctx = ctx_with(make_ctx, root)
    ctx.fake.script("mvn", ok=False)
    with pytest.raises(StepError, match="verify failed on JDK 17, 21"):
        rc.jdk_verify(ctx)
    assert len(mvn_calls(ctx)) == 3
    assert ctx.rf.get("jdkResults")["17"]["status"] == "fail"


def test_an_experimental_failure_is_a_warning_in_the_result(make_ctx, root):
    ctx = ctx_with(make_ctx, root)
    ctx.fake.script("mvn", "-B", "clean", "verify", out="")
    calls = []
    real = ctx.fake.cmd

    def cmd(argv, **kw):
        out = real(argv, **kw)
        calls.append(kw.get("env", {}).get("JAVA_HOME"))
        if kw.get("env", {}).get("JAVA_HOME", "").endswith("jdk-28"):
            return type(out)(False, 1, "")
        return out
    ctx.fake.cmd = cmd
    note = rc.jdk_verify(ctx)
    assert "WARNING: experimental JDK 28 failed" in note
    assert ctx.rf.get("jdkResults")["28"]["status"] == "warn"
    assert any("experimental JDK 28 failed" in s for s in ctx.fake.said)


def test_resume_reruns_only_the_jdks_that_did_not_pass(make_ctx, root):
    ctx = ctx_with(make_ctx, root, state={"jdkResults": {"17": {"status": "pass", "required": True}}})
    rc.jdk_verify(ctx)
    assert [c["env"]["JAVA_HOME"] for c in mvn_calls(ctx)] == [str(root / "jdk-21"), str(root / "jdk-28")]
    assert ctx.rf.get("jdkResults")["17"]["status"] == "pass"


def test_an_unresolvable_jdk_fails_before_any_build(make_ctx, root):
    ctx = ctx_with(make_ctx, root, required=(17,), experimental=None)
    ctx.rf.set("details", {**ctx.d, "jdks": ["17", "99"]})
    with pytest.raises(StepError, match="JDK 99"):
        rc.jdk_verify(ctx)


def test_no_jdks_listed_is_a_step_error(make_ctx):
    with pytest.raises(StepError, match="no jdks"):
        rc.jdk_verify(make_ctx())


def test_dry_run_runs_no_maven(make_ctx, root):
    ctx = ctx_with(make_ctx, root, dry_run=True)
    note = rc.jdk_verify(ctx)
    assert not mvn_calls(ctx) and note.startswith("skipped (dry run)")


# ---- --check -------------------------------------------------------------------------------------------------------

def check_row(ctx):
    return {n: r for g, n, r in preflight.run_checks(ctx, ("params",))}["JDKs resolve"]


def test_check_passes_when_every_jdk_resolves(make_ctx, root):
    ctx = ctx_with(make_ctx, root)
    got = check_row(ctx)
    assert got.status == PASS and "28 (experimental)" in got.reason


def test_check_fails_naming_the_jdk_and_the_reason(make_ctx, root):
    ctx = ctx_with(make_ctx, root, required=(17,), experimental=None)
    ctx.rf.set("details", {**ctx.d, "jdks": ["17", "99"]})
    got = check_row(ctx)
    assert got.status == FAIL and "99" in got.reason and "nothing matching" in got.reason


# ---- params and the email ------------------------------------------------------------------------------------------

def test_params_require_jdks_and_accept_experimental(tmp_path):
    pytest.importorskip("yaml")
    from jrm_release import params
    base = "version: 9.2.1\nrc: 1\nsummary: s\napache: {availid: jb}\ngpg: {keyid: A}\n"
    p = tmp_path / "p.yml"
    p.write_text(base)
    with pytest.raises(ParamsError, match="missing required key.*jdks"):
        params.load(p)
    p.write_text(base + "jdks: [17, 21]\njdks_experimental: [28, 28-ea]\n")
    loaded = params.load(p)
    assert loaded.details["jdks"] == ["17", "21"] and loaded.details["jdksExperimental"] == ["28", "28-ea"]
    p.write_text(base + "jdks: 17\n")
    with pytest.raises(ParamsError, match="'jdks' must be a list"):
        params.load(p)


def test_init_params_template_lists_the_defaults(tmp_path):
    pytest.importorskip("yaml")
    import yaml
    from jrm_release import params
    doc = yaml.safe_load(params.render_scaffold("9.2.1", "b", "9.2.2-SNAPSHOT", 1, "a@apache.org", "a", "K"))
    assert doc["jdks"] == [17, 21, 25, 27] and doc["jdks_experimental"] == [28]


def test_tested_line_lists_passing_jdks():
    results = {"17": {"status": "pass", "required": True}, "21": {"status": "fail", "required": True},
               "28": {"status": "pass", "required": False}}
    assert jdks.tested_line(results) == "Tested on JDKs: 17, 28 (experimental)"
    assert jdks.tested_line({}) == ""


def test_vote_email_carries_the_tested_line_only_when_there_is_one():
    d = dict(version="9.2.1", rc=1, branch="b", summary="s", highlights="", knownIssues="", acknowledgements="")
    facts = dict(src_sha="a", bin_sha="b", commit="c", repo="r", deadline="d")
    assert "Tested on" not in emails.vote(d, facts)[2]
    body = emails.vote(d, {**facts, "jdks": "Tested on JDKs: 17, 21"})[2]
    assert "Tested on JDKs: 17, 21\n\nBuild the release candidate using:" in body
