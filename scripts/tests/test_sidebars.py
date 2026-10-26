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
"""Tests for sidebars.ts: the numbered()/group() helpers as Docusaurus resolves them, and the Python text parsers.

The file is loaded with jiti, the TypeScript loader Docusaurus itself uses (a dependency of @docusaurus/utils, so present
whenever `npm ci` has run).  The tests fail, and never skip, when node or jiti is missing: a silent skip would hide the
central assertions.
"""

import json
import re
import shutil
import subprocess
from pathlib import Path

import pytest

from conftest import load_script

verify = load_script('verify-docs.py')

ROOT = Path(__file__).resolve().parents[2]
LOADER = ("const j = require('jiti')(process.argv[1] + '/package.json');"
          "const m = j(process.argv[1] + '/sidebars.ts');"
          "console.log(JSON.stringify(m.default ?? m))")
NUMBER_PREFIX = re.compile(r'^\d+(\.\d+)*\. ')
TOP_LEVEL = ['Getting Started', 'Guides', 'Documentation', 'Developer Info', 'Articles', 'Release Notes']


@pytest.fixture(scope='module')
def sidebars():
    node = shutil.which('node')
    if node is None:
        pytest.fail('node is not on the path; install Node and run `npm ci` in the docs root')
    if not (ROOT / 'node_modules' / 'jiti').is_dir():
        pytest.fail('node_modules/jiti is missing; run `npm ci` in the docs root')
    result = subprocess.run([node, '-e', LOADER, str(ROOT)], capture_output=True, text=True, check=False)
    if result.returncode != 0:
        pytest.fail(f'loading sidebars.ts failed:\n{result.stderr}')
    return json.loads(result.stdout)


@pytest.fixture(scope='module')
def top(sidebars):
    return {n['label']: n for n in sidebars['mainSidebar']}


def find_child(node, label):
    """The direct child whose label is `label`, looking through unnumbered groups, or None."""
    for item in node.get('items', []):
        if item['label'] == label:
            return item
        if item.get('customProps', {}).get('unnumbered'):
            found = find_child(item, label)
            if found:
                return found
    return None


def child(node, label):
    found = find_child(node, label)
    assert found, f"no child '{label}' under '{node['label']}'"
    return found


def walk(items):
    """Every item below `items`, depth first, parents before children."""
    for item in items:
        yield item
        yield from walk(item.get('items', []))


def test_there_is_exactly_one_sidebar(sidebars):
    assert list(sidebars) == ['mainSidebar']


def test_top_level_order(sidebars):
    assert [n['label'] for n in sidebars['mainSidebar']] == TOP_LEVEL


def test_numbers(top):
    docs = top['Documentation']
    labels = [n['label'] for n in docs['items']]
    assert labels[0] == '1. About Juneau'
    assert labels[1] == '2. Core & Data'
    assert labels[-1] == '7. Security & Migration'
    assert len(labels) == 7
    assert child(docs['items'][0], '1.1. Framework Comparisons')
    core = docs['items'][1]
    assert child(core, '2.1. juneau-commons')
    marshall = child(core, '2.2. juneau-marshall')
    first_group = marshall['items'][0]
    assert first_group['label'] == 'Core Concepts' and first_group['customProps']['unnumbered'] is True
    assert first_group['items'][0]['label'].startswith('2.2.1. Marshallers')
    json_support = child(marshall, '2.2.25. JSON Support')
    assert child(json_support, '2.2.25.5. @Json Annotation')
    assert [n['label'].split('.')[0] for n in top['Getting Started']['items']] == ['1', '2', '3', '4']
    assert [n['label'].split('.')[0] for n in top['Guides']['items']] == [str(i) for i in range(1, 10)]
    assert [n['label'].split('.')[0] for n in top['Developer Info']['items']] == ['1', '2', '3', '4']


