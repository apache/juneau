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
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Behavioral coverage for client-mode column-search DSL evaluation (WORK-J0612): on a table DataTables filters
 * itself, a column carrying {@code search} metadata is filtered by {@code JuneauViews.search.compile(...)} through
 * one {@code column().search.fixed("juneau-dsl", fn)} predicate, with its expression kept in a per-table store.
 *
 * <p>
 * A Node harness ({@code client-column-search.cjs}) drives the store, the popover, the shareable-URL collect/restore
 * pair and teardown against fake DataTables columns that implement {@code search.fixed}, and reports which fixture
 * rows survive a filter pass.  Gated on {@code node} being on {@code PATH}.
 *
 * <p>
 * Test groups: {@code a} routing (client / inline / server / no-metadata / DT1); {@code b} operator semantics
 * ({@code $in}, {@code $between}, {@code $eq} vs {@code $eqic}, {@code $not}, bare values - D1); {@code c} cell
 * shapes (missing property, array cell - D6); {@code d} strict validation and the popover (D3); {@code e} custom
 * operators (D4); {@code f} Copy-link collect and {@code ?state=} restore (incl. the {@code $regex} guard - S8);
 * {@code g} teardown (D7); {@code h} Java-serialized Date / Instant / LocalDate rows (S6).
 */
class ViewsJs_ClientColumnSearch_Test extends TestBase {

	/** S6 fixture bean: the three temporal shapes a client-mode dataUrl serializes. */
	public static class DatedRow {
		/** A legacy {@link Date}. */
		public Date date;
		/** An {@link Instant}. */
		public Instant instant;
		/** A {@link LocalDate}. */
		public LocalDate localDate;

