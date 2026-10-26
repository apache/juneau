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
"""Tests for check-project-pages.py and project_old_names.py (checks C1 to C12 of the project pages)."""

import json
from pathlib import Path

import pytest

import project_old_names as names
from conftest import load_script

check = load_script('check-project-pages.py')

REPO_ROOT = Path(__file__).resolve().parent.parent.parent


def old(*parts, sep='-'):
    """An old name assembled from fragments, so this file carries none as a literal (the zero-old-names gate stays clean)."""
    return sep.join(parts)


# ---------------------------------------------------------------------------
# C9: the old-names list
# ---------------------------------------------------------------------------

def test_old_modules_are_flagged():
    for name in (old('juneau', 'run'), old('juneau', 'run', 'report'), old('juneau', 'test'),
                 old('juneau', 'rest', 'server', 'console', 'ui'),
                 old('juneau', 'rest', 'server', 'console', 'ui', 'freemarker'),
                 old('juneau', 'rest', 'server', 'console', 'ui', 'freemarker', 'tests'),
                 old('juneau', 'rest', 'server', 'views'),
                 old('juneau', 'rest', 'server', 'views', 'markdown'),
                 old('juneau', 'rest', 'server', 'widgets'),
                 old('juneau', 'rest', 'server', 'datatables'),
                 old('juneau', 'rest', 'server', 'bus'),
                 old('juneau', 'rest', 'server', 'bus', 'websocket'),
                 old('juneau', 'rest', 'server', 'terminal'),
                 old('juneau', 'rest', 'server', 'terminal', 'bus')):
        assert names.find_old_names(f'use {name} here'), name


def test_whole_token_does_not_flag_kept_names():
    for ok in (old('juneau', 'test', 'utils'), old('juneau', 'test', 'marshall', 'utils'),
               old('juneau', 'rest', 'server', 'view', 'jsp'), old('juneau', 'rest', 'server', 'view', 'thymeleaf'),
               old('juneau', 'bct'), old('juneau', 'terminal'), old('juneau', 'console', 'views')):
        assert not names.find_old_names(f'use {ok} here'), ok


def test_old_packages_and_tool_names_are_flagged():
    assert names.find_old_names('import ' + old('org', 'apache', 'juneau', 'test', 'bct', sep='.') + '.BctAssertions;')
    assert names.find_old_names('import ' + old('org', 'apache', 'juneau', 'rest', 'server', 'views', sep='.') + '.X;')
    assert names.find_old_names(old('org', 'apache', 'juneau', 'rest', 'server', 'view', 'freemarker', 'console', sep='.'))
    assert names.find_old_names(old('JUNEAU', 'RUN', sep='_') + '_CONSOLE')
    assert names.find_old_names(old('juneau', 'run', sep='_'))
    assert names.find_old_names(old('juneau', 'scripts', sep='_'))
    assert names.find_old_names(old('RUN', 'MARKERS', sep='_'))
    assert names.find_old_names('see ' + old('/docs/topics/Juneau', 'Run', sep=''))
    assert names.find_old_names('see ' + old('/docs/topics/Driving', 'ARun', sep=''))
    assert not names.find_old_names(old('TERMINAL', 'MARKERS', sep='_'))   # not an old name


def test_run_feature_names_are_flagged():
    for text in (old('##', 'run ', sep=''), old('Run', 'Event', sep=''), old('Run', 'View', 'Page', sep=''),
                 old('run', 'View', sep=''), old('run', 'view', sep='_'), old('run', 'view'),
                 old('juneau', 'run', 'view'), old('Run', ' view', sep=''), old('juneau', 'run', 'script', sep='.'),
                 old('run', 'protocol', 'v1'),
                 'bootstrap.load("x", ' + old('run', '"y")', sep='=')):
        assert names.find_old_names(text), text


def test_kept_names_are_not_flagged():
    for ok in ('##pytest ', 'RunOverride', 'RunGroupTest', 'RunRequestedTest', 'RunDecisionTest',
               'foundry.dashboard.runs.Stage', 'RunEventsSseServlet', 'org/apache/juneau/console/', 'org/apache/juneau/views/',
               'org/apache/juneau/widgets/', 'org/apache/juneau/terminal/', old('JUNEAU', 'TERMINAL', 'MARKERS', 'FILE', sep='_')):
        assert not names.find_old_names(ok), ok


