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
"""
Gate checks as data: a Check names a function that returns a Verdict.  run.checks (and Session.checks,
Registry.checks) runs a list of them as one console row, one cell per check.  This module has no UI.

The factories wrap the git readers.  None of them changes the repository: they only read it (upstream fetches).
"""

from collections.abc import Callable
from dataclasses import dataclass

from . import git

STATUSES = ("pass", "warn", "fail")


@dataclass(frozen=True)
class Verdict:
    """A check's outcome.  status is pass, warn or fail; reason is one line for the row; hint is the lines printed
    beneath the row."""
    status: str
    reason: str = ""
    hint: tuple = ()

    def __post_init__(self):
        if self.status not in STATUSES:
            raise ValueError(f"status must be one of {', '.join(STATUSES)}, not {self.status!r}")
        object.__setattr__(self, "hint", tuple([self.hint] if isinstance(self.hint, str) else self.hint))


PASS = Verdict("pass")


@dataclass(frozen=True)
class Check:
    """name is the row's progress text while the check runs, and heads its hint block.  fn() returns a Verdict, or
    True/None for a pass and False for a fail with no text."""
    name: str
    fn: Callable


def evaluate(check, *, dry_run=False):
    """Run one check and return its Verdict.  A check that raises fails closed with "could not check <name>: <error>".
    With dry_run a fail becomes a warn whose reason starts "would refuse: "."""
    try:
        out = check.fn()
    except Exception as e:  # NOSONAR python:S2221 - a gate that cannot decide refuses
        out = Verdict("fail", f"could not check {check.name}: {e}")
    if out is None or out is True:
        out = PASS
    elif out is False:
        out = Verdict("fail")
    elif not isinstance(out, Verdict):
        out = Verdict("fail", f"could not check {check.name}: it returned {type(out).__name__}, not a Verdict")
    if dry_run and out.status == "fail":
        out = Verdict("warn", "would refuse: " + (out.reason or f"{check.name} failed"), out.hint)
    return out


def summary(verdicts):
    """(level, reason, hint lines) for a run of checks, given {name: Verdict} in check order.  level is "error" when
    any check failed, "warn" when one warned, else None.  The reason is the first such check's reason, plus " +N" for
    the others at that level; the hint lines are every failed or warned check's hint, its first line headed
    "<name>: ", in check order."""
    fails = [(name, v) for name, v in verdicts.items() if v.status == "fail"]
    warns = [(name, v) for name, v in verdicts.items() if v.status == "warn"]
    top = fails or warns
    if not top:
        return None, "", ()
    name, first = top[0]
    reason = (first.reason or f"{name} failed") + (f" +{len(top) - 1}" if len(top) > 1 else "")
    hint = []
    for name, v in verdicts.items():
        if v.status != "pass" and v.hint:
            hint.append(f"{name}: {v.hint[0]}")
            hint.extend(v.hint[1:])
    return ("error" if fails else "warn"), reason, tuple(hint)


def identity(repo=".", *, required=None, label="identity"):
    """Fail when git's user.email is unset or, with required, isn't that address.  The hint is the config command;
    the check never writes git config."""
    def fn():
        email = git.user_email(repo)
        if not email:
            return Verdict("fail", "git user.email is not set",
                           ("set it (this script never writes git config):", f"  git config user.email {required or '<email>'}"))
        if required is not None and email != required:
            return Verdict("fail", f"git user.email is {email}, not {required}",
                           ("set it (this script never writes git config):", f"  git config user.email {required}"))
        return PASS
    return Check(label, fn)


def unreviewed(repo=".", *, exempt=(), git_cmd="git", label="unreviewed",
               outcome="Nothing was committed or pushed."):
    """Fail when any path is unstaged or untracked (git.unreviewed_paths, less the exempt ones).  outcome is the
    sentence the hint uses to say what the refusal held back; git_cmd is how the hint's commands reach the repo."""
    def fn():
        paths = git.unreviewed_paths(repo, exempt=exempt)
        if not paths:
            return PASS
        reason, hint = git.unreviewed_refusal(paths, outcome=outcome, git_cmd=git_cmd)
        return Verdict("fail", reason, hint)
    return Check(label, fn)


def upstream(repo=".", *, git_cmd="git", label="upstream"):
    """Fail when the branch is behind its upstream, or the check can't tell (git.upstream_verdict).  No upstream
    passes."""
    def fn():
        v = git.upstream_verdict(repo, git_cmd=git_cmd)
        return PASS if v.ok else Verdict("fail", v.reason, v.hint)
    return Check(label, fn)


def staged(repo=".", *, label="staged"):
    """Warn "nothing staged" when the index matches HEAD."""
    def fn():
        return PASS if git.staged_paths(repo) else Verdict("warn", "nothing staged")
    return Check(label, fn)
