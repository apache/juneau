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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.time.*;
import java.util.*;

/**
 * A fully-resolved, transport-layer HTTP request.
 *
 * <p>
 * All values (URIs, headers, bodies) are evaluated before this object is constructed — suppliers have been
 * invoked, {@code null}-valued parts have been filtered, and the final URI has been assembled with query
 * parameters and path substitutions applied.
 *
 * <p>
 * Transport implementations receive a {@link TransportRequest} and are responsible for executing it against
 * the remote server, returning a {@link TransportResponse}.
 *
 * <p>
 * <b>Beta — API subject to change.</b>
 *
 * @since 9.2.1
 */
public final class TransportRequest {

	// Methods defined as idempotent by RFC 7231 §4.2.2 — replaying them cannot change server state beyond a
	// single application of the request, so a transport may safely re-send them once on a fresh connection.
	private static final Set<String> IDEMPOTENT_METHODS = Set.of("GET", "HEAD", "PUT", "DELETE", "OPTIONS", "TRACE");

	private final String method;
	private final URI uri;
	private final List<TransportHeader> headers;
	private final TransportBody body;
	private final Duration timeout;
	private final boolean policyEnforced;
	private final boolean allowPrivateUrls;

	private TransportRequest(Builder builder) {
		this.method = assertArgNotNull("method", builder.method);
		this.uri = assertArgNotNull("uri", builder.uri);
		this.headers = List.copyOf(builder.headers);
		this.body = builder.body;
		this.timeout = builder.timeout;
		this.policyEnforced = builder.policyEnforced;
		this.allowPrivateUrls = builder.allowPrivateUrls;
	}

	/** Returns a new {@link Builder}. */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Returns the HTTP method (e.g. {@code "GET"}, {@code "POST"}).
	 *
	 * @return The method. Never <jk>null</jk>.
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Returns the fully-resolved request URI, including query string.
	 *
	 * @return The URI. Never <jk>null</jk>.
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * Returns all request headers in the order they should be sent.
	 *
	 * @return An unmodifiable list. Never <jk>null</jk>.
	 */
	public List<TransportHeader> getHeaders() {
		return headers;
	}

