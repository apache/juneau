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
 * Tests for JsonSchemaRef fluent setter overrides.
 */
class JsonSchemaRef_Test extends TestBase {

	@Test void a01_fluentChaining_basicSetters() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test that fluent methods return JsonSchemaRef (not JsonSchema)
		JsonSchemaRef result;

		result = r.setName("myRef");
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setTitle("My Reference");
		assertSame(r, result);

		result = r.setDescription("Reference description");
		assertSame(r, result);

		result = r.setType(JsonType.STRING);
		assertSame(r, result);

		result = r.setId("http://example.com/id");
		assertSame(r, result);
	}

	@Test void a02_fluentChaining_numericConstraints() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test numeric constraint methods
		JsonSchemaRef result;

		result = r.setMultipleOf(5);
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setMaximum(100);
		assertSame(r, result);

		result = r.setMinimum(0);
		assertSame(r, result);

		result = r.setExclusiveMaximum(99);
		assertSame(r, result);

		result = r.setExclusiveMinimum(1);
		assertSame(r, result);
	}

	@Test void a03_fluentChaining_stringConstraints() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test string constraint methods
		JsonSchemaRef result;

		result = r.setMaxLength(50);
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setMinLength(1);
		assertSame(r, result);

		result = r.setPattern("^[a-z]+$");
		assertSame(r, result);

		result = r.setContentMediaType("text/plain");
		assertSame(r, result);

		result = r.setContentEncoding("base64");
		assertSame(r, result);
	}

	@Test void a04_fluentChaining_arrayConstraints() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test array constraint methods
		JsonSchemaRef result;

		result = r.setMaxItems(10);
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setMinItems(1);
		assertSame(r, result);

		result = r.setUniqueItems(true);
		assertSame(r, result);

		result = r.addItems(new JsonSchema());
		assertSame(r, result);

		result = r.addAdditionalItems(new JsonSchema());
		assertSame(r, result);
	}

	@Test void a05_fluentChaining_objectConstraints() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test object constraint methods
		JsonSchemaRef result;

		result = r.setMaxProperties(20);
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setMinProperties(1);
		assertSame(r, result);

		var props = new HashMap<String, JsonSchema>();
		result = r.setProperties(props);
		assertSame(r, result);

		result = r.addProperties(new JsonSchemaProperty("field1"));
		assertSame(r, result);
	}

	@Test void a06_fluentChaining_requiredAndEnum() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test required and enum methods
		JsonSchemaRef result;

		result = r.addRequired("field1", "field2");
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setRequired(l("field3"));
		assertSame(r, result);

		result = r.addEnum("value1", "value2");
		assertSame(r, result);

		result = r.setEnum(l("value3"));
		assertSame(r, result);
	}

	@Test void a07_fluentChaining_schemaComposition() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test schema composition methods
		JsonSchemaRef result;

		result = r.addAllOf(new JsonSchema());
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setAllOf(l(new JsonSchema()));
		assertSame(r, result);

		result = r.addAnyOf(new JsonSchema());
		assertSame(r, result);

		result = r.setAnyOf(l(new JsonSchema()));
		assertSame(r, result);

		result = r.addOneOf(new JsonSchema());
		assertSame(r, result);

		result = r.setOneOf(l(new JsonSchema()));
		assertSame(r, result);

		result = r.setNot(new JsonSchema());
		assertSame(r, result);
	}

	@Test void a08_fluentChaining_metadata() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test metadata methods
		JsonSchemaRef result;

		result = r.setReadOnly(true);
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setWriteOnly(false);
		assertSame(r, result);

		result = r.addExamples("example1", "example2");
		assertSame(r, result);

		result = r.setExamples(l("example3"));
		assertSame(r, result);

		result = r.setConst("constantValue");
		assertSame(r, result);
	}

	@Test void a09_fluentChaining_complex() {
		// Test chaining multiple fluent calls
		JsonSchemaRef result = new JsonSchemaRef("http://example.com/user-schema")
			.setName("userRef")
			.setTitle("User Reference")
			.setDescription("Reference to user schema");

		assertInstanceOf(JsonSchemaRef.class, result);
	}

	@Test void a10_constructor_withUri() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		assertInstanceOf(JsonSchemaRef.class, r);
	}

	@Test void a11_output_basic() throws Exception {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		String json = JsonSerializer.DEFAULT.serialize(r);

		assertTrue(json.contains("\"$ref\""));
		assertTrue(json.contains("http://example.com/schema"));
	}

	@Test void a12_fluentChaining_definitions() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test definition methods
		JsonSchemaRef result;

		result = r.addDefinition("def1", new JsonSchema());
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setDefinitions(new HashMap<>());
		assertSame(r, result);

		result = r.addDef("def2", new JsonSchema());
		assertSame(r, result);
	}

	@Test void a13_fluentChaining_dependencies() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test dependency methods
		JsonSchemaRef result;

		result = r.addDependency("dep1", new JsonSchema());
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setDependencies(new HashMap<>());
		assertSame(r, result);

		result = r.addDependentSchema("dep2", new JsonSchema());
		assertSame(r, result);

		result = r.setDependentSchemas(new HashMap<>());
		assertSame(r, result);

		result = r.addDependentRequired("dep3", l("field1"));
		assertSame(r, result);

		result = r.setDependentRequired(new HashMap<>());
		assertSame(r, result);
	}

	@Test void a14_fluentChaining_patternProperties() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test pattern property methods
		JsonSchemaRef result;

		result = r.addPatternProperties(new JsonSchemaProperty("pattern1"));
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setPatternProperties(new HashMap<>());
		assertSame(r, result);
	}

	@Test void a15_fluentChaining_prefixItems() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test prefix items methods
		JsonSchemaRef result;

		result = r.addPrefixItems(new JsonSchema());
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setPrefixItems(new JsonSchemaArray());
		assertSame(r, result);
	}

	@Test void a16_fluentChaining_unevaluated() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test unevaluated methods
		JsonSchemaRef result;

		result = r.setUnevaluatedItems(new JsonSchema());
		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);

		result = r.setUnevaluatedProperties(new JsonSchema());
		assertSame(r, result);
	}

	@Test void a17_fluentChaining_addTypes() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test addTypes method
		JsonSchemaRef result = r.addTypes(JsonType.STRING, JsonType.NULL);

		assertSame(r, result);
		assertInstanceOf(JsonSchemaRef.class, result);
	}

	@Test void a18_getterMethods() {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema");

		// Test getter methods that are overridden
		r.setNot(new JsonSchema().setType(JsonType.STRING));
		JsonSchema not = r.getNot();
		assertNotNull(not);

		r.setUnevaluatedItems(new JsonSchema());
		JsonSchema unevaluated = r.getUnevaluatedItems();
		assertNotNull(unevaluated);
	}

	@Test void a19_output_withAdditionalProperties() throws Exception {
		JsonSchemaRef r = new JsonSchemaRef("http://example.com/schema")
			.setTitle("User Schema")
			.setDescription("Schema for user objects");

		String json = JsonSerializer.DEFAULT.serialize(r);

		assertTrue(json.contains("\"$ref\""));
		assertTrue(json.contains("http://example.com/schema"));
		assertTrue(json.contains("\"title\":\"User Schema\""));
		assertTrue(json.contains("\"description\":\"Schema for user objects\""));
	}
}