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
"""Unit tests for the Python tmux engine (terminal_engine): the parts that need no tmux server."""

import contextlib
import io
import os
import pty
import sys
import subprocess
import termios
import tempfile
import time
import unittest
from pathlib import Path

from tmux_support import BIN      # also puts BIN on sys.path

from terminal_engine import mouse, panes, procs, registry, ttymode, usage_text


class MouseTest(unittest.TestCase):
    def test_left_press_returns_column_and_row(self):
        self.assertEqual(mouse.left_press("[<0;5;1M"), (5, 1))

    def test_everything_else_is_not_a_left_press(self):
        for seq in ("[<0;5;1m", "[<2;5;1M", "[<64;5;1M", "[A", "[<0;;1M", "", "[<0;x;5M", "[<0;5;7;9M"):
            with self.subTest(seq=seq):
                self.assertIsNone(mouse.left_press(seq))

    def test_the_toolbar_form_does_not_need_a_numeric_row(self):
        self.assertEqual(mouse.left_press("[<0;5;M", need_y=False), (5, 0))
        self.assertIsNone(mouse.left_press("[<0;;1M", need_y=False))


class UsageTextTest(unittest.TestCase):
    def test_pane_usage_is_the_old_header_without_the_license_or_the_edit_note(self):
        t = usage_text.TEXT["tmux-pane"]
        self.assertTrue(t.startswith("# Run commands in tmux side panes"))
        self.assertIn("# Usage: tmux-pane.sh [-d DIR]", t)
        self.assertNotIn("Edit this file atomically", t)
        self.assertNotIn("Licensed to the Apache", t)

    def test_watch_usage_names_its_flags(self):
        t = usage_text.TEXT["watch-run"]
        self.assertIn("--no-close-kept", t)
        self.assertIn("--print-tail-filter", t)


class CbreakInputTest(unittest.TestCase):
    def test_echo_and_canonical_mode_are_off_inside_and_restored_after(self):
        master, slave = pty.openpty()
        try:
            before = termios.tcgetattr(slave)
            with ttymode.CbreakInput(slave):
                inside = termios.tcgetattr(slave)
                self.assertFalse(inside[3] & termios.ECHO)
                self.assertFalse(inside[3] & termios.ICANON)
                self.assertEqual(inside[6][termios.VMIN], 1)
            mask = termios.ECHO | termios.ICANON      # macOS also flips PENDIN on a pty
            self.assertEqual(termios.tcgetattr(slave)[3] & mask, before[3] & mask)
        finally:
            os.close(master)
            os.close(slave)

    def test_a_non_tty_is_left_alone(self):
        with tempfile.TemporaryFile() as f:
            with ttymode.CbreakInput(f.fileno()):
                pass


