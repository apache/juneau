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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link SecurityScheme}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class SecurityScheme_Test {

	/**
	 * Test method for {@link SecurityScheme#type(java.lang.Object)}.
	 */
	@Test
	public void a01_type() {
		SecurityScheme t = new SecurityScheme();

		t.type("foo");
		assertString(t.type()).is("foo");

		t.type(null);
		assertString(t.type()).isNull();
	}

	/**
	 * Test method for {@link SecurityScheme#description(java.lang.Object)}.
	 */
	@Test
	public void a02_description() {
		SecurityScheme t = new SecurityScheme();

		t.description("foo");
		assertString(t.description()).is("foo");

		t.description(null);
		assertString(t.description()).isNull();
	}

	/**
	 * Test method for {@link SecurityScheme#name(java.lang.Object)}.
	 */
	@Test
	public void a03_name() {
		SecurityScheme t = new SecurityScheme();

		t.name("foo");
		assertString(t.name()).is("foo");

		t.name(null);
		assertString(t.name()).isNull();
	}

	/**
	 * Test method for {@link SecurityScheme#in(java.lang.Object)}.
	 */
	@Test
	public void a04_in() {
		SecurityScheme t = new SecurityScheme();

		t.in("foo");
		assertString(t.in()).is("foo");

		t.in(null);
		assertString(t.in()).isNull();
	}

	/**
	 * Test method for {@link SecurityScheme#flow(java.lang.Object)}.
	 */
	@Test
	public void a05_flow() {
		SecurityScheme t = new SecurityScheme();

		t.flow("foo");
		assertString(t.flow()).is("foo");

		t.flow(null);
		assertString(t.flow()).isNull();
	}

	/**
	 * Test method for {@link SecurityScheme#authorizationUrl(java.lang.Object)}.
	 */
	@Test
	public void a06_authorizationUrl() {
		SecurityScheme t = new SecurityScheme();

		t.authorizationUrl("foo");
		assertString(t.authorizationUrl()).is("foo");

		t.authorizationUrl(null);
		assertString(t.authorizationUrl()).isNull();
	}

	/**
	 * Test method for {@link SecurityScheme#tokenUrl(java.lang.Object)}.
	 */
	@Test
	public void a07_tokenUrl() {
		SecurityScheme t = new SecurityScheme();

		t.tokenUrl("foo");
		assertString(t.tokenUrl()).is("foo");

		t.tokenUrl(null);
		assertString(t.tokenUrl()).isNull();
	}

	/**
	 * Test method for {@link SecurityScheme#setScopes(java.util.Map)}.
	 */
	@Test
	public void a08_scopes() {
		SecurityScheme t = new SecurityScheme();

		t.scopes(AMap.of("foo","bar"));
		assertOptional(t.scopes()).isType(Map.class).asJson().is("{foo:'bar'}");

		t.scopes(AMap.create());
		assertOptional(t.scopes()).isType(Map.class).asJson().is("{}");

		t.scopes((Map<String,String>)null);
		assertOptional(t.scopes()).isNull();

		t.addScopes(AMap.of("foo","bar"));
		assertOptional(t.scopes()).isType(Map.class).asJson().is("{foo:'bar'}");

		t.addScopes(AMap.create());
		assertOptional(t.scopes()).isType(Map.class).asJson().is("{foo:'bar'}");

		t.addScopes(null);
		assertOptional(t.scopes()).isType(Map.class).asJson().is("{foo:'bar'}");
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
			.set("scopes", AMap.of("foo","bar"))
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
			.set("scopes", AMap.of("foo","bar"))
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
			.set("scopes", AMap.of("foo","bar"))
			.set("tokenUrl", "f")
			.set("type", "g")
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is("['authorizationUrl','description','flow','in','name','scopes','tokenUrl','type','$ref']");
	}
}
