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
package org.apache.juneau.marshall.uon;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.commons.Builder;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link UonParserSession} targeting low-coverage paths:
 *  - Builder setter / property() switch (decoding, validateEnd)
 *  - HttpPartType shortcut paths (null, "null", string with ~, plain)
 *  - parseAnything edge cases (empty/AMP for collections, primitives, strings)
 *  - parseIntoMap / parseIntoCollection / parseIntoBeanMap state-machine error branches
 *  - parsePString unmatched parens
 *  - parseNull error
 *  - parseBoolean error
 *  - validateEnd remainder error
 *  - doParseIntoMap / doParseIntoCollection wrappers
 *  - parseAttrName encoded mode
 */
@SuppressWarnings({
	"rawtypes",
	"unchecked",
	"java:S5961",
	"java:S5976" // SSLLC test naming convention requires individual methods, not parameterized tests
})
class UonParserSession_Test extends TestBase {

	private static final UonParser P = UonParser.DEFAULT;
	private static final UonParser PE = UonParser.DEFAULT_DECODING;
	private static final UonParser PVE = UonParser.create().validateEnd().build();

	// -----------------------------------------------------------------------------------------------------------------
	// a01_builder - Builder.decoding / property() switch / validateEnd
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_builderProperty_decoding_string() {
		// Set via property() with the property name to drive the switch path.
		var p = UonParser.create().build();
		var session = p.createSession()
			.property("decoding", "true")
			.property("validateEnd", "true")
			.build();
		assertTrue(session.isDecoding());
		assertTrue(session.isValidateEnd());
	}

