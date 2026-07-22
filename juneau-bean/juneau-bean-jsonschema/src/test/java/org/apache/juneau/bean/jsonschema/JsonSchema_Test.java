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
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"removal",   // Tests deprecated API for backward compatibility
	"rawtypes",  // JsonSchema/JsonSchemaProperty are self-typed CRTP roots; direct instantiation is intentionally raw (accepted 10.0.0 tradeoff).
	"unchecked"  // See rawtypes rationale above.
})
public class JsonSchema_Test extends TestBase {

	@Test void a01_schema1() throws Exception {
		var s = Json5Serializer.create().ws().build();
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
		var r = s.write(t);
		assertEquals(expected, r);
		var t2 = p.read(r, JsonSchema.class);
		r = s.write(t2);
		assertEquals(expected, r);
	}

	@Test void a02_schema2() throws Exception {
		var s = Json5Serializer.create().ws().build();
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
		var r = s.write(t);
		assertEquals(expected, r);
		var t2 = p.read(r, JsonSchema.class);
		r = s.write(t2);
		assertEquals(expected, r);
	}

	@Test void a03_toString() throws Exception {
		var s = Json5Serializer.create().ws().build();
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
		var t2 = p.read(r, JsonSchema.class);
		r = s.write(t2);
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
		var items = x.getAdditionalItemsAsSchemaArray();
		assertEquals(2, items.size());
		assertEquals("a", ((JsonSchemaProperty<?>) items.get(0)).getName());
		assertEquals("b", ((JsonSchemaProperty<?>) items.get(1)).getName());
	}

	@Test void b02_addAnyOf_calledTwice() {
		var x = new JsonSchema();
		x.addAnyOf(new JsonSchemaRef("http://a"));
		x.addAnyOf(new JsonSchemaRef("http://b")); // non-null path
		var anyOf = x.getAnyOf();
		assertEquals(2, anyOf.size());
		assertEquals(URI.create("http://a"), ((JsonSchema) anyOf.get(0)).getRef());
		assertEquals(URI.create("http://b"), ((JsonSchema) anyOf.get(1)).getRef());
	}

	@Test void b03_addDef_calledTwice() {
		var x = new JsonSchema();
		var d1 = new JsonSchema().setDescription("d1");
		var d2 = new JsonSchema().setDescription("d2");
		x.addDef("k1", d1);
		x.addDef("k2", d2); // non-null path
		var defs = x.getDefs();
		assertEquals(2, defs.size());
		assertSame(d1, defs.get("k1"));
		assertSame(d2, defs.get("k2"));
	}

	@Test void b04_addDefinition_calledTwice() {
		var x = new JsonSchema();
		var d1 = new JsonSchema().setDescription("d1");
		var d2 = new JsonSchema().setDescription("d2");
		x.addDefinition("d1", d1);
		x.addDefinition("d2", d2); // non-null path
		var definitions = x.getDefinitions();
		assertEquals(2, definitions.size());
		assertSame(d1, definitions.get("d1"));
		assertSame(d2, definitions.get("d2"));
	}

	@Test void b05_addDependency_calledTwice() {
		var x = new JsonSchema();
		var d1 = new JsonSchema().setDescription("d1");
		var d2 = new JsonSchema().setDescription("d2");
		x.addDependency("d1", d1);
		x.addDependency("d2", d2); // non-null path
		var dependencies = x.getDependencies();
		assertEquals(2, dependencies.size());
		assertSame(d1, dependencies.get("d1"));
		assertSame(d2, dependencies.get("d2"));
	}

	@Test void b06_addDependentRequired_calledTwice() {
		var x = new JsonSchema();
		x.addDependentRequired("k1", java.util.Arrays.asList("f1"));
		x.addDependentRequired("k2", java.util.Arrays.asList("f2")); // non-null path
		var dependentRequired = x.getDependentRequired();
		assertEquals(2, dependentRequired.size());
		assertEquals(java.util.Arrays.asList("f1"), dependentRequired.get("k1"));
		assertEquals(java.util.Arrays.asList("f2"), dependentRequired.get("k2"));
	}

	@Test void b07_addDependentSchema_calledTwice() {
		var x = new JsonSchema();
		var d1 = new JsonSchema().setDescription("d1");
		var d2 = new JsonSchema().setDescription("d2");
		x.addDependentSchema("k1", d1);
		x.addDependentSchema("k2", d2); // non-null path
		var dependentSchemas = x.getDependentSchemas();
		assertEquals(2, dependentSchemas.size());
		assertSame(d1, dependentSchemas.get("k1"));
		assertSame(d2, dependentSchemas.get("k2"));
	}

	@Test void b08_addEnum_calledTwice() {
		var x = new JsonSchema();
		x.addEnum("a");
		x.addEnum("b"); // non-null path
		var enum_ = x.getEnum();
		assertEquals(2, enum_.size());
		assertEquals("a", enum_.get(0));
		assertEquals("b", enum_.get(1));
	}

	@Test void b09_addExamples_calledTwice() {
		var x = new JsonSchema();
		x.addExamples("ex1");
		x.addExamples("ex2"); // non-null path
		var examples = x.getExamples();
		assertEquals(2, examples.size());
		assertEquals("ex1", examples.get(0));
		assertEquals("ex2", examples.get(1));
	}

	@Test void b10_addItems_calledTwice() {
		var x = new JsonSchema();
		x.addItems(new JsonSchema().setType(JsonType.STRING));
		x.addItems(new JsonSchema().setType(JsonType.NUMBER)); // non-null path
		var items = x.getItemsAsSchemaArray();
		assertEquals(2, items.size());
		assertEquals(JsonType.STRING, items.get(0).getType());
		assertEquals(JsonType.NUMBER, items.get(1).getType());
	}

