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
"""The `juneau-terminal` command: one console script over the engine, the tmux pane helpers and the file lookups.

    juneau-terminal run [juneau_terminal.py arguments]    one tool as one terminal-protocol step
    juneau-terminal watch [watch-run.sh arguments]        run a command in a watchable tmux pane
    juneau-terminal pane [tmux-pane.sh arguments]         the pane helper itself
    juneau-terminal path bin NAME [--toolkit-version V] [--pom P] [--cache-dir D]
    juneau-terminal path reporter                         the Playwright reporter file
    juneau-terminal path plugin                           the directory holding the pytest plugin
    juneau-terminal --version

With no subcommand, `--bin NAME ...` goes to bootstrap's own command line unchanged, so `python3 <file>.pyz --bin
watch-run.sh` keeps working (there `--version` is the toolkit version to look up).  POSIX only.  Exit 0 done, 1 a
lookup failed, 2 a usage error, an unsupported platform, no tmux, or a missing tool."""

import os
import shutil
import sys
from pathlib import Path

try:
    import juneau_terminal_bootstrap as bootstrap       # the wheel and the .pyz
except ImportError:                                     # the source tree
    import bootstrap

SUBCOMMANDS = ("run", "watch", "pane", "path")
SCRIPTS = {"watch": "watch-run.sh", "pane": "tmux-pane.sh"}
PANE_TOOLS = ("bash", "tmux", "ps", "pgrep", "perl")
USAGE = "usage: juneau-terminal {run,watch,pane,path} ... | --version | --bin NAME [...]"


def build_version():
    """The package version written at build time, or None in a source tree."""
    try:
        from juneau_terminal_scripts import _build_version
    except ImportError:
        return None
    return _build_version.__version__


def version_line():
    from juneau_terminal_scripts.protocol import PROTOCOL_VERSION
    return f"juneau-terminal {build_version() or 'unknown (source tree)'} (terminal-protocol v{PROTOCOL_VERSION})"


def reporter_cache():
    """Where `path reporter` keeps a reporter extracted from an archive: the path it prints must outlive this process."""
    return Path.home() / ".cache" / "juneau-terminal" / "reporter"


def _err(msg):
    print(f"juneau-terminal: {msg}", file=sys.stderr)


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    if os.name == "nt":
        _err("POSIX-only (needs fcntl)")
        return 2
    first = argv[0] if argv else None
    if first not in SUBCOMMANDS and "--bin" in argv:
        return bootstrap.main(argv)
    if first == "--version":
        print(version_line())
        return 0
    if first in ("-h", "--help"):
        print(__doc__)
        return 0
    if first == "run":
        import juneau_terminal
        return juneau_terminal.main(argv[1:])
    if first in SCRIPTS:
        return _exec_script(SCRIPTS[first], argv[1:])
    if first == "path":
        return _path(argv[1:])
    if first is not None:
        _err(f"unknown command {first!r}")
    print(USAGE, file=sys.stderr)
    return 2


def _exec_script(name, args):
    """Preflight in a fixed order (no tmux, then each tool), then replace this process with `bash <script> args`."""
    if not os.environ.get("TMUX"):
        print(f"{name}: not inside tmux", file=sys.stderr)        # the script's own message and exit code
        _err("use 'juneau-terminal run' to run a tool without a pane")
        return 2
    missing = next((t for t in PANE_TOOLS if shutil.which(t) is None), None)
    if missing:
        _err(f"{name} needs {missing}, which is not on the PATH")
        return 2
    try:
        script = bootstrap.cli_bin(name, version=build_version())
    except bootstrap.BootstrapError as e:
        _err(str(e))
        return 1
    os.execvp("bash", ["bash", str(script), *args])
    return 1        # not reached; execvp only returns by raising


def _path(args):
    import argparse
    ap = argparse.ArgumentParser(prog="juneau-terminal path", description="Print the path of a packaged file.")
    sub = ap.add_subparsers(dest="what", required=True)
    b = sub.add_parser("bin", help="a juneau_terminal_scripts/bin/ script, e.g. watch-run.sh")
    b.add_argument("name")
    b.add_argument("--toolkit-version", help="toolkit version to look up (default: this package's)")
    b.add_argument("--pom", help="pom with <juneau.version>, for a source-tree lookup")
    b.add_argument("--cache-dir", help="where mvn dependency:copy cached the zip")
    sub.add_parser("reporter", help="the Playwright reporter file")
    sub.add_parser("plugin", help="the directory holding juneau_terminal_pytest.py")
    ns = ap.parse_args(args)
    if ns.what == "bin":
        try:
            print(bootstrap.cli_bin(ns.name, pom=ns.pom, cache_dir=ns.cache_dir,
                                    version=ns.toolkit_version or build_version()))
        except bootstrap.BootstrapError as e:
            _err(str(e))
            return 1
        return 0
    import juneau_terminal_scripts  # noqa: F401 - the engine finds an archive through this module's __file__
    import juneau_terminal
    if ns.what == "reporter":
        found = juneau_terminal.find_playwright_reporter(extract_dir=reporter_cache())
    else:
        found = juneau_terminal.find_pytest_plugin()
    if not found:
        _err(f"no {ns.what} found beside {Path(juneau_terminal.__file__).parent}")
        return 1
    print(found)
    return 0


if __name__ == "__main__":
    sys.exit(main())
