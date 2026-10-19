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
"""Parsers that live outside juneau_run.py, loaded by name when a step names one that is not built in.

juneau_run binds itself first (bind), so a parser module can subclass its Parser through _side; importing one before
that raises ParserNotBound.  REGISTRY maps a parser name to its module in this package, which defines PARSER."""

import importlib
import os
import re

from . import _side
from ._side import ParserNotBound, SideFile

__all__ = ["REGISTRY", "ParserNotBound", "SideFile", "bind", "detect", "known", "load"]

REGISTRY = {"bazel": "bazel"}


def bind(jr):
    _side.bind(jr)


def known(name):
    return name in REGISTRY


def load(name):
    """The parser class registered as name (KeyError if none).  bind() first."""
    return importlib.import_module("." + REGISTRY[name], __name__).PARSER


_TOOLS = {"mvn": "maven", "mvnw": "maven", "mvnw.cmd": "maven", "gradle": "gradle", "gradlew": "gradle",
          "gradlew.bat": "gradle", "npm": "npm"}
_BAZEL_TOOLS = ("bazel", "bazelisk")
_BAZEL_COMMANDS = ("build", "test", "coverage")
_PYTHON = re.compile(r"^(?:python|pypy)[\d.]*(?:\.exe)?$")
_QUIET = ("-q", "--quiet")
_LAUNCHERS = ("uv", "poetry", "pipenv", "pdm", "hatch", "rye")   # as in juneau_run._RUNNERS


def _program(arg):
    return arg.replace("\\", "/").rsplit("/", 1)[-1]


def _looks_like_pytest(args):
    """pytest or py.test as the program, or a python program running `-m pytest` (used only when juneau_run's own
    check is not given)."""
    if not args:
        return False
    if _program(args[0]) in ("pytest", "py.test"):
        return True
    return bool(_PYTHON.match(_program(args[0]))) and "-m" in args and \
        args[args.index("-m") + 1:args.index("-m") + 2] == ["pytest"]


def _foreign_dash_m(args):
    """True when args has a `-m` but its program is neither pytest, python, env nor a launcher such as uv:
    `git commit -m pytest` is not a pytest run."""
    prog = _program(args[0])
    return "-m" in args and prog not in ("pytest", "py.test", "env", *_LAUNCHERS) and not _PYTHON.match(prog)


def program_index(args):
    """Index of the real program in args, past the launchers in front of it: env (and its options and NAME=value
    pairs), npx/bunx (and their flags), pnpm/yarn (and exec/dlx/run), npm exec (and an optional --).  len(args) when
    nothing follows the launchers.  juneau_run carries the same walk as _program_index, for a parser that runs without
    this package."""
    i = 0
    while i < len(args):
        prog = _program(args[i])
        if prog == "env":
            i += 1
            while i < len(args):
                a = args[i]
                if a in ("-i", "--ignore-environment", "-"):
                    i += 1
                elif a in ("-u", "--unset"):
                    i += 2
                elif a.startswith("--unset=") or ("=" in a and not a.startswith("-")):
                    i += 1
                else:
                    break
        elif prog in ("npx", "bunx"):
            i += 1
            while i < len(args) and args[i].startswith("-"):
                i += 1
        elif prog == "pnpm":
            i += 2 if args[i + 1:i + 2] in (["exec"], ["dlx"]) else 1
        elif prog == "yarn":
            i += 2 if args[i + 1:i + 2] in (["run"], ["exec"], ["dlx"]) else 1
        elif prog == "npm" and args[i + 1:i + 2] == ["exec"]:
            i += 3 if args[i + 2:i + 3] == ["--"] else 2
        else:
            return i
    return len(args)


def _unwrap(args):
    """args with the launchers in front of the real program removed."""
    return args[program_index(args):]


def detect(argv, *, is_pytest=None):
    """The parser name for a command line (a list of str or path-like), or None for the generic parser.  is_pytest is
    juneau_run's _is_pytest_command (launchers such as `uv run`); launchers in front of the program are looked
    through.  A tool is named only for commands whose output its parser reads: `bazel query` and `playwright install`
    are not."""
    if isinstance(argv, (str, bytes)):
        return None
    args = [os.fsdecode(a) if isinstance(a, (os.PathLike, bytes)) else str(a) for a in argv]
    check = is_pytest or _looks_like_pytest
    if check(args) and not _foreign_dash_m(args):
        return "pytest"
    args = _unwrap(args)
    if not args:
        return None
    if check(args) and not _foreign_dash_m(args):
        return "pytest"
    prog = _program(args[0])
    if prog in _TOOLS:
        if _TOOLS[prog] == "maven" and any(a in _QUIET for a in args[1:]):
            return None   # quiet Maven prints nothing the Maven parser reads
        return _TOOLS[prog]
    if prog == "playwright":
        return "playwright" if args[1:2] == ["test"] else None
    if prog in _BAZEL_TOOLS:
        word = next((a for a in args[1:] if not a.startswith("-")), None)
        return "bazel" if word in _BAZEL_COMMANDS else None
    return None
