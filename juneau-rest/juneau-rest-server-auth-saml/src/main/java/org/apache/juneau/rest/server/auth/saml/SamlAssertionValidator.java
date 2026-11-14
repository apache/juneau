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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.logging.*;

import javax.xml.parsers.*;
import org.apache.juneau.commons.concurrent.*;
import org.apache.juneau.rest.server.auth.*;
import org.opensaml.core.criterion.*;
import org.opensaml.core.xml.io.*;
import org.opensaml.saml.common.xml.*;
import org.opensaml.saml.criterion.*;
import org.opensaml.saml.metadata.resolver.*;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.saml.security.impl.*;
import org.opensaml.security.credential.*;
import org.opensaml.security.x509.*;
import org.opensaml.xmlsec.encryption.support.*;
import org.opensaml.xmlsec.keyinfo.*;
import org.opensaml.xmlsec.keyinfo.impl.*;
import org.opensaml.xmlsec.signature.*;
import org.opensaml.xmlsec.signature.support.*;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.w3c.dom.*;

import net.shibboleth.shared.resolver.*;

/**
 * Validates a SAML 2.0 {@code <samlp:Response>} document and produces a {@link ClaimsPrincipal} marked
 * with {@code issuerType=SAML}.
 *
 * <p>
 * Wraps OpenSAML 5.x's parser, unmarshaller, signature validator, and (optionally) assertion decrypter.
 * The validator enforces:
 *
 * <ul>
 * 	<li><b>Mandatory assertion signature</b> &mdash; every assertion must carry its own enveloped signature.
 * 		A response that signs only the {@code <Response>} envelope (and not the assertion) is rejected as
 * 		unsigned; response-envelope-level signatures are not supported.
 * 	<li><b>Strict signature-algorithm allowlist</b> &mdash; only {@code rsa-sha256} and
 * 		{@code ecdsa-sha256} are accepted out of the box.  SHA-1 is permanently rejected.  The reference
 * 		{@code DigestMethod} is separately allowlisted (SHA-256/384/512), so a SHA-1 digest is rejected even
 * 		under a SHA-256 signature.
 * 	<li><b>Response status</b> &mdash; the {@code <samlp:Status>} must report the {@code Success} status code.
 * 	<li><b>Clock skew</b> &mdash; defaults to 60 seconds tolerance on {@code NotBefore} /
 * 		{@code NotOnOrAfter}; capped at 300 seconds.
 * 	<li><b>Audience restriction</b> &mdash; {@code <AudienceRestriction>} must list the configured SP entity ID.
 * 	<li><b>Bearer subject confirmation</b> &mdash; always enforced.  The ACS URL configured with
 * 		{@link Builder#recipient(String)} is required, and the assertion must carry a {@code bearer}
 * 		{@code <SubjectConfirmation>} whose {@code <SubjectConfirmationData>} names exactly that {@code Recipient},
 * 		is within its {@code NotBefore}/{@code NotOnOrAfter} window (the {@code NotOnOrAfter} is mandatory), and
 * 		satisfies the configured {@code Address} expectation.  The recipient is never taken from the request.
 * 		When the response carries a {@code Destination}, it must equal the configured ACS URL.
 * 	<li><b>Request binding</b> &mdash; a confirmation carrying {@code InResponseTo} must reference an
 * 		authentication request ID previously registered with {@link #rememberAuthnRequest(String)}; each ID is
 * 		accepted once.  A confirmation with no {@code InResponseTo} (an IdP-initiated response) is rejected unless
 * 		{@link Builder#allowIdpInitiated()} was set.
 * 	<li><b>One-time use</b> &mdash; each assertion ID is recorded in a {@link ReplayCache} and a second
 * 		presentation of the same ID is rejected.  The check is fail-closed: if the cache cannot answer, the
 * 		assertion is rejected.  An assertion whose validity window is unbounded &mdash; carrying neither a
 * 		{@code Conditions/NotOnOrAfter} nor a bearer {@code SubjectConfirmationData/NotOnOrAfter} &mdash; is
 * 		rejected outright, and the cache record is retained until the resolved {@code NotOnOrAfter} (plus clock
 * 		skew) rather than a short default window that would later expire and re-open replay.
 * 	<li><b>Encrypted assertions</b> &mdash; if the response contains
 * 		{@code <EncryptedAssertion>}, a {@code decryptionCredential} must be configured.  Only RSA-OAEP key
 * 		transport and AES-GCM content encryption are accepted, and every decryption problem is reported with the
 * 		same generic error.
 * </ul>
 *
 * <h2 class='topic'>Marker claim</h2>
 * <p>
 * The returned {@link ClaimsPrincipal} is annotated with {@code issuerType=SAML} so downstream code
 * can distinguish SAML-derived principals from JWT-derived principals without resorting to a subclass:
 *
 * <p class='bjava'>
 * 	<jk>if</jk> (<js>"SAML"</js>.equals(<jv>principal</jv>.getClaim(<js>"issuerType"</js>, String.<jk>class</jk>).orElse(<jk>null</jk>))) {
 * 		<jc>// SAML-issued principal</jc>
 * 	}
 * </p>
 *
 * <h2 class='topic'>Builder usage</h2>
 *
 * <p>
 * The ACS URL is mandatory.  Register each outgoing authentication request ID with
 * {@link #rememberAuthnRequest(String)} so the response can be bound to it.
 *
 * <p class='bjava'>
 * 	<jk>var</jk> validator = SamlAssertionValidator.<jsm>create</jsm>()
 * 		.metadataResolver(SamlMetadataResolvers.<jsm>url</jsm>(<js>"https://idp.example.com/metadata"</js>))
 * 		.spEntityId(<js>"https://sp.example.com"</js>)
 * 		.expectedIssuer(<js>"https://idp.example.com"</js>)
 * 		.recipient(<js>"https://sp.example.com/saml/acs"</js>)
 * 		.build();
 * </p>
 *
 * <h2 class='section'>See Also:</h2>
 * <ul>
 * 	<li class='jc'>{@link SamlAuthFilter}
 * 	<li class='jc'>{@link SamlMetadataResolvers}
 * 	<li class='link'><a class="doclink" href="https://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf">SAML 2.0 Core (OASIS)</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192", // Duplicate string literals are SAML protocol claim names and JWT-style field keys; intentional
	"java:S6539" // Monolithic class: validator is a single end-to-end pipeline.
})
public class SamlAssertionValidator {

	private static final Logger LOG = Logger.getLogger(SamlAssertionValidator.class.getName());

	/** Marker claim name on the returned {@link ClaimsPrincipal}. */
	public static final String ISSUER_TYPE_CLAIM = "issuerType";

	/** Marker claim value identifying the principal as SAML-issued. */
	public static final String ISSUER_TYPE_SAML = "SAML";

	/** Maximum clock-skew tolerance the builder will accept (5 minutes). */
	private static final Duration MAX_CLOCK_SKEW = Duration.ofSeconds(300);

	/** XML-DSig namespace URI, used when scanning the signature DOM for reference digest methods. */
	private static final String XMLSIG_NS = "http://www.w3.org/2000/09/xmldsig#";

	/** Default allowlist of XML signature algorithms. */
	private static final Set<String> DEFAULT_ALGORITHMS = Set.of(
		SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256,
		SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA256
	);

	/** Decryption allowlist: RSA-OAEP key transport and AES-GCM content encryption only. */
	private static final Set<String> DECRYPTION_ALGORITHMS = Set.of(
		EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP,
		EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP11,
		EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM,
		EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES192_GCM,
		EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM,
		// RSA-OAEP parameters: the OAEP digest (SHA-1 is the OAEP default and is not collision-sensitive there) and MGF1.
		SignatureConstants.ALGO_ID_DIGEST_SHA1,
		SignatureConstants.ALGO_ID_DIGEST_SHA256,
		SignatureConstants.ALGO_ID_DIGEST_SHA384,
		SignatureConstants.ALGO_ID_DIGEST_SHA512,
		EncryptionConstants.ALGO_ID_MGF1_SHA1,
		EncryptionConstants.ALGO_ID_MGF1_SHA256,
		EncryptionConstants.ALGO_ID_MGF1_SHA384,
		EncryptionConstants.ALGO_ID_MGF1_SHA512
	);

	/** Default allowlist of XML reference digest algorithms.  SHA-1 is deliberately excluded. */
	private static final Set<String> DEFAULT_DIGEST_ALGORITHMS = Set.of(
		SignatureConstants.ALGO_ID_DIGEST_SHA256,
		SignatureConstants.ALGO_ID_DIGEST_SHA384,
		SignatureConstants.ALGO_ID_DIGEST_SHA512
	);

	/**
	 * Static creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder for {@link SamlAssertionValidator}.
	 */
	public static class Builder {
		private MetadataResolver metadataResolver;
		private Credential signingCredential;
		private Credential decryptionCredential;
		private String spEntityId;
		private String expectedIssuer;
		private String recipient;
		private String expectedInResponseTo;
		private String expectedAddress;
		private Set<String> algorithms = new LinkedHashSet<>(DEFAULT_ALGORITHMS);
		private Set<String> digestAlgorithms = new LinkedHashSet<>(DEFAULT_DIGEST_ALGORITHMS);
		private Duration clockSkew = Duration.ofSeconds(60);
		private Clock clock = Clock.systemUTC();
		private ReplayCache replayCache;
		private AuthnRequestStore authnRequestStore;
		private boolean allowIdpInitiated;
		private Duration authnRequestTtl = Duration.ofMinutes(10);

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the {@link MetadataResolver} used to resolve the IdP's signing credential at validation time.
		 *
		 * <p>
		 * Mutually exclusive with {@link #signingCredential(Credential)}.
		 *
		 * <p>
		 * The metadata supplies the trust anchor, so prefer a resolver whose metadata signature is verified against
		 * a pinned certificate, e.g. {@link SamlMetadataResolvers#url(String, java.security.cert.X509Certificate)}.
		 * Unsigned metadata is only as trustworthy as the transport it was fetched over.
		 *
		 * @param value The metadata resolver.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder metadataResolver(MetadataResolver value) {
			metadataResolver = reqnn("value", value);
			return this;
		}

		/**
		 * Sets a static IdP signing credential.  Useful when metadata is not available.
		 *
		 * <p>
		 * Mutually exclusive with {@link #metadataResolver(MetadataResolver)}.
		 *
		 * @param value The IdP signing credential.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder signingCredential(Credential value) {
			signingCredential = reqnn("value", value);
			return this;
		}

		/**
		 * Sets the credential used to decrypt {@code <EncryptedAssertion>} elements.
		 *
		 * <p>
		 * If the response contains encrypted assertions and no decryption credential is configured,
		 * validation fails with {@link AuthenticationException}.
		 *
		 * @param value The SP decryption credential.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder decryptionCredential(Credential value) {
			decryptionCredential = reqnn("value", value);
			return this;
		}

		/**
		 * Sets the Service Provider entity ID used to validate {@code <AudienceRestriction>}.
		 *
		 * @param value The SP entity ID.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder spEntityId(String value) {
			spEntityId = reqnb("value", value);
			return this;
		}

		/**
		 * Sets the expected IdP issuer (the {@code <Issuer>} element).  Required.
		 *
		 * @param value The expected IdP entity ID.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder expectedIssuer(String value) {
			expectedIssuer = reqnb("value", value);
			return this;
		}

		/**
		 * Replaces the default signature-algorithm allowlist with the supplied URIs.
		 *
		 * <p>
		 * SHA-1 algorithms are silently filtered out (permanent rejection).
		 *
		 * @param values The XML-DSig algorithm URIs to allow.  Must contain at least one non-SHA-1 entry.
		 * @return This object.
		 */
		public Builder algorithms(String... values) {
			reqnn("values", values);
			if (values.length == 0)
				throw iaex("algorithms allowlist must be non-empty");
			var next = new LinkedHashSet<String>();
			for (var a : values) {
				reqnn("algorithm", a);
				if (a.toLowerCase(Locale.ROOT).contains("sha1"))
					throw iaex("SHA-1 algorithms are permanently rejected: %s", a);
				next.add(a);
			}
			algorithms = next;
			return this;
		}

		/**
		 * Replaces the default reference-digest allowlist with the supplied URIs.
		 *
		 * <p>
		 * The reference {@code DigestMethod} of the enveloped signature must appear in this set.  SHA-1
		 * algorithms are silently filtered out (permanent rejection), mirroring {@link #algorithms(String...)}.
		 *
		 * @param values The XML-DSig digest algorithm URIs to allow.  Must contain at least one non-SHA-1 entry.
		 * @return This object.
		 */
		public Builder digestAlgorithms(String... values) {
			reqnn("values", values);
			if (values.length == 0)
				throw iaex("digest allowlist must be non-empty");
			var next = new LinkedHashSet<String>();
			for (var a : values) {
				reqnn("digestAlgorithm", a);
				if (a.toLowerCase(Locale.ROOT).contains("sha1"))
					throw iaex("SHA-1 digest algorithms are permanently rejected: %s", a);
				next.add(a);
			}
			digestAlgorithms = next;
			return this;
		}

		/**
		 * Sets the expected {@code Recipient} (this SP's Assertion Consumer Service URL).  Required.
		 *
		 * <p>
		 * Every validated assertion must carry a {@code bearer} {@code <SubjectConfirmation>} whose
		 * {@code <SubjectConfirmationData>} names this exact {@code Recipient}, is within its
		 * {@code NotBefore}/{@code NotOnOrAfter} window, and satisfies the {@link #subjectAddress(String)}
		 * expectation.  A response carrying a {@code Destination} must name this URL as well.
		 *
		 * @param value The expected ACS URL.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder recipient(String value) {
			recipient = reqnb("value", value);
			return this;
		}

		/**
		 * Pins the request ID a bearer {@code <SubjectConfirmationData>} must echo in its {@code InResponseTo}
		 * attribute.
		 *
		 * <p>
		 * When set, the confirmation's {@code InResponseTo} must equal this value.  When left unset (the default),
		 * a present {@code InResponseTo} must reference an ID registered with
		 * {@link SamlAssertionValidator#rememberAuthnRequest(String)}.
		 *
		 * @param value The expected request ID.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder expectedInResponseTo(String value) {
			expectedInResponseTo = reqnb("value", value);
			return this;
		}

		/**
		 * Sets the client address a bearer {@code <SubjectConfirmationData>} must name in its {@code Address}
		 * attribute.
		 *
		 * <p>
		 * When set, the confirmation's
		 * {@code Address} must equal this value; when unset (the default) the {@code Address} attribute is not
		 * checked.
		 *
		 * @param value The expected client address.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder subjectAddress(String value) {
			expectedAddress = reqnb("value", value);
			return this;
		}

		/**
		 * Overrides the {@link ReplayCache} used to enforce one-time use of each assertion ID.
		 *
		 * <p>
		 * Defaults to a per-process {@link InMemoryReplayCache}, which enforces single-use within one JVM; a
		 * warning is logged when the default is used.
		 * Multi-node deployments should inject a {@link ReplayCache} backed by a store shared across every node
		 * so an assertion consumed against one node cannot be replayed against another.  The check is always
		 * fail-closed: a cache that cannot answer causes the assertion to be rejected.
		 *
		 * @param value The replay cache.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder replayCache(ReplayCache value) {
			replayCache = reqnn("value", value);
			return this;
		}

		/**
		 * Allows IdP-initiated responses, i.e. a bearer confirmation that carries no {@code InResponseTo}.
		 *
		 * <p>
		 * Off by default.  An unsolicited response cannot be bound to a request this SP made, so enable it only
		 * when the IdP starts the login flow.
		 *
		 * @return This object.
		 */
		public Builder allowIdpInitiated() {
			allowIdpInitiated = true;
			return this;
		}

		/**
		 * Overrides the {@link AuthnRequestStore} that tracks outstanding authentication request IDs.
		 *
		 * <p>
		 * Defaults to a per-process {@link InMemoryAuthnRequestStore}, which holds up to
		 * {@link InMemoryAuthnRequestStore#DEFAULT_MAX_ENTRIES} IDs and is not shared between nodes; a warning is
		 * logged when the default is used.  Multi-node deployments should inject a store shared across every node.
		 *
		 * @param value The store.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder authnRequestStore(AuthnRequestStore value) {
			authnRequestStore = reqnn("value", value);
			return this;
		}

		/**
		 * Sets how long a request ID registered with {@link SamlAssertionValidator#rememberAuthnRequest(String)}
		 * stays valid.  Defaults to 10 minutes.
		 *
		 * @param value The lifetime.  Must be positive.
		 * @return This object.
		 */
		public Builder authnRequestTtl(Duration value) {
			reqnn("value", value);
			if (value.isNegative() || value.isZero())
				throw iaex("authnRequestTtl must be positive");
			authnRequestTtl = value;
			return this;
		}

		/**
		 * Sets the clock-skew tolerance for {@code NotBefore} / {@code NotOnOrAfter} validation.
		 *
		 * @param value The tolerance.  Must be non-negative and not exceed 5 minutes.
		 * @return This object.
		 */
		public Builder clockSkew(Duration value) {
			reqnn("value", value);
			if (value.isNegative())
				throw iaex("clockSkew must be non-negative");
			if (value.compareTo(MAX_CLOCK_SKEW) > 0)
				throw iaex("clockSkew must not exceed 5 minutes (was %s)", value);
			clockSkew = value;
			return this;
		}

		/**
		 * Overrides the {@link Clock} used for timestamp validation.  Useful in tests.
		 *
		 * @param value The clock.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clock(Clock value) {
			clock = reqnn("value", value);
			return this;
		}

		/**
		 * Builds the validator.
		 *
		 * @return A new {@link SamlAssertionValidator}.
		 */
		public SamlAssertionValidator build() {
			if (spEntityId == null)
				throw new IllegalStateException("SamlAssertionValidator requires spEntityId(...)");
			if (expectedIssuer == null)
				throw new IllegalStateException("SamlAssertionValidator requires expectedIssuer(...)");
			if (metadataResolver == null && signingCredential == null)
				throw new IllegalStateException("SamlAssertionValidator requires either metadataResolver(...) or signingCredential(...)");
			if (metadataResolver != null && signingCredential != null)
				throw new IllegalStateException("SamlAssertionValidator: metadataResolver(...) and signingCredential(...) are mutually exclusive");
			if (recipient == null)
				throw new IllegalStateException("SamlAssertionValidator requires recipient(...) (the SP assertion consumer service URL)");
			if (replayCache == null) {
				LOG.warning("SamlAssertionValidator is using the default in-memory replay cache, which is not shared between nodes; configure replayCache(...) for multi-node deployments.");
				replayCache = new InMemoryReplayCache();
			}
			if (authnRequestStore == null) {
				LOG.warning("SamlAssertionValidator is using the default in-memory authentication request store, which is not shared between nodes; configure authnRequestStore(...) for multi-node deployments.");
				authnRequestStore = new InMemoryAuthnRequestStore();
			}
			return new SamlAssertionValidator(this);
		}
	}

	private final MetadataResolver metadataResolver;
	private final Credential signingCredential;
	private final Credential decryptionCredential;
	private final String spEntityId;
	private final String expectedIssuer;
	private final String recipient;
	private final String expectedInResponseTo;
	private final String expectedAddress;
	private final Set<String> algorithms;
	private final Set<String> digestAlgorithms;
	private final Duration clockSkew;
	private final Clock clock;
	private final ReplayCache replayCache;
	private final boolean allowIdpInitiated;
	private final Duration authnRequestTtl;
	private final AuthnRequestStore authnRequestStore;

	/** Attribute names that cannot replace the validator-set claims. */
	private static final Set<String> RESERVED_CLAIMS = Set.of(ISSUER_TYPE_CLAIM, "iss", "aud");

	/**
	 * Constructor.
	 *
	 * @param b The builder to read configuration from.
	 */
	protected SamlAssertionValidator(Builder b) {
		OpenSamlBootstrap.ensureInitialized();
		this.metadataResolver = b.metadataResolver;
		this.signingCredential = b.signingCredential;
		this.decryptionCredential = b.decryptionCredential;
		this.spEntityId = b.spEntityId;
		this.expectedIssuer = b.expectedIssuer;
		this.recipient = b.recipient;
		this.expectedInResponseTo = b.expectedInResponseTo;
		this.expectedAddress = b.expectedAddress;
		this.algorithms = u(cp(b.algorithms));
		this.digestAlgorithms = u(cp(b.digestAlgorithms));
		this.clockSkew = b.clockSkew;
		this.clock = b.clock;
		this.replayCache = b.replayCache;
		this.authnRequestStore = b.authnRequestStore;
		this.allowIdpInitiated = b.allowIdpInitiated;
		this.authnRequestTtl = b.authnRequestTtl;
	}

	/**
	 * Returns the SP entity ID this validator enforces in {@code <AudienceRestriction>}.
	 *
	 * @return The configured SP entity ID.
	 */
	public String getSpEntityId() {
		return spEntityId;
	}

	/**
	 * Returns the expected IdP {@code <Issuer>} value.
	 *
	 * @return The expected issuer.
	 */
	public String getExpectedIssuer() {
		return expectedIssuer;
	}

	/**
	 * Returns the configured signature algorithm allowlist.
	 *
	 * @return The unmodifiable allowlist.
	 */
	public Set<String> getAlgorithms() {
		return algorithms;
	}

	/**
	 * Returns the configured clock-skew tolerance.
	 *
	 * @return The clock skew.
	 */
	public Duration getClockSkew() {
		return clockSkew;
	}

	/**
	 * Registers the ID of an authentication request this SP sent, so a response answering it can be accepted.
	 *
	 * <p>
	 * Each registered ID is accepted once and expires after the configured request lifetime.  The registry is the
	 * {@link AuthnRequestStore} supplied to {@link Builder#authnRequestStore(AuthnRequestStore)} (in-memory by default).
	 *
	 * @param id The {@code ID} attribute of the outgoing {@code <AuthnRequest>}.  Must not be <jk>null</jk> or blank.
	 * @throws IllegalStateException If the store cannot accept more outstanding requests.
	 */
	public void rememberAuthnRequest(String id) {
		reqnb("id", id);
		var now = clock.millis();
		authnRequestStore.remember(id, now + authnRequestTtl.toMillis(), now);
	}

	private boolean consumeAuthnRequest(String id) {
		return authnRequestStore.consume(id, clock.millis());
	}

	/**
	 * Validates the supplied SAML 2.0 {@code <samlp:Response>} XML and returns the resolved principal.
	 *
	 * @param xml The full XML payload (already base64-decoded; already URL-decoded and inflated for the
	 * 	REDIRECT binding).  Must be a {@code <samlp:Response>} envelope.
	 * @return A {@link ClaimsPrincipal} carrying the IdP-supplied claims plus the {@code issuerType=SAML}
	 * 	marker.
	 * @throws AuthenticationException If the response cannot be parsed or validation fails.
	 */
	public Principal validate(String xml) throws AuthenticationException {
		reqnb("xml", xml);
		var response = parseResponse(xml);
		validateStatus(response);
		validateDestination(response);
		var assertion = extractAndDecryptAssertion(response);
		verifySignature(assertion);
		var claims = buildClaims(response, assertion);
		validateSubjectConfirmation(assertion);
		recordSingleUse(assertion);
		var subject = nameId(assertion);
		return new ClaimsPrincipal(subject, claims);
	}

	private void validateDestination(Response response) throws AuthenticationException {
		var destination = response.getDestination();
		if (destination != null && neq(recipient, destination))
			throw new AuthenticationException("SAML response <Destination> does not match the expected ACS URL")
				.wwwAuthenticate("SAML error=\"invalid_response\"");
	}

	private void validateStatus(Response response) throws AuthenticationException {
		var status = response.getStatus();
		var code = status == null ? null : status.getStatusCode();
		var value = code == null ? null : code.getValue();
		if (! StatusCode.SUCCESS.equals(value))
			throw new AuthenticationException("SAML response status is not Success: " + value)
				.wwwAuthenticate("SAML error=\"invalid_response\"");
	}

	private Response parseResponse(String xml) throws AuthenticationException {
		try {
			var doc = SamlXml.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
			Element root = doc.getDocumentElement();
			var registry = org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
			var unmarshaller = registry.getUnmarshaller(root);
			if (unmarshaller == null)
				throw new AuthenticationException("Unknown SAML element: " + root.getNodeName())
					.wwwAuthenticate("SAML error=\"invalid_response\"");
			var obj = unmarshaller.unmarshall(root);
			if (!(obj instanceof Response obj2))
				throw new AuthenticationException("SAML envelope is not a <Response>: " + cns(obj))
					.wwwAuthenticate("SAML error=\"invalid_response\"");
			return obj2;
		} catch (UnmarshallingException | ParserConfigurationException | IOException | org.xml.sax.SAXException e) {
			throw new AuthenticationException(e, "SAML response could not be parsed")
				.wwwAuthenticate("SAML error=\"invalid_response\"");
		}
	}

	private Assertion extractAndDecryptAssertion(Response response) throws AuthenticationException {
		var assertions = response.getAssertions();
		var encrypted = response.getEncryptedAssertions();
		if (assertions.isEmpty() && encrypted.isEmpty())
			throw new AuthenticationException("SAML response contains no <Assertion>")
				.wwwAuthenticate("SAML error=\"invalid_response\"");
		if (!encrypted.isEmpty()) {
			if (decryptionCredential == null) {
				LOG.fine("SAML response contains <EncryptedAssertion> but no decryption credential is configured");
				throw decryptionFailed();
			}
			var resolver = (KeyInfoCredentialResolver) new StaticKeyInfoCredentialResolver(decryptionCredential);
			EncryptedKeyResolver keyResolver = new ChainingEncryptedKeyResolver(List.of(
				new InlineEncryptedKeyResolver(),
				new SimpleRetrievalMethodEncryptedKeyResolver()
			));
			try {
				var decrypter = new Decrypter(null, resolver, keyResolver, DECRYPTION_ALGORITHMS, null);
				decrypter.setRootInNewDocument(true);
				return decrypter.decrypt(encrypted.get(0));
			} catch (DecryptionException | RuntimeException e) {
				LOG.log(Level.FINE, e, () -> "SAML <EncryptedAssertion> could not be decrypted");
				throw decryptionFailed();
			}
		}
		return assertions.get(0);
	}

	private static AuthenticationException decryptionFailed() {
		return new AuthenticationException("SAML response could not be decrypted")
			.wwwAuthenticate("SAML error=\"decryption_failed\"");
	}

	private void verifySignature(Assertion assertion) throws AuthenticationException {
		var signature = assertion.getSignature();
		if (signature == null)
			throw new AuthenticationException("SAML assertion is unsigned")
				.wwwAuthenticate("SAML error=\"signature_required\"");

		var alg = signature.getSignatureAlgorithm();
		if (alg == null || !algorithms.contains(alg)) // HTT: alg==null unreachable; a signature with no SignatureMethod is rejected while parsing the response
			throw new AuthenticationException("SAML signature algorithm not allowlisted: " + alg)
				.wwwAuthenticate("SAML error=\"signature_algorithm_rejected\"");

		validateDigestAlgorithms(signature);

		// First, run the SAML signature profile validator to catch wrapping/structural attacks.
		try {
			new SAMLSignatureProfileValidator().validate(signature);
		} catch (SignatureException e) {
			throw new AuthenticationException(e, "SAML signature profile validation failed")
				.wwwAuthenticate("SAML error=\"signature_invalid\"");
		}

		Credential credential = resolveSigningCredential();
		try {
			SignatureValidator.validate(signature, credential);
		} catch (SignatureException e) {
			throw new AuthenticationException(e, "SAML signature verification failed")
				.wwwAuthenticate("SAML error=\"signature_invalid\"");
		}
	}

	private Credential resolveSigningCredential() throws AuthenticationException {
		if (signingCredential != null)
			return signingCredential;
		var idp = resolveIdpDescriptor();
		var credential = extractSigningCredential(idp);
		if (credential == null)
			throw new AuthenticationException("No signing X.509 certificate in metadata for issuer: " + expectedIssuer)
				.wwwAuthenticate("SAML error=\"metadata_invalid\"");
		return credential;
	}

	private IDPSSODescriptor resolveIdpDescriptor() throws AuthenticationException {
		try {
			var criteria = new CriteriaSet(
				new EntityIdCriterion(expectedIssuer),
				new EntityRoleCriterion(IDPSSODescriptor.DEFAULT_ELEMENT_NAME)
			);
			var entityDescriptor = metadataResolver.resolveSingle(criteria);
			if (entityDescriptor == null)
				throw new AuthenticationException("No SAML metadata for issuer: " + expectedIssuer)
					.wwwAuthenticate("SAML error=\"metadata_missing\"");
			var idp = entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
			if (idp == null)
				throw new AuthenticationException("SAML metadata for issuer has no IDPSSODescriptor")
					.wwwAuthenticate("SAML error=\"metadata_invalid\"");
			return idp;
		} catch (ResolverException e) {
			throw new AuthenticationException(e, "SAML metadata resolution failed")
				.wwwAuthenticate("SAML error=\"metadata_invalid\"");
		}
	}

	private Credential extractSigningCredential(IDPSSODescriptor idp) {
		for (KeyDescriptor kd : idp.getKeyDescriptors()) {
			var credential = credentialFromKeyDescriptor(kd);
			if (credential != null)
				return credential;
		}
		return null;
	}

	private Credential credentialFromKeyDescriptor(KeyDescriptor kd) {
		if (!isSigningKey(kd))
			return null;
		var keyInfo = kd.getKeyInfo();
		if (keyInfo == null)
			return null;
		for (var x509 : keyInfo.getX509Datas()) {
			var credential = credentialFromX509Data(x509);
			if (credential != null)
				return credential;
		}
		return null;
	}

	private Credential credentialFromX509Data(X509Data x509) {
		for (var cert : x509.getX509Certificates()) {
			var base64 = cert.getValue();
			if (base64 == null)
				continue;
			var credential = parseX509Credential(base64);
			if (credential != null)
				return credential;
		}
		return null;
	}

	private static boolean isSigningKey(KeyDescriptor kd) {
		return kd.getUse() == null || kd.getUse() == UsageType.SIGNING || kd.getUse() == UsageType.UNSPECIFIED;
	}

	private BasicX509Credential parseX509Credential(String base64) {
		try {
			var bytes = Base64.getMimeDecoder().decode(base64);
			var cf = java.security.cert.CertificateFactory.getInstance("X.509");
			var certificate = (java.security.cert.X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
			// The metadata signing cert is the trust anchor; reject one outside its validity window so an
			// expired (or not-yet-valid) cert can no longer be used to verify assertions.
			certificate.checkValidity(Date.from(clock.instant()));
			return new BasicX509Credential(certificate);
		} catch (java.security.cert.CertificateException | IllegalArgumentException e) {
			return null;  // Expired/not-yet-valid or malformed cert: caller tries the next certificate.
		}
	}

	private Map<String,Object> buildClaims(Response response, Assertion assertion) throws AuthenticationException {
		validateConditions(assertion);
		validateIssuer(response, assertion);

		Map<String,Object> claims = m(ISSUER_TYPE_CLAIM, ISSUER_TYPE_SAML, "iss", expectedIssuer, "aud", spEntityId);

		for (var stmt : assertion.getAttributeStatements()) {
			for (var attr : stmt.getAttributes()) {
				var name = attr.getName();
				if (name == null || RESERVED_CLAIMS.contains(name))
					continue;
				var values = l();
				for (var av : attr.getAttributeValues())
					values.add(extractStringValue(av));
				if (values.size() == 1)
					claims.put(name, values.get(0));
				else if (!values.isEmpty())
					claims.put(name, values);
			}
		}
		return claims;
	}

	private void validateConditions(Assertion assertion) throws AuthenticationException {
		var conditions = assertion.getConditions();
		if (conditions == null)
			throw rejectAssertion("missing <Conditions>");

		var now = clock.instant();
		var nbf = conditions.getNotBefore();
		var noa = conditions.getNotOnOrAfter();
		if (nbf != null && nbf.minus(clockSkew).isAfter(now))
			throw rejectAssertion("assertion not yet valid");
		if (noa != null && !noa.plus(clockSkew).isAfter(now))
			throw rejectAssertion("assertion has expired");

		boolean audienceOk = false;
		for (var ar : conditions.getAudienceRestrictions()) {
			for (var aud : ar.getAudiences()) {
				if (eq(spEntityId, aud.getURI())) {
					audienceOk = true;
					break;
				}
			}
		}
		if (!audienceOk)
			throw rejectAssertion("audience does not include SP entity ID: " + spEntityId);
	}

	private void validateIssuer(Response response, Assertion assertion) throws AuthenticationException {
		var assertionIssuer = assertion.getIssuer();
		if (assertionIssuer == null || neq(expectedIssuer, assertionIssuer.getValue()))
			throw rejectAssertion("assertion <Issuer> does not match expected issuer");
		var respIssuer = response.getIssuer();
		if (respIssuer != null && neq(expectedIssuer, respIssuer.getValue()))
			throw rejectAssertion("response <Issuer> does not match expected issuer");
	}

	private void validateDigestAlgorithms(org.opensaml.xmlsec.signature.Signature signature) throws AuthenticationException {
		var dom = signature.getDOM();
		if (dom == null) // HTT: null DOM unreachable; a verifiable signature is always marshalled to DOM
			return; // HTT: null DOM unreachable; a verifiable signature is always marshalled to DOM
		var digestNodes = dom.getElementsByTagNameNS(XMLSIG_NS, "DigestMethod");
		for (var i = 0; i < digestNodes.getLength(); i++) {
			var alg = ((Element) digestNodes.item(i)).getAttribute("Algorithm");
			if (alg == null || alg.isEmpty() || alg.toLowerCase(Locale.ROOT).contains("sha1") || !digestAlgorithms.contains(alg)) // HTT: the alg==null clause is unreachable because Element.getAttribute never returns null; kept as a fail-closed guard
				throw new AuthenticationException("SAML signature digest algorithm not allowlisted: " + alg)
					.wwwAuthenticate("SAML error=\"signature_algorithm_rejected\"");
		}
	}

	private void validateSubjectConfirmation(Assertion assertion) throws AuthenticationException {
		var subject = assertion.getSubject();
		if (subject == null)
			throw rejectAssertion("missing <Subject> for bearer confirmation");
		String failure = null;
		var sawBearer = false;
		for (var sc : subject.getSubjectConfirmations()) {
			if (! SubjectConfirmation.METHOD_BEARER.equals(sc.getMethod()))
				continue;
			sawBearer = true;
			var reason = confirmationFailureReason(sc);
			if (reason == null)
				return;  // A valid bearer confirmation was found.
			failure = reason;
		}
		if (! sawBearer)
			throw rejectAssertion("missing bearer <SubjectConfirmation>");
		throw rejectAssertion(failure);
	}

	@SuppressWarnings({
		"java:S3776" // Branching is inherent to the ordered bearer-confirmation checks.
	})
	private String confirmationFailureReason(SubjectConfirmation sc) {
		var data = sc.getSubjectConfirmationData();
		if (data == null)
			return "bearer <SubjectConfirmationData> is missing";
		if (neq(recipient, data.getRecipient()))
			return "bearer confirmation <Recipient> does not match the expected ACS URL";
		var now = clock.instant();
		var noa = data.getNotOnOrAfter();
		if (noa == null)
			return "bearer confirmation is missing NotOnOrAfter";
		if (! noa.plus(clockSkew).isAfter(now))
			return "bearer confirmation has expired";
		var nbf = data.getNotBefore();
		if (nbf != null && nbf.minus(clockSkew).isAfter(now))
			return "bearer confirmation is not yet valid";
		var inResponseTo = data.getInResponseTo();
		var hasInResponseTo = inResponseTo != null && ! inResponseTo.isEmpty(); // HTT: OpenSAML trims an empty InResponseTo to null; kept as a fail-closed guard
		if (expectedInResponseTo != null) {
			if (neq(expectedInResponseTo, inResponseTo))
				return "bearer confirmation <InResponseTo> does not match the expected request ID";
		} else if (hasInResponseTo) {
			if (! consumeAuthnRequest(inResponseTo))
				return "bearer confirmation <InResponseTo> does not reference an outstanding request";
		} else if (! allowIdpInitiated) {
			return "unsolicited (IdP-initiated) responses are not allowed";
		}
		if (expectedAddress != null && neq(expectedAddress, data.getAddress()))
			return "bearer confirmation <Address> does not match the expected client address";
		return null;
	}

	private void recordSingleUse(Assertion assertion) throws AuthenticationException {
		var id = assertion.getID();
		if (id == null || id.isEmpty()) // HTT: OpenSAML trims an empty ID to null, so the isEmpty() branch cannot run; kept as a fail-closed guard
			throw rejectAssertion("assertion has no ID for single-use enforcement");
		var notOnOrAfter = resolveNotOnOrAfter(assertion);
		if (notOnOrAfter == null)
			throw rejectAssertion("assertion has no NotOnOrAfter to bound one-time use");
		var expiresAtMs = notOnOrAfter.plus(clockSkew).toEpochMilli();
		var firstSeen = replayCache.checkAndRecord(id, expiresAtMs, ReplayCache.FailMode.FAIL_CLOSED,
			e -> LOG.log(Level.WARNING, e, () -> "SAML assertion single-use check failed; rejecting (fail-closed)."));
		if (! firstSeen)
			throw new AuthenticationException("SAML assertion has already been presented")
				.wwwAuthenticate("SAML error=\"assertion_replayed\"");
	}

	/**
	 * Resolves the effective {@code NotOnOrAfter} that bounds one-time use of the assertion.
	 *
	 * <p>
	 * The resolved value is the latest of the assertion's {@code Conditions/NotOnOrAfter} and any bearer
	 * {@code SubjectConfirmationData/NotOnOrAfter}.  Returns <jk>null</jk> when the assertion carries neither
	 * &mdash; in that case its validity window is unbounded, so single-use cannot be enforced past a cache
	 * eviction and the caller rejects the assertion rather than inventing a short fallback that later re-opens
	 * replay.  {@code <Conditions>} itself is guaranteed non-<jk>null</jk> here (a null {@code <Conditions>} is
	 * rejected earlier by {@link #validateConditions(Assertion)}).
	 *
	 * @param assertion The assertion.
	 * @return The latest bounding instant, or <jk>null</jk> if the assertion carries no {@code NotOnOrAfter}.
	 */
	private static Instant resolveNotOnOrAfter(Assertion assertion) {
		var result = assertion.getConditions().getNotOnOrAfter();
		var subject = assertion.getSubject();
		if (subject != null) {
			for (var sc : subject.getSubjectConfirmations()) {
				var noa = bearerNotOnOrAfter(sc);
				if (noa != null && (result == null || noa.isAfter(result)))
					result = noa;
			}
		}
		return result;
	}

	/**
	 * Returns the {@code NotOnOrAfter} of {@code sc}'s {@code <SubjectConfirmationData>} if {@code sc} is a
	 * bearer confirmation carrying one, or <jk>null</jk> otherwise.
	 *
	 * @param sc The subject confirmation.
	 * @return The bearer confirmation's {@code NotOnOrAfter}, or <jk>null</jk>.
	 */
	private static Instant bearerNotOnOrAfter(SubjectConfirmation sc) {
		if (! SubjectConfirmation.METHOD_BEARER.equals(sc.getMethod()))
			return null;
		var data = sc.getSubjectConfirmationData();
		return data == null ? null : data.getNotOnOrAfter();
	}

	private static String extractStringValue(org.opensaml.core.xml.XMLObject av) {
		if (av == null) // HTT: null av unreachable; OpenSAML attribute value lists never contain null entries
			return null; // HTT: null av unreachable; OpenSAML attribute value lists never contain null entries
		var dom = av.getDOM();
		if (dom != null) // HTT: the response is always parsed from XML, so every unmarshalled value carries its DOM
			return dom.getTextContent();
		return av.toString(); // HTT: the response is always parsed from XML, so every unmarshalled value carries its DOM
	}

	private static String nameId(Assertion assertion) throws AuthenticationException {
		var subject = assertion.getSubject();
		var nameID = subject == null ? null : subject.getNameID();
		if (nameID == null || nameID.getValue() == null || nameID.getValue().isBlank())
			throw rejectAssertion("missing <NameID> subject");
		return nameID.getValue();
	}

	private static AuthenticationException rejectAssertion(String reason) {
		return new AuthenticationException("SAML assertion rejected: " + reason)
			.wwwAuthenticate("SAML error=\"invalid_assertion\"");
	}
}
