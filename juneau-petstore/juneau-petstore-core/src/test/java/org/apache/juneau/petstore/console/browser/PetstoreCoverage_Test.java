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
import static org.apache.juneau.test.bct.BctAssertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
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
	static final List<String> PARITY = List.of("export", "charts", "visibility-rules", "pending-changes-badge", "bulk-select", "per-view-tab-restriction", "url-state", "job-log-card", "confirm-dialogs", "inline-edit", "related-list", "async-job", "depth-4-nav");

	/** Every browser test class; a01 proves the list is complete. */
	static final List<Class<?>> BROWSER_TESTS = List.of(
		Harness_BrowserTest.class,
		PetstoreNav_BrowserTest.class,
		PetstoreFlavors_BrowserTest.class,
		PetstoreSecure_BrowserTest.class,
		PetstoreThemes_BrowserTest.class,
		PetstoreAsyncJob_BrowserTest.class,
		PetstoreDatatable_BrowserTest.class,
		PetstoreConsoleOutput_BrowserTest.class);

	/** Parity ids not yet covered, each with what it waits on.  Remove a row when its browser test lands. */
	static final Map<String,String> PENDING = Map.ofEntries(
		Map.entry("export", "P2 export: needs console custom card types and ribbon export"),
		Map.entry("charts", "P1/P8 charts: needs the console hook namespaces and the P1 dashboard charts"),
		Map.entry("visibility-rules", "P4 visibility rules: needs the console hook namespaces and the P4 pet page"),
		Map.entry("pending-changes-badge", "pending-changes badge: needs the console hook namespaces and P4 change staging"),
		Map.entry("bulk-select", "P2 bulk select: needs console custom card types"),
		Map.entry("per-view-tab-restriction", "per-view tab restriction: needs BeanQuery view restriction and the orders/users query endpoints"),
		Map.entry("url-state", "URL state: needs the console hook namespaces"),
		Map.entry("confirm-dialogs", "confirm renderers and dialogs: needs the console hook namespaces"),
		Map.entry("inline-edit", "inline edit: needs the console hook namespaces and the P4 pet page"),
		Map.entry("related-list", "related list: needs the console hook namespaces and the P4 pet page"),
		Map.entry("depth-4-nav", "depth-4 nav: needs the P4 pet page"));

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
}
