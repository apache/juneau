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
import org.apache.juneau.common.internal.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Operation}.
 */
class Operation_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new Operation();

		// Basic property setters
		assertBean(
			t.setConsumes(set(MediaType.of("text/a"))).setDeprecated(true).setDescription("b").setExternalDocs(externalDocumentation("c"))
			.setOperationId("d").setParameters(set(parameterInfo("e1","e2"))).setProduces(set(MediaType.of("text/f")))
			.setResponses(map("1",responseInfo("g"))).setSchemes(set("h")).setSecurity(alist(map("i",alist("j")))).setSummary("k").setTags(set("l1","l2")),
			"consumes,deprecated,description,externalDocs{url},operationId,parameters{0{in,name}},produces,responses{1{description}},schemes,security,summary,tags",
			"[text/a],true,b,{c},d,{{e1,e2}},[text/f],{{g}},[h],[{i=[j]}],k,[l1,l2]"
		);

		// Null values
		assertBean(
			t.setConsumes((Collection<MediaType>)null).setParameters((Collection<ParameterInfo>)null).setProduces((Collection<MediaType>)null)
			.setResponses((Map<String,ResponseInfo>)null).setSchemes((Set<String>)null).setSecurity((List<Map<String,List<String>>>)null).setTags((Collection<String>)null),
			"consumes,parameters,produces,responses,schemes,security,tags",
			"null,null,null,null,null,null,null"
		);

		// Other methods - empty collections
		assertBean(
			t.setConsumes(set()).setParameters(set()).setProduces(set()).setResponses(map()).setSchemes(set()).setSecurity(alist()).setTags(set()),
			"consumes,parameters,produces,responses,schemes,security,tags",
			"[],[],[],{},[],[],[]"
		);
	}

	/**
	 * Test method for {@link Operation#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void a01_set() {
		var t = new Operation();

		t
			.set("consumes", set(MediaType.of("text/a")))
			.set("deprecated", true)
			.set("description", "b")
			.set("externalDocs", externalDocumentation("c"))
			.set("operationId", "d")
			.set("parameters", set(parameterInfo("e1","e2")))
			.set("produces", set(MediaType.of("text/f")))
			.set("responses", map(1,responseInfo("g")))
			.set("schemes", set("h"))
			.set("security", set(map("i1",alist("i2"))))
			.set("summary", "j")
			.set("tags", set("k"))
			.set("$ref", "l");  // Not a bean property but accessed through get(String) and set(String,Object).

		assertBean(
			t,
			"consumes,deprecated,description,externalDocs{url},operationId,parameters{0{in,name}},produces,responses{1{description}},schemes,security,summary,tags,$ref",
			"[text/a],true,b,{c},d,{{e1,e2}},[text/f],{{g}},[h],[{i1=[i2]}],j,[k],l"
		);

		t
			.set("consumes", "['text/a']")
			.set("deprecated", "true")
			.set("description", "b")
			.set("externalDocs", "{url:'c'}")
			.set("operationId", "d")
			.set("parameters", "[{'in':'e1',name:'e2'}]")
			.set("produces", "['text/f']")
			.set("responses", "{'1':{description:'g'}}")
			.set("schemes", "['h']")
			.set("security", "[{i1:['i2']}]")
			.set("summary", "j")
			.set("tags", "['k']")
			.set("$ref", "l");

		assertBean(
			t,
			"consumes,deprecated,description,externalDocs{url},operationId,parameters{0{in,name}},produces,responses{1{description}},schemes,security,summary,tags,$ref",
			"[text/a],true,b,{c},d,{{e1,e2}},[text/f],{{g}},[h],[{i1=[i2]}],j,[k],l"
		);

		t
			.set("consumes", Utils.sb("['text/a']"))
			.set("deprecated", Utils.sb("true"))
			.set("description", Utils.sb("b"))
			.set("externalDocs", Utils.sb("{url:'c'}"))
			.set("operationId", Utils.sb("d"))
			.set("parameters", Utils.sb("[{'in':'e1',name:'e2'}]"))
			.set("produces", Utils.sb("['text/f']"))
			.set("responses", Utils.sb("{'1':{description:'g'}}"))
			.set("schemes", Utils.sb("['h']"))
			.set("security", Utils.sb("[{i1:['i2']}]"))
			.set("summary", Utils.sb("j"))
			.set("tags", Utils.sb("['k']"))
			.set("$ref", Utils.sb("l"));

		assertBean(
			t,
			"consumes,deprecated,description,externalDocs{url},operationId,parameters{0{in,name}},produces,responses{1{description}},schemes,security,summary,tags,$ref",
			"[text/a],true,b,{c},d,{{e1,e2}},[text/f],{{g}},[h],[{i1=[i2]}],j,[k],l"
		);

		assertMapped(t,
			(obj,prop) -> obj.get(prop, String.class),
			"consumes,deprecated,description,externalDocs,operationId,parameters,produces,responses,schemes,security,summary,tags,$ref",
			"['text/a'],true,b,{url:'c'},d,[{'in':'e1',name:'e2'}],['text/f'],{'1':{description:'g'}},['h'],[{i1:['i2']}],j,['k'],l"
		);

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"consumes,deprecated,description,externalDocs,operationId,parameters,produces,responses,schemes,security,summary,tags,$ref",
			"LinkedHashSet,Boolean,String,ExternalDocumentation,String,ArrayList,LinkedHashSet,LinkedHashMap,LinkedHashSet,ArrayList,String,LinkedHashSet,StringBuilder");

		assertMapped(t, (o,k) -> o.get(k, List.class).get(0).getClass().getSimpleName(), "consumes,parameters,produces", "MediaType,ParameterInfo,MediaType");
		assertMapped(t, (o,k) -> {
			Map<?,?> map = o.get(k, Map.class);
			return map.keySet().iterator().next().getClass().getSimpleName() + "," + map.values().iterator().next().getClass().getSimpleName();
		}, "responses", "String,ResponseInfo");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}";
		assertJson(s, JsonParser.DEFAULT.parse(s, Operation.class));
	}

	@Test void b03_copy() {
		var t = new Operation();

		t = t.copy();

		assertJson("{}", t);

		t
			.set("consumes", set(MediaType.of("text/a")))
			.set("deprecated", true)
			.set("description", "b")
			.set("externalDocs", externalDocumentation("c"))
			.set("operationId", "d")
			.set("parameters", set(parameterInfo("e1","e2")))
			.set("produces", set(MediaType.of("text/f")))
			.set("responses", map(1,responseInfo("g")))
			.set("schemes", set("h"))
			.set("security", set(map("i1",alist("i2"))))
			.set("summary", "j")
			.set("tags", set("k"))
			.set("$ref", "l")
			.copy();

		assertBean(
			t,
			"consumes,deprecated,description,externalDocs{url},operationId,parameters{0{in,name}},produces,responses{1{description}},schemes,security,summary,tags,$ref",
			"[text/a],true,b,{c},d,{{e1,e2}},[text/f],{{g}},[h],[{i1=[i2]}],j,[k],l"
		);
	}

	@Test void b04_keySet() {
		var t = new Operation();

		assertEmpty(t.keySet());

		t
			.set("consumes", set(MediaType.of("text/a")))
			.set("deprecated", true)
			.set("description", "b")
			.set("externalDocs", externalDocumentation("c"))
			.set("operationId", "d")
			.set("parameters", set(parameterInfo("e1","e2")))
			.set("produces", set(MediaType.of("text/f")))
			.set("responses", map(1,responseInfo("g")))
			.set("schemes", set("h"))
			.set("security", set(map("i1",alist("i2"))))
			.set("summary", "j")
			.set("tags", set("k"))
			.set("$ref", "l");

		assertList(t.keySet(), "consumes", "deprecated", "description", "externalDocs", "operationId", "parameters", "produces", "responses", "schemes", "security", "summary", "tags", "$ref");
	}
}