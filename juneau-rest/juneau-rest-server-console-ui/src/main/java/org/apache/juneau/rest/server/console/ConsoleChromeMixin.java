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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.io.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.util.*;

/**
 * Mixin that serves the admin-console chrome stylesheet at {@code /juneau-console/chrome.css}.
 *
 * <p>
 * Compose into a host resource via {@link Rest#mixins() @Rest(mixins=ConsoleChromeMixin.class)} (see
 * {@code DataTablesMixin} in the {@code juneau-rest-server-datatables} module for the identical asset-serving
 * pattern this mirrors &mdash; not linked here since that module is not a dependency of this one). The served
 * response is the static
 * structural {@code chrome.css} (shipped in this module's classpath) with the active theme's tokens appended as an
 * {@code html:root{}} block.
 *
 * <h5 class='section'>Mount styles:</h5>
 * <p>
 * Both of the following arrangements serve the assets at the {@link #CHROME_CSS_PATH} /
 * {@link #LOGO_ASSET_PATH} / {@link #PAGE_BG_ASSET_PATH} URLs with no path juggling by the host resource:
 * <ul>
 * 	<li><b>Composed</b> &mdash; the host resource is mounted wherever the application already mounts it (e.g.
 * 		{@code /rest/*}) and the assets hang off that mount, at
 * 		<code>&lt;host-mount&gt;/juneau-console/chrome.css</code>.
 * 	<li><b>Standalone</b> &mdash; the host resource is registered with the servlet container at url-pattern
 * 		{@code /juneau-console/*} so the assets sit at a fixed site-root URL, independent of which page or tab
 * 		rendered the referencing {@code <link>}.
 * </ul>
 * <p>
 * The two arrangements need different operation paths, because a container mount at {@code /juneau-console/*}
 * reports {@code servletPath="/juneau-console"} and Juneau resolves an operation's path against the request URI
 * with {@code contextPath + servletPath} already removed &mdash; leaving only {@code /chrome.css} to match.
 * Each operation below therefore declares <i>both</i> its prefixed path and the same path minus the
 * {@code /juneau-console} prefix, so whichever one the arrangement leaves to be matched resolves.
 * <p>
 * The same duality applies to the logo/page-background {@code url()}s written into the served stylesheet: they are
 * resolved per-request against the container's context path and the host's mount, picking the path form the active
 * arrangement leaves unconsumed, so the browser fetches them from the mount that served the stylesheet rather than
 * from the site root.
 *
 * <h5 class='section'>Theme precedence:</h5>
 * <p>
 * <code>{@link Builder#theme(Theme) mixin.theme(...)}</code> wins over a {@link ThemeSettings} {@code BeanStore}
 * bean, which wins over {@link Theme#OPEN}. {@link Theme#OPEN}'s token block is always appended first; if the
 * active theme differs from {@link Theme#OPEN}, a second appended block carries its override tokens. Every token
 * value is run through {@code CssValueEscaper} before being written into the response.
 *
 * <h5 class='section'>Stylesheet load-order band:</h5>
 * <p>
 * At equal cascade specificity, whichever of {@code juneau-views.css} and this class's {@code chrome.css} is
 * declared later in a page's {@code <head>} wins &mdash; the exact ambiguity the {@code rootBlock} theme block
 * below is raised to {@code html:root} to make irrelevant. Juneau's own in-tree page emitters (the examples
 * module) don't rely on that fix alone: they declare a five-position stylesheet/script load-order band,
 * enforced by a build-time test, so their own asset order is never left to chance in the first place:
 * <ol>
 * 	<li>vendor stylesheets
 * 	<li>{@code juneau-views.css} &mdash; the views base layer
 * 	<li>first-party widget stylesheets that build on the views layer, e.g. {@code juneau-calendar.css}
 * 	<li>the consumer theme, e.g. this class's {@code chrome.css}
 * 	<li>page-local {@code <style>}
 * </ol>
 * <p>
 * That guard reaches only the pages Juneau itself emits. It says nothing about, and claims no control over, an
 * arbitrary host application's own document &mdash; a consumer is free to link {@code chrome.css} and
 * {@code juneau-views.css} in either order, which is precisely why the {@code html:root} fix exists: so the
 * active theme wins regardless of which order the consumer's links happen to appear in.
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

	/** The URL path at which the configured logo asset is served (relative to the host mount). */
	public static final String LOGO_ASSET_PATH = "/juneau-console/assets/logo";

	/** The URL path at which the configured page-background asset is served (relative to the host mount). */
	public static final String PAGE_BG_ASSET_PATH = "/juneau-console/assets/page-bg";

	/** {@link #CHROME_CSS_PATH} minus the {@code /juneau-console} prefix - see the class javadoc's mount-styles section. */
	static final String CHROME_CSS_PATH_UNPREFIXED = "/chrome.css";

	/** {@link #LOGO_ASSET_PATH} minus the {@code /juneau-console} prefix - see the class javadoc's mount-styles section. */
	static final String LOGO_ASSET_PATH_UNPREFIXED = "/assets/logo";

	/** {@link #PAGE_BG_ASSET_PATH} minus the {@code /juneau-console} prefix - see the class javadoc's mount-styles section. */
	static final String PAGE_BG_ASSET_PATH_UNPREFIXED = "/assets/page-bg";

	/** The prefix the {@code *_UNPREFIXED} constants drop - see the class javadoc's mount-styles section. */
	static final String MOUNT_PREFIX = "/juneau-console";

	/** Classpath location of the shipped structural stylesheet. */
	static final String CHROME_CSS_RESOURCE = "/org/apache/juneau/console/chrome.css";

	/** Content type emitted for the chrome stylesheet. */
	static final String CONTENT_TYPE = "text/css;charset=utf-8";

	/** {@code Cache-Control} header emitted for the chrome stylesheet (1 day). */
	static final String CACHE_CONTROL = "max-age=86400, public";

	/** The image file extensions a configured logo/page-background asset is allowed to use. */
	private static final Set<String> ALLOWED_ASSET_EXTS = Set.of("svg", "png", "jpg", "jpeg", "webp", "gif");

	/**
	 * The framework-authored role-token alias derivations.
	 *
	 * <p>
	 * {@code chrome.css} consumes <i>role</i>-named tokens ({@code --jc-header-bg}, {@code --jc-surface},
	 * {@code --jc-on-accent}, ...); {@link Theme#OPEN} (and every consumer theme) still defines the original
	 * <i>colour</i>-named leaf tokens ({@code --jc-white}, {@code --jc-border-2}, ...). This block bridges the two
	 * by deriving each role token <b>from</b> the legacy token it replaces, so a consumer's existing override of a
	 * legacy token still flows through to every derived role, and a consumer who sets a role token directly
	 * out-ranks the derived default by cascade order (this block is emitted inside the {@code Theme.OPEN} block,
	 * before any active-theme override block).
	 *
	 * <p>
	 * These aliases are <b>permanent</b> (no deprecation window) and are deliberately <i>not</i>
	 * {@link Theme#OPEN} tokens. A {@code var(--jc-*)}-valued token IS legal Theme-layer syntax &mdash;
	 * {@code Theme.Builder} recognizes it as a reference and resolves it to a concrete literal at {@code build()}
	 * time &mdash; but that resolution scope deliberately excludes these role aliases, which are appended here
	 * outside {@code Theme.OPEN}'s token map. Making them {@code Theme.OPEN} tokens instead would resolve each alias
	 * to a <i>fixed literal</i> at composition time, snapshotting the live CSS cascade (the dark-mode /
	 * user-agent overrides that reach these role tokens at use time) into a frozen value &mdash; so
	 * {@code Theme.OPEN} is kept all-literal and the aliases stay here as framework-authored literal text that is
	 * emitted verbatim (never routed through {@code CssValueGrammar}, which exists to validate <i>consumer</i>
	 * input, not the framework's own stylesheet). {@code Theme.OPEN} owns leaf values; this block owns derived
	 * values; no token name is declared by both.
	 */
	static final String OPEN_ROLE_ALIASES = String.join("",
		"--jc-surface:var(--jc-white);",
		"--jc-header-bg:var(--jc-surface);",
		"--jc-nav-bg:var(--jc-header-bg);",
		"--jc-control-bg:var(--jc-surface);",
		"--jc-table-bg:var(--jc-surface);",
		"--jc-table-row-bg:var(--jc-surface);",
		"--jc-on-accent:var(--jc-white);",
		"--jc-on-btn-primary:var(--jc-on-accent);",
		"--jc-table-stripe-bg:var(--jc-card-bg);",
		"--jc-hover-bg:var(--jc-chrome-bg);",
		"--jc-table-header-bg:var(--jc-chrome-bg);",
		"--jc-control-border:var(--jc-border-2);",
		"--jc-header-icon-text:var(--jc-header-icon-color);",
		"--jc-btn-primary-bg:var(--jc-btn-primary);",
		"--jc-btn-primary-bg-hover:var(--jc-btn-primary-hover);"
	);

	/** The shipped static chrome.css bytes, read once from the classpath (shared - the static file never varies by theme). */
	private static volatile String staticCss;

	/**
	 * Read+cache+hash+serve helper for the configured logo/page-background assets, anchored on this class so
	 * {@link ClasspathAssetCache#buildVersion()} resolves this module's own implementation version (see that
	 * class's javadoc's version-anchor section).
	 */
	private static final ClasspathAssetCache ASSET_CACHE = new ClasspathAssetCache(ConsoleChromeMixin.class);

	private final boolean cacheAssets;
	private final Theme theme;
	private final String logoResource;
	private final String pageBackgroundResource;

	/** Per-mixin-instance cache of the fully-assembled (static + theme blocks) response body, keyed by mount (see {@link #mountKey}). */
	private final Map<String,byte[]> cachedBodies = new ConcurrentHashMap<>();

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
		this.logoResource = builder.logoResource;
		this.pageBackgroundResource = builder.pageBackgroundResource;
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
		path={CHROME_CSS_PATH, CHROME_CSS_PATH_UNPREFIXED},
		summary="Admin-console chrome stylesheet",
		description="Structural CSS for the admin-console chrome, with the active theme's tokens appended.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getChromeCss(RestRequest req) throws IOException {
		var body = cacheAssets ? cachedBody(req) : buildBody(req);
		return ASSET_CACHE.wrap(body, CONTENT_TYPE, CACHE_CONTROL);
	}

	/**
	 * [GET /juneau-console/assets/logo] &mdash; serve the configured logo asset.
	 *
	 * @return The configured logo image as an {@link HttpResource}.
	 * @throws IOException If the configured resource could not be read.
	 */
	@RestGet(
		path={LOGO_ASSET_PATH, LOGO_ASSET_PATH_UNPREFIXED},
		summary="Configured logo image asset",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getLogoAsset() throws IOException {
		if (logoResource == null)
			throw new NotFound("No logo asset configured.");
		return ASSET_CACHE.serve(logoResource, MimeTypeDetector.DEFAULT.getContentType(logoResource), CACHE_CONTROL);
	}

	/**
	 * [GET /juneau-console/assets/page-bg] &mdash; serve the configured page-background asset.
	 *
	 * @return The configured page-background image as an {@link HttpResource}.
	 * @throws IOException If the configured resource could not be read.
	 */
	@RestGet(
		path={PAGE_BG_ASSET_PATH, PAGE_BG_ASSET_PATH_UNPREFIXED},
		summary="Configured page-background image asset",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getPageBackgroundAsset() throws IOException {
		if (pageBackgroundResource == null)
			throw new NotFound("No page-background asset configured.");
		return ASSET_CACHE.serve(pageBackgroundResource, MimeTypeDetector.DEFAULT.getContentType(pageBackgroundResource), CACHE_CONTROL);
	}

	/**
	 * Returns the fully-assembled response body for the mount the request arrived under, computing (and caching) it
	 * on first call for that mount.
	 *
	 * <p>
	 * The cache is keyed by mount rather than held in a single field because the emitted asset URLs are mount-derived
	 * (see {@link #assetUrl}), and one mixin instance can be reached under more than one container mapping.
	 */
	private byte[] cachedBody(RestRequest req) throws IOException {
		try {
			return cachedBodies.computeIfAbsent(mountKey(req), k -> {
				try {
					return buildBody(req);
				} catch (IOException e) {  // HTT: staticCss() is the only throwing call and reads a resource shipped in this jar.
					throw new UncheckedIOException(e);
				}
			});
		} catch (UncheckedIOException e) {  // HTT: see above - the wrapped read cannot fail in a well-formed jar.
			throw e.getCause();
		}
	}

	/** The request's mount identity: the two request properties every emitted asset URL is derived from. */
	private static String mountKey(RestRequest req) {
		return req.getContextPath() + '\n' + req.getServletPath();
	}

	/**
	 * Builds the response body: the static structural CSS, then Theme.OPEN's block, then (if different) the active
	 * theme's override block, then (if configured) the logo/page-background asset override rules. Each override
	 * rule's {@code ?v=<buildVersion>-<hash8>} cache-buster is content-sensitive (see
	 * {@link ClasspathAssetCache#cacheBuster}, mirroring {@code ViewsMixin}) so a {@code -SNAPSHOT} rebuild of the
	 * configured asset busts the browser cache without relying on {@code buildVersion} (stable across dev rebuilds)
	 * alone.
	 */
	private byte[] buildBody(RestRequest req) throws IOException {
		buildCount.incrementAndGet();
		var sb = new StringBuilder(staticCss());
		sb.append('\n').append(openRootBlock());
		var active = resolveActiveTheme(req);
		if (! active.getName().equals(Theme.OPEN.getName()))
			sb.append('\n').append(rootBlock(active));
		if (pageBackgroundResource != null)
			sb.append('\n').append("html, body{background-image:url(\"").append(assetUrl(req, PAGE_BG_ASSET_PATH, PAGE_BG_ASSET_PATH_UNPREFIXED))
				.append(ASSET_CACHE.cacheBuster(pageBackgroundResource))
				.append("\"), var(--jc-page-bg);}");
		if (logoResource != null)
			sb.append('\n').append(".jc-logo{background-image:url(\"").append(assetUrl(req, LOGO_ASSET_PATH, LOGO_ASSET_PATH_UNPREFIXED))
				.append(ASSET_CACHE.cacheBuster(logoResource))
				.append("\");}");
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Resolves one of the mixin's asset endpoints to a URL a browser can fetch from wherever the referencing
	 * {@code chrome.css} was served: the container's context path and the host's mount, plus whichever of the
	 * endpoint's two declared paths the active mount leaves to be matched.
	 *
	 * <p>
	 * Under the <b>standalone</b> mount style the container has already consumed the {@code /juneau-console} segment
	 * into {@code servletPath}, so resolving the prefixed path against it would emit that segment twice; under the
	 * <b>composed</b> style {@code servletPath} is the host's own mount and the prefixed segment is exactly what is
	 * missing. {@code servletPath} ending in {@code /juneau-console} therefore identifies the standalone case.
	 *
	 * <p>
	 * A composing host whose own mount happens to end in {@code /juneau-console} reads as standalone here, which
	 * emits its unprefixed URL rather than its prefixed one - still a live URL, since every mount serves both forms
	 * (see the class javadoc's mount-styles section).
	 */
	private static String assetUrl(RestRequest req, String prefixedPath, String unprefixedPath) {
		var standalone = req.getServletPath().endsWith(MOUNT_PREFIX);
		return req.getUriResolver().resolve("servlet:" + (standalone ? unprefixedPath : prefixedPath));
	}

	/**
	 * Test-only diagnostic: the number of times this instance has (re)assembled its response body.
	 *
	 * @return The build count.
	 */
	int debugBuildCount() { return buildCount.get(); }

	/** Resolves the active theme via the documented precedence: builder-supplied {@code theme(Theme)} &gt; {@link ThemeSettings} bean &gt; {@link Theme#OPEN}. */
	@SuppressWarnings({
		"resource" // False positive: req.getContext().getBeanStore() returns a borrowed, container-owned AutoCloseable, not a resource created/owned here.
	})
	private Theme resolveActiveTheme(RestRequest req) {
		if (theme != null)
			return theme;
		return req.getContext().getBeanStore().getBean(ThemeSettings.class).orElse(ThemeSettings.DEFAULT).getTheme();
	}

	/**
	 * Renders one theme's tokens as an appended {@code html:root{ ... }} block, escaping every value at the
	 * declaration boundary.
	 *
	 * <p>
	 * The selector is {@code html:root} rather than a bare {@code :root} so the block scores {@code (0,0,1,1)}
	 * instead of {@code (0,0,1,0)}. A separately-linked stylesheet that declares the same token names in its own
	 * {@code :root} would tie a bare {@code :root} on specificity, and the cascade would then fall through to
	 * source order &mdash; which across two stylesheets is the order the consumer's {@code <link>} elements
	 * happen to appear in, something this framework neither sets nor can observe. The extra type selector makes
	 * the theme's tokens out-rank such a declaration in either link order.
	 */
	private static String rootBlock(Theme theme) {
		var sb = new StringBuilder();
		sb.append("html:root{");
		for (var e : theme.getTokens().entrySet())
			sb.append(e.getKey()).append(':').append(CssValueEscaper.escape(e.getValue())).append(';');
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Renders {@link Theme#OPEN}'s {@code html:root{}} block with the framework-authored role-token alias
	 * derivations ({@link #OPEN_ROLE_ALIASES}) appended inside the same block, immediately after the leaf tokens.
	 * The aliases are appended as trusted framework literal text (not routed through {@code CssValueEscaper},
	 * which is for consumer-supplied values); folding them into {@code Theme.OPEN}'s block keeps the served
	 * response at exactly one {@code html:root{}} block for the default theme while still placing every derived
	 * default before any active-theme override block. See {@link #rootBlock(Theme)} for why the selector carries
	 * the {@code html} type prefix.
	 */
	private static String openRootBlock() {
		var sb = new StringBuilder();
		sb.append("html:root{");
		for (var e : Theme.OPEN.getTokens().entrySet())
			sb.append(e.getKey()).append(':').append(CssValueEscaper.escape(e.getValue())).append(';');
		sb.append(OPEN_ROLE_ALIASES);
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
		String logoResource;
		String pageBackgroundResource;

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
		 * Configures a themeable logo image, served at {@link #LOGO_ASSET_PATH} and overriding the default
		 * {@code .jc-logo} background image in the emitted {@code chrome.css}.
		 *
		 * @param value
	 * 	An app-owned, classpath-root-absolute resource path (e.g. {@code "/static/img/oakleaf.svg"}). Must exist on
	 * 	the classpath, contain no {@code ..} path segment or {@code %} character, and end in one of {@code .svg}/
	 * 	{@code .png}/{@code .jpg}/{@code .jpeg}/{@code .webp}/{@code .gif}.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>, empty, traversal-shaped, has an
		 * 	unrecognized extension, or does not resolve to an existing classpath resource.
		 */
		public Builder logo(String value) {
			this.logoResource = validateAssetResource(value, "logo");
			return this;
		}

		/**
		 * Configures a themeable page-background image, served at {@link #PAGE_BG_ASSET_PATH} and layered over the
		 * active theme's {@code --jc-page-bg} gradient in the emitted {@code chrome.css}.
		 *
		 * @param value
		 * 	An app-owned, classpath-root-absolute resource path (e.g. {@code "/static/img/topo-bg.png"}). Same
		 * 	validation as {@link #logo(String)}.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>, empty, traversal-shaped, has an
		 * 	unrecognized extension, or does not resolve to an existing classpath resource.
		 */
		public Builder pageBackgroundImage(String value) {
			this.pageBackgroundResource = validateAssetResource(value, "pageBackgroundImage");
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

		/**
		 * Fail-closed validation for a configured asset's classpath resource path: reject <jk>null</jk>/empty,
		 * reject any traversal-shaped path (containing {@code ..} or a {@code %} URI-encoding escape, matching
		 * {@code BasicFileFinder.isInvalidPath}), reject an extension outside the image allowlist, and reject a
		 * path that does not resolve to an existing classpath resource.
		 */
		private static String validateAssetResource(String value, String paramName) {
			if (value == null || value.isEmpty())
				throw iaex("'%s' must not be null or empty.", paramName);
			if (value.contains("..") || value.contains("%"))
				throw iaex("'%s' must not contain '..' or '%%' (path traversal): '%s'.", paramName, value);
			if (! ALLOWED_ASSET_EXTS.contains(FileUtils.getFileExtension(value).toLowerCase(Locale.ROOT)))
				throw iaex("'%s' must end in one of .svg/.png/.jpg/.jpeg/.webp/.gif: '%s'.", paramName, value);
			if (ConsoleChromeMixin.class.getResource(value) == null)
				throw iaex("'%s' classpath resource not found: '%s'.", paramName, value);
			return value;
		}
	}
}
