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
package org.apache.juneau.ng.rest.client;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;
import static org.apache.juneau.commons.utils.Utils.eqic;

import java.net.*;
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

	private final String method;
	private final URI uri;
	private final List<TransportHeader> headers;
	private final TransportBody body;

	private TransportRequest(Builder builder) {
		this.method = assertArgNotNull("method", builder.method);
		this.uri = assertArgNotNull("uri", builder.uri);
		this.headers = List.copyOf(builder.headers);
		this.body = builder.body;
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
	 * @return The first matching header, or <jk>null</jk>.
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
		final List<TransportHeader> headers = new ArrayList<>();
		TransportBody body;

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
				throw new IllegalArgumentException("Invalid URI: " + value, e);
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
		 * Builds and returns the {@link TransportRequest}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public TransportRequest build() {
			return new TransportRequest(this);
		}
	}
}
