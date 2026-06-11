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
package org.apache.juneau.marshall.hocon;

import static org.apache.juneau.marshall.hocon.HoconTokenizer.TokenType.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link HoconTokenizer}.
 */
@SuppressWarnings({
	"java:S5976" // Explicit per-token-type tests are clearer and easier to debug than a single parameterized table; intentional.
})
class HoconTokenizer_Test extends TestBase {

	private static HoconTokenizer tokenizer(String input) {
		return new HoconTokenizer(new StringReader(input));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Constructor variants and line/column tracking
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_constructorWithBufferedReader() throws Exception {
		// Pass a BufferedReader directly to exercise the instanceof branch (line 166).
		var t = new HoconTokenizer(new BufferedReader(new StringReader("abc")));
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("abc", tok.stringValue());
	}

	@Test
	void a02_constructorWithPlainReader() throws Exception {
		var t = new HoconTokenizer(new StringReader("hello"));
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	@Test
	void a03_lineColumnTracking() throws Exception {
		var t = tokenizer("ab\ncd");
		assertEquals(1, t.getLine());
		assertEquals(1, t.getColumn());
		t.read(); // "ab" (an unquoted string token consumes "ab")
		// After reading "ab", line is still 1 (newline not consumed yet)
		assertEquals(1, t.getLine());
	}

	@Test
	void a04_columnAdvances() throws Exception {
		var t = tokenizer("foo");
		var startCol = t.getColumn();
		t.read();
		assertTrue(t.getColumn() > startCol);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// peek / read / readToken
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_peekReturnsSameToken() throws Exception {
		var t = tokenizer("foo");
		var p1 = t.peek();
		var p2 = t.peek();
		assertSame(p1, p2);
		var r = t.read();
		assertSame(p1, r);
	}

	@Test
	void b02_peekNoSkipReturnsCachedTokenIfAlreadyPeeked() throws Exception {
		var t = tokenizer("foo");
		var p = t.peekNoSkip();
		assertSame(p, t.peekNoSkip());
		assertSame(p, t.read());
	}

	@Test
	void b03_peekNoSkipDoesNotSkipWhitespace() throws Exception {
		var t = tokenizer("  foo");
		// peekNoSkip starts directly at the leading space and treats it as part of a string
		// (actually: leading space is part of unquoted forbidden chars for unquoted, but not handled
		// as a token here — it falls through to readUnquotedOrNumber which strips it).
		// More importantly: peekNoSkip does NOT call skipWhitespaceAndComments.
		// First char is ' ' — readTokenImmediate reads it, sees it's none of the structural
		// chars, so falls into readUnquotedOrNumber which builds an empty token and triggers
		// the IOException path.
		assertThrows(IOException.class, t::peekNoSkip);
	}

	@Test
	void b04_eofToken() throws Exception {
		var t = tokenizer("");
		assertEquals(EOF, t.read().type());
	}

	@Test
	void b05_eofAfterContent() throws Exception {
		var t = tokenizer("x");
		assertEquals(UNQUOTED_STRING, t.read().type());
		assertEquals(EOF, t.read().type());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Newline handling
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_newlineLFViaPeekNoSkip() throws Exception {
		// NEWLINE tokens are only surfaced via peekNoSkip; read() always skips whitespace+newlines first
		var t = tokenizer("\nfoo");
		assertEquals(NEWLINE, t.peekNoSkip().type());
		assertEquals(NEWLINE, t.read().type());
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	@Test
	void c02_newlineCRViaPeekNoSkip() throws Exception {
		var t = tokenizer("\rfoo");
		assertEquals(NEWLINE, t.peekNoSkip().type());
	}

	@Test
	void c03_newlineCRLFViaPeekNoSkip() throws Exception {
		// CR followed by LF is a single newline token
		var t = tokenizer("\r\nfoo");
		assertEquals(NEWLINE, t.peekNoSkip().type());
		assertEquals(NEWLINE, t.read().type());
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Comment handling: # and //
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_hashComment() throws Exception {
		var t = tokenizer("# comment\nx");
		t.skipWhitespaceAndComments();
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	@Test
	void d02_slashComment() throws Exception {
		var t = tokenizer("// line comment\ny");
		t.skipWhitespaceAndComments();
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	@Test
	void d03_hashCommentEndsAtEOF() throws Exception {
		// EOF inside line comment (no newline)
		var t = tokenizer("# trailing comment with no newline");
		t.skipWhitespaceAndComments();
		assertEquals(EOF, t.read().type());
	}

	@Test
	void d04_slashCommentEndsAtEOF() throws Exception {
		var t = tokenizer("// trailing comment");
		t.skipWhitespaceAndComments();
		assertEquals(EOF, t.read().type());
	}

	@Test
	void d05_hashCommentEndsAtCR() throws Exception {
		// CR ends a line comment
		var t = tokenizer("# comment\rx");
		t.skipWhitespaceAndComments();
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	@Test
	void d06_hashCommentEndsAtCRLF() throws Exception {
		// CRLF: comment terminator consumes both
		var t = tokenizer("# comment\r\nx");
		t.skipWhitespaceAndComments();
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	@Test
	void d07_singleSlashIsNotComment() throws Exception {
		// '/' alone (not //) starts an unquoted string
		var t = tokenizer("/path/to/thing");
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("/path/to/thing", tok.stringValue());
	}

	@Test
	void d08_skipWhitespaceWithTabsAndSpaces() throws Exception {
		var t = tokenizer(" \t  \r\n\t foo");
		t.skipWhitespaceAndComments();
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	@Test
	void d09_skipWhitespaceNoopWhenTokenCached() throws Exception {
		// Once peek caches a token, skipWhitespaceAndComments must be a no-op
		var t = tokenizer("foo  \nbar");
		t.peek(); // caches "foo"
		t.skipWhitespaceAndComments();
		// Read foo first
		assertEquals(UNQUOTED_STRING, t.read().type());
		// The whitespace+newline that followed must be intact
		t.skipWhitespaceAndCommentsExceptNewlines();
		assertEquals(NEWLINE, t.peekNoSkip().type());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// skipWhitespaceAndCommentsExceptNewlines
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_skipExceptNewlinesStopsAtLF() throws Exception {
		var t = tokenizer("   \nfoo");
		t.skipWhitespaceAndCommentsExceptNewlines();
		assertEquals(NEWLINE, t.peekNoSkip().type());
	}

	@Test
	void e02_skipExceptNewlinesStopsAtCR() throws Exception {
		var t = tokenizer("   \rfoo");
		t.skipWhitespaceAndCommentsExceptNewlines();
		assertEquals(NEWLINE, t.peekNoSkip().type());
	}

	@Test
	void e03_skipExceptNewlinesEofAfterSpaces() throws Exception {
		var t = tokenizer("    ");
		t.skipWhitespaceAndCommentsExceptNewlines();
		assertEquals(EOF, t.read().type());
	}

	@Test
	void e04_skipExceptNewlinesHashCommentStopsBeforeNewline() throws Exception {
		// Hash line comment without trailing newline
		var t = tokenizer("# comment");
		t.skipWhitespaceAndCommentsExceptNewlines();
		assertEquals(EOF, t.read().type());
	}

	@Test
	void e05_skipExceptNewlinesHashCommentBeforeNewline() throws Exception {
		// The line comment ends at the newline; the newline must remain
		var t = tokenizer("# comment\nfoo");
		t.skipWhitespaceAndCommentsExceptNewlines();
		assertEquals(NEWLINE, t.peekNoSkip().type());
	}

	@Test
	void e06_skipExceptNewlinesSlashComment() throws Exception {
		var t = tokenizer("// hi\nfoo");
		t.skipWhitespaceAndCommentsExceptNewlines();
		assertEquals(NEWLINE, t.peekNoSkip().type());
	}

	@Test
	void e07_skipExceptNewlinesTabsAndSpacesOnly() throws Exception {
		var t = tokenizer("\t   foo");
		t.skipWhitespaceAndCommentsExceptNewlines();
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	@Test
	void e08_skipExceptNewlinesSingleSlashNotComment() throws Exception {
		// A solitary '/' (no second '/') is not a comment — skip leaves it for tokenizer
		var t = tokenizer("  /path");
		t.skipWhitespaceAndCommentsExceptNewlines();
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("/path", tok.stringValue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Quoted strings
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_doubleQuotedString() throws Exception {
		var t = tokenizer("\"hello world\"");
		var tok = t.read();
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("hello world", tok.stringValue());
	}

	@Test
	void f02_singleQuotedString() throws Exception {
		var t = tokenizer("'hello world'");
		var tok = t.read();
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("hello world", tok.stringValue());
	}

	@Test
	void f03_quotedStringEscapes() throws Exception {
		var t = tokenizer("\"a\\tb\\nc\\rd\\fe\\bf\\\\g\\\"h\\/i\"");
		var tok = t.read();
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("a\tb\nc\rd\fe\bf\\g\"h/i", tok.stringValue());
	}

	@Test
	void f04_quotedStringSingleQuoteEscape() throws Exception {
		var t = tokenizer("\"a\\'b\"");
		var tok = t.read();
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("a'b", tok.stringValue());
	}

	@Test
	void f05_quotedStringUnknownEscapeFallsThrough() throws Exception {
		// Unknown escapes default to passing the char through as-is
		var t = tokenizer("\"a\\zb\"");
		var tok = t.read();
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("azb", tok.stringValue());
	}

	@Test
	void f06_quotedStringUnicodeEscape() throws Exception {
		var t = tokenizer("\"\\u0041\\u00E9\"");
		var tok = t.read();
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("Aé", tok.stringValue());
	}

	@Test
	void f07_quotedStringUnicodeEscapeMixedCase() throws Exception {
		var t = tokenizer("\"\\u00aB\\u00CD\"");
		var tok = t.read();
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("«Í", tok.stringValue());
	}

	@Test
	void f08_quotedStringInvalidUnicodeThrows() {
		var t = tokenizer("\"\\uXYZW\"");
		assertThrows(IOException.class, t::read);
	}

	@Test
	void f09_quotedStringUnicodeEofThrows() {
		var t = tokenizer("\"\\u00\"");
		assertThrows(IOException.class, t::read);
	}

	@Test
	void f10_quotedStringUnterminatedSwallowsEof() throws Exception {
		// readQuotedString just stops at EOF without throwing
		var t = tokenizer("\"hello");
		var tok = t.read();
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("hello", tok.stringValue());
	}

	@Test
	void f11_quotedStringEofAfterBackslash() throws Exception {
		// Backslash at EOF is silently dropped (c < 0 branch)
		var t = tokenizer("\"abc\\");
		var tok = t.read();
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("abc", tok.stringValue());
	}

	@Test
	void f12_publicReadQuotedStringNoOpenQuoteReturnsEmpty() throws Exception {
		// readQuotedString returns "" when first char is not a quote
		var t = tokenizer("abc");
		assertEquals("", t.readQuotedString());
	}

	@Test
	void f13_publicReadQuotedStringWithSingleQuote() throws Exception {
		var t = tokenizer("'foo'");
		assertEquals("foo", t.readQuotedString());
	}

	@Test
	void f14_publicReadQuotedStringWithDoubleQuote() throws Exception {
		var t = tokenizer("\"foo\"");
		assertEquals("foo", t.readQuotedString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Triple-quoted strings
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_tripleQuoted() throws Exception {
		var t = tokenizer("\"\"\"line1\nline2\"\"\"");
		var tok = t.read();
		assertEquals(TRIPLE_QUOTED, tok.type());
		assertEquals("line1\nline2", tok.stringValue());
	}

	@Test
	void h02_tripleQuotedNoEscapeProcessing() throws Exception {
		// Triple-quoted strings pass escape sequences through verbatim
		var t = tokenizer("\"\"\"a\\nb\"\"\"");
		var tok = t.read();
		assertEquals(TRIPLE_QUOTED, tok.type());
		assertEquals("a\\nb", tok.stringValue());
	}

	@Test
	void h03_tripleQuotedTrimsTrailingNewline() throws Exception {
		// Trailing newline before closing """ is stripped
		var t = tokenizer("\"\"\"hello\n\"\"\"");
		var tok = t.read();
		assertEquals(TRIPLE_QUOTED, tok.type());
		assertEquals("hello", tok.stringValue());
	}

	@Test
	void h04_tripleQuotedNormalizesCRLF() throws Exception {
		// CRLF is normalized to \n
		var t = tokenizer("\"\"\"a\r\nb\"\"\"");
		var tok = t.read();
		assertEquals(TRIPLE_QUOTED, tok.type());
		assertEquals("a\nb", tok.stringValue());
	}

	@Test
	void h05_tripleQuotedDropsLoneCR() throws Exception {
		// Lone CR (not followed by LF) is dropped — the loop's "else if (c != '\r')" branch is the only path // NOSONAR
		// that emits chars, and it skips '\r'. The post-return replace("\r","\n") has nothing to operate on.
		var t = tokenizer("\"\"\"a\rb\"\"\"");
		var tok = t.read();
		assertEquals(TRIPLE_QUOTED, tok.type());
		assertEquals("ab", tok.stringValue());
	}

	@Test
	void h06_tripleQuotedSingleQuoteInside() throws Exception {
		// Single internal " is appended literally
		var t = tokenizer("\"\"\"a\"b\"\"\"");
		var tok = t.read();
		assertEquals(TRIPLE_QUOTED, tok.type());
		assertEquals("a\"b", tok.stringValue());
	}

	@Test
	void h07_tripleQuotedTwoQuotesInside() throws Exception {
		// Two consecutive internal " are appended literally
		var t = tokenizer("\"\"\"a\"\"b\"\"\"");
		var tok = t.read();
		assertEquals(TRIPLE_QUOTED, tok.type());
		assertEquals("a\"\"b", tok.stringValue());
	}

	@Test
	void h08_tripleQuotedEmpty() throws Exception {
		var t = tokenizer("\"\"\"\"\"\"");
		var tok = t.read();
		assertEquals(TRIPLE_QUOTED, tok.type());
		assertEquals("", tok.stringValue());
	}

	@Test
	void h09_tripleQuotedUnterminatedReturnsAccumulatedText() throws Exception {
		// readTripleQuotedString just returns whatever was accumulated when EOF hits before closing """
		// This is read directly via the public API.
		var t = tokenizer("hello world");
		assertEquals("hello world", t.readTripleQuotedString());
	}

	@Test
	void h10_publicReadTripleQuotedStringDropsLoneCR() throws Exception {
		// Same observation as h05: lone CR is dropped during accumulation
		var t = tokenizer("a\rb\"\"\"");
		assertEquals("ab", t.readTripleQuotedString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readUnquotedString public API
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_readUnquotedStringSimple() throws Exception {
		var t = tokenizer("foobar=");
		assertEquals("foobar", t.readUnquotedString());
	}

	@Test
	void i02_readUnquotedStringEof() throws Exception {
		var t = tokenizer("");
		assertEquals("", t.readUnquotedString());
	}

	@Test
	void i03_readUnquotedStringStopsAtForbiddenChar() throws Exception {
		var t = tokenizer("abc[def");
		assertEquals("abc", t.readUnquotedString());
	}

	@Test
	void i04_readUnquotedStringTrims() throws Exception {
		// readUnquotedString trims; but the loop stops at whitespace which is forbidden
		var t = tokenizer("abc def");
		assertEquals("abc", t.readUnquotedString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Substitutions: ${var}, ${?var}, malformed
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_substitution() throws Exception {
		var t = tokenizer("${var}");
		var tok = t.read();
		assertEquals(SUBSTITUTION, tok.type());
		assertEquals("var", tok.stringValue());
	}

	@Test
	void j02_optSubstitution() throws Exception {
		var t = tokenizer("${?var}");
		var tok = t.read();
		assertEquals(OPT_SUBSTITUTION, tok.type());
		assertEquals("var", tok.stringValue());
	}

	@Test
	void j03_substitutionWithDots() throws Exception {
		var t = tokenizer("${a.b.c}");
		var tok = t.read();
		assertEquals(SUBSTITUTION, tok.type());
		assertEquals("a.b.c", tok.stringValue());
	}

	@Test
	void j04_substitutionTrimsInner() throws Exception {
		// Internal whitespace breaks the substitution path read; the closing `}` after whitespace is fine
		var t = tokenizer("${var }");
		var tok = t.read();
		// The space breaks out of the loop; then we see if c == '}' but we have ' ' so it falls through to the malformed path
		// Actually: the loop reads chars until '}', space, tab, \n, \r. On encountering ' ' we break. c is still ' '.
		// Then the check `if (c == '}')` is false, so it returns Token.string("${" + path). // NOSONAR
		// So the result is an UNQUOTED_STRING token, not a substitution.
		assertEquals(UNQUOTED_STRING, tok.type());
	}

	@Test
	void j05_substitutionMalformedNoClose() throws Exception {
		// EOF inside substitution
		var t = tokenizer("${incomplete");
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertTrue(tok.stringValue().startsWith("${"));
	}

	@Test
	void j06_optSubstitutionMalformed() throws Exception {
		var t = tokenizer("${?incomplete");
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertTrue(tok.stringValue().startsWith("${?"));
	}

	@Test
	void j07_substitutionWithNewlineInside() throws Exception {
		// Newline ends inner read; `}` not seen — malformed
		var t = tokenizer("${var\n}");
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
	}

	@Test
	void j08_substitutionWithTabInside() throws Exception {
		var t = tokenizer("${var\t}");
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
	}

	@Test
	void j09_substitutionWithCRInside() throws Exception {
		var t = tokenizer("${var\r}");
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
	}

	@Test
	void j10_dollarWithoutBrace() throws Exception {
		// '$' is in UNQUOTED_FORBIDDEN — dollar without brace is an orphan and triggers IOException
		var t = tokenizer("$abc");
		assertThrows(IOException.class, t::read);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Numbers: integer / decimal / scientific / sign
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k01_numberInteger() throws Exception {
		var t = tokenizer("42");
		var tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(42, tok.numberValue().intValue());
	}

	@Test
	void k02_numberNegative() throws Exception {
		var t = tokenizer("-42");
		var tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(-42, tok.numberValue().intValue());
	}

	@Test
	void k03_numberDecimal() throws Exception {
		var t = tokenizer("3.14");
		var tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(3.14, tok.numberValue().doubleValue(), 0.001);
	}

	@Test
	void k04_numberScientificLowerE() throws Exception {
		var t = tokenizer("1e10");
		var tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(1e10, tok.numberValue().doubleValue(), 0.001);
	}

	@Test
	void k05_numberScientificUpperE() throws Exception {
		var t = tokenizer("1E10");
		var tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(1e10, tok.numberValue().doubleValue(), 0.001);
	}

	@Test
	void k06_numberScientificSign() throws Exception {
		var t = tokenizer("1e-5");
		var tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(1e-5, tok.numberValue().doubleValue(), 1e-9);
	}

	@Test
	void k07_numberZero() throws Exception {
		var t = tokenizer("0");
		var tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(0, tok.numberValue().intValue());
	}

	@Test
	void k08_numberLong() throws Exception {
		// Number too large for Integer falls through to Long
		var t = tokenizer("9999999999");
		var tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(9999999999L, tok.numberValue().longValue());
	}

	@Test
	void k09_numberOverflowFallsBackToString() throws Exception {
		// Number too large for Long falls back to UNQUOTED_STRING
		var t = tokenizer("99999999999999999999999");
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
	}

	@Test
	void k10_numberWithUnderscoresIsString() throws Exception {
		// HOCON does not allow underscore separators in numbers — falls through to UNQUOTED_STRING
		var t = tokenizer("1_000");
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("1_000", tok.stringValue());
	}

	@Test
	void k11_numberLeadingZerosIsString() throws Exception {
		// "01" doesn't match the NUMBER pattern (which forbids leading zeros) — falls through to UNQUOTED_STRING
		var t = tokenizer("01");
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
	}

	@Test
	void k12_numberPlusSignIsString() throws Exception {
		// '+' is not in the regex; "+5" can't be tokenized as a leading char anyway since '+' alone goes
		// to the +/+ branch, and "+5" is "+ then 5"; the '+' lacks '=' continuation so we fall to the
		// unquoted path which throws.
		var t = tokenizer("+5");
		assertThrows(IOException.class, t::read);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Special tokens: true, false, null, =, :, ,, [], {}, +=
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l01_trueToken() throws Exception {
		assertEquals(TRUE, tokenizer("true").read().type());
	}

	@Test
	void l02_falseToken() throws Exception {
		assertEquals(FALSE, tokenizer("false").read().type());
	}

	@Test
	void l03_nullToken() throws Exception {
		assertEquals(NULL, tokenizer("null").read().type());
	}

	@Test
	void l04_structuralTokens() throws Exception {
		var t = tokenizer("{}[]:=,");
		assertEquals(LBRACE, t.read().type());
		assertEquals(RBRACE, t.read().type());
		assertEquals(LBRACKET, t.read().type());
		assertEquals(RBRACKET, t.read().type());
		assertEquals(COLON, t.read().type());
		assertEquals(EQUALS, t.read().type());
		assertEquals(COMMA, t.read().type());
	}

	@Test
	void l05_plusEquals() throws Exception {
		assertEquals(PLUS_EQUALS, tokenizer("+=").read().type());
	}

	@Test
	void l06_equalsFollowedByPlus() throws Exception {
		// '=' always EQUALS; '=+' must not be confused with partial '+='.
		var t = tokenizer("=+");
		assertEquals(EQUALS, t.read().type());
		// orphan '+' has no continuation
		assertThrows(IOException.class, t::read);
	}

	@Test
	void l07_equalsAndPlusEqualsDontInterfere() throws Exception {
		var t = tokenizer("= +=");
		assertEquals(EQUALS, t.read().type());
		assertEquals(PLUS_EQUALS, t.read().type());
		assertEquals(EOF, t.read().type());
	}

	@Test
	void l08_plusWithoutEqualsThrows() {
		// '+' alone is in UNQUOTED_FORBIDDEN and has no continuation
		var t = tokenizer("+");
		assertThrows(IOException.class, t::read);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Token record factory functions (cover the static factory methods)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m01_tokenRecordOf() {
		var tok = HoconTokenizer.Token.of(LBRACE);
		assertEquals(LBRACE, tok.type());
		assertNull(tok.stringValue());
		assertNull(tok.numberValue());
	}

	@Test
	void m02_tokenRecordString() {
		var tok = HoconTokenizer.Token.string("hi");
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("hi", tok.stringValue());
	}

	@Test
	void m03_tokenRecordQuoted() {
		var tok = HoconTokenizer.Token.quoted("hi");
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("hi", tok.stringValue());
	}

	@Test
	void m04_tokenRecordTripleQuoted() {
		var tok = HoconTokenizer.Token.tripleQuoted("hi");
		assertEquals(TRIPLE_QUOTED, tok.type());
		assertEquals("hi", tok.stringValue());
	}

	@Test
	void m05_tokenRecordNumber() {
		var tok = HoconTokenizer.Token.number(42);
		assertEquals(NUMBER, tok.type());
		assertEquals(42, tok.numberValue().intValue());
	}

	@Test
	void m06_tokenRecordSubstitution() {
		var sub = HoconTokenizer.Token.substitution("a", false);
		var opt = HoconTokenizer.Token.substitution("a", true);
		assertEquals(SUBSTITUTION, sub.type());
		assertEquals(OPT_SUBSTITUTION, opt.type());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// peekNoSkip and skipWhitespaceAndComments interaction
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void n01_skipIsNoopWhileTokenCached() throws Exception {
		var t = tokenizer("]\nx");
		assertEquals(RBRACKET, t.peekNoSkip().type());
		t.skipWhitespaceAndComments(); // must be no-op
		assertEquals(RBRACKET, t.read().type());
		assertEquals(NEWLINE, t.peekNoSkip().type());
	}

	@Test
	void n02_skipStillWorksWhenNothingCached() throws Exception {
		var t = tokenizer("   # comment\n   z");
		t.skipWhitespaceAndComments();
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("z", tok.stringValue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readQuotedString public method - direct invocation
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void o01_readQuotedStringDirectDoubleQuote() throws Exception {
		var t = tokenizer("\"abc\"");
		assertEquals("abc", t.readQuotedString());
	}

	@Test
	void o02_readQuotedStringDirectSingleQuote() throws Exception {
		var t = tokenizer("'abc'");
		assertEquals("abc", t.readQuotedString());
	}

	@Test
	void o03_readQuotedStringDirectMissingQuoteReturnsEmpty() throws Exception {
		var t = tokenizer("xyz");
		assertEquals("", t.readQuotedString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Multi-line / whitespace handling between tokens
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void p01_multipleNewlinesAreSkippedByRead() throws Exception {
		// read() invokes skipWhitespaceAndComments which collapses consecutive newlines into nothing
		var t = tokenizer("\n\n\n");
		assertEquals(EOF, t.read().type());
	}

	@Test
	void p02_keyValuePairBasic() throws Exception {
		var t = tokenizer("foo = bar");
		assertEquals(UNQUOTED_STRING, t.read().type());
		assertEquals(EQUALS, t.read().type());
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	@Test
	void p03_keyValuePairColon() throws Exception {
		var t = tokenizer("foo : bar");
		assertEquals(UNQUOTED_STRING, t.read().type());
		assertEquals(COLON, t.read().type());
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	@Test
	void p04_arrayLiteral() throws Exception {
		var t = tokenizer("[1, 2, 3]");
		assertEquals(LBRACKET, t.read().type());
		assertEquals(NUMBER, t.read().type());
		assertEquals(COMMA, t.read().type());
		assertEquals(NUMBER, t.read().type());
		assertEquals(COMMA, t.read().type());
		assertEquals(NUMBER, t.read().type());
		assertEquals(RBRACKET, t.read().type());
	}

	@Test
	void p05_objectLiteral() throws Exception {
		var t = tokenizer("{a: 1}");
		assertEquals(LBRACE, t.read().type());
		assertEquals(UNQUOTED_STRING, t.read().type());
		assertEquals(COLON, t.read().type());
		assertEquals(NUMBER, t.read().type());
		assertEquals(RBRACE, t.read().type());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// EOF inside line comment with CRLF terminator (covers stray '\r' after content)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void q01_lineCommentTerminatedByCR() throws Exception {
		// Line comment ends at CR; subsequent token should be reachable
		var t = tokenizer("# foo\r");
		t.skipWhitespaceAndComments();
		assertEquals(EOF, t.read().type());
	}

	@Test
	void q02_lineCommentMixedSlashAndHash() throws Exception {
		var t = tokenizer("# 1\n// 2\n# 3\nx");
		t.skipWhitespaceAndComments();
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("x", tok.stringValue());
	}
}