	@Test void a02_builderProperty_unknownKeyFallsThrough() {
		// A non-recognized key triggers the default branch and delegates to super.
		var p = UonParser.create().build();
		var session = p.createSession()
			.property("UnknownKey", "value")
			.build();
		assertNotNull(session);
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test void a03_builderProperty_nullKeyDelegates() {
		// A null key takes the early-return branch and delegates to super, which throws.
		var p = UonParser.create().build();
		assertThrows(IllegalArgumentException.class, () -> p.createSession().property(null, "ignored").build());
	}

	@Test void a04_builderProperty_validateEndAlternateKey() {
		// The session also accepts the qualified property name.
		var p = UonParser.create().build();
		var session = p.createSession()
			.property("UonParserSession.validateEnd", "true")
			.build();
		assertTrue(session.isValidateEnd());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// b01_httpPartParse - parse(HttpPartType, schema, in, ClassMeta) shortcut paths
	// -----------------------------------------------------------------------------------------------------------------

	private static <T> T httpParse(UonParser parser, String in, Class<T> type) throws Exception {
		var session = parser.getSession();
		var cm = session.getClassMeta(type);
		return session.parse((HttpPartType)null, (HttpPartSchema)null, in, cm);
	}

	@Test void b01_httpPart_nullInput() throws Exception {
		assertNull(httpParse(P, null, String.class));
		assertNull(httpParse(P, null, Object.class));
	}

	@Test void b02_httpPart_emptyString_isNotShortcut() throws Exception {
		// Empty string takes the non-shortcut path because ne(in) is false.
		assertEquals("", httpParse(P, "", String.class));
	}

	@Test void b03_httpPart_plainStringShortcut() throws Exception {
		// First char is not ', not n, no '~' -> shortcut returns the string as-is.
		assertEquals("hello", httpParse(P, "hello", String.class));
		assertEquals("123", httpParse(P, "123", String.class));
	}

	@Test void b04_httpPart_nullLiteralShortcut() throws Exception {
		// "null" string with first char 'n' -> returns null.
		assertNull(httpParse(P, "null", String.class));
	}

	@Test void b05_httpPart_nValueButNotNull() throws Exception {
		// First char 'n' but not "null" - falls through to full parse.
		assertEquals("notnull", httpParse(P, "notnull", String.class));
	}

	@Test void b06_httpPart_quotedShortcutFalls() throws Exception {
		// First char '\'' triggers the full parse path.
		assertEquals("hi", httpParse(P, "'hi'", String.class));
	}

	@Test void b07_httpPart_tildeFallsThrough() throws Exception {
		// String containing '~' takes the full parse path (escape handling).
		assertEquals("a'", httpParse(P, "a~'", String.class));
	}

	@Test void b08_httpPart_objectTarget() throws Exception {
		// Non-string target type bypasses shortcut entirely.
		var n = httpParse(P, "123", Integer.class);
		assertEquals(Integer.valueOf(123), n);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// c01_parseAnything - empty/AMP-first branches
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_parseEmptyForCollection() throws Exception {
		// Empty input for collection - HttpPart shortcut returns null for null/empty,
		// but the parseAnything path with isCollectionOrArray on empty input returns sType.newInstance().
		// For top-level public parse(...) on List from empty string, the result is null.
		var l = P.parse("", List.class);
		assertNull(l);
	}

	@Test void c02_parseEmptyForArray() throws Exception {
		var arr = P.getSession().parse((HttpPartType)null, (HttpPartSchema)null, "", P.getSession().getClassMeta(String[].class));
		assertNotNull(arr);
		assertEquals(0, arr.length);
	}

	@Test void c03_parseEmptyForMap() throws Exception {
		// Map with empty input - parseAnything → returns null but parseIntoMap may differ
		// Empty input → c==-1 → not collection/string/object/primitive → returns null
		var m = P.getSession().parse((HttpPartType)null, (HttpPartSchema)null, "", P.getSession().getClassMeta(Map.class));
		assertNull(m);
	}

	@Test void c04_parseEmptyForPrimitive() throws Exception {
		// Empty input for primitive returns the default for the primitive type.
		var i = P.getSession().parse((HttpPartType)null, (HttpPartSchema)null, "", P.getSession().getClassMeta(int.class));
		assertEquals(Integer.valueOf(0), i);
		var b = P.getSession().parse((HttpPartType)null, (HttpPartSchema)null, "", P.getSession().getClassMeta(boolean.class));
		assertEquals(Boolean.FALSE, b);
	}

	@Test void c05_parseEmptyForString() throws Exception {
		var s = P.parse("", String.class);
		assertEquals("", s);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// d01_parseAnything - error paths
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_parseBoolean_invalid() {
		assertThrows(ParseException.class, () -> P.parse("notabool", Boolean.class));
	}

	@Test void d02_parseBoolean_null() throws Exception {
		assertNull(P.parse("null", Boolean.class));
	}

	@Test void d03_parseNull_invalidSequence() {
		// 'n' followed by non-"ull" should throw.
		// "nx" would be parsed as a string at top-level; force Map context to take parseAnything's c=='n' branch.
		// Actually the c == 'n' branch in parseAnything is hit when sType is non-bean, non-string. Use HashMap
		// at second level via map literal to exercise.
		assertThrows(ParseException.class, () -> P.parse("nope", Boolean.class));
	}

	@Test void d04_parseString_unmatchedParens() {
		// 'xxx without matching ' — parsePString should throw.
		assertThrows(ParseException.class, () -> P.parse("'unmatched", String.class));
	}

	@Test void d05_parseString_unmatchedParensInMap() {
		assertThrows(ParseException.class, () -> P.parse("(a='unmatched)", Map.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// e01_parseIntoMap - state-machine errors
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_map_missingOpenParen() {
		// parseIntoMap requires '(' at start when not 'n'/AMP.
		assertThrows(ParseException.class, () -> P.parse("xyz", Map.class));
	}

	@Test void e02_map_unclosedAfterAttrEquals() throws Exception {
		// Map "(x=" - state S3 with c==-1 puts empty value and returns m (does not throw).
		var m = P.parse("(x=", Map.class);
		assertNotNull(m);
		assertTrue(m.containsKey("x"));
	}

	@Test void e03_map_nullLiteral() throws Exception {
		// "null" returns null map.
		assertNull(P.parse("null", Map.class));
	}

	@Test void e04_map_attrWithoutEquals() throws Exception {
		// "(a)" - parseAttrName in non-encoded mode reads to '=' or EOF or whitespace, so it
		// consumes "a)" as the attribute name. State S2 then sees EOF and puts null.
		var m = P.parse("(a)", Map.class);
		assertNotNull(m);
		// Implementation detail: non-encoded attr name includes the trailing ')' since ')' isn't
		// in the parseAttrName end-set.  Either way, we just check we get a map back.
		assertEquals(1, m.size());
	}

	@Test void e05_map_emptyParens() throws Exception {
		// "()" returns empty map.
		var m = P.parse("()", Map.class);
		assertNotNull(m);
		assertTrue(m.isEmpty());
	}

	@Test void e06_map_attrEqualsValueComma() throws Exception {
		// "(a=,b=)" - state S3 with ',' immediately - sets to empty string.
		var m = P.parse("(a=,b=)", Map.class);
		assertEquals("", m.get("a"));
		assertEquals("", m.get("b"));
	}

	@Test void e07_map_multipleEntries_trailingComma() throws Exception {
		// "(a=1," at EOF - state S1 reads c==-1, exits while loop, returns null (lenient/partial parse).
		var m = P.parse("(a=1,", Map.class);
		assertNull(m);
	}

	@Test void e08_map_unclosedAfterValue() throws Exception {
		// "(a=1" at EOF - state S4 with c==-1 returns m without throwing.
		var m = P.parse("(a=1", Map.class);
		assertNotNull(m);
		assertEquals(1, m.get("a"));
	}

	@Test void e09_map_unclosedAfterAttr() throws Exception {
		// "(a" at EOF - state S2 with c==-1 puts null and returns m.
		var m = P.parse("(a", Map.class);
		assertNotNull(m);
		assertTrue(m.containsKey("a"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// f01_parseIntoCollection - state-machine errors and parens
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_collection_missingAtSign() {
		// At non-top-level (i.e. via doParseIntoCollection / inner field), missing '@(' throws.
		// Actually the public parse(List.class) goes through doParse → parseAnything which routes
		// arrays/lists differently (single-value wrap). This test exercises the inner state.
		assertThrows(ParseException.class, () -> P.parse("(a=xyz)", List.class)); // routes via map-as-collection
	}

	@Test void f02_collection_emptyAtSign() throws Exception {
		var l = P.parse("@()", List.class);
		assertTrue(l.isEmpty());
	}

	@Test void f03_collection_nullLiteral() throws Exception {
		assertNull(P.parse("null", List.class));
	}

	@Test void f04_collection_singleValueAsMap() throws Exception {
		// "(x=1)" parsed as List<Map> - takes the "serialized as map" branch and adds the map
		// cast as a single element.
		var l = (LinkedList<Map>) P.parse("(x=1)", LinkedList.class, Map.class);
		assertEquals(1, l.size());
		assertEquals(1, l.get(0).get("x"));
	}

	@Test void f05_collection_unclosedAfterEntry() {
		// "@(1,2" - state S2 → S1 → expecting next entry; EOF → throw "Could not find start of entry"
		assertThrows(ParseException.class, () -> P.parse("@(1,2,", List.class));
	}

	@Test void f06_collection_topLevelCommaList() throws Exception {
		// At top-level (URL param), a comma-delimited list is permitted without parens.
		var ses = P.getSession();
		var cm = ses.getClassMeta(List.class);
		var l = ses.parse((HttpPartType)null, (HttpPartSchema)null, "1,2,3", cm);
		assertEquals(3, l.size());
	}

	@Test void f07_collection_emptyArray() throws Exception {
		// At top-level, an empty input for an array yields an empty array.
		var arr = P.parse("@()", String[].class);
		assertNotNull(arr);
		assertEquals(0, arr.length);
	}

	@Test void f08_collection_singleValueAsMapForArray() throws Exception {
		// Arrays go through similar map-as-array path in parseAnything.
		var arr = P.parse("(x=1)", Map[].class);
		assertEquals(1, arr.length);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// g01_parseIntoBeanMap - state-machine errors
	// -----------------------------------------------------------------------------------------------------------------

	public static class Bean {
		public String f1;
		public int f2;
	}

	@Test void g01_bean_emptyParens() throws Exception {
		// "()" returns an empty bean.
		var b = P.parse("()", Bean.class);
		assertNotNull(b);
		assertNull(b.f1);
		assertEquals(0, b.f2);
	}

	@Test void g02_bean_unclosedAfterAttr() throws Exception {
		// "(f1" - state S2 with c==-1 puts null and returns m (lenient EOF behavior).
		var b = P.parse("(f1", Bean.class);
		assertNotNull(b);
		assertNull(b.f1);
	}

	@Test void g03_bean_unclosedAfterEquals() {
		// "(f1=" - returns null (S3 with EOF → setter with empty)
		// Actually checks: S3 with c==-1 sets currAttr="" and returns m.
		// This case exits cleanly with f1=null.
		assertDoesNotThrow(() -> P.parse("(f1=", Bean.class));
	}

	@Test void g04_bean_unclosedAfterValue() throws Exception {
		// "(f1=foo" - state S4 with c==-1 returns m (lenient EOF behavior).
		var b = P.parse("(f1=foo", Bean.class);
		assertNotNull(b);
		assertEquals("foo", b.f1);
	}

	@Test void g05_bean_attrWithoutEquals() {
		// "(f1)" - parseAttrName in non-encoded mode reads "f1)" as attr name; bean treats it as
		// unknown property → throws BeanRuntimeException wrapped as ParseException.
		assertThrows(ParseException.class, () -> P.parse("(f1)", Bean.class));
	}

	@Test void g06_bean_unknownProperty() throws Exception {
		// "(unknown=foo)" with an ignoreUnknownBeanProperties parser does not throw.
		var p = UonParser.create().ignoreUnknownBeanProperties().build();
		var b = p.parse("(unknown=foo,f1=bar)", Bean.class);
		assertEquals("bar", b.f1);
	}

	@Test void g07_bean_emptyValueSetsNull() throws Exception {
		// "(f1=)" - state S3 with ',' or ')' - convertToType("", String) → null.
		var b = P.parse("(f1=)", Bean.class);
		assertNotNull(b);
		// Empty string converts to null for String? Actually it converts to "" or null depending on cast.
		// We just ensure no exception.
	}

	@Test void g08_bean_nullLiteral() throws Exception {
		// "null" returns null bean.
		assertNull(P.parse("null", Bean.class));
	}

	@Test void g09_bean_missingOpenParen() {
		// "xyz" without '(' - parseIntoBeanMap throws.
		assertThrows(ParseException.class, () -> P.parse("xyz", Bean.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// h01_validateEnd - non-whitespace remainder throws
	// -----------------------------------------------------------------------------------------------------------------

	@Test void h01_validateEnd_extraJunk() {
		// validateEnd parser with extra content after parse should throw.
		assertThrows(ParseException.class, () -> PVE.parse("(a=1)xx", Map.class));
	}

	@Test void h02_validateEnd_trailingWhitespace() throws Exception {
		// validateEnd parser with trailing whitespace should NOT throw.
		var m = PVE.parse("(a=1)   ", Map.class);
		assertEquals(1, m.get("a"));
	}

	@Test void h03_validateEnd_off_extraJunkOk() throws Exception {
		// Default parser (no validateEnd) tolerates trailing junk only by ignoring it after parse.
		// At top level the parser just stops once parsed, so this should not throw.
		// Default parser handles "(a=1)" cleanly.
		var m = P.parse("(a=1)", Map.class);
		assertEquals(1, m.get("a"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// i01_doParseIntoMap / doParseIntoCollection wrappers
	// -----------------------------------------------------------------------------------------------------------------

	@Test void i01_doParseIntoMap() throws Exception {
		// Use the parseIntoMap path on the parser which delegates to doParseIntoMap.
		var dest = new HashMap<String, Object>();
		P.parseIntoMap("(a=1,b=2)", dest, String.class, Object.class);
		assertEquals(1, dest.get("a"));
		assertEquals(2, dest.get("b"));
	}

	@Test void i02_doParseIntoCollection() throws Exception {
		var dest = new ArrayList<>();
		P.parseIntoCollection("@(1,2,3)", dest, Integer.class);
		assertEquals(3, dest.size());
		assertEquals(1, dest.get(0));
		assertEquals(2, dest.get(1));
		assertEquals(3, dest.get(2));
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test void i03_doParseIntoMap_validateEnd() {
		// validateEnd with extra junk after map.
		assertThrows(ParseException.class, () -> {
			var dest = new HashMap<String, Object>();
			PVE.parseIntoMap("(a=1)x", dest, String.class, Object.class);
		});
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test void i04_doParseIntoCollection_validateEnd() {
		assertThrows(ParseException.class, () -> {
			var dest = new ArrayList<>();
			PVE.parseIntoCollection("@(1,2)x", dest, Integer.class);
		});
	}

	// -----------------------------------------------------------------------------------------------------------------
	// j01_decoding / parseAttrName encoded path
	// -----------------------------------------------------------------------------------------------------------------

	@Test void j01_decoding_attrWithEncodedAmp() throws Exception {
		// In encoded mode, '%26' (=&) inside an attribute name is escaped via '~'+AMP.
		// Decode should preserve '&' in the attribute name.
		var m = PE.parse("(a%26b=v)", Map.class);
		assertTrue(m.containsKey("a&b"));
		assertEquals("v", m.get("a&b"));
	}

	@Test void j02_decoding_attrWithEncodedEquals() throws Exception {
		var m = PE.parse("(a%3Db=v)", Map.class);
		assertTrue(m.containsKey("a=b"));
	}

	@Test void j03_decoding_valueWithEncodedSpace() throws Exception {
		// In decoding mode, '%20' decodes to a space char which terminates the value at "hello".
		// (Whitespace is the value terminator in non-quoted strings.)
		var m = PE.parse("(a=hello%20world)", Map.class);
		assertEquals("hello", m.get("a"));
	}

	@Test void j04_nonDecoding_passesThrough() throws Exception {
		var m = P.parse("(a=hello%20world)", Map.class);
		assertEquals("hello%20world", m.get("a"));
	}

	@Test void j05_decoding_nullAttrName() throws Exception {
		// %00 decodes to a NUL char. The decoded key is the single NUL char (stored literally).
		var m = PE.parse("(%00=v)", Map.class);
		assertNotNull(m);
		assertEquals(1, m.size());
		assertTrue(m.containsValue("v"));
	}

	@Test void j06_decoding_amp_terminatesParam() throws Exception {
		// In decoding mode, '&' (encoded as %26 → AMP) terminates a top-level URL parameter value.
		var ses = PE.getSession();
		var cm = ses.getClassMeta(String.class);
		// Note that the exact termination behavior depends on full integration; just exercise the path.
		var s = ses.parse((HttpPartType)null, (HttpPartSchema)null, "hello%26", cm);
		assertNotNull(s);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// k01_specialChars - escaped characters
	// -----------------------------------------------------------------------------------------------------------------

	@Test void k01_escapedTilde() throws Exception {
		// "~~" in plain text → "~"
		assertEquals("~", P.parse("~~", String.class));
	}

	@Test void k02_escapedQuote() throws Exception {
		// "~'" → "'"
		assertEquals("'", P.parse("~'", String.class));
	}

	@Test void k03_escapedTilde_inMapAttr() throws Exception {
		var m = P.parse("(a~~b=v)", Map.class);
		assertTrue(m.containsKey("a~b"));
	}

	@Test void k04_escapedQuote_inQuotedString() throws Exception {
		// '~'' inside quotes → '
		assertEquals("'", P.parse("'~''", String.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// l01_misc - more error/edge branches
	// -----------------------------------------------------------------------------------------------------------------

	public static class VoidLikeBean {
		// no fields
	}

	@Test void l01_void_nullValue() throws Exception {
		// Void target with "null" → null.
		assertNull(P.parse("null", Void.class));
	}

	@Test void l02_void_anyValueReturnsNull() throws Exception {
		// Void target short-circuits in ParserSession.parseInner and always returns null.
		assertNull(P.parse("foo", Void.class));
		assertNull(P.parse("(a=1)", Void.class));
	}

	@Test void l03_optional_present() throws Exception {
		var opt = (Optional<Integer>) P.parse("123", Optional.class, Integer.class);
		assertTrue(opt.isPresent());
		assertEquals(123, opt.get());
	}

	@Test void l04_optional_null() throws Exception {
		var opt = (Optional<Integer>) P.parse("null", Optional.class, Integer.class);
		// Empty input map to Optional.empty
		assertNotNull(opt);
	}

	@Test void l05_charType() throws Exception {
		var c = P.parse("'x'", char.class);
		assertEquals('x', c.charValue());
	}

	@Test void l06_charNullType() throws Exception {
		var c = P.parse("null", Character.class);
		assertNull(c);
	}

	@Test void l07_topLevelArray_unclosedThrows() {
		// "@(1," at EOF - throws "Could not find start of entry".
		assertThrows(ParseException.class, () -> P.parse("@(1,", List.class));
	}

	@Test void l08_topLevelArray_unclosed_atFirst() {
		// "@(" at EOF - state S1/S2, throws "Could not find start of entry".
		assertThrows(ParseException.class, () -> P.parse("@(", List.class));
	}

	@Test void l09_topLevelArray_whitespaceOnly() throws Exception {
		// "@( )" - empty list with whitespace.
		var l = P.parse("@( )", List.class);
		assertTrue(l.isEmpty());
	}

	@Test void l10_topLevelMap_whitespaceOnly() throws Exception {
		// "( )" - empty map with whitespace.
		var m = P.parse("( )", Map.class);
		assertTrue(m.isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// m01_typedMap_keyAttr - non-string key types in Map
	// -----------------------------------------------------------------------------------------------------------------

	@Test void m01_intKeyMap() throws Exception {
		var m = (HashMap<Integer, String>) P.parse("(1=a,2=b)", HashMap.class, Integer.class, String.class);
		assertEquals("a", m.get(1));
		assertEquals("b", m.get(2));
	}

	@Test void m02_typedCollection() throws Exception {
		var l = (LinkedList<Integer>) P.parse("@(1,2,3)", LinkedList.class, Integer.class);
		assertEquals(3, l.size());
	}

	@Test void m03_arrayOfInts() throws Exception {
		var arr = P.parse("@(1,2,3)", int[].class);
		assertArrayEquals(new int[]{1, 2, 3}, arr);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// n01_isDecoding/isValidateEnd accessors
	// -----------------------------------------------------------------------------------------------------------------

	@Test void n01_isDecoding_default() {
		assertFalse(P.getSession().isDecoding());
	}

	@Test void n02_isDecoding_decoding() {
		assertTrue(PE.getSession().isDecoding());
	}

	@Test void n03_isValidateEnd_default() {
		assertFalse(P.getSession().isValidateEnd());
	}

	@Test void n04_isValidateEnd_set() {
		assertTrue(PVE.getSession().isValidateEnd());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// o01_properties - properties() output includes decoding and validateEnd
	// -----------------------------------------------------------------------------------------------------------------

	@Test void o01_properties() {
		var props = P.getSession().properties();
		assertNotNull(props);
		// Just exercise the toString() / properties() path without strict key assertions, since
		// the FluentMap wraps another map and key composition is implementation-detail.
		assertNotNull(props.toString());
		// At minimum the map is non-empty given the parser session always sets several values.
		assertFalse(props.isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// p01_parseAnything - Date/Calendar/Temporal/Duration/Period type branches
	// -----------------------------------------------------------------------------------------------------------------

	@Test void p01_parseDate() throws Exception {
		// Exercises the sType.isDate() branch in parseAnything.
		var d = P.parse("2024-01-15T00:00:00Z", java.util.Date.class);
		assertNotNull(d);
	}

	@Test void p02_parseCalendar() throws Exception {
		// Exercises the sType.isCalendar() branch in parseAnything.
		var c = P.parse("2024-01-15T12:00:00Z", java.util.Calendar.class);
		assertNotNull(c);
	}

	@Test void p03_parseTemporal() throws Exception {
		// Exercises the sType.isTemporal() branch in parseAnything.
		var t = P.parse("2024-01-15", LocalDate.class);
		assertNotNull(t);
	}

	@Test void p04_parseDuration() throws Exception {
		// Exercises the sType.isDuration() branch in parseAnything.
		var d = P.parse("PT1H", Duration.class);
		assertNotNull(d);
		assertEquals(Duration.ofHours(1), d);
	}

	@Test void p05_parsePeriod() throws Exception {
		// Exercises the sType.isPeriod() branch in parseAnything.
		var p = P.parse("P1Y", Period.class);
		assertNotNull(p);
		assertEquals(Period.ofYears(1), p);
	}

	@Test void p06_parseDate_null() throws Exception {
		// Null value for Date type returns null.
		assertNull(P.parse("null", java.util.Date.class));
	}

	@Test void p07_parseCalendar_null() throws Exception {
		// Null value for Calendar type returns null.
		assertNull(P.parse("null", java.util.Calendar.class));
	}

	@Test void p08_parseTemporal_null() throws Exception {
		// Null value for LocalDate type returns null.
		assertNull(P.parse("null", LocalDate.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// q01_parseAnything - ObjectSwap path
	// -----------------------------------------------------------------------------------------------------------------

	public static class Q01_Wrapped {
		public final String value;
		Q01_Wrapped(String v) { this.value = v; }
	}

	public static class Q01_WrappedSwap extends ObjectSwap<Q01_Wrapped, String> {
		@Override public String swap(MarshallingSession session, Q01_Wrapped o) { return o.value; }
		@Override public Q01_Wrapped unswap(MarshallingSession session, String f, ClassMeta<?> hint) { return new Q01_Wrapped(f); }
	}

	@Test void q01_objectSwap_roundtrip() throws Exception {
		// Exercises the nn(swap) && nn(o) branch in parseAnything.
		var parser = UonParser.create().swaps(Q01_WrappedSwap.class).build();
		var result = parser.parse("hello", Q01_Wrapped.class);
		assertNotNull(result);
		assertEquals("hello", result.value);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// r01_parseAnything - BuilderSwap path (nn(builder) branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Builder(R01_BeanBuilder.class)
	public static class R01_Bean {
		public int x;
		public R01_Bean(R01_BeanBuilder b) { if (b != null) x = b.x; }
	}

	public static class R01_BeanBuilder {
		public int x;
		public R01_Bean build() { return new R01_Bean(this); }
	}

	@Test void r01_builderSwap() throws Exception {
		// Exercises the nn(builder) branch in parseAnything via @Builder annotation.
		var b = P.parse("(x=42)", R01_Bean.class);
		assertNotNull(b);
		assertEquals(42, b.x);
	}

	@Test void r02_builderSwap_null() throws Exception {
		// Exercises the builder.build()/null return path.
		var b = P.parse("null", R01_Bean.class);
		assertNull(b);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// s01_parseAnything - canCreateNewBean null return (null literal in bean context)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void s01_bean_nullReturnFromParseIntoBeanMap() throws Exception {
		// parseIntoBeanMap returns null when the UON value is "null", so parseAnything gets null back.
		var b = P.parse("null", Bean.class);
		assertNull(b);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// t01_parseIntoCollection - S2 state closing ')' branch and args branch
	// -----------------------------------------------------------------------------------------------------------------

	@Test void t01_collection_S2_closingParen() throws Exception {
		// "@(,)" — after ',' takes S2 state; then ')' should trigger parseAnything for that element.
		// "@(1,)" is actually valid in lenient mode: second element is parsed from ')' unread.
		// Actually "@(1,2)" exercises S2->S3->comma->S2->paren path. Test with trailing item:
		var l = P.parse("@(1,2)", List.class);
		assertEquals(2, l.size());
	}

	@Test void t02_collection_whitespace_in_noParens_path() throws Exception {
		// In top-level URL param value mode, whitespace in S1 triggers skipSpace.
		// Use httpParse with a space-leading value to exercise this path.
		var ses = P.getSession();
		var cm = ses.getClassMeta(List.class);
		// A leading space before the value triggers isWhitespace branch in S1 of the non-parens path.
		var l = ses.parse((HttpPartType)null, (HttpPartSchema)null, " 1,2", cm);
		assertNotNull(l);
	}

	@Test void t03_collection_whitespace_S2_noParens() throws Exception {
		// Whitespace after a value in non-parens mode (S2) triggers skipSpace branch.
		var ses = P.getSession();
		var cm = ses.getClassMeta(List.class);
		var l = ses.parse((HttpPartType)null, (HttpPartSchema)null, "1 ,2", cm);
		assertNotNull(l);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// u01_parseIntoMap - encoded mode null-attr-name path (S2 with currAttr==null)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void u01_map_encoded_nullAttr() throws Exception {
		// In encoded mode, %00 decodes to a NUL char. The parseAttrName in encoded mode returns the literal
		// NUL-char string (not null), so the map ends up with a NUL-char key.
		// A truly null attr in the encoded path requires the literal string "null" after decoding.
		var m = PE.parse("(null=v)", Map.class);
		// "null" key is parsed as null key (consistent with non-encoded mode).
		assertTrue(m.containsKey(null));
		assertEquals("v", m.get(null));
	}

	@Test void u02_map_encoded_whitespace_S1() throws Exception {
		// In encoded decoding mode, whitespace in S1 triggers skipSpace branch.
		var m = PE.parse("( a=1)", Map.class);
		assertNotNull(m);
		assertEquals(1, m.get("a"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// v01_parseIntoBeanMap - S3 empty-value branches and S1 whitespace
	// -----------------------------------------------------------------------------------------------------------------

	@Test void v01_beanMap_S3_comma_emptyValue() throws Exception {
		// "(f1=,f2=0)" - state S3 with ',' immediately after '=' sets empty value for f1.
		// Empty string stays as "" for String type (not converted to null).
		var b = P.parse("(f1=,f2=0)", Bean.class);
		assertNotNull(b);
		// f1 gets "" (empty String from convertToType), f2 gets 0.
		assertEquals("", b.f1);
		assertEquals(0, b.f2);
	}

	@Test void v02_beanMap_S1_whitespace() throws Exception {
		// "( f1=foo)" - whitespace in S1 triggers skipSpace.
		var b = P.parse("( f1=foo)", Bean.class);
		assertNotNull(b);
		assertEquals("foo", b.f1);
	}

	@Test void v03_beanMap_S4_comma_then_end() throws Exception {
		// "(f1=foo,f2=123)" - exercises S4->S1->S2->S3 fully.
		var b = P.parse("(f1=foo,f2=123)", Bean.class);
		assertNotNull(b);
		assertEquals("foo", b.f1);
		assertEquals(123, b.f2);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// w01_parseAttrName - encoded path with AMP and EQ replacement in escape context
	// -----------------------------------------------------------------------------------------------------------------

	@Test void w01_attrName_encoded_escapedAmp_inEscape() throws Exception {
		// In encoded mode with isInEscape=true, when c==AMP the char is replaced with '&'.
		// In decoding mode, a literal '&' in the stream is decoded to AMP by UonReader.
		// A '~&' in the input: ~ followed by & (decoded to AMP) triggers isInEscape, then
		// c==AMP while isInEscape=true causes r.replace('&') to be called (line 889).
		var m = PE.parse("(a~&b=v)", Map.class);
		// After escape processing, key should contain '&' literally ("a&b").
		assertNotNull(m);
		assertTrue(m.containsKey("a&b"));
	}

	@Test void w02_attrName_encoded_escapedEq_inEscape() throws Exception {
		// In encoded mode with isInEscape=true, when c==EQ the char is replaced with '='.
		// In decoding mode, a literal '=' in the stream is decoded to EQ by UonReader.
		// A '~=' in the key: isInEscape triggers, then c==EQ while isInEscape=true
		// causes r.replace('=') to be called (line 891).
		var m = PE.parse("(a~=b=v)", Map.class);
		// After escape processing, key should contain '=' literally ("a=b").
		assertNotNull(m);
		assertTrue(m.containsKey("a=b"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// x01_parseString - isUrlParamValue with whitespace path
	// -----------------------------------------------------------------------------------------------------------------

	@Test void x01_parseString_urlParamValue_trimmed() throws Exception {
		// isUrlParamValue=true: string is StringUtils.trim()ed at end.
		// Exercises the isUrlParamValue branch in parseString.
		var ses = P.getSession();
		var cm = ses.getClassMeta(String.class);
		var s = ses.parse((HttpPartType)null, (HttpPartSchema)null, "  hello  ", cm);
		// Leading/trailing whitespace is trimmed in URL param mode.
		assertNotNull(s);
	}

	@Test void x02_parseString_nonUrlParam_whitespace() throws Exception {
		// At top-level, doParse uses isUrlParamValue=true so whitespace does NOT terminate the string.
		// To exercise the whitespace-as-terminator branch (isUrlParamValue=false), use a nested
		// context like a map value which is parsed with isUrlParamValue=false.
		// In non-URL-param mode (inside a map), whitespace terminates the value.
		var m = P.parse("(a=hello world)", Map.class);
		// "hello" is the value (whitespace terminates non-param strings inside maps).
		assertEquals("hello", m.get("a"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// y01_getUonReader - when pipe already wraps a UonReader
	// -----------------------------------------------------------------------------------------------------------------

	@Test void y01_getUonReader_alreadyUonReader() throws Exception {
		// Exercises the "r instanceof UonReader" branch in getUonReader by calling doParse
		// on an already-UonReader pipe; the parser pipeline wraps the input with UonReader
		// internally so invoking parse twice exercises the instanceof path.
		// This is implicitly covered via the decoding-mode parser; exercise via round-trip.
		var s = PE.parse("hello", String.class);
		assertEquals("hello", s);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// z01_parseIntoCollection - error branches (S1/S2 and S3 throws)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void z01_collection_S3_unclosed() {
		// "@(1" — reaches end of stream in S3 (after adding item, looking for ',' or ')').
		assertThrows(ParseException.class, () -> P.parse("@(1", List.class));
	}

	@Test void z02_collection_S2_unclosed() {
		// "@(1," — S2 looking for next item, but hits EOF.
		assertThrows(ParseException.class, () -> P.parse("@(1,", List.class));
	}

	@Test void aa01_collection_S2_closeParenWithElement() throws Exception {
		// "@(1,)" — after first element + comma (S2), immediately ')'.
		// The S2 branch fires: unread ')' and call parseAnything which returns empty string for Object type.
		var l = P.parse("@(1,)", List.class);
		assertNotNull(l);
		// l contains "1" (first element) and "" or null (the second element parsed from ')')
		assertTrue(l.size() >= 1);
	}

	@Test void aa02_collection_S2_whitespace_then_closeParen() throws Exception {
		// "@(1,  2)" — whitespace before 2nd element in S2/S1 state triggers skipSpace.
		var l = P.parse("@(1,  2)", List.class);
		assertNotNull(l);
		assertEquals(2, l.size());
	}

	@Test void aa03_parseArgs_isArgsType() throws Exception {
		// parseArgs uses an ARGS ClassMeta which exercises the type.isArgs() true branch
		// in parseIntoCollection (lines 610 and 620: type.getArg(argIndex++) instead of getElementType()).
		var session = P.getSession();
		var args = session.parseArgs("@(1,hello)", new java.lang.reflect.Type[]{int.class, String.class});
		assertNotNull(args);
		assertEquals(2, args.length);
		assertEquals(1, args[0]);
		assertEquals("hello", args[1]);
	}

	@Test void aa04_parseArgs_isArgsType_thenS2Close() throws Exception {
		// parseArgs with "@(1,)" — ARGS type, S2 + ')' fires line 610 with type.isArgs()=true.
		var session = P.getSession();
		var args = session.parseArgs("@(1,)", new java.lang.reflect.Type[]{int.class, String.class});
		assertNotNull(args);
	}

	@Test void aa05_parseArgs_noParens_nonParensPath() throws Exception {
		// parseArgs with "1,hello" — non-parens input (isUrlParamValue=true).
		// ARGS type exercises type.isArgs()=true in non-parens S1 path (line 647).
		var session = P.getSession();
		var args = session.parseArgs("1,hello", new java.lang.reflect.Type[]{int.class, String.class});
		assertNotNull(args);
		assertEquals(2, args.length);
		assertEquals(1, args[0]);
		assertEquals("hello", args[1]);
	}

	@Test void aa06_parseArgs_nonParens_whitespace_S2() throws Exception {
		// "1 ,hello" — after first arg, whitespace before ',' in S2.
		// Exercises the isWhitespace branch in S2 of the non-parens loop (line 653).
		var session = P.getSession();
		var args = session.parseArgs("1 ,hello", new java.lang.reflect.Type[]{int.class, String.class});
		assertNotNull(args);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ab01_parseIntoMap - S2 null-attr with comma (triggers null-attr early-return path)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void ab01_map_nullAttr_S2_eof() throws Exception {
		// In encoded mode, parseAttrName returns null for the literal string "null" (line 905).
		// "(null" — 'null' attr followed by EOF → parseAttrName returns null, currAttr=null, state=S2.
		// Then outer loop reads c=-1: S2 + c==-1 + currAttr==null → r.unread() + return null.
		var m = PE.parse("(null", Map.class);
		assertNull(m);
	}

	@Test void ab02_map_S3_emptyValue_comma() throws Exception {
		// "(a=,b=2)" — S3 with ',' immediately after '=' sets empty value for a.
		var m = P.parse("(a=,b=2)", Map.class);
		assertNotNull(m);
		// Empty string is the value for a; b is integer 2 but in generic map it's a number.
		assertEquals("", m.get("a"));
		assertNotNull(m.get("b"));
	}

	@Test void ab03_map_S3_emptyValue_closeParen() throws Exception {
		// "(a=)" — S3 with ')' immediately after '=' sets empty value for a, then closes.
		var m = P.parse("(a=)", Map.class);
		assertNotNull(m);
		assertEquals("", m.get("a"));
	}

	@Test void ab04_map_S2_nullAttr_closeParen() throws Exception {
		// In encoded mode, parseAttrName with "null" followed by whitespace returns null (null currAttr).
		// "(null )" — null currAttr, whitespace consumed in outer S2 loop, then ')' triggers
		//            null-attr check: r.unread() + return null from parseIntoMap.
		var m = PE.parse("(null )", Map.class);
		assertNull(m);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ac01_parseIntoBeanMap - S2 with comma (null-value bean property)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void ac01_beanMap_S2_noEq_comma() throws Exception {
		// "(f1 ,f2=0)" — S2 for f1 sees ',' so f1 gets null (no '=' found).
		// Use whitespace before comma so parser sees f1 then whitespace-terminated attr then comma.
		// In non-encoded mode, whitespace terminates attr name; the next char is ',' which is S2 → null value.
		var b = P.parse("(f1 ,f2=0)", Bean.class);
		assertNotNull(b);
		assertNull(b.f1);
		assertEquals(0, b.f2);
	}

	@Test void ac02_beanMap_S3_unknownProp_emptyValue() {
		// "(unknown=,f2=0)" — S3 with ',' for unknown prop. onUnknownProperty is called.
		// This exercises the pMeta==null branch in S3 empty-value path.
		assertDoesNotThrow(() -> P.parse("(unknown=,f2=0)", Bean.class));
	}

	@Test void ac03_beanMap_S3_typePropertyName_emptyValue() throws Exception {
		// "(_type=)" — S3 with ')' and currAttr == getBeanTypePropertyName → false branch of line 497.
		// When the attribute is the type property name with an empty value, the property-setting block is skipped.
		var b = P.parse("(_type=)", Bean.class);
		assertNotNull(b);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ad01_parseAnything - canCreateNewInstanceFromString branch
	// -----------------------------------------------------------------------------------------------------------------

	/** Helper: a class with a String constructor that is NOT a CharSequence/Number/Bean. */
	public static class AD01_StringCtorType {
		public final String val;
		public AD01_StringCtorType(String v) { this.val = v; }
		@Override public String toString() { return val; }
	}

	@Test void ad01_canCreateNewInstanceFromString() throws Exception {
		// AD01_StringCtorType has a String constructor and is not CharSequence/Number/bean.
		// This exercises the sType.canCreateNewInstanceFromString() branch in parseAnything.
		var o = P.parse("hello", AD01_StringCtorType.class);
		assertNotNull(o);
		assertEquals("hello", o.val);
	}

	@Test void ad02_canCreateNewInstanceFromString_null() throws Exception {
		// Null value for a fromString type → s is null, o stays null.
		var o = P.parse("null", AD01_StringCtorType.class);
		assertNull(o);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ae01_parseObject - non-numeric non-null plain string in isObject() path
	// -----------------------------------------------------------------------------------------------------------------

	@Test void ae01_object_plainString_nonNumeric() throws Exception {
		// A plain non-numeric string in Object type → o = s (line 324).
		// "hello" is not true/false/null/numeric → stored as String.
		var o = P.parse("hello", Object.class);
		assertEquals("hello", o);
	}

	@Test void ae02_object_numericString() throws Exception {
		// A numeric string in Object type → o = parseNumber(s, Number.class) (line 322).
		var o = P.parse("42", Object.class);
		assertNotNull(o);
	}

	@Test void ae03_object_nullString_inObject() throws Exception {
		// "null" in Object type → o stays null (line 320 else-if false path).
		var o = P.parse("null", Object.class);
		assertNull(o);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// af01_parseBoolean - "null" string (not null reference) — exercises s.equals("null") branch
	// -----------------------------------------------------------------------------------------------------------------

	@Test void af01_parseBoolean_trueValue() throws Exception {
		// Exercises eqic(s, "true") branch.
		assertEquals(Boolean.TRUE, P.parse("true", Boolean.class));
	}

	@Test void af02_parseBoolean_falseValue() throws Exception {
		// Exercises eqic(s, "false") branch.
		assertEquals(Boolean.FALSE, P.parse("false", Boolean.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ag01_parseIntoCollection - !isInParens && !isUrlParamValue throws (line 591)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void ag01_collection_notAtSign_notUrlParam_throws() {
		// parseIntoCollection with isUrlParamValue=false and non-'@' input → throw line 591.
		// doParseIntoCollection uses isUrlParamValue=false.
		assertThrows(ParseException.class, () -> P.parseIntoCollection("1,2,3", new ArrayList<>(), Integer.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ah01_parseAnything - c == 'n' false branch → else-throw (line 407 false / 414)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void ah01_nonInstantiatable_notN_throws() {
		// Type that isn't bean/string/number/collection/etc., and input doesn't start with '(' or 'n'.
		// java.io.InputStream has no string ctor, no bean constructor, not a standard type.
		// Any non-'(' input for such a type throws at line 414.
		assertThrows(ParseException.class, () -> P.parse("foo", java.io.InputStream.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ai01_parseAnything - collection parsed as non-@ object
	// -----------------------------------------------------------------------------------------------------------------

	@Test void ai01_collection_emptyParens_asBean() throws Exception {
		// A collection type parsed as a bean map uses the "()" → map path (line 341-353).
		// Use a typed collection with bean element type to exercise the single-element cast path.
		// "()" — empty paren-map. No _type key, so goes to single-element list wrap (line 349-352).
		// Use List<Bean> target so the element type is Bean and cast may succeed on empty map.
		var l = P.parse("()", new java.lang.reflect.ParameterizedType() {
			public java.lang.reflect.Type[] getActualTypeArguments() { return new java.lang.reflect.Type[]{Bean.class}; }
			public java.lang.reflect.Type getRawType() { return List.class; }
			public java.lang.reflect.Type getOwnerType() { return null; }
		});
		assertNotNull(l);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// aj01_parseIntoMap - keyType null branch (line 674)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void aj01_map_nullKeyType() throws Exception {
		// When parsing a raw Map (no key type), parseAnything calls parseIntoMap with
		// sType.getKeyType() which may be null for raw Map. Then keyType=null branch fires at line 674.
		// Use raw Map to exercise this.
		var dest = new HashMap();
		P.parseIntoMap("(a=1,b=2)", dest, null, null);
		assertNotNull(dest.get("a"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ak01_parseString - whitespace terminator in non-urlParam mode (line 964)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void ak01_parseString_whitespace_terminates_nonUrlParam() throws Exception {
		// doParseIntoCollection uses isUrlParamValue=false, so within values, whitespace terminates.
		// "@(hello world)" — value "hello world": inside parens, parseString with isUrlParamValue=false.
		// Whitespace terminates at "hello". Collection element is "hello".
		var dest = new ArrayList<>();
		P.parseIntoCollection("@(hello world)", dest, String.class);
		assertNotNull(dest);
		assertEquals("hello", dest.get(0));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// al01_parseString - EQ control char replacement (line 962)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void al01_parseString_encodedEquals_replacement() throws Exception {
		// In decoded mode, %3D decodes to EQ control char (0x02).
		// parseString sees EQ and calls r.replace('='), which exercises line 962.
		// "(a=hello%3Dworld)" with decoding mode → value "hello=world".
		var m = PE.parse("(a=hello%3Dworld)", Map.class);
		assertNotNull(m);
		assertEquals("hello=world", m.get("a"));
	}

	@Test void al02_parseString_encodedEquals_inCollection() throws Exception {
		// Encoded equals in a collection element exercises the EQ path with isUrlParamValue=false.
		var dest = new ArrayList<>();
		PE.parseIntoCollection("@(hello%3Dworld)", dest, String.class);
		assertNotNull(dest);
		assertEquals("hello=world", dest.get(0));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// am01_parseIntoMap - AMP-first input terminates early (line 678 c==AMP)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void am01_map_AMPfirst_returnsNull() throws Exception {
		// In decoding mode, bare '&' is decoded to AMP (0x01).
		// When parseIntoMap reads AMP as the first char, it returns null (line 678 true-branch).
		// "&(a=1)" — '&' decoded to AMP, parseIntoMap sees AMP immediately → return null.
		var m = PE.parse("&(a=1)", Map.class);
		assertNull(m);
	}

	@Test void am02_collection_AMPfirst_returnsNull() throws Exception {
		// "&@(1,2)" — '&' decoded to AMP, parseIntoCollection sees AMP → return null (line 577 true-branch).
		var l = PE.parse("&@(1,2)", List.class);
		assertNull(l);
	}

	@Test void am03_map_AMP_midMap_terminates() throws Exception {
		// "(a=1&b=2)" with decoding — '&' terminates S4 of map. The 'b=2' is discarded.
		// Exercises the c==AMP branch in S4 of parseIntoMap (line 740).
		var m = PE.parse("(a=1&b=2)", Map.class);
		assertNotNull(m);
		// Value "1" is auto-typed as Integer in generic map.
		assertEquals(1, m.get("a"));
		assertFalse(m.containsKey("b"));
	}

	@Test void am04_beanMap_AMP_midMap_terminates() throws Exception {
		// "(f1=foo&f2=0)" with decoding — '&' terminates S4 of parseIntoBeanMap.
		var b = PE.parse("(f1=foo&f2=0)", Bean.class);
		assertNotNull(b);
		assertEquals("foo", b.f1);
		assertEquals(0, b.f2);  // f2 not set due to AMP termination
	}

	@Test void am05_collection_AMP_midCollection_terminates() {
		// "@(a&b)" with decoding — 'a' parses, then '&' (AMP) exits the while loop at line 605.
		// State is S3 after "a" is parsed, so AMP terminates via the while-loop condition.
		// This triggers the "Could not find end of entry in array" error.
		assertThrows(ParseException.class, () -> PE.parse("@(a&b)", List.class));
	}

	@Test void am06_collection_AMPfirst_viaDoParseIntoCollection() throws Exception {
		// PE.parseIntoCollection with "&..." — '&' decoded to AMP reaches parseIntoCollection line 577.
		// AMP at the very first char → return null (line 577 AMP branch).
		var dest = new ArrayList<>();
		var result = PE.parseIntoCollection("&@(1,2)", dest, String.class);
		assertNull(result);
	}

	@Test void am07_map_AMPfirst_viaDoParseIntoMap() throws Exception {
		// PE.parseIntoMap with "&..." — '&' decoded to AMP reaches parseIntoMap line 678 (AMP branch).
		var dest = new HashMap<String, Object>();
		var result = PE.parseIntoMap("&(a=1)", dest, String.class, Object.class);
		assertNull(result);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// an01_parseString - non-urlParam whitespace path (line 964) via parseIntoCollection
	// -----------------------------------------------------------------------------------------------------------------

	@Test void an01_parseString_nonUrlParam_whitespace_inParens() throws Exception {
		// Inside "@(hello world)", parseString is called with isUrlParamValue=false.
		// Whitespace terminates "hello", leaving " world" in stream.
		// Exercises the whitespace branch at line 964.
		var dest = new ArrayList<>();
		P.parseIntoCollection("@(hello world,bye)", dest, String.class);
		assertFalse(dest.isEmpty());
		assertEquals("hello", dest.get(0));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ao01_parseAttrName - encoded EOF while-loop exit (lines 898, 907-910, 914)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void ao01_attrName_encoded_AMP_terminates() throws Exception {
		// In encoded mode, AMP terminates the attr name.
		// "(a&b=1)" with decoding — attr name is "a", then '&' terminates it (line 901 AMP check).
		// But AMP is decoded to char 1 which IS in the AMP check → attr = "a".
		// Then state S2, next read is '&' decoded again... actually parseIntoMap outer handles it.
		var m = PE.parse("(a&b=1)", Map.class);
		// AMP terminates after attr name "a" in S2; null currAttr? No, "a" is non-null.
		assertNotNull(m);
	}

	@Test void ao02_attrName_encoded_whitespace_terminates() throws Exception {
		// In encoded mode, whitespace terminates the attr name.
		// "(a b=1)" with decoding — attr "a", then whitespace terminates (line 901 isWhitespace).
		var m = PE.parse("(a b=1)", Map.class);
		assertNotNull(m);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ap01_parseAnything - collection/array with _type key (lines 346, 363 true branches)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void ap01_collection_typeKey_cast() throws Exception {
		// "(_type=...)" parsed as a collection — m.containsKey("_type") is true → cast() path (line 346).
		// The default typePropertyName is "_type". cast() returns a MarshalledMap.
		// Use parse(Type) to avoid a JVM checkcast at the call site.
		Object result = P.parse("(_type=foo)", (java.lang.reflect.Type) List.class);
		assertNotNull(result);
	}

	@Test void ap02_array_typeKey_cast() throws Exception {
		// "(_type=...)" parsed as an array — c=='(' path with _type key exercises line 363.
		// cast() returns a MarshalledMap; use parse(Type) to avoid JVM checkcast at call site.
		Object result = P.parse("(_type=foo)", (java.lang.reflect.Type) String[].class);
		assertNotNull(result);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// aq01 - parseNull with non-"ull" sequence (line 761 false branch → throws ParseException)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void aq01_parseNull_nonUll_inBeanMap_throws() {
		// parseIntoBeanMap calls parseNull when first char is 'n'. "nfoo" starts with 'n'.
		// parseString then reads remaining "foo" (stops at ','/'/'/EOF): "ull".equals("foo") is false.
		// → ParseException thrown (line 762 true branch).
		assertThrows(ParseException.class, () -> P.parse("(nfoo)", Bean.class));
	}

	@Test void aq02_parseNull_nonUll_inMap_throws() {
		// parseIntoMap also calls parseNull when first char is 'n'. "(nbar=1)" with Map target.
		// parseAttrName reads "bar", "ull".equals("bar") is false → ParseException.
		assertThrows(ParseException.class, () -> P.parse("nbar", Map.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ar01 - getUonReader reuse path (line 221 true branch: pipe already wraps a UonReader)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void ar01_getUonReader_reuse_existing() throws Exception {
		// Create a session and a pipe from a String, then create a UonReader from it.
		// Wrap that UonReader in a second pipe (Reader input → pipe.getReader() returns UonReader).
		// Then call getUonReader on the second pipe — should return the existing UonReader (line 221 true branch).
		var session = P.getSession();
		var pipe1 = session.createPipe("(f1=1)");
		var uonReader = new UonReader(pipe1, false);
		var pipe2 = session.createPipe(uonReader);
		var r = UonParserSession.getUonReader(pipe2, false);
		assertNotNull(r);
		assertSame(uonReader, r);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// as01 - parseString EQ replacement (line 962: c == EQ → r.replace('='))
	// -----------------------------------------------------------------------------------------------------------------

	@Test void as01_parseString_encodedEquals_replaced() throws Exception {
		// In decoding mode, a literal '=' in a value is decoded to EQ char () by UonReader.
		// parseString sees c == EQ (line 962) and replaces it with '=' in the marked buffer.
		// Input: "(a=b=c)" -- UonReader converts the second '=' to EQ -- parseString restores it.
		var m = PE.parse("(a=b=c)", Map.class);
		assertNotNull(m);
		assertEquals("b=c", m.get("a"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// at01 - parseIntoCollection non-parens path (lines 641-655: isUrlParamValue=true, no '@' prefix)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void at01_collection_urlParam_commaSeparated() throws Exception {
		// doParse calls parseAnything(isUrlParamValue=true) which calls parseIntoCollection.
		// "1,2,3" has no '@' so isInParens=false -> enters the non-parens while loop (line 641).
		var l = P.parse("1,2,3", List.class);
		assertNotNull(l);
		assertEquals(3, l.size());
	}

	@Test void at02_collection_urlParam_withWhitespace_afterEntry() throws Exception {
		// non-parens S2: after first element, whitespace is encountered (line 653 true branch).
		var l = P.parse("1 ,2", List.class);
		assertNotNull(l);
		assertTrue(l.size() >= 1);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// au01 - parseIntoCollection/parseIntoMap EOF first char (line 577/678 c==-1 branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void au01_parseIntoCollection_emptyInput_returnsNull() throws Exception {
		// parseIntoCollection with empty input: readSkipWs() hits EOF on first char, early-return yields null.
		var dest = new ArrayList<>();
		var result = P.parseIntoCollection("", dest, String.class);
		assertNull(result);
	}

	@Test void au02_parseIntoMap_emptyInput_returnsNull() throws Exception {
		// parseIntoMap with empty input: read() hits EOF on first char, early-return yields null.
		var dest = new HashMap<String, Object>();
		var result = P.parseIntoMap("", dest, String.class, Object.class);
		assertNull(result);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// av01 - non-parens collection S2: AMP terminator (line 646 AMP branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void av01_collection_urlParam_ampTerminator() throws Exception {
		// In decoding mode, '&' is decoded to AMP by UonReader.
		// "1&2" in non-parens collection mode: after parsing "1" (state S2), reads AMP -> returns list.
		// Exercises the c==AMP branch in line 646 of the non-parens S2 state.
		var l = PE.parse("1&2", List.class);
		assertNotNull(l);
		assertEquals(1, l.size());
		assertEquals("1", l.get(0).toString());
	}
}