class ProcsTest(unittest.TestCase):
    def test_lstart_and_alive_for_this_process(self):
        self.assertTrue(procs.lstart(os.getpid()))
        self.assertTrue(procs.alive(os.getpid()))
        self.assertFalse(procs.alive(999999))

    def test_a_zombie_is_not_alive(self):
        p = subprocess.Popen(["true"])
        deadline = time.time() + 5
        while procs.alive(p.pid) and time.time() < deadline:
            time.sleep(0.05)
        self.assertFalse(procs.alive(p.pid))
        p.wait()

    def test_descend_lists_the_root_and_its_children(self):
        p = subprocess.Popen(["sh", "-c", "sleep 30 & wait"])
        try:
            deadline = time.time() + 5
            found = []
            while len(found) < 2 and time.time() < deadline:
                time.sleep(0.1)
                found = procs.descend(p.pid)
            self.assertEqual(found[0], p.pid)
            self.assertGreaterEqual(len(found), 2)
        finally:
            p.kill()
            p.wait()

    def test_track_records_each_process_once(self):
        with tempfile.TemporaryDirectory() as d:
            pids = Path(d) / "x.pids"
            pids.write_text("")
            procs.track(os.getpid(), pids)
            procs.track(os.getpid(), pids)
            rows = [l.split("\t") for l in pids.read_text().splitlines()]
            mine = [r for r in rows if r[0] == str(os.getpid())]
            self.assertEqual(len(mine), 1)
            self.assertEqual(mine[0][1], procs.lstart(os.getpid()))

    def test_report_lists_a_live_orphan_and_reap_kills_it(self):
        p = subprocess.Popen(["sleep", "60"])
        try:
            with tempfile.TemporaryDirectory() as d:
                pids = Path(d) / "x.pids"
                pids.write_text("%d\t%s\tsleep 60\n" % (p.pid, procs.lstart(p.pid)))
                buf = io.StringIO()
                with contextlib.redirect_stdout(buf):
                    procs.report(pids, reap=True, grace=2)
                text = buf.getvalue()
                self.assertIn("orphans (started by the command, still running):", text)
                self.assertIn("pid %d  sleep 60" % p.pid, text)
                self.assertIn("reaped 1 process(es)", text)
                p.wait(timeout=5)
        finally:
            if p.poll() is None:
                p.kill()

    def test_report_ignores_a_reused_pid(self):
        with tempfile.TemporaryDirectory() as d:
            pids = Path(d) / "x.pids"
            pids.write_text("%d\tMon Jan  1 00:00:00 1990\tfake\n" % os.getpid())
            buf = io.StringIO()
            with contextlib.redirect_stdout(buf):
                procs.report(pids, reap=False, grace=1)
            self.assertEqual(buf.getvalue(), "")


class RegistryTest(unittest.TestCase):
    def test_roundtrip_with_spaces_and_quotes(self):
        with tempfile.TemporaryDirectory() as d:
            f = Path(d) / "5.reg"
            registry.write_reg(f, {"pane": "%5", "title": "my title", "cmdline": "echo 'hi there'", "rd": d})
            got = registry.read_reg(f)
            self.assertEqual(got["title"], "my title")
            self.assertEqual(got["cmdline"], "echo 'hi there'")
            self.assertEqual(got["pane"], "%5")

    def test_reads_what_the_bash_printf_q_wrote(self):
        with tempfile.TemporaryDirectory() as d:
            f = Path(d) / "6.reg"
            f.write_text("pane=%6\ntitle=my\\ title\ncallerpid=123\ncmdline=\\'\\'\nstarted=1700000000\nrd=/tmp/x\npgid=44\n")
            got = registry.read_reg(f)
            self.assertEqual(got["title"], "my title")
            self.assertEqual(got["pgid"], "44")

    def test_write_is_atomic_no_tmp_left_behind(self):
        with tempfile.TemporaryDirectory() as d:
            f = Path(d) / "7.reg"
            registry.write_reg(f, {"pane": "%7"})
            self.assertEqual(sorted(p.name for p in Path(d).iterdir()), ["7.reg"])

    def test_reg_files_lists_every_reg_in_the_directory_sorted(self):
        with tempfile.TemporaryDirectory() as d:
            for n in ("9.reg", "10.reg", "3.pids"):
                (Path(d) / n).write_text("")
            self.assertEqual([Path(p).name for p in registry.reg_files(d, extra=[])], ["10.reg", "9.reg"])


