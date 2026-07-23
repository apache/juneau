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

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Mixin that serves a {@code favicon.ico} icon at {@code /favicon.ico}.
 *
 * <p>
 * Sibling of {@link SeoMixin} (robots.txt / sitemap.xml), {@link VersionMixin}
 * ({@code /version}), and {@link WellKnownMixin}
 * ({@code /.well-known/*}). All four classes live in the {@code org.apache.juneau.rest.server.convention}
 * convention-endpoints mixin pack.
 *
 * <p>
 * Compose into a host resource via {@link Rest#mixins() @Rest(mixins=FaviconMixin.class)};
 * the {@code /favicon.ico} URL becomes available alongside the host's own endpoints with no further
 * wiring.
 *
 * <h5 class='section'>Hardcoded mount path:</h5>
 *
 * <p>
 * Unlike the sibling api-docs and ops mixins (see {@link org.apache.juneau.rest.server.docs.SwaggerMixin},
 * {@link VersionMixin}, etc.) the mount path here is <b>not</b> SVL-configurable
 * &mdash; {@code /favicon.ico} is fixed by browser convention and the {@code link rel="icon"}
 * default the HTML spec inherits from <a href="https://html.spec.whatwg.org/multipage/links.html#rel-icon">WHATWG HTML</a>.
 * Browsers fetch {@code /favicon.ico} from the site root regardless of any application-level
 * routing rewrites, so a runtime mount-path override here would have no practical effect.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/favicon.ico")} on {@link #getFavicon};
 * a class-level {@code @Rest(paths=...)} declaration would be silently ignored under the mixin
 * pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins=FaviconMixin.<jk>class</jk>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet {
 * 		<jc>// Use the default Juneau-branded favicon.</jc>
 * 	}
 *
 * 	<jc>// Or override the icon bytes via a @Bean factory:</jc>
 * 	<ja>@Bean FaviconMixin favicon()</ja> {
 * 		<jk>return</jk> FaviconMixin.<jsm>create</jsm>().bytes(myLogoBytes).build();
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
 * 		{@code @Bean FaviconMixin} whose builder supplies different bytes.
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
 * 		classpath resource (resolved against the {@code FaviconMixin} classloader).
 * 	<li>{@link Builder#cacheControl(String) cacheControl(String)} &mdash; override the default
 * 		30-day {@code Cache-Control} header.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link SeoMixin}
 * 	<li class='jc'>{@link VersionMixin}
 * 	<li class='jc'>{@link WellKnownMixin}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
public class FaviconMixin extends RestMixin {

	/** Default {@code Cache-Control} header value: {@code max-age=2592000, public} (30 days). */
	public static final String DEFAULT_CACHE_CONTROL = FaviconProvider.DEFAULT_CACHE_CONTROL;

	/**
	 * Creates a new builder for configuring a {@link FaviconMixin}.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final FaviconProvider worker;

	/**
	 * No-arg constructor &mdash; used when a host registers the mixin without supplying a
	 * builder-configured {@code @Bean FaviconMixin}. Delegates to a default {@link FaviconProvider} worker
	 * (the framework-shipped Juneau-branded favicon).
	 */
	public FaviconMixin() {
		this(new FaviconProvider());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared {@link FaviconProvider} worker this flavor delegates to. Must not be
	 * 	<jk>null</jk>.
	 */
	protected FaviconMixin(FaviconProvider worker) {
		this.worker = worker;
	}

	/**
	 * Worker + builder constructor.
	 *
	 * <p>
	 * Used by {@link Builder#build()} to both delegate to the shared {@link FaviconProvider} worker and stash the
	 * programmatic {@link RestBuilder}(carrying any {@code @Rest}-level overrides such as {@code path}) so those
	 * values take precedence over the mixin class's own {@link Rest @Rest} annotation.
	 *
	 * <h5 class='section'>Why worker + builder, not the flavor Builder:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Takes the already-built <b>worker bean</b> (not the flavor {@link Builder}) so the flavor can be
	 * 		constructed from ANY independently-supplied worker &mdash; e.g. a user's own {@code @Bean FaviconProvider}
	 * 		or BeanStore bean, per the delegate-bean model &mdash; not only via this flavor's own builder.
	 * 	<li>Takes the generic {@link RestBuilder}(here {@code this} from {@link Builder#build()}) so it honors the
	 * 		uniform &sect;2.4 {@code Foo(RestBuilder<?>)} injection contract the base class and DI resolution key on; the
	 * 		base knows nothing about the concrete flavor builder or the worker type.
	 * 	<li>Holds the finished worker product (the {@code final} {@link FaviconProvider} field), not a transient
	 * 		builder; the worker is materialized exactly once at {@link Builder#build()} time.
	 * 	<li>Keeps the capability worker and the REST-level config as two distinct inputs.
	 * </ul>
	 *
	 * @param worker The shared {@link FaviconProvider} worker this flavor delegates to. Must not be <jk>null</jk>.
	 * @param builder The programmatic configuration builder. May be <jk>null</jk>.
	 */
	protected FaviconMixin(FaviconProvider worker, RestBuilder<?> builder) {
		super(builder);
		this.worker = worker;
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
		return worker.serve();
	}

	/**
	 * Builder for {@link FaviconMixin} instances.
	 *
	 * <p>
	 * Mirrors {@link FaviconProvider.Builder}'s configuration methods on the mixin's own surface and
	 * forwards each call to an underlying {@link FaviconProvider.Builder}, which builds the shared worker
	 * the mixin delegates to.
	 *
	 * <p>
	 * Extends {@link org.apache.juneau.rest.server.servlet.RestMixin.Builder} so the mixin's bespoke worker-config setters
	 * ({@link #bytes(byte[])}, {@link #classpath(String)}, {@link #cacheControl(String)}) chain with true
	 * covariant returns alongside the inherited {@link RestBuilder}surface (e.g. {@code path}, {@code roleGuard}).
	 * This is how a multi-flavor capability avoids triplicating its REST-level config: the worker config is
	 * forwarded once into {@link FaviconProvider.Builder}, and the REST config is inherited once from
	 * {@link AbstractRestBuilder}.
	 */
	public static class Builder extends RestMixin.Builder<FaviconMixin,Builder> {

		private final FaviconProvider.Builder worker = FaviconProvider.create();

		/** Constructor &mdash; package access for {@link FaviconMixin#create()}. */
		protected Builder() {
			super(FaviconMixin.class);
		}

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
			worker.bytes(value);
			return this;
		}

		/**
		 * Sets the classpath resource path from which to load the favicon bytes.
		 *
		 * <p>
		 * A resolved-to-{@code null} stream falls back to the framework's default favicon.
		 * Mutually exclusive with {@link #bytes(byte[])}.
		 *
		 * @param value The classpath resource path (e.g. {@code "/myapp/icon.ico"}).
		 * 	Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder classpath(String value) {
			worker.classpath(value);
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
			worker.cacheControl(value);
			return this;
		}

		/**
		 * Builds a {@link FaviconMixin} instance, delegating to the shared {@link FaviconProvider} worker and
		 * stashing this builder so its {@code @Rest}-level overrides take precedence over the annotation.
		 *
		 * @return A configured instance.
		 */
		@Override /* AbstractRestBuilder */
		public FaviconMixin build() {
			return new FaviconMixin(worker.build(), this);
		}
	}
}
