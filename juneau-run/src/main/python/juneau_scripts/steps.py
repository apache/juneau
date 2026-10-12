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
        self._prog = None

    @property
    def names(self):
        return [name for name, _, _ in self._steps]

    def step(self, name, title):
        """Decorator: register fn(ctx) as the next step.  It fails by raising or by returning a Result that isn't
        ok."""
        if name in self.names:
            raise ValueError(f"step {name!r} is already registered")

        def register(fn):
            self._steps.append((name, title, fn))
            return fn
        return register

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
        run.say(f"To resume: {self._prog or 'the script'} --start-step {name}")

    def _start(self, args, checkpoint):
        """The index of the first step to run, or None when --resume finds every step done."""
        if args.start_step:
            return self._index(args.start_step)
        if not args.resume:
            return 0
        if checkpoint is None:
            raise ValueError("--resume needs a checkpoint")
        last = checkpoint.last()
        if last is None:
            run.say("No checkpoint recorded; starting at the first step.")
            return 0
        start = self._index(last) + 1
        if start >= len(self._steps):
            run.say(f"Every step has completed (the last was {last}); nothing to resume.")
            return None
        run.say(f"Resuming after {last}: starting at {self._steps[start][0]}.")
        return start

    def run(self, args, ctx, checkpoint=None):
        """Run the selected steps in order, each inside run.step, recording each finished (or skipped) one as the
        checkpoint's "last".  A failure prints the resume hint and returns a failed Result."""
        if args.list_steps:
            for n, (name, title, _) in enumerate(self._steps, 1):
                run.say(f"{n:>2}. {name:<24} {title}")
            return Result(True, 0)
        skip = set(args.skip_step or ())
        for name in skip:
            self._index(name)
        start = self._start(args, checkpoint)
        if start is None:
            return Result(True, 0)
        for n, (name, title, fn) in enumerate(self._steps[start:], start + 1):
            failed = None
            with run.step(name, title, n=n) as st:
                if name in skip:
                    st.skip()
                    run.say(f"⏭  Skipping {n}. {title}")
                else:
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
            if failed is not None:
                self._hint(name)
                return Result(False, failed.code or 1, err=failed.err)
            if checkpoint is not None:
                checkpoint.set("last", name)
        return Result(True, 0)
