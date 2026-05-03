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
package org.apache.juneau.bean.mcp;

/**
 * MCP protocol version strings carried in {@code initialize} requests and results.
 */
@SuppressWarnings("java:S115")
public final class McpProtocol {

	private McpProtocol() {}

	/** JSON-RPC version token. */
	public static final String JSON_RPC_2_0 = "2.0";

	/**
	 * Default MCP protocol revision targeted by this bean module.
	 *
	 * <p>
	 * Matches the revision commonly used by current MCP HTTP deployments; adjust when the spec moves.
	 */
	public static final String VERSION_2025_06_18 = "2025-06-18";
}
