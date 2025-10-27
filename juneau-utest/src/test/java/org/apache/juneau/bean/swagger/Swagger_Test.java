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
					.setTags(tag().setName("o"))
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
			// Test special getters
			var x = bean()
				.addPath("a1", "get", operation().setSummary("a2"))
				.addPath("b1", "get", operation().addResponse("200", responseInfo("b2")).setParameters(parameterInfo("b3", "b4")))
				.addParameter("c1", parameterInfo("c2", "c3"));

			assertBean(x.getPath("a1"), "get{summary}", "{a2}");
			assertBean(x.getOperation("a1", "get"), "summary", "a2");
			assertBean(x.getResponseInfo("b1", "get", "200"), "description", "b2");
			assertBean(x.getResponseInfo("b1", "get", 200), "description", "b2");
			assertBean(x.getParameterInfo("b1", "get", "b3", "b4"), "in,name", "b3,b4");

			// Test varargs variants of setters
			x = bean()
				.setConsumes(MediaType.of("e1"), MediaType.of("e2"))
				.setProduces(MediaType.of("f1"), MediaType.of("f2"))
				.setSchemes("g1", "g2");

			assertBean(x, "consumes,produces,schemes", "[e1,e2],[f1,f2],[g1,g2]");
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
			assertThrows(IllegalArgumentException.class, ()->x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, ()->x.set(null, "value"));
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

		@Test void a10_collectionSetters() {
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
				.setTags(list(tag().setName("d1"), tag().setName("d2")))
				.setSecurity(list(
					map("e1", list("e2")),
					map("e3", list("e4"))
				));

			assertBean(x,
				"consumes,produces,schemes,tags{0{name},1{name}},security{0{e1},1{e3}}",
				"[a1,a2],[b1,b2],[c1,c2],{{d1},{d2}},{{[e2]},{[e4]}}"
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
				.addTags(tag().setName("d1"))
				.addTags(tag().setName("d2"));

			assertBean(x,
				"consumes,produces,schemes,tags{0{name},1{name}}",
				"[a1,a2],[b1,b2],[c1,c2],{{d1},{d2}}"
			);
		}

		@Test void a12_collectionAdders() {
			// Test Collection addX methods - call each method twice
			var x = bean()
				.addConsumes(list(MediaType.of("a1")))
				.addConsumes(list(MediaType.of("a2")))
				.addProduces(list(MediaType.of("b1")))
				.addProduces(list(MediaType.of("b2")))
				.addSchemes(list("c1"))
				.addSchemes(list("c2"))
				.addSecurity(list(map("d1",list("d2"))))
				.addSecurity(list(map("d3",list("d4"))))
				.addTags(list(tag().setName("e1")))
				.addTags(list(tag().setName("e2")));

			assertBean(x,
				"consumes,produces,schemes,security,tags{0{name},1{name}}",
				"[a1,a2],[b1,b2],[c1,c2],[{d1=[d2]},{d3=[d4]}],{{e1},{e2}}"
			);
		}

		@Test void a13_asMap() {
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

		@Test void a14_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void a15_addResponse() {
			// Test addResponse method
			var x = bean()
				.addResponse("200", responseInfo().setDescription("a"))
				.addResponse("404", responseInfo().setDescription("b"));

			assertBean(x,
				"responses{200{description},404{description}}",
				"{{a},{b}}"
			);
		}

		@Test void a16_addSecurity() {
			// Test addSecurity method
			var x = bean()
				.addSecurity("scheme1", "a", "b")
				.addSecurity("scheme2", "c");

			assertBean(x,
				"security{0{scheme1},1{scheme2}}",
				"{{[a,b]},{[c]}}"
			);
		}

		@Test void a17_addSecurityDefinition() {
			// Test addSecurityDefinition method
			var x = bean()
				.addSecurityDefinition("def1", securityScheme().setType("a"))
				.addSecurityDefinition("def2", securityScheme().setType("b"));

			assertBean(x,
				"securityDefinitions{def1{type},def2{type}}",
				"{{a},{b}}"
			);
		}

		@Test void a18_getOperationNullPath() {
			var a = swagger()
				.addPath("/existing", "get", operation().setSummary("test"));

			// Test getOperation when path doesn't exist (returns null)
			assertNull(a.getOperation("/nonexistent", "get"));

			// Test getResponseInfo when path doesn't exist (returns null)
			assertNull(a.getResponseInfo("/nonexistent", "get", "200"));

			// Test getResponseInfo when operation doesn't exist (returns null)
			assertNull(a.getResponseInfo("/test", "post", "200"));

			// Test getParameterInfo when path doesn't exist (returns null)
			assertNull(a.getParameterInfo("/nonexistent", "get", "query", "param"));

			// Test getParameterInfo when operation doesn't exist (returns null)
			assertNull(a.getParameterInfo("/test", "post", "query", "param"));
		}

		@Test void a19_getMethodWithInvalidProperty() {
			var a = swagger();

			// Test get method with invalid property (should call super.get)
			assertNull(a.get("invalidProperty", String.class));
		}

		@Test void a20_strictMode() {
			assertThrows(RuntimeException.class, () -> bean().strict().set("foo", "bar"));
			assertDoesNotThrow(() -> bean().set("foo", "bar"));

			assertFalse(bean().isStrict());
			assertTrue(bean().strict().isStrict());
			assertFalse(bean().strict(false).isStrict());
		}

		@Test void a21_asJson() {
			var x = swagger().setHost("a");
			var json = x.asJson();
			assertTrue(json.contains("a"));
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
			assertList(TESTER.bean().setSwagger(null).keySet());
		}
	}

	@Nested class C_extraProperties extends TestBase {

		private static final BeanTester<Swagger> TESTER =
			testBean(
				bean()
					.set("basePath", "a")
					.set("consumes", list(MediaType.of("b")))
					.set("definitions", map("c1", JsonMap.of("type", "c2")))
					.set("externalDocs", externalDocumentation("d"))
					.set("host", "e")
					.set("info", info("f1", "f2"))
					.set("parameters", map("g1", parameterInfo("g2", "g3")))
					.set("paths", map("h1", operationMap().append("get", operation().setSummary("h2"))))
					.set("produces", list(MediaType.of("i")))
					.set("responses", map("j1", responseInfo().setDescription("j2")))
					.set("schemes", list("k"))
					.set("security", list(map("l1", list("l2"))))
					.set("securityDefinitions", map("m1", securityScheme().setType("m2")))
					.set("swagger", "n")
					.set("tags", list(tag().setName("o")))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("basePath,consumes,definitions{c1{type}},externalDocs{url},host,info{title,version},parameters{g1{in,name}},paths{h1{get{summary}}},produces,responses{j1{description}},schemes,security{0{l1}},securityDefinitions{m1{type}},swagger,tags{0{name}},x1,x2")
			.vals("a,[b],{{c2}},{d},e,{f1,f2},{{g2,g3}},{{{h2}}},[i],{{j2}},[k],{{[l2]}},{{m2}},n,{{o}},x1a,<null>")
			.json("{basePath:'a',consumes:['b'],definitions:{c1:{type:'c2'}},externalDocs:{url:'d'},host:'e',info:{title:'f1',version:'f2'},parameters:{g1:{'in':'g2',name:'g3'}},paths:{h1:{get:{summary:'h2'}}},produces:['i'],responses:{j1:{description:'j2'}},schemes:['k'],security:[{l1:['l2']}],securityDefinitions:{m1:{type:'m2'}},swagger:'n',tags:[{name:'o'}],x1:'x1a'}")
			.string("{'basePath':'a','consumes':['b'],'definitions':{'c1':{'type':'c2'}},'externalDocs':{'url':'d'},'host':'e','info':{'title':'f1','version':'f2'},'parameters':{'g1':{'in':'g2','name':'g3'}},'paths':{'h1':{'get':{'summary':'h2'}}},'produces':['i'],'responses':{'j1':{'description':'j2'}},'schemes':['k'],'security':[{'l1':['l2']}],'securityDefinitions':{'m1':{'type':'m2'}},'swagger':'n','tags':[{'name':'o'}],'x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "basePath", "consumes", "definitions", "externalDocs", "host", "info", "parameters", "paths", "produces", "responses", "schemes", "security", "securityDefinitions", "swagger", "tags", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"basePath,consumes,definitions{c1{type}},externalDocs{url},host,info{title,version},parameters{g1{in,name}},paths{h1{get{summary}}},produces,responses{j1{description}},schemes,security{0{l1}},securityDefinitions{m1{type}},swagger,tags{0{name}},x1,x2",
				"a,[b],{{c2}},{d},e,{f1,f2},{{g2,g3}},{{{h2}}},[i],{{j2}},[k],{{[l2]}},{{m2}},n,{{o}},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> scn(obj.get(prop, Object.class)),
				"basePath,consumes,definitions,externalDocs,host,info,parameters,paths,produces,responses,schemes,security,securityDefinitions,swagger,tags,x1,x2",
				"String,LinkedHashSet,LinkedHashMap,ExternalDocumentation,String,Info,LinkedHashMap,TreeMap,LinkedHashSet,LinkedHashMap,LinkedHashSet,ArrayList,LinkedHashMap,String,LinkedHashSet,String,<null>"
			);
		}

		@Test void c10_nullPropertyValue() {
			assertThrows(IllegalArgumentException.class, ()->bean().get(null));
			assertThrows(IllegalArgumentException.class, ()->bean().get(null, String.class));
			assertThrows(IllegalArgumentException.class, ()->bean().set(null, "a"));
		}
	}

	@Nested class D_refs extends TestBase {

		@Test void d01_findRef() {
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

		@Test void d02_findRefInvalidType() {
			// Test findRef with invalid type - should throw exception
			var x = swagger()
				.addDefinition("Pet", JsonMap.of("type", "object"))
				.addResponse("Error", responseInfo().setDescription("Error response"))
				.addParameter("petId", parameterInfo("path", "id"));

			// Test trying to parse a definition into an invalid type
			assertThrows(Exception.class, () -> x.findRef("#/definitions/Pet", Integer.class));

			// Test trying to parse a response into an invalid type
			assertThrows(Exception.class, () -> x.findRef("#/responses/Error", Integer.class));

			// Test trying to parse a parameter into an invalid type
			assertThrows(Exception.class, () -> x.findRef("#/parameters/petId", Integer.class));
		}

	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static Swagger bean() {
		return swagger();
	}
}