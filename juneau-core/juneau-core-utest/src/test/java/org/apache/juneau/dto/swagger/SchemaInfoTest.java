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
public class SchemaInfoTest {

	/**
	 * Test method for {@link SchemaInfo#format(java.lang.Object)}.
	 */
	@Test
	public void testFormat() {
		SchemaInfo t = new SchemaInfo();

		t.format("foo");
		assertEquals("foo", t.getFormat());

		t.format(new StringBuilder("foo"));
		assertEquals("foo", t.getFormat());
		assertObject(t.getFormat()).isType(String.class);

		t.format(null);
		assertNull(t.getFormat());
	}

	/**
	 * Test method for {@link SchemaInfo#title(java.lang.Object)}.
	 */
	@Test
	public void testTitle() {
		SchemaInfo t = new SchemaInfo();

		t.title("foo");
		assertEquals("foo", t.getTitle());

		t.title(new StringBuilder("foo"));
		assertEquals("foo", t.getTitle());
		assertObject(t.getTitle()).isType(String.class);

		t.title(null);
		assertNull(t.getTitle());
	}

	/**
	 * Test method for {@link SchemaInfo#description(java.lang.Object)}.
	 */
	@Test
	public void testDescription() {
		SchemaInfo t = new SchemaInfo();

		t.description("foo");
		assertEquals("foo", t.getDescription());

		t.description(new StringBuilder("foo"));
		assertEquals("foo", t.getDescription());
		assertObject(t.getDescription()).isType(String.class);

		t.description(null);
		assertNull(t.getDescription());
	}

	/**
	 * Test method for {@link SchemaInfo#_default(java.lang.Object)}.
	 */
	@Test
	public void test_default() {
		SchemaInfo t = new SchemaInfo();

		t._default("foo");
		assertEquals("foo", t.getDefault());

		t._default(new StringBuilder("foo"));
		assertEquals("foo", t.getDefault().toString());
		assertObject(t.getDefault()).isType(StringBuilder.class);

		t._default(null);
		assertNull(t.getDefault());
	}

	/**
	 * Test method for {@link SchemaInfo#multipleOf(java.lang.Object)}.
	 */
	@Test
	public void testMultipleOf() {
		SchemaInfo t = new SchemaInfo();

		t.multipleOf(123);
		assertEquals(123, t.getMultipleOf());
		assertObject(t.getMultipleOf()).isType(Integer.class);

		t.multipleOf(123f);
		assertEquals(123f, t.getMultipleOf());
		assertObject(t.getMultipleOf()).isType(Float.class);

		t.multipleOf("123");
		assertEquals(123, t.getMultipleOf());
		assertObject(t.getMultipleOf()).isType(Integer.class);

		t.multipleOf(new StringBuilder("123"));
		assertEquals(123, t.getMultipleOf());
		assertObject(t.getMultipleOf()).isType(Integer.class);

		t.multipleOf(null);
		assertNull(t.getMultipleOf());
	}

	/**
	 * Test method for {@link SchemaInfo#maximum(java.lang.Object)}.
	 */
	@Test
	public void testMaximum() {
		SchemaInfo t = new SchemaInfo();

		t.maximum(123);
		assertEquals(123, t.getMaximum());
		assertObject(t.getMaximum()).isType(Integer.class);

		t.maximum(123f);
		assertEquals(123f, t.getMaximum());
		assertObject(t.getMaximum()).isType(Float.class);

		t.maximum("123");
		assertEquals(123, t.getMaximum());
		assertObject(t.getMaximum()).isType(Integer.class);

		t.maximum(new StringBuilder("123"));
		assertEquals(123, t.getMaximum());
		assertObject(t.getMaximum()).isType(Integer.class);

		t.maximum(null);
		assertNull(t.getMaximum());
	}

