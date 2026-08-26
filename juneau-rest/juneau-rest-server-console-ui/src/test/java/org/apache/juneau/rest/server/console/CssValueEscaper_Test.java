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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Phase 2 gate: {@code CssValueEscaper} unit tests.
 *
 * <p>
 * Tested standalone (not through {@link Theme.Builder#token(String, String)}, since those payloads would be
 * REJECTed by {@code CssValueGrammar} before ever reaching the escaper) &mdash; this is the declaration-boundary
 * belt, not the sink-safety mechanism.
 */
class CssValueEscaper_Test extends TestBase {

	@Test void a01_breakoutCharacters_neutralized() {
		var escaped = CssValueEscaper.escape(";}*{display:none");
		assertFalse(escaped.contains(";"));
		assertFalse(escaped.contains("{"));
		assertFalse(escaped.contains("}"));
	}

	@Test void a02_htmlBreakoutPayload_neutralized() {
		var escaped = CssValueEscaper.escape("}</style><script>alert(1)</script>");
		assertFalse(escaped.contains("}"));
		assertFalse(escaped.contains("<"));
		assertFalse(escaped.contains(">"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"linear-gradient(180deg, #eef3f8, #dde6ef)",
		"#1589EE",
		"'Source Sans 3', Inter, sans-serif",
	})
	void a03_legitimateValues_passThroughByteForByte(String value) {
		assertEquals(value, CssValueEscaper.escape(value));
	}

	@Test void a06_parensNotWidened_gradientSurvives() {
		// The escaper must NOT escape ( or ) - doing so would corrupt a legitimate gradient/rgb() value on decode.
		var value = "rgb(21,137,238)";
		assertEquals(value, CssValueEscaper.escape(value));
	}

	@Test void a07_c0ControlChar_escaped() {
		var escaped = CssValueEscaper.escape("red\u0007");
		assertFalse(escaped.indexOf('\u0007') >= 0);
		assertTrue(escaped.startsWith("red"));
	}
}
