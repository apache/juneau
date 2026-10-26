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
Regenerate the generated images of the docs site, or check them.

Full docs: docs-images.md
"""

import argparse
import fnmatch
import html
import json
import os
import re
import shlex
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import threading
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

from juneau_root import find_juneau_root

CASES_DIR_NAME = 'docs-images'
SOURCES = {'ui': 'ui-cases.json', 'terminal': 'terminal-cases.json'}
IMAGE_DIRS = ('topics', 'developer-info')
IMAGE_EXTS = ('.png', '.svg', '.jpg', '.jpeg', '.gif', '.webp')
DEFAULT_PORT = 10000
PROBE_PATH = '/console/store'
DEFAULT_VIEWPORT = [1280, 900]
DEFAULT_SCALE = 2
SERVER_START_TIMEOUT = 600
# The petstore reads this to pin its clock to the seed data's own, so the store page's day figures repeat run to run.
FIXED_NOW_ENV = 'PETSTORE_FIXED_NOW'
FIXED_NOW = 'seed'
# A site-absolute image path: /topics/... or /developer-info/..., not part of a longer path such as /docs/topics/...
IMAGE_REF_RE = re.compile(r'(?<![\w/.-])/((?:' + '|'.join(re.escape(d) for d in IMAGE_DIRS) + r')/[^\s)"\'#?]+?\.(?:'
                          + '|'.join(e[1:] for e in IMAGE_EXTS) + r'))(?=[\s)"\'#?]|$)', re.IGNORECASE)
PETSTORE_CORE = Path('juneau-petstore') / 'juneau-petstore-core'
HARNESS = PETSTORE_CORE / 'src' / 'test' / 'js' / 'petstore-browser.cjs'


class Problem(Exception):
    """A failure reported to the user without a traceback."""


# ---------------------------------------------------------------------------------------------------------------------
# Cases files
# ---------------------------------------------------------------------------------------------------------------------

def load_cases(cases_dir, sources=None):
    """
    Load the cases files.

    Returns:
        (cases, unmanaged): cases is a list of dicts, each with its "source" added and the file's "defaults" merged
        in; unmanaged is the list of exempt globs from every file loaded.
    """
    cases, unmanaged, seen = [], [], {}
    for source, file_name in SOURCES.items():
        if sources and source not in sources:
            continue
        path = Path(cases_dir) / file_name
        if not path.is_file():
            continue
        data = json.loads(path.read_text(encoding='utf-8'))
        defaults = data.get('defaults', {})
        for entry in data.get('unmanaged', []):
            unmanaged.append(entry['glob'] if isinstance(entry, dict) else entry)
        for raw in data.get('cases', []):
            case = {**defaults, **raw, 'source': source}
            for key in ('id', 'image'):
                if not case.get(key):
                    raise Problem(f"{path.name}: a case has no '{key}': {json.dumps(raw)}")
            if case['id'] in seen:
                raise Problem(f"{path.name}: duplicate case id '{case['id']}'")
            seen[case['id']] = case
            cases.append(case)
    images = {}
    for case in cases:
        if case['image'] in images:
            raise Problem(f"cases '{images[case['image']]}' and '{case['id']}' both write {case['image']}")
        images[case['image']] = case['id']
    return cases, unmanaged


def select_cases(cases, only):
    """The cases whose id or image matches one of the --only patterns (globs), or all of them."""
    if not only:
        return list(cases)
    picked = [c for c in cases if any(fnmatch.fnmatchcase(c['id'], p) or fnmatch.fnmatchcase(c['image'], p)
                                      or fnmatch.fnmatchcase(Path(c['image']).name, p) for p in only)]
    if not picked:
        raise Problem(f"--only {' '.join(only)} matches no case")
    return picked


# ---------------------------------------------------------------------------------------------------------------------
# --check
# ---------------------------------------------------------------------------------------------------------------------

def page_references(pages_dir):
    """Map of each referenced image (path relative to static/) to the 'file:line' places that reference it."""
    refs = {}
    for md in sorted(Path(pages_dir).rglob('*.md*')):
        for n, line in enumerate(md.read_text(encoding='utf-8').splitlines(), 1):
            for m in IMAGE_REF_RE.finditer(line):
                refs.setdefault(m.group(1), []).append(f"{md.relative_to(pages_dir.parent)}:{n}")
    return refs


def image_files(static_dir):
    """Every image file under the managed static directories, relative to static/ with forward slashes."""
    out = []
    for d in IMAGE_DIRS:
        root = Path(static_dir) / d
        if root.is_dir():
            out += [p.relative_to(static_dir).as_posix() for p in root.rglob('*')
                    if p.is_file() and p.suffix.lower() in IMAGE_EXTS]
    return sorted(out)


def check(docs_root, cases, unmanaged):
    """
    Returns:
        (errors, warnings): lists of "KIND path: detail" lines.
    """
    static_dir, pages_dir = Path(docs_root) / 'static', Path(docs_root) / 'pages'
    refs = page_references(pages_dir)
    produced = {c['image']: c for c in cases}
    errors, warnings = [], []
    for image, case in produced.items():
        if not (static_dir / image).is_file():
            errors.append(f"MISSING static/{image}: case '{case['id']}' has not been generated")
    for image in image_files(static_dir):
        if image not in produced and not any(fnmatch.fnmatchcase(image, g) for g in unmanaged):
            errors.append(f"ORPHANED static/{image}: no case produces it and no 'unmanaged' glob exempts it")
    for image, places in sorted(refs.items()):
        if not (static_dir / image).is_file():
            errors.extend(f"DANGLING {place}: /{image} does not exist" for place in places)
    for image, case in produced.items():
        if image not in refs:
            warnings.append(f"UNREFERENCED static/{image}: no page references case '{case['id']}'")
    return errors, warnings


# ---------------------------------------------------------------------------------------------------------------------
# UI source
# ---------------------------------------------------------------------------------------------------------------------

def shot_name(case):
    """The file the harness writes for a case, in the scratch directory."""
    return case['id'] + Path(case['image']).suffix


def harness_input(cases, base_url, snapshots):
    """Translate UI cases into the petstore-browser.cjs input."""
    out = []
    for c in cases:
        shot = {'screenshot': shot_name(c)}
        if c.get('selector'):
            shot['selector'] = c['selector']
        hc = {'name': c['id'], 'path': c['path'], 'viewport': c.get('viewport', DEFAULT_VIEWPORT),
              'deviceScaleFactor': c.get('deviceScaleFactor', DEFAULT_SCALE),
              'actions': list(c.get('actions', [])) + [shot]}
        for key in ('waitUntil', 'initScript', 'allowFailedLoads'):
            if key in c:
                hc[key] = c[key]
        out.append(hc)
    return {'baseUrl': base_url.rstrip('/'), 'snapshots': str(snapshots), 'cases': out}


def case_failures(cases, report, snapshots):
    """'id: reason' for every case that did not produce a clean screenshot."""
    failures = []
    for c in cases:
        r = report.get(c['id'])
        if r is None:
            failures.append(f"{c['id']}: no report from the harness")
            continue
        why = []
        if r.get('status') != 200:
            why.append(f"HTTP status {r.get('status')}")
        if r.get('timeouts'):
            why.append('timed out waiting for ' + '; '.join(r['timeouts']))
        if r.get('jsFailures'):
            why.append('script errors: ' + '; '.join(r['jsFailures']))
        if not (Path(snapshots) / shot_name(c)).is_file():
            why.append('no screenshot written')
        if why:
            failures.append(f"{c['id']}: " + ', '.join(why))
    return failures


def playwright_version(juneau_root):
    pom = (Path(juneau_root) / PETSTORE_CORE / 'pom.xml').read_text(encoding='utf-8')
    m = re.search(r'<playwright\.version>([^<]+)</playwright\.version>', pom)
    if not m:
        raise Problem(f"no <playwright.version> in {PETSTORE_CORE / 'pom.xml'}")
    return m.group(1)


def ensure_playwright(juneau_root):
    """The js directory holding node_modules/playwright and the Chromium download, installing them if missing."""
    for tool in ('node', 'npm'):
        if not shutil.which(tool):
            raise Problem(f"'{tool}' is not on the PATH; the ui source needs Node.js")
    js_dir = Path(juneau_root) / PETSTORE_CORE / 'target' / 'js'
    env = {**os.environ, 'PLAYWRIGHT_BROWSERS_PATH': str(js_dir / 'browsers')}
    cli = js_dir / 'node_modules' / 'playwright' / 'cli.js'
    if not cli.is_file():
        version = playwright_version(juneau_root)
        print(f"-> Installing playwright@{version} into {js_dir}")
        run_checked(['npm', 'install', '--no-fund', '--no-audit', '--prefix', str(js_dir), f'playwright@{version}'])
    if not any((js_dir / 'browsers').glob('chromium*')):
        print(f"-> Installing Chromium into {js_dir / 'browsers'}")
        run_checked(['node', str(cli), 'install', 'chromium'], env=env)
    return js_dir


def run_checked(cmd, **kwargs):
    r = subprocess.run(cmd, capture_output=True, text=True, **kwargs)
    if r.returncode != 0:
        tail = '\n'.join((r.stdout + r.stderr).splitlines()[-20:])
        raise Problem(f"command failed ({r.returncode}): {' '.join(cmd)}\n{tail}")
    return r


def generate_ui(docs_root, juneau_root, cases, base_url):
    js_dir = ensure_playwright(juneau_root)
    harness = Path(juneau_root) / HARNESS
    if not harness.is_file():
        raise Problem(f"harness not found: {harness}")
    with tempfile.TemporaryDirectory(prefix='docs-images-') as tmp:
        snapshots = Path(tmp) / 'snapshots'
        snapshots.mkdir()
        input_file = Path(tmp) / 'input.json'
        input_file.write_text(json.dumps(harness_input(cases, base_url, snapshots), indent=1), encoding='utf-8')
        env = {**os.environ, 'NODE_PATH': str(js_dir / 'node_modules'),
               'PLAYWRIGHT_BROWSERS_PATH': str(js_dir / 'browsers')}
        print(f"-> Shooting {len(cases)} UI case(s) against {base_url}")
        r = run_checked(['node', str(harness), str(input_file)], env=env)
        report = json.loads(r.stdout).get('cases', {})
        failures = case_failures(cases, report, snapshots)
        if failures:
            raise Problem('UI cases failed, no image was replaced:\n  ' + '\n  '.join(failures))
        for c in cases:
            target = Path(docs_root) / 'static' / c['image']
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(snapshots / shot_name(c)), str(target))
            print(f"   static/{c['image']}")


# ---------------------------------------------------------------------------------------------------------------------
# Terminal source
# ---------------------------------------------------------------------------------------------------------------------

TERM_DEFAULT_COLUMNS, TERM_DEFAULT_ROWS, TERM_DEFAULT_TIMEOUT = 100, 30, 60
JUNEAU_TERMINAL_PY = Path('juneau-terminal') / 'src' / 'main' / 'python' / 'juneau_terminal.py'
FIXTURES_DIR = Path('scripts') / CASES_DIR_NAME / 'fixtures'
# The font stack of the site's code blocks (Infima's default); the SVG's text is real text in this font.
TERM_FONT = 'SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace'
TERM_FONT_SIZE, TERM_CELL_W, TERM_LINE_H, TERM_PAD = 14, 8.4, 18, 12
TERM_FG, TERM_BG, TERM_DIVIDER = '#d4d4d4', '#1e1e1e', '#3c3c3c'
# The 16 ANSI colours (VS Code dark), one fixed palette for both site themes: a terminal looks like a terminal.
TERM_PALETTE = ['#000000', '#cd3131', '#0dbc79', '#e5e510', '#2472c8', '#bc3fbc', '#11a8cd', '#e5e5e5',
                '#666666', '#f14c4c', '#23d18b', '#f5f543', '#3b8eea', '#d670d6', '#29b8db', '#ffffff']
ANSI_CSI_RE = re.compile(r'\x1b\[([0-9;:?<=>!"$#%&\' ]*)([@-~])')
ANSI_OTHER_RE = re.compile(r'\x1b(?:\][^\x07\x1b]*(?:\x07|\x1b\\)|[()][0-9A-Za-z]|[@-Z\\-_=>])')
# default style: (fg, bg, bold, dim, underline, reverse); fg and bg are None or a '#rrggbb' string
DEFAULT_STYLE = (None, None, False, False, False, False)


def color_256(n):
    """The '#rrggbb' of xterm colour n."""
    if n < 16:
        return TERM_PALETTE[n]
    if n < 232:
        n -= 16
        return '#%02x%02x%02x' % tuple(0 if c == 0 else 55 + 40 * c for c in (n // 36, n // 6 % 6, n % 6))
    v = 8 + 10 * (n - 232)
    return '#%02x%02x%02x' % (v, v, v)


def apply_sgr(style, params):
    """The style after one SGR sequence; unknown codes are ignored."""
    fg, bg, bold, dim, underline, reverse = style
    codes = [int(p) if p.isdigit() else 0 for p in (params.replace(':', ';') or '0').split(';')]
    i = 0
    while i < len(codes):
        c = codes[i]
        if c == 0:
            fg, bg, bold, dim, underline, reverse = DEFAULT_STYLE
        elif c == 1:
            bold = True
        elif c == 2:
            dim = True
        elif c == 4:
            underline = True
        elif c == 7:
            reverse = True
        elif c == 22:
            bold = dim = False
        elif c == 24:
            underline = False
        elif c == 27:
            reverse = False
        elif 30 <= c <= 37:
            fg = TERM_PALETTE[c - 30]
        elif c == 39:
            fg = None
        elif 40 <= c <= 47:
            bg = TERM_PALETTE[c - 40]
        elif c == 49:
            bg = None
        elif 90 <= c <= 97:
            fg = TERM_PALETTE[c - 90 + 8]
        elif 100 <= c <= 107:
            bg = TERM_PALETTE[c - 100 + 8]
        elif c in (38, 48):
            color = None
            if i + 2 < len(codes) and codes[i + 1] == 5:
                color, i = color_256(min(codes[i + 2], 255)), i + 2
            elif i + 4 < len(codes) and codes[i + 1] == 2:
                color, i = '#%02x%02x%02x' % tuple(min(v, 255) for v in codes[i + 2:i + 5]), i + 4
            if color:
                fg, bg = (color, bg) if c == 38 else (fg, color)
        i += 1
    return (fg, bg, bold, dim, underline, reverse)


def char_width(ch):
    if unicodedata.combining(ch):
        return 0
    return 2 if unicodedata.east_asian_width(ch) in ('W', 'F') else 1


def parse_ansi_line(line):
    """
    One captured line as a list of cells [text, width, style].  SGR sequences set the style; every other escape
    sequence and control character is dropped, never emitted.
    """
    cells, style, pos = [], DEFAULT_STYLE, 0
    while pos < len(line):
        if line[pos] == '\x1b':
            m = ANSI_CSI_RE.match(line, pos)
            if m:
                if m.group(2) == 'm':
                    style = apply_sgr(style, m.group(1))
                pos = m.end()
                continue
            m = ANSI_OTHER_RE.match(line, pos)
            pos = m.end() if m else pos + 1
            continue
        ch = line[pos]
        pos += 1
        if ch < ' ' or ch == '\x7f':
            continue
        w = char_width(ch)
        if w == 0:
            if cells:
                cells[-1][0] += ch
            continue
        cells.append([ch, w, style])
    while cells and cells[-1][0] == ' ' and cells[-1][2] == DEFAULT_STYLE:
        cells.pop()
    return cells


def parse_ansi(text):
    """Captured pane text as a list of rows of cells."""
    return [parse_ansi_line(line) for line in text.split('\n')]


def style_colors(style):
    """(fg, bg) of a style as '#rrggbb', with reverse applied."""
    fg, bg = style[0] or TERM_FG, style[1] or TERM_BG
    return (bg, fg) if style[5] else (fg, bg)


def row_runs(cells):
    """The row's cells merged into runs of one style: (first column, columns, text, style)."""
    runs, col = [], 0
    for text, width, style in cells:
        if runs and runs[-1][3] == style:
            runs[-1][1] += width
            runs[-1][2] += text
        else:
            runs.append([col, width, text, style])
        col += width
    return runs


