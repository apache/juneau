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

"""The Bazel parser: build-event records to rows, units, counts and a failure excerpt; replay of the recorded
fixtures; the parser under run_tool, run.cmd and the CLI with a fake bazel; and an opt-in live run."""

import io
import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

from scripts_support import PYTHON_SRC, base_env, copy_package, jr, markers, pycache_under, python, reset_run_state
from support import isolate_run_env

from juneau_scripts import parsers

parsers.bind(jr)
from juneau_scripts.parsers import bazel  # noqa: E402  (needs the binding above)
from parser_replay import FakeClock, replay, write_fake_bazel  # noqa: E402
from juneau_scripts import run  # noqa: E402

LIVE = os.environ.get("JUNEAU_PARSER_LIVE") == "1"
FIXTURES_DIR = Path(__file__).resolve().parent / "fixtures" / "parsers" / "bazel"


def started(command="build"):
    return {"id": {"started": {}}, "started": {"command": command}}


def progress(stderr="", stdout=""):
    return {"id": {"progress": {"opaqueCount": 1}}, "progress": {"stderr": stderr, "stdout": stdout}}


def configured(label, kind="genrule rule", size=None):
    c = {"targetKind": kind}
    if size is not None:
        c["testSize"] = size
    return {"id": {"targetConfigured": {"label": label}}, "configured": c}


def configure_aborted(label, description):
    return {"id": {"targetConfigured": {"label": label}},
            "aborted": {"reason": "ANALYSIS_FAILURE", "description": description}}


def completed(label, success=True):
    return {"id": {"targetCompleted": {"label": label, "configuration": {"id": "c"}}},
            "completed": {"success": True} if success else {}}


def action_failed(label, stderr=None, message=None):
    a = {"label": label, "exitCode": 1}
    if stderr is not None:
        a["stderr"] = {"name": "stderr", "uri": Path(stderr).as_uri()}
    if message is not None:
        a["failureDetail"] = {"message": message}
    return {"id": {"actionCompleted": {"primaryOutput": "bazel-out/x", "label": label}}, "action": a}


def test_result(label, status="PASSED"):
    return {"id": {"testResult": {"label": label, "run": 1, "shard": 1, "attempt": 1}}, "testResult": {"status": status}}


def test_summary(label, status, log=None):
    s = {"overallStatus": status, "totalRunCount": 1}
    if log is not None:
        s["failed"] = [{"uri": Path(log).as_uri()}]
    return {"id": {"testSummary": {"label": label}}, "testSummary": s}


def finished(code=0):
    exit_code = {"name": "SUCCESS"} if code == 0 else {"name": "FAILED", "code": code}
    return {"id": {"buildFinished": {}}, "finished": {"overallSuccess": code == 0, "exitCode": exit_code}}


NOTE = ("note", "warn", "no structured output from bazel", "t")


class Bep:
    """A BazelParser whose build-event file the test writes: poll(records) appends and polls, finish(code, records)
    appends and finishes."""

    def __init__(self, case, argv=("bazel", "build", "//..."), side_path=True):
        self.dir = Path(tempfile.mkdtemp())
        case.addCleanup(shutil.rmtree, self.dir, True)
        self.path = self.dir / "bep.json"
        self.parser = bazel.BazelParser(side_path=self.path if side_path else None, clock=FakeClock())
        self.parser.step_id = "t"
        self.cmd, _ = self.parser.prepare(list(argv), {}, str(self.dir))
        case.addCleanup(self.parser.close)

    def write(self, *records):
        with open(self.path, "a", encoding="utf-8") as f:
            for r in records:
                f.write((r if isinstance(r, str) else json.dumps(r)) + "\n")

    def poll(self, *records):
        self.write(*records)
        return self.parser.poll()

    def finish(self, code, *records):
        self.write(*records)
        return self.parser.finish(code)


