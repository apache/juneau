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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

/**
 * {@link PageSpec} cards: {@code table}, {@code html}, {@code card}, {@code csrf}, {@code bodyAttr}, and seeding the
 * built-card pool.
 *
 * @since 10.0.0
 */
class PageSpec_Cards_Test extends TestBase {

	private static final CardDirectiveModel MODEL = new CardDirectiveModel(
		CardRequirements.create().build(new ToolkitPackRegistry()), org.apache.juneau.rest.server.console.CardTypeRegistry.standard());

	private static TableSpec table(String id) {
		return TableSpec.create(id).dataUrl("/rest/" + id + "/data").columns(Column.create("pod"));
	}

	@Test void a01_table_addsDatatablesCard() {
		var spec = PageSpec.create().table(table("slo"));
		assertEquals(1, spec.cards().size());
		assertEquals("datatables", spec.card("slo").orElseThrow().type());
		assertNull(spec.card("slo").orElseThrow().toMap().get("title"));
	}

	@Test void a02_table_withTitle() {
		var spec = PageSpec.create().table(table("slo"), "SLO");
		assertEquals("SLO", spec.card("slo").orElseThrow().toMap().get("title"));
	}

	@Test void a03_table_consumerCanOverrideCssClass() {
		var spec = PageSpec.create().table(table("slo").cssClass("a b"), c -> c.cssClass("c"));
		assertEquals("c", spec.card("slo").orElseThrow().toMap().get("class"));
	}

	@Test void a04_table_carriesTableCssClass() {
		var spec = PageSpec.create().table(table("slo").cssClass("a b"));
		assertEquals("a b", spec.card("slo").orElseThrow().toMap().get("class"));
	}

	@Test void a05_table_validatesImmediately() {
		var t = TableSpec.create("slo").columns(Column.create("pod"));
		var ex = assertThrows(IllegalStateException.class, () -> PageSpec.create().table(t));
		assertEquals("TableSpec 'slo' requires dataUrl.", ex.getMessage());
	}

	@Test void a06_table_snapshotsBodyAtCallTime() {
		var t = table("slo");
		var spec = PageSpec.create().table(t);
		var before = Json.of(spec.tableBodies.get("slo"));
		assertTrue(before.contains("pod"));
		t.columns(Column.create("other"));
		assertEquals(before, Json.of(spec.tableBodies.get("slo")));
		assertFalse(Json.of(spec.tableBodies.get("slo")).contains("other"));
	}

	@Test void a07_table_duplicateId_rejected() {
		var spec = PageSpec.create().table(table("slo"));
		var ex = assertThrows(IllegalArgumentException.class, () -> spec.table(table("slo")));
		assertEquals("PageSpec card id 'slo' is already declared.", ex.getMessage());
	}

	@Test void a08_html_basic() {
		var spec = PageSpec.create().html("intro", "<p>hi</p>");
		assertEquals("html", spec.card("intro").orElseThrow().type());
		assertEquals("<p>hi</p>", spec.cardMarkup.get("intro"));
	}

	@Test void a09_html_withTitle() {
		var spec = PageSpec.create().html("intro", "Intro", "<p>hi</p>");
		assertEquals("Intro", spec.card("intro").orElseThrow().toMap().get("title"));
	}

	@Test void a10_html_consumerSetsCssClass() {
		var spec = PageSpec.create().html("intro", "<p>hi</p>", c -> c.cssClass("wide"));
		assertEquals("wide", spec.card("intro").orElseThrow().toMap().get("class"));
	}

