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
"""juneau_scripts.run.session: outcomes, done markers, SIGTERM, nesting, the bell and the plain summary."""

import io
import os
import signal
import sys
import tempfile
import threading
import unittest
from contextlib import redirect_stdout
from unittest import mock

import scripts_support
from scripts_support import FakeTty, isolate_run_env, jr, markers, reset_run_state

from juneau_scripts import notify, run, testing
from juneau_scripts.run import Result

NESTING = ("JUNEAU_RUN_ACTIVE", "JUNEAU_RUN_SESSION", "JUNEAU_PUSH_ACTIVE", "JUNEAU_RUN_N_BASE")


class SessionCase(unittest.TestCase):
    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)
        for key in ("SANDBOX_RUNTIME", "JUNEAU_SCRIPTS_QUIET"):
            os.environ.pop(key, None)
        # clean_run_env strips JUNEAU_RUN_LOG_DIR; point it (and HOME) at a temp dir so no test touches ~/.cache
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.log_dir = os.path.join(tmp.name, "logs")
        patcher = mock.patch.dict(os.environ, {"JUNEAU_RUN_LOG_DIR": self.log_dir, "HOME": tmp.name})
        patcher.start()
        self.addCleanup(patcher.stop)
        run._n = 0
        run._session = None
        self.bells = []
        self.sounds = []
        patcher = mock.patch.object(notify, "bell", lambda **kw: self.bells.append(kw))
        patcher.start()
        self.addCleanup(patcher.stop)
        patcher = mock.patch.object(notify, "sound", lambda ok, **kw: self.sounds.append(ok))
        patcher.start()
        self.addCleanup(patcher.stop)

    def markers_on(self):
        sys.modules["juneau_run"] = jr
        os.environ["RUN_MARKERS"] = "1"

    def go(self, body, **kw):
        """Run body(s) inside a session with exit=False; return (session, stdout)."""
        out = io.StringIO()
        kw.setdefault("exit", False)
        with redirect_stdout(out):
            with run.session("Demo", mode="push", project="demo", **kw) as s:
                body(s)
        return s, out.getvalue()


def ok(s):
    pass


def fails(s):
    s.fail("❌ It broke.", hint=["   try again", "   or not"])


def requires(s):
    s.require(Result(False, 4, err="first\nlast line\n"))


def requires_skip(s):
    s.require(Result(True, 3, status="skip"))


def boom(s):
    raise ValueError("boom")


def interrupt(s):
    raise KeyboardInterrupt


def term(s):
    os.kill(os.getpid(), signal.SIGTERM)


def exits(code):
    def body(s):
        sys.exit(code)
    return body


