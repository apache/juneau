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
# <Apache header>
"""juneau_scripts.notify: bell on a fake TTY, the silence variables, and the sound commands per platform."""

import os
import subprocess
import sys
import unittest
from unittest import mock

import scripts_support  # noqa: F401
from scripts_support import FakeTty

from juneau_scripts import notify

QUIET = ("SANDBOX_RUNTIME", "JUNEAU_SCRIPTS_QUIET", "MY_NO_BELL", "OTHER_NO_BELL")


class NotifyCase(unittest.TestCase):
    def setUp(self):
        patcher = mock.patch.dict(os.environ)
        patcher.start()
        self.addCleanup(patcher.stop)
        for key in QUIET:
            os.environ.pop(key, None)


class BellTest(NotifyCase):
    def ring(self, tty=True, **kw):
        out = FakeTty(tty)
        with mock.patch.object(sys, "stdout", out):
            notify.bell(**kw)
        return out.out

    def test_rings_on_a_tty(self):
        self.assertEqual(self.ring(), "\a")

    def test_silent_off_a_tty(self):
        self.assertEqual(self.ring(False), "")

    def test_silenced_by_each_variable(self):
        for key, kw in (("SANDBOX_RUNTIME", {}), ("JUNEAU_SCRIPTS_QUIET", {}),
                        ("MY_NO_BELL", {"silence_env": "MY_NO_BELL"}),
                        ("OTHER_NO_BELL", {"silence_env": ("MY_NO_BELL", "OTHER_NO_BELL")})):
            with self.subTest(key=key), mock.patch.dict(os.environ, {key: "1"}):
                self.assertEqual(self.ring(**kw), "")

    def test_only_exactly_1_silences(self):
        for value in ("0", "true", "yes", ""):
            with self.subTest(value=value), mock.patch.dict(os.environ, {"MY_NO_BELL": value, "SANDBOX_RUNTIME": value}):
                self.assertEqual(self.ring(silence_env="MY_NO_BELL"), "\a")

    def test_never_raises(self):
        out = FakeTty(True)
        out.write = mock.Mock(side_effect=OSError("closed"))
        with mock.patch.object(sys, "stdout", out):
            notify.bell()


class SoundTest(NotifyCase):
    def play(self, system, ok, run_side_effect=None, exists=True):
        with mock.patch.object(notify.platform, "system", return_value=system), \
                mock.patch.object(notify.os.path, "exists", return_value=exists), \
                mock.patch.object(notify.subprocess, "run", side_effect=run_side_effect) as run:
            notify.sound(ok)
        return [(c.args[0], c.kwargs.get("timeout")) for c in run.call_args_list]

    def test_macos(self):
        self.assertEqual(self.play("Darwin", True), [(["afplay", notify.MACOS_SUCCESS_SOUND], 5)])
        self.assertEqual(self.play("Darwin", False), [(["afplay", notify.MACOS_FAILURE_SOUND], 5)])
        self.assertEqual(self.play("Darwin", True, exists=False), [])

    def test_linux_falls_back_to_speaker_test_when_paplay_is_missing(self):
        self.assertEqual(self.play("Linux", True), [(["paplay", notify.LINUX_SUCCESS_SOUND], 5)])
        calls = self.play("Linux", False, run_side_effect=[OSError("no paplay"), None])
        self.assertEqual(calls, [(["paplay", notify.LINUX_FAILURE_SOUND], 5),
                                 (["speaker-test", "-t", "sine", "-f", "400", "-l", "1"], 2)])

    def test_windows(self):
        fake = mock.Mock(MB_OK=0, MB_ICONHAND=16)
        with mock.patch.dict(sys.modules, {"winsound": fake}):
            self.assertEqual(self.play("Windows", False), [])
        fake.MessageBeep.assert_called_once_with(16)

    def test_never_raises(self):
        self.play("Darwin", True, run_side_effect=subprocess.TimeoutExpired("afplay", 5))
        self.play("Linux", True, run_side_effect=[OSError("a"), OSError("b")])

    def test_silenced(self):
        with mock.patch.dict(os.environ, {"JUNEAU_SCRIPTS_QUIET": "1"}):
            self.assertEqual(self.play("Darwin", True), [])


if __name__ == "__main__":
    unittest.main()
