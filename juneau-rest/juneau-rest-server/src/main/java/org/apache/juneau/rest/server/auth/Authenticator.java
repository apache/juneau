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
package org.apache.juneau.rest.server.auth;

import java.util.*;

import jakarta.servlet.http.*;

/**
 * Lowest-level authentication contract shared by the servlet-filter layer ({@link AuthFilter},
 * {@link AuthFilterChain}) and the resource layer ({@link RestAuthenticator}).
 *
 * <p>
 * Three-state return contract:
 * <ul>
 * 	<li>{@link Optional#empty()} &mdash; this authenticator does not apply (no recognizable credentials).
 * 	<li>{@link Optional#of(Object) Optional.of(AuthResult)} &mdash; authentication succeeded.
 * 	<li>throw {@link AuthenticationException} &mdash; credentials were present but invalid.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AuthFilter}
 * 	<li class='jc'>{@link AuthFilterChain}
 * 	<li class='jc'>{@link RestAuthenticator}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerAuthenticator">REST Authenticator</a>
 * </ul>
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface Authenticator {

	/**
	 * Inspects the request and returns the authentication result.
	 *
	 * @param req The incoming HTTP request. Never <jk>null</jk>.
	 * @return The authentication result, or {@link Optional#empty()} if this authenticator does not apply.
	 * @throws AuthenticationException If the request carries recognizable credentials that are invalid.
	 */
	Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException;
}
