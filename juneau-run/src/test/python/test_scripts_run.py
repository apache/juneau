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
"""juneau_scripts.run's helpers in plain mode (no juneau_run) and under markers."""

import io
import os
import sys
import tempfile
import types
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

import scripts_support
from scripts_support import isolate_run_env, jr, markers, reset_run_state

from juneau_scripts import parsers, run, testing

PY = sys.executable


class PlainCase(unittest.TestCase):
    def setUp(self):
        isolate_run_env(self)
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)
        run._n = 0


class MarkerCase(unittest.TestCase):
    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        sys.modules["juneau_run"] = jr
        os.environ["RUN_MARKERS"] = "1"
        run._n = 0

    def capture(self, fn, *args, **kw):
        out = io.StringIO()
        with redirect_stdout(out):
            result = fn(*args, **kw)
        return result, markers(out.getvalue()), out.getvalue()


class SayAndStepTest(PlainCase):
    def test_say_prints_in_a_plain_run(self):
        out = io.StringIO()
        with redirect_stdout(out):
            run.say("hello")
            run.say("bad", "error")
        self.assertEqual(out.getvalue(), "hello\nbad\n")

    def test_step_yields_a_null_step_without_juneau_run(self):
        out = io.StringIO()
        with redirect_stdout(out), run.step("s", "Step") as st:
            st.fail(exit=3)
            st.skip()
        self.assertEqual(out.getvalue(), "")

    def test_a_null_steps_unit_yields_a_handle_that_does_nothing(self):
        out = io.StringIO()
        with redirect_stdout(out), run.step("s", "Step") as st:
            with st.unit("a", "A") as u:
                u.dot("fail")
                u.fail()
        self.assertEqual((out.getvalue(), st.status), ("", "ok"))

    def test_routed_and_console_requested_are_false(self):
        self.assertFalse(run.routed())
        self.assertFalse(run.console_requested())


class CmdPlainTest(PlainCase):
    def test_exit_code_and_capture(self):
        r = run.cmd([PY, "-c", "import sys; print('o'); print('e', file=sys.stderr); sys.exit(3)"], title="t",
                    capture=True)
        self.assertEqual((r.ok, r.code, r.out, r.err), (False, 3, "o\n", "e\n"))
        self.assertIsInstance(r.ms, int)

    def test_cmd_input_bytes_reach_stdin_not_log(self):
        out = io.StringIO()
        with redirect_stdout(out):
            r = run.cmd([PY, "-c", "import sys; print(len(sys.stdin.buffer.read()))"], title="stdin", capture=True, input=b"s3cret-token")
        self.assertEqual((r.ok, r.out.strip()), (True, "12"))
        self.assertNotIn("s3cret-token", out.getvalue() + r.out + r.err)

    def test_cmd_input_with_tty_rejected(self):
        with self.assertRaises(ValueError):
            run.cmd(["true"], title="x", tty=True, input=b"x")

    def test_reports_false_is_accepted_in_a_plain_run(self):
        self.assertTrue(run.cmd([PY, "-c", "pass"], title="t", reports=False).ok)

    def test_bar_is_ignored_in_a_plain_run(self):
        self.assertTrue(run.cmd([PY, "-c", "pass"], title="t", bar="tests").ok)

    def test_cannot_start(self):
        r = run.cmd(["/nonexistent/tool"], title="t")
        self.assertEqual((r.ok, r.code), (False, 127))
        self.assertIn("No such file or directory", r.err)

    def test_remembers_the_title(self):
        run.cmd([PY, "-c", "pass"], title="Build it")
        from juneau_scripts import _base
        self.assertEqual(_base.last_title(), "Build it")


class RoutedTest(MarkerCase):
    def test_routed_is_true_under_markers(self):
        self.assertTrue(run.routed())
        self.assertFalse(run.console_requested())

    def test_console_requested_follows_the_console_setting(self):
        with mock.patch.dict(os.environ, {"JUNEAU_RUN_CONSOLE": "condensed"}):
            self.assertTrue(run.console_requested())


