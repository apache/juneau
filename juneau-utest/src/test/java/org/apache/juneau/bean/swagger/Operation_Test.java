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
					.setConsumes(MediaType.of("a"))
					.setDeprecated(true)
					.setDescription("b")
					.setExternalDocs(externalDocumentation().setUrl(URI.create("c")))
					.setOperationId("d")
					.setParameters(parameterInfo().setName("e"))
					.setProduces(MediaType.of("f"))
					.setResponses(map("x1", responseInfo().setDescription("x2")))
					.setSchemes("g")
					.setSecurity(map("h", list("i")))
					.setSummary("j")
					.setTags("k")
			)
			.props("consumes,deprecated,description,externalDocs{url},operationId,parameters{#{name}},produces,responses{x1{description}},schemes,security{#{h}},summary,tags")
			.vals("[a],true,b,{c},d,{[{e}]},[f],{{x2}},[g],{[{[i]}]},j,[k]")
			.json("{consumes:['a'],deprecated:true,description:'b',externalDocs:{url:'c'},operationId:'d',parameters:[{name:'e'}],produces:['f'],responses:{x1:{description:'x2'}},schemes:['g'],security:[{h:['i']}],summary:'j',tags:['k']}")
			.string("{'consumes':['a'],'deprecated':true,'description':'b','externalDocs':{'url':'c'},'operationId':'d','parameters':[{'name':'e'}],'produces':['f'],'responses':{'x1':{'description':'x2'}},'schemes':['g'],'security':[{'h':['i']}],'summary':'j','tags':['k']}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "consumes", "deprecated", "description", "externalDocs", "operationId", "parameters", "produces", "responses", "schemes", "security", "summary", "tags");
		}

		@Test void a08_otherGettersAndSetters() {
			// Test Collection variants of setters
			var x = bean()
				.setParameters(list(
					parameterInfo("a1", "a2"),
					parameterInfo("a3", "a4")
				))
				.setConsumes(list(
					MediaType.of("b1"),
					MediaType.of("b2")
				))
				.setProduces(list(
					MediaType.of("c1"),
					MediaType.of("c2")
				))
				.setSchemes(list("d1", "d2"))
				.setSecurity(list(
					map("e1", list("e2")),
					map("e3", list("e4"))
				));

			assertBean(x,
				"parameters{#{in,name}},consumes,produces,schemes,security{0{e1},1{e3}}",
				"{[{a1,a2},{a3,a4}]},[b1,b2],[c1,c2],[d1,d2],{{[e2]},{[e4]}}"
			);

			// Test special getters
			x = bean()
				.setParameters(parameterInfo("a1", "a2"))
				.setResponses(map("b1", responseInfo("b2"), "200", responseInfo("b3")));

			assertBean(x.getParameter("a1", "a2"), "in,name", "a1,a2");
			assertBean(x.getResponse("b1"), "description", "b2");
			assertBean(x.getResponse(200), "description", "b3");
		}

		@Test void a09_nullParameters() {
			var x = bean();

			assertThrows(IllegalArgumentException.class, ()->x.getParameter(null, "a"));
			assertThrows(IllegalArgumentException.class, ()->x.getResponse(null));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Operation> TESTER =
			testBean(bean())
			.props("description,operationId,summary,tags,externalDocs,consumes,produces,parameters,responses,schemes,deprecated,security")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,false,<null>")
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
					.set("consumes", set(MediaType.of("a")))
					.set("deprecated", true)
					.set("description", "b")
					.set("externalDocs", externalDocumentation().setUrl(URI.create("c")))
					.set("operationId", "d")
					.set("parameters", list(parameterInfo().setName("e")))
					.set("produces", set(MediaType.of("f")))
					.set("responses", map("x1", responseInfo().setDescription("x2")))
					.set("schemes", set("g"))
					.set("security", list(map("h", list("i"))))
					.set("summary", "j")
					.set("tags", set("k"))
					.set("x3", "x3a")
					.set("x4", null)
			)
			.props("consumes,deprecated,description,externalDocs{url},operationId,parameters{#{name}},produces,responses{x1{description}},schemes,security{#{h}},summary,tags,x3,x4")
			.vals("[a],true,b,{c},d,{[{e}]},[f],{{x2}},[g],{[{[i]}]},j,[k],x3a,<null>")
			.json("{consumes:['a'],deprecated:true,description:'b',externalDocs:{url:'c'},operationId:'d',parameters:[{name:'e'}],produces:['f'],responses:{x1:{description:'x2'}},schemes:['g'],security:[{h:['i']}],summary:'j',tags:['k'],x3:'x3a'}")
			.string("{'consumes':['a'],'deprecated':true,'description':'b','externalDocs':{'url':'c'},'operationId':'d','parameters':[{'name':'e'}],'produces':['f'],'responses':{'x1':{'description':'x2'}},'schemes':['g'],'security':[{'h':['i']}],'summary':'j','tags':['k'],'x3':'x3a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "consumes", "deprecated", "description", "externalDocs", "operationId", "parameters", "produces", "responses", "schemes", "security", "summary", "tags", "x3", "x4");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"consumes,deprecated,description,externalDocs{url},operationId,parameters{#{name}},produces,responses{x1{description}},schemes,security{#{h}},summary,tags,x3,x4",
				"[a],true,b,{c},d,{[{e}]},[f],{{x2}},[g],{[{[i]}]},j,[k],x3a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"consumes,deprecated,description,externalDocs,operationId,parameters,produces,responses,schemes,security,summary,tags,x3,x4",
				"LinkedHashSet,Boolean,String,ExternalDocumentation,String,ArrayList,LinkedHashSet,LinkedHashMap,LinkedHashSet,ArrayList,String,LinkedHashSet,String,<null>"
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
			var x = bean()
				.addConsumes(MediaType.of("a1"))
				.addParameters(parameterInfo().setName("a2"))
				.addProduces(MediaType.of("a3"))
				.addResponse("200", responseInfo().setDescription("a4"))
				.addSchemes("a5")
				.addSecurity("a6", "a7")
				.addTags("a8");

			// Verify add methods don't throw exceptions and bean is not null
			assertNotNull(x);
			assertNotNull(x.getConsumes());
			assertNotNull(x.getParameters());
			assertNotNull(x.getProduces());
			assertNotNull(x.getResponses());
			assertNotNull(x.getSchemes());
			assertNotNull(x.getSecurity());
			assertNotNull(x.getTags());
		}

		@Test void d02_asMap() {
			assertBean(
				bean()
					.setDescription("a")
					.setOperationId("b")
					.set("x1", "x1a")
					.asMap(),
				"description,operationId,x1",
				"a,b,x1a"
			);
		}

		@Test void d03_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void d04_addMethodsWithNullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, ()->x.addResponse(null, responseInfo()));
			assertThrows(IllegalArgumentException.class, ()->x.addResponse("200", null));
			assertThrows(IllegalArgumentException.class, ()->x.addSecurity(null, "a"));
			assertThrows(IllegalArgumentException.class, ()->x.addSecurity(null));
		}

		@Test void d04_strict() {
			var x = bean();
			assertFalse(x.isStrict());
			x.strict();
			assertTrue(x.isStrict());
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

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static Operation bean() {
		return operation();
	}

}