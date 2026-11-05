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
import org.junit.jupiter.api.*;

/**
 * Tests for {@link VarSplit}.
 */
class VarSplit_Test extends TestBase {

	@Test void a01_plain() {
		assertEquals(1, VarSplit.firstTopLevel("C:a:b", ':'));
		assertEquals(3, VarSplit.lastTopLevel("C:a:b", ':'));
		assertEquals(List.of("C", "a", "b"), VarSplit.splitTopLevel("C:a:b", ':'));
	}

	@Test void a02_nestedVarIgnored() {
		assertEquals(1, VarSplit.firstTopLevel("P:${Q:z}:d", ':'));
		assertEquals(8, VarSplit.lastTopLevel("P:${Q:z}:d", ':'));
		assertEquals(List.of("P", "${Q:z}", "d"), VarSplit.splitTopLevel("P:${Q:z}:d", ':'));
	}

	@Test void a03_singleQuoted() {
		assertEquals(1, VarSplit.firstTopLevel("P:'a:b':d", ':'));
		assertEquals(7, VarSplit.lastTopLevel("P:'a:b':d", ':'));
		assertEquals(List.of("P", "'a:b'", "d"), VarSplit.splitTopLevel("P:'a:b':d", ':'));
	}

	@Test void a04_doubleQuoted() {
		assertEquals(List.of("P", "\"a:b\"", "d"), VarSplit.splitTopLevel("P:\"a:b\":d", ':'));
		assertEquals(7, VarSplit.lastTopLevel("P:\"a:b\":d", ':'));
	}

	@Test void a05_escape() {
		assertEquals(4, VarSplit.firstTopLevel("a\\:b:c", ':'));
		assertEquals(4, VarSplit.lastTopLevel("a\\:b:c", ':'));
		assertEquals(List.of("a\\:b", "c"), VarSplit.splitTopLevel("a\\:b:c", ':'));
	}

	@Test void a06_escapedBackslash() {
		assertEquals(2, VarSplit.firstTopLevel("\\\\:x", ':'));
		assertEquals(List.of("\\\\", "x"), VarSplit.splitTopLevel("\\\\:x", ':'));
	}

	@Test void a07_noSeparator() {
		assertEquals(-1, VarSplit.firstTopLevel("abc", ':'));
		assertEquals(-1, VarSplit.lastTopLevel("abc", ':'));
		assertEquals(List.of("abc"), VarSplit.splitTopLevel("abc", ':'));
	}

	@Test void a08_emptyString() {
		assertEquals(-1, VarSplit.firstTopLevel("", ':'));
		assertEquals(-1, VarSplit.lastTopLevel("", ':'));
		assertEquals(List.of(""), VarSplit.splitTopLevel("", ':'));
	}

	@Test void a09_emptySegments() {
		assertEquals(List.of("a", "", "b"), VarSplit.splitTopLevel("a::b", ':'));
		assertEquals(List.of("", ""), VarSplit.splitTopLevel(":", ':'));
	}

	@Test void a10_unterminatedBrace() {
		assertEquals(1, VarSplit.firstTopLevel("a:{b:c", ':'));
		assertEquals(1, VarSplit.lastTopLevel("a:{b:c", ':'));
		assertEquals(List.of("a", "{b:c"), VarSplit.splitTopLevel("a:{b:c", ':'));
	}

	@Test void a11_strayCloseBrace() {
		assertEquals(2, VarSplit.firstTopLevel("a}:b", ':'));
		assertEquals(List.of("a}", "b"), VarSplit.splitTopLevel("a}:b", ':'));
	}

	@Test void a12_unterminatedQuote() {
		assertEquals(1, VarSplit.lastTopLevel("a:'b:c", ':'));
		assertEquals(List.of("a", "'b:c"), VarSplit.splitTopLevel("a:'b:c", ':'));
	}

