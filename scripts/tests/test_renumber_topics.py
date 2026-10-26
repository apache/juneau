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
"""Tests for renumber-topics.py."""

import json
import re
from pathlib import Path

import pytest

from conftest import load_script

rt = load_script('renumber-topics.py')

# Chapter 03 has a hole at 03.02 (a folded page) and chapter 10 one at 10.02.  The shape is the production one: labels
# carry no numbers, `numbered([...])` and `group({...})` wrap items, and helper code with braces sits above `mainSidebar`.
SIDEBARS = """\
import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

function numbered(items) { return items; }
function group(category) { return category; }

const sidebars: SidebarsConfig = {
\tmainSidebar: [
\t\t{
\t\t\ttype: 'category',
\t\t\tlabel: 'Documentation',
\t\t\titems: numbered([
\t\t\t\t{
\t\t\t\t\ttype: 'category',
\t\t\t\t\tlabel: 'juneau-marshall',
\t\t\t\t\tlink: {
\t\t\t\t\t\ttype: 'doc',
\t\t\t\t\t\tid: 'topics/03.JuneauMarshall',
\t\t\t\t\t},
\t\t\t\t\titems: [
\t\t\t\t\t\t{
\t\t\t\t\t\t\ttype: 'doc',
\t\t\t\t\t\t\tid: 'topics/03.01.Alpha',
\t\t\t\t\t\t\tlabel: 'Alpha',
\t\t\t\t\t\t},
\t\t\t\t\t\t// A comment the renumber must keep.
\t\t\t\t\t\t{
\t\t\t\t\t\t\ttype: 'category',
\t\t\t\t\t\t\tlabel: 'Beta',
\t\t\t\t\t\t\tlink: {
\t\t\t\t\t\t\t\ttype: 'doc',
\t\t\t\t\t\t\t\tid: 'topics/03.03.Beta',
\t\t\t\t\t\t\t},
\t\t\t\t\t\t\titems: [
\t\t\t\t\t\t\t\t{
\t\t\t\t\t\t\t\t\ttype: 'doc',
\t\t\t\t\t\t\t\t\tid: 'topics/03.03.01.BetaOne',
\t\t\t\t\t\t\t\t\tlabel: 'Beta one',
\t\t\t\t\t\t\t\t},
\t\t\t\t\t\t\t\t{
\t\t\t\t\t\t\t\t\ttype: 'doc',
\t\t\t\t\t\t\t\t\tid: 'topics/03.03.02.BetaTwo',
\t\t\t\t\t\t\t\t\tlabel: 'Beta two',
\t\t\t\t\t\t\t\t},
\t\t\t\t\t\t\t],
\t\t\t\t\t\t},
\t\t\t\t\t\tgroup({
\t\t\t\t\t\t\ttype: 'category',
\t\t\t\t\t\t\tlabel: 'Extras',
\t\t\t\t\t\t\titems: [
\t\t\t\t\t\t\t\t{
\t\t\t\t\t\t\t\t\ttype: 'doc',
\t\t\t\t\t\t\t\t\tid: 'topics/03.04.Gamma',
\t\t\t\t\t\t\t\t\tlabel: 'Gamma',
\t\t\t\t\t\t\t\t},
\t\t\t\t\t\t\t],
\t\t\t\t\t\t}),
\t\t\t\t\t\t{
\t\t\t\t\t\t\ttype: 'ref',
\t\t\t\t\t\t\tid: 'topics/03.04.Gamma',
\t\t\t\t\t\t\tlabel: 'Gamma (shortcut)',
\t\t\t\t\t\t},
\t\t\t\t\t\t{
\t\t\t\t\t\t\ttype: 'doc',
\t\t\t\t\t\t\tid: 'guides/Intro',
\t\t\t\t\t\t\tlabel: 'Intro guide',
\t\t\t\t\t\t},
\t\t\t\t\t],
\t\t\t\t},
\t\t\t\t{
\t\t\t\t\ttype: 'category',
\t\t\t\t\tlabel: 'juneau-rest-server',
\t\t\t\t\tlink: {
\t\t\t\t\t\ttype: 'doc',
\t\t\t\t\t\tid: 'topics/10.RestServer',
\t\t\t\t\t},
\t\t\t\t\titems: [
\t\t\t\t\t\t{
\t\t\t\t\t\t\ttype: 'doc',
\t\t\t\t\t\t\tid: 'topics/10.01.Delta',
\t\t\t\t\t\t\tlabel: 'Delta',
\t\t\t\t\t\t},
\t\t\t\t\t\t{
\t\t\t\t\t\t\ttype: 'doc',
\t\t\t\t\t\t\tid: 'topics/10.03.Eps',
\t\t\t\t\t\t\tlabel: 'Eps',
\t\t\t\t\t\t},
\t\t\t\t\t],
\t\t\t\t},
\t\t\t]),
\t\t},
\t\t{
\t\t\ttype: 'category',
\t\t\tlabel: 'Developer Info',
\t\t\titems: numbered([
\t\t\t\t{
\t\t\t\t\ttype: 'doc',
\t\t\t\t\tid: 'developer-info/03.04.Gamma',
\t\t\t\t\tlabel: 'Gamma',
\t\t\t\t},
\t\t\t]),
\t\t},
\t] as SidebarsConfig[string],
};
"""

