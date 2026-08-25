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

import java.lang.reflect.*;

import jakarta.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * A {@link ViewCardBody} card body: a whole view table hosted inside a card, plus the request-aware emit surface that
 * makes it possible and the per-card action catalog that ships alongside it.
 *
 * <h5 class='section'>What is pinned here</h5>
 * <ul>
 * 	<li>The new body type is emitted as a real {@link ViewTable} shell, and it brings the table's <b>own</b> data path
 * 		&mdash; it never borrows the {@link CardFieldList} refresh envelope, so a card hosting one carries no
 * 		{@code data-juneau-card-refresh} / {@code data-juneau-card-poll-ms} wire.
 * 	<li>The emit surface is deliberately narrow: a request-aware <b>card</b> entry point, a package-private
 * 		grid-qualified one, and no request-aware grid entry point at all.  The pre-existing grid overload stays
 * 		request-free and therefore fails closed on a body that needs a request.
 * 	<li>Table identity inside a card is qualified by the enclosing card (and grid, when known), so two cards hosting
 * 		the same authored view cannot collide on a document-wide element id.  The {@code data-juneau-view} marker
 * 		stays the author's own id.
 * </ul>
 */
class CardGridTable_ViewCardBody_Test extends TestBase {

	private static final String TOKEN = "tok-123";

	private static ViewDef view() {
		return view("orders");
	}

	private static ViewDef view(String id) {
		return ViewDef.create(id)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/" + id)
			.columns(Column.of("ref").title("Ref"), Column.of("total").title("Total"))
			.build();
	}

	/** A view that emits fine but cannot pass its own {@link ViewDef#validate()}: a pill bound to an undeclared row action. */
	private static ViewDef brokenView() {
		return ViewDef.create("broken")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/broken")
			.columns(Column.of("state").title("State").render(Render.pill().meta("action", "nope")))
			.build();
	}

