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
"""juneau_terminal_scripts.traces: the detector, the allowlist, the log window and the Traces row."""

import io
import os
import sys
import tempfile
import unittest
from contextlib import redirect_stdout, redirect_stderr
from pathlib import Path

import scripts_support  # noqa: F401  (puts src/main/python on sys.path)
from scripts_support import isolate_run_env, reset_run_state

from juneau_terminal_scripts import traces

FIXTURES = Path(__file__).parent / "fixtures" / "traces"
TWO_JS_ERRORS = ("FAIL src/test/js/view_c.test.js\n"
                 "  ● Console\n\n"
                 "    console.error\n"
                 "      Error: expected failure\n"
                 "          at one (/work/app/static/views.js:1:1)\n"
                 "          at two (/work/app/static/views.js:2:2)\n\n"
                 "      TypeError: unexpected failure\n"
                 "          at three (/work/app/static/views.js:3:3)\n"
                 "          at four (/work/app/static/views.js:4:4)\n")


def fixture(name):
    return (FIXTURES / name).read_text(encoding="utf-8")


class ScanCase(unittest.TestCase):

    def test_java_trace_with_http_header(self):
        found = traces.scan(fixture("http-404.log"))
        self.assertEqual([(t.kind, t.header, t.frames) for t in found], [("java", "HTTP/1.1 404 Not Found", 4)])
        self.assertEqual(found[0].context, "WARNING: Error occurred during REST call.")

    def test_caused_by_with_multiline_message_is_one_trace(self):
        found = traces.scan(fixture("caused-by-multiline.log"))
        self.assertEqual(len(found), 1)
        self.assertEqual(found[0].header, "org.apache.juneau.rest.server.RestException: Response failed")
        self.assertEqual(found[0].frames, 4)
        self.assertEqual(found[0].test, "org.apache.juneau.rest.server.DebugMarshalling_Behavior_Test")

    def test_frames_with_trailing_jar_suffix_and_blank_line_before_header(self):
        found = traces.scan(fixture("logback-spring.log"))
        self.assertEqual([(t.header, t.frames) for t in found], [("java.lang.IllegalStateException: boom", 3)])

    def test_inline_u2028_trace(self):
        found = traces.scan(fixture("inline-u2028.log"))
        self.assertEqual([(t.header, t.frames) for t in found], [("java.io.IOException: disk full", 3)])
        self.assertEqual(found[0].context, "WARN  com.example.Cache - lookup failed")

    def test_python_traceback_header_is_the_last_line(self):
        found = traces.scan(fixture("python-keyboard-interrupt.log"))
        self.assertEqual([(t.kind, t.header) for t in found], [("python", "KeyboardInterrupt")])

    def test_jest_block_counts_once_and_lone_location_line_is_not_a_trace(self):
        found = traces.scan(fixture("jest-console.log"))
        self.assertEqual(len(found), 1)
        self.assertEqual(found[0].kind, "js")
        self.assertEqual(found[0].header, "resolveSettings: clearing unreadable settings TypeError: storage.getItem is not a function")
        self.assertEqual(found[0].test, "src/test/js/view_a.test.js")

    def test_prose_and_a_single_frame_are_not_traces(self):
        self.assertEqual(traces.scan(fixture("prose-and-source.log")), [])

    def test_ansi_is_stripped(self):
        text = "\x1b[31mjava.lang.Error: x\x1b[0m\n\tat a.B.c(B.java:1)\n\tat a.B.d(B.java:2)\n"
        self.assertEqual(traces.scan(text)[0].header, "java.lang.Error: x")

    def test_bare_caused_by_is_a_trace(self):
        found = traces.scan("Caused by: java.lang.Error: x\n")
        self.assertEqual([(t.header, t.frames) for t in found], [("Caused by: java.lang.Error: x", 0)])

    def test_two_js_runs_in_one_block_merge_and_keep_both_headers(self):
        found = traces.scan(TWO_JS_ERRORS)
        self.assertEqual(len(found), 1)
        self.assertEqual((found[0].kind, found[0].header, found[0].frames), ("js", "Error: expected failure", 4))
        self.assertEqual(found[0].headers, ("Error: expected failure", "TypeError: unexpected failure"))
        self.assertEqual(found[0].test, "src/test/js/view_c.test.js")

    def test_unmerged_trace_headers_is_its_header(self):
        self.assertEqual(traces.scan(fixture("http-404.log"))[0].headers, ("HTTP/1.1 404 Not Found",))

    def test_python_traceback_is_attributed_from_its_start(self):
        text = ("tests/test_a.py::test_two\n"
                "Traceback (most recent call last):\n"
                "  File \"a.py\", line 1, in <module>\n"
                "    Running com.example.Other_Test\n"
                "ValueError: bad\n")
        found = traces.scan(text)
        self.assertEqual([(t.kind, t.line, t.header) for t in found], [("python", 2, "ValueError: bad")])
        self.assertEqual(found[0].test, "tests/test_a.py::test_two")

    def test_two_traces_are_two(self):
        text = fixture("http-404.log") + "[INFO] ok\n" + fixture("logback-spring.log")
        self.assertEqual(len(traces.scan(text)), 2)


