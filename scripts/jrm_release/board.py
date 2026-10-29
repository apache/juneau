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
"""What release.py declares to the juneau-terminal board: which board features the toolkit has, this run's number and
the pause dividers with their predicted states.  Pure functions, so the tests need no toolkit."""
from . import details, emails, inventory


def board_features(deps):
    """The toolkit's BOARD_FEATURES (an older toolkit has none).  A new keyword, bar value or call is sent only when
    its name is in this set."""
    return frozenset(getattr(deps.run, "BOARD_FEATURES", frozenset()))


def _first_step(args, rf):
    """The number of the first step this run will execute."""
    if args.start_step:
        return inventory.num_of(args.start_step) or 1
    if args.resume:
        last = rf.last()
        return (inventory.num_of(last) or 0) + 1 if last else 1
    return 1


def divider_plan(args, rf):
    """(run number, [(after, text, state)]) for a non-revert run.  The run number is the attempts so far plus one.  A
    divider is passed when its step is before the first one that runs, when the pause will be acknowledged on resume,
    or on a dry run; the first other one is this run's stop and the rest are later (spec 2.2).
    The toolkit numbers the dividers that have not passed in plan order from the run number (spec 9.2), so the states are
    all this function decides."""
    run_n = len(rf.get("attempts") or []) + 1
    first = _first_step(args, rf)
    pending = rf.get("emailPending") if args.resume else None
    out, stop_taken = [], False
    for name, text in sorted(inventory.PAUSE_DIVIDERS.items(), key=lambda kv: inventory.num_of(kv[0])):
        num = inventory.num_of(name)
        acknowledged = pending is not None and emails.STEP_KIND.get(name) == pending
        if args.dry_run or num < first or (num == first and acknowledged):
            state = "passed"
        elif not stop_taken:
            state, stop_taken = "stops", True
        else:
            state = "later"
        out.append((name, text, state))
    return run_n, out


def history_alias(deps):
    """Juneau's test history, for the expected test count of the Maven steps that run tests (spec 2.5.3).  Empty when the
    toolkit cannot name the key.  The key is Juneau's history directory name for the expanded checkout path."""
    key = getattr(deps.run, "repo_key", lambda p: None)(details.checkout())
    if not key:
        return {}
    entry = (key, "test", "tests", "maven")
    return {"build-verify": entry, "deploy-snapshot": entry, "release-perform": entry}
