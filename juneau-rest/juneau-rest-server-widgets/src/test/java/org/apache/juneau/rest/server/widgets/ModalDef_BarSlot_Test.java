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
package org.apache.juneau.rest.server.widgets;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Bean contract for the <b>third</b> named {@link BarSlot} attachment, {@link ModalDef#barSlot}.
 *
 * <p>
 * The type is shared with the rich-view module's page and row-detail hosts while the placement is not: this host is
 * a dialog title.  Unlike those two Java-only fields, this one travels the wire (the modal itself is the fetched
 * payload with no separate server-rendered pass to ride into), so this class pins the wire shape (additive, omitted
 * when unset, no {@link ModalDef#CONTRACT_VERSION} bump) in addition to the field/fluent-setter/validate-cascade
 * shape {@code RowDetailDef_BarSlot_Test} pins for the second host.
 */
class ModalDef_BarSlot_Test extends TestBase {

	private static BarSlot bar(String id) {
		return BarSlot.create(id).widgets(BarBadge.of("open").label("Open").badge(Badge.count(3)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Field + fluent setter + validate cascade
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_fluentSetter_storesSlot() {
		var m = ModalDef.create("Really delete?").barSlot(bar("dialog-bar"));
		assertNotNull(m.barSlot);
		assertEquals("dialog-bar", m.barSlot.id);
	}

	@Test void a02_nullSlot_isLegal() {
		assertDoesNotThrow(() -> ModalDef.create("Really delete?").validate());
	}

	@Test void a03_validSlot_passes() {
		assertDoesNotThrow(() -> ModalDef.create("Really delete?").barSlot(bar("dialog-bar")).validate());
	}

	@Test void a04_cascadesIntoBarSlotValidate_blankId() {
		var m = ModalDef.create("Really delete?").barSlot(BarSlot.create(" ").widgets(BarText.of("x", "X")));
		var e = assertThrows(IllegalArgumentException.class, m::validate);
		assertTrue(e.getMessage().contains("BarSlot"), e::getMessage);
	}

	@Test void a05_cascadesIntoBarSlotValidate_noWidgets() {
		var m = ModalDef.create("Really delete?").barSlot(BarSlot.create("dialog-bar"));
		var e = assertThrows(IllegalArgumentException.class, m::validate);
		assertTrue(e.getMessage().contains("BarSlot"), e::getMessage);
	}

	@Test void a06_cascadesIntoBarSlotValidate_badRefreshUrl() {
		var m = ModalDef.create("Really delete?").barSlot(bar("dialog-bar").refreshUrl("http://evil.example/counts"));
		var e = assertThrows(IllegalArgumentException.class, m::validate);
		assertTrue(e.getMessage().contains("BarSlot"), e::getMessage);
	}

	@Test void a07_checkedCascadesToBarSlotValidateToo() {
		// The serving-path hook validates the whole modal, including a malformed dialog bar slot.
		var m = ModalDef.create("Really delete?").barSlot(BarSlot.create("dialog-bar"));
		assertThrows(IllegalArgumentException.class, m::checked);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Wire shape: additive, omitted when unset, no CONTRACT_VERSION bump
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_barSlotIsAdditiveOnTheWire() {
		var json = Json.of(ModalDef.create("Really delete?").barSlot(bar("dialog-bar")));
		var expected = Json.to("""
			{"title":"Really delete?",
			 "barSlot":{"contractVersion":"1","id":"dialog-bar",
			            "widgets":[{"id":"open","label":"Open","badge":{"count":3}}]}}
			""", Map.class);
		assertEquals(expected, Json.to(json, Map.class), json);
	}

	@Test void b02_confirmOnlyModal_withNoBarSlot_omitsIt() {
		var json = Json.of(ModalDef.create("Really delete?"));
		assertFalse(json.contains("barSlot"), json);
	}

	@Test void b03_barSlotRidesAlongsideAFormBearingModal() {
		var modal = ModalDef.create("Acknowledge this incident?")
			.field("Incident", "INC-42")
			.form(FormDef.ofTemplate("servlet:/incidents/ack-form.ftl"))
			.barSlot(bar("dialog-bar"))
			.checked();
		var json = Json.of(modal);
		assertTrue(json.contains("\"barSlot\""), json);
		assertTrue(json.contains("\"contractVersion\":\"" + ModalDef.CONTRACT_VERSION + "\""), json);
	}

	@Test void b04_topLevelKeyOrder_barSlotIsLast() {
		var modal = ModalDef.create("t").field("A", "1").form(FormDef.ofTemplate("u")).idempotencyKey("k")
			.barSlot(bar("dialog-bar")).checked();
		Map<?,?> actual = Json.to(Json.of(modal), Map.class);
		assertEquals(List.of("contractVersion", "title", "fields", "form", "idempotencyKey", "barSlot"),
			new ArrayList<>(actual.keySet()));
	}

	@Test void c01_modalContractVersionUnchanged() {
		// Adding a third wire-carried, additive-only field does not bump the modal's own contract version - the
		// version guards the fail-loud form handshake, not this orthogonal, additive attachment.
		assertEquals("2", ModalDef.CONTRACT_VERSION);
	}

	@Test void c02_barSlotContractVersionUnchanged() {
		assertEquals("1", BarSlot.CONTRACT_VERSION);
	}
}
