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

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin that serves the first-party rich-view runtime assets &mdash; {@code juneau-views.js},
 * {@code juneau-ribbon.js}, {@code juneau-renders.js}, {@code juneau-views.css}, and the opt-in
 * {@code juneau-pages.js} tabs/sub-tabs page runtime (TODO-399 Phase C) &mdash; each at its stable path (design doc
 * §6.1).
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
 * {@code ?v=<buildVersion>-<hash8>} cache-buster (see {@link #viewAssetUrl(String)}), where {@code hash8} is an
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
	 * The URL path at which the opt-in tabs/sub-tabs page runtime is served (relative to the host mount) &mdash;
	 * TODO-399 Phase C.  A separate, opt-in asset (Decision 2(A)): single-view pages never load it.
	 */
	public static final String PAGES_JS_PATH = "/juneau-pages.js";

	/**
	 * The frozen {@code VIEW_META} contract-version handshake constant, kept in one source of truth with the value the
	 * model emits ({@link ViewDef#CONTRACT_VERSION}).
	 */
	public static final String CONTRACT_VERSION = ViewDef.CONTRACT_VERSION;

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

	/** Classpath location of the shipped page runtime (TODO-399 Phase C). */
	static final String PAGES_JS_RESOURCE = "/org/apache/juneau/views/juneau-pages.js";

	/** Content type emitted for the JavaScript assets. */
	static final String JS_CONTENT_TYPE = "text/javascript;charset=utf-8";

	/** Content type emitted for the stylesheet asset. */
	static final String CSS_CONTENT_TYPE = "text/css;charset=utf-8";

	/** {@code Cache-Control} header emitted for every asset (1 day). */
	static final String CACHE_CONTROL = "max-age=86400, public";

	/** Per-resource byte cache, populated on first request (the shipped files never vary). */
	private static final Map<String,byte[]> CACHE = new ConcurrentHashMap<>();

	/** Per-path content-hash cache, populated on first {@link #viewAssetUrl(String)} call (mirrors {@link #CACHE}). */
	private static final Map<String,String> HASH_CACHE = new ConcurrentHashMap<>();

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
	 * [GET /juneau-pages.js] &mdash; serve the opt-in tabs/sub-tabs page runtime (TODO-399 Phase C).
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
	 * Returns the servlet-relative URL for a served asset, carrying a {@code ?v=<buildVersion>-<hash8>} content-
	 * sensitive cache-buster suitable for a page's {@code head=} block (see the class Javadoc's cache-busting
	 * section for why the buster is content- rather than purely version-keyed).
	 *
	 * @param path One of the asset path constants ({@link #VIEWS_JS_PATH}, {@link #RIBBON_JS_PATH},
	 * 	{@link #RENDERS_JS_PATH}, {@link #VIEWS_CSS_PATH}, {@link #ICONS_JS_PATH}, {@link #PAGES_JS_PATH}).
	 * @return The servlet-relative asset URL with the version+content-hash cache-buster appended.
	 */
	public static String viewAssetUrl(String path) {
		return "servlet:" + path + "?v=" + buildVersion() + "-" + contentHash(path);
	}

	/** Reads (and caches) the classpath asset and wraps it as a cacheable {@link HttpResource}. */
	private static HttpResource serve(String resource, String contentType) {
		var bytes = CACHE.computeIfAbsent(resource, ViewsMixin::load);
		return HttpResourceBean.of(
			ByteArrayBody.of(bytes, contentType),
			list(ContentType.of(contentType), CacheControl.of(CACHE_CONTROL))
		);
	}

	/** Reads a shipped classpath asset into a byte array, wrapping the (effectively unreachable) IO failure. */
	private static byte[] load(String resource) {
		try (var in = ViewsMixin.class.getResourceAsStream(resource)) {
			if (in == null)
				throw new IOException("Classpath resource not found: " + resource);
			return IoUtils.readBytes(in);
		} catch (IOException e) {  // HTT: unreachable - the asset ships in the same jar as this class.
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Resolves the framework build version for asset cache-busting, falling back to {@code "dev"} when unset (e.g.
	 * running from an IDE/test classpath rather than a packaged jar).
	 */
	private static String buildVersion() {
		var v = ViewsMixin.class.getPackage().getImplementationVersion();
		return v == null ? "dev" : v;  // HTT: the non-null branch only fires from a packaged jar manifest - unreachable against unpackaged target/classes.
	}

	/** Computes (and caches) the given asset path's 8-hex-char content hash from its served bytes. */
	private static String contentHash(String path) {
		return HASH_CACHE.computeIfAbsent(path, p -> hash8(CACHE.computeIfAbsent(resourceFor(p), ViewsMixin::load)));
	}

	/** Maps a public asset path constant to its classpath resource constant (content-hashing only; routing itself is by {@code @RestGet(path=...)}). */
	private static String resourceFor(String path) {
		if (VIEWS_JS_PATH.equals(path)) return VIEWS_JS_RESOURCE;
		if (RIBBON_JS_PATH.equals(path)) return RIBBON_JS_RESOURCE;
		if (RENDERS_JS_PATH.equals(path)) return RENDERS_JS_RESOURCE;
		if (VIEWS_CSS_PATH.equals(path)) return VIEWS_CSS_RESOURCE;
		if (ICONS_JS_PATH.equals(path)) return ICONS_JS_RESOURCE;
		if (PAGES_JS_PATH.equals(path)) return PAGES_JS_RESOURCE;
		throw new IllegalArgumentException("Unknown asset path: " + path);
	}

	/** Formats a CRC32 checksum of {@code bytes} as a zero-padded 8-hex-char content hash. */
	private static String hash8(byte[] bytes) {
		var crc = new CRC32();
		crc.update(bytes);
		return String.format("%08x", crc.getValue());
	}
}
