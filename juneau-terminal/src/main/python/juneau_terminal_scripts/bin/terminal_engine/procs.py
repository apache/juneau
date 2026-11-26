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
"""Process helpers.  A PID is only ever acted on together with its recorded start time, so a reused PID is never hit."""

import os
import signal
import subprocess
import time


def _ps(*args):
    try:
        return subprocess.run(["ps"] + list(args), stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, universal_newlines=True).stdout
    except OSError:
        return ""


def lstart(pid):
    """The process's start time as ps prints it (the identity check); "" when it does not exist."""
    return _ps("-o", "lstart=", "-p", str(pid)).strip()


def alive(pid):
    """True when the process exists and is not a zombie."""
    stat = _ps("-o", "stat=", "-p", str(pid)).replace(" ", "").strip()
    return bool(stat) and not stat.startswith("Z")


def descend(root):
    """root and every descendant, depth first, from one `ps` snapshot."""
    children = {}
    for line in _ps("-A", "-o", "pid=,ppid=").splitlines():
        f = line.split()
        if len(f) == 2 and f[0].isdigit() and f[1].isdigit():
            children.setdefault(int(f[1]), []).append(int(f[0]))
    out = []

    def walk(p):
        out.append(p)
        for c in children.get(p, []):
            walk(c)

    walk(int(root))
    return out


def track(root, pids_file):
    """Append every not-yet-recorded descendant of root (and root) to pids_file: pid, start time, command line (tab separated)."""
    try:
        with open(pids_file) as f:
            seen = {tuple(line.rstrip("\n").split("\t")[:2]) for line in f}
    except OSError:
        seen = set()
    for p in descend(root):
        ls = lstart(p)
        if not ls or (str(p), ls) in seen:
            continue
        cmd = _ps("-o", "command=", "-p", str(p)).split("\n")[0][:200]
        with open(pids_file, "a") as f:
            f.write("%d\t%s\t%s\n" % (p, ls, cmd))
        seen.add((str(p), ls))


def _same(pid, ls):
    return alive(pid) and lstart(pid) == ls


def _ports(pid):
    """Listening TCP ports of pid, comma separated ("" when lsof is missing or finds none)."""
    try:
        out = subprocess.run(["lsof", "-nP", "-a", "-p", str(pid), "-iTCP", "-sTCP:LISTEN", "-Fn"], stdout=subprocess.PIPE,
                             stderr=subprocess.DEVNULL, universal_newlines=True).stdout
    except OSError:
        return ""
    return ",".join(l[1:] for l in out.splitlines() if l.startswith("n"))


def report(path, reap=False, grace=10):
    """Print the recorded processes that are still alive with the same start time; with reap, TERM then KILL exactly those PIDs."""
    try:
        with open(path) as f:
            rows = [line.rstrip("\n").split("\t", 2) for line in f if line.strip()]
    except OSError:
        return
    live = []
    for row in rows:
        row += [""] * (3 - len(row))
        pid, ls, cmd = row
        if not pid.isdigit() or not _same(int(pid), ls):
            continue
        ports = _ports(pid)
        if not live:
            print("orphans (started by the command, still running):")
        print("  pid %s  %s%s" % (pid, cmd, "  [listening: %s]" % ports if ports else ""))
        live.append((int(pid), ls))
    if not reap or not live:
        return
    left = len(live)
    for sig in (signal.SIGTERM, signal.SIGKILL):
        for pid, ls in live:
            if _same(pid, ls):
                try:
                    os.kill(pid, sig)
                except OSError:
                    pass
        if sig == signal.SIGKILL:
            time.sleep(0.3)
        for _ in range(int(grace) * 4):
            left = sum(1 for pid, ls in live if _same(pid, ls))
            if left == 0 or sig == signal.SIGKILL:
                break
            time.sleep(0.25)
        if left == 0:
            break
    if left == 0:
        print("  reaped %d process(es)" % len(live))
    else:
        print("  %d process(es) survived KILL" % left)
