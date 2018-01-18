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
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.utils.*;

import org.junit.*;

/**
 * Testcase for {@link ParameterInfo}.
 */
public class ParameterInfoTest {

	/**
	 * Test method for {@link ParameterInfo#name(java.lang.Object)}.
	 */
	@Test
	public void testName() {
		ParameterInfo t = new ParameterInfo();
		
		t.name("foo");
		assertEquals("foo", t.getName());
		
		t.name(new StringBuilder("foo"));
		assertEquals("foo", t.getName());
		assertType(String.class, t.getName());
		
		t.name(null);
		assertNull(t.getName());
	}

	/**
	 * Test method for {@link ParameterInfo#in(java.lang.Object)}.
	 */
	@Test
	public void testIn() {
		ParameterInfo t = new ParameterInfo();
		
		t.in("foo");
		assertEquals("foo", t.getIn());
		
		t.in(new StringBuilder("foo"));
		assertEquals("foo", t.getIn());
		assertType(String.class, t.getIn());
		
		t.in(null);
		assertNull(t.getIn());
	}

	/**
	 * Test method for {@link ParameterInfo#description(java.lang.Object)}.
	 */
	@Test
	public void testDescription() {
		ParameterInfo t = new ParameterInfo();
		
		t.description("foo");
		assertEquals("foo", t.getDescription());
		
		t.description(new StringBuilder("foo"));
		assertEquals("foo", t.getDescription());
		assertType(String.class, t.getDescription());
		
		t.description(null);
		assertNull(t.getDescription());
	}

	/**
	 * Test method for {@link ParameterInfo#required(java.lang.Object)}.
	 */
	@Test
	public void testRequired() {
		ParameterInfo t = new ParameterInfo();
		
		t.required(true);
		assertEquals(true, t.getRequired());
		assertType(Boolean.class, t.getRequired());
		
		t.required("true");
		assertEquals(true, t.getRequired());
		assertType(Boolean.class, t.getRequired());

		t.required(new StringBuilder("true"));
		assertEquals(true, t.getRequired());
		assertType(Boolean.class, t.getRequired());
		
		t.required(null);
		assertNull(t.getRequired());
	}

	/**
	 * Test method for {@link ParameterInfo#schema(java.lang.Object)}.
	 */
	@Test
	public void testSchema() {
		ParameterInfo t = new ParameterInfo();
		
		t.schema(schemaInfo().title("foo"));
		assertObjectEquals("{title:'foo'}", t.getSchema());
		
		t.schema("{title:'foo'}");
		assertObjectEquals("{title:'foo'}", t.getSchema());
		assertType(SchemaInfo.class, t.getSchema());

		t.schema(null);
		assertNull(t.getSchema());
	}

	/**
	 * Test method for {@link ParameterInfo#type(java.lang.Object)}.
	 */
	@Test
	public void testType() {
		ParameterInfo t = new ParameterInfo();
		
		t.type("foo");
		assertEquals("foo", t.getType());
		
		t.type(new StringBuilder("foo"));
		assertEquals("foo", t.getType());
		assertType(String.class, t.getType());
		
		t.type(null);
		assertNull(t.getType());
	}

	/**
	 * Test method for {@link ParameterInfo#format(java.lang.Object)}.
	 */
	@Test
	public void testFormat() {
		ParameterInfo t = new ParameterInfo();
		
		t.format("foo");
		assertEquals("foo", t.getFormat());
		
		t.format(new StringBuilder("foo"));
		assertEquals("foo", t.getFormat());
		assertType(String.class, t.getFormat());
		
		t.format(null);
		assertNull(t.getFormat());
	}

	/**
	 * Test method for {@link ParameterInfo#allowEmptyValue(java.lang.Object)}.
	 */
	@Test
	public void testAllowEmptyValue() {
		ParameterInfo t = new ParameterInfo();
		
		t.allowEmptyValue(true);
		assertEquals(true, t.getAllowEmptyValue());
		assertType(Boolean.class, t.getAllowEmptyValue());
		
		t.allowEmptyValue("true");
		assertEquals(true, t.getAllowEmptyValue());
		assertType(Boolean.class, t.getAllowEmptyValue());

		t.allowEmptyValue(new StringBuilder("true"));
		assertEquals(true, t.getAllowEmptyValue());
		assertType(Boolean.class, t.getAllowEmptyValue());
		
		t.allowEmptyValue(null);
		assertNull(t.getAllowEmptyValue());
	}

