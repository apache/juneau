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
"""CLI: run as a subprocess; exit codes pass through and run/done are emitted only by the outermost process."""

import io
import json
import os
import subprocess
import sys
import unittest
from unittest import mock

from support import SCRIPT, jr


def cli(*args, env=None):
    full_env = {k: v for k, v in os.environ.items() if not k.startswith(("RUN_", "JUNEAU_RUN_")) or k == "JUNEAU_RUN_LOG_DIR"}
    full_env["JUNEAU_RUN_HISTORY"] = "0"
    full_env.update(env or {})
    return subprocess.run([sys.executable, str(SCRIPT), *args], capture_output=True, text=True, env=full_env)


def evs(stdout):
    return [json.loads(line[len(jr.PREFIX):])["ev"] for line in stdout.splitlines() if line.startswith(jr.PREFIX)]


class CliTest(unittest.TestCase):
    def test_exit_code_passes_through_in_pass_through_mode(self):
        r = cli("generic", "--", sys.executable, "-c", "print('hi'); import sys; sys.exit(3)")
        self.assertEqual((r.returncode, r.stdout), (3, "hi\n"))

    def test_with_markers_the_outermost_cli_emits_run_step_end_done(self):
        r = cli("--step", "t", "generic", "--", sys.executable, "-c", "pass", env={"RUN_MARKERS": "1"})
        self.assertEqual(evs(r.stdout), ["run", "step", "end", "done"])
        self.assertIn('"mode":"test"', r.stdout)

    def test_failure_ends_the_run_with_done_fail(self):
        r = cli("generic", "--", sys.executable, "-c", "import sys; sys.exit(2)", env={"RUN_MARKERS": "1"})
        self.assertEqual(r.returncode, 2)
        self.assertIn('{"ev":"done","status":"fail"}', r.stdout)

    def test_nested_cli_emits_steps_but_no_run_or_done(self):
        r = cli("generic", "--", sys.executable, "-c", "pass", env={"RUN_MARKERS": "1", jr.ACTIVE_ENV: "1"})
        self.assertEqual(evs(r.stdout), ["step", "end"])

    def test_condensed_console_prints_only_the_parser_lines_unlike_the_full_console(self):
        code = "print('noise')"
        full = cli("--console", "full", "generic", "--", sys.executable, "-c", code)
        condensed = cli("--console", "condensed", "generic", "--", sys.executable, "-c", code)
        self.assertEqual(full.stdout, "noise\n")
        self.assertEqual(condensed.stdout, "")   # the generic parser condenses a passing run to nothing

    def test_condensed_console_on_failure_shows_only_the_generic_tail(self):
        code = "[print(i) for i in range(100)]; import sys; sys.exit(1)"
        full = cli("--console", "full", "generic", "--", sys.executable, "-c", code)
        condensed = cli("--console", "condensed", "generic", "--", sys.executable, "-c", code)
        self.assertEqual(len(full.stdout.splitlines()), 100)
        self.assertEqual(condensed.stdout.splitlines(), [str(i) for i in range(60, 100)])

    def test_no_command_is_a_usage_error(self):
        self.assertEqual(cli("generic").returncode, 2)


class ParserChoicesTest(unittest.TestCase):
    def test_help_lists_the_parsers(self):
        r = cli("--help")
        self.assertEqual(r.returncode, 0, r.stderr)
        for name in ("generic", "maven", "npm"):
            self.assertIn(name, r.stdout)

    def test_choices_come_from_parser_names(self):
        with mock.patch.object(jr, "_toolkit_package", return_value=mock.Mock(REGISTRY={"zz": "zz"})):
            self.assertEqual(jr._parser_names(), sorted([*jr.PARSERS, "zz"]))
        with mock.patch.object(jr, "_toolkit_package", side_effect=ImportError("gone")):
            self.assertEqual(jr._parser_names(), sorted(jr.PARSERS))

    def test_an_unknown_parser_is_a_usage_error(self):
        r = cli("nope", "--", sys.executable, "-c", "pass")
        self.assertEqual(r.returncode, 2)
        self.assertIn("invalid choice: 'nope'", r.stderr)

    def test_help_lists_registered_parsers(self):
        self.assertIn("bazel", cli("--help").stdout)


# A stand-in for `npx playwright test`: records the reporter it was given, then prints what the juneau-run reporter
# prints for one file of two tests (the line reporter's progress when it wasn't given), slowly enough for the live view
# to draw the file's line.
FAKE_PLAYWRIGHT = """
import json, os, sys, time
reporter = os.environ.get("PW_TEST_REPORTER", "")
open(sys.argv[1], "w").write(reporter + "|" + os.environ.get("PLAYWRIGHT_HTML_OPEN", ""))
def say(**m):
    if reporter.endswith("juneau_run_playwright.cjs"):
        print("##playwright " + json.dumps(m), flush=True)
    elif m["ev"] == "test":
        say.n = getattr(say, "n", 0) + 1
        print(f"[{say.n}/2] [chromium] > tests/a.spec.ts:1:1 > test", flush=True)
say(ev="hello")
say(ev="begin", f={"tests/a.spec.ts": 2})
say(ev="start", f="tests/a.spec.ts")
say(ev="test", f="tests/a.spec.ts", o="pass")
time.sleep(0.6)
say(ev="test", f="tests/a.spec.ts", o="pass")
print("  2 passed (1.0s)", flush=True)
"""


