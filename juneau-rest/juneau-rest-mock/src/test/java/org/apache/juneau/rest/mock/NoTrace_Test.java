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

import org.apache.juneau.commons.logging.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that the <c>No-Trace</c> request header and the <js>"NoTrace"</js> request attribute keep the stack trace off
 * the access-log record of a failed call, and that the next-gen {@link MockRestClient} can send the header from its
 * builder and per request.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures.
})
class NoTrace_Test {

	@Rest(path="/nt")
	public static class A_Resource {
		@RestGet(path="/bad")
		public String bad(@Query(name="n") int n) {
			return "x" + n;
		}

		@RestGet(path="/failPlain")
		public String failPlain(RestResponse res) {
			res.setException(new NotFound("expected"));
			return "x";
		}

		@RestGet(path="/failAttrFalse")
		public String failAttrFalse(RestResponse res) {
			res.setNoTrace(false);
			res.setException(new NotFound("expected"));
			return "x";
		}

		@RestGet(path="/failAttr")
		public String failAttr(RestResponse res) {
			res.setNoTrace();
			res.setException(new NotFound("expected"));
			return "x";
		}
	}

	private static java.util.logging.LogRecord capture(java.util.function.Consumer<MockRestClient> call, MockRestClient client) {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			call.accept(client);
			return c.getRecords().stream().filter(r -> r.getLoggerName() != null && r.getLoggerName().startsWith(A_Resource.class.getName())).findFirst().orElseThrow();
		}
	}

	private static void run(MockRestClient client, String path, boolean noTrace) {
		try {
			var req = client.get(path);
			if (noTrace)
				req.noTrace();
			req.run();
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	@Test void a01_withoutNoTrace_recordCarriesThrown() {
		var client = MockRestClient.create(A_Resource.class);
		var rec = capture(c -> run(c, "/bad?n=abc", false), client);
		assertNotNull(rec.getThrown());
	}

	@Test void a02_header_viaPerRequestOption_suppressesThrown() {
		var client = MockRestClient.create(A_Resource.class);
		var rec = capture(c -> run(c, "/bad?n=abc", true), client);
		assertNull(rec.getThrown());
		assertTrue(rec.getMessage().contains("[400]"), rec.getMessage());
	}

	@Test void a03_header_viaBuilderOption_suppressesThrown() {
		var client = MockRestClient.builder(A_Resource.class).noTrace().build();
		var rec = capture(c -> run(c, "/bad?n=abc", false), client);
		assertNull(rec.getThrown());
	}

	@Test void a04_header_false_keepsThrown() {
		var client = MockRestClient.create(A_Resource.class);
		var rec = capture(c -> {
			try {
				c.get("/bad?n=abc").header("No-Trace", "false").run();
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}, client);
		assertNotNull(rec.getThrown());
	}

	@Test void a05a_noAttribute_recordCarriesThrown() {
		var client = MockRestClient.create(A_Resource.class);
		var rec = capture(c -> run(c, "/failPlain", false), client);
		assertNotNull(rec.getThrown());
	}

	@Test void a05_requestAttribute_suppressesThrown() {
		var client = MockRestClient.create(A_Resource.class);
		var rec = capture(c -> run(c, "/failAttr", false), client);
		assertNull(rec.getThrown());
	}

	@Test void a06_builderDefault_isOptIn() {
		// ignoreErrors-style defaults do not turn No-Trace on: a plain client still logs the trace.
		var client = MockRestClient.builder(A_Resource.class).build();
		var rec = capture(c -> run(c, "/bad?n=abc", false), client);
		assertNotNull(rec.getThrown());
	}

	@Test void a07_attributeFalse_winsOverHeaderTrue_keepsThrown() {
		var client = MockRestClient.create(A_Resource.class);
		var rec = capture(c -> run(c, "/failAttrFalse", true), client);
		assertNotNull(rec.getThrown());
	}
}
