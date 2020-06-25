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

import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.net.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link License}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class LicenseTest {

	/**
	 * Test method for {@link License#name(java.lang.Object)}.
	 */
	@Test
	public void testName() {
		License t = new License();

		t.name("foo");
		assertEquals("foo", t.getName());

		t.name(new StringBuilder("foo"));
		assertEquals("foo", t.getName());
		assertObject(t.getName()).isType(String.class);

		t.name(null);
		assertNull(t.getName());
	}

	/**
	 * Test method for {@link License#url(java.lang.Object)}.
	 */
	@Test
	public void testUrl() {
		License t = new License();

		t.url(URI.create("foo"));
		assertEquals("foo", t.getUrl().toString());

		t.url("foo");
		assertEquals("foo", t.getUrl().toString());
		assertObject(t.getUrl()).isType(URI.class);

		t.url(null);
		assertNull(t.getUrl());
	}

	/**
	 * Test method for {@link License#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		License t = new License();

		t
			.set("name", "a")
			.set("url", URI.create("b"))
			.set("$ref", "ref");

		assertObject(t).json().is("{name:'a',url:'b','$ref':'ref'}");

		t
			.set("name", "a")
			.set("url", "b")
			.set("$ref", "ref");

		assertObject(t).json().is("{name:'a',url:'b','$ref':'ref'}");

		t
			.set("name", new StringBuilder("a"))
			.set("url", new StringBuilder("b"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).json().is("{name:'a',url:'b','$ref':'ref'}");

		assertEquals("a", t.get("name", String.class));
		assertEquals("b", t.get("url", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertObject(t.get("name", Object.class)).isType(String.class);
		assertObject(t.get("url", Object.class)).isType(URI.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{name:'a',url:'b','$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, License.class)).json().is(s);
	}
}
