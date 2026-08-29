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
 * structural {@code chrome.css} (shipped in this module's classpath) with the active {@link Theme}'s tokens &mdash;
 * or the active {@link ThemePack}'s tokens and alias references &mdash; appended as an {@code html:root{}} block.
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
 * Five steps, in order, first match wins:
 * <ol>
 * 	<li><code>{@link Builder#pack(ThemePack) mixin.pack(...)}</code>
 * 	<li><code>{@link Builder#theme(Theme) mixin.theme(...)}</code>
 * 	<li>a {@link ThemePackSettings} {@code BeanStore} bean
 * 	<li>a {@link ThemeSettings} {@code BeanStore} bean
 * 	<li>{@link Theme#OPEN}
 * </ol>
 * <p>
 * The ordering is <b>axis-major</b>: the explicit-builder-call tier sits entirely above the bean tier, and within
 * each tier a pack out-ranks a theme. So a mixin built with {@code theme(...)} beats a {@link ThemePackSettings}
 * bean &mdash; "an explicit builder call beats a bean" is the rule that decides the mixed case, because a consumer
 * who hand-configured the mixin made the more local, more deliberate statement.
 *
 * <p>
 * Selection is <b>winner-takes-all, never a merge</b>: if a pack wins, the standalone {@link Theme} is ignored
 * <i>entirely</i> rather than layered underneath it, and vice versa. A loser contributes nothing to the response.
 *
 * <p>
 * {@link Theme#OPEN}'s token block is always appended first. Exactly one further block may follow it &mdash; the
 * winning pack's, or the winning theme's if it differs from {@link Theme#OPEN} &mdash; so a themed host and a
 * pack-configured host both serve exactly two token blocks. Every leaf token value, from either channel, is run
 * through {@code CssValueEscaper} before being written into the response; a pack's alias references are emitted
 * verbatim, for the reason {@code packRootBlock}'s javadoc gives.
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

	/** The reserved token-name prefix guarded by {@link #rejectReservedChromeDeclaration(String, String)}. */
	private static final String CHROME_TOKEN_PREFIX = "--jc-chrome-";

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
	private final ThemePack pack;
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
		this.pack = builder.pack;
		// An explicitly configured asset wins; a builder-supplied pack's asset is the fallback.  Resolved here
		// because these fields, and the content-hash cache-busters derived from them, are fixed at construction -
		// which is also why a bean-supplied pack's assets cannot reach them.  See Builder.pack(ThemePack).
		this.logoResource = builder.logoResource != null ? builder.logoResource
			: (builder.pack != null ? builder.pack.getLogoResource() : null);
		this.pageBackgroundResource = builder.pageBackgroundResource != null ? builder.pageBackgroundResource
			: (builder.pack != null ? builder.pack.getPageBackgroundResource() : null);
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
	 * @param req
	 * 	The current request, used to resolve the active pack or theme &mdash; the two bean-tier steps of the
	 * 	precedence chain read {@link ThemePackSettings} / {@link ThemeSettings} from its bean store.
	 * @return The chrome stylesheet as a CSS {@link HttpResource}.
	 * @throws IOException If the shipped {@code chrome.css} resource could not be read (effectively unreachable
	 * 	&mdash; the resource is shipped in the same jar as this class).
	 * @throws IllegalArgumentException
	 * 	If the active theme arrived through a {@link ThemeSettings} bean and declares a reserved chrome-scale token
	 * 	name. A settings bean is resolved per request and so has no construction-time boundary of its own, which is
	 * 	why that rejection lands here, at emission, rather than at configuration time (see
	 * 	{@link #rejectReservedChromeDeclaration(String, String)}).
	 */
	@RestGet(
		path={CHROME_CSS_PATH, CHROME_CSS_PATH_UNPREFIXED},
		summary="Admin-console chrome stylesheet",
		description="Structural CSS for the admin-console chrome, with the active theme pack's or theme's tokens appended.",
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
	 * Builds the response body: the static structural CSS, then Theme.OPEN's block, then the active pack's block (or,
	 * if no pack is active, the active theme's override block if it differs from Theme.OPEN), then (if configured)
	 * the logo/page-background asset override rules. Each override
	 * rule's {@code ?v=<buildVersion>-<hash8>} cache-buster is content-sensitive (see
	 * {@link ClasspathAssetCache#cacheBuster}, mirroring {@code ViewsMixin}) so a {@code -SNAPSHOT} rebuild of the
	 * configured asset busts the browser cache without relying on {@code buildVersion} (stable across dev rebuilds)
	 * alone.
	 */
	private byte[] buildBody(RestRequest req) throws IOException {
		buildCount.incrementAndGet();
		var sb = new StringBuilder(staticCss());
		sb.append('\n').append(openRootBlock());
		var activePack = resolveActivePack(req);
		if (activePack.isPresent()) {
			// A pack REPLACES the standalone-theme block rather than adding a third one, so a pack-configured host
			// serves the same two token blocks a themed host does.
			var p = activePack.get();
			// Suppressed on the honest condition - a pack with no leaves AND no aliases has nothing to declare, so
			// emitting would leave a stray empty block.  Deliberately NOT the name.equals("open") test the theme
			// branch below uses: that test silently drops every override of a theme that happens to be named
			// "open", and a pack must not inherit that trap.
			if (! (p.getTheme().getTokens().isEmpty() && p.getAliases().isEmpty()))
				sb.append('\n').append(packRootBlock(p));
		} else {
			var active = resolveActiveTheme(req);
			if (! active.getName().equals(Theme.OPEN.getName()))
				sb.append('\n').append(rootBlock(active));
		}
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

	/**
	 * Resolves the active pack: the top two steps of the documented precedence chain, plus the pack half of the bean
	 * tier.
	 *
	 * <p>
	 * Returns empty whenever the winner is a theme rather than a pack, at which point {@link #resolveActiveTheme}
	 * decides between the remaining two steps. The middle branch is what makes the chain <b>axis-major</b>: an
	 * explicit {@code theme(...)} short-circuits the bean tier entirely, so it beats a {@link ThemePackSettings}
	 * bean. There is no default pack to fall back to &mdash; {@link ThemePackSettings} ships no {@code DEFAULT}
	 * constant, because a zero-config application renders {@link Theme#OPEN}.
	 */
	@SuppressWarnings({
		"resource" // False positive: req.getContext().getBeanStore() returns a borrowed, container-owned AutoCloseable, not a resource created/owned here.
	})
	private Optional<ThemePack> resolveActivePack(RestRequest req) {
		if (pack != null)
			return Optional.of(pack);
		if (theme != null)
			return Optional.empty();
		return req.getContext().getBeanStore().getBean(ThemePackSettings.class).map(ThemePackSettings::getPack);
	}

	/**
	 * Resolves the active theme via the documented precedence: builder-supplied {@code theme(Theme)} &gt;
	 * {@link ThemeSettings} bean &gt; {@link Theme#OPEN}.
	 *
	 * <p>
	 * Only consulted when {@link #resolveActivePack} came back empty, so its behaviour is unchanged from before
	 * packs existed.
	 */
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
		for (var e : theme.getTokens().entrySet()) {
			rejectReservedChromeDeclaration(e.getKey(), "theme '" + theme.getName() + "'");
			sb.append(e.getKey()).append(':').append(CssValueEscaper.escape(e.getValue())).append(';');
		}
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Renders one pack's {@code html:root{}} block: its {@link Theme} leaves first, escaped, then its alias
	 * references, verbatim.
	 *
	 * <p>
	 * Both channels share <b>one</b> block, mirroring {@link #openRootBlock()}'s folding of
	 * {@link #OPEN_ROLE_ALIASES} inside {@link Theme#OPEN}'s block, and for the same reason: it keeps the served
	 * response at a predictable block count. Within-block order does not affect correctness &mdash; CSS custom
	 * properties resolve lazily at use time, so an alias may textually precede its target &mdash; but leaves-then-
	 * aliases is pinned anyway so the body stays byte-stable for a given pack. See {@link #rootBlock(Theme)} for
	 * why the selector carries the {@code html} type prefix.
	 *
	 * <h5 class='section'>The two halves are escaped differently, deliberately:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>Leaves</b> are consumer-supplied <i>literals</i>, so every value goes through
	 * 		{@code CssValueEscaper} exactly as {@link #rootBlock(Theme)} does. Dropping that call here would reopen
	 * 		the declaration-boundary breakout hole <i>for pack-configured hosts only</i>, leaving the theme-channel
	 * 		test that guards it green while the pack channel is wide open &mdash; which is why the pack channel has
	 * 		an escaper-wiring test of its own.
	 * 	<li><b>Aliases</b> are emitted verbatim, with no escaper, and that is not an omission: every alias value has
	 * 		already been forced to match the anchored {@code var(--jc-name)} shape, whose alphabet cannot represent
	 * 		{@code ;}, <code>}</code>, a quote, a backslash, a comment or a {@code url(} production. There is
	 * 		nothing left for an escaper to neutralize, and escaping would corrupt the reference's parens. See
	 * 		{@link ThemePack}'s <i>Injection safety</i> section.
	 * </ul>
	 *
	 * <h5 class='section'>The var() asymmetry:</h5>
	 * <p>
	 * A {@code var()} written on a <b>{@link Theme} token</b> is resolved to a literal before it reaches here, and
	 * a test pins that the served declaration is never a reference. An <b>alias</b> is the exact opposite: its
	 * reference must <b>survive</b> to the wire, or the pack's derived tokens freeze to a snapshot of the cascade
	 * and the channel loses its purpose. Do not "unify" the two {@code var()} paths &mdash; they are deliberately
	 * opposite. The same warning sits on {@link ThemePack} itself.
	 */
	private static String packRootBlock(ThemePack pack) {
		var sb = new StringBuilder();
		sb.append("html:root{");
		for (var e : pack.getTheme().getTokens().entrySet()) {
			rejectReservedChromeDeclaration(e.getKey(), "theme pack '" + pack.getId() + "' leaf token");
			sb.append(e.getKey()).append(':').append(CssValueEscaper.escape(e.getValue())).append(';');
		}
		for (var e : pack.getAliases().entrySet()) {
			rejectReservedChromeDeclaration(e.getKey(), "theme pack '" + pack.getId() + "' alias");
			sb.append(e.getKey()).append(':').append(e.getValue()).append(';');
		}
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

	/**
	 * The single shared, fail-closed guard rejecting a declaration of a reserved chrome-scale token name, invoked on
	 * <b>every</b> path by which a token declaration can reach the served stylesheet.
	 *
	 * <p>
	 * <b>What it protects.</b> {@code juneau-views.css} declares one shared control-scale ladder
	 * ({@code --jc-chrome-control-height}, {@code --jc-chrome-font-size-1}, {@code --jc-chrome-glyph-size}, ...) at
	 * {@code :root}, and a contract test in that module pins it as declared exactly once so that every surface
	 * shares one ladder. A token block emitted here sits at {@code html:root}, which out-specifies that
	 * {@code :root} &mdash; so a consumer redeclaring a ladder step would win, render perfectly, and turn that
	 * contract into decoration with no test failing anywhere. Documentation is not a mitigation for that, so the
	 * rejection lives here.
	 *
	 * <p>
	 * <b>Where it fires.</b> The harm is done at emission, not at construction, so this guard covers every channel
	 * rather than just one: {@link ThemePack.Builder#alias(String, String)} and
	 * {@link ThemePack.Builder#theme(Theme)} at pack-construction time, {@link Builder#theme(Theme)} at
	 * mixin-construction time, and {@code rootBlock} / {@code packRootBlock} at emission.
	 *
	 * <p>
	 * The four channels are <b>not</b> all closed at the same layer, and it is worth being precise about which
	 * closes where, because only three of the call sites can be caught by a failing test:
	 * <ul class='spaced-list'>
	 * 	<li>A {@link ThemeSettings} bean is closed <b>only</b> by the {@code rootBlock} call. {@link Theme} itself
	 * 		permits a reserved name and the bean is resolved per request, so nothing earlier ever sees it. Deleting
	 * 		that call would serve the declaration, and a test pins that it does not.
	 * 	<li>A {@link ThemePackSettings} bean is closed one layer <b>earlier</b>: a {@link ThemePack} carrying a
	 * 		reserved declaration cannot be constructed at all, so no such pack can be placed in a bean. The
	 * 		{@code packRootBlock} calls are therefore unreachable defence-in-depth today &mdash; deliberately kept,
	 * 		because they are what makes the emission boundary total rather than contingent on {@link ThemePack}
	 * 		remaining the only way to build a pack, but with no test that can go red on their removal. A test pins
	 * 		the constructional closure instead, so the reasoning is checked even though the call site cannot be.
	 * </ul>
	 *
	 * <p>
	 * <b>It fires on the declared name only, never on a target.</b> Aliasing a pack token <i>to</i> a ladder step
	 * is legal and useful; <i>declaring</i> a ladder step is not.
	 *
	 * <p>
	 * <b>The {@link Theme#OPEN} exemption.</b> The reserved namespace is {@code --jc-chrome-*} <i>except</i> the
	 * names {@link Theme#OPEN} itself already declares. Exactly one name is exempt today:
	 * {@code --jc-chrome-bg}, a shipped colour token that predates the ladder, is consumed by {@code chrome.css},
	 * and is legitimately overridable. Without the exemption this guard would reject {@link Theme#OPEN}'s own token
	 * set &mdash; and therefore every theme built with {@link Theme#deriveFrom(String, Theme)} seeded from
	 * {@link Theme#OPEN}, which is the normal way to author a palette. The exemption is expressed against
	 * {@link Theme#OPEN} rather than as a hardcoded name list so it cannot drift: it needs no copy of the ladder's
	 * names (which live in a module this one deliberately cannot see), it stays correct if a chrome-named leaf is
	 * ever added to or removed from {@link Theme#OPEN}, and it fails <i>closed</i> for every new name &mdash; no
	 * ladder step is a {@link Theme#OPEN} token, and adding one could only ever happen through a reviewed edit to
	 * {@link Theme#OPEN} that its own pinned token count would surface.
	 *
	 * <p>
	 * A prefix test rather than an anchored {@code matches(...)}: by the time a name reaches here it has already
	 * passed a full-string {@code ^--jc-[a-z0-9-]+$} shape guard, so its alphabet is closed and
	 * {@link String#startsWith(String)} is total &mdash; there is no string that passes the shape guard, begins with
	 * the reserved prefix, and escapes this check.
	 *
	 * @param name The declared token name. Already shape-validated by the caller.
	 * @param source A short description of the channel the declaration arrived through, for the failure message.
	 * @throws IllegalArgumentException If {@code name} is a reserved chrome-scale token name.
	 */
	static void rejectReservedChromeDeclaration(String name, String source) {
		if (name.startsWith(CHROME_TOKEN_PREFIX) && ! Theme.OPEN.getTokens().containsKey(name))
			throw iaex(
				"Cannot declare reserved token '%s' (%s).  The '%s' namespace is the shared control-scale ladder declared by juneau-views.css; a token block served here out-specifies it.  Referencing one of these tokens is legal - declaring one is not.",
				name, source, CHROME_TOKEN_PREFIX);
	}

	/**
	 * Fail-closed validation for a configured asset's classpath resource path: reject <jk>null</jk>/empty,
	 * reject any traversal-shaped path (containing {@code ..} or a {@code %} URI-encoding escape, matching
	 * {@code BasicFileFinder.isInvalidPath}), reject an extension outside the image allowlist, and reject a
	 * path that does not resolve to an existing classpath resource.
	 *
	 * <p>
	 * Shared with {@link ThemePack.Builder#logo(String)} so a pack's assets go through this exact belt rather than
	 * a second copy of it.
	 *
	 * @param value The candidate classpath resource path.
	 * @param paramName The configuring method's name, for the failure message.
	 * @return {@code value}, unchanged, if it is accepted.
	 * @throws IllegalArgumentException If the path is rejected.
	 */
	static String validateAssetResource(String value, String paramName) {
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
		ThemePack pack;
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
		 * Explicitly sets the active theme, out-ranking every step below it in the precedence chain (see the class
		 * javadoc's <i>Theme precedence</i> section) &mdash; including a {@link ThemePackSettings} bean.
		 *
		 * <p>
		 * A theme set here is ignored outright if {@link #pack(ThemePack)} is also set; selection is
		 * winner-takes-all, so the two are never merged.
		 *
		 * @param value The theme to use. Can be <jk>null</jk> to leave the theme unset.
		 * @return This object.
		 * @throws IllegalArgumentException
		 * 	If {@code value} declares a reserved chrome-scale token name (see
		 * 	{@link ConsoleChromeMixin#rejectReservedChromeDeclaration(String, String)}). A {@link Theme} may
		 * 	legitimately carry such a name; what is rejected is <i>serving</i> it from this mixin, so the check sits
		 * 	here rather than in {@link Theme}.
		 */
		public Builder theme(Theme value) {
			if (value != null)
				for (var name : value.getTokens().keySet())
					rejectReservedChromeDeclaration(name, "theme '" + value.getName() + "'");
			this.theme = value;
			return this;
		}

		/**
		 * Explicitly sets the active theme pack, out-ranking every other step in the precedence chain (see the class
		 * javadoc's <i>Theme precedence</i> section).
		 *
		 * <p>
		 * The pack's leaf tokens and alias references are served as a single appended {@code html:root{}} block,
		 * <i>replacing</i> the block a standalone theme would have occupied &mdash; so a pack-configured host still
		 * serves exactly two token blocks.
		 *
		 * <h5 class='section'>Asset limitation:</h5>
		 * <p>
		 * A pack's {@link ThemePack#getLogoResource() logo} and
		 * {@link ThemePack#getPageBackgroundResource() page background} are resolved <b>here, at mixin construction
		 * time</b>, and only from a pack supplied through this method. A pack that arrives instead through a
		 * {@link ThemePackSettings} bean is resolved per request, after the asset fields and their content-hash
		 * cache-busters are already fixed &mdash; so <b>its assets are silently ignored</b> while its tokens and
		 * aliases still apply. This is a deliberate, test-pinned limitation rather than an oversight: assets are
		 * already a construction-time-only concern in this class (there is no settings bean for the logo either),
		 * and an application that bundles its own logo is configuring this builder anyway. An explicit
		 * {@link #logo(String)} / {@link #pageBackgroundImage(String)} always wins over the pack's.
		 *
		 * @param value The pack to use. Can be <jk>null</jk> to leave the pack unset.
		 * @return This object.
		 */
		public Builder pack(ThemePack value) {
			this.pack = value;
			return this;
		}

		/**
		 * Configures a themeable logo image, served at {@link #LOGO_ASSET_PATH} and overriding the default
		 * {@code .jc-logo} background image in the emitted {@code chrome.css}.
		 *
		 * <p>
		 * Wins over a {@link #pack(ThemePack) pack}'s own logo.
		 *
		 * @param value
		 * 	An app-owned, classpath-root-absolute resource path (e.g. {@code "/static/img/oakleaf.svg"}). Must exist
		 * 	on the classpath, contain no {@code ..} path segment or {@code %} character, and end in one of
		 * 	{@code .svg}/{@code .png}/{@code .jpg}/{@code .jpeg}/{@code .webp}/{@code .gif}.
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
		 * <p>
		 * Wins over a {@link #pack(ThemePack) pack}'s own page background.
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
	}
}
