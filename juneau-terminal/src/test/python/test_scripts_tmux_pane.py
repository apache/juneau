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
"""tmux-pane.sh on a throwaway tmux server (-L), skipped where no private server can start."""

import os
import shutil
import subprocess
import time
import unittest
from pathlib import Path

from tmux_support import TMUX_PANE_SH, PaneCase, requires_tmux, slow_tmux

SCRIPT = TMUX_PANE_SH


def pane_id(proc):
    line = proc.stdout.readline()
    assert line.startswith("pane %"), line
    return line.split()[1].rstrip(":")


def pid_alive(pid):
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    return subprocess.run(["ps", "-o", "stat=", "-p", str(pid)], capture_output=True, text=True).stdout.strip()[:1] != "Z"


def read_pid(path, timeout=10):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if path.exists() and path.read_text().strip():
            return int(path.read_text())
        time.sleep(0.1)
    raise AssertionError("%s never written" % path)


def heights(case):
    """(pane_id, top, height, state) rows of the throwaway server, in layout order."""
    out = case.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_id} #{pane_top} #{pane_height} #{@tmux_state}")
    rows = [line.split(None, 3) for line in out.splitlines()]
    return sorted(rows, key=lambda r: int(r[1]))


def column_widths(case):
    """Widths of the agent pane and of every leader (the columns), in left-to-right order."""
    out = case.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_left}|#{pane_id}|#{pane_width}|#{@tmux_pane}|#{@tmux_state}")
    rows = sorted((line.split("|") for line in out.splitlines()), key=lambda r: int(r[0]))
    return [int(r[2]) for r in rows if r[1] == case.agent or (r[3] == "1" and not r[4].startswith("follower"))]


def wait_equal(case, columns, timeout=10):
    deadline = time.time() + timeout
    widths = column_widths(case)
    while time.time() < deadline and not (len(widths) == columns and max(widths) - min(widths) <= columns):
        time.sleep(0.1)
        widths = column_widths(case)
    return widths


ESCAPE = """sleep 31337 & echo $! > %(a)s
python3 -c "import os, sys; os.setsid(); open(sys.argv[1], 'w').write(str(os.getpid())); os.execvp('sleep', ['sleep', '31338'])" %(b)s &
wait"""

ORPHAN = "sleep 4242 & echo $! > %s; sleep 2.6"


