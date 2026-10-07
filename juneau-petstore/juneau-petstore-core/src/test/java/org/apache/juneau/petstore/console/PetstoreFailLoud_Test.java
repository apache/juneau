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

import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.Path;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.junit.jupiter.api.*;

/**
 * Broken templates fail loudly at render time, with the console's own message.  Rows whose upstream E-code has not
 * landed are {@code @Disabled} with the gate that blocks them; none is invented here.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetstoreFailLoud_Test extends TestBase {

	/** Renders the failloud fixtures inside the production {@code base.ftlh} chrome. */
	@Rest(mixins=FreemarkerMixin.class, responseProcessors=FreemarkerViewRenderer.class, renderResponseStackTraces="true")
	public static class FixtureHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/org/apache/juneau/petstore/console/").chromeTemplate("templates/base.ftlh").build();
		}

		@RestGet(path="/t/{name}")
		public View t(@Path("name") String name) {
			return FreemarkerView.of("failloud/" + name + ".ftlh");
		}
	}

	private static void assert500(String fixture, String message) throws Exception {
		var body = MockRestClient.buildLax(FixtureHost.class).get("/t/" + fixture).run().assertStatus(500).getContent().asString();
		assertContains(message, body);
	}

	@Test void a01_unknownCardType() throws Exception {
		assert500("unknown-card-type", "<@card> type= must be one of html|datatables; got 'nope'.");
	}

	@Test void a02_badNavPath() throws Exception {
		assert500("bad-nav-path", "<@page tab='pets/nope'> does not match a visible <@node> path; known paths: 'store, pets, pets/sold, pets/changes, orders, users, ops, ops/jobs, ops/audit'.");
	}

	@Test void a04_malformedJson5() throws Exception {
		assert500("malformed-json5", "Card JSON5 is invalid:");
	}

	@Test void a18_badCustomTypeId() throws Exception {
		assert500("bad-custom-type-id", "<@card> type= must be one of html|datatables; got 'Gauge_1'.");
	}

	@Disabled("G-C2/C6: missing-slot E-code not landed")
	@Test void a03_missingSlot() {}

	@Disabled("G-C3: <@node under> / E-B14 not landed")
	@Test void a05_nodeUnderOutsidePage() {}

	@Disabled("G-C3: <@node under> / E-B14 not landed")
	@Test void a06_nodeUnderInNavigation() {}

	@Disabled("G-C3: <@node under> / E-B8 not landed")
	@Test void a07_nodeUnderUnknownParent() {}

	@Disabled("C1 E-3 is asserted by console-ui-freemarker (bad-dup-node); the production chrome has a fixed nav, so no petstore fixture can duplicate a sibling")
	@Test void a08_nodeDuplicateSibling() {}

	@Disabled("G-C3: E-B12 not landed")
	@Test void a09_missingViewRenderer() {}

	@Disabled("G-C3: PageSpec.badge / E-B15 not landed")
	@Test void a10_badgeDuplicate() {}

	@Disabled("G-C3: facts leaf / E-B16 not landed")
	@Test void a11_factsDuplicate() {}

	@Disabled("G-C3: <@facts> / E-B17 not landed")
	@Test void a12_factsBadType() {}

	@Disabled("G-C6: visibleWhen unknown-op E-code not landed")
	@Test void a13_visibleWhenBadOp() {}

	@Disabled("G-C3: <@badge> scope / E-68 not landed")
	@Test void a14_badgeScopeUnknownNode() {}

	@Disabled("G-C2: CardTypeHandler / E-22 not landed")
	@Test void a15_adopterCardTypeHandler() {}

	@Disabled("G-C2: E-23 not landed (a datatables body setting title renders 200)")
	@Test void a16_reservedKey() {}

	@Disabled("G-C2: E-25 not landed (markup in a datatables body renders 200)")
	@Test void a17_markupOnDatatables() {}

	@Disabled("Blocked on P4 pet summary page (G-C3 <@node under>)")
	@Test void b01_scriptInPetNameIsEscaped() {}

	@Disabled("G-C1 dev mode only rejects an invalid page contract (no overlay marker, no juneau.console.devMode property)")
	@Test void b02_devModeOverlay() {}
}