class BazelParserTest(unittest.TestCase):
    def test_a_build_gives_phases_targets_and_units(self):
        b = Bep(self)
        events = b.poll(started(), progress("INFO: Analyzed 2 targets (3 packages loaded).\n"),
                        configured("//a:a"), configured("//a:b"), completed("//a:a"), completed("//a:b"), finished())
        events += b.finish(0)
        self.assertEqual(events, [
            ("sub_start", "t/analysis", 1, "Analysis"),
            ("progress", "t", "INFO: Analyzed 2 targets (3 packages loaded)."),
            ("sub_end", "t/analysis", "ok", 500),
            ("sub_start", "t/build", 2, "Build"),
            ("units", "t", 2, "targets"),
            ("unit", "t", 1, "pass"),
            ("unit", "t", 2, "pass"),
            ("sub_end", "t/build", "ok", 500)])

    def test_a_test_run_adds_the_test_phase_and_counts(self):
        b = Bep(self, ("bazel", "test", "//..."))
        events = b.poll(started("test"), progress("INFO: Analyzed 2 targets.\n"), configured("//a:gen"),
                        configured("//a:t", "sh_test rule", "SMALL"), completed("//a:gen"), completed("//a:t"),
                        test_result("//a:t"), test_summary("//a:t", "PASSED"), finished())
        events += b.finish(0)
        self.assertEqual(events[3:], [
            ("sub_start", "t/build", 2, "Build"),
            ("units", "t", 2, "targets"),
            ("unit", "t", 1, "pass"),
            ("sub_end", "t/build", "ok", 500),
            ("sub_start", "t/test", 3, "Test"),
            ("unit", "t", 2, "pass"),
            ("tests", "t", 1, 0, 0, 0),
            ("sub_end", "t/test", "ok", 500)])

    def test_a_failing_test_gives_the_tail_of_its_log(self):
        b = Bep(self, ("bazel", "test", "//a:t"))
        log = b.dir / "test.log"
        log.write_text("".join(f"line {i}\n" for i in range(50)), encoding="utf-8")
        events = b.poll(started("test"), configured("//a:t", "sh_test rule", "SMALL"), completed("//a:t"),
                        test_result("//a:t", "FAILED"), test_summary("//a:t", "FAILED", log), finished(3))
        events += b.finish(3)
        text = "//a:t FAILED\n" + "\n".join(f"line {i}" for i in range(10, 50))
        self.assertIn(("unit", "t", 1, "fail"), events)
        self.assertIn(("tests", "t", 1, 1, 0, 0), events)
        self.assertIn(("sub_end", "t/test", "fail", 500), events)
        self.assertEqual(events[-1], ("failure", text, "t"))
        self.assertEqual(events[-42:-1], [("cond", line) for line in text.splitlines()])

    def test_a_build_failure_names_the_failing_action(self):
        b = Bep(self)
        events = b.poll(started(), configured("//b:bad"),
                        action_failed("//b:bad", message="Executing genrule //b:bad failed: (Exit 1)"),
                        completed("//b:bad", success=False), finished(1))
        events += b.finish(1)
        self.assertIn(("unit", "t", 1, "fail"), events)
        self.assertIn(("sub_end", "t/build", "fail", 500), events)
        self.assertEqual(events[-1], ("failure", "Executing genrule //b:bad failed: (Exit 1)", "t"))

    def test_a_build_failure_with_readable_stderr_shows_it(self):
        b = Bep(self)
        err = b.dir / "stderr-1"
        err.write_text("boom\n", encoding="utf-8")
        b.poll(started(), configured("//b:bad"), action_failed("//b:bad", stderr=err, message="genrule failed"),
               completed("//b:bad", success=False), finished(1))
        self.assertEqual(b.finish(1)[-1], ("failure", "genrule failed\nboom", "t"))

    def test_an_analysis_failure_fails_the_analysis_row(self):
        b = Bep(self)
        events = b.poll(started(), configure_aborted("//c:c", "no such target '//c:missing'"), finished(1))
        events += b.finish(1)
        self.assertEqual(events, [
            ("sub_start", "t/analysis", 1, "Analysis"),
            ("unit", "t", 1, "skip"),
            ("sub_end", "t/analysis", "fail", 500),
            ("cond", "no such target '//c:missing'"),
            ("failure", "no such target '//c:missing'", "t")])

    def test_a_build_failure_after_the_test_row_opened_fails_that_row(self):
        # Bazel 7 interleaves phases: a test row is open when a compile failure for another target arrives.
        b = Bep(self, ("bazel", "test", "//..."))
        events = b.poll(started("test"), configured("//a:t", "sh_test rule", "SMALL"), completed("//a:t"),
                        test_result("//a:t"), configured("//b:bad"),
                        action_failed("//b:bad", message="compile failed"), completed("//b:bad", success=False),
                        test_summary("//a:t", "PASSED"), finished(1))
        events += b.finish(1)
        self.assertIn(("sub_end", "t/test", "fail", 500), events)

    def test_a_configure_abort_after_the_build_row_opened_fails_that_row(self):
        b = Bep(self)
        events = b.poll(started(), configured("//a:a"), completed("//a:a"),
                        configure_aborted("//c:c", "no such target '//c:missing'"), finished(1))
        events += b.finish(1)
        self.assertIn(("sub_end", "t/build", "fail", 500), events)

    def test_an_aborted_configure_gives_a_skipped_unit(self):
        b = Bep(self)
        events = b.poll(started(), configure_aborted("//c:c", "no such target"), finished(1))
        self.assertIn(("unit", "t", 1, "skip"), events)

    def test_a_configured_test_without_a_summary_is_skipped_at_the_end(self):
        b = Bep(self, ("bazel", "test", "//..."))
        events = b.poll(started("test"), configured("//a:t", "sh_test rule", "SMALL"), completed("//a:t"),
                        finished(1))
        self.assertNotIn(("unit", "t", 1, "skip"), events)
        events = b.finish(1)
        self.assertIn(("unit", "t", 1, "skip"), events)

    def test_a_failed_test_without_a_readable_log_is_named_in_the_excerpt(self):
        b = Bep(self, ("bazel", "test", "//a:t"))
        events = b.poll(started("test"), configured("//a:t", "sh_test rule", "SMALL"), completed("//a:t"),
                        test_result("//a:t", "FAILED"),
                        test_summary("//a:t", "FAILED", "/no/such/dir/test.log"), finished(3))
        events += b.finish(3)
        self.assertEqual(events[-2:], [("cond", "//a:t FAILED"), ("failure", "//a:t FAILED", "t")])

    def test_no_records_falls_back_to_the_console_tail(self):
        b = Bep(self)
        b.parser.on_line("ERROR: something")
        self.assertEqual(b.finish(1), [NOTE, ("cond", "ERROR: something"), ("failure", "ERROR: something", "t")])
        b2 = Bep(self)
        self.assertEqual(b2.finish(0), [NOTE])

    def test_too_many_bad_records_fall_back(self):
        b = Bep(self)
        b.parser.on_line("ERROR: x")
        b.write(*["{not json"] * (bazel.BAD_RECORD_LIMIT + 1), started())
        events = b.finish(1)
        self.assertIn(NOTE, events)
        self.assertEqual(events[-1], ("failure", "ERROR: x", "t"))

    def test_one_bad_record_is_skipped(self):
        b = Bep(self)
        events = b.poll("{not json", started(), "[1, 2]", finished())
        events += b.finish(0)
        self.assertEqual(events, [("sub_start", "t/analysis", 1, "Analysis"), ("sub_end", "t/analysis", "ok", 500)])

    def test_a_non_build_command_is_left_alone(self):
        b = Bep(self, ("bazel", "query", "//..."))
        self.assertEqual(b.cmd, ["bazel", "query", "//..."])
        self.assertEqual(b.poll(started()), [])
        b.parser.on_line("x")
        self.assertEqual(b.parser.finish(1), [("cond", "x"), ("failure", "x", "t")])

    def test_the_flag_goes_after_the_command_word(self):
        b = Bep(self, ("bazel", "--output_user_root=/x", "test", "//..."))
        self.assertEqual(b.cmd, ["bazel", "--output_user_root=/x", "test", f"--build_event_json_file={b.path}", "//..."])

    def test_a_user_flag_is_read_and_its_stale_file_ignored(self):
        for flag in (["--build_event_json_file=mine.json"], ["--build_event_json_file", "mine.json"]):
            with self.subTest(flag=flag):
                d = Path(tempfile.mkdtemp())
                self.addCleanup(shutil.rmtree, d, True)
                (d / "mine.json").write_text(json.dumps(started()) + "\n", encoding="utf-8")
                p = bazel.BazelParser(clock=FakeClock())
                p.step_id = "t"
                argv = ["bazel", "build", *flag, "//..."]
                cmd, _ = p.prepare(list(argv), {}, str(d))
                self.assertEqual(cmd, argv)
                self.assertIsNone(p.tmp)
                self.assertEqual(p.poll(), [])
                (d / "mine.json").write_text(json.dumps(started()) + "\n" + json.dumps(finished()) + "\n",
                                             encoding="utf-8")
                self.assertEqual(p.poll()[0], ("sub_start", "t/analysis", 1, "Analysis"))

    def test_a_flag_after_the_double_dash_is_a_target_not_ours(self):
        p = bazel.BazelParser(clock=FakeClock())
        p.step_id = "t"
        cmd, _ = p.prepare(["bazel", "build", "--", "--build_event_json_file=x"], {}, ".")
        self.addCleanup(p.close)
        self.assertTrue(cmd[2].startswith(bazel.FLAG + "="))
        self.assertIsNotNone(p.tmp)

    def test_the_flag_goes_after_the_command_behind_a_launcher(self):
        for argv in (["env", "A=1", "bazel", "build", "//..."], ["env", "-u", "A", "bazel", "build", "//..."]):
            with self.subTest(argv=argv):
                p = bazel.BazelParser(clock=FakeClock())
                p.step_id = "t"
                cmd, _ = p.prepare(list(argv), {}, ".")
                self.addCleanup(p.close)
                at = cmd.index("build") + 1
                self.assertEqual(cmd[:at], argv[:at])
                self.assertTrue(cmd[at].startswith(bazel.FLAG + "="))
                self.assertEqual(cmd[at + 1:], ["//..."])

    def test_close_removes_the_temp_dir(self):
        p = bazel.BazelParser(clock=FakeClock())
        p.step_id = "t"
        cmd, _ = p.prepare(["bazel", "build", "//..."], {}, ".")
        made = Path(cmd[2].split("=", 1)[1]).parent
        self.assertTrue(made.is_dir())
        p.close()
        p.close()
        self.assertFalse(made.exists())

    def test_interactive_still_injects_the_flag(self):
        p = bazel.BazelParser(clock=FakeClock())
        p.step_id = "t"
        p.interactive = True
        cmd, _ = p.prepare(["bazel", "build", "//..."], {}, ".")
        self.addCleanup(p.close)
        self.assertTrue(cmd[2].startswith(bazel.FLAG + "="))

    def test_a_target_gives_one_unit(self):
        b = Bep(self)
        events = b.poll(started(), configured("//a:a"), completed("//a:a"), completed("//a:a"))
        self.assertEqual([e for e in events if e[0] == "unit"], [("unit", "t", 1, "pass")])

    def test_no_configured_targets_means_no_units_event(self):
        b = Bep(self)
        events = b.poll(started(), completed("//a:a"))
        self.assertEqual([e for e in events if e[0] in ("units", "unit")], [("unit", "t", 1, "pass")])

    def test_progress_shows_the_last_line_after_its_last_carriage_return(self):
        b = Bep(self)
        events = b.poll(started(), progress("\rINFO: 1 action running\r[3 / 5] Compiling a.c\n"),
                        progress("\x1b[32mINFO:\x1b[0m Analyzed 1 target\n"))
        self.assertEqual(events, [
            ("sub_start", "t/analysis", 1, "Analysis"),
            ("progress", "t", "[3 / 5] Compiling a.c"),
            ("progress", "t", "INFO: Analyzed 1 target"),
            ("sub_end", "t/analysis", "ok", 500),
            ("sub_start", "t/build", 2, "Build")])

    def test_the_expanded_pattern_gives_the_target_total_before_any_is_configured(self):
        # Bazel configures and completes targets one after another; the pattern record lists them all up front.
        pattern = {"id": {"pattern": {"pattern": ["//a:all"]}},
                   "children": [{"targetConfigured": {"label": "//a:a"}}, {"targetConfigured": {"label": "//a:b"}}]}
        b = Bep(self)
        events = b.poll(started(), pattern, configured("//a:a"), completed("//a:a"), configured("//a:b"))
        self.assertIn(("units", "t", 2, "targets"), events)

    def test_a_bare_level_prefix_is_not_a_progress_line(self):
        b = Bep(self)
        events = b.poll(started(), progress("INFO: Build completed successfully\nINFO: \n"))
        self.assertEqual([e for e in events if e[0] == "progress"],
                         [("progress", "t", "INFO: Build completed successfully")])

    def test_file_uri_path(self):
        self.assertEqual(bazel.file_uri_path("file:///a%20b/c"), Path("/a b/c"))
        self.assertEqual(bazel.file_uri_path("file://localhost/x"), Path("/x"))
        self.assertIsNone(bazel.file_uri_path("file://host/x"))
        self.assertIsNone(bazel.file_uri_path("https://x/y"))
        self.assertIsNone(bazel.file_uri_path(None))

    def test_importing_before_bind_raises_parser_not_bound(self):
        code = ("import sys; sys.path.insert(0, sys.argv[1])\n"
                "from juneau_scripts.parsers import ParserNotBound\n"
                "try:\n    import juneau_scripts.parsers.bazel\nexcept ParserNotBound:\n    print('unbound')\n")
        r = python(["-c", code, str(PYTHON_SRC)], env=base_env())
        self.assertEqual((r.returncode, r.stdout), (0, "unbound\n"), r.stderr)

    def test_bazel_is_registered(self):
        self.assertTrue(parsers.known("bazel"))
        loaded = parsers.load("bazel")   # not assertIs: binding another juneau_run drops the imported parser modules
        self.assertEqual((loaded.__module__, loaded.__name__), (bazel.__name__, "BazelParser"))
        self.assertTrue(jr.known_parser("bazel"))