	private static MockServletRequest tokenRequest() {
		return MockServletRequest.create().attribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, TOKEN);
	}

	private static Card viewCard(String cardId, ViewDef v) {
		return Card.create(cardId, "Orders").body(ViewCardBody.of(v));
	}

	private static CardFieldList fieldBody() {
		return CardFieldList.create().fields(CardField.of("k", "L", "v"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The body renders a table shell inside the card
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_viewCardBodyRendersTableShellInsideCardBody() {
		var h = Html.of(CardGridTable.of(tokenRequest(), viewCard("c1", view())));
		assertTrue(h.contains("data-juneau-card-body"), h);
		assertTrue(h.contains("<table"), h);
		assertTrue(h.contains("data-juneau-view=\"orders\""), h);
		// The table is inside the card's body wrapper, not a sibling of it.
		var bodyAt = h.indexOf("data-juneau-card-body");
		var tableAt = h.indexOf("<table");
		assertTrue(bodyAt >= 0 && tableAt > bodyAt, "table must render inside the card body wrapper:\n" + h);
	}

	@Test void a02_theViewsOwnSidecarAndColumnsComeAlong() {
		var h = Html.of(CardGridTable.of(tokenRequest(), viewCard("c1", view())));
		assertTrue(h.contains("application/json"), h);
		assertTrue(h.contains(">Ref<") && h.contains(">Total<"), h);
	}

	@Test void a03_theRequestReachesTheTable_soCsrfIsEmbedded() {
		// The whole point of routing through a request-aware card overload: ViewTable.of(req, ...) receives the
		// request, so the row-action token is auto-embedded instead of silently absent.
		var h = Html.of(CardGridTable.of(tokenRequest(), viewCard("c1", view())));
		assertTrue(h.contains(ViewTable.CSRF_ATTR + "=\"" + TOKEN + "\""), h);
	}

	@Test void a04_cardHostingATableCarriesNoRefreshWire() {
		// The new body brings its own data path (the table's ajax/refresh); it must not reuse the card refresh
		// envelope, so none of the CardFieldList refresh attributes may appear.
		var h = Html.of(CardGridTable.of(tokenRequest(), viewCard("c1", view())));
		assertFalse(h.contains(CardGridTable.CARD_REFRESH_ATTR), h);
		assertFalse(h.contains(CardGridTable.CARD_POLL_ATTR), h);
		assertFalse(h.contains(CardGridTable.CARD_CONTRACT_ATTR), h);
		assertFalse(h.contains(CardGridTable.CARD_REFRESH_TRIGGER_ATTR), h);
		assertFalse(h.contains(CardGridTable.CARD_STATUS_ATTR), h);
	}

	@Test void a05_cardChromeIsStillEmitted() {
		var h = Html.of(CardGridTable.of(tokenRequest(), viewCard("c1", view())));
		assertTrue(h.contains(CardGridTable.CARD_MARKER), h);
		assertTrue(h.contains("data-juneau-card-id=\"c1\""), h);
		assertTrue(h.contains(CardGridTable.CARD_BANNER_ATTR), h);
		assertTrue(h.contains(">Orders<"), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) ViewCardBody bean contract: validate() cascades, and there is no refresh wire to carry
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_contractVersion() {
		assertEquals("1", ViewCardBody.CONTRACT_VERSION);
	}

	@Test void b02_validateCascadesToViewDefValidate() {
		// A ViewDef that cannot stand on its own must be rejected through the body, not at emit time.
		var bad = brokenView();
		var body = ViewCardBody.of(bad);
		var direct = assertThrows(IllegalArgumentException.class, bad::validate);
		var viaBody = assertThrows(IllegalArgumentException.class, body::validate);
		assertEquals(direct.getMessage(), viaBody.getMessage(), "the body must surface the view's own complaint");
	}

	@Test void b03_nullViewRejected() {
		var e = assertThrows(IllegalArgumentException.class, () -> ViewCardBody.of(null).validate());
		assertTrue(e.getMessage().contains("view"), e::getMessage);
	}

	@Test void b04_validateReachesTheBodyThroughTheCard() {
		var c = Card.create("c1", "T").body(ViewCardBody.of(brokenView()));
		assertThrows(IllegalArgumentException.class, c::validate);
	}

	@Test void b05_noRefreshEnvelopeFieldsExistOnThisBody() {
		// The refresh envelope belongs to CardFieldList alone.  A field of any of these names here would let a
		// caller declare a card-level refresh wire the emitter would then have to decide what to do with.
		var declared = new java.util.ArrayList<String>();
		for (var f : ViewCardBody.class.getDeclaredFields())
			declared.add(f.getName());
		for (var forbidden : new String[]{"refreshEndpoint", "pollIntervalMs", "refresh", "poll"})
			assertFalse(declared.contains(forbidden),
				() -> "ViewCardBody must not carry the CardFieldList refresh envelope; found: " + forbidden);
		for (var m : ViewCardBody.class.getMethods())
			assertFalse(m.getName().equals("refresh") || m.getName().equals("pollIntervalMs"),
				() -> "ViewCardBody must expose no refresh-envelope setter; found: " + m.getName());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Emit-surface shape: one request-aware card entry point, no request-aware grid entry point
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_publicRequestAwareCardOverloadExists_andOmitsGridId() throws Exception {
		var m = CardGridTable.class.getDeclaredMethod("of", HttpServletRequest.class, Card.class);
		assertTrue(Modifier.isPublic(m.getModifiers()), "of(HttpServletRequest, Card) must be public");
		assertTrue(Modifier.isStatic(m.getModifiers()));
		assertEquals(2, m.getParameterCount(), "the public request-aware overload takes no grid id");
	}

	@Test void c02_gridQualifiedOverloadIsPackagePrivate() throws Exception {
		var m = CardGridTable.class.getDeclaredMethod("of", HttpServletRequest.class, Card.class, String.class);
		var mods = m.getModifiers();
		assertFalse(Modifier.isPublic(mods), "the grid-qualified overload must not be public API");
		assertFalse(Modifier.isProtected(mods), "the grid-qualified overload must not be protected API");
		assertFalse(Modifier.isPrivate(mods), "the grid-qualified overload must be reachable from the grid assembler");
		assertTrue(Modifier.isStatic(mods));
	}

	@Test void c03_thereIsNoRequestAwareGridOverload() {
		assertThrows(NoSuchMethodException.class,
			() -> CardGridTable.class.getDeclaredMethod("of", HttpServletRequest.class, CardGrid.class));
		// Belt to those braces: no public method of ANY name takes (request, grid).
		for (var m : CardGridTable.class.getMethods()) {
			var p = m.getParameterTypes();
			var offends = p.length == 2 && HttpServletRequest.class.isAssignableFrom(p[0]) && p[1] == CardGrid.class;
			assertFalse(offends, () -> "no public (request, CardGrid) entry point may exist; found: " + m);
		}
	}

	@Test void c04_theGridOverloadStaysRequestFree() throws Exception {
		var m = CardGridTable.class.getDeclaredMethod("of", CardGrid.class);
		assertTrue(Modifier.isPublic(m.getModifiers()));
		// It embeds no token, because it has no request to read one from.
		var g = CardGrid.create("g1").cards(Card.create("c1", "T").body(fieldBody()));
		assertFalse(Html.of(CardGridTable.of(g)).contains(ViewTable.CSRF_ATTR), "the grid overload embeds no token");
	}

	@Test void c05_viewCardBodyOnTheRequestFreeGridPathFailsClosed() {
		var g = CardGrid.create("g1").cards(viewCard("c1", view()));
		var e = assertThrows(IllegalArgumentException.class, () -> CardGridTable.of(g));
		assertTrue(e.getMessage().contains("ViewCardBody"), e::getMessage);
		assertTrue(e.getMessage().contains("request"), e::getMessage);
	}

	@Test void c06_nullArgumentsRejected() {
		assertThrows(IllegalArgumentException.class, () -> CardGridTable.of(tokenRequest(), (Card)null));
		assertThrows(IllegalArgumentException.class, () -> CardGridTable.of(null, viewCard("c1", view())));
	}

	@Test void c07_theCardOverloadValidatesOnEntry() {
		var c = Card.create("  ", "T").body(fieldBody());
		assertThrows(IllegalArgumentException.class, () -> CardGridTable.of(tokenRequest(), c));
	}

	@Test void c08_theCardOverloadAlsoEmitsAFieldListBody() {
		// The request-aware entry point is not ViewCardBody-only: an ordinary field list still renders through it,
		// refresh wire and all.
		var c = Card.create("c1", "Live").body(fieldBody().refresh("/data/summary"));
		var h = Html.of(CardGridTable.of(tokenRequest(), c));
		assertTrue(h.contains(CardGridTable.CARD_REFRESH_ATTR + "=\"/data/summary\""), h);
		assertTrue(h.contains(CardGridTable.CARD_CONTRACT_ATTR + "=\"" + CardFieldList.CONTRACT_VERSION + "\""), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Card-scoped table identity (the marker attribute stays the author's id)
	//------------------------------------------------------------------------------------------------------------------

	/** The emitted {@code <table>}'s opening tag. */
	private static String tableTag(String html) {
		var at = html.indexOf("<table");
		assertTrue(at >= 0, html);
		return html.substring(at, html.indexOf('>', at));
	}

	@Test void d01_publicCardOverload_mintsCardQualifiedIds_withoutAGridId() {
		var h = Html.of(CardGridTable.of(tokenRequest(), viewCard("c1", view())));
		assertTrue(tableTag(h).contains("id=\"c1:orders\""), tableTag(h));
		assertTrue(h.contains("id=\"" + ViewTable.SIDECAR_ID_PREFIX + "c1:orders\""), h);
	}

	@Test void d02_gridQualifiedOverload_mintsGridAndCardQualifiedIds() throws Exception {
		var h = Html.of(invokeGridQualified(tokenRequest(), viewCard("c1", view()), "g1"));
		assertTrue(tableTag(h).contains("id=\"g1:c1:orders\""), tableTag(h));
		assertTrue(h.contains("id=\"" + ViewTable.SIDECAR_ID_PREFIX + "g1:c1:orders\""), h);
	}

	@Test void d03_markerAttributeStaysTheAuthorId() throws Exception {
		// The runtime's authored-identity marker must not become the minted DOM id, or every author-keyed lookup
		// (and the sidecar's own contents) would disagree with the page.
		var h = Html.of(invokeGridQualified(tokenRequest(), viewCard("c1", view()), "g1"));
		assertTrue(h.contains(ViewTable.MARKER_ATTR + "=\"orders\""), h);
		assertFalse(h.contains(ViewTable.MARKER_ATTR + "=\"g1:c1:orders\""), h);
	}

	@Test void d04_twoCardsSharingAViewIdDoNotCollide() throws Exception {
		// The whole reason identity is card-qualified: the same authored view in two cards of one grid.
		var a = Html.of(invokeGridQualified(tokenRequest(), viewCard("left", view()), "g1"));
		var b = Html.of(invokeGridQualified(tokenRequest(), viewCard("right", view()), "g1"));
		assertTrue(a.contains("id=\"" + ViewTable.SIDECAR_ID_PREFIX + "g1:left:orders\""), a);
		assertTrue(b.contains("id=\"" + ViewTable.SIDECAR_ID_PREFIX + "g1:right:orders\""), b);
		assertNotEquals(tableTag(a), tableTag(b), "two cards must not mint the same table id");
	}

	@Test void d05_aTableOutsideACardMintsExactlyWhatItAlwaysDid() {
		// The regression guard on the qualifier being additive: with no qualifier the emitted identity is the
		// author's id, byte-for-byte as before.
		var h = Html.of(ViewTable.of(tokenRequest(), view()));
		assertTrue(h.contains("id=\"orders\""), h);
		assertTrue(h.contains("id=\"" + ViewTable.SIDECAR_ID_PREFIX + "orders\""), h);
		assertTrue(h.contains(ViewTable.MARKER_ATTR + "=\"orders\""), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Unknown bodies still fail closed (including a raw-content body, which this child does not ship)
	//------------------------------------------------------------------------------------------------------------------

	/** A raw-markup body shape.  There is no sanitizer in the toolkit, so the emitter must refuse it. */
	static class CardContent implements CardBody {
		@SuppressWarnings({
			"unused"   // The field exists to make this stand-in look exactly like the body type it stands in for.
		})
		String html = "<b>hi</b>";

		@Override public void validate() {}
	}

	@Test void e01_rawContentBodyFailsClosedOnTheRequestAwarePath() {
		var c = Card.create("c1", "T").body(new CardContent());
		var e = assertThrows(IllegalArgumentException.class, () -> CardGridTable.of(tokenRequest(), c));
		assertTrue(e.getMessage().contains("CardFieldList"), e::getMessage);
		assertTrue(e.getMessage().contains(CardContent.class.getName()), e::getMessage);
	}

	@Test void e02_rawContentBodyFailsClosedOnTheGridPathToo() {
		var g = CardGrid.create("g1").cards(Card.create("c1", "T").body(new CardContent()));
		var e = assertThrows(IllegalArgumentException.class, () -> CardGridTable.of(g));
		assertTrue(e.getMessage().contains("CardFieldList"), e::getMessage);
	}

	@Test void e03_noRawContentMarkupIsEverEmitted() {
		var h = Html.of(CardGridTable.of(tokenRequest(), viewCard("c1", view())));
		assertFalse(h.contains("card-content"), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Contract versioning: the refresh handshake is untouched
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_theRefreshHandshakeIsNotBumped() {
		// data-juneau-card-contract is stamped with CardFieldList.CONTRACT_VERSION and compared against the baked
		// literal in juneau-cards.js.  Adding a body type and an action catalog must not move it.
		assertEquals("1", CardFieldList.CONTRACT_VERSION);
		assertEquals(CardFieldList.CONTRACT_VERSION, ViewsMixin.CARDS_CONTRACT_VERSION);
	}

	@Test void f02_theNewBodyIsVersionedIndependently() {
		assertEquals("1", ViewCardBody.CONTRACT_VERSION);
		// The table runtime versions on its own axis, and hosting a table in a card moves neither it nor the grid.
		assertEquals("4", ViewsMixin.CONTRACT_VERSION);
		assertEquals("1", CardGrid.CONTRACT_VERSION);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) Per-card actions
	//------------------------------------------------------------------------------------------------------------------

	private static Card actionCard() {
		return Card.create("c1", "Orders").body(fieldBody()).actions(
			HeaderAction.link("open", "external", "Open full report", "/reports/1"),
			HeaderAction.safe("export", "download", "Export as CSV", "card-export"),
			HeaderAction.menu("more", "overflow", "More actions")
				.menu(MenuItem.safe("pin", "Pin card", "card-pin"), MenuItem.link("docs", "Docs", "/docs")));
	}

	@Test void g01_actionsRenderIntoTheCardActionRow() {
		var h = Html.of(CardGridTable.of(tokenRequest(), actionCard()));
		assertTrue(h.contains("juneau-view-card-actions"), h);
		assertTrue(h.contains(AppHeaderTable.ACTION_MARKER + "=\"open\""), h);
		assertTrue(h.contains(AppHeaderTable.ACTION_MARKER + "=\"export\""), h);
		assertTrue(h.contains(AppHeaderTable.ACTION_MARKER + "=\"more\""), h);
	}

	@Test void g02_behaviorsUseTheSharedHeaderVocabulary() {
		var h = Html.of(CardGridTable.of(tokenRequest(), actionCard()));
		assertTrue(h.contains(AppHeaderTable.BEHAVIOR_ATTR + "=\"link\""), h);
		assertTrue(h.contains(AppHeaderTable.BEHAVIOR_ATTR + "=\"safe\""), h);
		assertTrue(h.contains(AppHeaderTable.BEHAVIOR_ATTR + "=\"menu\""), h);
		assertTrue(h.contains(AppHeaderTable.SAFE_ATTR + "=\"card-export\""), h);
		assertTrue(h.contains(AppHeaderTable.ICON_ATTR + "=\"external\""), h);
	}

	@Test void g03_tooltipIsTheAccessibleName() {
		var h = Html.of(CardGridTable.of(tokenRequest(), actionCard()));
		assertTrue(h.contains("aria-label=\"Open full report\""), h);
		assertTrue(h.contains("title=\"Open full report\""), h);
	}

	@Test void g04_menuListIsCardScoped_andWiredByAriaControls() {
		var h = Html.of(invokeGridQualified(tokenRequest(), actionCard(), "g1"));
		var menuId = AppHeaderTable.MENU_ID_PREFIX + "g1:c1:more";
		assertTrue(h.contains("id=\"" + menuId + "\""), h);
		assertTrue(h.contains("aria-controls=\"" + menuId + "\""), h);
		assertTrue(h.contains("aria-haspopup=\"menu\""), h);
		assertTrue(h.contains("aria-expanded=\"false\""), h);
		assertTrue(h.contains("jc-menu-item"), h);
		assertTrue(h.contains(">Pin card<"), h);
	}

	@Test void g05_menuIdOmitsTheGridIdOnThePublicOverload() {
		var h = Html.of(CardGridTable.of(tokenRequest(), actionCard()));
		assertTrue(h.contains("id=\"" + AppHeaderTable.MENU_ID_PREFIX + "c1:more\""), h);
	}

	@Test void g06_twoCardsWithTheSameActionIdDoNotShareAMenuId() throws Exception {
		var a = Html.of(invokeGridQualified(tokenRequest(), actionCard(), "g1"));
		var b = Html.of(invokeGridQualified(tokenRequest(), actionCard().body(fieldBody()), "g2"));
		assertTrue(a.contains(AppHeaderTable.MENU_ID_PREFIX + "g1:c1:more"), a);
		assertTrue(b.contains(AppHeaderTable.MENU_ID_PREFIX + "g2:c1:more"), b);
	}

	@Test void g07_actionTooltipStaysInAttributePositionAndNeverBecomesMarkup() {
		// A tooltip is emitted the one way the header vocabulary emits it: as attribute values, never as a child.
		// So a tag-shaped tooltip cannot open an element, because it is not in element-content position at all.
		var c = Card.create("c1", "T").body(fieldBody()).actions(
			HeaderAction.link("open", "external", "<b>x</b>", "/reports/1"));
		var h = Html.of(CardGridTable.of(tokenRequest(), c));
		assertTrue(h.contains("aria-label=\"<b>x</b>\""), h);
		assertTrue(h.contains("title=\"<b>x</b>\""), h);
		assertFalse(h.contains("><b>x</b><"), "a tooltip must never reach element-content position:\n" + h);
	}

	@Test void g07b_menuItemLabelsAreEscapedAsText() {
		// Labels are text children, so they get the same entity-escaping the card title already gets.
		var c = Card.create("c1", "T").body(fieldBody()).actions(
			HeaderAction.menu("more", "overflow", "More").menu(MenuItem.safe("pin", "<script>alert(1)</script>", "card-pin")));
		var h = Html.of(CardGridTable.of(tokenRequest(), c));
		assertFalse(h.contains("<script>alert(1)"), "a menu label must not become a live tag:\n" + h);
		assertTrue(h.contains("&lt;script&gt;"), h);
	}

	@Test void g08_aCardWithNoActionsIsUnchanged() {
		var h = Html.of(CardGridTable.of(tokenRequest(), Card.create("c1", "T").body(fieldBody())));
		assertFalse(h.contains(AppHeaderTable.ACTION_MARKER), h);
		assertFalse(h.contains(AppHeaderTable.BEHAVIOR_ATTR), h);
	}

	@Test void g09_actionsCoexistWithTheBuiltInRefreshButton() {
		var c = Card.create("c1", "Live").body(fieldBody().refresh("/data/summary")).actions(
			HeaderAction.link("open", "external", "Open", "/reports/1"));
		var h = Html.of(CardGridTable.of(tokenRequest(), c));
		assertTrue(h.contains(CardGridTable.CARD_REFRESH_TRIGGER_ATTR), h);
		assertTrue(h.contains(AppHeaderTable.ACTION_MARKER + "=\"open\""), h);
	}

	@Test void g10_actionsAlsoRenderOnTheRequestFreeGridPath() {
		// Actions need no request; only a ViewCardBody does.  A static card with actions still emits on of(grid).
		var g = CardGrid.create("g1").cards(actionCard());
		var h = Html.of(CardGridTable.of(g));
		assertTrue(h.contains(AppHeaderTable.ACTION_MARKER + "=\"open\""), h);
		assertTrue(h.contains("id=\"" + AppHeaderTable.MENU_ID_PREFIX + "g1:c1:more\""), h);
	}

	@Test void g11_actionsAreValidatedBeforeEmit() {
		var c = Card.create("c1", "T").body(fieldBody()).actions(
			HeaderAction.link("open", "external", "  ", "/reports/1"));
		assertThrows(IllegalArgumentException.class, () -> CardGridTable.of(tokenRequest(), c));
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) The package-private grid assembler routes every card through the grid-qualified overload
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_requestAwareGridAssembly_qualifiesEveryCardAndKeepsTheGridChrome() {
		var g = CardGrid.create("g1").title("Dashboard").cards(
			viewCard("left", view("orders")),
			viewCard("right", view("orders")));
		var h = Html.of(CardGridTable.ofGrid(tokenRequest(), g));
		assertTrue(h.contains(CardGridTable.GRID_MARKER), h);
		assertTrue(h.contains(CardGridTable.GRID_ID_ATTR + "=\"g1\""), h);
		assertTrue(h.contains(">Dashboard<"), h);
		assertTrue(h.contains("id=\"" + ViewTable.SIDECAR_ID_PREFIX + "g1:left:orders\""), h);
		assertTrue(h.contains("id=\"" + ViewTable.SIDECAR_ID_PREFIX + "g1:right:orders\""), h);
	}

	@Test void h02_theGridAssemblerIsNotPublicApi() throws Exception {
		var m = CardGridTable.class.getDeclaredMethod("ofGrid", HttpServletRequest.class, CardGrid.class);
		assertFalse(Modifier.isPublic(m.getModifiers()), "request-aware grid assembly stays package-private");
	}

	/** Invokes the package-private grid-qualified overload (same package, so a direct call is enough). */
	private static Object invokeGridQualified(HttpServletRequest req, Card card, String gridId) {
		return CardGridTable.of(req, card, gridId);
	}
}
