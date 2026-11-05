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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/** Tests for {@link OpFunctions}. */
class OpFunctions_Test extends TestBase {

	private static final VarResolver LENIENT = VarResolver.create().defaultFunctions().build();
	private static final VarResolver STRICT = VarResolver.create().defaultFunctions().strict(true).build();

	private static Object op(VarResolver vr, String name, Object...args) {
		var s = vr.createSession();
		try {
			return s.findFunction("$op." + name).invokeValue(s, Arrays.asList(args));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Object op(String name, Object...args) { return op(LENIENT, name, args); }

	@Test void a01_add() {
		assertEquals(3L, op("add", 1L, 2L));
		assertEquals("a1", op("add", "a", 1L));
		assertEquals("1a", op("add", 1L, "a"));
		assertEquals("anull", op("add", "a", null));
		assertEquals(2.5, op("add", 1L, 1.5));
		assertEquals(3L, op("add", 1, 2));
	}

	@Test void a02_overflow() {
		assertEquals(new BigDecimal("9223372036854775808"), op("add", Long.MAX_VALUE, 1L));
		assertEquals(new BigDecimal("-9223372036854775809"), op("sub", Long.MIN_VALUE, 1L));
		assertEquals(new BigDecimal("18446744073709551614"), op("mul", Long.MAX_VALUE, 2L));
		assertEquals(new BigDecimal("9223372036854775808"), op("neg", Long.MIN_VALUE));
	}

	@Test void a03_bigdecimal_and_double() {
		assertEquals(new BigDecimal("3.5"), op("add", new BigDecimal("1.5"), 2L));
		assertEquals(3.5, op("add", new BigDecimal("1.5"), 2.0));
	}

	@Test void a04_subMulDivMod() {
		assertEquals(-1L, op("sub", 1L, 2L));
		assertEquals(6L, op("mul", 2L, 3L));
		assertEquals(3L, op("div", 7L, 2L));
		assertEquals(3.5, op("div", 7L, 2.0));
		assertEquals(2L, op("div", 6L, 3L));
		assertEquals(1L, op("mod", 7L, 3L));
		assertEquals(Double.POSITIVE_INFINITY, op("div", 1L, 0.0));
		assertTrue(((Double)op("mod", 1L, 0.0)).isNaN());
	}

	@Test void a05_zeroDivision() {
		assertThrows(VarResolverException.class, () -> op("div", 1L, 0L));
		var m = assertThrows(VarResolverException.class, () -> op("mod", 1L, 0L));
		assertEquals("Operator '%' modulo by zero", m.getMessage());
		var d = assertThrows(VarResolverException.class, () -> op("div", 1L, 0L));
		assertEquals("Operator '/' division by zero", d.getMessage());
		assertThrows(VarResolverException.class, () -> op("div", new BigDecimal("1"), new BigDecimal("0")));
		var bm = assertThrows(VarResolverException.class, () -> op("mod", new BigDecimal("1"), new BigDecimal("0")));
		assertEquals("Operator '%' modulo by zero", bm.getMessage());
	}

	@Test void a06_nonNumericErrors() {
		var e = assertThrows(VarResolverException.class, () -> op("sub", "a", 1L));
		assertEquals("Operator '-' requires a numeric operand but got String", e.getMessage());
		assertThrows(VarResolverException.class, () -> op("mul", 1L, null));
		assertThrows(VarResolverException.class, () -> op("div", true, 1L));
		assertThrows(VarResolverException.class, () -> op("mod", 1L, "x"));
		assertThrows(VarResolverException.class, () -> op("add", true, 1L));
		assertThrows(VarResolverException.class, () -> op("neg", "x"));
		assertThrows(VarResolverException.class, () -> op("pos", "x"));
	}

	@Test void a07_unary() {
		assertEquals(-5L, op("neg", 5L));
		assertEquals(-1.5, op("neg", 1.5));
		assertEquals(new BigDecimal("-2"), op("neg", new BigDecimal("2")));
		assertEquals(5L, op("pos", 5L));
	}

	@Test void b01_eqNe() {
		assertEquals(true, op("eq", 1L, 1.0));
		assertEquals(true, op("eq", null, null));
		assertEquals(false, op("eq", null, "a"));
		assertEquals(true, op("eq", "a", "a"));
		assertEquals(false, op("eq", "1", 1L));
		assertEquals(true, op("ne", 1L, 2L));
		assertEquals(false, op("ne", 1L, 1.0));
	}

	@Test void b02_relational() {
		assertEquals(true, op("lt", 1L, 2.5));
		assertEquals(true, op("le", 2L, 2L));
		assertEquals(true, op("gt", "b", "a"));
		assertEquals(true, op("ge", "a", "a"));
		assertEquals(false, op("lt", 2L, 1L));
		var le = assertThrows(VarResolverException.class, () -> op("lt", "a", 1L));
		assertEquals("Operator '<' cannot compare String with Long", le.getMessage());
		assertThrows(VarResolverException.class, () -> op("gt", new Object(), new Object()));
	}

	@Test void b03_nullStrictVsLenient() {
		for (var n : List.of("lt", "le", "gt", "ge")) {
			assertEquals(false, op(LENIENT, n, null, 1L), n);
			assertEquals(false, op(LENIENT, n, 1L, null), n);
			assertThrows(VarResolverException.class, () -> op(STRICT, n, null, 1L), n);
		}
	}

	@Test void c01_boolean() {
		assertEquals(true, op("and", true, "true"));
		assertEquals(false, op("and", true, "false"));
		assertEquals(true, op("or", false, "true"));
		assertEquals(false, op("or", "false", false));
		assertEquals(true, op("not", "false"));
		assertEquals(false, op("not", true));
		assertThrows(VarResolverException.class, () -> op("and", 1L, true));
		assertThrows(VarResolverException.class, () -> op("or", "yes", true));
		assertThrows(VarResolverException.class, () -> op("not", (Object)null));
	}

	@Test void c02_elvis() {
		assertEquals("", op("elvis", "", "x"));
		assertEquals(false, op("elvis", false, "x"));
		assertEquals(0L, op("elvis", 0L, "x"));
		assertEquals("x", op("elvis", null, "x"));
	}

	@Test void c03_ternary() {
		assertEquals("a", op("ternary", true, "a", "b"));
		assertEquals("b", op("ternary", "false", "a", "b"));
		assertThrows(VarResolverException.class, () -> op("ternary", 1L, "a", "b"));
	}

	@Test void d01_matches() {
		assertEquals(true, op("matches", "abc123", "[a-z]+\\d+"));
		assertEquals(false, op("matches", "abc123x", "[a-z]+\\d+"));
		var e = assertThrows(VarResolverException.class, () -> op("matches", "a", "("));
		assertTrue(e.getMessage().contains("invalid regex"), e.getMessage());
		assertThrows(VarResolverException.class, () -> op("matches", 1L, "a"));
		assertThrows(VarResolverException.class, () -> op("matches", "a", null));
	}

	@Test void e01_stubs() {
		assertEquals("evaluator-only", assertThrows(UnsupportedOperationException.class, () -> op("select", "a", "b")).getMessage());
		assertEquals("evaluator-only", assertThrows(UnsupportedOperationException.class, () -> op("project", "a", "b")).getMessage());
	}

	@Test void e02_registration() {
		var s = LENIENT.createSession();
		for (var n : List.of("add", "sub", "mul", "div", "mod", "eq", "ne", "lt", "le", "gt", "ge", "and", "or", "not", "neg", "pos", "elvis", "ternary", "matches", "select", "project")) {
			var f = s.findFunction("$op." + n);
			assertNotNull(f, n);
			assertEquals("$op." + n, f.name());
		}
	}

	@Test void e03_plainNamesNotShadowed() {
		var s = LENIENT.createSession();
		var add = s.findFunction("add");
		assertNotNull(add);
		assertFalse(add instanceof OpFunctions.Op);
		assertFalse(s.findFunction("eq") instanceof OpFunctions.Op);
		assertNull(s.findFunction("elvis"));
		assertNull(s.findFunction("$op.nope"));
		assertNull(s.findFunction("nope"));
		assertEquals("5", LENIENT.resolve("#{add(2, 3)}"));
	}

	@Test void e04_arity() {
		assertThrows(VarResolverException.class, () -> op("add", 1L));
	}

	@Test void f01_nanAndSignedZero() {
		assertEquals(true, op("eq", 0.0, -0.0));
		assertEquals(false, op("eq", Double.NaN, Double.NaN));
		assertEquals(true, op("ne", Double.NaN, Double.NaN));
		for (var n : List.of("lt", "le", "gt", "ge")) {
			assertEquals(false, op(n, Double.NaN, 1.0), n);
			assertEquals(false, op(n, 1L, Double.NaN), n);
			assertEquals(false, op(n, Double.NaN, Double.NaN), n);
		}
		assertEquals(true, op("le", 0.0, -0.0));
		assertEquals(true, op("ge", -0.0, 0.0));
	}

	@Test void f02_longVsDoublePrecision() {
		assertEquals(false, op("eq", 9007199254740993L, 9007199254740992.0));
		assertEquals(true, op("gt", 9007199254740993L, 9007199254740992.0));
		assertEquals(true, op("eq", 9007199254740992L, 9007199254740992.0));
		assertEquals(true, op("lt", 5L, Double.POSITIVE_INFINITY));
		assertEquals(true, op("gt", 5L, Double.NEGATIVE_INFINITY));
		assertEquals(false, op("eq", 5L, Double.POSITIVE_INFINITY));
	}

	@Test void f03_concatRendering() {
		assertEquals("a1", op("add", "a", 1.0));
		assertEquals("a1.5", op("add", "a", 1.5));
		assertEquals("1b", op("add", 1.0f, "b"));
		assertEquals("a100000000", op("add", "a", new BigDecimal("1E+8")));
		assertEquals("a0.00001", op("add", "a", new BigDecimal("1E-5")));
		assertEquals("anull", op("add", "a", null));
		assertEquals("nullb", op("add", null, "b"));
		assertEquals("atrue", op("add", "a", true));
	}

	@Test void f04_andOrValidateBoth() {
		assertThrows(VarResolverException.class, () -> op("and", false, 1L));
		assertThrows(VarResolverException.class, () -> op("or", true, "yes"));
		assertThrows(VarResolverException.class, () -> op("and", false, null));
	}

	@Test void f05_boolCaseInsensitive() {
		assertEquals(true, op("not", "FALSE"));
		assertEquals(true, op("and", "TRUE", "True"));
	}

	@Test void f06_smallTypes() {
		assertEquals(3L, op("add", (byte)1, (short)2));
		assertEquals(4.0, op("mul", 2.0f, 2L));
		assertEquals(new BigDecimal("5"), op("add", BigInteger.valueOf(2), BigInteger.valueOf(3)));
		assertEquals(new BigDecimal("5"), op("add", BigInteger.valueOf(2), 3L));
		assertEquals(true, op("eq", BigInteger.TEN, 10L));
	}

	@Test void f07_modNegatives() {
		assertEquals(-1L, op("mod", -7L, 3L));
		assertEquals(1L, op("mod", 7L, -3L));
	}

	@Test void f08_minValueByMinusOne() {
		// Long.MIN_VALUE / -1 overflows long: promoted to BigDecimal.  Long.MIN_VALUE % -1 is mathematically 0.
		assertEquals(new BigDecimal("9223372036854775808"), op("div", Long.MIN_VALUE, -1L));
		assertEquals(0L, op("mod", Long.MIN_VALUE, -1L));
	}

	@Test void f09_boundaries() {
		assertEquals(true, op("le", 1L, 2L));
		assertEquals(false, op("le", 3L, 2L));
		assertEquals(true, op("ge", 2L, 2L));
		assertEquals(false, op("ge", 1L, 2L));
		assertEquals(false, op("lt", 2L, 2L));
		assertEquals(false, op("gt", 2L, 2L));
	}

	@Test void f10_arity() {
		for (var n : List.of("not", "neg", "pos")) {
			assertThrows(VarResolverException.class, () -> op(n), n);
			assertThrows(VarResolverException.class, () -> op(n, 1L, 2L), n);
		}
		for (var n : List.of("add", "sub", "mul", "div", "mod", "eq", "ne", "lt", "le", "gt", "ge", "and", "or", "elvis", "matches", "select", "project")) {
			assertThrows(VarResolverException.class, () -> op(n, 1L), n);
			assertThrows(VarResolverException.class, () -> op(n, 1L, 2L, 3L), n);
		}
		assertThrows(VarResolverException.class, () -> op("ternary", true, 1L));
		assertThrows(VarResolverException.class, () -> op("ternary", true, 1L, 2L, 3L));
	}

	@Test void f11_invokeString() {
		var s = LENIENT.createSession();
		assertEquals("3", s.findFunction("$op.add").invoke(s, Arrays.asList(1L, 2L)));
		assertEquals("", s.findFunction("$op.elvis").invoke(s, Arrays.asList(null, null)));
		assertEquals("true", s.findFunction("$op.eq").invoke(s, Arrays.asList(null, null)));
	}

	@Test void f12_matchesUsesSharedCache() {
		var re = "uniq-" + System.nanoTime() + "[a-z]+";
		var before = RegexFunctions.PATTERN_CACHE.size();
		assertEquals(true, op("matches", re.replace("[a-z]+", "abc"), re));
		assertEquals(before + 1, RegexFunctions.PATTERN_CACHE.size());
		// Cached by RegexFunctions' own function: no growth when reused via match().
		assertEquals("true", LENIENT.resolve("#{match(\"" + re.replace("[a-z]+", "abc") + "\", \"" + re + "\")}"));
		assertEquals(before + 1, RegexFunctions.PATTERN_CACHE.size());
	}
}