class PanesUnitTest(unittest.TestCase):
    def test_equal_columns_picks_the_agent_and_every_non_follower_leader_left_to_right(self):
        text = "\n".join([
            "0|%1|100||",
            "100|%2|50|1|running",
            "100|%3|50|1|follower",
            "150|%4|50|1|exit 0",
        ])
        self.assertEqual(panes.equal_columns(text, agent="%1", excl=""), [("%1", 66), ("%2", 66)])

    def test_equal_columns_leaves_out_the_pane_about_to_close(self):
        text = "0|%1|100||\n100|%2|50|1|running\n150|%4|50|1|exit 0"
        self.assertEqual(panes.equal_columns(text, agent="%1", excl="%4"), [("%1", 75)])

    def test_equal_columns_needs_two_columns(self):
        self.assertEqual(panes.equal_columns("0|%1|100||", agent="%1", excl=""), [])

    def test_age_format(self):
        self.assertEqual(panes.format_age(5), "5s")
        self.assertEqual(panes.format_age(125), "2m")
        self.assertEqual(panes.format_age(3 * 3600 + 7 * 60), "3h7m")

    def test_pct(self):
        self.assertEqual(panes.pct("12"), "%12")
        self.assertEqual(panes.pct("%12"), "%12")


class CopyParseTest(unittest.TestCase):
    def test_parse_copy_args(self):
        self.assertEqual(panes.parse_copy_args(["%5"]), ("%5", None))
        self.assertEqual(panes.parse_copy_args(["-n", "3", "p"]), ("p", 3))
        self.assertEqual(panes.parse_copy_args(["p", "-n7"]), ("p", 7))
        self.assertEqual(panes.parse_copy_args(["p", "-n", ""]), ("p", None))     # bash treated an empty -n as no -n

    def test_parse_copy_args_rejects_bad_input(self):
        from terminal_engine import UsageError
        for argv in ([], ["-n"], ["-n", "x", "p"], ["a", "b"]):
            with self.subTest(argv=argv):
                with self.assertRaises(UsageError):
                    panes.parse_copy_args(argv)

    def test_tail_lines(self):
        self.assertEqual(panes.tail_lines("a\nb\nc", 2), "b\nc")
        self.assertEqual(panes.tail_lines("a\nb", None), "a\nb")
        self.assertEqual(panes.tail_lines("a\nb", 0), "")


class CancelUnitTest(unittest.TestCase):
    def reg(self, d, **extra):
        f = Path(d) / "7.reg"
        fields = {"pane": "%7", "title": "demo", "callerpid": "1", "cmdline": "x", "started": "1", "rd": d}
        fields.update(extra)
        registry.write_reg(f, fields)
        return f

    def test_a_target_that_is_not_this_run_returns_3(self):
        from terminal_engine import cancel
        with tempfile.TemporaryDirectory() as d:
            self.assertEqual(cancel.cancel_one(self.reg(d), "%9", 1), 3)

    def test_matches_by_id_with_or_without_the_percent_and_by_title(self):
        from terminal_engine import cancel
        with tempfile.TemporaryDirectory() as d:
            f = self.reg(d)
            for t in ("%7", "7", "demo"):
                with self.subTest(target=t):
                    self.assertTrue(cancel.matches(registry.read_reg(f), t))

    def test_a_run_that_has_not_started_yet_returns_4(self):
        from terminal_engine import cancel
        with tempfile.TemporaryDirectory() as d:
            self.assertEqual(cancel.cancel_one(self.reg(d), "--all", 1), 4)

    def test_a_reused_pid_is_not_touched(self):
        from terminal_engine import cancel
        with tempfile.TemporaryDirectory() as d:
            f = self.reg(d, pgid=str(os.getpid()), cpid=str(os.getpid()), cpidstart="Mon Jan  1 00:00:00 1990")
            self.assertEqual(cancel.cancel_one(f, "--all", 1), 4)

    def test_group_gone(self):
        from terminal_engine import cancel
        self.assertTrue(cancel.group_gone(999999))
        self.assertFalse(cancel.group_gone(os.getpgrp()))


