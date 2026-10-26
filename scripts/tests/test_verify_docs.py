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
"""Tests for verify-docs.py: the sidebars.ts parser, check 16 (guides), the guide cases of check 11 (links) and the check
selection."""

from pathlib import Path

import pytest

from conftest import load_script

verify = load_script('verify-docs.py')

TOPICS = {
    '00.00.GettingStarted.md': 'id: 00.GettingStarted\ntitle: "Getting Started"\nslug: GettingStarted',
    '00.01.GettingStartedInstall.md': 'title: "Install"\nslug: GettingStartedInstall',
    '10.01.ConsolePage.md': 'title: "Console Page"\nslug: ConsolePage',
}
GUIDES = {
    '00.Guides.md': 'id: 00.Guides\ntitle: "Guides"\nslug: /guides',
    '01.FirstGuide.md': 'id: 01.FirstGuide\ntitle: "First"\nslug: FirstGuide',
    '02.SecondGuide.md': 'id: 02.SecondGuide\ntitle: "Second"\nslug: SecondGuide',
}
GUIDE_ITEMS = ("{type: 'doc', id: 'guides/01.FirstGuide', label: 'First'},"
               "{type: 'doc', id: 'guides/02.SecondGuide', label: 'It\\'s second'},"
               "{type: 'ref', id: 'topics/10.01.ConsolePage', label: 'Console Page'},")

LANDING_BODY = ('Intro links [Getting Started](/docs/topics/GettingStarted).\n\n'
                '1. [First](/docs/guides/FirstGuide): one.\n'
                '2. [Second](/docs/guides/SecondGuide): two.\n'
                '3. [Console Page](/docs/topics/ConsolePage): an existing topic.\n')


# The helper code above `mainSidebar` holds braces and quotes the parser must never look at.
HELPERS = ("function numbered(items) { return items.map((i) => ({...i, label: `1. ${i.label}`})); }\n"
           "function group(category) { return {...category, customProps: {unnumbered: true}}; }\n")


def sidebar(guide_items=GUIDE_ITEMS, guide_categories=1):
    """The production shape: no Documentation wrapper around Getting Started and Guides, numbered() and group()."""
    guides = ("{type: 'category', label: 'Guides', link: {type: 'doc', id: 'guides/00.Guides'}, items: numbered(["
              + guide_items + "])},") * guide_categories
    return (HELPERS + "const sidebars = {mainSidebar: ["
            "{type: 'category', label: 'Getting Started', link: {type: 'doc', id: 'topics/00.GettingStarted'}, items: numbered(["
            "{type: 'doc', id: 'topics/00.01.GettingStartedInstall', label: 'Install'}])},"
            + guides +
            "{type: 'category', label: 'Documentation', items: numbered(["
            "group({type: 'category', label: 'Console', items: [{type: 'doc', id: 'topics/10.01.ConsolePage', label: 'Console'}]}),"
            "])},"
            "] as SidebarsConfig[string]};\nexport default sidebars;\n")


# The shape before the one-sidebar reorganization: a Documentation wrapper and typed numbers.
LEGACY_SIDEBAR = ("const sidebars = {mainSidebar: [{type: 'category', label: 'Documentation', items: ["
                  "{type: 'category', label: 'Getting Started', link: {type: 'doc', id: 'topics/00.GettingStarted'}, items: ["
                  "{type: 'doc', id: 'topics/00.01.GettingStartedInstall', label: '1. Install'}]},"
                  "{type: 'category', label: 'Console', items: [{type: 'doc', id: 'topics/10.01.ConsolePage', label: '10.1. Console'}]},"
                  "]}]};\nexport default sidebars;\n")


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)


@pytest.fixture
def docs(tmp_path):
    """A docs root with three topics, a valid Guides section and its sidebar category."""
    for name, fm in TOPICS.items():
        write(tmp_path / 'pages' / 'topics' / name, f'---\n{fm}\n---\n\nBody.\n')
    for name, fm in GUIDES.items():
        body = LANDING_BODY if name == '00.Guides.md' else 'Body.\n'
        write(tmp_path / 'pages' / 'guides' / name, f'---\n{fm}\n---\n\n{body}')
    write(tmp_path / 'sidebars.ts', sidebar())
    return tmp_path


