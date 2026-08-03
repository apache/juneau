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

/**
 * The dispatcher-owned payload sealed into an opaque {@code requestState} token by {@link RequestStateCodec}.
 *
 * <p>
 * Never wire-visible directly &mdash;
 * {@link org.apache.juneau.bean.mcp.v20260728.InputRequiredResult#getRequestState()} carries only the sealed
 * string form. This type exists so the round counter (SEP-2322's max-rounds cap) and expiry travel *inside*
 * the sealed token itself, per {@code McpRevision}'s documented statelessness: no server-side session exists
 * to hold them instead.
 *
 * <p>
 * <b>The sealed continuation is the only trusted per-round carrier.</b> This is what makes it security-relevant:
 * the {@code continuation} sealed here is authenticated (AEAD) and cannot be altered by the client between
 * rounds, whereas the request {@code arguments} a handler sees on RESUME come from the follow-up request and are
 * fully client-controlled (see {@link McpMrtrResumeContext}). Anything a handler must be able to trust across
 * rounds &mdash; an authenticated principal, an authorization decision, a price, a resource id &mdash; must live
 * <i>inside</i> this continuation, never be re-derived from the per-round arguments.
 *
 * <p>
 * <b>Continuation type fidelity.</b> {@code continuation} is declared as {@code Object} because it is
 * handler-opaque, but {@link AeadRequestStateCodec#unseal} reconstructs it via generic JSON deserialization
 * ({@code Json.to(json, McpRequestState.class)}). A handler that pauses with a {@code Map} or ordinary-bean
 * continuation therefore gets back a <i>generic JSON</i> value on resume &mdash; a {@code JsonMap}/
 * {@code JsonList}/boxed primitive/{@code String} &mdash; never the original Java type. A handler that needs its
 * original type back should not reconstruct it by hand: it retrieves the resume context from the
 * {@code BeanStore} and calls {@link McpMrtrResumeContext#continuationAs(Class)}, which converts the generic JSON
 * value back to any Juneau-marshallable target type in one call.
 *
 * @param continuation The handler-opaque continuation value, exactly as thrown in
 * 	{@code McpInputRequiredSignal#getContinuation()} (or, on a subsequent pause, exactly as the handler
 * 	returned it a second time). Must be Juneau-marshallable (ordinary bean, {@code Map}, {@code String}, or
 * 	similar). See the type-fidelity note above: it round-trips as generic JSON, not its original Java type.
 * @param method The JSON-RPC method this state was sealed for ({@code tools/call}, {@code prompts/get}, or
 * 	{@code resources/read}). Part of the AAD contract (see {@link RequestStateCodec}); also checked directly
 * 	by the dispatcher against the follow-up request's own method as an extra sanity check.
 * @param round The 1-based round counter: 1 on the first PAUSE, incremented on every subsequent PAUSE.
 * @param expiresAtMs Absolute expiry timestamp in epoch milliseconds.
 */
public record McpRequestState(Object continuation, String method, int round, long expiresAtMs) {
}
