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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for the {@code juneau-calendar.js} runtime.  The source-shape layer always runs (the pure
 * helpers must be exported on {@code JuneauCalendar.pure} and event fill must be {@code textContent}, never
 * {@code innerHTML}); the behavioral Node harness runs when {@code node} is on {@code PATH} (skipped otherwise —
 * no {@code -Pjs-tests} required).
 */
class CalendarJs_Test extends TestBase {

	private static String calendarJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.CALENDAR_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.CALENDAR_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_pureHelpersExportedOnNamespace() throws Exception {
		var body = calendarJs();
		for (var name : new String[]{
			"pad2: pad2",
			"daysInMonth: daysInMonth",
			"dayOfWeek: dayOfWeek",
			"firstWeekdayOffset: firstWeekdayOffset",
			"toEpochDay: toEpochDay",
			"fromEpochDay: fromEpochDay",
			"dateKey: dateKey",
			"buildMonthCells: buildMonthCells",
			"civilKey: civilKey",
			"contractOk: contractOk",
			"echoOk: echoOk",
			"sanitizeEvents: sanitizeEvents",
			"colorToken: colorToken",
			"isSafeDocumentUrl: isSafeDocumentUrl",
			"substituteEndpoint: substituteEndpoint",
			"eventsForDay: eventsForDay",
			"applyCap: applyCap",
			"coalesceKey: coalesceKey"
		})
			assertTrue(body.contains(name), () -> "missing pure export '" + name + "'");
		// The DOM entry points are exposed for the harness/browser runtime.
		assertTrue(body.contains("NS.initInstance = initInstance"), body);
		assertTrue(body.contains("NS.fillEventNode = fillEventNode"), body);
	}

	/** Strips {@code //} line comments and block comments so shape assertions see executable code only. */
	private static String stripComments(String js) {
		return js.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
	}

	@Test void a02_eventFill_usesTextContent_neverInnerHtml() throws Exception {
		var body = calendarJs();
		// Security-critical: chip/title text goes in via textContent so an "<img onerror>" title stays literal.
		assertTrue(body.contains("node.textContent = event.title"), body);
		var code = stripComments(body);
		assertFalse(code.contains("innerHTML"), "runtime must never assign innerHTML: " + code);
	}

