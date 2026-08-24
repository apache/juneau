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
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Timestamp renderer coverage: source-shape always runs; Node behavioral harness (local + California
 * formatting for a fixed Instant) runs when {@code node} is on {@code PATH}.
 */
class ViewsJs_Renders_Test extends TestBase {

	/** Fixed Instant matching the IRS screenshot: cell {@code 08/20/2026 20:11Z}. */
	static final Instant INSTANT = Instant.parse("2026-08-20T20:11:00Z");

	/** Zone used for California popup lines. */
	static final ZoneId CALIFORNIA = ZoneId.of("America/Los_Angeles");

	/** Zone the Node harness is launched under so local-time assertions are deterministic. */
	static final String LOCAL_TZ = "America/New_York";

	private static String rendersJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.RENDERS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_formattersExportedOnNs() throws Exception {
		var body = rendersJs();
		for (var name : new String[]{
			"formatUtcZulu", "formatLocalTime", "formatCalifornia", "popupLines", "data-juneau-ts"
		})
			assertTrue(body.contains(name), () -> "missing '" + name + "'");
		assertTrue(body.contains("America/Los_Angeles"), body);
		assertTrue(body.contains("local.textContent"), body);
		assertTrue(body.contains("california.textContent"), body);
	}

	@Test void a02_javaZoneContract_californiaIsLosAngeles() {
		var zdt = INSTANT.atZone(CALIFORNIA);
		assertEquals("America/Los_Angeles", CALIFORNIA.getId());
		assertEquals(13, zdt.getHour());  // 20:11Z in August PDT (UTC-7) is 13:11
		assertEquals(11, zdt.getMinute());
		assertTrue(zdt.getZone().getRules().isDaylightSavings(INSTANT));
		assertFalse(Instant.parse("2026-01-15T20:11:00Z").atZone(CALIFORNIA).getZone().getRules()
			.isDaylightSavings(Instant.parse("2026-01-15T20:11:00Z")));
	}

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		try {
			Files.writeString(rendersFile, rendersJs(), UTF_8);
			report = Json.to(runNode(harness, rendersFile), Map.class);
		} finally {
			Files.deleteIfExists(rendersFile);
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
			var p = Path.of(basedir, "src/test/js/renders.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/renders.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/renders.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs) throws Exception {
		var stdout = Files.createTempFile("renders-stdout-", ".json");
		var stderr = Files.createTempFile("renders-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			pb.environment().put("TZ", LOCAL_TZ);
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("renders.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("renders.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or renders.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_utcCell_matchesIrsZuluFormat() {
		assertEquals("08/20/2026\u00a020:11Z", report().get("utc"));
	}

	@Test void b02_localTime_sameDayIsBareTime() {
		assertEquals("4:11 pm", report().get("local"));
		assertEquals("Local time: 4:11 pm", report().get("popupLocal"));
	}

	@Test void b03_california_pdtForAugustInstant() {
		assertEquals("08/20/2026\u00a001:11 pm\u00a0PDT", report().get("california"));
		assertEquals("California: 08/20/2026\u00a001:11 pm\u00a0PDT", report().get("popupCalifornia"));
	}

	@Test void b04_california_pstInJanuary() {
		assertEquals("01/15/2026\u00a012:11 pm\u00a0PST", report().get("janCalifornia"));
	}

	@Test void b05_localTime_prefixesDateOnDayRollover() {
		assertEquals("08/19/2026 11:11 pm", report().get("rolloverLocal"));
	}

	@Test void b06_tsZuluDisplay_wrapsIsoForPopup_unlessMetaOff() {
		var r = report();
		assertEquals(true, r.get("displayHasSpan"));
		assertEquals(true, r.get("displayIsoAttr"));
		assertEquals(true, r.get("displayOffIsPlain"));
		assertEquals("08/20/2026\u00a020:11Z", r.get("displayOffUtc"));
	}

	@Test void b07_datetime_popupIsOptIn() {
		var r = report();
		assertEquals(false, r.get("datetimeDefaultHasSpan"));
		assertEquals(true, r.get("datetimePopupHasSpan"));
	}

	@Test void b08_unparseableValue_isHtmlEscaped() {
		assertEquals("&lt;img src=x onerror=alert(1)&gt;", report().get("xss"));
		assertEquals("", report().get("blank"));
	}

	@Test void c01_progress_unknownIsEmptyTrack_notZero() {
		var r = report();
		assertEquals(true, r.get("progress_null"));
		assertEquals(true, r.get("progress_empty"));
		assertEquals(true, r.get("progress_ws"));
		assertEquals(true, r.get("progress_arr"));
		assertEquals(true, r.get("progress_obj"));
		assertEquals(true, r.get("progress_true"));
		assertEquals(true, r.get("progress_false"));
		assertEquals(true, r.get("progress_inf"));
		assertEquals(true, r.get("progress_ninf"));
		assertEquals(true, r.get("progress_foo"));
		assertEquals(true, r.get("progress_badMax"));
		assertEquals(true, r.get("progress_emptyMax"));
	}

	@Test void c02_progress_zeroAndRatioAndClamp() {
		var r = report();
		assertEquals("0", r.get("progress_zeroWidth"));
		assertEquals(true, r.get("progress_zeroLabel"));
		assertEquals("0", r.get("progress_strZeroWidth"));
		assertEquals("50", r.get("progress_mid"));
		assertEquals("100", r.get("progress_eqMax"));
		assertEquals(true, r.get("progress_eqMaxOk"));
		assertEquals("100", r.get("progress_overWidth"));
		assertEquals(true, r.get("progress_overLabel"));
		assertEquals(true, r.get("progress_overExceeds"));
		assertEquals("0", r.get("progress_negWidth"));
		assertEquals(true, r.get("progress_negLabel"));
		assertEquals("67", r.get("progress_round"));
		assertEquals(true, r.get("progress_roundLabel"));
	}

	@Test void c03_progress_stateClassBoundaries() {
		var r = report();
		assertEquals(true, r.get("progress_warnEq"));
		assertEquals(true, r.get("progress_exceedsEq"));
		assertEquals(true, r.get("progress_warnEqExceeds"));
		assertEquals(true, r.get("progress_exceedsBelowWarn"));
		assertEquals(true, r.get("progress_units"));
	}

	@Test void c04_progress_labelAndField() {
		var r = report();
		assertEquals(true, r.get("progress_labelNone"));
		assertEquals(true, r.get("progress_labelValue"));
		assertEquals(true, r.get("progress_labelBogus"));
		assertEquals(true, r.get("progress_fieldHasProgress"));
		assertEquals(true, r.get("progress_fieldHasCpu"));
		assertEquals(true, r.get("progress_noField"));
		assertEquals(true, r.get("progress_columnIgnored"));
		assertEquals(42, ((Number) r.get("progress_sort")).intValue());
	}

	@Test void c05_progress_hostileInputCanary() {
		var r = report();
		assertEquals(false, r.get("progress_script"));
		assertEquals(false, r.get("progress_onerror"));
		assertEquals(false, r.get("progress_onmouse"));
		assertEquals(true, r.get("progress_hostileEmpty"));
		assertEquals(true, r.get("progress_widthCanary"));
	}

	@Test void c06_sinkFreeze_ignoresRegisterRendererOverride() {
		var r = report();
		assertEquals(true, r.get("freeze_cellHonorsOverride"));
		assertEquals(true, r.get("freeze_sinkStillBuiltin"));
		assertEquals(true, r.get("freeze_sinkDisplaySafe"));
		assertEquals("bool,date,datetime,decimal,json,linked,progress,tag,truncate,ts-zulu", r.get("freeze_ids"));
	}

	// -----------------------------------------------------------------------------------------------------------
	// TODO-445k: pill cell renderer (display-only by default; opt-in action-binding; cell-path only)
	// -----------------------------------------------------------------------------------------------------------

	@Test void d01_pill_displayOnlyChip_dotAndRawValue() {
		var r = report();
		// Raw textContent "ok" (NOT uppercased), themed via .tag.state.ok, dot has no tone class, data-juneau-pill.
		assertEquals(
			"<span class=\"jc-pill tag state ok\" data-juneau-pill>"
				+ "<span class=\"jc-pill-dot\" aria-hidden=\"true\"></span>ok</span>",
			r.get("pill_display"));
		assertEquals("pill-cell", r.get("pill_class"));
	}

	@Test void d02_pill_dotOffDropsTheDot() {
		var r = report();
		assertEquals(
			"<span class=\"jc-pill tag state ok\" data-juneau-pill>ok</span>",
			r.get("pill_dotOff"));
	}

	@Test void d03_pill_toneClassOnlyForOkWarnExceeds() {
		var r = report();
		assertEquals(true, r.get("pill_toneOk"));
		assertEquals(true, r.get("pill_toneWarn"));
		assertEquals(true, r.get("pill_toneExceeds"));
		assertEquals(true, r.get("pill_toneNeutralNoClass"));
		assertEquals(true, r.get("pill_toneInfoNoClass"));   // info is NOT a valid tone (B7-fold)
		assertEquals(true, r.get("pill_toneAbsentNoClass"));
	}

	@Test void d04_pill_actionAddsRoleTabindexAndId_onlyWhenSet() {
		var r = report();
		assertEquals(true, r.get("pill_actionHasRole"));
		assertEquals(true, r.get("pill_actionHasTabindex"));
		assertEquals(true, r.get("pill_actionHasId"));
		assertEquals(true, r.get("pill_noActionNoRole"));
		assertEquals(true, r.get("pill_noSelect"));   // no aria-pressed / select toggle ever (B5/N1-fold)
	}

	@Test void d05_pill_blankValueIsEmpty() {
		var r = report();
		assertEquals("", r.get("pill_blank"));
		assertEquals("", r.get("pill_empty"));
	}

	@Test void d06_pill_hostileInputEscaped() {
		var r = report();
		assertEquals(false, r.get("pill_hostileScript"));
		assertEquals(true, r.get("pill_hostileEscaped"));
	}

	@Test void d07_pill_isNotAFrozenBuiltin_cellPathOnly() {
		var r = report();
		assertEquals(true, r.get("pill_notFrozen"));       // resolveSinkRenderer("pill") == null
		assertEquals(true, r.get("pill_notInFrozenIds"));  // "pill" absent from frozen id snapshot (size stays 10)
	}
}