class KeptTest(unittest.TestCase):
    def test_click_hit_accepts_a_left_press_on_the_button_only(self):
        from terminal_engine import kept
        box = (5, 3, 26)
        self.assertTrue(kept.click_hit("[<0;3;5M", *box))
        self.assertTrue(kept.click_hit("[<0;26;5M", *box))
        for seq in ("[<0;2;5M", "[<0;27;5M", "[<0;10;4M", "[<0;10;5m", "[<2;10;5M", "[<64;10;5M", "[<65;10;5M", "[A", "[<0;x;5M"):
            with self.subTest(seq=seq):
                self.assertFalse(kept.click_hit(seq, *box))

    def test_without_a_located_button_any_left_press_counts(self):
        from terminal_engine import kept
        self.assertTrue(kept.click_hit("[<0;40;9M"))
        self.assertFalse(kept.click_hit("[<0;40;9m"))
        self.assertFalse(kept.click_hit("[<64;40;9M"))

    def test_the_prompt_text(self):
        from terminal_engine import kept
        self.assertEqual(kept.prompt_prefix(0, False), "[exit 0]  ")
        self.assertEqual(kept.prompt_prefix(3, True), "[cancelled, exit 3] stopped by tmux-pane.sh cancel;  ")
        self.assertEqual(kept.readonly_message(2, False), "\n[exit 2] press Enter to close this pane\n")
        self.assertEqual(kept.readonly_message(2, True),
                         "\n[cancelled, exit 2] stopped by tmux-pane.sh cancel; press Enter to close this pane\n")

    def test_the_cursor_report_gives_the_button_row_and_columns(self):
        from terminal_engine import kept
        self.assertEqual(kept.button_box(b"\x1b[12;7", "[exit 0]  "), (11, 11, 11 + len(kept.BUTTON) - 1))
        self.assertEqual(kept.button_box(b"\x1b[12;7R", "[exit 0]  "), (11, 11, 11 + len(kept.BUTTON) - 1))
        self.assertIsNone(kept.button_box(b"garbage", "[exit 0]  "))

    def test_a_pty_run_closes_on_enter(self):
        import select as _select
        pid, fd = pty.fork()
        if pid == 0:
            sys.path.insert(0, str(BIN))
            from terminal_engine import kept as k
            k.kept_wait("[exit 0]  ", None)
            os._exit(0)
        try:
            out = b""
            deadline = time.time() + 5
            while b"press Enter" not in out and time.time() < deadline:
                if _select.select([fd], [], [], 0.2)[0]:
                    out += os.read(fd, 4096)
            self.assertIn(b"[ click here to close ]", out)
            os.write(fd, b"\r")
            _, status = os.waitpid(pid, 0)
            self.assertEqual(status, 0)
        finally:
            os.close(fd)


class LeaderUnitTest(unittest.TestCase):
    def test_exit_status_mapping(self):
        from terminal_engine import leader
        self.assertEqual(leader.exit_code(0), 0)
        self.assertEqual(leader.exit_code(5), 5)
        self.assertEqual(leader.exit_code(-15), 143)      # killed by TERM, as bash reports it
        self.assertEqual(leader.exit_code(-9), 137)

    def test_the_command_in_its_own_foreground_group_can_use_the_terminal(self):
        import select as _select
        pid, fd = pty.fork()
        if pid == 0:
            sys.path.insert(0, str(BIN))
            from terminal_engine import leader as l
            code = l.run_command(["sh", "-c", "[ -t 0 ] && echo istty; exit 7"], Path(tempfile.mkdtemp()))
            os._exit(code)
        out = b""
        deadline = time.time() + 5
        while time.time() < deadline:
            if _select.select([fd], [], [], 0.2)[0]:
                try:
                    chunk = os.read(fd, 4096)
                except OSError:
                    break
                if not chunk:
                    break
                out += chunk
        _, status = os.waitpid(pid, 0)
        os.close(fd)
        self.assertIn(b"istty", out)
        self.assertEqual(os.WEXITSTATUS(status), 7)

    def test_a_missing_command_is_127_and_a_directory_is_126(self):
        from terminal_engine import leader
        d = Path(tempfile.mkdtemp())
        self.assertEqual(leader.run_command(["no-such-command-xyz"], d), 127)
        self.assertEqual(leader.run_command([str(d)], d), 126)

    def test_cpid_is_written_for_the_caller(self):
        from terminal_engine import leader
        d = Path(tempfile.mkdtemp())
        leader.run_command(["true"], d)
        self.assertTrue((d / "cpid").read_text().strip().isdigit())

    def test_a_signal_death_is_128_plus_the_signal(self):
        from terminal_engine import leader
        d = Path(tempfile.mkdtemp())
        self.assertEqual(leader.run_command(["sh", "-c", "kill -TERM $$"], d), 143)


