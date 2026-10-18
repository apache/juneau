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
"""Named, numbered steps with --list-steps, --start-step, --skip-step and --resume."""

from . import run
from ._base import Result


class Registry:
    def __init__(self):
        self._steps = []    # (name, title, fn), in registration order
        self._labels = {}   # name -> the console row's label, for the steps registered with one
        self._prog = None
        self._current = None    # the open step's handle while a step's fn runs (Registry.checks)

    @property
    def names(self):
        return [name for name, _, _ in self._steps]

    def step(self, name, title, *, label=None):
        """Decorator: register fn(ctx) as the next step.  It fails by raising or by returning a Result that isn't
        ok; a str it returns becomes its row's summary.  label, when set, is passed to run.step for the step's
        console row."""
        if name in self.names:
            raise ValueError(f"step {name!r} is already registered")

        def register(fn):
            self._steps.append((name, title, fn))
            if label:
                self._labels[name] = label
            return fn
        return register

    def checks(self, name, title, checks, *, label=None):
        """Register the next step as a run of gate checks (run.checks semantics, on this step's row): checks is a
        list of checks.Check, or fn(ctx) returning one.  A failed check fails the step, which prints the resume
        hint."""
        def fn(ctx):
            listed = checks(ctx) if callable(checks) else checks
            st = self._current
            result = run.drive_checks(name, st, list(listed))
            return Result(True, 0) if result.ok else Result(False, 1)
        self.step(name, title, label=label)(fn)
        return fn

    def add_arguments(self, parser):
        """--list-steps, --start-step NAME, --skip-step NAME (repeatable) and --resume.  Unknown names are argparse
        usage errors (exit 2); --start-step and --resume are mutually exclusive."""
        self._prog = parser.prog
        names = self.names
        parser.add_argument("--list-steps", action="store_true", help="list the steps and exit")
        group = parser.add_mutually_exclusive_group()
        group.add_argument("--start-step", choices=names, metavar="NAME",
                           help="start at this step, skipping the ones before it")
        group.add_argument("--resume", action="store_true", help="start after the last step the checkpoint recorded")
        parser.add_argument("--skip-step", choices=names, action="append", default=[], metavar="NAME",
                            help="skip this step (repeatable)")

    def _index(self, name):
        if name not in self.names:
            raise ValueError(f"unknown step {name!r}; the steps are {', '.join(self.names)}")
        return self.names.index(name)

    def _hint(self, name):
        """The resume hint, at error level so every detail level (summary included) shows it."""
        run.say(f"To resume: {self._prog or 'the script'} --start-step {name}", "error")

    def _start(self, args, checkpoint):
        """(index, message): the index of the first step to run (None when --resume finds every step done) and a line
        for run() to print, or None.  Pure: plan() calls this too, and must print nothing."""
        if args.start_step:
            return self._index(args.start_step), None
        if not args.resume:
            return 0, None
        if checkpoint is None:
            raise ValueError("--resume needs a checkpoint")
        last = checkpoint.last()
        if last is None:
            return 0, "No checkpoint recorded; starting at the first step."
        start = self._index(last) + 1
        if start >= len(self._steps):
            return None, f"Every step has completed (the last was {last}); nothing to resume."
        return start, f"Resuming after {last}: starting at {self._steps[start][0]}."

    def _selected(self, args, checkpoint):
        """(start, skip, message): the index of the first step to run (None when --resume finds every step done), the set
        of step names to skip and the --resume line for run() to print.  plan() and run() both select through this, so they cannot disagree."""
        skip = set(args.skip_step or ())
        for name in skip:
            self._index(name)
        start, message = self._start(args, checkpoint)
        return start, skip, message

    def plan(self, args, checkpoint=None):
        """The names of the steps run() would execute for these arguments, in order (for session(plan=...))."""
        start, skip, _ = self._selected(args, checkpoint)
        return [] if start is None else [name for name, _, _ in self._steps[start:] if name not in skip]

    def run(self, args, ctx, checkpoint=None, *, show_unselected=False):
        """Run the selected steps in order, each inside run.step, recording each finished (or skipped) one as the
        checkpoint's "last".  A failure prints the resume hint and returns a failed Result.

        show_unselected walks every step, numbered from 1: one before the start is a skip row saying "(done
        earlier)" and a --skip-step one a skip row saying "(skipped)", with no "Skipping" line.  Neither is in
        plan(); a "(done earlier)" row leaves the checkpoint alone."""
        if args.list_steps:
            for n, (name, title, _) in enumerate(self._steps, 1):
                run.say(f"{n:>2}. {name:<24} {title}")
            return Result(True, 0)
        start, skip, message = self._selected(args, checkpoint)
        if message:
            run.say(message)
        if start is None:
            return Result(True, 0)
        first = 0 if show_unselected else start
        for n, (name, title, fn) in enumerate(self._steps[first:], first + 1):
            failed = None
            earlier = n - 1 < start
            with run.step(name, title, n=n, label=self._labels.get(name)) as st:
                if earlier or name in skip:
                    st.skip()
                    if show_unselected:
                        st.summary = "(done earlier)" if earlier else "(skipped)"
                    else:
                        run.say(f"⏭  Skipping {n}. {title}")
                else:
                    self._current = st
                    try:
                        out = fn(ctx)
                    except Exception as e:  # NOSONAR python:S2221 - a step's error is reported, then the hint
                        st.fail()
                        run.say(f"❌ ERROR: {type(e).__name__}: {e}", "error")
                        failed = Result(False, 1, err=str(e))
                    except BaseException:
                        st.fail()
                        self._hint(name)
                        raise
                    else:
                        if isinstance(out, Result) and not out.ok:
                            st.fail(exit=out.code)
                            failed = out
                        elif isinstance(out, str):
                            st.summary = out
            if failed is not None:
                self._hint(name)
                return Result(False, failed.code or 1, err=failed.err)
            if checkpoint is not None and not earlier:
                checkpoint.set("last", name)
        return Result(True, 0)
