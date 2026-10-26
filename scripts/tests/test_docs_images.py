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
"""Tests for docs-images.py: cases files, --check, and the UI case translation."""

import json
import os
import socket
import sys

import pytest

from conftest import load_script

docs_images = load_script('docs-images.py')


def write(path, text=''):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)
    return path


def write_cases(root, cases, unmanaged=None, defaults=None, name='ui-cases.json'):
    data = {'cases': cases}
    if unmanaged is not None:
        data['unmanaged'] = unmanaged
    if defaults is not None:
        data['defaults'] = defaults
    write(root / 'scripts' / 'docs-images' / name, json.dumps(data))


def run_check(root):
    cases, unmanaged = docs_images.load_cases(root / 'scripts' / 'docs-images')
    return docs_images.check(root, cases, unmanaged)


@pytest.fixture
def clean_tree(tmp_path):
    """One generated image, referenced from one page, plus one unmanaged image."""
    write(tmp_path / 'static' / 'topics' / 'Page.Shot.1.png', 'png')
    write(tmp_path / 'static' / 'topics' / 'Legacy.1.png', 'png')
    write(tmp_path / 'pages' / 'topics' / '01.Page.md',
          '# Page\n\n![A shot](/topics/Page.Shot.1.png)\n\n![Old](/topics/Legacy.1.png)\n'
          'See [another page](/docs/topics/Other) and /docs/topics/Foo.png in prose.\n')
    write_cases(tmp_path, [{'id': 'shot', 'image': 'topics/Page.Shot.1.png', 'path': '/x'}],
                unmanaged=[{'glob': 'topics/Legacy.*', 'reason': 'no route reproduces it'}])
    return tmp_path


# ---------------------------------------------------------------------------------------------------------------------
# --check
# ---------------------------------------------------------------------------------------------------------------------

def test_clean_tree_has_no_findings(clean_tree):
    assert run_check(clean_tree) == ([], [])
    assert docs_images.main(['--check', '--docs-root', str(clean_tree)]) == 0


def test_missing(clean_tree):
    (clean_tree / 'static' / 'topics' / 'Page.Shot.1.png').unlink()
    errors, _ = run_check(clean_tree)
    assert "MISSING static/topics/Page.Shot.1.png: case 'shot' has not been generated" in errors
    assert docs_images.main(['--check', '--docs-root', str(clean_tree)]) == 1


def test_orphaned(clean_tree):
    write(clean_tree / 'static' / 'developer-info' / 'how-to' / 'Stray.png', 'png')
    errors, _ = run_check(clean_tree)
    assert errors == ['ORPHANED static/developer-info/how-to/Stray.png: no case produces it and no '
                      "'unmanaged' glob exempts it"]


def test_unmanaged_plain_string_glob_exempts(clean_tree):
    write(clean_tree / 'static' / 'developer-info' / 'how-to' / 'Stray.png', 'png')
    write_cases(clean_tree, [{'id': 'shot', 'image': 'topics/Page.Shot.1.png', 'path': '/x'}],
                unmanaged=['topics/Legacy.*', 'developer-info/how-to/*'])
    assert run_check(clean_tree) == ([], [])


def test_non_image_files_are_not_orphans(clean_tree):
    write(clean_tree / 'static' / 'topics' / 'notes.txt', 'x')
    assert run_check(clean_tree) == ([], [])


def test_dangling_reference(clean_tree):
    write(clean_tree / 'pages' / 'topics' / '02.Other.md', 'x\n<img src="/topics/Gone.svg" alt="gone"/>\n')
    errors, _ = run_check(clean_tree)
    assert errors == ['DANGLING pages/topics/02.Other.md:2: /topics/Gone.svg does not exist']


def test_unreferenced_is_a_warning_only(clean_tree):
    write(clean_tree / 'pages' / 'topics' / '01.Page.md', '![Old](/topics/Legacy.1.png)\n')
    errors, warnings = run_check(clean_tree)
    assert errors == []
    assert warnings == ["UNREFERENCED static/topics/Page.Shot.1.png: no page references case 'shot'"]
    assert docs_images.main(['--check', '--docs-root', str(clean_tree)]) == 0


