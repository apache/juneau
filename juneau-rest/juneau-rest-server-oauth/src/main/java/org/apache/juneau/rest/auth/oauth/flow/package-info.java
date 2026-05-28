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
 * OAuth 2.0 client-side flow helpers.
 *
 * <p>
 * Thin facades over Nimbus SDK grant types and {@code TokenRequest}.  Each helper exposes a Juneau-style
 * builder and returns a Juneau-native {@link org.apache.juneau.rest.auth.oauth.OAuthToken} record.
 *
 * <h5 class='topic'>Flow inventory</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.oauth.flow.OAuthClientCredentialsFlow}
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.oauth.flow.OAuthAuthorizationCodeFlow} (with PKCE)
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.oauth.flow.OAuthRefreshTokenFlow}
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.oauth.flow.OAuthResourceOwnerFlow} (discouraged)
 * </ul>
 *
 * @since 9.5.0
 */
package org.apache.juneau.rest.auth.oauth.flow;