class ExportConsoleTest(PlainCase):
    def test_export_console_precedence(self):
        with mock.patch.dict(os.environ, {}):
            os.environ.pop("JUNEAU_RUN_CONSOLE", None)
            self.assertEqual(run.export_console(), "condensed")                  # the default
            self.assertEqual(run.export_console(default="full"), "condensed")    # now the env wins over the default
            os.environ["JUNEAU_RUN_CONSOLE"] = "none"
            self.assertEqual(run.export_console(), "none")
            self.assertEqual(run.export_console("full"), "full")                 # the flag wins over the env
            self.assertEqual(os.environ["JUNEAU_RUN_CONSOLE"], "full")
            with self.assertRaises(ValueError):
                run.export_console("loud")
            self.assertEqual(os.environ["JUNEAU_RUN_CONSOLE"], "full")

    def test_add_console_argument(self):
        import argparse
        parser = argparse.ArgumentParser()
        run.add_console_argument(parser)
        self.assertIsNone(parser.parse_args([]).console)
        self.assertEqual(parser.parse_args(["--console", "none"]).console, "none")


class ParserForTest(unittest.TestCase):
    def test_consumer_commands_get_the_parser_they_had_or_the_detected_one(self):
        cases = [
            (["git", "push"], None, "generic"),
            ([PY, "scripts/check.py"], None, "generic"),
            (["mvn", "verify"], None, "maven"),
            (["mvn", "-q", "verify"], None, "generic"),   # quiet Maven prints nothing the Maven parser reads
            (["mvn", "-q", "verify"], "generic", "generic"),   # an explicit tool always wins
            (["npm", "ci"], None, "npm"),
            (["uv", "run", "--with", "pytest", "pytest", "-q"], None, "pytest"),
            (["./gradlew", "build"], None, "gradle"),
            (["bazel", "query", "//..."], None, "generic"),
            ([PY, "-c", "pass"], "no-such-parser", "generic"),
            ([PY, "-c", "pass"], "maven", "maven"),
        ]
        for cmd, tool, want in cases:
            with self.subTest(cmd=cmd, tool=tool):
                self.assertEqual(run._parser_for(jr, cmd, tool), want)

    def test_a_parser_class_or_instance_passes_through(self):
        p = jr.GenericParser()
        self.assertIs(run._parser_for(jr, ["mvn"], p), p)
        self.assertIs(run._parser_for(jr, ["mvn"], jr.GenericParser), jr.GenericParser)

    def test_an_old_juneau_run_without_known_parser_keeps_the_builtin_check(self):
        old = types.SimpleNamespace(PARSERS={"maven": object, "generic": object})
        self.assertEqual(run._parser_for(old, ["mvn", "verify"], None), "maven")
        self.assertEqual(run._parser_for(old, ["bazel", "build", "//..."], None), "generic")
        self.assertEqual(run._parser_for(old, ["x"], "bazel"), "generic")

    def test_bazel_build_commands_get_the_bazel_parser(self):
        self.assertEqual(run._parser_for(jr, ["bazel", "test", "//..."], None), "bazel")
        self.assertEqual(run._parser_for(jr, ["bazel", "query", "//..."], None), "generic")
        self.assertEqual(run._parser_for(jr, ["x"], "bazel"), "bazel")

class ConsoleNoneCase(unittest.TestCase):
    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        sys.modules["juneau_run"] = jr
        self.log = Path(tempfile.mkdtemp()) / "full.log"
        os.environ["JUNEAU_RUN_CONSOLE"] = "none"
        os.environ["JUNEAU_RUN_FULL_LOG"] = str(self.log)
        run._n = 0


