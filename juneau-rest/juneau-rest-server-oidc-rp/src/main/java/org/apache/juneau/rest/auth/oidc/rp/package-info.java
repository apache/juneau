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
 * OpenID Connect Relying Party (RP) login support for Juneau REST servers.
 *
 * <p>
 * This opt-in module turns a Juneau REST server into a complete OpenID Connect <i>Relying Party</i> &mdash;
 * the "Log in with Google / Okta / Entra ID / Keycloak" end-to-end glue &mdash; sitting on top of
 * {@code juneau-rest-server-oauth}.  It orchestrates the browser redirect &rarr; callback &rarr; session
 * dance, owning the {@code state} / {@code nonce} / PKCE bookkeeping, strict ID-token validation, the
 * session SPI, and the request-time session&rarr;identity binding.
 *
 * <p>
 * The {@code com.nimbusds:oauth2-oidc-sdk} dependency is {@code provided}-scoped and contained at the
 * module boundary &mdash; it never leaks transitively.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.auth.oidc.rp.OidcRelyingParty}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/OidcRelyingParty">OIDC Relying Party</a>
 * 	<li class='link'><a class="doclink" href="https://openid.net/specs/openid-connect-core-1_0.html">OpenID Connect Core 1.0</a>
 * </ul>
 */
package org.apache.juneau.rest.auth.oidc.rp;
