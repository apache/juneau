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
"""Preflight: every precondition a release depends on, as a flat registry of named checks.  Each check is fn(ctx) ->
Result(status, reason): "pass", "fail" (the release would break) or "cant" (it could not be checked: no network, an
input another check already failed on).  run_checks never lets one check stop another.  `release.py --check` prints the
whole table; a new run refuses to start on any non-pass; --resume runs only RESUME_GROUPS.  No reason, argv or log line
ever holds a secret: secrets reach gpg on stdin and gh in its environment, and are only reported by where they came from."""
import importlib.util
import json
import os
import re
import shutil
import tempfile
from collections import namedtuple
from datetime import datetime

from . import details, gpgkeys, inventory, params, vote
from .nexus import HttpError

PASS, FAIL, CANT = "pass", "fail", "cant"
Result = namedtuple("Result", "status reason")
GROUPS = ("tools", "params", "secrets-present", "secrets-valid", "repo", "notes", "remote")
RESUME_GROUPS = ("tools", "secrets-present", "secrets-valid")
REGISTRY = []   # [(group, name, fn(ctx) -> Result)] in table order; "<version>" in a name is filled in once known

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
# gpg-agent must keep the passphrase from gpg-unlock until release-perform signs, well over the 10 minutes it keeps it by
# default: default-cache-ttl counts from last use, max-cache-ttl is absolute.
MIN_DEFAULT_CACHE_TTL = 1800
MIN_MAX_CACHE_TTL = 7200


def parse_version(text):
    """(major, minor, dotted) from a tool's version output, or None.  A java banner's quoted version wins."""
    text = text or ""
    # The closing quote is found with str.find rather than a trailing [^"]*", which would backtrack over the digits.
    m = re.search(r'version "(\d+)(?:\.(\d+))?', text)
    close = text.find('"', m.end()) if m else -1
    if close >= 0:
        return int(m[1]), int(m[2] or 0), text[m.start():close + 1]
    m = re.search(r"(?<!\d)(\d+)\.(\d+)(?:\.\d+)*", text)
    if not m:
        return None
    return int(m[1]), int(m[2]), m[0]


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


class Cant(Exception):
    """Raised inside a check when its input is unavailable; reported as can't-check with the message."""


def check(group, name):
    def register(fn):
        REGISTRY.append((group, name, fn))
        return fn
    return register


def _ok(reason="ok"):
    return Result(PASS, reason)


def _fail(reason):
    return Result(FAIL, reason)


def _skipped(ctx):
    """The pass result of a check that needs a real secret, under --dry-run (no secret is looked up), else None."""
    return Result(PASS, "skipped (dry run)") if ctx.dry_run else None


def _details(ctx):
    d = ctx.pf.get("details") or ctx.rf.get("details")
    if not d:
        raise Cant("no usable params file")
    return d


def _rc_tag(ctx):
    d = _details(ctx)
    return f"juneau-{d['version']}-RC{d['rc']}"


def display_name(ctx, name):
    """name with <version> replaced by the release version, when the details are known."""
    if "<version>" not in name:
        return name
    try:
        return name.replace("<version>", _details(ctx)["version"])
    except Cant:
        return name


def run_checks(ctx, groups=GROUPS):
    """[(group, name, Result)] for every registered check in the groups; none short-circuits another, and an exception
    inside a check is a can't-check naming its class.  The scratch repo (if any check made one) is removed at the end."""
    out = []
    try:
        for group, name, fn in list(REGISTRY):
            if group not in groups:
                continue
            try:
                res = fn(ctx)
            except Cant as e:
                res = Result(CANT, str(e))
            except Exception as e:  # noqa: BLE001 - a check never takes the run down
                res = Result(CANT, f"{type(e).__name__}: {e}")
            out.append((group, display_name(ctx, name), res))
    finally:
        cleanup(ctx)
    return out


def blocks(res):
    """Whether a result stops a release: a fail, or a can't-check other than a deliberate skip."""
    return res.status == FAIL or (res.status == CANT and not res.reason.startswith("skipped"))


def exit_code(results):
    """2 when anything failed, else 1 when anything could not be checked, else 0."""
    statuses = {r.status for _, _, r in results}
    return 2 if FAIL in statuses else 1 if CANT in statuses else 0


