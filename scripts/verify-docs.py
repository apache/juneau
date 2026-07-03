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
Documentation-integrity checker for the Apache Juneau Docusaurus site.

This verifies the invariants that keep pages/topics/*.md, their YAML frontmatter,
sidebars.ts, and the cross-page links mutually consistent. It is pure Python 3
standard library (no third-party dependencies) and does NOT invoke Docusaurus.

Conventions enforced (see the individual check_* functions for details):

Topic pages live in pages/topics/*.md. Each has YAML frontmatter with:
  - id    (optional; the EFFECTIVE id is this when present, else the filename stem)
  - title (required)
  - slug  (required; must equal the PascalCase name part of the filename)

Filenames look like  NN(.NN)*.PascalName.md  -- one or more 2-digit zero-padded
numeric segments, then a PascalCase name, then '.md'. LANDING pages (the target of
a sidebar category's link.id) carry a trailing '.00' numeric segment so they sort
before their children in a directory listing; leaf pages must NOT carry a spurious
trailing '.00'.

sidebars.ts references pages by id ('topics/<ID>'); category link.id targets are
landing pages. Body links between pages use /docs/topics/<slug> (or a bare relative
<slug> within topic pages). MDX imports use @site/pages/topics/<file>.

Checks (ERRORS: 1-9, 11, 12; WARNINGS: 10, 13, 14, 15):
   1 frontmatter        Required frontmatter (title, slug) present and non-empty.
   2 slug-filename      slug == PascalCase name part of filename.
   3 filename-format    Filename matches ^\\d\\d(\\.\\d\\d)*\\.[A-Z][A-Za-z0-9]*\\.md$
   4 unique-ids         Slugs and effective ids are unique across topic files.
   5 landing-00         Category-link targets end in '.00'; leaves don't.
   6 dangling           Every sidebar topics/<ID> resolves to a file.
   7 orphans            Every topic file is referenced in sidebars.ts (see allowlist).
   8 counts             File count vs distinct sidebar doc-ref count (+ extras each side).
   9 order              Sidebar depth-first order == directory (filename) order.
  10 labels             Sidebar dotted-number label matches target file prefix. (warn)
  11 links              /docs/topics/<slug> (and bare relative) links resolve.
  12 imports            @site/pages/topics/<file> imports resolve.
  13 javadoc            /site/apidocs/... link count + format sanity. (warn)
  14 anchors            /docs/topics/<slug>#<frag> heading targets exist. (--anchors, warn)
  15 hygiene            Duplicate titles, near-empty pages, stray files. (warn)

Usage:
    python3 scripts/verify-docs.py [--strict] [--anchors] [--json]
                                   [--only IDS] [--skip IDS] [--docs-dir DIR]

Options:
    --strict       Treat warnings as errors for the exit code.
    --anchors      Enable check 14 (anchor-target resolution).
    --json         Emit machine-readable JSON instead of the text report.
    --only IDS     Run only the given checks (comma-separated numbers or names).
    --skip IDS     Skip the given checks (comma-separated numbers or names).
    --docs-dir DIR Docs root (defaults to the parent of this script's directory).

Exit code: 0 when there are no errors; non-zero when errors exist (and, under
--strict, when warnings exist).
"""

import argparse
import json
import re
import sys
from pathlib import Path

# ---------------------------------------------------------------------------
# Check registry (number -> stable name). Order is the reporting order.
# ---------------------------------------------------------------------------
CHECKS = [
    (1, 'frontmatter'),
    (2, 'slug-filename'),
    (3, 'filename-format'),
    (4, 'unique-ids'),
    (5, 'landing-00'),
    (6, 'dangling'),
    (7, 'orphans'),
    (8, 'counts'),
    (9, 'order'),
    (10, 'labels'),
    (11, 'links'),
    (12, 'imports'),
    (13, 'javadoc'),
    (14, 'anchors'),
    (15, 'hygiene'),
]
NAME_BY_NUM = {n: name for n, name in CHECKS}
NUM_BY_NAME = {name: n for n, name in CHECKS}
WARNING_CHECKS = {10, 13, 14, 15}

FILENAME_RE = re.compile(r'^(\d\d(?:\.\d\d)*)\.([A-Z][A-Za-z0-9]*)\.md$')
# Leading numeric prefix + trailing '.md' stripped => PascalCase name part.
NAME_PART_RE = re.compile(r'^\d\d(?:\.\d\d)*\.([A-Z][A-Za-z0-9]*)\.md$')


# ---------------------------------------------------------------------------
# Findings collector
# ---------------------------------------------------------------------------
class Findings:
    """Accumulates errors/warnings/notes tagged with their originating check."""

    def __init__(self):
        self.items = []  # list of dicts: severity, check, num, message, file, line

    def add(self, severity, num, message, file=None, line=None):
        self.items.append({
            'severity': severity,
            'check': NAME_BY_NUM[num],
            'num': num,
            'message': message,
            'file': file,
            'line': line,
        })

    def error(self, num, message, file=None, line=None):
        self.add('error', num, message, file, line)

    def warning(self, num, message, file=None, line=None):
        self.add('warning', num, message, file, line)

    def note(self, num, message, file=None, line=None):
        self.add('note', num, message, file, line)

    def counts(self, strict):
        errors = sum(1 for i in self.items if i['severity'] == 'error')
        warnings = sum(1 for i in self.items if i['severity'] == 'warning')
        return errors, warnings


# ---------------------------------------------------------------------------
# Minimal, dependency-free sidebars.ts parser
# ---------------------------------------------------------------------------
def _strip_js_comments(text):
    """Remove /* */ and // comments while respecting single-quoted strings."""
    out = []
    i, n = 0, len(text)
    in_str = False
    while i < n:
        c = text[i]
        if in_str:
            out.append(c)
            if c == '\\' and i + 1 < n:
                out.append(text[i + 1])
                i += 2
                continue
            if c == "'":
                in_str = False
            i += 1
            continue
        if c == "'":
            in_str = True
            out.append(c)
            i += 1
            continue
        if c == '/' and i + 1 < n and text[i + 1] == '*':
            j = text.find('*/', i + 2)
            i = n if j == -1 else j + 2
            continue
        if c == '/' and i + 1 < n and text[i + 1] == '/':
            j = text.find('\n', i + 2)
            i = n if j == -1 else j
            continue
        out.append(c)
        i += 1
    return ''.join(out)


