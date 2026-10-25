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
Shared fenced-code-block extraction for the code-fence checks (check-code-fences.py, check-java-fences.py, check-topic-template.py).
"""

import re
from pathlib import Path

FENCE_RE = re.compile(r'^(\s*)(`{3,}|~{3,})\s*(.*?)\s*$')


def iter_fences(text):
    """Yield (start_line, info, body) for each fenced code block in Markdown text.

    start_line is the 1-based line of the opening fence, info is everything after the fence marker (language plus
    meta), and body is the fenced text.  A fence closes on a line of the same marker character at least as long as the
    opener; an unterminated fence runs to the end of the file.
    """
    lines = text.split('\n')
    i = 0
    while i < len(lines):
        m = FENCE_RE.match(lines[i])
        if not m:
            i += 1
            continue
        marker, info = m.group(2), m.group(3)
        j = i + 1
        while j < len(lines):
            c = FENCE_RE.match(lines[j])
            if c and c.group(2)[0] == marker[0] and len(c.group(2)) >= len(marker) and not c.group(3):
                break
            j += 1
        yield i + 1, info, '\n'.join(lines[i + 1:j])
        i = j + 1


def page_files(docs_root):
    """Every Markdown page under <docs_root>/pages, sorted."""
    return sorted(Path(docs_root, 'pages').rglob('*.md'))
