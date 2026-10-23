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
Keeps the script docs complete and in step with the scripts (the same file is used, unchanged but for SKIP_DIRS, in
every repository that has script docs).

Every entry point under scripts/ (any *.sh, any *.py with a top-level ``if __name__ == "__main__":``, and any
extensionless file with a shell or Python shebang) needs a ``<name>.md`` next to it.  The checks:

1. an entry point has no doc;
2. a script doc (first line ``# <script file name>``) has no script beside it;
3. a script doc lacks one of the seven standard headings, or has them out of order;
4. a flag the script declares is missing from the doc's Options section.

``scripts/script-docs-exclude.txt`` lists exceptions (format described in that file).  The checker only reads files:
it never runs a script and never passes ``--help``.
"""

import ast
import fnmatch
import re
import warnings
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parents[1]
SKIP_DIRS = ('tests', 'vendor', 'node_modules', '__pycache__', 'fixtures')
EXCLUDE_FILE = 'script-docs-exclude.txt'
HEADINGS = ['When to use', 'Usage', 'Options', 'What it does', 'Output and exit codes', 'Gotchas', 'See also']
SCRIPT_EXTS = ('.py', '.sh')
SHEBANG_RE = re.compile(r'^#!.*\b(sh|bash|zsh|python[0-9.]*)\b')
FLAG_RE = re.compile(r'^--?[A-Za-z][\w-]*$')
SCRIPT_DOC_H1_RE = re.compile(r'^# (\S+\.(?:py|sh))\s*$')


# ---------------------------------------------------------------------------------------------------------------------
# Finding scripts and docs
# ---------------------------------------------------------------------------------------------------------------------

def _skipped(path, scripts_dir):
    return any(part in SKIP_DIRS for part in path.relative_to(scripts_dir).parts[:-1])


def _has_main_guard(source):
    """True when the module has a top-level ``if __name__ == "__main__":`` (either operand order, either quotes)."""
    try:
        tree = ast.parse(source)
    except SyntaxError:
        return False
    for node in tree.body:
        if not isinstance(node, ast.If) or not isinstance(node.test, ast.Compare):
            continue
        test = node.test
        if len(test.ops) != 1 or not isinstance(test.ops[0], ast.Eq):
            continue
        operands = [test.left, test.comparators[0]]
        names = [o.id for o in operands if isinstance(o, ast.Name)]
        consts = [o.value for o in operands if isinstance(o, ast.Constant)]
        if names == ['__name__'] and consts == ['__main__']:
            return True
    return False


def is_entry_point(path):
    """Whether a file under scripts/ is a runnable entry point (see the module docstring)."""
    if path.suffix == '.sh':
        return True
    if path.suffix == '.py':
        return _has_main_guard(path.read_text(encoding='utf-8'))
    if path.suffix == '' and path.is_file():
        try:
            first = path.read_text(encoding='utf-8').split('\n', 1)[0]
        except (UnicodeDecodeError, OSError):
            return False
        return bool(SHEBANG_RE.match(first))
    return False


def find_entry_points(scripts_dir):
    """All entry points under scripts_dir, as sorted paths."""
    return sorted(p for p in Path(scripts_dir).rglob('*')
                  if p.is_file() and not _skipped(p, Path(scripts_dir)) and is_entry_point(p))


def doc_for(script):
    """The doc that belongs beside a script: ``<stem>.md`` (``push.py`` -> ``push.md``)."""
    return script.with_name(script.stem + '.md') if script.suffix else script.with_name(script.name + '.md')


def find_script_docs(scripts_dir):
    """Markdown files whose first line is ``# <script file name>``."""
    docs = []
    for p in sorted(Path(scripts_dir).rglob('*.md')):
        if _skipped(p, Path(scripts_dir)):
            continue
        lines = p.read_text(encoding='utf-8').splitlines()
        if lines and SCRIPT_DOC_H1_RE.match(lines[0]):
            docs.append(p)
    return docs


# ---------------------------------------------------------------------------------------------------------------------
# The exclude file
# ---------------------------------------------------------------------------------------------------------------------

def parse_excludes(text):
    """
    Parse script-docs-exclude.txt.

    Returns:
        (skips, flag_skips, packages, errors): skips and flag_skips are lists of patterns relative to scripts/,
        packages is a list of directory names that need a README.md, errors lists the lines that are malformed.
    """
    skips, flag_skips, packages, errors = [], [], [], []
    for n, raw in enumerate(text.splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith('#'):
            continue
        body, _, reason = line.partition('#')
        body, reason = body.strip(), reason.strip()
        if body.startswith('packages:'):
            packages.extend(d.strip() for d in body[len('packages:'):].split(',') if d.strip())
            continue
        if not reason:
            errors.append(f"{EXCLUDE_FILE}:{n}: '{body}' has no '# reason'")
            continue
        if body.startswith('flags:'):
            flag_skips.append(body[len('flags:'):].strip())
        else:
            skips.append(body)
    return skips, flag_skips, packages, errors


def load_excludes(scripts_dir):
    path = Path(scripts_dir) / EXCLUDE_FILE
    return parse_excludes(path.read_text(encoding='utf-8') if path.is_file() else '')


def _matches(rel, patterns):
    return any(fnmatch.fnmatchcase(rel, p) for p in patterns)


def _rel(path, scripts_dir):
    return path.relative_to(scripts_dir).as_posix()


# ---------------------------------------------------------------------------------------------------------------------
# Flags
# ---------------------------------------------------------------------------------------------------------------------

def python_flags(source):
    """
    Flags and positionals a Python script declares, found by walking the AST (the script is never run).

    Returns:
        (items, uses_argparse): items is the set of ``-x``/``--long`` flags, positional names and sub-command names;
        uses_argparse says the module imports argparse.
    """
    tree = ast.parse(source)
    items, uses_argparse = set(), False
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            uses_argparse |= any(a.name == 'argparse' for a in node.names)
        elif isinstance(node, ast.ImportFrom):
            uses_argparse |= node.module == 'argparse'
        elif isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute):
            strings = [a.value for a in node.args if isinstance(a, ast.Constant) and isinstance(a.value, str)]
            if node.func.attr == 'add_argument':
                flags = [s for s in strings if s.startswith('-')]
                items.update(flags if flags else strings[:1])
            elif node.func.attr == 'add_parser' and strings:
                items.add(strings[0])
    return items, uses_argparse


def python_known_flags(source):
    """Every string constant in the source that looks like a flag (used for the reverse check)."""
    return {n.value for n in ast.walk(ast.parse(source))
            if isinstance(n, ast.Constant) and isinstance(n.value, str) and FLAG_RE.match(n.value)}


def sibling_sources(path, scripts_dir_of_path=None):
    """Source of the sibling modules (same directory) that a Python script imports; flags may be declared there."""
    sources = []
    for node in ast.walk(ast.parse(path.read_text(encoding='utf-8'))):
        names = []
        if isinstance(node, ast.Import):
            names = [a.name for a in node.names]
        elif isinstance(node, ast.ImportFrom) and node.module:
            names = [node.module]
        for name in names:
            candidate = path.with_name(name.split('.')[0] + '.py')
            if candidate.is_file() and candidate != path:
                sources.append(candidate.read_text(encoding='utf-8'))
    return sources


def _shell_tokens(text):
    out = set()
    for token in re.split(r'[\s|,\[\]()]+', text):
        token = token.split('=', 1)[0].strip('"\'')
        if FLAG_RE.match(token):
            out.add(token)
    return out


def shell_flags(source):
    """
    Flags a shell script handles: the alternatives of ``case`` patterns whose subject is an option variable, plus the
    ``-x`` tokens named on ``Usage:`` lines of the leading comment block.  Other ``case`` blocks (OS detection and the
    like) are not read.
    """
    flags, stack = set(), []
    option_case = re.compile(r'^\s*case\s+"?\$\{?(1|opt|arg|a|OPT|ARG|option)\}?"?\s+in\b')
    any_case = re.compile(r'^\s*case\s+.*\s+in\b')
    pattern = re.compile(r'^\s*([^)#]+)\)')
    for line in source.splitlines():
        if option_case.match(line):
            stack.append(True)
        elif any_case.match(line):
            stack.append(False)
        elif re.match(r'^\s*esac\b', line):
            if stack:
                stack.pop()
        elif stack and stack[-1]:
            m = pattern.match(line)
            if m:
                flags |= _shell_tokens(m.group(1))
    for line in source.splitlines():
        if line.strip() and not line.lstrip().startswith('#'):
            if not line.startswith('#!'):
                break
        if 'usage:' in line.lower():
            flags |= _shell_tokens(line.split(':', 1)[1])
    return flags


def declared_flags(path):
    """
    (flags, status) for a script: status is ``ok``, ``unverifiable`` (argparse imported but no add_argument found).
    """
    source = path.read_text(encoding='utf-8')
    if path.suffix == '.py':
        items, uses_argparse = python_flags(source)
        return items, ('unverifiable' if uses_argparse and not items else 'ok')
    return shell_flags(source), 'ok'


# ---------------------------------------------------------------------------------------------------------------------
# Docs
# ---------------------------------------------------------------------------------------------------------------------

def h2_headings(text):
    """The ``## `` headings of a doc, outside fenced code blocks, in order."""
    out, fence = [], False
    for line in text.splitlines():
        if line.lstrip().startswith(('```', '~~~')):
            fence = not fence
        elif not fence and line.startswith('## '):
            out.append(line[3:].strip())
    return out


def missing_headings(text):
    """The standard headings that are absent, or present but out of order."""
    present = h2_headings(text)
    missing, position = [], -1
    for heading in HEADINGS:
        if heading in present and present.index(heading) > position:
            position = present.index(heading)
        else:
            missing.append(heading)
    return missing


def options_section(text):
    """The text between ``## Options`` and the next H2 (empty when there is no such section)."""
    lines, out, inside, fence = text.splitlines(), [], False, False
    for line in lines:
        if line.lstrip().startswith(('```', '~~~')):
            fence = not fence
        if not fence and line.startswith('## '):
            inside = line[3:].strip() == 'Options'
            continue
        if inside:
            out.append(line)
    return '\n'.join(out)


def documented_items(section):
    """Backticked names in the first cell of each table row of an Options section."""
    items = set()
    for line in section.splitlines():
        if line.startswith('|'):
            cells = line.strip().strip('|').split('|')
            items.update(re.findall(r'`([^`]+)`', cells[0]))
    return items


def documented_flags(section):
    """Backticked names that look like flags, anywhere in the section (used by check 4)."""
    return set(re.findall(r'`([^`]+)`', section))


# ---------------------------------------------------------------------------------------------------------------------
# The four checks, as functions returning lists of offenders
# ---------------------------------------------------------------------------------------------------------------------

def check_missing_docs(scripts_dir):
    skips, _, _, _ = load_excludes(scripts_dir)
    return [_rel(s, scripts_dir) for s in find_entry_points(scripts_dir)
            if not _matches(_rel(s, scripts_dir), skips) and not doc_for(s).is_file()]


def check_orphan_docs(scripts_dir):
    bad = []
    for doc in find_script_docs(scripts_dir):
        named = doc.read_text(encoding='utf-8').splitlines()[0][2:].strip()
        if not (doc.parent / named).is_file() or doc_for(doc.parent / named) != doc:
            bad.append(f"{_rel(doc, scripts_dir)}: names '{named}' but no matching script is beside it")
    return bad


def check_headings(scripts_dir):
    _, _, packages, _ = load_excludes(scripts_dir)
    bad = []
    for doc in find_script_docs(scripts_dir):
        missing = missing_headings(doc.read_text(encoding='utf-8'))
        if missing:
            bad.append(f"{_rel(doc, scripts_dir)}: missing or out of order: {', '.join(missing)}")
    skips, _, _, _ = load_excludes(scripts_dir)
    for s in find_entry_points(scripts_dir):
        if _matches(_rel(s, scripts_dir), skips) and doc_for(s).is_file():
            missing = missing_headings(doc_for(s).read_text(encoding='utf-8'))
            if missing:
                bad.append(f"{_rel(doc_for(s), scripts_dir)}: missing or out of order: {', '.join(missing)}")
    return bad


def check_flags(scripts_dir):
    """(failures, warnings): undocumented flags and unverifiable scripts fail; stale Options rows only warn."""
    skips, flag_skips, _, _ = load_excludes(scripts_dir)
    failures, warns = [], []
    for script in find_entry_points(scripts_dir):
        rel = _rel(script, scripts_dir)
        doc = doc_for(script)
        if _matches(rel, skips) or _matches(rel, flag_skips) or not doc.is_file():
            continue
        flags, status = declared_flags(script)
        if status == 'unverifiable':
            failures.append(f"FLAGS-UNVERIFIABLE {rel}: uses argparse but no add_argument call was found; add "
                            f"'flags: {rel}  # reason' to {EXCLUDE_FILE}")
            continue
        section = options_section(doc.read_text(encoding='utf-8'))
        mentioned = documented_flags(section)
        for flag in sorted(flags - mentioned):
            failures.append(f"{_rel(doc, scripts_dir)}: '{flag}' is declared by {script.name} but is not in the "
                            f"Options section")
        if script.suffix == '.py':
            known = python_known_flags(script.read_text(encoding='utf-8'))
            for source in sibling_sources(script):
                known |= python_known_flags(source)
            for item in sorted(documented_items(section)):
                if FLAG_RE.match(item) and item not in known:
                    warns.append(f"{_rel(doc, scripts_dir)}: Options lists '{item}', which {script.name} does not "
                                 f"declare")
    return failures, warns


# ---------------------------------------------------------------------------------------------------------------------
# Tests against this repository
# ---------------------------------------------------------------------------------------------------------------------

def test_entry_points_have_docs():
    assert check_missing_docs(SCRIPTS_DIR) == [], 'entry points without a <name>.md beside them (add the doc or an exclude line)'


def test_script_docs_have_scripts():
    assert check_orphan_docs(SCRIPTS_DIR) == []


def test_script_docs_have_all_headings():
    assert check_headings(SCRIPTS_DIR) == []


def test_declared_flags_are_documented():
    failures, _ = check_flags(SCRIPTS_DIR)
    assert failures == []


def test_documented_flags_exist_in_code():
    """Warning only: an Options row for a flag the script no longer declares."""
    _, warns = check_flags(SCRIPTS_DIR)
    for message in warns:
        warnings.warn(message, UserWarning)


def test_exclude_file_is_well_formed():
    skips, flag_skips, packages, errors = load_excludes(SCRIPTS_DIR)
    assert errors == []
    files = [_rel(p, SCRIPTS_DIR) for p in SCRIPTS_DIR.rglob('*') if p.is_file()]
    stale = [p for p in skips + flag_skips if not any(fnmatch.fnmatchcase(f, p) for f in files)]
    assert stale == [], 'exclude patterns that match no file'


def test_listed_packages_have_readmes():
    _, _, packages, _ = load_excludes(SCRIPTS_DIR)
    bad = []
    for package in packages:
        directory = SCRIPTS_DIR / package
        if not (directory / 'README.md').is_file():
            bad.append(f"{package}: no README.md")
        elif not list(directory.glob('*.py')):
            bad.append(f"{package}: README.md in a directory with no .py module")
        else:
            lines = [l for l in (directory / 'README.md').read_text(encoding='utf-8').splitlines() if l.strip()]
            if len(lines) < 2 or not lines[0].startswith('# ') or lines[1].startswith('#'):
                bad.append(f"{package}: README.md needs an H1 followed by a paragraph")
    assert bad == []


# ---------------------------------------------------------------------------------------------------------------------
# Tests of the checker itself
# ---------------------------------------------------------------------------------------------------------------------

def _doc(options='None.', extra=''):
    body = '# x.py\n\nSummary.\n\n'
    for heading in HEADINGS:
        body += f'## {heading}\n\n' + (options if heading == 'Options' else 'None.') + '\n\n'
    return body + extra


def test_python_flags_with_aliases_groups_and_subparsers():
    source = (
        "import argparse\n"
        "p = argparse.ArgumentParser()\n"
        "p.add_argument('-v', '--verbose', action='store_true')\n"
        "p.add_argument('message', nargs='?')\n"
        "g = p.add_mutually_exclusive_group()\n"
        "g.add_argument('--a')\n"
        "s = p.add_subparsers().add_parser('build')\n"
        "s.add_argument('--target')\n")
    items, uses = python_flags(source)
    assert uses
    assert items == {'-v', '--verbose', 'message', '--a', 'build', '--target'}


def test_python_dynamic_flags_are_unverifiable(tmp_path):
    script = tmp_path / 's.py'
    script.write_text("import argparse\np = argparse.ArgumentParser()\nfor f in ['--a']:\n    p.add_argument(f)\n"
                      "if __name__ == '__main__':\n    pass\n")
    assert declared_flags(script) == (set(), 'unverifiable')


def test_python_without_argparse_has_no_checkable_flags(tmp_path):
    script = tmp_path / 's.py'
    script.write_text("import sys\nif '--verbose' in sys.argv:\n    pass\n")
    assert declared_flags(script) == (set(), 'ok')


def test_main_guard_detection():
    assert _has_main_guard("if __name__ == '__main__':\n    pass\n")
    assert _has_main_guard('if "__main__" == __name__:\n    pass\n')
    assert not _has_main_guard("def f():\n    if __name__ == '__main__':\n        pass\n")
    assert not _has_main_guard("x = 1\n")


def test_shell_case_loop_flags():
    source = ('#!/bin/bash\n# Usage: run.sh [-q] [--fast]\n'
              'while [ $# -gt 0 ]; do\n  case "$1" in\n    -n|--dry-run) D=1;;\n    --out=*) O=1;;\n    *) break;;\n'
              '  esac\n  shift\ndone\n')
    assert shell_flags(source) == {'-q', '--fast', '-n', '--dry-run', '--out'}


def test_shell_os_case_is_not_misread():
    source = '#!/bin/bash\ncase "$(uname)" in\n  Darwin) X=1;;\n  Linux) X=2;;\nesac\n'
    assert shell_flags(source) == set()


def test_shell_usage_fallback():
    assert shell_flags('#!/bin/sh\n# Usage: tool [--force] FILE\necho hi\n') == {'--force'}


def test_exclude_file_parsing():
    text = ('# comment\n\nfoo/*.sh  # fixture\nflags: bar.py  # dynamic flags\npackages: a, b\nbad.py\n')
    skips, flag_skips, packages, errors = parse_excludes(text)
    assert skips == ['foo/*.sh']
    assert flag_skips == ['bar.py']
    assert packages == ['a', 'b']
    assert len(errors) == 1 and 'bad.py' in errors[0]


def test_heading_checks():
    assert missing_headings(_doc()) == []
    assert missing_headings(_doc().replace('## Gotchas', '## Notes')) == ['Gotchas']
    swapped = _doc().replace('## Usage', '## TMP').replace('## Options', '## Usage').replace('## TMP', '## Options')
    assert missing_headings(swapped) != []


def test_options_section_and_documented_items():
    text = _doc(options='| Option | Argument |\n|---|---|\n| `--a`, `-a` | none |\n| `name` | text |')
    assert documented_items(options_section(text)) == {'--a', '-a', 'name'}


def test_checks_on_a_scratch_tree(tmp_path):
    (tmp_path / 'tests').mkdir()
    (tmp_path / 'fixtures').mkdir()
    (tmp_path / 'fixtures' / 'f.sh').write_text('#!/bin/sh\n')
    (tmp_path / 'a.py').write_text("import argparse\np = argparse.ArgumentParser()\np.add_argument('--go')\n"
                                   "if __name__ == '__main__':\n    pass\n")
    (tmp_path / 'lib.py').write_text('X = 1\n')
    (tmp_path / 'b.sh').write_text('#!/bin/sh\n')
    assert check_missing_docs(tmp_path) == ['a.py', 'b.sh']
    (tmp_path / 'a.md').write_text(_doc().replace('# x.py', '# a.py'))
    (tmp_path / 'b.md').write_text(_doc().replace('# x.py', '# b.sh'))
    assert check_missing_docs(tmp_path) == []
    assert check_orphan_docs(tmp_path) == []
    failures, _ = check_flags(tmp_path)
    assert failures and "'--go'" in failures[0]
    (tmp_path / 'a.md').write_text(_doc(options='| `--go` | none |').replace('# x.py', '# a.py'))
    assert check_flags(tmp_path) == ([], [])
    (tmp_path / 'c.md').write_text(_doc().replace('# x.py', '# c.py'))
    assert len(check_orphan_docs(tmp_path)) == 1
    (tmp_path / 'a.md').write_text(_doc().replace('# x.py', '# a.py').replace('## See also', '## Misc'))
    assert len(check_headings(tmp_path)) == 1
