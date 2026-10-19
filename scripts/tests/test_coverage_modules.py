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
"""coverage.py --run: a module whose tests live in a sibling `<name>-tests` module runs both modules."""

import importlib.util
from pathlib import Path
from types import SimpleNamespace

COVERAGE_PATH = Path(__file__).resolve().parent.parent / "coverage.py"


def load_coverage(root):
    spec = importlib.util.spec_from_file_location("coverage_under_test", COVERAGE_PATH)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    mod.REPO_ROOT = root
    return mod


def make_module(root, rel):
    d = root / rel
    d.mkdir(parents=True)
    (d / "pom.xml").write_text("<project/>", encoding="utf-8")
    return d


def test_a_module_with_a_sibling_tests_module_runs_both(tmp_path):
    cov = load_coverage(tmp_path)
    src = make_module(tmp_path, "juneau-rest/juneau-rest-server-console-ui-freemarker")
    tests = make_module(tmp_path, "juneau-rest/juneau-rest-server-console-ui-freemarker-tests")
    assert cov.modules_to_test(src) == [src, tests]


def test_a_module_without_a_tests_sibling_runs_alone(tmp_path):
    cov = load_coverage(tmp_path)
    src = make_module(tmp_path, "juneau-rest/juneau-rest-server-view-freemarker")
    assert cov.modules_to_test(src) == [src]


def test_run_tests_passes_both_modules_to_maven(tmp_path, monkeypatch):
    cov = load_coverage(tmp_path)
    src = make_module(tmp_path, "juneau-rest/a")
    make_module(tmp_path, "juneau-rest/a-tests")
    seen = []
    monkeypatch.setattr(cov.subprocess, "run", lambda cmd, **kw: seen.append(cmd) or SimpleNamespace(returncode=0))
    cov.run_tests(src)
    assert seen[0][seen[0].index("-pl") + 1] == "juneau-rest/a,juneau-rest/a-tests"
