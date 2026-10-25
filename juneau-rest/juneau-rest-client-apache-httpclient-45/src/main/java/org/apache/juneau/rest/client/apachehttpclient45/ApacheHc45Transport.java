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
import java.util.concurrent.*;

import org.apache.http.*;
import org.apache.http.client.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.*;
import org.apache.http.config.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.juneau.http.remote.*;
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
	private final CloseableHttpClient pinnedHttpClient;
	private final BasicCookieStore cookieStore;
	// One further pinned client (own connection pool) per distinct non-empty per-client allow-list, created on first use,
	// so a connection opened under one allow-list can never be handed to a request with another.
	private final Map<RemoteUrlPolicy.AllowedRanges,CloseableHttpClient> rangedPinnedHttpClients = new ConcurrentHashMap<>();

	ApacheHc45Transport(ApacheHc45TransportBuilder builder) {
		if (builder.httpClient != null) {
			// A caller-supplied client cannot be given pin-on-connect, so supportsUrlPolicy() reports false for it.
			this.httpClient = builder.httpClient;
			this.pinnedHttpClient = null;
			this.cookieStore = null;
		} else {
			// Two clients, one per connection pool, sharing a cookie store so that cookies behave as on one client.
			this.cookieStore = new BasicCookieStore();
			this.httpClient = createDefaultHttpClient(cookieStore);
			this.pinnedHttpClient = createPinnedHttpClient(cookieStore, PolicyPinningDnsResolver.INSTANCE);
		}
	}

	// The client for ordinary (non-@Remote) requests: system DNS and HttpClient's own redirect handling.
	private static CloseableHttpClient createDefaultHttpClient(BasicCookieStore cookieStore) {
		return HttpClients.custom()
			.setDefaultCookieStore(cookieStore)
			.setRedirectStrategy(new UserInfoRefusingRedirectStrategy())
			.build();
	}

	// A redirect Location with user info would make HttpClient replace the client's stored credentials for the target's scope
	// with the user info's, so it is refused (the policy path refuses it too, in PolicyEnforcedRedirects).
	private static final class UserInfoRefusingRedirectStrategy extends DefaultRedirectStrategy {
		@Override /* DefaultRedirectStrategy */
		public URI getLocationURI(HttpRequest request, HttpResponse response, org.apache.http.protocol.HttpContext context) throws org.apache.http.ProtocolException {
			var uri = super.getLocationURI(request, response, context);
			if (uri.getRawUserInfo() != null)
				throw new org.apache.http.ProtocolException("Redirect Location carries user info, which is refused: " + uri.getHost());
			return uri;
		}
	}

	// The client for SSRF-guard-active @Remote requests.  Its connection manager is its own and resolves every host
	// through PolicyPinningDnsResolver, so every connection in its pool was opened through the pin; a guarded request
	// can never be handed an idle connection that an unguarded request opened.  Redirect handling is disabled so that
	// PolicyEnforcedRedirects can revalidate every hop.
	private static CloseableHttpClient createPinnedHttpClient(BasicCookieStore cookieStore, PolicyPinningDnsResolver resolver) {
		var registry = RegistryBuilder.<ConnectionSocketFactory>create()
			.register("http", PlainConnectionSocketFactory.getSocketFactory())
			.register("https", SSLConnectionSocketFactory.getSocketFactory())
			.build();
		return HttpClients.custom()
			.setConnectionManager(new PoolingHttpClientConnectionManager(registry, resolver))
			.setDefaultCookieStore(cookieStore)
			.disableRedirectHandling()
			.build();
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
		if (request.isSsrfGuardActive())
			return executeWithPolicy(request);
		try {
			return sendOnce(request, httpClient, Hc45RedirectCredentialGuard.newGuardingContext());
		} catch (StaleConnectionException e) {
			// A pooled keep-alive connection was torn down by the server before any response was received.
			// Replay once on a fresh connection, but only when it is provably safe (idempotent + repeatable body).
			if (! request.isSafeToReplay())
				throw e.asTransportException();
			try {
				return sendOnce(request, httpClient, Hc45RedirectCredentialGuard.newGuardingContext());
			} catch (StaleConnectionException e2) {
				throw e2.asTransportException();
			}
		}
	}

	@Override /* HttpTransport */
	public boolean supportsUrlPolicy() {
		return pinnedHttpClient != null;
	}

	// Juneau-controlled redirect revalidation for a guarded request; every hop goes through sendOncePinned.
	private TransportResponse executeWithPolicy(TransportRequest request) throws TransportException {
		// One guarding context for the whole chain: each hop is a separate execute, and the guard must remember the first origin.
		var context = Hc45RedirectCredentialGuard.newGuardingContext();
		return PolicyEnforcedRedirects.execute(request, hop -> sendOncePinned(hop, context));
	}

	// Performs one pin-on-connect hop on the pinned client.  A caller-supplied client has no pinned client, so its hops
	// are sent unpinned on that client; RestRequest never routes a guarded request there (supportsUrlPolicy() is false).
	// PolicyPinningDnsResolver refuses a host whose every address is denied with an IllegalArgumentException, which
	// HttpClient propagates unchanged; it is reported as the documented TransportException.
	private TransportResponse sendOncePinned(TransportRequest request, HttpClientContext context) throws TransportException {
		try {
			return sendOnce(request, pinnedClientFor(request), context);
		} catch (StaleConnectionException e) {
			throw e.asTransportException();
		} catch (IllegalArgumentException e) {
			throw new TransportException("HTTP transport error: " + e.getMessage(), e);
		}
	}

	// The pinned client for the request's per-client allow-list: the shared one when it allows nothing, else a dedicated
	// one (own pool, own resolver) per distinct allow-list.  A caller-supplied client has none, and is used as is.
	private CloseableHttpClient pinnedClientFor(TransportRequest request) {
		if (pinnedHttpClient == null)
			return httpClient;
		var ranges = request.getAllowedAddressRanges();
		if (ranges.isEmpty())
			return pinnedHttpClient;
		return rangedPinnedHttpClients.computeIfAbsent(ranges, r -> createPinnedHttpClient(cookieStore, new PolicyPinningDnsResolver(r)));
	}

	// Performs a single HTTP exchange.  Throws StaleConnectionException (a retryable signal) when the failure is a
	// pre-response stale-connection failure; throws TransportException for every other failure.  Every request is sent with a
	// context that applies the redirect credential guard, so transport-built and caller-supplied clients behave the same.  A legacy AbstractHttpClient is refused by the builder because
	// it never sets the request again before it is written, so the wire-stage strip could not run for it.
	private static TransportResponse sendOnce(TransportRequest request, CloseableHttpClient client, HttpClientContext context) throws TransportException, StaleConnectionException {
		var hcRequest = buildHcRequest(request, client);
		CloseableHttpResponse hcResponse;
		try {
			hcResponse = client.execute(hcRequest, context);
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
		try {
			httpClient.close();
		} finally {
			try {
				if (pinnedHttpClient != null)
					pinnedHttpClient.close();
			} finally {
				for (var c : rangedPinnedHttpClients.values())
					c.close();
			}
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------------------------------------------------

	// The effective default config to layer the per-request timeouts on: the client's default if any, else the
	// HttpClient defaults -- so client-level settings (redirects, cookie spec, ...) survive.
	private static RequestConfig baseConfig(CloseableHttpClient client) {
		if (client instanceof Configurable c && c.getConfig() != null)
			return c.getConfig();
		return RequestConfig.DEFAULT;
	}

	private static HttpUriRequest buildHcRequest(TransportRequest request, CloseableHttpClient client) {
		var builder = RequestBuilder.create(request.getMethod()).setUri(request.getUri());
		for (var h : request.getHeaders())
			builder.addHeader(h.name(), h.value());
		var timeout = request.getTimeout();
		if (timeout != null) {
			// Per-request socket (read) and connect timeouts; overrides the client's default RequestConfig for this call only.
			var ms = (int)Math.min(Integer.MAX_VALUE, Math.max(1, timeout.toMillis()));
			builder.setConfig(RequestConfig.copy(baseConfig(client)).setSocketTimeout(ms).setConnectTimeout(ms).build());
		}
		var body = request.getBody();
		if (body != null)
			builder.setEntity(new TransportBodyEntity(body));
		return builder.build();
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
			return !body.isRepeatable();
		}
	}
}