class RunnerParseTest(unittest.TestCase):
    def parse(self, *argv):
        from terminal_engine import runner
        return runner.parse_options(list(argv))

    def test_cmd_mode_after_double_dash(self):
        o = self.parse("-k", "never", "-t", "T", "--", "echo", "hi")
        self.assertEqual((o.keep, o.title, o.cmdmode, o.rest), ("never", "T", True, ["echo", "hi"]))

    def test_cmd_mode_on_the_first_non_option_word(self):
        o = self.parse("-d/tmp", "echo", "x")
        self.assertEqual((o.dir, o.cmdmode, o.rest), ("/tmp", True, ["echo", "x"]))

    def test_panes_with_sizes_and_titles(self):
        o = self.parse("--pane", "a", "--size", "30%", "--pane-title", "ft", "--pane", "b")
        self.assertEqual(o.panes, ["a", "b"])
        self.assertEqual(o.sizes, ["", "30"])
        self.assertEqual(o.ptitles, ["", "ft"])

    def test_usage_errors(self):
        from terminal_engine import UsageError
        bad = (["--pane"], ["--size", "20%", "--pane", "true"], ["--pane", "true", "--size", "20%"],
               ["--size", "abc", "--pane", "true", "--pane", "true"], ["--size", "20", "--pane", "true", "--pane", "true"],
               ["--pane", "true", "--", "true"], ["--pane", "true", "true"], ["-k", "bogus", "--", "true"],
               ["-K", "x", "--", "true"], ["-z", "--", "true"], [], ["-k"])
        for argv in bad:
            with self.subTest(argv=argv):
                with self.assertRaises(UsageError):
                    self.parse(*argv)

    def test_f_was_removed(self):
        from terminal_engine import runner
        self.assertEqual(runner.run(["-f", "x"]), 2)

    def test_the_channel_name(self):
        from terminal_engine import runner
        self.assertTrue(runner.chan_name(123, 456).startswith("tmux-pane-123-"))


class WatchUnitTest(unittest.TestCase):
    def test_tmux_version_gate(self):
        from terminal_engine import watch
        for text, want in (("tmux 3.7c", True), ("tmux 3.4", True), ("tmux 3.3a", False), ("tmux next-3.5", True),
                           ("tmux 2.9", False), ("tmux 4.0", True), ("tmux master", True), ("", True)):
            with self.subTest(text=text):
                self.assertEqual(watch.tmux_version_ok(text), want)

    def test_safe_name(self):
        from terminal_engine import watch
        self.assertEqual(watch.safe_name("push.py"), "push.py")
        self.assertEqual(watch.safe_name("a b/c"), "a-b-c")
        self.assertEqual(watch.safe_name("--x--"), "x")
        self.assertEqual(watch.safe_name("///"), "run")

    def test_script_stem_picks_the_first_script_like_word(self):
        from terminal_engine import watch
        self.assertEqual(watch.script_stem(["python3", "scripts/push.py", "msg"]), "push")
        self.assertEqual(watch.script_stem(["bash", "-c", "echo hi"]), "bash")
        self.assertEqual(watch.script_stem(["./run.sh"]), "run")
        self.assertEqual(watch.script_stem(["make", "x=1", "all"]), "make")

    def test_tail_filter_text_is_the_bash_one(self):
        from terminal_engine import watch
        self.assertEqual(watch.TAIL_FILTER,
                         "perl -MEncode -e '$|=1; while (<STDIN>) { $_ = decode(\"UTF-8\", $_); s/[\\x{2028}\\x{2029}\\p{Cf}]/ /g; "
                         "s/[\\x00-\\x08\\x0b-\\x1a\\x1c-\\x1f\\x7f]//g; print encode(\"UTF-8\", $_) }'")

    def test_result_summary(self):
        from terminal_engine import watch
        r = {"status": "fail", "exit": 3, "ms": 12, "rows": [{"status": "FAIL", "label": "unit", "ms": 5,
             "tests": {"total": 2, "fail": 1, "err": 0, "skip": 0}, "summary": "boom"}],
             "notes": [{"level": "error", "text": "it broke"}], "logs": {"full": "/x/full.log"}}
        self.assertEqual(watch.result_summary(r).splitlines(), [
            "result: status=fail exit=3 ms=12",
            "  FAIL  unit (5ms) tests=2 fail=1 err=0 skip=0 boom",
            "  error: it broke",
            "  log.full: /x/full.log"])


