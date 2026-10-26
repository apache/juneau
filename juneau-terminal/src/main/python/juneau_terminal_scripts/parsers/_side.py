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
"""What parser modules build on: juneau_terminal's parser API, bound at run time, and SideFile.

juneau_terminal calls parsers.bind(itself) before it loads a parser, so a parser module subclasses _side.Parser without
importing juneau_terminal (which may be a file loaded by path, a zip member's sibling, or __main__).  Reading an export
before then raises ParserNotBound."""

import os
import sys
from pathlib import Path


class ParserNotBound(RuntimeError):
    """A parser module was used before juneau_terminal bound itself (parsers.bind)."""


_jr = None
_EXPORTS = ("Parser", "GenericParser", "strip_ansi", "warn")
_PACKAGE = __name__.rsplit(".", 1)[0]


def bind(jr):
    """Make jr's parser API available here.  Binding a different module drops the parser modules already imported, so
    the next load subclasses the new module's Parser."""
    global _jr
    if _jr is not None and _jr is not jr:
        for name in [n for n in sys.modules if n.startswith(_PACKAGE + ".") and n != __name__]:
            del sys.modules[name]
    _jr = jr


def bound():
    if _jr is None:
        raise ParserNotBound("juneau_terminal_scripts.parsers is not bound to juneau_terminal (call parsers.bind(jr) first)")
    return _jr


def __getattr__(name):
    if name in _EXPORTS:
        return getattr(bound(), name)
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


class SideFile:
    """Reads a file another process appends to, a complete line at a time.  A file that shrinks is read again from the
    start.  ignore_existing skips the file as it is now (a report left by an earlier run) until it changes."""

    def __init__(self, path, ignore_existing=False):
        self.path = Path(path)
        self.offset = 0
        self.buffer = b""
        self.stale = self._stat() if ignore_existing else None

    def _stat(self):
        try:
            st = os.stat(self.path)
        except OSError:
            return None
        return st.st_size, st.st_mtime_ns, st.st_ino

    def _read(self):
        if self.stale is not None:
            if self._stat() == self.stale:
                return
            self.stale = None
        try:
            with open(self.path, "rb") as f:
                if os.fstat(f.fileno()).st_size < self.offset:
                    self.offset, self.buffer = 0, b""
                f.seek(self.offset)
                data = f.read()
        except OSError:
            return
        self.offset += len(data)
        self.buffer += data

    def read_new(self):
        """The complete lines appended since the last call, without their newlines."""
        self._read()
        *lines, self.buffer = self.buffer.split(b"\n")
        return lines

    def drain(self):
        """read_new() plus a final line with no newline: for when the writer has exited."""
        lines = self.read_new()
        if self.buffer:
            lines.append(self.buffer)
            self.buffer = b""
        return lines
