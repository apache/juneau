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
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

/**
 * D12: {@code <@console theme=>} selects a built-in theme directly (Task 6 adds {@code theme=} to {@code <@console>}'s
 * attribute set); a nested {@code <@theme name=>} can still override it.
 */
@SuppressWarnings({
	"resource" // Fluent request/response chain returns the same Closeable already managed by the try-with-resources.
})
class ConsoleDirective_ThemeAttr_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class ThemeAttrHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/console-theme-attr.ftlh").build();
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class NoThemeHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/console-no-theme.ftlh").build();
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Test void a01_themeAttribute_selectsNamedTheme() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(ThemeAttrHost.class); var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("href=\"/juneau-console/themes/juneau-theme-gray.css"), () -> body);
	}

	@Test void a02_noTheme_linksOpenStockTheme() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(NoThemeHost.class); var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("href=\"/juneau-console/themes/juneau-theme-open.css"), () -> body);
		assertFalse(body.contains("<style>html:root{"), () -> body);
	}
}
