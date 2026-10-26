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
package org.apache.juneau.console.freemarker;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.console.freemarker.ToolkitPack.Kind.*;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.logging.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.console.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.terminal.region.*;
import org.apache.juneau.console.views.*;
import org.apache.juneau.console.widgets.*;

/**
 * Registry of named toolkit packs for the {@code <@page toolkit=...>} directive and for the packs cards require.
 *
 * <p>
 * Built-in packs:
 * <ul>
 * 	<li>{@code "jquery"}, {@code "datatables"} and {@code "datatables-buttons"} ({@link ToolkitPack.Kind#VENDOR}) &mdash;
 * 		WebJars, resolved through {@link #WEBJAR_RESOLVER} to the host's {@link WebJarsMixin} mount.
 * 		{@code "datatables"} depends on {@code "jquery"}; {@code "datatables-buttons"} on {@code "datatables"}.
 * 	<li>{@code "datatables-glue"} ({@link ToolkitPack.Kind#VENDOR}) &mdash; the first-party glue
 * 		({@link ViewsMixin#DATATABLES_JS_PATH}, served by {@code DataTablesMixin}); depends on {@code "datatables"}.
 * 		Every {@code <@card type="datatables">} requires it.
 * 	<li>{@code "views"} ({@link ToolkitPack.Kind#RUNTIME}) &mdash; the views runtime.  JS order is a contract: bus (first), renders,
 * 		icons, search, pagestate, urlstate, ribbon, views, config, regions, terminal-view, helpers.
 * 		{@code juneau-urlstate.js} must precede {@code juneau-views.js} / {@code juneau-config.js}.
 * 	<li>{@code "calendar"} ({@link ToolkitPack.Kind#RUNTIME}) &mdash; the {@code juneau-calendar.*} runtime from
 * 		{@code juneau-console-widgets}, resolved through {@link #WIDGETS_RESOLVER}.
 * 	<li>{@code "xterm"} ({@link ToolkitPack.Kind#VENDOR}) &mdash; the xterm.js WebJar (library and stylesheet), resolved
 * 		through {@link #WEBJAR_RESOLVER}.
 * 	<li>{@code "terminal"} ({@link ToolkitPack.Kind#RUNTIME}) &mdash; the {@code juneau-terminal.*} runtime from
 * 		{@code juneau-terminal-region}, resolved through {@link TerminalMixin#terminalAssetUrl(RestRequest, String)};
 * 		depends on {@code "views"} and {@code "xterm"}.  Every {@code <@card type="terminal">} requires it, so a page
 * 		with a terminal card needs no {@code toolkit=}.
 * 	<li>{@code "charts"} ({@link ToolkitPack.Kind#RUNTIME}) &mdash; {@code juneau-charts.js} from
 * 		{@code juneau-console}, resolved through {@link ConsoleChromeMixin#consoleAssetUrl(RestRequest, String)};
 * 		depends on {@code "echarts"} and {@code "data"}.  Every {@code <@card type="chart">} requires it.
 * 	<li>{@code "joblog"} ({@link ToolkitPack.Kind#RUNTIME}) &mdash; {@code juneau-joblog.js} from
 * 		{@code juneau-console}; no dependencies.  Every {@code <@card type="job-log">} requires it.
 * 	<li>{@code "echarts"} ({@link ToolkitPack.Kind#VENDOR}) &mdash; the ECharts WebJar ({@link #ECHARTS_JS_ASSET}),
 * 		optional on the classpath: a missing jar is E-70 at startup when the app opts in
 * 		({@code ConsoleFreemarkerMixin.Builder.requirePacks}), else at the first chart page.
 * 	<li>{@code "echarts-gl"} ({@link ToolkitPack.Kind#VENDOR}) &mdash; the echarts-gl WebJar; depends on {@code "echarts"};
 * 		a page opts in with {@code toolkit("echarts-gl")}.
 * 	<li>{@code "data"} ({@link ToolkitPack.Kind#RUNTIME}) &mdash; {@code juneau-data.js} ({@code JuneauViews.data}); depends on
 * 		{@code "views"}.
 * </ul>
 *
 * <p>
 * {@link #resolve(List, RestRequest, List, String)} expands the requested packs depth-first over
 * {@link ToolkitPack#dependsOn()} (each pack after its dependencies, siblings in request order, each pack once), skips
 * {@linkplain #provide(Collection) provided} packs (their dependencies still load), skips an asset whose file name the
 * page already loads through {@code init=} or {@code css=} (with a WARN), and splits the result by kind.
 * {@link #validate()} checks the static graph: unknown {@code dependsOn} names, cycles, and a VENDOR pack depending on
 * a RUNTIME pack.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Repeated literals are contract wire keys; named constants would obscure the contract shape.
})
public final class ToolkitPackRegistry {

	private static final Logger LOG = Logger.getLogger(ToolkitPackRegistry.class.getName());

	/** The built-in views runtime pack. */
	public static final String PACK_VIEWS = "views";

	/**
	 * The built-in {@code "calendar"} pack: the already-shipping {@code juneau-calendar.*} runtime from
	 * {@code juneau-console-widgets}.  Never inferred from {@code type="calendar"}; a calendar page lists it
	 * explicitly (e.g. {@code toolkit="views,calendar"}).
	 */
	public static final String PACK_CALENDAR = "calendar";

	/** The built-in jQuery WebJar pack. */
	public static final String PACK_JQUERY = "jquery";

	/** The built-in DataTables WebJar pack (library plus default styling CSS). */
	public static final String PACK_DATATABLES = "datatables";

	/** The built-in pack holding the first-party DataTables glue. */
	public static final String PACK_DATATABLES_GLUE = "datatables-glue";

	/** The built-in DataTables Buttons WebJar pack (copy and CSV export, no JSZip or pdfmake). */
	public static final String PACK_DATATABLES_BUTTONS = "datatables-buttons";

	/** The built-in xterm.js WebJar pack (library plus stylesheet). */
	public static final String PACK_XTERM = "xterm";

	/** The built-in terminal runtime pack; depends on {@code "views"} and {@code "xterm"}. */
	public static final String PACK_TERMINAL = "terminal";

	/** The built-in chart-card runtime pack ({@code juneau-charts.js}); depends on {@code "echarts"} and {@code "data"}. */
	public static final String PACK_CHARTS = "charts";

	/** The built-in job-log card runtime pack ({@code juneau-joblog.js}); no dependencies. */
	public static final String PACK_JOBLOG = "joblog";

	/** Dashboard chrome ({@code PageSpec.dashboard}); added by the builder, never needed in {@code toolkit(...)}. */
	public static final String PACK_DASHBOARD = "dashboard";

	/** The built-in ECharts WebJar pack ({@code org.webjars.npm:echarts}, optional on the classpath). */
	public static final String PACK_ECHARTS = "echarts";

	/** The built-in echarts-gl WebJar pack; depends on {@code "echarts"}; never required by a card type. */
	public static final String PACK_ECHARTS_GL = "echarts-gl";

	/** The built-in DataRegistry runtime pack ({@code juneau-data.js}); depends on {@code "views"}. */
	public static final String PACK_DATA = "data";

	private static final String NPM = "org.webjars.npm";

	/** The ECharts library asset id, for {@link #WEBJAR_RESOLVER}. */
	public static final String ECHARTS_JS_ASSET = WebJarResolver.asset(NPM, "echarts", "echarts/{version}/dist/echarts.min.js");

	/** The echarts-gl library asset id (path unverified against the WebJar; no module depends on it). */
	public static final String ECHARTS_GL_JS_ASSET = WebJarResolver.asset(NPM, "echarts-gl", "echarts-gl/{version}/dist/echarts-gl.min.js");

	static final String E70 = "chart cards need the ECharts WebJar org.webjars.npm:echarts, which is not on the classpath; add it to the application's pom (version property 'webjars-echarts.version')";
	static final String E70_PACK = "toolkit pack '%s' needs the WebJar %s, which is not on the classpath; add it to the application's pom (version property '%s')";

	// groupId:artifactId -> the version property the message names.
	// echarts is not listed: it has its own message (E70) in webJarMissing.
	private static final Map<String,String> WEBJAR_PROPERTIES = Map.of(
		NPM + ":echarts-gl", "webjars-echarts-gl.version");

	/** Resolves an asset path to an absolute, cache-busted URL against the in-flight request. */
	@FunctionalInterface
	public interface AssetUrlResolver {
		/**
		 * @param req The in-flight request.
		 * @param path The asset path.
		 * @return The resolved, cache-busted URL.
		 */
		String resolve(RestRequest req, String path);
	}

	/** The default resolver: {@link ViewsMixin#viewAssetUrl(RestRequest, String)}. */
	public static final AssetUrlResolver VIEWS_RESOLVER = ViewsMixin::viewAssetUrl;

	/** The {@code "calendar"} pack resolver: {@link WidgetsMixin#widgetAssetUrl(RestRequest, String)}. */
	public static final AssetUrlResolver WIDGETS_RESOLVER = WidgetsMixin::widgetAssetUrl;

	/** The WebJar resolver: paths are {@link WebJarResolver#asset(String, String, String)} ids. */
	public static final AssetUrlResolver WEBJAR_RESOLVER = WebJarResolver::resolve;

	/**
	 * Resolved absolute URLs, split by pack kind.
	 *
	 * @param vendorCss VENDOR CSS, emitted before the page {@code css=}.
	 * @param vendorJs VENDOR JS, emitted before the runtime JS.
	 * @param runtimeCss RUNTIME CSS, emitted after the page {@code css=}.
	 * @param runtimeJs RUNTIME JS, emitted before the {@code after-toolkit} slot.
	 */
	public record Resolved(List<String> vendorCss, List<String> vendorJs, List<String> runtimeCss, List<String> runtimeJs) {
		/** Nothing to load. */
		public static final Resolved EMPTY = new Resolved(List.of(), List.of(), List.of(), List.of());
	}

	private final Map<String,ToolkitPack> packs = new LinkedHashMap<>();
	private final Set<String> provided = new LinkedHashSet<>();

	/** Constructs the registry with the built-in packs installed. */
	public ToolkitPackRegistry() {
		register(ToolkitPack.create(PACK_JQUERY).kind(VENDOR).resolver(WEBJAR_RESOLVER)
			.js(WebJarResolver.asset("org.webjars", "jquery", "jquery/{version}/jquery.min.js"))
			.build());
		register(ToolkitPack.create(PACK_DATATABLES).kind(VENDOR).resolver(WEBJAR_RESOLVER)
			.css(WebJarResolver.asset(NPM, "datatables.net-dt", "datatables.net-dt/{version}/css/dataTables.dataTables.min.css"))
			.js(WebJarResolver.asset(NPM, "datatables.net", "datatables.net/{version}/js/dataTables.min.js"))
			.dependsOn(PACK_JQUERY)
			.build());
		register(ToolkitPack.create(PACK_DATATABLES_GLUE).kind(VENDOR).resolver(VIEWS_RESOLVER)
			.js(ViewsMixin.DATATABLES_JS_PATH)
			.dependsOn(PACK_DATATABLES)
			.build());
		register(ToolkitPack.create(PACK_DATATABLES_BUTTONS).kind(VENDOR).resolver(WEBJAR_RESOLVER)
			.css(WebJarResolver.asset(NPM, "datatables.net-buttons-dt", "datatables.net-buttons-dt/{version}/css/buttons.dataTables.min.css"))
			.js(
				WebJarResolver.asset(NPM, "datatables.net-buttons", "datatables.net-buttons/{version}/js/dataTables.buttons.min.js"),
				WebJarResolver.asset(NPM, "datatables.net-buttons", "datatables.net-buttons/{version}/js/buttons.html5.min.js"))
			.dependsOn(PACK_DATATABLES)
			.build());
		register(ToolkitPack.create(PACK_VIEWS).kind(RUNTIME).resolver(VIEWS_RESOLVER)
			.css(ViewsMixin.VIEWS_CSS_PATH, ViewsMixin.CONFIG_CSS_PATH)
			.js(
				// Dependency-free and read by views, regions and the console shell: it must load first (E-JS-46).
				ViewsMixin.BUS_JS_PATH,
				ViewsMixin.RENDERS_JS_PATH,
				ViewsMixin.ICONS_JS_PATH,
				ViewsMixin.SEARCH_JS_PATH,
				ViewsMixin.PAGESTATE_JS_PATH,
				ViewsMixin.URLSTATE_JS_PATH,
				ViewsMixin.RIBBON_JS_PATH,
				ViewsMixin.VIEWS_JS_PATH,
				ViewsMixin.CONFIG_JS_PATH,
				ViewsMixin.REGIONS_JS_PATH,
				ViewsMixin.RUN_VIEW_JS_PATH,
				ViewsMixin.HELPERS_JS_PATH)
			.build());
		// The "calendar" pack ships from juneau-console-widgets and resolves through that mixin's own
		// cache-busting helper, not VIEWS_RESOLVER.  A page that wants it lists toolkit="views,calendar" so
		// juneau-views.js still loads before juneau-calendar.js (shared layer stack).
		register(ToolkitPack.create(PACK_CALENDAR).kind(RUNTIME).resolver(WIDGETS_RESOLVER)
			.css(WidgetsMixin.CALENDAR_CSS_PATH)
			.js(WidgetsMixin.CALENDAR_JS_PATH)
			.build());
		register(ToolkitPack.create(PACK_XTERM).kind(VENDOR).resolver(WEBJAR_RESOLVER)
			.css(TerminalMixin.XTERM_CSS_ASSET)
			.js(TerminalMixin.XTERM_JS_ASSET)
			.build());
		register(ToolkitPack.create(PACK_TERMINAL).kind(RUNTIME).resolver(TerminalMixin::terminalAssetUrl)
			.css(TerminalMixin.CSS_PATH)
			.js(TerminalMixin.JS_PATH)
			.dependsOn(PACK_VIEWS, PACK_XTERM)
			.build());
		register(ToolkitPack.create(PACK_ECHARTS).kind(VENDOR).resolver(WEBJAR_RESOLVER)
			.js(ECHARTS_JS_ASSET)
			.build());
		register(ToolkitPack.create(PACK_ECHARTS_GL).kind(VENDOR).resolver(WEBJAR_RESOLVER)
			.js(ECHARTS_GL_JS_ASSET)
			.dependsOn(PACK_ECHARTS)
			.build());
		register(ToolkitPack.create(PACK_DATA).kind(RUNTIME).resolver(VIEWS_RESOLVER)
			.js(ViewsMixin.DATA_JS_PATH)
			.dependsOn(PACK_VIEWS)
			.build());
		register(ToolkitPack.create(PACK_CHARTS).kind(RUNTIME).resolver(ConsoleChromeMixin::consoleAssetUrl)
			.js(ConsoleChromeMixin.CHARTS_JS_PATH)
			.dependsOn(PACK_ECHARTS, PACK_DATA)
			.build());
		register(ToolkitPack.create(PACK_JOBLOG).kind(RUNTIME).resolver(ConsoleChromeMixin::consoleAssetUrl)
			.js(ConsoleChromeMixin.JOBLOG_JS_PATH)
			.build());
		register(ToolkitPack.create(PACK_DASHBOARD).kind(RUNTIME).resolver(ConsoleChromeMixin::consoleAssetUrl)
			.js(ConsoleChromeMixin.DASHBOARD_JS_PATH)
			.dependsOn(PACK_VIEWS, PACK_DATA)
			.build());
	}

	/**
	 * Registers a pack, replacing any pack with the same name (all of its fields, including {@code dependsOn}).
	 *
	 * @param pack The pack.
	 */
	public synchronized void register(ToolkitPack pack) {
		packs.put(pack.name(), pack);
	}

	/**
	 * Marks packs as already loaded by the app: they contribute no URLs, but their dependencies still load.
	 *
	 * @param names The pack names.
	 * @return This object.
	 */
	public synchronized ToolkitPackRegistry provide(Collection<String> names) {
		provided.addAll(names);
		return this;
	}

	/**
	 * @param name A pack name.
	 * @return <jk>true</jk> if a pack with that name is registered.
	 */
	public synchronized boolean contains(String name) {
		return packs.containsKey(name);
	}

	/**
	 * Checks the static graph: every {@code dependsOn} and provided name is registered, there is no cycle, and no
	 * VENDOR pack depends on a RUNTIME pack.
	 *
	 * @return This object.
	 * @throws IllegalArgumentException On the first problem found.
	 */
	public synchronized ToolkitPackRegistry validate() {
		for (var pack : packs.values()) {
			for (var dep : pack.dependsOn()) {
				var target = packs.get(dep);
				if (n(target))
					throw iaex("Unknown toolkit pack '%s' (dependsOn of pack '%s').", dep, pack.name());
				if (pack.kind() == VENDOR && target.kind() == RUNTIME)
					throw iaex("VENDOR toolkit pack '%s' cannot depend on RUNTIME pack '%s'.", pack.name(), dep);
			}
		}
		for (var name : provided)
			if (! packs.containsKey(name))
				throw iaex("Unknown toolkit pack '%s' (providedPacks).", name);
		expand(packs.keySet(), "registered");
		return this;
	}

	/**
	 * Checks that every name is a registered pack.
	 *
	 * @param names The pack names.
	 * @param source Where the names came from, for the message (e.g. {@code <@page toolkit=>}).
	 * @throws IllegalArgumentException If a name is unknown.
	 */
	public synchronized void requireKnown(Collection<String> names, String source) {
		for (var name : names)
			if (! packs.containsKey(name))
				throw iaex("Unknown toolkit pack '%s' (%s).", name, source);
	}

	/**
	 * @param root A registered pack name.
	 * @param target A pack name.
	 * @return <jk>true</jk> if {@code target} is {@code root} or one of its transitive {@code dependsOn}.
	 */
	public synchronized boolean reaches(String root, String target) {
		return expand(List.of(root), "reaches").stream().anyMatch(p -> p.name().equals(target));
	}

	/**
	 * Probes, at startup, every WebJar the given packs reach: each non-provided VENDOR pack resolved through
	 * {@link #WEBJAR_RESOLVER}.
	 *
	 * @param roots Root pack names; empty probes nothing.
	 * @return This object.
	 * @throws IllegalStateException E-70 when a WebJar or its file is not on the classpath.
	 */
	public ToolkitPackRegistry checkWebJars(Collection<String> roots) {
		return checkWebJars(roots, WebJarResolver::resolvePath);
	}

	// Package-private: the probe is injectable because the echarts jar is on every test classpath.
	synchronized ToolkitPackRegistry checkWebJars(Collection<String> roots, UnaryOperator<String> probe) {
		for (var pack : expand(new LinkedHashSet<>(roots), "requirePacks")) {
			if (provided.contains(pack.name()) || pack.kind() != VENDOR || pack.resolver() != WEBJAR_RESOLVER)
				continue;
			for (var asset : concat(pack.cssPaths(), pack.jsPaths())) {
				try {
					probe.apply(asset);
				} catch (IllegalStateException e) {
					var x = webJarMissing(pack, asset, e);
					throw x != null ? x : new IllegalStateException("Pack '" + pack.name() + "' " + e.getMessage(), e);
				}
			}
		}
		return this;
	}

	private static List<String> concat(List<String> a, List<String> b) {
		var out = new ArrayList<String>(a);
		out.addAll(b);
		return out;
	}

	// Distinguishes "jar absent" from "jar present, file missing" (the resolver's own message says which).
	private static String causeSuffix(RuntimeException cause) {
		return nn(cause.getMessage()) ? " [" + cause.getMessage() + "]" : "";
	}

	// E-70 for a WebJar the message table knows; null when the asset is not a known WebJar.
	private static IllegalStateException webJarMissing(ToolkitPack pack, String asset, RuntimeException cause) {
		var parts = asset.split(":", 3);
		var coords = parts.length == 3 ? parts[0] + ":" + parts[1] : asset;
		if (eq(coords, NPM + ":echarts"))
			return new IllegalStateException("E-70: " + E70 + causeSuffix(cause), cause);
		var prop = WEBJAR_PROPERTIES.get(coords);
		if (prop == null)
			return null;
		return new IllegalStateException("E-70: " + f(E70_PACK, pack.name(), coords, prop) + causeSuffix(cause), cause);
	}

	/**
	 * Resolves the root packs and their dependencies to absolute URLs, split by kind.
	 *
	 * @param names The root pack names, in order.  Empty or <jk>null</jk> = nothing (no request needed).
	 * @param req The in-flight request.
	 * @param pageUrls The page's own {@code init=} and {@code css=} URLs; a pack asset with the same file name is
	 * 	skipped with a WARN.
	 * @param template The page template name, for the WARN.
	 * @return The resolved URLs.
	 * @throws IllegalArgumentException On an unknown pack or a cycle.
	 * @throws IllegalStateException If the request is missing or a resolver fails (e.g. a WebJar isn't on the classpath).
	 */
	public Resolved resolve(List<String> names, RestRequest req, List<String> pageUrls, String template) {
		if (n(names) || names.isEmpty())
			return Resolved.EMPTY;
		if (n(req))
			throw isex("<@page toolkit> needs FreemarkerRenderScope.request() (renderer wrap).");
		List<ToolkitPack> order;
		Set<String> skip;
		synchronized (this) {
			order = expand(new LinkedHashSet<>(names), "requested");
			skip = Set.copyOf(provided);
		}
		var pageFiles = new LinkedHashMap<String,String>();
		for (var url : pageUrls)
			pageFiles.putIfAbsent(fileName(url), url);
		var vendorCss = new ArrayList<String>();
		var vendorJs = new ArrayList<String>();
		var runtimeCss = new ArrayList<String>();
		var runtimeJs = new ArrayList<String>();
		for (var pack : order) {
			if (skip.contains(pack.name()))
				continue;
			var vendor = pack.kind() == VENDOR;
			addUrls(pack, pack.cssPaths(), vendor ? vendorCss : runtimeCss, req, pageFiles, template);
			addUrls(pack, pack.jsPaths(), vendor ? vendorJs : runtimeJs, req, pageFiles, template);
		}
		return new Resolved(List.copyOf(vendorCss), List.copyOf(vendorJs), List.copyOf(runtimeCss), List.copyOf(runtimeJs));
	}

	/**
	 * The file name of a URL: the last path segment, without query or fragment, lower-cased.
	 *
	 * @param url The URL.
	 * @return The file name.
	 */
	static String fileName(String url) {
		var s = url;
		var q = s.indexOf('?');
		if (q >= 0)
			s = s.substring(0, q);
		var h = s.indexOf('#');
		if (h >= 0)
			s = s.substring(0, h);
		return s.substring(s.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
	}

	private static void addUrls(ToolkitPack pack, List<String> paths, List<String> out, RestRequest req, Map<String,String> pageFiles, String template) {
		for (var path : paths) {
			String url;
			try {
				url = pack.resolver().resolve(req, path);
			} catch (IllegalStateException | IllegalArgumentException e) {
				var x = e instanceof IllegalStateException ? webJarMissing(pack, path, e) : null;
				throw x != null ? x : new IllegalStateException("Pack '" + pack.name() + "' " + e.getMessage(), e);
			}
			var dup = pageFiles.get(fileName(url));
			if (nn(dup)) {
				LOG.log(Level.WARNING, () -> f("Template '%s': skipped pack '%s' asset %s; the page already loads %s.", template, pack.name(), url, dup));
				continue;
			}
			out.add(url);
		}
	}

	// Caller holds the monitor.
	private List<ToolkitPack> expand(Collection<String> roots, String source) {
		var out = new LinkedHashMap<String,ToolkitPack>();
		for (var name : roots)
			visit(name, source, new ArrayList<>(), out);
		return List.copyOf(out.values());
	}

	private void visit(String name, String source, List<String> path, Map<String,ToolkitPack> out) {
		if (out.containsKey(name))
			return;
		var at = path.indexOf(name);
		if (at >= 0) {
			var cycle = tl(path.subList(at, path.size()));
			cycle.add(name);
			throw new IllegalArgumentException("Toolkit pack cycle: " + String.join(" → ", cycle) + ".");
		}
		var pack = packs.get(name);
		if (n(pack))
			throw iaex("Unknown toolkit pack '%s' (%s).", name, source);
		path.add(name);
		for (var dep : pack.dependsOn())
			visit(dep, "dependsOn of pack '" + name + "'", path, out);
		path.remove(path.size() - 1);
		out.put(name, pack);
	}
}
