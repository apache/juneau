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


def test_classpath_union_drops_juneau_artifacts_and_dedupes(tmp_path):
    for m, cp in (("a", "/r/x.jar:/r/org/apache/juneau/j.jar"), ("b", "/r/x.jar:/r/y.jar")):
        t = tmp_path / m / "target"
        t.mkdir(parents=True)
        (t / mod.CLASSPATH_FILE).write_text(cp, encoding="utf-8")
    assert mod.dependency_classpath(tmp_path) == "/r/x.jar:/r/y.jar"
