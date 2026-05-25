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
 * Authentication (AuthN) guards, SPIs, and request-time helpers for {@code juneau-rest-server}.
 *
 * <p>
 * This package ships the framework-level AuthN surface &mdash; bearer-token and API-key
 * {@link org.apache.juneau.rest.guard.RestGuard} implementations, the {@link org.apache.juneau.rest.auth.TokenValidator}
 * and {@link org.apache.juneau.rest.auth.ApiKeyStore} SPIs that back them, an
 * {@link org.apache.juneau.rest.auth.AuthArg @Auth Principal} arg resolver, and the
 * {@link org.apache.juneau.rest.auth.AuthenticationException} HTTP error type with its
 * {@code WWW-Authenticate} fluent setter.
 *
 * <p>
 * No third-party crypto / JOSE dependency is pulled in by this package &mdash; users who want
 * JWT verification add the optional {@code juneau-rest-server-jwt} sub-module (which plugs into the
 * {@link org.apache.juneau.rest.auth.TokenValidator} SPI from this package).
 *
 * <h5 class='topic'>Composition example</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja>(name=<js>"guards"</js>)
 * 		<jk>public</jk> RestGuardList guards(BeanStore <jv>bs</jv>) {
 * 			<jk>return</jk> RestGuardList.<jsm>create</jsm>(<jv>bs</jv>)
 * 				.append(
 * 					BearerTokenGuard.<jsm>create</jsm>()
 * 						.realm(<js>"api"</js>)
 * 						.validator(<jv>myTokenValidator</jv>)
 * 						.build())
 * 				.build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(path=<js>"/me"</js>)
 * 		<jk>public</jk> Profile me(<ja>@Auth</ja> Principal <jv>p</jv>) {
 * 			<jk>return</jk> <jf>profileService</jf>.lookup(<jv>p</jv>.getName());
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 9.5.0
 */
package org.apache.juneau.rest.auth;
