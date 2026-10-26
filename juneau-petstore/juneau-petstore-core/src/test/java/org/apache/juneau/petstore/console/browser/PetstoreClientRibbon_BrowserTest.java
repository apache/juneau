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
import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * P3 Sold in the real browser: the client-mode table's ribbon filters the rows it already holds, with no new request
 * for rows.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreClientRibbon_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	/**
	 * The distinct species on the shown page (the third cell of each row).  Body cells carry no column hook, so the
	 * column is found by position, which the page's fixed column list pins.
	 */
	private static final String SPECIES = "[...new Set([...document.querySelectorAll('#sold tbody tr[data-juneau-row-id] td:nth-child(3)')].map(td => td.textContent.trim()))]";

	/** The paging pill's total, from its {@code 1-25 of 87} text. */
	private static final String TOTAL = "Number(document.querySelector('#sold-body .juneau-view-pagingpill-info').textContent.split(' of ')[1])";

	private static final String DOGS = "#sold-body .juneau-view-ribbon-btn[aria-label=\"Dogs\"]";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("client-ribbon", base, List.of(
			Map.of("name", "sold", "path", "/console/pets/sold", "capture", "/console/pets/sold/rows",
				"actions", List.of(
					Map.of("waitFor", "document.querySelector('#sold tbody tr[data-juneau-row-id]')"),
					Map.of("evaluate", "window.__before = " + TOTAL + "; window.__beforeSpecies = " + SPECIES),
					Map.of("click", DOGS),
					Map.of("waitFor", TOTAL + " < window.__before")),
				"queries", Map.of(
					"before", "window.__before",
					"beforeSpecies", "window.__beforeSpecies.length",
					"after", TOTAL,
					"species", SPECIES,
					"pressed", "document.querySelector('" + DOGS + "').getAttribute('aria-pressed')"),
				"fetches", Map.of("pets", "/petstore/pets"))));
	}

	@Test void a01_ribbonFiltersClientSide() {
		var c = assertClean(report, "sold");
		assertTrue(((Number) query(c, "beforeSpecies")).intValue() > 1, () -> "the unfiltered page shows one species: " + c.get("queries"));
		assertList(query(c, "species"), "DOG");
		assertString("true", query(c, "pressed"));
	}

	@Test void a02_noNewRowRequest() {
		var c = assertClean(report, "sold");
		assertSize(1, (List<?>) c.get("requests"));
	}

	@Test void a03_totalIsTheSoldDogs() throws Exception {
		var c = assertClean(report, "sold");
		@SuppressWarnings({
			"unchecked" // fetches are {status, body} objects.
		})
		var f = (Map<String,Object>)((Map<String,Object>)c.get("fetches")).get("pets");
		assertEquals(200, ((Number) f.get("status")).intValue(), () -> String.valueOf(f));
		var dogs = 0;
		for (var p : new JsonList(String.valueOf(f.get("body"))).elements(JsonMap.class))
			if ("SOLD".equals(p.getString("status")) && "DOG".equals(p.getString("species")))
				dogs++;
		assertTrue(dogs > 0, "the seed sold no dogs");
		assertEquals(dogs, ((Number) query(c, "after")).intValue());
		assertTrue(dogs < ((Number) query(c, "before")).intValue(), () -> String.valueOf(c.get("queries")));
	}
}
