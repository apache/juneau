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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link ParameterInfo}.
 */
class ParameterInfo_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<ParameterInfo> TESTER =
			testBean(
				bean()
					.setAllowEmptyValue(true)
					.setCollectionFormat("a")
					.setDefault("b")
					.setDescription("c")
					.setEnum(set("d"))
					.setExample("e")
					.setExamples(map("f", "g"))
					.setExclusiveMaximum(true)
					.setExclusiveMinimum(true)
					.setFormat("h")
					.setIn("i")
					.setItems(items().setType("j"))
					.setMaximum(1)
					.setMaxItems(2)
					.setMaxLength(3)
					.setMinimum(4)
					.setMinItems(5)
					.setMinLength(6)
					.setMultipleOf(7)
					.setName("k")
					.setPattern("l")
					.setRequired(true)
					.setSchema(schemaInfo().setType("m"))
					.setType("n")
					.setUniqueItems(true)
			)
			.props("allowEmptyValue,collectionFormat,default,description,enum,example,examples,exclusiveMaximum,exclusiveMinimum,format,in,items{type},maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,name,pattern,required,schema{type},type,uniqueItems")
			.vals("true,a,b,c,[d],e,{f=g},true,true,h,i,{j},1,2,3,4,5,6,7,k,l,true,{m},n,true")
			.json("{allowEmptyValue:true,collectionFormat:'a','default':'b',description:'c','enum':['d'],example:'e',examples:{f:'g'},exclusiveMaximum:true,exclusiveMinimum:true,format:'h','in':'i',items:{type:'j'},maxItems:2,maxLength:3,maximum:1,minItems:5,minLength:6,minimum:4,multipleOf:7,name:'k',pattern:'l',required:true,schema:{type:'m'},type:'n',uniqueItems:true}")
			.string("{'allowEmptyValue':true,'collectionFormat':'a','default':'b','description':'c','enum':['d'],'example':'e','examples':{'f':'g'},'exclusiveMaximum':true,'exclusiveMinimum':true,'format':'h','in':'i','items':{'type':'j'},'maxItems':2,'maxLength':3,'maximum':1,'minItems':5,'minLength':6,'minimum':4,'multipleOf':7,'name':'k','pattern':'l','required':true,'schema':{'type':'m'},'type':'n','uniqueItems':true}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "allowEmptyValue", "collectionFormat", "default", "description", "enum", "example", "examples", "exclusiveMaximum", "exclusiveMinimum", "format", "in", "items", "maxItems", "maxLength", "maximum", "minItems", "minLength", "minimum", "multipleOf", "name", "pattern", "required", "schema", "type", "uniqueItems");
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

		@Test void a10_setEnumVarargs() {
			var x = bean().setEnum("a", "b", "c");
			assertBean(x, "enum", "[a,b,c]");
		}

		@Test void a11_setEnumVarargsEmpty() {
			var x = bean().setEnum();
			assertBean(x, "enum", "<null>");
		}

		@Test void a12_setEnumVarargsNull() {
			var x = bean().setEnum((Object[])null);
			assertBean(x, "enum", "<null>");
		}

		@Test void a13_asMap() {
			assertBean(
				bean()
					.setDescription("a")
					.setIn("b")
					.setName("c")
					.set("x1", "x1a")
					.asMap(),
				"description,in,name,x1",
				"a,b,c,x1a"
			);
		}

		@Test void a14_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void a15_copyFrom() {
			var x = bean().setName("a").setIn("b");
			var y = bean().copyFrom(x);
			assertBean(y, "name,in", "a,b");

			y = bean().setName("c").copyFrom(x);
			assertBean(y, "name,in", "a,b");

			y = bean().setName("c").copyFrom(null);
			assertBean(y, "name", "c");
		}

		@Test void a16_setInPathSetsRequired() {
			var x = bean().setIn("path");
			assertBean(x, "in,required", "path,true");
		}

		@Test void a17_setInNonPathDoesNotSetRequired() {
			var x = bean().setIn("query");
			assertBean(x, "in,required", "query,<null>");
		}

		@Test void a18_setNameWithBodyInDoesNotSetName() {
			var x = bean().setIn("body").setName("test");
			assertBean(x, "in,name", "body,<null>");
		}

		@Test void a19_setNameWithNonBodyInSetsName() {
			var x = bean().setIn("query").setName("test");
			assertBean(x, "in,name", "query,test");
		}

		@Test void a20_copyFromAllProperties() {
			var source = bean()
				.setName("a")
				.setIn("b")
				.setDescription("c")
				.setType("d")
				.setFormat("e")
				.setPattern("f")
				.setCollectionFormat("g")
				.setMaximum(1)
				.setMinimum(2)
				.setMultipleOf(3)
				.setMaxLength(4)
				.setMinLength(5)
				.setMaxItems(6)
				.setMinItems(7)
				.setRequired(true)
				.setAllowEmptyValue(true)
				.setExclusiveMaximum(true)
				.setExclusiveMinimum(true)
				.setUniqueItems(true)
				.setSchema(schemaInfo().setType("h"))
				.setItems(items().setType("i"))
				.setDefault("j")
				.setEnum(set("k"))
				.setExample("l")
				.setExamples(map("m1","m2"));

			var target = bean().copyFrom(source);
			assertBean(target,
				"name,in,description,type,format,pattern,collectionFormat,maximum,minimum,multipleOf,maxLength,minLength,maxItems,minItems,required,allowEmptyValue,exclusiveMaximum,exclusiveMinimum,uniqueItems,schema{type},items{type},default,enum,example,examples",
				"a,b,c,d,e,f,g,1,2,3,4,5,6,7,true,true,true,true,true,{h},{i},j,[k],l,{m1=m2}"
			);
		}

		@Test void a21_copyFromPartialProperties() {
			var source = bean()
				.setName("a")
				.setIn("b")
				.setDescription("c");
			// Leave other properties null

			var target = bean()
				.setType("d")
				.setFormat("e")
				.copyFrom(source);

			assertBean(
				target,
				"name,in,description,type,format",
				"a,b,c,d,e"
			);

			// Entirely empty.
			assertJson(
				"{}",
				bean().copyFrom(bean())
			);
		}

		@Test void a22_strictMode() {
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

			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setIn(String).  Value='invalid', valid values=['query','header','path','formData','body']", () -> x.setIn("invalid"));
			assertDoesNotThrow(() -> x.setIn("query"));
			assertDoesNotThrow(() -> x.setIn("header"));
			assertDoesNotThrow(() -> x.setIn("path"));
			assertDoesNotThrow(() -> x.setIn("formData"));
			assertDoesNotThrow(() -> x.setIn("body"));
			assertDoesNotThrow(() -> y.setIn("invalid"));

			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setType(String).  Value='invalid', valid values=['string','number','integer','boolean','array','file']", () -> x.setType("invalid"));
			assertDoesNotThrow(() -> x.setType("string"));
			assertDoesNotThrow(() -> x.setType("number"));
			assertDoesNotThrow(() -> x.setType("integer"));
			assertDoesNotThrow(() -> x.setType("boolean"));
			assertDoesNotThrow(() -> x.setType("array"));
			assertDoesNotThrow(() -> x.setType("file"));
			assertDoesNotThrow(() -> y.setType("invalid"));

			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setCollectionFormat(String).  Value='null', valid values=['csv','ssv','tsv','pipes','multi']", () -> x.setCollectionFormat(null));

			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setIn(String).  Value='null', valid values=['query','header','path','formData','body']", () -> x.setIn(null));

			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setType(String).  Value='null', valid values=['string','number','integer','boolean','array','file']", () -> x.setType(null));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<ParameterInfo> TESTER =
			testBean(bean())
			.props("description,in,name,required,schema,type,format,allowEmptyValue,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
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

		private static final BeanTester<ParameterInfo> TESTER =
			testBean(
				bean()
					.set("allowEmptyValue", true)
					.set("collectionFormat", "a")
					.set("default", "b")
					.set("description", "c")
					.set("enum", set("d"))
					.set("example", "e")
					.set("examples", map("f", "g"))
					.set("exclusiveMaximum", true)
					.set("exclusiveMinimum", true)
					.set("format", "h")
					.set("in", "f")
					.set("items", items().setType("g"))
					.set("maximum", 1)
					.set("maxItems", 2)
					.set("maxLength", 3)
					.set("minimum", 4)
					.set("minItems", 5)
					.set("minLength", 6)
					.set("multipleOf", 7)
					.set("name", "h")
					.set("pattern", "i")
					.set("required", true)
					.set("schema", schemaInfo().setType("j"))
					.set("type", "k")
					.set("uniqueItems", true)
					.set("x3", "x3a")
					.set("x4", null)
			)
			.props("allowEmptyValue,collectionFormat,default,description,enum,example,examples,exclusiveMaximum,exclusiveMinimum,format,in,items{type},maximum,maxItems,maxLength,minimum,minItems,minLength,multipleOf,name,pattern,required,schema{type},type,uniqueItems,x3,x4")
			.vals("true,a,b,c,[d],e,{f=g},true,true,h,f,{g},1,2,3,4,5,6,7,h,i,true,{j},k,true,x3a,<null>")
			.json("{allowEmptyValue:true,collectionFormat:'a','default':'b',description:'c','enum':['d'],example:'e',examples:{f:'g'},exclusiveMaximum:true,exclusiveMinimum:true,format:'h','in':'f',items:{type:'g'},maxItems:2,maxLength:3,maximum:1,minItems:5,minLength:6,minimum:4,multipleOf:7,name:'h',pattern:'i',required:true,schema:{type:'j'},type:'k',uniqueItems:true,x3:'x3a'}")
			.string("{'allowEmptyValue':true,'collectionFormat':'a','default':'b','description':'c','enum':['d'],'example':'e','examples':{'f':'g'},'exclusiveMaximum':true,'exclusiveMinimum':true,'format':'h','in':'f','items':{'type':'g'},'maxItems':2,'maxLength':3,'maximum':1,'minItems':5,'minLength':6,'minimum':4,'multipleOf':7,'name':'h','pattern':'i','required':true,'schema':{'type':'j'},'type':'k','uniqueItems':true,'x3':'x3a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "allowEmptyValue", "collectionFormat", "default", "description", "enum", "example", "examples", "exclusiveMaximum", "exclusiveMinimum", "format", "in", "items", "maxItems", "maxLength", "maximum", "minItems", "minLength", "minimum", "multipleOf", "name", "pattern", "required", "schema", "type", "uniqueItems", "x3", "x4");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"allowEmptyValue,collectionFormat,default,description,enum,example,examples,exclusiveMaximum,exclusiveMinimum,format,in,items{type},maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,name,pattern,required,schema{type},type,uniqueItems,x3,x4",
				"true,a,b,c,[d],e,{f=g},true,true,h,f,{g},2,3,1,5,6,4,7,h,i,true,{j},k,true,x3a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> scn(obj.get(prop, Object.class)),
				"allowEmptyValue,collectionFormat,default,description,enum,example,examples,exclusiveMaximum,exclusiveMinimum,format,in,items,maxItems,maxLength,maximum,minItems,minLength,minimum,multipleOf,name,pattern,required,schema,type,uniqueItems,x3,x4",
				"Boolean,String,String,String,LinkedHashSet,String,LinkedHashMap,Boolean,Boolean,String,String,Items,Integer,Integer,Integer,Integer,Integer,Integer,Integer,String,String,Boolean,SchemaInfo,String,Boolean,String,<null>"
			);
		}

		@Test void c10_nullPropertyValue() {
			assertThrows(IllegalArgumentException.class, ()->bean().get(null));
			assertThrows(IllegalArgumentException.class, ()->bean().get(null, String.class));
			assertThrows(IllegalArgumentException.class, ()->bean().set(null, "a"));
		}
	}

	@Nested class D_refs extends TestBase {

		@Test void d01_resolveRefs_schema() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"));

			assertBean(
				parameterInfo().setName("pet").setIn("body").setSchema(schemaInfo().setRef("#/definitions/Pet")).resolveRefs(swagger, new ArrayDeque<>(), 10),
				"name,in,schema{type,title}",
				"pet,body,{object,Pet}"
			);
		}

		@Test void d02_resolveRefs_items() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"));

			assertBean(
				parameterInfo().setName("pets").setIn("query").setType("array").setItems(items().setRef("#/definitions/Pet")).resolveRefs(swagger, new ArrayDeque<>(), 10),
				"name,in,type,items{type,title}",
				"pets,query,array,{object,Pet}"
			);
		}

		@Test void d03_resolveRefs_maxDepth() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"));

			assertBean(
				parameterInfo().setName("pet").setIn("body").setSchema(schemaInfo().setRef("#/definitions/Pet")).resolveRefs(swagger, new ArrayDeque<>(), 0),
				"name,in,schema{ref}",
				"pet,body,{#/definitions/Pet}"
			);
		}

		@Test void d04_resolveRefs_nullSchema() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"));

			assertBean(
				parameterInfo().setName("pet").setIn("body").resolveRefs(swagger, new ArrayDeque<>(), 10),
				"name,in,schema",
				"pet,body,<null>"
			);
		}

		@Test void d05_resolveRefs_nullItems() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"));

			assertBean(
				parameterInfo().setName("pets").setIn("query").setType("array").resolveRefs(swagger, new ArrayDeque<>(), 10),
				"name,in,type,items",
				"pets,query,array,<null>"
			);
		}

		@Test void d06_resolveRefs_bothNull() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"));

			assertBean(
				parameterInfo().setName("test").setIn("query").resolveRefs(swagger, new ArrayDeque<>(), 10),
				"name,in,schema,items",
				"test,query,<null>,<null>"
			);
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static ParameterInfo bean() {
		return parameterInfo();
	}
}