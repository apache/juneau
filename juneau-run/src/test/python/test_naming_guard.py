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
"""The console view and its log are called "board" everywhere; the old name must not come back in juneau-run's sources or
the scripts that use it."""

import unittest
from pathlib import Path

# Spelled in two parts so this file does not match itself.
OLD_NAME = "con" + "densed"
ROOT = Path(__file__).resolve().parents[4]
TREES = (ROOT / "juneau-run" / "src", ROOT / "scripts")
SKIPPED_DIRS = {"__pycache__", "node_modules", ".pytest_cache"}


class OldNameGuardTest(unittest.TestCase):
    def test_no_file_uses_the_old_name(self):
        hits = []
        for tree in TREES:
            for p in tree.rglob("*"):
                if not p.is_file() or SKIPPED_DIRS & set(p.parts) or p.suffix in (".pyc", ".zip", ".png"):
                    continue
                try:
                    text = p.read_text(encoding="utf-8")
                except UnicodeDecodeError:
                    continue
                if OLD_NAME in text.lower():
                    hits.append(str(p.relative_to(ROOT)))
        self.assertEqual(hits, [])


if __name__ == "__main__":
    unittest.main()
