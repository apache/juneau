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
 * JSON-RPC method names used by MCP over HTTP.
 */
@SuppressWarnings("java:S115")
public final class McpMethods {

	private McpMethods() {}

	/** Initialize handshake. */
	public static final String INITIALIZE = "initialize";

	/** Liveness / keepalive. */
	public static final String PING = "ping";

	/** List tools. */
	public static final String TOOLS_LIST = "tools/list";

	/** Execute a tool. */
	public static final String TOOLS_CALL = "tools/call";

	/** List prompts. */
	public static final String PROMPTS_LIST = "prompts/list";

	/** Fetch a prompt. */
	public static final String PROMPTS_GET = "prompts/get";

	/** List resources. */
	public static final String RESOURCES_LIST = "resources/list";

	/** Read a resource. */
	public static final String RESOURCES_READ = "resources/read";
}
