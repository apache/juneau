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
import java.util.concurrent.*;

import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises {@link ResponseStatusLine#assertValue()} and {@link ResponseStatusLine#response()}, which aren't
 * reached by other end-to-end client tests.
 */
@SuppressWarnings({
	"resource" // response() returns a RestResponse whose ownership is transferred to the caller, who is responsible for closing it; the underlying client is a short-lived test fixture and Eclipse JDT's @Owning warning is by design.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ResponseStatusLine_Test {

	private static HttpServer server;
	private static ExecutorService executor;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		// Bind to (and connect via) the explicit IPv4 loopback, not the wildcard address + "localhost": a wildcard bind
		// can be handed an ephemeral port that another process already holds on 127.0.0.1 (SO_REUSEADDR), and that
		// more-specific listener then wins every "localhost" connection -- the client hangs on its response.
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		port = server.getAddress().getPort();
		// Without an explicit executor, exchanges run on HttpServer's single internal dispatch thread, which
		// is a bottleneck under -T1C parallel load. (The "server failed to respond" flake once blamed on starvation was most likely
		// the wildcard-bind port shadowing fixed by binding 127.0.0.1 above; the executor is kept as harmless.)
		executor = Executors.newCachedThreadPool();
		server.setExecutor(executor);
		server.createContext("/ok", exchange -> {
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
		if (executor != null)
			executor.shutdownNow();
	}

	private static RestResponse response() throws Exception {
		var client = RestClient.create().rootUrl("http://127.0.0.1:" + port).build();
		return client.get("/ok").run();
	}

	@Test void a01_assertValue() throws Exception {
		try (var response = response()) {
			response.getStatusLine().assertValue().asCode().is(200);
		}
	}

	@Test void a02_response_returnsOwningResponse() throws Exception {
		try (var response = response()) {
			assertSame(response, response.getStatusLine().response());
		}
	}
}
