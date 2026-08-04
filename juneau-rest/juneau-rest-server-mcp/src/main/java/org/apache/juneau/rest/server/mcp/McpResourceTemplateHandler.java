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

import org.apache.juneau.commons.inject.*;

/**
 * Handler for a single MCP resource template.
 *
 * <p>
 * Implementations declare a {@link #descriptor() descriptor} (the {@link McpResourceTemplateSpec} returned by
 * {@code resources/templates/list}), a {@link #read(String, Map, BeanStore) read} body invoked when this
 * template is selected for a matching {@code resources/read} request, and an optional
 * {@link #completer(String) completer} for one of the template's declared variables.
 *
 * <p>
 * A registration whose {@link #descriptor() descriptor}.{@code uriTemplate} is not one of
 * {@link McpUriTemplateMatcher}'s reverse-matchable forms remains listable and completable but is never
 * selected for a template-backed read. Use {@link McpServerConfig#addResourceTemplate(McpResourceTemplateSpec...)}
 * to register a listing-only template without implementing this interface.
 *
 * <p>
 * Use {@link #of(McpResourceTemplateSpec, ReadFunction)} for the common case of wiring both
 * {@link #descriptor()} and {@link #read(String, Map, BeanStore)} in a single expression.
 */
public interface McpResourceTemplateHandler {

	/**
	 * A three-argument function from ({@code uri}, {@code variables}, {@code ctx}) to a
	 * {@link McpResourceOutcome}, matching {@link #read(String, Map, BeanStore)}'s signature.
	 *
	 * <p>
	 * A dedicated functional interface rather than {@link java.util.function.BiFunction} because
	 * {@code read(...)} takes three parameters, not two.
	 */
	@FunctionalInterface
	interface ReadFunction {
		/**
		 * Reads the resource body for a concrete URI matched against the template.
		 *
		 * @param uri The original concrete URI from the {@code resources/read} request.
		 * @param variables An immutable, insertion-ordered map of the template's declared variables to their
		 * 	decoded values, captured from {@code uri}.
		 * @param ctx Per-request bean store. Never {@code null}.
		 * @return The resource contents, or {@code null} if this template does not serve {@code uri}.
		 */
		McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx);
	}

	/**
	 * Builds a handler from a fixed descriptor and a {@code read} lambda.
	 *
	 * <p>
	 * The sanctioned one-expression path for registering a resource template: wires both
	 * {@link #descriptor()} and {@link #read(String, Map, BeanStore)} from a single call.
	 *
	 * @param descriptor The static descriptor returned from every {@link #descriptor()} call. Must not be
	 * 	<jk>null</jk> and must carry a non-blank {@link McpResourceTemplateSpec#getUriTemplate() uriTemplate}
	 * 	(routing and {@code completion/complete} both match on it, so a templateless registration would be
	 * 	silently unreachable). Must not be <jk>null</jk>.
	 * @param read The read body, invoked with the concrete URI, its captured template variables, and the
	 * 	per-request bean store. Must not be <jk>null</jk>.
	 * @return A new handler wiring both. Never <jk>null</jk>.
	 */
	static McpResourceTemplateHandler of(McpResourceTemplateSpec descriptor, ReadFunction read) {
		assertArgNotNull("descriptor", descriptor);
		assertArgNotNullOrBlank("descriptor.getUriTemplate()", descriptor.getUriTemplate());
		assertArgNotNull("read", read);
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() {
				return descriptor;
			}

			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) {
				return read.read(uri, variables, ctx);
			}
		};
	}

	/**
	 * Returns the static descriptor for this resource template.
	 *
	 * <p>
	 * The {@link McpResourceTemplateSpec#getUriTemplate() uriTemplate} value is used by the bound
	 * {@link McpRevision} to route incoming {@code resources/read} requests and by {@code completion/complete}
	 * to identify this template, so each handler in an {@link McpServerConfig} must use a unique template
	 * string.
	 *
	 * @return The resource-template descriptor. Never {@code null}.
	 */
	McpResourceTemplateSpec descriptor();

	/**
	 * Reads the resource body for a concrete URI matched against this template.
	 *
	 * @param uri The original concrete URI from the {@code resources/read} request.
	 * @param variables An immutable, insertion-ordered map of this template's declared variables to their
	 * 	decoded values, captured from {@code uri}.
	 * @param ctx Per-request bean store. Never {@code null}.
	 * @return The resource contents, or {@code null} if this template does not serve {@code uri}, which is
	 * 	reported as resource-not-found. A {@code null} result never falls through to a less-specific template.
	 */
	McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx);

	/**
	 * Returns the completer for one of this template's declared variables.
	 *
	 * @param variableName The variable name from a {@code completion/complete} request.
	 * @return The completer for that variable, or {@code null} if none is registered. Callers must treat an
	 * 	undeclared variable name as having no completer regardless of what this method returns.
	 */
	default McpCompleter completer(String variableName) {
		return null;
	}
}
