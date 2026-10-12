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
"""
Git helpers.  Every command runs with env() (GIT_TERMINAL_PROMPT=0), text=True and errors="replace".

Readers always capture.  The path-list readers and has_upstream fail closed: if git can't run or the directory
isn't a repository they raise GitError, so an empty list means "nothing there" and False means "no upstream", never
"git failed".

Writers return a Result and relay git's output as Foundry's push.py does: inherited by the terminal in a plain run
(out and err stay empty), captured and shown as notes under a console view (info on success, error on failure), and
captured into the Result in either mode with capture=True.

No helper here runs `git add`, passes --force or --no-verify, or writes git config.  commit_index commits the index
exactly as found.  commit_paths is a pathspec commit with git's --only semantics: it commits the working-tree content
of the named tracked paths, including unstaged edits to them, and leaves every other staged path staged.
"""

import os
import shlex
import subprocess

from . import _base
from ._base import Result


class GitError(RuntimeError):
    """A git reader could not run or the directory isn't a repository.  result is the failed command's Result."""

    def __init__(self, cmd, result):
        self.cmd = list(cmd)
        self.result = result
        lines = (result.err or result.out).strip().splitlines()
        detail = f": {lines[-1]}" if lines else ""
        super().__init__(f"{' '.join(self.cmd[:2])} failed (exit code {result.code}){detail}")


def env(extra=None):
    """A copy of os.environ with GIT_TERMINAL_PROMPT=0, merged with extra."""
    merged = dict(os.environ)
    merged["GIT_TERMINAL_PROMPT"] = "0"
    if extra:
        merged.update(extra)
    return merged


def _run(cmd, *, cwd=".", input=None, capture=True):
    """The one subprocess entry point.  A git that can't start gives Result(False, 127, err=str(e))."""
    pipe = subprocess.PIPE if capture else None
    try:
        done = subprocess.run(cmd, cwd=cwd, env=env(), input=input, text=True, errors="replace", stdout=pipe,
                              stderr=pipe)
    except OSError as e:
        return Result(False, 127, err=str(e))
    return Result(done.returncode == 0, done.returncode, out=done.stdout or "", err=done.stderr or "")


def _last(result, fallback):
    lines = (result.err or result.out).strip().splitlines()
    return lines[-1] if lines else fallback


def _read(cmd, repo):
    result = _run(cmd, cwd=repo)
    if not result.ok:
        raise GitError(cmd, result)
    return result.out


def _paths(cmd, repo):
    return [p for p in _read(cmd, repo).split("\0") if p]


def _require_repo(repo):
    # Outside a repository `git diff` exits 129 with a usage dump; rev-parse gives 128 and "not a git repository".
    _read(["git", "rev-parse", "--git-dir"], repo)


def staged_paths(repo="."):
    """`git diff --cached --name-only`.  Raises GitError when git fails."""
    _require_repo(repo)
    return _paths(["git", "diff", "--cached", "--name-only", "-z"], repo)


def unstaged_paths(repo="."):
    """`git diff --name-only`.  Raises GitError when git fails."""
    _require_repo(repo)
    return _paths(["git", "diff", "--name-only", "-z"], repo)


def untracked_paths(repo="."):
    """`git ls-files --others --exclude-standard`.  Raises GitError when git fails."""
    return _paths(["git", "ls-files", "--others", "--exclude-standard", "-z"], repo)


def changed_under(repo=".", prefix="", *, base="HEAD", untracked=True):
    """Paths under prefix that differ from base (staged or not), plus untracked ones when asked.  Sorted.  Paths are
    relative to the repository root when repo is its top level."""
    _require_repo(repo)
    pathspec = ["--", prefix] if prefix else []
    paths = set(_paths(["git", "diff", "--name-only", "-z", base, *pathspec], repo))
    if untracked:
        paths.update(_paths(["git", "ls-files", "--others", "--exclude-standard", "-z", *pathspec], repo))
    return sorted(paths)


def _scalar(cmd, repo):
    result = _run(cmd, cwd=repo)
    value = result.out.strip()
    return value if result.ok and value else None


def current_branch(repo="."):
    """The branch name, or None on a detached HEAD (or when git can't answer)."""
    return _scalar(["git", "symbolic-ref", "--short", "-q", "HEAD"], repo)


def short_head(repo="."):
    """The short SHA of HEAD, or None before the first commit."""
    return _scalar(["git", "rev-parse", "--short", "-q", "--verify", "HEAD"], repo)


