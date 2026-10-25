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
Renumber pages/topics files gap-free in mainSidebar order: rename the files, rewrite front-matter ids, patch the
sidebars.ts ids and labels in place, and rewrite references to the old ids and file names.  Dry run by default.

Full docs: renumber-topics.md
"""

import argparse
import importlib.util
import json
import os
import re
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
CHAPTER_RE = re.compile(r'^\d\d$')
TOPIC_ID_RE = re.compile(r"""(['"])topics/([^'"]+)\1""")
LABEL_RE = re.compile(r"""(\blabel:\s*)('(?:[^'\\\n]|\\.)*'|"(?:[^"\\\n]|\\.)*")""")
LABEL_NUMBER_RE = re.compile(r'^\s*\d+(?:\.\d+)*\.\s+')
TMP_SUFFIX = '.renumtmp'


def _load_verify_docs():
    spec = importlib.util.spec_from_file_location('verify_docs_for_renumber', SCRIPT_DIR / 'verify-docs.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


vd = _load_verify_docs()


class RenumberError(Exception):
    """The sidebar and the topic files disagree in a way the renumber cannot resolve."""


# ---------------------------------------------------------------------------
# Planning
# ---------------------------------------------------------------------------
def _node_id(node):
    """The topics effective id a sidebar node numbers (a doc, or a category's doc link), else None."""
    if node.get('type') == 'doc':
        return vd._topics_id(node.get('id'))
    if node.get('type') == 'category':
        link = node.get('link')
        if isinstance(link, dict) and link.get('type') == 'doc':
            return vd._topics_id(link.get('id'))
    return None


def _js_string(literal):
    """Decode a quoted JS string literal as written in sidebars.ts."""
    return re.sub(r'\\(.)', r'\1', literal[1:-1])


def _js_quote(value, quote):
    return quote + value.replace('\\', '\\\\').replace(quote, '\\' + quote) + quote


def main_sidebar_span(text):
    """(start, end) offsets of the mainSidebar array in sidebars.ts (the array ends at the first tab-indented ']')."""
    m = re.search(r'\bmainSidebar\s*:\s*\[', text)
    if not m:
        raise RenumberError('mainSidebar not found in sidebars.ts')
    end = text.find('\n\t]', m.end())
    if end < 0:
        raise RenumberError('end of mainSidebar not found in sidebars.ts')
    return m.start(), end


def plan(docs_dir, chapter=None):
    """Return the renumber plan: renames, relabels, unmapped files and the mapped count."""
    topics_dir = docs_dir / 'pages' / 'topics'
    topics = [vd.Topic(p) for p in sorted(topics_dir.glob('*.md'))]
    by_id = {}
    for t in topics:
        if t.effective_id in by_id:
            raise RenumberError(f'duplicate effective id {t.effective_id}: {by_id[t.effective_id].name}, {t.name}')
        by_id[t.effective_id] = t
    tree = vd.parse_sidebars((docs_dir / 'sidebars.ts').read_text(encoding='utf-8'))

    mapping = {}   # old effective id -> (topic, new file name, new effective id)
    relabels = []  # (chapter, old label, new label)

    def topic(i):
        if i not in by_id:
            raise RenumberError(f'sidebar id topics/{i} has no page in pages/topics')
        t = by_id[i]
        if t.numeric_prefix is None:
            raise RenumberError(f'{t.name} has no numeric prefix')
        return t

    def assign(t, prefix, is_landing):
        npref = '.'.join('%02d' % x for x in prefix)
        new_file = npref + ('.00' if is_landing else '') + '.' + t.name_part + '.md'
        new_id = npref + '.' + re.sub(r'^(\d\d\.)+', '', t.id) if t.id else new_file[:-3]
        mapping[t.effective_id] = (t, new_file, new_id)

    def walk(items, prefix, counter):
        for n in items:
            i = _node_id(n)
            if i is None:
                if n.get('type') == 'category':
                    # A category with no doc link (a group) does not consume a number; its items continue the count.
                    counter = walk(n.get('items', []), prefix, counter)
                continue
            counter += 1
            number(n, i, prefix + [counter])
        return counter

    def number(n, i, p):
        t = topic(i)
        is_landing = n['type'] == 'category'
        assign(t, p, is_landing)
        label = n.get('label')
        if label is not None:
            padded = vd._padded_prefix_from_label(label)
            if padded is not None and (padded == t.numeric_prefix or padded + '.00' == t.numeric_prefix):
                new_label = '.'.join(str(x) for x in p) + '. ' + LABEL_NUMBER_RE.sub('', label)
                if new_label != label:
                    relabels.append((t.numeric_prefix[:2], label, new_label))
        if is_landing:
            walk(n.get('items', []), p, 0)

    def chapters(items):
        for n in items:
            i = _node_id(n)
            if i is None:
                if n.get('type') == 'category':
                    chapters(n.get('items', []))
                continue
            t = topic(i)
            segs = t.numeric_prefix.split('.')
            if len(segs) > 1 and segs[-1] == '00':
                segs = segs[:-1]
            if len(segs) != 1:
                raise RenumberError(f'{t.name} sits at chapter level but has prefix {t.numeric_prefix}')
            mapping[i] = (t, t.name, t.effective_id)  # a chapter keeps its number, file and id
            if n['type'] == 'category':
                walk(n.get('items', []), [int(segs[0])], 0)

    chapters(tree)

    new_files = [m[1] for m in mapping.values()]
    if len(new_files) != len(set(new_files)):
        raise RenumberError('renumber would give two pages the same file name')
    new_ids = [m[2] for m in mapping.values()]
    if len(new_ids) != len(set(new_ids)):
        raise RenumberError('renumber would give two pages the same id')

    def in_scope(t):
        return chapter is None or t.numeric_prefix[:2] == chapter

    renames = [{'old_file': t.name, 'new_file': new_file, 'old_id': i, 'new_id': new_id}
               for i, (t, new_file, new_id) in mapping.items()
               if in_scope(t) and (new_file != t.name or new_id != i)]
    renames.sort(key=lambda r: r['old_file'])
    unmapped = sorted(t.name for t in topics if t.effective_id not in mapping)
    for r in renames:
        if r['new_file'] in unmapped:
            raise RenumberError(f"{r['old_file']} would overwrite {r['new_file']}, which is not in mainSidebar")
    return {
        'mapped': len(mapping),
        'total': len(topics),
        'renames': renames,
        'relabels': [{'old': old, 'new': new} for ch, old, new in relabels if chapter is None or ch == chapter],
        'unmapped': unmapped,
    }


# ---------------------------------------------------------------------------
# Applying
# ---------------------------------------------------------------------------
def patch_sidebars(text, renames, relabels):
    """Rewrite 'topics/<old id>' strings anywhere and the relabelled labels inside mainSidebar, in one pass each."""
    id_map = {r['old_id']: r['new_id'] for r in renames if r['old_id'] != r['new_id']}
    text = TOPIC_ID_RE.sub(lambda m: m.group(1) + 'topics/' + id_map.get(m.group(2), m.group(2)) + m.group(1), text)
    if not relabels:
        return text
    label_map = {r['old']: r['new'] for r in relabels}
    start, end = main_sidebar_span(text)
    span = text[start:end]
    seen = {}
    for m in LABEL_RE.finditer(span):
        value = _js_string(m.group(2))
        if value in label_map:
            seen[value] = seen.get(value, 0) + 1
    for old in label_map:
        if seen.get(old, 0) != 1:
            raise RenumberError(f'label {old!r} occurs {seen.get(old, 0)} times in mainSidebar; expected exactly 1')

    def relabel(m):
        value = _js_string(m.group(2))
        if value not in label_map:
            return m.group(0)
        return m.group(1) + _js_quote(label_map[value], m.group(2)[0])

    return text[:start] + LABEL_RE.sub(relabel, span) + text[end:]


def _token_regex(tokens):
    alternatives = '|'.join(re.escape(k) for k in sorted(tokens, key=len, reverse=True))
    return re.compile(r'(?<![\w.\-])(' + alternatives + r')(?![\w\-])')


def rewrite_references(text, tokens, token_rx):
    """Rewrite @site/pages/topics/<file>.md imports and bare old ids and file stems."""
    text = re.sub(r'@site/pages/topics/(\S+?)\.md\b',
                  lambda m: '@site/pages/topics/' + tokens.get(m.group(1), m.group(1)) + '.md', text)
    return token_rx.sub(lambda m: tokens[m.group(1)], text)


def apply(docs_dir, result):
    """Rename the files and rewrite ids, sidebar entries and references.  Everything is computed before any write."""
    renames = result['renames']
    if not renames and not result['relabels']:
        return
    topics_dir = docs_dir / 'pages' / 'topics'
    tokens = {}
    for r in renames:
        if r['old_id'] != r['new_id']:
            tokens[r['old_id']] = r['new_id']
        if r['old_file'] != r['new_file']:
            tokens[r['old_file'][:-3]] = r['new_file'][:-3]
    token_rx = _token_regex(tokens) if tokens else None

    # 1. New text for every file that changes, keyed by its final path.
    writes = {}
    renamed_from = {r['old_file']: r for r in renames}
    for path in sorted(topics_dir.glob('*.md')):
        text = path.read_text(encoding='utf-8')
        new_text = text
        if token_rx:
            # One simultaneous pass, so a shift (A -> B, B -> C) never chains.
            new_text = rewrite_references(text, tokens, token_rx)
        r = renamed_from.get(path.name)
        if r is not None:
            t = vd.Topic(path)
            if t.id and not re.search(r'(?m)^id:\s*' + re.escape(r['new_id']) + r'\s*$', new_text):
                raise RenumberError(f'could not rewrite the id line of {path.name}')
        target = topics_dir / (r['new_file'] if r else path.name)
        if new_text != text or target != path:
            writes[target] = (path, new_text)
    if token_rx:
        others = [p for p in (docs_dir / 'pages').rglob('*.md*')
                  if p.suffix in ('.md', '.mdx') and p.parent != topics_dir
                  and 'release-notes' not in p.parts and 'developer-info' not in p.parts]
        allowlist = docs_dir / 'scripts' / 'verify-docs-allowlist.txt'
        if allowlist.is_file():
            others.append(allowlist)
        for path in sorted(others):
            text = path.read_text(encoding='utf-8')
            new_text = rewrite_references(text, tokens, token_rx)
            if new_text != text:
                writes[path] = (path, new_text)
    sidebars = docs_dir / 'sidebars.ts'
    sidebar_text = sidebars.read_text(encoding='utf-8')
    new_sidebar = patch_sidebars(sidebar_text, renames, result['relabels'])

    # 2. Two-phase rename, then write the new contents.
    moved = [(topics_dir / r['old_file'], topics_dir / r['new_file']) for r in renames if r['old_file'] != r['new_file']]
    for old, _new in moved:
        os.rename(old, str(old) + TMP_SUFFIX)
    for old, new in moved:
        os.rename(str(old) + TMP_SUFFIX, new)
    for target, (_source, new_text) in writes.items():
        target.write_text(new_text, encoding='utf-8')
    if new_sidebar != sidebar_text:
        sidebars.write_text(new_sidebar, encoding='utf-8')


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------
def print_text(result, applied):
    for r in result['renames']:
        if r['old_file'] != r['new_file']:
            print(f"RENAME {r['old_file']} -> {r['new_file']}")
        if r['old_id'] != r['new_id']:
            print(f"ID     {r['old_id']} -> {r['new_id']}")
    for r in result['relabels']:
        print(f"LABEL  {r['old']!r} -> {r['new']!r}")
    for name in result['unmapped']:
        print(f"UNMAPPED {name} (not in mainSidebar; left alone)")
    print(f"mapped {result['mapped']} of {result['total']} topic pages; {len(result['renames'])} renamed, "
          f"{len(result['relabels'])} relabelled, {len(result['unmapped'])} unmapped"
          + ('; applied' if applied else '; dry run, nothing written'))


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Renumber pages/topics gap-free in mainSidebar order.')
    parser.add_argument('--apply', action='store_true', help='Write the changes (default: dry run).')
    parser.add_argument('--json', action='store_true', help='Print the plan as JSON.')
    parser.add_argument('--chapter', default=None, help='Only renumber one two-digit chapter, e.g. 03.')
    parser.add_argument('--docs-dir', default=None, help='Docs root (defaults to the parent of scripts/).')
    args = parser.parse_args(argv)
    if args.chapter is not None and not CHAPTER_RE.match(args.chapter):
        parser.error('--chapter takes a two-digit chapter prefix, e.g. 03')
    return args


def main(argv=None):
    args = parse_args(argv)
    docs_dir = Path(args.docs_dir).resolve() if args.docs_dir else SCRIPT_DIR.parent
    for required in (docs_dir / 'pages' / 'topics', docs_dir / 'sidebars.ts'):
        if not required.exists():
            print(f'ERROR: not found: {required}', file=sys.stderr)
            return 2
    try:
        result = plan(docs_dir, args.chapter)
        if args.apply:
            apply(docs_dir, result)
    except RenumberError as e:
        print(f'ERROR: {e}', file=sys.stderr)
        return 1
    if args.json:
        print(json.dumps(dict(result, applied=args.apply), indent=2))
    else:
        print_text(result, args.apply)
    return 0


if __name__ == "__main__":
    sys.exit(main())
