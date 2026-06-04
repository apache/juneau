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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.zip.*;

import org.apache.juneau.rest.auth.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * {@link AuthFilter} that authenticates requests carrying a SAML 2.0 {@code <samlp:Response>} delivered via
 * the {@link SamlBinding#POST} or {@link SamlBinding#REDIRECT} HTTP binding.
 *
 * <p>
 * On each request this filter:
 * <ol>
 * 	<li>Inspects the URL path against the configured {@code consumerPath} (default {@code /saml/acs}).  If the
 * 		request path does not match, returns {@link Optional#empty()} (filter does not apply).
 * 	<li>For the {@link SamlBinding#POST} binding: reads the {@code SAMLResponse} form parameter and
 * 		base64-decodes it.  For {@link SamlBinding#REDIRECT}: reads the {@code SAMLResponse} query parameter,
 * 		base64-decodes it, and then DEFLATE-inflates (RFC 1951) per OASIS SAML 2.0 Redirect binding rules.
 * 	<li>Delegates to {@link SamlAssertionValidator#validate(String)}.  On success, builds an {@link AuthResult}
 * 		carrying the resolved {@link ClaimsPrincipal}.  On failure, re-throws as an
 * 		{@link AuthenticationException} with a {@code WWW-Authenticate: SAML ...} challenge.
 * </ol>
 *
 * <h5 class='topic'>Roles</h5>
 * <p>
 * If the resolved {@link ClaimsPrincipal} contains a claim whose name equals the configured
 * {@code rolesClaim} (default {@code "roles"}) and whose value is a {@link List} of {@link String}, those role
 * names are populated on the {@link AuthResult}.  Otherwise the role set is empty.
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<jk>var</jk> validator = SamlAssertionValidator.<jsm>create</jsm>()
 * 		.metadataResolver(SamlMetadataResolvers.<jsm>url</jsm>(<js>"https://idp.example.com/metadata"</js>))
 * 		.spEntityId(<js>"https://sp.example.com"</js>)
 * 		.expectedIssuer(<js>"https://idp.example.com"</js>)
 * 		.build();
 *
 * 	AuthFilterChain.<jsm>create</jsm>(<jv>bs</jv>)
 * 		.append(SamlAuthFilter.<jsm>create</jsm>()
 * 			.consumerPath(<js>"/saml/acs"</js>)
 * 			.binding(SamlBinding.<jsf>POST</jsf>)
 * 			.validator(<jv>validator</jv>)
 * 			.build())
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link SamlAssertionValidator}
 * 	<li class='jc'>{@link SamlBinding}
 * 	<li class='link'><a class="doclink" href="https://docs.oasis-open.org/security/saml/v2.0/saml-bindings-2.0-os.pdf">SAML 2.0 Bindings (OASIS)</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthFilterFramework">AuthN Filter Framework</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are SAML attribute names and HTTP header values; intentional
})
public class SamlAuthFilter extends AuthFilter {

	private static final String DEFAULT_CONSUMER_PATH = "/saml/acs";
	private static final String DEFAULT_ROLES_CLAIM = "roles";
	private static final String SAML_RESPONSE_PARAM = "SAMLResponse";

	/**
	 * Static creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder for {@link SamlAuthFilter}.
	 */
	public static class Builder {
		private SamlAssertionValidator validator;
		private SamlBinding binding = SamlBinding.POST;
		private String consumerPath = DEFAULT_CONSUMER_PATH;
		private String rolesClaim = DEFAULT_ROLES_CLAIM;
		private String realm = "saml";

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the SAML assertion validator.
		 *
		 * @param value The validator.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder validator(SamlAssertionValidator value) {
			validator = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the SAML binding the filter listens for.  Defaults to {@link SamlBinding#POST}.
		 *
		 * @param value The binding.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder binding(SamlBinding value) {
			binding = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the request path that carries the SAML assertion.  Defaults to {@code /saml/acs}.
		 *
		 * @param value The request path.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder consumerPath(String value) {
			consumerPath = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the claim name used to extract roles from the resolved {@link ClaimsPrincipal}.
		 *
		 * @param value The claim name.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder rolesClaim(String value) {
			rolesClaim = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the {@code WWW-Authenticate: SAML realm="<value>"} challenge realm.
		 *
		 * @param value The realm.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder realm(String value) {
			realm = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Builds the filter.
		 *
		 * @return A new {@link SamlAuthFilter}.
		 */
		public SamlAuthFilter build() {
			if (validator == null)
				throw new IllegalStateException("SamlAuthFilter requires a SamlAssertionValidator");
			return new SamlAuthFilter(this);
		}
	}

	private final SamlAssertionValidator validator;
	private final SamlBinding binding;
	private final String consumerPath;
	private final String rolesClaim;
	private final String challenge;

	/**
	 * Constructor.
	 *
	 * @param b The builder to read configuration from.
	 */
	protected SamlAuthFilter(Builder b) {
		this.validator = b.validator;
		this.binding = b.binding;
		this.consumerPath = b.consumerPath;
		this.rolesClaim = b.rolesClaim;
		this.challenge = "SAML realm=\"" + b.realm + "\"";
	}

	@Override /* Overridden from AuthFilter */
	public Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException {
		if (!matchesPath(req))
			return opte();
		var raw = req.getParameter(SAML_RESPONSE_PARAM);
		if (isEmpty(raw))
			return opte();
		var xml = decodeSamlResponse(raw);
		var principal = runValidator(xml);
		return opt(AuthResult.of(principal, extractRoles(principal)));
	}

	private boolean matchesPath(HttpServletRequest req) {
		var p = req.getPathInfo();
		var s = p != null ? p : req.getServletPath();
		return s != null && s.equals(consumerPath);
	}

	private String decodeSamlResponse(String raw) throws AuthenticationException {
		try {
			byte[] decoded = Base64.getMimeDecoder().decode(raw);
			if (binding == SamlBinding.REDIRECT) {
				// Per SAML 2.0 Redirect binding: deflate using raw DEFLATE (no zlib wrapper).
				return inflate(decoded);
			}
			return new String(decoded, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			throw new AuthenticationException(e, "SAMLResponse parameter is not valid base64")
				.wwwAuthenticate(challenge);
		} catch (IOException e) {
			throw new AuthenticationException(e, "SAMLResponse parameter could not be inflated (REDIRECT binding)")
				.wwwAuthenticate(challenge);
		}
	}

	private static String inflate(byte[] data) throws IOException {
		var inflater = new Inflater(true);  // nowrap=true: raw DEFLATE per SAML Redirect.
		inflater.setInput(data);
		var out = new ByteArrayOutputStream();
		var buf = new byte[4096];
		try {
			while (!inflater.finished()) {
				int n = inflater.inflate(buf);
				if (n == 0) {
					if (inflater.needsInput() || inflater.needsDictionary())
						throw new IOException("SAMLResponse: DEFLATE stream truncated");
					break;
				}
				out.write(buf, 0, n);
			}
		} catch (DataFormatException e) {
			throw new IOException("SAMLResponse: malformed DEFLATE", e);
		} finally {
			inflater.end();
		}
		return out.toString(StandardCharsets.UTF_8);
	}

	private Principal runValidator(String xml) throws AuthenticationException {
		try {
			var p = validator.validate(xml);
			if (p == null)
				throw new AuthenticationException("SAML validator returned null").wwwAuthenticate(challenge);
			return p;
		} catch (AuthenticationException e) {
			var hasChallenge = e.getHeaders().stream()
				.anyMatch(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName()));
			if (!hasChallenge)
				e.wwwAuthenticate(challenge);
			throw e;
		} catch (RuntimeException e) {
			throw new AuthenticationException(e, "SAML validation failed").wwwAuthenticate(challenge);
		}
	}

	@SuppressWarnings({
		"unchecked" // Type erasure on reflective/generic cast; element type is verified at call site
	})
	private Set<String> extractRoles(Principal principal) {
		if (principal instanceof ClaimsPrincipal cp) {
			var v = cp.getClaims().get(rolesClaim);
			if (v instanceof List<?> list) {
				var roles = new HashSet<String>();
				for (var item : (List<Object>) list)
					if (item instanceof String s)
						roles.add(s);
				return roles;
			}
		}
		return Collections.emptySet();
	}
}
