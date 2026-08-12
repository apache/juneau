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
package org.apache.juneau.rest.client.classic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.marshall.xml.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the private {@code RestRequest#streamBodyEntity(RecordStreamBody, Serializer)} helper (reached via
 * {@code content(RecordStreamBody)} + {@code run()} when the content-negotiated serializer is finally known) --
 * entirely uncovered previously, since none of the {@code juneau-integration-tests} suites use the classic
 * module's {@link RecordStreamBody} streaming-body feature. Mirrors the equivalent non-classic
 * {@code RecordStreamBody_Test}'s capability-mismatch cases, adapted to the classic module's later
 * (request-run-time, not construction-time) surface-check timing.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class RestRequest_StreamBodyEntity_Coverage_Test {

	/** A bare {@link Serializer} implementing neither {@link RecordWritable} nor {@link TokenWritable}. */
	private static final class NeitherSurfaceSerializer extends Serializer {
		// produces()/accept() must be set, or getPrimaryMediaType() (called earlier in run(), before the
		// RecordWritable/TokenWritable check) throws IllegalStateException first and never reaches streamBodyEntity.
		NeitherSurfaceSerializer() { super(Serializer.create().produces("application/test").accept("application/test")); }
	}

	/** A {@link TokenWritable} serializer whose {@code writeTokens} always fails, to exercise the outer IOException-wrapping branch. */
	private static final class ThrowingTokenSerializer extends Serializer implements TokenWritable {
		ThrowingTokenSerializer() { super(Serializer.create().produces("application/test").accept("application/test")); }
		@Override public TokenWriter writeTokens(Object output) throws IOException { throw new IOException("Simulated: cannot open token writer."); }
	}

	private static HttpServer server;
	private static int port;
	private static volatile byte[] lastRequestBody = new byte[0];

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		server.createContext("/echo", exchange -> {
			lastRequestBody = exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private static String url() {
		return "http://localhost:" + port + "/echo";
	}

	// ==========================================================================
	// a - happy path
	// ==========================================================================

	@Test void a01_records_recordWritableSerializer_succeeds() throws Exception {
		// JsonSerializer implements RecordWritable (via TokenWritable extends RecordWritable), so records() reaches
		// the writeRecords() branch of streamBodyEntity's dispatch even though the underlying session only defines
		// writeTokens() directly.
		try (var client = RestClient.create().build();
				var req = client.post(url(), null)
					.serializer(JsonSerializer.DEFAULT)
					.content(RecordStreamBody.records(w -> {
						try {
							w.write("hello");
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}));
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertTrue(new String(lastRequestBody, StandardCharsets.UTF_8).contains("hello"));
	}

	@Test void a02b_records_recordWritableOnlySerializer_writerBased_roundTrips() throws Exception {
		// Fixed: XmlSerializer is RecordWritable but NOT TokenWritable, and its
		// writeRecords(Object) is a writer-based (RecordAdapter-backed) cursor whose close() closes the
		// underlying OutputStreamWriter -- previously, streamBodyEntity()'s redundant post-close flush() on that
		// already-closed writer threw "Stream closed", breaking every writer-based non-JSON format. Verifies the
		// fix restores a correct round-trip.
		try (var client = RestClient.create().build();
				var req = client.post(url(), null)
					.serializer(XmlSerializer.DEFAULT)
					.content(RecordStreamBody.records(w -> {
						try {
							w.write("hello");
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}));
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		var body = new String(lastRequestBody, StandardCharsets.UTF_8);
		assertTrue(body.contains("hello"), "Unexpected body: " + body);
	}

	@Test void a02_token_tokenWritableSerializer_succeeds() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.post(url(), null)
					.serializer(JsonSerializer.DEFAULT)
					.content(RecordStreamBody.token(w -> {
						try {
							w.startObject();
							w.fieldName("name");
							w.string("eve");
							w.endObject();
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}));
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("{\"name\":\"eve\"}", new String(lastRequestBody, StandardCharsets.UTF_8));
	}

	// ==========================================================================
	// b - capability / configuration errors
	// ==========================================================================

	@Test void b01_noSerializerRegistered_throwsIllegalArgumentException() throws IOException {
		try (var client = RestClient.create().build()) {
			// No .json()/.xml()/etc and no explicit .serializer(...) -- content negotiation resolves to null.
			try (var req = client.post(url(), null).content(RecordStreamBody.records(w -> {}))) {
				var e = assertThrows(IllegalArgumentException.class, req::run);
				assertTrue(e.getMessage().contains("No serializer registered"), "Unexpected message: " + e.getMessage());
			}
		} catch (RestCallException e) {
			throw new RuntimeException(e);
		}
	}

	@Test void b02_tokenSurfaceUnsupported_throwsIllegalArgumentException() throws IOException {
		try (var client = RestClient.create().build()) {
			// XmlSerializer implements RecordWritable but not TokenWritable.
			try (var req = client.post(url(), null).serializer(XmlSerializer.DEFAULT).content(RecordStreamBody.token(w -> {}))) {
				var e = assertThrows(IllegalArgumentException.class, req::run);
				assertTrue(e.getMessage().contains("does not support the token-writer surface"), "Unexpected message: " + e.getMessage());
			}
		} catch (RestCallException e) {
			throw new RuntimeException(e);
		}
	}

	@Test void b03_recordSurfaceUnsupported_throwsIllegalArgumentException() throws IOException {
		try (var client = RestClient.create().build()) {
			try (var req = client.post(url(), null).serializer(new NeitherSurfaceSerializer()).content(RecordStreamBody.records(w -> {}))) {
				var e = assertThrows(IllegalArgumentException.class, req::run);
				assertTrue(e.getMessage().contains("does not support the record-writer surface"), "Unexpected message: " + e.getMessage());
			}
		} catch (RestCallException e) {
			throw new RuntimeException(e);
		}
	}

	@Test void c01_ioExceptionOpeningWriter_wrappedAsRuntimeException() throws IOException {
		try (var client = RestClient.create().build()) {
			try (var req = client.post(url(), null).serializer(new ThrowingTokenSerializer()).content(RecordStreamBody.token(w -> {}))) {
				var e = assertThrows(RuntimeException.class, req::run);
				assertTrue(e.getMessage().contains("I/O error streaming request body"), "Unexpected message: " + e.getMessage());
				assertInstanceOf(IOException.class, e.getCause());
			}
		} catch (RestCallException e) {
			throw new RuntimeException(e);
		}
	}
}
