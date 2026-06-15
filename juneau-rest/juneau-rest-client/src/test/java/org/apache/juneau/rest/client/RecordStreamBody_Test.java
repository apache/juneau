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
import org.apache.juneau.marshall.plaintext.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the next-generation {@link RecordStreamBody} streaming request body.
 */
class RecordStreamBody_Test {

	public static class Bean {
		public String name;
		public int age;
		public Bean() {}
		public Bean(String name, int age) { this.name = name; this.age = age; }
	}

	private static String writeToString(RecordStreamBody body) throws IOException {
		var baos = new ByteArrayOutputStream();
		body.writeTo(baos);
		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
	}

	// ==========================================================================
	// a — record(...) (whole-value record cursor)
	// ==========================================================================

	@Test
	@SuppressWarnings({
		"resource" // Fluent writer is caller-owned; nothing new to close.
	})
	void a01_record_defaultJson() throws Exception {
		var body = RecordStreamBody.records(w -> {
			try {
				w.write(new Bean("dave", 99));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		// The record cursor's POJO walk emits bean properties in alphabetical order.
		assertEquals("{\"age\":99,\"name\":\"dave\"}", writeToString(body));
	}

	@Test
	void a02_record_metadata() {
		var body = RecordStreamBody.records(w -> {});
		assertEquals("application/json", body.getContentType());
		assertEquals(-1, body.getContentLength());
		assertFalse(body.isRepeatable());
	}

	@Test
	@SuppressWarnings({
		"resource" // Fluent writer is caller-owned; nothing new to close.
	})
	void a03_record_explicitSerializer() throws Exception {
		var body = RecordStreamBody.records(JsonSerializer.DEFAULT, w -> {
			try {
				w.write(new Bean("amy", 7));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		assertEquals("{\"age\":7,\"name\":\"amy\"}", writeToString(body));
	}

	// ==========================================================================
	// b — token(...) (fine-grained structural cursor)
	// ==========================================================================

	@Test
	@SuppressWarnings({
		"resource" // Fluent writer is caller-owned; nothing new to close.
	})
	void b01_token_defaultJson() throws Exception {
		var body = RecordStreamBody.token(w -> {
			try {
				w.startObject();
				w.fieldName("name");
				w.string("eve");
				w.fieldName("age");
				w.number(45);
				w.endObject();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		assertEquals("{\"name\":\"eve\",\"age\":45}", writeToString(body));
	}

	// ==========================================================================
	// c — repeatability + capability mismatch
	// ==========================================================================

	@Test
	void c01_repeatable_optIn() {
		var body = RecordStreamBody.records(w -> {}).repeatable();
		assertTrue(body.isRepeatable());
	}

	@Test
	@SuppressWarnings({
		"resource" // Fluent writer is caller-owned; nothing new to close.
	})
	void c02_repeatable_isReusable() throws Exception {
		var body = RecordStreamBody.records(w -> {
			try {
				w.write(new Bean("x", 1));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).repeatable();
		// A repeatable body must produce identical output across multiple writeTo invocations.
		assertEquals(writeToString(body), writeToString(body));
	}

	@Test
	@SuppressWarnings({
		"resource" // Fluent writer is caller-owned; nothing new to close.
	})
	void c03_nonRepeatable_failsFastOnResend() throws Exception {
		var body = RecordStreamBody.records(w -> {
			try {
				w.write(new Bean("x", 1));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		writeToString(body); // First write succeeds.
		var baos = new ByteArrayOutputStream();
		// A non-repeatable body must fail fast if a resend (second write) is required.
		assertThrows(IOException.class, () -> body.writeTo(baos));
	}

	@Test
	void c04_tokenSurfaceUnsupported_failsFastAtConstruction() {
		// PlainTextSerializer is a writer-serializer but does not implement the token-writer surface.
		assertThrows(IllegalArgumentException.class, () -> RecordStreamBody.token(PlainTextSerializer.DEFAULT, w -> {}));
	}
}
