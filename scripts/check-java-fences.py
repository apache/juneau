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
Parse every ```java fence in the docs pages and report the ones that are not valid Java.

Full docs: check-java-fences.md
"""

import argparse
import subprocess
import sys
import tempfile
from pathlib import Path

from code_fences import iter_fences, page_files

HELPER = Path(__file__).resolve().parent / 'JavaFenceParse.java'
DEFAULT_JAVA = Path.home() / 'jdk' / 'default' / 'bin' / 'java'


def find_java_fences(docs_root):
    """Return [(relative path, line, body)] for each ```java fence not marked with the `fragment` meta flag."""
    fences = []
    for page in page_files(docs_root):
        for start, info, body in iter_fences(page.read_text(encoding='utf-8')):
            words = info.split()
            if words and words[0] == 'java' and 'fragment' not in words[1:]:
                fences.append((str(page.relative_to(docs_root)), start, body))
    return fences


def parse_fences(fences, java):
    """Run the JDK-parser helper over the fences; return the list of indexes that failed to parse."""
    with tempfile.TemporaryDirectory() as tmp:
        for n, (_rel, _line, body) in enumerate(fences):
            Path(tmp, f'{n}.java.txt').write_text(body, encoding='utf-8')
        result = subprocess.run([str(java), str(HELPER), tmp, str(len(fences))], text=True, capture_output=True)
    if result.returncode != 0:
        raise RuntimeError(f'{HELPER.name} failed ({result.returncode}): {result.stderr.strip()}')
    return [int(line.split()[1]) for line in result.stdout.splitlines() if line.startswith('FAIL ')]


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Report ```java fences in pages/ that do not parse as Java.')
    parser.add_argument('--docs-root', type=Path, default=Path(__file__).resolve().parent.parent,
                        help='docs checkout to scan (default: parent of scripts/)')
    parser.add_argument('--java', type=Path, default=DEFAULT_JAVA, help='java launcher (JDK 17+; default: ~/jdk/default/bin/java)')
    parser.add_argument('--strict', action='store_true', help='exit 1 when any fence fails to parse')
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    fences = find_java_fences(args.docs_root.resolve())
    failed = parse_fences(fences, args.java)
    for n in failed:
        rel, line, _body = fences[n]
        print(f"{rel}:{line}: java fence does not parse")
    print(f"Java fences checked: {len(fences)}; failing: {len(failed)}; passing: {len(fences) - len(failed)}")
    if failed and args.strict:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
