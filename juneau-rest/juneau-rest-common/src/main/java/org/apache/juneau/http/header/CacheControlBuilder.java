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
package org.apache.juneau.http.header;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.time.*;
import java.util.*;

/**
 * Fluent builder for assembling an HTTP <c>Cache-Control</c> header value.
 *
 * <p>
 * Composes the directives defined by
 * <a class='doclink' href='https://www.rfc-editor.org/rfc/rfc9111#name-cache-control'>RFC 9111 §5.2</a> into the
 * comma-separated wire format expected by the {@code Cache-Control} response header. Mutually exclusive
 * cacheability directives (e.g. {@link #publicCache()} vs {@link #privateCache()}) overwrite one another;
 * boolean directives (e.g. {@link #noStore()}) are idempotent.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	String <jv>value</jv> = CacheControlBuilder.<jsm>create</jsm>()
 * 		.publicCache()
 * 		.maxAge(3600)
 * 		.mustRevalidate()
 * 		.build();
 * 	<jc>// =&gt; "public, max-age=3600, must-revalidate"</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link CacheControl}
 * 	<li class='extlink'><a class='doclink' href='https://www.rfc-editor.org/rfc/rfc9111'>RFC 9111 - HTTP Caching</a>
 * </ul>
 *
 * @since 9.5.0
 */
@SuppressWarnings({
	"java:S115" // ARG_xxx constants use camelCase after prefix intentionally (constructor arg name keys, not enum-style constants)
})
public class CacheControlBuilder {

	private static final String ARG_value = "value";

	private String cacheability;        // "public" or "private" or null
	private boolean noCache;
	private boolean noStore;
	private boolean noTransform;
	private boolean mustRevalidate;
	private boolean proxyRevalidate;
	private boolean immutable;
	private Long maxAge;
	private Long sMaxAge;
	private Long staleWhileRevalidate;
	private Long staleIfError;
	private final List<String> extensions = new ArrayList<>();

	/**
	 * Creates a new empty builder.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static CacheControlBuilder create() {
		return new CacheControlBuilder();
	}

	/**
	 * Adds the <c>public</c> directive (responses may be cached by any cache).
	 *
	 * <p>
	 * Clears any previously set <c>private</c> directive.
	 *
	 * @return This object.
	 */
	public CacheControlBuilder publicCache() {
		cacheability = "public";
		return this;
	}

	/**
	 * Adds the <c>private</c> directive (responses may only be cached by the originating client).
	 *
	 * <p>
	 * Clears any previously set <c>public</c> directive.
	 *
	 * @return This object.
	 */
	public CacheControlBuilder privateCache() {
		cacheability = "private";
		return this;
	}

	/**
	 * Adds the <c>no-cache</c> directive.
	 *
	 * <p>
	 * Forces caches to revalidate with the origin server before reusing a stored response.
	 *
	 * @return This object.
	 */
	public CacheControlBuilder noCache() {
		noCache = true;
		return this;
	}

	/**
	 * Adds the <c>no-store</c> directive.
	 *
	 * <p>
	 * Forbids caches from storing any part of the request or response.
	 *
	 * @return This object.
	 */
	public CacheControlBuilder noStore() {
		noStore = true;
		return this;
	}

	/**
	 * Adds the <c>no-transform</c> directive.
	 *
	 * <p>
	 * Forbids intermediaries from transforming the response payload.
	 *
	 * @return This object.
	 */
	public CacheControlBuilder noTransform() {
		noTransform = true;
		return this;
	}

	/**
	 * Adds the <c>must-revalidate</c> directive.
	 *
	 * <p>
	 * Forces caches to revalidate stale responses with the origin before serving them.
	 *
	 * @return This object.
	 */
	public CacheControlBuilder mustRevalidate() {
		mustRevalidate = true;
		return this;
	}

	/**
	 * Adds the <c>proxy-revalidate</c> directive (must-revalidate for shared caches only).
	 *
	 * @return This object.
	 */
	public CacheControlBuilder proxyRevalidate() {
		proxyRevalidate = true;
		return this;
	}

	/**
	 * Adds the <c>immutable</c> directive.
	 *
	 * <p>
	 * Signals that the response body will not change for the duration of its freshness lifetime.
	 *
	 * @return This object.
	 */
	public CacheControlBuilder immutable() {
		immutable = true;
		return this;
	}

