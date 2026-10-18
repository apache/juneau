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
"""juneau_scripts.checks and the runners that drive it: run.checks, Session.checks and Registry.checks."""

import argparse
import io
import os
import sys
import unittest
from contextlib import redirect_stdout
from unittest import mock

import scripts_support
from scripts_support import isolate_run_env, jr, markers, reset_run_state
from test_scripts_session import SessionCase

from juneau_scripts import checks, run, steps, testing
from juneau_scripts.checks import Check, Verdict

IDENTITY = Check("identity", lambda: Verdict("fail", "git user.email is not set",
                                             ("set it:", "  git config user.email a@b")))
UPSTREAM = Check("upstream", lambda: True)
UNREVIEWED = Check("unreviewed", lambda: Verdict("fail", "2 unreviewed paths", ("a.txt", "b.txt")))


class ConsoleCase(unittest.TestCase):
    """juneau_run loaded with a plain-screen console subscribed, and every bus event recorded in self.seen."""

    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        sys.modules["juneau_run"] = jr
        os.environ.pop("RUN_MARKERS", None)
        run._n = 0
        self.seen = []
        jr.subscribe(lambda kind, fields: self.seen.append((kind, dict(fields))))
        self.out = io.StringIO()
        jr._console = jr.subscribe(jr.Console(jr.Sinks(console="condensed"), stream=self.out))

    def of(self, kind):
        return [f for k, f in self.seen if k == kind]

    def screen(self):
        jr._console.close()
        return self.out.getvalue()


class VerdictTest(unittest.TestCase):
    def test_status_is_validated_and_a_str_hint_is_one_line(self):
        with self.assertRaises(ValueError):
            Verdict("maybe")
        self.assertEqual(Verdict("warn", "x", "one line").hint, ("one line",))

    def test_evaluate_maps_bools_and_none(self):
        self.assertEqual(checks.evaluate(Check("a", lambda: None)), checks.PASS)
        self.assertEqual(checks.evaluate(Check("a", lambda: True)), checks.PASS)
        self.assertEqual(checks.evaluate(Check("a", lambda: False)).status, "fail")
        self.assertIn("not a Verdict", checks.evaluate(Check("a", lambda: "yes")).reason)


class ChecksTest(ConsoleCase):
    def test_one_cell_per_check(self):
        run.checks("pre", "Prechecks", [IDENTITY, UPSTREAM, UNREVIEWED])
        self.assertEqual(self.of("units"), [{"step": "pre", "total": 3, "kind": "checks"}])
        self.assertEqual([u["outcome"] for u in self.of("unit")], ["fail", "pass", "fail"])
        self.assertIn("F#F  3/3", self.screen())

    def test_progress_text_is_check_name(self):
        run.checks("pre", "Prechecks", [IDENTITY, UPSTREAM, UNREVIEWED])
        self.assertEqual([p["text"] for p in self.of("step_progress")], ["identity", "upstream", "unreviewed"])

    def test_fail_sets_reason_and_hint(self):
        result = run.checks("pre", "Prechecks", [UPSTREAM, IDENTITY])
        self.assertFalse(result.ok)
        row = jr._console.board.rows["pre"]
        self.assertEqual((row.status, row.reason, row.reason_level), ("fail", "git user.email is not set", "error"))
        self.assertEqual(self.of("hint"), [{"step": "pre", "lines": ["identity: set it:",
                                                                      "  git config user.email a@b"]}])
        screen = self.screen()
        self.assertIn("✗ Prechecks", screen)
        self.assertIn("    identity: set it:\n      git config user.email a@b\n", screen)

    def test_all_checks_run_reason_plus_n(self):
        ran = []
        listed = [Check(n, lambda n=n: ran.append(n) or Verdict("fail", f"{n} broke")) for n in ("a", "b", "c")]
        result = run.checks("pre", "Prechecks", listed)
        self.assertEqual(ran, ["a", "b", "c"])
        self.assertEqual(list(result.verdicts), ["a", "b", "c"])
        self.assertEqual(jr._console.board.rows["pre"].reason, "a broke +2")

    def test_refused_is_true_for_a_fail_and_false_for_pass_and_plain_warn(self):
        self.assertTrue(run.checks("pre", "Prechecks", [UNREVIEWED]).refused)
        self.assertFalse(run.checks("pre", "Prechecks", [Check("a", lambda: True)]).refused)
        self.assertFalse(run.checks("pre", "Prechecks", [Check("a", lambda: Verdict("warn", "meh"))], dry_run=True).refused)

    def test_raise_fails_closed(self):
        result = run.checks("pre", "Prechecks", [Check("java", lambda: 1 / 0)])
        self.assertFalse(result.ok)
        self.assertEqual(result.verdicts["java"].reason, "could not check java: division by zero")

    def test_dry_run_fail_becomes_warn_with_hints(self):
        result = run.checks("pre", "Prechecks", [UPSTREAM, UNREVIEWED], dry_run=True)
        self.assertTrue(result.ok)
        self.assertTrue(result.refused)
        self.assertEqual(result.verdicts["unreviewed"], Verdict("warn", "would refuse: 2 unreviewed paths",
                                                               ("a.txt", "b.txt")))
        self.assertEqual([u["outcome"] for u in self.of("unit")], ["pass", "warn"])
        row = jr._console.board.rows["pre"]
        self.assertEqual((row.status, jr.row_state(row)), ("ok", "warn"))
        screen = self.screen()
        self.assertIn("⚠ Prechecks", screen)
        self.assertIn("#!  2/2  would refuse: 2 unreviewed paths", screen)
        self.assertIn("    unreviewed: a.txt\n    b.txt\n", screen)

    def test_tick_called_between_checks(self):
        calls = []
        with mock.patch.object(jr, "tick", side_effect=lambda: calls.append("tick")):
            listed = [Check(n, lambda n=n: calls.append(n)) for n in ("a", "b")]
            run.checks("pre", "Prechecks", listed)
        self.assertEqual(calls, ["tick", "a", "tick", "tick", "b", "tick"])