class AllowlistCase(unittest.TestCase):

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)

    def write(self, text):
        path = Path(self.dir.name) / "trace-allowlist.txt"
        path.write_text(text, encoding="utf-8")
        return path

    def test_missing_file_is_empty(self):
        allow = traces.load_allowlist(Path(self.dir.name) / "none.txt")
        self.assertEqual((allow.entries, allow.errors), ((), ()))

    def test_entry_comment_and_blank_lines(self):
        allow = traces.load_allowlist(self.write("# top\n\n^java\\.io\\.IOException  # the cache test asserts it\n"))
        self.assertEqual(allow.errors, ())
        self.assertEqual([(e.source, e.reason, e.line) for e in allow.entries],
                         [("^java\\.io\\.IOException", "the cache test asserts it", 3)])

    def test_hash_inside_a_regex_is_part_of_the_regex(self):
        allow = traces.load_allowlist(self.write("Bean#prop failed  # reason\n"))
        self.assertEqual(allow.entries[0].source, "Bean#prop failed")

    def test_reason_holding_a_hash_splits_on_the_first(self):
        allow = traces.load_allowlist(self.write("^java\\.io\\.IOException  # see issue # 12, then # more\n"))
        self.assertEqual(allow.errors, ())
        self.assertEqual([(e.source, e.reason) for e in allow.entries],
                         [("^java\\.io\\.IOException", "see issue # 12, then # more")])
        self.assertTrue(allow.entries[0].pattern.search("java.io.IOException: disk full"))

    def test_errors_name_file_and_line(self):
        allow = traces.load_allowlist(self.write("no reason here\nbad(  # r\ndup  # a\ndup  # b\nempty #\n"))
        self.assertEqual(len(allow.entries), 1)
        self.assertEqual([e.split(":")[1] for e in allow.errors], ["1", "2", "4", "5"])
        self.assertIn("no `# reason`", allow.errors[0])
        self.assertIn("bad regex", allow.errors[1])
        self.assertIn("duplicate of line 3", allow.errors[2])

    def test_partition_and_stale(self):
        allow = traces.load_allowlist(self.write("IOException  # a\nNeverSeen  # b\n"))
        found = traces.scan(fixture("inline-u2028.log")) + traces.scan(fixture("http-404.log"))
        allowed, denied, used = allow.partition(found)
        self.assertEqual([t.header for t in allowed], ["java.io.IOException: disk full"])
        self.assertEqual([t.header for t in denied], ["HTTP/1.1 404 Not Found"])
        self.assertEqual([e.source for e in allow.stale(used)], ["NeverSeen"])

    def test_merged_trace_with_one_unallowlisted_header_is_denied(self):
        allow = traces.load_allowlist(self.write("^Error: expected failure  # asserted by the view test\n"))
        allowed, denied, used = allow.partition(traces.scan(TWO_JS_ERRORS))
        self.assertEqual((len(allowed), len(denied)), (0, 1))
        self.assertEqual(used, {1})

    def test_merged_trace_with_every_header_allowlisted_is_allowed(self):
        allow = traces.load_allowlist(self.write("^Error: expected failure  # a\n^TypeError: unexpected  # b\n"))
        allowed, denied, used = allow.partition(traces.scan(TWO_JS_ERRORS))
        self.assertEqual((len(allowed), len(denied)), (1, 0))
        self.assertEqual(used, {1, 2})
        self.assertEqual(allow.stale(used), [])


class MarkCase(unittest.TestCase):

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.root = Path(self.dir.name)

    def test_no_log_reads_none(self):
        self.assertIsNone(traces.Mark().read())

    def test_missing_log_reads_none(self):
        self.assertIsNone(traces.Mark(self.root / "gone.log").read())

    def test_unreadable_log_reads_none(self):
        self.assertIsNone(traces.Mark(self.root).read())

    def test_empty_window_reads_empty(self):
        log = self.root / "run.log"
        log.write_text("before\n", encoding="utf-8")
        self.assertEqual(traces.Mark(log, log.stat().st_size).read(), "")


