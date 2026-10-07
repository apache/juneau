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
import static org.apache.juneau.test.bct.BctAssertions.*;
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
 * Behavioral DOM-order coverage for the header sort + per-column search glyphs (design 7.1).  A Node harness
 * ({@code header-sort-search.cjs}) builds DT2-shaped header cells and drives {@code wireHeaderSortSearch(...)},
 * then reports the resulting glyph order and active-state class.  Source-shape pins live in
 * {@link ViewsJs_HeaderSortSearch_Test}.  Gated on {@code node} being on {@code PATH}.
 */
class ViewsJs_HeaderSortSearchDom_Test extends TestBase {

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
			var p = Path.of(basedir, "src/test/js/header-sort-search.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/header-sort-search.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/header-sort-search.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("header-sort-search-stdout-", ".json");
		var stderr = Files.createTempFile("header-sort-search-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("header-sort-search.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("header-sort-search.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or header-sort-search.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void a01_orderableColumn_sortThenSearchThenTitle() {
		var r = report();
		// Design 7.1: sort glyph, then the magnifying glass immediately beside it, then the column title.
		assertBean(r, "hasWireHeaderSortSearch,orderableColumnGlyphOrder", "true,[sort,search,title]");
	}

	@Test void a02_nonOrderableColumn_searchLeadsTitle() {
		var r = report();
		// No sort glyph on a non-orderable column, so the magnifying glass is the leftmost control.
		assertEquals(List.of("search", "title"), r.get("nonOrderableColumnGlyphOrder"), r::toString);
	}

	@Test void a03_activeStateReflectsAnAlreadyAppliedFilter() {
		var r = report();
		assertBean(r, "unfilteredColumnActive,filteredColumnActive", "false,true");
	}

	@Test void a04_activeStateReadsTheClientDslStore() {
		var r = report();
		// WORK-J0612: a client-filtered DSL column's expression lives in the per-table store (col.search() is "").
		assertBean(r, "dslStoredColumnActive,dslEmptyColumnActive", "true,false");
	}
}
