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
package org.apache.juneau.http.header;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.security.*;
import java.util.*;
import java.util.function.*;

/**
 * Represents an HTTP <c>Content-Security-Policy</c> (or <c>Content-Security-Policy-Report-Only</c>) response header.
 *
 * <p>
 * A Content Security Policy lets a server declare which content sources a browser may load and execute for a page,
 * mitigating cross-site-scripting (XSS) and data-injection attacks.  This bean carries an already-rendered policy
 * value; use {@link Builder} to assemble the value from typed directives.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Build a static policy and emit it as a response header.</jc>
 * 	ContentSecurityPolicy <jv>csp</jv> = ContentSecurityPolicy.<jsm>create</jsm>()
 * 		.defaultSrc(ContentSecurityPolicy.Builder.<jsf>SELF</jsf>)
 * 		.objectSrc(ContentSecurityPolicy.Builder.<jsf>NONE</jsf>)
 * 		.toHeader();
 *
 * 	<jc>// =&gt; "Content-Security-Policy: default-src 'self'; object-src 'none'"</jc>
 * </p>
 *
 * <p>
 * Because this is an {@link HttpStringHeader}, an instance can be added directly to the REST server's
 * <c>defaultResponseHeaders</c> {@link HttpHeaderList} (e.g. via a <c><ja>@Bean</ja>(name=<js>"defaultResponseHeaders"</js>)</c>
 * factory method) and is emitted through the normal response-header path.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Builder}
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/TR/CSP3/'>Content Security Policy Level 3 (W3C WD)</a>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/TR/CSP2/'>Content Security Policy Level 2 (W3C Rec)</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // NAME / REPORT_ONLY_NAME mirror the external HTTP header literal exactly.
})
public class ContentSecurityPolicy extends HttpStringHeader {

	/** The enforcing header name: {@value}. */
	public static final String NAME = "Content-Security-Policy";

	/** The observe-only header name: {@value}. */
	public static final String REPORT_ONLY_NAME = "Content-Security-Policy-Report-Only";

	/**
	 * Constructor with an eager value (enforcing {@value #NAME} header).
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 */
	public ContentSecurityPolicy(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with a lazy value supplier (enforcing {@value #NAME} header).
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 */
	public ContentSecurityPolicy(Supplier<String> valueSupplier) {
		super(NAME, valueSupplier);
	}

	/**
	 * Constructor allowing the header name to be chosen (enforcing vs. report-only).
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. May be <jk>null</jk>.
	 */
	protected ContentSecurityPolicy(String name, String value) {
		super(name, value);
	}

	/**
	 * Constructor allowing the header name to be chosen, with a lazy value supplier.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 */
	protected ContentSecurityPolicy(String name, Supplier<String> valueSupplier) {
		super(name, valueSupplier);
	}

	/**
	 * Static factory method for an enforcing ({@value #NAME}) header with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ContentSecurityPolicy of(String value) {
		return new ContentSecurityPolicy(value);
	}

	/**
	 * Static factory method for an enforcing ({@value #NAME}) header with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ContentSecurityPolicy of(Supplier<String> valueSupplier) {
		return new ContentSecurityPolicy(valueSupplier);
	}

	/**
	 * Static factory method for an observe-only ({@value #REPORT_ONLY_NAME}) header with an eager value.
	 *
	 * <p>
	 * Browsers report violations against this header but do not block anything, letting operators tune a policy
	 * before enforcing it.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ContentSecurityPolicy ofReportOnly(String value) {
		return new ContentSecurityPolicy(REPORT_ONLY_NAME, value);
	}

	/**
	 * Static factory method for an observe-only ({@value #REPORT_ONLY_NAME}) header with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ContentSecurityPolicy ofReportOnly(Supplier<String> valueSupplier) {
		return new ContentSecurityPolicy(REPORT_ONLY_NAME, valueSupplier);
	}

	/**
	 * Creates a new empty {@link Builder}.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder create() {
		return Builder.create();
	}

	/**
	 * Creates a {@link Builder} pre-populated with the nonce-based <i>strict starter</i> preset.
	 *
	 * @return A new builder pre-populated with the strict-starter directives. Never <jk>null</jk>.
	 * @see Builder#strictStarter()
	 */
	public static Builder strictStarter() {
		return Builder.strictStarter();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Inner types
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for assembling a <c>Content-Security-Policy</c> header value.
	 *
	 * <p>
	 * Composes the directives defined by
	 * <a class='doclink' href='https://www.w3.org/TR/CSP3/'>CSP Level 3</a> into the {@code directive source source; ...}
	 * wire format expected by the {@code Content-Security-Policy} response header.  Typed setters cover the common
	 * directives; {@link #directive(String, String...)} is a generic escape hatch so new directives never require an API
	 * change.  Setting the same directive more than once <b>replaces</b> the previous source list.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String <jv>value</jv> = ContentSecurityPolicy.<jsm>create</jsm>()
	 * 		.defaultSrc(<jsf>SELF</jsf>)
	 * 		.imgSrc(<jsf>SELF</jsf>, <js>"data:"</js>)
	 * 		.objectSrc(<jsf>NONE</jsf>)
	 * 		.build();
	 * 	<jc>// =&gt; "default-src 'self'; img-src 'self' data:; object-src 'none'"</jc>
	 * </p>
	 *
	 * <h5 class='section'>Nonces:</h5>
	 * <p>
	 * Source helper {@link #nonce(String)} renders a <c>'nonce-&lt;token&gt;'</c> source expression.  For per-request
	 * nonces (where the actual token is not known until the response is rendered), use {@link #NONCE_PLACEHOLDER} as the
	 * token and have the REST layer rewrite it per-response via {@link #resolveNonce(String, String)}; the same nonce is
	 * then stamped onto the inline <c>&lt;script&gt;</c>/<c>&lt;style&gt;</c> tags the serializer emits.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link ContentSecurityPolicy}
	 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/TR/CSP3/'>Content Security Policy Level 3 (W3C WD)</a>
	 * </ul>
	 *
	 * @since 10.0.0
	 */
	@SuppressWarnings({
		"java:S115" // Source-keyword constants mirror external CSP literals exactly (e.g. 'self', 'none').
	})
	public static class Builder {

		//--------------------------------------------------------------------------------------------------------------
		// Source-expression keyword helpers
		//--------------------------------------------------------------------------------------------------------------

		/** The <c>'self'</c> source-expression keyword. */
		public static final String SELF = "'self'";

		/** The <c>'none'</c> source-expression keyword. */
		public static final String NONE = "'none'";

		/** The <c>'unsafe-inline'</c> source-expression keyword. */
		public static final String UNSAFE_INLINE = "'unsafe-inline'";

		/** The <c>'unsafe-eval'</c> source-expression keyword. */
		public static final String UNSAFE_EVAL = "'unsafe-eval'";

		/** The <c>'strict-dynamic'</c> source-expression keyword. */
		public static final String STRICT_DYNAMIC = "'strict-dynamic'";

		/**
		 * The placeholder token used in nonce sources when the actual per-request nonce is not yet known.
		 *
		 * <p>
		 * {@link #strictStarter()} emits {@code 'nonce-{nonce}'}; the REST layer substitutes the real nonce per-response
		 * via {@link #resolveNonce(String, String)}.
		 */
		public static final String NONCE_PLACEHOLDER = "{nonce}";

		/**
		 * Renders a <c>'nonce-&lt;token&gt;'</c> source expression.
		 *
		 * @param token The base64 nonce value (or {@link #NONCE_PLACEHOLDER} for deferred substitution). Must not be <jk>null</jk> or blank.
		 * @return The source expression. Never <jk>null</jk>.
		 * @throws IllegalArgumentException If {@code token} is <jk>null</jk> or blank.
		 */
		public static String nonce(String token) {
			assertArgNotNull("token", token);
			if (token.trim().isEmpty())
				throw iaex("nonce token must not be blank");
			return "'nonce-" + token + "'";
		}

		/**
		 * Renders a scheme source expression, ensuring a single trailing colon (e.g. <js>"https"</js> =&gt; <c>https:</c>).
		 *
		 * @param scheme The scheme (with or without a trailing colon). Must not be <jk>null</jk> or blank.
		 * @return The scheme source expression. Never <jk>null</jk>.
		 * @throws IllegalArgumentException If {@code scheme} is <jk>null</jk> or blank.
		 */
		public static String scheme(String scheme) {
			assertArgNotNull("scheme", scheme);
			var s = scheme.trim();
			if (s.isEmpty())
				throw iaex("scheme must not be blank");
			return s.endsWith(":") ? s : s + ":";
		}

		/**
		 * Renders a <c>'sha256-&lt;hash&gt;'</c> (or sha384/sha512) source expression.
		 *
		 * @param algorithm One of <js>"sha256"</js>, <js>"sha384"</js>, or <js>"sha512"</js>. Must not be <jk>null</jk> or blank.
		 * @param base64Hash The base64-encoded hash of the inline block. Must not be <jk>null</jk> or blank.
		 * @return The hash source expression. Never <jk>null</jk>.
		 * @throws IllegalArgumentException If either argument is <jk>null</jk> or blank.
		 */
		public static String hash(String algorithm, String base64Hash) {
			assertArgNotNull("algorithm", algorithm);
			assertArgNotNull("base64Hash", base64Hash);
			if (algorithm.trim().isEmpty())
				throw iaex("hash algorithm must not be blank");
			if (base64Hash.trim().isEmpty())
				throw iaex("hash value must not be blank");
			return "'" + algorithm.trim() + "-" + base64Hash.trim() + "'";
		}

		private static final SecureRandom SECURE_RANDOM = new SecureRandom();

		/**
		 * Mints a fresh, cryptographically-random CSP nonce.
		 *
		 * <p>
		 * Generates 128 bits (16 bytes) from {@link SecureRandom} encoded as an unpadded base64url string.  Each call
		 * returns a new value; a nonce must never be reused across responses.
		 *
		 * @return A new nonce token. Never <jk>null</jk>.
		 */
		public static String generateNonce() {
			var bytes = new byte[16];
			SECURE_RANDOM.nextBytes(bytes);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		}

		/**
		 * Replaces every occurrence of {@link #NONCE_PLACEHOLDER} in a rendered policy value with the supplied nonce.
		 *
		 * @param policyValue The rendered policy value (may contain {@link #NONCE_PLACEHOLDER}). May be <jk>null</jk>.
		 * @param nonce The per-request nonce token. Must not be <jk>null</jk>.
		 * @return The policy value with placeholders substituted, or <jk>null</jk> if {@code policyValue} was <jk>null</jk>.
		 * @throws IllegalArgumentException If {@code nonce} is <jk>null</jk>.
		 */
		public static String resolveNonce(String policyValue, String nonce) {
			assertArgNotNull("nonce", nonce);
			if (policyValue == null)
				return null;
			return policyValue.replace(NONCE_PLACEHOLDER, nonce);
		}

		//--------------------------------------------------------------------------------------------------------------
		// Instance
		//--------------------------------------------------------------------------------------------------------------

		private final Map<String,List<String>> directives = new LinkedHashMap<>();
		private boolean reportOnly;

		/**
		 * Creates a new empty builder.
		 *
		 * @return A new builder. Never <jk>null</jk>.
		 */
		public static Builder create() {
			return new Builder();
		}

		/**
		 * Creates a builder pre-populated with the nonce-based <i>strict starter</i> preset.
		 *
		 * <p>
		 * The preset is:
		 * <p class='bcode'>
		 * 	default-src 'self';
		 * 	script-src 'self' 'nonce-{nonce}';
		 * 	style-src 'self' 'nonce-{nonce}';
		 * 	img-src 'self' data:;
		 * 	connect-src 'self';
		 * 	object-src 'none';
		 * 	base-uri 'self';
		 * 	frame-ancestors 'self'
		 * </p>
		 *
		 * <p>
		 * The {@code {nonce}} tokens are {@link #NONCE_PLACEHOLDER}s; for the policy to function on Juneau-served HTML
		 * (which emits inline {@code <script>}/{@code <style>}) the REST layer must mint a per-request nonce, substitute
		 * it via {@link #resolveNonce(String, String)}, and stamp the same value onto the emitted inline tags.
		 *
		 * @return A new pre-populated builder. Never <jk>null</jk>.
		 */
		public static Builder strictStarter() {
			return create()
				.defaultSrc(SELF)
				.scriptSrc(SELF, nonce(NONCE_PLACEHOLDER))
				.styleSrc(SELF, nonce(NONCE_PLACEHOLDER))
				.imgSrc(SELF, "data:")
				.connectSrc(SELF)
				.objectSrc(NONE)
				.baseUri(SELF)
				.frameAncestors(SELF);
		}

		/**
		 * Sets the <c>default-src</c> directive.
		 *
		 * @param sources The source expressions. May be empty.
		 * @return This object.
		 */
		public Builder defaultSrc(String...sources) {
			return directive("default-src", sources);
		}

		/**
		 * Sets the <c>script-src</c> directive.
		 *
		 * @param sources The source expressions. May be empty.
		 * @return This object.
		 */
		public Builder scriptSrc(String...sources) {
			return directive("script-src", sources);
		}

		/**
		 * Sets the <c>style-src</c> directive.
		 *
		 * @param sources The source expressions. May be empty.
		 * @return This object.
		 */
		public Builder styleSrc(String...sources) {
			return directive("style-src", sources);
		}

		/**
		 * Sets the <c>img-src</c> directive.
		 *
		 * @param sources The source expressions. May be empty.
		 * @return This object.
		 */
		public Builder imgSrc(String...sources) {
			return directive("img-src", sources);
		}

		/**
		 * Sets the <c>connect-src</c> directive.
		 *
		 * @param sources The source expressions. May be empty.
		 * @return This object.
		 */
		public Builder connectSrc(String...sources) {
			return directive("connect-src", sources);
		}

		/**
		 * Sets the <c>font-src</c> directive.
		 *
		 * @param sources The source expressions. May be empty.
		 * @return This object.
		 */
		public Builder fontSrc(String...sources) {
			return directive("font-src", sources);
		}

		/**
		 * Sets the <c>object-src</c> directive.
		 *
		 * @param sources The source expressions. May be empty.
		 * @return This object.
		 */
		public Builder objectSrc(String...sources) {
			return directive("object-src", sources);
		}

		/**
		 * Sets the <c>base-uri</c> directive.
		 *
		 * @param sources The source expressions. May be empty.
		 * @return This object.
		 */
		public Builder baseUri(String...sources) {
			return directive("base-uri", sources);
		}

		/**
		 * Sets the <c>frame-ancestors</c> directive.
		 *
		 * @param sources The source expressions. May be empty.
		 * @return This object.
		 */
		public Builder frameAncestors(String...sources) {
			return directive("frame-ancestors", sources);
		}

		/**
		 * Sets the <c>report-uri</c> directive (deprecated in CSP3 but still widely supported).
		 *
		 * @param uris The collector URIs. May be empty.
		 * @return This object.
		 */
		public Builder reportUri(String...uris) {
			return directive("report-uri", uris);
		}

		/**
		 * Sets the <c>report-to</c> directive (the CSP3 reporting-group mechanism).
		 *
		 * @param groups The reporting-group names. May be empty.
		 * @return This object.
		 */
		public Builder reportTo(String...groups) {
			return directive("report-to", groups);
		}

		/**
		 * Generic escape hatch for setting any directive by name.
		 *
		 * <p>
		 * Replaces any previously-registered source list for the same directive while preserving its original position.
		 *
		 * @param name The directive name (e.g. <js>"worker-src"</js>). Must not be <jk>null</jk> or blank.
		 * @param sources The source expressions. May be empty or <jk>null</jk> (renders the bare directive name).
		 * @return This object.
		 * @throws IllegalArgumentException If {@code name} is <jk>null</jk> or blank.
		 */
		public Builder directive(String name, String...sources) {
			assertArgNotNull("name", name);
			var n = name.trim();
			if (n.isEmpty())
				throw iaex("directive name must not be blank");
			directives.put(n, sources == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(sources)));
			return this;
		}

		/**
		 * Flips the rendered header to the observe-only {@link ContentSecurityPolicy#REPORT_ONLY_NAME} variant.
		 *
		 * @return This object.
		 */
		public Builder reportOnly() {
			return reportOnly(true);
		}

		/**
		 * Sets whether the rendered header uses the observe-only {@link ContentSecurityPolicy#REPORT_ONLY_NAME} variant.
		 *
		 * @param value <jk>true</jk> to emit {@code Content-Security-Policy-Report-Only} instead of {@code Content-Security-Policy}.
		 * @return This object.
		 */
		public Builder reportOnly(boolean value) {
			reportOnly = value;
			return this;
		}

		/**
		 * Returns <jk>true</jk> if this builder is configured to emit the report-only header variant.
		 *
		 * @return A boolean flag.
		 */
		public boolean isReportOnly() {
			return reportOnly;
		}

		/**
		 * Builds the rendered policy value (the header value only, not including the header name).
		 *
		 * <p>
		 * Returns an empty string when no directives have been registered.
		 *
		 * @return The policy value. Never <jk>null</jk>.
		 */
		public String build() {
			var parts = new ArrayList<String>(directives.size());
			directives.forEach((name, sources) -> {
				if (sources.isEmpty())
					parts.add(name);
				else
					parts.add(name + " " + String.join(" ", sources));
			});
			return String.join("; ", parts);
		}

		/**
		 * Builds a {@link ContentSecurityPolicy} header bean from this builder.
		 *
		 * <p>
		 * The header name is {@link ContentSecurityPolicy#REPORT_ONLY_NAME} when {@link #isReportOnly()} is <jk>true</jk>,
		 * otherwise {@link ContentSecurityPolicy#NAME}.
		 *
		 * @return A new header bean carrying {@link #build()}. Never <jk>null</jk>.
		 */
		public ContentSecurityPolicy toHeader() {
			return reportOnly ? ContentSecurityPolicy.ofReportOnly(build()) : ContentSecurityPolicy.of(build());
		}

		@Override
		public String toString() {
			return build();
		}
	}
}
