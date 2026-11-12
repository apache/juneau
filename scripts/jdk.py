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
The --jdk option shared by push.py and test.py: resolve a JDK number or path to a JDK home, and put it first on the
environment so every Maven and javadoc subprocess uses it.

Documented in the Options and Gotchas of push.md.
"""

import os
import subprocess
from pathlib import Path


class JdkError(Exception):
    """The --jdk value does not name exactly one usable JDK."""


def _home(path):
    """The JDK home at path: path itself, or its Contents/Home when that is a macOS bundle; None if neither has bin/java."""
    for candidate in (path, path / "Contents" / "Home"):
        if (candidate / "bin" / "java").exists():
            return candidate
    return None


def resolve(value, jdk_dir=None):
    """
    A JDK home (Path) for value: an all-digit number is ~/jdk/jdk-<N> if that exists, else the single ~/jdk/*<N>* entry;
    anything else is a path used as given.  A macOS bundle resolves to its Contents/Home.  Raises JdkError when the
    number is missing or ambiguous or the result has no bin/java.
    """
    if value.isdigit():
        base = Path(jdk_dir) if jdk_dir else Path.home() / "jdk"
        exact = base / f"jdk-{value}"
        if exact.exists():
            candidates = [exact]
        else:
            candidates = sorted(base.glob(f"*{value}*")) if base.is_dir() else []
        if not candidates:
            raise JdkError(f"--jdk {value}: no JDK named jdk-{value} or matching *{value}* under {base}")
        if len(candidates) > 1:
            names = ", ".join(p.name for p in candidates)
            raise JdkError(f"--jdk {value}: ambiguous, matches {names}; pass a path or rename one to jdk-{value}")
        path = candidates[0]
    else:
        path = Path(value).expanduser()
    home = _home(path)
    if home is None:
        raise JdkError(f"--jdk {value}: no bin/java under {path} (or {path}/Contents/Home)")
    return home


def version_line(home):
    """The first line of `java -version` for home, or "unknown" if it cannot be run."""
    try:
        result = subprocess.run([str(home / "bin" / "java"), "-version"], capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.SubprocessError):
        return "unknown"
    lines = (result.stderr or result.stdout).strip().splitlines()
    return lines[0] if lines else "unknown"


def apply(home, environ=None):
    """Set JAVA_HOME to home and put home/bin first on PATH in environ (default os.environ); returns environ."""
    env = os.environ if environ is None else environ
    env["JAVA_HOME"] = str(home)
    env["PATH"] = str(home / "bin") + os.pathsep + env.get("PATH", "")
    return env