	/**
	 * Test method for {@link SchemaInfo#exclusiveMaximum(java.lang.Object)}.
	 */
	@Test
	public void testExclusiveMaximum() {
		SchemaInfo t = new SchemaInfo();

		t.exclusiveMaximum(true);
		assertEquals(true, t.getExclusiveMaximum());
		assertObject(t.getExclusiveMaximum()).isType(Boolean.class);

		t.exclusiveMaximum("true");
		assertEquals(true, t.getExclusiveMaximum());
		assertObject(t.getExclusiveMaximum()).isType(Boolean.class);

		t.exclusiveMaximum(new StringBuilder("true"));
		assertEquals(true, t.getExclusiveMaximum());
		assertObject(t.getExclusiveMaximum()).isType(Boolean.class);

		t.exclusiveMaximum(null);
		assertNull(t.getExclusiveMaximum());
	}

	/**
	 * Test method for {@link SchemaInfo#minimum(java.lang.Object)}.
	 */
	@Test
	public void testMinimum() {
		SchemaInfo t = new SchemaInfo();

		t.minimum(123);
		assertEquals(123, t.getMinimum());
		assertObject(t.getMinimum()).isType(Integer.class);

		t.minimum(123f);
		assertEquals(123f, t.getMinimum());
		assertObject(t.getMinimum()).isType(Float.class);

		t.minimum("123");
		assertEquals(123, t.getMinimum());
		assertObject(t.getMinimum()).isType(Integer.class);

		t.minimum(new StringBuilder("123"));
		assertEquals(123, t.getMinimum());
		assertObject(t.getMinimum()).isType(Integer.class);

		t.minimum(null);
		assertNull(t.getMinimum());
	}

	/**
	 * Test method for {@link SchemaInfo#exclusiveMinimum(java.lang.Object)}.
	 */
	@Test
	public void testExclusiveMinimum() {
		SchemaInfo t = new SchemaInfo();

		t.exclusiveMinimum(true);
		assertEquals(true, t.getExclusiveMinimum());
		assertObject(t.getExclusiveMinimum()).isType(Boolean.class);

		t.exclusiveMinimum("true");
		assertEquals(true, t.getExclusiveMinimum());
		assertObject(t.getExclusiveMinimum()).isType(Boolean.class);

		t.exclusiveMinimum(new StringBuilder("true"));
		assertEquals(true, t.getExclusiveMinimum());
		assertObject(t.getExclusiveMinimum()).isType(Boolean.class);

		t.exclusiveMinimum(null);
		assertNull(t.getExclusiveMinimum());
	}

	/**
	 * Test method for {@link SchemaInfo#maxLength(java.lang.Object)}.
	 */
	@Test
	public void testMaxLength() {
		SchemaInfo t = new SchemaInfo();

		t.maxLength(123);
		assertEquals(123, t.getMaxLength().intValue());
		assertObject(t.getMaxLength()).isType(Integer.class);

		t.maxLength(123f);
		assertEquals(123, t.getMaxLength().intValue());
		assertObject(t.getMaxLength()).isType(Integer.class);

		t.maxLength("123");
		assertEquals(123, t.getMaxLength().intValue());
		assertObject(t.getMaxLength()).isType(Integer.class);

		t.maxLength(new StringBuilder("123"));
		assertEquals(123, t.getMaxLength().intValue());
		assertObject(t.getMaxLength()).isType(Integer.class);

		t.maxLength(null);
		assertNull(t.getMaxLength());
	}

	/**
	 * Test method for {@link SchemaInfo#minLength(java.lang.Object)}.
	 */
	@Test
	public void testMinLength() {
		SchemaInfo t = new SchemaInfo();

		t.minLength(123);
		assertEquals(123, t.getMinLength().intValue());
		assertObject(t.getMinLength()).isType(Integer.class);

		t.minLength(123f);
		assertEquals(123, t.getMinLength().intValue());
		assertObject(t.getMinLength()).isType(Integer.class);

		t.minLength("123");
		assertEquals(123, t.getMinLength().intValue());
		assertObject(t.getMinLength()).isType(Integer.class);

		t.minLength(new StringBuilder("123"));
		assertEquals(123, t.getMinLength().intValue());
		assertObject(t.getMinLength()).isType(Integer.class);

		t.minLength(null);
		assertNull(t.getMinLength());
	}

