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
 * Contract test for the declarative modal ({@link ModalDef}) + form ({@link FormDef}) payload the modal-open
 * confirmation fetch returns (design doc §6.2). This pins the bean half of that contract; the runtime half is
 * pinned separately in the views module.
 *
 * <p>
 * The confirmation body is typed fields painted with textContent (never innerHTML); this test pins that field
 * shape, the typed FormDef inputs the client paints (never template markup), plus the omit-when-unset rule for
 * the optional form and idempotency key.
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
		// A form-bearing modal run through checked() stamps contractVersion FIRST on both beans.
		var modal = ModalDef.create("t").field("A", "1").form(FormDef.ofTemplate("u")).idempotencyKey("k").checked();
		Map<?,?> actual = Json.to(Json.of(modal), Map.class);
		assertEquals(List.of("contractVersion", "title", "fields", "form", "idempotencyKey"), new ArrayList<>(actual.keySet()));
		assertEquals(ModalDef.CONTRACT_VERSION, actual.get("contractVersion"));
	}

	@Test void a03_confirmOnlyModal_omitsFieldsFormAndKey() {
		// A bare confirm-only modal (no fields, no form, no key) - every optional field omitted, not null.
		var json = Json.of(ModalDef.create("Really delete?"));
		assertEquals(Json.to("{\"title\":\"Really delete?\"}", Map.class), Json.to(json, Map.class), json);
		for (var k : List.of("fields", "form", "idempotencyKey", "contractVersion"))
			assertFalse(json.contains("\"" + k + "\""), () -> "unset field leaked: " + k + "\n" + json);
	}

	@Test void a05_confirmOnlyChecked_staysUnversioned() {
		// A confirm-only modal (no form) stays unversioned even after checked() - contractVersion is not on the wire.
		var modal = ModalDef.create("Really delete?").field("Note", "gone").checked();
		assertNull(modal.contractVersion);
		var json = Json.of(modal);
		assertFalse(json.contains("contractVersion"), json);
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

	@Test void c04_formDef_serializesTypedInputs() {
		var form = FormDef.create()
			.field(FormDef.Input.of("resolution", "Resolution comment", "textarea").required().value("done"));
		var json = Json.of(form);
		var expected = Json.to("""
			{"fields":[{"name":"resolution","label":"Resolution comment","type":"textarea","required":true,"value":"done"}]}
			""", Map.class);
		assertEquals(expected, Json.to(json, Map.class), json);
		assertFalse(json.contains("\"template\""), json);
	}

	@Test void c05_formDef_textDefaultAndOptionalPrefillOmitted() {
		var json = Json.of(FormDef.create().field(FormDef.Input.of("note", "Note", null)));
		assertTrue(json.contains("\"type\":\"text\""), json);
		assertFalse(json.contains("\"required\""), json);
		assertFalse(json.contains("\"value\""), json);
	}

	@Test void c06_formDef_inputBlankNameOrLabelThrows() {
		assertThrows(IllegalArgumentException.class, () -> FormDef.Input.of("  ", "L", "text"));
		assertThrows(IllegalArgumentException.class, () -> FormDef.Input.of(null, "L", "text"));
		assertThrows(IllegalArgumentException.class, () -> FormDef.Input.of("n", "  ", "text"));
		assertThrows(IllegalArgumentException.class, () -> FormDef.Input.of("n", null, "text"));
	}

	@Test void c07_formDef_unknownTypeThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> FormDef.Input.of("n", "L", "password"));
		assertTrue(e.getMessage().contains("text"), e::getMessage);
		assertThrows(IllegalArgumentException.class, () -> FormDef.Input.of("n", "L", "<img>"));
	}

	@Test void c08_formDef_nullFieldThrows() {
		var form = FormDef.create();
		assertThrows(IllegalArgumentException.class, () -> form.field(null));
	}

	@Test void c09_formDef_templateAndFieldsTogether() {
		var json = Json.of(FormDef.ofTemplate("servlet:/x.ftl")
			.field(FormDef.Input.of("resolution", "Resolution", "textarea")));
		assertTrue(json.contains("\"template\":\"servlet:/x.ftl\""), json);
		assertTrue(json.contains("\"name\":\"resolution\""), json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) v1 control vocabulary: checkbox / toggle / select / action
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_eachNewTypeBuilds() {
		// All six allowlisted types build and validate.
		var form = FormDef.create()
			.field(FormDef.Input.of("t", "Text", "text"))
			.field(FormDef.Input.of("ta", "Textarea", "textarea"))
			.field(FormDef.Input.of("cb", "Checkbox", "checkbox"))
			.field(FormDef.Input.of("tg", "Toggle", "toggle"))
			.field(FormDef.Input.of("sel", "Select", "select").option("a", "A").option("b", "B"))
			.field(FormDef.Input.of("act", "Do it", "action").actionId("ack"));
		assertDoesNotThrow(form::validate);
	}

	@Test void d02_selectRequiresAtLeastOneOption() {
		var form = FormDef.create().field(FormDef.Input.of("sel", "Select", "select"));
		var e = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(e.getMessage().contains("option"), e::getMessage);
	}

	@Test void d03_optionBlankLabelOrNullValueThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> FormDef.Input.Option.of("v", "  "));
		assertTrue(e.getMessage().contains("blank"), e::getMessage);
		assertThrows(IllegalArgumentException.class, () -> FormDef.Input.Option.of("v", null));
		assertThrows(IllegalArgumentException.class, () -> FormDef.Input.Option.of(null, "L"));
	}

	@Test void d04_nonSelectRejectsOptions() {
		var form = FormDef.create().field(FormDef.Input.of("t", "Text", "text").option("a", "A"));
		var e = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(e.getMessage().contains("select"), e::getMessage);
	}

	@Test void d05_patternThatDoesNotCompileThrows() {
		var form = FormDef.create().field(FormDef.Input.of("t", "Text", "text").pattern("(unclosed"));
		var eV = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(eV.getMessage().contains("compile"), eV::getMessage);
		// The serving-path hook surfaces the same failure.
		var form2 = FormDef.create().field(FormDef.Input.of("t", "Text", "text").pattern("(unclosed"));
		assertThrows(IllegalArgumentException.class, form2::checked);
	}

	@Test void d06_patternTooLongThrows() {
		var pat = "a".repeat(FormDef.PATTERN_MAX_LENGTH + 1);
		var form = FormDef.create().field(FormDef.Input.of("t", "Text", "text").pattern(pat));
		var e = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(e.getMessage().contains("characters"), e::getMessage);
	}

	@Test void d07_maxLengthNonPositiveThrows() {
		var form = FormDef.create().field(FormDef.Input.of("t", "Text", "text").maxLength(0));
		var e = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(e.getMessage().contains("maxLength"), e::getMessage);
	}

	@Test void d08_patternOrMaxLengthOnNonTextThrows() {
		for (var type : List.of("checkbox", "toggle", "select", "action")) {
			var withPattern = FormDef.create().field(baseFor(type).pattern("x"));
			assertThrows(IllegalArgumentException.class, withPattern::validate, () -> "pattern on " + type);
			var withMax = FormDef.create().field(baseFor(type).maxLength(5));
			assertThrows(IllegalArgumentException.class, withMax::validate, () -> "maxLength on " + type);
		}
	}

	@Test void d09_duplicateFieldNameThrows() {
		var form = FormDef.create()
			.field(FormDef.Input.of("dup", "One", "text"))
			.field(FormDef.Input.of("dup", "Two", "text"));
		var e = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(e.getMessage().contains("duplicate"), e::getMessage);
	}

	@Test void d10_selectValueNotMatchingOptionThrows() {
		var form = FormDef.create()
			.field(FormDef.Input.of("sel", "Select", "select").option("a", "A").option("b", "B").value("z"));
		var e = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(e.getMessage().contains("does not match"), e::getMessage);
	}

	@Test void d11_actionRequiresActionId() {
		var form = FormDef.create().field(FormDef.Input.of("act", "Do it", "action"));
		var e = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(e.getMessage().contains("actionId"), e::getMessage);
		// A non-action field with an actionId is rejected too.
		var form2 = FormDef.create().field(FormDef.Input.of("t", "Text", "text").actionId("ack"));
		assertThrows(IllegalArgumentException.class, form2::validate);
	}

	@Test void d12_actionFromActionRefStoresId() {
		var form = FormDef.create().field(FormDef.Input.of("act", "Do it", "action").action(ActionRef.of("ack")));
		assertDoesNotThrow(form::validate);
		assertTrue(Json.of(form).contains("\"actionId\":\"ack\""), Json.of(form));
	}

	@Test void d13_selectWireShape() {
		var json = Json.of(FormDef.create()
			.field(FormDef.Input.of("sev", "Severity", "select").options(
				FormDef.Input.Option.of("p1", "P1"), FormDef.Input.Option.of("p2", "P2")).value("p1")));
		var expected = Json.to("""
			{"fields":[{"name":"sev","label":"Severity","type":"select","value":"p1",
			            "options":[{"value":"p1","label":"P1"},{"value":"p2","label":"P2"}]}]}
			""", Map.class);
		assertEquals(expected, Json.to(json, Map.class), json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Per-widget contract version + fail-loud handshake (h5)
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * The two bean halves of the three-way lockstep, pinned to the literal.  The runtime's baked-in third is pinned
	 * against these in the views module's {@code ViewsJs_ContractVersion_Test}; bumping any two of the three makes
	 * every form-bearing dialog refuse to open, so the literal is asserted here rather than compared to itself.
	 */
	@Test void e01_contractVersionConstantsAreTwo() {
		assertEquals("2", FormDef.CONTRACT_VERSION);
		assertEquals("2", ModalDef.CONTRACT_VERSION);
	}

	@Test void e02_rawFormDefLeaksNoVersion() {
		// A raw builder never leaks a version on the nested form until checked() is called on the serving path.
		var form = FormDef.create().field(FormDef.Input.of("r", "R", "textarea"));
		assertNull(form.contractVersion);
		assertFalse(Json.of(form).contains("contractVersion"), Json.of(form));
	}

	@Test void e03_checkedStampsBothVersionsFirst() {
		var modal = ModalDef.create("t")
			.form(FormDef.create().field(FormDef.Input.of("r", "R", "textarea"))).checked();
		assertEquals(ModalDef.CONTRACT_VERSION, modal.contractVersion);
		assertEquals(FormDef.CONTRACT_VERSION, modal.form.contractVersion);
		var json = Json.of(modal);
		assertTrue(json.contains("\"contractVersion\":\"" + ModalDef.CONTRACT_VERSION + "\""), json);
		// The nested form carries its own version too.
		assertEquals(FormDef.CONTRACT_VERSION, Json.to(Json.of(modal.form), Map.class).get("contractVersion"));
	}

	@Test void e04_validateDoesNotRequireVersionSet() {
		// A raw-built form-bearing modal that validates directly must NOT false-refuse on a null version.
		var modal = ModalDef.create("t").form(FormDef.create().field(FormDef.Input.of("r", "R", "textarea")));
		assertNull(modal.contractVersion);
		assertDoesNotThrow(modal::validate);
	}

	@Test void e05_checkedRejectsMalformedFormAtServeTime() {
		// The serving-path hook fails on a bad form/modal - not silently on the wire.
		var badForm = ModalDef.create("t").form(FormDef.create().field(FormDef.Input.of("sel", "S", "select")));
		assertThrows(IllegalArgumentException.class, badForm::checked);
		var badTitle = new ModalDef();
		assertThrows(IllegalArgumentException.class, badTitle::checked);
	}

	@Test void e06_backCompatAllTextFormValidates() {
		var form = FormDef.create()
			.field(FormDef.Input.of("a", "A", "text"))
			.field(FormDef.Input.of("b", "B", "textarea"));
		assertDoesNotThrow(form::checked);
	}

	@Test void e07_templateOnlyFormValidatesAndChecks() {
		// A fieldless / template-only form is a shipped shape; validate() over empty fields is a no-op.
		var form = FormDef.ofTemplate("servlet:/x.ftl");
		assertDoesNotThrow(form::validate);
		assertEquals(FormDef.CONTRACT_VERSION, form.checked().contractVersion);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Sectioned forms: an ordered list of labelled field groups instead of one flat list
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_sectionsAlone_validatesAndChecks() {
		var form = FormDef.create()
			.section(FormDef.Section.of("basics", "Basics").field(FormDef.Input.of("name", "Name", "text")))
			.section(FormDef.Section.of("advanced", "Advanced").field(FormDef.Input.of("notes", "Notes", "textarea")));
		assertDoesNotThrow(form::validate);
		assertEquals(FormDef.CONTRACT_VERSION, form.checked().contractVersion);
	}

	@Test void f02_flatFieldsAlone_stillValidates() {
		// The flat list IS the single-section case and stays a first-class shape.
		var form = FormDef.create().field(FormDef.Input.of("name", "Name", "text"));
		assertDoesNotThrow(form::validate);
		assertNull(form.sections);
	}

	@Test void f03_bothFieldsAndSections_areRejected() {
		var form = FormDef.create()
			.field(FormDef.Input.of("name", "Name", "text"))
			.section(FormDef.Section.of("basics", "Basics").field(FormDef.Input.of("notes", "Notes", "textarea")));
		var e = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(e.getMessage().contains("mutually exclusive"), e.getMessage());
		// checked() reaches the same rejection, so a serving path cannot emit the ambiguous shape either.
		assertThrows(IllegalArgumentException.class, form::checked);
	}

	@Test void f04_emptySectionsList_isRejected() {
		var form = FormDef.create();
		form.sections = new ArrayList<>();
		assertThrows(IllegalArgumentException.class, form::validate);
	}

	@Test void f05_duplicateSectionId_isRejected() {
		var form = FormDef.create()
			.section(FormDef.Section.of("dup", "First").field(FormDef.Input.of("a", "A", "text")))
			.section(FormDef.Section.of("dup", "Second").field(FormDef.Input.of("b", "B", "text")));
		var e = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(e.getMessage().contains("duplicate section id"), e.getMessage());
	}

	@Test void f06_fieldlessSection_isRejected() {
		var form = FormDef.create().section(FormDef.Section.of("empty", "Empty"));
		assertThrows(IllegalArgumentException.class, form::validate);
	}

	/**
	 * Field names are unique across the WHOLE form, not per section: every control shares one submit body and one
	 * dialog-scoped element-id namespace, so two sections declaring the same name would collide in both.
	 */
	@Test void f07_duplicateFieldNameAcrossSections_isRejected() {
		var form = FormDef.create()
			.section(FormDef.Section.of("one", "One").field(FormDef.Input.of("shared", "A", "text")))
			.section(FormDef.Section.of("two", "Two").field(FormDef.Input.of("shared", "B", "text")));
		var e = assertThrows(IllegalArgumentException.class, form::validate);
		assertTrue(e.getMessage().contains("duplicate field name"), e.getMessage());
	}

	@Test void f08_perFieldValidationStillRunsInsideASection() {
		// A select with no options is malformed wherever it is declared.
		var form = FormDef.create()
			.section(FormDef.Section.of("one", "One").field(FormDef.Input.of("sev", "Severity", "select")));
		assertThrows(IllegalArgumentException.class, form::validate);
	}

	@Test void f09_sectionFactoryRejectsBlankIdAndLabel() {
		assertThrows(IllegalArgumentException.class, () -> FormDef.Section.of(" ", "Label"));
		assertThrows(IllegalArgumentException.class, () -> FormDef.Section.of("id", " "));
		assertThrows(IllegalArgumentException.class, () -> FormDef.Section.of(null, "Label"));
		assertThrows(IllegalArgumentException.class, () -> FormDef.Section.of("id", null));
	}

	@Test void f10_sectionsWireShape() {
		var json = Json.of(FormDef.create()
			.section(FormDef.Section.of("basics", "Basics").field(FormDef.Input.of("name", "Name", "text")))
			.section(FormDef.Section.of("advanced", "Advanced").field(FormDef.Input.of("notes", "Notes", "textarea"))));
		var expected = Json.to("""
			{"sections":[{"id":"basics","label":"Basics","fields":[{"name":"name","label":"Name","type":"text"}]},
			             {"id":"advanced","label":"Advanced","fields":[{"name":"notes","label":"Notes","type":"textarea"}]}]}
			""", Map.class);
		assertEquals(expected, Json.to(json, Map.class), json);
	}

	@Test void f11_flatFormOmitsSectionsFromTheWire() {
		var json = Json.of(FormDef.create().field(FormDef.Input.of("name", "Name", "text")));
		assertFalse(json.contains("sections"), json);
	}

	@Test void f12_nullSection_isRejectedAtTheBuilder() {
		var form = FormDef.create();
		assertThrows(IllegalArgumentException.class, () -> form.section(null));
	}

	/** Builds a minimal valid field of the given non-text type (for cross-type rejection tests). */
	private static FormDef.Input baseFor(String type) {
		var i = FormDef.Input.of("f", "F", type);
		if ("select".equals(type))
			i.option("a", "A");
		else if ("action".equals(type))
			i.actionId("ack");
		return i;
	}
}
