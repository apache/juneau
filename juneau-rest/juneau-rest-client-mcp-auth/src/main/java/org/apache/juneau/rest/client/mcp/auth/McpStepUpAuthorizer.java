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
import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Client-side SEP-2350 step-up state machine: wraps an MCP call so that, on a {@code 401}/{@code 403 insufficient_scope}
 * challenge, it computes the required-scope union ({@link McpScopeAccumulator}), drives (re-)authorization via a
 * caller-supplied {@link Reauthorizer}, and retries the original call &mdash; bounded by a hard attempt cap with
 * per-<code>(resource, operation)</code> attempt tracking so a misconfigured server can't drive an unbounded
 * re-authorization loop (spec "no more than a few times").
 *
 * <p>
 * Step-up for user-delegation clients is inherently interactive (a fresh auth-code + PKCE round-trip), so this is an
 * <b>explicit orchestration helper</b> the caller wraps around MCP calls &mdash; not a silent interceptor retry.  The
 * caller's {@link ScopedCall} performs the actual MCP request and throws {@link McpInsufficientScopeException} (carrying
 * the parsed {@link WwwAuthenticateChallenge}) when the server responds with a step-up challenge; the
 * {@link Reauthorizer} performs the (re-)authorization with the widened scope union (e.g. via
 * {@link McpAuthorizationCodeAcquirer} for user clients or a client-credentials re-acquire) and installs the new token.
 *
 * <p>
 * {@code client_credentials} (own-behalf) clients MAY abort immediately rather than attempt step-up
 * ({@link Builder#attemptStepUp(boolean) attemptStepUp(false)}).
 *
 * <h5 class='topic'>Attempt-counter lifetime</h5>
 * <p>
 * Per-<code>(resource, operation)</code> attempt counts exist only to bound a single step-up burst.  A counter is
 * <b>cleared automatically</b> when {@link #execute(String, ScopedCall) execute} returns successfully for that operation,
 * so a long-lived authorizer does not permanently refuse a later legitimate step-up for the same operation.  A counter
 * that reaches the cap without a success (a permanent failure) is <b>not</b> auto-cleared &mdash; the caller can reset it
 * explicitly with {@link #reset(String)} (one operation) or {@link #resetAll()} (all operations) once the underlying
 * cause is resolved.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., ARG_value)
})
public class McpStepUpAuthorizer {

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";
	private static final String ARG_operation = "operation";

	/** A caller-supplied MCP call that throws {@link McpInsufficientScopeException} on a step-up challenge. */
	@FunctionalInterface
	public interface ScopedCall<T> {
		/**
		 * Performs the MCP call.
		 *
		 * @return The call result.
		 * @throws McpInsufficientScopeException If the server responded with a step-up {@code insufficient_scope} challenge.
		 */
		T call() throws McpInsufficientScopeException;
	}

	/** Drives (re-)authorization with the computed scope union and installs the resulting token. */
	@FunctionalInterface
	public interface Reauthorizer {
		/**
		 * Performs (re-)authorization with the widened scope union.
		 *
		 * @param unionScopes The order-preserving union of previously-requested + challenge scopes.
		 */
		void reauthorize(Set<String> unionScopes);
	}

	/**
	 * Static creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder.
	 */
	public static class Builder {
		URI resource;
		URI issuer;
		McpScopeAccumulator accumulator;
		Reauthorizer reauthorizer;
		int maxAttempts = 3;
		boolean attemptStepUp = true;
		final Set<String> previouslyRequestedScopes = st();

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the RFC 8707 resource indicator this authorizer tracks attempts for.  Required.
		 *
		 * @param value The resource URI.
		 * @return This object.
		 */
		public Builder resource(URI value) {
			resource = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the authorization server issuer.  Required.
		 *
		 * @param value The issuer URI.
		 * @return This object.
		 */
		public Builder issuer(URI value) {
			issuer = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the scope accumulator.  Defaults to a fresh {@link McpScopeAccumulator}.
		 *
		 * @param value The accumulator.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder accumulator(McpScopeAccumulator value) {
			accumulator = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the re-authorization callback invoked with the widened scope union.  Required.
		 *
		 * @param value The reauthorizer.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder reauthorizer(Reauthorizer value) {
			reauthorizer = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the hard cap on re-authorization attempts per {@code (resource, operation)} before a permanent failure.
		 * Default 3.
		 *
		 * @param value The maximum attempts.  Must be &ge; 1.
		 * @return This object.
		 */
		public Builder maxAttempts(int value) {
			assertArg(value >= 1, "maxAttempts must be >= 1 (was %s)", value);
			maxAttempts = value;
			return this;
		}

		/**
		 * Whether to attempt step-up at all.  Default <jk>true</jk>; a {@code client_credentials} client MAY set
		 * <jk>false</jk> to abort immediately on the first challenge.
		 *
		 * @param value <jk>false</jk> to abort immediately rather than attempt step-up.
		 * @return This object.
		 */
		public Builder attemptStepUp(boolean value) {
			attemptStepUp = value;
			return this;
		}

		/**
		 * Seeds the scope accumulator with the client's previously-requested scopes so the first union preserves them.
		 *
		 * @param values The previously-requested scopes.
		 * @return This object.
		 */
		public Builder previouslyRequestedScopes(String... values) {
			assertArgNotNull("values", values);
			for (var v : values) {
				assertArgNotNullOrBlank("scope", v);
				previouslyRequestedScopes.add(v);
			}
			return this;
		}

		/**
		 * Builds the authorizer.
		 *
		 * @return A new {@link McpStepUpAuthorizer}.
		 */
		public McpStepUpAuthorizer build() {
			if (resource == null)
				throw isex("McpStepUpAuthorizer requires resource(...)");
			if (issuer == null)
				throw isex("McpStepUpAuthorizer requires issuer(...)");
			if (reauthorizer == null)
				throw isex("McpStepUpAuthorizer requires reauthorizer(...)");
			if (accumulator == null)
				accumulator = new McpScopeAccumulator();
			if (! previouslyRequestedScopes.isEmpty())
				accumulator.seed(resource, issuer, previouslyRequestedScopes);
			return new McpStepUpAuthorizer(this);
		}
	}

	private final URI resource;
	private final URI issuer;
	private final McpScopeAccumulator accumulator;
	private final Reauthorizer reauthorizer;
	private final int maxAttempts;
	private final boolean attemptStepUp;
	private final ConcurrentMap<String,Integer> attemptsByOperation = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected McpStepUpAuthorizer(Builder b) {
		this.resource = b.resource;
		this.issuer = b.issuer;
		this.accumulator = b.accumulator;
		this.reauthorizer = b.reauthorizer;
		this.maxAttempts = b.maxAttempts;
		this.attemptStepUp = b.attemptStepUp;
	}

	/**
	 * Executes an MCP call, transparently handling SEP-2350 step-up: on an {@code insufficient_scope} challenge it
	 * unions the challenge scopes onto the accumulated set, re-authorizes, and retries &mdash; up to the configured
	 * attempt cap, after which it throws a permanent {@link McpAuthException}.
	 *
	 * @param <T> The call result type.
	 * @param operation The operation identifier (e.g. {@code tools/call:deploy}) used for per-operation attempt
	 * 	tracking.  Must not be <jk>null</jk> or blank.
	 * @param call The MCP call.  Must not be <jk>null</jk>.
	 * @return The successful call result.
	 * @throws McpAuthException If step-up is disabled, or the attempt cap is exceeded (permanent authorization failure).
	 */
	public <T> T execute(String operation, ScopedCall<T> call) {
		assertArgNotNullOrBlank(ARG_operation, operation);
		assertArgNotNull("call", call);
		var opKey = opKey(operation);
		while (true) {
			try {
				var result = call.call();
				// Success clears the operation's attempt burst so a later legitimate step-up starts fresh.
				attemptsByOperation.remove(opKey);
				return result;
			} catch (McpInsufficientScopeException e) {
				if (! attemptStepUp)
					throw new McpAuthException("Step-up authorization aborted for operation '" + operation
						+ "' (attemptStepUp=false)", e);
				var used = attemptsByOperation.merge(opKey, 1, Integer::sum);
				if (used > maxAttempts)
					throw new McpAuthException("Step-up authorization exhausted after " + maxAttempts
						+ " attempt(s) for resource '" + resource + "' operation '" + operation
						+ "'; treating as a permanent authorization failure", e);
				var union = accumulator.accumulate(resource, issuer, e.challenge().scopes());
				reauthorizer.reauthorize(union);
			}
		}
	}

	/**
	 * Returns the number of step-up attempts recorded for the given operation.
	 *
	 * @param operation The operation identifier.  Must not be <jk>null</jk> or blank.
	 * @return The recorded attempt count.
	 */
	public int attempts(String operation) {
		assertArgNotNullOrBlank(ARG_operation, operation);
		return attemptsByOperation.getOrDefault(opKey(operation), 0);
	}

	/**
	 * Clears the recorded step-up attempt count for a single operation (e.g. after the caller has resolved the cause of
	 * a prior permanent failure), so a subsequent {@link #execute(String, ScopedCall)} may step up again.
	 *
	 * @param operation The operation identifier.  Must not be <jk>null</jk> or blank.
	 */
	public void reset(String operation) {
		assertArgNotNullOrBlank(ARG_operation, operation);
		attemptsByOperation.remove(opKey(operation));
	}

	/**
	 * Clears all recorded step-up attempt counts for this authorizer's resource across every operation.
	 */
	public void resetAll() {
		attemptsByOperation.clear();
	}

	/** The per-operation attempt-tracking key: {@code resource + '\n' + operation}. */
	private String opKey(String operation) {
		return resource.toString() + '\n' + operation;
	}
}
