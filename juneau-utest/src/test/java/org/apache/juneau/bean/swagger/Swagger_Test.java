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

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Swagger}.
 */
class Swagger_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<Swagger> TESTER =
			testBean(
				bean()
					.setBasePath("a")
					.setConsumes(MediaType.of("b"))
					.setDefinitions(map("c1", JsonMap.of("type", "c2")))
					.setExternalDocs(externalDocumentation("d"))
					.setHost("e")
					.setInfo(info("f1", "f2"))
					.setParameters(map("g1", parameterInfo("g2", "g3")))
					.setPaths(map("h1", operationMap().append("get", operation().setSummary("h2"))))
					.setProduces(MediaType.of("i"))
					.setResponses(map("j1", responseInfo().setDescription("j2")))
					.setSchemes("k")
					.setSecurity(list(map("l1", list("l2"))))
					.setSecurityDefinitions(map("m1", securityScheme().setType("m2")))
					.setSwagger("n")
					.setTags(list(tag().setName("o")))
			)
			.props("basePath,consumes,definitions{c1{type}},externalDocs{url},host,info{title,version},parameters{g1{in,name}},paths{h1{get{summary}}},produces,responses{j1{description}},schemes,security{0{l1}},securityDefinitions{m1{type}},swagger,tags{0{name}}")
			.vals("a,[b],{{c2}},{d},e,{f1,f2},{{g2,g3}},{{{h2}}},[i],{{j2}},[k],{{[l2]}},{{m2}},n,{{o}}")
			.json("{basePath:'a',consumes:['b'],definitions:{c1:{type:'c2'}},externalDocs:{url:'d'},host:'e',info:{title:'f1',version:'f2'},parameters:{g1:{'in':'g2',name:'g3'}},paths:{h1:{get:{summary:'h2'}}},produces:['i'],responses:{j1:{description:'j2'}},schemes:['k'],security:[{l1:['l2']}],securityDefinitions:{m1:{type:'m2'}},swagger:'n',tags:[{name:'o'}]}")
			.string("{'basePath':'a','consumes':['b'],'definitions':{'c1':{'type':'c2'}},'externalDocs':{'url':'d'},'host':'e','info':{'title':'f1','version':'f2'},'parameters':{'g1':{'in':'g2','name':'g3'}},'paths':{'h1':{'get':{'summary':'h2'}}},'produces':['i'],'responses':{'j1':{'description':'j2'}},'schemes':['k'],'security':[{'l1':['l2']}],'securityDefinitions':{'m1':{'type':'m2'}},'swagger':'n','tags':[{'name':'o'}]}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "basePath", "consumes", "definitions", "externalDocs", "host", "info", "parameters", "paths", "produces", "responses", "schemes", "security", "securityDefinitions", "swagger", "tags");
		}

	@Test void a08_otherGettersAndSetters() {
		// Test Collection variants of setters
		var x = bean()
			.setConsumes(list(
				MediaType.of("a1"),
				MediaType.of("a2")
			))
			.setProduces(list(
				MediaType.of("b1"),
				MediaType.of("b2")
			))
			.setSchemes(list("c1", "c2"))
			.setSecurity(list(
				map("d1", list("d2")),
				map("d3", list("d4"))
			));

		assertBean(x,
			"consumes,produces,schemes,security{0{d1},1{d3}}",
			"[a1,a2],[b1,b2],[c1,c2],{{[d2]},{[d4]}}"
		);

		// Test special getters
		x = bean()
			.addPath("a1", "get", operation().setSummary("a2"))
			.addPath("b1", "get", operation().addResponse("200", responseInfo("b2")).setParameters(parameterInfo("b3", "b4")))
			.addParameter("c1", parameterInfo("c2", "c3"));

		assertBean(x.getPath("a1"), "get{summary}", "{a2}");
		assertBean(x.getOperation("a1", "get"), "summary", "a2");
		assertBean(x.getResponseInfo("b1", "get", "200"), "description", "b2");
		assertBean(x.getResponseInfo("b1", "get", 200), "description", "b2");
		assertBean(x.getParameterInfo("b1", "get", "b3", "b4"), "in,name", "b3,b4");
	}

		@Test void a09_nullParameters() {
			var x = bean();

			assertThrows(IllegalArgumentException.class, ()->x.getPath(null));
			assertThrows(IllegalArgumentException.class, ()->x.getOperation(null, "a"));
			assertThrows(IllegalArgumentException.class, ()->x.getOperation("a", null));
			assertThrows(IllegalArgumentException.class, ()->x.getResponseInfo(null, "a", "a"));
			assertThrows(IllegalArgumentException.class, ()->x.getResponseInfo("a", null, "a"));
			assertThrows(IllegalArgumentException.class, ()->x.getResponseInfo("a", "a", null));
			assertThrows(IllegalArgumentException.class, ()->x.getParameterInfo(null, "a", "a", "a"));
			assertThrows(IllegalArgumentException.class, ()->x.getParameterInfo("a", null, "a", "a"));
			assertThrows(IllegalArgumentException.class, ()->x.getParameterInfo("a", "a", null, "a"));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Swagger> TESTER =
			testBean(bean())
			.props("basePath,host,swagger,info,tags,schemes,consumes,produces,paths,definitions,parameters,responses,securityDefinitions,security,externalDocs")
			.vals("<null>,<null>,2.0,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
			.json("{swagger:'2.0'}")
			.string("{\"swagger\":\"2.0\"}")
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
			assertList(TESTER.bean().keySet(), "swagger");
		}
	}

	@Nested class C_extraProperties extends TestBase {

		private static final BeanTester<Swagger> TESTER =
			testBean(
				bean()
					.setBasePath("a")
					.setHost("b")
					.setSwagger("c")
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("basePath,host,swagger,x1,x2")
			.vals("a,b,c,x1a,<null>")
			.json("{basePath:'a',host:'b',swagger:'c',x1:'x1a'}")
			.string("{'basePath':'a','host':'b','swagger':'c','x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "basePath", "host", "swagger", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"basePath,host,swagger,x1,x2",
				"a,b,c,x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"basePath,host,swagger,x1,x2",
				"String,String,String,String,<null>"
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
			// Call add methods twice - first call creates collection, second adds to existing
			var x = bean()
				.addConsumes(MediaType.of("a"))
				.addConsumes(MediaType.of("b"))
				.addDefinition("c1", JsonMap.of("type", "c2"))
				.addDefinition("d1", JsonMap.of("type", "d2"))
				.addParameter("e1", parameterInfo("e2", "e3"))
				.addParameter("f1", parameterInfo("f2", "f3"))
				.addPath("g1", "get", operation().setSummary("g2"))
				.addPath("h1", "post", operation().setSummary("h2"))
				.addProduces(MediaType.of("i"))
				.addProduces(MediaType.of("j"))
				.addResponse("k1", responseInfo().setDescription("k2"))
				.addResponse("l1", responseInfo().setDescription("l2"))
				.addSchemes("m")
				.addSchemes("n")
				.addSecurity("o1", "o2")
				.addSecurity("p1", "p2")
				.addSecurityDefinition("q1", securityScheme().setType("q2"))
				.addSecurityDefinition("r1", securityScheme().setType("r2"))
				.addTags(tag().setName("s"))
				.addTags(tag().setName("t"));

			assertBean(x,
				"consumes,definitions{c1{type},d1{type}},parameters{e1{in,name},f1{in,name}},paths{g1{get{summary}},h1{post{summary}}},produces,responses{k1{description},l1{description}},schemes,security{0{o1},1{p1}},securityDefinitions{q1{type},r1{type}},tags{0{name},1{name}}",
				"[a,b],{{c2},{d2}},{{e2,e3},{f2,f3}},{{{g2}},{{h2}}},[i,j],{{k2},{l2}},[m,n],{{[o2]},{[p2]}},{{q2},{r2}},{{s},{t}}"
			);
		}

		@Test void d02_asMap() {
			assertBean(
				bean()
					.setBasePath("a")
					.setHost("b")
					.set("x1", "x1a")
					.asMap(),
				"basePath,host,swagger,x1",
				"a,b,2.0,x1a"
			);
		}

		@Test void d03_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void d04_strict() {
			var x = bean();
			assertFalse(x.isStrict());
			x.strict();
			assertTrue(x.isStrict());
		}

		@Test void d05_addMethodsWithNullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, ()->x.addDefinition(null, JsonMap.of("a", "b")));
			assertThrows(IllegalArgumentException.class, ()->x.addDefinition("a", null));
			assertThrows(IllegalArgumentException.class, ()->x.addParameter(null, parameterInfo("a", "b")));
			assertThrows(IllegalArgumentException.class, ()->x.addParameter("a", null));
			assertThrows(IllegalArgumentException.class, ()->x.addPath(null, "get", operation()));
			assertThrows(IllegalArgumentException.class, ()->x.addPath("a", null, operation()));
			assertThrows(IllegalArgumentException.class, ()->x.addPath("a", "get", null));
			assertThrows(IllegalArgumentException.class, ()->x.addResponse(null, responseInfo()));
			assertThrows(IllegalArgumentException.class, ()->x.addResponse("a", null));
			assertThrows(IllegalArgumentException.class, ()->x.addSecurity(null, "a"));
			assertThrows(IllegalArgumentException.class, ()->x.addSecurityDefinition(null, securityScheme()));
			assertThrows(IllegalArgumentException.class, ()->x.addSecurityDefinition("a", null));
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

	@Nested class G_utilityMethods extends TestBase {

		@Test void g01_asJson() {
			var x = swagger().setHost("a");
			var json = x.asJson();
			assertTrue(json.contains("a"));
		}

		@Test void g02_findRef() {
			var x = swagger().addDefinition("a1", JsonMap.of("type", "a2"));
			assertBean(
				x.findRef("#/definitions/a1", JsonMap.class), 
				"type", 
				"a2"
			);

			assertNull(x.findRef("#/definitions/notfound", JsonMap.class));

			assertThrows(IllegalArgumentException.class, () -> x.findRef(null, JsonMap.class));
			assertThrows(IllegalArgumentException.class, () -> x.findRef("a", null));
			assertThrows(IllegalArgumentException.class, () -> x.findRef("", JsonMap.class));
			assertThrowsWithMessage(BasicRuntimeException.class, "Unsupported reference:  'invalid'", () -> x.findRef("invalid", JsonMap.class));
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static Swagger bean() {
		return swagger();
	}

}