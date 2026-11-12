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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.ExprParser.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link ExprParser} lexer and {@link ExprException}.
 */
@SuppressWarnings({
	"java:S5976" // Each lexer case asserts distinct token expectations and reads clearer as its own test
})
class ExprLexer_Test extends TestBase {

	private static List<Token> lex(String s) {
		return ExprParser.lex(s);
	}

	private static String types(String s) {
		var sb = new StringBuilder();
		for (var t : lex(s))
			sb.append(sb.isEmpty() ? "" : " ").append(t.type());
		return sb.toString();
	}

	@Test void a01_stringEscapedQuote() {
		var t = lex("'it''s'");
		assertEquals(TokenType.STRING, t.get(0).type());
		assertEquals("it's", t.get(0).value());
		assertEquals(0, t.get(0).pos());
		assertEquals(TokenType.END, t.get(1).type());
		assertEquals(7, t.get(1).pos());
	}

	@Test void a02_doubleQuotedString() {
		assertEquals("a\"b", lex("\"a\"\"b\"").get(0).value());
		assertEquals("x y", lex("\"x y\"").get(0).value());
	}

	@Test void a03_numbers() {
		assertEquals(12L, lex("12").get(0).value());
		assertEquals(1.0, lex("1.").get(0).value());
		assertEquals(0.5, lex(".5").get(0).value());
		assertEquals(1.5, lex("1.5").get(0).value());
		assertEquals(1000.0, lex("1e3").get(0).value());
		assertEquals(0.025, lex("2.5E-2").get(0).value());
		assertEquals(1.0E30, (Double)lex("1000000000000000000000000000000").get(0).value(), 1e20);
		assertEquals("NUMBER END", types("1."));
		assertEquals("NUMBER END", types(".5"));
	}

	@Test void a04_dottedPathIsNotNumber() {
		assertEquals("IDENT DOT IDENT END", types("a.b"));
		assertEquals("IDENT DOT IDENT DOT IDENT END", types("a.b.c"));
	}

	@Test void a05_varRef() {
		var t = lex("${C:x:y}");
		assertEquals(TokenType.VARREF, t.get(0).type());
		assertEquals("${C:x:y}", t.get(0).text());
		assertEquals(2, t.size());
		assertEquals("${P:${Q:z}}", lex("${P:${Q:z}}").get(0).text());
		assertEquals("VARREF END", types("${P:${Q:z}}"));
		assertEquals("${A:'}'}", lex("${A:'}'}").get(0).text());
		assertEquals("VARREF PLUS NUMBER END", types("${a} + 1"));
	}

	@Test void a06_quotedDollarIsPlainString() {
		var t = lex("'${x}'");
		assertEquals(TokenType.STRING, t.get(0).type());
		assertEquals("${x}", t.get(0).value());
	}

	@Test void a07_multiCharOperators() {
		assertEquals("AND OR ELVIS SAFE_DOT SELECT PROJECT END", types("&& || ?: ?. .?[ .!["));
		assertEquals("EQ NE LE GE LT GT END", types("== != <= >= < >"));
		assertEquals("QUESTION COLON NOT END", types("? : !"));
		assertEquals("PLUS MINUS STAR SLASH PERCENT LPAREN RPAREN LBRACKET RBRACKET COMMA END", types("+ - * / % ( ) [ ] ,"));
	}

	@Test void a08_keywords() {
		assertEquals("AND OR NOT MATCHES TRUE FALSE NULL END", types("and or not matches true false null"));
		assertEquals("IDENT END", types("android"));
		assertEquals("IDENT END", types("nullable"));
		assertEquals("OR", lex("or").get(0).type().name());
		assertEquals("or", lex("or").get(0).text());
	}

	@Test void a09_whitespaceAndPositions() {
		var t = lex("  a  +b");
		assertEquals(2, t.get(0).pos());
		assertEquals(5, t.get(1).pos());
		assertEquals(6, t.get(2).pos());
	}

	@Test void a10_callTokens() {
		assertEquals("IDENT LPAREN IDENT COMMA NUMBER RPAREN END", types("f(a, 1)"));
	}

	@Test void b01_unterminatedString() {
		var e = assertThrows(ExprException.class, () -> lex("a + 'abc"));
		assertEquals(4, e.getPos());
		assertTrue(e.getMessage().contains("at position 4"), e.getMessage());
	}

	@Test void b02_unterminatedVarRef() {
		var e = assertThrows(ExprException.class, () -> lex("1 + ${a:{b}"));
		assertEquals(4, e.getPos());
	}

