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

import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.apache.juneau.utest.utils.Utils2.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link HeaderInfo}.
 */
class HeaderInfo_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new HeaderInfo();
		assertEquals("foo", t.setDescription("foo").getDescription());
		assertNull(t.setDescription(null).getDescription());
		assertEquals("foo", t.setType("foo").getType());
		assertNull(t.setType(null).getType());
		assertEquals("foo", t.setFormat("foo").getFormat());
		assertNull(t.setFormat(null).getFormat());
		assertJson(t.setItems(items("foo")).getItems(), "{type:'foo'}");
		assertNull(t.setItems((Items)null).getItems());
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
		assertJson(t.setEnum(set()).getEnum(), "[]");
		assertJson(t.setEnum("foo","bar").getEnum(), "['foo','bar']");
		assertNull(t.setEnum((Set<Object>)null).getEnum());
		assertJson(t.addEnum("foo","bar").getEnum(), "['foo','bar']");
		assertJson(t.addEnum("baz").getEnum(), "['foo','bar','baz']");
		assertEquals(123, t.setMultipleOf(123).getMultipleOf());
		assertEquals(123f, t.setMultipleOf(123f).getMultipleOf());
		assertEquals("foo", t.setExample("foo").getExample());
		assertEquals(123f, t.setExample(123f).getExample());
		assertNull(t.setExample((String)null).getExample());
		assertEquals("foo", t.setRef("foo").getRef());
		assertNull(t.setRef(null).getRef());
	}

	/**
	 * Test method for {@link HeaderInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		var t = new HeaderInfo();

		t
			.set("default", "a")
			.set("enum", set("b"))
			.set("collectionFormat", "c")
			.set("description", "d")
			.set("example", "e")
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

		assertJson(t, "{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}");

		t
			.set("default", "a")
			.set("enum", "['b']")
			.set("collectionFormat", "c")
			.set("description", "d")
			.set("example", "e")
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

		assertJson(t, "{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}");

		t
			.set("default", new StringBuilder("a"))
			.set("enum", new StringBuilder("['b']"))
			.set("collectionFormat", new StringBuilder("c"))
			.set("description", new StringBuilder("d"))
			.set("example", new StringBuilder("e"))
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

		assertJson(t, "{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}");

		assertString("a", t.get("default", Object.class));
		assertJson(t.get("enum", Object.class), "['b']");
		assertEquals("c", t.get("collectionFormat", Object.class));
		assertEquals("d", t.get("description", Object.class));
		assertString("e", t.get("example", Object.class));
		assertTrue((Boolean)t.get("exclusiveMaximum", Object.class));
		assertTrue((Boolean)t.get("exclusiveMinimum", Object.class));
		assertEquals("g", t.get("format", Object.class));
		assertJson(t.get("items", Object.class), "{type:'h'}");
		assertEquals(123f, t.get("maximum", Object.class));
		assertEquals(123, t.get("maxItems", Object.class));
		assertEquals(123, t.get("maxLength", Object.class));
		assertEquals(123f, t.get("minimum", Object.class));
		assertEquals(123, t.get("minItems", Object.class));
		assertEquals(123, t.get("minLength", Object.class));
		assertEquals(123f, t.get("multipleOf", Object.class));
		assertEquals("i", t.get("pattern", Object.class));
		assertEquals("j", t.get("type", Object.class));
		assertTrue((Boolean)t.get("uniqueItems", Object.class));
		assertEquals("ref", t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}";
		assertJson(JsonParser.DEFAULT.parse(s, HeaderInfo.class), s);
	}

	@Test void b02_copy() {
		var t = new HeaderInfo();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("default", "a")
			.set("enum", set("b"))
			.set("collectionFormat", "c")
			.set("description", "d")
			.set("example", "e")
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

		assertJson(t, "{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}");
	}

	@Test void b03_keySet() {
		var t = new HeaderInfo();

		assertJson(t.keySet(), "[]");

		t
			.set("default", "a")
			.set("enum", set("b"))
			.set("collectionFormat", "c")
			.set("description", "d")
			.set("example", "e")
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

		assertJson(t.keySet(), "['collectionFormat','default','description','enum','example','exclusiveMaximum','exclusiveMinimum','format','items','maximum','maxItems','maxLength','minimum','minItems','minLength','multipleOf','pattern','$ref','type','uniqueItems']");
	}

	@Test void c01_strict() {
		var t = new HeaderInfo().strict();
		t.setCollectionFormat("csv");
		assertThrowsWithMessage(Exception.class, "Invalid", ()->t.setCollectionFormat("foo"));
		t.setType("string");
		assertThrowsWithMessage(Exception.class, "Invalid", ()->t.setType("foo"));
	}

	@Test void c02_resolveRefs() {
		var s = new Swagger();
		var t = new HeaderInfo();
		assertNotThrown(()->t.resolveRefs(s, new ArrayDeque<>(), 10));
	}
}