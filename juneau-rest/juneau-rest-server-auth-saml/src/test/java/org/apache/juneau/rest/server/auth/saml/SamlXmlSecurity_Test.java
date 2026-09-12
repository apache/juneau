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
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.opensaml.core.criterion.*;
import org.opensaml.saml.metadata.resolver.impl.*;
import org.opensaml.security.credential.*;
import org.xml.sax.*;

import com.sun.net.httpserver.*;

import net.shibboleth.shared.resolver.*;

/**
 * Checks the DOM parsing boundaries for SAML responses and remotely loaded metadata.
 */
class SamlXmlSecurity_Test extends TestBase {

	private static final String ISSUER = "https://idp.example.com";
	private static final String AUDIENCE = "https://sp.example.com";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final String METADATA = "<EntityDescriptor xmlns='urn:oasis:names:tc:SAML:2.0:metadata' entityID='" + ISSUER + "'>"
		+ "<IDPSSODescriptor protocolSupportEnumeration='urn:oasis:names:tc:SAML:2.0:protocol'>"
		+ "<SingleSignOnService Binding='urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect' Location='https://idp.example.com/sso'/>"
		+ "</IDPSSODescriptor><Organization><OrganizationName xml:lang='en'>safe</OrganizationName>"
		+ "<OrganizationDisplayName xml:lang='en'>safe</OrganizationDisplayName>"
		+ "<OrganizationURL xml:lang='en'>https://idp.example.com</OrganizationURL></Organization></EntityDescriptor>";
	private BasicCredential credential;
	private String signedResponse;
	private volatile String metadataBody;
	private String metadataUrl;
	private final AtomicInteger metadataRequests = new AtomicInteger();

	private HttpServer server;
	private final AtomicInteger requests = new AtomicInteger();
	private String external;

