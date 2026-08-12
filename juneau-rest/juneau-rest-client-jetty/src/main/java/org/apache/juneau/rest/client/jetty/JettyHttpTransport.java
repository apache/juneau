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
package org.apache.juneau.rest.client.jetty;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.channels.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.client.*;
import org.eclipse.jetty.client.*;

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
 * 	<jv>client</jv> = RestClient.<jsm>builder</jsm>()
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
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack.
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
	"resource" // Transport methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public final class JettyHttpTransport implements HttpTransport {

	private final HttpClient httpClient;
	private final long responseTimeoutMs;

	JettyHttpTransport(JettyHttpTransportBuilder builder) throws Exception {
		this.httpClient = builder.httpClient != null ? builder.httpClient : new CredentialGuardingHttpClient();
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
			abortQuietly(listener);
			throw new TransportException("HTTP request interrupted", e);
		} catch (TimeoutException e) {
			abortQuietly(listener);
			throw new TransportException("HTTP request timed out", e);
		} catch (ExecutionException e) {
			abortQuietly(listener);
			var cause = e.getCause();
			if (isStaleConnectionFailure(cause))
				throw new StaleConnectionException(cause);
			throw new TransportException("HTTP transport error: " + cause.getMessage(), cause);
		}
		return buildTransportResponse(jettyResponse, listener.getInputStream());
	}

	// A failure is a stale-connection (pre-response) failure when the server closed the pooled connection before
	// sending any response.  Jetty surfaces this as an EOFException (org.eclipse.jetty.io.EofException extends
	// EOFException) or a ClosedChannelException.  Because listener.get() only returns once response headers have
	// arrived, any such failure here is guaranteed to have occurred before any response bytes were read.
	private static boolean isStaleConnectionFailure(Throwable cause) {
		return cause instanceof EOFException || cause instanceof ClosedChannelException;
	}

	// Releases the Jetty response content when the response never reaches the caller (interrupt/timeout/error),
	// so the connection is not abandoned.  Closing the listener's input stream aborts and releases the exchange.
	private static void abortQuietly(InputStreamResponseListener listener) {
		try {
			listener.getInputStream().close();
		} catch (IOException e) {
			// Best-effort cleanup on an already-failing path; nothing more can be done.
		}
	}

	@Override /* Closeable */
	public void close() throws IOException {
		try {
			httpClient.stop();
		} catch (Exception e) {
			throw ioex(e, "Failed to stop Jetty HttpClient: %s", e.getMessage());
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
			.body(bodyStream)
			// Release the Jetty response InputStream when the RestResponse is closed; TransportResponse.close() only runs this callback.
			.closeCallback(bodyStream);
		jettyResponse.getHeaders().forEach(field -> builder.header(field.getName(), field.getValue()));
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
