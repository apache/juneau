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
 * Authentication (AuthN) guards, servlet-layer filters, SPIs, and request-time helpers for
 * {@code juneau-rest-server}.
 *
 * <h5 class='topic'>Guard-level authentication ({@link org.apache.juneau.rest.guard.RestGuard})</h5>
 *
 * <p>
 * {@link org.apache.juneau.rest.auth.BearerTokenGuard} and {@link org.apache.juneau.rest.auth.ApiKeyGuard} are
 * framework-level guard implementations that run inside the Juneau request pipeline (after servlet routing).
 * They stash the resolved {@link java.security.Principal} on the request under
 * {@link org.apache.juneau.rest.RestServerConstants#PRINCIPAL_ATTR} so downstream handlers can read it via
 * {@link org.apache.juneau.rest.auth.AuthArg @Auth Principal} or
 * {@code req.getAttributes().get(PRINCIPAL_ATTR)}.
 *
 * <h5 class='topic'>Servlet-layer authentication ({@link org.apache.juneau.rest.auth.AuthFilter})</h5>
 *
 * <p>
 * This package also ships a two-layer pluggable servlet-filter authentication framework that runs <b>at the
 * servlet container layer</b> — before Juneau's request routing:
 *
 * <p>
 * <b>Layer 1 &mdash; Standalone filters</b>: each concrete {@link org.apache.juneau.rest.auth.AuthFilter}
 * subclass ({@link org.apache.juneau.rest.auth.BearerTokenAuthFilter},
 * {@link org.apache.juneau.rest.auth.ApiKeyAuthFilter}) is also a {@code jakarta.servlet.Filter} and can be
 * registered directly with a servlet container or Spring Boot's {@code FilterRegistrationBean}.
 *
 * <p>
 * <b>Layer 2 &mdash; {@link org.apache.juneau.rest.auth.AuthFilterChain} orchestrator</b>: composes multiple
 * {@code AuthFilter} instances with first-success principal resolution, role aggregation across all successful
 * filters, and all-failure aggregation into a single {@code 401} response.
 *
 * <h5 class='topic'>Optional&lt;AuthResult&gt; three-state contract</h5>
 *
 * <p>
 * {@link org.apache.juneau.rest.auth.AuthFilter#authenticate authenticate(HttpServletRequest)} returns:
 * <ul>
 * 	<li>{@link java.util.Optional#empty()} &mdash; filter does not apply (no recognizable credentials).
 * 	<li>throw {@link org.apache.juneau.rest.auth.AuthenticationException} &mdash; credentials present but invalid.
 * 	<li>{@link java.util.Optional#of(Object) Optional.of(AuthResult)} &mdash; authentication succeeded.
 * </ul>
 *
 * <h5 class='topic'>Principal and role surface</h5>
 *
 * <p>
 * On success the chain wraps the request in {@link org.apache.juneau.rest.auth.AuthenticatedRequestWrapper},
 * which overrides {@link jakarta.servlet.http.HttpServletRequest#getUserPrincipal()},
 * {@link jakarta.servlet.http.HttpServletRequest#isUserInRole(String)},
 * {@link jakarta.servlet.http.HttpServletRequest#getRemoteUser()}, and
 * {@link jakarta.servlet.http.HttpServletRequest#getAttribute(String)} for the
 * {@link org.apache.juneau.rest.RestServerConstants#PRINCIPAL_ATTR} key.
 * This makes {@link org.apache.juneau.rest.guard.RoleBasedRestGuard} and {@code @Auth Principal} arg injection work
 * with zero changes.
 *
 * <h5 class='topic'>SPIs</h5>
 *
 * <p>
 * {@link org.apache.juneau.rest.auth.TokenValidator} and {@link org.apache.juneau.rest.auth.ApiKeyStore} are the
 * backing SPIs for both the guard-level and filter-level authentication classes.
 * No third-party crypto / JOSE dependency is pulled in by this package — users who want JWT verification add the
 * optional {@code juneau-rest-server-jwt} sub-module (which plugs into the
 * {@link org.apache.juneau.rest.auth.TokenValidator} SPI from this package).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthFilterFramework">AuthN Filter Framework</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.auth;
