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
 * Typed variant of {@link McpToolHandler} where MCP {@code tools/call} arguments bind into a Juneau bean.
 *
 * <p>
 * Implementations declare a typed argument class {@code A} and a typed return type {@code R}. The dispatcher
 * (via {@link McpTypedHandlers#adaptTool(McpTypedToolHandler)}) handles map-to-bean conversion using the
 * default {@link org.apache.juneau.json.JsonParser} and wraps non-{@link CallToolResult} returns in a
 * single-{@link TextContent} {@link CallToolResult}.
 *
 * @param <A> Argument bean type.
 * @param <R> Return type. If {@link CallToolResult}, the return value passes through unchanged.
 */
public interface McpTypedToolHandler<A, R> {

	/**
	 * Returns the static descriptor for this tool.
	 *
	 * @return The tool descriptor. Never {@code null}.
	 */
	Tool descriptor();

	/**
	 * Returns the runtime argument class for binding.
	 *
	 * <p>
	 * Defaults to introspecting the {@code A} type parameter on the implementation; lambdas / heavily-generic
	 * implementations can override this method to provide it explicitly.
	 *
	 * @return The argument class. Never {@code null}.
	 */
	Class<A> argumentType();

	/**
	 * Invokes the tool with bound arguments.
	 *
	 * @param arguments Bound argument bean (may be {@code null} when no arguments are supplied).
	 * @param ctx Per-request bean store.
	 * @return The call result.
	 */
	R call(A arguments, BasicBeanStore ctx);
}
