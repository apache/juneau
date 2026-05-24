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
package org.apache.juneau.rest.convention;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Mixin that serves a {@code favicon.ico} icon at {@code /favicon.ico}.
 *
 * <p>
 * Sibling of {@link BasicSeoResource} (robots.txt / sitemap.xml), {@link BasicVersionResource}
 * ({@code /version} / {@code /info} / {@code /about}), and {@link BasicWellKnownResource}
 * ({@code /.well-known/*}). All four classes live in the {@code org.apache.juneau.rest.convention}
 * convention-endpoints mixin pack.
 *
 * <p>
 * Compose into a host resource via {@link Rest#mixins() @Rest(mixins=BasicFaviconResource.class)};
 * the {@code /favicon.ico} URL becomes available alongside the host's own endpoints with no further
 * wiring. Or extend the class directly for a standalone deployment whose mount paths come from the
 * inherited {@link Rest#paths() @Rest(paths)} default.
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins=BasicFaviconResource.<jk>class</jk>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet {
 * 		<jc>// Use the default Juneau-branded favicon.</jc>
 * 	}
 *
 * 	<jc>// Or override the icon bytes via a @Bean factory:</jc>
 * 	<ja>@Bean BasicFaviconResource favicon()</ja> {
 * 		<jk>return</jk> BasicFaviconResource.<jsm>create</jsm>().bytes(myLogoBytes).build();
 * 	}
 * </p>
 *
 * <h5 class='section'>Behavior:</h5>
 *
 * <ul class='spaced-list'>
 * 	<li>{@code GET /favicon.ico} returns the configured icon bytes with
 * 		{@code Content-Type: image/x-icon} and {@code Cache-Control: max-age=2592000, public}
 * 		(30 days &mdash; favicons rarely change and browsers re-fetch frequently when uncached).
 * 	<li>The default icon ({@code juneau-favicon.ico} on the framework classpath) is a
 * 		16&times;16 Juneau-branded ICO; users replace it by registering an alternate
 * 		{@code @Bean BasicFaviconResource} whose builder supplies different bytes.
 * 	<li>The handler is excluded from generated Swagger / OpenAPI specs via
 * 		{@link OpSwagger#ignore() @OpSwagger(ignore=true)} &mdash; favicons are not
 * 		API-meaningful.
 * </ul>
 *
 * <h5 class='section'>Builder API:</h5>
 *
 * <ul class='spaced-list'>
 * 	<li>{@link #create() create()} &mdash; entry point for configuring an instance.
 * 	<li>{@link Builder#bytes(byte[]) bytes(byte[])} &mdash; raw favicon bytes.
 * 	<li>{@link Builder#classpath(String) classpath(String)} &mdash; load icon bytes from a
 * 		classpath resource (resolved against the {@code BasicFaviconResource} classloader).
 * 	<li>{@link Builder#cacheControl(String) cacheControl(String)} &mdash; override the default
 * 		30-day {@code Cache-Control} header.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicSeoResource}
 * 	<li class='jc'>{@link BasicVersionResource}
 * 	<li class='jc'>{@link BasicWellKnownResource}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest(paths={"/favicon.ico"})
public class BasicFaviconResource {

	/** Default {@code Cache-Control} header value: {@code max-age=2592000, public} (30 days). */
	public static final String DEFAULT_CACHE_CONTROL = "max-age=2592000, public";

	/** Classpath location of the framework-shipped default favicon. */
	private static final String DEFAULT_FAVICON_RESOURCE = "/juneau-favicon.ico";

	/**
	 * Creates a new builder for configuring a {@link BasicFaviconResource}.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final byte[] bytes;
	private final String cacheControl;

	/**
	 * No-arg constructor &mdash; used when a host registers the mixin without supplying a
	 * builder-configured {@code @Bean BasicFaviconResource}. Loads the default Juneau-branded
	 * favicon from the classpath.
	 */
	public BasicFaviconResource() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder.
	 */
	protected BasicFaviconResource(Builder builder) {
		bytes = builder.resolveBytes();
		cacheControl = builder.cacheControl;
	}

	/**
	 * [GET /favicon.ico] &mdash; serve the configured favicon bytes.
	 *
	 * @return The favicon as an {@link HttpResource} with proper headers.
	 */
	@RestGet(
		path="/favicon.ico",
		summary="Favorites icon",
		description="Browser favorites icon (favicon.ico).",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getFavicon() {
		var hdrs = new ArrayList<HttpHeader>();
		hdrs.add(ContentType.of("image/x-icon"));
		hdrs.add(CacheControl.of(cacheControl));
		return HttpResourceBean.of(ByteArrayBody.of(bytes, "image/x-icon"), hdrs);
	}

	/**
	 * Builder for {@link BasicFaviconResource} instances.
	 */
	public static class Builder {

		private byte[] bytes;
		private String classpath;
		private String cacheControl = DEFAULT_CACHE_CONTROL;

		/** Constructor &mdash; package access for {@link BasicFaviconResource#create()}. */
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
		 * Resolved via {@link Class#getResourceAsStream(String) BasicFaviconResource.class.getResourceAsStream(...)}
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
		 * Builds a {@link BasicFaviconResource} instance.
		 *
		 * @return A configured instance.
		 */
		public BasicFaviconResource build() {
			return new BasicFaviconResource(this);
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

		private static byte[] readClasspath(String path) {
			try (var in = BasicFaviconResource.class.getResourceAsStream(path)) {
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
