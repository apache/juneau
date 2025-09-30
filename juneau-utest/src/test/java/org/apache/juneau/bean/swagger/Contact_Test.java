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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
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

		// General - Combined assertBean test
		assertBean(
			t.setName("a").setEmail("b").setUrl(URI.create("http://c")),
			"name,email,url",
			"a,b,http://c"
		);

		// Null cases
		assertBean(
			t.setName(null).setEmail(null),
			"name,email",
			"<null>,<null>"
		);
	}

	/**
	 * Test method for {@link Contact#set(String, Object)}.
	 */
	@Test void b01_set() {
		var t = new Contact();

		t
			.set("email", "a")
			.set("name", "b")
			.set("$ref", "c")
			.set("url", "d");

		assertBean(t, "email,name,$ref,url", "a,b,c,d");

		t
			.set("email", Utils.sb("a"))
			.set("name", Utils.sb("b"))
			.set("$ref", Utils.sb("c"))
			.set("url", Utils.sb("d"));

		assertBean(t, "email,name,$ref,url", "a,b,c,d");

		assertMapped(
			t, (obj,prop) -> obj.get(prop, String.class),
			"email,name,$ref,url",
			"a,b,c,d"
		);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		assertBean(JsonParser.DEFAULT.parse("{name:'a',url:'b',email:'c','$ref':'d'}", Contact.class), "name,url,email,$ref", "a,b,c,d");
	}

	@Test void b03_copy() {
		var t = new Contact();

		t = t.copy();

		assertBean(t, "email,name,url", "<null>,<null>,<null>");

		t
			.set("name", "a")
			.set("url", "b")
			.set("email", "c")
			.set("$ref", "d")
			.copy();

		assertBean(t, "name,url,email,$ref", "a,b,c,d");
	}

	@Test void b04_keySet() {
		var t = new Contact();

		assertEmpty(t.keySet());

		t
			.set("name", "a")
			.set("url", "b")
			.set("email", "c")
			.set("$ref", "d");

		assertList(t.keySet(), "$ref", "email", "name", "url");
	}
}