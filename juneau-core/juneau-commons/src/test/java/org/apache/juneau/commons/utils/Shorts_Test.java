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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Smoke tests for {@link Shorts}: one alias per domain section verifying delegation is wired correctly.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class Shorts_Test extends TestBase {

	// ---- ObjectUtils aliases ----

	@Test
	void a001_nn_notNull() { assertTrue(nn("x")); }

	@Test
	void a002_n_null() { assertTrue(n((Object) null)); }

	@Test
	void a003_eq_equal() { assertTrue(eq("a", "a")); }

	@Test
	void a004_neq_notEqual() { assertTrue(neq("a", "b")); }

	@Test
	void a005_cmp_compare() { assertEquals(0, cmp("a", "a")); }

	@Test
	void a006_lt_lessThan() { assertTrue(lt("a", "b")); }

	@Test
	void a007_def_coalesce() { assertEquals("y", ObjectUtils.coalesce(null, "y")); }

	@Test
	void a008_or_coalesceVarargs() { assertEquals("a", or(null, "a")); }

	@Test
	void a009_and_allTrue() { assertTrue(and(true, true)); }

	@Test
	void a010_not_negate() { assertTrue(not(false)); }

	@Test
	void a011_bool_toBoolean() { assertTrue(b("true")); }

	@Test
	void a012_s_stringify() { assertEquals("42", s(42)); }

	@Test
	void a013_h_hash() { assertEquals(ObjectUtils.hash("a"), h("a")); }

	@Test
	void a014_e_isEmpty_object() { assertTrue(ie((Object) null)); }

	@Test
	void a015_ne_isNotEmpty_object() { assertTrue(ine("x")); }

	@Test
	void a016_o_optional() { assertEquals(Optional.of("x"), o("x")); }

	@Test
	void a017_req_requireNonNull() { assertEquals("x", rnn("x")); }

	@Test
	void a018_sz_size() { assertEquals(0, sz(null)); }

	// ---- StringUtils aliases ----

	@Test
	void b001_e_isEmpty_charSeq() { assertTrue(ie("")); }

	@Test
	void b002_ne_isNotEmpty_charSeq() { assertTrue(ine("x")); }

	@Test
	void b003_f_format() { assertEquals("hello world", f("{0} {1}", "hello", "world")); }

	@Test
	void b004_b_isBlank() { assertTrue(ib("   ")); }

	@Test
	void b005_nb_isNotBlank() { assertTrue(inb("x")); }

	@Test
	void b006_lc_lowerCase() { assertEquals("abc", lc("ABC")); }

	@Test
	void b007_uc_upperCase() { assertEquals("ABC", uc("abc")); }

	@Test
	void b008_tr_trim() { assertEquals("x", tr("  x  ")); }

	@Test
	void b009_sw_startsWith() { assertTrue(sw("foobar", "foo")); }

	@Test
	void b010_ew_endsWith() { assertTrue(ew("foobar", "bar")); }

	@Test
	void b011_co_contains() { assertTrue(co("foobar", "oba")); }

	@Test
	void b012_ein_emptyIfNull_string() { assertEquals("", ein((String) null)); }

	@Test
	void b013_ein_emptyIfNull_object() { assertEquals("", ein((Object) null)); }

	@Test
	void b014_bin_blankIfNull() { assertEquals(" ", bin(" ")); }

	@Test
	void b015_nie_nullIfEmpty() { assertNull(nie("")); }

	@Test
	void b016_cn_className() { assertEquals(ClassUtils.className("x"), cn("x")); }

	@Test
	void b017_fne_firstNonEmpty() { assertEquals("a", fne("", null, "a")); }

	@Test
	void b018_b64_roundTrip() { assertEquals("hello", new String(StringUtils.base64Decode(StringUtils.base64Encode("hello".getBytes())))); }

	// ---- CollectionUtils aliases ----

	@Test
	void c001_a_array() { assertArrayEquals(new String[]{"x"}, a("x")); }

	@Test
	void c002_l_list() { assertEquals(List.of("x"), l("x")); }

	@Test
	void c003_set_newSet() { assertEquals(Set.of("x"), CollectionUtils.set("x")); }

	@Test
	void c004_at_elementAt() { assertEquals("b", at(l("a", "b", "c"), 1)); }

	@Test
	void c005_e_isEmpty_collection() { assertTrue(ie(List.of())); }

	@Test
	void c006_ne_isNotEmpty_collection() { assertTrue(ine(List.of("x"))); }

	@Test
	void c007_st_set() { assertEquals(Set.of("x"), st("x")); }

	@Test
	void c007a_ss_sortedSet() { assertEquals(Set.of("a", "b", "c"), ss("c", "a", "b")); }

	@Test
	void c008_ints_intArray() { assertArrayEquals(new int[]{1, 2}, ints(1, 2)); }

	@Test
	void c009_tl_toList() { assertEquals(List.of("a"), CollectionUtils.toList(Set.of("a"))); }

	// ---- ClassUtils aliases ----

	@Test
	void d001_cast_castTo() { assertEquals("x", cast(String.class, "x")); }

	@Test
	void d002_isArr_isArray() { assertTrue(ClassUtils.isArray(new int[0])); }

	@Test
	void d003_cns_classNameSimple() { assertEquals(ClassUtils.classNameSimple("x"), cns("x")); }

	// ---- ThrowableUtils aliases ----

	@Test
	void e001_rex_runtimeException() {
		RuntimeException e = rex("test {0}", "msg");
		assertEquals("test msg", e.getMessage());
	}

	@Test
	void e002_toRex_toRuntimeException() {
		RuntimeException e = ThrowableUtils.toRuntimeException(new IllegalStateException("wrap"));
		assertEquals("wrap", e.getMessage());
	}

	@Test
	void e003_ise_illegalState() {
		IllegalStateException e = isex("bad state {0}", 1);
		assertEquals("bad state 1", e.getMessage());
	}

	@Test
	void e004_gst_getStackTrace() {
		String st = gst(new RuntimeException("x"));
		assertTrue(st.contains("RuntimeException"));
	}

	@Test
	void e005_safe_safeRun_snippet() {
		int[] count = {0};
		safe(() -> count[0]++);
		assertEquals(1, count[0]);
	}

	@Test
	void e006_quiet_runQuietly() {
		int[] count = {0};
		quiet(() -> count[0]++);
		assertEquals(1, count[0]);
	}

	@Test
	void e007_safeOpt_safeOptional() {
		Optional<String> r = safeOpt(() -> "ok");
		assertEquals("ok", r.orElse(null));
	}

	@Test
	void e008_safeOrNull_safeRunOrNull() {
		String r = safeOrNull(() -> "ok");
		assertEquals("ok", r);
	}

	// ---- PredicateUtils aliases ----

	@Test
	void f001_test_testValue() { assertTrue(t(s -> s.equals("x"), "x")); }

	@Test
	void f002_dbk_distinctByKey() {
		var pred = PredicateUtils.distinctByKey((String s) -> s.length());
		assertTrue(pred.test("ab"));
		assertFalse(pred.test("cd"));
	}

	// ---- AssertionUtils (canonical; Shorts assertion aliases removed 2026-07-05) ----

	@Test
	void g001_assertArgNotNull() { assertEquals("x", AssertionUtils.assertArgNotNull("v", "x")); }

	@Test
	void g002_assertNotNull() { assertEquals("x", AssertionUtils.assertNotNull("x", "must not be null")); }

	@Test
	void g003_assertArg() {
		assertDoesNotThrow(() -> AssertionUtils.assertArg(true, "must be true"));
		assertThrows(IllegalArgumentException.class, () -> AssertionUtils.assertArg(false, "must be true"));
	}

	// ---- DateUtils alias ----

	@Test
	void h001_dtf_getDateTimeFormatter() {
		assertNotNull(DateUtils.getDateTimeFormatter("yyyy-MM-dd"));
	}
}
