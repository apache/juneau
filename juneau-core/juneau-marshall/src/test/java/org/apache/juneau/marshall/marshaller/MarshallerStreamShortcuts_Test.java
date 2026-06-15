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
package org.apache.juneau.marshall.marshaller;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the {@link Marshaller} low-level stream shortcut methods
 * ({@code readTokens}/{@code writeTokens}/{@code readRecords}/{@code writeRecords}/
 * {@code readArrayRecords}/{@code writeArrayRecords}) — each is a thin cast-and-delegate to the
 * underlying parser/serializer capability role.
 */
@SuppressWarnings({
	"resource" // Cursors are closed via try-with-resources where it matters; in-memory targets need no cleanup.
})
class MarshallerStreamShortcuts_Test extends TestBase {

	@Test void a01_readTokens() throws Exception {
		try (var r = Json.DEFAULT.readTokens("[1]")) {
			assertEquals(TokenType.START_ARRAY, r.next());
			assertEquals(TokenType.VALUE_NUMBER, r.next());
		}
	}

	@Test void a02_writeTokens() throws Exception {
		var sb = new StringBuilder();
		try (var w = Json.DEFAULT.writeTokens(sb)) {
			w.startArray().number(1).endArray();
		}
		assertEquals("[1]", sb.toString());
	}

	@Test void a03_readRecords() throws Exception {
		try (var r = Json.DEFAULT.readRecords("{\"a\":1}")) {
			assertTrue(r.isStreaming());
			var m = r.read(Map.class);
			assertBean(m, "a", "1");
		}
	}

	@Test void a04_writeRecords() throws Exception {
		var sb = new StringBuilder();
		try (var w = Json.DEFAULT.writeRecords(sb)) {
			w.write(Map.of("a", 1));
		}
		assertEquals("{\"a\":1}", sb.toString());
	}

	@Test void a05_readArrayRecords() throws Exception {
		var seen = new ArrayList<Integer>();
		try (var r = Json.DEFAULT.readArrayRecords("[1,2,3]")) {
			assertTrue(r.isStreaming());
			while (r.canRead())
				seen.add(r.read(Integer.class));
		}
		assertList(seen, "1", "2", "3");
	}

	@Test void a06_writeArrayRecords() throws Exception {
		var sb = new StringBuilder();
		try (var w = Json.DEFAULT.writeArrayRecords(sb)) {
			w.write(1);
			w.write(2);
		}
		assertEquals("[1,2]", sb.toString());
	}

	@Test void a07_jsonlArrayRecordsAreLineDelimited() throws Exception {
		// JSONL aliases its line record stream — no surrounding [...] brackets.
		var sb = new StringBuilder();
		try (var w = Jsonl.DEFAULT.writeArrayRecords(sb)) {
			w.write(Map.of("x", 1));
			w.write(Map.of("x", 2));
		}
		assertEquals("{\"x\":1}\n{\"x\":2}\n", sb.toString());

		var seen = new ArrayList<Map<?, ?>>();
		try (var r = Jsonl.DEFAULT.readArrayRecords(sb.toString())) {
			while (r.canRead())
				seen.add(r.read(Map.class));
		}
		assertEquals(2, seen.size());
		assertBean(seen.get(0), "x", "1");
		assertBean(seen.get(1), "x", "2");
	}

	@Test void a08_readTokensClassCastForNonTokenFormat() {
		// Csv's parser is not a TokenReadable, so the structural-token shortcut yields ClassCastException.
		assertThrows(ClassCastException.class, () -> Csv.DEFAULT.readTokens("a\n1"));
	}

	@Test void a09_writeTokensClassCastForNonTokenFormat() {
		assertThrows(ClassCastException.class, () -> Csv.DEFAULT.writeTokens(new StringBuilder()));
	}
}