def num(v):
    return ('%.2f' % v).rstrip('0').rstrip('.')


def render_svg(panes, title, description):
    """
    Render one or more panes side by side as an SVG whose text is real <text>: one <text> per row, one <tspan> per
    same-style run, each placed at its column.  A pane is a list of rows of cells, as parse_ansi returns; a tuple of
    panes is a column of panes stacked top to bottom, one row apart.
    """
    columns = [p if isinstance(p, tuple) else (p,) for p in panes]
    gap = 1 if len(columns) > 1 else 0
    cols = [max([sum(c[1] for c in row) for rows in column for row in rows] + [1]) for column in columns]
    heights = [sum(len(rows) for rows in column) + len(column) - 1 for column in columns]
    total_cols = sum(cols) + gap * (len(columns) - 1)
    n_rows = max(max(heights), 1)
    width, height = total_cols * TERM_CELL_W + 2 * TERM_PAD, n_rows * TERM_LINE_H + 2 * TERM_PAD
    bg_parts, text_parts, x_col = [], [], 0
    for p, column in enumerate(columns):
        if p:
            bg_parts.append(f'<rect x="{num(TERM_PAD + (x_col - gap) * TERM_CELL_W)}" y="{num(TERM_PAD)}" '
                            f'width="{num(gap * TERM_CELL_W)}" height="{num(n_rows * TERM_LINE_H)}" fill="{TERM_DIVIDER}"/>')
        y_row = 0
        for q, rows in enumerate(column):
            if q:
                bg_parts.append(f'<rect x="{num(TERM_PAD + x_col * TERM_CELL_W)}" y="{num(TERM_PAD + y_row * TERM_LINE_H)}" '
                                f'width="{num(cols[p] * TERM_CELL_W)}" height="{TERM_LINE_H}" fill="{TERM_DIVIDER}"/>')
                y_row += 1
            for r, cells in enumerate(rows):
                y = TERM_PAD + (y_row + r) * TERM_LINE_H
                spans = []
                for col, w, text, style in row_runs(cells):
                    fg, bg = style_colors(style)
                    x = TERM_PAD + (x_col + col) * TERM_CELL_W
                    if bg != TERM_BG:
                        bg_parts.append(f'<rect x="{num(x)}" y="{num(y)}" width="{num(w * TERM_CELL_W)}" '
                                        f'height="{TERM_LINE_H}" fill="{bg}"/>')
                    # Words are placed one by one, so a run's inner spaces never stretch; an underlined run stays whole.
                    segments = [(col, w, text)] if style[4] else [
                        (col + sum(map(char_width, text[:m.start()])), sum(map(char_width, m.group())), m.group())
                        for m in re.finditer(r'[^ ]+', text)]
                    for seg_col, seg_w, seg_text in segments:
                        attrs = f'x="{num(TERM_PAD + (x_col + seg_col) * TERM_CELL_W)}" ' \
                                f'textLength="{num(seg_w * TERM_CELL_W)}" fill="{fg}"'
                        if style[2]:
                            attrs += ' font-weight="bold"'
                        if style[3]:
                            attrs += ' fill-opacity="0.6"'
                        if style[4]:
                            attrs += ' text-decoration="underline"'
                        spans.append(f'<tspan {attrs}>{html.escape(seg_text, quote=True)}</tspan>')
                if spans:
                    text_parts.append(f'<text y="{num(y + TERM_LINE_H * 0.75)}">' + ''.join(spans) + '</text>')
            y_row += len(rows)
        x_col += cols[p] + gap
    return '\n'.join([
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{num(width)}" height="{num(height)}" '
        f'viewBox="0 0 {num(width)} {num(height)}" role="img" aria-labelledby="title desc">',
        f'<title id="title">{html.escape(title, quote=True)}</title>',
        f'<desc id="desc">{html.escape(description, quote=True)}</desc>',
        f'<rect width="{num(width)}" height="{num(height)}" rx="6" fill="{TERM_BG}"/>',
        *bg_parts,
        f'<g font-family=\'{TERM_FONT}\' font-size="{TERM_FONT_SIZE}" xml:space="preserve">',
        *text_parts,
        '</g>',
        '</svg>',
        ''])


