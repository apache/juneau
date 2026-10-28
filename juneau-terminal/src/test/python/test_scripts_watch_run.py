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
"""watch-run.sh on a throwaway tmux server (-L), skipped where no private server can start."""

import re
import subprocess
import time
import unittest
from pathlib import Path

from tmux_support import WATCH_RUN_SH, WatchCase, requires_tmux, slow_tmux

SCRIPT = WATCH_RUN_SH

WRITE_RESULT = """import json, os, sys
json.dump({"v": 1, "status": "fail", "exit": 3, "ms": 12,
           "rows": [{"id": "b", "label": "Build", "status": "fail", "ms": 5, "summary": "boom"}],
           "notes": [{"level": "error", "text": "it broke"}], "logs": {"full": "/x/full.log"}},
          open(os.environ["JUNEAU_TERMINAL_RESULT"], "w"))
print("pane-only-text")
sys.exit(3)
"""

FIXTURE = (b"plain\ttab line\n"
           b"ls\xe2\x80\xa8sep\xe2\x80\xa9para\xe2\x80\x8bzw\xc2\xadsoft\xef\xbb\xbfbom\n"
           b"bare\rcr\x7fdel\x01ctl\n"
           b"\x1b[31mred\x1b[0m ok\n"
           b"bad\xff\xfeutf8 then still going\n"
           b"last line\n")


def tail_path(out):
    m = re.search(r"^tail -f (\S+)$", out, re.M)
    assert m, out
    return Path(m.group(1))


def script(tmp_path, name, body):
    f = tmp_path / name
    f.write_text(body)
    return str(f)


def run_filter(data):
    cmd = subprocess.run([str(SCRIPT), "--print-tail-filter"], capture_output=True, text=True, check=True).stdout.strip()
    return subprocess.run(["bash", "-c", cmd], input=data, capture_output=True, timeout=30).stdout


