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
"""
Check that the docs entry pages tell one story: one format count tied to the marshaller code, one canonical first
resource, one set of ports and no comparison claims that Framework Comparisons does not support.

Full docs: check-entry-pages.md
"""

import argparse
import re
import sys
from pathlib import Path

from code_fences import iter_fences, page_files
from juneau_root import find_juneau_root

# The entry pages (spec §3.1, E1 to E16 less E9); the About pages (E9) are added by about_pages().
ENTRY_PAGES = (
    'pages/topics/00.00.GettingStarted.md',
    'pages/topics/00.01.GettingStartedInstall.md',
    'pages/topics/00.02.GettingStartedSerializer.md',
    'pages/topics/00.03.GettingStartedRestApp.md',
    'pages/topics/00.04.GettingStartedClient.md',
    'pages/topics/01.00.JuneauEcosystemOverview.md',
    'pages/topics/01.01.FrameworkComparisons.md',
    'pages/topics/01.02.WhyJuneau.md',
    'src/pages/index.tsx',
    'pages/topics/03.00.JuneauMarshall.md',
    'pages/topics/10.00.JuneauRestServer.md',
    'pages/topics/15.01.MicroserviceOverview.md',
    'pages/topics/11.01.JuneauMcpQuickstart.md',
    'pages/topics/19.00.JuneauPetstore.md',
    'pages/topics/24.StarterProjects.md',
)
ABOUT_DIR = 'src/pages/about'
ABOUT_FALLBACK = 'src/pages/about.md'
RELEASE_NOTES = 'pages/release-notes/'
ALLOWLIST = 'scripts/check-entry-pages-allowlist.txt'

MARSHALL_PAGE = 'pages/topics/03.00.JuneauMarshall.md'
MARSHALL_DIRS = (
    Path('juneau-core/juneau-marshall/src/main/java/org/apache/juneau/marshall/marshaller'),
    Path('juneau-core/juneau-marshall-rdf/src/main/java/org/apache/juneau/marshall/jena/marshaller'),
)
RDF_HREF = '/docs/topics/Rdf'
# "38 formats", "38+ formats", "[38 formats](link)", each optionally followed by "(26 in ..., 12 RDF ...)".
COUNT_RE = re.compile(r'\b(\d+)(\+?)\s+formats\b(?:\]\([^)\s]*\))?'
                      r'(?:\s*\((\d+)\s+in\b[^,)]*,\s*(\d+)\s+RDF\b[^)]*\))?', re.I)
BADGE_RE = re.compile(r'<a href="([^"]+)"><span className="badge\b')
FACADE_RE = re.compile(r'^public (?:final )?class \w+ extends (?:CharMarshaller|StreamMarshaller)\b', re.M)

MCP_QUICKSTART = 'pages/topics/11.01.JuneauMcpQuickstart.md'
PORT_RE = re.compile(r'localhost:(\d+)')
PORTS = {10000, 8080, 5000}
BANNED_PHRASES = ('zero configuration', 'zero-dependency', 'zero dependencies', 'minimal memory footprint',
                  'fast startup', 'productive immediately', 'fewer security vulnerabilities',
                  'complex annotation system', 'minimal dependencies')

K2A_BANNED = {
    '/swagger-ui.html': 'Swagger UI is at /swagger (Swagger v2 JSON at /api, OpenAPI 3.1 at /openapi)',
    'Java-serialized-object': 'Juneau has no Java-serialized-object serializer',
}
K2B_BANNED = 'org.apache.juneau.rest.annotation'
FIRST_RESOURCE_PAGES = (
    'pages/topics/00.03.GettingStartedRestApp.md',
    'pages/topics/01.02.WhyJuneau.md',
    'pages/topics/10.00.JuneauRestServer.md',
    'pages/topics/15.01.MicroserviceOverview.md',
)
ABOUT_REST_PAGE = 'src/pages/about/rest-server.mdx'
BARE_PATH_RE = re.compile(r'@Path\b(?!\s*\()')
TIP_LINK = '/docs/topics/GettingStartedRestApp#servlet-or-resource'