class CmdConsoleNoneTest(ConsoleNoneCase):
    def test_routed_under_none(self):
        self.assertTrue(run.routed())

    def test_child_output_goes_to_the_full_log_and_not_stdout(self):
        out = io.StringIO()
        with redirect_stdout(out):
            r = run.cmd([PY, "-c", "print('child-line')"], title="Child")
        self.assertTrue(r.ok)
        self.assertEqual(out.getvalue(), "")
        self.assertIn(b"child-line", self.log.read_bytes())

    def test_cmd_input_not_in_full_log(self):
        with redirect_stdout(io.StringIO()):
            r = run.cmd([PY, "-c", "import sys; sys.stdout.write(str(len(sys.stdin.buffer.read())))"], title="stdin",
                        input=b"s3cret-token")
        self.assertTrue(r.ok)
        self.assertIn(b"12", self.log.read_bytes())
        self.assertNotIn(b"s3cret-token", self.log.read_bytes())

    def test_default_full_log_when_none_is_given(self):
        del os.environ["JUNEAU_RUN_FULL_LOG"]
        os.environ["JUNEAU_RUN_LOG_DIR"] = str(self.log.parent)
        out = io.StringIO()
        with redirect_stdout(out):
            run.cmd([PY, "-c", "print('child-line')"], title="Child")
        self.assertEqual(out.getvalue(), "")
        logs = list(self.log.parent.glob("*.log"))
        self.assertEqual(len(logs), 1)
        self.assertIn(b"child-line", logs[0].read_bytes())

    def test_no_markers_under_none(self):
        out = io.StringIO()
        with redirect_stdout(out):
            run.cmd([PY, "-c", "pass"], title="Quiet")
        self.assertNotIn("##run", out.getvalue())


class CmdMarkersTest(MarkerCase):
    def test_runs_through_run_tool_with_the_step_number(self):
        r, got, _ = self.capture(run.cmd, [PY, "-c", "print('x')"], title="Build it", n=4)
        self.assertTrue(r.ok)
        self.assertEqual((r.out, r.err), ("", ""))
        self.assertEqual(got[0], {"ev": "step", "id": "build-it", "n": 4, "title": "Build it"})
        self.assertEqual(got[-1], {"ev": "end", "id": "build-it", "status": "ok"})

    def test_cmd_input_bytes_reach_stdin_not_log(self):
        r, got, text = self.capture(run.cmd, [PY, "-c", "import sys; print(len(sys.stdin.buffer.read()))"], title="stdin", capture=True, input=b"s3cret-token")
        self.assertEqual((r.ok, r.out.strip()), (True, "12"))
        self.assertNotIn("s3cret-token", text + repr(got))

    def test_no_tool_detects_the_parser_from_the_command(self):
        with mock.patch.object(jr, "run_tool", wraps=jr.run_tool) as rt:
            r, _, _ = self.capture(run.cmd, ["/nonexistent/mvn", "verify"], title="mvn verify")
        self.assertEqual(r.code, 127)
        self.assertEqual(rt.call_args.args[1], "maven")

    def test_reports_false_reaches_run_tool(self):
        with mock.patch.object(jr, "run_tool", wraps=jr.run_tool) as rt:
            self.capture(run.cmd, [PY, "-c", "pass"], title="t", reports=False)
        self.assertIs(rt.call_args.kwargs["reports"], False)

    def test_reports_left_on_is_not_passed_so_an_older_juneau_run_still_works(self):
        with mock.patch.object(jr, "run_tool", wraps=jr.run_tool) as rt:
            self.capture(run.cmd, [PY, "-c", "pass"], title="t")
        self.assertNotIn("reports", rt.call_args.kwargs)

    def test_bar_reaches_run_tool(self):
        with mock.patch.object(jr, "run_tool", return_value=jr.Result(0, 1)) as rt:
            self.capture(run.cmd, ["pytest"], title="t", tool="pytest", bar="tests")
        self.assertEqual(rt.call_args.kwargs["bar"], "tests")

    def test_no_bar_is_not_passed_so_an_older_juneau_run_still_works(self):
        with mock.patch.object(jr, "run_tool", return_value=jr.Result(0, 1)) as rt:
            self.capture(run.cmd, ["pytest"], title="t", tool="pytest")
        self.assertNotIn("bar", rt.call_args.kwargs)

    def test_a_bar_the_detected_parser_does_not_offer_raises_before_the_command_runs(self):
        with mock.patch.object(jr.subprocess, "Popen") as popen, self.assertRaises(ValueError):
            self.capture(run.cmd, [PY, "-c", "pass"], title="t", bar="tests")     # detected: generic
        popen.assert_not_called()

    def test_an_explicit_tool_wins_over_detection(self):
        with mock.patch.object(jr, "run_tool", wraps=jr.run_tool) as rt:
            self.capture(run.cmd, ["/nonexistent/mvn", "verify"], title="mvn verify", tool="generic")
        self.assertEqual(rt.call_args.args[1], "generic")

    def test_capture_returns_the_merged_output(self):
        r, _, _ = self.capture(run.cmd, [PY, "-c", "import sys; print('o'); print('e', file=sys.stderr)"],
                               title="t", capture=True)
        self.assertIn("o", r.out)
        self.assertIn("e", r.out)
        self.assertEqual(r.err, "")

    def test_numbers_come_from_the_session_counter(self):
        _, first, _ = self.capture(run.cmd, [PY, "-c", "pass"], title="a")
        _, second, _ = self.capture(run.cmd, [PY, "-c", "pass"], title="b")
        self.assertEqual((first[0]["n"], second[0]["n"]), (1, 2))

    def test_cannot_start_closes_the_step_twice_and_returns_127(self):
        r, got, _ = self.capture(run.cmd, ["/nonexistent/mvn"], title="mvn verify", id="tests", n=3, tool="maven")
        self.assertEqual((r.ok, r.code), (False, 127))
        self.assertIn("No such file or directory", r.err)
        ends = [m for m in got if m["ev"] == "end"]
        self.assertEqual(ends, [{"ev": "end", "id": "tests", "status": "fail", "exit": 127},
                                {"ev": "end", "id": "tests", "status": "fail"}])

    def test_unknown_parser_name_falls_back_to_generic(self):
        r, got, _ = self.capture(run.cmd, [PY, "-c", "pass"], title="t", tool="no-such-parser")
        self.assertTrue(r.ok)

    def test_a_parser_instance_is_accepted(self):
        r, _, _ = self.capture(run.cmd, [PY, "-c", "pass"], title="t", tool=jr.GenericParser())
        self.assertTrue(r.ok)

    def test_step_emits_markers(self):
        out = io.StringIO()
        with redirect_stdout(out):
            with run.step("s", "Step", n=2) as st:
                st.skip()
        self.assertEqual(markers(out.getvalue()), [{"ev": "step", "id": "s", "n": 2, "title": "Step"},
                                                   {"ev": "end", "id": "s", "status": "skip"}])


