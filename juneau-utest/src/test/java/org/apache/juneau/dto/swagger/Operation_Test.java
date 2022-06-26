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
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Operation}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Operation_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		Operation t = new Operation();
		assertObject(t.setTags(set("foo","bar")).getTags()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.setTags("bar","baz").getTags()).isType(Set.class).asJson().is("['bar','baz']");
		assertObject(t.setTags(set()).getTags()).isType(Set.class).asJson().is("[]");
		assertObject(t.setTags((Collection<String>)null).getTags()).isNull();
		assertString(t.setSummary("foo").getSummary()).is("foo");
		assertString(t.setDescription("foo").getDescription()).is("foo");
		assertObject(t.setExternalDocs(externalDocumentation("foo")).getExternalDocs()).isType(ExternalDocumentation.class).asJson().is("{url:'foo'}");
		assertString(t.setOperationId("foo").getOperationId()).is("foo");
		assertObject(t.setConsumes(set(MediaType.of("text/foo"))).getConsumes()).isType(Set.class).asJson().is("['text/foo']");
		assertObject(t.setConsumes(set()).getConsumes()).isType(Set.class).asJson().is("[]");
		assertObject(t.setConsumes((Collection<MediaType>)null).getConsumes()).isNull();
		assertObject(t.setProduces(set(MediaType.of("text/foo"))).getProduces()).isType(Set.class).asJson().is("['text/foo']");
		assertObject(t.setProduces(set()).getProduces()).isType(Set.class).asJson().is("[]");
		assertObject(t.setProduces((Collection<MediaType>)null).getProduces()).isNull();
		assertObject(t.setParameters(set(parameterInfo("foo","bar"))).getParameters()).isType(List.class).asJson().is("[{'in':'foo',name:'bar'}]");
		assertObject(t.setParameters(set()).getParameters()).isType(List.class).asJson().is("[]");
		assertObject(t.setParameters((Collection<ParameterInfo>)null).getParameters()).isNull();
		assertObject(t.setResponses(map("123",responseInfo("bar"))).getResponses()).isType(Map.class).asJson().is("{'123':{description:'bar'}}");
		assertObject(t.setResponses(map()).getResponses()).isType(Map.class).asJson().is("{}");
		assertObject(t.setResponses((Map<String,ResponseInfo>)null).getResponses()).isNull();
		assertObject(t.setSchemes(set("foo")).getSchemes()).isType(Set.class).asJson().is("['foo']");
		assertObject(t.setSchemes(set()).getSchemes()).isType(Set.class).asJson().is("[]");
		assertObject(t.setSchemes((Set<String>)null).getSchemes()).isNull();
		assertObject(t.setSecurity(alist(map("foo",alist("bar")))).getSecurity()).isType(List.class).asJson().is("[{foo:['bar']}]");
		assertObject(t.setSecurity(alist()).getSecurity()).isType(List.class).asJson().is("[]");
		assertObject(t.setSecurity((List<Map<String,List<String>>>)null).getSecurity()).isNull();
		assertObject(t.setDeprecated(true).getDeprecated()).isType(Boolean.class).is(true);
	}

	/**
	 * Test method for {@link Operation#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		Operation t = new Operation();

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
			.set("$ref", "ref");

		assertObject(t).asJson().is("{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");

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
			.set("$ref", "ref");

		assertObject(t).asJson().is("{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");

		t
			.set("consumes", new StringBuilder("['text/a']"))
			.set("deprecated", new StringBuilder("true"))
			.set("description", new StringBuilder("b"))
			.set("externalDocs", new StringBuilder("{url:'c'}"))
			.set("operationId", new StringBuilder("d"))
			.set("parameters", new StringBuilder("[{'in':'e1',name:'e2'}]"))
			.set("produces", new StringBuilder("['text/f']"))
			.set("responses", new StringBuilder("{'1':{description:'g'}}"))
			.set("schemes", new StringBuilder("['h']"))
			.set("security", new StringBuilder("[{i1:['i2']}]"))
			.set("summary", new StringBuilder("j"))
			.set("tags", new StringBuilder("['k']"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).asJson().is("{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");

		assertString(t.get("consumes", String.class)).is("['text/a']");
		assertString(t.get("deprecated", String.class)).is("true");
		assertString(t.get("description", String.class)).is("b");
		assertString(t.get("externalDocs", String.class)).is("{url:'c'}");
		assertString(t.get("operationId", String.class)).is("d");
		assertString(t.get("parameters", String.class)).is("[{'in':'e1',name:'e2'}]");
		assertString(t.get("produces", String.class)).is("['text/f']");
		assertString(t.get("responses", String.class)).is("{'1':{description:'g'}}");
		assertString(t.get("schemes", String.class)).is("['h']");
		assertString(t.get("security", String.class)).is("[{i1:['i2']}]");
		assertString(t.get("summary", String.class)).is("j");
		assertString(t.get("tags", String.class)).is("['k']");
		assertString(t.get("$ref", String.class)).is("ref");

		assertObject(t.get("consumes", Object.class)).isType(Set.class);
		assertObject(t.get("consumes", List.class).get(0)).isType(MediaType.class);
		assertObject(t.get("deprecated", Object.class)).isType(Boolean.class);
		assertObject(t.get("description", Object.class)).isType(String.class);
		assertObject(t.get("externalDocs", Object.class)).isType(ExternalDocumentation.class);
		assertObject(t.get("operationId", Object.class)).isType(String.class);
		assertObject(t.get("parameters", Object.class)).isType(List.class);
		assertObject(t.get("parameters", List.class).get(0)).isType(ParameterInfo.class);
		assertObject(t.get("produces", Object.class)).isType(Set.class);
		assertObject(t.get("produces", List.class).get(0)).isType(MediaType.class);
		assertObject(t.get("responses", Object.class)).isType(Map.class);
		assertObject(t.get("responses", Map.class).keySet().iterator().next()).isType(String.class);
		assertObject(t.get("responses", Map.class).values().iterator().next()).isType(ResponseInfo.class);
		assertObject(t.get("schemes", Object.class)).isType(Set.class);
		assertObject(t.get("security", Object.class)).isType(List.class);
		assertObject(t.get("summary", Object.class)).isType(String.class);
		assertObject(t.get("tags", Object.class)).isType(Set.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, Operation.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
		Operation t = new Operation();

		t = t.copy();

		assertObject(t).asJson().is("{}");

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
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		Operation t = new Operation();

		assertObject(t.keySet()).asJson().is("[]");

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
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is(
			"['consumes','deprecated','description','externalDocs','operationId','parameters','produces','responses','schemes','security','summary','tags','$ref']"
		);
	}
}
