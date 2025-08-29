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

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Xml}.
 */
class Xml_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new Xml();

		// General
		assertBean(
			t.setName("a").setNamespace("b").setPrefix("c").setAttribute(true).setWrapped(true),
			"name,namespace,prefix,attribute,wrapped",
			"a,b,c,true,true"
		);

		// Edge cases for nulls.
		assertNull(t.setName(null).getName());
		assertNull(t.setNamespace(null).getNamespace());
		assertNull(t.setPrefix(null).getPrefix());
	}

	/**
	 * Test method for {@link Xml#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		var t = new Xml();

		t
			.set("attribute", true)
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", true)
			.set("$ref", "ref");

		// Comprehensive object state validation
		assertBean(t,
			"name,namespace,prefix,attribute,wrapped,$ref",
			"a,b,c,true,true,ref");

		t
			.set("attribute", "true")
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", "true")
			.set("$ref", "ref");

		assertBean(t,
			"name,namespace,prefix,attribute,wrapped,$ref",
			"a,b,c,true,true,ref");

		t
			.set("attribute", new StringBuilder("true"))
			.set("name", new StringBuilder("a"))
			.set("namespace", new StringBuilder("b"))
			.set("prefix", new StringBuilder("c"))
			.set("wrapped", new StringBuilder("true"))
			.set("$ref", new StringBuilder("ref"));

		assertBean(t,
			"name,namespace,prefix,attribute,wrapped,$ref",
			"a,b,c,true,true,ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"attribute,name,namespace,prefix,wrapped,$ref",
			"true,a,b,c,true,ref");

		assertType(Boolean.class, t.get("attribute", Object.class));
		assertType(String.class, t.get("name", Object.class));
		assertType(String.class, t.get("namespace", Object.class));
		assertType(String.class, t.get("prefix", Object.class));
		assertType(Boolean.class, t.get("wrapped", Object.class));
		assertType(StringBuilder.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		var s = "{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, Xml.class), s);
	}

	@Test void b02_copy() {
		var t = new Xml();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("attribute", true)
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", true)
			.set("$ref", "ref")
			.copy();

		assertBean(t,
			"name,namespace,prefix,attribute,wrapped,$ref",
			"a,b,c,true,true,ref");
	}

	@Test void b03_keySet() {
		var t = new Xml();

		assertEmpty(t.keySet());

		t
			.set("attribute", true)
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", true)
			.set("$ref", "ref");

		assertSet(t.keySet(), "attribute,name,namespace,prefix,wrapped,$ref");
	}
}