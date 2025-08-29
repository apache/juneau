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

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Contact}.
 */
class Contact_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new Contact();

		assertBean(
			t.setName("a").setEmail("b").setUrl(URI.create("http://c")),
			"name,email,url",
			"a,b,http://c"
		);

		assertNull(t.setName(null).getName());
		assertNull(t.setEmail(null).getEmail());
	}

	/**
	 * Test method for {@link Contact#set(String, Object)}.
	 */
	@Test void b01_set() throws Exception {
		var t = new Contact();

		t
			.set("name", "a")
			.set("url", "b")
			.set("email", "c")
			.set("$ref", "d");

		assertBean(t, "name,url,email,$ref", "a,b,c,d");

		t
			.set("name", new StringBuilder("a"))
			.set("url", new StringBuilder("b"))
			.set("email", new StringBuilder("c"))
			.set("$ref", new StringBuilder("d"));

		assertBean(t, "name,url,email,$ref", "a,b,c,d");

		assertMapped(
			t, (obj,prop) -> obj.get(prop, String.class),
			"name,url,email,$ref",
			"a,b,c,d"
		);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		assertBean(JsonParser.DEFAULT.parse("{name:'a',url:'b',email:'c','$ref':'d'}", Contact.class), "name,url,email,$ref", "a,b,c,d");
	}

	@Test void b02_copy() {
		var t = new Contact();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("name", "a")
			.set("url", "b")
			.set("email", "c")
			.set("$ref", "d")
			.copy();

		assertBean(t, "name,url,email,$ref", "a,b,c,d");
	}

	@Test void b03_keySet() {
		var t = new Contact();

		assertJson(t.keySet(), "[]");

		t
			.set("name", "foo")
			.set("url", "bar")
			.set("email", "baz")
			.set("$ref", "qux");

		assertSet(t.keySet(), "email,name,url,$ref");
	}
}