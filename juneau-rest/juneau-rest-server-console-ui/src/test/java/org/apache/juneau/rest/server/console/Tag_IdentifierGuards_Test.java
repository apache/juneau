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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Phase 4 gate: {@link Tag#of(String, String)} identifier REJECT guards.
 *
 * <p>
 * Written first against the deliberately-unvalidated {@code Tag} skeleton (proving the guard is genuinely absent,
 * RED), then again once the lowercase-then-anchored-match guard is added (GREEN).
 */
class Tag_IdentifierGuards_Test extends TestBase {

	@Test void a01_scriptTagValue_rejects() {
		assertThrows(IllegalArgumentException.class, () -> Tag.of("status", "<script>"));
	}

	@Test void a02_emptyValue_rejects() {
		assertThrows(IllegalArgumentException.class, () -> Tag.of("status", ""));
	}

	@Test void a03_emptyDomain_rejects() {
		assertThrows(IllegalArgumentException.class, () -> Tag.of("", "released"));
	}

	/** Uppercase does NOT reject - it lowercases to a legal value first. Do not write this as a false-negative "throws" test. */
	@Test void a04_uppercaseDomain_lowercasesRatherThanRejecting() {
		var span = Tag.of("Status", "x");
		assertEquals("tag status x", span.getAttr(String.class, "class"));
	}

	@Test void a05_positiveControl_rendersExpectedClasses() {
		var span = Tag.of("status", "released");
		assertEquals("tag status released", span.getAttr(String.class, "class"));
	}
}
