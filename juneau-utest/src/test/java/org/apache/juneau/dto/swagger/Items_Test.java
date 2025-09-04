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
import org.apache.juneau.common.internal.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Items}.
 */
class Items_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new Items();

		// General - Combined assertBean test
		assertBean(
			t.setCollectionFormat("a").setDefault("b").setEnum(set("c1","c2")).setExclusiveMaximum(true)
				.setExclusiveMinimum(true).setFormat("d").setItems(items("e")).setMaxItems(1).setMaxLength(2)
				.setMaximum(3).setMinItems(4).setMinLength(5).setMinimum(6).setMultipleOf(7)
				.setPattern("f").setType("g").setUniqueItems(true),
			"collectionFormat,default,enum,exclusiveMaximum,exclusiveMinimum,format,items{type},maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,pattern,type,uniqueItems",
			"a,b,[c1,c2],true,true,d,{e},1,2,3,4,5,6,7,f,g,true"
		);

		// Null cases
		assertBean(
			t.setCollectionFormat(null).setDefault(null).setEnum((Collection<Object>)null).setFormat(null).setPattern(null).setType(null),
			"collectionFormat,default,enum,format,pattern,type",
			"null,null,null,null,null,null"
		);

		// Empty collections
		assertBean(
			t.setEnum(set()),
			"enum",
			"[]"
		);

		// Other
		assertBean(
			t.setDefault(Utils.sb("a")).setMaximum(1f).setMinimum(2f).setMultipleOf(3f),
			"default,maximum,minimum,multipleOf", "a,1.0,2.0,3.0");

		// addEnum
		assertSet(t.addEnum("a","b").getEnum(), "a", "b");
		assertSet(t.addEnum("c").getEnum(), "a", "b", "c");
	}

	/**
	 * Test method for {@link Items#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() {
		var t = new Items();

		t
			.set("collectionFormat", "c")
			.set("default", "a")
			.set("enum", set("b"))
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "g")
			.set("items", items("h"))
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("maximum", 1f)
			.set("minItems", 5)
			.set("minLength", 6)
			.set("minimum", 4f)
			.set("multipleOf", 7f)
			.set("pattern", "i")
			.set("$ref", "k")
			.set("type", "j")
			.set("uniqueItems", true);

		assertBean(t,
			"collectionFormat,default,enum,exclusiveMaximum,exclusiveMinimum,format,items{type},maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,pattern,ref,type,uniqueItems",
			"c,a,[b],true,true,g,{h},2,3,1.0,5,6,4.0,7.0,i,k,j,true");

		t
			.set("default", "a")
			.set("enum", "['b']")
			.set("collectionFormat", "c")
			.set("exclusiveMaximum", "true")
			.set("exclusiveMinimum", "true")
			.set("format", "g")
			.set("items", "{type:'h'}")
			.set("maximum", "1f")
			.set("maxItems", "2")
			.set("maxLength", "3")
			.set("minimum", "4f")
			.set("minItems", "5")
			.set("minLength", "6")
			.set("multipleOf", "7f")
			.set("pattern", "i")
			.set("type", "j")
			.set("uniqueItems", "true")
			.set("$ref", "k");

		assertBean(t,
			"type,format,items{type},collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,ref",
			"j,g,{h},c,a,1.0,true,4.0,true,3,6,i,2,5,true,[b],7.0,k");

		t
			.set("collectionFormat", Utils.sb("c"))
			.set("default", Utils.sb("a"))
			.set("enum", Utils.sb("['b']"))
			.set("exclusiveMaximum", Utils.sb("true"))
			.set("exclusiveMinimum", Utils.sb("true"))
			.set("format", Utils.sb("g"))
			.set("items", Utils.sb("{type:'h'}"))
			.set("maxItems", Utils.sb("2"))
			.set("maxLength", Utils.sb("3"))
			.set("maximum", Utils.sb("1f"))
			.set("minItems", Utils.sb("5"))
			.set("minLength", Utils.sb("6"))
			.set("minimum", Utils.sb("4f"))
			.set("multipleOf", Utils.sb("7f"))
			.set("pattern", Utils.sb("i"))
			.set("$ref", Utils.sb("k"))
			.set("type", Utils.sb("j"))
			.set("uniqueItems", Utils.sb("true"));

		assertBean(t, "collectionFormat,default,enum,exclusiveMaximum,exclusiveMinimum,format,items{type},maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,pattern,ref,type,uniqueItems", "c,a,[b],true,true,g,{h},2,3,1.0,5,6,4.0,7.0,i,k,j,true");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"collectionFormat,default,enum,exclusiveMaximum,exclusiveMinimum,format,items,maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,pattern,$ref,type,uniqueItems",
			"c,a,['b'],true,true,g,{type:'h'},2,3,1.0,5,6,4.0,7.0,i,k,j,true");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"collectionFormat,default,enum,exclusiveMaximum,exclusiveMinimum,format,items,maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,pattern,$ref,type,uniqueItems",
			"String,StringBuilder,LinkedHashSet,Boolean,Boolean,String,Items,Integer,Integer,Float,Integer,Integer,Float,Float,String,String,String,Boolean");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:1.0,exclusiveMaximum:true,minimum:4.0,exclusiveMinimum:true,maxLength:3,minLength:6,pattern:'i',maxItems:2,minItems:5,uniqueItems:true,'enum':['b'],multipleOf:7.0,'$ref':'k'}";
		assertJson(JsonParser.DEFAULT.parse(s, Items.class), s);
	}

	@Test void b03_copy() {
		var t = new Items();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("collectionFormat", "c")
			.set("default", "a")
			.set("enum", set("b"))
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "g")
			.set("items", items("h"))
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("maximum", 1f)
			.set("minItems", 5)
			.set("minLength", 6)
			.set("minimum", 4f)
			.set("multipleOf", 7f)
			.set("pattern", "i")
			.set("$ref", "k")
			.set("type", "j")
			.set("uniqueItems", true)
			.copy();

		assertBean(t,
			"collectionFormat,default,enum,exclusiveMaximum,exclusiveMinimum,format,items{type},maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,pattern,ref,type,uniqueItems",
			"c,a,[b],true,true,g,{h},2,3,1.0,5,6,4.0,7.0,i,k,j,true");
	}

	@Test void b04_keySet() {
		var t = new Items();

		assertEmpty(t.keySet());

		t
			.set("collectionFormat", "c")
			.set("default", "a")
			.set("enum", set("b"))
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "g")
			.set("items", items("h"))
			.set("maximum", 1f)
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("minimum", 4f)
			.set("minItems", 5)
			.set("minLength", 6)
			.set("multipleOf", 7f)
			.set("pattern", "i")
			.set("$ref", "k")
			.set("type", "j")
			.set("uniqueItems", true);

		assertSet(
			t.keySet(),
			"collectionFormat", "default", "enum", "exclusiveMaximum", "exclusiveMinimum", "format", "items", "maximum", "maxItems", "maxLength", "minimum", "minItems", "minLength", "multipleOf", "pattern", "$ref", "type", "uniqueItems"
		);
	}
}