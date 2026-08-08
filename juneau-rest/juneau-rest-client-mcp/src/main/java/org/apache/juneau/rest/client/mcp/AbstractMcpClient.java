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
package org.apache.juneau.rest.client.mcp;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.client.*;

/**
 * Revision-neutral base class for Model Context Protocol (MCP) clients.
 *
 * <p>
 * Wraps a single configured {@link RestClient} and executes raw JSON-RPC 2.0 request/response round trips against one
 * endpoint in JSON response mode. A generic {@link RestCallInterceptor} passthrough is provided on the builder so
 * per-revision facades can layer auth and other cross-cutting concerns without changing this core.
 *
 * <p>
 * {@link #send(JsonRpcRequest)} returns the complete {@link JsonRpcResponse} envelope untouched, including
 * {@code error} responses - it does not throw {@link McpException} on a JSON-RPC error. Typed facades are
 * responsible for translating a response with a non-<jk>null</jk> {@link JsonRpcResponse#getError() error} into a
 * thrown {@link McpException}.
 *
 * <p>
 * Instances are thread-safe for concurrent reuse: {@link RestClient} and endpoint are final, and each
 * {@link #send(JsonRpcRequest)} creates a fresh request/response scope. Builders are single-use value assemblers:
 * calling {@code build()} twice creates two clients and both use the same explicitly configured transport instance.
 *
 * <p>
 * <b>Beta - API subject to change:</b> This class builds directly on {@code org.apache.juneau.rest.client}, which
 * is itself Beta.
 *
 * @since 10.0.0
 */
