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
package org.apache.juneau.petstore.console.browser;

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.test.bct.BctAssertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.dev.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * Parity gate: every console parity item has a petstore page and a browser test, or names the gate it
 * waits on.
 */
@SuppressWarnings({
	"resource" // Each render's response is fully consumed by assertStatus(); Eclipse JDT @Owning warning is by design.
})
class PetstoreCoverage_Test extends TestBase {

	/** The parity ids. */
	static final List<String> PARITY = List.of("export", "charts", "visibility-rules", "pending-changes-badge", "bulk-select", "per-view-tab-restriction", "url-state", "terminal-card", "confirm-dialogs", "inline-edit", "related-list", "async-job", "depth-4-nav");

	/** Every browser test class; a01 proves the list is complete. */
	static final List<Class<?>> BROWSER_TESTS = List.of(
		PetstoreHarness_BrowserTest.class,
		PetstoreNav_BrowserTest.class,
		PetstoreFlavors_BrowserTest.class,
		PetstoreSecure_BrowserTest.class,
		PetstoreThemes_BrowserTest.class,
		PetstoreAsyncJob_BrowserTest.class,
		PetstoreRowJob_BrowserTest.class,
		PetstoreDatatable_BrowserTest.class,
		PetstoreTerminal_BrowserTest.class,
		PetstorePetDetail_BrowserTest.class,
		PetstoreData_BrowserTest.class,
		PetstoreTypedConfirm_BrowserTest.class,
		PetstoreOrderWizard_BrowserTest.class,
		PetstoreCustomCard_BrowserTest.class,
		PetstoreDeleteConfirm_BrowserTest.class,
		PetstoreConsoleWrites_BrowserTest.class,
		PetstoreChart_BrowserTest.class,
		PetstoreTreemap_BrowserTest.class,
		PetstoreUrlState_BrowserTest.class,
		PetstoreOrdersDay_BrowserTest.class,
		PetstoreClientRibbon_BrowserTest.class,
		PetstoreBulkSelect_BrowserTest.class,
		PetstoreBulkDiscard_BrowserTest.class,
		PetstoreDashboard_BrowserTest.class,
		PetstorePending_BrowserTest.class,
		PetstoreHooks_BrowserTest.class,
		PetstoreVisibility_BrowserTest.class,
		PetstoreTwins_BrowserTest.class,
		PetstoreHookRegistry_BrowserTest.class);

	/** Parity ids not yet covered, each with what it waits on.  Remove a row when its browser test lands. */
	static final Map<String,String> PENDING = Map.of();

	private static Map<String,List<Class<?>>> covered() {
		var m = new TreeMap<String,List<Class<?>>>();
		for (var c : BROWSER_TESTS) {
			var a = c.getAnnotation(Covers.class);
			if (a != null)
				for (var id : a.value())
					m.computeIfAbsent(id, k -> new ArrayList<>()).add(c);
		}
		return m;
	}

