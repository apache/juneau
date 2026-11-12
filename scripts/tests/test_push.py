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
        self.said = []

    def cmd(self, cmd, **kw):
        self.cmds.append((kw['id'], list(cmd)))
        subprocess.run(cmd, cwd=kw.get('cwd'), check=True, capture_output=True)

    def skip(self, id, label, title, reason, **kw):
        self.skips.append((id, reason))

    def say(self, text='', level='info'):
        self.said.append(text)


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


# ---------------------------------------------------------------------------------------
# AI artifacts: regenerated after the Prechecks pass, and only that run's own output is staged.
# ---------------------------------------------------------------------------------------
FAKE_CHECK = ("import pathlib, sys\n"
              "root = pathlib.Path(__file__).resolve().parent.parent\n"
              "sys.exit(0 if (root / 'static' / 'llms.txt').read_text() == 'new' else 1)\n")


def fake_generator(extra=''):
    return ("import pathlib\n"
            "root = pathlib.Path(__file__).resolve().parent.parent\n"
            "(root / 'static' / 'ai').mkdir(parents=True, exist_ok=True)\n"
            "(root / 'static' / 'llms.txt').write_text('new')\n"
            "(root / 'static' / 'ai' / 'manifest.json').write_text('{}')\n" + extra)


@pytest.fixture
def docs_ai(docs, tmp_path):
    """docs with committed fake check/generate scripts and a stale static/llms.txt; returns (docs, juneau_root)."""
    (docs / 'scripts').mkdir()
    (docs / 'scripts' / 'check-ai-artifacts.py').write_text(FAKE_CHECK, encoding='utf-8')
    (docs / 'scripts' / 'generate-ai-artifacts.py').write_text(fake_generator(), encoding='utf-8')
    (docs / 'static').mkdir()
    (docs / 'static' / 'llms.txt').write_text('old', encoding='utf-8')
    git(docs, 'add', '-A')
    git(docs, 'commit', '-q', '-m', 'ai fixtures')
    juneau = tmp_path / 'juneau'
    juneau.mkdir()
    return docs, juneau


def test_fresh_ai_artifacts_are_left_alone(docs_ai):
    docs, juneau = docs_ai
    (docs / 'static' / 'llms.txt').write_text('new', encoding='utf-8')
    git(docs, 'commit', '-q', '-am', 'fresh')
    view = RecordingView()

    assert push.regenerate_ai_artifacts(view, docs, juneau) == (0, [])

    assert git(docs, 'status', '--porcelain') == ''


def test_stale_ai_artifacts_are_regenerated_and_exactly_those_paths_staged(docs_ai):
    docs, juneau = docs_ai
    (docs / 'a.md').write_text('reviewed\n', encoding='utf-8')
    git(docs, 'add', 'a.md')                       # the operator's own staged work
    view = RecordingView()

    code, paths = push.regenerate_ai_artifacts(view, docs, juneau)

    assert code == 0 and paths == ['static/ai/manifest.json', 'static/llms.txt']
    assert git(docs, 'diff', '--cached', '--name-only').splitlines() == ['a.md', *paths]
    assert git(docs, 'status', '--porcelain') .count('\n') == 2        # nothing left unstaged
    said = '\n'.join(view.said)
    assert 'static/llms.txt' in said and 'static/ai/manifest.json' in said


def test_a_generator_that_touches_anything_else_is_refused_and_stages_nothing(docs_ai):
    docs, juneau = docs_ai
    (docs / 'scripts' / 'generate-ai-artifacts.py').write_text(
        fake_generator("(root / 'a.md').write_text('clobbered')\n"), encoding='utf-8')
    git(docs, 'commit', '-q', '-am', 'bad generator')
    view = RecordingView()

    code, paths = push.regenerate_ai_artifacts(view, docs, juneau)

    assert code == 1 and paths == []
    assert git(docs, 'diff', '--cached', '--name-only') == ''


def test_a_path_the_operator_already_staged_is_refused(docs_ai):
    docs, juneau = docs_ai
    (docs / 'static' / 'llms.txt').write_text('hand edit', encoding='utf-8')
    git(docs, 'add', 'static/llms.txt')

    code, paths = push.regenerate_ai_artifacts(RecordingView(), docs, juneau)

    assert code == 1 and paths == []
    assert git(docs, 'diff', '--cached', '--name-only') == 'static/llms.txt'      # left staged, untouched


def test_a_failing_generator_fails_the_step(docs_ai):
    docs, juneau = docs_ai
    (docs / 'scripts' / 'generate-ai-artifacts.py').write_text('import sys\nsys.exit(3)\n', encoding='utf-8')

    assert push.regenerate_ai_artifacts(RecordingView(), docs, juneau) == (1, [])


def test_without_a_juneau_checkout_the_step_is_skipped(docs_ai):
    docs, _ = docs_ai
    view = RecordingView()

    assert push.regenerate_ai_artifacts(view, docs, None) == (0, [])

    assert view.skips == [('ai-artifacts', 'no Juneau checkout')]


def test_unstage_regenerated_unstages_exactly_those_paths(docs_ai):
    docs, juneau = docs_ai
    (docs / 'a.md').write_text('reviewed\n', encoding='utf-8')
    git(docs, 'add', 'a.md')
    _, paths = push.regenerate_ai_artifacts(RecordingView(), docs, juneau)

    push.unstage_regenerated(docs, paths)

    assert git(docs, 'diff', '--cached', '--name-only') == 'a.md'


