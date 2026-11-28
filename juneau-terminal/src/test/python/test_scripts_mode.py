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
"""juneau_terminal_scripts.mode: the resolution table, argument rewriting, the raw scrub, engine choice and the apply() flow, with the probes injected (no tmux)."""

import argparse
import os
import sys
import unittest
from unittest import mock

from scripts_support import PYTHON_SRC  # noqa: F401  (puts src/main/python on sys.path)

from juneau_terminal_scripts import mode

P = mode.Probes


def clean_environ(case):
    """Run the case with none of the run-session variables of the process that started the tests (a push runs its own tests inside a run)."""
    patch = mock.patch.dict(os.environ)
    patch.start()
    case.addCleanup(patch.stop)
    for k in [k for k in os.environ if k.startswith(("JUNEAU_TERMINAL_", "TMUX_PANE_RUN_")) or k in ("TMUX", "TMUX_PANE")]:
        del os.environ[k]


class IsolatedCase(unittest.TestCase):

    def setUp(self):
        clean_environ(self)
        mode._warned.clear()


def probes(tmux=True, tty_out=True, tty_in=True, inside=False, toolkit=True):
    return P(toolkit, tmux, tty_out, tty_in, inside)


class ResolveTest(IsolatedCase):

    def resolve(self, requested, p, env=None, oneshot=False):
        return mode.resolve(requested, p, env or {}, oneshot)

    def test_auto_without_toolkit_is_silently_raw(self):
        self.assertEqual(self.resolve("auto", probes(toolkit=False)), ("raw", None))

    def test_auto_split_with_tmux_and_a_tty(self):
        self.assertEqual(self.resolve("auto", probes()), ("split", None))

    def test_auto_split_inside_tmux_without_a_tty(self):
        self.assertEqual(self.resolve("auto", probes(tty_out=False, tty_in=False, inside=True)), ("split", None))

    def test_auto_dash_without_tmux(self):
        self.assertEqual(self.resolve("auto", probes(tmux=False)), ("dash", None))

    def test_auto_dash_without_a_tty_outside_tmux(self):
        self.assertEqual(self.resolve("auto", probes(tty_out=False)), ("dash", None))

    def test_auto_dash_when_stdin_is_not_a_tty_outside_tmux(self):
        self.assertEqual(self.resolve("auto", probes(tty_in=False)), ("dash", None))

    def test_oneshot_auto_is_dash_but_explicit_split_is_honored(self):
        self.assertEqual(self.resolve("auto", probes(), oneshot=True), ("dash", None))
        self.assertEqual(self.resolve("split", probes(), oneshot=True), ("split", None))

    def test_raw_always_runs(self):
        self.assertEqual(self.resolve("raw", probes(tmux=False, tty_out=False)), ("raw", None))

    def test_explicit_dash_needs_the_toolkit(self):
        m, w = self.resolve("dash", probes(toolkit=False))
        self.assertEqual(m, "raw")
        self.assertIn("--mode dash", w)

    def test_explicit_split_falls_back_to_raw_not_dash(self):
        for p, word in ((probes(tmux=False), "tmux"), (probes(tty_out=False), "attach"), (probes(toolkit=False), "juneau-terminal")):
            with self.subTest(word=word):
                m, w = self.resolve("split", p)
                self.assertEqual(m, "raw")
                self.assertIn(word, w)

    def test_nested_run_never_splits(self):
        for k in mode.NESTED_ENV + mode.WRAPPED_ENV:
            with self.subTest(k=k):
                self.assertEqual(self.resolve("auto", probes(), {k: "x"})[0], "dash")
                self.assertEqual(self.resolve("split", probes(), {k: "x"})[0], "dash")

    def test_nested_run_keeps_raw(self):
        self.assertEqual(self.resolve("raw", probes(), {"JUNEAU_TERMINAL_SESSION": "x"})[0], "raw")

    def test_requested_precedence_flag_then_env_then_auto(self):
        self.assertEqual(mode.requested_mode("raw", {mode.MODE_ENV: "dash"}), "raw")
        self.assertEqual(mode.requested_mode(None, {mode.MODE_ENV: "dash"}), "dash")
        self.assertEqual(mode.requested_mode(None, {}), "auto")
        with self.assertRaises(ValueError):
            mode.requested_mode(None, {mode.MODE_ENV: "bogus"})


class KeepTest(IsolatedCase):
    def test_requested_keep_precedence_flag_then_env_then_fail(self):
        self.assertEqual(mode.requested_keep("always", {mode.KEEP_ENV: "never"}), "always")
        self.assertEqual(mode.requested_keep(None, {mode.KEEP_ENV: "never"}), "never")
        self.assertEqual(mode.requested_keep(None, {}), "fail")

    def test_a_bad_environment_value_is_an_error(self):
        with self.assertRaises(ValueError) as e:
            mode.requested_keep(None, {mode.KEEP_ENV: "bogus"})
        self.assertIn(mode.KEEP_ENV, str(e.exception))

    def test_the_flag_takes_only_the_three_values(self):
        import argparse
        p = argparse.ArgumentParser()
        mode.add_argument(p)
        self.assertEqual(p.parse_args(["--keep", "never"]).keep, "never")
        self.assertIsNone(p.parse_args([]).keep)
        with self.assertRaises(SystemExit):
            p.parse_args(["--keep", "sometimes"])

    def test_the_launch_passes_the_keep_value_to_watch_run(self):
        args = mode._watch_args("/w", "t", False, ["x"], "always")
        self.assertEqual(args[args.index("-k") + 1], "always")
        self.assertEqual(mode._watch_args("/w", "t", False, ["x"])[4:6], ["-k", "fail"])