def write_case(root, records, stdout="INFO: Build completed successfully\n", exit_code=0):
    case = Path(root)
    case.mkdir(parents=True, exist_ok=True)
    (case / "stdout.log").write_text(stdout, encoding="utf-8")
    (case / "side.log").write_bytes("".join(json.dumps(r) + "\n" for r in records).encode())
    (case / "expected.log").write_text(json.dumps({"argv": ["bazel", "build", "//..."], "exit": exit_code,
                                                   "status": "ok" if exit_code == 0 else "fail", "events": None}) + "\n")
    return case


class ReplayHarnessTest(unittest.TestCase):
    RECORDS = [started(), progress("INFO: Analyzed 2 targets.\n"), configured("//a:a"), configured("//a:b"),
               completed("//a:a"), completed("//a:b"), finished()]

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.case = write_case(self.tmp / "case", self.RECORDS)

    def replay(self, chunks):
        side = self.tmp / f"side-{chunks}.json"
        return replay(bazel.BazelParser(side_path=side, clock=FakeClock()), self.case, side_path=side, chunks=chunks)

    def test_fake_clock_advances_per_call(self):
        c = FakeClock(step=0.25)
        self.assertEqual((c(), c(), c()), (0.25, 0.5, 0.75))

    def test_replay_gives_json_shaped_events_and_the_prepared_command(self):
        events, cmd = self.replay(3)
        self.assertEqual(events[0], ["sub_start", "t/analysis", 1, "Analysis"])
        self.assertEqual(events[-1], ["sub_end", "t/build", "ok", 500])
        self.assertEqual(cmd[:2], ["bazel", "build"])
        self.assertTrue(cmd[2].startswith(bazel.FLAG + "="))

    def test_chunking_does_not_change_the_events(self):
        self.assertEqual(self.replay(1)[0], self.replay(7)[0])

    def test_the_cli_prints_and_expects(self):
        script = Path(__file__).resolve().parent / "parser_replay.py"
        bazel_case = self.tmp / "bazel" / "case"
        shutil.copytree(self.case, bazel_case)
        printed = subprocess.run([sys.executable, "-B", str(script), "print", str(bazel_case)], capture_output=True,
                                 text=True, env=base_env())
        self.assertEqual(printed.returncode, 0, printed.stderr)
        self.assertEqual(json.loads(printed.stdout.splitlines()[0]), ["sub_start", "t/analysis", 1, "Analysis"])
        r = subprocess.run([sys.executable, "-B", str(script), "expect", str(bazel_case)], capture_output=True,
                           text=True, env=base_env())
        self.assertEqual(r.returncode, 0, r.stderr)
        text = (bazel_case / "expected.log").read_text(encoding="utf-8")
        self.assertEqual(text.count("\n"), 1)
        self.assertEqual(json.loads(text)["events"], [json.loads(line) for line in printed.stdout.splitlines()])


