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

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.ng.http.*;
import org.apache.juneau.ng.http.entity.*;
import org.apache.juneau.ng.http.header.*;
import org.apache.juneau.ng.http.part.*;

/**
 * Fluent builder for a single HTTP request.
 *
 * <p>
 * Obtain instances from the {@link NgRestClient} factory methods (e.g. {@link NgRestClient#get(String)}).
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // client is not owned here; run() transfers NgRestResponse ownership to caller
})
public final class NgRestRequest {

	private final NgRestClient client;
	private final String method;
	private final String url;
	private final List<HttpHeader> headers = new ArrayList<>();
	private final List<HttpPart> queryData = new ArrayList<>();
	private final List<HttpPart> formData = new ArrayList<>();
	private final Map<String, Object> pathData = new LinkedHashMap<>();
	private HttpBody body;
	private TransportBody convertedBody;
	private boolean debug;
	private URI resolvedUri;

	NgRestRequest(NgRestClient client, String method, String url) {
		this.client = client;
		this.method = method;
		this.url = url;
		// Inherit client defaults
		this.headers.addAll(client.defaultHeaders);
		this.queryData.addAll(client.defaultQueryData);
	}

	// --------------------------------------------------
	// Header
	// --------------------------------------------------

	/**
	 * Adds request headers.
	 *
	 * @param value The headers to add. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public NgRestRequest headers(HttpHeader... value) {
		headers.addAll(Arrays.asList(value));
		return this;
	}

	/**
	 * Adds a request header.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. May be <jk>null</jk> to omit.
	 * @return This object.
	 */
	public NgRestRequest header(String name, String value) {
		headers.add(HttpHeaderBean.of(name, value));
		return this;
	}

	/**
	 * Adds a request header with a lazy value.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value Supplier for the value. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public NgRestRequest header(String name, Supplier<String> value) {
		headers.add(HttpHeaderBean.of(name, value));
		return this;
	}

	// --------------------------------------------------
	// Query
	// --------------------------------------------------

	/**
	 * Adds query parameters.
	 *
	 * @param value The parts to add. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public NgRestRequest queryData(HttpPart... value) {
		queryData.addAll(Arrays.asList(value));
		return this;
	}

	/**
	 * Adds a query parameter.
	 *
	 * @param name The parameter name. Must not be <jk>null</jk>.
	 * @param value The parameter value. May be <jk>null</jk> to omit.
	 * @return This object.
	 */
	public NgRestRequest queryData(String name, String value) {
		queryData.add(HttpPartBean.of(name, value));
		return this;
	}

	/**
	 * Adds a query parameter with a lazy value.
	 *
	 * @param name The parameter name. Must not be <jk>null</jk>.
	 * @param value Supplier for the value. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public NgRestRequest queryData(String name, Supplier<String> value) {
		queryData.add(HttpPartBean.of(name, value));
		return this;
	}

	// --------------------------------------------------
	// Form data
	// --------------------------------------------------

	/**
	 * Adds form fields (for {@code application/x-www-form-urlencoded} bodies).
	 *
	 * @param value The parts to add. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public NgRestRequest formData(HttpPart... value) {
		formData.addAll(Arrays.asList(value));
		return this;
	}

	/**
	 * Adds a form field.
	 *
	 * @param name The field name. Must not be <jk>null</jk>.
	 * @param value The field value. May be <jk>null</jk> to omit.
	 * @return This object.
	 */
	public NgRestRequest formData(String name, String value) {
		formData.add(HttpPartBean.of(name, value));
		return this;
	}

	// --------------------------------------------------
	// Path substitution
	// --------------------------------------------------

	/**
	 * Adds path variable substitutions for URI templates (e.g. replaces {@code {id}} in the URL).
	 *
	 * @param value The path parts to apply. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public NgRestRequest pathData(HttpPart... value) {
		for (var p : value)
			pathData.put(p.getName(), p.getValue());
		return this;
	}

	/**
	 * Adds a path variable.
	 *
	 * @param name The variable name (without braces). Must not be <jk>null</jk>.
	 * @param value The replacement value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public NgRestRequest pathData(String name, Object value) {
		pathData.put(name, value);
		return this;
	}

	// --------------------------------------------------
	// Body
	// --------------------------------------------------

	/**
	 * Sets the request body directly from an {@link HttpBody}.
	 *
	 * @param value The body. May be <jk>null</jk>.
	 * @return This object.
	 */
	public NgRestRequest body(HttpBody value) {
		body = value;
		convertedBody = null;
		return this;
	}

	/**
	 * Sets the request body from an arbitrary Java object, converting it via the client's body converter chain.
	 *
	 * <p>
	 * The default converters handle: {@link HttpBody} (passthrough), {@link InputStream},
	 * {@code byte[]}, and {@link java.io.File}. Custom converters can be registered on the builder.
	 *
	 * @param value The body object. May be <jk>null</jk> to clear the body.
	 * @return This object.
	 * @throws IOException If a converter fails.
	 * @throws IllegalArgumentException If no converter can handle the given type.
	 */
	public NgRestRequest body(Object value) throws IOException {
		if (value == null) {
			body = null;
			convertedBody = null;
			return this;
		}
		for (var converter : client.bodyConverters) {
			if (converter.canConvert(value)) {
				convertedBody = converter.convert(value);
				body = null;
				return this;
			}
		}
		throw new IllegalArgumentException("No BodyConverter found for type: " + value.getClass().getName());
	}

	/**
	 * Sets the request body to a raw string (UTF-8, {@code text/plain}).
	 *
	 * <p>
	 * Unlike {@link #body(Object)}, this bypasses the converter chain and sends the string as-is.
	 *
	 * @param value The string body. May be <jk>null</jk> to clear the body.
	 * @return This object.
	 */
	public NgRestRequest bodyString(String value) {
		if (value == null) {
			body = null;
			convertedBody = null;
		} else {
			body = StringBody.of(value, "text/plain");
			convertedBody = null;
		}
		return this;
	}

	// --------------------------------------------------
	// Debug
	// --------------------------------------------------

	/**
	 * Flags this request for verbose debug logging.
	 *
	 * <p>
	 * When set, the configured {@link RestLogger} will receive an entry with {@link RestLogEntry#isDebug()} {@code true},
	 * enabling full request/response header and body logging for this call only.
	 *
	 * @return This object.
	 */
	public NgRestRequest debug() {
		debug = true;
		return this;
	}

	// --------------------------------------------------
	// Accessors (for logging / interceptors)
	// --------------------------------------------------

	/**
	 * Returns the HTTP method for this request (e.g. {@code "GET"}).
	 *
	 * @return The method. Never <jk>null</jk>.
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Returns the fully resolved URI for this request, or {@code null} if {@link #run()} has not been called yet.
	 *
	 * @return The URI, possibly <jk>null</jk>.
	 */
	public URI getUri() {
		return resolvedUri;
	}

	// --------------------------------------------------
	// Execute
	// --------------------------------------------------

	/**
	 * Executes the request and returns the response.
	 *
	 * <p>
	 * The caller is responsible for closing the returned {@link NgRestResponse}.
	 *
	 * @return The response. Never <jk>null</jk>.
	 * @throws TransportException If a network-level error occurs.
	 * @throws NgRestCallException If the response could not be processed.
	 */
	@SuppressWarnings({
		"resource" // response is returned to caller who must close it; transport is owned by client
	})
	public NgRestResponse run() throws TransportException, NgRestCallException {
		var start = Instant.now();
		NgRestResponse response = null;
		Throwable error = null;
		try {
			for (var interceptor : client.interceptors)
				interceptor.onInit(this);

			var transportRequest = buildTransportRequest();
			var transportResponse = client.transport.execute(transportRequest);
			response = new NgRestResponse(transportResponse);

			for (var interceptor : client.interceptors)
				interceptor.onConnect(this, response);

			return response;
		} catch (TransportException e) {
			error = e;
			throw e;
		} catch (NgRestCallException e) {
			error = e;
			throw e;
		} catch (Exception e) {
			error = e;
			throw new TransportException("Request failed: " + e.getMessage(), e);
		} finally {
			var elapsed = Duration.between(start, Instant.now());
			NgRestResponse finalResponse = response;
			for (var interceptor : client.interceptors) {
				try {
					interceptor.onClose(this, finalResponse);
				} catch (Exception e2) { // HTT: exception in onClose; hard to test reliably
					// suppress interceptor close errors — best effort
				}
			}
			if (client.logger != null) {
				var entry = RestLogEntry.builder()
					.request(this)
					.response(finalResponse)
					.error(error)
					.elapsed(elapsed)
					.debug(debug)
					.build();
				client.logger.log(entry);
			}
		}
	}

	private TransportRequest buildTransportRequest() {
		var resolvedUrl = applyPathSubstitutions(url);
		resolvedUri = appendQuery(resolvedUrl);

		var builder = TransportRequest.builder()
			.method(method)
			.uri(resolvedUri);

		// Headers
		for (var h : headers) {
			var v = h.getValue();
			if (v != null)
				builder.header(h.getName(), v);
		}

		// Pre-converted body from body(Object) takes priority
		if (convertedBody != null) {
			if (convertedBody.getContentType() != null)
				builder.header("Content-Type", convertedBody.getContentType());
			builder.body(convertedBody);
		} else if (!formData.isEmpty() && body == null) {
			// Form body when form data is present and no explicit body was set
			var formBody = buildFormBody();
			builder.header("Content-Type", formBody.getContentType());
			builder.body(TransportBody.of(formBody));
		} else if (body != null) {
			if (body.getContentType() != null)
				builder.header("Content-Type", body.getContentType());
			builder.body(TransportBody.of(body));
		}

		return builder.build();
	}

	private String applyPathSubstitutions(String template) {
		var result = template;
		for (var entry : pathData.entrySet()) {
			var replacement = entry.getValue() != null ? entry.getValue().toString() : "";
			result = result.replace("{" + entry.getKey() + "}", urlEncode(replacement));
		}
		return result;
	}

	private URI appendQuery(String baseUrl) {
		if (queryData.isEmpty()) {
			try {
				return new URI(baseUrl);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Invalid URL: " + baseUrl, e);
			}
		}
		var sb = new StringBuilder(baseUrl);
		var first = !baseUrl.contains("?");
		for (var part : queryData) {
			var v = part.getValue();
			if (v == null)
				continue;
			sb.append(first ? '?' : '&');
			sb.append(urlEncode(part.getName())).append('=').append(urlEncode(v));
			first = false;
		}
		try {
			return new URI(sb.toString());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL: " + sb, e);
		}
	}

	private HttpBody buildFormBody() {
		var sb = new StringBuilder();
		var first = true;
		for (var part : formData) {
			var v = part.getValue();
			if (v == null)
				continue;
			if (!first)
				sb.append('&');
			sb.append(urlEncode(part.getName())).append('=').append(urlEncode(v));
			first = false;
		}
		final var encoded = sb.toString();
		return new HttpBody() {
			@Override public String getContentType() { return "application/x-www-form-urlencoded"; }
			@Override public long getContentLength() { return encoded.getBytes(StandardCharsets.UTF_8).length; }
			@Override public void writeTo(OutputStream out) throws IOException { out.write(encoded.getBytes(StandardCharsets.UTF_8)); }
			@Override public boolean isRepeatable() { return true; }
		};
	}

	private static String urlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
