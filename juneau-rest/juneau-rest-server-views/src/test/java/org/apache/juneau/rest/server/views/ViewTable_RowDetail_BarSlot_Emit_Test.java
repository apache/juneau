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
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Emit shape for {@link RowDetailDef#barSlot} &mdash; the second named {@link BarSlot} host.
 *
 * <p>
 * The region is painted <b>inside the row-expand {@code <template>}</b>, so it is cloned per expanded row.  Two
 * server-side anchors exist, because the detail ribbon is assembled client-side and only for a detail with
 * <b>two or more</b> sections:
 *
 * <ul>
 * 	<li><b>&ge;2 sections</b> &mdash; the region is the template's <b>last direct child</b> and carries
 * 		{@code data-juneau-bar-slot-anchor="ribbon"}; the runtime moves it to the ribbon's trailing position.
 * 	<li><b>1 section</b> &mdash; there is no ribbon and none is synthesized, so the region is emitted <b>inside</b>
 * 		that lone section as the immediate next sibling of {@code h2.juneau-view-detail-section-title}, carrying
 * 		{@code data-juneau-bar-slot-anchor="section-title"}.
 * </ul>
 *
 * <p>
 * The sidecar is emitted {@code id}-less and found by {@link BarSlotTable#BAR_META_ATTR}, exactly like the nested-table
 * VIEW_META sidecar: a {@code <template>} clone cannot carry a document-unique {@code id}, so the runtime mints one per
 * expanded row.  Nothing here lands in the archived DataTables toolbar row, and nothing here is a nav tab.
 */
class ViewTable_RowDetail_BarSlot_Emit_Test extends TestBase {

	private static final String AUTHOR_ID = "detail-ctx";

	/** The full marker attribute, so an {@code indexOf} never matches the longer anchor attribute by prefix. */
	private static final String MARKER = BarSlotTable.BAR_SLOT_MARKER + "=\"" + AUTHOR_ID + "\"";

	private static BarSlot bar() {
		return BarSlot.create(AUTHOR_ID).widgets(BarBadge.of("open").label("Open").badge(Badge.count(3)));
	}

	private static DetailSection section(String id, String title, String field) {
		return DetailSection.create(id, title).fields(DetailField.of(field).title(field));
	}

	private static String html(DetailSection...sections) {
		var v = ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create().endpoint("/data/alerts/{id}").sections(sections).barSlot(bar()))
			.build();
		return Html.of(ViewTable.of(v));
	}

	private static String twoSections() {
		return html(section("overview", "Overview", "severity"), section("detail", "Detail", "summary"));
	}

	private static String oneSection() {
		return html(section("overview", "Overview", "severity"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// The region is inside the detail template, exactly once, never a toolbar row and never a nav tab
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_regionEmittedInsideTheDetailTemplate() {
		var h = twoSections();
		assertTrue(h.contains(MARKER), h);
		assertTrue(h.indexOf(MARKER) > h.indexOf(ViewTable.DETAIL_TEMPLATE_ATTR), h);
		assertTrue(h.indexOf(MARKER) < h.indexOf("</template>"), h);
	}

	@Test void a02_regionEmittedExactlyOnce() {
		var h = twoSections();
		assertEquals(h.indexOf(MARKER), h.lastIndexOf(MARKER), h);
	}

	@Test void a03_neverAToolbarRowAndNeverANavTab() {
		var h = twoSections();
		assertFalse(h.contains("jc-nav-tab"), h);
		assertFalse(h.contains("jc-nav"), h);
		assertFalse(h.contains("juneau-view-toolbar"), h);
		assertFalse(h.contains("jc-subtab-bar"), h);
	}

	@Test void a04_widgetsArePaintedIntoTheRegion() {
		var h = twoSections();
		assertTrue(h.contains("jc-bar-slot"), h);
		assertTrue(h.contains("jc-bar-badge"), h);
		assertTrue(h.contains(">Open<"), h);
	}

	@Test void a05_noRegionWhenNoDetailBarSlotDeclared() {
		var v = ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.sections(section("overview", "Overview", "severity")))
			.build();
		var h = Html.of(ViewTable.of(v));
		assertFalse(h.contains(BarSlotTable.BAR_SLOT_MARKER), h);
		assertFalse(h.contains(BarSlotTable.BAR_META_ATTR), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Anchors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_twoSections_anchorIsRibbon_andRegionIsTheLastDirectChild() {
		var h = twoSections();
		assertTrue(h.contains(BarSlotTable.BAR_SLOT_ANCHOR_ATTR + "=\"" + BarSlotTable.ANCHOR_RIBBON + "\""), h);
		// Last direct child of the template: after BOTH sections, so nothing can be mistaken for section content.
		assertTrue(h.indexOf(MARKER) > h.lastIndexOf("juneau-view-detail-section-title"), h);
		assertTrue(h.indexOf(MARKER) > h.lastIndexOf("</section>"), h);
	}

	@Test void b02_oneSection_anchorIsSectionTitle_andNoRibbonIsSynthesized() {
		var h = oneSection();
		assertTrue(h.contains(BarSlotTable.BAR_SLOT_ANCHOR_ATTR + "=\"" + BarSlotTable.ANCHOR_SECTION_TITLE + "\""), h);
		assertFalse(h.contains("juneau-view-detail-tabs"), h);
		assertFalse(h.contains("data-juneau-strip-mode"), h);
	}

	@Test void b03_oneSection_regionImmediatelyFollowsTheSectionTitle() {
		var h = oneSection();
		var titleEnd = h.indexOf("</h2>", h.indexOf("juneau-view-detail-section-title"));
		assertTrue(titleEnd > 0, h);
		var region = h.indexOf(MARKER);
		var fields = h.indexOf("juneau-view-detail-fields");
		// Between the section title and the fields grid, and still inside the <section>.
		assertTrue(region > titleEnd, h);
		assertTrue(region < fields, h);
		assertTrue(region < h.indexOf("</section>"), h);
	}

	@Test void b04_detailRegionCarriesTheDetailClassForCssAndRelocation() {
		assertTrue(twoSections().contains(BarSlotTable.DETAIL_SLOT_CLASS), twoSections());
		assertTrue(oneSection().contains(BarSlotTable.DETAIL_SLOT_CLASS), oneSection());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Sidecar: id-less in the template, found by attribute (the runtime mints the per-row id)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_sidecarIsIdLess_andFoundByAttribute() {
		var h = twoSections();
		assertTrue(h.contains(BarSlotTable.BAR_META_ATTR + "=\"" + AUTHOR_ID + "\""), h);
		assertFalse(h.contains("id=\"" + BarSlotTable.SIDECAR_ID_PREFIX), h);
	}

	@Test void c02_sidecarCarriesTheBarContractAndInitialCounts() {
		var h = twoSections();
		var at = h.indexOf(BarSlotTable.BAR_META_ATTR);
		var json = h.substring(at, h.indexOf("</script>", at));
		assertTrue(json.contains("\"contractVersion\":\"" + BarSlot.CONTRACT_VERSION + "\""), json);
		assertTrue(json.contains(BarSlotTable.BADGE_NS + ":open"), json);
	}

	@Test void c03_sidecarIsADirectChildOfTheTemplate() {
		var h = twoSections();
		assertTrue(h.indexOf(BarSlotTable.BAR_META_ATTR) > h.indexOf(ViewTable.DETAIL_TEMPLATE_ATTR), h);
		assertTrue(h.indexOf(BarSlotTable.BAR_META_ATTR) < h.indexOf("</template>"), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// No contract bump: an existing expand GET still handshakes
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_expandGetHandshakeUnchanged() {
		for (var h : new String[]{twoSections(), oneSection()}) {
			assertTrue(h.contains("data-juneau-detail-contract=\"1\""), h);
			assertEquals("1", RowDetailDef.CONTRACT_VERSION);
		}
	}
}
