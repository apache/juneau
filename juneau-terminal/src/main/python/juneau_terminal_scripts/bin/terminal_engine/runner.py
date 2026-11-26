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
"""tmux-pane.sh with a command: open the leader (and follower) panes, track the process tree, block until the leader ends."""

import os
import shlex
import signal
import subprocess
import sys
import time

from . import ENGINE_PY, UsageError, script_path
from . import panes, procs, registry
from .tmux import show_option, tmux, tmux_out


class Options:
    def __init__(self):
        self.dir = os.getcwd()
        self.lines = "60"
        self.title = ""
        self.keep = "fail"
        self.keepmin = ""
        self.reap = False
        self.keeponcancel = False
        self.readonly = False
        self.cmdmode = False
        self.panes = []
        self.sizes = []
        self.ptitles = []
        self.rest = []


def chan_name(pid, rnd):
    return "tmux-pane-%d-%d" % (pid, rnd)


def _set(o, flag, value):
    if flag == "-d":
        o.dir = value
    elif flag == "-n":
        o.lines = value
    elif flag == "-t":
        o.title = value
    elif flag == "-k":
        o.keep = value
    elif flag == "-K":
        o.keepmin = value


def parse_options(argv):
    """The option loop of the old bash script, including every usage error it raised.  Raises UsageError."""
    o = Options()
    pending = ptitle = ""
    i = 0
    n = len(argv)
    while i < n:
        a = argv[i]
        if a == "--":
            o.cmdmode = True
            i += 1
            break
        if a == "--reap":
            o.reap = True
        elif a == "--keep-on-cancel":
            o.keeponcancel = True
        elif a == "--read-only":
            o.readonly = True
        elif a == "--pane":
            if i + 1 >= n:
                raise UsageError()
            o.panes.append(argv[i + 1])
            o.sizes.append(pending)
            o.ptitles.append(ptitle)
            pending = ptitle = ""
            i += 1
        elif a == "--pane-title":
            if i + 1 >= n:
                raise UsageError()
            ptitle = argv[i + 1]
            i += 1
        elif a == "--size":
            if i + 1 >= n:
                raise UsageError()
            v = argv[i + 1]
            digits = v[:-1] if v.endswith("%") else v
            if digits == v or not digits.isdigit() or not 1 <= int(digits) <= 99:
                raise UsageError()
            pending = digits
            i += 1
        elif a == "-f":
            print("tmux-pane.sh: -f was removed; use watch-run.sh to tail the log", file=sys.stderr)
            raise SystemExit(2)
        elif len(a) == 2 and a[0] == "-" and a[1] in "dntkK":
            if i + 1 >= n:
                raise UsageError()
            _set(o, a, argv[i + 1])
            i += 1
        elif len(a) > 2 and a[0] == "-" and a[1] in "dntkK":
            _set(o, a[:2], a[2:])
        elif a.startswith("-"):
            raise UsageError()
        else:
            o.cmdmode = True
            break
        i += 1
    o.rest = argv[i:]
    if o.cmdmode:
        if o.panes or not o.rest:
            raise UsageError()
    elif not o.panes or o.rest:
        raise UsageError()
    if pending or ptitle:
        raise UsageError()
    if o.sizes and (o.sizes[0] or o.ptitles[0]):
        raise UsageError()
    if o.keep not in ("fail", "always", "never"):
        raise UsageError()
    if o.keepmin and not o.keepmin.isdigit():
        raise UsageError()
    return o


def _pane_h(pane):
    h = tmux_out("display-message", "-p", "-t", pane, "#{pane_height}")
    return int(h) if h.isdigit() else 0


def _write_json(path, data):
    import json
    tmp = path + ".tmp"
    with open(tmp, "w") as f:
        json.dump(data, f)
    os.replace(tmp, path)


