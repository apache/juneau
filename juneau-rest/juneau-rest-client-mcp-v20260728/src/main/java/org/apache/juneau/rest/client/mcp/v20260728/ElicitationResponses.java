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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * Static helper for encoding the client's answers to server&rarr;client MCP {@code 2026-07-28} SEP-2322
 * elicitation requests back into the raw {@code inputResponses} wire shape a resume call expects (MRTR,
 * the Multi-Round-Trip Requests loop, client side).
 *
 * <p>
 * Paired with {@link ElicitationRequests}, which reads the server's questions out of a paused
 * {@link McpClient#callRaw} result &mdash; the same {@code ElicitationRequests}/{@code ElicitationResponses}
 * split the server side already uses (server {@code ElicitationRequests} builds the pause signal; server
 * {@code ElicitationResponses} reads the resumed answers).
 *
 * @since 10.0.0
 */
public final class ElicitationResponses {

	private ElicitationResponses() {}

	/**
	 * Encodes a single typed {@link ElicitResult} into the raw {@code inputResponses} payload shape.
	 *
	 * @param id The server-assigned id this answer responds to.  Must not be <jk>null</jk>.
	 * @param result The typed answer.  Must not be <jk>null</jk>.
	 * @return A single-entry map ready to pass to {@link #toInputResponses(Map)}'s multi-answer form, or to hand
	 * 	directly to a concrete request bean's {@code setInputResponses(...)} after merging with other answers.
	 * @throws IllegalArgumentException If {@code id} or {@code result} is <jk>null</jk>.
	 */
	public static Map<String,Object> toInputResponse(String id, ElicitResult result) {
		assertArgNotNull("id", id);
		assertArgNotNull("result", result);
		return toInputResponses(Map.of(id, result));
	}

	/**
	 * Encodes several typed {@link ElicitResult}s into the raw {@code inputResponses} payload shape a resume
	 * call's request bean expects (e.g. {@code CallToolRequest.setInputResponses(...)}).
	 *
	 * <p>
	 * Unlike {@code ElicitationRequests.of(Map,Object)} (server side), an empty {@code results} map is accepted
	 * (returning an empty map), not rejected &mdash; deliberately asymmetric, since a caller may legitimately
	 * resume with zero elicitation answers if a round only carried non-elicitation MRTR pauses, whereas building
	 * a signal with zero questions server-side would be a pointless pause.
	 *
	 * @param results Server-assigned-id-keyed typed answers.  Must not be <jk>null</jk>, and no value may be
	 * 	<jk>null</jk>.  Output iteration order mirrors this map's order, so a caller wanting deterministic
	 * 	ordering should pass a {@link LinkedHashMap}.
	 * @return A keyed map of raw, wire-shaped answers.  Never <jk>null</jk> (empty if {@code results} was empty).
	 * @throws IllegalArgumentException If {@code results} is <jk>null</jk>, or any value in it is <jk>null</jk>.
	 */
	public static Map<String,Object> toInputResponses(Map<String,ElicitResult> results) {
		assertArgNotNull("results", results);
		Map<String,Object> out = new LinkedHashMap<>();
		results.forEach((id, result) -> {
			assertArgNotNull("results[" + id + "]", result);
			// Pre-marshalled to JsonMap here (rather than left as the typed ElicitResult for McpClient.call's
			// own toWireParams flattening) so that (a) a null-check on the encoded shape is meaningful even
			// when this helper is used standalone, outside McpClient.call's flow, and (b) the returned map is
			// immediately wire-ready for a caller who hands it straight to a concrete request bean's
			// setInputResponses(...) without ever going through McpClient at all.
			out.put(id, Json.to(Json.of(result), JsonMap.class));
		});
		return out;
	}
}