def format_table(results):
    """The --check table: a marker, the check, its reason, then a summary line."""
    mark = {PASS: "PASS", FAIL: "FAIL", CANT: "???"}
    width = max([len(n) for _, n, _ in results] + [10])
    lines = [f"{mark[r.status]:<4}  {n:<{width}}  {r.reason}" for _, n, r in results]
    count = lambda s: sum(1 for _, _, r in results if r.status == s)  # noqa: E731
    lines.append(f"{count(PASS)} passed, {count(FAIL)} failed, {count(CANT)} could not be checked")
    return lines


def problems(results):
    """The non-passing results as `name: reason` lines, for a refusal message."""
    return [f"{n}: {r.reason}" for _, n, r in results if r.status != PASS]


def cleanup(ctx):
    scratch = ctx.pf.pop("scratch", None)
    ctx.pf.pop("tip_ok", None)
    if scratch:
        shutil.rmtree(scratch, ignore_errors=True)


def prepare(ctx, args):
    """Load and resolve the params file into ctx.pf (a failure is stored for the 'params file' check, never raised) and
    seed ctx.creds with the non-secret identity."""
    ctx.pf.clear()
    try:
        loaded = params.load(params.require(args))
    except params.ParamsError as e:
        ctx.pf["params_error"] = str(e)
        return
    ctx.pf["loaded"] = loaded
    ctx.creds.update(loaded.creds)
    try:
        ctx.pf["details"] = params.resolve(ctx, loaded, getattr(args, "version", None))
    except params.ParamsError as e:
        ctx.pf["details_error"] = str(e)


# ---- Tools ---------------------------------------------------------------------------------------------------------------

def _tool(name):
    def fn(ctx):
        failure = _tool_failure(ctx, name)
        return _fail(failure) if failure else _ok("found")
    return fn


for _name in TOOLS:
    REGISTRY.append(("tools", f"tool {_name}", _tool(_name)))


@check("tools", "PyYAML")
def _pyyaml(ctx):
    return _ok("installed") if importlib.util.find_spec("yaml") is not None else _fail(
        "not installed; params files need it (python3 -m pip install pyyaml)")


@check("tools", "nexus reachable")
def _nexus_reachable(ctx):
    return _ok(inventory.NEXUS_URL) if ctx.http_ok(inventory.NEXUS_URL) else Result(
        CANT, f"cannot reach {inventory.NEXUS_URL} (check your network or proxy)")


@check("tools", "dist/dev reachable")
def _dist_reachable(ctx):
    if shutil.which("svn") is None:
        return Result(CANT, "skipped (svn is missing)")
    if ctx.svn.ls(inventory.DIST_DEV + "/", quiet=True) is None:   # anonymous: no username, no password
        return Result(CANT, f"cannot list {inventory.DIST_DEV} anonymously (check your network or proxy)")
    return _ok(inventory.DIST_DEV)


def _agent_ttl(ctx, option):
    """The effective seconds of a gpg-agent option from `gpgconf --list-options gpg-agent`: the configured value (index 9)
    if set, else the default (index 7).  Cant when gpgconf is missing or the option cannot be parsed."""
    if shutil.which("gpgconf") is None:
        raise Cant("gpgconf is missing")
    r = ctx.runner.quiet(["gpgconf", "--list-options", "gpg-agent"], title="gpg-agent options")
    if not r.ok:
        raise Cant("gpgconf --list-options gpg-agent failed")
    for line in r.out.splitlines():
        f = line.split(":")
        if f[0] == option and len(f) > 9:   # 0-based: f[7] default, f[9] value
            value = (f[9] or f[7]).strip().lstrip('"')
            if value.isdigit():
                return int(value)
    raise Cant(f"cannot read {option} from gpgconf --list-options gpg-agent")