	/**
	 * Returns the first header with the given name (case-insensitive), or {@code null} if absent.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @return The first matching header, or <jk>null</jk> if absent.
	 */
	public TransportHeader getFirstHeader(String name) {
		return headers.stream()
			.filter(h -> eqic(h.name(), name))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Returns the request body, or {@code null} if this is a body-less request (e.g. GET, HEAD).
	 *
	 * @return The body, possibly <jk>null</jk>.
	 */
	public TransportBody getBody() {
		return body;
	}

	/**
	 * Returns {@code true} if this request can be transparently re-sent once after a stale-connection failure.
	 *
	 * <p>
	 * A pooled keep-alive socket can be torn down by the server between requests; when a transport then reuses
	 * that socket the first write fails before any response is received (e.g. an early {@code EOF} or a
	 * {@code NoHttpResponseException}).  Such a failure is safe to retry on a fresh connection <b>only</b> when
	 * replaying the request cannot cause a duplicate side effect, which requires both of:
	 * <ul>
	 * 	<li>an <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7231#section-4.2.2">idempotent</a>
	 * 		HTTP method ({@code GET}, {@code HEAD}, {@code PUT}, {@code DELETE}, {@code OPTIONS}, {@code TRACE}) — never
	 * 		{@code POST} or any other non-idempotent method, and
	 * 	<li>a body that is either absent or {@link TransportBody#isRepeatable() repeatable}, so the exact same bytes
	 * 		can be written again.
	 * </ul>
	 *
	 * <p>
	 * When in doubt this returns {@code false} — the caller must fail closed rather than risk double-executing a
	 * request.
	 *
	 * @return {@code true} if the request is provably safe to replay once.
	 */
	public boolean isSafeToReplay() {
		return IDEMPOTENT_METHODS.contains(method.toUpperCase(Locale.ROOT)) && (body == null || body.isRepeatable());
	}

	/**
	 * Returns the per-call response timeout, or {@code null} if none was set.
	 *
	 * <p>
	 * Transports apply this as the response/read timeout for the request; a {@code null} value means the
	 * transport's own default applies.  Connect timeouts remain a client-level concern.
	 *
	 * @return The response timeout, possibly <jk>null</jk>.
	 */
	public Duration getTimeout() {
		return timeout;
	}

	/**
	 * Returns {@code true} if this request originated from a {@code @Remote}/{@code @Url} call and is subject to
	 * the SSRF guardrail (see {@code org.apache.juneau.http.remote.RemoteUrlPolicy}).
	 *
	 * <p>
	 * Ordinary (non-{@code @Remote}) requests built directly via {@code RestClient.get(url)} etc. are never
	 * policy-covered.
	 *
	 * @return {@code true} if the SSRF guardrail applies to this request.
	 */
	public boolean isPolicyEnforced() {
		return policyEnforced;
	}

	/**
	 * Returns {@code true} if the {@code allowPrivateUrls} opt-in is in effect for this request, disabling the
	 * deny-private / pin-on-connect / redirect-revalidation checks (the {@code http}/{@code https} scheme
	 * requirement still applies upstream).
	 *
	 * @return {@code true} if private/loopback/link-local/metadata targets are allowed for this request.
	 */
	public boolean isAllowPrivateUrls() {
		return allowPrivateUrls;
	}

	/**
	 * Returns {@code true} if the SSRF guardrail's connect-time machinery (pin-on-connect + Juneau-controlled
	 * redirect revalidation) must be applied by the transport for this request: {@link #isPolicyEnforced()} is set
	 * and {@link #isAllowPrivateUrls()} is not.
	 *
	 * @return {@code true} if the transport must pin-on-connect and manually revalidate redirects for this request.
	 */
	public boolean isSsrfGuardActive() {
		return policyEnforced && ! allowPrivateUrls;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Builder
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for {@link TransportRequest}.
	 *
	 * <p>
	 * <b>Beta — API subject to change.</b>
	 *
	 * @since 9.2.1
	 */
	public static final class Builder {

		String method;
		URI uri;
		final List<TransportHeader> headers = l();
		TransportBody body;
		Duration timeout;
		boolean policyEnforced;
		boolean allowPrivateUrls;

		private Builder() {}

		/**
		 * Sets the HTTP method.
		 *
		 * @param value The method (e.g. {@code "GET"}). Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder method(String value) {
			method = value;
			return this;
		}

		/**
		 * Sets the request URI.
		 *
		 * @param value The URI. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder uri(URI value) {
			uri = value;
			return this;
		}

		/**
		 * Sets the request URI from a string.
		 *
		 * @param value The URI string. Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException If the string is not a valid URI.
		 */
		public Builder uri(String value) {
			try {
				uri = new URI(value);
			} catch (URISyntaxException e) {
				throw iaex(e, "Invalid URI: %s", value);
			}
			return this;
		}

		/**
		 * Appends a request header.
		 *
		 * @param name The header name. Must not be <jk>null</jk>.
		 * @param value The header value. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder header(String name, String value) {
			headers.add(TransportHeader.of(name, value));
			return this;
		}

		/**
		 * Appends multiple request headers.
		 *
		 * @param value The headers to add. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder headers(Collection<TransportHeader> value) {
			headers.addAll(value);
			return this;
		}

		/**
		 * Sets the request body.
		 *
		 * @param value The body. May be <jk>null</jk> for body-less requests.
		 * @return This object.
		 */
		public Builder body(TransportBody value) {
			body = value;
			return this;
		}

		/**
		 * Sets the per-call response timeout.
		 *
		 * @param value The response timeout. May be <jk>null</jk> to use the transport default.
		 * @return This object.
		 */
		public Builder timeout(Duration value) {
			timeout = value;
			return this;
		}

		/**
		 * Marks this request as originating from a {@code @Remote}/{@code @Url} call, subject to the SSRF guardrail,
		 * and records whether the {@code allowPrivateUrls} opt-in is in effect.
		 *
		 * @param policyEnforced {@code true} if this request is policy-covered.
		 * @param allowPrivateUrls {@code true} if the {@code allowPrivateUrls} opt-in disables the deny-private checks.
		 * @return This object.
		 */
		public Builder remoteUrlPolicy(boolean policyEnforced, boolean allowPrivateUrls) {
			this.policyEnforced = policyEnforced;
			this.allowPrivateUrls = allowPrivateUrls;
			return this;
		}

		/**
		 * Builds and returns the {@link TransportRequest}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public TransportRequest build() {
			return new TransportRequest(this);
		}
	}
}
