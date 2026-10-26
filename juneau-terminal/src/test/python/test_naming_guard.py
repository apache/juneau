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
"""Old names must not come back in juneau-terminal's sources or the scripts that use it: the console view and its log are
called "board" everywhere, and every module, package, tool and feature name renamed to the Terminal, BCT and Console
project names is listed in old_names.json beside this file."""

import json
import re
import unittest
from pathlib import Path

# Spelled in two parts so this file does not match itself.
OLD_NAME = "con" + "densed"
HERE = Path(__file__).resolve().parent
ROOT = Path(__file__).resolve().parents[4]
TREES = (ROOT / "juneau-terminal" / "src", ROOT / "scripts")
SKIPPED_DIRS = {"__pycache__", "node_modules", ".pytest_cache"}
OLD_NAMES = HERE / "old_names.json"

# Match kinds of old_names.json entries: (lookbehind, lookahead) around the escaped name.
BOUNDS = {
    "token": (r"(?<![A-Za-z0-9_-])", r"(?![A-Za-z0-9_-])"),
    "prefix": (r"(?<![A-Za-z0-9_-])", ""),
    "ident": (r"(?<![A-Za-z0-9_])", r"(?![A-Za-z0-9_])"),
    "package": (r"(?<![\w.])", r"(?!\w)"),
    "literal": ("", ""),
}


def compile_entry(entry):
    """An old_names.json entry ({label, parts, sep, kind}, optional before/after) as a regex."""
    before, after = BOUNDS[entry.get("kind", "token")]
    before = entry.get("before", before)
    after = entry.get("after", after)
    return re.compile(before + re.escape(entry.get("sep", "").join(entry["parts"])) + after)


def load_old_names():
    raw = json.loads(OLD_NAMES.read_text(encoding="utf-8"))
    return ([(e["label"], compile_entry(e)) for e in raw["entries"]], [compile_entry(e) for e in raw["keep"]])


def source_files():
    for tree in TREES:
        for p in sorted(tree.rglob("*")):
            if not p.is_file() or SKIPPED_DIRS & set(p.parts) or p.suffix in (".pyc", ".zip", ".png"):
                continue
            try:
                yield p, p.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue


def old_name_hits(text, entries, keep):
    """The labels of the old names in text, after masking the keep names."""
    for rx in keep:
        text = rx.sub(lambda m: "\x00" * len(m.group(0)), text)
    return [label for label, rx in entries if rx.search(text)]


class OldNameGuardTest(unittest.TestCase):
    def test_no_file_uses_the_old_name(self):
        hits = [str(p.relative_to(ROOT)) for p, text in source_files() if OLD_NAME in text.lower()]
        self.assertEqual(hits, [])

    def test_no_file_uses_a_renamed_name(self):
        entries, keep = load_old_names()
        hits = []
        for p, text in source_files():
            hits += [f"{p.relative_to(ROOT)}: {label}" for label in old_name_hits(text, entries, keep)]
        self.assertEqual(hits, [])

    def test_the_list_and_this_guard_spell_no_old_name_whole(self):
        entries, keep = load_old_names()
        for p in (OLD_NAMES, Path(__file__).resolve()):
            self.assertEqual(old_name_hits(p.read_text(encoding="utf-8"), entries, keep), [], p.name)

    def test_the_list_catches_an_old_name_and_spares_a_kept_one(self):
        entries, keep = load_old_names()
        self.assertTrue(old_name_hits("x " + "juneau" + "-run" + " y", entries, keep))
        self.assertEqual(old_name_hits("juneau" + "-test-utils", entries, keep), [])
        self.assertEqual(old_name_hits("##" + "pytest x", entries, keep), [])


if __name__ == "__main__":
    unittest.main()
