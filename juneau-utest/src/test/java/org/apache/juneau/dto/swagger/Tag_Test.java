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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Tag}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Tag_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		Tag t = new Tag();
		assertString(t.setName("foo").getName()).is("foo");
		assertString(t.setName(null).getName()).isNull();
		assertString(t.setDescription("foo").getDescription()).is("foo");
		assertString(t.setDescription(null).getDescription()).isNull();
		assertObject(t.setExternalDocs(externalDocumentation("foo")).getExternalDocs()).asJson().is("{url:'foo'}");
	}

	/**
	 * Test method for {@link Tag#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		Tag t = new Tag();

		t
			.set("description", "a")
			.set("externalDocs", externalDocumentation("b"))
			.set("name", "c")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}");

		t
			.set("description", "a")
			.set("externalDocs", "{url:'b'}")
			.set("name", "c")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}");

		t
			.set("description", new StringBuilder("a"))
			.set("externalDocs", new StringBuilder("{url:'b'}"))
			.set("name", new StringBuilder("c"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).asJson().is("{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}");

		assertString(t.get("description", String.class)).is("a");
		assertString(t.get("externalDocs", String.class)).is("{url:'b'}");
		assertString(t.get("name", String.class)).is("c");
		assertString(t.get("$ref", String.class)).is("ref");

		assertObject(t.get("description", Object.class)).isType(String.class);
		assertObject(t.get("externalDocs", Object.class)).isType(ExternalDocumentation.class);
		assertObject(t.get("name", Object.class)).isType(String.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, Tag.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
		Tag t = new Tag();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("description", "a")
			.set("externalDocs", externalDocumentation("b"))
			.set("name", "c")
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		Tag t = new Tag();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("description", "a")
			.set("externalDocs", externalDocumentation("b"))
			.set("name", "c")
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is("['description','externalDocs','name','$ref']");
	}
}
