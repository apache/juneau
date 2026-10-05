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
import java.util.stream.*;

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
 * structural {@code chrome.css} (shipped in this module's classpath) with {@link Theme#OPEN}'s block, then the
 * selected stock theme's override block if one other than {@code open} was selected, appended as
 * {@code html:root{}} blocks.
 *
 * <h5 class='section'>Mount styles:</h5>
 * <p>
 * Both of the following arrangements serve the stylesheets at the {@link #CHROME_CSS_PATH} / {@link #THEME_CSS_DIR}
 * URLs with no path juggling by the host resource:
 * <ul>
 * 	<li><b>Composed</b> &mdash; the host resource is mounted wherever the application already mounts it (e.g.
 * 		{@code /rest/*}) and the stylesheets hang off that mount, at
 * 		<code>&lt;host-mount&gt;/juneau-console/chrome.css</code>.
 * 	<li><b>Standalone</b> &mdash; the host resource is registered with the servlet container at url-pattern
 * 		{@code /juneau-console/*} so the stylesheets sit at a fixed site-root URL, independent of which page or tab
 * 		rendered the referencing {@code <link>}.
 * </ul>
 * <p>
 * The two arrangements need different operation paths, because a container mount at {@code /juneau-console/*}
 * reports {@code servletPath="/juneau-console"} and Juneau resolves an operation's path against the request URI
 * with {@code contextPath + servletPath} already removed &mdash; leaving only {@code /chrome.css} to match.
 * Each operation below therefore declares <i>both</i> its prefixed path and the same path minus the
 * {@code /juneau-console} prefix, so whichever one the arrangement leaves to be matched resolves.
 *
 * <h5 class='section'>Theme precedence</h5>
 * <p>{@link Builder#theme(String)} if set, else {@link Theme#OPEN}. {@link Theme#OPEN}'s block is always appended
 * first; a selected non-{@code open} stock theme adds exactly one more block.
 *
 * <h5 class='section'>Stylesheet load-order band:</h5>
 * <p>
 * At equal cascade specificity, whichever of {@code juneau-views.css} and this class's {@code chrome.css} is
 * declared later in a page's {@code <head>} wins &mdash; the exact ambiguity the {@code overrideBlock} theme block
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
 * 	<jc>// Stock chrome.css (Theme.OPEN tokens), mounted standalone at /juneau-console/*.</jc>
 * 	<ja>@Rest</ja>(mixins=ConsoleChromeMixin.<jk>class</jk>)
 * 	<jk>public class</jk> ConsoleAssetsRest <jk>extends</jk> BasicRestServlet {
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> ConsoleChromeMixin consoleChrome() {
 * 			<jk>return</jk> ConsoleChromeMixin.<jsm>create</jsm>().build();
 * 		}
 * 	}
 *
 * 	<jc>// Bake a different stock palette into the served chrome.css.</jc>
 * 	ConsoleChromeMixin.<jsm>create</jsm>().theme(<js>"gray"</js>).build();
 * </p>
 *
 * <p>
 * Custom palettes aren't configured here. Author them in the FreeMarker chrome with {@code <@theme>}/{@code <@token>}
 * (see the {@link Theme} example), which emits the stock-theme {@code <link>} plus an inline override block.
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
@SuppressWarnings({
	"java:S125", // Comments are explanatory; they are not commented-out code.
	"java:S1192" // Duplicated "html:root{" selector text is a CSS protocol fragment; a constant would obscure the emitter.
})
public class ConsoleChromeMixin {

	/** The URL path at which the chrome stylesheet is served (relative to the host mount). */
	public static final String CHROME_CSS_PATH = "/juneau-console/chrome.css";

	/** {@link #CHROME_CSS_PATH} minus the {@code /juneau-console} prefix - see the class javadoc's mount-styles section. */
	static final String CHROME_CSS_PATH_UNPREFIXED = "/chrome.css";

	/** Classpath location of the shipped structural stylesheet. */
	static final String CHROME_CSS_RESOURCE = "/org/apache/juneau/console/chrome.css";

	/**
	 * The URL directory the shipped stock-theme stylesheets are served under (relative to the host mount), e.g.
	 * {@code /juneau-console/themes/juneau-theme-light-red.css}. A subdirectory of the chrome mount so a
	 * templated {@code {file}} path cannot collide with the fixed {@link #CHROME_CSS_PATH} endpoint. The FTL
	 * {@code <@theme name="…"/>} directive links these; see {@link #themeAssetUrl(RestRequest, String)}.
	 */
	public static final String THEME_CSS_DIR = "/juneau-console/themes/";

	/** {@link #THEME_CSS_DIR} minus the {@code /juneau-console} prefix - see the class javadoc's mount-styles section. */
	static final String THEME_CSS_DIR_UNPREFIXED = "/themes/";

	/** Classpath directory the shipped theme stylesheets live in (same package as {@link #CHROME_CSS_RESOURCE}). */
	static final String THEME_CSS_RESOURCE_DIR = "/org/apache/juneau/console/";

	/** The stock palettes by FTL/builder name, in documented order; the single source for the name list and file allowlist. */
	private static final Map<String,Theme> STOCK_THEMES = stockThemes();

	/**
	 * The built-in stock-theme names - the kebab-case of the {@link Theme} constants ({@link Theme#OPEN},
	 * {@link Theme#LIGHT_RED}, {@link Theme#LIGHT_BROWN}, {@link Theme#RED}, {@link Theme#GRAY}). Each maps to a
	 * shipped {@code juneau-theme-<name>.css} pack. Consumed by the FTL {@code <@theme>} directive to fail an
	 * unknown name closed, and here to bound the served {@code {file}} to a known pack.
	 */
	public static final List<String> BUILTIN_THEME_NAMES = List.copyOf(STOCK_THEMES.keySet());

	/** The served stock stylesheet file names, derived from {@link #BUILTIN_THEME_NAMES}. */
	private static final Set<String> ALLOWED_THEME_FILES = BUILTIN_THEME_NAMES.stream()
		.map(n -> "juneau-theme-" + n + ".css")
		.collect(Collectors.toUnmodifiableSet());

	private static Map<String,Theme> stockThemes() {
		var m = new LinkedHashMap<String,Theme>();
		m.put("open", Theme.OPEN);
		m.put("light-red", Theme.LIGHT_RED);
		m.put("light-brown", Theme.LIGHT_BROWN);
		m.put("red", Theme.RED);
		m.put("gray", Theme.GRAY);
		return u(m);
	}

	/** Content type emitted for the chrome stylesheet. */
	static final String CONTENT_TYPE = "text/css;charset=utf-8";

	/** {@code Cache-Control} header emitted for the chrome stylesheet (1 day). */
	static final String CACHE_CONTROL = "max-age=86400, public";

	/** Context-root-absolute path of the console shell script. */
	public static final String CONSOLE_JS_PATH = "/juneau-console/juneau-console.js";

	/** {@link #CONSOLE_JS_PATH} minus the {@code /juneau-console} prefix - see the class javadoc's mount-styles section. */
	static final String CONSOLE_JS_PATH_UNPREFIXED = "/juneau-console.js";

	/** Classpath location of the console shell script. */
	public static final String CONSOLE_JS_RESOURCE = "/org/apache/juneau/console/juneau-console.js";

	/** Context-root-absolute path of the page-contract schema. */
	public static final String SCHEMA_PATH = "/juneau-console/juneau-page.schema.json";

	/** {@link #SCHEMA_PATH} minus the {@code /juneau-console} prefix. */
	static final String SCHEMA_PATH_UNPREFIXED = "/juneau-page.schema.json";

	/** Classpath location of the page-contract schema (same file as {@link PageContractSchema#RESOURCE}). */
	static final String SCHEMA_RESOURCE = "/" + PageContractSchema.RESOURCE;

	static final String JS_CONTENT_TYPE = "text/javascript;charset=utf-8";
	static final String JSON_CONTENT_TYPE = "application/json;charset=utf-8";

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
	 *
	 * <p>
	 * {@code --jc-header-bg} (and, transitively, {@code --jc-nav-bg}) keys off {@code --jc-white}, not
	 * {@code --jc-chrome-bg}: the header and both page-nav rows (Page Tabs and Page Subtabs) stay white on
	 * every stock theme, matching {@link Theme#OPEN}'s blue look. Theme accent still paints the selected-tab
	 * indicator, page-nav floor, and the rest of the page; {@code --jc-chrome-bg} remains the page-chrome
	 * fallback under {@code --jc-page-bg}, the hover fill, and dialog chrome. Pinning header/nav to
	 * {@code --jc-chrome-bg} washed those bars in light-red / light-brown / red / gray. {@code --jc-page-nav-accent}
	 * defaults to {@code --jc-accent} so a consumer can retint the page-nav floor / selected-section bar without
	 * recoloring buttons. {@code --jc-table-header-bg} also keys off {@code --jc-white} so DataTable / views-table
	 * column headers stay white (IRS {@code table.dataTable thead th} has no background), not the page-chrome grey.
	 */
	static final String OPEN_ROLE_ALIASES = String.join("",
		"--jc-surface:var(--jc-white);",
		"--jc-header-bg:var(--jc-white);",
		"--jc-nav-bg:var(--jc-header-bg);",
		"--jc-page-nav-accent:var(--jc-accent);",
		"--jc-control-bg:var(--jc-surface);",
		"--jc-table-bg:var(--jc-surface);",
		"--jc-table-row-bg:var(--jc-surface);",
		"--jc-on-accent:var(--jc-white);",
		"--jc-on-btn-primary:var(--jc-on-accent);",
		"--jc-hover-bg:var(--jc-chrome-bg);",
		// DataTable / views-table headers are white (IRS `table.dataTable thead th` carries no background;
		// the white card shows through). Do not key this off --jc-chrome-bg — that page-chrome grey tints
		// every column header and is the wrong IRS surface (detail-table thead #f6f4f4 is a different widget).
		"--jc-table-header-bg:var(--jc-white);",
		"--jc-control-border:var(--jc-border-2);",
		"--jc-header-icon-text:var(--jc-header-icon-color);",
		"--jc-btn-primary-bg:var(--jc-btn-primary);",
		"--jc-btn-primary-bg-hover:var(--jc-btn-primary-hover);"
	);

	/** The shipped static chrome.css bytes, read once from the classpath (shared - the static file never varies by theme). */
	private static volatile String staticCss;

	/**
	 * Read+cache+hash+serve helper for the configured theme/chrome assets, anchored on this class so
	 * {@link ClasspathAssetCache#buildVersion()} resolves this module's own implementation version (see that
	 * class's javadoc's version-anchor section).
	 */
	private static final ClasspathAssetCache ASSET_CACHE = new ClasspathAssetCache(ConsoleChromeMixin.class);

	private final boolean cacheAssets;
	private final Theme theme;

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
	 * @param req The current request, used to key the per-mount body cache.
	 * @return The chrome stylesheet as a CSS {@link HttpResource}.
	 * @throws IOException If the shipped {@code chrome.css} resource could not be read (effectively unreachable
	 * 	&mdash; the resource is shipped in the same jar as this class).
	 */
	@RestGet(
		path={CHROME_CSS_PATH, CHROME_CSS_PATH_UNPREFIXED},
		summary="Admin-console chrome stylesheet",
		description="Structural CSS for the admin-console chrome, with the selected stock theme's tokens appended.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getChromeCss(RestRequest req) throws IOException {
		var body = cacheAssets ? cachedBody(req) : buildBody();
		return ASSET_CACHE.wrap(body, CONTENT_TYPE, CACHE_CONTROL);
	}

	/**
	 * [GET /juneau-console/themes/{file}] &mdash; serve one of the shipped stock-theme stylesheets.
	 *
	 * <p>
	 * The {@code {file}} segment is fail-closed to the known {@link #BUILTIN_THEME_NAMES} stylesheet filenames, so
	 * this endpoint can never read an arbitrary classpath resource. Each stylesheet is a structure-free
	 * {@code html:root{}} token block (one per {@link Theme} constant); the FTL {@code <@theme name="…"/>} directive
	 * links the winning one after {@code chrome.css} so its tokens override {@code chrome.css}'s baked-in
	 * {@link Theme#OPEN} block.
	 *
	 * @param file The requested stylesheet filename (e.g. {@code juneau-theme-light-red.css}).
	 * @return The theme stylesheet as a CSS {@link HttpResource}.
	 * @throws NotFound If {@code file} is not one of the shipped stylesheet filenames.
	 */
	@RestGet(
		path={THEME_CSS_DIR + "{file}", THEME_CSS_DIR_UNPREFIXED + "{file}"},
		summary="Admin-console stock theme stylesheet",
		description="One of the shipped juneau-theme-<name>.css stock theme stylesheets (html:root token block), linked by the FTL <@theme> directive.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getThemeCss(@Path("file") String file) {
		if (! ALLOWED_THEME_FILES.contains(file))
			throw new NotFound("Unknown theme stylesheet: " + file);
		return ASSET_CACHE.serve(THEME_CSS_RESOURCE_DIR + file, CONTENT_TYPE, CACHE_CONTROL);
	}

	/**
	 * [GET /juneau-console/juneau-console.js] &mdash; serve the console shell.
	 *
	 * @return The script.
	 */
	@RestGet(
		path={CONSOLE_JS_PATH, CONSOLE_JS_PATH_UNPREFIXED},
		summary="Console shell script",
		description="Renders the #juneau-page contract into header, nav, cards and footer.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getConsoleJs() {
		return ASSET_CACHE.serve(CONSOLE_JS_RESOURCE, JS_CONTENT_TYPE, CACHE_CONTROL);
	}

	/**
	 * [GET /juneau-console/juneau-page.schema.json] &mdash; serve the page-contract schema.
	 *
	 * @return The schema.
	 */
	@RestGet(
		path={SCHEMA_PATH, SCHEMA_PATH_UNPREFIXED},
		summary="Console page-contract schema",
		description="JSON Schema 2020-12 for the #juneau-page contract.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getPageSchema() {
		return ASSET_CACHE.serve(SCHEMA_RESOURCE, JSON_CONTENT_TYPE, CACHE_CONTROL);
	}

	/**
	 * Returns a real, browser-fetchable URL for a shipped theme stylesheet, resolved against the request's
	 * <b>context root</b> (not the page resource) and carrying the same {@code ?v=<buildVersion>-<hash8>}
	 * content-sensitive cache-buster {@link #chromeCssUrl(RestRequest)} uses.
	 *
	 * <p>
	 * The console document shell ({@code <@console>} / {@code <@theme>}) emits this as a {@code <link href>} to the
	 * standalone {@code /juneau-console/*} mount. Resolving with {@code context:} rather than {@code servlet:} is
	 * load-bearing: a page resource at {@code /rest/setup} reports that path as its servlet path, so
	 * {@code servlet:/juneau-console/themes/…} would become {@code /rest/setup/juneau-console/themes/…} and 404.
	 * A template-rendering consumer sits downstream of Juneau's {@code servlet:}-rewriting serializer, so it needs
	 * the URL already resolved here.
	 *
	 * @param req The current request, supplying the context path to resolve against.
	 * @param name One of the {@link #BUILTIN_THEME_NAMES}.
	 * @return The context-root-absolute theme-stylesheet URL with the version+content-hash cache-buster appended.
	 */
	public static String themeAssetUrl(RestRequest req, String name) {
		var file = "juneau-theme-" + name + ".css";
		return consoleMountUrl(req, THEME_CSS_DIR + file, THEME_CSS_RESOURCE_DIR + file);
	}

	/**
	 * Returns a real, browser-fetchable URL for the shipped {@code chrome.css}, resolved against the request's
	 * <b>context root</b> (not the page resource) and carrying the same content-sensitive {@code ?v=…} cache-buster
	 * the theme stylesheets use.
	 *
	 * <p>
	 * The console document shell ({@code <@console>}) emits this as the first stylesheet in its head cascade. See
	 * {@link #themeAssetUrl(RestRequest, String)} for why the resolution is {@code context:} rather than
	 * {@code servlet:}.
	 *
	 * @param req The current request, supplying the context path to resolve against.
	 * @return The context-root-absolute {@code chrome.css} URL with the version+content-hash cache-buster appended.
	 */
	public static String chromeCssUrl(RestRequest req) {
		return consoleMountUrl(req, CHROME_CSS_PATH, CHROME_CSS_RESOURCE);
	}

	/**
	 * Context-root-absolute, cache-busted URL of the console shell script.
	 *
	 * @param req The current request.
	 * @return The URL.
	 */
	public static String consoleJsUrl(RestRequest req) {
		return consoleMountUrl(req, CONSOLE_JS_PATH, CONSOLE_JS_RESOURCE);
	}

	/**
	 * Context-root-absolute URL for an asset on the standalone {@code /juneau-console/*} mount.
	 *
	 * <p>
	 * Always the prefixed path ({@link #CHROME_CSS_PATH} / {@link #THEME_CSS_DIR}), resolved with {@code context:}
	 * so the href is {@code {contextPath}/juneau-console/…} regardless of which page resource rendered the shell.
	 */
	private static String consoleMountUrl(RestRequest req, String prefixedPath, String resource) {
		return req.getUriResolver().resolve("context:" + prefixedPath) + ASSET_CACHE.cacheBuster(resource);
	}

	/**
	 * Maps one of the {@link #BUILTIN_THEME_NAMES} stock names to its {@link Theme} constant.
	 *
	 * <p>
	 * The single place the kebab-case stock name (as written on FTL {@code <@theme name="…">} / the
	 * {@code theme(String)} builder) is resolved to its shipped {@link Theme}, without a second copy of the
	 * name&rarr;theme table.
	 *
	 * @param name One of the {@link #BUILTIN_THEME_NAMES}.
	 * @return The stock {@link Theme} for {@code name}.
	 * @throws IllegalArgumentException If {@code name} is not a built-in stock-theme name.
	 */
	public static Theme stockTheme(String name) {
		var t = STOCK_THEMES.get(name);
		if (t == null)
			throw iaex("Unknown stock theme name: '%s'.  Built-in themes: %s.", name, String.join(", ", BUILTIN_THEME_NAMES));
		return t;
	}

	/**
	 * Returns the fully-assembled response body for the mount the request arrived under, computing (and caching) it
	 * on first call for that mount.
	 *
	 * <p>
	 * The cache is keyed by mount rather than held in a single field so one mixin instance registered under more
	 * than one container mapping (see the class javadoc's mount-styles section) builds and caches its body
	 * independently for each mount it is reached under.
	 */
	private byte[] cachedBody(RestRequest req) throws IOException {
		try {
			return cachedBodies.computeIfAbsent(mountKey(req), k -> {
				try {
					return buildBody();
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
	 * Builds the response body: the static structural CSS, then {@link Theme#OPEN}'s block, then the selected stock
	 * theme's override block if a non-{@code open} stock theme was selected.
	 */
	private byte[] buildBody() throws IOException {
		buildCount.incrementAndGet();
		var sb = new StringBuilder(staticCss());
		sb.append('\n').append(openRootBlock());
		var active = theme == null ? Theme.OPEN : theme;
		if (! active.getName().equals(Theme.OPEN.getName()))
			sb.append('\n').append(overrideBlock(active));
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Test-only diagnostic: the number of times this instance has (re)assembled its response body.
	 *
	 * @return The build count.
	 */
	int debugBuildCount() { return buildCount.get(); }

	/**
	 * Renders a theme's override block: one {@code html:root&#123;...&#125;} carrying the theme's leaf tokens, then its
	 * alias tokens, each in declaration order.
	 *
	 * <p>
	 * The two halves are escaped differently. A leaf is a validated literal and goes through
	 * {@link CssValueEscaper}: a grammar-accepted value can still carry a character such as {@code ;} that would end
	 * the declaration early. An alias is a single anchored {@code var(--jc-name)} reference
	 * ({@link Theme.Builder#alias(String, String)}), in which nothing dangerous is representable, so it is emitted
	 * verbatim. That asymmetry is the reason {@link Theme#getAliases()} stays a map.
	 *
	 * <p>
	 * The block uses the {@code html} type prefix. Two {@code :root} blocks in separately linked stylesheets tie at
	 * specificity {@code (0,0,1,0)} and are resolved by {@code <link>} order, which this framework neither sets
	 * nor observes. {@code html:root} scores {@code (0,0,1,1)}, so a theme override wins in either order.
	 *
	 * <p>
	 * A theme with no leaves and no aliases renders as the empty string. Each declared name is re-checked against
	 * the reserved {@code --jc-chrome-*} namespace (defense in depth; a {@link Theme} built by its builder can't
	 * carry one). This method is public so the FreeMarker {@code <@theme>} directive renders its inline block with
	 * the same code that serves {@code chrome.css}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Theme <jv>theme</jv> = Theme.<jsm>create</jsm>(<js>"corporate"</js>)
	 * 		.token(<js>"--jc-accent"</js>, <js>"#b45309"</js>)
	 * 		.alias(<js>"--jc-tab-bar-bg"</js>, <js>"var(--jc-card-bg)"</js>)
	 * 		.build();
	 * 	String <jv>css</jv> = ConsoleChromeMixin.<jsm>overrideBlock</jsm>(<jv>theme</jv>);
	 * 	<jc>// html:root{--jc-accent:#b45309;--jc-tab-bar-bg:var(--jc-card-bg);}</jc>
	 * </p>
	 *
	 * @param theme The theme.  Must not be <jk>null</jk>.
	 * @return The override block, or an empty string if the theme declares nothing.
	 */
	public static String overrideBlock(Theme theme) {
		if (theme.getTokens().isEmpty() && theme.getAliases().isEmpty())
			return "";
		var sb = new StringBuilder("html:root{");
		for (var e : theme.getTokens().entrySet()) {
			Theme.rejectReservedChromeDeclaration(e.getKey(), "theme '" + theme.getName() + "' leaf token");
			sb.append(e.getKey()).append(':').append(CssValueEscaper.escape(e.getValue())).append(';');
		}
		for (var e : theme.getAliases().entrySet()) {
			Theme.rejectReservedChromeDeclaration(e.getKey(), "theme '" + theme.getName() + "' alias");
			sb.append(e.getKey()).append(':').append(e.getValue()).append(';');
		}
		return sb.append('}').toString();
	}

	/**
	 * Renders {@link Theme#OPEN}'s {@code html:root{}} block with the framework-authored role-token alias
	 * derivations ({@link #OPEN_ROLE_ALIASES}) appended inside the same block, immediately after the leaf tokens.
	 * The aliases are appended as trusted framework literal text (not routed through {@code CssValueEscaper},
	 * which is for consumer-supplied values); folding them into {@code Theme.OPEN}'s block keeps the served
	 * response at exactly one {@code html:root{}} block for the default theme while still placing every derived
	 * default before any active-theme override block. See {@link #overrideBlock(Theme)} for why the selector carries
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
		 * Selects one of the shipped stock palettes by name (the same names FTL {@code <@theme name="…">} accepts).
		 *
		 * <p>
		 * A custom palette is authored in FTL with {@code <@theme>}/{@code <@token>}.
		 *
		 * @param stockName One of the {@link ConsoleChromeMixin#BUILTIN_THEME_NAMES}
		 * 	({@code open}, {@code light-red}, {@code light-brown}, {@code red}, {@code gray}).
		 * @return This object.
		 * @throws IllegalArgumentException If {@code stockName} is not a built-in stock-theme name.
		 */
		public Builder theme(String stockName) {
			this.theme = stockTheme(stockName);
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
