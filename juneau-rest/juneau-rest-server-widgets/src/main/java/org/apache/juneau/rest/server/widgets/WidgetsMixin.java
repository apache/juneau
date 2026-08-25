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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.util.*;

/**
 * The widget toolkit's serving-path mixin: the mount, asset-URL API and serving endpoints for the widget-owned
 * browser runtime ({@code juneau-cards.js}, {@code juneau-calendar.js}, {@code juneau-calendar.css},
 * {@code juneau-chrome.js}), plus the contract-version handshake constants those runtimes bake in.
 *
 * <p>
 * Compose into a host resource via {@link Rest#mixins() @Rest(mixins=WidgetsMixin.class)}; the asset URLs then become
 * available alongside the host's own endpoints.
 *
 * <h5 class='section'>These assets still need the view runtime beside them</h5>
 * <p>
 * The bytes live here, but they are <b>not</b> a standalone widget runtime: the card, calendar and chrome scripts
 * resolve their glyphs through the rich-view module's icon registry ({@code juneau-icons.js}) and push their
 * popovers onto the ONE shared layer stack that {@code juneau-views.js} publishes.  A page that loads any of these
 * three scripts must therefore <b>also</b> load the view module's {@code juneau-icons.js} and
 * {@code juneau-views.js}.  "Widgets without views" is deliberately not a goal here; what this module owns is the
 * bean contracts and the bytes, not independence from views.
 *
 * <p>
 * For backwards compatibility the rich-view mixin keeps <b>deprecated</b> accessors at the same paths it served
 * these assets from before they moved, reading the bytes from <b>this</b> module's classpath.  An existing
 * application therefore keeps working unchanged; a new one composes this mixin.
 *
 * <h5 class='section'>Contract-version handshake:</h5>
 * <p>
 * Each {@code *_CONTRACT_VERSION} constant is an <b>alias</b> of the bean constant it mirrors, never a copied
 * literal, so revising a bean's wire contract cannot leave a stale duplicate behind here.  The four are deliberately
 * distinct from one another: a card-envelope revision must never force a calendar, header, or bar-sidecar bump, or
 * vice-versa.
 *
 * <h5 class='section'>Cache-busting + versioned URLs:</h5>
 * <p>
 * {@link #widgetAssetUrl(String)} carries the same {@code ?v=<buildVersion>-<hash8>} content-sensitive cache-buster
 * shape the views mixin uses: an 8-hex-char content hash of the asset's own served bytes, computed once and cached.
 * Keying the buster off content rather than the version string alone matters for {@code -SNAPSHOT} builds, whose
 * version is stable across dev rebuilds.  The hashing itself is the shared
 * {@link org.apache.juneau.commons.utils.ChecksumUtils#hash8(byte[])} function reached through
 * {@link ClasspathAssetCache}; this module holds its <b>own</b> cache instance anchored on this class, so its
 * {@code buildVersion} resolves this module's implementation version rather than a neighbouring module's.  The two
 * modules share the hash function, not a cache.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link CardFieldList}
 * 	<li class='jc'>{@link CalendarDef}
 * 	<li class='jc'>{@link AppHeaderDef}
 * 	<li class='jc'>{@link BarSlot}
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
public class WidgetsMixin {

	/**
	 * The URL path at which the card-layout runtime is served (relative to the host mount).  Load it after the view
	 * module's icon registry &mdash; the refresh button's glyph resolves from there.
	 */
	public static final String CARDS_JS_PATH = "/juneau-cards.js";

	/**
	 * The URL path at which the reusable-calendar runtime is served (relative to the host mount).
	 *
	 * <h5 class='section'>Load order is a contract, not a preference:</h5>
	 * <p>
	 * This {@code <script>} MUST come after the view module's {@code juneau-views.js}.  The calendar's
	 * {@code "+N more"} popover is pushed onto the ONE shared layer stack that script publishes, so Escape, focus
	 * return and z-order match every other layer in the page.  The calendar deliberately defines no second stack of
	 * its own: if the shared one is absent it fails loud rather than opening a popover nothing can dismiss.
	 */
	public static final String CALENDAR_JS_PATH = "/juneau-calendar.js";

	/** The URL path at which the reusable-calendar stylesheet is served (relative to the host mount). */
	public static final String CALENDAR_CSS_PATH = "/juneau-calendar.css";

	/**
	 * The URL path at which the page-chrome runtime is served (relative to the host mount).  Load it after the view
	 * module's icon registry &mdash; header action glyphs resolve from there.
	 */
	public static final String CHROME_JS_PATH = "/juneau-chrome.js";

	/**
	 * The card refresh-envelope contract-version handshake constant that the card runtime bakes in, aliased from the
	 * value the card model emits ({@link CardFieldList#CONTRACT_VERSION}).
	 */
	public static final String CARDS_CONTRACT_VERSION = CardFieldList.CONTRACT_VERSION;

	/**
	 * The per-month calendar-event envelope contract-version handshake constant that the calendar runtime bakes in,
	 * aliased from the value the calendar model emits ({@link CalendarDef#CONTRACT_VERSION}).
	 */
	public static final String CALENDAR_CONTRACT_VERSION = CalendarDef.CONTRACT_VERSION;

	/**
	 * The app-header refresh-envelope contract-version handshake constant that the chrome runtime bakes in, aliased
	 * from the value the header model emits ({@link AppHeaderDef#CONTRACT_VERSION}).
	 */
	public static final String HEADER_CONTRACT_VERSION = AppHeaderDef.CONTRACT_VERSION;

	/**
	 * The bar-slot refresh-envelope contract-version handshake constant that the chrome runtime bakes in, aliased
	 * from the value the bar model emits ({@link BarSlot#CONTRACT_VERSION}).
	 */
	public static final String BAR_CONTRACT_VERSION = BarSlot.CONTRACT_VERSION;

	/** Classpath location of the shipped card-layout runtime. */
	static final String CARDS_JS_RESOURCE = "/org/apache/juneau/widgets/juneau-cards.js";

	/** Classpath location of the shipped reusable-calendar runtime. */
	static final String CALENDAR_JS_RESOURCE = "/org/apache/juneau/widgets/juneau-calendar.js";

	/** Classpath location of the shipped reusable-calendar stylesheet. */
	static final String CALENDAR_CSS_RESOURCE = "/org/apache/juneau/widgets/juneau-calendar.css";

	/** Classpath location of the shipped page-chrome runtime. */
	static final String CHROME_JS_RESOURCE = "/org/apache/juneau/widgets/juneau-chrome.js";

	/** Content type emitted for the JavaScript assets. */
	static final String JS_CONTENT_TYPE = "text/javascript;charset=utf-8";

	/** Content type emitted for the stylesheet asset. */
	static final String CSS_CONTENT_TYPE = "text/css;charset=utf-8";

	/** {@code Cache-Control} header emitted for every asset (1 day). */
	static final String CACHE_CONTROL = "max-age=86400, public";

	/**
	 * Read+cache+hash helper for this module's shipped assets, anchored on this class so
	 * {@link ClasspathAssetCache#buildVersion()} resolves this module's own implementation version (see that class's
	 * javadoc's version-anchor section).  Independent of any other module's cache by construction.
	 */
	private static final ClasspathAssetCache ASSET_CACHE = new ClasspathAssetCache(WidgetsMixin.class);

	/**
	 * [GET /juneau-cards.js] &mdash; serve the card-layout runtime.
	 *
	 * @return The card-layout runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=CARDS_JS_PATH,
		summary="Juneau widget card-layout runtime",
		description="First-party, opt-in JavaScript that enhances a card grid's refreshable cards: contract handshake, built-in refresh button, and an optional per-card poll loop.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getCardsScript() {
		return serve(CARDS_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-calendar.js] &mdash; serve the reusable-calendar runtime.
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
	 * [GET /juneau-calendar.css] &mdash; serve the reusable-calendar stylesheet.
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
	 * [GET /juneau-chrome.js] &mdash; serve the page-chrome runtime.
	 *
	 * @return The page-chrome runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=CHROME_JS_PATH,
		summary="Juneau page-chrome runtime",
		description="First-party, opt-in JavaScript that enhances a page's app-header / avatar / bar-slot chrome: contract handshake, icon hydration, avatar fallback, SAFE host-events, and demand-only same-origin count refresh.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getChromeScript() {
		return serve(CHROME_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * Returns the servlet-relative URL for a widget asset served by this mixin, carrying a
	 * {@code ?v=<buildVersion>-<hash8>} content-sensitive cache-buster suitable for a page's {@code head=} block.
	 *
	 * @param path One of the asset path constants ({@link #CARDS_JS_PATH}, {@link #CALENDAR_JS_PATH},
	 * 	{@link #CALENDAR_CSS_PATH}, {@link #CHROME_JS_PATH}).
	 * @return The servlet-relative asset URL with the version+content-hash cache-buster appended.
	 * @throws IllegalArgumentException If this mixin does not ship the bytes for the given path.
	 */
	public static String widgetAssetUrl(String path) {
		return assetUrl(path, resourceFor(path));
	}

	/**
	 * Returns a real, browser-fetchable <b>absolute</b> URL for a widget asset served by this mixin, resolved against
	 * the given request's context path and mount &mdash; carrying the same cache-buster as
	 * {@link #widgetAssetUrl(String)}.
	 *
	 * <p>
	 * {@link #widgetAssetUrl(String)} returns a {@code servlet:}-prefixed URL that only Juneau's own HTML serializer
	 * resolves.  A template-rendering consumer sits downstream of that serializer, so it never sees the rewrite and
	 * would otherwise receive the literal, unfetchable string.  This overload resolves the URL itself, per-request.
	 *
	 * <p>
	 * The path is validated before the request is touched, so an unserved path is rejected identically with or
	 * without a live request.
	 *
	 * @param req The current request, supplying the context path/mount to resolve against.
	 * @param path One of the asset path constants ({@link #CARDS_JS_PATH}, {@link #CALENDAR_JS_PATH},
	 * 	{@link #CALENDAR_CSS_PATH}, {@link #CHROME_JS_PATH}).
	 * @return The absolute asset URL with the version+content-hash cache-buster appended.
	 * @throws IllegalArgumentException If this mixin does not ship the bytes for the given path.
	 */
	public static String widgetAssetUrl(RestRequest req, String path) {
		return assetUrl(req, path, resourceFor(path));
	}

	/** Composes the servlet-relative URL + cache-buster for a classpath asset shipped by this module. */
	static String assetUrl(String path, String classpathResource) {
		return "servlet:" + path + ASSET_CACHE.cacheBuster(classpathResource);
	}

	/** Composes the request-resolved absolute URL + cache-buster for a classpath asset shipped by this module. */
	static String assetUrl(RestRequest req, String path, String classpathResource) {
		return req.getUriResolver().resolve("servlet:" + path) + ASSET_CACHE.cacheBuster(classpathResource);
	}

	/** Reads (and caches) the classpath asset and wraps it as a cacheable {@link HttpResource}. */
	private static HttpResource serve(String resource, String contentType) {
		return ASSET_CACHE.serve(resource, contentType, CACHE_CONTROL);
	}

	/**
	 * Maps a public asset path constant to the classpath resource this module ships for it.
	 *
	 * <p>
	 * A new asset gets its entry here and its {@code @RestGet} accessor in the same change, so the mount and the
	 * bytes always arrive together and no declared path can resolve to an empty response.
	 */
	private static String resourceFor(String path) {
		if (CARDS_JS_PATH.equals(path)) return CARDS_JS_RESOURCE;
		if (CALENDAR_JS_PATH.equals(path)) return CALENDAR_JS_RESOURCE;
		if (CALENDAR_CSS_PATH.equals(path)) return CALENDAR_CSS_RESOURCE;
		if (CHROME_JS_PATH.equals(path)) return CHROME_JS_RESOURCE;
		throw iaex("Widget asset ''%s'' is not served by this mixin.", path);
	}
}
