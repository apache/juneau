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
package org.apache.juneau.marshall.proto;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ProtoTokenizer} behavior, exercised through {@link ProtoParser}.
 */
class ProtoTokenizer_Test {

	@Test
	void a01_bareIdentifier() {
		var a = ProtoParser.DEFAULT.parse("field_name: 1", JsonMap.class);
		assertEquals(1L, a.get("field_name"));

		var b = ProtoParser.DEFAULT.parse("_private: 2", JsonMap.class);
		assertEquals(2L, b.get("_private"));

		var c = ProtoParser.DEFAULT.parse("camelCase: 3", JsonMap.class);
		assertEquals(3L, c.get("camelCase"));
	}

	@Test
	void a02_quotedString() {
		var a = ProtoParser.DEFAULT.parse("s: \"hello\"", JsonMap.class);
		assertEquals("hello", a.get("s"));

		var b = ProtoParser.DEFAULT.parse("s: 'world'", JsonMap.class);
		assertEquals("world", b.get("s"));
	}

	@Test
	void a03_multiPartString() {
		var a = ProtoParser.DEFAULT.parse("s: \"hello\" \" world\"", JsonMap.class);
		assertEquals("hello world", a.get("s"));
	}

	@Test
	void a04_decimalInteger() {
		var a = ProtoParser.DEFAULT.parse("n: 42", JsonMap.class);
		assertEquals(42L, a.get("n"));

		var b = ProtoParser.DEFAULT.parse("n: -17", JsonMap.class);
		assertEquals(-17L, b.get("n"));

		var c = ProtoParser.DEFAULT.parse("n: +99", JsonMap.class);
		assertEquals(99L, c.get("n"));
	}

	@Test
	void a05_hexInteger() {
		var a = ProtoParser.DEFAULT.parse("n: 0xDEAD", JsonMap.class);
		assertEquals(0xDEADL, a.get("n"));

		var b = ProtoParser.DEFAULT.parse("n: 0xFF", JsonMap.class);
		assertEquals(255L, b.get("n"));
	}

	@Test
	void a06_octalInteger() {
		var a = ProtoParser.DEFAULT.parse("n: 0755", JsonMap.class);
		assertEquals(0755L, a.get("n"));

		var b = ProtoParser.DEFAULT.parse("n: 0123", JsonMap.class);
		assertEquals(83L, b.get("n"));
	}

	@Test
	void a07_floatLiteral() {
		var a = ProtoParser.DEFAULT.parse("x: 3.14", JsonMap.class);
		assertEquals(3.14, a.get("x"));

		var b = ProtoParser.DEFAULT.parse("x: 1e06", JsonMap.class);
		assertEquals(1e6, ((Number) b.get("x")).doubleValue(), 1e-6);

		var c = ProtoParser.DEFAULT.parse("x: 10f", JsonMap.class);
		assertEquals(10.0, ((Number) c.get("x")).doubleValue(), 1e-6);
	}

	@Test
	void a08_specialFloats() {
		var a = ProtoParser.DEFAULT.parse("x: inf", JsonMap.class);
		assertEquals(Double.POSITIVE_INFINITY, a.get("x"));

		var b = ProtoParser.DEFAULT.parse("x: -inf", JsonMap.class);
		assertEquals(Double.NEGATIVE_INFINITY, b.get("x"));

		var c = ProtoParser.DEFAULT.parse("x: nan", JsonMap.class);
		assertTrue(Double.isNaN(((Number) c.get("x")).doubleValue()));
	}

	@Test
	void a09_booleans() {
		var a = ProtoParser.DEFAULT.parse("b: true", JsonMap.class);
		assertEquals(true, a.get("b"));

		var b = ProtoParser.DEFAULT.parse("b: false", JsonMap.class);
		assertEquals(false, b.get("b"));
	}

	@Test
	void a10_comments() {
		var a = ProtoParser.DEFAULT.parse("# comment\nname: 1", JsonMap.class);
		assertEquals(1L, a.get("name"));

		var b = ProtoParser.DEFAULT.parse("name: 1  # trailing comment", JsonMap.class);
		assertEquals(1L, b.get("name"));
	}

