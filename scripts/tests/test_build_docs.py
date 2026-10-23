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
