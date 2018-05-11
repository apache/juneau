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

import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Info}.
 */
public class InfoTest {

	/**
	 * Test method for {@link Info#title(java.lang.Object)}.
	 */
	@Test
	public void testTitle() {
		Info t = new Info();
		
		t.title("foo");
		assertEquals("foo", t.getTitle());
		
		t.title(new StringBuilder("foo"));
		assertEquals("foo", t.getTitle());
		assertInstanceOf(String.class, t.getTitle());
		
		t.title(null);
		assertNull(t.getTitle());
	}

	/**
	 * Test method for {@link Info#description(java.lang.Object)}.
	 */
	@Test
	public void testDescription() {
		Info t = new Info();
		
		t.description("foo");
		assertEquals("foo", t.getDescription());
		
		t.description(new StringBuilder("foo"));
		assertEquals("foo", t.getDescription());
		assertInstanceOf(String.class, t.getDescription());
		
		t.description(null);
		assertNull(t.getDescription());
	}

	/**
	 * Test method for {@link Info#termsOfService(java.lang.Object)}.
	 */
	@Test
	public void testTermsOfService() {
		Info t = new Info();
		
		t.termsOfService("foo");
		assertEquals("foo", t.getTermsOfService());
		
		t.termsOfService(new StringBuilder("foo"));
		assertEquals("foo", t.getTermsOfService());
		assertInstanceOf(String.class, t.getTermsOfService());
		
		t.termsOfService(null);
		assertNull(t.getTermsOfService());
	}

	/**
	 * Test method for {@link Info#contact(java.lang.Object)}.
	 */
	@Test
	public void testContact() {
		Info t = new Info();
		
		t.contact(contact("foo"));
		assertObjectEquals("{name:'foo'}", t.getContact());
		
		t.contact("{name:'foo'}");
		assertObjectEquals("{name:'foo'}", t.getContact());
		assertInstanceOf(Contact.class, t.getContact());

		t.contact(null);
		assertNull(t.getContact());
	}

	/**
	 * Test method for {@link Info#license(java.lang.Object)}.
	 */
	@Test
	public void testLicense() {
		Info t = new Info();
		
		t.license(license("foo"));
		assertObjectEquals("{name:'foo'}", t.getLicense());
		
		t.license("{name:'foo'}");
		assertObjectEquals("{name:'foo'}", t.getLicense());
		assertInstanceOf(License.class, t.getLicense());

		t.license(null);
		assertNull(t.getLicense());
	}

	/**
	 * Test method for {@link Info#version(java.lang.Object)}.
	 */
	@Test
	public void testVersion() {
		Info t = new Info();
		
		t.version("foo");
		assertEquals("foo", t.getVersion());
		
		t.version(new StringBuilder("foo"));
		assertEquals("foo", t.getVersion());
		assertInstanceOf(String.class, t.getVersion());
		
		t.version(null);
		assertNull(t.getVersion());
	}

	/**
	 * Test method for {@link Info#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		Info t = new Info();

		t
			.set("contact", contact("a"))
			.set("description", "b")
			.set("license", license("c"))
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref");
	
		assertObjectEquals("{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}", t);
		
		t
			.set("contact", "{name:'a'}")
			.set("description", "b")
			.set("license", "{name:'c'}")
			.set("termsOfService", "d")
			.set("title", "e")
			.set("version", "f")
			.set("$ref", "ref");
		
		assertObjectEquals("{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}", t);
		
		t
			.set("contact", new StringBuilder("{name:'a'}"))
			.set("description", new StringBuilder("b"))
			.set("license", new StringBuilder("{name:'c'}"))
			.set("termsOfService", new StringBuilder("d"))
			.set("title", new StringBuilder("e"))
			.set("version", new StringBuilder("f"))
			.set("$ref", new StringBuilder("ref"));
		
		assertObjectEquals("{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}", t);

		assertEquals("{name:'a'}", t.get("contact", String.class));
		assertEquals("b", t.get("description", String.class));
		assertEquals("{name:'c'}", t.get("license", String.class));
		assertEquals("d", t.get("termsOfService", String.class));
		assertEquals("e", t.get("title", String.class));
		assertEquals("f", t.get("version", String.class));
		assertEquals("ref", t.get("$ref", String.class));
	
		assertInstanceOf(Contact.class, t.get("contact", Object.class));
		assertInstanceOf(String.class, t.get("description", Object.class));
		assertInstanceOf(License.class, t.get("license", Object.class));
		assertInstanceOf(String.class, t.get("termsOfService", Object.class));
		assertInstanceOf(String.class, t.get("title", Object.class));
		assertInstanceOf(String.class, t.get("version", Object.class));
		assertInstanceOf(StringBuilder.class, t.get("$ref", Object.class));
	
		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
		
		String s = "{title:'e',description:'b',version:'f',contact:{name:'a'},license:{name:'c'},termsOfService:'d','$ref':'ref'}";
		assertObjectEquals(s, JsonParser.DEFAULT.parse(s, Info.class));
	}
}
