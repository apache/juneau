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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Swagger}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Swagger_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		Swagger t = new Swagger();
		assertString(t.setSwagger("foo").getSwagger()).is("foo");
		assertString(t.setSwagger(null).getSwagger()).isNull();
		assertObject(t.setInfo(info("foo", "bar")).getInfo()).asJson().is("{title:'foo',version:'bar'}");
		assertString(t.setHost("foo").getHost()).is("foo");
		assertString(t.setHost(null).getHost()).isNull();
		assertString(t.setBasePath("foo").getBasePath()).is("foo");
		assertString(t.setBasePath(null).getBasePath()).isNull();
		assertObject(t.setSchemes(set("foo","bar")).getSchemes()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.setSchemes(set()).getSchemes()).isType(Set.class).asJson().is("[]");
		assertObject(t.setSchemes((Collection<String>)null).getSchemes()).isNull();
		assertObject(t.setConsumes(set(MediaType.of("text/foo"))).getConsumes()).isType(Set.class).asJson().is("['text/foo']");
		assertObject(t.setConsumes(set()).getConsumes()).isType(Set.class).asJson().is("[]");
		assertObject(t.setConsumes((Collection<MediaType>)null).getConsumes()).isNull();
		assertObject(t.setProduces(set(MediaType.of("text/foo"))).getProduces()).isType(Set.class).asJson().is("['text/foo']");
		assertObject(t.setProduces(set()).getProduces()).isType(Set.class).asJson().is("[]");
		assertObject(t.setProduces((Collection<MediaType>)null).getProduces()).isNull();
		assertObject(t.setPaths(map("foo", new OperationMap().append("bar",operation().setSummary("baz")))).getPaths()).isType(Map.class).asJson().is("{foo:{bar:{summary:'baz'}}}");
		assertObject(t.setPaths(map()).getPaths()).isNull();
		assertObject(t.setPaths((Map<String,OperationMap>)null).getPaths()).isNull();
		assertObject(t.setPaths(null).addPath("a", "a1", operation().setDescription("a2")).addPath("b", null, null).getPaths()).asJson().is("{a:{a1:{description:'a2'}},b:{null:null}}");
		assertObject(t.setDefinitions(map("foo",JsonMap.of("type","bar"))).getDefinitions()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");
		assertObject(t.setDefinitions(map()).getDefinitions()).isType(Map.class).asJson().is("{}");
		assertObject(t.setDefinitions((Map<String,JsonMap>)null).getDefinitions()).isNull();
		assertObject(t.setDefinitions(null).addDefinition("a", JsonMap.of("type","a1")).addDefinition("b", (JsonMap)null).addDefinition(null, JsonMap.of("type", "c1")).getDefinitions()).asJson().is("{a:{type:'a1'},b:null,null:{type:'c1'}}");
		assertObject(t.setParameters(map("foo",parameterInfo().setName("bar"))).getParameters()).isType(Map.class).asJson().is("{foo:{name:'bar'}}");
		assertObject(t.setParameters(map()).getParameters()).isType(Map.class).asJson().is("{}");
		assertObject(t.setParameters((Map<String,ParameterInfo>)null).getParameters()).isNull();
		assertObject(t.setParameters(null).addParameter("a", parameterInfo().setIn("a1")).addParameter("b", null).addParameter(null, parameterInfo().setIn("c1")).getParameters()).asJson().is("{a:{'in':'a1'},b:null,null:{'in':'c1'}}");
		assertObject(t.setResponses(map("123",responseInfo("bar"))).getResponses()).isType(Map.class).asJson().is("{'123':{description:'bar'}}");
		assertObject(t.setResponses(map()).getResponses()).isType(Map.class).asJson().is("{}");
		assertObject(t.setResponses((Map<String,ResponseInfo>)null).getResponses()).isNull();
		assertObject(t.setResponses(null).addResponse("a", responseInfo("a1")).addResponse(null, responseInfo("b1")).addResponse("c", null).getResponses()).asJson().is("{a:{description:'a1'},null:{description:'b1'},c:null}");
		assertObject(t.setSecurityDefinitions(map("foo",securityScheme("bar"))).getSecurityDefinitions()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");
		assertObject(t.setSecurityDefinitions(map()).getSecurityDefinitions()).isType(Map.class).asJson().is("{}");
		assertObject(t.setSecurityDefinitions((Map<String,SecurityScheme>)null).getSecurityDefinitions()).isNull();
		assertObject(t.setSecurityDefinitions(null).addSecurityDefinition("a", securityScheme("a1")).addSecurityDefinition("b", null).addSecurityDefinition(null, securityScheme("c1")).getSecurityDefinitions()).asJson().is("{a:{type:'a1'},b:null,null:{type:'c1'}}");
		assertObject(t.setSecurity(set(map("foo",alist("bar")))).getSecurity()).isType(List.class).asJson().is("[{foo:['bar']}]");
		assertObject(t.setSecurity(set()).getSecurity()).isType(List.class).asJson().is("[]");
		assertObject(t.setSecurity((Collection<Map<String, List<String>>>)null).getSecurity()).isNull();
		assertObject(t.setSecurity(null).addSecurity("a", "a1", "a2").addSecurity("b", (String)null).addSecurity(null, "d1", "d2").getSecurity()).asJson().is("[{a:['a1','a2']},{b:[null]},{null:['d1','d2']}]");
		assertObject(t.setTags(set(tag("foo"))).getTags()).isType(Set.class).asJson().is("[{name:'foo'}]");
		assertObject(t.setTags(set()).getTags()).isType(Set.class).asJson().is("[]");
		assertObject(t.setTags((Collection<Tag>)null).getTags()).isNull();
		assertObject(t.setExternalDocs(externalDocumentation("foo")).getExternalDocs()).asJson().is("{url:'foo'}");
	}

	/**
	 * Test method for {@link Swagger#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
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
		assertObject(t.get("definitions", Map.class).values().iterator().next()).isType(JsonMap.class);
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

		assertObject(t).asJson().is("{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		Swagger t = new Swagger();

		assertObject(t.keySet()).asJson().is("['swagger']");

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

		assertObject(t.keySet()).asJson().is(
			"['basePath','consumes','definitions','externalDocs','host','info','parameters','paths','produces','responses',"
			+ "'schemes','security','securityDefinitions','swagger','tags','$ref']"
		);
	}
}