class Remembering(bazel.BazelParser):
    """Records the temp dir it made, so a test can check close() removed it."""

    def prepare(self, cmd, env, cwd):
        out = super().prepare(cmd, env, cwd)
        self.made = Path(self.tmp)
        return out


class CancelAfterRecords(Remembering):
    def poll(self):
        events = super().poll()
        if self.records:
            raise KeyboardInterrupt
        return events


class FakeBazelTest(unittest.TestCase):
    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        patcher = mock.patch.dict(sys.modules, {"juneau_run": jr})   # restored, not left pointing at this copy
        patcher.start()
        self.addCleanup(patcher.stop)
        saved = sys.dont_write_bytecode
        self.addCleanup(setattr, sys, "dont_write_bytecode", saved)
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.bazel = write_fake_bazel(self.tmp / "bin")
        self.argv_file = self.tmp / "argv.json"
        os.environ.update(RUN_MARKERS="1", FAKE_BAZEL_ARGV=str(self.argv_file))
        self.sinks = jr.Sinks("none", str(self.tmp / "full.log"))

    def run_tool(self, parser):
        out = io.StringIO()
        with redirect_stdout(out):
            result = jr.run_tool([str(self.bazel), "build", "//a:a"], parser, "b", "Bazel", sinks=self.sinks)
        return result, markers(out.getvalue())

    def test_rows_come_from_the_side_file_and_the_temp_dir_is_removed(self):
        p = Remembering()
        result, ms = self.run_tool(p)
        self.assertEqual(result.exit, 0)
        self.assertEqual([m["id"] for m in ms if m["ev"] == "step" and m.get("parent") == "b"], ["b/analysis", "b/build"])
        self.assertEqual([m["status"] for m in ms if m["ev"] == "end" and m["id"].startswith("b/")], ["ok", "ok"])
        argv = json.loads(self.argv_file.read_text())
        self.assertEqual(argv[:2], ["build", f"--build_event_json_file={p.made / 'bep.json'}"])
        self.assertFalse(p.made.exists())
        self.assertEqual((self.tmp / "full.log").read_text(), "INFO: Build completed\n")

    def test_a_cancelled_run_still_removes_the_temp_dir(self):
        os.environ["FAKE_BAZEL_SLEEP"] = "5"
        p = CancelAfterRecords()
        with self.assertRaises(KeyboardInterrupt):
            self.run_tool(p)
        self.assertFalse(p.made.exists())

    def test_an_error_in_the_run_loop_still_removes_the_temp_dir(self):
        class Boom(jr.Sinks):
            def write_full(self, chunk):
                raise RuntimeError("sink failed")

        self.sinks = Boom("none", str(self.tmp / "full.log"))   # the same failure Task 3's test_close_runs_on_error uses
        p = Remembering()
        with self.assertRaises(RuntimeError):
            self.run_tool(p)
        self.assertFalse(p.made.exists())

    def test_run_cmd_detects_bazel(self):
        run._n = 0
        out = io.StringIO()
        with redirect_stdout(out):
            r = run.cmd([str(self.bazel), "build", "//a:a"], title="bazel build")
        self.assertTrue(r.ok)
        ids = [m["id"] for m in markers(out.getvalue()) if m["ev"] == "step"]
        self.assertIn("bazel-build/analysis", ids)

    def test_the_cli_from_a_copied_package_runs_the_bazel_parser_without_bytecode(self):
        dest = copy_package(self.tmp / "pkg")
        env = base_env(RUN_MARKERS="1", FAKE_BAZEL_ARGV=str(self.argv_file))
        r = python([str(dest / "juneau_run.py"), "bazel", str(self.bazel), "build", "//a:a"], cwd=str(self.tmp),
                   env=env, bytecode=True)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("bazel/analysis", [m.get("id") for m in markers(r.stdout) if m["ev"] == "step"])
        self.assertEqual(pycache_under(dest), [])


