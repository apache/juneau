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

Under an active console every command is polled rather than waited on: the helpers call juneau_run's tick() every
POLL_SECONDS, so the spinner and the elapsed time keep moving during a `git fetch` or `git push`.

No helper here runs `git add`, passes --force or --no-verify, or writes git config, and only rebase_onto_upstream
passes --autostash (when asked; rebase_keeping_index never does).  commit_index commits the index exactly as found.  commit_paths is a pathspec commit with git's --only semantics: it commits the working-tree
content of the named tracked paths, including unstaged edits to them, and leaves every other staged path staged.

Every `hint` field below is a tuple of lines, ready to print beneath a row.
"""

import os
import shlex
import subprocess
import sys
from dataclasses import dataclass

from . import _base
from ._base import Result

#: How often a polled git command calls juneau_run's tick() under an active console.
POLL_SECONDS = 0.1

#: The message of the stash rebase_keeping_index holds the reviewed changes in.
STASH_MESSAGE = "juneau_scripts: reviewed changes held across the rebase"


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


def _ticker():
    """juneau_run's tick() while a console view owns the screen, else None."""
    jr = _base.jr()
    if jr is None or not jr.console_active():
        return None
    return getattr(jr, "tick", None)


def _polled(cmd, cwd, input, pipe, text, tick):
    stdin = subprocess.PIPE if input is not None else None
    with subprocess.Popen(cmd, cwd=cwd, env=env(), stdin=stdin, stdout=pipe, stderr=pipe, **text) as proc:
        while True:
            try:
                # communicate, not wait: it drains the pipes, so a chatty command can't block on a full one.
                out, err = proc.communicate(input, timeout=POLL_SECONDS)
                return proc.returncode, out, err
            except subprocess.TimeoutExpired:
                tick()


def _run(cmd, *, cwd=".", input=None, capture=True, binary=False):
    """
    The one subprocess entry point.  A git that can't start gives Result(False, 127, err=str(e)).  binary=True reads
    out (and takes input) as bytes; err is still decoded.  Polled under an active console (see the module docstring).
    """
    pipe = subprocess.PIPE if capture else None
    text = {} if binary else {"text": True, "errors": "replace"}
    tick = _ticker()
    try:
        if tick is None:
            done = subprocess.run(cmd, cwd=cwd, env=env(), input=input, stdout=pipe, stderr=pipe, **text)
            code, out, err = done.returncode, done.stdout, done.stderr
        else:
            code, out, err = _polled(cmd, cwd, input, pipe, text, tick)
    except OSError as e:
        return Result(False, 127, out=b"" if binary else "", err=str(e))
    if binary:
        return Result(code == 0, code, out=out or b"", err=(err or b"").decode(errors="replace"))
    return Result(code == 0, code, out=out or "", err=err or "")


def _last(result, fallback):
    lines = (result.err or result.out).strip().splitlines()
    return lines[-1] if lines else fallback


def _read(cmd, repo):
    result = _run(cmd, cwd=repo)
    if not result.ok:
        raise GitError(cmd, result)
    return result.out


def _paths(cmd, repo):
    # Bytes, not text: text mode folds a CR in a name.  -z output is never C-quoted, so each name is the real path.
    result = _run(cmd, cwd=repo, binary=True)
    if not result.ok:
        raise GitError(cmd, result)
    return [os.fsdecode(p) for p in result.out.split(b"\0") if p]


def _require_repo(repo):
    # Outside a repository `git diff` exits 129 with a usage dump; rev-parse gives 128 and "not a git repository".
    _read(["git", "rev-parse", "--git-dir"], repo)


def staged_paths(repo="."):
    """`git diff --cached --name-only --no-renames`: a staged rename lists both sides.  Raises GitError when git
    fails."""
    _require_repo(repo)
    return _paths(["git", "diff", "--cached", "--name-only", "--no-renames", "-z"], repo)


def unstaged_paths(repo="."):
    """`git diff --name-only`.  Raises GitError when git fails."""
    _require_repo(repo)
    return _paths(["git", "diff", "--name-only", "-z"], repo)


def untracked_paths(repo="."):
    """`git ls-files --others --exclude-standard`.  Raises GitError when git fails."""
    return _paths(["git", "ls-files", "--others", "--exclude-standard", "-z"], repo)