	/**
	 * Test method for {@link SchemaInfo#pattern(java.lang.Object)}.
	 */
	@Test
	public void testPattern() {
		SchemaInfo t = new SchemaInfo();

		t.pattern("foo");
		assertEquals("foo", t.getPattern());

		t.pattern(new StringBuilder("foo"));
		assertEquals("foo", t.getPattern());
		assertObject(t.getPattern()).isType(String.class);

		t.pattern(null);
		assertNull(t.getPattern());
	}

	/**
	 * Test method for {@link SchemaInfo#maxItems(java.lang.Object)}.
	 */
	@Test
	public void testMaxItems() {
		SchemaInfo t = new SchemaInfo();

		t.maxItems(123);
		assertEquals(123, t.getMaxItems().intValue());
		assertObject(t.getMaxItems()).isType(Integer.class);

		t.maxItems(123f);
		assertEquals(123, t.getMaxItems().intValue());
		assertObject(t.getMaxItems()).isType(Integer.class);

		t.maxItems("123");
		assertEquals(123, t.getMaxItems().intValue());
		assertObject(t.getMaxItems()).isType(Integer.class);

		t.maxItems(new StringBuilder("123"));
		assertEquals(123, t.getMaxItems().intValue());
		assertObject(t.getMaxItems()).isType(Integer.class);

		t.maxItems(null);
		assertNull(t.getMaxItems());
	}

	/**
	 * Test method for {@link SchemaInfo#minItems(java.lang.Object)}.
	 */
	@Test
	public void testMinItems() {
		SchemaInfo t = new SchemaInfo();

		t.minItems(123);
		assertEquals(123, t.getMinItems().intValue());
		assertObject(t.getMinItems()).isType(Integer.class);

		t.minItems(123f);
		assertEquals(123, t.getMinItems().intValue());
		assertObject(t.getMinItems()).isType(Integer.class);

		t.minItems("123");
		assertEquals(123, t.getMinItems().intValue());
		assertObject(t.getMinItems()).isType(Integer.class);

		t.minItems(new StringBuilder("123"));
		assertEquals(123, t.getMinItems().intValue());
		assertObject(t.getMinItems()).isType(Integer.class);

		t.minItems(null);
		assertNull(t.getMinItems());
	}

	/**
	 * Test method for {@link SchemaInfo#uniqueItems(java.lang.Object)}.
	 */
	@Test
	public void testUniqueItems() {
		SchemaInfo t = new SchemaInfo();

		t.uniqueItems(true);
		assertEquals(true, t.getUniqueItems());
		assertObject(t.getUniqueItems()).isType(Boolean.class);

		t.uniqueItems("true");
		assertEquals(true, t.getUniqueItems());
		assertObject(t.getUniqueItems()).isType(Boolean.class);

		t.uniqueItems(new StringBuilder("true"));
		assertEquals(true, t.getUniqueItems());
		assertObject(t.getUniqueItems()).isType(Boolean.class);

		t.uniqueItems(null);
		assertNull(t.getUniqueItems());
	}

	/**
	 * Test method for {@link SchemaInfo#maxProperties(java.lang.Object)}.
	 */
	@Test
	public void testMaxProperties() {
		SchemaInfo t = new SchemaInfo();

		t.maxProperties(123);
		assertEquals(123, t.getMaxProperties().intValue());
		assertObject(t.getMaxProperties()).isType(Integer.class);

		t.maxProperties(123f);
		assertEquals(123, t.getMaxProperties().intValue());
		assertObject(t.getMaxProperties()).isType(Integer.class);

		t.maxProperties("123");
		assertEquals(123, t.getMaxProperties().intValue());
		assertObject(t.getMaxProperties()).isType(Integer.class);

		t.maxProperties(new StringBuilder("123"));
		assertEquals(123, t.getMaxProperties().intValue());
		assertObject(t.getMaxProperties()).isType(Integer.class);

		t.maxProperties(null);
		assertNull(t.getMaxProperties());
	}

