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
The end-of-run terminal bell of push.py and test.py: rung on a TTY only, never in the Claude Code sandbox, silenced by
JUNEAU_PUSH_NO_SOUND, and rung once per run (test.py stays quiet when push.py or an enclosing run owns the run).
"""

from __future__ import annotations

import importlib.util
import io
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS_DIR / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class Stdout(io.StringIO):
    def __init__(self, tty):
        super().__init__()
        self.tty = tty

    def isatty(self):
        return self.tty


@pytest.fixture(autouse=True)
def clean_env(monkeypatch):
    for variable in ("SANDBOX_RUNTIME", "JUNEAU_PUSH_NO_SOUND", "JUNEAU_RUN_ACTIVE", "JUNEAU_RUN_N_BASE",
                     "JUNEAU_PUSH_ACTIVE"):
        monkeypatch.delenv(variable, raising=False)


@pytest.fixture
def push_mod(monkeypatch):
    module = load("_bell_push", "push.py")
    monkeypatch.setattr(module.platform, "system", lambda: "Other")    # no afplay, paplay or winsound
    return module


@pytest.fixture
def test_mod(monkeypatch):
    module = load("_bell_test", "test.py")
    return module


def terminal(monkeypatch, tty=True):
    out = Stdout(tty)
    monkeypatch.setattr("sys.stdout", out)
    return out


@pytest.mark.parametrize("success", [True, False])
def test_push_rings_on_a_tty_whether_the_run_passed_or_failed(push_mod, monkeypatch, success):
    out = terminal(monkeypatch)
    push_mod.play_sound(success=success)
    assert out.getvalue() == "\a"


def test_push_is_silent_off_a_tty(push_mod, monkeypatch):
    out = terminal(monkeypatch, tty=False)
    push_mod.play_sound(success=True)
    assert out.getvalue() == ""


def test_push_is_silent_in_the_sandbox(push_mod, monkeypatch):
    monkeypatch.setenv("SANDBOX_RUNTIME", "1")
    out = terminal(monkeypatch)
    push_mod.play_sound(success=True)
    assert out.getvalue() == ""


def test_push_is_silent_when_opted_out(push_mod, monkeypatch):
    monkeypatch.setenv("JUNEAU_PUSH_NO_SOUND", "1")
    out = terminal(monkeypatch)
    push_mod.play_sound(success=False)
    assert out.getvalue() == ""


@pytest.mark.parametrize("silenced", [True, False])
def test_push_no_sound_silences_the_toolkit_sound(push_mod, monkeypatch, silenced):
    notify = push_mod.toolkit("notify")
    played = []
    monkeypatch.setattr(notify, "_play", lambda ok: played.append(ok))
    if silenced:
        monkeypatch.setenv("JUNEAU_PUSH_NO_SOUND", "1")
    terminal(monkeypatch, tty=False)
    push_mod.play_sound(success=False)
    assert played == ([] if silenced else [False])


def test_push_never_raises_when_stdout_cannot_be_written(push_mod, monkeypatch):
    class Broken:
        def isatty(self):
            return True

        def write(self, text):
            raise OSError("closed")

    monkeypatch.setattr("sys.stdout", Broken())
    push_mod.play_sound(success=True)


@pytest.mark.parametrize("code", [0, 1])
def test_test_py_rings_when_a_standalone_run_ends(test_mod, monkeypatch, code):
    monkeypatch.setattr(test_mod, "_main", lambda: code)
    out = terminal(monkeypatch)
    assert test_mod.main() == code
    assert out.getvalue().count("\a") == 1


def test_test_py_rings_when_a_standalone_run_is_cancelled(test_mod, monkeypatch):
    def interrupted():
        raise KeyboardInterrupt

    monkeypatch.setattr(test_mod, "_main", interrupted)
    out = terminal(monkeypatch)
    assert test_mod.main() == 130
    assert out.getvalue().count("\a") == 1


def test_test_py_rings_once_standalone_and_never_under_push(test_mod, monkeypatch):
    monkeypatch.setattr(test_mod, "_main", lambda: 0)
    out = terminal(monkeypatch)
    test_mod.main()
    assert out.getvalue().count("\a") == 1
    monkeypatch.setenv("JUNEAU_PUSH_ACTIVE", "1")
    out = terminal(monkeypatch)
    test_mod.main()
    assert "\a" not in out.getvalue()


def test_test_py_sound_is_silenced_by_juneau_push_no_sound(test_mod, monkeypatch):
    """test.py's end-of-run signal is the bell; JUNEAU_PUSH_NO_SOUND=1 (shared with push.py) silences it."""
    monkeypatch.setenv("JUNEAU_PUSH_NO_SOUND", "1")
    monkeypatch.setattr(test_mod, "_main", lambda: 1)
    out = terminal(monkeypatch)
    assert test_mod.main() == 1
    assert "\a" not in out.getvalue()


@pytest.mark.parametrize("owner", ["JUNEAU_RUN_ACTIVE", "JUNEAU_RUN_N_BASE", "JUNEAU_PUSH_ACTIVE"])
def test_test_py_stays_quiet_when_push_or_a_run_owns_the_run(test_mod, monkeypatch, owner):
    monkeypatch.setenv(owner, "1")
    monkeypatch.setattr(test_mod, "_main", lambda: 0)
    out = terminal(monkeypatch)
    test_mod.main()
    assert "\a" not in out.getvalue()


def test_test_py_is_silent_off_a_tty(test_mod, monkeypatch):
    monkeypatch.setattr(test_mod, "_main", lambda: 0)
    out = terminal(monkeypatch, tty=False)
    test_mod.main()
    assert "\a" not in out.getvalue()


def test_test_py_is_silent_in_the_sandbox(test_mod, monkeypatch):
    monkeypatch.setenv("SANDBOX_RUNTIME", "1")
    monkeypatch.setattr(test_mod, "_main", lambda: 0)
    out = terminal(monkeypatch)
    test_mod.main()
    assert "\a" not in out.getvalue()


def test_test_py_is_silent_when_opted_out(test_mod, monkeypatch):
    monkeypatch.setenv("JUNEAU_PUSH_NO_SOUND", "1")
    monkeypatch.setattr(test_mod, "_main", lambda: 0)
    out = terminal(monkeypatch)
    test_mod.main()
    assert "\a" not in out.getvalue()


@pytest.mark.parametrize("sandbox", [False, True])
def test_push_rings_when_a_run_is_cancelled(push_mod, monkeypatch, sandbox):
    def interrupted():
        raise KeyboardInterrupt

    if sandbox:
        monkeypatch.setenv("SANDBOX_RUNTIME", "1")
    monkeypatch.setattr(push_mod, "_main", interrupted)
    out = terminal(monkeypatch)
    assert push_mod.main() == 130
    assert out.getvalue().count("\a") == (0 if sandbox else 1)


# ---- characterization: what makes test.py defer to an enclosing run ----------------------------------------------------
@pytest.mark.parametrize("owner", ["JUNEAU_PUSH_ACTIVE", "JUNEAU_RUN_N_BASE", "JUNEAU_RUN_ACTIVE", None])
def test_test_py_under_push_is_owned_by_the_enclosing_run(test_mod, monkeypatch, owner):
    if owner is not None:
        monkeypatch.setenv(owner, "1")
    assert test_mod.owned_by_enclosing_run() is (owner is not None)


def test_next_step_number_starts_after_n_base(monkeypatch):
    monkeypatch.setenv("JUNEAU_RUN_N_BASE", "2")
    fresh = load("_bell_test_fresh", "test.py")
    assert fresh.next_step_number() == 3
