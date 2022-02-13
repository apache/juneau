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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Items}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Items_Test {

	/**
	 * Test method for {@link Items#type(java.lang.Object)}.
	 */
	@Test
	public void a01_type() {
		Items t = new Items();

		t.type("foo");
		assertString(t.type()).is("foo");

		t.type(null);
		assertString(t.type()).isNull();
	}

	/**
	 * Test method for {@link Items#format(java.lang.Object)}.
	 */
	@Test
	public void a02_format() {
		Items t = new Items();

		t.format("foo");
		assertString(t.format()).is("foo");

		t.format(null);
		assertString(t.format()).isNull();
	}

	/**
	 * Test method for {@link Items#items(java.lang.Object)}.
	 */
	@Test
	public void a03_items() {
		Items t = new Items();

		t.items(items("foo"));
		assertOptional(t.items()).asJson().is("{type:'foo'}");

		t.items("{type:'foo'}");
		assertOptional(t.items()).isType(Items.class).asJson().is("{type:'foo'}");

		t.items((String)null);
		assertOptional(t.items()).isNull();
	}

	/**
	 * Test method for {@link Items#collectionFormat(java.lang.Object)}.
	 */
	@Test
	public void a04_collectionFormat() {
		Items t = new Items();

		t.collectionFormat("foo");
		assertString(t.collectionFormat()).is("foo");

		t.collectionFormat(null);
		assertString(t.collectionFormat()).isNull();
	}

	/**
	 * Test method for {@link Items#_default(java.lang.Object)}.
	 */
	@Test
	public void a05_default() {
		Items t = new Items();

		t._default("foo");
		assertOptional(t._default()).is("foo");

		t._default(new StringBuilder("foo"));
		assertOptional(t._default()).isType(StringBuilder.class).asString().is("foo");

		t._default(null);
		assertOptional(t._default()).isNull();
	}

	/**
	 * Test method for {@link Items#maximum(java.lang.Object)}.
	 */
	@Test
	public void a06_maximum() {
		Items t = new Items();

		t.maximum(123);
		assertOptional(t.maximum()).isType(Integer.class).is(123);

		t.maximum(123f);
		assertOptional(t.maximum()).isType(Float.class).is(123f);

		t.maximum("123");
		assertOptional(t.maximum()).isType(Integer.class).is(123);

		t.maximum((String)null);
		assertOptional(t.maximum()).isNull();
	}

	/**
	 * Test method for {@link Items#exclusiveMaximum(java.lang.Object)}.
	 */
	@Test
	public void a07_exclusiveMaximum() {
		Items t = new Items();

		t.exclusiveMaximum(true);
		assertOptional(t.exclusiveMaximum()).isType(Boolean.class).is(true);

		t.exclusiveMaximum("true");
		assertOptional(t.exclusiveMaximum()).isType(Boolean.class).is(true);

		t.exclusiveMaximum((String)null);
		assertOptional(t.exclusiveMaximum()).isNull();
	}

	/**
	 * Test method for {@link Items#minimum(java.lang.Object)}.
	 */
	@Test
	public void a08_minimum() {
		Items t = new Items();

		t.minimum(123);
		assertOptional(t.minimum()).isType(Integer.class).is(123);

		t.minimum(123f);
		assertOptional(t.minimum()).isType(Float.class).is(123f);

		t.minimum("123");
		assertOptional(t.minimum()).isType(Integer.class).is(123);

		t.minimum((String)null);
		assertOptional(t.minimum()).isNull();
	}

	/**
	 * Test method for {@link Items#exclusiveMinimum(java.lang.Object)}.
	 */
	@Test
	public void a09_exclusiveMinimum() {
		Items t = new Items();

		t.exclusiveMinimum(true);
		assertOptional(t.exclusiveMinimum()).isType(Boolean.class).is(true);

		t.exclusiveMinimum("true");
		assertOptional(t.exclusiveMinimum()).isType(Boolean.class).is(true);

		t.exclusiveMinimum((String)null);
		assertOptional(t.exclusiveMinimum()).isNull();
	}

	/**
	 * Test method for {@link Items#maxLength(java.lang.Object)}.
	 */
	@Test
	public void a10_maxLength() {
		Items t = new Items();

		t.maxLength(123);
		assertOptional(t.maxLength()).isType(Integer.class).is(123);

		t.maxLength("123");
		assertOptional(t.maxLength()).isType(Integer.class).is(123);

		t.maxLength((String)null);
		assertOptional(t.maxLength()).isNull();
	}

	/**
	 * Test method for {@link Items#minLength(java.lang.Object)}.
	 */
	@Test
	public void a11_minLength() {
		Items t = new Items();

		t.minLength(123);
		assertOptional(t.minLength()).isType(Integer.class).is(123);

		t.minLength("123");
		assertOptional(t.minLength()).isType(Integer.class).is(123);

		t.minLength((String)null);
		assertOptional(t.minLength()).isNull();
	}

	/**
	 * Test method for {@link Items#pattern(java.lang.Object)}.
	 */
	@Test
	public void a12_pattern() {
		Items t = new Items();

		t.pattern("foo");
		assertString(t.pattern()).is("foo");

		t.pattern(null);
		assertString(t.pattern()).isNull();
	}

	/**
	 * Test method for {@link Items#maxItems(java.lang.Object)}.
	 */
	@Test
	public void a13_maxItems() {
		Items t = new Items();

		t.maxItems(123);
		assertOptional(t.maxItems()).isType(Integer.class).is(123);

		t.maxItems("123");
		assertOptional(t.maxItems()).isType(Integer.class).is(123);

		t.maxItems((String)null);
		assertOptional(t.maxItems()).isNull();
	}

	/**
	 * Test method for {@link Items#minItems(java.lang.Object)}.
	 */
	@Test
	public void a14_minItems() {
		Items t = new Items();

		t.minItems(123);
		assertOptional(t.minItems()).isType(Integer.class).is(123);

		t.minItems("123");
		assertOptional(t.minItems()).isType(Integer.class).is(123);

		t.minItems((String)null);
		assertOptional(t.minItems()).isNull();
	}

	/**
	 * Test method for {@link Items#uniqueItems(java.lang.Object)}.
	 */
	@Test
	public void a15_uniqueItems() {
		Items t = new Items();

		t.uniqueItems(true);
		assertOptional(t.uniqueItems()).isType(Boolean.class).is(true);

		t.uniqueItems("true");
		assertOptional(t.uniqueItems()).isType(Boolean.class).is(true);

		t.uniqueItems((String)null);
		assertOptional(t.uniqueItems()).isNull();
	}

	/**
	 * Test method for {@link Items#setEnum(java.util.Collection)}.
	 */
	@Test
	public void a16_enum() {
		Items t = new Items();

		t._enum(ASet.of("foo","bar"));
		assertOptional(t._enum()).isType(Set.class).asJson().is("['foo','bar']");

		t._enum("foo","baz");
		assertOptional(t._enum()).isType(Set.class).asJson().is("['foo','baz']");

		t._enum(ASet.of());
		assertOptional(t._enum()).isType(Set.class).asJson().is("[]");

		t._enum((Collection<Object>)null);
		assertOptional(t._enum()).isNull();

		t.addEnum(ASet.of("foo","bar"));
		assertObject(t.getEnum()).isType(Set.class).asJson().is("['foo','bar']");

		t.addEnum(ASet.of("baz"));
		assertObject(t.getEnum()).isType(Set.class).asJson().is("['foo','bar','baz']");

		t.addEnum(null);
		assertObject(t.getEnum()).isType(Set.class).asJson().is("['foo','bar','baz']");
	}

	/**
	 * Test method for {@link Items#multipleOf(java.lang.Object)}.
	 */
	@Test
	public void a17_multipleOf() {
		Items t = new Items();

		t.multipleOf(123);
		assertOptional(t.multipleOf()).isType(Integer.class).is(123);

		t.multipleOf(123f);
		assertOptional(t.multipleOf()).isType(Float.class).is(123f);

		t.multipleOf("123");
		assertOptional(t.multipleOf()).isType(Integer.class).is(123);

		t.multipleOf((String)null);
		assertOptional(t.multipleOf()).isNull();
	}

	/**
	 * Test method for {@link Items#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		Items t = new Items();

		t
			.set("default", "a")
			.set("enum", ASet.of("b"))
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

		assertObject(t).asJson().is("{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

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

		assertObject(t).asJson().is("{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

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

		assertObject(t).asJson().is("{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

		assertString(t.get("default", String.class)).is("a");
		assertString(t.get("enum", String.class)).is("['b']");
		assertString(t.get("collectionFormat", String.class)).is("c");
		assertString(t.get("exclusiveMaximum", String.class)).is("true");
		assertString(t.get("exclusiveMinimum", String.class)).is("true");
		assertString(t.get("format", String.class)).is("g");
		assertString(t.get("items", String.class)).is("{type:'h'}");
		assertString(t.get("maximum", String.class)).is("123.0");
		assertString(t.get("maxItems", String.class)).is("123");
		assertString(t.get("maxLength", String.class)).is("123");
		assertString(t.get("minimum", String.class)).is("123.0");
		assertString(t.get("minItems", String.class)).is("123");
		assertString(t.get("minLength", String.class)).is("123");
		assertString(t.get("multipleOf", String.class)).is("123.0");
		assertString(t.get("pattern", String.class)).is("i");
		assertString(t.get("type", String.class)).is("j");
		assertString(t.get("uniqueItems", String.class)).is("true");
		assertString(t.get("$ref", String.class)).is("ref");

		assertObject(t.get("default", Object.class)).isType(StringBuilder.class);
		assertObject(t.get("enum", Object.class)).isType(Set.class);
		assertObject(t.get("collectionFormat", Object.class)).isType(String.class);
		assertObject(t.get("exclusiveMaximum", Object.class)).isType(Boolean.class);
		assertObject(t.get("exclusiveMinimum", Object.class)).isType(Boolean.class);
		assertObject(t.get("format", Object.class)).isType(String.class);
		assertObject(t.get("items", Object.class)).isType(Items.class);
		assertObject(t.get("maximum", Object.class)).isType(Float.class);
		assertObject(t.get("maxItems", Object.class)).isType(Integer.class);
		assertObject(t.get("maxLength", Object.class)).isType(Integer.class);
		assertObject(t.get("minimum", Object.class)).isType(Float.class);
		assertObject(t.get("minItems", Object.class)).isType(Integer.class);
		assertObject(t.get("minLength", Object.class)).isType(Integer.class);
		assertObject(t.get("multipleOf", Object.class)).isType(Float.class);
		assertObject(t.get("pattern", Object.class)).isType(String.class);
		assertObject(t.get("type", Object.class)).isType(String.class);
		assertObject(t.get("uniqueItems", Object.class)).isType(Boolean.class);
		assertObject(t.get("$ref", Object.class)).isType(String.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, Items.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
		Items t = new Items();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("default", "a")
			.set("enum", ASet.of("b"))
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

		assertObject(t).asJson().is("{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		Items t = new Items();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("collectionFormat", "c")
			.set("default", "a")
			.set("enum", ASet.of("b"))
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

		assertObject(t.keySet()).asJson().is(
			"['collectionFormat','default','enum','exclusiveMaximum','exclusiveMinimum','format','items','maximum','maxItems',"
			+ "'maxLength','minimum','minItems','minLength','multipleOf','pattern','$ref','type','uniqueItems']"
		);
	}
}
