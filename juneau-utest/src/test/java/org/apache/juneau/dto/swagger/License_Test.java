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
import org.apache.juneau.common.internal.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link License}.
 */
class License_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new License();

		// Basic property setters
		assertBean(
			t.setName("a").setUrl(URI.create("http://b")),
			"name,url",
			"a,http://b"
		);

		// Null values
		assertNull(t.setName(null).getName());
	}

	/**
	 * Test method for {@link License#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() {
		var t = new License();

		t
			.set("name", "a")
			.set("url", URI.create("b"))
			.set("$ref", "ref");

		// Comprehensive object state validation
		assertBean(t,
			"name,url,$ref",
			"a,b,ref");

		t
			.set("name", "a")
			.set("url", "b")
			.set("$ref", "ref");

		assertBean(t,
			"name,url,$ref",
			"a,b,ref");

		t
			.set("name", Utils.sb("a"))
			.set("url", Utils.sb("b"))
			.set("$ref", Utils.sb("ref"));

		assertBean(t,
			"name,url,$ref",
			"a,b,ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"name,url,$ref",
			"a,b,ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"name,url,$ref",
			"String,URI,StringBuilder");

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{name:'a',url:'b','$ref':'ref'}";
		assertJson(s, JsonParser.DEFAULT.parse(s, License.class));
	}

	@Test void b03_copy() {
		var t = new License();

		t = t.copy();

		assertJson("{}", t);

		t
			.set("name", "a")
			.set("url", URI.create("b"))
			.set("$ref", "ref")
			.copy();

		assertBean(t,
			"name,url,$ref",
			"a,b,ref");
	}

	@Test void b04_keySet() {
		var t = new License();

		assertEmpty(t.keySet());

		t
			.set("name", "a")
			.set("url", URI.create("b"))
			.set("$ref", "ref");

		assertSet(t.keySet(), "name", "url", "$ref");
	}
}