def run(argv):
    try:
        o = parse_options(argv)
    except SystemExit as e:
        return e.code
    if not os.environ.get("TMUX"):
        print("tmux-pane.sh: not inside tmux", file=sys.stderr)
        return 2
    import random
    import shutil
    import tempfile
    self_path = script_path("tmux-pane.sh")
    tmpdir = os.environ.get("TMPDIR") or "/tmp"
    try:
        grace = int(os.environ.get("TMUX_PANE_GRACE") or 10)
    except ValueError:
        grace = 10
    if o.cmdmode:
        cmd = list(o.rest)
        cmdline = " ".join(o.rest)
        title = o.title or os.path.basename(o.rest[0].rstrip("/")) or o.rest[0]
    else:
        cmd = ["bash", "-c", o.panes[0]]
        cmdline = o.panes[0]
        title = o.title or "bash"
    state = tempfile.mkdtemp(prefix="tmux-pane.", dir=tmpdir)
    chan = chan_name(os.getpid(), random.randrange(32768))
    # The panes' shells get the tmux server's environment, not ours, so pass the caller's env through explicitly (one file,
    # shared by the leader and every follower).
    _write_json(os.path.join(state, "env.json"), dict(os.environ))
    _write_json(os.path.join(state, "spec.json"),
                {"dir": o.dir, "title": title, "cmd": cmd, "self": self_path, "chan": chan, "readonly": o.readonly,
                 "keepmin": int(o.keepmin) if o.keepmin else None})
    agent = os.environ.get("TMUX_PANE", "")
    leader_cmd = "%s %s leader %s" % (shlex.quote(sys.executable), shlex.quote(ENGINE_PY), shlex.quote(state))
    rc, out = tmux("split-window", "-h", "-d", "-l", "50%", "-P", "-F", "#{pane_id}", leader_cmd)
    pane = out.strip()
    if rc != 0 or not pane:
        shutil.rmtree(state, ignore_errors=True)
        return 2
    win = tmux_out("display-message", "-p", "-t", pane, "#{window_id}")
    print("pane %s: %s" % (pane, title))
    print("cancel: %s cancel %s" % (self_path, pane))
    ident = pane[1:] if pane.startswith("%") else pane
    regdir = registry.default_dir()
    os.makedirs(regdir, exist_ok=True)
    reg = os.path.join(regdir, ident + ".reg")
    pids = os.path.join(regdir, ident + ".pids")
    cancelflag = os.path.join(regdir, ident + ".cancel")
    started = int(time.time())
    tmux("set-option", "-p", "-t", pane, "@tmux_pane", "1", ";", "set-option", "-p", "-t", pane, "@tmux_title", title, ";",
         "set-option", "-p", "-t", pane, "@tmux_state", "running", ";", "set-option", "-p", "-t", pane, "@tmux_start", str(started), ";",
         "set-option", "-p", "-t", pane, "@tmux_reg", regdir, ";", "set-option", "-p", "-t", pane, "@tmux_agent", agent)
    tmux("select-pane", "-T", title, "-t", pane)
    panes.border_frame(pane)
    if win:
        panes.border_setup(win)
        panes.equalize(win, agent)
    reg_fields = {"pane": pane, "title": title, "callerpid": os.getpid(), "cmdline": cmdline, "started": started, "rd": regdir}
    registry.write_reg(reg, reg_fields)
    open(pids, "w").close()

    def on_signal(signum, frame):
        raise SystemExit(128 + signum)
    for s in (signal.SIGINT, signal.SIGTERM, signal.SIGHUP):
        signal.signal(s, on_signal)
    try:
        return _supervise(o, state, chan, pane, win, agent, title, regdir, reg, reg_fields, pids, cancelflag, grace, self_path)
    finally:
        for p in (reg, reg + ".tmp", pids, cancelflag):
            try:
                os.unlink(p)
            except OSError:
                pass
        shutil.rmtree(state, ignore_errors=True)


