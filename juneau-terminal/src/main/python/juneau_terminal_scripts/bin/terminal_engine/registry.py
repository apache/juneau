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
"""The run registry under ${TMPDIR}/tmux-pane/: one .reg file per leader pane (shell-quoted key=value lines, so a file
written by the old bash scripts and by this code read the same), plus .pids and .cancel files beside it."""

import os
import shlex

from .tmux import tmux_out


def default_dir():
    return os.path.join(os.environ.get("TMPDIR") or "/tmp", "tmux-pane")


def read_reg(path):
    """The key=value lines of a .reg file as a dict of strings (values unquoted as a shell would)."""
    fields = {}
    with open(path) as f:
        for line in f:
            key, sep, value = line.rstrip("\n").partition("=")
            if not sep:
                continue
            try:
                parts = shlex.split(value)
            except ValueError:
                parts = [value]
            fields[key] = parts[0] if parts else ""
    return fields


def write_reg(path, fields):
    """Write fields atomically (temp file, then rename)."""
    path = str(path)
    tmp = path + ".tmp"
    with open(tmp, "w") as f:
        for k, v in fields.items():
            f.write("%s=%s\n" % (k, shlex.quote(str(v))))
    os.replace(tmp, path)


def regdirs(own=None):
    """This process's registry directory, plus any other one named by a tagged pane (a shell with another TMPDIR can still find it)."""
    own = own or default_dir()
    dirs = [own]
    for d in sorted(set(tmux_out("list-panes", "-a", "-F", "#{@tmux_reg}").splitlines())):
        if d and d != own:
            dirs.append(d)
    return dirs


def reg_files(own=None, extra=None):
    """Every .reg path in the registry directories (own first, then the tagged ones; `extra` replaces the tmux lookup in tests)."""
    dirs = [own or default_dir()] + (regdirs(own)[1:] if extra is None else list(extra))
    files = []
    for d in dirs:
        try:
            names = sorted(n for n in os.listdir(d) if n.endswith(".reg"))
        except OSError:
            continue
        files.extend(os.path.join(d, n) for n in names)
    return files