PAGES = {
    '03.00.JuneauMarshall.md': '---\nid: 03.JuneauMarshall\ntitle: "Marshall"\nslug: JuneauMarshall\n---\n\nChapter.\n',
    '03.01.Alpha.md': ('---\ntitle: "Alpha"\nslug: Alpha\npagination_next: topics/03.03.Beta\n---\n\n'
                       'See 03.03.01.BetaOne and 03.03.01.BetaOneExtra.\n'),
    '03.03.00.Beta.md': '---\nid: 03.03.Beta\ntitle: "Beta"\nslug: Beta\n---\n\nBeta.\n',
    '03.03.01.BetaOne.md': '---\ntitle: "Beta one"\nslug: BetaOne\n---\n\nOne.\n',
    '03.03.02.BetaTwo.md': '---\ntitle: "Beta two"\nslug: BetaTwo\n---\n\nTwo.\n',
    '03.04.Gamma.md': '---\ntitle: "Gamma"\nslug: Gamma\n---\n\nGamma.\n',
    '10.00.RestServer.md': '---\nid: 10.RestServer\ntitle: "REST"\nslug: RestServer\n---\n\nChapter.\n',
    '10.01.Delta.md': '---\ntitle: "Delta"\nslug: Delta\n---\n\nDelta.\n',
    '10.03.Eps.md': '---\ntitle: "Eps"\nslug: Eps\n---\n\nEps.\n',
}


@pytest.fixture
def docs(tmp_path):
    (tmp_path / 'sidebars.ts').write_text(SIDEBARS)
    topics = tmp_path / 'pages' / 'topics'
    topics.mkdir(parents=True)
    for name, text in PAGES.items():
        (topics / name).write_text(text)
    for rel in ('pages/release-notes/10.0.0.md', 'pages/developer-info/01.Notes.md', 'pages/guides/Intro.md'):
        path = tmp_path / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text('Mentions 03.04.Gamma, in pages/topics/03.04.Gamma.md.\n')
    (tmp_path / 'scripts').mkdir()
    (tmp_path / 'scripts' / 'verify-docs-allowlist.txt').write_text('# orphans\n03.04.Gamma\n')
    return tmp_path


def run(docs, *extra):
    return rt.main(['--docs-dir', str(docs), *extra])


def topic_names(docs):
    return sorted(p.name for p in (docs / 'pages' / 'topics').glob('*.md'))


def test_dry_run_reports_and_writes_nothing(docs, capsys):
    assert run(docs) == 0
    out = capsys.readouterr().out
    assert 'RENAME 03.03.00.Beta.md -> 03.02.00.Beta.md' in out
    assert 'ID     03.03.Beta -> 03.02.Beta' in out
    assert 'mapped 9 of 9 topic pages; 5 renamed, 0 unmapped; dry run, nothing written' in out
    assert topic_names(docs) == sorted(PAGES)
    assert (docs / 'sidebars.ts').read_text() == SIDEBARS


def test_apply_closes_gaps(docs, capsys):
    assert run(docs, '--apply') == 0
    assert topic_names(docs) == sorted([
        '03.00.JuneauMarshall.md', '03.01.Alpha.md', '03.02.00.Beta.md', '03.02.01.BetaOne.md', '03.02.02.BetaTwo.md',
        '03.03.Gamma.md', '10.00.RestServer.md', '10.01.Delta.md', '10.02.Eps.md'])
    topics = docs / 'pages' / 'topics'
    assert 'id: 03.02.Beta\n' in (topics / '03.02.00.Beta.md').read_text()
    assert not list(topics.glob('*' + rt.TMP_SUFFIX))


