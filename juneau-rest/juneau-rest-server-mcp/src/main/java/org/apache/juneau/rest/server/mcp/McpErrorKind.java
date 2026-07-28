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

/**
 * Revision-neutral classification of a dispatch failure.
 *
 * <p>
 * The core raises a <em>kind</em>; each revision maps kinds to its own JSON-RPC error codes via
 * {@link McpRevision#errorCode(McpErrorKind)}. Several kinds legitimately map to the same code on a
 * given revision — the point of the enum is that the core never has to know which.
 */
public enum McpErrorKind {

	/** The JSON-RPC envelope itself was unusable (absent or empty {@code method}, null envelope). */
	INVALID_REQUEST,

	/** The requested top-level JSON-RPC method is not implemented by this revision. */
	UNKNOWN_METHOD,

	/** A {@code tools/call} named a tool that is not registered. */
	TOOL_NOT_FOUND,

	/** A {@code prompts/get} named a prompt that is not registered. */
	PROMPT_NOT_FOUND,

	/** A {@code resources/read} named a resource that is not registered. */
	RESOURCE_NOT_FOUND,

	/** A required parameter was missing, or a parameter had the wrong JSON shape. */
	INVALID_PARAMS,

	/** A handler threw an unexpected exception. */
	INTERNAL_ERROR,

	/**
	 * The request body could not be parsed as JSON.
	 *
	 * <p>
	 * No dispatch path raises this kind today — JSON parsing happens in the REST layer before
	 * dispatch is reached. The constant exists so a revision's code table is complete; that is not
	 * the same claim as the path being reachable.
	 */
	PARSE_ERROR
}
