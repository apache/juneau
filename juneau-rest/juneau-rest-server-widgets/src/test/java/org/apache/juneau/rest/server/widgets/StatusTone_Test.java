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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link StatusTone} palette, wire-token and validity contract.
 */
class StatusTone_Test extends TestBase {

	@Test void a01_wireTokens_areTheFiveTones() {
		assertList(StatusTone.WIRE_TOKENS, "info", "success", "warning", "error", "neutral");
	}

	@Test void a02_isValid() {
		for (var t : StatusTone.values())
			assertTrue(StatusTone.isValid(t.wire()), t.name());
		for (var s : new String[]{null, "", "INFO", "warn", "danger", "accent"})
			assertFalse(StatusTone.isValid(s), String.valueOf(s));
	}

	@Test void a03_wireTokens_areUnmodifiable() {
		assertThrows(UnsupportedOperationException.class, () -> StatusTone.WIRE_TOKENS.add("x"));
	}
}
