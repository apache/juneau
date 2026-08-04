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

import org.apache.juneau.marshall.*;

/**
 * Terminal JSON-RPC result for a gracefully-closed {@code subscriptions/listen} stream.
 *
 * <p>
 * Carries no members beyond the inherited {@code resultType} (defaults to {@code "complete"}, per
 * {@link Result}) and {@code _meta}. Sent as the final frame on the SSE stream once the server closes the
 * subscription gracefully; an abrupt drop ends the stream with no terminal response at all.
 *
 * <p>
 * The inherited {@code _meta} here is a {@link ResultMeta}, not a {@link RequestMeta}; the subscription id
 * (see {@link RequestMeta#KEY_SUBSCRIPTION_ID}) is nonetheless carried on this terminal frame via
 * {@link ResultMeta}'s own untyped {@code set(String, Object)} extension.
 */
@Marshalled
public class SubscriptionsListenResult extends Result<SubscriptionsListenResult> {
}