class OutcomeTest(SessionCase):
    def test_codes_and_done_statuses(self):
        for body, code, status in ((ok, 0, "ok"), (fails, 1, "fail"), (requires, 1, "fail"), (requires_skip, 0, "ok"),
                                   (boom, 1, "fail"), (interrupt, 130, "cancelled"), (term, 130, "cancelled"),
                                   (exits(0), 0, "ok"), (exits(None), 0, "ok"), (exits(5), 5, "fail"),
                                   (exits("bad thing"), 1, "fail")):
            with self.subTest(body=body.__name__, code=code):
                reset_run_state()
                self.markers_on()
                s, out = self.go(body)
                self.assertEqual(s.code, code)
                done = [m for m in markers(out) if m["ev"] == "done"]
                self.assertEqual(done, [{"ev": "done", "status": status}])

    def test_messages(self):
        _, out = self.go(fails)
        self.assertIn("❌ It broke.\n   try again\n   or not\n", out)
        run.cmd([sys.executable, "-c", "pass"], title="mvn verify")
        _, out = self.go(requires)
        self.assertIn("❌ ERROR: mvn verify failed (exit code 4)\n   last line\n", out)
        _, out = self.go(boom)
        self.assertIn("❌ ERROR: ValueError: boom", out)
        _, out = self.go(interrupt)
        self.assertIn("\n❌ Cancelled.", out)

    def test_sys_exit_with_a_message_reports_it(self):
        _, out = self.go(exits("bad thing"))
        self.assertIn("❌ ERROR: bad thing", out)
        _, out = self.go(exits(5))
        self.assertNotIn("❌ ERROR", out)

    def test_fail_needs_a_message(self):
        def body(s):
            with self.assertRaises(TypeError):
                s.fail()
        s, _ = self.go(body)
        self.assertEqual(s.code, 0)

    def test_commit_is_carried_into_done(self):
        self.markers_on()

        def body(s):
            s.commit = "abc1234"
            s.fail("❌ push failed")
        s, out = self.go(body)
        self.assertEqual([m for m in markers(out) if m["ev"] == "done"],
                         [{"ev": "done", "status": "fail", "commit": "abc1234"}])

    def test_run_marker_comes_first(self):
        self.markers_on()
        with mock.patch.object(run.git, "current_branch", return_value="main"), \
                mock.patch.object(run.git, "short_head", return_value="abc1234"):
            _, out = self.go(ok)
        self.assertEqual(markers(out)[0], {"ev": "run", "v": 1, "mode": "push", "project": "demo",
                                           "branch": "main", "head": "abc1234"})

    def test_git_is_not_queried_with_markers_off(self):
        with mock.patch.object(run.git, "current_branch") as branch:
            self.go(ok)
        branch.assert_not_called()

    def test_exit_true_raises_system_exit(self):
        with self.assertRaises(SystemExit) as cm, redirect_stdout(io.StringIO()):
            with run.session("Demo"):
                pass
        self.assertEqual(cm.exception.code, 0)
        with self.assertRaises(SystemExit) as cm, redirect_stdout(io.StringIO()):
            with run.session("Demo") as s:
                s.fail("❌ no")
        self.assertEqual(cm.exception.code, 1)

    def test_plain_summary_line(self):
        _, out = self.go(ok)
        self.assertRegex(out.splitlines()[-1], r"^✅ Demo: ok \(\d+\.\ds\)$")
        _, out = self.go(fails)
        self.assertRegex(out.splitlines()[-1], r"^❌ Demo: failed, exit code 1 \(\d+\.\ds\)$")
        _, out = self.go(interrupt)
        self.assertRegex(out.splitlines()[-1], r"^❌ Demo: cancelled \(\d+\.\ds\)$")

    def test_plain_output_has_no_markers_and_no_escape_codes(self):
        _, out = self.go(lambda s: run.say("hello"))
        self.assertTrue(out.startswith("hello\n"))
        self.assertNotIn("##run", out)
        self.assertNotIn("\x1b", out)

    def test_second_session_in_one_process_is_an_error(self):
        def body(s):
            with self.assertRaises(RuntimeError):
                with run.session("Inner"):
                    pass
        s, _ = self.go(body)
        self.assertEqual(s.code, 0)
        self.assertIsNone(run._session)


class SigtermTest(SessionCase):
    def test_handler_restored_on_every_path(self):
        for body in (ok, fails, boom, interrupt, term):
            with self.subTest(body=body.__name__):
                before = signal.getsignal(signal.SIGTERM)
                self.go(body)
                self.assertIs(signal.getsignal(signal.SIGTERM), before)

    def test_second_sigterm_during_cleanup_is_ignored(self):
        def body(s):
            os.kill(os.getpid(), signal.SIGTERM)
        seen = []
        real_done = run._done

        def done_with_second_term(status, commit, code):
            seen.append(signal.getsignal(signal.SIGTERM))
            os.kill(os.getpid(), signal.SIGTERM)    # ignored: the handler set SIG_IGN
            real_done(status, commit, code)
        with mock.patch.object(run, "_done", done_with_second_term):
            s, _ = self.go(body)
        self.assertEqual(s.code, 130)
        self.assertEqual(seen, [signal.SIG_IGN])

    def test_off_the_main_thread_no_handler_is_installed(self):
        before = signal.getsignal(signal.SIGTERM)
        result = {}

        def worker():
            s, _ = self.go(ok)
            result["code"] = s.code
        t = threading.Thread(target=worker)
        t.start()
        t.join()
        self.assertEqual(result, {"code": 0})
        self.assertIs(signal.getsignal(signal.SIGTERM), before)


class NestingTest(SessionCase):
    def test_each_variable_makes_the_session_nested(self):
        for key in NESTING:
            with self.subTest(key=key), mock.patch.dict(os.environ, {key: "3" if key.endswith("N_BASE") else "1"}):
                self.bells.clear()
                self.sounds.clear()
                s, out = self.go(ok, bell=True, sound=True)
                self.assertEqual((self.bells, self.sounds), ([], []))
                self.assertNotRegex(out, r"✅ Demo: ok")

    def test_zero_and_empty_are_not_nested(self):
        with mock.patch.dict(os.environ, {"JUNEAU_RUN_SESSION": "0", "JUNEAU_PUSH_ACTIVE": "0",
                                          "JUNEAU_RUN_N_BASE": ""}):
            self.go(ok, bell=True)
        self.assertEqual(len(self.bells), 1)

    def test_n_base_starts_the_step_counter(self):
        with mock.patch.dict(os.environ, {"JUNEAU_RUN_N_BASE": "4"}):
            def body(s):
                self.assertEqual(run._next_n(), 5)
            self.go(body)

    def test_environment_restored(self):
        self.markers_on()
        self.go(ok)
        self.assertNotIn("JUNEAU_RUN_ACTIVE", os.environ)
        self.assertNotIn("JUNEAU_RUN_SESSION", os.environ)


