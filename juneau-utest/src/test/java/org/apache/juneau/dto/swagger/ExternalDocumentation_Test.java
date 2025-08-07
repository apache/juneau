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
 * Testcase for {@link ExternalDocumentation}.
 */
class ExternalDocumentation_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		ExternalDocumentation t = new ExternalDocumentation();
		assertEquals("foo", t.setDescription("foo").getDescription());
		assertString(t.setDescription(null).getDescription()).isNull();
		assertString("http://bar", t.setUrl(URI.create("http://bar")).getUrl());
	}

	/**
	 * Test method for {@link ExternalDocumentation#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		ExternalDocumentation t = new ExternalDocumentation();

		t
			.set("description", "foo")
			.set("url", "bar")
			.set("$ref", "baz");

		assertJson(t, "{description:'foo',url:'bar','$ref':'baz'}");

		t
			.set("description", new StringBuilder("foo"))
			.set("url", new StringBuilder("bar"))
			.set("$ref", new StringBuilder("baz"));

		assertJson(t, "{description:'foo',url:'bar','$ref':'baz'}");

		assertEquals("foo", t.get("description", String.class));
		assertString("bar", t.get("url", URI.class));
		assertEquals("baz", t.get("$ref", String.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		assertJson(JsonParser.DEFAULT.parse("{description:'foo',url:'bar','$ref':'baz'}", ExternalDocumentation.class), "{description:'foo',url:'bar','$ref':'baz'}");
	}

	@Test void b02_copy() {
		ExternalDocumentation t = new ExternalDocumentation();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("description", "foo")
			.set("url", "bar")
			.set("$ref", "baz")
			.copy();

		assertJson(t, "{description:'foo',url:'bar','$ref':'baz'}");
	}

	@Test void b03_keySet() {
		ExternalDocumentation t = new ExternalDocumentation();

		assertJson(t.keySet(), "[]");

		t
			.set("description", "foo")
			.set("url", "bar")
			.set("$ref", "baz");

		assertJson(t.keySet(), "['description','url','$ref']");
	}
}