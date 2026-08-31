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
 * Pins {@link BarSlotTable#ANCHOR_DIALOG_TITLE} &mdash; the anchor constant for the <b>third</b> named
 * {@link BarSlot} host, a dialog's {@code ModalDef.barSlot}.
 *
 * <p>
 * Unlike the row-detail host, no server-side code path ever calls {@link BarSlotTable#detailRegion} with this anchor
 * in production: a dialog is a fetched JSON payload with no server-rendered pass to ride into, so the
 * {@code juneau-views.js} runtime paints the region client-side instead (pinned separately by the node harness in
 * {@code ViewsJs_Dialog_BarSlot_Test}).  This class instead proves the SHAPE the runtime must mirror: the existing,
 * anchor-agnostic {@link BarSlotTable#detailRegion(BarSlot, String)} emitter already produces the correct markup for
 * this third anchor value with no code change, exactly as it does for {@link BarSlotTable#ANCHOR_RIBBON} and
 * {@link BarSlotTable#ANCHOR_SECTION_TITLE} &mdash; the "wire the dialog host through {@code of}/{@code
 * detailRegion}" shape the spec calls for.
 */
class BarSlotTable_DialogAnchor_Test extends TestBase {

	private static BarSlot bar() {
		return BarSlot.create("dialog-ctx").widgets(BarBadge.of("open").label("Open").badge(Badge.count(3)));
	}

	@Test void a01_anchorConstant_isDialogTitle() {
		assertEquals("dialog-title", BarSlotTable.ANCHOR_DIALOG_TITLE);
	}

	@Test void a02_anchorConstant_isDistinctFromTheOtherTwo() {
		assertNotEquals(BarSlotTable.ANCHOR_DIALOG_TITLE, BarSlotTable.ANCHOR_RIBBON);
		assertNotEquals(BarSlotTable.ANCHOR_DIALOG_TITLE, BarSlotTable.ANCHOR_SECTION_TITLE);
	}

	@Test void a03_detailRegion_emitsTheDialogAnchorAttribute() {
		var h = Html.of(BarSlotTable.detailRegion(bar(), BarSlotTable.ANCHOR_DIALOG_TITLE));
		assertTrue(h.contains(BarSlotTable.BAR_SLOT_ANCHOR_ATTR + "=\"" + BarSlotTable.ANCHOR_DIALOG_TITLE + "\""), h);
	}

	@Test void a04_detailRegion_carriesTheAuthorMarkerAndDetailClass() {
		var h = Html.of(BarSlotTable.detailRegion(bar(), BarSlotTable.ANCHOR_DIALOG_TITLE));
		assertTrue(h.contains(BarSlotTable.BAR_SLOT_MARKER + "=\"dialog-ctx\""), h);
		assertTrue(h.contains(BarSlotTable.DETAIL_SLOT_CLASS), h);
	}

	@Test void a05_detailRegion_paintsTheWidget() {
		var h = Html.of(BarSlotTable.detailRegion(bar(), BarSlotTable.ANCHOR_DIALOG_TITLE));
		assertTrue(h.contains("jc-bar-slot"), h);
		assertTrue(h.contains("jc-bar-badge"), h);
		assertTrue(h.contains(">Open<"), h);
	}

	@Test void a06_detailSidecar_isIdLess_andFoundByAttribute_sameAsTheOtherTwoAnchors() {
		// The dialog runtime mints its own clone-time id from this id-less shape (mintDetailBarSlotIdentity), just
		// like the row-detail host - so the sidecar's SERVER shape must be identical regardless of anchor.
		var h = Html.of(BarSlotTable.detailSidecar(bar()));
		assertTrue(h.contains(BarSlotTable.BAR_META_ATTR + "=\"dialog-ctx\""), h);
		assertFalse(h.contains("id=\"" + BarSlotTable.SIDECAR_ID_PREFIX), h);
		assertTrue(h.contains("\"contractVersion\":\"" + BarSlot.CONTRACT_VERSION + "\""), h);
		assertTrue(h.contains(BarSlotTable.BADGE_NS + ":open"), h);
	}
}
