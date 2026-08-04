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
package org.apache.juneau.bean.mcp.v20260728;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.junit.jupiter.api.*;

/**
 * Coverage for {@link ElicitSchema}'s fluent builder and build-time restricted-schema validation.
 */
class ElicitSchema_Test {

	@Test void a01_stringField_allModifiersRoundTrip() {
		var schema = ElicitSchema.create()
			.stringField("name").title("Name").description("Full name").format("email").defaultValue("a@b.com")
			.build();
		assertEquals(
			"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"title\":\"Name\","
				+ "\"description\":\"Full name\",\"format\":\"email\",\"default\":\"a@b.com\"}}}",
			schema.toString());
	}

	@Test void a02_numberAndIntegerField_minMaxDefaultRoundTrip() {
		var schema = ElicitSchema.create()
			.numberField("price").min(0.0).max(100.0).defaultValue(9.99)
			.integerField("qty").min(1).max(10).defaultValue(1)
			.build();
		assertEquals(
			"{\"type\":\"object\",\"properties\":{\"price\":{\"type\":\"number\",\"minimum\":0.0,"
				+ "\"maximum\":100.0,\"default\":9.99},\"qty\":{\"type\":\"integer\",\"minimum\":1,"
				+ "\"maximum\":10,\"default\":1}}}",
			schema.toString());
	}

	@Test void a03_booleanField_titleOnly() {
		var schema = ElicitSchema.create().booleanField("confirm").title("Confirm").build();
		assertEquals("{\"type\":\"object\",\"properties\":{\"confirm\":{\"type\":\"boolean\",\"title\":\"Confirm\"}}}",
			schema.toString());
	}

	@Test void a04_enumField_valuesAndNamesRoundTrip() {
		var schema = ElicitSchema.create().enumField("color", "r", "g", "b").enumNames("Red", "Green", "Blue").build();
		var property = schema.getMap("properties").getMap("color");
		assertEquals(List.of("r", "g", "b"), property.get("enum"));
		assertEquals(List.of("Red", "Green", "Blue"), property.get("enumNames"));
	}

	@Test void a05_multipleFields_onOneSchema() {
		var schema = ElicitSchema.create()
			.stringField("name").title("Name")
			.integerField("age").min(0).max(150)
			.build();
		var properties = schema.getMap("properties");
		assertEquals(Map.of("type", "string", "title", "Name"), properties.getMap("name"));
		assertEquals(Map.of("type", "integer", "minimum", 0, "maximum", 150), properties.getMap("age"));
	}

	@Test void a06_reDeclaringFieldName_lastFieldWins() {
		// Re-declaring "name" via a second stringField(...) call must discard the first declaration's
		// modifiers (title) entirely, leaving only what was applied after the second declaration.
		var schema = ElicitSchema.create()
			.stringField("name").title("First").description("first description")
			.stringField("name").format("email")
			.build();
		var property = schema.getMap("properties").getMap("name");
		assertEquals(Map.of("type", "string", "format", "email"), property);
	}

