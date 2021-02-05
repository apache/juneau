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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link HeaderInfo}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class HeaderInfo_Test {

	/**
	 * Test method for {@link HeaderInfo#description(java.lang.Object)}.
	 */
	@Test
	public void a01_description() {
		HeaderInfo t = new HeaderInfo();

		t.description("foo");
		assertString(t.description()).is("foo");

		t.description(null);
		assertString(t.description()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#type(java.lang.Object)}.
	 */
	@Test
	public void a02_type() {
		HeaderInfo t = new HeaderInfo();

		t.type("foo");
		assertString(t.type()).is("foo");

		t.type(null);
		assertString(t.type()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#format(java.lang.Object)}.
	 */
	@Test
	public void a03_format() {
		HeaderInfo t = new HeaderInfo();

		t.format("foo");
		assertString(t.format()).is("foo");

		t.format(null);
		assertString(t.format()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#items(java.lang.Object)}.
	 */
	@Test
	public void a04_items() {
		HeaderInfo t = new HeaderInfo();

		t.items(items("foo"));
		assertObject(t.items()).asJson().is("{type:'foo'}");

		t.items("{type:'foo'}");
		assertObject(t.items()).isType(Items.class).asJson().is("{type:'foo'}");

		t.items((Items)null);
		assertObject(t.items()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#collectionFormat(java.lang.Object)}.
	 */
	@Test
	public void a05_collectionFormat() {
		HeaderInfo t = new HeaderInfo();

		t.collectionFormat("foo");
		assertString(t.collectionFormat()).is("foo");

		t.collectionFormat(null);
		assertString(t.collectionFormat()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#_default(java.lang.Object)}.
	 */
	@Test
	public void a06_default() {
		HeaderInfo t = new HeaderInfo();

		t._default("foo");
		assertString(t._default()).is("foo");

		t._default(new StringBuilder("foo"));
		assertObject(t._default()).isType(StringBuilder.class).asString().is("foo");

		t._default(null);
		assertObject(t._default()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#maximum(java.lang.Object)}.
	 */
	@Test
	public void a07_maximum() {
		HeaderInfo t = new HeaderInfo();

		t.maximum(123);
		assertObject(t.maximum()).isType(Integer.class).is(123);

		t.maximum(123f);
		assertObject(t.maximum()).isType(Float.class).is(123f);

		t.maximum("123");
		assertObject(t.maximum()).isType(Integer.class).is(123);

		t.maximum((String)null);
		assertObject(t.maximum()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#exclusiveMaximum(java.lang.Object)}.
	 */
	@Test
	public void a08_exclusiveMaximum() {
		HeaderInfo t = new HeaderInfo();

		t.exclusiveMaximum(true);
		assertObject(t.exclusiveMaximum()).isType(Boolean.class).is(true);

		t.exclusiveMaximum("true");
		assertObject(t.exclusiveMaximum()).isType(Boolean.class).is(true);

		t.exclusiveMaximum((String)null);
		assertObject(t.exclusiveMaximum()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#minimum(java.lang.Object)}.
	 */
	@Test
	public void a09_minimum() {
		HeaderInfo t = new HeaderInfo();

		t.minimum(123);
		assertObject(t.minimum()).isType(Integer.class).is(123);

		t.minimum(123f);
		assertObject(t.minimum()).isType(Float.class).is(123f);

		t.minimum("123");
		assertObject(t.minimum()).isType(Integer.class).is(123);

		t.minimum((String)null);
		assertObject(t.minimum()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#exclusiveMinimum(java.lang.Object)}.
	 */
	@Test
	public void a10_exclusiveMinimum() {
		HeaderInfo t = new HeaderInfo();

		t.exclusiveMinimum(true);
		assertObject(t.exclusiveMinimum()).isType(Boolean.class).is(true);

		t.exclusiveMinimum("true");
		assertObject(t.exclusiveMinimum()).isType(Boolean.class).is(true);

		t.exclusiveMinimum((String)null);
		assertObject(t.exclusiveMinimum()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#maxLength(java.lang.Object)}.
	 */
	@Test
	public void a11_maxLength() {
		HeaderInfo t = new HeaderInfo();

		t.maxLength(123);
		assertObject(t.maxLength()).isType(Integer.class).is(123);

		t.maxLength("123");
		assertObject(t.maxLength()).isType(Integer.class).is(123);

		t.maxLength((String)null);
		assertObject(t.maxLength()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#minLength(java.lang.Object)}.
	 */
	@Test
	public void a12_minLength() {
		HeaderInfo t = new HeaderInfo();

		t.minLength(123);
		assertObject(t.minLength()).isType(Integer.class).is(123);

		t.minLength("123");
		assertObject(t.minLength()).isType(Integer.class).is(123);

		t.minLength((String)null);
		assertObject(t.minLength()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#pattern(java.lang.Object)}.
	 */
	@Test
	public void a13_pattern() {
		HeaderInfo t = new HeaderInfo();

		t.pattern("foo");
		assertString(t.pattern()).is("foo");

		t.pattern(null);
		assertString(t.pattern()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#maxItems(java.lang.Object)}.
	 */
	@Test
	public void a14_maxItems() {
		HeaderInfo t = new HeaderInfo();

		t.maxItems(123);
		assertObject(t.maxItems()).isType(Integer.class).is(123);

		t.maxItems("123");
		assertObject(t.maxItems()).isType(Integer.class).is(123);

		t.maxItems((String)null);
		assertObject(t.maxItems()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#minItems(java.lang.Object)}.
	 */
	@Test
	public void a15_minItems() {
		HeaderInfo t = new HeaderInfo();

		t.minItems(123);
		assertObject(t.minItems()).isType(Integer.class).is(123);

		t.minItems("123");
		assertObject(t.minItems()).isType(Integer.class).is(123);

		t.minItems((String)null);
		assertObject(t.minItems()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#uniqueItems(java.lang.Object)}.
	 */
	@Test
	public void a16_uniqueItems() {
		HeaderInfo t = new HeaderInfo();

		t.uniqueItems(true);
		assertObject(t.uniqueItems()).isType(Boolean.class).is(true);

		t.uniqueItems("true");
		assertObject(t.uniqueItems()).isType(Boolean.class).is(true);

		t.uniqueItems((String)null);
		assertObject(t.uniqueItems()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#setEnum(java.util.Collection)}.
	 */
	@Test
	public void a17_enum() {
		HeaderInfo t = new HeaderInfo();

		t._enum(ASet.of("foo","bar"));
		assertObject(t._enum()).isType(Set.class).asJson().is("['foo','bar']");

		t._enum(ASet.of());
		assertObject(t._enum()).isType(Set.class).asJson().is("[]");

		t._enum("foo","bar");
		assertObject(t._enum()).isType(Set.class).asJson().is("['foo','bar']");

		t._enum(new Object[0]);
		assertObject(t._enum()).isType(Set.class).asJson().is("[]");

		t._enum((Collection<Object>)null);
		assertObject(t._enum()).isNull();

		t.addEnum(ASet.of("foo","bar"));
		assertObject(t.getEnum()).isType(Set.class).asJson().is("['foo','bar']");

		t.addEnum(ASet.of("baz"));
		assertObject(t.getEnum()).isType(Set.class).asJson().is("['foo','bar','baz']");

		t.addEnum(null);
		assertObject(t.getEnum()).isType(Set.class).asJson().is("['foo','bar','baz']");

		t.addEnum(ASet.of("foo","bar"));
		assertObject(t._enum()).isType(Set.class).asJson().is("['foo','bar','baz']");
	}

	/**
	 * Test method for {@link HeaderInfo#multipleOf(java.lang.Object)}.
	 */
	@Test
	public void a18_multipleOf() {
		HeaderInfo t = new HeaderInfo();

		t.multipleOf(123);
		assertObject(t.multipleOf()).isType(Integer.class).is(123);

		t.multipleOf(123f);
		assertObject(t.multipleOf()).isType(Float.class).is(123f);

		t.multipleOf("123");
		assertObject(t.multipleOf()).isType(Integer.class).is(123);

		t.multipleOf((String)null);
		assertObject(t.multipleOf()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#multipleOf(java.lang.Object)}.
	 */
	@Test
	public void a19_example() {
		HeaderInfo t = new HeaderInfo();

		t.example("foo");
		assertObject(t.example()).isType(String.class).is("foo");

		t.example(123f);
		assertObject(t.example()).isType(Float.class).is(123f);

		t.example((String)null);
		assertObject(t.example()).isNull();
	}


	/**
	 * Test method for {@link HeaderInfo#multipleOf(java.lang.Object)}.
	 */
	@Test
	public void a20_ref() {
		HeaderInfo t = new HeaderInfo();

		t.ref("foo");
		assertString(t.ref()).is("foo");

		t.ref(null);
		assertObject(t.ref()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		HeaderInfo t = new HeaderInfo();

		t
			.set("default", "a")
			.set("enum", ASet.of("b"))
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

		assertObject(t).asJson().is("{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}");

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

		assertObject(t).asJson().is("{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}");

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

		assertObject(t).asJson().is("{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}");

		assertObject(t.get("default", Object.class)).isType(StringBuilder.class).asString().is("a");
		assertObject(t.get("enum", Object.class)).isType(Set.class).asString().is("['b']");
		assertObject(t.get("collectionFormat", Object.class)).isType(String.class).is("c");
		assertObject(t.get("description", Object.class)).isType(String.class).is("d");
		assertObject(t.get("example", Object.class)).isType(StringBuilder.class).asString().is("e");
		assertObject(t.get("exclusiveMaximum", Object.class)).isType(Boolean.class).is(true);
		assertObject(t.get("exclusiveMinimum", Object.class)).isType(Boolean.class).is(true);
		assertObject(t.get("format", Object.class)).isType(String.class).is("g");
		assertObject(t.get("items", Object.class)).isType(Items.class).asJson().is("{type:'h'}");
		assertObject(t.get("maximum", Object.class)).isType(Float.class).is(123f);
		assertObject(t.get("maxItems", Object.class)).isType(Integer.class).is(123);
		assertObject(t.get("maxLength", Object.class)).isType(Integer.class).is(123);
		assertObject(t.get("minimum", Object.class)).isType(Float.class).is(123f);
		assertObject(t.get("minItems", Object.class)).isType(Integer.class).is(123);
		assertObject(t.get("minLength", Object.class)).isType(Integer.class).is(123);
		assertObject(t.get("multipleOf", Object.class)).isType(Float.class).is(123f);
		assertObject(t.get("pattern", Object.class)).isType(String.class).is("i");
		assertObject(t.get("type", Object.class)).isType(String.class).is("j");
		assertObject(t.get("uniqueItems", Object.class)).isType(Boolean.class).is(true);
		assertObject(t.get("$ref", Object.class)).isType(String.class).is("ref");

		t.set("null", null).set(null, "null");
		assertObject(t.get("null", Object.class)).isNull();
		assertObject(t.get(null, Object.class)).isNull();
		assertObject(t.get("foo", Object.class)).isNull();

		String s = "{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}";
		assertObject(JsonParser.DEFAULT.parse(s, HeaderInfo.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
		HeaderInfo t = new HeaderInfo();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("default", "a")
			.set("enum", ASet.of("b"))
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

		assertObject(t).asJson().is("{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		HeaderInfo t = new HeaderInfo();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("default", "a")
			.set("enum", ASet.of("b"))
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

		assertObject(t.keySet()).asJson().is("['collectionFormat','default','description','enum','example','exclusiveMaximum','exclusiveMinimum','format','items','maximum','maxItems','maxLength','minimum','minItems','minLength','multipleOf','pattern','$ref','type','uniqueItems']");
	}

	@Test
	public void c01_strict() throws Exception {
		HeaderInfo t = new HeaderInfo().strict();
		t.collectionFormat("csv");
		assertThrown(()->t.collectionFormat("foo")).contains("Invalid");
		t.type("string");
		assertThrown(()->t.type("foo")).contains("Invalid");
	}

	@Test
	public void c02_resolveRefs() throws Exception {
		Swagger s = new Swagger();
		HeaderInfo t = new HeaderInfo();
		t.resolveRefs(s, new ArrayDeque<String>(), 10);
	}
}
