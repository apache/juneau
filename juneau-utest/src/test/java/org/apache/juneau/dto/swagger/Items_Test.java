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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Items}.
 */
class Items_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new Items();
		assertEquals("foo", t.setType("foo").getType());
		assertNull(t.setType(null).getType());
		assertEquals("foo", t.setFormat("foo").getFormat());
		assertNull(t.setFormat(null).getFormat());
		assertJson(t.setItems(items("foo")).getItems(), "{type:'foo'}");
		assertEquals("foo", t.setCollectionFormat("foo").getCollectionFormat());
		assertNull(t.setCollectionFormat(null).getCollectionFormat());
		assertEquals("foo", t.setDefault("foo").getDefault());
		assertString("foo", t.setDefault(new StringBuilder("foo")).getDefault());
		assertNull(t.setDefault(null).getDefault());
		assertEquals(123, t.setMaximum(123).getMaximum());
		assertEquals(123f, t.setMaximum(123f).getMaximum());
		assertTrue(t.setExclusiveMaximum(true).getExclusiveMaximum());
		assertEquals(123, t.setMinimum(123).getMinimum());
		assertEquals(123f, t.setMinimum(123f).getMinimum());
		assertTrue(t.setExclusiveMinimum(true).getExclusiveMinimum());
		assertEquals(123, t.setMaxLength(123).getMaxLength());
		assertEquals(123, t.setMinLength(123).getMinLength());
		assertEquals("foo", t.setPattern("foo").getPattern());
		assertNull(t.setPattern(null).getPattern());
		assertEquals(123, t.setMaxItems(123).getMaxItems());
		assertEquals(123, t.setMinItems(123).getMinItems());
		assertTrue(t.setUniqueItems(true).getUniqueItems());
		assertJson(t.setEnum(set("foo","bar")).getEnum(), "['foo','bar']");
		assertJson(t.setEnum("foo","baz").getEnum(), "['foo','baz']");
		assertJson(t.setEnum(set()).getEnum(), "[]");
		assertNull(t.setEnum((Collection<Object>)null).getEnum());
		assertJson(t.addEnum("foo","bar").getEnum(), "['foo','bar']");
		assertJson(t.addEnum("baz").getEnum(), "['foo','bar','baz']");
		assertEquals(123, t.setMultipleOf(123).getMultipleOf());
		assertEquals(123f, t.setMultipleOf(123f).getMultipleOf());
	}

	/**
	 * Test method for {@link Items#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		var t = new Items();

		t
			.set("default", "a")
			.set("enum", set("b"))
			.set("collectionFormat", "c")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "g")
			.set("items", items("h"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("multipleOf", 123f)
			.set("pattern", "i")
			.set("type", "j")
			.set("uniqueItems", true)
			.set("$ref", "ref");

		assertJson(t, "{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

		t
			.set("default", "a")
			.set("enum", "['b']")
			.set("collectionFormat", "c")
			.set("exclusiveMaximum", "true")
			.set("exclusiveMinimum", "true")
			.set("format", "g")
			.set("items", "{type:'h'}")
			.set("maximum", "123f")
			.set("maxItems", "123")
			.set("maxLength", "123")
			.set("minimum", "123f")
			.set("minItems", "123")
			.set("minLength", "123")
			.set("multipleOf", "123f")
			.set("pattern", "i")
			.set("type", "j")
			.set("uniqueItems", "true")
			.set("$ref", "ref");

		assertJson(t, "{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

		t
			.set("default", new StringBuilder("a"))
			.set("enum", new StringBuilder("['b']"))
			.set("collectionFormat", new StringBuilder("c"))
			.set("exclusiveMaximum", new StringBuilder("true"))
			.set("exclusiveMinimum", new StringBuilder("true"))
			.set("format", new StringBuilder("g"))
			.set("items", new StringBuilder("{type:'h'}"))
			.set("maximum", new StringBuilder("123f"))
			.set("maxItems", new StringBuilder("123"))
			.set("maxLength", new StringBuilder("123"))
			.set("minimum", new StringBuilder("123f"))
			.set("minItems", new StringBuilder("123"))
			.set("minLength", new StringBuilder("123"))
			.set("multipleOf", new StringBuilder("123f"))
			.set("pattern", new StringBuilder("i"))
			.set("type", new StringBuilder("j"))
			.set("uniqueItems", new StringBuilder("true"))
			.set("$ref", new StringBuilder("ref"));

		assertJson(t, "{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

		assertEquals("a", t.get("default", String.class));
		assertEquals("['b']", t.get("enum", String.class));
		assertEquals("c", t.get("collectionFormat", String.class));
		assertEquals("true", t.get("exclusiveMaximum", String.class));
		assertEquals("true", t.get("exclusiveMinimum", String.class));
		assertEquals("g", t.get("format", String.class));
		assertEquals("{type:'h'}", t.get("items", String.class));
		assertEquals("123.0", t.get("maximum", String.class));
		assertEquals("123", t.get("maxItems", String.class));
		assertEquals("123", t.get("maxLength", String.class));
		assertEquals("123.0", t.get("minimum", String.class));
		assertEquals("123", t.get("minItems", String.class));
		assertEquals("123", t.get("minLength", String.class));
		assertEquals("123.0", t.get("multipleOf", String.class));
		assertEquals("i", t.get("pattern", String.class));
		assertEquals("j", t.get("type", String.class));
		assertEquals("true", t.get("uniqueItems", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertType(StringBuilder.class, t.get("default", Object.class));
		assertType(Set.class, t.get("enum", Object.class));
		assertType(String.class, t.get("collectionFormat", Object.class));
		assertType(Boolean.class, t.get("exclusiveMaximum", Object.class));
		assertType(Boolean.class, t.get("exclusiveMinimum", Object.class));
		assertType(String.class, t.get("format", Object.class));
		assertType(Items.class, t.get("items", Object.class));
		assertType(Float.class, t.get("maximum", Object.class));
		assertType(Integer.class, t.get("maxItems", Object.class));
		assertType(Integer.class, t.get("maxLength", Object.class));
		assertType(Float.class, t.get("minimum", Object.class));
		assertType(Integer.class, t.get("minItems", Object.class));
		assertType(Integer.class, t.get("minLength", Object.class));
		assertType(Float.class, t.get("multipleOf", Object.class));
		assertType(String.class, t.get("pattern", Object.class));
		assertType(String.class, t.get("type", Object.class));
		assertType(Boolean.class, t.get("uniqueItems", Object.class));
		assertType(String.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		var s = "{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, Items.class), s);
	}

	@Test void b02_copy() {
		var t = new Items();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("default", "a")
			.set("enum", set("b"))
			.set("collectionFormat", "c")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "g")
			.set("items", items("h"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("multipleOf", 123f)
			.set("pattern", "i")
			.set("type", "j")
			.set("uniqueItems", true)
			.set("$ref", "ref")
			.copy();

		assertJson(t, "{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");
	}

	@Test void b03_keySet() {
		var t = new Items();

		assertJson(t.keySet(), "[]");

		t
			.set("collectionFormat", "c")
			.set("default", "a")
			.set("enum", set("b"))
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "g")
			.set("items", items("h"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("multipleOf", 123f)
			.set("pattern", "i")
			.set("$ref", "ref")
			.set("type", "j")
			.set("uniqueItems", true);

		assertJson(t.keySet(),
			"['collectionFormat','default','enum','exclusiveMaximum','exclusiveMinimum','format','items','maximum','maxItems',"
			+ "'maxLength','minimum','minItems','minLength','multipleOf','pattern','$ref','type','uniqueItems']"
		);
	}
}