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
package org.apache.juneau.commons.http;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link SafePathTemplate} &mdash; the shared same-origin / non-templated path predicates, the
 * poll-interval floor, and the calendar path-template / document-URL helpers layered on the same core.
 */
class SafePathTemplate_Test extends TestBase {

	@Test void a01_minPollInterval_is5000() {
		assertEquals(5_000L, SafePathTemplate.MIN_POLL_INTERVAL_MS);
	}

	@Test void a02_sameOrigin_accepts() {
		assertTrue(SafePathTemplate.isSameOriginPath("/data/alerts"));
		assertTrue(SafePathTemplate.isSameOriginPath("data/alerts"));
		assertTrue(SafePathTemplate.isSameOriginPath("/data/alerts/{id}"));   // templated is still same-origin
		assertTrue(SafePathTemplate.isSameOriginPath("/events/{year}/{month}"));
		assertTrue(SafePathTemplate.isSameOriginPath("/events/123?x=1#frag"));
	}

	@Test void a03_sameOrigin_rejects() {
		assertFalse(SafePathTemplate.isSameOriginPath(null));
		assertFalse(SafePathTemplate.isSameOriginPath(""));
		assertFalse(SafePathTemplate.isSameOriginPath("   "));
		assertFalse(SafePathTemplate.isSameOriginPath("https://evil/x"));
		assertFalse(SafePathTemplate.isSameOriginPath("//evil/x"));
		assertFalse(SafePathTemplate.isSameOriginPath("servlet:/data/x"));
		assertFalse(SafePathTemplate.isSameOriginPath("javascript:alert(1)"));
		assertFalse(SafePathTemplate.isSameOriginPath("/data/../x"));
		assertFalse(SafePathTemplate.isSameOriginPath("a:b"));
		assertFalse(SafePathTemplate.isSameOriginPath(".."));
	}

	@Test void a04_nonTemplated_acceptsFixedPathOnly() {
		assertTrue(SafePathTemplate.isNonTemplatedPath("/data/alerts"));
		assertTrue(SafePathTemplate.isNonTemplatedPath("data/alerts"));
	}

	@Test void a05_nonTemplated_rejectsTemplateAndUnsafe() {
		assertFalse(SafePathTemplate.isNonTemplatedPath("/data/alerts/{id}"));
		assertFalse(SafePathTemplate.isNonTemplatedPath("/cards/{cardId}"));
		assertFalse(SafePathTemplate.isNonTemplatedPath("/{anything}"));
		assertFalse(SafePathTemplate.isNonTemplatedPath("https://evil/x"));
		assertFalse(SafePathTemplate.isNonTemplatedPath("//evil/x"));
		assertFalse(SafePathTemplate.isNonTemplatedPath("/data/../x"));
		assertFalse(SafePathTemplate.isNonTemplatedPath(null));
	}

	@Test void a06_clampPollInterval_floors() {
		assertEquals(5_000L, SafePathTemplate.clampPollInterval(0L));
		assertEquals(5_000L, SafePathTemplate.clampPollInterval(1_000L));
		assertEquals(5_000L, SafePathTemplate.clampPollInterval(5_000L));
		assertEquals(30_000L, SafePathTemplate.clampPollInterval(30_000L));
	}

	@Test void a07_safeTemplate_requiresTokens() {
		assertTrue(SafePathTemplate.isSafeTemplate("/events/{year}/{month}", "{year}", "{month}"));
		assertTrue(SafePathTemplate.isSafeTemplate("/e?y={year}&m={month}", "{year}", "{month}"));
		assertTrue(SafePathTemplate.isSafeTemplate("/events/all"));
		assertFalse(SafePathTemplate.isSafeTemplate("/events/{year}", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate("/events/{month}", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate("/events/all", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate("http://evil/{year}/{month}", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate("//evil/{year}/{month}", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate("../{year}/{month}", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate(null, "{year}"));
	}

	@Test void a08_safeDocumentUrl() {
		assertTrue(SafePathTemplate.isSafeDocumentUrl("/events/123"));
		assertTrue(SafePathTemplate.isSafeDocumentUrl("/events/123?x=1"));
		assertTrue(SafePathTemplate.isSafeDocumentUrl("/events/123#frag"));
		assertTrue(SafePathTemplate.isSafeDocumentUrl("events/123"));
		assertFalse(SafePathTemplate.isSafeDocumentUrl("https://evil/1"));
		assertFalse(SafePathTemplate.isSafeDocumentUrl("//evil/1"));
		assertFalse(SafePathTemplate.isSafeDocumentUrl("javascript:alert(1)"));
		assertFalse(SafePathTemplate.isSafeDocumentUrl("/x/../../etc/passwd"));
		assertFalse(SafePathTemplate.isSafeDocumentUrl(null));
	}
}
