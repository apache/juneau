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
Documentation-inventory checker for the Apache Juneau Docusaurus site.

Guards against the module list drifting away from what's actually documented on the
public `/about` page (and any other inventory page this script is taught about).

Source of truth for "what modules exist": the keys of `artifact-paths.json`, which is
already hand-maintained to track essentially every reactor module (both `artifact-paths.json`
and `artifact-packages.json` are read so that stale/missing `artifact-packages.json`
entries are also reported). Two kinds of module are expected to be ABSENT from the
inventory pages:

  1. Pure aggregator/parent POMs — structurally never leaf artifacts, detected here via
     a hardcoded set (they don't change often; see AGGREGATOR_POMS below) rather than by
     parsing the Maven reactor, so this script has zero build-tool dependencies.
  2. Explicitly allow-listed modules — real leaf modules that are deliberately not part
     of the public ecosystem table (test helpers, example/test scaffolding, petstore
     submodules folded into one row, etc). See `check-doc-inventory-allowlist.txt`.

Everything else in `artifact-paths.json` is a "publishable module" that MUST appear on
every inventory page this script checks (currently just `src/pages/about.md`'s
"Ecosystem" table). A publishable module missing from an inventory page is an ERROR.

This is pure Python 3.9+ standard library (no third-party deps) and does NOT invoke
Docusaurus or Maven — safe to run at any time, including while other builds are in
flight against the sibling `juneau` code tree.

Checks:
  1. inventory   Every publishable module (artifact-paths.json minus aggregators minus
                 allow-list) appears in every configured inventory page.
  2. stale-rows  Every module referenced by an inventory page actually exists in
                 artifact-paths.json (catches rows for renamed/removed coordinates).
  3. packages    Every artifact-packages.json key is also an artifact-paths.json key
                 (catches stale/renamed package entries), and vice versa for
                 publishable modules (warning only — some leaf modules legitimately
                 ship no Java source, e.g. resource-only or reserved modules).
  4. allowlist   Every allow-list entry actually exists in artifact-paths.json and is
                 NOT also present in an inventory page (catches a stale allow-list).

Usage:
    python3 scripts/check-doc-inventory.py [--strict] [--json] [--docs-dir DIR]

Exit code: 0 when there are no errors; non-zero when errors exist (and, under
--strict, when warnings exist).
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

# ---------------------------------------------------------------------------
# Structural exclusions: pure aggregator/parent POMs.
#
# These never have their own artifact content to document, so they're excluded
# unconditionally rather than via the (editable) allow-list.  Keep in sync with
# TODO-347's "Pure aggregator poms" list if the reactor's aggregator set changes.
# ---------------------------------------------------------------------------
AGGREGATOR_POMS = {
    'juneau',
    'juneau-core',
    'juneau-rest',
    'juneau-bean',
    'juneau-microservice-parent',
    'juneau-sc',
    'juneau-shaded',
    'juneau-bundles',
    'juneau-examples',
    'juneau-petstore',
    'juneau-bom',
    'juneau-distrib',
    'juneau-integration-tests',
}

# Inventory pages checked for module-row coverage, relative to the docs root.
# Each entry is a Markdown/MDX file containing `| [module-name](...) | ... |` rows.
INVENTORY_PAGES = [
    'src/pages/about.md',
]

MODULE_ROW_RE = re.compile(r'^\s*\|\s*\[([A-Za-z0-9_-]+)\]\(')


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


def extract_inventory_modules(path: Path) -> set[str]:
    if not path.exists():
        return set()
    modules = set()
    for line in path.read_text(encoding='utf-8').splitlines():
        m = MODULE_ROW_RE.match(line)
        if m:
            modules.add(m.group(1))
    return modules


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

    # --- 1. inventory: every publishable module appears on every inventory page -----
    for rel in INVENTORY_PAGES:
        page = docs_dir / rel
        if not page.exists():
            findings.error('inventory', f'Configured inventory page not found: {rel}')
            continue
        listed = extract_inventory_modules(page)
        missing = sorted(publishable - listed)
        for mod in missing:
            findings.error(
                'inventory',
                f'Publishable module `{mod}` is missing from {rel} '
                f'(add a row, or add it to check-doc-inventory-allowlist.txt with a rationale)')

        # --- 2. stale-rows: every row references a real module -----------------
        stale = sorted(listed - all_modules)
        for mod in stale:
            findings.error(
                'stale-rows',
                f'{rel} references `{mod}`, which is not a key in artifact-paths.json '
                f'(renamed/removed coordinate, or a page-name collision — e.g. the old '
                f'un-versioned `juneau-bean-mcp`)')

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

    for rel in INVENTORY_PAGES:
        page = docs_dir / rel
        if not page.exists():
            continue
        listed = extract_inventory_modules(page)
        contradicted = sorted(allowlist & listed)
        for mod in contradicted:
            findings.warning(
                'allowlist',
                f'`{mod}` is both allow-listed as intentionally-undocumented AND listed '
                f'in {rel} — remove it from the allow-list')


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
