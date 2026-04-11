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
package org.apache.juneau.ng.rest.client.javahttpclient;

import java.io.*;
import java.net.http.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;

import org.apache.juneau.ng.rest.client.*;

/**
 * {@link HttpTransport} implementation backed by the JDK's built-in {@link java.net.http.HttpClient}.
 *
 * <p>
 * No external dependencies are required — this transport is always available on Java 11+.
 * It is auto-discovered via {@link java.util.ServiceLoader} and can also be instantiated explicitly:
 *
 * <p class='bjava'>
 * 	<jv>transport</jv> = JavaHttpTransport.<jsm>builder</jsm>()
 * 		.httpClient(HttpClient.newHttpClient())
 * 		.build();
 *
 * 	<jv>client</jv> = NgRestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.build();
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class JavaHttpTransport implements HttpTransport {

	private final java.net.http.HttpClient httpClient;

	JavaHttpTransport(JavaHttpTransportBuilder builder) {
		this.httpClient = builder.httpClient != null ? builder.httpClient : java.net.http.HttpClient.newHttpClient();
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
	 * Returns a new instance backed by a default {@link java.net.http.HttpClient}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static JavaHttpTransport create() {
		return builder().build();
	}

	@Override /* HttpTransport */
	public TransportResponse execute(TransportRequest request) throws TransportException {
		HttpRequest jdkRequest;
		try {
			jdkRequest = buildJdkRequest(request);
		} catch (IOException e) {
			throw new TransportException("Failed to build HTTP request: " + e.getMessage(), e);
		}
		HttpResponse<InputStream> jdkResponse;
		try {
			jdkResponse = httpClient.send(jdkRequest, BodyHandlers.ofInputStream());
		} catch (IOException e) {
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

	private static HttpRequest buildJdkRequest(TransportRequest request) throws IOException {
		var builder = HttpRequest.newBuilder().uri(request.getUri()).method(request.getMethod(), buildBodyPublisher(request.getBody()));
		for (var h : request.getHeaders())
			builder.header(h.name(), h.value());
		return builder.build();
	}

	@SuppressWarnings({
		"java:S2095" // PipedInputStream closed by HttpClient when body publishing finishes; PipedOutputStream closed in writer thread try-with-resources
	})
	private static BodyPublisher buildBodyPublisher(TransportBody body) throws IOException {
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
		var builder = TransportResponse.builder()
			.statusCode(jdkResponse.statusCode())
			.body(jdkResponse.body());
		jdkResponse.headers().map().forEach((name, values) ->
			values.forEach(value -> builder.header(name, value)));
		return builder.build();
	}
}
