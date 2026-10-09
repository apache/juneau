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
import org.junit.jupiter.api.*;

/**
 * Proves a {@link PageSpec} header subtitle and user menu survive a real {@code <@console>} render into the page
 * contract, and coexist with the console's own brand, icon and chrome header fields.
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class PageSpec_HeaderSlots_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/")
				.chromeTemplate("admin/console-chrome-bare.ftlh").build();
		}
		@RestGet(path="/with-spec")
		public View withSpec(RestRequest req) {
			return PageSpec.create()
				.header(h -> h.subtitle("Queue health").userMenu(Map.of("label", "Jane Doe", "initials", "JD")))
				.template("admin/page-header-slots.ftlh").view(req);
		}
		@RestGet(path="/no-spec")
		public View noSpec() {
			return FreemarkerView.of("admin/page-header-slots.ftlh");
		}
	}

	private static String get(String path) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get(path).run()) {
			var body = rsp.getContent().asString();
			assertEquals(200, rsp.getStatusCode(), () -> body);
			return body;
		}
	}

	@Test void a01_subtitleAndUserMenu_flowThroughConsoleRender_andCoexistWithBrandIconChrome() throws Exception {
		var header = assertPage(get("/with-spec")).isValid().contract().getMap("header");
		assertEquals("Queue health", header.getString("subtitle"));
		assertEquals(Map.of("label", "Jane Doe", "initials", "JD"), header.getMap("userMenu"));
		assertEquals("Admin Console", header.getString("title"));
		assertEquals("/app/logo.svg", header.getMap("logo").getString("src"));
		assertEquals(Boolean.TRUE, header.get("chrome"));
	}

	@Test void a02_subtitleAndUserMenu_absentWithoutAPageSpec() throws Exception {
		var header = assertPage(get("/no-spec")).isValid().contract().getMap("header");
		assertNull(header.getString("subtitle"));
		assertNull(header.get("userMenu"));
		assertEquals("Admin Console", header.getString("title"));
	}
}