def flatten_groups(items):
    """The items of a list with every unnumbered group replaced by its own items."""
    for item in items:
        if item.get('customProps', {}).get('unnumbered'):
            yield from flatten_groups(item['items'])
        else:
            yield item


def test_groups_are_unnumbered_and_continue_the_count(top):
    nodes = list(walk(top['Documentation']['items']))
    groups = [n for n in nodes if n.get('customProps', {}).get('unnumbered')]
    assert len(groups) == 12
    assert not [g for g in groups if NUMBER_PREFIX.match(g['label'])]
    # Every numbered category: its numbered children, looking through groups, count 1..n under its own number.
    for node in nodes:
        if node in groups or not node.get('items'):
            continue
        own = NUMBER_PREFIX.match(node['label']).group(0)[:-2]
        for position, item in enumerate(flatten_groups(node['items']), start=1):
            assert item['label'].startswith(f'{own}.{position}. '), (node['label'], item['label'])


def test_articles(top):
    articles = top['Articles']
    assert articles['link'] == {'type': 'doc', 'id': 'developer-info/05.ArticlesIndex'}
    assert articles.get('customProps', {}).get('unnumbered') is None
    assert [i['type'] for i in articles['items']] == ['link'] * 3 + ['doc']
    assert [i['label'][:13] for i in articles['items']] == ['2018-11-12 · ', '2018-12-15 · ', '2019-10-21 · ', '2026-10-26 · ']


def test_release_notes(top):
    notes = top['Release Notes']
    assert [n['label'] for n in notes['items']] == ['10.0.0', '9.2.0', '9.1.0', '9.0.1', '9.0.0', '8.x', '7.x', '6.x', '5.x']
    v10 = notes['items'][0]
    assert v10['type'] == 'category' and len(v10['items']) == 5
    assert all(i['type'] == 'doc' for i in v10['items'])
    for section in ('Articles', 'Release Notes'):
        for node in walk(top[section]['items']):
            assert not NUMBER_PREFIX.match(node['label']), node['label']


def page_ids():
    """`<directory>/<id>` for every page under pages/; <id> is the leading front-matter `id:`, else the file stem."""
    ids = []
    for path in sorted((ROOT / 'pages').glob('*/*.md')):
        text = path.read_text(encoding='utf-8')
        front = text.split('---', 2)[1] if text.startswith('---') else ''
        m = re.search(r'^id:\s*["\']?([^"\'\n]+?)["\']?\s*$', front, re.MULTILINE)
        ids.append(f'{path.parent.name}/{m.group(1) if m else path.stem}')
    return ids


def sidebar_doc_ids(sidebars):
    ids = []
    for node in walk(sidebars['mainSidebar']):
        if node['type'] == 'doc':
            ids.append(node['id'])
        elif node['type'] == 'category' and (node.get('link') or {}).get('type') == 'doc':
            ids.append(node['link']['id'])
    return ids


def test_every_page_is_in_the_sidebar_exactly_once(sidebars):
    in_sidebar = sidebar_doc_ids(sidebars)
    assert len(in_sidebar) == len(set(in_sidebar)), 'duplicate doc ids'
    assert set(in_sidebar) == set(page_ids())


def test_python_parser_agrees_with_the_resolved_tree(sidebars):
    expected = [i for i in sidebar_doc_ids_in_order(sidebars['mainSidebar']) if i.startswith('topics/')]
    text = (ROOT / 'sidebars.ts').read_text(encoding='utf-8')
    _refs, _landing, preorder = verify.collect_sidebar(verify.parse_sidebars(text))
    assert ['topics/' + i for i in preorder] == expected


def sidebar_doc_ids_in_order(items):
    """Depth first, a category's doc link before its items; doc items and doc-linked categories only."""
    for node in items:
        if node['type'] == 'doc':
            yield node['id']
        elif node['type'] == 'category':
            if (node.get('link') or {}).get('type') == 'doc':
                yield node['link']['id']
            yield from sidebar_doc_ids_in_order(node['items'])
