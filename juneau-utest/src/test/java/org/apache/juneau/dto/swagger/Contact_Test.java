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

import static org.junit.jupiter.api.Assertions.*;
import static org.apache.juneau.TestUtils.*;

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
		assertEquals("foo", t.setName("foo").getName());
		assertNull(t.setName(null).getName());
		assertString("http://bar", t.setUrl(URI.create("http://bar")).getUrl());
		assertEquals("foo", t.setEmail("foo").getEmail());
		assertNull(t.setEmail(null).getEmail());
	}

	/**
	 * Test method for {@link Contact#set(String, Object)}.
	 */
	@Test void b01_set() throws Exception {
		var t = new Contact();

		t
			.set("name", "foo")
			.set("url", "bar")
			.set("email", "baz")
			.set("$ref", "qux");

		assertJson(t, "{name:'foo',url:'bar',email:'baz','$ref':'qux'}");

		t
			.set("name", new StringBuilder("foo"))
			.set("url", new StringBuilder("bar"))
			.set("email", new StringBuilder("baz"))
			.set("$ref", new StringBuilder("qux"));

		assertJson(t, "{name:'foo',url:'bar',email:'baz','$ref':'qux'}");

		assertEquals("foo", t.get("name", String.class));
		assertString("bar", t.get("url", URI.class));
		assertEquals("baz", t.get("email", String.class));
		assertEquals("qux", t.get("$ref", String.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		assertJson(JsonParser.DEFAULT.parse("{name:'foo',url:'bar',email:'baz','$ref':'qux'}", Contact.class), "{name:'foo',url:'bar',email:'baz','$ref':'qux'}");
	}

	@Test void b02_copy() {
		var t = new Contact();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("name", "foo")
			.set("url", "bar")
			.set("email", "baz")
			.set("$ref", "qux")
			.copy();

		assertJson(t, "{name:'foo',url:'bar',email:'baz','$ref':'qux'}");
	}

	@Test void b03_keySet() {
		var t = new Contact();

		assertJson(t.keySet(), "[]");

		t
			.set("name", "foo")
			.set("url", "bar")
			.set("email", "baz")
			.set("$ref", "qux");

		assertJson(t.keySet(), "['email','name','url','$ref']");
	}
}