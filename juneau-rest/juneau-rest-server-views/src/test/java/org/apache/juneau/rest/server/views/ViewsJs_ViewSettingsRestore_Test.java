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
import static org.apache.juneau.commons.utils.Shorts.*;
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
 * Always-on behavioral coverage for Gap 1: a persisted View Settings blob is applied to the live grid at table
 * construction, in BOTH data modes.
 *
 * <p>Regression for the JRM e2e "a column hidden in View Settings comes back after a reload": {@code initTableFromDef}'s
 * {@code go()} consulted only the named-saved-view path and never the View Settings blob that every dialog Apply
 * writes to the page-state store, so a reload rebuilt the catalog-default columns (server mode and client mode alike).
 * Driven by {@code view-settings-restore.cjs}, which reloads a table against a pre-seeded store and reports the options
 * handed to the DataTable constructor.  Self-skips when {@code node} is absent.
 */
class ViewsJs_ViewSettingsRestore_Test extends TestBase {

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var files = new ArrayList<Path>();
		try {
			for (var res : List.of(ViewsMixin.PAGESTATE_JS_RESOURCE, ViewsMixin.RENDERS_JS_RESOURCE,
					ViewsJs_ConfigPersistence_Test.CONFIG_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE)) {
				var f = Files.createTempFile("juneau-vsr-", ".js");
				files.add(f);
				Files.writeString(f, resource(res), UTF_8);
			}
			report = Json.to(runNode(harness, files), Map.class);
		} finally {
			for (var f : files)
				Files.deleteIfExists(f);
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
		for (var rel : List.of(
			"src/test/js/view-settings-restore.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/view-settings-restore.cjs"
		)) {
			var p = Path.of(System.getProperty("basedir", "."), rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
			p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, List<Path> scripts) throws Exception {
		var stdout = Files.createTempFile("vsr-stdout-", ".json");
		var stderr = Files.createTempFile("vsr-stderr-", ".txt");
		try {
			var cmd = new ArrayList<String>(List.of("node", harness.toString()));
			scripts.forEach(s -> cmd.add(s.toString()));
			var p = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("view-settings-restore.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("view-settings-restore.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr));
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
		assumeTrue(report != null, "node not available or view-settings-restore.cjs not found - behavioral layer skipped");
		return report;
	}

	private static Map<?,?> col(String key, String data) {
		for (var c : (List<?>)report().get(key))
			if (eq(data, ((Map<?,?>)c).get("data"))) return (Map<?,?>)c;
		return fail("no column '" + data + "' in " + key);
	}

	@Test void a01_hiddenColumn_staysHiddenAfterReload_serverMode() {
		assertNull(report().get("server_error"));
		assertEquals(false, col("server_columns", "stage").get("visible"));
		assertEquals(true, col("server_columns", "status").get("visible"));
		assertEquals(true, col("server_columns", "name").get("visible"));
	}

	@Test void a02_hiddenColumn_staysHiddenAfterReload_clientMode() {
		assertNull(report().get("client_error"));
		assertEquals(false, col("client_columns", "stage").get("visible"));
	}

	@Test void b01_persistedSort_becomesTheInitialOrder() {
		// status is dtIndex 2 (no selection/detail columns in the fixture); the catalog default was [0,"asc"].
		assertEquals(List.of(List.of(2, "desc")), report().get("server_order"));
		assertEquals(List.of(List.of(2, "desc")), report().get("client_order"));
	}

	@Test void b02_persistedSearchMembership_downgradesExcludedColumns() {
		// 'hidden' is absent from the persisted search membership, so its header search is switched off.
		assertEquals(false, col("server_columns", "hidden").get("searchable"));
		assertEquals(true, col("server_columns", "stage").get("searchable"));
	}

	@Test void b03_persistedOptions_areAppliedToTheLiveGrid() {
		assertEquals(50, ((Number)report().get("server_pageLen")).intValue());
		assertEquals(true, report().get("server_wrapClass"));
		assertEquals(true, report().get("client_wrapClass"));
	}

	@Test void c01_noBlob_leavesCatalogDefaultsUntouched() {
		assertEquals(true, col("noBlob_columns", "stage").get("visible"));
		assertEquals(false, col("noBlob_columns", "hidden").get("visible"));
	}

	@Test void c02_tabRestrictedView_ignoresStaleFacetsForHiddenTabs() {
		assertEquals(true, col("restricted_columns", "stage").get("visible"));
		assertEquals(List.of(List.of(0, "asc")), report().get("restricted_order"));
	}

	@Test void c03_staleSchemaVersion_isDiscardedOnce_withResetFlag() {
		assertEquals(true, col("stale_columns", "stage").get("visible"));
		assertEquals(true, report().get("stale_reset"));
		assertEquals(true, report().get("stale_blobRemoved"));
	}

	@Test void d01_autoRefreshOption_wiresTheTimerAtConstruction() {
		assertEquals(true, report().get("auto_timerWired"));
		assertEquals(false, report().get("noAuto_timerWired"));
	}

	@Test void e01_dialogSeedsFromRestoredSettings_notCatalogDefaults() {
		assertEquals(List.of("name", "status"), report().get("seed_draftVisible"));
		assertEquals(false, report().get("seed_dialogStageChecked"));
	}

	@Test void e02_staleBlob_resetNoticeReachesTheDialogOnce() {
		assertEquals(true, report().get("stale_noticeArmed"));
		assertEquals("Saved view settings were reset because the table changed.", report().get("stale_noticeText"));
		assertEquals(true, report().get("stale_noticeCleared"));
	}

	@Test void e03_sortDraft_isSeededFromViewDefaultOrder() {
		var expected = List.of(Map.of("column", "name", "dir", "asc"));
		assertEquals(expected, report().get("sortSeed_noBlob"));
		assertEquals(expected, report().get("sortSeed_persisted"));
		assertEquals(List.of(), report().get("sortSeed_emptyDefault"));
	}

	@Test void e04_teardown_clearsAutoRefreshTimer() {
		assertEquals(true, report().get("auto_timerAfterTeardown"));
	}

	@Test void e05_tableWithoutColumnConfig_ignoresStoredBlob() {
		assertEquals(true, col("noColumnConfig_columns", "stage").get("visible"));
		assertEquals(true, report().get("noColumnConfig_blobKept"));
	}

	@Test void e06_rowControlAriaLabels_includeTheColumnLabel() {
		assertEquals("Show column Stage", report().get("seed_ariaLabels"));
	}
}
