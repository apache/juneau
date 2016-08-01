/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.dto.jsonschema;

import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import java.net.*;

import org.junit.*;

import com.ibm.juno.core.dto.jsonschema.*;
import com.ibm.juno.core.json.*;


public class CT_JsonSchema {

	@Test
	public void testSchema1() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT_LAX_READABLE;
		JsonParser p = JsonParser.DEFAULT;
		String r;
		Schema t, t2;

		t = getTest1();
		r = s.serialize(t);
		String expected = readFile("/com/ibm/juno/core/test/dto/jsonschema/test1.json");
		assertEquals(expected, r);
		t2 = p.parse(r, Schema.class);
		assertEqualObjects(t, t2);
	}

	@Test
	public void testSchema2() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT_LAX_READABLE;
		JsonParser p = JsonParser.DEFAULT;
		String r;
		Schema t, t2;

		t = getTest2();
		r = s.serialize(t);
		String expected = readFile("/com/ibm/juno/core/test/dto/jsonschema/test2.json");
		assertEquals(expected, r);
		t2 = p.parse(r, Schema.class);
		assertEqualObjects(t, t2);
	}

	/** Bean with simple values for each property */
	public static Schema getTest1() {
		return new Schema()
			.setId("http://id")
			.setSchemaVersionUri("http://schemaVersionUri")
			.setTitle("title")
			.setDescription("description")
			.setType(JsonType.NUMBER)
			.addDefinition("definition", new SchemaRef("http://definition"))
			.addProperties(new SchemaProperty("property", JsonType.NUMBER))
			.addPatternProperties(new SchemaProperty("/pattern/", JsonType.NUMBER))
			.addDependency("dependency", new SchemaRef("http://dependency"))
			.addItems(new Schema().setType(JsonType.NUMBER))
			.setMultipleOf(1)
			.setMaximum(2)
			.setExclusiveMaximum(true)
			.setMinimum(3)
			.setExclusiveMinimum(true)
			.setMaxLength(4)
			.setMinLength(5)
			.setPattern("/pattern/")
			.addAdditionalItems(new SchemaProperty("additionalItem", JsonType.NUMBER))
			.setMaxItems(6)
			.setMinItems(7)
			.setUniqueItems(true)
			.setMaxProperties(8)
			.setMinProperties(9)
			.addRequired("required")
			.setAdditionalProperties(new SchemaRef("http://additionalProperty"))
			.addEnum("enum")
			.addAllOf(new SchemaRef("http://allOf"))
			.addAnyOf(new SchemaRef("http://anyOf"))
			.addOneOf(new SchemaRef("http://oneOf"))
			.setNot(new SchemaRef("http://not"));
	}

	/** Bean with other possible property value types not covered in test1 */
	public static Schema getTest2() {
		return new Schema()
			.setId(URI.create("http://id"))
			.setSchemaVersionUri(URI.create("http://schemaVersionUri"))
			.setType(new JsonTypeArray(JsonType.STRING, JsonType.NUMBER))
			.addDefinition("definition", new Schema().setId("http://definition"))
			.setItems(new SchemaArray(new SchemaRef("http://items")))
			.setAdditionalItems(Boolean.TRUE)
			.setAdditionalProperties(Boolean.TRUE);
	}
}
