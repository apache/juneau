# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
"""
Script docs checker: every runnable script under scripts/ has a <name>.md beside it, with the seven standard sections,
and every flag the script declares is named in that doc's Options section.

Source copy: this file in apache/juneau (scripts/tests/test_script_docs.py), version 1.  Other repositories carry the
same file; the only intended difference is SKIP_DIRS.  Change the shared logic everywhere, not in one copy.

Read-only and hermetic: it parses source with ast and text matching, never runs or imports a script, and writes nothing.
Exceptions live in scripts/script-docs-exclude.txt (format in scripts/tests/README.md).
"""

from __future__ import annotations

import ast
import fnmatch
import re
import warnings
from pathlib import Path

import pytest

# scripts/ is resolved from this file, so the result does not depend on the current directory.
SCRIPTS_DIR = Path(__file__).resolve().parents[1]
EXCLUDE_FILE = "script-docs-exclude.txt"

# Directory names never scanned for entry points or docs.
SKIP_DIRS = ("tests", "vendor", "node_modules", "__pycache__", "fixtures")

REQUIRED_HEADINGS = ["When to use", "Usage", "Options", "What it does", "Output and exit codes", "Gotchas", "See also"]

SCRIPT_EXTENSIONS = (".py", ".sh")
SHEBANG = re.compile(r"^#!.*\b(sh|bash|zsh|python[0-9.]*)\b")
DOC_TITLE = re.compile(r"^# (?P<name>[\w.\-]+\.(?:py|sh|bash|zsh))\s*$")
HELP_FLAGS = ("-h", "--help")

# Option variables a shell `case` may switch on (as $1, $opt and so on); any other case subject is not an option parser.
SHELL_OPTION_SUBJECTS = ("1", "opt", "arg", "a", "option", "flag", "key", "OPT", "ARG")
SHELL_CASE = re.compile(r"^\s*case\s+\"?\$\{?(?P<subject>\w+)\}?\"?\s+in\b")
SHELL_ESAC = re.compile(r"^\s*esac\b")
SHELL_PATTERN = re.compile(r"^\s*(?P<alts>-[^)\s]*(?:\s*\|\s*-[^)\s]*)*)\)")
SHELL_FLAG = re.compile(r"^(--?[A-Za-z0-9][\w-]*)")
USAGE_FLAG = re.compile(r"(?<![\w-])(--?[A-Za-z][\w-]*)")


# --------------------------------------------------------------------------------------------------------------------
# Discovery
# --------------------------------------------------------------------------------------------------------------------

def _skipped(relative: Path) -> bool:
    """True for a path inside a skipped or hidden directory."""
    return any(part in SKIP_DIRS or part.startswith(".") for part in relative.parts[:-1])


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def has_main_guard(source: str) -> bool:
    """True when the module has a top-level `if __name__ == "__main__":` (either operand order, either quote)."""
    try:
        tree = ast.parse(source)
    except SyntaxError:
        return re.search(r"^if\s+__name__\s*==\s*['\"]__main__['\"]", source, re.MULTILINE) is not None
    for node in tree.body:
        if not isinstance(node, ast.If) or not isinstance(node.test, ast.Compare):
            continue
        test = node.test
        if len(test.ops) != 1 or not isinstance(test.ops[0], ast.Eq):
            continue
        sides = [test.left, *test.comparators]
        names = [s for s in sides if isinstance(s, ast.Name) and s.id == "__name__"]
        consts = [s for s in sides if isinstance(s, ast.Constant) and s.value == "__main__"]
        if names and consts:
            return True
    return False


def is_entry_point(path: Path) -> bool:
    """A .py file with a __main__ guard, a .sh file, or an extensionless file with a shell or Python shebang."""
    if path.suffix == ".py":
        return has_main_guard(_read(path))
    if path.suffix == ".sh":
        return True
    if path.suffix == "":
        try:
            first_line = _read(path).splitlines()[0]
        except (IndexError, OSError):
            return False
        return SHEBANG.match(first_line) is not None
    return False


def find_entry_points(scripts_dir: Path) -> list[Path]:
    """Every entry point under scripts_dir, as sorted absolute paths."""
    found = []
    for path in sorted(scripts_dir.rglob("*")):
        if path.is_file() and not _skipped(path.relative_to(scripts_dir)) and is_entry_point(path):
            found.append(path)
    return found