class _JsParser:
    """Parses the restricted JS-object-literal subset used by sidebars.ts."""

    def __init__(self, text):
        self.s = text
        self.i = 0
        self.n = len(text)

    def _ws(self):
        while self.i < self.n and self.s[self.i] in ' \t\r\n':
            self.i += 1

    def parse_value(self):
        self._ws()
        c = self.s[self.i]
        if c == '{':
            return self._object()
        if c == '[':
            return self._array()
        if c == "'":
            return self._string()
        return self._bareword()

    def _object(self):
        obj = {}
        self.i += 1  # consume '{'
        while True:
            self._ws()
            if self.s[self.i] == '}':
                self.i += 1
                return obj
            key = self._key()
            self._ws()
            assert self.s[self.i] == ':', f'expected : at {self.i}'
            self.i += 1
            obj[key] = self.parse_value()
            self._ws()
            if self.i < self.n and self.s[self.i] == ',':
                self.i += 1

    def _array(self):
        arr = []
        self.i += 1  # consume '['
        while True:
            self._ws()
            if self.s[self.i] == ']':
                self.i += 1
                return arr
            arr.append(self.parse_value())
            self._ws()
            if self.i < self.n and self.s[self.i] == ',':
                self.i += 1

    def _key(self):
        self._ws()
        if self.s[self.i] == "'":
            return self._string()
        start = self.i
        while self.i < self.n and (self.s[self.i].isalnum() or self.s[self.i] in '_$'):
            self.i += 1
        return self.s[start:self.i]

    def _string(self):
        self.i += 1  # consume opening quote
        buf = []
        while self.i < self.n:
            c = self.s[self.i]
            if c == '\\' and self.i + 1 < self.n:
                buf.append(self.s[self.i + 1])
                self.i += 2
                continue
            if c == "'":
                self.i += 1
                return ''.join(buf)
            buf.append(c)
            self.i += 1
        raise ValueError('unterminated string literal')

    def _bareword(self):
        start = self.i
        while self.i < self.n and self.s[self.i] not in ',}] \t\r\n':
            self.i += 1
        word = self.s[start:self.i]
        if word == 'true':
            return True
        if word == 'false':
            return False
        if word == 'null':
            return None
        return word


