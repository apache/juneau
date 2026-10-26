# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""push.py's Traces row: the plan, the guard over the full log, --warn-traces, --dry-run and the stale-snapshot message."""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parents[2]
TRACE = ("java.lang.IllegalStateException: boom\n"
         "\tat com.example.Foo.run(Foo.java:10)\n"
         "\tat com.example.Foo.main(Foo.java:4)\n")


@pytest.fixture
def push(tmp_path, monkeypatch):
    spec = importlib.util.spec_from_file_location("_undertest_push_traces", REPO_ROOT / "scripts" / "push.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    monkeypatch.delitem(sys.modules, "juneau_terminal", raising=False)
    module.toolkit()
    monkeypatch.setattr(module, "TRACE_ALLOWLIST", tmp_path / "trace-allowlist.txt")
    monkeypatch.setattr(module, "TRACE_REPORT", tmp_path / "target" / "trace-guard.txt")
    log = tmp_path / "full.log"
    log.write_text("", encoding="utf-8")
    monkeypatch.setenv("JUNEAU_TERMINAL_FULL_LOG", str(log))
    module.log = log
    return module


class Session:
    def __init__(self):
        self.failed = []

    def fail(self, msg, *, hint=None):
        self.failed.append(msg)
        raise SystemExit(1)


def args(push, *flags):
    return push.build_parser().parse_args(list(flags))


def test_the_guard_fails_on_a_trace_written_after_the_mark(push):
    since = push.traces.mark()
    push.log.write_text(TRACE, encoding="utf-8")
    with pytest.raises(SystemExit):
        push._trace_guard(Session(), args(push), since)
    assert "IllegalStateException: boom" in push.TRACE_REPORT.read_text(encoding="utf-8")


def test_a_trace_before_the_mark_is_ignored(push):
    push.log.write_text(TRACE, encoding="utf-8")
    push._trace_guard(Session(), args(push), push.traces.mark())


def test_warn_traces_does_not_fail(push):
    since = push.traces.mark()
    push.log.write_text(TRACE, encoding="utf-8")
    push._trace_guard(Session(), args(push, "--warn-traces"), since)


def test_an_allowlisted_trace_passes(push):
    push.TRACE_ALLOWLIST.write_text("^java\\.lang\\.IllegalStateException: boom$  # asserted by the Foo test\n", encoding="utf-8")
    since = push.traces.mark()
    push.log.write_text(TRACE, encoding="utf-8")
    push._trace_guard(Session(), args(push), since)


def test_an_allowlist_entry_with_no_reason_fails_even_under_warn_traces(push):
    push.TRACE_ALLOWLIST.write_text("^boom$\n", encoding="utf-8")
    with pytest.raises(SystemExit):
        push._trace_guard(Session(), args(push, "--warn-traces"), push.traces.mark())


def test_every_gate_skipped_runs_no_guard(push, monkeypatch):
    monkeypatch.setattr(push.traces, "gate", lambda *a, **k: pytest.fail("the guard ran"))
    push._trace_guard(Session(), args(push, "--skip-tests", "--skip-pytest", "--skip-e2e"), push.traces.mark())


def test_a_dry_run_shows_the_row_without_scanning(push, monkeypatch, capsys):
    monkeypatch.setattr(push.traces, "gate", lambda *a, **k: pytest.fail("the guard ran"))
    push._trace_guard(Session(), args(push, "--dry-run"), push.traces.mark())
    assert "Would run Traces" in capsys.readouterr().out


def test_a_snapshot_without_the_module_names_the_remedy(push, monkeypatch):
    monkeypatch.setitem(sys.modules, "juneau_terminal_scripts.traces", None)
    monkeypatch.delattr(sys.modules["juneau_terminal_scripts"], "traces")
    with pytest.raises(push.bootstrap.BootstrapError, match="mvn install"):
        push.toolkit()
