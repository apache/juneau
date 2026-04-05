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
package org.apache.juneau.bean.jsonschema;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.json5.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"removal" // Tests deprecated API for backward compatibility
})
public class JsonSchema_Test extends TestBase {

	@Test void a01_schema1() throws Exception {
		var s = Json5Serializer.create().ws().sortProperties().build();
		var p = Json5Parser.DEFAULT;

		var expected = """
			{
				'$schema': 'http://schemaVersionUri',
				additionalItems: [
					{
						type: 'number'
					}
				],
				additionalProperties: {
					'$ref': 'http://additionalProperty'
				},
				allOf: [
					{
						'$ref': 'http://allOf'
					}
				],
				anyOf: [
					{
						'$ref': 'http://anyOf'
					}
				],
				definitions: {
					definition: {
						'$ref': 'http://definition'
					}
				},
				dependencies: {
					dependency: {
						'$ref': 'http://dependency'
					}
				},
				description: 'description',
				'enum': [
					'enum'
				],
				exclusiveMaximum: 10,
				exclusiveMinimum: 1,
				id: 'http://id',
				items: [
					{
						type: 'number'
					}
				],
				maxItems: 6,
				maxLength: 4,
				maxProperties: 8,
				maximum: 2,
				minItems: 7,
				minLength: 5,
				minProperties: 9,
				minimum: 3,
				multipleOf: 1,
				not: {
					'$ref': 'http://not'
				},
				oneOf: [
					{
						'$ref': 'http://oneOf'
					}
				],
				pattern: '/pattern/',
				patternProperties: {
					'/pattern/': {
						type: 'number'
					}
				},
				properties: {
					property: {
						type: 'number'
					}
				},
				required: [
					'required'
				],
				title: 'title',
				type: 'number',
				uniqueItems: true
			}""";

		var t = getTest1();
		var r = s.serialize(t);
		assertEquals(expected, r);
		var t2 = p.parse(r, JsonSchema.class);
		r = s.serialize(t2);
		assertEquals(expected, r);
	}

	@Test void a02_schema2() throws Exception {
		var s = Json5Serializer.create().ws().sortProperties().build();
		var p = Json5Parser.DEFAULT;

		var expected = """
			{
				'$schema': 'http://schemaVersionUri',
				additionalItems: true,
				additionalProperties: true,
				definitions: {
					definition: {
						id: 'http://definition'
					}
				},
				id: 'http://id',
				items: [
					{
						'$ref': 'http://items'
					}
				],
				type: [
					'string',
					'number'
				]
			}""";

		var t = getTest2();
		var r = s.serialize(t);
		assertEquals(expected, r);
		var t2 = p.parse(r, JsonSchema.class);
		r = s.serialize(t2);
		assertEquals(expected, r);
	}

	@Test void a03_toString() throws Exception {
		var s = Json5Serializer.create().ws().sortProperties().build();
		var p = Json5Parser.DEFAULT;

		var expected = """
			{
				'$schema': 'http://schemaVersionUri',
				additionalItems: [
					{
						type: 'number'
					}
				],
				additionalProperties: {
					'$ref': 'http://additionalProperty'
				},
				allOf: [
					{
						'$ref': 'http://allOf'
					}
				],
				anyOf: [
					{
						'$ref': 'http://anyOf'
					}
				],
				definitions: {
					definition: {
						'$ref': 'http://definition'
					}
				},
				dependencies: {
					dependency: {
						'$ref': 'http://dependency'
					}
				},
				description: 'description',
				'enum': [
					'enum'
				],
				exclusiveMaximum: 10,
				exclusiveMinimum: 1,
				id: 'http://id',
				items: [
					{
						type: 'number'
					}
				],
				maxItems: 6,
				maxLength: 4,
				maxProperties: 8,
				maximum: 2,
				minItems: 7,
				minLength: 5,
				minProperties: 9,
				minimum: 3,
				multipleOf: 1,
				not: {
					'$ref': 'http://not'
				},
				oneOf: [
					{
						'$ref': 'http://oneOf'
					}
				],
				pattern: '/pattern/',
				patternProperties: {
					'/pattern/': {
						type: 'number'
					}
				},
				properties: {
					property: {
						type: 'number'
					}
				},
				required: [
					'required'
				],
				title: 'title',
				type: 'number',
				uniqueItems: true
			}""";

		var t = getTest1();
		var r = t.toString();
		var t2 = p.parse(r, JsonSchema.class);
		r = s.serialize(t2);
		assertEquals(expected, r);
	}

