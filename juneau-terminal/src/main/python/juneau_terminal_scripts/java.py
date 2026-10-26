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
"""Find a JDK home by major version."""

import os
import re
import subprocess
import sys
from pathlib import Path

_VERSION = re.compile(r'^JAVA_VERSION="?(\d+)(?:\.(\d+))?', re.MULTILINE)


def release_major(home):
    """The major version from <home>/release (1.8 reads as 8), or None."""
    try:
        text = (Path(home) / "release").read_text(encoding="utf-8", errors="replace")
    except OSError:
        return None
    m = _VERSION.search(text)
    if not m:
        return None
    major = int(m.group(1))
    return int(m.group(2)) if major == 1 and m.group(2) else major


def _roots():
    return [Path.home() / "jdk", Path("/usr/lib/jvm")]


def _candidates(major):
    if sys.platform == "darwin" and os.path.exists("/usr/libexec/java_home"):
        try:
            done = subprocess.run(["/usr/libexec/java_home", "-v", str(major)], capture_output=True, text=True,
                                  timeout=10, check=False)
        except (OSError, subprocess.TimeoutExpired):
            done = None
        if done is not None and done.returncode == 0 and done.stdout.strip():
            yield done.stdout.strip()
    for root in _roots():
        if root.is_dir():
            for child in sorted(root.iterdir()):
                yield str(child)
                yield str(child / "Contents" / "Home")     # a macOS .jdk bundle


def find_home(major, *, override_env=None):
    """os.environ[override_env] if set (used as given), then JAVA_HOME if it is that major, then
    /usr/libexec/java_home -v <major> on macOS, then ~/jdk/* and /usr/lib/jvm/*.  None if nothing matches."""
    if override_env and os.environ.get(override_env):
        return os.environ[override_env]
    java_home = os.environ.get("JAVA_HOME")
    if java_home and release_major(java_home) == major:
        return java_home
    for candidate in _candidates(major):
        if release_major(candidate) == major:
            return candidate
    return None
