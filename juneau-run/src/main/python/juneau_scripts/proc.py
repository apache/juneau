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
"""Run a child in its own process group, and stop the whole group on Ctrl-C.  POSIX-only."""

import os
import signal
import subprocess
import time

from ._base import Result, posix_only


def run_group(cmd, *, cwd=None, env=None, grace=30):
    """Run cmd in a new session.  On KeyboardInterrupt: SIGTERM to the child, wait up to grace seconds, then SIGKILL
    to its whole group, and re-raise.  A child that can't start gives Result(False, 127, err)."""
    posix_only("proc.run_group")
    t0 = time.monotonic()
    try:
        child = subprocess.Popen(cmd, cwd=cwd, env=env, start_new_session=True)
    except OSError as e:
        return Result(False, 127, err=str(e))
    try:
        code = child.wait()
    except KeyboardInterrupt:
        try:
            child.send_signal(signal.SIGTERM)
            child.wait(timeout=grace)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(child.pid, signal.SIGKILL)
            except (ProcessLookupError, PermissionError):   # the group is already gone, or (macOS) exiting
                pass
            child.wait()
        raise
    return Result(code == 0, code, ms=int((time.monotonic() - t0) * 1000))