	/** Bean with simple values for each property */
	public static JsonSchema getTest1() {
		return new JsonSchema()
			.setId("http://id")
			.setSchemaVersionUri("http://schemaVersionUri")
			.setTitle("title")
			.setDescription("description")
			.setType(JsonType.NUMBER)
			.addDefinition("definition", new JsonSchemaRef("http://definition"))
			.addProperties(new JsonSchemaProperty("property", JsonType.NUMBER))
			.addPatternProperties(new JsonSchemaProperty("/pattern/", JsonType.NUMBER))
			.addDependency("dependency", new JsonSchemaRef("http://dependency"))
			.addItems(new JsonSchema().setType(JsonType.NUMBER))
			.setMultipleOf(1)
			.setMaximum(2)
			.setExclusiveMaximum(Integer.valueOf(10))  // Changed from Boolean to Number (Draft 06+)
			.setMinimum(3)
			.setExclusiveMinimum(Integer.valueOf(1))   // Changed from Boolean to Number (Draft 06+)
			.setMaxLength(4)
			.setMinLength(5)
			.setPattern("/pattern/")
			.addAdditionalItems(new JsonSchemaProperty("additionalItem", JsonType.NUMBER))
			.setMaxItems(6)
			.setMinItems(7)
			.setUniqueItems(true)
			.setMaxProperties(8)
			.setMinProperties(9)
			.addRequired("required")
			.setAdditionalProperties(new JsonSchemaRef("http://additionalProperty"))
			.addEnum("enum")
			.addAllOf(new JsonSchemaRef("http://allOf"))
			.addAnyOf(new JsonSchemaRef("http://anyOf"))
			.addOneOf(new JsonSchemaRef("http://oneOf"))
			.setNot(new JsonSchemaRef("http://not"))
		;
	}

	/** Bean with other possible property value types not covered in test1 */
	public static JsonSchema getTest2() {
		return new JsonSchema()
			.setId(URI.create("http://id"))
			.setSchemaVersionUri(URI.create("http://schemaVersionUri"))
			.setType(new JsonTypeArray(JsonType.STRING, JsonType.NUMBER))
			.addDefinition("definition", new JsonSchema().setId("http://definition"))
			.setItems(new JsonSchemaArray(new JsonSchemaRef("http://items")))
			.setAdditionalItems(Boolean.TRUE)
			.setAdditionalProperties(Boolean.TRUE);
	}

	// Tests for addXxx() false branches (call each method twice to hit the non-null path)

	@Test void b01_addAdditionalItems_calledTwice() {
		var x = new JsonSchema();
		x.addAdditionalItems(new JsonSchemaProperty("a", JsonType.STRING));
		x.addAdditionalItems(new JsonSchemaProperty("b", JsonType.NUMBER)); // non-null path
		assertEquals(2, x.getAdditionalItemsAsSchemaArray().size());
	}

	@Test void b02_addAnyOf_calledTwice() {
		var x = new JsonSchema();
		x.addAnyOf(new JsonSchemaRef("http://a"));
		x.addAnyOf(new JsonSchemaRef("http://b")); // non-null path
		assertEquals(2, x.getAnyOf().size());
	}

	@Test void b03_addDef_calledTwice() {
		var x = new JsonSchema();
		x.addDef("k1", new JsonSchema());
		x.addDef("k2", new JsonSchema()); // non-null path
		assertEquals(2, x.getDefs().size());
	}

	@Test void b04_addDefinition_calledTwice() {
		var x = new JsonSchema();
		x.addDefinition("d1", new JsonSchema());
		x.addDefinition("d2", new JsonSchema()); // non-null path
		assertEquals(2, x.getDefinitions().size());
	}

	@Test void b05_addDependency_calledTwice() {
		var x = new JsonSchema();
		x.addDependency("d1", new JsonSchema());
		x.addDependency("d2", new JsonSchema()); // non-null path
		assertEquals(2, x.getDependencies().size());
	}

	@Test void b06_addDependentRequired_calledTwice() {
		var x = new JsonSchema();
		x.addDependentRequired("k1", java.util.Arrays.asList("f1"));
		x.addDependentRequired("k2", java.util.Arrays.asList("f2")); // non-null path
		assertEquals(2, x.getDependentRequired().size());
	}

	@Test void b07_addDependentSchema_calledTwice() {
		var x = new JsonSchema();
		x.addDependentSchema("k1", new JsonSchema());
		x.addDependentSchema("k2", new JsonSchema()); // non-null path
		assertEquals(2, x.getDependentSchemas().size());
	}