def test_check_reads_every_cases_file(clean_tree):
    write_cases(clean_tree, [{'id': 'term', 'image': 'topics/Run.Live.1.svg'}], name='terminal-cases.json')
    errors, _ = run_check(clean_tree)
    assert errors == ["MISSING static/topics/Run.Live.1.svg: case 'term' has not been generated"]


def test_reference_pattern():
    refs = [m.group(1) for m in docs_images.IMAGE_REF_RE.finditer(
        '![a](/topics/A.1.png) ![b](/developer-info/how-to/B.2.PNG "title") [doc](/docs/topics/C.png) '
        'https://x.org/topics/D.png src="/topics/E.svg" /topics/NotAnImage')]
    assert refs == ['topics/A.1.png', 'developer-info/how-to/B.2.PNG', 'topics/E.svg']


# ---------------------------------------------------------------------------------------------------------------------
# Cases files
# ---------------------------------------------------------------------------------------------------------------------

def test_defaults_are_merged_and_case_wins(tmp_path):
    write_cases(tmp_path, [{'id': 'a', 'image': 'topics/A.png', 'path': '/a'},
                           {'id': 'b', 'image': 'topics/B.png', 'path': '/b', 'viewport': [800, 600]}],
                defaults={'viewport': [1280, 900], 'waitUntil': 'load'})
    cases, _ = docs_images.load_cases(tmp_path / 'scripts' / 'docs-images')
    assert [(c['viewport'], c['waitUntil'], c['source']) for c in cases] == [
        ([1280, 900], 'load', 'ui'), ([800, 600], 'load', 'ui')]


@pytest.mark.parametrize('cases, message', [
    ([{'image': 'topics/A.png'}], "a case has no 'id'"),
    ([{'id': 'a'}], "a case has no 'image'"),
    ([{'id': 'a', 'image': 'topics/A.png'}, {'id': 'a', 'image': 'topics/B.png'}], "duplicate case id 'a'"),
    ([{'id': 'a', 'image': 'topics/A.png'}, {'id': 'b', 'image': 'topics/A.png'}], "both write topics/A.png"),
])
def test_bad_cases_files(tmp_path, cases, message):
    write_cases(tmp_path, cases)
    with pytest.raises(docs_images.Problem, match=message):
        docs_images.load_cases(tmp_path / 'scripts' / 'docs-images')


def test_source_filter(clean_tree):
    write_cases(clean_tree, [{'id': 'term', 'image': 'topics/Run.Live.1.svg'}], name='terminal-cases.json')
    cases, _ = docs_images.load_cases(clean_tree / 'scripts' / 'docs-images', ['ui'])
    assert [c['id'] for c in cases] == ['shot']


def test_select_by_id_image_or_file_name_glob():
    cases = [{'id': 'themes-open', 'image': 'topics/PetstoreThemes.Stock.1.png'},
             {'id': 'store', 'image': 'topics/PetstoreStore.Page.1.png'}]
    pick = lambda *only: [c['id'] for c in docs_images.select_cases(cases, list(only))]
    assert pick() == ['themes-open', 'store']
    assert pick('themes-*') == ['themes-open']
    assert pick('topics/PetstoreStore.Page.1.png') == ['store']
    assert pick('PetstoreThemes.*') == ['themes-open']
    with pytest.raises(docs_images.Problem, match='matches no case'):
        pick('nothing')


# ---------------------------------------------------------------------------------------------------------------------
# UI case translation
# ---------------------------------------------------------------------------------------------------------------------

