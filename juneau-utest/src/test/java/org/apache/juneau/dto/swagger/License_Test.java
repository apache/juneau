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
import static org.junit.jupiter.api.Assertions.*;
import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link License}.
 */
class License_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		License t = new License();
		assertEquals("foo", t.setName("foo").getName());
		assertString(t.setName(null).getName()).isNull();
		assertString("foo", t.setUrl(URI.create("foo")).getUrl());
	}

	/**
	 * Test method for {@link License#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		License t = new License();

		t
			.set("name", "a")
			.set("url", URI.create("b"))
			.set("$ref", "ref");

		assertObject(t).asJson().is("{name:'a',url:'b','$ref':'ref'}");

		t
			.set("name", "a")
			.set("url", "b")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{name:'a',url:'b','$ref':'ref'}");

		t
			.set("name", new StringBuilder("a"))
			.set("url", new StringBuilder("b"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).asJson().is("{name:'a',url:'b','$ref':'ref'}");

		assertEquals("a", t.get("name", String.class));
		assertEquals("b", t.get("url", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertType(String.class, t.get("name", Object.class));
		assertType(URI.class, t.get("url", Object.class));
		assertType(StringBuilder.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{name:'a',url:'b','$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, License.class)).asJson().is(s);
	}

	@Test void b02_copy() {
		License t = new License();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("name", "a")
			.set("url", URI.create("b"))
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{name:'a',url:'b','$ref':'ref'}");
	}

	@Test void b03_keySet() {
		License t = new License();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("name", "a")
			.set("url", URI.create("b"))
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is("['name','url','$ref']");
	}
}