def doc_path(script: Path) -> Path:
    """The doc beside a script: <stem>.md in the same directory."""
    return script.with_name(script.stem + ".md")


def find_script_docs(scripts_dir: Path) -> list[Path]:
    """Every .md under scripts_dir outside the skipped directories."""
    return [p for p in sorted(scripts_dir.rglob("*.md")) if not _skipped(p.relative_to(scripts_dir))]


# --------------------------------------------------------------------------------------------------------------------
# Exclude file
# --------------------------------------------------------------------------------------------------------------------

class Excludes:
    """The parsed exclude file: whole-file excludes, flag-check excludes, package directories and format errors."""

    def __init__(self):
        self.skip = []        # (pattern, reason)
        self.flags = []       # (pattern, reason)
        self.packages = []    # (directory, reason)
        self.errors = []      # human-readable format problems


def parse_excludes(text: str) -> Excludes:
    """Parse the exclude file text.  A line is `[flags:|packages:] <pattern> # <reason>`; the reason is mandatory."""
    result = Excludes()
    for number, raw in enumerate(text.splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        body, hash_mark, reason = line.partition(" #")
        body, reason = body.strip(), reason.strip()
        if not hash_mark or not reason:
            result.errors.append(f"line {number}: no '# reason' after '{body}'")
            continue
        if body.startswith("flags:"):
            result.flags.append((body[len("flags:"):].strip(), reason))
        elif body.startswith("packages:"):
            for name in body[len("packages:"):].split(","):
                if name.strip():
                    result.packages.append((name.strip().rstrip("/"), reason))
        else:
            result.skip.append((body, reason))
    return result


def load_excludes(scripts_dir: Path) -> Excludes:
    path = scripts_dir / EXCLUDE_FILE
    return parse_excludes(_read(path)) if path.is_file() else Excludes()


def _matches(relative: str, patterns: list[tuple[str, str]]) -> bool:
    return any(fnmatch.fnmatchcase(relative, pattern) for pattern, _ in patterns)


# --------------------------------------------------------------------------------------------------------------------
# Flag extraction
# --------------------------------------------------------------------------------------------------------------------

class Declared:
    """What a script declares: flags, positionals, sub-command names, and whether it is argparse-based at all."""

    def __init__(self):
        self.flags = []
        self.positionals = []
        self.subcommands = []
        self.uses_argparse = False
        self.add_calls = 0


def python_declared(source: str) -> Declared:
    """Flags from add_argument calls on any receiver (parser, group, sub-parser) and sub-commands from add_parser."""
    declared = Declared()
    try:
        tree = ast.parse(source)
    except SyntaxError:
        return declared
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            declared.uses_argparse |= any(alias.name == "argparse" for alias in node.names)
        elif isinstance(node, ast.ImportFrom):
            declared.uses_argparse |= node.module == "argparse"
        elif isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute):
            strings = [a.value for a in node.args if isinstance(a, ast.Constant) and isinstance(a.value, str)]
            if node.func.attr == "add_argument":
                declared.add_calls += 1
                for value in strings:
                    if value.startswith("-"):
                        if value not in HELP_FLAGS and value not in declared.flags:
                            declared.flags.append(value)
                    elif value not in declared.positionals:
                        declared.positionals.append(value)
            elif node.func.attr == "add_parser" and strings:
                declared.add_calls += 1
                if strings[0] not in declared.subcommands:
                    declared.subcommands.append(strings[0])
    return declared


def shell_flags(source: str) -> list[str]:
    """
    Flags of a shell script.  Option patterns (`--long|-s)`) inside a `case` over an option variable; when there are
    none, the flags named on `Usage:` lines of the leading comment block.
    """
    flags = []
    in_option_case = False
    for line in source.splitlines():
        case = SHELL_CASE.match(line)
        if case:
            in_option_case = case.group("subject") in SHELL_OPTION_SUBJECTS
            continue
        if SHELL_ESAC.match(line):
            in_option_case = False
            continue
        if in_option_case:
            pattern = SHELL_PATTERN.match(line)
            if pattern:
                for alt in pattern.group("alts").split("|"):
                    match = SHELL_FLAG.match(alt.strip())
                    if match and match.group(1) not in HELP_FLAGS and match.group(1) not in flags:
                        flags.append(match.group(1))
    if flags:
        return flags
    for line in source.splitlines():
        if line.strip() and not line.startswith("#"):
            break
        if line.startswith("#") and "Usage:" in line:
            for token in USAGE_FLAG.findall(line.split("Usage:", 1)[1]):
                if token not in HELP_FLAGS and token not in flags:
                    flags.append(token)
    return flags


