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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SwaggerElement}.
 */
class SwaggerElement_Test extends SimpleTestBase {

	/**
	 * Test class that extends SwaggerElement for testing purposes.
	 */
	public static class TestSwaggerElement extends SwaggerElement {
		private String testProperty;

		public TestSwaggerElement() {}

		public TestSwaggerElement(TestSwaggerElement copyFrom) {
			super(copyFrom);
			this.testProperty = copyFrom.testProperty;
		}

		public String getTestProperty() { return testProperty; }
		public TestSwaggerElement setTestProperty(String testProperty) { this.testProperty = testProperty; return this; }
	}

	/**
	 * Test method for strict mode.
	 */
	@Test void a01_strictMode() {
		var t = new TestSwaggerElement();

		// Default should be non-strict
		assertFalse(t.isStrict());

		// Test setting strict mode
		assertBean(t.strict(true), "strict", "true");
		assertTrue(t.isStrict());

		assertBean(t.strict(false), "strict", "false");
		assertFalse(t.isStrict());
	}

	/**
	 * Test method for extra properties.
	 */
	@Test void a02_extraProperties() {
		var t = new TestSwaggerElement();

		// Test setting extra properties
		t.set("key1", "value1");
		t.set("key2", "value2");

		// Test getting extra properties
		assertBean(t, "key1,key2", "value1,value2");
	}

	/**
	 * Test method for extra properties with null.
	 */
	@Test void a03_extraPropertiesNull() {
		var t = new TestSwaggerElement();

		// Test setting null extra properties
		t.set("key1", null);
		assertNull(t.get("key1"));
	}

	/**
	 * Test method for extra properties with empty map.
	 */
	@Test void a04_extraPropertiesEmpty() {
		var t = new TestSwaggerElement();

		// Test that no extra properties are set initially
		assertTrue(t.extraKeys().isEmpty());
	}

	/**
	 * Test method for copy constructor.
	 */
	@Test void a05_copyConstructor() {
		var original = new TestSwaggerElement();
		original.setTestProperty("test");
		original.strict(true);
		original.set("key", "value");

		var copy = new TestSwaggerElement(original);

		// Test that properties are copied
		assertEquals("test", copy.getTestProperty());
		assertTrue(copy.isStrict());
		assertEquals("value", copy.get("key"));
	}

	/**
	 * Test method for serialization with extra properties.
	 */
	@Test void a06_serializationWithExtra() {
		var t = new TestSwaggerElement();
		t.setTestProperty("test");
		t.set("extraKey", "extraValue");

		assertEquals("{\"testProperty\":\"test\",\"extraKey\":\"extraValue\"}", t.toString());
	}

	/**
	 * Test method for deserialization with extra properties.
	 */
	@Test void a07_deserializationWithExtra() {
		var json = "{\"testProperty\":\"test\",\"extraKey\":\"extraValue\"}";

		var t = JsonParser.DEFAULT.parse(json, TestSwaggerElement.class);
		assertBean(t, "testProperty,extraKey", "test,extraValue");
	}

	/**
	 * Test method for fluent setters.
	 */
	@Test void a08_fluentSetters() {
		var t = new TestSwaggerElement();

		// Test chaining
		var result = t.setTestProperty("test")
			.strict(true)
			.set("key", "value");

		assertSame(t, result);
		assertBean(t, "testProperty,strict,key", "test,true,value");
	}

	/**
	 * Test method for extra properties modification.
	 */
	@Test void a09_extraPropertiesModification() {
		var t = new TestSwaggerElement();
		t.set("key1", "value1");

		// Modify the extra properties
		t.set("key2", "value2");

		// Verify the modification
		assertBean(t, "key1,key2", "value1,value2");
	}

	/**
	 * Test method for complex extra properties.
	 */
	@Test void a10_complexExtraProperties() {
		var t = new TestSwaggerElement();

		t.set("string", "value");
		t.set("number", 123);
		t.set("boolean", true);
		t.set("array", list("a", "b", "c"));
		t.set("object", map("nested", "value"));

		assertBean(t, "string,number,boolean,array,object", "value,123,true,[a,b,c],{nested=value}");
	}
}
