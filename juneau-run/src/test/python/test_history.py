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
"""The history store: repo key, JSONL append/read, compaction, failure tolerance."""

import os
import subprocess
import sys
import shutil
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import support
from support import jr


class HistoryEnv:
    """Mixin: history on, pointed at a temp dir, and a throwaway git repo."""

    def setUp(self):
        support.isolate_run_env(self)
        self.dir = self._mk("jr-hist-")
        os.environ["JUNEAU_RUN_HISTORY_DIR"] = self.dir
        os.environ["JUNEAU_RUN_HISTORY"] = "1"
        self.repo = self._git_repo()

    def _mk(self, prefix):
        d = tempfile.mkdtemp(prefix=prefix)
        self.addCleanup(shutil.rmtree, d, ignore_errors=True)
        return d

    def _git_repo(self):
        d = self._mk("jr-repo-")
        subprocess.run(["git", "init", "-q", d], check=True)
        subprocess.run(["git", "-C", d, "-c", "user.email=a@b", "-c", "user.name=n", "commit", "-q",
                        "--allow-empty", "-m", "x"], check=True)
        return d


class StoreTest(HistoryEnv, unittest.TestCase):

    def test_worktrees_share_a_key_and_same_named_repos_do_not(self):
        wt = self._mk("jr-wt-") + "/w"
        subprocess.run(["git", "-C", self.repo, "worktree", "add", "-q", wt], check=True)
        self.assertEqual(jr.repo_key(self.repo), jr.repo_key(wt))
        other = self._git_repo()
        self.assertNotEqual(jr.repo_key(self.repo), jr.repo_key(other))
        self.assertRegex(jr.repo_key(self.repo), r"^[\w.-]+-[0-9a-f]{8}$")

    def test_non_git_directory_falls_back(self):
        d = self._mk("jr-nogit-")
        self.assertRegex(jr.repo_key(d), r"-[0-9a-f]{8}$")

    def test_script_name_is_sanitized(self):
        with mock.patch.object(sys, "argv", ["python3 -m unittest"]):
            self.assertEqual(jr.script_name(), "python3-m-unittest")

    def test_round_trip_and_bad_lines_skipped(self):
        h = jr.HistoryStore(self.repo, "push")
        h.append({"v": 1, "step": "s", "tool": "t", "ok": True, "complete": True, "wall_s": 1.0})
        with open(h.path, "ab") as f:
            f.write(b"\xff\xfe junk\n[1,2]\n" + b'{"v":2,"step":"s","tool":"t"}\n' + b'{"v":1,"step":"s","tool"')   # torn tail
        h.append({"v": 1, "step": "s", "tool": "t", "ok": True, "complete": True, "wall_s": 2.0})
        recs = h.read()
        self.assertEqual([r["wall_s"] for r in recs], [1.0, 2.0])

    def test_torn_tail_does_not_glue(self):
        h = jr.HistoryStore(self.repo, "push")
        h.append({"v": 1, "step": "s", "tool": "t", "wall_s": 1})
        with open(h.path, "ab") as f:
            f.write(b'{"v":1,"step":"s"')
        h.append({"v": 1, "step": "s", "tool": "t", "wall_s": 3})
        self.assertEqual(h.read()[-1]["wall_s"], 3)

    def test_concurrent_appends_none_lost(self):
        code = ("import sys; sys.path.insert(0, %r); import support; jr = support.jr\n"
                "import os\nos.environ['JUNEAU_RUN_HISTORY_DIR']=%r; os.environ['JUNEAU_RUN_HISTORY']='1'\n"
                "h = jr.HistoryStore(%r, 'push'); h.LOCK_WAIT = 60\n"   # compactions can hold the lock past the 2s default
                "for i in range(50): h.append({'v':1,'step':'s'+sys.argv[1],'tool':'t','wall_s':i,'pad':'x'*3000})\n"
                % (str(support.FIXTURES.parent), self.dir, self.repo))
        procs = [subprocess.Popen([sys.executable, "-c", code, str(i)]) for i in range(8)]
        for p in procs:
            self.assertEqual(p.wait(), 0)
        self.assertEqual(len(jr.HistoryStore(self.repo, "push").read()), 400)

    def test_compaction_keeps_newest_50_per_key(self):
        h = jr.HistoryStore(self.repo, "push")
        h.COMPACT_BYTES = 4096
        for i in range(200):
            h.append({"v": 1, "step": "a", "tool": "t", "wall_s": i, "pad": "x" * 100})
        h.append({"v": 1, "step": "b", "tool": "t", "wall_s": -1, "pad": "x" * 100})
        recs = h.read()
        self.assertEqual([r["wall_s"] for r in recs if r["step"] == "a"][-1], 199)
        self.assertLessEqual(len([r for r in recs if r["step"] == "a"]), jr.HISTORY_KEEP + h.KEEP_SLACK)
        self.assertEqual(len([r for r in recs if r["step"] == "b"]), 1)

    def test_disabled_writes_nothing(self):
        os.environ["JUNEAU_RUN_HISTORY"] = "0"
        h = jr.HistoryStore(self.repo, "push")
        h.append({"v": 1, "step": "s", "tool": "t", "wall_s": 1})
        self.assertFalse(Path(self.dir, jr.repo_key(self.repo)).exists())
        self.assertEqual(h.read(), [])

    def test_unwritable_directory_is_silent(self):
        os.environ["JUNEAU_RUN_HISTORY_DIR"] = "/proc/nope" if sys.platform != "darwin" else "/dev/null/nope"
        jr.HistoryStore(self.repo, "push").append({"v": 1, "step": "s", "tool": "t", "wall_s": 1})   # must not raise

    def test_oversized_record_drops_subs_then_skips(self):
        h = jr.HistoryStore(self.repo, "push")
        h.append({"v": 1, "step": "s", "tool": "t", "wall_s": 1, "subs": {f"m{i}": 1.0 for i in range(10000)}})
        self.assertNotIn("subs", h.read()[-1])
        h.append({"v": 1, "step": "s", "tool": "t", "wall_s": 1, "title": "x" * 80000})
        self.assertEqual(len(h.read()), 1)

    @staticmethod
    def _juneau_tests_record(n):
        """A tests-step record shaped like a full Juneau reactor's: n subs and n entries in each series, keyed by
        module paths of about 40 characters."""
        keys = [f"juneau-microservice/juneau-module-{i:04d}-x" for i in range(n)]
        return {"v": 1, "ts": "2026-10-14T00:00:00Z", "step": "tests", "tool": "maven", "sig": "s1",
                "commit": "abc1234", "ok": True, "complete": True, "wall_s": 900.0, "tests": 30000,
                "subs": {k: 12.345 for k in keys},
                "series": {"surefire_s": {k: 4.5 for k in keys}, "surefire_s.core": {k: 1.25 for k in keys},
                           "reactor_s": {k: 10.125 for k in keys}}}

    def test_a_juneau_sized_tests_record_keeps_its_series(self):
        h = jr.HistoryStore(self.repo, "test")
        rec = self._juneau_tests_record(100)
        h.append(rec)
        got = h.read()[-1]
        self.assertEqual(got["series"], rec["series"])
        self.assertEqual(len(got["subs"]), 100)

    def test_a_20kb_record_round_trips_through_append_and_read(self):
        import json
        h = jr.HistoryStore(self.repo, "test")
        rec = self._juneau_tests_record(100)
        size = len(json.dumps(rec, separators=(",", ":")))
        self.assertTrue(16 * 1024 < size < 32 * 1024, size)   # would have lost subs and series at 16 KiB
        h.append(rec)
        self.assertEqual(h.read(), [rec])