def cli_on_terminal(*args, env=None):
    """cli() with stdout and stderr on a fresh PTY: (exit code, everything that reached the screen)."""
    import pty
    import select
    full_env = {k: v for k, v in os.environ.items() if not k.startswith(("RUN_", "JUNEAU_RUN_")) or k == "JUNEAU_RUN_LOG_DIR"}
    full_env.update({"JUNEAU_RUN_HISTORY": "0", "TERM": "xterm", **(env or {})})
    master, slave = pty.openpty()
    try:
        proc = subprocess.Popen([sys.executable, str(SCRIPT), *args], stdin=subprocess.DEVNULL, stdout=slave,
                                stderr=slave, env=full_env)
    finally:
        os.close(slave)
    screen = b""
    try:
        while True:
            if select.select([master], [], [], 0.1)[0]:
                try:
                    chunk = os.read(master, 4096)
                except OSError:     # EIO: the child has closed the slave side
                    break
                if not chunk:
                    break
                screen += chunk
            elif proc.poll() is not None:
                break
    finally:
        os.close(master)
    return proc.wait(timeout=30), screen.decode("utf-8", errors="replace")


@unittest.skipUnless(os.name == "posix", "needs a PTY")
class CondensedPlaywrightTest(unittest.TestCase):
    """The CLI's condensed console draws the live view on a terminal, so Playwright gets one line per test file."""

    def setUp(self):
        import tempfile
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.seen = os.path.join(self.tmp.name, "seen")
        self.log_dir = {"JUNEAU_RUN_LOG_DIR": self.tmp.name}

    def run_playwright(self, *extra, terminal=True, env=None):
        args = ("--console", "condensed", "--step", "e2e", "--title", "E2E", "playwright", "--",
                sys.executable, "-c", FAKE_PLAYWRIGHT, self.seen, *extra)
        if terminal:
            return cli_on_terminal(*args, env={**self.log_dir, **(env or {})})
        r = cli(*args, env={**self.log_dir, **(env or {})})
        return r.returncode, r.stdout

    def reporter_given(self):
        with open(self.seen, encoding="utf-8") as f:
            return f.read().split("|")

    def test_on_a_terminal_the_reporter_is_injected_and_each_file_gets_a_line_under_the_row(self):
        rc, screen = self.run_playwright()
        self.assertEqual(rc, 0, screen)
        reporter, html_open = self.reporter_given()
        self.assertTrue(reporter.endswith("juneau_run_playwright.cjs"), reporter)
        self.assertEqual(html_open, "never")
        self.assertRegex(screen, r"tests/a\.spec\.ts\s+\S*•")    # the file's line with its first dot
        self.assertIn("E2E", screen)
        self.assertNotIn("##playwright", screen)                  # the markers go to the full log only

    def test_on_a_terminal_the_opt_outs_still_leave_the_reporter_out(self):
        for extra, env in ((("--reporter=line",), None), ((), {"JUNEAU_RUN_PLAYWRIGHT_REPORTER": "0"}),
                           ((), {"PW_TEST_REPORTER": "mine"})):
            with self.subTest(extra=extra, env=env):
                rc, screen = self.run_playwright(*extra, env=env)
                self.assertEqual(rc, 0, screen)
                self.assertEqual(self.reporter_given()[0], (env or {}).get("PW_TEST_REPORTER", ""))
                self.assertNotIn("tests/a.spec.ts", screen)
                self.assertNotIn("##playwright", screen)

    def test_off_a_terminal_condensed_output_is_unchanged(self):
        rc, out = self.run_playwright(terminal=False)
        self.assertEqual((rc, out), (0, "  2 passed (1.0s)\n"))
        self.assertEqual(self.reporter_given(), ["", ""])

    def test_the_view_needs_condensed_a_redrawing_terminal_no_markers_and_no_enclosing_session(self):
        class TTY:
            def isatty(self):
                return True
        cases = [("condensed", {}, True), ("full", {}, False), ("none", {}, False),
                 ("condensed", {"RUN_MARKERS": "1"}, False), ("condensed", {"TERM": "dumb"}, False),
                 ("condensed", {"JUNEAU_RUN_LIVE": "0"}, False), ("condensed", {jr.SESSION_ENV: "1"}, False)]
        for console, env, expected in cases:
            with self.subTest(console=console, env=env), mock.patch.dict(os.environ, {**self.log_dir, **env}), \
                    mock.patch.object(jr.sys, "stdout", TTY()):
                for key in {"RUN_MARKERS", "TERM", "JUNEAU_RUN_LIVE", jr.SESSION_ENV} - set(env):
                    os.environ.pop(key, None)
                self.assertEqual(jr._cli_view(jr.Sinks(console, full_log=self.seen)), expected)
        with mock.patch.object(jr.sys, "stdout", io.StringIO()):
            self.assertFalse(jr._cli_view(jr.Sinks("condensed", full_log=self.seen)))

    def test_with_markers_on_a_terminal_no_view_is_drawn(self):
        rc, screen = self.run_playwright(env={"RUN_MARKERS": "1"})
        self.assertEqual(rc, 0, screen)
        self.assertIn('##run {"ev":"done","status":"ok"}', screen)
        self.assertNotIn("tail -f", screen)

if __name__ == "__main__":
    unittest.main()
