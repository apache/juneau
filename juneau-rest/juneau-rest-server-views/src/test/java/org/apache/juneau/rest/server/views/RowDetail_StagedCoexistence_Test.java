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
 * After the deprecation window closed, design test 48a's {@code .sections(...)} arm has no subject.
 *
 * <p>
 * The surviving assertions are the {@code .region(...)}-only arms: named populator stamp, exactly one region
 * container, no section frames, no field slots, {@code enabledWhen} rejected on the region path, and a missing
 * region fails startup.  XOR / sections coexistence cases retired with {@code RowDetailDef.sections(...)}.
 */
class RowDetail_StagedCoexistence_Test extends TestBase {

	private static final String ENDPOINT = "/things/{id}";

	private static RowAction ack() {
		return RowAction.create("ack").endpoint("/things/{id}/ack").method(RowAction.Method.POST);
	}

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
	// z) The populator-name stamp.
	//------------------------------------------------------------------------------------------------------------------

	@Test void z01_namedPopulator_isStampedOnTheContainer() {
		var region = RegionDef.create("detail").allowPopulators("thing-detail").populate("thing-detail");
		var html = Html.of(RegionTable.of(region));
		assertTrue(html.contains("data-juneau-region-populate=\"thing-detail\""),
			() -> "the populator name must ride the container, or mintRegion silently falls back to the default "
				+ "populator and the author's populate never runs:\n" + html);
	}

	@Test void z02_unnamedPopulator_omitsTheAttributeEntirely() {
		var html = Html.of(RegionTable.of(RegionDef.create("detail")));
		assertFalse(html.contains("data-juneau-region-populate"),
			() -> "an absent attribute is how 'use the default populator' is expressed; a blank one is a third "
				+ "state the runtime does not model:\n" + html);
	}

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

	@Test void a01_regionPanel_emitsNoSectionFrames() {
		var h = html(regionView());
		assertEquals(0, count(h, ViewTable.DETAIL_SECTION_ATTR + "=\""),
			() -> "a region panel has NO detail-section nodes:\n" + h);
	}

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

	@Test void c01_regionPanel_hasNoFieldSlots() {
		var h = html(regionView());
		assertEquals(0, count(h, ViewTable.DETAIL_FIELD_ATTR + "=\""),
			() -> "the author's catalog is a JS literal; the server emits no field slots on this path:\n" + h);
	}

	@Test void c02_twoRegionViewsOnOnePage_eachEmitOneBody() {
		var page = PageDef.create("p1").tabs(
			Tab.create("a", "A").view(regionView()),
			Tab.create("b", "B").view(ViewDef.create("other")
				.columns(Column.of("id").title("ID"))
				.details(RowDetailDef.create()
					.endpoint(ENDPOINT)
					.region(RegionDef.create("other").populate("other-detail").allowPopulators("other-detail")))
				.build()));
		page.validate();
		var h = Html.of(PageTable.of(page));
		assertEquals(0, count(h, ViewTable.DETAIL_SECTION_ATTR + "=\""), h);
		assertEquals(2, count(h, RegionTable.REGION_ATTR + "=\""), h);
		assertEquals(2, count(h, ViewTable.DETAIL_TEMPLATE_ATTR + "=\""),
			() -> "two row-detail templates, one per view, on one page:\n" + h);
	}

	@Test void d01_regionBean_declaringEnabledWhen_isRejectedAtStartup() {
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
		assertTrue(e.getMessage().contains("ctx.write"), e.getMessage());
		assertTrue(e.getMessage().contains("region body"), e.getMessage());
	}

	@Test void d02_regionBean_withNoEnabledWhen_keepsItsUngatedHeaderBar() {
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

	@Test void e01_missingRegion_failsStartup() {
		var d = RowDetailDef.create().endpoint(ENDPOINT);
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("region"), e.getMessage());
	}

	@Test void f01_regionOnlyBean_constructs() {
		var d = RowDetailDef.create()
			.endpoint(ENDPOINT)
			.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail"));
		assertDoesNotThrow(() -> d.validate(null));
	}

	@Test void f02_regionOnlyBean_isRegionBody() {
		var d = RowDetailDef.create().endpoint(ENDPOINT)
			.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail"));
		assertTrue(d.isRegionBody());
	}

	@Test void f03_regionSetterStampsTheRowDetailTypeOnTheAuthorsBehalf() {
		var r = RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail");
		assertNull(r.type, "an author never declares the type - the host does");
		RowDetailDef.create().endpoint(ENDPOINT).region(r);
		assertEquals(RegionDef.TYPE_ROW_DETAIL, r.type);
	}

	@Test void f04_regionSetterRejectsNull() {
		assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create().region(null));
	}

	@Test void g01_regionBodyDeclaringProjectedFields_isRejected() {
		var d = RowDetailDef.create()
			.endpoint(ENDPOINT)
			.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail")
				.fields(RegionDef.Field.of("k")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("must not declare fields"), e.getMessage());
	}

	@Test void g02_regionBodyValidationCascadesIntoTheRegion() {
		var d = RowDetailDef.create()
			.endpoint(ENDPOINT)
			.region(RegionDef.create("detail").populate("thing-detail").allowPopulators("thing-detail")
				.dataUrl("https://evil.example/x"));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("same-origin"), e.getMessage());
	}
}
