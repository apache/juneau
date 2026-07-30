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
package org.apache.juneau.rest.server.mcp.v20250618;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.*;

/**
 * Adapter that converts a {@link McpTypedPromptHandler} into the raw {@link McpPromptHandler} interface
 * consumed by {@link McpRevision}.
 *
 * <p>
 * Typed tool handling is revision-neutral and lives in the core module; this dated helper adapts prompts only.
 */
public final class McpTypedHandlers {

	private McpTypedHandlers() {}

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
		assertArgNotNull("typed", typed);
		return new McpPromptHandler() {
			@Override
			public McpPromptSpec descriptor() {
				return McpWire.toNeutral(typed.descriptor());
			}

			@Override
			public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) {
				A bound = bindPromptArguments(arguments, typed.argumentType());
				return McpWire.toNeutral(typed.get(bound, ctx));
			}
		};
	}

	private static <T> T bindPromptArguments(Map<String,Object> arguments, Class<T> type) {
		if (arguments == null || arguments.isEmpty())
			return null;
		try {
			var json = Json.of(arguments);
			return Json.to(json, type);
		} catch (Exception e) {
			throw new McpException(McpRevision.CODE_INVALID_PARAMS, "Failed to bind arguments to " + type.getName() + ": " + e.getMessage());
		}
	}
}
