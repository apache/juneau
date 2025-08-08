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

import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.bean.swagger.Tag;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Swagger}.
 */
class Swagger_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		Swagger t = new Swagger();
		assertEquals("foo", t.setSwagger("foo").getSwagger());
		assertNull(t.setSwagger(null).getSwagger());
		assertJson(t.setInfo(info("foo", "bar")).getInfo(), "{title:'foo',version:'bar'}");
		assertEquals("foo", t.setHost("foo").getHost());
		assertNull(t.setHost(null).getHost());
		assertEquals("foo", t.setBasePath("foo").getBasePath());
		assertNull(t.setBasePath(null).getBasePath());
		assertJson(t.setSchemes(set("foo","bar")).getSchemes(), "['foo','bar']");
		assertJson(t.setSchemes(set()).getSchemes(), "[]");
		assertNull(t.setSchemes((Collection<String>)null).getSchemes());
		assertJson(t.setConsumes(set(MediaType.of("text/foo"))).getConsumes(), "['text/foo']");
		assertJson(t.setConsumes(set()).getConsumes(), "[]");
		assertNull(t.setConsumes((Collection<MediaType>)null).getConsumes());
		assertJson(t.setProduces(set(MediaType.of("text/foo"))).getProduces(), "['text/foo']");
		assertJson(t.setProduces(set()).getProduces(), "[]");
		assertNull(t.setProduces((Collection<MediaType>)null).getProduces());
		assertJson(t.setPaths(map("foo", new OperationMap().append("bar",operation().setSummary("baz")))).getPaths(), "{foo:{bar:{summary:'baz'}}}");
		assertNull(t.setPaths(map()).getPaths());
		assertNull(t.setPaths((Map<String,OperationMap>)null).getPaths());
		assertJson(t.setPaths(null).addPath("a", "a1", operation().setDescription("a2")).addPath("b", null, null).getPaths(), "{a:{a1:{description:'a2'}},b:{null:null}}");
		assertJson(t.setDefinitions(map("foo",JsonMap.of("type","bar"))).getDefinitions(), "{foo:{type:'bar'}}");
		assertJson(t.setDefinitions(map()).getDefinitions(), "{}");
		assertNull(t.setDefinitions((Map<String,JsonMap>)null).getDefinitions());
		assertJson(t.setDefinitions(null).addDefinition("a", JsonMap.of("type","a1")).addDefinition("b", (JsonMap)null).addDefinition(null, JsonMap.of("type", "c1")).getDefinitions(), "{a:{type:'a1'},b:null,null:{type:'c1'}}");
		assertJson(t.setParameters(map("foo",parameterInfo().setName("bar"))).getParameters(), "{foo:{name:'bar'}}");
		assertJson(t.setParameters(map()).getParameters(), "{}");
		assertNull(t.setParameters((Map<String,ParameterInfo>)null).getParameters());
		assertJson(t.setParameters(null).addParameter("a", parameterInfo().setIn("a1")).addParameter("b", null).addParameter(null, parameterInfo().setIn("c1")).getParameters(), "{a:{'in':'a1'},b:null,null:{'in':'c1'}}");
		assertJson(t.setResponses(map("123",responseInfo("bar"))).getResponses(), "{'123':{description:'bar'}}");
		assertJson(t.setResponses(map()).getResponses(), "{}");
		assertNull(t.setResponses((Map<String,ResponseInfo>)null).getResponses());
		assertJson(t.setResponses(null).addResponse("a", responseInfo("a1")).addResponse(null, responseInfo("b1")).addResponse("c", null).getResponses(), "{a:{description:'a1'},null:{description:'b1'},c:null}");
		assertJson(t.setSecurityDefinitions(map("foo",securityScheme("bar"))).getSecurityDefinitions(), "{foo:{type:'bar'}}");
		assertJson(t.setSecurityDefinitions(map()).getSecurityDefinitions(), "{}");
		assertNull(t.setSecurityDefinitions((Map<String,SecurityScheme>)null).getSecurityDefinitions());
		assertJson(t.setSecurityDefinitions(null).addSecurityDefinition("a", securityScheme("a1")).addSecurityDefinition("b", null).addSecurityDefinition(null, securityScheme("c1")).getSecurityDefinitions(), "{a:{type:'a1'},b:null,null:{type:'c1'}}");
		assertJson(t.setSecurity(set(map("foo",alist("bar")))).getSecurity(), "[{foo:['bar']}]");
		assertJson(t.setSecurity(set()).getSecurity(), "[]");
		assertNull(t.setSecurity((Collection<Map<String, List<String>>>)null).getSecurity());
		assertJson(t.setSecurity(null).addSecurity("a", "a1", "a2").addSecurity("b", (String)null).addSecurity(null, "d1", "d2").getSecurity(), "[{a:['a1','a2']},{b:[null]},{null:['d1','d2']}]");
		assertJson(t.setTags(set(tag("foo"))).getTags(), "[{name:'foo'}]");
		assertJson(t.setTags(set()).getTags(), "[]");
		assertNull(t.setTags((Collection<Tag>)null).getTags());
		assertJson(t.setExternalDocs(externalDocumentation("foo")).getExternalDocs(), "{url:'foo'}");
	}

	/**
	 * Test method for {@link Swagger#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		Swagger t = new Swagger();

		t
			.set("basePath", "a")
			.set("consumes", set(MediaType.of("text/b")))
			.set("definitions", map("c",schemaInfo().setType("c1")))
			.set("externalDocs", externalDocumentation("d"))
			.set("host", "e")
			.set("info", info("f1", "f2"))
			.set("parameters", map("g",parameterInfo("g1", "g2")))
			.set("paths", map("h",map("h1",operation().setOperationId("h2"))))
			.set("produces", set(MediaType.of("text/i")))
			.set("responses", map("j",responseInfo("j1")))
			.set("schemes", set("k1"))
			.set("security", set(map("l1",alist("l2"))))
			.set("securityDefinitions", map("m",securityScheme("m1")))
			.set("swagger", "n")
			.set("tags", set(tag("o")))
			.set("$ref", "ref");

		assertJson(t, "{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");

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

		assertJson(t, "{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");

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

		assertJson(t, "{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");

		assertEquals("a", t.get("basePath", String.class));
		assertEquals("['text/b']", t.get("consumes", String.class));
		assertEquals("{c:{type:'c1'}}", t.get("definitions", String.class));
		assertEquals("{url:'d'}", t.get("externalDocs", String.class));
		assertEquals("e", t.get("host", String.class));
		assertEquals("{title:'f1',version:'f2'}", t.get("info", String.class));
		assertEquals("{g:{'in':'g1',name:'g2'}}", t.get("parameters", String.class));
		assertEquals("{h:{h1:{operationId:'h2'}}}", t.get("paths", String.class));
		assertEquals("['text/i']", t.get("produces", String.class));
		assertEquals("{j:{description:'j1'}}", t.get("responses", String.class));
		assertEquals("['k1']", t.get("schemes", String.class));
		assertEquals("[{l1:['l2']}]", t.get("security", String.class));
		assertEquals("{m:{type:'m1'}}", t.get("securityDefinitions", String.class));
		assertEquals("n", t.get("swagger", String.class));
		assertEquals("[{name:'o'}]", t.get("tags", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertType(String.class, t.get("basePath", Object.class));
		assertType(Set.class, t.get("consumes", Object.class));
		assertType(MediaType.class, t.get("consumes", List.class).get(0));
		assertType(Map.class, t.get("definitions", Object.class));
		assertType(JsonMap.class, t.get("definitions", Map.class).values().iterator().next());
		assertType(ExternalDocumentation.class, t.get("externalDocs", Object.class));
		assertType(String.class, t.get("host", Object.class));
		assertType(Info.class, t.get("info", Object.class));
		assertType(Map.class, t.get("parameters", Object.class));
		assertType(ParameterInfo.class, t.get("parameters", Map.class).values().iterator().next());
		assertType(Map.class, t.get("paths", Object.class));
		assertType(Set.class, t.get("produces", Object.class));
		assertType(MediaType.class, t.get("consumes", List.class).get(0));
		assertType(Map.class, t.get("responses", Object.class));
		assertType(ResponseInfo.class, t.get("responses", Map.class).values().iterator().next());
		assertType(Set.class, t.get("schemes", Object.class));
		assertType(List.class, t.get("security", Object.class));
		assertType(Map.class, t.get("securityDefinitions", Object.class));
		assertType(SecurityScheme.class, t.get("securityDefinitions", Map.class).values().iterator().next());
		assertType(String.class, t.get("swagger", Object.class));
		assertType(Set.class, t.get("tags", Object.class));
		assertType(Tag.class, t.get("tags", List.class).get(0));
		assertType(StringBuilder.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, Swagger.class), s);
	}

	@Test void b02_copy() {
		Swagger t = new Swagger();

		t = t.copy();

		assertJson(t, "{swagger:'2.0'}");

		t
			.set("basePath", "a")
			.set("consumes", set(MediaType.of("text/b")))
			.set("definitions", map("c",schemaInfo().setType("c1")))
			.set("externalDocs", externalDocumentation("d"))
			.set("host", "e")
			.set("info", info("f1", "f2"))
			.set("parameters", map("g",parameterInfo("g1", "g2")))
			.set("paths", map("h",map("h1",operation().setOperationId("h2"))))
			.set("produces", set(MediaType.of("text/i")))
			.set("responses", map("j",responseInfo("j1")))
			.set("schemes", set("k1"))
			.set("security", set(map("l1",alist("l2"))))
			.set("securityDefinitions", map("m",securityScheme("m1")))
			.set("swagger", "n")
			.set("tags", set(tag("o")))
			.set("$ref", "ref")
			.copy();

		assertJson(t, "{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");
	}

	@Test void b03_keySet() {
		Swagger t = new Swagger();

		assertJson(t.keySet(), "['swagger']");

		t
			.set("basePath", "a")
			.set("consumes", set(MediaType.of("text/b")))
			.set("definitions", map("c",schemaInfo().setType("c1")))
			.set("externalDocs", externalDocumentation("d"))
			.set("host", "e")
			.set("info", info("f1", "f2"))
			.set("parameters", map("g",parameterInfo("g1", "g2")))
			.set("paths", map("h",map("h1",operation().setOperationId("h2"))))
			.set("produces", set(MediaType.of("text/i")))
			.set("responses", map("j",responseInfo("j1")))
			.set("schemes", set("k1"))
			.set("security", set(map("l1",alist("l2"))))
			.set("securityDefinitions", map("m",securityScheme("m1")))
			.set("swagger", "n")
			.set("tags", set(tag("o")))
			.set("$ref", "ref");

		assertJson(t.keySet(), 
			"['basePath','consumes','definitions','externalDocs','host','info','parameters','paths','produces','responses',"
			+ "'schemes','security','securityDefinitions','swagger','tags','$ref']"
		);
	}
}