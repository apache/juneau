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
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Operation}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class OperationTest {

	/**
	 * Test method for {@link Operation#setTags(java.util.Collection)}.
	 */
	@Test
	public void testSetTags() {
		Operation t = new Operation();

		t.setTags(ASet.of("foo","bar"));
		assertObject(t.getTags()).json().is("['foo','bar']");
		assertObject(t.getTags()).isType(List.class);

		t.setTags(ASet.of());
		assertObject(t.getTags()).json().is("[]");
		assertObject(t.getTags()).isType(List.class);

		t.setTags(null);
		assertNull(t.getTags());
	}

	/**
	 * Test method for {@link Operation#addTags(java.util.Collection)}.
	 */
	@Test
	public void testAddTags() {
		Operation t = new Operation();

		t.addTags(ASet.of("foo","bar"));
		assertObject(t.getTags()).json().is("['foo','bar']");
		assertObject(t.getTags()).isType(List.class);

		t.addTags(ASet.of());
		assertObject(t.getTags()).json().is("['foo','bar']");
		assertObject(t.getTags()).isType(List.class);

		t.addTags(null);
		assertObject(t.getTags()).json().is("['foo','bar']");
		assertObject(t.getTags()).isType(List.class);
	}

	/**
	 * Test method for {@link Operation#tags(java.lang.Object[])}.
	 */
	@Test
	public void testTags() {
		Operation t = new Operation();

		t.tags(ASet.of("a"));
		t.tags(ASet.of(new StringBuilder("b")));
		t.tags((Object)new String[] {"c"});
		t.tags((Object)new Object[] {new StringBuilder("d")});
		t.tags("e");
		t.tags("['f']");
		t.tags("[]");
		t.tags((Object)null);
		assertObject(t.getTags()).json().is("['a','b','c','d','e','f']");
		for (String s : t.getTags())
			assertObject(s).isType(String.class);
	}

	/**
	 * Test method for {@link Operation#summary(java.lang.Object)}.
	 */
	@Test
	public void testSummary() {
		Operation t = new Operation();

		t.summary("foo");
		assertEquals("foo", t.getSummary());

		t.summary(new StringBuilder("foo"));
		assertEquals("foo", t.getSummary());
		assertObject(t.getSummary()).isType(String.class);

		t.summary(null);
		assertNull(t.getSummary());
	}

	/**
	 * Test method for {@link Operation#description(java.lang.Object)}.
	 */
	@Test
	public void testDescription() {
		Operation t = new Operation();

		t.description("foo");
		assertEquals("foo", t.getDescription());

		t.description(new StringBuilder("foo"));
		assertEquals("foo", t.getDescription());
		assertObject(t.getDescription()).isType(String.class);

		t.description(null);
		assertNull(t.getDescription());
	}

	/**
	 * Test method for {@link Operation#externalDocs(java.lang.Object)}.
	 */
	@Test
	public void testExternalDocs() {
		Operation t = new Operation();

		t.externalDocs(externalDocumentation("foo"));
		assertObject(t.getExternalDocs()).json().is("{url:'foo'}");

		t.externalDocs("{url:'foo'}");
		assertObject(t.getExternalDocs()).json().is("{url:'foo'}");
		assertObject(t.getExternalDocs()).isType(ExternalDocumentation.class);

		t.externalDocs(null);
		assertNull(t.getExternalDocs());
	}

	/**
	 * Test method for {@link Operation#operationId(java.lang.Object)}.
	 */
	@Test
	public void testOperationId() {
		Operation t = new Operation();

		t.operationId("foo");
		assertEquals("foo", t.getOperationId());

		t.operationId(new StringBuilder("foo"));
		assertEquals("foo", t.getOperationId());
		assertObject(t.getOperationId()).isType(String.class);

		t.operationId(null);
		assertNull(t.getOperationId());
	}

	/**
	 * Test method for {@link Operation#setConsumes(java.util.Collection)}.
	 */
	@Test
	public void testSetConsumes() {
		Operation t = new Operation();

		t.setConsumes(ASet.of(MediaType.of("text/foo")));
		assertObject(t.getConsumes()).json().is("['text/foo']");
		assertObject(t.getConsumes()).isType(List.class);

		t.setConsumes(ASet.of());
		assertObject(t.getConsumes()).json().is("[]");
		assertObject(t.getConsumes()).isType(List.class);

		t.setConsumes(null);
		assertNull(t.getConsumes());
	}

	/**
	 * Test method for {@link Operation#addConsumes(java.util.Collection)}.
	 */
	@Test
	public void testAddConsumes() {
		Operation t = new Operation();

		t.addConsumes(ASet.of(MediaType.of("text/foo")));
		assertObject(t.getConsumes()).json().is("['text/foo']");
		assertObject(t.getConsumes()).isType(List.class);

		t.addConsumes(ASet.of());
		assertObject(t.getConsumes()).json().is("['text/foo']");
		assertObject(t.getConsumes()).isType(List.class);

		t.addConsumes(null);
		assertObject(t.getConsumes()).json().is("['text/foo']");
		assertObject(t.getConsumes()).isType(List.class);
	}

	/**
	 * Test method for {@link Operation#consumes(java.lang.Object[])}.
	 */
	@Test
	public void testConsumes() {
		Operation t = new Operation();

		t.consumes(ASet.of(MediaType.of("text/foo")));
		t.consumes(MediaType.of("text/bar"));
		t.consumes("text/baz");
		t.consumes(new StringBuilder("text/qux"));
		t.consumes((Object)new String[]{"text/quux"});
		t.consumes((Object)ASet.of("text/quuux"));
		t.consumes("['text/quuuux']");
		t.consumes("[]");
		t.consumes((Object)null);
		assertObject(t.getConsumes()).json().is("['text/foo','text/bar','text/baz','text/qux','text/quux','text/quuux','text/quuuux']");
		assertObject(t.getConsumes()).isType(List.class);
		for (MediaType mt : t.getConsumes())
			assertObject(mt).isType(MediaType.class);
	}

	/**
	 * Test method for {@link Operation#setProduces(java.util.Collection)}.
	 */
	@Test
	public void testSetProduces() {
		Operation t = new Operation();

		t.setProduces(ASet.of(MediaType.of("text/foo")));
		assertObject(t.getProduces()).json().is("['text/foo']");
		assertObject(t.getProduces()).isType(List.class);

		t.setProduces(ASet.of());
		assertObject(t.getProduces()).json().is("[]");
		assertObject(t.getProduces()).isType(List.class);

		t.setProduces(null);
		assertNull(t.getProduces());
	}

	/**
	 * Test method for {@link Operation#addProduces(java.util.Collection)}.
	 */
	@Test
	public void testAddProduces() {
		Operation t = new Operation();

		t.addProduces(ASet.of(MediaType.of("text/foo")));
		assertObject(t.getProduces()).json().is("['text/foo']");
		assertObject(t.getProduces()).isType(List.class);

		t.addProduces(ASet.of());
		assertObject(t.getProduces()).json().is("['text/foo']");
		assertObject(t.getProduces()).isType(List.class);

		t.addProduces(null);
		assertObject(t.getProduces()).json().is("['text/foo']");
		assertObject(t.getProduces()).isType(List.class);
	}

	/**
	 * Test method for {@link Operation#produces(java.lang.Object[])}.
	 */
	@Test
	public void testProduces() {
		Operation t = new Operation();

		t.produces(ASet.of(MediaType.of("text/foo")));
		t.produces(MediaType.of("text/bar"));
		t.produces("text/baz");
		t.produces(new StringBuilder("text/qux"));
		t.produces((Object)new String[]{"text/quux"});
		t.produces((Object)ASet.of("text/quuux"));
		t.produces("['text/quuuux']");
		t.produces("[]");
		t.produces((Object)null);
		assertObject(t.getProduces()).json().is("['text/foo','text/bar','text/baz','text/qux','text/quux','text/quuux','text/quuuux']");
		assertObject(t.getProduces()).isType(List.class);
		for (MediaType mt : t.getProduces())
			assertObject(mt).isType(MediaType.class);
	}

	/**
	 * Test method for {@link Operation#setParameters(java.util.Collection)}.
	 */
	@Test
	public void testSetParameters() {
		Operation t = new Operation();

		t.setParameters(ASet.of(parameterInfo("foo","bar")));
		assertObject(t.getParameters()).json().is("[{'in':'foo',name:'bar'}]");
		assertObject(t.getParameters()).isType(List.class);

		t.setParameters(ASet.of());
		assertObject(t.getParameters()).json().is("[]");
		assertObject(t.getParameters()).isType(List.class);

		t.setParameters(null);
		assertNull(t.getParameters());
	}

	/**
	 * Test method for {@link Operation#addParameters(java.util.Collection)}.
	 */
	@Test
	public void testAddParameters() {
		Operation t = new Operation();

		t.addParameters(ASet.of(parameterInfo("foo","bar")));
		assertObject(t.getParameters()).json().is("[{'in':'foo',name:'bar'}]");
		assertObject(t.getParameters()).isType(List.class);

		t.addParameters(ASet.of());
		assertObject(t.getParameters()).json().is("[{'in':'foo',name:'bar'}]");
		assertObject(t.getParameters()).isType(List.class);

		t.addParameters(null);
		assertObject(t.getParameters()).json().is("[{'in':'foo',name:'bar'}]");
		assertObject(t.getParameters()).isType(List.class);
	}

	/**
	 * Test method for {@link Operation#parameters(java.lang.Object[])}.
	 */
	@Test
	public void testParameters() {
		Operation t = new Operation();

		t.parameters(ASet.of(parameterInfo("a1","a2")));
		t.parameters(parameterInfo("b1","b2"));
		t.parameters("{in:'c1',name:'c2'}");
		t.parameters(new StringBuilder("{in:'d1',name:'d2'}"));
		t.parameters((Object)new String[]{"{in:'e1',name:'e2'}"});
		t.parameters((Object)ASet.of("{in:'f1',name:'f2'}"));
		t.parameters("[{in:'g1',name:'g2'}]");
		t.parameters("[]");
		t.parameters((Object)null);
		assertObject(t.getParameters()).json().is("[{'in':'a1',name:'a2'},{'in':'b1',name:'b2'},{'in':'c1',name:'c2'},{'in':'d1',name:'d2'},{'in':'e1',name:'e2'},{'in':'f1',name:'f2'},{'in':'g1',name:'g2'}]");
		assertObject(t.getParameters()).isType(List.class);
		for (ParameterInfo pi : t.getParameters())
			assertObject(pi).isType(ParameterInfo.class);
	}

	/**
	 * Test method for {@link Operation#setResponses(java.util.Map)}.
	 */
	@Test
	public void testSetResponses() {
		Operation t = new Operation();

		t.setResponses(AMap.of("123",responseInfo("bar")));
		assertObject(t.getResponses()).json().is("{'123':{description:'bar'}}");
		assertObject(t.getResponses()).isType(Map.class);

		t.setResponses(AMap.create());
		assertObject(t.getResponses()).json().is("{}");
		assertObject(t.getResponses()).isType(Map.class);

		t.setResponses(null);
		assertNull(t.getResponses());
	}

	/**
	 * Test method for {@link Operation#addResponses(java.util.Map)}.
	 */
	@Test
	public void testAddResponses() {
		Operation t = new Operation();

		t.addResponses(AMap.of("123",responseInfo("bar")));
		assertObject(t.getResponses()).json().is("{'123':{description:'bar'}}");
		assertObject(t.getResponses()).isType(Map.class);

		t.addResponses(AMap.create());
		assertObject(t.getResponses()).json().is("{'123':{description:'bar'}}");
		assertObject(t.getResponses()).isType(Map.class);

		t.addResponses(null);
		assertObject(t.getResponses()).json().is("{'123':{description:'bar'}}");
		assertObject(t.getResponses()).isType(Map.class);
	}

	/**
	 * Test method for {@link Operation#response(java.lang.Integer, org.apache.juneau.dto.swagger.ResponseInfo)}.
	 */
	@Test
	public void testResponse() {
		Operation t = new Operation();

		t.response("1", responseInfo("foo"));
		t.response((String)null, responseInfo("bar"));
		t.response("2", null);
		assertObject(t.getResponses()).json().is("{'1':{description:'foo'},null:{description:'bar'},'2':null}");
	}

	/**
	 * Test method for {@link Operation#responses(java.lang.Object[])}.
	 */
	@Test
	public void testResponses() {
		Operation t = new Operation();

		t.responses(AMap.of(1,responseInfo("a")));
		t.responses(AMap.of("2","{description:'b'}"));
		t.responses("{3:{description:'c'}}");
		t.responses("{}");
		t.responses((Object)null);

		assertObject(t.getResponses()).json().is("{'1':{description:'a'},'2':{description:'b'},'3':{description:'c'}}");
		for (Map.Entry<String,ResponseInfo> e : t.getResponses().entrySet()) {
			assertObject(e.getKey()).isType(String.class);
			assertObject(e.getValue()).isType(ResponseInfo.class);
		}
	}

	/**
	 * Test method for {@link Operation#setSchemes(java.util.Collection)}.
	 */
	@Test
	public void testSetSchemes() {
		Operation t = new Operation();

		t.setSchemes(ASet.of("foo"));
		assertObject(t.getSchemes()).json().is("['foo']");
		assertObject(t.getSchemes()).isType(List.class);

		t.setSchemes(ASet.of());
		assertObject(t.getSchemes()).json().is("[]");
		assertObject(t.getSchemes()).isType(List.class);

		t.setSchemes(null);
		assertNull(t.getSchemes());
	}

	/**
	 * Test method for {@link Operation#setSecurity(java.util.Collection)}.
	 */
	@Test
	public void testSetSecurity() {
		Operation t = new Operation();

		t.setSecurity(ASet.of(AMap.of("foo",AList.of("bar"))));
		assertObject(t.getSecurity()).json().is("[{foo:['bar']}]");
		assertObject(t.getSecurity()).isType(List.class);

		t.setSecurity(ASet.of());
		assertObject(t.getSecurity()).json().is("[]");
		assertObject(t.getSecurity()).isType(List.class);

		t.setSecurity(null);
		assertNull(t.getSecurity());
	}

	/**
	 * Test method for {@link Operation#addSchemes(java.util.Collection)}.
	 */
	@Test
	public void testAddSchemes() {
		Operation t = new Operation();

		t.addSchemes(ASet.of("foo"));
		assertObject(t.getSchemes()).json().is("['foo']");
		assertObject(t.getSchemes()).isType(List.class);

		t.addSchemes(ASet.of());
		assertObject(t.getSchemes()).json().is("['foo']");
		assertObject(t.getSchemes()).isType(List.class);

		t.addSchemes(null);
		assertObject(t.getSchemes()).json().is("['foo']");
		assertObject(t.getSchemes()).isType(List.class);
	}

	/**
	 * Test method for {@link Operation#schemes(java.lang.Object[])}.
	 */
	@Test
	public void testSchemes() {
		Operation t = new Operation();

		t.schemes(ASet.of("a"));
		t.schemes(ASet.of(new StringBuilder("b")));
		t.schemes((Object)new String[] {"c"});
		t.schemes((Object)new Object[] {new StringBuilder("d")});
		t.schemes("e");
		t.schemes("['f']");
		t.schemes("[]");
		t.schemes((Object)null);
		assertObject(t.getSchemes()).json().is("['a','b','c','d','e','f']");
		for (String s : t.getSchemes())
			assertObject(s).isType(String.class);
	}

	/**
	 * Test method for {@link Operation#deprecated(java.lang.Object)}.
	 */
	@Test
	public void testDeprecated() {
		Operation t = new Operation();

		t.deprecated(true);
		assertEquals(true, t.getDeprecated());
		assertObject(t.getDeprecated()).isType(Boolean.class);

		t.deprecated("true");
		assertEquals(true, t.getDeprecated());
		assertObject(t.getDeprecated()).isType(Boolean.class);

		t.deprecated(new StringBuilder("true"));
		assertEquals(true, t.getDeprecated());
		assertObject(t.getDeprecated()).isType(Boolean.class);

		t.deprecated(null);
		assertNull(t.getDeprecated());
	}

	/**
	 * Test method for {@link Operation#addSecurity(java.util.List)}.
	 */
	@Test
	public void testAddSecurity() {
		Operation t = new Operation();

		t.addSecurity(ASet.of(AMap.of("foo",AList.of("bar"))));
		assertObject(t.getSecurity()).json().is("[{foo:['bar']}]");
		assertObject(t.getSecurity()).isType(List.class);

		t.addSecurity(ASet.of());
		assertObject(t.getSecurity()).json().is("[{foo:['bar']}]");
		assertObject(t.getSecurity()).isType(List.class);

		t.addSecurity(null);
		assertObject(t.getSecurity()).json().is("[{foo:['bar']}]");
		assertObject(t.getSecurity()).isType(List.class);
	}

	/**
	 * Test method for {@link Operation#security(java.lang.String, java.lang.String[])}.
	 */
	@Test
	public void testSecurity() {
		Operation t = new Operation();

		t.security("a", "a1", "a2");
		t.security("b");
		t.security("c", (String)null);
		t.security(null, "d");

		assertObject(t.getSecurity()).json().is("[{a:['a1','a2']},{b:[]},{c:[null]},{null:['d']}]");
	}

	/**
	 * Test method for {@link Operation#securities(java.lang.Object[])}.
	 */
	@Test
	public void testSecurities() {
		Operation t = new Operation();

		t.securities(ASet.of(AMap.of("a1",AList.of("a2"))));
		t.securities(AMap.of("b1",AList.of("b2")));
		t.securities("{c1:['c2']}");
		t.securities(new StringBuilder("{d1:['d2']}"));
		t.securities((Object)new String[]{"{e1:['e2']}"});
		t.securities((Object)ASet.of("{f1:['f2']}"));
		t.securities("[{g1:['g2']}]");
		t.securities("[]");
		t.securities((Object)null);
		assertObject(t.getSecurity()).json().is("[{a1:['a2']},{b1:['b2']},{c1:['c2']},{d1:['d2']},{e1:['e2']},{f1:['f2']},{g1:['g2']}]");
		assertObject(t.getSecurity()).isType(List.class);
	}

	/**
	 * Test method for {@link Operation#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		Operation t = new Operation();

		t
			.set("consumes", ASet.of(MediaType.of("text/a")))
			.set("deprecated", true)
			.set("description", "b")
			.set("externalDocs", externalDocumentation("c"))
			.set("operationId", "d")
			.set("parameters", ASet.of(parameterInfo("e1","e2")))
			.set("produces", ASet.of(MediaType.of("text/f")))
			.set("responses", AMap.of(1,responseInfo("g")))
			.set("schemes", ASet.of("h"))
			.set("security", ASet.of(AMap.of("i1",AList.of("i2"))))
			.set("summary", "j")
			.set("tags", ASet.of("k"))
			.set("$ref", "ref");

		assertObject(t).json().is("{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");

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

		assertObject(t).json().is("{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");

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

		assertObject(t).json().is("{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}");

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

		assertObject(t.get("consumes", Object.class)).isType(List.class);
		assertObject(t.get("consumes", List.class).get(0)).isType(MediaType.class);
		assertObject(t.get("deprecated", Object.class)).isType(Boolean.class);
		assertObject(t.get("description", Object.class)).isType(String.class);
		assertObject(t.get("externalDocs", Object.class)).isType(ExternalDocumentation.class);
		assertObject(t.get("operationId", Object.class)).isType(String.class);
		assertObject(t.get("parameters", Object.class)).isType(List.class);
		assertObject(t.get("parameters", List.class).get(0)).isType(ParameterInfo.class);
		assertObject(t.get("produces", Object.class)).isType(List.class);
		assertObject(t.get("produces", List.class).get(0)).isType(MediaType.class);
		assertObject(t.get("responses", Object.class)).isType(Map.class);
		assertObject(t.get("responses", Map.class).keySet().iterator().next()).isType(String.class);
		assertObject(t.get("responses", Map.class).values().iterator().next()).isType(ResponseInfo.class);
		assertObject(t.get("schemes", Object.class)).isType(List.class);
		assertObject(t.get("security", Object.class)).isType(List.class);
		assertObject(t.get("summary", Object.class)).isType(String.class);
		assertObject(t.get("tags", Object.class)).isType(List.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, Operation.class)).json().is(s);
	}
}