def run(docs, checks):
    findings = verify.Findings()
    verify.run_checks(docs, {verify.NUM_BY_NAME[c] for c in checks}, False, findings)
    return [(i['num'], i['message'], i['file']) for i in findings.items if i['severity'] == 'error']


def messages(docs, checks):
    return [m for _n, m, _f in run(docs, checks)]


def test_valid_section_passes(docs):
    assert run(docs, ['guides', 'links', 'order', 'dangling', 'orphans', 'landing-00']) == []


def test_main_exit_code(docs):
    assert verify.main(['--docs-dir', str(docs), '--only', 'guides']) == 0
    write(docs / 'pages' / 'guides' / '03.ThirdGuide.md', '---\nid: 03.ThirdGuide\ntitle: "Third"\nslug: ThirdGuide\n---\n')
    assert verify.main(['--docs-dir', str(docs), '--only', 'guides']) == 1


def test_no_guides_dir_skips_check(docs):
    for p in (docs / 'pages' / 'guides').iterdir():
        p.unlink()
    (docs / 'pages' / 'guides').rmdir()
    assert run(docs, ['guides']) == []


def test_missing_frontmatter(docs):
    write(docs / 'pages' / 'guides' / '01.FirstGuide.md', '---\nid: 01.FirstGuide\nslug: FirstGuide\n---\n')
    assert (16, 'Missing/empty frontmatter `title`', 'guides/01.FirstGuide.md') in run(docs, ['guides'])


def test_bad_file_name(docs):
    write(docs / 'pages' / 'guides' / '3.bad.md', '---\nid: 3.bad\ntitle: "Bad"\nslug: bad\n---\n')
    assert (16, 'File name does not match `NN.Name.md`', 'guides/3.bad.md') in run(docs, ['guides'])


def test_id_must_equal_stem(docs):
    write(docs / 'pages' / 'guides' / '01.FirstGuide.md', '---\nid: FirstGuide\ntitle: "First"\nslug: FirstGuide\n---\n')
    assert 'id `FirstGuide` != file stem `01.FirstGuide`' in messages(docs, ['guides'])


def test_slug_must_equal_name_part(docs):
    write(docs / 'pages' / 'guides' / '01.FirstGuide.md', '---\nid: 01.FirstGuide\ntitle: "First"\nslug: First\n---\n')
    assert 'slug `First` != filename name part `FirstGuide`' in messages(docs, ['guides'])


def test_landing_slug_must_be_absolute_guides(docs):
    write(docs / 'pages' / 'guides' / '00.Guides.md', '---\nid: 00.Guides\ntitle: "Guides"\nslug: Guides\n---\n')
    assert 'Landing slug `Guides` != `/guides`' in messages(docs, ['guides'])


def test_missing_landing(docs):
    (docs / 'pages' / 'guides' / '00.Guides.md').unlink()
    assert 'Missing landing page pages/guides/00.Guides.md' in messages(docs, ['guides'])


@pytest.mark.parametrize('count', [0, 2])
def test_exactly_one_guides_category(docs, count):
    write(docs / 'sidebars.ts', sidebar(guide_categories=count))
    assert (f'Expected one sidebar category linking `guides/00.Guides`, found {count}'
            in messages(docs, ['guides']))


def test_sidebar_doc_without_file(docs):
    write(docs / 'sidebars.ts', sidebar(GUIDE_ITEMS + "{type: 'doc', id: 'guides/09.Gone', label: 'Gone'},"))
    assert 'Sidebar doc `guides/09.Gone` has no file in pages/guides/' in messages(docs, ['guides'])


