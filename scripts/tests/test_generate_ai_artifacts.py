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
"""Tests for generate-ai-artifacts.py --check: source commits are not a freshness key."""

import json
import sys

import pytest

from conftest import load_script

gen = load_script('generate-ai-artifacts.py')

SIDEBARS = """export default {
  mainSidebar: [
    {
      type: 'category',
      label: 'Documentation',
      items: [
        {
          type: 'category',
          label: 'Basics',
          items: [{type: 'doc', id: 'topics/Alpha'}, {type: 'doc', id: 'topics/Beta'}],
        },
      ],
    },
  ],
};
"""


def write_topic(docs, name, title, body):
    path = docs / 'pages' / 'topics' / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(f'---\ntitle: "{title}"\n---\n\n{body}\n', encoding='utf-8')


@pytest.fixture
def layout(tmp_path, monkeypatch):
    docs = tmp_path / 'docs'
    juneau = tmp_path / 'juneau'
    juneau.mkdir()
    write_topic(docs, '01.Alpha.md', 'Alpha', 'Alpha topic.')
    (docs / 'sidebars.ts').write_text(SIDEBARS, encoding='utf-8')
    commits = {'docs': 'a' * 40, 'juneau': 'b' * 40}
    monkeypatch.setattr(gen, 'get_git_commit', lambda root: commits['docs' if root == docs else 'juneau'])
    return docs, juneau, commits


def run(monkeypatch, docs, juneau, *extra):
    monkeypatch.setattr(sys, 'argv', ['generate-ai-artifacts.py', '--docs-root', str(docs),
                                      '--juneau-root', str(juneau), '--output-dir', str(docs / 'static' / 'ai'),
                                      *extra])
    return gen.main()


def manifest(docs):
    return json.loads((docs / 'static' / 'ai' / 'manifest.json').read_text(encoding='utf-8'))


def test_check_passes_when_only_source_commits_change(layout, monkeypatch):
    docs, juneau, commits = layout
    assert run(monkeypatch, docs, juneau) == 0
    commits['docs'] = 'c' * 40
    commits['juneau'] = 'd' * 40
    assert run(monkeypatch, docs, juneau, '--check') == 0


def test_generate_still_records_source_commits(layout, monkeypatch):
    docs, juneau, commits = layout
    assert run(monkeypatch, docs, juneau) == 0
    assert manifest(docs)['source_commit'] == {'juneau_docs': 'a' * 40, 'juneau': 'b' * 40}
    commits['juneau'] = 'd' * 40
    assert run(monkeypatch, docs, juneau) == 0
    assert manifest(docs)['source_commit']['juneau'] == 'd' * 40


def test_check_fails_on_topic_edit(layout, monkeypatch, capsys):
    docs, juneau, _ = layout
    assert run(monkeypatch, docs, juneau) == 0
    write_topic(docs, '01.Alpha.md', 'Alpha', 'Alpha topic, edited.')
    assert run(monkeypatch, docs, juneau, '--check') == 1
    assert 'juneau-knowledge.jsonl' in capsys.readouterr().out


def test_check_fails_on_added_record(layout, monkeypatch, capsys):
    docs, juneau, commits = layout
    assert run(monkeypatch, docs, juneau) == 0
    write_topic(docs, '02.Beta.md', 'Beta', 'Beta topic.')
    commits['docs'] = 'c' * 40
    assert run(monkeypatch, docs, juneau, '--check') == 1
    assert 'manifest.json' in capsys.readouterr().out


def test_is_stale_compares_other_manifest_fields_byte_exact(tmp_path):
    path = tmp_path / 'manifest.json'
    expected = gen.render_json({'record_count': 2, 'source_commit': {'juneau': 'x'}, 'version': '1.0'})
    drifted = gen.render_json({'record_count': 2, 'source_commit': {'juneau': 'y'}, 'version': '1.0'})
    assert gen.is_stale(path, drifted, expected) is False
    assert gen.is_stale(path, drifted.replace('"1.0"', '"1.1"'), expected) is True
    assert gen.is_stale(path, json.dumps(json.loads(drifted)), expected) is True
    assert gen.is_stale(path, gen.render_json({'record_count': 2, 'version': '1.0'}), expected) is True
    assert gen.is_stale(path, 'not json', expected) is True
    assert gen.is_stale(path, None, expected) is True


def test_is_stale_ignores_nothing_outside_manifest(tmp_path):
    path = tmp_path / 'taxonomy.json'
    expected = gen.render_json({'source_commit': 'x'})
    assert gen.is_stale(path, gen.render_json({'source_commit': 'y'}), expected) is True
