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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ResponseBody#asCursor(Class)} / {@link ResponseBody#asCursor(org.apache.juneau.marshall.parser.Parser, Class)}.
 */
class ResponseBody_Cursor_Test {

	public static class Bean {
		public String name;
		public int age;
		public Bean() {
			// No-arg constructor required for deserialization.
		}
	}

	@SuppressWarnings({
		"resource" // Returned RestResponse owns the inner RestClient; caller closes via try-with-resources at the call site.
	})
	private static RestResponse response(String json) {
		var tr = TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
			.build();
		return new RestResponse(tr, RestClient.create());
	}

	// ==========================================================================
	// a — record / token cursors
	// ==========================================================================

	@Test
	void a01_recordCursor_explicitParser() throws Exception {
		try (var resp = response("{\"name\":\"alice\",\"age\":30}")) {
			try (RecordReader r = resp.body().asCursor(JsonParser.DEFAULT, RecordReader.class)) {
				var b = r.read(Bean.class);
				assertEquals("alice", b.name);
				assertEquals(30, b.age);
			}
		}
	}

	@Test
	void a02_recordCursor_defaultJsonParser() throws Exception {
		try (var resp = response("{\"name\":\"bob\",\"age\":40}")) {
			try (RecordReader r = resp.body().asCursor(RecordReader.class)) {
				var b = r.read(Bean.class);
				assertEquals("bob", b.name);
				assertEquals(40, b.age);
			}
		}
	}

	@Test
	void a03_tokenCursor() throws Exception {
		try (var resp = response("{\"name\":\"carol\",\"age\":50}")) {
			try (TokenReader r = resp.body().asCursor(TokenReader.class)) {
				var b = r.read(Bean.class);
				assertEquals("carol", b.name);
				assertEquals(50, b.age);
			}
		}
	}

	@Test
	void a04_concreteCursorType() throws Exception {
		try (var resp = response("{\"name\":\"dan\",\"age\":60}")) {
			try (JsonTokenReader r = resp.body().asCursor(JsonTokenReader.class)) {
				var b = r.read(Bean.class);
				assertEquals("dan", b.name);
			}
		}
	}

	// ==========================================================================
	// b — error paths
	// ==========================================================================

	@Test
	void b01_cursorTypeNotAssignable() throws Exception {
		// JsonParser produces a JsonTokenReader, which is not assignable to JsonlTokenReader.
		try (var resp = response("{\"name\":\"x\",\"age\":1}")) {
			assertThrows(IOException.class, () -> resp.body().asCursor(JsonParser.DEFAULT, JsonlTokenReader.class));
		}
	}
}
