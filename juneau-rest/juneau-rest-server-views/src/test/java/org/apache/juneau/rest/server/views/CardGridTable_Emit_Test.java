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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Markup + escaping tests for the {@link CardGridTable} emitter.
 *
 * <p>
 * Pins the {@code data-juneau-*} DOM contract the {@code juneau-cards.js} runtime depends on: per-card (not
 * per-grid) refresh wiring, the generated {@code aria-labelledby}/title-span id pairing, per-field wrapper
 * {@code <div>}s under an inline {@code grid-template-columns}, the banner host, the closed v1 body dispatch, and
 * &mdash; the security-critical part &mdash; that human strings and the server-painted initial value are
 * entity-escaped so a {@code <script>}-shaped value can never become a live tag.
 */
class CardGridTable_Emit_Test extends TestBase {

	private static CardFieldList staticBody() {
		return CardFieldList.create().columns(3).fields(
			CardField.of("name", "Name", "Widget A"),
			CardField.of("status", "Status", "OK"));
	}

	private static CardFieldList refreshBody() {
		return CardFieldList.create().fields(CardField.of("k", "Label", "v")).refresh("/data/summary");
	}

	private static CardGrid grid() {
		return CardGrid.create("g1").title("Dashboard").minCardPx(320).cards(
			Card.create("c1", "Static").body(staticBody()),
			Card.create("c2", "Live").body(refreshBody().pollIntervalMs(1_000)));
	}

