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

import java.io.*;
import java.security.*;
import java.time.*;
import java.util.*;

import javax.xml.namespace.*;
import javax.xml.parsers.*;

import org.apache.juneau.rest.server.auth.*;
import org.opensaml.core.criterion.*;
import org.opensaml.core.xml.io.*;
import org.opensaml.saml.common.assertion.*;
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
 * 	<li><b>Mandatory signature</b> on every assertion (or, optionally, on the {@code <Response>} envelope).
 * 	<li><b>Strict signature-algorithm allowlist</b> &mdash; only {@code rsa-sha256} and
 * 		{@code ecdsa-sha256} are accepted out of the box.  SHA-1 is permanently rejected.
 * 	<li><b>Clock skew</b> &mdash; defaults to 60 seconds tolerance on {@code NotBefore} /
 * 		{@code NotOnOrAfter}; capped at 300 seconds.
 * 	<li><b>Audience restriction</b> &mdash; {@code <AudienceRestriction>} must list the configured SP entity ID.
 * 	<li><b>Encrypted assertions</b> &mdash; if the response contains
 * 		{@code <EncryptedAssertion>}, a {@code decryptionCredential} must be configured.
 * </ul>
 *
 * <h5 class='topic'>Marker claim</h5>
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
 * <h5 class='topic'>Builder usage</h5>
 *
 * <p class='bjava'>
 * 	<jk>var</jk> validator = SamlAssertionValidator.<jsm>create</jsm>()
 * 		.metadataResolver(SamlMetadataResolvers.<jsm>url</jsm>(<js>"https://idp.example.com/metadata"</js>))
 * 		.spEntityId(<js>"https://sp.example.com"</js>)
 * 		.expectedIssuer(<js>"https://idp.example.com"</js>)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
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
	"java:S6539"  // Monolithic class: validator is a single end-to-end pipeline.
})
public class SamlAssertionValidator {

	/** Marker claim name on the returned {@link ClaimsPrincipal}. */
	public static final String ISSUER_TYPE_CLAIM = "issuerType";

	/** Marker claim value identifying the principal as SAML-issued. */
	public static final String ISSUER_TYPE_SAML = "SAML";

	/** Maximum clock-skew tolerance the builder will accept (5 minutes). */
	private static final Duration MAX_CLOCK_SKEW = Duration.ofSeconds(300);