def mask_text(text, masks):
    """Apply the case's [regex, replacement] pairs, line by line, to the captured text."""
    for pattern, replacement in masks:
        text = re.sub(pattern, replacement, text, flags=re.MULTILINE)
    return text


def tmux(socket_name, *args):
    return subprocess.run(['tmux', '-u', '-L', socket_name, '-f', '/dev/null', *args], capture_output=True, text=True)


def expand_command(command, juneau_root, docs_root):
    subs = {'{juneau_terminal}': str(Path(juneau_root) / JUNEAU_TERMINAL_PY), '{fixtures}': str(Path(docs_root) / FIXTURES_DIR)}
    out = []
    for arg in command:
        for key, value in subs.items():
            arg = arg.replace(key, value)
        out.append(arg)
    return out


def capture_pane(pane, juneau_root, docs_root, cwd):
    """
    Run one pane spec in a detached tmux session of fixed size, wait until its "waitFor" regex shows and the pane has
    stopped changing, and return the captured text with escapes (tmux capture-pane -e -p), masked.  The session lives
    on a private tmux server (own socket, no user config) that is killed afterwards.
    """
    columns, rows = pane.get('columns', TERM_DEFAULT_COLUMNS), pane.get('rows', TERM_DEFAULT_ROWS)
    timeout = pane.get('timeout', TERM_DEFAULT_TIMEOUT)
    masks = pane.get('mask', [])
    wait_for = re.compile(pane['waitFor'], re.MULTILINE) if pane.get('waitFor') else None
    command = expand_command(pane['command'], juneau_root, docs_root)
    env = {'LANG': 'en_US.UTF-8', 'LC_ALL': 'en_US.UTF-8', **pane.get('env', {})}
    shell = 'env ' + ' '.join(shlex.quote(f'{k}={v}') for k, v in env.items()) + ' ' + shlex.join(command) + '; sleep 600'
    name = re.sub(r'[^\w-]', '_', f"docs-images-{os.getpid()}-{pane.get('id', 'pane')}")
    r = tmux(name, 'new-session', '-d', '-s', name, '-x', str(columns), '-y', str(rows), '-c', str(cwd), shell)
    if r.returncode != 0:
        raise Problem(f"tmux could not start the fixture: {r.stderr.strip()}")
    try:
        deadline = time.monotonic() + timeout
        last = None
        while True:
            plain = tmux(name, 'capture-pane', '-p', '-t', name).stdout
            if wait_for is None or wait_for.search(plain):
                shown = tmux(name, 'capture-pane', '-e', '-p', '-t', name).stdout
                masked = mask_text(shown, masks)
                if masked == last:
                    return masked
                last = masked
            elif time.monotonic() > deadline:
                raise Problem(f"timed out after {timeout}s waiting for /{pane['waitFor']}/ in:\n{plain}")
            if time.monotonic() > deadline:
                raise Problem(f"the pane kept changing for {timeout}s; mask what varies:\n{last}")
            time.sleep(0.3)
    finally:
        tmux(name, 'kill-session', '-t', name)
        tmux(name, 'kill-server')
        # kill-server leaves the socket file behind
        (Path(os.environ.get('TMUX_TMPDIR', '/tmp')) / f'tmux-{os.getuid()}' / name).unlink(missing_ok=True)


