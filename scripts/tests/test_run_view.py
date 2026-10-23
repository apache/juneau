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
"""The juneau-run adapter: loading the toolkit, the plain fallback, and the scripts' row plans."""

import argparse
import contextlib
import os
import subprocess
import sys
import types
from pathlib import Path

import pytest

import run_view
from conftest import SCRIPTS_DIR, load_script

build_docs = load_script('build-docs.py')
release_stage = load_script('release-docs-stage.py')
release = load_script('release-docs.py')


def build_args(**flags):
    names = ('skip_npm', 'skip_maven', 'skip_copy', 'skip_verify')
    return argparse.Namespace(**{name: flags.get(name, False) for name in names})


def git(repo, *args):
    subprocess.run(['git', *args], cwd=repo, check=True, capture_output=True)


@pytest.fixture
def repo(tmp_path):
    repo = tmp_path / 'repo'
    repo.mkdir()
    git(repo, 'init', '-q', '-b', 'docs')
    git(repo, 'config', 'user.name', 'Test')
    git(repo, 'config', 'user.email', 'someone@example.org')
    git(repo, 'commit', '-q', '--allow-empty', '-m', 'init')
    return repo


@pytest.fixture
def clean_modules(monkeypatch):
    """Lets a test load a fake bootstrap/juneau_scripts without leaking it into later tests."""
    for name in [n for n in sys.modules if n == 'bootstrap' or n.split('.')[0] == 'juneau_scripts']:
        monkeypatch.delitem(sys.modules, name)
    monkeypatch.setattr(sys, 'path', list(sys.path))
    monkeypatch.setattr(sys, 'dont_write_bytecode', sys.dont_write_bytecode)
    yield
    for name in [n for n in sys.modules if n == 'bootstrap' or n.split('.')[0] == 'juneau_scripts']:
        sys.modules.pop(name, None)


def fake_toolkit(root, run_source):
    """A Juneau checkout whose juneau-run bootstrap puts a juneau_scripts package with run_source on the path."""
    src = run_view.toolkit_dir(root)
    (src / 'juneau_scripts').mkdir(parents=True)
    (src / 'juneau_scripts' / '__init__.py').write_text('')
    (src / 'juneau_scripts' / 'run.py').write_text(run_source)
    (src / 'bootstrap.py').write_text(
        'import sys\n'
        f'def load(pom=None, run=False):\n    sys.path.insert(0, {str(src)!r})\n')
    (root / 'pom.xml').write_text('<project/>')


# ----------------------------------------------------------------------------------------------------------------------
# Loading the toolkit

def test_no_root_is_plain_and_silent(capsys):
    assert run_view.load_toolkit(None) is None
    assert run_view.load_toolkit('/nonexistent/juneau') is None
    assert capsys.readouterr().err == ''


def test_checkout_without_juneau_run_warns_and_is_plain(tmp_path, capsys):
    (tmp_path / 'pom.xml').write_text('<project/>')
    assert run_view.load_toolkit(tmp_path) is None
    assert 'juneau-run unavailable' in capsys.readouterr().err


def test_bootstrap_that_raises_is_plain(tmp_path, capsys, clean_modules):
    src = run_view.toolkit_dir(tmp_path)
    src.mkdir(parents=True)
    (src / 'bootstrap.py').write_text('def load(pom=None, run=False):\n    raise RuntimeError("needs Python 3.99")\n')
    assert run_view.load_toolkit(tmp_path) is None
    assert 'needs Python 3.99' in capsys.readouterr().err


def test_toolkit_missing_a_function_is_plain(tmp_path, capsys, clean_modules):
    fake_toolkit(tmp_path, 'def session(*a, **k): pass\n')
    assert run_view.load_toolkit(tmp_path) is None
    assert 'has no step' in capsys.readouterr().err


def test_complete_toolkit_loads(tmp_path, capsys, clean_modules):
    fake_toolkit(tmp_path, ''.join(f'def {name}(*a, **k): pass\n' for name in run_view._REQUIRED))
    run = run_view.load_toolkit(tmp_path)
    assert run is not None and run.__name__ == 'juneau_scripts.run'
    assert capsys.readouterr().err == ''


def test_plain_never_loads(tmp_path, clean_modules):
    fake_toolkit(tmp_path, 'raise AssertionError("loaded")\n')
    view = run_view.open_view(tmp_path, 'board', plain=True)
    assert not view.live


