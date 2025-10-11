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
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Operation}.
 */
class Operation_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<Operation> TESTER =
			testBean(
				bean()
					.setDescription("a")
					.setOperationId("b")
					.setParameters(parameter().setIn("c1").setName("c2"))
					.setRequestBody(requestBodyInfo().setDescription("d"))
					.setResponses(map("200", response().setDescription("e")))
					.setSecurity(securityRequirement().setRequirements(map("f1",list("f2"))))
					.setServers(server().setUrl(java.net.URI.create("http://example.com")))
					.setSummary("g")
					.setTags("h")
			)
			.props("description,operationId,parameters{0{in,name}},requestBody{description},responses{200{description}},security{0{requirements{f1}}},servers{0{url}},summary,tags")
			.vals("a,b,{{c1,c2}},{d},{{e}},{{{[f2]}}},{{http://example.com}},g,[h]")
			.json("{description:'a',operationId:'b',parameters:[{'in':'c1',name:'c2'}],requestBody:{description:'d'},responses:{'200':{description:'e'}},security:[{requirements:{f1:['f2']}}],servers:[{url:'http://example.com'}],summary:'g',tags:['h']}")
			.string("{'description':'a','operationId':'b','parameters':[{'in':'c1','name':'c2'}],'requestBody':{'description':'d'},'responses':{'200':{'description':'e'}},'security':[{'requirements':{'f1':['f2']}}],'servers':[{'url':'http://example.com'}],'summary':'g','tags':['h']}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "description", "operationId", "parameters", "requestBody", "responses", "security", "servers", "summary", "tags");
		}

		@Test void a08_otherGettersAndSetters() {
			// Test special getters
			var x = bean()
				.setParameters(parameter().setIn("a1").setName("a2"))
				.setResponses(map("b1", response().setDescription("b2"), "200", response().setDescription("b3")));

			assertBean(x.getParameter("a1", "a2"), "in,name", "a1,a2");
			assertBean(x.getResponse("b1"), "description", "b2");
			assertBean(x.getResponse(200), "description", "b3");
		}

		@Test void a09_nullParameters() {
			var x = bean();

			assertThrows(IllegalArgumentException.class, ()->x.getParameter(null, "a"));
			assertThrows(IllegalArgumentException.class, ()->x.getParameter("a", null));
			assertThrows(IllegalArgumentException.class, ()->x.getResponse(null));
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}

		@Test void a10_collectionSetters() {
			// Test Collection variants of setters
			var x = bean()
				.setParameters(list(
					parameter().setIn("a1").setName("a2"),
					parameter().setIn("a3").setName("a4")
				))
				.setSecurity(list(
					securityRequirement().setRequirements(map("b1", list("b2"))),
					securityRequirement().setRequirements(map("b3", list("b4")))
				))
				.setTags(list("c1", "c2"));

			assertBean(x,
				"parameters{#{in,name}},security{0{requirements{b1}},1{requirements{b3}}},tags",
				"{[{a1,a2},{a3,a4}]},{{{[b2]}},{{[b4]}}},[c1,c2]"
			);
		}

		@Test void a11_asMap() {
			assertBean(
				bean()
					.setSummary("a")
					.set("x1", "x1a")
					.asMap(),
				"summary,x1",
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
		
		@Test void a14_collectionSetters() {
			var x = bean()
				.setParameters(list(parameter().setIn("a1").setName("a2"), parameter().setIn("b1").setName("b2")))
				.setSecurity(list(securityRequirement().setRequirements(map("c1", list("c2"))), securityRequirement().setRequirements(map("d1", list("d2")))))
				.setServers(list(server().setUrl(java.net.URI.create("http://example1.com")), server().setUrl(java.net.URI.create("http://example2.com"))))
				.setTags(list("f1", "f2"));

			assertBean(x,
				"parameters{#{in,name}},security{#{requirements}},servers{#{url}},tags",
				"{[{a1,a2},{b1,b2}]},{[{{c1=[c2]}},{{d1=[d2]}}]},{[{http://example1.com},{http://example2.com}]},[f1,f2]"
			);
		}

		@Test void a15_getParameter() {
			// Test getParameter with null parameters list (covers the null check branch)
			var y = bean();
			assertNull(y.getParameter("query", "param1"));

			// Test with parameters set
			var x = bean()
				.setParameters(list(
					parameter().setIn("query").setName("param1"),
					parameter().setIn("path").setName("param2")
				));

			// Test normal parameter lookup
			var param1 = x.getParameter("query", "param1");
			assertNotNull(param1);
			assertEquals("param1", param1.getName());
			assertEquals("query", param1.getIn());

			var param2 = x.getParameter("path", "param2");
			assertNotNull(param2);
			assertEquals("param2", param2.getName());
			assertEquals("path", param2.getIn());

			// Test non-existent parameter
			assertNull(x.getParameter("query", "nonexistent"));
			assertNull(x.getParameter("header", "param1"));
		}

		@Test void a16_getResponseBranchCoverage() {
			// Test getResponse with null responses map (covers the null check branch)
			var y = bean();
			assertNull(y.getResponse("200"));

			// Test with responses set
			var x = bean()
				.setResponses(map("200", response().setDescription("OK"), "404", response().setDescription("Not Found")));

			var response200 = x.getResponse("200");
			assertNotNull(response200);
			assertEquals("OK", response200.getDescription());

			var response404 = x.getResponse("404");
			assertNotNull(response404);
			assertEquals("Not Found", response404.getDescription());

			assertNull(x.getResponse("500"));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Operation> TESTER =
			testBean(bean())
			.props("summary,description,operationId,tags,parameters,requestBody,responses,security")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
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
		private static final BeanTester<Operation> TESTER =
			testBean(
				bean()
					.set("callbacks", map("a1", callback()))
					.set("deprecated", true)
					.set("description", "b")
					.set("externalDocs", externalDocumentation().setUrl(URI.create("c")))
					.set("operationId", "d")
					.set("parameters", list(parameter("e1", "e2")))
					.set("requestBody", requestBodyInfo().setDescription("f"))
					.set("responses", map("g1", response("g2")))
					.set("security", list(securityRequirement().set("h1", list("h2"))))
					.set("servers", list(server().setUrl(URI.create("i"))))
					.set("summary", "j")
					.set("tags", list("k"))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("callbacks{a1},deprecated,description,externalDocs{url},operationId,parameters{#{in,name}},requestBody{description},responses{g1{description}},security{#{toString}},servers{#{url}},summary,tags{#{toString},x1,x2")
			.vals("{{}},true,b,{c},d,{[{e1,e2}]},{f},{{g2}},{[{<null>}]},{[{i}]},j,[k]")
			.json("{callbacks:{a1:{}},deprecated:true,description:'b',externalDocs:{url:'c'},operationId:'d',parameters:[{'in':'e1',name:'e2'}],requestBody:{description:'f'},responses:{g1:{description:'g2'}},security:[{h1:['h2']}],servers:[{url:'i'}],summary:'j',tags:['k'],x1:'x1a'}")
			.string("{'callbacks':{'a1':{}},'deprecated':true,'description':'b','externalDocs':{'url':'c'},'operationId':'d','parameters':[{'in':'e1','name':'e2'}],'requestBody':{'description':'f'},'responses':{'g1':{'description':'g2'}},'security':[{'h1':['h2']}],'servers':[{'url':'i'}],'summary':'j','tags':['k'],'x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "callbacks", "deprecated", "description", "externalDocs", "operationId", "parameters", "requestBody", "responses", "security", "servers", "summary", "tags", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"callbacks{a1},deprecated,description,externalDocs{url},operationId,parameters{#{in,name}},requestBody{description},responses{g1{description}},security{#{h1{#{toString}}}},servers{#{url}},summary,tags{#{toString}},x1,x2",
				"{{}},true,b,{c},d,{[{e1,e2}]},{f},{{g2}},{[{{[{h2}]}}]},{[{i}]},j,{[{k}]},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"callbacks,deprecated,description,externalDocs,operationId,parameters,requestBody,responses,security,servers,summary,tags,x1,x2",
				"LinkedHashMap,Boolean,String,ExternalDocumentation,String,ArrayList,RequestBodyInfo,LinkedHashMap,ArrayList,ArrayList,String,ArrayList,String,<null>"
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

	private static Operation bean() {
		return operation();
	}
}