# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
# * with the License.  You may obtain a copy of the License at                                                              *
# *                                                                                                                         *
# *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
# *                                                                                                                         *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
# * specific language governing permissions and limitations under the License.                                              *
# ***************************************************************************************************************************
"""
Parse src/data/projects.ts into plain Python data.

The file is TypeScript, but its GROUPS and PROJECTS literals follow strict format rules so a line-based parser is
safe: one object per entry, one `key: <literal>,` per line, single-quoted strings with no backslash or inner quote,
and single-line arrays of single-quoted strings.  Anything else raises ProjectIndexError with the line number,
rather than guessing.  Imported by check-project-pages.py, check-doc-inventory.py and generate-ai-artifacts.py.
"""

import re
from pathlib import Path

PROJECTS_FILE = 'src/data/projects.ts'

# Pure aggregator/parent POMs; never documented on their own.  check-doc-inventory.py and
# check-project-pages.py both import this set so they cannot disagree.
AGGREGATOR_POMS = frozenset({
    'juneau', 'juneau-core', 'juneau-rest', 'juneau-bean', 'juneau-microservice-parent', 'juneau-sc',
    'juneau-shaded', 'juneau-bundles', 'juneau-examples', 'juneau-petstore', 'juneau-bom', 'juneau-distrib',
    'juneau-integration-tests',
})

_STR = r"'([^'\\\n]*)'"
_FIELD_RE = re.compile(r'^\s*(\w+):\s*(.+?),\s*$')
_STR_RE = re.compile(r'^' + _STR + r'$')
_ARR_RE = re.compile(r"^\[\s*(?:" + _STR + r"(?:\s*,\s*" + _STR + r")*)?\s*\]$")

# Fields that, when present, must be a single-quoted string (not an array).
OPTIONAL_STRING_FIELDS = ('gettingStarted',)


class ProjectIndexError(ValueError):
    """The file breaks a format rule; the message names the line."""


def _literal(value, lineno):
    m = _STR_RE.match(value)
    if m:
        return m.group(1)
    if value.startswith('['):
        if not _ARR_RE.match(value):
            raise ProjectIndexError(f'line {lineno}: array must be one line of single-quoted strings: {value}')
        return re.findall(_STR, value)
    raise ProjectIndexError(f'line {lineno}: not a single-quoted string or one-line array: {value}')


def _block(lines, name, closer='];'):
    """The (lineno, text) pairs strictly between `export const NAME` and its closing line (`];` unless told)."""
    start = next((i for i, ln in enumerate(lines) if re.match(rf'^export const {name}\b', ln)), None)
    if start is None:
        raise ProjectIndexError(f'export const {name} not found')
    out = []
    for i in range(start + 1, len(lines)):
        if lines[i].rstrip() == closer:
            return out
        out.append((i + 1, lines[i]))
    raise ProjectIndexError(f'line {start + 1}: {name} is not closed by a line `{closer}`')


def _entries(block):
    entries, current, opened = [], None, 0
    for lineno, text in block:
        s = text.strip()
        if not s:
            continue
        if s == '{':
            if current is not None:
                raise ProjectIndexError(f'line {lineno}: nested object')
            current, opened = {}, lineno
        elif s in ('}', '},'):
            if current is None:
                raise ProjectIndexError(f'line {lineno}: unexpected `}}`')
            entries.append(current)
            current = None
        else:
            m = _FIELD_RE.match(text)
            if current is None or not m:
                raise ProjectIndexError(f'line {lineno}: expected `key: literal,` inside an object: {s}')
            value = _literal(m.group(2), lineno)
            if m.group(1) in OPTIONAL_STRING_FIELDS and not isinstance(value, str):
                raise ProjectIndexError(f'line {lineno}: {m.group(1)} must be a single-quoted string: {m.group(2)}')
            current[m.group(1)] = value
    if current is not None:
        raise ProjectIndexError(f'line {opened}: object is not closed by `}},`')
    return entries


def parse(text):
    """(groups, projects): lists of dicts, one per entry, keyed by the field names in the file."""
    lines = text.splitlines()
    return _entries(_block(lines, 'GROUPS')), _entries(_block(lines, 'PROJECTS'))


_PAIR_RE = re.compile(r'^\s*' + _STR + r':\s*' + _STR + r',\s*$')


def parse_module_docs(text):
    """The MODULE_DOCS map (module name -> topic slug): one `'module': 'Slug',` per line, no duplicate keys."""
    out = {}
    for lineno, line in _block(text.splitlines(), 'MODULE_DOCS', '};'):
        if not line.strip():
            continue
        m = _PAIR_RE.match(line)
        if not m:
            raise ProjectIndexError(f"line {lineno}: expected `'module': 'Slug',` in MODULE_DOCS: {line.strip()}")
        if m.group(1) in out:
            raise ProjectIndexError(f'line {lineno}: duplicate MODULE_DOCS key "{m.group(1)}"')
        out[m.group(1)] = m.group(2)
    return out


def load_module_docs(docs_root):
    return parse_module_docs(Path(docs_root, PROJECTS_FILE).read_text(encoding='utf-8'))


def load(docs_root):
    return parse(Path(docs_root, PROJECTS_FILE).read_text(encoding='utf-8'))
