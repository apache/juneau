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
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.MediaType;
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
					.setSecurity(map("h", l("i")))
					.setSummary("j")
					.setTags("k")
			)
			.props("consumes,deprecated,description,externalDocs{url},operationId,parameters{#{name}},produces,responses{x1{description}},schemes,security{#{h}},summary,tags")
			.vals("[a],true,b,{c},d,{[{e}]},[f],{{x2}},[g],{[{[i]}]},j,[k]")
			.json("{consumes:['a'],deprecated:true,description:'b',externalDocs:{url:'c'},operationId:'d',parameters:[{name:'e'}],produces:['f'],responses:{x1:{description:'x2'}},schemes:['g'],security:[{h:['i']}],summary:'j',tags:['k']}")
			.string("{'consumes':['a'],'deprecated':true,'description':'b','externalDocs':{'url':'c'},'operationId':'d','parameters':[{'name':'e'}],'produces':['f'],'responses':{'x1':{'description':'x2'}},'schemes':['g'],'security':[{'h':['i']}],'summary':'j','tags':['k']}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "consumes", "deprecated", "description", "externalDocs", "operationId", "parameters", "produces", "responses", "schemes", "security", "summary", "tags");
		}

		@Test void a08_otherGettersAndSetters() {
			// Test special getters
			var x = bean()
				.setParameters(parameterInfo("a1", "a2"))
				.setResponses(map("b1", responseInfo("b2"), "200", responseInfo("b3")));

			assertBean(x.getParameter("a1", "a2"), "in,name", "a1,a2");
			assertBean(x.getResponse("b1"), "description", "b2");
			assertBean(x.getResponse(200), "description", "b3");

			assertNull(bean().getResponse("x"));

			// Test Collection variant of addSecurity
			x = bean()
				.addSecurity(l(
					m("c1", l("c2")),
					m("c3", l("c4"))
				));

			assertBean(x, "security{0{c1},1{c3}}", "{{[c2]},{[c4]}}");
		}

		@Test void a09_nullParameters() {
			var x = bean();

			assertThrows(IllegalArgumentException.class, ()->x.getParameter(null, "a"));
			assertThrows(IllegalArgumentException.class, ()->x.getResponse(null));
			assertThrows(IllegalArgumentException.class, ()->x.addResponse(null, responseInfo()));
			assertThrows(IllegalArgumentException.class, ()->x.addResponse("200", null));
			assertThrows(IllegalArgumentException.class, ()->x.addSecurity(null, "a"));
			assertThrows(IllegalArgumentException.class, ()->x.addSecurity(null));
		}

		@Test void a10_collectionSetters() {
			// Test Collection variants of setters
			var x = bean()
				.setParameters(l(
					parameterInfo("a1", "a2"),
					parameterInfo("a3", "a4")
				))
				.setConsumes(l(
					MediaType.of("b1"),
					MediaType.of("b2")
				))
				.setProduces(l(
					MediaType.of("c1"),
					MediaType.of("c2")
				))
				.setSchemes(l("d1", "d2"))
				.setSecurity(l(
					m("e1", l("e2")),
					m("e3", l("e4"))
				));

			assertBean(x,
				"parameters{#{in,name}},consumes,produces,schemes,security{0{e1},1{e3}}",
				"{[{a1,a2},{a3,a4}]},[b1,b2],[c1,c2],[d1,d2],{{[e2]},{[e4]}}"
			);
		}

		@Test void a11_varargAdders() {
			// Test varargs addX methods - call each method twice
			var x = bean()
				.addConsumes(MediaType.of("a1"))
				.addConsumes(MediaType.of("a2"))
				.addProduces(MediaType.of("b1"))
				.addProduces(MediaType.of("b2"))
				.addSchemes("c1")
				.addSchemes("c2")
				.addTags("d1")
				.addTags("d2");

			assertBean(x,
				"consumes,produces,schemes,tags",
				"[a1,a2],[b1,b2],[c1,c2],[d1,d2]"
			);
		}

		@Test void a12_collectionAdders() {
			// Test Collection addX methods - call each method twice
			var x = bean()
				.addConsumes(l(MediaType.of("a1")))
				.addConsumes(l(MediaType.of("a2")))
				.addParameters(l(parameterInfo("query", "a")))
				.addParameters(l(parameterInfo("path", "b")))
				.addProduces(l(MediaType.of("b1")))
				.addProduces(l(MediaType.of("b2")))
				.addSchemes(l("c1"))
				.addSchemes(l("c2"))
				.addTags(l("d1"))
				.addTags(l("d2"));

			assertBean(x,
				"consumes,produces,schemes,tags",
				"[a1,a2],[b1,b2],[c1,c2],[d1,d2]"
			);
		}

		@Test void a13_asMap() {
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

		@Test void a14_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void a15_addSecurity() {
			// Test addSecurity method
			var x = bean()
				.addSecurity("scheme1", "a", "b")
				.addSecurity("scheme2", "c");

			assertBean(x,
				"security{0{scheme1},1{scheme2}}",
				"{{[a,b]},{[c]}}"
			);
		}

		@Test void a16_addSecurityCollection() {
			// Test addSecurity with Collection
			Map<String,List<String>> map1 = map();
			map1.put("scheme1", l("a"));
			Map<String,List<String>> map2 = map();
			map2.put("scheme2", l("b"));

			Collection<Map<String,List<String>>> coll1 = l(map1);
			Collection<Map<String,List<String>>> coll2 = l(map2);

			var x = bean()
				.addSecurity(coll1)
				.addSecurity(coll2);

			assertBean(x,
				"security{0{scheme1},1{scheme2}}",
				"{{[a]},{[b]}}"
			);
		}

		@Test void a17_getParameter() {
			// Test getParameter method with different scenarios
			var x = bean()
				.addParameters(
					parameterInfo("query", "param1"),
					parameterInfo("path", "param2"),
					parameterInfo("body", null) // body parameter with null name
				);

			// Test getting an existing parameter
			assertNotNull(x.getParameter("query", "param1"));

			// Test getting a non-existent parameter
			assertNull(x.getParameter("query", "nonexistent"));

			// Test getting a parameter with different location
			assertNull(x.getParameter("header", "param1"));

			// Test getting a body parameter (special case - name can be null)
			assertNotNull(x.getParameter("body", null));
			assertNotNull(x.getParameter("body", "anyName")); // body matches regardless of name

			// Test with null parameters list (covers the null check branch)
			var y = bean();
			assertNull(y.getParameter("query", "param1"));

			// Test with parameters that include a body parameter (covers the "body" branch)
			x = bean()
				.setParameters(l(
					parameterInfo("query", "param1"),
					parameterInfo("body", null) // body parameter with null name
				));

			// Test normal parameter lookup
			var param1 = x.getParameter("query", "param1");
			assertNotNull(param1);
			assertEquals("param1", param1.getName());
			assertEquals("query", param1.getIn());

			assertNull(x.getParameter("query", "nonexistent"));

			// Test body parameter lookup (this covers the missing branch)
			var bodyParam = x.getParameter("body", null);
			assertNotNull(bodyParam);
			assertEquals("body", bodyParam.getIn());

			// Test body parameter with any name (should still match)
			var bodyParam2 = x.getParameter("body", "anyName");
			assertNotNull(bodyParam2);
			assertEquals("body", bodyParam2.getIn());
		}

		@Test void a18_strictMode() {
			assertThrows(RuntimeException.class, () -> bean().strict().set("foo", "bar"));
			assertDoesNotThrow(() -> bean().set("foo", "bar"));

			assertFalse(bean().isStrict());
			assertTrue(bean().strict().isStrict());
			assertFalse(bean().strict(false).isStrict());
		}

		@Test void a19_isDeprecated() {
			assertFalse(bean().isDeprecated());
			assertFalse(bean().setDeprecated(false).isDeprecated());
			assertTrue(bean().setDeprecated(true).isDeprecated());
		}

		@Test void a20_addResponse() {
			var b = bean().addResponse("200", responseInfo()).addResponse("201", responseInfo());
			assertSize(2, b.getResponses());
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
					.set("parameters", l(parameterInfo().setName("e")))
					.set("produces", set(MediaType.of("f")))
					.set("responses", m("x1", responseInfo().setDescription("x2")))
					.set("schemes", set("g"))
					.set("security", l(map("h", l("i"))))
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
				TESTER.bean(), (obj,prop) -> cns(obj.get(prop, Object.class)),
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

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static Operation bean() {
		return operation();
	}
}