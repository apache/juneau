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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.sse.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ResponseBody#asEventStream()}.
 */
@SuppressWarnings("resource") // 'tr'/TransportResponse values below are handed to (and closed by) the enclosing RestResponse; test helpers return a RestResponse the caller closes via try-with-resources.
class ResponseBody_AsEventStream_Test {

	private static RestResponse response(String sseText, Closeable closeCallback) {
		return response(new ByteArrayInputStream(sseText.getBytes(StandardCharsets.UTF_8)), closeCallback);
	}

	private static RestResponse response(InputStream bodyStream, Closeable closeCallback) {
		var tr = TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(bodyStream)
			.closeCallback(closeCallback)
			.build();
		return new RestResponse(tr, RestClient.builder().build());
	}

	@Test
	void a01_asEventStream_readsEventsInOrder() throws Exception {
		var sse = """
			event: progress
			data: {"step":1}

			event: progress
			data: {"step":2}

			""";
		try (var resp = response(sse, () -> {})) {
			try (SseEventReader r = resp.body().asEventStream()) {
				assertTrue(r.hasNext());
				var e1 = r.next();
				assertEquals("progress", e1.getEvent());
				assertEquals("{\"step\":1}", e1.getData());

				assertTrue(r.hasNext());
				var e2 = r.next();
				assertEquals("progress", e2.getEvent());
				assertEquals("{\"step\":2}", e2.getData());

				assertFalse(r.hasNext());
			}
		}
	}

	@Test
	void a02_asEventStream_noBody_throwsIOException() throws Exception {
		var tr = TransportResponse.builder().statusCode(204).build();
		try (var resp = new RestResponse(tr, RestClient.builder().build())) {
			assertThrows(IOException.class, () -> resp.body().asEventStream());
		}
	}

	@Test
	void b01_close_alsoClosesParentResponse() throws Exception {
		var closed = new AtomicBoolean();
		try (var resp = response("event: x\ndata: y\n\n", () -> closed.set(true))) {
			try (SseEventReader r = resp.body().asEventStream()) {
				assertFalse(closed.get());
				r.close();
				assertTrue(closed.get());
			}
		}
	}

	@Test
	void c01_close_closesResponseBeforeReader() throws Exception {
		var closed = new ArrayList<String>();
		var responseClosed = new AtomicBoolean();
		var readerClosed = new AtomicBoolean();
		var a = new ByteArrayInputStream("event: x\ndata: y\n\n".getBytes(StandardCharsets.UTF_8)) {
			@Override
			public void close() throws IOException {
				if (readerClosed.compareAndSet(false, true))
					closed.add("reader");
				super.close();
			}
		};
		try (var b = response(a, () -> {
			if (responseClosed.compareAndSet(false, true))
				closed.add("response");
		})) {
			try (var c = b.body().asEventStream()) {
				c.close();
				assertEquals(List.of("response", "reader"), closed);
			}
		}
	}

	@Test
	void c02_close_responseCloseThrows_stillClosesReader() throws Exception {
		var readerClosed = new AtomicBoolean();
		var a = new ByteArrayInputStream("event: x\ndata: y\n\n".getBytes(StandardCharsets.UTF_8)) {
			@Override
			public void close() throws IOException {
				readerClosed.set(true);
				super.close();
			}
		};
		var b = response(a, () -> {
			throw new IOException("boom");
		});
		var c = b.body().asEventStream();
		try {
			var d = assertThrows(IOException.class, c::close);
			assertEquals("boom", d.getMessage());
			assertTrue(readerClosed.get());
		} finally {
			try {
				c.close();
			} catch (IOException e) {
				// Ignore cleanup failures from the intentional throwing close callback.
			}
			try {
				b.close();
			} catch (IOException e) {
				// Ignore cleanup failures from the intentional throwing close callback.
			}
		}
	}
}
