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

import java.io.*;
import java.net.http.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;

/**
 * {@link HttpTransport} implementation backed by the JDK's built-in {@link HttpClient}.
 *
 * <p>
 * This is the <b>default</b> transport used by {@link RestClient}.  It is built into the
 * {@code juneau-rest-client} artifact, requires no external dependencies, and is always available on Java 11+.
 *
 * <p>
 * It is auto-discovered via {@link java.util.ServiceLoader} and can also be instantiated explicitly:
 *
 * <p class='bjava'>
 * 	<jv>transport</jv> = JavaHttpTransport.<jsm>builder</jsm>()
 * 		.httpClient(HttpClient.newHttpClient())
 * 		.build();
 *
 * 	<jv>client</jv> = RestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.build();
 * </p>
 *
 * <p>
 * When a pooled keep-alive connection is torn down by the server before any response bytes are received (an
 * immediate {@code EOF} surfaced by the JDK client as an {@code IOException} whose message is
 * {@code "HTTP/1.1 header parser received no bytes"}), this transport replays the request exactly once on a fresh
 * connection &mdash; but only when {@link TransportRequest#isSafeToReplay()} proves it is safe (an idempotent method
 * with an absent or repeatable body).  The JDK client already retries body-less {@code GET} requests itself, but not
 * other idempotent methods (e.g. {@code PUT}/{@code DELETE}); this backstop closes that gap while never replaying a
 * {@code POST} or any other non-idempotent request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // Not owned here; lifecycle is managed by the surrounding context
})
public final class JavaHttpTransport implements HttpTransport {

	private final HttpClient httpClient;

	JavaHttpTransport(JavaHttpTransportBuilder builder) {
		this.httpClient = builder.httpClient != null ? builder.httpClient : HttpClient.newHttpClient();
	}

	/**
	 * Returns a new builder for this transport.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static JavaHttpTransportBuilder builder() {
		return new JavaHttpTransportBuilder();
	}

	/**
	 * Returns a new instance backed by a default {@link HttpClient}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static JavaHttpTransport create() {
		return builder().build();
	}

	@Override /* HttpTransport */
	public TransportResponse execute(TransportRequest request) throws TransportException {
		try {
			return sendOnce(request);
		} catch (StaleConnectionException e) {
			// A pooled keep-alive connection was torn down by the server before any response was received.
			// Replay once on a fresh connection, but only when it is provably safe (idempotent + repeatable body).
			if (! request.isSafeToReplay())
				throw e.asTransportException();
			try {
				return sendOnce(request);
			} catch (StaleConnectionException e2) {
				throw e2.asTransportException();
			}
		}
	}

	// Performs a single HTTP exchange.  Throws StaleConnectionException (a retryable signal) when the failure is a
	// pre-response stale-connection failure; throws TransportException for every other failure.
	private TransportResponse sendOnce(TransportRequest request) throws TransportException, StaleConnectionException {
		var jdkRequest = buildJdkRequest(request);
		HttpResponse<InputStream> jdkResponse;
		try {
			jdkResponse = httpClient.send(jdkRequest, BodyHandlers.ofInputStream());
		} catch (IOException e) {
			// httpClient.send() only returns once the response headers have been read, so any IOException here
			// occurred before any response bytes were received.
			if (TransportException.isStaleConnectionFailure(e))
				throw new StaleConnectionException(e);
			throw new TransportException("HTTP transport error: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TransportException("HTTP request interrupted", e);
		}
		return buildTransportResponse(jdkResponse);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------------------------------------------------

	private static HttpRequest buildJdkRequest(TransportRequest request) {
		var builder = HttpRequest.newBuilder().uri(request.getUri()).method(request.getMethod(), buildBodyPublisher(request.getBody()));
		for (var h : request.getHeaders())
			builder.header(h.name(), h.value());
		var timeout = request.getTimeout();
		if (timeout != null)
			builder.timeout(timeout);
		return builder.build();
	}

	@SuppressWarnings({
		"java:S2095" // PipedInputStream closed by HttpClient when body publishing finishes; PipedOutputStream closed in writer thread try-with-resources
	})
	private static BodyPublisher buildBodyPublisher(TransportBody body) {
		if (body == null)
			return BodyPublishers.noBody();
		// Use a pipe so body.writeTo() streams directly to the JDK client without full in-memory buffering.
		// The writer runs on a daemon thread; the JDK client reads from the PipedInputStream on its own threads.
		// The supplier runs when the client publishes the request body (not when the HttpRequest is built).
		return BodyPublishers.ofInputStream(() -> {
			var in = new PipedInputStream();
			try {
				var out = new PipedOutputStream(in);
				var writer = new Thread(() -> {
					try (out) {
						body.writeTo(out);
					} catch (IOException e) {
						// Closing the pipe on error causes the reader to see an IOException,
						// which propagates to the JDK client as a send failure.
					}
				}, "juneau-ng-body-writer");
				writer.setDaemon(true);
				writer.start();
				return in;
			} catch (IOException e) {
				try {
					in.close();
				} catch (IOException e2) {
					e.addSuppressed(e2);
				}
				throw new UncheckedIOException(e);
			}
		});
	}

	private static TransportResponse buildTransportResponse(HttpResponse<InputStream> jdkResponse) {
		var body = jdkResponse.body();
		var builder = TransportResponse.builder()
			.statusCode(jdkResponse.statusCode())
			.body(body)
			.closeCallback(body);
		jdkResponse.headers().map().forEach((name, values) ->
			values.forEach(value -> builder.header(name, value)));
		return builder.build();
	}

	// Internal signal that a pre-response stale-connection failure occurred and may be retried once.  Carries the
	// original cause so the caller can build the user-visible TransportException if the retry is not attempted or
	// also fails.
	private static final class StaleConnectionException extends Exception {
		private static final long serialVersionUID = 1L;

		StaleConnectionException(Throwable cause) {
			super(cause);
		}

		TransportException asTransportException() {
			return new TransportException("HTTP transport error: " + getCause().getMessage(), getCause());
		}
	}
}
