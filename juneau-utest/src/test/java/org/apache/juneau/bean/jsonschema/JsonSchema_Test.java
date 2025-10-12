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
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

@SuppressWarnings("deprecation")
public class JsonSchema_Test extends TestBase {

	@Test void a01_schema1() throws Exception {
		var s = JsonSerializer.create().json5().ws().sortProperties().build();
		var p = JsonParser.DEFAULT;

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
		var s = JsonSerializer.create().json5().ws().sortProperties().build();
		var p = JsonParser.DEFAULT;

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
		var s = JsonSerializer.create().json5().ws().sortProperties().build();
		var p = JsonParser.DEFAULT;

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
}
