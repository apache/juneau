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
 * Golden-HTML tests for the {@code <@theme name="…"/>} chrome directive: a named theme emits that
 * shipped pack's {@code <link rel="stylesheet">} and no other; omitting the directive defaults to the
 * {@code open} pack; an unknown attribute (and {@code theme=} on {@code <@page>}) are rejected.
 *
 * @since 10.0.0
 */
class ThemeDirective_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class ThemedHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/theme-chrome.ftlh")
				.build();
		}
		@RestGet(path="/themed")
		public View themed() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class DefaultHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/theme-chrome-default.ftlh")
				.build();
		}
		@RestGet(path="/themed-default")
		public View themedDefault() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
		@RestGet(path="/page-theme-attr")
		public View pageThemeAttr() {
			return FreemarkerView.of("admin/page-theme-attr.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class BogusHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/theme-bogus-chrome.ftlh")
				.build();
		}
		@RestGet(path="/themed-bogus")
		public View themedBogus() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	static int count(String body, String needle) {
		var n = 0;
		for (var i = body.indexOf(needle); i >= 0; i = body.indexOf(needle, i + needle.length()))
			n++;
		return n;
	}

	@Test void d01_theme_named_emitsThatPack() throws Exception {
		var body = MockRestClient.buildLax(ThemedHost.class).get("/themed").run()
			.assertStatus(200).getContent().asString();
		assertEquals(1, count(body, "juneau-theme-light-red.css"), () -> body);
		assertFalse(body.contains("juneau-theme-open.css"), () -> body);  // named pack wins; no second block
		assertFalse(body.contains("slds-"), () -> body);
	}

	@Test void d02_omitTheme_defaultsToOpen() throws Exception {
		var body = MockRestClient.buildLax(DefaultHost.class).get("/themed-default").run()
			.assertStatus(200).getContent().asString();
		assertTrue(body.contains("juneau-theme-open.css"), () -> body);
	}

	@Test void d03_unknownThemeAttr_isRejected() throws Exception {
		var rsp = MockRestClient.buildLax(BogusHost.class).get("/themed-bogus").run();
		rsp.assertStatus(500);
		var body = rsp.getContent().asString();
		assertTrue(body.contains("unknown attribute") && body.contains("bogus"), () -> body);
	}

	@Test void d04_pageThemeAttr_isRejected() throws Exception {
		var rsp = MockRestClient.buildLax(DefaultHost.class).get("/page-theme-attr").run();
		rsp.assertStatus(500);
		var body = rsp.getContent().asString();
		assertTrue(body.contains("unknown attribute") && body.contains("theme"), () -> body);
	}
}
