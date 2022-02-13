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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Swagger}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Swagger_Test {

	/**
	 * Test method for {@link Swagger#swagger(java.lang.Object)}.
	 */
	@Test
	public void a01_swagger() {
		Swagger t = new Swagger();

		t.swagger("foo");
		assertString(t.swagger()).is("foo");

		t.swagger(null);
		assertString(t.swagger()).isNull();
	}

	/**
	 * Test method for {@link Swagger#info(java.lang.Object)}.
	 */
	@Test
	public void a02_info() {
		Swagger t = new Swagger();

		t.info(info("foo", "bar"));
		assertOptional(t.info()).asJson().is("{title:'foo',version:'bar'}");

		t.info("{title:'foo',version:'bar'}");
		assertOptional(t.info()).isType(Info.class).asJson().is("{title:'foo',version:'bar'}");

		t.info((String)null);
		assertOptional(t.info()).isNull();
	}

	/**
	 * Test method for {@link Swagger#host(java.lang.Object)}.
	 */
	@Test
	public void a03_host() {
		Swagger t = new Swagger();

		t.host("foo");
		assertString(t.host()).is("foo");

		t.host(null);
		assertString(t.host()).isNull();
	}

	/**
	 * Test method for {@link Swagger#basePath(java.lang.Object)}.
	 */
	@Test
	public void a04_basePath() {
		Swagger t = new Swagger();

		t.basePath("foo");
		assertString(t.basePath()).is("foo");

		t.basePath(null);
		assertString(t.basePath()).isNull();
	}

	/**
	 * Test method for {@link Swagger#setSchemes(java.util.Collection)}.
	 */
	@Test
	public void a05_schemes() {
		Swagger t = new Swagger();

		t.schemes(ASet.of("foo","bar"));
		assertOptional(t.schemes()).isType(Set.class).asJson().is("['foo','bar']");

		t.schemes(ASet.of());
		assertOptional(t.schemes()).isType(Set.class).asJson().is("[]");

		t.schemes((Collection<String>)null);
		assertOptional(t.schemes()).isNull();

		t.addSchemes(ASet.of("foo","bar"));
		assertOptional(t.schemes()).isType(Set.class).asJson().is("['foo','bar']");

		t.addSchemes(ASet.of());
		assertOptional(t.schemes()).isType(Set.class).asJson().is("['foo','bar']");

		t.addSchemes(null);
		assertOptional(t.schemes()).isType(Set.class).asJson().is("['foo','bar']");
	}

	/**
	 * Test method for {@link Swagger#setConsumes(java.util.Collection)}.
	 */
	@Test
	public void a06_consumes() {
		Swagger t = new Swagger();

		t.consumes(ASet.of(MediaType.of("text/foo")));
		assertOptional(t.consumes()).isType(Set.class).asJson().is("['text/foo']");

		t.consumes(ASet.of());
		assertOptional(t.consumes()).isType(Set.class).asJson().is("[]");

		t.consumes((Collection<MediaType>)null);
		assertOptional(t.consumes()).isNull();

		t.addConsumes(ASet.of(MediaType.of("text/foo")));
		assertOptional(t.consumes()).isType(Set.class).asJson().is("['text/foo']");

		t.addConsumes(ASet.of());
		assertOptional(t.consumes()).isType(Set.class).asJson().is("['text/foo']");

		t.addConsumes(null);
		assertOptional(t.consumes()).isType(Set.class).asJson().is("['text/foo']");
	}

	/**
	 * Test method for {@link Swagger#setProduces(java.util.Collection)}.
	 */
	@Test
	public void a07_produces() {
		Swagger t = new Swagger();

		t.produces(ASet.of(MediaType.of("text/foo")));
		assertOptional(t.produces()).isType(Set.class).asJson().is("['text/foo']");

		t.produces(ASet.of());
		assertOptional(t.produces()).isType(Set.class).asJson().is("[]");

		t.produces((Collection<MediaType>)null);
		assertOptional(t.produces()).isNull();

		t.addProduces(ASet.of(MediaType.of("text/foo")));
		assertOptional(t.produces()).isType(Set.class).asJson().is("['text/foo']");

		t.addProduces(ASet.of());
		assertOptional(t.produces()).isType(Set.class).asJson().is("['text/foo']");

		t.addProduces(null);
		assertOptional(t.produces()).isType(Set.class).asJson().is("['text/foo']");
	}

	/**
	 * Test method for {@link Swagger#setPaths(java.util.Map)}.
	 */
	@Test
	public void a08_paths() {
		Swagger t = new Swagger();

		t.paths(AMap.of("foo", new OperationMap().append("bar",operation().summary("baz"))));
		assertOptional(t.paths()).isType(Map.class).asJson().is("{foo:{bar:{summary:'baz'}}}");

		t.paths(AMap.create());
		assertOptional(t.paths()).isNull();

		t.paths((Map<String,OperationMap>)null);
		assertOptional(t.paths()).isNull();

		t.addPaths(AMap.of("foo",new OperationMap().append("bar",operation().summary("baz"))));
		assertOptional(t.paths()).isType(Map.class).asJson().is("{foo:{bar:{summary:'baz'}}}");

		t.addPaths(AMap.create());
		assertOptional(t.paths()).isType(Map.class).asJson().is("{foo:{bar:{summary:'baz'}}}");

		t.addPaths(null);
		assertOptional(t.paths()).isType(Map.class).asJson().is("{foo:{bar:{summary:'baz'}}}");

		t.setPaths(null);
		t.path("a", "a1", operation().description("a2"));
		t.path("b", null, null);

		assertOptional(t.paths()).asJson().is("{a:{a1:{description:'a2'}},b:{null:null}}");
	}

	/**
	 * Test method for {@link Swagger#setDefinitions(java.util.Map)}.
	 */
	@Test
	public void a09_definitions() {
		Swagger t = new Swagger();

		t.definitions(AMap.of("foo",OMap.of("type","bar")));
		assertOptional(t.definitions()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");

		t.definitions(AMap.create());
		assertOptional(t.definitions()).isType(Map.class).asJson().is("{}");

		t.definitions((Map<String,OMap>)null);
		assertOptional(t.definitions()).isNull();

		t.addDefinitions(AMap.of("foo",OMap.of("type", "bar")));
		assertOptional(t.definitions()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");

		t.addDefinitions(AMap.create());
		assertOptional(t.definitions()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");

		t.addDefinitions(null);
		assertOptional(t.definitions()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");

		t.setDefinitions(null);
		t.definition("a", OMap.of("type","a1"));
		t.definition("b", (OMap)null);
		t.definition(null, OMap.of("type", "c1"));

		assertOptional(t.definitions()).asJson().is("{a:{type:'a1'},b:null,null:{type:'c1'}}");
	}

	/**
	 * Test method for {@link Swagger#setParameters(java.util.Map)}.
	 */
	@Test
	public void a10_parameters() {
		Swagger t = new Swagger();

		t.parameters(AMap.of("foo",parameterInfo().name("bar")));
		assertOptional(t.parameters()).isType(Map.class).asJson().is("{foo:{name:'bar'}}");

		t.parameters(AMap.create());
		assertOptional(t.parameters()).isType(Map.class).asJson().is("{}");

		t.parameters((Map<String,ParameterInfo>)null);
		assertOptional(t.parameters()).isNull();

		t.addParameters(AMap.of("foo",parameterInfo().name("bar")));
		assertOptional(t.parameters()).isType(Map.class).asJson().is("{foo:{name:'bar'}}");

		t.addParameters(AMap.create());
		assertOptional(t.parameters()).isType(Map.class).asJson().is("{foo:{name:'bar'}}");

		t.addParameters(null);
		assertOptional(t.parameters()).isType(Map.class).asJson().is("{foo:{name:'bar'}}");

		t.setParameters(null);
		t.parameter("a", parameterInfo().in("a1"));
		t.parameter("b", null);
		t.parameter(null, parameterInfo().in("c1"));

		assertOptional(t.parameters()).asJson().is("{a:{'in':'a1'},b:null,null:{'in':'c1'}}");
	}

	/**
	 * Test method for {@link Swagger#setResponses(java.util.Map)}.
	 */
	@Test
	public void a11_responses() {
		Swagger t = new Swagger();

		t.responses(AMap.of("123",responseInfo("bar")));
		assertOptional(t.responses()).isType(Map.class).asJson().is("{'123':{description:'bar'}}");

		t.responses(AMap.create());
		assertOptional(t.responses()).isType(Map.class).asJson().is("{}");

		t.responses((Map<String,ResponseInfo>)null);
		assertOptional(t.responses()).isNull();

		t.addResponses(AMap.of("123",responseInfo("bar")));
		assertOptional(t.responses()).isType(Map.class).asJson().is("{'123':{description:'bar'}}");

		t.addResponses(AMap.create());
		assertOptional(t.responses()).isType(Map.class).asJson().is("{'123':{description:'bar'}}");

		t.addResponses(null);
		assertOptional(t.responses()).isType(Map.class).asJson().is("{'123':{description:'bar'}}");

		t.setResponses(null);
		t.response("a", responseInfo("a1"));
		t.response(null, responseInfo("b1"));
		t.response("c", null);

		assertOptional(t.responses()).asJson().is("{a:{description:'a1'},null:{description:'b1'},c:null}");
	}

	/**
	 * Test method for {@link Swagger#setSecurityDefinitions(java.util.Map)}.
	 */
	@Test
	public void a12_securityDefinitions() {
		Swagger t = new Swagger();

		t.securityDefinitions(AMap.of("foo",securityScheme("bar")));
		assertOptional(t.securityDefinitions()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");

		t.securityDefinitions(AMap.create());
		assertOptional(t.securityDefinitions()).isType(Map.class).asJson().is("{}");

		t.securityDefinitions((Map<String,SecurityScheme>)null);
		assertOptional(t.securityDefinitions()).isNull();

		t.addSecurityDefinitions(AMap.of("foo",securityScheme("bar")));
		assertOptional(t.securityDefinitions()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");

		t.addSecurityDefinitions(AMap.create());
		assertOptional(t.securityDefinitions()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");

		t.addSecurityDefinitions(null);
		assertOptional(t.securityDefinitions()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");

		t.setSecurityDefinitions(null);
		t.securityDefinition("a", securityScheme("a1"));
		t.securityDefinition("b", null);
		t.securityDefinition(null, securityScheme("c1"));

		assertOptional(t.securityDefinitions()).asJson().is("{a:{type:'a1'},b:null,null:{type:'c1'}}");
	}

	/**
	 * Test method for {@link Swagger#setSecurity(java.util.Collection)}.
	 */
	@Test
	public void a13_security() {
		Swagger t = new Swagger();

		t.security(ASet.of(AMap.of("foo",AList.of("bar"))));
		assertOptional(t.security()).isType(List.class).asJson().is("[{foo:['bar']}]");

		t.security(ASet.of());
		assertOptional(t.security()).isType(List.class).asJson().is("[]");

		t.security((Collection<Map<String, List<String>>>)null);
		assertOptional(t.security()).isNull();

		t.addSecurity(ASet.of(AMap.of("foo",AList.of("bar"))));
		assertOptional(t.security()).isType(List.class).asJson().is("[{foo:['bar']}]");

		t.addSecurity(ASet.of());
		assertOptional(t.security()).isType(List.class).asJson().is("[{foo:['bar']}]");

		t.addSecurity(null);
		assertOptional(t.security()).isType(List.class).asJson().is("[{foo:['bar']}]");

		t.setSecurity(null);
		t.security("a", "a1", "a2");
		t.security("b", (String)null);
		t.security(null, "d1", "d2");

		assertOptional(t.security()).asJson().is("[{a:['a1','a2']},{b:[null]},{null:['d1','d2']}]");
	}

	/**
	 * Test method for {@link Swagger#setTags(java.util.Collection)}.
	 */
	@Test
	public void a14_tags() {
		Swagger t = new Swagger();

		t.tags(ASet.of(tag("foo")));
		assertOptional(t.tags()).isType(Set.class).asJson().is("[{name:'foo'}]");

		t.tags(ASet.of());
		assertOptional(t.tags()).isType(Set.class).asJson().is("[]");

		t.tags((Collection<Tag>)null);
		assertOptional(t.tags()).isNull();

		t.addTags(ASet.of(tag("foo")));
		assertOptional(t.tags()).isType(Set.class).asJson().is("[{name:'foo'}]");

		t.addTags(ASet.of());
		assertOptional(t.tags()).isType(Set.class).asJson().is("[{name:'foo'}]");

		t.addTags(null);
		assertOptional(t.tags()).isType(Set.class).asJson().is("[{name:'foo'}]");
	}

	/**
	 * Test method for {@link Swagger#externalDocs(java.lang.Object)}.
	 */
	@Test
	public void a15_externalDocs() {
		Swagger t = new Swagger();

		t.externalDocs(externalDocumentation("foo"));
		assertOptional(t.externalDocs()).asJson().is("{url:'foo'}");

		t.externalDocs("{url:'foo'}");
		assertOptional(t.externalDocs()).isType(ExternalDocumentation.class).asJson().is("{url:'foo'}");

		t.externalDocs((String)null);
		assertOptional(t.externalDocs()).isNull();
	}

	/**
	 * Test method for {@link Swagger#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		Swagger t = new Swagger();

		t
			.set("basePath", "a")
			.set("consumes", ASet.of(MediaType.of("text/b")))
			.set("definitions", AMap.of("c",schemaInfo().type("c1")))
			.set("externalDocs", externalDocumentation("d"))
			.set("host", "e")
			.set("info", info("f1", "f2"))
			.set("parameters", AMap.of("g",parameterInfo("g1", "g2")))
			.set("paths", AMap.of("h",AMap.of("h1",operation().operationId("h2"))))
			.set("produces", ASet.of(MediaType.of("text/i")))
			.set("responses", AMap.of("j",responseInfo("j1")))
			.set("schemes", ASet.of("k1"))
			.set("security", ASet.of(AMap.of("l1",AList.of("l2"))))
			.set("securityDefinitions", AMap.of("m",securityScheme("m1")))
			.set("swagger", "n")
			.set("tags", ASet.of(tag("o")))
			.set("$ref", "ref");

		assertObject(t).asJson().is("{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");

		t
			.set("basePath", "a")
			.set("consumes", "['text/b']")
			.set("definitions", "{c:{type:'c1'}}")
			.set("externalDocs", "{url:'d'}")
			.set("host", "e")
			.set("info", "{title:'f1',version:'f2'}")
			.set("parameters", "{g:{'in':'g1',name:'g2'}}")
			.set("paths", "{h:{h1:{operationId:'h2'}}}")
			.set("produces", "['text/i']")
			.set("responses", "{j:{description:'j1'}}")
			.set("schemes", "['k1']")
			.set("security", "[{l1:['l2']}]")
			.set("securityDefinitions", "{m:{type:'m1'}}")
			.set("swagger", "n")
			.set("tags", "[{name:'o'}]")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");

		t
			.set("basePath", new StringBuilder("a"))
			.set("consumes", new StringBuilder("['text/b']"))
			.set("definitions", new StringBuilder("{c:{type:'c1'}}"))
			.set("externalDocs", new StringBuilder("{url:'d'}"))
			.set("host", new StringBuilder("e"))
			.set("info", new StringBuilder("{title:'f1',version:'f2'}"))
			.set("parameters", new StringBuilder("{g:{'in':'g1',name:'g2'}}"))
			.set("paths", new StringBuilder("{h:{h1:{operationId:'h2'}}}"))
			.set("produces", new StringBuilder("['text/i']"))
			.set("responses", new StringBuilder("{j:{description:'j1'}}"))
			.set("schemes", new StringBuilder("['k1']"))
			.set("security", new StringBuilder("[{l1:['l2']}]"))
			.set("securityDefinitions", new StringBuilder("{m:{type:'m1'}}"))
			.set("swagger", new StringBuilder("n"))
			.set("tags", new StringBuilder("[{name:'o'}]"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).asJson().is("{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");

		assertString(t.get("basePath", String.class)).is("a");
		assertString(t.get("consumes", String.class)).is("['text/b']");
		assertString(t.get("definitions", String.class)).is("{c:{type:'c1'}}");
		assertString(t.get("externalDocs", String.class)).is("{url:'d'}");
		assertString(t.get("host", String.class)).is("e");
		assertString(t.get("info", String.class)).is("{title:'f1',version:'f2'}");
		assertString(t.get("parameters", String.class)).is("{g:{'in':'g1',name:'g2'}}");
		assertString(t.get("paths", String.class)).is("{h:{h1:{operationId:'h2'}}}");
		assertString(t.get("produces", String.class)).is("['text/i']");
		assertString(t.get("responses", String.class)).is("{j:{description:'j1'}}");
		assertString(t.get("schemes", String.class)).is("['k1']");
		assertString(t.get("security", String.class)).is("[{l1:['l2']}]");
		assertString(t.get("securityDefinitions", String.class)).is("{m:{type:'m1'}}");
		assertString(t.get("swagger", String.class)).is("n");
		assertString(t.get("tags", String.class)).is("[{name:'o'}]");
		assertString(t.get("$ref", String.class)).is("ref");

		assertObject(t.get("basePath", Object.class)).isType(String.class);
		assertObject(t.get("consumes", Object.class)).isType(Set.class);
		assertObject(t.get("consumes", List.class).get(0)).isType(MediaType.class);
		assertObject(t.get("definitions", Object.class)).isType(Map.class);
		assertObject(t.get("definitions", Map.class).values().iterator().next()).isType(OMap.class);
		assertObject(t.get("externalDocs", Object.class)).isType(ExternalDocumentation.class);
		assertObject(t.get("host", Object.class)).isType(String.class);
		assertObject(t.get("info", Object.class)).isType(Info.class);
		assertObject(t.get("parameters", Object.class)).isType(Map.class);
		assertObject(t.get("parameters", Map.class).values().iterator().next()).isType(ParameterInfo.class);
		assertObject(t.get("paths", Object.class)).isType(Map.class);
		assertObject(t.get("produces", Object.class)).isType(Set.class);
		assertObject(t.get("consumes", List.class).get(0)).isType(MediaType.class);
		assertObject(t.get("responses", Object.class)).isType(Map.class);
		assertObject(t.get("responses", Map.class).values().iterator().next()).isType(ResponseInfo.class);
		assertObject(t.get("schemes", Object.class)).isType(Set.class);
		assertObject(t.get("security", Object.class)).isType(List.class);
		assertObject(t.get("securityDefinitions", Object.class)).isType(Map.class);
		assertObject(t.get("securityDefinitions", Map.class).values().iterator().next()).isType(SecurityScheme.class);
		assertObject(t.get("swagger", Object.class)).isType(String.class);
		assertObject(t.get("tags", Object.class)).isType(Set.class);
		assertObject(t.get("tags", List.class).get(0)).isType(Tag.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, Swagger.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
		Swagger t = new Swagger();

		t = t.copy();

		assertObject(t).asJson().is("{swagger:'2.0'}");

		t
			.set("basePath", "a")
			.set("consumes", ASet.of(MediaType.of("text/b")))
			.set("definitions", AMap.of("c",schemaInfo().type("c1")))
			.set("externalDocs", externalDocumentation("d"))
			.set("host", "e")
			.set("info", info("f1", "f2"))
			.set("parameters", AMap.of("g",parameterInfo("g1", "g2")))
			.set("paths", AMap.of("h",AMap.of("h1",operation().operationId("h2"))))
			.set("produces", ASet.of(MediaType.of("text/i")))
			.set("responses", AMap.of("j",responseInfo("j1")))
			.set("schemes", ASet.of("k1"))
			.set("security", ASet.of(AMap.of("l1",AList.of("l2"))))
			.set("securityDefinitions", AMap.of("m",securityScheme("m1")))
			.set("swagger", "n")
			.set("tags", ASet.of(tag("o")))
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		Swagger t = new Swagger();

		assertObject(t.keySet()).asJson().is("['swagger']");

		t
			.set("basePath", "a")
			.set("consumes", ASet.of(MediaType.of("text/b")))
			.set("definitions", AMap.of("c",schemaInfo().type("c1")))
			.set("externalDocs", externalDocumentation("d"))
			.set("host", "e")
			.set("info", info("f1", "f2"))
			.set("parameters", AMap.of("g",parameterInfo("g1", "g2")))
			.set("paths", AMap.of("h",AMap.of("h1",operation().operationId("h2"))))
			.set("produces", ASet.of(MediaType.of("text/i")))
			.set("responses", AMap.of("j",responseInfo("j1")))
			.set("schemes", ASet.of("k1"))
			.set("security", ASet.of(AMap.of("l1",AList.of("l2"))))
			.set("securityDefinitions", AMap.of("m",securityScheme("m1")))
			.set("swagger", "n")
			.set("tags", ASet.of(tag("o")))
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is(
			"['basePath','consumes','definitions','externalDocs','host','info','parameters','paths','produces','responses',"
			+ "'schemes','security','securityDefinitions','swagger','tags','$ref']"
		);
	}
}
