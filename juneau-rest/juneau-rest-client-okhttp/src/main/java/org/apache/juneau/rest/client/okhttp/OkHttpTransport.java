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
package org.apache.juneau.rest.client.okhttp;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.client.*;

import okhttp3.*;
import okio.*;

/**
 * {@link HttpTransport} implementation backed by OkHttp 5.x.
 *
 * <p>
 * This transport is auto-discovered via {@link java.util.ServiceLoader} when
 * {@code com.squareup.okhttp3:okhttp-jvm} is on the classpath.  You can also instantiate it explicitly:
 *
 * <p class='bjava'>
 * 	<jv>transport</jv> = OkHttpTransport.<jsm>builder</jsm>()
 * 		.httpClient(<jk>new</jk> OkHttpClient())
 * 		.build();
 *
 * 	<jv>client</jv> = RestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.build();
 * </p>
 *
 * <p>
 * OkHttp's {@link RequestBody#writeTo(BufferedSink)} streams the request body directly to the socket
 * without in-memory buffering for bodies with a known content length.
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
public final class OkHttpTransport implements HttpTransport {

	// OkHttp's own IP-literal test (okhttp3.internal HostnamesCommon): such hosts are connected to without a Dns lookup.
	private static final Pattern OKHTTP_IP_LITERAL = Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");

	private final OkHttpClient httpClient;
	private final OkHttpClient policyHttpClient;
	// One further pin-on-connect client per distinct non-empty per-client allow-list, created on first use.  Each has its
	// own connection pool, so a connection opened under one allow-list is never reused by a request with another.
	private final Map<RemoteUrlPolicy.AllowedRanges,OkHttpClient> rangedPolicyHttpClients = new ConcurrentHashMap<>();

	OkHttpTransport(OkHttpTransportBuilder builder) {
		// A caller-supplied client gets the redirect credential guard on a derived client: newBuilder() shares its
		// connection pool, dispatcher and every other setting, and the caller's own client is left unchanged.
		this.httpClient = builder.httpClient != null ? withCredentialGuard(builder.httpClient) : createDefaultHttpClient();
		// A dedicated client -- sharing this client's connection pool/dispatcher via newBuilder(), but with its own
		// pin-on-connect Dns and with auto-redirect-following disabled -- used only for SSRF-guard-active @Remote
		// calls (see executeWithPolicy); ordinary (non-@Remote) calls on httpClient are completely unaffected.
		// Unlike the shared-connection-manager engines (Apache HttpClient, Jetty), OkHttp's Dns/followRedirects are
		// per-client (not tied to the shared connection pool), so this derivation works even for a caller-supplied
		// httpClient.  The exception is a proxy: OkHttp asks Dns for the proxy's address, not the target's (the proxy
		// resolves the target), so the pin cannot apply -- see supportsUrlPolicy() and requireNoProxy().
		this.policyHttpClient = this.httpClient.newBuilder()
			.followRedirects(false)
			.dns(PolicyPinningDns.INSTANCE)
			.build();
	}

	private static OkHttpClient createDefaultHttpClient() {
		return withCredentialGuard(new OkHttpClient());
	}

	// OkHttp's own redirect follower keeps a caller-set Cookie, so every client this transport sends through has the
	// guard as a network interceptor, which runs for each hop.  The first-hop recorder is the last application
	// interceptor, so the origin is the URL after any caller application interceptor has rewritten it.
	private static OkHttpClient withCredentialGuard(OkHttpClient client) {
		return client.newBuilder()
			.addInterceptor(OkHttpRedirectCredentialGuard.FIRST_HOP_RECORDER)
			.addNetworkInterceptor(new OkHttpRedirectCredentialGuard())
			.build();
	}

	/**
	 * Returns a new builder for this transport.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static OkHttpTransportBuilder builder() {
		return new OkHttpTransportBuilder();
	}

	/**
	 * Returns a new instance backed by a default {@link OkHttpClient}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static OkHttpTransport create() {
		return builder().build();
	}

	@Override /* HttpTransport */
	public TransportResponse execute(TransportRequest request) throws TransportException {
		if (request.isSsrfGuardActive())
			return PolicyEnforcedRedirects.execute(request, this::sendOncePinned);
		return sendOnce(request, httpClient);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Reports <jk>false</jk> when the {@link OkHttpClient} has an explicit {@linkplain OkHttpClient#proxy() proxy}: a
	 * proxy resolves the target host itself, so the pin-on-connect {@link okhttp3.Dns} is never consulted for the target.
	 * A client that selects a proxy per URL through a {@link ProxySelector} is accepted here, but a guarded request
	 * whose URL is routed through a proxy is refused when sent (fail closed).
	 */
	@Override /* HttpTransport */
	public boolean supportsUrlPolicy() {
		var proxy = httpClient.proxy();
		return proxy == null || proxy.type() == Proxy.Type.DIRECT;
	}

	// Performs one pin-on-connect hop via the dedicated, redirect-disabled policyHttpClient.
	private TransportResponse sendOncePinned(TransportRequest request) throws TransportException {
		try {
			var url = HttpUrl.get(request.getUri().toString());
			requireNoProxy(url);
			requireAllowedIpLiteral(url.host(), request.getAllowedAddressRanges());
		} catch (IllegalArgumentException e) {
			throw new TransportException("HTTP transport error: " + e.getMessage(), e);
		}
		return sendOnce(request, policyClientFor(request.getAllowedAddressRanges()));
	}

	private OkHttpClient policyClientFor(RemoteUrlPolicy.AllowedRanges ranges) {
		if (ranges.isEmpty())
			return policyHttpClient;
		return rangedPolicyHttpClients.computeIfAbsent(ranges, r -> policyHttpClient.newBuilder()
			.dns(new PolicyPinningDns(r))
			.connectionPool(new ConnectionPool())
			.build());
	}

	// A proxy (SOCKS, or HTTP/CONNECT) resolves the target host itself, so OkHttp never asks Dns about the target and the
	// pin cannot apply.  Fail closed rather than send a guarded request through one.
	private void requireNoProxy(HttpUrl url) {
		var explicit = httpClient.proxy();
		var uri = url.uri();
		// Like OkHttp, skip the ProxySelector when java.net.URI cannot parse a host (e.g. an underscore host name).
		var proxies = explicit != null ? List.of(explicit) : uri.getHost() == null ? List.of(Proxy.NO_PROXY) : selectedProxies(uri);
		for (var p : proxies)
			if (p.type() != Proxy.Type.DIRECT)
				throw iaex("Request to '%s' would be routed through a proxy, which resolves the target host itself and bypasses the SSRF guard's address pinning; use a client without a proxy (or set allowPrivateUrls)", url.host());
	}

	// Like OkHttp, a ProxySelector answer of null or empty means no proxy.
	private List<Proxy> selectedProxies(URI uri) {
		var selected = httpClient.proxySelector().select(uri);
		return selected == null || selected.isEmpty() ? List.of(Proxy.NO_PROXY) : selected;
	}

	// OkHttp connects to any host it considers an IP literal without consulting Dns, so the pin never sees one; apply the
	// same address check here.  Its literal test is deliberately broader than dotted-quad: an all-digit/dot host such as
	// 2130706433 or 127.1 is parsed by InetAddress to 127.0.0.1.  (Host names are checked by PolicyPinningDns.)
	// The literal is parsed locally and never resolved: a host that matches the pattern but is not a valid literal
	// (1.2.3.4.5, 127.0.0.1.) would otherwise fall through to a DNS lookup here and a second one in OkHttp, so it is refused.
	private static void requireAllowedIpLiteral(String host, RemoteUrlPolicy.AllowedRanges ranges) {
		if (! OKHTTP_IP_LITERAL.matcher(host).matches())
			return;
		var address = parseIpLiteral(host);
		if (address == null)
			throw iaex("Host '%s' matches the IP-literal form but is not a valid IP literal; refusing to resolve it", host);
		if (RemoteUrlPolicy.isDeniedAddress(address, ranges))
			throw iaex("No allowed address for host '%s': the IP literal is private/loopback/link-local/reserved (set allowPrivateUrls to allow local-dev targets)", host);
	}

	// Returns the address for an IP-literal host without any name lookup, or null if it is not a valid literal.
	private static InetAddress parseIpLiteral(String host) {
		try {
			if (host.indexOf(':') != -1)
				return InetAddress.getByName("[" + host + "]"); // brackets make the JDK treat it as IPv6 only, never DNS
			var parts = host.split("\\.", -1);
			if (parts.length > 4)
				return null;
			var value = 0L;
			for (var i = 0; i < parts.length; i++) {
				var part = parts[i];
				if (part.isEmpty() || part.length() > 10 || (part.length() > 1 && part.charAt(0) == '0'))
					return null;
				var n = Long.parseLong(part);
				var last = i == parts.length - 1;
				if (n > (last ? (1L << (8 * (5 - parts.length))) - 1 : 255))
					return null;
				value |= last ? n : n << (8 * (3 - i));
			}
			return InetAddress.getByAddress(new byte[]{(byte)(value >> 24), (byte)(value >> 16), (byte)(value >> 8), (byte)value});
		} catch (UnknownHostException e) {
			return null;
		}
	}

	private static TransportResponse sendOnce(TransportRequest request, OkHttpClient client) throws TransportException {
		var okRequest = buildOkRequest(request);
		var timeout = request.getTimeout();
		// Per-request read timeout (response-header wait and gaps between body reads); newBuilder() shares the connection pool.
		var effective = timeout == null ? client : client.newBuilder().readTimeout(timeout).build();
		Response okResponse;
		try {
			okResponse = effective.newCall(okRequest).execute();
		} catch (IOException | IllegalArgumentException e) {
			// IllegalArgumentException: any IAE raised while executing the call, on both the guarded and the ordinary
			// path, is surfaced as a TransportException.  The expected source is a Dns.lookup failure, which OkHttp
			// propagates unchanged -- notably PolicyPinningDns refusing a host whose every resolved address is denied.
			throw new TransportException("HTTP transport error: " + e.getMessage(), e);
		}
		try {
			return buildTransportResponse(okResponse);
		} catch (TransportException | RuntimeException e) {
			// If response-body wiring fails the closeCallback never runs, so close here to avoid leaking the connection.
			okResponse.close();
			throw e;
		}
	}

	@Override /* Closeable */
	public void close() {
		httpClient.dispatcher().executorService().shutdown();
		httpClient.connectionPool().evictAll();
		for (var c : rangedPolicyHttpClients.values())
			c.connectionPool().evictAll();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------------------------------------------------

	private static Request buildOkRequest(TransportRequest request) {
		var builder = new Request.Builder().url(request.getUri().toString());
		for (var h : request.getHeaders())
			builder.addHeader(h.name(), h.value());
		builder.method(request.getMethod(), buildOkBody(request.getBody(), request.getMethod()));
		return builder.build();
	}

	private static RequestBody buildOkBody(TransportBody body, String method) {
		if (body == null) {
			// OkHttp requires a non-null body for POST/PUT/PATCH even if empty
			return requiresBody(method) ? RequestBody.create(new byte[0], null) : null;
		}
		var ct = body.getContentType();
		var mediaType = ct != null ? MediaType.parse(ct) : null;
		return new RequestBody() {
			@Override
			public MediaType contentType() {
				return mediaType;
			}

			@Override
			public long contentLength() {
				return body.getContentLength();
			}

			@Override
			public void writeTo(BufferedSink sink) throws IOException {
				body.writeTo(sink.outputStream());
			}
		};
	}

	private static boolean requiresBody(String method) {
		return eqic("POST", method) || eqic("PUT", method) || eqic("PATCH", method);
	}

	@SuppressWarnings({
		"java:S2095" // okResponse is closed via TransportResponse.closeCallback, not here
	})
	private static TransportResponse buildTransportResponse(Response okResponse) throws TransportException {
		var builder = TransportResponse.builder()
			.statusCode(okResponse.code())
			.reasonPhrase(okResponse.message())
			.closeCallback(okResponse);
		for (var i = 0; i < okResponse.headers().size(); i++)
			builder.header(okResponse.headers().name(i), okResponse.headers().value(i));
		try {
			builder.body(okResponse.body().byteStream()); // body() is @NonNull in OkHttp 5.x
		} catch (Exception e) {
			throw new TransportException("Failed to read response body: " + e.getMessage(), e);
		}
		return builder.build();
	}
}