@requires_tmux
class TmuxPaneTest(PaneCase):

    # --- back-compat of `-- CMD` ---

    def test_dash_dash_command_prints_the_pane_tail_and_propagates_the_exit_code(self):
        code, out = self.run_cmd("bash", "-c", "echo plain-output; exit 4")
        self.assertEqual(code, 4)
        self.assertIn("plain-output", out)
        self.assertIn("exit=4", out)
        self.assertNotIn("result:", out)

    def test_no_juneau_terminal_knowledge_in_the_environment(self):
        code, out = self.run_cmd("bash", "-c", 'echo "R=[${JUNEAU_TERMINAL_RESULT-unset}] C=[${JUNEAU_TERMINAL_CANCEL-unset}] L=[${JUNEAU_TERMINAL_FULL_LOG-unset}]"')
        self.assertIn("R=[unset] C=[unset] L=[unset]", out)
        self.assertEqual([d for d in Path(self.env["TMPDIR"]).glob("**/result.json")], [])

    @slow_tmux
    def test_launch_output_and_first_pane_line_carry_the_cancel_hint(self):
        proc = self.start("-k", "never", "--", "sleep", "30")
        pid = pane_id(proc)
        self.assertIn("tmux-pane.sh cancel %s" % pid, proc.stdout.readline())
        time.sleep(1)
        self.assertEqual(self.sub("cancel", pid)[0], 0)
        proc.communicate(timeout=30)

    # --- the kept pane's click-to-close button ---

    def click_hit(self, report, *box):
        """Exit status of the script's click_hit on the bytes after ESC, run under /bin/bash (3.2 on macOS)."""
        src = subprocess.run(["sed", "-n", "/^# --- kept-pane prompt/,/^# --- end kept-pane prompt/p", str(SCRIPT)],
                             capture_output=True, text=True, check=True).stdout
        r = subprocess.run(["/bin/bash", "-c", src + '\nclick_hit "$@"', "x", report, *map(str, box)], capture_output=True, text=True)
        return r.returncode

    def test_click_hit_accepts_a_left_press_on_the_button_only(self):
        box = (5, 3, 26)
        self.assertEqual(self.click_hit("[<0;3;5M", *box), 0)
        self.assertEqual(self.click_hit("[<0;26;5M", *box), 0)
        self.assertEqual(self.click_hit("[<0;2;5M", *box), 1)         # left of the button
        self.assertEqual(self.click_hit("[<0;27;5M", *box), 1)        # right of it
        self.assertEqual(self.click_hit("[<0;10;4M", *box), 1)        # another row
        self.assertEqual(self.click_hit("[<0;10;5m", *box), 1)        # release, not press
        self.assertEqual(self.click_hit("[<2;10;5M", *box), 1)        # right button
        self.assertEqual(self.click_hit("[<64;10;5M", *box), 1)       # wheel up
        self.assertEqual(self.click_hit("[<65;10;5M", *box), 1)       # wheel down
        self.assertEqual(self.click_hit("[A", *box), 1)               # an arrow key
        self.assertEqual(self.click_hit("[<0;x;5M", *box), 1)

    def test_click_hit_without_a_located_button_accepts_any_left_press(self):
        self.assertEqual(self.click_hit("[<0;80;40M"), 0)
        self.assertEqual(self.click_hit("[<0;80;40m"), 1)
        self.assertEqual(self.click_hit("[<64;80;40M"), 1)

    def test_kept_pane_shows_the_button_and_enables_mouse_reporting(self):
        code, out = self.sub("-k", "always", "--", "true")
        self.assertEqual(code, 0)
        pane = out.split()[1].rstrip(":")
        time.sleep(1)
        shown = self.tmux("capture-pane", "-p", "-t", pane)
        self.assertIn("[exit 0]  [ click here to close ]  (or press Enter)", shown)
        self.assertEqual(self.tmux("show-options", "-pqv", "-t", pane, "@tmux_state").strip(), "kept (exit 0)")
        self.assertEqual(self.tmux("display", "-p", "-t", pane, "#{mouse_any_flag}#{mouse_sgr_flag}").strip(), "11")
        self.sub("close", pane)

    def test_clicking_the_button_closes_the_pane_and_other_clicks_do_not(self):
        code, out = self.sub("-k", "always", "--", "true")
        pane = out.split()[1].rstrip(":")
        time.sleep(1)
        lines = self.tmux("capture-pane", "-p", "-t", pane).splitlines()
        row = next(i for i, l in enumerate(lines) if "click here" in l) + 1
        col = lines[row - 1].index("click here") + 1
        for report in ("\033[<0;%d;%dM" % (col, row + 1), "\033[<64;%d;%dM" % (col, row), "\033[<0;%d;%dm" % (col, row)):
            self.tmux("send-keys", "-l", "-t", pane, report)
        time.sleep(0.5)
        self.assertEqual(self.panes(), 2)
        self.tmux("send-keys", "-l", "-t", pane, "\033[<0;%d;%dM" % (col, row))
        self.assertEqual(self.wait_panes(1), 1)

    def test_clicking_the_button_closes_the_pane_when_the_cursor_started_on_the_last_row(self):
        code, out = self.sub("-k", "always", "--", "sh", "-c", "sleep 1; seq 1 300")
        pane = out.split()[1].rstrip(":")
        time.sleep(1.5)
        lines = self.tmux("capture-pane", "-p", "-t", pane).splitlines()
        row = next(i for i, l in enumerate(lines) if "click here" in l) + 1
        col = lines[row - 1].index("click here") + 1
        self.tmux("send-keys", "-l", "-t", pane, "\033[<0;%d;%dM" % (col, row))
        self.assertEqual(self.wait_panes(1), 1)

    def test_read_only_kept_pane_has_no_button(self):
        code, out = self.sub("-k", "always", "--read-only", "--", "true")
        pane = out.split()[1].rstrip(":")
        time.sleep(1)
        shown = self.tmux("capture-pane", "-p", "-t", pane)
        self.assertIn("[exit 0] press Enter to close this pane", shown)
        self.assertNotIn("click here", shown)
        self.sub("close", pane)

    def test_script_emits_matching_mouse_enable_and_disable_sequences(self):
        text = SCRIPT.read_text()
        self.assertIn(r"\033[?1000h\033[?1006h", text)
        self.assertIn(r"\033[?1006l\033[?1000l", text)

    # --- pane border titles ---

    def border(self, target):
        return self.tmux("display", "-p", "-t", target, "#{pane-border-status}|#{pane-border-format}").strip()

    def test_border_line_is_titled_while_a_run_is_open_and_restored_after(self):
        self.assertEqual(self.border(self.agent).split("|")[0], "off")
        proc = self.start("-k", "never", "-t", "my title", "--pane", "sleep 30", "--pane-title", "x.log", "--pane", "sleep 31")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(3), 3)
        time.sleep(1)
        self.assertEqual(self.border(leader).split("|")[0], "top")
        titles = self.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_id}=#{pane_title}")
        self.assertIn("%s=my title" % leader, titles)
        self.assertIn("=x.log", titles)
        self.assertEqual(self.tmux("show-options", "-pqv", "-t", self.agent, "pane-border-format").strip(), "")
        self.assertEqual(self.tmux("show-options", "-pq", "-t", self.agent, "pane-border-format").strip(), "pane-border-format ''")
        self.sub("cancel", leader)
        proc.communicate(timeout=30)
        self.assertEqual(self.wait_panes(1), 1)
        time.sleep(1)
        self.assertEqual(self.border(self.agent).split("|")[0], "off")
        self.assertEqual(self.tmux("show-options", "-pq", "-t", self.agent, "pane-border-format").strip(), "")

    def test_toolkit_panes_get_a_framed_border_title_and_the_agent_pane_stays_blank(self):
        proc = self.start("-k", "never", "--pane", "sleep 30", "--pane-title", "x.log", "--pane", "sleep 31")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(3), 3)
        time.sleep(1)
        for p in self.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_id}").split():
            fmt = self.tmux("show-options", "-pqv", "-t", p, "pane-border-format").strip()
            if p == self.agent:
                self.assertEqual(fmt, "")
                self.assertEqual(self.tmux("show-options", "-pqv", "-t", p, "@tmux_border_blank").strip(), "1")
            else:
                self.assertIn("─┤", fmt)
                self.assertIn("#{pane_title}", fmt)
                self.assertIn("├─", fmt)
                self.assertIn("range=control|8", fmt)
                self.assertIn("range=control|9", fmt)
                self.assertNotIn("pane_index", fmt)
                self.assertEqual(self.tmux("show-options", "-pqv", "-t", p, "@tmux_border_blank").strip(), "")
        self.sub("cancel", leader)
        proc.communicate(timeout=30)

    def test_a_pane_format_of_its_own_is_not_overwritten(self):
        self.tmux("set-option", "-p", "-t", self.agent, "pane-border-format", "mine")
        code, out = self.run_cmd("true")
        self.assertEqual(self.tmux("show-options", "-pqv", "-t", self.agent, "pane-border-format").strip(), "mine")

    def test_border_line_already_on_is_left_alone(self):
        self.tmux("set-option", "-w", "-t", "t", "pane-border-status", "bottom")
        code, out = self.run_cmd("true")
        self.assertEqual(code, 0)
        self.assertEqual(self.border(self.agent).split("|")[0], "bottom")
        self.assertEqual(self.tmux("show-options", "-wqv", "-t", "t", "@tmux_border_prev").strip(), "")

    def test_killing_the_leader_externally_closes_its_follower(self):
        proc = self.start("-k", "never", "--pane", "sleep 30", "--pane", "sleep 31")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(3), 3)
        time.sleep(1)
        self.tmux("kill-pane", "-t", leader)
        self.assertEqual(self.wait_panes(1, timeout=15), 1)
        proc.communicate(timeout=30)
        self.assertEqual(self.border(self.agent).split("|")[0], "off")

    # --- argument parsing ---

    def test_usage_errors_exit_2(self):
        for args in (
            ["--pane"],
            ["--size", "20%", "--pane", "true"],                       # --size on the leader
            ["--pane", "true", "--size", "20%"],                       # --size without a following --pane
            ["--size", "abc", "--pane", "true", "--pane", "true"],
            ["--size", "20", "--pane", "true", "--pane", "true"],
            ["--pane", "true", "--", "true"],                          # -- mixed with --pane
            ["--pane", "true", "true"],                                # stray positional word
            ["-k", "sometimes", "--pane", "true"],
            [],
        ):
            with self.subTest(args=args):
                code, out = self.sub(*args)
                self.assertEqual(code, 2)

    def test_f_was_removed_and_names_watch_run(self):
        code, out = self.sub("-f", "--", "true")
        self.assertEqual(code, 2)
        self.assertIn("watch-run.sh", out)

    def test_not_inside_tmux_exits_2(self):
        env = {k: v for k, v in self.env.items() if k != "TMUX"}
        r = subprocess.run([str(SCRIPT), "--", "true"], capture_output=True, text=True, env=env)
        self.assertEqual(r.returncode, 2)
        self.assertIn("not inside tmux", r.stderr)

    # --- layout ---

    @slow_tmux
    def test_leader_and_two_followers_stack_in_order_with_default_sizes(self):
        proc = self.start("-k", "never", "--pane", "sleep 30", "--pane", "sleep 31", "--pane", "sleep 32")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(4), 4)
        time.sleep(0.5)
        rows = heights(self)
        ids = [r[0] for r in rows]
        self.assertEqual(ids[0], "%0")      # %0 is the caller's pane, left of the column
        self.assertEqual(ids[1], leader)
        self.assertEqual(len(ids), 4)
        col = [r for r in rows if r[0] != "%0"]
        # same column, leader on top, followers below in order
        tops = [int(r[1]) for r in col]
        self.assertEqual(tops, sorted(tops))
        self.assertEqual(col[0][0], leader)
        leader_h, f1_h, f2_h = (int(r[2]) for r in col)
        colh = leader_h + f1_h + f2_h + 2
        self.assertLessEqual(abs((f1_h + f2_h + 1) - colh * 0.4), 3)          # first follower's 40% is shared by the two followers
        self.assertLessEqual(abs(f1_h - f2_h), 2)
        self.assertEqual([r[3] for r in col[1:]], ["follower", "follower"])
        self.sub("cancel", leader)
        proc.communicate(timeout=30)

    @slow_tmux
    def test_size_is_honoured_for_a_follower(self):
        proc = self.start("-k", "never", "--pane", "sleep 30", "--size", "20%", "--pane", "sleep 31")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(3), 3)
        time.sleep(0.5)
        col = [r for r in heights(self) if r[0] != "%0"]
        colh = sum(int(r[2]) for r in col) + 1
        self.assertLessEqual(abs(int(col[1][2]) - colh * 0.2), 2)
        self.sub("cancel", leader)
        proc.communicate(timeout=30)

    @slow_tmux
    def test_followers_are_tagged_with_their_leader(self):
        proc = self.start("-k", "never", "--pane", "sleep 30", "--pane", "sleep 31")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(3), 3)
        time.sleep(0.5)
        out = self.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_id}|#{@tmux_state}|#{@tmux_parent}")
        self.assertIn("|follower|%s" % leader, out)
        listing = self.sub("list")[1]
        self.assertIn("follower of %s" % leader, listing)
        self.assertIn("follower", listing)
        self.assertIn("running", listing)
        self.sub("cancel", leader)
        proc.communicate(timeout=30)

    # --- env and exit code ---

    @slow_tmux
    def test_generic_exports_reach_the_leader_and_followers(self):
        tmp_path = self.tmp
        out_l, out_f = tmp_path / "l.txt", tmp_path / "f.txt"
        show = 'echo "id=$TMUX_PANE_RUN_ID cancel=$TMUX_PANE_RUN_CANCEL pane=$TMUX_PANE" > %s; sleep %s'
        proc = self.start("-k", "never", "-d", str(tmp_path), "--pane", show % (out_l, 30), "--pane", show % (out_f, 30))
        leader = pane_id(proc)
        for f in (out_l, out_f):
            deadline = time.time() + 10
            while (not f.exists() or not f.read_text().strip()) and time.time() < deadline:
                time.sleep(0.1)
        lt, ft = out_l.read_text(), out_f.read_text()
        self.assertIn("id=%s " % leader, lt)
        self.assertIn("id=%s " % leader, ft)
        self.assertIn("pane=%s" % leader, lt)
        self.assertNotIn("pane=%s" % leader, ft)          # a follower keeps its own TMUX_PANE
        cancel = lt.split("cancel=")[1].split(" pane=")[0].strip()
        self.assertTrue(cancel.endswith("cancel %s" % leader))
        self.assertIn("tmux-pane.sh", cancel)
        # the exported command works
        time.sleep(1)  # the caller notices the command's pid on its next 0.2 s tick
        r = subprocess.run(["bash", "-c", cancel], capture_output=True, text=True, env=self.env, timeout=60)
        self.assertEqual(r.returncode, 0)
        self.assertIn("sent INT", r.stdout)
        out, _ = proc.communicate(timeout=30)
        self.assertIn("closed (cancelled)", out)

    def test_caller_values_do_not_override_the_generic_exports(self):
        self.env["TMUX_PANE_RUN_ID"] = "bogus"
        code, out = self.run_cmd("bash", "-c", 'echo "id=$TMUX_PANE_RUN_ID"')
        self.assertIn("id=%", out)
        self.assertNotIn("bogus", out)

    @slow_tmux
    def test_exit_code_comes_from_the_leader_only(self):
        code = self.start("-k", "never", "--pane", "sleep 1", "--pane", "exit 7")
        out, _ = code.communicate(timeout=30)
        self.assertEqual(code.returncode, 0)
        self.assertIn("exit=0", out)
        p = self.start("-k", "never", "--pane", "exit 5", "--pane", "sleep 2")
        out, _ = p.communicate(timeout=30)
        self.assertEqual(p.returncode, 5)
        self.assertIn("exit=5", out)

    def test_pane_and_dash_dash_leaders_print_the_pane_tail(self):
        p = self.start("-k", "never", "--pane", "echo leader-text; exit 2")
        out, _ = p.communicate(timeout=30)
        self.assertEqual(p.returncode, 2)
        self.assertIn("leader-text", out)
        self.assertIn("exit=2", out)

    @slow_tmux
    def test_followers_have_no_registry_entry(self):
        proc = self.start("-k", "never", "--pane", "sleep 30", "--pane", "sleep 31")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(3), 3)
        time.sleep(0.5)
        regs = list((Path(self.env["TMPDIR"]) / "tmux-pane").glob("*.reg"))
        self.assertEqual([r.name for r in regs], ["%s.reg" % leader[1:]])
        self.sub("cancel", leader)
        proc.communicate(timeout=30)

    # --- lifecycle ---

    @slow_tmux
    def test_never_keeps_everything_and_close_takes_the_lot(self):
        p = self.start("-k", "never", "--pane", "exit 0", "--pane", "sleep 60")
        out, _ = p.communicate(timeout=30)
        self.assertIn("closed", out)
        self.assertEqual(self.wait_panes(1), 1)
        p = self.start("-k", "always", "--pane", "exit 3", "--pane", "sleep 60")
        out, _ = p.communicate(timeout=30)
        self.assertIn("kept open", out)
        self.assertEqual(self.panes(), 3)
        code, out = self.sub("close")
        self.assertEqual(code, 0)
        self.assertEqual(out.count("closed %"), 2)
        self.assertEqual(self.panes(), 1)

    def test_kept_leader_close_pane_sweeps_followers(self):
        p = self.start("-k", "always", "--pane", "exit 3", "--pane", "sleep 60")
        leader = pane_id(p)
        p.communicate(timeout=30)
        self.assertEqual(self.panes(), 3)
        self.assertEqual(self.sub("close", leader)[0], 0)
        self.assertEqual(self.panes(), 1)

    @slow_tmux
    def test_fail_closes_everything_on_success_and_keeps_on_failure(self):
        p = self.start("-k", "fail", "--pane", "exit 0", "--pane", "sleep 60")
        p.communicate(timeout=30)
        self.assertEqual(self.wait_panes(1), 1)
        p = self.start("-k", "fail", "--pane", "exit 9", "--pane", "sleep 60")
        out, _ = p.communicate(timeout=30)
        self.assertEqual(p.returncode, 9)
        self.assertIn("kept open", out)
        self.assertEqual(self.panes(), 3)
        self.sub("close", "--kept")
        self.assertEqual(self.panes(), 1)

    @slow_tmux
    def test_enter_in_the_kept_leader_closes_its_followers(self):
        p = self.start("-k", "always", "--pane", "exit 1", "--pane", "sleep 60")
        leader = pane_id(p)
        p.communicate(timeout=30)
        self.assertEqual(self.panes(), 3)
        self.tmux("send-keys", "-t", leader, "Enter")
        self.assertEqual(self.wait_panes(1), 1)

    @slow_tmux
    def test_killing_the_leader_pane_returns_129_and_sweeps_followers(self):
        proc = self.start("-k", "never", "--pane", "sleep 60", "--pane", "sleep 61")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(3), 3)
        time.sleep(1)
        self.tmux("kill-pane", "-t", leader)
        out, _ = proc.communicate(timeout=20)
        self.assertEqual(proc.returncode, 129)
        self.assertIn("pane gone", out)
        self.assertEqual(self.wait_panes(1), 1)

    @slow_tmux
    def test_ctrl_c_in_the_pane_still_interrupts_the_command(self):
        proc = self.start("-k", "never", "--", "sleep", "60")
        pid = pane_id(proc)
        time.sleep(1.5)
        self.tmux("send-keys", "-t", pid, "C-c")
        out, _ = proc.communicate(timeout=20)
        self.assertEqual(proc.returncode, 130)
        self.assertIn("exit=130", out)

    @slow_tmux
    def test_killing_the_pane_mid_run_returns_129_instead_of_hanging(self):
        proc = self.start("-k", "never", "--", "sleep", "60")
        pid = pane_id(proc)
        time.sleep(1)
        self.tmux("kill-pane", "-t", pid)
        out, _ = proc.communicate(timeout=20)
        self.assertEqual(proc.returncode, 129)
        self.assertIn("pane gone", out)

    # --- cancel ---

    @slow_tmux
    def test_cancel_stops_a_sleep_tree_and_a_setsid_escaped_child(self):
        a, b = self.tmp / "a.pid", self.tmp / "b.pid"
        proc = self.start("-k", "never", "--", "bash", "-c", ESCAPE % {"a": a, "b": b})
        pid = pane_id(proc)
        pa, pb = read_pid(a), read_pid(b)
        time.sleep(3)  # the descendant sampler runs about every 2 s
        code, out = self.sub("cancel", pid)
        self.assertEqual(code, 0)
        self.assertIn("sent INT", out)
        self.assertIn(str(pb), out)  # the escaped child is outside the group, so it is reported then reaped
        run_out, _ = proc.communicate(timeout=30)
        self.assertFalse(pid_alive(pa))
        self.assertFalse(pid_alive(pb))
        self.assertIn("exit=", run_out)

    @slow_tmux
    def test_cancel_by_title_and_with_nothing_registered(self):
        code, out = self.sub("cancel", "--all")
        self.assertEqual(code, 1)
        self.assertIn("no tmux-pane.sh runs", out)
        proc = self.start("-k", "never", "-t", "mytitle", "--", "sleep", "60")
        pane_id(proc)
        time.sleep(1.5)
        self.assertEqual(self.sub("cancel", "nope")[0], 1)
        self.assertEqual(self.sub("cancel", "mytitle")[0], 0)
        proc.communicate(timeout=30)

    @slow_tmux
    def test_cancel_closes_leader_and_followers_by_default_even_with_k_never(self):
        proc = self.start("-k", "never", "--pane", "echo before-cancel; sleep 60", "--pane", "sleep 61")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(3), 3)
        time.sleep(1.5)
        self.assertEqual(self.sub("cancel", leader)[0], 0)
        out, _ = proc.communicate(timeout=30)
        self.assertNotEqual(proc.returncode, 0)
        lines = out.splitlines()
        tail = next(i for i, line in enumerate(lines) if "before-cancel" in line)
        exit_i = next(i for i, line in enumerate(lines) if line.startswith("exit=") and line != "exit=0")
        closed_i = lines.index("pane %s closed (cancelled)" % leader)
        self.assertTrue(tail < exit_i < closed_i)
        self.assertEqual(self.wait_panes(1), 1)
        self.assertIn("no tmux-pane.sh panes", self.sub("list")[1])

    @slow_tmux
    def test_keep_on_cancel_keeps_everything_and_close_sweeps_it(self):
        proc = self.start("-k", "fail", "--keep-on-cancel", "--pane", "sleep 60", "--pane", "sleep 61")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(3), 3)
        time.sleep(1.5)
        self.sub("cancel", leader)
        out, _ = proc.communicate(timeout=30)
        self.assertIn("kept open", out)
        self.assertNotIn("closed (cancelled)", out)
        self.assertEqual(self.panes(), 3)
        listing = self.sub("list")[1]
        self.assertIn("kept (cancelled, exit", listing)
        time.sleep(0.5)
        self.assertIn("[cancelled", self.tmux("capture-pane", "-p", "-J", "-t", leader))
        self.assertEqual(self.sub("close")[0], 0)
        self.assertEqual(self.panes(), 1)

    def test_a_plain_failure_is_still_kept_and_a_plain_success_closed(self):
        p = self.start("-k", "fail", "--pane", "exit 4")
        out, _ = p.communicate(timeout=30)
        self.assertIn("kept open", out)
        self.assertNotIn("cancelled", self.sub("list")[1])
        self.sub("close")
        p = self.start("-k", "never", "--pane", "exit 0")
        out, _ = p.communicate(timeout=30)
        self.assertIn("closed", out)
        self.assertNotIn("(cancelled)", out)
        self.assertEqual(self.wait_panes(1), 1)

    # --- orphans, list and close ---

    @slow_tmux
    def test_orphan_report_lists_a_left_behind_sleep_and_reap_kills_it(self):
        f = self.tmp / "o.pid"
        code, out = self.run_cmd("bash", "-c", ORPHAN % f)
        orphan = read_pid(f)
        try:
            self.assertEqual(code, 0)
            self.assertIn("orphans", out)
            self.assertIn("pid %d" % orphan, out)
            self.assertNotIn("reaped", out)
            self.assertTrue(pid_alive(orphan))
        finally:
            os.kill(orphan, 9)
        g = self.tmp / "o2.pid"
        code, out = self.run_cmd("bash", "-c", ORPHAN.replace("4242", "4243") % g, flags=["--reap"])
        self.assertIn("pid %d" % read_pid(g), out)
        self.assertIn("reaped 1", out)
        time.sleep(0.3)
        self.assertFalse(pid_alive(read_pid(g)))

    def test_list_and_close_work_on_kept_panes(self):
        self.assertIn("no tmux-pane.sh panes", self.sub("list")[1])
        code, out = self.run_cmd("bash", "-c", "exit 3", flags=["-k", "always", "-t", "kept-one"])
        self.assertEqual(code, 3)
        self.assertIn("kept open", out)
        self.assertEqual(self.panes(), 2)
        listing = self.sub("list")[1]
        self.assertIn("kept-one", listing)
        self.assertIn("kept (exit 3)", listing)
        code, out = self.sub("close")
        self.assertEqual(code, 0)
        self.assertIn("closed %", out)
        self.assertEqual(self.panes(), 1)
        self.assertIn("no tmux-pane.sh panes", self.sub("list")[1])

    @slow_tmux
    def test_close_refuses_a_running_pane_and_closes_a_named_one(self):
        proc = self.start("-k", "never", "--", "sleep", "60")
        pid = pane_id(proc)
        time.sleep(1)
        code, out = self.sub("close", pid)
        self.assertEqual(code, 1)
        self.assertIn("still running", out)
        self.assertEqual(self.panes(), 2)
        self.assertIn("running", self.sub("list")[1])
        self.sub("cancel", pid)
        proc.communicate(timeout=30)

    # --- equal column widths ---

    def check_agent_and_run_columns_get_equal_widths(self, runs):
        procs = [self.start("-k", "never", "--pane", "sleep %d" % (60 + i), "--pane", "sleep %d" % (70 + i)) for i in range(runs)]
        leaders = [pane_id(p) for p in procs]
        self.assertEqual(self.wait_panes(1 + 2 * runs), 1 + 2 * runs)
        time.sleep(1.5)
        widths = wait_equal(self, runs + 1)
        self.assertEqual(len(widths), runs + 1)
        self.assertLessEqual(max(widths) - min(widths), runs + 1, widths)
        # Followers stay in their leader's column, with the leader's width.
        out = self.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_id} #{pane_width} #{@tmux_parent}")
        wid = {line.split()[0]: int(line.split()[1]) for line in out.splitlines()}
        for line in out.splitlines():
            f = line.split()
            if len(f) == 3:
                self.assertEqual(wid[f[0]], wid[f[2]])
        for leader in leaders:
            self.sub("cancel", leader)
        for p in procs:
            p.communicate(timeout=30)

    @slow_tmux
    def test_agent_and_run_columns_get_equal_widths_1(self):
        self.check_agent_and_run_columns_get_equal_widths(1)

    @slow_tmux
    def test_agent_and_run_columns_get_equal_widths_2(self):
        self.check_agent_and_run_columns_get_equal_widths(2)

    @slow_tmux
    def test_agent_and_run_columns_get_equal_widths_3(self):
        self.check_agent_and_run_columns_get_equal_widths(3)

    @slow_tmux
    def test_columns_are_equalised_again_after_a_run_closes(self):
        procs = [self.start("-k", "never", "--", "sleep", str(60 + i)) for i in range(3)]
        leaders = [pane_id(p) for p in procs]
        self.assertEqual(self.wait_panes(4), 4)
        self.assertEqual(len(wait_equal(self, 4)), 4)
        time.sleep(1.5)
        self.assertEqual(self.sub("cancel", leaders[1])[0], 0)
        procs[1].communicate(timeout=30)
        self.assertEqual(self.wait_panes(3), 3)
        widths = wait_equal(self, 3)
        self.assertEqual(len(widths), 3)
        self.assertLessEqual(max(widths) - min(widths), 3, widths)
        for i in (0, 2):
            self.sub("cancel", leaders[i])
            procs[i].communicate(timeout=30)
        self.assertEqual(self.wait_panes(1), 1)

    @slow_tmux
    def test_close_equalises_the_remaining_columns(self):
        self.run_cmd("bash", "-c", "exit 3", flags=["-k", "always", "-t", "kept-one"])
        proc = self.start("-k", "never", "--", "sleep", "60")
        leader = pane_id(proc)
        self.assertEqual(self.wait_panes(3), 3)
        self.assertEqual(len(wait_equal(self, 3)), 3)
        time.sleep(1.5)
        self.assertEqual(self.sub("close")[0], 0)
        widths = wait_equal(self, 2)
        self.assertEqual(len(widths), 2)
        self.assertLessEqual(max(widths) - min(widths), 2, widths)
        self.sub("cancel", leader)
        proc.communicate(timeout=30)

    def test_equalize_subcommand_rejects_a_missing_pane(self):
        self.assertEqual(self.sub("equalize", "%999")[0], 1)

    def test_equalize_failure_is_a_warning_not_an_exit_code(self):
        tmux_bin = Path(shutil.which("tmux"))
        shim = Path(self.env["TMPDIR"]) / "bin"
        shim.mkdir()
        (shim / "tmux").write_text(
            '#!/bin/bash\n[ "$1" = resize-pane ] && exit 1\nexec %s "$@"\n' % tmux_bin)
        (shim / "tmux").chmod(0o755)
        self.env["PATH"] = "%s:%s" % (shim, self.env["PATH"])
        r = subprocess.run([str(SCRIPT), "-k", "never", "--", "bash", "-c", "exit 5"], capture_output=True, text=True,
                           env=self.env, timeout=60)
        self.assertEqual(r.returncode, 5)
        self.assertIn("warning: could not resize", r.stderr)

    # --- copy / zoom / --read-only ---

    def _shim(self, body):
        """Puts a PATH shim directory in front; returns it."""
        shim = Path(self.env["TMPDIR"]) / "shim"
        shim.mkdir(exist_ok=True)
        (shim / body[0]).write_text(body[1])
        (shim / body[0]).chmod(0o755)
        self.env["PATH"] = "%s:%s" % (shim, self.env["PATH"])
        return shim

    def _kept_pane(self, title="cp"):
        proc = self.start("-k", "always", "-t", title, "--", "bash", "-c", "echo l1; echo l2; echo l3")
        pid = pane_id(proc)
        deadline = time.time() + 10
        while "l3" not in self.tmux("capture-pane", "-p", "-t", pid) and time.time() < deadline:
            time.sleep(0.1)
        return proc, pid

    def test_copy_pipes_the_scrollback_to_pbcopy_and_n_limits_it(self):
        out = self.tmp / "clip.txt"
        self._shim(("pbcopy", "#!/bin/bash\ncat > %s\n" % out))
        proc, pid = self._kept_pane()
        code, msg = self.sub("copy", "cp")
        self.assertEqual(code, 0)
        self.assertIn("l1", out.read_text())
        self.assertIn("lines copied", msg)
        code, msg = self.sub("copy", pid, "-n", "4")
        self.assertEqual(code, 0)
        self.assertNotIn("l1", out.read_text())
        self.assertIn("l3", out.read_text())
        self.sub("close", pid)
        proc.communicate(timeout=30)

    def test_copy_without_pbcopy_prints_the_text(self):
        proc, pid = self._kept_pane()
        shim = Path(self.env["TMPDIR"]) / "nopb"
        shim.mkdir()
        for tool in ("tmux", "bash", "env", "wc", "tr", "awk", "sort", "grep", "sed", "ps", "pgrep", "cat", "date", "mkdir", "rm", "mktemp", "basename", "dirname"):
            found = shutil.which(tool)
            if found:
                (shim / tool).symlink_to(found)
        self.env["PATH"] = str(shim)
        code, msg = self.sub("copy", pid)
        self.assertEqual(code, 0)
        self.assertIn("l2", msg)
        self.assertIn("no pbcopy", msg)
        self.sub("close", pid)
        proc.communicate(timeout=30)

    def test_copy_and_zoom_on_an_unknown_pane_fail(self):
        self.assertEqual(self.sub("copy", "nope")[0], 1)
        self.assertEqual(self.sub("zoom", "nope")[0], 1)

    def test_zoom_toggles_the_pane_zoom(self):
        proc, pid = self._kept_pane()
        flag = lambda: self.tmux("display-message", "-p", "-t", pid, "#{window_zoomed_flag}").strip()
        self.assertEqual(flag(), "0")
        self.assertEqual(self.sub("zoom", "cp")[0], 0)
        self.assertEqual(flag(), "1")
        self.assertEqual(self.sub("zoom", pid)[0], 0)
        self.assertEqual(flag(), "0")
        self.sub("close", pid)
        proc.communicate(timeout=30)

    def test_read_only_disables_input_on_the_leader_and_cancel_still_works(self):
        proc = self.start("-k", "never", "--read-only", "-t", "ro", "--", "sleep", "60")
        pid = pane_id(proc)
        time.sleep(1.5)
        self.assertEqual(self.tmux("display-message", "-p", "-t", pid, "#{pane_input_off}").strip(), "1")
        self.assertEqual(self.sub("cancel", "ro")[0], 0)
        proc.communicate(timeout=30)

    def test_input_is_enabled_by_default(self):
        proc = self.start("-k", "never", "-t", "rw", "--", "sleep", "60")
        pid = pane_id(proc)
        time.sleep(1.5)
        self.assertEqual(self.tmux("display-message", "-p", "-t", pid, "#{pane_input_off}").strip(), "0")
        self.assertEqual(self.sub("cancel", "rw")[0], 0)
        proc.communicate(timeout=30)

    # --- list ---

    @slow_tmux
    def test_list_has_no_stray_empty_line_after_real_panes(self):
        procs = [self.start("-k", "never", "--pane", "sleep %d" % (60 + i), "--pane", "sleep %d" % (70 + i)) for i in range(2)]
        leaders = [pane_id(p) for p in procs]
        self.assertEqual(self.wait_panes(5), 5)
        time.sleep(1.5)
        listing = self.sub("list")[1]
        self.assertNotIn("no tmux-pane.sh panes", listing)
        self.assertEqual(listing.count("running"), 2)
        self.assertEqual(listing.count("follower of"), 2)
        for leader in leaders:
            self.sub("cancel", leader)
        for p in procs:
            p.communicate(timeout=30)


if __name__ == "__main__":
    unittest.main()
