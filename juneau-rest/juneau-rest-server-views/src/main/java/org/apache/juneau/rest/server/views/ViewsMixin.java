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
package org.apache.juneau.rest.server.views;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.util.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Mixin that serves the first-party rich-view runtime assets &mdash; {@code juneau-views.js},
 * {@code juneau-ribbon.js}, {@code juneau-renders.js}, {@code juneau-views.css}, the opt-in
 * {@code juneau-pages.js} tabs/sub-tabs page runtime, the opt-in {@code juneau-config.js}/
 * {@code juneau-config.css} column-chooser runtime, and the opt-in {@code juneau-cards.js} card-layout runtime
 * &mdash; each at its stable path (design doc §6.1).
 *
 * <p>
 * Compose into a host resource via {@link Rest#mixins() @Rest(mixins=ViewsMixin.class)}; the asset URLs then become
 * available alongside the host's own endpoints, so browser pages can load the runtime without the application hosting
 * it itself.  This mirrors the sibling {@code DataTablesMixin}/{@code ConsoleChromeMixin} serving pattern (those
 * modules are not dependencies of this one, so they are not linked here).
 *
 * <h5 class='section'>What this ships (and what it deliberately does not):</h5>
 * <p>
 * This is an Apache project; the <a class="doclink" href="https://datatables.net">DataTables</a> library and jQuery are
 * <b>not</b> ASF category-A licensed, so <b>they are not bundled here.</b>  Only the thin, first-party, clean-room
 * runtime is served; the DataTables/jQuery/Buttons libraries stay <b>caller-provided</b> (CDN or self-hosted).  The
 * base {@code juneau-views.css} {@code .tag} chip is dependency-free (neutral, no colors); {@code console-ui}'s
 * {@code chrome.css} themes the same {@code .tag.<domain>.<value>} classes when present, but this module takes
 * <b>no</b> dependency on it.
 *
 * <h5 class='section'>Cache-busting + versioned URLs:</h5>
 * <p>
 * Each asset is served with a one-day {@code Cache-Control} and referenced from a page's {@code head=} block with a
 * {@code ?v=<buildVersion>-<hash8>} cache-buster (see {@link #viewAssetUrl(String)}, or
 * {@link #viewAssetUrl(RestRequest, String)} for a template-rendered consumer that needs the URL already resolved
 * to an absolute, browser-fetchable form), where {@code hash8} is an
 * 8-hex-char CRC32 of that asset's own served bytes, computed once and cached (classpath resources never change
 * within a running JVM). Keying the buster off content rather than {@code buildVersion} alone matters for
 * {@code -SNAPSHOT} builds: that version string is stable across dev rebuilds, so a version-only buster would keep
 * serving a browser's stale cached copy after every rebuild - the content hash changes the instant the bytes do,
 * with no SNAPSHOT special-casing needed. {@code buildVersion} is the framework's
 * {@link Package#getImplementationVersion() implementation version}, falling back to {@code "dev"} when running from an
 * unpackaged (IDE/test) classpath.  The served endpoint ignores the query string, so any (or no) {@code ?v=} still
 * resolves to the same asset.
 *
 * <h5 class='section'>Contract-version handshake:</h5>
 * <p>
 * {@link #CONTRACT_VERSION} is the single wire-contract discriminator, kept in one source of truth with the value the
 * model emits ({@link ViewDef#CONTRACT_VERSION}).  {@code juneau-views.js} bakes in the same value so the client can
 * fail loud when a served {@code VIEW_META} sidecar's {@code contractVersion} differs from the runtime's.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 * <p>
 * The mount paths are pinned at the op level by {@code @RestGet(path=...)} on the serving methods; a class-level
 * {@code @Rest(paths=...)} would be silently ignored under the mixin pattern.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewDef}
 * 	<li class='jc'>{@link ViewTable}
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
public class ViewsMixin {

	/** The URL path at which the client initializer is served (relative to the host mount). */
	public static final String VIEWS_JS_PATH = "/juneau-views.js";

	/** The URL path at which the ribbon runtime is served (relative to the host mount). */
	public static final String RIBBON_JS_PATH = "/juneau-ribbon.js";

	/** The URL path at which the renderer registry is served (relative to the host mount). */
	public static final String RENDERS_JS_PATH = "/juneau-renders.js";

	/** The URL path at which the base view stylesheet is served (relative to the host mount). */
	public static final String VIEWS_CSS_PATH = "/juneau-views.css";

	/** The URL path at which the icon registry is served (relative to the host mount). */
	public static final String ICONS_JS_PATH = "/juneau-icons.js";

	/**
	 * The URL path at which the shared SVG symbol sprite is served (relative to the host mount).
	 * {@code juneau-icons.js} fetches this next to itself; the key/legend file is not served to browsers.
	 */
	public static final String SYMBOLS_SVG_PATH = "/juneau-symbols.svg";

	/**
	 * The URL path at which the opt-in tabs/sub-tabs page runtime is served (relative to the host mount).  A
	 * separate, opt-in asset (Decision 2(A)): single-view pages never load it.
	 */
	public static final String PAGES_JS_PATH = "/juneau-pages.js";

	/**
	 * The URL path at which the opt-in column-chooser runtime is served (relative to the host mount).  A
	 * consumer adds this {@code <script>} after {@code juneau-views.js}; a non-configurable table never loads it.
	 */
	public static final String CONFIG_JS_PATH = "/juneau-config.js";

	/**
	 * The URL path at which the opt-in column-chooser stylesheet is served (relative to the host mount).
	 */
	public static final String CONFIG_CSS_PATH = "/juneau-config.css";

	/**
	 * The URL path at which the opt-in card-layout runtime is served (relative to the host mount).  A consumer adds
	 * this {@code <script>} after {@code juneau-icons.js} (the refresh button's glyph is resolved from the icon
	 * registry); a page with no {@code data-juneau-card-grid} never loads it.
	 */
	public static final String CARDS_JS_PATH = "/juneau-cards.js";

	/**
	 * The URL path at which the opt-in reusable-calendar runtime is served (relative to the host mount).  A
	 * consumer adds this {@code <script>} after {@code juneau-views.js}; a page with no calendar never loads it.
	 */
	public static final String CALENDAR_JS_PATH = "/juneau-calendar.js";

	/**
	 * The URL path at which the opt-in reusable-calendar stylesheet is served (relative to the host mount).
	 */
	public static final String CALENDAR_CSS_PATH = "/juneau-calendar.css";

	/**
	 * The URL path at which the opt-in page-chrome runtime is served (relative to the host mount).  A consumer adds
	 * this {@code <script>} after {@code juneau-icons.js} (header action glyphs resolve from the icon registry); a
	 * page with no {@code data-juneau-app-header}/{@code data-juneau-bar-slot} region never loads it.
	 */
	public static final String CHROME_JS_PATH = "/juneau-chrome.js";

	/**
	 * The frozen {@code VIEW_META} contract-version handshake constant, kept in one source of truth with the value the
	 * model emits ({@link ViewDef#CONTRACT_VERSION}).
	 */
	public static final String CONTRACT_VERSION = ViewDef.CONTRACT_VERSION;

	/**
	 * The card refresh-envelope contract-version handshake constant that {@code juneau-cards.js} bakes in, kept in one
	 * source of truth with the value the card model emits ({@link CardFieldList#CONTRACT_VERSION}).  Deliberately a
	 * distinct constant from {@link #CONTRACT_VERSION} (the {@code VIEW_META} sidecar contract): a card-envelope
	 * revision must never force a view-sidecar bump, or vice-versa.
	 */
	public static final String CARDS_CONTRACT_VERSION = CardFieldList.CONTRACT_VERSION;

	/**
	 * The app-header refresh-envelope contract-version handshake constant that {@code juneau-chrome.js} bakes in, kept
	 * in one source of truth with the value the header model emits ({@link AppHeaderDef#CONTRACT_VERSION}).
	 * Deliberately distinct from {@link #CONTRACT_VERSION} and {@link #BAR_CONTRACT_VERSION}: a header-envelope
	 * revision must never force a view-sidecar or bar-sidecar bump, or vice-versa.
	 */
	public static final String HEADER_CONTRACT_VERSION = AppHeaderDef.CONTRACT_VERSION;

	/**
	 * The bar-slot refresh-envelope contract-version handshake constant that {@code juneau-chrome.js} bakes in, kept in
	 * one source of truth with the value the bar model emits ({@link BarSlot#CONTRACT_VERSION}).  Deliberately a
	 * distinct constant from {@link #HEADER_CONTRACT_VERSION} (see that constant).
	 */
	public static final String BAR_CONTRACT_VERSION = BarSlot.CONTRACT_VERSION;

	/** Classpath location of the shipped initializer. */
	static final String VIEWS_JS_RESOURCE = "/org/apache/juneau/views/juneau-views.js";

	/** Classpath location of the shipped ribbon runtime. */
	static final String RIBBON_JS_RESOURCE = "/org/apache/juneau/views/juneau-ribbon.js";

	/** Classpath location of the shipped renderer registry. */
	static final String RENDERS_JS_RESOURCE = "/org/apache/juneau/views/juneau-renders.js";

	/** Classpath location of the shipped base stylesheet. */
	static final String VIEWS_CSS_RESOURCE = "/org/apache/juneau/views/juneau-views.css";

	/** Classpath location of the shipped icon registry. */
	static final String ICONS_JS_RESOURCE = "/org/apache/juneau/views/juneau-icons.js";

	/** Classpath location of the shipped SVG symbol sprite. */
	static final String SYMBOLS_SVG_RESOURCE = "/org/apache/juneau/views/juneau-symbols.svg";

	/** Classpath location of the shipped page runtime. */
	static final String PAGES_JS_RESOURCE = "/org/apache/juneau/views/juneau-pages.js";

	/** Classpath location of the shipped column-chooser runtime. */
	static final String CONFIG_JS_RESOURCE = "/org/apache/juneau/views/juneau-config.js";

	/** Classpath location of the shipped column-chooser stylesheet. */
	static final String CONFIG_CSS_RESOURCE = "/org/apache/juneau/views/juneau-config.css";

	/** Classpath location of the shipped card-layout runtime. */
	static final String CARDS_JS_RESOURCE = "/org/apache/juneau/views/juneau-cards.js";

	/** Classpath location of the shipped reusable-calendar runtime. */
	static final String CALENDAR_JS_RESOURCE = "/org/apache/juneau/views/juneau-calendar.js";

	/** Classpath location of the shipped reusable-calendar stylesheet. */
	static final String CALENDAR_CSS_RESOURCE = "/org/apache/juneau/views/juneau-calendar.css";

	/** Classpath location of the shipped page-chrome runtime. */
	static final String CHROME_JS_RESOURCE = "/org/apache/juneau/views/juneau-chrome.js";

	/** Content type emitted for the JavaScript assets. */
	static final String JS_CONTENT_TYPE = "text/javascript;charset=utf-8";

	/** Content type emitted for the stylesheet asset. */
	static final String CSS_CONTENT_TYPE = "text/css;charset=utf-8";

	/** Content type emitted for the SVG symbol sprite. */
	static final String SVG_CONTENT_TYPE = "image/svg+xml";

	/** {@code Cache-Control} header emitted for every asset (1 day). */
	static final String CACHE_CONTROL = "max-age=86400, public";

	/**
	 * Read+cache+hash+serve helper for this mixin's shipped assets, anchored on this class so
	 * {@link ClasspathAssetCache#buildVersion()} resolves this module's own implementation version (see that
	 * class's javadoc's version-anchor section).
	 */
	private static final ClasspathAssetCache ASSET_CACHE = new ClasspathAssetCache(ViewsMixin.class);

	/**
	 * [GET /juneau-views.js] &mdash; serve the client initializer.
	 *
	 * @return The initializer as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=VIEWS_JS_PATH,
		summary="Juneau rich-view client initializer",
		description="First-party JavaScript that auto-initializes <table data-juneau-view> elements from their VIEW_META sidecar.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getViewsScript() {
		return serve(VIEWS_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-ribbon.js] &mdash; serve the ribbon/toolbar runtime.
	 *
	 * @return The ribbon runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=RIBBON_JS_PATH,
		summary="Juneau rich-view ribbon runtime",
		description="First-party JavaScript that builds the ribbon/toolbar from a view's VIEW_META ribbon actions.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getRibbonScript() {
		return serve(RIBBON_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-renders.js] &mdash; serve the renderer registry.
	 *
	 * @return The renderer registry as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=RENDERS_JS_PATH,
		summary="Juneau rich-view renderer registry",
		description="First-party, dependency-free JavaScript cell-renderer registry consumed by the view initializer.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getRendersScript() {
		return serve(RENDERS_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-views.css] &mdash; serve the base view stylesheet.
	 *
	 * @return The base stylesheet as a CSS {@link HttpResource}.
	 */
	@RestGet(
		path=VIEWS_CSS_PATH,
		summary="Juneau rich-view base stylesheet",
		description="First-party base '.tag' chip stylesheet (neutral, no colors); console-ui themes the same classes when present.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getViewsStylesheet() {
		return serve(VIEWS_CSS_RESOURCE, CSS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-icons.js] &mdash; serve the icon registry.
	 *
	 * @return The icon registry as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=ICONS_JS_PATH,
		summary="Juneau rich-view icon registry",
		description="First-party, dependency-free JavaScript icon registry (name -> inline-SVG markup) consumed by the ribbon/paging-pill runtime.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getIconsScript() {
		return serve(ICONS_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-symbols.svg] &mdash; serve the shared SVG symbol sprite.
	 *
	 * @return The sprite as an SVG {@link HttpResource}.
	 */
	@RestGet(
		path=SYMBOLS_SVG_PATH,
		summary="Juneau rich-view SVG symbol sprite",
		description="Shared SVG <symbol> sprite referenced by juneau-icons.js <use href> hosts. The key/legend file is not served.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getSymbolsSvg() {
		return serve(SYMBOLS_SVG_RESOURCE, SVG_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-pages.js] &mdash; serve the opt-in tabs/sub-tabs page runtime.
	 *
	 * @return The page runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=PAGES_JS_PATH,
		summary="Juneau rich-view tabs/sub-tabs page runtime",
		description="First-party, opt-in JavaScript that hash-routes a PageDef's tabs/sub-tabs and lazy-inits each panel's view table on first activation.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getPagesScript() {
		return serve(PAGES_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-config.js] &mdash; serve the opt-in column-chooser / saved-views runtime.
	 *
	 * @return The column-chooser runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=CONFIG_JS_PATH,
		summary="Juneau rich-view column-chooser runtime",
		description="First-party, opt-in JavaScript that renders the View-tab column chooser and saved-views persistence for a columnConfig view.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getConfigScript() {
		return serve(CONFIG_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-config.css] &mdash; serve the opt-in column-chooser stylesheet.
	 *
	 * @return The column-chooser stylesheet as a CSS {@link HttpResource}.
	 */
	@RestGet(
		path=CONFIG_CSS_PATH,
		summary="Juneau rich-view column-chooser stylesheet",
		description="First-party, opt-in CSS for the View-tab column chooser dialog.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getConfigStylesheet() {
		return serve(CONFIG_CSS_RESOURCE, CSS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-cards.js] &mdash; serve the opt-in card-layout runtime.
	 *
	 * @return The card-layout runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=CARDS_JS_PATH,
		summary="Juneau rich-view card-layout runtime",
		description="First-party, opt-in JavaScript that enhances a CardGridTable's refreshable cards: contract handshake, built-in refresh button, and an optional per-card poll loop.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getCardsScript() {
		return serve(CARDS_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * Returns the servlet-relative URL for a served asset, carrying a {@code ?v=<buildVersion>-<hash8>} content-
	 * sensitive cache-buster suitable for a page's {@code head=} block (see the class Javadoc's cache-busting
	 * section for why the buster is content- rather than purely version-keyed).
	 *
	 * @param path One of the asset path constants ({@link #VIEWS_JS_PATH}, {@link #RIBBON_JS_PATH},
	 * 	{@link #RENDERS_JS_PATH}, {@link #VIEWS_CSS_PATH}, {@link #ICONS_JS_PATH}, {@link #SYMBOLS_SVG_PATH},
	 * 	{@link #PAGES_JS_PATH}, {@link #CONFIG_JS_PATH}, {@link #CONFIG_CSS_PATH}, {@link #CARDS_JS_PATH},
	 * 	{@link #CALENDAR_JS_PATH}, {@link #CALENDAR_CSS_PATH}).
	 * @return The servlet-relative asset URL with the version+content-hash cache-buster appended.
	 */
	public static String viewAssetUrl(String path) {
		return "servlet:" + path + ASSET_CACHE.cacheBuster(resourceFor(path));
	}

	/**
	 * Returns a real, browser-fetchable <b>absolute</b> URL for a served asset, resolved against the given
	 * request's context path and mount &mdash; carrying the same {@code ?v=<buildVersion>-<hash8>} content-
	 * sensitive cache-buster as {@link #viewAssetUrl(String)} (see the class Javadoc's cache-busting section).
	 *
	 * <p>
	 * {@link #viewAssetUrl(String)} returns a {@code servlet:}-prefixed URL that only Juneau's own HTML serializer
	 * resolves (it rewrites {@code servlet:} against the request at render time). A template-rendering consumer
	 * (e.g. {@code juneau-rest-server-view-freemarker}) sits downstream of that serializer, so it never sees the
	 * rewrite and would otherwise receive the literal, unfetchable string. This overload resolves the URL itself,
	 * per-request, the same way {@code ConsoleChromeMixin.assetUrl(RestRequest, ...)} does &mdash; via
	 * {@link RestRequest#getUriResolver()} &mdash; so it is usable from any consumer, template-rendered or not.
	 *
	 * @param req The current request, supplying the context path/mount to resolve against.
	 * @param path One of the asset path constants ({@link #VIEWS_JS_PATH}, {@link #RIBBON_JS_PATH},
	 * 	{@link #RENDERS_JS_PATH}, {@link #VIEWS_CSS_PATH}, {@link #ICONS_JS_PATH}, {@link #SYMBOLS_SVG_PATH},
	 * 	{@link #PAGES_JS_PATH}, {@link #CONFIG_JS_PATH}, {@link #CONFIG_CSS_PATH}, {@link #CARDS_JS_PATH},
	 * 	{@link #CALENDAR_JS_PATH}, {@link #CALENDAR_CSS_PATH}).
	 * @return The absolute asset URL with the version+content-hash cache-buster appended.
	 */
	public static String viewAssetUrl(RestRequest req, String path) {
		return req.getUriResolver().resolve("servlet:" + path) + ASSET_CACHE.cacheBuster(resourceFor(path));
	}

	/**
	 * [GET /juneau-calendar.js] &mdash; serve the opt-in reusable-calendar runtime.
	 *
	 * @return The calendar runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=CALENDAR_JS_PATH,
		summary="Juneau reusable-calendar runtime",
		description="First-party, opt-in JavaScript that hydrates a data-juneau-calendar month grid from its seed sidecar or a same-origin per-month GET.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getCalendarScript() {
		return serve(CALENDAR_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-calendar.css] &mdash; serve the opt-in reusable-calendar stylesheet.
	 *
	 * @return The calendar stylesheet as a CSS {@link HttpResource}.
	 */
	@RestGet(
		path=CALENDAR_CSS_PATH,
		summary="Juneau reusable-calendar stylesheet",
		description="First-party, opt-in CSS for the reusable-calendar month grid, chips, legend and day popover; declares --jc-* token fallbacks.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getCalendarStylesheet() {
		return serve(CALENDAR_CSS_RESOURCE, CSS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-chrome.js] &mdash; serve the opt-in page-chrome runtime.
	 *
	 * @return The page-chrome runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=CHROME_JS_PATH,
		summary="Juneau page-chrome runtime",
		description="First-party, opt-in JavaScript that enhances a PageDef's app-header / avatar / bar-slot chrome: contract handshake, icon hydration, avatar fallback, SAFE host-events, and demand-only same-origin count refresh.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getChromeScript() {
		return serve(CHROME_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/** Reads (and caches) the classpath asset and wraps it as a cacheable {@link HttpResource}. */
	private static HttpResource serve(String resource, String contentType) {
		return ASSET_CACHE.serve(resource, contentType, CACHE_CONTROL);
	}

	/** Maps a public asset path constant to its classpath resource constant (content-hashing only; routing itself is by {@code @RestGet(path=...)}). */
	private static String resourceFor(String path) {
		if (VIEWS_JS_PATH.equals(path)) return VIEWS_JS_RESOURCE;
		if (RIBBON_JS_PATH.equals(path)) return RIBBON_JS_RESOURCE;
		if (RENDERS_JS_PATH.equals(path)) return RENDERS_JS_RESOURCE;
		if (VIEWS_CSS_PATH.equals(path)) return VIEWS_CSS_RESOURCE;
		if (ICONS_JS_PATH.equals(path)) return ICONS_JS_RESOURCE;
		if (SYMBOLS_SVG_PATH.equals(path)) return SYMBOLS_SVG_RESOURCE;
		if (PAGES_JS_PATH.equals(path)) return PAGES_JS_RESOURCE;
		if (CONFIG_JS_PATH.equals(path)) return CONFIG_JS_RESOURCE;
		if (CONFIG_CSS_PATH.equals(path)) return CONFIG_CSS_RESOURCE;
		if (CARDS_JS_PATH.equals(path)) return CARDS_JS_RESOURCE;
		if (CALENDAR_JS_PATH.equals(path)) return CALENDAR_JS_RESOURCE;
		if (CALENDAR_CSS_PATH.equals(path)) return CALENDAR_CSS_RESOURCE;
		if (CHROME_JS_PATH.equals(path)) return CHROME_JS_RESOURCE;
		throw new IllegalArgumentException("Unknown asset path: " + path);
	}
}
