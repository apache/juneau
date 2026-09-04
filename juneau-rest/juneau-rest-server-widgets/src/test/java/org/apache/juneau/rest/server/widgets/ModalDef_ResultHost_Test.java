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
 * Bean contract for the in-dialog <b>result host</b> opt-in ({@link ModalDef#keepOpenOnSubmit}), the dialog-scoped
 * <b>child-action catalog</b> ({@link ModalDef#childActions} / {@link ModalDef.ChildAction}), and the confirmation
 * field <b>display kind</b> ({@link ModalDef.Field#kind} / {@link ModalDef.Field#code(String,String)}).
 *
 * <p>
 * All three are additive to an already-served payload, so the three properties this pins hardest are the ones that
 * make that claim true rather than merely intended:
 * <ul class='spaced-list'>
 * 	<li><b>Appended, never interleaved.</b> The new keys land after the frozen prefix, in {@code @BeanType} order.
 * 	<li><b>Omitted when unset.</b> An un-opted-in modal's payload is byte-identical to one served before any of
 * 		this existed &mdash; which is what lets an older runtime keep reading it, and lets the golden fixtures in
 * 		{@code ModalDef_FormDef_Test} / {@code ModalDef_BarSlot_Test} stay untouched.
 * 	<li><b>No {@link ModalDef#CONTRACT_VERSION} bump.</b> Additive means the version does not move.
 * </ul>
 *
 * <p>
 * The remaining group is the fail-closed serve-time refusals: a payload that would produce a silently-wrong or
 * silently-empty client surface is rejected by {@link ModalDef#validate()} at serve time, where the author sees it,
 * rather than degrading quietly in a browser.
 */
class ModalDef_ResultHost_Test extends TestBase {

	private static FormDef form() {
		return FormDef.create().field(FormDef.Input.of("note", "Note", "text"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) keepOpenOnSubmit - the receipt opt-in
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_fluentSetter_storesTheFlag() {
		assertEquals(Boolean.TRUE, ModalDef.create("Delete?").keepOpenOnSubmit(true).keepOpenOnSubmit);
	}

	@Test void a02_falseClearsRatherThanSerializingFalse() {
		// Clearing, not storing FALSE: an un-opted-in payload must be byte-identical to one that never mentioned
		// the flag, or every existing golden fixture would have to grow a "keepOpenOnSubmit":false.
		assertNull(ModalDef.create("Delete?").keepOpenOnSubmit(true).keepOpenOnSubmit(false).keepOpenOnSubmit);
		assertFalse(Json.of(ModalDef.create("Delete?").keepOpenOnSubmit(false)).contains("keepOpenOnSubmit"));
	}

	@Test void a03_omittedWhenUnset() {
		var json = Json.of(ModalDef.create("Delete?").checked());
		assertFalse(json.contains("keepOpenOnSubmit"), () -> "unset flag leaked:\n" + json);
		assertFalse(json.contains("childActions"), () -> "unset catalog leaked:\n" + json);
	}

	@Test void a04_emittedWhenSet_andAppendedLast() {
		var m = ModalDef.create("Delete?").field("Id", "INC-1").form(form()).keepOpenOnSubmit(true).checked();
		Map<?,?> actual = Json.to(Json.of(m), Map.class);
		assertEquals(List.of("contractVersion", "title", "fields", "form", "keepOpenOnSubmit"),
			new ArrayList<>(actual.keySet()));
	}

	@Test void a05_noContractVersionBump() {
		// A form-bearing modal is the one that carries a stamped version at all (checked() clears it otherwise),
		// so it is the one that can show the version did not move.
		assertEquals("2", ModalDef.CONTRACT_VERSION);
		var json = Json.of(ModalDef.create("Delete?").form(form()).keepOpenOnSubmit(true).checked());
		assertTrue(json.contains("\"contractVersion\":\"2\""), json);
		assertTrue(json.contains("\"keepOpenOnSubmit\":true"), json);
	}

	@Test void a06_keepOpenOnSubmitAloneIsValid() {
		// The opt-in does not require a form: a confirm-only action can still host its own receipt.
		assertDoesNotThrow(() -> ModalDef.create("Delete?").keepOpenOnSubmit(true).validate());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Field.kind - the display kind on a confirmation field
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_kindOmittedWhenUnset() {
		var json = Json.of(ModalDef.create("Delete?").field("Id", "INC-1").checked());
		assertFalse(json.contains("kind"), () -> "unset kind leaked:\n" + json);
	}

	@Test void b02_codeFactorySetsTheKind() {
		var f = ModalDef.Field.code("Token", "abc-123");
		assertEquals("Token", f.label);
		assertEquals("abc-123", f.value);
		assertEquals(ModalDef.FIELD_KIND_CODE, f.kind);
	}

	@Test void b03_codeFieldOnTheModal_appendsKindLast() {
		var m = ModalDef.create("Done").codeField("Token", "abc-123").checked();
		var fields = (List<?>) Json.to(Json.of(m), Map.class).get("fields");
		assertEquals(List.of("label", "value", "kind"), new ArrayList<>(((Map<?,?>) fields.get(0)).keySet()));
	}

	@Test void b04_codeFactoryArgumentChecksTheLabel() {
		assertThrows(IllegalArgumentException.class, () -> ModalDef.Field.code(null, "v"));
		assertThrows(IllegalArgumentException.class, () -> ModalDef.Field.code("  ", "v"));
		// A null VALUE is legal (an absent value renders empty, it is not an authoring error).
		assertDoesNotThrow(() -> ModalDef.Field.code("Token", null));
	}

	@Test void b05_bothAllowlistTokensValidate() {
		for (var k : List.of(ModalDef.FIELD_KIND_TEXT, ModalDef.FIELD_KIND_CODE)) {
			var m = ModalDef.create("Done");
			m.field("L", "v");
			m.fields.get(0).kind = k;
			assertDoesNotThrow(m::validate, k);
		}
	}

	@Test void b06_offAllowlistKindIsRefusedAtServeTime() {
		// Fail closed where the author can see it.  The runtime ALSO falls back to `text` for an unknown token
		// rather than trusting it, but a served payload should never get that far.
		var m = ModalDef.create("Done");
		m.field("L", "v");
		m.fields.get(0).kind = "html";
		var e = assertThrows(IllegalArgumentException.class, m::validate);
		assertTrue(e.getMessage().contains("kind"), e::getMessage);
		assertTrue(e.getMessage().contains("html"), e::getMessage);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) childActions - the dialog-scoped catalog
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_factoryStoresIdAndLabel() {
		var c = ModalDef.ChildAction.of("review", "Review");
		assertEquals("review", c.id);
		assertEquals("Review", c.label);
		assertNull(c.form);
		assertNull(c.endpoint);
		assertNull(c.method);
		assertNull(c.onSuccess);
		assertNull(c.carryDrafts);
	}

	@Test void c02_factoryArgumentChecks() {
		assertThrows(IllegalArgumentException.class, () -> ModalDef.ChildAction.of(null, "L"));
		assertThrows(IllegalArgumentException.class, () -> ModalDef.ChildAction.of("  ", "L"));
		assertThrows(IllegalArgumentException.class, () -> ModalDef.ChildAction.of("id", null));
		assertThrows(IllegalArgumentException.class, () -> ModalDef.ChildAction.of("id", "  "));
	}

	@Test void c03_fluentSettersAndWireShape() {
		var m = ModalDef.create("Step 1")
			.form(form())
			.childAction(ModalDef.ChildAction.of("review", "Review")
				.form("/x/review-form").endpoint("/x/review").method("POST").onSuccess("redraw").carryDrafts(true))
			.checked();
		var child = (Map<?,?>) ((List<?>) Json.to(Json.of(m), Map.class).get("childActions")).get(0);
		assertEquals(List.of("id", "label", "form", "endpoint", "method", "onSuccess", "carryDrafts"),
			new ArrayList<>(child.keySet()));
		assertEquals("review", child.get("id"));
		assertEquals(Boolean.TRUE, child.get("carryDrafts"));
	}

	@Test void c04_childActionsAppendedAfterKeepOpenOnSubmit() {
		var m = ModalDef.create("Step 1")
			.form(form())
			.keepOpenOnSubmit(true)
			.childAction(ModalDef.ChildAction.of("review", "Review"))
			.checked();
		Map<?,?> actual = Json.to(Json.of(m), Map.class);
		assertEquals(List.of("contractVersion", "title", "form", "keepOpenOnSubmit", "childActions"),
			new ArrayList<>(actual.keySet()));
	}

	@Test void c05_unsetOptionalsOmittedFromAChild() {
		var m = ModalDef.create("Step 1").childAction(ModalDef.ChildAction.of("review", "Review")).checked();
		var child = (Map<?,?>) ((List<?>) Json.to(Json.of(m), Map.class).get("childActions")).get(0);
		assertEquals(List.of("id", "label"), new ArrayList<>(child.keySet()));
	}

	@Test void c06_carryDraftsFalseClearsRatherThanSerializingFalse() {
		var c = ModalDef.ChildAction.of("review", "Review").carryDrafts(true).carryDrafts(false);
		assertNull(c.carryDrafts);
	}

	@Test void c07_nullChildIsRefusedByTheAdder() {
		assertThrows(IllegalArgumentException.class, () -> ModalDef.create("Step 1").childAction(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) The fail-closed validate rules on the catalog
	//------------------------------------------------------------------------------------------------------------------

	// A raw list, bypassing the factory - the shape a bean-deserialized payload can actually arrive in.
	private static ModalDef withRawChildren(ModalDef.ChildAction... children) {
		var m = ModalDef.create("Step 1");
		m.childActions = new ArrayList<>(Arrays.asList(children));
		return m;
	}

	private static ModalDef.ChildAction raw(String id, String label) {
		var c = new ModalDef.ChildAction();
		c.id = id;
		c.label = label;
		return c;
	}

	@Test void d01_blankIdIsRefused() {
		var e = assertThrows(IllegalArgumentException.class, () -> withRawChildren(raw("  ", "Review")).validate());
		assertTrue(e.getMessage().contains("id"), e::getMessage);
		assertThrows(IllegalArgumentException.class, () -> withRawChildren(raw(null, "Review")).validate());
	}

	@Test void d02_blankLabelIsRefused() {
		// A child with no label paints a button with nothing on it - unclickable in practice, and invisible in a
		// screen reader.  Refused where the author can see it.
		var e = assertThrows(IllegalArgumentException.class, () -> withRawChildren(raw("review", "  ")).validate());
		assertTrue(e.getMessage().contains("label"), e::getMessage);
		assertThrows(IllegalArgumentException.class, () -> withRawChildren(raw("review", null)).validate());
	}

	@Test void d03_duplicateIdIsRefused() {
		// The resolver returns the FIRST match, so a duplicate id means one of the two declared children is
		// unreachable with no signal anywhere.
		var e = assertThrows(IllegalArgumentException.class,
			() -> withRawChildren(raw("review", "Review"), raw("review", "Review again")).validate());
		assertTrue(e.getMessage().contains("more than once"), e::getMessage);
	}

	@Test void d04_nullChildInARawListIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> withRawChildren((ModalDef.ChildAction) null).validate());
	}

	@Test void d05_carryDraftsOnAFormlessModalIsRefused() {
		// Nothing to collect FROM: the parent has no inputs, so the carry would always be an empty object.
		var m = ModalDef.create("Step 1")
			.childAction(ModalDef.ChildAction.of("review", "Review").form("/x/review-form").carryDrafts(true));
		var e = assertThrows(IllegalArgumentException.class, m::validate);
		assertTrue(e.getMessage().contains("carryDrafts"), e::getMessage);
		assertTrue(e.getMessage().contains("no form"), e::getMessage);
	}

	@Test void d06_carryDraftsOnAFormlessChildIsRefused() {
		// Nowhere to PUT them: a confirm-only child issues no form GET, so the query parameter has no request to
		// ride on and the drafts would vanish with no error.
		var m = ModalDef.create("Step 1")
			.form(form())
			.childAction(ModalDef.ChildAction.of("review", "Review").carryDrafts(true));
		var e = assertThrows(IllegalArgumentException.class, m::validate);
		assertTrue(e.getMessage().contains("carryDrafts"), e::getMessage);
		assertTrue(e.getMessage().contains("form URL"), e::getMessage);
	}

	@Test void d07_theLegalCarryDraftsShapePasses() {
		assertDoesNotThrow(() -> ModalDef.create("Step 1")
			.form(form())
			.childAction(ModalDef.ChildAction.of("review", "Review").form("/x/review-form").carryDrafts(true))
			.validate());
	}

	@Test void d08_aChildWithoutCarryDraftsNeedsNoForm() {
		assertDoesNotThrow(() -> ModalDef.create("Step 1")
			.childAction(ModalDef.ChildAction.of("review", "Review"))
			.validate());
	}
}
