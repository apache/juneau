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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * Static helper that builds an {@link McpInputRequiredSignal} from one or more typed {@link ElicitRequest}s (MCP
 * {@code 2026-07-28} SEP-2322 elicitation, riding the Multi-Round-Trip Requests loop).
 *
 * <p>
 * {@link McpInputRequiredSignal}'s constructor takes {@code Map<String,Object>} whose values must be {@code Map}
 * at runtime (see its own Javadoc); this helper serializes each {@link ElicitRequest} to that shape via Juneau's
 * JSON marshaller (round-tripped through {@link JsonMap}, the same technique
 * {@link McpMrtrResumeContext#continuationAs(Class)} already demonstrates for the continuation side) so a
 * handler never hand-builds the raw map itself. Pure marshalling: no dispatcher-visible state, no new capability
 * gate — the existing elicitation-specific gate in {@code McpRevision#pause} already covers every signal this
 * helper produces.
 */
public final class ElicitationRequests {

	private ElicitationRequests() {}

	/**
	 * Builds a single-question {@link McpInputRequiredSignal}.
	 *
	 * @param id The server-assigned id for this question.  Must not be <jk>null</jk>.
	 * @param request The typed elicitation request.  Must not be <jk>null</jk>.
	 * @param continuation Handler-opaque continuation value.  Can be <jk>null</jk> if the handler needs no
	 * 	state carried between rounds.
	 * @return A new signal ready to be thrown.
	 * @throws IllegalArgumentException If {@code id} or {@code request} is <jk>null</jk>.
	 */
	public static McpInputRequiredSignal of(String id, ElicitRequest request, Object continuation) {
		assertArgNotNull("id", id);
		assertArgNotNull("request", request);
		return of(Map.of(id, request), continuation);
	}

	/**
	 * Builds a multi-question {@link McpInputRequiredSignal} (MRTR's multi-request-per-round case: several
	 * elicitations paused on together, resumed together).
	 *
	 * <p>
	 * Unlike {@code ElicitationResponses.toInputResponses(Map)} (client side), an empty {@code requests} map is
	 * rejected, not accepted &mdash; deliberately asymmetric, since a signal built with zero questions would be
	 * a pointless pause, whereas a caller resuming may legitimately have zero elicitation answers on hand if
	 * the round it is resuming only carried non-elicitation MRTR pauses.
	 *
	 * @param requests Server-assigned-id-keyed typed elicitation requests.  Must not be <jk>null</jk> or empty,
	 * 	and no value may be <jk>null</jk>.  Output iteration order mirrors this map's order, so a caller
	 * 	wanting deterministic ordering should pass a {@link LinkedHashMap}.
	 * @param continuation Handler-opaque continuation value.  Can be <jk>null</jk>.
	 * @return A new signal ready to be thrown.
	 * @throws IllegalArgumentException If {@code requests} is <jk>null</jk> or empty, or any value in it is
	 * 	<jk>null</jk>.
	 */
	public static McpInputRequiredSignal of(Map<String,ElicitRequest> requests, Object continuation) {
		assertArgNotNull("requests", requests);
		if (requests.isEmpty())
			throw iaex("requests must not be empty");
		Map<String,Object> raw = new LinkedHashMap<>();
		requests.forEach((id, request) -> {
			assertArgNotNull("requests[" + id + "]", request);
			raw.put(id, Json.to(Json.of(request), JsonMap.class));
		});
		return new McpInputRequiredSignal(raw, continuation);
	}
}
