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
 * Testcase for {@link HeaderInfo}.
 */
class HeaderInfo_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new HeaderInfo();

		// General - Combined assertBean test
		assertBean(
			t.setDescription("foo").setType("foo").setFormat("foo").setItems(items("foo"))
			 .setCollectionFormat("foo").setDefault("foo").setMaximum(123).setExclusiveMaximum(true)
			 .setMinimum(123).setExclusiveMinimum(true).setMaxLength(123).setMinLength(123)
			 .setPattern("foo").setMaxItems(123).setMinItems(123).setUniqueItems(true)
			 .setEnum(set("foo","bar")).setMultipleOf(123).setExample("foo").setRef("foo"),
			"description,type,format,items{type},collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,example,ref",
			"foo,foo,foo,{foo},foo,foo,123,true,123,true,123,123,foo,123,123,true,[foo,bar],123,foo,foo"
		);

		// Special cases
		assertNull(t.setDescription(null).getDescription());
		assertNull(t.setType(null).getType());
		assertNull(t.setFormat(null).getFormat());
		assertNull(t.setItems((Items)null).getItems());
		assertNull(t.setCollectionFormat(null).getCollectionFormat());
		assertNull(t.setDefault(null).getDefault());
		assertEquals(123f, t.setMaximum(123f).getMaximum());
		assertEquals(123f, t.setMinimum(123f).getMinimum());
		assertNull(t.setPattern(null).getPattern());
		assertEmpty(t.setEnum(set()).getEnum());
		assertSet(t.setEnum("foo","bar").getEnum(), "foo,bar");
		assertNull(t.setEnum((Set<Object>)null).getEnum());
		assertSet(t.addEnum("foo","bar").getEnum(), "foo,bar");
		assertSet(t.addEnum("baz").getEnum(), "foo,bar,baz");
		assertEquals(123f, t.setMultipleOf(123f).getMultipleOf());
		assertBean(t.setExample(123f), "example", "123.0");
		assertNull(t.setExample((String)null).getExample());
		assertBean(t.setRef(null), "ref", "null");
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

		assertBean(t,
			"description,type,format,items{type},collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,ref,example",
			"d,j,g,{h},c,a,123.0,true,123.0,true,123,123,i,123,123,true,[b],123.0,ref,e");

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

		assertBean(t,
			"description,type,format,items{type},collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,ref,example",
			"d,j,g,{h},c,a,123.0,true,123.0,true,123,123,i,123,123,true,[b],123.0,ref,e");

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

		assertBean(t,
			"description,type,format,items{type},collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,ref,example",
			"d,j,g,{h},c,a,123.0,true,123.0,true,123,123,i,123,123,true,[b],123.0,ref,e");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class),
			"default,collectionFormat,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items,maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,type,uniqueItems,$ref",
			"a,c,d,[b],e,true,true,g,{\"type\":\"h\"},123.0,123,123,123.0,123,123,123.0,i,j,true,ref");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		var s = "{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}";
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

		assertBean(t,
			"description,type,format,items{type},collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,ref,example",
			"d,j,g,{h},c,a,123.0,true,123.0,true,123,123,i,123,123,true,[b],123.0,ref,e");
	}

	@Test void b03_keySet() {
		var t = new HeaderInfo();

		assertEmpty(t.keySet());

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

		assertSet(t.keySet(), "collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items,maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,$ref,type,uniqueItems");
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