def test_hits_carry_label_line_and_text():
    hits = names.find_old_names('fine\nsee ' + old('juneau', 'run') + ' now')
    assert hits == [('module', 2, old('juneau', 'run'))]


def test_old_names_module_contains_no_literal_old_name():
    for f in (Path(names.__file__), Path(__file__)):
        assert not names.find_old_names(f.read_text(encoding='utf-8')), f.name


# ---------------------------------------------------------------------------
# The tree builder
# ---------------------------------------------------------------------------

GROUPS = [{'id': 'g1', 'label': 'First'}, {'id': 'g2', 'label': 'Second'}]
PROJECTS = [
    {'slug': 'alpha', 'name': 'Juneau Alpha', 'group': 'g1', 'tagline': 'The first one.',
     'modules': ['juneau-a'], 'docs': ['Alpha']},
    {'slug': 'beta-two', 'name': 'Juneau Beta', 'group': 'g2', 'tagline': 'The second one.',
     'modules': ['juneau-b'], 'docs': ['Alpha']},
]


def _arr(items):
    return '[' + ', '.join(f"'{i}'" for i in items) + ']'


def render_projects_ts(groups, projects, module_docs=None):
    def entries(rows):
        out = ''
        for row in rows:
            out += '  {\n'
            for k, v in row.items():
                out += f"    {k}: {_arr(v) if isinstance(v, list) else repr(v)},\n"
            out += '  },\n'
        return out
    return ('// header comment\nexport const GROUPS: Group[] = [\n' + entries(groups) + '];\n\n'
            'export const PROJECTS: Project[] = [\n' + entries(projects) + '];\n' +
            ('\nexport const MODULE_DOCS: Record<string, string> = {\n' +
             ''.join(f"  '{k}': '{v}',\n" for k, v in module_docs.items()) + '};\n' if module_docs is not None else ''))


def page_text(project, *, title=None, description=None, attrs=None, body='See [it](/docs/topics/Alpha).', toc=True):
    attrs = f'slug="{project["slug"]}"' if attrs is None else attrs
    fm = f'title: {title if title is not None else project["name"]}\n'
    fm += f'description: {description if description is not None else project["tagline"]}\n'
    if toc:
        fm += 'hide_table_of_contents: true\n'
    return (f"---\n{fm}---\n\nimport ProjectLayout from '@site/src/components/ProjectLayout';\n\n"
            f'<ProjectLayout {attrs}>\n\n{body}\n\n</ProjectLayout>\n')


OVERVIEW = ("---\ntitle: About\ndescription: All of it.\nhide_table_of_contents: true\n---\n\n"
            "import {Redirect} from '@docusaurus/router';\n"
            "import Head from '@docusaurus/Head';\n\n"
            "<Head>\n  <meta http-equiv=\"refresh\" content=\"0; url=/\" />\n"
            "  <meta name=\"robots\" content=\"noindex\" />\n</Head>\n\n"
            "<Redirect to=\"/\" />\n")


def write(root, rel, text):
    f = Path(root, rel)
    f.parent.mkdir(parents=True, exist_ok=True)
    f.write_text(text, encoding='utf-8')


def make_tree(tmp_path, *, projects=None, groups=None, paths=None, topics=('Alpha',), overview=OVERVIEW, skip=(),
              module_docs=None):
    """projects.ts, src/pages/about/*.mdx, artifact-paths.json and pages/topics/*.md in a throwaway docs root."""
    projects = PROJECTS if projects is None else projects
    write(tmp_path, 'src/data/projects.ts', render_projects_ts(GROUPS if groups is None else groups, projects, {} if module_docs is None else module_docs))
    for p in projects:
        if p['slug'] not in skip:
            write(tmp_path, f'src/pages/about/{p["slug"]}.mdx', page_text(p))
    if overview is not None:
        write(tmp_path, 'src/pages/about/index.mdx', overview)
    write(tmp_path, 'artifact-paths.json', json.dumps(paths if paths is not None else {'juneau-a': '', 'juneau-b': '', 'juneau': ''}))
    for t in topics:
        write(tmp_path, f'pages/topics/01.{t}.md', f'---\nslug: {t}\n---\n\n# {t}\n')
    return tmp_path