def line_of(text, offset):
    """1-based line number of a character offset."""
    return text.count('\n', 0, offset) + 1


def read(docs_root, rel):
    return Path(docs_root, rel).read_text(encoding='utf-8')


def about_pages(docs_root):
    """The About pages: src/pages/about/*.mdx, or src/pages/about.md if there are none."""
    pages = sorted(p.relative_to(docs_root).as_posix() for p in Path(docs_root, ABOUT_DIR).glob('*.mdx'))
    if not pages and Path(docs_root, ABOUT_FALLBACK).is_file():
        pages = [ABOUT_FALLBACK]
    return pages


def entry_pages(docs_root):
    """The entry pages that exist, About pages included."""
    return [rel for rel in ENTRY_PAGES if Path(docs_root, rel).is_file()] + about_pages(docs_root)


def check_k0(docs_root):
    """K0: every entry page exists, and there is an About page."""
    failures = [(rel, 1, 'K0', '', 'entry page missing') for rel in ENTRY_PAGES if not Path(docs_root, rel).is_file()]
    if not about_pages(docs_root):
        failures.append((ABOUT_DIR, 1, 'K0', '', f'no About page ({ABOUT_DIR}/*.mdx or {ABOUT_FALLBACK})'))
    return failures


def badge_counts(text):
    """(core, rdf) badge counts on the JuneauMarshall page: an RDF badge links to /docs/topics/Rdf."""
    hrefs = BADGE_RE.findall(text)
    rdf = sum(1 for h in hrefs if h == RDF_HREF)
    return len(hrefs) - rdf, rdf


def count_facades(directory):
    """Marshaller facades directly in a directory: public non-abstract classes extending Char/StreamMarshaller."""
    return sum(1 for f in sorted(Path(directory).glob('*.java')) if FACADE_RE.search(f.read_text(encoding='utf-8')))


def check_k1(docs_root, juneau_root, pages):
    """K1a: every format count on an entry page matches the badges. K1b: the badges match the facade classes."""
    failures, notes = [], []
    marshall = Path(docs_root, MARSHALL_PAGE)
    if not marshall.is_file():
        return failures, notes  # K0 reports it
    mtext = marshall.read_text(encoding='utf-8')
    core, rdf = badge_counts(mtext)
    total = core + rdf
    if total == 0:
        failures.append((MARSHALL_PAGE, 1, 'K1a', '', 'no format badges found; K1a needs the badge block'))
    for rel in pages:
        text = read(docs_root, rel)
        for m in COUNT_RE.finditer(text):
            n, plus, pair_core, pair_rdf = m.groups()
            lit, line = f'{n}{plus} formats', line_of(text, m.start())
            if plus:
                failures.append((rel, line, 'K1a', lit, f"'{lit}': no '+' on a format count; write '{total} formats'"))
            elif total and int(n) != total:
                failures.append((rel, line, 'K1a', lit,
                                 f"'{lit}': the count is {total} ({core} core + {rdf} RDF badges on {MARSHALL_PAGE})"))
            if total and pair_core is not None and (int(pair_core), int(pair_rdf)) != (core, rdf):
                failures.append((rel, line, 'K1a', lit,
                                 f"'{lit}': the split is ({core} in juneau-marshall, {rdf} RDF in juneau-marshall-rdf), "
                                 f"not ({pair_core}, {pair_rdf})"))
    dirs = [Path(juneau_root, d) for d in MARSHALL_DIRS]
    if total and all(d.is_dir() for d in dirs):
        facades = tuple(count_facades(d) for d in dirs)
        if facades != (core, rdf):
            failures.append((MARSHALL_PAGE, line_of(mtext, mtext.find('<a href=')), 'K1b', f'{core}+{rdf}',
                             f'badges show {core} core + {rdf} RDF formats; '
                             f'the marshaller packages have {facades[0]} + {facades[1]} facades'))
    elif total:
        notes.append(f'K1b skipped: no Juneau checkout at {juneau_root}')
    return failures, notes


