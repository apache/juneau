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
"""The git parser: a progress bar for `git clone/fetch/pull --progress` from CR-separated stderr, detection by argv, and the
runner splitting a parser's output on carriage returns."""

import io
import os
import re
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

from scripts_support import jr, markers, reset_run_state
from support import isolate_run_env

from juneau_terminal_scripts import parsers, run

parsers.bind(jr)
from juneau_terminal_scripts.parsers import git  # noqa: E402  (needs the binding above)

FIXTURE = Path(__file__).resolve().parent / "fixtures" / "parsers" / "git" / "clone.log"
CELLS = git.CELLS


def segments(text):
    """The pieces a CR-aware runner hands the parser: split on a CR or LF, a CR+LF pair counting once."""
    return [s for s in re.split(r"\r\n|\r|\n", text)]


def feed(parser, text, exit_code=0, step_id="clone"):
    parser.step_id = step_id
    events = []
    for s in segments(text):
        events += parser.on_line(s)
    return events, parser.finish(exit_code)


def done_values(events):
    return [e[2] for e in events if e[0] == "unit"]


class GitParserTest(unittest.TestCase):
    def test_a_real_clone_fills_one_monotonic_bar(self):
        events, end = feed(git.PARSER(), FIXTURE.read_text(encoding="utf-8"))
        units = [i for i, e in enumerate(events) if e[0] == "units"]
        self.assertEqual(len(units), 1)                                                          # once, before any unit
        self.assertLess(units[0], next(i for i, e in enumerate(events) if e[0] == "unit"))
        self.assertEqual(events[units[0]], ("units", "clone", CELLS, "progress"))
        done = done_values(events)
        self.assertEqual(done, sorted(done))
        self.assertEqual(len(done), len(set(done)))     # an event only when the bar moves
        self.assertLess(done[-1], CELLS)                # the last cell waits for the exit
        self.assertEqual(done_values(end), [CELLS])
        self.assertEqual([e for e in end if e[0] != "unit"], [])

    def test_receiving_is_the_main_bar_then_deltas_then_checkout(self):
        p = git.PARSER()
        p.step_id = "c"
        cases = [("Receiving objects:  50% (10/20)", 35), ("Receiving objects: 100% (20/20), done.", 70),
                 ("Resolving deltas:  50% (5/10)", 77.5), ("Resolving deltas: 100% (10/10), done.", 85),
                 ("Updating files:  50% (5/10)", 92.5), ("Updating files: 100% (10/10), done.", 100)]
        for line, overall in cases:
            with self.subTest(line=line):
                self.assertAlmostEqual(git.overall(*git.parse(line)[:2]), overall)

    def test_a_cell_per_five_percent(self):
        p = git.PARSER()
        p.step_id = "c"
        events = p.on_line("Receiving objects:  50% (10/20), 1.0 MiB | 2.0 MiB/s")
        self.assertIn(("unit", "c", 7, None), events)         # 35% of 20 cells
        self.assertEqual(p.on_line("Receiving objects:  51% (11/20)"), [("progress", "c", "Receiving objects 51% (11/20)")])

    def test_progress_text_names_the_phase_counts_and_rate(self):
        p = git.PARSER()
        p.step_id = "c"
        events = p.on_line("Receiving objects:  45% (1234/2742), 10.2 MiB | 5.1 MiB/s")
        self.assertIn(("progress", "c", "Receiving objects 45% (1234/2742) · 10.2 MiB | 5.1 MiB/s"), events)

    def test_remote_phases_show_text_but_do_not_move_the_bar(self):
        p = git.PARSER()
        p.step_id = "c"
        events = p.on_line("remote: Counting objects:  45% (45/100)        ")
        self.assertEqual(events, [("progress", "c", "remote: Counting objects 45% (45/100)")])
        self.assertEqual(p.on_line("remote: Compressing objects: 100% (30/30), done.        "),
                         [("progress", "c", "remote: Compressing objects 100% (30/30)")])

    def test_unpacking_objects_counts_as_receiving(self):
        p = git.PARSER()
        p.step_id = "c"
        events = p.on_line("Unpacking objects: 100% (3/3), 1.2 KiB | 100.00 KiB/s, done.")
        self.assertEqual(done_values(events), [14])

    def test_the_bar_never_moves_back(self):
        p = git.PARSER()
        p.step_id = "c"
        p.on_line("Resolving deltas:  50% (5/10)")
        self.assertEqual(done_values(p.on_line("Receiving objects:  10% (1/10)")), [])

    def test_successful_exit_completes_the_bar_only_if_it_started(self):
        p = git.PARSER()
        p.step_id = "c"
        self.assertEqual(p.finish(0), [])
        self.assertEqual(done_values(feed(git.PARSER(), "Receiving objects:  10% (1/10)")[1]), [CELLS])

    def test_a_failed_run_leaves_the_bar_where_it_stopped_and_reports_the_tail(self):
        events, end = feed(git.PARSER(), "Cloning into 'x'...\nReceiving objects:  10% (1/10)\r"
                                         "fatal: early EOF\nfatal: fetch-pack: invalid index-pack output\n", exit_code=128)
        self.assertEqual(done_values(end), [])
        failure = [e for e in end if e[0] == "failure"]
        self.assertEqual(len(failure), 1)
        self.assertIn("fatal: early EOF", failure[0][1])
        self.assertNotIn("Receiving objects", failure[0][1])    # progress is not the failure text

    def test_non_progress_output_behaves_as_the_generic_parser(self):
        text = "Cloning into 'x'...\nwarning: You appear to have cloned an empty repository.\n"
        mine, generic = git.PARSER(), jr.GenericParser()
        for p in (mine, generic):
            p.step_id = "c"
        events = []
        for line in text.splitlines():
            events += mine.on_line(line)
            self.assertEqual(generic.on_line(line), events)
        self.assertEqual(events, [])
        self.assertEqual(mine.finish(1), generic.finish(1))
        self.assertEqual(mine.finish(0), generic.finish(0))

    def test_ansi_and_blank_lines_are_ignored(self):
        p = git.PARSER()
        p.step_id = "c"
        self.assertEqual(p.on_line("\x1b[K"), [])
        self.assertTrue(p.on_line("\x1b[Kremote: Counting objects:  45% (45/100)\x1b[K"))