def ids(errors):
    return {e.split(':', 1)[0] for e in errors}


def test_valid_tree_passes(tmp_path):
    assert check.check(make_tree(tmp_path)) == []


# C1, C2
def test_slug_without_page_fails(tmp_path):
    assert 'C1' in ids(check.check(make_tree(tmp_path, skip=('beta-two',))))


def test_page_without_slug_fails(tmp_path):
    make_tree(tmp_path)
    write(tmp_path, 'src/pages/about/gamma.mdx', page_text({**PROJECTS[0], 'slug': 'gamma'}))
    assert 'C2' in ids(check.check(tmp_path))


# C3
def test_layout_slug_mismatch_fails(tmp_path):
    make_tree(tmp_path)
    write(tmp_path, 'src/pages/about/alpha.mdx', page_text(PROJECTS[0], attrs='slug="beta-two"'))
    assert 'C3' in ids(check.check(tmp_path))


def test_missing_layout_fails(tmp_path):
    make_tree(tmp_path)
    write(tmp_path, 'src/pages/about/alpha.mdx', page_text(PROJECTS[0]).replace('<ProjectLayout slug="alpha">', '<div>')
          .replace('</ProjectLayout>', '</div>'))
    assert 'C3' in ids(check.check(tmp_path))


@pytest.mark.parametrize('attrs', ['slug="alpha"', "slug='alpha'", "slug={'alpha'}", 'slug={"alpha"}',
                                   'slug = "alpha"', 'id="x"\n  slug="alpha"'])
def test_layout_slug_quote_forms(tmp_path, attrs):
    make_tree(tmp_path)
    write(tmp_path, 'src/pages/about/alpha.mdx', page_text(PROJECTS[0], attrs=attrs))
    assert check.check(tmp_path) == []


def test_index_redirect_stub_passes(tmp_path):
    assert check.check(make_tree(tmp_path)) == []


def test_index_with_project_layout_fails(tmp_path):
    make_tree(tmp_path, overview=OVERVIEW + '\n<ProjectLayout>\n\nHello.\n\n</ProjectLayout>\n')
    assert 'C3' in ids(check.check(tmp_path))


def test_index_without_redirect_fails(tmp_path):
    make_tree(tmp_path, overview=OVERVIEW.replace('<Redirect to="/" />', 'Hello.'))
    assert 'C3' in ids(check.check(tmp_path))


def test_index_without_meta_refresh_fails(tmp_path):
    make_tree(tmp_path, overview=OVERVIEW.replace('http-equiv="refresh"', 'http-equiv="x"'))
    errors = [e for e in check.check(tmp_path) if e.startswith('C3')]
    assert len(errors) == 1 and 'meta http-equiv="refresh"' in errors[0]


def test_index_redirect_elsewhere_fails(tmp_path):
    make_tree(tmp_path, overview=OVERVIEW.replace('to="/"', 'to="/downloads"'))
    assert 'C3' in ids(check.check(tmp_path))


def test_two_layouts_fails(tmp_path):
    make_tree(tmp_path)
    page = page_text(PROJECTS[0])
    write(tmp_path, 'src/pages/about/alpha.mdx', page + '\n<ProjectLayout slug="alpha">\n\nmore\n\n</ProjectLayout>\n')
    assert 'C3' in ids(check.check(tmp_path))


# C4
def test_duplicate_slug_fails(tmp_path):
    dup = [PROJECTS[0], {**PROJECTS[1], 'slug': 'alpha'}]
    assert 'C4' in ids(check.check(make_tree(tmp_path, projects=dup)))


def test_non_kebab_slug_fails(tmp_path):
    bad = [PROJECTS[0], {**PROJECTS[1], 'slug': 'Beta_Two'}]
    assert 'C4' in ids(check.check(make_tree(tmp_path, projects=bad)))


# C5
def test_unknown_group_fails(tmp_path):
    bad = [PROJECTS[0], {**PROJECTS[1], 'group': 'nope'}]
    assert 'C5' in ids(check.check(make_tree(tmp_path, projects=bad)))


def test_empty_group_fails(tmp_path):
    assert 'C5' in ids(check.check(make_tree(tmp_path, groups=GROUPS + [{'id': 'g3', 'label': 'Third'}])))