def fence_lines(text, langs):
    """Yield (line number, line) for each body line of a fence whose language (first info word) is in langs."""
    for start, info, body in iter_fences(text):
        words = info.split()
        if words and words[0].lower() in langs:
            for i, line in enumerate(body.split('\n')):
                yield start + 1 + i, line


def site_files(docs_root):
    """K2a's scope: pages/**/*.md and src/pages/** (.md, .mdx, .tsx), less the release notes."""
    files = sorted(Path(docs_root, 'pages').rglob('*.md'))
    files += sorted(p for p in Path(docs_root, 'src/pages').rglob('*') if p.suffix in ('.md', '.mdx', '.tsx'))
    rels = [p.relative_to(docs_root).as_posix() for p in files if p.is_file()]
    return [rel for rel in rels if not rel.startswith(RELEASE_NOTES)]


def java_fence_files(docs_root):
    """K2b's scope: page_files() (pages/**/*.md and src/pages/about/*.mdx) plus the about.md fallback, less the release notes."""
    rels = [p.relative_to(docs_root).as_posix() for p in page_files(docs_root)]
    rels += [rel for rel in about_pages(docs_root) if rel not in rels]
    return [rel for rel in rels if not rel.startswith(RELEASE_NOTES)]


def check_k2(docs_root):
    """K2a: no wrong literals anywhere on the site. K2b: no pre-rename annotation package in Java fences."""
    failures = []
    for rel in site_files(docs_root):
        text = read(docs_root, rel)
        for lit, why in K2A_BANNED.items():
            for m in re.finditer(re.escape(lit), text):
                failures.append((rel, line_of(text, m.start()), 'K2a', lit, f"'{lit}': {why}"))
    for rel in java_fence_files(docs_root):
        for n, line in fence_lines(read(docs_root, rel), {'java'}):
            if K2B_BANNED in line:
                failures.append((rel, n, 'K2b', K2B_BANNED,
                                 f"'{K2B_BANNED}' no longer exists; import org.apache.juneau.rest.server.*"))
    return failures


def check_k3(docs_root):
    """K3a: @Path is named. K3b: BasicRestResource links to the tip. K3c: no shaded jar in a dependency snippet."""
    failures = []
    pages = list(FIRST_RESOURCE_PAGES) + [ABOUT_REST_PAGE]
    for rel in [p for p in pages if Path(docs_root, p).is_file()]:
        text = read(docs_root, rel)
        for n, line in fence_lines(text, {'java'}):
            if BARE_PATH_RE.search(line):
                failures.append((rel, n, 'K3a', line.strip(), '@Path needs a name, e.g. @Path("id")'))
            if 'extends BasicRestResource' in line and TIP_LINK not in text:
                failures.append((rel, n, 'K3b', line.strip(), f'BasicRestResource on this page needs a link to {TIP_LINK}'))
        for n, line in fence_lines(text, {'xml'}):
            if 'juneau-shaded-all' in line:
                failures.append((rel, n, 'K3c', line.strip(),
                                 'use the juneau-bom import + juneau-microservice-jetty-bundle (see Install Juneau)'))
    return failures


def prose(text):
    """The text with every fenced block (opener, body and closer) blanked, keeping line numbers."""
    lines = text.split('\n')
    for start, _info, body in iter_fences(text):
        first = start - 1
        last = first + (len(body.split('\n')) if body else 0) + 1
        for i in range(first, min(last + 1, len(lines))):
            lines[i] = ''
    return '\n'.join(lines)