def test_a_run_that_fails_before_the_commit_unstages_what_it_regenerated(docs_ai, monkeypatch):
    docs, juneau = docs_ai

    def failing_steps(args, view, stage, docs_dir, juneau_root, regenerated):
        regenerated.extend(push.regenerate_ai_artifacts(view, docs_dir, juneau_root)[1])
        return 1
    monkeypatch.setattr(push, '_run_steps', failing_steps)

    assert push.push_steps(argparse.Namespace(), RecordingView(), stage, docs, juneau) == 1

    assert git(docs, 'diff', '--cached', '--name-only') == ''


def test_a_run_that_raises_before_the_commit_unstages_too(docs_ai, monkeypatch):
    docs, juneau = docs_ai

    def raising_steps(args, view, stage, docs_dir, juneau_root, regenerated):
        regenerated.extend(push.regenerate_ai_artifacts(view, docs_dir, juneau_root)[1])
        raise KeyboardInterrupt
    monkeypatch.setattr(push, '_run_steps', raising_steps)

    with pytest.raises(KeyboardInterrupt):
        push.push_steps(argparse.Namespace(), RecordingView(), stage, docs, juneau)

    assert git(docs, 'diff', '--cached', '--name-only') == ''


def test_a_successful_run_leaves_the_regenerated_paths_staged_for_the_commit(docs_ai, monkeypatch):
    docs, juneau = docs_ai

    def ok_steps(args, view, stage, docs_dir, juneau_root, regenerated):
        regenerated.extend(push.regenerate_ai_artifacts(view, docs_dir, juneau_root)[1])
        return 0
    monkeypatch.setattr(push, '_run_steps', ok_steps)

    assert push.push_steps(argparse.Namespace(), RecordingView(), stage, docs, juneau) == 0

    assert git(docs, 'diff', '--cached', '--name-only').splitlines() == ['static/ai/manifest.json', 'static/llms.txt']


def test_the_plan_draws_the_ai_artifacts_row_right_after_the_prechecks():
    ids = [id for id, _ in push.push_plan(argparse.Namespace(no_stage=True))]
    assert ids[:2] == ['prechecks', 'ai-artifacts']


def test_the_dry_run_names_the_one_staging_exception(docs, capsys):
    push.dry_run(argparse.Namespace(no_stage=False, message='m'), terminal_view.View(), stage, docs)
    out = capsys.readouterr().out
    assert 'check-ai-artifacts.py' in out and 'generate-ai-artifacts.py' in out
    assert 'stages nothing else' in out


JD = 'static/javadocs/10.0.0/'


@pytest.fixture
def docs_jd(docs):
    """docs with committed javadoc pages and a non-javadoc tracked file the 'build' will rewrite."""
    (docs / 'static' / 'javadocs' / '10.0.0').mkdir(parents=True)
    for name in ('A.html', 'B.html', 'Gone.html'):
        (docs / JD / name).write_text('old', encoding='utf-8')
    (docs / 'build-map.json').write_text('{}', encoding='utf-8')
    git(docs, 'add', '-A')
    git(docs, 'commit', '-q', '-m', 'javadoc fixtures')
    return docs


def fake_build(docs):
    (docs / JD / 'A.html').write_text('new', encoding='utf-8')
    (docs / JD / 'New.html').write_text('new', encoding='utf-8')
    (docs / JD / 'Gone.html').unlink()
    (docs / 'build-map.json').write_text('{"x":1}', encoding='utf-8')


def test_built_javadocs_are_staged_and_committed_but_other_build_output_is_not(docs_jd):
    fake_build(docs_jd)
    view = RecordingView()

    code, paths = push.stage_built_javadocs(view, docs_jd, set())

    assert code == 0
    assert sorted(paths) == [JD + 'A.html', JD + 'Gone.html', JD + 'New.html']
    assert sorted(git(docs_jd, 'diff', '--cached', '--name-only').splitlines()) == sorted(paths)
    assert any('Staged 3 regenerated javadoc files' in s for s in view.said)
    assert git(docs_jd, 'diff', '--name-only') == 'build-map.json'
    push.commit_and_push(view, docs_jd, 'm')
    assert sorted(git(docs_jd, 'show', '--name-only', '--format=', 'HEAD').splitlines()) == sorted(paths)
    assert git(docs_jd, 'diff', '--name-only') == 'build-map.json'


def test_nothing_built_stages_nothing(docs_jd):
    assert push.stage_built_javadocs(RecordingView(), docs_jd, set()) == (0, [])
    assert git(docs_jd, 'diff', '--cached', '--name-only') == ''


def test_a_failed_run_unstages_new_javadoc_paths_but_not_operator_staged_ones(docs_jd, monkeypatch):
    (docs_jd / JD / 'B.html').write_text('reviewed', encoding='utf-8')
    git(docs_jd, 'add', JD + 'B.html')

    def failing_steps(args, view, stage, docs_dir, juneau_root, regenerated):
        (docs_dir / JD / 'B.html').write_text('rebuilt', encoding='utf-8')
        fake_build(docs_dir)
        regenerated.extend(push.stage_built_javadocs(view, docs_dir, {JD + 'B.html'})[1])
        assert JD + 'B.html' not in regenerated
        return 1
    monkeypatch.setattr(push, '_run_steps', failing_steps)

    assert push.push_steps(argparse.Namespace(), RecordingView(), stage, docs_jd, None) == 1

    assert git(docs_jd, 'diff', '--cached', '--name-only') == JD + 'B.html'
