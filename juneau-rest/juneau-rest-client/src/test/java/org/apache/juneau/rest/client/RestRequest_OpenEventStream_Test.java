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
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.sse.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link RestRequest#openEventStream()}.
 */
@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class RestRequest_OpenEventStream_Test {

	@Test
	void a01_get_readsEvents() throws Exception {
		var sse = "event: progress\ndata: {\"step\":1}\n\n";
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(sse.getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = RestClient.builder().transport(transport).build()) {
			try (SseEventReader r = client.get("http://x/events").openEventStream()) {
				assertTrue(r.hasNext());
				var e = r.next();
				assertEquals("progress", e.getEvent());
				assertEquals("{\"step\":1}", e.getData());
			}
		}
	}

	@Test
	void a02_post_sendsBodyAndReadsEvents() throws Exception {
		var sse = "data: ok\n\n";
		var seenMethod = new AtomicReference<String>();
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenMethod.set(tReq.getMethod());
			try {
				var a = new ByteArrayOutputStream();
				tReq.getBody().writeTo(a);
				seenBody.set(a.toString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "text/event-stream")
				.body(new ByteArrayInputStream(sse.getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = RestClient.builder().transport(transport).build()) {
			try (SseEventReader r = client.post("http://x/events").bodyString("{}").openEventStream()) {
				assertTrue(r.hasNext());
				assertEquals("ok", r.next().getData());
			}
		}
		assertEquals("POST", seenMethod.get());
		assertEquals("{}", seenBody.get());
	}

	@Test
	void a03_setsAcceptHeader_whenNotAlreadySet() throws Exception {
		var seenAccept = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			var h = tReq.getFirstHeader("Accept");
			seenAccept.set(h == null ? null : h.value());
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "text/event-stream")
				.body(new ByteArrayInputStream("data: x\n\n".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = RestClient.builder().transport(transport).build()) {
			try (SseEventReader r = client.get("http://x/events").openEventStream()) {
				assertTrue(r.hasNext());
			}
		}
		assertEquals("text/event-stream", seenAccept.get());
	}

	@Test
	void a04_doesNotOverrideExplicitAcceptHeader() throws Exception {
		var seenAccept = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			var h = tReq.getFirstHeader("Accept");
			seenAccept.set(h == null ? null : h.value());
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "text/event-stream")
				.body(new ByteArrayInputStream("data: x\n\n".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = RestClient.builder().transport(transport).build()) {
			try (SseEventReader r = client.get("http://x/events").header("Accept", "text/event-stream;q=0.9").openEventStream()) {
				assertTrue(r.hasNext());
			}
		}
		assertEquals("text/event-stream;q=0.9", seenAccept.get());
	}

	@Test
	void b01_noResponseBody_closesResponseAndThrows() throws Exception {
		var closed = new AtomicBoolean();
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(204)
			.closeCallback(() -> closed.set(true))
			.build();
		try (var client = RestClient.builder().transport(transport).build()) {
			var a = assertThrows(IOException.class, () -> client.get("http://x/events").openEventStream());
			assertEquals("Response has no body to open an event stream over.", a.getMessage());
		}
		assertTrue(closed.get());
	}

	@Test
	void b02_non2xxResponse_closesResponseAndThrows() throws Exception {
		var closed = new AtomicBoolean();
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(500)
			.reasonPhrase("Internal Server Error")
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{\"error\":\"nope\"}".getBytes(StandardCharsets.UTF_8)))
			.closeCallback(() -> closed.set(true))
			.build();
		try (var client = RestClient.builder().transport(transport).build()) {
			assertThrows(RestCallException.class, () -> client.get("http://x/events").openEventStream());
		}
		assertTrue(closed.get());
	}
}
