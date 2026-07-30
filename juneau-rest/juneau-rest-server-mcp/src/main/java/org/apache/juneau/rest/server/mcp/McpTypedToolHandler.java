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

import java.lang.reflect.*;

import org.apache.juneau.commons.inject.*;

/**
 * Strongly-typed tool handler whose argument and result Java types drive schema derivation and binding.
 *
 * <p>
 * Where {@link McpToolHandler} is the raw, hand-crafted contract (author owns the descriptor schemas and the
 * generic {@link McpToolOutcome}), this interface lets an author express a tool in terms of ordinary Java types.
 * {@link McpTypedHandlers#adaptTool(McpTypedToolHandler)} bridges an instance of this interface into a raw
 * {@link McpToolHandler}: it derives the input and output JSON Schemas from the declared {@link #argumentType()}
 * and {@link #resultType()}, binds incoming JSON arguments into an instance of the argument type, invokes
 * {@link #call(Object, BeanStore)}, and canonicalizes the returned value into the outcome's
 * {@code structuredContent}.
 *
 * @param <A> The tool argument type.
 * @param <R> The tool result type.
 */
public interface McpTypedToolHandler<A,R> {

	/**
	 * Returns the base descriptor for this tool.
	 *
	 * <p>
	 * The {@link McpToolSpec#getName() name} and {@link McpToolSpec#getDescription() description} are carried over
	 * verbatim by the adapter. Any {@link McpToolSpec#getInputSchema() input} or {@link McpToolSpec#getOutputSchema()
	 * output} schemas set here are <em>replaced</em> by the schemas derived from {@link #argumentType()} and
	 * {@link #resultType()}; declare authoritative schema shape through those Java types, not here.
	 *
	 * @return The base descriptor. Never <jk>null</jk>.
	 */
	McpToolSpec descriptor();

	/**
	 * The authoritative Java {@link Type} of the tool's arguments.
	 *
	 * <p>
	 * This explicit accessor is the source of truth for both input-schema derivation and argument binding, so
	 * parameterized types (e.g. {@code List<Foo>}) are honored exactly as returned here rather than being erased.
	 *
	 * @return The argument type. Never <jk>null</jk>.
	 */
	Type argumentType();

	/**
	 * The authoritative Java {@link Type} of the tool's result.
	 *
	 * <p>
	 * This explicit accessor is the source of truth for output-schema derivation, so parameterized result types
	 * (e.g. {@code List<Foo>}) are honored exactly as returned here rather than being inferred from the runtime
	 * class of the returned value.
	 *
	 * @return The result type. Never <jk>null</jk>.
	 */
	Type resultType();

	/**
	 * Invokes the tool with bound arguments.
	 *
	 * @param arguments The arguments, bound from the incoming JSON into an instance of {@link #argumentType()}.
	 * @param ctx Per-request bean store. Never <jk>null</jk>.
	 * @return The typed result, canonicalized by the adapter into structured content.
	 */
	R call(A arguments, BeanStore ctx);

	/**
	 * Whether the adapter mirrors the canonical {@code structuredContent} as a single text content block.
	 *
	 * <p>
	 * Defaults to <jk>true</jk> so clients that only understand text content still see the structured result.
	 * Override to return <jk>false</jk> to suppress the text mirror and emit structured content only.
	 *
	 * @return <jk>true</jk> to mirror structured content as text (the default).
	 */
	default boolean mirrorStructuredContentAsText() {
		return true;
	}
}
