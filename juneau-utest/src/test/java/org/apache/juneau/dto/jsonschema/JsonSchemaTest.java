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
package org.apache.juneau.dto.jsonschema;

import static org.junit.Assert.*;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonschema.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

public class JsonSchemaTest extends SimpleTestBase {

	@Test void a01_schema1() throws Exception {
		var s = JsonSerializer.create().json5().ws().build();
		JsonParser p = JsonParser.DEFAULT;
		String r;
		JsonSchema t, t2;

		String expected = """
			{
				id: 'http://id',
				'$schema': 'http://schemaVersionUri',
				title: 'title',
				description: 'description',
				type: 'number',
				definitions: {
					definition: {
						'$ref': 'http://definition'
					}
				},
				properties: {
					property: {
						type: 'number'
					}
				},
				patternProperties: {
					'/pattern/': {
						type: 'number'
					}
				},
				dependencies: {
					dependency: {
						'$ref': 'http://dependency'
					}
				},
				items: [
					{
						type: 'number'
					}
				],
				multipleOf: 1,
				maximum: 2,
				exclusiveMaximum: true,
				minimum: 3,
				exclusiveMinimum: true,
				maxLength: 4,
				minLength: 5,
				pattern: '/pattern/',
				additionalItems: [
					{
						type: 'number'
					}
				],
				maxItems: 6,
				minItems: 7,
				uniqueItems: true,
				maxProperties: 8,
				minProperties: 9,
				required: [
					'required'
				],
				additionalProperties: {
					'$ref': 'http://additionalProperty'
				},
				'enum': [
					'enum'
				],
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
				oneOf: [
					{
						'$ref': 'http://oneOf'
					}
				],
				not: {
					'$ref': 'http://not'
				}
			}""";

		t = getTest1();
		r = s.serialize(t);
		assertEquals(expected, r);
		t2 = p.parse(r, JsonSchema.class);
		r = s.serialize(t2);
		assertEquals(expected, r);
	}

	@Test void a02_schema2() throws Exception {
		var s = JsonSerializer.create().json5().ws().build();
		JsonParser p = JsonParser.DEFAULT;
		String r;
		JsonSchema t, t2;

		String expected = """
			{
				id: 'http://id',
				'$schema': 'http://schemaVersionUri',
				type: [
					'string',
					'number'
				],
				definitions: {
					definition: {
						id: 'http://definition'
					}
				},
				items: [
					{
						'$ref': 'http://items'
					}
				],
				additionalItems: true,
				additionalProperties: true
			}""";

		t = getTest2();
		r = s.serialize(t);
		assertEquals(expected, r);
		t2 = p.parse(r, JsonSchema.class);
		r = s.serialize(t2);
		assertEquals(expected, r);
	}

	@Test void a03_toString() throws Exception {
		var s = JsonSerializer.create().json5().ws().build();
		JsonParser p = JsonParser.DEFAULT;
		String r;
		JsonSchema t, t2;

		String expected = """
			{
				id: 'http://id',
				'$schema': 'http://schemaVersionUri',
				title: 'title',
				description: 'description',
				type: 'number',
				definitions: {
					definition: {
						'$ref': 'http://definition'
					}
				},
				properties: {
					property: {
						type: 'number'
					}
				},
				patternProperties: {
					'/pattern/': {
						type: 'number'
					}
				},
				dependencies: {
					dependency: {
						'$ref': 'http://dependency'
					}
				},
				items: [
					{
						type: 'number'
					}
				],
				multipleOf: 1,
				maximum: 2,
				exclusiveMaximum: true,
				minimum: 3,
				exclusiveMinimum: true,
				maxLength: 4,
				minLength: 5,
				pattern: '/pattern/',
				additionalItems: [
					{
						type: 'number'
					}
				],
				maxItems: 6,
				minItems: 7,
				uniqueItems: true,
				maxProperties: 8,
				minProperties: 9,
				required: [
					'required'
				],
				additionalProperties: {
					'$ref': 'http://additionalProperty'
				},
				'enum': [
					'enum'
				],
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
				oneOf: [
					{
						'$ref': 'http://oneOf'
					}
				],
				not: {
					'$ref': 'http://not'
				}
			}""";

		t = getTest1();
		r = t.toString();
		t2 = p.parse(r, JsonSchema.class);
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
			.setExclusiveMaximum(true)
			.setMinimum(3)
			.setExclusiveMinimum(true)
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
