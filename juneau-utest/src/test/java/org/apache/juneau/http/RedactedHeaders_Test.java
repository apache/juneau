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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that header values commonly carrying credentials are masked before being written somewhere
 * observable, while non-sensitive headers pass through unchanged.
 */
class RedactedHeaders_Test extends TestBase {

	@Test void a01_sensitiveNamesDetectedCaseInsensitively() {
		assertTrue(RedactedHeaders.isSensitive("Authorization"));
		assertTrue(RedactedHeaders.isSensitive("authorization"));
		assertTrue(RedactedHeaders.isSensitive("Cookie"));
		assertTrue(RedactedHeaders.isSensitive("Set-Cookie"));
	}

	@Test void a02_nonSensitiveNamesPassThrough() {
		assertFalse(RedactedHeaders.isSensitive("Accept"));
		assertFalse(RedactedHeaders.isSensitive("Content-Type"));
		assertFalse(RedactedHeaders.isSensitive(null));
	}

	@Test void a03_redactMasksSensitiveValues() {
		assertEquals(RedactedHeaders.REDACTED, RedactedHeaders.redact("Authorization", "Bearer secret"));
		assertEquals("text/plain", RedactedHeaders.redact("Content-Type", "text/plain"));
	}

	@Test void a04_customNameSet() {
		var names = Set.of("X-Trace-Token");
		assertEquals(RedactedHeaders.REDACTED, RedactedHeaders.redact("X-Trace-Token", "abc", names));
		assertEquals("Bearer secret", RedactedHeaders.redact("Authorization", "Bearer secret", names));
	}
}
