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
"""juneau_terminal_scripts.prompt with and without juneau_terminal, and juneau_terminal's public paused()."""

import builtins
import contextlib
import io
import json
import os
import sys
import unittest
from unittest import mock

import scripts_support
from scripts_support import jr, reset_run_state

from juneau_terminal_scripts import prompt, testing


def eof(_prompt=""):
    raise EOFError


class PlainPromptTest(unittest.TestCase):
    """No juneau_terminal loaded: plain input() and getpass()."""

    def setUp(self):
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)

    def test_text(self):
        with mock.patch.object(builtins, "input", return_value="  hello  "):
            self.assertEqual(prompt.text("Q"), "hello")
        with mock.patch.object(builtins, "input", return_value=""):
            self.assertIsNone(prompt.text("Q"))
            self.assertEqual(prompt.text("Q", default="d"), "d")
        with mock.patch.object(builtins, "input", side_effect=eof):
            self.assertIsNone(prompt.text("Q"))
            self.assertIsNone(prompt.text("Q", default="d"))

    def test_confirm(self):
        for answer, default, expected in (("y", False, True), ("YES", False, True), ("n", True, False),
                                          ("", True, True), ("", False, False), ("maybe", True, False)):
            with self.subTest(answer=answer, default=default), \
                    mock.patch.object(builtins, "input", return_value=answer):
                self.assertIs(prompt.confirm("Go? ", default=default), expected)
        with mock.patch.object(builtins, "input", side_effect=eof):
            self.assertIs(prompt.confirm("Go? ", default=True), True)

    def test_confirm_shows_the_yes_no_hint_with_the_default_capitalized(self):
        for default, hint in ((False, "(y/N)"), (True, "(Y/n)")):
            with self.subTest(default=default), mock.patch.object(builtins, "input", return_value="y") as ask:
                prompt.confirm("Go? ", default=default)
            ask.assert_called_once_with(f"Go? {hint} ")
        with mock.patch.object(builtins, "input", return_value="X") as ask:
            prompt.confirm("Type X", expect="X")
        ask.assert_called_once_with("Type X")

    def test_email_confirm_uses_the_shared_hint(self):
        with mock.patch.object(builtins, "input", return_value="y") as ask, \
                contextlib.redirect_stdout(io.StringIO()):
            prompt.email(to="a@x.org", subject="s", body="b")
        ask.assert_called_once_with("Sent it from your apache.org address? (y/N) ")

    def test_secret(self):
        with mock.patch.object(prompt.getpass, "getpass", return_value="s3cret") as gp:
            self.assertEqual(prompt.secret("Password: "), "s3cret")
        gp.assert_called_once_with("Password: ")
        with mock.patch.object(prompt.getpass, "getpass", side_effect=EOFError):
            self.assertIsNone(prompt.secret("Password: "))


class ConsolePromptTest(unittest.TestCase):
    """juneau_terminal loaded: text and confirm go through jr.ask; secret pauses the view and is never published."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        sys.modules["juneau_terminal"] = jr
        self.events = []
        jr.subscribe(lambda kind, fields: self.events.append((kind, dict(fields))))

    def test_text_goes_through_ask(self):
        with mock.patch.object(builtins, "input", return_value="answer"):
            self.assertEqual(prompt.text("Q"), "answer")
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


class InlineVisibilityTest(unittest.TestCase):
    """Inline prompts show their default and print labels and errors with the console view paused."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        prompt._reset()
        sys.modules["juneau_terminal"] = jr

    def test_text_shows_default_and_colon(self):
        asked = []
        with mock.patch.object(prompt, "_ask", side_effect=lambda m: asked.append(m) or ""):
            prompt.text("Release branch", default="main")
            prompt.text("Name")
            prompt.text("Done: ", default="x")
            prompt.text("Blank", default="")
            prompt.text("Repo name: orgapache-", default="")
        self.assertEqual(asked, ["Release branch [main]: ", "Name: ", "Done: ", "Blank: ", "Repo name: orgapache-"])

    def test_multiline_label_and_error_print_while_paused(self):
        console = mock.Mock()
        state = {"paused": False}
        console.pause.side_effect = lambda: state.update(paused=True)
        console.resume.side_effect = lambda: state.update(paused=False)
        seen = []
        real_print = builtins.print

        def spy(*a, **k):
            seen.append((a[0] if a else "", state["paused"]))
            real_print(*a, **k)

        fields = [prompt.Field("count", "Count", type="int", required=True),
                  prompt.Field("notes", "Summary", multiline=True)]
        out = io.StringIO()
        with mock.patch.object(jr, "_console", console), mock.patch.object(builtins, "print", spy), \
                contextlib.redirect_stdout(out), \
                mock.patch.object(builtins, "input", side_effect=["abc", "7", "a", "."]):
            self.assertEqual(prompt.form("Details", fields), {"count": 7, "notes": "a"})
        by_text = dict(seen)
        self.assertTrue(by_text["Count must be a whole number"])
        self.assertTrue(by_text["Summary (end with a line holding only '.'):"])


