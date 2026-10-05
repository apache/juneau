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
 * shipped pack's {@code <link rel="stylesheet">} and no other (and the contract's {@code theme.name});
 * omitting the directive defaults to the {@code open} pack; an unknown attribute (and {@code theme=} on
 * {@code <@page>}) are rejected. {@code <@theme>} is only valid inside {@code <@console>} (E-18 is covered
 * in {@link ConsoleDirective_Errors_Test}).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class ThemeDirective_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class ThemedHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-theme-named.ftlh")
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
				.chromeTemplate("admin/console-chrome-bare.ftlh")
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
				.chromeTemplate("admin/console-theme-bogus.ftlh")
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
		String body;
		try (var c = MockRestClient.buildLax(ThemedHost.class);
			var rsp = c.get("/themed").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertEquals(1, count(body, "juneau-theme-light-red.css"), () -> body);
		assertFalse(body.contains("juneau-theme-open.css"), () -> body);  // named pack wins; no second block
		assertFalse(body.contains("slds-"), () -> body);
		assertPage(body).isValid().hasTheme("light-red");
		assertFalse(body.contains("<style>"), () -> body);  // no tokens means no override block
	}

	@Test void d02_omitTheme_defaultsToOpen() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(DefaultHost.class);
			var rsp = c.get("/themed-default").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("juneau-theme-open.css"), () -> body);
		assertPage(body).isValid().hasTheme("open");
	}

	@Test void d03_unknownThemeAttr_isRejected() throws Exception {
		try (var c = MockRestClient.buildLax(BogusHost.class);
			var rsp = c.get("/themed-bogus").run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains("<@theme> unknown attribute 'bogus'."), () -> body);
		}
	}

	@Test void d04_pageThemeAttr_isRejected() throws Exception {
		try (var c = MockRestClient.buildLax(DefaultHost.class);
			var rsp = c.get("/page-theme-attr").run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains("<@page> unknown attribute 'theme'."), () -> body);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e) <@theme>/<@token> inside <@console>: escaping and build-time diagnostics
	//-----------------------------------------------------------------------------------------------------------------

	private static FreemarkerMixin consoleMixin(String chrome) {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/" + chrome).build();
	}

	private static String render(Class<?> host, int expectedStatus) throws Exception {
		try (var c = MockRestClient.buildLax(host); var rsp = c.get("/naked").run()) {
			rsp.assertStatus(expectedStatus);
			return rsp.getContent().asString();
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class E01_SemicolonHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return consoleMixin("theme-token-semicolon.ftlh"); }
		@RestGet(path="/naked") public View naked() { return FreemarkerView.of("admin/page-naked.ftlh"); }
	}

	/**
	 * The FTL twin of the mixin's escaper-wiring gate: {@code 'My;Font'} is grammar-accepted, but a raw {@code ;}
	 * would end the declaration early. Only a wired {@code CssValueEscaper} turns it into {@code \3B }.
	 */
	@Test void e01_leafValueWithSemicolon_isEscapedInTheOverrideBlock() throws Exception {
		var body = render(E01_SemicolonHost.class, 200);
		assertTrue(body.contains("\\3B "), () -> "expected the CSS-hex escape for ';', body:\n" + body);
		assertFalse(body.contains("'My;Font'"), () -> "raw unescaped ';' leaked into the override block, body:\n" + body);
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class E02_ReservedHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return consoleMixin("theme-token-reserved.ftlh"); }
		@RestGet(path="/naked") public View naked() { return FreemarkerView.of("admin/page-naked.ftlh"); }
	}

	@Test void e02_reservedChromeToken_isRejected() throws Exception {
		var body = render(E02_ReservedHost.class, 500);
		assertTrue(body.contains("Cannot declare reserved token '--jc-chrome-control-height'"), () -> body);
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class E03_UnknownRefHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return consoleMixin("theme-alias-unknown.ftlh"); }
		@RestGet(path="/naked") public View naked() { return FreemarkerView.of("admin/page-naked.ftlh"); }
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class E04_CycleHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return consoleMixin("theme-token-cycle.ftlh"); }
		@RestGet(path="/naked") public View naked() { return FreemarkerView.of("admin/page-naked.ftlh"); }
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class E05_BothChannelsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return consoleMixin("theme-both-channels.ftlh"); }
		@RestGet(path="/naked") public View naked() { return FreemarkerView.of("admin/page-naked.ftlh"); }
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class E06_BodylessHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return consoleMixin("theme-bodyless.ftlh"); }
		@RestGet(path="/naked") public View naked() { return FreemarkerView.of("admin/page-naked.ftlh"); }
	}

	@Test void e03_leafReferencingAnUnknownToken_isRejectedWithTheBuildSentence() throws Exception {
		var body = render(E03_UnknownRefHost.class, 500);
		assertTrue(body.contains("references unknown token '--jc-nope'"), () -> body);
	}

	@Test void e04_cyclicLeafReferences_areRejectedWithTheBuildSentence() throws Exception {
		var body = render(E04_CycleHost.class, 500);
		assertTrue(body.contains("cyclic reference"), () -> body);
	}

	@Test void e05_nameDeclaredByBothChannels_isRejectedWithTheThemeSentence() throws Exception {
		var body = render(E05_BothChannelsHost.class, 500);
		assertTrue(body.contains("Theme 'open' declares '--jc-x' as both a leaf token and an alias"), () -> body);
	}

	/** A body-less <@theme> is just the stock stylesheet: one link, no inline block. */
	@Test void e06_bodylessTheme_linksTheStockStylesheet_andEmitsNoOverrideBlock() throws Exception {
		var body = render(E06_BodylessHost.class, 200);
		assertEquals(1, count(body, "juneau-theme-gray.css"), () -> body);
		assertFalse(body.contains("html:root{"), () -> body);
	}
}
