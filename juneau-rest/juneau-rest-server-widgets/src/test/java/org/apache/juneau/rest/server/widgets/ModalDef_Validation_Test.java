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
import org.junit.jupiter.api.*;

/**
 * {@link ModalDef#validate()} fail-closed branches: fields, child-action catalog, nested form and bar slot.
 */
class ModalDef_Validation_Test extends TestBase {

	private static void assertRejected(Runnable r, String fragment) {
		var e = assertThrows(IllegalArgumentException.class, r::run);
		assertTrue(e.getMessage().contains(fragment), () -> "expected '" + fragment + "' in: " + e.getMessage());
	}

	@Test void a01_title_blank_rejected() {
		var blank = ModalDef.create("T");
		blank.title = " ";
		assertRejected(blank::validate, "title must not be null or blank");
		var m = ModalDef.create("T");
		m.title = null;
		assertRejected(m::validate, "title must not be null or blank");
	}

	@Test void a02_field_nullBlankLabelAndBadKind_rejected() {
		var m = ModalDef.create("T").field("A", "1");
		m.fields.add(null);
		assertRejected(m::validate, "field must not be null");

		var blank = ModalDef.create("T");
		blank.fields = new ArrayList<>();
		var f = ModalDef.Field.of("A", "1");
		f.label = " ";
		blank.fields.add(f);
		assertRejected(blank::validate, "label must not be null or blank");

		var badKind = ModalDef.create("T").field("A", "1");
		badKind.fields.get(0).kind = "html";
		assertRejected(badKind::validate, "kind must be");
	}

	@Test void a03_field_textCodeAndUnsetKind_accepted() {
		var m = ModalDef.create("T").field("A", "1").codeField("B", "2");
		m.fields.add(ModalDef.Field.of("C", null));
		m.fields.get(2).kind = null;
		m.validate();
	}

	@Test void a04_codeField_appendsToExistingFields() {
		var m = ModalDef.create("T").field("A", "1").codeField("B", "2");
		assertEquals(2, m.fields.size());
		assertEquals(ModalDef.FIELD_KIND_CODE, m.fields.get(1).kind);
	}

	@Test void a05_field_nullLabel_rejected() {
		var m = ModalDef.create("T").field("A", "1");
		m.fields.get(0).label = null;
		assertRejected(m::validate, "label must not be null or blank");
	}

	@Test void b01_childAction_null_rejected() {
		assertRejected(() -> ModalDef.create("T").childAction(null), "ChildAction must not be null");
	}

	@Test void b02_childAction_accumulates_andDuplicateIdRejected() {
		var m = ModalDef.create("T")
			.childAction(ModalDef.ChildAction.of("a", "A"))
			.childAction(ModalDef.ChildAction.of("b", "B"));
		assertEquals(2, m.childActions.size());
		m.validate();
		m.childAction(ModalDef.ChildAction.of("a", "Again"));
		assertRejected(m::validate, "more than once");
	}

	@Test void b03_childAction_nullBlankIdOrLabel_rejected() {
		var nul = ModalDef.create("T").childAction(ModalDef.ChildAction.of("a", "A"));
		nul.childActions.add(null);
		assertRejected(nul::validate, "ChildAction must not be null");

		var noId = ModalDef.create("T").childAction(ModalDef.ChildAction.of("a", "A"));
		noId.childActions.get(0).id = " ";
		assertRejected(noId::validate, "id must not be null or blank");

		var noLabel = ModalDef.create("T").childAction(ModalDef.ChildAction.of("a", "A"));
		noLabel.childActions.get(0).label = null;
		assertRejected(noLabel::validate, "label must not be null or blank");
	}

	@Test void b04_carryDrafts_needsModalFormAndChildFormUrl() {
		var noModalForm = ModalDef.create("T")
			.childAction(ModalDef.ChildAction.of("a", "A").form("/f").carryDrafts(true));
		assertRejected(noModalForm::validate, "no form");

		var noChildForm = ModalDef.create("T").form(FormDef.create().field(FormDef.Input.of("n", "N", "text")))
			.childAction(ModalDef.ChildAction.of("a", "A").carryDrafts(true));
		assertRejected(noChildForm::validate, "no form URL");

		var blankChildForm = ModalDef.create("T").form(FormDef.create().field(FormDef.Input.of("n", "N", "text")))
			.childAction(ModalDef.ChildAction.of("a", "A").form(" ").carryDrafts(true));
		assertRejected(blankChildForm::validate, "no form URL");

		ModalDef.create("T").form(FormDef.create().field(FormDef.Input.of("n", "N", "text")))
			.childAction(ModalDef.ChildAction.of("a", "A").form("/f").carryDrafts(true)).validate();
	}

	@Test void c01_nestedForm_isValidated() {
		var m = ModalDef.create("T").form(FormDef.create().field(FormDef.Input.of("n", "N", "text")).field(FormDef.Input.of("n", "N2", "text")));
		assertRejected(m::validate, "duplicate field name");
	}

	@Test void c02_barSlot_setter_andNestedValidation() {
		var good = ModalDef.create("T").barSlot(BarSlot.create("b").widgets(BarText.of("t", "x")));
		assertNotNull(good.barSlot);
		good.validate();
		var bad = ModalDef.create("T").barSlot(BarSlot.create("b"));
		assertRejected(bad::validate, "at least one widget");
		assertNull(ModalDef.create("T").barSlot(null).barSlot);
	}
}
