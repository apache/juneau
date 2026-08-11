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
package org.apache.juneau.rest.client.mcp.v20260728;

import static org.apache.juneau.BasicTestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link ElicitationRequests}.
 */
class ElicitationRequests_Test extends TestBase {

	@Test void a01_isInputRequired_trueOnInputRequiredResultType() {
		assertTrue(ElicitationRequests.isInputRequired(Map.of("resultType", "input_required")));
	}

	@Test void a02_isInputRequired_falseOnCompleteResultType() {
		assertFalse(ElicitationRequests.isInputRequired(Map.of("resultType", "complete")));
	}

	@Test void a03_isInputRequired_nullRawThrows() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'raw' cannot be null.",
			() -> ElicitationRequests.isInputRequired(null));
	}

	@Test void a04_requests_decodesTypedElicitRequestsFromRawMap() {
		var raw = Map.<String,Object>of(
			"resultType", "input_required",
			"inputRequests", JsonMap.of(
				"q1", JsonMap.of(
					"message", "Pick",
					"requestedSchema", JsonMap.of(
						"type", "object",
						"properties", JsonMap.of("choice", JsonMap.of("type", "string"))
					)
				)
			)
		);
		var requests = ElicitationRequests.requests(raw);
		var q1 = requests.get("q1");
		assertEquals("Pick", q1.getMessage());
		assertNotNull(q1.getRequestedSchema());
	}

	@Test void a05_requests_missingInputRequestsReturnsEmptyMap() {
		var requests = ElicitationRequests.requests(Map.of("resultType", "complete"));
		assertNotNull(requests);
		assertTrue(requests.isEmpty());
	}

	@Test void a06_requestState_returnsEchoedToken() {
		assertEquals("tok-abc", ElicitationRequests.requestState(Map.of("resultType", "input_required", "requestState", "tok-abc")));
	}

	@Test void a07_requestState_missingReturnsNull() {
		assertNull(ElicitationRequests.requestState(Map.of("resultType", "complete")));
	}

	@Test void a08_requests_malformedEntryThrows() {
		// A JSON-array entry cannot be converted to a bean target: the marshaller throws (which a caller's own
		// error handling surfaces). Mirrors ElicitationResponses_Test's a05_toInputResponses_nullEntryThrows-style
		// server-side decode direction coverage.
		var raw = Map.<String,Object>of("resultType", "input_required", "inputRequests", JsonMap.of("q1", JsonList.of(1, 2)));
		assertThrows(RuntimeException.class, () -> ElicitationRequests.requests(raw));
	}

	@Test void a09_isInputRequired_falseOnMissingResultType() {
		assertFalse(ElicitationRequests.isInputRequired(Map.of("foo", "bar")));
	}

	@Test void a10_requests_nullEntryDecodesToNull() {
		// Mirrors ElicitationResponses.get()'s explicit null handling (server side): a null raw value decodes
		// to a null typed entry rather than round-tripping through the marshaller.
		var rawInputRequests = new LinkedHashMap<String,Object>();
		rawInputRequests.put("q1", null);
		var raw = Map.<String,Object>of("resultType", "input_required", "inputRequests", rawInputRequests);
		var requests = ElicitationRequests.requests(raw);
		assertTrue(requests.containsKey("q1"));
		assertNull(requests.get("q1"));
	}
}
