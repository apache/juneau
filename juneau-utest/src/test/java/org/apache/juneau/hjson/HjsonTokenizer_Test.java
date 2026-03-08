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
package org.apache.juneau.hjson;

import static org.apache.juneau.hjson.HjsonTokenizer.TokenType.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.parser.ParserPipe;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link HjsonTokenizer}.
 */
class HjsonTokenizer_Test extends TestBase {

	private static HjsonTokenizer tokenizer(String input) {
		return new HjsonTokenizer(new StringReader(input));
	}

	@Test
	void d01_quotedDouble() throws Exception {
		var t = tokenizer("\"hello\"");
		var tok = t.read();
		assertEquals(STRING, tok.type());
		assertEquals("hello", tok.stringValue());
	}

	@Test
	void d02_quotedSingle() throws Exception {
		var t = tokenizer("'world'");
		var tok = t.read();
		assertEquals(STRING, tok.type());
		assertEquals("world", tok.stringValue());
	}

	@Test
	void d03_multilineToken() throws Exception {
		var t = tokenizer("'''line1\nline2'''");
		var tok = t.read();
		assertEquals(MULTILINE, tok.type());
		assertTrue(tok.stringValue().contains("line1"));
		assertTrue(tok.stringValue().contains("line2"));
	}

	@Test
	void d04_quotelessToken() throws Exception {
		var t = tokenizer("hello world");
		var tok = t.read();
		assertEquals(QUOTELESS, tok.type());
		assertEquals("hello world", tok.stringValue());
	}

	@Test
	void d05_numberToken() throws Exception {
		var t = tokenizer("42");
		var tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(42, tok.numberValue());

		t = tokenizer("3.14");
		tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(3.14, (Double) tok.numberValue());

		t = tokenizer("-1");
		tok = t.read();
		assertEquals(NUMBER, tok.type());
		assertEquals(-1, tok.numberValue());
	}

	@Test
	void d06_booleanTokens() throws Exception {
		var t = tokenizer("true");
		var tok = t.read();
		assertEquals(TRUE, tok.type());

		t = tokenizer("false");
		tok = t.read();
		assertEquals(FALSE, tok.type());
	}

	@Test
	void d07_nullToken() throws Exception {
		var t = tokenizer("null");
		var tok = t.read();
		assertEquals(NULL, tok.type());
	}

	@Test
	void d08_structuralTokens() throws Exception {
		var t = tokenizer("{}[]:,");
		assertEquals(LBRACE, t.read().type());
		assertEquals(RBRACE, t.read().type());
		assertEquals(LBRACKET, t.read().type());
		assertEquals(RBRACKET, t.read().type());
		assertEquals(COLON, t.read().type());
		assertEquals(COMMA, t.read().type());
		assertEquals(EOF, t.read().type());
	}

	@Test
	void d09_hashComment() throws Exception {
		var t = tokenizer("# comment\nx");
		t.skipWhitespaceAndComments();
		var tok = t.read();
		assertEquals(QUOTELESS, tok.type());
		assertEquals("x", tok.stringValue());
	}

	@Test
	void d10_slashComment() throws Exception {
		var t = tokenizer("// comment\ny");
		t.skipWhitespaceAndComments();
		var tok = t.read();
		assertEquals(QUOTELESS, tok.type());
		assertEquals("y", tok.stringValue());
	}

	@Test
	void d11_blockComment() throws Exception {
		var t = tokenizer("/* block */\nz");
		t.skipWhitespaceAndComments();
		var tok = t.read();
		assertEquals(QUOTELESS, tok.type());
		assertEquals("z", tok.stringValue());
	}

	@Test
	void d12_multipleQuotelessWithNewlines() throws Exception {
		var t = tokenizer("a\nb\nc");
		var t1 = t.read();
		assertEquals(QUOTELESS, t1.type());
		assertEquals("a", t1.stringValue());
		var t2 = t.read();
		assertEquals(QUOTELESS, t2.type());
		assertEquals("b", t2.stringValue());
		var t3 = t.read();
		assertEquals(QUOTELESS, t3.type());
		assertEquals("c", t3.stringValue());
	}

	@Test
	void d13_bracelessInputFirstToken() throws Exception {
		// For "name: Bob\nage: 25", first token must be QUOTELESS "name" (not LBRACE)
		var t = tokenizer("name: Bob\nage: 25");
		t.skipWhitespaceAndComments();
		var peek = t.peek();
		assertEquals(QUOTELESS, peek.type(), "First token for braceless input should be QUOTELESS");
		assertEquals("name", peek.stringValue());
	}

	@Test
	void d14_bracelessWithParserReader() throws Exception {
		// Same test but using ParserReader (as the parser does) - must match d13
		var pipe = new ParserPipe("name: Bob\nage: 25");
		try (var r = pipe.getParserReader()) {
			var t = new HjsonTokenizer(r);
			t.skipWhitespaceAndComments();
			var peek = t.peek();
			assertEquals(QUOTELESS, peek.type(), "First token via ParserReader should be QUOTELESS");
			assertEquals("name", peek.stringValue());
		}
	}
}
