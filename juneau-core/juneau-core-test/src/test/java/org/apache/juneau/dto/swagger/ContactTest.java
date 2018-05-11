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

import java.net.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Contact}.
 */
public class ContactTest {

	/**
	 * Test method for {@link Contact#name(java.lang.Object)}.
	 */
	@Test
	public void testName() {
		Contact t = new Contact();
		
		t.name("foo");
		assertEquals("foo", t.getName());
		
		t.name(new StringBuilder("foo"));
		assertEquals("foo", t.getName());
		assertInstanceOf(String.class, t.getName());
		
		t.name(null);
		assertNull(t.getName());
	}

	/**
	 * Test method for {@link Contact#url(java.lang.Object)}.
	 */
	@Test
	public void testUrl() {
		Contact t = new Contact();

		t.url("foo");
		assertEquals("foo", t.getUrl().toString());
		
		t.url(new StringBuilder("foo"));
		assertEquals("foo", t.getUrl().toString());
		assertInstanceOf(URI.class, t.getUrl());
		
		t.url(null);
		assertNull(t.getUrl());
	}

	/**
	 * Test method for {@link Contact#email(java.lang.Object)}.
	 */
	@Test
	public void testEmail() {
		Contact t = new Contact();
		
		t.email("foo");
		assertEquals("foo", t.getEmail());
		
		t.email(new StringBuilder("foo"));
		assertEquals("foo", t.getEmail());
		assertInstanceOf(String.class, t.getEmail());

		t.email(null);
		assertNull(t.getEmail());
	}

	/**
	 * Test method for {@link Contact#set(String, Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		Contact t = new Contact();
		
		t
			.set("name", "foo")
			.set("url", "bar")
			.set("email", "baz")
			.set("$ref", "qux");
		
		assertObjectEquals("{name:'foo',url:'bar',email:'baz','$ref':'qux'}", t);
		
		t
			.set("name", new StringBuilder("foo"))
			.set("url", new StringBuilder("bar"))
			.set("email", new StringBuilder("baz"))
			.set("$ref", new StringBuilder("qux"));
		
		assertObjectEquals("{name:'foo',url:'bar',email:'baz','$ref':'qux'}", t);
		
		assertEquals("foo", t.get("name", String.class));
		assertEquals("bar", t.get("url", URI.class).toString());
		assertEquals("baz", t.get("email", String.class));
		assertEquals("qux", t.get("$ref", String.class));

		assertInstanceOf(String.class, t.get("name", String.class));
		assertInstanceOf(URI.class, t.get("url", URI.class));
		assertInstanceOf(String.class, t.get("email", String.class));
		assertInstanceOf(String.class, t.get("$ref", String.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
		
		assertObjectEquals("{name:'foo',url:'bar',email:'baz','$ref':'qux'}", JsonParser.DEFAULT.parse("{name:'foo',url:'bar',email:'baz','$ref':'qux'}", Contact.class));
	}
}
