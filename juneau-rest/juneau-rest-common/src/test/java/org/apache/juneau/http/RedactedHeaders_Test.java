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
package org.apache.juneau.http;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

class RedactedHeaders_Test {

	@Test void a01_defaultSetContainsKnownCredentialHeaders() {
		assertTrue(RedactedHeaders.DEFAULT.contains("Authorization"));
		assertTrue(RedactedHeaders.DEFAULT.contains("Cookie"));
		assertTrue(RedactedHeaders.DEFAULT.contains("Set-Cookie"));
		assertTrue(RedactedHeaders.DEFAULT.contains("Proxy-Authorization"));
		assertTrue(RedactedHeaders.DEFAULT.contains("X-API-Key"));
		assertEquals(5, RedactedHeaders.DEFAULT.size());
	}

	@Test void a02_redactedSentinel() {
		assertEquals("[REDACTED]", RedactedHeaders.REDACTED);
	}

	@Test void b01_isSensitive_defaultSet_caseInsensitive() {
		assertTrue(RedactedHeaders.isSensitive("Authorization"));
		assertTrue(RedactedHeaders.isSensitive("authorization"));
		assertTrue(RedactedHeaders.isSensitive("AUTHORIZATION"));
		assertTrue(RedactedHeaders.isSensitive("cookie"));
		assertTrue(RedactedHeaders.isSensitive("SET-COOKIE"));
		assertTrue(RedactedHeaders.isSensitive("proxy-authorization"));
		assertTrue(RedactedHeaders.isSensitive("x-api-key"));
	}

	@Test void b02_isSensitive_nonSensitiveHeaderUntouched() {
		assertFalse(RedactedHeaders.isSensitive("User-Agent"));
		assertFalse(RedactedHeaders.isSensitive("Content-Type"));
		assertFalse(RedactedHeaders.isSensitive("X-Request-Id"));
	}

	@Test void b03_isSensitive_nullName() {
		assertFalse(RedactedHeaders.isSensitive(null));
		assertFalse(RedactedHeaders.isSensitive(null, RedactedHeaders.DEFAULT));
	}

	@Test void b04_isSensitive_customSet() {
		var names = Set.of("X-Internal-Trace");
		assertTrue(RedactedHeaders.isSensitive("x-internal-trace", names));
		assertFalse(RedactedHeaders.isSensitive("Authorization", names), "custom set replaces, does not extend, DEFAULT");
	}

	@Test void b05_isSensitive_nullOrEmptyNames() {
		assertFalse(RedactedHeaders.isSensitive("Authorization", null));
		assertFalse(RedactedHeaders.isSensitive("Authorization", Set.of()));
	}

	@Test void b06_isSensitive_setWithNullElementsIgnored() {
		var names = new LinkedHashSet<String>();
		names.add(null);
		names.add("Authorization");
		assertTrue(RedactedHeaders.isSensitive("authorization", names));
	}

	@Test void c01_redact_defaultSet_masksSensitiveValue() {
		assertEquals("[REDACTED]", RedactedHeaders.redact("Authorization", "Bearer abc123"));
		assertEquals("[REDACTED]", RedactedHeaders.redact("cookie", "session=abc"));
	}

	@Test void c02_redact_defaultSet_leavesNonSensitiveValueUntouched() {
		assertEquals("curl/8.0", RedactedHeaders.redact("User-Agent", "curl/8.0"));
		assertEquals("application/json", RedactedHeaders.redact("Content-Type", "application/json"));
	}

	@Test void c03_redact_nullValuePassesThroughWhenNotSensitive() {
		assertNull(RedactedHeaders.redact("User-Agent", null));
	}

	@Test void c04_redact_nullNamePassesValueThrough() {
		assertEquals("hello", RedactedHeaders.redact(null, "hello"));
	}

	@Test void c05_redact_customSet() {
		var names = Set.of("X-Internal-Trace");
		assertEquals("[REDACTED]", RedactedHeaders.redact("X-INTERNAL-TRACE", "abc-trace-id", names));
		assertEquals("Bearer abc123", RedactedHeaders.redact("Authorization", "Bearer abc123", names));
	}
}
