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

from . import emails

FUNCS = {}


def step(fn):
    FUNCS[fn.__name__.replace("_", "-")] = fn
    return fn


@step
def vote_gate(ctx):
    ctx.rf.set("phase", "vote")
    raise ctx.pause("Vote is open.  When it closes, run: release.py --resume", done=True)


@step
def vote_tally(ctx):
    deadline = ctx.rf.get("voteDeadline")
    if deadline and ctx.now() < datetime.fromisoformat(deadline):
        ok = ctx.prompt.confirm(f"The vote is open until {deadline}.  Tally it anyway?", key="vote.early")
        if ok is not True:
            raise ctx.pause("Vote still open.  Re-run with --resume after it closes.", done=False)
    F = ctx.prompt.Field
    got = ctx.prompt.form("Vote tally", [F("plus1", "Binding +1 votes", required=True, type="int", default="0"),
                                         F("zero", "0 votes", type="int", default="0"),
                                         F("minus1", "-1 votes", type="int", default="0"),
                                         F("binding", "Binding voters", required=True)], key="vote.tally")
    if got is None:
        raise KeyboardInterrupt
    passed = got["plus1"] >= 3 and got["plus1"] > got["minus1"]
    ctx.rf.set("vote", {**got, "outcome": "passed" if passed else "failed"})
    return "vote passed" if passed else "vote failed"


@step
def compose_result_email(ctx):
    t = ctx.rf.get("vote") or {}
    outcome = "passed" if t.get("outcome") == "passed" else "failed"
    tally = (f"Binding +1: {t.get('plus1', 0)} ({t.get('binding', '')})\n0: {t.get('zero', 0)}\n"
             f"-1: {t.get('minus1', 0)}")
    to, sub, body = emails.result(ctx.d, outcome, tally)
    msg = emails.send(ctx, "result", to, sub, body)
    if outcome == "failed":
        ctx.rf.set("phase", "vote-failed")
        raise ctx.pause("The vote failed.  Undo this candidate with: release.py --revert", done=True)
    return msg
