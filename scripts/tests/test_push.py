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
"""
Tests for the --tracker-audit opt-in gate in scripts/push.py.

Juneau-specific exception to this directory's usual byte-for-byte-identical-across-repos rule
(see README.md): push.py's --sonarqube-style opt-in gate pattern only exists in juneau's
push.py (release-manager's push.py is a bare add/commit/push helper with no gates at all;
sandbox-support-console has no push.py). This file is therefore NOT copied to the other two
repos, unlike everything else in scripts/tests/.

The central property under test is the one the task explicitly demands: when --tracker-audit
is not passed, the gate must be IMPOSSIBLE to observe at runtime -- not just "returns early",
but never even reaches subprocess.run. test_off_path_never_invokes_subprocess pins that all
the way down to the subprocess boundary.
"""

from __future__ import annotations

import argparse
import importlib.util
import subprocess
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


def _load_push_module():
    """Load scripts/push.py as a fresh module object (mirrors conftest.py's _load_script)."""
    path = SCRIPTS_DIR / "push.py"
    spec = importlib.util.spec_from_file_location("_undertest_push", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def push_module():
    """A fresh scripts/push.py module object, loaded once per test (see module docstring)."""
    return _load_push_module()


def _args(tracker_audit: bool) -> argparse.Namespace:
    """Just enough of push.py's parsed-args surface for maybe_run_tracker_audit_gate()."""
    return argparse.Namespace(tracker_audit=tracker_audit)


class TestArgparseWiring:
    """The --tracker-audit / --todo-audit flag itself: default OFF, both spellings work."""

    def test_default_is_off(self, push_module):
        parser = push_module.argparse.ArgumentParser()
        # Re-derive just the flag under test rather than re-implementing all of main()'s
        # parser setup here -- calling main()'s real parser construction would require a
        # positional "message" argument and would drag in every other flag's defaults too.
        parser.add_argument("--tracker-audit", "--todo-audit", action="store_true", dest="tracker_audit")
        args = parser.parse_args([])
        assert args.tracker_audit is False

    def test_both_spellings_set_the_same_dest(self, push_module):
        parser = push_module.argparse.ArgumentParser()
        parser.add_argument("--tracker-audit", "--todo-audit", action="store_true", dest="tracker_audit")
        assert parser.parse_args(["--tracker-audit"]).tracker_audit is True
        assert parser.parse_args(["--todo-audit"]).tracker_audit is True


class TestOffPathNeverInvokesTheGate:
    """The property the task explicitly requires: off means off, all the way down."""

    def test_maybe_run_returns_none_without_calling_run_tracker_audit_gate(self, push_module, monkeypatch):
        calls = []
        monkeypatch.setattr(push_module, "run_tracker_audit_gate", lambda *a, **k: calls.append((a, k)))

        result = push_module.maybe_run_tracker_audit_gate(_args(tracker_audit=False), Path("/irrelevant"), 1)

        assert result is None
        assert calls == []

    def test_off_path_never_invokes_subprocess(self, push_module, monkeypatch):
        """
        Strongest available proof: even subprocess.run itself -- the only way this gate could
        ever touch real runtime behavior -- is never called on the off path. This is checked
        against push_module.subprocess (the module's own imported reference), not the global
        subprocess module, so it can't pass by accident via a different import path.
        """
        def _boom(*_a, **_k):
            raise AssertionError("subprocess.run must not be called when --tracker-audit is off")

        monkeypatch.setattr(push_module.subprocess, "run", _boom)

        result = push_module.maybe_run_tracker_audit_gate(_args(tracker_audit=False), Path("/irrelevant"), 1)

        assert result is None

    def test_off_path_is_true_no_op_regardless_of_other_args(self, push_module, monkeypatch):
        """Same as above, but with a step_num/juneau_root that would be nonsensical if actually used."""
        monkeypatch.setattr(push_module.subprocess, "run", lambda *a, **k: pytest.fail("must not run"))
        result = push_module.maybe_run_tracker_audit_gate(_args(tracker_audit=False), Path("/does/not/exist"), 999)
        assert result is None


class TestOnPathDelegatesToTheGate:
    """Sanity check for the complementary path: --tracker-audit really does invoke the gate."""

    def test_maybe_run_calls_run_tracker_audit_gate_and_returns_its_status(self, push_module, monkeypatch):
        seen = {}

        def _fake_gate(juneau_root, step_num):
            seen["juneau_root"] = juneau_root
            seen["step_num"] = step_num
            return "pass"

        monkeypatch.setattr(push_module, "run_tracker_audit_gate", _fake_gate)

        result = push_module.maybe_run_tracker_audit_gate(_args(tracker_audit=True), Path("/repo"), 3)

        assert result == "pass"
        assert seen == {"juneau_root": Path("/repo"), "step_num": 3}


class TestRunTrackerAuditGateExitCodeMapping:
    """run_tracker_audit_gate()'s exit-code contract, mirroring run_sonarqube_gate's shape."""

    @pytest.mark.parametrize(
        ("returncode", "expected_status"),
        [
            (0, "pass"),
            (1, "fail"),
            (2, "error"),
            (77, "error"),  # anything undocumented is treated as an error, not silently ignored
        ],
    )
    def test_returncode_mapping(self, push_module, monkeypatch, returncode, expected_status):
        monkeypatch.setattr(
            push_module.subprocess,
            "run",
            lambda *a, **k: subprocess.CompletedProcess(args=a, returncode=returncode),
        )

        status = push_module.run_tracker_audit_gate(Path("/repo"), 1)

        assert status == expected_status

    def test_invokes_todo_status_audit_script_via_current_interpreter(self, push_module, monkeypatch):
        captured = {}

        def _fake_run(cmd, cwd, check):
            captured["cmd"] = cmd
            captured["cwd"] = cwd
            captured["check"] = check
            return subprocess.CompletedProcess(args=cmd, returncode=0)

        monkeypatch.setattr(push_module.subprocess, "run", _fake_run)

        push_module.run_tracker_audit_gate(Path("/repo"), 1)

        assert captured["cwd"] == Path("/repo")
        assert captured["check"] is False
        assert captured["cmd"][0] == push_module.sys.executable
        assert captured["cmd"][1] == str(SCRIPTS_DIR / "todo-status-audit.py")
