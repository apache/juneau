# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
# * with the License.  You may obtain a copy of the License at                                                              *
# *                                                                                                                         *
# *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
# *                                                                                                                         *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
# * specific language governing permissions and limitations under the License.                                              *
# ***************************************************************************************************************************
"""Tests for build-docs.py: write-only-on-change artifact maps."""

import argparse
import json
import os

import pytest

from conftest import load_script

build_docs = load_script('build-docs.py')


def write_module(root, rel, artifact_id, java_pkg=None):
    mod = root / rel if rel else root
    mod.mkdir(parents=True, exist_ok=True)
    (mod / 'pom.xml').write_text(
        f'<project xmlns="http://maven.apache.org/POM/4.0.0"><artifactId>{artifact_id}</artifactId></project>')
    if java_pkg:
        pkg_dir = mod / 'src' / 'main' / 'java' / java_pkg
        pkg_dir.mkdir(parents=True)
        (pkg_dir / 'A.java').write_text('class A {}')


@pytest.fixture
def layout(tmp_path):
    juneau = tmp_path / 'juneau'
    write_module(juneau, '', 'juneau')
    write_module(juneau, 'juneau-core', 'juneau-core', 'org/apache/juneau/core')
    docs = tmp_path / 'docs'
    summary = docs / 'static' / 'javadocs' / '1.0.0' / 'org' / 'apache' / 'juneau' / 'core'
    summary.mkdir(parents=True)
    (summary / 'package-summary.html').write_text('<html/>')
    return juneau, docs


def test_write_json_if_changed_skips_identical_content(tmp_path):
    out = tmp_path / 'x.json'
    assert build_docs.write_json_if_changed(out, {'b': 1, 'a': 2}) is True
    os.utime(out, (1, 1))
    assert build_docs.write_json_if_changed(out, {'b': 1, 'a': 2}) is False
    assert out.stat().st_mtime == 1
    assert build_docs.write_json_if_changed(out, {'b': 1, 'a': 3}) is True
    assert json.loads(out.read_text()) == {'b': 1, 'a': 3}
    assert out.read_text().endswith('}\n')


def test_artifact_maps_not_rewritten_on_second_run(layout):
    juneau, docs = layout
    paths_file = docs / 'artifact-paths.json'
    packages_file = docs / 'artifact-packages.json'

    build_docs.generate_artifact_paths(juneau, docs)
    build_docs.generate_javadoc_packages(juneau, docs, '1.0.0')
    assert json.loads(paths_file.read_text()) == {'juneau': '', 'juneau-core': 'juneau-core'}
    assert json.loads(packages_file.read_text()) == {'juneau-core': 'org/apache/juneau/core'}

    os.utime(paths_file, (1, 1))
    os.utime(packages_file, (1, 1))
    build_docs.generate_artifact_paths(juneau, docs)
    build_docs.generate_javadoc_packages(juneau, docs, '1.0.0')
    assert paths_file.stat().st_mtime == 1
    assert packages_file.stat().st_mtime == 1


def test_artifact_paths_rewritten_when_a_module_is_added(layout):
    juneau, docs = layout
    build_docs.generate_artifact_paths(juneau, docs)
    write_module(juneau, 'juneau-rest', 'juneau-rest')
    build_docs.generate_artifact_paths(juneau, docs)
    assert 'juneau-rest' in json.loads((docs / 'artifact-paths.json').read_text())


def test_artifact_paths_refuses_root_without_modules(tmp_path):
    root = tmp_path / 'empty'
    root.mkdir()
    docs = tmp_path / 'docs'
    docs.mkdir()
    existing = '{\n  "keep": "me"\n}\n'
    (docs / 'artifact-paths.json').write_text(existing)
    with pytest.raises(SystemExit) as exc:
        build_docs.generate_artifact_paths(root, docs)
    assert exc.value.code != 0
    assert (docs / 'artifact-paths.json').read_text() == existing


