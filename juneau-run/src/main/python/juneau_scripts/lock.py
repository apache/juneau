#!/usr/bin/env python3
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
"""An advisory fcntl.flock, with an optional timeout.  POSIX-only."""

import contextlib
import os
import time
from pathlib import Path

from ._base import posix_only


class LockBusy(Exception):
    """The lock is held elsewhere (non-blocking), or was still held when the timeout ran out."""

    def __init__(self, path):
        super().__init__(f"lock is busy: {path}")
        self.path = path


@contextlib.contextmanager
def flock(path, *, blocking=True, timeout=None, poll=1.0):
    """Hold an exclusive flock on path for the block, creating the parent directory.  Non-blocking raises LockBusy
    at once; blocking with a timeout polls with LOCK_NB every poll seconds (flock has no native timeout)."""
    posix_only("lock.flock")
    import fcntl
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    fd = os.open(path, os.O_RDWR | os.O_CREAT, 0o644)
    try:
        if blocking and timeout is None:
            fcntl.flock(fd, fcntl.LOCK_EX)
        else:
            deadline = time.monotonic() + (timeout or 0)
            while True:
                try:
                    fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    break
                except BlockingIOError:
                    left = deadline - time.monotonic()
                    if not blocking or left <= 0:
                        raise LockBusy(path) from None
                    time.sleep(min(poll, left))
        try:
            yield path
        finally:
            fcntl.flock(fd, fcntl.LOCK_UN)
    finally:
        os.close(fd)
