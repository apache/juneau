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
 * The non-notification result of one {@link McpRevision#dispatch(McpExchange, McpServerConfig, org.apache.juneau.commons.inject.BeanStore)}
 * call: either a complete JSON-RPC response envelope ({@link McpResponseResult}) or a held-open streaming
 * publisher ({@link McpStreamResult}, for a revision-specific method whose successful result is not a
 * single JSON-RPC envelope, e.g. the {@code 2026-07-28} {@code subscriptions/listen} method).
 *
 * <p>
 * {@code dispatch(...)} may still return plain <jk>null</jk> (neither variant) for a notification
 * request, which the HTTP layer renders as an empty body — see {@link McpRevision#dispatch}.
 *
 * <p>
 * Introduced so callers {@code switch}/{@code instanceof}-pattern-match over a closed, compiler-checked
 * set of outcomes instead of casting the historical {@code Object}-typed return value (work item 331).
 *
 * @since 10.0.0
 */
public sealed interface McpDispatchResult permits McpResponseResult, McpStreamResult {}
