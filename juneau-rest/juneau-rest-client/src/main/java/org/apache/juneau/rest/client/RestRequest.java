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

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;

/**
 * Fluent builder for a single HTTP request.
 *
 * <p>
 * Obtain instances from the {@link RestClient} factory methods (e.g. {@link RestClient#get(String)}).
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S1192", // Duplicate string literals are HTTP header names and REST protocol wire values; intentional
	"resource"    // client is not owned here; run() transfers RestResponse ownership to caller
})
public final class RestRequest {

	private final RestClient client;
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
	private Duration timeout;
	// Per-request interceptors, unioned with the client-level interceptors at run() time.
	private final List<RestCallInterceptor> requestInterceptors = new ArrayList<>();

	RestRequest(RestClient client, String method, String url) {
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
	public RestRequest headers(HttpHeader... value) {
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
	public RestRequest header(String name, String value) {
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
	public RestRequest header(String name, Supplier<String> value) {
		headers.add(HttpHeaderBean.of(name, value));
		return this;
	}

	/**
	 * Returns <jk>true</jk> if a header with the given name (case-insensitive) is already set on this request.
	 *
	 * <p>
	 * Used by the next-gen remote-proxy engine to honor the precedence of a caller-supplied
	 * {@code @Header} parameter over an annotation-declared {@code accept}/{@code contentType} attribute.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the header is present.
	 */
	public boolean hasHeader(String name) {
		return headers.stream().anyMatch(h -> name.equalsIgnoreCase(h.getName()));
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
	public RestRequest queryData(HttpPart... value) {
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
	public RestRequest queryData(String name, String value) {
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
	public RestRequest queryData(String name, Supplier<String> value) {
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
	public RestRequest formData(HttpPart... value) {
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
	public RestRequest formData(String name, String value) {
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
	public RestRequest pathData(HttpPart... value) {
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
	public RestRequest pathData(String name, Object value) {
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
	public RestRequest body(HttpBody value) {
		body = value;
		convertedBody = null;
		return this;
	}

	/**
	 * Sets the request body from an arbitrary Java object.
	 *
	 * <p>
	 * The object is first run through the client's body converter chain. The default converters handle:
	 * {@link HttpBody} (passthrough), {@link InputStream}, {@code byte[]}, and {@link java.io.File}.
	 * Custom converters can be registered on the builder.
	 *
	 * <p>
	 * If no converter matches, the object is serialized with the client's default serializer
	 * ({@link RestClient#getDefaultSerializer()}) and sent as a string body using the serializer's content type.
	 *
	 * @param value The body object. May be <jk>null</jk> to clear the body.
	 * @return This object.
	 * @throws IOException If a converter fails or the default serializer fails.
	 * @throws IllegalArgumentException If the default serializer produces output that is neither text nor {@code byte[]}.
	 * 	Binary ({@code byte[]}) serializer output is sent as a binary body using the serializer's media type
	 * 	(falling back to {@code application/octet-stream} when the media type is <jk>null</jk>).
	 */
	public RestRequest body(Object value) throws IOException {
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
		// No converter matched: stream the POJO through the client's default serializer.
		// Mirrors classic SerializedEntity — the serializer writes straight to the transport output stream during
		// run() rather than being pre-materialized into a String/byte[].  Repeatable (re-serializes on resend), so a
		// future auto-retry can resend it; streaming bodies (InputStream/Reader) remain non-repeatable.
		var s = client.getDefaultSerializer();
		var mt = s.getResponseContentType();
		body = SerializerBody.of(s, value, mt != null ? mt.toString() : "application/json");
		convertedBody = null;
		return this;
	}

	/**
	 * Sets the request body to a token/record-streaming cursor body.
	 *
	 * <p>
	 * Convenience for {@link #body(HttpBody) body(streamBody)} that documents streaming intent and avoids a cast.
	 * The {@link RecordStreamBody} writes directly to the transport output stream during {@link #run()}, so large
	 * payloads are streamed to the wire without being buffered in memory.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jv>client</jv>.post(<js>"/bulk-upload"</js>)
	 * 		.streamBodyEntity(RecordStreamBody.<jsm>records</jsm>(<jv>w</jv> -&gt; {
	 * 			<jk>for</jk> (Bean <jv>b</jv> : <jv>source</jv>())
	 * 				<jv>w</jv>.write(<jv>b</jv>);
	 * 		}))
	 * 		.run();
	 * </p>
	 *
	 * @param value The streaming body. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RestRequest streamBodyEntity(RecordStreamBody value) {
		return body(value);
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
	public RestRequest bodyString(String value) {
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
	public RestRequest debug() {
		debug = true;
		return this;
	}

	// --------------------------------------------------
	// Timeout / interceptors
	// --------------------------------------------------

	/**
	 * Sets a per-call response timeout for this request.
	 *
	 * <p>
	 * Threaded onto the {@link TransportRequest} and applied by the transport as the response/read timeout.  Connect
	 * timeouts remain a client-level setting.
	 *
	 * @param value The response timeout. May be <jk>null</jk> to use the transport default.
	 * @return This object.
	 */
	public RestRequest timeout(Duration value) {
		timeout = value;
		return this;
	}

	/**
	 * Adds per-request lifecycle interceptors.
	 *
	 * <p>
	 * These are unioned with the client-level interceptors when the request runs: the client-level (builder)
	 * interceptors fire first, then these in the order added.  The next-gen remote-proxy engine uses this to layer
	 * annotation-declared interface-level then method-level interceptors after the builder-configured ones.
	 *
	 * @param value The interceptors to add. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RestRequest interceptors(RestCallInterceptor... value) {
		requestInterceptors.addAll(Arrays.asList(value));
		return this;
	}

	/**
	 * Returns <jk>true</jk> if this request's body can be safely re-sent (retry gating).
	 *
	 * <p>
	 * A request with no body is trivially repeatable.  Otherwise the answer is delegated to the underlying
	 * body's {@link HttpBody#isRepeatable()} (or {@link TransportBody#isRepeatable()} for a pre-converted body):
	 * buffered bodies (string/byte[]/file/serialized POJO) are repeatable; one-shot streaming bodies
	 * ({@link InputStream}/{@link Reader}/piped) are not.
	 *
	 * @return <jk>true</jk> if the body is absent or repeatable.
	 */
	public boolean isBodyRepeatable() {
		if (convertedBody != null)
			return convertedBody.isRepeatable();
		if (body != null)
			return body.isRepeatable();
		return true;
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
	 * The caller is responsible for closing the returned {@link RestResponse}.
	 *
	 * @return The response. Never <jk>null</jk>.
	 * @throws TransportException If a network-level error occurs.
	 * @throws RestCallException If the response could not be processed.
	 */
	@SuppressWarnings({
		"java:S1130" // RestCallException retained for public-API/source stability: interceptors declare 'throws Exception', so a RestCallException they raise is caught by the precise-rethrow multi-catch below and propagated unchanged; Sonar's flow analysis misses this path. Removing the throws would break callers that catch RestCallException (a sibling checked exception of TransportException).
	})
	public RestResponse run() throws TransportException, RestCallException {
		var start = Instant.now();
		RestResponse response = null;
		Throwable error = null;
		// Union the client-level (builder) interceptors with any per-request (interface- then
		// method-level) interceptors, preserving the order builder → interface → method.
		var effectiveInterceptors = effectiveInterceptors();
		try {
			for (var interceptor : effectiveInterceptors)
				interceptor.onInit(this);

			var transportRequest = buildTransportRequest();
			var transportResponse = client.transport.execute(transportRequest);
			response = new RestResponse(transportResponse, client);

			for (var interceptor : effectiveInterceptors)
				interceptor.onConnect(this, response);

			return response;
		} catch (TransportException | RestCallException e) {
			error = e;
			throw e;
		} catch (Exception e) {
			error = e;
			throw new TransportException("Request failed: " + e.getMessage(), e);
		} finally {
			var elapsed = Duration.between(start, Instant.now());
			RestResponse finalResponse = response;
			for (var interceptor : effectiveInterceptors) {
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

	/** Returns the client-level interceptors followed by the per-request ones (builder → interface → method order). */
	private List<RestCallInterceptor> effectiveInterceptors() {
		if (requestInterceptors.isEmpty())
			return client.interceptors;
		var l = new ArrayList<RestCallInterceptor>(client.interceptors.size() + requestInterceptors.size());
		l.addAll(client.interceptors);
		l.addAll(requestInterceptors);
		return l;
	}

	@SuppressWarnings({
		"java:S3776" // Request-assembly branching is intentional and cohesive; refactoring would reduce readability without benefit.
	})
	private TransportRequest buildTransportRequest() {
		var resolvedUrl = applyPathSubstitutions(url);
		resolvedUri = appendQuery(resolvedUrl);

		var builder = TransportRequest.builder()
			.method(method)
			.uri(resolvedUri)
			.timeout(timeout);

		// Headers
		for (var h : headers) {
			var v = h.getValue();
			if (v != null)
				builder.header(h.getName(), v);
		}

		if (client.parsers != null || client.defaultParser != null) {
			var hasAccept = headers.stream().anyMatch(h -> "Accept".equalsIgnoreCase(h.getName()));
			if (! hasAccept) {
				var accept = client.getDefaultAccept();
				if (accept != null)
					builder.header("Accept", accept);
			}
		}

		// A Content-Type already present in the header list (a caller-supplied @Header param, or a constant header)
		// wins over the body's own content type, and is never duplicated by it (dedup invariant).
		var hasContentType = hasHeader("Content-Type");

		// Pre-converted body from body(Object) takes priority
		if (convertedBody != null) {
			if (convertedBody.getContentType() != null && ! hasContentType)
				builder.header("Content-Type", convertedBody.getContentType());
			builder.body(convertedBody);
		} else if (!formData.isEmpty() && body == null) {
			// Form body when form data is present and no explicit body was set
			var formBody = buildFormBody();
			if (! hasContentType)
				builder.header("Content-Type", formBody.getContentType());
			builder.body(TransportBody.of(formBody));
		} else if (body != null) {
			if (body.getContentType() != null && ! hasContentType)
				builder.header("Content-Type", body.getContentType());
			builder.body(TransportBody.of(body));
		}

		return builder.build();
	}

	private String applyPathSubstitutions(String template) {
		var result = template;
		Object remainder = null;
		for (var entry : pathData.entrySet()) {
			if ("/*".equals(entry.getKey())) {
				remainder = entry.getValue();  // @PathRemainder — applied after named substitutions
				continue;
			}
			var replacement = entry.getValue() != null ? entry.getValue().toString() : "";
			result = result.replace("{" + entry.getKey() + "}", urlEncode(replacement));
		}
		if (remainder != null) {
			var r = remainder.toString();
			if (! r.isEmpty()) {
				if (result.endsWith("/*"))
					result = result.substring(0, result.length() - 2);
				if (! result.endsWith("/"))
					result += "/";
				result += urlEncodePath(r);
			}
		}
		return result;
	}

	/** URL-encodes a path remainder while preserving {@code /} segment separators. */
	private static String urlEncodePath(String value) {
		var segments = value.split("/", -1);
		var sb = new StringBuilder();
		for (var i = 0; i < segments.length; i++) {
			if (i > 0)
				sb.append('/');
			sb.append(urlEncode(segments[i]));
		}
		return sb.toString();
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
