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
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Components}.
 */
class Components_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<Components> TESTER =
			testBean(
				bean()
					.setCallbacks(map("a1", callback()))
					.setExamples(map("b1", example().setSummary("b2")))
					.setHeaders(map("c1", headerInfo(schemaInfo("c2"))))
					.setLinks(map("d1", link().setOperationId("d2")))
					.setParameters(map("e1", parameter("e2", "e3")))
					.setRequestBodies(map("f1", requestBodyInfo().setDescription("f2")))
					.setResponses(map("g1", response("g2")))
					.setSchemas(map("h1", schemaInfo().setType("h2")))
					.setSecuritySchemes(map("i1", securitySchemeInfo("i2")))
			)
			.props("callbacks{a1},examples{b1{summary}},headers{c1{schema{type}}},links{d1{operationId}},parameters{e1{in,name}},requestBodies{f1{description}},responses{g1{description}},schemas{h1{type}},securitySchemes{i1{type}}")
			.vals("{{}},{{b2}},{{{c2}}},{{d2}},{{e2,e3}},{{f2}},{{g2}},{{h2}},{{i2}}")
			.json("{callbacks:{a1:{}},examples:{b1:{summary:'b2'}},headers:{c1:{schema:{type:'c2'}}},links:{d1:{operationId:'d2'}},parameters:{e1:{'in':'e2',name:'e3'}},requestBodies:{f1:{description:'f2'}},responses:{g1:{description:'g2'}},schemas:{h1:{type:'h2'}},securitySchemes:{i1:{type:'i2'}}}")
			.string("{'callbacks':{'a1':{}},'examples':{'b1':{'summary':'b2'}},'headers':{'c1':{'schema':{'type':'c2'}}},'links':{'d1':{'operationId':'d2'}},'parameters':{'e1':{'in':'e2','name':'e3'}},'requestBodies':{'f1':{'description':'f2'}},'responses':{'g1':{'description':'g2'}},'schemas':{'h1':{'type':'h2'}},'securitySchemes':{'i1':{'type':'i2'}}}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "callbacks", "examples", "headers", "links", "parameters", "requestBodies", "responses", "schemas", "securitySchemes");
		}

		@Test void a08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}

		@Test void a09_asMap() {
			assertBean(
				bean()
					.setSchemas(map("a1", schemaInfo("a2")))
					.set("x1", "x1a")
					.asMap(),
				"schemas{a1{type}},x1",
				"{{a2}},x1a"
			);
		}

		@Test void a10_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void a11_strictMode() {
			assertThrows(RuntimeException.class, () -> bean().strict().set("foo", "bar"));

			assertFalse(bean().isStrict());
			assertTrue(bean().strict().isStrict());
			assertFalse(bean().strict(false).isStrict());

			assertDoesNotThrow(() -> bean().set("foo", "bar"));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Components> TESTER =
			testBean(bean())
			.props("schemas,responses,parameters,examples,requestBodies,headers,securitySchemes,links,callbacks")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
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
		private static final BeanTester<Components> TESTER =
			testBean(
				bean()
					.set("callbacks", map("a1", callback()))
					.set("examples", map("b1", example().setSummary("b2")))
					.set("headers", map("c1", headerInfo(schemaInfo("c2"))))
					.set("links", map("d1", link().setOperationId("d2")))
					.set("parameters", map("e1", parameter("e2", "e3")))
					.set("requestBodies", map("f1", requestBodyInfo().setDescription("f2")))
					.set("responses", map("g1", response("g2")))
					.set("schemas", map("h1", schemaInfo().setType("h2")))
					.set("securitySchemes", map("i1", securitySchemeInfo("i2")))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("callbacks{a1},examples{b1{summary}},headers{c1{schema{type}}},links{d1{operationId}},parameters{e1{in,name}},requestBodies{f1{description}},responses{g1{description}},schemas{h1{type}},securitySchemes{i1{type}},x1,x2")
			.vals("{{}},{{b2}},{{{c2}}},{{d2}},{{e2,e3}},{{f2}},{{g2}},{{h2}},{{i2}},x1a,<null>")
			.json("{callbacks:{a1:{}},examples:{b1:{summary:'b2'}},headers:{c1:{schema:{type:'c2'}}},links:{d1:{operationId:'d2'}},parameters:{e1:{'in':'e2',name:'e3'}},requestBodies:{f1:{description:'f2'}},responses:{g1:{description:'g2'}},schemas:{h1:{type:'h2'}},securitySchemes:{i1:{type:'i2'}},x1:'x1a'}")
			.string("{'callbacks':{'a1':{}},'examples':{'b1':{'summary':'b2'}},'headers':{'c1':{'schema':{'type':'c2'}}},'links':{'d1':{'operationId':'d2'}},'parameters':{'e1':{'in':'e2','name':'e3'}},'requestBodies':{'f1':{'description':'f2'}},'responses':{'g1':{'description':'g2'}},'schemas':{'h1':{'type':'h2'}},'securitySchemes':{'i1':{'type':'i2'}},'x1':'x1a'}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "callbacks", "examples", "headers", "links", "parameters", "requestBodies", "responses", "schemas", "securitySchemes", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"callbacks{a1},examples{b1{summary}},headers{c1{schema{type}}},links{d1{operationId}},parameters{e1{in,name}},requestBodies{f1{description}},responses{g1{description}},schemas{h1{type}},securitySchemes{i1{type}},x1,x2",
				"{{}},{{b2}},{{{c2}}},{{d2}},{{e2,e3}},{{f2}},{{g2}},{{h2}},{{i2}},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> scn(obj.get(prop, Object.class)),
				"callbacks,examples,headers,links,parameters,requestBodies,responses,schemas,securitySchemes,x1,x2",
				"LinkedHashMap,LinkedHashMap,LinkedHashMap,LinkedHashMap,LinkedHashMap,LinkedHashMap,LinkedHashMap,LinkedHashMap,LinkedHashMap,String,<null>"
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

	private static Components bean() {
		return components();
	}
}