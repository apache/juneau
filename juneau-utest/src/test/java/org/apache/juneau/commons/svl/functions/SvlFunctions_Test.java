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

import org.apache.juneau.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/**
 * Expanded edge-case tests for all SVL function categories to increase instruction coverage.
 * Supplements the per-category *_Test files with null/empty/boundary inputs.
 */
class SvlFunctions_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// ArithmeticFunctions — edge cases
	//------------------------------------------------------------------------------------------------------------------

	private final VarResolver arith = VarResolver.create().functions(ArithmeticFunctions.ALL).build();

	@Test void a01_divide_byZero_infinity() { assertEquals("Infinity", arith.resolve("#{divide(1, 0)}")); }
	@Test void a02_divide_zeroByZero_nan() { assertEquals("NaN", arith.resolve("#{divide(0, 0)}")); }
	@Test void a03_modulo_negative() { assertEquals("-1", arith.resolve("#{modulo(-7, 3)}")); }
	@Test void a04_add_negatives() { assertEquals("-5", arith.resolve("#{add(-2, -3)}")); }
	@Test void a05_multiply_byZero() { assertEquals("0", arith.resolve("#{multiply(99, 0)}")); }
	@Test void a06_subtract_yieldsNegative() { assertEquals("-5", arith.resolve("#{subtract(3, 8)}")); }
	@Test void a07_min_negatives() { assertEquals("-10", arith.resolve("#{min(-10, -5)}")); }
	@Test void a08_max_negatives() { assertEquals("-5", arith.resolve("#{max(-10, -5)}")); }
	@Test void a09_abs_zero() { assertEquals("0", arith.resolve("#{abs(0)}")); }
	@Test void a10_add_largeInts() { assertEquals("2000000000", arith.resolve("#{add(1000000000, 1000000000)}")); }
	@Test void a11_divide_negInfinity() { assertEquals("-Infinity", arith.resolve("#{divide(-1, 0)}")); }

	//------------------------------------------------------------------------------------------------------------------
	// BooleanFunctions — edge cases
	//------------------------------------------------------------------------------------------------------------------

	private final VarResolver bool = VarResolver.create().functions(BooleanFunctions.ALL).build();

	@Test void b01_and_empty() { assertEquals("true", bool.resolve("#{and(true)}")); }
	@Test void b02_or_single() { assertEquals("false", bool.resolve("#{or(false)}")); }
	@Test void b03_not_one() { assertEquals("false", bool.resolve("#{not(1)}")); }
	@Test void b04_not_zero() { assertEquals("true", bool.resolve("#{not(0)}")); }
	@Test void b05_and_onOffYesNo() { assertEquals("true", bool.resolve("#{and(on, yes, 1, true)}")); }
	@Test void b06_or_allFalsyForms() { assertEquals("false", bool.resolve("#{or(off, no, 0, false)}")); }
	@Test void b07_xor_falseFalse() { assertEquals("false", bool.resolve("#{xor(false, false)}")); }
	@Test void b08_lt_equal() { assertEquals("false", bool.resolve("#{lt(5, 5)}")); }
	@Test void b09_gt_equal() { assertEquals("false", bool.resolve("#{gt(5, 5)}")); }
	@Test void b10_lte_less() { assertEquals("true", bool.resolve("#{lte(3, 5)}")); }
	@Test void b11_gte_greater() { assertEquals("true", bool.resolve("#{gte(5, 3)}")); }
	@Test void b12_lt_negative() { assertEquals("true", bool.resolve("#{lt(-10, -5)}")); }
	@Test void b13_eq_empty() { assertEquals("true", bool.resolve("#{eq(\"\", \"\")}")); }
	@Test void b14_neq_empty() { assertEquals("false", bool.resolve("#{neq(\"\", \"\")}")); }
	@Test void b15_and_invalidCoercion() {
		assertThrows(IllegalArgumentException.class, () -> bool.resolve("#{and(true, maybe)}"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// ConditionalFunctions — edge cases
	//------------------------------------------------------------------------------------------------------------------

	private final VarResolver cond = VarResolver.create().functions(ConditionalFunctions.ALL).build();

	@Test void c01_if_onIsTruthy() { assertEquals("yes", cond.resolve("#{if(on, yes, no)}")); }
	@Test void c02_if_offIsFalsy() { assertEquals("no", cond.resolve("#{if(off, yes, no)}")); }
	@Test void c03_if_zeroIsFalsy() { assertEquals("no", cond.resolve("#{if(0, yes, no)}")); }
	@Test void c04_switch_firstArgEmpty() { assertEquals("empty", cond.resolve("#{switch(\"\", \"\", empty, *, other)}")); }
	@Test void c05_switch_singleDefault() { assertEquals("def", cond.resolve("#{switch(x, def)}")); }
	@Test void c06_switch_noArgsEmpty() { assertEquals("", cond.resolve("#{switch(\"\")}")); }
	@Test void c07_coalesce_single() { assertEquals("only", cond.resolve("#{coalesce(only)}")); }
	@Test void c08_notEmpty_whitespace() { assertEquals("true", cond.resolve("#{notEmpty(\" \")}")); }

	//------------------------------------------------------------------------------------------------------------------
	// EncodingFunctions — edge cases
	//------------------------------------------------------------------------------------------------------------------

	private final VarResolver enc = VarResolver.create().functions(EncodingFunctions.ALL).build();

	@Test void d01_base64Encode_empty() { assertEquals("", enc.resolve("#{base64Encode(\"\")}")); }
	@Test void d02_base64Decode_empty() { assertEquals("", enc.resolve("#{base64Decode(\"\")}")); }
	@Test void d03_urlEncode_empty() { assertEquals("", enc.resolve("#{urlEncode(\"\")}")); }
	@Test void d04_urlDecode_empty() { assertEquals("", enc.resolve("#{urlDecode(\"\")}")); }
	@Test void d05_htmlEscape_noSpecial() { assertEquals("hello", enc.resolve("#{htmlEscape(hello)}")); }
	@Test void d06_htmlUnescape_noEntities() { assertEquals("hello", enc.resolve("#{htmlUnescape(hello)}")); }
	@Test void d07_htmlEscape_singleQuote() { assertEquals("&#39;", enc.resolve("#{htmlEscape(\"'\")}")); }
	@Test void d08_htmlUnescape_apos() { assertEquals("'", enc.resolve("#{htmlUnescape(\"&apos;\")}")); }
	@Test void d09_htmlUnescape_hexEntity() { assertEquals("A", enc.resolve("#{htmlUnescape(\"&#x41;\")}")); }
	@Test void d10_htmlUnescape_unknownEntity() { assertEquals("&foo;", enc.resolve("#{htmlUnescape(\"&foo;\")}")); }
	@Test void d11_htmlEscape_quot() { assertEquals("&quot;", enc.resolve("#{htmlEscape(\"\\\"\")}")); }
	@Test void d12_urlEncode_specialChars() { assertEquals("%2F%3F%23", enc.resolve("#{urlEncode(\"/?#\")}")); }
	@Test void d13_base64_roundtrip_unicode() {
		var encoded = enc.resolve("#{base64Encode(\"éèê\")}");
		assertFalse(encoded.isEmpty());
		assertEquals("éèê", enc.resolve("#{base64Decode(" + encoded + ")}"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// StringFunctions — edge cases
	//------------------------------------------------------------------------------------------------------------------

	private final VarResolver str = VarResolver.create().functions(StringFunctions.ALL).build();

	@Test void e01_substring_beyondLength() { assertEquals("", str.resolve("#{substring(hi, 99)}")); }
	@Test void e02_upper_empty() { assertEquals("", str.resolve("#{upper(\"\")}")); }
	@Test void e03_lower_empty() { assertEquals("", str.resolve("#{lower(\"\")}")); }
	@Test void e04_trim_empty() { assertEquals("", str.resolve("#{trim(\"\")}")); }
	@Test void e05_stripSlashes_noSlashes() { assertEquals("abc", str.resolve("#{stripSlashes(abc)}")); }
	@Test void e06_stripSlashes_onlySlashes() { assertEquals("", str.resolve("#{stripSlashes(///)}")); }
	@Test void e07_len_delimiter() { assertEquals("3", str.resolve("#{len(\"a,b,c\", \",\")}")); }
	@Test void e08_replace_noMatch() { assertEquals("hello", str.resolve("#{replace(hello, x, y)}")); }
	@Test void e09_contains_emptySubstr() { assertEquals("true", str.resolve("#{contains(hello, \"\")}")); }
	@Test void e10_startsWith_false() { assertEquals("false", str.resolve("#{startsWith(hello, xyz)}")); }
	@Test void e11_endsWith_false() { assertEquals("false", str.resolve("#{endsWith(hello, xyz)}")); }
	@Test void e12_concat_single() { assertEquals("one", str.resolve("#{concat(one)}")); }
	@Test void e13_repeat_negative() { assertEquals("", str.resolve("#{repeat(abc, -1)}")); }
	@Test void e14_reverse_empty() { assertEquals("", str.resolve("#{reverse(\"\")}")); }
	@Test void e15_split_emptyString() { assertEquals("[]", str.resolve("#{split(\"\", \",\")}")); }
	@Test void e16_split_noDelimiter() { assertEquals("[\"abc\"]", str.resolve("#{split(abc, x)}")); }
	@Test void e17_join_empty() { assertEquals("", str.resolve("#{join(\"/\")}")); }
	@Test void e18_format_float() { assertEquals("3.14", str.resolve("#{format(\"%.2f\", 3.14159)}")); }
	@Test void e19_pathToken_empty() { assertEquals("", str.resolve("#{pathToken(\"\")}")); }
	@Test void e20_stripLeading_noWhitespace() { assertEquals("abc", str.resolve("#{stripLeading(abc)}")); }
	@Test void e21_stripTrailing_noWhitespace() { assertEquals("abc", str.resolve("#{stripTrailing(abc)}")); }

	//------------------------------------------------------------------------------------------------------------------
	// RegexFunctions — edge cases
	//------------------------------------------------------------------------------------------------------------------

	private final VarResolver regex = VarResolver.create().functions(RegexFunctions.ALL).build();

	@Test void f01_match_emptyString() { assertEquals("true", regex.resolve("#{match(\"\", \"\")}")); }
	@Test void f02_extract_noMatch() { assertEquals("", regex.resolve("#{extract(hello, \"\\\\d+\")}")); }
	@Test void f03_extract_invalidGroup() { assertEquals("", regex.resolve("#{extract(hello123, \"(\\\\d+)\", 5)}")); }
	@Test void f04_replaceRegex_noMatch() { assertEquals("hello", regex.resolve("#{replaceRegex(hello, \"\\\\d+\", X)}")); }
	@Test void f05_replaceRegex_all() { assertEquals("XXX", regex.resolve("#{replaceRegex(abc, \".\", X)}")); }
	@Test void f06_match_fullMatch() { assertEquals("true", regex.resolve("#{match(12345, \"^\\\\d+$\")}")); }

	//------------------------------------------------------------------------------------------------------------------
	// TypeConversionFunctions — edge cases
	//------------------------------------------------------------------------------------------------------------------

	private final VarResolver tc = VarResolver.create().functions(TypeConversionFunctions.ALL).build();

	@Test void g01_toInt_negative() { assertEquals("-42", tc.resolve("#{toInt(-42)}")); }
	@Test void g02_toLong_negative() { assertEquals("-999999999999", tc.resolve("#{toLong(-999999999999)}")); }
	@Test void g03_toDouble_negative() { assertEquals("-3.14", tc.resolve("#{toDouble(-3.14)}")); }
	@Test void g04_toBool_on() { assertEquals("true", tc.resolve("#{toBool(on)}")); }
	@Test void g05_toBool_off() { assertEquals("false", tc.resolve("#{toBool(off)}")); }
	@Test void g06_toBool_one() { assertEquals("true", tc.resolve("#{toBool(1)}")); }
	@Test void g07_toBool_zero() { assertEquals("false", tc.resolve("#{toBool(0)}")); }
	@Test void g08_toInt_zero() { assertEquals("0", tc.resolve("#{toInt(0)}")); }
	@Test void g09_toDouble_zero() { assertEquals("0.0", tc.resolve("#{toDouble(0.0)}")); }

	//------------------------------------------------------------------------------------------------------------------
	// DateFunctions — edge cases
	//------------------------------------------------------------------------------------------------------------------

	private final VarResolver date = VarResolver.create().functions(DateFunctions.ALL).build();

	@Test void h01_parseDate_empty() { assertEquals("0", date.resolve("#{parseDate(\"\")}")); }
	@Test void h02_formatDate_negativeEpoch() {
		// epoch 0 minus 1ms is valid
		assertEquals("1969-12-31T23:59:59.999Z", date.resolve("#{formatDate(-1)}"));
	}
	@Test void h03_parseDate_localDateTime() {
		var expected = String.valueOf(java.time.LocalDateTime.of(2026, 6, 1, 12, 0, 0)
			.toInstant(java.time.ZoneOffset.UTC).toEpochMilli());
		assertEquals(expected, date.resolve("#{parseDate(\"2026-06-01T12:00:00\")}"));
	}
	@Test void h04_formatDate_zeroEpoch() {
		assertEquals("1970-01-01T00:00:00Z", date.resolve("#{formatDate(0)}"));
	}
	@Test void h05_now_successive_nondecreasing() {
		var a = Long.parseLong(date.resolve("#{now()}"));
		var b = Long.parseLong(date.resolve("#{now()}"));
		assertTrue(b >= a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// JsonFunctions — edge cases
	//------------------------------------------------------------------------------------------------------------------

	private final VarResolver json = VarResolver.create().functions(JsonFunctions.ALL).build();

	private static String arg(String raw) {
		var sb = new StringBuilder("\"");
		for (var c : raw.toCharArray()) {
			if (c == '"' || c == '\\') sb.append('\\');
			sb.append(c);
		}
		sb.append('"');
		return sb.toString();
	}

	@Test void i01_jsonPath_rootObject() {
		var doc = "{\"a\":1}";
		// Path "/" returns the root
		var result = json.resolve("#{jsonPath(" + arg(doc) + ", \"/\")}");
		assertNotNull(result);
	}
	@Test void i02_get_outOfBoundsArray() {
		assertEquals("", json.resolve("#{get(" + arg("[\"a\",\"b\"]") + ", 99)}"));
	}
	@Test void i03_get_negativeIndex() {
		assertEquals("", json.resolve("#{get(" + arg("[\"a\",\"b\"]") + ", -1)}"));
	}
	@Test void i04_keys_primitiveJson() {
		assertEquals("[]", json.resolve("#{keys(" + arg("\"hello\"") + ")}"));
	}
	@Test void i05_values_primitiveJson() {
		assertEquals("[]", json.resolve("#{values(" + arg("\"hello\"") + ")}"));
	}
	@Test void i06_size_null() {
		assertEquals("0", json.resolve("#{size(" + arg("null") + ")}"));
	}
	@Test void i07_jsonPath_deepNested() {
		var doc = "{\"a\":{\"b\":{\"c\":\"deep\"}}}";
		assertEquals("deep", json.resolve("#{jsonPath(" + arg(doc) + ", \"/a/b/c\")}"));
	}
	@Test void i08_get_nonIntKeyOnArray() {
		assertEquals("", json.resolve("#{get(" + arg("[1,2,3]") + ", foo)}"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// RandomFunctions — edge cases
	//------------------------------------------------------------------------------------------------------------------

	private final VarResolver rnd = VarResolver.create().functions(RandomFunctions.ALL).build();

	@Test void j01_randString_zeroLength() { assertEquals("", rnd.resolve("#{randString(0)}")); }
	@Test void j02_randString_negativeLengthThrows() {
		assertThrows(IllegalArgumentException.class, () -> rnd.resolve("#{randString(-1)}"));
	}
	@Test void j03_randChoice_singleOption() { assertEquals("only", rnd.resolve("#{randChoice(only)}")); }
	@Test void j04_randChoice_noOptionsThrows() {
		assertThrows(IllegalArgumentException.class, () -> rnd.resolve("#{randChoice()}"));
	}
	@Test void j05_randInt_minEqualsMax() {
		assertEquals("5", rnd.resolve("#{randInt(5, 5)}"));
	}
	@Test void j06_randLong_minGreaterThanMaxThrows() {
		assertThrows(IllegalArgumentException.class, () -> rnd.resolve("#{randLong(200, 100)}"));
	}
	@Test void j07_randInt_minGreaterThanMaxThrows() {
		assertThrows(IllegalArgumentException.class, () -> rnd.resolve("#{randInt(10, 5)}"));
	}
}