def test_ref_must_resolve_to_a_topic(docs):
    write(docs / 'sidebars.ts', sidebar(GUIDE_ITEMS + "{type: 'ref', id: 'topics/99.Nope', label: 'Nope'},"))
    assert ('Sidebar ref `topics/99.Nope` in the Guides category does not resolve to a topic'
            in messages(docs, ['guides']))


def test_landing_topic_row_needs_a_ref(docs):
    # A regeneration that drops the ref must not pass: the landing still lists the topic as guide 3.
    items = ("{type: 'doc', id: 'guides/01.FirstGuide', label: 'First'},"
             "{type: 'doc', id: 'guides/02.SecondGuide', label: 'Second'},")
    write(docs / 'sidebars.ts', sidebar(items))
    assert messages(docs, ['guides']) == [
        'Landing lists `/docs/topics/ConsolePage` but the Guides category has no ref to it']


def test_refs_come_after_the_guides(docs):
    items = ("{type: 'doc', id: 'guides/01.FirstGuide', label: 'First'},"
             "{type: 'ref', id: 'topics/10.01.ConsolePage', label: 'Console Page'},"
             "{type: 'doc', id: 'guides/02.SecondGuide', label: 'Second'},")
    write(docs / 'sidebars.ts', sidebar(items))
    assert messages(docs, ['guides']) == [
        'Sidebar doc `guides/02.SecondGuide` follows a ref; the Guides category lists its refs last']


def test_guide_not_listed(docs):
    write(docs / 'pages' / 'guides' / '03.ThirdGuide.md', '---\nid: 03.ThirdGuide\ntitle: "Third"\nslug: ThirdGuide\n---\n')
    assert (16, 'Guide is not listed in the sidebar Guides category', 'guides/03.ThirdGuide.md') in run(docs, ['guides'])


def test_guide_listed_twice(docs):
    write(docs / 'sidebars.ts', sidebar(GUIDE_ITEMS + "{type: 'doc', id: 'guides/02.SecondGuide', label: 'Again'},"))
    assert 'Guide is listed 2 times in the sidebar Guides category' in messages(docs, ['guides'])


def test_listed_order_must_be_file_order(docs):
    items = ("{type: 'doc', id: 'guides/02.SecondGuide', label: 'Second'},"
             "{type: 'doc', id: 'guides/01.FirstGuide', label: 'First'},")
    write(docs / 'sidebars.ts', sidebar(items))
    assert ("Guides category order != file order: ['02.SecondGuide', '01.FirstGuide']"
            in messages(docs, ['guides']))


def test_guide_ids_stay_out_of_topic_checks(docs):
    # The Guides category sits after Getting Started but its files sort among nothing in pages/topics/:
    # checks 5-9 must neither count nor order them.
    assert run(docs, ['landing-00', 'dangling', 'orphans', 'counts', 'order']) == []


@pytest.mark.parametrize('link, error', [
    ('/docs/guides/FirstGuide', None),
    ('/docs/guides/FirstGuide#step-1', None),
    ('/docs/guides', None),
    ('/docs/guides/', None),
    ('/docs/guides/NoSuchGuide', 'Broken guide link `/docs/guides/NoSuchGuide`'),
    ('/docs/topics/ConsolePage', None),
])
def test_absolute_guide_links_from_a_topic(docs, link, error):
    write(docs / 'pages' / 'topics' / '10.01.ConsolePage.md',
          f'---\ntitle: "Console Page"\nslug: ConsolePage\n---\n\nSee [the guide]({link}).\n')
    assert messages(docs, ['links']) == ([error] if error else [])


@pytest.mark.parametrize('link, error', [
    ('SecondGuide', 'Relative guide link `SecondGuide`; use `/docs/guides/SecondGuide`'),
    ('ConsolePage', 'Relative topic link `ConsolePage`; use `/docs/topics/ConsolePage`'),
    ('NoSuchPage', 'Broken relative topic link `NoSuchPage`'),
    ('/docs/topics/NoSuchPage', 'Broken topic link `/docs/topics/NoSuchPage`'),
])
def test_links_from_a_guide_page(docs, link, error):
    write(docs / 'pages' / 'guides' / '01.FirstGuide.md',
          f'---\nid: 01.FirstGuide\ntitle: "First"\nslug: FirstGuide\n---\n\nSee [next]({link}).\n')
    assert (11, error, 'pages/guides/01.FirstGuide.md') in run(docs, ['links'])