# --------------------------------------------------------------------------------------------------------------------
# Doc parsing
# --------------------------------------------------------------------------------------------------------------------

def h2_sections(text: str) -> list[tuple[str, str]]:
    """(heading, body) for each `## ` heading outside code fences, in order."""
    sections = []
    fenced = False
    for line in text.splitlines():
        if line.lstrip().startswith("```"):
            fenced = not fenced
        if not fenced and line.startswith("## "):
            sections.append((line[3:].strip(), []))
        elif sections:
            sections[-1][1].append(line)
    return [(heading, "\n".join(body)) for heading, body in sections]


def documented_tokens(options_text: str) -> set[str]:
    """Every word inside a backtick span in the Options section, split on whitespace and commas."""
    tokens = set()
    for span in re.findall(r"`([^`]+)`", options_text):
        for word in re.split(r"[\s,|/]+", span):
            if word:
                tokens.add(word)
                tokens.add(word.split("=", 1)[0])
    return tokens


def options_section(text: str) -> str | None:
    for heading, body in h2_sections(text):
        if heading == "Options":
            return body
    return None


# --------------------------------------------------------------------------------------------------------------------
# The checks.  Each returns a list of failure strings (empty means pass).
# --------------------------------------------------------------------------------------------------------------------

def rel(scripts_dir: Path, path: Path) -> str:
    return path.relative_to(scripts_dir).as_posix()


def check_missing_docs(scripts_dir: Path, excludes: Excludes) -> list[str]:
    """Check 1: an entry point has no <name>.md beside it."""
    failures = []
    for script in find_entry_points(scripts_dir):
        if _matches(rel(scripts_dir, script), excludes.skip):
            continue
        if not doc_path(script).is_file():
            failures.append(f"{rel(scripts_dir, script)}: no doc; create {rel(scripts_dir, doc_path(script))}")
    return failures


def check_orphan_docs(scripts_dir: Path) -> list[str]:
    """Check 2: a script doc (first line `# <file name>.py`) whose script is missing or named differently."""
    failures = []
    for doc in find_script_docs(scripts_dir):
        lines = _read(doc).splitlines()
        title = DOC_TITLE.match(lines[0]) if lines else None
        if not title:
            continue
        name = title.group("name")
        if not (doc.parent / name).is_file():
            failures.append(f"{rel(scripts_dir, doc)}: its title names {name}, but there is no such script beside it")
        elif Path(name).stem != doc.stem:
            failures.append(f"{rel(scripts_dir, doc)}: named for {doc.stem} but its title names {name}")
    return failures


def check_headings(scripts_dir: Path, excludes: Excludes) -> list[str]:
    """Check 3: every script doc has exactly the seven H2 sections, in order."""
    failures = []
    docs = [doc_path(s) for s in find_entry_points(scripts_dir)]
    docs += [d for d in find_script_docs(scripts_dir) if DOC_TITLE.match((_read(d).splitlines() or [""])[0])]
    for doc in sorted(set(docs)):
        if not doc.is_file():
            continue
        text = _read(doc)
        if not text.startswith("# "):
            failures.append(f"{rel(scripts_dir, doc)}: first line must be the H1 '# <script file name>'")
        headings = [heading for heading, _ in h2_sections(text)]
        if headings != REQUIRED_HEADINGS:
            missing = [h for h in REQUIRED_HEADINGS if h not in headings]
            detail = f"missing {missing}" if missing else f"found {headings}, expected this order"
            failures.append(f"{rel(scripts_dir, doc)}: headings are not the required seven ({detail})")
    return failures


