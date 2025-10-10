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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Parameter}.
 */
class Parameter_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<Parameter> TESTER =
			testBean(
				bean()
					.setAllowEmptyValue(true)
					.setAllowReserved(true)
					.setDeprecated(true)
					.setDescription("a")
					.setExample("b")
					.setExamples(map("c1", example().setSummary("c2")))
					.setExplode(true)
					.setIn("d")
					.setName("e")
					.setRequired(true)
					.setSchema(schemaInfo().setType("f"))
					.setStyle("g")
			)
			.props("allowEmptyValue,allowReserved,deprecated,description,example,examples{c1{summary}},explode,in,name,required,schema{type},style")
			.vals("true,true,true,a,b,{{c2}},true,d,e,true,{f},g")
			.json("{allowEmptyValue:true,allowReserved:true,deprecated:true,description:'a',example:'b',examples:{c1:{summary:'c2'}},explode:true,'in':'d',name:'e',required:true,schema:{type:'f'},style:'g'}")
			.string("{'allowEmptyValue':true,'allowReserved':true,'deprecated':true,'description':'a','example':'b','examples':{'c1':{'summary':'c2'}},'explode':true,'in':'d','name':'e','required':true,'schema':{'type':'f'},'style':'g'}".replace('\'','"'))
		;

		@Test void a01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void a02_copy() {
			var t = new Parameter(TESTER.bean());
			assertNotSame(TESTER.bean(), t);
			assertBean(t, TESTER.props(), TESTER.vals());
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
			assertList(TESTER.bean().keySet(), "allowEmptyValue", "allowReserved", "deprecated", "description", "example", "examples", "explode", "in", "name", "required", "schema", "style");
		}

		@Test void a08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}

		@Test void a09_asMap() {
			assertBean(
				bean()
					.setName("a")
					.set("x1", "x1a")
					.asMap(),
				"name,x1",
				"a,x1a"
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

			var x = bean().strict();
			var y = bean(); // not strict
			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setIn(String).  Value='invalid', valid values=['query','header','path','cookie']", () -> x.setIn("invalid"));
			assertDoesNotThrow(() -> x.setIn("query"));
			assertDoesNotThrow(() -> x.setIn("header"));
			assertDoesNotThrow(() -> x.setIn("path"));
			assertDoesNotThrow(() -> x.setIn("cookie"));
			assertDoesNotThrow(() -> y.setIn("invalid"));

			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setStyle(String).  Value='invalid', valid values=['matrix','label','form','simple','spaceDelimited','pipeDelimited','deepObject']", () -> x.setStyle("invalid"));
			assertDoesNotThrow(() -> x.setStyle("matrix"));
			assertDoesNotThrow(() -> x.setStyle("label"));
			assertDoesNotThrow(() -> x.setStyle("form"));
			assertDoesNotThrow(() -> x.setStyle("simple"));
			assertDoesNotThrow(() -> x.setStyle("spaceDelimited"));
			assertDoesNotThrow(() -> x.setStyle("pipeDelimited"));
			assertDoesNotThrow(() -> x.setStyle("deepObject"));
			assertDoesNotThrow(() -> y.setStyle("invalid"));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Parameter> TESTER =
			testBean(bean())
			.props("name,in,description,required,deprecated,allowEmptyValue,style,explode,allowReserved,schema,example,examples")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
			.json("{}")
			.string("{}")
		;

		@Test void b01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void b02_copy() {
			var t = new Parameter(TESTER.bean());
			assertNotSame(TESTER.bean(), t);
			assertBean(t, TESTER.props(), TESTER.vals());
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
		private static final BeanTester<Parameter> TESTER =
			testBean(
				bean()
					.set("allowEmptyValue", true)
					.set("allowReserved", true)
					.set("content", map("a1", mediaType().setSchema(schemaInfo("a2"))))
					.set("deprecated", true)
					.set("description", "b")
					.set("example", "c")
					.set("examples", map("d1", example().setSummary("d2")))
					.set("explode", true)
					.set("in", "e")
					.set("name", "f")
					.set("required", true)
					.set("schema", schemaInfo("g"))
					.set("style", "h")
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("allowEmptyValue,allowReserved,content{a1{schema{type}}},deprecated,description,example,examples{d1{summary}},explode,in,name,required,schema{type},style,x1,x2")
			.vals("true,true,{{{a2}}},true,b,c,{{d2}},true,e,f,true,{g},h,x1a,<null>")
			.json("{allowEmptyValue:true,allowReserved:true,content:{a1:{schema:{type:'a2'}}},deprecated:true,description:'b',example:'c',examples:{d1:{summary:'d2'}},explode:true,'in':'e',name:'f',required:true,schema:{type:'g'},style:'h',x1:'x1a'}")
			.string("{'allowEmptyValue':true,'allowReserved':true,'content':{'a1':{'schema':{'type':'a2'}}},'deprecated':true,'description':'b','example':'c','examples':{'d1':{'summary':'d2'}},'explode':true,'in':'e','name':'f','required':true,'schema':{'type':'g'},'style':'h','x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "allowEmptyValue", "allowReserved", "content", "deprecated", "description", "example", "examples", "explode", "in", "name", "required", "schema", "style", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"allowEmptyValue,allowReserved,content{a1{schema{type}}},deprecated,description,example,examples{d1{summary}},explode,in,name,required,schema{type},style,x1,x2",
				"true,true,{{{a2}}},true,b,c,{{d2}},true,e,f,true,{g},h,x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"allowEmptyValue,allowReserved,content,deprecated,description,example,examples,explode,in,name,required,schema,style,x1,x2",
				"Boolean,Boolean,LinkedHashMap,Boolean,String,String,LinkedHashMap,Boolean,String,String,Boolean,SchemaInfo,String,String,<null>"
			);
		}

		@Test void c10_nullPropertyValue() {
			assertThrows(IllegalArgumentException.class, ()->bean().get(null));
			assertThrows(IllegalArgumentException.class, ()->bean().get(null, String.class));
			assertThrows(IllegalArgumentException.class, ()->bean().set(null, "a"));
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static Parameter bean() {
		return parameter();
	}
}