# C6
def test_unknown_module_fails(tmp_path):
    bad = [{**PROJECTS[0], 'modules': ['juneau-missing']}, PROJECTS[1]]
    assert 'C6' in ids(check.check(make_tree(tmp_path, projects=bad)))


def test_aggregator_module_fails(tmp_path):
    bad = [{**PROJECTS[0], 'modules': ['juneau']}, PROJECTS[1]]
    assert 'C6' in ids(check.check(make_tree(tmp_path, projects=bad)))


def test_shared_module_passes(tmp_path):
    shared = [{**PROJECTS[0], 'modules': ['juneau-a']}, {**PROJECTS[1], 'modules': ['juneau-a', 'juneau-b']}]
    assert check.check(make_tree(tmp_path, projects=shared)) == []


# C7
def test_unknown_docs_topic_fails(tmp_path):
    bad = [{**PROJECTS[0], 'docs': ['Nope']}, PROJECTS[1]]
    assert 'C7' in ids(check.check(make_tree(tmp_path, projects=bad)))


def test_topic_without_slug_uses_file_stem(tmp_path):
    make_tree(tmp_path)
    write(tmp_path, 'pages/topics/02.Stemmed.md', '# no front matter\n')
    ok = [{**PROJECTS[0], 'docs': ['02.Stemmed']}, PROJECTS[1]]
    write(tmp_path, 'src/data/projects.ts', render_projects_ts(GROUPS, ok, {}))
    assert check.check(tmp_path) == []


# C7: gettingStarted
def test_unknown_getting_started_topic_fails(tmp_path):
    projects = [{**PROJECTS[0], 'gettingStarted': 'Nope'}, PROJECTS[1]]
    errors = [e for e in check.check(make_tree(tmp_path, projects=projects)) if e.startswith('C7')]
    assert len(errors) == 1 and 'gettingStarted topic "Nope"' in errors[0]


def test_known_getting_started_topic_passes(tmp_path):
    projects = [{**PROJECTS[0], 'gettingStarted': 'Alpha'}, PROJECTS[1]]
    assert check.check(make_tree(tmp_path, projects=projects)) == []


# C13
def test_module_docs_valid_passes(tmp_path):
    assert check.check(make_tree(tmp_path, module_docs={'juneau-a': 'Alpha'})) == []


def test_module_docs_unlisted_module_fails(tmp_path):
    assert 'C13' in ids(check.check(make_tree(tmp_path, module_docs={'juneau-zzz': 'Alpha'})))


def test_module_docs_unknown_topic_fails(tmp_path):
    assert 'C13' in ids(check.check(make_tree(tmp_path, module_docs={'juneau-a': 'Nope'})))


def test_module_docs_absent_is_c0(tmp_path):
    make_tree(tmp_path)
    write(tmp_path, 'src/data/projects.ts', render_projects_ts(GROUPS, PROJECTS))
    assert 'C0' in ids(check.check(tmp_path))


# C8
def test_name_prefix_and_tagline(tmp_path):
    bad = [{**PROJECTS[0], 'name': 'Alpha'}, {**PROJECTS[1], 'tagline': ''}]
    errors = check.check(make_tree(tmp_path, projects=bad))
    assert len([e for e in errors if e.startswith('C8')]) == 2


# C9
def test_old_name_fails(tmp_path):
    make_tree(tmp_path)
    write(tmp_path, 'src/pages/about/alpha.mdx', page_text(PROJECTS[0], body='Use ' + old('juneau', 'run') + ' now.'))
    errors = [e for e in check.check(tmp_path) if e.startswith('C9')]
    assert len(errors) == 1 and 'alpha.mdx:' in errors[0] and old('juneau', 'run') in errors[0]
    make_tree(tmp_path / 'two')
    ts = tmp_path / 'two' / 'src/data/projects.ts'
    ts.write_text(ts.read_text().replace("'juneau-a'", "'" + old('juneau', 'test') + "'", 1))
    assert any(e.startswith('C9') and 'projects.ts' in e for e in check.check(tmp_path / 'two'))


# C10
def test_group_order_fails(tmp_path):
    assert 'C10' in ids(check.check(make_tree(tmp_path, projects=list(reversed(PROJECTS)))))


