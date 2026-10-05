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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.Path;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

/**
 * Dev mode (spec §4.7, E-14): {@code <@console>} validates its contract before writing it.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ConsoleDirective_DevMode_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class DevHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().devMode(true).basePath("/templates/").chromeTemplate("c1/chrome.ftlh").build();
		}
		@RestGet(path="/t/{name}")
		public View t(@Path("name") String name) {
			return FreemarkerView.of("c1/" + name + ".ftlh");
		}
	}

	@Test void a01_devMode_rejectsAnInvalidContract() throws Exception {
		try (var c = MockRestClient.buildLax(DevHost.class);
			var rsp = c.get("/t/dev-bad-template").run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains("Page contract failed schema validation: '"), () -> body);
			assertTrue(body.contains("missing"), () -> body);
		}
	}

	@Test void a02_devMode_passesAValidContract() throws Exception {
		try (var c = MockRestClient.buildLax(DevHost.class);
			var rsp = c.get("/t/console-full").run()) {
			rsp.assertStatus(200);
		}
	}

	@Test void a03_withoutDevMode_theSameTemplateRenders() {
		// The JS shell reports the missing template at runtime (E-JS-*); the server does not check it by default.
		assertNotNull(C1Fixtures.render("dev-bad-template"));
	}
}
