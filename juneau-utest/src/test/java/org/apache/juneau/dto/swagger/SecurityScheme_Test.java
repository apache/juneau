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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link SecurityScheme}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class SecurityScheme_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		SecurityScheme t = new SecurityScheme();
		assertString(t.setType("foo").getType()).is("foo");
		assertString(t.setType(null).getType()).isNull();
		assertString(t.setDescription("foo").getDescription()).is("foo");
		assertString(t.setDescription(null).getDescription()).isNull();
		assertString(t.setName("foo").getName()).is("foo");
		assertString(t.setName(null).getName()).isNull();
		assertString(t.setIn("foo").getIn()).is("foo");
		assertString(t.setIn(null).getIn()).isNull();
		assertString(t.setFlow("foo").getFlow()).is("foo");
		assertString(t.setFlow(null).getFlow()).isNull();
		assertString(t.setAuthorizationUrl("foo").getAuthorizationUrl()).is("foo");
		assertString(t.setAuthorizationUrl(null).getAuthorizationUrl()).isNull();
		assertString(t.setTokenUrl("foo").getTokenUrl()).is("foo");
		assertString(t.setTokenUrl(null).getTokenUrl()).isNull();
		assertObject(t.setScopes(map("foo","bar")).getScopes()).isType(Map.class).asJson().is("{foo:'bar'}");
		assertObject(t.setScopes(map()).getScopes()).isType(Map.class).asJson().is("{}");
		assertObject(t.setScopes((Map<String,String>)null).getScopes()).isNull();
	}

	/**
	 * Test method for {@link SecurityScheme#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
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

		assertString(t.get("authorizationUrl", String.class)).is("a");
		assertString(t.get("description", String.class)).is("b");
		assertString(t.get("flow", String.class)).is("c");
		assertString(t.get("in", String.class)).is("d");
		assertString(t.get("name", String.class)).is("e");
		assertString(t.get("scopes", String.class)).is("{foo:'bar'}");
		assertString(t.get("tokenUrl", String.class)).is("f");
		assertString(t.get("type", String.class)).is("g");
		assertString(t.get("$ref", String.class)).is("ref");

		assertObject(t.get("authorizationUrl", Object.class)).isType(String.class);
		assertObject(t.get("description", Object.class)).isType(String.class);
		assertObject(t.get("flow", Object.class)).isType(String.class);
		assertObject(t.get("in", Object.class)).isType(String.class);
		assertObject(t.get("name", Object.class)).isType(String.class);
		assertObject(t.get("scopes", Object.class)).isType(Map.class);
		assertObject(t.get("tokenUrl", Object.class)).isType(String.class);
		assertObject(t.get("type", Object.class)).isType(String.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, SecurityScheme.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
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

	@Test
	public void b03_keySet() throws Exception {
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
