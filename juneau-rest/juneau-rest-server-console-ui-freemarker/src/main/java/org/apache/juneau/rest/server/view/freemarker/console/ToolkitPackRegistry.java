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
package org.apache.juneau.rest.server.view.freemarker.console;

import java.util.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Registry of named toolkit packs (ordered CSS + JS asset paths) for the {@code <@page toolkit=...>}
 * directive.
 *
 * <p>
 * Ships the {@code "views"} pack against the real {@link ViewsMixin} path constants. Load order is a
 * contract: CSS then JS, JS in the order the JRM {@code TableSlotPage} uses. A consumer can register
 * extra packs via {@code ConsoleFreemarkerMixin.Builder.registerToolkitPack(...)}.
 *
 * <p>
 * Each pack carries its own asset-URL resolver so a pack whose assets live in a different mixin (e.g.
 * the {@code "calendar"} pack served by {@code WidgetsMixin}) resolves through that mixin's own
 * cache-busting helper rather than {@link ViewsMixin#viewAssetUrl(RestRequest, String)}.
 *
 * @since 10.0.0
 */
public final class ToolkitPackRegistry {

	/** The built-in first-party pack: the page-cards / views runtime. */
	public static final String PACK_VIEWS = "views";

	/**
	 * The built-in {@code "calendar"} pack (Q8b A): the already-shipping {@code juneau-calendar.*} runtime from
	 * {@code juneau-rest-server-widgets}. Additive &mdash; never inferred from {@code type="calendar"} (I3); a
	 * calendar page must list it explicitly (e.g. {@code toolkit="views,calendar"}).
	 */
	public static final String PACK_CALENDAR = "calendar";

	/** Resolves an asset path to an absolute, cache-busted URL against the in-flight request. */
	@FunctionalInterface
	public interface AssetUrlResolver {
		/**
		 * @param req The in-flight request.
		 * @param path The asset path constant.
		 * @return The resolved, cache-busted URL.
		 */
		String resolve(RestRequest req, String path);
	}

	/** The default resolver: {@link ViewsMixin#viewAssetUrl(RestRequest, String)}. */
	public static final AssetUrlResolver VIEWS_RESOLVER = ViewsMixin::viewAssetUrl;

	/** The {@code "calendar"} pack resolver: {@link WidgetsMixin#widgetAssetUrl(RestRequest, String)}. */
	public static final AssetUrlResolver WIDGETS_RESOLVER = WidgetsMixin::widgetAssetUrl;

	/** Resolved absolute URLs for a set of requested pack names. */
	public record Resolved(List<String> cssUrls, List<String> jsUrls) {}

	private final Map<String, Pack> packs = new LinkedHashMap<>();

	/** Constructs the registry with the built-in {@code "views"} pack installed. */
	public ToolkitPackRegistry() {
		register(PACK_VIEWS,
			List.of(ViewsMixin.VIEWS_CSS_PATH, ViewsMixin.CONFIG_CSS_PATH),
			List.of(
				ViewsMixin.RENDERS_JS_PATH,
				ViewsMixin.ICONS_JS_PATH,
				ViewsMixin.RIBBON_JS_PATH,
				ViewsMixin.VIEWS_JS_PATH,
				ViewsMixin.CONFIG_JS_PATH,
				ViewsMixin.REGIONS_JS_PATH,
				ViewsMixin.HELPERS_JS_PATH,
				ViewsMixin.PAGE_CARDS_JS_PATH
			));
		// The "calendar" pack ships from juneau-rest-server-widgets and resolves through that mixin's own
		// cache-busting helper, not VIEWS_RESOLVER.  A page that wants it lists toolkit="views,calendar" so
		// juneau-views.js still loads before juneau-calendar.js (shared layer stack).
		register(PACK_CALENDAR,
			List.of(WidgetsMixin.CALENDAR_CSS_PATH),
			List.of(WidgetsMixin.CALENDAR_JS_PATH),
			WIDGETS_RESOLVER);
	}

	/**
	 * Registers a pack resolved through the default {@link #VIEWS_RESOLVER}.
	 *
	 * @param name The pack name.
	 * @param cssPaths Ordered CSS asset paths.
	 * @param jsPaths Ordered JS asset paths.
	 */
	public synchronized void register(String name, List<String> cssPaths, List<String> jsPaths) {
		register(name, cssPaths, jsPaths, VIEWS_RESOLVER);
	}

	/**
	 * Registers a pack with its own asset-URL resolver.
	 *
	 * @param name The pack name.
	 * @param cssPaths Ordered CSS asset paths.
	 * @param jsPaths Ordered JS asset paths.
	 * @param resolver The per-pack asset-URL resolver.
	 */
	public synchronized void register(String name, List<String> cssPaths, List<String> jsPaths, AssetUrlResolver resolver) {
		packs.put(name, new Pack(List.copyOf(cssPaths), List.copyOf(jsPaths), resolver));
	}

	/**
	 * Resolves the given ordered pack names to absolute, cache-busted CSS + JS URLs.
	 *
	 * @param names The requested pack names (as authored). Empty = no pack.
	 * @param req The in-flight request.
	 * @return The resolved URLs.
	 */
	public Resolved resolve(List<String> names, RestRequest req) {
		if (names == null || names.isEmpty())
			return new Resolved(List.of(), List.of());
		if (req == null)
			throw new IllegalStateException("<@page toolkit> needs FreemarkerRenderScope.request() (renderer wrap).");
		var css = new ArrayList<String>();
		var js = new ArrayList<String>();
		for (var name : names) {
			var pack = packs.get(name);
			if (pack == null)
				throw new IllegalArgumentException("Unknown toolkit pack '" + name + "'.");
			for (var path : pack.cssPaths)
				css.add(pack.resolver.resolve(req, path));
			for (var path : pack.jsPaths)
				js.add(pack.resolver.resolve(req, path));
		}
		return new Resolved(List.copyOf(css), List.copyOf(js));
	}

	private record Pack(List<String> cssPaths, List<String> jsPaths, AssetUrlResolver resolver) {}
}
