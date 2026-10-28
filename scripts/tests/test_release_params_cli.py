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
"""release.py --params / --init-params end to end through main(): lookup order, flag conflicts, startup secret check,
persisted identity and the record copy.  Needs the juneau-terminal toolkit (JUNEAU_TERMINAL_PY), like test_release_cli."""
import json
import os
import types

import pytest

yaml = pytest.importorskip("yaml")

import release  # noqa: E402
from test_release_cli import run_main, seed, toolkit_deps  # noqa: E402
from test_release_params import GOOD, write  # noqa: E402

pytestmark = [pytest.mark.usefixtures("clean_env"),
              pytest.mark.skipif(not os.environ.get("JUNEAU_TERMINAL_PY"), reason="needs JUNEAU_TERMINAL_PY toolkit")]

SECRET_ENV = {"JRM_APACHE_PASSWORD": "env-pw-9", "JRM_GPG_PASSPHRASE": "env-gpg-9", "JRM_GITHUB_TOKEN": "env-gh-9"}


def setup(tmp_path, monkeypatch, env=SECRET_ENV, platform="linux"):
    """(fake runner, deps) for a new release.  The keychain is off (platform) unless a test turns it on."""
    from conftest import FakeNexus, FakeRunner
    from jrm_release import keychain, preflight
    monkeypatch.setattr(preflight.shutil, "which", lambda n: "/usr/bin/" + n)
    monkeypatch.setattr(keychain.sys, "platform", platform)
    for k, v in env.items():
        monkeypatch.setenv(k, v)
    fake = FakeRunner()
    fake.script("git", "ls-remote", release.inventory.GIT_URL, "refs/heads/juneau-9.2.1-branch", out="abc\trefs/heads/x\n")
    fake.script("gh", "auth", "token", ok=False, code=1)
    return fake, toolkit_deps(tmp_path, fake, nexus=FakeNexus(), http_ok=lambda u: True, sleep=lambda s: None)


def run_json(home, version="9.2.1"):
    return json.loads((home / "runs" / version / "run.json").read_text())


IDENT = {"apacheId": "jbognar", "apacheEmail": "jamesbognar@apache.org", "gpgKeyId": "ABCD1234ABCD1234"}
GH_AUTH = ("gh", "auth", "token")
def convention(home, text=GOOD, version="9.2.1"):
    p = home / "params" / f"{version}.yml"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text)
    return p


def finish(home):
    """Mark the run released, so a next release of the same version may start."""
    path = home / "runs" / "9.2.1" / "run.json"
    data = json.loads(path.read_text())
    path.write_text(json.dumps({**data, "phase": "released"}))


# 1, 25, 20 -----------------------------------------------------------------------------------------------------------

def test_params_flag_starts_without_any_prompt_and_says_so_first(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch)
    p = write(tmp_path, GOOD)
    code, out = run_main(["--params", str(p)], home, capsys, deps=deps)
    assert code == 0, out
    assert "Send the [PROPOSE] email" in out and "--resume --version 9.2.1" in out
    assert run_json(home)["emailPending"] == "propose" and run_json(home)["attempts"][-1]["status"] == "paused"
    assert out.splitlines()[0] == f"Using params file {p}"
    assert out.index("Using params file") < out.index("✔ git")
    data = run_json(home)
    assert data["details"] == {"branch": "juneau-9.2.1-branch", "version": "9.2.1", "developmentVersion": "9.2.2-SNAPSHOT",
                               "rc": 1, "summary": "Security and maintenance release.",
                               "highlights": "- DirectoryResource hardened.", "knownIssues": "", "acknowledgements": ""}
    assert data["identity"] == IDENT
    assert (home / "runs" / "9.2.1" / "params.yml").read_bytes() == p.read_bytes()
    assert data["paramsPath"] == str(p.resolve())            # the live file the vote tally is read from


def test_secrets_from_env_reach_neither_run_json_nor_the_record_nor_logs_nor_argv(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch)
    code, out = run_main(["--params", str(write(tmp_path, GOOD))], home, capsys, deps=deps)
    run_dir = home / "runs" / "9.2.1"
    blob = (out + "".join(f.read_text() for f in run_dir.iterdir() if f.is_file())
            + (home / "attempts").joinpath(*[]).as_posix() + "".join(p.read_text() for p in (home / "attempts").rglob("*.log"))
            + json.dumps([c["argv"] for c in fake.calls]) + "\n".join(fake.said))
    for secret in SECRET_ENV.values():
        assert secret not in blob


