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
 * Testcase for {@link Swagger}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class SwaggerTest {

	/**
	 * Test method for {@link Swagger#swagger(java.lang.Object)}.
	 */
	@Test
	public void testSwagger() {
		Swagger t = new Swagger();

		t.swagger("foo");
		assertEquals("foo", t.getSwagger());

		t.swagger(new StringBuilder("foo"));
		assertEquals("foo", t.getSwagger());
		assertObject(t.getSwagger()).isType(String.class);

		t.swagger(null);
		assertNull(t.getSwagger());
	}

	/**
	 * Test method for {@link Swagger#info(java.lang.Object)}.
	 */
	@Test
	public void testInfo() {
		Swagger t = new Swagger();

		t.info(info("foo", "bar"));
		assertObject(t.getInfo()).json().is("{title:'foo',version:'bar'}");

		t.info("{title:'foo',version:'bar'}");
		assertObject(t.getInfo()).json().is("{title:'foo',version:'bar'}");
		assertObject(t.getInfo()).isType(Info.class);

		t.info(null);
		assertNull(t.getInfo());
	}

	/**
	 * Test method for {@link Swagger#host(java.lang.Object)}.
	 */
	@Test
	public void testHost() {
		Swagger t = new Swagger();

		t.host("foo");
		assertEquals("foo", t.getHost());

		t.host(new StringBuilder("foo"));
		assertEquals("foo", t.getHost());
		assertObject(t.getHost()).isType(String.class);

		t.host(null);
		assertNull(t.getHost());
	}

	/**
	 * Test method for {@link Swagger#basePath(java.lang.Object)}.
	 */
	@Test
	public void testBasePath() {
		Swagger t = new Swagger();

		t.basePath("foo");
		assertEquals("foo", t.getBasePath());

		t.basePath(new StringBuilder("foo"));
		assertEquals("foo", t.getBasePath());
		assertObject(t.getBasePath()).isType(String.class);

		t.basePath(null);
		assertNull(t.getBasePath());
	}

	/**
	 * Test method for {@link Swagger#setSchemes(java.util.Collection)}.
	 */
	@Test
	public void testSetSchemes() {
		Swagger t = new Swagger();

		t.setSchemes(ASet.of("foo","bar"));
		assertObject(t.getSchemes()).json().is("['foo','bar']");
		assertObject(t.getSchemes()).isType(List.class);

		t.setSchemes(ASet.of());
		assertObject(t.getSchemes()).json().is("[]");
		assertObject(t.getSchemes()).isType(List.class);

		t.setSchemes(null);
		assertNull(t.getSchemes());
	}

	/**
	 * Test method for {@link Swagger#addSchemes(java.util.Collection)}.
	 */
	@Test
	public void testAddSchemes() {
		Swagger t = new Swagger();

		t.addSchemes(ASet.of("foo","bar"));
		assertObject(t.getSchemes()).json().is("['foo','bar']");
		assertObject(t.getSchemes()).isType(List.class);

		t.addSchemes(ASet.of());
		assertObject(t.getSchemes()).json().is("['foo','bar']");
		assertObject(t.getSchemes()).isType(List.class);

		t.addSchemes(null);
		assertObject(t.getSchemes()).json().is("['foo','bar']");
		assertObject(t.getSchemes()).isType(List.class);
	}

	/**
	 * Test method for {@link Swagger#schemes(java.lang.Object[])}.
	 */
	@Test
	public void testSchemes() {
		Swagger t = new Swagger();

		t.schemes(ASet.of("foo"));
		t.schemes(ASet.of(new StringBuilder("bar")));
		t.schemes((Object)new String[] {"baz"});
		t.schemes("['qux']");
		t.schemes("quux");
		t.schemes("[]");
		t.schemes((Object)null);

		assertObject(t.getSchemes()).json().is("['foo','bar','baz','qux','quux']");
	}

	/**
	 * Test method for {@link Swagger#setConsumes(java.util.Collection)}.
	 */
	@Test
	public void testSetConsumes() {
		Swagger t = new Swagger();

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
	 * Test method for {@link Swagger#addConsumes(java.util.Collection)}.
	 */
	@Test
	public void testAddConsumes() {
		Swagger t = new Swagger();

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
	 * Test method for {@link Swagger#consumes(java.lang.Object[])}.
	 */
	@Test
	public void testConsumes() {
		Swagger t = new Swagger();

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
	 * Test method for {@link Swagger#setProduces(java.util.Collection)}.
	 */
	@Test
	public void testSetProduces() {
		Swagger t = new Swagger();

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
	 * Test method for {@link Swagger#addProduces(java.util.Collection)}.
	 */
	@Test
	public void testAddProduces() {
		Swagger t = new Swagger();

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
	 * Test method for {@link Swagger#produces(java.lang.Object[])}.
	 */
	@Test
	public void testProduces() {
		Swagger t = new Swagger();

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
	 * Test method for {@link Swagger#setPaths(java.util.Map)}.
	 */
	@Test
	public void testSetPaths() {
		Swagger t = new Swagger();

		t.setPaths(AMap.of("foo", new OperationMap().append("bar",operation().summary("baz"))));
		assertObject(t.getPaths()).json().is("{foo:{bar:{summary:'baz'}}}");
		assertObject(t.getPaths()).isType(Map.class);

		t.setPaths(AMap.of());
		assertObject(t.getPaths()).json().is("{}");
		assertObject(t.getPaths()).isType(Map.class);

		t.setPaths(null);
		assertNull(t.getPaths());
	}

	/**
	 * Test method for {@link Swagger#addPaths(java.util.Map)}.
	 */
	@Test
	public void testAddPaths() {
		Swagger t = new Swagger();

		t.addPaths(AMap.of("foo",new OperationMap().append("bar",operation().summary("baz"))));
		assertObject(t.getPaths()).json().is("{foo:{bar:{summary:'baz'}}}");
		assertObject(t.getPaths()).isType(Map.class);

		t.addPaths(AMap.of());
		assertObject(t.getPaths()).json().is("{foo:{bar:{summary:'baz'}}}");
		assertObject(t.getPaths()).isType(Map.class);

		t.addPaths(null);
		assertObject(t.getPaths()).json().is("{foo:{bar:{summary:'baz'}}}");
		assertObject(t.getPaths()).isType(Map.class);
	}

	/**
	 * Test method for {@link Swagger#path(java.lang.String, java.lang.String, org.apache.juneau.dto.swagger.Operation)}.
	 */
	@Test
	public void testPath() {
		Swagger t = new Swagger();

		t.path("a", "a1", operation().description("a2"));
		t.path("b", null, null);

		assertObject(t.getPaths()).json().is("{a:{a1:{description:'a2'}},b:{null:null}}");
	}

	/**
	 * Test method for {@link Swagger#paths(java.lang.Object[])}.
	 */
	@Test
	public void testPaths() {
		Swagger t = new Swagger();

		t.paths(AMap.of("a",AMap.of("a1",operation().operationId("a2"))));
		t.paths(AMap.of("b",AMap.of("b1","{operationId:'b2'}")));
		t.paths(AMap.of("c","{c1:{operationId:'c2'}}"));
		t.paths("{d:{d1:{operationId:'d2'}}}");
		t.paths("{}");
		t.paths((Object[])null);

		assertObject(t.getPaths()).json().is("{a:{a1:{operationId:'a2'}},b:{b1:{operationId:'b2'}},c:{c1:{operationId:'c2'}},d:{d1:{operationId:'d2'}}}");
		for (Map<String,Operation> m : t.getPaths().values()) {
			for (Operation o : m.values()) {
				assertObject(o).isType(Operation.class);
			}
		}
	}

	/**
	 * Test method for {@link Swagger#setDefinitions(java.util.Map)}.
	 */
	@Test
	public void testSetDefinitions() {
		Swagger t = new Swagger();

		t.setDefinitions(AMap.of("foo",OMap.of("type","bar")));
		assertObject(t.getDefinitions()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getDefinitions()).isType(Map.class);

		t.setDefinitions(AMap.of());
		assertObject(t.getDefinitions()).json().is("{}");
		assertObject(t.getDefinitions()).isType(Map.class);

		t.setDefinitions(null);
		assertNull(t.getDefinitions());
	}

	/**
	 * Test method for {@link Swagger#addDefinitions(java.util.Map)}.
	 */
	@Test
	public void testAddDefinitions() {
		Swagger t = new Swagger();

		t.addDefinitions(AMap.of("foo",OMap.of("type", "bar")));
		assertObject(t.getDefinitions()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getDefinitions()).isType(Map.class);

		t.addDefinitions(AMap.of());
		assertObject(t.getDefinitions()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getDefinitions()).isType(Map.class);

		t.addDefinitions(null);
		assertObject(t.getDefinitions()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getDefinitions()).isType(Map.class);
	}

	/**
	 * Test method for {@link Swagger#definition(java.lang.String, org.apache.juneau.dto.swagger.SchemaInfo)}.
	 */
	@Test
	public void testDefinition() {
		Swagger t = new Swagger();

		t.definition("a", OMap.of("type","a1"));
		t.definition("b", (OMap)null);
		t.definition(null, OMap.of("type", "c1"));

		assertObject(t.getDefinitions()).json().is("{a:{type:'a1'},b:null,null:{type:'c1'}}");
	}

	/**
	 * Test method for {@link Swagger#definitions(java.lang.Object[])}.
	 */
	@Test
	public void testDefinitions() {
		Swagger t = new Swagger();

		t.definitions(AMap.of("a",schemaInfo().type("a1")));
		t.definitions(AMap.of("b","{type:'b1'}"));
		t.definitions("{c:{type:'c1'}}");
		t.definitions("{}");
		t.definitions((Object[])null);

		assertObject(t.getDefinitions()).json().is("{a:{type:'a1'},b:{type:'b1'},c:{type:'c1'}}");
	}

	/**
	 * Test method for {@link Swagger#setParameters(java.util.Map)}.
	 */
	@Test
	public void testSetParameters() {
		Swagger t = new Swagger();

		t.setParameters(AMap.of("foo",parameterInfo().name("bar")));
		assertObject(t.getParameters()).json().is("{foo:{name:'bar'}}");
		assertObject(t.getParameters()).isType(Map.class);

		t.setParameters(AMap.of());
		assertObject(t.getParameters()).json().is("{}");
		assertObject(t.getParameters()).isType(Map.class);

		t.setParameters(null);
		assertNull(t.getParameters());
	}

	/**
	 * Test method for {@link Swagger#addParameters(java.util.Map)}.
	 */
	@Test
	public void testAddParameters() {
		Swagger t = new Swagger();

		t.addParameters(AMap.of("foo",parameterInfo().name("bar")));
		assertObject(t.getParameters()).json().is("{foo:{name:'bar'}}");
		assertObject(t.getParameters()).isType(Map.class);

		t.addParameters(AMap.of());
		assertObject(t.getParameters()).json().is("{foo:{name:'bar'}}");
		assertObject(t.getParameters()).isType(Map.class);

		t.addParameters(null);
		assertObject(t.getParameters()).json().is("{foo:{name:'bar'}}");
		assertObject(t.getParameters()).isType(Map.class);
	}

	/**
	 * Test method for {@link Swagger#parameter(java.lang.String, org.apache.juneau.dto.swagger.ParameterInfo)}.
	 */
	@Test
	public void testParameter() {
		Swagger t = new Swagger();

		t.parameter("a", parameterInfo().in("a1"));
		t.parameter("b", null);
		t.parameter(null, parameterInfo().in("c1"));

		assertObject(t.getParameters()).json().is("{a:{'in':'a1'},b:null,null:{'in':'c1'}}");
	}

	/**
	 * Test method for {@link Swagger#parameters(java.lang.Object[])}.
	 */
	@Test
	public void testParameters() {
		Swagger t = new Swagger();

		t.parameters(AMap.of("a",parameterInfo("a1", "a2")));
		t.parameters(AMap.of("b","{in:'b1',name:'b2'}"));
		t.parameters("{c:{in:'c1',name:'c2'}}");
		t.parameters("{}");
		t.parameters((Object[])null);

		assertObject(t.getParameters()).json().is("{a:{'in':'a1',name:'a2'},b:{'in':'b1',name:'b2'},c:{'in':'c1',name:'c2'}}");
	}

	/**
	 * Test method for {@link Swagger#setResponses(java.util.Map)}.
	 */
	@Test
	public void testSetResponses() {
		Swagger t = new Swagger();

		t.setResponses(AMap.of("123",responseInfo("bar")));
		assertObject(t.getResponses()).json().is("{'123':{description:'bar'}}");
		assertObject(t.getResponses()).isType(Map.class);

		t.setResponses(AMap.of());
		assertObject(t.getResponses()).json().is("{}");
		assertObject(t.getResponses()).isType(Map.class);

		t.setResponses(null);
		assertNull(t.getResponses());
	}

	/**
	 * Test method for {@link Swagger#addResponses(java.util.Map)}.
	 */
	@Test
	public void testAddResponses() {
		Swagger t = new Swagger();

		t.addResponses(AMap.of("123",responseInfo("bar")));
		assertObject(t.getResponses()).json().is("{'123':{description:'bar'}}");
		assertObject(t.getResponses()).isType(Map.class);

		t.addResponses(AMap.of());
		assertObject(t.getResponses()).json().is("{'123':{description:'bar'}}");
		assertObject(t.getResponses()).isType(Map.class);

		t.addResponses(null);
		assertObject(t.getResponses()).json().is("{'123':{description:'bar'}}");
		assertObject(t.getResponses()).isType(Map.class);
	}

	/**
	 * Test method for {@link Swagger#response(java.lang.String, org.apache.juneau.dto.swagger.ResponseInfo)}.
	 */
	@Test
	public void testResponse() {
		Swagger t = new Swagger();

		t.response("a", responseInfo("a1"));
		t.response(null, responseInfo("b1"));
		t.response("c", null);

		assertObject(t.getResponses()).json().is("{a:{description:'a1'},null:{description:'b1'},c:null}");
	}

	/**
	 * Test method for {@link Swagger#responses(java.lang.Object[])}.
	 */
	@Test
	public void testResponses() {
		Swagger t = new Swagger();

		t.responses(AMap.of("a",responseInfo("a1")));
		t.responses(AMap.of("b","{description:'b1'}"));
		t.responses("{c:{description:'c1'}}");
		t.responses("{}");
		t.responses((Object[])null);

		assertObject(t.getResponses()).json().is("{a:{description:'a1'},b:{description:'b1'},c:{description:'c1'}}");
		for (ResponseInfo ri : t.getResponses().values())
			assertObject(ri).isType(ResponseInfo.class);
	}

	/**
	 * Test method for {@link Swagger#setSecurityDefinitions(java.util.Map)}.
	 */
	@Test
	public void testSetSecurityDefinitions() {
		Swagger t = new Swagger();

		t.setSecurityDefinitions(AMap.of("foo",securityScheme("bar")));
		assertObject(t.getSecurityDefinitions()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getSecurityDefinitions()).isType(Map.class);

		t.setSecurityDefinitions(AMap.of());
		assertObject(t.getSecurityDefinitions()).json().is("{}");
		assertObject(t.getSecurityDefinitions()).isType(Map.class);

		t.setSecurityDefinitions(null);
		assertNull(t.getSecurityDefinitions());
	}

	/**
	 * Test method for {@link Swagger#addSecurityDefinitions(java.util.Map)}.
	 */
	@Test
	public void testAddSecurityDefinitions() {
		Swagger t = new Swagger();

		t.addSecurityDefinitions(AMap.of("foo",securityScheme("bar")));
		assertObject(t.getSecurityDefinitions()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getSecurityDefinitions()).isType(Map.class);

		t.addSecurityDefinitions(AMap.of());
		assertObject(t.getSecurityDefinitions()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getSecurityDefinitions()).isType(Map.class);

		t.addSecurityDefinitions(null);
		assertObject(t.getSecurityDefinitions()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getSecurityDefinitions()).isType(Map.class);
	}

	/**
	 * Test method for {@link Swagger#securityDefinition(java.lang.String, org.apache.juneau.dto.swagger.SecurityScheme)}.
	 */
	@Test
	public void testSecurityDefinition() {
		Swagger t = new Swagger();

		t.securityDefinition("a", securityScheme("a1"));
		t.securityDefinition("b", null);
		t.securityDefinition(null, securityScheme("c1"));

		assertObject(t.getSecurityDefinitions()).json().is("{a:{type:'a1'},b:null,null:{type:'c1'}}");
	}

	/**
	 * Test method for {@link Swagger#securityDefinitions(java.lang.Object[])}.
	 */
	@Test
	public void testSecurityDefinitions() {
		Swagger t = new Swagger();

		t.securityDefinitions(AMap.of("a",securityScheme("a1")));
		t.securityDefinitions(AMap.of("b","{type:'b1'}"));
		t.securityDefinitions("{c:{type:'c1'}}");
		t.securityDefinitions("{}");
		t.securityDefinitions((Object[])null);

		assertObject(t.getSecurityDefinitions()).json().is("{a:{type:'a1'},b:{type:'b1'},c:{type:'c1'}}");
		for (SecurityScheme ss : t.getSecurityDefinitions().values())
			assertObject(ss).isType(SecurityScheme.class);
	}

	/**
	 * Test method for {@link Swagger#setSecurity(java.util.Collection)}.
	 */
	@Test
	public void testSetSecurity() {
		Swagger t = new Swagger();

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
	 * Test method for {@link Swagger#addSecurity(java.util.Collection)}.
	 */
	@Test
	public void testAddSecurity() {
		Swagger t = new Swagger();

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
	 * Test method for {@link Swagger#security(java.lang.String, java.lang.String[])}.
	 */
	@Test
	public void testSecurity() {
		Swagger t = new Swagger();

		t.security("a", "a1", "a2");
		t.security("b", (String)null);
		t.security("c");
		t.security(null, "d1", "d2");

		assertObject(t.getSecurity()).json().is("[{a:['a1','a2']},{b:[null]},{c:[]},{null:['d1','d2']}]");
	}

	/**
	 * Test method for {@link Swagger#securities(java.lang.Object[])}.
	 */
	@Test
	public void testSecurities() {
		Swagger t = new Swagger();
		//Collection<Map<String,List<String>>>
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
	 * Test method for {@link Swagger#setTags(java.util.Collection)}.
	 */
	@Test
	public void testSetTags() {
		Swagger t = new Swagger();

		t.setTags(ASet.of(tag("foo")));
		assertObject(t.getTags()).json().is("[{name:'foo'}]");
		assertObject(t.getTags()).isType(List.class);

		t.setTags(ASet.of());
		assertObject(t.getTags()).json().is("[]");
		assertObject(t.getTags()).isType(List.class);

		t.setTags(null);
		assertNull(t.getTags());
	}

	/**
	 * Test method for {@link Swagger#addTags(java.util.Collection)}.
	 */
	@Test
	public void testAddTags() {
		Swagger t = new Swagger();

		t.addTags(ASet.of(tag("foo")));
		assertObject(t.getTags()).json().is("[{name:'foo'}]");
		assertObject(t.getTags()).isType(List.class);

		t.addTags(ASet.of());
		assertObject(t.getTags()).json().is("[{name:'foo'}]");
		assertObject(t.getTags()).isType(List.class);

		t.addTags(null);
		assertObject(t.getTags()).json().is("[{name:'foo'}]");
		assertObject(t.getTags()).isType(List.class);
	}

	/**
	 * Test method for {@link Swagger#tags(java.lang.Object[])}.
	 */
	@Test
	public void testTags() {
		Swagger t = new Swagger();

		t.tags(ASet.of(tag("a")));
		t.tags(ASet.of("{name:'b'}"));
		t.tags((Object)new Tag[] {tag("c")});
		t.tags((Object)new String[] {"{name:'d'}"});
		t.tags("{name:'e'}");
		t.tags(new StringBuilder("{name:'f'}"));
		t.tags("[{name:'g'}]");
		t.tags("[]");
		t.tags((Object)null);
		assertObject(t.getTags()).json().is("[{name:'a'},{name:'b'},{name:'c'},{name:'d'},{name:'e'},{name:'f'},{name:'g'}]");
		assertObject(t.getTags()).isType(List.class);
		for (Tag tag : t.getTags())
			assertObject(tag).isType(Tag.class);
	}

	/**
	 * Test method for {@link Swagger#externalDocs(java.lang.Object)}.
	 */
	@Test
	public void testExternalDocs() {
		Swagger t = new Swagger();

		t.externalDocs(externalDocumentation("foo"));
		assertObject(t.getExternalDocs()).json().is("{url:'foo'}");

		t.externalDocs("{url:'foo'}");
		assertObject(t.getExternalDocs()).json().is("{url:'foo'}");
		assertObject(t.getExternalDocs()).isType(ExternalDocumentation.class);

		t.externalDocs(null);
		assertNull(t.getExternalDocs());
	}

	/**
	 * Test method for {@link Swagger#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
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

		assertObject(t).json().is("{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");

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

		assertObject(t).json().is("{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");

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

		assertObject(t).json().is("{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}");

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

		assertObject(t.get("basePath", Object.class)).isType(String.class);
		assertObject(t.get("consumes", Object.class)).isType(List.class);
		assertObject(t.get("consumes", List.class).get(0)).isType(MediaType.class);
		assertObject(t.get("definitions", Object.class)).isType(Map.class);
		assertObject(t.get("definitions", Map.class).values().iterator().next()).isType(OMap.class);
		assertObject(t.get("externalDocs", Object.class)).isType(ExternalDocumentation.class);
		assertObject(t.get("host", Object.class)).isType(String.class);
		assertObject(t.get("info", Object.class)).isType(Info.class);
		assertObject(t.get("parameters", Object.class)).isType(Map.class);
		assertObject(t.get("parameters", Map.class).values().iterator().next()).isType(ParameterInfo.class);
		assertObject(t.get("paths", Object.class)).isType(Map.class);
		assertObject(t.get("produces", Object.class)).isType(List.class);
		assertObject(t.get("consumes", List.class).get(0)).isType(MediaType.class);
		assertObject(t.get("responses", Object.class)).isType(Map.class);
		assertObject(t.get("responses", Map.class).values().iterator().next()).isType(ResponseInfo.class);
		assertObject(t.get("schemes", Object.class)).isType(List.class);
		assertObject(t.get("security", Object.class)).isType(List.class);
		assertObject(t.get("securityDefinitions", Object.class)).isType(Map.class);
		assertObject(t.get("securityDefinitions", Map.class).values().iterator().next()).isType(SecurityScheme.class);
		assertObject(t.get("swagger", Object.class)).isType(String.class);
		assertObject(t.get("tags", Object.class)).isType(List.class);
		assertObject(t.get("tags", List.class).get(0)).isType(Tag.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{swagger:'n',info:{title:'f1',version:'f2'},tags:[{name:'o'}],externalDocs:{url:'d'},basePath:'a',schemes:['k1'],consumes:['text/b'],produces:['text/i'],paths:{h:{h1:{operationId:'h2'}}},definitions:{c:{type:'c1'}},parameters:{g:{'in':'g1',name:'g2'}},responses:{j:{description:'j1'}},securityDefinitions:{m:{type:'m1'}},security:[{l1:['l2']}],'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, Swagger.class)).json().is(s);
	}

}