class StoreHardeningTest(HistoryEnv, unittest.TestCase):

    def test_compaction_does_not_rewrite_on_every_append(self):
        h = jr.HistoryStore(self.repo, "push")
        h.COMPACT_BYTES = 2000
        for i in range(20):      # 20 distinct keys: compaction cannot shrink below the limit
            h.append({"v": 1, "step": f"s{i}", "tool": "t", "wall_s": i, "pad": "x" * 300})
        with mock.patch.object(jr.HistoryStore, "_compact", autospec=True, side_effect=jr.HistoryStore._compact) as c:
            for i in range(20, 30):
                h.append({"v": 1, "step": f"s{i}", "tool": "t", "wall_s": i, "pad": "x" * 300})
            self.assertLessEqual(c.call_count, 2)

    def test_malformed_records_are_dropped_on_read(self):
        h = jr.HistoryStore(self.repo, "push")
        h.append({"v": 1, "step": "s", "tool": "t", "wall_s": 1})
        with open(h.path, "ab") as f:
            for bad in ('{"v":true,"step":"s","tool":"t"}', '{"v":1,"step":5,"tool":"t"}', '{"v":1,"step":"s","tool":[1]}',
                        '{"v":1,"step":"s","tool":"t","units":3}', '{"v":1,"step":"s","tool":"t","wall_s":"x"}',
                        '{"v":1,"step":"s","tool":"t","sig":4}', '{"v":1,"step":"s","tool":"t","ok":"yes"}'):
                f.write(bad.encode() + b"\n")
        self.assertEqual(len(h.read()), 1)

    def test_disabled_store_runs_no_git(self):
        os.environ["JUNEAU_RUN_HISTORY"] = "0"
        with mock.patch.object(jr.subprocess, "run", side_effect=AssertionError("git ran")):
            h = jr.HistoryStore(self.repo, "push")
            h.append({"v": 1, "step": "s", "tool": "t", "wall_s": 1})
            self.assertEqual(h.read(), [])

    def test_a_held_lock_skips_the_write_after_a_bounded_wait(self):
        import fcntl, time
        h = jr.HistoryStore(self.repo, "push")
        h.append({"v": 1, "step": "s", "tool": "t", "wall_s": 1})
        with open(str(h.path) + ".lock", "a") as lock:
            fcntl.flock(lock, fcntl.LOCK_EX)
            t = time.monotonic()
            h.append({"v": 1, "step": "s", "tool": "t", "wall_s": 2})
            self.assertLess(time.monotonic() - t, 5)
        self.assertEqual([r["wall_s"] for r in h.read()], [1])

    def test_git_has_a_timeout(self):
        with mock.patch.object(jr.subprocess, "run") as run:
            run.return_value.stdout = "abc\n"
            jr._git("rev-parse", "HEAD")
            self.assertEqual(run.call_args.kwargs.get("timeout"), 5)


