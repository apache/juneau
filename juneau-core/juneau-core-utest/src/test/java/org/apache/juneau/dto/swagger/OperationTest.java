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

import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Testcase for {@link Operation}.
 */
public class OperationTest {

	/**
	 * Test method for {@link Operation#setTags(java.util.Collection)}.
	 */
	@Test
	public void testSetTags() {
		Operation t = new Operation();

		t.setTags(ASet.of("foo","bar"));
		assertObjectEquals("['foo','bar']", t.getTags());
		assertInstanceOf(List.class, t.getTags());

		t.setTags(ASet.of());
		assertObjectEquals("[]", t.getTags());
		assertInstanceOf(List.class, t.getTags());

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
		assertObjectEquals("['foo','bar']", t.getTags());
		assertInstanceOf(List.class, t.getTags());

		t.addTags(ASet.of());
		assertObjectEquals("['foo','bar']", t.getTags());
		assertInstanceOf(List.class, t.getTags());

		t.addTags(null);
		assertObjectEquals("['foo','bar']", t.getTags());
		assertInstanceOf(List.class, t.getTags());
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
		assertObjectEquals("['a','b','c','d','e','f']", t.getTags());
		for (String s : t.getTags())
			assertInstanceOf(String.class, s);
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
		assertInstanceOf(String.class, t.getSummary());

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
		assertInstanceOf(String.class, t.getDescription());

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
		assertObjectEquals("{url:'foo'}", t.getExternalDocs());

		t.externalDocs("{url:'foo'}");
		assertObjectEquals("{url:'foo'}", t.getExternalDocs());
		assertInstanceOf(ExternalDocumentation.class, t.getExternalDocs());

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
		assertInstanceOf(String.class, t.getOperationId());

		t.operationId(null);
		assertNull(t.getOperationId());
	}

	/**
	 * Test method for {@link Operation#setConsumes(java.util.Collection)}.
	 */
	@Test
	public void testSetConsumes() {
		Operation t = new Operation();

		t.setConsumes(ASet.of(MediaType.forString("text/foo")));
		assertObjectEquals("['text/foo']", t.getConsumes());
		assertInstanceOf(List.class, t.getConsumes());

		t.setConsumes(ASet.of());
		assertObjectEquals("[]", t.getConsumes());
		assertInstanceOf(List.class, t.getConsumes());

		t.setConsumes(null);
		assertNull(t.getConsumes());
	}

	/**
	 * Test method for {@link Operation#addConsumes(java.util.Collection)}.
	 */
	@Test
	public void testAddConsumes() {
		Operation t = new Operation();

		t.addConsumes(ASet.of(MediaType.forString("text/foo")));
		assertObjectEquals("['text/foo']", t.getConsumes());
		assertInstanceOf(List.class, t.getConsumes());

		t.addConsumes(ASet.of());
		assertObjectEquals("['text/foo']", t.getConsumes());
		assertInstanceOf(List.class, t.getConsumes());

		t.addConsumes(null);
		assertObjectEquals("['text/foo']", t.getConsumes());
		assertInstanceOf(List.class, t.getConsumes());
	}

	/**
	 * Test method for {@link Operation#consumes(java.lang.Object[])}.
	 */
	@Test
	public void testConsumes() {
		Operation t = new Operation();

		t.consumes(ASet.of(MediaType.forString("text/foo")));
		t.consumes(MediaType.forString("text/bar"));
		t.consumes("text/baz");
		t.consumes(new StringBuilder("text/qux"));
		t.consumes((Object)new String[]{"text/quux"});
		t.consumes((Object)ASet.of("text/quuux"));
		t.consumes("['text/quuuux']");
		t.consumes("[]");
		t.consumes((Object)null);
		assertObjectEquals("['text/foo','text/bar','text/baz','text/qux','text/quux','text/quuux','text/quuuux']", t.getConsumes());
		assertInstanceOf(List.class, t.getConsumes());
		for (MediaType mt : t.getConsumes())
			assertInstanceOf(MediaType.class, mt);
	}

	/**
	 * Test method for {@link Operation#setProduces(java.util.Collection)}.
	 */
	@Test
	public void testSetProduces() {
		Operation t = new Operation();

		t.setProduces(ASet.of(MediaType.forString("text/foo")));
		assertObjectEquals("['text/foo']", t.getProduces());
		assertInstanceOf(List.class, t.getProduces());

		t.setProduces(ASet.of());
		assertObjectEquals("[]", t.getProduces());
		assertInstanceOf(List.class, t.getProduces());

		t.setProduces(null);
		assertNull(t.getProduces());
	}

