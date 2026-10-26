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
The pre-rename names that no project page may mention (check C9 of check-project-pages.py).

The list is J1047 spec section 6 ("Old-names list for J1048's checker C9"), as whole-token patterns.  Every
pattern is assembled from fragments, so this file spells no old name as a contiguous literal and the zero-old-names
grep gate needs no exclude entry for it; test_check_project_pages.py enforces that.

Old heading anchors are deliberately not here: they come from the rename tool's anchor report, not from a fixed list.
"""

import re

_W = r'[A-Za-z0-9_]'


def _tok(*parts, tail=''):
    """A hyphenated module or tool name as a whole token: a word character or `-` on either side disqualifies it, so
    the bare test module name does not match its `-utils` sibling.  `tail` is an optional regex suffix."""
    return re.compile(r'(?<![\w-])' + '-'.join(parts) + tail + r'(?![\w-])')


def _word(text):
    """A name made of word characters (CamelCase, snake_case, UPPER_CASE) as a whole token."""
    return re.compile(r'(?<!' + _W + r')' + text + r'(?!' + _W + r')')


def _prefix(text):
    """A name that may continue (an env-var prefix, a class-name stem), anchored on the left only."""
    return re.compile(r'(?<!' + _W + r')' + text)


def _pkg(*parts, tail=''):
    """A dotted package name; `tail` is an optional regex suffix.  Anything but a word character may follow."""
    return re.compile(r'(?<![\w.])' + r'\.'.join(parts) + tail + r'(?!' + _W + r')')


def _slug(name):
    return re.compile(re.escape('/docs/topics/') + name + r'(?!' + _W + r')')


_JUNEAU = 'juneau'
_RS = (_JUNEAU, 'rest', 'server')
_ORG = ('org', 'apache', 'juneau')

OLD_NAME_PATTERNS = (
    # Modules
    ('module', _tok(_JUNEAU, 'run')),
    ('module', _tok(_JUNEAU, 'run', 'report')),
    ('module', _tok(_JUNEAU, 'test')),
    ('module', _tok(*_RS, 'console', 'ui', tail='(?:-freemarker(?:-tests)?)?')),
    ('module', _tok(*_RS, tail='-(?:views(?:-markdown)?|widgets|datatables|bus(?:-websocket)?|terminal(?:-bus)?)')),
    # Packages
    ('package', re.compile(r'(?<![\w.])' + r'\.'.join(_ORG) + r'\.test\.')),
    ('package', _pkg(*_ORG, 'rest', 'server', tail=r'\.(?:views|console|widgets|datatables|bus|terminal|runreport)')),
    ('package', _pkg(*_ORG, 'rest', 'server', 'view', 'freemarker', 'console')),
    # Tool
    ('tool', _word('_'.join((_JUNEAU, 'run')))),
    ('tool', _prefix('_'.join(('JUNEAU', 'RUN')) + '_')),
    ('tool', re.compile(re.escape('.cache/') + '-'.join((_JUNEAU, 'run')) + r'(?![\w-])')),
    ('tool', _word('_'.join((_JUNEAU, 'run', 'pytest')))),
    ('tool', _word('_'.join((_JUNEAU, 'run', 'playwright')))),
    ('tool', _word('_'.join((_JUNEAU, 'scripts')))),
    ('tool', _tok(_JUNEAU, 'scripts')),
    ('tool', _prefix('_'.join(('JUNEAU', 'SCRIPTS')) + '_')),
    ('tool', _word('_'.join(('RUN', 'MARKERS')))),
    ('tool', _word('_'.join(('RUN', 'ARTIFACTS')))),
    ('tool slug', _slug('Juneau' + 'Run')),
    ('tool slug', _slug('Juneau' + 'Test')),
    ('tool slug', _slug('Juneau' + 'Test' + 'JUnitExtensions')),
    # The run feature
    ('run feature', re.compile('##' + r'run(?!\w)')),
    ('run feature', _word('Run' + 'Event')),
    ('run feature', _word('Run' + 'View' + 'Checks')),
    ('run feature', _word('Run' + 'View' + 'CardType')),
    ('run feature', _prefix('Run' + 'View' + r'(?:Def|Endpoints|Log|Mixin|Page|Source|_BrowserTest|Css_Test)(?![a-z0-9])')),
    ('run feature', _tok(_JUNEAU, 'run', 'view')),
    ('run feature', _tok('run', 'view')),
    ('run feature', _word('run' + 'View')),
    ('run feature', _word('_'.join(('run', 'view')))),
    ('run feature', re.compile(r'(?<!\w)' + 'Run' + ' view' + r'(?!\w)')),
    ('run feature slug', _slug('Driving' + 'ARun')),
    ('run feature slug', _slug('Run' + 'View' + 'Region')),
    # Other
    ('script id', _pkg(_JUNEAU, 'run', 'script')),
    ('protocol', _tok('run', 'protocol', 'v1')),
    ('bootstrap keyword', re.compile(r'\bload' + r'\(' + r'[^\n]*\b' + 'run' + '=')),
)


def find_old_names(text):
    """Every hit in `text` as (label, lineno, matched_text); line numbers are 1-based."""
    hits = []
    for lineno, line in enumerate(text.splitlines(), 1):
        for label, pattern in OLD_NAME_PATTERNS:
            for m in pattern.finditer(line):
                hits.append((label, lineno, m.group(0)))
    return hits