@check("tools", "gpg-agent cache TTL")
def _gpg_agent_ttl(ctx):
    default_ttl, max_ttl = _agent_ttl(ctx, "default-cache-ttl"), _agent_ttl(ctx, "max-cache-ttl")
    low = [f"{n} is {v}s (needs >= {m})" for n, v, m in (("default-cache-ttl", default_ttl, MIN_DEFAULT_CACHE_TTL),
                                                         ("max-cache-ttl", max_ttl, MIN_MAX_CACHE_TTL)) if v < m]
    if low:
        return _fail("; ".join(low) + ": run python3 scripts/gpg-agent-ttl.py (it sets default-cache-ttl "
                     "and max-cache-ttl in ~/.gnupg/gpg-agent.conf and reloads gpg-agent)")
    return _ok(f"default-cache-ttl {default_ttl}s, max-cache-ttl {max_ttl}s")


# ---- Params --------------------------------------------------------------------------------------------------------------

@check("params", "params file")
def _params_file(ctx):
    if "params_error" in ctx.pf:
        return _fail(ctx.pf["params_error"])
    if "loaded" not in ctx.pf:
        raise Cant("no params file given")
    return _ok(str(ctx.pf["loaded"].path))


@check("params", "release details")
def _release_details(ctx):
    if "details_error" in ctx.pf:
        return _fail(ctx.pf["details_error"])
    d = _details(ctx)
    return _ok(f"{d['version']} RC{d['rc']} on {d['branch']}, next {d['developmentVersion']}")


@check("params", "vote block")
def _vote_block(ctx):
    """The `vote:` block, once the run has passed the vote gate.  Before that (or for a new release) it is not needed."""
    version = _details(ctx)["version"]
    run_json = ctx.home / "runs" / version / "run.json"
    data = {}
    if run_json.exists():
        try:
            data = json.loads(run_json.read_text(encoding="utf-8"))
        except (OSError, ValueError) as e:
            raise Cant(f"cannot read {run_json}: {e}")          # never "not needed yet" for a run file that will not parse
    last = data.get("last")
    reached = last in inventory.NAMES and inventory.NAMES.index(last) >= inventory.NAMES.index("vote-gate")
    if data.get("vote") or not reached:
        return _ok("not needed yet")
    tally, problems = params.load_vote(vote.params_path_for(data, ctx.home, version))
    if problems:
        return _fail("; ".join(problems))
    return _ok(f"+1 {tally['plus1']}, 0 {tally['zero']}, -1 {tally['minus1']}, {len(tally['binding'])} binding")


# ---- Secrets present / valid ---------------------------------------------------------------------------------------------

SECRET_CHECKS = (("Apache password", "credential.apache.password"), ("GPG passphrase", "credential.gpg.passphrase"),
                 ("GitHub token", "credential.github.token"))
LABELS_BY_KEY = {k: label for label, k in SECRET_CHECKS}


def _present(key):
    def fn(ctx):
        if _skipped(ctx):
            return _skipped(ctx)
        if ctx.find_secret(key) is None:
            return _fail(f"not found: {params.SECRET_HINTS[key]}")
        return _ok(f"found ({ctx.secret_source.get(key, 'cache')})")
    return fn


for _label, _key in SECRET_CHECKS:
    REGISTRY.append(("secrets-present", f"{_label} present", _present(_key)))


def _secret_or_cant(ctx, key):
    """The secret, or Cant when it cannot be found (its 'present' check has already failed)."""
    value = ctx.find_secret(key)
    if value is None:
        raise Cant(f"no {LABELS_BY_KEY[key]} to validate")
    return value


def _key_in_ring(ctx):
    """The configured gpg.keyid's Key from the local secret keyring, or None; Cant when it cannot be listed or none is set."""
    keyid = ctx.creds.get("credential.gpg.keyid")
    if not keyid:
        raise Cant("no gpg.keyid")
    r = ctx.runner.quiet(["gpg", "--list-secret-keys", "--with-colons", "--keyid-format", "long"], title="List GPG keys")
    if not r.ok:
        raise Cant("cannot list the gpg keyring")
    want = keyid.upper()
    return next((k for k in gpgkeys.parse(r.out) if want == k.keyid.upper() or want == k.fingerprint.upper()
                 or (k.fingerprint and k.fingerprint.upper().endswith(want))), None)