	/**
	 * Adds the <c>max-age=N</c> directive (seconds).
	 *
	 * @param seconds Maximum freshness lifetime in seconds. Must be non-negative.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code seconds} is negative.
	 */
	public CacheControlBuilder maxAge(long seconds) {
		if (seconds < 0)
			throw illegalArg("max-age must be non-negative: {0}", seconds);
		maxAge = seconds;
		return this;
	}

	/**
	 * Adds the <c>max-age=N</c> directive from a {@link Duration}.
	 *
	 * @param value Maximum freshness lifetime. Must not be <jk>null</jk> or negative.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or negative.
	 */
	public CacheControlBuilder maxAge(Duration value) {
		assertArgNotNull(ARG_value, value);
		return maxAge(value.getSeconds());
	}

	/**
	 * Adds the <c>s-maxage=N</c> directive (shared-cache max-age in seconds).
	 *
	 * @param seconds Maximum freshness lifetime in seconds for shared caches. Must be non-negative.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code seconds} is negative.
	 */
	public CacheControlBuilder sMaxAge(long seconds) {
		if (seconds < 0)
			throw illegalArg("s-maxage must be non-negative: {0}", seconds);
		sMaxAge = seconds;
		return this;
	}

	/**
	 * Adds the <c>s-maxage=N</c> directive from a {@link Duration}.
	 *
	 * @param value Maximum freshness lifetime for shared caches. Must not be <jk>null</jk> or negative.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or negative.
	 */
	public CacheControlBuilder sMaxAge(Duration value) {
		assertArgNotNull(ARG_value, value);
		return sMaxAge(value.getSeconds());
	}

	/**
	 * Adds the <c>stale-while-revalidate=N</c> directive (seconds).
	 *
	 * @param seconds Number of seconds a cache may serve a stale response while asynchronously revalidating.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code seconds} is negative.
	 */
	public CacheControlBuilder staleWhileRevalidate(long seconds) {
		if (seconds < 0)
			throw illegalArg("stale-while-revalidate must be non-negative: {0}", seconds);
		staleWhileRevalidate = seconds;
		return this;
	}

	/**
	 * Adds the <c>stale-if-error=N</c> directive (seconds).
	 *
	 * @param seconds Number of seconds a cache may serve a stale response when the origin is unreachable.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code seconds} is negative.
	 */
	public CacheControlBuilder staleIfError(long seconds) {
		if (seconds < 0)
			throw illegalArg("stale-if-error must be non-negative: {0}", seconds);
		staleIfError = seconds;
		return this;
	}

	/**
	 * Adds an arbitrary cache-control extension directive (e.g. <c>"community=\"UCI\""</c>).
	 *
	 * <p>
	 * Extensions are appended in registration order at the end of the directive list. The caller is responsible for
	 * supplying a token that conforms to the <c>cache-directive</c> grammar of RFC 9111 §5.2.3.
	 *
	 * @param value The extension token. Must not be <jk>null</jk> or blank.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
	 */
	public CacheControlBuilder extension(String value) {
		assertArgNotNull(ARG_value, value);
		var v = value.trim();
		if (v.isEmpty())
			throw illegalArg("cache-control extension must not be blank");
		extensions.add(v);
		return this;
	}

	/**
	 * Builds the comma-separated <c>Cache-Control</c> header value.
	 *
	 * <p>
	 * Returns an empty string when no directives have been registered.
	 *
	 * @return The header value. Never <jk>null</jk>.
	 */
	public String build() {
		var parts = new ArrayList<String>(12);
		if (cacheability != null)
			parts.add(cacheability);
		if (noCache)
			parts.add("no-cache");
		if (noStore)
			parts.add("no-store");
		if (noTransform)
			parts.add("no-transform");
		if (mustRevalidate)
			parts.add("must-revalidate");
		if (proxyRevalidate)
			parts.add("proxy-revalidate");
		if (immutable)
			parts.add("immutable");
		if (maxAge != null)
			parts.add("max-age=" + maxAge);
		if (sMaxAge != null)
			parts.add("s-maxage=" + sMaxAge);
		if (staleWhileRevalidate != null)
			parts.add("stale-while-revalidate=" + staleWhileRevalidate);
		if (staleIfError != null)
			parts.add("stale-if-error=" + staleIfError);
		parts.addAll(extensions);
		return String.join(", ", parts);
	}

	/**
	 * Builds a {@link CacheControl} header bean directly from this builder.
	 *
	 * @return A {@link CacheControl} carrying {@link #build()}. Never <jk>null</jk>.
	 */
	public CacheControl toHeader() {
		return CacheControl.of(build());
	}

	@Override
	public String toString() {
		return build();
	}
}
