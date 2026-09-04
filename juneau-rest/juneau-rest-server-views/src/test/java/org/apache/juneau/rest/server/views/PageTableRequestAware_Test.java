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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * {@link PageTable#of(RestRequest, PageDef)} propagates the request into every child {@code ViewTable.of(req, ...)}.
 *
 * <p>
 * Before this, the page emitter used the request only to resolve its own saved-views base and then delegated to the
 * context-only overload, so a page-hosted table saw no request at all: its CSRF stamp, its own saved-views stamp, and
 * &mdash; the reason this matters &mdash; the shipped {@link ViewDef#serverValues} resolution were all dead on that
 * path.  Those three are the intended consequences of the fix, so they are asserted here rather than discovered later.
 * The context-only {@code of(ctx, pageDef, savedViewsBase)} overload stays request-free and must emit exactly what it
 * always has.
 */
@SuppressWarnings({
	"resource"  // Closeable test fixtures held in static fields; lifecycle managed by the test/framework.
})
class PageTableRequestAware_Test extends TestBase {

	private static final String TOKEN = "tok-page-child";

	private static ViewDef view(String id) {
		return ViewDef.create(id)
			.columns(Column.of("name").title("Name"))
			.build();
	}

	/** A page-hosted view declaring the shipped v1 host, which never ran through a page before this change. */
	static final ViewDef RESOLVING = ViewDef.create("resolving")
		.columns(Column.of("name").title("Col:$FV{env}"))
		.serverValues(ServerValues.create()
			.value("env", s -> s.getBean(RestRequest.class).map(r -> r.getQueryParam("env").orElse("?")).orElse("?")))
		.build();

	private static PageDef leafPage() {
		return PageDef.create("admin").tabs(Tab.create("releases", "Releases").view(view("releases"))).build();
	}

	private static PageDef subtabPage() {
		return PageDef.create("admin").tabs(
			Tab.create("catalog", "Catalog").subtabs(
				Subtab.create("packages", "Packages").view(view("packages")))).build();
	}

	@Rest(mixins=ViewsMixin.class)
	public static class PageTableRequestAwareHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public VarResolver varResolver(VarResolver.Builder b) {
			return b.vars(ServerValuesVar.class).build();
		}

		@RestGet(path="/leaf") public HttpResource leaf(RestRequest req) {
			req.setAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, TOKEN);
			return html(Html.of(PageTable.of(req, leafPage())));
		}

		@RestGet(path="/subtab") public HttpResource subtab(RestRequest req) {
			req.setAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, TOKEN);
			return html(Html.of(PageTable.of(req, subtabPage())));
		}

		@RestGet(path="/resolving") public HttpResource resolving(RestRequest req) {
			return html(Html.of(PageTable.of(req,
				PageDef.create("admin").tabs(Tab.create("t", "T").view(RESOLVING)).build())));
		}

		private static HttpResource html(String markup) {
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(PageTableRequestAwareHost.class);

	private static String body(String path) throws Exception {
		return c.get(path).accept("text/html").run().assertStatus(200).getContent().asString();
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The newly-propagated request reaches the child views
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts the emitted {@code <table>} carries a CSRF stamp holding this request's token, and nothing else: a stamp
	 * that merely <i>contains</i> the token still 403s, because the server compares the attribute's whole text against
	 * the token it published (see {@code ViewTableCsrfStamp_Test} for the stamp's own contract).
	 */
	private static void assertChildCarriesToken(String html) {
		var at = html.indexOf(ViewTable.CSRF_ATTR + "=\"");
		assertTrue(at >= 0, () -> "no CSRF stamp on the page-hosted table:\n" + html);
		var value = html.substring(at + ViewTable.CSRF_ATTR.length() + 2, html.indexOf('"', at + ViewTable.CSRF_ATTR.length() + 2));
		assertEquals(TOKEN, value, () -> "CSRF stamp does not carry this request's token: " + value);
	}

	@Test void a01_pageHostedLeafTableNowCarriesTheCsrfStamp() throws Exception {
		assertChildCarriesToken(body("/leaf"));
	}

	@Test void a02_pageHostedSubtabTableNowCarriesTheCsrfStamp() throws Exception {
		assertChildCarriesToken(body("/subtab"));
	}

	@Test void a03_pageHostedTableNowCarriesItsOwnSavedViewsStamp() throws Exception {
		var html = body("/leaf");
		// The page shell still stamps it (the pre-existing behavior)...
		assertTrue(html.contains(PageTable.SAVED_VIEWS_ATTR + "="), html);
		// ...and now so does each child wrapper, so the count is at least two.
		var first = html.indexOf(ViewTable.SAVED_VIEWS_ATTR + "=");
		assertTrue(html.indexOf(ViewTable.SAVED_VIEWS_ATTR + "=", first + 1) > 0,
			"the page-hosted table wrapper must now carry its own saved-views stamp");
		assertTrue(html.contains(SavedViewsMixin.SAVED_VIEWS_PREFIX), html);
	}

	@Test void a04_shippedViewDefServerValuesNowResolvesForAPageHostedView() throws Exception {
		var html = body("/resolving?env=prod");
		assertTrue(html.contains("Col:prod"), "ViewDef.serverValues must now run through a page");
		assertFalse(html.contains("Col:$FV{env}"), html);
		assertEquals("Col:$FV{env}", RESOLVING.columns.get(0).title, "the author template must be restored");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) The request-free overloads are untouched
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_contextOnlyOverloadStampsNoCsrfOnChildren() {
		var html = Html.of(PageTable.of(leafPage()));
		assertFalse(html.contains(ViewTable.CSRF_ATTR), html);
		assertFalse(html.contains(ViewTable.SAVED_VIEWS_ATTR), html);
	}

	@Test void b02_contextOnlyOverloadWithPreResolvedBaseStampsOnlyTheShell() {
		var html = Html.of(PageTable.of(MarshallingContext.DEFAULT, leafPage(), "/ctx/juneau-saved-views"));
		var first = html.indexOf(PageTable.SAVED_VIEWS_ATTR + "=");
		assertTrue(first >= 0, html);
		assertEquals(-1, html.indexOf(PageTable.SAVED_VIEWS_ATTR + "=", first + 1),
			"the request-free overload must not stamp child wrappers");
	}

	@Test void b03_contextOnlyOverloadOutputIsUnchangedByRequestAwareness() {
		// The two request-free spellings must still agree byte-for-byte.
		assertEquals(Html.of(PageTable.of(leafPage())), Html.of(PageTable.of(MarshallingContext.DEFAULT, leafPage())));
	}
}