def declared_for(script: Path) -> Declared | None:
    """Declared flags for a script, or None when its flags cannot be determined without running it."""
    source = _read(script)
    if script.suffix == ".py":
        declared = python_declared(source)
        if declared.uses_argparse and declared.add_calls == 0:
            return None
        return declared
    declared = Declared()
    declared.flags = shell_flags(source)
    return declared


def check_flags(scripts_dir: Path, excludes: Excludes) -> tuple[list[str], list[str]]:
    """Check 4: every declared flag is in backticks in the Options section.  Returns (failures, reverse warnings)."""
    failures, warns = [], []
    for script in find_entry_points(scripts_dir):
        name = rel(scripts_dir, script)
        if _matches(name, excludes.skip) or _matches(name, excludes.flags):
            continue
        doc = doc_path(script)
        if not doc.is_file():
            continue   # check 1 reports it
        declared = declared_for(script)
        if declared is None:
            failures.append(f"{name}: FLAGS-UNVERIFIABLE (argparse without literal add_argument calls); document it by "
                            f"hand and add 'flags: {name}  # <reason>' to {EXCLUDE_FILE}")
            continue
        options = options_section(_read(doc))
        if options is None:
            continue   # check 3 reports it
        tokens = documented_tokens(options)
        for flag in [*declared.flags, *declared.subcommands]:
            if flag not in tokens:
                failures.append(f"{name}: flag {flag} is not in backticks in the Options section of {doc.name}")
        for positional in declared.positionals:
            if positional not in tokens:
                warns.append(f"{name}: positional {positional} is not in backticks in the Options section of {doc.name}")
        if declared.flags or declared.subcommands:
            known = {*declared.flags, *declared.subcommands, *HELP_FLAGS}
            for token in sorted(t for t in tokens if t.startswith("-") and t not in known):
                warns.append(f"{name}: the Options section of {doc.name} lists {token}, which the script does not declare")
    return failures, warns


def check_excludes(scripts_dir: Path, excludes: Excludes) -> list[str]:
    """The exclude file: every line has a reason, and no pattern is stale."""
    failures = list(excludes.errors)
    entries = [rel(scripts_dir, s) for s in find_entry_points(scripts_dir)]
    for kind, patterns in (("exclude", excludes.skip), ("flags:", excludes.flags)):
        for pattern, _ in patterns:
            if not any(fnmatch.fnmatchcase(entry, pattern) for entry in entries):
                failures.append(f"stale {kind} pattern '{pattern}' matches no entry point")
    return failures


def check_packages(scripts_dir: Path, excludes: Excludes) -> list[str]:
    """Each directory listed with `packages:` exists, holds a .py module, and has a README.md (H1 plus a paragraph)."""
    failures = []
    for name, _ in excludes.packages:
        directory = scripts_dir / name
        if not directory.is_dir():
            failures.append(f"package {name}: no such directory")
        elif not list(directory.glob("*.py")):
            failures.append(f"package {name}: no .py module in it")
        elif not (directory / "README.md").is_file():
            failures.append(f"package {name}: no README.md")
        else:
            lines = [ln for ln in _read(directory / "README.md").splitlines() if ln.strip()]
            if not lines or not lines[0].startswith("# ") or len(lines) < 2 or lines[1].startswith("#"):
                failures.append(f"package {name}: README.md needs an H1 followed by at least one paragraph")
    return failures


def _fail(failures: list[str]) -> None:
    if failures:
        pytest.fail("\n" + "\n".join(f"  {f}" for f in failures), pytrace=False)


# --------------------------------------------------------------------------------------------------------------------
# The checks against this repository
# --------------------------------------------------------------------------------------------------------------------

def test_every_entry_point_has_a_doc():
    _fail(check_missing_docs(SCRIPTS_DIR, load_excludes(SCRIPTS_DIR)))


def test_every_script_doc_has_a_script():
    _fail(check_orphan_docs(SCRIPTS_DIR))


def test_every_script_doc_has_the_seven_headings():
    _fail(check_headings(SCRIPTS_DIR, load_excludes(SCRIPTS_DIR)))


def test_every_declared_flag_is_documented():
    failures, warns = check_flags(SCRIPTS_DIR, load_excludes(SCRIPTS_DIR))
    for message in warns:
        warnings.warn(message, stacklevel=1)
    _fail(failures)


