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
 * Testcase for {@link Tag}.
 */
public class TagTest {

	/**
	 * Test method for {@link Tag#name(java.lang.Object)}.
	 */
	@Test
	public void testName() {
		Tag t = new Tag();
		
		t.name("foo");
		assertEquals("foo", t.getName());
		
		t.name(new StringBuilder("foo"));
		assertEquals("foo", t.getName());
		assertType(String.class, t.getName());
		
		t.name(null);
		assertNull(t.getName());
	}

	/**
	 * Test method for {@link Tag#description(java.lang.Object)}.
	 */
	@Test
	public void testDescription() {
		Tag t = new Tag();
		
		t.description("foo");
		assertEquals("foo", t.getDescription());
		
		t.description(new StringBuilder("foo"));
		assertEquals("foo", t.getDescription());
		assertType(String.class, t.getDescription());
		
		t.description(null);
		assertNull(t.getDescription());
	}

	/**
	 * Test method for {@link Tag#externalDocs(java.lang.Object)}.
	 */
	@Test
	public void testExternalDocs() {
		Tag t = new Tag();
		
		t.externalDocs(externalDocumentation("foo"));
		assertObjectEquals("{url:'foo'}", t.getExternalDocs());
		
		t.externalDocs("{url:'foo'}");
		assertObjectEquals("{url:'foo'}", t.getExternalDocs());
		assertType(ExternalDocumentation.class, t.getExternalDocs());

		t.externalDocs(null);
		assertNull(t.getExternalDocs());
	}

	/**
	 * Test method for {@link Tag#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		Tag t = new Tag();
		
		t
			.set("description", "a")
			.set("externalDocs", externalDocumentation("b"))
			.set("name", "c")
			.set("$ref", "ref");
	
		assertObjectEquals("{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}", t);
		
		t
			.set("description", "a")
			.set("externalDocs", "{url:'b'}")
			.set("name", "c")
			.set("$ref", "ref");
		
		assertObjectEquals("{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}", t);
		
		t
			.set("description", new StringBuilder("a"))
			.set("externalDocs", new StringBuilder("{url:'b'}"))
			.set("name", new StringBuilder("c"))
			.set("$ref", new StringBuilder("ref"));
		
		assertObjectEquals("{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}", t);
	
		assertEquals("a", t.get("description", String.class));
		assertEquals("{url:'b'}", t.get("externalDocs", String.class));
		assertEquals("c", t.get("name", String.class));
		assertEquals("ref", t.get("$ref", String.class));
	
		assertType(String.class, t.get("description", Object.class));
		assertType(ExternalDocumentation.class, t.get("externalDocs", Object.class));
		assertType(String.class, t.get("name", Object.class));
		assertType(StringBuilder.class, t.get("$ref", Object.class));
	
		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
		
		String s = "{name:'c',description:'a',externalDocs:{url:'b'},'$ref':'ref'}";
		assertObjectEquals(s, JsonParser.DEFAULT.parse(s, Tag.class));
	}
}
