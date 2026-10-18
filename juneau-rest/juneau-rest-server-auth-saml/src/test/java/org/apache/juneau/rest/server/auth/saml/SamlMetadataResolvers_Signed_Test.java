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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.apache.commons.xml.secure.SecureTransformerFactory;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.opensaml.core.xml.config.*;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.xmlsec.signature.impl.*;
import org.opensaml.xmlsec.signature.support.*;

import net.shibboleth.shared.component.*;

import com.sun.net.httpserver.*;

/**
 * Tests for {@link SamlMetadataResolvers#url(String, java.security.cert.X509Certificate)}: pinned-certificate
 * metadata signature validation, plus fetch failures (interrupt, non-XML body, truncated body).
 *
 * @since 10.0.0
 */
class SamlMetadataResolvers_Signed_Test extends TestBase {

	private static final String UNSIGNED_METADATA =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		+ "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" ID=\"_md1\" entityID=\"https://idp.example.com/idp\">"
		+ "<IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
		+ "<SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://idp.example.com/sso\"/>"
		+ "</IDPSSODescriptor></EntityDescriptor>";

	@FunctionalInterface
	private interface UrlAction {
		void run(String url) throws Exception;
	}

	private static void withBody(byte[] body, UrlAction action) throws Exception {
		withServer(exchange -> {
			exchange.sendResponseHeaders(200, body.length);
			try (var os = exchange.getResponseBody()) {
				os.write(body);
			}
		}, action);
	}

	private static void withServer(HttpHandler handler, UrlAction action) throws Exception {
		var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/metadata", handler);
		server.start();
		try {
			action.run("http://127.0.0.1:" + server.getAddress().getPort() + "/metadata");
		} finally {
			server.stop(0);
		}
	}

	/** Signs {@link #UNSIGNED_METADATA} with the test key and returns the serialized document. */
	private static String signedMetadata() throws Exception {
		OpenSamlBootstrap.ensureInitialized(); // the registry lookups below need it, whatever order the tests run in
		var doc = SamlXml.newDocumentBuilder().parse(new ByteArrayInputStream(UNSIGNED_METADATA.getBytes(StandardCharsets.UTF_8)));
		var root = doc.getDocumentElement();
		var ed = (EntityDescriptor) XMLObjectProviderRegistrySupport.getUnmarshallerFactory().getUnmarshaller(root).unmarshall(root);
		var sig = new SignatureBuilder().buildObject();
		sig.setSigningCredential(SamlTestKeys.credential());
		sig.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
		sig.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
		ed.setSignature(sig);
		var dom = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(ed).marshall(ed);
		Signer.signObject(sig);
		var sw = new StringWriter();
		var transformer = SecureTransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(dom), new StreamResult(sw));
		return sw.toString();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: pinned metadata-signing certificate
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_signedMetadata_pinnedCertMatches_resolves() throws Exception {
		var cert = SamlTestKeys.certificate();
		var body = signedMetadata().getBytes(StandardCharsets.UTF_8);
		withBody(body, url -> assertNotNull(SamlMetadataResolvers.url(url, cert)));
	}

	@Test void a02_unsignedMetadata_pinnedCert_rejected() throws Exception {
		var cert = SamlTestKeys.certificate();
		withBody(UNSIGNED_METADATA.getBytes(StandardCharsets.UTF_8), url -> {
			// Positive control: the same document resolves when no certificate is pinned, so the pin is what rejects it.
			assertNotNull(SamlMetadataResolvers.url(url));
			var ex = assertThrows(IOException.class, () -> SamlMetadataResolvers.url(url, cert));
			assertTrue(ex.getMessage().contains("Failed to initialize DOMMetadataResolver"), ex.getMessage());
			assertInstanceOf(ComponentInitializationException.class, ex.getCause()); // not an XML parse failure
		});
	}

	@Test void a03_signedMetadata_wrongPinnedCert_rejected() throws Exception {
		// Same document, but the pin is a different certificate (the signer's key does not match it).
		var other = SamlTestKeys.otherCertificate();
		var body = signedMetadata().getBytes(StandardCharsets.UTF_8);
		withBody(body, url -> {
			var ex = assertThrows(IOException.class, () -> SamlMetadataResolvers.url(url, other));
			// A signature-filter rejection, not a plain fetch failure.
			assertTrue(ex.getMessage().contains("Failed to initialize DOMMetadataResolver"), ex.getMessage());
			assertInstanceOf(ComponentInitializationException.class, ex.getCause()); // not an XML parse failure
		});
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: fetch failures
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_bodyNotXml_throwsIOException() throws Exception {
		withBody("this is not xml".getBytes(StandardCharsets.UTF_8), url -> assertThrows(IOException.class, () -> SamlMetadataResolvers.url(url)));
	}

	@Test void b02_interruptedBeforeFetch_throwsIOExceptionAndKeepsInterruptFlag() throws Exception {
		withBody(UNSIGNED_METADATA.getBytes(StandardCharsets.UTF_8), url -> {
			Thread.currentThread().interrupt();
			try {
				var ex = assertThrows(IOException.class, () -> SamlMetadataResolvers.url(url));
				assertTrue(ex.getMessage().contains("Interrupted"), ex.getMessage());
				assertTrue(Thread.currentThread().isInterrupted());
			} finally {
				Thread.interrupted(); // clear the flag so later tests are unaffected
			}
		});
	}

	@Test void b03_connectionClosedMidBody_throwsIOException() throws Exception {
		withServer(exchange -> {
			exchange.sendResponseHeaders(200, 100_000); // promise far more bytes than are sent
			try (var os = exchange.getResponseBody()) {
				os.write(new byte[16]);
				os.flush();
			}
			exchange.close();
		}, url -> assertThrows(IOException.class, () -> SamlMetadataResolvers.url(url)));
	}
}