def test_exclude_file_is_valid_and_not_stale():
    _fail(check_excludes(SCRIPTS_DIR, load_excludes(SCRIPTS_DIR)))


def test_listed_packages_have_a_readme():
    _fail(check_packages(SCRIPTS_DIR, load_excludes(SCRIPTS_DIR)))


# --------------------------------------------------------------------------------------------------------------------
# Unit tests for the parsers and the checks, on inline fixtures
# --------------------------------------------------------------------------------------------------------------------

GOOD_DOC = "# tool.py\n\nSummary.\n\n" + "".join(f"## {h}\n\nNone.\n\n" for h in REQUIRED_HEADINGS)
GOOD_DOC = GOOD_DOC.replace("## Options\n\nNone.", "## Options\n\n| Option |\n|---|\n| `--alpha`, `-a` |\n| `--beta` |")

ARGPARSE_SCRIPT = '''
import argparse
p = argparse.ArgumentParser()
p.add_argument("--alpha", "-a")
g = p.add_mutually_exclusive_group()
g.add_argument("--beta", action="store_true")
sub = p.add_subparsers()
run = sub.add_parser("run")
run.add_argument("--gamma")
p.add_argument("target")
if __name__ == "__main__":
    pass
'''


def _scripts(tmp_path: Path, files: dict[str, str]) -> Path:
    scripts = tmp_path / "scripts"
    for name, content in files.items():
        target = scripts / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
    return scripts


def test_python_flags_with_aliases_groups_and_subparsers():
    declared = python_declared(ARGPARSE_SCRIPT)
    assert declared.flags == ["--alpha", "-a", "--beta", "--gamma"]
    assert declared.subcommands == ["run"]
    assert declared.positionals == ["target"]
    assert declared.uses_argparse


def test_python_help_flags_are_ignored():
    assert python_declared('import argparse\np.add_argument("--help", "-h")').flags == []


def test_python_dynamic_flags_are_unverifiable():
    source = "import argparse\nfor flag in FLAGS:\n    p.add_argument(flag)\n"
    declared = python_declared(source)
    assert declared.uses_argparse and declared.flags == []
    assert declared.add_calls == 1 and python_declared("import argparse\nparser = None\n").add_calls == 0


def test_main_guard_detection():
    assert has_main_guard('if __name__ == "__main__":\n    pass\n')
    assert has_main_guard("if '__main__' == __name__:\n    pass\n")
    assert not has_main_guard("def main():\n    if __name__ == '__main__':\n        pass\n")
    assert not has_main_guard("def helper():\n    pass\n")


def test_shell_flags_from_a_case_loop():
    source = '#!/bin/bash\nwhile [ $# -gt 0 ]; do\n  case "$1" in\n    --out|-o) OUT=$2; shift;;\n' \
             '    --verbose) V=1;;\n    -h|--help) usage;;\n    *) break;;\n  esac\ndone\n'
    assert shell_flags(source) == ["--out", "-o", "--verbose"]


def test_shell_case_over_something_else_is_not_an_option_parser():
    source = '#!/bin/bash\ncase "$(uname)" in\n  -x) echo x;;\nesac\ncase "$OSTYPE" in\n  --odd) :;;\nesac\n'
    assert shell_flags(source) == []


def test_shell_usage_comment_fallback():
    assert shell_flags("#!/bin/sh\n# Usage: tool.sh [--dry-run] [-n NAME]\nrun\n") == ["--dry-run", "-n"]
    assert shell_flags("#!/bin/sh\nrun\n# Usage: tool.sh --late\n") == []


def test_entry_points_cover_py_sh_and_shebang_files(tmp_path):
    scripts = _scripts(tmp_path, {
        "a.py": "if __name__ == '__main__':\n    pass\n", "lib.py": "def f():\n    pass\n", "b.sh": "echo hi\n",
        "c": "#!/usr/bin/env bash\necho hi\n", "d": "plain text\n", "tests/e.py": "if __name__ == '__main__':\n    pass\n",
        "vendor/f.sh": "echo\n", "fixtures/g.sh": "echo\n", "sub/h.py": "if __name__ == '__main__':\n    pass\n"})
    assert [rel(scripts, p) for p in find_entry_points(scripts)] == ["a.py", "b.sh", "c", "sub/h.py"]