def test_convention_file_is_picked_up_by_version(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch)
    conv = convention(home)
    code, out = run_main(["--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 0, out
    assert run_json(home)["emailPending"] == "propose"
    assert out.splitlines()[0] == f"Using params file {conv}"
    assert run_json(home)["identity"] == IDENT


def test_without_a_file_there_is_no_form_and_no_prompt(home, capsys, tmp_path, monkeypatch):
    for argv in ([], ["--version", "9.2.1"]):
        fake, deps = setup(tmp_path, monkeypatch)
        code, out = run_main(argv, home, capsys, deps=deps)
        assert code == 2
        assert "--init-params" in out
    assert not (home / "runs").exists()


# 26, 27 --------------------------------------------------------------------------------------------------------------

def test_explicit_params_beat_the_convention_file(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch)
    convention(home, GOOD.replace("Security and maintenance release.", "from the convention file"))
    flag = write(tmp_path, GOOD.replace("Security and maintenance release.", "from the flag file"))
    code, out = run_main(["--params", str(flag), "--version", "9.2.1"], home, capsys, deps=deps)
    assert out.splitlines()[0] == f"Using params file {flag}"
    assert run_json(home)["details"]["summary"] == "from the flag file"


def test_version_that_disagrees_with_the_file_exits_2(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch)
    flag = write(tmp_path, GOOD)
    code, out = run_main(["--params", str(flag), "--version", "9.2.2"], home, capsys, deps=deps)
    assert code == 2
    assert f"--version 9.2.2 does not match version 9.2.1 in {flag}" in out
    assert not (home / "runs").exists()


@pytest.mark.parametrize("text, fragment", [
    ("version: [9.2.1\n", "not valid YAML"),
    (GOOD + "knownIssue: x\n", "unknown key 'knownIssue'"),
    (GOOD.replace("apache: {", "apache: {password: hunter2, "), "'apache.password' is a secret"),
])
def test_invalid_convention_file_is_an_error_never_a_fallback(home, capsys, tmp_path, monkeypatch, text, fragment):
    fake, deps = setup(tmp_path, monkeypatch)
    conv = convention(home, text)
    code, out = run_main(["--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 2, out
    assert fragment in out and str(conv) in out
    assert "hunter2" not in out
    assert not (home / "runs").exists()


def test_missing_params_file_is_exit_2(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch)
    code, out = run_main(["--params", str(tmp_path / "nope.yml")], home, capsys, deps=deps)
    assert code == 2 and "cannot read" in out


def test_todo_placeholder_exits_2(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch)
    code, out = run_main(["--params", str(write(tmp_path, GOOD.replace("jbognar", "TODO"))), ], home, capsys, deps=deps)
    assert code == 2 and "'apache.availid' still contains a TODO placeholder" in out


def test_params_refused_while_a_run_is_active(home, capsys, tmp_path, monkeypatch):
    seed(home, {"phase": "rc", "last": "workspace", "details": {"version": "9.2.1", "rc": 1}})
    code, out = run_main(["--params", str(write(tmp_path, GOOD))], home, capsys)
    assert code == 2 and "--resume" in out


# 6 -------------------------------------------------------------------------------------------------------------------

@pytest.mark.parametrize("flag", [["--resume"], ["--revert"], ["--start-step", "workspace"], ["--list-steps"]])
def test_params_cannot_be_combined_with_run_control_flags(home, capsys, tmp_path, flag):
    code, out = run_main(["--params", str(tmp_path / "f.yml"), *flag], home, capsys)
    assert code == 2
    assert "--params cannot be combined with --resume, --revert, --start-step or --list-steps" in out


def test_params_allows_skip_step_and_dry_run():
    args = release.parse(["--params", "f.yml", "--skip-step", "javadoc-verify", "--dry-run"])
    assert args.skip_step == ["javadoc-verify"] and args.dry_run


# 24 init-params ------------------------------------------------------------------------------------------------------

def init_deps(*, email="jamesbognar@apache.org\n"):
    from conftest import FakeRunner
    fake = FakeRunner()
    fake.script("git", "config", "user.email", out=email)
    fake.script("gpg", "--list-secret-keys", out="")
    return fake, types.SimpleNamespace(run=fake)


def test_init_params_writes_the_template_and_exits_0(home, capsys):
    fake, deps = init_deps()
    code, out = run_main(["--init-params", "--version", "9.2.1"], home, capsys, deps=deps)
    target = home / "params" / "9.2.1.yml"
    assert code == 0 and target.is_file()
    assert f"Wrote {target}." in out and "python3 scripts/release.py --version 9.2.1" in out
    assert yaml.safe_load(target.read_text())["version"] == "9.2.1"


def test_init_params_honors_params_file_and_refuses_to_overwrite(home, capsys, tmp_path):
    fake, deps = init_deps()
    target = tmp_path / "mine.yml"
    code, out = run_main(["--init-params", "--version", "9.2.1", "--params", str(target)], home, capsys, deps=deps)
    assert code == 0 and target.is_file() and not (home / "params").exists()
    before = target.read_bytes()
    code, out = run_main(["--init-params", "--version", "9.2.1", "--params", str(target)], home, capsys, deps=deps)
    assert code == 2
    assert f"{target} already exists; not overwriting (edit it, or delete it to start over)" in out
    assert target.read_bytes() == before


def test_init_params_needs_version(home, capsys):
    code, out = run_main(["--init-params"], home, capsys)
    assert code == 2 and "--init-params needs --version" in out


@pytest.mark.parametrize("flag", [["--resume"], ["--revert"], ["--start-step", "workspace"], ["--list-steps"], ["--dry-run"]])
def test_init_params_conflicts(home, capsys, flag):
    code, out = run_main(["--init-params", "--version", "9.2.1", *flag], home, capsys)
    assert code == 2
    assert "--init-params cannot be combined with --resume, --revert, --start-step, --list-steps or --dry-run" in out
    assert not (home / "params").exists()


def test_init_params_needs_no_toolkit_session_or_idle_state(home, capsys):
    seed(home, {"phase": "rc", "last": "workspace", "details": {"version": "9.2.1", "rc": 1}})   # a run is active
    fake, deps = init_deps()
    code, out = run_main(["--init-params", "--version", "9.2.2"], home, capsys, deps=deps)
    assert code == 0, out


# 7, 17 secrets at startup --------------------------------------------------------------------------------------------

def test_no_secrets_found_lists_all_three_and_changes_nothing(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch, env={})
    code, out = run_main(["--params", str(write(tmp_path, GOOD))], home, capsys, deps=deps)
    assert code == 2
    assert "nothing was asked or changed" in out
    for name in ("Apache password present", "GPG passphrase present", "GitHub token present"):
        assert name in out
    assert "JRM_APACHE_PASSWORD" in out and "JRM_GPG_PASSPHRASE" in out and "JRM_GITHUB_TOKEN" in out
    assert not (home / "runs").exists()


def test_empty_env_string_counts_as_unset_and_found_ones_are_not_listed(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch, env={"JRM_APACHE_PASSWORD": "pw", "JRM_GPG_PASSPHRASE": ""})
    code, out = run_main(["--params", str(write(tmp_path, GOOD))], home, capsys, deps=deps)
    assert code == 2
    assert "GPG passphrase present: not found" in out and "GitHub token present: not found" in out
    assert "Apache password present: not found" not in out


def test_partially_populated_keychain_lists_exactly_the_missing_two(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch, env={}, platform="darwin")
    fake.script("security", "find-generic-password", ok=False, code=44)
    fake.script("security", "find-generic-password", "-s", "jrm.apache.password", "-a", "jbognar", "-w", out="kc-pw\n")
    code, out = run_main(["--params", str(write(tmp_path, GOOD))], home, capsys, deps=deps)
    assert code == 2
    assert "GPG passphrase present: not found" in out and "GitHub token present: not found" in out
    assert "Apache password present: not found" not in out
    assert "kc-pw" not in out and not (home / "runs").exists()


def test_token_found_only_via_gh_counts_and_everything_found_proceeds(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch, env={"JRM_APACHE_PASSWORD": "pw", "JRM_GPG_PASSPHRASE": "gp"})
    fake.script(*GH_AUTH, out="gh-found-token\n")
    code, out = run_main(["--params", str(write(tmp_path, GOOD))], home, capsys, deps=deps)
    assert code == 0, out
    assert run_json(home)["emailPending"] == "propose"
    assert "gh-found-token" not in out + json.dumps([c["argv"] for c in fake.calls])


def test_missing_availid_is_the_missing_required_key_error(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch)
    code, out = run_main(["--params", str(write(tmp_path, GOOD.replace("jbognar", "TODO").replace("availid: TODO,", "")))],
                         home, capsys, deps=deps)
    assert code == 2 and "missing required key(s): apache.availid" in out


# 11, 30 dry run ------------------------------------------------------------------------------------------------------

def test_dry_run_with_params_validates_but_touches_no_secret_and_writes_nothing(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch, platform="darwin")
    code, out = run_main(["--dry-run", "--params", str(write(tmp_path, GOOD))], home, capsys, deps=deps)
    assert fake.ran("security") == [] and fake.ran(*GH_AUTH) == []
    assert not (home / "runs").exists() and not (home / "runs" / "9.2.1" / "params.yml").exists()
    for secret in SECRET_ENV.values():
        assert secret not in out


def test_dry_run_still_rejects_an_invalid_file(home, capsys, tmp_path, monkeypatch):
    fake, deps = setup(tmp_path, monkeypatch)
    code, out = run_main(["--dry-run", "--params", str(write(tmp_path, GOOD + "bogus: 1\n"))], home, capsys, deps=deps)
    assert code == 2 and "unknown key 'bogus'" in out


def test_resume_never_reads_a_params_file(home, capsys, tmp_path, monkeypatch):
    from test_release_cli import ACTIVE
    seed(home, {**ACTIVE, "last": "dist-verify", "identity": IDENT})
    convention(home, "this is not even yaml: [\n")
    fake, deps = setup(tmp_path, monkeypatch)
    code, out = run_main(["--resume", "--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 0, out                                    # no startup secret failure on resume
    assert "Using params file" not in out


# 12 PyYAML -----------------------------------------------------------------------------------------------------------

def test_missing_pyyaml_is_a_preflight_failure_exit_2(home, capsys, tmp_path, monkeypatch):
    from jrm_release import preflight
    fake, deps = setup(tmp_path, monkeypatch)
    real = preflight.importlib.util.find_spec
    monkeypatch.setattr(preflight.importlib.util, "find_spec", lambda name, *a: None if name == "yaml" else real(name, *a))
    monkeypatch.setattr(preflight.shutil, "which", lambda n: None if n == "gh" else "/usr/bin/" + n)
    code, out = run_main(["--params", str(write(tmp_path, GOOD))], home, capsys, deps=deps)
    assert code == 2
    assert "2 check(s) did not pass" in out
    assert "PyYAML: not installed; params files need it (python3 -m pip install pyyaml)" in out
    assert "gh: not on PATH" in out
    assert not (home / "runs").exists()


# 29 record copy ------------------------------------------------------------------------------------------------------

def test_a_second_release_overwrites_the_record_and_the_record_can_be_reused(home, capsys, tmp_path, monkeypatch):
    first = write(tmp_path, GOOD, "one.yml")
    second = write(tmp_path, GOOD.replace("rc: 1", "rc: 2"), "two.yml")
    record = home / "runs" / "9.2.1" / "params.yml"
    fake, deps = setup(tmp_path, monkeypatch)
    run_main(["--params", str(first)], home, capsys, deps=deps)
    assert record.read_bytes() == first.read_bytes()
    finish(home)
    run_main(["--params", str(second)], home, capsys, deps=deps)
    assert record.read_bytes() == second.read_bytes() and run_json(home)["details"]["rc"] == 2
    finish(home)
    code, out = run_main(["--params", str(record)], home, capsys, deps=deps)        # the record is the source: skip
    assert "could not record" not in out and record.read_bytes() == second.read_bytes()
    assert run_json(home)["details"]["rc"] == 2


def test_a_record_copy_failure_warns_and_the_release_continues(home, capsys, tmp_path, monkeypatch):
    from jrm_release import params as params_mod
    real = os.replace

    def failing(src, dst, *a, **k):
        if str(dst).endswith("params.yml"):
            raise PermissionError(13, "Permission denied")
        return real(src, dst, *a, **k)

    fake, deps = setup(tmp_path, monkeypatch)
    monkeypatch.setattr(params_mod.os, "replace", failing)
    code, out = run_main(["--params", str(write(tmp_path, GOOD))], home, capsys, deps=deps)
    assert "could not record params file: " in out
    assert code == 0 and run_json(home)["emailPending"] == "propose"
    assert not (home / "runs" / "9.2.1" / "params.yml").exists()
    assert [p.name for p in (home / "runs" / "9.2.1").iterdir() if p.name.startswith("params")] == []   # no temp left


# regression: nothing changes without the new flags ----------------------------------------------------------------

