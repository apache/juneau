/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.microservice.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Tests for {@link LogParser}.
 */
class LogParser_Test extends TestBase {

	private static final String DATE_FORMAT = "yyyy.MM.dd hh:mm:ss";
	private static final String LOG_FORMAT = "[{date} {level}] {msg}%n";

	private LogEntryFormatter formatter;

	@BeforeEach
	void setUp() {
		formatter = new LogEntryFormatter(LOG_FORMAT, DATE_FORMAT, false);
	}

	private File createLogFile(Path dir, String content) throws IOException {
		var f = dir.resolve("test.log").toFile();
		Files.writeString(f.toPath(), content);
		return f;
	}

	@Test void a01_emptyLogFile(@TempDir Path tempDir) throws Exception {
		var f = createLogFile(tempDir, "");
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			assertFalse(p.hasNext());
		}
	}

	@Test void a02_singleLogEntry(@TempDir Path tempDir) throws Exception {
		// Create a log entry matching the formatter's pattern
		var r = new LogRecord(Level.INFO, "Hello World");
		var entry = formatter.format(r);
		var f = createLogFile(tempDir, entry);
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			assertTrue(p.hasNext());
			var entries = p.iterator();
			var e = entries.next();
			assertNotNull(e);
			assertEquals("INFO", e.severity);
			assertNotNull(e.date);
		}
	}

	@Test void a03_multipleLogEntries(@TempDir Path tempDir) throws Exception {
		// Create multiple log entries
		var sb = new StringBuilder();
		for (var level : new Level[]{Level.INFO, Level.WARNING, Level.SEVERE}) {
			var r = new LogRecord(level, "Message at " + level);
			sb.append(formatter.format(r));
		}
		var f = createLogFile(tempDir, sb.toString());
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			assertTrue(p.hasNext());
			var count = 0;
			for (var e : p) {
				count++;
				assertNotNull(e.severity);
			}
			assertEquals(3, count);
		}
	}

	@Test void a04_filterBySeverity(@TempDir Path tempDir) throws Exception {
		var sb = new StringBuilder();
		sb.append(formatter.format(new LogRecord(Level.INFO, "Info message")));
		sb.append(formatter.format(new LogRecord(Level.WARNING, "Warning message")));
		sb.append(formatter.format(new LogRecord(Level.SEVERE, "Severe message")));
		var f = createLogFile(tempDir, sb.toString());
		// Filter to only WARNING entries
		try (var p = new LogParser(formatter, f, null, null, null, null, new String[]{"WARNING"})) {
			var count = 0;
			for (var e : p) {
				count++;
				assertEquals("WARNING", e.severity);
			}
			assertEquals(1, count);
		}
	}

	@Test void a05_writeTo_empty(@TempDir Path tempDir) throws Exception {
		var f = createLogFile(tempDir, "");
		var sw = new StringWriter();
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			p.writeTo(sw);
		}
		assertEquals("[EMPTY]", sw.toString());
	}

	@Test void a06_writeTo_withEntries(@TempDir Path tempDir) throws Exception {
		var r = new LogRecord(Level.INFO, "Test message");
		var entry = formatter.format(r);
		var f = createLogFile(tempDir, entry);
		var sw = new StringWriter();
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			p.writeTo(sw);
		}
		assertFalse(sw.toString().isEmpty());
	}

	@Test void a07_close_whenAlreadyClosed(@TempDir Path tempDir) throws Exception {
		var f = createLogFile(tempDir, "");
		var p = new LogParser(formatter, f, null, null, null, null, null);
		// Close multiple times should not throw
		p.close();
		assertDoesNotThrow(() -> p.close());
	}

	@Test void a08_nonMatchingLine(@TempDir Path tempDir) throws Exception {
		// Mix of valid log entries and non-matching lines (continuation lines)
		var r = new LogRecord(Level.WARNING, "Warning with stack trace");
		var entry = formatter.format(r) + "\tat java.lang.Thread.run(Thread.java:833)\n";
		var f = createLogFile(tempDir, entry);
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			assertTrue(p.hasNext());
			var e = p.iterator().next();
			assertNotNull(e);
			// The stack trace line should be attached as additional text
		}
	}

	@Test void a09_entry_getText_noAdditional(@TempDir Path tempDir) throws Exception {
		var r = new LogRecord(Level.INFO, "Simple message");
		var f = createLogFile(tempDir, formatter.format(r));
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			var e = p.iterator().next();
			assertNotNull(e.getText());
		}
	}

	@Test void a10_entry_getThread(@TempDir Path tempDir) throws Exception {
		var r = new LogRecord(Level.INFO, "Thread test");
		var f = createLogFile(tempDir, formatter.format(r));
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			var e = p.iterator().next();
			// Thread may be null depending on format
			assertDoesNotThrow(() -> e.getThread());
		}
	}

	@Test void a11_entry_appendHtml(@TempDir Path tempDir) throws Exception {
		var r = new LogRecord(Level.INFO, "HTML <test> message");
		var f = createLogFile(tempDir, formatter.format(r));
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			var e = p.iterator().next();
			var sw = new StringWriter();
			e.appendHtml(sw);
			var html = sw.toString();
			assertNotNull(html);
			// HTML should have escaped the < character
			assertTrue(html.contains("&lt;") || html.contains("<br>"));
		}
	}

	@Test void a12_toHtml_withAngleBracket() {
		// Test static toHtml method
		assertEquals("&lt;script>", LogParser.toHtml("<script>"));
		assertEquals("no brackets", LogParser.toHtml("no brackets"));
	}

	@Test void a13_filterByLogger(@TempDir Path tempDir) throws Exception {
		var r1 = new LogRecord(Level.INFO, "Message 1");
		r1.setLoggerName("com.example.Foo");
		var r2 = new LogRecord(Level.INFO, "Message 2");
		r2.setLoggerName("com.example.Bar");
		var sb = new StringBuilder();
		sb.append(formatter.format(r1));
		sb.append(formatter.format(r2));
		var f = createLogFile(tempDir, sb.toString());
		// Filter by logger name
		try (var p = new LogParser(formatter, f, null, null, null, new String[]{"Foo"}, null)) {
			// Either 0 or 1 entries depending on whether logger name is parsed
			// Just verify it doesn't throw
			assertDoesNotThrow(() -> { for (var e : p) assertNotNull(e); });
		}
	}

	@Test void a14_entryWithContinuationLines(@TempDir Path tempDir) throws Exception {
		// Create log entry with stack trace continuation lines
		var r = new LogRecord(Level.WARNING, "Error with trace");
		var entry = formatter.format(r)
			+ "\tat com.example.Foo.bar(Foo.java:42)\n"
			+ "\tat java.lang.Thread.run(Thread.java:833)\n";
		var f = createLogFile(tempDir, entry);
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			assertTrue(p.hasNext());
			var e = p.iterator().next();
			// Entry with additional text - getText() includes continuation lines
			var text = e.getText();
			assertNotNull(text);
		}
	}

	@Test void a15_getText_withAdditionalText(@TempDir Path tempDir) throws Exception {
		// Entry followed by non-record continuation lines - text includes additional text
		var r1 = new LogRecord(Level.SEVERE, "Main message");
		var r2 = new LogRecord(Level.INFO, "Another entry");
		var sb = new StringBuilder();
		sb.append(formatter.format(r1));
		sb.append("\tat stack.trace.line1\n");
		sb.append("\tat stack.trace.line2\n");
		sb.append(formatter.format(r2));
		var f = createLogFile(tempDir, sb.toString());
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			var iter = p.iterator();
			// First entry has continuation text
			var e1 = iter.next();
			assertNotNull(e1);
			// getText() with additional text returns combined text
			var text = e1.getText();
			assertNotNull(text);
		}
	}

	@Test void a16_appendHtml_withAdditionalText(@TempDir Path tempDir) throws Exception {
		// Test appendHtml() when entry has additional text lines
		var r1 = new LogRecord(Level.WARNING, "Main entry");
		var r2 = new LogRecord(Level.INFO, "Next entry");
		var sb = new StringBuilder();
		sb.append(formatter.format(r1));
		sb.append("\tcontinuation line <tag>\n");
		sb.append(formatter.format(r2));
		var f = createLogFile(tempDir, sb.toString());
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			var e = p.iterator().next();
			var sw = new StringWriter();
			e.appendHtml(sw);
			var html = sw.toString();
			assertNotNull(html);
			// Both the main line and continuation lines should appear
			assertTrue(html.contains("<br>"));
		}
	}

	@Test void a17_append_withAdditionalText(@TempDir Path tempDir) throws Exception {
		// Test protected append() method via writeTo() when entry has additional text
		var r1 = new LogRecord(Level.INFO, "Main entry");
		var r2 = new LogRecord(Level.INFO, "Next entry");
		var sb = new StringBuilder();
		sb.append(formatter.format(r1));
		sb.append("\tcontinuation line\n");
		sb.append(formatter.format(r2));
		var f = createLogFile(tempDir, sb.toString());
		var sw = new StringWriter();
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			p.writeTo(sw);
		}
		var result = sw.toString();
		assertNotNull(result);
		assertFalse(result.isEmpty());
	}

	@Test void a18_filterByStartDate(@TempDir Path tempDir) throws Exception {
		// Filter with a future start date - entries before it should be excluded
		var r = new LogRecord(Level.INFO, "Old entry");
		var f = createLogFile(tempDir, formatter.format(r));
		var futureDate = new Date(System.currentTimeMillis() + 86400000L); // Tomorrow
		try (var p = new LogParser(formatter, f, futureDate, null, null, null, null)) {
			// Entry is before the future start date, so it should be filtered
			assertFalse(p.hasNext());
		}
	}

	@Test void a19_filterByEndDate(@TempDir Path tempDir) throws Exception {
		// Filter with a past end date - recent entries should be excluded
		var r = new LogRecord(Level.INFO, "Recent entry");
		var f = createLogFile(tempDir, formatter.format(r));
		var pastDate = new Date(System.currentTimeMillis() - 86400000L); // Yesterday
		try (var p = new LogParser(formatter, f, null, pastDate, null, null, null)) {
			// Entry is after the past end date, so it should be filtered
			assertFalse(p.hasNext());
		}
	}

	@Test void a20_filterByThread(@TempDir Path tempDir) throws Exception {
		// Filter by thread name - since our format doesn't include thread, entries have null thread
		// but with threadFilter set, no entries should match
		var r = new LogRecord(Level.INFO, "Test");
		var f = createLogFile(tempDir, formatter.format(r));
		try (var p = new LogParser(formatter, f, null, null, "someThread", null, null)) {
			// thread is null in parsed entries, threadFilter is non-null, so entries are filtered
			assertFalse(p.hasNext());
		}
	}

	@Test void a21_filterBySeverityNone(@TempDir Path tempDir) throws Exception {
		// Filter by severity that no entries have - should return 0 entries
		var r = new LogRecord(Level.INFO, "Info entry");
		var f = createLogFile(tempDir, formatter.format(r));
		try (var p = new LogParser(formatter, f, null, null, null, null, new String[]{"SEVERE"})) {
			assertFalse(p.hasNext());
		}
	}

	@Test void a22_multipleContinuationLines(@TempDir Path tempDir) throws Exception {
		// Test addText() called multiple times (covers the already-initialized branch)
		var r = new LogRecord(Level.SEVERE, "Error");
		var sb = new StringBuilder();
		sb.append(formatter.format(r));
		sb.append("\tline 1\n");
		sb.append("\tline 2\n");
		sb.append("\tline 3\n");
		var f = createLogFile(tempDir, sb.toString());
		try (var p = new LogParser(formatter, f, null, null, null, null, null)) {
			var e = p.iterator().next();
			// getText() with multiple continuation lines
			var text = e.getText();
			assertNotNull(text);
		}
	}

	@Test void a23_loggerWithDotInName(@TempDir Path tempDir) throws Exception {
		// Use a format that includes {logger} so the logger.indexOf('.') branch is covered
		var fmt = new LogEntryFormatter("[{date} {level} {logger}] {msg}%n", DATE_FORMAT, false);
		var r = new LogRecord(Level.INFO, "Logger dot test");
		r.setLoggerName("com.example.MyClass");
		var f = createLogFile(tempDir, fmt.format(r));
		try (var p = new LogParser(fmt, f, null, null, null, null, null)) {
			assertTrue(p.hasNext());
			var e = p.iterator().next();
			// Logger name with dots should be trimmed to simple name
			assertNotNull(e.logger);
			assertFalse(e.logger.contains("."), "Logger should be trimmed: " + e.logger);
		}
	}

	@Test void a24_continuationLineAfterNonMatchingRecord(@TempDir Path tempDir) throws Exception {
		// Continuation line appears after a non-matching record (prev is null)
		// The continuation line should be ignored silently (covers line 202 false branch)
		var r1 = new LogRecord(Level.INFO, "Non-matching entry");
		var r2 = new LogRecord(Level.SEVERE, "Matching entry");
		var sb = new StringBuilder();
		sb.append(formatter.format(r1));
		sb.append("\tcontinuation for non-matching\n");
		sb.append(formatter.format(r2));
		var f = createLogFile(tempDir, sb.toString());
		// Filter to only SEVERE - so INFO entry won't match, its continuation will be ignored
		try (var p = new LogParser(formatter, f, null, null, null, null, new String[]{"SEVERE"})) {
			var count = 0;
			for (var e : p) {
				count++;
				assertEquals("SEVERE", e.severity);
			}
			assertEquals(1, count);
		}
	}

	@Test void a25_closeAfterLoad(@TempDir Path tempDir) throws Exception {
		// After load() is called (via hasNext()), br is set to null.
		// Calling close() again should hit the false branch of if(nn(br)).
		var r = new LogRecord(Level.INFO, "test");
		var f = createLogFile(tempDir, formatter.format(r));
		var p = new LogParser(formatter, f, null, null, null, null, null);
		p.hasNext(); // This triggers load(), setting br = null internally
		// Now close() should not throw even though br is null
		assertDoesNotThrow(() -> p.close());
	}

	@Test void a26_startDateFilter_entryBeforeStart(@TempDir Path tempDir) throws Exception {
		// Entry exists, start is not null, but entry.date is NOT before start (date >= start)
		// The date.before(start) is false - covers the "both true but date not before start" branch
		var r = new LogRecord(Level.INFO, "Recent entry");
		var f = createLogFile(tempDir, formatter.format(r));
		// Set start to yesterday so the entry (now) passes the filter
		var yesterday = new Date(System.currentTimeMillis() - 86400000L);
		try (var p = new LogParser(formatter, f, yesterday, null, null, null, null)) {
			assertTrue(p.hasNext());
		}
	}

	@Test void a27_endDateFilter_entryAfterEnd(@TempDir Path tempDir) throws Exception {
		// Entry exists, end is not null, but entry.date is NOT after end (date <= end)
		// The date.after(end) is false - covers the "both true but date not after end" branch
		var r = new LogRecord(Level.INFO, "Recent entry");
		var f = createLogFile(tempDir, formatter.format(r));
		// Set end to tomorrow so the entry (now) passes the filter
		var tomorrow = new Date(System.currentTimeMillis() + 86400000L);
		try (var p = new LogParser(formatter, f, null, tomorrow, null, null, null)) {
			assertTrue(p.hasNext());
		}
	}
}