CASES = ("build", "pass", "cached", "fail", "build-failure", "truncated")


class ReplayTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, True)

    def replay(self, case, chunks=3):
        side = self.tmp / f"{case}-{chunks}.json"
        return replay(bazel.BazelParser(side_path=side, clock=FakeClock()), FIXTURES_DIR / case, side_path=side,
                      chunks=chunks)

    def expected(self, case):
        return json.loads((FIXTURES_DIR / case / "expected.log").read_text(encoding="utf-8"))

    def rows(self, events):
        return [(e[1], e[2]) for e in events if e[0] == "sub_end"]

    def test_every_case_replays_to_its_recorded_events(self):
        for case in CASES:
            with self.subTest(case=case):
                self.assertEqual(self.replay(case)[0], self.expected(case)["events"])

    def test_chunking_does_not_change_any_case(self):
        for case in CASES:
            with self.subTest(case=case):
                self.assertEqual(self.replay(case, 1)[0], self.replay(case, 7)[0])

    def test_the_replayed_command_carries_the_flag(self):
        events, cmd = self.replay("pass")
        self.assertEqual(cmd[:2], ["bazel", "test"])
        self.assertTrue(cmd[2].startswith(bazel.FLAG + "="))

    def test_pass_and_cached_are_green_with_one_test(self):
        for case in ("pass", "cached"):
            with self.subTest(case=case):
                events = self.replay(case)[0]
                self.assertEqual(self.rows(events), [("t/analysis", "ok"), ("t/build", "ok"), ("t/test", "ok")])
                self.assertFalse([e for e in events if e[0] == "failure"])
                self.assertEqual([e for e in events if e[0] == "tests"][-1], ["tests", "t", 1, 0, 0, 0])

    def test_build_has_two_rows_and_two_targets(self):
        events = self.replay("build")[0]
        self.assertEqual(self.rows(events), [("t/analysis", "ok"), ("t/build", "ok")])
        self.assertIn(["units", "t", 2, "targets"], events)

    def test_fail_counts_the_failing_test_and_names_it(self):
        events = self.replay("fail")[0]
        self.assertEqual([e for e in events if e[0] == "tests"][-1], ["tests", "t", 2, 1, 0, 0])
        self.assertEqual(self.rows(events)[-1], ("t/test", "fail"))
        self.assertIn("//fail:bad_test", events[-1][1])

    def test_build_failure_fails_the_build_row_and_names_the_target(self):
        events = self.replay("build-failure")[0]
        self.assertEqual(self.rows(events)[-1], ("t/build", "fail"))
        self.assertIn("//broken:bad", events[-1][1])

    def test_a_truncated_file_fails_the_open_row_without_the_fallback_note(self):
        events = self.replay("truncated")[0]
        self.assertEqual(self.rows(events)[-1][1], "fail")
        self.assertEqual(events[-1][0], "failure")
        self.assertNotIn("no structured output from bazel", [e[2] for e in events if e[0] == "note"])


