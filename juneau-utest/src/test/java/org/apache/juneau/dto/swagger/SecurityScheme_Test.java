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

		// Basic property setters
		assertBean(
			t.setType("a").setDescription("b").setName("c").setIn("d").setFlow("e")
			.setAuthorizationUrl("f").setTokenUrl("g").setScopes(map("h1","h2")),
			"type,description,name,in,flow,authorizationUrl,tokenUrl,scopes",
			"a,b,c,d,e,f,g,{h1=h2}"
		);

		// Null values
		assertBean(
			t.setType(null).setDescription(null).setName(null).setIn(null).setFlow(null)
				.setAuthorizationUrl(null).setTokenUrl(null).setScopes((Map<String,String>)null),
			"type,description,name,in,flow,authorizationUrl,tokenUrl,scopes",
			"null,null,null,null,null,null,null,null"
		);

		// Other methods
		assertEmpty(t.setScopes(map()).getScopes());
	}

	/**
	 * Test method for {@link SecurityScheme#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() {
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
			.set("authorizationUrl", sb("a"))
			.set("description", sb("b"))
			.set("flow", sb("c"))
			.set("in", sb("d"))
			.set("name", sb("e"))
			.set("scopes", sb("{foo:'bar'}"))
			.set("tokenUrl", sb("f"))
			.set("type", sb("g"))
			.set("$ref", sb("ref"));

		assertBean(t,
			"type,description,name,in,flow,authorizationUrl,tokenUrl,scopes,$ref",
			"g,b,e,d,c,a,f,{foo=bar},ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"authorizationUrl,description,flow,in,name,scopes,tokenUrl,type,$ref",
			"a,b,c,d,e,{foo:'bar'},f,g,ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"authorizationUrl,description,flow,in,name,scopes,tokenUrl,type,$ref",
			"String,String,String,String,String,LinkedHashMap,String,String,StringBuilder");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{type:'g',description:'b',name:'e','in':'d',flow:'c',authorizationUrl:'a',tokenUrl:'f',scopes:{foo:'bar'},'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, SecurityScheme.class), s);
	}

	@Test void b03_copy() {
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

	@Test void b04_keySet() {
		var t = new SecurityScheme();

		assertEmpty(t.keySet());

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