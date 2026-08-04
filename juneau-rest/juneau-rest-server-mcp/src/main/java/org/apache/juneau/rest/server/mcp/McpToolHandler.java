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
 * Handler for a single MCP tool.
 *
 * <p>
 * Implementations declare a {@link #descriptor() descriptor} (returned from {@code tools/list}) and
 * a {@link #call(Map, BeanStore) call} body invoked when a matching {@code tools/call} arrives. Both
 * use the revision-neutral model, so a handler compiles against exactly one protocol revision's
 * worth of assumptions: none.
 *
 * <p>
 * The {@link BeanStore} argument is the per-request bean store, letting handlers look up additional
 * services (or the underlying {@code RestRequest}) without this interface depending on REST runtime types.
 *
 * <p>
 * This is a genuine two-method contract, not a single-method one: both {@link #descriptor()} and
 * {@link #call(Map, BeanStore)} are abstract, so it is deliberately <b>not</b> annotated
 * {@code @FunctionalInterface} and a bare lambda cannot implement it. Routing calls {@link #descriptor()}
 * on every registered handler for both {@code tools/list} and {@code tools/call}, so a handler with no
 * usable descriptor is not a valid handler; making {@link #descriptor()} abstract catches that at compile
 * time instead of the first request to reach it at runtime. Use {@link #of(McpToolSpec, BiFunction)} for
 * the common case of wiring both in a single expression.
 */
public interface McpToolHandler {

	/**
	 * Builds a handler from a fixed descriptor and a {@code call} lambda.
	 *
	 * <p>
	 * The sanctioned one-expression path for registering a tool: unlike a bare lambda (which cannot
	 * satisfy this two-method interface at all), this factory wires both {@link #descriptor()} and
	 * {@link #call(Map, BeanStore)} from a single call.
	 *
	 * @param descriptor The static descriptor returned from every {@link #descriptor()} call. Must not be
	 * 	<jk>null</jk> and must carry a non-blank {@link McpToolSpec#getName() name} (routing matches
	 * 	incoming {@code tools/call} requests on name, so a nameless tool would be silently unreachable).
	 * @param call The call body, invoked with the incoming arguments and the per-request bean store. Must not be <jk>null</jk>.
	 * @return A new handler wiring both. Never <jk>null</jk>.
	 */
	static McpToolHandler of(McpToolSpec descriptor, BiFunction<Map<String,Object>,BeanStore,McpToolOutcome> call) {
		assertArgNotNull("descriptor", descriptor);
		assertArgNotNullOrBlank("descriptor.getName()", descriptor.getName());
		assertArgNotNull("call", call);
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() {
				return descriptor;
			}

			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				return call.apply(arguments, ctx);
			}
		};
	}

	/**
	 * Returns the static descriptor for this tool.
	 *
	 * <p>
	 * The {@link McpToolSpec#getName() name} value is used by the bound {@link McpRevision} to route
	 * incoming {@code tools/call} requests, so each handler in an {@link McpServerConfig} must use a
	 * unique name.
	 *
	 * @return The tool descriptor. Never {@code null}.
	 */
	McpToolSpec descriptor();

	/**
	 * Invokes the tool.
	 *
	 * @param arguments The arguments object passed in the JSON-RPC params (never {@code null}; empty map when omitted).
	 * @param ctx Per-request bean store. Never {@code null}.
	 * @return The call result. Never {@code null}.
	 */
	McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx);
}
