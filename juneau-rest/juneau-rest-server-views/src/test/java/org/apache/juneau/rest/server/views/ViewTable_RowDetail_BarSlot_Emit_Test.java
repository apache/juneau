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
 * Emit shape for {@link RowDetailDef#barSlot} on a region-only panel.
 *
 * <p>
 * A region panel has no framework ribbon, so the server always anchors the slot at
 * {@link BarSlotTable#ANCHOR_SECTION_TITLE}.  The sidecar is {@code id}-less and found by
 * {@link BarSlotTable#BAR_META_ATTR}.
 */
class ViewTable_RowDetail_BarSlot_Emit_Test extends TestBase {

	private static final String AUTHOR_ID = "detail-ctx";

	/** The full marker attribute, so an {@code indexOf} never matches the longer anchor attribute by prefix. */
	private static final String MARKER = BarSlotTable.BAR_SLOT_MARKER + "=\"" + AUTHOR_ID + "\"";

	private static BarSlot bar() {
		return BarSlot.create(AUTHOR_ID).widgets(BarBadge.of("open").label("Open").badge(Badge.count(3)));
	}

	private static String html(boolean withBar) {
		var d = RowDetailDef.create()
			.endpoint("/data/alerts/{id}")
			.region(RegionDef.create("detail").allowPopulators("p").populate("p"));
		if (withBar)
			d.barSlot(bar());
		var v = ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.details(d)
			.build();
		return Html.of(ViewTable.of(v));
	}

	@Test void a01_regionEmittedInsideTheDetailTemplate() {
		var h = html(true);
		assertTrue(h.contains(MARKER), h);
		assertTrue(h.indexOf(MARKER) > h.indexOf(ViewTable.DETAIL_TEMPLATE_ATTR), h);
		assertTrue(h.indexOf(MARKER) < h.indexOf("</template>"), h);
	}

	@Test void a02_regionEmittedExactlyOnce() {
		var h = html(true);
		assertEquals(h.indexOf(MARKER), h.lastIndexOf(MARKER), h);
	}

	@Test void a03_neverAToolbarRowAndNeverANavTab() {
		var h = html(true);
		assertFalse(h.contains("jc-nav-tab"), h);
		assertFalse(h.contains("jc-nav"), h);
		assertFalse(h.contains("juneau-view-toolbar"), h);
		assertFalse(h.contains("jc-subtab-bar"), h);
	}

	@Test void a04_widgetsArePaintedIntoTheRegion() {
		var h = html(true);
		assertTrue(h.contains("jc-bar-slot"), h);
		assertTrue(h.contains("jc-bar-badge"), h);
		assertTrue(h.contains(">Open<"), h);
	}

	@Test void a05_noRegionWhenNoDetailBarSlotDeclared() {
		var h = html(false);
		assertFalse(h.contains(BarSlotTable.BAR_SLOT_MARKER), h);
		assertFalse(h.contains(BarSlotTable.BAR_META_ATTR), h);
	}

	@Test void b01_anchorIsSectionTitle_andNoRibbonIsSynthesized() {
		var h = html(true);
		assertTrue(h.contains(BarSlotTable.BAR_SLOT_ANCHOR_ATTR + "=\"" + BarSlotTable.ANCHOR_SECTION_TITLE + "\""), h);
		assertFalse(h.contains("juneau-view-detail-tabs"), h);
		assertFalse(h.contains("data-juneau-strip-mode"), h);
	}

	@Test void b02_detailRegionCarriesTheDetailClassForCssAndRelocation() {
		assertTrue(html(true).contains(BarSlotTable.DETAIL_SLOT_CLASS), html(true));
	}

	@Test void c01_sidecarIsIdLess_andFoundByAttribute() {
		var h = html(true);
		assertTrue(h.contains(BarSlotTable.BAR_META_ATTR + "=\"" + AUTHOR_ID + "\""), h);
		assertFalse(h.contains("id=\"" + BarSlotTable.SIDECAR_ID_PREFIX), h);
	}

	@Test void c02_sidecarCarriesTheBarContractAndInitialCounts() {
		var h = html(true);
		var at = h.indexOf(BarSlotTable.BAR_META_ATTR);
		var json = h.substring(at, h.indexOf("</script>", at));
		assertTrue(json.contains("\"contractVersion\":\"" + BarSlot.CONTRACT_VERSION + "\""), json);
		assertTrue(json.contains(BarSlotTable.BADGE_NS + ":open"), json);
	}

	@Test void c03_sidecarIsADirectChildOfTheTemplate() {
		var h = html(true);
		assertTrue(h.indexOf(BarSlotTable.BAR_META_ATTR) > h.indexOf(ViewTable.DETAIL_TEMPLATE_ATTR), h);
		assertTrue(h.indexOf(BarSlotTable.BAR_META_ATTR) < h.indexOf("</template>"), h);
	}

	@Test void d01_expandGetHandshakeUnchanged() {
		var h = html(true);
		assertTrue(h.contains("data-juneau-detail-contract=\"1\""), h);
		assertEquals("1", RowDetailDef.CONTRACT_VERSION);
	}
}