	/**
	 * Test method for {@link ParameterInfo#items(java.lang.Object)}.
	 */
	@Test
	public void testItems() {
		ParameterInfo t = new ParameterInfo();
		
		t.items(items("foo"));
		assertObjectEquals("{type:'foo'}", t.getItems());
		
		t.items("{type:'foo'}");
		assertObjectEquals("{type:'foo'}", t.getItems());
		assertType(Items.class, t.getItems());

		t.items(null);
		assertNull(t.getItems());
	}

	/**
	 * Test method for {@link ParameterInfo#collectionFormat(java.lang.Object)}.
	 */
	@Test
	public void testCollectionFormat() {
		ParameterInfo t = new ParameterInfo();
		
		t.collectionFormat("foo");
		assertEquals("foo", t.getCollectionFormat());
		
		t.collectionFormat(new StringBuilder("foo"));
		assertEquals("foo", t.getCollectionFormat());
		assertType(String.class, t.getCollectionFormat());
		
		t.collectionFormat(null);
		assertNull(t.getCollectionFormat());
	}

	/**
	 * Test method for {@link ParameterInfo#_default(java.lang.Object)}.
	 */
	@Test
	public void test_default() {
		ParameterInfo t = new ParameterInfo();
		
		t._default("foo");
		assertEquals("foo", t.getDefault());
		
		t._default(new StringBuilder("foo"));
		assertEquals("foo", t.getDefault().toString());
		assertType(StringBuilder.class, t.getDefault());
		
		t._default(null);
		assertNull(t.getDefault());
	}

	/**
	 * Test method for {@link ParameterInfo#maximum(java.lang.Object)}.
	 */
	@Test
	public void testMaximum() {
		ParameterInfo t = new ParameterInfo();
		
		t.maximum(123);
		assertEquals(123, t.getMaximum());
		assertType(Integer.class, t.getMaximum());
		
		t.maximum(123f);
		assertEquals(123f, t.getMaximum());
		assertType(Float.class, t.getMaximum());

		t.maximum("123");
		assertEquals(123, t.getMaximum());
		assertType(Integer.class, t.getMaximum());

		t.maximum(new StringBuilder("123"));
		assertEquals(123, t.getMaximum());
		assertType(Integer.class, t.getMaximum());
		
		t.maximum(null);
		assertNull(t.getMaximum());
	}

	/**
	 * Test method for {@link ParameterInfo#exclusiveMaximum(java.lang.Object)}.
	 */
	@Test
	public void testExclusiveMaximum() {
		ParameterInfo t = new ParameterInfo();
		
		t.exclusiveMaximum(true);
		assertEquals(true, t.getExclusiveMaximum());
		assertType(Boolean.class, t.getExclusiveMaximum());
		
		t.exclusiveMaximum("true");
		assertEquals(true, t.getExclusiveMaximum());
		assertType(Boolean.class, t.getExclusiveMaximum());

		t.exclusiveMaximum(new StringBuilder("true"));
		assertEquals(true, t.getExclusiveMaximum());
		assertType(Boolean.class, t.getExclusiveMaximum());
		
		t.exclusiveMaximum(null);
		assertNull(t.getExclusiveMaximum());
	}

	/**
	 * Test method for {@link ParameterInfo#minimum(java.lang.Object)}.
	 */
	@Test
	public void testMinimum() {
		ParameterInfo t = new ParameterInfo();
		
		t.minimum(123);
		assertEquals(123, t.getMinimum());
		assertType(Integer.class, t.getMinimum());
		
		t.minimum(123f);
		assertEquals(123f, t.getMinimum());
		assertType(Float.class, t.getMinimum());

		t.minimum("123");
		assertEquals(123, t.getMinimum());
		assertType(Integer.class, t.getMinimum());

		t.minimum(new StringBuilder("123"));
		assertEquals(123, t.getMinimum());
		assertType(Integer.class, t.getMinimum());
		
		t.minimum(null);
		assertNull(t.getMinimum());
	}

