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

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SchemaInfo}.
 */
class SchemaInfo_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<SchemaInfo> TESTER =
			testBean(
				bean()
					.setAdditionalProperties(schemaInfo().setType("a"))
					.setAllOf(set(schemaInfo().setType("b")))
					.setDefault("c")
					.setDescription("d")
					.setDiscriminator("e")
					.setEnum(set("f"))
					.setExample("g")
					.setExclusiveMaximum(true)
					.setExclusiveMinimum(true)
					.setExternalDocs(externalDocumentation().setUrl(URI.create("h")))
					.setFormat("i")
					.setItems(items().setType("j"))
					.setMaximum(1)
					.setMaxItems(2)
					.setMaxLength(3)
					.setMaxProperties(4)
					.setMinimum(5)
					.setMinItems(6)
					.setMinLength(7)
					.setMinProperties(8)
					.setMultipleOf(9)
					.setPattern("k")
					.setProperties(map("x1", schemaInfo().setType("x2")))
					.setReadOnly(true)
					.setRef("l")
					.setRequired(true)
					.setRequiredProperties("m")
					.setTitle("n")
					.setType("o")
					.setUniqueItems(true)
					.setXml(xml().setName("p"))
			)
			.props("additionalProperties{type},allOf{#{type}},default,description,discriminator,enum,example,exclusiveMaximum,exclusiveMinimum,externalDocs{url},format,items{type},maximum,maxItems,maxLength,maxProperties,minimum,minItems,minLength,minProperties,multipleOf,pattern,properties{x1{type}},readOnly,ref,required,requiredProperties,title,type,uniqueItems,xml{name}")
			.vals("{a},{[{b}]},c,d,e,[f],g,true,true,{h},i,{j},1,2,3,4,5,6,7,8,9,k,{{x2}},true,l,true,[m],n,o,true,{p}")
			.json("{'$ref':'l',additionalProperties:{type:'a'},allOf:[{type:'b'}],'default':'c',description:'d',discriminator:'e','enum':['f'],example:'g',exclusiveMaximum:true,exclusiveMinimum:true,externalDocs:{url:'h'},format:'i',items:{type:'j'},maxItems:2,maxLength:3,maxProperties:4,maximum:1,minItems:6,minLength:7,minProperties:8,minimum:5,multipleOf:9,pattern:'k',properties:{x1:{type:'x2'}},readOnly:true,required:true,requiredProperties:['m'],title:'n',type:'o',uniqueItems:true,xml:{name:'p'}}")
			.string("{'$ref':'l','additionalProperties':{'type':'a'},'allOf':[{'type':'b'}],'default':'c','description':'d','discriminator':'e','enum':['f'],'example':'g','exclusiveMaximum':true,'exclusiveMinimum':true,'externalDocs':{'url':'h'},'format':'i','items':{'type':'j'},'maxItems':2,'maxLength':3,'maxProperties':4,'maximum':1,'minItems':6,'minLength':7,'minProperties':8,'minimum':5,'multipleOf':9,'pattern':'k','properties':{'x1':{'type':'x2'}},'readOnly':true,'required':true,'requiredProperties':['m'],'title':'n','type':'o','uniqueItems':true,'xml':{'name':'p'}}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "$ref", "additionalProperties", "allOf", "default", "description", "discriminator", "enum", "example", "exclusiveMaximum", "exclusiveMinimum", "externalDocs", "format", "items", "maxItems", "maxLength", "maxProperties", "maximum", "minItems", "minLength", "minProperties", "minimum", "multipleOf", "pattern", "properties", "readOnly", "required", "requiredProperties", "title", "type", "uniqueItems", "xml");
		}

		@Test void a08_addMethods() {
			var x = bean()
				.addEnum("a1")
				.setAllOf(schemaInfo().setTitle("a1"))
				.addAllOf(schemaInfo().setTitle("a2"))
			;

			assertNotNull(x);
			assertNotNull(x.getEnum());
			assertNotNull(x.getAllOf());
		}

		@Test void a09_asMap() {
			assertBean(
				bean()
					.setDescription("a")
					.setTitle("b")
					.setType("c")
					.set("x1", "x1a")
					.asMap(),
				"description,title,type,x1",
				"a,b,c,x1a"
			);
		}

		@Test void a10_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void a11_strictMode() {
			assertThrows(RuntimeException.class, () -> bean().strict().set("foo", "bar"));
			assertDoesNotThrow(() -> bean().set("foo", "bar"));

			assertFalse(bean().isStrict());
			assertTrue(bean().strict().isStrict());
			assertFalse(bean().strict(false).isStrict());
		}

		@Test void a12_collectionSetters() {
			var x = bean()
				.setAllOf(list(schemaInfo().setType("a"), schemaInfo().setType("b")))
				.setEnum(list("c", "d"))
				.setRequiredProperties(list("e", "f"));

			assertBean(x,
				"allOf{#{type}},enum,requiredProperties",
				"{[{a},{b}]},[c,d],[e,f]"
			);
		}

		@Test void a13_varargAdders() {
			var x = bean()
				.addAllOf(schemaInfo().setType("a1"))
				.addAllOf(schemaInfo().setType("a2"))
				.addEnum("b1")
				.addEnum("b2")
				.addRequiredProperties("c1")
				.addRequiredProperties("c2");

			assertBean(x,
				"allOf{#{type}},enum,requiredProperties",
				"{[{a1},{a2}]},[b1,b2],[c1,c2]"
			);
		}

		@Test void a14_collectionAdders() {
			// Test that Collection versions of addX methods exist
			// Due to Java method resolution preferring varargs over Collection,
			// we test the basic functionality with varargs versions
			var x = bean()
				.addAllOf(list(schemaInfo().setType("a1")))
				.addAllOf(list(schemaInfo().setType("a2")))
				.addEnum(list("b1"))
				.addEnum(list("b2"))
				.addRequiredProperties(list("c1"))
				.addRequiredProperties(list("c2"));

			assertBean(x,
				"allOf{#{type}},enum,requiredProperties",
				"{[{a1},{a2}]},[b1,b2],[c1,c2]"
			);
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<SchemaInfo> TESTER =
			testBean(bean())
			.props("description,title,type,format,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,discriminator,readOnly,xml,externalDocs,example,required,properties,additionalProperties,allOf")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
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

		@Test void b08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}
	}

	@Nested class C_extraProperties extends TestBase {

		private static final BeanTester<SchemaInfo> TESTER =
			testBean(
				bean()
					.set("additionalProperties", schemaInfo().setType("a"))
					.set("allOf", set(schemaInfo().setType("b")))
					.set("default", "c")
					.set("description", "d")
					.set("discriminator", "e")
					.set("enum", set("f"))
					.set("example", "g")
					.set("exclusiveMaximum", true)
					.set("exclusiveMinimum", true)
					.set("externalDocs", externalDocumentation().setUrl(URI.create("h")))
					.set("format", "i")
					.set("items", items().setType("j"))
					.set("maximum", 1)
					.set("maxItems", 2)
					.set("maxLength", 3)
					.set("maxProperties", 4)
					.set("minimum", 5)
					.set("minItems", 6)
					.set("minLength", 7)
					.set("minProperties", 8)
					.set("multipleOf", 9)
					.set("pattern", "k")
					.set("properties", map("x1", schemaInfo().setType("x2")))
					.set("readOnly", true)
					.set("$ref", "l")
					.set("required", true)
					.set("requiredProperties", set("m"))
					.set("title", "n")
					.set("type", "o")
					.set("uniqueItems", true)
					.set("xml", xml().setName("p"))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("additionalProperties{type},allOf,default,description,discriminator,enum,example,exclusiveMaximum,exclusiveMinimum,externalDocs{url},format,items{type},maximum,maxItems,maxLength,maxProperties,minimum,minItems,minLength,minProperties,multipleOf,pattern,properties{x1{type}},readOnly,ref,required,requiredProperties,title,type,uniqueItems,xml{name},x1,x2")
			.vals("{a},[{\"type\":\"b\"}],c,d,e,[f],g,true,true,{h},i,{j},1,2,3,4,5,6,7,8,9,k,{{x2}},true,l,true,[m],n,o,true,{p},x1a,<null>")
			.json("{'$ref':'l',additionalProperties:{type:'a'},allOf:[{type:'b'}],'default':'c',description:'d',discriminator:'e','enum':['f'],example:'g',exclusiveMaximum:true,exclusiveMinimum:true,externalDocs:{url:'h'},format:'i',items:{type:'j'},maxItems:2,maxLength:3,maxProperties:4,maximum:1,minItems:6,minLength:7,minProperties:8,minimum:5,multipleOf:9,pattern:'k',properties:{x1:{type:'x2'}},readOnly:true,required:true,requiredProperties:['m'],title:'n',type:'o',uniqueItems:true,x1:'x1a',xml:{name:'p'}}")
			.string("{'$ref':'l','additionalProperties':{'type':'a'},'allOf':[{'type':'b'}],'default':'c','description':'d','discriminator':'e','enum':['f'],'example':'g','exclusiveMaximum':true,'exclusiveMinimum':true,'externalDocs':{'url':'h'},'format':'i','items':{'type':'j'},'maxItems':2,'maxLength':3,'maxProperties':4,'maximum':1,'minItems':6,'minLength':7,'minProperties':8,'minimum':5,'multipleOf':9,'pattern':'k','properties':{'x1':{'type':'x2'}},'readOnly':true,'required':true,'requiredProperties':['m'],'title':'n','type':'o','uniqueItems':true,'x1':'x1a','xml':{'name':'p'}}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "$ref", "additionalProperties", "allOf", "default", "description", "discriminator", "enum", "example", "exclusiveMaximum", "exclusiveMinimum", "externalDocs", "format", "items", "maxItems", "maxLength", "maxProperties", "maximum", "minItems", "minLength", "minProperties", "minimum", "multipleOf", "pattern", "properties", "readOnly", "required", "requiredProperties", "title", "type", "uniqueItems", "x1", "x2", "xml");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"additionalProperties{type},allOf{#{type}},default,description,discriminator,enum,example,exclusiveMaximum,exclusiveMinimum,externalDocs{url},format,items{type},maximum,maxItems,maxLength,maxProperties,minimum,minItems,minLength,minProperties,multipleOf,pattern,properties{x1{type}},readOnly,$ref,required,requiredProperties,title,type,uniqueItems,xml{name},x1,x2",
				"{a},{[{b}]},c,d,e,[f],g,true,true,{h},i,{j},1,2,3,4,5,6,7,8,9,k,{{x2}},true,l,true,[m],n,o,true,{p},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"additionalProperties,allOf,default,description,discriminator,enum,example,exclusiveMaximum,exclusiveMinimum,externalDocs,format,items,maximum,maxItems,maxLength,maxProperties,minimum,minItems,minLength,minProperties,multipleOf,pattern,properties,readOnly,$ref,required,requiredProperties,title,type,uniqueItems,xml,x1,x2",
				"SchemaInfo,LinkedHashSet,String,String,String,LinkedHashSet,String,Boolean,Boolean,ExternalDocumentation,String,Items,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,String,LinkedHashMap,Boolean,String,Boolean,LinkedHashSet,String,String,Boolean,Xml,String,<null>"
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
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"))
				.addDefinition("Pets", JsonMap.of("type", "array", "items", JsonMap.of("$ref", "#/definitions/Pet")));

			assertBean(
				schemaInfo().setRef("#/definitions/Pet").resolveRefs(swagger, new ArrayDeque<>(), 10),
				"type,title",
				"object,Pet"
			);
		}

		@Test void d02_resolveRefs_nested() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"))
				.addDefinition("Pets", JsonMap.of("type", "array", "items", JsonMap.of("$ref", "#/definitions/Pet")));

			assertBean(
				schemaInfo().setRef("#/definitions/Pets").resolveRefs(swagger, new ArrayDeque<>(), 10),
				"type,items{type,title}",
				"array,{object,Pet}"
			);
		}

		@Test void d03_resolveRefs_maxDepth() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"))
				.addDefinition("Pets", JsonMap.of("type", "array", "items", JsonMap.of("$ref", "#/definitions/Pet")));

			assertBean(
				schemaInfo().setRef("#/definitions/Pets").resolveRefs(swagger, new ArrayDeque<>(), 1),
				"type,items{ref}",
				"array,{#/definitions/Pet}"
			);
		}

		@Test void d04_resolveRefs_circular() {
			var swagger = swagger()
				.addDefinition("A", JsonMap.of("type", "object", "title", "A", "properties", JsonMap.of("b", JsonMap.of("$ref", "#/definitions/B"))))
				.addDefinition("B", JsonMap.of("type", "object", "title", "B", "properties", JsonMap.of("a", JsonMap.of("$ref", "#/definitions/A"))));

			assertBean(
				schemaInfo().setRef("#/definitions/A").resolveRefs(swagger, new ArrayDeque<>(), 10),
				"type,title,properties{b{type,title,properties{a{ref}}}}",
				"object,A,{{object,B,{{#/definitions/A}}}}"
			);
		}

		@Test void d05_resolveRefs_properties() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"));

			assertBean(
				schemaInfo().setType("object").addProperty("pet", schemaInfo().setRef("#/definitions/Pet")).resolveRefs(swagger, new ArrayDeque<>(), 10),
				"type,properties{pet{type,title}}",
				"object,{{object,Pet}}"
			);
		}

		@Test void d06_resolveRefs_additionalProperties() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"));

			assertBean(
				schemaInfo().setType("object").setAdditionalProperties(schemaInfo().setRef("#/definitions/Pet")).resolveRefs(swagger, new ArrayDeque<>(), 10),
				"type,additionalProperties{type,title}",
				"object,{object,Pet}"
			);
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static SchemaInfo bean() {
		return schemaInfo();
	}
}