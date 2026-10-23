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
"""The in-flight module lines of a pane too short for them all: the last ones shown whole, never packed onto one line."""

import os
import unittest

from support import jr, reset_run_state


class RegionOverflowTest(unittest.TestCase):
    def test_the_last_module_lines_show_whole_and_never_exceed_columns_minus_one(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        b.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        for i in range(30):
            b.apply("unit_start", dict(step="mvn", id=f"m{i}", title=f"Module number {i:02d}"))
        live = jr.LiveRenderer(lambda t: None, size=lambda: os.terminal_size((60, 24)), clock=lambda: 100.0, color=False)
        size = live.size()
        lines = live.region(b, size.columns - 1, size.lines - 2)
        # region's cols already is columns - 1: the row and the 8 oldest module lines scrolled out of view
        self.assertEqual([line.split()[2] for line in lines], [f"{i:02d}" for i in range(8, 30)])
        self.assertTrue(all(jr._vlen(line) == 59 for line in lines), lines)


if __name__ == "__main__":
    unittest.main()