	/**
	 * Test method for {@link ParameterInfo#exclusiveMinimum(java.lang.Object)}.
	 */
	@Test
	public void testExclusiveMinimum() {
		ParameterInfo t = new ParameterInfo();
		
		t.exclusiveMinimum(true);
		assertEquals(true, t.getExclusiveMinimum());
		assertType(Boolean.class, t.getExclusiveMinimum());
		
		t.exclusiveMinimum("true");
		assertEquals(true, t.getExclusiveMinimum());
		assertType(Boolean.class, t.getExclusiveMinimum());

		t.exclusiveMinimum(new StringBuilder("true"));
		assertEquals(true, t.getExclusiveMinimum());
		assertType(Boolean.class, t.getExclusiveMinimum());
		
		t.exclusiveMinimum(null);
		assertNull(t.getExclusiveMinimum());
	}

	/**
	 * Test method for {@link ParameterInfo#maxLength(java.lang.Object)}.
	 */
	@Test
	public void testMaxLength() {
		ParameterInfo t = new ParameterInfo();
		
		t.maxLength(123);
		assertEquals(123, t.getMaxLength().intValue());
		assertType(Integer.class, t.getMaxLength());
		
		t.maxLength(123f);
		assertEquals(123, t.getMaxLength().intValue());
		assertType(Integer.class, t.getMaxLength());

		t.maxLength("123");
		assertEquals(123, t.getMaxLength().intValue());
		assertType(Integer.class, t.getMaxLength());

		t.maxLength(new StringBuilder("123"));
		assertEquals(123, t.getMaxLength().intValue());
		assertType(Integer.class, t.getMaxLength());
		
		t.maxLength(null);
		assertNull(t.getMaxLength());
	}

	/**
	 * Test method for {@link ParameterInfo#minLength(java.lang.Object)}.
	 */
	@Test
	public void testMinLength() {
		ParameterInfo t = new ParameterInfo();
		
		t.minLength(123);
		assertEquals(123, t.getMinLength().intValue());
		assertType(Integer.class, t.getMinLength());
		
		t.minLength(123f);
		assertEquals(123, t.getMinLength().intValue());
		assertType(Integer.class, t.getMinLength());

		t.minLength("123");
		assertEquals(123, t.getMinLength().intValue());
		assertType(Integer.class, t.getMinLength());

		t.minLength(new StringBuilder("123"));
		assertEquals(123, t.getMinLength().intValue());
		assertType(Integer.class, t.getMinLength());
		
		t.minLength(null);
		assertNull(t.getMinLength());
	}

	/**
	 * Test method for {@link ParameterInfo#pattern(java.lang.Object)}.
	 */
	@Test
	public void testPattern() {
		ParameterInfo t = new ParameterInfo();
		
		t.pattern("foo");
		assertEquals("foo", t.getPattern());
		
		t.pattern(new StringBuilder("foo"));
		assertEquals("foo", t.getPattern());
		assertType(String.class, t.getPattern());
		
		t.pattern(null);
		assertNull(t.getPattern());
	}

	/**
	 * Test method for {@link ParameterInfo#maxItems(java.lang.Object)}.
	 */
	@Test
	public void testMaxItems() {
		ParameterInfo t = new ParameterInfo();
		
		t.maxItems(123);
		assertEquals(123, t.getMaxItems().intValue());
		assertType(Integer.class, t.getMaxItems());
		
		t.maxItems(123f);
		assertEquals(123, t.getMaxItems().intValue());
		assertType(Integer.class, t.getMaxItems());

		t.maxItems("123");
		assertEquals(123, t.getMaxItems().intValue());
		assertType(Integer.class, t.getMaxItems());

		t.maxItems(new StringBuilder("123"));
		assertEquals(123, t.getMaxItems().intValue());
		assertType(Integer.class, t.getMaxItems());
		
		t.maxItems(null);
		assertNull(t.getMaxItems());
	}

