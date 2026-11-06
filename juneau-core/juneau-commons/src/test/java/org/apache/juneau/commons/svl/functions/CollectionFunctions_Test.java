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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/** Tests for {@link CollectionFunctions}. */
class CollectionFunctions_Test extends TestBase {

	private static final VarResolver STRICT = VarResolver.create().defaultFunctions().strict(true).build();
	private static final VarResolver LENIENT = VarResolver.create().defaultFunctions().build();

	private static String render(VarResolver vr, String expr, Map<String,Object> scope) {
		return vr.compile("#{" + expr + "}").resolve(vr.createSession().scope(scope::get));
	}

	@Test void a01_listBuildsList() {
		assertEquals("[1,2,\"x\"]", render(STRICT, "list(1, 2, 'x')", Map.of()));
	}

	@Test void a02_emptyList() {
		assertEquals("[]", render(STRICT, "list()", Map.of()));
	}

	@Test void a03_takePrefix() {
		assertEquals("[1,2]", render(STRICT, "take(list(1, 2, 3), 2)", Map.of()));
	}

	@Test void a04_takeMoreThanSize() {
		assertEquals("[1,2,3]", render(STRICT, "take(list(1, 2, 3), 10)", Map.of()));
	}

	@Test void a05_takeZero() {
		assertEquals("[]", render(STRICT, "take(list(1, 2, 3), 0)", Map.of()));
	}

	@Test void a06_takeNegativeIsError() {
		assertThrows(RuntimeException.class, () -> render(STRICT, "take(list(1), -1)", Map.of()));
	}

	@Test void a07_takeNonWholeIsError() {
		assertThrows(RuntimeException.class, () -> render(STRICT, "take(list(1), 1.5)", Map.of()));
	}

	@Test void a08_takeNullListIsEmpty() {
		assertEquals("[]", render(STRICT, "take(missing, 3)", Map.of()));
	}

	@Test void a09_takeNonListIsError() {
		assertThrows(RuntimeException.class, () -> render(STRICT, "take('abc', 3)", Map.of()));
	}

	@Test void a10_overflowLine() {
		var scope = Map.<String,Object>of("xs", List.of(1, 2, 3, 4, 5, 6, 7));
		assertEquals("1,2,3 ... and 4 more", render(LENIENT, "join(',', take(xs, 3)) + (size(xs) > 3 ? ' ... and ' + (size(xs) - 3) + ' more' : '')", scope));
	}

	@Test void a11_tableOverProjectedRows() {
		var scope = Map.<String,Object>of("rows", List.of(Map.of("a", "1", "b", "2"), Map.of("a", "3", "b", "4")));
		var out = render(STRICT, "table(list('A', 'B'), rows.![list(a, b)])", scope);
		assertTrue(out.contains("A") && out.contains("B") && out.contains("1") && out.contains("4"), out);
	}

	@Test void a12_splitReturnsJsonTextNotList() {
		// Pins the grammar: split() yields JSON-array text, so it cannot be passed to table() as the header.
		assertEquals("[\"A\",\"B\"]", render(STRICT, "split('A,B', ',')", Map.of()));
		assertThrows(RuntimeException.class, () -> render(STRICT, "table(split('A,B', ','), list())", Map.of()));
	}

	@Test void a13_backslashNInQuotedLiteralIsNotANewline() {
		// Pins the grammar: the quoted literal keeps the backslash and the n as two characters (in both modes).
		// A real newline has to come from the scope (a name bound to a newline).
		assertEquals("a\\nb", render(STRICT, "'a\\nb'", Map.of()));
		assertEquals("a\\nb", render(LENIENT, "'a\\nb'", Map.of()));
		assertEquals("a\nb", render(LENIENT, "'a' + NL + 'b'", Map.of("NL", "\n")));
	}
}
