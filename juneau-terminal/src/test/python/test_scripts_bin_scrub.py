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
"""The shipped side-pane scripts and their tests: license header, no personal or tracker text, usage output, mode bits."""

import os
import re
import subprocess
import unittest
from pathlib import Path

from tmux_support import TMUX_PANE_SH, WATCH_RUN_SH

TESTS = Path(__file__).resolve().parent
FILES = [TMUX_PANE_SH, WATCH_RUN_SH, TESTS / "test_scripts_tmux_pane.py",
         TESTS / "test_scripts_watch_run.py", TESTS / "tmux_support.py"]
# Each token is built from two halves so this module never matches itself.
BANNED = [r"\b" + "Ja" + r"mes\b", r"\b" + "Bog" + r"nar\b", r"\b" + "I" + r"RS\b", r"\b" + "Sales" + r"force\b",
          r"\b" + "SF" + r"DC\b", r"\b" + "G" + r"US\b", "/Us" + "ers/", "Project" + " Work", "WORK" + "-"]
TRACKER = r"\b[PJICR]0[0-9]{3}\b"   # as an ordinary regex: it matches ids, not this source line


def usage_of(script):
    return subprocess.run([str(script)], capture_output=True, text=True, env={"PATH": os.environ["PATH"]})


class BinScrubTest(unittest.TestCase):
    def test_license_header_on_the_scripts(self):
        for p in (TMUX_PANE_SH, WATCH_RUN_SH):
            lines = p.read_text().splitlines()
            self.assertEqual(lines[0], "#!/usr/bin/env bash", p.name)
            self.assertIn("Licensed to the Apache Software Foundation", "\n".join(lines[:14]), p.name)

    def test_no_personal_or_tracker_text(self):
        for p in FILES:
            text = p.read_text()
            for pat in BANNED:
                self.assertIsNone(re.search(pat, text), "%s matches %s" % (p.name, pat))
            self.assertIsNone(re.search(TRACKER, text, re.I), p.name)

    @unittest.skipUnless(os.name == "posix", "mode bits are not portable")
    def test_scripts_are_executable(self):
        for p in (TMUX_PANE_SH, WATCH_RUN_SH):
            self.assertTrue(os.access(p, os.X_OK), p.name)

    def test_usage_is_the_description_only(self):
        for p in (TMUX_PANE_SH, WATCH_RUN_SH):
            r = usage_of(p)
            lines = r.stderr.splitlines()
            self.assertEqual(r.returncode, 2, p.name)
            self.assertFalse(lines[0].startswith("# *"), "%s usage starts inside the license block: %r" % (p.name, lines[0]))
            self.assertIn("Usage:", r.stderr, p.name)
            self.assertIn("Requires:", r.stderr, p.name)
            for bad in ("Licensed", "License", "WARRANTIES", "apache.org", "tail_filter", "perl -", "Edit this file"):
                self.assertNotIn(bad, r.stderr, "%s usage has %s" % (p.name, bad))
            # the first usage line is the script's description: the first comment line after the license block
            body = p.read_text().splitlines()
            rules = [i for i, line in enumerate(body) if re.fullmatch(r"# \*{40,}", line)]
            self.assertEqual(lines[0], body[rules[1] + 1], p.name)
        self.assertIn("perl (for the follower filter)", usage_of(WATCH_RUN_SH).stderr)


if __name__ == "__main__":
    unittest.main()
