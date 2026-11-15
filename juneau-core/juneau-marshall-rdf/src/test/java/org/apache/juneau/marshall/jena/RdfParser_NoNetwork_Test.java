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
package org.apache.juneau.marshall.jena;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests that the RDF parsers do not make network requests while parsing.
 */
class RdfParser_NoNetwork_Test extends TestBase {

	private HttpServer server;
	private final AtomicInteger hits = new AtomicInteger();

	@BeforeEach void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/", x -> {
			hits.incrementAndGet();
			var body = "{\"@context\":{\"name\":\"http://example.com/name\"}}".getBytes();
			x.getResponseHeaders().add("Content-Type", "application/ld+json");
			x.sendResponseHeaders(200, body.length);
			x.getResponseBody().write(body);
			x.close();
		});
		server.start();
	}

	@AfterEach void tearDown() {
		server.stop(0);
	}

	private String url() {
		return "http://127.0.0.1:" + server.getAddress().getPort() + "/ctx.jsonld";
	}

	@Test void a01_jsonLd_remoteContextNotFetchedByDefault() {
		var in = "{\"@context\":\"" + url() + "\",\"@id\":\"http://example.com/a\",\"name\":\"x\"}";
		assertThrows(Exception.class, () -> JsonLdParser.DEFAULT.read(in, Object.class));
		assertEquals(0, hits.get());
	}

	@Test void a02_jsonLd_remoteContextFetchedWhenAllowed() {
		var in = "{\"@context\":\"" + url() + "\",\"@id\":\"http://example.com/a\",\"name\":\"x\"}";
		var p = JsonLdParser.create().allowRemoteContexts().build();
		try {
			p.read(in, Object.class);
		} catch (@SuppressWarnings("unused") Exception e) {
			// Result shape is irrelevant here; only the fetch is checked.
		}
		assertTrue(hits.get() > 0);
	}

	private static final String OTHER = "http://other.example/doc.dtd";

	private static final String RDF_OPEN = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:e=\"http://example.com/\">";

	private static String rdf(String name) {
		return RDF_OPEN + "<rdf:Description rdf:about=\"http://example.com/a\"><e:name>" + name + "</e:name></rdf:Description></rdf:RDF>";
	}

	private static Object readXml(String in) throws Exception {
		return RdfParser.create().xml().build().read(in, Object.class);
	}

	@Test void a03_rdfXml_externalEntityRejected() {
		var in = "<?xml version=\"1.0\"?><!DOCTYPE rdf:RDF [<!ENTITY x SYSTEM \"" + url() + "\">]>" + rdf("&x;");
		var e = assertThrows(ParseException.class, () -> readXml(in));
		assertTrue(e.getMessage().contains("External DTD and entity declarations are not supported."), e.getMessage());
		assertEquals(0, hits.get());
	}

	@Test void a04_rdfXml_externalDtdRejected() {
		var in = "<?xml version=\"1.0\"?><!DOCTYPE rdf:RDF SYSTEM \"" + url() + "\">" + rdf("x");
		var e = assertThrows(ParseException.class, () -> readXml(in));
		assertTrue(e.getMessage().contains("External DTD and entity declarations are not supported."), e.getMessage());
		assertEquals(0, hits.get());
	}

	@Test void a05_rdfXml_externalParameterEntityAndPublicIdRejected() {
		assertThrows(ParseException.class, () -> readXml("<!DOCTYPE rdf:RDF [<!ENTITY % p SYSTEM \"" + url() + "\"> %p;]>" + rdf("x")));
		assertThrows(ParseException.class, () -> readXml("<!DOCTYPE rdf:RDF PUBLIC \"-//X//EN\" \"" + url() + "\">" + rdf("x")));
		assertEquals(0, hits.get());
	}

	@Test void a06_rdfXml_internalEntityStillParses() throws Exception {
		var in = "<?xml version=\"1.0\"?><!DOCTYPE rdf:RDF [<!ENTITY x \"value\">]>" + rdf("&x;");
		assertTrue(String.valueOf(readXml(in)).contains("value"));
	}

	@Test void a07_rdfXml_noDoctypeOrDoctypeTextInCommentOrCdataParses() throws Exception {
		assertTrue(String.valueOf(readXml(rdf("x"))).contains("x"));
		var comment = "<?xml version=\"1.0\"?><!-- <!DOCTYPE rdf:RDF SYSTEM \"" + url() + "\"> -->" + rdf("y");
		assertTrue(String.valueOf(readXml(comment)).contains("y"));
		var cdata = rdf("<![CDATA[<!DOCTYPE rdf:RDF SYSTEM \"" + url() + "\">]]>");
		assertTrue(String.valueOf(readXml(cdata)).contains("DOCTYPE"));
		assertEquals(0, hits.get());
	}

	@Test void a08_hasExternalDoctype() {
		assertFalse(RdfParserSession.hasExternalDoctype(""));
		assertFalse(RdfParserSession.hasExternalDoctype("<a/>"));
		assertFalse(RdfParserSession.hasExternalDoctype("<!DOCTYPE a>"));
		assertFalse(RdfParserSession.hasExternalDoctype("<!DOCTYPE a [<!ENTITY x \"SYSTEM\"><!-- SYSTEM --><!ELEMENT a ANY>]><a/>"));
		assertFalse(RdfParserSession.hasExternalDoctype("<a><!DOCTYPE b SYSTEM \"" + OTHER + "\"></a>"));
		assertTrue(RdfParserSession.hasExternalDoctype("\uFEFF<?xml version=\"1.0\"?>\n<!DOCTYPE a SYSTEM \"" + OTHER + "\">"));
		assertTrue(RdfParserSession.hasExternalDoctype("<!DOCTYPE a PUBLIC \"p\" \"" + OTHER + "\">"));
		assertTrue(RdfParserSession.hasExternalDoctype("<!DOCTYPE a [<!ENTITY x \"v\"><!ENTITY y SYSTEM '" + OTHER + "'>]>"));
		assertTrue(RdfParserSession.hasExternalDoctype("<!DOCTYPE a [<!ENTITY y PUBLIC 'p' '" + OTHER + "'>]>"));
	}
}
