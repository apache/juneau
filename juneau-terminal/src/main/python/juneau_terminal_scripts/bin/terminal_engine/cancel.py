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
"""`tmux-pane.sh cancel [PANE|TITLE|--all]`: stop a registered run from any shell."""

import os
import signal
import sys
import tempfile
import time

from . import registry
from .panes import pct
from .procs import lstart, report
from .tmux import tmux


def group_gone(pgid):
    """True when no process group answers a null signal.  Like the bash `! kill -0 -- -pgid`, a refused signal (a group of
    zombies answers EPERM on macOS) counts as gone."""
    try:
        os.killpg(int(pgid), 0)
    except OSError:
        return True
    return False


def matches(reg, target):
    return target == "--all" or reg["pane"] == target or reg["pane"] == pct(target) or reg.get("title") == target


def _copy(src, dst):
    with open(src, "rb") as f:
        data = f.read()
    with open(dst, "wb") as f:
        f.write(data)


def cancel_one(reg_file, target, grace):
    """0 = handled, 3 = reg_file is not the target, 4 = nothing to cancel."""
    reg = registry.read_reg(reg_file)
    if not matches(reg, target):
        return 3
    pane, title, rd = reg["pane"], reg.get("title", ""), reg["rd"]
    ident = pane[1:] if pane.startswith("%") else pane
    pidsfile = os.path.join(rd, ident + ".pids")
    print("cancel: %s (%s)" % (pane, title))
    pgid = reg.get("pgid")
    if not pgid:
        print("  not started yet; nothing to signal")
        return 4
    cur = lstart(reg.get("cpid", ""))
    if cur and cur != reg.get("cpidstart", ""):
        print("  pid %s was reused; not touching it" % reg.get("cpid", ""))
        return 4
    # The caller can't test the .cancel flag once this finishes (it is removed), so it learns of the cancel from this option.
    tmux("set-option", "-p", "-t", pane, "@tmux_cancelled", "1")
    flag = os.path.join(rd, ident + ".cancel")
    open(flag, "a").close()
    fd, copy = tempfile.mkstemp(prefix="tmux-pane-cancel.", dir=os.environ.get("TMPDIR") or "/tmp")
    os.close(fd)
    try:
        _copy(pidsfile, copy)
    except OSError:
        pass
    pg = int(pgid)
    if group_gone(pg):
        print("  process group %d already gone" % pg)
    else:
        for sig, name in ((signal.SIGINT, "INT"), (signal.SIGTERM, "TERM"), (signal.SIGKILL, "KILL")):
            try:
                os.killpg(pg, sig)
            except OSError:
                pass
            print("  sent %s to process group %d" % (name, pg))
            if name == "KILL":
                time.sleep(0.3)
                break
            for _ in range(grace * 4):
                if group_gone(pg):
                    break
                time.sleep(0.25)
            if group_gone(pg):
                break
        print("  process group stopped" if group_gone(pg) else "  process group %d survived KILL" % pg)
    if os.path.isfile(pidsfile):
        _copy(pidsfile, copy)
    sys.stdout.flush()
    report(copy, reap=True, grace=grace)
    for f in (copy, flag):
        try:
            os.unlink(f)
        except OSError:
            pass
    return 0


def cmd_cancel(target=""):
    grace = int(os.environ.get("TMUX_PANE_GRACE", "10") or 10)
    files = registry.reg_files()
    if not files:
        print("cancel: no tmux-pane.sh runs are registered")
        return 1
    if not target:
        if len(files) != 1:
            print("cancel: %d runs are registered; name one (pane id or title) or use --all:" % len(files), file=sys.stderr)
            for f in files:
                r = registry.read_reg(f)
                print("  %s  %s" % (r["pane"], r.get("title", "")), file=sys.stderr)
            return 2
        target = "--all"
    matched = 0
    for f in files:
        if cancel_one(f, target, grace) != 3:
            matched += 1
    if matched == 0:
        print("cancel: no registered run matches '%s'" % target, file=sys.stderr)
        return 1
    return 0
