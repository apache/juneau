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

		// General - Combined assertBean test
		assertBean(
			t.setAllowEmptyValue(true).setCollectionFormat("a").setDefault("b").setDescription("c").setEnum(set("d1","d2"))
				.setExclusiveMaximum(true).setExclusiveMinimum(true).setFormat("e").setIn("f").setItems(items("g"))
				.setMaxItems(1).setMaxLength(2).setMaximum(3).setMinItems(4).setMinLength(5).setMinimum(6)
				.setMultipleOf(7).setName("h").setPattern("i").setRequired(true).setSchema(schemaInfo().setTitle("j"))
				.setType("k").setUniqueItems(true),
			"allowEmptyValue,collectionFormat,default,description,enum,exclusiveMaximum,exclusiveMinimum,format,in,items{type},maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,name,pattern,required,schema{title},type,uniqueItems",
			"true,a,b,c,[d1,d2],true,true,e,f,{g},1,2,3,4,5,6,7,h,i,true,{j},k,true"
		);

		// Null cases
		assertBean(
			t.setCollectionFormat(null).setDefault(null).setDescription(null).setEnum((Collection<Object>)null)
				.setFormat(null).setIn(null).setName(null).setPattern(null).setType(null),
			"collectionFormat,default,description,enum,format,in,name,pattern,type",
			"null,null,null,null,null,null,null,null,null"
		);

		// Empty collections
		assertBean(
			t.setEnum(set()),
			"enum",
			"[]"
		);

		// Other
		assertBean(
			t.setDefault(sb("a")).setMaximum(1f).setMinimum(2f).setMultipleOf(3f),
			"default,maximum,minimum,multipleOf", "a,1.0,2.0,3.0");

		// addEnum
		assertSet(t.addEnum("a","b").getEnum(), "a,b");
		assertSet(t.addEnum("c").getEnum(), "a,b,c");
	}

	/**
	 * Test method for {@link ParameterInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void a01_set() {
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
			.set("maximum", 1f)
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("minimum", 4f)
			.set("minItems", 5)
			.set("minLength", 6)
			.set("multipleOf", 7f)
			.set("name", "h")
			.set("pattern", "i")
			.set("required", true)
			.set("schema", schemaInfo().setTitle("j"))
			.set("type", "k")
			.set("uniqueItems", true)
			.set("$ref", "l");

		assertBean(t,
			"in,name,type,description,required,schema{title},format,allowEmptyValue,items{type},collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,$ref",
			"f,h,k,d,true,{j},e,true,{g},c,a,1.0,true,4.0,true,3,6,i,2,5,true,[b],7.0,l");

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
			.set("maximum", "1.0")
			.set("maxItems", "2")
			.set("maxLength", "3")
			.set("minimum", "4.0")
			.set("minItems", "5")
			.set("minLength", "6")
			.set("multipleOf", "7.0")
			.set("name", "h")
			.set("pattern", "i")
			.set("required", "true")
			.set("schema", "{title:'j'}")
			.set("type", "k")
			.set("uniqueItems", "true")
			.set("$ref", "l");

		assertBean(t,
			"in,name,type,description,required,schema{title},format,allowEmptyValue,items{type},collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,$ref",
			"f,h,k,d,true,{j},e,true,{g},c,a,1.0,true,4.0,true,3,6,i,2,5,true,[b],7.0,l");

		t
			.set("default", sb("a"))
			.set("enum", sb("['b']"))
			.set("allowEmptyValue", sb("true"))
			.set("collectionFormat", sb("c"))
			.set("description", sb("d"))
			.set("exclusiveMaximum", sb("true"))
			.set("exclusiveMinimum", sb("true"))
			.set("format", sb("e"))
			.set("in", sb("f"))
			.set("items", sb("{type:'g'}"))
			.set("maxItems", sb("2"))
			.set("maxLength", sb("3"))
			.set("maximum", sb("1.0"))
			.set("minItems", sb("5"))
			.set("minLength", sb("6"))
			.set("minimum", sb("4.0"))
			.set("multipleOf", sb("7.0"))
			.set("name", sb("h"))
			.set("pattern", sb("i"))
			.set("$ref", sb("l"))
			.set("required", sb("true"))
			.set("schema", sb("{title:'j'}"))
			.set("type", sb("k"))
			.set("uniqueItems", sb("true"));

		assertBean(t,
			"allowEmptyValue,collectionFormat,default,description,enum,exclusiveMaximum,exclusiveMinimum,format,in,items{type},maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,name,pattern,$ref,required,schema{title},type,uniqueItems",
			"true,c,a,d,[b],true,true,e,f,{g},2,3,1.0,5,6,4.0,7.0,h,i,l,true,{j},k,true");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"allowEmptyValue,collectionFormat,default,description,enum,exclusiveMaximum,exclusiveMinimum,format,in,items,maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,name,pattern,$ref,required,schema,type,uniqueItems",
			"true,c,a,d,['b'],true,true,e,f,{type:'g'},2,3,1.0,5,6,4.0,7.0,h,i,l,true,{title:'j'},k,true");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"allowEmptyValue,collectionFormat,default,description,enum,exclusiveMaximum,exclusiveMinimum,format,in,items,maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,name,pattern,$ref,required,schema,type,uniqueItems",
			"Boolean,String,StringBuilder,String,LinkedHashSet,Boolean,Boolean,String,String,Items,Integer,Integer,Float,Integer,Integer,Float,Float,String,String,StringBuilder,Boolean,SchemaInfo,String,Boolean");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{'in':'f',name:'h',type:'k',description:'d',required:true,schema:{title:'j'},format:'e',allowEmptyValue:true,items:{type:'g'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, ParameterInfo.class), s);
	}

	@Test void b03_copy() {
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
			.set("maximum", 1f)
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("minimum", 4f)
			.set("minItems", 5)
			.set("minLength", 6)
			.set("multipleOf", 7f)
			.set("name", "h")
			.set("pattern", "i")
			.set("required", true)
			.set("schema", schemaInfo().setTitle("j"))
			.set("type", "k")
			.set("uniqueItems", true)
			.set("$ref", "l")
			.copy();

		assertBean(t,
			"in,name,type,description,required,schema{title},format,allowEmptyValue,items{type},collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,$ref",
			"f,h,k,d,true,{j},e,true,{g},c,a,1.0,true,4.0,true,3,6,i,2,5,true,[b],7.0,l");
	}

	@Test void b04_keySet() {
		var t = new ParameterInfo();

		assertEmpty(t.keySet());

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
			.set("maximum", 1f)
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("minimum", 4f)
			.set("minItems", 5)
			.set("minLength", 6)
			.set("multipleOf", 7f)
			.set("name", "h")
			.set("pattern", "i")
			.set("required", true)
			.set("schema", schemaInfo().setTitle("j"))
			.set("type", "k")
			.set("uniqueItems", true)
			.set("$ref", "l");

		assertSet(t.keySet(), "allowEmptyValue,collectionFormat,default,description,enum,exclusiveMaximum,exclusiveMinimum,format,in,items,maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,name,pattern,required,schema,type,uniqueItems,$ref");
	}
}