	@Test void b01_format_onNonStringField_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''age'': format is only valid on string fields",
			() -> ElicitSchema.create().integerField("age").format("email").build());
	}

	@Test void b02_minMax_onNonNumericField_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''name'': min/max are only valid on number/integer fields",
			() -> ElicitSchema.create().stringField("name").min(0).build());
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''flag'': min/max are only valid on number/integer fields",
			() -> ElicitSchema.create().booleanField("flag").max(1).build());
	}

	@Test void b03_enumNames_withoutEnumValues_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''name'': enumNames requires enum values",
			() -> ElicitSchema.create().stringField("name").enumNames("A").build());
	}

	@Test void b04_noFieldStarted_modifierThrowsIllegalStateException() {
		assertThrowsWithMessage(IllegalStateException.class,
			"No field has been started yet; call stringField(...)/numberField(...)/etc. first",
			() -> ElicitSchema.create().title("x"));
	}

	@Test void b05_enumField_nullValues_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class,
			"Field ''color'': enumField values must not be null or empty",
			() -> ElicitSchema.create().enumField("color", (String[])null));
	}

	@Test void b06_enumField_emptyValues_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class,
			"Field ''color'': enumField values must not be null or empty",
			() -> ElicitSchema.create().enumField("color"));
	}

	@Test void b07_title_null_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> ElicitSchema.create().stringField("name").title(null));
	}

	@Test void b08_description_null_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class,
			() -> ElicitSchema.create().stringField("name").description(null));
	}

	@Test void b09_format_null_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> ElicitSchema.create().stringField("name").format(null));
	}

	@Test void b10_enumNames_null_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "enumNames values must not be null or empty",
			() -> ElicitSchema.create().enumField("color", "r", "g").enumNames((String[])null));
	}

	@Test void b11_enumNames_empty_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "enumNames values must not be null or empty",
			() -> ElicitSchema.create().enumField("color", "r", "g").enumNames());
	}

	@Test void b12_enumNames_lengthMismatch_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "enumNames length (1) must match enum length (2)",
			() -> ElicitSchema.create().enumField("color", "r", "g").enumNames("Red"));
	}

	@Test void b13_emptyBuilder_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"ElicitSchema requires at least one field (an empty requestedSchema is unanswerable)",
			() -> ElicitSchema.create().build());
	}

	@Test void b14_required_unknownFieldName_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"required(...) names field ''age'', which was never added",
			() -> ElicitSchema.create().stringField("name").required("age").build());
	}

	@Test void b15_required_nullOrEmptyNames_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "required field names must not be null or empty",
			() -> ElicitSchema.create().stringField("name").required((String[])null));
		assertThrowsWithMessage(IllegalArgumentException.class, "required field names must not be null or empty",
			() -> ElicitSchema.create().stringField("name").required());
	}

	@Test void b16_minLength_onNonStringField_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''age'': minLength/maxLength are only valid on string fields",
			() -> ElicitSchema.create().integerField("age").minLength(1).build());
	}

	@Test void b17_maxLength_onNonStringField_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''age'': minLength/maxLength are only valid on string fields",
			() -> ElicitSchema.create().integerField("age").maxLength(1).build());
	}

	@Test void b18_minLength_negative_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''name'': minLength must not be negative",
			() -> ElicitSchema.create().stringField("name").minLength(-1).build());
	}

	@Test void b19_maxLength_negative_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''name'': maxLength must not be negative",
			() -> ElicitSchema.create().stringField("name").maxLength(-1).build());
	}

	@Test void b20_minLength_exceedsMaxLength_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''name'': minLength must not exceed maxLength",
			() -> ElicitSchema.create().stringField("name").minLength(5).maxLength(2).build());
	}

	@Test void b21_stringField_nullName_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'name' cannot be null.",
			() -> ElicitSchema.create().stringField(null));
	}

	@Test void b22_stringField_emptyName_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'name' cannot be blank.",
			() -> ElicitSchema.create().stringField(""));
	}

	@Test void b23_enumField_nullName_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'name' cannot be null.",
			() -> ElicitSchema.create().enumField(null, "r", "g"));
	}

	@Test void b24_enumField_emptyName_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'name' cannot be blank.",
			() -> ElicitSchema.create().enumField("", "r", "g"));
	}

	@Test void b25_enumField_nullElementValue_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class,
			"Field ''color'': enumField values must not contain a null element",
			() -> ElicitSchema.create().enumField("color", "r", null, "g"));
	}

	@Test void b26_enumNames_nullElementValue_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "enumNames values must not contain a null element",
			() -> ElicitSchema.create().enumField("color", "r", "g").enumNames("Red", null));
	}

	@Test void b27_required_nullElementName_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "required field names must not contain a null element",
			() -> ElicitSchema.create().stringField("a").stringField("b").required("a", (String)null));
	}

	@Test void b28_min_null_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'value' cannot be null.",
			() -> ElicitSchema.create().integerField("age").min(null));
	}

	@Test void b29_max_null_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'value' cannot be null.",
			() -> ElicitSchema.create().integerField("age").max(null));
	}

	@Test void b30_minLength_onEnumField_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''color'': minLength/maxLength are not valid on an enum (closed-choice) field",
			() -> ElicitSchema.create().enumField("color", "r", "g").minLength(1).build());
	}

	@Test void b31_maxLength_onEnumField_throwsAtBuildTime() {
		assertThrowsWithMessage(IllegalStateException.class,
			"Field ''color'': minLength/maxLength are not valid on an enum (closed-choice) field",
			() -> ElicitSchema.create().enumField("color", "r", "g").maxLength(1).build());
	}

	@Test void c01_noObjectOrArrayFieldMethodExists() {
		var methodNames = Arrays.stream(ElicitSchema.class.getDeclaredMethods())
			.map(m -> m.getName().toLowerCase(Locale.ROOT))
			.collect(Collectors.toSet());
		for (var forbidden : List.of("objectfield", "arrayfield", "nestedfield"))
			assertFalse(methodNames.contains(forbidden), () -> "must not declare a " + forbidden + " method");
	}

	@Test void d01_build_returnsSchemaIndependentOfFurtherBuilderMutation() {
		// Pins the fix for the shallow-copy aliasing bug: build() must deep-copy each per-field map so that
		// applying further modifiers to the builder after build() cannot mutate an already-returned schema.
		var builder = ElicitSchema.create().stringField("name").title("Name");
		var first = builder.build();
		builder.description("added after build");
		builder.integerField("age").min(0);
		assertEquals("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"title\":\"Name\"}}}",
			first.toString());
	}

	@Test void e01_required_singleField_emitsRequiredArray() {
		var schema = ElicitSchema.create().stringField("name").required("name").build();
		assertEquals(List.of("name"), schema.get("required"));
	}

	@Test void e02_required_multipleFields_preservesInsertionOrder() {
		var schema = ElicitSchema.create()
			.stringField("name").integerField("age").booleanField("confirm")
			.required("confirm", "name")
			.build();
		assertEquals(List.of("confirm", "name"), schema.get("required"));
	}

	@Test void e03_required_notCalled_omitsRequiredKey() {
		var schema = ElicitSchema.create().stringField("name").build();
		assertFalse(schema.containsKey("required"));
	}

	@Test void e04_required_multipleCalls_accumulate() {
		var schema = ElicitSchema.create()
			.stringField("name").integerField("age")
			.required("name")
			.required("age")
			.build();
		assertEquals(List.of("name", "age"), schema.get("required"));
	}

	@Test void e05_required_reDeclaredField_markingSurvives() {
		// required(...) is a builder-level, order-independent set (see class Javadoc): redeclaring "name" via
		// a second stringField(...) call discards its per-field modifiers (last-field-wins) but must NOT
		// discard its earlier required(...) marking.
		var schema = ElicitSchema.create()
			.stringField("name").required("name")
			.stringField("name")
			.build();
		assertEquals(List.of("name"), schema.get("required"));
	}

	@Test void e06_required_beforeFieldDeclared_survivesToBuild() {
		// The critical ordering: required(...) named before the field it names is even added must still
		// emit that name in the built "required" array.
		var schema = ElicitSchema.create()
			.required("email")
			.stringField("email")
			.build();
		assertEquals("{\"type\":\"object\",\"properties\":{\"email\":{\"type\":\"string\"}},\"required\":[\"email\"]}",
			schema.toString());
	}

	@Test void e07_required_mixedBeforeAndAfterFieldDeclaration_bothSurvive() {
		var schema = ElicitSchema.create()
			.stringField("a").required("a", "b").stringField("b")
			.build();
		assertEquals(List.of("a", "b"), schema.get("required"));
	}

	@Test void f01_minLengthAndMaxLength_emitOnStringField() {
		var schema = ElicitSchema.create().stringField("name").minLength(1).maxLength(10).build();
		assertEquals(Map.of("type", "string", "minLength", 1, "maxLength", 10),
			schema.getMap("properties").getMap("name"));
	}
}