	/**
	 * Test method for {@link SchemaInfo#minProperties(java.lang.Object)}.
	 */
	@Test
	public void testMinProperties() {
		SchemaInfo t = new SchemaInfo();

		t.minProperties(123);
		assertEquals(123, t.getMinProperties().intValue());
		assertObject(t.getMinProperties()).isType(Integer.class);

		t.minProperties(123f);
		assertEquals(123, t.getMinProperties().intValue());
		assertObject(t.getMinProperties()).isType(Integer.class);

		t.minProperties("123");
		assertEquals(123, t.getMinProperties().intValue());
		assertObject(t.getMinProperties()).isType(Integer.class);

		t.minProperties(new StringBuilder("123"));
		assertEquals(123, t.getMinProperties().intValue());
		assertObject(t.getMinProperties()).isType(Integer.class);

		t.minProperties(null);
		assertNull(t.getMinProperties());
	}

	/**
	 * Test method for {@link SchemaInfo#required(java.lang.Object)}.
	 */
	@Test
	public void testRequired() {
		SchemaInfo t = new SchemaInfo();

		t.required("['x']");
		assertEquals("[x]", t.getRequired().toString());
		assertObject(t.getRequired()).isType(List.class);

		t.required("['x']");
		assertEquals("[x, x]", t.getRequired().toString());
		assertObject(t.getRequired()).isType(List.class);

		t.required(new StringBuilder("['x']"));
		assertEquals("[x, x, x]", t.getRequired().toString());
		assertObject(t.getRequired()).isType(List.class);

		t.setRequired(null);
		assertNull(t.getRequired());
	}

	/**
	 * Test method for {@link SchemaInfo#setEnum(java.util.Collection)}.
	 */
	@Test
	public void testSetEnum() {
		SchemaInfo t = new SchemaInfo();

		t.setEnum(ASet.of("foo","bar"));
		assertObject(t.getEnum()).json().is("['foo','bar']");
		assertObject(t.getEnum()).isType(List.class);

		t.setEnum(ASet.of());
		assertObject(t.getEnum()).json().is("[]");
		assertObject(t.getEnum()).isType(List.class);

		t.setEnum(null);
		assertNull(t.getEnum());
	}

	/**
	 * Test method for {@link SchemaInfo#addEnum(java.util.Collection)}.
	 */
	@Test
	public void testAddEnum() {
		SchemaInfo t = new SchemaInfo();

		t.addEnum(ASet.of("foo","bar"));
		assertObject(t.getEnum()).json().is("['foo','bar']");
		assertObject(t.getEnum()).isType(List.class);

		t.addEnum(ASet.of("baz"));
		assertObject(t.getEnum()).json().is("['foo','bar','baz']");
		assertObject(t.getEnum()).isType(List.class);

		t.addEnum(null);
		assertObject(t.getEnum()).json().is("['foo','bar','baz']");
		assertObject(t.getEnum()).isType(List.class);
	}

	/**
	 * Test method for {@link SchemaInfo#_enum(java.lang.Object[])}.
	 */
	@Test
	public void test_enum() {
		SchemaInfo t = new SchemaInfo();

		t._enum(ASet.of("foo","bar"));
		assertObject(t.getEnum()).json().is("['foo','bar']");
		assertObject(t.getEnum()).isType(List.class);

		t._enum(ASet.of("baz"));
		assertObject(t.getEnum()).json().is("['foo','bar','baz']");
		assertObject(t.getEnum()).isType(List.class);

		t._enum((Object[])null);
		assertObject(t.getEnum()).json().is("['foo','bar','baz']");
		assertObject(t.getEnum()).isType(List.class);

		t.setEnum(null);
		t._enum("foo")._enum(new StringBuilder("bar"))._enum("['baz','qux']")._enum((Object)new String[]{"quux"});
		assertObject(t.getEnum()).json().is("['foo','bar','baz','qux','quux']");
		assertObject(t.getEnum()).isType(List.class);
	}

