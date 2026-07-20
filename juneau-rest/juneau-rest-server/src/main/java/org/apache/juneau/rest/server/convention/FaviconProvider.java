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
package org.apache.juneau.rest.server.convention;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.response.*;

/**
 * Flavor-neutral favicon worker bean shared by the favicon {@code @Rest} flavors.
 *
 * <p>
 * Holds the capability state (the icon bytes + {@code Cache-Control} header value) and the serve
 * logic (build an {@link HttpResource} with {@code Content-Type: image/x-icon} + the cache header).
 * The {@link FaviconMixin} (mixin), {@link FaviconResource} (child), and {@link FaviconServlet}
 * (servlet) flavors are independent {@code @Rest} classes that each hold a {@code FaviconProvider} worker
 * and delegate to it &mdash; so the three deployment forms cannot drift, and no flavor is another
 * flavor's worker.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FaviconMixin}
 * 	<li class='jc'>{@link FaviconResource}
 * 	<li class='jc'>{@link FaviconServlet}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
public class FaviconProvider {

	/** Default {@code Cache-Control} header value: {@code max-age=2592000, public} (30 days). */
	public static final String DEFAULT_CACHE_CONTROL = "max-age=2592000, public";

	/** Classpath location of the framework-shipped default favicon. */
	private static final String DEFAULT_FAVICON_RESOURCE = "/juneau-favicon.ico";

	/**
	 * Creates a new builder for configuring a {@link FaviconProvider}.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final byte[] bytes;
	private final String cacheControl;

	/**
	 * No-arg constructor &mdash; loads the default Juneau-branded favicon from the classpath.
	 */
	public FaviconProvider() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be <jk>null</jk>.
	 */
	protected FaviconProvider(Builder builder) {
		bytes = builder.resolveBytes();
		cacheControl = builder.cacheControl;
	}

	/**
	 * Serves the configured favicon bytes.
	 *
	 * @return The favicon as an {@link HttpResource} with {@code Content-Type: image/x-icon} and the
	 * 	configured {@code Cache-Control} header.
	 */
	public HttpResource serve() {
		List<HttpHeader> hdrs = list(ContentType.of("image/x-icon"), CacheControl.of(cacheControl));
		return HttpResourceBean.of(ByteArrayBody.of(bytes, "image/x-icon"), hdrs);
	}

	/**
	 * Builder for {@link FaviconProvider} instances.
	 */
	public static class Builder {

		private byte[] bytes;
		private String classpath;
		private String cacheControl = DEFAULT_CACHE_CONTROL;

		/** Constructor &mdash; package access for {@link FaviconProvider#create()}. */
		protected Builder() {}

		/**
		 * Sets the raw favicon bytes.
		 *
		 * <p>
		 * Mutually exclusive with {@link #classpath(String)} &mdash; whichever is set last wins
		 * at {@link #build()} time.
		 *
		 * @param value The favicon bytes (typically an {@code .ico} or {@code .png} payload).
		 * 	Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder bytes(byte[] value) {
			bytes = value;
			classpath = null;
			return this;
		}

		/**
		 * Sets the classpath resource path from which to load the favicon bytes.
		 *
		 * <p>
		 * Resolved via {@link Class#getResourceAsStream(String) FaviconProvider.class.getResourceAsStream(...)}
		 * at {@link #build()} time. A resolved-to-{@code null} stream falls back to the framework's
		 * default favicon. Mutually exclusive with {@link #bytes(byte[])}.
		 *
		 * @param value The classpath resource path (e.g. {@code "/myapp/icon.ico"}).
		 * 	Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder classpath(String value) {
			classpath = value;
			bytes = null;
			return this;
		}

		/**
		 * Sets the {@code Cache-Control} header value emitted with the favicon response.
		 *
		 * <p>
		 * Defaults to {@value #DEFAULT_CACHE_CONTROL} (30 days).
		 *
		 * @param value The new {@code Cache-Control} value. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder cacheControl(String value) {
			cacheControl = value;
			return this;
		}

		/**
		 * Builds a {@link FaviconProvider} instance.
		 *
		 * @return A configured instance.
		 */
		public FaviconProvider build() {
			return new FaviconProvider(this);
		}

		byte[] resolveBytes() {
			if (bytes != null)
				return bytes;
			if (classpath != null) {
				var resolved = readClasspath(classpath);
				if (resolved != null)
					return resolved;
			}
			// Falls through to the framework's default-shipping ICO; the resource is shipped in
			// the same jar as this class so it must be present at runtime.
			return readClasspath(DEFAULT_FAVICON_RESOURCE);
		}

		@SuppressWarnings({
			"java:S1168" // null is a meaningful "resource not found" sentinel: resolveBytes() falls through to the default favicon; an empty array would suppress that fallback.
		})
		private static byte[] readClasspath(String path) {
			try (var in = FaviconProvider.class.getResourceAsStream(path)) {
				if (in == null)
					return null;
				return in.readAllBytes();
			} catch (IOException e) {
				// readAllBytes on a classpath resource is effectively unreachable; the catch is
				// here only to satisfy the checked exception contract.
				throw new InternalServerError(e);
			}
		}
	}
}
