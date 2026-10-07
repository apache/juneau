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
package org.apache.juneau.petstore.console.dev.flavors;

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Rendering flavors: every flavor has a console page, and the three server flavors render the same pets as fragments.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class FlavorsRest_Test extends TestBase {

	private static final Pattern CELL = Pattern.compile("<td>([^<]*)</td>");

	private static String fragment(String engine) throws Exception {
		return client().get("/console/dev/flavors/" + engine + "/fragment").run().assertStatus(200).getContent().asString();
	}

	private static List<String> cells(String html) {
		var out = new ArrayList<String>();
		var m = CELL.matcher(html);
		while (m.find())
			out.add(m.group(1).trim());
		return out;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - pages
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name="{0}")
	@ValueSource(strings={"html", "freemarker", "mustache"})
	void a01_serverFlavorPage(String engine) throws Exception {
		var html = page(client(), "/console/dev/flavors/" + engine);
		assertPage(html).isValid().hasActiveNav("dev", "flavors", engine).hasCardOrder("caption", "flavor").hasCard("flavor", "html");
		assertContainsAll(html, "/console/dev/flavors/" + engine + "/fragment", "View source", Flavor.of(engine).caption());
	}

	@Test void a02_reactPageIframesTheApp() throws Exception {
		var html = page(client(), "/console/dev/flavors/react");
		assertPage(html).isValid().hasActiveNav("dev", "flavors", "react").hasCardOrder("caption", "flavor").hasCard("flavor", "html");
		assertContainsAll(html, "<iframe", "src=\"/petstore-ui\"", "title=\"Petstore React app\"");
	}

	@Test void a03_flavorsRootRedirectsToHtml() throws Exception {
		rawClient().get("/console/dev/flavors").run().assertStatus(303).assertHeader("Location").isMatches("*/console/dev/flavors/html");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - fragments
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name="{0}")
	@ValueSource(strings={"html", "freemarker", "mustache"})
	void b01_fragmentHasNoChrome(String engine) throws Exception {
		var html = fragment(engine);
		assertContainsAll(html, "<table", "data-flavor=\"" + engine + "\"", "Mr. Frisky");
		for (var chrome : List.of("<html", "<body", "juneau-page", "<script"))
			assertFalse(html.contains(chrome), () -> "fragment carries page chrome '" + chrome + "': " + html);
	}

	@Test void b02_allThreeFlavorsAgree() throws Exception {
		var html = cells(fragment("html"));
		assertSize(40, html);  // 10 pets x name, species, price, status
		assertList(cells(fragment("freemarker")), html.toArray());
		assertList(cells(fragment("mustache")), html.toArray());
	}

	@Test void b03_firstRowIsTheFirstSeededPet() throws Exception {
		assertList(cells(fragment("html")).subList(0, 4), "Mr. Frisky", "CAT", "39.99", "AVAILABLE");
	}

	@Test void b04_reactHasNoFragment() throws Exception {
		client().get("/console/dev/flavors/react/fragment").accept("text/html").run().assertStatus(404);
	}
}
