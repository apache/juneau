// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.bean.jsonschema;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Tests for JsonSchemaProperty fluent setter overrides.
 */
class JsonSchemaProperty_Test extends TestBase {

	@Test void a01_fluentChaining_basicSetters() {
		var p = new JsonSchemaProperty();

		// Test that fluent methods return JsonSchemaProperty (not JsonSchema)
		JsonSchemaProperty result;

		result = p.setName("myProperty");
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setTitle("My Property");
		assertSame(p, result);

		result = p.setDescription("Property description");
		assertSame(p, result);

		result = p.setType(JsonType.STRING);
		assertSame(p, result);

		result = p.setId("http://example.com/schema");
		assertSame(p, result);
	}

	@Test void a02_fluentChaining_numericConstraints() {
		var p = new JsonSchemaProperty();

		// Test numeric constraint methods
		JsonSchemaProperty result;

		result = p.setMultipleOf(5);
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setMaximum(100);
		assertSame(p, result);

		result = p.setMinimum(0);
		assertSame(p, result);

		result = p.setExclusiveMaximum(99);
		assertSame(p, result);

		result = p.setExclusiveMinimum(1);
		assertSame(p, result);
	}

	@Test void a03_fluentChaining_stringConstraints() {
		var p = new JsonSchemaProperty();

		// Test string constraint methods
		JsonSchemaProperty result;

		result = p.setMaxLength(50);
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setMinLength(1);
		assertSame(p, result);

		result = p.setPattern("^[a-z]+$");
		assertSame(p, result);

		result = p.setContentMediaType("text/plain");
		assertSame(p, result);

		result = p.setContentEncoding("base64");
		assertSame(p, result);
	}

	@Test void a04_fluentChaining_arrayConstraints() {
		var p = new JsonSchemaProperty();

		// Test array constraint methods
		JsonSchemaProperty result;

		result = p.setMaxItems(10);
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setMinItems(1);
		assertSame(p, result);

		result = p.setUniqueItems(true);
		assertSame(p, result);

		result = p.addItems(new JsonSchema());
		assertSame(p, result);

		result = p.addAdditionalItems(new JsonSchema());
		assertSame(p, result);
	}

	@Test void a05_fluentChaining_objectConstraints() {
		var p = new JsonSchemaProperty();

		// Test object constraint methods
		JsonSchemaProperty result;

		result = p.setMaxProperties(20);
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setMinProperties(1);
		assertSame(p, result);

		var props = new HashMap<String, JsonSchema>();
		result = p.setProperties(props);
		assertSame(p, result);

		result = p.addProperties(new JsonSchemaProperty("field1"));
		assertSame(p, result);
	}

	@Test void a06_fluentChaining_requiredAndEnum() {
		var p = new JsonSchemaProperty();

		// Test required and enum methods
		JsonSchemaProperty result;

		result = p.addRequired("field1", "field2");
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setRequired(l("field3"));
		assertSame(p, result);

		result = p.addEnum("value1", "value2");
		assertSame(p, result);

		result = p.setEnum(l("value3"));
		assertSame(p, result);
	}

	@Test void a07_fluentChaining_schemaComposition() {
		var p = new JsonSchemaProperty();

		// Test schema composition methods
		JsonSchemaProperty result;

		result = p.addAllOf(new JsonSchema());
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setAllOf(l(new JsonSchema()));
		assertSame(p, result);

		result = p.addAnyOf(new JsonSchema());
		assertSame(p, result);

		result = p.setAnyOf(l(new JsonSchema()));
		assertSame(p, result);

		result = p.addOneOf(new JsonSchema());
		assertSame(p, result);

		result = p.setOneOf(l(new JsonSchema()));
		assertSame(p, result);

		result = p.setNot(new JsonSchema());
		assertSame(p, result);
	}

	@Test void a08_fluentChaining_metadata() {
		var p = new JsonSchemaProperty();

		// Test metadata methods
		JsonSchemaProperty result;

		result = p.setReadOnly(true);
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setWriteOnly(false);
		assertSame(p, result);

		result = p.addExamples("example1", "example2");
		assertSame(p, result);

		result = p.setExamples(l("example3"));
		assertSame(p, result);

		result = p.setConst("constantValue");
		assertSame(p, result);
	}

