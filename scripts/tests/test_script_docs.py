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
Keeps scripts/ documented: every entry point has a <name>.md beside it, every script doc has its script, every script
doc has the seven fixed headings, and every flag a script declares appears in its doc's Options section.

Read-only and hermetic: it reads files, never runs a script and writes nothing.  Flags are found by parsing the source
(ast for Python, the option `case` for shell), never by running --help.  Exclusions and the library packages that need a
README.md are listed in scripts/script-docs-exclude.txt.

One copy per repository, the same shape in all of them; only SKIP_DIRS may differ.
"""
import ast
import fnmatch
import re
import warnings
from pathlib import Path

import pytest

SCRIPTS = Path(__file__).resolve().parents[1]
SKIP_DIRS = ("tests", "vendor", "node_modules", "__pycache__", "fixtures")
EXCLUDE_FILE = "script-docs-exclude.txt"
HEADINGS = ["When to use", "Usage", "Options", "What it does", "Output and exit codes", "Gotchas", "See also"]
SHEBANG = re.compile(r"^#!.*\b(sh|bash|zsh|python3?)\b")
H1_SCRIPT = re.compile(r"^# (\S+\.(?:py|sh))\s*$")


# ---------------------------------------------------------------------------------------------------------------------
# Finding files

def candidate_files(root):
    """Every file under root outside the skipped and dot-directories, as paths."""
    out = []
    for p in sorted(root.rglob("*")):
        rel = p.relative_to(root)
        if p.is_file() and not any(part in SKIP_DIRS or part.startswith(".") for part in rel.parts[:-1]):
            out.append(p)
    return out


def _first_line(path):
    try:
        with open(path, "rb") as f:
            return f.readline(200).decode("utf-8", "replace")
    except OSError:
        return ""


def has_main_guard(source):
    """True when the source has a top-level `if __name__ == "__main__":` (either operand order)."""
    try:
        tree = ast.parse(source)
    except SyntaxError:
        return False
    for node in tree.body:
        if isinstance(node, ast.If) and isinstance(node.test, ast.Compare) and len(node.test.ops) == 1:
            sides = [node.test.left, *node.test.comparators]
            names = [s.id for s in sides if isinstance(s, ast.Name)]
            consts = [s.value for s in sides if isinstance(s, ast.Constant)]
            if names == ["__name__"] and consts == ["__main__"]:
                return True
    return False


def script_kind(path):
    """'py', 'sh' or None: whether path is an entry point (a runnable script)."""
    if path.suffix == ".sh":
        return "sh"
    if path.suffix == ".py":
        return "py" if has_main_guard(path.read_text(encoding="utf-8", errors="replace")) else None
    if path.suffix == "":
        m = SHEBANG.match(_first_line(path))
        if m:
            return "py" if m.group(1).startswith("python") else "sh"
    return None


def doc_for(path):
    """The doc beside a script: push.py -> push.md, pd-close -> pd-close.md."""
    return path.with_suffix(".md") if path.suffix else path.with_name(path.name + ".md")


# ---------------------------------------------------------------------------------------------------------------------
# The exclude file

class Excludes:
    def __init__(self):
        self.paths = []      # (pattern, reason)
        self.flags = []      # (pattern, reason)
        self.packages = []   # directory names relative to scripts/
        self.errors = []


def parse_excludes(text):
    ex = Excludes()
    for n, raw in enumerate(text.splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        m = re.match(r"^(.*?)\s+#\s*(.*)$", line)
        body, reason = m.groups() if m else (line, "")
        body, reason = body.strip(), reason.strip()
        if body.startswith("packages:"):
            ex.packages += [p.strip() for p in body[len("packages:"):].split(",") if p.strip()]
        elif not reason:
            ex.errors.append(f"{EXCLUDE_FILE}:{n}: '{body}' has no '# reason'")
        elif body.startswith("flags:"):
            ex.flags.append((body[len("flags:"):].strip(), reason))
        else:
            ex.paths.append((body, reason))
    return ex


def load_excludes(root):
    path = root / EXCLUDE_FILE
    return parse_excludes(path.read_text(encoding="utf-8")) if path.exists() else Excludes()


def matches(rel, pattern):
    return fnmatch.fnmatchcase(rel, pattern)


# ---------------------------------------------------------------------------------------------------------------------
# Finding flags without running anything

def py_flags(source):
    """(flags, subcommands, uses_argparse) from the source: string constants starting with '-' in add_argument calls, the
    names given to add_parser, and whether argparse is imported at all."""
    tree = ast.parse(source)
    uses = False
    flags, subs = set(), set()
    for node in ast.walk(tree):
        if isinstance(node, ast.Import) and any(a.name.split(".")[0] == "argparse" for a in node.names):
            uses = True
        elif isinstance(node, ast.ImportFrom) and (node.module or "").split(".")[0] == "argparse":
            uses = True
        elif isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute):
            strings = [a.value for a in node.args if isinstance(a, ast.Constant) and isinstance(a.value, str)]
            if node.func.attr == "add_argument":
                flags.update(s for s in strings if s.startswith("-") and s not in ("-h", "--help"))
            elif node.func.attr == "add_parser" and strings:
                subs.add(strings[0])
    return flags, subs, uses


def sh_flags(text):
    """The flags a shell script accepts: --long and -s alternatives of `case` patterns whose subject is an option
    variable, plus tokens starting with '-' on Usage: lines of the leading comment block."""
    flags = set()
    in_case = False
    for line in text.splitlines():
        if re.match(r"\s*case\s+\"?\$\{?(1|opt|arg|a)\}?\"?\s+in\b", line):
            in_case = True
            continue
        if in_case and re.match(r"\s*esac\b", line):
            in_case = False
            continue
        if in_case:
            m = re.match(r"\s*([^)#]+)\)", line)
            if m:
                flags.update(t.strip() for t in m.group(1).split("|") if re.fullmatch(r"-{1,2}[A-Za-z][\w-]*", t.strip()))
    for line in text.splitlines():
        if line.strip() and not line.startswith("#"):
            if not line.startswith("#!"):
                break
        if re.match(r"#\s*Usage:", line):
            flags.update(re.findall(r"(?<![\w-])(--?[A-Za-z][\w-]*)", line))
    return flags - {"-h", "--help"}


# ---------------------------------------------------------------------------------------------------------------------
# Reading docs

def fenced_free_lines(text):
    """The lines of a markdown text outside code fences."""
    out, fenced = [], False
    for line in text.splitlines():
        if line.lstrip().startswith("```"):
            fenced = not fenced
            continue
        if not fenced:
            out.append(line)
    return out


def h2_headings(text):
    return [l[3:].strip() for l in fenced_free_lines(text) if l.startswith("## ")]


def section(text, name):
    lines, grab, out = fenced_free_lines(text), False, []
    for l in lines:
        if l.startswith("## "):
            grab = l[3:].strip() == name
        elif grab:
            out.append(l)
    return "\n".join(out)


def backticked(text):
    """Every token inside backticks, with multi-word spans also split on whitespace and commas."""
    toks = set()
    for span in re.findall(r"`([^`]+)`", text):
        toks.add(span)
        toks.update(t for t in re.split(r"[\s,]+", span) if t)
    return toks


def is_subsequence(want, have):
    it = iter(have)
    return all(any(w == h for h in it) for w in want)


def has_paragraph(text):
    lines = fenced_free_lines(text)
    return any(l.strip() and not l.startswith("#") for l in lines)


# ---------------------------------------------------------------------------------------------------------------------
# The checks

def collect(root):
    """Run every check over root (a scripts/ directory).  Returns {check name: [failure text]}."""
    ex = load_excludes(root)
    files = candidate_files(root)
    rels = {p: p.relative_to(root).as_posix() for p in files}
    f = {"exclude-file": list(ex.errors), "no-doc": [], "no-script": [], "headings": [], "flags": [], "packages": []}
    for kind, pats in (("path", ex.paths), ("flags", ex.flags)):
        for pat, _ in pats:
            if not any(matches(r, pat) for r in rels.values()):
                f["exclude-file"].append(f"stale {kind} exclude matches no file: {pat}")
    entries = {}
    for p in files:
        k = script_kind(p)
        if k and not any(matches(rels[p], pat) for pat, _ in ex.paths):
            entries[p] = k
    for p in entries:
        if not doc_for(p).exists():
            f["no-doc"].append(f"{rels[p]}: no {doc_for(p).name} beside it")
    docs = [p for p in files if p.suffix == ".md"]
    for d in docs:
        text = d.read_text(encoding="utf-8")
        first = text.splitlines()[0] if text.strip() else ""
        m = H1_SCRIPT.match(first)
        if m:
            if not (d.parent / m.group(1)).exists():
                f["no-script"].append(f"{rels[d]}: H1 names {m.group(1)}, which is not beside it")
            have = h2_headings(text)
            if not is_subsequence(HEADINGS, have):
                f["headings"].append(f"{rels[d]}: needs, in order, the headings {HEADINGS}; has {have}")
        elif d.name == "README.md" and d.parent != root and not any(c.suffix == ".py" for c in d.parent.iterdir()):
            f["no-script"].append(f"{rels[d]}: a package README needs at least one .py module in its directory")
    for p, k in entries.items():
        doc = doc_for(p)
        if not doc.exists():
            continue
        text = doc.read_text(encoding="utf-8")
        if not H1_SCRIPT.match(text.splitlines()[0] if text.strip() else "") and not is_subsequence(HEADINGS, h2_headings(text)):
            f["headings"].append(f"{rels[doc]}: needs, in order, the headings {HEADINGS}")
        if any(matches(rels[p], pat) for pat, _ in ex.flags):
            continue
        src = p.read_text(encoding="utf-8", errors="replace")
        if k == "py":
            flags, subs, uses = py_flags(src)
            if uses and not flags and not subs:
                f["flags"].append(f"FLAGS-UNVERIFIABLE {rels[p]}: argparse is used but no add_argument was found; "
                                  f"add a 'flags: {rels[p]}  # reason' line to {EXCLUDE_FILE}")
                continue
        else:
            flags, subs = sh_flags(src), set()
        options = backticked(section(text, "Options"))
        missing = sorted(x for x in (flags | subs) if x not in options)
        if missing:
            f["flags"].append(f"{rels[doc]}: Options does not list {missing}")
        elif k == "py":
            shown = {t for t in re.findall(r"^\|\s*`(-[^`]+)`", section(text, "Options"), flags=re.M)}
            stale = sorted(t for t in shown if t not in flags and t not in ("-h", "--help") and (flags or subs))
            if stale:
                warnings.warn(f"{rels[doc]}: Options lists {stale}, which the code does not declare")
    for pkg in ex.packages:
        if not (root / pkg / "README.md").exists():
            f["packages"].append(f"{pkg}/README.md: listed under 'packages:' but missing")
        elif not has_paragraph((root / pkg / "README.md").read_text(encoding="utf-8")):
            f["packages"].append(f"{pkg}/README.md: needs an H1 and at least one paragraph")
    return f


# ---------------------------------------------------------------------------------------------------------------------
# The real repository

@pytest.fixture(scope="module")
def failures():
    return collect(SCRIPTS)


@pytest.mark.parametrize("check", ["exclude-file", "no-doc", "no-script", "headings", "flags", "packages"])
def test_scripts_are_documented(failures, check):
    assert not failures[check], f"[{check}]\n  " + "\n  ".join(failures[check])


# ---------------------------------------------------------------------------------------------------------------------
# The checker itself, against small inline fixtures

GOOD_DOC = "# {name}\n\nWhat it is.\n\n" + "".join(f"## {h}\n\n{{body}}\n\n" for h in HEADINGS)


def good_doc(name, options="None."):
    return GOOD_DOC.replace("{name}", name).replace("{body}", "x").replace("## Options\n\nx", "## Options\n\n" + options)


PY_ARGS = ('import argparse\np = argparse.ArgumentParser()\ng = p.add_mutually_exclusive_group()\n'
           'g.add_argument("--sonar", "--sonarqube")\nsub = p.add_subparsers()\nsub.add_parser("run").add_argument("-v")\n'
           'p.add_argument("message")\nif __name__ == "__main__":\n    pass\n')


def tree(tmp_path, files):
    for name, text in files.items():
        p = tmp_path / name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text)
    return tmp_path


def test_py_flags_collects_aliases_groups_and_subparsers():
    flags, subs, uses = py_flags(PY_ARGS)
    assert flags == {"--sonar", "--sonarqube", "-v"}
    assert subs == {"run"}
    assert uses


def test_py_flags_without_argparse():
    assert py_flags("import sys\nprint(sys.argv)\n") == (set(), set(), False)


def test_main_guard_either_order_and_quote():
    assert has_main_guard('if __name__ == "__main__":\n    pass\n')
    assert has_main_guard("if '__main__' == __name__:\n    pass\n")
    assert not has_main_guard("def f():\n    if __name__ == '__main__':\n        pass\n")


def test_sh_flags_reads_option_case_and_usage_but_not_os_case():
    src = ('#!/bin/bash\n# Usage: tool.sh [--fast] [-n]\nwhile [ $# -gt 0 ]; do\n  case "$1" in\n    --dry-run|-d) x=1;;\n'
           '    *) ;;\n  esac\ndone\ncase "$(uname)" in\n  Darwin) y=1;;\nesac\n')
    assert sh_flags(src) == {"--fast", "-n", "--dry-run", "-d"}


def test_sh_flags_none():
    assert sh_flags("#!/bin/sh\necho hi\n") == set()


def test_parse_excludes_requires_reason_and_reads_prefixes():
    ex = parse_excludes("# c\n\na/*.sh  # fixture\nflags: b.py  # dynamic\npackages: p, q\nbad.py\n")
    assert ex.paths == [("a/*.sh", "fixture")]
    assert ex.flags == [("b.py", "dynamic")]
    assert ex.packages == ["p", "q"]
    assert len(ex.errors) == 1 and "bad.py" in ex.errors[0]


def test_clean_tree_passes(tmp_path):
    t = tree(tmp_path, {"a.py": PY_ARGS, "a.md": good_doc("a.py", "`--sonar`, `--sonarqube`, `-v`, `run`"),
                        "pkg/__init__.py": "", "pkg/README.md": "# pkg\n\nA library.\n", EXCLUDE_FILE: "packages: pkg\n"})
    assert not any(collect(t).values())


def test_missing_doc_is_reported(tmp_path):
    t = tree(tmp_path, {"a.py": PY_ARGS})
    assert collect(t)["no-doc"]


def test_stray_doc_is_reported(tmp_path):
    t = tree(tmp_path, {"gone.md": good_doc("gone.py")})
    assert collect(t)["no-script"]


def test_missing_heading_is_reported(tmp_path):
    t = tree(tmp_path, {"a.py": "if __name__ == '__main__':\n    pass\n",
                        "a.md": good_doc("a.py").replace("## Gotchas\n\nx\n\n", "")})
    assert collect(t)["headings"]


def test_undocumented_flag_is_reported(tmp_path):
    t = tree(tmp_path, {"a.py": PY_ARGS, "a.md": good_doc("a.py", "`--sonar`, `-v`, `run`")})
    assert any("--sonarqube" in m for m in collect(t)["flags"])


def test_unverifiable_flags_need_an_exclude(tmp_path):
    src = "import argparse\nfor n in ('--a',):\n    pass\nif __name__ == '__main__':\n    pass\n"
    t = tree(tmp_path, {"a.py": src, "a.md": good_doc("a.py")})
    assert any(m.startswith("FLAGS-UNVERIFIABLE") for m in collect(t)["flags"])
    (t / EXCLUDE_FILE).write_text("flags: a.py  # flags added by a loop\n")
    assert not collect(t)["flags"]


def test_stale_exclude_is_reported(tmp_path):
    t = tree(tmp_path, {EXCLUDE_FILE: "nothing/*.py  # matches nothing\n"})
    assert collect(t)["exclude-file"]


def test_excluded_script_needs_no_doc(tmp_path):
    t = tree(tmp_path, {"a.py": PY_ARGS, EXCLUDE_FILE: "a.py  # not documented here\n"})
    assert not collect(t)["no-doc"]


def test_missing_package_readme_is_reported(tmp_path):
    t = tree(tmp_path, {"pkg/__init__.py": "", EXCLUDE_FILE: "packages: pkg\n"})
    assert collect(t)["packages"]


def test_extensionless_shebang_script_is_an_entry_point(tmp_path):
    t = tree(tmp_path, {"close": "#!/usr/bin/env bash\necho hi\n"})
    assert collect(t)["no-doc"]


def test_reverse_check_only_warns(tmp_path):
    t = tree(tmp_path, {"a.py": PY_ARGS, "a.md": good_doc("a.py", "`--sonar`, `--sonarqube`, `-v`, `run`\n\n| `--old` | x |")})
    with pytest.warns(UserWarning, match="--old"):
        assert not collect(t)["flags"]
