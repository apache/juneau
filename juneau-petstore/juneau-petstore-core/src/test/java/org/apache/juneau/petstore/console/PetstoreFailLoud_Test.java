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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
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
	"java:S1186", // Empty bodies are @Disabled placeholder rows; each names the gate that blocks it.
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

	private static String render200(String fixture) throws Exception {
		return MockRestClient.buildLax(FixtureHost.class).get("/t/" + fixture).run().assertStatus(200).getContent().asString();
	}

	@Test void a01_unknownCardType_rendersThroughTheGenericPassthrough() throws Exception {
		// A well-formed unregistered type is no longer a server error; the JS shell raises E-JS-4 instead.
		assertContains("\"type\":\"nope\"", render200("unknown-card-type"));
	}

	@Test void a02_badNavPath() throws Exception {
		assert500("bad-nav-path", "<@page tab='pets/nope'> does not match a visible <@node> path; known paths: 'store, pets, pets/sold, pets/changes, orders, users, ops, ops/jobs, ops/audit, dev, dev/cards, dev/themes, "
			+ "dev/flavors, dev/flavors/html, dev/flavors/freemarker, dev/flavors/mustache, dev/flavors/react, dev/secure, about'.");
	}

	@Test void a04_malformedJson5() throws Exception {
		assert500("malformed-json5", "Card JSON5 is invalid:");
	}

	@Test void a15_adopterHandlerForAReservedType_failsAtMixinBuild() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> ConsoleFreemarkerMixin.create().cardType(new ImpostorDatatables()).build());
		assertEquals("Card type 'datatables' is reserved and cannot be replaced; reserved: 'chart, console-output, datatables, html, run-view'.",
			ex.getMessage());
	}

	@Test void a16_reservedBodyKey() throws Exception {
		assert500("reserved-key", "<@card id='x'> body key 'title' is reserved; set it as an attribute.");
	}

	@Test void a17_markupOnDatatables() throws Exception {
		assert500("markup-on-datatables", "<@card id='x'> type='datatables' requires a JSON5 object body; got '");
	}

	@Test void a18_badCustomTypeId() throws Exception {
		assert500("bad-custom-type-id", "<@card id='x'> type='Gauge_1' must match ^[a-z][a-z0-9-]{0,31}$.");
	}

	/** An adopter handler that tries to take over the reserved {@code datatables} type. */
	private static final class ImpostorDatatables implements CardTypeHandler {
		@Override public String type() { return "datatables"; }
		@Override public JsonMap toFragment(CardSource source) { return new JsonMap(); }
	}

	@Test void a19_consoleOutputUnsafeLinesUrl() throws Exception {
		assert500("console-output-unsafe", "ConsoleOutputDef 'log' linesUrl must be a same-origin path");
	}

	@Disabled("Not implemented yet: an error code for a page that leaves a required slot empty")
	@Test void a03_missingSlot() {}

	@Disabled("Not implemented yet: E-B14 for a <@node under=...> used outside a page or inside the navigation")
	@Test void a05_nodeUnderOutsidePage() {}

	@Disabled("Not implemented yet: E-B14 for a <@node under=...> used outside a page or inside the navigation")
	@Test void a06_nodeUnderInNavigation() {}

	@Disabled("Not implemented yet: E-B8 for a <@node under=...> naming an unknown parent")
	@Test void a07_nodeUnderUnknownParent() {}

	@Disabled("Duplicate-sibling error E-3 is asserted by the console FreeMarker bridge tests (bad-dup-node); the production chrome has a fixed nav, so no petstore fixture can duplicate a sibling")
	@Test void a08_nodeDuplicateSibling() {}

	@Disabled("Not implemented yet: E-B12 for a page whose view has no registered renderer")
	@Test void a09_missingViewRenderer() {}

	@Disabled("Not implemented yet: E-B15 for a duplicate PageSpec.badge")
	@Test void a10_badgeDuplicate() {}

	@Disabled("Not implemented yet: E-B16 for a duplicate facts leaf")
	@Test void a11_factsDuplicate() {}

	@Disabled("Not implemented yet: E-B17 for a <@facts> with a bad type")
	@Test void a12_factsBadType() {}

	@Disabled("Not implemented yet: an error code for a visibleWhen expression with an unknown operator")
	@Test void a13_visibleWhenBadOp() {}

	@Disabled("Not implemented yet: E-68 for a <@badge> scoped to an unknown node")
	@Test void a14_badgeScopeUnknownNode() {}

	@Disabled("Blocked on the pet summary page, which needs <@node under=...> support")
	@Test void b01_scriptInPetNameIsEscaped() {}

	@Disabled("Dev mode only rejects an invalid page contract (there is no overlay marker and no juneau.console.devMode property yet)")
	@Test void b02_devModeOverlay() {}
}
