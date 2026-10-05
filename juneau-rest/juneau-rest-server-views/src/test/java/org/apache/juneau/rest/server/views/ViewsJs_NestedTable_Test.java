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
import org.apache.juneau.rest.server.datatables.*;
import org.junit.jupiter.api.*;

/**
 * Node-harness coverage for {@code nested-table.cjs} against the BeanQuery DataTables design doc §3.1/D8 rewrite of
 * {@code buildOptions}/{@code applyNestedScope} in {@code juneau-views.js}.
 *
 * <p>
 * Server-mode ajax now comes from {@code window.JuneauDataTables.ajax} (the POST/JSON wire).  The nested-scope param
 * and any {@code param:}-scoped ribbon option are re-derived onto the request URL per request through
 * {@code beforeSend}.  A column-scoped ribbon option is merged into the outgoing JSON
 * body's {@code columns[i].search.value} through {@code window.JuneauViews.ribbon.ribbonColumnSearches} /
 * {@code mergeColumnSearches} in {@code juneau-ribbon.js}, ANDed with any existing per-column user search &mdash;
 * never sent as a URL-only param the POST-only endpoint cannot read.
 *
 * <p>
 * The three scripts are read off the classpath (not the source tree), so the harness exercises the exact bytes the
 * server would serve.  Skipped (not failed) when {@code node} is not on {@code PATH} or the harness file is not
 * found, like the module's other Node-harness tests.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bconsole'>
 * 	mvn -pl juneau-rest/juneau-rest-server-views test -Dtest=ViewsJs_NestedTable_Test
 * </p>
 *
 * @since 10.0.0
 */