	@Test void b11_addOneOf_calledTwice() {
		var x = new JsonSchema();
		x.addOneOf(new JsonSchemaRef("http://a"));
		x.addOneOf(new JsonSchemaRef("http://b")); // non-null path
		var oneOf = x.getOneOf();
		assertEquals(2, oneOf.size());
		assertEquals(URI.create("http://a"), ((JsonSchema) oneOf.get(0)).getRef());
		assertEquals(URI.create("http://b"), ((JsonSchema) oneOf.get(1)).getRef());
	}

	@Test void b12_addPatternProperties_calledTwice() {
		var x = new JsonSchema();
		x.addPatternProperties(new JsonSchemaProperty("/pat1/", JsonType.STRING));
		x.addPatternProperties(new JsonSchemaProperty("/pat2/", JsonType.NUMBER)); // non-null path
		var patternProperties = x.getPatternProperties();
		assertEquals(2, patternProperties.size());
		assertEquals(JsonType.STRING, ((JsonSchema) patternProperties.get("/pat1/")).getType());
		assertEquals(JsonType.NUMBER, ((JsonSchema) patternProperties.get("/pat2/")).getType());
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
		var prefixItems = x.getPrefixItems();
		assertEquals(2, prefixItems.size());
		assertEquals(JsonType.STRING, prefixItems.get(0).getType());
		assertEquals(JsonType.NUMBER, prefixItems.get(1).getType());
	}

	@Test void b15_addProperties_calledTwice() {
		var x = new JsonSchema();
		x.addProperties(new JsonSchemaProperty("f1", JsonType.STRING));
		x.addProperties(new JsonSchemaProperty("f2", JsonType.NUMBER)); // non-null path
		var properties = x.getProperties();
		assertEquals(2, properties.size());
		assertEquals(JsonType.STRING, ((JsonSchema) properties.get("f1")).getType());
		assertEquals(JsonType.NUMBER, ((JsonSchema) properties.get("f2")).getType());
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
		assertEquals(java.util.Arrays.asList("f1", "f2"), x.getRequired());
	}

	@Test void b18_addRequired_listOverload_calledTwice() {
		var x = new JsonSchema();
		x.addRequired(java.util.Arrays.asList("f1"));
		x.addRequired(java.util.Arrays.asList("f2")); // non-null path
		assertEquals(java.util.Arrays.asList("f1", "f2"), x.getRequired());
	}

	@Test void b19_jsonTypeArray_constructor() {
		var arr = new JsonTypeArray(JsonType.STRING, JsonType.NUMBER);
		assertEquals(2, arr.size());
		assertEquals(JsonType.STRING, arr.get(0));
		assertEquals(JsonType.NUMBER, arr.get(1));
	}

	@Test void b20_addRequired_stringOverload_calledTwice() {
		var x = new JsonSchema();
		x.addRequired("f1");
		x.addRequired("f2"); // non-null path
		assertEquals(java.util.Arrays.asList("f1", "f2"), x.getRequired());
	}

	@Test void b21_addTypes_calledTwice() {
		var x = new JsonSchema();
		x.addTypes(JsonType.STRING);
		x.addTypes(JsonType.NUMBER); // non-null path
		var types = x.getTypeAsJsonTypeArray();
		assertEquals(2, types.size());
		assertEquals(JsonType.STRING, types.get(0));
		assertEquals(JsonType.NUMBER, types.get(1));
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

	@Test void b23e2_getAdditionalPropertiesAsBooleanFromGetter() {
		// Regression: getAdditionalProperties() used to return the additionalItems backing field, so a
		// boolean additionalProperties could never be read back and could be masked by additionalItems.
		var x = new JsonSchema()
			.setAdditionalItems(Boolean.FALSE)
			.setAdditionalProperties(Boolean.TRUE);
		assertEquals(Boolean.TRUE, x.getAdditionalProperties());
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

	@Test void b25_formatCommentDeprecated_roundTrip() throws Exception {
		var s = Json5Serializer.create().ws().build();
		var p = Json5Parser.DEFAULT;

		var x = new JsonSchema()
			.setType(JsonType.STRING)
			.setFormat("uri")
			.setComment("schema note")
			.setDeprecated(true);

		var r = s.write(x);
		assertTrue(r.contains("format: 'uri'"));
		assertTrue(r.contains("'$comment': 'schema note'"));
		assertTrue(r.contains("deprecated: true"));

		var x2 = p.read(r, JsonSchema.class);
		assertEquals("uri", x2.getFormat());
		assertEquals("schema note", x2.getComment());
		assertEquals(Boolean.TRUE, x2.getDeprecated());
	}

	@Test void b26_summary_roundTrip() throws Exception {
		var s = Json5Serializer.create().ws().build();
		var p = Json5Parser.DEFAULT;

		var x = new JsonSchema()
			.setType(JsonType.STRING)
			.setSummary("AI-friendly short description");

		var r = s.write(x);
		assertTrue(r.contains("summary: 'AI-friendly short description'"));

		var x2 = p.read(r, JsonSchema.class);
		assertEquals("AI-friendly short description", x2.getSummary());
	}

	@Test void b27_summary_fluentOverridesOnSubclasses() {
		var prop = new JsonSchemaProperty("p", JsonType.STRING).setSummary("prop-summary");
		var ref = new JsonSchemaRef("http://x").setSummary("ref-summary");
		assertEquals("prop-summary", prop.getSummary());
		assertEquals("ref-summary", ref.getSummary());
	}
}