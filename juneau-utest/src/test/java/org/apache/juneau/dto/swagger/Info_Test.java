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
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Info}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Info_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		Info t = new Info();
		assertString(t.setTitle("foo").getTitle()).is("foo");
		assertString(t.setTitle(null).getTitle()).isNull();
		assertString(t.setDescription("foo").getDescription()).is("foo");
		assertString(t.setDescription(null).getDescription()).isNull();
		assertString(t.setTermsOfService("foo").getTermsOfService()).is("foo");
		assertString(t.setTermsOfService(null).getTermsOfService()).isNull();
		assertObject(t.setContact(contact("foo")).getContact()).asJson().is("{name:'foo'}");
		assertObject(t.setLicense(license("foo")).getLicense()).isType(License.class).asJson().is("{name:'foo'}");
		assertString(t.setVersion("foo").getVersion()).is("foo");
		assertString(t.setVersion(null).getVersion()).isNull();
	}

	/**
	 * Test method for {@link Info#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		Info t = new Info();

		t
			.set("contact", contact("a"))
			.set("description", "b")
			.set("license", license("c"))
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}");

		t
			.set("contact", "{name:'a'}")
			.set("description", "b")
			.set("license", "{name:'c'}")
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}");

		t
			.set("contact", new StringBuilder("{name:'a'}"))
			.set("description", new StringBuilder("b"))
			.set("license", new StringBuilder("{name:'c'}"))
			.set("termsOfService", new StringBuilder("d"))
			.set("title", new StringBuilder("e"))
			.set("version", new StringBuilder("f"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).asJson().is("{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}");

		assertString(t.get("contact", String.class)).is("{name:'a'}");
		assertString(t.get("description", String.class)).is("b");
		assertString(t.get("license", String.class)).is("{name:'c'}");
		assertString(t.get("termsOfService", String.class)).is("d");
		assertString(t.get("title", String.class)).is("e");
		assertString(t.get("version", String.class)).is("f");
		assertString(t.get("$ref", String.class)).is("ref");

		assertObject(t.get("contact", Object.class)).isType(Contact.class);
		assertObject(t.get("description", Object.class)).isType(String.class);
		assertObject(t.get("license", Object.class)).isType(License.class);
		assertObject(t.get("termsOfService", Object.class)).isType(String.class);
		assertObject(t.get("title", Object.class)).isType(String.class);
		assertObject(t.get("version", Object.class)).isType(String.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertObject(t.get("null", Object.class)).isNull();
		assertObject(t.get(null, Object.class)).isNull();
		assertObject(t.get("foo", Object.class)).isNull();

		String s = "{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, Info.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
		Info t = new Info();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("contact", contact("a"))
			.set("description", "b")
			.set("license", license("c"))
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		Info t = new Info();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("contact", contact("a"))
			.set("description", "b")
			.set("license", license("c"))
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is("['contact','description','license','termsOfService','title','version','$ref']");
	}
}
