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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SecurityScheme}.
 */
class SecurityScheme_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		SecurityScheme t = new SecurityScheme();
		assertEquals("foo", t.setType("foo").getType());
		assertString(t.setType(null).getType()).isNull();
		assertEquals("foo", t.setDescription("foo").getDescription());
		assertString(t.setDescription(null).getDescription()).isNull();
		assertEquals("foo", t.setName("foo").getName());
		assertString(t.setName(null).getName()).isNull();
		assertEquals("foo", t.setIn("foo").getIn());
		assertString(t.setIn(null).getIn()).isNull();
		assertEquals("foo", t.setFlow("foo").getFlow());
		assertString(t.setFlow(null).getFlow()).isNull();
		assertEquals("foo", t.setAuthorizationUrl("foo").getAuthorizationUrl());
		assertString(t.setAuthorizationUrl(null).getAuthorizationUrl()).isNull();
		assertEquals("foo", t.setTokenUrl("foo").getTokenUrl());
		assertString(t.setTokenUrl(null).getTokenUrl()).isNull();
		assertJson(t.setScopes(map("foo","bar")).getScopes(), "{foo:'bar'}");
		assertJson(t.setScopes(map()).getScopes(), "{}");
		assertNull(t.setScopes((Map<String,String>)null).getScopes());
	}

	/**
	 * Test method for {@link SecurityScheme#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		SecurityScheme t = new SecurityScheme();

		t
			.set("authorizationUrl", "a")
			.set("description", "b")
			.set("flow", "c")
			.set("in", "d")
			.set("name", "e")
			.set("scopes", map("foo","bar"))
			.set("tokenUrl", "f")
			.set("type", "g")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}");

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

		assertObject(t).asJson().is("{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}");

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

		assertObject(t).asJson().is("{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}");

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
		assertObject(JsonParser.DEFAULT.parse(s, SecurityScheme.class)).asJson().is(s);
	}

	@Test void b02_copy() {
		SecurityScheme t = new SecurityScheme();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("authorizationUrl", "a")
			.set("description", "b")
			.set("flow", "c")
			.set("in", "d")
			.set("name", "e")
			.set("scopes", map("foo","bar"))
			.set("tokenUrl", "f")
			.set("type", "g")
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}");
	}

	@Test void b03_keySet() {
		SecurityScheme t = new SecurityScheme();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("authorizationUrl", "a")
			.set("description", "b")
			.set("flow", "c")
			.set("in", "d")
			.set("name", "e")
			.set("scopes", map("foo","bar"))
			.set("tokenUrl", "f")
			.set("type", "g")
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is("['authorizationUrl','description','flow','in','name','scopes','tokenUrl','type','$ref']");
	}
}