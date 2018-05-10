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

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Xml}.
 */
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
		assertType(String.class, t.getName());
		
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
		assertType(String.class, t.getNamespace());
		
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
		assertType(String.class, t.getPrefix());
		
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
		assertType(Boolean.class, t.getAttribute());
		
		t.attribute("true");
		assertEquals(true, t.getAttribute());
		assertType(Boolean.class, t.getAttribute());

		t.attribute(new StringBuilder("true"));
		assertEquals(true, t.getAttribute());
		assertType(Boolean.class, t.getAttribute());
		
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
		assertType(Boolean.class, t.getWrapped());
		
		t.wrapped("true");
		assertEquals(true, t.getWrapped());
		assertType(Boolean.class, t.getWrapped());

		t.wrapped(new StringBuilder("true"));
		assertEquals(true, t.getWrapped());
		assertType(Boolean.class, t.getWrapped());
		
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
	
		assertObjectEquals("{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}", t);
		
		t
			.set("attribute", "true")
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", "true")
			.set("$ref", "ref");
	
		assertObjectEquals("{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}", t);

		t
			.set("attribute", new StringBuilder("true"))
			.set("name", new StringBuilder("a"))
			.set("namespace", new StringBuilder("b"))
			.set("prefix", new StringBuilder("c"))
			.set("wrapped", new StringBuilder("true"))
			.set("$ref", new StringBuilder("ref"));
	
		assertObjectEquals("{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}", t);
		
		assertEquals("true", t.get("attribute", String.class));
		assertEquals("a", t.get("name", String.class));
		assertEquals("b", t.get("namespace", String.class));
		assertEquals("c", t.get("prefix", String.class));
		assertEquals("true", t.get("wrapped", String.class));
		assertEquals("ref", t.get("$ref", String.class));
	
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
		
		String s = "{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}";
		assertObjectEquals(s, JsonParser.DEFAULT.parse(s, Xml.class));
	}
}