	@Test void a01_everyBrowserTestIsListed() throws Exception {
		Set<String> onDisk;
		try (var s = Files.walk(Path.of("src/test/java/org/apache/juneau/petstore"))) {
			onDisk = s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith("_BrowserTest.java"))
				.map(n -> n.substring(0, n.length() - 5)).collect(Collectors.toCollection(TreeSet::new));
		}
		var listed = BROWSER_TESTS.stream().map(Class::getSimpleName).collect(Collectors.toCollection(TreeSet::new));
		assertList(() -> "BROWSER_TESTS is out of date", listed, onDisk.toArray());
	}

	@Test void a02_everyParityItemIsCoveredOrPending() {
		var covered = covered();
		var problems = new ArrayList<String>();
		for (var id : PARITY) {
			var c = covered.containsKey(id);
			var p = PENDING.containsKey(id);
			if (! (c || p))
				problems.add(String.format("Parity item '%s' has no petstore page or browser test", id));
			if (c && p)
				problems.add(String.format("Parity item '%s' is covered by %s but still listed as pending ('%s'); remove the PENDING row", id, covered.get(id), PENDING.get(id)));
		}
		assertString("", String.join("\n", problems));
	}

	@Test void a03_noUnknownIds() {
		var unknown = new TreeSet<String>(covered().keySet());
		unknown.addAll(PENDING.keySet());
		unknown.removeAll(PARITY);
		assertString("", String.join(",", unknown));  // unknown parity ids
	}

	@Test void a04_everyCoveringPageRenders() throws Exception {
		try (var c = client()) {
			for (var t : BROWSER_TESTS) {
				var a = t.getAnnotation(Covers.class);
				if (a != null)
					for (var path : a.pages())
						c.get(path).accept("text/html").run().assertStatus(200);
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - card coverage
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * The petstore's card-type registry, built the way {@code PetstoreConsolePage.freemarker()} builds it: the standard
	 * registry plus the custom gauge.  JSON-only types such as {@code kpi} have no handler, so they are not listed.
	 */
	static Set<String> registeredCardTypes() {
		return new TreeSet<>(CardTypeRegistry.standard().copy().add(new InventoryGaugeCardType()).build().types());
	}

	/** Every card type the contract pages use, read from each page's {@code #juneau-page} contract. */
	static Set<String> usedCardTypes() throws Exception {
		var used = new TreeSet<String>();
		var c = client();
		for (var p : PetstorePages_ContractTest.PAGE_PATHS)
			for (var o : assertPage(page(c, p)).contract().getList("cards"))
				used.add(((JsonMap)o).getString("type"));
		return used;
	}

	@Test void b01_everyRegisteredCardTypeIsUsed() throws Exception {
		var unused = registeredCardTypes();
		unused.removeAll(usedCardTypes());
		assertString("", unused.stream().map(x -> String.format("Card type '%s' is registered but no petstore page uses it", x)).collect(Collectors.joining("\n")));
	}

	@Test void b02_jsonOnlyCardTypeIsUsed() throws Exception {
		assertContains("kpi", usedCardTypes());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - hook coverage
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Hook namespaces with no petstore usage, each with why.  A name here must be in the registry and must not also be
	 * claimed by a {@link HookCovers} test; build a real usage and remove the row instead of claiming it falsely.
	 */
	static final Map<String,String> HOOK_EXEMPT = Map.of(
		"probeSelection", "framework-internal: enhances [data-juneau-probe-group] markup, and the petstore has no probe group",
		"html", "escaping helpers for script-built markup, and the petstore script builds DOM with createElement and textContent only",
		"tables", "table handle helpers for scripts that drive a table, and the petstore has none (the toolkit redraws tables itself)");

	/** The hook namespaces each {@link HookCovers} browser test claims, keyed by name. */
	static Map<String,List<Class<?>>> hookClaims() {
		var m = new TreeMap<String,List<Class<?>>>();
		for (var c : BROWSER_TESTS) {
			var a = c.getAnnotation(HookCovers.class);
			if (a != null)
				for (var id : a.value())
					m.computeIfAbsent(id, k -> new ArrayList<>()).add(c);
		}
		return m;
	}

	/**
	 * Checks the hook claims against the registry.  Reports an exempt name that is also claimed, an exempt name not in
	 * the registry, a claim naming something not in the registry, and a registry name that is neither claimed nor
	 * exempt.
	 */
	static List<String> hookProblems(Collection<String> registry, Map<String,? extends Collection<?>> claims, Map<String,String> exempt) {
		var problems = new TreeSet<String>();
		for (var e : exempt.keySet()) {
			if (! registry.contains(e))
				problems.add(String.format("Exempt hook '%s' is not in JuneauViews.HOOK_NAMESPACES", e));
			if (claims.containsKey(e))
				problems.add(String.format("Hook '%s' is exempt but also claimed by a @HookCovers test; remove the exemption", e));
		}
		for (var c : claims.keySet())
			if (! registry.contains(c))
				problems.add(String.format("@HookCovers names '%s', which is not in JuneauViews.HOOK_NAMESPACES", c));
		for (var r : registry)
			if (! (claims.containsKey(r) || exempt.containsKey(r)))
				problems.add(String.format("Hook '%s' is not claimed by any @HookCovers test and is not exempt", r));
		return new ArrayList<>(problems);
	}

	private static final List<String> REG = List.of("a", "b", "c");
	private static final Map<String,List<String>> CLAIMS = Map.of("a", List.of("T"), "b", List.of("T"));
	private static final Map<String,String> EXEMPT = Map.of("c", "why");

	@Test void c02_cleanCoveragePasses() {
		assertString("", String.join(",", hookProblems(REG, CLAIMS, EXEMPT)));
	}

	@Test void c03_exemptAndClaimedIsReported() {
		assertList(hookProblems(REG, CLAIMS, Map.of("c", "why", "b", "why")), "Hook 'b' is exempt but also claimed by a @HookCovers test; remove the exemption");
	}

	@Test void c04_exemptNotInRegistryIsReported() {
		assertList(hookProblems(REG, CLAIMS, Map.of("c", "why", "z", "why")), "Exempt hook 'z' is not in JuneauViews.HOOK_NAMESPACES");
	}

	@Test void c05_unknownRegistryNameIsReported() {
		assertList(hookProblems(List.of("a", "b", "c", "d"), CLAIMS, EXEMPT), "Hook 'd' is not claimed by any @HookCovers test and is not exempt");
	}

	@Test void c06_claimNotInRegistryIsReported() {
		assertList(hookProblems(REG, Map.of("a", List.of("T"), "b", List.of("T"), "q", List.of("T")), EXEMPT), "@HookCovers names 'q', which is not in JuneauViews.HOOK_NAMESPACES");
	}

	@Test void c07_aMissingClaimIsReported() {
		assertList(hookProblems(REG, Map.of("a", List.of("T")), EXEMPT), "Hook 'b' is not claimed by any @HookCovers test and is not exempt");
	}
}
