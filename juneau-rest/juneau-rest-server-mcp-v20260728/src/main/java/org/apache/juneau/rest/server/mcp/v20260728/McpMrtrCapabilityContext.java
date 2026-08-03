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
package org.apache.juneau.rest.server.mcp.v20260728;

/**
 * Bean always stashed into the {@link org.apache.juneau.commons.inject.BeanStore} passed to a
 * {@code tools/call}/{@code prompts/get}/{@code resources/read} handler (both first-round and resume calls),
 * exposing whether the original request's client advertised MRTR/elicitation support.
 *
 * <p>
 * Lets a handler pre-check and degrade (return a normal result or a domain error) instead of throwing
 * {@link McpInputRequiredSignal} into a dispatcher that would just reject it with
 * {@code MISSING_REQUIRED_CLIENT_CAPABILITY} &mdash; satisfying the spec's &sect;4 "client's capability is
 * EXPOSED to handlers" requirement.
 *
 * @param elicitationSupported Whether the original request's {@code _meta.clientCapabilities} included a
 * 	non-<jk>null</jk> {@code elicitation} capability.
 */
public record McpMrtrCapabilityContext(boolean elicitationSupported) {
}
