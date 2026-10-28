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
"""Prerequisites: the Prerequisites board row.  The checks themselves live in preflight.py (the Tools group); this module
adapts their results to the toolkit's Verdict model and to the step."""
from . import preflight
from .ctx import StepError
from .preflight import TOOL_NAMES, TOOLS, java_binary, java_major, parse_version  # noqa: F401 - re-exported


def _verdict(res):
    from juneau_terminal_scripts import checks   # the toolkit's gate-check model; on sys.path once release.py has bootstrapped
    return checks.Verdict("fail", res.reason) if preflight.blocks(res) else checks.PASS


def _board_name(name):
    """The row's cell label: the tool's own name, without the table's 'tool ' prefix."""
    return name.removeprefix("tool ")


def check(ctx):
    """The Tools group's failure strings (empty = all good), as `name: reason`.  Nothing is drawn: the Prerequisites
    row (checks() below) replays the verdicts, so its bar is the one that stays on the screen.  The result is kept on
    ctx.prereq and ctx.prereq_verdicts so nothing runs twice.  A tool's own failure text already starts with its
    name; a can't-check (an unreachable network) is a failure here as it is for the whole preflight."""
    results = [(n, r) for g, n, r in (ctx.preflight_results or preflight.run_checks(ctx, ("tools",))) if g == "tools"]
    ctx.prereq_verdicts = {_board_name(n): _verdict(r) for n, r in results}
    ctx.prereq = [r.reason if n.startswith("tool ") else f"{n}: {r.reason}" for n, r in results
                  if preflight.blocks(r)]
    return ctx.prereq


def checks(ctx):
    """The Prerequisites row's checks, one bar cell each: the early result replayed (run now if release.py has not
    run it)."""
    from juneau_terminal_scripts import checks as ck
    if ctx.prereq_verdicts is None:
        check(ctx)
    return [ck.Check(name, lambda v=v: v) for name, v in ctx.prereq_verdicts.items()]


def step(ctx):
    """The Prerequisites step: the early result when release.py already ran it, else run it now."""
    failed = ctx.prereq if ctx.prereq is not None else check(ctx)
    if failed:
        raise StepError("; ".join(failed))
    return "all prerequisites met"
