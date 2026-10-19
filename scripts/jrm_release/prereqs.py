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
    "svnmucc": (["svnmucc", "--version", "--quiet"], (1, 10), "needs --password-from-stdin",
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
    out = ctx.runner.run([binary, "-version"], title="java version", mutating=False, merge_err=True).out
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
    out = ctx.runner.run(argv, title=f"{name} version", mutating=False).out
    got = parse_version(out)
    if got is None:
        return f"{name}: cannot read a version from `{' '.join(argv)}`; output starts: {_first_line(out)}"
    if (got[0], got[1]) < minimum:
        ver = ".".join(map(str, minimum))
        return f"{name}: {ver}+ required, found {got[2]}; {why} ({hint})"
    return None


def check(ctx):
    """Run every answer-independent check, say each ✔/❌ line, and return the failure strings (empty = all good).
    The result is kept on ctx.prereq so the Prerequisites step reports it without running anything again."""
    failed = []

    def report(name, failure):
        ctx.say(f"{'❌' if failure else '✔'} {name}", "error" if failure else "info")
        if failure:
            failed.append(failure)

    for name in TOOLS:
        report(name, _tool_failure(ctx, name))
    report("nexus reachable", None if ctx.http_ok(inventory.NEXUS_URL)
           else f"nexus reachable: cannot reach {inventory.NEXUS_URL} (check your network or proxy)")
    if shutil.which("svn") is None:
        ctx.say("- dist/dev reachable: skipped (svn is missing)")
    else:   # anonymous: no username, no password
        report("dist/dev reachable", None if ctx.svn.ls(inventory.DIST_DEV + "/") is not None
               else f"dist/dev reachable: cannot list {inventory.DIST_DEV} anonymously (check your network or proxy)")
    ctx.prereq = failed
    return failed


def step(ctx):
    """The Prerequisites step: the early result when release.py already ran it, else run it now."""
    failed = ctx.prereq if ctx.prereq is not None else check(ctx)
    if failed:
        raise StepError("; ".join(failed))
    return "all prerequisites met"