def parse_sidebars(text):
    """Return the parsed `mainSidebar` array from sidebars.ts source text."""
    text = _strip_js_comments(text)
    m = re.search(r'mainSidebar\s*:', text)
    if not m:
        raise ValueError('mainSidebar not found in sidebars.ts')
    parser = _JsParser(text)
    parser.i = m.end()
    return parser.parse_value()


# ---------------------------------------------------------------------------
# Sidebar tree walkers
# ---------------------------------------------------------------------------
def _topics_id(raw):
    """Return the effective id for a raw sidebar id, or None if not a topics ref."""
    if isinstance(raw, str) and raw.startswith('topics/'):
        return raw[len('topics/'):]
    return None


def collect_sidebar(nodes):
    """Walk the sidebar tree, returning:

    - doc_refs:    effective ids referenced by type:'doc' entries (with multiplicity).
    - landing_ids: effective ids that are a category link.id (LANDING pages).
    - preorder:    depth-first pre-order sequence of topic ids (category link first,
                   then its items).
    - labels:      list of (effective_id, label) pairs for topics targets.
    """
    doc_refs = []
    landing_ids = []
    preorder = []
    labels = []

    def walk(node):
        if isinstance(node, list):
            for item in node:
                walk(item)
            return
        if not isinstance(node, dict):
            return
        ntype = node.get('type')
        if ntype == 'doc':
            tid = _topics_id(node.get('id'))
            if tid is not None:
                doc_refs.append(tid)
                preorder.append(tid)
                if 'label' in node:
                    labels.append((tid, node['label']))
        elif ntype == 'category':
            link = node.get('link')
            link_id = None
            if isinstance(link, dict):
                link_id = _topics_id(link.get('id'))
            if link_id is not None:
                landing_ids.append(link_id)
                preorder.append(link_id)
                if 'label' in node:
                    labels.append((link_id, node['label']))
            for item in node.get('items', []):
                walk(item)

    walk(nodes)
    return doc_refs, landing_ids, preorder, labels


# ---------------------------------------------------------------------------
# Topic file model
# ---------------------------------------------------------------------------
class Topic:
    def __init__(self, path):
        self.path = path
        self.name = path.name
        self.text = path.read_text(encoding='utf-8')
        self.frontmatter, self.body = _split_frontmatter(self.text)
        self.id = self.frontmatter.get('id')
        self.title = self.frontmatter.get('title')
        self.slug = self.frontmatter.get('slug')
        m = FILENAME_RE.match(self.name)
        self.numeric_prefix = m.group(1) if m else None
        self.name_part = m.group(2) if m else None

    @property
    def effective_id(self):
        # Explicit frontmatter id wins; otherwise the filename without '.md'.
        if self.id:
            return self.id
        return self.name[:-3] if self.name.endswith('.md') else self.name


def _split_frontmatter(text):
    """Return (frontmatter dict, body string). Frontmatter is a simple key: value block."""
    m = re.match(r'^---\s*\n(.*?)\n---\s*\n?(.*)$', text, re.DOTALL)
    if not m:
        return {}, text
    fm = {}
    for line in m.group(1).splitlines():
        line = line.rstrip()
        if not line or line.lstrip().startswith('#'):
            continue
        km = re.match(r'^([A-Za-z0-9_-]+)\s*:\s*(.*)$', line)
        if not km:
            continue
        key = km.group(1)
        val = km.group(2).strip()
        if len(val) >= 2 and val[0] in '"\'' and val[-1] == val[0]:
            val = val[1:-1]
        fm[key] = val
    return fm, m.group(2)


def _padded_prefix_from_label(label):
    """Extract a zero-padded numeric prefix from a sidebar label, or None.

    '3.3.1. Java Beans Support' -> '03.03.01' ; '8. juneau-rest' -> '08'.
    """
    m = re.match(r'^\s*(\d+(?:\.\d+)*)\.\s', label)
    if not m:
        return None
    return '.'.join(seg.zfill(2) for seg in m.group(1).split('.'))


