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
package org.apache.juneau.rest.mcp;

import java.util.*;

import org.apache.juneau.bean.mcp.*;
import org.apache.juneau.marshaller.*;

/**
 * Adapters that convert {@link McpTypedToolHandler} / {@link McpTypedPromptHandler} into the raw
 * {@link McpToolHandler} / {@link McpPromptHandler} interfaces consumed by {@link McpDispatcher}.
 */
public final class McpTypedHandlers {

	private McpTypedHandlers() {}

	/**
	 * Converts a typed tool handler into a raw handler suitable for {@link McpServerConfig#addTool(McpToolHandler...)}.
	 *
	 * <p>
	 * Conversion behavior:
	 * <ul>
	 * 	<li>The incoming {@code Map<String,Object>} arguments are serialized to JSON and re-parsed as
	 * 	    {@link McpTypedToolHandler#argumentType()} via {@link Json#of(Object)} / {@link Json#to(Object, Class, java.lang.reflect.Type...)}.
	 * 	<li>If the typed handler returns a {@link CallToolResult}, it is passed through unchanged.
	 * 	<li>Otherwise, the return value is JSON-serialized and wrapped in a single {@link TextContent}
	 * 	    inside a {@link CallToolResult}.
	 * </ul>
	 *
	 * @param typed The typed handler. Never {@code null}.
	 * @param <A> Argument type.
	 * @param <R> Return type.
	 * @return A raw handler delegating to {@code typed}.
	 */
	public static <A, R> McpToolHandler adaptTool(McpTypedToolHandler<A, R> typed) {
		return new McpToolHandler() {
			@Override
			public Tool descriptor() {
				return typed.descriptor();
			}

			@Override
			public CallToolResult call(Map<String, Object> arguments, org.apache.juneau.cp.BasicBeanStore ctx) {
				A bound = bindArguments(arguments, typed.argumentType());
				R result = typed.call(bound, ctx);
				return wrapToolResult(result);
			}
		};
	}

	/**
	 * Converts a typed prompt handler into a raw handler suitable for {@link McpServerConfig#addPrompt(McpPromptHandler...)}.
	 *
	 * <p>
	 * The incoming arguments map is JSON-roundtripped into {@link McpTypedPromptHandler#argumentType()} before
	 * delegating to the typed implementation.
	 *
	 * @param typed The typed handler. Never {@code null}.
	 * @param <A> Argument type.
	 * @return A raw handler delegating to {@code typed}.
	 */
	public static <A> McpPromptHandler adaptPrompt(McpTypedPromptHandler<A> typed) {
		return new McpPromptHandler() {
			@Override
			public Prompt descriptor() {
				return typed.descriptor();
			}

			@Override
			public GetPromptResult get(Map<String, Object> arguments, org.apache.juneau.cp.BasicBeanStore ctx) {
				A bound = bindArguments(arguments, typed.argumentType());
				return typed.get(bound, ctx);
			}
		};
	}

	private static <T> T bindArguments(Map<String, Object> arguments, Class<T> type) {
		if (arguments == null || arguments.isEmpty())
			return null;
		try {
			var json = Json.of(arguments);
			return Json.to(json, type);
		} catch (Exception e) {
			throw new McpException(McpDispatcher.CODE_INVALID_PARAMS, "Failed to bind arguments to " + type.getName() + ": " + e.getMessage());
		}
	}

	private static CallToolResult wrapToolResult(Object result) {
		if (result instanceof CallToolResult ctr)
			return ctr;
		String text;
		if (result == null)
			text = "";
		else if (result instanceof String s)
			text = s;
		else {
			try {
				text = Json.of(result);
			} catch (Exception e) {
				throw new McpException(McpDispatcher.CODE_INTERNAL_ERROR, "Failed to serialize tool result: " + e.getMessage());
			}
		}
		return new CallToolResult().setContent(List.of(new TextContent().setText(text)));
	}
}
