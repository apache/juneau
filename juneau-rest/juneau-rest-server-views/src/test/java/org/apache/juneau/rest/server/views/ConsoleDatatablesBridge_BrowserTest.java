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

import static org.apache.juneau.rest.server.views.ConsoleBrowserFixture.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The {@code datatables} bridge card hands every table to {@code JuneauViews.regions.mount} once, on
 * DOMContentLoaded, whether views loads after the shell (the Foundry order) or before it (the toolkit order). The
 * engine is stubbed (P4); {@code Regions_Mount_Test} covers {@code regions.mount} itself.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleDatatablesBridge_BrowserTest extends TestBase {

	private static final String TABLES = """
		{"contractVersion":"1","title":"Tables","cards":[
		 {"id":"t1","type":"datatables","table":"/rest/a"},
		 {"id":"t2","type":"datatables","table":{"dataUrl":"/rest/b"}}]}
		""";

	private static final String STUB = """
		<script>
		window.__probe = { calls: [] };
		window.JuneauViews = { regions: { mount: function (h) { window.__probe.calls.push(JSON.parse(JSON.stringify(h))); } } };
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		report = ConsoleBrowserFixture.create("datatables-bridge")
			.page("views-after", ORIGIN + "/after", contractPage(TABLES, "", "", STUB),
				"t1", "main.jc-main > [data-juneau-card=\"datatables\"] > #t1-body.jc-card-body",
				"t2", "main.jc-main > [data-juneau-card=\"datatables\"] > #t2-body.jc-card-body")
			.page("views-before", ORIGIN + "/before", contractPage(TABLES, "", STUB, ""),
				"t1", "main.jc-main > [data-juneau-card=\"datatables\"] > #t1-body.jc-card-body")
			.run();
	}

	private static void assertOneHookup(String caseName) {
		var r = report.get(caseName);
		assertNoShellErrors(r);
		var expected = List.of(Map.of("t1-body", Map.of("table", "/rest/a"), "t2-body", Map.of("table", Map.of("dataUrl", "/rest/b"))));
		assertList(() -> caseName, ConsoleBrowserFixture.probe(r).get("calls"), expected.toArray());
		assertEquals("", queries(r).get("t1"), caseName + ": the mount body must exist and be empty until the engine fills it");
	}

	@Test void a01_viewsLoadedAfterTheShell() {
		assertOneHookup("views-after");
		assertEquals("", queries(report.get("views-after")).get("t2"));
	}

	@Test void a02_viewsLoadedBeforeTheShell() {
		assertOneHookup("views-before");
	}
}
