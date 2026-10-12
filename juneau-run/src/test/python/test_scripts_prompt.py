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
"""juneau_scripts.prompt with and without juneau_run, and juneau_run's public paused()."""

import builtins
import sys
import unittest
from unittest import mock

import scripts_support
from scripts_support import jr, reset_run_state

from juneau_scripts import prompt, testing


def eof(_prompt=""):
    raise EOFError


class PlainPromptTest(unittest.TestCase):
    """No juneau_run loaded: plain input() and getpass()."""

    def setUp(self):
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)

    def test_text(self):
        with mock.patch.object(builtins, "input", return_value="  hello  "):
            self.assertEqual(prompt.text("Q: "), "hello")
        with mock.patch.object(builtins, "input", return_value=""):
            self.assertIsNone(prompt.text("Q: "))
            self.assertEqual(prompt.text("Q: ", default="d"), "d")
        with mock.patch.object(builtins, "input", side_effect=eof):
            self.assertIsNone(prompt.text("Q: "))
            self.assertIsNone(prompt.text("Q: ", default="d"))

    def test_confirm(self):
        for answer, default, expected in (("y", False, True), ("YES", False, True), ("n", True, False),
                                          ("", True, True), ("", False, False), ("maybe", True, False)):
            with self.subTest(answer=answer, default=default), \
                    mock.patch.object(builtins, "input", return_value=answer):
                self.assertIs(prompt.confirm("Go? ", default=default), expected)
        with mock.patch.object(builtins, "input", side_effect=eof):
            self.assertIs(prompt.confirm("Go? ", default=True), True)

    def test_secret(self):
        with mock.patch.object(prompt.getpass, "getpass", return_value="s3cret") as gp:
            self.assertEqual(prompt.secret("Password: "), "s3cret")
        gp.assert_called_once_with("Password: ")
        with mock.patch.object(prompt.getpass, "getpass", side_effect=EOFError):
            self.assertIsNone(prompt.secret("Password: "))


class ConsolePromptTest(unittest.TestCase):
    """juneau_run loaded: text and confirm go through jr.ask; secret pauses the view and is never published."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        sys.modules["juneau_run"] = jr
        self.events = []
        jr.subscribe(lambda kind, fields: self.events.append((kind, dict(fields))))

    def test_text_goes_through_ask(self):
        with mock.patch.object(builtins, "input", return_value="answer"):
            self.assertEqual(prompt.text("Q: "), "answer")
        self.assertIn(("ask", {"prompt": "Q: ", "answer": "answer"}), self.events)

    def test_secret_is_never_published(self):
        with mock.patch.object(prompt.getpass, "getpass", return_value="s3cret"):
            self.assertEqual(prompt.secret("Password: "), "s3cret")
        self.assertNotIn("s3cret", repr(self.events))

    def test_paused_is_public_and_pauses_the_console(self):
        console = mock.Mock()
        with mock.patch.object(jr, "_console", console):
            with jr.paused():
                console.pause.assert_called_once_with()
                console.resume.assert_not_called()
            console.resume.assert_called_once_with()

    def test_secret_pauses_the_console(self):
        console = mock.Mock()
        with mock.patch.object(jr, "_console", console), \
                mock.patch.object(prompt.getpass, "getpass", return_value="x"):
            prompt.secret("Password: ")
        console.pause.assert_called_once_with()
        console.resume.assert_called_once_with()


if __name__ == "__main__":
    unittest.main()
