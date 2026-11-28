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
"""watch-run.sh: run a juneau-terminal based script in a watchable tmux pane with its full log tailed below, and print the result summary."""

import os
import re
import shlex
import signal
import subprocess
import sys
import time

from . import UsageError, script_path
from .out import emit, write_exit_code
from .tmux import tmux_out

TAIL_FILTER = ("perl -MEncode -e '$|=1; while (<STDIN>) { $_ = decode(\"UTF-8\", $_); s/[\\x{2028}\\x{2029}\\p{Cf}]/ /g; "
               "s/[\\x00-\\x08\\x0b-\\x1a\\x1c-\\x1f\\x7f]//g; print encode(\"UTF-8\", $_) }'")
SCRIPT_EXT = (".py", ".sh", ".bash", ".js", ".mjs", ".rb", ".pl", ".ts")


def tmux_version_ok(text):
    """True when `tmux -V` text names version 3.4 or newer ("tmux 3.7c", "tmux next-3.5").  Text with no version passes."""
    v = text[5:] if text.startswith("tmux ") else text
    if v.startswith("next-"):
        v = v[5:]
    m = re.match(r"([0-9]+)\.([0-9]*)", v)
    if not m:
        return True
    major = int(m.group(1))
    minor = int(m.group(2)) if m.group(2) else 0
    return major > 3 or (major == 3 and minor >= 4)


def safe_name(s):
    """juneau-terminal's safe_name: runs of anything but word characters and dots become one `-`, edges trimmed, empty is `run`."""
    s = re.sub(r"[^\w.]+", "-", s)
    s = re.sub(r"^[-.]+", "", s)
    s = re.sub(r"[-.]+$", "", s)
    return s or "run"


def script_stem(args):
    """The first script-like word of the command (not an option, no whitespace or `=`, containing `/` or ending in a script
    extension), else the first word."""
    pick = None
    for a in args:
        if not a or a.startswith("-") or re.search(r"\s", a) or "=" in a:
            continue
        if "/" in a or a.endswith(SCRIPT_EXT):
            pick = a
            break
    if pick is None:
        pick = args[0]
    b = pick.rsplit("/", 1)[-1]
    if not b.startswith(".") and "." in b:
        b = b.rsplit(".", 1)[0]
    return safe_name(b)


def result_summary(r):
    out = ["result: status=%s exit=%s ms=%s" % (r.get("status"), r.get("exit"), r.get("ms"))]
    for row in r.get("rows", []):
        t = row.get("tests")
        tests = " tests=%s fail=%s err=%s skip=%s" % (t["total"], t["fail"], t["err"], t["skip"]) if t else ""
        label = row.get("label") or row.get("id")
        out.append(("  %-5s %s (%sms)%s %s" % (row.get("status", "?"), label, row.get("ms"), tests, row.get("summary") or "")).rstrip())
    for n in r.get("notes", []):
        out.append("  %s: %s" % (n.get("level"), n.get("text")))
    for k, v in r.get("logs", {}).items():
        out.append("  log.%s: %s" % (k, v))
    return "\n".join(out)


ERROR_WORDS = re.compile(r"error|fail|exception|traceback|fatal|refused|cannot|denied", re.I)


def failure_block(full, limit=15):
    """Lines telling where a failed run's log is and what went wrong: its path, then the last error-looking lines (else the last lines)."""
    try:
        with open(full, errors="replace") as f:
            lines = [l.rstrip() for l in f.readlines()[-4000:] if l.strip()]
    except OSError:
        lines = []
    hits = [l for l in lines if ERROR_WORDS.search(l)] or lines
    out = ["full log: %s" % full]
    if hits:
        out.append("last error lines:")
        out += ["  " + l[:300] for l in hits[-limit:]]
    return out


def keeping(keep, keep_flag):
    """True when the panes stay after the run: --keep, or an explicit -k always or fail."""
    return keep_flag or keep in ("always", "fail")


