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
"""Thin wrappers over the tmux and process calls the scripts made; stderr of tmux is discarded as the bash scripts did."""

import subprocess
import sys


def tmux(*args):
    """Run tmux ARGS.  Returns (returncode, stdout); (127, "") when tmux cannot be started."""
    try:
        r = subprocess.run(["tmux"] + list(args), stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, universal_newlines=True)
    except OSError:
        return 127, ""
    return r.returncode, r.stdout


def tmux_out(*args):
    """Stdout of a tmux call with trailing newlines removed (bash `$(...)`); "" when the call failed."""
    rc, text = tmux(*args)
    return text.rstrip("\n") if rc == 0 else ""


def show_option(target, name, scope="p"):
    """`tmux show-options -<scope>qv -t target name` (the value only; scope p = pane, w = window)."""
    return tmux_out("show-options", "-%sqv" % scope, "-t", target, name)


def option_is_set(target, name):
    """True when the pane option is set at all, even to the empty string (`show-options -pq` prints `name ''` then)."""
    return tmux_out("show-options", "-pq", "-t", target, name) != ""


def pane_exists(pane):
    return tmux("display-message", "-p", "-t", pane, "#{pane_id}")[0] == 0


def warn(message):
    print("tmux-pane.sh: warning: " + message, file=sys.stderr)
