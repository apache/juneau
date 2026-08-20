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

package org.apache.juneau.releng.rest;

import java.util.List;
import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.http.Path;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.marshall.html.HtmlSerializer;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.converter.ProtocolQueryable;
import org.apache.juneau.rest.server.converter.QueryableSettings;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.rest.server.view.freemarker.console.ConsoleFreemarkerMixin;
import org.apache.juneau.rest.server.views.Column;
import org.apache.juneau.rest.server.views.ColumnConfig;
import org.apache.juneau.rest.server.views.RibbonAction;
import org.apache.juneau.rest.server.views.ViewDef;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.views.ViewDef.Dir;
import org.apache.juneau.rest.server.views.ViewTable;
import org.apache.juneau.rest.server.views.ViewsMixin;
import org.apache.juneau.releng.release.Release;
import org.apache.juneau.releng.release.ReleaseListService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Releases tab: server-rendered HTML page + DataTables server-side-processing data endpoint.
 *
 * <p>
 * Built on the {@code juneau-rest-server-views} rich-view toolkit: {@link #releasesView()} declares the typed
 * {@link ViewDef} (columns + renderers + ribbon), {@link #page(RestRequest)} emits its {@link ViewTable} shell as trusted markup
 * into the FreeMarker template, and {@link #data()} serves the {@code DataTablesResults} envelope via
 * {@link ProtocolQueryable} + the view's {@link ViewDef#queryableSettings() queryable settings}. The four runtime
 * assets are served by the composed {@link ViewsMixin} at this resource's mount.
 */
@Rest(path = "/releases", title = "Releases", responseProcessors = FreemarkerViewRenderer.class, mixins = ViewsMixin.class)
public class ReleaseRest extends BasicRestResource {

	/** This resource's absolute mount (RootRest {@code /rest/*} + {@code /releases}), used to resolve asset/data URLs. */
	static final String MOUNT = "/rest/releases";

	/** The rich-view toolkit's cell renderer id for a clickable/href-bearing column (see {@link Column#render(String)}). */
	static final String RENDER_LINKED = "linked";

	private final ReleaseListService service;

	public ReleaseRest(ReleaseListService service) {
		this.service = service;
	}

	// Return type stays FreemarkerMixin - FreemarkerViewRenderer does an exact-type bean lookup (see
	// ConsoleFreemarkerMixin's class Javadoc).
	@Bean
	public FreemarkerMixin freemarker() {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").templateSuffix(".ftlh").build();
	}

	/**
	 * The DataTables server-side-processing settings for {@link #data()}, sourced from the view definition so the
	 * protocol's positional column resolution stays in sync with the declared columns.
	 */
	@Bean
	public QueryableSettings queryableSettings() {
		return releasesView().queryableSettings();
	}

	/**
	 * The typed rich-view definition for the Releases tab (server-side mode): a linked Version column, {@code tag}-
	 * rendered Status/Stage pills (emitting the shared {@code .tag.<domain>.<value>} classes the app's console-ui
	 * palette themes), timestamp/date columns, and a copy/csv export + column-search + status quick-filter + refresh
	 * ribbon. Data arrives via ajax draws against {@link #data()}. Static (no instance state) so {@code AdminRest}
	 * can reuse this same declarative definition when composing the {@code Admin} tab page.
	 */
	static ViewDef releasesView() {
		return ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl(MOUNT + "/data")
			.defaultOrder("version", Dir.DESC)
			.columns(
				Column.of("version").title("Version").render(RENDER_LINKED).href(MOUNT + "/{version}/1").pinned(true),
				Column.of("rc").title("RC"),
				Column.of("status").title("Status").render("tag:status"),
				Column.of("stage").title("Stage").render("tag:stage"),
				Column.of("voteCloses").title("Vote closes").render("ts-zulu").formats("ts-zulu", "datetime", "date"),
				Column.of("released").title("Released").render("date").formats("date", "datetime", "ts-zulu"),
				Column.of("githubReleaseUrl").title("GitHub").render(RENDER_LINKED).href("{githubReleaseUrl}").orderable(false)
					.defaultVisible(false),
				Column.of("milestoneUrl").title("Milestone").render(RENDER_LINKED).href("{milestoneUrl}").orderable(false)
					.defaultVisible(false))
			.columnConfig(ColumnConfig.create())
			.ribbon(
				// "filters" clusters the column-search toggle and the dropped-only quick-filter into one
				// segmented ribbon group (visual-parity control-row layout: filter-ribbon); "export" actions are
				// clustered into their own group automatically (one action, one visual cluster - see juneau-
				// ribbon.js's buildRibbon).
				RibbonAction.columnSearchToggle().group("filters"),
				RibbonAction.option("dropped-only").title("Dropped only").column("status").value("DROPPED").persist(true)
					.symbol("filter_alt").group("filters"),
				RibbonAction.export("copy", "csv").optional("excel", "pdf"),
				RibbonAction.refresh())
			.build();
	}

	/** Human page — the rich-view table shell (emitted as trusted markup) + JSON sidecar, hydrated by the toolkit JS. */
	@RestGet("/")
	public View page(RestRequest req) {
		var markup = HtmlSerializer.DEFAULT_SIMPLE_SQ.toString(ViewTable.of(req, releasesView()));
		return ConsolePage.of("releases", req)
			.attr("viewTable", markup)
			.attr("viewsCssUrl", asset(req, ViewsMixin.VIEWS_CSS_PATH))
			.attr("configCssUrl", asset(req, ViewsMixin.CONFIG_CSS_PATH))
			.attr("rendersJsUrl", asset(req, ViewsMixin.RENDERS_JS_PATH))
			.attr("iconsJsUrl", asset(req, ViewsMixin.ICONS_JS_PATH))
			.attr("ribbonJsUrl", asset(req, ViewsMixin.RIBBON_JS_PATH))
			.attr("viewsJsUrl", asset(req, ViewsMixin.VIEWS_JS_PATH))
			.attr("configJsUrl", asset(req, ViewsMixin.CONFIG_JS_PATH));
	}

	/**
	 * Resolves a toolkit asset to an absolute, cache-busted URL for the FreeMarker head block via the
	 * request-aware {@link ViewsMixin#viewAssetUrl(RestRequest, String)}, resolved per-request against
	 * this resource's actual mount/context path rather than a hardcoded string-replace of the {@code servlet:}
	 * scheme &mdash; the FreeMarker template is rendered outside Juneau's {@code HtmlDoc} URL-resolution, which is
	 * why the URL must already be resolved before it reaches the template.
	 */
	private static String asset(RestRequest req, String path) {
		return ViewsMixin.viewAssetUrl(req, path);
	}

	/**
	 * Machine endpoint — the DataTables server-side-processing envelope ({@code {draw, recordsTotal, recordsFiltered,
	 * data}}). The method returns the row {@code List}; {@link ProtocolQueryable} parses the DataTables request, runs
	 * the shared query engine (search/sort/paginate), and wraps the page in a {@code DataTablesResults} envelope.
	 */
	@RestGet(path = "/data", converters = ProtocolQueryable.class)
	public List<Release> data() {
		return service.list();
	}

	/**
	 * Human page — a single release's detail view, linked from the Releases table's version cell.
	 * {@code rc} is the RC number (e.g. {@code 1} for RC1); it's not currently used to pick among multiple
	 * historical RCs of the same version (only one {@link Release} row exists per version today), but is
	 * part of the path so a future multi-RC history view doesn't need a URL-breaking change.
	 */
	@RestGet("/{version}/{rc}")
	public View detail(@Path("version") String version, @Path("rc") String rc, HttpServletRequest req) {
		var release = findByVersion(version);
		return ConsolePage.of("release-detail", req).attr("release", release).attr("rc", rc);
	}

	private Release findByVersion(String version) {
		return service.list().stream().filter(r -> version.equals(r.version)).findFirst()
				.orElseThrow(() -> new NotFound("No release found for %s", version));
	}
}
