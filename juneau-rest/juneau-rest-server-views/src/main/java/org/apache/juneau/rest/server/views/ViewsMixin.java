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

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.util.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Mixin that serves the first-party rich-view runtime assets &mdash; {@code juneau-views.js},
 * {@code juneau-ribbon.js}, {@code juneau-renders.js}, {@code juneau-views.css}, the opt-in
 * {@code juneau-regions.js} region-populate runtime, the opt-in {@code juneau-config.js}/{@code juneau-config.css}
 * column-chooser runtime &mdash; each at its stable path (design doc §6.1), plus deprecated compatibility mounts
 * for the two assets that have since moved to the widget module.
 *
 * <p>
 * Compose into a host resource via {@link Rest#mixins() @Rest(mixins=ViewsMixin.class)}; the asset URLs then become
 * available alongside the host's own endpoints, so browser pages can load the runtime without the application hosting
 * it itself.  This mirrors the sibling {@code DataTablesMixin}/{@code ConsoleChromeMixin} serving pattern (those
 * modules are not dependencies of this one, so they are not linked here).
 *
 * <h5 class='section'>What this ships (and what it deliberately does not):</h5>
 * <p>
 * Juneau's jars don't copy the <a class="doclink" href="https://datatables.net">DataTables</a> library or jQuery.  Only
 * the thin, first-party runtime is served here; the DataTables, jQuery and Buttons files come from the WebJars that
 * {@code juneau-rest-server-datatables} declares, served by {@code WebJarsMixin}.  The
 * base {@code juneau-views.css} {@code .tag} chip is dependency-free (neutral, no colors); {@code console-ui}'s
 * {@code chrome.css} themes the same {@code .tag.<domain>.<value>} classes when present, but this module takes
 * <b>no</b> dependency on it.
 *
 * <h5 class='section'>Relocated: two assets now ship in the widget module</h5>
 * <p>
 * {@code juneau-calendar.js} and {@code juneau-calendar.css} are widget runtimes, not
 * table runtimes, and their bytes now live in {@code juneau-rest-server-widgets} beside the bean contracts that
 * drive them.  {@link WidgetsMixin} is where a new application gets them.
 *
 * <p>
 * This mixin keeps a <b>deprecated</b> mount and path constant for each of the two so that an existing
 * {@code @Rest(mixins=ViewsMixin.class)} application keeps working with no change: the mount reads the widget
 * module's bytes off the classpath (that module is a compile-scope dependency of this one) rather than holding a
 * second copy, so the body and the cache-buster are identical to what {@link WidgetsMixin} serves.  Composing both
 * mixins is therefore harmless.
 *
 * <p>
 * The relocation does <b>not</b> make that script standalone: it still resolves glyphs through this module's
 * {@code juneau-icons.js} and pushes its popovers onto the ONE shared layer stack {@code juneau-views.js} publishes.
 * A page loading the widget calendar runtime must still load this module's {@code juneau-icons.js} and
 * {@code juneau-views.js} first.
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
 * {@link #CONTRACT_VERSION} is the single wire-contract discriminator for a {@code VIEW_META} sidecar.
 * {@code juneau-views.js} bakes in the same value so the client can fail loud when a served sidecar's
 * {@code contractVersion} differs from the runtime's.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 * <p>
 * The mount paths are pinned at the op level by {@code @RestGet(path=...)} on the serving methods; a class-level
 * {@code @Rest(paths=...)} would be silently ignored under the mixin pattern.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewTable}
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
@SuppressWarnings({
	"java:S1133" // Still referenced externally (compatibility mount, forRemoval=false); removal is a separate deprecation-cycle decision.
})
public class ViewsMixin {

	/**
	 * The URL path at which the client initializer is served (relative to the host mount).  A page with any
	 * datatables card must load {@code juneau-datatables.js} ({@link #DATATABLES_JS_PATH}) <b>before</b> this script,
	 * so {@code window.JuneauDataTables} is already present when {@code buildOptions} wires up that card's ajax.
	 * This mixin does not serve {@code juneau-datatables.js} itself &mdash; the resource
	 * mixes in {@code DataTablesMixin} for that, the same way it already mixes in {@code ViewsMixin} for this
	 * script; a duplicate {@code @RestGet} for the same path on two mixed-in classes would fail route
	 * registration.  A missing {@code window.JuneauDataTables} when a server-side table initializes fails loudly (console warning,
	 * no silent GET fallback) rather than silently misbehaving.  A page with any ribbon options should likewise load
	 * {@code juneau-ribbon.js} before this script, so {@code window.JuneauViews.ribbon} is present when
	 * {@code buildOptions} merges that view's active ribbon state into the request; unlike the
	 * {@code JuneauDataTables} case this one degrades rather than fails &mdash; a missing
	 * {@code window.JuneauViews.ribbon} warns and simply contributes no ribbon filters, since a view with no ribbon
	 * configured never needed {@code juneau-ribbon.js} loaded at all.
	 */
	public static final String VIEWS_JS_PATH = "/juneau-views.js";

	/**
	 * The URL path of the DataTables glue script, {@code juneau-datatables.js} (the same path as
	 * {@link DataTablesMixin#GLUE_PATH}).
	 *
	 * <p>
	 * Named here only so {@link #viewAssetUrl(RestRequest, String)} can build its cache-busted URL.  This mixin does
	 * <b>not</b> serve it (see {@link #VIEWS_JS_PATH} for why); {@link DataTablesMixin} does.  On a console page the
	 * {@code "datatables-glue"} toolkit pack emits it for every {@code type="datatables"} card, client or server mode,
	 * after jQuery and DataTables and before {@code juneau-views.js}.  A host with datatables cards composes
	 * {@link DataTablesMixin} and {@code WebJarsMixin}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(mixins={ViewsMixin.<jk>class</jk>, DataTablesMixin.<jk>class</jk>, WebJarsMixin.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 * 		<jc>// Emitted by the "datatables-glue" pack before juneau-views.js:</jc>
	 * 		<jc>//   ViewsMixin.viewAssetUrl(req, ViewsMixin.DATATABLES_JS_PATH)</jc>
	 * 	}
	 * </p>
	 */
	public static final String DATATABLES_JS_PATH = DataTablesMixin.GLUE_PATH;

	/** The URL path at which the ribbon runtime is served (relative to the host mount). */
	public static final String RIBBON_JS_PATH = "/juneau-ribbon.js";

	/** The URL path at which the renderer registry is served (relative to the host mount). */
	public static final String RENDERS_JS_PATH = "/juneau-renders.js";

	/** The URL path at which the base view stylesheet is served (relative to the host mount). */
	public static final String VIEWS_CSS_PATH = "/juneau-views.css";

	/**
	 * The URL path at which the icon registry is served (relative to the host mount).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Page-level sprite layers, read once before the chrome draws:</jc>
	 * 	<jc>// &lt;script src="/views/juneau-icons.js"</jc>
	 * 	<jc>//         data-juneau-icon-replacement="/my/set.svg"</jc>
	 * 	<jc>//         data-juneau-icon-override="/my/overrides.svg"&gt;&lt;/script&gt;</jc>
	 * </p>
	 */
	public static final String ICONS_JS_PATH = "/juneau-icons.js";

	/**
	 * The URL path at which the column-search engine is served (relative to the host mount).  This
	 * dependency-free {@code <script>} loads before {@code juneau-views.js} (like the icon registry): it publishes
	 * {@code JuneauViews.search} &mdash; the client mirror of the server-side {@link SearchExpressionParser}/
	 * {@link SearchOperatorSet}/{@link InMemoryBeanQueryContext} context that the header search popup and a client-mode
	 * grid use to parse and evaluate each column's raw search expression.
	 */
	public static final String SEARCH_JS_PATH = "/juneau-search.js";

	/**
	 * The URL path at which the general page-state store is served (relative to the host mount).  This
	 * dependency-free {@code <script>} loads before {@code juneau-views.js} and {@code juneau-config.js} (like the
	 * icon registry and column-search engine): it publishes {@code JuneauViews.pageState} &mdash; a per-table /
	 * per-page key/value store, backed by {@code localStorage} by default and degrading to a silent no-op when
	 * browser storage is blocked.  The datatable View Settings dialog and any other page preference share it.
	 */
	public static final String PAGESTATE_JS_PATH = "/juneau-pagestate.js";

	/**
	 * The URL path at which the shareable "Copy link" URL-state codec is served (relative to the host mount).  This
	 * dependency-free {@code <script>} loads before {@code juneau-views.js} and {@code juneau-config.js} (like the
	 * icon registry, column-search engine, and page-state store): it publishes {@code JuneauViews.urlState} &mdash;
	 * the single codec for the one {@code ?state=} query parameter that carries a shareable link's live tab, primary-
	 * table filters, and primary-table sort (never the browser-local View Settings, and never nested tables).
	 */
	public static final String URLSTATE_JS_PATH = "/juneau-urlstate.js";

	/**
	 * The URL path at which the shared SVG symbol sprite is served (relative to the host mount).
	 * {@code juneau-icons.js} fetches this next to itself by default (pack {@code original}); the key/legend
	 * file is not served to browsers.  An app replaces part or all of this shipped set per page with a replacement
	 * and/or override sprite that uses the same {@code juneau-sym-{stem}} symbol ids.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Page-level sprite layers, read once before the chrome draws:</jc>
	 * 	<jc>// &lt;script src="/views/juneau-icons.js"</jc>
	 * 	<jc>//         data-juneau-icon-replacement="/my/set.svg"</jc>
	 * 	<jc>//         data-juneau-icon-override="/my/overrides.svg"&gt;&lt;/script&gt;</jc>
	 * </p>
	 */
	public static final String SYMBOLS_SVG_PATH = "/juneau-symbols.svg";

	/**
	 * The URL path at which the opt-in Material Symbols Outlined sprite is served (relative to the host mount).
	 * Selected as the legacy replacement layer by {@code JuneauViews.icons.pack("material")} /
	 * {@code data-juneau-icon-pack="material"}; new apps point {@code data-juneau-icon-replacement} /
	 * {@code JuneauViews.icons.sprites({replacementUrl})} at the set they want instead.
	 * Default remains {@link #SYMBOLS_SVG_PATH} (Juneau's own shipped set).  The Material pack loads as the replacement
	 * layer on top of that shipped sprite, so stems the Material file does not carry fall back to the shipped glyph.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Opt in to the Material Symbols sprite as the replacement layer over the shipped set, before the chrome draws:</jc>
	 * 	<jc>// &lt;script src="/views/juneau-icons.js" data-juneau-icon-pack="material"&gt;&lt;/script&gt;</jc>
	 * 	<jc>// or, from script before first paint:</jc>
	 * 	<jc>// JuneauViews.icons.pack("material");</jc>
	 * </p>
	 */
	public static final String SYMBOLS_MATERIAL_SVG_PATH = "/juneau-symbols-material.svg";

	/**
	 * The URL path at which the opt-in region-populate runtime is served (relative to the host mount).  A page with
	 * no {@code data-juneau-region} element never needs to load it.
	 *
	 * <h5 class='section'>Load order is a contract, not a preference:</h5>
	 * <p>
	 * This {@code <script>} MUST come after {@code juneau-views.js}.  The region runtime publishes onto the same
	 * {@code window.JuneauViews} namespace that {@code juneau-views.js} creates, and reuses that runtime's
	 * {@code renderAsyncStatus} status renderer rather than defining one of its own.
	 */
	public static final String REGIONS_JS_PATH = "/juneau-regions.js";

	/**
	 * The URL path at which the opt-in region-populate paint library is served (relative to the host mount).  A
	 * page of purely custom regions (every populate hand-writes its own DOM) never needs to load it.
	 *
	 * <h5 class='section'>Load order is a contract, not a preference:</h5>
	 * <p>
	 * This {@code <script>} MUST come after {@code juneau-regions.js} (itself after {@code juneau-views.js}).
	 * {@code fieldGrid}'s renderer dispatch reads the renderer registry {@code juneau-views.js} publishes and its
	 * markdown/sanitized-HTML paths reuse that runtime's allowlist copiers, and {@code ctx.helpers}
	 * (a bare pass-through of this asset's exports) is a field of the {@code ctx} {@code juneau-regions.js} freezes.
	 */
	public static final String HELPERS_JS_PATH = "/juneau-helpers.js";

	/**
	 * The URL path at which the opt-in console-output module is served (relative to the host mount).
	 *
	 * <h5 class='section'>Load order is a contract, not a preference:</h5>
	 * <p>
	 * This {@code <script>} MUST come after {@code juneau-regions.js} (itself after {@code juneau-views.js}) and
	 * before {@code juneau-helpers.js}.  It reads the URL helpers {@code juneau-views.js} exports and registers its
	 * populator with {@code juneau-regions.js}; icons are built through {@code JuneauViews.helpers.icon}, which is
	 * resolved at call time, so loading before {@code juneau-helpers.js} is safe.
	 *
	 * @since 10.0.0
	 */
	public static final String CONSOLE_OUTPUT_JS_PATH = "/juneau-console-output.js";

	/**
	 * The URL path at which the opt-in run-view module is served (relative to the host mount).
	 *
	 * <h5 class='section'>Load order is a contract, not a preference:</h5>
	 * <p>
	 * This {@code <script>} MUST come after {@code juneau-regions.js} (itself after {@code juneau-views.js}) and
	 * before {@code juneau-helpers.js}.  It registers its populator with {@code juneau-regions.js} at load time;
	 * icons are built through {@code JuneauViews.helpers.icon}, which is resolved at call time, so loading before
	 * {@code juneau-helpers.js} is safe.
	 *
	 * @since 10.0.0
	 */
	public static final String RUN_VIEW_JS_PATH = "/juneau-run-view.js";

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
	 * The URL path at which the opt-in reusable-calendar runtime is served (relative to the host mount).  A page
	 * with no calendar never loads it.
	 *
	 * <h5 class='section'>Load order is a contract, not a preference:</h5>
	 * <p>
	 * This {@code <script>} MUST come after {@code juneau-views.js}.  The calendar's {@code "+N more"} popover is
	 * pushed onto the ONE shared layer stack that {@code juneau-views.js} publishes, so that Escape, focus return
	 * and z-order are the same as every other layer in the page.  The calendar deliberately defines no second
	 * stack of its own: if the shared one is absent it fails loud (console error plus a visible in-cell refusal)
	 * rather than quietly opening a popover nothing can dismiss.
	 *
	 * @deprecated The calendar runtime now ships in the widget module; compose
	 * 	{@link org.apache.juneau.rest.server.widgets.WidgetsMixin} and use its constant of the same name.  This one
	 * 	remains only so that an application composing this mixin alone keeps serving the asset at the same URL.
	 */
	@Deprecated(since = "10.0.0", forRemoval = false)
	public static final String CALENDAR_JS_PATH = "/juneau-calendar.js";

	/**
	 * The URL path at which the opt-in reusable-calendar stylesheet is served (relative to the host mount).
	 *
	 * @deprecated The calendar stylesheet now ships in the widget module; compose
	 * 	{@link org.apache.juneau.rest.server.widgets.WidgetsMixin} and use its constant of the same name.  This one
	 * 	remains only so that an application composing this mixin alone keeps serving the asset at the same URL.
	 */
	@Deprecated(since = "10.0.0", forRemoval = false)
	public static final String CALENDAR_CSS_PATH = "/juneau-calendar.css";

	/**
	 * The URL path at which the console shell runtime is served (relative to the host mount).
	 *
	 * <p>
	 * {@code juneau-console.js} mounts the C1 page contract (header, nav, cards, footer) from a page's
	 * {@code #juneau-page} script island. This module takes a compile-scope dependency on
	 * {@code juneau-rest-server-console-ui} so that a {@code views}-only application can serve the console shell
	 * from the same mixin it already composes, without separately adding {@link ConsoleChromeMixin}. The toolkit
	 * pack ({@code juneau-views.js} and siblings) deliberately does <b>not</b> bundle this script: a console page's
	 * shell loads it once via {@code PageCapture.writeBody}, and a second copy from the toolkit pack would
	 * double-load the runtime.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// A views-only application serves the console shell at the same stable (unprefixed) path ConsoleChromeMixin serves it at.</jc>
	 * 	<ja>&#64;Rest</ja>(mixins=ViewsMixin.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {}
	 *
	 * 	<jc>// GET /juneau-console.js now returns the shell runtime.</jc>
	 * </p>
	 */
	public static final String CONSOLE_JS_PATH = ConsoleChromeMixin.CONSOLE_JS_PATH_UNPREFIXED;

	/**
	 * The frozen {@code VIEW_META} contract-version handshake constant. FTL {@code <@card type="datatables">}
	 * catalogs emit this on the lifted {@code view} object.
	 *
	 * <p>
	 * Templates never author {@code view.contractVersion}: the framework stamps this value when it lifts the catalog,
	 * so a contract bump never touches adopter templates. A pre-built envelope body is rejected at render time, and
	 * {@code ConsoleTemplateValidator}'s {@code view-contract-version} rule flags a literal pin statically.
	 */
	public static final String CONTRACT_VERSION = "5";

	/**
	 * The app-header refresh-envelope contract-version handshake constant that the console shell bakes in.
	 * Deliberately distinct from {@link #CONTRACT_VERSION} and {@link #BAR_CONTRACT_VERSION}: a header-envelope
	 * revision must never force a view-sidecar or bar-sidecar bump, or vice-versa.
	 */
	public static final String HEADER_CONTRACT_VERSION = "1";

	/**
	 * The bar-slot refresh-envelope contract-version handshake constant that the console shell bakes in.
	 * Deliberately a distinct constant from {@link #HEADER_CONTRACT_VERSION} (see that constant).
	 */
	public static final String BAR_CONTRACT_VERSION = "1";

	/**
	 * The slot-envelope ({@code SLOT_META}) contract-version handshake constant that {@code juneau-views.js}
	 * bakes in. Deliberately a distinct constant from {@link #CONTRACT_VERSION} (the
	 * inner {@code VIEW_META} object): a slot-envelope revision must never force a view-sidecar bump, or vice-versa.
	 */
	public static final String SLOT_CONTRACT_VERSION = "1";

	/** Classpath location of the shipped initializer. */
	static final String VIEWS_JS_RESOURCE = "/org/apache/juneau/views/juneau-views.js";

	/** Classpath location of the DataTables glue script, shipped by {@code juneau-rest-server-datatables}. */
	static final String DATATABLES_JS_RESOURCE = "/org/apache/juneau/rest/server/datatables/juneau-datatables.js";

	/** Classpath location of the shipped ribbon runtime. */
	static final String RIBBON_JS_RESOURCE = "/org/apache/juneau/views/juneau-ribbon.js";

	/** Classpath location of the shipped renderer registry. */
	static final String RENDERS_JS_RESOURCE = "/org/apache/juneau/views/juneau-renders.js";

	/** Classpath location of the shipped base stylesheet. */
	static final String VIEWS_CSS_RESOURCE = "/org/apache/juneau/views/juneau-views.css";

	/** Classpath location of the shipped icon registry. */
	static final String ICONS_JS_RESOURCE = "/org/apache/juneau/views/juneau-icons.js";

	/** Classpath location of the shipped column-search engine (client mirror of the Java column-search engine). */
	static final String SEARCH_JS_RESOURCE = "/org/apache/juneau/views/juneau-search.js";

	/** Classpath location of the shipped general page-state store. */
	static final String PAGESTATE_JS_RESOURCE = "/org/apache/juneau/views/juneau-pagestate.js";

	/** Classpath location of the shipped shareable-link URL-state codec. */
	static final String URLSTATE_JS_RESOURCE = "/org/apache/juneau/views/juneau-urlstate.js";

	/** Classpath location of the shipped SVG symbol sprite (Juneau's own set, default pack). */
	static final String SYMBOLS_SVG_RESOURCE = "/org/apache/juneau/views/juneau-symbols.svg";

	/** Classpath location of the opt-in Material Symbols Outlined sprite. */
	static final String SYMBOLS_MATERIAL_SVG_RESOURCE = "/org/apache/juneau/views/juneau-symbols-material.svg";

	/** Classpath location of the shipped region-populate runtime. */
	static final String REGIONS_JS_RESOURCE = "/org/apache/juneau/views/juneau-regions.js";

	/** Classpath location of the shipped region-populate paint library. */
	static final String HELPERS_JS_RESOURCE = "/org/apache/juneau/views/juneau-helpers.js";

	/** Classpath location of the shipped console-output module. */
	static final String CONSOLE_OUTPUT_JS_RESOURCE = "/org/apache/juneau/views/juneau-console-output.js";

	/** Classpath location of the shipped run-view module. */
	static final String RUN_VIEW_JS_RESOURCE = "/org/apache/juneau/views/juneau-run-view.js";

	/** Classpath location of the shipped column-chooser runtime. */
	static final String CONFIG_JS_RESOURCE = "/org/apache/juneau/views/juneau-config.js";

	/** Classpath location of the shipped column-chooser stylesheet. */
	static final String CONFIG_CSS_RESOURCE = "/org/apache/juneau/views/juneau-config.css";

	/**
	 * Classpath location of the reusable-calendar runtime, which the <b>widget</b> module now ships.
	 *
	 * <p>
	 * The two widget-owned assets below are read out of the widget module's classpath (a compile-scope dependency of
	 * this one), never copied into this module's resources.  Reading rather than copying is what makes this mixin's
	 * deprecated accessor serve the same bytes the widget mixin serves, and it is why the absent-from-this-module
	 * guard in the serving test can assert a move rather than a duplication.
	 */
	static final String CALENDAR_JS_RESOURCE = "/org/apache/juneau/widgets/juneau-calendar.js";

	/** Classpath location of the reusable-calendar stylesheet, which the widget module now ships (see {@link #CALENDAR_JS_RESOURCE}). */
	static final String CALENDAR_CSS_RESOURCE = "/org/apache/juneau/widgets/juneau-calendar.css";

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
	 * The same helper, but anchored on the <b>widget</b> mixin, for the two assets that module now ships and this
	 * one only keeps deprecated mounts for.
	 *
	 * <p>
	 * The anchor governs two things: which classpath the bytes are read from (irrelevant here &mdash; the widget
	 * module is a compile-scope dependency, so either anchor finds the same bytes) and which module's
	 * implementation version the {@code ?v=<buildVersion>-<hash8>} cache-buster carries.  Anchoring the relocated
	 * two on the widget mixin makes this mixin's deprecated URL for an asset <b>byte-identical</b> to the widget
	 * mixin's URL for it, buster included, rather than merely serving the same body behind two differently-versioned
	 * URLs.  A page that mixes both mixins therefore cannot end up caching the same script twice.
	 */
	private static final ClasspathAssetCache WIDGET_ASSET_CACHE = new ClasspathAssetCache(WidgetsMixin.class);

	/**
	 * The same helper, but anchored on the <b>console-ui</b> mixin, for the console shell script that module ships
	 * and this one serves at {@link #CONSOLE_JS_PATH}.  Anchoring on {@link ConsoleChromeMixin} (rather than this
	 * class) makes this mount's {@code ?v=<buildVersion>-<hash8>} cache-buster identical to
	 * {@code ConsoleChromeMixin}'s own mount of the same bytes, so a page that composes both mixins cannot end up
	 * caching the same script twice under two different busters.
	 */
	private static final ClasspathAssetCache CONSOLE_ASSET_CACHE = new ClasspathAssetCache(ConsoleChromeMixin.class);

	/**
	 * The same helper, but anchored on {@link DataTablesMixin}, for {@link #DATATABLES_JS_PATH}: the cache-buster
	 * carries the datatables module's implementation version, since that module ships (and serves) the bytes.
	 */
	private static final ClasspathAssetCache DATATABLES_ASSET_CACHE = new ClasspathAssetCache(DataTablesMixin.class);

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
	 * [GET /juneau-search.js] &mdash; serve the column-search engine.
	 *
	 * @return The column-search engine as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=SEARCH_JS_PATH,
		summary="Juneau rich-view column-search engine",
		description="First-party, dependency-free JavaScript mirror of the server-side column-search engine (parser, operator metadata, and per-type leaf semantics) published as JuneauViews.search.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getSearchScript() {
		return serve(SEARCH_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-pagestate.js] &mdash; serve the general page-state store.
	 *
	 * @return The page-state store as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=PAGESTATE_JS_PATH,
		summary="Juneau rich-view page-state store",
		description="First-party, dependency-free JavaScript key/value store (per-table and per-page namespaces, localStorage-backed with a blocked-storage no-op fallback) published as JuneauViews.pageState.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getPageStateScript() {
		return serve(PAGESTATE_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-urlstate.js] &mdash; serve the shareable-link URL-state codec.
	 *
	 * @return The URL-state codec as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=URLSTATE_JS_PATH,
		summary="Juneau rich-view shareable-link URL-state codec",
		description="First-party, dependency-free JavaScript codec for the one ?state= query parameter (tab/filter/sort of the primary table only, never View Settings or nested tables) published as JuneauViews.urlState.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getUrlStateScript() {
		return serve(URLSTATE_JS_RESOURCE, JS_CONTENT_TYPE);
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
	 * [GET /juneau-symbols-material.svg] &mdash; serve the opt-in Material Symbols Outlined sprite.
	 *
	 * @return The Material sprite as an SVG {@link HttpResource}.
	 */
	@RestGet(
		path=SYMBOLS_MATERIAL_SVG_PATH,
		summary="Juneau rich-view Material Symbols SVG sprite (opt-in)",
		description="NOTICE-attributed Material Symbols Outlined sprite. Apps normally supply their own set via the icon replacementUrl (data-juneau-icon-replacement); JuneauViews.icons.pack(\"material\") remains the legacy base selector. Default remains juneau-symbols.svg.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getSymbolsMaterialSvg() {
		return serve(SYMBOLS_MATERIAL_SVG_RESOURCE, SVG_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-regions.js] &mdash; serve the opt-in region-populate runtime.
	 *
	 * @return The region-populate runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=REGIONS_JS_PATH,
		summary="Juneau rich-view region-populate runtime",
		description="First-party, opt-in JavaScript that resolves a name-keyed populator against a data-juneau-region container.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getRegionsScript() {
		return serve(REGIONS_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-helpers.js] &mdash; serve the opt-in region-populate paint library.
	 *
	 * @return The paint library as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=HELPERS_JS_PATH,
		summary="Juneau rich-view region-populate paint library",
		description="First-party, opt-in JavaScript helper library (fieldGrid, kvTable, tabStrip, dataPane, recordTable, and the rest of JuneauViews.helpers) for a data-juneau-region populator.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getHelpersScript() {
		return serve(HELPERS_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-console-output.js] &mdash; serve the opt-in console-output module.
	 *
	 * @return The console-output module as a JavaScript {@link HttpResource}.
	 * @since 10.0.0
	 */
	@RestGet(
		path=CONSOLE_OUTPUT_JS_PATH,
		summary="Juneau rich-view console-output module",
		description="First-party, opt-in JavaScript for the console-output region: a live-tailing, append-only log pane.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getConsoleOutputScript() {
		return serve(CONSOLE_OUTPUT_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-run-view.js] &mdash; serve the opt-in run-view module.
	 *
	 * @return The run-view module as a JavaScript {@link HttpResource}.
	 * @since 10.0.0
	 */
	@RestGet(
		path=RUN_VIEW_JS_PATH,
		summary="Juneau rich-view run-view module",
		description="First-party, opt-in JavaScript for the run-view region: a live test and build run summary with suites, failures and notes.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getRunViewScript() {
		return serve(RUN_VIEW_JS_RESOURCE, JS_CONTENT_TYPE);
	}

	/**
	 * [GET /juneau-config.js] &mdash; serve the opt-in column-chooser runtime (column configuration is localStorage-persisted).
	 *
	 * @return The column-chooser runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=CONFIG_JS_PATH,
		summary="Juneau rich-view column-chooser runtime",
		description="First-party, opt-in JavaScript that renders the View-tab column chooser and column configuration (localStorage-persisted) for a columnConfig view.",
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
	 * Returns the servlet-relative URL for a served asset, carrying a {@code ?v=<buildVersion>-<hash8>} content-
	 * sensitive cache-buster suitable for a page's {@code head=} block (see the class Javadoc's cache-busting
	 * section for why the buster is content- rather than purely version-keyed).
	 *
	 * @param path One of the asset path constants ({@link #VIEWS_JS_PATH}, {@link #RIBBON_JS_PATH},
	 * 	{@link #RENDERS_JS_PATH}, {@link #VIEWS_CSS_PATH}, {@link #ICONS_JS_PATH}, {@link #SYMBOLS_SVG_PATH},
	 * 	{@link #SYMBOLS_MATERIAL_SVG_PATH}, {@link #REGIONS_JS_PATH}, {@link #HELPERS_JS_PATH}, {@link #CONSOLE_OUTPUT_JS_PATH}, {@link #RUN_VIEW_JS_PATH}, {@link #CONFIG_JS_PATH}, {@link #CONFIG_CSS_PATH},
	 * 	{@link #CALENDAR_JS_PATH}, {@link #CALENDAR_CSS_PATH}, {@link #DATATABLES_JS_PATH}).
	 * @return The servlet-relative asset URL with the version+content-hash cache-buster appended.
	 */
	public static String viewAssetUrl(String path) {
		return "servlet:" + path + cacheFor(path).cacheBuster(resourceFor(path));
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
	 * 	{@link #SYMBOLS_MATERIAL_SVG_PATH}, {@link #REGIONS_JS_PATH}, {@link #HELPERS_JS_PATH}, {@link #CONSOLE_OUTPUT_JS_PATH}, {@link #RUN_VIEW_JS_PATH}, {@link #CONFIG_JS_PATH}, {@link #CONFIG_CSS_PATH},
	 * 	{@link #CALENDAR_JS_PATH}, {@link #CALENDAR_CSS_PATH}, {@link #DATATABLES_JS_PATH}).
	 * @return The absolute asset URL with the version+content-hash cache-buster appended.
	 */
	public static String viewAssetUrl(RestRequest req, String path) {
		return req.getUriResolver().resolve("servlet:" + path) + cacheFor(path).cacheBuster(resourceFor(path));
	}

	/**
	 * [GET /juneau-calendar.js] &mdash; serve the opt-in reusable-calendar runtime from the widget module's classpath.
	 *
	 * @return The calendar runtime as a JavaScript {@link HttpResource}.
	 * @deprecated Compose {@link WidgetsMixin} instead, which ships these bytes.  This mount stays so that an
	 * 	application composing only this mixin keeps serving the asset at the same URL with the same body.
	 */
	@Deprecated(since = "10.0.0", forRemoval = false)
	@RestGet(
		path=CALENDAR_JS_PATH,
		summary="Juneau reusable-calendar runtime (relocated)",
		description="Deprecated compatibility mount. The calendar runtime now ships in juneau-rest-server-widgets; these bytes are read from that module and are identical to the ones WidgetsMixin serves.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getCalendarScript() {
		return WIDGET_ASSET_CACHE.serve(CALENDAR_JS_RESOURCE, JS_CONTENT_TYPE, CACHE_CONTROL);
	}

	/**
	 * [GET /juneau-calendar.css] &mdash; serve the opt-in reusable-calendar stylesheet from the widget module's classpath.
	 *
	 * @return The calendar stylesheet as a CSS {@link HttpResource}.
	 * @deprecated Compose {@link WidgetsMixin} instead, which ships these bytes.  This mount stays so that an
	 * 	application composing only this mixin keeps serving the asset at the same URL with the same body.
	 */
	@Deprecated(since = "10.0.0", forRemoval = false)
	@RestGet(
		path=CALENDAR_CSS_PATH,
		summary="Juneau reusable-calendar stylesheet (relocated)",
		description="Deprecated compatibility mount. The calendar stylesheet now ships in juneau-rest-server-widgets; these bytes are read from that module and are identical to the ones WidgetsMixin serves.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getCalendarStylesheet() {
		return WIDGET_ASSET_CACHE.serve(CALENDAR_CSS_RESOURCE, CSS_CONTENT_TYPE, CACHE_CONTROL);
	}

	/**
	 * [GET /juneau-console.js] &mdash; serve the console shell runtime from the console-ui module's classpath.
	 *
	 * @return The console shell runtime as a JavaScript {@link HttpResource}.
	 */
	@RestGet(
		path=CONSOLE_JS_PATH,
		summary="Juneau console shell runtime",
		description="Renders the #juneau-page contract into header, nav, cards and footer; served from the console-ui module's classpath.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getConsoleJs() {
		return CONSOLE_ASSET_CACHE.serve(ConsoleChromeMixin.CONSOLE_JS_RESOURCE, JS_CONTENT_TYPE, CACHE_CONTROL);
	}

	/** Reads (and caches) the classpath asset and wraps it as a cacheable {@link HttpResource}. */
	private static HttpResource serve(String resource, String contentType) {
		return ASSET_CACHE.serve(resource, contentType, CACHE_CONTROL);
	}

	/**
	 * Selects the cache that owns the given asset path: the two relocated widget assets hash and version through
	 * the widget module's cache (see {@link #WIDGET_ASSET_CACHE}), the DataTables glue through the datatables
	 * module's (see {@link #DATATABLES_ASSET_CACHE}), everything else through this module's own.
	 */
	private static ClasspathAssetCache cacheFor(String path) {
		if (eqa(path, CALENDAR_JS_PATH, CALENDAR_CSS_PATH))
			return WIDGET_ASSET_CACHE;
		if (eq(path, DATATABLES_JS_PATH))
			return DATATABLES_ASSET_CACHE;
		return ASSET_CACHE;
	}

	/** Maps a public asset path constant to its classpath resource constant (content-hashing only; routing itself is by {@code @RestGet(path=...)}). */
	@SuppressWarnings({
		"java:S3776" // Flat path-to-resource lookup table; one branch per asset, no nesting.
	})
	private static String resourceFor(String path) {
		if (eq(path, VIEWS_JS_PATH)) return VIEWS_JS_RESOURCE;
		if (eq(path, RIBBON_JS_PATH)) return RIBBON_JS_RESOURCE;
		if (eq(path, RENDERS_JS_PATH)) return RENDERS_JS_RESOURCE;
		if (eq(path, VIEWS_CSS_PATH)) return VIEWS_CSS_RESOURCE;
		if (eq(path, ICONS_JS_PATH)) return ICONS_JS_RESOURCE;
		if (eq(path, SEARCH_JS_PATH)) return SEARCH_JS_RESOURCE;
		if (eq(path, PAGESTATE_JS_PATH)) return PAGESTATE_JS_RESOURCE;
		if (eq(path, URLSTATE_JS_PATH)) return URLSTATE_JS_RESOURCE;
		if (eq(path, SYMBOLS_SVG_PATH)) return SYMBOLS_SVG_RESOURCE;
		if (eq(path, SYMBOLS_MATERIAL_SVG_PATH)) return SYMBOLS_MATERIAL_SVG_RESOURCE;
		if (eq(path, REGIONS_JS_PATH)) return REGIONS_JS_RESOURCE;
		if (eq(path, HELPERS_JS_PATH)) return HELPERS_JS_RESOURCE;
		if (eq(path, CONSOLE_OUTPUT_JS_PATH)) return CONSOLE_OUTPUT_JS_RESOURCE;
		if (eq(path, RUN_VIEW_JS_PATH)) return RUN_VIEW_JS_RESOURCE;
		if (eq(path, CONFIG_JS_PATH)) return CONFIG_JS_RESOURCE;
		if (eq(path, CONFIG_CSS_PATH)) return CONFIG_CSS_RESOURCE;
		if (eq(path, CALENDAR_JS_PATH)) return CALENDAR_JS_RESOURCE;
		if (eq(path, CALENDAR_CSS_PATH)) return CALENDAR_CSS_RESOURCE;
		if (eq(path, DATATABLES_JS_PATH)) return DATATABLES_JS_RESOURCE;
		throw new IllegalArgumentException("Unknown asset path: " + path);
	}
}
