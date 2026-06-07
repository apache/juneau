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
 * SAML 2.0 assertion validation for the Juneau REST server, layered on the
 * {@link org.apache.juneau.rest.server.auth.AuthFilter AuthFilter} framework.
 *
 * <p>
 * Wraps OpenSAML 5.x ({@code org.opensaml:opensaml-*}) behind a small, opinionated facade with strict
 * security defaults.  The OpenSAML dependencies are declared {@code provided}-scope in
 * {@code juneau-rest-server-saml}'s POM, so they never bleed into the dependency tree of an upstream module.
 *
 * <h5 class='topic'>Key types</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.auth.saml.SamlAssertionValidator}
 * 		&mdash; validates a {@code <samlp:Response>} XML document and returns a
 * 		{@link org.apache.juneau.rest.server.auth.ClaimsPrincipal} marked with {@code issuerType=SAML}.
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.auth.saml.SamlAuthFilter}
 * 		&mdash; servlet filter that recognizes SAML responses delivered via HTTP POST or HTTP Redirect.
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.auth.saml.SamlBinding}
 * 		&mdash; enum identifying the SAML 2.0 transport binding the filter expects.
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.auth.saml.SamlMetadataResolvers}
 * 		&mdash; convenience factories that wrap OpenSAML's {@code FilesystemMetadataResolver} and
 * 		{@code DOMMetadataResolver}.
 * </ul>
 *
 * <h5 class='topic'>Security defaults</h5>
 * <ul>
 * 	<li><b>Signature required</b> &mdash; unsigned assertions are rejected.
 * 	<li><b>Strict algorithm allowlist</b> &mdash; only {@code rsa-sha256} and {@code ecdsa-sha256}; SHA-1
 * 		is permanently rejected at the builder.
 * 	<li><b>Clock skew</b> &mdash; defaults to 60 seconds tolerance, capped at 5 minutes.
 * 	<li><b>Audience restriction</b> &mdash; the assertion's {@code <AudienceRestriction>} must list the SP
 * 		entity ID configured on the validator.
 * 	<li><b>Decryption</b> &mdash; {@code <EncryptedAssertion>} requires an explicit
 * 		{@link org.apache.juneau.rest.server.auth.saml.SamlAssertionValidator.Builder#decryptionCredential(org.opensaml.security.credential.Credential) decryptionCredential}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf">SAML 2.0 Core (OASIS)</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SamlAuthSupport">SAML Auth Support</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.auth.saml;
