#!/usr/bin/env python3
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
TODO-288: Normalize generic type-argument comma spacing.

Rewrites `Foo<A, B>` -> `Foo<A,B>` (at every nesting level, e.g.
`Map<K, List<V>>` -> `Map<K,List<V>>`) everywhere a "generic-shaped"
`<...>` span can be found, INCLUDING inside Javadoc/comments
(`{@link Foo<A, B>}`, `{@code Map<String, Object>}`, and prose like
"returns a `Map<String, Object>`"), per the maintainer decision that
Javadoc/comments are in scope for the one-shot sweep (the Eclipse
formatter can't rewrite comment text, so this script is the mechanism
for that half of the job).

This is intentionally a lexer-style, not a full-AST, transform:
JavaParser (or any real AST) will happily rewrite `ParameterizedType`
argument lists in *code*, but has no mandate -- and no printer support
-- for rewriting free-text *inside* Javadoc/comments. Since decision #2
requires touching comment text too, an AST-only approach can't satisfy
the spec by itself, and bolting a second comment-text pass onto an
AST tool doesn't save any of the hard work below. A single state-aware
scanner that treats "code" and "comment" text uniformly once real
strings/chars/text-blocks have been carved out is simpler and easier to
reason about end-to-end.

Algorithm, in two layers:

1. Segmentation pass (single left-to-right scan of the whole file):
   classify every character into one of CODE / LINE_COMMENT /
   BLOCK_COMMENT (this includes Javadoc, since `/**` is just a block
   comment) / STRING / CHAR / TEXT_BLOCK, honoring `\\` escapes inside
   string/char literals and triple-quote text-block delimiters. This is
   the part that guarantees we never touch a comma inside a string
   literal or text block (rule #3 in the task boundaries).

2. Generic-span pass (applied independently to each CODE and
   LINE_COMMENT/BLOCK_COMMENT segment; STRING/CHAR/TEXT_BLOCK segments
   are copied through verbatim): scan for `<`, then tentatively walk
   forward maintaining an angle-bracket nesting depth, accepting only
   characters that can legally appear inside a Java type-argument list
   (identifier characters, `.`, `?`, `[`, `]`, whitespace, `,`, nested
   `<`/`>`, and a lone `&` for intersection bounds -- `&&` bails). The
   walk fails closed: the first character outside that set (`(`, `)`,
   `;`, `=`, `"`, `&&`, etc.) aborts the match and the `<` is emitted
   literally, so ordinary comparisons/shifts (`a < b`, `x >> 3`),
   method-argument lists, annotation element lists, and HTML markup in
   Javadoc (`<p>`, `<a href="...">`, `</code>`) are left untouched --
   they either never reach a matching top-level `>`, or they reach one
   with no comma inside to rewrite anyway. `>>`/`>>>` naturally fall
   out of the depth-counting (each `>` closes exactly one level) rather
   than needing special-casing as shift-operator tokens. Bounded
   wildcards (`? extends Foo`, `? super Bar`) survive untouched because
   only `,[ \\t]+` sequences are collapsed to `,` -- nothing else in the
   matched span is rewritten.

Known, accepted false-negative (never false-positive) gaps -- see the
prototype report for the reasoning:
  - A generic span that is itself interrupted by a `//` or `/* */`
    comment (e.g. `Map<String, // key type\\n    Object>`) will fail to
    match (the segmentation pass has already cut the span into two
    pieces) and is left unrewritten.
  - Type-use annotations inside a type-argument list (`List<@NonNull
    String>`) are not in the allowed character set and are left
    unrewritten.
Both are misses, not corruptions: the script never rewrites a comma
outside a validated `<...>` span.

One real over-match was found and fixed during the reactor-wide run:
prose/comments enumerating comparison-operator examples (e.g. a Javadoc
comment listing `<123, >=123, <=123, ...` as sample query-syntax
tokens) are pure digits/punctuation with no letters, which satisfied
the depth-and-charset walk (the first `>` closed a "span" whose content
was just digits, a comma, and whitespace) even though it is not a type
list at all. Fix: a matched span must contain at least one letter or
`?` somewhere in it, since every real Java type reference is either an
identifier (which starts with a letter/`_`/`$`) or a bare `?` wildcard
-- a span of only digits/punctuation/whitespace can never be a genuine
type-argument list and is now rejected (bailed, i.e. left unrewritten).

Usage:
    python3 scripts/normalize-generic-commas.py <path> [<path> ...]
    python3 scripts/normalize-generic-commas.py --check <path> [...]   # dry run, exit 1 if changes needed

<path> may be a .java file or a directory (walked recursively, minus
target/ and .git/).
"""

import re
import sys
from pathlib import Path

# Characters that may legally appear inside a Java type-argument list.
# NOTE: '&' is handled separately (single '&' allowed, '&&' rejected).
_ALLOWED_CHARS = set(
    'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
    '_$.?[] \t\r\n,'
)

# Safety cap: a genuine type-argument list is never anywhere near this
# long. Bail rather than let a pathological run of allowed characters
# (e.g. a long prose sentence with lots of commas and no punctuation)
# swallow half a file.
_MAX_SPAN = 2000

_COMMA_SPACE_RE = re.compile(r',[ \t]+')
_HAS_LETTER_OR_WILDCARD_RE = re.compile(r'[A-Za-z?]')


def _find_generic_end(text, pos):
    """
    text[pos] == '<'. Try to find the index just past the matching
    top-level '>' for a "generic-shaped" span starting at pos, walking
    forward and tracking nesting depth. Returns that index, or None if
    the span is not generic-shaped (contains a disallowed character,
    an '&&', runs off the end of the segment, exceeds _MAX_SPAN, or has
    no letter/'?' anywhere in it -- every real type reference is an
    identifier or a wildcard, so a span of pure digits/punctuation,
    e.g. a comment enumerating comparison operators like "<123, >=123",
    can never be a genuine type-argument list).
    """
    n = len(text)
    depth = 0
    i = pos
    while i < n:
        if i - pos > _MAX_SPAN:
            return None
        c = text[i]
        if c == '<':
            depth += 1
            i += 1
            continue
        if c == '>':
            depth -= 1
            i += 1
            if depth == 0:
                if not _HAS_LETTER_OR_WILDCARD_RE.search(text[pos:i]):
                    return None
                return i
            continue
        if c == '&':
            prev_amp = i > pos + 1 and text[i - 1] == '&'
            next_amp = i + 1 < n and text[i + 1] == '&'
            if prev_amp or next_amp:
                return None
            i += 1
            continue
        if c in _ALLOWED_CHARS:
            i += 1
            continue
        return None
    return None


def tighten_generic_commas(text):
    """
    Scan `text` for generic-shaped `<...>` spans and collapse
    `,<space/tab>+` to `,` inside them (at every nesting level, in one
    shot, since a validated span never contains anything but
    type-argument-list characters). Returns (new_text, num_commas_changed).
    """
    out = []
    i = 0
    n = len(text)
    changed = 0
    while i < n:
        c = text[i]
        if c == '<':
            end = _find_generic_end(text, i)
            if end is not None:
                span = text[i:end]
                new_span, span_changed = _COMMA_SPACE_RE.subn(',', span)
                out.append(new_span)
                changed += span_changed
                i = end
                continue
        out.append(c)
        i += 1
    return ''.join(out), changed


# ---------------------------------------------------------------------------
# Segmentation: split a whole .java file into (kind, text) segments so the
# generic-span pass above can be applied only to CODE / comment text, never
# to string/char/text-block contents.
# ---------------------------------------------------------------------------

CODE = 'CODE'
LINE_COMMENT = 'LINE_COMMENT'
BLOCK_COMMENT = 'BLOCK_COMMENT'
STRING = 'STRING'
CHAR = 'CHAR'
TEXT_BLOCK = 'TEXT_BLOCK'

_REWRITE_KINDS = (CODE, LINE_COMMENT, BLOCK_COMMENT)


def _scan_escaped_literal(text, pos, quote):
    """
    text[pos] == quote (a single `"` or `'`). Return the index just past
    the matching unescaped closing quote, honoring `\\` escapes. If the
    literal is unterminated before a newline or EOF, returns the index
    of that newline/EOF (best-effort; malformed input is left as-is).
    """
    n = len(text)
    i = pos + 1
    while i < n:
        c = text[i]
        if c == '\\':
            i += 2
            continue
        if c == quote:
            return i + 1
        if c == '\n':
            return i
        i += 1
    return i


def _scan_text_block(text, pos):
    """
    text[pos:pos+3] == '\"\"\"'. Return the index just past the closing
    '\"\"\"' delimiter, honoring a single preceding backslash-escape of
    the closing delimiter. Falls back to EOF if unterminated.
    """
    n = len(text)
    i = pos + 3
    while i < n:
        if text[i] == '"' and text[i:i + 3] == '"""':
            # An odd number of immediately-preceding backslashes means
            # the first quote of this run is escaped; skip just past it
            # and keep looking for a real delimiter.
            backslashes = 0
            j = i - 1
            while j >= 0 and text[j] == '\\':
                backslashes += 1
                j -= 1
            if backslashes % 2 == 1:
                i += 1
                continue
            return i + 3
        i += 1
    return n


def segment_java_source(text):  # NOSONAR python:S3776 -- state-machine tokenizer; splitting further would obscure the transitions
    """
    Split `text` into a list of (kind, text) tuples covering the entire
    file, classifying each stretch as CODE, LINE_COMMENT, BLOCK_COMMENT
    (Javadoc included), STRING, CHAR, or TEXT_BLOCK.
    """
    segments = []
    n = len(text)
    i = 0
    start = 0

    def flush(kind, end):
        if end > start:
            segments.append((kind, text[start:end]))

    while i < n:
        c = text[i]
        if c == '/' and i + 1 < n and text[i + 1] == '/':
            flush(CODE, i)
            j = text.find('\n', i)
            j = n if j == -1 else j
            segments.append((LINE_COMMENT, text[i:j]))
            i = j
            start = i
            continue
        if c == '/' and i + 1 < n and text[i + 1] == '*':
            flush(CODE, i)
            j = text.find('*/', i + 2)
            j = n if j == -1 else j + 2
            segments.append((BLOCK_COMMENT, text[i:j]))
            i = j
            start = i
            continue
        if c == '"' and text[i:i + 3] == '"""':
            flush(CODE, i)
            j = _scan_text_block(text, i)
            segments.append((TEXT_BLOCK, text[i:j]))
            i = j
            start = i
            continue
        if c == '"':
            flush(CODE, i)
            j = _scan_escaped_literal(text, i, '"')
            segments.append((STRING, text[i:j]))
            i = j
            start = i
            continue
        if c == "'":
            flush(CODE, i)
            j = _scan_escaped_literal(text, i, "'")
            segments.append((CHAR, text[i:j]))
            i = j
            start = i
            continue
        i += 1

    flush(CODE, n)
    return segments


def transform_source(text):
    """
    Apply the generic-comma-tightening transform to a whole file's
    source text. Returns (new_text, code_changed, comment_changed).
    """
    segments = segment_java_source(text)
    out = []
    code_changed = 0
    comment_changed = 0
    for kind, seg_text in segments:
        if kind in _REWRITE_KINDS:
            new_seg, n_changed = tighten_generic_commas(seg_text)
            out.append(new_seg)
            if kind == CODE:
                code_changed += n_changed
            else:
                comment_changed += n_changed
        else:
            out.append(seg_text)
    return ''.join(out), code_changed, comment_changed


def find_java_files(root_dir):
    root_path = Path(root_dir)
    if root_path.is_file():
        return [root_path]
    java_files = []
    for java_file in root_path.rglob('*.java'):
        if 'target' in java_file.parts or '.git' in java_file.parts:
            continue
        java_files.append(java_file)
    return sorted(java_files)


def main():
    args = sys.argv[1:]
    check_only = False
    if args and args[0] == '--check':
        check_only = True
        args = args[1:]
    if not args:
        print(__doc__)
        return 1

    all_files = []
    for arg in args:
        all_files.extend(find_java_files(arg))

    files_changed = 0
    total_code_sites = 0
    total_comment_sites = 0
    needs_change = False

    for path in all_files:
        original = path.read_text(encoding='utf-8')
        new_text, code_n, comment_n = transform_source(original)
        if new_text != original:
            needs_change = True
            files_changed += 1
            total_code_sites += code_n
            total_comment_sites += comment_n
            if check_only:
                print(f"WOULD CHANGE: {path} (code={code_n}, comment={comment_n})")
            else:
                path.write_text(new_text, encoding='utf-8')
                print(f"Changed: {path} (code={code_n}, comment={comment_n})")

    print("\nSummary:")
    print(f"  Files scanned: {len(all_files)}")
    print(f"  Files changed: {files_changed}")
    print(f"  Comma sites tightened in code:    {total_code_sites}")
    print(f"  Comma sites tightened in comments: {total_comment_sites}")

    if check_only and needs_change:
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
