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
"""
Optional juneau-run live view for build-docs.py, release-docs-stage.py and release-docs.py.

The scripts load juneau-run's toolkit from the Juneau checkout they resolve
(<juneau-root>/juneau-run/src/main/python).  With it, each stage is a row of the console view
(--console full|condensed|none, default condensed, or JUNEAU_RUN_CONSOLE), with the full and
condensed logs under ~/.cache/juneau-run/.  When it can't be found or imported, View is plain:
banners and the tools' own output on the terminal, as the scripts printed before.  juneau-docs
never depends on juneau-run.
"""

import contextlib
import importlib
import importlib.util
import os
import subprocess
import sys
from pathlib import Path

CONSOLE_MODES = ('full', 'condensed', 'none')

#: Every row build-docs.py draws, in order, as (id, label).  Skipped stages still draw, as skipped rows.
BUILD_ROWS = (
    ('clean', 'Clean'),
    ('npm', 'npm'),
    ('install', 'Install'),
    ('site', 'Site'),
    ('copy', 'Copy'),
    ('docs', 'Docs build'),
    ('verify', 'Verify'),
    ('links', 'Links'),
)

# The toolkit functions the scripts call; an older juneau-run without one of them counts as unavailable.
_REQUIRED = ('session', 'step', 'cmd', 'say', 'handoff', 'checks', 'export_console', 'paused')


def toolkit_dir(juneau_root):
    """Where juneau-run's Python sources sit in a Juneau checkout."""
    return Path(juneau_root) / 'juneau-run' / 'src' / 'main' / 'python'


def load_toolkit(juneau_root):
    """
    juneau_scripts.run from the Juneau checkout, or None.

    Returns None silently when juneau_root is None or doesn't exist (e.g. a CI runner with only
    the docs checkout), and with one warning on stderr when the checkout has no usable juneau-run.
    """
    if juneau_root is None or not Path(juneau_root).is_dir():
        return None
    src = toolkit_dir(juneau_root)
    try:
        if not (src / 'bootstrap.py').is_file():
            raise ImportError(f"{src / 'bootstrap.py'} not found")
        sys.dont_write_bytecode = True   # nothing may write __pycache__ into the Juneau source tree
        bootstrap = sys.modules.get('bootstrap')
        if bootstrap is None or Path(getattr(bootstrap, '__file__', '')).resolve() != (src / 'bootstrap.py').resolve():
            spec = importlib.util.spec_from_file_location('bootstrap', src / 'bootstrap.py')
            bootstrap = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(bootstrap)
            sys.modules['bootstrap'] = bootstrap
        bootstrap.load(pom=str(Path(juneau_root) / 'pom.xml'), run=True)
        run = importlib.import_module('juneau_scripts.run')
        missing = [name for name in _REQUIRED if not hasattr(run, name)]
        if missing:
            raise ImportError(f"juneau_scripts.run has no {', '.join(missing)}")
        return run
    except Exception as e:  # any failure means plain output, never a failed build
        print(f"warning: juneau-run unavailable ({e}); running with plain output", file=sys.stderr)
        return None


def open_view(juneau_root, console=None, *, verbose=False, plain=False):
    """
    The View for a script run.  console is the --console value (None: JUNEAU_RUN_CONSOLE, else
    condensed).  plain=True (e.g. --dry-run) skips juneau-run altogether.
    """
    run = None if plain else load_toolkit(juneau_root)
    if run is not None:
        try:
            run.export_console(console)
        except Exception as e:  # e.g. a bad JUNEAU_RUN_CONSOLE value: plain output, never a failed build
            print(f"warning: juneau-run console setting rejected ({e}); running with plain output", file=sys.stderr)
            run = None
    return View(run, verbose=verbose)


def add_console_argument(parser):
    """--console, for the docs scripts.  Validated here, since the toolkit may not be loaded."""
    parser.add_argument('--console', choices=CONSOLE_MODES, default=None,
                        help='Console output when juneau-run is available: full, condensed (default) or none '
                             '(also JUNEAU_RUN_CONSOLE; the flag wins)')


def branch_warning(repo):
    """
    None when repo is on a branch that isn't behind its upstream; otherwise one line saying what
    is off (detached HEAD, behind upstream, or the check couldn't run).  Fetches the upstream first,
    without prompting for credentials, so it never hangs or stops a release.
    """
    env = {**os.environ, 'GIT_TERMINAL_PROMPT': '0', 'GIT_SSH_COMMAND': 'ssh -oBatchMode=yes'}

    def git(*args):
        return subprocess.run(['git', *args], cwd=repo, capture_output=True, text=True, check=False,
                              env=env, timeout=60)

    try:
        branch = git('symbolic-ref', '--short', '-q', 'HEAD')
        if branch.returncode != 0:
            return 'detached HEAD, not on a branch'
        if git('rev-parse', '--abbrev-ref', '--symbolic-full-name', '@{upstream}').returncode != 0:
            return None
        if git('fetch', '-q').returncode != 0:
            return f"{branch.stdout.strip()}: could not fetch its upstream"
        counts = git('rev-list', '--left-right', '--count', 'HEAD...@{upstream}')
        if counts.returncode != 0:
            return f"{branch.stdout.strip()}: could not compare with its upstream"
        behind = int(counts.stdout.split()[1])
    except (OSError, subprocess.SubprocessError, ValueError, IndexError) as e:
        return f"could not check the branch ({e})"
    if behind:
        return f"{branch.stdout.strip()} is {behind} commit{'' if behind == 1 else 's'} behind its upstream"
    return None