@requires_tmux
class WatchRunTest(WatchCase):

    def pane_ids(self):
        return set(self.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_id}").split())

    def wait_for_panes(self, n):
        deadline = time.time() + 10
        while self.panes() < n and time.time() < deadline:
            time.sleep(0.1)

    def follower_pane(self):
        rows = self.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_id}|#{@tmux_state}").splitlines()
        return next(p for p in rows if p.endswith("|follower")).split("|")[0]

    def test_not_inside_tmux_exits_2(self):
        env = {k: v for k, v in self.env.items() if k != "TMUX"}
        r = subprocess.run([str(SCRIPT), "--", "true"], capture_output=True, text=True, env=env)
        self.assertEqual(r.returncode, 2)
        self.assertIn("not inside tmux", r.stderr)

    def test_usage_error_without_a_command(self):
        self.assertEqual(self.run_watch("--no-tail")[0], 2)

    def test_paths_stem_pattern_and_pretouch_before_the_pane_opens(self):
        tmp_path = self.tmp
        out_file = tmp_path / "seen.txt"
        probe = script(tmp_path, "push.py",
                       "import os\n"
                       "open(%r, 'w').write(' '.join(os.path.exists(os.environ[k]) and 'Y' or 'N' for k in "
                       "('JUNEAU_TERMINAL_FULL_LOG', 'JUNEAU_TERMINAL_BOARD_LOG')) + ' ' + os.environ['JUNEAU_TERMINAL_FULL_LOG'] + ' ' "
                       "+ os.environ['JUNEAU_TERMINAL_BOARD_LOG'])\n" % str(out_file))
        code, out, _ = self.run_watch("-k", "never", "--", "python3", probe)
        self.assertEqual(code, 0)
        full = tail_path(out)
        self.assertRegex(full.name, r"push-\d{8}-\d{6}-\d+\.log")
        self.assertEqual(full.parent, self.logs)
        self.assertIn("board: %s.board.log" % full.with_suffix(""), out)
        y1, y2, f, c = out_file.read_text().split()
        self.assertEqual((y1, y2), ("Y", "Y"))
        self.assertEqual(f, str(full))
        self.assertTrue(c.endswith(".board.log"))
        self.assertEqual(c[:-10], str(full)[:-4])
        # the tail line is printed before the pane runner's own output
        self.assertLess(out.index("tail -f "), out.index("pane %"))

    def test_leader_env_quoting_and_cancel_mapping(self):
        tmp_path = self.tmp
        seen = tmp_path / "env.txt"
        probe = script(tmp_path, "probe.sh",
                       'printf "%s\\n" "$1" "$JUNEAU_TERMINAL_RESULT" "$JUNEAU_TERMINAL_CANCEL" "$TMUX_PANE_RUN_CANCEL" > ' + str(seen) + "\n")
        tricky = "it's a \"quoted\" arg  with   spaces $HOME"
        code, out, _ = self.run_watch("-k", "never", "--", "bash", probe, tricky)
        self.assertEqual(code, 0)
        arg, result, cancel, run_cancel = seen.read_text().splitlines()
        self.assertEqual(arg, tricky)
        self.assertTrue(result.endswith("/result.json"))
        self.assertIn("/watch-run.", result)
        self.assertEqual(cancel, run_cancel)
        self.assertIn("tmux-pane.sh", cancel)
        self.assertIn(" cancel %", cancel)

    @slow_tmux
    def test_result_dir_is_removed_and_concurrent_runs_get_different_paths(self):
        tmp_path = self.tmp
        seen = tmp_path / "res.txt"
        probe = script(tmp_path, "slow.sh", 'echo "$JUNEAU_TERMINAL_RESULT $JUNEAU_TERMINAL_FULL_LOG" >> %s; sleep 2\n' % seen)
        a = self.popen("-k", "never", "--", "bash", probe)
        b = self.popen("-k", "never", "--", "bash", probe)
        oa, _ = a.communicate(timeout=60)
        ob, _ = b.communicate(timeout=60)
        self.assertNotEqual(tail_path(oa), tail_path(ob))
        lines = seen.read_text().splitlines()
        self.assertEqual(len(lines), 2)
        self.assertEqual(len({ln.split()[0] for ln in lines}), 2)
        self.assertEqual(len({ln.split()[1] for ln in lines}), 2)
        for ln in lines:
            self.assertFalse(Path(ln.split()[0]).parent.exists())

    @slow_tmux
    def test_follower_tails_the_same_log_and_shows_appended_content(self):
        probe = script(self.tmp, "tailme.sh", 'sleep 1; echo from-the-command >> "$JUNEAU_TERMINAL_FULL_LOG"; sleep 4\n')
        proc = self.popen("-k", "never", "--", "bash", probe)
        self.wait_for_panes(3)
        self.assertEqual(self.panes(), 3)
        time.sleep(2.5)
        self.assertIn("from-the-command", self.tmux("capture-pane", "-p", "-J", "-t", self.follower_pane()))
        # the pipeline's foreground process is the pane's bash, so look for tail and the filter among the pane's descendants
        ps = subprocess.run(["ps", "-axo", "command"], capture_output=True, text=True, errors="replace").stdout
        self.assertIn("tail -F", ps)
        self.assertIn("perl -MEncode", ps)
        out, _ = proc.communicate(timeout=60)
        self.assertEqual(proc.returncode, 0)
        self.assertEqual(self.wait_panes(1), 1)

    @slow_tmux
    def test_no_tail_creates_no_follower(self):
        probe = script(self.tmp, "nt.sh", "sleep 2\n")
        proc = self.popen("--no-tail", "-k", "never", "--", "bash", probe)
        time.sleep(1.5)
        self.assertEqual(self.panes(), 2)
        proc.communicate(timeout=60)

    def test_summary_follows_the_pane_tail_with_exit_last(self):
        fx = script(self.tmp, "fx.py", WRITE_RESULT)
        code, out, _ = self.run_watch("-k", "never", "--", "python3", fx)
        self.assertEqual(code, 3)
        lines = out.splitlines()
        self.assertIn("pane-only-text", out)
        self.assertIn("result: status=fail exit=3 ms=12", lines)
        self.assertIn("fail  Build (5ms) boom", out)
        self.assertIn("error: it broke", out)
        self.assertIn("log.full: /x/full.log", out)
        self.assertLess(out.index("pane-only-text"), out.index("result: status=fail"))
        self.assertEqual(lines[-1], "exit=3")

    def test_no_result_or_bad_json_prints_no_summary_and_no_error(self):
        code, out, err = self.run_watch("-k", "never", "--", "bash", "-c", "echo hello; exit 4")
        self.assertEqual(code, 4)
        self.assertNotIn("result:", out)
        self.assertIn("hello", out)
        self.assertEqual(out.splitlines()[-1], "exit=4")
        self.assertFalse(err.strip())
        code, out, err = self.run_watch("-k", "never", "--", "bash", "-c", 'echo "{not json" > "$JUNEAU_TERMINAL_RESULT"')
        self.assertEqual(code, 0)
        self.assertNotIn("result:", out)
        self.assertFalse(err.strip())

    @slow_tmux
    def test_cancel_closes_panes_removes_the_temp_dir_and_exits_nonzero(self):
        seen = self.tmp / "res.txt"
        probe = script(self.tmp, "long.sh", 'echo "$JUNEAU_TERMINAL_RESULT" > %s; sleep 60\n' % seen)
        proc = self.popen("-k", "never", "--", "bash", probe)
        first = proc.stdout.readline()
        self.assertTrue(first.startswith("tail -f "))
        deadline = time.time() + 10
        while not seen.exists() and time.time() < deadline:
            time.sleep(0.1)
        time.sleep(1)
        code, out = self.pane_sub("cancel", "--all")
        self.assertEqual(code, 0)
        self.assertIn("sent INT", out)
        rest, _ = proc.communicate(timeout=60)
        self.assertNotEqual(proc.returncode, 0)
        self.assertIn("closed (cancelled)", rest)
        self.assertTrue(rest.splitlines()[-1].startswith("exit="))
        self.assertEqual(self.wait_panes(1), 1)
        self.assertFalse(Path(seen.read_text().strip()).parent.exists())

    def test_exit_code_is_the_leaders_not_the_followers(self):
        code, out, _ = self.run_watch("-k", "never", "--", "bash", "-c", "exit 6")
        self.assertEqual(code, 6)
        self.assertEqual(out.splitlines()[-1], "exit=6")

    def test_tail_filter_sanitizes_format_and_control_characters(self):
        out = run_filter(FIXTURE).decode("utf-8")
        lines = out.split("\n")
        self.assertEqual(lines[0], "plain\ttab line")
        self.assertEqual(lines[1], "ls sep para zw soft bom")
        self.assertEqual(lines[2], "barecrdelctl")
        self.assertEqual(lines[3], "\x1b[31mred\x1b[0m ok")
        self.assertTrue(lines[4].startswith("bad"))
        self.assertTrue(lines[4].endswith("utf8 then still going"))
        self.assertEqual(lines[5], "last line")

    def test_tail_filter_survives_invalid_utf8_and_leaves_the_log_alone(self):
        log = self.tmp / "x.log"
        log.write_bytes(FIXTURE)
        out = run_filter(log.read_bytes())
        self.assertTrue(out.endswith(b"last line\n"))
        self.assertEqual(log.read_bytes(), FIXTURE)

    @slow_tmux
    def test_follower_pane_filters_the_log_it_tails(self):
        probe = script(self.tmp, "fl.sh", 'sleep 1; printf "wide\\xe2\\x80\\xa8line\\n" >> "$JUNEAU_TERMINAL_FULL_LOG"; sleep 4\n')
        proc = self.popen("-k", "never", "--", "bash", probe)
        self.wait_for_panes(3)
        self.assertEqual(self.panes(), 3)
        time.sleep(2.5)
        text = self.tmux("capture-pane", "-p", "-J", "-t", self.follower_pane())
        self.assertIn("wide line", text)
        self.assertNotIn("\u2028", text)
        proc.communicate(timeout=60)

    def test_a_kept_pane_from_an_earlier_run_is_closed_before_the_new_pane_opens(self):
        self.assertEqual(self.run_watch("-k", "always", "--", "true")[0], 0)
        old = self.pane_ids() - {"%0"}
        self.assertEqual(len(old), 2)  # kept leader and its follower
        seen = self.tmp / "ids.txt"
        probe = script(self.tmp, "later.sh", 'tmux list-panes -s -t t -F "#{pane_id}" > %s\n' % seen)
        self.assertEqual(self.run_watch("-k", "never", "--", "bash", probe)[0], 0)
        self.assertFalse(old & set(seen.read_text().split()))

    def test_no_close_kept_leaves_a_kept_pane_from_an_earlier_run_open(self):
        self.assertEqual(self.run_watch("-k", "always", "--", "true")[0], 0)
        old = self.pane_ids() - {"%0"}
        self.assertEqual(len(old), 2)  # kept leader and its follower
        seen = self.tmp / "ids.txt"
        probe = script(self.tmp, "later.sh", 'tmux list-panes -s -t t -F "#{pane_id}" > %s\n' % seen)
        self.assertEqual(self.run_watch("--no-close-kept", "-k", "never", "--", "bash", probe)[0], 0)
        self.assertLessEqual(old, set(seen.read_text().split()))
        self.assertLessEqual(old, self.pane_ids())

    @slow_tmux
    def test_a_running_pane_is_left_alone_by_the_cleanup(self):
        slow = script(self.tmp, "slow.sh", "sleep 5\n")
        proc = self.popen("-k", "never", "--", "bash", slow)
        self.wait_for_panes(3)
        running = self.pane_ids() - {"%0"}
        self.assertEqual(len(running), 2)
        self.assertEqual(self.run_watch("-k", "never", "--", "true")[0], 0)
        self.assertLessEqual(running, self.pane_ids())
        proc.communicate(timeout=60)
        self.assertEqual(proc.returncode, 0)

    def test_a_failing_cleanup_warns_and_the_launch_continues(self):
        d = self.tmp / "copy"
        d.mkdir()
        for name in ("watch-run.sh", "tmux-pane.sh"):
            (d / name).write_text((SCRIPT.parent / name).read_text())
            (d / name).chmod(0o755)
        real = d / "tmux-pane-real.sh"
        (d / "tmux-pane.sh").rename(real)
        (d / "tmux-pane.sh").write_text(
            '#!/bin/bash\n[ "${1:-}" != close ] || { echo "boom" >&2; exit 1; }\nexec %s "$@"\n' % real)
        (d / "tmux-pane.sh").chmod(0o755)
        r = subprocess.run([str(d / "watch-run.sh"), "-k", "never", "--", "bash", "-c", "echo launched"],
                           capture_output=True, text=True, env=self.env, timeout=60)
        self.assertEqual(r.returncode, 0)
        self.assertIn("exit=0", r.stdout)
        self.assertIn("warning", r.stderr)
        self.assertIn("leftover", r.stderr)


if __name__ == "__main__":
    unittest.main()
