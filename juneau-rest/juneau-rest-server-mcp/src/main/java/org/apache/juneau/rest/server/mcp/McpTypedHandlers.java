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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.marshaller.Json;

/**
 * Bridges {@link McpTypedToolHandler typed tool handlers} into raw {@link McpToolHandler}s.
 *
 * <p>
 * The single public entry point is {@link #adaptTool(McpTypedToolHandler)}, which:
 * <ul>
 * 	<li>Derives the input and output JSON Schemas once (at adaptation time) from the typed handler's declared
 * 		{@link McpTypedToolHandler#argumentType() argument} and {@link McpTypedToolHandler#resultType() result}
 * 		types, caching them in a single {@link McpToolSpec descriptor} instance returned on every
 * 		{@link McpToolHandler#descriptor()} call.
 * 	<li>Binds incoming JSON arguments into an instance of the argument type, mapping binding failures to
 * 		JSON-RPC {@code -32602} (invalid params).
 * 	<li>Invokes the typed handler and canonicalizes the returned value into a generic JSON tree in a single
 * 		serialization pass, mapping canonicalization and structural-safety failures to JSON-RPC {@code -32603}
 * 		(internal error).
 * 	<li>Optionally mirrors the canonical structured content as a single text content block.
 * </ul>
 */
public final class McpTypedHandlers {

	private static final JsonSerializer CANONICAL =
		JsonSerializer.create().addBeanTypes(false).uriResolution(UriResolution.NONE).build();

	private static final JsonSchemaGenerator SCHEMA_GENERATOR =
		JsonSchemaGenerator.create().useBeanDefs().build();

	private McpTypedHandlers() {}

	/**
	 * Adapts a typed tool handler into a raw {@link McpToolHandler}.
	 *
	 * @param <A> The tool argument type.
	 * @param <R> The tool result type.
	 * @param typed The typed handler to adapt. Must not be <jk>null</jk>.
	 * @return A raw handler that derives schemas, binds arguments, and canonicalizes results. Never <jk>null</jk>.
	 */
	public static <A,R> McpToolHandler adaptTool(McpTypedToolHandler<A,R> typed) {
		assertArgNotNull("typed", typed);
		var declared = assertArgNotNull("typed.descriptor()", typed.descriptor());
		var descriptor = new McpToolSpec()
			.setName(declared.getName())
			.setDescription(declared.getDescription())
			.setInputSchema(schema(typed.argumentType()))
			.setOutputSchema(schema(typed.resultType()));
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() {
				return descriptor;
			}

			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var bound = McpTypedHandlers.<A>bind(arguments, typed.argumentType());
				var result = typed.call(bound, ctx);
				var tree = canonicalize(result);
				try {
					McpJsonValueSafety.check(tree, "Tool structuredContent");
				} catch (IllegalArgumentException e) {
					throw new McpException(-32603, e.getMessage());
				}
				var outcome = new McpToolOutcome().setStructuredContent(tree);
				if (typed.mirrorStructuredContentAsText())
					outcome.setContent(List.of(McpContentBlock.text(render(tree))));
				return outcome;
			}
		};
	}

	/**
	 * Derives a revision-neutral schema carrier from a Java type.
	 *
	 * <p>
	 * Uses a generator with bean definitions enabled, so nested bean types are emitted under {@code $defs} and
	 * referenced rather than inlined.
	 *
	 * @param type The type to derive a schema for.
	 * @return The derived schema, or <jk>null</jk> if the generator produced no schema.
	 */
	private static McpSchema schema(Type type) {
		try {
			var session = SCHEMA_GENERATOR.getSession();
			var raw = session.getSchema(type);
			if (raw != null && session.getBeanDefs() != null && ! session.getBeanDefs().isEmpty())
				raw.append("$defs", session.getBeanDefs());
			return raw == null ? null : McpSchema.of(raw);
		} catch (Exception e) {
			throw new McpException(-32603, "Failed to derive tool schema for " + type.getTypeName() + ": " + e.getMessage());
		}
	}

	@SuppressWarnings({
		"unchecked" // Parser returns the value described by the authoritative runtime Type.
	})
	private static <A> A bind(Map<String,Object> arguments, Type type) {
		try {
			return (A)JsonParser.DEFAULT.read(Json.of(arguments), type);
		} catch (Exception e) {
			throw new McpException(-32602, "Failed to bind tool arguments to " + type.getTypeName() + ": " + e.getMessage());
		}
	}

	private static Object canonicalize(Object result) {
		try {
			var json = CANONICAL.write(result);
			return JsonParser.DEFAULT.read(json, Object.class);
		} catch (Exception e) {
			throw new McpException(-32603, "Failed to canonicalize tool result: " + e.getMessage());
		}
	}

	private static String render(Object tree) {
		try {
			return CANONICAL.write(tree);
		} catch (Exception e) {
			throw new McpException(-32603, "Failed to mirror tool structuredContent: " + e.getMessage());
		}
	}
}
