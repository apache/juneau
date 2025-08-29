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
 * Testcase for {@link ExternalDocumentation}.
 */
class ExternalDocumentation_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new ExternalDocumentation();

		assertBean(
			t.setDescription("a").setUrl(URI.create("http://b")),
			"description,url",
			"a,http://b"
		);

		assertNull(t.setDescription(null).getDescription());
	}

	/**
	 * Test method for {@link ExternalDocumentation#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		var t = new ExternalDocumentation();

		t
			.set("description", "a")
			.set("url", "b")
			.set("$ref", "c");

		assertBean(t, "description,url,$ref", "a,b,c");

		t
			.set("description", new StringBuilder("a2"))
			.set("url", new StringBuilder("b2"))
			.set("$ref", new StringBuilder("c2"));

		assertBean(t, "description,url,$ref", "a2,b2,c2");

		assertMapped(
			t, (obj,prop) -> obj.get(prop, String.class),
			"description,url,$ref",
			"a2,b2,c2"
		);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		assertBean(JsonParser.DEFAULT.parse("{description:'a',url:'b','$ref':'c'}", ExternalDocumentation.class), "description,url,$ref", "a,b,c");
	}

	@Test void b02_copy() {
		var t = new ExternalDocumentation();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("description", "a")
			.set("url", "b")
			.set("$ref", "c")
			.copy();

		assertBean(t, "description,url,$ref", "a,b,c");
	}

	@Test void b03_keySet() {
		var t = new ExternalDocumentation();

		assertSet(t.keySet());

		t
			.set("description", "foo")
			.set("url", "bar")
			.set("$ref", "baz");

		assertSet(t.keySet(), "description,url,$ref");
	}
}