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
"""Tests for check-guide-snippets.py."""

from pathlib import Path

import pytest

from conftest import load_script

snippets = load_script('check-guide-snippets.py')

GUIDE_PAGE = 'pages/guides/01.Demo.md'
JAVA_SRC = 'src/test/java/org/example/Demo_Test.java'
DEMO_JAVA = 'package org.example;\n\n// region body\n\tpublic int x() {\n\t\treturn 1;\n\t}\n// endregion body\n'


@pytest.fixture
def roots(tmp_path):
    """A docs root and a Juneau root holding an empty guides module."""
    docs, juneau = tmp_path / 'docs', tmp_path / 'juneau'
    (docs / 'pages').mkdir(parents=True)
    (juneau / snippets.MODULE).mkdir(parents=True)
    (juneau / 'pom.xml').write_text('<project/>')
    return docs, juneau


def write(root, rel, text):
    path = Path(root, rel)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)


def source(juneau, rel, text):
    write(juneau / snippets.MODULE, rel, text)


def run(roots, capsys):
    docs, juneau = roots
    code = snippets.main(['--docs-root', str(docs), '--juneau-root', str(juneau)])
    return code, capsys.readouterr().out


def test_matching_fence_passes(roots, capsys):
    docs, juneau = roots
    source(juneau, JAVA_SRC, DEMO_JAVA)
    write(docs, GUIDE_PAGE, '```java guide=Demo_Test.java#body\npublic int x() {\n    return 1;\n}\n```\n')
    code, out = run(roots, capsys)
    assert code == 0
    assert 'Guide fences checked: 1; failing: 0; regions unreferenced: 0' in out


def test_tabs_spaces_indent_and_blank_lines_are_normalized(roots, capsys):
    docs, juneau = roots
    source(juneau, JAVA_SRC, DEMO_JAVA)
    write(docs, GUIDE_PAGE, '```java title="Demo" guide=Demo_Test.java#body showLineNumbers\n\n'
                            '        public int x() {   \n            return 1;\n        }\n\n```\n')
    assert run(roots, capsys)[0] == 0


def test_normalize():
    assert snippets.normalize('\r\n\t a  \r\n\t   b\n\n') == 'a\n  b'


def test_mismatch_prints_diff(roots, capsys):
    docs, juneau = roots
    source(juneau, JAVA_SRC, DEMO_JAVA)
    write(docs, GUIDE_PAGE, 'intro\n\n```java guide=Demo_Test.java#body\npublic int x() {\n    return 2;\n}\n```\n')
    code, out = run(roots, capsys)
    assert code == 1
    assert f'{GUIDE_PAGE}:3: guide fence does not match Demo_Test.java#body' in out
    assert '    -    return 1;' in out
    assert '    +    return 2;' in out


def test_missing_source(roots, capsys):
    docs, _juneau = roots
    write(docs, GUIDE_PAGE, '```java guide=Nope.java#body\nx\n```\n')
    code, out = run(roots, capsys)
    assert code == 1
    assert 'guide source not found: `Nope.java`' in out


def test_ambiguous_basename(roots, capsys):
    docs, juneau = roots
    source(juneau, 'src/test/resources/A/pom.xml', '<!-- region pom -->\n<a/>\n<!-- endregion pom -->\n')
    source(juneau, 'src/test/resources/B/pom.xml', '<!-- region pom -->\n<b/>\n<!-- endregion pom -->\n')
    write(docs, GUIDE_PAGE, '```xml guide=pom.xml#pom\n<a/>\n```\n')
    code, out = run(roots, capsys)
    assert code == 1
    assert 'ambiguous guide source `pom.xml`: src/test/resources/A/pom.xml, src/test/resources/B/pom.xml' in out


def test_relative_path_and_non_java_markers(roots, capsys):
    docs, juneau = roots
    source(juneau, 'src/test/resources/A/pom.xml', '<!-- region pom -->\n<a/>\n<!-- endregion pom -->\n')
    source(juneau, 'src/test/resources/B/pom.xml', '<!-- region pom -->\n<b/>\n<!-- endregion pom -->\n')
    source(juneau, 'src/test/resources/A/app.ini', '# region settings\n[server]\nport = 8080\n# endregion settings\n')
    write(docs, GUIDE_PAGE, '```xml guide=B/pom.xml#pom\n<b/>\n```\n\n```ini guide=A/app.ini#settings\n[server]\nport = 8080\n```\n')
    code, out = run(roots, capsys)
    assert code == 0, out
    assert 'checked: 2; failing: 0; regions unreferenced: 1' in out


@pytest.mark.parametrize('text, message', [
    ('// region other\nx\n// endregion other\n', 'region not found: `Demo_Test.java#body`'),
    ('// region body\nx\n', 'region unterminated: `body`'),
    ('// region body\nx\n// endregion body\n// region body\ny\n// endregion body\n', 'duplicate region `body`'),
    ('// region body\n// region inner\nx\n// endregion inner\n// endregion body\n', 'nested region `inner` inside `body`'),
    ('// region body\nx\n// endregion\n', 'endregion mismatch: `endregion` closes `body`'),
    ('// region body\nx\n// endregion other\n', 'endregion mismatch: `endregion other` closes `body`'),
])
def test_region_errors(roots, capsys, text, message):
    docs, juneau = roots
    source(juneau, JAVA_SRC, text)
    write(docs, GUIDE_PAGE, '```java guide=Demo_Test.java#body\nx\n```\n')
    code, out = run(roots, capsys)
    assert code == 1
    assert message in out


def test_two_pointers(roots, capsys):
    docs, juneau = roots
    source(juneau, JAVA_SRC, DEMO_JAVA)
    write(docs, GUIDE_PAGE, '```java guide=Demo_Test.java#body guide=Demo_Test.java#body\nx\n```\n')
    code, out = run(roots, capsys)
    assert code == 1
    assert 'more than one guide= pointer' in out


def test_guide_page_fence_without_pointer(roots, capsys):
    docs, _juneau = roots
    write(docs, GUIDE_PAGE, '```bash\nmvn test\n```\n\n```text\nok\n```\n\n```json\n{}\n```\n')
    write(docs, 'pages/guides/00.Guides.md', '```json\n{}\n```\n')
    write(docs, 'pages/topics/01.Topic.md', '```java\nint x;\n```\n')
    code, out = run(roots, capsys)
    assert code == 1
    assert f'{GUIDE_PAGE}:9: guide fence without pointer' in out
    assert '00.Guides.md' not in out
    assert '01.Topic.md' not in out


def test_no_pointer_passes_without_module(tmp_path, capsys):
    docs, juneau = tmp_path / 'docs', tmp_path / 'juneau'
    write(docs, 'pages/topics/01.Topic.md', '```java\nint x;\n```\n')
    write(juneau, 'pom.xml', '<project/>')
    assert snippets.main(['--docs-root', str(docs), '--juneau-root', str(juneau)]) == 0
    assert 'Guide fences checked: 0; failing: 0; regions unreferenced: 0' in capsys.readouterr().out


def test_pointer_without_module_fails(tmp_path, capsys):
    docs, juneau = tmp_path / 'docs', tmp_path / 'juneau'
    write(docs, 'pages/topics/01.Topic.md', '```java guide=Demo_Test.java#body\nx\n```\n')
    write(juneau, 'pom.xml', '<project/>')
    assert snippets.main(['--docs-root', str(docs), '--juneau-root', str(juneau)]) == 1
    assert 'guide module not found' in capsys.readouterr().out
