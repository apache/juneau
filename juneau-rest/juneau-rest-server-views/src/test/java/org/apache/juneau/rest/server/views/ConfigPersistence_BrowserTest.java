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
 * {@code juneau-config.js} in a real headless browser against REAL {@code window.localStorage}
 * and a stubbed {@code fetch}, and asserts the async persistence facade actually behaves as documented at
 * runtime &mdash; not merely that the shipped source CONTAINS the right shapes.
 *
 * <h5 class='section'>Why this exists (beyond {@link ViewsJs_ConfigPersistence_Test}):</h5>
 * <p>
 * That sibling class proves the served script's <i>source shape</i> - the right constants, methods and string
 * literals are present.  It cannot prove a save actually round-trips through {@code localStorage}, that a
 * dangling {@code active} pointer actually resolves to Default, that a 51st view in one scope is actually
 * refused, that a synthetic {@code storage} event actually reaches {@code watchExternalChanges}' callback, or
 * that the server provider's stubbed {@code fetch} calls actually carry the documented URL/method/headers/body.
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
		// The pom's js-tests profile provisions ONE harness property (the panel-visibility prober); this third
		// canary lives beside it in src/test/js, so it is derived from that property's directory rather than
		// adding a new pom property - the same trick RowActionCsrf_BrowserTest already uses.
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("config-persistence.cjs");

		// The fixture restates nothing under test: it loads the REAL served juneau-views.js (for NS.init's CSRF
		// helpers, which the server provider calls) followed by the REAL juneau-config.js, exactly the load
		// order the module's own doc comment requires.
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
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

	@SuppressWarnings("unchecked")
	private static Map<String,Object> obj(String key) { return (Map<String,Object>) report.get(key); }

	@SuppressWarnings("unchecked")
	private static List<Object> list(String key) { return (List<Object>) report.get(key); }

	@SuppressWarnings("unchecked")
	private static List<Object> views(Map<String,Object> listResult) { return (List<Object>) listResult.get("views"); }

	//------------------------------------------------------------------------------------------------------------------
	// a) the runtime loaded cleanly
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_runtimeLoadedWithNoScriptErrors() {
		assertEquals(Boolean.TRUE, report.get("hasConfig"),
			() -> "juneau-config.js did not populate JuneauViews.persistence/config: " + report);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) the localStorage provider round-trips through REAL window.localStorage
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_listOnAnUnusedScopeIsEmptyNeverAnError() {
		var r = obj("a_listEmpty");
		assertNull(r.get("active"), () -> report.toString());
		assertEquals(List.of(), views(r), () -> report.toString());
	}

	@Test void b02_saveIsVisibleToASubsequentList() {
		var names = views(obj("a_listAfterSave")).stream().map(x -> ((Map<?,?>) x).get("name")).toList();
		assertEquals(List.of("My View"), names, () -> report.toString());
	}

	@Test void b03_loadReturnsTheExactBlobThatWasSaved() {
		var blob = obj("a_loaded");
		assertEquals(1.0, ((Number) blob.get("schemaVersion")).doubleValue(), () -> report.toString());
		assertEquals(List.of("x"), blob.get("columns"), () -> report.toString());
	}

	@Test void b04_setActiveIsReflectedByGetActive() {
		var r = obj("a_activeAfterSetActive");
		assertEquals("My View", r.get("name"), () -> report.toString());
		assertEquals(Boolean.FALSE, r.get("dangling"), () -> report.toString());
	}

	@Test void b05_deletingTheActiveViewResolvesToDefaultPlusDanglingNotice() {
		// The dangling-active resolution (§3.2 should-fix) proven at runtime, not just asserted present in source.
		var r = obj("a_activeAfterDelete");
		assertNull(r.get("name"), () -> "a dangling active pointer must resolve to Default (null): " + report);
		assertEquals(Boolean.TRUE, r.get("dangling"), () -> "the dangling flag must be raised: " + report);
	}

	@Test void b06_saveAndActivateIsAtomicFromTheCallersPerspective() {
		var r = obj("a_activeAfterSaveAndActivate");
		assertEquals("Second View", r.get("name"), () -> report.toString());
		assertEquals(Boolean.FALSE, r.get("dangling"), () -> report.toString());
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
	// e) the server-persisted provider's stubbed-fetch calls carry the exact locked transport envelope
	//------------------------------------------------------------------------------------------------------------------

	private static String url(Map<String,Object> call) { return (String) call.get("url"); }

	@SuppressWarnings("unchecked")
	private static Map<String,Object> init(Map<String,Object> call) { return (Map<String,Object>) call.get("init"); }

	@Test void e01_list_isAPlainCsrfFreeGetWithSameOriginCredentials() {
		var call = obj("d_listCall");
		var i = init(call);
		assertEquals("GET", i.get("method"), () -> report.toString());
		assertEquals("same-origin", i.get("credentials"), () -> report.toString());
		assertTrue(url(call).contains("view=serverView") && url(call).contains("page=serverPage"),
			() -> "list() must carry view+page query params: " + url(call));
		assertNull(i.get("body"), () -> "a GET must never carry a body: " + report);
	}

	@Test void e02_save_isAJsonPutCarryingTheCsrfHeaderAndNameAsAQueryParam_neverAPathSegment() {
		var call = obj("d_saveCall");
		var i = init(call);
		assertEquals("PUT", i.get("method"), () -> report.toString());
		assertEquals("same-origin", i.get("credentials"), () -> report.toString());
		@SuppressWarnings("unchecked")
		var h = (Map<String,Object>) i.get("headers");
		assertEquals("application/json", h.get("Content-Type"), () -> report.toString());
		assertEquals("tok-123", h.get("X-Csrf-Token"), () -> report.toString());
		assertTrue(url(call).contains("name=My%20View") || url(call).contains("name=My+View"),
			() -> "name must ride as a QUERY PARAM: " + url(call));
		assertFalse(url(call).contains("/My%20View") || url(call).contains("/My+View"),
			() -> "name must NEVER be a path segment: " + url(call));
		assertFalse(url(call).contains("activate="), () -> "a bare save() must not set the activate flag: " + url(call));
	}

	@Test void e03_saveAndActivate_usesTheDedicatedActivateQueryFlag_neverAFieldInsideTheBlob() {
		var call = obj("d_saveAndActivateCall");
		var i = init(call);
		assertTrue(url(call).contains("activate=1"), () -> "saveAndActivate must set ?activate=1: " + url(call));
		assertFalse(String.valueOf(i.get("body")).contains("activate"),
			() -> "the activate flag must never leak into the persisted blob body: " + i.get("body"));
	}

	@Test void e04_clearingActiveStillSendsARealJsonBody_neverEmpty() {
		var call = obj("d_clearActiveCall");
		var i = init(call);
		assertEquals("PUT", i.get("method"), () -> report.toString());
		assertTrue(url(call).endsWith("/active") || url(call).contains("/active?"),
			() -> "setActive must target the /active sub-path: " + url(call));
		assertEquals("{}", i.get("body"), () -> "clearing active must send {} - never an empty string: " + report);
	}

	@Test void e05_delete_sendsNameAsAQueryParamWithARealJsonBody() {
		var call = obj("d_deleteCall");
		var i = init(call);
		assertEquals("DELETE", i.get("method"), () -> report.toString());
		assertTrue(url(call).contains("name=My%20View") || url(call).contains("name=My+View"),
			() -> "delete's name must ride as a query param: " + url(call));
		assertEquals("{}", i.get("body"), () -> report.toString());
	}

	@Test void e06_missingShellAttribute_failsClosed_zeroFetchCallsIssued() {
		var r = obj("d_noShell");
		assertEquals(Boolean.TRUE, r.get("threw"), () -> "an absent [data-juneau-saved-views] shell must reject: " + report);
		assertEquals("unavailable", r.get("code"), () -> report.toString());
		assertEquals(0.0, ((Number) r.get("calls")).doubleValue(),
			() -> "a fail-closed provider must never have issued a request: " + report);
	}
}