class ToolbarPureTest(unittest.TestCase):
    def test_status_words(self):
        from terminal_engine import toolbar
        for raw, want in (("", "closed"), ("running", "running"), ("exit 0", "ok"), ("exit 3", "failed (exit 3)"),
                          ("kept (exit 0)", "ok"), ("kept (exit 12)", "failed (exit 12)"), ("kept (cancelled, exit 130)", "cancelled"),
                          ("weird", "weird")):
            with self.subTest(raw=raw):
                self.assertEqual(toolbar.status_word(raw), want)

    def test_line(self):
        from terminal_engine import toolbar
        self.assertEqual(toolbar.render_line("running", "0", "demo"), "[Hide output] [Cancel] [Close]  demo: running")
        self.assertEqual(toolbar.render_line("running", "1", "demo"), "[Show output] [Cancel] [Close]  demo: running")
        self.assertEqual(toolbar.render_line("exit 0", "0", "demo", msg="cancelling"), "[Hide output] [Cancel] [Close]  demo: ok (cancelling)")

    def test_line_fits_the_columns_only_above_the_buttons(self):
        from terminal_engine import toolbar
        self.assertEqual(toolbar.render_line("running", "0", "demo", cols=36), "[Hide output] [Cancel] [Close]  demo")
        self.assertEqual(toolbar.render_line("running", "0", "demo", cols=20), "[Hide output] [Cancel] [Close]  demo: running")

    def test_styled_line_dims_cancel_once_the_run_is_over(self):
        from terminal_engine import toolbar
        self.assertIn("\033[2m[Cancel]\033[22m", toolbar.render_line("exit 0", "0", "demo", styled=True))
        self.assertNotIn("\033[2m", toolbar.render_line("running", "0", "demo", styled=True))

    def test_hit_button(self):
        from terminal_engine import toolbar
        for col, want in ((1, "hide"), (13, "hide"), (14, "none"), (15, "cancel"), (22, "cancel"), (23, "none"), (24, "close"), (30, "close"),
                          (31, "none"), (0, "none"), ("x", "none"), ("", "none")):
            with self.subTest(col=col):
                self.assertEqual(toolbar.hit_button(col), want)

    def test_click_column(self):
        from terminal_engine import toolbar
        self.assertEqual(toolbar.click_column("[<0;5;1M"), 5)
        for seq in ("[<0;5;1m", "[<2;5;1M", "[<64;5;1M", "[A", "[<0;;1M", ""):
            with self.subTest(seq=seq):
                self.assertIsNone(toolbar.click_column(seq))


