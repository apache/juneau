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
"""End-of-run notifications: the terminal bell and a platform sound.  Both are best-effort and never raise.  Both are
silent when SANDBOX_RUNTIME, JUNEAU_TERMINAL_SCRIPTS_QUIET or any variable named in silence_env is exactly "1"."""

import os
import platform
import subprocess
import sys

QUIET_ENV = "JUNEAU_TERMINAL_SCRIPTS_QUIET"
MACOS_SUCCESS_SOUND = "/System/Library/Sounds/Glass.aiff"
MACOS_FAILURE_SOUND = "/System/Library/Sounds/Basso.aiff"
LINUX_SUCCESS_SOUND = "/usr/share/sounds/freedesktop/stereo/complete.oga"
LINUX_FAILURE_SOUND = "/usr/share/sounds/freedesktop/stereo/dialog-error.oga"


def silenced(silence_env=()):
    """True when SANDBOX_RUNTIME, JUNEAU_TERMINAL_SCRIPTS_QUIET or a silence_env variable (a name or a tuple of names) is "1"."""
    names = (silence_env,) if isinstance(silence_env, str) else tuple(silence_env)
    return any(os.environ.get(name) == "1" for name in ("SANDBOX_RUNTIME", QUIET_ENV, *names))


def bell(*, silence_env=()):
    """Write \\a to sys.stdout (looked up now) when it is a TTY."""
    try:
        if silenced(silence_env):
            return
        out = sys.stdout
        if out is not None and out.isatty():
            out.write("\a")
            out.flush()
    except Exception:  # NOSONAR python:S1166 - a bell must never fail a run
        pass


def sound(ok, *, silence_env=()):
    """Play the platform's success or failure sound: afplay (macOS), paplay falling back to speaker-test (Linux),
    winsound (Windows)."""
    try:
        if not silenced(silence_env):
            _play(ok)
    except Exception:  # NOSONAR python:S1166 - a sound must never fail a run
        pass


def _play(ok):
    quiet = {"stdout": subprocess.DEVNULL, "stderr": subprocess.DEVNULL, "check": False}
    system = platform.system()
    if system == "Darwin":
        path = MACOS_SUCCESS_SOUND if ok else MACOS_FAILURE_SOUND
        if os.path.exists(path):
            subprocess.run(["afplay", path], timeout=5, **quiet)
    elif system == "Linux":
        try:
            subprocess.run(["paplay", LINUX_SUCCESS_SOUND if ok else LINUX_FAILURE_SOUND], timeout=5, **quiet)
        except OSError:
            subprocess.run(["speaker-test", "-t", "sine", "-f", "1000" if ok else "400", "-l", "1"], timeout=2, **quiet)
    elif system == "Windows":
        import winsound
        winsound.MessageBeep(winsound.MB_OK if ok else winsound.MB_ICONHAND)
