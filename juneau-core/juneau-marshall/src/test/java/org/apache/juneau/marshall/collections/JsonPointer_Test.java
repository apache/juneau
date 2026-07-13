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
package org.apache.juneau.marshall.collections;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the RFC 6901 JSON-Pointer implementation {@link JsonPointer}.
 *
 * <p>
 * Covers parsing/validation, the RFC 6901 §5 example vectors, {@code ~0}/{@code ~1} token escaping,
 * nested read navigation, auto-vivifying writes (including the {@code -} append token), and removal.
 */
class JsonPointer_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// a - Parsing / validation / toString
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_emptyPointerIsRoot() {
		assertEquals("", JsonPointer.of("").toString());
	}

	@Test void a02_toStringRoundTrip() {
		assertEquals("/foo/0", JsonPointer.of("/foo/0").toString());
		assertEquals("/a~1b", JsonPointer.of("/a~1b").toString());
		assertEquals("/m~0n", JsonPointer.of("/m~0n").toString());
		assertEquals("/", JsonPointer.of("/").toString());
	}

	@Test void a03_malformedPointerThrows() {
		assertThrows(IllegalArgumentException.class, () -> JsonPointer.of("foo"));
		assertThrows(IllegalArgumentException.class, () -> JsonPointer.of("foo/bar"));
		assertThrows(IllegalArgumentException.class, () -> JsonPointer.of(null));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// b - Token escape / unescape
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_decodeToken() {
		assertEquals("a/b", JsonPointer.decodeToken("a~1b"));
		assertEquals("m~n", JsonPointer.decodeToken("m~0n"));
		// ~01 decodes to ~1 (the ~0 -> ~ is applied after ~1 -> /).
		assertEquals("~1", JsonPointer.decodeToken("~01"));
		assertEquals("plain", JsonPointer.decodeToken("plain"));
	}

	@Test void b02_encodeToken() {
		assertEquals("a~1b", JsonPointer.encodeToken("a/b"));
		assertEquals("m~0n", JsonPointer.encodeToken("m~n"));
		assertEquals("~01", JsonPointer.encodeToken("~1"));
		assertEquals("plain", JsonPointer.encodeToken("plain"));
	}

	@Test void b03_escapeRoundTrip() {
		for (var raw : new String[]{"a/b", "m~n", "~", "/", "a~1b", "c/d~e"})
			assertEquals(raw, JsonPointer.decodeToken(JsonPointer.encodeToken(raw)));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// c - RFC 6901 §5 example vectors
	// -----------------------------------------------------------------------------------------------------------------

	private static JsonMap rfcDoc() {
		return JsonMap.of(
			"foo", JsonList.of("bar", "baz"),
			"", 0,
			"a/b", 1,
			"c%d", 2,
			"e^f", 3,
			"g|h", 4,
			"i\\j", 5,
			"k\"l", 6,
			" ", 7,
			"m~n", 8
		);
	}

	@Test void c01_wholeDocument() {
		var doc = rfcDoc();
		assertSame(doc, JsonPointer.of("").eval(doc));
	}

	@Test void c02_array() {
		var doc = rfcDoc();
		assertEquals(JsonList.of("bar", "baz"), JsonPointer.of("/foo").eval(doc));
	}

	@Test void c03_arrayIndex() {
		assertEquals("bar", JsonPointer.of("/foo/0").eval(rfcDoc()));
	}

	@Test void c04_emptyStringKey() {
		assertEquals(0, JsonPointer.of("/").eval(rfcDoc()));
	}

	@Test void c05_escapedSlash() {
		assertEquals(1, JsonPointer.of("/a~1b").eval(rfcDoc()));
	}

	@Test void c06_percent() {
		assertEquals(2, JsonPointer.of("/c%d").eval(rfcDoc()));
	}

	@Test void c07_caret() {
		assertEquals(3, JsonPointer.of("/e^f").eval(rfcDoc()));
	}

	@Test void c08_pipe() {
		assertEquals(4, JsonPointer.of("/g|h").eval(rfcDoc()));
	}

	@Test void c09_backslash() {
		assertEquals(5, JsonPointer.of("/i\\j").eval(rfcDoc()));
	}

	@Test void c10_quote() {
		assertEquals(6, JsonPointer.of("/k\"l").eval(rfcDoc()));
	}

	@Test void c11_space() {
		assertEquals(7, JsonPointer.of("/ ").eval(rfcDoc()));
	}

	@Test void c12_escapedTilde() {
		assertEquals(8, JsonPointer.of("/m~0n").eval(rfcDoc()));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// d - Read navigation + miss behavior
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_nested() {
		var doc = JsonMap.of("a", JsonMap.of("b", JsonList.of("x", "y", "z")));
		assertEquals("z", JsonPointer.of("/a/b/2").eval(doc));
	}

	@Test void d02_missingKey() {
		assertNull(JsonPointer.of("/missing").eval(JsonMap.of("a", 1)));
		assertNull(JsonPointer.of("/a/missing").eval(JsonMap.of("a", JsonMap.of("b", 1))));
	}

	@Test void d03_indexOutOfRange() {
		assertNull(JsonPointer.of("/0").eval(JsonList.of()));
		assertNull(JsonPointer.of("/5").eval(JsonList.of("x")));
	}

	@Test void d04_typeMismatch() {
		// Descending into a scalar yields a miss.
		assertNull(JsonPointer.of("/a/b").eval(JsonMap.of("a", "scalar")));
		// Non-numeric token on an array yields a miss.
		assertNull(JsonPointer.of("/x").eval(JsonList.of(1, 2)));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// e - Write (set) + auto-vivify + append
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_replaceExisting() {
		var root = JsonMap.of("a", 1);
		assertSame(root, JsonPointer.of("/a").set(root, 2));
		assertEquals(2, JsonPointer.of("/a").eval(root));
	}

	@Test void e02_vivifyNestedObject() {
		var root = new JsonMap();
		JsonPointer.of("/a/b/c").set(root, 1);
		assertEquals("{\"a\":{\"b\":{\"c\":1}}}", Json.of(root));
	}

	@Test void e03_vivifyNestedArray() {
		var root = new JsonMap();
		JsonPointer.of("/a/0").set(root, "x");
		assertEquals("{\"a\":[\"x\"]}", Json.of(root));
		assertInstanceOf(JsonList.class, root.get("a"));
	}

	@Test void e04_appendToken() {
		var root = JsonMap.of("a", JsonList.of(1, 2));
		JsonPointer.of("/a/-").set(root, 3);
		assertEquals("{\"a\":[1,2,3]}", Json.of(root));
	}

	@Test void e05_rootPointerReturnsValue() {
		assertEquals("v", JsonPointer.of("").set(JsonMap.of("a", 1), "v"));
	}

	@Test void e06_setIndexOutOfRangeThrows() {
		// Setting at an index beyond the end of an existing list (not the append slot) is an error.
		var root = JsonMap.of("a", JsonList.of(1, 2));
		var p = JsonPointer.of("/a/5");
		assertThrows(IllegalArgumentException.class, () -> p.set(root, 9));
	}

	@Test void e07_setReplaceAtExistingIndex() {
		var root = JsonMap.of("a", JsonList.of(1, 2, 3));
		JsonPointer.of("/a/1").set(root, 9);
		assertEquals("{\"a\":[1,9,3]}", Json.of(root));
	}

	@Test void e08_vivifyListViaDashToken() {
		// A '-' next token makes the auto-vivified intermediate container a list, and appends to it.
		var root = new JsonMap();
		JsonPointer.of("/a/-").set(root, "x");
		assertEquals("{\"a\":[\"x\"]}", Json.of(root));
		assertInstanceOf(JsonList.class, root.get("a"));
	}

	@Test void e09_descendThroughExistingObject() {
		// navigateToParent descends through an already-present intermediate map without re-vivifying it.
		var root = JsonMap.of("a", new JsonMap());
		JsonPointer.of("/a/b").set(root, 1);
		assertEquals("{\"a\":{\"b\":1}}", Json.of(root));
	}

	@Test void e10_setNonNumericListTokenThrows() {
		// A non-numeric, non-'-' token on a list is an invalid array index on write.
		var root = JsonMap.of("a", JsonList.of(1, 2));
		var p = JsonPointer.of("/a/foo");
		assertThrows(IllegalArgumentException.class, () -> p.set(root, 9));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// f - Remove
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_removeMember() {
		var root = JsonMap.of("a", 1, "b", 2);
		assertEquals(1, JsonPointer.of("/a").remove(root));
		assertEquals("{\"b\":2}", Json.of(root));
	}

	@Test void f02_removeArrayElement() {
		var root = JsonMap.of("a", JsonList.of("x", "y", "z"));
		assertEquals("y", JsonPointer.of("/a/1").remove(root));
		assertEquals("{\"a\":[\"x\",\"z\"]}", Json.of(root));
	}

	@Test void f03_removeAbsentReturnsNull() {
		assertNull(JsonPointer.of("/missing").remove(JsonMap.of("a", 1)));
		assertNull(JsonPointer.of("/a/5").remove(JsonMap.of("a", JsonList.of("x"))));
	}

	@Test void f04_removeRootReturnsNull() {
		// The root pointer addresses the whole document, which cannot be removed in place.
		assertNull(JsonPointer.of("").remove(JsonMap.of("a", 1)));
	}

	@Test void f05_removeMissingIntermediateReturnsNull() {
		assertNull(JsonPointer.of("/a/b/c").remove(new JsonMap()));
	}

	@Test void f06_removeNonNumericListTokenReturnsNull() {
		var root = JsonMap.of("a", JsonList.of("x", "y"));
		assertNull(JsonPointer.of("/a/-").remove(root));
		assertNull(JsonPointer.of("/a/foo").remove(root));
		// The list is left unchanged.
		assertEquals("{\"a\":[\"x\",\"y\"]}", Json.of(root));
	}

	@Test void f07_removeThroughScalarReturnsNull() {
		assertNull(JsonPointer.of("/a/b").remove(JsonMap.of("a", "scalar")));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// g - Array index token parsing
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_emptyTokenOnArrayMisses() {
		// An empty reference token is not a valid array index, so it resolves to a miss.
		assertNull(JsonPointer.of("/").eval(JsonList.of("x")));
	}

	@Test void g02_leadingZeroIndexRejected() {
		// RFC 6901 forbids leading zeros in array indices, so they resolve to a miss.
		assertNull(JsonPointer.of("/01").eval(JsonList.of("a", "b")));
		assertNull(JsonPointer.of("/00").eval(JsonList.of("a", "b")));
	}

	@Test void g03_multiDigitIndex() {
		var l = new JsonList();
		for (var i = 0; i < 12; i++)
			l.add(i);
		assertEquals(10, JsonPointer.of("/10").eval(l));
	}

	@Test void g04_overflowingIndexMisses() {
		// An all-digit token too large to fit in an int is treated as a non-index (miss), not an error.
		assertNull(JsonPointer.of("/99999999999999999999").eval(JsonList.of("x")));
	}
}
