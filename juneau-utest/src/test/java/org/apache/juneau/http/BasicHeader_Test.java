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
package org.apache.juneau.http;

import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class BasicHeader_Test extends TestBase {

	@Test void a01_ofPair() {
		var x = stringHeader("Foo:bar");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = stringHeader(" Foo : bar ");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = stringHeader(" Foo : bar : baz ");
		assertEquals("Foo", x.getName());
		assertEquals("bar : baz", x.getValue());

		x = stringHeader("Foo");
		assertEquals("Foo", x.getName());
		assertEquals("", x.getValue());

		assertNull(stringHeader((String)null));
	}

	@Test void a02_of() {
		var x = header("Foo","bar");
		assertString("Foo: bar", x);
		x = header("Foo",()->"bar");
		assertString("Foo: bar", x);
	}

	@Test void a05_assertions() {
		var x = header("X1","1");
		x.assertName().is("X1").assertStringValue().is("1");
	}

	@Test void a07_eqIC() {
		var x = header("X1","1");
		assertTrue(x.equalsIgnoreCase("1"));
		assertFalse(x.equalsIgnoreCase("2"));
		assertFalse(x.equalsIgnoreCase(null));
	}

	@Test void a08_getElements() {
		var m = Value.of(1);
		var h1 = header("X1","1");
		var h2 = header("X2",()->m);
		var h3 = header("X3",null);

		var x = h1.getElements();

		assertEquals(1, x.length);
		assertEquals("1", x[0].getName());
		x = h1.getElements();
		assertEquals(1, x.length);
		assertEquals("1", x[0].getName());

		x = h2.getElements();
		assertEquals(1, x.length);
		assertEquals("Value(1)", x[0].getName());
		m.set(2);
		x = h2.getElements();
		assertEquals(1, x.length);
		assertEquals("Value(2)", x[0].getName());

		x = h3.getElements();
		assertEquals(0, x.length);
	}

	@Test void a09_equals() {
		var h1 = header("Foo","bar");
		var h2 = header("Foo","bar");
		var h3 = header("Bar","bar");
		var h4 = header("Foo","baz");
		assertEquals(h1, h2);
		assertNotEquals(h1, h3);
		assertNotEquals(h1, h4);
		assertNotEquals(h1, "foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private static BasicHeader header(String name, Object val) {
		return basicHeader(name, val);
	}

	private static BasicHeader header(String name, Supplier<?> val) {
		return basicHeader(name, val);
	}
}