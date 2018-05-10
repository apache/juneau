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

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Testcase for {@link SecurityScheme}.
 */
public class SecuritySchemeTest {

	/**
	 * Test method for {@link SecurityScheme#type(java.lang.Object)}.
	 */
	@Test
	public void testType() {
		SecurityScheme t = new SecurityScheme();
		
		t.type("foo");
		assertEquals("foo", t.getType());
		
		t.type(new StringBuilder("foo"));
		assertEquals("foo", t.getType());
		assertType(String.class, t.getType());
		
		t.type(null);
		assertNull(t.getType());
	}

	/**
	 * Test method for {@link SecurityScheme#description(java.lang.Object)}.
	 */
	@Test
	public void testDescription() {
		SecurityScheme t = new SecurityScheme();
		
		t.description("foo");
		assertEquals("foo", t.getDescription());
		
		t.description(new StringBuilder("foo"));
		assertEquals("foo", t.getDescription());
		assertType(String.class, t.getDescription());
		
		t.description(null);
		assertNull(t.getDescription());
	}

	/**
	 * Test method for {@link SecurityScheme#name(java.lang.Object)}.
	 */
	@Test
	public void testName() {
		SecurityScheme t = new SecurityScheme();
		
		t.name("foo");
		assertEquals("foo", t.getName());
		
		t.name(new StringBuilder("foo"));
		assertEquals("foo", t.getName());
		assertType(String.class, t.getName());
		
		t.name(null);
		assertNull(t.getName());
	}

	/**
	 * Test method for {@link SecurityScheme#in(java.lang.Object)}.
	 */
	@Test
	public void testIn() {
		SecurityScheme t = new SecurityScheme();
		
		t.in("foo");
		assertEquals("foo", t.getIn());
		
		t.in(new StringBuilder("foo"));
		assertEquals("foo", t.getIn());
		assertType(String.class, t.getIn());
		
		t.in(null);
		assertNull(t.getIn());
	}

	/**
	 * Test method for {@link SecurityScheme#flow(java.lang.Object)}.
	 */
	@Test
	public void testFlow() {
		SecurityScheme t = new SecurityScheme();
		
		t.flow("foo");
		assertEquals("foo", t.getFlow());
		
		t.flow(new StringBuilder("foo"));
		assertEquals("foo", t.getFlow());
		assertType(String.class, t.getFlow());
		
		t.flow(null);
		assertNull(t.getFlow());
	}

	/**
	 * Test method for {@link SecurityScheme#authorizationUrl(java.lang.Object)}.
	 */
	@Test
	public void testAuthorizationUrl() {
		SecurityScheme t = new SecurityScheme();
		
		t.authorizationUrl("foo");
		assertEquals("foo", t.getAuthorizationUrl());
		
		t.authorizationUrl(new StringBuilder("foo"));
		assertEquals("foo", t.getAuthorizationUrl());
		assertType(String.class, t.getAuthorizationUrl());
		
		t.authorizationUrl(null);
		assertNull(t.getAuthorizationUrl());
	}

	/**
	 * Test method for {@link SecurityScheme#tokenUrl(java.lang.Object)}.
	 */
	@Test
	public void testTokenUrl() {
		SecurityScheme t = new SecurityScheme();
		
		t.tokenUrl("foo");
		assertEquals("foo", t.getTokenUrl());
		
		t.tokenUrl(new StringBuilder("foo"));
		assertEquals("foo", t.getTokenUrl());
		assertType(String.class, t.getTokenUrl());
		
		t.tokenUrl(null);
		assertNull(t.getTokenUrl());
	}

	/**
	 * Test method for {@link SecurityScheme#setScopes(java.util.Map)}.
	 */
	@Test
	public void testSetScopes() {
		SecurityScheme t = new SecurityScheme();
		
		t.setScopes(new AMap<String,String>().append("foo","bar"));
		assertObjectEquals("{foo:'bar'}", t.getScopes());
		assertType(Map.class, t.getScopes());
		
		t.setScopes(new AMap<String,String>());
		assertObjectEquals("{}", t.getScopes());
		assertType(Map.class, t.getScopes());

		t.setScopes(null);
		assertNull(t.getScopes());
	}