	@Test void a11_html_nullMarkup_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().html("intro", null));
		assertEquals("PageSpec.html id 'intro' requires a non-null markup.", ex.getMessage());
	}

	@Test void a12_html_consumerSettingSrc_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().html("intro", "<p/>", c -> c.src("/x")));
		assertEquals("PageSpec.html id 'intro' sets a markup body; its Consumer<CardSpec> must not also set src or template.", ex.getMessage());
	}

	@Test void a13_html_consumerSettingTemplate_rejected() {
		assertThrows(IllegalArgumentException.class, () -> PageSpec.create().html("intro", "<p/>", c -> c.template("t")));
	}

	@Test void a14_card_registeredCustomType_accepted() {
		var spec = PageSpec.create().card(CardSpec.of("console-output", "out").title("Output"));
		assertEquals("console-output", spec.card("out").orElseThrow().type());
	}

	@Test void a15_card_unregisteredNonReservedType_accepted() {
		var spec = PageSpec.create().card(CardSpec.of("my-widget", "w"), "<b>x</b>");
		assertEquals("<b>x</b>", spec.cardMarkup.get("w"));
	}

	@Test void a16_card_removedType_rejected() {
		for (var type : List.of("js", "json", "calendar")) {
			var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().card(CardSpec.of(type, "x")));
			assertEquals("PageSpec.card type '" + type + "' was removed in 10.0.0; use PageSpec.html(...) with a markup/template, or a registered card type.", ex.getMessage());
		}
	}

	@Test void a17_card_dedicatedMethodTypes_rejected() {
		var h = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().card(CardSpec.html("x")));
		assertEquals("PageSpec.card type 'html' has a dedicated method; use PageSpec.html(...).", h.getMessage());
		var d = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().card(CardSpec.of("datatables", "x")));
		assertEquals("PageSpec.card type 'datatables' has a dedicated method; use PageSpec.table(...).", d.getMessage());
	}

	@Test void a18_card_reservedWithoutHandler_rejected() {
		for (var type : List.of("chart", "run-view")) {
			var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().card(CardSpec.of(type, "x")));
			assertEquals("PageSpec.card type '" + type + "' is reserved and has no card-type handler; it cannot be built with PageSpec.", ex.getMessage());
		}
	}

	@Test void a19_cards_isSnapshot() {
		var spec = PageSpec.create().html("a", "<p/>");
		var snap = spec.cards();
		spec.html("b", "<p/>");
		assertEquals(1, snap.size());
		assertEquals(2, spec.cards().size());
		assertTrue(spec.card("nope").isEmpty());
	}

	@Test void a20_csrf_setsBothAttrs() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().csrf("t\"1", "X-CSRF").applyTo(cap, null);
		assertEquals("data-juneau-csrf=\"t&quot;1\" data-juneau-csrf-header=\"X-CSRF\"", cap.bodyAttrs);
	}

	@Test void a21_csrf_blankHeaderName_omitsHeaderAttr() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().csrf("tok", " ").applyTo(cap, null);
		assertEquals("data-juneau-csrf=\"tok\"", cap.bodyAttrs);
	}

	@Test void a22_bodyAttr_escapesAndLastWins() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().bodyAttr("data-a", "1").bodyAttr("data-b", "<&>").bodyAttr("data-a", "2").applyTo(cap, null);
		assertEquals("data-a=\"2\" data-b=\"&lt;&amp;&gt;\"", cap.bodyAttrs);
	}

	@Test void a23_applyTo_noBodyAttrs_leavesCaptureUntouched() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().applyTo(cap, null);
		assertNull(cap.bodyAttrs);
	}

	@Test void a24_applyTo_seedsBuiltCardsWithMarkup() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().html("intro", "<p>hi</p>").applyTo(cap, MODEL);
		assertEquals(Map.of("intro", "<p>hi</p>"), cap.templates());
		assertEquals(List.of(), cap.cards());
		assertEquals("intro", cap.unplacedCards().get(0).id());
		assertEquals("intro", cap.unplacedCards().get(0).template());
	}

	@Test void a25_applyTo_duplicateOfAuthoredCard_rejected() throws Exception {
		var cap = new PageCapture();
		cap.addCard(CardSpec.html("intro"), null);
		var ex = assertThrows(freemarker.template.TemplateModelException.class,
			() -> PageSpec.create().html("intro", "<p/>").applyTo(cap, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='intro'> duplicates an existing card id."));
	}

	@Test void a26_addCard_afterBuiltCardWithSameId_rejected() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().html("intro", "<p/>").applyTo(cap, MODEL);
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> cap.addCard(CardSpec.html("intro"), null));
		assertTrue(ex.getMessage().contains("<@card id='intro'> duplicates an existing card id."));
	}

	@Test void a27_bodyAttr_badName_rejected() {
		for (var name : new String[]{null, "", "1a", "a b", "a=\"b", "a>"}) {
			var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().bodyAttr(name, "v"));
			assertEquals("PageSpec.bodyAttr name '" + name + "' must match [A-Za-z_:][-A-Za-z0-9_:.]*.", ex.getMessage());
		}
	}

	@Test void a28_csrf_nullToken_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().csrf(null, "X"));
		assertEquals("PageSpec.csrf requires a non-null token.", ex.getMessage());
	}

	@Test void a29_card_nullCardOrType_rejected() {
		assertEquals("PageSpec.card requires a non-null card.",
			assertThrows(IllegalArgumentException.class, () -> PageSpec.create().card((CardSpec)null)).getMessage());
		assertEquals("PageSpec.card id 'x' requires a non-blank type.",
			assertThrows(IllegalArgumentException.class, () -> PageSpec.create().card(CardSpec.of(null, "x"))).getMessage());
	}

	@Test void a30_table_consumerSettingSrcOrTemplate_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().table(table("slo"), c -> c.src("/x")));
		assertEquals("PageSpec.table id 'slo' sets src or template; the table supplies the card's data.", ex.getMessage());
		assertThrows(IllegalArgumentException.class, () -> PageSpec.create().table(table("slo"), c -> c.template("t")));
	}

	@Test void a31_copy_isIndependent() {
		var a = CardSpec.of("x", "i").title("T").cssClass("c").body("k", 1);
		var b = a.copy().template("t").title("U");
		assertNull(a.template());
		assertEquals("T", a.toMap().get("title"));
		assertEquals("U", b.toMap().get("title"));
		assertEquals(1, b.toMap().get("k"));
		assertEquals("c", b.cssClass());
	}

	@Test void a32_applyTo_tableCard_isLiftedThroughRegistry() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().table(table("slo").cssClass("wide"), "SLO").applyTo(cap, MODEL);
		var card = cap.unplacedCards().get(0).toMap();
		assertEquals("datatables", card.get("type"));
		assertEquals("SLO", card.get("title"));
		assertEquals("wide", card.get("class"));
		assertTrue(card.containsKey("table"), card.toString());
		assertTrue(Json.of(card).contains("/rest/slo/data"), card.toString());
	}

	@Test void a33_applyTo_recordsRequiredPacks() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().table(table("slo")).html("intro", "<p/>").applyTo(cap, MODEL);
		assertTrue(cap.requiredPacks().contains(ToolkitPackRegistry.PACK_DATATABLES_GLUE), cap.requiredPacks().toString());
	}

	@Test void a34_applyTo_doesNotMutateTheSpec_andCanRunTwice() throws Exception {
		var spec = PageSpec.create().html("intro", "<p/>").table(table("slo"));
		spec.applyTo(new PageCapture(), MODEL);
		assertNull(spec.card("intro").orElseThrow().template());
		assertNull(spec.card("slo").orElseThrow().toMap().get("table"));
		var cap = new PageCapture();
		spec.applyTo(cap, MODEL);
		assertEquals(2, cap.unplacedCards().size());
	}

	@Test void a35_applyTo_cardsWithoutCardModel_rejected() {
		var ex = assertThrows(freemarker.template.TemplateModelException.class,
			() -> PageSpec.create().html("intro", "<p/>").applyTo(new PageCapture(), null));
		assertTrue(ex.getMessage().contains("PageSpec cards need the console card directive"), ex.getMessage());
	}
}
