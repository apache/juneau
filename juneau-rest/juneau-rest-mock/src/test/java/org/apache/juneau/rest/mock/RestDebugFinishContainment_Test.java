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
import java.util.logging.*;

import org.apache.juneau.commons.logging.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.logging.*;
import org.junit.jupiter.api.*;

/**
 * Proves that a formatter (or scrubber) that throws while rendering the debug record during {@link RestSession#finish()}
 * cannot escape and fail a completed request, and that only a fixed token — never the secret, the message, or a second
 * formatter pass — is logged.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures.
})
class RestDebugFinishContainment_Test {

	private static final String FINISH_LOGGER = RestSession.class.getName();

	/** A resource that IS its own formatter and throws a {@code RuntimeException} while rendering the body ({@code FINEST}). */
	@Rest(path="/finbody")
	public static class A_ThrowsRuntimeInBody implements RestDebugFormatter {
		@Override public String formatBasic(RestRequest req, RestResponse res) { return "[basic]"; }
		@Override public String formatBody(RestRequest req, RestResponse res) {
			throw new RuntimeException("secret-in-formatter-BODY");
		}
		@RestPost(path="/echo")
		public String echo(RestRequest req) throws IOException {
			return req.getContent().asString();
		}
	}

	@Test void a01_formatterThrowsRuntimeAtFinest_requestStillCompletes_fixedTokenOnly() throws Exception {
		try (var c = RichLogger.getLogger(FINISH_LOGGER).captureEvents(Level.WARNING)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(A_ThrowsRuntimeInBody.class).debug().build();

			// The request thread must not fail even though the formatter throws during finish().
			client.post("/echo", "request-payload").run().assertStatus().asCode().is(200).assertContent("request-payload");

			var rec = c.getRecords().stream()
				.filter(r -> FINISH_LOGGER.equals(r.getLoggerName()))
				.reduce((a, b) -> b)
				.orElse(null);
			assertNotNull(rec, "a fixed diagnostic-failure token should be logged");
			assertEquals("debug formatter failed", rec.getMessage());
			assertFalse(rec.getMessage().contains("secret-in-formatter-BODY"), rec.getMessage());
			assertNull(rec.getThrown(), "the failure must not attach the formatter's exception (which carries the body)");
		}
	}

	/** A resource that IS its own formatter and throws an {@code Error} while rendering the basic line ({@code INFO}). */
	@Rest(path="/finbasic")
	public static class B_ThrowsErrorInBasic implements RestDebugFormatter {
		@Override public String formatBasic(RestRequest req, RestResponse res) {
			throw new AssertionError("error-secret-in-basic");
		}
		@RestPost(path="/echo")
		public String echo(RestRequest req) throws IOException {
			return req.getContent().asString();
		}
	}

	@Test void a02_formatterThrowsErrorAtInfo_requestStillCompletes_fixedTokenOnly() throws Exception {
		var target = Logger.getLogger(B_ThrowsErrorInBasic.class.getName());
		var prevLevel = target.getLevel();
		target.setLevel(Level.INFO);
		try (var c = RichLogger.getLogger(FINISH_LOGGER).captureEvents(Level.WARNING)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(B_ThrowsErrorInBasic.class).build();

			client.post("/echo", "request-payload").run().assertStatus().asCode().is(200).assertContent("request-payload");

			var rec = c.getRecords().stream()
				.filter(r -> FINISH_LOGGER.equals(r.getLoggerName()))
				.reduce((a, b) -> b)
				.orElse(null);
			assertNotNull(rec);
			assertEquals("debug formatter failed", rec.getMessage());
			assertFalse(rec.getMessage().contains("error-secret-in-basic"), rec.getMessage());
		} finally {
			target.setLevel(prevLevel);
		}
	}
}
