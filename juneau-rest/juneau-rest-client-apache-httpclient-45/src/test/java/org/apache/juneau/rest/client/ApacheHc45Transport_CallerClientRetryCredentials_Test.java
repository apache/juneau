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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.http.impl.client.*;
import org.apache.juneau.rest.client.apachehttpclient45.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that a caller-supplied {@link CloseableHttpClient} does not leak caller credential headers to a different
 * origin when HttpClient transparently retries a request after a pre-response connection failure.
 *
 * <p>
 * HttpClient's {@code RetryExec} restores the headers it captured before the protocol stage ran, i.e. the caller's
 * original credential headers, before every retry of a hop, then runs the protocol stage again on the same request
 * object.  The guard must therefore strip them again on each such pass, not only the first time it sees a request.
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ApacheHc45Transport_CallerClientRetryCredentials_Test {

	private static final Map<String,String> ALL = Map.of("authorization", "Bearer secret-token", "cookie", "session=abc123", "x-api-key", "k-1", "x-trace", "t-1");
	private static final Map<String,String> STRIPPED = Map.of("x-trace", "t-1");

	/** Scripted server: {@code script} gets the request path and returns a raw response, or {@code null} to drop the connection. */
	private static final class Server implements AutoCloseable {
		private final ServerSocket socket;
		/** Credential-bearing headers of every request received, as "path -> headers". */
		final List<String> log = new CopyOnWriteArrayList<>();
		final List<Map<String,String>> headersSeen = new CopyOnWriteArrayList<>();
		private final Function<String,String> script;

		Server(Function<String,String> script) throws IOException {
			this.script = script;
			socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
			var t = new Thread(this::loop, "hc45-scripted-server");
			t.setDaemon(true);
			t.start();
		}

		int port() {
			return socket.getLocalPort();
		}

		private void loop() {
			while (! socket.isClosed()) {
				try (var s = socket.accept()) {
					var in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.ISO_8859_1));
					var path = in.readLine().split(" ")[1];
					var headers = new TreeMap<String,String>();
					String line;
					while ((line = in.readLine()) != null && ! line.isEmpty()) {
						var i = line.indexOf(':');
						var name = line.substring(0, i).trim().toLowerCase(Locale.ROOT);
						if (name.equals("authorization") || name.equals("cookie") || name.equals("x-api-key") || name.equals("x-trace"))
							headers.put(name, line.substring(i + 1).trim());
					}
					log.add(path);
					headersSeen.add(headers);
					var response = script.apply(path);
					if (response != null)
						s.getOutputStream().write(response.getBytes(StandardCharsets.ISO_8859_1));
				} catch (IOException | RuntimeException e) {
					// Socket closed or reset; fall through.
				}
			}
		}

		@Override
		public void close() throws IOException {
			socket.close();
		}
	}

	private static String redirect(String location) {
		return "HTTP/1.1 302 Found\r\nLocation: " + location + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
	}

	private static String ok() {
		return "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nOK";
	}

	private static void run(int portA, String path) throws Exception {
		run(portA, path, HttpClients.createDefault());
	}

	private static void run(int portA, String path, CloseableHttpClient callerClient) throws Exception {
		var transport = ApacheHc45Transport.builder().httpClient(callerClient).build();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build();
				var response = client.get(path)
					.header("Authorization", "Bearer secret-token")
					.header("Cookie", "session=abc123")
					.header("X-API-Key", "k-1")
					.header("X-Trace", "t-1")
					.run()) {
			assertEquals(200, response.getStatusCode());
		}
	}

	@Test
	void a01_crossOriginRedirect_retriedAfterDroppedConnection_credentialsStayStripped() throws Exception {
		var drops = new ConcurrentLinkedQueue<>(List.of("drop"));
		try (var b = new Server(path -> drops.poll() != null ? null : ok());
				var a = new Server(path -> redirect("http://127.0.0.1:" + b.port() + "/echo"))) {
			run(a.port(), "/start");
			assertEquals(List.of("/start"), a.log, "HttpClient's own retry (not a transport-level replay) must be what re-sent the request");
			assertEquals(List.of(STRIPPED, STRIPPED), b.headersSeen, "dropped attempt and retry must both be stripped: " + b.headersSeen);
		}
	}

	@Test
	void a02_originBackToFirstOrigin_retriedAfterDroppedConnection_credentialsRestoredOnEveryAttempt() throws Exception {
		var drops = new ConcurrentLinkedQueue<>(List.of("drop"));
		var portA = new int[1];
		var portB = new int[1];
		try (var a = new Server(path -> path.equals("/start") ? redirect("http://127.0.0.1:" + portB[0] + "/bounce") : drops.poll() != null ? null : ok());
				var b = new Server(path -> redirect("http://127.0.0.1:" + portA[0] + "/echo"))) {
			portB[0] = b.port();
			portA[0] = a.port();
			run(a.port(), "/start");
			// A saw: /start, then /echo twice (dropped, then retried).  The hop back at the first origin gets the caller's credentials
			// back, and the retry must restore them again rather than lose them.
			assertEquals(List.of("/start", "/echo", "/echo"), a.log);
			assertEquals(List.of(ALL, ALL, ALL), a.headersSeen);
			assertEquals(List.of(STRIPPED), b.headersSeen);
		}
	}

	@Test
	void a03_sameOriginRedirect_retriedAfterDroppedConnection_credentialsStillForwarded() throws Exception {
		var drops = new ConcurrentLinkedQueue<>(List.of("drop"));
		try (var a = new Server(path -> path.equals("/start") ? redirect("/echo") : drops.poll() != null ? null : ok())) {
			run(a.port(), "/start");
			assertEquals(List.of(ALL, ALL, ALL), a.headersSeen);
		}
	}

	@Test
	void a04_crossOriginRedirect_retriedAfter503_credentialsStayStripped() throws Exception {
		// ServiceUnavailableRetryExec restores the caller's headers the same way RetryExec does.
		var unavailable = new ConcurrentLinkedQueue<>(List.of("503"));
		try (var b = new Server(path -> unavailable.poll() != null ? "HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n" : ok());
				var a = new Server(path -> redirect("http://127.0.0.1:" + b.port() + "/echo"))) {
			var callerClient = HttpClients.custom().setServiceUnavailableRetryStrategy(new DefaultServiceUnavailableRetryStrategy(1, 1)).build();
			run(a.port(), "/start", callerClient);
			assertEquals(List.of(STRIPPED, STRIPPED), b.headersSeen, "503 attempt and retry must both be stripped: " + b.headersSeen);
		}
	}
}
