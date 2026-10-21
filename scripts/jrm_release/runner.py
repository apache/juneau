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
"""The one place a command runs.  Under --dry-run a mutating command is printed, never executed."""
import os
import subprocess
from collections import namedtuple

Outcome = namedtuple("Outcome", "ok code out")
MASK = "********"


def missing_dir(argv, cwd):
    """The directory a command would run in (cwd, or git's -C target) when it does not exist, else None."""
    if cwd is not None and not os.path.isdir(cwd):
        return str(cwd)
    if "-C" in argv:
        i = argv.index("-C")
        if i + 1 < len(argv) and not os.path.isdir(argv[i + 1]):
            return argv[i + 1]
    return None


class Runner:
    def __init__(self, run_mod, dry_run=False, secrets=lambda: ()):
        self.run_mod, self.dry_run, self.secrets = run_mod, dry_run, secrets
        self.skipped = []   # what a dry run declined to run, in order; steps report their slice of it

    def mask(self, text):
        for s in self.secrets():
            text = text.replace(s, MASK)
        return text

    def gate(self, describe, mutating=True):
        """True when the caller should really do it.  Under --dry-run a mutation is recorded and printed and returns False.
        (Not at warn: the condensed console would then show the note as the row's reason in place of its result;
        the step reports the skipped commands in its result instead, see skipped_note.)"""
        if self.dry_run and mutating:
            self.skipped.append(self.mask(describe))
            self.run_mod.say(self.mask(f"would run: {describe}"))
            return False
        return True

    def skipped_note(self, mark, done, limit=2):
        """A step's result: `done`, or under --dry-run, when the step's mutating commands were skipped since `mark`
        (a len(skipped) taken before the step), what was skipped instead; `done` would claim work that never ran."""
        new = self.skipped[mark:]
        if not new:
            return done
        shown = "; ".join(f"would run {c}" for c in new[:limit]) + (f"; +{len(new) - limit} more" if len(new) > limit else "")
        return f"skipped (dry run): {shown}"

    def _no_dir(self, argv, cwd):
        """Under --dry-run a read-only command aimed at a directory an earlier skipped step would have created (the
        workspace) is not run: it would only fail.  Printed, but not counted as a step's skipped mutation."""
        missing = self.dry_run and missing_dir(argv, cwd)
        if missing:
            self.run_mod.say(self.mask(f"would run (dry run created no {missing}): {' '.join(argv)}"))
        return bool(missing)

    def run(self, cmd, *, title, mutating, tool="generic", cwd=None, env=None, input=None, tty=False, capture=None,
            merge_err=False):
        """Run argv.  capture defaults to read-only commands (their stdout is the answer); pass capture=True for a
        mutating command whose output the caller needs.  merge_err appends stderr to the answer, for tools like
        `java -version` that print there (the toolkit's plain mode keeps the two apart)."""
        argv = [str(c) for c in cmd]
        if not self.gate(" ".join(argv), mutating) or self._no_dir(argv, cwd):
            return Outcome(True, 0, "")
        child_env = getattr(self.run_mod, "child_env", None)
        if child_env is not None:   # the toolkit's: this process's environment minus juneau-run's own variables
            env = {**child_env(), **(env or {})}
        r = self.run_mod.cmd(argv, title=title, tool=tool, cwd=cwd, env=env, input=input, tty=tty,
                             capture=(not mutating) if capture is None else capture)
        out = getattr(r, "out", "") or ""
        if merge_err:
            out += getattr(r, "err", "") or ""
        return Outcome(r.ok, r.code, out)

    def quiet(self, cmd, *, title, cwd=None, merge_err=False):
        """Run a read-only argv whose output the caller wants but the log does not (a ~1,000-line pom).  Never
        echoed, so it is not a row and never reaches full.log; honors the same dry-run rule (reads always run).
        merge_err appends stderr to the answer, as run() does."""
        argv = [str(c) for c in cmd]
        if self._no_dir(argv, cwd):
            return Outcome(True, 0, "")
        hook = getattr(self.run_mod, "quiet_cmd", None)
        if hook is not None:   # tests' fake
            r = hook(argv, title=title, cwd=cwd)
            return Outcome(r.ok, r.code, (getattr(r, "out", "") or "") + (getattr(r, "err", "") or "" if merge_err else ""))
        child_env = getattr(self.run_mod, "child_env", None)
        try:
            r = subprocess.run(argv, cwd=cwd, env=child_env() if child_env else None, capture_output=True, text=True,
                               check=False)
        except OSError as e:
            return Outcome(False, 127, str(e))
        return Outcome(r.returncode == 0, r.returncode, r.stdout + (r.stderr if merge_err else ""))
