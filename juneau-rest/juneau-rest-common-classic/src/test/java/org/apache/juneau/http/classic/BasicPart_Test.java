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
package org.apache.juneau.http.classic;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.http.classic.HttpHeaders.*;
import static org.apache.juneau.http.classic.HttpParts.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.http.classic.part.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

class BasicPart_Test extends TestBase {

	@Test void a01_ofPair() {
		var x = basicPart("Foo:bar");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = basicPart(" Foo : bar ");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = basicPart(" Foo : bar : baz ");
		assertEquals("Foo", x.getName());
		assertEquals("bar : baz", x.getValue());

		x = basicPart("Foo");
		assertEquals("Foo", x.getName());
		assertEquals("", x.getValue());

		assertNull(basicPart((String)null));
	}

	@Test void a02_of() {
		var x = part("Foo","bar");
		assertString("Foo=bar", x);
		x = part("Foo",()->"bar");
		assertString("Foo=bar", x);
	}

	@Test void a03_cast() {
		var x1 = part("X1","1");
		var x2 = serializedPart("X2","2");
		var x3 = header("X3","3");
		var x4 = serializedHeader("X4","4");
		Map.Entry<String,Object> x5 = m("X5",(Object)"5").entrySet().iterator().next();
		org.apache.http.message.BasicNameValuePair x6 = new org.apache.http.message.BasicNameValuePair("X6","6");
		NameValuePairable x7 = () -> part("X7","7");
		Headerable x8 = () -> header("X8","8");

		assertTypeAndJson(NameValuePair.class, "'X1=1'", BasicPart.cast(x1));
		assertTypeAndJson(NameValuePair.class, "'X2=2'", BasicPart.cast(x2));
		assertTypeAndJson(NameValuePair.class, "'X3: 3'", BasicPart.cast(x3));
		assertTypeAndJson(NameValuePair.class, "'X4: 4'", BasicPart.cast(x4));
		assertTypeAndJson(NameValuePair.class, "'X5=5'", BasicPart.cast(x5));
		assertTypeAndJson(NameValuePair.class, "{name:'X6',value:'6'}", BasicPart.cast(x6));
		assertTypeAndJson(NameValuePair.class, "'X7=7'", BasicPart.cast(x7));
		assertTypeAndJson(NameValuePair.class, "'X8=8'", BasicPart.cast(x8));

		assertThrowsWithMessage(RuntimeException.class, "Object of type java.lang.String could not be converted to a Part.", ()->BasicPart.cast("X"));
		assertThrowsWithMessage(RuntimeException.class, "Object of type null could not be converted to a Part.", ()->BasicPart.cast(null));

		assertTrue(BasicPart.canCast(x1));
		assertTrue(BasicPart.canCast(x2));
		assertTrue(BasicPart.canCast(x3));
		assertTrue(BasicPart.canCast(x4));
		assertTrue(BasicPart.canCast(x5));
		assertTrue(BasicPart.canCast(x6));
		assertTrue(BasicPart.canCast(x7));

		assertFalse(BasicPart.canCast("X"));
		assertFalse(BasicPart.canCast(null));
	}

	@Test void a04_asHeader() {
		var x = part("X1","1");
		assertString("X1: 1", x.asHeader());
	}

	@Test void a05_assertions() {
		var x = part("X1","1");
		x.assertName().is("X1").assertValue().is("1");
	}

	@Test void a06_getRawValue() {
		var x = part("X1","1");
		assertEquals("1", x.getRawValue());

		var x2 = part("X2",(Supplier<?>)() -> "2");
		assertEquals("2", x2.getRawValue());

		var x3 = part("X3", null);
		assertNull(x3.getRawValue());

		// Nested supplier unwrapping
		Supplier<String> inner = () -> "inner";
		Supplier<Supplier<String>> outer = () -> inner;
		var x4 = part("X4", outer);
		assertEquals("inner", x4.getRawValue());
		assertEquals("inner", x4.getValue());
	}

	@Test
	@SuppressWarnings({
		"java:S3415" // Argument order is intentional: assertNotEquals(x1, ...) exercises BasicPart.equals() with a non-matching arg/null, so x1 must be the receiver.
	})
	void a07_equals() {
		var x1 = part("Foo","bar");
		var x2 = part("Foo","bar");
		var x3 = part("Foo","baz");
		var x4 = part("Bar","bar");

		// Reflexive (same reference short-circuit)
		assertEquals(x1, x1);

		// Equal by name+value
		assertEquals(x1, x2);
		assertEquals(x2, x1);

		// Different value
		assertNotEquals(x1, x3);

		// Different name
		assertNotEquals(x1, x4);

		// Non-NameValuePair
		assertNotEquals(x1, "Foo=bar");
		assertNotEquals(x1, null);

		// Equal to a different NameValuePair impl with same name/value (cross-impl equality via the shared interface)
		NameValuePair other = new org.apache.http.message.BasicNameValuePair("Foo","bar");
		assertEquals(x1, other);
	}

	@Test void a08_hashCode() {
		var x1 = part("Foo","bar");
		var x2 = part("Foo","bar");
		var x3 = part("Foo","baz");

		assertEquals(x1.hashCode(), x2.hashCode());
		assertNotEquals(x1.hashCode(), x3.hashCode());

		// hashCode includes the resolved value (supplier unwrapped via getValue())
		var x4 = part("Foo",(Supplier<?>)() -> "bar");
		assertEquals(x1.hashCode(), x4.hashCode());
	}

	@Test void a09_toString() {
		var x = part("Foo","bar");
		assertEquals("Foo=bar", x.toString());

		var x2 = part("Empty",null);
		assertEquals("Empty=null", x2.toString());

		var x3 = part("Sup",(Supplier<?>)() -> "v");
		assertEquals("Sup=v", x3.toString());
	}

	@Test void a10_copyCtor() {
		// Exercise the protected copy constructor via a local subclass
		var orig = new BasicPart("Foo","bar");
		var copy = new BasicPartSub(orig);
		assertEquals("Foo", copy.getName());
		assertEquals("bar", copy.getValue());
		assertEquals(orig, copy);

		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'copyFrom' cannot be null", ()->new BasicPartSub((BasicPart)null));
	}

	@Test void a11_directCtorAndGetters() {
		var x = new BasicPart("Foo","bar");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		var nullVal = new BasicPart("Foo", null);
		assertNull(nullVal.getValue());
	}

	// Local subclass to exercise the protected copy constructor.
	private static class BasicPartSub extends BasicPart {
		BasicPartSub(BasicPart copyFrom) {
			super(copyFrom);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private static BasicHeader header(String name, Object val) {
		return basicHeader(name, val);
	}

	private static BasicPart part(String name, Supplier<?> val) {
		return basicPart(name, val);
	}

	private static BasicPart part(String name, Object val) {
		return basicPart(name, val);
	}

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	private static void assertTypeAndJson(Class<?> c, String json, Object value) {
		assertInstanceOf(c, value);
		assertEquals(json, Json5.DEFAULT.of(value));
	}
}