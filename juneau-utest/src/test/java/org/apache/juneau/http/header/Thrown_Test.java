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
package org.apache.juneau.http.header;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Thrown_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — String constructors and factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_basic_stringConstructorAndFactory() {
		var x = new Thrown("RuntimeException;boom");
		assertEquals("Thrown", x.getName());
		assertEquals("RuntimeException;boom", x.getValue());

		var y = Thrown.of("RuntimeException;boom");
		assertEquals("Thrown", y.getName());
		assertEquals("RuntimeException;boom", y.getValue());
	}

	@Test void a02_basic_varargsStringConstructor() {
		var x = new Thrown("RuntimeException;boom", "IllegalArgumentException;bad");
		assertEquals("Thrown", x.getName());
		assertEquals("RuntimeException;boom, IllegalArgumentException;bad", x.getValue());

		// To reach Thrown.of(String...) the array must be passed explicitly — otherwise
		// the 2-arg call resolves to the inherited HttpCsvHeader.of(String, String) static
		// method (see a05 below for that footgun).
		var y = Thrown.of(new String[]{"RuntimeException;boom", "IllegalArgumentException;bad"});
		assertEquals("Thrown", y.getName());
		assertEquals("RuntimeException;boom, IllegalArgumentException;bad", y.getValue());
	}

	@Test void a03_basic_emptyAndNullVarargs() {
		// Empty varargs: eagerTokens is non-null empty array, so getValue() joins to ""
		var x = Thrown.of(new String[0]);
		assertEquals("Thrown", x.getName());
		assertEquals("", x.getValue());

		// Null varargs: eagerTokens is null, super.getValue() is null, getValue() returns null
		var y = Thrown.of((String[])null);
		assertEquals("Thrown", y.getName());
		assertNull(y.getValue());
	}

	@Test void a04_basic_nullStringConstructor() {
		// Single-arg null routes to Thrown.of(String) -> Thrown(String) -> super(NAME, null)
		var x = Thrown.of((String)null);
		assertEquals("Thrown", x.getName());
		assertNull(x.getValue());
	}

	/**
	 * Documents production footgun: {@code Thrown.of("a","b")} with two String literals
	 * resolves to the inherited {@link HttpCsvHeader#of(String, String)} static method, not to
	 * {@link Thrown#of(String...)}. The result is an {@link HttpCsvHeader} (not a {@link Thrown})
	 * where the first argument becomes the header name. Callers that want a {@code Thrown}
	 * varargs invocation must pass an explicit {@code String[]}.
	 */
	@Test void a05_documentedFootgun_inheritedOfStringStringShadowsVarargs() {
		var x = Thrown.of("RuntimeException;boom", "IllegalArgumentException;bad");
		// Returns base HttpCsvHeader, not Thrown.
		assertFalse(x instanceof Thrown);
		assertEquals("RuntimeException;boom", x.getName());
		assertEquals("IllegalArgumentException;bad", x.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — Throwable factory
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_throwable_singleWithMessage() {
		var t = new RuntimeException("boom");
		var x = Thrown.of(t);
		assertEquals("Thrown", x.getName());
		assertEquals("java.lang.RuntimeException;boom", x.getValue());
	}

	@Test void b02_throwable_singleWithNullMessage() {
		var t = new RuntimeException();
		var x = Thrown.of(t);
		// Class name only, trailing ';' but no message
		assertEquals("java.lang.RuntimeException;", x.getValue());
	}

	@Test void b03_throwable_messageRequiringUrlEncoding() {
		var t = new RuntimeException("a, b & c");
		var x = Thrown.of(t);
		// Message is URL-encoded: comma -> %2C, ampersand -> %26, space -> +
		assertEquals("java.lang.RuntimeException;a%2C+b+%26+c", x.getValue());
	}

	@Test void b04_throwable_multipleEntries() {
		var a = new RuntimeException("first");
		var b = new IllegalArgumentException("second");
		var x = Thrown.of(a, b);
		assertEquals("java.lang.RuntimeException;first, java.lang.IllegalArgumentException;second", x.getValue());
	}

	@Test void b05_throwable_skipsNullEntries() {
		var a = new RuntimeException("first");
		var b = new IllegalStateException("third");
		// null entry between two valid ones is skipped, no leading separator before 'first'
		var x = Thrown.of(a, null, b);
		assertEquals("java.lang.RuntimeException;first, java.lang.IllegalStateException;third", x.getValue());

		// null entry before any valid one — no leading ", "
		var y = Thrown.of(null, a);
		assertEquals("java.lang.RuntimeException;first", y.getValue());
	}

	@Test void b06_throwable_nullArray() {
		var x = Thrown.of((Throwable[])null);
		assertEquals("Thrown", x.getName());
		assertEquals("", x.getValue());
	}

	@Test void b07_throwable_emptyArray() {
		var x = Thrown.of(new Throwable[0]);
		assertEquals("", x.getValue());
	}

	@Test void b08_throwable_allNullEntries() {
		var x = Thrown.of((Throwable)null, (Throwable)null);
		assertEquals("", x.getValue());
	}

	@Test void b09_throwable_nestedCause() {
		var cause = new IllegalStateException("root");
		var outer = new RuntimeException("outer", cause);
		var x = Thrown.of(outer);
		// Only outer message gets serialized, cause is not walked
		assertEquals("java.lang.RuntimeException;outer", x.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — Lazy variants
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazyWire() {
		var x = Thrown.ofLazyWire(() -> "java.lang.RuntimeException;late");
		assertEquals("Thrown", x.getName());
		assertEquals("java.lang.RuntimeException;late", x.getValue());
	}

	@Test void c02_lazyWire_nullSupplied() {
		Supplier<String> s = () -> null;
		var x = Thrown.ofLazyWire(s);
		assertNull(x.getValue());
	}

	@Test void c03_lazyTokens() {
		var x = Thrown.ofLazyTokens(() -> new String[]{"java.lang.RuntimeException;a", "java.lang.IllegalArgumentException;b"});
		assertEquals("Thrown", x.getName());
		assertEquals("java.lang.RuntimeException;a, java.lang.IllegalArgumentException;b", x.getValue());
	}

	@Test void c04_lazyTokens_nullSupplied() {
		Supplier<String[]> s = () -> null;
		var x = Thrown.ofLazyTokens(s);
		assertNull(x.getValue());
	}
}