def trim_rows(rows):
    """The rows without the blank ones at the end (at least one row is kept)."""
    rows = list(rows)
    while len(rows) > 1 and not rows[-1]:
        rows.pop()
    return rows


def render_terminal_case(case, juneau_root, docs_root):
    """The SVG text of one terminal case (a single pane, or its "panes" side by side; a {"stack": [...]} entry is a column)."""
    specs = case.get('panes') or [case]
    with tempfile.TemporaryDirectory(prefix='docs-images-term-') as cwd:
        def grid(spec, name):
            return trim_rows(parse_ansi(capture_pane({**spec, 'id': f"{case['id']}-{name}"}, juneau_root, docs_root, cwd)))
        grids = [tuple(grid(s, f'{i}-{j}') for j, s in enumerate(spec['stack'])) if 'stack' in spec else grid(spec, i)
                 for i, spec in enumerate(specs)]
    return render_svg(grids, case.get('title', case['id']), case.get('description', ''))


def generate_terminal(docs_root, juneau_root, cases):
    if not shutil.which('tmux'):
        raise Problem("'tmux' is not on the PATH; the terminal source needs tmux")
    if not (Path(juneau_root) / JUNEAU_TERMINAL_PY).is_file():
        raise Problem(f"juneau_terminal.py not found: {Path(juneau_root) / JUNEAU_TERMINAL_PY}")
    print(f"-> Capturing {len(cases)} terminal case(s)")
    rendered = {}
    for c in cases:
        rendered[c['id']] = render_terminal_case(c, juneau_root, docs_root)
    for c in cases:
        target = Path(docs_root) / 'static' / c['image']
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(rendered[c['id']], encoding='utf-8')
        print(f"   static/{c['image']}")


