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
"""Maven per-module surefire seconds, emitted as a `series` event from each class line's Time elapsed (spec 4.2)."""

import tempfile
import unittest
from pathlib import Path

from support import jr
from test_parsers import feed, maven, write_demo_tree


class SeriesTest(unittest.TestCase):
    LINE = "[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.234 s -- in org.x.FooTest"

    def test_class_line_groups_are_unchanged_and_time_has_its_own_pattern(self):
        m = jr.MavenParser.CLASS_LINE.match(self.LINE)
        self.assertEqual(m.group(5), "org.x.FooTest")             # CLASS_LINE is untouched: group 5 is still the class
        self.assertEqual(jr.MavenParser.CLASS_TIME.search(self.LINE).group(1), "1.234")
        self.assertEqual(jr.MavenParser.CLASS_TIME.search("Time elapsed: 1,234.5 s").group(1), "1,234.5")

    def test_sum_per_module_is_emitted_as_series(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_demo_tree(root)
            for name, secs in (("AaTest", "1.234"), ("BbTest", "2.0")):
                d = root / "mod-b" / "target" / "test-classes" / "org" / "x"
                d.mkdir(parents=True, exist_ok=True)
                (d / f"{name}.class").write_bytes(b"")
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "test"], None, str(root))
            text = "".join(
                f"[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: {secs} s -- in org.x.{name}\n"
                for name, secs in (("AaTest", "1.234"), ("BbTest", "2.0")))
            ev = feed(parser, text)
        series = [e for e in ev if e[0] == "series"]
        self.assertEqual(series[-1][2:4], ("surefire_s", "mvn/mod-b"))
        self.assertAlmostEqual(series[-1][4], 1.234 + 2.0)

    def test_module_without_classes_has_no_entry(self):
        _, ev = maven("maven-serial-success.log")
        series = {e[3] for e in ev if e[0] == "series"}
        self.assertIn("mvn/demo-parent", {e[1] for e in ev if e[0] == "sub_start"})      # the pom ran, with no test class
        self.assertEqual(series, {"mvn/mod-a", "mvn/mod-b", "mvn/mod-c"})
        self.assertNotIn("mvn/demo-parent", series)


if __name__ == "__main__":
    unittest.main()
