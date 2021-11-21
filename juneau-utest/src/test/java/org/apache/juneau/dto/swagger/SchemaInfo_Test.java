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
 * Testcase for {@link SchemaInfo}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class SchemaInfo_Test {

	/**
	 * Test method for {@link SchemaInfo#format(java.lang.Object)}.
	 */
	@Test
	public void a01_format() {
		SchemaInfo t = new SchemaInfo();

		t.format("foo");
		assertString(t.format()).is("foo");

		t.format(null);
		assertString(t.format()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#title(java.lang.Object)}.
	 */
	@Test
	public void a02_title() {
		SchemaInfo t = new SchemaInfo();

		t.title("foo");
		assertString(t.title()).is("foo");

		t.title(null);
		assertString(t.title()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#description(java.lang.Object)}.
	 */
	@Test
	public void a03_description() {
		SchemaInfo t = new SchemaInfo();

		t.description("foo");
		assertString(t.description()).is("foo");

		t.description(null);
		assertString(t.description()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#_default(java.lang.Object)}.
	 */
	@Test
	public void a04_default() {
		SchemaInfo t = new SchemaInfo();

		t._default("foo");
		assertString(t._default()).is("foo");

		t._default(new StringBuilder("foo"));
		assertOptional(t._default()).isType(StringBuilder.class).asString().is("foo");

		t._default(null);
		assertOptional(t._default()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#multipleOf(java.lang.Object)}.
	 */
	@Test
	public void a05_multipleOf() {
		SchemaInfo t = new SchemaInfo();

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
	 * Test method for {@link SchemaInfo#maximum(java.lang.Object)}.
	 */
	@Test
	public void a06_maximum() {
		SchemaInfo t = new SchemaInfo();

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
	 * Test method for {@link SchemaInfo#exclusiveMaximum(java.lang.Object)}.
	 */
	@Test
	public void a07_exclusiveMaximum() {
		SchemaInfo t = new SchemaInfo();

		t.exclusiveMaximum(true);
		assertOptional(t.exclusiveMaximum()).isType(Boolean.class).is(true);

		t.exclusiveMaximum("true");
		assertOptional(t.exclusiveMaximum()).isType(Boolean.class).is(true);

		t.exclusiveMaximum((String)null);
		assertOptional(t.exclusiveMaximum()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#minimum(java.lang.Object)}.
	 */
	@Test
	public void a08_minimum() {
		SchemaInfo t = new SchemaInfo();

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
	 * Test method for {@link SchemaInfo#exclusiveMinimum(java.lang.Object)}.
	 */
	@Test
	public void a09_exclusiveMinimum() {
		SchemaInfo t = new SchemaInfo();

		t.exclusiveMinimum(true);
		assertOptional(t.exclusiveMinimum()).isType(Boolean.class).is(true);

		t.exclusiveMinimum("true");
		assertOptional(t.exclusiveMinimum()).isType(Boolean.class).is(true);

		t.exclusiveMinimum((String)null);
		assertOptional(t.exclusiveMinimum()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#maxLength(java.lang.Object)}.
	 */
	@Test
	public void a10_maxLength() {
		SchemaInfo t = new SchemaInfo();

		t.maxLength(123);
		assertOptional(t.maxLength()).isType(Integer.class).is(123);

		t.maxLength("123");
		assertOptional(t.maxLength()).isType(Integer.class).is(123);

		t.maxLength((String)null);
		assertOptional(t.maxLength()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#minLength(java.lang.Object)}.
	 */
	@Test
	public void a11_minLength() {
		SchemaInfo t = new SchemaInfo();

		t.minLength(123);
		assertOptional(t.minLength()).isType(Integer.class).is(123);

		t.minLength("123");
		assertOptional(t.minLength()).isType(Integer.class).is(123);

		t.minLength((String)null);
		assertOptional(t.minLength()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#pattern(java.lang.Object)}.
	 */
	@Test
	public void a12_pattern() {
		SchemaInfo t = new SchemaInfo();

		t.pattern("foo");
		assertString(t.pattern()).is("foo");

		t.pattern(null);
		assertString(t.pattern()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#maxItems(java.lang.Object)}.
	 */
	@Test
	public void a13_maxItems() {
		SchemaInfo t = new SchemaInfo();

		t.maxItems(123);
		assertOptional(t.maxItems()).isType(Integer.class).is(123);

		t.maxItems("123");
		assertOptional(t.maxItems()).isType(Integer.class).is(123);

		t.maxItems((String)null);
		assertOptional(t.maxItems()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#minItems(java.lang.Object)}.
	 */
	@Test
	public void a14_minItems() {
		SchemaInfo t = new SchemaInfo();

		t.minItems(123);
		assertOptional(t.minItems()).isType(Integer.class).is(123);

		t.minItems("123");
		assertOptional(t.minItems()).isType(Integer.class).is(123);

		t.minItems((String)null);
		assertOptional(t.minItems()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#uniqueItems(java.lang.Object)}.
	 */
	@Test
	public void a15_uniqueItems() {
		SchemaInfo t = new SchemaInfo();

		t.uniqueItems(true);
		assertOptional(t.uniqueItems()).isType(Boolean.class).is(true);

		t.uniqueItems("true");
		assertOptional(t.uniqueItems()).isType(Boolean.class).is(true);

		t.uniqueItems((String)null);
		assertOptional(t.uniqueItems()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#maxProperties(java.lang.Object)}.
	 */
	@Test
	public void a16_maxProperties() {
		SchemaInfo t = new SchemaInfo();

		t.maxProperties(123);
		assertOptional(t.maxProperties()).isType(Integer.class).is(123);

		t.maxProperties("123");
		assertOptional(t.maxProperties()).isType(Integer.class).is(123);

		t.maxProperties((String)null);
		assertOptional(t.maxProperties()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#minProperties(java.lang.Object)}.
	 */
	@Test
	public void a17_minProperties() {
		SchemaInfo t = new SchemaInfo();

		t.minProperties(123);
		assertOptional(t.minProperties()).isType(Integer.class).is(123);

		t.minProperties("123");
		assertOptional(t.minProperties()).isType(Integer.class).is(123);

		t.minProperties((String)null);
		assertOptional(t.minProperties()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#required(java.lang.Object)}.
	 */
	@Test
	public void a18_required() {
		SchemaInfo t = new SchemaInfo();

		t.requiredProperties("['x']");
		assertOptional(t.requiredProperties()).isType(Set.class).asJson().is("['x']");

		t.setRequiredProperties(null);
		assertOptional(t.requiredProperties()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#setEnum(java.util.Collection)}.
	 */
	@Test
	public void a19_enum() {
		SchemaInfo t = new SchemaInfo();

		t._enum(ASet.of("foo","bar"));
		assertOptional(t._enum()).isType(Set.class).asJson().is("['foo','bar']");

		t._enum(ASet.of());
		assertOptional(t._enum()).isType(Set.class).asJson().is("[]");

		t._enum((Collection<Object>)null);
		assertOptional(t._enum()).isNull();

		t.addEnum(ASet.of("foo","bar"));
		assertOptional(t._enum()).isType(Set.class).asJson().is("['foo','bar']");

		t.addEnum(ASet.of("baz"));
		assertOptional(t._enum()).isType(Set.class).asJson().is("['foo','bar','baz']");

		t.addEnum(null);
		assertOptional(t._enum()).isType(Set.class).asJson().is("['foo','bar','baz']");
	}

	/**
	 * Test method for {@link SchemaInfo#type(java.lang.Object)}.
	 */
	@Test
	public void a20_type() {
		SchemaInfo t = new SchemaInfo();

		t.type("foo");
		assertString(t.type()).is("foo");

		t.type(null);
		assertString(t.type()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#items(java.lang.Object)}.
	 */
	@Test
	public void a21_items() {
		SchemaInfo t = new SchemaInfo();

		t.items(items("foo"));
		assertOptional(t.items()).asJson().is("{type:'foo'}");

		t.items("{type:'foo'}");
		assertOptional(t.items()).isType(Items.class).asJson().is("{type:'foo'}");

		t.items((String)null);
		assertOptional(t.items()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#setAllOf(java.util.Collection)}.
	 */
	@Test
	public void a22_allOf() {
		SchemaInfo t = new SchemaInfo();

		t.allOf(ASet.of("foo","bar"));
		assertOptional(t.allOf()).isType(Set.class).asJson().is("['foo','bar']");

		t.allOf(ASet.of());
		assertOptional(t.allOf()).isType(Set.class).asJson().is("[]");

		t.allOf((Collection<Object>)null);
		assertOptional(t.allOf()).isNull();

		t.addAllOf(ASet.of("foo","bar"));
		assertOptional(t.allOf()).isType(Set.class).asJson().is("['foo','bar']");

		t.addAllOf(ASet.of());
		assertOptional(t.allOf()).isType(Set.class).asJson().is("['foo','bar']");

		t.addAllOf(null);
		assertOptional(t.allOf()).isType(Set.class).asJson().is("['foo','bar']");
	}

	/**
	 * Test method for {@link SchemaInfo#setProperties(java.util.Map)}.
	 */
	@Test
	public void a23_properties() {
		SchemaInfo t = new SchemaInfo();

		t.properties(AMap.of("foo",new SchemaInfo().type("foo")));
		assertOptional(t.properties()).isType(Map.class).asJson().is("{foo:{type:'foo'}}");

		t.properties(AMap.create());
		assertOptional(t.properties()).isType(Map.class).asJson().is("{}");

		t.properties((Map<String,SchemaInfo>)null);
		assertOptional(t.properties()).isNull();

		t.addProperties(AMap.of("foo", new SchemaInfo().type("foo")));
		assertOptional(t.properties()).isType(Map.class).asJson().is("{foo:{type:'foo'}}");

		t.addProperties(AMap.create());
		assertOptional(t.properties()).isType(Map.class).asJson().is("{foo:{type:'foo'}}");

		t.addProperties(null);
		assertOptional(t.properties()).isType(Map.class).asJson().is("{foo:{type:'foo'}}");
	}

	/**
	 * Test method for {@link SchemaInfo#setAdditionalProperties(java.util.Map)}.
	 */
	@Test
	public void a24_additionalProperties() {
		SchemaInfo t = new SchemaInfo();

		t.additionalProperties(new SchemaInfo().type("foo"));
		assertOptional(t.additionalProperties()).isType(SchemaInfo.class).asJson().is("{type:'foo'}");

		t.additionalProperties(new SchemaInfo());
		assertOptional(t.additionalProperties()).isType(SchemaInfo.class).asJson().is("{}");

		t.additionalProperties((SchemaInfo)null);
		assertOptional(t.additionalProperties()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#discriminator(java.lang.Object)}.
	 */
	@Test
	public void a25_discriminator() {
		SchemaInfo t = new SchemaInfo();

		t.discriminator("foo");
		assertString(t.discriminator()).is("foo");

		t.discriminator(null);
		assertString(t.discriminator()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#readOnly(java.lang.Object)}.
	 */
	@Test
	public void a26_readOnly() {
		SchemaInfo t = new SchemaInfo();

		t.readOnly(true);
		assertOptional(t.readOnly()).isType(Boolean.class).is(true);

		t.readOnly("true");
		assertOptional(t.readOnly()).isType(Boolean.class).is(true);

		t.readOnly((String)null);
		assertOptional(t.readOnly()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#xml(java.lang.Object)}.
	 */
	@Test
	public void a27_xml() {
		SchemaInfo t = new SchemaInfo();

		t.xml(xml().name("foo"));
		assertOptional(t.xml()).asJson().is("{name:'foo'}");

		t.xml("{name:'foo'}");
		assertOptional(t.xml()).isType(Xml.class).asJson().is("{name:'foo'}");

		t.xml((String)null);
		assertOptional(t.xml()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#externalDocs(java.lang.Object)}.
	 */
	@Test
	public void a28_xternalDocs() {
		SchemaInfo t = new SchemaInfo();

		t.externalDocs(externalDocumentation("foo"));
		assertOptional(t.externalDocs()).asJson().is("{url:'foo'}");

		t.externalDocs("{url:'foo'}");
		assertOptional(t.externalDocs()).isType(ExternalDocumentation.class).asJson().is("{url:'foo'}");

		t.externalDocs((String)null);
		assertOptional(t.externalDocs()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#example(java.lang.Object)}.
	 */
	@Test
	public void a29_example() {
		SchemaInfo t = new SchemaInfo();

		t.example("foo");
		assertObject(t.getExample()).is("foo");

		t.example(123);
		assertObject(t.getExample()).is(123);

		t.example(null);
		assertObject(t.getExample()).isNull();
	}

	/**
	 * Test method for {@link SchemaInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		SchemaInfo t = new SchemaInfo();

		t
			.set("default", "a")
			.set("enum", ASet.of("b"))
			.set("additionalProperties", AMap.of("c",AList.of("c1")))
			.set("allOf", ASet.of("d"))
			.set("description", "e")
			.set("discriminator", "f")
			.set("example", "g")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("externalDocs", externalDocumentation("h"))
			.set("format", "i")
			.set("items", items("j"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("maxProperties", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("minProperties", 123)
			.set("multipleOf", 123f)
			.set("pattern", "k")
			.set("properties", AMap.of("l",AMap.of("l1", 1)))
			.set("readOnly", true)
			.set("requiredProperties", ASet.of("x"))
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", true)
			.set("xml", xml().name("o"))
			.set("$ref", "ref");

		assertObject(t).asJson().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");

		t
			.set("default", "a")
			.set("enum", "['b']")
			.set("additionalProperties", "{c:['c1']}")
			.set("allOf", "['d']")
			.set("description", "e")
			.set("discriminator", "f")
			.set("example", "g")
			.set("exclusiveMaximum", "true")
			.set("exclusiveMinimum", "true")
			.set("externalDocs", "{url:'h'}")
			.set("format", "i")
			.set("items", "{type:'j'}")
			.set("maximum", "123.0")
			.set("maxItems", "123")
			.set("maxLength", "123")
			.set("maxProperties", "123")
			.set("minimum", "123.0")
			.set("minItems", "123")
			.set("minLength", "123")
			.set("minProperties", "123")
			.set("multipleOf", "123.0")
			.set("pattern", "k")
			.set("properties", "{l:{l1:1}}")
			.set("readOnly", "true")
			.set("requiredProperties", "['x']")
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", "true")
			.set("xml", "{name:'o'}")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");

		t
			.set("default", new StringBuilder("a"))
			.set("enum", new StringBuilder("['b']"))
			.set("additionalProperties", new StringBuilder("{c:['c1']}"))
			.set("allOf", new StringBuilder("['d']"))
			.set("description", new StringBuilder("e"))
			.set("discriminator", new StringBuilder("f"))
			.set("example", new StringBuilder("g"))
			.set("exclusiveMaximum", new StringBuilder("true"))
			.set("exclusiveMinimum", new StringBuilder("true"))
			.set("externalDocs", new StringBuilder("{url:'h'}"))
			.set("format", new StringBuilder("i"))
			.set("items", new StringBuilder("{type:'j'}"))
			.set("maximum", new StringBuilder("123.0"))
			.set("maxItems", new StringBuilder("123"))
			.set("maxLength", new StringBuilder("123"))
			.set("maxProperties", new StringBuilder("123"))
			.set("minimum", new StringBuilder("123.0"))
			.set("minItems", new StringBuilder("123"))
			.set("minLength", new StringBuilder("123"))
			.set("minProperties", new StringBuilder("123"))
			.set("multipleOf", new StringBuilder("123.0"))
			.set("pattern", new StringBuilder("k"))
			.set("properties", new StringBuilder("{l:{l1:1}}"))
			.set("readOnly", new StringBuilder("true"))
			.set("requiredProperties", new StringBuilder("['x']"))
			.set("title", new StringBuilder("m"))
			.set("type", new StringBuilder("n"))
			.set("uniqueItems", new StringBuilder("true"))
			.set("xml", new StringBuilder("{name:'o'}"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).asJson().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");

		assertString(t.get("default", String.class)).is("a");
		assertString(t.get("enum", String.class)).is("['b']");
		assertString(t.get("additionalProperties", String.class)).is("{c:['c1']}");
		assertString(t.get("allOf", String.class)).is("['d']");
		assertString(t.get("description", String.class)).is("e");
		assertString(t.get("discriminator", String.class)).is("f");
		assertString(t.get("example", String.class)).is("g");
		assertString(t.get("exclusiveMaximum", String.class)).is("true");
		assertString(t.get("exclusiveMinimum", String.class)).is("true");
		assertString(t.get("externalDocs", String.class)).is("{url:'h'}");
		assertString(t.get("format", String.class)).is("i");
		assertString(t.get("items", String.class)).is("{type:'j'}");
		assertString(t.get("maximum", String.class)).is("123.0");
		assertString(t.get("maxItems", String.class)).is("123");
		assertString(t.get("maxLength", String.class)).is("123");
		assertString(t.get("maxProperties", String.class)).is("123");
		assertString(t.get("minimum", String.class)).is("123.0");
		assertString(t.get("minItems", String.class)).is("123");
		assertString(t.get("minLength", String.class)).is("123");
		assertString(t.get("minProperties", String.class)).is("123");
		assertString(t.get("multipleOf", String.class)).is("123.0");
		assertString(t.get("pattern", String.class)).is("k");
		assertString(t.get("properties", String.class)).is("{l:{l1:1}}");
		assertString(t.get("readOnly", String.class)).is("true");
		assertString(t.get("requiredProperties", String.class)).is("['x']");
		assertString(t.get("title", String.class)).is("m");
		assertString(t.get("type", String.class)).is("n");
		assertString(t.get("uniqueItems", String.class)).is("true");
		assertString(t.get("xml", String.class)).is("{name:'o'}");
		assertString(t.get("$ref", String.class)).is("ref");

		assertObject(t.get("default", Object.class)).isType(StringBuilder.class);
		assertObject(t.get("enum", Object.class)).isType(Set.class);
		assertObject(t.get("additionalProperties", Object.class)).isType(SchemaInfo.class);
		assertObject(t.get("allOf", Object.class)).isType(Set.class);
		assertObject(t.get("description", Object.class)).isType(String.class);
		assertObject(t.get("discriminator", Object.class)).isType(String.class);
		assertObject(t.get("example", Object.class)).isType(StringBuilder.class);
		assertObject(t.get("exclusiveMaximum", Object.class)).isType(Boolean.class);
		assertObject(t.get("exclusiveMinimum", Object.class)).isType(Boolean.class);
		assertObject(t.get("externalDocs", Object.class)).isType(ExternalDocumentation.class);
		assertObject(t.get("format", Object.class)).isType(String.class);
		assertObject(t.get("items", Object.class)).isType(Items.class);
		assertObject(t.get("maximum", Object.class)).isType(Float.class);
		assertObject(t.get("maxItems", Object.class)).isType(Integer.class);
		assertObject(t.get("maxLength", Object.class)).isType(Integer.class);
		assertObject(t.get("maxProperties", Object.class)).isType(Integer.class);
		assertObject(t.get("minimum", Object.class)).isType(Float.class);
		assertObject(t.get("minItems", Object.class)).isType(Integer.class);
		assertObject(t.get("minLength", Object.class)).isType(Integer.class);
		assertObject(t.get("minProperties", Object.class)).isType(Integer.class);
		assertObject(t.get("multipleOf", Object.class)).isType(Float.class);
		assertObject(t.get("pattern", Object.class)).isType(String.class);
		assertObject(t.get("properties", Object.class)).isType(Map.class);
		assertObject(t.get("readOnly", Object.class)).isType(Boolean.class);
		assertObject(t.get("requiredProperties", Object.class)).isType(Set.class);
		assertObject(t.get("title", Object.class)).isType(String.class);
		assertObject(t.get("type", Object.class)).isType(String.class);
		assertObject(t.get("uniqueItems", Object.class)).isType(Boolean.class);
		assertObject(t.get("xml", Object.class)).isType(Xml.class);
		assertObject(t.get("$ref", Object.class)).isType(String.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, SchemaInfo.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
		SchemaInfo t = new SchemaInfo();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("default", "a")
			.set("enum", ASet.of("b"))
			.set("additionalProperties", AMap.of("c",AList.of("c1")))
			.set("allOf", ASet.of("d"))
			.set("description", "e")
			.set("discriminator", "f")
			.set("example", "g")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("externalDocs", externalDocumentation("h"))
			.set("format", "i")
			.set("items", items("j"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("maxProperties", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("minProperties", 123)
			.set("multipleOf", 123f)
			.set("pattern", "k")
			.set("properties", AMap.of("l",AMap.of("l1", 1)))
			.set("readOnly", true)
			.set("requiredProperties", ASet.of("x"))
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", true)
			.set("xml", xml().name("o"))
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		SchemaInfo t = new SchemaInfo();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("additionalProperties", AMap.of("c",AList.of("c1")))
			.set("allOf", ASet.of("d"))
			.set("default", "a")
			.set("description", "e")
			.set("discriminator", "f")
			.set("enum", ASet.of("b"))
			.set("example", "g")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("externalDocs", externalDocumentation("h"))
			.set("format", "i")
			.set("items", items("j"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("maxProperties", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("minProperties", 123)
			.set("multipleOf", 123f)
			.set("pattern", "k")
			.set("properties", AMap.of("l",AMap.of("l1", 1)))
			.set("readOnly", true)
			.set("$ref", "ref")
			.set("requiredProperties", ASet.of("x"))
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", true)
			.set("xml", xml().name("o"));

		assertObject(t.keySet()).asJson().is(
			"['additionalProperties','allOf','default','description','discriminator','enum','example','exclusiveMaximum','exclusiveMinimum',"
			+ "'externalDocs','format','items','maximum','maxItems','maxLength','maxProperties','minimum','minItems','minLength','minProperties',"
			+ "'multipleOf','pattern','properties','readOnly','$ref','requiredProperties','title','type','uniqueItems','xml']"
		);
	}
}