class InlineKindsTest(unittest.TestCase):
    def setUp(self):
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)
        prompt._reset()

    def test_inline_text_default_and_eof(self):
        with mock.patch("builtins.input", side_effect=["", "  x  ", EOFError]):
            self.assertEqual(prompt.text("Version", default="9.2.1"), "9.2.1")
            self.assertEqual(prompt.text("Version"), "x")
            self.assertIsNone(prompt.text("Version", default="d"))

    def test_inline_form_multiline_and_int_reask(self):
        fields = [prompt.Field("name", "Name", default="Ann"), prompt.Field("count", "Count", type="int", required=True),
                  prompt.Field("notes", "Notes", multiline=True)]
        answers = ["", "abc", "7", "line1", "line2", "."]
        with mock.patch("builtins.input", side_effect=answers):
            self.assertEqual(prompt.form("Details", fields), {"name": "Ann", "count": 7, "notes": "line1\nline2"})

    def test_inline_form_secret_field_uses_getpass(self):
        with mock.patch("builtins.input", side_effect=["Ann"]), \
                mock.patch.object(prompt.getpass, "getpass", return_value="pw") as gp:
            got = prompt.form("Login", [prompt.Field("user", "User"), prompt.Field("pass", "Password", secret=True)])
        self.assertEqual(got, {"user": "Ann", "pass": "pw"})
        gp.assert_called_once()

    def test_inline_email_prints_block_and_confirms(self):
        out = io.StringIO()
        with mock.patch("builtins.input", side_effect=["y"]), contextlib.redirect_stdout(out):
            self.assertTrue(prompt.email(to="dev@x.org", subject="[VOTE] Y", body="Hello", cc=["pmc@x.org"]))
        text = out.getvalue()
        for needle in ("To: dev@x.org", "Cc: pmc@x.org", "Subject: [VOTE] Y", "Hello", "mailto:", "─"):
            self.assertIn(needle, text)

    def test_inline_email_long_body_has_no_link(self):
        out = io.StringIO()
        with mock.patch("builtins.input", side_effect=[""]), contextlib.redirect_stdout(out):
            self.assertFalse(prompt.email(to=["dev@x.org"], subject="s", body="x" * 5000))
        self.assertNotIn("mailto:dev", out.getvalue())
        self.assertIn("too long for a link", out.getvalue())

    def test_inline_choice_and_confirm_expect(self):
        with mock.patch("builtins.input", side_effect=["b"]):
            self.assertEqual(prompt.choice("Pick", [("a", "Alpha"), ("b", "Beta")]), "b")
        with mock.patch("builtins.input", side_effect=["2"]):
            self.assertEqual(prompt.choice("Pick", ["a", "b"]), "b")
        with mock.patch("builtins.input", side_effect=["nope", "RELEASE"]):
            self.assertFalse(prompt.confirm("Type RELEASE", expect="RELEASE"))
            self.assertTrue(prompt.confirm("Type RELEASE", expect="RELEASE"))