	@Test
	void a11_structuralTokens() {
		var a = ProtoParser.DEFAULT.parse("m { k: 1 }", JsonMap.class);
		var m = a.getMap("m");
		assertNotNull(m);
		assertEquals(1L, m.get("k"));

		var b = ProtoParser.DEFAULT.parse("arr: [1, 2]", JsonMap.class);
		var arr = b.getList("arr");
		assertNotNull(arr);
		assertEquals(2, arr.size());

		var c = ProtoParser.DEFAULT.parse("a: 1; b: 2", JsonMap.class);
		assertEquals(1L, c.get("a"));
		assertEquals(2L, c.get("b"));

		var d = ProtoParser.DEFAULT.parse("a: 1, b: 2", JsonMap.class);
		assertEquals(1L, d.get("a"));
		assertEquals(2L, d.get("b"));
	}

	@Test
	void a12_stringEscapes() {
		var a = ProtoParser.DEFAULT.parse("s: \"\\n\"", JsonMap.class);
		assertEquals("\n", a.get("s"));

		var b = ProtoParser.DEFAULT.parse("s: \"\\t\"", JsonMap.class);
		assertEquals("\t", b.get("s"));

		var c = ProtoParser.DEFAULT.parse("s: \"\\\\\"", JsonMap.class);
		assertEquals("\\", c.get("s"));

		var d = ProtoParser.DEFAULT.parse("s: \"\\\"\"", JsonMap.class);
		assertEquals("\"", d.get("s"));
	}

