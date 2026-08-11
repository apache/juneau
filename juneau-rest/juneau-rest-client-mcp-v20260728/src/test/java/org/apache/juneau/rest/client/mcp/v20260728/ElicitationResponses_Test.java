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
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link ElicitationResponses}.
 */
class ElicitationResponses_Test extends TestBase {

	@Test void a01_toInputResponse_singleAnswer_encodesAcceptWithContent() {
		var result = new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("choice", "red");
		var encoded = ElicitationResponses.toInputResponse("q1", result);
		assertEquals(1, encoded.size());
		var q1 = (JsonMap) encoded.get("q1");
		assertEquals("accept", q1.getString("action"));
		assertEquals("red", q1.getMap("content").getString("choice"));
	}

	@Test void a02_toInputResponses_multipleAnswers_encodesAll() {
		var results = new LinkedHashMap<String,ElicitResult>();
		results.put("q1", new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("choice", "red"));
		results.put("q2", new ElicitResult().setAction(ElicitAction.DECLINE));
		var encoded = ElicitationResponses.toInputResponses(results);
		assertEquals(2, encoded.size());
		var q1 = (JsonMap) encoded.get("q1");
		assertEquals("accept", q1.getString("action"));
		assertEquals("red", q1.getMap("content").getString("choice"));
		var q2 = (JsonMap) encoded.get("q2");
		assertEquals("decline", q2.getString("action"));
		assertNull(q2.get("content"));
	}

	@Test void a03_toInputResponse_nullIdThrows() {
		var result = new ElicitResult();
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'id' cannot be null.",
			() -> ElicitationResponses.toInputResponse(null, result));
	}

	@Test void a04_toInputResponse_nullResultThrows() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'result' cannot be null.",
			() -> ElicitationResponses.toInputResponse("q1", null));
	}

	@Test void a05_toInputResponses_nullMapThrows() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'results' cannot be null.",
			() -> ElicitationResponses.toInputResponses(null));
	}

	@Test void a06_toInputResponses_nullEntryThrows() {
		var results = new LinkedHashMap<String,ElicitResult>();
		results.put("q1", new ElicitResult().setAction(ElicitAction.ACCEPT));
		results.put("q2", null);
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'results[q2]' cannot be null.",
			() -> ElicitationResponses.toInputResponses(results));
	}

	/**
	 * Documents the wire-shape symmetry between this client module's {@link ElicitationRequests}/
	 * {@link ElicitationResponses} pair and the server module's {@code ElicitationRequests}/
	 * {@code ElicitationResponses} (Phase 4) without introducing an illegal cross-module test dependency:
	 * {@code juneau-rest-client-mcp-v20260728} must not depend on {@code juneau-rest-server-mcp-v20260728}, so
	 * the server-side raw shape is hand-built here to match it exactly (verified by inspection/spec
	 * cross-reference against {@code ElicitationRequests.of}/{@code ElicitationResponses.get}, not a live
	 * cross-module call).
	 */
	@Test void a07_roundTrip_requestsToResponses_matchesServerSideHelpers() {
		// Mirrors the raw shape ElicitationRequests.of("q1", elicitRequest, ...) (server side) would place into
		// McpInputRequiredSignal.getInputRequests(), which callRaw ultimately surfaces as raw's "inputRequests".
		var rawInputRequests = JsonMap.of(
			"q1", JsonMap.of("message", "Pick one", "requestedSchema", JsonMap.of("type", "object"))
		);
		var raw = Map.<String,Object>of("resultType", "input_required", "requestState", "tok-abc", "inputRequests", rawInputRequests);

		var decodedRequests = ElicitationRequests.requests(raw);
		assertEquals("Pick one", decodedRequests.get("q1").getMessage());
		assertEquals("tok-abc", ElicitationRequests.requestState(raw));

		var answer = new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("choice", "red");
		var encodedResponses = ElicitationResponses.toInputResponse("q1", answer);

		// This is exactly the shape ElicitationResponses.get(ctx, "q1") (server side) decodes via
		// Json.to(Json.of(ctx.inputResponses().get("q1")), ElicitResult.class).
		var q1 = (JsonMap) encodedResponses.get("q1");
		assertEquals("accept", q1.getString("action"));
		assertEquals("red", q1.getMap("content").getString("choice"));
	}
}