	@Test void a13_nestedBracesTwoLevels() {
		assertEquals(11, VarSplit.firstTopLevel("a{b{c:d}:e}:f", ':'));
		assertEquals(List.of("a{b{c:d}:e}", "f"), VarSplit.splitTopLevel("a{b{c:d}:e}:f", ':'));
	}

	@Test void a14_otherSeparator() {
		assertEquals(List.of("a", "${b,c}", "d"), VarSplit.splitTopLevel("a,${b,c},d", ','));
		assertEquals(1, VarSplit.firstTopLevel("a,b", ','));
	}

	@Test void a15_apostropheInDoubleQuotes() {
		assertEquals(6, VarSplit.firstTopLevel("\"it's\":x", ':'));
		assertEquals(List.of("\"it's\"", "x"), VarSplit.splitTopLevel("\"it's\":x", ':'));
	}

	@Test void a16_escapedQuoteInQuotes() {
		assertEquals(List.of("'a\\'b:c'", "d"), VarSplit.splitTopLevel("'a\\'b:c':d", ':'));
	}

	@Test void a17_midWordApostropheIsPlain() {
		assertEquals(List.of("RQ", "don't", "b"), VarSplit.splitTopLevel("RQ:don't:b", ':'));
		assertEquals(List.of("it's", "x"), VarSplit.splitTopLevel("it's:x", ':'));
		assertEquals(List.of("a", "b'c", "d'e"), VarSplit.splitTopLevel("a:b'c:d'e", ':'));
	}

	@Test void a18_quoteOnlyAtPartStart() {
		assertEquals(List.of("A", "'x:y'"), VarSplit.splitTopLevel("A:'x:y'", ':'));
		assertEquals(List.of("A", "k", "\"x:y\""), VarSplit.splitTopLevel("A:k:\"x:y\"", ':'));
		assertEquals(List.of("A", "x'y", "z'"), VarSplit.splitTopLevel("A:x'y:z'", ':'));
	}

	@Test void a19_quoteAtNestedBodyStart() {
		assertEquals(List.of("A", "${B:'q:r'}", "d"), VarSplit.splitTopLevel("A:${B:'q:r'}:d", ':'));
		assertEquals(List.of("A", "${B:'}'}", "d"), VarSplit.splitTopLevel("A:${B:'}'}:d", ':'));
	}

	@Test void a20_expressionInBodyUsesSpelQuotes() {
		// Inside a nested #{...}, quotes open anywhere and '' is an escaped quote.
		assertEquals(List.of("A", "#{x == 'a:}'}", "d"), VarSplit.splitTopLevel("A:#{x == 'a:}'}:d", ':'));
		assertEquals(List.of("A", "#{'it''s:'}", "d"), VarSplit.splitTopLevel("A:#{'it''s:'}:d", ':'));
	}

	@Test void a21_closeOfBody() {
		assertEquals(8, VarSplit.closeOfBody("${S:it's} == 'x'", 2));
		assertEquals(9, VarSplit.closeOfBody("${A:'x}y'}", 2));
		assertEquals(13, VarSplit.closeOfBody("${A:${B:k}:\\}}", 2));
		assertEquals(-1, VarSplit.closeOfBody("${A:'x}", 2));
		assertEquals(-1, VarSplit.closeOfBody("${A:k", 2));
	}

	@Test void a22_closeOfExpr() {
		assertEquals(20, VarSplit.closeOfExpr("#{ ${S:it's} == 'x' }", 2));
		assertEquals(7, VarSplit.closeOfExpr("#{'}'+1}", 2));
		assertEquals(8, VarSplit.closeOfExpr("#{'a''}'}", 2));
		assertEquals(-1, VarSplit.closeOfExpr("#{'a\\'b'}", 2));
		assertEquals(9, VarSplit.closeOfExpr("#{{1:'}'}}", 2));
	}

	@Test void a23_deepNestingNoStackOverflow() {
		var s = "${A:".repeat(100_000) + "}".repeat(100_000);
		assertEquals(s.length() - 1, VarSplit.closeOfBody(s, 2));
	}
}
