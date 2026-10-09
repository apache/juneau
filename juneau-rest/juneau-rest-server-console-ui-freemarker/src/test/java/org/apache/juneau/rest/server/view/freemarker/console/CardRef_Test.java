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
 * Placing PageSpec cards from a page template with {@code <@card ref>}: placement and append order, and the
 * already-placed, unknown-card and mixed-attribute rejections.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class CardRef_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/")
				.chromeTemplate("admin/console-chrome-bare.ftlh").build();
		}
		private static PageSpec spec(String template) {
			return PageSpec.create().template(template).html("a", "<p>a</p>").html("b", "<p>b</p>").html("c", "<p>c</p>");
		}
		@RestGet(path="/combo")
		public View combo(RestRequest req) { return spec("admin/card-ref-combo.ftlh").view(req); }
		@RestGet(path="/local-only")
		public View localOnly(RestRequest req) { return spec("admin/card-ref-local-only.ftlh").view(req); }
		@RestGet(path="/no-template")
		public View noTemplate(RestRequest req) {
			return PageSpec.create().html("a", "<p>a</p>").html("b", "<p>b</p>").html("c", "<p>c</p>").view(req);
		}
		@RestGet(path="/duplicate")
		public View duplicate(RestRequest req) { return spec("admin/card-ref-duplicate.ftlh").view(req); }
		@RestGet(path="/unknown")
		public View unknown(RestRequest req) {
			return PageSpec.create().html("x", "<p>x</p>").html("y", "<p>y</p>")
				.template("admin/card-ref-unknown.ftlh").view(req);
		}
		@RestGet(path="/withattr")
		public View withAttr(RestRequest req) { return spec("admin/card-ref-withattr.ftlh").view(req); }
		@RestGet(path="/withbody")
		public View withBody(RestRequest req) { return spec("admin/card-ref-withbody.ftlh").view(req); }
		@RestGet(path="/empty")
		public View empty(RestRequest req) { return spec("admin/card-ref-empty.ftlh").view(req); }
		@RestGet(path="/blankbody")
		public View blankBody(RestRequest req) { return spec("admin/card-ref-blankbody.ftlh").view(req); }
		@RestGet(path="/outside")
		public View outside(RestRequest req) { return spec("admin/card-ref-outside.ftlh").view(req); }
		@RestGet(path="/nested")
		public View nested(RestRequest req) { return spec("admin/card-ref-nested.ftlh").view(req); }
		@RestGet(path="/macro")
		public View macro(RestRequest req) { return spec("admin/card-ref-macro.ftlh").view(req); }
		@RestGet(path="/manyattrs")
		public View manyAttrs(RestRequest req) { return spec("admin/card-ref-manyattrs.ftlh").view(req); }
	}

	private static String get(String path, int status) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get(path).run()) {
			rsp.assertStatus(status);
			return rsp.getContent().asString();
		}
	}

	private static List<String> order(String path) throws Exception {
		var ids = new ArrayList<String>();
		for (var o : assertPage(get(path, 200)).isValid().contract().getList("cards"))
			ids.add(String.valueOf(((Map<?,?>)o).get("id")));
		return ids;
	}

	@Test void a01_refPlacesBuiltCardBetweenAuthoredCards_restAppended() throws Exception {
		assertEquals(List.of("x1", "b", "x2", "a", "c"), order("/combo"));
	}

	@Test void a02_authoredCardsOnly_builtCardsAppendedInDeclarationOrder() throws Exception {
		assertEquals(List.of("local", "a", "b", "c"), order("/local-only"));
	}

	@Test void a03_noTemplate_isTheCardsInOrder() throws Exception {
		assertEquals(List.of("a", "b", "c"), order("/no-template"));
	}

	@Test void b01_placingTwice_isRejected() throws Exception {
		assertTrue(get("/duplicate", 500).contains("<@card ref='a'> places a PageSpec card that was already placed."));
	}

	@Test void b02_unknownRef_namesDeclaredCards() throws Exception {
		assertTrue(get("/unknown", 500).contains("<@card ref='nope'> names no PageSpec card; declared: 'x, y'."));
	}

	@Test void b03_refWithAnotherAttribute_isRejected() throws Exception {
		assertTrue(get("/withattr", 500).contains("<@card ref='a'> must not also set 'title' or a body."));
	}

	@Test void b04_refWithABody_isRejected() throws Exception {
		assertTrue(get("/withbody", 500).contains("<@card ref='a'> must not have a body."));
	}

	@Test void b05_emptyRef_isAnUnknownCard() throws Exception {
		assertTrue(get("/empty", 500).contains("<@card ref=''> names no PageSpec card; declared: 'a, b, c'."));
	}

	@Test void a04_whitespaceOnlyBody_countsAsNoBody() throws Exception {
		assertEquals(List.of("a", "b", "c"), order("/blankbody"));
	}

	@Test void b06_refOutsidePage_isRejected() throws Exception {
		assertTrue(get("/outside", 500).contains("<@card> must be nested inside <@page>."));
	}

	@Test void b07_refInsideCard_isRejected() throws Exception {
		assertTrue(get("/nested", 500).contains("<@card> cannot be nested inside another <@card>."));
	}

	@Test void a05_refInsideContainerMacro_placesTheCard() throws Exception {
		// The macro's wrapper markup before and after the nested ref becomes bare segment cards around it.
		assertEquals(List.of("jc-seg-1", "b", "jc-seg-2", "a", "c"), order("/macro"));
	}

	@Test void b08_severalExtraAttributes_reportsTheFirstInFixedOrder() throws Exception {
		assertTrue(get("/manyattrs", 500).contains("<@card ref='a'> must not also set 'id' or a body."));
	}
}