# ----------------------------------------------------------------------------------------------------------------------
# The plain View

def test_plain_cmd_runs_and_raises_on_failure(capfd):
    view = run_view.View()
    view.cmd([sys.executable, '-c', 'print("hi")'], id='x', title='X')
    assert 'hi' in capfd.readouterr().out
    with pytest.raises(subprocess.CalledProcessError) as e:
        view.cmd([sys.executable, '-c', 'raise SystemExit(3)'], id='x', title='X')
    assert e.value.returncode == 3


def test_plain_stage_and_skip_print_the_reason(capsys):
    view = run_view.View()
    with view.stage('copy', 'Copy', 'Copying') as step:
        step.skip('--skip-maven')
    view.skip('npm', 'npm', 'npm install', '--skip-npm')
    out = capsys.readouterr().out
    assert '-> Copying' in out and 'skipped (--skip-maven)' in out
    assert '-> Skipping npm install (--skip-npm)' in out


def test_plain_prechecks_fail_only_on_fail(capsys):
    view = run_view.View()
    assert view.prechecks([('a', lambda: None), ('b', lambda: ('warn', 'behind', ()))])
    assert not view.prechecks([('c', lambda: ('fail', 'wrong email', ('fix it',))), ('d', lambda: None)])
    out = capsys.readouterr().out
    assert '✅ a' in out and 'b: behind' in out and '❌ c: wrong email' in out and 'fix it' in out and '✅ d' in out


def test_plain_handoff_adds_nothing():
    with run_view.View().handoff() as extra:
        assert extra == {}


@pytest.mark.parametrize('code', ['import sys; sys.exit(2)'])
def test_plain_remote_cmd_failure_says_why_and_raises(code, capsys):
    with pytest.raises(subprocess.CalledProcessError):
        run_view.View().remote_cmd([sys.executable, '-c', code], id='fetch', title='Fetch')
    assert 'credentials' in capsys.readouterr().out


def test_plain_remote_cmd_success_is_silent(capsys):
    run_view.View().remote_cmd([sys.executable, '-c', 'pass'], id='fetch', title='Fetch')
    assert 'credentials' not in capsys.readouterr().out


class FakeView:
    """Records the remote_cmd rows a release script asks for, failing the first one."""

    def __init__(self):
        self.rows = []

    def say(self, *args, **kwargs):
        pass

    def remote_cmd(self, cmd, **kwargs):
        self.rows.append(cmd[:2])
        raise subprocess.CalledProcessError(128, cmd)


def test_stage_clone_and_fetch_go_through_remote_cmd(tmp_path):
    view = FakeView()
    with pytest.raises(subprocess.CalledProcessError):
        release_stage.fetch_staging(view, tmp_path / 'absent', 'https://example.org/x.git')
    assert view.rows == [['git', 'clone']]
    (tmp_path / 'there' / '.git').mkdir(parents=True)
    view = FakeView()
    with pytest.raises(subprocess.CalledProcessError):
        release_stage.fetch_staging(view, tmp_path / 'there', 'https://example.org/x.git')
    assert view.rows == [['git', 'fetch']]


def test_plain_paused_is_a_noop(capsys):
    with run_view.View().paused() as entered:
        assert entered is None
    assert capsys.readouterr().out == ''


def test_live_paused_goes_through_the_toolkit():
    events = []

    class Run:
        @contextlib.contextmanager
        def paused(self):
            events.append('pause')
            yield
            events.append('resume')

    with run_view.View(Run()).paused():
        events.append('body')
    assert events == ['pause', 'body', 'resume']


class PushView:
    """A view that records each cmd and whether it ran inside paused()."""

    def __init__(self):
        self.calls = []
        self.depth = 0

    @contextlib.contextmanager
    def paused(self):
        self.depth += 1
        try:
            yield
        finally:
            self.depth -= 1

    @contextlib.contextmanager
    def stage(self, id, label, title, **kwargs):
        yield types.SimpleNamespace(summary=None)

    @contextlib.contextmanager
    def handoff(self):
        yield {}

    def banner(self, title):
        pass

    def say(self, *args, **kwargs):
        pass

    def skip(self, *args, **kwargs):
        pass

    def prechecks(self, checks):
        return True

    def cmd(self, cmd, **kwargs):
        self.calls.append((cmd[:2], self.depth))

    remote_cmd = cmd


