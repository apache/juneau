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

"""Git: a progress bar for `git clone`, `fetch` and `pull` run with --progress (or under a terminal).

Git writes its progress to stderr, one carriage-return-terminated piece per update ("Receiving objects:  45% (1234/2742),
10.2 MiB | 5.1 MiB/s"), so the parser sets cr_lines and the runner hands it every piece as it arrives.  One bar of CELLS
cells covers the transfer: Receiving (or Unpacking) objects is 0-70 %, Resolving deltas 70-85 % and Updating files
(Checking out files) 85-100 %.  The bar only moves forward, and the last cell fills when git exits 0.  The "remote:"
phases (Enumerating, Counting, Compressing) run on the server before the transfer: they show as the row's text but
leave the bar alone.  Everything else is kept for the failure tail, as the generic parser does, so a git command that
prints no progress behaves exactly as it did."""

import re

from . import _side

CELLS = 20
# phase -> (low, high) share of the bar, in percent
RANGES = {"Receiving objects": (0, 70), "Unpacking objects": (0, 70), "Resolving deltas": (70, 85),
          "Updating files": (85, 100), "Checking out files": (85, 100)}
PHASES = ("Enumerating objects", "Counting objects", "Compressing objects", "Writing objects", "Filtering content",
          *RANGES)
PROGRESS = re.compile(r"^(remote: )?(" + "|".join(PHASES) + r"): +(\d+)% \((\d+)/(\d+)\)(?:, (.*))?$")


def parse(line):
    """(percent of the phase, phase, remote?, text) for a git progress piece, else None.  text is the row text."""
    m = PROGRESS.match(re.sub(r", done\.\s*$", "", line.strip()))
    if not m:
        return None
    remote, phase, pct, num, total, rate = m.groups()
    text = f"{'remote: ' if remote else ''}{phase} {pct}% ({num}/{total})" + (f" · {rate}" if rate else "")
    return int(pct), phase, bool(remote), text


def overall(pct, phase):
    """The share of the whole bar, in percent, once phase is pct done."""
    low, high = RANGES[phase]
    return low + (high - low) * pct / 100


class GitParser(_side.GenericParser):
    """One bar of CELLS units over the transfer, the current phase as the row's text, and the generic failure tail."""

    name = "git"
    cr_lines = True

    def __init__(self):
        super().__init__()
        self.cells = 0         # cells filled so far
        self.started = False
        self.text = None

    def handle(self, line):
        parsed = parse(line)
        if parsed is None:
            return super().handle(line)
        pct, phase, remote, text = parsed
        events = []
        if text != self.text:
            self.text = text
            events.append(("progress", self.step_id, text))
        if not remote and phase in RANGES:
            filled = int(overall(pct, phase) * CELLS / 100)
            if not self.started:
                self.started = True
                events.insert(0, ("units", self.step_id, CELLS, "progress"))
            if filled > self.cells:
                self.cells = filled
                events.append(("unit", self.step_id, filled, None))
        return events

    def finish(self, exit_code):
        events = []
        if exit_code == 0 and self.started and self.cells < CELLS:
            self.cells = CELLS
            events.append(("unit", self.step_id, CELLS, None))
        return events + super().finish(exit_code)


PARSER = GitParser
