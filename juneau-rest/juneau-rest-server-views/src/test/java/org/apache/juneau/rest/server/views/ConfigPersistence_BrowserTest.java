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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The persistence half of the module's <b>JavaScript-execution harness</b>: runs the REAL served
 * {@code juneau-config.js} in a real headless browser against REAL {@code window.localStorage},
 * and asserts the async persistence facade actually behaves as documented at
 * runtime &mdash; not merely that the shipped source CONTAINS the right shapes.
 *
 * <h5 class='section'>Why this exists (beyond {@link ViewsJs_ConfigPersistence_Test}):</h5>
 * <p>
 * That sibling class proves the served script's <i>source shape</i> - the right constants, methods and string
 * literals are present.  It cannot prove a save actually round-trips through {@code localStorage}, that a
 * dangling {@code active} pointer actually resolves to Default, that a 51st view in one scope is actually
 * refused, or that a synthetic {@code storage} event actually reaches {@code watchExternalChanges}' callback.
 * Node has no Web Storage API at all, so the localStorage half of this canary is the ONLY place in the module's
 * test suite that exercises the real browser API rather than a hand-rolled shim that could quietly diverge from
 * it.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests}
 * Maven profile does.  It reuses that profile's provisioned Node + Playwright browser, and derives its own
 * prober ({@code config-persistence.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom
 * change is needed to add this third canary.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewsJs_ConfigPersistence_Test} &mdash; the always-on source-shape half of the same
 * 		contract, which runs with no Node at all.
 * 	<li class='jc'>{@link RowActionCsrf_BrowserTest} &mdash; the sibling {@code juneau-views.js} canary this one
 * 		borrows its CSRF-transport conventions from.
 * </ul>
 */
@EnabledIfSystemProperty(named=ConfigPersistence_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
@SuppressWarnings({
	"unchecked" // obj()/list()/views() cast values of the JS-harness JSON report to Map<String,Object>/List<Object>
})
class ConfigPersistence_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		// The pom's js-tests profile provisions ONE harness property; this canary lives beside it in src/test/js,
		// so it is derived from that property's directory rather than adding a new pom property.
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("config-persistence.cjs");

		// The fixture restates nothing under test: it loads the REAL served juneau-views.js (the namespace
		// juneau-config.js extends), then juneau-pagestate.js (so NS.pageState - the localStorage-
		// backed store the View Settings reload round-trip rides on - exists), then the REAL juneau-config.js,
		// exactly the load order the module's own doc comment requires.
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.PAGESTATE_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsJs_ConfigPersistence_Test.CONFIG_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("config-persistence.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	/** Runs the prober, failing with its stderr attached (its exit code alone is not a diagnosis). */
	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("config-persistence-stdout.json");
		var stderr = dir.resolve("config-persistence-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (!p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	private static Map<String,Object> obj(String key) { return (Map<String,Object>) report.get(key); }

	private static List<Object> list(String key) { return (List<Object>) report.get(key); }

	private static List<Object> views(Map<String,Object> listResult) { return (List<Object>) listResult.get("views"); }

	//------------------------------------------------------------------------------------------------------------------
	// a) the runtime loaded cleanly
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_runtimeLoadedWithNoScriptErrors() {
		assertBean(report, "hasConfig,jsFailures", "true,[]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) the localStorage provider round-trips through REAL window.localStorage
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_listOnAnUnusedScopeIsEmptyNeverAnError() {
		var r = obj("a_listEmpty");
		assertBean(r, "active,views", "<null>,[]");
	}

	@Test void b02_saveIsVisibleToASubsequentList() {
		// #{name} collection-iteration syntax eliminates the manual stream/map name-extraction entirely.
		assertBean(obj("a_listAfterSave"), "views{#{name}}", "{[{My View}]}");
	}

	@Test void b03_loadReturnsTheExactBlobThatWasSaved() {
		var blob = obj("a_loaded");
		assertEquals(2.0, ((Number) blob.get("schemaVersion")).doubleValue(), () -> report.toString());
		assertEquals(List.of("x"), blob.get("columns"), () -> report.toString());
	}

	@Test void b04_setActiveIsReflectedByGetActive() {
		var r = obj("a_activeAfterSetActive");
		assertBean(r, "name,dangling", "My View,false");
	}

	@Test void b05_deletingTheActiveViewResolvesToDefaultPlusDanglingNotice() {
		// The dangling-active resolution (§3.2 should-fix) proven at runtime, not just asserted present in source.
		var r = obj("a_activeAfterDelete");
		assertNull(r.get("name"), () -> "a dangling active pointer must resolve to Default (null): " + report);
		assertEquals(Boolean.TRUE, r.get("dangling"), () -> "the dangling flag must be raised: " + report);
	}

	@Test void b06_saveAndActivateIsAtomicFromTheCallersPerspective() {
		var r = obj("a_activeAfterSaveAndActivate");
		assertBean(r, "name,dangling", "Second View,false");
	}

	@Test void b07_twoPagesSharingAViewIdDoNotCollide() {
		// pageId-qualification (§3.1) proven at runtime: reportsB/orders sees none of reportsA/orders' saved views.
		var r = obj("a_otherPageScopeIsIndependent");
		assertNull(r.get("active"), () -> report.toString());
		assertEquals(List.of(), views(r), () -> report.toString());
	}

	@Test void b08_savingUnderTheReservedNameRejectsAsMalformed() {
		var r = obj("a_reservedNameRejection");
		assertEquals(Boolean.TRUE, r.get("threw"), () -> "'Default' must be rejected, not silently accepted: " + report);
		assertEquals("malformed", r.get("code"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) the per-scope quota is actually enforced by the 51st real localStorage write
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_the51stViewInOneScopeIsRefusedWithATypedQuotaError() {
		var r = obj("b_overQuota");
		assertEquals(Boolean.TRUE, r.get("threw"), () -> "MAX_VIEWS_PER_SCOPE (50) was not enforced: " + report);
		assertEquals("quota", r.get("code"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) a synthetic cross-tab `storage` event actually reaches watchExternalChanges' callback
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_watchExternalChanges_seesOnlyItsOwnScopesKeyAndOnlyWhileWatching() {
		var seen = list("c_storageEventsSeen");
		assertEquals(1, seen.size(),
			() -> "expected exactly one in-scope, pre-unwatch storage event to be observed: " + report);
		var key = (String) seen.get(0);
		assertTrue(key.endsWith(".columns.views.someKey"), () -> "wrong key observed: " + key);
		assertFalse(key.contains("SOME-OTHER-SCOPE"), () -> "an out-of-scope key leaked through: " + key);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) the last-applied View Settings survive a REAL page reload through the localStorage-backed page-state store
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_pageStateStoreIsPresentAfterReload() {
		var r = obj("f_reload");
		assertEquals(Boolean.TRUE, r.get("hasPageState"),
			() -> "juneau-pagestate.js must populate NS.pageState (the reload round-trip rides on it): " + report);
	}

	@Test void f02_committedViewSettingsAreRestoredAfterReload() {
		// The exact blob written before the reload comes back verbatim from REAL localStorage (design §6.1).
		var restored = obj("f_reload");
		assertBean(restored, "restored{schemaVersion,visible,search,sort,options{pageSize,wrap,density}}",
			"{2,[a,b],[a],[{column=b,dir=asc}],{50,true,compact}}");
	}

	@Test void f03_secondViewIdKeepsItsOwnIndependentSlotAcrossReload() {
		// Per-table keying (§6.2): a different view id is neither clobbered by nor merged with the first.
		var restored = obj("f_reload");
		assertBean(restored, "other{visible,options{pageSize,wrap,density}}", "{[z],{10,false,comfortable}}");
	}

	@Test void f04_neverWrittenViewAndTableWithNoViewIdBothReadBackNull() {
		var restored = obj("f_reload");
		assertNull(restored.get("absent"), () -> "a view id that was never written must read back null: " + report);
		assertNull(restored.get("noId"), () -> "a table with no data-juneau-view has no scope, so null: " + report);
	}
}
