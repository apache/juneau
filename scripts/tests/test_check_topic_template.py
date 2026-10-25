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
"""Tests for check-topic-template.py."""

import json
from pathlib import Path

from conftest import load_script

ctt = load_script('check-topic-template.py')

FM = '---\ntitle: "T"\nslug: T\n---\n'
LONG = ' '.join(['word'] * 160)


def write_page(root, name, text):
    page = Path(root, 'pages', 'topics', name)
    page.parent.mkdir(parents=True, exist_ok=True)
    page.write_text(text)


def run_json(tmp_path, capsys, *extra):
    assert ctt.main(['--docs-dir', str(tmp_path), '--json', *extra]) == 0
    return json.loads(capsys.readouterr().out)


def by_file(report):
    return {p['file']: p for p in report['pages']}


def test_word_count_excludes_frontmatter_and_fences_and_counts_tags():
    text = FM + 'The <a href="x">Foo</a> class.\n\n```java\nimport a.b;\nint x;\n```\n\n<tree>\n<node-0>Bar</node-0>\n</tree>\n'
    m = ctt.measure(text)
    # The(1) <a(2) href="x">Foo</a>(3) class.(4) <tree>(5) <node-0>Bar</node-0>(6) </tree>(7)
    assert m['words'] == 7
    assert m['fences'] == 1


def test_empty_fence_and_tilde_fence_are_masked():
    text = FM + 'one\n```\n```\ntwo\n~~~\n## Not a heading\n~~~\nthree\n'
    m = ctt.measure(text)
    assert m['words'] == 3
    assert m['fences'] == 2
    assert m['h2'] == []


def test_stub_and_fenceless_pages_are_reported(tmp_path, capsys):
    write_page(tmp_path, '03.01.Stub.md', FM + 'Short page.\n\n```java\nint x;\n```\n')
    write_page(tmp_path, '03.02.NoFence.md', FM + LONG + '\n')
    report = run_json(tmp_path, capsys)
    pages = by_file(report)
    assert pages['03.01.Stub.md']['words'] == 2
    assert pages['03.02.NoFence.md']['fences'] == 0
    assert report['totals']['under_min'] == 1
    assert report['totals']['no_fence'] == 1
    assert report['totals']['stubs_by_chapter'] == {'03': 1}


def test_out_of_order_headings_are_reported(tmp_path, capsys):
    write_page(tmp_path, '03.01.Bad.md', FM + 'Lead.\n\n## See also\n\nx\n\n## Example\n\ny\n')
    report = run_json(tmp_path, capsys)
    page = by_file(report)['03.01.Bad.md']
    assert page['out_of_order'] is True
    assert page['template_headings'] == ['See also', 'Example']


def test_conforming_page_is_not_reported(tmp_path, capsys):
    write_page(tmp_path, '03.01.Good.md',
               FM + LONG + '\n\n## When to use it\n\nx\n\n## Examples\n\n```java\nclass A {}\n```\n\n'
               '## Configuration\n\ny\n\n## Other notes\n\nz\n\n## See also\n\n- a\n')
    report = run_json(tmp_path, capsys)
    page = by_file(report)['03.01.Good.md']
    assert page['template_headings'] == ['When to use it', 'Example', 'Options', 'See also']
    assert page['out_of_order'] is False
    assert report['totals']['under_min'] == 0
    assert report['totals']['no_fence'] == 0
    assert report['totals']['out_of_order'] == 0


def test_heading_inside_fence_is_ignored(tmp_path, capsys):
    write_page(tmp_path, '03.01.A.md', FM + '## See also\n\n```md\n## Example\n```\n')
    page = by_file(run_json(tmp_path, capsys))['03.01.A.md']
    assert page['template_headings'] == ['See also']
    assert page['out_of_order'] is False


def test_text_report_exits_zero_with_findings(tmp_path, capsys):
    write_page(tmp_path, '03.01.Stub.md', FM + 'Short.\n')
    assert ctt.main(['--docs-dir', str(tmp_path)]) == 0
    out = capsys.readouterr().out
    assert 'Under 150 words (1):' in out
    assert '03.01.Stub.md' in out
    assert 'Totals: 1 pages' in out


def test_chapter_restricts_pages(tmp_path, capsys):
    write_page(tmp_path, '03.01.A.md', FM + 'a\n')
    write_page(tmp_path, '10.01.B.md', FM + 'b\n')
    report = run_json(tmp_path, capsys, '--chapter', '10')
    assert [p['file'] for p in report['pages']] == ['10.01.B.md']


def test_min_words_option(tmp_path, capsys):
    write_page(tmp_path, '03.01.A.md', FM + 'one two three\n')
    assert run_json(tmp_path, capsys, '--min-words', '3')['totals']['under_min'] == 0
    assert run_json(tmp_path, capsys, '--min-words', '4')['totals']['under_min'] == 1


def test_missing_topics_dir_exits_2(tmp_path, capsys):
    assert ctt.main(['--docs-dir', str(tmp_path)]) == 2


def test_fence_re_shape_this_script_relies_on():
    # _closes reads FENCE_RE group 2 (the marker) and group 3 (the info string) from code_fences.py.
    m = ctt.FENCE_RE.match('  ````java title="A.java"')
    assert (m.group(2), m.group(3)) == ('````', 'java title="A.java"')
    assert ctt.FENCE_RE.match('```').group(3) == ''


def test_custom_ids_and_qualifiers_do_not_hide_template_headings():
    h2 = ctt.measure('## When to use it (vs. `MockRestClient`)\n\n## Example {#example-1}\n\n## See also\n')['h2']
    assert h2 == ['When to use it (vs. `MockRestClient`)', 'Example', 'See also']
    assert ctt.template_order(h2) == [0, 1, 3]
