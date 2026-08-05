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
package org.apache.juneau.rest.client.mcp.auth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

/**
 * Signals that an MCP call was rejected with a SEP-2350 {@code 401}/{@code 403 insufficient_scope} step-up challenge.
 *
 * <p>
 * A caller-supplied {@link McpStepUpAuthorizer.ScopedCall} throws this (carrying the parsed
 * {@link WwwAuthenticateChallenge}) so {@link McpStepUpAuthorizer} can compute the scope union, re-authorize, and retry.
 * It is deliberately <b>not</b> an {@link McpAuthException}: the transient step-up signal must be distinguishable from
 * the permanent authorization failure the authorizer ultimately throws when retries are exhausted.
 *
 * @since 10.0.0
 */
public class McpInsufficientScopeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	// Non-transient: WwwAuthenticateChallenge is Serializable (a scheme string + immutable string map), so the
	// challenge survives serialization and the challenge() "Never null" guarantee holds after a round-trip.
	private final WwwAuthenticateChallenge challenge;

	/**
	 * Constructor.
	 *
	 * @param challenge The parsed {@code WWW-Authenticate} challenge from the server.  Must not be <jk>null</jk>.
	 */
	public McpInsufficientScopeException(WwwAuthenticateChallenge challenge) {
		super("MCP call rejected with an insufficient_scope step-up challenge: "
			+ assertArgNotNull("challenge", challenge).scopes());
		this.challenge = challenge;
	}

	/**
	 * Returns the parsed challenge carrying the required scopes for the current operation.
	 *
	 * @return The challenge.  Never <jk>null</jk>.
	 */
	public WwwAuthenticateChallenge challenge() {
		return challenge;
	}
}
