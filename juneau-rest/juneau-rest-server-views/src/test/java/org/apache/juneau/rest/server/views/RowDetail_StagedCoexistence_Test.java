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
 * WORK-J0522d, design test <b>48a</b>: SF-E's <b>staged coexistence</b>, asserted rather than promised.
 *
 * <p>
 * The one test that would have caught the blank-or-double-render window an unstaged collapse would have shipped.
 * <b>One page, two views</b>: one declaring the deprecated {@code .sections(...)} with three sections, one declaring
 * the new {@code .region(...)}. Both emit, in the same build, and each draws exactly one body.
 *
 * <p>
 * Deliberately an <b>emitter</b> test rather than a bean test, because both failure modes are structural: a panel
 * that renders its content twice and a panel that renders it not at all are both things you see in the DOM. The six
 * sub-cases are the design's own:
 *
 * <ol>
 * 	<li><b>(i)</b> exactly one framework-drawn strip &mdash; in the {@code .sections(...)} panel, and its input (the
 * 		{@code [data-juneau-detail-section]} nodes the client builds the strip from) is present and countable.
 * 	<li><b>(ii)</b> exactly one author strip &mdash; the {@code .region(...)} panel's single region container, which
 * 		is where {@code helpers.tabStrip} paints.
 * 	<li><b>(iii)</b> no second body anywhere &mdash; the {@code .region(...)} panel has no section frames and no
 * 		field slots; the {@code .sections(...)} panel has no region container.
 * 	<li><b>(iv)</b> the per-bean {@code enabledWhen} absorb &mdash; the {@code .sections(...)} bean still passes its
 * 		cross-check and still gates its buttons, while the {@code .region(...)} bean is <b>rejected at startup</b> for
 * 		declaring one. This is the half a process-wide reading of the absorb would have broken.
 * 	<li><b>(v)</b> the XOR &mdash; a bean setting both fails startup, rather than one of them being silently unread.
 * 	<li><b>(vi)</b> a {@code .region(...)}-only bean with <b>no</b> sections <i>constructs</i>. This is the direction
 * 		that was <b>false</b> before this change, where the "at least one section" throw fired unconditionally, and it
 * 		is <b>the assertion that fails first</b> if the emitter/validation table is not built.
 * </ol>
 */
