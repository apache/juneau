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

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Targeted coverage tests for {@link StringUtils}, filling branch gaps not otherwise exercised
 * by {@code StringUtils_Test} (there is already a comprehensive per-method test there - this file
 * focuses on specific branch gaps identified via {@code coverage.py}).
 */
class StringUtils_Coverage_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a: contains(String,String) / endsWith(String,String) / startsWith(String,String) - null second-arg branch
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_contains_stringSubstringNull_returnsFalse() {
		assertFalse(contains("test", (String)null));
	}

	@Test
	void a02_endsWith_stringSuffixNull_returnsFalse() {
		assertFalse(endsWith("test", (String)null));
	}

	@Test
	void a03_startsWith_stringPrefix_bothNullCombosReturnFalse() {
		assertFalse(startsWith(null, "test"));
		assertFalse(startsWith("test", null));
		assertFalse(startsWith(null, (String)null));
		assertTrue(startsWith("test", "te"));
	}

	@Test
	void a04_startsWith_bothNonNull_prefixDoesNotMatch_returnsFalse() {
		// Both args non-null (so the first two "!= null" operands are true), but the actual
		// String.startsWith(prefix) delegate call itself returns false.
		assertFalse(startsWith("test", "xyz"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b: formatNamed(String, Function<String,Object>) - null string / null resolver branches (only ever
	// invoked through formatNamed(String,Map) elsewhere, which never passes a null s or null resolver)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_formatNamedFunction_nullString_returnsNull() {
		assertNull(formatNamed(null, key -> "x"));
	}

	@Test
	void b02_formatNamedFunction_nullResolver_returnsStringUnchanged() {
		assertEquals("Hello {name}", formatNamed("Hello {name}", (java.util.function.Function<String,Object>)null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c: getDuration(String) - multiple-decimal-point and trailing-whitespace-exhausts-string branches
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_getDuration_multipleDecimalPoints_firstDecimalWins() {
		// "1.5.6m": the number scanner stops at the second '.' (hasDecimal already true), so "1.5" is parsed as
		// a unit-less (millisecond) segment - (long)1.5 = 1ms - then the rest (".6m") is parsed as a second
		// segment: 0.6 minutes = 36000ms. Total: 1 + 36000 = 36001.
		assertEquals(36001, getDuration("1.5.6m"));
	}

	@Test
	void c02_getDuration_unrecognizedUnitStartingWithMill_returnsInvalid() {
		// "milliamps" starts with "mill" (excluded from the plain-minutes branch) but isn't ms/millis/milliseconds,
		// so it falls through every unit check and parseUnit() returns -1 (unknown unit).
		assertEquals(-1, getDuration("5milliamps"));
	}

	@Test
	void c03_getDuration_internalWhitespaceBetweenSegments_skipped() {
		// trim() only strips leading/trailing whitespace, so a space BETWEEN two combined-format segments
		// (e.g. "1h 30m") is the only way to exercise the inner per-segment whitespace-skip loop's
		// "isWhitespace(...) == true" branch.
		assertEquals(3600000L + 1800000L, getDuration("1h 30m"));
	}

	@Test
	void c04_getDuration_unitStartingWithMs_butNotExactlyMs_returnsInvalid() {
		// "msec" isn't exactly "ms"/"millis"/"milliseconds" (so the line 8478 equals-chain misses it), but it
		// DOES start with "ms", so the minutes branch's "!unit.startsWith(\"ms\")" operand specifically
		// evaluates false, and every remaining unit check also fails - unknown unit.
		assertEquals(-1, getDuration("5msec"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d: matchNumberPrefix(String,boolean) - not otherwise directly unit-tested in StringUtils_Test (only exercised
	// incidentally through juneau-marshall's INI/HOCON/HJSON tokenizers)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_matchNumberPrefix_nullString_throws() {
		assertThrows(IllegalArgumentException.class, () -> matchNumberPrefix(null, false));
	}

	@Test
	void d02_matchNumberPrefix_leadingSign_consumed() {
		assertEquals(4, matchNumberPrefix("-123abc", false));
	}

	@Test
	void d03_matchNumberPrefix_noLeadingSign_consumed() {
		assertEquals(3, matchNumberPrefix("123abc", false));
	}

	@Test
	void d04_matchNumberPrefix_signOnly_noDigits_returnsInvalid() {
		assertEquals(-1, matchNumberPrefix("-", false));
	}

	@Test
	void d05_matchNumberPrefix_disallowLeadingZeros_singleZeroOnly() {
		// Leading zeros disallowed: "0" is consumed on its own, "123" after it is NOT part of the integer.
		assertEquals(1, matchNumberPrefix("0123", false));
	}

	@Test
	void d06_matchNumberPrefix_allowLeadingZeros_allDigitsConsumed() {
		assertEquals(3, matchNumberPrefix("007", true));
	}

	@Test
	void d07_matchNumberPrefix_allowLeadingZeros_nonDigitFirstChar_returnsInvalid() {
		assertEquals(-1, matchNumberPrefix("abc", true));
	}

	@Test
	void d08_matchNumberPrefix_disallowLeadingZeros_nonDigitFirstChar_returnsInvalid() {
		assertEquals(-1, matchNumberPrefix("abc", false));
	}

	@Test
	void d09_matchNumberPrefix_fraction_noDigitsAfterDot_notConsumed() {
		assertEquals(3, matchNumberPrefix("123.", false));
		assertEquals(3, matchNumberPrefix("123.e5", false));
	}

	@Test
	void d10_matchNumberPrefix_fraction_valid_consumed() {
		assertEquals(6, matchNumberPrefix("123.45", false));
	}

	@Test
	void d11_matchNumberPrefix_exponent_noSign_consumed() {
		assertEquals(3, matchNumberPrefix("1e5", false));
	}

	@Test
	void d12_matchNumberPrefix_exponent_plusSign_consumed() {
		assertEquals(4, matchNumberPrefix("1e+5", false));
	}

	@Test
	void d13_matchNumberPrefix_exponent_minusSign_consumed() {
		assertEquals(4, matchNumberPrefix("1e-5", false));
	}

	@Test
	void d14_matchNumberPrefix_exponent_noDigitsAfterE_notConsumed() {
		assertEquals(1, matchNumberPrefix("1e", false));
		assertEquals(1, matchNumberPrefix("1e+", false));
		assertEquals(1, matchNumberPrefix("1ex", false));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e: naturalCompare(String,String) - leading-zero-run that consumes to end of string (loop exits via the
	// length boundary rather than via a non-'0' character)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_naturalCompare_numberIsAllLeadingZeros_loopExitsAtStringEnd() {
		assertTrue(naturalCompare("v0", "v1") < 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f: metaphone(String) - silent-GH, silent-GN, silent-H (vowel-H-vowel), and TI+[OA] branches, none of which
	// are reachable through the words already exercised by StringUtils_Test's a033_doubleMetaphone-adjacent tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_metaphone_silentGH_beforeVowel() {
		assertEquals("H", metaphone("AGHO"));
	}

	@Test
	void f02_metaphone_silentGN_beforeED() {
		assertEquals("N", metaphone("AGNE"));
	}

	@Test
	void f03_metaphone_silentH_betweenVowels() {
		// The H between two vowels is dropped (silent), leaving an empty encoding; the method then falls back
		// to returning the first character of the uppercased input rather than an empty string.
		assertEquals("A", metaphone("AHA"));
	}

	@Test
	void f04_metaphone_ti_followedByO_encodesAsX() {
		assertEquals("X", metaphone("ATIO"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g: splitQuoted(String) - leading tab character in state S1 (as opposed to a leading space, which is a
	// different branch outcome of the same condition)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_splitQuoted_leadingTab_treatedAsWhitespace() {
		assertArrayEquals(new String[]{"foo"}, splitQuoted("\tfoo"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h: urlEncodePath(Object) - malformed/unpaired surrogate handling
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_urlEncodePath_unpairedHighSurrogateAtEndOfString_doesNotThrow() {
		var result = urlEncodePath("test\uD83C");
		assertNotNull(result);
		assertTrue(result.startsWith("test"));
	}

	@Test
	void h02_urlEncodePath_highSurrogateFollowedByNonLowSurrogate_doesNotThrow() {
		var result = urlEncodePath("test\uD83Cx");
		assertNotNull(result);
		assertTrue(result.startsWith("test"));
		assertTrue(result.endsWith("x"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i: wrap(String,int,String) - trailing empty line (no newline appended after the last, already-empty line)
	// and a broken long word that is the LAST word on its line (leaves currentLine empty at loop end)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_wrap_trailingEmptyLine_noTrailingNewlineAdded() {
		assertEquals("hello", wrap("hello\n", 10, "\n"));
	}

	@Test
	void i02_wrap_brokenWordIsLastWordOnLine_currentLineEmptyAtEnd() {
		assertEquals("hi\navery\nlongw\nordth\natisl\nong", wrap("hi averylongwordthatislong", 5, "\n"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// j: isValidIPv6Address(String) - triple-colon-prefixed IPv4-mapped form (ipv6Part equals "::" exactly,
	// distinct from the already-tested "::ffff"/"::FFFF"/empty forms)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_isValidIPv6Address_tripleColonIpv4Mapped_ipv6PartEqualsDoubleColon() {
		assertTrue(isValidIPv6Address(":::1.2.3.4"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// k: newStringBuilder(Object) / newStringBuilder(Object...) - null-value branch
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k01_newStringBuilder_singleNullValue_rendersLiteralNull() {
		assertEquals("null", newStringBuilder((Object)null).toString());
		assertEquals("foo", newStringBuilder("foo").toString());
	}

	@Test
	void k02_newStringBuilder_varargsWithNullValue_rendersLiteralNull() {
		assertEquals("foonullbar", newStringBuilder("foo", null, "bar").toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// l: parseIntWithSuffix(String) / parseLongWithSuffix(String) - empty (non-null) string branch, which is
	// distinct from the null-argument branch already tested in StringUtils_Test
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l01_parseIntWithSuffix_emptyString_throwsNumberFormatException() {
		assertThrows(NumberFormatException.class, () -> parseIntWithSuffix(""));
	}

	@Test
	void l02_parseLongWithSuffix_emptyString_throwsNumberFormatException() {
		assertThrows(NumberFormatException.class, () -> parseLongWithSuffix(""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// m: getSoundexCode(char), reached via soundex(String) - the 'A' literal in the vowel/H/W/Y OR-chain is never
	// exercised because every word in StringUtils_Test's soundex tests places 'A' only at index 0, which soundex()
	// always skips (the loop starts at index 1).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m01_soundex_aNotAtFirstPosition_treatedAsVowel() {
		assertEquals("B000", soundex("BA"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// n: camelCase(String) via splitWords(String) - word-boundary heuristic edge cases not hit by the "ABCDe"/
	// "XMLHt" pinning tests already in StringUtils_Test
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void n01_camelCase_twoConsecutiveUppercaseThenLowercase_splitsBeforeLastUppercase() {
		// "ABc": count reaches 2 exactly on 'B', then 'c' (lowercase) triggers the "split all but the last
		// uppercase" branch directly - Case 2 (uppercase-after-uppercase) never gets a chance to fire first
		// because 'B' itself only reaches count==2 (not >=2) at ITS OWN check, one iteration too late.
		assertEquals("aBc", camelCase("ABc"));
	}

	@Test
	void n02_camelCase_uppercaseRunEndsExactlyAtStringEnd_noLookaheadPossible() {
		// "XMLH": by the time we reach the trailing 'H', consecutiveUpperCount is already >=2, but there's no
		// next character to look ahead at, so the split-before-lowercase check is skipped entirely and the
		// whole string stays a single word.
		assertEquals("xMLH", camelCase("XMLH"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// o: matchNumberPrefix(String,boolean) - additional branch gaps: empty (non-null) string, a bare integer with
	// nothing trailing (so the fraction/exponent lookaheads see i==len rather than a non-matching character), and
	// the fraction/exponent digit-range checks failing on a character that isn't a decimal digit (as opposed to
	// failing because the string is exhausted, which the "123." / "1e" cases above already cover).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void o01_matchNumberPrefix_emptyString_returnsInvalid() {
		// Empty (but non-null) string: the leading-sign check's "i < len" operand is false immediately.
		assertEquals(-1, matchNumberPrefix("", false));
	}

	@Test
	void o02_matchNumberPrefix_bareInteger_noTrailingChars_consumedFully() {
		// "123" with nothing after it: the fraction and exponent lookaheads both see i == len (their leading
		// "i < len" operand is false) rather than a non-matching character.
		assertEquals(3, matchNumberPrefix("123", false));
	}

	@Test
	void o03_matchNumberPrefix_fractionDigitRun_stoppedByNonDigitAboveNine() {
		// After consuming ".4", the fraction digit-run's while condition sees 'x' (0x78), which is >= '0' but
		// > '9', so the "charAt <= '9'" operand specifically evaluates false (as opposed to the string simply
		// running out, which the "123.45" case above already covers).
		assertEquals(5, matchNumberPrefix("123.4x", false));
	}

	@Test
	void o04_matchNumberPrefix_fractionDigitRun_stoppedByCharBelowZero() {
		// '!' (0x21) is below '0', so the "charAt >= '0'" operand of the fraction digit-run specifically
		// evaluates false (short-circuiting before the "<= '9'" operand is even reached).
		assertEquals(5, matchNumberPrefix("123.4!", false));
	}

	@Test
	void o05_matchNumberPrefix_exponentDigitCheck_charBelowZero_notConsumed() {
		// '!' (0x21) is below '0', so the exponent's "charAt(j) >= '0'" operand evaluates false and the "e" is
		// not consumed as part of the number.
		assertEquals(1, matchNumberPrefix("1e!", false));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// p: naturalCompare(String,String) - digit-vs-non-digit mismatch at the same position, and str2's
	// leading-zero-skip loop exhausting str2 entirely (exiting via the length boundary rather than via a
	// non-'0' character, mirroring e01's coverage of the same gap for str1).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void p01_naturalCompare_digitVersusNonDigitAtSamePosition_fallsBackToCharCompare() {
		// At index 1: '1' is a digit but 'a' is not, so the "both digits" check's second operand is false and
		// the comparison falls back to a plain case-insensitive character compare.
		assertTrue(naturalCompare("v1", "va") < 0);
	}

	@Test
	void p02_naturalCompare_secondNumberIsAllLeadingZeros_loopExitsAtStringEnd() {
		assertTrue(naturalCompare("v1", "v00") > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// q: metaphone(String) - additional silent-GN (next2=='D') and TI+A branches not covered by f02/f04 above
	// (which only exercised the next2=='E' and next2=='O' arms of their respective two-way OR checks).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void q01_metaphone_silentGN_beforeD() {
		assertEquals("NT", metaphone("AGND"));
	}

	@Test
	void q02_metaphone_ti_followedByA_encodesAsX() {
		assertEquals("X", metaphone("ATIA"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// r: splitQuoted(String) - leading space in state S1, the sibling branch outcome to g01's leading tab (both
	// characters take the same "is whitespace" path, but the OR-chain's first operand ('!=\' \'\'') is only
	// exercised false by a literal space, not by a tab).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void r01_splitQuoted_leadingSpace_treatedAsWhitespace() {
		assertArrayEquals(new String[]{"foo"}, splitQuoted(" foo"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// s: urlEncodePath(Object) - a properly paired surrogate pair (valid astral-plane character), the sibling
	// branch outcome to h01/h02's malformed-surrogate cases.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void s01_urlEncodePath_validSurrogatePair_roundTripsCorrectly() {
		var input = "test\uD83D\uDE00end";
		var encoded = urlEncodePath(input);
		var decoded = java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
		assertEquals(input, decoded);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// t: isValidIPv6Address(String) - the ipv6Part-equals-single-colon-with-double-colon-prefix arm of the
	// IPv4-mapped OR-chain, distinct from j01's ipv6Part-equals-"::" arm.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void t01_isValidIPv6Address_doubleColonIpv4Mapped_ipv6PartEqualsSingleColon() {
		assertTrue(isValidIPv6Address("::1.2.3.4"));
	}
}
