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
"""Every engine file must parse as Python 3.9 (the oldest python3 on James's machines is /usr/bin/python3 = 3.9.6)."""

import ast
import unittest

from tmux_support import BIN


class FloorTest(unittest.TestCase):
    def test_every_engine_file_parses_as_python_3_9(self):
        files = [BIN / "engine.py"] + sorted((BIN / "terminal_engine").glob("*.py"))
        self.assertGreater(len(files), 5)
        for f in files:
            with self.subTest(f=f.name):
                ast.parse(f.read_text(encoding="utf-8"), filename=str(f), feature_version=(3, 9))

    def test_no_union_annotations_or_process_group(self):
        for f in sorted((BIN / "terminal_engine").glob("*.py")):
            text = f.read_text(encoding="utf-8")
            with self.subTest(f=f.name):
                self.assertNotIn("process_group=", text)
                self.assertNotRegex(text, r"->\s*[\w\[\], ]+\s\|\s")
                self.assertNotRegex(text, r":\s*[\w\[\]]+\s\|\s[\w\[\]]+\s*[=,)]")


if __name__ == "__main__":
    unittest.main()
