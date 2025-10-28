/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
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
					.setEnum("d")
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

		@Test void a09_addMethods() {
			var x = bean().addEnum("a1");
			assertNotNull(x);
			assertNotNull(x.getEnum());
		}

		@Test void a10_asMap() {
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

		@Test void a11_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void a12_strictMode() {
			assertThrows(RuntimeException.class, () -> bean().strict().set("foo", "bar"));
			assertDoesNotThrow(() -> bean().set("foo", "bar"));

			assertFalse(bean().isStrict());
			assertTrue(bean().strict().isStrict());
			assertFalse(bean().strict(false).isStrict());

			var x = bean().strict();
			var y = bean(); // not strict

			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setCollectionFormat(String).  Value='invalid', valid values=['csv','ssv','tsv','pipes','multi']", () -> x.setCollectionFormat("invalid"));
			assertDoesNotThrow(() -> x.setCollectionFormat("csv"));
			assertDoesNotThrow(() -> x.setCollectionFormat("ssv"));
			assertDoesNotThrow(() -> x.setCollectionFormat("tsv"));
			assertDoesNotThrow(() -> x.setCollectionFormat("pipes"));
			assertDoesNotThrow(() -> x.setCollectionFormat("multi"));
			assertDoesNotThrow(() -> y.setCollectionFormat("invalid"));

			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setType(String).  Value='invalid', valid values=['string','number','integer','boolean','array']", () -> x.setType("invalid"));
			assertDoesNotThrow(() -> x.setType("string"));
			assertDoesNotThrow(() -> x.setType("number"));
			assertDoesNotThrow(() -> x.setType("integer"));
			assertDoesNotThrow(() -> x.setType("boolean"));
			assertDoesNotThrow(() -> x.setType("array"));
			assertDoesNotThrow(() -> y.setType("invalid"));
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
				TESTER.bean(), (obj,prop) -> scn(obj.get(prop, Object.class)),
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

	@Nested class D_refs extends TestBase {

		@Test void d01_resolveRefs_basic() {
			var swagger = swagger()
				.addDefinition("MyHeader", JsonMap.of("type", "string", "description", "My Header"));

			assertBean(
				headerInfo().setRef("#/definitions/MyHeader").resolveRefs(swagger, new ArrayDeque<>(), 10),
				"type,description",
				"string,My Header"
			);
		}

		@Test void d02_resolveRefs_withItems() {
			var swagger = swagger()
				.addDefinition("MyItem", JsonMap.of("type", "string"))
				.addDefinition("MyHeader", JsonMap.of("type", "array", "items", JsonMap.of("$ref", "#/definitions/MyItem")));

			assertBean(
				headerInfo().setRef("#/definitions/MyHeader").resolveRefs(swagger, new ArrayDeque<>(), 10),
				"type,items{type}",
				"array,{string}"
			);
		}

		@Test void d03_resolveRefs_maxDepth() {
			var swagger = swagger()
				.addDefinition("MyItem", JsonMap.of("type", "string"))
				.addDefinition("MyHeader", JsonMap.of("type", "array", "items", JsonMap.of("$ref", "#/definitions/MyItem")));

			assertBean(headerInfo().setRef("#/definitions/MyHeader").resolveRefs(swagger, new ArrayDeque<>(), 1),
				"type,items{ref}",
				"array,{#/definitions/MyItem}"
			);
		}

		@Test void d04_resolveRefs_noRefNoItems() {
			// Test resolveRefs when both ref and items are null (covers the missing branch)
			var swagger = swagger();
			var header = headerInfo()
				.setType("string")
				.setDescription("Test header");

			var result = header.resolveRefs(swagger, new ArrayDeque<>(), 10);

			// Should return the same object unchanged
			assertSame(header, result);
			assertEquals("string", result.getType());
			assertEquals("Test header", result.getDescription());
		}

		@Test void d05_resolveRefs_noRefWithItems() {
			// Test resolveRefs when ref is null but items is not null (covers the missing branch)
			var swagger = swagger()
				.addDefinition("MyItem", JsonMap.of("type", "string"));

			var header = headerInfo()
				.setType("array")
				.setItems(items().setRef("#/definitions/MyItem"));

			var result = header.resolveRefs(swagger, new ArrayDeque<>(), 10);

			// Should return the same object with resolved items
			assertSame(header, result);
			assertEquals("array", result.getType());
			assertNotNull(result.getItems());
			assertEquals("string", result.getItems().getType());
			assertNull(result.getItems().getRef()); // ref should be resolved
		}

		@Test void d06_resolveRefs_circularReference() {
			// Test circular reference detection (covers the refStack.contains(ref) branch)
			var swagger = swagger()
				.addDefinition("Header1", JsonMap.of("$ref", "#/definitions/Header2"))
				.addDefinition("Header2", JsonMap.of("$ref", "#/definitions/Header1"));

			var refStack = new ArrayDeque<String>();
			refStack.add("#/definitions/Header1"); // Pre-populate the stack to simulate circular reference

			var header = headerInfo().setRef("#/definitions/Header1");
			var result = header.resolveRefs(swagger, refStack, 10);

			// Should return the original object without resolving (circular reference detected)
			assertSame(header, result);
			assertEquals("#/definitions/Header1", result.getRef());
		}

		@Test void d07_resolveRefs_maxDepthDirect() {
			// Test max depth directly (covers the refStack.size() >= maxDepth branch directly)
			var swagger = swagger()
				.addDefinition("MyHeader", JsonMap.of("type", "string"));

			// Create a refStack that's already at max depth
			var refStack = new ArrayDeque<String>();
			refStack.add("dummy1");
			refStack.add("dummy2");
			refStack.add("dummy3");

			var header = headerInfo().setRef("#/definitions/MyHeader");
			var result = header.resolveRefs(swagger, refStack, 3); // maxDepth = 3, refStack.size() = 3

			// Should return the original object without resolving (max depth reached)
			assertSame(header, result);
			assertEquals("#/definitions/MyHeader", result.getRef());
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static HeaderInfo bean() {
		return headerInfo();
	}
}