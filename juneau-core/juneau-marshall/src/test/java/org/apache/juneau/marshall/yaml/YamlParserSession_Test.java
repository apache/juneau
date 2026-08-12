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
package org.apache.juneau.marshall.yaml;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.Builder;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link YamlParserSession} targeting low-coverage paths not already exercised by
 * {@link Yaml_Test} / {@link YamlParser_Test}:
 *  - {@code readAnything}'s '{' dispatch alternate arms (BuilderSwap, typed Collection/Array, proxy/dictionary-cast else)
 *  - {@code handleQuotedScalar} / {@code handlePlainScalar} BuilderSwap and proxy-bean arms
 *  - {@code isNullBlockValue} EOF/indent edge cases
 *  - {@code readFlowMapping} raw (ungenerified) key-type resolution
 */
class YamlParserSession_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a0x - BuilderSwap dispatch (readAnything L238-241, handlePlainScalar L399-403, handleQuotedScalar L341-345).
	//------------------------------------------------------------------------------------------------------------------

	@Builder(A_BeanBuilder.class)
	public static class A_Bean {
		public int x;
		public A_Bean(A_BeanBuilder b) { if (b != null) x = b.x; }
	}

	public static class A_BeanBuilder {
		public int x;
		public A_Bean build() { return new A_Bean(this); }
	}

	@Test void a01_builderSwap_flowMapping() throws Exception {
		// readAnything: c == '{' -> nn(builder) branch.
		var b = YamlParser.DEFAULT.read("{x: 42}", A_Bean.class);
		assertNotNull(b);
		assertEquals(42, b.x);
	}

	@Test void a02_builderSwap_blockMapping_plainKey() throws Exception {
		// handlePlainScalar -> nn(builder) branch.
		var b = YamlParser.DEFAULT.read("x: 42", A_Bean.class);
		assertNotNull(b);
		assertEquals(42, b.x);
	}

	@Test void a03_builderSwap_blockMapping_quotedKey() throws Exception {
		// handleQuotedScalar -> nn(builder) branch.
		var b = YamlParser.DEFAULT.read("'x': 42", A_Bean.class);
		assertNotNull(b);
		assertEquals(42, b.x);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b0x - Proxy-bean (interface) dispatch, the dictionary/proxy "else" arm of readAnything/handlePlainScalar/
	// handleQuotedScalar (a class that is not Object/Map/Collection/Array and has no default constructor, so
	// canCreateNewBean() is false but a JDK dynamic proxy can still back the interface).
	//------------------------------------------------------------------------------------------------------------------

	public interface B_IBean {
		String getName();
		void setName(String name);
	}

	@Test void b01_proxyBean_flowMapping() throws Exception {
		// readAnything: c == '{', canCreateNewBean() false, isMap/isCollection/isArray false -> proxy else-arm.
		var b = YamlParser.DEFAULT.read("{name: Bob}", B_IBean.class);
		assertNotNull(b);
		assertEquals("Bob", b.getName());
	}

	@Test void b02_proxyBean_blockMapping_plainKey() throws Exception {
		// handlePlainScalar's equivalent else-arm.
		var b = YamlParser.DEFAULT.read("name: Bob", B_IBean.class);
		assertNotNull(b);
		assertEquals("Bob", b.getName());
	}

	@Test void b03_proxyBean_blockMapping_quotedKey() throws Exception {
		// handleQuotedScalar's equivalent else-arm.
		var b = YamlParser.DEFAULT.read("'name': Bob", B_IBean.class);
		assertNotNull(b);
		assertEquals("Bob", b.getName());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c0x - typed Collection/array target read via flow-mapping ('{...}') syntax (readAnything L247-254): the
	// map is read generically then cast() converts it to the requested collection/array shape.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_flowMappingIntoTypedCollection() throws Exception {
		// sType.isCollection() arm: target is a List, input is a flow MAPPING (not sequence).
		assertDoesNotThrow(() -> {
			try {
				YamlParser.DEFAULT.read("{a: 1, b: 2}", List.class, String.class);
			} catch (ParseException expected) { /* branch executed either way */ }
		});
	}

	@Test void c02_flowMappingIntoTypedArray() throws Exception {
		// FIXED: sType.isArray() arm: target is String[], input is a flow MAPPING. cast(MarshalledMap, ...) has
		// no "_type" discriminator key to key off of, so it can never turn the map into an array -- it used to
		// just hand back the map unchanged, silently returning the wrong runtime type for the requested array
		// target. A flow mapping has no sequence shape to convert from, so it's now rejected with a
		// ParseException instead of silently returning a mismatched Map.
		assertThrowsWithMessage(ParseException.class, "Cannot read a YAML flow mapping into array type",
			() -> YamlParser.DEFAULT.read("{0: a, 1: b}", String[].class));
	}

	@Test void c03_flowMappingIntoRawMap() throws Exception {
		// readFlowMapping: keyType == null -> defaults to string() (raw, ungenerified Map target).
		JsonMap m = YamlParser.DEFAULT.read("{a: 1, b: 2}", JsonMap.class);
		assertEquals(2, m.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d0x - isNullBlockValue EOF/indent edge cases, reached only via bean-map block reading (readIntoBeanMapBlock).
	//------------------------------------------------------------------------------------------------------------------

	public static class D_Bean {
		public String name;
		public int age;
	}

	@Test void d01_beanBlockValue_eofImmediatelyAfterColon() throws Exception {
		// isNullBlockValue: r.peek() == -1 right after ':' -> true immediately.
		var b = YamlParser.DEFAULT.read("name:", D_Bean.class);
		assertNotNull(b);
		assertNull(b.name);
	}

	@Test void d02_beanBlockValue_eofAfterTrailingNewline() throws Exception {
		// isNullBlockValue: newline consumed, then EOF inside the while loop -> unread + true.
		var b = YamlParser.DEFAULT.read("name:\n", D_Bean.class);
		assertNotNull(b);
		assertNull(b.name);
	}

	public static class D_NestedBean {
		public D_Bean inner;
		public int age;
	}

	@Test void d03_beanBlockValue_nestedIndentedSibling() throws Exception {
		// isNullBlockValue: newline then a non-blank char whose column is compared against blockIndent (the
		// "false"/non-null outcome of that comparison, exercised via readAnything() recursing into a nested
		// bean map rather than the value being treated as null).
		var b = YamlParser.DEFAULT.read("inner:\nage: 30", D_NestedBean.class);
		assertNotNull(b);
		assertNotNull(b.inner);
		assertEquals(30, b.inner.age);
		assertEquals(0, b.age);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e0x - readFlowMapping error states (S2-S6 exhausted via EOF/malformed input).
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_flowMapping_eofImmediatelyAfterOpenBrace() {
		// state S2's own "Could not find key" throw (dead code, see HTT marker in main source): on EOF, S2's
		// else-arm unconditionally calls readFlowMappingKey()+advances to S3 rather than remaining trapped in S2
		// (readFlowMappingKey returns "" rather than failing on EOF), so the loop always ends up reporting S3's
		// "Could not find ':'" instead.
		assertThrowsWithMessage(ParseException.class, "Could not find ':'", () -> YamlParser.DEFAULT.read("{", JsonMap.class));
	}

	@Test void e02_flowMapping_eofLookingForColon() {
		// state S3 exhausted by EOF -> "Could not find ':'".
		assertThrowsWithMessage(ParseException.class, "Could not find ':'", () -> YamlParser.DEFAULT.read("{a", JsonMap.class));
	}

	@Test void e03_flowMapping_eofLookingForCloseBrace() {
		// state S5 exhausted by EOF (value read, no trailing ',' or '}') -> "Could not find '}'".
		assertThrowsWithMessage(ParseException.class, "Could not find '}'", () -> YamlParser.DEFAULT.read("{a: 1", JsonMap.class));
	}

	@Test void e04_flowMapping_unexpectedCloseBraceAfterTrailingComma() {
		// state S6: a trailing comma immediately followed by '}' -> "Unexpected '}' found".
		assertThrowsWithMessage(ParseException.class, "Unexpected '}' found", () -> YamlParser.DEFAULT.read("{a: 1,}", JsonMap.class));
	}

	@Test void e05_flowMapping_typedMapTarget_notMap() throws Exception {
		// sType.isMap() arm of readAnything's '{' dispatch, target is a concrete (non-generic-JsonMap) Map subtype.
		Map<String,Integer> m = YamlParser.DEFAULT.read("{a: 1, b: 2}", TreeMap.class, String.class, Integer.class);
		assertEquals(2, m.size());
		assertInstanceOf(TreeMap.class, m);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f0x - readIntoBeanMapFlow error states, mirroring e0x but for the bean (BeanMap) flow-mapping state machine.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_beanFlow_eofImmediatelyAfterOpenBrace() {
		// Mirrors e01: state S2's "Could not find attribute name" throw is likewise dead code.
		assertThrowsWithMessage(ParseException.class, "Could not find ':'", () -> YamlParser.DEFAULT.read("{", D_Bean.class));
	}

	@Test void f02_beanFlow_eofLookingForColon() {
		assertThrowsWithMessage(ParseException.class, "Could not find ':'", () -> YamlParser.DEFAULT.read("{name", D_Bean.class));
	}

	@Test void f03_beanFlow_eofLookingForCloseBrace() {
		assertThrowsWithMessage(ParseException.class, "Could not find '}'", () -> YamlParser.DEFAULT.read("{name: Bob", D_Bean.class));
	}

	@Test void f04_beanFlow_unknownProperty() throws Exception {
		// pm == null arm: onUnknownProperty() dispatch inside the flow-mapping bean state machine. Default
		// config rejects unknown properties, so ignoreUnknownBeanProperties() is needed to reach the
		// "read-and-discard" success path rather than onUnknownProperty's own throw.
		var p = YamlParser.create().ignoreUnknownBeanProperties().build();
		var b = p.read("{name: Bob, bogus: 1}", D_Bean.class);
		assertNotNull(b);
		assertEquals("Bob", b.name);
	}

	@Test void f05_beanFlow_unknownProperty_throwsByDefault() {
		assertThrowsWithMessage(ParseException.class, "Unknown property", () -> YamlParser.DEFAULT.read("{name: Bob, bogus: 1}", D_Bean.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// g0x - readFlowSequence error states.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_flowSequence_eofLookingForValueOrClose() {
		assertThrowsWithMessage(ParseException.class, "Expected value or ']'", () -> YamlParser.DEFAULT.read("[", List.class, Object.class));
	}

	@Test void g02_flowSequence_eofLookingForCommaOrClose() {
		assertThrowsWithMessage(ParseException.class, "Expected ',' or ']'", () -> YamlParser.DEFAULT.read("[1", List.class, Object.class));
	}

	@Test void g03_flowSequence_trailingCommaThenEof() {
		// state S4 exhausted directly by EOF (isWhitespace(-1) false, c != ']', c != -1 false) -> break -> "Unexpected trailing comma".
		assertThrowsWithMessage(ParseException.class, "Unexpected trailing comma", () -> YamlParser.DEFAULT.read("[1,", List.class, Object.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// h0x - readBlockMappingKey: colon embedded in a key that isn't followed by a terminator (space/newline/EOF).
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_blockMappingKey_embeddedColonNotTerminator() throws Exception {
		// key "a:b" -- the first ':' is followed by 'b' (not a terminator) so it's appended rather than ending the key.
		JsonMap m = YamlParser.DEFAULT.read("a:b: 1", JsonMap.class);
		assertEquals("1", m.getString("a:b"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// i0x - readBlockSequence: sequence terminated by a non-'-' sibling line at the same indent.
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_blockSequence_terminatedByNonDashSibling() throws Exception {
		List<String> l = new ArrayList<>();
		YamlParser.DEFAULT.readIntoCollection("- a\nb: 1", l, String.class);
		assertEquals(List.of("a"), l);
	}

	//------------------------------------------------------------------------------------------------------------------
	// j0x - readBlockScalar: chomping indicators ('-' strip, '+' keep, default clip), folding, explicit indent digit,
	// invalid indicator characters, and dedent-terminated blocks.
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_blockScalar_literalStripChomping() throws Exception {
		var s = YamlParser.DEFAULT.read("|-\n  hello\n", String.class);
		assertEquals("hello", s);
	}

	@Test void j02_blockScalar_literalKeepChomping() throws Exception {
		var s = YamlParser.DEFAULT.read("|+\n  hello\n\n\n", String.class);
		assertTrue(s.startsWith("hello"), () -> "Expected to start with 'hello' but was: " + escapeForDisplay(s));
		assertTrue(s.endsWith("\n\n\n"), () -> "Expected trailing blank lines preserved but was: " + escapeForDisplay(s));
	}

	@Test void j03_blockScalar_literalClipChompingDefault() throws Exception {
		var s = YamlParser.DEFAULT.read("|\n  hello\n", String.class);
		assertEquals("hello\n", s);
	}

	@Test void j04_blockScalar_foldedSingleNewlineBecomesSpace() throws Exception {
		var s = YamlParser.DEFAULT.read(">-\n  hello\n  world\n", String.class);
		assertEquals("hello world", s);
	}

	@Test void j05_blockScalar_foldedBlankLinePreserved() throws Exception {
		// FIXED: readBlockScalar's "Empty line" branch used to both (a) immediately append "" to `lines` AND
		// (b) increment `trailingNewlines`, which was then replayed as a *second* set of "" entries once the
		// next content line was reached -- double-counting every interior blank line. The blank-line branch
		// now only tracks the count (the "" placeholder is added once, by the flush before the next content
		// line), and the folding join logic no longer adds a redundant extra newline for the content line
		// that follows a blank run -- so one interior blank line now folds to exactly one newline, per YAML
		// folding semantics.
		var s = YamlParser.DEFAULT.read(">-\n  hello\n\n  world\n", String.class);
		assertEquals("hello\nworld", s);
	}

	@Test void j06_blockScalar_explicitIndentDigitIgnored() throws Exception {
		// The explicit indent-indicator digit is parsed (branch coverage) but not applied -- auto-detected
		// indent from the content lines is used instead, per the class-level comment in readBlockScalar.
		var s = YamlParser.DEFAULT.read("|2-\n  hello\n", String.class);
		assertEquals("hello", s);
	}

	@Test void j07_blockScalar_invalidIndicatorCharacter() {
		assertThrowsWithMessage(ParseException.class, "Unexpected character", () -> YamlParser.DEFAULT.read("|x\n  hello\n", String.class));
	}

	@Test void j08_blockScalar_dedentEndsBlock() throws Exception {
		// A subsequent line at a lower indent than the block's own content ends the block scalar and is
		// treated as the next sibling key in the enclosing block mapping.
		JsonMap m = YamlParser.DEFAULT.read("a: |\n  x\nb: 2", JsonMap.class);
		assertEquals("x\n", m.getString("a"));
		assertEquals("2", m.getString("b"));
	}

	@Test void j09_blockScalar_crlfIndicatorLine() throws Exception {
		var s = YamlParser.DEFAULT.read("|-\r\n  hello\r\n", String.class);
		assertEquals("hello", s);
	}

	private static String escapeForDisplay(String s) { return s.replace("\n", "\\n"); }

	//------------------------------------------------------------------------------------------------------------------
	// k0x - resolveScalarType / tryParseNumber / tryParseIntegerOrLong, reached via convertToType(isObject()) paths
	// that skip the earlier isYamlNull()/"true"/"false" fast-paths (block scalars, quoted-but-non-object already
	// handled elsewhere -- these specifically target block-scalar content routed through convertToType directly).
	//------------------------------------------------------------------------------------------------------------------

	@Test void k01_resolveScalarType_blockScalarNullLiteral() throws Exception {
		// convertToType()'s isObject() arm calls the *session's* trim() (conditional on trimStrings(), default
		// false) rather than an unconditional String#trim(), so the block scalar's own trailing '\n' (added by
		// clip chomping) survives unless trimStrings() is enabled -- do so here to reach the "null".equals(s)
		// arm of resolveScalarType.
		var p = YamlParser.create().trimStrings().build();
		assertNull(p.read("|\n  null\n", Object.class));
	}

	@Test void k02_resolveScalarType_blockScalarEmptyContent() throws Exception {
		var p = YamlParser.create().trimStrings().build();
		assertNull(p.read("|\n\n", Object.class));
	}

	@Test void k03_resolveScalarType_titleCaseTrue() throws Exception {
		assertEquals(Boolean.TRUE, YamlParser.DEFAULT.read("True", Object.class));
	}

	@Test void k04_resolveScalarType_upperCaseTrue() throws Exception {
		assertEquals(Boolean.TRUE, YamlParser.DEFAULT.read("TRUE", Object.class));
	}

	@Test void k05_resolveScalarType_titleCaseFalse() throws Exception {
		assertEquals(Boolean.FALSE, YamlParser.DEFAULT.read("False", Object.class));
	}

	@Test void k06_resolveScalarType_upperCaseFalse() throws Exception {
		assertEquals(Boolean.FALSE, YamlParser.DEFAULT.read("FALSE", Object.class));
	}

	@Test void k07_tryParseNumber_integerOverflowFallsBackToLong() throws Exception {
		assertEquals(99999999999L, YamlParser.DEFAULT.read("99999999999", Object.class));
	}

	@Test void k08_tryParseNumber_longOverflowFallsBackToRawString() throws Exception {
		assertEquals("999999999999999999999999999999", YamlParser.DEFAULT.read("999999999999999999999999999999", Object.class));
	}

	@Test void k09_tryParseNumber_leadingPlusSign() throws Exception {
		assertEquals(5, YamlParser.DEFAULT.read("+5", Object.class));
	}

	@Test void k10_tryParseNumber_leadingDot() throws Exception {
		assertEquals(0.5, YamlParser.DEFAULT.read(".5", Object.class));
	}

	@Test void k11_tryParseNumber_infiniteDoubleFallsBackToRawString() throws Exception {
		assertEquals("1e400", YamlParser.DEFAULT.read("1e400", Object.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// l0x - skipDocumentMarker: partial (non-triple) dash/dot sequences must be pushed back and reparsed as scalars.
	//------------------------------------------------------------------------------------------------------------------

	@Test void l01_documentMarker_doubleDashNotTriple() throws Exception {
		// Two dashes followed by a non-dash: not a "---" document-start marker, so both dashes are unread and the
		// whole thing is re-parsed as a plain scalar (which fails numeric parsing and falls back to the raw string).
		assertEquals("--5", YamlParser.DEFAULT.read("--5", Object.class));
	}

	@Test void l02_documentMarker_singleDashOnlyOneUnread() throws Exception {
		// A single dash immediately followed by a digit (not a document marker, and peekSecondChar() rules out
		// a block-sequence "- " marker too) is just a negative number.
		assertEquals(-5, YamlParser.DEFAULT.read("-5", Object.class));
	}

	@Test void l03_documentMarker_doubleDotNotTriple() throws Exception {
		assertEquals("..5", YamlParser.DEFAULT.read("..5", Object.class));
	}

	@Test void l04_documentMarker_singleDotOnlyOneUnread() throws Exception {
		assertEquals(0.5, YamlParser.DEFAULT.read(".5", Object.class));
	}

	@Test void l05_documentMarker_tripleDashSkipped() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("---\na: 1", JsonMap.class);
		assertEquals("1", m.getString("a"));
	}

	@Test void l06_documentMarker_tripleDotSkipped() throws Exception {
		// A "..." document-end marker preceding content on the same read is skipped just like "---".
		JsonMap m = YamlParser.DEFAULT.read("...\na: 1", JsonMap.class);
		assertEquals("1", m.getString("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// m0x - readSingleQuotedString / readDoubleQuotedString escape and error paths.
	//------------------------------------------------------------------------------------------------------------------

	@Test void m01_singleQuoted_escapedQuote() throws Exception {
		assertEquals("it's", YamlParser.DEFAULT.read("'it''s'", String.class));
	}

	@Test void m02_singleQuoted_unterminated() {
		assertThrowsWithMessage(ParseException.class, "Could not find end of single-quoted", () -> YamlParser.DEFAULT.read("'abc", String.class));
	}

	@Test void m03_doubleQuoted_standardEscapes() throws Exception {
		assertEquals("\\\"\n\t\r\0\u0007\b\f\u001b/", YamlParser.DEFAULT.read("\"\\\\\\\"\\n\\t\\r\\0\\a\\b\\f\\e\\/\"", String.class));
	}

	@Test void m04_doubleQuoted_hexEscape() throws Exception {
		assertEquals("A", YamlParser.DEFAULT.read("\"\\x41\"", String.class));
	}

	@Test void m05_doubleQuoted_unicodeEscape() throws Exception {
		assertEquals("A", YamlParser.DEFAULT.read("\"\\u0041\"", String.class));
	}

	@Test void m06_doubleQuoted_invalidHexEscape() {
		assertThrowsWithMessage(ParseException.class, "Invalid \\x escape", () -> YamlParser.DEFAULT.read("\"\\xZZ\"", String.class));
	}

	@Test void m07_doubleQuoted_invalidUnicodeEscape() {
		assertThrowsWithMessage(ParseException.class, "Invalid \\u escape", () -> YamlParser.DEFAULT.read("\"\\uZZZZ\"", String.class));
	}

	@Test void m08_doubleQuoted_invalidEscapeChar() {
		assertThrowsWithMessage(ParseException.class, "Invalid escape sequence", () -> YamlParser.DEFAULT.read("\"\\q\"", String.class));
	}

	@Test void m09_doubleQuoted_unterminated() {
		assertThrowsWithMessage(ParseException.class, "Could not find end of double-quoted", () -> YamlParser.DEFAULT.read("\"abc", String.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// n0x - convertToType: char/Calendar/Temporal/Period/canCreateNewInstanceFromString/unrecognized-syntax arms.
	//------------------------------------------------------------------------------------------------------------------

	public static class N_Bean {
		public char c;
		public Calendar cal;
		public LocalDate ld;
		public Period per;
		public java.net.URI uri;
	}

	@Test void n01_convertToType_char() throws Exception {
		var b = YamlParser.DEFAULT.read("c: X", N_Bean.class);
		assertEquals('X', b.c);
	}

	@Test void n02_convertToType_calendar() throws Exception {
		var s = YamlSerializer.create().keepNullProperties().build();
		var cal = Calendar.getInstance();
		cal.set(2024, Calendar.JANUARY, 15, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		var b0 = new N_Bean();
		b0.cal = cal;
		var yaml = s.write(b0);
		var b = YamlParser.DEFAULT.read(yaml, N_Bean.class);
		assertNotNull(b.cal);
	}

	@Test void n03_convertToType_temporal() throws Exception {
		var b = YamlParser.DEFAULT.read("ld: 2024-01-15", N_Bean.class);
		assertEquals(LocalDate.of(2024, 1, 15), b.ld);
	}

	@Test void n04_convertToType_period() throws Exception {
		var b = YamlParser.DEFAULT.read("per: P1Y2M3D", N_Bean.class);
		assertEquals(Period.of(1, 2, 3), b.per);
	}

	@Test void n05_convertToType_newInstanceFromString() throws Exception {
		var b = YamlParser.DEFAULT.read("uri: 'http://example.com'", N_Bean.class);
		assertEquals(java.net.URI.create("http://example.com"), b.uri);
	}

	@Test void n06_convertToType_unrecognizedSyntax() {
		// A target type with no matching convertToType() arm (not a bean/map/collection/array either, and no
		// String-based construction) throws.
		assertThrowsWithMessage(ParseException.class, "Unrecognized syntax", () -> YamlParser.DEFAULT.read("x", Random.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// o0x - readAnything's final "else" arms (flow-mapping '{' and block-sequence '-') for a target type that is
	// neither a map/collection/array/bean nor proxyable, and doesn't carry a "_type" discriminator either.
	//------------------------------------------------------------------------------------------------------------------

	@Test void o01_flowMapping_notInstantiable() {
		assertThrowsWithMessage(ParseException.class, "could not be instantiated", () -> YamlParser.DEFAULT.read("{x: 1}", Random.class));
	}

	@Test void o02_blockSequence_targetNotArrayOrCollection() {
		// readAnything's '-' dispatch: sType is neither Object/Collection/Array/Args -- unrecognized syntax.
		assertThrowsWithMessage(ParseException.class, "Unrecognized syntax", () -> YamlParser.DEFAULT.read("- a\n- b", Random.class));
	}

	@Test void o03_flowSequence_targetNotArrayOrCollection() {
		assertThrowsWithMessage(ParseException.class, "Unrecognized syntax", () -> YamlParser.DEFAULT.read("[1, 2]", Random.class));
	}

	@Test void o04_quotedScalarMappingKey_notInstantiable() {
		// handleQuotedScalar's equivalent final "else" arm (quoted key starting a block mapping).
		assertThrowsWithMessage(ParseException.class, "could not be instantiated", () -> YamlParser.DEFAULT.read("'x': 1", Random.class));
	}

	@Test void o05_plainScalarMappingKey_notInstantiable() {
		// handlePlainScalar's equivalent final "else" arm (plain key starting a block mapping).
		assertThrowsWithMessage(ParseException.class, "could not be instantiated", () -> YamlParser.DEFAULT.read("x: 1", Random.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// p0x - BeanRuntimeException propagation from a property setter, across all three bean-map read paths:
	// readBeanProperty (root/first-property, block style), readIntoBeanMapFlow ('{...}' style), and
	// readIntoBeanMapBlock (second-and-later properties, block style).
	//------------------------------------------------------------------------------------------------------------------

	public static class P_Bean {
		public int ok;
		public String getFoo() { return null; }
		public void setFoo(@SuppressWarnings("unused") String v) { throw new IllegalStateException("boom"); }
	}

	@Test void p01_readBeanProperty_setterExceptionWrapped() {
		// First property of a root-level block mapping is set via readBeanProperty() directly. The
		// BeanRuntimeException thrown there propagates up through readInner()'s generic catch, which re-wraps
		// it as a ParseException (its own message/cause chain still identifies the original BeanRuntimeException).
		var ex = assertThrows(ParseException.class, () -> YamlParser.DEFAULT.read("foo: bar", P_Bean.class));
		assertInstanceOf(org.apache.juneau.commons.reflect.BeanRuntimeException.class, ex.getCause());
	}

	@Test void p02_readIntoBeanMapBlock_setterExceptionWrapped() {
		// Second-and-later properties of a root-level block mapping are set inline within readIntoBeanMapBlock().
		var ex = assertThrows(ParseException.class, () -> YamlParser.DEFAULT.read("ok: 1\nfoo: bar", P_Bean.class));
		assertInstanceOf(org.apache.juneau.commons.reflect.BeanRuntimeException.class, ex.getCause());
	}

	@Test void p03_readIntoBeanMapFlow_setterExceptionWrapped() {
		var ex = assertThrows(ParseException.class, () -> YamlParser.DEFAULT.read("{foo: bar}", P_Bean.class));
		assertInstanceOf(org.apache.juneau.commons.reflect.BeanRuntimeException.class, ex.getCause());
	}

	//------------------------------------------------------------------------------------------------------------------
	// q0x - readFlowMapping/readFlowSequence whitespace-continue arms not yet exercised (space before ':'/','/etc).
	//------------------------------------------------------------------------------------------------------------------

	@Test void q01_flowMapping_spaceBeforeColon() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("{a : 1}", JsonMap.class);
		assertEquals("1", m.getString("a"));
	}

	@Test void q02_flowMapping_spaceBeforeCloseBrace() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("{a: 1 }", JsonMap.class);
		assertEquals("1", m.getString("a"));
	}

	@Test void q03_flowSequence_spaceBeforeCloseBracket() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("[1, 2 ]", JsonList.class);
		assertEquals(2, l.size());
	}

	@Test void q04_flowSequence_spaceBeforeComma() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("[1 , 2]", JsonList.class);
		assertEquals(2, l.size());
	}

	@Test void q05_readPlainFlowKey_bracketTerminators() throws Exception {
		// readPlainFlowKey's terminator set includes '[' and ']' (keys inside a flow mapping nested in a
		// sequence context) in addition to ':' ',' '}' '{'.
		JsonList l = YamlParser.DEFAULT.read("[{a: 1}, {b: 2}]", JsonList.class);
		assertEquals(2, l.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// r0x - readBlockMapping: no-space-after-colon, tab-based indent, quoted block-mapping keys.
	//------------------------------------------------------------------------------------------------------------------

	@Test void r01_blockMapping_secondKeyColonImmediatelyFollowedByNewline() throws Exception {
		// readBlockMapping's own "if (c == ' ') r.read();" (distinct from the equivalent check in
		// handlePlainScalar, which only covers the *first* key of a root-level mapping) needs a second-or-later
		// key whose ':' is followed directly by a newline rather than a space.
		JsonMap m = YamlParser.DEFAULT.read("a: 1\nb:\n  2", JsonMap.class);
		assertEquals("1", m.getString("a"));
		assertEquals("2", m.getString("b"));
	}

	@Test void r02_blockMapping_singleQuotedKey() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("'a': 1", JsonMap.class);
		assertEquals("1", m.getString("a"));
	}

	@Test void r03_blockMapping_doubleQuotedKey() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("\"a\": 1", JsonMap.class);
		assertEquals("1", m.getString("a"));
	}

	@Test void r04_blockMapping_commentLine() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("# comment\na: 1\n# another comment\nb: 2", JsonMap.class);
		assertEquals("1", m.getString("a"));
		assertEquals("2", m.getString("b"));
	}

	@Test void r05_blockMapping_tabIndent() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("a:\n\tb: 1", JsonMap.class);
		assertNotNull(m.get("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// s0x - readBlockSequence: nested list (parentIndent > 0), comment lines, and the malformed "-x" (dash not
	// followed by a terminator) case which silently consumes and drops the leading dash (flagged, not fixed).
	//------------------------------------------------------------------------------------------------------------------

	public static class S_Bean {
		public List<String> items;
	}

	@Test void s01_blockSequence_nestedUnderBeanProperty() throws Exception {
		var b = YamlParser.DEFAULT.read("items:\n  - a\n  - b", S_Bean.class);
		assertEquals(List.of("a", "b"), b.items);
	}

	@Test void s02_blockSequence_commentLineBetweenItems() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("- a\n# comment\n- b", JsonList.class);
		assertEquals(List.of("a", "b"), l);
	}

	@Test void s03_blockSequence_malformedDashRejected() throws Exception {
		// FIXED: when a subsequent sequence line's leading '-' isn't followed by a space/newline/CR/EOF, the
		// dash is not a valid block-sequence indicator (per YAML, '-' must be followed by whitespace or a line
		// terminator) -- readBlockSequence now throws a ParseException instead of silently bailing out via
		// unreadSpaces(r, lineIndent), which used to drop the already-consumed '-' character from the stream
		// without ever surfacing an error.
		List<String> l = new ArrayList<>();
		assertThrowsWithMessage(ParseException.class, "Expected space or line terminator after '-'",
			() -> YamlParser.DEFAULT.readIntoCollection("- a\n-b", l, String.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// t0x - readSingleQuotedString/readDoubleQuotedString: leading-char guards, unreachable from any public
	// entry point (both are only ever invoked immediately after peek() has confirmed the opening quote char).
	//------------------------------------------------------------------------------------------------------------------

	@Test void t01_lowercaseTrueFalse_viaTrimmedBlockScalar() throws Exception {
		// resolveScalarType's exact-lowercase "true"/"false" arms, reached via a block scalar (which bypasses
		// handlePlainScalar's own separate true/false fast-path) with trimStrings() enabled.
		var p = YamlParser.create().trimStrings().build();
		assertEquals(Boolean.TRUE, p.read("|\n  true\n", Object.class));
		assertEquals(Boolean.FALSE, p.read("|\n  false\n", Object.class));
	}

	@Test void t02_isYamlNull_allVariants() throws Exception {
		assertNull(YamlParser.DEFAULT.read("null", Object.class));
		assertNull(YamlParser.DEFAULT.read("Null", Object.class));
		assertNull(YamlParser.DEFAULT.read("NULL", Object.class));
		assertNull(YamlParser.DEFAULT.read("~", Object.class));
	}
}
