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

		// General
		assertBean(
			t.setTitle("a").setDescription("b").setTermsOfService("c").setContact(contact("d")).setLicense(license("e")).setVersion("f"),
			"title,description,termsOfService,contact{name},license{name},version",
			"a,b,c,{d},{e},f"
		);

		// Edge cases for nulls.
		assertNull(t.setTitle(null).getTitle());
		assertNull(t.setDescription(null).getDescription());
		assertNull(t.setTermsOfService(null).getTermsOfService());
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

		// Comprehensive object state validation
		assertBean(t,
			"title,description,version,contact{name},license{name},termsOfService,$ref",
			"e,b,f,{a},{c},d,ref");

		t
			.set("contact", "{name:'a'}")
			.set("description", "b")
			.set("license", "{name:'c'}")
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref");

		assertBean(t,
			"title,description,version,contact{name},license{name},termsOfService,$ref",
			"e,b,f,{a},{c},d,ref");

		t
			.set("contact", new StringBuilder("{name:'a'}"))
			.set("description", new StringBuilder("b"))
			.set("license", new StringBuilder("{name:'c'}"))
			.set("termsOfService", new StringBuilder("d"))
			.set("title", new StringBuilder("e"))
			.set("version", new StringBuilder("f"))
			.set("$ref", new StringBuilder("ref"));

		assertBean(t,
			"title,description,version,contact{name},license{name},termsOfService,$ref",
			"e,b,f,{a},{c},d,ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"contact,description,license,termsOfService,title,version,$ref",
			"{name:'a'},b,{name:'c'},d,e,f,ref");

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

		assertBean(t,
			"title,description,version,contact{name},license{name},termsOfService,$ref",
			"e,b,f,{a},{c},d,ref");
	}

	@Test void b03_keySet() {
		var t = new Info();

		assertEmpty(t.keySet());

		t
			.set("contact", contact("a"))
			.set("description", "b")
			.set("license", license("c"))
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref");

		assertSet(t.keySet(), "contact,description,license,termsOfService,title,version,$ref");
	}
}