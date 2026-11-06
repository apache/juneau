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
The record of which JDKs a commit passed the build gate on, written by push.py and test.py after a green gate and read by
the release manager for the vote email ("Tested on JDK 17, 21 and 28-ea.").

The file is ~/.cache/juneau-terminal/jdk-passes.json (JUNEAU_JDK_PASSES overrides the path).  Format:

    {"<full commit sha>": [{"jdk": "28-ea", "time": "2026-11-06T12:00:00+00:00", "source": "push"}, ...]}

"jdk" is the major version as the JDK reports it ("17", "21", "28-ea"), "time" is UTC ISO-8601 and "source" is "push",
"test" or "release".  A commit has at most one entry per JDK (a later pass replaces the earlier one).  Writes are atomic
(temp file + rename); a missing or corrupt file reads as empty.  Entries older than 90 days, and all but the newest 300
commits, are dropped on write.  The release manager keeps a small reader of the same format.
"""

import json
import os
import re
import subprocess
import tempfile
from datetime import datetime, timedelta, timezone
from pathlib import Path

MAX_AGE_DAYS = 90
MAX_COMMITS = 300


def path():
    """The record file: $JUNEAU_JDK_PASSES, else ~/.cache/juneau-terminal/jdk-passes.json."""
    override = os.environ.get("JUNEAU_JDK_PASSES")
    return Path(override) if override else Path.home() / ".cache" / "juneau-terminal" / "jdk-passes.json"


def _load(file):
    """The parsed record, or {} when the file is missing, unreadable or not a JSON object."""
    try:
        data = json.loads(file.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}
    return data if isinstance(data, dict) else {}


def major(version_line):
    """
    The major version label for a `java -version` first line or a bare version string: "17.0.12" and
    'openjdk version "17.0.12" 2024-07-16' give "17", "1.8.0_402" gives "8", '"28-ea"' gives "28-ea".  None when
    no version is found.
    """
    match = re.search(r'"?(\d+(?:\.\d+)*)([^"\s]*)', version_line or "")
    if not match:
        return None
    parts = match.group(1).split(".")
    number = parts[1] if parts[0] == "1" and len(parts) > 1 else parts[0]
    return number + ("-ea" if "-ea" in match.group(2) else "")


def jdk_label(java_home=None):
    """The major-version label of the JDK in java_home (else $JAVA_HOME, else java on PATH); None if it cannot be run."""
    home = java_home or os.environ.get("JAVA_HOME")
    java = str(Path(home) / "bin" / "java") if home else "java"
    try:
        result = subprocess.run([java, "-version"], capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.SubprocessError):
        return None
    return major(((result.stderr or result.stdout).strip().splitlines() or [""])[0])


def _prune(data, now):
    """data minus entries older than MAX_AGE_DAYS, then minus all but the newest MAX_COMMITS commits."""
    cutoff = (now - timedelta(days=MAX_AGE_DAYS)).isoformat()
    kept = {}
    for sha, entries in data.items():
        if not isinstance(entries, list):
            continue
        fresh = [e for e in entries if isinstance(e, dict) and str(e.get("time", "")) >= cutoff]
        if fresh:
            kept[sha] = fresh
    newest = sorted(kept, key=lambda s: max(e["time"] for e in kept[s]), reverse=True)[:MAX_COMMITS]
    return {sha: kept[sha] for sha in newest}


def record(sha, jdk, source, file=None, now=None):
    """
    Record that sha passed the gate on jdk (a label such as "21" or "28-ea") from source ("push", "test" or "release").
    Never raises: returns True when written, False when there was nothing to write or the write failed.
    """
    if not sha or not jdk:
        return False
    file = Path(file) if file else path()
    now = now or datetime.now(timezone.utc)
    try:
        data = _load(file)
        entries = [e for e in data.get(sha, []) if isinstance(e, dict) and e.get("jdk") != jdk]
        entries.append({"jdk": jdk, "time": now.isoformat(), "source": source})
        data[sha] = entries
        data = _prune(data, now)
        file.parent.mkdir(parents=True, exist_ok=True)
        fd, tmp = tempfile.mkstemp(dir=file.parent, prefix=".jdk-passes-", suffix=".tmp")
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as out:
                json.dump(data, out, indent=1, sort_keys=True)
            os.replace(tmp, file)
        except BaseException:
            Path(tmp).unlink(missing_ok=True)
            raise
        return True
    except (OSError, ValueError):
        return False


def passes(sha, file=None):
    """The JDK labels sha passed on, oldest major first ("17", "21", "28-ea"); [] when there are none."""
    entries = _load(Path(file) if file else path()).get(sha, [])
    labels = {e["jdk"] for e in entries if isinstance(e, dict) and isinstance(e.get("jdk"), str)}
    return sorted(labels, key=lambda label: (int(re.match(r"\d+", label).group()) if re.match(r"\d+", label) else 0, label))


def head_sha(repo, require_clean=False):
    """HEAD of repo as a full sha; None when git fails, or when require_clean and the tree has uncommitted changes."""
    try:
        if require_clean:
            status = subprocess.run(["git", "status", "--porcelain"], cwd=str(repo), capture_output=True, text=True, check=True)
            if status.stdout.strip():
                return None
        result = subprocess.run(["git", "rev-parse", "HEAD"], cwd=str(repo), capture_output=True, text=True, check=True)
    except (OSError, subprocess.SubprocessError):
        return None
    return result.stdout.strip() or None


def record_head(repo, source, require_clean=False):
    """Record HEAD of repo as passed on the current JDK (see jdk_label).  Best effort; returns True when written."""
    return record(head_sha(repo, require_clean), jdk_label(), source)