class GateCase(unittest.TestCase):

    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        sys.modules.pop("juneau_terminal", None)
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.root = Path(self.dir.name)
        self.log = self.root / "run.log"
        self.log.write_text("", encoding="utf-8")
        os.environ[traces.FULL_LOG_ENV] = str(self.log)
        self.allow = self.root / "trace-allowlist.txt"
        self.report = self.root / "target" / "trace-guard.txt"

    def gate(self, **kw):
        out, err = io.StringIO(), io.StringIO()
        with redirect_stdout(out), redirect_stderr(err):
            code = traces.gate("traces", "Traces", self.allow, report=self.report, **kw)
        return code, out.getvalue() + err.getvalue()

    def test_window_ignores_what_came_before_the_mark(self):
        self.log.write_text(fixture("http-404.log"), encoding="utf-8")
        since = traces.mark()
        with self.log.open("a", encoding="utf-8") as f:
            f.write("[INFO] clean\n")
        code, out = self.gate(since=since)
        self.assertEqual(code, 0)
        self.assertTrue(self.report.is_file())

    def test_trace_after_the_mark_fails_with_header_and_attribution(self):
        since = traces.mark()
        with self.log.open("a", encoding="utf-8") as f:
            f.write(fixture("caused-by-multiline.log"))
        code, out = self.gate(since=since)
        self.assertEqual(code, 1)
        self.assertIn("RestException: Response failed", out)
        self.assertIn("near org.apache.juneau.rest.server.DebugMarshalling_Behavior_Test", out)
        self.assertIn("NOT ALLOWLISTED: 1", self.report.read_text(encoding="utf-8"))

    def test_warn_only_downgrades(self):
        since = traces.mark()
        self.log.write_text(fixture("http-404.log"), encoding="utf-8")
        code, out = self.gate(since=since, warn_only=True)
        self.assertEqual(code, 0)
        self.assertIn("--warn-traces", out)

    def test_allowlisted_trace_passes_and_stale_entry_warns_only_on_a_full_run(self):
        self.allow.write_text("^HTTP/1\\.1 404  # asserted by the not-found test\nNeverSeen  # gone\n", encoding="utf-8")
        since = traces.mark()
        self.log.write_text(fixture("http-404.log"), encoding="utf-8")
        code, out = self.gate(since=since)
        self.assertEqual(code, 0)
        self.assertIn("Stale trace allowlist entry (line 2): NeverSeen", out)
        code, out = self.gate(since=since, full_run=False)
        self.assertEqual(code, 0)
        self.assertNotIn("Stale", out)

    def test_allowlist_error_fails_even_when_warn_only(self):
        self.allow.write_text("no reason\n", encoding="utf-8")
        since = traces.mark()
        code, out = self.gate(since=since, warn_only=True)
        self.assertEqual(code, 1)
        self.assertIn("no `# reason`", out)

    def test_no_log_and_no_text_is_a_visible_skip(self):
        del os.environ[traces.FULL_LOG_ENV]
        code, out = self.gate(since=traces.mark())
        self.assertEqual(code, 0)
        self.assertIn("Trace guard skipped", out)

    def test_unreadable_log_fails_and_names_it(self):
        since = traces.mark()
        self.log.unlink()
        code, out = self.gate(since=since)
        self.assertEqual(code, 1)
        self.assertIn(f"cannot read the full log {self.log}", out)
        self.assertNotIn("0 traces", out)

    def test_no_log_with_only_extra_is_still_a_skip(self):
        del os.environ[traces.FULL_LOG_ENV]
        code, out = self.gate(since=traces.mark(), extra=(fixture("logback-spring.log"),))
        self.assertEqual(code, 0)
        self.assertIn("Trace guard skipped", out)
        self.assertFalse(self.report.exists())

    def test_merged_trace_report_lists_every_header(self):
        self.allow.write_text("^Error: expected failure  # asserted by the view test\n", encoding="utf-8")
        since = traces.mark()
        self.log.write_text(TWO_JS_ERRORS, encoding="utf-8")
        code, out = self.gate(since=since)
        self.assertEqual(code, 1)
        self.assertIn("Error: expected failure | TypeError: unexpected failure", out)
        self.assertIn("    joined: TypeError: unexpected failure", self.report.read_text(encoding="utf-8"))

    def test_text_is_the_source_when_there_is_no_log(self):
        del os.environ[traces.FULL_LOG_ENV]
        code, _ = self.gate(since=traces.mark(), text=fixture("http-404.log"))
        self.assertEqual(code, 1)

    def test_extra_sources_are_scanned_too(self):
        since = traces.mark()
        code, _ = self.gate(since=since, extra=(fixture("logback-spring.log"),))
        self.assertEqual(code, 1)


if __name__ == "__main__":
    unittest.main()
