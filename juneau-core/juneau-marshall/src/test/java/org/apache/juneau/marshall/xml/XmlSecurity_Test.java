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
package org.apache.juneau.marshall.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.concurrent.atomic.*;

import javax.xml.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import com.sun.net.httpserver.*;

/**
 * Exercises external-resource and expansion attacks through Juneau's configured StAX reader.
 */
class XmlSecurity_Test extends TestBase {

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
		server.start();
		external = "http://127.0.0.1:" + server.getAddress().getPort() + "/external";
	}

	@AfterEach void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private static String externalDocument(String vector, String uri) {
		return switch (vector) {
			case "general" -> "<!DOCTYPE A [<!ENTITY ext SYSTEM '" + uri + "'>]><A>&ext;</A>";
			case "parameter" -> "<!DOCTYPE A [<!ENTITY % ext SYSTEM '" + uri + "'>%ext;]><A>safe</A>";
			case "systemDtd" -> "<!DOCTYPE A SYSTEM '" + uri + "'><A>safe</A>";
			case "publicDtd" -> "<!DOCTYPE A PUBLIC '-//Juneau//DTD XXE Test//EN' '" + uri + "'><A>safe</A>";
			default -> throw new IllegalArgumentException(vector);
		};
	}

	private static String expansionDocument() {
		// 100,000 leaf expansions exceed the JDK 17/21 default count, but stay small if protection regresses.
		var dtd = new StringBuilder("<!DOCTYPE A [<!ENTITY e0 'x'>");
		for (int i = 1; i <= 5; i++)
			dtd.append("<!ENTITY e").append(i).append(" '").append(("&e" + (i - 1) + ";").repeat(10)).append("'>");
		return dtd.append("]><A>&e5;</A>").toString();
	}

	/**
	 * Declines every request, leaving the secure fallback responsible for blocking access.
	 */
	public static class DecliningResolver implements XMLResolver {
		@Override public Object resolveEntity(String publicId, String systemId, String baseUri, String namespace) {
			return null;
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"general", "parameter", "systemDtd", "publicDtd"})
	void externalReferencesNeverFetch(String vector) throws Exception {
		for (var parser : new XmlParser[] {XmlParser.DEFAULT, XmlParser.create().resolver(DecliningResolver.class).build()}) {
			assertEquals("safe", parser.read("<A>safe</A>", String.class));
			try {
				var result = parser.read(externalDocument(vector, external), String.class);
				assertFalse(result != null && result.contains("FETCHED"));
			} catch (ParseException expected) {
				// Rejection and empty resolution are both safe, but neither may fetch the resource first.
			}
			assertEquals(0, requests.get(), vector + " fetched an external resource");
		}
	}

	@Test void fileEntityDoesNotDiscloseContents(@TempDir Path directory) throws Exception {
		var secret = directory.resolve("secret.txt");
		Files.writeString(secret, "PRIVATE_FILE_MARKER");
		assertEquals("safe", XmlParser.DEFAULT.read("<A>safe</A>", String.class));
		try {
			var result = XmlParser.DEFAULT.read(externalDocument("general", secret.toUri().toString()), String.class);
			assertFalse(result != null && result.contains("PRIVATE_FILE_MARKER"));
		} catch (ParseException expected) {
			for (Throwable cause = expected; cause != null; cause = cause.getCause())
				assertFalse(cause.toString().contains("PRIVATE_FILE_MARKER"));
		}
	}

	@Test
	@Timeout(10)
	void exponentialEntityExpansionIsRejected() {
		assertThrows(ParseException.class, () -> XmlParser.DEFAULT.read(expansionDocument(), String.class));
	}
}