def test_harness_input_translation(tmp_path):
    cases = [
        {'id': 'page', 'image': 'topics/P.1.png', 'path': '/console/store',
         'actions': [{'waitFor': 'ready()'}], 'waitUntil': 'load'},
        {'id': 'card', 'image': 'topics/P.2.png', 'path': '/console/dev/charts', 'selector': '#c1',
         'viewport': [1000, 700], 'deviceScaleFactor': 1, 'initScript': 'x=1', 'description': 'ignored'},
    ]
    got = docs_images.harness_input(cases, 'http://localhost:1234/', tmp_path)
    assert got == {'baseUrl': 'http://localhost:1234', 'snapshots': str(tmp_path), 'cases': [
        {'name': 'page', 'path': '/console/store', 'viewport': [1280, 900], 'deviceScaleFactor': 2,
         'waitUntil': 'load', 'actions': [{'waitFor': 'ready()'}, {'screenshot': 'page.png'}]},
        {'name': 'card', 'path': '/console/dev/charts', 'viewport': [1000, 700], 'deviceScaleFactor': 1,
         'initScript': 'x=1', 'actions': [{'screenshot': 'card.png', 'selector': '#c1'}]},
    ]}
    # The case's own action list is not modified.
    assert cases[0]['actions'] == [{'waitFor': 'ready()'}]


def test_case_failures(tmp_path):
    cases = [{'id': n, 'image': f'topics/{n}.png'} for n in ('ok', 'status', 'timeout', 'js', 'noshot', 'absent')]
    for n in ('ok', 'status', 'timeout', 'js'):
        write(tmp_path / f'{n}.png', 'png')
    clean = {'status': 200, 'timeouts': [], 'jsFailures': []}
    report = {'ok': clean, 'status': {**clean, 'status': 404}, 'timeout': {**clean, 'timeouts': ['ready()']},
              'js': {**clean, 'jsFailures': ['harness: no element']}, 'noshot': clean}
    assert docs_images.case_failures(cases, report, tmp_path) == [
        'status: HTTP status 404',
        'timeout: timed out waiting for ready()',
        'js: script errors: harness: no element',
        'noshot: no screenshot written',
        'absent: no report from the harness',
    ]


def test_playwright_version_comes_from_the_pom(tmp_path):
    write(tmp_path / 'juneau-petstore' / 'juneau-petstore-core' / 'pom.xml',
          '<project><profiles><profile><properties><playwright.version>1.49.1</playwright.version>'
          '</properties></profile></profiles></project>')
    assert docs_images.playwright_version(tmp_path) == '1.49.1'


# ---------------------------------------------------------------------------------------------------------------------
# Server start
# ---------------------------------------------------------------------------------------------------------------------

def free_port():
    with socket.socket() as s:
        s.bind(('127.0.0.1', 0))
        return s.getsockname()[1]


FAKE_PETSTORE = """
import http.server, os, sys, time
time.sleep(1)
class H(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200 if self.path == '/console/store' else 404)
        self.end_headers()
    def log_message(self, *a):
        pass
http.server.HTTPServer(('localhost', int(sys.argv[1])), H).serve_forever()
"""


def test_server_waits_for_the_console_and_stops(tmp_path):
    port = free_port()
    url = f'http://localhost:{port}'
    script = write(tmp_path / 'fake.py', FAKE_PETSTORE)
    assert not docs_images.port_in_use(url)
    server = docs_images.Server([sys.executable, str(script), str(port)], cwd=tmp_path, url=url, timeout=30, poll=0.2)
    try:
        assert docs_images.looks_like_petstore(url)
        assert docs_images.port_in_use(url)
    finally:
        server.stop()
    assert server.proc.poll() is not None
    assert not docs_images.looks_like_petstore(url)


