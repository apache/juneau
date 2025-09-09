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
 * Testcase for {@link HeaderInfo}.
 */
class HeaderInfo_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new HeaderInfo();

		// General - Combined assertBean test
		assertBean(
			t.setCollectionFormat("a").setDefault("b").setDescription("c").setEnum(set("d1","d2")).setExample("e")
				.setExclusiveMaximum(true).setExclusiveMinimum(true).setFormat("f").setItems(items("g"))
				.setMaxItems(1).setMaxLength(2).setMaximum(3).setMinItems(4).setMinLength(5)
				.setMinimum(6).setMultipleOf(7).setPattern("h").setRef("i").setType("j").setUniqueItems(true),
			"collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items{type},maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,pattern,ref,type,uniqueItems",
			"a,b,c,[d1,d2],e,true,true,f,{g},1,2,3,4,5,6,7,h,i,j,true"
		);

		// Null cases
		assertBean(
			t.setCollectionFormat(null).setDefault(null).setDescription(null).setEnum((Set<Object>)null).setExample((String)null)
				.setFormat(null).setItems((Items)null).setPattern(null).setRef(null).setType(null),
			"collectionFormat,default,description,enum,example,format,items,pattern,ref,type",
			"<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>"
		);

		// Empty collections
		assertBean(
			t.setEnum(set()),
			"enum",
			"[]"
		);

		// Other
		assertBean(
			t.setExample(1f).setMaximum(2f).setMinimum(3f).setMultipleOf(4f),
			"example,maximum,minimum,multipleOf", "1.0,2.0,3.0,4.0");

		// addEnum
		assertList(t.addEnum("a","b").getEnum(), "a", "b");
		assertList(t.addEnum("c").getEnum(), "a", "b", "c");
	}

	/**
	 * Test method for {@link HeaderInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() {
		var t = new HeaderInfo();

		t
			.set("collectionFormat", "a")
			.set("default", "b")
			.set("description", "c")
			.set("enum", set("d"))
			.set("example", "e")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "f")
			.set("items", items("g"))
			.set("maximum", 1f)
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("minimum", 4f)
			.set("minItems", 5)
			.set("minLength", 6)
			.set("multipleOf", 7f)
			.set("pattern", "h")
			.set("$ref", "i")
			.set("type", "j")
			.set("uniqueItems", true);

		assertBean(t,
			"collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items{type},maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,ref,type,uniqueItems",
			"a,b,c,[d],e,true,true,f,{g},1.0,2,3,4.0,5,6,7.0,h,i,j,true");

		t
			.set("collectionFormat", "a")
			.set("default", "b")
			.set("description", "c")
			.set("enum", "['d']")
			.set("example", "e")
			.set("exclusiveMaximum", "true")
			.set("exclusiveMinimum", "true")
			.set("format", "f")
			.set("items", "{type:'g'}")
			.set("maximum", "1f")
			.set("maxItems", "2")
			.set("maxLength", "3")
			.set("minimum", "4f")
			.set("minItems", "5")
			.set("minLength", "6")
			.set("multipleOf", "7f")
			.set("pattern", "h")
			.set("$ref", "i")
			.set("type", "j")
			.set("uniqueItems", "true");

		assertBean(t,
			"collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items{type},maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,ref,type,uniqueItems",
			"a,b,c,[d],e,true,true,f,{g},1.0,2,3,4.0,5,6,7.0,h,i,j,true");

		t
			.set("collectionFormat", Utils.sb("a"))
			.set("default", Utils.sb("b"))
			.set("description", Utils.sb("c"))
			.set("enum", Utils.sb("['d']"))
			.set("example", Utils.sb("e"))
			.set("exclusiveMaximum", Utils.sb("true"))
			.set("exclusiveMinimum", Utils.sb("true"))
			.set("format", Utils.sb("f"))
			.set("items", Utils.sb("{type:'g'}"))
			.set("maximum", Utils.sb("1f"))
			.set("maxItems", Utils.sb("2"))
			.set("maxLength", Utils.sb("3"))
			.set("minimum", Utils.sb("4f"))
			.set("minItems", Utils.sb("5"))
			.set("minLength", Utils.sb("6"))
			.set("multipleOf", Utils.sb("7f"))
			.set("pattern", Utils.sb("h"))
			.set("$ref", Utils.sb("i"))
			.set("type", Utils.sb("j"))
			.set("uniqueItems", Utils.sb("true"));

		assertBean(t,
			"collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items{type},maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,ref,type,uniqueItems",
			"a,b,c,[d],e,true,true,f,{g},1.0,2,3,4.0,5,6,7.0,h,i,j,true");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class),
			"collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items,maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,$ref,type,uniqueItems",
			"a,b,c,[d],e,true,true,f,{\"type\":\"g\"},1.0,2,3,4.0,5,6,7.0,h,i,j,true");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{description:'d',type:'j',format:'g',items:{type:'h'},collectionFormat:'c','default':'a',maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'i',maxItems:123,minItems:123,uniqueItems:true,'enum':['b'],multipleOf:123.0,'$ref':'ref',example:'e'}";
		assertJson(s, JsonParser.DEFAULT.parse(s, HeaderInfo.class));
	}

	@Test void b03_copy() {
		var t = new HeaderInfo();

		t = t.copy();

		assertBean(t,
			"collectionFormat,default,description,enum,exclusiveMaximum,exclusiveMinimum,format,items,maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,type,uniqueItems",
			"<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>");

		t
			.set("collectionFormat", "a")
			.set("default", "b")
			.set("description", "c")
			.set("enum", set("d"))
			.set("example", "e")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "f")
			.set("items", items("g"))
			.set("maximum", 1f)
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("minimum", 4f)
			.set("minItems", 5)
			.set("minLength", 6)
			.set("multipleOf", 7f)
			.set("pattern", "h")
			.set("$ref", "i")
			.set("type", "j")
			.set("uniqueItems", true)
			.copy();

		assertBean(t,
			"collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items{type},maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,ref,type,uniqueItems",
			"a,b,c,[d],e,true,true,f,{g},1.0,2,3,4.0,5,6,7.0,h,i,j,true");
	}

	@Test void b04_keySet() {
		var t = new HeaderInfo();

		assertEmpty(t.keySet());

		t
			.set("collectionFormat", "a")
			.set("default", "b")
			.set("description", "c")
			.set("enum", set("d"))
			.set("example", "e")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("format", "f")
			.set("items", items("g"))
			.set("maximum", 1f)
			.set("maxItems", 2)
			.set("maxLength", 3)
			.set("minimum", 4f)
			.set("minItems", 5)
			.set("minLength", 6)
			.set("multipleOf", 7f)
			.set("pattern", "h")
			.set("$ref", "i")
			.set("type", "j")
			.set("uniqueItems", true);

		assertList(t.keySet(), "collectionFormat", "default", "description", "enum", "example", "exclusiveMaximum", "exclusiveMinimum", "format", "items", "maximum", "maxItems", "maxLength", "minimum", "minItems", "minLength", "multipleOf", "pattern", "$ref", "type", "uniqueItems");
	}

	@Test void c01_strict() {
		var t = new HeaderInfo().strict();
		t.setCollectionFormat("csv");
		assertThrowsWithMessage(Exception.class, "Invalid", ()->t.setCollectionFormat("foo"));
		t.setType("string");
		assertThrowsWithMessage(Exception.class, "Invalid", ()->t.setType("foo"));
	}

	@Test void c02_resolveRefs() {
		var s = new Swagger();
		var t = new HeaderInfo();
		assertDoesNotThrow(()->t.resolveRefs(s, new ArrayDeque<>(), 10));
	}
}