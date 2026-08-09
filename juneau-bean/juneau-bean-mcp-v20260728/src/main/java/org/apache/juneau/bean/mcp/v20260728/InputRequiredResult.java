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
package org.apache.juneau.bean.mcp.v20260728;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;

/**
 * Result of a {@code tools/call}, {@code prompts/get}, or {@code resources/read} handler pausing to request more
 * input (MCP {@code 2026-07-28} SEP-2322 Multi-Round-Trip Requests).
 *
 * <p>
 * Carries a server-assigned-id-keyed map of requested inputs ({@link #getInputRequests()}) and/or an opaque,
 * tamper-evident continuation token ({@link #getRequestState()}). The pinned schema requires at least one of the
 * two to be present; a caller must treat {@code requestState} as a black box, meaningful only to the codec that
 * sealed it.
 *
 * <p>
 * <b>{@code inputRequests} is lossless.</b> Each value is a full sub-request object exactly as the pinned schema
 * models it (a sampling/roots/elicitation sub-request), typed as a raw {@link JsonMap} so a handler-supplied map
 * reaches the wire byte-for-byte &mdash; there is no synthetic {@code {type, payload}} envelope, and no member is
 * relocated or dropped. Concrete sub-request shapes are owned by the eventual consumers of this mechanism (C5
 * sampling, C6 elicitation), neither of which is defined by this class. This mirrors the equally free-form shape
 * of the request-side {@code inputResponses} map.
 */
@Marshalled
public class InputRequiredResult extends Result<InputRequiredResult> {

	private Map<String,JsonMap> inputRequests;
	private String requestState;

	/**
	 * Constructor. Sets {@code resultType} to {@code "input_required"}.
	 */
	public InputRequiredResult() {
		setResultType("input_required");
	}

	/**
	 * Server-assigned-id-keyed map of requested inputs.
	 *
	 * <p>
	 * Each value is a raw sub-request object (see the class Javadoc); it is carried losslessly, never wrapped in
	 * a synthetic envelope.
	 *
	 * @return The map, or {@code null} if not set.
	 */
	public Map<String,JsonMap> getInputRequests() {
		return u(inputRequests);
	}

	/**
	 * Sets the requested-inputs map.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public InputRequiredResult setInputRequests(Map<String,JsonMap> value) {
		inputRequests = value;
		return this;
	}

	/**
	 * Convenience method to add a single requested-input entry.
	 *
	 * @param id The server-assigned id.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The raw sub-request object, carried to the wire byte-for-byte.  Can be <jk>null</jk> (stored
	 * 	as <jk>null</jk>).
	 * @return This object (for method chaining).
	 */
	public InputRequiredResult putInputRequest(String id, JsonMap value) {
		if (inputRequests == null)
			inputRequests = map();
		inputRequests.put(id, value);
		return this;
	}

	/**
	 * The opaque, tamper-evident continuation token.
	 *
	 * @return The token, or {@code null} if not set.
	 */
	public String getRequestState() {
		return requestState;
	}

	/**
	 * Sets the continuation token.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public InputRequiredResult setRequestState(String value) {
		requestState = value;
		return this;
	}

	/**
	 * Validates the schema invariant that at least one of {@link #getInputRequests()} or
	 * {@link #getRequestState()} is present.
	 *
	 * @throws IllegalStateException If both are absent/empty.
	 */
	public void validate() {
		if ((inputRequests == null || inputRequests.isEmpty()) && ie(requestState))
			throw isex("InputRequiredResult requires at least one of inputRequests or requestState");
	}
}
