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
import static org.junit.runners.MethodSorters.*;

import java.net.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Contact}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Contact_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		Contact t = new Contact();
		assertString(t.setName("foo").getName()).is("foo");
		assertString(t.setName(null).getName()).isNull();
		assertString(t.setUrl(URI.create("http://bar")).getUrl()).is("http://bar");
		assertString(t.setEmail("foo").getEmail()).is("foo");
		assertString(t.setEmail(null).getEmail()).isNull();
	}

	/**
	 * Test method for {@link Contact#set(String, Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		Contact t = new Contact();

		t
			.set("name", "foo")
			.set("url", "bar")
			.set("email", "baz")
			.set("$ref", "qux");

		assertObject(t).asJson().is("{name:'foo',url:'bar',email:'baz','$ref':'qux'}");

		t
			.set("name", new StringBuilder("foo"))
			.set("url", new StringBuilder("bar"))
			.set("email", new StringBuilder("baz"))
			.set("$ref", new StringBuilder("qux"));

		assertObject(t).asJson().is("{name:'foo',url:'bar',email:'baz','$ref':'qux'}");

		assertObject(t.get("name", String.class)).isType(String.class).is("foo");
		assertObject(t.get("url", URI.class)).isType(URI.class).asString().is("bar");
		assertObject(t.get("email", String.class)).isType(String.class).is("baz");
		assertObject(t.get("$ref", String.class)).isType(String.class).is("qux");

		t.set("null", null).set(null, "null");
		assertObject(t.get("null", Object.class)).isNull();
		assertObject(t.get(null, Object.class)).isNull();
		assertObject(t.get("foo", Object.class)).isNull();

		assertObject(JsonParser.DEFAULT.parse("{name:'foo',url:'bar',email:'baz','$ref':'qux'}", Contact.class)).asJson().is("{name:'foo',url:'bar',email:'baz','$ref':'qux'}");
	}

	@Test
	public void b02_copy() throws Exception {
		Contact t = new Contact();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("name", "foo")
			.set("url", "bar")
			.set("email", "baz")
			.set("$ref", "qux")
			.copy();

		assertObject(t).asJson().is("{name:'foo',url:'bar',email:'baz','$ref':'qux'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		Contact t = new Contact();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("name", "foo")
			.set("url", "bar")
			.set("email", "baz")
			.set("$ref", "qux");

		assertObject(t.keySet()).asJson().is("['email','name','url','$ref']");
	}
}