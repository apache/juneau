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
"""Pane bookkeeping: the @tmux_* pane options, list/close, column widths, titled borders, copy and zoom."""

import subprocess
import sys
import time

from . import UsageError
from .tmux import option_is_set, show_option, tmux, tmux_out, warn

FIELDS = ("one", "id", "state", "parent", "start", "title")
BORDER_FORMAT = ('─┤#{?pane_active,#[reverse],} #{pane_title} #[default]├─#{?#{mouse},#[align=right]#[range=control|8]'
                 '[#{?window_zoomed_flag,u,z}]#[norange]#[range=control|9][x]#[norange],}')


def pct(target):
    """`%${target#%}`: a pane id with exactly one leading %."""
    return "%" + (target[1:] if target.startswith("%") else target)


def pane_rows():
    """The toolkit panes (@tmux_pane=1) of every window, as dicts with keys one, id, state, parent, start, title."""
    text = tmux_out("list-panes", "-a", "-F", "#{@tmux_pane}|#{pane_id}|#{@tmux_state}|#{@tmux_parent}|#{@tmux_start}|#{@tmux_title}")
    rows = []
    for line in text.splitlines():
        f = (line.split("|", 5) + [""] * 6)[:6]
        if f[0] == "1":
            rows.append(dict(zip(FIELDS, f)))
    return rows


