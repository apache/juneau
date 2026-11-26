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
"""The two processes tmux-pane starts inside panes (formerly generated bash text): `leader STATE_DIR` runs the command and
waits on the kept prompt, `follower STATE_DIR N` runs a follower's command.  They read the JSON the runner left in STATE_DIR:
spec.json (the leader), follower-N.json (a follower), env.json (the caller's environment)."""

import os
import shlex
import signal
import subprocess
import sys
from pathlib import Path

from . import kept, panes
from .tmux import show_option, tmux, tmux_out

# `set -m; sh -c ...` of the old leader script: the shell records its PID (which becomes the command's, and the process group's)
# before exec'ing the command, and reports 127/126 with its own message when the command cannot run.
_WRAPPER = 'echo $$ > "$1"; shift; exec "$@"'


def exit_code(returncode):
    """A shell's view of a child's status: 128+N for a child killed by signal N."""
    return returncode if returncode >= 0 else 128 - returncode


def _own_foreground_group():
    """In the child, before exec: a process group of its own that becomes the terminal's foreground group (bash `set -m`), so
    Ctrl-C reaches it and a read from /dev/tty works.  Not a new session: that would drop the controlling terminal."""
    os.setpgrp()
    signal.signal(signal.SIGTTOU, signal.SIG_IGN)
    try:
        os.tcsetpgrp(0, os.getpgrp())
    except OSError:
        pass
    finally:
        signal.signal(signal.SIGTTOU, signal.SIG_DFL)      # an ignored signal would stay ignored across exec


def _take_terminal():
    """The leader becomes the foreground group again after the command ended, or reading the kept prompt would stop it (SIGTTIN)."""
    old = signal.signal(signal.SIGTTOU, signal.SIG_IGN)
    try:
        os.tcsetpgrp(0, os.getpgrp())
    except OSError:
        pass
    finally:
        signal.signal(signal.SIGTTOU, old)


def run_command(cmd, state):
    """Run cmd as the pane's foreground job; the wrapper shell records its PID (the process group id) in STATE/cpid; returns the
    shell-style exit code."""
    child = subprocess.Popen(["sh", "-c", _WRAPPER, "sh", str(Path(state) / "cpid")] + list(cmd), preexec_fn=_own_foreground_group)
    rc = child.wait()
    _take_terminal()
    return exit_code(rc)


def _load_json(path):
    import json
    with open(path) as f:
        return json.load(f)


def _enter(state, spec, mypane):
    """The common start of both processes: cd, then the caller's environment (which overwrites TMUX_PANE, so `mypane` was read first)."""
    try:
        os.chdir(spec["dir"])
    except OSError as e:
        print("tmux-pane.sh: cd: %s: %s" % (spec["dir"], e.strerror), file=sys.stderr)
        return False
    os.environ.update(_load_json(Path(state) / "env.json"))
    os.environ["TMUX_PANE"] = mypane
    return True


def leader(state_dir):
    state = Path(state_dir)
    spec = _load_json(state / "spec.json")
    mypane = os.environ.get("TMUX_PANE", "")
    if not _enter(state, spec, mypane):
        return 1
    os.environ["TMUX_PANE_RUN_ID"] = mypane
    os.environ["TMUX_PANE_RUN_CANCEL"] = "%s cancel %s" % (shlex.quote(spec["self"]), mypane)
    sys.stdout.write("\033]2;%s\033\\" % spec["title"])
    sys.stdout.flush()
    rc = run_command(spec["cmd"], state)
    (state / "exit").write_text("%d\n" % rc)
    tmux("set-option", "-p", "-t", mypane, "@tmux_state", "exit %d" % rc)
    # A cancel sets @tmux_cancelled on this pane first, so the message can say why the pane is there.
    cancelled = bool(show_option(mypane, "@tmux_cancelled"))
    tmux("wait-for", "-S", spec["chan"])
    if spec["readonly"]:
        kept.readonly_wait(rc, cancelled, spec["keepmin"])
    else:
        kept.kept_wait(kept.prompt_prefix(rc, cancelled), spec["keepmin"])
    for line in tmux_out("list-panes", "-a", "-F", "#{pane_id} #{@tmux_parent}").splitlines():
        pane, _, parent = line.partition(" ")
        if parent == mypane:
            tmux("kill-pane", "-t", pane)
    panes.cmd_equalize(mypane, closing=True)
    return 0


def follower(state_dir, index):
    state = Path(state_dir)
    spec = _load_json(state / ("follower-%d.json" % index))
    mypane = os.environ.get("TMUX_PANE", "")
    if not _enter(state, spec, mypane):
        return 1
    os.environ["TMUX_PANE_RUN_ID"] = spec["leader"]
    os.environ["TMUX_PANE_RUN_CANCEL"] = "%s cancel %s" % (shlex.quote(spec["self"]), spec["leader"])
    # The follower closes itself once its leader pane is gone (the run notices within about 2 s, and so does this).  The
    # watcher stays in this pane's process group so closing the pane takes it down too.
    watcher = ("while tmux display-message -p -t %s '#{pane_id}' >/dev/null 2>&1; do sleep 1; done; tmux kill-pane -t %s"
               % (shlex.quote(spec["leader"]), shlex.quote(mypane)))
    subprocess.Popen(["sh", "-c", watcher], stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    os.execvp("bash", ["bash", "-c", spec["cmd"]])
    return 1
