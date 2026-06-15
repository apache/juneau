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
package org.apache.juneau.marshall.stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Shared test assertions for {@link TokenReader} / {@link TokenWriter} suites across formats.
 *
 * <p>
 * Imported as a static-method facade by per-format token-stream tests.  Adding a new format's
 * test class should not duplicate the assertion logic &mdash; reuse this harness so the
 * declarative shape (sequence of expected token types, capability tier, etc.) stays consistent
 * across the codebase.
 */
public final class TokenStreamAssertions {

	private TokenStreamAssertions() {
		// Static-method facade.
	}

	/**
	 * Asserts that the cursor emits exactly the given sequence of token types when {@link
	 * TokenReader#next()} is called once per element of <c>expected</c>.
	 *
	 * @param r The cursor to drive.  Must be positioned before the first token (i.e.
	 * 	immediately after construction, with {@link TokenReader#getCurrentToken()} returning
	 * 	{@link TokenType#NOT_AVAILABLE}).
	 * @param expected The expected token types in emit order.
	 * @throws Exception If the cursor's {@link TokenReader#next()} throws.
	 */
	public static void assertSequence(TokenReader r, TokenType... expected) throws Exception {
		for (var i = 0; i < expected.length; i++) {
			var actual = r.next();
			assertEquals(expected[i], actual, "Token at index " + i);
		}
	}

	/**
	 * Asserts that the cursor reports O(1)-memory streaming (always true for a structural
	 * {@link TokenReader}).
	 *
	 * @param r The cursor.
	 */
	public static void assertReaderStreaming(TokenReader r) {
		assertTrue(r.isStreaming(),
			"Reader should report streaming on " + r.getClass().getName());
	}

	/**
	 * Asserts that the writer reports O(1)-memory streaming (always true for a structural
	 * {@link TokenWriter}).
	 *
	 * @param w The writer.
	 */
	public static void assertWriterStreaming(TokenWriter w) {
		assertTrue(w.isStreaming(),
			"Writer should report streaming on " + w.getClass().getName());
	}
}
