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

import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.apache.juneau.testutils.TestUtils.*;
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
		assertInstanceOf(String.class, t.getFormat());

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
		assertInstanceOf(String.class, t.getTitle());

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
		assertInstanceOf(String.class, t.getDescription());

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
		assertInstanceOf(StringBuilder.class, t.getDefault());

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
		assertInstanceOf(Integer.class, t.getMultipleOf());

		t.multipleOf(123f);
		assertEquals(123f, t.getMultipleOf());
		assertInstanceOf(Float.class, t.getMultipleOf());

		t.multipleOf("123");
		assertEquals(123, t.getMultipleOf());
		assertInstanceOf(Integer.class, t.getMultipleOf());

		t.multipleOf(new StringBuilder("123"));
		assertEquals(123, t.getMultipleOf());
		assertInstanceOf(Integer.class, t.getMultipleOf());

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
		assertInstanceOf(Integer.class, t.getMaximum());

		t.maximum(123f);
		assertEquals(123f, t.getMaximum());
		assertInstanceOf(Float.class, t.getMaximum());

		t.maximum("123");
		assertEquals(123, t.getMaximum());
		assertInstanceOf(Integer.class, t.getMaximum());

		t.maximum(new StringBuilder("123"));
		assertEquals(123, t.getMaximum());
		assertInstanceOf(Integer.class, t.getMaximum());

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
		assertInstanceOf(Boolean.class, t.getExclusiveMaximum());

		t.exclusiveMaximum("true");
		assertEquals(true, t.getExclusiveMaximum());
		assertInstanceOf(Boolean.class, t.getExclusiveMaximum());

		t.exclusiveMaximum(new StringBuilder("true"));
		assertEquals(true, t.getExclusiveMaximum());
		assertInstanceOf(Boolean.class, t.getExclusiveMaximum());

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
		assertInstanceOf(Integer.class, t.getMinimum());

		t.minimum(123f);
		assertEquals(123f, t.getMinimum());
		assertInstanceOf(Float.class, t.getMinimum());

		t.minimum("123");
		assertEquals(123, t.getMinimum());
		assertInstanceOf(Integer.class, t.getMinimum());

		t.minimum(new StringBuilder("123"));
		assertEquals(123, t.getMinimum());
		assertInstanceOf(Integer.class, t.getMinimum());

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
		assertInstanceOf(Boolean.class, t.getExclusiveMinimum());

		t.exclusiveMinimum("true");
		assertEquals(true, t.getExclusiveMinimum());
		assertInstanceOf(Boolean.class, t.getExclusiveMinimum());

		t.exclusiveMinimum(new StringBuilder("true"));
		assertEquals(true, t.getExclusiveMinimum());
		assertInstanceOf(Boolean.class, t.getExclusiveMinimum());

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
		assertInstanceOf(Integer.class, t.getMaxLength());

		t.maxLength(123f);
		assertEquals(123, t.getMaxLength().intValue());
		assertInstanceOf(Integer.class, t.getMaxLength());

		t.maxLength("123");
		assertEquals(123, t.getMaxLength().intValue());
		assertInstanceOf(Integer.class, t.getMaxLength());

		t.maxLength(new StringBuilder("123"));
		assertEquals(123, t.getMaxLength().intValue());
		assertInstanceOf(Integer.class, t.getMaxLength());

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
		assertInstanceOf(Integer.class, t.getMinLength());

		t.minLength(123f);
		assertEquals(123, t.getMinLength().intValue());
		assertInstanceOf(Integer.class, t.getMinLength());

		t.minLength("123");
		assertEquals(123, t.getMinLength().intValue());
		assertInstanceOf(Integer.class, t.getMinLength());

		t.minLength(new StringBuilder("123"));
		assertEquals(123, t.getMinLength().intValue());
		assertInstanceOf(Integer.class, t.getMinLength());

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
		assertInstanceOf(String.class, t.getPattern());

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
		assertInstanceOf(Integer.class, t.getMaxItems());

		t.maxItems(123f);
		assertEquals(123, t.getMaxItems().intValue());
		assertInstanceOf(Integer.class, t.getMaxItems());

		t.maxItems("123");
		assertEquals(123, t.getMaxItems().intValue());
		assertInstanceOf(Integer.class, t.getMaxItems());

		t.maxItems(new StringBuilder("123"));
		assertEquals(123, t.getMaxItems().intValue());
		assertInstanceOf(Integer.class, t.getMaxItems());

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
		assertInstanceOf(Integer.class, t.getMinItems());

		t.minItems(123f);
		assertEquals(123, t.getMinItems().intValue());
		assertInstanceOf(Integer.class, t.getMinItems());

		t.minItems("123");
		assertEquals(123, t.getMinItems().intValue());
		assertInstanceOf(Integer.class, t.getMinItems());

		t.minItems(new StringBuilder("123"));
		assertEquals(123, t.getMinItems().intValue());
		assertInstanceOf(Integer.class, t.getMinItems());

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
		assertInstanceOf(Boolean.class, t.getUniqueItems());

		t.uniqueItems("true");
		assertEquals(true, t.getUniqueItems());
		assertInstanceOf(Boolean.class, t.getUniqueItems());

		t.uniqueItems(new StringBuilder("true"));
		assertEquals(true, t.getUniqueItems());
		assertInstanceOf(Boolean.class, t.getUniqueItems());

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
		assertInstanceOf(Integer.class, t.getMaxProperties());

		t.maxProperties(123f);
		assertEquals(123, t.getMaxProperties().intValue());
		assertInstanceOf(Integer.class, t.getMaxProperties());

		t.maxProperties("123");
		assertEquals(123, t.getMaxProperties().intValue());
		assertInstanceOf(Integer.class, t.getMaxProperties());

		t.maxProperties(new StringBuilder("123"));
		assertEquals(123, t.getMaxProperties().intValue());
		assertInstanceOf(Integer.class, t.getMaxProperties());

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
		assertInstanceOf(Integer.class, t.getMinProperties());

		t.minProperties(123f);
		assertEquals(123, t.getMinProperties().intValue());
		assertInstanceOf(Integer.class, t.getMinProperties());

		t.minProperties("123");
		assertEquals(123, t.getMinProperties().intValue());
		assertInstanceOf(Integer.class, t.getMinProperties());

		t.minProperties(new StringBuilder("123"));
		assertEquals(123, t.getMinProperties().intValue());
		assertInstanceOf(Integer.class, t.getMinProperties());

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
		assertInstanceOf(List.class, t.getRequired());

		t.required("['x']");
		assertEquals("[x, x]", t.getRequired().toString());
		assertInstanceOf(List.class, t.getRequired());

		t.required(new StringBuilder("['x']"));
		assertEquals("[x, x, x]", t.getRequired().toString());
		assertInstanceOf(List.class, t.getRequired());

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
		assertInstanceOf(List.class, t.getEnum());

		t.setEnum(ASet.of());
		assertObject(t.getEnum()).json().is("[]");
		assertInstanceOf(List.class, t.getEnum());

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
		assertInstanceOf(List.class, t.getEnum());

		t.addEnum(ASet.of("baz"));
		assertObject(t.getEnum()).json().is("['foo','bar','baz']");
		assertInstanceOf(List.class, t.getEnum());

		t.addEnum(null);
		assertObject(t.getEnum()).json().is("['foo','bar','baz']");
		assertInstanceOf(List.class, t.getEnum());
	}

	/**
	 * Test method for {@link SchemaInfo#_enum(java.lang.Object[])}.
	 */
	@Test
	public void test_enum() {
		SchemaInfo t = new SchemaInfo();

		t._enum(ASet.of("foo","bar"));
		assertObject(t.getEnum()).json().is("['foo','bar']");
		assertInstanceOf(List.class, t.getEnum());

		t._enum(ASet.of("baz"));
		assertObject(t.getEnum()).json().is("['foo','bar','baz']");
		assertInstanceOf(List.class, t.getEnum());

		t._enum((Object[])null);
		assertObject(t.getEnum()).json().is("['foo','bar','baz']");
		assertInstanceOf(List.class, t.getEnum());

		t.setEnum(null);
		t._enum("foo")._enum(new StringBuilder("bar"))._enum("['baz','qux']")._enum((Object)new String[]{"quux"});
		assertObject(t.getEnum()).json().is("['foo','bar','baz','qux','quux']");
		assertInstanceOf(List.class, t.getEnum());
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
		assertInstanceOf(String.class, t.getType());

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
		assertInstanceOf(Items.class, t.getItems());

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
		assertInstanceOf(List.class, t.getAllOf());

		t.setAllOf(ASet.of());
		assertObject(t.getAllOf()).json().is("[]");
		assertInstanceOf(List.class, t.getAllOf());

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
		assertInstanceOf(List.class, t.getAllOf());

		t.addAllOf(ASet.of());
		assertObject(t.getAllOf()).json().is("['foo','bar']");
		assertInstanceOf(List.class, t.getAllOf());

		t.addAllOf(null);
		assertObject(t.getAllOf()).json().is("['foo','bar']");
		assertInstanceOf(List.class, t.getAllOf());
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
		assertInstanceOf(Map.class, t.getProperties());

		t.setProperties(AMap.of());
		assertObject(t.getProperties()).json().is("{}");
		assertInstanceOf(Map.class, t.getProperties());

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
		assertInstanceOf(Map.class, t.getProperties());

		t.addProperties(AMap.of());
		assertObject(t.getProperties()).json().is("{foo:{type:'foo'}}");
		assertInstanceOf(Map.class, t.getProperties());

		t.addProperties(null);
		assertObject(t.getProperties()).json().is("{foo:{type:'foo'}}");
		assertInstanceOf(Map.class, t.getProperties());
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
		assertInstanceOf(SchemaInfo.class, t.getProperties().get("a"));
		assertInstanceOf(SchemaInfo.class, t.getProperties().get("b"));
		assertInstanceOf(SchemaInfo.class, t.getProperties().get("c"));
	}

	/**
	 * Test method for {@link SchemaInfo#setAdditionalProperties(java.util.Map)}.
	 */
	@Test
	public void testSetAdditionalProperties() {
		SchemaInfo t = new SchemaInfo();

		t.setAdditionalProperties(new SchemaInfo().type("foo"));
		assertObject(t.getAdditionalProperties()).json().is("{type:'foo'}");
		assertInstanceOf(SchemaInfo.class, t.getAdditionalProperties());

		t.setAdditionalProperties(new SchemaInfo());
		assertObject(t.getAdditionalProperties()).json().is("{}");
		assertInstanceOf(SchemaInfo.class, t.getAdditionalProperties());

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
		assertInstanceOf(SchemaInfo.class, t.getAdditionalProperties());
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
		assertInstanceOf(String.class, t.getDiscriminator());

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
		assertInstanceOf(Boolean.class, t.getReadOnly());

		t.readOnly("true");
		assertEquals(true, t.getReadOnly());
		assertInstanceOf(Boolean.class, t.getReadOnly());

		t.readOnly(new StringBuilder("true"));
		assertEquals(true, t.getReadOnly());
		assertInstanceOf(Boolean.class, t.getReadOnly());

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
		assertInstanceOf(Xml.class, t.getXml());

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
		assertInstanceOf(ExternalDocumentation.class, t.getExternalDocs());

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

		assertInstanceOf(StringBuilder.class, t.get("default", Object.class));
		assertInstanceOf(List.class, t.get("enum", Object.class));
		assertInstanceOf(SchemaInfo.class, t.get("additionalProperties", Object.class));
		assertInstanceOf(List.class, t.get("allOf", Object.class));
		assertInstanceOf(String.class, t.get("description", Object.class));
		assertInstanceOf(String.class, t.get("discriminator", Object.class));
		assertInstanceOf(StringBuilder.class, t.get("example", Object.class));
		assertInstanceOf(Boolean.class, t.get("exclusiveMaximum", Object.class));
		assertInstanceOf(Boolean.class, t.get("exclusiveMinimum", Object.class));
		assertInstanceOf(ExternalDocumentation.class, t.get("externalDocs", Object.class));
		assertInstanceOf(String.class, t.get("format", Object.class));
		assertInstanceOf(Items.class, t.get("items", Object.class));
		assertInstanceOf(Float.class, t.get("maximum", Object.class));
		assertInstanceOf(Integer.class, t.get("maxItems", Object.class));
		assertInstanceOf(Integer.class, t.get("maxLength", Object.class));
		assertInstanceOf(Integer.class, t.get("maxProperties", Object.class));
		assertInstanceOf(Float.class, t.get("minimum", Object.class));
		assertInstanceOf(Integer.class, t.get("minItems", Object.class));
		assertInstanceOf(Integer.class, t.get("minLength", Object.class));
		assertInstanceOf(Integer.class, t.get("minProperties", Object.class));
		assertInstanceOf(Float.class, t.get("multipleOf", Object.class));
		assertInstanceOf(String.class, t.get("pattern", Object.class));
		assertInstanceOf(Map.class, t.get("properties", Object.class));
		assertInstanceOf(Boolean.class, t.get("readOnly", Object.class));
		assertInstanceOf(List.class, t.get("required", Object.class));
		assertInstanceOf(String.class, t.get("title", Object.class));
		assertInstanceOf(String.class, t.get("type", Object.class));
		assertInstanceOf(Boolean.class, t.get("uniqueItems", Object.class));
		assertInstanceOf(Xml.class, t.get("xml", Object.class));
		assertInstanceOf(String.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,required:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, SchemaInfo.class)).json().is(s);
	}
}
