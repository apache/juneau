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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.openapi3.OpenApiBuilder.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SchemaInfo}.
 */
class SchemaInfo_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<SchemaInfo> TESTER =
			testBean(
				bean()
					.setDefault("a")
					.setDeprecated(true)
					.setDescription("b")
					.setEnum(l("c1", "c2"))
					.setExample("d")
					.setExclusiveMaximum(true)
					.setExclusiveMinimum(true)
					.setFormat("e")
					.setItems(items("f"))
					.setMaxItems(1)
					.setMaxLength(2)
					.setMaxProperties(3)
					.setMaximum(4)
					.setMinItems(5)
					.setMinLength(6)
					.setMinProperties(7)
					.setMinimum(8)
					.setMultipleOf(9)
					.setNullable(true)
					.setPattern("g")
					.setReadOnly(true)
					.setRef("h")
					.setRequired(l("i"))
					.setTitle("j")
					.setType("k")
					.setUniqueItems(true)
					.setWriteOnly(true)
			)
			.props("default,deprecated,description,enum,example,exclusiveMaximum,exclusiveMinimum,format,items{type},maxItems,maxLength,maxProperties,maximum,minItems,minLength,minProperties,minimum,multipleOf,nullable,pattern,readOnly,ref,required,title,type,uniqueItems,writeOnly")
			.vals("a,true,b,[c1,c2],d,true,true,e,{f},1,2,3,4,5,6,7,8,9,true,g,true,h,[i],j,k,true,true")
			.json("{'$ref':'h','default':'a',deprecated:true,description:'b','enum':['c1','c2'],example:'d',exclusiveMaximum:true,exclusiveMinimum:true,format:'e',items:{type:'f'},maxItems:1,maxLength:2,maxProperties:3,maximum:4,minItems:5,minLength:6,minProperties:7,minimum:8,multipleOf:9,nullable:true,pattern:'g',readOnly:true,required:['i'],title:'j',type:'k',uniqueItems:true,writeOnly:true}")
			.string("{'$ref':'h','default':'a','deprecated':true,'description':'b','enum':['c1','c2'],'example':'d','exclusiveMaximum':true,'exclusiveMinimum':true,'format':'e','items':{'type':'f'},'maxItems':1,'maxLength':2,'maxProperties':3,'maximum':4,'minItems':5,'minLength':6,'minProperties':7,'minimum':8,'multipleOf':9,'nullable':true,'pattern':'g','readOnly':true,'required':['i'],'title':'j','type':'k','uniqueItems':true,'writeOnly':true}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "$ref", "default", "deprecated", "description", "enum", "example", "exclusiveMaximum", "exclusiveMinimum", "format", "items", "maxItems", "maxLength", "maxProperties", "maximum", "minItems", "minLength", "minProperties", "minimum", "multipleOf", "nullable", "pattern", "readOnly", "required", "title", "type", "uniqueItems", "writeOnly");
		}

		@Test void a08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}

		@Test void a08b_getSetRef() {
			// Test get/set with "$ref" property to cover switch branches
			var x = bean();
			x.set("$ref", "#/components/schemas/MySchema");
			assertEquals("#/components/schemas/MySchema", x.get("$ref", String.class));
			assertEquals("#/components/schemas/MySchema", x.getRef());
		}

		@Test void a09_addMethods() {
			assertBean(
				bean()
					.addEnum("a1", "a2")
					.addRequired("b1", "b2")
					.addAllOf(schemaInfo("c1"), schemaInfo("c2"))
					.addAnyOf(schemaInfo("d1"), schemaInfo("d2"))
					.addOneOf(schemaInfo("e1"), schemaInfo("e2")),
				"enum,required,allOf{#{type}},anyOf{#{type}},oneOf{#{type}}",
				"[a1,a2],[b1,b2],{[{c1},{c2}]},{[{d1},{d2}]},{[{e1},{e2}]}"
			);
		}

		@Test void a10_asMap() {
			assertBean(
				bean()
					.setType("a")
					.set("x1", "x1a")
					.asMap(),
				"type,x1",
				"a,x1a"
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
		}

		@Test void a13_collectionSetters() {
			var x = bean()
				.setEnum(l("a1", "a2"))
				.setRequired(l("b1", "b2"));

			assertBean(x,
				"enum,required",
				"[a1,a2],[b1,b2]"
			);
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<SchemaInfo> TESTER =
			testBean(bean())
			.props("type,format,title,description,default,multipleOf,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,maxProperties,minProperties,required,enum,items,example,nullable,readOnly,writeOnly,deprecated,ref")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
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
		private static final BeanTester<SchemaInfo> TESTER =
			testBean(
				bean()
					.set("additionalItems", schemaInfo("a"))
					.set("additionalProperties", schemaInfo("b"))
					.set("allOf", l(schemaInfo("c1"), schemaInfo("c2")))
					.set("anyOf", l(schemaInfo("d1"), schemaInfo("d2")))
					.set("default", "e")
					.set("description", "f")
					.set("discriminator", discriminator("g"))
					.set("enum", l("h1", "h2"))
					.set("example", "i")
					.set("exclusiveMaximum", true)
					.set("exclusiveMinimum", true)
					.set("externalDocs", externalDocumentation().setUrl(URI.create("j")))
					.set("format", "k")
					.set("items", schemaInfo("l"))
					.set("maxItems", 1)
					.set("maxLength", 2)
					.set("maxProperties", 3)
					.set("maximum", 4)
					.set("minItems", 5)
					.set("minLength", 6)
					.set("minProperties", 7)
					.set("minimum", 8)
					.set("multipleOf", 9)
					.set("not", schemaInfo("m"))
					.set("nullable", true)
					.set("oneOf", l(schemaInfo("n1"), schemaInfo("n2")))
					.set("pattern", "o")
					.set("properties", m("p1", schemaInfo("p2")))
					.set("readOnly", true)
					.set("required", l("q1", "q2"))
					.set("title", "r")
					.set("type", "s")
					.set("uniqueItems", true)
					.set("writeOnly", true)
					.set("xml", xml().setName("t"))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("additionalItems{type},additionalProperties{type},allOf{#{type}},anyOf{#{type}},default,description,discriminator{propertyName},enum{#{toString}},example,exclusiveMaximum,exclusiveMinimum,externalDocs{url},format,items{type},maxItems,maxLength,maxProperties,maximum,minItems,minLength,minProperties,minimum,multipleOf,not{type},nullable,oneOf{#{type}},pattern,properties{p1{type}},readOnly,required{#{toString}},title,type,uniqueItems,writeOnly,xml{name},x1,x2")
			.vals("{a},{b},{[{c1},{c2}]},{[{d1},{d2}]},e,f,{g},{[{h1},{h2}]},i,true,true,{j},k,{l},1,2,3,4,5,6,7,8,9,{m},true,{[{n1},{n2}]},o,{{p2}},true,{[{q1},{q2}]},r,s,true,true,{t},x1a,<null>")
			.json("{additionalItems:{type:'a'},additionalProperties:{type:'b'},allOf:[{type:'c1'},{type:'c2'}],anyOf:[{type:'d1'},{type:'d2'}],'default':'e',description:'f',discriminator:{propertyName:'g'},'enum':['h1','h2'],example:'i',exclusiveMaximum:true,exclusiveMinimum:true,externalDocs:{url:'j'},format:'k',items:{type:'l'},maxItems:1,maxLength:2,maxProperties:3,maximum:4,minItems:5,minLength:6,minProperties:7,minimum:8,multipleOf:9,not:{type:'m'},nullable:true,oneOf:[{type:'n1'},{type:'n2'}],pattern:'o',properties:{p1:{type:'p2'}},readOnly:true,required:['q1','q2'],title:'r',type:'s',uniqueItems:true,writeOnly:true,x1:'x1a',xml:{name:'t'}}")
			.string("{'additionalItems':{'type':'a'},'additionalProperties':{'type':'b'},'allOf':[{'type':'c1'},{'type':'c2'}],'anyOf':[{'type':'d1'},{'type':'d2'}],'default':'e','description':'f','discriminator':{'propertyName':'g'},'enum':['h1','h2'],'example':'i','exclusiveMaximum':true,'exclusiveMinimum':true,'externalDocs':{'url':'j'},'format':'k','items':{'type':'l'},'maxItems':1,'maxLength':2,'maxProperties':3,'maximum':4,'minItems':5,'minLength':6,'minProperties':7,'minimum':8,'multipleOf':9,'not':{'type':'m'},'nullable':true,'oneOf':[{'type':'n1'},{'type':'n2'}],'pattern':'o','properties':{'p1':{'type':'p2'}},'readOnly':true,'required':['q1','q2'],'title':'r','type':'s','uniqueItems':true,'writeOnly':true,'x1':'x1a','xml':{'name':'t'}}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "additionalItems", "additionalProperties", "allOf", "anyOf", "default", "description", "discriminator", "enum", "example", "exclusiveMaximum", "exclusiveMinimum", "externalDocs", "format", "items", "maxItems", "maxLength", "maxProperties", "maximum", "minItems", "minLength", "minProperties", "minimum", "multipleOf", "not", "nullable", "oneOf", "pattern", "properties", "readOnly", "required", "title", "type", "uniqueItems", "writeOnly", "x1", "x2", "xml");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"additionalItems{type},additionalProperties{type},allOf{#{type}},anyOf{#{type}},default,description,discriminator{propertyName},enum{#{toString}},example,exclusiveMaximum,exclusiveMinimum,externalDocs{url},format,items{type},maxItems,maxLength,maxProperties,maximum,minItems,minLength,minProperties,minimum,multipleOf,not{type},nullable,oneOf{#{type}},pattern,properties{p1{type}},readOnly,required{#{toString}},title,type,uniqueItems,writeOnly,xml{name},x1,x2",
				"{a},{b},{[{c1},{c2}]},{[{d1},{d2}]},e,f,{g},{[{h1},{h2}]},i,true,true,{j},k,{l},1,2,3,4,5,6,7,8,9,{m},true,{[{n1},{n2}]},o,{{p2}},true,{[{q1},{q2}]},r,s,true,true,{t},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> scn(obj.get(prop, Object.class)),
				"additionalItems,additionalProperties,allOf,anyOf,default,description,discriminator,enum,example,exclusiveMaximum,exclusiveMinimum,externalDocs,format,items,maxItems,maxLength,maxProperties,maximum,minItems,minLength,minProperties,minimum,multipleOf,not,nullable,oneOf,pattern,properties,readOnly,required,title,type,uniqueItems,writeOnly,xml,x1,x2",
				"SchemaInfo,SchemaInfo,ArrayList,ArrayList,String,String,Discriminator,ArrayList,String,Boolean,Boolean,ExternalDocumentation,String,Items,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,SchemaInfo,Boolean,ArrayList,String,LinkedHashMap,Boolean,ArrayList,String,String,Boolean,Boolean,Xml,String,<null>"
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
			var openApi = openApi()
				.setComponents(components().setSchemas(m(
					"Pet", schemaInfo().setType("object").setTitle("Pet")
				)));
	
			assertBean(
				schemaInfo().setRef("#/components/schemas/Pet").resolveRefs(openApi, new ArrayDeque<>(), 10),
				"type,title",
				"object,Pet"
			);
		}
	
		@Test void d02_resolveRefs_nested() {
			var openApi = openApi()
				.setComponents(components().setSchemas(m(
					"Pet", schemaInfo().setType("object").setTitle("Pet"),
					"Pets", schemaInfo().setType("array").setItems(items().setRef("#/components/schemas/Pet"))
				)));
	
			assertBean(
				schemaInfo().setRef("#/components/schemas/Pets").resolveRefs(openApi, new ArrayDeque<>(), 10),
				"type,items{type,title}",
				"array,{object,Pet}"
			);
		}
	
		@Test void d03_resolveRefs_maxDepth() {
			var openApi = openApi()
				.setComponents(components().setSchemas(m(
					"Pet", schemaInfo().setType("object").setTitle("Pet"),
					"Pets", schemaInfo().setType("array").setItems(items().setRef("#/components/schemas/Pet"))
				)));
	
			assertBean(
				schemaInfo().setRef("#/components/schemas/Pets").resolveRefs(openApi, new ArrayDeque<>(), 1),
				"type,items{ref}",
				"array,{#/components/schemas/Pet}"
			);
		}
	
		@Test void d04_resolveRefs_circular() {
			var openApi = openApi()
				.setComponents(components().setSchemas(m(
					"A", schemaInfo().setType("object").setTitle("A").setProperties(m("b", schemaInfo().setRef("#/components/schemas/B"))),
					"B", schemaInfo().setType("object").setTitle("B").setProperties(m("a", schemaInfo().setRef("#/components/schemas/A")))
				)));
	
			assertBean(
				schemaInfo().setRef("#/components/schemas/A").resolveRefs(openApi, new ArrayDeque<>(), 10),
				"type,title,properties{b{type,title,properties{a{ref}}}}",
				"object,A,{{object,B,{{#/components/schemas/A}}}}"
			);
		}

		@Test void d05_resolveRefs() {
			// Test resolveRefs when both ref and allOf are null (covers the missing branch)
			var openApi = openApi()
				.setComponents(components().setSchemas(map("MySchema", schemaInfo().setType("object").setTitle("My Schema"))));

			var schema = bean()
				.setType("string")
				.setDescription("Test schema");

			var result = schema.resolveRefs(openApi, new ArrayDeque<>(), 10);

			// Should return the same object unchanged
			assertSame(schema, result);
			assertEquals("string", result.getType());
			assertEquals("Test schema", result.getDescription());
		}

		@Test void d06_resolveRefs_maxDepthDirect() {
			// Test max depth directly (covers the refStack.size() >= maxDepth branch)
			var openApi = openApi()
				.setComponents(components().setSchemas(map("MySchema", schemaInfo().setType("string"))));

			var refStack = new ArrayDeque<String>();
			refStack.add("dummy1");
			refStack.add("dummy2");
			refStack.add("dummy3");

			var schema = schemaInfo().setRef("#/components/schemas/MySchema");
			var result = schema.resolveRefs(openApi, refStack, 3);

			// Should return the original object without resolving
			assertSame(schema, result);
			assertEquals("#/components/schemas/MySchema", result.getRef());
		}

		@Test void d07_resolveRefs_additionalProperties() {
			// Test resolveRefs with additionalProperties to cover that branch
			var openApi = openApi()
				.setComponents(components().setSchemas(map(
					"MyAdditional", schemaInfo().setType("string").setDescription("Additional property schema")
				)));

			var schema = schemaInfo()
				.setType("object")
				.setAdditionalProperties(schemaInfo().setRef("#/components/schemas/MyAdditional"));
			var result = schema.resolveRefs(openApi, new ArrayDeque<>(), 10);

			// additionalProperties should have its ref resolved
			assertSame(schema, result);
			assertNotNull(result.getAdditionalProperties());
			assertEquals("string", result.getAdditionalProperties().getType());
			assertNull(result.getAdditionalProperties().getRef());
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static SchemaInfo bean() {
		return schemaInfo();
	}
}