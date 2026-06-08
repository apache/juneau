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
package org.apache.juneau.rest.server.tracing.otel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Exercises {@link RestRequestTextMapGetter} against a live {@link RestRequest} captured inside a
 * {@code @RestOp} handler so that header names, present/absent semantics, and the OpenTelemetry
 * {@code TextMapGetter} contract are all validated against the real request shape.
 */
class RestRequestTextMapGetter_Test extends TestBase {

	private static final AtomicReference<RestRequest> CAPTURED = new AtomicReference<>();

	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/capture")
		public String capture(RestRequest req) {
			CAPTURED.set(req);
			return "ok";
		}
	}

	private static final MockRestClient C = MockRestClient.buildLax(A.class);

	@BeforeEach
	void resetCaptured() { CAPTURED.set(null); }

	@Test void a01_get_returnsPresentHeaderValue() throws Exception {
		C.get("/capture")
			.header("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01")
			.run().assertStatus(200);

		var req = CAPTURED.get();
		assertNotNull(req);
		var g = RestRequestTextMapGetter.INSTANCE;
		assertEquals("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01", g.get(req, "traceparent"));
	}

	@Test void a02_get_returnsNullForAbsentHeader() throws Exception {
		C.get("/capture").run().assertStatus(200);
		var req = CAPTURED.get();
		assertNull(RestRequestTextMapGetter.INSTANCE.get(req, "x-not-present"));
	}

	@Test void a03_get_isCaseInsensitiveForHttpHeaders() throws Exception {
		C.get("/capture").header("X-Foo", "bar").run().assertStatus(200);
		var req = CAPTURED.get();
		// HTTP header names are case-insensitive; RequestHeader lookup must honor that.
		assertEquals("bar", RestRequestTextMapGetter.INSTANCE.get(req, "x-foo"));
		assertEquals("bar", RestRequestTextMapGetter.INSTANCE.get(req, "X-FOO"));
	}

	@Test void a04_get_nullCarrier_returnsNull() {
		assertNull(RestRequestTextMapGetter.INSTANCE.get(null, "anything"));
	}

	@Test void a05_keys_includesSentHeader() throws Exception {
		C.get("/capture").header("X-Trace-Id", "abc").run().assertStatus(200);
		var req = CAPTURED.get();
		var keys = RestRequestTextMapGetter.INSTANCE.keys(req);
		assertNotNull(keys);
		boolean found = false;
		for (var k : keys) {
			if ("X-Trace-Id".equalsIgnoreCase(k)) { found = true; break; }
		}
		assertTrue(found, "keys() should include sent header name (any case)");
	}

	@Test void a06_singleton_isStable() {
		assertSame(RestRequestTextMapGetter.INSTANCE, RestRequestTextMapGetter.INSTANCE);
	}
}