class _PlainStep:
    """What View.stage yields in a plain run: the row's outcome, printed when it matters."""

    def __init__(self):
        self.status = 'ok'
        self.summary = None

    def fail(self, exit=None):  # NOSONAR python:S5806 - mirrors juneau_run.Step.fail
        self.status = 'fail'

    def skip(self, reason=None):
        self.status = 'skip'
        self.summary = reason

    def warn(self, text):
        print(f"⚠ {text}", flush=True)


class View:
    """The live view through juneau-run (run set), or plain output (run None)."""

    def __init__(self, run=None, *, verbose=False):
        self.run = run
        self.verbose = verbose

    @property
    def live(self):
        """True when juneau-run is loaded (the rows are drawn unless --console full)."""
        return self.run is not None

    def say(self, text='', level='info'):
        """Status text: printed in a plain run; under the view a note filtered by its detail level."""
        if self.run is None:
            print(text, flush=True)
        else:
            self.run.say(text, level)

    def banner(self, title):
        """A stage header in a plain run (the row says it under the view)."""
        if self.run is None:
            print(f"\n=== {title} ===" if self.verbose else f"-> {title}", flush=True)

    @contextlib.contextmanager
    def stage(self, id, label, title, *, parent=None):  # NOSONAR python:S5806 - mirrors juneau_run.step
        """A row around the block.  Set .summary on what it yields, or call .skip(reason) or .fail()."""
        if self.run is None:
            self.banner(title)
            step = _PlainStep()
            yield step
            if step.status == 'skip':
                print(f"   skipped{f' ({step.summary})' if step.summary else ''}", flush=True)
            return
        with self.run.step(id, title, label=label, parent=parent) as step:
            yield step

    def skip(self, id, label, title, reason, *, parent=None):  # NOSONAR python:S5806 - mirrors juneau_run.step
        """A row shown skipped, with reason as its text."""
        if self.run is None:
            print(f"\n=== Skipping {title} ({reason}) ===" if self.verbose else f"-> Skipping {title} ({reason})",
                  flush=True)
            return
        with self.run.step(id, title, label=label, parent=parent) as step:
            step.skip(reason)

    def paused(self):
        """The console view cleared for the block, so a prompt or an interactive child isn't drawn over; a no-op when plain."""
        if self.run is None:
            return contextlib.nullcontext()
        return self.run.paused()

    def cmd(self, cmd, *, id, title, label=None, tool=None, cwd=None, env=None, parent=None, tty=False):  # NOSONAR python:S5806
        """
        Run cmd as its own row (a child row with parent).  tool is juneau-run's parser (None picks it
        from the command: mvn is maven, npm is npm).  Raises CalledProcessError when it fails.
        """
        if self.run is None:
            print(f"Running: {' '.join(map(str, cmd))}", flush=True)
            if cwd:
                print(f"  (in directory: {cwd})", flush=True)
            subprocess.run(cmd, cwd=cwd, env=env, check=True)
            return
        result = self.run.cmd(cmd, title=title, id=id, label=label, tool=tool, cwd=cwd, env=env, parent=parent,
                              tty=tty)
        if not result.ok:
            raise subprocess.CalledProcessError(result.code, cmd)

    def remote_cmd(self, cmd, **kwargs):
        """
        cmd (a git clone or fetch) as its own row, as cmd().  When it fails, adds a line naming the likely
        cause, since a remote that wants credentials fails without a prompt here, then re-raises.
        """
        try:
            self.cmd(cmd, **kwargs)
        except subprocess.CalledProcessError:
            self.say("   The remote may need credentials; load your ssh key into the agent "
                     "(ssh-add) or sign in to your git credential helper, then run again.", 'error')
            raise

    def prechecks(self, checks):
        """
        Run checks as the Prechecks row and return False when one failed.  checks is a list of
        (name, fn); fn() returns None for a pass, or (status, reason, hint lines) with status
        "warn" or "fail".  Every check runs, even after a failure.
        """
        if self.run is None:
            ok = True
            for name, fn in checks:
                out = fn()
                if out is None:
                    print(f"✅ {name}", flush=True)
                    continue
                status, reason, hint = out
                print(f"{'❌' if status == 'fail' else '⚠️ '} {name}: {reason}", flush=True)
                for line in hint:
                    print(f"   {line}", flush=True)
                ok = ok and status != 'fail'
            return ok
        jc = importlib.import_module('juneau_scripts.checks')

        def verdict(fn):
            def check():
                out = fn()
                return None if out is None else jc.Verdict(out[0], out[1], tuple(out[2]))
            return check

        items = [jc.Check(name, verdict(fn)) for name, fn in checks]
        return self.run.checks('prechecks', 'Prechecks', items, label='Prechecks').ok

    @contextlib.contextmanager
    def handoff(self):
        """The terminal for a child script that joins this view; yields the env additions it needs."""
        if self.run is None:
            yield {}
            return
        with self.run.handoff() as extra:
            yield extra

    def session(self, title, body, *, header=(), lines=(), plan=None, repo='.', mode=None):
        """
        Run body() -- which returns an exit code -- inside a juneau-run session and return the
        session's code: 0, 1 on a failure (a failed command's row already says why), 130 on Ctrl-C.
        header is the items after the title; lines are header lines under it (one each, cut to one
        line on the live screen).  Only for a live View.
        """
        def guarded():
            try:
                return body()
            except subprocess.CalledProcessError:
                raise SystemExit(1)

        with self.run.session(title, header=tuple(header), lines=tuple(lines), repo=str(repo), mode=mode,
                              project=Path(repo).name, exit=False,
                              plan=list(plan) if plan is not None else None) as s:
            code = guarded()
            if code:
                raise SystemExit(code)
        return s.code
