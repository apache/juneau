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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.openapi3.OpenApiBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.common.internal.Utils;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Items}.
 */
class Items_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<Items> TESTER =
			testBean(
				bean()
					.setCollectionFormat("a")
					.setDefault("b")
					.setEnum(list("c1", "c2"))
					.setExclusiveMaximum(true)
					.setExclusiveMinimum(true)
					.setFormat("d")
					.setItems(bean().setType("e"))
					.setMaxItems(1)
					.setMaxLength(2)
					.setMaximum(3)
					.setMinItems(4)
					.setMinLength(5)
					.setMinimum(6)
					.setMultipleOf(7)
					.setPattern("f")
					.setRef("g")
					.setType("h")
					.setUniqueItems(true)
			)
			.props("collectionFormat,default,enum,exclusiveMaximum,exclusiveMinimum,format,items{type},maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,pattern,ref,type,uniqueItems")
			.vals("a,b,[c1,c2],true,true,d,{e},3,1,2,6,4,5,7,f,g,h,true")
			.json("{'$ref':'g',collectionFormat:'a','default':'b','enum':['c1','c2'],exclusiveMaximum:true,exclusiveMinimum:true,format:'d',items:{type:'e'},maxItems:1,maxLength:2,maximum:3,minItems:4,minLength:5,minimum:6,multipleOf:7,pattern:'f',type:'h',uniqueItems:true}")
			.string("{'$ref':'g','collectionFormat':'a','default':'b','enum':['c1','c2'],'exclusiveMaximum':true,'exclusiveMinimum':true,'format':'d','items':{'type':'e'},'maxItems':1,'maxLength':2,'maximum':3,'minItems':4,'minLength':5,'minimum':6,'multipleOf':7,'pattern':'f','type':'h','uniqueItems':true}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "$ref", "collectionFormat", "default", "enum", "exclusiveMaximum", "exclusiveMinimum", "format", "items", "maxItems", "maxLength", "maximum", "minItems", "minLength", "minimum", "multipleOf", "pattern", "type", "uniqueItems");
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Items> TESTER =
			testBean(bean())
			.props("type,format,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,ref")
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
		private static final BeanTester<Items> TESTER =
			testBean(
				bean()
					.set("additionalItems", schemaInfo("a"))
					.set("allOf", list(schemaInfo("b1"), schemaInfo("b2")))
					.set("collectionFormat", "c")
					.set("default", "d")
					.set("discriminator", "e")
					.set("enum", list("f1", "f2"))
					.set("example", "g")
					.set("exclusiveMaximum", true)
					.set("exclusiveMinimum", true)
					.set("externalDocs", externalDocumentation().setUrl(URI.create("h")))
					.set("format", "i")
					.set("items", bean().setType("j"))
					.set("maxItems", 1)
					.set("maxLength", 2)
					.set("maxProperties", 3)
					.set("maximum", 4)
					.set("minItems", 5)
					.set("minLength", 6)
					.set("minProperties", 7)
					.set("minimum", 8)
					.set("multipleOf", 9)
					.set("pattern", "k")
					.set("properties", map("l1", schemaInfo("l2")))
					.set("readOnly", true)
					.set("required", list("m1", "m2"))
					.set("title", "n")
					.set("type", "o")
					.set("uniqueItems", true)
					.set("xml", xml().setName("p"))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("additionalItems{type},allOf{#{type}},collectionFormat,default,discriminator,enum{#{toString}},example,exclusiveMaximum,exclusiveMinimum,externalDocs{url},format,items{type},maxItems,maxLength,maxProperties,maximum,minItems,minLength,minProperties,minimum,multipleOf,pattern,properties{l1{type}},readOnly,required{#{toString}},title,type,uniqueItems,xml{name},x1,x2")
			.vals("{a},{[{b1},{b2}]},c,d,e,{[{f1},{f2}]},g,true,true,{h},i,{j},1,2,3,4,5,6,7,8,9,k,{{l2}},true,{[{m1},{m2}]},n,o,true,{p},x1a,<null>")
			.json("{additionalItems:{type:'a'},allOf:[{type:'b1'},{type:'b2'}],collectionFormat:'c','default':'d',discriminator:'e','enum':['f1','f2'],example:'g',exclusiveMaximum:true,exclusiveMinimum:true,externalDocs:{url:'h'},format:'i',items:{type:'j'},maxItems:1,maxLength:2,maxProperties:3,maximum:4,minItems:5,minLength:6,minProperties:7,minimum:8,multipleOf:9,pattern:'k',properties:{l1:{type:'l2'}},readOnly:true,required:['m1','m2'],title:'n',type:'o',uniqueItems:true,x1:'x1a',xml:{name:'p'}}")
			.string("{'additionalItems':{'type':'a'},'allOf':[{'type':'b1'},{'type':'b2'}],'collectionFormat':'c','default':'d','discriminator':'e','enum':['f1','f2'],'example':'g','exclusiveMaximum':true,'exclusiveMinimum':true,'externalDocs':{'url':'h'},'format':'i','items':{'type':'j'},'maxItems':1,'maxLength':2,'maxProperties':3,'maximum':4,'minItems':5,'minLength':6,'minProperties':7,'minimum':8,'multipleOf':9,'pattern':'k','properties':{'l1':{'type':'l2'}},'readOnly':true,'required':['m1','m2'],'title':'n','type':'o','uniqueItems':true,'x1':'x1a','xml':{'name':'p'}}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "additionalItems", "allOf", "collectionFormat", "default", "discriminator", "enum", "example", "exclusiveMaximum", "exclusiveMinimum", "externalDocs", "format", "items", "maxItems", "maxLength", "maxProperties", "maximum", "minItems", "minLength", "minProperties", "minimum", "multipleOf", "pattern", "properties", "readOnly", "required", "title", "type", "uniqueItems", "x1", "x2", "xml");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"additionalItems{type},allOf{#{type}},collectionFormat,default,discriminator,enum{#{toString}},example,exclusiveMaximum,exclusiveMinimum,externalDocs{url},format,items{type},maxItems,maxLength,maxProperties,maximum,minItems,minLength,minProperties,minimum,multipleOf,pattern,properties{l1{type}},readOnly,required{#{toString}},title,type,uniqueItems,xml{name},x1,x2",
				"{a},{[{b1},{b2}]},c,d,e,{[{f1},{f2}]},g,true,true,{h},i,{j},1,2,3,4,5,6,7,8,9,k,{{l2}},true,{[{m1},{m2}]},n,o,true,{p},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"additionalItems,allOf,collectionFormat,default,discriminator,enum,example,exclusiveMaximum,exclusiveMinimum,externalDocs,format,items,maxItems,maxLength,maxProperties,maximum,minItems,minLength,minProperties,minimum,multipleOf,pattern,properties,readOnly,required,title,type,uniqueItems,xml,x1,x2",
				"SchemaInfo,ArrayList,String,String,String,ArrayList,String,Boolean,Boolean,ExternalDocumentation,String,Items,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,String,LinkedHashMap,Boolean,ArrayList,String,String,Boolean,Xml,String,<null>"
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
			assertBean(
				bean()
					.addEnum("a1", "a2"),
				"enum",
				"[a1,a2]"
			);
		}

		@Test void d02_asMap() {
			assertBean(
				bean()
					.setType("a")
					.set("x1", "x1a")
					.asMap(),
				"type,x1",
				"a,x1a"
			);
		}

		@Test void d03_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void d04_getItemsProperty() {
			var x = bean().setItems(bean().setType("a"));
			assertBean(x.get("items", Items.class), "type", "a");
		}

		@Test void d05_resolveRefsWithRef() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string")
				)));

			var x = items().setRef("#/components/schemas/MyItem");
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 10);
			assertBean(y, "type", "string");
		}

		@Test void d06_resolveRefsWithRefStackContains() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string")
				)));

			var refStack = new ArrayDeque<String>();
			refStack.add("#/components/schemas/MyItem");

			var x = items().setRef("#/components/schemas/MyItem");
			var y = x.resolveRefs(openApi, refStack, 10);
			assertBean(y, "ref", "#/components/schemas/MyItem");
		}

		@Test void d07_resolveRefsWithMaxDepth() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string")
				)));

			var x = items().setRef("#/components/schemas/MyItem");
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 0);
			assertBean(y, "ref", "#/components/schemas/MyItem");
		}

		@Test void d08_resolveRefsWithProperties() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Utils.map(
					"MyItem", schemaInfo().setType("string")
				)));

			var x = items()
				.set("properties", JsonMap.of("prop1", JsonMap.of("$ref", "#/components/schemas/MyItem")));
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 10);
			assertBean(y, "properties{prop1{type}}", "{{string}}");
		}

		@Test void d09_resolveRefsWithItems() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string")
				)));

			var x = items().setItems(items().setRef("#/components/schemas/MyItem"));
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 10);
			assertBean(y, "items{type}", "{string}");
		}

		@Test void d10_resolveRefsSetsExampleNull() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string")
				)));

			var x = items().set("example", "test");
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 10);
			assertBean(y, "example", "<null>");
		}

		@Test void d11_resolveRefsWithoutRef() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string")
				)));

			var x = items().setType("string");
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 10);
			assertBean(y, "type,example", "string,<null>");
		}

		@Test void d12_resolveRefsWithNullItems() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string")
				)));

			var x = items().setType("string"); // items is null
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 10);
			assertBean(y, "type,items,example", "string,<null>,<null>");
		}

		@Test void d13_resolveRefsWithNullProperties() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string")
				)));

			var x = items().setType("string"); // no properties set
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 10);
			assertBean(y, "type,example", "string,<null>");
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

		@Test void e04_strictModeSetType() {
			var x = bean().strict();
			assertThrowsWithMessage(IllegalArgumentException.class, "Invalid value passed in to setType(String).  Value='invalid', valid values=['string','number','integer','boolean','array']", () -> x.setType("invalid"));
			assertDoesNotThrow(() -> x.setType("string"));
			assertDoesNotThrow(() -> x.setType("number"));
			assertDoesNotThrow(() -> x.setType("integer"));
			assertDoesNotThrow(() -> x.setType("boolean"));
			assertDoesNotThrow(() -> x.setType("array"));
			var y = bean(); // not strict
			assertDoesNotThrow(() -> y.setType("invalid"));
		}

		@Test void e05_strictModeSetCollectionFormat() {
			var x = bean().strict();
			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setCollectionFormat(String).  Value='invalid', valid values=[csv, ssv, tsv, pipes, multi]", () -> x.setCollectionFormat("invalid"));
			assertDoesNotThrow(() -> x.setCollectionFormat("csv"));
			assertDoesNotThrow(() -> x.setCollectionFormat("ssv"));
			assertDoesNotThrow(() -> x.setCollectionFormat("tsv"));
			assertDoesNotThrow(() -> x.setCollectionFormat("pipes"));
			assertDoesNotThrow(() -> x.setCollectionFormat("multi"));
			var y = bean(); // not strict
			assertDoesNotThrow(() -> y.setCollectionFormat("invalid"));
		}
	}

	@Nested class F_refs extends TestBase {

		@Test void f01_resolveRefs_basic() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string")
				)));

			var x = items().setRef("#/components/schemas/MyItem");
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 10);
			assertBean(y, "type", "string");
		}

		@Test void f02_resolveRefs_nestedItems() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string"),
					"MyArray", schemaInfo().setType("array").setItems(items().setRef("#/components/schemas/MyItem"))
				)));

			var x = items().setRef("#/components/schemas/MyArray");
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 10);
			assertBean(y, "type,items{type}", "array,{string}");
		}

		@Test void f03_resolveRefs_maxDepth() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyItem", schemaInfo().setType("string"),
					"MyArray", schemaInfo().setType("array").setItems(items().setRef("#/components/schemas/MyItem"))
				)));

			var x = items().setRef("#/components/schemas/MyArray");
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 1);
			assertBean(y, "type,items{ref}", "array,{#/components/schemas/MyItem}");
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static Items bean() {
		return items();
	}
}
