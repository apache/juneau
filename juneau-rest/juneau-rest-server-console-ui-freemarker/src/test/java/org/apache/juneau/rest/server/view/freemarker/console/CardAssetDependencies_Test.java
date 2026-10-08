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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.Path;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

/**
 * Card-declared asset dependencies: builder validation and page assembly.
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class CardAssetDependencies_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Hosts
	//-----------------------------------------------------------------------------------------------------------------

	static ConsoleFreemarkerMixin.Builder base() {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/console-chrome-bare.ftlh");
	}

	// renderResponseStackTraces=true so reject() messages reach the 500 body unscrubbed (see CardDirective_Test).
	@Rest(mixins={FreemarkerMixin.class, ViewsMixin.class}, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() { return base().build(); }
		@RestGet(path="/d/{name}")
		public View d(@Path("name") String name) { return FreemarkerView.of("deps/" + name + ".ftlh"); }
	}

	@Rest(mixins={FreemarkerMixin.class, ViewsMixin.class, WebJarsMixin.class}, renderResponseStackTraces="true")
	public static class MountedHost extends Host {
		private static final long serialVersionUID = 1L;
	}

	@Rest(mixins={FreemarkerMixin.class, ViewsMixin.class}, renderResponseStackTraces="true")
	public static class ProvidedHost extends Host {
		private static final long serialVersionUID = 1L;
		@Override @Bean public FreemarkerMixin freemarker() { return base().providedPacks("jquery").build(); }
	}

	@Rest(mixins={FreemarkerMixin.class, ViewsMixin.class}, renderResponseStackTraces="true")
	public static class HtmlRequiresHost extends Host {
		private static final long serialVersionUID = 1L;
		@Override @Bean public FreemarkerMixin freemarker() { return base().cardRequires("html", "datatables-glue").build(); }
	}

	@Rest(mixins={FreemarkerMixin.class, ViewsMixin.class}, renderResponseStackTraces="true")
	public static class ProbeHost extends Host {
		private static final long serialVersionUID = 1L;
		@Override @Bean public FreemarkerMixin freemarker() {
			return base().registerToolkitPack("probe", List.of(), List.of(ViewsMixin.VIEWS_JS_PATH)).build();
		}
	}

	static String get(Class<?> host, String name) throws Exception {
		try (var c = MockRestClient.buildLax(host);
			var rsp = c.get("/d/" + name).run()) {
			rsp.assertStatus(200);
			return rsp.getContent().asString();
		}
	}

	static String error(String name) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/d/" + name).run()) {
			rsp.assertStatus(500);
			return rsp.getContent().asString();
		}
	}

	static int count(String body, String s) {
		return body.split(Pattern.quote(s), -1).length - 1;
	}

	static void assertBefore(String body, String...parts) {
		var last = -1;
		for (var s : parts) {
			var i = body.indexOf(s);
			var prev = last;
			assertTrue(i > prev, () -> "'" + s + "' missing or out of order in:\n" + body);
			last = i;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - build-time validation
	//-----------------------------------------------------------------------------------------------------------------

	private static IllegalArgumentException buildFails(ConsoleFreemarkerMixin.Builder b) {
		return assertThrows(IllegalArgumentException.class, b::build);
	}

	@Test void a01_unknownDependsOn() {
		var e = buildFails(ConsoleFreemarkerMixin.create()
			.registerToolkitPack(ToolkitPack.create("x").kind(ToolkitPack.Kind.VENDOR).js("/x.js").dependsOn("ghost").build()));
		assertString("Unknown toolkit pack 'ghost' (dependsOn of pack 'x').", e.getMessage());
	}

	@Test void a02_cycle() {
		var e = buildFails(ConsoleFreemarkerMixin.create()
			.registerToolkitPack(ToolkitPack.create("a").kind(ToolkitPack.Kind.VENDOR).dependsOn("b").build())
			.registerToolkitPack(ToolkitPack.create("b").kind(ToolkitPack.Kind.VENDOR).dependsOn("a").build()));
		assertString("Toolkit pack cycle: a \u2192 b \u2192 a.", e.getMessage());
	}

	@Test void a03_unknownProvided() {
		assertString("Unknown toolkit pack 'ghost' (providedPacks).", buildFails(ConsoleFreemarkerMixin.create().providedPacks("ghost")).getMessage());
	}

	@Test void a04_unknownCardRequires() {
		assertString("Unknown toolkit pack 'ghost' (cardRequires for type 'html').", buildFails(ConsoleFreemarkerMixin.create().cardRequires("html", "ghost")).getMessage());
	}

	@Test void a05_vendorOnRuntime() {
		var e = buildFails(ConsoleFreemarkerMixin.create()
			.registerToolkitPack(ToolkitPack.create("v").kind(ToolkitPack.Kind.VENDOR).dependsOn("views").build()));
		assertString("VENDOR toolkit pack 'v' cannot depend on RUNTIME pack 'views'.", e.getMessage());
	}

	@Test void a06_validBuilder_builds() {
		assertNotNull(ConsoleFreemarkerMixin.create()
			.registerToolkitPack(ToolkitPack.create("export").kind(ToolkitPack.Kind.VENDOR).js("/x.js").dependsOn("datatables-buttons").build())
			.providedPacks("jquery")
			.cardRequires("datatables", "export")
			.build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - recording from cards
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_noCards_noVendor() throws Exception {
		var body = get(Host.class, "no-cards");
		assertFalse(body.contains("/webjars/jquery/"), () -> body);
		assertFalse(body.toLowerCase().contains("datatables"), () -> body);
	}

	@Test void b02_oneTable_withoutToolkit_loadsVendorOnly() throws Exception {
		var body = get(Host.class, "one-table");
		assertBefore(body, "/jquery.min.js", "/js/dataTables.min.js", "juneau-datatables.js");
		assertFalse(body.contains("juneau-views.js"), () -> body);  // datatables doesn't require views
	}

	@Test void b03_twoTables_eachAssetOnce() throws Exception {
		var body = get(Host.class, "two-tables");
		for (var s : List.of("/jquery.min.js", "/js/dataTables.min.js", "juneau-datatables.js", "dataTables.dataTables.min.css"))
			assertEquals(1, count(body, s), () -> s + " in:\n" + body);
	}

	@Test void b04_requiresString() throws Exception {
		var body = get(Host.class, "requires-string");
		assertBefore(body, "juneau-views.js", "juneau-calendar.js");
	}

	@Test void b05_requiresSeq_addsToTypePacks() throws Exception {
		var body = get(Host.class, "requires-seq");
		assertBefore(body, "/js/dataTables.min.js", "juneau-datatables.js", "dataTables.buttons.min.js", "buttons.html5.min.js", "juneau-views.js");
		assertTrue(body.contains("buttons.dataTables.min.css"), () -> body);
	}

	@Test void b06_requiresEmpty_isNothing() throws Exception {
		var body = get(Host.class, "requires-empty");
		assertFalse(body.contains("/webjars/jquery/"), () -> body);
		assertFalse(body.contains("juneau-calendar.js"), () -> body);
	}

	@Test void b07_requiresUnknown_namesCard() throws Exception {
		var body = error("requires-unknown");
		assertTrue(body.contains("Unknown toolkit pack 'ghost' (<@card id='box'> requires=)."), () -> body);
	}

	@Test void b08_requiresUnknown_autoId() throws Exception {
		var body = error("requires-unknown-auto");
		assertTrue(body.contains("Unknown toolkit pack 'ghost' (<@card id='jc-card-1'> requires=)."), () -> body);
	}

	@Test void b09_cardInIncludedTemplate_isRecorded() throws Exception {
		var body = get(Host.class, "include-host");
		assertBefore(body, "/jquery.min.js", "juneau-datatables.js", "juneau-views.js");
	}

	@Test void b10_looseMarkup_isNotRecorded_unlessHtmlCardsRequire() throws Exception {
		var plain = get(Host.class, "loose-markup");
		assertFalse(plain.contains("juneau-datatables.js"), () -> plain);
		// Loose markup with no authored card must not trigger the html mapping either.
		var onlyLoose = get(HtmlRequiresHost.class, "loose-markup-only");
		assertFalse(onlyLoose.contains("juneau-datatables.js"), () -> onlyLoose);
		var mapped = get(HtmlRequiresHost.class, "loose-markup");
		assertBefore(mapped, "/jquery.min.js", "juneau-datatables.js", "juneau-views.js");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - provided packs and kinds
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_providedJquery_onlyDataTables() throws Exception {
		var body = get(ProvidedHost.class, "one-table");
		assertFalse(body.contains("/jquery.min.js"), () -> body);
		assertBefore(body, "/js/dataTables.min.js", "juneau-datatables.js");
	}

	@Test void c02_threeArgPack_isRuntime_afterVendor() throws Exception {
		var body = get(ProbeHost.class, "probe");
		assertBefore(body, "juneau-datatables.js", "juneau-views.js");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - mounting
	//-----------------------------------------------------------------------------------------------------------------

	private static String jqueryUrl(String body) {
		var m = Pattern.compile("src=\"([^\"]*/jquery\\.min\\.js[^\"]*)\"").matcher(body);
		assertTrue(m.find(), () -> body);
		return m.group(1).replace("&amp;", "&");
	}

	@Test void d01_mountedWebJars_serveTheEmittedUrl() throws Exception {
		try (var c = MockRestClient.buildLax(MountedHost.class)) {
			var url = jqueryUrl(c.get("/d/one-table").run().assertStatus(200).getContent().asString());
			c.get(url).run().assertStatus(200);
		}
	}

	@Test void d02_unmountedWebJars_404() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class)) {
			var url = jqueryUrl(c.get("/d/one-table").run().assertStatus(200).getContent().asString());
			c.get(url).run().assertStatus(404);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e - duplicates against the page's own init=
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_initJquery_skipsPackJquery() throws Exception {
		var body = get(Host.class, "dup-init");
		assertEquals(1, count(body, "jquery.min.js"), () -> body);
		assertTrue(body.contains("/app/static/jquery.min.js"), () -> body);
		assertFalse(body.contains("/webjars/jquery/"), () -> body);
		assertTrue(body.contains("/js/dataTables.min.js"), () -> body);
	}
}
