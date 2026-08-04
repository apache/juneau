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
package org.apache.juneau.rest.server.mcp;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.inject.*;

/**
 * Handler for a single MCP prompt.
 *
 * <p>
 * Implementations declare a {@link #descriptor() descriptor} (the {@link McpPromptSpec} returned by
 * {@code prompts/list}) and a {@link #get(Map, BeanStore) get} body invoked when the matching
 * {@code prompts/get} method runs.
 *
 * <p>
 * This is a genuine two-method contract, not a single-method one: both {@link #descriptor()} and
 * {@link #get(Map, BeanStore)} are abstract, so it is deliberately <b>not</b> annotated
 * {@code @FunctionalInterface} and a bare lambda cannot implement it. Routing calls {@link #descriptor()}
 * on every registered handler for both {@code prompts/list} and {@code prompts/get}, so a handler with no
 * usable descriptor is not a valid handler; making {@link #descriptor()} abstract catches that at compile
 * time instead of the first request to reach it at runtime. Use {@link #of(McpPromptSpec, BiFunction)} for
 * the common case of wiring both in a single expression.
 */
public interface McpPromptHandler {

	/**
	 * Builds a handler from a fixed descriptor and a {@code get} lambda.
	 *
	 * <p>
	 * The sanctioned one-expression path for registering a prompt: unlike a bare lambda (which cannot
	 * satisfy this two-method interface at all), this factory wires both {@link #descriptor()} and
	 * {@link #get(Map, BeanStore)} from a single call.
	 *
	 * @param descriptor The static descriptor returned from every {@link #descriptor()} call. Must not be
	 * 	<jk>null</jk> and must carry a non-blank {@link McpPromptSpec#getName() name} (routing matches
	 * 	incoming {@code prompts/get} requests on name, so a nameless prompt would be silently unreachable).
	 * @param get The get body, invoked with the incoming arguments and the per-request bean store. Must not be <jk>null</jk>.
	 * @return A new handler wiring both. Never <jk>null</jk>.
	 */
	static McpPromptHandler of(McpPromptSpec descriptor, BiFunction<Map<String,Object>,BeanStore,McpPromptOutcome> get) {
		assertArgNotNull("descriptor", descriptor);
		assertArgNotNullOrBlank("descriptor.getName()", descriptor.getName());
		assertArgNotNull("get", get);
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() {
				return descriptor;
			}

			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) {
				return get.apply(arguments, ctx);
			}
		};
	}

	/**
	 * Returns the static descriptor for this prompt.
	 *
	 * <p>
	 * The {@link McpPromptSpec#getName() name} value is used by the bound {@link McpRevision} to route
	 * incoming {@code prompts/get} requests, so each handler in an {@link McpServerConfig} must use a
	 * unique name.
	 *
	 * @return The prompt descriptor. Never {@code null}.
	 */
	McpPromptSpec descriptor();

	/**
	 * Renders the prompt.
	 *
	 * @param arguments The arguments object passed in the JSON-RPC params (never {@code null}; empty map when omitted).
	 * @param ctx Per-request bean store. Never {@code null}.
	 * @return The rendered prompt. Never {@code null}.
	 */
	McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx);
}
