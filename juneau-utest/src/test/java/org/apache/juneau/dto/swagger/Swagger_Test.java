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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.bean.swagger.Tag;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
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
		var t = new Swagger();

		// Basic property setters
		assertBean(
			t.setBasePath("a").setHost("b").setSwagger("c"),
			"basePath,host,swagger",
			"a,b,c"
		);

		// Complex objects
		assertBean(
			t.setInfo(info("a", "b")).setExternalDocs(externalDocumentation("c"))
				.setSchemes(set("d","e")).setConsumes(set(MediaType.of("text/f")))
				.setProduces(set(MediaType.of("text/g")))
				.setPaths(map("h", new OperationMap().append("i",operation().setSummary("j"))))
				.setDefinitions(map("k",JsonMap.of("type","l")))
				.setParameters(map("m",parameterInfo().setName("n")))
				.setResponses(map("1",responseInfo("o")))
				.setSecurityDefinitions(map("p",securityScheme("q")))
				.setSecurity(set(map("r",alist("s"))))
				.setTags(set(tag("t"))),
			"consumes,definitions{k{type}},externalDocs{url},info{title,version},parameters{m{name}},paths{h{i{summary}}},produces,responses{1{description}},schemes,security,securityDefinitions{p{type}},tags{#{name}}",
			"[text/f],{{l}},{c},{a,b},{{n}},{{{j}}},[text/g],{{o}},[d,e],[{r=[s]}],{{q}},{[{t}]}"
		);

		// Null values
		assertBean(
			t.setSwagger(null).setHost(null).setBasePath(null).setSchemes((Collection<String>)null)
				.setConsumes((Collection<MediaType>)null).setProduces((Collection<MediaType>)null)
				.setPaths((Map<String,OperationMap>)null).setDefinitions((Map<String,JsonMap>)null)
				.setParameters((Map<String,ParameterInfo>)null).setResponses((Map<String,ResponseInfo>)null)
				.setSecurityDefinitions((Map<String,SecurityScheme>)null).setSecurity((Collection<Map<String, List<String>>>)null)
				.setTags((Collection<Tag>)null),
			"basePath,consumes,definitions,host,parameters,paths,produces,responses,schemes,security,securityDefinitions,swagger,tags",
			"null,null,null,null,null,null,null,null,null,null,null,null,null"
		);

		// Other methods - empty collections/maps
		assertBean(
			t.setSchemes(set()).setConsumes(set()).setProduces(set()).setDefinitions(map())
				.setParameters(map()).setResponses(map()).setSecurityDefinitions(map())
				.setSecurity(set()).setTags(set()),
			"consumes,definitions,parameters,produces,responses,schemes,security,securityDefinitions,tags",
			"[],{},{},[],{},[],[],{},[]"
		);

		assertNull(t.setPaths(map()).getPaths());  // BUG?

		// addX methods.
		assertBean(t.setPaths(null).addPath("a", "a1", operation().setDescription("a2")).addPath("b", null, null),
			"paths", "{a={a1={\"description\":\"a2\"}},b={null=null}}");
		assertBean(t.setDefinitions(null).addDefinition("a", JsonMap.of("type","a1")).addDefinition("b", (JsonMap)null).addDefinition(null, JsonMap.of("type", "c1")),
			"definitions", "{a={type=a1},b=null,null={type=c1}}");
		assertBean(t.setParameters(null).addParameter("a", parameterInfo().setIn("a1")).addParameter("b", null).addParameter(null, parameterInfo().setIn("c1")),
			"parameters", "{a={\"in\":\"a1\"},b=null,null={\"in\":\"c1\"}}");
		assertBean(t.setResponses(null).addResponse("a", responseInfo("a1")).addResponse(null, responseInfo("b1")).addResponse("c", null),
			"responses", "{a={\"description\":\"a1\"},null={\"description\":\"b1\"},c=null}");
		assertBean(t.setSecurityDefinitions(null).addSecurityDefinition("a", securityScheme("a1")).addSecurityDefinition("b", null).addSecurityDefinition(null, securityScheme("c1")),
			"securityDefinitions", "{a={\"type\":\"a1\"},b=null,null={\"type\":\"c1\"}}");
		assertBean(t.setSecurity(null).addSecurity("a", "a1", "a2").addSecurity("b", (String)null).addSecurity(null, "d1", "d2"),
			"security", "[{a=[a1,a2]},{b=[null]},{null=[d1,d2]}]");
	}

	/**
	 * Test method for {@link Swagger#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() {
		var t = new Swagger();

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
			.set("$ref", "p");

		assertBean(t,
			"basePath,consumes,definitions{c{type}},externalDocs{url},host,info{title,version},parameters{g{in,name}},paths{h{h1{operationId}}},produces,$ref,responses{j{description}},schemes,security,securityDefinitions{m{type}},swagger,tags{#{name}}",
			"a,[text/b],{{c1}},{d},e,{f1,f2},{{g1,g2}},{{{h2}}},[text/i],p,{{j1}},[k1],[{l1=[l2]}],{{m1}},n,{[{o}]}");

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

		assertBean(t,
			"swagger,info{title,version},tags{#{name}},externalDocs{url},basePath,schemes,consumes,produces,paths{h{h1{operationId}}},definitions{c{type}},parameters{g{in,name}},responses{j{description}},securityDefinitions{m{type}},security,$ref",
			"n,{f1,f2},{[{o}]},{d},a,[k1],[text/b],[text/i],{{{h2}}},{{c1}},{{g1,g2}},{{j1}},{{m1}},[{l1=[l2]}],ref");

		t
			.set("basePath", Utils.sb("a"))
			.set("consumes", Utils.sb("['text/b']"))
			.set("definitions", Utils.sb("{c:{type:'c1'}}"))
			.set("externalDocs", Utils.sb("{url:'d'}"))
			.set("host", Utils.sb("e"))
			.set("info", Utils.sb("{title:'f1',version:'f2'}"))
			.set("parameters", Utils.sb("{g:{'in':'g1',name:'g2'}}"))
			.set("paths", Utils.sb("{h:{h1:{operationId:'h2'}}}"))
			.set("produces", Utils.sb("['text/i']"))
			.set("responses", Utils.sb("{j:{description:'j1'}}"))
			.set("schemes", Utils.sb("['k1']"))
			.set("security", Utils.sb("[{l1:['l2']}]"))
			.set("securityDefinitions", Utils.sb("{m:{type:'m1'}}"))
			.set("swagger", Utils.sb("n"))
			.set("tags", Utils.sb("[{name:'o'}]"))
			.set("$ref", Utils.sb("ref"));

		assertBean(t,
			"swagger,info{title,version},tags{#{name}},externalDocs{url},basePath,schemes,consumes,produces,paths{h{h1{operationId}}},definitions{c{type}},parameters{g{in,name}},responses{j{description}},securityDefinitions{m{type}},security,$ref",
			"n,{f1,f2},{[{o}]},{d},a,[k1],[text/b],[text/i],{{{h2}}},{{c1}},{{g1,g2}},{{j1}},{{m1}},[{l1=[l2]}],ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"basePath,consumes,definitions,externalDocs,host,info,parameters,paths,produces,responses,schemes,security,securityDefinitions,swagger,tags,$ref",
			"a,['text/b'],{c:{type:'c1'}},{url:'d'},e,{title:'f1',version:'f2'},{g:{'in':'g1',name:'g2'}},{h:{h1:{operationId:'h2'}}},['text/i'],{j:{description:'j1'}},['k1'],[{l1:['l2']}],{m:{type:'m1'}},n,[{name:'o'}],ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"basePath,consumes,definitions,externalDocs,host,info,parameters,paths,produces,responses,schemes,security,securityDefinitions,swagger,tags,$ref",
			"String,LinkedHashSet,LinkedHashMap,ExternalDocumentation,String,Info,LinkedHashMap,TreeMap,LinkedHashSet,LinkedHashMap,LinkedHashSet,ArrayList,LinkedHashMap,String,LinkedHashSet,StringBuilder");

		assertMapped(t, (o,k) -> o.get(k, List.class).get(0).getClass().getSimpleName(), "consumes,tags", "MediaType,Tag");
		assertMapped(t, (o,k) -> o.get(k, Map.class).values().iterator().next().getClass().getSimpleName(), "definitions,parameters,responses,securityDefinitions", "JsonMap,ParameterInfo,ResponseInfo,SecurityScheme");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, Swagger.class), s);
	}

	@Test void b03_copy() {
		var t = new Swagger();

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
			.set("$ref", "p")
			.copy();

		assertBean(t,
			"swagger,info{title,version},tags{#{name}},externalDocs{url},basePath,schemes,consumes,produces,paths{h{h1{operationId}}},definitions{c{type}},parameters{g{in,name}},responses{j{description}},securityDefinitions{m{type}},security,$ref",
			"n,{f1,f2},{[{o}]},{d},a,[k1],[text/b],[text/i],{{{h2}}},{{c1}},{{g1,g2}},{{j1}},{{m1}},[{l1=[l2]}],p");
	}

	@Test void b04_keySet() {
		var t = new Swagger();

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
			.set("$ref", "p");

		assertSet(t.keySet(), "basePath", "consumes", "definitions", "externalDocs", "host", "info", "parameters", "paths", "produces", "responses", "schemes", "security", "securityDefinitions", "swagger", "tags", "$ref");
	}
}