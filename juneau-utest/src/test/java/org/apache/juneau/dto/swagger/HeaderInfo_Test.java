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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link HeaderInfo}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class HeaderInfo_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		HeaderInfo t = new HeaderInfo();
		assertString(t.setDescription("foo").getDescription()).is("foo");
		assertString(t.setDescription(null).getDescription()).isNull();
		assertString(t.setType("foo").getType()).is("foo");
		assertString(t.setType(null).getType()).isNull();
		assertString(t.setFormat("foo").getFormat()).is("foo");
		assertString(t.setFormat(null).getFormat()).isNull();
		assertObject(t.setItems(items("foo")).getItems()).asJson().is("{type:'foo'}");
		assertObject(t.setItems((Items)null).getItems()).isNull();
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
		assertObject(t.setEnum("foo","bar").getEnum()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.setEnum((Set<Object>)null).getEnum()).isNull();
		assertObject(t.addEnum("foo","bar").getEnum()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.addEnum("baz").getEnum()).isType(Set.class).asJson().is("['foo','bar','baz']");
		assertObject(t.setMultipleOf(123).getMultipleOf()).isType(Integer.class).is(123);
		assertObject(t.setMultipleOf(123f).getMultipleOf()).isType(Float.class).is(123f);
		assertObject(t.setExample("foo").getExample()).isType(String.class).is("foo");
		assertObject(t.setExample(123f).getExample()).isType(Float.class).is(123f);
		assertObject(t.setExample((String)null).getExample()).isNull();
		assertString(t.setRef("foo").getRef()).is("foo");
		assertObject(t.setRef(null).getRef()).isNull();
	}

	/**
	 * Test method for {@link HeaderInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		HeaderInfo t = new HeaderInfo();

		t
			.set("default", "a")
			.set("enum", set("b"))
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
		assertObject(t.get("enum", Object.class)).isType(Set.class).asJson().is("['b']");
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
			.set("enum", set("b"))
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
			.set("enum", set("b"))
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
		t.setCollectionFormat("csv");
		assertThrown(()->t.setCollectionFormat("foo")).asMessage().isContains("Invalid");
		t.setType("string");
		assertThrown(()->t.setType("foo")).asMessage().isContains("Invalid");
	}

	@Test
	public void c02_resolveRefs() throws Exception {
		Swagger s = new Swagger();
		HeaderInfo t = new HeaderInfo();
		t.resolveRefs(s, new ArrayDeque<String>(), 10);
	}
}
