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
 * Behavioral coverage for {@code buildPagingPill} nav handlers against DataTables 2.1.8
 * {@code draw(resetPaging)}.  A Node fake models the default {@code resetPaging=true} (jump to page 0)
 * versus {@code draw(false)} (hold the page {@code page("next")} just set).  Source-shape pins live in
 * {@link PagingPill_Wiring_Test}.  Gated on {@code node} being on {@code PATH}.
 */
class ViewsJs_PagingPillDraw_Test extends TestBase {

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String rendersJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		try {
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			Files.writeString(rendersFile, rendersJs(), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
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
			var p = Path.of(basedir, "src/test/js/paging-pill-draw.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/paging-pill-draw.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/paging-pill-draw.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("paging-pill-draw-stdout-", ".json");
		var stderr = Files.createTempFile("paging-pill-draw-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("paging-pill-draw.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("paging-pill-draw.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or paging-pill-draw.cjs not found — behavioral layer skipped");
		return report;
	}

	private static int num(Map<?,?> r, String key) {
		return ((Number)r.get(key)).intValue();
	}

	@Test void a01_fakeModelsDt218ResetVersusHold() {
		var r = report();
		assertEquals(true, r.get("hasBuildPagingPill"));
		// Same probe as Scripts (69 rows, length 25): no-arg draw() jumps back to 0; draw(false) holds.
		assertEquals(0, num(r, "control_drawNoArg_page"), r::toString);
		assertEquals(1, num(r, "control_drawFalse_page"), r::toString);
		assertEquals(0, num(r, "control_lastDrawNoArg_page"), r::toString);
		assertEquals(2, num(r, "control_lastDrawFalse_page"), r::toString);
	}

	@Test void b01_nextAndLastHoldThePageTheHandlerJustSet() {
		var r = report();
		assertEquals(0, num(r, "initial_page"), r::toString);
		assertEquals("1-25 of 69", r.get("initial_summary"));
		assertEquals(false, r.get("initial_nextDisabled"));
		assertEquals(false, r.get("initial_lastDisabled"));
		assertEquals(1, num(r, "next_page"), r::toString);
		assertEquals(true, r.get("next_drawArgIsFalse"));
		assertEquals("26-50 of 69", r.get("next_summary"));
		assertEquals(2, num(r, "last_page"), r::toString);
		assertEquals(true, r.get("last_drawArgIsFalse"));
		assertEquals("51-69 of 69", r.get("last_summary"));
	}

	@Test void b02_prevAndFirstWalkBack_allFourNavDrawsHold() {
		var r = report();
		assertEquals(1, num(r, "prev_page"), r::toString);
		assertEquals(true, r.get("prev_drawArgIsFalse"));
		assertEquals(0, num(r, "first_page"), r::toString);
		assertEquals(true, r.get("first_drawArgIsFalse"));
		assertEquals(true, r.get("navDrawArgsAllFalse"));
	}

	@Test void b03_pageSizeDrawResetsToPageZero() {
		var r = report();
		assertEquals(1, num(r, "beforeSizeChange_page"), r::toString);
		assertEquals(0, num(r, "pageSize_page"), r::toString);
		assertEquals(false, r.get("pageSize_drawArgIsFalse"));
		assertEquals(true, r.get("pageSize_drawArgOmitted"));
	}
}
