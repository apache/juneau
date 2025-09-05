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
import org.apache.juneau.common.internal.*;
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

		// Basic property setters
		assertBean(
			t.setTitle("a").setDescription("b").setTermsOfService("c").setContact(contact("d")).setLicense(license("e")).setVersion("f"),
			"title,description,termsOfService,contact{name},license{name},version",
			"a,b,c,{d},{e},f"
		);

		// Null values
		assertBean(
			t.setTitle(null).setDescription(null).setTermsOfService(null).setVersion(null),
			"title,description,termsOfService,version",
			"null,null,null,null"
		);
	}

	/**
	 * Test method for {@link Info#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() {
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
			.set("contact", Utils.sb("{name:'a'}"))
			.set("description", Utils.sb("b"))
			.set("license", Utils.sb("{name:'c'}"))
			.set("termsOfService", Utils.sb("d"))
			.set("title", Utils.sb("e"))
			.set("version", Utils.sb("f"))
			.set("$ref", Utils.sb("ref"));

		assertBean(t,
			"title,description,version,contact{name},license{name},termsOfService,$ref",
			"e,b,f,{a},{c},d,ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"contact,description,license,termsOfService,title,version,$ref",
			"{name:'a'},b,{name:'c'},d,e,f,ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"contact,description,license,termsOfService,title,version,$ref",
			"Contact,String,License,String,String,String,StringBuilder");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}";
		assertJson(s, JsonParser.DEFAULT.parse(s, Info.class));
	}

	@Test void b03_copy() {
		var t = new Info();

		t = t.copy();

		assertJson("{}", t);

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

	@Test void b04_keySet() {
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

		assertList(t.keySet(), "contact", "description", "license", "termsOfService", "title", "version", "$ref");
	}
}