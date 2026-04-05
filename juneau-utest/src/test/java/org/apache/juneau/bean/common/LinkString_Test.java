/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.common;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.*;
import org.junit.jupiter.api.*;

class LinkString_Test extends TestBase {

	@Test void a01_noArgConstructor() {
		var a = new LinkString();
		assertNull(a.getName());
		assertNull(a.getUri());
	}

	@Test void a02_constructorWithArgs() {
		var a = new LinkString("Home", "http://example.org/");
		assertEquals("Home", a.getName());
		assertEquals("http://example.org/", a.getUri().toString());
	}

	@Test void a03_constructorWithFormatArgs() {
		var a = new LinkString("Item", "/items/{0}", "abc");
		assertEquals("Item", a.getName());
		assertEquals("/items/abc", a.getUri().toString());
	}

	@Test void a04_setUri_URI() {
		var a = new LinkString();
		a.setUri(URI.create("http://example.org/"));
		assertEquals("http://example.org/", a.getUri().toString());
	}

	@Test void a05_setUri_String() {
		var a = new LinkString();
		a.setUri("http://example.org/path");
		assertEquals("http://example.org/path", a.getUri().toString());
	}

	@Test void a06_compareTo() {
		var a = new LinkString("Apple", "http://a.org/");
		var b = new LinkString("Banana", "http://b.org/");
		assertTrue(a.compareTo(b) < 0);
		assertTrue(b.compareTo(a) > 0);
		assertEquals(0, a.compareTo(a));
	}

	@Test void a07_equals() {
		var a = new LinkString("Home", "http://example.org/");
		var b = new LinkString("Home", "http://example.org/");
		var c = new LinkString("Other", "http://other.org/");

		assertTrue(a.equals(b));
		assertFalse(a.equals(c));
		assertFalse(a.equals(null));
		assertFalse(a.equals((Object)"notALinkString"));
		assertTrue(a.equals(a));
	}

	@Test void a08_hashCode_withNonNullName() {
		var a = new LinkString("Home", "http://example.org/");
		assertEquals("Home".hashCode(), a.hashCode());
	}

	@Test void a09_hashCode_withNullName() {
		var a = new LinkString();
		assertEquals(0, a.hashCode());
	}

	@Test void a10_toString() {
		var a = new LinkString("Home", "http://example.org/");
		assertEquals("Home", a.toString());
	}

	@Test void a11_toString_nullName() {
		var a = new LinkString();
		assertNull(a.toString());
	}
}
