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
Check that the modules in artifact-paths.json are covered by the projects in src/data/projects.ts.

Full docs: check-doc-inventory.md
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import project_index

# ---------------------------------------------------------------------------
# Structural exclusions: pure aggregator/parent POMs.
#
# These never have their own artifact content to document, so they're excluded
# unconditionally rather than via the (editable) allow-list.  Keep in sync with
# the reactor's aggregator poms if that set changes.
# ---------------------------------------------------------------------------
# The set itself lives in project_index.py, next to the projects.ts parser, so the project checker uses the same one.
AGGREGATOR_POMS = project_index.AGGREGATOR_POMS


class Findings:
    def __init__(self):
        self.items = []

    def error(self, check, message):
        self.items.append(('error', check, message))

    def warning(self, check, message):
        self.items.append(('warning', check, message))

    def note(self, check, message):
        self.items.append(('note', check, message))

    def counts(self):
        errors = sum(1 for s, _, _ in self.items if s == 'error')
        warnings = sum(1 for s, _, _ in self.items if s == 'warning')
        return errors, warnings


def load_json(path: Path) -> dict:
    if not path.exists():
        raise FileNotFoundError(f'missing required file: {path}')
    return json.loads(path.read_text(encoding='utf-8'))


def load_allowlist(path: Path) -> set[str]:
    if not path.exists():
        return set()
    out = set()
    for line in path.read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if line and not line.startswith('#'):
            out.add(line)
    return out


def run_checks(docs_dir: Path, findings: Findings) -> None:
    paths_json = load_json(docs_dir / 'artifact-paths.json')
    packages_json = load_json(docs_dir / 'artifact-packages.json')
    allowlist = load_allowlist(docs_dir / 'scripts' / 'check-doc-inventory-allowlist.txt')

    all_modules = set(paths_json)
    publishable = all_modules - AGGREGATOR_POMS - allowlist

    findings.note('inventory', f'{len(all_modules)} modules in artifact-paths.json; '
                                f'{len(AGGREGATOR_POMS)} aggregator POMs excluded; '
                                f'{len(allowlist)} allow-listed; '
                                f'{len(publishable)} expected to be publicly documented')

    # --- 1. inventory: every publishable module is in some project's modules[] ----
    try:
        _, projects = project_index.load(docs_dir)
    except (FileNotFoundError, project_index.ProjectIndexError) as e:
        findings.error('inventory', f'cannot read src/data/projects.ts: {e}')
        projects = []
    listed = {m for p in projects for m in p['modules']}
    for mod in sorted(publishable - listed):
        findings.error(
            'inventory',
            f'Publishable module `{mod}` is in no project\'s modules[] in src/data/projects.ts '
            f'(add it to a project, or add it to check-doc-inventory-allowlist.txt with a rationale)')

    # --- 2. stale-rows: every listed module is a real module ------------------------
    for mod in sorted(listed - all_modules):
        findings.error(
            'stale-rows',
            f'src/data/projects.ts lists `{mod}`, which is not a key in artifact-paths.json '
            f'(renamed or removed coordinate)')

    # --- 3. packages: artifact-packages.json <-> artifact-paths.json reconciliation --
    pkg_keys = set(packages_json)
    unknown_pkg_keys = sorted(pkg_keys - all_modules)
    for mod in unknown_pkg_keys:
        findings.error(
            'packages',
            f'artifact-packages.json has an entry for `{mod}`, which is not a key in '
            f'artifact-paths.json')

    missing_pkg_keys = sorted(publishable - pkg_keys)
    for mod in missing_pkg_keys:
        findings.warning(
            'packages',
            f'Publishable module `{mod}` has no artifact-packages.json entry '
            f'(fine if the module ships no Java source; otherwise add its base package)')

    # --- 4. allowlist: every allow-list entry is real and NOT actually documented ----
    unknown_allowlist = sorted(allowlist - all_modules)
    for mod in unknown_allowlist:
        findings.error(
            'allowlist',
            f'check-doc-inventory-allowlist.txt lists `{mod}`, which is not a key in '
            f'artifact-paths.json (stale allow-list entry — remove it)')

    for mod in sorted(allowlist & listed):
        findings.error(
            'allowlist',
            f'`{mod}` is both allow-listed as intentionally-undocumented and in a project\'s modules[] '
            f'in src/data/projects.ts; remove it from the allow-list')


def print_report(findings: Findings) -> None:
    errors = [i for i in findings.items if i[0] == 'error']
    warnings = [i for i in findings.items if i[0] == 'warning']
    notes = [i for i in findings.items if i[0] == 'note']

    def emit(items, header):
        if not items:
            return
        print(f'\n{header}')
        print('=' * len(header))
        for _, check, message in items:
            print(f'  [{check}] {message}')

    print('Juneau Documentation Inventory Checker')
    print('=' * 50)
    emit(errors, f'ERRORS ({len(errors)})')
    emit(warnings, f'WARNINGS ({len(warnings)})')
    emit(notes, 'NOTES')
    print(f'\ncheck-doc-inventory: {len(errors)} errors, {len(warnings)} warnings')


def print_json_report(findings: Findings) -> None:
    errors, warnings = findings.counts()
    payload = {
        'summary': {'errors': errors, 'warnings': warnings},
        'findings': [
            {'severity': s, 'check': c, 'message': m} for s, c, m in findings.items
        ],
    }
    print(json.dumps(payload, indent=2))


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(
        description='Documentation-inventory checker for the Juneau Docusaurus site.')
    parser.add_argument('--strict', action='store_true',
                         help='Treat warnings as errors for the exit code.')
    parser.add_argument('--json', action='store_true',
                         help='Emit machine-readable JSON instead of text.')
    parser.add_argument('--docs-dir', default=None,
                         help='Docs root (defaults to the parent of scripts/).')
    args = parser.parse_args(argv)

    docs_dir = Path(args.docs_dir).resolve() if args.docs_dir \
        else Path(__file__).parent.parent.resolve()

    findings = Findings()
    try:
        run_checks(docs_dir, findings)
    except FileNotFoundError as e:
        print(f'ERROR: {e}', file=sys.stderr)
        return 2

    if args.json:
        print_json_report(findings)
    else:
        print_report(findings)

    errors, warnings = findings.counts()
    if errors > 0:
        return 1
    if args.strict and warnings > 0:
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