def test_relative_links_in_a_topic_page_still_flagged(docs):
    write(docs / 'pages' / 'topics' / '10.01.ConsolePage.md',
          '---\ntitle: "Console Page"\nslug: ConsolePage\n---\n\nSee [install](GettingStartedInstall).\n')
    assert messages(docs, ['links']) == [
        'Relative topic link `GettingStartedInstall`; use `/docs/topics/GettingStartedInstall`']


def test_collect_section_returns_nested_items():
    tree = [{'type': 'category', 'link': {'type': 'doc', 'id': 'guides/00.Guides'}, 'items': [
        {'type': 'doc', 'id': 'guides/01.A'},
        {'type': 'category', 'label': 'More', 'items': [{'type': 'ref', 'id': 'topics/02.B'}]},
    ]}, {'type': 'doc', 'id': 'topics/03.C'}]
    assert verify.collect_section(tree, 'guides/00.Guides') == [[('doc', 'guides/01.A'), ('ref', 'topics/02.B')]]


def test_parser_reads_through_numbered_and_group():
    tree = verify.parse_sidebars(sidebar())
    assert [n['label'] for n in tree] == ['Getting Started', 'Guides', 'Documentation']
    doc_refs, landing_ids, preorder = verify.collect_sidebar(tree)
    assert preorder == ['00.GettingStarted', '00.01.GettingStartedInstall', '10.01.ConsolePage']
    assert doc_refs == ['00.01.GettingStartedInstall', '10.01.ConsolePage']
    assert landing_ids == ['00.GettingStarted']


def test_parser_still_reads_the_legacy_shape():
    _refs, _landing, preorder = verify.collect_sidebar(verify.parse_sidebars(LEGACY_SIDEBAR))
    assert preorder == ['00.GettingStarted', '00.01.GettingStartedInstall', '10.01.ConsolePage']


@pytest.mark.parametrize('call, error', [
    ("foo([{type: 'doc', id: 'topics/x'}])", 'unsupported call `foo(`'),
    ("numbered([], 2)", 'expected `)` closing `numbered(`'),
])
def test_parser_rejects_other_calls(call, error):
    with pytest.raises(ValueError, match=error.replace('(', r'\(').replace(')', r'\)')):
        verify.parse_sidebars("const s = {mainSidebar: [{type: 'category', label: 'X', items: " + call + "}]};")


@pytest.mark.parametrize('selection', ['labels', '10'])
def test_retired_check_10_is_rejected(docs, selection):
    assert verify.main(['--docs-dir', str(docs), '--only', selection]) == 2
    assert verify.main(['--docs-dir', str(docs), '--skip', selection]) == 2


LOCK_OK = '{"packages": {"node_modules/a": {"resolved": "https://registry.npmjs.org/a/-/a-1.0.0.tgz"}}}\n'


def test_lockfile_on_public_registry_passes(docs):
    write(docs / 'package-lock.json', LOCK_OK)
    assert run(docs, ['lockfile']) == []


def test_lockfile_absent_is_skipped(docs):
    assert run(docs, ['lockfile']) == []


def test_lockfile_other_host_fails(docs):
    write(docs / 'package-lock.json', LOCK_OK.replace('https://registry.npmjs.org/', 'https://mirror.example.net/npm/'))
    assert run(docs, ['lockfile']) == [(17, '`resolved` URL is not on the public npm registry: '
                                            'https://mirror.example.net/npm/a/-/a-1.0.0.tgz', 'package-lock.json')]
    assert verify.main(['--docs-dir', str(docs), '--only', 'lockfile']) == 1
