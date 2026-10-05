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
 * Behavioral coverage for the general page-state store (design §6.2).  A Node harness ({@code page-state.cjs}) loads
 * {@code juneau-pagestate.js} standalone into the DOM shim and reports: two tables on one page keep separate per-table
 * keys, a page-level namespace is independent, a JSON value round-trips through the default store, a replaced store
 * implementation is actually used (and the default store is bypassed), and a blocked store degrades to a silent no-op
 * rather than throwing.  Gated on {@code node} being on {@code PATH}.
 */
class ViewsJs_PageState_Test extends TestBase {

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, name);
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
		var pageStateFile = Files.createTempFile("juneau-pagestate-", ".js");
		try {
			Files.writeString(pageStateFile, resource(ViewsMixin.PAGESTATE_JS_RESOURCE), UTF_8);
			report = Json.to(runNode(harness, pageStateFile), Map.class);
		} finally {
			Files.deleteIfExists(pageStateFile);
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
			var p = Path.of(basedir, "src/test/js/page-state.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/page-state.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/page-state.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path pageStateJs) throws Exception {
		var stdout = Files.createTempFile("page-state-stdout-", ".json");
		var stderr = Files.createTempFile("page-state-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), pageStateJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("page-state.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("page-state.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or page-state.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void a01_moduleAttachesPageStateScope() {
		var r = report();
		assertEquals(true, r.get("hasPageState"), r::toString);
	}

	@Test void a02_twoTablesKeepSeparateKeys() {
		var r = report();
		// Same value NAME under two distinct table keys must not clobber (design §6.2 per-table keying).  Each
		// scope lands under its own fully-qualified key; a page-level namespace is independent again.
		assertBean(r, "table1Cols,table2Cols,storeKeys,pageTab",
			"[name,status],[id],[juneau.pagestate.page.nav.tab,juneau.pagestate.table.releases.visibleCols,juneau.pagestate.table.users.visibleCols],setup");
	}

	@Test void a03_jsonValueRoundTripsAndMissingKeyIsNull() {
		var r = report();
		assertMap((Map<?,?>) r.get("roundTrip"), "rows=compact", "wrap=false");
		assertNull(r.get("missingKey"), r::toString);
	}

	@Test void a04_replacedStoreIsUsed() {
		var r = report();
		// A store installed via useStore() actually receives writes and serves reads...and the default
		// localStorage is bypassed once replaced.
		assertBean(r, "replacedStoreRecorded,replacedStoreValue,defaultStoreUntouchedByReplaced", "true,3,true");
	}

	@Test void a05_blockedStoreIsSilentNoOp() {
		var r = report();
		// A store whose calls throw (private mode/quota) degrades to a no-op: no throw, and get reads back null.
		assertEquals(true, r.get("blockedNoThrow"), r::toString);
		assertNull(r.get("blockedGet"), r::toString);
	}
}
