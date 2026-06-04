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
package org.apache.juneau.hocon;

import static org.apache.juneau.hocon.HoconTokenizer.TokenType.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.TestBase;
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

	@Test
	void g01_unquotedString() throws Exception {
		var t = tokenizer("hello");
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("hello", tok.stringValue());
	}

	@Test
	void g02_quotedString() throws Exception {
		var t = tokenizer("\"hello world\"");
		var tok = t.read();
		assertEquals(QUOTED_STRING, tok.type());
		assertEquals("hello world", tok.stringValue());
	}

	@Test
	void g03_tripleQuoted() throws Exception {
		var t = tokenizer("\"\"\"line1\nline2\"\"\"");
		var tok = t.read();
		assertEquals(TRIPLE_QUOTED, tok.type());
		assertEquals("line1\nline2", tok.stringValue());
	}

	@Test
	void g04_number() throws Exception {
		var t = tokenizer("42");
		var tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(42, tok.numberValue().intValue());

		t = tokenizer("3.14");
		tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(3.14, tok.numberValue().doubleValue(), 0.001);
	}

	@Test
	void g05_booleanNull() throws Exception {
		var t = tokenizer("true");
		assertEquals(TRUE, t.read().type());

		t = tokenizer("false");
		assertEquals(FALSE, t.read().type());

		t = tokenizer("null");
		assertEquals(NULL, t.read().type());
	}

	@Test
	void g06_structural() throws Exception {
		var t = tokenizer("{}[]:=,");
		assertEquals(LBRACE, t.read().type());
		assertEquals(RBRACE, t.read().type());
		assertEquals(LBRACKET, t.read().type());
		assertEquals(RBRACKET, t.read().type());
		assertEquals(COLON, t.read().type());
		assertEquals(EQUALS, t.read().type());
		assertEquals(COMMA, t.read().type());
		assertEquals(EOF, t.read().type());
	}

	@Test
	void g07_plusEquals() throws Exception {
		var t = tokenizer("+=");
		var tok = t.read();
		assertEquals(PLUS_EQUALS, tok.type());
		assertEquals(EOF, t.read().type());
	}

	@Test
	void g08_substitution() throws Exception {
		var t = tokenizer("${var}");
		var tok = t.read();
		assertEquals(SUBSTITUTION, tok.type());
		assertEquals("var", tok.stringValue());
	}

	@Test
	void g09_optSubstitution() throws Exception {
		var t = tokenizer("${?var}");
		var tok = t.read();
		assertEquals(OPT_SUBSTITUTION, tok.type());
		assertEquals("var", tok.stringValue());
	}

	@Test
	void g10_hashComment() throws Exception {
		var t = tokenizer("# comment\nx");
		t.skipWhitespaceAndComments();
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("x", tok.stringValue());
	}

	@Test
	void g11_slashComment() throws Exception {
		var t = tokenizer("// comment\ny");
		t.skipWhitespaceAndComments();
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("y", tok.stringValue());
	}

	@Test
	void g12_equalsVsColon() throws Exception {
		var t = tokenizer("a = b");
		t.skipWhitespaceAndComments();
		assertEquals(UNQUOTED_STRING, t.read().type());
		t.skipWhitespaceAndComments();
		assertEquals(EQUALS, t.read().type());
		t.skipWhitespaceAndComments();
		assertEquals(UNQUOTED_STRING, t.read().type());

		t = tokenizer("a: b");
		t.skipWhitespaceAndComments();
		assertEquals(UNQUOTED_STRING, t.read().type());
		t.skipWhitespaceAndComments();
		assertEquals(COLON, t.read().type());
		t.skipWhitespaceAndComments();
		assertEquals(UNQUOTED_STRING, t.read().type());
	}

	// Regression test for the '=+' lookahead bug (FINISHED-137).
	// '=' is always EQUALS regardless of the following character; the '+=' (PLUS_EQUALS)
	// operator is handled by a separate branch that reads '+' first, so '=+' is never
	// confused with a partial '+='.
	@Test
	void g13_equalsFollowedByPlus() throws Exception {
		// '=' when the next char is '+' must still emit EQUALS, not throw.
		var t = tokenizer("=+");
		assertEquals(EQUALS, t.read().type());
		// '+' alone is in UNQUOTED_FORBIDDEN and has no continuation; reading it throws.
		assertThrows(IOException.class, t::read);
	}

	// Confirm '+=', '=', and their interleaving all tokenize independently after the fix.
	@Test
	void g14_equalsAndPlusEqualsDontInterfere() throws Exception {
		var t = tokenizer("= +=");
		assertEquals(EQUALS, t.read().type());
		t.skipWhitespaceAndComments();
		assertEquals(PLUS_EQUALS, t.read().type());
		assertEquals(EOF, t.read().type());
	}

	// Structural invariant (work item 135): skipWhitespaceAndComments is a no-op while a token is
	// cached.  peekNoSkip() reads a structural token and leaves the underlying reader positioned
	// AFTER it.  A subsequent skipWhitespaceAndComments() must NOT advance past the cached token and
	// swallow the whitespace/newline that follows it — that whitespace belongs to the enclosing
	// scope (e.g. an in-array element separator).
	@Test
	void g15_skipIsNoopWhileTokenCached() throws Exception {
		var t = tokenizer("]\nx");
		// peekNoSkip caches RBRACKET; the reader is now positioned at the '\n' that follows ']'.
		assertEquals(RBRACKET, t.peekNoSkip().type());
		// Must be a no-op: the trailing '\n' must survive.
		t.skipWhitespaceAndComments();
		// The cached RBRACKET is still returned first...
		assertEquals(RBRACKET, t.read().type());
		// ...and the '\n' that followed ']' was preserved, not swallowed.
		assertEquals(NEWLINE, t.peekNoSkip().type());
	}

	// Companion to g15: the no-op guard must not regress the normal (nothing-cached) skip behavior.
	@Test
	void g16_skipStillWorksWhenNothingCached() throws Exception {
		var t = tokenizer("   # comment\n   z");
		t.skipWhitespaceAndComments();
		var tok = t.read();
		assertEquals(UNQUOTED_STRING, tok.type());
		assertEquals("z", tok.stringValue());
	}
}
