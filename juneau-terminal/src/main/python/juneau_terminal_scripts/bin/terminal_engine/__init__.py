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
"""The juneau-terminal tmux engine (tmux-pane, watch-run, tmux-toolbar) in Python, standard library only, Python 3.9+.

The `.sh` files beside engine.py are shims for it.  Import it directly with
`bootstrap.load_engine()` (or put the shim directory on sys.path and `import terminal_engine`), then `terminal_engine.main(["watch-run", ...])`."""

import os
import sys

BIN_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENGINE_PY = os.path.join(BIN_DIR, "engine.py")


def script_path(name):
    """The shim called NAME (for example tmux-pane.sh) in the same directory as engine.py."""
    return os.path.join(BIN_DIR, name)


class UsageError(Exception):
    """A bad command line.  main() prints the script's usage text and returns 2."""


def main(argv):
    """Run one engine command: argv[0] is tmux-pane, watch-run, tmux-toolbar (the three scripts) or leader / follower
    (internal pane processes).  Returns the process exit code."""
    try:
        sys.stdout.reconfigure(line_buffering=True)     # callers read `pane %N:` lines as they appear
    except (AttributeError, ValueError):
        pass
    if not argv:
        print("usage: engine.py tmux-pane|watch-run|tmux-toolbar ARGS...", file=sys.stderr)
        return 2
    name, rest = argv[0], argv[1:]
    try:
        if name == "tmux-pane":
            from . import pane
            return pane.main(rest)
        if name == "watch-run":
            from . import watch
            return watch.main(rest)
        if name == "tmux-toolbar":
            from . import toolbar
            return toolbar.main(rest)
        if name == "leader":
            from . import leader
            return leader.leader(rest[0])
        if name == "follower":
            from . import leader
            return leader.follower(rest[0], int(rest[1]))
    except UsageError:
        from .usage_text import TEXT
        sys.stderr.write(TEXT.get(name, ""))
        return 2
    print("engine.py: unknown command %s" % name, file=sys.stderr)
    return 2