class MarkersTest(unittest.TestCase):
    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        sys.modules["juneau_run"] = jr
        os.environ["RUN_MARKERS"] = "1"
        run._n = 0

    def test_markers_one_step_with_notes(self):
        out = io.StringIO()
        with redirect_stdout(out):
            run.checks("pre", "Prechecks", [IDENTITY, UPSTREAM, UNREVIEWED])
        found = markers(out.getvalue())
        self.assertEqual([m["ev"] for m in found], ["step", "note", "note", "end"])
        self.assertEqual([m["text"] for m in found if m["ev"] == "note"],
                         ["identity: git user.email is not set", "unreviewed: 2 unreviewed paths"])
        self.assertEqual({m.get("level") for m in found if m["ev"] == "note"}, {"error"})


class PlainRunTest(unittest.TestCase):
    def setUp(self):
        isolate_run_env(self)
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)
        patcher = mock.patch.dict(sys.modules)
        patcher.start()
        self.addCleanup(patcher.stop)
        sys.modules.pop("juneau_run", None)
        run._n = 0

    def test_plain_run_prints_verdicts(self):
        out = io.StringIO()
        warned = Check("staged", lambda: Verdict("warn", "nothing staged"))
        with redirect_stdout(out):
            result = run.checks("pre", "Prechecks", [UPSTREAM, warned, IDENTITY])
        self.assertFalse(result.ok)
        self.assertEqual(out.getvalue(), "✔ upstream\n⚠ staged: nothing staged\n✗ identity: git user.email is not set\n"
                                         "  identity: set it:\n    git config user.email a@b\n")


class SessionChecksTest(SessionCase):
    def test_session_checks_ends_session_silently(self):
        ran = []

        def body(s):
            s.checks("pre", "Prechecks", [IDENTITY])
            ran.append("after")
        s, out = self.go(body)
        self.assertEqual((s.code, ran), (1, []))
        self.assertNotIn("ERROR", out)
        self.assertIn("✗ identity: git user.email is not set", out)

    def test_a_passing_session_checks_returns_the_result(self):
        def body(s):
            body.result = s.checks("pre", "Prechecks", [UPSTREAM])
        s, _ = self.go(body)
        self.assertEqual((s.code, body.result.ok), (0, True))


class RegistryChecksTest(ConsoleCase):
    def test_registry_checks_step(self):
        reg = steps.Registry()
        reg.checks("prechecks", "Prechecks", lambda ctx: [UPSTREAM, ctx])
        reg.step("build", "Build")(lambda ctx: None)
        parser = argparse.ArgumentParser(prog="r.py")
        reg.add_arguments(parser)
        result = reg.run(parser.parse_args([]), ctx=IDENTITY)
        self.assertEqual((result.ok, result.code), (False, 1))
        self.assertEqual([u["outcome"] for u in self.of("unit")], ["pass", "fail"])
        self.assertNotIn("build", [f["id"] for f in self.of("step_start")])
        row = jr._console.board.rows["prechecks"]
        self.assertEqual((row.status, row.reason), ("fail", "git user.email is not set"))
        self.assertIn("To resume: r.py --start-step prechecks", self.screen())


if __name__ == "__main__":
    unittest.main()