	/**
	 * Test method for {@link SchemaInfo#type(java.lang.Object)}.
	 */
	@Test
	public void testType() {
		SchemaInfo t = new SchemaInfo();

		t.type("foo");
		assertEquals("foo", t.getType());

		t.type(new StringBuilder("foo"));
		assertEquals("foo", t.getType());
		assertObject(t.getType()).isType(String.class);

		t.type(null);
		assertNull(t.getType());
	}

	/**
	 * Test method for {@link SchemaInfo#items(java.lang.Object)}.
	 */
	@Test
	public void testItems() {
		SchemaInfo t = new SchemaInfo();

		t.items(items("foo"));
		assertObject(t.getItems()).json().is("{type:'foo'}");

		t.items("{type:'foo'}");
		assertObject(t.getItems()).json().is("{type:'foo'}");
		assertObject(t.getItems()).isType(Items.class);

		t.items(null);
		assertNull(t.getItems());
	}

	/**
	 * Test method for {@link SchemaInfo#setAllOf(java.util.Collection)}.
	 */
	@Test
	public void testSetAllOf() {
		SchemaInfo t = new SchemaInfo();

		t.setAllOf(ASet.of("foo","bar"));
		assertObject(t.getAllOf()).json().is("['foo','bar']");
		assertObject(t.getAllOf()).isType(List.class);

		t.setAllOf(ASet.of());
		assertObject(t.getAllOf()).json().is("[]");
		assertObject(t.getAllOf()).isType(List.class);

		t.setAllOf(null);
		assertNull(t.getAllOf());
	}

	/**
	 * Test method for {@link SchemaInfo#addAllOf(java.util.Collection)}.
	 */
	@Test
	public void testAddAllOf() {
		SchemaInfo t = new SchemaInfo();

		t.addAllOf(ASet.of("foo","bar"));
		assertObject(t.getAllOf()).json().is("['foo','bar']");
		assertObject(t.getAllOf()).isType(List.class);

		t.addAllOf(ASet.of());
		assertObject(t.getAllOf()).json().is("['foo','bar']");
		assertObject(t.getAllOf()).isType(List.class);

		t.addAllOf(null);
		assertObject(t.getAllOf()).json().is("['foo','bar']");
		assertObject(t.getAllOf()).isType(List.class);
	}

	/**
	 * Test method for {@link SchemaInfo#allOf(java.lang.Object[])}.
	 */
	@Test
	public void testAllOf() {
		SchemaInfo t = new SchemaInfo();

		t.allOf(ASet.of("a"));
		t.allOf(ASet.of(new StringBuilder("b")));
		t.allOf((Object)new String[] {"c"});
		t.allOf((Object)new Object[] {new StringBuilder("d")});
		t.allOf("e");
		t.allOf("['f']");
		t.allOf("[]");
		t.allOf((Object)null);
		assertObject(t.getAllOf()).json().is("['a','b','c','d','e','f']");
	}

	/**
	 * Test method for {@link SchemaInfo#setProperties(java.util.Map)}.
	 */
	@Test
	public void testSetProperties() {
		SchemaInfo t = new SchemaInfo();

		t.setProperties(AMap.of("foo",new SchemaInfo().type("foo")));
		assertObject(t.getProperties()).json().is("{foo:{type:'foo'}}");
		assertObject(t.getProperties()).isType(Map.class);

		t.setProperties(AMap.of());
		assertObject(t.getProperties()).json().is("{}");
		assertObject(t.getProperties()).isType(Map.class);

		t.setProperties(null);
		assertNull(t.getProperties());
	}

	/**
	 * Test method for {@link SchemaInfo#addProperties(java.util.Map)}.
	 */
	@Test
	public void testAddProperties() {
		SchemaInfo t = new SchemaInfo();

		t.addProperties(AMap.of("foo", new SchemaInfo().type("foo")));
		assertObject(t.getProperties()).json().is("{foo:{type:'foo'}}");
		assertObject(t.getProperties()).isType(Map.class);

		t.addProperties(AMap.of());
		assertObject(t.getProperties()).json().is("{foo:{type:'foo'}}");
		assertObject(t.getProperties()).isType(Map.class);

		t.addProperties(null);
		assertObject(t.getProperties()).json().is("{foo:{type:'foo'}}");
		assertObject(t.getProperties()).isType(Map.class);
	}

