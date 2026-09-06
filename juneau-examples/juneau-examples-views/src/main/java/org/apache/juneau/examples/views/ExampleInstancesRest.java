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
package org.apache.juneau.examples.views;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.juneau.commons.utils.CollectionUtils.list;

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.marshaller.Html;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.DataTablesMixin;
import org.apache.juneau.rest.server.servlet.BasicRestServlet;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.views.ViewDef.Dir;
import org.apache.juneau.rest.server.views.*;

/**
 * Design <b>§11.1a</b> stood up as a live page: <b>ten tabs, and the server knows about none of them.</b>
 *
 * <p>
 * This is the affordability proof the whole region design rests on. §11.1 shows a single-pane detail in eight lines;
 * the question a reviewer should actually ask is what happens to a <i>big</i> panel &mdash; ten tabs, three different
 * payload shapes, per-tab lazy loading &mdash; now that the framework draws none of it. If that case is not short,
 * the convenience layer bought nothing.
 *
 * <p>
 * <b>What is absent from {@link #instancesView()} is the entire point.</b> No {@code .sections(...)}, no
 * {@link DetailField}, no {@link DetailSection}, no {@link ActionBar}, and no tab declared anywhere in Java &mdash;
 * yet the rendered panel has ten working tabs. The per-tab cost is <b>one line of JavaScript and zero lines of
 * Java</b>, because pane <i>shape</i> (grid / map / list) picks the recipe rather than tab <i>identity</i>. Adding an
 * eleventh tab is one more line.
 *
 * <h5 class='section'>The properties this page makes observable rather than merely claimed</h5>
 * <ul>
 * 	<li><b>One request per expand.</b> The eager Details tab reads {@code ctx.data ?? ctx.fetchDeclared()}, which
 * 		joins the panel's own in-flight expand envelope instead of issuing a second GET, so a ten-tab expand costs
 * 		<b>one</b> HTTP request until the user opens a second tab.
 * 	<li><b>Fetch-on-first-show needs no hook and no attribute.</b> {@code lazy} defaults to {@code true} whenever a
 * 		{@code populate} is given, so nine of the ten tabs defer with zero author code; Details opts out explicitly.
 * 	<li><b>Tab switching never refetches</b>, via the strip's fill-once rule.
 * 	<li><b>Cancellation is one word.</b> {@code {signal: ctx.signal}} flows into every pane, so collapsing the row
 * 		aborts every in-flight tab with no author-written {@code AbortError} guard.
 * 	<li><b>Every load resolves one of exactly two shapes</b> &mdash; a values map or an array of values maps. Nine
 * 		identical loaders is what makes that discipline checkable: one loader to read, nine tabs covered.
 * </ul>
 *
 * <h5 class='section'>Dual-hat</h5>
 * <p>
 * The transferable part of this example is the <i>shape</i> &mdash; one declared region, one strip call, two pane
 * recipes &mdash; never the vocabulary. Every tab label here is generic ("Extra Metrics", not any observed product's
 * acronym), and {@code ExampleInstancesRegion_Test} enforces that as a <b>build gate</b> rather than trusting a
 * comment, because a comment is exactly what a copy-paste defeats.
 *
 * <h5 class='section'>Two deviations from the document's snippet, both forced by the live tree</h5>
 * <ul>
 * 	<li>★ <b>{@code endpoint} is a plain path, not {@code "servlet:/instances/{id}"}.</b> The document writes the
 * 		{@code servlet:} pseudo-scheme in this and every other §11 example, but
 * 		{@link RowDetailDef#isSafeDetailEndpoint(String)} rejects any value whose first colon precedes its first
 * 		slash &mdash; treating it as a URL scheme &mdash; and {@link RegionDef}'s {@code dataUrl} validation reuses
 * 		that same predicate. The snippet therefore throws at startup exactly as written. This example uses the
 * 		module's own {@code /data/...} convention instead, which is what every other view in this module already does.
 * 	<li><b>{@code allowPopulators} is explicit</b>, because a named populator has to be opted into by the bean that
 * 		references it or startup validation refuses the name.
 * </ul>
 *
 * @since 10.0.0
 */