class UnroutedDetectTest(PlainCase):
    def test_unrouted_runs_never_detect(self):
        with mock.patch.object(parsers, "detect", side_effect=AssertionError("detect ran")), \
                redirect_stdout(io.StringIO()):
            r = run.cmd([PY, "-c", "pass"], title="t")
        self.assertTrue(r.ok)


class GateTest(PlainCase):
    def gate(self, code, **kw):
        return run.gate([PY, "-c", f"import sys; sys.exit({code})"], title="Gate", **kw)

    def test_default_codes(self):
        for code, ok, status in ((0, True, "pass"), (2, False, "fail"), (3, True, "skip"), (1, False, "error"),
                                 (9, False, "error")):
            with self.subTest(code=code):
                r = self.gate(code)
                self.assertEqual((r.ok, r.code, r.status), (ok, code, status))

    def test_custom_codes(self):
        r = self.gate(1, codes={0: "pass", 1: "fail"})
        self.assertEqual((r.ok, r.status), (False, "fail"))

    def test_default_codes_are_read_only(self):
        with self.assertRaises(TypeError):
            run.GATE_CODES[5] = "pass"


class GateMarkersTest(MarkerCase):
    def test_a_gate_detects_the_parser_for_its_child_row(self):
        with mock.patch.object(jr, "run_tool", wraps=jr.run_tool) as rt:
            self.capture(run.gate, ["/nonexistent/mvn", "verify"], title="Sonar")
        self.assertEqual(rt.call_args.args[1:3], ("maven", "sonar-run"))

    def test_skip_marks_the_parent_step_and_its_child_row_skipped(self):
        r, got, _ = self.capture(run.gate, [PY, "-c", "import sys; sys.exit(3)"], title="Sonar", n=6)
        self.assertEqual((r.ok, r.status), (True, "skip"))
        self.assertEqual(got[0], {"ev": "step", "id": "sonar", "n": 6, "title": "Sonar"})
        self.assertEqual(got[1], {"ev": "step", "id": "sonar-run", "n": 1, "title": "Sonar", "parent": "sonar"})
        self.assertEqual(got[-2], {"ev": "end", "id": "sonar-run", "status": "skip"})
        self.assertEqual(got[-1], {"ev": "end", "id": "sonar", "status": "skip"})

    def test_fail_carries_the_exit_code_on_both_rows(self):
        _, got, _ = self.capture(run.gate, [PY, "-c", "import sys; sys.exit(2)"], title="Sonar")
        self.assertEqual(got[-2], {"ev": "end", "id": "sonar-run", "status": "fail", "exit": 2})
        self.assertEqual(got[-1], {"ev": "end", "id": "sonar", "status": "fail", "exit": 2})

    def test_pass_ends_both_rows_ok(self):
        _, got, _ = self.capture(run.gate, [PY, "-c", "pass"], title="Sonar")
        self.assertEqual(got[-2:], [{"ev": "end", "id": "sonar-run", "status": "ok"},
                                    {"ev": "end", "id": "sonar", "status": "ok"}])

    def test_reports_false_reaches_the_gates_child_row(self):
        with mock.patch.object(jr, "run_tool", wraps=jr.run_tool) as rt:
            self.capture(run.gate, [PY, "-c", "pass"], title="Sonar", reports=False)
        self.assertIs(rt.call_args.kwargs["reports"], False)

    def test_bar_reaches_the_gates_child_row(self):
        with mock.patch.object(jr, "run_tool", return_value=jr.Result(0, 1)) as rt:
            self.capture(run.gate, ["pytest"], title="Tests", tool="pytest", bar="tests")
        self.assertEqual(rt.call_args.kwargs["bar"], "tests")

    def test_the_label_names_the_gates_row(self):
        with mock.patch.object(jr, "step", wraps=jr.step) as step:
            self.capture(run.gate, [PY, "-c", "pass"], title="SonarQube gate", id="sonar", label="Sonar")
        self.assertEqual(step.call_args.kwargs["label"], "Sonar")


