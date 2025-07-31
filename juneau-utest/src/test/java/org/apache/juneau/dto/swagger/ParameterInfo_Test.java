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
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link ParameterInfo}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ParameterInfo_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		ParameterInfo t = new ParameterInfo();
		assertString(t.setName("foo").getName()).is("foo");
		assertObject(t.setName(null).getName()).isNull();
		assertString(t.setIn("foo").getIn()).is("foo");
		assertString(t.setIn(null).getIn()).isNull();
		assertString(t.setDescription("foo").getDescription()).is("foo");
		assertString(t.setDescription(null).getDescription()).isNull();
		assertObject(t.setRequired(true).getRequired()).isType(Boolean.class).is(true);
		assertObject(t.setSchema(schemaInfo().setTitle("foo")).getSchema()).asJson().is("{title:'foo'}");
		assertString(t.setType("foo").getType()).is("foo");
		assertString(t.setType(null).getType()).isNull();
		assertString(t.setFormat("foo").getFormat()).is("foo");
		assertString(t.setFormat(null).getFormat()).isNull();
		assertObject(t.setAllowEmptyValue(true).getAllowEmptyValue()).isType(Boolean.class).is(true);
		assertObject(t.setItems(items("foo")).getItems()).asJson().is("{type:'foo'}");
		assertString(t.setCollectionFormat("foo").getCollectionFormat()).is("foo");
		assertString(t.setCollectionFormat(null).getCollectionFormat()).isNull();
		assertString(t.setDefault("foo").getDefault()).is("foo");
		assertObject(t.setDefault(new StringBuilder("foo")).getDefault()).isType(StringBuilder.class).asString().is("foo");
		assertObject(t.setDefault(null).getDefault()).isNull();
		assertObject(t.setMaximum(123).getMaximum()).isType(Integer.class).is(123);
		assertObject(t.setMaximum(123f).getMaximum()).isType(Float.class).is(123f);
		assertObject(t.setExclusiveMaximum(true).getExclusiveMaximum()).isType(Boolean.class).is(true);
		assertObject(t.setMinimum(123).getMinimum()).isType(Integer.class).is(123);
		assertObject(t.setMinimum(123f).getMinimum()).isType(Float.class).is(123f);
		assertObject(t.setExclusiveMinimum(true).getExclusiveMinimum()).isType(Boolean.class).is(true);
		assertObject(t.setMaxLength(123).getMaxLength()).isType(Integer.class).is(123);
		assertObject(t.setMinLength(123).getMinLength()).isType(Integer.class).is(123);
		assertString(t.setPattern("foo").getPattern()).is("foo");
		assertString(t.setPattern(null).getPattern()).isNull();
		assertObject(t.setMaxItems(123).getMaxItems()).isType(Integer.class).is(123);
		assertObject(t.setMinItems(123).getMinItems()).isType(Integer.class).is(123);
		assertObject(t.setUniqueItems(true).getUniqueItems()).isType(Boolean.class).is(true);
		assertObject(t.setEnum(set("foo","bar")).getEnum()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.setEnum(set()).getEnum()).isType(Set.class).asJson().is("[]");
		assertObject(t.setEnum((Collection<Object>)null).getEnum()).isNull();
		assertObject(t.addEnum("foo","bar").getEnum()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.addEnum("baz").getEnum()).isType(Set.class).asJson().is("['foo','bar','baz']");
		assertObject(t.setMultipleOf(123).getMultipleOf()).isType(Integer.class).is(123);
		assertObject(t.setMultipleOf(123f).getMultipleOf()).isType(Float.class).is(123f);
	}

	/**
	 * Test method for {@link ParameterInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		ParameterInfo t = new ParameterInfo();

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
	public void b02_copy() {
		ParameterInfo t = new ParameterInfo();

		t = t.copy();

		assertObject(t).asJson().is("{}");

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

		assertObject(t).asJson().is("{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}");
	}

	@Test
	public void b03_keySet() {
		ParameterInfo t = new ParameterInfo();

		assertObject(t.keySet()).asJson().is("[]");

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

		assertObject(t.keySet()).asJson().is(
			"['allowEmptyValue','collectionFormat','default','description','enum','exclusiveMaximum','exclusiveMinimum',"
			+ "'format','in','items','maximum','maxItems','maxLength','minimum','minItems','minLength','multipleOf','name',"
			+ "'pattern','required','schema','type','uniqueItems','$ref']"
		);
	}
}