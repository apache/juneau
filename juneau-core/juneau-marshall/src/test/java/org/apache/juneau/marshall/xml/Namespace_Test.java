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
package org.apache.juneau.marshall.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Namespace_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// create(Object)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_create_null() {
		assertNull(Namespace.create(null));
	}

	@Test void a02_create_namespaceInstance() {
		var ns = Namespace.of("foo", "http://foo");
		assertSame(ns, Namespace.create(ns));
	}

	@Test void a03_create_charSequence() {
		var ns = Namespace.create("foo:http://foo");
		assertNotNull(ns);
		assertEquals("foo", ns.getName());
	}

	@Test void a04_create_invalidType() {
		assertThrows(RuntimeException.class, () -> Namespace.create(42));
	}

	//------------------------------------------------------------------------------------------------------------------
	// createArray(Object)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_createArray_namespaceArray() {
		var arr = new Namespace[]{Namespace.of("a", "http://a")};
		assertSame(arr, Namespace.createArray(arr));
	}

	@Test void b02_createArray_stringArray() {
		var arr = Namespace.createArray(new String[]{"foo:http://foo", "bar:http://bar"});
		assertEquals(2, arr.length);
		assertEquals("foo", arr[0].getName());
		assertEquals("bar", arr[1].getName());
	}

	@Test void b03_createArray_charSequence_commaSeparated() {
		var arr = Namespace.createArray("foo:http://foo,bar:http://bar");
		assertEquals(2, arr.length);
		assertEquals("foo", arr[0].getName());
	}

	@Test void b04_createArray_collectionOfNamespace() {
		var list = List.of(Namespace.of("a", "http://a"), Namespace.of("b", "http://b"));
		var arr = Namespace.createArray(list);
		assertEquals(2, arr.length);
		assertEquals("a", arr[0].getName());
	}

	@Test void b05_createArray_collectionOfCharSequence() {
		List<CharSequence> list = List.of("foo:http://foo");
		var arr = Namespace.createArray(list);
		assertEquals(1, arr.length);
		assertEquals("foo", arr[0].getName());
	}

	@Test void b06_createArray_collectionInvalidType() {
		var list = List.of(42);
		assertThrows(RuntimeException.class, () -> Namespace.createArray(list));
	}

	@Test void b07_createArray_invalidType() {
		assertThrows(RuntimeException.class, () -> Namespace.createArray(42));
	}

	//------------------------------------------------------------------------------------------------------------------
	// of(String) — various forms
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_of_colonSeparated() {
		var ns = Namespace.of("foo:http://foo");
		assertEquals("foo", ns.getName());
		assertEquals("http://foo", ns.getUri());
	}

	@Test void c02_of_noColon() {
		var ns = Namespace.of("foo");
		assertEquals("foo", ns.getName());
		assertNull(ns.getUri());
	}

	@Test void c03_of_httpUrl() {
		var ns = Namespace.of("http://example.com/ns");
		assertNull(ns.getName());
		assertEquals("http://example.com/ns", ns.getUri());
	}

	@Test void c04_of_httpsUrl() {
		var ns = Namespace.of("https://example.com/ns");
		assertNull(ns.getName());
		assertEquals("https://example.com/ns", ns.getUri());
	}

	@Test void c05_of_cached() {
		var ns1 = Namespace.of("foo", "http://foo");
		var ns2 = Namespace.of("foo", "http://foo");
		assertSame(ns1, ns2);
	}
}