# ---------------------------------------------------------------------------------------------------------------------
# Petstore server
# ---------------------------------------------------------------------------------------------------------------------

def looks_like_petstore(base_url, timeout=5):
    try:
        with urllib.request.urlopen(base_url.rstrip('/') + PROBE_PATH, timeout=timeout) as resp:
            return resp.status == 200
    except (urllib.error.URLError, OSError, ValueError):
        return False


def select_for_server(cases, fresh):
    """Drops the fresh-server-only cases when the petstore was not started by this script, and warns about drift."""
    if fresh:
        return cases
    print(f"WARNING: this petstore was not started by this script, so it does not run on the fixed clock "
          f"({FIXED_NOW_ENV}): shots that show the date or the time of day may differ from run to run.")
    kept = [c for c in cases if not c.get('freshServerOnly')]
    for c in cases:
        if c not in kept:
            print(f"   skipping {c['id']}: it needs a petstore started by this script (it counts requests since start)")
    return kept


class Server:
    """A petstore started by this script: it is ready once the console answers at url, and its process group is stopped."""

    def __init__(self, cmd, cwd, url, timeout=SERVER_START_TIMEOUT, poll=2, env=None):
        self.cmd, self.url, self.lines = cmd, url, []
        self.proc = subprocess.Popen(cmd, cwd=cwd, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                                     stderr=subprocess.STDOUT, text=True, start_new_session=True, env=env)

        def pump():
            for line in self.proc.stdout:
                self.lines.append(line.rstrip('\n'))

        threading.Thread(target=pump, daemon=True).start()
        deadline = time.monotonic() + timeout
        try:
            while not looks_like_petstore(url):
                if self.proc.poll() is not None or time.monotonic() > deadline:
                    self.stop()
                    time.sleep(0.2)  # let the pump drain the last lines
                    tail = '\n'.join(self.lines[-20:])
                    raise Problem(f"petstore did not start: {' '.join(cmd)}\n{tail}")
                time.sleep(poll)
        except BaseException:
            self.stop()
            raise

    def stop(self):
        if self.proc.poll() is not None:
            return
        try:
            os.killpg(self.proc.pid, signal.SIGTERM)
            self.proc.wait(30)
        except subprocess.TimeoutExpired:
            os.killpg(self.proc.pid, signal.SIGKILL)
            self.proc.wait()
        except ProcessLookupError:
            pass