	@BeforeEach void startServer() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/external", exchange -> {
			requests.incrementAndGet();
			var body = "<!ENTITY injected 'FETCHED'>".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (var stream = exchange.getResponseBody()) {
				stream.write(body);
			}
		});
		server.createContext("/metadata", exchange -> {
			metadataRequests.incrementAndGet();
			var body = metadataBody.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (var stream = exchange.getResponseBody()) {
				stream.write(body);
			}
		});
		server.start();
		external = "http://127.0.0.1:" + server.getAddress().getPort() + "/external";
		metadataUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/metadata";
		metadataBody = METADATA;
		credential = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		signedResponse = SamlTestSupport.buildSignedResponse(credential, ISSUER, AUDIENCE, "alice",
			NOW.minusSeconds(60), NOW.plusSeconds(300), Map.of());
	}

	@AfterEach void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private SamlAssertionValidator validator() {
		return SamlAssertionValidator.create().spEntityId(AUDIENCE).expectedIssuer(ISSUER)
			.signingCredential(credential).clock(Clock.fixed(NOW, ZoneOffset.UTC)).build();
	}

	private static String rootName(String xml) {
		return xml.substring(1, xml.indexOf(' '));
	}

	private static String attack(String xml, String vector, String uri, String marker) {
		var root = rootName(xml);
		return switch (vector) {
			case "general" -> "<!DOCTYPE " + root + " [<!ENTITY ext SYSTEM '" + uri + "'>]>" + xml.replace(marker, "&ext;");
			case "parameter" -> "<!DOCTYPE " + root + " [<!ENTITY % ext SYSTEM '" + uri + "'>%ext;]>" + xml;
			case "systemDtd" -> "<!DOCTYPE " + root + " SYSTEM '" + uri + "'>" + xml;
			case "publicDtd" -> "<!DOCTYPE " + root + " PUBLIC '-//Juneau//DTD XXE Test//EN' '" + uri + "'>" + xml;
			case "xincludeXml", "xincludeText" -> xml.replace(marker,
				"<xi:include xmlns:xi='http://www.w3.org/2001/XInclude' href='" + uri + "' parse='"
				+ (vector.equals("xincludeXml") ? "xml" : "text") + "'/>");
			case "schemaLocation" -> xml.replaceFirst(" ", " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='"
				+ "urn:oasis:names:tc:SAML:2.0:metadata " + uri + " urn:oasis:names:tc:SAML:2.0:protocol " + uri + "' ");
			case "noNamespaceSchemaLocation" -> xml.replaceFirst(" ",
				" xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='" + uri + "' ");
			default -> throw new IllegalArgumentException(vector);
		};
	}

	private String readMetadata() throws Exception {
		var resolver = SamlMetadataResolvers.url(metadataUrl);
		try {
			var entity = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(ISSUER)));
			assertNotNull(entity, "The metadata fixture must resolve successfully");
			return entity.getOrganization().getOrganizationNames().get(0).getValue();
		} finally {
			((DOMMetadataResolver) resolver).destroy();
		}
	}

	@Test void ordinaryDocumentsStillWork() throws Exception {
		assertEquals("alice", validator().validate(signedResponse).getName());
		assertEquals("safe", readMetadata());
		assertEquals(1, metadataRequests.get());
	}

	@ParameterizedTest
	@ValueSource(strings = {"general", "parameter", "systemDtd", "publicDtd", "xincludeXml", "xincludeText",
		"schemaLocation", "noNamespaceSchemaLocation"})
	void responseReferencesNeverFetch(String vector) throws Exception {
		var v = validator();
		try {
			v.validate(attack(signedResponse, vector, external, "alice"));
		} catch (AuthenticationException expected) {
			// Signature or XML rejection is safe only if no external access happened first.
		}
		assertEquals(0, requests.get(), vector + " fetched an external resource");
	}

	@ParameterizedTest
	@ValueSource(strings = {"general", "parameter", "systemDtd", "publicDtd", "xincludeXml", "xincludeText",
		"schemaLocation", "noNamespaceSchemaLocation"})
	void metadataReferencesNeverFetch(String vector) throws Exception {
		metadataBody = attack(METADATA, vector, external, "safe");
		try {
			readMetadata();
		} catch (IOException expected) {
			// Both rejection and ignoring a reference are permitted.
		}
		assertEquals(1, metadataRequests.get(), "The intended top-level metadata must be fetched");
		assertEquals(0, requests.get(), vector + " fetched an external resource");
	}

	@ParameterizedTest
	@ValueSource(strings = {"general", "parameter", "systemDtd", "publicDtd", "xincludeText"})
	void responseFileReferencesCannotSupplySignedSubject(String vector, @TempDir Path directory) throws Exception {
		var file = directory.resolve("external.txt");
		Files.writeString(file, vector.equals("general") || vector.equals("xincludeText") ? "alice" : "<!ENTITY subject 'alice'>");
		var xml = attack(signedResponse, vector, file.toUri().toString(), "alice");
		if (!vector.equals("general") && !vector.equals("xincludeText"))
			xml = xml.replace("alice", "&subject;");
		var input = xml;
		var v = validator();
		// Resolving the file reconstructs the correctly signed assertion. An unrelated bad signature cannot mask a leak.
		assertThrows(AuthenticationException.class, () -> v.validate(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"general", "parameter", "systemDtd", "publicDtd", "xincludeText"})
	void metadataFileReferencesCannotSupplyContent(String vector, @TempDir Path directory) throws Exception {
		var file = directory.resolve("external.txt");
		Files.writeString(file, vector.equals("general") || vector.equals("xincludeText")
			? "PRIVATE_FILE_MARKER" : "<!ENTITY secret 'PRIVATE_FILE_MARKER'>");
		metadataBody = attack(METADATA, vector, file.toUri().toString(), "safe");
		if (!vector.equals("general") && !vector.equals("xincludeText"))
			metadataBody = metadataBody.replace("safe", "&secret;");
		try {
			assertNotEquals("PRIVATE_FILE_MARKER", readMetadata());
		} catch (IOException expected) {
			for (Throwable cause = expected; cause != null; cause = cause.getCause())
				assertFalse(cause.toString().contains("PRIVATE_FILE_MARKER"));
		}
	}

	private static String expansion(String xml, String marker) {
		// A bounded fixture exceeds normal expansion-count limits without generating a true gigabyte payload.
		var dtd = new StringBuilder("<!DOCTYPE " + rootName(xml) + " [<!ENTITY e0 'x'>");
		for (int i = 1; i <= 5; i++)
			dtd.append("<!ENTITY e").append(i).append(" '").append(("&e" + (i - 1) + ";").repeat(10)).append("'>");
		return dtd.append("]>").append(xml.replace(marker, "&e5;")).toString();
	}

	private static void assertXmlRejection(Throwable failure) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause())
			if (cause instanceof SAXParseException)
				return;
		fail("Expected XML parser rejection, not a later SAML/signature failure", failure);
	}

	@Test
	@Timeout(10)
	void responseExpansionIsRejectedByXmlParser() {
		var v = validator();
		var xml = expansion(signedResponse, "alice");
		assertXmlRejection(assertThrows(AuthenticationException.class, () -> v.validate(xml)));
	}

	@Test
	@Timeout(10)
	void metadataExpansionIsRejectedByXmlParser() {
		metadataBody = expansion(METADATA, "safe");
		assertXmlRejection(assertThrows(IOException.class, () -> SamlMetadataResolvers.url(metadataUrl)));
	}
}
