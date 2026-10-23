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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.widgets.FormDef.*;
import org.apache.juneau.rest.server.widgets.FormDef.Input.*;
import org.junit.jupiter.api.*;

/**
 * {@link FormDef}, {@link Input}, {@link Option} and {@link Section} builder and fail-closed validation branches
 * not already exercised through the serialized {@link ModalDef} shapes.
 */
class FormDef_Test extends TestBase {

	private static void assertRejected(Runnable r, String fragment) {
		var e = assertThrows(IllegalArgumentException.class, r::run);
		assertTrue(e.getMessage().contains(fragment), () -> "expected '" + fragment + "' in: " + e.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Input builders
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_input_nullOrBlankType_defaultsToText() {
		assertEquals("text", Input.of("n", "N", null).type);
		assertEquals("text", Input.of("n", "N", "  ").type);
	}

	@Test void a02_input_required_flag() {
		assertEquals(Boolean.TRUE, Input.of("n", "N", "text").required().required);
		assertEquals(Boolean.TRUE, Input.of("n", "N", "text").required(true).required);
		assertNull(Input.of("n", "N", "text").required().required(false).required);
	}

	@Test void a03_input_simpleSetters() {
		var i = Input.of("n", "N", "text").value("v").pattern("[a-z]+").maxLength(12).help("hint").actionId("act");
		assertBean(i, "name,label,type,value,pattern,maxLength,help,actionId", "n,N,text,v,[a-z]+,12,hint,act");
	}

	@Test void a04_options_replacesExistingAndRejectsNulls() {
		var i = Input.of("n", "N", "select").option("a", "A");
		i.options(Option.of("b", "B"), Option.of("c", "C"));
		assertSize(2, i.options);
		assertEquals("b", i.options.get(0).value);
		assertRejected(() -> i.options((Option[]) null), "options must not be null");
		assertRejected(() -> i.options(Option.of("x", "X"), null), "option must not be null");
	}

	@Test void a05_option_createsListLazily() {
		var i = Input.of("n", "N", "select");
		assertNull(i.options);
		i.option("a", "A").option("b", "B");
		assertSize(2, i.options);
	}

	@Test void a06_action_nullRef_rejected() {
		assertRejected(() -> Input.of("n", "N", "action").action(null), "action must not be null");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Input validation
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_validate_blankName_rejected() {
		var i = Input.of("n", "N", "text");
		i.name = "  ";
		assertRejected(i::validate, "name must not be null or blank");
		i.name = null;
		assertRejected(i::validate, "name must not be null or blank");
	}

	@Test void b02_validate_typeNullOrBlank_treatedAsText() {
		var i = Input.of("n", "N", "text");
		i.type = null;
		i.validate();
		i.type = " ";
		i.validate();
	}

	@Test void b03_validate_unknownType_rejected() {
		var i = Input.of("n", "N", "text");
		i.type = "radio";
		assertRejected(i::validate, "type must be one of");
	}

	@Test void b03a_validate_passwordType_accepted() {
		var i = Input.of("pw", "Password", "password");
		i.validate();
		assertEquals("password", i.type);
	}

	@Test void b03b_validate_passwordWithValue_rejected() {
		var i = Input.of("pw", "Password", "password").value("hunter2");
		assertRejected(i::validate, "value is not allowed on a password field");
		i.value = null;
		i.validate();
	}

	@Test void b04_validate_select_nullOrEmptyOptions_rejected() {
		var i = Input.of("n", "N", "select");
		assertRejected(i::validate, "at least one option");
		i.options = new ArrayList<>();
		assertRejected(i::validate, "at least one option");
	}

	@Test void b05_validate_select_badOptionEntries_rejected() {
		var i = Input.of("n", "N", "select");
		i.options = new ArrayList<>();
		i.options.add(null);
		assertRejected(i::validate, "option must not be null");
		var noValue = new Option();
		noValue.label = "L";
		i.options.set(0, noValue);
		assertRejected(i::validate, "option value must not be null");
		var noLabel = new Option();
		noLabel.value = "v";
		i.options.set(0, noLabel);
		assertRejected(i::validate, "option label must not be null or blank");
		noLabel.label = "  ";
		assertRejected(i::validate, "option label must not be null or blank");
	}

	@Test void b06_validate_select_valueMustMatchAnOption() {
		var i = Input.of("n", "N", "select").option("a", "A").value("a");
		i.validate();
		i.value("zzz");
		assertRejected(i::validate, "does not match any option");
	}

	@Test void b07_validate_optionsOnNonSelect_rejected() {
		var i = Input.of("n", "N", "text").option("a", "A");
		assertRejected(i::validate, "only allowed on a select");
	}

	@Test void b08_validate_patternAndMaxLength_onlyOnTextual() {
		for (var t : new String[]{"checkbox", "toggle"}) {
			assertRejected(Input.of("n", "N", t).pattern("a")::validate, "pattern is only allowed");
			assertRejected(Input.of("n", "N", t).maxLength(4)::validate, "maxLength is only allowed");
		}
		Input.of("n", "N", "textarea").pattern("a").maxLength(4).validate();
		Input.of("n", "N", "text").pattern("a").maxLength(4).validate();
	}

	@Test void b09_validate_patternChecks() {
		assertRejected(Input.of("n", "N", "text").pattern("(")::validate, "does not compile");
		assertRejected(Input.of("n", "N", "text").pattern("a".repeat(FormDef.PATTERN_MAX_LENGTH + 1))::validate, "at most");
		Input.of("n", "N", "text").pattern("a".repeat(FormDef.PATTERN_MAX_LENGTH)).validate();
		assertRejected(Input.of("n", "N", "text").maxLength(0)::validate, "maxLength must be > 0");
	}

	@Test void b10_validate_actionField() {
		assertRejected(Input.of("n", "N", "action")::validate, "must declare an actionId");
		assertRejected(Input.of("n", "N", "action").actionId("  ")::validate, "must declare an actionId");
		Input.of("n", "N", "action").action(ActionRef.of("approve")).validate();
		assertRejected(Input.of("n", "N", "text").actionId("x")::validate, "actionId is only allowed");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Section and form
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_section_field_nullRejected_andListCreatedLazily() {
		var s = Section.of("s", "S");
		assertNull(s.fields);
		assertRejected(() -> s.field(null), "field must not be null");
		s.field(Input.of("a", "A", "text")).field(Input.of("b", "B", "text"));
		assertSize(2, s.fields);
	}

	@Test void c02_section_validate_rejections() {
		var s = Section.of("s", "S").field(Input.of("a", "A", "text"));
		s.validate();
		s.fields.add(null);
		assertRejected(s::validate, "field must not be null");
		var noFields = Section.of("s", "S");
		assertRejected(noFields::validate, "at least one field");
		noFields.fields = new ArrayList<>();
		assertRejected(noFields::validate, "at least one field");
		var blankLabel = Section.of("s", "S").field(Input.of("a", "A", "text"));
		blankLabel.label = " ";
		assertRejected(blankLabel::validate, "label must not be null or blank");
		var blankId = Section.of("s", "S").field(Input.of("a", "A", "text"));
		blankId.id = null;
		assertRejected(blankId::validate, "id must not be null or blank");
	}

	@Test void c03_form_nullFieldAndSectionRejected() {
		assertRejected(() -> FormDef.create().field(null), "field must not be null");
		assertRejected(() -> FormDef.create().section(null), "section must not be null");
	}

	@Test void c04_form_blankTemplate_rejected_andOfTemplate() {
		assertRejected(() -> FormDef.create().template(null), "template must not be null or blank");
		assertRejected(() -> FormDef.ofTemplate(" "), "template must not be null or blank");
		assertEquals("t.ftl", FormDef.ofTemplate("t.ftl").template);
	}

	@Test void c05_form_validate_nullEntriesInDirectlyAssignedLists() {
		var flat = FormDef.create().field(Input.of("a", "A", "text"));
		flat.fields.add(null);
		assertRejected(flat::validate, "field must not be null");

		var sectioned = FormDef.create().section(Section.of("s", "S").field(Input.of("a", "A", "text")));
		sectioned.sections.add(null);
		assertRejected(sectioned::validate, "section must not be null");
	}

	@Test void c06_form_flatFieldWithoutFieldsOrSections_validates() {
		FormDef.create().validate();
	}

	@Test void c07_form_sectionedDuplicateFieldNameInOneSection_rejected() {
		var f = FormDef.create().section(Section.of("s", "S").field(Input.of("a", "A", "text")).field(Input.of("a", "A2", "text")));
		assertRejected(f::validate, "duplicate field name");
	}

	@Test void c08_section_validate_blankIdAndNullLabel_rejected() {
		var blankId = Section.of("s", "S").field(Input.of("a", "A", "text"));
		blankId.id = "  ";
		assertRejected(blankId::validate, "id must not be null or blank");
		var nullLabel = Section.of("s", "S").field(Input.of("a", "A", "text"));
		nullLabel.label = null;
		assertRejected(nullLabel::validate, "label must not be null or blank");
	}
}