def port_holder(port):
    """A description of the process listening on port ('PID 123 (java)'), or None if it can't be found."""
    try:
        r = subprocess.run(['lsof', '-nP', f'-iTCP:{port}', '-sTCP:LISTEN', '-Fpc'],
                           capture_output=True, text=True, timeout=10)
    except (OSError, subprocess.SubprocessError):
        return None
    pid = cmd = None
    for line in r.stdout.splitlines():
        if line.startswith('p') and pid is None:
            pid = line[1:]
        elif line.startswith('c') and cmd is None:
            cmd = line[1:]
    return f'PID {pid}' + (f' ({cmd})' if cmd else '') if pid else None


def port_in_use(url):
    parsed = urllib.parse.urlsplit(url)
    try:
        with socket.create_connection((parsed.hostname, parsed.port), timeout=2):
            return True
    except OSError:
        return False


# ---------------------------------------------------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------------------------------------------------

def parse_args(argv):
    script_dir = Path(__file__).resolve().parent
    p = argparse.ArgumentParser(description='Regenerate or check the generated images of the docs site.')
    p.add_argument('--check', action='store_true', help='write nothing; report problems and exit 1 on any')
    p.add_argument('--only', nargs='+', metavar='PATTERN', help='case ids or image names (globs) to regenerate')
    p.add_argument('--source', choices=sorted(SOURCES), help='regenerate one source only')
    p.add_argument('--port', type=int, default=DEFAULT_PORT, metavar='N',
                   help=f'port of the petstore to reuse or start (default {DEFAULT_PORT}); ignored with --base-url')
    p.add_argument('--base-url', help='an already-running petstore to shoot (default: reuse or start one)')
    p.add_argument('--juneau-root', type=Path, default=None,
                   help='Juneau checkout (default: $JUNEAU_ROOT, then sibling ../master, then ../juneau)')
    p.add_argument('--docs-root', type=Path, default=script_dir.parent, help=argparse.SUPPRESS)
    return p.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    docs_root = args.docs_root.resolve()
    cases_dir = docs_root / 'scripts' / CASES_DIR_NAME
    try:
        if args.check:
            cases, unmanaged = load_cases(cases_dir)
            errors, warnings = check(docs_root, cases, unmanaged)
            for line in errors + warnings:
                print(line)
            print(f"{len(cases)} generated image(s): {len(errors)} problem(s), {len(warnings)} warning(s)")
            return 1 if errors else 0

        cases, _ = load_cases(cases_dir, [args.source] if args.source else None)
        cases = select_cases(cases, args.only)
        ui = [c for c in cases if c['source'] == 'ui']
        terminal = [c for c in cases if c['source'] == 'terminal']
        if not ui and not terminal:
            print('Nothing to generate.')
            return 0
        juneau_root = find_juneau_root(docs_root, args.juneau_root)
        if terminal:
            start = time.monotonic()
            generate_terminal(docs_root, juneau_root, terminal)
            print(f"Generated {len(terminal)} terminal image(s) in {time.monotonic() - start:.0f}s")
        if not ui:
            return 0
        server = None
        base_url = args.base_url
        fresh = False
        if not base_url:
            default_url = f'http://localhost:{args.port}'
            if looks_like_petstore(default_url):
                base_url = default_url
                print(f"-> Using the petstore already running at {base_url}")
            elif port_in_use(default_url):
                holder = port_holder(args.port)
                raise Problem(f"port {args.port} is in use by something that is not the petstore"
                              f"{f' ({holder})' if holder else ''}; stop it, or pass "
                              "--port/--base-url for a petstore running elsewhere")
            else:
                print('-> Starting petstore-jetty (this can take a few minutes on a first build)')
                server = Server([sys.executable, str(juneau_root / 'scripts' / 'start-petstore-jetty.py'),
                                 '--port', str(args.port)],
                                cwd=juneau_root, url=default_url, env={**os.environ, FIXED_NOW_ENV: FIXED_NOW})
                base_url = server.url
                fresh = True
                print(f"   started at {base_url}")
        elif not looks_like_petstore(base_url):
            raise Problem(f"{base_url}{PROBE_PATH} does not answer 200; is the petstore running there?")
        ui = select_for_server(ui, fresh)
        if not ui:
            print('Nothing to generate.')
            return 0
        try:
            start = time.monotonic()
            generate_ui(docs_root, juneau_root, ui, base_url)
            print(f"Generated {len(ui)} image(s) in {time.monotonic() - start:.0f}s")
        finally:
            if server:
                server.stop()
        return 0
    except Problem as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print('Interrupted; no image was replaced.', file=sys.stderr)
        return 130


if __name__ == '__main__':
    sys.exit(main())
