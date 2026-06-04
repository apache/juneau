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
 * OAuth 2.0 / OIDC integration for the Juneau REST server.
 *
 * <p>
 * Layered on the {@link org.apache.juneau.rest.auth.AuthFilter AuthFilter} framework and the
 * {@link org.apache.juneau.rest.auth.TokenValidator TokenValidator} SPI.  Wraps Nimbus's
 * {@code oauth2-oidc-sdk} (Apache 2.0) for the introspection, flow, and discovery wire shapes.
 *
 * <p>
 * The Nimbus dependency is declared {@code provided}-scope in {@code juneau-rest-server-oauth}'s POM, so
 * it never bleeds into the dependency tree of an upstream module.  Consumers who want OAuth functionality
 * declare {@code com.nimbusds:oauth2-oidc-sdk} themselves at runtime.
 *
 * <h5 class='topic'>Key types</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.oauth.OAuthIntrospectionValidator}
 * 		&mdash; opaque-token validator wrapping Nimbus's RFC 7662 introspection client.
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.oauth.OAuthFilter}
 * 		&mdash; servlet filter that recognizes {@code Authorization: Bearer ...} OAuth tokens.
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.oauth.TokenCache}
 * 		&mdash; cache SPI used by the validator (and flow helpers); default impl
 * 		{@link org.apache.juneau.rest.auth.oauth.BoundedLruTokenCache}.
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.oauth.OAuthToken}
 * 		&mdash; immutable token record produced by every flow helper.
 * 	<li class='jp'>{@link org.apache.juneau.rest.auth.oauth.flow flow}
 * 		&mdash; client-side flow helpers (client-credentials, authorization-code with PKCE, refresh-token,
 * 		and the discouraged resource-owner password-credentials).
 * 	<li class='jp'>{@link org.apache.juneau.rest.auth.oauth.oidc oidc}
 * 		&mdash; OIDC discovery client + metadata record.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6749">RFC 6749 &mdash; OAuth 2.0</a>
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7662">RFC 7662 &mdash; Token Introspection</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/OAuthAuthSupport">OAuth Auth Support</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.auth.oauth;