class KeyParserTest(unittest.TestCase):
    def feed(self, data):
        from terminal_engine import toolbar
        return toolbar.KeyParser().feed(data)

    def test_keys(self):
        self.assertEqual(self.feed("hHkKxXq"), ["hide", "hide", "cancel", "cancel", "close", "close"])

    def test_a_click_acts_like_its_key(self):
        self.assertEqual(self.feed("\033[<0;3;1M"), ["hide"])
        self.assertEqual(self.feed("\033[<0;16;1M"), ["cancel"])
        self.assertEqual(self.feed("\033[<0;25;1M"), ["close"])

    def test_release_wheel_and_arrows_are_ignored(self):
        self.assertEqual(self.feed("\033[<0;3;1m\033[<64;3;1M"), [])
        self.assertEqual(self.feed("\033[A"), [])

    def test_a_sequence_split_across_reads_is_reassembled(self):
        from terminal_engine import toolbar
        p = toolbar.KeyParser()
        self.assertEqual(p.feed("\033[<0;3"), [])
        self.assertEqual(p.feed(";1M"), ["hide"])

    def test_a_stalled_sequence_is_dropped_after_a_second_of_silence(self):
        from terminal_engine import toolbar
        p = toolbar.KeyParser()
        p.feed("\033[<0")
        p.timeout()
        self.assertEqual(p.feed("h"), ["hide"])


class SnapshotTest(unittest.TestCase):
    def test_parse_snapshot(self):
        from terminal_engine import toolbar
        text = "\n".join(["%1|||@1|50", "%2||running|@1|40", "%3|%2|follower|@1|30", "%4|%2|follower|@1|1"])
        s = toolbar.parse_snapshot(text, run="%2", self_pane="%4")
        self.assertEqual((s.leader, s.log, s.hidden, s.height), ("running", "%3", 0, "1"))

    def test_the_log_in_another_window_is_hidden(self):
        from terminal_engine import toolbar
        text = "%2||running|@1|40\n%3|%2|follower|@9|30\n%4|%2|follower|@1|1"
        self.assertEqual(toolbar.parse_snapshot(text, run="%2", self_pane="%4").hidden, 1)

    def test_a_gone_leader_reads_as_empty(self):
        from terminal_engine import toolbar
        s = toolbar.parse_snapshot("%4|%2|follower|@1|1", run="%2", self_pane="%4")
        self.assertEqual((s.leader, s.log), ("", ""))


class DirectPathTest(unittest.TestCase):
    def test_main_returns_the_shim_exit_codes_without_a_subprocess(self):
        import terminal_engine
        self.assertEqual(terminal_engine.main(["watch-run", "--check-tmux-version", "tmux 3.7c"]), 0)
        self.assertEqual(terminal_engine.main(["watch-run", "--check-tmux-version", "tmux 3.3"]), 1)
        self.assertEqual(terminal_engine.main(["tmux-toolbar", "--print-hit", "15"]), 0)

    def test_usage_errors_print_the_usage_and_return_2(self):
        import contextlib
        import io
        import terminal_engine
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            self.assertEqual(terminal_engine.main(["watch-run"]), 2)
        self.assertIn("Usage: watch-run.sh", err.getvalue())

    def test_outside_tmux_exits_2(self):
        import terminal_engine
        old = os.environ.pop("TMUX", None)
        try:
            self.assertEqual(terminal_engine.main(["watch-run", "--", "true"]), 2)
            self.assertEqual(terminal_engine.main(["tmux-pane", "--", "true"]), 2)
        finally:
            if old is not None:
                os.environ["TMUX"] = old

    def test_the_shim_and_the_direct_call_agree(self):
        shim = subprocess.run([str(BIN / "tmux-toolbar.sh"), "--print-line", "running", "1", "demo"],
                              stdout=subprocess.PIPE, universal_newlines=True)
        from terminal_engine import toolbar
        self.assertEqual(shim.stdout.rstrip("\n"), toolbar.render_line("running", "1", "demo"))
        self.assertEqual(shim.returncode, 0)

    def test_an_unknown_engine_command_is_2(self):
        import terminal_engine
        self.assertEqual(terminal_engine.main(["nope"]), 2)


if __name__ == "__main__":
    unittest.main()
