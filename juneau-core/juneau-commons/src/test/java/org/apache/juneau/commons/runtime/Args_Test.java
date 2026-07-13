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
package org.apache.juneau.commons.runtime;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S5961", // High assertion count acceptable in comprehensive test
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class Args_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a - Basic positional + legacy short-flag parsing
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_emptyArgs() {
		var x = new Args(a());
		assertTrue(x.isEmpty());
		assertEquals(0, x.argCount());
		assertEquals(0, x.optionCount());
		assertFalse(x.has(0));
		assertFalse(x.has("foo"));
		assertTrue(x.get(0).isEmpty());
		assertTrue(x.get(-1).isEmpty());
		assertTrue(x.get("foo").isEmpty());
		assertTrue(x.getAll("foo").isEmpty());
	}

	@Test void a02_singlePositional() {
		var x = new Args(a("foo"));
		assertEquals("foo", x.get(0).orElse(null));
		assertTrue(x.get(1).isEmpty());
		assertTrue(x.get(-1).isEmpty());
		assertTrue(x.get("foo").isEmpty());
		assertEquals(1, x.argCount());
		assertEquals(0, x.optionCount());
		assertFalse(x.isEmpty());
	}

	@Test void a03_multiplePositionals() {
		var x = new Args(a("foo", "bar bar"));
		assertEquals("foo", x.get(0).orElse(null));
		assertEquals("bar bar", x.get(1).orElse(null));
		assertEquals(2, x.argCount());
		assertEquals(2, x.positional().size());
	}

	@Test void a04_flagWithoutValue() {
		var x = new Args(a("foo", "bar bar", "-foo"));
		assertEquals("foo", x.get(0).orElse(null));
		assertEquals("bar bar", x.get(1).orElse(null));
		assertTrue(x.has("foo"));
		assertTrue(x.get("foo").isEmpty());
		assertTrue(x.getAll("foo").isEmpty());
	}

	@Test void a05_flagWithSingleValue() {
		var x = new Args(a("foo", "bar bar", "-foo", "bar bar"));
		assertEquals("foo", x.get(0).orElse(null));
		assertEquals("bar bar", x.get(1).orElse(null));
		assertEquals("bar bar", x.get("foo").orElse(null));
		assertEquals(1, x.getAll("foo").size());
		assertTrue(x.has("foo"));
	}

	@Test void a06_repeatedFlag() {
		var x = new Args(a("-tag", "a", "-tag", "b", "-tag", "c"));
		assertEquals("a", x.get("tag").orElse(null));
		assertEquals(l("a", "b", "c"), x.getAll("tag"));
	}

	@Test void a07_flagAcceptsMultipleValues() {
		var x = new Args(a("-tag", "a", "b", "c"));
		assertEquals("a", x.get("tag").orElse(null));
		assertEquals(l("a", "b", "c"), x.getAll("tag"));
	}

	@Test void a08_positionalsStopAtFirstFlag() {
		var x = new Args(a("p1", "-key", "v1", "p2"));
		assertEquals(l("p1"), x.positional());
		assertEquals(l("v1", "p2"), x.getAll("key"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - String-line constructor
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_stringConstructor() {
		var x = new Args("foo \"bar bar\" -opt v1 -opt v2");
		assertEquals("foo", x.get(0).orElse(null));
		assertEquals("bar bar", x.get(1).orElse(null));
		assertEquals(l("v1", "v2"), x.getAll("opt"));
	}

	@Test void b02_nullStringConstructor() {
		var x = new Args((String) null);
		assertTrue(x.isEmpty());
	}

	@Test void b03_nullArrayConstructor() {
		var x = new Args((String[]) null);
		assertTrue(x.isEmpty());
	}

	@Test void b04_nullTokensSkipped() {
		var x = new Args(new String[]{"foo", null, "-key", null, "v"});
		assertEquals("foo", x.get(0).orElse(null));
		assertEquals("v", x.get("key").orElse(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - Builder grammar hooks
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_longFlags() {
		var x = Args.create().build(a("--name", "alice", "--name", "bob"));
		assertEquals(l("alice", "bob"), x.getAll("name"));
	}

	@Test void c02_equalsForm() {
		var x = Args.create().build(a("--name=alice", "--port=8080"));
		assertEquals("alice", x.get("name").orElse(null));
		assertEquals("8080", x.get("port").orElse(null));
	}

	@Test void c03_systemPropStyle() {
		var x = Args.create().build(a("-Dfoo=bar", "-Dbaz=qux"));
		assertEquals("bar", x.get("foo").orElse(null));
		assertEquals("qux", x.get("baz").orElse(null));
	}

	@Test void c04_disableEquals() {
		var x = Args.create().allowEquals(false).build(a("--name=alice"));
		assertTrue(x.has("name=alice"));
		assertTrue(x.getAll("name=alice").isEmpty());
	}

	@Test void c05_disableShortFlags() {
		var x = Args.create().allowShortFlags(false).build(a("-foo", "bar"));
		// Without short-flag prefix, '-' tokens are no longer flags but '--' is still long.
		// Token "-foo" stays positional because there's no current key.
		assertEquals(l("-foo", "bar"), x.positional());
	}

	@Test void c06_disableLongFlags() {
		var x = Args.create().allowLongFlags(false).build(a("--name", "alice"));
		// Without long-flag prefix, "--" doesn't match; the short prefix "-" matches "--name" -> body="-name".
		assertTrue(x.has("-name"));
		assertEquals("alice", x.get("-name").orElse(null));
	}

	@Test void c07_disableSystemPropStyle() {
		var x = Args.create().allowSystemPropStyle(false).build(a("-Dfoo=bar"));
		// Equals form still applies; key body = "Dfoo".
		assertEquals("bar", x.get("Dfoo").orElse(null));
	}

	@Test void c08_caseInsensitive() {
		var x = Args.create().caseSensitive(false).build(a("-Foo", "value", "-FOO", "another"));
		assertEquals(l("value", "another"), x.getAll("foo"));
		assertEquals("value", x.get("FOO").orElse(null));
		assertTrue(x.has("FoO"));
	}

	@Test void c09_customPrefix() {
		var x = Args.create().customPrefix("/").build(a("/key", "value", "/flag"));
		assertEquals("value", x.get("key").orElse(null));
		assertTrue(x.has("flag"));
	}

	@Test void c10_customPrefixEmpty() {
		var x = Args.create().customPrefix().build(a("-foo", "bar", "--baz"));
		// No prefixes -> everything is positional.
		assertEquals(l("-foo", "bar", "--baz"), x.positional());
	}

	@Test void c11_customPrefixSupersedesShortLong() {
		// customPrefix REPLACES the default short/long settings.
		var x = Args.create().allowShortFlags(false).allowLongFlags(false).customPrefix("+").build(a("+foo", "v"));
		assertEquals("v", x.get("foo").orElse(null));
	}

	@Test void c12_longestPrefixWins() {
		// With both "-" and "--", "--foo" should match "--" not "-".
		var x = Args.create().build(a("--foo", "v"));
		assertEquals("v", x.get("foo").orElse(null));
		assertFalse(x.has("-foo"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - asMap / positional accessors
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_asMapNamedOnly() {
		var x = new Args(a("p0", "p1", "-k", "v"));
		var map = x.asMap();
		assertEquals(1, map.size());
		assertTrue(map.containsKey("k"));
		assertEquals(l("v"), map.get("k"));
	}

	@Test void d02_asMapUnmodifiable() {
		var x = new Args(a("-k", "v"));
		assertThrows(UnsupportedOperationException.class, () -> x.asMap().put("nope", l("x")));
	}

	@Test void d03_positionalUnmodifiable() {
		var x = new Args(a("p0"));
		assertThrows(UnsupportedOperationException.class, () -> x.positional().add("nope"));
	}

	@Test void d04_getAllReturnsUnmodifiable() {
		var x = new Args(a("-k", "v"));
		assertThrows(UnsupportedOperationException.class, () -> x.getAll("k").add("nope"));
	}

	@Test void d05_optionCountAndArgCount() {
		var x = new Args(a("p0", "p1", "-k1", "v1", "-k2", "v2", "-k1", "v1b"));
		assertEquals(2, x.argCount());
		assertEquals(2, x.optionCount());
	}
}
