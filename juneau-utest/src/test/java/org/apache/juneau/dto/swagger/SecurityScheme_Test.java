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
		var t = new SecurityScheme();

		// General
		assertBean(
			t.setType("a").setDescription("b").setName("c").setIn("d").setFlow("e")
			.setAuthorizationUrl("f").setTokenUrl("g").setScopes(map("h1","h2")),
			"type,description,name,in,flow,authorizationUrl,tokenUrl,scopes",
			"a,b,c,d,e,f,g,{h1=h2}"
		);

		// Edge cases for nulls and collections.
		assertNull(t.setType(null).getType());
		assertNull(t.setDescription(null).getDescription());
		assertNull(t.setName(null).getName());
		assertNull(t.setIn(null).getIn());
		assertNull(t.setFlow(null).getFlow());
		assertNull(t.setAuthorizationUrl(null).getAuthorizationUrl());
		assertNull(t.setTokenUrl(null).getTokenUrl());
		assertMap(t.setScopes(map()).getScopes());
		assertNull(t.setScopes((Map<String,String>)null).getScopes());
	}

	/**
	 * Test method for {@link SecurityScheme#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		var t = new SecurityScheme();

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

		// Comprehensive object state validation
		assertBean(t,
			"type,description,name,in,flow,authorizationUrl,tokenUrl,scopes,$ref",
			"g,b,e,d,c,a,f,{foo=bar},ref");

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

		assertBean(t,
			"type,description,name,in,flow,authorizationUrl,tokenUrl,scopes,$ref",
			"g,b,e,d,c,a,f,{foo=bar},ref");

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

		assertBean(t,
			"type,description,name,in,flow,authorizationUrl,tokenUrl,scopes,$ref",
			"g,b,e,d,c,a,f,{foo=bar},ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"authorizationUrl,description,flow,in,name,scopes,tokenUrl,type,$ref",
			"a,b,c,d,e,{foo:'bar'},f,g,ref");

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

		var s = "{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, SecurityScheme.class), s);
	}

	@Test void b02_copy() {
		var t = new SecurityScheme();

		t = t.copy();

		assertJson(t, "{}");

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

		assertBean(t,
			"type,description,name,in,flow,authorizationUrl,tokenUrl,scopes,$ref",
			"g,b,e,d,c,a,f,{foo=bar},ref");
	}

	@Test void b03_keySet() {
		var t = new SecurityScheme();

		assertSet(t.keySet());

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

		assertSet(t.keySet(), "authorizationUrl,description,flow,in,name,scopes,tokenUrl,type,$ref");
	}
}