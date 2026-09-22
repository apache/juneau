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
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@code <@card>} shared directive: always emits a {@code .jc-card}; {@code type=} only
 * (no {@code format=}); omit {@code type} = html sugar.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class CardDirective_Test extends TestBase {

	// renderResponseStackTraces=true so the reject() diagnostic reaches the 500 body verbatim (F1 / b02): the
	// secure default suppressed-body path runs RestContext.scrubForXss, which replaces '<' '>' '&' with spaces
	// as an XSS defense, so literal "<@card>" cannot survive there.  In DEBUG mode the full stack trace (with
	// the plain-cause IllegalArgumentException carrying the frozen sentence) is written unscrubbed.
	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/test-chrome.ftlh")
				.build();
		}
		@RestGet(path="/cards-html")
		public View cardsHtml() { return FreemarkerView.of("admin/page-cards-html.ftlh"); }
		@RestGet(path="/cards-format")
		public View cardsFormat() { return FreemarkerView.of("admin/page-cards-format.ftlh"); }
		@RestGet(path="/cards-bogus")
		public View cardsBogus() { return FreemarkerView.of("admin/page-cards-bogus.ftlh"); }
		@RestGet(path="/card-json5")
		public View cardJson5() { return FreemarkerView.of("admin/page-card-json5.ftlh"); }
		@RestGet(path="/card-js")
		public View cardJs() { return FreemarkerView.of("admin/page-card-js.ftlh"); }
		@RestGet(path="/card-js-noid")
		public View cardJsNoId() { return FreemarkerView.of("admin/page-card-js-noid.ftlh"); }
		@RestGet(path="/card-js-blank")
		public View cardJsBlank() { return FreemarkerView.of("admin/page-card-js-blank.ftlh"); }
		@RestGet(path="/card-datatables")
		public View cardDatatables() { return FreemarkerView.of("admin/page-card-datatables.ftlh"); }
		@RestGet(path="/card-datatables-noid")
		public View cardDatatablesNoId() { return FreemarkerView.of("admin/page-card-datatables-noid.ftlh"); }
		@RestGet(path="/card-datatables-nocols")
		public View cardDatatablesNoCols() { return FreemarkerView.of("admin/page-card-datatables-nocols.ftlh"); }
		@RestGet(path="/card-html-template")
		public View cardHtmlTemplate() { return FreemarkerView.of("admin/page-card-html-template.ftlh"); }
		@RestGet(path="/card-html-source")
		public View cardHtmlSource() { return FreemarkerView.of("admin/page-card-html-source.ftlh"); }
		@RestGet(path="/card-html-override-noid")
		public View cardHtmlOverrideNoId() { return FreemarkerView.of("admin/page-card-html-override-noid.ftlh"); }
		@RestGet(path="/card-json-datatables")
		public View cardJsonDatatables() { return FreemarkerView.of("admin/page-card-json-datatables.ftlh"); }
	}

	// Composes ViewsMixin + WidgetsMixin so BOTH the "views" and "calendar" toolkit packs' asset routes are
	// mounted: the "calendar" pack resolves through WidgetsMixin::widgetAssetUrl, the "views" pack through
	// ViewsMixin::viewAssetUrl, against the in-flight request.
	@Rest(mixins={FreemarkerMixin.class, ViewsMixin.class, WidgetsMixin.class}, renderResponseStackTraces="true")
	public static class CalendarHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/test-chrome.ftlh")
				.build();
		}
		@RestGet(path="/card-calendar")
		public View cardCalendar() { return FreemarkerView.of("admin/page-card-calendar.ftlh"); }
		@RestGet(path="/card-calendar-noview")
		public View cardCalendarNoView() { return FreemarkerView.of("admin/page-card-calendar-noview.ftlh"); }
		@RestGet(path="/card-calendar-noid")
		public View cardCalendarNoId() { return FreemarkerView.of("admin/page-card-calendar-noid.ftlh"); }
	}

	static String get(String path) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get(path).run()) {
			rsp.assertStatus(200);
			return rsp.getContent().asString();
		}
	}

	/** Extracts, script-unescapes, and parses the strict-JSON sidecar for the given card id. */
	static JsonMap sidecar(String body, String id) throws Exception {
		var marker = "data-juneau-card-sidecar=\"" + id + "\">";
		var start = body.indexOf(marker);
		assertTrue(start >= 0, () -> "no sidecar for '" + id + "' in " + body);
		start += marker.length();
		var end = body.indexOf("</script>", start);
		assertTrue(end > start, () -> body);
		return Json.DEFAULT.read(body.substring(start, end), JsonMap.class);
	}

	static int count(String haystack, String needle) {
		var n = 0;
		for (var i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length()))
			n++;
		return n;
	}

	@Test void b01_omitType_isHtmlSugar_emitsJcCard() throws Exception {
		var body = get("/cards-html");
		assertTrue(body.contains("class=\"jc-card\"") || body.contains("class='jc-card'"), () -> body);
		assertTrue(body.contains("<h1>Title</h1>"), () -> body);
		assertFalse(body.contains("&lt;h1"), () -> "double-escaped card body: " + body);
		assertTrue(body.contains("id=\"second\"") || body.contains("id='second'"), () -> body);
		assertFalse(body.contains("data-juneau-layout=\"wide\""), () -> body);
		assertFalse(body.contains("juneau-card-sidecar"), () -> body);
		assertFalse(body.contains("slds-"), () -> body);
	}

	@Test void b02_formatAttr_isRejected() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/cards-format").run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains("<@card> uses type= only; format= is not a valid attribute."), () -> body);
		}
	}

	@Test void b03_unknownAttr_isRejected() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/cards-bogus").run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains("unknown attribute"), () -> body);
		}
	}

	@Test void b04_typeJson_htmlEnvelope_desugarsToJcCard_notEscaped() throws Exception {
		var body = get("/card-json5");
		assertTrue(body.contains("class=\"jc-card\"") || body.contains("class='jc-card'"), () -> body);
		assertTrue(body.contains("<h1>Title</h1>"), () -> body);
		assertFalse(body.contains("&lt;h1"), () -> "double-escaped json envelope content: " + body);
	}

	@Test void b05_typeJs_emitsJcCardWithId_andStrictJsonSidecar() throws Exception {
		var body = get("/card-js");
		assertTrue(body.contains("class=\"jc-card\" id=\"probes\"") || body.contains("id=\"probes\""), () -> body);
		assertTrue(body.contains("class=\"juneau-card-sidecar\""), () -> body);
		assertTrue(body.contains("data-juneau-card-sidecar=\"probes\""), () -> body);
		// Sidecar is strict JSON (quoted keys), not JSON5.
		assertTrue(body.contains("\"contractVersion\":\"1\""), () -> body);
		assertTrue(body.contains("\"id\":\"probes\""), () -> body);
		assertTrue(body.contains("\"populate\":\"probes\""), () -> body);
		assertFalse(body.contains("populate:"), () -> "sidecar must be strict JSON, not JSON5: " + body);
	}

	@Test void b06_typeJs_missingId_is500() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/card-js-noid").run()) {
			rsp.assertStatus(500);
		}
	}

	@Test void b07_typeJs_blankBody_is500() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/card-js-blank").run()) {
			rsp.assertStatus(500);
		}
	}

	@Test void b08_typeDatatables_innerWide_andLiftedSlotMetaSidecar() throws Exception {
		var body = get("/card-datatables");
		// Two cards; the title card and the datatables card.
		assertEquals(2, count(body, "class=\"jc-card\""), () -> body);
		// Title card carries the <h1> and NO wide layout on the card itself.
		assertTrue(body.contains("<h1>All Releases</h1>"), () -> body);
		// wide lives on the INNER body div, not the outer .jc-card.
		assertTrue(body.contains("<div id=\"releases\" class=\"jc-card-body\" data-juneau-layout=\"wide\">"), () -> body);
		assertTrue(body.contains("<div class=\"jc-card\" data-juneau-card=\"datatables\">"), () -> body);

		var s = sidecar(body, "releases");
		assertEquals("1", s.getString("contractVersion"));   // SIDECAR_CONTRACT_VERSION
		assertEquals("releases", s.getString("id"));
		var table = s.getMap("table");
		assertEquals("1", table.getString("contractVersion")); // ViewSlot.CONTRACT_VERSION
		assertEquals("wide", table.getString("layout"));
		var view = table.getMap("view");
		assertEquals("4", view.getString("contractVersion"));  // ViewDef.CONTRACT_VERSION
		assertEquals("releases", view.getString("id"));
		assertEquals("/rest/releases/data", view.getString("dataUrl"));
		var col0 = view.getList("columns").getMap(0);
		assertEquals("name", col0.getString("data"));          // author key -> data
		assertEquals("Name", col0.getString("title"));         // author label -> title
	}

	@Test void b09_typeDatatables_missingId_is500() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/card-datatables-noid").run()) {
			rsp.assertStatus(500);
		}
	}

	@Test void b10_typeDatatables_missingColumns_is500() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/card-datatables-nocols").run()) {
			rsp.assertStatus(500);
		}
	}

	@Test void b11_htmlCard_withTemplate_emitsStrictJsonSidecar_keepsBodyMarkup() throws Exception {
		var body = get("/card-html-template");
		// The card is a plain .jc-card with the author id, and it keeps its fallback body markup.
		assertTrue(body.contains("class=\"jc-card\" id=\"dialog\""), () -> body);
		assertTrue(body.contains("<p>fallback</p>"), () -> body);
		assertFalse(body.contains("&lt;p"), () -> "double-escaped card body: " + body);
		// It carries a strict-JSON sidecar copying template= verbatim.
		var s = sidecar(body, "dialog");
		assertEquals("1", s.getString("contractVersion"));   // SIDECAR_CONTRACT_VERSION, not aliased to VIEW_META
		assertEquals("dialog", s.getString("id"));
		assertEquals("dialogBody", s.getString("template"));
		// A name-only template sidecar carries no populate/table (the runtime paints it via page.templates).
		assertNull(s.get("populate"), () -> body);
		assertNull(s.get("table"), () -> body);
		assertFalse(body.contains("template:"), () -> "sidecar must be strict JSON, not JSON5: " + body);
	}

	@Test void b12_htmlCard_source_isScriptEscaped_roundTripsToLiteral() throws Exception {
		var body = get("/card-html-source");
		// F4: a source holding a literal </script> is emitted script-escaped so it cannot break the element.
		assertTrue(body.contains("\\u003c/script>"), () -> "source </script> was not escaped: " + body);
		// The escaped payload round-trips: unescaping restores the author's exact source string.
		var s = sidecar(body, "widget");
		assertEquals("function(){ return '<b>hi</b></script>'; }", s.getString("source"), () -> body);
	}

	@Test void b13_htmlCard_overrideAttr_missingId_is500() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/card-html-override-noid").run()) {
			rsp.assertStatus(500);
			var b = rsp.getContent().asString();
			assertTrue(b.contains("requires id="), () -> b);
		}
	}

	@Test void b14_markupOnlyHtml_staysSidecarFree() throws Exception {
		// The existing html-sugar fixture (h1 + a plain id) must NOT gain a sidecar now that the html branch
		// can emit one: only an override attribute triggers it.
		var body = get("/cards-html");
		assertFalse(body.contains("juneau-card-sidecar"), () -> body);
	}

	@Test void b15_typeJson_datatablesEnvelope_desugarsToSameLiftedSlotMeta() throws Exception {
		// Desugar pin (Task 11): a type="json" envelope carrying type:'datatables' emits the SAME wide markup
		// + lifted SLOT_META sidecar as the type="datatables" sugar (b08).  The envelope's own 'type' key is
		// ignored by the lift.
		var body = get("/card-json-datatables");
		// One card, wide on the inner body div, data-juneau-card="datatables" on the outer .jc-card.
		assertEquals(1, count(body, "class=\"jc-card\""), () -> body);
		assertTrue(body.contains("<div class=\"jc-card\" data-juneau-card=\"datatables\">"), () -> body);
		assertTrue(body.contains("<div id=\"releases\" class=\"jc-card-body\" data-juneau-layout=\"wide\">"), () -> body);

		var s = sidecar(body, "releases");
		assertEquals("1", s.getString("contractVersion"));   // SIDECAR_CONTRACT_VERSION
		assertEquals("releases", s.getString("id"));
		var table = s.getMap("table");
		assertEquals("1", table.getString("contractVersion")); // ViewSlot.CONTRACT_VERSION
		assertEquals("wide", table.getString("layout"));
		var view = table.getMap("view");
		assertEquals("4", view.getString("contractVersion"));  // ViewDef.CONTRACT_VERSION
		assertEquals("releases", view.getString("id"));
		assertEquals("/rest/releases/data", view.getString("dataUrl"));
		var col0 = view.getList("columns").getMap(0);
		assertEquals("name", col0.getString("data"));          // author key -> data
		assertEquals("Name", col0.getString("title"));         // author label -> title
		// The envelope's own inner 'type' does not leak into the lifted view.
		assertNull(view.get("type"), () -> body);
	}

	@Test void b16_typeCalendar_emitsJcCardWithCalendarMount_sidecar_andBothPacks() throws Exception {
		var body = get2("/card-calendar");
		// One .jc-card wrapping the inner data-juneau-calendar mount carrying the author id.
		assertEquals(1, count(body, "class=\"jc-card\""), () -> body);
		assertTrue(body.contains("<div class=\"jc-card\" data-juneau-card=\"calendar\">"), () -> body);
		assertTrue(body.contains(
			"<div id=\"release-cal\" class=\"jc-card-body\" data-juneau-calendar=\"release-cal\""), () -> body);
		// The mount carries the baked contract handshake string the runtime fails-loud on, and the endpoint.
		assertTrue(body.contains("data-juneau-calendar-contract=\"2\""), () -> body);
		assertTrue(body.contains("data-juneau-calendar-endpoint=\"/rest/releases/calendar\""), () -> body);

		// The page-cards sidecar for the card id.
		assertTrue(body.contains("data-juneau-card-sidecar=\"release-cal\""), () -> body);
		var s = sidecar(body, "release-cal");
		assertEquals("1", s.getString("contractVersion"));   // SIDECAR_CONTRACT_VERSION
		assertEquals("release-cal", s.getString("id"));
		assertEquals("/rest/releases/calendar", s.getMap("calendar").getString("dataUrl"));

		// The "calendar" pack URLs (css + js) AND the "views" pack URLs are all emitted.
		assertTrue(body.contains("juneau-calendar.css"), () -> body);
		assertTrue(body.contains("juneau-calendar.js"), () -> body);
		assertTrue(body.contains("juneau-views.js"), () -> body);
		// Shared layer stack: juneau-views.js loads BEFORE juneau-calendar.js (author order views,calendar).
		assertTrue(body.indexOf("juneau-views.js") < body.indexOf("juneau-calendar.js"), () -> body);
		assertFalse(body.contains("slds-"), () -> body);
	}

	@Test void b17_typeCalendar_packNotInferred_toolkitViewsOnly_noCalendarJs() throws Exception {
		// A type="calendar" card whose page lists toolkit="views" (no calendar) emits NO juneau-calendar.js:
		// the pack is an explicit toolkit= entry, never inferred from the card type (Q7 A / I3).
		var body = get2("/card-calendar-noview");
		assertTrue(body.contains("data-juneau-calendar=\"release-cal\""), () -> body);   // the card still emits
		assertFalse(body.contains("juneau-calendar.js"), () -> body);
		assertFalse(body.contains("juneau-calendar.css"), () -> body);
	}

	@Test void b18_typeCalendar_missingId_is500() throws Exception {
		try (var c = MockRestClient.buildLax(CalendarHost.class);
			var rsp = c.get("/card-calendar-noid").run()) {
			rsp.assertStatus(500);
		}
	}

	private static String get2(String path) throws Exception {
		try (var c = MockRestClient.buildLax(CalendarHost.class);
			var rsp = c.get(path).run()) {
			rsp.assertStatus(200);
			return rsp.getContent().asString();
		}
	}
}
