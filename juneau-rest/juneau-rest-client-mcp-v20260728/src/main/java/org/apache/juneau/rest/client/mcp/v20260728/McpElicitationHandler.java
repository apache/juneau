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

import java.io.*;
import java.util.*;

import org.apache.juneau.bean.mcp.v20260728.*;

/**
 * Caller-supplied callback that answers one round of server&rarr;client MCP {@code 2026-07-28} SEP-2322
 * elicitation requests during a client-side Multi-Round-Trip-Request (MRTR) auto-resume loop.
 *
 * <p>
 * Passed to {@link McpClient#callToolWithElicitation}, {@link McpClient#getPromptWithElicitation}, and
 * {@link McpClient#readResourceWithElicitation}: each time the server pauses a call with an {@code input_required}
 * result, the client decodes that round's pending requests (there may be more than one) and invokes
 * {@link #elicit(Map)} to obtain the matching answers, which it then echoes back on the resume call. This turns
 * the otherwise hand-driven {@link McpClient#callRaw}/{@link ElicitationRequests}/{@link ElicitationResponses}
 * loop into a single call plus one handler.
 *
 * <p>
 * A handler answers by returning, keyed by the same server-assigned ids it was given, an {@link ElicitResult}
 * per request it wishes to answer &mdash; an {@link ElicitAction#ACCEPT} with content, or an
 * {@link ElicitAction#DECLINE}/{@link ElicitAction#CANCEL} to refuse. Decline/cancel answers are still sent back
 * to the server (which decides the terminal outcome of a refused elicitation); the client does not short-circuit
 * them locally.
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface McpElicitationHandler {

	/**
	 * Answers one round of elicitation requests.
	 *
	 * <p>
	 * The returned map may legitimately omit ids the handler chooses not to answer (the server may then
	 * re-pause, bounded by the auto-resume loop's max-rounds guard); returning <jk>null</jk> outright is illegal
	 * and causes the auto-resume loop to throw {@link IllegalArgumentException}; and any extra/unknown ids
	 * present in the returned map are echoed back to the server unvalidated.
	 *
	 * @param requests The pending requests for this round, keyed by server-assigned id (decoded via
	 * 	{@link ElicitationRequests#requests(Map)}). Never <jk>null</jk>; may contain more than one entry, and an
	 * 	individual value may be <jk>null</jk> if the corresponding raw entry was absent/<jk>null</jk>.
	 * @return The answers keyed by the same server-assigned ids. Must not be <jk>null</jk>. An
	 * 	{@link ElicitResult} value must not be <jk>null</jk>. Use a {@link LinkedHashMap} for deterministic
	 * 	ordering of the echoed {@code inputResponses}. May legitimately omit ids the handler chooses not to
	 * 	answer (the server may then re-pause, bounded by the loop's max-rounds guard).
	 * @throws IOException If the handler performs its own I/O (e.g. prompting a remote user) and it fails; the
	 * 	auto-resume loop propagates it to the caller unchanged.
	 */
	Map<String,ElicitResult> elicit(Map<String,ElicitRequest> requests) throws IOException;
}