class HandoffTest(MarkerCase):
    def test_plain_handoff(self):
        del sys.modules["juneau_run"]
        with run.handoff(reserve=3) as extra:
            self.assertEqual(extra, {"JUNEAU_PUSH_ACTIVE": "1"})

    def test_reserves_step_numbers_with_markers_on(self):
        run._n = 4
        with run.handoff(reserve=3) as extra:
            self.assertEqual(extra["JUNEAU_PUSH_ACTIVE"], "1")
            self.assertEqual(extra["JUNEAU_RUN_N_BASE"], "4")
        self.assertEqual(run._next_n(), 8)
        self.assertNotIn("JUNEAU_PUSH_ACTIVE", os.environ)

    def test_no_reserve_no_base(self):
        with run.handoff() as extra:
            self.assertNotIn("JUNEAU_RUN_N_BASE", extra)


class ChildEnvTest(unittest.TestCase):
    def test_strips_protocol_and_nesting_variables(self):
        env = {"RUN_MARKERS": "1", "RUN_ARTIFACTS": "/a", "JUNEAU_PUSH_ACTIVE": "1", "JUNEAU_RUN_ACTIVE": "1",
               "JUNEAU_RUN_SESSION": "1", "JUNEAU_RUN_N_BASE": "3", "JUNEAU_RUN_PY": "/x.py", "KEEP_ME": "1"}
        with mock.patch.dict(os.environ, env):
            child = run.child_env()
            self.assertEqual({k: child.get(k) for k in env},
                             {**{k: None for k in env}, "JUNEAU_RUN_PY": "/x.py", "KEEP_ME": "1"})
            self.assertIn("JUNEAU_RUN_SESSION", run.child_env(keep=("JUNEAU_RUN_SESSION",)))
            self.assertEqual(run.child_env(strip_protocol=False)["RUN_MARKERS"], "1")
            self.assertEqual(os.environ["RUN_MARKERS"], "1")