def unreviewed_paths(repo=".", *, exempt=()):
    """
    The paths staging hasn't vouched for: unstaged modifications and deletions plus untracked, non-ignored files,
    sorted, minus the repo-relative paths in exempt (compared literally).  Raises GitError when git fails.
    """
    skip = {os.fspath(p) for p in exempt}
    return sorted({*unstaged_paths(repo), *untracked_paths(repo)} - skip)


def unreviewed_refusal(paths, *, outcome, git_cmd="git"):
    """
    The shared refusal for unreviewed paths: (reason, hint).  reason is one line ("2 paths are not staged"); hint
    lists the paths, says why they matter, then outcome ("Nothing was built, committed or pushed.") and the stash
    commands, with git_cmd as the git invocation (e.g. "git -C ../juneau-docs").
    """
    n = len(paths)
    reason = f"{n} path{'' if n == 1 else 's'} {'is' if n == 1 else 'are'} not staged"
    hint = ("these paths are not staged (staging is how this script knows you reviewed them):",
            *(f"  {p}" for p in paths),
            "Why: everything this pushes should be something you've read, not just something that happened to be "
            "in the tree.",
            f"{outcome} Stage each path above, or set it aside for this run and restore it after:",
            *(f"  {line}" for line in stash_hint(paths, git_cmd=git_cmd).splitlines()))
    return reason, hint


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


def stash_hint(paths, *, git_cmd="git"):
    """`git stash push -- <shell-quoted paths>`, then `git stash pop` on a second line; git_cmd replaces "git"."""
    return f"{git_cmd} stash push -- " + " ".join(shlex.quote(p) for p in paths) + f"\n{git_cmd} stash pop"


@dataclass(frozen=True)
class UpstreamVerdict:
    """
    upstream_verdict's answer.  status is one of:
      "ok"           not behind (ahead or level);
      "no-upstream"  nothing to compare against, so the push proceeds;
      "behind"       behind only: hint is the ff-only merge;
      "diverged"     behind and ahead: hint is stash, pull --rebase, stash pop --index;
      "error"        the fetch or the comparison failed: fail closed.
    ahead and behind are None for "no-upstream" and "error".
    """
    status: str
    ahead: int | None = None
    behind: int | None = None
    reason: str = ""
    hint: tuple = ()

    @property
    def ok(self):
        """True when the push may proceed ("ok" or "no-upstream")."""
        return self.status in ("ok", "no-upstream")


def upstream_verdict(repo=".", *, git_cmd="git", fetch=True):
    """
    Whether the branch may be pushed as far as its upstream goes: ahead_behind(fetch=fetch) turned into an
    UpstreamVerdict.  git_cmd is the git invocation shown in the hint.  Never raises: a git failure is "error".
    """
    try:
        ahead, behind, err = ahead_behind(repo, fetch=fetch)
    except GitError as e:
        return UpstreamVerdict("error", reason=f"could not check upstream: {e}")
    if err == "no upstream":
        return UpstreamVerdict("no-upstream", reason="no upstream branch configured")
    if err:
        return UpstreamVerdict("error", reason=f"could not check upstream: {err}")
    if not behind:
        return UpstreamVerdict("ok", ahead, behind)
    commits = f"{behind} commit{'' if behind == 1 else 's'}"
    if ahead:
        return UpstreamVerdict("diverged", ahead, behind, f"{commits} behind upstream",
                               (f"the branch is {commits} behind its upstream and {ahead} ahead of it.",
                                f"  {git_cmd} stash push && {git_cmd} pull --rebase && {git_cmd} stash pop --index"))
    return UpstreamVerdict("behind", ahead, behind, f"{commits} behind upstream",
                           (f"the branch is {commits} behind its upstream.",
                            f"  {git_cmd} merge --ff-only @{{upstream}}"))


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


def _warn(text):
    """A warning: a warn note under a console view, else a line on stderr."""
    jr = _base.jr()
    if jr is not None and jr.console_active():
        jr.say(text, "warn")
    else:
        print(text, file=sys.stderr)


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


