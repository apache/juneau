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

import java.util.*;

import org.apache.juneau.marshall.marshaller.*;

/**
 * Bean stashed into the {@link org.apache.juneau.commons.inject.BeanStore} passed to a handler on a RESUME call,
 * carrying the decoded continuation and the client's collected answers.
 *
 * <p>
 * Absent (not stashed) on a first-round call &mdash; a handler that only ever completes in one round never has to
 * know this type exists. A resumable handler calls {@code ctx.getBean(McpMrtrResumeContext.class)} to detect
 * resume and retrieve both fields.
 *
 * <p>
 * <b>Per-round arguments are client-controlled; the continuation is not.</b> On RESUME the handler receives its
 * ordinary {@code arguments} from the follow-up request &mdash; the client re-sends them each round and full
 * schema validation runs per round, but nothing about the original round's arguments is sealed or verified, so a
 * client can alter them between rounds and the MRTR layer will accept the resume. Only {@link #continuation()}
 * (sealed by {@link RequestStateCodec}, AEAD-authenticated) is tamper-evident across rounds. Therefore any value
 * a handler must trust across rounds must be carried <i>in</i> the continuation, never re-read from the per-round
 * arguments. The client is expected to re-send the original {@code arguments} on every round; a handler that
 * needs the exact original arguments should stash them in the continuation rather than rely on the client
 * echoing them faithfully.
 *
 * <p>
 * <b>Continuation type fidelity (round-trip contract).</b> {@link #continuation()} is whatever
 * {@link RequestStateCodec#unseal} decoded from the client's echoed {@code requestState}: a <i>generic JSON</i>
 * value ({@code JsonMap}/{@code JsonList}/boxed primitive/{@code String}), <b>never</b> the original Java type
 * the handler threw in {@link McpInputRequiredSignal} (see the type-fidelity note on {@link McpRequestState}).
 * Rather than force handler authors to parse that generic value by hand, {@link #continuationAs(Class)} converts
 * it back to any Juneau-marshallable target type in one call, so a handler that paused with a bean recovers a
 * bean of the same shape ergonomically.
 *
 * @param continuation The decoded continuation, exactly as {@link RequestStateCodec#unseal} produced it. Can be
 * 	<jk>null</jk> if the handler paused with a <jk>null</jk> continuation. See the round-trip contract above:
 * 	it is generic JSON, not the original Java type &mdash; use {@link #continuationAs(Class)} to recover a typed
 * 	view.
 * @param inputResponses The client's collected answers, echoed from the follow-up request's
 * 	{@code inputResponses} field. Never <jk>null</jk> (an empty map when the client sent none).
 */
public record McpMrtrResumeContext(Object continuation, Map<String,Object> inputResponses) {

	/**
	 * Returns the continuation converted to the given type.
	 *
	 * <p>
	 * Convenience for the round-trip contract documented on this record: because {@link #continuation()} is a
	 * generic JSON value (not the handler's original Java type), a handler that paused with, say, a
	 * {@code ResumeState} bean would otherwise have to reconstruct it manually from a {@code JsonMap}. This
	 * converts the generic value to {@code type} via Juneau's JSON marshaller &mdash; the same mechanism the
	 * codec used to serialize it &mdash; so {@code continuationAs(ResumeState.class)} returns a fully populated
	 * {@code ResumeState}.
	 *
	 * <p>
	 * <b>Failure contract:</b> conversion is best-effort against a generic JSON value. If the continuation's
	 * decoded shape is incompatible with {@code type}, the underlying marshaller throws (a
	 * {@code RuntimeException}); a handler that lets it propagate surfaces the {@code -32603} internal-error
	 * fail-safe from {@code McpRevision#dispatch}. A handler that pauses and resumes with a single, consistent
	 * continuation type never hits this.
	 *
	 * @param <T> The target type.
	 * @param type The target type to convert the continuation to. Must not be <jk>null</jk>.
	 * @return The continuation converted to {@code type}, or <jk>null</jk> if the continuation is <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code type} is <jk>null</jk>.
	 * @throws RuntimeException If the continuation's decoded shape cannot be converted to {@code type}.
	 */
	public <T> T continuationAs(Class<T> type) {
		assertArgNotNull("type", type);
		if (continuation == null)
			return null;
		return Json.to(Json.of(continuation), type);
	}

	/**
	 * Returns the continuation as a {@link String}.
	 *
	 * <p>
	 * Convenience for the common case documented on {@link #continuationAs(Class)}: a handler that pauses with
	 * a plain {@code String} continuation can call this instead of spelling out {@code continuationAs(String.class)}
	 * at the headline pause/resume call site.
	 *
	 * <p>
	 * <b>Conversion is not lenient.</b> {@code continuationAs(String.class)} routes through the JSON marshaller,
	 * which parses {@code String} the same as any other target type &mdash; it requires a quoted JSON string
	 * literal, so a continuation that decoded to anything else (a bare number, a {@code JsonMap}, a
	 * {@code JsonList}, a boolean) throws below rather than being stringified. Only a continuation whose
	 * decoded shape is already a {@link String} converts successfully.
	 *
	 * @return The continuation as a {@link String}, or <jk>null</jk> if the continuation is <jk>null</jk>.
	 * @throws RuntimeException If the continuation's decoded shape is not already a {@link String}.
	 */
	public String continuationAsString() {
		return continuationAs(String.class);
	}
}
