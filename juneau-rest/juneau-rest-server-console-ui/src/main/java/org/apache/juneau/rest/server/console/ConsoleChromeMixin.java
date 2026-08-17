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
package org.apache.juneau.rest.server.console;

import java.io.*;
import java.nio.charset.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin that serves the admin-console chrome stylesheet at {@code /juneau-console/chrome.css}.
 *
 * <p>
 * Compose into a host resource via {@link Rest#mixins() @Rest(mixins=ConsoleChromeMixin.class)} (see
 * {@code DataTablesMixin} in the {@code juneau-rest-server-datatables} module for the identical asset-serving
 * pattern this mirrors &mdash; not linked here since that module is not a dependency of this one). The served
 * response is the static
 * structural {@code chrome.css} (shipped in this module's classpath) with the active theme's tokens appended as a
 * {@code :root{}} block.
 *
 * <h5 class='section'>Theme precedence:</h5>
 * <p>
 * <code>{@link Builder#theme(Theme) mixin.theme(...)}</code> wins over a {@link ThemeSettings} {@code BeanStore}
 * bean, which wins over {@link Theme#OPEN}. {@link Theme#OPEN}'s token block is always appended first; if the
 * active theme differs from {@link Theme#OPEN}, a second appended block carries its override tokens. Every token
 * value is run through {@code CssValueEscaper} before being written into the response.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(mixins=ConsoleChromeMixin.<jk>class</jk>)
 * 	<jk>public class</jk> MyConsoleResource <jk>extends</jk> BasicRestServlet {...}
 * </p>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
public class ConsoleChromeMixin {

	/** The URL path at which the chrome stylesheet is served (relative to the host mount). */
	public static final String CHROME_CSS_PATH = "/juneau-console/chrome.css";

	/** Classpath location of the shipped structural stylesheet. */
	static final String CHROME_CSS_RESOURCE = "/org/apache/juneau/console/chrome.css";

	/** Content type emitted for the chrome stylesheet. */
	static final String CONTENT_TYPE = "text/css;charset=utf-8";

	/** {@code Cache-Control} header emitted for the chrome stylesheet (1 day). */
	static final String CACHE_CONTROL = "max-age=86400, public";

	/** The shipped static chrome.css bytes, read once from the classpath (shared - the static file never varies by theme). */
	private static volatile String staticCss;

	private final boolean cacheAssets;
	private final Theme theme;

	/** Per-mixin-instance cache of the fully-assembled (static + theme blocks) response body. Never shared across mounts. */
	private volatile byte[] cachedBody;

	/** Test-only diagnostic: counts every call to {@link #buildBody(RestRequest)} (i.e. every cache miss / every call when caching is disabled). */
	private final AtomicInteger buildCount = new AtomicInteger();

	/**
	 * No-arg constructor, mirroring {@code FreemarkerMixin()}'s equivalent so a bean-store lookup miss can fall back
	 * to a default-configured instance without relying on {@code BeanInstantiator}'s builder-pattern detection.
	 */
	public ConsoleChromeMixin() {
		this(create());
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	protected ConsoleChromeMixin(Builder builder) {
		this.cacheAssets = builder.cacheAssets;
		this.theme = builder.theme;
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * [GET /juneau-console/chrome.css] &mdash; serve the admin-console chrome stylesheet.
	 *
	 * @param req The current request (used to resolve the active theme via {@link ThemeSettings}).
	 * @return The chrome stylesheet as a CSS {@link HttpResource}.
	 * @throws IOException If the shipped {@code chrome.css} resource could not be read (effectively unreachable
	 * 	&mdash; the resource is shipped in the same jar as this class).
	 */
	@RestGet(
		path=CHROME_CSS_PATH,
		summary="Admin-console chrome stylesheet",
		description="Structural CSS for the admin-console chrome, with the active theme's tokens appended.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getChromeCss(RestRequest req) throws IOException {
		var body = cacheAssets ? cachedBody(req) : buildBody(req);
		return HttpResourceBean.of(
			ByteArrayBody.of(body, CONTENT_TYPE),
			CollectionUtils.list(ContentType.of(CONTENT_TYPE), CacheControl.of(CACHE_CONTROL))
		);
	}

	/** Returns the fully-assembled response body, computing (and instance-caching) it on first call. */
	private byte[] cachedBody(RestRequest req) throws IOException {
		var b = cachedBody;
		if (b == null) {
			synchronized (this) {
				b = cachedBody;
				if (b == null) {  // HTT: the "already set" branch is only reachable under a lock-acquisition race - unhittable single-threaded.
					b = buildBody(req);
					cachedBody = b;
				}
			}
		}
		return b;
	}

	/** Builds the response body: the static structural CSS, then Theme.OPEN's block, then (if different) the active theme's override block. */
	private byte[] buildBody(RestRequest req) throws IOException {
		buildCount.incrementAndGet();
		var sb = new StringBuilder(staticCss());
		sb.append('\n').append(rootBlock(Theme.OPEN));
		var active = resolveActiveTheme(req);
		if (! active.getName().equals(Theme.OPEN.getName()))
			sb.append('\n').append(rootBlock(active));
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Test-only diagnostic: the number of times this instance has (re)assembled its response body.
	 *
	 * @return The build count.
	 */
	int debugBuildCount() { return buildCount.get(); }

	/** Resolves the active theme via the documented precedence: builder-supplied {@code theme(Theme)} &gt; {@link ThemeSettings} bean &gt; {@link Theme#OPEN}. */
	private Theme resolveActiveTheme(RestRequest req) {
		if (theme != null)
			return theme;
		return req.getContext().getBeanStore().getBean(ThemeSettings.class).orElse(ThemeSettings.DEFAULT).getTheme();
	}

	/** Renders one theme's tokens as an appended {@code :root{ ... }} block, escaping every value at the declaration boundary. */
	private static String rootBlock(Theme theme) {
		var sb = new StringBuilder();
		sb.append(":root{");
		for (var e : theme.getTokens().entrySet())
			sb.append(e.getKey()).append(':').append(CssValueEscaper.escape(e.getValue())).append(';');
		sb.append('}');
		return sb.toString();
	}

	/** Returns the shipped static chrome.css text, reading (and caching, process-wide - the static file never varies) it from the classpath on first call. */
	private static String staticCss() throws IOException {
		var s = staticCss;
		if (s == null) {
			synchronized (ConsoleChromeMixin.class) {
				s = staticCss;
				if (s == null) {  // HTT: the "already set" branch is only reachable under a lock-acquisition race - unhittable single-threaded.
					try (var in = ConsoleChromeMixin.class.getResourceAsStream(CHROME_CSS_RESOURCE)) {
						if (in == null)
							throw new IOException("Classpath resource not found: " + CHROME_CSS_RESOURCE);
						s = IoUtils.read(in);
					}
					staticCss = s;
				}
			}
		}
		return s;
	}

	/**
	 * Builder for {@link ConsoleChromeMixin}.
	 */
	public static class Builder {
		boolean cacheAssets = true;
		Theme theme;

		/**
		 * Whether to cache the assembled response body after the first request (default <jk>true</jk>).
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder cacheAssets(boolean value) {
			this.cacheAssets = value;
			return this;
		}

		/**
		 * Explicitly sets the active theme, overriding any {@link ThemeSettings} {@code BeanStore} bean.
		 *
		 * @param value The theme to use.
		 * @return This object.
		 */
		public Builder theme(Theme value) {
			this.theme = value;
			return this;
		}

		/**
		 * Builds the mixin.
		 *
		 * @return A new {@link ConsoleChromeMixin}.
		 */
		public ConsoleChromeMixin build() {
			return new ConsoleChromeMixin(this);
		}
	}
}
