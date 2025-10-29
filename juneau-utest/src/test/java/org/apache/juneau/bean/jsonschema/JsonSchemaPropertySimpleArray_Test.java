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
 * Tests for JsonSchemaPropertySimpleArray fluent setter overrides.
 */
class JsonSchemaPropertySimpleArray_Test extends TestBase {

	@Test void a01_fluentChaining_basicSetters() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("myArray", JsonType.STRING);

		// Test that fluent methods return JsonSchemaPropertySimpleArray (not JsonSchemaProperty or JsonSchema)
		JsonSchemaPropertySimpleArray result;

		result = p.setName("renamedArray");
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.setTitle("My Array");
		assertSame(p, result);

		result = p.setDescription("Array description");
		assertSame(p, result);

		result = p.setId("http://example.com/schema");
		assertSame(p, result);
	}

	@Test void a02_fluentChaining_arrayConstraints() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("items", JsonType.NUMBER);

		// Test array constraint methods
		JsonSchemaPropertySimpleArray result;

		result = p.setMaxItems(10);
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.setMinItems(1);
		assertSame(p, result);

		result = p.setUniqueItems(true);
		assertSame(p, result);

		result = p.addItems(new JsonSchema());
		assertSame(p, result);

		result = p.addAdditionalItems(new JsonSchema());
		assertSame(p, result);
	}

	@Test void a03_fluentChaining_numericConstraints() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("numbers", JsonType.INTEGER);

		// Test numeric constraint methods
		JsonSchemaPropertySimpleArray result;

		result = p.setMultipleOf(5);
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.setMaximum(100);
		assertSame(p, result);

		result = p.setMinimum(0);
		assertSame(p, result);
	}

	@Test void a04_fluentChaining_stringConstraints() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("tags", JsonType.STRING);

		// Test string constraint methods
		JsonSchemaPropertySimpleArray result;

		result = p.setMaxLength(50);
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.setMinLength(1);
		assertSame(p, result);

		result = p.setPattern("^[a-z]+$");
		assertSame(p, result);
	}

	@Test void a05_fluentChaining_metadata() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("data", JsonType.OBJECT);

		// Test metadata methods
		JsonSchemaPropertySimpleArray result;

		result = p.setReadOnly(true);
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.setWriteOnly(false);
		assertSame(p, result);

		result = p.addExamples("example1", "example2");
		assertSame(p, result);

		result = p.setConst("constantValue");
		assertSame(p, result);
	}

	@Test void a06_fluentChaining_requiredAndEnum() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("values", JsonType.STRING);

		// Test required and enum methods
		JsonSchemaPropertySimpleArray result;

		result = p.addRequired("field1", "field2");
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.addEnum("value1", "value2");
		assertSame(p, result);
	}

	@Test void a07_fluentChaining_schemaComposition() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("composite", JsonType.ANY);

		// Test schema composition methods
		JsonSchemaPropertySimpleArray result;

		result = p.addAllOf(new JsonSchema());
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.addAnyOf(new JsonSchema());
		assertSame(p, result);

		result = p.addOneOf(new JsonSchema());
		assertSame(p, result);

		result = p.setNot(new JsonSchema());
		assertSame(p, result);
	}

	@Test void a08_fluentChaining_complex() {
		// Test chaining multiple fluent calls
		JsonSchemaPropertySimpleArray result = new JsonSchemaPropertySimpleArray("usernames", JsonType.STRING)
			.setMinItems(1)
			.setMaxItems(10)
			.setMinLength(3)
			.setMaxLength(20)
			.setPattern("^[a-zA-Z0-9_]+$")
			.setDescription("List of usernames");

		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);
	}

	@Test void a09_constructor_withNameAndType() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("myArray", JsonType.STRING);

		assertInstanceOf(JsonSchemaPropertySimpleArray.class, p);
	}

	@Test void a10_output_basic() throws Exception {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("tags", JsonType.STRING)
			.setMinItems(1)
			.setMaxItems(5);

		String json = JsonSerializer.DEFAULT.serialize(p);

		assertTrue(json.contains("\"type\":\"array\""));
		assertTrue(json.contains("\"minItems\":1"));
		assertTrue(json.contains("\"maxItems\":5"));
		assertTrue(json.contains("\"items\""));
	}

	@Test void a11_fluentChaining_definitions() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("array", JsonType.STRING);

		// Test definition methods
		JsonSchemaPropertySimpleArray result;

		result = p.addDefinition("def1", new JsonSchema());
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.setDefinitions(new HashMap<>());
		assertSame(p, result);
	}

	@Test void a12_fluentChaining_properties() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("objects", JsonType.OBJECT);

		// Test property methods
		JsonSchemaPropertySimpleArray result;

		result = p.addProperties(new JsonSchemaProperty("field1"));
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.setProperties(new HashMap<>());
		assertSame(p, result);
	}

	@Test void a13_fluentChaining_dependencies() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("data", JsonType.ANY);

		// Test dependency methods
		JsonSchemaPropertySimpleArray result;

		result = p.addDependency("dep1", new JsonSchema());
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.addDependentSchema("dep2", new JsonSchema());
		assertSame(p, result);

		result = p.addDependentRequired("dep3", l("field1"));
		assertSame(p, result);
	}

	@Test void a14_fluentChaining_prefixItems() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("tuple", JsonType.ANY);

		// Test prefix items methods
		JsonSchemaPropertySimpleArray result;

		result = p.addPrefixItems(new JsonSchema());
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.setPrefixItems(new JsonSchemaArray());
		assertSame(p, result);
	}

	@Test void a15_fluentChaining_unevaluated() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("array", JsonType.ANY);

		// Test unevaluated methods
		JsonSchemaPropertySimpleArray result;

		result = p.setUnevaluatedItems(new JsonSchema());
		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);

		result = p.setUnevaluatedProperties(new JsonSchema());
		assertSame(p, result);
	}

	@Test void a16_output_withConstraints() throws Exception {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("ids", JsonType.INTEGER)
			.setMinItems(1)
			.setUniqueItems(true)
			.setDescription("List of unique IDs");

		String json = JsonSerializer.DEFAULT.serialize(p);

		assertTrue(json.contains("\"type\":\"array\""));
		assertTrue(json.contains("\"minItems\":1"));
		assertTrue(json.contains("\"uniqueItems\":true"));
		assertTrue(json.contains("\"description\":\"List of unique IDs\""));
	}

	@Test void a17_fluentChaining_addTypes() {
		JsonSchemaPropertySimpleArray p = new JsonSchemaPropertySimpleArray("mixed", JsonType.ANY);

		// Test addTypes method
		JsonSchemaPropertySimpleArray result = p.addTypes(JsonType.STRING, JsonType.NULL);

		assertSame(p, result);
		assertInstanceOf(JsonSchemaPropertySimpleArray.class, result);
	}
}