public abstract class AbstractMcpClient implements Closeable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_BUILDER = "builder";
	private static final String ARG_ENDPOINT = "endpoint";
	private static final String ARG_INTERCEPTOR = "interceptor";
	private static final String ARG_REQUEST = "request";

	private final RestClient restClient;
	private final String endpoint;

	/**
	 * Constructor.
	 *
	 * @param builder The builder supplying this client's configuration. Must not be <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code builder} is <jk>null</jk>, or its endpoint is <jk>null</jk>/blank
	 * 	or is not a syntactically valid absolute {@code http}/{@code https} URL (see
	 * 	{@link #validateEndpoint(String)}).
	 */
	protected AbstractMcpClient(Builder<?> builder) {
		assertArgNotNull(ARG_BUILDER, builder);
		this.endpoint = validateEndpoint(builder.endpoint);
		this.restClient = builder.restClientBuilder
			.defaultSerializer(JsonSerializer.DEFAULT)
			.defaultParser(JsonParser.DEFAULT)
			.build();
	}

	/**
	 * Validates the configured endpoint: non-<jk>null</jk>/non-blank (as before), and additionally that it
	 * parses as a syntactically valid, absolute URL using the {@code http} or {@code https} scheme.
	 *
	 * <p>
	 * Rejecting a malformed or non-http(s) endpoint here, at construction, is preferable to letting it surface
	 * later as an opaque transport failure on the first {@link #send(JsonRpcRequest)} call.
	 *
	 * @param value The candidate endpoint. Must not be <jk>null</jk>/blank.
	 * @return {@code value}, unchanged, once validated.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>/blank, is not a syntactically valid URI,
	 * 	has no scheme, or has a scheme other than {@code http}/{@code https} (case-insensitive).
	 */
	private static String validateEndpoint(String value) {
		var endpointValue = assertArgNotNullOrBlank(ARG_ENDPOINT, value);
		URI uri;
		try {
			uri = new URI(endpointValue);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid MCP endpoint URL '" + endpointValue + "': " + e.getMessage(), e);
		}
		var scheme = uri.getScheme();
		if (scheme == null || ! (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
			throw new IllegalArgumentException(
				"Invalid MCP endpoint URL '" + endpointValue + "': must be an absolute http or https URL, but scheme was '" + scheme + "'.");
		return endpointValue;
	}

	/**
	 * Sends a raw JSON-RPC request and returns the raw JSON-RPC response envelope.
	 *
	 * <p>
	 * A request with a <jk>null</jk> {@link JsonRpcRequest#getId() id} is a notification: this method still sends
	 * it over the wire, but always returns <jk>null</jk> without attempting to read a response body, per the
	 * JSON-RPC 2.0 notification contract (the server sends no response body for a notification).
	 *
	 * @param request The JSON-RPC request to send. Must not be <jk>null</jk>.
	 * @return The JSON-RPC response envelope, or <jk>null</jk> if {@code request} is a notification.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 */
	public JsonRpcResponse send(JsonRpcRequest request) throws IOException {
		return send(request, Map.of());
	}

	/**
	 * Sends a raw JSON-RPC request and returns the raw JSON-RPC response envelope.
	 *
	 * <p>
	 * A request with a <jk>null</jk> {@link JsonRpcRequest#getId() id} is a notification: this method still sends
	 * it over the wire, but always returns <jk>null</jk> without attempting to read a response body, per the
	 * JSON-RPC 2.0 notification contract (the server sends no response body for a notification).
	 *
	 * <p>
	 * <b>Note:</b> for a notification, this method returns <jk>null</jk> immediately after the transport call
	 * completes, without ever inspecting {@link RestResponse#getStatusCode()}. A non-2xx HTTP status on a
	 * notification is therefore silently swallowed - the caller sees no indication that the server rejected it.
	 * This is intentional per the notification contract above (there is no response envelope to report an error
	 * through), but it does mean transport-level failures on notifications are otherwise invisible to callers.
	 *
	 * @param request The JSON-RPC request to send. Must not be <jk>null</jk>.
	 * @param httpHeaders Optional additional HTTP headers to add on this request. Can be <jk>null</jk>.
	 * @return The JSON-RPC response envelope, or <jk>null</jk> if {@code request} is a notification.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 */
	public JsonRpcResponse send(JsonRpcRequest request, Map<String,String> httpHeaders) throws IOException {
		assertArgNotNull(ARG_REQUEST, request);
		var req = restClient.post(endpoint).body(request);
		if (httpHeaders != null) {
			for (var e : httpHeaders.entrySet())
				req.header(e.getKey(), e.getValue());
		}
		try (var res = req.run()) {
			if (JsonRpcResponse.notification(request.getId()))
				return null;
			var sc = res.getStatusCode();
			JsonRpcResponse parsed;
			try {
				parsed = res.body().as(JsonRpcResponse.class);
			} catch (IOException e) {
				if (sc < 200 || sc > 299)
					throw ioex(e, "MCP server returned HTTP %s and the body was not a JSON-RPC envelope.", sc);
				throw e;
			}
			if (parsed == null)
				throw ioex("No response body received for JSON-RPC request id '%s' (HTTP %s).", request.getId(), sc);
			return parsed;
		}
	}

	/**
	 * Opens a Server-Sent-Events stream on the configured endpoint, for revision-specific facades that support a
	 * duplex (server-initiated) channel alongside the request/response {@link #send} calls.
	 *
	 * @return A reader over the opened event stream. Never <jk>null</jk>. Callers are responsible for closing it.
	 * @throws IOException If a transport-level error occurs opening the stream.
	 */
	protected SseEventReader openEventStream() throws IOException {
		return restClient.post(endpoint).openEventStream();
	}

	/**
	 * Opens a Server-Sent-Events stream on the configured endpoint by first POSTing {@code request} as the request
	 * body — for a revision-specific method (e.g. {@code subscriptions/listen}) whose held-open SSE response is
	 * itself the answer to one specific JSON-RPC request, as opposed to {@link #openEventStream()}'s body-less
	 * open used by the duplex server-push channel.
	 *
	 * @param request The JSON-RPC request to POST as the body that opens the stream. Must not be <jk>null</jk>.
	 * @return A reader over the opened event stream. Never <jk>null</jk>. Callers are responsible for closing it.
	 * @throws IOException If a transport-level or (de)serialization error occurs opening the stream.
	 */
	protected SseEventReader openEventStream(JsonRpcRequest request) throws IOException {
		return openEventStream(request, Map.of());
	}

	/**
	 * Opens a Server-Sent-Events stream on the configured endpoint by first POSTing {@code request} as the request
	 * body, with additional HTTP headers on the opening request — for a revision-specific method whose held-open
	 * SSE response must agree with a header-based method/name contract (e.g. SEP-2243's {@code Mcp-Method}/
	 * {@code Mcp-Name}) the same way {@link #send(JsonRpcRequest, Map)} already does for the ordinary
	 * request/response path.
	 *
	 * <p>
	 * Unlike the body-less {@link #openEventStream()}, this opening POST is itself the answer to one specific
	 * JSON-RPC request, so the server may reject it with a JSON-RPC error envelope instead of opening the stream
	 * (e.g. a capability, {@code Accept}-header, or over-limit gate) — on any HTTP status, not just 2xx. Rather
	 * than handing that envelope to an {@link SseEventReader} (which would see zero events and eventually look
	 * like an indistinguishable transport failure), this checks the response {@code Content-Type} first: only a
	 * body reporting {@code text/event-stream} is opened as a stream. Anything else is parsed as a
	 * {@link JsonRpcResponse}; a non-<jk>null</jk> {@link JsonRpcResponse#getError() error} is thrown synchronously
	 * as an {@link McpException} carrying the server's code/message/data, matching the way mid-stream JSON-RPC
	 * errors are already surfaced to a stream's listener. A response that is neither {@code text/event-stream} nor
	 * a JSON-RPC error envelope still fails as a plain {@link IOException} (or the underlying
	 * {@link RestCallException} from a non-2xx status), never silently masquerading as an opened stream.
	 *
	 * @param request The JSON-RPC request to POST as the body that opens the stream. Must not be <jk>null</jk>.
	 * @param httpHeaders Optional additional HTTP headers to add on this request. Can be <jk>null</jk>.
	 * @return A reader over the opened event stream. Never <jk>null</jk>. Callers are responsible for closing it.
	 * @throws IOException If a transport-level or (de)serialization error occurs opening the stream, or the
	 * 	response was neither an SSE stream nor a JSON-RPC error envelope.
	 * @throws McpException If the server rejected {@code request} with a JSON-RPC error instead of opening the
	 * 	stream.
	 */
	protected SseEventReader openEventStream(JsonRpcRequest request, Map<String,String> httpHeaders) throws IOException {
		assertArgNotNull(ARG_REQUEST, request);
		var req = restClient.post(endpoint).body(request);
		if (httpHeaders != null) {
			for (var e : httpHeaders.entrySet())
				req.header(e.getKey(), e.getValue());
		}
		if (! req.hasHeader("Accept"))
			req.header("Accept", "text/event-stream");
		var res = req.run();
		var opened = false;
		try {
			if (isEventStream(res)) {
				res.assertOk();
				var r = res.body().asEventStream();
				opened = true;
				return r;
			}
			var parsed = res.body().as(JsonRpcResponse.class);
			if (parsed != null && parsed.getError() != null)
				throw McpException.fromJsonRpcError(parsed.getError());
			res.assertOk(); // not a JSON-RPC error envelope either: fall back to the ordinary status diagnostic
			throw ioex("Expected a text/event-stream response opening %s, but got Content-Type '%s'.",
				request.getMethod(), contentTypeOf(res));
		} finally {
			if (! opened)
				quiet(res::close);
		}
	}

	private static boolean isEventStream(RestResponse res) {
		var contentType = contentTypeOf(res);
		return contentType != null && contentType.toLowerCase(Locale.ROOT).contains("text/event-stream");
	}

	private static String contentTypeOf(RestResponse res) {
		var h = res.getFirstHeader("Content-Type");
		return h == null ? null : h.value();
	}

	@Override /* Overridden from Closeable */
	public void close() throws IOException {
		restClient.close();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Builder
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent, self-typed builder for {@link AbstractMcpClient} subclasses.
	 *
	 * @param <SELF> The concrete builder type (CRTP self-type), allowing dated adapters to add their own builder
	 * 	methods while preserving fluent chaining.
	 * @since 10.0.0
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> {

		String endpoint;
		RestClient.Builder restClientBuilder = RestClient.builder();

		/**
		 * Sets the MCP server endpoint URL.
		 *
		 * @param value The absolute endpoint URL (e.g. {@code "http://localhost:8080/mcp"}). Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF endpoint(String value) {
			endpoint = assertArgNotNullOrBlank(ARG_ENDPOINT, value);
			return self();
		}

		/**
		 * Sets the HTTP transport used by the underlying {@link RestClient}.
		 *
		 * @param value The transport. Can be <jk>null</jk> (a transport is auto-discovered via {@link java.util.ServiceLoader}).
		 * @return This object.
		 */
		public SELF transport(HttpTransport value) {
			restClientBuilder.transport(value);
			return self();
		}

		/**
		 * Adds a lifecycle interceptor (e.g. the auth-token seam) to the underlying {@link RestClient}.
		 *
		 * @param value The interceptor to add. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF interceptor(RestCallInterceptor value) {
			restClientBuilder.interceptors(assertArgNotNull(ARG_INTERCEPTOR, value));
			return self();
		}

		@SuppressWarnings({
			"unchecked" // SELF is always the concrete builder subtype by construction (CRTP).
		})
		protected SELF self() {
			return (SELF) this;
		}
	}
}
