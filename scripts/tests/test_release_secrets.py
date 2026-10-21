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
import os
import subprocess
import sys
from pathlib import Path

from jrm_release import promote, rc, revert

pytestmark = pytest.mark.usefixtures("clean_env")

PW, GPG, GH = "s3cr3t-pw", "s3cr3t-gpg", "s3cr3t-gh"
PASSED = {"vote": {"outcome": "passed"}, "nexusRepoId": "r-1", "rcCommit": "abc123", "phase": "vote"}


def _drive_secret_steps(ctx):
    for fn in (rc.gpg_unlock, rc.release_prepare, rc.release_perform, rc.dist_stage, promote.dist_promote,
               promote.final_tag, promote.github_release, revert.revert_tag, revert.revert_versions):
        try:
            fn(ctx)
        except Exception:   # noqa: BLE001 - only the recorded calls matter here
            pass


def test_no_secret_on_any_argv(make_ctx):
    ctx = make_ctx(state=PASSED, answers={"confirm.revert-versions": True, "confirm.drop-stale": False})
    _drive_secret_steps(ctx)
    assert ctx.fake.calls
    for c in ctx.fake.calls:
        assert not any(s in " ".join(c["argv"]) for s in (PW, GPG, GH))


def test_secret_env_only_on_its_child(make_ctx):
    ctx = make_ctx(state=PASSED)
    _drive_secret_steps(ctx)
    for c in ctx.fake.calls:
        env = c["env"]
        if "GH_TOKEN" in env:
            assert c["argv"][0] == "gh"
        if "JRM_ASKPASS_PASS" in env:
            assert c["argv"][0] in ("git", "mvn")
    assert any(c["argv"][0] == "gh" and c["env"].get("GH_TOKEN") == GH for c in ctx.fake.calls)


def test_secret_stdin_only_on_gpg_and_svnmucc(make_ctx):
    ctx = make_ctx(state=PASSED)
    _drive_secret_steps(ctx)
    for c in ctx.fake.calls:
        if c["input"] is not None:
            assert c["argv"][0] in ("gpg", "svnmucc")


def test_secrets_absent_from_run_json_and_full_log(make_ctx, home):
    ctx = make_ctx(state=PASSED)
    _drive_secret_steps(ctx)
    ctx.rf.set("probe", True)
    text = ctx.rf.path.read_text()
    said = "\n".join(ctx.fake.said)
    for s in (PW, GPG, GH):
        assert s not in text
        assert s not in said


def test_askpass_answers_from_environment_only():
    script = Path(__file__).resolve().parent.parent / "jrm_release" / "askpass.py"
    assert os.access(script, os.X_OK)
    env = {**os.environ, "JRM_ASKPASS_USER": "jamesbognar", "JRM_ASKPASS_PASS": PW}

    def ask(prompt):
        return subprocess.run([sys.executable, "-B", str(script), prompt], env=env, capture_output=True,
                              text=True, check=True).stdout

    assert ask("Username for 'https://gitbox.apache.org': ") == "jamesbognar"
    assert ask("Password for 'https://jamesbognar@gitbox.apache.org': ") == PW