	/**
	 * Test method for {@link Operation#addProduces(java.util.Collection)}.
	 */
	@Test
	public void testAddProduces() {
		Operation t = new Operation();

		t.addProduces(ASet.of(MediaType.forString("text/foo")));
		assertObjectEquals("['text/foo']", t.getProduces());
		assertInstanceOf(List.class, t.getProduces());

		t.addProduces(ASet.of());
		assertObjectEquals("['text/foo']", t.getProduces());
		assertInstanceOf(List.class, t.getProduces());

		t.addProduces(null);
		assertObjectEquals("['text/foo']", t.getProduces());
		assertInstanceOf(List.class, t.getProduces());
	}

	/**
	 * Test method for {@link Operation#produces(java.lang.Object[])}.
	 */
	@Test
	public void testProduces() {
		Operation t = new Operation();

		t.produces(ASet.of(MediaType.forString("text/foo")));
		t.produces(MediaType.forString("text/bar"));
		t.produces("text/baz");
		t.produces(new StringBuilder("text/qux"));
		t.produces((Object)new String[]{"text/quux"});
		t.produces((Object)ASet.of("text/quuux"));
		t.produces("['text/quuuux']");
		t.produces("[]");
		t.produces((Object)null);
		assertObjectEquals("['text/foo','text/bar','text/baz','text/qux','text/quux','text/quuux','text/quuuux']", t.getProduces());
		assertInstanceOf(List.class, t.getProduces());
		for (MediaType mt : t.getProduces())
			assertInstanceOf(MediaType.class, mt);
	}

	/**
	 * Test method for {@link Operation#setParameters(java.util.Collection)}.
	 */
	@Test
	public void testSetParameters() {
		Operation t = new Operation();

		t.setParameters(ASet.of(parameterInfo("foo","bar")));
		assertObjectEquals("[{'in':'foo',name:'bar'}]", t.getParameters());
		assertInstanceOf(List.class, t.getParameters());

		t.setParameters(ASet.of());
		assertObjectEquals("[]", t.getParameters());
		assertInstanceOf(List.class, t.getParameters());

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
		assertObjectEquals("[{'in':'foo',name:'bar'}]", t.getParameters());
		assertInstanceOf(List.class, t.getParameters());

		t.addParameters(ASet.of());
		assertObjectEquals("[{'in':'foo',name:'bar'}]", t.getParameters());
		assertInstanceOf(List.class, t.getParameters());

		t.addParameters(null);
		assertObjectEquals("[{'in':'foo',name:'bar'}]", t.getParameters());
		assertInstanceOf(List.class, t.getParameters());
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
		assertObjectEquals("[{'in':'a1',name:'a2'},{'in':'b1',name:'b2'},{'in':'c1',name:'c2'},{'in':'d1',name:'d2'},{'in':'e1',name:'e2'},{'in':'f1',name:'f2'},{'in':'g1',name:'g2'}]", t.getParameters());
		assertInstanceOf(List.class, t.getParameters());
		for (ParameterInfo pi : t.getParameters())
			assertInstanceOf(ParameterInfo.class, pi);
	}

	/**
	 * Test method for {@link Operation#setResponses(java.util.Map)}.
	 */
	@Test
	public void testSetResponses() {
		Operation t = new Operation();

		t.setResponses(AMap.of("123",responseInfo("bar")));
		assertObjectEquals("{'123':{description:'bar'}}", t.getResponses());
		assertInstanceOf(Map.class, t.getResponses());

		t.setResponses(AMap.of());
		assertObjectEquals("{}", t.getResponses());
		assertInstanceOf(Map.class, t.getResponses());

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
		assertObjectEquals("{'123':{description:'bar'}}", t.getResponses());
		assertInstanceOf(Map.class, t.getResponses());

		t.addResponses(AMap.of());
		assertObjectEquals("{'123':{description:'bar'}}", t.getResponses());
		assertInstanceOf(Map.class, t.getResponses());

		t.addResponses(null);
		assertObjectEquals("{'123':{description:'bar'}}", t.getResponses());
		assertInstanceOf(Map.class, t.getResponses());
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
		assertObjectEquals("{'1':{description:'foo'},null:{description:'bar'},'2':null}", t.getResponses());
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

		assertObjectEquals("{'1':{description:'a'},'2':{description:'b'},'3':{description:'c'}}", t.getResponses());
		for (Map.Entry<String,ResponseInfo> e : t.getResponses().entrySet()) {
			assertInstanceOf(String.class, e.getKey());
			assertInstanceOf(ResponseInfo.class, e.getValue());
		}
	}