def main(argv):
    no_tail = False
    keep_flag = False
    keep = None
    close_kept = True
    title = ""
    toolbar = os.environ.get("JUNEAU_TERMINAL_TOOLBAR", "1") != "0"
    passthru = []
    i = 0
    n = len(argv)
    while i < n:
        a = argv[i]
        if a == "--":
            i += 1
            break
        if a == "--no-tail":
            no_tail = True
        elif a == "--no-close-kept":
            close_kept = False
        elif a == "--keep":
            keep_flag = True
        elif a == "--no-toolbar":
            toolbar = False
        elif a == "--check-tmux-version":
            return 0 if tmux_version_ok(argv[i + 1] if i + 1 < n else "") else 1
        elif a == "--print-tail-filter":
            print(TAIL_FILTER)
            return 0
        elif a in ("--reap", "--keep-on-cancel", "--read-only"):
            passthru.append(a)
        elif len(a) == 2 and a[0] == "-" and a[1] in "dntkK":
            if i + 1 >= n:
                raise UsageError()
            if a == "-t":
                title = argv[i + 1]
            if a == "-k":
                keep = argv[i + 1]
            passthru += [a, argv[i + 1]]
            i += 1
        elif len(a) > 2 and a[0] == "-" and a[1] in "dntkK":
            if a[:2] == "-t":
                title = a[2:]
            if a[:2] == "-k":
                keep = a[2:]
            passthru.append(a)
        else:
            raise UsageError()
        i += 1
    cmd = argv[i:]
    if not cmd:
        raise UsageError()
    if not os.environ.get("TMUX"):
        print("watch-run.sh: not inside tmux", file=sys.stderr)
        return 2
    if toolbar and not tmux_version_ok(tmux_out("-V")):
        print("watch-run.sh: warning: the toolbar needs tmux 3.4 or newer; opening without it", file=sys.stderr)
        toolbar = False
    import json
    import shutil
    import tempfile
    stem = script_stem(cmd)
    if not title:
        passthru += ["-t", stem]
    if keep is None:
        keep = "always" if keep_flag else "never"
        passthru += ["-k", keep]
    kept = keeping(keep, False)
    passthru += ["--hide-launcher", "--quiet-exit"]
    logdir = os.environ.get("JUNEAU_TERMINAL_LOG_DIR") or os.path.join(os.path.expanduser("~"), ".cache", "juneau-terminal")
    try:
        os.makedirs(logdir, exist_ok=True)
    except OSError:
        return 1
    logdir = os.path.abspath(logdir)
    base = os.path.join(logdir, "%s-%s-%d" % (stem, time.strftime("%Y%m%d-%H%M%S"), os.getpid()))
    full, board = base + ".log", base + ".board.log"
    rd = tempfile.mkdtemp(prefix="watch-run.", dir=os.environ.get("TMPDIR") or "/tmp")
    result = os.path.join(rd, "result.json")
    try:
        for p in (full, board):
            open(p, "a").close()
        emit("tail -f %s" % shlex.quote(full))
        emit("board: %s" % shlex.quote(board))
        # Two levels of `bash -c` (the pane's wrapper, then this string), so every value is quoted.  JUNEAU_TERMINAL_CANCEL is
        # left as a reference, expanded inside the pane where the leader has already exported TMUX_PANE_RUN_CANCEL.
        cmdq = " ".join(shlex.quote(a) for a in cmd)
        leader = ("JUNEAU_TERMINAL_FULL_LOG=%s JUNEAU_TERMINAL_BOARD_LOG=%s JUNEAU_TERMINAL_RESULT=%s "
                  "JUNEAU_TERMINAL_CANCEL=\"$TMUX_PANE_RUN_CANCEL\" %s"
                  % (shlex.quote(full), shlex.quote(board), shlex.quote(result), cmdq))
        args = passthru + ["--pane", leader]
        if not no_tail:
            args += ["--pane-title", os.path.basename(full), "--pane", "tail -F %s | %s" % (shlex.quote(full), TAIL_FILTER)]
        # The toolbar is the last follower.  The panes must outlive the run (the toolbar shows its result), and a cancel must not
        # close them, so -k always and --keep-on-cancel come last and win over anything the caller passed.
        if toolbar:
            args += ["--pane-title", "controls", "--pane",
                     "exec %s --title %s" % (shlex.quote(script_path("tmux-toolbar.sh")), shlex.quote(title or stem))]
            if kept:
                args += ["-k", keep, "--keep-on-cancel"]
        pane_sh = script_path("tmux-pane.sh")
        # Clear finished (kept) panes from earlier runs first; a failed cleanup only warns.
        if close_kept:
            try:
                ok = subprocess.run([pane_sh, "close", "--kept"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0
            except OSError:
                ok = False
            if not ok:
                print("watch-run.sh: warning: could not close leftover panes", file=sys.stderr)
        # Not exec'd, so the summary can follow; INT and TERM are forwarded so the pane runner cleans up.
        child = subprocess.Popen([pane_sh] + args)
        old = {s: signal.signal(s, lambda signum, frame: child.terminate()) for s in (signal.SIGINT, signal.SIGTERM)}
        rc = child.wait()
        for s, h in old.items():
            signal.signal(s, h)
        # The summary supplements the pane tail; a missing or unparseable result file is silent.
        code = rc if rc >= 0 else 128 - rc
        if kept and (keep != "fail" or code != 0):          # the panes are still up; with -k fail a clean run closed them
            try:
                if os.path.getsize(result) > 0:
                    with open(result) as f:
                        text = result_summary(json.load(f))
                    emit(text)
            except Exception:
                pass
        if code != 0:
            for line in failure_block(full):
                emit(line)
        emit("exit=%d" % code)
        write_exit_code(code)
        return code
    finally:
        shutil.rmtree(rd, ignore_errors=True)
