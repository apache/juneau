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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link ElicitationAccess}.
 */
class ElicitationAccess_Test {

	@Test void a01_isInputRequired_trueOnInputRequiredResultType() {
		assertTrue(ElicitationAccess.isInputRequired(Map.of("resultType", "input_required")));
	}

	@Test void a02_isInputRequired_falseOnCompleteResultType() {
		assertFalse(ElicitationAccess.isInputRequired(Map.of("resultType", "complete")));
	}

	@Test void a03_isInputRequired_nullRawThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationAccess.isInputRequired(null));
		assertEquals("Argument 'raw' cannot be null.", e.getMessage());
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
		var requests = ElicitationAccess.requests(raw);
		var q1 = requests.get("q1");
		assertEquals("Pick", q1.getMessage());
		assertNotNull(q1.getRequestedSchema());
	}

	@Test void a05_requests_missingInputRequestsReturnsEmptyMap() {
		var requests = ElicitationAccess.requests(Map.of("resultType", "complete"));
		assertNotNull(requests);
		assertTrue(requests.isEmpty());
	}

	@Test void a06_requestState_returnsEchoedToken() {
		assertEquals("tok-abc", ElicitationAccess.requestState(Map.of("resultType", "input_required", "requestState", "tok-abc")));
	}

	@Test void a07_requestState_missingReturnsNull() {
		assertNull(ElicitationAccess.requestState(Map.of("resultType", "complete")));
	}

	@Test void a08_toInputResponse_singleAnswer_encodesAcceptWithContent() {
		var result = new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("choice", "red");
		var encoded = ElicitationAccess.toInputResponse("q1", result);
		assertEquals(1, encoded.size());
		var q1 = (JsonMap) encoded.get("q1");
		assertEquals("accept", q1.getString("action"));
		assertEquals("red", q1.getMap("content").getString("choice"));
	}

	@Test void a09_toInputResponses_multipleAnswers_encodesAll() {
		var results = new LinkedHashMap<String,ElicitResult>();
		results.put("q1", new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("choice", "red"));
		results.put("q2", new ElicitResult().setAction(ElicitAction.DECLINE));
		var encoded = ElicitationAccess.toInputResponses(results);
		assertEquals(2, encoded.size());
		var q1 = (JsonMap) encoded.get("q1");
		assertEquals("accept", q1.getString("action"));
		assertEquals("red", q1.getMap("content").getString("choice"));
		var q2 = (JsonMap) encoded.get("q2");
		assertEquals("decline", q2.getString("action"));
		assertNull(q2.get("content"));
	}

	@Test void a10_toInputResponse_nullIdThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationAccess.toInputResponse(null, new ElicitResult()));
		assertEquals("Argument 'id' cannot be null.", e.getMessage());
	}

	@Test void a11_toInputResponse_nullResultThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationAccess.toInputResponse("q1", null));
		assertEquals("Argument 'result' cannot be null.", e.getMessage());
	}

	@Test void a12_toInputResponses_nullMapThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationAccess.toInputResponses(null));
		assertEquals("Argument 'results' cannot be null.", e.getMessage());
	}

	/**
	 * Documents the wire-shape symmetry between this client module's {@link ElicitationAccess} and the server
	 * module's {@code ElicitationRequests}/{@code ElicitationResponses} (Phase 4) without introducing an illegal
	 * cross-module test dependency: {@code juneau-rest-client-mcp-v20260728} must not depend on
	 * {@code juneau-rest-server-mcp-v20260728}, so the server-side raw shape is hand-built here to match it
	 * exactly (verified by inspection/spec cross-reference against {@code ElicitationRequests.of}/
	 * {@code ElicitationResponses.get}, not a live cross-module call).
	 */
	@Test void a13_roundTrip_requestsToResponses_matchesServerSideHelpers() {
		// Mirrors the raw shape ElicitationRequests.of("q1", elicitRequest, ...) would place into
		// McpInputRequiredSignal.getInputRequests(), which callRaw ultimately surfaces as raw's "inputRequests".
		var rawInputRequests = JsonMap.of(
			"q1", JsonMap.of("message", "Pick one", "requestedSchema", JsonMap.of("type", "object"))
		);
		var raw = Map.<String,Object>of("resultType", "input_required", "requestState", "tok-abc", "inputRequests", rawInputRequests);

		var decodedRequests = ElicitationAccess.requests(raw);
		assertEquals("Pick one", decodedRequests.get("q1").getMessage());
		assertEquals("tok-abc", ElicitationAccess.requestState(raw));

		var answer = new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("choice", "red");
		var encodedResponses = ElicitationAccess.toInputResponse("q1", answer);

		// This is exactly the shape ElicitationResponses.get(ctx, "q1") decodes via
		// Json.to(Json.of(ctx.inputResponses().get("q1")), ElicitResult.class).
		var q1 = (JsonMap) encodedResponses.get("q1");
		assertEquals("accept", q1.getString("action"));
		assertEquals("red", q1.getMap("content").getString("choice"));
	}

	@Test void a14_requests_malformedEntryThrows() {
		// A JSON-array entry cannot be converted to a bean target: the marshaller throws (which a caller's own
		// error handling surfaces). Mirrors ElicitationResponses_Test's a09_get_malformedShapeThrows for the
		// server-side decode direction.
		var raw = Map.<String,Object>of("resultType", "input_required", "inputRequests", JsonMap.of("q1", JsonList.of(1, 2)));
		assertThrows(RuntimeException.class, () -> ElicitationAccess.requests(raw));
	}

	@Test void a15_toInputResponses_nullEntryThrows() {
		var results = new LinkedHashMap<String,ElicitResult>();
		results.put("q1", new ElicitResult().setAction(ElicitAction.ACCEPT));
		results.put("q2", null);
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationAccess.toInputResponses(results));
		assertEquals("Argument 'results[q2]' cannot be null.", e.getMessage());
	}

	@Test void a16_isInputRequired_falseOnMissingResultType() {
		assertFalse(ElicitationAccess.isInputRequired(Map.of("foo", "bar")));
	}

	@Test void a17_requests_nullEntryDecodesToNull() {
		// Mirrors ElicitationResponses.get()'s explicit null handling: a null raw value decodes to a null
		// typed entry rather than round-tripping through the marshaller.
		var rawInputRequests = new LinkedHashMap<String,Object>();
		rawInputRequests.put("q1", null);
		var raw = Map.<String,Object>of("resultType", "input_required", "inputRequests", rawInputRequests);
		var requests = ElicitationAccess.requests(raw);
		assertTrue(requests.containsKey("q1"));
		assertNull(requests.get("q1"));
	}
}