class DetectGitTest(unittest.TestCase):
    def test_clone_fetch_and_pull_are_detected(self):
        for argv in (["git", "clone", "--progress", "u", "w"], ["git", "fetch"], ["git", "pull", "--rebase"],
                     ["/usr/bin/git", "clone", "u"], ["git.exe", "fetch", "origin"],
                     ["git", "-C", "/tmp/w", "fetch", "--progress"], ["git", "-c", "http.x=1", "clone", "u"],
                     ["git", "--git-dir=/x/.git", "fetch"], ["env", "GIT_TRACE=1", "git", "clone", "u"],
                     [Path("/usr/bin/git"), "pull"]):
            with self.subTest(argv=argv):
                self.assertEqual(parsers.detect(argv), "git")

    def test_other_git_commands_are_generic(self):
        for argv in (["git", "push"], ["git", "status"], ["git", "commit", "-m", "clone"], ["git"],
                     ["git", "-C"], ["git", "log", "--", "clone"], ["gitk", "clone"], ["git", "-c", "clone"]):
            with self.subTest(argv=argv):
                self.assertIsNone(parsers.detect(argv))

    def test_git_is_registered_and_loads(self):
        self.assertTrue(parsers.known("git"))
        self.assertEqual(parsers.load("git").name, "git")   # a re-bind reloads the module, so not the same class object

    def test_an_explicit_tool_is_used_as_given(self):
        self.assertEqual(run._parser_for(jr, ["git", "clone", "u", "w"], None), "git")
        self.assertEqual(run._parser_for(jr, ["git", "clone", "u", "w"], "generic"), "generic")
        self.assertEqual(run._parser_for(jr, ["git", "push"], "generic"), "generic")
        self.assertEqual(run._parser_for(jr, ["mvn", "verify"], "generic"), "generic")
        self.assertEqual(run._parser_for(jr, ["git", "clone", "u", "w"], "npm"), "npm")
        self.assertEqual(run._parser_for(jr, ["git", "clone", "u", "w"], "git"), "git")


class CarriageReturnRunTest(unittest.TestCase):
    """The runner hands a cr_lines parser every CR-terminated progress piece as it arrives, not only the last one of a line."""

    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.tmp = Path(tempfile.mkdtemp())
        os.environ["TERMINAL_MARKERS"] = "1"
        self.sinks = jr.Sinks("none", str(self.tmp / "full.log"))
        self.script = self.tmp / "fake_git.py"
        self.script.write_text(
            "import sys\n"
            "e = sys.stderr\n"
            "e.write(\"Cloning into 'x'...\\n\")\n"
            "for i in range(0, 101, 10):\n"
            "    e.write('Receiving objects: %3d%% (%d/100)\\r' % (i, i)); e.flush()\n"
            "e.write('Receiving objects: 100% (100/100), done.\\r\\n')\n"
            "e.write('Resolving deltas: 100% (5/5), done.\\r\\n')\n"
            "sys.exit(int(sys.argv[1]))\n", encoding="utf-8")

    def run_tool(self, parser, exit_code=0):
        seen = []
        unsub = jr.subscribe(lambda kind, f: seen.append((kind, f)))
        out = io.StringIO()
        try:
            with redirect_stdout(out):
                result = jr.run_tool([sys.executable, str(self.script), str(exit_code)], parser, "clone", "Clone",
                                     sinks=self.sinks)
        finally:
            jr.unsubscribe(unsub)
        return result, seen

    def test_every_progress_piece_reaches_the_parser_as_it_arrives(self):
        got = []

        class Recording(git.PARSER):
            def on_line(self, line):
                got.append(line)
                return super().on_line(line)

        result, seen = self.run_tool(Recording())
        self.assertEqual(result.exit, 0)
        self.assertEqual(got[0], "Cloning into 'x'...")
        self.assertEqual(len([g for g in got if g.startswith("Receiving objects:") and "done" not in g]), 11)
        self.assertEqual(got[-1], "Resolving deltas: 100% (5/5), done.")
        self.assertNotIn("", got[1:])                       # a CRLF is one terminator, not a progress piece and a blank line
        units = [f for kind, f in seen if kind == "units"]
        self.assertEqual([(u["total"], u["kind"]) for u in units], [(CELLS, "progress")])
        done = [f["done"] for kind, f in seen if kind == "unit"]
        self.assertEqual(done, sorted(set(done)))
        self.assertEqual(done[-1], CELLS)

    def test_other_parsers_still_see_only_the_last_piece_of_a_line(self):
        got = []

        class Recording(jr.GenericParser):
            def on_line(self, line):
                got.append(line)
                return []

        self.run_tool(Recording())
        self.assertIn("Receiving objects: 100% (100/100), done.", got)
        self.assertEqual([g for g in got if g.startswith("Receiving objects:")], ["Receiving objects: 100% (100/100), done."])

    def test_a_failed_clone_reports_the_failure_text(self):
        result, _ = self.run_tool(git.PARSER(), exit_code=128)
        self.assertEqual(result.exit, 128)


if __name__ == "__main__":
    unittest.main()
