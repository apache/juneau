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
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
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
		assertJson(t.setTags(set("foo","bar")).getTags(), "['foo','bar']");
		assertJson(t.setTags("bar","baz").getTags(), "['bar','baz']");
		assertJson(t.setTags(set()).getTags(), "[]");
		assertNull(t.setTags((Collection<String>)null).getTags());
		assertEquals("foo", t.setSummary("foo").getSummary());
		assertEquals("foo", t.setDescription("foo").getDescription());
		assertJson(t.setExternalDocs(externalDocumentation("foo")).getExternalDocs(), "{url:'foo'}");
		assertEquals("foo", t.setOperationId("foo").getOperationId());
		assertJson(t.setConsumes(set(MediaType.of("text/foo"))).getConsumes(), "['text/foo']");
		assertJson(t.setConsumes(set()).getConsumes(), "[]");
		assertNull(t.setConsumes((Collection<MediaType>)null).getConsumes());
		assertJson(t.setProduces(set(MediaType.of("text/foo"))).getProduces(), "['text/foo']");
		assertJson(t.setProduces(set()).getProduces(), "[]");
		assertNull(t.setProduces((Collection<MediaType>)null).getProduces());
		assertJson(t.setParameters(set(parameterInfo("foo","bar"))).getParameters(), "[{'in':'foo',name:'bar'}]");
		assertJson(t.setParameters(set()).getParameters(), "[]");
		assertNull(t.setParameters((Collection<ParameterInfo>)null).getParameters());
		assertJson(t.setResponses(map("123",responseInfo("bar"))).getResponses(), "{'123':{description:'bar'}}");
		assertJson(t.setResponses(map()).getResponses(), "{}");
		assertNull(t.setResponses((Map<String,ResponseInfo>)null).getResponses());
		assertJson(t.setSchemes(set("foo")).getSchemes(), "['foo']");
		assertJson(t.setSchemes(set()).getSchemes(), "[]");
		assertNull(t.setSchemes((Set<String>)null).getSchemes());
		assertJson(t.setSecurity(alist(map("foo",alist("bar")))).getSecurity(), "[{foo:['bar']}]");
		assertJson(t.setSecurity(alist()).getSecurity(), "[]");
		assertNull(t.setSecurity((List<Map<String,List<String>>>)null).getSecurity());
		assertTrue(t.setDeprecated(true).getDeprecated());
	}

	/**
	 * Test method for {@link Operation#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void a01_set() throws Exception {
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
			.set("$ref", "ref");

		assertJson(t, "{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");

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

		assertJson(t, "{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");

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

		assertJson(t, "{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");

		assertEquals("['text/a']", t.get("consumes", String.class));
		assertEquals("true", t.get("deprecated", String.class));
		assertEquals("b", t.get("description", String.class));
		assertEquals("{url:'c'}", t.get("externalDocs", String.class));
		assertEquals("d", t.get("operationId", String.class));
		assertEquals("[{'in':'e1',name:'e2'}]", t.get("parameters", String.class));
		assertEquals("['text/f']", t.get("produces", String.class));
		assertEquals("{'1':{description:'g'}}", t.get("responses", String.class));
		assertEquals("['h']", t.get("schemes", String.class));
		assertEquals("[{i1:['i2']}]", t.get("security", String.class));
		assertEquals("j", t.get("summary", String.class));
		assertEquals("['k']", t.get("tags", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertType(Set.class, t.get("consumes", Object.class));
		assertType(MediaType.class, t.get("consumes", List.class).get(0));
		assertType(Boolean.class, t.get("deprecated", Object.class));
		assertType(String.class, t.get("description", Object.class));
		assertType(ExternalDocumentation.class, t.get("externalDocs", Object.class));
		assertType(String.class, t.get("operationId", Object.class));
		assertType(List.class, t.get("parameters", Object.class));
		assertType(ParameterInfo.class, t.get("parameters", List.class).get(0));
		assertType(Set.class, t.get("produces", Object.class));
		assertType(MediaType.class, t.get("produces", List.class).get(0));
		assertType(Map.class, t.get("responses", Object.class));
		assertType(String.class, t.get("responses", Map.class).keySet().iterator().next());
		assertType(ResponseInfo.class, t.get("responses", Map.class).values().iterator().next());
		assertType(Set.class, t.get("schemes", Object.class));
		assertType(List.class, t.get("security", Object.class));
		assertType(String.class, t.get("summary", Object.class));
		assertType(Set.class, t.get("tags", Object.class));
		assertType(StringBuilder.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, Operation.class), s);
	}

	@Test void b02_copy() {
		var t = new Operation();

		t = t.copy();

		assertJson(t, "{}");

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

		assertJson(t, "{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");
	}

	@Test void b03_keySet() {
		var t = new Operation();

		assertJson(t.keySet(), "[]");

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

		assertJson(t.keySet(),
			"['consumes','deprecated','description','externalDocs','operationId','parameters','produces','responses','schemes','security','summary','tags','$ref']"
		);
	}
}