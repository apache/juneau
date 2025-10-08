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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link HeaderInfo}.
 */
class HeaderInfo_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<HeaderInfo> TESTER =
			testBean(
				bean()
					.setCollectionFormat("a")
					.setDefault("b")
					.setDescription("c")
					.setEnum(set("d"))
					.setExample("e")
					.setExclusiveMaximum(true)
					.setExclusiveMinimum(true)
					.setFormat("f")
					.setItems(items().setType("g"))
					.setMaximum(1)
					.setMaxItems(2)
					.setMaxLength(3)
					.setMinimum(4)
					.setMinItems(5)
					.setMinLength(6)
					.setMultipleOf(7)
					.setPattern("h")
					.setRef("i")
					.setType("j")
					.setUniqueItems(true)
			)
			.props("collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items{type},maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,ref,type,uniqueItems")
			.vals("a,b,c,[d],e,true,true,f,{g},1,2,3,4,5,6,7,h,i,j,true")
			.json("{'$ref':'i',collectionFormat:'a','default':'b',description:'c','enum':['d'],example:'e',exclusiveMaximum:true,exclusiveMinimum:true,format:'f',items:{type:'g'},maxItems:2,maxLength:3,maximum:1,minItems:5,minLength:6,minimum:4,multipleOf:7,pattern:'h',type:'j',uniqueItems:true}")
			.string("{'$ref':'i','collectionFormat':'a','default':'b','description':'c','enum':['d'],'example':'e','exclusiveMaximum':true,'exclusiveMinimum':true,'format':'f','items':{'type':'g'},'maxItems':2,'maxLength':3,'maximum':1,'minItems':5,'minLength':6,'minimum':4,'multipleOf':7,'pattern':'h','type':'j','uniqueItems':true}".replace('\'', '"'))
		;

		@Test void a01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void a02_copy() {
			TESTER.assertCopy();
		}

		@Test void a03_toJson() {
			TESTER.assertToJson();
		}

		@Test void a04_fromJson() {
			TESTER.assertFromJson();
		}

		@Test void a05_roundTrip() {
			TESTER.assertRoundTrip();
		}

		@Test void a06_toString() {
			TESTER.assertToString();
		}

		@Test void a07_keySet() {
			assertList(TESTER.bean().keySet(), "$ref", "collectionFormat", "default", "description", "enum", "example", "exclusiveMaximum", "exclusiveMinimum", "format", "items", "maxItems", "maxLength", "maximum", "minItems", "minLength", "minimum", "multipleOf", "pattern", "type", "uniqueItems");
		}

		@Test void a08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<HeaderInfo> TESTER =
			testBean(bean())
			.props("description,type,format,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
			.json("{}")
			.string("{}")
		;

		@Test void b01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void b02_copy() {
			TESTER.assertCopy();
		}

		@Test void b03_toJson() {
			TESTER.assertToJson();
		}

		@Test void b04_fromJson() {
			TESTER.assertFromJson();
		}

		@Test void b05_roundTrip() {
			TESTER.assertRoundTrip();
		}

		@Test void b06_toString() {
			TESTER.assertToString();
		}

		@Test void b07_keySet() {
			assertEmpty(TESTER.bean().keySet());
		}
	}

	@Nested class C_extraProperties extends TestBase {

		private static final BeanTester<HeaderInfo> TESTER =
			testBean(
				bean()
					.set("collectionFormat", "a")
					.set("default", "b")
					.set("description", "c")
					.set("enum", set("d"))
					.set("example", "e")
					.set("exclusiveMaximum", true)
					.set("exclusiveMinimum", true)
					.set("format", "f")
					.set("items", items().setType("g"))
					.set("maximum", 1)
					.set("maxItems", 2)
					.set("maxLength", 3)
					.set("minimum", 4)
					.set("minItems", 5)
					.set("minLength", 6)
					.set("multipleOf", 7)
					.set("pattern", "h")
					.set("$ref", "i")
					.set("type", "j")
					.set("uniqueItems", true)
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items{type},maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,ref,type,uniqueItems,x1,x2")
			.vals("a,b,c,[d],e,true,true,f,{g},1,2,3,4,5,6,7,h,i,j,true,x1a,<null>")
			.json("{'$ref':'i',collectionFormat:'a','default':'b',description:'c','enum':['d'],example:'e',exclusiveMaximum:true,exclusiveMinimum:true,format:'f',items:{type:'g'},maxItems:2,maxLength:3,maximum:1,minItems:5,minLength:6,minimum:4,multipleOf:7,pattern:'h',type:'j',uniqueItems:true,x1:'x1a'}")
			.string("{'$ref':'i','collectionFormat':'a','default':'b','description':'c','enum':['d'],'example':'e','exclusiveMaximum':true,'exclusiveMinimum':true,'format':'f','items':{'type':'g'},'maxItems':2,'maxLength':3,'maximum':1,'minItems':5,'minLength':6,'minimum':4,'multipleOf':7,'pattern':'h','type':'j','uniqueItems':true,'x1':'x1a'}".replace('\'', '"'))
		;

		@Test void c01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void c02_copy() {
			TESTER.assertCopy();
		}

		@Test void c03_toJson() {
			TESTER.assertToJson();
		}

		@Test void c04_fromJson() {
			TESTER.assertFromJson();
		}

		@Test void c05_roundTrip() {
			TESTER.assertRoundTrip();
		}

		@Test void c06_toString() {
			TESTER.assertToString();
		}

		@Test void c07_keySet() {
			assertList(TESTER.bean().keySet(), "$ref", "collectionFormat", "default", "description", "enum", "example", "exclusiveMaximum", "exclusiveMinimum", "format", "items", "maxItems", "maxLength", "maximum", "minItems", "minLength", "minimum", "multipleOf", "pattern", "type", "uniqueItems", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items{type},maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,$ref,type,uniqueItems,x1,x2",
				"a,b,c,[d],e,true,true,f,{g},1,2,3,4,5,6,7,h,i,j,true,x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"collectionFormat,default,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items,maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,$ref,type,uniqueItems,x1,x2",
				"String,String,String,LinkedHashSet,String,Boolean,Boolean,String,Items,Integer,Integer,Integer,Integer,Integer,Integer,Integer,String,String,String,Boolean,String,<null>"
			);
		}

		@Test void c10_nullPropertyValue() {
			assertThrows(IllegalArgumentException.class, ()->bean().get(null));
			assertThrows(IllegalArgumentException.class, ()->bean().get(null, String.class));
			assertThrows(IllegalArgumentException.class, ()->bean().set(null, "a"));
		}
	}

	@Nested class D_additionalMethods extends TestBase {

		@Test void d01_addMethods() {
			var x = bean().addEnum("a1");
			assertNotNull(x);
			assertNotNull(x.getEnum());
		}

		@Test void d02_asMap() {
			assertBean(
				bean()
					.setDescription("a")
					.setType("b")
					.set("x1", "x1a")
					.asMap(),
				"description,type,x1",
				"a,b,x1a"
			);
		}

		@Test void d03_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void d04_strict() {
			var x = bean();
			assertFalse(x.isStrict());
			x.strict();
			assertTrue(x.isStrict());
		}
	}

	@Nested class E_strictMode extends TestBase {

		@Test void e01_strictModeSetThrowsException() {
			var x = bean().strict();
			assertThrowsWithMessage(RuntimeException.class, "Cannot set property 'foo' in strict mode", () -> x.set("foo", "bar"));
		}

		@Test void e02_nonStrictModeAllowsSet() {
			var x = bean(); // not strict
			assertDoesNotThrow(() -> x.set("foo", "bar"));
		}

		@Test void e03_strictModeToggle() {
			var x = bean();
			assertFalse(x.isStrict());
			x.strict();
			assertTrue(x.isStrict());
			x.strict(false);
			assertFalse(x.isStrict());
		}

		@Test void e04_strictModeSetCollectionFormat() {
			var x = bean().strict();
			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setCollectionFormat(String).  Value='invalid', valid values=['csv','ssv','tsv','pipes','multi']", () -> x.setCollectionFormat("invalid"));
			assertDoesNotThrow(() -> x.setCollectionFormat("csv"));
			assertDoesNotThrow(() -> x.setCollectionFormat("ssv"));
			assertDoesNotThrow(() -> x.setCollectionFormat("tsv"));
			assertDoesNotThrow(() -> x.setCollectionFormat("pipes"));
			assertDoesNotThrow(() -> x.setCollectionFormat("multi"));
			var y = bean(); // not strict
			assertDoesNotThrow(() -> y.setCollectionFormat("invalid"));
		}

		@Test void e05_strictModeSetType() {
			var x = bean().strict();
			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setType(String).  Value='invalid', valid values=['string','number','integer','boolean','array']", () -> x.setType("invalid"));
			assertDoesNotThrow(() -> x.setType("string"));
			assertDoesNotThrow(() -> x.setType("number"));
			assertDoesNotThrow(() -> x.setType("integer"));
			assertDoesNotThrow(() -> x.setType("boolean"));
			assertDoesNotThrow(() -> x.setType("array"));
			var y = bean(); // not strict
			assertDoesNotThrow(() -> y.setType("invalid"));
		}
	}

	@Nested class F_refs extends TestBase {

		@Test void f01_resolveRefs_basic() {
			var swagger = swagger()
				.addDefinition("MyHeader", JsonMap.of("type", "string", "description", "My Header"));

			var x = headerInfo().setRef("#/definitions/MyHeader");
			var y = x.resolveRefs(swagger, new ArrayDeque<>(), 10);
			assertBean(y, "type,description", "string,My Header");
		}

		@Test void f02_resolveRefs_withItems() {
			var swagger = swagger()
				.addDefinition("MyItem", JsonMap.of("type", "string"))
				.addDefinition("MyHeader", JsonMap.of("type", "array", "items", JsonMap.of("$ref", "#/definitions/MyItem")));

			var x = headerInfo().setRef("#/definitions/MyHeader");
			var y = x.resolveRefs(swagger, new ArrayDeque<>(), 10);
			assertBean(y, "type,items{type}", "array,{string}");
		}

		@Test void f03_resolveRefs_maxDepth() {
			var swagger = swagger()
				.addDefinition("MyItem", JsonMap.of("type", "string"))
				.addDefinition("MyHeader", JsonMap.of("type", "array", "items", JsonMap.of("$ref", "#/definitions/MyItem")));

			var x = headerInfo().setRef("#/definitions/MyHeader");
			var y = x.resolveRefs(swagger, new ArrayDeque<>(), 1);
			assertBean(y, "type,items{ref}", "array,{#/definitions/MyItem}");
		}
	}


	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static HeaderInfo bean() {
		return headerInfo();
	}
}