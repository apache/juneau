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
 * Testcase for {@link ResponseInfo}.
 */
class ResponseInfo_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<ResponseInfo> TESTER =
			testBean(
				bean()
					.setDescription("a")
					.setExamples(map("x1", "x2"))
					.setHeaders(map("x3", headerInfo().setType("x4")))
					.setSchema(schemaInfo().setType("b"))
			)
			.props("description,examples{x1},headers{x3{type}},schema{type}")
			.vals("a,{x2},{{x4}},{b}")
			.json("{description:'a',examples:{x1:'x2'},headers:{x3:{type:'x4'}},schema:{type:'b'}}")
			.string("{'description':'a','examples':{'x1':'x2'},'headers':{'x3':{'type':'x4'}},'schema':{'type':'b'}}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "description", "examples", "headers", "schema");
		}

		@Test void a08_otherGettersAndSetters() {
			// No Collection variants for ResponseInfo setters

			// Test special getter
			var x = bean().setHeaders(map(
				"a1", headerInfo().setType("a2"),
				"a3", headerInfo().setType("a4")
			));

			assertBean(x.getHeader("a1"), "type", "a2");
			assertBean(x.getHeader("a3"), "type", "a4");
		}

		@Test void a09_nullParameters() {
			var x = bean();

			assertThrows(IllegalArgumentException.class, ()->x.getHeader(null));
			assertThrows(IllegalArgumentException.class, ()->x.addExample(null, "a"));
			assertThrows(IllegalArgumentException.class, ()->x.addExample("a", null));
			assertThrows(IllegalArgumentException.class, ()->x.addHeader(null, headerInfo()));
			assertThrows(IllegalArgumentException.class, ()->x.addHeader("a", null));
		}

		@Test void a10_addMethods() {
			var x = bean().addExample("a1", "a2").addHeader("a3", headerInfo().setDescription("a4"));
			assertNotNull(x);
			assertNotNull(x.getExamples());
			assertNotNull(x.getHeaders());
		}

		@Test void a11_asMap() {
			assertBean(
				bean()
					.setDescription("a")
					.set("x1", "x1a")
					.asMap(),
				"description,x1",
				"a,x1a"
			);
		}

		@Test void a12_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void a13_strictMode() {
			assertThrows(RuntimeException.class, () -> bean().strict().set("foo", "bar"));
			assertDoesNotThrow(() -> bean().set("foo", "bar"));

			assertFalse(bean().isStrict());
			assertTrue(bean().strict().isStrict());
			assertFalse(bean().strict(false).isStrict());
		}

		@Test void a14_copyFrom() {
			var x = bean().setDescription("a").setSchema(schemaInfo().setTitle("b")).addHeader("c1", headerInfo()).addExample("d1", "d2");
			var y = bean().copyFrom(x);
			assertBean(y, "description,schema{title}", "a,{b}");

			y = bean().setDescription("c").copyFrom(x);
			assertBean(y, "description,schema{title}", "a,{b}");

			y = bean().setDescription("c").copyFrom(null);
			assertBean(y, "description", "c");

			assertJson("{}", bean().copyFrom(bean()));
		}

		@Test void a15_getHeader() {
			// Test with null headers (covers the null check branch)
			var x = bean();
			assertNull(x.getHeader("nonexistent"));

			// Test with headers set
			var y = bean()
				.setHeaders(map("header1", headerInfo().setType("string")));

			assertNotNull(y.getHeader("header1"));
			assertEquals("string", y.getHeader("header1").getType());
			assertNull(y.getHeader("nonexistent"));

			// Test null name parameter
			assertThrows(IllegalArgumentException.class, () -> y.getHeader(null));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<ResponseInfo> TESTER =
			testBean(bean())
			.props("description,schema,headers,examples")
			.vals("<null>,<null>,<null>,<null>")
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

		private static final BeanTester<ResponseInfo> TESTER =
			testBean(
				bean()
					.set("description", "a")
					.set("examples", map("x1", "x2"))
					.set("headers", map("x3", headerInfo().setType("x4")))
					.set("schema", schemaInfo().setType("b"))
					.set("x5", "x5a")
					.set("x6", null)
			)
			.props("description,examples{x1},headers{x3{type}},schema{type},x5,x6")
			.vals("a,{x2},{{x4}},{b},x5a,<null>")
			.json("{description:'a',examples:{x1:'x2'},headers:{x3:{type:'x4'}},schema:{type:'b'},x5:'x5a'}")
			.string("{'description':'a','examples':{'x1':'x2'},'headers':{'x3':{'type':'x4'}},'schema':{'type':'b'},'x5':'x5a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "description", "examples", "headers", "schema", "x5", "x6");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"description,examples{x1},headers{x3{type}},schema{type},x5,x6",
				"a,{x2},{{x4}},{b},x5a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"description,examples,headers,schema,x5,x6",
				"String,LinkedHashMap,LinkedHashMap,SchemaInfo,String,<null>"
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
				responseInfo().setDescription("Success").setSchema(schemaInfo().setRef("#/definitions/Pet")).resolveRefs(swagger, new ArrayDeque<>(), 10),
				"description,schema{type,title}",
				"Success,{object,Pet}"
			);
		}

		@Test void d02_resolveRefs_headers() {
			var swagger = swagger()
				.addDefinition("MyHeader", JsonMap.of("type", "string", "description", "My Header"));

			assertBean(
				responseInfo().setDescription("Success").addHeader("X-Custom", headerInfo().setRef("#/definitions/MyHeader")).resolveRefs(swagger, new ArrayDeque<>(), 10),
				"description,headers{X-Custom{type,description}}",
				"Success,{{string,My Header}}"
			);
		}

		@Test void d03_resolveRefs_maxDepth() {
			var swagger = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object", "title", "Pet"));

			assertBean(
				responseInfo().setDescription("Success").setSchema(schemaInfo().setRef("#/definitions/Pet")).resolveRefs(swagger, new ArrayDeque<>(), 0),
				"description,schema{ref}",
				"Success,{#/definitions/Pet}"
			);
		}
	}


	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static ResponseInfo bean() {
		return responseInfo();
	}

}