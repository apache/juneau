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

import static org.apache.juneau.commons.utils.Utils.eqic;

import java.io.*;
import java.util.*;

/**
 * A transport-layer HTTP response.
 *
 * <p>
 * Produced by {@link HttpTransport#execute(TransportRequest)} and consumed by the higher-level
 * {@code NgRestResponse}.
 *
 * <p>
 * Callers <b>must</b> close this object (it implements {@link Closeable}) after consuming the response body;
 * transport implementations use the close callback to release connection-pool resources.
 *
 * <p>
 * <b>Beta — API subject to change.</b>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // body and closeCallback are owned by TransportResponse and released in close()
})
public final class TransportResponse implements Closeable {

	private final int statusCode;
	private final String reasonPhrase;
	private final List<TransportHeader> headers;
	private final InputStream body;
	private final Closeable closeCallback;

	private TransportResponse(Builder builder) {
		this.statusCode = builder.statusCode;
		this.reasonPhrase = builder.reasonPhrase;
		this.headers = List.copyOf(builder.headers);
		this.body = builder.body;
		this.closeCallback = builder.closeCallback != null ? builder.closeCallback : () -> {};
	}

	/** Returns a new {@link Builder}. */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Returns the HTTP status code.
	 *
	 * @return The status code (e.g. {@code 200}).
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Returns the reason phrase (e.g. {@code "OK"}), or {@code null} if absent.
	 *
	 * @return The reason phrase, possibly <jk>null</jk>.
	 */
	public String getReasonPhrase() {
		return reasonPhrase;
	}

	/**
	 * Returns all response headers.
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
	 * Returns the response body stream, or {@code null} if the response has no body.
	 *
	 * <p>
	 * Callers must not close this stream directly — close the {@link TransportResponse} instead.
	 *
	 * @return The body stream, possibly <jk>null</jk>.
	 */
	public InputStream getBody() {
		return body;
	}

	@Override /* Closeable */
	public void close() throws IOException {
		closeCallback.close();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Builder
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for {@link TransportResponse}.
	 *
	 * <p>
	 * Primarily used by transport implementations and tests.
	 *
	 * @since 9.2.1
	 */
	@SuppressWarnings({
		"resource" // body and closeCallback are passed into TransportResponse which owns and closes them
	})
	public static final class Builder {

		int statusCode;
		String reasonPhrase;
		final List<TransportHeader> headers = new ArrayList<>();
		InputStream body;
		Closeable closeCallback;

		private Builder() {}

		/**
		 * Sets the HTTP status code.
		 *
		 * @param value The status code.
		 * @return This object.
		 */
		public Builder statusCode(int value) {
			statusCode = value;
			return this;
		}

		/**
		 * Sets the reason phrase.
		 *
		 * @param value The reason phrase. May be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder reasonPhrase(String value) {
			reasonPhrase = value;
			return this;
		}

		/**
		 * Appends a response header.
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
		 * Sets the response body stream.
		 *
		 * @param value The stream. May be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder body(InputStream value) {
			body = value;
			return this;
		}

		/**
		 * Sets a callback invoked when {@link TransportResponse#close()} is called.
		 *
		 * <p>
		 * Transport implementations use this to return connections to the pool.
		 *
		 * @param value The close callback. May be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder closeCallback(Closeable value) {
			closeCallback = value;
			return this;
		}

		/**
		 * Builds and returns the {@link TransportResponse}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public TransportResponse build() {
			return new TransportResponse(this);
		}
	}
}
