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
"""Row adoption on the board: reactor bar mode keeps a tests-mode module's cell even when it ran no tests."""
import unittest

from support import jr, reset_run_state


def board():
    reset_run_state()
    return jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)


class ReactorKeepTest(unittest.TestCase):
    def setUp(self):
        self.addCleanup(reset_run_state)

    def test_a_module_with_no_tests_keeps_its_cell_in_reactor_mode(self):
        b = board()
        b.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        b.apply("reactor", dict(step="mvn", size=3, poms=[], tests=True, keep=True))
        for mod in ("a", "b", "c"):
            b.apply("unit_start", dict(step="mvn", id=mod, title=mod))
            b.apply("unit_end", dict(step="mvn", id=mod, status="ok", ms=1))
        r = b.rows["mvn"]
        self.assertEqual((r.units_done, r.units_total), (3, 3))

    def test_default_mode_still_drops_a_module_with_no_tests(self):
        b = board()
        b.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        b.apply("reactor", dict(step="mvn", size=3, poms=[], tests=True))     # no keep: an older publisher
        for mod in ("a", "b", "c"):
            b.apply("unit_start", dict(step="mvn", id=mod, title=mod))
            b.apply("unit_end", dict(step="mvn", id=mod, status="ok", ms=1))
        self.assertLess(b.rows["mvn"].units_total, 3)


if __name__ == "__main__":
    unittest.main()
