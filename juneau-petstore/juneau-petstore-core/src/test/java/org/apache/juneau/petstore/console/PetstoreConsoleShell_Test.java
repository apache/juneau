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
package org.apache.juneau.petstore.console;

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetstoreConsoleShell_Test extends TestBase {

	@Test void a01_storeRendersInChrome() throws Exception {
		assertPage(page(client(), "/console/store"))
			.isValid()
			.hasContractVersion("1")
			.hasActiveNav("store")
			.hasHeaderTitle("Juneau Petstore")
			.hasFooterText("Apache Juneau petstore sample");
	}

	@Test void a02_staticNavTree() throws Exception {
		assertPage(page(client(), "/console/store"))
			.hasNavPath("store").hasNavPath("pets").hasNavPath("orders").hasNavPath("users").hasNavPath("ops").hasNavPath("dev").hasNavPath("about")
			.hasNavChildren("pets", "sold", "changes")
			.hasNavChildren("ops", "jobs", "audit")
			.hasNavHref("pets/sold", "/console/pets/sold");
	}

	@Test void a03_consoleRootRedirectsToStore() throws Exception {
		rawClient().get("/console").run().assertStatus(303).assertHeader("Location").isMatches("*/console/store");
	}

	@Test void a04_unknownConsolePage404() throws Exception {
		client().get("/console/nope").accept("text/html").run().assertStatus(404);
	}

	@Test void a05_everyNavLinkIs200() throws Exception {
		var c = client();
		var nav = assertPage(page(c, "/console/store"));
		var links = new java.util.LinkedHashMap<String,String>();
		links.put("store", "/console/store");
		links.put("ops/jobs", "/console/ops/jobs");
		links.put("ops/audit", "/console/ops/audit");
		for (var f : new String[]{"html", "freemarker", "mustache", "react"})
			links.put("dev/flavors/" + f, "/console/dev/flavors/" + f);
		for (var stub : ConsoleStubs.ALL)
			links.put(stub.tab(), stub.href());
		links.forEach(nav::hasNavHref);
		for (var href : links.values())
			c.get(href).accept("text/html").run().assertStatus(200);
	}

	@Test void a06_stubPagesSayComingSoon() throws Exception {
		for (var s : ConsoleStubs.ALL)
			assertContainsAll(page(client(), s.href()), "Coming soon", s.pending());
	}

	@Test void a07_missingStoreBeanFailsLoudly() {
		var r = new StoreRest();
		var e = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, r::store);
		assertContains("No PetStore bean is wired", e.getMessage());
	}
}