		static DatedRow of(String isoInstant) {
			var r = new DatedRow();
			r.instant = Instant.parse(isoInstant);
			r.date = Date.from(r.instant);
			r.localDate = LocalDate.ofInstant(r.instant, ZoneOffset.UTC);
			return r;
		}
	}

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
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		var searchFile = Files.createTempFile("juneau-search-", ".js");
		var datesFile = Files.createTempFile("client-column-search-dates-", ".json");
		try {
			Files.writeString(viewsFile, resource(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			Files.writeString(rendersFile, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			Files.writeString(searchFile, resource(ViewsMixin.SEARCH_JS_RESOURCE), UTF_8);
			// The same JSON serializer a client-mode dataUrl response goes through.
			Files.writeString(datesFile, Json.of(List.of(
				DatedRow.of("2026-01-15T10:00:00Z"),
				DatedRow.of("2026-02-15T10:00:00Z"),
				DatedRow.of("2026-04-15T10:00:00Z"))), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile, searchFile, datesFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(rendersFile);
			Files.deleteIfExists(searchFile);
			Files.deleteIfExists(datesFile);
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
			var p = Path.of(basedir, "src/test/js/client-column-search.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/client-column-search.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/client-column-search.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs, Path searchJs, Path datesJson) throws Exception {
		var stdout = Files.createTempFile("client-column-search-stdout-", ".json");
		var stderr = Files.createTempFile("client-column-search-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString(),
					searchJs.toString(), datesJson.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("client-column-search.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("client-column-search.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or client-column-search.cjs not found — behavioral layer skipped");
		return report;
	}

	//------------------------------------------------------------------------------------------------------------------
	// a - routing
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_surface() {
		assertBean(report(), "hasGetColumnExpr,hasSetColumnExpr,hasCompile,hasRegisterCustom", "true,true,true,true");
	}

	@Test void a02_clientFiltered_coversInlineRows() {
		// Inline rows force client filtering even under dataMode:'server' (constructTable records !opts.serverSide).
		assertBean(report(), "clientFilteredClient,clientFilteredServer,clientFilteredInline", "true,false,true");
	}

	@Test void a03_constructTable_recordsClientFilteredBeforeConstruction() throws Exception {
		var src = resource(ViewsMixin.VIEWS_JS_RESOURCE);
		var flag = src.indexOf("ctx.clientFiltered = !opts.serverSide;");
		var construct = src.indexOf("ctx.dataTable = $(table).DataTable(opts);");
		assertTrue(flag > 0 && construct > flag, "clientFiltered must be set before the DataTable is constructed");
	}

	@Test void a04_clientDslColumn_usesFixedSearchNotNative() {
		// The expression lives in the store and one "juneau-dsl" fixed predicate; native col.search() stays "".
		assertBean(report(), "enumInOk,enumInRows,enumInNativeSearch,enumInFixedNames,enumInStoredExpr",
			"true,[0,1],,[juneau-dsl],$in(Triaged,New)");
	}

	@Test void a05_serverMode_unchanged() {
		// D2: server mode keeps sending the raw expression through native col.search(); no fixed predicate.
		assertBean(report(), "serverOk,serverNative,serverFixedNames", "true,$in(Triaged,New),[]");
	}

	@Test void a06_inlineRows_useTheDslPath() {
		assertBean(report(), "inlineRows,inlineNative", "[0,1],");
	}

	@Test void a07_noMetadataColumn_andDt1_stayNative() {
		assertBean(report(), "noMetaNative,noMetaFixedNames,noMetaRows,dt1Native", "x,[],[0,3],New");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b - operator semantics
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_operators() {
		assertBean(report(), "numericBetweenRows,textEqRows,textEqicRows,enumNotRows",
			"[1,3],[1],[0,1],[0,2,3]");
	}

	@Test void b02_bareValues_followServerSemantics() {
		// D1: a bare enum value is a whole-value match ("Tri" matches nothing; "Tri*" is the prefix form); bare
		// text stays a case-insensitive substring.
		assertBean(report(), "enumBareTriRows,enumBareTriStarRows,textBareRows", "[],[0],[0,1]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// c - cell shapes
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_missingProperty_matchesNe() {
		// Server parity (corpus v01): a row with the property absent is a null cell, which $ne(New) matches.
		assertBean(report(), "missingCellNeRows", "[0,2,3]");
	}

	@Test void c02_arrayCell_isMatchedAsItsStringForm() {
		// D6 pin: ['a','b'] is matched as String(value) == "a,b".
		assertBean(report(), "arrayCellEqJoinedRows,arrayCellContainsRows", "[0],[0,3]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d - strict validation + popover
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_invalidExpression_installsNothing_andKeepsThePriorFilter() {
		assertBean(report(), "strictBadOk,strictBadCode,strictKeptRows,strictKeptExpr,strictUnknownOpCode,strictOutOfTypeCode",
			"false,BAD_VALUE,true,$gt(2),UNKNOWN_OPERATOR,OPERATOR_TYPE");
	}

	@Test void d02_blankExpression_removesThePredicate() {
		assertBean(report(), "strictClearOk,strictClearFixedNames,strictClearExpr", "true,[],");
	}

	@Test void d03_popover_showsTheServerMessage_andAppliesNothing() {
		assertBean(report(), "popoverStillOpen,popoverInvalid,popoverStatus,popoverFixedNames",
			"true,true,Value 'abc' is not a valid numeric for column 'priority'.,[]");
	}

	@Test void d04_popover_commitsAValidExpression() {
		assertBean(report(), "popoverCommitClosed,popoverCommitRows,popoverCommitDraws", "true,[1,3],1");
	}

	@Test void d05_popover_bareLivePreview_andEscRevert() {
		assertBean(report(), "popoverLiveRows,popoverLiveNative,popoverRevertRows,popoverRevertExpr",
			"[1],,[0,1,2,3],");
	}

	@Test void d06_popover_utcNote_onlyOnTimestampColumns() {
		assertBean(report(), "popoverTextHasTzHelp,popoverTimestampHasTzHelp", "false,true");
	}

	//------------------------------------------------------------------------------------------------------------------
	// e - custom operators
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_unregisteredCustom_isRejectedAndWarnedOnce() {
		assertBean(report(), "customUnregisteredOk,customUnregisteredCode,customWarnCount,customSecondAttemptOk",
			"false,UNKNOWN_OPERATOR,1,false");
		assertList((List<?>) report().get("popoverHelpOpsBeforeRegister"), "$eq", "$eqic", "$contains", "$not");
	}

	@Test void e02_registeredCustom_isEvaluated() {
		assertBean(report(), "customRegisteredOk,customRegisteredRows,customRegisterBuiltinThrows", "true,[0,1],TypeError");
		assertList((List<?>) report().get("popoverHelpOpsAfterRegister"), "$eq", "$eqic", "$contains", "$not", "$startsCI");
	}

	//------------------------------------------------------------------------------------------------------------------
	// f - Copy link + ?state= restore
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_collectLiveUrlState_readsTheStore() {
		assertList((List<?>) report().get("collectFilters"), "status=$in(Triaged,New)", "note=x");
	}

	@Test void f02_restore_skipsInvalidExpressions() {
		// Valid status filter installs; the BAD_VALUE priority keeps the prior $gt(1); a $regex the column does not
		// offer and an over-length pattern are both skipped (S8); a short $regex on a column offering it restores.
		assertBean(report(), "restoreStatusExpr,restorePriorityExpr,restoreNameExpr,restoreCodeExpr,restoreRows,restoreCodeShortExpr",
			"$in(Triaged,New),$gt(1),,,[1],$regex(c.*)");
	}

	//------------------------------------------------------------------------------------------------------------------
	// g - teardown
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_teardown_clearsTheStore() {
		// D7 pin: a View Settings rebuild drops column filters, exactly as the native path always has.
		assertBean(report(), "teardownBefore,teardownAfter", "$in(Triaged,New),");
	}

	//------------------------------------------------------------------------------------------------------------------
	// h - S6 Java-serialized temporal cells
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_javaSerializedDates_matchBetween() {
		var r = report();
		// Only the 2026-02-15 row falls inside $between(2026-02-01,2026-03-31) for every temporal shape.  The window
		// is deliberately far (weeks) from each cell, so the Date shift pinned in h02 cannot move a row across it.
		assertBean(r, "dateBetween{date,instant,localDate}", "{[1],[1],[1]}");
	}

	@Test void h02_javaSerializedDates_wireShape() {
		// Pins what the JSON serializer puts on the wire, so a serializer change that breaks client parsing shows up
		// here.  Instant and LocalDate are UTC-faithful.  java.util.Date is emitted as JVM-LOCAL wall-clock time with
		// NO offset, which both engines read as UTC (S6) - so on a non-UTC server a client-filtered Date column is
		// shifted by the server's UTC offset (a known parity gap; the server's InMemoryMatch sees the real instant).
		var sample = (List<?>) report().get("dateSample");
		assertTrue(String.valueOf(sample.get(0)).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})?"), sample::toString);
		assertList(sample.subList(1, 3), "2026-01-15T10:00:00Z", "2026-01-15");
	}
}
