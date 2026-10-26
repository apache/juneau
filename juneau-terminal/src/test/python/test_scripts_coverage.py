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
"""juneau_terminal_scripts.coverage: HTT detection, HTT-adjusted per-file stats, the by-module and detail reports, and the
command line, against a small JaCoCo XML fixture and a throwaway source tree."""

import io
import tempfile
import unittest
from unittest import mock
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path

import scripts_support  # noqa: F401  (puts src/main/python on sys.path)
from juneau_terminal_scripts import coverage

XML = """<report name="x">
 <package name="com/acme/app/rest">
  <sourcefile name="A.java">
   <line nr="3" mi="2" ci="0" mb="2" cb="0"/>
   <line nr="4" mi="0" ci="3" mb="0" cb="2"/>
   <line nr="5" mi="1" ci="0" mb="0" cb="0"/>
   <counter type="INSTRUCTION" missed="3" covered="3"/>
   <counter type="BRANCH" missed="2" covered="2"/>
  </sourcefile>
 </package>
 <package name="com/acme/app">
  <sourcefile name="Root.java">
   <line nr="1" mi="0" ci="4" mb="0" cb="0"/>
   <counter type="INSTRUCTION" missed="0" covered="4"/>
   <counter type="BRANCH" missed="0" covered="0"/>
  </sourcefile>
 </package>
</report>
"""

SRC_A = "package x;\nclass A {\n int a; // HTT: unreachable here\n int b;\n int c;\n}\n"


class CoverageCase(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        pkg = self.root / "src" / "main" / "java" / "com" / "acme" / "app" / "rest"
        pkg.mkdir(parents=True)
        (pkg / "A.java").write_text(SRC_A, encoding="utf-8")
        self.xml = self.root / "report.xml"
        self.xml.write_text(XML, encoding="utf-8")
        self.cfg = coverage.Config(repo_root=self.root, base_pkg="com/acme/app", report_name="Acme coverage",
                                   title="Acme", scope="Acme is one module.")

    def out(self, fn, *a):
        buf = io.StringIO()
        with redirect_stdout(buf):
            fn(*a)
        return buf.getvalue()

    def test_htt_marker_reason(self):
        self.assertEqual(coverage.htt_marker_reason("x; // HTT: nope"), "nope")
        self.assertEqual(coverage.htt_marker_reason("x; /* HTT */"), "(no reason given)")
        self.assertIsNone(coverage.htt_marker_reason("x; // HTTP is fine"))
        self.assertIsNone(coverage.htt_marker_reason('s = "HTT";'))

    def test_top_package_depth(self):
        self.assertEqual(coverage.top_package(self.cfg, "com/acme/app"), "(root)")
        self.assertEqual(coverage.top_package(self.cfg, "com/acme/app/rest/sub"), "rest")
        self.assertEqual(coverage.top_package(self.cfg, "other/pkg"), "other/pkg")
        deep = coverage.Config(**{**self.cfg.__dict__, "depth": 2})
        self.assertEqual(coverage.top_package(deep, "com/acme/app/rest/sub/x"), "rest/sub")

    def test_file_stats_drops_htt_line(self):
        import xml.etree.ElementTree as ET
        sf = ET.fromstring(XML).find("package/sourcefile")
        mb, cb, mi, ci, unc, exc = coverage.file_stats(self.cfg, sf, "com/acme/app/rest", "A.java")
        self.assertEqual((mb, cb, mi, ci), (0, 2, 1, 3))
        self.assertEqual(exc, [(3, "unreachable here")])
        self.assertEqual([u[0] for u in unc], [5])

    def test_by_module_report(self):
        text = self.out(coverage.by_module_report, self.cfg, self.xml)
        self.assertIn("(root)", text)
        self.assertIn("rest", text)
        self.assertIn("TOTAL", text)
        self.assertLess(text.index("rest"), text.index("(root)"))  # lowest branch % first

    def test_detail_report_by_package_and_path(self):
        text = self.out(coverage.detail_report, self.cfg, self.xml, "rest", False)
        self.assertIn("com.acme.app.rest.A", text)
        self.assertIn("Excluded 1 hard-to-test line(s)", text)
        self.assertIn("line    5:  1 instructions missed", text)
        path = "src/main/java/com/acme/app/rest/A.java"
        self.assertIn("com.acme.app.rest.A", self.out(coverage.detail_report, self.cfg, self.xml, path, True))

    def test_detail_report_unknown_target_dies(self):
        with redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            coverage.detail_report(self.cfg, self.xml, "nope", False)

    def test_parse_args(self):
        o = coverage.parse_args(self.cfg, ["-r", "-b", "--by-module", "rest"])
        self.assertEqual((o.do_run, o.branches_only, o.by_module, o.target), (True, True, True, "rest"))
        with redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            coverage.parse_args(self.cfg, ["--depth", "2"])
        with redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            coverage.parse_args(self.cfg, ["--bogus"])

    def test_depth_and_modes_are_opt_in(self):
        cfg = coverage.Config(**{**self.cfg.__dict__, "depth_option": True, "modes": {"--py": lambda _c: None}})
        o = coverage.parse_args(cfg, ["--depth", "2", "--py"])
        self.assertEqual((o.depth, o.mode), (2, "--py"))

    def test_main_mode_hook_gets_parsed_depth_and_skips_jacoco(self):
        seen = []
        cfg = coverage.Config(**{**self.cfg.__dict__, "depth_option": True, "modes": {"--py": seen.append}})
        with mock.patch.object(coverage, "generate_report") as gen, mock.patch.object(coverage, "run_tests") as run, \
                mock.patch.object(coverage.subprocess, "run") as sub:
            coverage.main(cfg, ["--depth", "3", "--py"])
        self.assertEqual([c.depth for c in seen], [3])
        self.assertEqual(cfg.depth, 1)  # the caller's Config is not mutated
        gen.assert_not_called()
        run.assert_not_called()
        sub.assert_not_called()

    def test_print_uncovered(self):
        self.assertIn("All lines covered!", self.out(coverage.print_uncovered, []))
        text = self.out(coverage.print_uncovered, [(7, 1, 2, 3)])
        self.assertIn("line    7:  1/2 branches missed, 3 instructions missed", text)
        self.assertNotIn("All lines covered!", text)

    def test_by_module_report_with_no_data(self):
        empty = self.root / "empty.xml"
        empty.write_text('<report name="x"/>', encoding="utf-8")
        self.assertEqual(self.out(coverage.by_module_report, self.cfg, empty), "No JaCoCo data found.\n")

    def test_help_text_mentions_config(self):
        cfg = coverage.Config(**{**self.cfg.__dict__, "mvn_args": ("-Dx=1",), "package_example": "rest"})
        text = coverage.help_text(cfg)
        self.assertIn("Acme", text)
        self.assertIn("mvn test -Dx=1", text)
        self.assertIn("(e.g. rest)", text)

    def test_main_help_and_no_target(self):
        self.assertIn("Usage:", self.out(coverage.main, self.cfg, ["--help"]))
        with redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            coverage.main(self.cfg, ["--by-module"])  # no jacoco.exec in the temp repo


if __name__ == "__main__":
    unittest.main()
