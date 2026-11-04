# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
"""Tests for mvn_lock.py: the shared Maven lock (same mkdir protocol as juneau-mvn-lock.sh)."""

import os
import subprocess
import sys
import threading
import time
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import mvn_lock  # noqa: E402


@pytest.fixture(autouse=True)
def lockenv(tmp_path, monkeypatch):
    monkeypatch.setenv('JUNEAU_MVN_LOCKDIR', str(tmp_path / 'lockdir'))
    monkeypatch.setenv('JUNEAU_MVN_PRIORITY_DIR', str(tmp_path / 'prio'))
    monkeypatch.setenv('JUNEAU_MVN_POLL', '0.05')
    monkeypatch.delenv('JUNEAU_MVN_LOCK_HELD', raising=False)
    monkeypatch.delenv('JUNEAU_MVN_WRAPPER', raising=False)
    return tmp_path


def test_acquire_writes_pid_and_release_removes_dir(lockenv):
    lock = mvn_lock.MvnLock()
    with lock:
        assert (lockenv / 'lockdir' / 'pid').read_text().strip() == str(os.getpid())
        assert os.environ['JUNEAU_MVN_LOCK_HELD'] == str(os.getpid())
    assert not (lockenv / 'lockdir').exists()
    assert 'JUNEAU_MVN_LOCK_HELD' not in os.environ


def test_release_is_idempotent(lockenv):
    lock = mvn_lock.MvnLock().acquire()
    lock.release()
    lock.release()


def test_waits_and_reports_holder_then_proceeds(lockenv):
    sleeper = subprocess.Popen(['sleep', '30'])
    try:
        (lockenv / 'lockdir').mkdir()
        (lockenv / 'lockdir' / 'pid').write_text(f"{sleeper.pid}\n")
        waits = []
        threading.Timer(0.3, lambda: __import__('shutil').rmtree(lockenv / 'lockdir')).start()
        with mvn_lock.MvnLock(on_wait=lambda pid, secs: waits.append(pid)):
            pass
        assert waits and waits[0] == sleeper.pid
    finally:
        sleeper.kill()
        sleeper.wait()


def test_breaks_lock_of_dead_pid(lockenv):
    dead = subprocess.Popen(['true'])
    dead.wait()
    (lockenv / 'lockdir').mkdir()
    (lockenv / 'lockdir' / 'pid').write_text(f"{dead.pid}\n")
    with mvn_lock.MvnLock():
        assert (lockenv / 'lockdir' / 'pid').read_text().strip() == str(os.getpid())


def test_never_breaks_lock_of_live_pid(lockenv):
    sleeper = subprocess.Popen(['sleep', '30'])
    try:
        (lockenv / 'lockdir').mkdir()
        (lockenv / 'lockdir' / 'pid').write_text(f"{sleeper.pid}\n")
        calls = []

        def stop(pid, secs):
            calls.append(pid)
            if len(calls) >= 3:
                raise KeyboardInterrupt

        with pytest.raises(KeyboardInterrupt):
            mvn_lock.MvnLock(on_wait=stop).acquire()
        assert (lockenv / 'lockdir').exists()
    finally:
        sleeper.kill()
        sleeper.wait()


def test_reentrant_for_child_of_holder(lockenv, monkeypatch):
    with mvn_lock.MvnLock():
        inner = mvn_lock.MvnLock().acquire()
        assert not inner.held
        inner.release()
        assert (lockenv / 'lockdir').exists()


def test_live_priority_marker_blocks_ordinary_waiter(lockenv):
    prio = lockenv / 'prio'
    prio.mkdir()
    sleeper = subprocess.Popen(['sleep', '30'])
    try:
        start = mvn_lock._lstart(sleeper.pid)
        (prio / str(sleeper.pid)).write_text(f"{sleeper.pid} {start}\n")
        lock = mvn_lock.MvnLock()
        assert lock._try_take() is False
        sleeper.kill()
        sleeper.wait()
        assert lock._try_take() is True
        assert not (prio / str(sleeper.pid)).exists()
        lock.held = True
        lock.release()
    finally:
        if sleeper.poll() is None:
            sleeper.kill()


def test_drops_shell_lock_wrapper_while_held(lockenv, monkeypatch):
    monkeypatch.setenv('JUNEAU_MVN_WRAPPER', '/x/juneau-mvn-lock.sh')
    with mvn_lock.MvnLock():
        assert 'JUNEAU_MVN_WRAPPER' not in os.environ
    assert os.environ['JUNEAU_MVN_WRAPPER'] == '/x/juneau-mvn-lock.sh'


def test_wait_notifier_throttles():
    said = []
    notify = mvn_lock.wait_notifier(said.append, every=60)
    notify(None, 0)
    notify(None, 5)
    notify(None, 61)
    assert len(said) == 2 and 'another build' in said[0]


@pytest.mark.skipif(not Path.home().joinpath('agents/skills/juneau-worktrees/juneau-mvn-lock.sh').exists(),
                    reason='shell lock script not installed')
def test_shell_wrapper_waits_for_python_holder(lockenv):
    script = Path.home() / 'agents/skills/juneau-worktrees/juneau-mvn-lock.sh'
    env = dict(os.environ)
    with mvn_lock.MvnLock():
        p = subprocess.Popen([str(script), 'true'], env=env)
        time.sleep(0.5)
        assert p.poll() is None
    assert p.wait(timeout=15) == 0


def test_release_removes_the_lock_even_when_a_test_has_replaced_subprocess_run(lockenv, monkeypatch):
    """test_run_adoption's gates fixture replaces subprocess.run process-wide; a release that shelled out to `rm -rf`
    then left the lock dir behind, and the next acquire in that process waited on its own pid forever."""
    monkeypatch.setattr(subprocess, 'run', lambda cmd, *a, **k: subprocess.CompletedProcess(cmd, 0, '', ''))
    with mvn_lock.MvnLock():
        pass
    assert not (lockenv / 'lockdir').exists()


def test_a_stale_lock_is_broken_even_when_subprocess_run_is_replaced(lockenv, monkeypatch):
    (lockenv / 'lockdir').mkdir()
    (lockenv / 'lockdir' / 'pid').write_text('999999\n')
    monkeypatch.setattr(subprocess, 'run', lambda cmd, *a, **k: subprocess.CompletedProcess(cmd, 0, '', ''))
    with mvn_lock.MvnLock():
        assert (lockenv / 'lockdir' / 'pid').read_text().strip() == str(os.getpid())
