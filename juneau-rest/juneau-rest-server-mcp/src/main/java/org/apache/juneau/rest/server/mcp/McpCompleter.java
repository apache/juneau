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

import org.apache.juneau.commons.inject.*;

/**
 * Application behavior invoked for a {@code completion/complete} request against a declared prompt
 * argument ({@link McpPromptArgument#getCompleter()}) or resource-template variable
 * ({@link McpResourceTemplateHandler#completer(String)}).
 *
 * <p>
 * Order and duplicates in the returned {@link McpCompletionResult#getValues() values} are
 * application-owned: this SPI performs no ranking, filtering, deduplication, or pagination beyond the
 * 100-value cap documented on {@link McpCompletionResult}.
 */
@FunctionalInterface
public interface McpCompleter {

	/**
	 * Computes completion suggestions for the current value of one prompt argument or template
	 * variable.
	 *
	 * @param request The completion request. Never <jk>null</jk>.
	 * @param ctx Per-request bean store. Never <jk>null</jk>.
	 * @return The completion result. A <jk>null</jk> return is an internal handler failure - see
	 * 	{@link McpCompletionResult#normalize(McpCompletionResult)}.
	 */
	McpCompletionResult complete(McpCompletionRequest request, BeanStore ctx);
}