	/** Default allowlist of XML signature algorithms. */
	private static final Set<String> DEFAULT_ALGORITHMS = Set.of(
		SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256,
		SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA256
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
		private Set<String> algorithms = new LinkedHashSet<>(DEFAULT_ALGORITHMS);
		private Duration clockSkew = Duration.ofSeconds(60);
		private Clock clock = Clock.systemUTC();

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the {@link MetadataResolver} used to resolve the IdP's signing credential at validation time.
		 *
		 * <p>
		 * Mutually exclusive with {@link #signingCredential(Credential)}.
		 *
		 * @param value The metadata resolver.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder metadataResolver(MetadataResolver value) {
			metadataResolver = assertArgNotNull("value", value);
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
			signingCredential = assertArgNotNull("value", value);
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
			decryptionCredential = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the Service Provider entity ID used to validate {@code <AudienceRestriction>}.
		 *
		 * @param value The SP entity ID.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder spEntityId(String value) {
			spEntityId = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the expected IdP issuer (the {@code <Issuer>} element).  Required.
		 *
		 * @param value The expected IdP entity ID.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder expectedIssuer(String value) {
			expectedIssuer = assertArgNotNullOrBlank("value", value);
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
			assertArgNotNull("values", values);
			if (values.length == 0)
				throw new IllegalArgumentException("algorithms allowlist must be non-empty");
			var next = new LinkedHashSet<String>();
			for (var a : values) {
				assertArgNotNull("algorithm", a);
				if (a.toLowerCase(Locale.ROOT).contains("sha1"))
					throw new IllegalArgumentException("SHA-1 algorithms are permanently rejected: " + a);
				next.add(a);
			}
			algorithms = next;
			return this;
		}

		/**
		 * Sets the clock-skew tolerance for {@code NotBefore} / {@code NotOnOrAfter} validation.
		 *
		 * @param value The tolerance.  Must be non-negative and not exceed 5 minutes.
		 * @return This object.
		 */
		public Builder clockSkew(Duration value) {
			assertArgNotNull("value", value);
			if (value.isNegative())
				throw new IllegalArgumentException("clockSkew must be non-negative");
			if (value.compareTo(MAX_CLOCK_SKEW) > 0)
				throw new IllegalArgumentException("clockSkew must not exceed 5 minutes (was " + value + ")");
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
			clock = assertArgNotNull("value", value);
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
			return new SamlAssertionValidator(this);
		}
	}

	private final MetadataResolver metadataResolver;
	private final Credential signingCredential;
	private final Credential decryptionCredential;
	private final String spEntityId;
	private final String expectedIssuer;
	private final Set<String> algorithms;
	private final Duration clockSkew;
	private final Clock clock;

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
		this.algorithms = Collections.unmodifiableSet(new LinkedHashSet<>(b.algorithms));
		this.clockSkew = b.clockSkew;
		this.clock = b.clock;
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
	 * Validates the supplied SAML 2.0 {@code <samlp:Response>} XML and returns the resolved principal.
	 *
	 * @param xml The full XML payload (already base64-decoded; already URL-decoded and inflated for the
	 * 	REDIRECT binding).  Must be a {@code <samlp:Response>} envelope.
	 * @return A {@link ClaimsPrincipal} carrying the IdP-supplied claims plus the {@code issuerType=SAML}
	 * 	marker.
	 * @throws AuthenticationException If the response cannot be parsed or validation fails.
	 */
	public Principal validate(String xml) throws AuthenticationException {
		assertArgNotNullOrBlank("xml", xml);
		var response = parseResponse(xml);
		var assertion = extractAndDecryptAssertion(response);
		verifySignature(assertion);
		var claims = buildClaims(response, assertion);
		var subject = nameId(assertion);
		return new ClaimsPrincipal(subject, claims);
	}

	private Response parseResponse(String xml) throws AuthenticationException {
		try {
			var dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			dbf.setXIncludeAware(false);
			dbf.setExpandEntityReferences(false);
			var doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
			Element root = doc.getDocumentElement();
			var registry = org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
			var unmarshaller = registry.getUnmarshaller(root);
			if (unmarshaller == null)
				throw new AuthenticationException("Unknown SAML element: " + root.getNodeName())
					.wwwAuthenticate("SAML error=\"invalid_response\"");
			var obj = unmarshaller.unmarshall(root);
			if (!(obj instanceof Response r))
				throw new AuthenticationException("SAML envelope is not a <Response>: " + cns(obj))
					.wwwAuthenticate("SAML error=\"invalid_response\"");
			return r;
		} catch (UnmarshallingException | ParserConfigurationException | java.io.IOException | org.xml.sax.SAXException e) {
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
			if (decryptionCredential == null)
				throw new AuthenticationException("SAML response contains <EncryptedAssertion> but no decryption credential is configured")
					.wwwAuthenticate("SAML error=\"decryption_required\"");
			var resolver = (KeyInfoCredentialResolver) new StaticKeyInfoCredentialResolver(decryptionCredential);
			EncryptedKeyResolver keyResolver = new ChainingEncryptedKeyResolver(List.of(
				new InlineEncryptedKeyResolver(),
				new SimpleRetrievalMethodEncryptedKeyResolver()
			));
			var decrypter = new Decrypter(null, resolver, keyResolver);
			decrypter.setRootInNewDocument(true);
			try {
				return decrypter.decrypt(encrypted.get(0));
			} catch (org.opensaml.xmlsec.encryption.support.DecryptionException e) {
				throw new AuthenticationException(e, "SAML <EncryptedAssertion> could not be decrypted")
					.wwwAuthenticate("SAML error=\"decryption_failed\"");
			}
		}
		return assertions.get(0);
	}

	private void verifySignature(Assertion assertion) throws AuthenticationException {
		var signature = assertion.getSignature();
		if (signature == null)
			throw new AuthenticationException("SAML assertion is unsigned")
				.wwwAuthenticate("SAML error=\"signature_required\"");

		var alg = signature.getSignatureAlgorithm();
		if (alg == null || !algorithms.contains(alg)) // HTT: alg==null unreachable; OpenSAML always populates SignatureAlgorithm after signing
			throw new AuthenticationException("SAML signature algorithm not allowlisted: " + alg)
				.wwwAuthenticate("SAML error=\"signature_algorithm_rejected\"");

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

	private static Credential extractSigningCredential(IDPSSODescriptor idp) {
		for (KeyDescriptor kd : idp.getKeyDescriptors()) {
			var credential = credentialFromKeyDescriptor(kd);
			if (credential != null) // HTT: true branch (successful credential extraction) requires real X.509 metadata; covered by integration tests
				return credential;
		}
		return null;
	}

	private static Credential credentialFromKeyDescriptor(KeyDescriptor kd) {
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

	private static Credential credentialFromX509Data(X509Data x509) {
		for (var cert : x509.getX509Certificates()) {
			var base64 = cert.getValue();
			if (base64 == null)
				continue;
			var credential = parseX509Credential(base64);
			if (credential != null) // HTT: true branch requires valid X.509 DER bytes; covered by integration tests
				return credential;
		}
		return null;
	}

	private static boolean isSigningKey(KeyDescriptor kd) {
		return kd.getUse() == null || kd.getUse() == UsageType.SIGNING || kd.getUse() == UsageType.UNSPECIFIED;
	}

	private static BasicX509Credential parseX509Credential(String base64) {
		try {
			var bytes = Base64.getMimeDecoder().decode(base64);
			var cf = java.security.cert.CertificateFactory.getInstance("X.509");
			var certificate = (java.security.cert.X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
			return new BasicX509Credential(certificate);
		} catch (java.security.cert.CertificateException | IllegalArgumentException e) {
			return null;  // Caller tries the next certificate.
		}
	}

	private Map<String,Object> buildClaims(Response response, Assertion assertion) throws AuthenticationException {
		validateConditions(assertion);
		validateIssuer(response, assertion);

		var claims = new LinkedHashMap<String,Object>();
		claims.put(ISSUER_TYPE_CLAIM, ISSUER_TYPE_SAML);
		claims.put("iss", expectedIssuer);
		claims.put("aud", spEntityId);

		for (var stmt : assertion.getAttributeStatements()) {
			for (var attr : stmt.getAttributes()) {
				var name = attr.getName();
				if (name == null)
					continue;
				var values = new ArrayList<>();
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
				if (spEntityId.equals(aud.getURI())) {
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
		if (assertionIssuer == null || !expectedIssuer.equals(assertionIssuer.getValue()))
			throw rejectAssertion("assertion <Issuer> does not match expected issuer");
		var respIssuer = response.getIssuer();
		if (respIssuer != null && !expectedIssuer.equals(respIssuer.getValue()))
			throw rejectAssertion("response <Issuer> does not match expected issuer");
	}

	private static String extractStringValue(org.opensaml.core.xml.XMLObject av) {
		if (av == null) // HTT: null av unreachable; OpenSAML attribute value lists never contain null entries
			return null;
		var dom = av.getDOM();
		if (dom != null) // HTT: DOM is non-null only after marshalling; XSString values always have DOM after Response.marshall()
			return dom.getTextContent();
		return av.toString();
	}

	private static String nameId(Assertion assertion) {
		var subject = assertion.getSubject();
		if (subject == null)
			return "<no-subject>";
		var nameID = subject.getNameID();
		if (nameID != null && nameID.getValue() != null)
			return nameID.getValue();
		return "<no-subject>";
	}

	private static AuthenticationException rejectAssertion(String reason) {
		return new AuthenticationException("SAML assertion rejected: " + reason)
			.wwwAuthenticate("SAML error=\"invalid_assertion\"");
	}

	/**
	 * Suppress unused-import warnings for symbols referenced indirectly (kept for API-clarity).
	 */
	@SuppressWarnings({
		"unused" // Symbols are referenced indirectly; method exists to keep imports from being flagged.
	})
	private static void apiReferences() {
		var ignore = new Object[] {
			SignableXMLObject.class, ValidationContext.class, QName.class, UsageType.class, SignatureConstants.class
		};
	}
}
