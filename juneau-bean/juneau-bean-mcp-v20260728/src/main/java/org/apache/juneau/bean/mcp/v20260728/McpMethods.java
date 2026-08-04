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
package org.apache.juneau.bean.mcp.v20260728;

/**
 * JSON-RPC method names used by MCP over HTTP.
 */
@SuppressWarnings({
	"java:S115" // Constant names mirror MCP protocol field names exactly (e.g., TOOLS_LIST = "tools/list").
})
public final class McpMethods {

	private McpMethods() {}

	/** Discover server identity and capabilities. */
	public static final String SERVER_DISCOVER = "server/discover";

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

	/** List resource templates. */
	public static final String RESOURCES_TEMPLATES_LIST = "resources/templates/list";

	/** Complete a prompt argument or resource-template variable. */
	public static final String COMPLETION_COMPLETE = "completion/complete";

	/**
	 * Server-to-client duplex sampling request (MCP sampling). Never dispatched through
	 * {@code McpRevision} — flows over the {@code McpDuplexDispatcher}/{@code McpServerRequestHandler} seam.
	 */
	public static final String SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

	/**
	 * Start a held-open notification stream (SEP-2575). Replaces {@code resources/subscribe}/
	 * {@code resources/unsubscribe} and the old HTTP GET SSE endpoint.
	 */
	public static final String SUBSCRIPTIONS_LISTEN = "subscriptions/listen";

	/** Notification: a subscribed resource's content changed. Only deliverable inside a {@link #SUBSCRIPTIONS_LISTEN} stream. */
	public static final String NOTIFICATIONS_RESOURCES_UPDATED = "notifications/resources/updated";

	/** Notification: the resource list changed. Only deliverable inside a {@link #SUBSCRIPTIONS_LISTEN} stream. */
	public static final String NOTIFICATIONS_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

	/** Notification: the tool list changed. Only deliverable inside a {@link #SUBSCRIPTIONS_LISTEN} stream. */
	public static final String NOTIFICATIONS_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

	/** Notification: the prompt list changed. Only deliverable inside a {@link #SUBSCRIPTIONS_LISTEN} stream. */
	public static final String NOTIFICATIONS_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

	/** Mandatory first frame on every {@link #SUBSCRIPTIONS_LISTEN} stream, echoing the honored filter. */
	public static final String NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED = "notifications/subscriptions/acknowledged";
}
