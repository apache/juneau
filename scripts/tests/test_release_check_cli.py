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
"""release.py --check and the pre-run preflight end to end through main(): exit codes, flag conflicts, no prompt, nothing
created, the resume subset and the secret-leak guard.  Needs the juneau-terminal toolkit (JUNEAU_TERMINAL_PY)."""
import json
import os

import pytest

pytest.importorskip("yaml")

from test_release_cli import ENV_SECRETS, new_run_setup, params_file, run_main, seed  # noqa: E402

pytestmark = [pytest.mark.usefixtures("clean_env"),
              pytest.mark.skipif(not os.environ.get("JUNEAU_TERMINAL_PY"), reason="needs JUNEAU_TERMINAL_PY toolkit")]

SENTINELS = {"JRM_APACHE_PASSWORD": "SENTINEL-apache-pw-7731", "JRM_GPG_PASSPHRASE": "SENTINEL-gpg-pp-7731",
             "JRM_GITHUB_TOKEN": "SENTINEL-gh-tok-7731"}


def check_setup(tmp_path, monkeypatch, home, env=True):
    """(fake, prompts, deps) for a --check or a new run; the params file is in place and the secrets are in the
    environment unless env is False."""
    params_file(home)
    fake, prompts, deps = new_run_setup(tmp_path, monkeypatch, {})
    if not env:
        for k in ENV_SECRETS:
            monkeypatch.delenv(k)
    return fake, prompts, deps


def test_check_all_pass_exits_0_and_changes_nothing(home, capsys, tmp_path, monkeypatch):
    fake, prompts, deps = check_setup(tmp_path, monkeypatch, home)
    code, out = run_main(["--check", "--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 0, out
    assert "PASS" in out and "FAIL" not in out
    assert "0 failed, 0 could not be checked" in out
    assert prompts.asked == []
    assert not (home / "runs").exists() and not (home / "attempts").exists()


def test_check_failed_check_exits_2_and_lists_it(home, capsys, tmp_path, monkeypatch):
    fake, prompts, deps = check_setup(tmp_path, monkeypatch, home, env=False)
    code, out = run_main(["--check", "--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 2, out
    assert "FAIL  Apache password present" in out
    assert prompts.asked == [] and not (home / "runs").exists()


def test_check_that_could_not_run_exits_1(home, capsys, tmp_path, monkeypatch):
    fake, prompts, deps = check_setup(tmp_path, monkeypatch, home)
    fake.script("git", "-C", ok=False, code=128)         # the lazy branch-tip fetch fails: both content checks are cant
    code, out = run_main(["--check", "--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 1, out
    assert "???" in out and "FAIL" not in out


def test_check_with_a_failure_and_a_cant_exits_2(home, capsys, tmp_path, monkeypatch):
    fake, prompts, deps = check_setup(tmp_path, monkeypatch, home, env=False)
    fake.script("git", "-C", ok=False, code=128)
    code, out = run_main(["--check", "--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 2, out
    assert "FAIL" in out and "???" in out


@pytest.mark.parametrize("flags", [["--resume"], ["--revert"], ["--start-step", "workspace"], ["--init-params"],
                                   ["--dry-run"], ["--list-steps"], ["--skip-step", "javadoc-verify"]])
def test_check_cannot_be_combined_with_run_control_flags(home, capsys, flags):
    code, out = run_main(["--check", "--version", "9.2.1", *flags], home, capsys)
    assert code == 2
    assert "--check cannot be combined" in out


def test_check_without_a_params_file_exits_2_with_the_init_hint(home, capsys, tmp_path, monkeypatch):
    fake, prompts, deps = new_run_setup(tmp_path, monkeypatch, {})
    for argv in (["--check"], ["--check", "--version", "9.2.1"]):
        code, out = run_main(argv, home, capsys, deps=deps)
        assert code == 2
        assert "--init-params" in out
    assert prompts.asked == []


def test_a_new_run_whose_preflight_fails_lists_everything_and_creates_nothing(home, capsys, tmp_path, monkeypatch):
    from jrm_release import preflight
    fake, prompts, deps = check_setup(tmp_path, monkeypatch, home, env=False)
    monkeypatch.setattr(preflight.shutil, "which", lambda n: None if n == "svnmucc" else "/usr/bin/" + n)
    code, out = run_main(["--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 2
    assert "svnmucc: not on PATH" in out
    for name in ("Apache password present", "GPG passphrase present", "GitHub token present"):
        assert name in out
    assert "nothing was asked or changed" in out
    assert prompts.asked == [] and not (home / "runs").exists()


def test_resume_runs_only_the_tools_and_secret_groups(home, capsys, tmp_path, monkeypatch):
    from test_release_cli import ACTIVE
    seed(home, {**ACTIVE, "last": "dist-verify"})
    fake, prompts, deps = new_run_setup(tmp_path, monkeypatch, {"email.vote": True})
    code, out = run_main(["--resume"], home, capsys, deps=deps)
    assert code == 0, out
    assert not fake.ran("git", "ls-remote")              # the repo group did not run
    assert not fake.ran("git", "init")                   # and no scratch repository was created
    assert fake.ran("gh", "api", "user")                 # the secrets-valid group did


def test_no_secret_reaches_output_argv_or_run_state(home, capsys, tmp_path, monkeypatch):
    params_file(home)
    fake, prompts, deps = new_run_setup(tmp_path, monkeypatch, {"email.propose": None})
    for k, v in SENTINELS.items():
        monkeypatch.setenv(k, v)
    outs = []
    for argv in (["--check", "--version", "9.2.1"], ["--version", "9.2.1"]):
        code, out = run_main(argv, home, capsys, deps=deps)
        outs.append(out)
    blob = "\n".join(outs) + json.dumps(fake.argvs()) + "\n".join(fake.said)
    for root, _, files in os.walk(home):
        for f in files:
            blob += (open(os.path.join(root, f), errors="replace").read())
    for secret in SENTINELS.values():
        assert secret not in blob