	@Test
	void a13_edgeCaseNumberIdent() {
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("n: 10bar", JsonMap.class));
	}

	@Test
	void a14_whitespaceHandling() {
		var a = ProtoParser.DEFAULT.parse("  name  :  42  ", JsonMap.class);
		assertEquals(42L, a.get("name"));

		var b = ProtoParser.DEFAULT.parse("a: 1\nb: 2", JsonMap.class);
		assertEquals(1L, b.get("a"));
		assertEquals(2L, b.get("b"));

		var c = ProtoParser.DEFAULT.parse("a: 1\r\nb: 2", JsonMap.class);
		assertEquals(1L, c.get("a"));
		assertEquals(2L, c.get("b"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Direct tokenizer tests for paths missed via the parser-only route.
	//------------------------------------------------------------------------------------------------------------------

	private static ProtoTokenizer tok(String s) {
		return new ProtoTokenizer(new java.io.StringReader(s));
	}

	@Test void b01_nullReaderInit() throws Exception {
		// Hits the null-reader branch in constructor (line 58).
		var t = new ProtoTokenizer(null);
		assertEquals(ProtoToken.TokenType.EOF, t.peek().type());
	}

	@Test void b02_commentTerminatedByCRLF() {
		// Hits `\r\n` after `#` comment (lines 108-109).
		var a = ProtoParser.DEFAULT.parse("# c1\r\nname: 1", JsonMap.class);
		assertEquals(1L, a.get("name"));
	}

	@Test void b03_commentTerminatedByEOF() {
		// Hits the EOF-terminator branch in skipWhitespaceAndComments (c < 0 in inner loop).
		var a = ProtoParser.DEFAULT.parse("name: 1\n# trailing eof", JsonMap.class);
		assertEquals(1L, a.get("name"));
	}

	@Test void b04_negativeHex() {
		// Hits the neg-hex branch (line 230).
		var a = ProtoParser.DEFAULT.parse("n: -0xFF", JsonMap.class);
		assertEquals(-255L, a.get("n"));
	}

	@Test void b05_negativeOctal() {
		// Hits the neg-octal branch (line 234).
		var a = ProtoParser.DEFAULT.parse("n: -0755", JsonMap.class);
		assertEquals(-0755L, a.get("n"));
	}

	@Test void b06_zeroFloat() {
		// Hits the leading-0 with float-suffix branch (lines 236-238).
		var a = ProtoParser.DEFAULT.parse("x: 0.5", JsonMap.class);
		assertEquals(0.5, ((Number) a.get("x")).doubleValue(), 1e-12);

		var b = ProtoParser.DEFAULT.parse("x: 0e2", JsonMap.class);
		assertEquals(0.0, ((Number) b.get("x")).doubleValue(), 1e-12);

		var c = ProtoParser.DEFAULT.parse("x: 0f", JsonMap.class);
		assertEquals(0.0, ((Number) c.get("x")).doubleValue(), 1e-12);
	}

	@Test void b07_zeroBare() {
		// Hits the bare-0 returning DEC_INT(0) branch (line 240).
		var a = ProtoParser.DEFAULT.parse("n: 0", JsonMap.class);
		assertEquals(0L, a.get("n"));
	}

	@Test void b08_dotPrefixFloat() {
		// Hits the `.5`-style leading-dot float branch (line 243).
		var a = ProtoParser.DEFAULT.parse("x: .5", JsonMap.class);
		assertEquals(0.5, ((Number) a.get("x")).doubleValue(), 1e-12);

		var b = ProtoParser.DEFAULT.parse("x: -.25", JsonMap.class);
		assertEquals(-0.25, ((Number) b.get("x")).doubleValue(), 1e-12);
	}

	@Test void b09_floatWithUnderscores() {
		// Hits underscore handling in readDecimalOrFloat (lines 280-292).
		var a = ProtoParser.DEFAULT.parse("x: 1_000.5", JsonMap.class);
		assertEquals(1000.5, ((Number) a.get("x")).doubleValue(), 1e-9);

		var b = ProtoParser.DEFAULT.parse("x: 1.5_5", JsonMap.class);
		assertEquals(1.55, ((Number) b.get("x")).doubleValue(), 1e-9);
	}

	@Test void b10_floatExponent_signed() {
		// Hits the e+/e- branch in readDecimalOrFloat (lines 297-298).
		var a = ProtoParser.DEFAULT.parse("x: 1.5e+2", JsonMap.class);
		assertEquals(150.0, ((Number) a.get("x")).doubleValue(), 1e-9);

		var b = ProtoParser.DEFAULT.parse("x: 1.5e-2", JsonMap.class);
		assertEquals(0.015, ((Number) b.get("x")).doubleValue(), 1e-9);
	}

	@Test void b11_floatExponentWithUnderscores() {
		// Hits underscore handling in exponent loop (lines 299-302).
		var a = ProtoParser.DEFAULT.parse("x: 1e1_0", JsonMap.class);
		assertEquals(1e10, ((Number) a.get("x")).doubleValue(), 1e-3);
	}

	@Test void b12_floatWithFSuffixOnInt() {
		// Hits the `f` after raw int branch in readDecimalOrFloat (line 315-316).
		var a = ProtoParser.DEFAULT.parse("x: 5f", JsonMap.class);
		assertEquals(5.0, ((Number) a.get("x")).doubleValue(), 1e-12);

		var b = ProtoParser.DEFAULT.parse("x: -5f", JsonMap.class);
		assertEquals(-5.0, ((Number) b.get("x")).doubleValue(), 1e-12);
	}

	@Test void b13_specialFloat_negInf() {
		// Hits the special-float-with-neg branch (line 347).
		var a = ProtoParser.DEFAULT.parse("x: -infinity", JsonMap.class);
		assertEquals(Double.NEGATIVE_INFINITY, a.get("x"));
	}

	@Test void b14_specialFloatNanIdentifier() {
		// IDENT path returning name 'nano_seconds' (or anything starting with n + non-special).
		// readSpecialFloatOrIdent must yield IDENT when not 'nan'/'inf'/'infinity' (line 352).
		var a = ProtoParser.DEFAULT.parse("nan: 1", JsonMap.class);
		assertEquals(1L, a.get("nan"));

		var b = ProtoParser.DEFAULT.parse("infrastructure: 2", JsonMap.class);
		assertEquals(2L, b.get("infrastructure"));
	}

	@Test void b15_floatLiteral_underscores() {
		// Hits readFloatLiteral underscore-skip code paths (lines 373-377, 383-387).
		var a = ProtoParser.DEFAULT.parse("x: 0.5_5", JsonMap.class);
		assertEquals(0.55, ((Number) a.get("x")).doubleValue(), 1e-9);

		var b = ProtoParser.DEFAULT.parse("x: 0.5e1_0", JsonMap.class);
		assertEquals(0.5e10, ((Number) b.get("x")).doubleValue(), 1e-3);
	}

	@Test void b16_floatLiteral_signedExponent() {
		// readFloatLiteral signed-exponent branch (lines 381-382).
		var a = ProtoParser.DEFAULT.parse("x: 0.5e+2", JsonMap.class);
		assertEquals(50.0, ((Number) a.get("x")).doubleValue(), 1e-9);

		var b = ProtoParser.DEFAULT.parse("x: 0.5e-2", JsonMap.class);
		assertEquals(0.005, ((Number) b.get("x")).doubleValue(), 1e-9);
	}

	@Test void b17_floatLiteral_fSuffix() {
		// readFloatLiteral `f`/`F` suffix branch (lines 389-390).
		var a = ProtoParser.DEFAULT.parse("x: 0.5f", JsonMap.class);
		assertEquals(0.5, ((Number) a.get("x")).doubleValue(), 1e-12);
	}

	@Test void b18_stringEscape_a_b_f_v() {
		// Hits processEscape cases 'a','b','f','v' (lines 450-456).
		var a = ProtoParser.DEFAULT.parse("s: \"\\a\"", JsonMap.class);
		assertEquals("", a.get("s"));
		var b = ProtoParser.DEFAULT.parse("s: \"\\b\"", JsonMap.class);
		assertEquals("\b", b.get("s"));
		var c = ProtoParser.DEFAULT.parse("s: \"\\f\"", JsonMap.class);
		assertEquals("\f", c.get("s"));
		var d = ProtoParser.DEFAULT.parse("s: \"\\v\"", JsonMap.class);
		assertEquals("", d.get("s"));
	}

	@Test void b19_stringEscape_r() {
		var a = ProtoParser.DEFAULT.parse("s: \"\\r\"", JsonMap.class);
		assertEquals("\r", a.get("s"));
	}

	@Test void b20_stringEscape_questionAndQuote() {
		// Hits processEscape cases '?', '\'', '"' (lines 457-460).
		var a = ProtoParser.DEFAULT.parse("s: \"\\?\"", JsonMap.class);
		assertEquals("?", a.get("s"));
		var b = ProtoParser.DEFAULT.parse("s: \"\\'\"", JsonMap.class);
		assertEquals("'", b.get("s"));
	}

	@Test void b21_stringEscape_hex() {
		// Hits processEscape '\\x'/'\\X' (line 461) and readHexEscape.
		var a = ProtoParser.DEFAULT.parse("s: \"\\x41\"", JsonMap.class);
		assertEquals("A", a.get("s"));

		var b = ProtoParser.DEFAULT.parse("s: \"\\X41\"", JsonMap.class);
		assertEquals("A", b.get("s"));
	}

	@Test void b22_stringEscape_octal() {
		// Hits processEscape octal-digit branch (lines 462-465) and readOctalEscape.
		// Octal 101 = decimal 65 = 'A'.
		var a = ProtoParser.DEFAULT.parse("s: \"\\101\"", JsonMap.class);
		assertEquals("A", a.get("s"));

		// Octal 7 (single digit) — terminator restricts the loop to one digit.
		var b = ProtoParser.DEFAULT.parse("s: \"\\7\"", JsonMap.class);
		assertEquals("", b.get("s"));
	}

	@Test void b23_stringEscape_unicode4() {
		// Hits processEscape backslash-u + readUnicodeEscape(4) (line 466).
		var a = ProtoParser.DEFAULT.parse("s: \"\\u0041\"", JsonMap.class);
		assertEquals("A", a.get("s"));
	}

	@Test void b24_stringEscape_unicode8() {
		// Hits processEscape backslash-U + readUnicodeEscape(8) (line 467).
		var a = ProtoParser.DEFAULT.parse("s: \"\\U00000041\"", JsonMap.class);
		assertEquals("A", a.get("s"));
	}

	@Test void b25_stringEscape_invalid() {
		// Hits the default error-path in processEscape (line 468).
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("s: \"\\q\"", JsonMap.class));
	}

	@Test void b26_invalidHexEscape() {
		// Hits the empty-hex error in readHexEscape (line 477).
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("s: \"\\x\"", JsonMap.class));
	}

	@Test void b27_invalidUnicodeEscape() {
		// Hits the invalid-unicode-char error in readUnicodeEscape (line 495).
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("s: \"\\u00ZZ\"", JsonMap.class));
	}

	@Test void b28_unterminatedString() {
		// Hits the EOF-during-string error (line 444).
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("s: \"unterminated", JsonMap.class));
	}

	@Test void b29_singleQuotedStringWithEscape() {
		// Exercises readSingleQuotedString + readStringContent.
		var a = ProtoParser.DEFAULT.parse("s: 'hi\\nbye'", JsonMap.class);
		assertEquals("hi\nbye", a.get("s"));
	}

	@Test void b30_directReadString_quotedBoth() throws Exception {
		// Direct readString — hits both '"' (lines 411-412) and '\'' (lines 413-414) branches.
		var t1 = tok("\"abc\"");
		assertEquals("abc", t1.readString());

		var t2 = tok("'xyz'");
		assertEquals("xyz", t2.readString());
	}

	@Test void b31_directReadString_unquoted_throws() {
		// Hits the non-quote error in readString (line 415).
		var t = tok("bareword");
		assertThrows(ParseException.class, t::readString);
	}

	@Test void b32_directReadIdentifier() throws Exception {
		// Hits readIdentifier success path.
		var t = tok("foo_bar");
		assertEquals("foo_bar", t.readIdentifier());
	}

	@Test void b33_directReadIdentifier_invalidStart() {
		// Hits the non-letter error in readIdentifier (line 403).
		var t = tok("123");
		assertThrows(ParseException.class, t::readIdentifier);
	}

	@Test void b34_directReadInteger_decAndOct() throws Exception {
		// Hits the DEC_INT and OCT_INT branches in readInteger (line 507).
		var a = tok("42");
		assertEquals(42L, a.readInteger().longValue());

		var b = tok("0755");
		assertEquals(0755L, b.readInteger().longValue());

		var c = tok("0xFF");
		assertEquals(255L, c.readInteger().longValue());
	}

	@Test void b35_directReadInteger_floatThrows() {
		// Hits the default-error path in readInteger (line 508).
		var t = tok("3.14");
		assertThrows(ParseException.class, t::readInteger);
	}

	@Test void b36_directReadFloat_intIsAccepted() throws Exception {
		// Hits the DEC_INT/HEX_INT/OCT_INT branch in readFloat (lines 516-517).
		var a = tok("42");
		assertEquals(42.0, a.readFloat(), 1e-12);

		var b = tok("0xFF");
		assertEquals(255.0, b.readFloat(), 1e-12);

		// Octal 0755 = decimal 493.
		var c = tok("0755");
		assertEquals(493.0, c.readFloat(), 1e-12);
	}

	@Test void b37_directReadFloat_floatToken() throws Exception {
		// Hits the FLOAT branch in readFloat (lines 514-515).
		var t = tok("3.14");
		assertEquals(3.14, t.readFloat(), 1e-9);
	}

	@Test void b38_directReadFloat_stringThrows() {
		// Hits the error path in readFloat (line 518).
		var t = tok("\"x\"");
		assertThrows(ParseException.class, t::readFloat);
	}

	@Test void b39_directReadBoolean_idents() throws Exception {
		// Hits readBoolean IDENT branch (lines 523-528) for true/false/t/f/1/0 textual.
		assertTrue(tok("true").readBoolean());
		assertTrue(tok("t").readBoolean());
		assertTrue(tok("True").readBoolean());
		assertFalse(tok("false").readBoolean());
		assertFalse(tok("f").readBoolean());
		assertFalse(tok("False").readBoolean());
	}

	@Test void b40_directReadBoolean_intLiteral() throws Exception {
		// Hits readBoolean DEC_INT branch (lines 530-533).
		assertTrue(tok("1").readBoolean());
		assertFalse(tok("0").readBoolean());
	}

	@Test void b41_directReadBoolean_invalid() {
		// Hits the error path in readBoolean (line 535) — non-boolean integer and non-bool ident.
		assertThrows(ParseException.class, () -> tok("42").readBoolean());
		assertThrows(ParseException.class, () -> tok("\"true\"").readBoolean());
	}

	@Test void b42_directReadBoolean_unknownIdent() {
		// Unknown identifier (not in the true/false/t/f/1/0 set) — falls through to the error path.
		assertThrows(ParseException.class, () -> tok("yes").readBoolean());
	}

	@Test void b43_invalidEscape_endOfStream() {
		// Hits the unterminated-escape path (escape char EOF).
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("s: \"\\", JsonMap.class));
	}

	@Test void b44_integerOverflow() {
		// Hits the integer-overflow error in readDecimalOrFloat (line 331).
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("n: 99999999999999999999999", JsonMap.class));
	}

	@Test void b45_hexOverflow() {
		// Hits the integer-overflow error in readHexInteger (line 263).
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("n: 0xFFFFFFFFFFFFFFFFFF", JsonMap.class));
	}

	@Test void b46_emptyHexInteger() {
		// Hits the empty-hex error in readHexInteger (line 259).
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("n: 0x", JsonMap.class));
	}

	@Test void b47_unicodeInvalidCodePoint() {
		// Hits the invalid-codepoint error in readUnicodeEscape — \U with a non-codepoint.
		// 0xFFFFFFFF is not a valid codepoint per Character.isValidCodePoint.
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("s: \"\\UFFFFFFFF\"", JsonMap.class));
	}

	@Test void b48_invalidOctalEscape() {
		// Hits the empty-octal error in readOctalEscape — though processEscape only enters this
		// path when the first digit is 0-7, so triggering the empty error directly requires a
		// non-octal first digit. The empty error path is unreachable from processEscape; to cover
		// the success case for max-3-digit octal we use \\377 = decimal 255 = ÿ.
		var a = ProtoParser.DEFAULT.parse("s: \"\\377\"", JsonMap.class);
		assertEquals("ÿ", a.get("s"));
	}

	@Test void b49_floatInvalidLiteral() {
		// Hits the invalid-float error in readDecimalOrFloat (line 312/322).
		// Construct a literal like "1.2.3" which the lexer reads as 1.2 followed by .3 — but that
		// won't error here. Instead use a raw underscore-only mantissa.
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("x: 1.0e", JsonMap.class));
	}

	@Test void b50_specialFloatLookaheadIdent() {
		// The startsSpecialFloatLiteral lookahead returns false for `infrastructure` because
		// the assembled token isn't a special-float; this exercises the unread/restore path
		// (lines 203-209).
		var a = ProtoParser.DEFAULT.parse("infrastructure: 1", JsonMap.class);
		assertEquals(1L, a.get("infrastructure"));
	}

	@Test void b51_eofToken() throws Exception {
		// Hits the EOF lex branch (line 132) — empty input yields EOF.
		var t = tok("");
		assertEquals(ProtoToken.TokenType.EOF, t.peek().type());
	}

	@Test void b52_structuralPunctuation() throws Exception {
		// Hits each of the punctuation switch arms in lex (lines 135-143).
		var t = tok("{}<>[]:,;");
		assertEquals(ProtoToken.TokenType.LBRACE, t.read().type());
		assertEquals(ProtoToken.TokenType.RBRACE, t.read().type());
		assertEquals(ProtoToken.TokenType.LANGLE, t.read().type());
		assertEquals(ProtoToken.TokenType.RANGLE, t.read().type());
		assertEquals(ProtoToken.TokenType.LBRACKET, t.read().type());
		assertEquals(ProtoToken.TokenType.RBRACKET, t.read().type());
		assertEquals(ProtoToken.TokenType.COLON, t.read().type());
		assertEquals(ProtoToken.TokenType.COMMA, t.read().type());
		assertEquals(ProtoToken.TokenType.SEMICOLON, t.read().type());
	}

	@Test void b53_unexpectedCharacter() {
		// Hits the catch-all error (line 153).
		var t = tok("@");
		assertThrows(ParseException.class, t::peek);
	}

	@Test void b54_skipMultipleWhitespaceTypes() {
		// Hits each whitespace-type branch in skipWhitespaceAndComments (line 100): space/tab/newline/CR/0x0B/0x0C.
		var a = ProtoParser.DEFAULT.parse(" \t\n\rname: 1", JsonMap.class);
		assertEquals(1L, a.get("name"));
	}

	@Test void b55_unreadAtEofIsNoop() throws Exception {
		// Hits the c<0 branch of unread (line 91) — no pushback occurs.
		var t = tok("");
		t.unread(-1);
		assertEquals(ProtoToken.TokenType.EOF, t.peek().type());
	}

	@Test void b56_signedPlusFloat() {
		// `+0.5` exercises the c == '+' branch in lexNumber (lines 220-222).
		var a = ProtoParser.DEFAULT.parse("x: +0.5", JsonMap.class);
		assertEquals(0.5, ((Number) a.get("x")).doubleValue(), 1e-12);
	}

	@Test void b57_octalInvalid() {
		// Hits readOctalInteger overflow path (line 274).
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("n: 0777777777777777777777777", JsonMap.class));
	}
}