class ViewsJs_NestedTable_Test extends TestBase {

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness("nested-table.cjs");
		if (harness == null)
			return;
		var viewsJs = writeTempResource("juneau-views-", ViewsMixin.class, ViewsMixin.VIEWS_JS_RESOURCE);
		var dataTablesJs = writeTempResource("juneau-datatables-", DataTablesMixin.class, "juneau-datatables.js");
		var ribbonJs = writeTempResource("juneau-ribbon-", ViewsMixin.class, ViewsMixin.RIBBON_JS_RESOURCE);
		try {
			report = Json.to(
				runNode(harness, List.of(viewsJs.toString(), dataTablesJs.toString(), ribbonJs.toString())), Map.class);
		} finally {
			Files.deleteIfExists(viewsJs);
			Files.deleteIfExists(dataTablesJs);
			Files.deleteIfExists(ribbonJs);
		}
	}

	private static Path writeTempResource(String prefix, Class<?> anchor, String resource) throws IOException {
		try (var in = anchor.getResourceAsStream(resource)) {
			assertNotNull(in, () -> "missing classpath resource: " + resource + " (relative to " + anchor.getName() + ")");
			var f = Files.createTempFile(prefix, ".js");
			Files.write(f, in.readAllBytes());
			return f;
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

	private static Path locateHarness(String name) {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js", name);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/" + name,
			"juneau-rest/juneau-rest-server-views/src/test/js/" + name
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, List<String> args) throws Exception {
		var stdout = Files.createTempFile("nested-table-stdout-", ".json");
		var stderr = Files.createTempFile("nested-table-stderr-", ".txt");
		try {
			var cmd = new ArrayList<String>();
			cmd.add("node");
			cmd.add(harness.toString());
			cmd.addAll(args);
			var pb = new ProcessBuilder(cmd)
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail(harness.getFileName() + " did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail(harness.getFileName() + " exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or nested-table.cjs not found — skipped");
		return report;
	}

	@Test void a01_serverMode_usesJuneauDataTablesAjax_postJson() {
		assertBean(report(), "bo_server_serverSide,bo_server_type,bo_server_contentType,bo_server_hasBeforeSend,bo_server_hasJsonDataFn",
			"true,POST,application/json,true,true");
	}

	@Test void a02_serverMode_ribbonAndScopeParamsRideTheReDerivedUrl() {
		assertBean(report(), "bo_server_urlHasRibbon,bo_server_urlHasScope", "true,true");
	}

	@Test void a02b_serverMode_dataRequestCarriesCsrfHeader() {
		assertBean(report(), "bo_csrf_table,bo_csrf_customHeader,bo_csrf_metaFallback,bo_csrf_tableBeatsMeta",
			"{X-Csrf-Token=tok-table},{X-My-Csrf=tok-c},{X-Csrf-Token=tok-meta},{X-Csrf-Token=tok-table}");
	}

	@Test void a02c_serverMode_noCsrfHeaderWithoutToken() {
		assertBean(report(), "bo_csrf_none,bo_csrf_blank", "{},{}");
	}

	@Test void a03_applyNestedScope_server_wrapsBeforeSend_keepsRibbonAppendsScope() {
		assertBean(report(), "scope_server_ribbonKept,scope_server_paramAdded", "true,true");
	}

	@Test void a04_applyNestedScope_client_unchangedByD8() {
		assertBean(report(), "scope_client_paramAdded,scope_blank_absent,scope_getter_first,scope_getter_second,scope_customName",
			"true,true,a1,b2,true");
	}

	@Test void a05_clientMode_unaffectedByD8() {
		// bo_client_serverSide is the boolean "serverSide === false" check, so true means client mode stayed client.
		assertBean(report(), "bo_client_serverSide,bo_client_dataSrc,bo_client_scope", "true,true,true");
	}

	@Test void a06_topLevelView_noNestedScope_noDataFnEitherMode() {
		assertBean(report(), "bo_top_client_noDataFn", "true");
	}

	@Test void a07_missingJuneauDataTables_failsLoudly_noSilentGetFallback() {
		assertBean(report(), "bo_server_noDt_warned,bo_server_noDt_noAjax", "true,true");
	}

	@Test void a08_columnScopedRibbonFilterLandsInBodyNeverOnUrl() {
		// The column-scoped ribbon fix: a column-scoped ribbon filter must go into the POST body as that
		// column's own search.value, ANDed with any existing user search on the same column, and must never be
		// droppable as a URL-only param.  Goes through the real window.JuneauViews.ribbon functions (loaded from
		// juneau-ribbon.js), the exact names/signatures downstream ribbon parity corpora bind to.
		assertBean(report(),
			"bo_server_columnSearch_ribbonOnly,bo_server_columnSearch_notOnUrl,bo_server_columnSearch_andedWithUserSearch,bo_server_columnSearch_passthroughWhenNoDep",
			"true,true,true,true");
	}

	@Test void a09_missingJuneauRibbonNamespace_degradesLoudly_noCrash() {
		// A view with ribbon options but no window.JuneauViews.ribbon loaded warns and contributes no ribbon filters -
		// it degrades, it does not throw and does not drop the user's own per-column search.
		assertBean(report(), "bo_server_noRibbonNs_warned,bo_server_noRibbonNs_bodyUnchanged", "true,true");
	}

	@Test void a10_twoActiveRibbonOptionsOnSameColumn_combineAsAnd() {
		// Two ACTIVE ribbon options targeting the SAME column combine as $and(first,second), in viewDef.ribbon's own
		// declared order - never one silently overwriting the other.
		assertBean(report(), "bo_server_columnSearch_twoOptionsSameColumn_and", "true");
	}

	@Test void a11_blankUserSearchValue_droppedBeforeMerge() {
		// A whitespace-only user search value is dropped before merging, never ANDed in as a literal blank clause.
		assertBean(report(), "bo_server_columnSearch_blankUserValueDropped", "true");
	}

	@Test void a13_ribbonFilterOnNonSearchableColumn_flagFlippedSoServerAppliesIt() {
		// The adapter skips searchable:false columns, so a ribbon filter there would otherwise be dropped silently.
		assertBean(report(), "bo_server_columnSearch_nonSearchableTargetFlipped", "true");
	}

	@Test void a12_findRowDetailTemplate_nestedNeverInheritsParentTemplate() {
		// A nested table constructed before DataTables wraps it has no .dt-container of its own, so findViewWrapper
		// reaches the PARENT's; that wrapper must be rejected or the nested table grows the parent's expander column.
		assertBean(report(),
			"own_parentTemplateNotTheNestedOne,own_nestedTemplateIsItsOwn,own_unwrappedNestedIgnoresParentWrapperTemplate,own_wrappedParentStillFindsItsTemplate",
			"true,true,true,true");
	}
}
