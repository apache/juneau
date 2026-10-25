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
Check every ```lang guide=FILE#REGION fence in the docs pages against that region of the juneau-examples-guides module,
and require a pointer on every non-shell fence of a guide page.

Full docs: check-guide-snippets.md
"""

import argparse
import difflib
import re
import sys
import textwrap
from pathlib import Path

from code_fences import iter_fences, page_files
from juneau_root import find_juneau_root

MODULE = Path('juneau-examples', 'juneau-examples-guides')
SEARCH_ROOTS = (Path('src', 'test', 'java'), Path('src', 'test', 'resources'))
GUIDES_DIR = 'pages/guides/'
LANDING = 'pages/guides/00.Guides.md'
UNCHECKED_LANGS = {'bash', 'sh', 'shell', 'console', 'text'}
POINTER_RE = re.compile(r'^guide=([A-Za-z0-9_./-]+)#([A-Za-z0-9_-]+)$')
MARKER_RE = re.compile(r'^\s*(?://|#|<!--)\s*(region|endregion)(?:\s+([A-Za-z0-9_-]+))?\s*(?:-->)?\s*$')
DIFF_LINES = 12


def normalize(text):
    """CRLF to LF, tabs to 4-column stops, trailing whitespace stripped, common indent removed, outer blank lines dropped."""
    lines = [line.expandtabs(4).rstrip() for line in text.replace('\r\n', '\n').split('\n')]
    lines = textwrap.dedent('\n'.join(lines)).split('\n')
    while lines and not lines[0]:
        lines.pop(0)
    while lines and not lines[-1]:
        lines.pop()
    return '\n'.join(lines)


def parse_regions(path):
    """Return ({name: raw body}, [(line, message)]) for the region markers in one source file."""
    regions, errors = {}, []
    open_name, open_line, body = None, None, []
    lines = path.read_text(encoding='utf-8', errors='replace').replace('\r\n', '\n').split('\n')
    for n, line in enumerate(lines, 1):
        m = MARKER_RE.match(line)
        if not m:
            if open_name is not None:
                body.append(line)
            continue
        kind, name = m.groups()
        if kind == 'region':
            if name is None:
                errors.append((n, 'region without a name'))
            elif open_name is not None:
                errors.append((n, f'nested region `{name}` inside `{open_name}`'))
            elif name in regions:
                errors.append((n, f'duplicate region `{name}`'))
            else:
                open_name, open_line, body = name, n, []
        elif open_name is None or name != open_name:
            errors.append((n, f'endregion mismatch: `endregion{" " + name if name else ""}` '
                              f'{"closes `" + open_name + "`" if open_name else "has no open region"}'))
        else:
            regions[name] = '\n'.join(body)
            open_name = None
    if open_name is not None:
        errors.append((open_line, f'region unterminated: `{open_name}`'))
    return regions, errors


def resolve_source(module_dir, file):
    """Return (path, None) for a guide= FILE, or (None, error message).

    A bare basename must match exactly one file under the search roots; a relative path is tried under each root.
    """
    if '/' in file:
        hits = [module_dir / root / file for root in SEARCH_ROOTS if (module_dir / root / file).is_file()]
    else:
        hits = [p for root in SEARCH_ROOTS for p in sorted((module_dir / root).rglob(file)) if p.is_file()]
    if not hits:
        return None, f'guide source not found: `{file}`'
    if len(hits) > 1:
        names = ', '.join(str(p.relative_to(module_dir)) for p in hits)
        return None, f'ambiguous guide source `{file}`: {names}'
    return hits[0], None


def all_region_names(module_dir):
    """Every (relative path, region name) defined in the module's search roots."""
    found = set()
    for root in SEARCH_ROOTS:
        for path in sorted((module_dir / root).rglob('*')):
            if path.is_file():
                rel = str(path.relative_to(module_dir))
                found.update((rel, name) for name in parse_regions(path)[0])
    return found


def check(docs_root, module_dir):
    """Return (failures, checked, unreferenced): failures is [(page, line, message, diff lines)]."""
    failures, checked, referenced = [], 0, set()
    module_ok = module_dir is not None and module_dir.is_dir()
    for page in page_files(docs_root):
        rel = page.relative_to(docs_root).as_posix()
        on_guide_page = rel.startswith(GUIDES_DIR) and rel != LANDING
        for start, info, body in iter_fences(page.read_text(encoding='utf-8')):
            words = info.split()
            lang = words[0] if words else ''
            pointers = [w for w in words[1:] if w.startswith('guide=')]
            if not pointers:
                if on_guide_page and lang not in UNCHECKED_LANGS:
                    failures.append((rel, start, 'guide fence without pointer', []))
                continue
            checked += 1
            if len(pointers) > 1:
                failures.append((rel, start, f'more than one guide= pointer: {" ".join(pointers)}', []))
                continue
            m = POINTER_RE.match(pointers[0])
            if not m:
                failures.append((rel, start, f'malformed guide= pointer `{pointers[0]}` (want guide=FILE#REGION)', []))
                continue
            file, region = m.groups()
            if not module_ok:
                failures.append((rel, start, f'guide module not found: {module_dir}', []))
                continue
            source, error = resolve_source(module_dir, file)
            if error:
                failures.append((rel, start, error, []))
                continue
            source_rel = str(source.relative_to(module_dir))
            referenced.add((source_rel, region))
            regions, errors = parse_regions(source)
            if errors:
                for line, message in errors:
                    failures.append((rel, start, f'{source_rel}:{line}: {message}', []))
                continue
            if region not in regions:
                failures.append((rel, start, f'region not found: `{file}#{region}`', []))
                continue
            want, got = normalize(regions[region]), normalize(body)
            if want != got:
                diff = list(difflib.unified_diff(want.split('\n'), got.split('\n'), f'{file}#{region}', rel, lineterm=''))
                failures.append((rel, start, f'guide fence does not match {file}#{region}', diff[:DIFF_LINES]))
    unreferenced = len(all_region_names(module_dir) - referenced) if module_ok else 0
    return failures, checked, unreferenced


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Check guide= fences in pages/ against the juneau-examples-guides module.')
    parser.add_argument('--juneau-root', type=Path, default=None,
                        help='Juneau checkout (default: $JUNEAU_ROOT, then ../master, then ../juneau)')
    parser.add_argument('--docs-root', type=Path, default=Path(__file__).resolve().parent.parent,
                        help='docs checkout to scan (default: parent of scripts/)')
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    docs_root = args.docs_root.resolve()
    juneau_root = find_juneau_root(docs_root, args.juneau_root, allow_missing=True)
    failures, checked, unreferenced = check(docs_root, juneau_root / MODULE)
    for rel, line, message, diff in failures:
        print(f'{rel}:{line}: {message}')
        for d in diff:
            print(f'    {d}')
    failing = len({(rel, line) for rel, line, _message, _diff in failures})
    print(f'Guide fences checked: {checked}; failing: {failing}; regions unreferenced: {unreferenced}')
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
