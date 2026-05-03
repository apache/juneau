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

import org.apache.juneau.bean.mcp.*;
import org.apache.juneau.cp.*;

/**
 * Typed variant of {@link McpPromptHandler} where MCP {@code prompts/get} arguments bind into a Juneau bean.
 *
 * @param <A> Argument bean type.
 */
public interface McpTypedPromptHandler<A> {

	/**
	 * Returns the static descriptor for this prompt.
	 *
	 * @return The prompt descriptor. Never {@code null}.
	 */
	Prompt descriptor();

	/**
	 * Returns the runtime argument class for binding.
	 *
	 * @return The argument class. Never {@code null}.
	 */
	Class<A> argumentType();

	/**
	 * Renders the prompt.
	 *
	 * @param arguments Bound argument bean (may be {@code null} when no arguments are supplied).
	 * @param ctx Per-request bean store.
	 * @return The rendered prompt.
	 */
	GetPromptResult get(A arguments, BasicBeanStore ctx);
}
