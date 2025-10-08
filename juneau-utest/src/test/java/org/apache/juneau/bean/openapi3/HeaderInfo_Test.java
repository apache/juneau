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

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link HeaderInfo}.
 */
class HeaderInfo_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<HeaderInfo> TESTER =
			testBean(
				bean()
					.setAllowEmptyValue(true)
					.setAllowReserved(true)
					.setDeprecated(true)
					.setDescription("a")
					.setExample("b")
					.setExamples(map("c1",example().setDescription("c2")))
					.setExplode(true)
					.setRef("c")
					.setRequired(true)
					.setSchema(schemaInfo().setType("d"))
			)
			.props("allowEmptyValue,allowReserved,deprecated,description,example,examples{c1{description}},explode,ref,required,schema{type}")
			.vals("true,true,true,a,b,{{c2}},true,c,true,{d}")
			.json("{'$ref':'c',allowEmptyValue:true,allowReserved:true,deprecated:true,description:'a',examples:{c1:{description:'c2'}},explode:true,required:true,schema:{type:'d'},'x-example':'b'}")
			.string("{'$ref':'c','allowEmptyValue':true,'allowReserved':true,'deprecated':true,'description':'a','examples':{'c1':{'description':'c2'}},'explode':true,'required':true,'schema':{'type':'d'},'x-example':'b'}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "$ref", "allowEmptyValue", "allowReserved", "deprecated", "description", "examples", "explode", "required", "schema", "x-example");
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
			.props("description,required,explode,deprecated,allowEmptyValue,allowReserved,ref,schema,example,examples")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
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
					.set("allowEmptyValue", true)
					.set("allowReserved", true)
					.set("deprecated", true)
					.set("description", "a")
					.set("x-example", "b")
					.set("examples", map("c1", example().setSummary("c2")))
					.set("explode", true)
					.set("required", true)
					.set("schema", schemaInfo("d"))
					.set("style", "e")
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("allowEmptyValue,allowReserved,deprecated,description,example,examples{c1{summary}},explode,required,schema{type},style,x1,x2")
			.vals("true,true,true,a,b,{{c2}},true,true,{d},e,x1a,<null>")
			.json("{allowEmptyValue:true,allowReserved:true,deprecated:true,description:'a',examples:{c1:{summary:'c2'}},explode:true,required:true,schema:{type:'d'},style:'e','x-example':'b',x1:'x1a'}")
			.string("{'allowEmptyValue':true,'allowReserved':true,'deprecated':true,'description':'a','examples':{'c1':{'summary':'c2'}},'explode':true,'required':true,'schema':{'type':'d'},'style':'e','x-example':'b','x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "allowEmptyValue", "allowReserved", "deprecated", "description", "examples", "explode", "required", "schema", "style", "x-example", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"allowEmptyValue,allowReserved,deprecated,description,x-example,examples{c1{summary}},explode,required,schema{type},style,x1,x2",
				"true,true,true,a,b,{{c2}},true,true,{d},e,x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"allowEmptyValue,allowReserved,deprecated,description,x-example,examples,explode,required,schema,style,x1,x2",
				"Boolean,Boolean,Boolean,String,String,LinkedHashMap,Boolean,Boolean,SchemaInfo,String,String,<null>"
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
					.addExample("a1", example().setSummary("a2")),
				"examples{a1{summary}}",
				"{{a2}}"
			);
		}

		@Test void d02_asMap() {
			assertBean(
				bean()
					.setDescription("a")
					.set("x1", "x1a")
					.asMap(),
				"description,x1",
				"a,x1a"
			);
		}

		@Test void d03_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}
	}

	@Nested class E_strictMode extends TestBase {

		@Test void e01_strictModeSetThrowsException() {
			var x = bean().strict();
			assertThrows(RuntimeException.class, () -> x.set("foo", "bar"));
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
	}

	@Nested class F_refs extends TestBase {

		@Test void f01_resolveRefs_basic() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyHeader", schemaInfo().setType("string").setDescription("My Header")
				)));

			var x = headerInfo().setRef("#/components/schemas/MyHeader");
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 10);
			assertBean(y, "type,description", "string,My Header");
		}

		@Test void f02_resolveRefs_maxDepth() {
			var openApi = openApi()
				.setComponents(components().setSchemas(Map.of(
					"MyHeader", schemaInfo().setType("string").setDescription("My Header")
				)));

			var x = headerInfo().setRef("#/components/schemas/MyHeader");
			var y = x.resolveRefs(openApi, new ArrayDeque<>(), 0);
			assertBean(y, "ref", "#/components/schemas/MyHeader");
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static HeaderInfo bean() {
		return headerInfo();
	}
}