def test_apply_patches_sidebar_in_place(docs):
    assert run(docs, '--apply') == 0
    text = (docs / 'sidebars.ts').read_text()
    assert "id: 'topics/03.02.Beta'," in text
    assert "id: 'topics/03.02.02.BetaTwo'," in text
    assert "id: 'topics/10.02.Eps'," in text
    assert '// A comment the renumber must keep.' in text
    assert text.count("id: 'topics/03.03.Gamma',") == 2       # the doc and the ref
    assert "id: 'guides/Intro'," in text
    assert "id: 'developer-info/03.04.Gamma'," in text         # not a topics id: untouched
    labels = re.compile(r"label: '[^']*'")
    assert labels.findall(text) == labels.findall(SIDEBARS)    # labels are never rewritten
    assert text.count('\n') == SIDEBARS.count('\n')            # patched in place, not re-serialized


def test_apply_rewrites_references(docs):
    assert run(docs, '--apply') == 0
    alpha = (docs / 'pages' / 'topics' / '03.01.Alpha.md').read_text()
    assert 'pagination_next: topics/03.02.Beta\n' in alpha
    assert 'See 03.02.01.BetaOne and 03.03.01.BetaOneExtra.' in alpha   # a longer token is not a match
    assert (docs / 'pages' / 'guides' / 'Intro.md').read_text() == 'Mentions 03.03.Gamma, in pages/topics/03.03.Gamma.md.\n'
    assert (docs / 'scripts' / 'verify-docs-allowlist.txt').read_text() == '# orphans\n03.03.Gamma\n'
    unchanged = 'Mentions 03.04.Gamma, in pages/topics/03.04.Gamma.md.\n'
    assert (docs / 'pages' / 'release-notes' / '10.0.0.md').read_text() == unchanged
    assert (docs / 'pages' / 'developer-info' / '01.Notes.md').read_text() == unchanged


def test_second_run_is_a_no_op(docs, capsys):
    assert run(docs, '--apply') == 0
    capsys.readouterr()
    assert run(docs, '--json') == 0
    result = json.loads(capsys.readouterr().out)
    assert result['renames'] == [] and 'relabels' not in result


def test_chapter_scope(docs, capsys):
    assert run(docs, '--apply', '--chapter', '10') == 0
    names = topic_names(docs)
    assert '10.02.Eps.md' in names
    assert '03.03.00.Beta.md' in names and '03.04.Gamma.md' in names
    text = (docs / 'sidebars.ts').read_text()
    assert "id: 'topics/10.02.Eps'," in text and "id: 'topics/03.04.Gamma'," in text


def test_json_output(docs, capsys):
    assert run(docs, '--json') == 0
    result = json.loads(capsys.readouterr().out)
    assert result['applied'] is False
    assert {'old_file': '10.03.Eps.md', 'new_file': '10.02.Eps.md', 'old_id': '10.03.Eps', 'new_id': '10.02.Eps'} \
        in result['renames']


def test_unmapped_page_is_reported_and_left_alone(docs, capsys):
    (docs / 'pages' / 'topics' / '03.09.Orphan.md').write_text('---\ntitle: "O"\nslug: Orphan\n---\n')
    assert run(docs, '--apply') == 0
    assert 'UNMAPPED 03.09.Orphan.md' in capsys.readouterr().out
    assert (docs / 'pages' / 'topics' / '03.09.Orphan.md').exists()


def test_rename_onto_an_unmapped_page_is_refused(docs, capsys):
    (docs / 'pages' / 'topics' / '03.03.Gamma.md').write_text('---\ntitle: "G"\nslug: GammaOld\n---\n')
    assert run(docs, '--apply') == 1
    assert 'would overwrite 03.03.Gamma.md' in capsys.readouterr().err
    assert '03.04.Gamma.md' in topic_names(docs)


def test_dangling_sidebar_id_is_an_error(docs, capsys):
    (docs / 'pages' / 'topics' / '10.03.Eps.md').unlink()
    assert run(docs) == 1
    assert 'topics/10.03.Eps has no page' in capsys.readouterr().err


def test_missing_sidebars_exits_2(tmp_path):
    (tmp_path / 'pages' / 'topics').mkdir(parents=True)
    assert rt.main(['--docs-dir', str(tmp_path)]) == 2
