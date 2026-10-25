#!/usr/bin/env python3
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
Fail on HTML anchors that open a line inside fenced code blocks in the docs pages.

Full docs: check-code-fences.md
"""

import argparse
import re
import sys
from pathlib import Path

from code_fences import iter_fences, page_files

ANCHOR_RE = re.compile(r'^\s*<a\s[^>]*\bhref\s*=', re.IGNORECASE)


def find_anchors(docs_root):
    """Return (relative path, line, text) for each anchor tag inside a fenced code block under pages/."""
    findings = []
    for page in page_files(docs_root):
        text = page.read_text(encoding='utf-8')
        for start, _info, body in iter_fences(text):
            for offset, line in enumerate(body.split('\n'), start=1):
                if ANCHOR_RE.search(line):
                    findings.append((str(page.relative_to(docs_root)), start + offset, line.strip()))
    return findings


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Fail on HTML anchors inside fenced code blocks in pages/.')
    parser.add_argument('--docs-root', type=Path, default=Path(__file__).resolve().parent.parent,
                        help='docs checkout to scan (default: parent of scripts/)')
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    findings = find_anchors(args.docs_root.resolve())
    for rel, line, text in findings:
        print(f"{rel}:{line}: HTML anchor in code fence: {text}")
    if findings:
        print(f"FAILED: {len(findings)} HTML anchor(s) inside code fences.")
        return 1
    print("No HTML anchors inside code fences.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
