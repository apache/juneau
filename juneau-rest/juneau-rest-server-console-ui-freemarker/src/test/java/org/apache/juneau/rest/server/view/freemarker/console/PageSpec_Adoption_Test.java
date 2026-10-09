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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

/**
 * Proves the full public {@code PageSpec} surface end to end, in the shape a from-scratch adopter actually uses
 * it &mdash; no page template and no FTL authored for the page body (the realistic JRM/Foundry "optional adoption"
 * shape). Every other test in this module exercises one feature in isolation; this one chains tab, toolkit, a
 * table card, a badge, top-level facts and a csrf token in a single page and asserts the whole contract.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>PageSpec</jv>.<jsm>create</jsm>()
 * 		.tab(<js>"activity/today"</js>)
 * 		.toolkit(<js>"views"</js>)
 * 		.table(<jv>table</jv>)
 * 		.badge(<jv>badge</jv>)
 * 		.facts(Map.<jsm>of</jsm>(<js>"build"</js>, <js>"1.0"</js>))
 * 		.view(<jv>req</jv>);
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class PageSpec_Adoption_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/")
				.chromeTemplate("parity/c3/parity-chrome.ftlh").build();
		}
		@RestGet(path="/adopted")
		public View adopted(RestRequest req) {
			var t = TableSpec.create("work").dataUrl("/rest/work/data")
				.columns(Column.create("id").label("Id"), Column.create("state").label("State"))
				.ribbon(RibbonItem.refresh());
			return PageSpec.create()
				.tab("activity/today")
				.toolkit("views")
				.table(t)
				.badge(BadgeDef.create("health").src("/rest/health").table("work").refreshMs(30_000))
				.facts(Map.of("build", Map.of("version", "1.0")))
				.csrf("tok-adopt", "X-CSRF")
				.view(req);
		}
	}

	@Test void adoptedPage_noTemplate_noFtlBody_rendersFullContract() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get("/adopted").run()) {
			body = rsp.getContent().asString();
			assertEquals(200, rsp.getStatusCode(), () -> body);
		}
		var page = assertPage(body).isValid().hasActiveNav("activity", "today").hasCardOrder("work").hasCard("work", "datatables");
		var bodyTag = java.util.regex.Pattern.compile("<body[^>]*>").matcher(body);
		assertTrue(bodyTag.find(), () -> body);
		assertTrue(bodyTag.group().contains("data-juneau-csrf=\"tok-adopt\""), bodyTag::group);
		assertTrue(bodyTag.group().contains("data-juneau-csrf-header=\"X-CSRF\""), bodyTag::group);
		var contract = page.contract();
		var badges = contract.getMap("header").getList("badges");
		assertEquals(1, badges.size());
		assertEquals("health", ((Map<?,?>)badges.get(0)).get("id"));
		assertEquals("1.0", contract.getMap("facts").getMap("build").get("version"));
	}
}
