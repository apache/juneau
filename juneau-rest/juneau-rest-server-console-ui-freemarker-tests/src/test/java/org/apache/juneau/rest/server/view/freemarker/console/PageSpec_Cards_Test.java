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

import static org.apache.juneau.test.bct.BctAssertions.assertSize;
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

	/** A host registry with the adopter type {@code meter} on top of the standard handlers. */
	private static final CardDirectiveModel CUSTOM = new CardDirectiveModel(
		CardRequirements.create().build(new ToolkitPackRegistry()),
		org.apache.juneau.rest.server.console.CardTypeRegistry.standard().copy().add(new MeterCardType()).build());

	private static TableSpec table(String id) {
		return TableSpec.create(id).dataUrl("/rest/" + id + "/data").columns(Column.create("pod"));
	}

	@Test void a01_table_addsDatatablesCard() {
		var spec = PageSpec.create().table(table("slo"));
		assertSize(1, spec.cards());
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
		var target = PageSpec.create();
		var ex = assertThrows(IllegalStateException.class, () -> target.table(t));
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
		var arg = table("slo");
		var ex = assertThrows(IllegalArgumentException.class, () -> spec.table(arg));
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
		var target2 = PageSpec.create();
		var ex = assertThrows(IllegalArgumentException.class, () -> target2.html("intro", null));
		assertEquals("PageSpec.html id 'intro' requires a non-null markup.", ex.getMessage());
	}

	@Test void a12_html_consumerSettingSrc_rejected() {
		var target3 = PageSpec.create();
		var ex = assertThrows(IllegalArgumentException.class, () -> target3.html("intro", "<p/>", c -> c.src("/x")));
		assertEquals("PageSpec.html id 'intro' sets a markup body; its Consumer<CardSpec> must not also set src or template.", ex.getMessage());
	}

	@Test void a13_html_consumerSettingTemplate_rejected() {
		var target4 = PageSpec.create();
		assertThrows(IllegalArgumentException.class, () -> target4.html("intro", "<p/>", c -> c.template("t")));
	}

	@Test void a14_card_registeredCustomType_accepted() {
		var spec = PageSpec.create().card(CardSpec.of("terminal", "out").title("Output"));
		assertEquals("terminal", spec.card("out").orElseThrow().type());
	}

	@Test void a15_card_unregisteredNonReservedType_accepted() {
		var spec = PageSpec.create().card(CardSpec.of("my-widget", "w"), "<b>x</b>");
		assertEquals("<b>x</b>", spec.cardMarkup.get("w"));
	}

	@Test void a16_card_removedType_rejected() {
		for (var type : List.of("js", "json", "calendar")) {
			var target5 = PageSpec.create();
			var arg2 = CardSpec.of(type, "x");
			var ex = assertThrows(IllegalArgumentException.class, () -> target5.card(arg2));
			assertEquals("PageSpec.card type '" + type + "' was removed in 10.0.0; use PageSpec.html(...) with a markup/template, or a registered card type.", ex.getMessage());
		}
	}

	@Test void a17_card_dedicatedMethodTypes_rejected() {
		var target6 = PageSpec.create();
		var arg3 = CardSpec.html("x");
		var h = assertThrows(IllegalArgumentException.class, () -> target6.card(arg3));
		assertEquals("PageSpec.card type 'html' has a dedicated method; use PageSpec.html(...).", h.getMessage());
		var target7 = PageSpec.create();
		var arg4 = CardSpec.of("datatables", "x");
		var d = assertThrows(IllegalArgumentException.class, () -> target7.card(arg4));
		assertEquals("PageSpec.card type 'datatables' has a dedicated method; use PageSpec.table(...).", d.getMessage());
	}

	@Test void a18_card_reservedWithoutHandler_rejected() {
		for (var type : List.of("run-view")) {
			var target8 = PageSpec.create();
			var arg5 = CardSpec.of(type, "x");
			var ex = assertThrows(IllegalArgumentException.class, () -> target8.card(arg5));
			assertEquals("PageSpec.card type '" + type + "' is reserved and has no card-type handler; it cannot be built with PageSpec.", ex.getMessage());
		}
	}

	@Test void a19_cards_isSnapshot() {
		var spec = PageSpec.create().html("a", "<p/>");
		var snap = spec.cards();
		spec.html("b", "<p/>");
		assertSize(1, snap);
		assertSize(2, spec.cards());
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
		var target18 = PageSpec.create().html("intro", "<p/>");
		var ex = assertThrows(freemarker.template.TemplateModelException.class,
			() -> target18.applyTo(cap, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='intro'> duplicates an existing card id."));
	}

	@Test void a26_addCard_afterBuiltCardWithSameId_rejected() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().html("intro", "<p/>").applyTo(cap, MODEL);
		var arg10 = CardSpec.html("intro");
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> cap.addCard(arg10, null));
		assertTrue(ex.getMessage().contains("<@card id='intro'> duplicates an existing card id."));
	}

	@Test void a27_bodyAttr_badName_rejected() {
		for (var name : new String[]{null, "", "1a", "a b", "a=\"b", "a>"}) {
			var target9 = PageSpec.create();
			var ex = assertThrows(IllegalArgumentException.class, () -> target9.bodyAttr(name, "v"));
			assertEquals("PageSpec.bodyAttr name '" + name + "' must match [A-Za-z_:][-A-Za-z0-9_:.]*.", ex.getMessage());
		}
	}

	@Test void a28_csrf_nullToken_rejected() {
		var target10 = PageSpec.create();
		var ex = assertThrows(IllegalArgumentException.class, () -> target10.csrf(null, "X"));
		assertEquals("PageSpec.csrf requires a non-null token.", ex.getMessage());
	}

	@Test void a29_card_nullCardOrType_rejected() {
		var target11 = PageSpec.create();
		assertEquals("PageSpec.card requires a non-null card.",
			assertThrows(IllegalArgumentException.class, () -> target11.card((CardSpec)null)).getMessage());
		var target12 = PageSpec.create();
		var arg6 = CardSpec.of(null, "x");
		assertEquals("PageSpec.card id 'x' requires a non-blank type.",
			assertThrows(IllegalArgumentException.class, () -> target12.card(arg6)).getMessage());
	}

	@Test void a30_table_consumerSettingSrcOrTemplate_rejected() {
		var target13 = PageSpec.create();
		var arg7 = table("slo");
		var ex = assertThrows(IllegalArgumentException.class, () -> target13.table(arg7, c -> c.src("/x")));
		assertEquals("PageSpec.table id 'slo' sets src or template; the table supplies the card's data.", ex.getMessage());
		var target14 = PageSpec.create();
		var arg8 = table("slo");
		assertThrows(IllegalArgumentException.class, () -> target14.table(arg8, c -> c.template("t")));
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

	@Test void a32b_applyTo_tableCard_liftKeepsBare() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().table(table("slo"), c -> c.bare(true)).applyTo(cap, MODEL);
		var card = cap.unplacedCards().get(0).toMap();
		assertEquals("datatables", card.get("type"));
		assertEquals(true, card.get("bare"));
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
		assertSize(2, cap.unplacedCards());
	}

	@Test void a35_applyTo_cardsWithoutCardModel_rejected() {
		var target19 = PageSpec.create().html("intro", "<p/>");
		var arg11 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class,
			() -> target19.applyTo(arg11, null));
		assertTrue(ex.getMessage().contains("PageSpec cards need the console card directive"), ex.getMessage());
	}

	@Test void a36_applyTo_chartCard_liftKeepsBareAndWiring() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(ChartDef.line("trend").title("Orders").src("/s").x("day").series("placed", "Placed").toCardSpec()
			.cssClass("wide").bare(true)
			.visibleWhen(org.apache.juneau.rest.server.views.VisibilityRule.when("a").present())
			.subscribes(org.apache.juneau.rest.server.console.Subscription.to("ops.jobs").as("params").map("q", "search"))).applyTo(cap, MODEL);
		var card = cap.unplacedCards().get(0).toMap();
		assertEquals("chart", card.get("type"));
		assertEquals("Orders", card.get("title"));
		assertEquals("/s", card.get("src"));
		assertEquals("wide", card.get("class"));
		assertEquals("line", card.get("kind"));
		assertEquals(true, card.get("bare"));
		assertNotNull(card.get("visibleWhen"));
		assertNotNull(card.get("subscribes"));
	}

	@Test void a37_applyTo_invalidChartCard_rejectedByHandler() {
		var spec = PageSpec.create().card(CardSpec.of("chart", "trend").src("/s").body("kind", "line"));
		var arg12 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg12, MODEL));
		assertTrue(ex.getMessage().contains("type='chart' kind='line' requires x and at least one series."), ex.getMessage());
	}

	@Test void a38_applyTo_chartCardReservedBodyKey_rejected() {
		var spec = PageSpec.create().card(CardSpec.of("chart", "trend").src("/s").body("kind", "line").body("title", "x"));
		var arg13 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg13, MODEL));
		assertTrue(ex.getMessage().contains("reserved"), ex.getMessage());
	}

	@Test void a39_applyTo_chartCard_pullsChartsPack() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(ChartDef.line("trend").src("/s").x("day").series("placed", "Placed").toCardSpec()).applyTo(cap, MODEL);
		assertTrue(cap.requiredPacks().contains(ToolkitPackRegistry.PACK_CHARTS), cap.requiredPacks().toString());
	}

	@Test void a40_applyTo_chartCardWithTemplate_rejected() {
		var spec = PageSpec.create().card(ChartDef.line("trend").src("/s").x("day").series("placed", "Placed").toCardSpec().template("tpl"));
		var arg14 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg14, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='trend'> type='chart' takes a JSON5 body (plus src= for remote data); template= is not allowed."), ex.getMessage());
	}

	@Test void a41_applyTo_jobLogCard_isLiftedThroughHandler_andPullsJobLogPack() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("job-log", "rebuild").title("Rebuild")
			.body("submit", Map.of("endpoint", "/jobs/rebuild"))).applyTo(cap, MODEL);
		var card = cap.unplacedCards().get(0).toMap();
		assertEquals("job-log", card.get("type"));
		assertEquals("Rebuild", card.get("title"));
		assertEquals(Map.of("endpoint", "/jobs/rebuild"), card.get("submit"));
		assertTrue(cap.requiredPacks().contains(ToolkitPackRegistry.PACK_JOBLOG), cap.requiredPacks().toString());
	}

	@Test void a41b_applyTo_jobLogCard_liftKeepsBare() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("job-log", "rebuild")
			.body("submit", Map.of("endpoint", "/jobs/rebuild")).bare(true)).applyTo(cap, MODEL);
		var card = cap.unplacedCards().get(0).toMap();
		assertEquals("job-log", card.get("type"));
		assertEquals(true, card.get("bare"));
	}

	@Test void a41c_applyTo_terminalCard_bareRefused() {
		var spec = PageSpec.create().card(CardSpec.of("terminal", "out").body("terminal", Map.of("bytesUrl", "/t")));
		var base = CardSpec.of("terminal", "out");
		var ex = assertThrows(IllegalArgumentException.class, () -> base.bare(true));
		assertTrue(ex.getMessage().contains("a terminal card does not support bare"), ex.getMessage());
		assertDoesNotThrow(() -> spec.applyTo(new PageCapture(), MODEL));
	}

	@Test void a42_applyTo_jobLogCard_crossOriginEndpoint_rejected() {
		var arg15 = new PageCapture();
		var spec = PageSpec.create().card(CardSpec.of("job-log", "rebuild").body("submit", Map.of("endpoint", "//evil.test/x")));
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg15, MODEL));
		assertTrue(ex.getMessage().contains("type='job-log' submit.endpoint must be a same-origin path; got '//evil.test/x'."), ex.getMessage());
	}

	@Test void a43_applyTo_jobLogCardWithTemplate_rejected() {
		var spec = PageSpec.create().card(CardSpec.of("job-log", "rebuild").body("submit", Map.of("endpoint", "/a")).template("tpl"));
		var arg16 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg16, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='rebuild'> type='job-log' takes its options as the body; template= is not allowed."), ex.getMessage());
	}

	@Test void a44_applyTo_jobLogCardWithSrc_rejected() {
		var spec = PageSpec.create().card(CardSpec.of("job-log", "rebuild").src("/a").body("submit", Map.of("endpoint", "/a")));
		var arg17 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg17, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='rebuild'> type='job-log' takes its options as the body; src= is not allowed."), ex.getMessage());
	}

	@Test void a45_applyTo_unsafeCardSrc_rejectedForNonLiftedType() {
		var spec = PageSpec.create().card(CardSpec.of("my-widget", "w").src("\\\\evil.test/x"));
		var arg18 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg18, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='w'> type='my-widget' src must be a same-origin path"), ex::getMessage);
	}

	@Test void a46_addCard_unsafeSrc_rejected() {
		var target20 = new PageCapture();
		var arg19 = CardSpec.html("h").src("//evil.test/x");
		var ex = assertThrows(freemarker.template.TemplateModelException.class,
			() -> target20.addCard(arg19, null));
		assertTrue(ex.getMessage().contains("<@card id='h'> type='html' src must be a same-origin path"), ex::getMessage);
		assertDoesNotThrow(() -> new PageCapture().addCard(CardSpec.html("h").src("/ok/{id}"), null));
	}

	@Test void a47_htmlSrc_basic() {
		var spec = PageSpec.create().htmlSrc("hours", "Hours", "/console/store/fragments/hours");
		var card = spec.card("hours").orElseThrow();
		assertEquals("html", card.type());
		assertEquals("Hours", card.toMap().get("title"));
		assertEquals("/console/store/fragments/hours", card.toMap().get("src"));
		assertNull(spec.cardMarkup.get("hours"));
	}

	@Test void a48_htmlSrc_nullTitle_hasNoTitle() {
		var card = PageSpec.create().htmlSrc("hours", null, "/x").card("hours").orElseThrow();
		assertFalse(card.toMap().containsKey("title"));
		assertEquals("/x", card.toMap().get("src"));
	}

	@Test void a49_htmlSrc_applyTo_carriesSrcAndNoTemplate() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().htmlSrc("hours", "Hours", "/ok/{id}").applyTo(cap, MODEL);
		assertEquals(Map.of(), cap.templates());
		var card = cap.unplacedCards().get(0);
		assertEquals("hours", card.id());
		assertEquals("/ok/{id}", card.toMap().get("src"));
		assertNull(card.template());
	}

	@Test void a50_htmlSrc_unsafeSrc_refusedAtApplyTo() {
		for (var src : new String[]{"//evil.test/x", "https://evil.test/x", "javascript:alert(1)", "/a/../b", "\\\\evil.test/x"}) {
			var spec = PageSpec.create().htmlSrc("h", "H", src);
			var arg20 = new PageCapture();
			var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg20, MODEL), src);
			assertTrue(ex.getMessage().contains("<@card id='h'> type='html' src must be a same-origin path"), ex::getMessage);
		}
	}

	@Test void a50b_htmlSrc_sanitizeWithUnsafeSrc_srcRefusalWins() {
		var arg21 = new PageCapture();
		var spec = PageSpec.create().htmlSrc("h", "H", "//evil.test/x", true);
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg21, MODEL));
		assertTrue(ex.getMessage().contains("src must be a same-origin path"), ex::getMessage);
	}

	@Test void a51_htmlSrc_blankSrc_rejected() {
		var target16 = PageSpec.create();
		var nul = assertThrows(IllegalArgumentException.class, () -> target16.htmlSrc("h", "H", null));
		assertEquals("PageSpec.htmlSrc id 'h' requires a non-blank src.", nul.getMessage());
		var target17 = PageSpec.create();
		assertThrows(IllegalArgumentException.class, () -> target17.htmlSrc("h", "H", "  "));
	}

	@Test void a52_htmlSrc_sanitize_matchesTemplateTwin() throws Exception {
		var twin = CardDirective_Test.card(CardDirective_Test.get("/card-html-sanitize"), "on");
		var card = PageSpec.create().htmlSrc("on", null, "/rest/fragment", true).card("on").orElseThrow();
		assertEquals(twin, card.toMap());
		assertEquals(true, card.toMap().get("sanitize"));
	}

	@Test void a53_htmlSrc_sanitizeFalse_matchesThreeArgForm() {
		var off = PageSpec.create().htmlSrc("h", "H", "/x", false).card("h").orElseThrow().toMap();
		assertEquals(PageSpec.create().htmlSrc("h", "H", "/x").card("h").orElseThrow().toMap(), off);
		assertFalse(off.containsKey("sanitize"));
	}

	@Test void a54_htmlSrc_sanitize_carriedToApplyTo() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().htmlSrc("h", "H", "/ok", true).applyTo(cap, MODEL);
		assertEquals(true, cap.unplacedCards().get(0).toMap().get("sanitize"));
	}

	@Test void a55_sanitize_onNonHtmlOrSrcless_refusedWithTemplateMessage() {
		var notHtml = PageSpec.create().card(CardSpec.of("my-widget", "t").src("/rest/data").sanitize(true));
		var arg22 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> notHtml.applyTo(arg22, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='t'> sanitize= applies only to an html card with src=."), ex::getMessage);
		var noSrc = CardSpec.html("h").template("tpl").sanitize(true);
		var target21 = new PageCapture();
		var ex2 = assertThrows(freemarker.template.TemplateModelException.class, () -> target21.addCard(noSrc, null));
		assertTrue(ex2.getMessage().contains("<@card id='h'> sanitize= applies only to an html card with src=."), ex2::getMessage);
	}

	@Test void a56_applyTo_customCard_isShapedByItsHandler() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("meter", "load").title("Load").body("value", 3).body("max", 4)).applyTo(cap, CUSTOM);
		var card = cap.unplacedCards().get(0).toMap();
		assertEquals("meter", card.get("type"));
		assertEquals("Load", card.get("title"));
		assertEquals(Map.of("value", 3, "max", 4), card.get("meter"));
		assertFalse(card.containsKey("value"), card::toString);
	}

	@Test void a57_applyTo_invalidCustomCard_refusedByItsHandler() {
		var spec = PageSpec.create().card(CardSpec.of("meter", "load").body("max", 4));
		var arg23 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg23, CUSTOM));
		assertTrue(ex.getMessage().contains("<@card id='load'> type='meter' requires a numeric 'value'."), ex::getMessage);
	}

	@Test void a58_applyTo_customCardWithoutBody_refusedWithTheTemplateMessage() {
		var spec = PageSpec.create().card(CardSpec.of("meter", "load"));
		var arg24 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg24, CUSTOM));
		assertTrue(ex.getMessage().contains("<@card id='load'> type='meter' requires a JSON5 object body; got ''."), ex::getMessage);
	}

	@Test void a59_applyTo_customCard_liftKeepsClassBareAndWiring() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("meter", "load").body("value", 1).cssClass("wide").bare(true)
			.visibleWhen(org.apache.juneau.rest.server.views.VisibilityRule.when("a").present())
			.subscribes(org.apache.juneau.rest.server.console.Subscription.to("ops.jobs").as("refresh"))).applyTo(cap, CUSTOM);
		var card = cap.unplacedCards().get(0).toMap();
		assertEquals("wide", card.get("class"));
		assertEquals(true, card.get("bare"));
		assertNotNull(card.get("visibleWhen"));
		assertNotNull(card.get("subscribes"));
	}

	@Test void a60_applyTo_unregisteredType_reservedBodyKey_refusedWithTheTemplateMessage() {
		var spec = PageSpec.create().card(CardSpec.of("my-widget", "w").body("template", "t"));
		var arg25 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg25, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='w'> body key 'template' is reserved; set it as an attribute."), ex::getMessage);
	}

	@Test void a61_applyTo_customCardWithBodyAndSrc_refusedWithTheTemplateMessage() {
		var spec = PageSpec.create().card(CardSpec.of("my-widget", "w").src("/rest/w").body("k", 1));
		var arg26 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg26, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='w'> type='my-widget' takes at most one of a body, template= or src=."), ex::getMessage);
	}

	@Test void a62_applyTo_unregisteredTypeWithMarkup_keepsItsTemplate() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("my-widget", "w"), "<b>x</b>").applyTo(cap, MODEL);
		assertEquals(Map.of("w", "<b>x</b>"), cap.templates());
		assertEquals("w", cap.unplacedCards().get(0).template());
	}

	@Test void a63_applyTo_terminalCard_liftedThroughHandler() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("terminal", "out").body("terminal", Map.of("bytesUrl", "/t"))).applyTo(cap, MODEL);
		assertEquals(Map.of("bytesUrl", "/t"), cap.unplacedCards().get(0).toMap().get("terminal"));
	}

	@Test void a64_applyTo_customCardMarkup_isTheHandlersBody() {
		var spec = PageSpec.create().card(CardSpec.of("meter", "load"), "<b>x</b>");
		var arg27 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg27, CUSTOM));
		assertTrue(ex.getMessage().contains("<@card id='load'> type='meter' requires a JSON5 object body; got '<b>x</b>'."), ex::getMessage);
	}

	@Test void a65_applyTo_customCardJsonMarkup_isNotATemplate() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("meter", "load"), "{value: 3, max: 4}").applyTo(cap, CUSTOM);
		var card = cap.unplacedCards().get(0);
		assertEquals(Map.of("value", 3, "max", 4), card.toMap().get("meter"));
		assertNull(card.template());
		assertEquals(Map.of(), cap.templates());
	}

	@Test void a66_applyTo_customCardWithBodyAndMarkup_refusedWithTheTemplateMessage() {
		var spec = PageSpec.create().card(CardSpec.of("my-widget", "w").body("k", 1), "<b>x</b>");
		var arg28 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg28, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='w'> type='my-widget' takes at most one of a body, template= or src=."), ex::getMessage);
	}

	@Test void a67_applyTo_customCardWithBodyAndBlankMarkup_isBodyOnly() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("meter", "load").body("value", 1), "  ").applyTo(cap, CUSTOM);
		assertEquals(Map.of("value", 1, "max", 100), cap.unplacedCards().get(0).toMap().get("meter"));
		assertEquals(Map.of(), cap.templates());
	}

	@Test void a68_applyTo_customCardWithBodyAndTemplate_refusedWithTheTemplateMessage() {
		var spec = PageSpec.create().card(CardSpec.of("my-widget", "w").template("t").body("k", 1));
		var arg29 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg29, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='w'> type='my-widget' takes at most one of a body, template= or src=."), ex::getMessage);
	}

	@Test void a69_applyTo_customCardSrcOnly_keepsItsSrc() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("my-widget", "w").title("W").src("/rest/w")).applyTo(cap, MODEL);
		var card = cap.unplacedCards().get(0).toMap();
		assertEquals("/rest/w", card.get("src"));
		assertEquals("W", card.get("title"));
		assertFalse(card.containsKey("template"), card::toString);
	}

	@Test void a70_applyTo_customCardJsonMarkupBusKeys_areWired() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("my-widget", "w"), "{k: 1, subscribes: [ { topic: 'ssc.focus', as: 'refresh' } ]}").applyTo(cap, MODEL);
		var card = cap.unplacedCards().get(0).toMap();
		assertEquals(List.of(Map.of("topic", "ssc.focus", "as", "refresh")), card.get("subscribes"));
		assertEquals(1, card.get("k"));
	}

	@Test void a71_applyTo_customCardJsonMarkupBusKeys_withSpecWiring_refusedWithTheTemplateMessage() {
		var spec = PageSpec.create().card(CardSpec.of("my-widget", "w").subscribes(org.apache.juneau.rest.server.console.Subscription.to("ops.jobs").as("refresh")),
			"{k: 1, subscribes: [ { topic: 'ssc.focus', as: 'refresh' } ]}");
		var arg30 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg30, MODEL));
		assertTrue(ex.getMessage().contains("<@card id='w'> body key 'subscribes' is reserved; set it as an attribute."), ex::getMessage);
	}

	@Test void a72_applyTo_sanitizeOnCustomCard_refusedBeforeItsHandler() {
		var spec = PageSpec.create().card(CardSpec.of("meter", "load").body("max", 4).sanitize(true));
		var arg31 = new PageCapture();
		var ex = assertThrows(freemarker.template.TemplateModelException.class, () -> spec.applyTo(arg31, CUSTOM));
		assertTrue(ex.getMessage().contains("<@card id='load'> sanitize= applies only to an html card with src=."), ex::getMessage);
	}
}