def test_group_not_contiguous_fails(tmp_path):
    three = [PROJECTS[0], PROJECTS[1], {**PROJECTS[0], 'slug': 'gamma', 'name': 'Juneau Gamma'}]
    assert 'C10' in ids(check.check(make_tree(tmp_path, projects=three)))


# C11
def test_front_matter_mismatch_fails(tmp_path):
    make_tree(tmp_path)
    write(tmp_path, 'src/pages/about/alpha.mdx', page_text(PROJECTS[0], title='Other'))
    write(tmp_path, 'src/pages/about/beta-two.mdx', page_text(PROJECTS[1], description='Different.'))
    assert len([e for e in check.check(tmp_path) if e.startswith('C11')]) == 2


def test_quoted_front_matter_is_accepted(tmp_path):
    make_tree(tmp_path)
    page = page_text(PROJECTS[0]).replace('description: The first one.', 'description: "The first one."')
    write(tmp_path, 'src/pages/about/alpha.mdx', page)
    assert check.check(tmp_path) == []


def test_missing_hide_toc_fails(tmp_path):
    make_tree(tmp_path)
    write(tmp_path, 'src/pages/about/alpha.mdx', page_text(PROJECTS[0], toc=False))
    write(tmp_path, 'src/pages/about/index.mdx', OVERVIEW.replace('hide_table_of_contents: true\n', ''))
    assert len([e for e in check.check(tmp_path) if e.startswith('C11')]) == 2


# C12
def test_broken_highlight_link_fails(tmp_path):
    make_tree(tmp_path)
    write(tmp_path, 'src/pages/about/alpha.mdx', page_text(PROJECTS[0], body='See [x](/docs/topics/Nope/).'))
    errors = [e for e in check.check(tmp_path) if e.startswith('C12')]
    assert len(errors) == 1 and 'Nope' in errors[0]


# C0 and the entry point
def test_parse_error_is_reported(tmp_path):
    make_tree(tmp_path)
    ts = tmp_path / 'src/data/projects.ts'
    ts.write_text(ts.read_text().replace("slug: 'alpha',", "slug: 'al\\'pha',"))
    errors = check.check(tmp_path)
    assert len(errors) == 1 and errors[0].startswith('C0') and 'line' in errors[0]


def test_main_exit_codes(tmp_path, capsys):
    make_tree(tmp_path)
    assert check.main(['--docs-dir', str(tmp_path)]) == 0
    assert 'Project pages OK (2 projects)' in capsys.readouterr().out
    write(tmp_path, 'src/pages/about/alpha.mdx', page_text(PROJECTS[0], title='Other', body='See [x](/docs/topics/Nope/).'))
    assert check.main(['--docs-dir', str(tmp_path)]) == 1
    out = capsys.readouterr().out.strip().splitlines()
    assert len(out) == 2 and out[0].startswith('C11') and out[1].startswith('C12')


# ---------------------------------------------------------------------------
# J1047's old-names list (skipped until juneau is renamed and a checkout is found)
# ---------------------------------------------------------------------------

def _decode(entry):
    return entry['sep'].join(entry['parts'])


def test_old_names_match_j1047_list():
    from juneau_root import find_juneau_root
    root = find_juneau_root(REPO_ROOT, allow_missing=True)
    listing = Path(root, 'juneau-terminal/src/test/python/old_names.json') if root else None
    if listing is None or not listing.is_file():
        pytest.skip('no renamed juneau checkout with juneau-terminal/src/test/python/old_names.json')
    data = json.loads(listing.read_text(encoding='utf-8'))
    assert set(data) >= {'entries', 'keep'}, f'unexpected shape: keys {sorted(data)}'
    misses = []
    for entry in data['entries']:
        if not names.find_old_names(_decode(entry)):
            misses.append(f'not flagged: {entry["label"]}')
    for entry in data['keep']:
        if names.find_old_names(_decode(entry)):
            misses.append(f'wrongly flagged: {entry["label"]}')
    assert not misses, '\n'.join(misses)


# ---------------------------------------------------------------------------
# The real tree (Phase B: needs src/pages/about/index.mdx)
# ---------------------------------------------------------------------------

@pytest.mark.skipif(not (REPO_ROOT / 'src/pages/about/index.mdx').exists(), reason='project pages not written yet')
def test_real_tree_passes():
    assert check.check(REPO_ROOT) == []
