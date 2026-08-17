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

import java.util.logging.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end server-side proof that the always-on correlation id reaches the debug record on both sinks &mdash; the
 * structured {@code requestId} field and the rendered {@code [requestId=<id>]} message prefix &mdash; with <b>no</b>
 * {@code RequestIdFilter} wiring, including the moved-{@code attributeKey} read-path and the early-404 path.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures.
})
class RequestIdDebugCapture_Test {

	@Rest(path="/api")
	public static class A_Resource {
		@RestGet(path="/who")
		public String who() { return "ok"; }
	}

	@Test void a01_serverDebugRecordCarriesRequestId_noFilter() throws Exception {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(A_Resource.class).debug().build();
			var res = client.get("/who").run().assertStatus(200);
			var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();

			var rec = c.getRecords().stream()
				.filter(r -> (A_Resource.class.getName() + ".who").equals(r.getLoggerName()))
				.findFirst().orElseThrow();
			assertEquals(echoed, LogRecordContext.of(rec).get("requestId"));
			assertTrue(rec.getMessage().contains("[requestId=" + echoed + "] "), rec.getMessage());
			assertTrue(rec.getMessage().contains("[200] HTTP GET /api/who"), rec.getMessage());
		}
	}

	@Test void a02_honorsIncomingIdInDebugRecord() throws Exception {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(A_Resource.class).debug().build();
			var id = "550e8400-e29b-41d4-a716-446655440000";
			client.get("/who").header("X-Request-Id", id).run().assertStatus(200);

			var rec = c.getRecords().stream()
				.filter(r -> (A_Resource.class.getName() + ".who").equals(r.getLoggerName()))
				.findFirst().orElseThrow();
			assertEquals(id, LogRecordContext.of(rec).get("requestId"));
			assertTrue(rec.getMessage().contains("[requestId=" + id + "] "), rec.getMessage());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Read-path proof: a moved attributeKey must not blank the rendered prefix (statusLine reads the session cache,
	// never the fixed default public attribute).
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path="/api")
	public static class CustomKey_Resource {
		@Bean public RequestIdSettings requestIdSettings() {
			return RequestIdSettings.create().attributeKey("customReqId").build();
		}
		@RestGet(path="/who")
		public String who() { return "ok"; }
	}

	@Test void b01_movedAttributeKeyStillRendersPrefix() throws Exception {
		try (var c = RichLogger.getLogger(CustomKey_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(CustomKey_Resource.class).debug().build();
			var res = client.get("/who").run().assertStatus(200);
			var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();

			var rec = c.getRecords().stream()
				.filter(r -> (CustomKey_Resource.class.getName() + ".who").equals(r.getLoggerName()))
				.findFirst().orElseThrow();
			assertEquals(echoed, LogRecordContext.of(rec).get("requestId"));
			assertTrue(rec.getMessage().contains("[requestId=" + echoed + "] "), rec.getMessage());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Early-404 path: a request that never resolves an operation still mints + renders the id (statusLine on the
	// no-op/404 render path).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_early404RecordCarriesRequestId() throws Exception {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(A_Resource.class).ignoreErrors().debug().build();
			var res = client.get("/does-not-exist").run().assertStatus(404);
			var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();

			var rec = c.getRecords().stream()
				.filter(r -> r.getMessage() != null && r.getMessage().contains("[404]"))
				.findFirst().orElseThrow();
			assertTrue(rec.getMessage().contains("[requestId=" + echoed + "] "), rec.getMessage());
		}
	}
}
