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
 * JWT bearer-token validation backed by {@code com.nimbusds:nimbus-jose-jwt}.
 *
 * <p>
 * This is the optional companion to {@link org.apache.juneau.rest.auth.BearerTokenGuard} in
 * {@code juneau-rest-server}. Adding this module to a service's classpath lets the bearer guard
 * delegate to {@link org.apache.juneau.rest.auth.jwt.JwtTokenValidator} for full JWT validation
 * (signature, {@code iss}, {@code aud}, {@code exp}, {@code nbf}) without bringing the
 * nimbus library into the core {@code juneau-rest-server} jar.
 *
 * <h5 class='topic'>Containment</h5>
 *
 * <p>
 * The {@code nimbus-jose-jwt} dependency is declared in {@code provided} scope on this module's
 * POM. Consumers that want JWT validation must add {@code nimbus-jose-jwt} themselves at the
 * version of their choice. This way:
 * <ul>
 * 	<li>The core {@code juneau-rest-server} jar stays JWT-free.
 * 	<li>Services that never use JWT pay zero classpath cost.
 * 	<li>Services that DO use JWT pick their own nimbus version (security upgrades, CVE patches)
 * 		without waiting for a Juneau release.
 * </ul>
 *
 * <h5 class='topic'>Security defaults</h5>
 *
 * <p>
 * {@link org.apache.juneau.rest.auth.jwt.JwtTokenValidator} ships with secure defaults:
 * <ul>
 * 	<li>{@code alg: none} is permanently rejected — there is no opt-in.
 * 	<li>{@code HS256} is rejected unless explicitly allowlisted via
 * 		{@link org.apache.juneau.rest.auth.jwt.JwtTokenValidator.Builder#algorithms algorithms(...)}.
 * 		Mixing HMAC and asymmetric keys in the same validator is intentionally prevented.
 * 	<li>{@code iss}, {@code aud}, {@code exp}, {@code nbf} are mandatory by default. A token missing
 * 		any of these is rejected.
 * 	<li>Clock-skew tolerance defaults to {@code 60s} and is capped at {@code 300s} by the builder.
 * 	<li>JWKS keys are cached for 5 minutes. Cache continues to serve stale keys when the JWKS
 * 		endpoint is unreachable, logging a warning, so a transient network blip doesn't take
 * 		down auth for the whole service.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.BearerTokenGuard}
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.TokenValidator}
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.ClaimsPrincipal}
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519 — JSON Web Token</a>
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7517">RFC 7517 — JSON Web Key</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.auth.jwt;