	/**
	 * Test method for {@link Operation#setSchemes(java.util.Collection)}.
	 */
	@Test
	public void testSetSchemes() {
		Operation t = new Operation();

		t.setSchemes(ASet.of("foo"));
		assertObjectEquals("['foo']", t.getSchemes());
		assertInstanceOf(List.class, t.getSchemes());

		t.setSchemes(ASet.of());
		assertObjectEquals("[]", t.getSchemes());
		assertInstanceOf(List.class, t.getSchemes());

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
		assertObjectEquals("[{foo:['bar']}]", t.getSecurity());
		assertInstanceOf(List.class, t.getSecurity());

		t.setSecurity(ASet.of());
		assertObjectEquals("[]", t.getSecurity());
		assertInstanceOf(List.class, t.getSecurity());

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
		assertObjectEquals("['foo']", t.getSchemes());
		assertInstanceOf(List.class, t.getSchemes());

		t.addSchemes(ASet.of());
		assertObjectEquals("['foo']", t.getSchemes());
		assertInstanceOf(List.class, t.getSchemes());

		t.addSchemes(null);
		assertObjectEquals("['foo']", t.getSchemes());
		assertInstanceOf(List.class, t.getSchemes());
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
		assertObjectEquals("['a','b','c','d','e','f']", t.getSchemes());
		for (String s : t.getSchemes())
			assertInstanceOf(String.class, s);
	}

	/**
	 * Test method for {@link Operation#deprecated(java.lang.Object)}.
	 */
	@Test
	public void testDeprecated() {
		Operation t = new Operation();

		t.deprecated(true);
		assertEquals(true, t.getDeprecated());
		assertInstanceOf(Boolean.class, t.getDeprecated());

		t.deprecated("true");
		assertEquals(true, t.getDeprecated());
		assertInstanceOf(Boolean.class, t.getDeprecated());

		t.deprecated(new StringBuilder("true"));
		assertEquals(true, t.getDeprecated());
		assertInstanceOf(Boolean.class, t.getDeprecated());

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
		assertObjectEquals("[{foo:['bar']}]", t.getSecurity());
		assertInstanceOf(List.class, t.getSecurity());

		t.addSecurity(ASet.of());
		assertObjectEquals("[{foo:['bar']}]", t.getSecurity());
		assertInstanceOf(List.class, t.getSecurity());

		t.addSecurity(null);
		assertObjectEquals("[{foo:['bar']}]", t.getSecurity());
		assertInstanceOf(List.class, t.getSecurity());
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

		assertObjectEquals("[{a:['a1','a2']},{b:[]},{c:[null]},{null:['d']}]", t.getSecurity());
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
		assertObjectEquals("[{a1:['a2']},{b1:['b2']},{c1:['c2']},{d1:['d2']},{e1:['e2']},{f1:['f2']},{g1:['g2']}]", t.getSecurity());
		assertInstanceOf(List.class, t.getSecurity());
	}

	/**
	 * Test method for {@link Operation#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		Operation t = new Operation();

		t
			.set("consumes", ASet.of(MediaType.forString("text/a")))
			.set("deprecated", true)
			.set("description", "b")
			.set("externalDocs", externalDocumentation("c"))
			.set("operationId", "d")
			.set("parameters", ASet.of(parameterInfo("e1","e2")))
			.set("produces", ASet.of(MediaType.forString("text/f")))
			.set("responses", AMap.of(1,responseInfo("g")))
			.set("schemes", ASet.of("h"))
			.set("security", ASet.of(AMap.of("i1",AList.of("i2"))))
			.set("summary", "j")
			.set("tags", ASet.of("k"))
			.set("$ref", "ref");

		assertObjectEquals("{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}", t);

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

		assertObjectEquals("{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}", t);

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

		assertObjectEquals("{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}", t);

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

		assertInstanceOf(List.class, t.get("consumes", Object.class));
		assertInstanceOf(MediaType.class, t.get("consumes", List.class).get(0));
		assertInstanceOf(Boolean.class, t.get("deprecated", Object.class));
		assertInstanceOf(String.class, t.get("description", Object.class));
		assertInstanceOf(ExternalDocumentation.class, t.get("externalDocs", Object.class));
		assertInstanceOf(String.class, t.get("operationId", Object.class));
		assertInstanceOf(List.class, t.get("parameters", Object.class));
		assertInstanceOf(ParameterInfo.class, t.get("parameters", List.class).get(0));
		assertInstanceOf(List.class, t.get("produces", Object.class));
		assertInstanceOf(MediaType.class, t.get("produces", List.class).get(0));
		assertInstanceOf(Map.class, t.get("responses", Object.class));
		assertInstanceOf(String.class, t.get("responses", Map.class).keySet().iterator().next());
		assertInstanceOf(ResponseInfo.class, t.get("responses", Map.class).values().iterator().next());
		assertInstanceOf(List.class, t.get("schemes", Object.class));
		assertInstanceOf(List.class, t.get("security", Object.class));
		assertInstanceOf(String.class, t.get("summary", Object.class));
		assertInstanceOf(List.class, t.get("tags", Object.class));
		assertInstanceOf(StringBuilder.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{operationId:'d',summary:'j',description:'b',tags:['k'],externalDocs:{url:'c'},consumes:['text/a'],produces:['text/f'],parameters:[{'in':'e1',name:'e2'}],responses:{'1':{description:'g'}},schemes:['h'],deprecated:true,security:[{i1:['i2']}],'$ref':'ref'}";
		assertObjectEquals(s, JsonParser.DEFAULT.parse(s, Operation.class));
	}
}