	/**
	 * Test method for {@link ParameterInfo#minItems(java.lang.Object)}.
	 */
	@Test
	public void testMinItems() {
		ParameterInfo t = new ParameterInfo();
		
		t.minItems(123);
		assertEquals(123, t.getMinItems().intValue());
		assertType(Integer.class, t.getMinItems());
		
		t.minItems(123f);
		assertEquals(123, t.getMinItems().intValue());
		assertType(Integer.class, t.getMinItems());

		t.minItems("123");
		assertEquals(123, t.getMinItems().intValue());
		assertType(Integer.class, t.getMinItems());

		t.minItems(new StringBuilder("123"));
		assertEquals(123, t.getMinItems().intValue());
		assertType(Integer.class, t.getMinItems());
		
		t.minItems(null);
		assertNull(t.getMinItems());
	}

	/**
	 * Test method for {@link ParameterInfo#uniqueItems(java.lang.Object)}.
	 */
	@Test
	public void testUniqueItems() {
		ParameterInfo t = new ParameterInfo();
		
		t.uniqueItems(true);
		assertEquals(true, t.getUniqueItems());
		assertType(Boolean.class, t.getUniqueItems());
		
		t.uniqueItems("true");
		assertEquals(true, t.getUniqueItems());
		assertType(Boolean.class, t.getUniqueItems());

		t.uniqueItems(new StringBuilder("true"));
		assertEquals(true, t.getUniqueItems());
		assertType(Boolean.class, t.getUniqueItems());
		
		t.uniqueItems(null);
		assertNull(t.getUniqueItems());
	}

	/**
	 * Test method for {@link ParameterInfo#setEnum(java.util.Collection)}.
	 */
	@Test
	public void testSetEnum() {
		ParameterInfo t = new ParameterInfo();
		
		t.setEnum(new ASet<Object>().appendAll("foo","bar"));
		assertObjectEquals("['foo','bar']", t.getEnum());
		assertType(List.class, t.getEnum());
		
		t.setEnum(new ASet<Object>());
		assertObjectEquals("[]", t.getEnum());
		assertType(List.class, t.getEnum());

		t.setEnum(null);
		assertNull(t.getEnum());
	}

	/**
	 * Test method for {@link ParameterInfo#addEnum(java.util.Collection)}.
	 */
	@Test
	public void testAddEnum() {
		ParameterInfo t = new ParameterInfo();
		
		t.addEnum(new ASet<Object>().appendAll("foo","bar"));
		assertObjectEquals("['foo','bar']", t.getEnum());
		assertType(List.class, t.getEnum());
		
		t.addEnum(new ASet<Object>().appendAll("baz"));
		assertObjectEquals("['foo','bar','baz']", t.getEnum());
		assertType(List.class, t.getEnum());

		t.addEnum(null);
		assertObjectEquals("['foo','bar','baz']", t.getEnum());
		assertType(List.class, t.getEnum());
	}

	/**
	 * Test method for {@link ParameterInfo#_enum(java.lang.Object[])}.
	 */
	@Test
	public void test_enum() {
		ParameterInfo t = new ParameterInfo();
		
		t._enum(new ASet<Object>().appendAll("foo","bar"));
		assertObjectEquals("['foo','bar']", t.getEnum());
		assertType(List.class, t.getEnum());
		
		t._enum(new ASet<Object>().appendAll("baz"));
		assertObjectEquals("['foo','bar','baz']", t.getEnum());
		assertType(List.class, t.getEnum());

		t._enum((Object[])null);
		assertObjectEquals("['foo','bar','baz']", t.getEnum());
		assertType(List.class, t.getEnum());
		
		t.setEnum(null);
		t._enum("foo")._enum(new StringBuilder("bar"))._enum("['baz','qux']")._enum((Object)new String[]{"quux"});
		assertObjectEquals("['foo','bar','baz','qux','quux']", t.getEnum());
		assertType(List.class, t.getEnum());
	}