class ChannelCase(unittest.TestCase):
    """A real channel over two pipes.  Answers are written before the prompt is asked (ids are p1, p2, ...)."""

    def setUp(self):
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)
        prompt._reset()
        self.addCleanup(prompt._reset)
        self.out_r, out_w = os.pipe()      # script -> host
        in_r, self.in_w = os.pipe()        # host -> script
        os.environ["JUNEAU_PROMPT_FD_OUT"] = str(out_w)
        os.environ["JUNEAU_PROMPT_FD_IN"] = str(in_r)
        self.addCleanup(self._close)

    def _close(self):
        for fd in (self.out_r, self.in_w):
            try:
                os.close(fd)
            except OSError:
                pass

    def answer(self, **fields):
        os.write(self.in_w, (json.dumps({"v": 1, "type": "answer", **fields}) + "\n").encode())

    def sent(self):
        prompt._reset()  # closes the script's write end, so reading to EOF terminates
        data = b""
        while True:
            chunk = os.read(self.out_r, 65536)
            if not chunk:
                break
            data += chunk
        return [json.loads(line) for line in data.decode().splitlines()]


class ChannelTest(ChannelCase):
    def test_channel_round_trip_each_kind(self):
        self.answer(id="p1", value="9.2.1", source="entered")
        self.answer(id="p2", value=True)
        self.answer(id="p3", value="tok", source="stored")
        self.answer(id="p4", value="b")
        self.answer(id="p5", value={"name": "Ann", "count": "7"})
        self.answer(id="p6", value="sent")
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(prompt.text("Version", default="9.2.1", key="version"), "9.2.1")
            self.assertTrue(prompt.confirm("Go?"))
            self.assertEqual(prompt.secret("Token", key="token"), "tok")
            self.assertEqual(prompt.choice("Pick", [("a", "A"), ("b", "B")]), "b")
            self.assertEqual(prompt.form("F", [prompt.Field("name", "Name"), prompt.Field("count", "Count", type="int")]),
                             {"name": "Ann", "count": 7})
            self.assertTrue(prompt.email(to="a@x.org", subject="s", body="b"))
        msgs = self.sent()
        self.assertEqual(msgs[0]["type"], "hello")
        prompts = [m for m in msgs if m["type"] == "prompt"]
        self.assertEqual([p["kind"] for p in prompts], ["text", "confirm", "secret", "choice", "form", "email"])
        self.assertEqual([p["id"] for p in prompts], ["p1", "p2", "p3", "p4", "p5", "p6"])
        self.assertTrue(all(p["v"] == 1 for p in prompts))
        self.assertEqual(prompts[0]["key"], "version")
        self.assertEqual(prompts[1]["hint"], "(y/N)")
        self.assertIn("mailto:", prompts[5]["email"]["mailto"])
        self.assertEqual([f["kind"] for f in prompts[4]["fields"]], ["text", "int"])

    def test_hello_carries_the_extra_fields(self):
        prompt.hello(attempt="a1")
        self.answer(id="p1", value="x")
        with contextlib.redirect_stdout(io.StringIO()):
            prompt.text("T")
        hello = self.sent()[0]
        self.assertEqual((hello["type"], hello["attempt"]), ("hello", "a1"))
        self.assertIn("pid", hello)

    def test_channel_secret_not_published_or_logged(self):
        sys.modules["juneau_terminal"] = jr
        self.addCleanup(sys.modules.pop, "juneau_terminal", None)
        events = []
        jr.subscribe(lambda kind, fields: events.append((kind, dict(fields))))
        self.answer(id="p1", value="hunter2-secret", source="entered")
        self.answer(id="p2", value="visible")
        err = io.StringIO()
        out = io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            self.assertEqual(prompt.secret("Token"), "hunter2-secret")
            self.assertEqual(prompt.text("Name"), "visible")
        self.assertNotIn("hunter2-secret", out.getvalue() + err.getvalue() + repr(events))
        self.assertIn("********", out.getvalue())
        self.assertIn(("ask", {"prompt": "Name", "answer": "visible"}), events)

    def test_secret_form_field_masked(self):
        self.answer(id="p1", value={"user": "ann", "pw": "hunter2-secret"})
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            got = prompt.form("Login", [prompt.Field("user", "User"), prompt.Field("pw", "Pass", secret=True)])
        self.assertEqual(got["pw"], "hunter2-secret")
        self.assertNotIn("hunter2-secret", out.getvalue())
        self.assertIn("user=ann", out.getvalue())

    def test_transcript_line_masks_secret(self):
        self.answer(id="p1", value="abc", source="stored")
        with contextlib.redirect_stdout(io.StringIO()) as out:
            prompt.secret("Token")
        self.assertIn("? Token: ******** (stored)", out.getvalue())

    def test_channel_env_removed_after_read(self):
        self.answer(id="p1", value="x")
        with contextlib.redirect_stdout(io.StringIO()):
            prompt.text("T")
        self.assertNotIn("JUNEAU_PROMPT_FD_OUT", os.environ)
        self.assertNotIn("JUNEAU_PROMPT_FD_IN", os.environ)

    def test_channel_cancel_and_eof_return_none(self):
        self.answer(id="p1", cancel=True)
        self.assertIsNone(prompt.text("T"))
        os.close(self.in_w)
        self.in_w = os.open(os.devnull, os.O_RDONLY)  # host end gone -> EOF
        self.assertIsNone(prompt.text("T"))
        self.assertIsNone(prompt.confirm("C"))   # dead channel: immediate None, no hang

    def test_channel_mismatched_id_dropped(self):
        self.answer(id="p9", value="stale")
        self.answer(id="p1", value="right")
        err = io.StringIO()
        with contextlib.redirect_stderr(err), contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(prompt.text("T"), "right")
        self.assertNotIn("stale", err.getvalue())

    def test_non_json_and_unknown_type_lines_skipped(self):
        os.write(self.in_w, b"not json at all\n")
        os.write(self.in_w, b'{"v":1,"type":"future","id":"p1"}\n')
        self.answer(id="p1", value="ok")
        err = io.StringIO()
        with contextlib.redirect_stderr(err), contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(prompt.text("T"), "ok")
        self.assertNotIn("not json at all", err.getvalue())

    def test_channel_validation_reasks_with_error(self):
        self.answer(id="p1", value={"count": "abc"})
        self.answer(id="p2", value={"count": "4"})
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(prompt.form("F", [prompt.Field("count", "Count", type="int", required=True)]),
                             {"count": 4})
        second = [m for m in self.sent() if m["type"] == "prompt"][1]
        self.assertIn("error", second)
        self.assertNotIn("abc", second["error"])

    def test_choice_rejects_unlisted_value(self):
        self.answer(id="p1", value="zzz")
        self.answer(id="p2", value="a")
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(prompt.choice("Pick", ["a", "b"]), "a")
        self.assertIn("error", [m for m in self.sent() if m["type"] == "prompt"][1])

    def test_confirm_expect_compares_the_typed_text(self):
        self.answer(id="p1", value="9.2.1")
        self.answer(id="p2", value="9.2.2")
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertTrue(prompt.confirm("Type it", expect="9.2.1"))
            self.assertFalse(prompt.confirm("Type it", expect="9.2.1"))

    def test_unknown_version_is_eof(self):
        os.write(self.in_w, b'{"v":2,"type":"answer","id":"p1","value":"x"}\n')
        self.assertIsNone(prompt.text("T"))

    def test_step_field_follows_current_step(self):
        prompt.current_step = "tag"
        self.answer(id="p1", value="x")
        with contextlib.redirect_stdout(io.StringIO()):
            prompt.text("T")
        self.assertEqual([m for m in self.sent() if m["type"] == "prompt"][0]["step"], "tag")


class ChannelFallbackTest(unittest.TestCase):
    def test_channel_bad_fd_falls_back_inline(self):
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)
        prompt._reset()
        self.addCleanup(prompt._reset)
        os.environ["JUNEAU_PROMPT_FD_OUT"] = "999"
        os.environ["JUNEAU_PROMPT_FD_IN"] = "notanumber"
        err = io.StringIO()
        with contextlib.redirect_stderr(err), mock.patch("builtins.input", side_effect=["typed"]):
            self.assertEqual(prompt.text("T"), "typed")
        self.assertNotIn("JUNEAU_PROMPT_FD_OUT", os.environ)
        self.assertNotIn("notanumber", err.getvalue())

    def test_second_open_failure_closes_first(self):
        out = io.StringIO()
        with mock.patch.object(prompt.os, "fdopen", side_effect=[out, OSError("boom")]), \
                self.assertRaises(OSError):
            prompt._Channel(7, 8)
        self.assertTrue(out.closed)


if __name__ == "__main__":
    unittest.main()
