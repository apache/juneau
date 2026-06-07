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
package org.apache.juneau.rest.server.ops;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin that serves a request-echo / round-trip introspection endpoint at {@code /echo/*}
 * (configurable).
 *
 * <p>
 * Sibling of {@link AdminMixin} ({@code /admin/*}) and {@link RouteIndexMixin}
 * ({@code /options}, {@code /routes}). All three classes live in the
 * {@code org.apache.juneau.rest.server.ops} ops/introspection mixin pack.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=EchoMixin.class)}; the {@code /echo/*} URL becomes
 * available alongside the host's own endpoints with no further wiring.
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /echo/*} can be overridden via the SVL variable
 * {@code ${juneau.echo.path:echo}} &mdash; set via system property
 * ({@code -Djuneau.echo.path=introspect}), environment variable
 * ({@code JUNEAU_ECHO_PATH=introspect}), or {@code Config} key
 * ({@code juneau.echo.path = introspect}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time (SVL resolution in {@code @RestOp(path)}).
 *
 * <p>
 * Override accepts bare token ({@code echo}), leading slash ({@code /echo}), trailing slash
 * ({@code echo/}), or wildcard suffix ({@code /echo/*}) &mdash; all resolve to the same mount.
 *
 * <p>
 * <b>Migration note (10.0.0):</b> Earlier development snapshots of this mixin mounted at both
 * {@code /echo/*} <i>and</i> {@code /debug/echo/*} as historical aliases on a single op. That
 * dual default has been collapsed to a single SVL-configurable mount as part of the
 * "single path per op" principle. Deployers who relied on the
 * {@code /debug/echo/*} alias must now either set {@code -Djuneau.echo.path=debug/echo} or
 * compose a second instance with the override.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestOp @RestOp(path="/&#123;juneau.echo.path:echo&#125;/*")} on
 * {@link #echo}; a class-level {@code @Rest(paths=...)} declaration would be silently ignored
 * under the mixin pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(
 * 		path=<js>"/api"</js>,
 * 		mixins=EchoMixin.<jk>class</jk>,
 * 		debug=<js>"conditional"</js>          <jc>// gates the echo endpoint per request</jc>
 * 	)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet { }
 *
 * 	<jc>// Optional: tighten the body cap or adjust redacted headers via a @Bean factory:</jc>
 * 	<ja>@Bean</ja> EchoMixin echo() {
 * 		<jk>return</jk> EchoMixin.<jsm>create</jsm>()
 * 			.bodyLimit(64 * 1024L)
 * 			.redactHeader(<js>"X-Internal-Trace"</js>)
 * 			.build();
 * 	}
 * </p>
 *
 * <h5 class='section'>Debug gating:</h5>
 *
 * <p>
 * The handler is gated behind the host's
 * {@link RestContext#getDebugEnablement() DebugEnablement} chain &mdash; the same mechanism that
 * powers {@code @Rest(debug=...)}. When {@code Debug} resolves to {@code OFF} for the current
 * request, the handler returns {@code 404 Not Found} (so the existence of the endpoint isn't
 * disclosed). When debug is {@code ALWAYS}, or {@code CONDITIONAL} with the {@code Debug: true}
 * request header, the full echo payload is returned. The recommended posture for production
 * deployments is {@code @Rest(debug="conditional")} paired with a guard chain so only authorized
 * operators can flip the {@code Debug} header.
 *
 * <h5 class='section'>Sensitive-header redaction:</h5>
 *
 * <p>
 * Token-bearing headers must never be reflected back to the caller (would defeat any auth scheme
 * in front of the endpoint). The default redacted list is &mdash; case-insensitively &mdash;
 * {@code Authorization}, {@code Cookie}, {@code Set-Cookie}, {@code Proxy-Authorization}, and
 * {@code X-API-Key}. Each redacted header surfaces in the echo body with the literal value
 * {@value #REDACTED} so the caller can see the header was present without leaking its value.
 * Override the default list via {@link Builder#redactedHeaders(String...)} (replaces) or extend
 * via {@link Builder#redactHeader(String)} (additive).
 *
 * <h5 class='section'>Body capture and truncation:</h5>
 *
 * <p>
 * The handler reads up to {@link Builder#bodyLimit(long) bodyLimit} bytes (default
 * {@value #DEFAULT_BODY_LIMIT_DOC} = 1 MB) of the inbound body and emits it as a UTF-8 string in
 * the {@code content} field of the JSON response. When the body exceeds the cap, the captured
 * portion is truncated and the {@code truncated} flag is set to {@code true} so callers can see
 * the response is incomplete.
 *
 * <h5 class='section'>Response shape:</h5>
 *
 * <p class='bjson'>
 * 	{
 * 		<jok>"method"</jok>: <jov>"POST"</jov>,
 * 		<jok>"path"</jok>: <jov>"/echo/foo/bar"</jov>,
 * 		<jok>"queryString"</jok>: <jov>"x=1"</jov>,
 * 		<jok>"pathRemainder"</jok>: <jov>"foo/bar"</jov>,
 * 		<jok>"headers"</jok>: { <jok>"User-Agent"</jok>: <jov>"curl"</jov>, <jok>"Authorization"</jok>: <jov>"[REDACTED]"</jov> },
 * 		<jok>"queryParams"</jok>: { <jok>"x"</jok>: <jov>"1"</jov> },
 * 		<jok>"attributes"</jok>: { },
 * 		<jok>"contentLength"</jok>: <jov>5</jov>,
 * 		<jok>"content"</jok>: <jov>"hello"</jov>,
 * 		<jok>"truncated"</jok>: <jov>false</jov>
 * 	}
 * </p>
 *
 * <p>
 * The endpoint is excluded from generated Swagger / OpenAPI specs via
 * {@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AdminMixin}
 * 	<li class='jc'>{@link RouteIndexMixin}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
public class EchoMixin {

	/** Sentinel value emitted in place of redacted header values. */
	public static final String REDACTED = "[REDACTED]";

	/** Default body capture cap, in bytes (1&nbsp;MB). */
	public static final long DEFAULT_BODY_LIMIT = 1_048_576L;

	/** Default body capture cap rendered for the class-level javadoc &mdash; do not use programmatically. */
	static final String DEFAULT_BODY_LIMIT_DOC = "1048576";

	/** Default redacted-header set (case-insensitive lookup). */
	public static final Set<String> DEFAULT_REDACTED_HEADERS = Set.of(
		"Authorization", "Cookie", "Set-Cookie", "Proxy-Authorization", "X-API-Key");

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final long bodyLimit;
	private final Set<String> redactedHeadersLower;

	/** No-arg constructor &mdash; uses {@link #DEFAULT_BODY_LIMIT} and {@link #DEFAULT_REDACTED_HEADERS}. */
	public EchoMixin() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder.
	 */
	protected EchoMixin(Builder builder) {
		bodyLimit = builder.bodyLimit;
		var s = new LinkedHashSet<String>();
		for (var h : builder.redactedHeaders)
			s.add(h.toLowerCase(Locale.ROOT));
		redactedHeadersLower = Collections.unmodifiableSet(s);
	}

	/**
	 * [* /echo/*] &mdash; emit an introspection echo of the inbound request.
	 *
	 * <p>
	 * Returns {@code 404 Not Found} when {@code Debug} is not enabled for the current request.
	 *
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @param remainder The path remainder after the mount prefix (multi-segment, may be empty).
	 * @throws IOException If an I/O error occurs while reading the request or writing the response.
	 * @throws NotFound When {@code Debug} resolves to {@code OFF} for the current request.
	 */
	@RestOp(
		method="*",
		path="/#{pathToken(${juneau.echo.path:echo})}/*",
		summary="Request echo",
		description="Round-trip introspection of the inbound request. Debug-gated.",
		swagger=@OpSwagger(ignore=true)
	)
	public void echo(RestRequest req, RestResponse res, @Path("/*") String remainder) throws IOException {
		var ctx = req.getContext();
		var de = ctx.getDebugEnablement();
		var sreq = req.getHttpServletRequest();
		if (de == null || ! de.isDebug(ctx, sreq))
			throw new NotFound("Echo endpoint disabled (Debug not enabled).");

		var headers = new LinkedHashMap<String,String>();
		var names = sreq.getHeaderNames();
		while (names != null && names.hasMoreElements()) {
			var name = names.nextElement();
			var key = name.toLowerCase(Locale.ROOT);
			headers.put(name, redactedHeadersLower.contains(key) ? REDACTED : sreq.getHeader(name));
		}

		var queryParams = new LinkedHashMap<String,String>();
		for (var p : req.getQueryParams())
			queryParams.put(p.getName(), p.asString().orElse(null));

		var attributes = new LinkedHashMap<String,String>();
		req.getAttributes().asMap().forEach((k, v) -> attributes.put(k, String.valueOf(v)));

		var capture = readBoundedBody(sreq.getInputStream(), bodyLimit);

		var out = new LinkedHashMap<String,Object>();
		out.put("method", req.getMethod());
		out.put("path", sreq.getRequestURI());
		out.put("queryString", sreq.getQueryString());
		out.put("pathRemainder", remainder == null ? "" : remainder);
		out.put("headers", headers);
		out.put("queryParams", queryParams);
		out.put("attributes", attributes);
		out.put("contentLength", capture.bytesRead);
		if (capture.bytesRead > 0)
			out.put("content", new String(capture.bytes, StandardCharsets.UTF_8));
		out.put("truncated", capture.truncated);

		try (var w = res.getDirectWriter("application/json")) {
			JsonSerializer.DEFAULT_READABLE.serialize(out, w);
		}
	}

	/**
	 * Returns the configured body-capture cap (test/inspection helper).
	 *
	 * @return The body capture cap, in bytes.
	 */
	public long getBodyLimit() {
		return bodyLimit;
	}

	/**
	 * Returns the redacted-header set as lowercased, immutable strings (test/inspection helper).
	 *
	 * @return The redacted-header set.
	 */
	public Set<String> getRedactedHeadersLower() {
		return redactedHeadersLower;
	}

	@SuppressWarnings({
		"java:S135" // The two breaks are distinct truncation exits (limit-reached vs. partial-final-chunk); merging them would obscure the bounded-read logic.
	})
	private static BodyCapture readBoundedBody(InputStream in, long limit) throws IOException {
		if (in == null)
			return new BodyCapture(new byte[0], 0, false);
		var buf = new ByteArrayOutputStream();
		var chunk = new byte[8192];
		var truncated = false;
		long total = 0;
		int n;
		while ((n = in.read(chunk)) != -1) {
			var room = limit - total;
			if (room <= 0) {
				truncated = true;
				break;
			}
			if (n > room) {
				buf.write(chunk, 0, (int) room);
				total += room;
				truncated = true;
				break;
			}
			buf.write(chunk, 0, n);
			total += n;
		}
		return new BodyCapture(buf.toByteArray(), total, truncated);
	}

	private static final class BodyCapture {
		final byte[] bytes;
		final long bytesRead;
		final boolean truncated;

		BodyCapture(byte[] bytes, long bytesRead, boolean truncated) {
			this.bytes = bytes;
			this.bytesRead = bytesRead;
			this.truncated = truncated;
		}
	}

	/**
	 * Builder for {@link EchoMixin} instances.
	 */
	public static class Builder {

		private long bodyLimit = DEFAULT_BODY_LIMIT;
		private final Set<String> redactedHeaders;

		/** Constructor &mdash; package access for {@link EchoMixin#create()}. */
		protected Builder() {
			redactedHeaders = new LinkedHashSet<>(DEFAULT_REDACTED_HEADERS);
		}

		/**
		 * Sets the body capture cap.
		 *
		 * <p>
		 * Captured content is truncated when the inbound body exceeds this size; the
		 * {@code truncated} flag in the response is set to {@code true}.
		 *
		 * @param value Cap, in bytes. Must be {@code >= 0}.
		 * @return This object.
		 */
		public Builder bodyLimit(long value) {
			if (value < 0)
				throw new IllegalArgumentException("bodyLimit must be >= 0");
			bodyLimit = value;
			return this;
		}

		/**
		 * Replaces the redacted-header set with the supplied values.
		 *
		 * <p>
		 * Header names are matched case-insensitively. Pass an empty array to disable redaction
		 * (not recommended outside of integration tests).
		 *
		 * @param values The header names to redact.
		 * @return This object.
		 */
		public Builder redactedHeaders(String...values) {
			redactedHeaders.clear();
			if (values != null)
				for (var v : values)
					if (v != null && ! v.isBlank())
						redactedHeaders.add(v);
			return this;
		}

		/**
		 * Adds an additional header name to the redacted-header set.
		 *
		 * @param value The header name to redact (case-insensitive). Must not be {@code null} or
		 * 	blank.
		 * @return This object.
		 */
		public Builder redactHeader(String value) {
			if (isBlank(value))
				throw new IllegalArgumentException("Argument 'value' must not be null or blank");
			redactedHeaders.add(value);
			return this;
		}

		/**
		 * Builds a {@link EchoMixin} instance.
		 *
		 * @return A configured instance.
		 */
		public EchoMixin build() {
			return new EchoMixin(this);
		}
	}
}
