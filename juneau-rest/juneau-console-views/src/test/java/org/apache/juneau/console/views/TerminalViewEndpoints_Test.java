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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.response.*;
import org.junit.jupiter.api.*;

class TerminalViewEndpoints_Test extends TestBase {

	@Test void a01_checkRunId() {
		assertEquals("run-1", TerminalViewEndpoints.checkRunId("run-1"));
		assertThrows(BadRequest.class, () -> TerminalViewEndpoints.checkRunId("../x"));
		assertThrows(BadRequest.class, () -> TerminalViewEndpoints.checkRunId(null));
		var tooLong = "x".repeat(129);
		assertThrows(BadRequest.class, () -> TerminalViewEndpoints.checkRunId(tooLong));
	}

	@Test void a02_caps() {
		assertEquals(2000, TerminalViewEndpoints.MAX_ENDPOINT_PAGE_EVENTS);
		assertTrue(TerminalViewEndpoints.MAX_ENDPOINT_PAGE_EVENTS <= TerminalViewPage.MAX_PAGE_EVENTS);
	}
}