@unittest.skipUnless(LIVE and shutil.which("bazel"), "set JUNEAU_PARSER_LIVE=1 (and USE_BAZEL_VERSION) with bazel on PATH")
class BazelLiveTest(unittest.TestCase):
    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.ws = Path(tempfile.mkdtemp(prefix="jrbz", dir="/tmp"))
        self.root = self.ws / "root"
        shutil.copytree(FIXTURES_DIR / "workspace", self.ws / "ws")
        for script in (self.ws / "ws").glob("*/*.sh"):
            script.chmod(0o755)
        self.addCleanup(self.cleanup)
        os.environ["RUN_MARKERS"] = "1"
        os.environ.setdefault("USE_BAZEL_VERSION", "7.4.1")

    def cleanup(self):
        subprocess.run(["bazel", f"--output_user_root={self.root}", "shutdown"], cwd=self.ws / "ws",
                       capture_output=True, timeout=120)
        subprocess.run(["chmod", "-R", "u+w", str(self.ws)], capture_output=True)
        shutil.rmtree(self.ws, True)

    def test_a_real_bazel_test_run_gives_three_green_rows(self):
        out = io.StringIO()
        with redirect_stdout(out):
            r = jr.run_tool(["bazel", f"--output_user_root={self.root}", "test", "//ok:all"], "bazel", "b", "Bazel",
                            cwd=str(self.ws / "ws"), sinks=jr.Sinks("none", str(self.ws / "full.log")))
        ms = markers(out.getvalue())
        self.assertEqual(r.exit, 0, (self.ws / "full.log").read_text(errors="replace")[-2000:])
        self.assertEqual([m["title"] for m in ms if m["ev"] == "step" and m.get("parent") == "b"],
                         ["Analysis", "Build", "Test"])
