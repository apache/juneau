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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Xml}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class XmlTest {

	/**
	 * Test method for {@link Xml#name(java.lang.Object)}.
	 */
	@Test
	public void testName() {
		Xml t = new Xml();

		t.name("foo");
		assertEquals("foo", t.getName());

		t.name(new StringBuilder("foo"));
		assertEquals("foo", t.getName());
		assertObject(t.getName()).isType(String.class);

		t.name(null);
		assertNull(t.getName());
	}

	/**
	 * Test method for {@link Xml#namespace(java.lang.Object)}.
	 */
	@Test
	public void testNamespace() {
		Xml t = new Xml();

		t.namespace("foo");
		assertEquals("foo", t.getNamespace());

		t.namespace(new StringBuilder("foo"));
		assertEquals("foo", t.getNamespace());
		assertObject(t.getNamespace()).isType(String.class);

		t.namespace(null);
		assertNull(t.getNamespace());
	}

	/**
	 * Test method for {@link Xml#prefix(java.lang.Object)}.
	 */
	@Test
	public void testPrefix() {
		Xml t = new Xml();

		t.prefix("foo");
		assertEquals("foo", t.getPrefix());

		t.prefix(new StringBuilder("foo"));
		assertEquals("foo", t.getPrefix());
		assertObject(t.getPrefix()).isType(String.class);

		t.prefix(null);
		assertNull(t.getPrefix());
	}

	/**
	 * Test method for {@link Xml#attribute(java.lang.Object)}.
	 */
	@Test
	public void testAttribute() {
		Xml t = new Xml();

		t.attribute(true);
		assertEquals(true, t.getAttribute());
		assertObject(t.getAttribute()).isType(Boolean.class);

		t.attribute("true");
		assertEquals(true, t.getAttribute());
		assertObject(t.getAttribute()).isType(Boolean.class);

		t.attribute(new StringBuilder("true"));
		assertEquals(true, t.getAttribute());
		assertObject(t.getAttribute()).isType(Boolean.class);

		t.attribute(null);
		assertNull(t.getAttribute());
	}

	/**
	 * Test method for {@link Xml#wrapped(java.lang.Object)}.
	 */
	@Test
	public void testWrapped() {
		Xml t = new Xml();

		t.wrapped(true);
		assertEquals(true, t.getWrapped());
		assertObject(t.getWrapped()).isType(Boolean.class);

		t.wrapped("true");
		assertEquals(true, t.getWrapped());
		assertObject(t.getWrapped()).isType(Boolean.class);

		t.wrapped(new StringBuilder("true"));
		assertEquals(true, t.getWrapped());
		assertObject(t.getWrapped()).isType(Boolean.class);

		t.wrapped(null);
		assertNull(t.getWrapped());
	}

	/**
	 * Test method for {@link Xml#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		Xml t = new Xml();

		t
			.set("attribute", true)
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", true)
			.set("$ref", "ref");

		assertObject(t).json().is("{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}");

		t
			.set("attribute", "true")
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", "true")
			.set("$ref", "ref");

		assertObject(t).json().is("{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}");

		t
			.set("attribute", new StringBuilder("true"))
			.set("name", new StringBuilder("a"))
			.set("namespace", new StringBuilder("b"))
			.set("prefix", new StringBuilder("c"))
			.set("wrapped", new StringBuilder("true"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).json().is("{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}");

		assertEquals("true", t.get("attribute", String.class));
		assertEquals("a", t.get("name", String.class));
		assertEquals("b", t.get("namespace", String.class));
		assertEquals("c", t.get("prefix", String.class));
		assertEquals("true", t.get("wrapped", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertObject(t.get("attribute", Object.class)).isType(Boolean.class);
		assertObject(t.get("name", Object.class)).isType(String.class);
		assertObject(t.get("namespace", Object.class)).isType(String.class);
		assertObject(t.get("prefix", Object.class)).isType(String.class);
		assertObject(t.get("wrapped", Object.class)).isType(Boolean.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, Xml.class)).json().is(s);
	}
}
