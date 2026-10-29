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
"""history_alias (spec 2.5.3): the expected test count read from another history, read-only."""

import json
import os
import tempfile
import unittest
from pathlib import Path

from support import jr, reset_run_state


def write(root, key, script, records):
    d = Path(root) / key
    d.mkdir(parents=True, exist_ok=True)
    (d / f"{script}.jsonl").write_text("".join(json.dumps(r) + "\n" for r in records))


def rec(step, tool, tests, wall=192.6):
    return dict(v=1, step=step, tool=tool, ok=True, complete=True, wall_s=wall, tests=tests, units={"kind": "modules", "total": 100},
                sig="", ts="2026-10-28T10:00:00")


class AliasTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        os.environ["JUNEAU_TERMINAL_HISTORY_DIR"] = self.tmp.name
        os.environ.pop("JUNEAU_TERMINAL_HISTORY", None)
        write(self.tmp.name, "juneau-abc", "test", [rec("tests", "maven", 158577)])
        self.alias = {"build-verify": ("juneau-abc", "test", "tests", "maven")}

    def est(self, repo="/nonexistent-release-repo"):
        return jr.make_estimator(repo, "release", history_key="jrm-key", alias=self.alias)

    def test_alias_supplies_only_the_test_count(self):
        e = self.est()("build-verify", "maven", None)
        self.assertEqual(e.tests, 158577)
        self.assertIsNone(e.wall_s)
        self.assertIsNone(e.units)

    def test_own_history_with_tests_wins(self):
        write(self.tmp.name, "jrm-key", "release", [rec("build-verify", "maven", 1000, wall=50.0)])
        e = self.est()("build-verify", "maven", None)
        self.assertEqual((e.tests, e.wall_s), (1000, 50.0))

    def test_own_history_without_tests_borrows_the_count(self):
        write(self.tmp.name, "jrm-key", "release", [rec("build-verify", "maven", None, wall=50.0)])
        e = self.est()("build-verify", "maven", None)
        self.assertEqual((e.tests, e.wall_s), (158577, 50.0))

    def test_no_history_anywhere_is_none_and_unaliased_steps_are_untouched(self):
        self.assertIsNone(self.est()("javadoc-verify", "maven", None))
        os.environ["JUNEAU_TERMINAL_HISTORY_DIR"] = tempfile.mkdtemp()
        self.assertIsNone(self.est()("build-verify", "maven", None))

    def test_alias_never_writes(self):
        before = sorted(Path(self.tmp.name).rglob("*"))
        self.est()("build-verify", "maven", None)
        self.assertEqual(before, sorted(Path(self.tmp.name).rglob("*")))

    def test_a_running_row_with_a_count_only_estimate_renders(self):
        b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0,
                     estimator=lambda s, t, sig=None: jr.Estimate(None, None, None, 1, 158577))
        b.apply("step_start", dict(id="build-verify", n=1, title="x", parent=None, tool="maven", sig=""))
        b.apply("reactor", dict(step="build-verify", size=3, poms=[], tests=True, keep=True))
        text = jr.row_text(b.rows["build-verify"], jr.PLAIN_GLYPHS, 130.0)
        self.assertIsNotNone(text)
