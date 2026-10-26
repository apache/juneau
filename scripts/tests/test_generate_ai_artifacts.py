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

SIDEBARS = """function numbered(items) { return items; }
function group(category) { return category; }

const sidebars = {
  mainSidebar: [
    {type: 'category', label: 'Getting Started', items: numbered([{type: 'doc', id: 'topics/Intro'}])},
    {type: 'category', label: 'Guides', items: numbered([])},
    {
      type: 'category',
      label: 'Documentation',
      items: numbered([
        {
          type: 'category',
          label: 'Basics',
          items: [{type: 'doc', id: 'topics/Alpha'}, {type: 'doc', id: 'topics/Beta'}],
        },
      ]),
    },
  ],
};

export default sidebars;
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
    modules: ['juneau-marshall', 'juneau-marshall-rdf'],
    docs: ['Alpha'],
  },
];
"""

IMPORT_LAYOUT = "import ProjectLayout from '@site/src/components/ProjectLayout';\n"


def add_about(docs):
    (docs / 'src' / 'data').mkdir(parents=True)
    (docs / 'src' / 'data' / 'projects.ts').write_text(PROJECTS_TS, encoding='utf-8')
    about = docs / 'src' / 'pages' / 'about'
    about.mkdir(parents=True)
    (about / 'index.mdx').write_text(
        f"---\ntitle: About Apache Juneau\ndescription: The overview.\n---\n\n{IMPORT_LAYOUT}\n"
        "<ProjectLayout>\n\nHello overview.\n\n</ProjectLayout>\n", encoding='utf-8')
    (about / 'marshall.mdx').write_text(
        f"---\ntitle: Juneau Marshall\ndescription: One bean. Every format.\n---\n\n{IMPORT_LAYOUT}\n"
        '<ProjectLayout slug="marshall">\n\n```java\nJson.of(bean);\n```\n\n</ProjectLayout>\n', encoding='utf-8')


def test_about_pages_become_records(layout):
    docs, juneau, _ = layout
    add_about(docs)
    config = gen.ExtractConfig(docs_root=docs, juneau_root=juneau, output_dir=docs / 'static' / 'ai')
    recs = [r for r in gen.build_records(config, '10.0.0') if r['source_type'] == 'about']
    by_id = {r['id']: r for r in recs}
    assert set(by_id) == {'about', 'about-marshall'}
    m = by_id['about-marshall']
    assert m['title'] == 'Juneau Marshall' and m['summary'] == 'One bean. Every format.'
    assert m['module'] == 'juneau-marshall'
    assert m['source_path'] == 'src/pages/about/marshall.mdx'
    assert 'Modules: juneau-marshall, juneau-marshall-rdf' in m['description']
    assert 'Json.of(bean);' in m['description']
    assert 'import ProjectLayout' not in m['description'] and '<ProjectLayout' not in m['description']
    gen.validate_records(recs)
    o = by_id['about']
    assert o['module'] == 'general' and o['source_path'] == 'src/pages/about/index.mdx'
    assert o['title'] == 'About Apache Juneau' and o['summary'] == 'The overview.'


def test_long_about_page_keeps_its_modules_line(layout):
    docs, juneau, _ = layout
    add_about(docs)
    page = docs / 'src' / 'pages' / 'about' / 'marshall.mdx'
    page.write_text(page.read_text(encoding='utf-8') + 'word ' * 1000, encoding='utf-8')
    config = gen.ExtractConfig(docs_root=docs, juneau_root=juneau, output_dir=docs / 'static' / 'ai')
    rec = next(r for r in gen.build_records(config, '10.0.0') if r['id'] == 'about-marshall')
    assert len(rec['description']) == 2000 and rec['description'].endswith('...')
    assert 'Modules: juneau-marshall' in rec['description']


def test_llms_txt_has_an_about_section(layout):
    docs, juneau, _ = layout
    add_about(docs)
    config = gen.ExtractConfig(docs_root=docs, juneau_root=juneau, output_dir=docs / 'static' / 'ai')
    text = gen.render_llms_txt(config, gen.build_records(config, '10.0.0'), '10.0.0')
    assert '## About' in text
    assert '- [About Apache Juneau](https://juneau.apache.org/about/): The overview.' in text
    assert '- [Juneau Marshall](https://juneau.apache.org/about/marshall/): One bean. Every format.' in text
    assert text.index('## About') < text.index('## Resources')


def test_no_about_pages_means_no_about_section(layout):
    docs, juneau, _ = layout
    config = gen.ExtractConfig(docs_root=docs, juneau_root=juneau, output_dir=docs / 'static' / 'ai')
    assert '## About' not in gen.render_llms_txt(config, gen.build_records(config, '10.0.0'), '10.0.0')


def test_parse_sidebar_tree_skips_helper_code_before_main_sidebar():
    tree = gen.parse_sidebar_tree(SIDEBARS)
    assert [n['label'] for n in tree] == ['Getting Started', 'Guides', 'Documentation']


def test_parse_sidebar_tree_requires_main_sidebar():
    with pytest.raises(ValueError, match='mainSidebar'):
        gen.parse_sidebar_tree('const sidebars = {};')


def test_llms_txt_requires_the_three_top_level_categories(layout):
    docs, juneau, _ = layout
    (docs / 'sidebars.ts').write_text(SIDEBARS.replace("label: 'Guides'", "label: 'Other'"), encoding='utf-8')
    config = gen.ExtractConfig(docs_root=docs, juneau_root=juneau, output_dir=docs / 'static' / 'ai')
    with pytest.raises(ValueError, match="'Guides'"):
        gen.render_llms_txt(config, gen.build_records(config, '10.0.0'), '10.0.0')
