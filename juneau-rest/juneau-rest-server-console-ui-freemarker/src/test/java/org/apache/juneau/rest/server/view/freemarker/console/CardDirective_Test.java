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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@code <@card>} shared directive: adds a {@code cards[]} entry (html with a {@code <template>}, or
 * the datatables bridge), and markup is never JSON-escaped into the contract.
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
				.chromeTemplate("admin/console-chrome-bare.ftlh")
				.build();
		}
		@RestGet(path="/cards-html")
		public View cardsHtml() { return FreemarkerView.of("admin/page-cards-html.ftlh"); }
		@RestGet(path="/cards-format")
		public View cardsFormat() { return FreemarkerView.of("admin/page-cards-format.ftlh"); }
		@RestGet(path="/cards-bogus")
		public View cardsBogus() { return FreemarkerView.of("admin/page-cards-bogus.ftlh"); }
		@RestGet(path="/card-datatables")
		public View cardDatatables() { return FreemarkerView.of("admin/page-card-datatables.ftlh"); }
		@RestGet(path="/card-datatables-noid")
		public View cardDatatablesNoId() { return FreemarkerView.of("admin/page-card-datatables-noid.ftlh"); }
		@RestGet(path="/card-datatables-nocols")
		public View cardDatatablesNoCols() { return FreemarkerView.of("admin/page-card-datatables-nocols.ftlh"); }
		@RestGet(path="/card-datatables-search")
		public View cardDatatablesSearch() { return FreemarkerView.of("admin/page-card-datatables-search.ftlh"); }
		@RestGet(path="/card-datatables-badcustom")
		public View cardDatatablesBadCustom() { return FreemarkerView.of("admin/page-card-datatables-badcustom.ftlh"); }
		@RestGet(path="/cards-class")
		public View cardsClass() { return FreemarkerView.of("admin/page-cards-class.ftlh"); }
		@RestGet(path="/cards-class-bad")
		public View cardsClassBad() { return FreemarkerView.of("admin/page-cards-class-bad.ftlh"); }
		@RestGet(path="/cards-class-digit")
		public View cardsClassDigit() { return FreemarkerView.of("admin/page-cards-class-digit.ftlh"); }
		@RestGet(path="/card-html-template")
		public View cardHtmlTemplate() { return FreemarkerView.of("admin/page-card-html-template.ftlh"); }
	}

	static String get(String path) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get(path).run()) {
			rsp.assertStatus(200);
			return rsp.getContent().asString();
		}
	}

	/** The contract entry for card {@code id}. */
	static JsonMap card(String body, String id) {
		for (var o : assertPage(body).isValid().contract().getList("cards"))
			if (id.equals(((JsonMap)o).getString("id")))
				return (JsonMap)o;
		throw new AssertionError("no card '" + id + "' in " + body);
	}

	@Test void b01_omitType_isHtml_bodyBecomesTemplate() throws Exception {
		var body = get("/cards-html");
		assertPage(body).isValid().hasCardOrder("jc-card-1", "second")
			.hasCard("jc-card-1", "html").templateContains("jc-card-1", "<h1>Title</h1>")
			.hasCard("second", "html").templateContains("second", "<p>more</p>");
		assertFalse(body.contains("&lt;h1"), () -> "double-escaped card body: " + body);
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
			assertTrue(body.contains("<@card> unknown attribute 'bogus'."), () -> body);
		}
	}

	@Test void b08_typeDatatables_liftedSlotMetaInContract() throws Exception {
		var body = get("/card-datatables");
		assertPage(body).hasCardOrder("jc-card-1", "releases").hasCard("releases", "datatables")
			.templateContains("jc-card-1", "<h1>All Releases</h1>");
		var table = card(body, "releases").getMap("table");
		// The contract carries the author catalog; the browser lifts it to VIEW_META at mount time.
		assertEquals("/rest/releases/data", table.getString("dataUrl"));
		var col0 = table.getList("columns").getMap(0);
		assertBean(col0, "key,label", "name,Name");
		// The bridge card has no server markup; the shell renders its mount (spec §4.6).
		assertFalse(body.contains("data-juneau-layout=\"wide\""), () -> body);
	}

	@Test void b09_typeDatatables_missingId_is500() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/card-datatables-noid").run()) {
			rsp.assertStatus(500);
			var b = rsp.getContent().asString();
			assertTrue(b.contains("<@card type=\"datatables\"> requires id=."), () -> b);
		}
	}

	@Test void b10_typeDatatables_missingColumns_is500() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/card-datatables-nocols").run()) {
			rsp.assertStatus(500);
			var b = rsp.getContent().asString();
			assertTrue(b.contains("'columns'"), () -> b);
		}
	}

	/** The operator {@code name}s of a lifted VIEW_META {@code search.operators} list, in wire order. */
	private static List<String> opNames(JsonList operators) {
		var l = new ArrayList<String>();
		for (var i = 0; i < operators.size(); i++)
			l.add(operators.getMap(i).getString("name"));
		return l;
	}

	@Test void b10a_typeDatatables_emitsPerColumnSearchMetadata() throws Exception {
		var view = card(get("/card-datatables-search"), "releases").getMap("table");
		var cols = view.getList("columns");

		// Column 0 (text, list absent) → the full text universe, each operator carrying help; no custom leaked in.
		var name = cols.getMap(0).getMap("search");
		assertEquals("text", name.getString("type"));
		assertEquals(List.of("$eq", "$eqic", "$ne", "$in", "$and", "$or", "$not", "$contains", "$prefix", "$regex", "$blank"),
			opNames(name.getList("operators")));
		var eq = name.getList("operators").getMap(0);
		assertBean(eq, "name,minArgs,maxArgs,combinator,custom", "$eq,1,1,false,false");
		assertFalse(eq.containsKey("help"));

		// Column 1 (numeric, explicit allow-list) → exactly the named applicable operators, in the author's order.
		var count = cols.getMap(1).getMap("search");
		assertEquals("numeric", count.getString("type"));
		assertEquals(List.of("$eq", "$gt", "$between"), opNames(count.getList("operators")));

		// Column 2 (text + custom $near) → the custom is appended after the built-ins, flagged, carrying its help.
		var statusOps = cols.getMap(2).getMap("search").getList("operators");
		var near = statusOps.getMap(statusOps.size() - 1);
		assertBean(near, "name,custom,help", "$near,true,Fuzzy match. Example: $near(jhon)");

		// Column 3 (no searchType) → non-searchable: no search block at all.
		assertNull(cols.getMap(3).get("search"), cols::toString);
	}

	@Test void b10b_typeDatatables_customOperatorNotAnObject_is500() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/card-datatables-badcustom").run()) {
			rsp.assertStatus(500);
			var b = rsp.getContent().asString();
			assertTrue(b.contains("each customOperator must be an object."), () -> b);
		}
	}

	@Test void b11_htmlCard_templateReference() throws Exception {
		var body = get("/card-html-template");
		assertPage(body).hasCardOrder("dialogBody", "dialog").templateContains("dialogBody", "<p>fallback</p>");
		assertEquals("dialogBody", card(body, "dialog").getString("template"));
		assertFalse(body.contains("<template data-card=\"dialog\">"), () -> body);
		assertFalse(body.contains("&lt;p"), () -> body);
	}

	@Test void b14_noSidecars() throws Exception {
		for (var path : new String[] {"/cards-html", "/card-datatables", "/card-html-template"})
			assertFalse(get(path).contains("juneau-card-sidecar"), path);
	}

	@Test void b15_classAttr_singleClass_isInContract() throws Exception {
		var body = get("/cards-class");
		assertEquals("ssc-skills", card(body, "one").getString("class"));
		assertEquals("ssc-scripts", card(body, "dt").getString("class"));
	}

	@Test void b16_classAttr_multipleClasses_areNormalizedToSingleSpaces() throws Exception {
		assertEquals("ssc-skills x_1 -y", card(get("/cards-class"), "many").getString("class"));
	}

	@Test void b17_classAttr_absent_emitsNoClassKey() throws Exception {
		assertFalse(card(get("/cards-class"), "none").containsKey("class"));
	}

	@Test void b18_classAttr_invalidToken_failsTheRender() throws Exception {
		for (var path : new String[] {"/cards-class-bad", "/cards-class-digit"})
			try (var c = MockRestClient.buildLax(Host.class);
				var rsp = c.get(path).run()) {
				rsp.assertStatus(500);
				var body = rsp.getContent().asString();
				assertTrue(body.contains("<@card id='bad'> class='"), () -> body);
				assertTrue(body.contains("must be a space-separated list of CSS class names"), () -> body);
			}
	}
}
