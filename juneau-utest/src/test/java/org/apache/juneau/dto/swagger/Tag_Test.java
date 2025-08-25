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
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.bean.swagger.Tag;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Tag}.
 */
class Tag_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new Tag();
		assertEquals("foo", t.setName("foo").getName());
		assertNull(t.setName(null).getName());
		assertEquals("foo", t.setDescription("foo").getDescription());
		assertNull(t.setDescription(null).getDescription());
		assertJson(t.setExternalDocs(externalDocumentation("foo")).getExternalDocs(), "{url:'foo'}");
	}

	/**
	 * Test method for {@link Tag#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		var t = new Tag();

		t
			.set("description", "a")
			.set("externalDocs", externalDocumentation("b"))
			.set("name", "c")
			.set("$ref", "ref");

		assertJson(t, "{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}");

		t
			.set("description", "a")
			.set("externalDocs", "{url:'b'}")
			.set("name", "c")
			.set("$ref", "ref");

		assertJson(t, "{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}");

		t
			.set("description", new StringBuilder("a"))
			.set("externalDocs", new StringBuilder("{url:'b'}"))
			.set("name", new StringBuilder("c"))
			.set("$ref", new StringBuilder("ref"));

		assertJson(t, "{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}");

		assertEquals("a", t.get("description", String.class));
		assertEquals("{url:'b'}", t.get("externalDocs", String.class));
		assertEquals("c", t.get("name", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertType(String.class, t.get("description", Object.class));
		assertType(ExternalDocumentation.class, t.get("externalDocs", Object.class));
		assertType(String.class, t.get("name", Object.class));
		assertType(StringBuilder.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, Tag.class), s);
	}

	@Test void b02_copy() {
		var t = new Tag();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("description", "a")
			.set("externalDocs", externalDocumentation("b"))
			.set("name", "c")
			.set("$ref", "ref")
			.copy();

		assertJson(t, "{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}");
	}

	@Test void b03_keySet() {
		var t = new Tag();

		assertJson(t.keySet(), "[]");

		t
			.set("description", "a")
			.set("externalDocs", externalDocumentation("b"))
			.set("name", "c")
			.set("$ref", "ref");

		assertJson(t.keySet(), "['description','externalDocs','name','$ref']");
	}
}