def test_busy_non_petstore_port_fails_naming_port_and_holder(tmp_path, monkeypatch, capsys):
    write_cases(tmp_path, [{'id': 'shot', 'image': 'topics/Page.Shot.1.png', 'path': '/x'}])
    write(tmp_path / 'pom.xml')
    write(tmp_path / 'scripts' / 'start-petstore-jetty.py')
    monkeypatch.setattr(docs_images, 'find_juneau_root', lambda *a, **k: tmp_path)
    monkeypatch.setattr(docs_images, 'port_holder', lambda port: 'PID 4242 (python3)')
    started = []
    monkeypatch.setattr(docs_images, 'Server', lambda *a, **k: started.append(a))
    listener = socket.socket()
    listener.bind(('127.0.0.1', 0))
    listener.listen(16)
    port = listener.getsockname()[1]
    try:
        rc = docs_images.main(['--docs-root', str(tmp_path), '--port', str(port)])
    finally:
        listener.close()
    assert rc == 1
    assert not started
    err = capsys.readouterr().err
    assert f'port {port} is in use by something that is not the petstore (PID 4242 (python3))' in err


def test_server_that_exits_without_starting_is_reported(tmp_path):
    script = write(tmp_path / 'fake.py', 'print("BUILD FAILURE")\nraise SystemExit(1)\n')
    with pytest.raises(docs_images.Problem, match='(?s)petstore did not start.*BUILD FAILURE'):
        docs_images.Server([sys.executable, str(script)], cwd=tmp_path, url=f'http://localhost:{free_port()}',
                           timeout=30, poll=0.2)


def test_fresh_server_only_cases_are_skipped_on_a_reused_server(capsys):
    cases = [{'id': 'a'}, {'id': 'b', 'freshServerOnly': True}]
    assert docs_images.select_for_server(cases, True) == cases
    assert capsys.readouterr().out == ''
    assert docs_images.select_for_server(cases, False) == [{'id': 'a'}]
    out = capsys.readouterr().out
    assert 'WARNING' in out and 'skipping b' in out


def test_server_passes_its_environment(tmp_path):
    port = free_port()
    seen = tmp_path / 'seen.txt'
    script = write(tmp_path / 'fake.py', FAKE_PETSTORE.replace(
        'http.server.HTTPServer(',
        f'open({str(seen)!r}, "w").write(os.environ.get("PETSTORE_FIXED_NOW", ""))\nhttp.server.HTTPServer(', 1)
        )
    server = docs_images.Server([sys.executable, str(script), str(port)], cwd=tmp_path, url=f'http://localhost:{port}',
                                timeout=30, poll=0.2, env={**os.environ, docs_images.FIXED_NOW_ENV: docs_images.FIXED_NOW})
    try:
        assert seen.read_text() == 'seed'
    finally:
        server.stop()


# ---------------------------------------------------------------------------------------------------------------------
# Terminal source: ANSI to SVG
# ---------------------------------------------------------------------------------------------------------------------

def cells_text(row):
    return ''.join(c[0] for c in row)


def test_ansi_plain_text_and_trailing_spaces():
    rows = docs_images.parse_ansi('hello   \n\nx')
    assert [cells_text(r) for r in rows] == ['hello', '', 'x']


def test_ansi_sgr_attributes():
    [row] = docs_images.parse_ansi('\x1b[1;4;31mA\x1b[0mB\x1b[2mC\x1b[7mD')
    assert [c[2] for c in row] == [
        ('#cd3131', None, True, False, True, False), docs_images.DEFAULT_STYLE,
        (None, None, False, True, False, False), (None, None, False, True, False, True)]


def test_ansi_256_and_truecolour_and_backgrounds():
    [row] = docs_images.parse_ansi('\x1b[38;5;196mA\x1b[38;2;1;2;255;48;5;232mB\x1b[44;91mC\x1b[39;49mD')
    assert row[0][2][0] == '#ff0000'
    assert row[1][2][:2] == ('#0102ff', '#080808')
    assert row[2][2][:2] == ('#f14c4c', '#2472c8')
    assert row[3][2] == docs_images.DEFAULT_STYLE


def test_ansi_unknown_and_malformed_sequences_are_dropped():
    [row] = docs_images.parse_ansi('a\x1b[2Jb\x1b[?25lc\x1b]0;title\x07d\x1b(Be\x1b[38;5mf\x1b[99mg\x00h\x1b')
    assert cells_text(row) == 'abcdefgh'


