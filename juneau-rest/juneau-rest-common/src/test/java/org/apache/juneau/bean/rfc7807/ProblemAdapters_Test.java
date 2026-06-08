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
package org.apache.juneau.bean.rfc7807;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.rfc7807.adapter.*;
import org.apache.juneau.http.response.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link ProblemAdapters#fromException(BasicHttpException)} across the 8 most-common
 * {@link BasicHttpException} subclasses, plus null safety and the {@code detail}-vs-reason-phrase fallback.
 */
class ProblemAdapters_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: per-subclass status + title mapping
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_badRequest_noMessage() {
		var p = ProblemAdapters.fromException(new BadRequest());
		assertEquals(Integer.valueOf(400), p.getStatus());
		assertEquals("Bad Request", p.getTitle());
		assertNull(p.getDetail(), "detail should be null when message == reason phrase");
		assertNull(p.getType(), "type must not be synthesized");
	}

	@Test
	void a02_unauthorized_noMessage() {
		var p = ProblemAdapters.fromException(new Unauthorized());
		assertEquals(Integer.valueOf(401), p.getStatus());
		assertEquals("Unauthorized", p.getTitle());
		assertNull(p.getDetail());
	}

	@Test
	void a03_forbidden_noMessage() {
		var p = ProblemAdapters.fromException(new Forbidden());
		assertEquals(Integer.valueOf(403), p.getStatus());
		assertEquals("Forbidden", p.getTitle());
		assertNull(p.getDetail());
	}

	@Test
	void a04_notFound_noMessage() {
		var p = ProblemAdapters.fromException(new NotFound());
		assertEquals(Integer.valueOf(404), p.getStatus());
		assertEquals("Not Found", p.getTitle());
		assertNull(p.getDetail());
	}

	@Test
	void a05_conflict_noMessage() {
		var p = ProblemAdapters.fromException(new Conflict());
		assertEquals(Integer.valueOf(409), p.getStatus());
		assertEquals("Conflict", p.getTitle());
		assertNull(p.getDetail());
	}

	@Test
	void a06_internalServerError_noMessage() {
		var p = ProblemAdapters.fromException(new InternalServerError());
		assertEquals(Integer.valueOf(500), p.getStatus());
		assertEquals("Internal Server Error", p.getTitle());
		assertNull(p.getDetail());
	}

	@Test
	void a07_notImplemented_noMessage() {
		var p = ProblemAdapters.fromException(new NotImplemented());
		assertEquals(Integer.valueOf(501), p.getStatus());
		assertEquals("Not Implemented", p.getTitle());
		assertNull(p.getDetail());
	}

	@Test
	void a08_serviceUnavailable_noMessage() {
		var p = ProblemAdapters.fromException(new ServiceUnavailable());
		assertEquals(Integer.valueOf(503), p.getStatus());
		assertEquals("Service Unavailable", p.getTitle());
		assertNull(p.getDetail());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: with-message variants — detail should be populated when distinct from reason phrase
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_badRequest_withMessage() {
		var p = ProblemAdapters.fromException(new BadRequest("Field {0} is invalid", "balance"));
		assertEquals(Integer.valueOf(400), p.getStatus());
		assertEquals("Bad Request", p.getTitle());
		assertEquals("Field balance is invalid", p.getDetail());
	}

	@Test
	void b02_notFound_withFormatArgs() {
		var p = ProblemAdapters.fromException(new NotFound("Order {0} not found", 42));
		assertEquals(Integer.valueOf(404), p.getStatus());
		assertEquals("Not Found", p.getTitle());
		assertEquals("Order 42 not found", p.getDetail());
	}

	@Test
	void b03_internalServerError_withMessage() {
		var p = ProblemAdapters.fromException(new InternalServerError("DB down"));
		assertEquals(Integer.valueOf(500), p.getStatus());
		assertEquals("Internal Server Error", p.getTitle());
		assertEquals("DB down", p.getDetail());
	}

	@Test
	void b04_serviceUnavailable_withMessageMatchingReasonPhrase_omitsDetail() {
		// Explicit message that exactly matches the reason phrase should still be suppressed
		// (the bare-`new ServiceUnavailable()` case echoes the reason phrase via BasicHttpException.getMessage()).
		var p = ProblemAdapters.fromException(new ServiceUnavailable("Service Unavailable"));
		assertEquals(Integer.valueOf(503), p.getStatus());
		assertEquals("Service Unavailable", p.getTitle());
		assertNull(p.getDetail());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: with-cause variants — detail flows from cause.getMessage() when no explicit message is given
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_internalServerError_withCause() {
		var cause = new IllegalStateException("boom");
		var p = ProblemAdapters.fromException(new InternalServerError(cause));
		assertEquals(Integer.valueOf(500), p.getStatus());
		assertEquals("Internal Server Error", p.getTitle());
		assertEquals("boom", p.getDetail());
	}

	@Test
	void c02_notFound_withCauseAndMessage() {
		var cause = new IllegalArgumentException("ignored");
		var p = ProblemAdapters.fromException(new NotFound(cause, "Order {0} missing", 7));
		assertEquals(Integer.valueOf(404), p.getStatus());
		assertEquals("Not Found", p.getTitle());
		assertEquals("Order 7 missing", p.getDetail());
	}

	@Test
	void c03_forbidden_withNullCause() {
		var p = ProblemAdapters.fromException(new Forbidden((Throwable) null));
		assertEquals(Integer.valueOf(403), p.getStatus());
		assertEquals("Forbidden", p.getTitle());
		assertNull(p.getDetail());
	}

	@Test
	void c04_conflict_withCauseHavingNullMessage() {
		var cause = new RuntimeException((String) null);
		var p = ProblemAdapters.fromException(new Conflict(cause));
		assertEquals(Integer.valueOf(409), p.getStatus());
		assertEquals("Conflict", p.getTitle());
		// cause.getMessage() == null → BasicHttpException.getMessage() falls back to the reason phrase,
		// which the adapter then suppresses.
		assertNull(p.getDetail());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: null-input safety + type / instance never set
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_nullException_returnsNull() {
		assertNull(ProblemAdapters.fromException(null));
	}

	@Test
	void d02_type_isNeverSynthesized() {
		// Locked decision Q7: never set type (don't synthesize about:blank on the wire).
		var p = ProblemAdapters.fromException(new BadRequest("oops"));
		assertNull(p.getType());
		assertNull(p.getInstance());
	}

	@Test
	void d03_arbitrary_basicHttpException_status() {
		// Direct BasicHttpException construction with a custom status code that doesn't have a dedicated subclass.
		var p = ProblemAdapters.fromException(new BasicHttpException(418, "I'm a teapot", "Short and stout"));
		assertEquals(Integer.valueOf(418), p.getStatus());
		assertEquals("I'm a teapot", p.getTitle());
		assertEquals("Short and stout", p.getDetail());
	}
}
