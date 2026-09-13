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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.concurrent.atomic.*;

import javax.xml.stream.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the independent StAX path used to validate serialized XML in integration tests.
 */
class XmlValidatorParser_Security_Test extends TestBase {

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

	@Test void ordinaryXmlStillValidates() throws Exception {
		new XmlValidatorParser().validate(new StringReader("<A>safe &amp; sound</A>"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"general", "parameter", "systemDtd", "publicDtd"})
	void externalReferencesNeverFetch(String vector) throws Exception {
		try {
			new XmlValidatorParser().validate(new StringReader(externalDocument(vector, external)));
		} catch (XMLStreamException expected) {
			// Empty resolution or rejection is allowed, but fetching before rejection is not.
		}
		assertEquals(0, requests.get(), vector + " fetched an external resource");
	}

	@Test void fileEntityIsNotExpanded(@TempDir Path directory) throws Exception {
		var secret = directory.resolve("secret.txt");
		Files.writeString(secret, "PRIVATE_FILE_MARKER");
		try {
			var reader = new XmlValidatorParser().getStaxReader(new StringReader(externalDocument("general", secret.toUri().toString())));
			try {
				while (reader.hasNext()) {
					reader.next();
					if (reader.hasText())
						assertFalse(reader.getText().contains("PRIVATE_FILE_MARKER"));
				}
			} finally {
				reader.close();
			}
		} catch (XMLStreamException expected) {
			for (Throwable cause = expected; cause != null; cause = cause.getCause())
				assertFalse(cause.toString().contains("PRIVATE_FILE_MARKER"));
		}
	}

	@Test
	@Timeout(10)
	void exponentialEntityExpansionIsRejected() {
		assertThrows(XMLStreamException.class, () -> new XmlValidatorParser().validate(new StringReader(expansionDocument())));
	}
}