class RowDetail_StagedCoexistence_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// z) The populator-name stamp.  Found by standing design 11.1a up as a LIVE PAGE, not by any unit test here -
	//    which is precisely the argument for test 53 being a running page rather than a compile check.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * A named populator must be stamped on the container as {@code data-juneau-region-populate}.
	 *
	 * <p>
	 * <b>This failed silently before it was fixed, in the worst available way.</b> {@code mintRegion} reads the name
	 * with {@code el.getAttribute(...)}; a missing attribute reads {@code null}, and {@code null} resolves to the
	 * <b>default</b> populator rather than to an error. So a view whose author registered {@code "instance-detail"}
	 * rendered the framework default instead: no console error, no error pane, a plausible-looking panel, and the
	 * author's populate never invoked. Nothing in the emitter or bean suites noticed, because every one of them
	 * asserted on the region's identity, type and contract stamp &mdash; none on the one attribute that decides which
	 * function actually runs.
	 */
	@Test void z01_namedPopulator_isStampedOnTheContainer() {
		var region = RegionDef.create("detail").allowPopulators("thing-detail").populate("thing-detail");
		var html = Html.of(RegionTable.of(region));
		assertTrue(html.contains("data-juneau-region-populate=\"thing-detail\""),
			() -> "the populator name must ride the container, or mintRegion silently falls back to the default "
				+ "populator and the author's populate never runs:\n" + html);
	}

	/** No {@code populate} declared means the attribute is ABSENT, not blank - a blank is a third state nobody handles. */
	@Test void z02_unnamedPopulator_omitsTheAttributeEntirely() {
		var html = Html.of(RegionTable.of(RegionDef.create("detail")));
		assertFalse(html.contains("data-juneau-region-populate"),
			() -> "an absent attribute is how 'use the default populator' is expressed; a blank one is a third "
				+ "state the runtime does not model:\n" + html);
	}

	/** The stamp survives the real detail-emitter path, not just a direct RegionTable call. */
	@Test void z03_theStampSurvivesTheDetailEmitterPath() {
		var view = ViewDef.create("things")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint(ENDPOINT)
				.region(RegionDef.create("detail").allowPopulators("thing-detail").populate("thing-detail")))
			.build();
		var html = Html.of(ViewTable.of(view));
		assertTrue(html.contains("data-juneau-region-populate=\"thing-detail\""),
			() -> "the detail emitter must carry the populator stamp through to the template:\n" + html);
	}

	// NOT "servlet:/things/{id}": RowDetailDef.validateEndpoint rejects a colon-before-slash as a scheme, so the
	// pseudo-scheme form the design's own §11.1a snippet uses does not validate.  A plain same-origin path does.
	private static final String ENDPOINT = "/things/{id}";

	private static RowAction ack() {
		return RowAction.create("ack").endpoint("/things/{id}/ack").method(RowAction.Method.POST);
	}

	private static DetailSection section(String id) {
		return DetailSection.create(id, id).fields(DetailField.of(id + "Key").title(id + " Key"));
	}

	/** The deprecated shape: three sections, a gated header button, the whole existing tree. */
	@SuppressWarnings("deprecation")
	private static ViewDef sectionsView() {
		return ViewDef.create("legacy")
			.columns(Column.of("id").title("ID"))
			.rowActions(ack())
			.details(RowDetailDef.create()
				.endpoint(ENDPOINT)
				.headerActions(ActionBar.create().items(
					ActionRef.of("ack").enabledWhen("aKey", Op.EQ, "open", "Not open.")))
				.sections(section("a"), section("b"), section("c")))
			.build();
	}

	/** The new shape: one region, no sections, no framework strip. */
	private static ViewDef regionView() {
		return ViewDef.create("modern")
			.columns(Column.of("id").title("ID"))
			.details(RowDetailDef.create()
				.endpoint(ENDPOINT)
				.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail")))
			.build();
	}

	private static String html(ViewDef v) {
		return Html.of(ViewTable.of(v));
	}

	private static int count(String haystack, String needle) {
		var n = 0;
		var i = haystack.indexOf(needle);
		while (i >= 0) {
			n++;
			i = haystack.indexOf(needle, i + needle.length());
		}
		return n;
	}

	//------------------------------------------------------------------------------------------------------------------
	// (i) Exactly one framework-drawn strip - and it is the .sections(...) panel's.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_sectionsPanel_emitsTheStripsInput_threeSectionFrames() {
		var h = html(sectionsView());
		assertEquals(3, count(h, ViewTable.DETAIL_SECTION_ATTR + "=\""),
			() -> "the framework strip is built client-side from these nodes; three sections means three:\n" + h);
		assertTrue(h.contains(ViewTable.DETAIL_SECTION_ATTR + "=\"a\""), h);
		assertTrue(h.contains(ViewTable.DETAIL_SECTION_ATTR + "=\"c\""), h);
	}

	@Test void a02_regionPanel_emitsNoSectionFrames_soTheFrameworkStripNoOps() {
		var h = html(regionView());
		assertEquals(0, count(h, ViewTable.DETAIL_SECTION_ATTR + "=\""),
			() -> "a region panel has NO detail-section nodes, which is exactly why buildDetailStrip returns null "
				+ "for it with no client edit:\n" + h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// (ii) Exactly one author strip host - the region container - and exactly one of them.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_regionPanel_emitsExactlyOneEmptyRegionContainer() {
		var h = html(regionView());
		assertEquals(1, count(h, RegionTable.REGION_ATTR + "=\""),
			() -> "chrome plus EXACTLY ONE empty region container:\n" + h);
		assertTrue(h.contains(RegionTable.REGION_ATTR + "=\"detail\""), h);
		assertTrue(h.contains(RegionTable.REGION_TYPE_ATTR + "=\"" + RegionDef.TYPE_ROW_DETAIL + "\""),
			() -> "ctx.type for detail is 'row-detail' (SF-G):\n" + h);
		assertTrue(h.contains(RegionTable.REGION_CONTRACT_ATTR + "=\"" + RegionDef.CONTRACT_VERSION + "\""), h);
	}

	@Test void b02_regionContainerIsEmpty() {
		var h = html(regionView());
		var i = h.indexOf(RegionTable.REGION_ATTR + "=\"detail\"");
		assertTrue(i >= 0, h);
		var close = h.indexOf('>', i);
		assertTrue(h.startsWith("</div>", close + 1),
			() -> "the container must be EMPTY - the server emits no content into it at all:\n" + h);
	}

	@Test void b03_regionSidecarCarriesTheDescriptor_oneSidecarPerHost() {
		var h = html(regionView());
		assertEquals(1, count(h, RegionTable.REGION_META_ATTR + "=\""),
			() -> "one id-less, attribute-found sidecar per host (the template is cloned per row):\n" + h);
		assertTrue(h.contains("\\\"populate\\\":\\\"thing-detail\\\"") || h.contains("\"populate\":\"thing-detail\""),
			() -> "the populator name rides the sidecar, not an attribute:\n" + h);
	}

	@Test void b04_regionSidecarIsIdless_becauseTheTemplateIsClonedPerRow() {
		var h = html(regionView());
		assertFalse(h.contains("id=\"" + RegionTable.SIDECAR_ID_PREFIX),
			() -> "a stamped id would collide across every open panel:\n" + h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// (iii) No second body anywhere, in BOTH directions.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_regionPanel_hasNoFieldSlots() {
		var h = html(regionView());
		assertEquals(0, count(h, ViewTable.DETAIL_FIELD_ATTR + "=\""),
			() -> "the author's catalog is a JS literal; the server emits no field slots on this path:\n" + h);
	}

	@Test void c02_sectionsPanel_hasNoRegionContainer() {
		var h = html(sectionsView());
		assertEquals(0, count(h, RegionTable.REGION_ATTR + "=\""),
			() -> "an unmigrated panel gets today's output byte for byte - no region container appears:\n" + h);
		assertEquals(0, count(h, RegionTable.REGION_META_ATTR + "=\""), h);
	}

	@Test void c03_sectionsPanel_stillEmitsItsFieldSlots() {
		var h = html(sectionsView());
		assertEquals(3, count(h, ViewTable.DETAIL_FIELD_ATTR + "=\""),
			() -> "the deprecated path still EMITS - deprecation is a marking, not a removal:\n" + h);
	}

	@Test void c04_bothShapesEmitInTheSameBuild_onOnePage() {
		// The coexistence claim itself: one page, both views, both panels, each with exactly one body.
		var page = PageDef.create("p1").tabs(
			Tab.create("legacy", "Legacy").view(sectionsView()),
			Tab.create("modern", "Modern").view(regionView()));
		page.validate();
		var h = Html.of(PageTable.of(page));
		assertEquals(3, count(h, ViewTable.DETAIL_SECTION_ATTR + "=\""), h);
		assertEquals(1, count(h, RegionTable.REGION_ATTR + "=\""), h);
		assertEquals(2, count(h, ViewTable.DETAIL_TEMPLATE_ATTR + "=\""),
			() -> "two row-detail templates, one per view, on one page:\n" + h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// (iv) The per-bean enabledWhen absorb - BOTH halves, because a process-wide reading breaks the first one.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_sectionsBean_keepsEnabledWhenAndItsCrossCheck() {
		// Not merely "does not throw": the rule survives ON the bean, and the cross-check that validates it still
		// runs.  d02 is what proves the check is live rather than skipped.
		var v = sectionsView();
		v.validate();
		var ar = (ActionRef) v.details.headerActions.items.get(0);
		assertNotNull(ar.enabledWhen, "an unmigrated bean keeps enabledWhen for the whole window");
		assertEquals(1, ar.enabledWhen.size());
	}

	@SuppressWarnings("deprecation")
	@Test void d02_sectionsBean_crossCheckStillFiresOnATypo() {
		var v = ViewDef.create("legacy")
			.columns(Column.of("id").title("ID"))
			.rowActions(ack())
			.details(RowDetailDef.create()
				.endpoint(ENDPOINT)
				.headerActions(ActionBar.create().items(
					ActionRef.of("ack").enabledWhen("nopeNotAField", Op.EQ, "open", "Not open.")))
				.sections(section("a")))
			.build();
		var e = assertThrows(IllegalArgumentException.class, v::validate);
		assertTrue(e.getMessage().contains("nopeNotAField"), e.getMessage());
		assertTrue(e.getMessage().contains("no DetailField of this panel returns"), e.getMessage());
	}

	@Test void d03_regionBean_declaringEnabledWhen_isRejectedAtStartup() {
		var v = ViewDef.create("modern")
			.columns(Column.of("id").title("ID"))
			.rowActions(ack())
			.details(RowDetailDef.create()
				.endpoint(ENDPOINT)
				.headerActions(ActionBar.create().items(
					ActionRef.of("ack").enabledWhen("status", Op.EQ, "open", "Not open.")))
				.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail")))
			.build();
		var e = assertThrows(IllegalArgumentException.class, v::validate);
		assertTrue(e.getMessage().contains("enabledWhen"), e.getMessage());
		// Never silently ignored: the message must name the replacement, or an author has no way forward.
		assertTrue(e.getMessage().contains("ctx.write"), e.getMessage());
		assertTrue(e.getMessage().contains("region body"), e.getMessage());
	}

	@Test void d04_regionBean_withNoEnabledWhen_keepsItsUngatedHeaderBar() {
		// The absorb removes the GATE, not the bar: an ungated ActionRef on a region panel is still legal.
		var v = ViewDef.create("modern")
			.columns(Column.of("id").title("ID"))
			.rowActions(ack())
			.details(RowDetailDef.create()
				.endpoint(ENDPOINT)
				.headerActions(ActionBar.create().items(ActionRef.of("ack")))
				.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail")))
			.build();
		assertDoesNotThrow(v::validate);
	}

	//------------------------------------------------------------------------------------------------------------------
	// (v) The XOR - a startup rejection, not a precedence rule.
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	@Test void e01_bothSectionsAndRegion_failsStartup() {
		var d = RowDetailDef.create()
			.endpoint(ENDPOINT)
			.sections(section("a"))
			.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail"));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("mutually exclusive"), e.getMessage());
	}

	@SuppressWarnings("deprecation")
	@Test void e02_theXorIsOrderIndependent() {
		// Declaring them the other way round must fail identically - a precedence rule would make one order "work".
		var d = RowDetailDef.create()
			.endpoint(ENDPOINT)
			.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail"))
			.sections(section("a"));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("mutually exclusive"), e.getMessage());
	}

	@Test void e03_neitherSectionsNorRegion_stillFails() {
		var d = RowDetailDef.create().endpoint(ENDPOINT);
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("at least one section, or a region"), e.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// (vi) THE ASSERTION THAT FAILS FIRST if the emitter/validation table is not built.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_regionOnlyBeanWithNoSections_constructs() {
		var d = RowDetailDef.create()
			.endpoint(ENDPOINT)
			.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail"));
		assertNull(d.sections, "a region-only bean declares no sections at all");
		assertDoesNotThrow(() -> d.validate(null),
			"before the emitter/validation table existed, the 'at least one section' throw fired unconditionally "
			+ "and this bean could not be constructed - so the new path did not exist");
	}

	@Test void f02_regionOnlyBean_isRegionBody() {
		var d = RowDetailDef.create().endpoint(ENDPOINT).region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail"));
		assertTrue(d.isRegionBody());
	}

	@SuppressWarnings("deprecation")
	@Test void f03_sectionsOnlyBean_isNotRegionBody() {
		var d = RowDetailDef.create().endpoint(ENDPOINT).sections(section("a"));
		assertFalse(d.isRegionBody());
	}

	@Test void f04_regionSetterStampsTheRowDetailTypeOnTheAuthorsBehalf() {
		var r = RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail");
		assertNull(r.type, "an author never declares the type - the host does");
		RowDetailDef.create().endpoint(ENDPOINT).region(r);
		assertEquals(RegionDef.TYPE_ROW_DETAIL, r.type);
	}

	@Test void f05_regionSetterRejectsNull() {
		assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create().region(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// The type stamp is what makes RegionDef's own row-detail catalog rule reachable - the two halves must agree.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_regionBodyDeclaringProjectedFields_isRejected() {
		// A row-detail catalog is an author JS literal, never a projected one.  RegionDef enforces it; the stamp
		// applied by region(...) is what lets it.
		var d = RowDetailDef.create()
			.endpoint(ENDPOINT)
			.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail").fields(RegionDef.Field.of("k")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("must not declare fields"), e.getMessage());
	}

	@Test void g02_regionBodyValidationCascadesIntoTheRegion() {
		var d = RowDetailDef.create()
			.endpoint(ENDPOINT)
			.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail").dataUrl("https://evil.example/x"));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("same-origin"), e.getMessage());
	}
}
