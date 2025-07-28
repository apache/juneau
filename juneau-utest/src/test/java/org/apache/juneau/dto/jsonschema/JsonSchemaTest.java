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
import static org.junit.runners.MethodSorters.*;

import java.net.*;

import org.apache.juneau.bean.jsonschema.JsonSchema;
import org.apache.juneau.bean.jsonschema.JsonSchemaArray;
import org.apache.juneau.bean.jsonschema.JsonSchemaProperty;
import org.apache.juneau.bean.jsonschema.JsonSchemaRef;
import org.apache.juneau.bean.jsonschema.JsonType;
import org.apache.juneau.bean.jsonschema.JsonTypeArray;
import org.apache.juneau.json.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class JsonSchemaTest {

	@Test
	public void testSchema1() throws Exception {
		JsonSerializer s = JsonSerializer.create().json5().ws().build();
		JsonParser p = JsonParser.DEFAULT;
		String r;
		JsonSchema t, t2;

		String expected =
			"{\n"
			+"	id: 'http://id',\n"
			+"	'$schema': 'http://schemaVersionUri',\n"
			+"	title: 'title',\n"
			+"	description: 'description',\n"
			+"	type: 'number',\n"
			+"	definitions: {\n"
			+"		definition: {\n"
			+"			'$ref': 'http://definition'\n"
			+"		}\n"
			+"	},\n"
			+"	properties: {\n"
			+"		property: {\n"
			+"			type: 'number'\n"
			+"		}\n"
			+"	},\n"
			+"	patternProperties: {\n"
			+"		'/pattern/': {\n"
			+"			type: 'number'\n"
			+"		}\n"
			+"	},\n"
			+"	dependencies: {\n"
			+"		dependency: {\n"
			+"			'$ref': 'http://dependency'\n"
			+"		}\n"
			+"	},\n"
			+"	items: [\n"
			+"		{\n"
			+"			type: 'number'\n"
			+"		}\n"
			+"	],\n"
			+"	multipleOf: 1,\n"
			+"	maximum: 2,\n"
			+"	exclusiveMaximum: true,\n"
			+"	minimum: 3,\n"
			+"	exclusiveMinimum: true,\n"
			+"	maxLength: 4,\n"
			+"	minLength: 5,\n"
			+"	pattern: '/pattern/',\n"
			+"	additionalItems: [\n"
			+"		{\n"
			+"			type: 'number'\n"
			+"		}\n"
			+"	],\n"
			+"	maxItems: 6,\n"
			+"	minItems: 7,\n"
			+"	uniqueItems: true,\n"
			+"	maxProperties: 8,\n"
			+"	minProperties: 9,\n"
			+"	required: [\n"
			+"		'required'\n"
			+"	],\n"
			+"	additionalProperties: {\n"
			+"		'$ref': 'http://additionalProperty'\n"
			+"	},\n"
			+"	'enum': [\n"
			+"		'enum'\n"
			+"	],\n"
			+"	allOf: [\n"
			+"		{\n"
			+"			'$ref': 'http://allOf'\n"
			+"		}\n"
			+"	],\n"
			+"	anyOf: [\n"
			+"		{\n"
			+"			'$ref': 'http://anyOf'\n"
			+"		}\n"
			+"	],\n"
			+"	oneOf: [\n"
			+"		{\n"
			+"			'$ref': 'http://oneOf'\n"
			+"		}\n"
			+"	],\n"
			+"	not: {\n"
			+"		'$ref': 'http://not'\n"
			+"	}\n"
			+"}";

		t = getTest1();
		r = s.serialize(t);
		assertEquals(expected, r);
		t2 = p.parse(r, JsonSchema.class);
		r = s.serialize(t2);
		assertEquals(expected, r);
	}

	@Test
	public void testSchema2() throws Exception {
		JsonSerializer s = JsonSerializer.create().json5().ws().build();
		JsonParser p = JsonParser.DEFAULT;
		String r;
		JsonSchema t, t2;

		String expected =
			"{\n"
			+"	id: 'http://id',\n"
			+"	'$schema': 'http://schemaVersionUri',\n"
			+"	type: [\n"
			+"		'string',\n"
			+"		'number'\n"
			+"	],\n"
			+"	definitions: {\n"
			+"		definition: {\n"
			+"			id: 'http://definition'\n"
			+"		}\n"
			+"	},\n"
			+"	items: [\n"
			+"		{\n"
			+"			'$ref': 'http://items'\n"
			+"		}\n"
			+"	],\n"
			+"	additionalItems: true,\n"
			+"	additionalProperties: true\n"
			+"}";

		t = getTest2();
		r = s.serialize(t);
		assertEquals(expected, r);
		t2 = p.parse(r, JsonSchema.class);
		r = s.serialize(t2);
		assertEquals(expected, r);
	}

	@Test
	public void testToString() throws Exception {
		JsonSerializer s = JsonSerializer.create().json5().ws().build();
		JsonParser p = JsonParser.DEFAULT;
		String r;
		JsonSchema t, t2;

		String expected =
			"{\n"
			+"	id: 'http://id',\n"
			+"	'$schema': 'http://schemaVersionUri',\n"
			+"	title: 'title',\n"
			+"	description: 'description',\n"
			+"	type: 'number',\n"
			+"	definitions: {\n"
			+"		definition: {\n"
			+"			'$ref': 'http://definition'\n"
			+"		}\n"
			+"	},\n"
			+"	properties: {\n"
			+"		property: {\n"
			+"			type: 'number'\n"
			+"		}\n"
			+"	},\n"
			+"	patternProperties: {\n"
			+"		'/pattern/': {\n"
			+"			type: 'number'\n"
			+"		}\n"
			+"	},\n"
			+"	dependencies: {\n"
			+"		dependency: {\n"
			+"			'$ref': 'http://dependency'\n"
			+"		}\n"
			+"	},\n"
			+"	items: [\n"
			+"		{\n"
			+"			type: 'number'\n"
			+"		}\n"
			+"	],\n"
			+"	multipleOf: 1,\n"
			+"	maximum: 2,\n"
			+"	exclusiveMaximum: true,\n"
			+"	minimum: 3,\n"
			+"	exclusiveMinimum: true,\n"
			+"	maxLength: 4,\n"
			+"	minLength: 5,\n"
			+"	pattern: '/pattern/',\n"
			+"	additionalItems: [\n"
			+"		{\n"
			+"			type: 'number'\n"
			+"		}\n"
			+"	],\n"
			+"	maxItems: 6,\n"
			+"	minItems: 7,\n"
			+"	uniqueItems: true,\n"
			+"	maxProperties: 8,\n"
			+"	minProperties: 9,\n"
			+"	required: [\n"
			+"		'required'\n"
			+"	],\n"
			+"	additionalProperties: {\n"
			+"		'$ref': 'http://additionalProperty'\n"
			+"	},\n"
			+"	'enum': [\n"
			+"		'enum'\n"
			+"	],\n"
			+"	allOf: [\n"
			+"		{\n"
			+"			'$ref': 'http://allOf'\n"
			+"		}\n"
			+"	],\n"
			+"	anyOf: [\n"
			+"		{\n"
			+"			'$ref': 'http://anyOf'\n"
			+"		}\n"
			+"	],\n"
			+"	oneOf: [\n"
			+"		{\n"
			+"			'$ref': 'http://oneOf'\n"
			+"		}\n"
			+"	],\n"
			+"	not: {\n"
			+"		'$ref': 'http://not'\n"
			+"	}\n"
			+"}";

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