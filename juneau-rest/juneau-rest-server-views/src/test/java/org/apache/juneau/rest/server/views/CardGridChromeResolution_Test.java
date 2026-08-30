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
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Serve-time chrome resolution through the {@link CardGridTable} emit path: the card hosts' own
 * resolve/paint/restore windows, the {@code $}-presence gate that keeps a template-free grid byte-identical, and the
 * strings deliberately left off the allowlist.
 *
 * <p>
 * {@code CardHost.properties} (same package, {@code src/test/resources}) backs {@code RestRequest.getMessages()}:
 * {@code grid.title=Dashboard}, {@code card.title=Open Orders}, {@code card.ref=Reference},
 * {@code card.tooltip=Open full report}, {@code card.pin=Pin card}.
 */
@SuppressWarnings({
	"resource"  // Closeable test fixtures held in static fields; lifecycle managed by the test/framework.
})
class CardGridChromeResolution_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Shared definitions.  Card beans declare no ServerValues of their own, so every resolution below runs through
	// the request's own $L var - exactly the localization case the pass exists for.
	//------------------------------------------------------------------------------------------------------------------

	static final CardGrid GRID = CardGrid.create("dash")
		.title("$L{grid.title}")
		.cards(Card.create("orders", "$L{card.title}")
			.body(CardFieldList.create().fields(
				CardField.of("ref", "$L{card.ref}", "R-1"),
				CardField.of("plain", "Plain", "$L{card.ref}"))));   // value is data, never chrome

	/** Excluded-by-design strings: a raw-markup body, an action tooltip, and a menu item label. */
	static final CardGrid EXCLUDED = CardGrid.create("excl")
		.cards(
			Card.create("raw", "Raw").body(CardContent.create().content("<b>$L{card.ref}</b>")),
			Card.create("acts", "Acts")
				.body(CardFieldList.create().fields(CardField.of("k", "L", "v")))
				.actions(
					HeaderAction.link("open", "external", "$L{card.tooltip}", "/reports/1"),
					HeaderAction.menu("more", "overflow", "More")
						.menu(MenuItem.safe("pin", "$L{card.pin}", "card-pin"))));

	/** No {@code $} anywhere: the gate must never lock, resolve, or mutate this one. */
	static final CardGrid LITERAL = CardGrid.create("lit")
		.title("Dashboard")
		.cards(Card.create("orders", "Open Orders")
			.body(CardFieldList.create().fields(CardField.of("ref", "Reference", "R-1"))));

	//------------------------------------------------------------------------------------------------------------------
	// Host servlet.  $L is already part of the default REST var set, so no varResolver override is needed.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(mixins=ViewsMixin.class)
	public static class CardHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/grid") public HttpResource grid(RestRequest req) {
			return html(Html.of(CardGridTable.ofGrid(req, GRID)));
		}

		@RestGet(path="/card") public HttpResource card(RestRequest req) {
			return html(Html.of(CardGridTable.of(req, GRID.cards.get(0))));
		}

		@RestGet(path="/excluded") public HttpResource excluded(RestRequest req) {
			return html(Html.of(CardGridTable.ofGrid(req, EXCLUDED)));
		}

		private static HttpResource html(String markup) {
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(CardHost.class);

	private static String body(String path) throws Exception {
		return c.get(path).run().assertStatus(200).getContent().asString();
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The three allowlisted card-chrome strings resolve.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_gridTitle_resolves() throws Exception {
		var html = body("/grid");
		assertTrue(html.contains(">Dashboard<"), html);
		assertFalse(html.contains("$L{grid.title}"), html);
	}

	@Test void a02_cardTitle_resolves() throws Exception {
		var html = body("/grid");
		assertTrue(html.contains(">Open Orders<"), html);
		assertFalse(html.contains("$L{card.title}"), html);
	}

	@Test void a03_cardFieldLabel_resolves() throws Exception {
		var html = body("/grid");
		assertTrue(html.contains("<dt>Reference</dt>"), html);
		// Scoped to the <dt> half: the sibling field's data value is the same template, and stays literal (see b01).
		assertFalse(html.contains("<dt>$L{card.ref}</dt>"), html);
	}

	@Test void a04_cardHost_resolvesWithNoEnclosingGrid() throws Exception {
		// The public per-card overload takes the card's own monitor with no grid above it.
		var html = body("/card");
		assertTrue(html.contains(">Open Orders<"), html);
		assertTrue(html.contains("<dt>Reference</dt>"), html);
		assertFalse(html.contains(">Dashboard<"), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) What stays off the allowlist, and what a resolved response leaves behind.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_fieldValue_isDataNotChrome() throws Exception {
		// The <dd> half carries the field's data; a $-shaped value is emitted literally, like a view's row cells.
		var html = body("/grid");
		assertTrue(html.contains("$L{card.ref}</dd>"), html);
	}

	@Test void b02_cardContent_staysVerbatim() throws Exception {
		// Resolving a raw-markup sink would put an SVL-resolved value into trusted HTML; it is excluded by design.
		var html = body("/excluded");
		assertTrue(html.contains("<b>$L{card.ref}</b>"), html);
	}

	@Test void b03_actionTooltipAndMenuLabel_stayExcluded() throws Exception {
		var html = body("/excluded");
		assertTrue(html.contains("aria-label=\"$L{card.tooltip}\""), html);
		assertTrue(html.contains("$L{card.pin}"), html);
		assertFalse(html.contains("Open full report"), html);
		assertFalse(html.contains("Pin card"), html);
	}

	@Test void b04_sharedDefs_restoredAfterTheResponse() throws Exception {
		body("/grid");
		assertEquals("$L{grid.title}", GRID.title);
		var card = GRID.cards.get(0);
		assertEquals("$L{card.title}", card.title);
		assertEquals("$L{card.ref}", ((CardFieldList) card.body).fields.get(0).label);
	}

	@Test void b05_twoSequentialRequests_eachResolveFreshly() throws Exception {
		assertTrue(body("/grid").contains(">Dashboard<"));
		assertTrue(body("/grid").contains(">Dashboard<"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Byte stability: no request and no template each cost nothing.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_requestFreeGrid_staysLiteral() {
		var html = Html.of(CardGridTable.of(GRID));
		assertTrue(html.contains("$L{grid.title}"), html);
		assertTrue(html.contains("$L{card.title}"), html);
		assertTrue(html.contains("$L{card.ref}"), html);
	}

	@Test void c02_gridWithNoDollar_isNeverMutated() {
		var before = Html.of(CardGridTable.of(LITERAL));
		assertEquals(before, Html.of(CardGridTable.of(LITERAL)));
		assertEquals("Dashboard", LITERAL.title);
		assertEquals("Open Orders", LITERAL.cards.get(0).title);
	}
}
