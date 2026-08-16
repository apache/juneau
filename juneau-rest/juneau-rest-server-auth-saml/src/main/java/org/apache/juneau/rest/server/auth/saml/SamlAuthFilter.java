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
package org.apache.juneau.rest.server.auth.saml;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.rest.server.auth.*;

import jakarta.servlet.http.*;

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
 * 	<li>Derives the ACS recipient URL (scheme, host, port, and path) from the current request and delegates
 * 		to {@link SamlAssertionValidator#validate(String, String)} with it, so the assertion's bearer
 * 		{@code <SubjectConfirmation>} is always bound to the actual endpoint this request was delivered to
 * 		&mdash; regardless of whether the {@link SamlAssertionValidator} was itself built with
 * 		{@link SamlAssertionValidator.Builder#recipient(String) recipient(...)} configured.  On success,
 * 		builds an {@link AuthResult} carrying the resolved {@link ClaimsPrincipal}.  On failure, re-throws as
 * 		an {@link AuthenticationException} with a {@code WWW-Authenticate: SAML ...} challenge.
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
 * <p>
 * Note that {@code validator} below is <i>not</i> built with
 * {@link SamlAssertionValidator.Builder#recipient(String) recipient(...)}.  That is safe here because this
 * filter always derives the ACS recipient from the request and binds it for that call, so bearer
 * subject-confirmation is enforced regardless of the validator's own configuration.
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
	 * Default ceiling on the number of decompressed bytes accepted from a REDIRECT-binding
	 * {@code SAMLResponse} (1 MiB).
	 */
	public static final long DEFAULT_MAX_INFLATED_BYTES = 1L * 1024 * 1024;

	/**
	 * Default ceiling on the decompressed-to-compressed size ratio accepted from a REDIRECT-binding
	 * {@code SAMLResponse}.
	 */
	public static final int DEFAULT_MAX_INFLATE_RATIO = 100;

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
		private long maxInflatedBytes = DEFAULT_MAX_INFLATED_BYTES;
		private int maxInflateRatio = DEFAULT_MAX_INFLATE_RATIO;

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
		 * Sets the ceiling on the number of decompressed bytes accepted from a {@link SamlBinding#REDIRECT}
		 * {@code SAMLResponse}.  Defaults to {@link SamlAuthFilter#DEFAULT_MAX_INFLATED_BYTES}.
		 *
		 * <p>
		 * A REDIRECT-binding {@code SAMLResponse} is DEFLATE-compressed in transit, so a small query-string
		 * payload can expand to a much larger document.  Decompression stops and the request is rejected once
		 * the running output exceeds this cap, so a highly-compressible payload cannot drive an unbounded
		 * allocation.  A value {@code <= 0} disables the absolute cap (the ratio guard still applies).
		 *
		 * @param value The maximum decompressed size in bytes.
		 * @return This object.
		 */
		public Builder maxInflatedBytes(long value) {
			maxInflatedBytes = value;
			return this;
		}

		/**
		 * Sets the ceiling on the decompressed-to-compressed size ratio accepted from a
		 * {@link SamlBinding#REDIRECT} {@code SAMLResponse}.  Defaults to
		 * {@link SamlAuthFilter#DEFAULT_MAX_INFLATE_RATIO}.
		 *
		 * <p>
		 * This guard catches a highly-compressible payload even when the absolute
		 * {@link #maxInflatedBytes(long) byte cap} is generous.  A value {@code <= 0} disables the ratio guard.
		 *
		 * @param value The maximum output-to-input ratio.
		 * @return This object.
		 */
		public Builder maxInflateRatio(int value) {
			maxInflateRatio = value;
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
	private final long maxInflatedBytes;
	private final int maxInflateRatio;

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
		this.maxInflatedBytes = b.maxInflatedBytes;
		this.maxInflateRatio = b.maxInflateRatio;
	}

	@Override /* Overridden from AuthFilter */
	public Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException {
		if (!matchesPath(req))
			return oe();
		var raw = req.getParameter(SAML_RESPONSE_PARAM);
		if (isEmpty(raw))
			return oe();
		var xml = decodeSamlResponse(raw);
		var principal = runValidator(xml, deriveRecipient(req));
		return o(AuthResult.of(principal, extractRoles(principal)));
	}

	private boolean matchesPath(HttpServletRequest req) {
		var p = req.getPathInfo();
		var s = p != null ? p : req.getServletPath();
		return s != null && s.equals(consumerPath);
	}

	/**
	 * Derives the ACS recipient URL (scheme, host, port, and path) that this request was actually delivered
	 * to, so it can be bound to the assertion's bearer {@code <SubjectConfirmation>}.
	 *
	 * <p>
	 * Normally this is just {@link HttpServletRequest#getRequestURL()}.  Some servlet-container and test
	 * request implementations return <jk>null</jk> from that method, so as a fallback the URL is
	 * reconstructed from {@link HttpServletRequest#getScheme()}, {@link HttpServletRequest#getServerName()},
	 * {@link HttpServletRequest#getServerPort()} (omitted when it's the scheme's default port), and
	 * {@link HttpServletRequest#getRequestURI()}.  If the recipient still cannot be determined, the request
	 * is rejected via {@link AuthenticationException} &mdash; never with an {@link NullPointerException}.
	 *
	 * @param req The current request; {@link #matchesPath} has already confirmed its path matches
	 * 	{@code consumerPath}.
	 * @return The recipient URL, e.g. {@code https://sp.example.com/saml/acs}.
	 * @throws AuthenticationException If the recipient URL cannot be determined from the request.
	 */
	private String deriveRecipient(HttpServletRequest req) throws AuthenticationException {
		var url = req.getRequestURL();
		if (url != null)
			return url.toString();
		return reconstructRecipient(req);
	}

	/**
	 * Fallback for {@link #deriveRecipient(HttpServletRequest)} when {@link HttpServletRequest#getRequestURL()}
	 * returns <jk>null</jk>.
	 *
	 * @param req The current request.
	 * @return The reconstructed recipient URL.
	 * @throws AuthenticationException If {@code scheme}, {@code serverName}, or {@code requestURI} is
	 * 	<jk>null</jk> or blank, so no recipient can be determined.
	 */
	private String reconstructRecipient(HttpServletRequest req) throws AuthenticationException {
		var scheme = req.getScheme();
		var host = req.getServerName();
		var uri = req.getRequestURI();
		if (isBlank(scheme) || isBlank(host) || isBlank(uri))
			throw new AuthenticationException("Unable to derive SAML ACS recipient from request").wwwAuthenticate(challenge);
		var port = req.getServerPort();
		var isDefaultPort = port <= 0
			|| (port == 80 && "http".equalsIgnoreCase(scheme))
			|| (port == 443 && "https".equalsIgnoreCase(scheme));
		var sb = new StringBuilder(scheme).append("://").append(host);
		if (!isDefaultPort)
			sb.append(':').append(port);
		return sb.append(uri).toString();
	}

	private String decodeSamlResponse(String raw) throws AuthenticationException {
		try {
			byte[] decoded = Base64.getMimeDecoder().decode(raw);
			return switch (binding) {
				// Per SAML 2.0 Redirect binding: raw DEFLATE (no zlib wrapper), decompressed under a hard
				// output-size cap and ratio guard so a compressible payload cannot force an unbounded allocation.
				case REDIRECT -> new String(IoUtils.inflate(decoded, true, maxInflatedBytes, maxInflateRatio), StandardCharsets.UTF_8);
				// Per SAML 2.0 POST binding: the base64-decoded bytes are the raw XML document.
				case POST -> new String(decoded, StandardCharsets.UTF_8);
			};
		} catch (IllegalArgumentException e) {
			throw new AuthenticationException(e, "SAMLResponse parameter is not valid base64")
				.wwwAuthenticate(challenge);
		} catch (IOException e) {
			throw new AuthenticationException(e, "SAMLResponse parameter could not be inflated (REDIRECT binding)")
				.wwwAuthenticate(challenge);
		}
	}

	private Principal runValidator(String xml, String recipient) throws AuthenticationException {
		try {
			var p = validator.validate(xml, recipient);
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
		if (principal instanceof ClaimsPrincipal principal2) {
			var v = principal2.getClaims().get(rolesClaim);
			if (v instanceof List<?> v2) {
				var roles = new HashSet<String>();
				for (var item : (List<Object>) v2)
					if (item instanceof String item2)
						roles.add(item2);
				return roles;
			}
		}
		return Collections.emptySet();
	}
}