def test_promote_push_runs_paused(tmp_path, monkeypatch):
    site = tmp_path / 'site'
    (site / '.git').mkdir(parents=True)
    monkeypatch.setattr(release, 'site_dir_for', lambda docs_dir: site)
    monkeypatch.setattr(release, 'get_git_remote_url', lambda: 'https://example.org/x.git')
    monkeypatch.setattr(release.subprocess, 'run', lambda *a, **k: None)
    view = PushView()
    assert release.promote(argparse.Namespace(no_push=False), view, tmp_path) == 0
    assert [c for c in view.calls if c[0][1] == 'push'] == [(['git', 'push'], 1)]
    assert all(depth == 0 for cmd, depth in view.calls if cmd[1] != 'push')


def test_stage_push_runs_paused(tmp_path, monkeypatch):
    (tmp_path / 'build').mkdir()
    staging = tmp_path / 'staging'
    staging.mkdir()
    monkeypatch.setattr(release_stage, 'staging_dir_for', lambda docs_dir: staging)
    monkeypatch.setattr(release_stage, 'get_git_remote_url', lambda: 'https://example.org/x.git')
    monkeypatch.setattr(release_stage, 'BUILD_SCRIPT', Path(__file__))
    monkeypatch.setattr(release_stage.subprocess, 'run', lambda *a, **k: types.SimpleNamespace(returncode=0))
    for name in ('fetch_staging', 'copy_build', 'commit_staging'):
        monkeypatch.setattr(release_stage, name, lambda *a, **k: None)
    monkeypatch.setattr(release_stage.webbrowser, 'open', lambda url: True)
    view = PushView()
    args = argparse.Namespace(no_push=False, juneau_root=None, console=None, commit_message='m')
    assert release_stage.release_stage(args, view, tmp_path, None) == 0
    assert view.calls == [(['git', 'push'], 1)]


def test_session_passes_header_lines_through():
    seen = []

    class Run:
        @contextlib.contextmanager
        def session(self, title, **kw):
            seen.append((title, kw))
            yield types.SimpleNamespace(code=0)

    view = run_view.View(Run())
    assert view.session('Build', lambda: 0, header=['Juneau Docs'], lines=['Docs: /d'], repo='/r') == 0
    assert seen[0][0] == 'Build'
    assert (seen[0][1]['header'], seen[0][1]['lines']) == (('Juneau Docs',), ('Docs: /d',))


def test_session_lines_default_to_none():
    seen = []

    class Run:
        @contextlib.contextmanager
        def session(self, title, **kw):
            seen.append(kw)
            yield types.SimpleNamespace(code=0)

    run_view.View(Run()).session('Build', lambda: 0, header=['Juneau Docs'], repo='/r')
    assert seen[0]['lines'] == ()


# ----------------------------------------------------------------------------------------------------------------------
# Row plans

def test_build_rows_default_runs_everything():
    plan = build_docs.row_plan(build_args())
    assert [(id, label) for id, label, _ in plan] == list(run_view.BUILD_ROWS)
    assert all(reason is None for _, _, reason in plan)


@pytest.mark.parametrize('flags, skipped', [
    ({'skip_maven': True}, {'install': '--skip-maven', 'site': '--skip-maven', 'copy': '--skip-maven',
                            'links': '--skip-maven'}),
    ({'skip_npm': True}, {'npm': '--skip-npm', 'docs': '--skip-npm'}),
    ({'skip_verify': True}, {'verify': '--skip-verify'}),
    ({'skip_copy': True}, {}),   # the javadocs copy still runs; only the site copy is skipped, inside the row
])
def test_build_rows_skip_flags(flags, skipped):
    plan = {id: reason for id, _, reason in build_docs.row_plan(build_args(**flags))}
    assert {id: reason for id, reason in plan.items() if reason} == skipped


def test_dev_server_skips_npm_and_docs_but_flag_wins():
    plan = {id: reason for id, _, reason in build_docs.row_plan(build_args(), 'dev server running')}
    assert plan['npm'] == plan['docs'] == 'dev server running'
    plan = {id: reason for id, _, reason in build_docs.row_plan(build_args(skip_npm=True), 'dev server running')}
    assert plan['npm'] == '--skip-npm'


def test_stage_plan_nests_the_build_rows():
    plan = release_stage.stage_plan()
    assert plan[0] == ('prechecks', 'Prechecks')
    assert plan[1:-2] == list(run_view.BUILD_ROWS)
    assert plan[-2:] == [('stage', 'Stage push'), ('url', 'URL')]
    assert len({id for id, _ in plan}) == len(plan)


