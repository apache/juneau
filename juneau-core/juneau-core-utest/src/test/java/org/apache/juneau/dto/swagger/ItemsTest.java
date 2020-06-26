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
 * Testcase for {@link Items}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ItemsTest {

	/**
	 * Test method for {@link Items#type(java.lang.Object)}.
	 */
	@Test
	public void testType() {
		Items t = new Items();

		t.type("foo");
		assertEquals("foo", t.getType());

		t.type(new StringBuilder("foo"));
		assertEquals("foo", t.getType());
		assertObject(t.getType()).isType(String.class);

		t.type(null);
		assertNull(t.getType());
	}

	/**
	 * Test method for {@link Items#format(java.lang.Object)}.
	 */
	@Test
	public void testFormat() {
		Items t = new Items();

		t.format("foo");
		assertEquals("foo", t.getFormat());

		t.format(new StringBuilder("foo"));
		assertEquals("foo", t.getFormat());
		assertObject(t.getFormat()).isType(String.class);

		t.format(null);
		assertNull(t.getFormat());
	}

	/**
	 * Test method for {@link Items#items(java.lang.Object)}.
	 */
	@Test
	public void testItems() {
		Items t = new Items();

		t.items(items("foo"));
		assertObject(t.getItems()).json().is("{type:'foo'}");

		t.items("{type:'foo'}");
		assertObject(t.getItems()).json().is("{type:'foo'}");
		assertObject(t.getItems()).isType(Items.class);

		t.items(null);
		assertNull(t.getItems());
	}

	/**
	 * Test method for {@link Items#collectionFormat(java.lang.Object)}.
	 */
	@Test
	public void testCollectionFormat() {
		Items t = new Items();

		t.collectionFormat("foo");
		assertEquals("foo", t.getCollectionFormat());

		t.collectionFormat(new StringBuilder("foo"));
		assertEquals("foo", t.getCollectionFormat());
		assertObject(t.getCollectionFormat()).isType(String.class);

		t.collectionFormat(null);
		assertNull(t.getCollectionFormat());
	}

	/**
	 * Test method for {@link Items#_default(java.lang.Object)}.
	 */
	@Test
	public void test_default() {
		Items t = new Items();

		t._default("foo");
		assertEquals("foo", t.getDefault());

		t._default(new StringBuilder("foo"));
		assertEquals("foo", t.getDefault().toString());
		assertObject(t.getDefault()).isType(StringBuilder.class);

		t._default(null);
		assertNull(t.getDefault());
	}

	/**
	 * Test method for {@link Items#maximum(java.lang.Object)}.
	 */
	@Test
	public void testMaximum() {
		Items t = new Items();

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
	 * Test method for {@link Items#exclusiveMaximum(java.lang.Object)}.
	 */
	@Test
	public void testExclusiveMaximum() {
		Items t = new Items();

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
	 * Test method for {@link Items#minimum(java.lang.Object)}.
	 */
	@Test
	public void testMinimum() {
		Items t = new Items();

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
	 * Test method for {@link Items#exclusiveMinimum(java.lang.Object)}.
	 */
	@Test
	public void testExclusiveMinimum() {
		Items t = new Items();

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
	 * Test method for {@link Items#maxLength(java.lang.Object)}.
	 */
	@Test
	public void testMaxLength() {
		Items t = new Items();

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
	 * Test method for {@link Items#minLength(java.lang.Object)}.
	 */
	@Test
	public void testMinLength() {
		Items t = new Items();

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
	 * Test method for {@link Items#pattern(java.lang.Object)}.
	 */
	@Test
	public void testPattern() {
		Items t = new Items();

		t.pattern("foo");
		assertEquals("foo", t.getPattern());

		t.pattern(new StringBuilder("foo"));
		assertEquals("foo", t.getPattern());
		assertObject(t.getPattern()).isType(String.class);

		t.pattern(null);
		assertNull(t.getPattern());
	}

	/**
	 * Test method for {@link Items#maxItems(java.lang.Object)}.
	 */
	@Test
	public void testMaxItems() {
		Items t = new Items();

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
	 * Test method for {@link Items#minItems(java.lang.Object)}.
	 */
	@Test
	public void testMinItems() {
		Items t = new Items();

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
	 * Test method for {@link Items#uniqueItems(java.lang.Object)}.
	 */
	@Test
	public void testUniqueItems() {
		Items t = new Items();

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
	 * Test method for {@link Items#setEnum(java.util.Collection)}.
	 */
	@Test
	public void testSetEnum() {
		Items t = new Items();

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
	 * Test method for {@link Items#addEnum(java.util.Collection)}.
	 */
	@Test
	public void testAddEnum() {
		Items t = new Items();

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
	 * Test method for {@link Items#_enum(java.lang.Object[])}.
	 */
	@Test
	public void test_enum() {
		Items t = new Items();

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
	 * Test method for {@link Items#multipleOf(java.lang.Object)}.
	 */
	@Test
	public void testMultipleOf() {
		Items t = new Items();

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
	 * Test method for {@link Items#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
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

		assertObject(t).json().is("{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

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

		assertObject(t).json().is("{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

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

		assertObject(t).json().is("{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

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

		assertObject(t.get("default", Object.class)).isType(StringBuilder.class);
		assertObject(t.get("enum", Object.class)).isType(List.class);
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
		assertObject(JsonParser.DEFAULT.parse(s, Items.class)).json().is(s);
	}
}
