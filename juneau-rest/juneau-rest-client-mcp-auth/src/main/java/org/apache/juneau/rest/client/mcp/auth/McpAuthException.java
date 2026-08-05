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
package org.apache.juneau.rest.client.mcp.auth;

/**
 * Runtime exception raised by the MCP client-side auth layer when metadata discovery, PRM fetch/parse, an issuer
 * ({@code iss}) validation check, or token acquisition fails.
 *
 * <p>
 * Unchecked so it can propagate cleanly out of a {@link java.util.function.Supplier#get()} token-supplier call fed to
 * {@code McpAuthInterceptor} (which aborts the request when the supplier throws).
 *
 * @since 10.0.0
 */
public class McpAuthException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param msg The detail message.  May be {@code null}.
	 */
	public McpAuthException(String msg) {
		super(msg);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The detail message.  May be {@code null}.
	 * @param cause The cause.  May be {@code null}.
	 */
	public McpAuthException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
