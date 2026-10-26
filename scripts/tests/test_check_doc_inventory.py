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
"""Tests for check-doc-inventory.py: module coverage comes from src/data/projects.ts."""

import json

from conftest import load_script

inventory = load_script('check-doc-inventory.py')

PROJECTS_TS = """export const GROUPS: Group[] = [
  {
    id: 'data',
    label: 'Data',
  },
];

export const PROJECTS: Project[] = [
  {
    slug: 'marshall',
    name: 'Juneau Marshall',
    group: 'data',
    tagline: 'One bean. Every format.',
    modules: ['juneau-marshall'],
    docs: ['JuneauMarshall'],
  },
];
"""


def make_tree(tmp_path, modules, allowlist=()):
    (tmp_path / 'src' / 'data').mkdir(parents=True)
    (tmp_path / 'src' / 'data' / 'projects.ts').write_text(PROJECTS_TS, encoding='utf-8')
    (tmp_path / 'scripts').mkdir()
    (tmp_path / 'scripts' / 'check-doc-inventory-allowlist.txt').write_text(
        ''.join(f'{m}\n' for m in allowlist), encoding='utf-8')
    (tmp_path / 'artifact-paths.json').write_text(json.dumps({m: m for m in modules}), encoding='utf-8')
    (tmp_path / 'artifact-packages.json').write_text(json.dumps({m: 'x' for m in modules}), encoding='utf-8')
    return tmp_path


def run(docs):
    findings = inventory.Findings()
    inventory.run_checks(docs, findings)
    return [(sev, check, msg) for sev, check, msg in findings.items if sev == 'error']


def test_module_in_a_project_passes(tmp_path):
    assert run(make_tree(tmp_path, ['juneau-marshall'])) == []


def test_unassigned_module_fails(tmp_path):
    errors = run(make_tree(tmp_path, ['juneau-marshall', 'juneau-orphan']))
    assert [(c, 'juneau-orphan' in m) for _, c, m in errors] == [('inventory', True)]


def test_module_both_allowlisted_and_assigned_fails(tmp_path):
    errors = run(make_tree(tmp_path, ['juneau-marshall'], allowlist=['juneau-marshall']))
    assert [c for _, c, _ in errors] == ['allowlist']


def test_project_module_missing_from_artifact_paths_fails(tmp_path):
    errors = run(make_tree(tmp_path, ['juneau-other'], allowlist=['juneau-other']))
    assert [c for _, c, _ in errors] == ['stale-rows']
