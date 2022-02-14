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

import org.apache.juneau.http.header.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Operation}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Operation_Test {

	/**
	 * Test method for {@link Operation#setTags(java.util.Collection)}.
	 */
	@Test
	public void a01_tags() {
		Operation t = new Operation();

		t.tags(set("foo","bar"));
		assertOptional(t.tags()).isType(Set.class).asJson().is("['foo','bar']");

		t.tags("bar","baz");
		assertOptional(t.tags()).isType(Set.class).asJson().is("['bar','baz']");

		t.tags(set());
		assertOptional(t.tags()).isType(Set.class).asJson().is("[]");

		t.tags((Collection<String>)null);
		assertOptional(t.tags()).isNull();

		t.addTags(set("foo","bar"));
		assertOptional(t.tags()).isType(Set.class).asJson().is("['foo','bar']");

		t.addTags(set());
		assertOptional(t.tags()).isType(Set.class).asJson().is("['foo','bar']");;

		t.addTags(null);
		assertOptional(t.tags()).isType(Set.class).asJson().is("['foo','bar']");

	}

	/**
	 * Test method for {@link Operation#summary(java.lang.Object)}.
	 */
	@Test
	public void a02_summary() {
		Operation t = new Operation();

		t.summary("foo");
		assertString(t.summary()).is("foo");

		t.summary(null);
		assertString(t.summary()).isNull();
	}

	/**
	 * Test method for {@link Operation#description(java.lang.Object)}.
	 */
	@Test
	public void a03_description() {
		Operation t = new Operation();

		t.description("foo");
		assertString(t.description()).is("foo");

		t.description(null);
		assertString(t.description()).isNull();
	}

	/**
	 * Test method for {@link Operation#externalDocs(java.lang.Object)}.
	 */
	@Test
	public void a04_externalDocs() {
		Operation t = new Operation();

		t.externalDocs(externalDocumentation("foo"));
		assertOptional(t.externalDocs()).isType(ExternalDocumentation.class).asJson().is("{url:'foo'}");

		t.externalDocs("{url:'bar'}");
		assertOptional(t.externalDocs()).isType(ExternalDocumentation.class).asJson().is("{url:'bar'}");

		t.externalDocs((String)null);
		assertOptional(t.externalDocs()).isNull();
	}

	/**
	 * Test method for {@link Operation#operationId(java.lang.Object)}.
	 */
	@Test
	public void a05_operationId() {
		Operation t = new Operation();

		t.operationId("foo");
		assertString(t.operationId()).is("foo");

		t.operationId(null);
		assertString(t.operationId()).isNull();
	}

	/**
	 * Test method for {@link Operation#setConsumes(java.util.Collection)}.
	 */
	@Test
	public void a06_consumes() {
		Operation t = new Operation();

		t.consumes(set(MediaType.of("text/foo")));
		assertOptional(t.consumes()).isType(Set.class).asJson().is("['text/foo']");

		t.consumes(set());
		assertOptional(t.consumes()).isType(Set.class).asJson().is("[]");

		t.consumes((Collection<MediaType>)null);
		assertOptional(t.consumes()).isNull();

		t.addConsumes(set(MediaType.of("text/foo")));
		assertOptional(t.consumes()).isType(Set.class).asJson().is("['text/foo']");

		t.addConsumes(set());
		assertOptional(t.consumes()).isType(Set.class).asJson().is("['text/foo']");

		t.addConsumes(null);
		assertOptional(t.consumes()).isType(Set.class).asJson().is("['text/foo']");
	}

	/**
	 * Test method for {@link Operation#setProduces(java.util.Collection)}.
	 */
	@Test
	public void a07_produces() {
		Operation t = new Operation();

		t.produces(set(MediaType.of("text/foo")));
		assertOptional(t.produces()).isType(Set.class).asJson().is("['text/foo']");

		t.produces(set());
		assertOptional(t.produces()).isType(Set.class).asJson().is("[]");

		t.produces((Collection<MediaType>)null);
		assertOptional(t.produces()).isNull();

		t.addProduces(set(MediaType.of("text/foo")));
		assertOptional(t.produces()).isType(Set.class).asJson().is("['text/foo']");

		t.addProduces(set());
		assertOptional(t.produces()).isType(Set.class).asJson().is("['text/foo']");

		t.addProduces(null);
		assertOptional(t.produces()).isType(Set.class).asJson().is("['text/foo']");
	}

	/**
	 * Test method for {@link Operation#setParameters(java.util.Collection)}.
	 */
	@Test
	public void a08_parameters() {
		Operation t = new Operation();

		t.parameters(set(parameterInfo("foo","bar")));
		assertOptional(t.parameters()).isType(List.class).asJson().is("[{'in':'foo',name:'bar'}]");

		t.parameters(set());
		assertOptional(t.parameters()).isType(List.class).asJson().is("[]");

		t.parameters((Collection<ParameterInfo>)null);
		assertOptional(t.parameters()).isNull();

		t.addParameters(set(parameterInfo("foo","bar")));
		assertOptional(t.parameters()).isType(List.class).asJson().is("[{'in':'foo',name:'bar'}]");;

		t.addParameters(set());
		assertOptional(t.parameters()).isType(List.class).asJson().is("[{'in':'foo',name:'bar'}]");

		t.addParameters(null);
		assertOptional(t.parameters()).isType(List.class).asJson().is("[{'in':'foo',name:'bar'}]");
	}

	/**
	 * Test method for {@link Operation#setResponses(java.util.Map)}.
	 */
	@Test
	public void a09_responses() {
		Operation t = new Operation();

		t.responses(map("123",responseInfo("bar")));
		assertOptional(t.responses()).isType(Map.class).asJson().is("{'123':{description:'bar'}}");

		t.responses(map());
		assertOptional(t.responses()).isType(Map.class).asJson().is("{}");

		t.responses((Map<String,ResponseInfo>)null);
		assertOptional(t.responses()).isNull();

		t.addResponses(map("123",responseInfo("bar")));
		assertOptional(t.responses()).isType(Map.class).asJson().is("{'123':{description:'bar'}}");

		t.addResponses(map());
		assertOptional(t.responses()).isType(Map.class).asJson().is("{'123':{description:'bar'}}");

		t.addResponses(null);
		assertOptional(t.responses()).isType(Map.class).asJson().is("{'123':{description:'bar'}}");
	}

	/**
	 * Test method for {@link Operation#setSchemes(java.util.Collection)}.
	 */
	@Test
	public void a10_schemes() {
		Operation t = new Operation();

		t.schemes(set("foo"));
		assertOptional(t.schemes()).isType(Set.class).asJson().is("['foo']");

		t.schemes(set());
		assertOptional(t.schemes()).isType(Set.class).asJson().is("[]");

		t.schemes((Set<String>)null);
		assertOptional(t.schemes()).isNull();

		t.addSchemes(set("foo"));
		assertOptional(t.schemes()).isType(Set.class).asJson().is("['foo']");

		t.addSchemes(set());
		assertOptional(t.schemes()).isType(Set.class).asJson().is("['foo']");

		t.addSchemes(null);
		assertOptional(t.schemes()).isType(Set.class).asJson().is("['foo']");
	}

	/**
	 * Test method for {@link Operation#setSecurity(java.util.Collection)}.
	 */
	@Test
	public void a11_security() {
		Operation t = new Operation();

		t.security(alist(map("foo",alist("bar"))));
		assertOptional(t.security()).isType(List.class).asJson().is("[{foo:['bar']}]");

		t.security(alist());
		assertOptional(t.security()).isType(List.class).asJson().is("[]");

		t.security((List<Map<String,List<String>>>)null);
		assertOptional(t.security()).isNull();

		t.addSecurity(set(map("foo",alist("bar"))));
		assertOptional(t.security()).isType(List.class).asJson().is("[{foo:['bar']}]");

		t.addSecurity(set());
		assertOptional(t.security()).isType(List.class).asJson().is("[{foo:['bar']}]");

		t.addSecurity(null);
		assertOptional(t.security()).isType(List.class).asJson().is("[{foo:['bar']}]");
	}

	/**
	 * Test method for {@link Operation#deprecated(java.lang.Object)}.
	 */
	@Test
	public void a12_deprecated() {
		Operation t = new Operation();

		t.deprecated(true);
		assertOptional(t.deprecated()).isType(Boolean.class).is(true);

		t.deprecated("true");
		assertOptional(t.deprecated()).isType(Boolean.class).is(true);

		t.deprecated((String)null);
		assertOptional(t.deprecated()).isNull();
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
