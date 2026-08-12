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
package org.apache.juneau.rest.client.apachehttpclient45;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.juneau.rest.client.*;

/**
 * {@link HttpTransport} implementation backed by Apache HttpClient 4.5.
 *
 * <p>
 * This transport is auto-discovered via {@link java.util.ServiceLoader} when
 * {@code org.apache.httpcomponents:httpclient} is on the classpath.  You can also instantiate it explicitly:
 *
 * <p class='bjava'>
 * 	<jv>transport</jv> = ApacheHc45Transport.<jsm>builder</jsm>()
 * 		.httpClient(HttpClients.createDefault())
 * 		.build();
 *
 * 	<jv>client</jv> = RestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.build();
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // httpClient is owned by this transport and closed in close()
})
public final class ApacheHc45Transport implements HttpTransport {

	private final CloseableHttpClient httpClient;

	ApacheHc45Transport(ApacheHc45TransportBuilder builder) {
		this.httpClient = builder.httpClient != null ? builder.httpClient : createDefaultHttpClient();
	}

	private static CloseableHttpClient createDefaultHttpClient() {
		return HttpClients.custom().addInterceptorLast(new Hc45RedirectCredentialGuard()).build();
	}

	/**
	 * Returns a new builder for this transport.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static ApacheHc45TransportBuilder builder() {
		return new ApacheHc45TransportBuilder();
	}

	/**
	 * Returns a new instance backed by a default {@link CloseableHttpClient}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ApacheHc45Transport create() {
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
		var hcRequest = buildHcRequest(request);
		CloseableHttpResponse hcResponse;
		try {
			hcResponse = httpClient.execute(hcRequest);
		} catch (IOException e) {
			// httpClient.execute() only returns once response headers have been read, so any IOException here
			// occurred before any response bytes were received.
			if (isStaleConnectionFailure(e))
				throw new StaleConnectionException(e);
			throw new TransportException("HTTP transport error: " + e.getMessage(), e);
		}
		try {
			return buildTransportResponse(hcResponse);
		} catch (TransportException | RuntimeException e) {
			// If response wiring fails the closeCallback never runs, so close here to release the leased connection.
			closeQuietly(hcResponse);
			throw e;
		}
	}

	// A failure is a stale-connection (pre-response) failure when the server closed the pooled connection before
	// sending any response.  Apache HttpClient 4.5 surfaces this as a NoHttpResponseException ("failed to respond")
	// or a connection-reset SocketException.  HttpClient 4.5's default retry handler does not retry these for
	// entity-enclosing idempotent methods (e.g. PUT) or for POST, so the transport applies its own idempotency-safe
	// retry here.
	private static boolean isStaleConnectionFailure(IOException e) {
		return e instanceof NoHttpResponseException
			|| (e instanceof SocketException && e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("reset"));
	}

	private static void closeQuietly(CloseableHttpResponse hcResponse) {
		try {
			hcResponse.close();
		} catch (IOException e) {
			// Best-effort cleanup on an already-failing path; nothing more can be done.
		}
	}

	@Override /* Closeable */
	public void close() throws IOException {
		httpClient.close();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------------------------------------------------

	private static HttpUriRequest buildHcRequest(TransportRequest request) throws TransportException {
		var builder = RequestBuilder.create(request.getMethod()).setUri(request.getUri());
		for (var h : request.getHeaders())
			builder.addHeader(h.name(), h.value());
		var body = request.getBody();
		if (body != null)
			builder.setEntity(new TransportBodyEntity(body));
		try {
			return builder.build();
		} catch (Exception e) {
			throw new TransportException("Failed to build HTTP request: " + e.getMessage(), e);
		}
	}

	private static TransportResponse buildTransportResponse(CloseableHttpResponse hcResponse) throws TransportException {
		var statusLine = hcResponse.getStatusLine();
		var builder = TransportResponse.builder()
			.statusCode(statusLine.getStatusCode())
			.reasonPhrase(statusLine.getReasonPhrase())
			.closeCallback(hcResponse);
		for (var h : hcResponse.getAllHeaders())
			builder.header(h.getName(), h.getValue());
		var entity = hcResponse.getEntity();
		if (entity != null) {
			try {
				builder.body(entity.getContent());
			} catch (IOException e) {
				throw new TransportException("Failed to read response body: " + e.getMessage(), e);
			}
		}
		return builder.build();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// TransportBodyEntity — bridges TransportBody to Apache HttpEntity
	// -----------------------------------------------------------------------------------------------------------------

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

	/**
	 * Bridges a {@link TransportBody} to Apache HttpClient's {@link AbstractHttpEntity}.
	 *
	 * <p>Package-protected (rather than {@code private}) so {@code ApacheHc45Transport_TransportBodyEntity_Test}
	 * (same package) can construct it directly and assert its {@code getContent()} contract.
	 */
	static final class TransportBodyEntity extends AbstractHttpEntity {

		private final TransportBody body;

		TransportBodyEntity(TransportBody body) {
			this.body = body;
			var ct = body.getContentType();
			if (ct != null)
				setContentType(ct);
		}

		@Override /* HttpEntity */
		public boolean isRepeatable() {
			return body.isRepeatable();
		}

		@Override /* HttpEntity */
		public long getContentLength() {
			return body.getContentLength();
		}

		@Override /* HttpEntity */
		public InputStream getContent() throws UnsupportedOperationException {
			throw new UnsupportedOperationException("Use writeTo(OutputStream) instead");
		}

		@Override /* HttpEntity */
		public void writeTo(OutputStream out) throws IOException {
			body.writeTo(out);
		}

		@Override /* HttpEntity */
		public boolean isStreaming() {
			return !body.isRepeatable(); // HTT — only called by HC4.5 retry/redirect machinery, not in normal single-request flow
		}
	}
}