	/**
	 * Test method for {@link SchemaInfo#properties(java.lang.Object[])}.
	 */
	@Test
	public void testProperties() {
		SchemaInfo t = new SchemaInfo();

		t.properties(AMap.of("a",AMap.of("type","foo")));
		t.properties(AMap.of("b","{type:'bar'}"));
		t.properties("{c:{type:'baz'}}");
		t.properties("{}");
		t.properties((Object[])null);

		assertObject(t.getProperties()).json().is("{a:{type:'foo'},b:{type:'bar'},c:{type:'baz'}}");
		assertObject(t.getProperties().get("a")).isType(SchemaInfo.class);
		assertObject(t.getProperties().get("b")).isType(SchemaInfo.class);
		assertObject(t.getProperties().get("c")).isType(SchemaInfo.class);
	}

	/**
	 * Test method for {@link SchemaInfo#setAdditionalProperties(java.util.Map)}.
	 */
	@Test
	public void testSetAdditionalProperties() {
		SchemaInfo t = new SchemaInfo();

		t.setAdditionalProperties(new SchemaInfo().type("foo"));
		assertObject(t.getAdditionalProperties()).json().is("{type:'foo'}");
		assertObject(t.getAdditionalProperties()).isType(SchemaInfo.class);

		t.setAdditionalProperties(new SchemaInfo());
		assertObject(t.getAdditionalProperties()).json().is("{}");
		assertObject(t.getAdditionalProperties()).isType(SchemaInfo.class);

		t.setAdditionalProperties((SchemaInfo)null);
		assertNull(t.getAdditionalProperties());
	}

	/**
	 * Test method for {@link SchemaInfo#additionalProperties(java.lang.Object[])}.
	 */
	@Test
	public void testAdditionalProperties() {
		SchemaInfo t = new SchemaInfo();

		t.additionalProperties(AMap.of("type","foo"));

		assertObject(t.getAdditionalProperties()).json().is("{type:'foo'}");
		assertObject(t.getAdditionalProperties()).isType(SchemaInfo.class);
	}

	/**
	 * Test method for {@link SchemaInfo#discriminator(java.lang.Object)}.
	 */
	@Test
	public void testDiscriminator() {
		SchemaInfo t = new SchemaInfo();

		t.discriminator("foo");
		assertEquals("foo", t.getDiscriminator());

		t.discriminator(new StringBuilder("foo"));
		assertEquals("foo", t.getDiscriminator());
		assertObject(t.getDiscriminator()).isType(String.class);

		t.discriminator(null);
		assertNull(t.getDiscriminator());
	}

	/**
	 * Test method for {@link SchemaInfo#readOnly(java.lang.Object)}.
	 */
	@Test
	public void testReadOnly() {
		SchemaInfo t = new SchemaInfo();

		t.readOnly(true);
		assertEquals(true, t.getReadOnly());
		assertObject(t.getReadOnly()).isType(Boolean.class);

		t.readOnly("true");
		assertEquals(true, t.getReadOnly());
		assertObject(t.getReadOnly()).isType(Boolean.class);

		t.readOnly(new StringBuilder("true"));
		assertEquals(true, t.getReadOnly());
		assertObject(t.getReadOnly()).isType(Boolean.class);

		t.readOnly(null);
		assertNull(t.getReadOnly());
	}

	/**
	 * Test method for {@link SchemaInfo#xml(java.lang.Object)}.
	 */
	@Test
	public void testXml() {
		SchemaInfo t = new SchemaInfo();

		t.xml(xml().name("foo"));
		assertObject(t.getXml()).json().is("{name:'foo'}");

		t.xml("{name:'foo'}");
		assertObject(t.getXml()).json().is("{name:'foo'}");
		assertObject(t.getXml()).isType(Xml.class);

		t.xml(null);
		assertNull(t.getXml());
	}