	/**
	 * Test method for {@link SecurityScheme#addScopes(java.util.Map)}.
	 */
	@Test
	public void testAddScopes() {
		SecurityScheme t = new SecurityScheme();
		
		t.addScopes(new AMap<String,String>().append("foo","bar"));
		assertObjectEquals("{foo:'bar'}", t.getScopes());
		assertType(Map.class, t.getScopes());
		
		t.addScopes(new AMap<String,String>());
		assertObjectEquals("{foo:'bar'}", t.getScopes());
		assertType(Map.class, t.getScopes());

		t.addScopes(null);
		assertObjectEquals("{foo:'bar'}", t.getScopes());
		assertType(Map.class, t.getScopes());
	}

	/**
	 * Test method for {@link SecurityScheme#scopes(java.lang.Object[])}.
	 */
	@Test
	public void testScopes() {
		SecurityScheme t = new SecurityScheme();
		
		t.scopes(new AMap<String,String>().append("a", "a1"));
		t.scopes("{b:'b1'}");
		t.scopes("{}");
		t.scopes((Object[])null);
		
		assertObjectEquals("{a:'a1',b:'b1'}", t.getScopes());
	}

	/**
	 * Test method for {@link SecurityScheme#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		SecurityScheme t = new SecurityScheme();
		
		t
			.set("authorizationUrl", "a")
			.set("description", "b")
			.set("flow", "c")
			.set("in", "d")
			.set("name", "e")
			.set("scopes", new AMap<String,String>().append("foo","bar"))
			.set("tokenUrl", "f")
			.set("type", "g")
			.set("$ref", "ref");
	
		assertObjectEquals("{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}", t);
		
		t
			.set("authorizationUrl", "a")
			.set("description", "b")
			.set("flow", "c")
			.set("in", "d")
			.set("name", "e")
			.set("scopes", "{foo:'bar'}")
			.set("tokenUrl", "f")
			.set("type", "g")
			.set("$ref", "ref");
		
		assertObjectEquals("{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}", t);
		
		t
			.set("authorizationUrl", new StringBuilder("a"))
			.set("description", new StringBuilder("b"))
			.set("flow", new StringBuilder("c"))
			.set("in", new StringBuilder("d"))
			.set("name", new StringBuilder("e"))
			.set("scopes", new StringBuilder("{foo:'bar'}")) 
			.set("tokenUrl", new StringBuilder("f"))
			.set("type", new StringBuilder("g"))
			.set("$ref", new StringBuilder("ref"));
		
		assertObjectEquals("{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}", t);

		assertEquals("a", t.get("authorizationUrl", String.class));
		assertEquals("b", t.get("description", String.class));
		assertEquals("c", t.get("flow", String.class));
		assertEquals("d", t.get("in", String.class));
		assertEquals("e", t.get("name", String.class));
		assertEquals("{foo:'bar'}", t.get("scopes", String.class));
		assertEquals("f", t.get("tokenUrl", String.class));
		assertEquals("g", t.get("type", String.class));
		assertEquals("ref", t.get("$ref", String.class));
	
		assertType(String.class, t.get("authorizationUrl", Object.class));
		assertType(String.class, t.get("description", Object.class));
		assertType(String.class, t.get("flow", Object.class));
		assertType(String.class, t.get("in", Object.class));
		assertType(String.class, t.get("name", Object.class));
		assertType(Map.class, t.get("scopes", Object.class));
		assertType(String.class, t.get("tokenUrl", Object.class));
		assertType(String.class, t.get("type", Object.class));
		assertType(StringBuilder.class, t.get("$ref", Object.class));
	
		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
		
		String s = "{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}";
		assertObjectEquals(s, JsonParser.DEFAULT.parse(s, SecurityScheme.class));
	}
}