def test_exclude_parser_prefixes_and_reasons():
    parsed = parse_excludes("# c\n\nfoo/*.sh  # fixture\nflags: bar.py  # dynamic\npackages: a, b  # libs\nbad.py\nalso.py #\n")
    assert parsed.skip == [("foo/*.sh", "fixture")]
    assert parsed.flags == [("bar.py", "dynamic")]
    assert parsed.packages == [("a", "libs"), ("b", "libs")]
    assert len(parsed.errors) == 2


# Each check is broken once, to prove it fails for the thing it guards.

def test_check1_fails_without_a_doc(tmp_path):
    scripts = _scripts(tmp_path, {"tool.py": "if __name__ == '__main__':\n    pass\n"})
    assert check_missing_docs(scripts, Excludes()) != []
    assert check_missing_docs(scripts, parse_excludes("tool.py  # test\n")) == []


def test_check2_fails_for_a_doc_without_a_script(tmp_path):
    scripts = _scripts(tmp_path, {"tool.md": GOOD_DOC, "README.md": "# Notes\n\nProse.\n"})
    assert len(check_orphan_docs(scripts)) == 1
    (scripts / "tool.py").write_text("x = 1\n")
    assert check_orphan_docs(scripts) == []


def test_check3_fails_for_a_missing_heading(tmp_path):
    scripts = _scripts(tmp_path, {"tool.py": "if __name__ == '__main__':\n    pass\n", "tool.md": GOOD_DOC})
    assert check_headings(scripts, Excludes()) == []
    (scripts / "tool.md").write_text(GOOD_DOC.replace("## Gotchas\n\nNone.\n\n", ""))
    assert len(check_headings(scripts, Excludes())) == 1


def test_check3_ignores_headings_inside_code_fences(tmp_path):
    fenced = GOOD_DOC.replace("## Usage\n\nNone.", "## Usage\n\n```\n## Not a heading\n```")
    scripts = _scripts(tmp_path, {"tool.py": "if __name__ == '__main__':\n    pass\n", "tool.md": fenced})
    assert check_headings(scripts, Excludes()) == []


def test_check4_fails_for_an_undocumented_flag(tmp_path):
    scripts = _scripts(tmp_path, {"tool.py": ARGPARSE_SCRIPT, "tool.md": GOOD_DOC})
    failures, _ = check_flags(scripts, Excludes())
    assert any("--gamma" in f for f in failures) and any("run" in f for f in failures)
    assert not any("--alpha" in f or "-a" in f.split(":")[-1] for f in failures)


def test_check4_unverifiable_flags_need_an_exclude(tmp_path):
    scripts = _scripts(tmp_path, {"tool.py": "import argparse\nif __name__ == '__main__':\n    pass\n", "tool.md": GOOD_DOC})
    failures, _ = check_flags(scripts, Excludes())
    assert "FLAGS-UNVERIFIABLE" in failures[0]
    assert check_flags(scripts, parse_excludes("flags: tool.py  # built by a helper\n"))[0] == []


def test_check4_reverse_check_only_warns(tmp_path):
    source = 'import argparse\np.add_argument("--alpha")\nif __name__ == "__main__":\n    pass\n'
    scripts = _scripts(tmp_path, {"tool.py": source, "tool.md": GOOD_DOC})
    failures, warns = check_flags(scripts, Excludes())
    assert failures == [] and any("--beta" in w for w in warns)


def test_stale_exclude_patterns_fail(tmp_path):
    scripts = _scripts(tmp_path, {"tool.py": "if __name__ == '__main__':\n    pass\n"})
    assert check_excludes(scripts, parse_excludes("gone.py  # was removed\n")) != []
    assert check_excludes(scripts, parse_excludes("tool.py  # kept\nflags: tool.py  # kept\n")) == []
    assert check_excludes(scripts, parse_excludes("tool.py\n")) != []


def test_packages_need_a_readme(tmp_path):
    scripts = _scripts(tmp_path, {"lib/mod.py": "x = 1\n"})
    excludes = parse_excludes("packages: lib  # library\n")
    assert check_packages(scripts, excludes) != []
    (scripts / "lib" / "README.md").write_text("# lib\n\nWhat it holds.\n")
    assert check_packages(scripts, excludes) == []