def format_age(d):
    if d >= 3600:
        return "%dh%dm" % (d // 3600, d % 3600 // 60)
    if d >= 60:
        return "%dm" % (d // 60)
    return "%ds" % d


def cmd_list():
    now = int(time.time())
    n = 0
    for r in pane_rows():
        n += 1
        start = int(r["start"]) if r["start"].isdigit() else now
        title = "follower of %s" % r["parent"] if r["state"] == "follower" else r["title"]
        print("%-6s %-8s %-6s %s" % (r["id"], r["state"], format_age(now - start), title))
    if n == 0:
        print("no tmux-pane.sh panes")
    return 0


def equal_columns(text, agent, excl=""):
    """From `list-panes` text (left|id|width|@tmux_pane|@tmux_state): the (pane, width) resizes that give the agent pane and every
    leader (a follower shares its leader's column) equal widths, left to right; the last column takes what is left."""
    cols = []
    for line in text.splitlines():
        f = line.split("|")
        if len(f) < 5 or not f[0].isdigit() or not f[2].isdigit():
            continue
        left, pid, width, toolkit, state = int(f[0]), f[1], int(f[2]), f[3], f[4]
        if pid != excl and (pid == agent or (toolkit == "1" and state != "follower")):
            cols.append((left, pid, width))
    cols.sort(key=lambda c: c[0])
    if len(cols) < 2:
        return []
    w = sum(c[2] for c in cols) // len(cols)
    return [(c[1], w) for c in cols[:-1]]


def equalize(win, agent, excl=""):
    """Best effort: a failure warns on stderr and never changes the exit code."""
    rc, text = tmux("list-panes", "-t", win, "-F", "#{pane_left}|#{pane_id}|#{pane_width}|#{@tmux_pane}|#{@tmux_state}")
    if rc != 0:
        warn("could not list panes of %s to equalize widths" % win)
        return
    for pane, w in equal_columns(text, agent, excl):
        if tmux("resize-pane", "-x", str(w), "-t", pane)[0] != 0:
            warn("could not resize %s to %d columns" % (pane, w))


def border_setup(win):
    """Turn the window's border line on (top) when it is off, remembering the prior setting; blank the border of every non-toolkit pane."""
    if not show_option(win, "@tmux_border_prev", "w"):
        if tmux_out("display-message", "-p", "-t", win, "#{pane-border-status}") != "off":
            return
        cur = show_option(win, "pane-border-status", "w")
        rc = tmux("set-option", "-w", "-t", win, "@tmux_border_prev", cur or "unset", ";",
                  "set-option", "-w", "-t", win, "pane-border-status", "top")[0]
        if rc != 0:
            warn("could not turn on the border line of %s" % win)
            return
    for line in tmux_out("list-panes", "-t", win, "-F", "#{pane_id}|#{@tmux_pane}").splitlines():
        pane, _, toolkit = line.partition("|")
        if not pane or toolkit == "1" or option_is_set(pane, "pane-border-format"):
            continue
        tmux("set-option", "-p", "-t", pane, "pane-border-format", "", ";", "set-option", "-p", "-t", pane, "@tmux_border_blank", "1")


def border_frame(pane):
    """Frame a toolkit pane's border title; a pane that already has its own pane-border-format keeps it."""
    if option_is_set(pane, "pane-border-format"):
        return
    tmux("set-option", "-p", "-t", pane, "pane-border-format", BORDER_FORMAT)


def border_restore(win, excl=""):
    """When no toolkit pane is left in the window (excl is one about to close), put back the border setting border_setup replaced."""
    prev = show_option(win, "@tmux_border_prev", "w")
    if not prev:
        return
    left = 0
    for line in tmux_out("list-panes", "-t", win, "-F", "#{pane_id}|#{@tmux_pane}").splitlines():
        pid, _, toolkit = line.partition("|")
        if toolkit == "1" and pid != excl:
            left += 1
    if left:
        return
    for line in tmux_out("list-panes", "-t", win, "-F", "#{pane_id}|#{@tmux_border_blank}").splitlines():
        pid, _, blank = line.partition("|")
        if blank == "1":
            tmux("set-option", "-pu", "-t", pid, "pane-border-format", ";", "set-option", "-pu", "-t", pid, "@tmux_border_blank")
    if prev == "unset":
        rc = tmux("set-option", "-wu", "-t", win, "pane-border-status")[0]
    else:
        rc = tmux("set-option", "-w", "-t", win, "pane-border-status", prev)[0]
    if rc != 0:
        warn("could not restore the border line of %s" % win)
    tmux("set-option", "-wu", "-t", win, "@tmux_border_prev")


def cmd_equalize(target, closing=False):
    if not target:
        raise UsageError()
    info = tmux_out("display-message", "-p", "-t", target, "#{window_id}|#{@tmux_agent}")
    win, _, agent = info.partition("|")
    if not win:
        print("equalize: %s is not a tmux pane" % target, file=sys.stderr)
        return 1
    if closing:
        equalize(win, agent, target)
        border_restore(win, target)
    else:
        equalize(win, agent)
    return 0


def cmd_close(target="--kept"):
    rows = pane_rows()
    if target != "--kept":
        target = pct(target)
        st = next((r["state"] for r in rows if r["id"] == target and r["state"] != "follower"), "")
        if not st:
            print("close: %s is not a tmux-pane.sh pane" % target, file=sys.stderr)
            return 1
        if st == "running":
            print("close: %s is still running; use: tmux-pane.sh cancel %s" % (target, target), file=sys.stderr)
            return 1
    closed = 0
    wins = []
    for r in rows:
        pane, st, par = r["id"], r["state"], r["parent"]
        if st == "follower":
            # A follower goes with its leader; one whose leader is gone or finished goes too.
            leader_state = next((x["state"] for x in rows if x["id"] == par), "")
            if target == "--kept":
                if leader_state == "running":
                    continue
            elif par != target:
                continue
        else:
            if st == "running":
                continue
            if target != "--kept" and pane != target:
                continue
            wins.append(tmux_out("display-message", "-p", "-t", pane, "#{window_id}|#{@tmux_agent}"))
        if tmux("kill-pane", "-t", pane)[0] == 0:
            closed += 1
            print("closed %s" % pane)
    for w in sorted({x for x in wins if x}):
        win, _, agent = w.partition("|")
        equalize(win, agent)
        border_restore(win)
    if closed == 0:
        print("nothing to close")
    return 0


def resolve_pane(t):
    """A pane id or title to a pane id through tmux and the @tmux_title option; None when nothing matches."""
    if t.startswith("%"):
        rc, out = tmux("display-message", "-p", "-t", t, "#{pane_id}")
        if rc == 0:
            return out.strip()
    for r in pane_rows():
        if r["id"] == t or r["title"] == t:
            return r["id"]
    return None


def parse_copy_args(argv):
    """(target, lines-or-None) from `PANE|TITLE [-n LINES]`; UsageError when malformed."""
    target = ""
    n = ""
    i = 0
    while i < len(argv):
        a = argv[i]
        if a == "-n":
            if i + 1 >= len(argv):
                raise UsageError()
            n = argv[i + 1]
            i += 1
        elif a.startswith("-n") and len(a) > 2:
            n = a[2:]
        else:
            if target:
                raise UsageError()
            target = a
        i += 1
    if not target:
        raise UsageError()
    if n and not n.isdigit():
        raise UsageError()
    return target, (int(n) if n else None)


def tail_lines(text, n):
    if n is None:
        return text
    return "\n".join(text.split("\n")[-n:]) if n else ""


def cmd_copy(argv):
    import shutil
    target, n = parse_copy_args(argv)
    pane = resolve_pane(target)
    if pane is None:
        print("copy: no pane matches '%s'" % target, file=sys.stderr)
        return 1
    rc, text = tmux("capture-pane", "-p", "-J", "-t", pane, "-S", "-")
    if rc != 0:
        print("copy: could not capture %s" % pane, file=sys.stderr)
        return 1
    text = tail_lines(text.rstrip("\n"), n).rstrip("\n")
    count = text.count("\n") + 1
    if shutil.which("pbcopy"):
        subprocess.run(["pbcopy"], input=text + "\n", universal_newlines=True)
        print("copy: %d lines copied from %s" % (count, pane), file=sys.stderr)
    else:
        print(text)
        print("copy: no pbcopy; %d lines from %s printed above" % (count, pane), file=sys.stderr)
    return 0


def cmd_zoom(argv):
    if len(argv) != 1:
        raise UsageError()
    pane = resolve_pane(argv[0])
    if pane is None:
        print("zoom: no pane matches '%s'" % argv[0], file=sys.stderr)
        return 1
    try:
        return subprocess.run(["tmux", "resize-pane", "-Z", "-t", pane]).returncode      # tmux's own error text stays visible
    except OSError:
        return 127
