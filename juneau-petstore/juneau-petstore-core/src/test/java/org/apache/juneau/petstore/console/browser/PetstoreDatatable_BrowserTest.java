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

import static org.apache.juneau.petstore.console.browser.PetstoreBrowser.*;
import static org.apache.juneau.test.bct.BctAssertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * P7 Audit in the real browser: the server-mode table POSTs a DataTables request to the BeanQuery endpoint, and a
 * global search sends a new request carrying the search value.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreDatatable_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	/** An audit entity the seed data guarantees (its 300 audit rows are Pet, Order and User entries), so the search has hits. */
	private static final String SEARCH = "Order";

	private static final String ROWS = "[...document.querySelectorAll('#audit-body tbody tr')].filter(tr => !tr.querySelector('.dt-empty')).map(tr => tr.textContent)";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("datatable", base, List.of(
			Map.of("name", "audit", "path", "/console/ops/audit", "capture", "/console/ops/audit/query",
				"actions", List.of(
					Map.of("waitForCaptured", 1),
					Map.of("waitFor", "document.querySelector('#audit-body tbody tr td')"),
					Map.of("fill", List.of("#audit-body input[type=search]", SEARCH)),
					Map.of("waitForCaptured", 2, "timeoutMs", 5000),
					// The POST returning is not the redraw: wait until the table shows only rows the search matched.
					Map.of("waitFor", "(() => { const r = [...document.querySelectorAll('#audit-body tbody tr')]; return r.length > 0 && r.every(tr => !tr.querySelector('.dt-empty') && tr.textContent.includes('" + SEARCH + "')); })()")),
				"queries", Map.of("rows", ROWS))));
	}

	@SuppressWarnings("unchecked") // requests is a JSON array of {url, method, postData}.
	private static List<Map<String,Object>> requests() {
		return (List<Map<String,Object>>) assertClean(report, "audit").get("requests");
	}

	private static Map<?,?> body(Map<String,Object> request) throws Exception {
		return Json.to((String) request.get("postData"), Map.class);
	}

	@Test void a01_firstLoadPostsADataTablesRequest() throws Exception {
		var r = requests().get(0);
		assertString("POST", r.get("method"));
		var b = body(r);
		assertBean(b, "draw,start,length", "1,0,25");  // views toolkit default page size: juneau-views.js PAGE_SIZE_OPTIONS[0] = 25
		assertContainsAll(String.valueOf(b.get("columns")), "at", "actor", "action", "entity", "entityId", "detail");
	}

	@Test void a02_globalSearchSendsTheValue() throws Exception {
		var reqs = requests();
		assertContains(SEARCH, String.valueOf(body(reqs.get(reqs.size() - 1)).get("search")));
	}

	@Test void a03_onlyRowsMatchingTheSearchRender() {
		var rows = (List<?>) query(assertClean(report, "audit"), "rows");  // DataTables' "no data" row is filtered out
		assertNotEmpty(rows);
		for (var row : rows)
			assertContains(SEARCH, row);
	}
}