	/**
	 * Test method for {@link ParameterInfo#multipleOf(java.lang.Object)}.
	 */
	@Test
	public void testMultipleOf() {
		ParameterInfo t = new ParameterInfo();
		
		t.multipleOf(123);
		assertEquals(123, t.getMultipleOf());
		assertType(Integer.class, t.getMultipleOf());
		
		t.multipleOf(123f);
		assertEquals(123f, t.getMultipleOf());
		assertType(Float.class, t.getMultipleOf());

		t.multipleOf("123");
		assertEquals(123, t.getMultipleOf());
		assertType(Integer.class, t.getMultipleOf());

		t.multipleOf(new StringBuilder("123"));
		assertEquals(123, t.getMultipleOf());
		assertType(Integer.class, t.getMultipleOf());
		
		t.multipleOf(null);
		assertNull(t.getMultipleOf());
	}

	/**
	 * Test method for {@link ParameterInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		ParameterInfo t = new ParameterInfo();
		
		t
			.set("default", "a")
			.set("enum", new ASet<Object>().appendAll("b"))
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
	
		assertObjectEquals("{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}", t);
		
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
	
		assertObjectEquals("{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}", t);
		
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
	
		assertObjectEquals("{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}", t);

		assertEquals("a", t.get("default", String.class));
		assertEquals("['b']", t.get("enum", String.class));
		assertEquals("true", t.get("allowEmptyValue", String.class));
		assertEquals("c", t.get("collectionFormat", String.class));
		assertEquals("d", t.get("description", String.class));
		assertEquals("true", t.get("exclusiveMaximum", String.class));
		assertEquals("true", t.get("exclusiveMinimum", String.class));
		assertEquals("e", t.get("format", String.class));
		assertEquals("f", t.get("in", String.class));
		assertEquals("{type:'g'}", t.get("items", String.class));
		assertEquals("123.0", t.get("maximum", String.class));
		assertEquals("123", t.get("maxItems", String.class));
		assertEquals("123", t.get("maxLength", String.class));
		assertEquals("123.0", t.get("minimum", String.class));
		assertEquals("123", t.get("minItems", String.class));
		assertEquals("123", t.get("minLength", String.class));
		assertEquals("123.0", t.get("multipleOf", String.class));
		assertEquals("h", t.get("name", String.class));
		assertEquals("i", t.get("pattern", String.class));
		assertEquals("true", t.get("required", String.class));
		assertEquals("{title:'j'}", t.get("schema", String.class));
		assertEquals("k", t.get("type", String.class));
		assertEquals("true", t.get("uniqueItems", String.class));
		assertEquals("ref", t.get("$ref", String.class));
	
		assertType(StringBuilder.class, t.get("default", Object.class));
		assertType(List.class, t.get("enum", Object.class));
		assertType(Boolean.class, t.get("allowEmptyValue", Object.class));
		assertType(String.class, t.get("collectionFormat", Object.class));
		assertType(String.class, t.get("description", Object.class));
		assertType(Boolean.class, t.get("exclusiveMaximum", Object.class));
		assertType(Boolean.class, t.get("exclusiveMinimum", Object.class));
		assertType(String.class, t.get("format", Object.class));
		assertType(String.class, t.get("in", Object.class));
		assertType(Items.class, t.get("items", Object.class));
		assertType(Float.class, t.get("maximum", Object.class));
		assertType(Integer.class, t.get("maxItems", Object.class));
		assertType(Integer.class, t.get("maxLength", Object.class));
		assertType(Float.class, t.get("minimum", Object.class));
		assertType(Integer.class, t.get("minItems", Object.class));
		assertType(Integer.class, t.get("minLength", Object.class));
		assertType(Float.class, t.get("multipleOf", Object.class));
		assertType(String.class, t.get("name", Object.class));
		assertType(String.class, t.get("pattern", Object.class));
		assertType(Boolean.class, t.get("required", Object.class));
		assertType(SchemaInfo.class, t.get("schema", Object.class));
		assertType(String.class, t.get("type", Object.class));
		assertType(Boolean.class, t.get("uniqueItems", Object.class));
		assertType(StringBuilder.class, t.get("$ref", Object.class));
	
		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
		
		String s = "{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}";
		assertObjectEquals(s, JsonParser.DEFAULT.parse(s, ParameterInfo.class));
	}
}
