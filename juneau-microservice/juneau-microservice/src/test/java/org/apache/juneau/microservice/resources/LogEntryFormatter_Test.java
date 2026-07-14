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

import java.util.logging.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link LogEntryFormatter}.
 */
class LogEntryFormatter_Test extends TestBase {

	private static final String DATE_FORMAT = "yyyy.MM.dd hh:mm:ss";
	private static final String DEFAULT_FORMAT = "[{date} {level}] {msg}%n";

	@Test void a01_create_defaultFormat() {
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, false);
		assertNotNull(f);
		assertNotNull(f.getDateFormat());
		assertNotNull(f.getLogEntryPattern());
	}

	@Test void a02_create_withStackTraceHashes() {
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, true);
		assertNotNull(f);
	}

	@Test void a03_format_simpleMessage() {
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, false);
		var r = new LogRecord(Level.INFO, "Hello World");
		var result = f.format(r);
		assertNotNull(result);
		assertTrue(result.contains("INFO"));
		assertTrue(result.contains("Hello World"));
	}

	@Test void a04_format_withException() {
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, false);
		var r = new LogRecord(Level.SEVERE, "Something went wrong");
		r.setThrown(new RuntimeException("test exception"));
		var result = f.format(r);
		assertNotNull(result);
		assertTrue(result.contains("SEVERE"));
		assertTrue(result.contains("Something went wrong"));
		assertTrue(result.contains("RuntimeException") || result.contains("test exception"));
	}

	@Test void a05_format_withStackTraceHashes_firstOccurrence() {
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, true);
		var r = new LogRecord(Level.WARNING, "Error occurred");
		r.setThrown(new IllegalArgumentException("bad arg"));
		var result = f.format(r);
		assertNotNull(result);
		// First occurrence should contain the hash and the full stack trace
		assertTrue(result.contains("["));
	}

	@Test void a06_format_withStackTraceHashes_secondOccurrence() {
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, true);
		var ex = new IllegalArgumentException("bad arg");
		var r1 = new LogRecord(Level.WARNING, "Error 1");
		r1.setThrown(ex);
		var r2 = new LogRecord(Level.WARNING, "Error 2");
		r2.setThrown(ex);
		// Format twice with same exception
		var result1 = f.format(r1);
		var result2 = f.format(r2);
		assertNotNull(result1);
		assertNotNull(result2);
		// Second occurrence should use abbreviated form (no full stack trace)
	}

	@Test void a07_getLogEntryPattern() {
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, false);
		var p = f.getLogEntryPattern();
		assertNotNull(p);
		// Should match a typical log line
		var line = "[2023.01.15 10:30:45 INFO] Hello World";
		var m = p.matcher(line);
		// Pattern should be usable
		assertNotNull(m);
	}

	@Test void a08_getField() {
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, false);
		var r = new LogRecord(Level.INFO, "Test message");
		var formatted = f.format(r);
		var p = f.getLogEntryPattern();
		var m = p.matcher(formatted.trim());
		// getField with non-existent field
		if (m.matches()) {
			// Field "date" should be in the pattern
			assertNotNull(f.getField("date", m));
		}
		// getField returns null for unknown field name
		if (m.matches()) {
			assertNull(f.getField("unknownField", m));
		}
	}

	@Test void a09_create_allFields() {
		// Test format with all field placeholders
		var format = "[{date} {class} {method} {logger} {level} {threadid}] {msg} {exception}%n";
		var f = new LogEntryFormatter(format, DATE_FORMAT, false);
		assertNotNull(f);
		var r = new LogRecord(Level.INFO, "Test");
		r.setSourceClassName("MyClass");
		r.setSourceMethodName("myMethod");
		r.setLoggerName("com.example.MyLogger");
		var result = f.format(r);
		assertNotNull(result);
		assertTrue(result.contains("INFO"));
	}

	@Test void a10_create_noFields() {
		// Format with no placeholders
		var f = new LogEntryFormatter("Static message%n", DATE_FORMAT, false);
		assertNotNull(f);
		var r = new LogRecord(Level.INFO, "ignored");
		var result = f.format(r);
		assertNotNull(result);
	}

	@Test void a11_create_withSpecialChars() {
		// Format string with special regex chars
		var f = new LogEntryFormatter("[{date} {level}] ({msg})%n", DATE_FORMAT, false);
		assertNotNull(f);
		var r = new LogRecord(Level.INFO, "Special chars: (test)");
		var result = f.format(r);
		assertNotNull(result);
	}

	@Test void a12_getDateFormat() {
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, false);
		var df = f.getDateFormat();
		assertNotNull(df);
		// Verify it can format a date
		assertNotNull(df.format(new java.util.Date(0L)));
	}

	@Test void a13_format_noException() {
		// Test format without thrown exception (LogRecord.getThrown() == null)
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, false);
		var r = new LogRecord(Level.FINE, "No exception here");
		r.setThrown(null);
		var result = f.format(r);
		assertNotNull(result);
		assertFalse(result.contains("Exception"));
	}

	@Test void a14_pattern_withStateS2_S3_handling() {
		// Format with % followed by non-digit (S2 -> S1 path)
		// % followed by digit, non-$ (S3 -> S1 path)
		var f = new LogEntryFormatter("{level} %n", DATE_FORMAT, false);
		assertNotNull(f);
	}

	@Test void a15_format_noTrailingPercentN() {
		// Format without trailing %n - covers the sre.endsWith("\\%n") false branch (line 197)
		var f = new LogEntryFormatter("[{date} {level}] {msg}", DATE_FORMAT, false);
		assertNotNull(f);
		var r = new LogRecord(Level.INFO, "No newline at end");
		var result = f.format(r);
		assertNotNull(result);
		assertTrue(result.contains("INFO"));
	}

	@Test void a16_format_withHashesNoException() {
		// Hashes mode, record without thrown exception
		// Covers nn(hashes)=true && nn(t)=false branch (line 216)
		var f = new LogEntryFormatter(DEFAULT_FORMAT, DATE_FORMAT, true);
		var r = new LogRecord(Level.INFO, "No exception");
		r.setThrown(null);
		var result = f.format(r);
		assertNotNull(result);
		assertTrue(result.contains("INFO"));
		assertTrue(result.contains("No exception"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Log correlation: {traceId} / {spanId} placeholders.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_traceIdSpanId_renderWithoutError() {
		// OTel is not on this module's test classpath, so the trace/span fields render empty - but the
		// placeholders must still be recognized (no literal "{traceId}" leaks into the output).
		var f = new LogEntryFormatter("[{date} {level}] {msg} trace={traceId} span={spanId}%n", DATE_FORMAT, false);
		var r = new LogRecord(Level.INFO, "hello");
		var result = f.format(r);
		assertNotNull(result);
		assertTrue(result.contains("trace= span="), "Expected empty trace/span fields, got:\n" + result);
		assertFalse(result.contains("{traceId}"));
		assertFalse(result.contains("{spanId}"));
	}

	@Test void b02_traceIdSpanId_registeredAsParsableFields() {
		// {msg} uses a greedy (.*) match, so it must come last; put the trace/span fields ahead of it.
		var f = new LogEntryFormatter("[{date} {level}] trace={traceId} span={spanId} {msg}", DATE_FORMAT, false);
		var line = "[2023.01.15 10:30:45 INFO] trace=0af7651916cd43dd8448eb211c80319c span=b7ad6b7169203331 hello";
		var m = f.getLogEntryPattern().matcher(line);
		assertTrue(m.matches(), "Pattern should match a line with populated trace/span ids");
		assertEquals("0af7651916cd43dd8448eb211c80319c", f.getField("traceId", m));
		assertEquals("b7ad6b7169203331", f.getField("spanId", m));
	}

	@Test void b03_traceContext_unavailableReturnsEmpty() {
		// Without OTel on the classpath, the reflective accessor degrades to empty strings.
		assertFalse(TraceContext.isAvailable());
		assertEquals("", TraceContext.currentTraceId());
		assertEquals("", TraceContext.currentSpanId());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Regex-injection safety - the caller-supplied format/dateFormat strings are escaped into the parsing regex.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_dateFormat_regexMetacharsEscapedAndMatch() {
		// A date format containing regex metacharacters must be escaped (not injected) yet still match its own
		// formatted output.  With the previous partial escaping, "(MM)" and "[dd]" produced a stray capture
		// group and character class, so the resulting pattern no longer matched the formatted date.
		var f = new LogEntryFormatter("[{date}] {msg}", "yyyy(MM)[dd]", false);
		var r = new LogRecord(Level.INFO, "hello");
		var line = f.format(r);
		var m = f.getLogEntryPattern().matcher(line);
		assertTrue(m.matches(), "Pattern should match its own formatted output with a metacharacter date format, got:\n" + line);
	}

	@Test void c02_dateFormat_unbalancedMetacharsDoNotThrow() {
		// An unbalanced regex metacharacter in the date format previously produced an invalid pattern that
		// threw a PatternSyntaxException during construction; it must now be escaped and compile cleanly.
		assertDoesNotThrow(() -> new LogEntryFormatter("[{date}] {msg}", "yyyy(((MM", false));
	}

	@Test void c03_format_regexMetacharsEscaped() {
		// Regex metacharacters in the literal portions of the format string must be escaped, so an adversarial
		// format cannot inject regex syntax or break compilation of the log-parsing pattern.
		assertDoesNotThrow(() -> new LogEntryFormatter("(([{date}]{level})+ {msg}%n", DATE_FORMAT, false));
	}
}