	/**
	 * Test method for {@link SchemaInfo#externalDocs(java.lang.Object)}.
	 */
	@Test
	public void testExternalDocs() {
		SchemaInfo t = new SchemaInfo();

		t.externalDocs(externalDocumentation("foo"));
		assertObject(t.getExternalDocs()).json().is("{url:'foo'}");

		t.externalDocs("{url:'foo'}");
		assertObject(t.getExternalDocs()).json().is("{url:'foo'}");
		assertObject(t.getExternalDocs()).isType(ExternalDocumentation.class);

		t.externalDocs(null);
		assertNull(t.getExternalDocs());
	}

	/**
	 * Test method for {@link SchemaInfo#example(java.lang.Object)}.
	 */
	@Test
	public void testExample() {
		SchemaInfo t = new SchemaInfo();

		t.example("foo");
		assertEquals("foo", t.getExample());

		t.example(123);
		assertEquals(123, t.getExample());

		t.example(null);
		assertNull(t.getExample());
	}

	/**
	 * Test method for {@link SchemaInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
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
			.set("required", ASet.of("x"))
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", true)
			.set("xml", xml().name("o"))
			.set("$ref", "ref");

		assertObject(t).json().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,required:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");

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
			.set("required", "['x']")
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", "true")
			.set("xml", "{name:'o'}")
			.set("$ref", "ref");

		assertObject(t).json().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,required:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");

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
			.set("required", new StringBuilder("['x']"))
			.set("title", new StringBuilder("m"))
			.set("type", new StringBuilder("n"))
			.set("uniqueItems", new StringBuilder("true"))
			.set("xml", new StringBuilder("{name:'o'}"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).json().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,required:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");

		assertEquals("a", t.get("default", String.class));
		assertEquals("['b']", t.get("enum", String.class));
		assertEquals("{c:['c1']}", t.get("additionalProperties", String.class));
		assertEquals("['d']", t.get("allOf", String.class));
		assertEquals("e", t.get("description", String.class));
		assertEquals("f", t.get("discriminator", String.class));
		assertEquals("g", t.get("example", String.class));
		assertEquals("true", t.get("exclusiveMaximum", String.class));
		assertEquals("true", t.get("exclusiveMinimum", String.class));
		assertEquals("{url:'h'}", t.get("externalDocs", String.class));
		assertEquals("i", t.get("format", String.class));
		assertEquals("{type:'j'}", t.get("items", String.class));
		assertEquals("123.0", t.get("maximum", String.class));
		assertEquals("123", t.get("maxItems", String.class));
		assertEquals("123", t.get("maxLength", String.class));
		assertEquals("123", t.get("maxProperties", String.class));
		assertEquals("123.0", t.get("minimum", String.class));
		assertEquals("123", t.get("minItems", String.class));
		assertEquals("123", t.get("minLength", String.class));
		assertEquals("123", t.get("minProperties", String.class));
		assertEquals("123.0", t.get("multipleOf", String.class));
		assertEquals("k", t.get("pattern", String.class));
		assertEquals("{l:{l1:1}}", t.get("properties", String.class));
		assertEquals("true", t.get("readOnly", String.class));
		assertEquals("['x']", t.get("required", String.class));
		assertEquals("m", t.get("title", String.class));
		assertEquals("n", t.get("type", String.class));
		assertEquals("true", t.get("uniqueItems", String.class));
		assertEquals("{name:'o'}", t.get("xml", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertObject(t.get("default", Object.class)).isType(StringBuilder.class);
		assertObject(t.get("enum", Object.class)).isType(List.class);
		assertObject(t.get("additionalProperties", Object.class)).isType(SchemaInfo.class);
		assertObject(t.get("allOf", Object.class)).isType(List.class);
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
		assertObject(t.get("required", Object.class)).isType(List.class);
		assertObject(t.get("title", Object.class)).isType(String.class);
		assertObject(t.get("type", Object.class)).isType(String.class);
		assertObject(t.get("uniqueItems", Object.class)).isType(Boolean.class);
		assertObject(t.get("xml", Object.class)).isType(Xml.class);
		assertObject(t.get("$ref", Object.class)).isType(String.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,required:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, SchemaInfo.class)).json().is(s);
	}
}
