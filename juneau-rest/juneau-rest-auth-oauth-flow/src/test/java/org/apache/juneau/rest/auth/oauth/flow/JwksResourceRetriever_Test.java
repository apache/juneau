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
package org.apache.juneau.rest.auth.oauth.flow;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.Arrays;

import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests for {@link JwksResourceRetriever}.
 *
 * @since 10.0.0
 */
class JwksResourceRetriever_Test {

	private HttpServer server;

	@BeforeEach
	void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/ok", ex -> {
			var b = "{\"keys\":[]}".getBytes();
			ex.sendResponseHeaders(200, b.length);
			try (var os = ex.getResponseBody()) { os.write(b); }
		});
		server.createContext("/redirect", ex -> {
			ex.getResponseHeaders().add("Location", "/ok");
			ex.sendResponseHeaders(302, -1);
			ex.close();
		});
		server.createContext("/big", ex -> {
			var b = new byte[JwksResourceRetriever.MAX_BYTES + 1000];
			Arrays.fill(b, (byte)'x');
			ex.sendResponseHeaders(200, b.length);
			try (var os = ex.getResponseBody()) { os.write(b); } catch (IOException e) { /* client closed */ }
		});
		server.start();
	}

	@AfterEach
	void stop() {
		server.stop(0);
	}

	private URL url(String path) throws IOException {
		return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path).toURL();
	}

	@Test void a01_ok() throws Exception {
		assertEquals("{\"keys\":[]}", new JwksResourceRetriever().retrieveResource(url("/ok")).getContent());
	}

	@Test void a02_redirectNotFollowed() throws IOException {
		var r = new JwksResourceRetriever();
		var u = url("/redirect");
		assertThrows(IOException.class, () -> r.retrieveResource(u));
	}

	@Test void a03_oversizedResponseRejected() throws IOException {
		var r = new JwksResourceRetriever();
		var u = url("/big");
		assertThrows(IOException.class, () -> r.retrieveResource(u));
	}

	@Test void a04_plainHttpNonLoopbackRejected() throws IOException {
		var r = new JwksResourceRetriever();
		var u = URI.create("http://other.example/jwks").toURL();
		assertThrows(IOException.class, () -> r.retrieveResource(u));
	}
}
