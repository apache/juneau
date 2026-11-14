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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpClient.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;

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
 * <p>
 * For SSRF-guard-active ({@code @Remote}) requests (see {@link TransportRequest#isSsrfGuardActive()}), this
 * transport pin-on-connects: it resolves the request host, selects an address that passes
 * {@link RemoteUrlPolicy}, and rewrites the request URI to that literal address
 * (preserving the original host via an explicit {@code Host} header) so the socket connects only to the
 * validated address &mdash; and re-runs the same check on every {@code Location} hop via
 * {@link PolicyEnforcedRedirects}. The JDK {@link HttpClient} has no connect-time/DNS SPI, so an IP-literal
 * rewrite cannot preserve TLS SNI/hostname verification for the original hostname; per the locked design, policy
 * -covered <b>HTTPS</b> requests are therefore refused (fail closed) on this transport &mdash; use a transport
 * that supports connect-time pinning (e.g. Apache HttpClient 4.5/5), or set {@code allowPrivateUrls(true)} if the
 * target is an intentional local-dev/intranet endpoint. A policy-covered request is also refused if the
 * underlying {@link HttpClient} was built with automatic redirect-following enabled ({@link Redirect#NORMAL} or
 * {@link Redirect#ALWAYS}), since this transport cannot re-validate hops that client follows on its own; build it
 * with the default {@link Redirect#NEVER} (the JDK default) to let this transport's own redirect loop apply.
 *
 * <h2 class='section'>See Also:</h2><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">Next-Generation REST Client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // Not owned here; lifecycle is managed by the surrounding context
})
public final class JavaHttpTransport implements HttpTransport {

	/** Default connect timeout of the {@link HttpClient} this transport creates when none is supplied. */
	public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);

	/** Default per-request timeout (until the response headers arrive) applied when the request carries none. */
	public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);

	/** Default cap, in bytes, on the response body this transport will deliver. */
	public static final long DEFAULT_MAX_RESPONSE_BYTES = 128L * 1024 * 1024;

	private static final String RESTRICTED_HEADERS_PROPERTY = "jdk.httpclient.allowRestrictedHeaders";

	private final HttpClient httpClient;

	private final long maxResponseBytes;

	JavaHttpTransport(JavaHttpTransportBuilder builder) {
		this.httpClient = builder.httpClient != null ? builder.httpClient : HttpClient.newBuilder().connectTimeout(DEFAULT_CONNECT_TIMEOUT).build();
		this.maxResponseBytes = builder.maxResponseBytes;
		if (builder.allowHostHeaderOverride)
			allowHostHeader();
	}

	// Raw System property access is intentional: the JDK reads this value from System properties only, so Settings overrides/sources must not be consulted.
	private static void allowHostHeader() {
		var existing = System.getProperty(RESTRICTED_HEADERS_PROPERTY);
		if (ib(existing))
			System.setProperty(RESTRICTED_HEADERS_PROPERTY, "host");
		else if (! existing.toLowerCase(Locale.ROOT).contains("host"))
			System.setProperty(RESTRICTED_HEADERS_PROPERTY, existing + ",host");
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
		if (request.isSsrfGuardActive())
			return executeWithPolicy(request);
		requireNoUnguardedRedirects(request);
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

	@Override /* HttpTransport */
	public boolean supportsUrlPolicy() {
		return true;
	}

	// A caller-supplied HttpClient that follows redirects itself would replay the request, credential headers included, against
	// a redirect target of another origin; the JDK client offers no hook to strip them.  Refuse (fail closed) rather than send.
	private void requireNoUnguardedRedirects(TransportRequest request) throws TransportException {
		if (httpClient.followRedirects() == Redirect.NEVER)
			return;
		for (var h : request.getHeaders())
			if (RedactedHeaders.isSensitive(h.name(), RedirectSecurity.stripOnCrossOrigin()))
				throw new TransportException("Refusing to send a request carrying the credential header '" + h.name()
					+ "' through a JavaHttpTransport whose HttpClient auto-follows redirects (" + httpClient.followRedirects()
					+ "); the JDK client would forward it to a redirect target of another origin.  Build the HttpClient with "
					+ "the default Redirect.NEVER, or pass the credential another way.");
	}

	// Runs the Juneau-controlled redirect loop, pinning each hop to a policy-allowed address.
	private TransportResponse executeWithPolicy(TransportRequest request) throws TransportException {
		if (httpClient.followRedirects() != Redirect.NEVER)
			throw new TransportException("Refusing to send a policy-covered @Remote request through a "
				+ "JavaHttpTransport whose HttpClient auto-follows redirects (" + httpClient.followRedirects()
				+ "); this transport cannot re-validate hops that the client follows on its own. Build the "
				+ "HttpClient with the default Redirect.NEVER, or set allowPrivateUrls(true) if this is an "
				+ "intentional local-dev/intranet target.");
		return PolicyEnforcedRedirects.execute(request, this::sendOncePinned);
	}

	// Performs one pin-on-connect hop: resolves and pins the request host, then sends without following redirects.
	private TransportResponse sendOncePinned(TransportRequest request) throws TransportException {
		var uri = request.getUri();
		var scheme = uri.getScheme();
		if (neqic("http", scheme))
			throw new TransportException("JavaHttpTransport cannot preserve TLS SNI/hostname verification while "
				+ "pinning the resolved address (the JDK HttpClient has no connect-time/DNS SPI); refusing "
				+ "(fail closed) a policy-covered HTTPS request: " + uri
				+ ".  Use a transport with connect-time pinning support (e.g. Apache HttpClient 4.5/5), or set "
				+ "allowPrivateUrls(true) if this is an intentional local-dev/intranet target.");
		InetAddress pinned;
		try {
			pinned = RemoteUrlPolicy.selectAllowedAddress(requireHost(uri), false, RemoteUrlPolicy.AddressResolver.DEFAULT, request.getAllowedAddressRanges());
		} catch (UnknownHostException e) {
			throw new TransportException("Could not resolve @Remote request host: " + uri.getHost(), e);
		} catch (IllegalArgumentException e) {
			// A refused pin (every resolved address is denied) is reported as the documented TransportException.
			throw new TransportException("HTTP transport error: " + e.getMessage(), e);
		}
		URI pinnedUri;
		try {
			pinnedUri = new URI(pinnedUriString(scheme, pinned, uri));
		} catch (URISyntaxException e) {
			throw new TransportException("Could not build a pinned request URI for: " + uri, e);
		}
		var hostHeader = uri.getPort() >= 0 ? uri.getHost() + ":" + uri.getPort() : uri.getHost();
		try {
			return sendOnce(request, pinnedUri, hostHeader);
		} catch (StaleConnectionException e) {
			throw e.asTransportException();
		}
	}

	// java.net.URI parses some host names (an underscore name such as my_service) with a null host, which
	// InetAddress would resolve as the loopback address; refuse such a URI by name instead.
	private static String requireHost(URI uri) {
		var host = uri.getHost();
		if (host == null)
			throw iaex("Request URI has no host that java.net.URI can parse, so it cannot be pinned: %s", uri);
		return host;
	}

	// Performs a single HTTP exchange.  Throws StaleConnectionException (a retryable signal) when the failure is a
	// pre-response stale-connection failure; throws TransportException for every other failure.
	private TransportResponse sendOnce(TransportRequest request) throws TransportException, StaleConnectionException {
		return sendOnce(request, request.getUri(), null);
	}

	// Performs a single HTTP exchange against overrideUri (the pinned address) instead of request.getUri(),
	// adding an explicit Host header (the original hostname[:port]) when hostHeader is non-null so virtual-hosting
	// still works after the IP-literal rewrite.
	private TransportResponse sendOnce(TransportRequest request, URI overrideUri, String hostHeader) throws TransportException, StaleConnectionException {
		HttpRequest jdkRequest;
		try {
			jdkRequest = buildJdkRequest(request, overrideUri, hostHeader);
		} catch (IllegalArgumentException e) {
			// The JDK refuses a URI it cannot use (for example an underscore host name, which java.net.URI parses
			// with a null host) or an invalid header.
			if (hostHeader == null)
				throw new TransportException("Could not build the HTTP request for " + request.getUri() + ": " + e.getMessage(), e);
			// With hostHeader set, the JDK rejected the "Host" header override -- i.e. the JVM
			// was not started with -Djdk.httpclient.allowRestrictedHeaders=host. Fail closed rather than silently pinning without preserving the original Host.
			throw new TransportException("Could not preserve the original Host header while pin-on-connecting a "
				+ "policy-covered HTTP request (the JDK HttpClient restricts the Host header unless the JVM "
				+ "is started with the flag below); refusing (fail closed): " + request.getUri()
				+ ".  Call allowHostHeaderOverride() on the transport builder (a JVM-wide setting) or set the JVM flag -Djdk.httpclient.allowRestrictedHeaders=host, use a transport with native "
				+ "connect-time pinning support (e.g. Apache HttpClient 4.5/5), or set allowPrivateUrls(true) if "
				+ "this is an intentional local-dev/intranet target.", e);
		}
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
		return buildTransportResponse(jdkResponse, maxResponseBytes);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------------------------------------------------

	// Builds a URI string identical to "uri" except with its host replaced by the pinned literal address, by
	// concatenating uri's already-percent-encoded raw components -- NOT via the multi-arg java.net.URI
	// constructor, which treats its arguments as decoded components and would double-encode any "%" already
	// present in the raw path/query/fragment (e.g. "%20" would become "%2520").
	private static String pinnedUriString(String scheme, InetAddress pinned, URI uri) {
		var hostLiteral = pinned instanceof Inet6Address ? "[" + pinned.getHostAddress() + "]" : pinned.getHostAddress();
		var sb = new StringBuilder(scheme).append("://").append(hostLiteral);
		if (uri.getPort() >= 0)
			sb.append(':').append(uri.getPort());
		if (uri.getRawPath() != null)
			sb.append(uri.getRawPath());
		if (uri.getRawQuery() != null)
			sb.append('?').append(uri.getRawQuery());
		if (uri.getRawFragment() != null)
			sb.append('#').append(uri.getRawFragment());
		return sb.toString();
	}

	private static HttpRequest buildJdkRequest(TransportRequest request, URI uri, String hostHeader) {
		var builder = HttpRequest.newBuilder().uri(uri).method(request.getMethod(), buildBodyPublisher(request.getBody()));
		if (hostHeader != null)
			builder.header("Host", hostHeader);
		for (var h : request.getHeaders())
			builder.header(h.name(), h.value());
		var timeout = request.getTimeout();
		builder.timeout(timeout != null ? timeout : DEFAULT_REQUEST_TIMEOUT);
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
		var publisher = BodyPublishers.ofInputStream(() -> {
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
		// A repeatable body with a known length is sent fixed-length (Content-Length) rather than chunked;
		// unknown-length (or non-repeatable streaming) bodies keep chunked transfer encoding.
		var length = body.getContentLength();
		if (body.isRepeatable() && length >= 0)
			return length == 0 ? BodyPublishers.noBody() : BodyPublishers.fromPublisher(publisher, length);
		return publisher;
	}

	private static TransportResponse buildTransportResponse(HttpResponse<InputStream> jdkResponse, long maxBytes) {
		var raw = jdkResponse.body();
		var body = maxBytes > 0 ? new LimitedInputStream(raw, maxBytes) : raw;
		var builder = TransportResponse.builder()
			.statusCode(jdkResponse.statusCode())
			.body(body)
			.closeCallback(raw);
		jdkResponse.headers().map().forEach((name, values) ->
			values.forEach(value -> builder.header(name, value)));
		return builder.build();
	}

	// Fails a read once more than the configured number of body bytes has been delivered.
	private static final class LimitedInputStream extends FilterInputStream {
		private final long limit;
		private long count;

		LimitedInputStream(InputStream in, long limit) {
			super(in);
			this.limit = limit;
		}

		@Override /* InputStream */
		public int read() throws IOException {
			var b = super.read();
			if (b >= 0)
				add(1);
			return b;
		}

		@Override /* InputStream */
		public int read(byte[] b, int off, int len) throws IOException {
			var n = super.read(b, off, len);
			if (n > 0)
				add(n);
			return n;
		}

		@Override /* InputStream */
		public long skip(long n) throws IOException {
			var skipped = super.skip(n);
			if (skipped > 0)
				add(skipped);
			return skipped;
		}

		private void add(long n) throws IOException {
			count += n;
			if (count > limit)
				throw new IOException("Response body exceeds the configured maximum of " + limit + " bytes");
		}
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
