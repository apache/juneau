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
"""Vote phase: the gate that pauses the run, the tally, and the RESULT email."""
from datetime import datetime
from pathlib import Path

from . import emails, params

FUNCS = {}


def step(fn):
    FUNCS[fn.__name__.replace("_", "-")] = fn
    return fn


def params_path_for(data, home, version):
    """The live params file the tally is read from: the path recorded in a run.json dict, else <home>/params/<version>.yml."""
    recorded = data.get("paramsPath")
    return Path(recorded) if isinstance(recorded, str) and recorded else Path(home) / "params" / f"{version}.yml"


def params_path(ctx):
    return params_path_for({"paramsPath": ctx.rf.get("paramsPath")}, ctx.home, ctx.version)


@step
def vote_gate(ctx):
    ctx.rf.set("phase", "vote")
    raise ctx.pause(f"The vote is open.  When it closes, run: {ctx.resume_hint}", done=True)


@step
def vote_tally(ctx):
    deadline = ctx.rf.get("voteDeadline")
    try:
        open_ = bool(deadline) and ctx.now() < datetime.fromisoformat(deadline)
    except (TypeError, ValueError):     # naive or unparseable value in an old run.json: do not crash the step
        raise ctx.pause(f"voteDeadline {deadline!r} in {ctx.rf.path} is not a valid ISO timestamp; fix or remove it, then run: "
                        f"{ctx.resume_hint}", done=False)
    if open_:
        raise ctx.pause(f"The vote is open until {deadline}.  After it closes, run: {ctx.resume_hint}", done=False)
    path = params_path(ctx)
    got, problems = params.load_vote(path)
    if problems:
        raise ctx.pause(f"Fill in the vote: block of {path} ({'; '.join(problems)}), then run: {ctx.resume_hint}",
                        done=False)
    passed = got["plus1"] >= 3 and got["plus1"] > got["minus1"]
    ctx.say(f"Tally from {path}: +1 {got['plus1']}, 0 {got['zero']}, -1 {got['minus1']}; binding: {', '.join(got['binding'])}")
    if ctx.dry_run:
        return "skipped (dry run): vote not recorded"
    ctx.rf.set("vote", {**got, "outcome": "passed" if passed else "failed"})
    return "vote passed" if passed else "vote failed"


@step
def compose_result_email(ctx):
    t = ctx.rf.get("vote") or {}
    outcome = "passed" if t.get("outcome") == "passed" else "failed"
    names = t.get("binding")
    names = ", ".join(names) if isinstance(names, list) else (names or "")
    tally = f"Binding +1: {t.get('plus1', 0)} ({names})\n0: {t.get('zero', 0)}\n-1: {t.get('minus1', 0)}"
    to, sub, body = emails.result(ctx.d, outcome, tally)
    msg = emails.send(ctx, "result", to, sub, body)
    if outcome == "failed":
        ctx.rf.set("phase", "vote-failed")
        raise ctx.pause("The vote failed.  Undo this candidate with: release.py --revert", done=True)
    return msg
