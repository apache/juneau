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
package org.apache.juneau.rest.client.apachehttpclient50;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.hc.client5.http.config.*;
import org.apache.hc.client5.http.cookie.*;
import org.apache.hc.client5.http.impl.*;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.client5.http.protocol.*;
import org.apache.hc.client5.http.routing.*;
import org.apache.hc.client5.http.impl.io.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.*;
import org.apache.hc.core5.http.io.support.*;
import org.apache.hc.core5.http.message.*;
import org.apache.hc.core5.http.protocol.*;
import org.apache.hc.core5.io.*;
import org.apache.hc.core5.util.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.client.*;

/**
 * {@link HttpTransport} implementation backed by Apache HttpClient 5.x.
 *
 * <p>
 * This transport is auto-discovered via {@link java.util.ServiceLoader} when
 * {@code org.apache.httpcomponents.client5:httpclient5} is on the classpath.  You can also instantiate it explicitly:
 *
 * <p class='bjava'>
 * 	<jv>transport</jv> = ApacheHc5Transport.<jsm>builder</jsm>()
 * 		.httpClient(HttpClients.createDefault())
 * 		.build();
 *
 * 	<jv>client</jv> = RestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.build();
 * </p>
 *
 * <p>
 * The transport passes each ordinary request a per-request context that removes the caller's credential headers
 * ({@link RedirectSecurity#stripOnCrossOrigin()}) from a redirect hop to another origin and from every later hop.  On
 * the client the transport creates itself, a redirect to another origin is therefore followed without those headers.
 * With a caller-supplied client, the client's own redirect settings and {@code RedirectStrategy} decide which redirects
 * are followed, and the context guards whatever strategy the client was built with, including
 * {@code LaxRedirectStrategy}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">Next-Generation REST Client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // httpClient is owned by this transport and closed in close()
})
public final class ApacheHc5Transport implements HttpTransport {

	private final CloseableHttpClient httpClient;
	private final CloseableHttpClient pinnedHttpClient;
	private final BasicCookieStore cookieStore;
	// One further pinned client (own connection pool) per distinct non-empty per-client allow-list, created on first use,
	// so a connection opened under one allow-list can never be handed to a request with another.
	private final Map<RemoteUrlPolicy.AllowedRanges,CloseableHttpClient> rangedPinnedHttpClients = new ConcurrentHashMap<>();

	ApacheHc5Transport(ApacheHc5TransportBuilder builder) {
		if (builder.httpClient != null) {
			// A caller-supplied client cannot be given pin-on-connect, so supportsUrlPolicy() reports false for it.
			this.httpClient = builder.httpClient;
			this.pinnedHttpClient = null;
			this.cookieStore = null;
		} else {
			// Two clients, one per connection pool, sharing a cookie store so that cookies behave as on one client.
			this.cookieStore = new BasicCookieStore();
			this.httpClient = HttpClients.custom()
				.setDefaultCookieStore(cookieStore)
				.setRedirectStrategy(StrippingRedirectStrategy.INSTANCE)
				.build();
			this.pinnedHttpClient = createPinnedHttpClient(cookieStore, PolicyPinningDnsResolver.INSTANCE);
		}
	}

	// The redirect strategy of the transport's own client for ordinary requests.  DefaultRedirectStrategy refuses a
	// cross-origin redirect that would replay a sensitive header (every header in the stripped set is marked sensitive),
	// so the caller would get the 3xx back.  Every request on that client is sent with a CredentialGuardingContext, which
	// removes those headers from such a hop, so the redirect is followed without them instead.  Which responses count as
	// redirects is DefaultRedirectStrategy's own; the method rewrite, the redirect cap and the circular-redirect check are
	// the redirect executor's, and unchanged.
	private static final class StrippingRedirectStrategy extends DefaultRedirectStrategy {
		static final StrippingRedirectStrategy INSTANCE = new StrippingRedirectStrategy();

		@Override /* DefaultRedirectStrategy */
		public boolean isRedirectAllowed(HttpHost currentTarget, HttpHost newTarget, HttpRequest redirect, HttpContext context) {
			return true;
		}
	}

	// The client for SSRF-guard-active @Remote requests.  Its connection manager is its own and resolves every host
	// through PolicyPinningDnsResolver, so every connection in its pool was opened through the pin; a guarded request
	// can never be handed an idle connection that an unguarded request opened.  Redirect handling is disabled so that
	// PolicyEnforcedRedirects can revalidate every hop.
	private static CloseableHttpClient createPinnedHttpClient(BasicCookieStore cookieStore, PolicyPinningDnsResolver resolver) {
		var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setDnsResolver(resolver)
			.build();
		return HttpClients.custom()
			.setConnectionManager(connectionManager)
			.setDefaultCookieStore(cookieStore)
			.disableRedirectHandling()
			.build();
	}

	/**
	 * Returns a new builder for this transport.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static ApacheHc5TransportBuilder builder() {
		return new ApacheHc5TransportBuilder();
	}

	/**
	 * Returns a new instance backed by a default {@link CloseableHttpClient}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ApacheHc5Transport create() {
		return builder().build();
	}

	@Override /* HttpTransport */
	public TransportResponse execute(TransportRequest request) throws TransportException {
		if (request.isSsrfGuardActive())
			return executeWithPolicy(request);
		return sendOnce(request, httpClient, buildContext(request, httpClient));
	}

	@Override /* HttpTransport */
	public boolean supportsUrlPolicy() {
		return pinnedHttpClient != null;
	}

	// Juneau-controlled redirect revalidation for a guarded request; every hop goes through sendOncePinned.
	private TransportResponse executeWithPolicy(TransportRequest request) throws TransportException {
		return PolicyEnforcedRedirects.execute(request, this::sendOncePinned);
	}

	// Performs one pin-on-connect hop on the pinned client.  A caller-supplied client has no pinned client, so its hops
	// are sent unpinned on that client; RestRequest never routes a guarded request there (supportsUrlPolicy() is false).
	// PolicyPinningDnsResolver refuses a host whose every address is denied with an IllegalArgumentException, which
	// HttpClient propagates unchanged; it is reported as the documented TransportException.
	private TransportResponse sendOncePinned(TransportRequest request) throws TransportException {
		try {
			var client = pinnedClientFor(request);
			return sendOnce(request, client, buildContext(request, client));
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

	private static TransportResponse sendOnce(TransportRequest request, CloseableHttpClient client, HttpClientContext context) throws TransportException {
		var hcRequest = buildHcRequest(request);
		ClassicHttpResponse hcResponse;
		try {
			// An explicit target, which a client from HttpClients.createMinimal() requires.
			hcResponse = client.executeOpen(RoutingSupport.determineHost(hcRequest), hcRequest, context);
		} catch (IOException | HttpException e) {
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

	private static void closeQuietly(ClassicHttpResponse hcResponse) {
		try {
			if (hcResponse instanceof ModalCloseable mc)
				mc.close(CloseMode.IMMEDIATE);
			else
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

	// The request context.  On the client for ordinary requests (the transport's own, or a caller-supplied one) it is a
	// CredentialGuardingContext, since that client may follow a cross-origin redirect with the caller's credentials; the
	// pinned clients follow no redirects (PolicyEnforcedRedirects strips per hop).  It carries a request-scoped
	// RequestConfig only for a per-request response (socket read) timeout, so the client's default config applies
	// otherwise; on a pinned client with neither, the context is null.
	private HttpClientContext buildContext(TransportRequest request, CloseableHttpClient client) {
		var timeout = request.getTimeout();
		var guarded = client == httpClient;
		if (timeout == null && ! guarded)
			return null;
		var context = guarded ? new CredentialGuardingContext() : HttpClientContext.create();
		if (timeout == null)
			return context;
		// A context-level RequestConfig replaces (not merges with) the client default, so copy the client's default first.
		var base = client instanceof Configurable c && c.getConfig() != null ? c.getConfig() : RequestConfig.DEFAULT;
		context.setRequestConfig(RequestConfig.copy(base).setResponseTimeout(Timeout.ofMilliseconds(Math.max(1, timeout.toMillis()))).build());
		return context;
	}

	private static ClassicHttpRequest buildHcRequest(TransportRequest request) throws TransportException {
		var builder = ClassicRequestBuilder.create(request.getMethod()).setUri(request.getUri());
		for (var h : request.getHeaders())
			builder.addHeader(new BasicHeader(h.name(), h.value(), RedactedHeaders.isSensitive(h.name(), RedirectSecurity.stripOnCrossOrigin())));
		var body = request.getBody();
		if (body != null)
			builder.setEntity(buildEntity(body));
		try {
			return builder.build();
		} catch (Exception e) {
			throw new TransportException("Failed to build HTTP request: " + e.getMessage(), e);
		}
	}

	private static HttpEntity buildEntity(TransportBody body) {
		var ct = body.getContentType();
		var contentType = ct != null ? ContentType.parse(ct) : ContentType.APPLICATION_OCTET_STREAM;
		return new EntityTemplate(body.getContentLength(), contentType, null, body::writeTo);
	}

	private static TransportResponse buildTransportResponse(ClassicHttpResponse hcResponse) throws TransportException {
		var builder = TransportResponse.builder()
			.statusCode(hcResponse.getCode())
			.reasonPhrase(hcResponse.getReasonPhrase());
		// A graceful close drains the entity before releasing the connection, which never finishes on an open-ended SSE
		// stream; close those immediately (discarding the connection) so close() and any blocked reader return promptly.
		if (isEventStream(hcResponse) && hcResponse instanceof ModalCloseable mc)
			builder.closeCallback(() -> mc.close(CloseMode.IMMEDIATE));
		else
			builder.closeCallback(hcResponse);
		for (var h : hcResponse.getHeaders())
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

	private static boolean isEventStream(ClassicHttpResponse hcResponse) {
		var ct = hcResponse.getFirstHeader("Content-Type");
		return ct != null && "text/event-stream".equalsIgnoreCase(ct.getValue().split(";", 2)[0].trim());
	}
}
