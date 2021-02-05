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

import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link ParameterInfo}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ParameterInfo_Test {

	/**
	 * Test method for {@link ParameterInfo#name(java.lang.Object)}.
	 */
	@Test
	public void a01_name() {
		ParameterInfo t = new ParameterInfo();

		t.name("foo");
		assertString(t.name()).is("foo");

		t.name(null);
		assertString(t.name()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#in(java.lang.Object)}.
	 */
	@Test
	public void a02_in() {
		ParameterInfo t = new ParameterInfo();

		t.in("foo");
		assertString(t.in()).is("foo");

		t.in(null);
		assertString(t.in()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#description(java.lang.Object)}.
	 */
	@Test
	public void a03_description() {
		ParameterInfo t = new ParameterInfo();

		t.description("foo");
		assertString(t.description()).is("foo");

		t.description(null);
		assertString(t.description()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#required(java.lang.Object)}.
	 */
	@Test
	public void a04_required() {
		ParameterInfo t = new ParameterInfo();

		t.required(true);
		assertObject(t.required()).isType(Boolean.class).is(true);

		t.required("true");
		assertObject(t.required()).isType(Boolean.class).is(true);

		t.required((String)null);
		assertObject(t.required()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#schema(java.lang.Object)}.
	 */
	@Test
	public void a05_schema() {
		ParameterInfo t = new ParameterInfo();

		t.schema(schemaInfo().title("foo"));
		assertObject(t.schema()).asJson().is("{title:'foo'}");

		t.schema("{title:'foo'}");
		assertObject(t.schema()).isType(SchemaInfo.class).asJson().is("{title:'foo'}");

		t.schema((String)null);
		assertObject(t.schema()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#type(java.lang.Object)}.
	 */
	@Test
	public void a06_type() {
		ParameterInfo t = new ParameterInfo();

		t.type("foo");
		assertString(t.type()).is("foo");

		t.type(null);
		assertString(t.type()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#format(java.lang.Object)}.
	 */
	@Test
	public void a07_format() {
		ParameterInfo t = new ParameterInfo();

		t.format("foo");
		assertString(t.format()).is("foo");

		t.format(null);
		assertString(t.format()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#allowEmptyValue(java.lang.Object)}.
	 */
	@Test
	public void a08_allowEmptyValue() {
		ParameterInfo t = new ParameterInfo();

		t.allowEmptyValue(true);
		assertObject(t.allowEmptyValue()).isType(Boolean.class).is(true);

		t.allowEmptyValue("true");
		assertObject(t.allowEmptyValue()).isType(Boolean.class).is(true);

		t.allowEmptyValue((String)null);
		assertObject(t.allowEmptyValue()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#items(java.lang.Object)}.
	 */
	@Test
	public void a09_items() {
		ParameterInfo t = new ParameterInfo();

		t.items(items("foo"));
		assertObject(t.items()).asJson().is("{type:'foo'}");

		t.items("{type:'foo'}");
		assertObject(t.items()).isType(Items.class).asJson().is("{type:'foo'}");

		t.items((String)null);
		assertObject(t.items()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#collectionFormat(java.lang.Object)}.
	 */
	@Test
	public void a10_collectionFormat() {
		ParameterInfo t = new ParameterInfo();

		t.collectionFormat("foo");
		assertString(t.collectionFormat()).is("foo");

		t.collectionFormat(null);
		assertString(t.collectionFormat()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#_default(java.lang.Object)}.
	 */
	@Test
	public void a11_default() {
		ParameterInfo t = new ParameterInfo();

		t._default("foo");
		assertString(t._default()).is("foo");

		t._default(new StringBuilder("foo"));
		assertObject(t._default()).isType(StringBuilder.class).asString().is("foo");

		t._default(null);
		assertObject(t._default()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#maximum(java.lang.Object)}.
	 */
	@Test
	public void a12_maximum() {
		ParameterInfo t = new ParameterInfo();

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
	 * Test method for {@link ParameterInfo#exclusiveMaximum(java.lang.Object)}.
	 */
	@Test
	public void a13_exclusiveMaximum() {
		ParameterInfo t = new ParameterInfo();

		t.exclusiveMaximum(true);
		assertObject(t.exclusiveMaximum()).isType(Boolean.class).is(true);

		t.exclusiveMaximum("true");
		assertObject(t.exclusiveMaximum()).isType(Boolean.class).is(true);

		t.exclusiveMaximum((String)null);
		assertObject(t.exclusiveMaximum()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#minimum(java.lang.Object)}.
	 */
	@Test
	public void a14_minimum() {
		ParameterInfo t = new ParameterInfo();

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
	 * Test method for {@link ParameterInfo#exclusiveMinimum(java.lang.Object)}.
	 */
	@Test
	public void a15_exclusiveMinimum() {
		ParameterInfo t = new ParameterInfo();

		t.exclusiveMinimum(true);
		assertObject(t.exclusiveMinimum()).isType(Boolean.class).is(true);

		t.exclusiveMinimum("true");
		assertObject(t.exclusiveMinimum()).isType(Boolean.class).is(true);

		t.exclusiveMinimum((String)null);
		assertObject(t.exclusiveMinimum()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#maxLength(java.lang.Object)}.
	 */
	@Test
	public void a16_maxLength() {
		ParameterInfo t = new ParameterInfo();

		t.maxLength(123);
		assertObject(t.maxLength()).isType(Integer.class).is(123);

		t.maxLength("123");
		assertObject(t.maxLength()).isType(Integer.class).is(123);

		t.maxLength((String)null);
		assertObject(t.maxLength()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#minLength(java.lang.Object)}.
	 */
	@Test
	public void a17_minLength() {
		ParameterInfo t = new ParameterInfo();

		t.minLength(123);
		assertObject(t.minLength()).isType(Integer.class).is(123);

		t.minLength("123");
		assertObject(t.minLength()).isType(Integer.class).is(123);

		t.minLength((String)null);
		assertObject(t.minLength()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#pattern(java.lang.Object)}.
	 */
	@Test
	public void a18_pattern() {
		ParameterInfo t = new ParameterInfo();

		t.pattern("foo");
		assertString(t.pattern()).is("foo");

		t.pattern(null);
		assertString(t.pattern()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#maxItems(java.lang.Object)}.
	 */
	@Test
	public void a19_maxItems() {
		ParameterInfo t = new ParameterInfo();

		t.maxItems(123);
		assertObject(t.maxItems()).isType(Integer.class).is(123);

		t.maxItems("123");
		assertObject(t.maxItems()).isType(Integer.class).is(123);

		t.maxItems((String)null);
		assertObject(t.maxItems()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#minItems(java.lang.Object)}.
	 */
	@Test
	public void a20_minItems() {
		ParameterInfo t = new ParameterInfo();

		t.minItems(123);
		assertObject(t.minItems()).isType(Integer.class).is(123);

		t.minItems("123");
		assertObject(t.minItems()).isType(Integer.class).is(123);

		t.minItems((String)null);
		assertObject(t.minItems()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#uniqueItems(java.lang.Object)}.
	 */
	@Test
	public void a21_uniqueItems() {
		ParameterInfo t = new ParameterInfo();

		t.uniqueItems(true);
		assertObject(t.uniqueItems()).isType(Boolean.class).is(true);

		t.uniqueItems("true");
		assertObject(t.uniqueItems()).isType(Boolean.class).is(true);

		t.uniqueItems((String)null);
		assertObject(t.uniqueItems()).isNull();
	}

	/**
	 * Test method for {@link ParameterInfo#setEnum(java.util.Collection)}.
	 */
	@Test
	public void a22_enum() {
		ParameterInfo t = new ParameterInfo();

		t._enum(ASet.of("foo","bar"));
		assertObject(t._enum()).isType(Set.class).asJson().is("['foo','bar']");

		t._enum(ASet.of());
		assertObject(t._enum()).isType(Set.class).asJson().is("[]");

		t._enum((Collection<Object>)null);
		assertObject(t._enum()).isNull();

		t.addEnum(ASet.of("foo","bar"));
		assertObject(t._enum()).isType(Set.class).asJson().is("['foo','bar']");

		t.addEnum(ASet.of("baz"));
		assertObject(t._enum()).isType(Set.class).asJson().is("['foo','bar','baz']");

		t.addEnum(null);
		assertObject(t._enum()).isType(Set.class).asJson().is("['foo','bar','baz']");
	}

	/**
	 * Test method for {@link ParameterInfo#multipleOf(java.lang.Object)}.
	 */
	@Test
	public void a23_multipleOf() {
		ParameterInfo t = new ParameterInfo();

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
	 * Test method for {@link ParameterInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		ParameterInfo t = new ParameterInfo();

		t
			.set("default", "a")
			.set("enum", ASet.of("b"))
			.set("allowEmptyValue", true)
			.set("collectionFormat", "c")
			.set("description", "d")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "e")
			.set("in", "f")
			.set("items", items("g"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("multipleOf", 123f)
			.set("name", "h")
			.set("pattern", "i")
			.set("required", true)
			.set("schema", schemaInfo().title("j"))
			.set("type", "k")
			.set("uniqueItems", true)
			.set("$ref", "ref");

		assertObject(t).asJson().is("{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

		t
			.set("default", "a")
			.set("enum", "['b']")
			.set("allowEmptyValue", "true")
			.set("collectionFormat", "c")
			.set("description", "d")
			.set("exclusiveMaximum", "true")
			.set("exclusiveMinimum", "true")
			.set("format", "e")
			.set("in", "f")
			.set("items", "{type:'g'}")
			.set("maximum", "123.0")
			.set("maxItems", "123")
			.set("maxLength", "123")
			.set("minimum", "123.0")
			.set("minItems", "123")
			.set("minLength", "123")
			.set("multipleOf", "123.0")
			.set("name", "h")
			.set("pattern", "i")
			.set("required", "true")
			.set("schema", "{title:'j'}")
			.set("type", "k")
			.set("uniqueItems", "true")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

		t
			.set("default", new StringBuilder("a"))
			.set("enum", new StringBuilder("['b']"))
			.set("allowEmptyValue", new StringBuilder("true"))
			.set("collectionFormat", new StringBuilder("c"))
			.set("description", new StringBuilder("d"))
			.set("exclusiveMaximum", new StringBuilder("true"))
			.set("exclusiveMinimum", new StringBuilder("true"))
			.set("format", new StringBuilder("e"))
			.set("in", new StringBuilder("f"))
			.set("items", new StringBuilder("{type:'g'}"))
			.set("maximum", new StringBuilder("123.0"))
			.set("maxItems", new StringBuilder("123"))
			.set("maxLength", new StringBuilder("123"))
			.set("minimum", new StringBuilder("123.0"))
			.set("minItems", new StringBuilder("123"))
			.set("minLength", new StringBuilder("123"))
			.set("multipleOf", new StringBuilder("123.0"))
			.set("name", new StringBuilder("h"))
			.set("pattern", new StringBuilder("i"))
			.set("required", new StringBuilder("true"))
			.set("schema", new StringBuilder("{title:'j'}"))
			.set("type", new StringBuilder("k"))
			.set("uniqueItems", new StringBuilder("true"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).asJson().is("{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

		assertString(t.get("default", String.class)).is("a");
		assertString(t.get("enum", String.class)).is("['b']");
		assertString(t.get("allowEmptyValue", String.class)).is("true");
		assertString(t.get("collectionFormat", String.class)).is("c");
		assertString(t.get("description", String.class)).is("d");
		assertString(t.get("exclusiveMaximum", String.class)).is("true");
		assertString(t.get("exclusiveMinimum", String.class)).is("true");
		assertString(t.get("format", String.class)).is("e");
		assertString(t.get("in", String.class)).is("f");
		assertString(t.get("items", String.class)).is("{type:'g'}");
		assertString(t.get("maximum", String.class)).is("123.0");
		assertString(t.get("maxItems", String.class)).is("123");
		assertString(t.get("maxLength", String.class)).is("123");
		assertString(t.get("minimum", String.class)).is("123.0");
		assertString(t.get("minItems", String.class)).is("123");
		assertString(t.get("minLength", String.class)).is("123");
		assertString(t.get("multipleOf", String.class)).is("123.0");
		assertString(t.get("name", String.class)).is("h");
		assertString(t.get("pattern", String.class)).is("i");
		assertString(t.get("required", String.class)).is("true");
		assertString(t.get("schema", String.class)).is("{title:'j'}");
		assertString(t.get("type", String.class)).is("k");
		assertString(t.get("uniqueItems", String.class)).is("true");
		assertString(t.get("$ref", String.class)).is("ref");

		assertObject(t.get("default", Object.class)).isType(StringBuilder.class);
		assertObject(t.get("enum", Object.class)).isType(Set.class);
		assertObject(t.get("allowEmptyValue", Object.class)).isType(Boolean.class);
		assertObject(t.get("collectionFormat", Object.class)).isType(String.class);
		assertObject(t.get("description", Object.class)).isType(String.class);
		assertObject(t.get("exclusiveMaximum", Object.class)).isType(Boolean.class);
		assertObject(t.get("exclusiveMinimum", Object.class)).isType(Boolean.class);
		assertObject(t.get("format", Object.class)).isType(String.class);
		assertObject(t.get("in", Object.class)).isType(String.class);
		assertObject(t.get("items", Object.class)).isType(Items.class);
		assertObject(t.get("maximum", Object.class)).isType(Float.class);
		assertObject(t.get("maxItems", Object.class)).isType(Integer.class);
		assertObject(t.get("maxLength", Object.class)).isType(Integer.class);
		assertObject(t.get("minimum", Object.class)).isType(Float.class);
		assertObject(t.get("minItems", Object.class)).isType(Integer.class);
		assertObject(t.get("minLength", Object.class)).isType(Integer.class);
		assertObject(t.get("multipleOf", Object.class)).isType(Float.class);
		assertObject(t.get("name", Object.class)).isType(String.class);
		assertObject(t.get("pattern", Object.class)).isType(String.class);
		assertObject(t.get("required", Object.class)).isType(Boolean.class);
		assertObject(t.get("schema", Object.class)).isType(SchemaInfo.class);
		assertObject(t.get("type", Object.class)).isType(String.class);
		assertObject(t.get("uniqueItems", Object.class)).isType(Boolean.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, ParameterInfo.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
		ParameterInfo t = new ParameterInfo();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("default", "a")
			.set("enum", ASet.of("b"))
			.set("allowEmptyValue", true)
			.set("collectionFormat", "c")
			.set("description", "d")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "e")
			.set("in", "f")
			.set("items", items("g"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("multipleOf", 123f)
			.set("name", "h")
			.set("pattern", "i")
			.set("required", true)
			.set("schema", schemaInfo().title("j"))
			.set("type", "k")
			.set("uniqueItems", true)
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		ParameterInfo t = new ParameterInfo();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("allowEmptyValue", true)
			.set("collectionFormat", "c")
			.set("default", "a")
			.set("description", "d")
			.set("enum", ASet.of("b"))
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "e")
			.set("in", "f")
			.set("items", items("g"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("multipleOf", 123f)
			.set("name", "h")
			.set("pattern", "i")
			.set("required", true)
			.set("schema", schemaInfo().title("j"))
			.set("type", "k")
			.set("uniqueItems", true)
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is(
			"['allowEmptyValue','collectionFormat','default','description','enum','exclusiveMaximum','exclusiveMinimum',"
			+ "'format','in','items','maximum','maxItems','maxLength','minimum','minItems','minLength','multipleOf','name',"
			+ "'pattern','required','schema','type','uniqueItems','$ref']"
		);
	}
}