def test_promote_plan():
    assert release.PLAN == [('prechecks', 'Prechecks'), ('promote', 'Promote')]


# ----------------------------------------------------------------------------------------------------------------------
# Prechecks

@pytest.mark.parametrize('module', [release_stage, release])
def test_identity_check(module, repo):
    status, reason, hint = module.identity_problem(repo)
    assert status == 'fail' and 'someone@example.org' in reason and any('git config user.email' in h for h in hint)
    git(repo, 'config', 'user.email', module.REQUIRED_GIT_EMAIL)
    assert module.identity_problem(repo) is None


def test_branch_warning(repo):
    assert run_view.branch_warning(repo) is None   # on a branch with no upstream
    git(repo, 'checkout', '-q', '--detach')
    assert run_view.branch_warning(repo) == 'detached HEAD, not on a branch'
    assert release_stage.branch_problem(repo) == ('warn', 'detached HEAD, not on a branch', ())


def test_branch_behind_upstream(repo, tmp_path):
    git(tmp_path, 'clone', '-q', str(repo), 'clone')
    git(repo, 'commit', '-q', '--allow-empty', '-m', 'two')
    assert run_view.branch_warning(tmp_path / 'clone') == 'docs is 1 commit behind its upstream'



def test_branch_warning_fetch_never_prompts(repo, tmp_path, monkeypatch):
    git(tmp_path, 'clone', '-q', str(repo), 'clone')
    git(tmp_path / 'clone', 'remote', 'set-url', 'origin', str(tmp_path / 'missing'))
    seen = []
    real = subprocess.run
    monkeypatch.setattr(run_view.subprocess, 'run', lambda *a, **k: seen.append(k['env']) or real(*a, **k))
    assert run_view.branch_warning(tmp_path / 'clone') == 'docs: could not fetch its upstream'
    assert all(e['GIT_TERMINAL_PROMPT'] == '0' and 'BatchMode=yes' in e['GIT_SSH_COMMAND'] for e in seen)


def test_branch_warning_reports_instead_of_raising(repo, monkeypatch):
    def hang(*a, **k):
        raise subprocess.TimeoutExpired(a[0], 60)
    monkeypatch.setattr(run_view.subprocess, 'run', hang)
    assert run_view.branch_warning(repo).startswith('could not check the branch (')


def test_open_view_falls_back_to_plain_on_bad_console(monkeypatch, capsys):
    class Run:
        def export_console(self, console):
            raise ValueError(f"bad console {console!r}")
    monkeypatch.setattr(run_view, 'load_toolkit', lambda root: Run())
    view = run_view.open_view('/nowhere', 'bogus')
    assert view.run is None and 'console setting rejected' in capsys.readouterr().err

# ----------------------------------------------------------------------------------------------------------------------
# build-docs.py end to end, plain

def run_build_docs(*args, juneau_root):
    env = {k: v for k, v in os.environ.items() if not k.startswith('JUNEAU_')}
    return subprocess.run([sys.executable, '-B', str(SCRIPTS_DIR / 'build-docs.py'), *args,
                           '--juneau-root', str(juneau_root)], capture_output=True, text=True, env=env)


def test_dry_run_is_plain_with_skipped_rows(tmp_path):
    result = run_build_docs('--dry-run', '--skip-maven', '--skip-npm', juneau_root=tmp_path / 'missing')
    assert result.returncode == 0, result.stdout + result.stderr
    assert '-> Skipping Maven install (--skip-maven)' in result.stdout
    assert '-> Skipping Docusaurus build (--skip-npm)' in result.stdout
    assert 'juneau-run' not in result.stderr


def test_bogus_juneau_root_fails_as_before(tmp_path):
    result = run_build_docs('--skip-maven', juneau_root=tmp_path / 'missing')
    assert result.returncode == 1
    assert 'is not a Juneau checkout' in result.stdout


@pytest.mark.parametrize('script', ['build-docs.py', 'release-docs-stage.py', 'release-docs.py'])
def test_console_flag_is_validated(script):
    result = subprocess.run([sys.executable, '-B', str(SCRIPTS_DIR / script), '--console', 'loud'],
                            capture_output=True, text=True)
    assert result.returncode == 2 and "invalid choice: 'loud'" in result.stderr