def test_artifact_paths_refuses_root_with_only_pom_packaging(tmp_path):
    root = tmp_path / 'juneau'
    write_module(root, '', 'juneau')
    (root / 'pom.xml').write_text(
        '<project xmlns="http://maven.apache.org/POM/4.0.0"><artifactId>juneau</artifactId>'
        '<packaging>pom</packaging></project>')
    docs = tmp_path / 'docs'
    docs.mkdir()
    with pytest.raises(SystemExit):
        build_docs.generate_artifact_paths(root, docs)
    assert not (docs / 'artifact-paths.json').exists()


def test_artifact_paths_written_for_root_with_jar_module(tmp_path):
    root = tmp_path / 'juneau'
    write_module(root, '', 'juneau')
    (root / 'pom.xml').write_text(
        '<project xmlns="http://maven.apache.org/POM/4.0.0"><artifactId>juneau</artifactId>'
        '<packaging>pom</packaging></project>')
    write_module(root, 'juneau-core', 'juneau-core')
    docs = tmp_path / 'docs'
    docs.mkdir()
    build_docs.generate_artifact_paths(root, docs)
    assert json.loads((docs / 'artifact-paths.json').read_text()) == {'juneau': '', 'juneau-core': 'juneau-core'}


def test_check_code_fences_runs_as_a_child_of_links(monkeypatch, tmp_path):
    calls = []
    monkeypatch.setattr(build_docs, 'run_command', lambda cmd, **kw: calls.append((cmd, kw)))
    build_docs.check_code_fences(tmp_path)
    (cmd, kw), = calls
    assert cmd[1].endswith('check-code-fences.py')
    assert cmd[2:] == ['--docs-root', str(tmp_path)]
    assert kw['id'] == 'links-fences' and kw['parent'] == 'links'


def test_check_java_fences_runs_strict_as_a_child_of_links(monkeypatch, tmp_path):
    calls = []
    monkeypatch.setattr(build_docs, 'run_command', lambda cmd, **kw: calls.append((cmd, kw)))
    build_docs.check_java_fences(tmp_path)
    (cmd, kw), = calls
    assert cmd[1].endswith('check-java-fences.py')
    assert cmd[2:] == ['--docs-root', str(tmp_path), '--strict']
    assert kw['id'] == 'links-java' and kw['parent'] == 'links'


def test_check_guide_snippets_runs_as_a_child_of_links(monkeypatch, tmp_path):
    calls = []
    monkeypatch.setattr(build_docs, 'run_command', lambda cmd, **kw: calls.append((cmd, kw)))
    build_docs.check_guide_snippets(tmp_path / 'juneau', tmp_path)
    (cmd, kw), = calls
    assert cmd[1].endswith('check-guide-snippets.py')
    assert cmd[2:] == ['--juneau-root', str(tmp_path / 'juneau')]
    assert kw['id'] == 'links-guides' and kw['parent'] == 'links' and kw['cwd'] == tmp_path


def test_links_stage_runs_every_checker_in_order(monkeypatch, tmp_path):
    # The build itself, not just the helper: a Links stage that forgets a checker must fail here.
    args = argparse.Namespace(skip_npm=True, skip_maven=False, skip_verify=True, skip_copy=True, staging=False)
    monkeypatch.setattr(build_docs, 'DRY_RUN', True)
    monkeypatch.setattr(build_docs, 'row_plan', lambda a, d=None: [
        (id, id, None if id == 'links' else 'skipped') for id, _label in build_docs.run_view.BUILD_ROWS])
    for name in ('generate_artifact_paths', 'get_current_release', 'ensure_apidocs_symlink', 'resolve_javadocs_version'):
        monkeypatch.setattr(build_docs, name, lambda *a, **kw: None)
    calls = []
    for name in ('check_topic_links', 'check_ai_artifacts', 'check_code_fences', 'check_java_fences', 'check_guide_snippets'):
        monkeypatch.setattr(build_docs, name, lambda *a, n=name: calls.append(n))
    assert build_docs.build(args, tmp_path / 'juneau', tmp_path) == 0
    assert calls == ['check_topic_links', 'check_ai_artifacts', 'check_code_fences', 'check_java_fences',
                     'check_guide_snippets']
