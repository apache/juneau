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
"""Tests for apply-renames.py."""

import json
from pathlib import Path

from conftest import load_script

ar = load_script('apply-renames.py')

# A fold rule pair: anchored links keep their anchor, bare links get the new section anchor.
FOLD_RULES = [
    {"comment": "fold, anchored", "pattern": r"/docs/topics/OldSlug#", "replacement": "/docs/topics/Parent#",
     "is_regex": True},
    {"comment": "fold, bare", "pattern": r"/docs/topics/OldSlug(?![\w#-])", "replacement": "/docs/topics/Parent#old-slug",
     "is_regex": True},
]


def write(root, rel, text):
    path = Path(root, rel)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)
    return path


def table(tmp_path, rules=FOLD_RULES):
    path = tmp_path / 'rules.json'
    path.write_text(json.dumps(rules))
    return str(path)


def run(tmp_path, *extra):
    return ar.main(['--docs-dir', str(tmp_path), '--table', table(tmp_path), *extra])


def test_fold_rules_respect_word_boundary_and_anchor(tmp_path):
    page = write(tmp_path, 'pages/topics/03.01.A.md',
                 'See [x](/docs/topics/OldSlug), [y](/docs/topics/OldSlug#detail) and [z](/docs/topics/OldSlugLonger).\n')
    assert run(tmp_path) == 0
    assert page.read_text() == ('See [x](/docs/topics/Parent#old-slug), [y](/docs/topics/Parent#detail) and '
                                '[z](/docs/topics/OldSlugLonger).\n')


def test_code_fences_and_inline_code_are_not_rewritten(tmp_path):
    text = 'Link /docs/topics/OldSlug.\n\n```text\n/docs/topics/OldSlug\n```\n\nInline `/docs/topics/OldSlug`.\n'
    page = write(tmp_path, 'pages/topics/03.01.A.md', text)
    assert run(tmp_path) == 0
    assert page.read_text() == text.replace('Link /docs/topics/OldSlug.', 'Link /docs/topics/Parent#old-slug.')


def test_inline_triple_backticks_round_trip_unchanged(tmp_path):
    # Inline code that quotes a fence marker used to be corrupted by mask/restore (02.09.AiKnowledgeAuthoring.md).
    text = 'Every ` ```java ` fence is parsed.\n\n````markdown\n```java fragment\nx\n```\n````\n'
    page = write(tmp_path, 'pages/developer-info/02.09.A.md', text)
    assert run(tmp_path) == 0
    assert page.read_text() == text


def test_historical_release_notes_are_skipped(tmp_path):
    old = write(tmp_path, 'pages/release-notes/9.2.0.md', '/docs/topics/OldSlug\n')
    new = write(tmp_path, 'pages/release-notes/10.0.0.md', '/docs/topics/OldSlug\n')
    assert run(tmp_path) == 0
    assert old.read_text() == '/docs/topics/OldSlug\n'
    assert new.read_text() == '/docs/topics/Parent#old-slug\n'


def test_readme_and_site_pages_are_included(tmp_path):
    write(tmp_path, 'pages/topics/03.01.A.md', 'none\n')
    readme = write(tmp_path, 'README.md', '/docs/topics/OldSlug\n')
    about = write(tmp_path, 'src/pages/about.md', '/docs/topics/OldSlug\n')
    home = write(tmp_path, 'src/pages/index.tsx', '<Link to="/docs/topics/OldSlug">Go</Link>\n')
    css = write(tmp_path, 'src/pages/index.module.css', '/docs/topics/OldSlug\n')
    assert run(tmp_path) == 0
    assert readme.read_text() == about.read_text() == '/docs/topics/Parent#old-slug\n'
    assert home.read_text() == '<Link to="/docs/topics/Parent#old-slug">Go</Link>\n'
    assert css.read_text() == '/docs/topics/OldSlug\n'


def test_dry_run_writes_nothing_and_reports(tmp_path, capsys):
    page = write(tmp_path, 'pages/topics/03.01.A.md', '/docs/topics/OldSlug\n')
    assert run(tmp_path, '--dry-run') == 0
    assert page.read_text() == '/docs/topics/OldSlug\n'
    out = capsys.readouterr().out
    assert 'dry run, nothing written' in out
    assert 'Files would change (1):' in out
    assert 'pages/topics/03.01.A.md' in out


def test_literal_rule_from_custom_table(tmp_path):
    page = write(tmp_path, 'pages/topics/03.01.A.md', 'old text\n')
    rules = [{"comment": "literal", "pattern": "old text", "replacement": "new text", "is_regex": False}]
    assert ar.main(['--docs-dir', str(tmp_path), '--table', table(tmp_path, rules)]) == 0
    assert page.read_text() == 'new text\n'


def test_missing_table_or_pages_exits_2(tmp_path):
    assert ar.main(['--docs-dir', str(tmp_path), '--table', str(tmp_path / 'missing.json')]) == 2
    assert ar.main(['--docs-dir', str(tmp_path), '--table', table(tmp_path)]) == 2