class SessionHistoryAndPlanTest(MarkerCase):
    class Recorder:
        def __init__(self):
            self.events = []

        def __call__(self, kind, fields):
            self.events.append((kind, dict(fields)))

        def of(self, kind):
            return [f for k, f in self.events if k == kind]

    def test_session_plan_kwarg_and_method(self):
        rec = jr.subscribe(self.Recorder())
        with redirect_stdout(io.StringIO()), run.session("t", plan=["a", "b"], exit=False) as s:
            self.assertEqual(rec.of("plan")[-1]["ids"], ["a", "b"])
            s.plan(["b"])
            self.assertEqual(rec.of("plan")[-1]["ids"], ["b"])

    def test_session_starts_history_once_for_the_repo(self):
        with redirect_stdout(io.StringIO()), mock.patch.object(jr, "history_start") as hs, \
                run.session("t", repo="/some/repo", exit=False):
            pass
        hs.assert_called_once_with("/some/repo")

    def test_session_records_history_in_the_given_dir(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.environ["JUNEAU_RUN_HISTORY"] = "1"
            os.environ["JUNEAU_RUN_HISTORY_DIR"] = tmp
            with redirect_stdout(io.StringIO()), run.session("t", repo=tmp, exit=False):
                with run.step("a", "A"):
                    pass
            files = list(Path(tmp).rglob("*.jsonl"))
            self.assertEqual(len(files), 1)
            self.assertIn('"step":"a"', files[0].read_text(encoding="utf-8"))

    def test_history_off_writes_nothing(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.environ["JUNEAU_RUN_HISTORY_DIR"] = tmp
            with redirect_stdout(io.StringIO()), run.session("t", repo=tmp, exit=False):
                with run.step("a", "A"):
                    pass
            self.assertEqual(list(Path(tmp).rglob("*.jsonl")), [])


class _Recorder:
    def __init__(self):
        self.events = []

    def __call__(self, kind, fields):
        self.events.append((kind, dict(fields)))

    def of(self, kind):
        return [f for k, f in self.events if k == kind]


class CmdSummarizeTest(unittest.TestCase):
    """juneau_run loaded, markers off, default sinks: a summarize callback routes the command through run_tool."""

    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        saved = sys.modules.get("juneau_run")
        self.addCleanup(lambda: sys.modules.__setitem__("juneau_run", saved) if saved is not None
                        else sys.modules.pop("juneau_run", None))
        sys.modules["juneau_run"] = jr
        run._n = 0

    def test_cmd_with_summarize_routes_through_run_tool_without_markers(self):
        rec = jr.subscribe(_Recorder())
        with redirect_stdout(io.StringIO()):
            r = run.cmd([PY, "-c", "print('x')"], title="T", summarize=lambda text: "saw " + text.strip())
        self.assertTrue(r.ok)
        self.assertEqual([e["summary"] for e in rec.of("step_end")], ["saw x"])

    def test_cmd_summarize_runs_while_the_row_is_open(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.environ.pop("JUNEAU_RUN_HISTORY", None)
            os.environ["JUNEAU_RUN_HISTORY_DIR"] = tmp
            jr.history_start(tmp)

            def summarize(text):
                jr.history_series("x", {"a": 1})
                return None
            with redirect_stdout(io.StringIO()):
                run.cmd([PY, "-c", "pass"], title="T", id="t", summarize=summarize)
            [record] = jr.HistoryStore(tmp, "test").read()
        self.assertEqual(record["series"]["x"], {"a": 1.0})

    def test_cmd_without_summarize_keeps_the_plain_path(self):
        with mock.patch.object(jr, "run_tool", side_effect=AssertionError("routed")):
            r = run.cmd([PY, "-c", "pass"], title="T")
        self.assertTrue(r.ok)


class CmdSummarizeWithoutEngineTest(PlainCase):
    def test_cmd_with_summarize_and_no_jr_runs_plain_and_never_calls_it(self):
        f = mock.Mock(return_value="never")
        r = run.cmd([PY, "-c", "import sys; sys.exit(4)"], title="T", summarize=f)
        self.assertEqual((r.ok, r.code), (False, 4))
        f.assert_not_called()


class SessionWithoutEngineTest(PlainCase):
    def test_session_without_the_engine_loaded_does_not_start_history(self):
        with redirect_stdout(io.StringIO()), run.session("t", plan=["a"], exit=False) as s:
            s.plan(["b"])
        self.assertEqual(s.code, 0)


class ErrAndHintPlainTest(PlainCase):
    def test_err_and_hint_go_to_stderr_in_a_plain_run(self):
        out, err = io.StringIO(), io.StringIO()
        with redirect_stdout(out), mock.patch.object(sys, "stderr", err):
            run.err("bad")
            run.hint("try this")
            run.hint(["a", "b"])
        self.assertEqual(out.getvalue(), "")
        self.assertEqual(err.getvalue(), "bad\ntry this\na\nb\n")


class ErrAndHintConsoleTest(MarkerCase):
    def test_err_and_hint_use_stderr_when_the_view_does_not_own_the_screen(self):
        out, err = io.StringIO(), io.StringIO()
        with redirect_stdout(out), mock.patch.object(sys, "stderr", err):
            run.err("bad")
            run.hint(["fix it"])
        self.assertEqual(err.getvalue(), "bad\nfix it\n")

    def test_err_and_hint_go_to_the_view_when_it_owns_the_screen(self):
        with mock.patch.object(jr, "console_owns_screen", return_value=True), \
                mock.patch.object(jr, "failure") as failure, mock.patch.object(jr, "hint") as hint:
            run.err("bad")
            run.hint("fix it")
        failure.assert_called_once_with("bad")
        hint.assert_called_once_with(["fix it"])


class NextNTest(PlainCase):
    def test_next_n_counts_and_reserves(self):
        self.assertEqual(run.next_n(), 1)
        self.assertEqual(run.next_n(count=3), 2)
        self.assertEqual(run.next_n(), 5)

    def test_next_n_follows_the_session_counter_and_its_base(self):
        os.environ[run.N_BASE_ENV] = "7"
        with redirect_stdout(io.StringIO()), run.session("t", exit=False):
            self.assertEqual(run.next_n(), 8)
            with run.step("s", "Step") as st:   # a step takes the next number too
                pass
            self.assertEqual(run.next_n(), 10)


class CmdParentAndLabelTest(MarkerCase):
    def test_cmd_with_a_parent_is_a_child_row_numbered_from_one(self):
        out = io.StringIO()
        with redirect_stdout(out):
            with run.step("outer", "Outer"):
                run.cmd([PY, "-c", "pass"], title="Inner", id="inner", parent="outer")
            self.assertEqual(run.next_n(), 2)   # the child took no number from the session counter
        got = markers(out.getvalue())
        self.assertEqual(got[1], {"ev": "step", "id": "inner", "n": 1, "title": "Inner", "parent": "outer"})

    def test_cmd_passes_label_to_the_row(self):
        events = []
        jr.subscribe(lambda kind, f: events.append((kind, f)))
        with redirect_stdout(io.StringIO()):
            run.cmd([PY, "-c", "pass"], title="Build everything", label="Build")
        starts = [f for kind, f in events if kind == "step_start"]
        self.assertEqual(starts[0]["label"], "Build")


class CmdParentPlainTest(PlainCase):
    def test_cmd_with_a_parent_in_a_plain_run_just_runs(self):
        r = run.cmd([PY, "-c", "pass"], title="t", parent="outer")
        self.assertTrue(r.ok)
        self.assertEqual(run.next_n(), 1)


if __name__ == "__main__":
    unittest.main()
