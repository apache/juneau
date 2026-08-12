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
package org.apache.juneau.rest.client.classic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the {@code Map}-based {@code *SessionOptions{Header,Query}(Map)} convenience overloads' empty-map
 * short-circuit branches, and the {@code serializer(Class)} null-clearing branch -- small, previously-uncovered
 * guard clauses on {@link RestRequest}. Verified against a real local server (rather than inspecting
 * {@code RestRequest} internals) so the assertions reflect what's actually transmitted on the wire.
 */
@SuppressWarnings({
	"resource" // req() returns a RestRequest whose ownership is transferred to the caller, who is responsible for closing it; Eclipse JDT's @Owning warning is by design.
})
class RestRequest_SessionOptions_Coverage_Test {

	private static HttpServer server;
	private static int port;
	private static volatile String lastHeader;
	private static volatile String lastQuery;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		server.createContext("/x", exchange -> {
			lastHeader = exchange.getRequestHeaders().getFirst("x-juneau-serializer-options");
			if (lastHeader == null)
				lastHeader = exchange.getRequestHeaders().getFirst("x-juneau-parser-options");
			lastQuery = exchange.getRequestURI().getQuery();
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private static RestRequest req() throws RestCallException {
		return RestClient.create().build().get("http://localhost:" + port + "/x");
	}

	@Test void a01_serializerSessionOptionsHeader_emptyMap_returnsThisWithoutSettingHeader() throws Exception {
		try (var r = req()) {
			assertSame(r, r.serializerSessionOptionsHeader(Map.of()));
			try (var res = r.run()) {
				assertNull(lastHeader);
			}
		}
	}

	@Test void a02_serializerSessionOptionsHeader_nonEmptyMap_setsHeader() throws Exception {
		try (var r = req().serializerSessionOptionsHeader(Map.of("sortCollections", true)); var res = r.run()) {
			assertNotNull(lastHeader);
		}
	}

	@Test void a03_parserSessionOptionsHeader_emptyMap_returnsThisWithoutSettingHeader() throws Exception {
		try (var r = req()) {
			assertSame(r, r.parserSessionOptionsHeader(Map.of()));
			try (var res = r.run()) {
				assertNull(lastHeader);
			}
		}
	}

	@Test void a04_parserSessionOptionsHeader_nonEmptyMap_setsHeader() throws Exception {
		try (var r = req().parserSessionOptionsHeader(Map.of("strict", true)); var res = r.run()) {
			assertNotNull(lastHeader);
		}
	}

	@Test void b01_serializerSessionOptionsQuery_emptyMap_returnsThisWithoutSettingQuery() throws Exception {
		try (var r = req()) {
			assertSame(r, r.serializerSessionOptionsQuery(Map.of()));
			try (var res = r.run()) {
				assertTrue(lastQuery == null || !lastQuery.contains("juneauSerializerOptions"));
			}
		}
	}

	@Test void b02_serializerSessionOptionsQuery_nonEmptyMap_setsQuery() throws Exception {
		try (var r = req().serializerSessionOptionsQuery(Map.of("sortCollections", true)); var res = r.run()) {
			assertTrue(lastQuery != null && lastQuery.contains("juneauSerializerOptions"));
		}
	}

	@Test void b03_parserSessionOptionsQuery_emptyMap_returnsThisWithoutSettingQuery() throws Exception {
		try (var r = req()) {
			assertSame(r, r.parserSessionOptionsQuery(Map.of()));
			try (var res = r.run()) {
				assertTrue(lastQuery == null || !lastQuery.contains("juneauParserOptions"));
			}
		}
	}

	@Test void b04_parserSessionOptionsQuery_nonEmptyMap_setsQuery() throws Exception {
		try (var r = req().parserSessionOptionsQuery(Map.of("strict", true)); var res = r.run()) {
			assertTrue(lastQuery != null && lastQuery.contains("juneauParserOptions"));
		}
	}

	@Test void c01_serializerClass_null_clearsOverride() throws Exception {
		// No public getter to observe the cleared field directly, but a null Class argument must not throw and
		// must still produce a normal, successful call -- exercises the ternary's null branch in serializer(Class).
		try (var r = req().serializer(JsonSerializer.class).serializer((Class<? extends org.apache.juneau.marshall.serializer.Serializer>)null);
				var res = r.run()) {
			assertEquals(200, res.getStatusCode());
		}
	}
}