# ---------------------------------------------------------------------------
# Body-scan helpers (strip code so links inside code blocks are ignored)
# ---------------------------------------------------------------------------
def _strip_code(text):
    text = re.sub(r'```.*?```', '', text, flags=re.DOTALL)
    text = re.sub(r'`[^`]*`', '', text)
    return text


def _iter_lines_with(text, pattern):
    """Yield (lineno, match) for a compiled pattern over the code-stripped text."""
    stripped = _strip_code(text)
    for m in pattern.finditer(stripped):
        line = stripped.count('\n', 0, m.start()) + 1
        yield line, m


def _github_anchor(heading):
    """Approximate Docusaurus/GitHub heading-anchor slugification."""
    h = heading.strip().lower()
    h = re.sub(r'`', '', h)
    h = re.sub(r'[^a-z0-9 \-_]', '', h)
    h = h.replace(' ', '-')
    h = re.sub(r'-+', '-', h).strip('-')
    return h


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------
def run_checks(docs_dir, enabled, anchors_enabled, findings):
    topics_dir = docs_dir / 'pages' / 'topics'
    pages_dir = docs_dir / 'pages'
    sidebars_path = docs_dir / 'sidebars.ts'
    allowlist_path = docs_dir / 'scripts' / 'verify-docs-allowlist.txt'

    md_files = sorted(topics_dir.glob('*.md'), key=lambda p: p.name)
    topics = [Topic(p) for p in md_files]
    file_count = len(topics)

    # Effective-id / slug indexes (first occurrence wins for lookups).
    effid_to_topic = {}
    slug_to_topic = {}
    for t in topics:
        effid_to_topic.setdefault(t.effective_id, t)
        if t.slug:
            slug_to_topic.setdefault(t.slug, t)
    all_slugs = {t.slug for t in topics if t.slug}

    # Parse sidebars.ts once.
    sidebar_tree = None
    doc_refs = landing_ids = preorder = labels = None
    if sidebars_path.exists():
        try:
            sidebar_tree = parse_sidebars(sidebars_path.read_text(encoding='utf-8'))
            doc_refs, landing_ids, preorder, labels = collect_sidebar(sidebar_tree)
        except Exception as e:  # noqa: BLE001 - report parse failure as an error
            findings.error(6, f'Failed to parse sidebars.ts: {e}', file='sidebars.ts')
    landing_set = set(landing_ids or [])
    referenced_ids = set(doc_refs or []) | landing_set

    def on(num):
        return num in enabled

    # --- 1. frontmatter ----------------------------------------------------
    if on(1):
        for t in topics:
            if not t.title:
                findings.error(1, 'Missing/empty frontmatter `title`', file=t.name)
            if not t.slug:
                findings.error(1, 'Missing/empty frontmatter `slug`', file=t.name)

    # --- 2. slug-filename --------------------------------------------------
    if on(2):
        for t in topics:
            if t.name_part is None:
                continue  # filename-format error reported by check 3
            if t.slug and t.slug != t.name_part:
                findings.error(
                    2,
                    f'slug `{t.slug}` != filename name part `{t.name_part}`',
                    file=t.name)

    # --- 3. filename-format ------------------------------------------------
    if on(3):
        for t in topics:
            if not FILENAME_RE.match(t.name):
                findings.error(
                    3,
                    'Filename does not match ^\\d\\d(\\.\\d\\d)*\\.[A-Z][A-Za-z0-9]*\\.md$',
                    file=t.name)

    # --- 4. unique-ids -----------------------------------------------------
    if on(4):
        slug_map = {}
        id_map = {}
        for t in topics:
            if t.slug:
                slug_map.setdefault(t.slug, []).append(t.name)
            id_map.setdefault(t.effective_id, []).append(t.name)
        for slug, names in sorted(slug_map.items()):
            if len(names) > 1:
                findings.error(4, f'Duplicate slug `{slug}` in: {", ".join(sorted(names))}')
        for eid, names in sorted(id_map.items()):
            if len(names) > 1:
                findings.error(4, f'Duplicate effective id `{eid}` in: {", ".join(sorted(names))}')

    # --- 5. landing-00 -----------------------------------------------------
    if on(5) and sidebar_tree is not None:
        for t in topics:
            if t.numeric_prefix is None:
                continue
            ends_00 = t.numeric_prefix.split('.')[-1] == '00'
            is_landing = t.effective_id in landing_set
            if is_landing and not ends_00:
                findings.error(
                    5,
                    f'Landing page (category link target) `{t.effective_id}` '
                    f'lacks a trailing `.00` prefix segment',
                    file=t.name)
            if not is_landing and ends_00:
                findings.error(
                    5,
                    f'Leaf page carries a spurious trailing `.00` segment '
                    f'(prefix `{t.numeric_prefix}`)',
                    file=t.name)

    # --- 6. dangling -------------------------------------------------------
    if on(6) and sidebar_tree is not None:
        for rid in sorted(referenced_ids):
            if rid not in effid_to_topic:
                findings.error(6, f'Sidebar reference `topics/{rid}` resolves to no file')

    # --- 7. orphans --------------------------------------------------------
    allowlist = set()
    if allowlist_path.exists():
        for line in allowlist_path.read_text(encoding='utf-8').splitlines():
            line = line.strip()
            if line and not line.startswith('#'):
                allowlist.add(line)
    if on(7) and sidebar_tree is not None:
        for t in topics:
            if t.effective_id not in referenced_ids and t.effective_id not in allowlist:
                findings.error(
                    7,
                    f'Orphan: `{t.effective_id}` is not referenced in sidebars.ts',
                    file=t.name)

    # --- 8. counts ---------------------------------------------------------
    if on(8) and sidebar_tree is not None:
        distinct_refs = referenced_ids
        findings.note(
            8,
            f'{file_count} topic files vs {len(distinct_refs)} distinct sidebar refs')
        files_not_ref = sorted(t.effective_id for t in topics
                               if t.effective_id not in distinct_refs)
        refs_not_file = sorted(r for r in distinct_refs if r not in effid_to_topic)
        if files_not_ref:
            findings.note(8, f'Files not referenced ({len(files_not_ref)}): '
                             f'{", ".join(files_not_ref)}')
        if refs_not_file:
            findings.note(8, f'Refs without a file ({len(refs_not_file)}): '
                             f'{", ".join(refs_not_file)}')

    # --- 9. order ----------------------------------------------------------
    if on(9) and sidebar_tree is not None:
        seen = set()
        seq_files = []
        for rid in preorder:
            if rid in seen:
                continue
            seen.add(rid)
            t = effid_to_topic.get(rid)
            if t is not None:  # dangling refs handled by check 6
                seq_files.append(t.name)
        expected = sorted(seq_files)
        if seq_files != expected:
            div = None
            for idx, (got, want) in enumerate(zip(seq_files, expected)):
                if got != want:
                    div = (idx, got, want)
                    break
            if div is None:
                div = (min(len(seq_files), len(expected)), '<end>', '<end>')
            idx, got, want = div
            findings.error(
                9,
                f'Sidebar order != directory order at position {idx}: '
                f'sidebar has `{got}`, directory-sorted expects `{want}`')
            lo = max(0, idx - 2)
            hi = idx + 3
            findings.note(9, 'sidebar[{}:{}]  = {}'.format(lo, hi, seq_files[lo:hi]))
            findings.note(9, 'directory[{}:{}] = {}'.format(lo, hi, expected[lo:hi]))

    # --- 10. labels (warning) ---------------------------------------------
    if on(10) and sidebar_tree is not None:
        for eid, label in labels:
            padded = _padded_prefix_from_label(label)
            if padded is None:
                continue
            t = effid_to_topic.get(eid)
            if t is None or t.numeric_prefix is None:
                continue
            prefix = t.numeric_prefix
            if prefix != padded and prefix != padded + '.00':
                findings.warning(
                    10,
                    f'Sidebar label `{label}` -> `{padded}` does not match '
                    f'file prefix `{prefix}` ({t.name})')

    # --- 11. links ---------------------------------------------------------
    if on(11):
        md_link_re = re.compile(r'\]\(([^)\s]+)\)')
        docs_topics_re = re.compile(r'/docs/topics/([^)\s#]+)')
        for page in sorted(pages_dir.rglob('*.md')) + sorted(pages_dir.rglob('*.mdx')):
            text = page.read_text(encoding='utf-8')
            rel = page.relative_to(docs_dir).as_posix()
            in_topics = topics_dir in page.parents
            for line, m in _iter_lines_with(text, md_link_re):
                target = m.group(1)
                frag = target.split('#', 1)[0]
                if frag.startswith('/docs/topics/'):
                    slug = frag[len('/docs/topics/'):].rstrip('/')
                    if slug and slug not in all_slugs:
                        findings.error(
                            11, f'Broken topic link `/docs/topics/{slug}`',
                            file=rel, line=line)
                elif in_topics and re.fullmatch(r'[A-Za-z][A-Za-z0-9]*', frag):
                    # Bare relative link within a topic page -> resolves to a sibling slug.
                    if frag not in all_slugs:
                        findings.error(
                            11, f'Broken relative topic link `{frag}`',
                            file=rel, line=line)

    # --- 12. imports -------------------------------------------------------
    if on(12):
        import_re = re.compile(r'@site/pages/topics/([^\s\'")]+)')
        for page in sorted(pages_dir.rglob('*.md')) + sorted(pages_dir.rglob('*.mdx')):
            text = page.read_text(encoding='utf-8')
            rel = page.relative_to(docs_dir).as_posix()
            for line, m in _iter_lines_with(text, import_re):
                target = (topics_dir / m.group(1))
                if not target.exists():
                    findings.error(
                        12, f'Broken import `@site/pages/topics/{m.group(1)}`',
                        file=rel, line=line)

    # --- 13. javadoc (warning) --------------------------------------------
    if on(13):
        apidocs_re = re.compile(r'/site/apidocs/([^)\s\'">]+)')
        total = 0
        for page in sorted(pages_dir.rglob('*.md')) + sorted(pages_dir.rglob('*.mdx')):
            text = page.read_text(encoding='utf-8')
            rel = page.relative_to(docs_dir).as_posix()
            for line, m in _iter_lines_with(text, apidocs_re):
                total += 1
                path_part = m.group(1).split('#', 1)[0]
                if not path_part.endswith('.html'):
                    findings.warning(
                        13, f'Javadoc link does not point at a .html file: '
                            f'/site/apidocs/{m.group(1)}',
                        file=rel, line=line)
        findings.note(13, f'{total} /site/apidocs/ links found (format sanity-checked only)')

    # --- 14. anchors (warning; off by default) ----------------------------
    if on(14) and anchors_enabled:
        heading_anchors = {}
        for t in topics:
            anchors = set()
            for line in _strip_code(t.body).splitlines():
                hm = re.match(r'^#{1,6}\s+(.*?)\s*$', line)
                if hm:
                    anchors.add(_github_anchor(hm.group(1)))
            if t.slug:
                heading_anchors[t.slug] = anchors
        anchored_re = re.compile(r'/docs/topics/([A-Za-z0-9]+)#([^)\s"<>]+)')
        for page in sorted(pages_dir.rglob('*.md')) + sorted(pages_dir.rglob('*.mdx')):
            text = page.read_text(encoding='utf-8')
            rel = page.relative_to(docs_dir).as_posix()
            for line, m in _iter_lines_with(text, anchored_re):
                slug, frag = m.group(1), m.group(2)
                if slug in heading_anchors and frag not in heading_anchors[slug]:
                    findings.warning(
                        14, f'Anchor `#{frag}` not found on `/docs/topics/{slug}`',
                        file=rel, line=line)

    # --- 15. hygiene (warning) --------------------------------------------
    if on(15):
        title_map = {}
        for t in topics:
            if t.title:
                title_map.setdefault(t.title, []).append(t.name)
        for title, names in sorted(title_map.items()):
            if len(names) > 1:
                findings.warning(
                    15, f'Duplicate title `{title}` in: {", ".join(sorted(names))}')
        for t in topics:
            body_chars = len(re.sub(r'\s', '', t.body))
            if body_chars < 50:
                findings.warning(
                    15, f'Near-empty page ({body_chars} non-whitespace body chars)',
                    file=t.name)
        if topics_dir.exists():
            for p in sorted(topics_dir.iterdir()):
                if p.is_file() and not p.name.endswith('.md'):
                    findings.warning(15, 'Stray non-.md file in pages/topics/', file=p.name)

    return file_count


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------
def parse_check_selection(value):
    """Turn a comma-separated list of numbers/names into a set of check numbers."""
    result = set()
    for token in value.split(','):
        token = token.strip()
        if not token:
            continue
        if token.isdigit():
            n = int(token)
            if n in NAME_BY_NUM:
                result.add(n)
            else:
                raise ValueError(f'unknown check number: {token}')
        elif token in NUM_BY_NAME:
            result.add(NUM_BY_NAME[token])
        else:
            raise ValueError(f'unknown check: {token}')
    return result