def remote_url(repo=".", remote="origin"):
    return _scalar(["git", "remote", "get-url", remote], repo)


def user_email(repo="."):
    """`git config --get user.email`, or None.  Read-only."""
    return _scalar(["git", "config", "--get", "user.email"], repo)


def has_upstream(repo=".", branch=None):
    """True when branch (default: the current one) has an upstream.  Raises GitError when git can't run or repo isn't
    a repository, so False only ever means "no upstream"."""
    _require_repo(repo)
    ref = f"{branch}@{{upstream}}" if branch else "@{upstream}"
    return _run(["git", "rev-parse", "--abbrev-ref", "--symbolic-full-name", ref], cwd=repo).ok


def ahead_behind(repo=".", *, fetch):
    """(ahead, behind, err) against the upstream.  fetch=True runs `git fetch -q` first.  Any failure gives
    (None, None, err); no upstream gives (None, None, "no upstream").  Raises GitError when repo isn't a repository."""
    if not has_upstream(repo):
        return None, None, "no upstream"
    if fetch:
        result = _run(["git", "fetch", "-q"], cwd=repo)
        if not result.ok:
            return None, None, _last(result, f"git fetch failed (exit code {result.code})")
    result = _run(["git", "rev-list", "--left-right", "--count", "HEAD...@{upstream}"], cwd=repo)
    if not result.ok:
        return None, None, _last(result, f"git rev-list failed (exit code {result.code})")
    ahead, behind = (int(n) for n in result.out.split())
    return ahead, behind, None


def stash_hint(paths):
    """`git stash push -- <shell-quoted paths>`, then `git stash pop` on a second line."""
    return "git stash push -- " + " ".join(shlex.quote(p) for p in paths) + "\ngit stash pop"


def _write(cmd, repo, *, input=None, capture=False):
    _base.remember(" ".join(cmd[:2]))
    jr = _base.jr()
    console = jr is not None and jr.console_active()
    result = _run(cmd, cwd=repo, input=input, capture=capture or console)
    if console and not capture:
        level = "info" if result.ok else "error"
        for line in (result.out + result.err).splitlines():
            jr.say(line, level)
        return Result(result.ok, result.code)
    return result


def commit_index(message, repo=".", *, stdin=False, capture=False):
    """Commit the index exactly as found: `git commit -m <message>`, or `git commit -F -` with the message on stdin."""
    if stdin:
        return _write(["git", "commit", "-F", "-"], repo, input=message, capture=capture)
    return _write(["git", "commit", "-m", message], repo, capture=capture)


def commit_paths(paths, message, repo=".", *, capture=False):
    """`git commit -m <message> -- <paths>`: a pathspec commit with --only semantics (see the module docstring).  Runs
    no `git add`, so an untracked path must be added by the caller first."""
    paths = [os.fspath(p) for p in paths]
    if not paths:
        raise ValueError("commit_paths needs at least one path")
    return _write(["git", "commit", "-m", message, "--", *paths], repo, capture=capture)


def push(repo=".", *, remote="origin", branch=None, set_upstream=False, capture=False):
    """`git push [-u] <remote> <branch>`; branch defaults to the current one.  A detached HEAD with no branch gives
    Result(ok=False) without pushing."""
    branch = branch or current_branch(repo)
    if branch is None:
        return Result(False, 1, err="detached HEAD: pass branch= to push")
    return _write(["git", "push", *(["-u"] if set_upstream else []), remote, branch], repo, capture=capture)


def rebase_onto_upstream(repo=".", *, autostash=True, capture=False):
    """`git rebase [--autostash] @{upstream}`."""
    return _write(["git", "rebase", *(["--autostash"] if autostash else []), "@{upstream}"], repo, capture=capture)


def tag(name, repo=".", *, message=None, sign=False, capture=False):
    """An annotated tag (-a) when message is given, a signed one (-s) with sign=True, otherwise a lightweight tag."""
    if sign or message is not None:
        cmd = ["git", "tag", "-s" if sign else "-a", name, "-m", message if message is not None else name]
    else:
        cmd = ["git", "tag", name]
    return _write(cmd, repo, capture=capture)


def delete_tag(name, repo=".", *, remote=None, capture=False):
    """`git tag -d <name>`, then with remote `git push <remote> :refs/tags/<name>`."""
    result = _write(["git", "tag", "-d", name], repo, capture=capture)
    if not result.ok or remote is None:
        return result
    return _write(["git", "push", remote, f":refs/tags/{name}"], repo, capture=capture)
