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
"""The collapsed in-flight module line: the shared title prefix and the comma-joined, width-capped name list."""

import os
import unittest

from support import jr, reset_run_state


class PrefixTest(unittest.TestCase):
    def test_longest_shared_words(self):
        self.assertEqual(jr.common_prefix(["Apache Juneau REST Server", "Apache Juneau REST Common"]), "Apache Juneau REST ")

    def test_parent_title_equal_to_prefix_is_shortened(self):
        # the shared words are "Apache Juneau", which equals the parent title, so the loop must shorten it
        self.assertEqual(jr.common_prefix(["Apache Juneau", "Apache Juneau REST Server"]), "Apache ")
        self.assertEqual(jr.common_prefix(["Apache Juneau REST Server", "Apache Juneau"]), "Apache ")

    def test_single_title_no_strip(self):
        self.assertEqual(jr.common_prefix(["Apache Juneau Core"]), "")

    def test_gradle_paths(self):
        self.assertEqual(jr.common_prefix([":app:compileJava", ":app:test"]), ":app:")

    def test_no_common(self):
        self.assertEqual(jr.common_prefix(["Core", "Server"]), "")


class CollapseTest(unittest.TestCase):
    def test_fits(self):
        self.assertEqual(jr.collapse_names(["A", "B", "C"], 80), "A, B, C")

    def test_others_suffix_counted_in_fit(self):
        names = [f"module{i}" for i in range(10)]
        line = jr.collapse_names(names, 40)
        self.assertLessEqual(jr._vlen(line) + 2, 40)            # cols is already columns - 1 (spec 3); the caller adds a 2-column indent
        self.assertRegex(line, r", … and \d+ others$")

    def test_first_name_cut_with_ellipsis(self):
        line = jr.collapse_names(["x" * 80, "y"], 30)
        self.assertLessEqual(jr._vlen(line) + 2, 30)
        self.assertEqual(line.count("…"), 2)                    # one closing the cut name, one in the suffix
        self.assertTrue(line.startswith("x" * 5))
        self.assertIn("…, … and 1 others", line)

    def test_single_name_alone(self):
        self.assertEqual(jr.collapse_names(["Only"], 80), "Only")

    def test_wide_characters_measured_by_display_columns(self):
        line = jr.collapse_names(["日本語" * 4, "b", "c"], 20)
        self.assertLessEqual(jr._vlen(line) + 2, 20)

    def test_width_is_counted_once(self):
        names = [f"m{i}" for i in range(10)]
        joined = ", ".join(names)                                 # 38 columns
        self.assertEqual(jr.collapse_names(names, 40), joined)    # 38 + the 2-column indent fills cols exactly
        self.assertIn("others", jr.collapse_names(names, 39))

    def test_never_exceeds_the_budget_at_tiny_widths(self):
        names = ["x" * 40, "y" * 40, "z" * 40]
        for cols in range(0, 40):
            for chosen in (names, names[:1], ["日本語" * 5, "b"]):
                line = jr.collapse_names(chosen, cols)
                self.assertLessEqual(jr._vlen(line), max(0, cols - 2), (cols, line))


class RegionCollapsedWidthTest(unittest.TestCase):
    def test_collapsed_line_fills_but_never_exceeds_columns_minus_one(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        b.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        for i in range(30):
            b.apply("module_start", dict(step="mvn", module=f"m{i}", title=f"Module number {i:02d}"))
        live = jr.LiveRenderer(lambda t: None, size=lambda: os.terminal_size((60, 24)), clock=lambda: 100.0, color=False)
        size = live.size()
        lines = live.region(b, size.columns - 1, size.lines - 2)
        collapsed = [l for l in lines if l.startswith("    ")][0]
        self.assertLessEqual(jr._vlen(collapsed), 59)
        names = [f"{i:02d}" for i in range(30)]
        # region's cols already is columns - 1: no second -1; the 4-space indent takes two more than the default 2
        self.assertEqual(collapsed, "    " + jr.collapse_names(names, 57))


if __name__ == "__main__":
    unittest.main()
