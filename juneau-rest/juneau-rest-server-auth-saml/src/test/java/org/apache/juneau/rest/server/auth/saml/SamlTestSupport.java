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

import java.io.*;
import java.security.*;
import java.time.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.opensaml.core.config.*;
import org.opensaml.core.xml.config.*;
import org.opensaml.core.xml.io.*;
import org.opensaml.saml.common.*;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.security.credential.*;
import org.opensaml.xmlsec.signature.impl.*;
import org.opensaml.xmlsec.signature.support.*;
import org.opensaml.xmlsec.signature.support.Signer;

/**
 * Test-fixture helpers for building and signing minimal SAML 2.0 {@code <Response>} XML documents.
 *
 * <p>
 * Centralizes the OpenSAML builder boilerplate so individual test classes stay focused on the assertion
 * shapes they care about.
 */
@SuppressWarnings({
	"unchecked"  // Unchecked cast required for generic test utility.
})
final class SamlTestSupport {

	static {
		try {
			InitializationService.initialize();
		} catch (Exception e) {
			throw new RuntimeException("OpenSAML init failed", e);
		}
	}

	// Fixed issue-instant: the response/assertion IssueInstant is informational (the validator checks
	// only the explicit NotBefore/NotOnOrAfter window), so a constant replaces the system clock (java:S8692).
	private static final Instant ISSUE_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

	private SamlTestSupport() {}

	static KeyPair generateRsaKeyPair() throws Exception {
		var kp = KeyPairGenerator.getInstance("RSA");
		kp.initialize(2048);
		return kp.generateKeyPair();
	}

	static BasicCredential credential(KeyPair pair) {
		var c = new BasicCredential(pair.getPublic(), pair.getPrivate());
		c.setUsageType(UsageType.SIGNING);
		return c;
	}

	/**
	 * Builds a minimal signed {@code <samlp:Response>} carrying a single {@code <Assertion>} with the
	 * supplied subject, issuer, audience, and notBefore/notOnOrAfter window.
	 */
	static String buildSignedResponse(
			BasicCredential signingCred,
			String issuer,
			String audience,
			String subjectName,
			Instant notBefore,
			Instant notOnOrAfter,
			java.util.Map<String,String> attributes) throws Exception {

		var bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
		var responseBuilder = (SAMLObjectBuilder<Response>) bf.getBuilder(Response.DEFAULT_ELEMENT_NAME);
		var response = responseBuilder.buildObject();
		response.setID("_resp-" + System.nanoTime());
		response.setIssueInstant(ISSUE_INSTANT);
		response.setVersion(org.opensaml.saml.common.SAMLVersion.VERSION_20);

		response.setIssuer(buildIssuer(bf, issuer));
		response.setStatus(buildStatus(bf));

		var assertion = buildAssertion(bf, issuer, audience, subjectName, notBefore, notOnOrAfter, attributes);

		// Build a Signature object and attach to the assertion.
		var sig = new SignatureBuilder().buildObject();
		sig.setSigningCredential(signingCred);
		sig.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
		sig.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
		assertion.setSignature(sig);

		response.getAssertions().add(assertion);

		// Marshal first so the Signature element is bound to the DOM; then sign.
		MarshallerFactory mf = XMLObjectProviderRegistrySupport.getMarshallerFactory();
		var mar = mf.getMarshaller(response);
		var dom = mar.marshall(response);
		Signer.signObject(sig);

		// Serialize to XML string.
		var sw = new StringWriter();
		var tf = TransformerFactory.newInstance();
		var transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(dom), new StreamResult(sw));
		return sw.toString();
	}

	private static Issuer buildIssuer(org.opensaml.core.xml.XMLObjectBuilderFactory bf, String value) {
		var b = (SAMLObjectBuilder<Issuer>) bf.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
		var i = b.buildObject();
		i.setValue(value);
		return i;
	}

	private static Status buildStatus(org.opensaml.core.xml.XMLObjectBuilderFactory bf) {
		var sb = (SAMLObjectBuilder<Status>) bf.getBuilder(Status.DEFAULT_ELEMENT_NAME);
		var scb = (SAMLObjectBuilder<StatusCode>) bf.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME);
		var status = sb.buildObject();
		var code = scb.buildObject();
		code.setValue(StatusCode.SUCCESS);
		status.setStatusCode(code);
		return status;
	}

	private static Assertion buildAssertion(
			org.opensaml.core.xml.XMLObjectBuilderFactory bf,
			String issuer, String audience, String subjectName,
			Instant notBefore, Instant notOnOrAfter,
			java.util.Map<String,String> attributes) {
		var ab = (SAMLObjectBuilder<Assertion>) bf.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
		var assertion = ab.buildObject();
		assertion.setID("_a-" + System.nanoTime());
		assertion.setIssueInstant(ISSUE_INSTANT);
		assertion.setVersion(org.opensaml.saml.common.SAMLVersion.VERSION_20);
		assertion.setIssuer(buildIssuer(bf, issuer));

		var subBuilder = (SAMLObjectBuilder<Subject>) bf.getBuilder(Subject.DEFAULT_ELEMENT_NAME);
		var nidBuilder = (SAMLObjectBuilder<NameID>) bf.getBuilder(NameID.DEFAULT_ELEMENT_NAME);
		var sub = subBuilder.buildObject();
		var nid = nidBuilder.buildObject();
		nid.setValue(subjectName);
		nid.setFormat(NameIDType.UNSPECIFIED);
		sub.setNameID(nid);
		assertion.setSubject(sub);

		var condBuilder = (SAMLObjectBuilder<Conditions>) bf.getBuilder(Conditions.DEFAULT_ELEMENT_NAME);
		var arBuilder = (SAMLObjectBuilder<AudienceRestriction>) bf.getBuilder(AudienceRestriction.DEFAULT_ELEMENT_NAME);
		var audBuilder = (SAMLObjectBuilder<Audience>) bf.getBuilder(Audience.DEFAULT_ELEMENT_NAME);
		var conditions = condBuilder.buildObject();
		conditions.setNotBefore(notBefore);
		conditions.setNotOnOrAfter(notOnOrAfter);
		var ar = arBuilder.buildObject();
		var aud = audBuilder.buildObject();
		aud.setURI(audience);
		ar.getAudiences().add(aud);
		conditions.getAudienceRestrictions().add(ar);
		assertion.setConditions(conditions);

		if (attributes != null && !attributes.isEmpty()) {
			var asBuilder = (SAMLObjectBuilder<AttributeStatement>) bf.getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME);
			var attrBuilder = (SAMLObjectBuilder<Attribute>) bf.getBuilder(Attribute.DEFAULT_ELEMENT_NAME);
			var stringBuilder = (org.opensaml.core.xml.schema.impl.XSStringBuilder)
				bf.getBuilder(org.opensaml.core.xml.schema.XSString.TYPE_NAME);
			var stmt = asBuilder.buildObject();
			for (var e : attributes.entrySet()) {
				var attr = attrBuilder.buildObject();
				attr.setName(e.getKey());
				var av = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME,
					org.opensaml.core.xml.schema.XSString.TYPE_NAME);
				av.setValue(e.getValue());
				attr.getAttributeValues().add(av);
				stmt.getAttributes().add(attr);
			}
			assertion.getAttributeStatements().add(stmt);
		}
		return assertion;
	}
}
