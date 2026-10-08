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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.time.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that {@link MockHttpTransport} simulates {@link TransportRequest#getTimeout()}.
 */
@SuppressWarnings({
	"java:S2925", // The handler sleeps to simulate a slow upstream.
	"resource" // Transport/response instances are short-lived test fixtures.
})
class MockHttpTransport_Timeout_Test {

	private static MockHttpTransport slowTransport() {
		return MockHttpTransport.builder().fallback((MockHttpTransport.RequestHandler)req -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8))).build();
		}).build();
	}

	private static TransportRequest request(Duration timeout) {
		return TransportRequest.builder().method("GET").uri("http://localhost/slow").timeout(timeout).build();
	}

	@Test void a01_shortTimeout_throwsTransportException() {
		var start = System.nanoTime();
		var e = assertThrows(TransportException.class, () -> slowTransport().execute(request(Duration.ofMillis(200))));
		assertInstanceOf(TimeoutException.class, e.getCause());
		assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 1800);
	}

	@Test void a02_noTimeout_runsInline() throws Exception {
		try (var response = slowTransport().execute(request(null))) {
			assertEquals(200, response.getStatusCode());
		}
	}

	@Test void a03_longTimeout_succeeds() throws Exception {
		try (var response = slowTransport().execute(request(Duration.ofSeconds(10)))) {
			assertEquals(200, response.getStatusCode());
		}
	}

	@Test void a04_handlerTransportException_propagatesUnchanged() {
		var transport = MockHttpTransport.builder().fallback((MockHttpTransport.RequestHandler)req -> {
			throw new TransportException("boom");
		}).build();
		var e = assertThrows(TransportException.class, () -> transport.execute(request(Duration.ofSeconds(5))));
		assertEquals("boom", e.getMessage());
	}
}
