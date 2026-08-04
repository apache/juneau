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

import java.util.function.*;

import org.apache.juneau.commons.inject.*;

/**
 * Handler for a single MCP resource.
 *
 * <p>
 * Implementations declare a {@link #descriptor() descriptor} (the {@link McpResourceSpec} returned by
 * {@code resources/list}) and a {@link #read(String, BeanStore) read} body invoked when the matching
 * {@code resources/read} method runs.
 *
 * <p>
 * This is a genuine two-method contract, not a single-method one: both {@link #descriptor()} and
 * {@link #read(String, BeanStore)} are abstract, so it is deliberately <b>not</b> annotated
 * {@code @FunctionalInterface} and a bare lambda cannot implement it. Routing calls {@link #descriptor()}
 * on every registered handler for both {@code resources/list} and {@code resources/read}, so a handler
 * with no usable descriptor is not a valid handler; making {@link #descriptor()} abstract catches that at
 * compile time instead of the first request to reach it at runtime. Use {@link #of(McpResourceSpec, BiFunction)}
 * for the common case of wiring both in a single expression.
 */
public interface McpResourceHandler {

	/**
	 * Builds a handler from a fixed descriptor and a {@code read} lambda.
	 *
	 * <p>
	 * The sanctioned one-expression path for registering a resource: unlike a bare lambda (which cannot
	 * satisfy this two-method interface at all), this factory wires both {@link #descriptor()} and
	 * {@link #read(String, BeanStore)} from a single call.
	 *
	 * @param descriptor The static descriptor returned from every {@link #descriptor()} call. Must not be
	 * 	<jk>null</jk> and must carry a non-blank {@link McpResourceSpec#getUri() uri} (routing matches
	 * 	incoming {@code resources/read} requests on uri, not name, so a uri-less resource would be
	 * 	silently unreachable).
	 * @param read The read body, invoked with the incoming uri and the per-request bean store. Must not be <jk>null</jk>.
	 * @return A new handler wiring both. Never <jk>null</jk>.
	 */
	static McpResourceHandler of(McpResourceSpec descriptor, BiFunction<String,BeanStore,McpResourceOutcome> read) {
		assertArgNotNull("descriptor", descriptor);
		assertArgNotNullOrBlank("descriptor.getUri()", descriptor.getUri());
		assertArgNotNull("read", read);
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() {
				return descriptor;
			}

			@Override public McpResourceOutcome read(String uri, BeanStore ctx) {
				return read.apply(uri, ctx);
			}
		};
	}

	/**
	 * Returns the static descriptor for this resource.
	 *
	 * <p>
	 * The {@link McpResourceSpec#getUri() uri} value is used by the bound {@link McpRevision} to route
	 * incoming {@code resources/read} requests, so each handler in an {@link McpServerConfig} must use
	 * a unique URI.
	 *
	 * @return The resource descriptor. Never {@code null}.
	 */
	McpResourceSpec descriptor();

	/**
	 * Reads the resource body.
	 *
	 * @param uri The URI from the {@code resources/read} request (matches {@link #descriptor()}.{@code uri}).
	 * @param ctx Per-request bean store. Never {@code null}.
	 * @return The resource contents. Never {@code null}.
	 */
	McpResourceOutcome read(String uri, BeanStore ctx);
}
