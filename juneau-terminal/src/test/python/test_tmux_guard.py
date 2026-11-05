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
"""No test may run the real tmux: every tmux call in the test sources names a private server with -L."""

import re
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
CALL = re.compile(r'\[\s*"tmux"\s*,(?!\s*"-L")')


class TmuxGuardTest(unittest.TestCase):
    def test_every_tmux_call_uses_a_private_server(self):
        bad = []
        for f in sorted(HERE.glob("*.py")):
            if f.name == Path(__file__).name:
                continue
            for n, line in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
                if CALL.search(line):
                    bad.append(f"{f.name}:{n}")
        self.assertEqual(bad, [], "tmux called without -L (it could act on the operator's panes)")


if __name__ == "__main__":
    unittest.main()