class RepoArgumentTest(HistoryEnv, unittest.TestCase):
    def test_history_commit_is_the_repos_head_not_the_cwds(self):
        head = subprocess.run(["git", "-C", self.repo, "rev-parse", "--short", "HEAD"], capture_output=True,
                              text=True, check=True).stdout.strip()
        h = jr.History(jr.HistoryStore(self.repo, "push"))
        h.on_event("step_start", {"id": "a", "title": "A"})
        self.assertEqual(h.rows["a"]["commit"], head)

    def test_console_estimator_reads_the_given_repo(self):
        with mock.patch.object(jr, "make_estimator") as me:
            jr.Console(jr.Sinks(console="none"), repo=self.repo).close()
        self.assertEqual(me.call_args.args[0], self.repo)


class FakeClock:
    def __init__(self):
        self.t = 100.0

    def __call__(self):
        return self.t

    def advance(self, seconds):
        self.t += seconds


class RecordTest(HistoryEnv, unittest.TestCase):
    def setUp(self):
        super().setUp()
        support.reset_run_state()
        self.addCleanup(support.reset_run_state)
        self.clock = FakeClock()
        self.store = jr.HistoryStore(self.repo, "push")
        self.hist = jr.History(self.store, clock=self.clock)

    def read(self):
        return self.store.read()

    def pub(self, event, /, **f):
        self.hist.on_event(event, f)

    def start(self, id="mvn", tool="maven", sig="abc", parent=None):
        self.pub("step_start", id=id, n=1, title=id, parent=parent, tool=tool, sig=sig)

    def test_maven_record(self):
        self.pub("step_start", id="mvn", n=1, title="mvn verify", parent=None, tool="maven", sig="abc")
        self.pub("units", step="mvn", total=85, kind="modules")
        self.pub("unit", step="mvn", done=85, outcome="pass")
        self.pub("series", step="mvn", name="surefire_s", sub="mvn/core", value=4.0)
        self.pub("tests", step="mvn", total=100, fail=0, err=0, skip=0)
        self.clock.advance(101.4)
        self.pub("step_end", id="mvn", status="ok", ms=101400)
        r = self.read()[-1]
        self.assertEqual((r["step"], r["tool"], r["sig"], r["ok"], r["complete"]), ("mvn", "maven", "abc", True, True))
        self.assertEqual(r["units"], {"kind": "modules", "total": 85, "done": 85})
        self.assertEqual(r["series"], {"surefire_s": {"core": 4.0}})   # the "mvn/" step prefix is stripped
        self.assertEqual(r["tests"], 100)
        self.assertEqual(r["title"], "mvn verify")
        self.assertEqual(r["v"], 1)
        self.assertRegex(r["ts"], r"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\dZ$")
        self.assertAlmostEqual(r["wall_s"], 101.4)

    def test_maven_modules_counted_from_reactor_and_module_end(self):
        self.start()
        self.pub("reactor", step="mvn", size=4, poms=["mvn/parent"], tests=True)
        self.pub("module_end", step="mvn", module="mvn/parent", status="ok", ms=100, totals=None)
        self.pub("module_end", step="mvn", module="mvn/a", status="ok", ms=2500, totals=None)
        self.pub("module_end", step="mvn", module="mvn/b", status="ok", ms=1500, totals=None)
        self.pub("module_end", step="mvn", module="mvn/c", status="ok", ms=500, totals=None)
        self.pub("step_end", id="mvn", status="ok", ms=5000)
        r = self.read()[-1]
        self.assertEqual(r["units"], {"kind": "modules", "total": 3, "done": 3})
        self.assertTrue(r["complete"])
        self.assertEqual(r["subs"]["a"], 2.5)
        self.assertEqual(len(r["subs"]), 4)
        self.assertTrue(all("/" not in k for k in r["subs"]))

    def test_gradle_tasks_units_from_module_end(self):
        self.start(id="g", tool="gradle")
        self.pub("module_end", step="g", module="g/a", status="ok", ms=1000, totals=None)
        self.pub("module_end", step="g", module="g/b", status="ok", ms=1000, totals=None)
        self.pub("step_end", id="g", status="ok", ms=3000)
        self.assertEqual(self.read()[-1]["units"], {"kind": "tasks", "total": 2, "done": 2})

    def test_subs_capped_at_400(self):
        self.start()
        for i in range(450):
            self.pub("module_end", step="mvn", module=f"mvn/m{i}", status="ok", ms=1000, totals=None)
        self.pub("step_end", id="mvn", status="ok", ms=1000)
        self.assertEqual(len(self.read()[-1].get("subs", {})), 400)

    def test_incomplete_when_units_short(self):
        self.start()
        self.pub("units", step="mvn", total=85, kind="modules")
        self.pub("unit", step="mvn", done=80, outcome="pass")
        self.pub("step_end", id="mvn", status="ok", ms=1000)
        r = self.read()[-1]
        self.assertTrue(r["ok"])
        self.assertFalse(r["complete"])

    def test_failed_row_recorded_but_flagged(self):
        self.start()
        self.pub("step_end", id="mvn", status="fail", ms=1000)
        r = self.read()[-1]
        self.assertFalse(r["ok"])
        self.assertFalse(r["complete"])

    def test_skip_row_not_recorded(self):
        self.start()
        self.pub("step_end", id="mvn", status="skip", ms=0)
        self.assertEqual(self.read(), [])

    def test_pass_through_run_has_wall_only(self):
        self.start(id="t", tool="generic", sig="")
        self.pub("step_end", id="t", status="ok", ms=2000)
        r = self.read()[-1]
        self.assertEqual(r["wall_s"], 2.0)
        for key in ("units", "tests", "subs", "series"):
            self.assertNotIn(key, r)

    def test_row_without_tool_uses_literal_step(self):
        self.pub("step_start", id="bom", n=2, title="BOM", parent=None, tool=None, sig="")
        self.pub("step_end", id="bom", status="ok", ms=1000)
        r = self.read()[-1]
        self.assertEqual((r["tool"], r["sig"]), ("step", ""))

    def test_child_row_folds_into_parent(self):
        self.pub("step_start", id="gate", n=1, title="gate", parent=None, tool=None, sig="")
        self.pub("step_start", id="gate-mvn", n=1, title="mvn", parent="gate", tool="maven", sig="zz")
        self.pub("units", step="gate-mvn", total=5, kind="modules")
        self.pub("unit", step="gate-mvn", done=5, outcome="pass")
        self.pub("tests", step="gate-mvn", total=9, fail=0, err=0, skip=0)
        self.pub("step_end", id="gate-mvn", status="ok", ms=500)
        self.assertEqual(self.read(), [])
        self.pub("step_end", id="gate", status="ok", ms=900)
        recs = self.read()
        self.assertEqual(len(recs), 1)
        self.assertEqual((recs[0]["step"], recs[0]["units"]["done"], recs[0]["tests"]), ("gate", 5, 9))

    def test_gate_takes_childs_tool(self):
        self.pub("step_start", id="gate", n=1, title="gate", parent=None, tool=None, sig="")
        self.pub("step_start", id="gate-mvn", n=1, title="mvn", parent="gate", tool="maven", sig="zz")
        self.pub("step_end", id="gate-mvn", status="ok", ms=500)
        self.pub("step_end", id="gate", status="ok", ms=900)
        r = self.read()[-1]
        self.assertEqual((r["tool"], r["sig"]), ("maven", "zz"))

    def test_history_series_attributed_to_innermost_running_top_level_row(self):
        jr.history_start(self.repo, history=self.hist)
        self.pub("step_start", id="t", n=1, title="t", parent=None, tool=None, sig="")
        jr.history_series("surefire_s.core", {"juneau-core": 3.0})
        self.pub("step_end", id="t", status="ok", ms=1000)
        self.assertEqual(self.read()[-1]["series"]["surefire_s.core"], {"juneau-core": 3.0})

    def test_history_series_ignored_without_running_row_bad_name_or_nan(self):
        jr.history_start(self.repo, history=self.hist)
        jr.history_series("x", {"a": 1.0})               # no running row
        self.pub("step_start", id="t", n=1, title="t", parent=None, tool=None, sig="")
        jr.history_series("Bad Name", {"a": 1.0})
        jr.history_series("x" * 65, {"a": 1.0})
        jr.history_series("ok_name", {"a": float("nan"), "b": float("inf"), "c": "str", "d": True})
        jr.history_series("ok_name", None)               # never raises
        self.pub("step_end", id="t", status="ok", ms=1000)
        self.assertNotIn("series", self.read()[-1])

    def test_history_series_noop_when_history_not_started(self):
        jr.history_series("x", {"a": 1.0})               # must not raise

    def test_history_start_is_idempotent_and_off_when_disabled(self):
        os.environ["JUNEAU_RUN_HISTORY"] = "0"
        jr.history_start(self.repo)
        self.assertEqual(jr._subscribers, [])
        os.environ["JUNEAU_RUN_HISTORY"] = "1"
        jr.history_start(self.repo)
        jr.history_start(self.repo)
        self.assertEqual(len(jr._subscribers), 1)

    def test_pytest_record_has_no_subs(self):
        self.start(id="py", tool="pytest")
        self.pub("units", step="py", total=3, kind="tests")
        self.pub("unit", step="py", done=3, outcome="pass")
        self.pub("step_end", id="py", status="ok", ms=1000)
        r = self.read()[-1]
        self.assertNotIn("subs", r)
        self.assertEqual(r["units"], {"kind": "tests", "total": 3, "done": 3})

    def test_end_to_end_through_the_bus(self):
        jr.history_start(self.repo)
        with jr.row("bom", "BOM"):
            pass
        self.assertEqual(jr.HistoryStore(self.repo, jr.script_name()).read()[-1]["step"], "bom")


if __name__ == "__main__":
    unittest.main()