class BellTest(SessionCase):
    def test_bell_and_sound_on_every_outcome(self):
        for body, good in ((ok, True), (fails, False), (interrupt, False)):
            with self.subTest(body=body.__name__):
                self.bells.clear()
                self.sounds.clear()
                self.go(body, bell=True, sound=True, silence_env="MY_QUIET")
                self.assertEqual(self.bells, [{"silence_env": "MY_QUIET"}])
                self.assertEqual(self.sounds, [good])

    def test_off_by_default(self):
        self.go(ok)
        self.assertEqual((self.bells, self.sounds), ([], []))


class RealBellTest(SessionCase):
    """notify.bell itself under a session (not patched): silenced by each variable, rung on a fake TTY."""

    def setUp(self):
        super().setUp()
        mock.patch.stopall()
        self.addCleanup(mock.patch.stopall)

    def ring(self, env):
        tty = FakeTty(True)
        with mock.patch.dict(os.environ, env), mock.patch.object(sys, "stdout", tty):
            with run.session("Demo", bell=True, silence_env="MY_QUIET", exit=False):
                pass
        return tty.out.count("\a")

    def test_silencers(self):
        self.assertEqual(self.ring({}), 1)
        for key in ("SANDBOX_RUNTIME", "JUNEAU_SCRIPTS_QUIET", "MY_QUIET"):
            with self.subTest(key=key):
                self.assertEqual(self.ring({key: "1"}), 0)
                self.assertEqual(self.ring({key: "yes"}), 1)


class BoardTest(SessionCase):
    """[J0689] The console view: opened when the console is requested, nested children stay quiet."""

    def test_console_opens_and_closes_with_the_final_line(self):
        sys.modules["juneau_run"] = jr
        with mock.patch.dict(os.environ, {"JUNEAU_RUN_CONSOLE": "board"}), \
                mock.patch.object(jr, "session", wraps=jr.session) as opened:
            s, out = self.go(ok)
        opened.assert_called_once()
        self.assertEqual(opened.call_args.args[0], "Demo")
        self.assertEqual(s.code, 0)
        self.assertEqual(str(jr.full_log_dir()), self.log_dir)    # hermetic: never the real ~/.cache
        self.assertNotRegex(out, r"✅ Demo: ok \(")    # the console's final line replaces the plain summary

    def test_the_view_a_session_started_ends_with_the_session(self):
        sys.modules["juneau_run"] = jr
        with mock.patch.dict(os.environ, {"JUNEAU_RUN_CONSOLE": "board"}):
            self.go(ok)
            self.assertFalse(jr.session_open())
            self.go(ok)     # a second session in the same process starts clean
            self.assertFalse(jr.session_open())

    def test_a_view_the_session_did_not_start_is_left_open(self):
        sys.modules["juneau_run"] = jr
        with mock.patch.dict(os.environ, {"JUNEAU_RUN_CONSOLE": "board"}):
            with redirect_stdout(io.StringIO()):
                jr.session("outer")
            self.go(ok)
            self.assertTrue(jr.session_open())
            jr.end_session()
            jr.end_session()    # twice is fine
            self.assertFalse(jr.session_open())


class HeaderLinesTest(SessionCase):
    def test_lines_are_forwarded_to_the_console_session(self):
        seen = []
        sys.modules["juneau_run"] = jr
        with mock.patch.object(run, "console_requested", return_value=True), \
                mock.patch.object(jr, "session", side_effect=lambda *a, **k: seen.append((a, k))):
            with run.session("Push", header=("Juneau", "master"), lines=("Commit Message: m",), exit=False):
                pass
        self.assertEqual(seen[0][0][:2], ("Push", ("Juneau", "master")))
        self.assertEqual(seen[0][1]["lines"], ("Commit Message: m",))


if __name__ == "__main__":
    unittest.main()