def check_k4(docs_root, pages):
    """K4a: entry pages use only the documented ports. K4b: the MCP quickstart uses 10000."""
    failures = []
    for rel in pages:
        text = read(docs_root, rel)
        for m in PORT_RE.finditer(text):
            port, line = int(m.group(1)), line_of(text, m.start())
            lit = f'localhost:{port}'
            if rel == MCP_QUICKSTART:
                if port != 10000:
                    failures.append((rel, line, 'K4b', lit, f"'{lit}': the MCP quickstart runs on the Jetty microservice, port 10000"))
            elif port not in PORTS:
                failures.append((rel, line, 'K4a', lit,
                                 f"'{lit}': not one of 10000, 8080, 5000 (see Ports used in these docs)"))
    return failures


def check_k5(docs_root, pages):
    """K5: no unsupported comparison claims in the prose of an entry page."""
    failures = []
    for rel in pages:
        text = prose(read(docs_root, rel))
        for phrase in BANNED_PHRASES:
            for m in re.finditer(re.escape(phrase), text, re.I):
                failures.append((rel, line_of(text, m.start()), 'K5', phrase,
                                 f"'{m.group(0)}': not supported by Framework Comparisons; reword or remove"))
    return failures


def check(docs_root, juneau_root):
    """Run every rule. Returns (failures, notes, pages checked); a failure is (path, line, rule, literal, message)."""
    pages = entry_pages(docs_root)
    failures = check_k0(docs_root)
    k1, notes = check_k1(docs_root, juneau_root, pages)
    failures += k1
    failures += check_k2(docs_root) + check_k3(docs_root) + check_k4(docs_root, pages) + check_k5(docs_root, pages)
    return failures, notes, len(pages)


def read_allowlist(file):
    """Parse the allow-list. Returns (entries, errors): entries are (line, path, rule, literal), errors (line, message)."""
    entries, errors = [], []
    if not Path(file).is_file():
        return entries, errors
    for n, raw in enumerate(Path(file).read_text(encoding='utf-8').split('\n'), 1):
        if not raw.strip() or raw.lstrip().startswith('#'):
            continue
        parts = raw.split('\t')
        if len(parts) != 4 or not all(p.strip() for p in parts):
            errors.append((n, 'malformed entry: want path<TAB>rule<TAB>literal<TAB>reason'))
            continue
        entries.append((n, parts[0].strip(), parts[1].strip(), parts[2].strip()))
    return entries, errors


def apply_allowlist(failures, entries):
    """Drop the failures an entry names (by path, rule and literal). Returns (kept failures, stale entries)."""
    keys = {(rel, rule, lit) for _n, rel, rule, lit in entries}
    kept = [f for f in failures if (f[0], f[2], f[3]) not in keys]
    used = {(f[0], f[2], f[3]) for f in failures}
    stale = [e for e in entries if (e[1], e[2], e[3]) not in used]
    return kept, stale


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Check that the docs entry pages tell one story '
                                                 '(format count, first resource, ports, claims).')
    parser.add_argument('--juneau-root', type=Path, default=None,
                        help='Juneau checkout for K1b (default: $JUNEAU_ROOT, then ../master, then ../juneau); '
                             'K1b is skipped without one')
    parser.add_argument('--docs-root', type=Path, default=Path(__file__).resolve().parent.parent,
                        help='Docs checkout to check (default: the parent of scripts/)')
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    docs_root = args.docs_root.resolve()
    juneau_root = find_juneau_root(docs_root, args.juneau_root, allow_missing=True)
    failures, notes, checked = check(docs_root, juneau_root)
    entries, errors = read_allowlist(docs_root / ALLOWLIST)
    failures, stale = apply_allowlist(failures, entries)
    failures += [(ALLOWLIST, n, 'allow-list', '', msg) for n, msg in errors]
    failures += [(ALLOWLIST, n, 'allow-list', lit, f'stale entry: no {rule} failure for {lit!r} in {rel}')
                 for n, rel, rule, lit in stale]
    for rel, line, rule, _lit, msg in sorted(failures):
        print(f'{rel}:{line}: {rule}: {msg}')
    for note in notes:
        print(note)
    print(f'Entry pages checked: {checked}; failures: {len(failures)}')
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main())
