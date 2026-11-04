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
"""
The shared Maven lock, in Python: the same `mkdir` lock as ~/agents/skills/juneau-worktrees/juneau-mvn-lock.sh.

Documented in the Gotchas of push.md and build-docs.md.
"""

import os
import shutil
import subprocess
import time
from pathlib import Path

LOCK_ENV = 'JUNEAU_MVN_LOCKDIR'
PRIO_ENV = 'JUNEAU_MVN_PRIORITY_DIR'
POLL_ENV = 'JUNEAU_MVN_POLL'
HELD_ENV = 'JUNEAU_MVN_LOCK_HELD'
WRAPPER_ENV = 'JUNEAU_MVN_WRAPPER'
DEFAULT_LOCK = '/tmp/claude-501/juneau-mvn.lockdir'
DEFAULT_PRIO = '/tmp/claude-501/juneau-mvn.priority'


def _alive(pid):
    if pid is None or pid <= 1:
        return False
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    return True


def _read_pid(lock):
    try:
        return int((lock / 'pid').read_text().split()[0])
    except (OSError, ValueError, IndexError):
        return None


def _lstart(pid):
    return subprocess.run(['ps', '-o', 'lstart=', '-p', str(pid)], capture_output=True, text=True,
                          check=False).stdout.strip()


def _live_priority_marker(prio):
    """True when a harvest-priority waiter is queued (stale markers are removed), as the shell wrapper does."""
    live = False
    try:
        entries = list(Path(prio).iterdir())
    except OSError:
        return False
    for f in entries:
        if not f.name.isdigit():
            continue
        pid = int(f.name)
        try:
            content = f.read_text().strip()
        except OSError:
            continue
        if _alive(pid) and content == f"{pid} {_lstart(pid)}".strip():
            live = True
        else:
            f.unlink(missing_ok=True)
    return live


class MvnLock:
    """
    Holds the Juneau Maven lock from acquire() to release().  Usable as a context manager.

    Reentrant for child processes: acquire() while JUNEAU_MVN_LOCK_HELD names the live pid that owns the lock
    directory returns at once without taking or releasing anything.
    """

    def __init__(self, on_wait=None):
        self.lock = Path(os.environ.get(LOCK_ENV) or DEFAULT_LOCK).absolute()
        self.prio = Path(os.environ.get(PRIO_ENV) or DEFAULT_PRIO).absolute()
        self.poll = float(os.environ.get(POLL_ENV) or 5)
        self.on_wait = on_wait
        self.held = False
        self._saved_env = None
        self._saved_wrapper = None

    def holder(self):
        """The pid that holds the lock now, or None."""
        return _read_pid(self.lock)

    def _try_take(self):
        if _live_priority_marker(self.prio):
            return False
        try:
            self.lock.parent.mkdir(parents=True, exist_ok=True)
            self.lock.mkdir()
        except OSError:
            return False
        (self.lock / 'pid').write_text(f"{os.getpid()}\n")
        return True

    def _break_if_stale(self):
        pid = _read_pid(self.lock)
        if pid is not None and not _alive(pid) and _read_pid(self.lock) == pid:
            shutil.rmtree(self.lock, ignore_errors=True)

    def acquire(self):
        """Wait for and take the lock, calling on_wait(holder_pid, seconds_waited) on each poll while it is busy."""
        if self.held:
            return self
        owner = os.environ.get(HELD_ENV)
        if owner and owner.isdigit() and _alive(int(owner)) and _read_pid(self.lock) == int(owner):
            return self
        start = time.time()
        while not self._try_take():
            self._break_if_stale()
            if self.on_wait:
                self.on_wait(self.holder(), time.time() - start)
            time.sleep(self.poll)
        self.held = True
        self._saved_env = os.environ.get(HELD_ENV)
        os.environ[HELD_ENV] = str(os.getpid())
        # A child that would wrap its own mvn in the shell lock script would wait on us forever.
        wrapper = os.environ.get(WRAPPER_ENV)
        if wrapper and 'juneau-mvn-lock' in wrapper:
            self._saved_wrapper = wrapper
            del os.environ[WRAPPER_ENV]
        return self

    def release(self):
        """Release the lock if this object holds it; idempotent."""
        if not self.held:
            return
        self.held = False
        if self._saved_env is None:
            os.environ.pop(HELD_ENV, None)
        else:
            os.environ[HELD_ENV] = self._saved_env
        if self._saved_wrapper is not None:
            os.environ[WRAPPER_ENV] = self._saved_wrapper
            self._saved_wrapper = None
        if _read_pid(self.lock) == os.getpid():
            shutil.rmtree(self.lock, ignore_errors=True)

    def __enter__(self):
        return self.acquire()

    def __exit__(self, *exc):
        self.release()
        return False


def describe_holder(pid):
    """A one-line description of the lock's holder for a wait message."""
    if pid is None:
        return "another build"
    cmd = subprocess.run(['ps', '-o', 'command=', '-p', str(pid)], capture_output=True, text=True,
                         check=False).stdout.strip()
    return f"pid {pid}" + (f" ({cmd[:80]})" if cmd else "")


def wait_notifier(say, every=60):
    """An on_wait callback that says who holds the lock: at once, then about every `every` seconds."""
    state = {'last': None}

    def notify(pid, waited):
        if state['last'] is None or waited - state['last'] >= every:
            state['last'] = waited
            say(f"Waiting for the Maven lock held by {describe_holder(pid)} ({int(waited)}s so far)")
    return notify