def push_command(repo="."):
    """
    The push to the branch's tracked upstream: `git push <remote> HEAD:<merge>` from branch.<b>.remote and
    branch.<b>.merge, or `git push -u origin <branch>` when the branch tracks nothing.  Raises GitError on a detached
    HEAD, on tracking config it can't resolve (only one of the two keys set, or a git failure reading them), on a
    local upstream (remote "."), or on a merge ref outside refs/heads/.  Never falls back to -u origin when an upstream
    is set.
    """
    _require_repo(repo)
    refused = ["git", "push"]
    branch = current_branch(repo)
    if branch is None:
        raise GitError(refused, Result(False, 1, err="detached HEAD: there is no branch to push"))
    keys = {}
    for key in ("remote", "merge"):
        cmd = ["git", "config", "--get", f"branch.{branch}.{key}"]
        result = _run(cmd, cwd=repo)
        if result.code not in (0, 1):    # 1 is "not set"
            raise GitError(cmd, result)
        keys[key] = result.out.strip() if result.ok else None
    remote, merge = keys["remote"], keys["merge"]
    if remote is None and merge is None:
        return ["git", "push", "-u", "origin", branch]
    if not remote or not merge:
        raise GitError(refused, Result(False, 1, err=f"could not resolve the upstream of {branch} "
                                                     f"(branch.{branch}.remote={remote!r}, merge={merge!r})"))
    if remote == ".":
        raise GitError(refused, Result(False, 1, err=f"{branch} tracks a local branch ({merge}); nothing to push"))
    if not merge.startswith("refs/heads/"):
        raise GitError(refused, Result(False, 1, err=f"{branch} tracks {merge}, which is not a branch"))
    return ["git", "push", remote, f"HEAD:{merge}"]


def push_tracked(repo=".", *, capture=False):
    """Run push_command(repo).  A refusal from push_command gives Result(ok=False, code=1, err=<why>) without
    pushing."""
    try:
        cmd = push_command(repo)
    except GitError as e:
        return Result(False, 1, err=str(e))
    return _write(cmd, repo, capture=capture)


def rebase_onto_upstream(repo=".", *, autostash=False, capture=False):
    """`git rebase [--autostash] @{upstream}`.  autostash is off by default: its pop restores the stash without
    --index, so staged changes come back unstaged.  rebase_keeping_index keeps them staged."""
    return _write(["git", "rebase", *(["--autostash"] if autostash else []), "@{upstream}"], repo, capture=capture)


def stash_ref_sha(repo="."):
    """The commit refs/stash points at, or None when there is no stash."""
    return _scalar(["git", "rev-parse", "-q", "--verify", "refs/stash"], repo)


def stash_entry_ref(repo, sha):
    """The `stash@{N}` entry holding commit sha, or None when no stash entry does."""
    listed = _run(["git", "stash", "list", "--format=%H"], cwd=repo)
    for n, entry in enumerate(listed.out.split() if listed.ok else ()):
        if entry == sha:
            return f"stash@{{{n}}}"
    return None


def restore_stash_index(repo, sha):
    """
    Re-apply stash sha's staged changes (its index commit against its base) to the index and the working tree, all or
    nothing, then drop that stash entry.  This is `git stash pop --index` except that a staged deletion the rebased
    HEAD no longer has is left out: the path is gone either way, and pop --index fails on it.  Bytes throughout
    (binary patches, names with a CR) and literal pathspecs, so a path with glob characters only matches itself.

    Returns the failed command's Result, or Result(ok=True); a stash that can't be dropped afterwards only warns (err
    holds the warning).
    """
    def git(*argv, stdin=None):
        return _run(["git", "--literal-pathspecs", *argv], cwd=repo, input=stdin, binary=True)

    def names(out):
        return [os.fsdecode(n) for n in out.split(b"\0") if n]

    base, index = f"{sha}^1", f"{sha}^2"
    changed = git("diff", "--name-only", "--no-renames", "-z", base, index)
    deleted = git("diff", "--name-only", "--no-renames", "--diff-filter=D", "-z", base, index)
    if not (changed.ok and deleted.ok):
        return changed if not changed.ok else deleted
    deleted_paths = set(names(deleted.out))
    in_head = set()
    if deleted_paths:
        listed = git("ls-tree", "-z", "--name-only", "HEAD", "--", *sorted(deleted_paths))
        if not listed.ok:
            return listed
        in_head = set(names(listed.out))
    paths = [p for p in names(changed.out) if p not in deleted_paths or p in in_head]
    if paths:
        patch = git("diff", "--binary", "--no-renames", base, index, "--", *paths)
        if not patch.ok:
            return patch
        applied = git("apply", "--index", "--binary", "-", stdin=patch.out)
        if not applied.ok:
            return applied
    ref = stash_entry_ref(repo, sha)
    if ref:
        dropped = git("stash", "drop", "-q", ref)
        if not dropped.ok:
            warning = (f"the staged changes were restored, but stash {sha} could not be dropped: "
                       f"{_last(dropped, 'git stash drop failed')}")
            _warn(warning)
            return Result(True, 0, err=warning)
    return Result(True, 0)


