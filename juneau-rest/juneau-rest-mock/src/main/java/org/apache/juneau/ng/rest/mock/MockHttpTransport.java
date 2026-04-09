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
package org.apache.juneau.ng.rest.mock;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.ng.rest.client.*;

/**
 * A programmable in-process {@link HttpTransport} for unit testing.
 *
 * <p>
 * Register one or more route handlers, or a catch-all handler, and the transport will dispatch
 * each {@link TransportRequest} to the appropriate handler without any real network activity.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>transport</jv> = MockHttpTransport.<jsm>builder</jsm>()
 * 		.on(<js>"GET"</js>, <js>"/hello"</js>, req -&gt; TransportResponse.<jsm>builder</jsm>()
 * 			.statusCode(200)
 * 			.reasonPhrase(<js>"OK"</js>)
 * 			.header(<js>"Content-Type"</js>, <js>"text/plain"</js>)
 * 			.body(new ByteArrayInputStream(<js>"Hello, World!"</js>.getBytes(StandardCharsets.UTF_8)))
 * 			.build())
 * 		.build();
 *
 * 	NgRestClient <jv>client</jv> = NgRestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.build();
 * </p>
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
@SuppressWarnings({
	"resource" // execute()/of()/build() return Closeable types; caller must close TransportResponse / MockHttpTransport
})
public final class MockHttpTransport implements HttpTransport {

	/**
	 * A handler that maps an incoming {@link TransportRequest} to a {@link TransportResponse}.
	 *
	 * @since 9.2.1
	 */
	@FunctionalInterface
	public interface RequestHandler {
		/**
		 * Handles the request.
		 *
		 * @param request The incoming request. Never {@code null}.
		 * @return The response to return. Never {@code null}.
		 * @throws TransportException If the handler wants to simulate a network error.
		 */
		TransportResponse handle(TransportRequest request) throws TransportException;
	}

	private final List<Route> routes;
	private final RequestHandler fallback;
	private final List<TransportRequest> recordedRequests;

	private MockHttpTransport(Builder builder) {
		this.routes = List.copyOf(builder.routes);
		this.fallback = builder.fallback;
		this.recordedRequests = builder.recordRequests ? new CopyOnWriteArrayList<>() : null;
	}

	/** Returns a new {@link Builder}. */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Creates a simple mock transport that always returns the given status code and body string.
	 *
	 * @param statusCode The HTTP status code to return.
	 * @param body The body string to return (UTF-8 encoded). May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static MockHttpTransport of(int statusCode, String body) {
		return builder()
			.fallback((RequestHandler) req -> TransportResponse.builder()
				.statusCode(statusCode)
				.body(body != null ? new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)) : null)
				.build())
			.build();
	}

	@Override /* HttpTransport */
	public TransportResponse execute(TransportRequest request) throws TransportException {
		if (recordedRequests != null)
			recordedRequests.add(request);

		var path = request.getUri().getPath();
		var method = request.getMethod();

		for (var route : routes) {
			if (route.matches(method, path))
				return route.handler().handle(request);
		}

		if (fallback != null)
			return fallback.handle(request);

		return TransportResponse.builder()
			.statusCode(404)
			.reasonPhrase("Not Found")
			.body(new ByteArrayInputStream(("No mock route matched: " + method + " " + path).getBytes(StandardCharsets.UTF_8)))
			.build();
	}

	/**
	 * Returns an unmodifiable view of all requests received since creation (only available when recording is enabled).
	 *
	 * @return The recorded requests. Empty if recording is disabled.
	 */
	public List<TransportRequest> getRecordedRequests() {
		return recordedRequests != null ? Collections.unmodifiableList(recordedRequests) : List.of();
	}

	/**
	 * Clears the list of recorded requests.
	 */
	public void clearRecordedRequests() {
		if (recordedRequests != null)
			recordedRequests.clear();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Inner types
	// -----------------------------------------------------------------------------------------------------------------

	private record Route(String method, String path, RequestHandler handler) {
		boolean matches(String m, String p) {
			return (method == null || method.equalsIgnoreCase(m)) && (path == null || path.equals(p));
		}
	}

	/**
	 * Fluent builder for {@link MockHttpTransport}.
	 *
	 * @since 9.2.1
	 */
	public static final class Builder {

		final List<Route> routes = new ArrayList<>();
		RequestHandler fallback;
		boolean recordRequests;

		private Builder() {}

		/**
		 * Registers a handler for the given HTTP method and path.
		 *
		 * @param method The HTTP method (e.g. {@code "GET"}). {@code null} matches any method.
		 * @param path The exact request path (e.g. {@code "/api/users"}). {@code null} matches any path.
		 * @param handler The handler. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder on(String method, String path, RequestHandler handler) {
			routes.add(new Route(method, path, handler));
			return this;
		}

		/**
		 * Sets a fallback handler used when no route matches.
		 *
		 * <p>
		 * Without a fallback, unmatched requests receive a {@code 404} response.
		 *
		 * @param value The fallback handler. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder fallback(RequestHandler value) {
			fallback = value;
			return this;
		}

		/**
		 * Enables recording of all received requests so they can be retrieved via
		 * {@link MockHttpTransport#getRecordedRequests()}.
		 *
		 * @return This object.
		 */
		public Builder recordRequests() {
			recordRequests = true;
			return this;
		}

		/**
		 * Builds and returns the {@link MockHttpTransport}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public MockHttpTransport build() {
			return new MockHttpTransport(this);
		}
	}
}
