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
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Info}.
 */
class Info_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new Info();
		assertEquals("foo", t.setTitle("foo").getTitle());
		assertNull(t.setTitle(null).getTitle());
		assertEquals("foo", t.setDescription("foo").getDescription());
		assertNull(t.setDescription(null).getDescription());
		assertEquals("foo", t.setTermsOfService("foo").getTermsOfService());
		assertNull(t.setTermsOfService(null).getTermsOfService());
		assertJson(t.setContact(contact("foo")).getContact(), "{name:'foo'}");
		assertJson(t.setLicense(license("foo")).getLicense(), "{name:'foo'}");
		assertEquals("foo", t.setVersion("foo").getVersion());
		assertNull(t.setVersion(null).getVersion());
	}

	/**
	 * Test method for {@link Info#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		var t = new Info();

		t
			.set("contact", contact("a"))
			.set("description", "b")
			.set("license", license("c"))
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref");

		assertJson(t, "{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}");

		t
			.set("contact", "{name:'a'}")
			.set("description", "b")
			.set("license", "{name:'c'}")
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref");

		assertJson(t, "{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}");

		t
			.set("contact", new StringBuilder("{name:'a'}"))
			.set("description", new StringBuilder("b"))
			.set("license", new StringBuilder("{name:'c'}"))
			.set("termsOfService", new StringBuilder("d"))
			.set("title", new StringBuilder("e"))
			.set("version", new StringBuilder("f"))
			.set("$ref", new StringBuilder("ref"));

		assertJson(t, "{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}");

		assertEquals("{name:'a'}", t.get("contact", String.class));
		assertEquals("b", t.get("description", String.class));
		assertEquals("{name:'c'}", t.get("license", String.class));
		assertEquals("d", t.get("termsOfService", String.class));
		assertEquals("e", t.get("title", String.class));
		assertEquals("f", t.get("version", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertType(Contact.class, t.get("contact", Object.class));
		assertType(String.class, t.get("description", Object.class));
		assertType(License.class, t.get("license", Object.class));
		assertType(String.class, t.get("termsOfService", Object.class));
		assertType(String.class, t.get("title", Object.class));
		assertType(String.class, t.get("version", Object.class));
		assertType(StringBuilder.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		var s = "{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, Info.class), s);
	}

	@Test void b02_copy() {
		var t = new Info();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("contact", contact("a"))
			.set("description", "b")
			.set("license", license("c"))
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref")
			.copy();

		assertJson(t, "{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}");
	}

	@Test void b03_keySet() {
		var t = new Info();

		assertJson(t.keySet(), "[]");

		t
			.set("contact", contact("a"))
			.set("description", "b")
			.set("license", license("c"))
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref");

		assertJson(t.keySet(), "['contact','description','license','termsOfService','title','version','$ref']");
	}
}