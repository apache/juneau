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
/**
 * Client-side OAuth 2.1 token acquisition for the Model Context Protocol {@code 2026-07-28} revision.
 *
 * <p>
 * This module supplies the token-provider implementations that plug into the {@code McpAuthInterceptor} seam of
 * {@code juneau-rest-client-mcp} (its {@code Supplier<String>}).  It deliberately does <b>not</b> depend on
 * {@code juneau-rest-server} &mdash; the client-side flow/discovery helpers are relocated (see the {@code flow} and
 * {@code oidc} sub-packages) rather than reused from {@code juneau-rest-server-auth-oauth}, which compile-depends on
 * the server.  Sub-project D (auth-module de-layering) is expected to later rationalize this relocation.
 *
 * <h5 class='topic'>What's here (F1)</h5>
 * <ul>
 * 	<li>{@link org.apache.juneau.rest.client.mcp.auth.McpTokenProvider} &mdash; a {@code Supplier<String>} token
 * 		manager for pre-provisioned (static) tokens, the client-credentials grant, and the refresh-token grant
 * 		(SEP-2207, with rotated-refresh-token capture); all requests carry the RFC 8707 {@code resource} indicator.
 * 	<li>{@link org.apache.juneau.rest.client.mcp.auth.McpProtectedResourceMetadataClient} and
 * 		{@link org.apache.juneau.rest.client.mcp.auth.WwwAuthenticateChallenge} &mdash; the client half of the OAuth
 * 		2.1 baseline: consume a {@code 401 WWW-Authenticate resource_metadata} pointer, fetch the RFC 9728 PRM
 * 		document, select an authorization server, and validate its issuer via RFC 8414 / OIDC discovery.
 * 	<li>{@link org.apache.juneau.rest.client.mcp.auth.McpAuthorizationCodeAcquirer} and
 * 		{@link org.apache.juneau.rest.client.mcp.auth.LoopbackRedirectReceiver} &mdash; interactive
 * 		authorization-code + PKCE (S256) acquisition over a loopback redirect for headless/CLI use, including the
 * 		SEP-2468 {@code iss} check on the callback.
 * </ul>
 *
 * <p>
 * Adoption-focused documentation for this module is intentionally tracked as a separate follow-up (not part of F1).
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.client.mcp.auth;
