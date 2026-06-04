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
package org.apache.juneau.rest.auth.saml;

/**
 * SAML 2.0 bindings supported by {@link SamlAuthFilter} for delivering a {@code <Response>} from an IdP to the SP.
 *
 * <p>
 * v1 ships the two HTTP-flavored bindings.  Artifact and SOAP back-channel bindings are deferred.
 *
 * <h5 class='topic'>HTTP POST binding</h5>
 * <p>
 * The IdP returns an auto-submitting HTML form whose {@code SAMLResponse} field carries the base64-encoded
 * {@code <Response>} document.  The browser POSTs the form to the SP's assertion-consumer URL.  This is the
 * dominant binding for SAML 2.0 web SSO.
 *
 * <h5 class='topic'>HTTP Redirect binding</h5>
 * <p>
 * The IdP redirects the browser to the SP with the {@code SAMLResponse} carried in a query-string parameter.
 * The value is deflated (RFC 1951) and then base64-encoded so that even mid-size assertions fit inside URL
 * length limits.  This binding is more commonly used for {@code <AuthnRequest>} than for {@code <Response>},
 * but some IdPs offer it for assertions too.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link SamlAuthFilter}
 * 	<li class='link'><a class="doclink" href="https://docs.oasis-open.org/security/saml/v2.0/saml-bindings-2.0-os.pdf">SAML 2.0 Bindings (OASIS)</a>
 * </ul>
 *
 * @since 10.0.0
 */
public enum SamlBinding {

	/**
	 * HTTP POST binding.  {@code SAMLResponse} carried as a base64-encoded form field on a POST request.
	 */
	POST,

	/**
	 * HTTP Redirect binding.  {@code SAMLResponse} carried as a deflated + base64-encoded query parameter on a
	 * GET request.
	 */
	REDIRECT
}
