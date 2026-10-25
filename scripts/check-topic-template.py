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
Report topic pages that drift from the reference-topic template: short pages, pages with no code fence, and pages
whose template headings are out of order.  Advisory only: exits 0 whatever it finds.

Full docs: check-topic-template.md
"""

import argparse
import json
import re
import statistics
import sys
from pathlib import Path

from code_fences import FENCE_RE, iter_fences

FRONTMATTER_RE = re.compile(r'^---\s*\n.*?\n---\s*\n?', re.DOTALL)
# The heading text, without a trailing {#custom-id}.
H2_RE = re.compile(r'^##\s+(.+?)\s*(?:\{#[^}]*\})?\s*$')
# A trailing parenthetical qualifies a template heading without changing its part: "When to use it (vs. X)".
QUALIFIER_RE = re.compile(r'\s*\([^()]*\)$')
CHAPTER_RE = re.compile(r'^\d\d$')

# The template parts that have a heading, in template order (part 1, the lead, has none).
TEMPLATE_HEADINGS = [
    ('When to use it', re.compile(r'^when to use( it)?$', re.IGNORECASE)),
    ('Example', re.compile(r'^(examples?|a complete example)$', re.IGNORECASE)),
    ('Options', re.compile(r'^(options|configuration|options and configuration)$', re.IGNORECASE)),
    ('See also', re.compile(r'^see also$', re.IGNORECASE)),
]


def strip_frontmatter(text):
    m = FRONTMATTER_RE.match(text)
    return text[m.end():] if m else text


def _closes(line, opener):
    """Whether line closes the fence opened by opener (the iter_fences closer rule)."""
    o, c = FENCE_RE.match(opener), FENCE_RE.match(line)
    return bool(c) and c.group(2)[0] == o.group(2)[0] and len(c.group(2)) >= len(o.group(2)) and not c.group(3)


def fenced_line_indexes(lines):
    """Return (set of 0-based line indexes covered by fences, fence count).  A fence covers its opener, body and closer."""
    covered = set()
    count = 0
    for start, _info, body in iter_fences('\n'.join(lines)):
        count += 1
        opener = start - 1
        # iter_fences reports '' both for an empty fence and for a fence holding one blank line.
        empty = opener + 1 < len(lines) and _closes(lines[opener + 1], lines[opener])
        n_body = 0 if empty else len(body.split('\n'))
        covered.update(range(opener, min(opener + n_body + 2, len(lines))))
    return covered, count


def measure(text):
    """Return a dict with words, fences and the h2 headings of one page (front matter and fences excluded)."""
    lines = strip_frontmatter(text).split('\n')
    covered, fences = fenced_line_indexes(lines)
    prose = [line for i, line in enumerate(lines) if i not in covered]
    h2 = [m.group(1) for m in (H2_RE.match(line) for line in prose) if m]
    return {
        'words': sum(len(line.split()) for line in prose),
        'fences': fences,
        'h2': h2,
    }


def template_order(h2):
    """Return the template indexes of the recognized headings, in page order."""
    order = []
    for heading in h2:
        for index, (_name, pattern) in enumerate(TEMPLATE_HEADINGS):
            if pattern.match(QUALIFIER_RE.sub('', heading)):
                order.append(index)
                break
    return order


def scan(docs_dir, min_words, chapter=None):
    """Return (pages, totals).  pages is a list of per-page dicts in file order."""
    pages = []
    for path in sorted((docs_dir / 'pages' / 'topics').glob('*.md')):
        if chapter and not path.name.startswith(chapter + '.'):
            continue
        m = measure(path.read_text(encoding='utf-8'))
        order = template_order(m['h2'])
        pages.append({
            'file': path.name,
            'chapter': path.name[:2],
            'words': m['words'],
            'fences': m['fences'],
            'h2_count': len(m['h2']),
            'template_headings': [TEMPLATE_HEADINGS[i][0] for i in order],
            'out_of_order': order != sorted(order),
        })
    return pages, totals(pages, min_words)


def totals(pages, min_words):
    words = [p['words'] for p in pages]
    stubs = {}
    for p in pages:
        if p['words'] < min_words:
            stubs[p['chapter']] = stubs.get(p['chapter'], 0) + 1
    no_h2 = [p for p in pages if p['h2_count'] == 0]
    return {
        'pages': len(pages),
        'median_words': statistics.median(words) if words else 0,
        'min_words': min_words,
        'under_min': sum(1 for w in words if w < min_words),
        'under_100': sum(1 for w in words if w < 100),
        'no_fence': sum(1 for p in pages if p['fences'] == 0),
        'under_min_and_no_fence': sum(1 for p in pages if p['words'] < min_words and p['fences'] == 0),
        'no_h2': len(no_h2),
        'no_h2_300': sum(1 for p in no_h2 if p['words'] >= 300),
        'no_h2_500': sum(1 for p in no_h2 if p['words'] >= 500),
        'with_template_heading': sum(1 for p in pages if p['template_headings']),
        'out_of_order': sum(1 for p in pages if p['out_of_order']),
        'stubs_by_chapter': dict(sorted(stubs.items())),
    }


def print_text(pages, t):
    def section(title, rows):
        print(f"{title} ({len(rows)}):")
        for p in rows:
            print(f"  {p['words']:5d}  {p['file']}")
        print()

    section(f"Under {t['min_words']} words", [p for p in pages if p['words'] < t['min_words']])
    section('No code fence', [p for p in pages if p['fences'] == 0])
    print(f"Template headings out of order ({t['out_of_order']}):")
    for p in pages:
        if p['out_of_order']:
            print(f"  {p['words']:5d}  {p['file']}: {', '.join(p['template_headings'])}")
    print()
    stubs = ', '.join(f"{ch}: {n}" for ch, n in t['stubs_by_chapter'].items()) or 'none'
    print(f"Totals: {t['pages']} pages, median {t['median_words']} words; {t['under_min']} under {t['min_words']} "
          f"({t['under_100']} under 100); {t['no_fence']} with no fence ({t['under_min_and_no_fence']} also under "
          f"{t['min_words']}); {t['no_h2']} with no ## ({t['no_h2_300']} of 300+ words, {t['no_h2_500']} of 500+); "
          f"{t['with_template_heading']} with a template heading, {t['out_of_order']} out of order; "
          f"under {t['min_words']} by chapter: {stubs}")


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Report topic pages that drift from the reference-topic template.')
    parser.add_argument('--docs-dir', default=None, help='Docs root (defaults to the parent of scripts/).')
    parser.add_argument('--min-words', type=int, default=150, help='Report pages under this many words (default 150).')
    parser.add_argument('--json', action='store_true', help='Emit machine-readable JSON instead of text.')
    parser.add_argument('--chapter', default=None, help='Restrict to one two-digit chapter prefix, e.g. 03.')
    args = parser.parse_args(argv)
    if args.chapter is not None and not CHAPTER_RE.match(args.chapter):
        parser.error('--chapter takes a two-digit chapter prefix, e.g. 03')
    return args


def main(argv=None):
    args = parse_args(argv)
    docs_dir = Path(args.docs_dir).resolve() if args.docs_dir else Path(__file__).resolve().parent.parent
    if not (docs_dir / 'pages' / 'topics').is_dir():
        print(f"ERROR: topics directory not found: {docs_dir / 'pages' / 'topics'}", file=sys.stderr)
        return 2
    pages, t = scan(docs_dir, args.min_words, args.chapter)
    if args.json:
        print(json.dumps({'totals': t, 'pages': pages}, indent=2))
    else:
        print_text(pages, t)
    return 0


if __name__ == "__main__":
    sys.exit(main())