	@Test void a03_civilDate_neverDateParse() throws Exception {
		// Civil-date bucketing must be field-wise; Date.parse of a date-only string would timezone-shift it.
		var code = stripComments(calendarJs());
		assertFalse(code.contains("Date.parse"), code);
		assertFalse(code.contains("new Date("), code);
	}

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var calendarFile = Files.createTempFile("juneau-calendar-", ".js");
		try {
			Files.writeString(calendarFile, calendarJs(), UTF_8);
			report = Json.to(runNode(harness, calendarFile), Map.class);
		} finally {
			Files.deleteIfExists(calendarFile);
		}
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/juneau-calendar.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/juneau-calendar.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/juneau-calendar.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path calendarJs) throws Exception {
		var stdout = Files.createTempFile("juneau-calendar-stdout-", ".json");
		var stderr = Files.createTempFile("juneau-calendar-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), calendarJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("juneau-calendar.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("juneau-calendar.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
					+ "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	private static String quietRead(Path p) {
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}

	private static Map<?,?> report() {
		assumeTrue(report != null, "node not available or juneau-calendar.cjs not found — behavioral layer skipped");
		return report;
	}

	private static void assertNum(long expected, Object actual) {
		assertInstanceOf(Number.class, actual, () -> "expected a number, got: " + actual);
		assertEquals(expected, ((Number)actual).longValue());
	}

	@Test void b01_contractAndMinPoll() {
		var r = report();
		assertEquals("1", r.get("contractVersion"));
		assertNum(5000, r.get("minPoll"));
	}

	@Test void b02_daysInMonth_leapYearCorrect() {
		var r = report();
		assertNum(29, r.get("dim_feb2024"));
		assertNum(28, r.get("dim_feb2026"));
		assertNum(30, r.get("dim_apr"));
		assertNum(31, r.get("dim_jan"));
	}

	@Test void b03_firstWeekdayOffset_bothWeekStarts() {
		var r = report();
		assertNum(6, r.get("dow_aug1"));       // Aug 1 2026 is a Saturday
		assertNum(6, r.get("off_sunday"));
		assertNum(5, r.get("off_monday"));
	}

	@Test void b04_buildMonthCells_always42_adjacentTagged() {
		var r = report();
		assertNum(42, r.get("cells_count"));
		assertNum(42, r.get("cellsMon_count"));
		assertEquals("2026-08-01", r.get("cells_firstInMonth"));
		assertEquals(true, r.get("cells_leadingAdjacent"));
		assertEquals(true, r.get("cells_firstOfMonthAt6"));       // Sunday-start: 6 leading adjacent cells
		assertEquals(true, r.get("cellsMon_firstOfMonthAt5"));    // Monday-start: 5 leading adjacent cells
		assertNum(31, r.get("cells_inMonthCount"));
	}

	@Test void b05_civilBucketing_neverDateParseShift() {
		var r = report();
		assertEquals("2026-08-14", r.get("civil_dateOnly"));
		assertEquals("2026-08-14", r.get("civil_dateTime"));      // leading date only, no tz shift
		assertNull(r.get("civil_bad"));
		assertNull(r.get("civil_short"));
		assertNull(r.get("civil_badSep"));
	}

	@Test void b06_contractHandshake_strictStringOne() {
		var r = report();
		assertEquals(true, r.get("contract_okStr"));
		assertEquals(false, r.get("contract_badNum"));           // numeric 1 must fail strict ===
		assertEquals(false, r.get("contract_bad2"));
	}

	@Test void b07_echoCheck() {
		var r = report();
		assertEquals(true, r.get("echo_ok"));
		assertEquals(false, r.get("echo_badMonth"));
		assertEquals(false, r.get("echo_null"));
	}

	@Test void b08_sanitize_dropsMissingAndDupWithWarn() {
		var r = report();
		assertEquals("a,b", r.get("sanitize_ids"));
		assertEquals(true, r.get("sanitize_warned"));
	}

	@Test void b09_colorToken_unknownFallsToNeutralWithWarn() {
		var r = report();
		assertEquals("blue", r.get("color_known"));
		assertEquals("neutral", r.get("color_unknown"));
		assertEquals(true, r.get("color_unknownWarned"));
		assertEquals("neutral", r.get("color_none"));
	}

	@Test void b10_documentUrlSafety() {
		var r = report();
		assertEquals(true, r.get("url_path"));
		assertEquals(true, r.get("url_rel"));
		assertEquals(false, r.get("url_abs"));
		assertEquals(false, r.get("url_protoRel"));
		assertEquals(false, r.get("url_scheme"));
		assertEquals(false, r.get("url_dotdot"));
	}

	@Test void b11_substituteAndCapAndCoalesceKey() {
		var r = report();
		assertEquals("/events/2026/8", r.get("sub"));
		assertEquals("1,2", r.get("eventsForDay_ids"));          // sorted by start ascending
		assertNum(3, r.get("cap_shown"));
		assertNum(2, r.get("cap_overflow"));
		assertEquals("cal1:2026-8:4", r.get("coalesce"));
	}

	@Test void b12_eventFill_usesTextContent_noMarkup() {
		var r = report();
		assertEquals("SPAN", r.get("fill_tag"));
		assertTrue(String.valueOf(r.get("fill_text")).contains("<img"));   // literal text, not parsed markup
		assertNum(0, r.get("fill_noChildEls"));
		assertEquals("jc-cal-event jc-cal-cat--blue", r.get("fill_class"));
		assertEquals("A", r.get("fill_linkedTag"));
		assertEquals("/events/1", r.get("fill_linkedHref"));
		assertEquals("SPAN", r.get("fill_unsafeTag"));            // unsafe href not linked
		assertEquals(true, r.get("fill_unsafeNoHref"));
	}

	@Test void b13_readCategoryMap_skipsColumnHeader() {
		var r = report();
		assertEquals("blue", r.get("map_team"));
		assertEquals("green", r.get("map_review"));
		assertEquals(true, r.get("map_noHeader"));
	}

	@Test void b14_seedMonth_paintedFromSidecar_noFetch() {
		var r = report();
		assertNum(6, r.get("seed_weeks"));
		assertEquals(true, r.get("seed_painted"));
		assertEquals(true, r.get("seed_noFetch"));
		assertEquals(true, r.get("seed_todayCell"));
	}

	@Test void b15_contractMismatch_failsLoud_noFetchNoPaint() {
		var r = report();
		assertEquals(true, r.get("badContract_error"));
		assertEquals(true, r.get("badContract_noFetch"));
		assertEquals(true, r.get("badContract_notInit"));
		assertEquals(true, r.get("badContract_loud"));
	}

	@Test void b16_liveBodyNumericContract_refused() {
		var r = report();
		assertEquals(true, r.get("liveNumeric_error"));
	}

	@Test void b17_echoCheck_wrongMonthBodyDropped() {
		var r = report();
		assertEquals(true, r.get("echo_dropped"));
	}

	@Test void b18_coalesceAndAbort_staleMonthDropped() {
		var r = report();
		assertEquals(true, r.get("coalesce_aborted"));
		assertEquals(true, r.get("coalesce_octPainted"));
		assertEquals(true, r.get("coalesce_staleDropped"));
	}

	@Test void b19_fetchError_singleAttempt_emptyMonthVisibleError() {
		var r = report();
		assertEquals(true, r.get("err_visible"));
		assertEquals(true, r.get("err_singleAttempt"));
		assertEquals(true, r.get("err_emptyMonth"));
		assertEquals(true, r.get("html_error"));
	}
}
