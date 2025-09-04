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
import org.apache.juneau.bean.swagger.Tag;
import org.apache.juneau.common.internal.*;
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

		// Basic property setters
		assertBean(
			t.setName("a").setDescription("b").setExternalDocs(externalDocumentation("c")),
			"name,description,externalDocs{url}",
			"a,b,{c}"
		);

		// Null values
		assertBean(
			t.setName(null).setDescription(null),
			"name,description",
			"null,null"
		);
	}

	/**
	 * Test method for {@link Tag#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() {
		var t = new Tag();

		t
			.set("description", "a")
			.set("externalDocs", externalDocumentation("b"))
			.set("name", "c")
			.set("$ref", "ref");

		// Comprehensive object state validation
		assertBean(t,
			"name,description,externalDocs{url},$ref",
			"c,a,{b},ref");

		t
			.set("description", "a")
			.set("externalDocs", "{url:'b'}")
			.set("name", "c")
			.set("$ref", "ref");

		assertBean(t,
			"name,description,externalDocs{url},$ref",
			"c,a,{b},ref");

		t
			.set("description", Utils.sb("a"))
			.set("externalDocs", Utils.sb("{url:'b'}"))
			.set("name", Utils.sb("c"))
			.set("$ref", Utils.sb("ref"));

		assertBean(t,
			"name,description,externalDocs{url},$ref",
			"c,a,{b},ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"description,externalDocs,name,$ref",
			"a,{url:'b'},c,ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"description,externalDocs,name,$ref",
			"String,ExternalDocumentation,String,StringBuilder");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, Tag.class), s);
	}

	@Test void b03_copy() {
		var t = new Tag();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("description", "a")
			.set("externalDocs", externalDocumentation("b"))
			.set("name", "c")
			.set("$ref", "ref")
			.copy();

		assertBean(t,
			"name,description,externalDocs{url},$ref",
			"c,a,{b},ref");
	}

	@Test void b04_keySet() {
		var t = new Tag();

		assertEmpty(t.keySet());

		t
			.set("description", "a")
			.set("externalDocs", externalDocumentation("b"))
			.set("name", "c")
			.set("$ref", "ref");

		assertSet(t.keySet(), "description", "externalDocs", "name", "$ref");
	}
}