	@Test void b03_unexpectedChar() {
		var e = assertThrows(ExprException.class, () -> lex("a # b"));
		assertEquals(2, e.getPos());
		e = assertThrows(ExprException.class, () -> lex("a & b"));
		assertEquals(2, e.getPos());
		e = assertThrows(ExprException.class, () -> lex("a | b"));
		assertEquals(2, e.getPos());
		e = assertThrows(ExprException.class, () -> lex("$x"));
		assertEquals(0, e.getPos());
	}

	@Test void c01_exceptionMessageCaret() {
		var e = new ExprException("bad", "a%s b", 2);
		assertEquals("bad at position 2:\na%s b\n  ^", e.getMessage());
		assertEquals("bad", e.getBaseMessage());
		assertEquals("a%s b", e.getSource());
	}

	@Test void c02_exceptionMultiLine() {
		var e = new ExprException("bad", "1 +\n2 # 3\n4", 6);
		assertEquals("bad at position 6:\n2 # 3\n  ^", e.getMessage());
	}

	@Test void c03_exceptionTab() {
		var e = new ExprException("bad", "\ta\t#", 3);
		assertEquals("bad at position 3:\n\ta\t#\n\t \t^", e.getMessage());
	}

	@Test void d01_positionsAndKinds() {
		var t = lex("${a} +b?:c");
		assertEquals(0, t.get(0).pos());
		assertEquals(TokenType.VARREF, t.get(0).type());
		assertEquals(5, t.get(1).pos());
		assertEquals(6, t.get(2).pos());
		assertEquals(7, t.get(3).pos());
		assertEquals(TokenType.ELVIS, t.get(3).type());
		assertEquals(9, t.get(4).pos());
	}

	@Test void d02_safeBracket() {
		assertEquals("IDENT SAFE_DOT LBRACKET END", types("a?.["));
	}

	@Test void d03_numberEdges() {
		var t = lex("1.5.3");
		assertEquals("NUMBER NUMBER END", types("1.5.3"));
		assertEquals(1.5, t.get(0).value());
		assertEquals(0.3, t.get(1).value());
		assertEquals("NUMBER DOT NUMBER END", types("1..2"));
		assertEquals("NUMBER IDENT END", types("1e"));
	}

	@Test void d04_emptyAndWhitespace() {
		var t = lex("");
		assertEquals(1, t.size());
		assertEquals(TokenType.END, t.get(0).type());
		assertEquals(0, t.get(0).pos());
		assertEquals("IDENT PLUS IDENT END", types("a\n\t+\r\nb"));
	}

	@Test void d05_strayCloseBrace() {
		var e = assertThrows(ExprException.class, () -> lex("${a}}"));
		assertEquals(4, e.getPos());
		assertTrue(e.getMessage().contains("Unexpected character '}'"), e.getMessage());
	}

	@Test void d06_overflowFallsBackToDouble() {
		assertEquals(Long.MAX_VALUE, lex("9223372036854775807").get(0).value());
		assertEquals(9223372036854775808.0, lex("9223372036854775808").get(0).value());
	}

	@Test void e01_varRefMidWordApostrophe() {
		// Inside ${...} a quote opens only at the start of a part, so it's / don't are plain text.
		assertEquals("${S:it's}", lex("${S:it's} == 'x'").get(0).text());
		assertEquals("VARREF EQ STRING END", types("${S:it's} == 'x'"));
		assertEquals("${RQ:don't:b}", lex("${RQ:don't:b}").get(0).text());
	}

	@Test void e02_varRefPartStartQuote() {
		assertEquals("${A:'x}y'}", lex("${A:'x}y'}").get(0).text());
		assertEquals("${A:k:'}'}", lex("${A:k:'}'}").get(0).text());
		assertEquals("${A:'a\\'}'}", lex("${A:'a\\'}'}").get(0).text());
	}

	@Test void e03_varRefUnterminated() {
		var e = assertThrows(ExprException.class, () -> lex("1 + ${A:'x}"));
		assertEquals(4, e.getPos());
		assertTrue(e.getMessage().contains("Unterminated ${"), e.getMessage());
	}

	@Test void e04_backslashIsNotAStringEscape() {
		// Pinned: SpEL strings have no backslash escape, so 'a\'b' is the string a\ then an unterminated string.
		var e = assertThrows(ExprException.class, () -> lex("'a\\'b'"));
		assertEquals(5, e.getPos());
		assertEquals("a\\", lex("'a\\'").get(0).value());
	}
}
