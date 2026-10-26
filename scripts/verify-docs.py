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
Check the integrity of the pages/topics/*.md files, their frontmatter and the rest of the docs site.

Full docs: verify-docs.md
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
    (11, 'links'),
    (12, 'imports'),
    (13, 'javadoc'),
    (14, 'anchors'),
    (15, 'hygiene'),
    (16, 'guides'),
    (17, 'lockfile'),
]
NAME_BY_NUM = {n: name for n, name in CHECKS}
NUM_BY_NAME = {name: n for n, name in CHECKS}
WARNING_CHECKS = {13, 14, 15}
# sidebars.ts helpers that return their one argument unchanged in shape; the parser reads straight through them.
TRANSPARENT_CALLS = {'numbered', 'group'}

FILENAME_RE = re.compile(r'^(\d\d(?:\.\d\d)*)\.([A-Z][A-Za-z0-9]*)\.md$')
# Leading numeric prefix + trailing '.md' stripped => PascalCase name part.
NAME_PART_RE = re.compile(r'^\d\d(?:\.\d\d)*\.([A-Z][A-Za-z0-9]*)\.md$')
# The Guides section: its landing page is the category link target and is served at /docs/guides.
GUIDES_LINK_ID = 'guides/00.Guides'
GUIDES_LANDING_SLUG = '/guides'
# Every `resolved` URL in package-lock.json must be on the public npm registry.
LOCKFILE_RESOLVED_RE = re.compile(r'"resolved":\s*"([^"]*)"')
PUBLIC_NPM_REGISTRY = 'https://registry.npmjs.org/'
# A numbered landing row that links a topic (`8. [Title](/docs/topics/Slug)...`): a guide kept as a sidebar `ref`.
LANDING_TOPIC_ROW_RE = re.compile(r'^\d+\. \[[^\]]*\]\(/docs/topics/([A-Za-z][A-Za-z0-9]*)\)', re.M)


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
        while self.i < self.n and self.s[self.i] not in ',}]( \t\r\n':
            self.i += 1
        word = self.s[start:self.i]
        if self.i < self.n and self.s[self.i] == '(':
            if word not in TRANSPARENT_CALLS:
                raise ValueError(f'unsupported call `{word}(` at offset {start}')
            self.i += 1  # consume '('
            value = self.parse_value()
            self._ws()
            if self.i >= self.n or self.s[self.i] != ')':
                raise ValueError(f'expected `)` closing `{word}(` at offset {self.i}')
            self.i += 1
            return value
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
def _prefixed_id(raw, prefix):
    """Return the effective id for a raw sidebar id under `prefix` (e.g. 'guides/'), or None if it is not one."""
    if isinstance(raw, str) and raw.startswith(prefix):
        return raw[len(prefix):]
    return None


def _topics_id(raw):
    """Return the effective id for a raw sidebar id, or None if not a topics ref."""
    return _prefixed_id(raw, 'topics/')


def collect_sidebar(nodes):
    """Walk the sidebar tree, returning:

    - doc_refs:    effective ids referenced by type:'doc' entries (with multiplicity).
    - landing_ids: effective ids that are a category link.id (LANDING pages).
    - preorder:    depth-first pre-order sequence of topic ids (category link first,
                   then its items).
    """
    doc_refs = []
    landing_ids = []
    preorder = []

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
        elif ntype == 'category':
            link = node.get('link')
            link_id = None
            if isinstance(link, dict):
                link_id = _topics_id(link.get('id'))
            if link_id is not None:
                landing_ids.append(link_id)
                preorder.append(link_id)
            for item in node.get('items', []):
                walk(item)

    walk(nodes)
    return doc_refs, landing_ids, preorder


def collect_section(nodes, link_id):
    """Find every category whose link.id is `link_id`.

    Returns one list per such category: its `doc` and `ref` items as (type, raw id) pairs, depth-first, nested
    categories included.  Separate from collect_sidebar so that checks 5-9 stay topics-only.
    """
    sections = []

    def walk(node, out):
        if isinstance(node, list):
            for item in node:
                walk(item, out)
            return
        if not isinstance(node, dict):
            return
        ntype = node.get('type')
        if out is not None and ntype in ('doc', 'ref'):
            out.append((ntype, node.get('id')))
        elif ntype == 'category':
            link = node.get('link')
            if isinstance(link, dict) and link.get('id') == link_id:
                out = []
                sections.append(out)
            for item in node.get('items', []):
                walk(item, out)

    walk(nodes, None)
    return sections


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


# ---------------------------------------------------------------------------
# Body-scan helpers (strip code so links inside code blocks are ignored)
# ---------------------------------------------------------------------------
def _strip_code(text):
    text = re.sub(r'```.*?```', '', text, flags=re.DOTALL)
    text = re.sub(r'`[^`]*`', '', text)
    return text


def _strip_fenced_code(text):
    """Remove fenced code blocks only, preserving inline-code (backtick) spans.

    Used ahead of heading extraction: Docusaurus/GitHub slugify inline code in a
    heading by keeping its text and dropping just the backticks (see
    _github_anchor), so headings must NOT go through _strip_code first — that
    would delete the inline-code *content*, not just the backtick markers.
    """
    return re.sub(r'```.*?```', '', text, flags=re.DOTALL)


def _iter_lines_with(text, pattern):
    """Yield (lineno, match) for a compiled pattern over the code-stripped text."""
    stripped = _strip_code(text)
    for m in pattern.finditer(stripped):
        line = stripped.count('\n', 0, m.start()) + 1
        yield line, m


def _github_anchor(heading):
    """Approximate Docusaurus/GitHub heading-anchor slugification.

    Docusaurus's slugger (github-slugger) does NOT collapse consecutive hyphens
    and does NOT strip leading/trailing hyphens produced when punctuation is
    removed from the start/middle/end of a heading — e.g. "A / B" -> "a--b" and
    "`#{...}` Foo" -> "-foo" are both real, valid anchors. Do not "clean up"
    the result any further than this.
    """
    h = heading.strip().lower()
    h = re.sub(r'`', '', h)
    h = re.sub(r'[^a-z0-9 \-_]', '', h)
    h = h.replace(' ', '-')
    return h


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------
def run_checks(docs_dir, enabled, anchors_enabled, findings):
    topics_dir = docs_dir / 'pages' / 'topics'
    guides_dir = docs_dir / 'pages' / 'guides'
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

    # Guide pages (pages/guides/*.md): checked by 16, link targets for 11.  Same file model as topics.
    guides = [Topic(p) for p in sorted(guides_dir.glob('*.md'), key=lambda p: p.name)]
    guide_slugs = {g.slug for g in guides if g.slug and not g.slug.startswith('/')}

    # Parse sidebars.ts once.
    sidebar_tree = None
    doc_refs = landing_ids = preorder = None
    if sidebars_path.exists():
        try:
            sidebar_tree = parse_sidebars(sidebars_path.read_text(encoding='utf-8'))
            doc_refs, landing_ids, preorder = collect_sidebar(sidebar_tree)
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

    # --- 11. links ---------------------------------------------------------
    if on(11):
        md_link_re = re.compile(r'\]\(([^)\s]+)\)')
        docs_topics_re = re.compile(r'/docs/topics/([^)\s#]+)')
        for page in sorted(pages_dir.rglob('*.md')) + sorted(pages_dir.rglob('*.mdx')):
            text = page.read_text(encoding='utf-8')
            rel = page.relative_to(docs_dir).as_posix()
            in_topics = topics_dir in page.parents
            in_guides = guides_dir in page.parents
            for line, m in _iter_lines_with(text, md_link_re):
                target = m.group(1)
                frag = target.split('#', 1)[0]
                if frag.startswith('/docs/topics/'):
                    slug = frag[len('/docs/topics/'):].rstrip('/')
                    if slug and slug not in all_slugs:
                        findings.error(
                            11, f'Broken topic link `/docs/topics/{slug}`',
                            file=rel, line=line)
                elif frag.startswith('/docs/guides/'):
                    # `/docs/guides` itself (the landing) is valid with or without the trailing slash.
                    slug = frag[len('/docs/guides/'):].rstrip('/')
                    if slug and slug not in guide_slugs:
                        findings.error(
                            11, f'Broken guide link `/docs/guides/{slug}`',
                            file=rel, line=line)
                elif (in_topics or in_guides) and re.fullmatch(r'[A-Za-z][A-Za-z0-9]*', frag):
                    # Bare relative link within a topic or guide page: resolves under the trailing-slash path, so
                    # it must be written as an absolute /docs/topics/<slug> or /docs/guides/<slug> link.
                    if frag in all_slugs:
                        findings.error(
                            11, f'Relative topic link `{frag}`; use `/docs/topics/{frag}`',
                            file=rel, line=line)
                    elif frag in guide_slugs:
                        findings.error(
                            11, f'Relative guide link `{frag}`; use `/docs/guides/{frag}`',
                            file=rel, line=line)
                    else:
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
        explicit_id_re = re.compile(r'\{#([^}]+)\}\s*$')
        for t in topics:
            anchors = set()
            for line in _strip_fenced_code(t.body).splitlines():
                hm = re.match(r'^#{1,6}\s+(.*?)\s*$', line)
                if hm:
                    heading_text = hm.group(1)
                    explicit = explicit_id_re.search(heading_text)
                    if explicit:
                        # Docusaurus honors an explicit `{#custom-id}` heading
                        # suffix verbatim instead of auto-slugifying the text.
                        anchors.add(explicit.group(1))
                    else:
                        anchors.add(_github_anchor(heading_text))
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

    # --- 16. guides --------------------------------------------------------
    if on(16) and guides_dir.is_dir():
        _check_guides(guides, sidebar_tree, effid_to_topic, findings)

    # --- 17. lockfile ------------------------------------------------------
    if on(17):
        _check_lockfile(docs_dir / 'package-lock.json', findings)

    return file_count


def _check_lockfile(lockfile, findings):
    """Check 17: every `resolved` URL in package-lock.json is on the public npm registry (a mirror leaks in when npm runs
    against a local registry setting).  Skipped when there is no lockfile."""
    if not lockfile.is_file():
        return
    for line, text in enumerate(lockfile.read_text(encoding='utf-8').splitlines(), 1):
        m = LOCKFILE_RESOLVED_RE.search(text)
        if not m:
            continue
        url = m.group(1)
        if not url.startswith(PUBLIC_NPM_REGISTRY):
            findings.error(17, f'`resolved` URL is not on the public npm registry: {url}',
                           file=lockfile.name, line=line)


def _check_guides(guides, sidebar_tree, effid_to_topic, findings):
    """Check 16: guide frontmatter and file names, and the sidebar's Guides category."""
    landing = _prefixed_id(GUIDES_LINK_ID, 'guides/')
    for g in guides:
        rel = f'guides/{g.name}'
        for key in ('id', 'title', 'slug'):
            if not g.frontmatter.get(key):
                findings.error(16, f'Missing/empty frontmatter `{key}`', file=rel)
        if g.name_part is None:
            findings.error(16, 'File name does not match `NN.Name.md`', file=rel)
            continue
        stem = g.name[:-3]
        if g.id and g.id != stem:
            findings.error(16, f'id `{g.id}` != file stem `{stem}`', file=rel)
        if stem == landing:
            if g.slug and g.slug != GUIDES_LANDING_SLUG:
                findings.error(16, f'Landing slug `{g.slug}` != `{GUIDES_LANDING_SLUG}`', file=rel)
        elif g.slug and g.slug != g.name_part:
            findings.error(16, f'slug `{g.slug}` != filename name part `{g.name_part}`', file=rel)

    guide_ids = {g.effective_id for g in guides}
    if landing not in guide_ids:
        findings.error(16, f'Missing landing page pages/{GUIDES_LINK_ID}.md')
    if sidebar_tree is None:
        return
    sections = collect_section(sidebar_tree, GUIDES_LINK_ID)
    if len(sections) != 1:
        findings.error(16, f'Expected one sidebar category linking `{GUIDES_LINK_ID}`, found {len(sections)}',
                       file='sidebars.ts')
        return
    listed, ref_slugs = [], []
    for kind, raw in sections[0]:
        if kind == 'doc':
            gid = _prefixed_id(raw, 'guides/')
            if gid is None:
                continue
            if ref_slugs:
                findings.error(16, f'Sidebar doc `{raw}` follows a ref; the Guides category lists its refs last',
                               file='sidebars.ts')
            if gid in guide_ids:
                listed.append(gid)
            else:
                findings.error(16, f'Sidebar doc `{raw}` has no file in pages/guides/', file='sidebars.ts')
        else:
            topic = effid_to_topic.get(_topics_id(raw))
            if topic is None:
                findings.error(16, f'Sidebar ref `{raw}` in the Guides category does not resolve to a topic',
                               file='sidebars.ts')
            ref_slugs.append(topic.slug if topic else None)
    for g in guides:
        if g.effective_id == landing:
            continue
        n = listed.count(g.effective_id)
        if n == 0:
            findings.error(16, 'Guide is not listed in the sidebar Guides category', file=f'guides/{g.name}')
        elif n > 1:
            findings.error(16, f'Guide is listed {n} times in the sidebar Guides category', file=f'guides/{g.name}')
    if listed != sorted(listed):
        findings.error(16, f'Guides category order != file order: {listed}', file='sidebars.ts')
    # Every topic the landing lists as a guide must stay in the category as a ref, so that a sidebar
    # regeneration cannot drop it silently.
    page = next((g for g in guides if g.effective_id == landing), None)
    for slug in LANDING_TOPIC_ROW_RE.findall(page.body if page else ''):
        if slug not in ref_slugs:
            findings.error(16, f'Landing lists `/docs/topics/{slug}` but the Guides category has no ref to it',
                           file='sidebars.ts')


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
