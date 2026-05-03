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
package org.apache.juneau.ng.rest.client.jetty;

import java.io.*;
import java.util.concurrent.*;

import org.eclipse.jetty.client.*;
import org.apache.juneau.ng.rest.client.*;

/**
 * {@link HttpTransport} implementation backed by Jetty 12 {@link HttpClient}.
 *
 * <p>
 * This transport is auto-discovered via {@link java.util.ServiceLoader} when
 * {@code org.eclipse.jetty:jetty-client} is on the classpath.  You can also instantiate it explicitly:
 *
 * <p class='bjava'>
 * 	<jv>transport</jv> = JettyHttpTransport.<jsm>builder</jsm>()
 * 		.httpClient(<jk>new</jk> HttpClient())
 * 		.build();
 *
 * 	<jv>client</jv> = NgRestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.build();
 * </p>
 *
 * <p>
 * The Jetty {@link HttpClient} is started automatically when the transport is created and stopped when
 * {@link #close()} is called.  The request body is streamed via {@link OutputStreamRequestContent} using
 * a background thread, avoiding full in-memory buffering.  The response header timeout defaults to 30 seconds
 * and is configurable via {@link JettyHttpTransportBuilder#responseTimeoutMs(long)}.
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
public final class JettyHttpTransport implements HttpTransport {

	private final HttpClient httpClient;
	private final long responseTimeoutMs;

	JettyHttpTransport(JettyHttpTransportBuilder builder) throws Exception {
		this.httpClient = builder.httpClient != null ? builder.httpClient : new HttpClient();
		this.responseTimeoutMs = builder.responseTimeoutMs;
		if (!this.httpClient.isStarted())
			this.httpClient.start();
	}

	/**
	 * Returns a new builder for this transport.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static JettyHttpTransportBuilder builder() {
		return new JettyHttpTransportBuilder();
	}

	/**
	 * Returns a new instance backed by a default {@link HttpClient}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 * @throws Exception If the Jetty {@link HttpClient} cannot be started.
	 */
	public static JettyHttpTransport create() throws Exception {
		return builder().build();
	}

	@Override /* HttpTransport */
	public TransportResponse execute(TransportRequest request) throws TransportException {
		var jettyRequest = buildJettyRequest(request);
		var listener = new InputStreamResponseListener();
		jettyRequest.send(listener);
		Response jettyResponse;
		try {
			jettyResponse = responseTimeoutMs > 0
				? listener.get(responseTimeoutMs, TimeUnit.MILLISECONDS)
				: listener.get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TransportException("HTTP request interrupted", e);
		} catch (TimeoutException e) {
			throw new TransportException("HTTP request timed out", e);
		} catch (ExecutionException e) {
			throw new TransportException("HTTP transport error: " + e.getCause().getMessage(), e.getCause());
		}
		return buildTransportResponse(jettyResponse, listener.getInputStream());
	}

	@Override /* Closeable */
	public void close() throws IOException {
		try {
			httpClient.stop();
		} catch (Exception e) {
			throw new IOException("Failed to stop Jetty HttpClient: " + e.getMessage(), e);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------------------------------------------------

	private Request buildJettyRequest(TransportRequest request) {
		var jettyRequest = httpClient.newRequest(request.getUri()).method(request.getMethod());
		for (var h : request.getHeaders())
			jettyRequest.headers(fields -> fields.add(h.name(), h.value()));
		var body = request.getBody();
		if (body != null)
			jettyRequest.body(buildRequestContent(body));
		return jettyRequest;
	}

	private static OutputStreamRequestContent buildRequestContent(TransportBody body) {
		var ct = body.getContentType();
		var contentType = ct != null ? ct : "application/octet-stream";
		var content = new OutputStreamRequestContent(contentType);
		// Write body to the content's OutputStream on a background thread so Jetty can stream it concurrently.
		var writer = new Thread(() -> {
			try (var out = content.getOutputStream()) {
				body.writeTo(out);
			} catch (IOException e) {
				// Closing the stream on error signals Jetty to abort the request.
			}
		}, "juneau-ng-body-writer");
		writer.setDaemon(true);
		writer.start();
		return content;
	}

	private static TransportResponse buildTransportResponse(Response jettyResponse, InputStream bodyStream) {
		var builder = TransportResponse.builder()
			.statusCode(jettyResponse.getStatus())
			.reasonPhrase(jettyResponse.getReason())
			.body(bodyStream);
		jettyResponse.getHeaders().forEach(field -> builder.header(field.getName(), field.getValue()));
		return builder.build();
	}
}
