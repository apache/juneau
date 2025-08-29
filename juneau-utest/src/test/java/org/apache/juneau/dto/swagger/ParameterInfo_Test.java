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
 * Testcase for {@link ParameterInfo}.
 */
class ParameterInfo_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new ParameterInfo();
		assertBean(t.setName("foo"), "name", "foo");
		assertNull(t.setName(null).getName());
		assertBean(t.setIn("foo"), "in", "foo");
		assertNull(t.setIn(null).getIn());
		assertBean(t.setDescription("foo"), "description", "foo");
		assertNull(t.setDescription(null).getDescription());
		assertTrue(t.setRequired(true).getRequired());
		assertJson(t.setSchema(schemaInfo().setTitle("foo")).getSchema(), "{title:'foo'}");
		assertBean(t.setType("foo"), "type", "foo");
		assertNull(t.setType(null).getType());
		assertBean(t.setFormat("foo"), "format", "foo");
		assertNull(t.setFormat(null).getFormat());
		assertTrue(t.setAllowEmptyValue(true).getAllowEmptyValue());
		assertJson(t.setItems(items("foo")).getItems(), "{type:'foo'}");
		assertBean(t.setCollectionFormat("foo"), "collectionFormat", "foo");
		assertNull(t.setCollectionFormat(null).getCollectionFormat());
		assertBean(t.setDefault("foo"), "default", "foo");
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
		assertBean(t.setPattern("foo"), "pattern", "foo");
		assertNull(t.setPattern(null).getPattern());
		assertEquals(123, t.setMaxItems(123).getMaxItems());
		assertEquals(123, t.setMinItems(123).getMinItems());
		assertTrue(t.setUniqueItems(true).getUniqueItems());
		assertJson(t.setEnum(set("foo","bar")).getEnum(), "['foo','bar']");
		assertJson(t.setEnum(set()).getEnum(), "[]");
		assertNull(t.setEnum((Collection<Object>)null).getEnum());
		assertJson(t.addEnum("foo","bar").getEnum(), "['foo','bar']");
		assertJson(t.addEnum("baz").getEnum(), "['foo','bar','baz']");
		assertEquals(123, t.setMultipleOf(123).getMultipleOf());
		assertEquals(123f, t.setMultipleOf(123f).getMultipleOf());
	}

	/**
	 * Test method for {@link ParameterInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void a01_set() throws Exception {
		var t = new ParameterInfo();

		t
			.set("default", "a")
			.set("enum", set("b"))
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
			.set("schema", schemaInfo().setTitle("j"))
			.set("type", "k")
			.set("uniqueItems", true)
			.set("$ref", "ref");

		assertJson(t, "{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

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

		assertJson(t, "{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

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

		assertJson(t, "{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"default,enum,allowEmptyValue,collectionFormat,description,exclusiveMaximum,exclusiveMinimum,format,in,items,maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,name,pattern,required,schema,type,uniqueItems,$ref",
			"a,['b'],true,c,d,true,true,e,f,{type:'g'},123.0,123,123,123.0,123,123,123.0,h,i,true,{title:'j'},k,true,ref");

		assertType(StringBuilder.class, t.get("default", Object.class));
		assertType(Set.class, t.get("enum", Object.class));
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

		var s = "{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, ParameterInfo.class), s);
	}

	@Test void b02_copy() {
		var t = new ParameterInfo();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("default", "a")
			.set("enum", set("b"))
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
			.set("schema", schemaInfo().setTitle("j"))
			.set("type", "k")
			.set("uniqueItems", true)
			.set("$ref", "ref")
			.copy();

		assertJson(t, "{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");
	}

	@Test void b03_keySet() {
		var t = new ParameterInfo();

		assertJson(t.keySet(), "[]");

		t
			.set("allowEmptyValue", true)
			.set("collectionFormat", "c")
			.set("default", "a")
			.set("description", "d")
			.set("enum", set("b"))
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
			.set("schema", schemaInfo().setTitle("j"))
			.set("type", "k")
			.set("uniqueItems", true)
			.set("$ref", "ref");

		assertJson(t.keySet(),
			"['allowEmptyValue','collectionFormat','default','description','enum','exclusiveMaximum','exclusiveMinimum',"
			+ "'format','in','items','maximum','maxItems','maxLength','minimum','minItems','minLength','multipleOf','name',"
			+ "'pattern','required','schema','type','uniqueItems','$ref']"
		);
	}
}