def _supervise(o, state, chan, pane, win, agent, title, regdir, reg, reg_fields, pids, cancelflag, grace, self_path):
    followers = []
    unsized = [True]

    def add_follower(index, cmd, size, ftitle):
        lh = _pane_h(pane)
        colh = lh
        target = pane
        live = []
        for f in followers:                      # followers that already ended (a quick command) are not part of the column any more
            if tmux("display-message", "-p", "-t", f, "#{pane_id}")[0] != 0:
                continue
            live.append(f)
            colh += _pane_h(f) + 1
            target = f
        k = len(live)
        region = colh - lh - 1
        if size:
            want = colh * int(size) // 100
            unsized[0] = False
        elif k == 0:
            want = colh * 40 // 100
        else:
            want = (region - k) // (k + 1)
        th = _pane_h(target)
        if want > th - 2:
            want = th - 2
        if want < 1:
            print("tmux-pane.sh: no room for another pane; skipped: %s" % cmd, file=sys.stderr)
            return
        # One spec file per follower, written whole before its pane exists.
        _write_json(os.path.join(state, "follower-%d.json" % index), {"dir": o.dir, "self": self_path, "leader": pane, "cmd": cmd})
        fcmd = "%s %s follower %s %d" % (shlex.quote(sys.executable), shlex.quote(ENGINE_PY), shlex.quote(state), index)
        rc, out = tmux("split-window", "-v", "-d", "-l", str(want), "-t", target, "-P", "-F", "#{pane_id}", fcmd)
        fp = out.strip()
        if rc != 0 or not fp:
            return
        followers.append(fp)
        tmux("select-pane", "-T", ftitle or cmd[:40], "-t", fp)
        panes.border_frame(fp)
        tmux("set-option", "-p", "-t", fp, "@tmux_pane", "1", ";", "set-option", "-p", "-t", fp, "@tmux_state", "follower", ";",
             "set-option", "-p", "-t", fp, "@tmux_parent", pane, ";", "set-option", "-p", "-t", fp, "@tmux_start", str(int(time.time())), ";",
             "set-option", "-p", "-t", fp, "@tmux_reg", regdir)
        if unsized[0] and k >= 1:                # with no explicit sizes the follower area is shared evenly
            each = (region - k) // (k + 1)
            for lp in live:
                tmux("resize-pane", "-y", str(each), "-t", lp)

    for i in range(1, len(o.panes)):
        add_follower(i, o.panes[i], o.sizes[i], o.ptitles[i])
    if o.readonly:
        for p in [pane] + followers:
            tmux("select-pane", "-d", "-t", p)

    def kill_all():
        for p in [pane] + followers:
            tmux("kill-pane", "-t", p)
        if win:
            panes.equalize(win, agent)
            panes.border_restore(win)

    # Wait for the pane's signal, but notice the pane disappearing (it would never signal) and sample the process tree.
    waiter = subprocess.Popen(["tmux", "wait-for", chan], stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    gone = False
    tick = 0
    sawcancel = False
    cpid = None
    cpid_file = os.path.join(state, "cpid")
    try:
        while waiter.poll() is None:
            try:
                waiter.wait(timeout=0.2)
            except subprocess.TimeoutExpired:
                pass
            tick += 1
            if os.path.exists(cancelflag):
                sawcancel = True
            if cpid is None and os.path.exists(cpid_file) and os.path.getsize(cpid_file) > 0:
                with open(cpid_file) as f:
                    cpid = f.read().strip()
                reg_fields.update({"pgid": cpid, "cpid": cpid, "cpidstart": procs.lstart(cpid)})
                registry.write_reg(reg, reg_fields)
                procs.track(cpid, pids)
            if tick % 10 == 0:
                if cpid:
                    procs.track(cpid, pids)
                if pane not in tmux_out("list-panes", "-a", "-F", "#{pane_id}").split():
                    gone = True
                    break
    except SystemExit:
        waiter.kill()
        raise
    if os.path.exists(cancelflag):
        sawcancel = True
    # A cancel in progress does the reaping and the reporting; give it time to finish so the two don't race.
    for _ in range((grace * 2 + 5) * 5):
        if not os.path.exists(cancelflag):
            break
        time.sleep(0.2)
    if gone:
        waiter.kill()
        waiter.wait()
        print("pane gone")
        procs.report(pids, o.reap, grace)
        kill_all()
        return 129
    waiter.wait()
    try:
        with open(os.path.join(state, "exit")) as f:
            rc = int(f.read().strip())
    except (OSError, ValueError):
        rc = 1
    # The cancel sets the pane option before it signals anything; the flag seen while polling is only a fallback.
    cancelled = bool(show_option(pane, "@tmux_cancelled")) or sawcancel
    captured = tmux_out("capture-pane", "-p", "-J", "-t", pane, "-S", "-%s" % o.lines)
    lines = captured.split("\n")
    while lines and lines[-1] == "":
        lines.pop()
    for line in lines:
        print(line)
    procs.report(pids, o.reap, grace)
    print("exit=%d" % rc)
    if cancelled and not o.keeponcancel:
        kill_all()
        print("pane %s closed (cancelled)" % pane)
    elif o.keep == "never" or (o.keep == "fail" and rc == 0):
        kill_all()
        print("pane %s closed" % pane)
    else:
        tmux("set-option", "-p", "-t", pane, "@tmux_state",
             "kept (cancelled, exit %d)" % rc if cancelled else "kept (exit %d)" % rc)
        print("pane %s kept open (press Enter in it to close, or: %s close %s)" % (pane, self_path, pane))
    return rc
