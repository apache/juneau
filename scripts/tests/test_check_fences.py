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
"""Tests for the code-fence checks (check-code-fences.py, check-java-fences.py, code_fences.py)."""
# ***************************************************************************************************************************

import shutil
from pathlib import Path

import pytest

from conftest import load_script
from code_fences import iter_fences, page_files

check_code_fences = load_script('check-code-fences.py')
check_java_fences = load_script('check-java-fences.py')

JAVA = check_java_fences.DEFAULT_JAVA


def write_page(root, name, text):
    page = Path(root, 'pages', 'topics', name)
    page.parent.mkdir(parents=True, exist_ok=True)
    page.write_text(text)


def test_page_files_includes_about_mdx(tmp_path):
    write_page(tmp_path, 'A.md', 'x\n')
    about = Path(tmp_path, 'src', 'pages', 'about')
    about.mkdir(parents=True)
    (about / 'marshall.mdx').write_text('```java\nint x;\n```\n')
    names = [p.relative_to(tmp_path).as_posix() for p in page_files(tmp_path)]
    assert names == ['pages/topics/A.md', 'src/pages/about/marshall.mdx']


def test_iter_fences_reports_line_info_and_body():
    text = 'intro\n:::tip\n```java fragment\nint x;\n```\n:::\n~~~\nraw\n~~~\n'
    assert list(iter_fences(text)) == [(3, 'java fragment', 'int x;'), (7, '', 'raw')]


def test_iter_fences_ignores_shorter_marker_inside_longer_fence():
    text = '````md\n```java\nx\n```\n````\n'
    assert list(iter_fences(text)) == [(1, 'md', '```java\nx\n```')]


def test_anchor_at_line_start_in_fence_is_found(tmp_path):
    write_page(tmp_path, 'A.md', 'text\n\n```java\n<a href="/x.html">@Request</a>\npublic interface A {}\n```\n')
    assert check_code_fences.find_anchors(tmp_path) == [('pages/topics/A.md', 4, '<a href="/x.html">@Request</a>')]


def test_anchor_in_prose_and_xml_elements_are_ignored(tmp_path):
    write_page(tmp_path, 'A.md',
               'See <a href="/x.html">x</a>.\n\n```xml\n<a>foo</a>\n```\n\n```java\nString s = "<a href=\'x\'>y</a>";\n```\n')
    assert check_code_fences.find_anchors(tmp_path) == []


def test_check_code_fences_main_exit_codes(tmp_path, capsys):
    write_page(tmp_path, 'A.md', '```java\nint x;\n```\n')
    assert check_code_fences.main(['--docs-root', str(tmp_path)]) == 0
    write_page(tmp_path, 'B.md', '```\n<a href="y">z</a>\n```\n')
    assert check_code_fences.main(['--docs-root', str(tmp_path)]) == 1
    assert 'B.md:2' in capsys.readouterr().out


def test_find_java_fences_skips_fragment_and_other_languages(tmp_path):
    write_page(tmp_path, 'A.md', '```java\nint x;\n```\n```java fragment\n...\n```\n```xml\n<a/>\n```\n')
    assert check_java_fences.find_java_fences(tmp_path) == [('pages/topics/A.md', 1, 'int x;')]


def test_find_java_fences_parse_checks_guide_fences_unless_fragment(tmp_path):
    # A guide= pointer is just another info-string word: the fence is still parse-checked, and `fragment` still opts out.
    page = Path(tmp_path, 'pages', 'guides', '01.Demo.md')
    page.parent.mkdir(parents=True)
    page.write_text('```java guide=Demo_Test.java#a\nint x;\n```\n```java guide=Demo_Test.java#b fragment\n...\n```\n')
    assert check_java_fences.find_java_fences(tmp_path) == [('pages/guides/01.Demo.md', 1, 'int x;')]


@pytest.mark.skipif(not (JAVA.exists() or shutil.which('java')), reason='needs a JDK')
def test_java_fence_forms_and_failures(tmp_path, capsys):
    java = JAVA if JAVA.exists() else Path(shutil.which('java'))
    write_page(tmp_path, 'A.md',
               '```java\npackage a;\npublic class A {}\n```\n'
               '```java\n@Rest\npublic String get() { return ""; }\n```\n'
               '```java\nint x = 1;\nfoo(x);\n```\n'
               '```java\npublic class {\n```\n'
               '```java fragment\npublic class {\n```\n')
    args = ['--docs-root', str(tmp_path), '--java', str(java)]
    assert check_java_fences.main(args) == 0
    out = capsys.readouterr().out
    assert 'A.md:13: java fence does not parse' in out
    assert 'checked: 4; failing: 1; passing: 3' in out
    assert check_java_fences.main(args + ['--strict']) == 1
