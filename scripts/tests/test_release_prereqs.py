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

from jrm_release import gpgkeys, inventory, preflight, prereqs, rc
from jrm_release.ctx import StepError

pytestmark = pytest.mark.usefixtures("clean_env")

ALL = set(prereqs.TOOL_NAMES)


def have(monkeypatch, *names):
    monkeypatch.setattr(preflight.shutil, "which", lambda n: "/usr/bin/" + n if n in (*names, "gpgconf") else None)


def test_tool_names_cover_what_the_code_runs():
    assert {"git", "mvn", "java", "svn", "svnmucc", "gpg", "gh", "unzip"} <= ALL


def test_all_present_passes_and_runs_no_prompt(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("svn", "--version", out="1.14.3\n")
    ctx.fake.script("svn", "ls", out="juneau-9.2.0/\n")
    assert prereqs.check(ctx) == []
    assert ctx.fake.ran("svn", "ls", "--non-interactive", "--no-auth-cache", inventory.DIST_DEV + "/")


def test_missing_tools_reported_all_at_once_with_hints(make_ctx, monkeypatch):
    have(monkeypatch, "git", "mvn", "java", "gpg", "unzip")
    ctx = make_ctx(with_details=False)
    failed = prereqs.check(ctx)
    text = "\n".join(failed)
    assert "svn" in text
    assert "svnmucc" in text
    assert "gh" in text
    assert "brew install subversion" in text
    assert "brew install gh" in text
    assert len([f for f in failed if f.startswith(("svn:", "svnmucc:", "gh:"))]) == 3
    assert ctx.fake.ran("svn", "ls") == []        # no svn, so no anonymous listing to try


def test_old_svn_fails_with_minimum(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("svn", "--version", out="1.9.7\n")
    ctx.fake.script("svn", "ls", out="x/\n")
    failed = prereqs.check(ctx)
    assert len(failed) == 1
    assert failed[0].startswith("svn:")
    assert "1.10" in failed[0]
    assert "1.9.7" in failed[0]


def test_unparseable_version_fails_naming_tool_and_output(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("svn", "--version", out="garbled nonsense\n")
    ctx.fake.script("svn", "ls", out="x/\n")
    failed = prereqs.check(ctx)
    assert len(failed) == 1
    assert failed[0].startswith("svn:")
    assert "garbled nonsense" in failed[0]


@pytest.mark.parametrize("tool,argv", [("mvn", ("mvn", "-version")), ("gpg", ("gpg", "--version")),
                                       ("gh", ("gh", "--version")), ("svnmucc", ("svnmucc", "--version"))])
def test_unparseable_output_fails_for_every_versioned_tool(make_ctx, monkeypatch, tool, argv):
    have(monkeypatch, *ALL)
    ctx = make_ctx(with_details=False)
    ctx.fake.script(*argv, out="no digits here\n")
    ctx.fake.script("svn", "ls", out="x/\n")
    failed = prereqs.check(ctx)
    assert len(failed) == 1
    assert failed[0].startswith(tool + ":")
    assert "no digits here" in failed[0]


BANNER = "Some vendor banner line\nFind more information at example.invalid\n"


def test_java_scans_past_a_banner_for_the_version_line(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    monkeypatch.delenv("JAVA_HOME", raising=False)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("java", "-version", out=BANNER + 'openjdk version "17.0.9" 2023-10-17\nOpenJDK Runtime\n')
    ctx.fake.script("svn", "ls", out="x/\n")
    assert prereqs.check(ctx) == []


def test_java_old_behind_a_banner_fails(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    monkeypatch.delenv("JAVA_HOME", raising=False)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("java", "-version", out=BANNER + 'java version "1.8.0_392"\n')
    ctx.fake.script("svn", "ls", out="x/\n")
    failed = prereqs.check(ctx)
    assert len(failed) == 1
    assert failed[0].startswith("java:")
    assert "17" in failed[0]
    assert "1.8.0_392" in failed[0]


def test_java_without_a_version_line_fails_naming_binary_and_first_line(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    monkeypatch.delenv("JAVA_HOME", raising=False)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("java", "-version", out=BANNER)
    ctx.fake.script("svn", "ls", out="x/\n")
    failed = prereqs.check(ctx)
    assert len(failed) == 1
    assert "java" in failed[0]
    assert "Some vendor banner line" in failed[0]


def test_java_home_binary_wins_and_is_reported(make_ctx, monkeypatch, tmp_path):
    have(monkeypatch, *ALL)
    jh = tmp_path / "jdk"
    (jh / "bin").mkdir(parents=True)
    exe = jh / "bin" / "java"
    exe.write_text("")
    exe.chmod(0o755)
    monkeypatch.setenv("JAVA_HOME", str(jh))
    ctx = make_ctx(with_details=False)
    ctx.fake.script(str(exe), "-version", out='openjdk version "21.0.1"\n')
    ctx.fake.script("svn", "ls", out="x/\n")
    assert prereqs.check(ctx) == []
    assert ctx.fake.ran(str(exe), "-version")
    assert not ctx.fake.ran("java", "-version")
    assert any(str(exe) in s and "21" in s for s in ctx.fake.said)


def test_java_home_without_a_binary_falls_back_to_path(make_ctx, monkeypatch, tmp_path):
    have(monkeypatch, *ALL)
    monkeypatch.setenv("JAVA_HOME", str(tmp_path / "missing"))
    ctx = make_ctx(with_details=False)
    ctx.fake.script("java", "-version", out='openjdk version "17.0.9"\n')
    ctx.fake.script("svn", "ls", out="x/\n")
    assert prereqs.check(ctx) == []
    assert ctx.fake.ran("java", "-version")


def test_parse_java_major():
    assert prereqs.java_major('java version "1.8.0_392"') == 8
    assert prereqs.java_major('openjdk version "17.0.9" 2023') == 17
    assert prereqs.java_major('openjdk version "21"') == 21
    assert prereqs.java_major("nothing") is None


def test_old_java_fails(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    monkeypatch.delenv("JAVA_HOME", raising=False)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("java", "-version", out='openjdk version "11.0.2" 2019-01-15\n')
    ctx.fake.script("svn", "ls", out="x/\n")
    failed = prereqs.check(ctx)
    assert len(failed) == 1
    assert failed[0].startswith("java:")
    assert "17" in failed[0]


def test_unreachable_nexus_and_dist_dev(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    ctx = make_ctx(with_details=False, http_ok=lambda url: False)
    ctx.fake.script("svn", "ls", ok=False)
    failed = prereqs.check(ctx)
    assert any(f.startswith("nexus reachable") for f in failed)
    assert any(f.startswith("dist/dev reachable") for f in failed)


def test_nexus_probe_needs_no_credentials(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    seen = []
    ctx = make_ctx(with_details=False, http_ok=lambda url: seen.append(url) or True)
    ctx.fake.script("svn", "ls", out="x/\n")
    ctx.nexus = None                  # the authenticated client must not be touched
    assert prereqs.check(ctx) == []
    assert seen == [inventory.NEXUS_URL]


def test_runs_in_a_dry_run(make_ctx, monkeypatch):
    have(monkeypatch, "git")
    assert prereqs.check(make_ctx(dry_run=True, with_details=False))


def test_step_raises_with_every_failure(make_ctx, monkeypatch):
    have(monkeypatch, "git")
    ctx = make_ctx(with_details=False)
    with pytest.raises(StepError) as e:
        rc.FUNCS["prerequisites"](ctx)
    assert "svn" in str(e.value)
    assert "gh" in str(e.value)


def test_step_reuses_the_early_result(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("svn", "ls", out="x/\n")
    prereqs.check(ctx)
    n = len(ctx.fake.calls)
    assert "met" in rc.FUNCS["prerequisites"](ctx)
    assert len(ctx.fake.calls) == n               # nothing ran twice


def sec(keyid, fpr, uids, expires="", caps="scESC", validity="u"):
    rows = [f"sec:{validity}:4096:1:{keyid}:1500000000:{expires}::u:::{caps}:::+:::23::0:",
            f"fpr:::::::::{fpr}:"]
    rows += [f"uid:{validity}::::1500000000::HASH{i}::{u}::::::::::0:" for i, u in enumerate(uids)]
    rows += ["ssb:u:4096:1:SUBKEY0000000001:1500000000::::::e:::+:::23:"]
    return "\n".join(rows) + "\n"


MINE = sec("36292BD2BA7D3A86", "A" * 24 + "36292BD2BA7D3A86", ["James Bognar <jamesbognar@apache.org>"])
OTHER = sec("1111111111111111", "B" * 40, ["Someone <someone@example.com>"])
NOW = 1_800_000_000


def candidates(out, email="jamesbognar@apache.org"):
    return gpgkeys.candidates(gpgkeys.parse(out), email, NOW)


def test_one_match_gives_the_long_id():
    assert [k.keyid for k in candidates(OTHER + MINE)] == ["36292BD2BA7D3A86"]


def test_match_is_case_insensitive_on_email():
    assert candidates(MINE, "JamesBognar@Apache.org")


def test_no_match():
    assert candidates(OTHER) == []


def test_expired_key_is_skipped():
    assert candidates(sec("2222222222222222", "C" * 40, ["J <jamesbognar@apache.org>"], expires="1700000000")) == []


def test_unexpired_dated_key_is_kept():
    assert candidates(sec("2222222222222222", "C" * 40, ["J <jamesbognar@apache.org>"], expires="1900000000"))


def test_revoked_key_is_skipped():
    assert candidates(sec("2222222222222222", "C" * 40, ["J <jamesbognar@apache.org>"], validity="r")) == []


def test_non_signing_key_is_skipped():
    assert candidates(sec("2222222222222222", "C" * 40, ["J <jamesbognar@apache.org>"], caps="e")) == []


def test_several_matches():
    two = sec("3333333333333333", "D" * 40, ["J <jamesbognar@apache.org>"])
    assert [k.keyid for k in candidates(MINE + two)] == ["36292BD2BA7D3A86", "3333333333333333"]


# ---- quiet capture ------------------------------------------------------------------------------------------

def test_branch_pom_is_read_quietly(make_ctx):
    from jrm_release import details
    ctx = make_ctx(with_details=False)
    ctx.fake.script("git", "-C", details.checkout(), "show", out="<version>9.2.1-SNAPSHOT</version>")
    details.pom_defaults(ctx, "x")
    assert ctx.fake.calls[-1]["quiet"] is True


def test_runner_merge_err_appends_stderr():
    from types import SimpleNamespace
    from jrm_release.runner import Runner
    mod = SimpleNamespace(cmd=lambda argv, **kw: SimpleNamespace(ok=True, code=0, out="o", err="e"))
    assert Runner(mod).run(["x"], title="t", mutating=False).out == "o"
    assert Runner(mod).run(["x"], title="t", mutating=False, merge_err=True).out == "oe"


def test_svnmucc_version_uses_no_quiet_flag_and_parses_its_banner(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("svnmucc", "--version", out="svnmucc, version 1.15.0 (r1937767)\n   compiled Jan  1 2026\n")
    ctx.fake.script("svn", "ls", out="x/\n")
    assert prereqs.check(ctx) == []
    assert ctx.fake.ran("svnmucc", "--version") == [["svnmucc", "--version"]]


# ---- PyYAML ------------------------------------------------------------------------------

PYYAML_MISSING = "PyYAML: not installed; params files need it (python3 -m pip install pyyaml)"


def no_yaml(monkeypatch):
    real = preflight.importlib.util.find_spec
    monkeypatch.setattr(preflight.importlib.util, "find_spec", lambda name, *a: None if name == "yaml" else real(name, *a))


def test_missing_pyyaml_is_listed_with_the_other_failures(make_ctx, monkeypatch):
    have(monkeypatch, *(ALL - {"gh"}))
    no_yaml(monkeypatch)
    ctx = make_ctx(with_details=False)
    failed = prereqs.check(ctx)
    assert PYYAML_MISSING in failed
    assert any(f.startswith("gh:") for f in failed)


def test_pyyaml_is_always_checked(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    no_yaml(monkeypatch)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("svn", "ls", out="x/\n")
    assert prereqs.check(ctx) == [PYYAML_MISSING]
    assert "PyYAML" in ctx.prereq_verdicts


def test_pyyaml_present_passes(make_ctx, monkeypatch):
    have(monkeypatch, *ALL)
    ctx = make_ctx(with_details=False)
    ctx.fake.script("svn", "ls", out="x/\n")
    assert PYYAML_MISSING not in prereqs.check(ctx)