def _fmt_location(item):
    if item['file'] and item['line']:
        return f"{item['file']}:{item['line']}"
    if item['file']:
        return item['file']
    return ''


def print_text_report(findings, file_count, enabled):
    errors = [i for i in findings.items if i['severity'] == 'error']
    warnings = [i for i in findings.items if i['severity'] == 'warning']
    notes = [i for i in findings.items if i['severity'] == 'note']

    def emit(items, header):
        if not items:
            return
        print(f'\n{header}')
        print('=' * len(header))
        for i in items:
            loc = _fmt_location(i)
            tag = f'[{i["num"]:>2} {i["check"]}]'
            if loc:
                print(f'  {tag} {loc}: {i["message"]}')
            else:
                print(f'  {tag} {i["message"]}')

    print('Juneau Documentation Integrity Checker')
    print('=' * 50)
    enabled_str = ', '.join(NAME_BY_NUM[n] for n in sorted(enabled))
    print(f'Checks run: {enabled_str}')

    emit(errors, f'ERRORS ({len(errors)})')
    emit(warnings, f'WARNINGS ({len(warnings)})')
    emit(notes, 'NOTES')

    print(f'\nverify-docs: {len(errors)} errors, {len(warnings)} warnings '
          f'across {file_count} files')


def print_json_report(findings, file_count, enabled):
    errors = sum(1 for i in findings.items if i['severity'] == 'error')
    warnings = sum(1 for i in findings.items if i['severity'] == 'warning')
    payload = {
        'file_count': file_count,
        'checks_run': [NAME_BY_NUM[n] for n in sorted(enabled)],
        'summary': {'errors': errors, 'warnings': warnings},
        'findings': findings.items,
    }
    print(json.dumps(payload, indent=2))


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------
def main(argv=None):
    parser = argparse.ArgumentParser(
        description='Documentation-integrity checker for the Juneau Docusaurus site.')
    parser.add_argument('--strict', action='store_true',
                        help='Treat warnings as errors for the exit code.')
    parser.add_argument('--anchors', action='store_true',
                        help='Enable anchor-target resolution (check 14).')
    parser.add_argument('--json', action='store_true',
                        help='Emit machine-readable JSON instead of text.')
    parser.add_argument('--only', default=None,
                        help='Run only these checks (comma-separated numbers or names).')
    parser.add_argument('--skip', default=None,
                        help='Skip these checks (comma-separated numbers or names).')
    parser.add_argument('--docs-dir', default=None,
                        help='Docs root (defaults to the parent of scripts/).')
    args = parser.parse_args(argv)

    docs_dir = Path(args.docs_dir).resolve() if args.docs_dir \
        else Path(__file__).parent.parent.resolve()

    topics_dir = docs_dir / 'pages' / 'topics'
    if not topics_dir.is_dir():
        print(f'ERROR: topics directory not found: {topics_dir}', file=sys.stderr)
        return 2

    enabled = set(NAME_BY_NUM)
    try:
        if args.only:
            enabled = parse_check_selection(args.only)
        if args.skip:
            enabled -= parse_check_selection(args.skip)
    except ValueError as e:
        print(f'ERROR: {e}', file=sys.stderr)
        return 2

    findings = Findings()
    file_count = run_checks(docs_dir, enabled, args.anchors, findings)

    if args.json:
        print_json_report(findings, file_count, enabled)
    else:
        print_text_report(findings, file_count, enabled)

    errors, warnings = findings.counts(args.strict)
    if errors > 0:
        return 1
    if args.strict and warnings > 0:
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