@check("secrets-valid", "Apache password valid")
def _apache_password_valid(ctx):
    if _skipped(ctx):
        return _skipped(ctx)
    _secret_or_cant(ctx, "credential.apache.password")
    ctx.cred("credential.apache.availid")
    try:
        ctx.nexus.repositories()   # authenticated, read-only staging-profile query
    except HttpError as e:
        if e.status in (401, 403):
            return _fail(f"Nexus rejected the Apache id or password (HTTP {e.status})")
        return Result(CANT, f"Nexus answered HTTP {e.status}")
    except OSError as e:
        return Result(CANT, f"cannot reach Nexus: {e}")
    return _ok("Nexus accepted the credentials")


@check("secrets-valid", "GPG passphrase valid")
def _gpg_passphrase_valid(ctx):
    if _skipped(ctx):
        return _skipped(ctx)
    if shutil.which("gpg") is None:
        raise Cant("gpg is missing")
    passphrase = _secret_or_cant(ctx, "credential.gpg.passphrase")
    keyid = ctx.creds.get("credential.gpg.keyid")
    if not keyid:
        raise Cant("no gpg.keyid")
    tmp = tempfile.mkdtemp(prefix="jrm-gpg-")
    try:
        blob = os.path.join(tmp, "blob.txt")
        with open(blob, "w", encoding="utf-8") as f:
            f.write("preflight")
        r = ctx.runner.quiet(["gpg", "--batch", "--yes", "--pinentry-mode", "loopback", "--passphrase-fd", "0",
                              "--local-user", keyid, "--armor", "--detach-sign", "--output", blob + ".asc", blob],
                             title="gpg preflight sign", input=(passphrase + "\n").encode(), timeout=60)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    return _ok("signed a throwaway blob") if r.ok else _fail("gpg could not sign with that key and passphrase")


@check("secrets-valid", "GPG key not expired")
def _gpg_key_valid(ctx):
    keyid = ctx.creds.get("credential.gpg.keyid")
    found = _key_in_ring(ctx)
    if found is None:
        return _fail(f"key {keyid} is not a secret key in the gpg keyring")
    if "s" not in found.caps.lower():
        return _fail(f"key {keyid} cannot sign")
    if found.validity in ("r", "e", "i", "d"):
        return _fail(f"key {keyid} is revoked, expired, invalid or disabled")
    if found.expires and found.expires <= ctx.now().timestamp():
        return _fail(f"key {keyid} expired")
    when = f"expires {datetime.fromtimestamp(found.expires):%Y-%m-%d}" if found.expires else "no expiry"
    return _ok(f"key {keyid} usable, {when}")


@check("secrets-valid", "GitHub token valid")
def _github_token_valid(ctx):
    if _skipped(ctx):
        return _skipped(ctx)
    token = _secret_or_cant(ctx, "credential.github.token")
    r = ctx.runner.quiet(["gh", "api", "user", "--include"], title="gh api user", env={"GH_TOKEN": token}, timeout=30)
    parts = next(iter((r.out or "").splitlines()), "").split()
    status = int(parts[1]) if len(parts) > 1 and parts[0].startswith("HTTP") and parts[1].isdigit() else None
    if status is None:
        raise Cant("no HTTP answer from api.github.com")
    if status in (401, 403):
        return _fail(f"GitHub rejected the token (HTTP {status})")
    if status != 200:
        return Result(CANT, f"GitHub answered HTTP {status}")
    scopes = next((ln.split(":", 1)[1] for ln in r.out.splitlines() if ln.lower().startswith("x-oauth-scopes:")), None)
    if scopes is None:
        return Result(CANT, "scopes not reported (fine-grained token?); cannot confirm the repo scope")
    return _ok("repo scope present") if "repo" in [s.strip() for s in scopes.split(",")] else _fail(
        "token lacks the repo scope")


# ---- Repo state, Release notes, Remote state -----------------------------------------------------------------------------

def _ls_remote(ctx, ref):
    r = ctx.runner.quiet(["git", "ls-remote", inventory.GIT_URL, ref], title=f"ls-remote {ref}", timeout=60)
    if not r.ok:
        raise Cant(f"cannot reach {inventory.GIT_URL}")
    return r.out.strip()


@check("repo", "branch on origin")
def _branch_on_origin(ctx):
    branch = _details(ctx)["branch"]
    return _ok(branch) if _ls_remote(ctx, f"refs/heads/{branch}") else _fail(
        f"branch {branch} is not on gitbox (check the branch name)")


