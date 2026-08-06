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
package org.apache.juneau.rest.client.mcp.v20260728;

/**
 * Thrown by the client-side MRTR (SEP-2322) auto-resume helpers
 * ({@link McpClient#callToolWithElicitation}, {@link McpClient#getPromptWithElicitation},
 * {@link McpClient#readResourceWithElicitation}) when a server keeps returning {@code input_required} pauses past
 * the loop's configured maximum number of resume rounds.
 *
 * <p>
 * This is a guard against an unbounded (or maliciously non-terminating) elicitation loop, distinct from a
 * server-reported JSON-RPC error (which surfaces as {@link org.apache.juneau.bean.jsonrpc.McpException}). It is an
 * unchecked exception so it does not widen the {@code throws} clause of the helper methods beyond
 * {@link java.io.IOException}; reaching it almost always indicates a server or handler that never converges rather
 * than a condition ordinary caller code can recover from.
 *
 * @since 10.0.0
 */
public class McpElicitationLimitException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int maxRounds;

	/**
	 * Constructor.
	 *
	 * @param maxRounds The configured maximum number of resume rounds that was exceeded.
	 */
	public McpElicitationLimitException(int maxRounds) {
		super("MCP elicitation auto-resume exceeded the maximum of " + maxRounds + " round(s) without reaching a terminal result.");
		this.maxRounds = maxRounds;
	}

	/**
	 * The configured maximum number of resume rounds that was exceeded.
	 *
	 * @return The max-rounds bound.
	 */
	public int getMaxRounds() {
		return maxRounds;
	}
}