def test_ansi_wide_and_combining_characters():
    [row] = docs_images.parse_ansi('✅ ok é')
    assert [(c[0], c[1]) for c in row] == [('✅', 2), (' ', 1), ('o', 1), ('k', 1), (' ', 1), ('é', 1)]


def test_runs_merge_by_style():
    [row] = docs_images.parse_ansi('ab\x1b[1mcd\x1b[0mef')
    assert [(c, w, t) for c, w, t, _ in docs_images.row_runs(row)] == [(0, 2, 'ab'), (2, 2, 'cd'), (4, 2, 'ef')]


def test_svg_has_real_text_and_escapes():
    svg = docs_images.render_svg([docs_images.parse_ansi('a < b & "c"')], 'T <1>', 'D & "d"')
    assert '<title id="title">T &lt;1&gt;</title>' in svg
    assert '<desc id="desc">D &amp; &quot;d&quot;</desc>' in svg
    assert '>a</tspan>' in svg and '&lt;' in svg and '&amp;' in svg and '&quot;c&quot;' in svg
    assert svg.count('<text ') == 1


def test_svg_background_reverse_underline_and_words():
    svg = docs_images.render_svg([docs_images.parse_ansi('\x1b[44mab\x1b[0m  \x1b[4mcd ef\x1b[0m')], 't', 'd')
    assert 'fill="#2472c8"' in svg                      # the background rect
    assert svg.count('text-decoration="underline"') == 1  # an underlined run stays whole
    assert '>cd ef</tspan>' in svg


def test_svg_places_panes_side_by_side_and_is_deterministic():
    left, right = docs_images.parse_ansi('left'), docs_images.parse_ansi('right')
    svg = docs_images.render_svg([left, right], 't', 'd')
    assert svg == docs_images.render_svg([left, right], 't', 'd')
    assert svg.count('<text ') == 2 and f'fill="{docs_images.TERM_DIVIDER}"' in svg


def test_mask_text_and_trim_rows():
    assert docs_images.mask_text('took 1:23\nnow 0:05', [[r'\d+:\d\d', '0:00']]) == 'took 0:00\nnow 0:00'
    assert docs_images.trim_rows([[1], [], []]) == [[1]]
    assert docs_images.trim_rows([[], []]) == [[]]


def test_terminal_cases_load_with_defaults(tmp_path):
    write_cases(tmp_path, [{'id': 't', 'image': 'topics/T.Shot.1.svg', 'command': ['true']}],
                defaults={'columns': 80}, name='terminal-cases.json')
    cases, _ = docs_images.load_cases(tmp_path / 'scripts' / 'docs-images')
    assert cases[0]['source'] == 'terminal' and cases[0]['columns'] == 80


def test_expand_command_placeholders(tmp_path):
    out = docs_images.expand_command(['{juneau_terminal}', '{fixtures}/x.sh', 'plain'], tmp_path / 'j', tmp_path / 'd')
    assert out == [str(tmp_path / 'j' / docs_images.JUNEAU_TERMINAL_PY), str(tmp_path / 'd' / docs_images.FIXTURES_DIR / 'x.sh'),
                   'plain']


@pytest.mark.skipif(not __import__('shutil').which('tmux'), reason='needs tmux')
def test_capture_pane_waits_masks_and_cleans_up(tmp_path):
    pane = {'id': 'unit', 'columns': 40, 'rows': 5, 'timeout': 30, 'waitFor': 'ready',
            'command': ['bash', '-c', 'printf "\\033[1mbold\\033[0m ready %s\\n" "$(date +%s)"; sleep 600'],
            'mask': [[r'\d{6,}', 'N']]}
    text = docs_images.capture_pane(pane, tmp_path, tmp_path, tmp_path)
    assert text.startswith('\x1b[1mbold') and 'ready N' in text
    sock = docs_images.Path('/tmp') / f'tmux-{os.getuid()}' / f'docs-images-{os.getpid()}-unit'
    assert not sock.exists()