@check("repo", "RC tag absent")
def _rc_tag_absent(ctx):
    tag = _rc_tag(ctx)
    return _fail(f"tag {tag} already exists (bump rc, or run --revert)") if _ls_remote(
        ctx, f"refs/tags/{tag}") else _ok(f"{tag} is free")


def _tip_file(ctx, path):
    """The text of path at the release branch's tip on gitbox, through one depth-1 fetch into a scratch bare repo
    (created on first use, removed by run_checks).  Cant when the fetch or the read fails."""
    branch = _details(ctx)["branch"]
    if "scratch" not in ctx.pf:
        scratch = tempfile.mkdtemp(prefix="jrm-tip-")
        ctx.pf["scratch"] = scratch
        ctx.pf["tip_ok"] = ctx.runner.quiet(["git", "init", "--bare", "-q", scratch], title="scratch repo").ok and \
            ctx.runner.quiet(["git", "-C", scratch, "fetch", "-q", "--depth", "1", inventory.GIT_URL, branch],
                             title="fetch branch tip", timeout=180).ok
    if not ctx.pf.get("tip_ok"):
        raise Cant("cannot fetch the branch tip from gitbox")
    r = ctx.runner.quiet(["git", "-C", ctx.pf["scratch"], "show", f"FETCH_HEAD:{path}"], title=f"read {path}")
    if not r.ok:
        raise Cant(f"cannot read {path} at the branch tip")
    return r.out


@check("repo", "pom is <version>-SNAPSHOT")
def _pom_snapshot(ctx):
    version = _details(ctx)["version"]
    cur = details.pom_version(_tip_file(ctx, "pom.xml"))
    return _ok(f"pom is {cur}") if cur == f"{version}-SNAPSHOT" else _fail(
        f"pom version is {cur}; the branch must be at {version}-SNAPSHOT")


@check("repo", "GPG key in KEYS")
def _key_in_keys(ctx):
    mine = _key_in_ring(ctx)
    keyid = ctx.creds.get("credential.gpg.keyid")
    if mine is None:
        raise Cant(f"key {keyid} is not in the local keyring")
    text = ctx.http_text(inventory.DOWNLOADS + "/KEYS")
    if text is None:
        raise Cant(f"cannot read {inventory.DOWNLOADS}/KEYS")
    return _ok(f"key {keyid} is published") if gpgkeys.in_keys_file(text, mine) else _fail(
        f"key {keyid} is not in {inventory.DOWNLOADS}/KEYS; add it before the vote")


@check("notes", "RELEASE-NOTES has <version>")
def _release_notes(ctx):
    version = _details(ctx)["version"]
    pattern = re.compile(r"^Release Notes - Juneau - Version " + re.escape(version) + r"( - .*)?$", re.M)
    return _ok(f"section for {version} found") if pattern.search(_tip_file(ctx, "RELEASE-NOTES.txt")) else _fail(
        f"RELEASE-NOTES.txt on the branch has no 'Release Notes - Juneau - Version {version}' section")


@check("remote", "no open staging repo")
def _no_open_staging(ctx):
    if _skipped(ctx):
        return _skipped(ctx)
    _secret_or_cant(ctx, "credential.apache.password")
    try:
        repos = ctx.nexus.repositories()
    except (HttpError, OSError) as e:
        raise Cant(f"cannot list staging repositories: {e}") from e
    openr = [r["repositoryId"] for r in repos if r.get("state") == "open"]
    closed = [r["repositoryId"] for r in repos if r.get("state") == "closed"]
    if openr:
        return _fail(f"open staging repository: {', '.join(openr)} (drop it, or run --revert)")
    return _ok("none open" + (f" ({len(closed)} closed, informational)" if closed else ""))


@check("remote", "no dist/dev RC dir")
def _no_dist_dev(ctx):
    tag = _rc_tag(ctx)
    present = []
    for kind in ("source", "binaries"):
        url = f"{inventory.DIST_DEV}/{kind}/{tag}"
        found = ctx.svn.exists(url)
        if found is None:
            raise Cant(f"cannot list {url}")
        if found:
            present.append(url)
    return _fail(f"already exists: {', '.join(present)}") if present else _ok("none present")
