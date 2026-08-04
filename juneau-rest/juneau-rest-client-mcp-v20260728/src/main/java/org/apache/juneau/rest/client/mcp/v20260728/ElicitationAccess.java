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
 * Static helper for driving MCP {@code 2026-07-28} SEP-2322 elicitation over {@link McpClient#callRaw}'s raw
 * {@code Map<String,Object>} result (MRTR, TODO-318's Multi-Round-Trip Requests loop, client side).
 *
 * <p>
 * Works at the raw map/JSON level rather than exposing typed overloads per concrete request bean, because
 * {@code CallToolRequest}/{@code GetPromptRequest}/{@code ReadResourceRequest} share no common
 * "has-inputResponses-and-requestState" interface (each independently declares its own pair of fields; their
 * actual common base, {@code RequestParams<T>}, carries only {@code _meta}). A caller resuming, say, a
 * {@code CallToolRequest} calls {@link #toInputResponses(Map)} for the raw payload, then makes its own
 * {@code .setInputResponses(...).setRequestState(...)} call on the concrete bean it already knows it holds.
 *
 * @since 10.0.0
 */
public final class ElicitationAccess {

	private ElicitationAccess() {}

	/**
	 * Whether a {@link McpClient#callRaw} result represents a paused elicitation (or any other MRTR pause).
	 *
	 * @param raw The raw result from {@link McpClient#callRaw}.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if {@code raw.get("resultType")} is {@code "input_required"}.
	 * @throws IllegalArgumentException If {@code raw} is <jk>null</jk>.
	 */
	public static boolean isInputRequired(Map<String,Object> raw) {
		assertArgNotNull("raw", raw);
		return "input_required".equals(raw.get("resultType"));
	}

	/**
	 * Decodes {@code raw}'s {@code inputRequests} into typed {@link ElicitRequest}s.
	 *
	 * <p>
	 * Each raw value is generic JSON (a {@code Map} after wire deserialization, per {@code InputRequiredResult}'s
	 * lossless-map contract), converted via the same {@code Json.to(Json.of(value), type)} technique
	 * {@code McpMrtrResumeContext.continuationAs(Class)} demonstrates server-side. A value that is present but
	 * <jk>null</jk> decodes to a <jk>null</jk> entry (see the return doc); this mirrors
	 * {@code ElicitationResponses.all(McpMrtrResumeContext)}'s null-passthrough on the server side.
	 *
	 * <p>
	 * <b>Every</b> entry is decoded as an {@link ElicitRequest}, unconditionally &mdash; per
	 * {@code InputRequiredResult}'s Javadoc, {@code inputRequests} entries may in principle be arbitrary
	 * sub-request kinds (sampling/roots/elicitation), not only elicitation. A non-elicitation entry (one that
	 * shares no field names with {@link ElicitRequest}) simply decodes to an all-<jk>null</jk>-fields
	 * {@link ElicitRequest}, since the underlying {@code Json.to} conversion tolerates unknown/missing bean
	 * properties; it is the caller's responsibility to only call this for elicitation-flavored MRTR pauses.
	 *
	 * @param raw The raw result from {@link McpClient#callRaw}.  Must not be <jk>null</jk>.
	 * @return A keyed map of typed requests.  Never <jk>null</jk> (empty if {@code raw} carried none). Values
	 * 	may be <jk>null</jk> when the corresponding raw entry is absent/<jk>null</jk>.
	 * @throws IllegalArgumentException If {@code raw} is <jk>null</jk>.
	 * @throws ClassCastException If {@code raw.get("inputRequests")} is present but is not, at runtime, a
	 * 	{@code Map} &mdash; which can only happen against a malformed/non-conforming remote response, since a
	 * 	well-formed one always deserializes {@code inputRequests} to a {@code Map} per JSON semantics.
	 * @throws RuntimeException If any entry's decoded shape cannot be converted to {@link ElicitRequest}.
	 */
	@SuppressWarnings({
		"unchecked" // raw.get("inputRequests") deserializes to a Map<String,Object> from JSON for any well-formed remote response; a malformed one surfaces as ClassCastException here (see @throws), not a silently-wrong cast.
	})
	public static Map<String,ElicitRequest> requests(Map<String,Object> raw) {
		assertArgNotNull("raw", raw);
		var rawRequests = (Map<String,Object>) raw.get("inputRequests");
		Map<String,ElicitRequest> out = new LinkedHashMap<>();
		if (rawRequests != null)
			rawRequests.forEach((id, value) -> out.put(id, value == null ? null : Json.to(Json.of(value), ElicitRequest.class)));
		return out;
	}

	/**
	 * Returns {@code raw}'s echoed continuation token.
	 *
	 * @param raw The raw result from {@link McpClient#callRaw}.  Must not be <jk>null</jk>.
	 * @return The token, or <jk>null</jk> if absent.
	 * @throws IllegalArgumentException If {@code raw} is <jk>null</jk>.
	 * @throws ClassCastException If {@code raw.get("requestState")} is present but is not, at runtime, a
	 * 	{@code String} &mdash; only possible against a malformed/non-conforming remote response.
	 */
	public static String requestState(Map<String,Object> raw) {
		assertArgNotNull("raw", raw);
		return (String) raw.get("requestState");
	}

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
	 * Unlike {@code ElicitationRequests.of(Map,Object)} (server side), an empty {@code results} map is accepted (returning
	 * an empty map), not rejected &mdash; deliberately asymmetric, since a caller may legitimately resume with
	 * zero elicitation answers if a round only carried non-elicitation MRTR pauses, whereas building a signal
	 * with zero questions server-side would be a pointless pause.
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
