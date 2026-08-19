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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Contract test for the declarative modal ({@link ModalDef}) + form ({@link FormDef}) payload the modal-open
 * confirmation fetch returns (design doc §6.2; the modal/form half of {@code TODO-416}).
 *
 * <p>
 * The confirmation body is typed structured fields painted client-side with {@code textContent} (never
 * {@code innerHTML}); this test pins that field shape and the FreeMarker-first form source, plus the omit-when-unset
 * rule for the optional form and idempotency key.
 */
class ModalDef_FormDef_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// ModalDef wire shape
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_fullModal_serializesToFrozenShape() {
		var modal = ModalDef.create("Acknowledge this incident?")
			.field("Incident", "INC-42")
			.field("Title", "API latency")
			.field("Service", "gateway")
			.field("Current status", "triggered")
			.form(FormDef.ofTemplate("servlet:/incidents/ack-form.ftl"))
			.idempotencyKey("deadbeef");
		var json = Json.of(modal);
		var expected = Json.to("""
			{"title":"Acknowledge this incident?",
			 "fields":[{"label":"Incident","value":"INC-42"},{"label":"Title","value":"API latency"},
			           {"label":"Service","value":"gateway"},{"label":"Current status","value":"triggered"}],
			 "form":{"template":"servlet:/incidents/ack-form.ftl"},
			 "idempotencyKey":"deadbeef"}
			""", Map.class);
		assertEquals(expected, Json.to(json, Map.class), json);
	}

	@Test void a02_topLevelKeyOrder() {
		var modal = ModalDef.create("t").field("A", "1").form(FormDef.ofTemplate("u")).idempotencyKey("k");
		Map<?,?> actual = Json.to(Json.of(modal), Map.class);
		assertEquals(List.of("title", "fields", "form", "idempotencyKey"), new ArrayList<>(actual.keySet()));
	}

	@Test void a03_confirmOnlyModal_omitsFieldsFormAndKey() {
		// A bare confirm-only modal (no fields, no form, no key) - every optional field omitted, not null.
		var json = Json.of(ModalDef.create("Really delete?"));
		assertEquals(Json.to("{\"title\":\"Really delete?\"}", Map.class), Json.to(json, Map.class), json);
		for (var k : List.of("fields", "form", "idempotencyKey"))
			assertFalse(json.contains("\"" + k + "\""), () -> "unset field leaked: " + k + "\n" + json);
	}

	@Test void a04_fieldNullValue_serializesLabelOnly() {
		// A field value may be null (rendered as an empty value client-side); the label is required.
		var json = Json.of(ModalDef.create("t").field("Note", null));
		assertTrue(json.contains("\"label\":\"Note\""), json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Validation guards
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_createBlankTitleThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ModalDef.create("  "));
		assertTrue(e.getMessage().contains("blank"), e::getMessage);
		assertThrows(IllegalArgumentException.class, () -> ModalDef.create(null));
	}

	@Test void b02_fieldBlankLabelThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ModalDef.Field.of("  ", "v"));
		assertTrue(e.getMessage().contains("blank"), e::getMessage);
		assertThrows(IllegalArgumentException.class, () -> ModalDef.Field.of(null, "v"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// FormDef (FreeMarker-first)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_formDef_serializesTemplateOnly() {
		var json = Json.of(FormDef.ofTemplate("servlet:/x/form.ftl"));
		assertEquals(Json.to("{\"template\":\"servlet:/x/form.ftl\"}", Map.class), Json.to(json, Map.class), json);
	}

	@Test void c02_formDef_reservedBeanTypeIsNotOnTheWireYet() {
		// The bean->form generator is a deferred follow-on; no "beanType" key leaks in the MVP.
		assertFalse(Json.of(FormDef.ofTemplate("u")).contains("beanType"));
	}

	@Test void c03_formDef_blankTemplateThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> FormDef.ofTemplate("  "));
		assertTrue(e.getMessage().contains("blank"), e::getMessage);
		assertThrows(IllegalArgumentException.class, () -> FormDef.ofTemplate(null));
	}
}
