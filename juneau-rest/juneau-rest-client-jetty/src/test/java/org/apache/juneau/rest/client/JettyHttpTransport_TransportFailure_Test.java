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
import java.util.concurrent.atomic.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.client.jetty.*;
import org.eclipse.jetty.client.*;
import org.junit.jupiter.api.*;

/**
 * Exercises {@link JettyHttpTransport} failure paths that {@code JettyHttpTransport_Test} and
 * {@code JettyHttpTransport_StaleConnectionRetry_Test} don't reach: thread interruption and timeout while
 * awaiting response headers, a non-stale-connection {@code ExecutionException} (e.g. connection refused),
 * a request-body writer failure, and an {@link HttpClient} that fails to stop.
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
class JettyHttpTransport_TransportFailure_Test {

	// -----------------------------------------------------------------------------------------------------------------
	// Test double: accepts connections but never writes a response, simulating a hung server.
	// -----------------------------------------------------------------------------------------------------------------

	private static final class NeverRespondingServer implements AutoCloseable {

		private final ServerSocket serverSocket;

		NeverRespondingServer() throws IOException {
			this.serverSocket = new ServerSocket(0);
			var acceptThread = new Thread(this::acceptLoop, "jetty-never-responding-server");
			acceptThread.setDaemon(true);
			acceptThread.start();
		}

		String rootUrl() {
			return "http://localhost:" + serverSocket.getLocalPort();
		}

		private void acceptLoop() {
			while (! serverSocket.isClosed()) {
				try {
					// Accept and hold the connection open indefinitely without ever responding; do not close it
					// in this loop, since closing would surface as a stale-connection failure instead.
					serverSocket.accept();
				} catch (IOException e) {
					return; // Server socket closed during shutdown.
				}
			}
		}

		@Override
		public void close() throws IOException {
			serverSocket.close();
		}
	}

	// =================================================================================================================
	// A: InterruptedException while awaiting response headers (JettyHttpTransport.java lines 125-128)
	// =================================================================================================================

	@Test
	void a01_interruptedWhileAwaitingResponse_throwsTransportException() throws Exception {
		try (var server = new NeverRespondingServer()) {
			var transport = JettyHttpTransport.builder().responseTimeoutMs(0).build();
			var request = TransportRequest.builder().method("GET").uri(URI.create(server.rootUrl() + "/x")).build();
			var caught = new AtomicReference<Throwable>();
			var thread = new Thread(() -> {
				try {
					transport.execute(request);
				} catch (Throwable t) {
					caught.set(t);
				}
			}, "interrupt-target");
			thread.start();
			// Give the request time to be sent and the thread to start blocking on listener.get(...).
			Thread.sleep(200);
			thread.interrupt();
			thread.join(5000);
			assertFalse(thread.isAlive(), "target thread did not terminate after interrupt");
			assertInstanceOf(TransportException.class, caught.get());
			assertTrue(caught.get().getMessage().contains("interrupted"), caught.get().getMessage());
		}
	}

	// =================================================================================================================
	// B: TimeoutException while awaiting response headers (JettyHttpTransport.java lines 129-131)
	// =================================================================================================================

	@Test
	void b01_responseTimeout_throwsTransportException() throws Exception {
		try (var server = new NeverRespondingServer()) {
			var transport = JettyHttpTransport.builder().responseTimeoutMs(200).build();
			var request = TransportRequest.builder().method("GET").uri(URI.create(server.rootUrl() + "/x")).build();
			var ex = assertThrows(TransportException.class, () -> transport.execute(request));
			assertTrue(ex.getMessage().contains("timed out"), ex.getMessage());
		}
	}

	// =================================================================================================================
	// C: ExecutionException with a non-stale-connection cause — connection refused (lines 135/137, and the
	//    EOFException==false / ClosedChannelException==false branch pair of isStaleConnectionFailure at line 147)
	// =================================================================================================================

	@Test
	void c01_connectionRefused_throwsGenericTransportException() throws Exception {
		int freePort;
		try (var probe = new ServerSocket(0)) {
			freePort = probe.getLocalPort();
		}
		// Nothing is listening on freePort now that the probe socket above has been closed.
		var transport = JettyHttpTransport.create();
		var request = TransportRequest.builder().method("GET").uri(URI.create("http://localhost:" + freePort + "/x")).build();
		var ex = assertThrows(TransportException.class, () -> transport.execute(request));
		assertTrue(ex.getMessage().contains("HTTP transport error"), ex.getMessage());
	}

	// =================================================================================================================
	// D: request-body writer failure (JettyHttpTransport.java line 191)
	// =================================================================================================================

	@Test
	void d01_bodyWriteFails_requestAborted() throws Exception {
		var serverSocket = new ServerSocket(0);
		var acceptThread = new Thread(() -> {
			try (var socket = serverSocket.accept()) {
				// Read whatever arrives (partial request) and never respond; the client-side write failure
				// is what we're exercising, not any particular server behavior.
				socket.getInputStream().readAllBytes();
			} catch (IOException e) {
				// Expected once the client aborts the connection after the body-write failure.
			}
		}, "jetty-body-write-fail-server");
		acceptThread.setDaemon(true);
		acceptThread.start();
		try {
			// Short response timeout: the server never responds, so without this the test would otherwise
			// block for the 30s default before the write failure's TransportException is even reachable.
			var transport = JettyHttpTransport.builder().responseTimeoutMs(500).build();
			HttpBody failingBody = new HttpBody() {
				@Override public String getContentType() { return "text/plain"; }
				@Override public void writeTo(OutputStream out) throws IOException { throw new IOException("simulated write failure"); }
			};
			var request = TransportRequest.builder()
				.method("POST")
				.uri(URI.create("http://localhost:" + serverSocket.getLocalPort() + "/x"))
				.body(TransportBody.of(failingBody))
				.build();
			// The background writer thread hits the IOException catch (line 191) and closes the content stream,
			// which surfaces to the caller as some flavor of TransportException.
			assertThrows(TransportException.class, () -> transport.execute(request));
		} finally {
			serverSocket.close();
		}
	}

	// =================================================================================================================
	// E: close()'s catch(Exception) when the underlying HttpClient fails to stop (lines 164-165)
	// =================================================================================================================

	private static final class ExplodingOnStopHttpClient extends HttpClient {
		@Override
		protected void doStop() throws Exception {
			throw new Exception("simulated stop failure");
		}
	}

	@Test
	void e01_httpClientStopFails_closeThrowsIOException() throws Exception {
		var transport = JettyHttpTransport.builder().httpClient(new ExplodingOnStopHttpClient()).build();
		var ex = assertThrows(IOException.class, transport::close);
		assertTrue(ex.getMessage().contains("Failed to stop Jetty HttpClient"), ex.getMessage());
	}
}
