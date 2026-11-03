#!/usr/bin/env python3
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
"""Tests for push.py: the Prechecks, the commit-and-push step against real git, and the dry run's plan."""

import argparse
import subprocess
import sys
from pathlib import Path

import pytest

import terminal_view
from conftest import load_script

push = load_script('push.py')
stage = push.load_sibling('release-docs-stage.py')


def git(repo, *args):
    return subprocess.run(['git', *args], cwd=repo, check=True, capture_output=True, text=True).stdout.strip()


@pytest.fixture
def docs(tmp_path):
    """A clone of a bare remote on branch docs, with the ASF identity configured."""
    remote = tmp_path / 'remote.git'
    git(tmp_path, 'init', '--bare', '-b', 'docs', str(remote))
    seed = tmp_path / 'seed'
    seed.mkdir()
    git(seed, 'init', '-b', 'docs', '.')
    git(seed, 'config', 'user.email', stage.REQUIRED_GIT_EMAIL)
    git(seed, 'config', 'user.name', 'Push Test')
    (seed / 'a.md').write_text('one\n', encoding='utf-8')
    git(seed, 'add', 'a.md')
    git(seed, 'commit', '-q', '-m', 'initial')
    git(seed, 'remote', 'add', 'origin', str(remote))
    git(seed, 'push', '-q', '-u', 'origin', 'docs')
    work = tmp_path / 'juneau-docs'
    git(tmp_path, 'clone', '-q', str(remote), str(work))
    git(work, 'config', 'user.email', stage.REQUIRED_GIT_EMAIL)
    git(work, 'config', 'user.name', 'Push Test')
    return work


class RecordingView(terminal_view.View):
    """A plain view that records rows instead of running commands."""

    def __init__(self):
        super().__init__()
        self.cmds = []
        self.skips = []

    def cmd(self, cmd, **kw):
        self.cmds.append((kw['id'], list(cmd)))
        subprocess.run(cmd, cwd=kw.get('cwd'), check=True, capture_output=True)

    def skip(self, id, label, title, reason, **kw):
        self.skips.append((id, reason))

    def say(self, text='', level='info'):
        pass


def test_identity_other_than_apache_fails(docs):
    git(docs, 'config', 'user.email', 'someone@example.com')
    status, reason, _ = stage.identity_problem(docs)
    assert status == 'fail' and 'someone@example.com' in reason


def test_a_branch_other_than_docs_fails(docs):
    assert push.branch_problem(docs) is None
    git(docs, 'checkout', '-q', '-b', 'topic')
    status, reason, hints = push.branch_problem(docs)
    assert status == 'fail' and 'topic' in reason and hints == ('git checkout docs',)


def test_unstaged_and_untracked_files_are_unreviewed_but_staged_and_ignored_are_not(docs):
    assert push.unreviewed_problem(docs) is None
    (docs / 'a.md').write_text('two\n', encoding='utf-8')
    (docs / 'new.md').write_text('new\n', encoding='utf-8')
    (docs / '.gitignore').write_text('ignored.txt\n', encoding='utf-8')
    (docs / 'ignored.txt').write_text('x\n', encoding='utf-8')
    git(docs, 'add', '.gitignore')
    status, reason, hints = push.unreviewed_problem(docs)
    assert status == 'fail' and reason.startswith('2 unreviewed paths')
    assert 'a.md' in hints and 'new.md' in hints and 'ignored.txt' not in hints and '.gitignore' not in hints
    git(docs, 'add', 'a.md', 'new.md')
    assert push.unreviewed_problem(docs) is None


def test_behind_upstream_fails(docs, tmp_path):
    other = tmp_path / 'other'
    git(tmp_path, 'clone', '-q', str(tmp_path / 'remote.git'), str(other))
    git(other, 'config', 'user.email', stage.REQUIRED_GIT_EMAIL)
    git(other, 'config', 'user.name', 'Other')
    (other / 'b.md').write_text('b\n', encoding='utf-8')
    git(other, 'add', 'b.md')
    git(other, 'commit', '-q', '-m', 'other')
    git(other, 'push', '-q')
    status, reason, _ = push.upstream_problem(docs)
    assert status == 'fail' and 'behind' in reason


def test_a_current_branch_with_an_upstream_passes(docs):
    assert push.upstream_problem(docs) is None


def test_commit_and_push_commits_the_index_as_found_and_pushes(docs, tmp_path):
    (docs / 'a.md').write_text('two\n', encoding='utf-8')
    git(docs, 'add', 'a.md')
    view = RecordingView()

    assert push.commit_and_push(view, docs, 'docs update') == 0

    assert [id for id, _ in view.cmds] == ['commit', 'push']
    assert git(docs, 'log', '-1', '--format=%s') == 'docs update'
    assert git(tmp_path / 'remote.git', 'rev-parse', 'docs') == git(docs, 'rev-parse', 'HEAD')


def test_commit_and_push_never_stages(docs):
    (docs / 'a.md').write_text('two\n', encoding='utf-8')
    view = RecordingView()

    assert push.commit_and_push(view, docs, 'msg') == 0

    assert view.cmds == [] and ('commit', 'nothing staged') in view.skips
    assert git(docs, 'diff', '--cached', '--name-only') == ''
    assert git(docs, 'diff', '--name-only') == 'a.md'     # still unstaged, untouched


def test_nothing_staged_but_unpushed_commits_are_pushed(docs, tmp_path):
    (docs / 'a.md').write_text('two\n', encoding='utf-8')
    git(docs, 'commit', '-q', '-am', 'local only')
    view = RecordingView()

    assert push.commit_and_push(view, docs, 'unused') == 0

    assert [id for id, _ in view.cmds] == ['push'] and ('commit', 'nothing staged') in view.skips
    assert git(tmp_path / 'remote.git', 'rev-parse', 'docs') == git(docs, 'rev-parse', 'HEAD')


def test_nothing_to_commit_or_push_skips_both(docs):
    view = RecordingView()
    assert push.commit_and_push(view, docs, 'msg') == 0
    assert view.cmds == [] and [id for id, _ in view.skips] == ['commit', 'push']


def test_push_plan_has_stage_rows_only_when_staging():
    ids = lambda **kw: [id for id, _ in push.push_plan(argparse.Namespace(no_stage=kw['no_stage']))]
    assert ids(no_stage=False)[-2:] == ['stage', 'url'] and 'commit' in ids(no_stage=False)
    assert 'stage' not in ids(no_stage=True) and ids(no_stage=True)[-2:] == ['commit', 'push']


def test_dry_run_runs_the_prechecks_and_changes_nothing(docs, capsys):
    (docs / 'a.md').write_text('two\n', encoding='utf-8')
    args = argparse.Namespace(no_stage=False, message='m')
    head = git(docs, 'rev-parse', 'HEAD')

    code = push.dry_run(args, terminal_view.View(), stage, docs)

    out = capsys.readouterr().out
    assert code == 1 and 'Steps that would be executed' in out and 'Unreviewed changes' in out
    assert git(docs, 'rev-parse', 'HEAD') == head


def test_push_never_calls_the_production_release():
    source = (Path(push.__file__)).read_text(encoding='utf-8')
    assert 'asf-site' not in source and "load_sibling('release-docs.py')" not in source