	@Test void b08_addEnum_calledTwice() {
		var x = new JsonSchema();
		x.addEnum("a");
		x.addEnum("b"); // non-null path
		assertEquals(2, x.getEnum().size());
	}

	@Test void b09_addExamples_calledTwice() {
		var x = new JsonSchema();
		x.addExamples("ex1");
		x.addExamples("ex2"); // non-null path
		assertEquals(2, x.getExamples().size());
	}

	@Test void b10_addItems_calledTwice() {
		var x = new JsonSchema();
		x.addItems(new JsonSchema().setType(JsonType.STRING));
		x.addItems(new JsonSchema().setType(JsonType.NUMBER)); // non-null path
		assertEquals(2, x.getItemsAsSchemaArray().size());
	}

	@Test void b11_addOneOf_calledTwice() {
		var x = new JsonSchema();
		x.addOneOf(new JsonSchemaRef("http://a"));
		x.addOneOf(new JsonSchemaRef("http://b")); // non-null path
		assertEquals(2, x.getOneOf().size());
	}

	@Test void b12_addPatternProperties_calledTwice() {
		var x = new JsonSchema();
		x.addPatternProperties(new JsonSchemaProperty("/pat1/", JsonType.STRING));
		x.addPatternProperties(new JsonSchemaProperty("/pat2/", JsonType.NUMBER)); // non-null path
		assertEquals(2, x.getPatternProperties().size());
	}

	@Test void b13_addPatternProperties_nullNameThrows() {
		var x = new JsonSchema();
		var p = new JsonSchemaProperty(); // no name set
		assertThrows(RuntimeException.class, () -> x.addPatternProperties(p));
	}

	@Test void b14_addPrefixItems_calledTwice() {
		var x = new JsonSchema();
		x.addPrefixItems(new JsonSchema().setType(JsonType.STRING));
		x.addPrefixItems(new JsonSchema().setType(JsonType.NUMBER)); // non-null path
		assertEquals(2, x.getPrefixItems().size());
	}

	@Test void b15_addProperties_calledTwice() {
		var x = new JsonSchema();
		x.addProperties(new JsonSchemaProperty("f1", JsonType.STRING));
		x.addProperties(new JsonSchemaProperty("f2", JsonType.NUMBER)); // non-null path
		assertEquals(2, x.getProperties().size());
	}

	@Test void b16_addProperties_nullNameThrows() {
		var x = new JsonSchema();
		var p = new JsonSchemaProperty(); // no name set
		assertThrows(RuntimeException.class, () -> x.addProperties(p));
	}

	@Test void b17_addRequired_propertyOverload_calledTwice() {
		var x = new JsonSchema();
		x.addRequired(new JsonSchemaProperty("f1"));
		x.addRequired(new JsonSchemaProperty("f2")); // non-null path
		assertEquals(2, x.getRequired().size());
	}

	@Test void b18_addRequired_listOverload_calledTwice() {
		var x = new JsonSchema();
		x.addRequired(java.util.Arrays.asList("f1"));
		x.addRequired(java.util.Arrays.asList("f2")); // non-null path
		assertEquals(2, x.getRequired().size());
	}

	@Test void b19_jsonTypeArray_constructor() {
		var arr = new JsonTypeArray(JsonType.STRING, JsonType.NUMBER);
		assertEquals(2, arr.size());
		assertTrue(arr.contains(JsonType.STRING));
		assertTrue(arr.contains(JsonType.NUMBER));
	}

	@Test void b20_addRequired_stringOverload_calledTwice() {
		var x = new JsonSchema();
		x.addRequired("f1");
		x.addRequired("f2"); // non-null path
		assertEquals(2, x.getRequired().size());
	}

	@Test void b21_addTypes_calledTwice() {
		var x = new JsonSchema();
		x.addTypes(JsonType.STRING);
		x.addTypes(JsonType.NUMBER); // non-null path
		assertEquals(2, x.getTypeAsJsonTypeArray().size());
	}

	@Test void b22_beanIgnoreGetters() {
		var x = new JsonSchema()
			.setAdditionalItems(Boolean.TRUE)
			.setAdditionalProperties(Boolean.TRUE)
			.setItems(new JsonSchema().setType(JsonType.STRING));

		assertNotNull(x.getAdditionalItemsAsBoolean());
		assertNotNull(x.getAdditionalPropertiesAsBoolean());
		assertNotNull(x.getItemsAsSchema());
	}

	@Test void b23_getAdditionalPropertiesAsSchema() {
		var x = new JsonSchema().setAdditionalProperties(new JsonSchemaRef("http://extra"));
		assertNotNull(x.getAdditionalPropertiesAsSchema());
	}

