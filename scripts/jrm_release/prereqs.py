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
"""Prerequisites: everything release.py can verify before it asks a single question -- tools, versions and
credential-free reachability.  Every failure is collected, so one run shows the whole list."""
import os
import re
import shutil

from . import inventory
from .ctx import StepError

# name -> (version argv or None, minimum (major, minor) or None, why the minimum, install hint)
# Floors follow the commands release.py actually runs:
#   mvn  3.6  release:prepare/perform and javadoc:aggregate run in batch mode (-B); the release plugin 3.x line needs
#             Maven 3.6.3+ (checked to major.minor only).
#   java 17   what Juneau itself builds on.
#   svn/svnmucc 1.10  --password-from-stdin (added in 1.10).
#   gpg  2.1  --pinentry-mode loopback with --passphrase-fd (both 2.1+).
#   gh   2.28 `release create --verify-tag` plus `--latest=false`; both are in `gh release create --help` on
#             2.89.0 (read locally).  The release in which each flag first appeared could not be looked up offline;
#             2.28 is a conservative recollection (--latest and --verify-tag were both added in the 2.2x series).
# A version that cannot be read is always a failure, never a silent pass.
TOOLS = {
    "git": (["git", "--version"], None, "", "brew install git"),
    "mvn": (["mvn", "-version"], (3, 6), "", "brew install maven"),
    "java": (["java", "-version"], (17, 0), "Juneau builds on Java 17", "brew install openjdk@17"),
    "svn": (["svn", "--version", "--quiet"], (1, 10), "needs --password-from-stdin", "brew install subversion"),
    "svnmucc": (["svnmucc", "--version"], (1, 10), "needs --password-from-stdin",
                "brew install subversion"),
    "gpg": (["gpg", "--version"], (2, 1), "needs --pinentry-mode loopback", "brew install gnupg"),
    "gh": (["gh", "--version"], (2, 28), "needs release create --verify-tag and --latest=false", "brew install gh"),
    "unzip": (None, None, "", "brew install unzip"),
}
TOOL_NAMES = tuple(TOOLS)


def parse_version(text):
    """(major, minor, dotted) from a tool's version output, or None.  A java banner's quoted version wins."""
    m = re.search(r'version "(\d+)(?:\.(\d+))?[^"]*"', text or "") or re.search(r"(\d+)\.(\d+)(?:\.\d+)*", text or "")
    if not m:
        return None
    return int(m[1]), int(m[2] or 0), m[0]


def java_major(text):
    """The major Java version from any `version "N...` line of `java -version` output (1.8.x is 8), or None.
    Scans every line, because a wrapper may print its own banner first."""
    m = re.search(r'version "(\d+)(?:\.(\d+))?', text or "")
    if not m:
        return None
    return int(m[2] or 0) if m[1] == "1" else int(m[1])


def java_binary():
    """The java mvn will use: $JAVA_HOME/bin/java when it exists and is executable, else `java` on PATH."""
    home = os.environ.get("JAVA_HOME")
    if home:
        exe = os.path.join(home, "bin", "java")
        if os.path.isfile(exe) and os.access(exe, os.X_OK):
            return exe
    return "java"


def _first_line(text):
    return next((ln.strip() for ln in (text or "").splitlines() if ln.strip()), "(no output)")


def _java_failure(ctx):
    binary = java_binary()
    if binary == "java" and shutil.which("java") is None:
        return f"java: not on PATH ({TOOLS['java'][3]})"
    out = ctx.runner.quiet([binary, "-version"], title="java version", merge_err=True).out
    major = java_major(out)
    if major is None:
        return f"java: no version line in the output of {binary} -version; first line: {_first_line(out)}"
    ctx.say(f"- java checked: {binary} (major {major})")
    if major < TOOLS["java"][1][0]:
        found = re.search(r'version "([^"]*)"', out)[1]
        return f"java: 17+ required, found {found} at {binary}; {TOOLS['java'][2]} ({TOOLS['java'][3]})"
    return None


def _tool_failure(ctx, name):
    """A failure string for one tool, or None.  A version that cannot be read is a failure too."""
    argv, minimum, why, hint = TOOLS[name]
    if name == "java":
        return _java_failure(ctx)
    if shutil.which(name) is None:
        return f"{name}: not on PATH ({hint})"
    if minimum is None or argv is None:
        return None
    out = ctx.runner.quiet(argv, title=f"{name} version").out
    got = parse_version(out)
    if got is None:
        return f"{name}: cannot read a version from `{' '.join(argv)}`; output starts: {_first_line(out)}"
    if (got[0], got[1]) < minimum:
        ver = ".".join(map(str, minimum))
        return f"{name}: {ver}+ required, found {got[2]}; {why} ({hint})"
    return None


def _verdict(failure):
    from juneau_scripts import checks   # the toolkit's gate-check model; on sys.path once release.py has bootstrapped
    return checks.Verdict("fail", failure) if failure else checks.PASS


def _plan(ctx):
    """[(name, fn)] for every answer-independent check; fn() returns a failure string or None."""
    plan = [(name, lambda name=name: _tool_failure(ctx, name)) for name in TOOLS]
    plan.append(("nexus reachable", lambda: None if ctx.http_ok(inventory.NEXUS_URL)
                 else f"nexus reachable: cannot reach {inventory.NEXUS_URL} (check your network or proxy)"))
    if shutil.which("svn") is None:
        ctx.say("- dist/dev reachable: skipped (svn is missing)")
    else:   # anonymous: no username, no password
        plan.append(("dist/dev reachable", lambda: None if ctx.svn.ls(inventory.DIST_DEV + "/", quiet=True) is not None
                     else f"dist/dev reachable: cannot list {inventory.DIST_DEV} anonymously "
                          "(check your network or proxy)"))
    return plan


def check(ctx):
    """Run every answer-independent check and return the failure strings (empty = all good).  Nothing is drawn: the
    Prerequisites row (checks() below) replays the verdicts, so its bar is the one that stays on the screen.  The
    result is kept on ctx.prereq and ctx.prereq_verdicts so nothing runs twice."""
    verdicts = {}
    for name, fn in _plan(ctx):
        verdicts[name] = _verdict(fn())
    ctx.prereq_verdicts = verdicts
    ctx.prereq = [v.reason for v in verdicts.values() if v.status == "fail"]
    return ctx.prereq


def checks(ctx):
    """The Prerequisites row's checks, one bar cell each: the early result replayed (run now if release.py has not
    run it)."""
    from juneau_scripts import checks as ck
    if ctx.prereq_verdicts is None:
        check(ctx)
    return [ck.Check(name, lambda v=v: v) for name, v in ctx.prereq_verdicts.items()]


def step(ctx):
    """The Prerequisites step: the early result when release.py already ran it, else run it now."""
    failed = ctx.prereq if ctx.prereq is not None else check(ctx)
    if failed:
        raise StepError("; ".join(failed))
    return "all prerequisites met"