class RewriteArgvTest(IsolatedCase):

    def test_absent_appends_mode_dash(self):
        self.assertEqual(mode.rewrite_argv(["a", "--x", "1"]), ["a", "--x", "1", "--mode", "dash"])

    def test_replaces_both_spellings_and_keeps_order(self):
        self.assertEqual(mode.rewrite_argv(["--mode", "split", "m"]), ["--mode", "dash", "m"])
        self.assertEqual(mode.rewrite_argv(["m", "--mode=auto", "--console", "full"]), ["m", "--mode=dash", "--console", "full"])

    def test_absent_goes_before_the_separator(self):
        self.assertEqual(mode.rewrite_argv(["a", "--", "--mode", "x"]), ["a", "--mode", "dash", "--", "--mode", "x"])

    def test_values_with_spaces_survive(self):
        self.assertEqual(mode.rewrite_argv(["a b", "--mode", "split"]), ["a b", "--mode", "dash"])


class PrepareRawTest(IsolatedCase):

    def test_scrubs_session_variables_and_forces_full(self):
        env = {k: "x" for k in mode.RAW_SCRUB}
        env[mode.CONSOLE_ENV] = "board"
        args = argparse.Namespace(console="none")
        with mock.patch.dict(os.environ, env):
            mode.prepare_raw(args)
            self.assertEqual([k for k in mode.RAW_SCRUB if k in os.environ], [])
            self.assertEqual(os.environ[mode.CONSOLE_ENV], "full")
        self.assertEqual(args.console, "full")


class EngineChoiceTest(IsolatedCase):

    def test_override_wins(self):
        self.assertEqual(mode.choose_engine({mode.ENGINE_ENV: "shim"}, sandboxed=lambda: False), "shim")
        self.assertEqual(mode.choose_engine({mode.ENGINE_ENV: "direct"}, sandboxed=lambda: True), "direct")

    def test_auto_follows_the_functional_probe(self):
        self.assertEqual(mode.choose_engine({}, sandboxed=lambda: True), "shim")
        self.assertEqual(mode.choose_engine({}, sandboxed=lambda: False), "direct")

    def test_no_environment_variable_is_trusted_as_a_marker(self):
        env = {"SANDBOX_RUNTIME": "1", "CLAUDE_CODE_SESSION_ATTENDED": "1"}
        self.assertEqual(mode.choose_engine(env, sandboxed=lambda: False), "direct")

    def test_bad_override_is_an_error(self):
        with self.assertRaises(ValueError):
            mode.choose_engine({mode.ENGINE_ENV: "x"})


class ApplyTest(IsolatedCase):

    def args(self, m=None):
        return argparse.Namespace(mode=m, console=None)

    def test_raw_scrubs_and_does_not_finish(self):
        a = self.args("raw")
        r = mode.apply(a, [], script="x/push.py", probes=probes())
        self.assertEqual(r, mode.Applied("raw", False, 0))
        self.assertEqual(a.console, "full")
        self.assertEqual(os.environ[mode.MODE_ENV], "raw")

    def test_dash_exports_dash(self):
        self.assertEqual(mode.apply(self.args("dash"), [], script="x/push.py", probes=probes()).mode, "dash")
        self.assertEqual(os.environ[mode.MODE_ENV], "dash")

    def test_split_launches_and_exports_dash_for_children(self):
        with mock.patch.object(mode, "launch_split", return_value=(True, 7)) as ls:
            r = mode.apply(self.args("split"), ["m"], script="x/push.py", probes=probes(inside=True))
        self.assertEqual(r, mode.Applied("split", True, 7))
        self.assertEqual(ls.call_args.args[:2], ("x/push.py", ["m"]))
        self.assertTrue(ls.call_args.kwargs["inside"])
        self.assertEqual(os.environ[mode.MODE_ENV], "dash")

    def test_failed_new_session_continues_as_dash(self):
        with mock.patch.object(mode, "launch_split", return_value=(False, 0)):
            r = mode.apply(self.args("split"), [], script="x/push.py", probes=probes())
        self.assertEqual(r, mode.Applied("dash", False, 0))

    def test_bad_environment_value_exits_2(self):
        os.environ[mode.MODE_ENV] = "bogus"
        self.assertEqual(mode.apply(self.args(), [], script="x/push.py", probes=probes()), mode.Applied("auto", True, 2))

    def test_fallback_warning_is_printed_once(self):
        with mock.patch("sys.stderr") as err:
            for _ in range(2):
                mode.apply(self.args("split"), [], script="x/push.py", probes=probes(tmux=False))
        text = "".join(c.args[0] for c in err.write.call_args_list)
        self.assertEqual(text.count("push.py: --mode split: tmux not found"), 1)

    def test_add_argument_rejects_a_bad_value(self):
        p = argparse.ArgumentParser()
        mode.add_argument(p)
        with mock.patch("sys.stderr"), self.assertRaises(SystemExit) as e:
            p.parse_args(["--mode", "bogus"])
        self.assertEqual(e.exception.code, 2)


if __name__ == "__main__":
    unittest.main()