	private static String html(CardGrid g) {
		return Html.of(CardGridTable.of(g));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Grid + card markers
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_gridMarkersAndTitle() {
		var h = html(grid());
		assertTrue(h.contains("data-juneau-card-grid"), h);
		assertTrue(h.contains("data-juneau-card-grid-id=\"g1\""), h);
		assertTrue(h.contains("juneau-view-card-grid-title"), h);
		assertTrue(h.contains(">Dashboard<"), h);
	}

	@Test void a02_minCardPxStampedInline() {
		assertTrue(html(grid()).contains("--jc-card-min:320px"), "expected inline --jc-card-min");
	}

	@Test void a03_minCardPxOmittedWhenUnset() {
		var g = CardGrid.create("g1").cards(Card.create("c1", "T").body(staticBody()));
		assertFalse(html(g).contains("--jc-card-min"), "no inline hint when minCardPx unset (N3)");
	}

	@Test void a04_cardMarkersAndIds() {
		var h = html(grid());
		assertTrue(h.contains("data-juneau-card-id=\"c1\""), h);
		assertTrue(h.contains("data-juneau-card-id=\"c2\""), h);
		assertTrue(h.contains("class=\"juneau-view-card\""), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// aria-labelledby / title-span id pairing (unique across page: gridId + cardId)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a05_titleIdAndAriaLabelledbyPair() {
		var h = html(grid());
		assertTrue(h.contains("id=\"g1-c1-title\""), h);
		assertTrue(h.contains("aria-labelledby=\"g1-c1-title\""), h);
		assertTrue(h.contains("id=\"g1-c2-title\""), h);
		assertTrue(h.contains("aria-labelledby=\"g1-c2-title\""), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Refresh wiring: per refreshable card only
	//------------------------------------------------------------------------------------------------------------------

	@Test void a06_refreshAttrsOnlyOnRefreshableCard() {
		var h = html(grid());
		assertTrue(h.contains("data-juneau-card-refresh=\"/data/summary\""), h);
		assertTrue(h.contains("data-juneau-card-contract=\"" + CardFieldList.CONTRACT_VERSION + "\""), h);
		assertTrue(h.contains("data-juneau-card-refresh-trigger"), h);
		assertTrue(h.contains("data-juneau-card-status"), h);
	}

	@Test void a07_contractIsPerCardNotPerGrid() {
		// Exactly one card is refreshable -> exactly one contract stamp (never a grid-level one).
		var h = html(grid());
		assertEquals(1, countOf(h, "data-juneau-card-contract="), h);
	}

	@Test void a08_staticCardHasNoRefreshWire() {
		var g = CardGrid.create("g1").cards(Card.create("c1", "Static").body(staticBody()));
		var h = html(g);
		assertFalse(h.contains("data-juneau-card-refresh"), h);
		assertFalse(h.contains("data-juneau-card-contract"), h);
		assertFalse(h.contains("data-juneau-card-poll-ms"), h);
		assertFalse(h.contains("data-juneau-card-refresh-trigger"), h);
	}

	@Test void a09_pollMsPresentAndJavaClamped() {
		// pollIntervalMs(1000) is clamped to the commons floor (5000) in CardFieldList.validate().
		var h = html(grid());
		assertTrue(h.contains("data-juneau-card-poll-ms=\"5000\""), h);
		assertFalse(h.contains("data-juneau-card-poll-ms=\"1000\""), h);
	}

	@Test void a10_pollMsOmittedWhenNoPoll() {
		var g = CardGrid.create("g1").cards(Card.create("c1", "Live").body(refreshBody()));
		var h = html(g);
		assertTrue(h.contains("data-juneau-card-refresh"), h);
		assertFalse(h.contains("data-juneau-card-poll-ms"), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Field list body: columns, per-field wrappers, slots, initial value
	//------------------------------------------------------------------------------------------------------------------

	@Test void a11_columnsStampedInlineAndPerFieldWrappers() {
		var h = html(grid());
		assertTrue(h.contains("grid-template-columns:repeat(3,minmax(0,1fr))"), h);
		assertTrue(h.contains("class=\"juneau-view-card-fields\""), h);
		assertTrue(h.contains("class=\"juneau-view-card-field\""), h);
	}

	@Test void a12_fieldSlotsCarryDataKeyAndServerValue() {
		var h = html(grid());
		assertTrue(h.contains("data-juneau-card-field=\"name\""), h);
		assertTrue(h.contains("data-juneau-card-field=\"status\""), h);
		assertTrue(h.contains(">Widget A<"), h);   // initial value painted server-side
		assertTrue(h.contains(">Name<"), h);        // label painted server-side
	}

	@Test void a13_bodyAndBannerHosts() {
		var h = html(grid());
		assertTrue(h.contains("data-juneau-card-body"), h);
		assertTrue(h.contains("data-juneau-card-banner"), h);
		assertTrue(h.contains("role=\"alert\""), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Escaping (security-critical): human strings + server value are entity-escaped, never live tags
	//------------------------------------------------------------------------------------------------------------------

	@Test void a14_titleIsEscaped() {
		var g = CardGrid.create("g1").cards(
			Card.create("c1", "<script>alert(1)</script>").body(staticBody()));
		var h = html(g);
		assertFalse(h.contains("<script>alert(1)"), "card title must not become a live tag:\n" + h);
		assertTrue(h.contains("&lt;script&gt;"), h);
	}

	@Test void a15_serverValueIsEscaped() {
		var g = CardGrid.create("g1").cards(
			Card.create("c1", "T").body(CardFieldList.create().fields(
				CardField.of("k", "L", "<img src=x onerror=alert(1)>"))));
		var h = html(g);
		assertFalse(h.contains("<img src=x"), "server value must not become a live tag:\n" + h);
		assertTrue(h.contains("&lt;img"), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Closed v1 dispatch: unknown CardBody fails closed at emit
	//------------------------------------------------------------------------------------------------------------------

	/** A well-formed CardBody the v1 emitter does not know how to render. */
	static class OtherBody implements CardBody {
		@Override public void validate() {}
	}

	@Test void a16_unknownBodyFailsClosed() {
		var g = CardGrid.create("g1").cards(Card.create("c1", "T").body(new OtherBody()));
		var e = assertThrows(IllegalArgumentException.class, () -> CardGridTable.of(g));
		assertTrue(e.getMessage().contains("CardFieldList"), e::getMessage);
	}

	@Test void a17_nullGridRejected() {
		assertThrows(IllegalArgumentException.class, () -> CardGridTable.of(null));
	}

	@Test void a18_invalidGridRejectedOnEntry() {
		// grid.validate() runs on entry: a grid with no cards must throw before any markup is produced.
		assertThrows(IllegalArgumentException.class, () -> CardGridTable.of(CardGrid.create("g1")));
	}

	//------------------------------------------------------------------------------------------------------------------
	// No CardContent this child (i2)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a19_noCardContentEmit() {
		assertFalse(html(grid()).contains("card-content"), "no CardContent markup this child (i2)");
	}

	private static int countOf(String haystack, String needle) {
		var n = 0;
		for (var i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length()))
			n++;
		return n;
	}
}