	@Test void a09_fluentChaining_complex() {
		// Test chaining multiple fluent calls
		var result = new JsonSchemaProperty()
			.setName("username")
			.setType(JsonType.STRING)
			.setMinLength(3)
			.setMaxLength(20)
			.setPattern("^[a-zA-Z0-9_]+$")
			.setDescription("User's login name");

		assertInstanceOf(JsonSchemaProperty.class, result);
	}

	@Test void a10_constructor_withName() {
		var p = new JsonSchemaProperty("myProperty");

		assertInstanceOf(JsonSchemaProperty.class, p);
	}

	@Test void a11_constructor_withNameAndType() {
		var p = new JsonSchemaProperty("myProperty", JsonType.STRING);

		assertInstanceOf(JsonSchemaProperty.class, p);
	}

	@Test void a12_output_basic() throws Exception {
		var p = new JsonSchemaProperty("name", JsonType.STRING)
			.setMinLength(1)
			.setMaxLength(50);

		String json = JsonSerializer.DEFAULT.serialize(p);

		// Verify basic schema properties are serialized
		assertTrue(json.contains("\"type\":\"string\""));
		assertTrue(json.contains("\"minLength\":1"));
		assertTrue(json.contains("\"maxLength\":50"));
	}

	@Test void a13_fluentChaining_definitions() {
		var p = new JsonSchemaProperty();

		// Test definition methods
		JsonSchemaProperty result;

		result = p.addDefinition("def1", new JsonSchema());
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setDefinitions(new HashMap<>());
		assertSame(p, result);

		result = p.addDef("def2", new JsonSchema());
		assertSame(p, result);
	}

	@Test void a14_fluentChaining_dependencies() {
		var p = new JsonSchemaProperty();

		// Test dependency methods
		JsonSchemaProperty result;

		result = p.addDependency("dep1", new JsonSchema());
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setDependencies(new HashMap<>());
		assertSame(p, result);

		result = p.addDependentSchema("dep2", new JsonSchema());
		assertSame(p, result);

		result = p.setDependentSchemas(new HashMap<>());
		assertSame(p, result);

		result = p.addDependentRequired("dep3", l("field1"));
		assertSame(p, result);

		result = p.setDependentRequired(new HashMap<>());
		assertSame(p, result);
	}

	@Test void a15_fluentChaining_patternProperties() {
		var p = new JsonSchemaProperty();

		// Test pattern property methods
		JsonSchemaProperty result;

		result = p.addPatternProperties(new JsonSchemaProperty("pattern1"));
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setPatternProperties(new HashMap<>());
		assertSame(p, result);
	}

	@Test void a16_fluentChaining_prefixItems() {
		var p = new JsonSchemaProperty();

		// Test prefix items methods
		JsonSchemaProperty result;

		result = p.addPrefixItems(new JsonSchema());
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setPrefixItems(new JsonSchemaArray());
		assertSame(p, result);
	}

	@Test void a17_fluentChaining_unevaluated() {
		var p = new JsonSchemaProperty();

		// Test unevaluated methods
		JsonSchemaProperty result;

		result = p.setUnevaluatedItems(new JsonSchema());
		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);

		result = p.setUnevaluatedProperties(new JsonSchema());
		assertSame(p, result);
	}

	@Test void a18_fluentChaining_addTypes() {
		var p = new JsonSchemaProperty();

		// Test addTypes method
		JsonSchemaProperty result = p.addTypes(JsonType.STRING, JsonType.NULL);

		assertSame(p, result);
		assertInstanceOf(JsonSchemaProperty.class, result);
	}

	@Test void a19_getterMethods() {
		var p = new JsonSchemaProperty();

		// Test getter methods that are overridden
		p.setNot(new JsonSchema().setType(JsonType.STRING));
		JsonSchema not = p.getNot();
		assertNotNull(not);

		p.setUnevaluatedItems(new JsonSchema());
		JsonSchema unevaluated = p.getUnevaluatedItems();
		assertNotNull(unevaluated);
	}
}