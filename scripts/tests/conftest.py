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
Keep the scripts tests from writing run history into the real ~/.cache/juneau-run/history, and the shared fixtures for
the tests that need history on: run_history (F1), fake_reports (F2) and history_record (F3).
"""

import importlib.util
import os
import shutil
import sys
import tempfile
from pathlib import Path
from types import SimpleNamespace

import pytest

RUN_MODULE_PATH = Path(__file__).resolve().parent.parent.parent / "juneau-run" / "src" / "main" / "python" / "juneau_run.py"


@pytest.fixture(scope="session", autouse=True)
def _no_real_run_history():
    tmp = tempfile.mkdtemp(prefix="juneau-run-test-history-")
    names = ("JUNEAU_RUN_HISTORY", "JUNEAU_RUN_HISTORY_DIR")
    saved = {n: os.environ.get(n) for n in names}
    os.environ["JUNEAU_RUN_HISTORY"] = "0"
    os.environ["JUNEAU_RUN_HISTORY_DIR"] = tmp
    try:
        yield
    finally:
        for n, v in saved.items():
            if v is None:
                os.environ.pop(n, None)
            else:
                os.environ[n] = v
        shutil.rmtree(tmp, ignore_errors=True)


@pytest.fixture(autouse=True)
def _full_console(monkeypatch):
    """push.main() exports the condensed console default into os.environ; pin the full console for every test (set,
    not unset, so teardown restores it) so that export cannot leak into later tests.  Tests of the default or of
    the flag/env precedence set JUNEAU_RUN_CONSOLE themselves."""
    monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "full")


@pytest.fixture(autouse=True)
def _no_push_sound(monkeypatch):
    """Keep push.py's end-of-run bell and sound silent in every test, including subprocess runs (they inherit it)."""
    monkeypatch.setenv("JUNEAU_PUSH_NO_SOUND", "1")


def load_juneau_run():
    """The juneau_run module the scripts share (sys.modules["juneau_run"]), loaded from the tree if not yet loaded."""
    module = sys.modules.get("juneau_run")
    if module is None:
        spec = importlib.util.spec_from_file_location("juneau_run", RUN_MODULE_PATH)
        module = importlib.util.module_from_spec(spec)
        sys.modules["juneau_run"] = module
        saved, sys.dont_write_bytecode = sys.dont_write_bytecode, True
        try:
            spec.loader.exec_module(module)
        finally:
            sys.dont_write_bytecode = saved
    return module


def _drop_history():
    module = sys.modules.get("juneau_run")
    if module is not None and getattr(module, "_history", None) is not None:
        module.unsubscribe(module._history)
        module._history = None


@pytest.fixture
def run_history(monkeypatch, tmp_path):
    """
    F1: run history on for one test, written under tmp_path/history.  argv[0] is /x/scripts/test.py so the writer's
    script_name() is "test", whatever runs the tests.  Yields a helper: start(repo) opens this process's History (for
    tests that drive run_tool directly), store(script="test", repo=...) is the HistoryStore a reader would open, and
    jr() is the juneau_run module now in sys.modules.
    """
    monkeypatch.delenv("JUNEAU_RUN_HISTORY", raising=False)
    monkeypatch.setenv("JUNEAU_RUN_HISTORY_DIR", str(tmp_path / "history"))
    monkeypatch.setattr(sys, "argv", ["/x/scripts/test.py"])
    _drop_history()

    def start(repo):
        load_juneau_run().history_start(repo)

    def store(script="test", repo=None):
        return load_juneau_run().HistoryStore(repo, script)

    try:
        yield SimpleNamespace(start=start, store=store, jr=load_juneau_run, root=tmp_path / "history")
    finally:
        _drop_history()


REACTOR_TEXT = """\
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for Fake 1.0:
[INFO]
[INFO] Module A ........................................... SUCCESS [ 10.0 s]
[INFO] Module B ........................................... SUCCESS [ 20.0 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
"""


def _module(root, path, name, report, test_class, tests, seconds, source=None):
    directory = root / path
    reports = directory / "target" / "surefire-reports"
    reports.mkdir(parents=True)
    (directory / "pom.xml").write_text(
        '<project xmlns="http://maven.apache.org/POM/4.0.0">'
        f"<artifactId>{path}</artifactId><name>{name}</name></project>", encoding="utf-8")
    (reports / report).write_text(
        f'<testsuite name="{test_class}" tests="{tests}" time="{seconds}"></testsuite>', encoding="utf-8")
    if source is not None:
        java = directory / "src" / "test" / "java" / Path(*test_class.split(".")).with_suffix(".java")
        java.parent.mkdir(parents=True)
        java.write_text(source, encoding="utf-8")


@pytest.fixture
def fake_reports(tmp_path):
    """
    F2: a two-module tree with Surefire reports: mod-a (a.ATest, 3 tests, 4.5 s, bucket core) and mod-b (b.BTest,
    1 test, 7.0 s, bucket container.springboot from its @SpringbootTest source).  Each module's pom has a <name>
    ("Module A", "Module B"); `text` is Maven output whose Reactor Summary gives them 10.0 s and 20.0 s.
    """
    root = tmp_path / "tree"
    root.mkdir()
    (root / "pom.xml").write_text("<project><artifactId>root</artifactId>"
                                  "<modules><module>mod-a</module><module>mod-b</module></modules></project>",
                                  encoding="utf-8")
    _module(root, "mod-a", "Module A", "TEST-a.ATest.xml", "a.ATest", 3, 4.5)
    _module(root, "mod-b", "Module B", "TEST-b.BTest.xml", "b.BTest", 1, 7.0,
            source="package b;\n@SpringbootTest\nclass BTest {}\n")
    return SimpleNamespace(root=root, text=REACTOR_TEXT)


@pytest.fixture
def history_record():
    """F3: a factory for a valid v1 `tests` record; keyword arguments override or add fields."""
    def make(**over):
        record = {"v": 1, "ts": "2026-10-14T00:00:00Z", "step": "tests", "tool": "maven", "sig": "s1",
                  "commit": "abc1234", "ok": True, "complete": True, "wall_s": 100.0, "tests": 4,
                  "series": {"surefire_s": {"mod-a": 4.5, "mod-b": 7.0}}}
        record.update(over)
        return record
    return make