@dataclass(frozen=True)
class KeepIndex:
    """
    rebase_keeping_index's answer.  status is one of:
      "rebased"           rebased, with any staged changes restored as staged;
      "up-to-date"        nothing to rebase onto;
      "no-upstream"       the branch tracks nothing (ok, nothing done);
      "refused"           unreviewed paths: nothing was stashed or rebased;
      "error"             a detached HEAD, a failed fetch, or a stash that didn't happen; nothing was rebased;
      "rebase-conflict"   the rebase stopped; the reviewed changes are kept in the stash;
      "restore-conflict"  rebased, but the reviewed changes no longer apply; they are kept in the stash.
    stash_sha and stash_ref name the held stash (stash_ref may be None if the entry can't be found).
    """
    ok: bool
    status: str
    stash_sha: str | None = None
    stash_ref: str | None = None
    reason: str = ""
    hint: tuple = ()


def rebase_keeping_index(repo=".", *, fetch=True, merges=False, exempt=()):
    """
    Rebase the branch onto its upstream and bring the staged (reviewed) changes back staged, with the reviewed blobs.
    In order: refuse on unreviewed_paths(repo, exempt=exempt) before anything is stashed; fetch (when fetch); stop at
    "up-to-date" when not behind; stash the index (`git stash push -q -m STASH_MESSAGE`, checked by refs/stash
    moving); `git rebase [--rebase-merges] @{upstream}`; restore_stash_index.  On a conflict the stash is kept and the
    hint names it.  Never uses --autostash, `git stash pop` or `git add`.  Returns a KeepIndex; the caller decides
    whether to fail.  Raises GitError when the readers can't run.
    """
    unreviewed = unreviewed_paths(repo, exempt=exempt)
    if unreviewed:
        reason, hint = unreviewed_refusal(unreviewed, outcome="Nothing was stashed or rebased.")
        return KeepIndex(False, "refused", reason=reason, hint=hint)
    if current_branch(repo) is None:
        return KeepIndex(False, "error", reason="detached HEAD: there is no branch to rebase")
    ahead, behind, err = ahead_behind(repo, fetch=fetch)
    if err == "no upstream":
        return KeepIndex(True, "no-upstream", reason="no upstream branch configured")
    if err:
        return KeepIndex(False, "error", reason=f"could not check upstream: {err}")
    if not behind:
        return KeepIndex(True, "up-to-date")

    sha = None
    if staged_paths(repo):
        before = stash_ref_sha(repo)
        stashed = _write(["git", "stash", "push", "-q", "-m", STASH_MESSAGE], repo, capture=True)
        sha = stash_ref_sha(repo)
        if not stashed.ok or sha is None or sha == before:
            return KeepIndex(False, "error", reason=f"git stash push created no stash: "
                                                    f"{_last(stashed, 'check git status')}")
    ref = stash_entry_ref(repo, sha) if sha else None
    held = (f"Your reviewed changes are kept in stash {ref or sha}.",
            f"  git stash pop --index {ref or sha}   (gets them back staged)") if sha else ()

    rebased = _write(["git", "rebase", *(["--rebase-merges"] if merges else []), "@{upstream}"], repo, capture=True)
    if not rebased.ok:
        return KeepIndex(False, "rebase-conflict", sha, ref, f"rebase stopped: {_last(rebased, 'conflict')}",
                         ("Resolve the conflicts and run `git rebase --continue`, or `git rebase --abort`; then "
                          "restore the stash and retry." if sha else
                          "Resolve the conflicts and run `git rebase --continue`, or `git rebase --abort`, then "
                          "retry.", *held))
    if sha is None:
        return KeepIndex(True, "rebased")
    restored = restore_stash_index(repo, sha)
    if not restored.ok:
        return KeepIndex(False, "restore-conflict", sha, ref,
                         f"the reviewed changes no longer apply: {_last(restored, 'git apply failed')}",
                         ("The rebase succeeded, but the staged changes overlap upstream's. Resolve the overlap so "
                          "the tree is clean, then restore the stash and retry.", *held))
    return KeepIndex(True, "rebased", reason=restored.err)


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
