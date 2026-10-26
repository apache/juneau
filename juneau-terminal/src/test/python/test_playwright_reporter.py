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
"""The Playwright reporter's node tests (src/test/js), run in the Maven gate."""

import shutil
import subprocess
import unittest

import support

TESTS = support.MODULE_ROOT / "src" / "test" / "js" / "juneau_terminal_playwright.test.cjs"


@unittest.skipUnless(shutil.which("node"), "node not installed")
class PlaywrightReporterTest(unittest.TestCase):
    def test_node_tests_pass(self):
        p = subprocess.run(["node", "--test", str(TESTS)], capture_output=True, text=True, timeout=120)
        self.assertEqual(p.returncode, 0, p.stdout + p.stderr)
