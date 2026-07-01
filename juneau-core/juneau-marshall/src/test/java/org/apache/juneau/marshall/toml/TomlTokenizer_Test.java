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
package org.apache.juneau.marshall.toml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class TomlTokenizer_Test extends TestBase {

	private static TomlTokenizer t(String s) {
		return new TomlTokenizer(new StringReader(s));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Constructor / null reader
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_nullReader() throws Exception {
		var tok = new TomlTokenizer(null);
		assertTrue(tok.isEof());
		assertEquals(-1, tok.peek());
		assertEquals(-1, tok.read());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// peek / read / unread / line / column tracking
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_peekReturnsSameCharTwice() throws Exception {
		var tok = t("ab");
		assertEquals('a', tok.peek());
		assertEquals('a', tok.peek());
		assertEquals('a', tok.read());
		assertEquals('b', tok.peek());
	}

	@Test
	void b02_lineColumnTracking() throws Exception {
		var tok = t("ab\ncd");
		assertEquals(1, tok.getLine());
		assertEquals(0, tok.getColumn());
		tok.read(); // a
		assertEquals(1, tok.getLine());
		assertEquals(1, tok.getColumn());
		tok.read(); // b
		assertEquals(2, tok.getColumn());
		tok.read(); // newline -> bumps line, resets column
		assertEquals(2, tok.getLine());
		assertEquals(0, tok.getColumn());
		tok.read(); // c
		assertEquals(1, tok.getColumn());
	}

	@Test
	void b03_unreadAndReread() throws Exception {
		var tok = t("xy");
		var c = tok.read();
		assertEquals('x', c);
		tok.unread('x');
		assertEquals('x', tok.read());
		assertEquals('y', tok.read());
	}

	@Test
	void b04_unreadNegativeIsNoOp() throws Exception {
		var tok = t("a");
		tok.unread(-1);
		assertEquals('a', tok.read());
		assertEquals(-1, tok.read());
	}

	@Test
	void b05_unreadNewlineUpdatesLineOnReread() throws Exception {
		// Pushback path advances line counter when re-reading a newline char
		var tok = t("a");
		tok.unread('\n');
		assertEquals(1, tok.getLine());
		tok.read(); // re-reads from pushback
		assertEquals(2, tok.getLine());
		assertEquals(0, tok.getColumn());
	}

	@Test
	void b06_isEofTrueAtEnd() throws Exception {
		var tok = t("a");
		assertFalse(tok.isEof());
		tok.read();
		assertTrue(tok.isEof());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readUntil
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_readUntilFindsStop() throws Exception {
		var tok = t("hello,world");
		assertEquals("hello", tok.readUntil(','));
		assertEquals('w', tok.read());
	}

	@Test
	void c02_readUntilMultipleStops() throws Exception {
		var tok = t("abc;rest");
		assertEquals("abc", tok.readUntil(',', ';'));
	}

	@Test
	void c03_readUntilHitsEof() throws Exception {
		var tok = t("noStopHere");
		assertEquals("noStopHere", tok.readUntil(','));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// skipWhitespace / skipWhitespaceAndNewlines / skipComment / skipWhitespaceAndComments / skipToNextLine
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_skipWhitespace() throws Exception {
		var tok = t("   \t  abc");
		tok.skipWhitespace();
		assertEquals('a', tok.peek());
	}

	@Test
	void d02_skipWhitespaceAndNewlines() throws Exception {
		var tok = t(" \t\n\r  abc");
		tok.skipWhitespaceAndNewlines();
		assertEquals('a', tok.peek());
	}

	@Test
	void d03_skipCommentDoesNothingWithoutHash() throws Exception {
		var tok = t("abc");
		tok.skipComment();
		assertEquals('a', tok.peek());
	}

	@Test
	void d04_skipCommentConsumesUntilNewline() throws Exception {
		var tok = t("# comment text\nrest");
		tok.skipComment();
		// Position should be at end of comment line; subsequent read advances to newline
		var c = tok.read();
		assertTrue(c == '\n' || c == '\r');
	}

	@Test
	void d05_skipWhitespaceAndComments() throws Exception {
		var tok = t("  # comment 1\n   # comment 2\nfoo");
		tok.skipWhitespaceAndComments();
		assertEquals('f', tok.peek());
	}

	@ParameterizedTest
	@ValueSource(strings = {"abc\r\nrest", "abc\nrest", "abc\rrest"})
	void d06_skipToNextLineStopsAtStartOfNextLine(String input) throws Exception {
		// CRLF, LF and CR-only line endings all advance to the first char of the next line.
		var tok = t(input);
		tok.skipToNextLine();
		assertEquals('r', tok.peek());
	}

	@Test
	void d09_skipToNextLineEof() throws Exception {
		var tok = t("abc");
		tok.skipToNextLine();
		assertTrue(tok.isEof());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readBareKey
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_readBareKeyEofReturnsNull() throws Exception {
		var tok = t("");
		assertNull(tok.readBareKey());
	}

	@Test
	void e02_readBareKeyAlpha() throws Exception {
		var tok = t("abc=1");
		assertEquals("abc", tok.readBareKey());
	}

	@Test
	void e03_readBareKeyAlphanumericUnderscore() throws Exception {
		var tok = t("a_b-c123 = 1");
		assertEquals("a_b-c123", tok.readBareKey());
	}

	@Test
	void e04_readBareKeyEmptyReturnsNull() throws Exception {
		var tok = t("=value");
		assertNull(tok.readBareKey());
	}

	@Test
	void e05_readBareKeyQuotedDelegatesToBasicString() throws Exception {
		var tok = t("\"my key\"=1");
		assertEquals("my key", tok.readBareKey());
	}

	@Test
	void e06_readBareKeyLiteralString() throws Exception {
		var tok = t("'my key'=1");
		assertEquals("my key", tok.readBareKey());
	}

	@Test
	void e07_readBareKeyMultiLineLiteralString() throws Exception {
		var tok = t("'''my key'''=1");
		assertEquals("my key", tok.readBareKey());
	}

	@Test
	void e08_readBareKeyTwoQuotesNotTriple() throws Exception {
		// peekTriple sees two ''  and a non-' char: empty literal followed by a quote begin.
		// readBareKey detects single ' (because peekTriple returns false after unread of two quotes)
		// and calls readLiteralString which reads up to next '.
		var tok = t("''");
		// Empty literal string
		assertEquals("", tok.readBareKey());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readBasicString and escapes
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_readBasicStringSimple() throws Exception {
		var tok = t("\"hello\"");
		assertEquals("hello", tok.readBasicString());
	}

	@Test
	void f03_readBasicStringAllEscapes() throws Exception {
		var tok = t("\"\\b\\t\\n\\f\\r\\\"\\\\\"");
		assertEquals("\b\t\n\f\r\"\\", tok.readBasicString());
	}

	@Test
	void f04_readBasicStringUnicodeShort() throws Exception {
		var tok = t("\"\\u0041\"");
		assertEquals("A", tok.readBasicString());
	}

	@Test
	void f05_readBasicStringUnicodeLong() throws Exception {
		var tok = t("\"\\U00000041\"");
		assertEquals("A", tok.readBasicString());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"hello",          // f02: missing opening quote
		"\"\\q\"",        // f06: invalid escape
		"\"hello",        // f07: unterminated
		"\"\\",           // f08: EOF after backslash
		"\"\\uXYZW\"",    // f09: invalid \\u unicode hex
		"\"\\u00\"",      // f10: short \\u unicode escape hits EOF
		"\"\\U00200000\"" // f11: \\U code point > 0x10FFFF (out of valid Unicode range)
	})
	void f02_readBasicStringInvalidInputsThrow(String input) {
		assertThrows(ParseException.class, t(input)::readBasicString);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readLiteralString
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_readLiteralString() throws Exception {
		var tok = t("'hello \\n world'");
		assertEquals("hello \\n world", tok.readLiteralString());
	}

	@Test
	void g02_readLiteralStringMissingOpenQuoteThrows() {
		var tok = t("hello");
		assertThrows(ParseException.class, tok::readLiteralString);
	}

	@Test
	void g03_readLiteralStringUnterminated() {
		var tok = t("'hello");
		assertThrows(ParseException.class, tok::readLiteralString);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readMultiLineBasicString
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_readMultiLineBasicStringPlain() throws Exception {
		var tok = t("line1\nline2\"\"\"");
		assertEquals("line1\nline2", tok.readMultiLineBasicString());
	}

	@Test
	void h02_readMultiLineBasicStringTrimsLeadingNewline() throws Exception {
		var tok = t("\nfirstLine\"\"\"");
		assertEquals("firstLine", tok.readMultiLineBasicString());
	}

	@Test
	void h03_readMultiLineBasicStringWithEmbeddedSingleQuote() throws Exception {
		// Embedded single quote should be passed through
		var tok = t("a\"b\"\"\"");
		assertEquals("a\"b", tok.readMultiLineBasicString());
	}

	@Test
	void h04_readMultiLineBasicStringWithEmbeddedDoubleQuote() throws Exception {
		// Two consecutive quotes (not three) should be appended literally
		var tok = t("a\"\"b\"\"\"");
		assertEquals("a\"\"b", tok.readMultiLineBasicString());
	}

	@Test
	void h05_readMultiLineBasicStringEscapedNewlineBackslashLineEnding() throws Exception {
		// Backslash at end of line continues with leading whitespace trim
		var tok = t("foo\\\n   bar\"\"\"");
		assertEquals("foobar", tok.readMultiLineBasicString());
	}

	@Test
	void h06_readMultiLineBasicStringEscapedCRLF() throws Exception {
		var tok = t("foo\\\r\n   bar\"\"\"");
		assertEquals("foobar", tok.readMultiLineBasicString());
	}

	@Test
	void h07_readMultiLineBasicStringWithEscapeSequences() throws Exception {
		var tok = t("hello \\\"world\\\"\"\"\"");
		assertEquals("hello \"world\"", tok.readMultiLineBasicString());
	}

	@Test
	void h08_readMultiLineBasicStringWithUnicodeEscape() throws Exception {
		var tok = t("\\u0041\"\"\"");
		assertEquals("A", tok.readMultiLineBasicString());
	}

	@Test
	void h09_readMultiLineBasicStringWithLongUnicodeEscape() throws Exception {
		var tok = t("\\U00000041\"\"\"");
		assertEquals("A", tok.readMultiLineBasicString());
	}

	@Test
	void h10_readMultiLineBasicStringUnterminated() {
		var tok = t("foo");
		assertThrows(ParseException.class, tok::readMultiLineBasicString);
	}

	@Test
	void h11_readMultiLineBasicStringInvalidEscape() {
		var tok = t("\\q\"\"\"");
		assertThrows(ParseException.class, tok::readMultiLineBasicString);
	}

	@Test
	void h12_readMultiLineBasicStringEscapesAllStandard() throws Exception {
		var tok = t("\\b\\t\\n\\f\\r\\\\\"\"\"");
		assertEquals("\b\t\n\f\r\\", tok.readMultiLineBasicString());
	}

	@Test
	void h13_readMultiLineBasicStringStartsWithCR() throws Exception {
		// First-newline trim path covers \r as well
		var tok = t("\rhello\"\"\"");
		assertEquals("hello", tok.readMultiLineBasicString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readMultiLineLiteralString
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_readMultiLineLiteralStringPlain() throws Exception {
		var tok = t("line1\nline2'''");
		assertEquals("line1\nline2", tok.readMultiLineLiteralString());
	}

	@Test
	void i02_readMultiLineLiteralStringTrimsLeadingNewline() throws Exception {
		var tok = t("\nfirstLine'''");
		assertEquals("firstLine", tok.readMultiLineLiteralString());
	}

	@Test
	void i03_readMultiLineLiteralStringTrimsLeadingCR() throws Exception {
		var tok = t("\rfirstLine'''");
		assertEquals("firstLine", tok.readMultiLineLiteralString());
	}

	@Test
	void i04_readMultiLineLiteralStringWithEmbeddedSingleQuote() throws Exception {
		var tok = t("a'b'''");
		assertEquals("a'b", tok.readMultiLineLiteralString());
	}

	@Test
	void i05_readMultiLineLiteralStringWithDoubleQuote() throws Exception {
		var tok = t("a''b'''");
		assertEquals("a''b", tok.readMultiLineLiteralString());
	}

	@Test
	void i06_readMultiLineLiteralStringPreservesBackslash() throws Exception {
		var tok = t("a\\nb'''");
		assertEquals("a\\nb", tok.readMultiLineLiteralString());
	}

	@Test
	void i07_readMultiLineLiteralStringUnterminated() {
		var tok = t("foo");
		assertThrows(ParseException.class, tok::readMultiLineLiteralString);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readInteger
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_readIntegerPositive() throws Exception {
		assertEquals(42L, t("42").readInteger());
	}

	@Test
	void j02_readIntegerNegative() throws Exception {
		assertEquals(-42L, t("-42").readInteger());
	}

	@Test
	void j03_readIntegerExplicitPlus() throws Exception {
		assertEquals(42L, t("+42").readInteger());
	}

	@Test
	void j04_readIntegerZero() throws Exception {
		assertEquals(0L, t("0").readInteger());
	}

	@Test
	void j05_readIntegerWithUnderscores() throws Exception {
		assertEquals(1000000L, t("1_000_000").readInteger());
	}

	@Test
	void j06_readIntegerHexUpper() throws Exception {
		assertEquals(255L, t("0xFF").readInteger());
	}

	@Test
	void j07_readIntegerHexLower() throws Exception {
		assertEquals(0xdeadL, t("0xdead").readInteger());
	}

	@Test
	void j08_readIntegerHexUppercaseX() throws Exception {
		assertEquals(255L, t("0XFF").readInteger());
	}

	@Test
	void j09_readIntegerHexNegative() throws Exception {
		assertEquals(-255L, t("-0xFF").readInteger());
	}

	@Test
	void j10_readIntegerHexUnderscores() throws Exception {
		assertEquals(0xFFFFFFFFL, t("0xFFFF_FFFF").readInteger());
	}

	@Test
	void j11_readIntegerOctal() throws Exception {
		assertEquals(0755L, t("0o755").readInteger());
	}

	@Test
	void j12_readIntegerOctalUpperO() throws Exception {
		assertEquals(0755L, t("0O755").readInteger());
	}

	@Test
	void j13_readIntegerOctalNegative() throws Exception {
		assertEquals(-0755L, t("-0o755").readInteger());
	}

	@Test
	void j14_readIntegerOctalUnderscores() throws Exception {
		assertEquals(0755L, t("0o7_5_5").readInteger());
	}

	@Test
	void j15_readIntegerBinary() throws Exception {
		assertEquals(10L, t("0b1010").readInteger());
	}

	@Test
	void j16_readIntegerBinaryUpperB() throws Exception {
		assertEquals(10L, t("0B1010").readInteger());
	}

	@Test
	void j17_readIntegerBinaryNegative() throws Exception {
		assertEquals(-10L, t("-0b1010").readInteger());
	}

	@Test
	void j18_readIntegerBinaryUnderscores() throws Exception {
		assertEquals(10L, t("0b10_10").readInteger());
	}

	@Test
	void j19_readIntegerEmptyHexThrows() {
		var tok = t("0x");
		assertThrows(ParseException.class, tok::readInteger);
	}

	@Test
	void j20_readIntegerEmptyOctalThrows() {
		var tok = t("0o");
		assertThrows(ParseException.class, tok::readInteger);
	}

	@Test
	void j21_readIntegerEmptyBinaryThrows() {
		var tok = t("0b");
		assertThrows(ParseException.class, tok::readInteger);
	}

	@Test
	void j22_readIntegerEmptyThrows() {
		var tok = t("");
		assertThrows(ParseException.class, tok::readInteger);
	}

	@Test
	void j23_readIntegerLeadingZerosThrows() {
		var tok = t("01");
		assertThrows(ParseException.class, tok::readInteger);
	}

	@Test
	void j24_readIntegerOverflowDecimal() {
		var tok = t("99999999999999999999");
		assertThrows(ParseException.class, tok::readInteger);
	}

	@Test
	void j25_readIntegerOverflowHex() {
		var tok = t("0xFFFFFFFFFFFFFFFFF");
		assertThrows(ParseException.class, tok::readInteger);
	}

	@Test
	void j26_readIntegerOverflowOctal() {
		var tok = t("0o7777777777777777777777");
		assertThrows(ParseException.class, tok::readInteger);
	}

	@Test
	void j27_readIntegerOverflowBinary() {
		var tok = t("0b" + "1".repeat(80));
		assertThrows(ParseException.class, tok::readInteger);
	}

	@Test
	void j28_readIntegerJustZeroFollowedByLetter() throws Exception {
		// "0a" - '0' then 'a' is not x/o/b, so unread '0', then readDecimalInteger reads "0"
		assertEquals(0L, t("0a").readInteger());
	}

	@Test
	void j29_readIntegerWithLeadingWhitespace() throws Exception {
		assertEquals(42L, t("  42").readInteger());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readFloat
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k01_readFloatBasic() throws Exception {
		assertEquals(3.14, (Double) t("3.14").readFloat(), 0.0001);
	}

	@Test
	void k02_readFloatNegative() throws Exception {
		assertEquals(-3.14, (Double) t("-3.14").readFloat(), 0.0001);
	}

	@Test
	void k03_readFloatPositive() throws Exception {
		assertEquals(3.14, (Double) t("+3.14").readFloat(), 0.0001);
	}

	@Test
	void k04_readFloatExponent() throws Exception {
		assertEquals(150.0, (Double) t("1.5e2").readFloat(), 0.0001);
	}

	@Test
	void k05_readFloatExponentUpperE() throws Exception {
		assertEquals(150.0, (Double) t("1.5E2").readFloat(), 0.0001);
	}

	@Test
	void k06_readFloatExponentNegative() throws Exception {
		assertEquals(0.015, (Double) t("1.5e-2").readFloat(), 0.0001);
	}

	@Test
	void k07_readFloatExponentExplicitPositive() throws Exception {
		assertEquals(150.0, (Double) t("1.5e+2").readFloat(), 0.0001);
	}

	@Test
	void k08_readFloatNoFraction() throws Exception {
		assertEquals(42.0, (Double) t("42").readFloat(), 0.0001);
	}

	@Test
	void k09_readFloatWithUnderscores() throws Exception {
		assertEquals(1234.5678, (Double) t("1_234.567_8").readFloat(), 0.0001);
	}

	@Test
	void k10_readFloatLeadingWhitespace() throws Exception {
		assertEquals(1.5, (Double) t("   1.5").readFloat(), 0.0001);
	}

	@Test
	void k11_readFloatEmptyThrows() {
		var tok = t("");
		assertThrows(ParseException.class, tok::readFloat);
	}

	@Test
	void k12_readFloatBareSignThrows() {
		var tok = t("-q");
		assertThrows(ParseException.class, tok::readFloat);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readSpecialFloat
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l01_specialInf() throws Exception {
		assertEquals(Double.POSITIVE_INFINITY, t("inf").readSpecialFloat());
	}

	@Test
	void l02_specialPosInfNotSupported() {
		// readSpecialFloat does not accept +inf (only "inf" without sign or "-inf"); // NOSONAR
		// note: the parser session has its own parseSpecialFloat that does accept "+inf".
		var tok = t("+inf");
		assertThrows(ParseException.class, tok::readSpecialFloat);
	}

	@Test
	void l03_specialNegInf() throws Exception {
		assertEquals(Double.NEGATIVE_INFINITY, t("-inf").readSpecialFloat());
	}

	@Test
	void l04_specialNan() throws Exception {
		assertTrue(Double.isNaN((Double) t("nan").readSpecialFloat()));
	}

	@Test
	void l05_specialPosNan() throws Exception {
		assertTrue(Double.isNaN((Double) t("+nan").readSpecialFloat()));
	}

	@Test
	void l06_specialNegNan() throws Exception {
		assertTrue(Double.isNaN((Double) t("-nan").readSpecialFloat()));
	}

	@Test
	void l07_specialInvalid() {
		var tok = t("bogus");
		assertThrows(ParseException.class, tok::readSpecialFloat);
	}

	@Test
	void l08_specialLeadingWhitespace() throws Exception {
		assertEquals(Double.POSITIVE_INFINITY, t("   inf").readSpecialFloat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readBoolean
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m01_booleanTrue() throws Exception {
		assertEquals(Boolean.TRUE, t("true").readBoolean());
	}

	@Test
	void m02_booleanFalse() throws Exception {
		assertEquals(Boolean.FALSE, t("false").readBoolean());
	}

	@Test
	void m03_booleanInvalid() {
		var tok = t("xyz");
		assertThrows(ParseException.class, tok::readBoolean);
	}

	@Test
	void m04_booleanWrongPrefix() {
		// starts with 't' but not "true"
		var tok = t("trxx");
		assertThrows(ParseException.class, tok::readBoolean);
	}

	@Test
	void m05_booleanWrongFalsePrefix() {
		var tok = t("flxxx");
		assertThrows(ParseException.class, tok::readBoolean);
	}

	@Test
	void m06_booleanLeadingWhitespace() throws Exception {
		assertEquals(Boolean.TRUE, t("  true").readBoolean());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// expect
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void n01_expectEofMidWordThrows() {
		var tok = t("tru");
		assertThrows(ParseException.class, tok::readBoolean);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readDateTime + parseDateTimeString
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void o01_readDateTimeOffset() throws Exception {
		var v = t("1979-05-27T07:32:00Z").readDateTime();
		assertTrue(v instanceof OffsetDateTime);
	}

	@Test
	void o02_readDateTimeOffsetExplicit() throws Exception {
		var v = t("1979-05-27T07:32:00+05:30").readDateTime();
		assertTrue(v instanceof OffsetDateTime);
		assertEquals(ZoneOffset.ofHoursMinutes(5, 30), ((OffsetDateTime) v).getOffset());
	}

	@Test
	void o03_readDateTimeLocal() throws Exception {
		var v = t("1979-05-27T07:32:00").readDateTime();
		assertTrue(v instanceof LocalDateTime);
	}

	@Test
	void o04_readDateTimeLocalSpaceSeparator() throws Exception {
		var v = t("1979-05-27 07:32:00").readDateTime();
		assertTrue(v instanceof LocalDateTime);
	}

	@Test
	void o05_readDateTimeLocalDate() throws Exception {
		var v = t("1979-05-27").readDateTime();
		assertTrue(v instanceof LocalDate);
	}

	@Test
	void o06_readDateTimeLocalTime() throws Exception {
		var v = t("07:32:00").readDateTime();
		assertTrue(v instanceof LocalTime);
	}

	@Test
	void o07_readDateTimeStopsAtComma() throws Exception {
		var tok = t("1979-05-27,extra");
		var v = tok.readDateTime();
		assertTrue(v instanceof LocalDate);
		assertEquals(',', tok.peek());
	}

	@Test
	void o08_readDateTimeEmptyThrows() {
		var tok = t("");
		assertThrows(ParseException.class, tok::readDateTime);
	}

	@ParameterizedTest
	@ValueSource(strings = {"1979-05-27\nrest", "1979-05-27 # comment", "1979-05-27]", "1979-05-27}"})
	void o09_readDateTimeStopsAtDelimiter(String input) throws Exception {
		// readDateTime terminates the date token at a newline, '#', ']' or '}' delimiter,
		// yielding a LocalDate for the leading date.
		var v = t(input).readDateTime();
		assertTrue(v instanceof LocalDate);
	}

	@Test
	void o13_parseDateTimeStringInvalidOffset() {
		assertThrows(ParseException.class, () -> TomlTokenizer.parseDateTimeString("1979-05-27T99:99:99Z"));
	}

	@Test
	void o14_parseDateTimeStringInvalidLocal() {
		assertThrows(ParseException.class, () -> TomlTokenizer.parseDateTimeString("1979-05-27T99:99:99"));
	}

	@Test
	void o15_parseDateTimeStringInvalidLocalDate() {
		assertThrows(ParseException.class, () -> TomlTokenizer.parseDateTimeString("9999-99-99"));
	}

	@Test
	void o16_parseDateTimeStringInvalidLocalTime() {
		assertThrows(ParseException.class, () -> TomlTokenizer.parseDateTimeString("99:99:99"));
	}

	@Test
	void o17_parseDateTimeStringNotRecognized() {
		assertThrows(ParseException.class, () -> TomlTokenizer.parseDateTimeString("nonsense"));
	}

	@Test
	void o18_parseDateTimeStringNegativeOffset() throws Exception {
		var v = TomlTokenizer.parseDateTimeString("1979-05-27T07:32:00-05:00");
		assertTrue(v instanceof OffsetDateTime);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// parseException carries line/column info
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void p01_parseExceptionFormat() {
		var tok = t("");
		var ex = tok.parseException("oops");
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("oops"));
		assertTrue(ex.getMessage().contains("line"));
		assertTrue(ex.getMessage().contains("column"));
	}
}