@Rest(path="/instances", mixins=ViewsMixin.class)
public class ExampleInstancesRest extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	private static final String MEDIA_HTML = "text/html;charset=utf-8";

	/** The one populator this example registers, allowlisted on the {@link RegionDef} that names it. */
	static final String POPULATOR = "instance-detail";

	/** The view id, also the region host's identity scope. */
	static final String VIEW_ID = "instances";

	private static final String COL_STATUS = "status";

	private static final List<ServiceInstance> INSTANCES = buildInstances();

	//------------------------------------------------------------------------------------------------------------------
	// The view.  Seven columns, one region, nothing else.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * The §11.1a view: a datatable whose row expander is <b>one region</b> and nothing else.
	 *
	 * @return The ten-tab region-hosted detail view.
	 */
	static ViewDef instancesView() {
		return ViewDef.create(VIEW_ID)
			.rowType(ServiceInstance.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/instances/data/instances")
			.defaultOrder("id", Dir.ASC)
			.columns(
				Column.of("id").title("ID"),
				Column.of("name").title("Name"),
				Column.of("type").title("Type"),
				Column.of("version").title("Version"),
				Column.of("zone").title("Zone"),
				Column.of(COL_STATUS).title("Status").render(Render.pill()),
				Column.of("lastSeen").title("Last Seen"))
			.details(RowDetailDef.create()
				.endpoint("/instances/data/instances/{id}")   // SF-A: the base GET, shared with the region
				.region(RegionDef.create("detail")            // ONE region.  No .sections(...)
					.allowPopulators(POPULATOR)
					.populate(POPULATOR)))
			.build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// The page.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * [GET /instances] &mdash; the live ten-tab detail page.
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}.
	 * @return The HTML page.
	 */
	@RestGet(path="/", summary="Design 11.1a: a ten-tab detail panel drawn entirely by one region populate")
	public HttpResource index(RestRequest req) {
		var tableMarkup = Html.of(ViewTable.of(req, instancesView()));
		var html = PAGE.formatted(
			DataTablesMixin.DATATABLES_CSS_CDN_URL,
			ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
			tableMarkup,
			DataTablesMixin.JQUERY_CDN_URL,
			DataTablesMixin.DATATABLES_JS_CDN_URL,
			ViewsMixin.viewAssetUrl(req, ViewsMixin.RENDERS_JS_PATH),
			ViewsMixin.viewAssetUrl(req, ViewsMixin.ICONS_JS_PATH),
			ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_JS_PATH),
			// The region runtime and the helpers bundle must both load AFTER juneau-views.js: regions.register and
			// every h.* name in the script below are theirs, and the populate is registered at parse time.
			ViewsMixin.viewAssetUrl(req, ViewsMixin.REGIONS_JS_PATH),
			ViewsMixin.viewAssetUrl(req, ViewsMixin.HELPERS_JS_PATH),
			POPULATOR,
			req.getContextPath() + "/instances/data/instances");
		return HttpResourceBean.of(
			ByteArrayBody.of(html.getBytes(UTF_8), MEDIA_HTML),
			list(ContentType.of(MEDIA_HTML)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Data endpoints.  One shared expand envelope, two map-shaped tabs, seven list-shaped tabs.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * [GET /instances/data/instances] &mdash; the table's rows.
	 *
	 * @return Every service instance.
	 */
	@RestGet(path="/data/instances", swagger=@OpSwagger(ignore=true))
	public List<ServiceInstance> instancesData() {
		return INSTANCES;
	}

	/**
	 * [GET /instances/data/instances/{id}] &mdash; the shared expand envelope the eager Details tab joins (SF-A).
	 *
	 * @param id The instance id.
	 * @return The contract-stamped detail envelope.
	 */
	@RestGet(path="/data/instances/{id}", swagger=@OpSwagger(ignore=true))
	public Map<String,Object> instanceDetail(@Path("id") String id) {
		var i = findInstance(id);
		var values = new LinkedHashMap<String,Object>();
		values.put("name", i.name);
		values.put("environment", i.environment);
		values.put("type", i.type);
		values.put("dbVendor", i.dbVendor);
		values.put("version", i.version);
		values.put("releaseCycle", i.releaseCycle);
		values.put(COL_STATUS, i.status);
		values.put("modified", i.modified);
		values.put("zone", i.zone);
		values.put("lastSeen", i.lastSeen);
		var out = new LinkedHashMap<String,Object>();
		out.put("contractVersion", RowDetailDef.CONTRACT_VERSION);
		out.put("values", values);
		return out;
	}

	/**
	 * [GET /instances/data/instances/{id}/metrics/{kind}] &mdash; the two <b>map</b>-shaped tab payloads.
	 *
	 * <p>
	 * A bare values map, with no envelope and no {@code items} wrapper: a map pane's {@code render} receives exactly
	 * what its loader resolves.
	 *
	 * @param id The instance id.
	 * @param kind Either {@code "core"} or {@code "extra"}.
	 * @return A values map.
	 */
	@RestGet(path="/data/instances/{id}/metrics/{kind}", swagger=@OpSwagger(ignore=true))
	public Map<String,Object> instanceMetrics(@Path("id") String id, @Path("kind") String kind) {
		var i = findInstance(id);
		var n = Math.abs(i.id.hashCode() % 40) + 5;
		var m = new LinkedHashMap<String,Object>();
		if ("core".equals(kind)) {
			m.put("Requests/sec", String.valueOf(n * 31));
			m.put("p95 Latency", n + " ms");
			m.put("Error Rate", (n % 5) + "." + (n % 9) + "%");
			m.put("CPU", (n % 80) + "%");
			m.put("Heap", (n % 70) + "%");
			return m;
		}
		if ("extra".equals(kind)) {
			m.put("Cache Hit Rate", (90 + n % 9) + "%");
			m.put("Open Connections", String.valueOf(n % 25));
			m.put("Queue Depth", String.valueOf(n % 7));
			m.put("GC Pause", (n % 12) + " ms");
			return m;
		}
		throw new NotFound("Unknown metrics kind: %s", kind);
	}

	/**
	 * [GET /instances/data/instances/{id}/{tab}] &mdash; the seven <b>list</b>-shaped tab payloads.
	 *
	 * <p>
	 * An <b>array</b> of values maps, again with no envelope. Suspensions deliberately returns an empty list for a
	 * healthy instance, so the page exercises the empty state for real alongside the painted one.
	 *
	 * @param id The instance id.
	 * @param tab The tab id.
	 * @return A list of values maps.
	 */
	@RestGet(path="/data/instances/{id}/{tab}", swagger=@OpSwagger(ignore=true))
	public List<Map<String,Object>> instanceTabList(@Path("id") String id, @Path("tab") String tab) {
		var i = findInstance(id);
		var n = Math.abs(i.id.hashCode() % 4);
		return switch (tab) {
			case "suspensions" -> "degraded".equals(i.status)
				? list(row("reason", "Capacity hold", "since", "2026-08-14T09:12:00Z", "actor", "capacity-bot"))
				: list();
			case "directives" -> list(
				row("name", "maxConnections", "value", String.valueOf(64 + n * 16)),
				row("name", "featureFlags", "value", "beta-router, fast-path"));
			case "releases" -> list(
				row("version", i.version, "applied", i.modified),
				row("version", "9.1." + (n + 3), "applied", "2026-06-1" + n + "T04:00:00Z"));
			case "org-requests" -> list(
				row("kind", "provision", "state", "complete", "created", "2026-07-0" + (n + 1) + "T11:20:00Z"),
				row("kind", "resize", "state", n % 2 == 0 ? "pending" : "complete",
					"created", "2026-08-1" + n + "T15:45:00Z"));
			case "pending-changes" -> list(
				row("change", "Rotate service credentials", "queued", "2026-09-0" + (n + 1) + "T02:00:00Z"));
			case "checks" -> list(
				row("check", "Heartbeat", "result", "healthy"),
				row("check", "Replication Lag", "result", "degraded".equals(i.status) ? "degraded" : "healthy"),
				row("check", "Disk Headroom", "result", "healthy"));
			case "audit-trail" -> list(
				row("at", i.modified, "actor", "release-bot", "what", "Upgraded to " + i.version),
				row("at", "2026-07-2" + n + "T13:05:00Z", "actor", "ops-oncall", "what", "Cleared alert backlog"));
			default -> throw new NotFound("Unknown instance tab: %s", tab);
		};
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fixtures.
	//------------------------------------------------------------------------------------------------------------------

	private static Map<String,Object> row(String...kv) {
		var m = new LinkedHashMap<String,Object>();
		for (var i = 0; i + 1 < kv.length; i += 2)
			m.put(kv[i], kv[i + 1]);
		return m;
	}

	private static ServiceInstance findInstance(String id) {
		for (var i : INSTANCES)
			if (i.id.equals(id))
				return i;
		throw new NotFound("Instance not found: %s", id);
	}

	private static List<ServiceInstance> buildInstances() {
		var zones = list("us-east", "us-west", "eu-central", "ap-south");
		var types = list("primary", "replica", "sandbox");
		var statuses = list("healthy", "healthy", "degraded", "offline");
		var vendors = list("postgres", "mysql", "oracle");
		var cycles = list("monthly", "quarterly", "continuous");
		var out = new ArrayList<ServiceInstance>();
		for (var i = 1; i <= 12; i++) {
			var id = "INST-" + i;
			out.add(new ServiceInstance(
					id,
					"svc-node-" + String.format("%02d", i),
					types.get(i % types.size()),
					"10.0." + (i % 6),
					zones.get(i % zones.size()),
					statuses.get(i % statuses.size()),
					"2026-09-%02dT%02d:15:00Z".formatted((i % 5) + 1, (6 + i) % 24))
				.detail(
					i % 3 == 0 ? "staging" : "production",
					vendors.get(i % vendors.size()),
					cycles.get(i % cycles.size()),
					"2026-08-%02dT11:00:00Z".formatted((i % 20) + 1)));
		}
		return List.copyOf(out);
	}

	//------------------------------------------------------------------------------------------------------------------
	// The page template.  Held as a constant so the Java above stays readable; the script block IS the example.
	//------------------------------------------------------------------------------------------------------------------

	private static final String PAGE = """
		<!DOCTYPE html>
		<html lang="en">
		<head>
		<meta charset="utf-8">
		<title>Apache Juneau - Ten-Tab Region Detail</title>
		<link rel="stylesheet" href="%s">
		<link rel="stylesheet" href="%s">
		<style>
		\tbody { font-family: -apple-system, Helvetica, Arial, sans-serif; margin: 2em; }
		</style>
		</head>
		<body>
		<h1>Apache Juneau &mdash; Ten Tabs, Zero Server Knowledge</h1>
		<p>Expand any row. The panel has <b>ten tabs</b>, and the server declares <b>one</b>
		<code>RegionDef</code> &mdash; no sections, no fields, no tabs in Java. <b>Details</b> paints eagerly by
		joining the row's own expand GET, so expanding costs one request rather than two; the other nine fetch on
		first activation and never refetch when you switch back. Collapsing the row aborts anything in flight.</p>
		%s
		<script src="%s"></script>
		<script src="%s"></script>
		<script src="%s"></script>
		<script src="%s"></script>
		<script src="%s"></script>
		<script src="%s"></script>
		<script src="%s"></script>
		<script>
		(function () {
		\tconst H = () => JuneauViews.helpers;

		\t// 1. Field metadata lives HERE, in JS - the same array shape fieldGrid has always taken.
		\tconst DETAIL_FIELDS = [
		\t\t{ data: "name",         label: "Name" },
		\t\t{ data: "environment",  label: "Environment" },
		\t\t{ data: "type",         label: "Type" },
		\t\t{ data: "dbVendor",     label: "DB Vendor" },
		\t\t{ data: "version",      label: "App Version" },
		\t\t{ data: "releaseCycle", label: "Release Cycle" },
		\t\t{ data: "status",       label: "Status", render: "pill" },
		\t\t{ data: "modified",     label: "Modified" },
		\t\t{ data: "zone",         label: "Zone" },
		\t\t{ data: "lastSeen",     label: "Last Seen" }
		\t];

		\tconst LIST_COLUMNS = {
		\t\tsuspensions:    [ {data:"reason", label:"Reason"}, {data:"since", label:"Since"},
		\t\t                  {data:"actor", label:"By"} ],
		\t\tdirectives:     [ {data:"name", label:"Directive"}, {data:"value", label:"Value"} ],
		\t\treleases:       [ {data:"version", label:"Version"}, {data:"applied", label:"Applied"} ],
		\t\torgRequests:    [ {data:"kind", label:"Kind"}, {data:"state", label:"State"},
		\t\t                  {data:"created", label:"Created"} ],
		\t\tpendingChanges: [ {data:"change", label:"Change"}, {data:"queued", label:"Queued"} ],
		\t\tchecks:         [ {data:"check", label:"Check"}, {data:"result", label:"Result",
		\t\t                  render:"pill"} ],
		\t\tauditTrail:     [ {data:"at", label:"When"}, {data:"actor", label:"Who"},
		\t\t                  {data:"what", label:"What"} ]
		\t};

		\tJuneauViews.regions.register("%s", function (ctx, container) {
		\t\tconst h  = H();
		\t\tconst id = encodeURIComponent(ctx.ids.rowId);   // the ONLY wiring.  No bus, no selection.

		\t\t// 2. ONE loader, reused nine times.  THE 404 ARM IS LOAD-BEARING, not defensive: dataPane's empty
		\t\t//    state is reached either by resolving empty or by rejecting with the framework's `empty` kind.
		\t\t//    An author's own fetch has no framework kind to reject with, so it says "empty" the other way -
		\t\t//    by RESOLVING null.  The obvious `r.ok ? ... : reject(...)` maps 404 to the ERROR pane, which is
		\t\t//    not what the chrome does for the same status.
		\t\tconst at = (path) => (t) =>
		\t\t\tfetch("%s/" + id + "/" + path, { signal: t.signal })
		\t\t\t\t.then(r => r.status === 404 ? null
		\t\t\t\t         : r.ok             ? r.json()
		\t\t\t\t         :                    Promise.reject(new Error("HTTP " + r.status)));

		\t\t// 3. TWO pane recipes, one per payload shape.  This is where the ten-times repetition dies.
		\t\tconst mapPane  = (path)      => h.dataPane({ load: at(path),
		\t\t                                  render: d    => h.kvTable(d) });
		\t\tconst listPane = (path, cat) => h.dataPane({ load: at(path),
		\t\t                                  render: rows => h.recordTable(cat, rows) });

		\t\t// 4. The strip.  Ten entries; the framework draws none of it.
		\t\tcontainer.replaceChildren(h.tabStrip([
		\t\t\t{ id: "details",         label: "Details",
		\t\t\t  populate: h.dataPane({
		\t\t\t      load:   () => ctx.data ?? ctx.fetchDeclared(),   // joins the panel's own expand GET
		\t\t\t      render: d  => h.fieldGrid(DETAIL_FIELDS, { values: d }) }),
		\t\t\t  lazy: false },                                       // the open tab paints eagerly
		\t\t\t{ id: "core-metrics",    label: "Core Metrics",  populate: mapPane("metrics/core") },
		\t\t\t{ id: "extra-metrics",   label: "Extra Metrics", populate: mapPane("metrics/extra") },
		\t\t\t{ id: "suspensions",     label: "Suspensions",
		\t\t\t  populate: listPane("suspensions",     LIST_COLUMNS.suspensions) },
		\t\t\t{ id: "directives",      label: "Directives",
		\t\t\t  populate: listPane("directives",      LIST_COLUMNS.directives) },
		\t\t\t{ id: "releases",        label: "Releases",
		\t\t\t  populate: listPane("releases",        LIST_COLUMNS.releases) },
		\t\t\t{ id: "org-requests",    label: "Org Requests",
		\t\t\t  populate: listPane("org-requests",    LIST_COLUMNS.orgRequests) },
		\t\t\t{ id: "pending-changes", label: "Pending Changes",
		\t\t\t  populate: listPane("pending-changes", LIST_COLUMNS.pendingChanges) },
		\t\t\t{ id: "checks",          label: "Checks",
		\t\t\t  populate: listPane("checks",          LIST_COLUMNS.checks) },
		\t\t\t{ id: "audit-trail",     label: "Audit Trail",
		\t\t\t  populate: listPane("audit-trail",     LIST_COLUMNS.auditTrail) }
		\t\t], { active: "details", signal: ctx.signal }));
		\t});
		})();
		</script>
		</body>
		</html>
		""";
}
