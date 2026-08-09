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
package org.apache.juneau.rest.server.logger;

import static org.apache.juneau.rest.server.logger.CallLoggingDetail.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link CallLogger} — request/response header redaction and status-based rule matching.
 *
 * @since 10.0.0
 */
class CallLogger_Test extends TestBase {

	private static HttpServletRequest req(Map<String,String> headers) {
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn("GET");
		when(r.getRequestURI()).thenReturn("/foo");
		when(r.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
		for (var e : headers.entrySet())
			when(r.getHeader(e.getKey())).thenReturn(e.getValue());
		return r;
	}

	private static HttpServletResponse res(int status, Map<String,String> headers) {
		var r = mock(HttpServletResponse.class);
		when(r.getStatus()).thenReturn(status);
		when(r.getHeaderNames()).thenReturn(headers.keySet());
		for (var e : headers.entrySet())
			when(r.getHeader(e.getKey())).thenReturn(e.getValue());
		return r;
	}

	private static final class CapturingLogger extends CallLogger {
		String captured;

		CapturingLogger(Builder builder) {
			super(builder);
		}

		@Override
		protected void log(Level level, String msg, Throwable e) {
			captured = msg;
		}
	}

	private static CallLogger.Builder builder() {
		return CallLogger.create(BasicBeanStore.INSTANCE)
			.requestDetail(HEADER)
			.responseDetail(HEADER)
			.enabled(Enablement.ALWAYS)
			.level(Level.SEVERE);
	}

	private static CapturingLogger logger() {
		return new CapturingLogger(builder());
	}

	@Test void a01_requestHeader_authorization_isRedacted() {
		var l = logger();
		l.log(req(Map.of("Authorization", "Bearer super-secret-token")), res(500, Map.of()));
		assertTrue(l.captured.contains("Authorization: " + RedactedHeaders.REDACTED));
		assertFalse(l.captured.contains("super-secret-token"));
	}

	@Test void a02_requestHeader_cookie_isRedacted() {
		var l = logger();
		l.log(req(Map.of("Cookie", "session=abc123")), res(500, Map.of()));
		assertTrue(l.captured.contains("Cookie: " + RedactedHeaders.REDACTED));
		assertFalse(l.captured.contains("abc123"));
	}

	@Test void a03_requestHeader_apiKey_isRedacted() {
		var l = logger();
		l.log(req(Map.of("X-API-Key", "key-12345")), res(500, Map.of()));
		assertTrue(l.captured.contains("X-API-Key: " + RedactedHeaders.REDACTED));
		assertFalse(l.captured.contains("key-12345"));
	}

	@Test void a04_responseHeader_setCookie_isRedacted() {
		var l = logger();
		l.log(req(Map.of()), res(500, Map.of("Set-Cookie", "session=xyz789")));
		assertTrue(l.captured.contains("Set-Cookie: " + RedactedHeaders.REDACTED));
		assertFalse(l.captured.contains("xyz789"));
	}

	@Test void a05_nonSensitiveHeader_isNotRedacted() {
		var l = logger();
		l.log(req(Map.of("User-Agent", "curl/8.0")), res(500, Map.of()));
		assertTrue(l.captured.contains("User-Agent: curl/8.0"));
	}

	@Test void a06_redactedHeaders_emptyArray_disablesRedaction() {
		var l = new CapturingLogger(builder().redactedHeaders());
		l.log(req(Map.of("Authorization", "Bearer super-secret-token")), res(500, Map.of()));
		assertTrue(l.captured.contains("Authorization: Bearer super-secret-token"));
	}

	@Test void a07_redactHeader_addsCustomSensitiveName() {
		var l = new CapturingLogger(builder().redactHeader("X-Internal-Token"));
		l.log(req(Map.of("X-Internal-Token", "internal-secret", "Authorization", "Bearer tok")), res(500, Map.of()));
		assertTrue(l.captured.contains("X-Internal-Token: " + RedactedHeaders.REDACTED));
		assertTrue(l.captured.contains("Authorization: " + RedactedHeaders.REDACTED));
		assertFalse(l.captured.contains("internal-secret"));
	}

	@Test void a08_statusLineDetail_doesNotIncludeHeaders() {
		var l = new CapturingLogger(CallLogger.create(BasicBeanStore.INSTANCE)
			.requestDetail(STATUS_LINE)
			.responseDetail(STATUS_LINE)
			.enabled(Enablement.ALWAYS)
			.level(Level.SEVERE));
		l.log(req(Map.of("Authorization", "Bearer super-secret-token")), res(500, Map.of()));
		assertFalse(l.captured.contains("Authorization"));
		assertFalse(l.captured.contains("super-secret-token"));
	}
}
