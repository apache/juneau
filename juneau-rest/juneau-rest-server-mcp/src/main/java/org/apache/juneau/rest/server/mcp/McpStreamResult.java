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

import java.util.concurrent.Flow;

import org.apache.juneau.marshall.sse.SseEvent;

/**
 * A {@link McpDispatchResult} wrapping a held-open streaming publisher, for a revision-specific method
 * whose successful result is not a single JSON-RPC envelope (for example the {@code 2026-07-28}
 * {@code subscriptions/listen} method).
 *
 * @param stream The event-stream publisher. Never <jk>null</jk>.
 * @since 10.0.0
 */
public record McpStreamResult(Flow.Publisher<SseEvent> stream) implements McpDispatchResult {}
