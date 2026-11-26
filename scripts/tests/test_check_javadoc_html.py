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
check-javadoc-html.py: the custom-element filter, the compile-noise filter, the digest and the exit code, driven from canned
javadoc output so no javadoc process runs.
"""

from __future__ import annotations

import importlib.util
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location("check_javadoc_html", SCRIPTS_DIR / "check-javadoc-html.py")
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

CUSTOM = "a/B.java:3: error: unknown tag: jk\n a/B.java:3: error: unknown tag: jk\n"
NOISE = "a/B.java:4: error: cannot find symbol\na/B.java:5: error: package org.foo does not exist\n"
REAL = (
    "a/B.java:6: error: unknown tag: bogus\n"
    "a/B.java:7: error: heading used out of sequence: <H5>, compared to implicit preceding heading: <H1>\n"
    "a/B.java:9: warning: no caption for table\n"
)


def run(tmp_path, text, *extra, capsys):
    f = tmp_path / "out.txt"
    f.write_text(text, encoding="utf-8")
    code = mod.main(["--from-file", str(f), "--root", str(tmp_path), *extra])
    return code, capsys.readouterr().out


def test_custom_elements_and_compile_noise_are_ignored(tmp_path, capsys):
    code, out = run(tmp_path, CUSTOM + NOISE, capsys=capsys)
    assert code == 0
    assert "clean" in out


def test_unknown_custom_name_is_reported(tmp_path, capsys):
    code, out = run(tmp_path, REAL, capsys=capsys)
    assert code == 1
    assert "unknown tag: bogus" in out
    assert "heading used out of sequence: <Hn>" in out
    assert "no caption for table" in out


def test_report_only_exits_zero(tmp_path, capsys):
    code, out = run(tmp_path, REAL, "--report-only", capsys=capsys)
    assert code == 0
    assert "3 diagnostics" in out


def test_duplicates_collapse():
    remaining, custom, noise = mod.classify(mod.parse("a/B.java:1: error: no caption for table\na/B.java:1: error: no caption for table\n"))
    assert len(remaining) == 1
    assert not custom
    assert noise == 0


def test_custom_list_covers_stylesheet_elements():
    css = (SCRIPTS_DIR.parent / "docs" / "src" / "javadoc" / "javadoc.css").read_text(encoding="utf-8")
    for name in ("jk", "jv", "js", "jc", "jsm", "ja", "jsf", "jf", "xt", "c", "l", "property"):
        assert name in mod.CUSTOM_ELEMENTS
    assert "jk" in css


def test_reference_error_is_reported(tmp_path, capsys):
    code, out = run(tmp_path, "a/B.java:86: error: reference not found\n", capsys=capsys)
    assert code == 1
    assert "reference not found" in out


def test_all_four_groups_are_accepted():
    assert {"html", "accessibility", "reference", "syntax"} <= set(mod.GROUPS)


def test_docs_dir_skipped_only_at_root(tmp_path):
    for rel in ("docs/src/main/java/X.java", "m/src/main/java/p/docs/Y.java", "m/src/main/java/p/Z.java"):
        f = tmp_path / rel
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text("", encoding="utf-8")
    assert [p.name for p in mod.source_files(tmp_path)] == ["Z.java", "Y.java"]


def make_modules(tmp_path, cps):
    """One module per name with Java sources; a name mapped to None gets no cached classpath file."""
    for m, cp in cps.items():
        (tmp_path / m / "src" / "main" / "java").mkdir(parents=True)
        (tmp_path / m / "pom.xml").write_text("<project/>", encoding="utf-8")
        if cp is not None:
            (tmp_path / m / "target").mkdir()
            (tmp_path / m / "target" / mod.CLASSPATH_FILE).write_text(cp, encoding="utf-8")


def fake_mvn(monkeypatch, tmp_path, modules, ok=True):
    """Replace subprocess.run: a successful build writes the .new file for each of modules, a failed one writes nothing."""
    calls = []

    def run(cmd, **kw):
        calls.append(cmd)
        if ok:
            for m in modules:
                (tmp_path / m / "target").mkdir(exist_ok=True)
                (tmp_path / m / "target" / (mod.CLASSPATH_FILE + ".new")).write_text("/n/" + m + ".jar", encoding="utf-8")
        return type("P", (), {"returncode": 0 if ok else 1, "stdout": "Could not resolve juneau-test-utils"})()
    monkeypatch.setattr(mod.subprocess, "run", run)
    return calls


def test_classpath_union_drops_juneau_artifacts_and_dedupes(tmp_path):
    make_modules(tmp_path, {"a": "/r/x.jar:/r/org/apache/juneau/j.jar", "b": "/r/x.jar:/r/y.jar"})
    assert mod.dependency_classpath(tmp_path) == "/r/x.jar:/r/y.jar"


def test_partial_cache_is_rejected_and_rebuilt(tmp_path, monkeypatch):
    make_modules(tmp_path, {"a": "/r/old.jar", "b": None})
    calls = fake_mvn(monkeypatch, tmp_path, ["a", "b"])
    assert mod.dependency_classpath(tmp_path) == "/n/a.jar:/n/b.jar"
    assert len(calls) == 1
    assert not list(tmp_path.glob("**/*.new"))


def test_failed_build_raises_and_keeps_old_cache(tmp_path, monkeypatch):
    make_modules(tmp_path, {"a": "/r/a.jar", "b": "/r/b.jar"})
    fake_mvn(monkeypatch, tmp_path, ["a", "b"], ok=False)
    with pytest.raises(RuntimeError):
        mod.dependency_classpath(tmp_path, refresh=True)
    assert (tmp_path / "a" / "target" / mod.CLASSPATH_FILE).read_text(encoding="utf-8") == "/r/a.jar"
    assert not list(tmp_path.glob("**/*.new"))


def test_partial_build_is_not_published(tmp_path, monkeypatch):
    make_modules(tmp_path, {"a": None, "b": None})
    fake_mvn(monkeypatch, tmp_path, ["a"])
    with pytest.raises(RuntimeError):
        mod.dependency_classpath(tmp_path)
    assert not list(tmp_path.glob("**/" + mod.CLASSPATH_FILE))


def test_full_cache_runs_all_four_groups(tmp_path, monkeypatch, capsys):
    make_modules(tmp_path, {"a": "/r/a.jar", "b": "/r/b.jar"})
    seen = {}
    monkeypatch.setattr(mod, "run_javadoc", lambda root, groups, jh, cp=None: seen.update(groups=groups, cp=cp) or "")
    assert mod.main(["--root", str(tmp_path)]) == 0
    assert seen["groups"] == list(mod.GROUPS)
    assert seen["cp"] == "/r/a.jar:/r/b.jar"
    assert "skipped" not in capsys.readouterr().out


def test_build_failure_skips_reference_and_runs_the_rest(tmp_path, monkeypatch, capsys):
    make_modules(tmp_path, {"a": None})
    fake_mvn(monkeypatch, tmp_path, ["a"], ok=False)
    seen = {}
    monkeypatch.setattr(mod, "run_javadoc", lambda root, groups, jh, cp=None: seen.update(groups=groups, cp=cp) or "")
    assert mod.main(["--root", str(tmp_path)]) == 0
    assert seen["groups"] == ["html", "accessibility", "syntax"]
    assert seen["cp"] is None
    captured = capsys.readouterr()
    assert "javadoc html check: skipped reference" in captured.out
    assert "warning: skipped the reference group" in captured.err


def test_build_failure_still_fails_on_findings_in_other_groups(tmp_path, monkeypatch, capsys):
    make_modules(tmp_path, {"a": None})
    fake_mvn(monkeypatch, tmp_path, ["a"], ok=False)
    monkeypatch.setattr(mod, "run_javadoc", lambda root, groups, jh, cp=None: "a/B.java:9: warning: no caption for table\n")
    assert mod.main(["--root", str(tmp_path)]) == 1


def test_reference_only_with_build_failure_runs_nothing(tmp_path, monkeypatch, capsys):
    make_modules(tmp_path, {"a": None})
    fake_mvn(monkeypatch, tmp_path, ["a"], ok=False)
    monkeypatch.setattr(mod, "run_javadoc", lambda *a, **k: pytest.fail("javadoc must not run"))
    assert mod.main(["--root", str(tmp_path), "--groups", "reference"]) == 0
    assert "skipped reference" in capsys.readouterr().out
