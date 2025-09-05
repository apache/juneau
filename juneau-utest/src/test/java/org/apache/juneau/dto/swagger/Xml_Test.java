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
import org.apache.juneau.common.internal.*;
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

		// Basic property setters
		assertBean(
			t.setName("a").setNamespace("b").setPrefix("c").setAttribute(true).setWrapped(true),
			"name,namespace,prefix,attribute,wrapped",
			"a,b,c,true,true"
		);

		// Null values
		assertBean(
			t.setName(null).setNamespace(null).setPrefix(null),
			"name,namespace,prefix",
			"null,null,null"
		);
	}

	/**
	 * Test method for {@link Xml#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() {
		var t = new Xml();

		t
			.set("attribute", true)
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", true)
			.set("$ref", "d");

		// Comprehensive object state validation
		assertBean(t,
			"name,namespace,prefix,attribute,wrapped,$ref",
			"a,b,c,true,true,d");

		t
			.set("attribute", "true")
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", "true")
			.set("$ref", "d");

		assertBean(t,
			"name,namespace,prefix,attribute,wrapped,$ref",
			"a,b,c,true,true,d");

		t
			.set("attribute", Utils.sb("true"))
			.set("name", Utils.sb("a"))
			.set("namespace", Utils.sb("b"))
			.set("prefix", Utils.sb("c"))
			.set("wrapped", Utils.sb("true"))
			.set("$ref", Utils.sb("d"));

		assertBean(t,
			"name,namespace,prefix,attribute,wrapped,$ref",
			"a,b,c,true,true,d");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"attribute,name,namespace,prefix,wrapped,$ref",
			"true,a,b,c,true,d");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"attribute,name,namespace,prefix,wrapped,$ref",
			"Boolean,String,String,String,Boolean,StringBuilder");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'d'}";
		assertJson(s, JsonParser.DEFAULT.parse(s, Xml.class));
	}

	@Test void b03_copy() {
		var t = new Xml();

		t = t.copy();

		assertJson("{}", t);

		t
			.set("attribute", true)
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", true)
			.set("$ref", "d")
			.copy();

		assertBean(t,
			"name,namespace,prefix,attribute,wrapped,$ref",
			"a,b,c,true,true,d");
	}

	@Test void b04_keySet() {
		var t = new Xml();

		assertEmpty(t.keySet());

		t
			.set("attribute", true)
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", true)
			.set("$ref", "d");

		assertList(t.keySet(), "attribute", "name", "namespace", "prefix", "wrapped", "$ref");
	}
}