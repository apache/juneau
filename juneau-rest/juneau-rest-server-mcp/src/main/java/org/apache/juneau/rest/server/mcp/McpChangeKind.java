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
 * The kind of change a neutral MCP subscription can be notified about.
 *
 * <p>
 * Mirrors the four notification shapes SEP-2575 defines for {@code subscriptions/listen}: one per-resource
 * change and three list-changed signals. {@link #RESOURCE_UPDATED} is the only kind that carries a resource
 * URI (see {@link McpChangeEvent#getResourceUri()}).
 */
public enum McpChangeKind {

	/** A specific subscribed resource's content changed. */
	RESOURCE_UPDATED,

	/** The tool list changed. */
	TOOLS_LIST_CHANGED,

	/** The prompt list changed. */
	PROMPTS_LIST_CHANGED,

	/** The resource list changed. */
	RESOURCES_LIST_CHANGED
}
