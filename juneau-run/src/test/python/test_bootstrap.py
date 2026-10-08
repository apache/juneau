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
"""Bootstrap: the README snippet, extracted and executed against a fake mvn on PATH."""

import contextlib
import io
import os
import re
import stat
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from support import MODULE_ROOT, SCRIPT


def snippet_source():
    text = (MODULE_ROOT / "README.md").read_text(encoding="utf-8")
    block = re.search(r"```python\n(def load_juneau_run.*?)```", text, re.S)
    assert block, "README.md has no load_juneau_run snippet"
    return block.group(1)


class BootstrapTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.bin = self.tmp / "bin"
        self.bin.mkdir()
        self.calls = self.tmp / "calls.log"
        self.cache = self.tmp / "cache"
        patcher = mock.patch.dict(os.environ, {"PATH": str(self.bin) + os.pathsep + "/usr/bin:/bin"})
        patcher.start()
        self.addCleanup(patcher.stop)
        os.environ.pop("JUNEAU_RUN_PY", None)
        namespace = {}
        exec(snippet_source(), namespace)
        self.load = namespace["load_juneau_run"]

    def fake_mvn(self, exit_code=0):
        script = self.bin / "mvn"
        script.write_text(
            "#!/bin/sh\n"
            f"echo \"$@\" >> {self.calls}\n"
            "for a in \"$@\"; do case \"$a\" in -DoutputDirectory=*) out=\"${a#-DoutputDirectory=}\";; esac; done\n"
            f"[ {exit_code} -eq 0 ] || exit {exit_code}\n"
            f"mkdir -p \"$out\" && cp {SCRIPT} \"$out/juneau-run.py\"\n", encoding="utf-8")
        script.chmod(script.stat().st_mode | stat.S_IEXEC)

    def call_count(self):
        return len(self.calls.read_text().splitlines()) if self.calls.exists() else 0

    def load_quietly(self, version="10.0.0"):
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            mod = self.load(version, str(self.cache))
        return mod, err.getvalue()

    def test_override_skips_maven_and_loads_the_given_file(self):
        os.environ["JUNEAU_RUN_PY"] = str(SCRIPT)
        mod, err = self.load_quietly()
        self.assertTrue(callable(mod.run_tool))
        self.assertEqual((self.call_count(), err), (0, ""))

    def test_release_pin_copies_once(self):
        self.fake_mvn()
        self.load_quietly("10.0.0")
        mod, _ = self.load_quietly("10.0.0")
        self.assertTrue(callable(mod.emit))
        self.assertEqual(self.call_count(), 1)

    def test_snapshot_pin_copies_on_every_call(self):
        self.fake_mvn()
        self.load_quietly("10.0.0-SNAPSHOT")
        self.load_quietly("10.0.0-SNAPSHOT")
        self.assertEqual(self.call_count(), 2)

    def test_copy_uses_dependency_copy_with_stripped_version(self):
        self.fake_mvn()
        self.load_quietly("10.0.0")
        args = self.calls.read_text()
        for expected in ("dependency:copy", "-Dartifact=org.apache.juneau:juneau-run:10.0.0:py",
                         "-Dmdep.stripVersion=true", f"-DoutputDirectory={self.cache}"):
            self.assertIn(expected, args)

    def test_failing_copy_returns_none_with_exactly_one_stderr_line(self):
        self.fake_mvn(exit_code=1)
        mod, err = self.load_quietly()
        self.assertIsNone(mod)
        self.assertEqual(len(err.strip().splitlines()), 1)
        self.assertTrue(err.startswith("warning: juneau-run unavailable"))

    def test_missing_mvn_returns_none_with_exactly_one_stderr_line(self):
        with mock.patch.dict(os.environ, {"PATH": str(self.bin)}):
            mod, err = self.load_quietly()
        self.assertIsNone(mod)
        self.assertEqual(len(err.strip().splitlines()), 1)


if __name__ == "__main__":
    unittest.main()