	@Test void b23b_getTypeAsJsonType() {
		var x = new JsonSchema().setType(JsonType.STRING);
		assertNotNull(x.getTypeAsJsonType());
	}

	@Test void b23c_setDefs_nonNull_setsMaster() {
		var child = new JsonSchema().setType(JsonType.NUMBER);
		var x = new JsonSchema().setDefs(java.util.Map.of("key", child));
		assertNotNull(x.getDefs());
	}

	@Test void b23d_setAdditionalItems_invalidType_throws() {
		var x = new JsonSchema();
		assertThrows(RuntimeException.class, () -> x.setAdditionalItems("invalid"));
	}

	@Test void b23e_setAdditionalProperties_invalidType_throws() {
		var x = new JsonSchema();
		assertThrows(RuntimeException.class, () -> x.setAdditionalProperties("invalid"));
	}

	@Test void b23f_setItems_jsonSchemaArray_path() {
		var x = new JsonSchema().setItems(new JsonSchemaArray(new JsonSchemaRef("http://a")));
		assertNotNull(x.getItemsAsSchemaArray());
	}

	@Test void b23g_setItems_invalidType_throws() {
		var x = new JsonSchema();
		assertThrows(RuntimeException.class, () -> x.setItems("invalid"));
	}

	@Test void b23h_setType_invalidType_throws() {
		var x = new JsonSchema();
		assertThrows(RuntimeException.class, () -> x.setType("invalid-string"));
	}

	@Test void b23h2_resolve_refSetButNoSchemaMap() {
		// ref is not null, but master.schemaMap is null → returns this
		var ref = new JsonSchemaRef("http://example.org/schema");
		// No schemaMap set → master.schemaMap == null → returns this
		var resolved = ref.resolve();
		assertSame(ref, resolved);
	}

	@Test void b23i_resolve_withSchemaMap() {
		var targetUri = URI.create("http://target.org/schema");
		var targetSchema = new JsonSchema().setType(JsonType.OBJECT);

		var map = new JsonSchemaMap() {
			@Override
			public JsonSchema load(URI uri) {
				if (uri.equals(targetUri))
					return targetSchema;
				return null;
			}
		};

		var ref = new JsonSchemaRef(targetUri);
		ref.setSchemaMap(map);
		var resolved = ref.resolve();
		assertSame(targetSchema, resolved);
	}

	@Test void b23j_setMaster_withAllFields() {
		// Create a richly-populated schema so that setMaster() traverses all branches
		var child = new JsonSchema()
			.setType(JsonType.OBJECT)
			.addDefinition("d1", new JsonSchema())
			.addDef("k1", new JsonSchema())
			.addProperties(new JsonSchemaProperty("p1", JsonType.STRING))
			.addPatternProperties(new JsonSchemaProperty("/pat/", JsonType.NUMBER))
			.addDependency("dep1", new JsonSchema())
			.addDependentSchema("dep2", new JsonSchema())
			.setItems(new JsonSchema().setType(JsonType.ANY))
			.addItems(new JsonSchema().setType(JsonType.STRING))
			.addPrefixItems(new JsonSchema())
			.addAdditionalItems(new JsonSchemaProperty("ai", JsonType.BOOLEAN))
			.setUnevaluatedItems(new JsonSchema())
			.setAdditionalProperties(new JsonSchemaRef("http://extra"))
			.setUnevaluatedProperties(new JsonSchema())
			.addAllOf(new JsonSchemaRef("http://allOf"))
			.addAnyOf(new JsonSchemaRef("http://anyOf"))
			.addOneOf(new JsonSchemaRef("http://oneOf"))
			.setNot(new JsonSchemaRef("http://not"))
			.setIf(new JsonSchema())
			.setThen(new JsonSchema())
			.setElse(new JsonSchema());

		// Adding 'child' to a parent triggers setMaster() with all fields
		var parent = new JsonSchema().addDefinition("child", child);
		assertNotNull(parent);
	}

	@Test void b24_getProperty() {
		var x = new JsonSchema();
		// properties is null → getProperty(name) returns null
		assertNull(x.getProperty("missing"));
		assertNull(x.getProperty("missing", false));
		assertNull(x.getProperty("missing", true));

		// Add a property
		x.addProperties(new JsonSchemaProperty("foo", JsonType.STRING));

		// Property exists → found
		assertNotNull(x.getProperty("foo"));

		// Missing property in non-null map → returns null
		assertNull(x.getProperty("bar"));

		// resolve = true path (no schemaMap, so resolve() returns self)
		assertNotNull(x.getProperty("foo", true));
	}
}