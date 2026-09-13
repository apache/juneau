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

import java.time.*;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.DataTablesMixin;
import org.apache.juneau.rest.server.servlet.BasicRestServlet;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.views.ViewDef.Dir;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.Op;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Demonstrates HTML-slot pages: one {@code .juneau-page-nav} construct (Catalog children Active/Archived, sibling
 * leaf sections Audit Log and Alerts) served as full page loads, each mounting a {@link ViewDef} into an empty
 * slot via {@link ViewSlot#envelope(RestRequest, ViewDef)} and {@code JuneauViews.regions.mount({ table })}.
 *
 * <p>
 * This is the example-module caller of the HTML-slot path outside the views module's own test sources.  Pair
 * URLs are {@code /catalog/active}, {@code /catalog/archived}, {@code /audit}, and {@code /alerts}; {@code GET /}
 * is a 303 to the first pair.  Changing the selected pair is a normal {@code href}, not a {@code juneau-pages.js}
 * hash swap.
 *
 * <h5 class='section'>What this dogfoods:</h5>
 * <ul>
 * 	<li>The Catalog/Active pair additionally declares {@link ViewDef#poll(long) poll} and
 * 		{@link ViewDef#details(RowDetailDef) details}, plus a ribbon and a {@code rowClassRule}, so this one view
 * 		exercises most of the toolkit's declarative surface in one place.  Expand GET
 * 		{@code /data/widgets/active/{id}} projects owner/updatedAt/notes (the expander is the only place notes
 * 		appear).
 * 	<li>Catalog/Archived and Audit Log are deliberately PLAIN (no ribbon/poll/details), both to keep a contrasting
 * 		baseline and to show a leaf section (Audit Log) that omits the children row.
 * 	<li>The Alerts pair dogfoods {@link RowDetailDef} with a named region populator, two mutating {@link ActionRef}s
 * 		on {@link ViewDef#rowActions} (and the ack dialog form), and expand GET {@code /data/alerts/{id}}.
 * 		Nested-table seeding inside a row-detail pane is deferred (F24).
 * 	<li>Three distinct row types ({@link Widget}, {@link AuditEntry}, {@link Alert}) rather than one type reused
 * 		everywhere.
 * 	<li>Every view uses {@link DataMode#CLIENT} for simplicity (a static in-memory row list, no
 * 		{@code ProtocolQueryable}/{@code QueryableSettings} wiring) &mdash; {@code SERVER} mode is already covered
 * 		end-to-end by {@code ViewServerWiring_Test} in the views module itself, so this example does not repeat it.
 * 	<li>Each pair carries enough rows (see {@link #buildActiveWidgets()}/{@link #buildArchivedWidgets()}/
 * 		{@link #buildAuditLog()}/{@link #buildAlerts()}) that a column-sizing regression would be visibly wrong.
 * </ul>
 *
 * <p>
 * A separate {@code /dashboard} endpoint dogfoods titled author-HTML panels (not a Java card type): a static
 * summary panel server-rendered so it reads with JavaScript disabled, and a live metrics slot populated by
 * {@code JuneauViews.regions.mount} against {@code /data/cards/summary}.
 *
 * <p>
 * A third endpoint, {@code /overview}, dogfoods the {@link QuickStats} header strip together with both display-only
 * pill hosts: {@link #overviewView()} attaches a strip of a scalar tile, a meter and a segmented breakdown above the
 * table's toolbar, paints a display-only status pill in a column, and repeats that chip as a fill-sink pill inside the
 * row-detail expander &mdash; the inert contrast case for the Alerts pair's action-bound pill.  Its tones and the
 * pills' tones come from one closed {@link StatusTone} palette.
 *
 * @since 10.0.0
 */
@Rest(mixins=ViewsMixin.class, children=ExampleInstancesRest.class)
@SuppressWarnings({
	"java:S1192", // Duplicated literals are demo protocol/JSON field names; extracting them obscures the payload.
	"java:S3400", // Demo helper returns a stable fixture value; a constant would hide that it is an operation result.
	"java:S2479" // Text-block CSS uses \t escapes for demo payloads; remaining tabs are javadoc indent.
})
public class ExampleViewsRest extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	/** Stable page identity stamped on the pair-page nav ({@code data-juneau-page}). */
	public static final String PAGE_ID = "widgets-demo";

	/**
	 * Named populators for the three in-tree row-detail regions.  Must load after {@code juneau-regions.js} and
	 * {@code juneau-helpers.js}.
	 */
	private static final String DETAIL_POPULATE_SCRIPT = """
		(function () {
			var H = function () { return JuneauViews.helpers; };
			function paintGrid(fields) {
				return function (ctx, container) {
					container.replaceChildren(H().fieldGrid(fields, { values: ctx.data || {} }));
				};
			}
			JuneauViews.regions.register("widgets-active-detail", paintGrid([
				{ data: "owner", label: "Owner" },
				{ data: "updatedAt", label: "Last updated" },
				{ data: "notes", label: "Notes" }
			]));
			JuneauViews.regions.register("alerts-detail", function (ctx, container) {
				var h = H();
				var values = function () { return ctx.data || {}; };
				container.replaceChildren(h.tabStrip([
					{ id: "overview", label: "Overview", lazy: false,
					  populate: function (c, el) {
					    el.replaceChildren(h.fieldGrid([
					      { data: "severity", label: "Severity" },
					      { data: "title", label: "Title" }
					    ], { values: values() }));
					  } },
					{ id: "context", label: "Context",
					  populate: function (c, el) {
					    el.replaceChildren(h.fieldGrid([
					      { data: "summary", label: "Summary" },
					      { data: "assignee", label: "Assignee" }
					    ], { values: values() }));
					  } }
				], { active: "overview", signal: ctx.signal }));
			});
			JuneauViews.regions.register("alert-overview-detail", paintGrid([
				{ data: "severity", label: "Severity" },
				{ data: "assignee", label: "Assignee" },
				{ data: "status", label: "Status", render: "pill", renderMeta: { field: "state" } }
			]));
		})();
		""";

	private static final String COL_UPDATED_AT = "updatedAt";
	private static final String COL_STATUS = "status";
	private static final String COL_OWNER = "owner";
	private static final String STATUS_ERROR = "error";
	private static final String VALUE_ARCHIVED = "archived";
	private static final String TITLE_OWNER = "Owner";
	private static final String STATUS_OPEN = "open";
	private static final String STATUS_ACKNOWLEDGED = "acknowledged";
	private static final String STATUS_ESCALATED = "escalated";
	private static final String COL_NAME = "name";
	private static final String TITLE_NAME = "Name";
	private static final String COL_SEVERITY = "severity";
	private static final String TITLE_SEVERITY = "Severity";
	private static final String SEVERITY_CRITICAL = "critical";
	private static final String SEVERITY_INFO = "info";
	private static final String COL_TITLE = "title";
	private static final String TITLE_TITLE = "Title";
	private static final String COL_TIMESTAMP = "timestamp";
	private static final String COL_ASSIGNEE = "assignee";
	private static final String TITLE_STATUS = "Status";
	private static final String TITLE_ARCHIVED = "Archived";
	private static final String RENDER_TAG_STATUS = "tag:status";
	private static final String RENDER_DATE = "date";
	private static final String META_FIELD = "field";
	private static final String META_STATE = "state";
	private static final String ACTION_ACK = "ack";
	private static final String ACTION_ESC = "esc";
	private static final String MEDIA_HTML = "text/html;charset=utf-8";
	// Single spelling per literal value; role-specific names below alias it so java:S1192 sees one raw token
	// per value while call sites keep role-specific names (column key vs render meta vs form input type vs view id).
	private static final String SPELLING_ACTIVE = "active";
	private static final String SUBTAB_ACTIVE = SPELLING_ACTIVE;
	private static final String STATUS_ACTIVE = SPELLING_ACTIVE;
	private static final String SPELLING_ALERTS = "alerts";
	private static final String TAB_ALERTS = SPELLING_ALERTS;
	private static final String VIEW_ALERTS = SPELLING_ALERTS;
	private static final String SPELLING_ACTION = "action";
	private static final String COL_ACTION = SPELLING_ACTION;
	private static final String META_ACTION = SPELLING_ACTION;
	private static final String INPUT_TYPE_ACTION = SPELLING_ACTION;

	private static final List<Widget> ACTIVE_WIDGETS = buildActiveWidgets();
	private static final List<Widget> ARCHIVED_WIDGETS = buildArchivedWidgets();
	private static final List<AuditEntry> AUDIT_LOG = buildAuditLog();
	private static final List<Alert> ALERTS = buildAlerts();
	private static final List<AlertEvent> ALERT_EVENTS = buildAlertEvents();

	//------------------------------------------------------------------------------------------------------------------
	// Register the $FV server-values variable on this serving-path resource's VarResolver.  This @Bean factory
	// receives the framework-built builder (so all default vars stay available) and returns a resolver that also
	// knows ServerValuesVar, letting a view's $FV{name} chrome resolve at serve time.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces the framework-built {@link VarResolver} with one that also knows {@link ServerValuesVar}.
	 *
	 * @param b The framework-built builder, pre-seeded with the default vars/functions.
	 * @return A resolver that additionally resolves <js>"$FV{name}"</js> chrome.
	 */
	@Bean
	public VarResolver varResolver(VarResolver.Builder b) {
		return b.vars(ServerValuesVar.class).build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pair pages: Catalog (Active/Archived children) + sibling leaf sections Audit Log and Alerts.
	//------------------------------------------------------------------------------------------------------------------

	/** One section/child pair.  Leaf sections leave {@code child} blank and omit the children row. */
	private enum DemoPair {
		CATALOG_ACTIVE("/catalog/active", "catalog", SUBTAB_ACTIVE),
		CATALOG_ARCHIVED("/catalog/archived", "catalog", VALUE_ARCHIVED),
		AUDIT("/audit", "audit", ""),
		ALERTS("/alerts", TAB_ALERTS, "");

		private final String path;
		private final String section;
		private final String child;

		DemoPair(String path, String section, String child) {
			this.path = path;
			this.section = section;
			this.child = child;
		}
	}

	/** The Catalog/Active pair &mdash; dogfoods poll/details/ribbon/rowClassRule together. */
	static ViewDef activeView() {
		return ViewDef.create("widgets-active")
			.rowType(Widget.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/widgets/active")
			.defaultOrder(COL_NAME, Dir.ASC)
			.columns(
				Column.of(COL_NAME).title(TITLE_NAME),
				Column.of(COL_STATUS).title(TITLE_STATUS).render(RENDER_TAG_STATUS),
				Column.of(COL_OWNER).title(TITLE_OWNER),
				Column.of(COL_UPDATED_AT).title("Updated").render(RENDER_DATE))
			.ribbon(
				RibbonAction.columnSearchToggle(),
				RibbonAction.refresh())
			.rowClassRule(COL_STATUS, Op.EQ, STATUS_ERROR, "row-flagged")
			// Use 10s for the per-table poll (well above the 5s floor) so the
			// staleness chip's "Xs ago" advance is easy to observe without hammering this demo endpoint.
			.poll(10_000L)
			// "notes" is intentionally not a table column; the expander GET is the only place it appears.
			.details(RowDetailDef.create()
				.endpoint("/data/widgets/active/{id}")
				.region(detailRegion("widgets-active-detail")))
			.build();
	}

	/** The Catalog/Archived pair &mdash; deliberately plain (the "at least one child has no bells on it" case). */
	static ViewDef archivedView() {
		return ViewDef.create("widgets-archived")
			.rowType(Widget.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/widgets/archived")
			.defaultOrder(COL_UPDATED_AT, Dir.DESC)
			.columns(
				Column.of(COL_NAME).title(TITLE_NAME),
				Column.of(COL_STATUS).title(TITLE_STATUS).render(RENDER_TAG_STATUS),
				Column.of(COL_OWNER).title(TITLE_OWNER),
				Column.of(COL_UPDATED_AT).title(TITLE_ARCHIVED).render(RENDER_DATE))
			.build();
	}

	/**
	 * A standalone view whose column title interpolates a server-side scalar value via {@code $FV}.
	 *
	 * <p>
	 * The {@code flaggedCount} provider is session-aware ({@code Function<VarResolverSession,?>}) and returns a
	 * scalar; {@link ViewSlot#envelope(RestRequest, ViewDef)} resolves the <js>"$FV{flaggedCount}"</js> chrome at
	 * serve time into plain, serializer-encoded text.  {@code $FV} is registered by {@link #varResolver(VarResolver.Builder)}.
	 */
	static ViewDef flaggedView() {
		return ViewDef.create("widgets-flagged")
			.rowType(Widget.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/widgets/active")
			.defaultOrder(COL_NAME, Dir.ASC)
			.columns(
				Column.of(COL_NAME).title(TITLE_NAME),
				Column.of(COL_STATUS).title("Status ($FV{flaggedCount} flagged)").render(RENDER_TAG_STATUS),
				Column.of(COL_OWNER).title(TITLE_OWNER))
			.serverValues(ServerValues.create()
				.value("flaggedCount", s -> ACTIVE_WIDGETS.stream().filter(w -> STATUS_ERROR.equals(w.status)).count()))
			.build();
	}

	/** The sibling PLAIN leaf section (no children row) - the contrast case the blank-panel regression needs. */
	static ViewDef auditView() {
		return ViewDef.create("audit-log")
			.rowType(AuditEntry.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/audit")
			.defaultOrder(COL_TIMESTAMP, Dir.DESC)
			.columns(
				Column.of(COL_TIMESTAMP).title("When").render(RENDER_DATE),
				Column.of("actor").title("Actor"),
				Column.of(COL_ACTION).title("Action"))
			.build();
	}

	/** Fake alerts table &mdash; named region populator, two mutating ActionRefs on rowActions, expand GET. */
	static ViewDef alertsView() {
		return ViewDef.create(VIEW_ALERTS)
			.rowType(Alert.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.defaultOrder("id", Dir.ASC)
			.columns(
				Column.of("id").title("Id"),
				Column.of(COL_SEVERITY).title(TITLE_SEVERITY).render(RENDER_TAG_STATUS),
				Column.of(COL_TITLE).title(TITLE_TITLE),
				// An action-bound status pill: the chip themes via .tag.state.<value> (generic "state" domain, not
				// an IRS probe vocabulary) and clicking (or Enter/Space on) it dispatches the "ack" RowAction through
				// the same confirm/dialog handler the row-action menu uses - proving pill dispatch is NOT gated on a
				// row-detail template (this view has both details AND rowActions).
				Column.of(COL_STATUS).title(TITLE_STATUS)
					.render(Render.pill().meta(META_FIELD, META_STATE).meta(META_ACTION, ACTION_ACK)))
			.rowActions(
				// "ack" is a present=dialog action: clicking it fetches the form envelope (ackForm below), paints a
				// typed input form, and submits to the POST endpoint on confirm.  The form carries a nested
				// type=action button targeting "esc" (modal-over-modal, h3).
				RowAction.create(ACTION_ACK).label("Acknowledge").endpoint("/data/alerts/{id}/ack")
					.method(RowAction.Method.POST).present(RowAction.Present.DIALOG)
					.form("/data/alerts/{id}/ack-form").onSuccess(RowAction.OnSuccess.REDRAW),
				// "esc" is a present=dialog CONFIRM-ONLY action (no form URL): clicking it opens a title-only
				// confirmation, and it is also the nested trigger reached from the ack form's action button.
				RowAction.create(ACTION_ESC).label("Escalate").endpoint("/data/alerts/{id}/esc")
					.method(RowAction.Method.POST).present(RowAction.Present.DIALOG)
					.confirm("Escalate this alert to on-call?").onSuccess(RowAction.OnSuccess.REDRAW))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.region(detailRegion("alerts-detail")))
			.build();
	}

	/** The read-only nested "related events" table (F24: no row-detail host until nested-table seeding is scoped). */
	static ViewDef relatedEventsView() {
		return ViewDef.create("alert-events")
			.rowType(AlertEvent.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts/events")
			.defaultOrder(COL_TIMESTAMP, Dir.ASC)
			.columns(
				Column.of(COL_TIMESTAMP).title("When").render(RENDER_DATE),
				Column.of("kind").title("Kind"),
				Column.of("detail").title("Detail"))
			.build();
	}

	private static RegionDef detailRegion(String populator) {
		return RegionDef.create("detail").allowPopulators(populator).populate(populator);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Dashboard: author-HTML titled panels (no Java Card/CardGrid type).
	//------------------------------------------------------------------------------------------------------------------

	private static String dashboardMarkup() {
		var total = ACTIVE_WIDGETS.size() + ARCHIVED_WIDGETS.size();
		return """
			<section class="juneau-view-card-grid" style="--jc-card-min: 320px">
			<h2 class="juneau-view-card-grid-title">Operations Dashboard</h2>
			<article class="juneau-view-card">
			<header class="juneau-view-card-header"><span class="juneau-view-card-title">Fleet Summary</span></header>
			<div class="juneau-view-card-body">
			<dl class="juneau-view-card-fields" style="grid-template-columns:repeat(2,minmax(0,1fr))">
			<div class="juneau-view-card-field"><dt>Total widgets</dt><dd>%d</dd></div>
			<div class="juneau-view-card-field"><dt>Active</dt><dd>%d</dd></div>
			<div class="juneau-view-card-field"><dt>%s</dt><dd>%d</dd></div>
			<div class="juneau-view-card-field"><dt>Total alerts</dt><dd>%d</dd></div>
			</dl>
			</div>
			</article>
			<article class="juneau-view-card">
			<header class="juneau-view-card-header">
			<span class="juneau-view-card-title">Live Alert Metrics</span>
			<div class="juneau-view-card-actions">
			<button type="button" class="juneau-view-card-refresh" id="live-refresh">Refresh</button>
			</div>
			</header>
			<div id="live-metrics" class="juneau-view-card-body"></div>
			</article>
			</section>
			""".formatted(total, ACTIVE_WIDGETS.size(), TITLE_ARCHIVED, ARCHIVED_WIDGETS.size(), ALERTS.size());
	}

	private static final String DASHBOARD_POPULATE_SCRIPT = """
		(function () {
			var H = function () { return JuneauViews.helpers; };
			var FIELDS = [
				{ data: "open", label: "Open" },
				{ data: "acknowledged", label: "Acknowledged" },
				{ data: "escalated", label: "Escalated" },
				{ data: "asOf", label: "As of" }
			];
			JuneauViews.regions.register("live-metrics", function (ctx, container) {
				function paint(data) {
					var values = (data && data.fields) ? data.fields : (data || {});
					container.replaceChildren(H().fieldGrid(FIELDS, { values: values }));
				}
				function load() {
					fetch("/data/cards/summary", { headers: { "Accept": "application/json" } })
						.then(function (r) { return r.ok ? r.json() : Promise.reject(new Error("HTTP " + r.status)); })
						.then(paint);
				}
				load();
				var timer = setInterval(load, 10000);
				if (ctx && ctx.signal) ctx.signal.addEventListener("abort", function () { clearInterval(timer); });
				var btn = document.getElementById("live-refresh");
				if (btn) btn.addEventListener("click", load);
			});
			JuneauViews.regions.mount({ "live-metrics": "live-metrics" });
		})();
		""";

	//------------------------------------------------------------------------------------------------------------------
	// Quick-stats strip + fill-sink pills (a second, non-card consumer of the status-tone palette).
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * A {@link QuickStats} strip for the alert table: one {@link StatTile} scalar, one {@link StatBar} meter, and one
	 * {@link SegmentedBadge} breakdown, all painted from values computed here on the server.
	 *
	 * <p>
	 * Every tone is one of the five {@link StatusTone} names, which are the same names a pill's {@code meta.tone}
	 * accepts &mdash; so "warning" is one colour across the whole toolkit rather than one per surface.  The strip is
	 * display-only: it has no refresh endpoint and no poll interval, unlike the live dashboard metrics slot
	 * above.  A figure that needs to move belongs on a dashboard panel or in a column, not in a quick-stat.
	 *
	 * @return The alert overview strip.
	 */
	static QuickStats alertQuickStats() {
		var open = 0L;
		var ack = 0L;
		var esc = 0L;
		var critical = 0L;
		for (var a : ALERTS) {
			if (STATUS_OPEN.equals(a.status)) open++;
			else if (STATUS_ACKNOWLEDGED.equals(a.status)) ack++;
			else if (STATUS_ESCALATED.equals(a.status)) esc++;
			if (SEVERITY_CRITICAL.equals(a.severity)) critical++;
		}
		return QuickStats.create("alert-overview").items(
			StatTile.of("total", "Total alerts", Long.toString(ALERTS.size()))
				.tone(StatusTone.INFO),
			// A meter reads "how much of the budget is used": critical alerts against the whole table.
			StatBar.of(SEVERITY_CRITICAL, "Critical", critical, ALERTS.size())
				.tone(critical == 0 ? StatusTone.SUCCESS : StatusTone.ERROR),
			SegmentedBadge.of("by-status", "By status").segments(
				SegmentedBadge.Segment.of(STATUS_OPEN, open).tone(StatusTone.WARNING),
				SegmentedBadge.Segment.of(STATUS_ACKNOWLEDGED, ack).tone(StatusTone.INFO),
				SegmentedBadge.Segment.of(STATUS_ESCALATED, esc).tone(StatusTone.ERROR)));
	}

	/**
	 * A read-only alert overview: a {@link QuickStats} strip above the toolbar, a display-only pill column, and a
	 * row-detail whose {@code state} field is a <b>fill-sink</b> pill.
	 *
	 * <p>
	 * The sink pill is the contrast case for the {@link #alertsView() Alerts} pair's action-bound pill: a fill sink has
	 * no {@code rowActions} in scope, so its pill is display-only by construction and carries no button role, no
	 * keyboard affordance, and no dispatch attribute.  Declaring {@code meta.action} on it would fail the view's own
	 * {@code validate()} rather than paint a dead chip.
	 *
	 * @return The overview view.
	 */
	static ViewDef overviewView() {
		return ViewDef.create("alert-overview")
			.rowType(Alert.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.defaultOrder("id", Dir.ASC)
			.quickStats(alertQuickStats())
			.columns(
				Column.of("id").title("Id"),
				Column.of(COL_SEVERITY).title(TITLE_SEVERITY).render(RENDER_TAG_STATUS),
				Column.of(COL_TITLE).title(TITLE_TITLE),
				// A display-only pill with an explicit tone from the five-value status palette.  No meta.action, so
				// no role/tabindex/dispatch attribute is emitted - the chip is presentation, and that is legal.
				Column.of(COL_STATUS).title(TITLE_STATUS)
					.render(Render.pill(StatusTone.WARNING.wire()).meta(META_FIELD, META_STATE)))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.region(detailRegion("alert-overview-detail")))
			.build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// HTML pages (hand-built, no template engine - this module takes no dependency on FreeMarker/console-ui).
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * [GET /] &mdash; 303 to the default pair {@code /catalog/active}.
	 *
	 * @return A see-other pointing at the Catalog/Active pair.
	 */
	@RestGet(path="/", summary="Redirects to the Catalog/Active pair page")
	public SeeOther index() {
		return new SeeOther().setLocation(DemoPair.CATALOG_ACTIVE.path);
	}

	/**
	 * [GET /catalog/active] &mdash; Catalog/Active pair: poll/details/ribbon table in an empty slot.
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}.
	 * @return The pair HTML page.
	 */
	@RestGet(path="/catalog/active", summary="Catalog/Active pair: poll, details, ribbon, rowClassRule")
	public HttpResource catalogActive(RestRequest req) {
		return tablePairPage(req, DemoPair.CATALOG_ACTIVE, "widgets-active");
	}

	/**
	 * [GET /catalog/active/view] &mdash; the {@link ViewSlot} envelope for {@link #activeView()}.
	 *
	 * @param req The current request, used to resolve {@code $FV} / {@code servlet:} chrome.
	 * @return The slot envelope.
	 */
	@RestGet(path="/catalog/active/view", swagger=@OpSwagger(ignore=true))
	public ViewSlot catalogActiveView(RestRequest req) {
		return ViewSlot.envelope(req, activeView());
	}

	/**
	 * [GET /catalog/archived] &mdash; Catalog/Archived pair: a plain table in an empty slot.
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}.
	 * @return The pair HTML page.
	 */
	@RestGet(path="/catalog/archived", summary="Catalog/Archived pair: a plain table")
	public HttpResource catalogArchived(RestRequest req) {
		return tablePairPage(req, DemoPair.CATALOG_ARCHIVED, "widgets-archived");
	}

	/**
	 * [GET /catalog/archived/view] &mdash; the {@link ViewSlot} envelope for {@link #archivedView()}.
	 *
	 * @param req The current request, used to resolve {@code $FV} / {@code servlet:} chrome.
	 * @return The slot envelope.
	 */
	@RestGet(path="/catalog/archived/view", swagger=@OpSwagger(ignore=true))
	public ViewSlot catalogArchivedView(RestRequest req) {
		return ViewSlot.envelope(req, archivedView());
	}

	/**
	 * [GET /audit] &mdash; Audit Log leaf section: a plain table, no children row.
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}.
	 * @return The pair HTML page.
	 */
	@RestGet(path="/audit", summary="Audit Log leaf section: a plain table")
	public HttpResource audit(RestRequest req) {
		return tablePairPage(req, DemoPair.AUDIT, "audit-log");
	}

	/**
	 * [GET /audit/view] &mdash; the {@link ViewSlot} envelope for {@link #auditView()}.
	 *
	 * @param req The current request, used to resolve {@code $FV} / {@code servlet:} chrome.
	 * @return The slot envelope.
	 */
	@RestGet(path="/audit/view", swagger=@OpSwagger(ignore=true))
	public ViewSlot auditViewEnvelope(RestRequest req) {
		return ViewSlot.envelope(req, auditView());
	}

	/**
	 * [GET /alerts] &mdash; Alerts leaf section: named region populator and mutating row actions.
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}.
	 * @return The pair HTML page.
	 */
	@RestGet(path="/alerts", summary="Alerts leaf section: region populator and mutating row actions")
	public HttpResource alerts(RestRequest req) {
		return tablePairPage(req, DemoPair.ALERTS, VIEW_ALERTS);
	}

	/**
	 * [GET /alerts/view] &mdash; the {@link ViewSlot} envelope for {@link #alertsView()}.
	 *
	 * @param req The current request, used to resolve {@code $FV} / {@code servlet:} chrome.
	 * @return The slot envelope.
	 */
	@RestGet(path="/alerts/view", swagger=@OpSwagger(ignore=true))
	public ViewSlot alertsViewEnvelope(RestRequest req) {
		return ViewSlot.envelope(req, alertsView());
	}

	/**
	 * [GET /dashboard] &mdash; author-HTML titled panels: a static Fleet Summary plus a live metrics slot
	 * populated by {@code JuneauViews.regions.mount} (no Java card type, no {@code juneau-cards.js}).
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}.
	 * @return The dashboard HTML page.
	 */
	@RestGet(path="/dashboard", summary="Author-HTML dashboard: static summary panel + live metrics slot")
	public HttpResource dashboard(RestRequest req) {
		var html = """
			<!DOCTYPE html>
			<html lang="en">
			<head>
			<meta charset="utf-8">
			<title>Apache Juneau - Card Dashboard Example</title>
			<link rel="stylesheet" href="%s">
			<style>
			\tbody { font-family: -apple-system, Helvetica, Arial, sans-serif; margin: 2em; }
			</style>
			</head>
			<body>
			<h1>Apache Juneau &mdash; Card Dashboard Example</h1>
			<p>Titled author-HTML panels (not a Java card type). The <b>Fleet Summary</b> panel is static
			(server-rendered, legible with JavaScript disabled); the <b>Live Alert Metrics</b> panel is an empty
			slot that <code>JuneauViews.regions.mount</code> populates from <code>/data/cards/summary</code>, with
			a Refresh button and a 10s poll. Acknowledge or escalate an alert on the
			<a href="/catalog/active">Catalog</a> or <a href="/alerts">Alerts</a> pair, then refresh this panel
			to see the counts move.</p>
			%s
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script>
			%s
			</script>
			</body>
			</html>
			""".formatted(
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
				dashboardMarkup(),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.REGIONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.HELPERS_JS_PATH),
				DASHBOARD_POPULATE_SCRIPT);
		return HttpResourceBean.of(
			ByteArrayBody.of(html.getBytes(UTF_8), MEDIA_HTML),
			list(ContentType.of(MEDIA_HTML)));
	}

	private static String pageNavHtml(DemoPair current) {
		var catalogCur = "catalog".equals(current.section) ? " aria-current=\"page\"" : "";
		var auditCur = "audit".equals(current.section) ? " aria-current=\"page\"" : "";
		var alertsCur = TAB_ALERTS.equals(current.section) ? " aria-current=\"page\"" : "";
		var children = "";
		if ("catalog".equals(current.section)) {
			var activeCur = SUBTAB_ACTIVE.equals(current.child) ? " aria-current=\"page\"" : "";
			var archivedCur = VALUE_ARCHIVED.equals(current.child) ? " aria-current=\"page\"" : "";
			children = """
				<div class="juneau-page-nav-children">
				<a class="juneau-page-nav-child" href="/catalog/active"%s>Active</a>
				<a class="juneau-page-nav-child" href="/catalog/archived"%s>%s</a>
				</div>
				""".formatted(activeCur, archivedCur, TITLE_ARCHIVED);
		}
		return """
			<nav class="juneau-page-nav" data-juneau-page="%s">
			<div class="juneau-page-nav-sections">
			<a class="juneau-page-nav-section" href="/catalog/active"%s>Catalog</a>
			<a class="juneau-page-nav-section" href="/audit"%s>Audit Log</a>
			<a class="juneau-page-nav-section" href="/alerts"%s>Alerts</a>
			</div>
			%s</nav>
			""".formatted(PAGE_ID, catalogCur, auditCur, alertsCur, children);
	}

	private HttpResource tablePairPage(RestRequest req, DemoPair pair, String slotId) {
		var envelopeUrl = req.getContextPath() + pair.path + "/view";
		var html = """
			<!DOCTYPE html>
			<html lang="en">
			<head>
			<meta charset="utf-8">
			<title>Apache Juneau - Rich Views Example</title>
			<link rel="stylesheet" href="%s">
			<link rel="stylesheet" href="%s">
			<style>
			\tbody { font-family: -apple-system, Helvetica, Arial, sans-serif; margin: 2em; }
			</style>
			</head>
			<body>
			<h1>Apache Juneau &mdash; Rich Views Example</h1>
			<p>HTML-slot pair pages: <b>Catalog</b> has children <b>Active</b>/<b>Archived</b>;
			<b>Audit Log</b> and <b>Alerts</b> are leaf sections (no children row). Clicks are normal
			<code>href</code>s (full page load). The Active pair also declares a poll interval (watch the
			staleness chip) and a row-details expander. Alerts dogfoods a named region populator and
			mutating row actions.</p>
			<p><a href="/catalog/archived">Open the Archived child</a> (a full page load, not a hash swap).
			See also the <a href="/dashboard">dashboard</a> and the
			<a href="/overview">QuickStats overview</a>.</p>
			%s
			<div id="%s"></div>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script>
			%s
			JuneauViews.regions.mount({ "%s": { table: "%s" } });
			</script>
			</body>
			</html>
			""".formatted(
				DataTablesMixin.DATATABLES_CSS_CDN_URL,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
				pageNavHtml(pair),
				slotId,
				DataTablesMixin.JQUERY_CDN_URL,
				DataTablesMixin.DATATABLES_JS_CDN_URL,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.RENDERS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.ICONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.RIBBON_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.REGIONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.HELPERS_JS_PATH),
				DETAIL_POPULATE_SCRIPT,
				slotId,
				envelopeUrl);
		return HttpResourceBean.of(
			ByteArrayBody.of(html.getBytes(UTF_8), MEDIA_HTML),
			list(ContentType.of(MEDIA_HTML)));
	}

	/**
	 * [GET /overview] &mdash; the {@link #overviewView() alert overview}: a {@link QuickStats} strip above a table's
	 * toolbar, a display-only status pill column, and a fill-sink pill inside the row-detail expander.
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}.
	 * @return The overview HTML page.
	 */
	@RestGet(path="/overview", summary="A QuickStats strip above a table, with display-only and fill-sink status pills")
	public HttpResource overview(RestRequest req) {
		var envelopeUrl = req.getContextPath() + "/overview/view";
		var html = """
			<!DOCTYPE html>
			<html lang="en">
			<head>
			<meta charset="utf-8">
			<title>Apache Juneau - QuickStats Example</title>
			<link rel="stylesheet" href="%s">
			<link rel="stylesheet" href="%s">
			<style>
			\tbody { font-family: -apple-system, Helvetica, Arial, sans-serif; margin: 2em; }
			</style>
			</head>
			<body>
			<h1>Apache Juneau &mdash; QuickStats Example</h1>
			<p>The strip above the table is a <code>QuickStats</code>: a scalar tile, a meter, and a segmented
			breakdown, painted from the same alert rows the table lists. It is display-only
			&mdash; no tile is clickable and nothing refreshes, unlike the
			<a href="dashboard">live card dashboard</a>.</p>
			<p>The <b>Status</b> column is a <b>display-only</b> pill with an explicit tone, and expanding a row shows
			the same chip again as a <b>fill-sink</b> pill. Neither is keyboard-actionable; contrast them with the
			action-bound pill on the <a href="/alerts">Alerts pair</a>, which dispatches a row action on click or
			Enter/Space. Tones on the strip and on the pills come from one palette:
			<code>info</code>, <code>success</code>, <code>warning</code>, <code>error</code>, <code>neutral</code>.</p>
			<div id="alert-overview"></div>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script>
			%s
			JuneauViews.regions.mount({ "alert-overview": { table: "%s" } });
			</script>
			</body>
			</html>
			""".formatted(
				DataTablesMixin.DATATABLES_CSS_CDN_URL,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
				DataTablesMixin.JQUERY_CDN_URL,
				DataTablesMixin.DATATABLES_JS_CDN_URL,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.RENDERS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.ICONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.REGIONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.HELPERS_JS_PATH),
				DETAIL_POPULATE_SCRIPT,
				envelopeUrl);
		return HttpResourceBean.of(
			ByteArrayBody.of(html.getBytes(UTF_8), MEDIA_HTML),
			list(ContentType.of(MEDIA_HTML)));
	}

	/**
	 * [GET /overview/view] &mdash; the {@link ViewSlot} envelope {@code JuneauViews.regions.mount} fetches into
	 * the overview page slot.
	 *
	 * @param req The current request, used to resolve {@code $FV} / {@code servlet:} chrome.
	 * @return The slot envelope.
	 */
	@RestGet(path="/overview/view", swagger=@OpSwagger(ignore=true))
	public ViewSlot overviewViewEnvelope(RestRequest req) {
		return ViewSlot.envelope(req, overviewView());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Region + helpers demo (WORK-J0522b): a hand-authored [data-juneau-region] container, populated by
	// juneau-regions.js (WORK-J0522a) via a JuneauViews.helpers.tabStrip (WORK-J0522b), following design
	// section 11.1a's "ten tabs, clean slate" worked example (genericized there; reproduced verbatim here).
	//------------------------------------------------------------------------------------------------------------------

	/** The one demo "instance" the region's ten tabs describe; every data endpoint below 404s on any other id. */
	static final String INSTANCE_ID = "svc-42";

	/**
	 * [GET /instance-detail] &mdash; a standalone region container (no {@code RowDetailDef} host:
	 * automatic host enrolment is WORK-J0522d, not this item) that a page-local script enrols by hand via
	 * {@code JuneauViews.regions.enrolIn}, after registering a populator that builds a ten-tab strip from
	 * {@code JuneauViews.helpers.tabStrip}/{@code dataPane}/{@code fieldGrid}/{@code kvTable}/{@code recordTable}
	 * &mdash; design &sect;11.1a's worked example, reproduced with one deliberate deviation: see
	 * {@link #instanceDetailScript()}'s Javadoc for why the "Details" tab uses the same author-owned
	 * {@code fetch} as the other nine tabs rather than the design snippet's {@code ctx.data ?? ctx.fetchDeclared()}.
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}.
	 * @return The region-demo HTML page.
	 */
	@RestGet(path="/instance-detail", summary="A region + helpers demo: a hand-enrolled tabStrip, ten tabs, one register() call")
	public HttpResource instanceDetail(RestRequest req) {
		var html = """
			<!DOCTYPE html>
			<html lang="en">
			<head>
			<meta charset="utf-8">
			<title>Apache Juneau - Region + Helpers Example</title>
			<link rel="stylesheet" href="%s">
			<style>
			\tbody { font-family: -apple-system, Helvetica, Arial, sans-serif; margin: 2em; }
			\t[data-juneau-region] { border: 1px solid #ccc; border-radius: 4px; padding: 1em; max-width: 48em; }
			</style>
			</head>
			<body>
			<h1>Apache Juneau &mdash; Region + Helpers Example</h1>
			<p>A ten-tab detail panel built with <b>zero framework-drawn DOM</b>: the page below is one
			<code>[data-juneau-region]</code> container, hand-enrolled (automatic host enrolment from a
			<code>RowDetailDef</code> is a later item, not this one), whose populator is a
			single <code>JuneauViews.regions.register(...)</code> call building a
			<code>JuneauViews.helpers.tabStrip(...)</code> from ten one-line tab entries. One tab is a declared
			field grid, two are ad-hoc value maps, and seven are record lists that fetch only when first opened
			(open more than one tab to see the lazy fetch fire). See also the <a href="/">Catalog demo</a>,
			the <a href="dashboard">card dashboard</a> and the <a href="overview">QuickStats overview</a>.</p>
			<div data-juneau-region="instance-demo" data-juneau-region-type="row-detail"
				data-juneau-region-populate="instance-detail" data-juneau-row-id="%s">Loading&hellip;</div>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script>%s</script>
			</body>
			</html>
			""".formatted(
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
				INSTANCE_ID,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.RENDERS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.ICONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_JS_PATH),
				// Load order is a contract (ViewsMixin#HELPERS_JS_PATH): regions.js after views.js, helpers.js
				// after regions.js.  AssetLoadOrderBand_Test#a07/a08 pin this page's emitted <script src>
				// sequence so a future edit cannot silently reorder it.
				ViewsMixin.viewAssetUrl(req, ViewsMixin.REGIONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.HELPERS_JS_PATH),
				instanceDetailScript());
		return HttpResourceBean.of(
			ByteArrayBody.of(html.getBytes(UTF_8), MEDIA_HTML),
			list(ContentType.of(MEDIA_HTML)));
	}

	/**
	 * The inline populator script for {@link #instanceDetail(RestRequest)} &mdash; design &sect;11.1a's worked
	 * example, reproduced with the tab labels and field/column shapes exactly as genericized there (no
	 * trademarks, no observed-product vocabulary; see {@code @dual-hat-irs}).
	 *
	 * <p>
	 * <b>One deliberate deviation from the design snippet, flagged rather than silently applied:</b> the design's
	 * "Details" tab loader is {@code () => ctx.data ?? ctx.fetchDeclared()}, joining the panel's own expand GET.
	 * {@link org.apache.juneau.rest.server.views.RegionDef}/{@code ctx.fetchDeclared()}/{@code ctx.declared} are
	 * now real (WORK-J0522c) and proven non-privileged (design &sect;8.4's L12 property, tests 11/12) &mdash; but
	 * wiring THIS example to the design's exact line is still not a clean win, for a reason specific to this
	 * page's shape rather than a missing primitive: R14a's pre-fetch is keyed on the descriptor attached to the
	 * WHOLE region, not on any one pane inside it, so giving this region a declared {@code dataUrl} (the only way
	 * to make {@code ctx.data}/{@code ctx.fetchDeclared()} resolve to anything but <jk>null</jk> here) would gate
	 * the ENTIRE ten-tab strip's first paint behind the "Details" tab's own fetch &mdash; every one of the other
	 * nine tabs would wait on it too, which is the opposite of what this page exists to demonstrate (a tab strip
	 * that paints immediately and lazy-fetches per tab). The design's "joins the panel's own expand GET" framing
	 * describes a host (a real {@code RowDetailDef} expand click) that already carries a payload for R14a to
	 * reuse; this standalone, hand-enrolled region has no such host and no such GET to join, so the join this
	 * snippet demonstrates does not actually exist for this example's shape &mdash; a WORK-J0522d
	 * (host-enrolment) concern to resolve, not a WORK-J0522c one. This example's "Details" tab therefore keeps
	 * the SAME author-owned {@code fetch} the other nine tabs use ({@code at("details")}), unchanged from `b`'s
	 * landing, rather than accepting that page-wide regression for a partial demonstration; see this child's
	 * build report for the full reasoning.
	 *
	 * @return The {@code <script>} body (no surrounding tag).
	 */
	private static String instanceDetailScript() {
		return """
			(function () {
				var H = function () { return JuneauViews.helpers; };
				var DETAIL_FIELDS = [
					{ data: "name",         label: "Name" },
					{ data: "environment",  label: "Environment" },
					{ data: "type",         label: "Type" },
					{ data: "dbVendor",     label: "DB Vendor" },
					{ data: "appVersion",   label: "App Version" },
					{ data: "releaseCycle", label: "Release Cycle" },
					{ data: "status",       label: "Status",  render: "pill" },
					{ data: "modified",     label: "Modified" },
					{ data: "dbId",         label: "DB ID" },
					{ data: "dbModel",      label: "DB Model" }
				];
				var LIST_COLUMNS = {
					suspensions:    [ {data:"reason",  label:"Reason"},    {data:"since",  label:"Since"},
					                  {data:"actor",   label:"By"} ],
					directives:     [ {data:"name",    label:"Directive"}, {data:"value",  label:"Value"} ],
					releases:       [ {data:"version", label:"Version"},   {data:"applied",label:"Applied"} ],
					orgRequests:    [ {data:"kind",    label:"Kind"},      {data:"state",  label:"State"},
					                  {data:"created", label:"Created"} ],
					pendingChanges: [ {data:"change",  label:"Change"},    {data:"queued", label:"Queued"} ],
					checks:         [ {data:"check",   label:"Check"},     {data:"result", label:"Result",
					                  render:"pill"} ],
					auditTrail:     [ {data:"at",      label:"When"},      {data:"actor",  label:"Who"},
					                  {data:"what",    label:"What"} ]
				};
				JuneauViews.regions.register("instance-detail", function (ctx, container) {
					var h = H();
					var id = encodeURIComponent(ctx.ids.rowId);
					// ONE loader, reused ten times (including "details" - see this method's Javadoc for why,
					// unlike the design snippet, the open tab uses this too rather than ctx.fetchDeclared()).
					// The 404 arm is load-bearing: it resolves null (dataPane's EMPTY arm), never rejects, so a
					// missing sub-resource paints the empty state rather than the error state.
					var at = function (path) {
						return function (t) {
							// Accept: application/json is REQUIRED, not a nicety: this endpoint is content-
							// negotiated like every other Juneau data URL (juneau-views.js's own fetch calls set
							// the same header), and a bare fetch() with no Accept sends "*/*", which this server
							// resolves to its default HTML view rather than JSON.
							return fetch("/data/instance/" + id + "/" + path,
								{ signal: t.signal, headers: { "Accept": "application/json" } }).then(function (r) {
								return r.status === 404 ? null : r.ok ? r.json() : Promise.reject(new Error("HTTP " + r.status));
							});
						};
					};
					var mapPane = function (path) {
						return h.dataPane({ load: at(path), render: function (d) { return h.kvTable(d); } });
					};
					var listPane = function (path, cat) {
						return h.dataPane({ load: at(path), render: function (rows) { return h.recordTable(cat, rows); } });
					};
					container.replaceChildren(h.tabStrip([
						{ id: "details",         label: "Details",
						  populate: h.dataPane({ load: at("details"),
						      render: function (d) { return h.fieldGrid(DETAIL_FIELDS, { values: d }); } }),
						  lazy: false },
						{ id: "core-metrics",    label: "Core Metrics",    populate: mapPane("metrics/core") },
						{ id: "extra-metrics",   label: "Extra Metrics",   populate: mapPane("metrics/extra") },
						{ id: "suspensions",     label: "Suspensions",
						  populate: listPane("suspensions", LIST_COLUMNS.suspensions) },
						{ id: "directives",      label: "Directives",
						  populate: listPane("directives", LIST_COLUMNS.directives) },
						{ id: "releases",        label: "Releases",
						  populate: listPane("releases", LIST_COLUMNS.releases) },
						{ id: "org-requests",    label: "Org Requests",
						  populate: listPane("org-requests", LIST_COLUMNS.orgRequests) },
						{ id: "pending-changes", label: "Pending Changes",
						  populate: listPane("pending-changes", LIST_COLUMNS.pendingChanges) },
						{ id: "checks",          label: "Checks",
						  populate: listPane("checks", LIST_COLUMNS.checks) },
						{ id: "audit-trail",     label: "Audit Trail",
						  populate: listPane("audit-trail", LIST_COLUMNS.auditTrail) }
					], { active: "details", signal: ctx.signal }));
				});
				JuneauViews.regions.enrolIn(document);
			})();
			""";
	}

	/**
	 * [GET /flagged] &mdash; a standalone table whose column title resolves a {@code $FV} server value.
	 *
	 * <p>
	 * Uses {@link ViewSlot#envelope(RestRequest, ViewDef)} so the declared {@code $FV{flaggedCount}} chrome is
	 * resolved against a per-response sibling session; the standalone {@link ViewTable} HTML-emitter
	 * path intentionally does not resolve {@code $FV}.
	 *
	 * @param req The current request, whose var resolver knows {@code $FV}.
	 * @return The HTML page with an empty slot.
	 */
	@RestGet(path="/flagged", swagger=@OpSwagger(ignore=true))
	public HttpResource flagged(RestRequest req) {
		var envelopeUrl = req.getContextPath() + "/flagged/view";
		var html = """
			<!DOCTYPE html>
			<html lang="en">
			<head>
			<meta charset="utf-8">
			<title>Apache Juneau - Flagged Widgets</title>
			<link rel="stylesheet" href="%s">
			<link rel="stylesheet" href="%s">
			<style>
			\tbody { font-family: -apple-system, Helvetica, Arial, sans-serif; margin: 2em; }
			</style>
			</head>
			<body>
			<h1>Apache Juneau &mdash; Flagged Widgets</h1>
			<div id="widgets-flagged"></div>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script>
			JuneauViews.regions.mount({ "widgets-flagged": { table: "%s" } });
			</script>
			</body>
			</html>
			""".formatted(
				DataTablesMixin.DATATABLES_CSS_CDN_URL,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
				DataTablesMixin.JQUERY_CDN_URL,
				DataTablesMixin.DATATABLES_JS_CDN_URL,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.RENDERS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.ICONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.REGIONS_JS_PATH),
				envelopeUrl);
		return HttpResourceBean.of(
			ByteArrayBody.of(html.getBytes(UTF_8), MEDIA_HTML),
			list(ContentType.of(MEDIA_HTML)));
	}

	/**
	 * [GET /flagged/view] &mdash; the {@link ViewSlot} envelope for {@link #flaggedView()}.
	 *
	 * @param req The current request, whose var resolver knows {@code $FV}.
	 * @return The slot envelope with resolved {@code $FV} chrome.
	 */
	@RestGet(path="/flagged/view", swagger=@OpSwagger(ignore=true))
	public ViewSlot flaggedViewEnvelope(RestRequest req) {
		return ViewSlot.envelope(req, flaggedView());
	}

	//------------------------------------------------------------------------------------------------------------------
	// CLIENT-mode data endpoints - each returns its full row list; DataTables paginates in-browser.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * [GET /data/widgets/active] &mdash; the Active sub-tab's rows.
	 *
	 * @return The active widgets.
	 */
	@RestGet(path="/data/widgets/active", swagger=@OpSwagger(ignore=true))
	public List<Widget> activeData() {
		return ACTIVE_WIDGETS;
	}

	/**
	 * [GET /data/widgets/active/{id}] &mdash; expand envelope for one active widget (row id = {@link Widget#name}).
	 *
	 * @param id The widget name.
	 * @return {@code {contractVersion, fields}} for the expander.
	 */
	@RestGet(path="/data/widgets/active/{id}", swagger=@OpSwagger(ignore=true))
	public Map<String,Object> activeWidgetDetail(@Path("id") String id) {
		for (var w : ACTIVE_WIDGETS)
			if (id.equals(w.name))
				return detailEnvelope(Map.of(
					COL_OWNER, w.owner,
					COL_UPDATED_AT, w.updatedAt,
					"notes", w.notes));
		throw new NotFound("Widget not found: %s", id);
	}

	/**
	 * [GET /data/widgets/archived] &mdash; the Archived sub-tab's rows.
	 *
	 * @return The archived widgets.
	 */
	@RestGet(path="/data/widgets/archived", swagger=@OpSwagger(ignore=true))
	public List<Widget> archivedData() {
		return ARCHIVED_WIDGETS;
	}

	/**
	 * [GET /data/audit] &mdash; the Audit Log tab's rows.
	 *
	 * @return The audit log entries.
	 */
	@RestGet(path="/data/audit", swagger=@OpSwagger(ignore=true))
	public List<AuditEntry> auditData() {
		return AUDIT_LOG;
	}

	/**
	 * [GET /data/alerts] &mdash; the Alerts pair's rows.
	 *
	 * @return The alerts.
	 */
	@RestGet(path="/data/alerts", swagger=@OpSwagger(ignore=true))
	public List<Alert> alertsData() {
		return ALERTS;
	}

	/**
	 * [GET /data/alerts/events] &mdash; the nested "related events" table's rows, scoped to one parent alert.
	 *
	 * <p>
	 * The nested table's own data GET carries the parent alert id under the {@code alertId} query parameter (the
	 * {@code parentScopeParam} declared on its {@link NestedTableDef}).  An absent/blank scope returns nothing rather
	 * than the whole unscoped set &mdash; a nested table without a parent id has no rows to show.
	 *
	 * @param alertId The parent alert id the nested table scoped its request to.
	 * @return The events for that alert.
	 */
	@RestGet(path="/data/alerts/events", swagger=@OpSwagger(ignore=true))
	public List<AlertEvent> alertEventsData(@Query("alertId") String alertId) {
		if (alertId == null || alertId.isBlank())
			return List.of();
		var out = new ArrayList<AlertEvent>();
		for (var e : ALERT_EVENTS)
			if (alertId.equals(e.alertId))
				out.add(e);
		return out;
	}

	/**
	 * [GET /data/alerts/{id}] &mdash; expand envelope for one alert.
	 *
	 * @param id The alert id.
	 * @return {@code {contractVersion, fields}} for the expander.
	 */
	@RestGet(path="/data/alerts/{id}", swagger=@OpSwagger(ignore=true))
	public Map<String,Object> alertDetail(@Path("id") String id) {
		var a = findAlert(id);
		return detailEnvelope(Map.of(
			COL_SEVERITY, a.severity,
			COL_TITLE, a.title,
			"summary", a.summary,
			COL_ASSIGNEE, a.assignee));
	}

	/**
	 * [GET /data/alerts/{id}/ack-form] &mdash; the modal-open confirmation payload for the {@code present=dialog}
	 * {@code ack} action: a typed input form (resolution comment, notify toggle, severity re-assignment select) plus
	 * a nested {@code type=action} button that opens the confirm-only {@code esc} dialog over this one.
	 *
	 * <p>
	 * The serving-path {@link ModalDef#checked() checked()} hook stamps the contract version and fail-closed validates
	 * the modal/form &mdash; a malformed form fails here at serve time, not silently on the wire.
	 *
	 * <p>
	 * This is also the toolkit's example of the <b>third</b> named {@link BarSlot} host, {@link ModalDef#barSlot}:
	 * a static {@link BarText} for severity context beside a {@link BarBadge} whose count
	 * ({@link #countOtherOpen(Alert)}) is live, painted client-side from this same JSON by
	 * {@code insertDialogBarSlot} the moment the dialog opens &mdash; unlike a page-nav or
	 * {@link RowDetailDef} bar slot, there is no server-rendered pass to ride into.
	 *
	 * @param id The alert id.
	 * @return The validated, version-stamped modal definition.
	 */
	@RestGet(path="/data/alerts/{id}/ack-form", swagger=@OpSwagger(ignore=true))
	public ModalDef ackForm(@Path("id") String id) {
		var a = findAlert(id);
		var key = IdempotencyKey.mint(ACTION_ACK, id);
		return ModalDef.create("Acknowledge this alert?")
			.field("Id", a.id)
			.field(TITLE_SEVERITY, a.severity)
			.field(TITLE_TITLE, a.title)
			.form(FormDef.create()
				.field(FormDef.Input.of("resolution", "Resolution comment", "textarea").required()
					.maxLength(500).help("Describe what you did to acknowledge this alert."))
				.field(FormDef.Input.of("notify", "Notify on-call", "toggle").value("true"))
				.field(FormDef.Input.of(COL_SEVERITY, "Re-assign severity", "select")
					.option(SEVERITY_CRITICAL, "Critical").option("warning", "Warning").option(SEVERITY_INFO, "Info").value(a.severity))
				.field(FormDef.Input.of("escalate", "Escalate instead…", INPUT_TYPE_ACTION).action(ActionRef.of(ACTION_ESC))))
			.barSlot(BarSlot.create("ack-form-bar").widgets(
				BarText.of("severity-note", "Severity: " + a.severity),
				BarBadge.of("other-open").label("other open at this severity").badge(Badge.count(countOtherOpen(a))
					.tone(SEVERITY_CRITICAL.equals(a.severity) ? Tone.DANGER : Tone.WARN))))
			.idempotencyKey(key.value())
			.checked();
	}

	/**
	 * Counts other {@link #STATUS_OPEN} alerts sharing {@code a}'s severity, for the {@link #ackForm(String)}
	 * dialog's live {@link BarBadge} count.
	 *
	 * @param a The alert being acknowledged (excluded from its own count).
	 * @return The number of other open alerts at the same severity.
	 */
	private static int countOtherOpen(Alert a) {
		var n = 0;
		for (var o : ALERTS)
			if (o != a && STATUS_OPEN.equals(o.status) && a.severity.equals(o.severity)) n++;
		return n;
	}

	/**
	 * [POST /data/alerts/{id}/ack] &mdash; acknowledge an open alert.
	 *
	 * @param id The alert id.
	 * @return The typed action result carrying the updated row.
	 */
	@RestPost(path="/data/alerts/{id}/ack", swagger=@OpSwagger(ignore=true))
	public ActionResult ackAlert(@Path("id") String id) {
		var a = findAlert(id);
		a.status = STATUS_ACKNOWLEDGED;
		return ActionResult.success(a);
	}

	/**
	 * [POST /data/alerts/{id}/esc] &mdash; escalate an alert.
	 *
	 * @param id The alert id.
	 * @return The typed action result carrying the updated row.
	 */
	@RestPost(path="/data/alerts/{id}/esc", swagger=@OpSwagger(ignore=true))
	public ActionResult escalateAlert(@Path("id") String id) {
		var a = findAlert(id);
		a.status = STATUS_ESCALATED;
		return ActionResult.success(a);
	}

	/**
	 * [GET /data/cards/summary] &mdash; the Live Alert Metrics slot's refresh envelope: live open/acknowledged/
	 * escalated counts (moved by the {@code ack}/{@code esc} endpoints above) plus a server timestamp.  The field
	 * keys match the labels the {@link #dashboardMarkup() dashboard} populate script reads.
	 *
	 * @return {@code {contractVersion, fields}} for the card runtime.
	 */
	@RestGet(path="/data/cards/summary", swagger=@OpSwagger(ignore=true))
	public Map<String,Object> cardsSummary() {
		var open = 0;
		var ack = 0;
		var esc = 0;
		for (var a : ALERTS) {
			if (STATUS_OPEN.equals(a.status)) open++;
			else if (STATUS_ACKNOWLEDGED.equals(a.status)) ack++;
			else if (STATUS_ESCALATED.equals(a.status)) esc++;
		}
		var fields = new LinkedHashMap<String,Object>();
		fields.put(STATUS_OPEN, open);
		fields.put(STATUS_ACKNOWLEDGED, ack);
		fields.put(STATUS_ESCALATED, esc);
		fields.put("asOf", Instant.now(Clock.systemUTC()).toString());
		return cardEnvelope(fields);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Region + helpers demo data (WORK-J0522b): each endpoint backs one instanceDetailScript() tab.  Every one
	// returns a BARE values map or a BARE array of values maps - no {contractVersion,fields} envelope, no
	// "items" wrapper - because dataPane's contract (design section 8.5) is unwrapped shapes; an envelope is
	// unwrapped in the AUTHOR's own loader, never invented by the endpoint.  Any id but INSTANCE_ID 404s, so the
	// client's at(path) helper's 404-to-null arm (dataPane's EMPTY state) is real, not defensive dead code.
	//------------------------------------------------------------------------------------------------------------------

	private static void requireDemoInstance(String id) {
		if (!INSTANCE_ID.equals(id))
			throw new NotFound("No such instance: %s", id);
	}

	/**
	 * [GET /data/instance/{id}/details] &mdash; the "Details" tab's field-grid values map.
	 *
	 * @param id The instance id ({@link #INSTANCE_ID} is the only one that resolves).
	 * @return The values map {@link #instanceDetailScript()}'s {@code DETAIL_FIELDS} catalog joins against.
	 */
	@RestGet(path="/data/instance/{id}/details", swagger=@OpSwagger(ignore=true))
	public Map<String,Object> instanceDetails(@Path("id") String id) {
		requireDemoInstance(id);
		var out = new LinkedHashMap<String,Object>();
		out.put("name", INSTANCE_ID);
		out.put("environment", "staging");
		out.put("type", "worker");
		out.put("dbVendor", "postgres");
		out.put("appVersion", "4.12.0");
		out.put("releaseCycle", "weekly");
		out.put(COL_STATUS, STATUS_ACTIVE);
		out.put("modified", "2026-09-01T10:00:00Z");
		out.put("dbId", "db-9981");
		out.put("dbModel", "shared");
		return out;
	}

	/**
	 * [GET /data/instance/{id}/metrics/core] &mdash; the "Core Metrics" tab's value map.
	 *
	 * @param id The instance id.
	 * @return The core metrics.
	 */
	@RestGet(path="/data/instance/{id}/metrics/core", swagger=@OpSwagger(ignore=true))
	public Map<String,Object> instanceCoreMetrics(@Path("id") String id) {
		requireDemoInstance(id);
		var out = new LinkedHashMap<String,Object>();
		out.put("cpuPercent", 34);
		out.put("memoryPercent", 58);
		out.put("requestsPerSecond", 210);
		return out;
	}

	/**
	 * [GET /data/instance/{id}/metrics/extra] &mdash; the "Extra Metrics" tab's value map (the design's own
	 * genericization of what an internal draft named after a specific product acronym; see
	 * {@link #instanceDetailScript()}'s Javadoc and design &sect;11.1a's "transfer rule").
	 *
	 * @param id The instance id.
	 * @return The extra metrics.
	 */
	@RestGet(path="/data/instance/{id}/metrics/extra", swagger=@OpSwagger(ignore=true))
	public Map<String,Object> instanceExtraMetrics(@Path("id") String id) {
		requireDemoInstance(id);
		var out = new LinkedHashMap<String,Object>();
		out.put("queueDepth", 3);
		out.put("cacheHitRate", "0.92");
		return out;
	}

	/**
	 * [GET /data/instance/{id}/suspensions] &mdash; the "Suspensions" tab's record list.
	 *
	 * @param id The instance id.
	 * @return The suspensions.
	 */
	@RestGet(path="/data/instance/{id}/suspensions", swagger=@OpSwagger(ignore=true))
	public List<Map<String,Object>> instanceSuspensions(@Path("id") String id) {
		requireDemoInstance(id);
		return list(
			rowOf("reason", "Scheduled maintenance", "since", "2026-08-20T02:00:00Z", "actor", "alice"),
			rowOf("reason", "Quota exceeded", "since", "2026-07-11T14:30:00Z", "actor", "system"));
	}

	/**
	 * [GET /data/instance/{id}/directives] &mdash; the "Directives" tab's record list.
	 *
	 * @param id The instance id.
	 * @return The directives.
	 */
	@RestGet(path="/data/instance/{id}/directives", swagger=@OpSwagger(ignore=true))
	public List<Map<String,Object>> instanceDirectives(@Path("id") String id) {
		requireDemoInstance(id);
		return list(
			rowOf("name", "max-connections", "value", "200"),
			rowOf("name", "read-only", "value", "false"));
	}

	/**
	 * [GET /data/instance/{id}/releases] &mdash; the "Releases" tab's record list.
	 *
	 * @param id The instance id.
	 * @return The releases.
	 */
	@RestGet(path="/data/instance/{id}/releases", swagger=@OpSwagger(ignore=true))
	public List<Map<String,Object>> instanceReleases(@Path("id") String id) {
		requireDemoInstance(id);
		return list(
			rowOf("version", "4.12.0", "applied", "2026-09-01T10:00:00Z"),
			rowOf("version", "4.11.2", "applied", "2026-08-15T09:00:00Z"),
			rowOf("version", "4.11.1", "applied", "2026-08-02T09:00:00Z"));
	}

	/**
	 * [GET /data/instance/{id}/org-requests] &mdash; the "Org Requests" tab's record list.
	 *
	 * @param id The instance id.
	 * @return The org requests.
	 */
	@RestGet(path="/data/instance/{id}/org-requests", swagger=@OpSwagger(ignore=true))
	public List<Map<String,Object>> instanceOrgRequests(@Path("id") String id) {
		requireDemoInstance(id);
		return list(rowOf("kind", "resize", META_STATE, "completed", "created", "2026-08-28T00:00:00Z"));
	}

	/**
	 * [GET /data/instance/{id}/pending-changes] &mdash; the "Pending Changes" tab's record list.
	 *
	 * @param id The instance id.
	 * @return The pending changes.
	 */
	@RestGet(path="/data/instance/{id}/pending-changes", swagger=@OpSwagger(ignore=true))
	public List<Map<String,Object>> instancePendingChanges(@Path("id") String id) {
		requireDemoInstance(id);
		return List.of();
	}

	/**
	 * [GET /data/instance/{id}/checks] &mdash; the "Checks" tab's record list; {@code result} renders as a pill.
	 *
	 * @param id The instance id.
	 * @return The checks.
	 */
	@RestGet(path="/data/instance/{id}/checks", swagger=@OpSwagger(ignore=true))
	public List<Map<String,Object>> instanceChecks(@Path("id") String id) {
		requireDemoInstance(id);
		return list(
			rowOf("check", "Disk space", "result", STATUS_ACTIVE),
			rowOf("check", "Health probe", "result", STATUS_ACTIVE),
			rowOf("check", "Backup age", "result", STATUS_ERROR));
	}

	/**
	 * [GET /data/instance/{id}/audit-trail] &mdash; the "Audit Trail" tab's record list.
	 *
	 * @param id The instance id.
	 * @return The audit trail.
	 */
	@RestGet(path="/data/instance/{id}/audit-trail", swagger=@OpSwagger(ignore=true))
	public List<Map<String,Object>> instanceAuditTrail(@Path("id") String id) {
		requireDemoInstance(id);
		return list(
			rowOf("at", "2026-09-01T10:00:00Z", "actor", "alice", "what", "deployed 4.12.0"),
			rowOf("at", "2026-08-28T00:00:00Z", "actor", "bob", "what", "resized instance"));
	}

	/** Builds a {@code LinkedHashMap} from alternating key/value pairs, preserving the given key order. */
	private static Map<String,Object> rowOf(Object... kv) {
		var out = new LinkedHashMap<String,Object>();
		for (var i = 0; i < kv.length; i += 2)
			out.put((String)kv[i], kv[i + 1]);
		return out;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Row generation - enough rows per panel that a column-sizing regression would be visibly wrong.
	//------------------------------------------------------------------------------------------------------------------

	/** Refresh envelope: a data-only field map (no table columns, no Java card type). */
	private static Map<String,Object> cardEnvelope(Map<String,?> fields) {
		var out = new LinkedHashMap<String,Object>();
		out.put("contractVersion", "1");
		out.put("fields", fields);
		return out;
	}

	private static Map<String,Object> detailEnvelope(Map<String,?> fields) {
		var out = new LinkedHashMap<String,Object>();
		out.put("contractVersion", RowDetailDef.CONTRACT_VERSION);
		out.put("fields", fields);
		return out;
	}

	private static Alert findAlert(String id) {
		for (var a : ALERTS)
			if (id.equals(a.id))
				return a;
		throw new NotFound("Alert not found: %s", id);
	}

	private static List<Widget> buildActiveWidgets() {
		var out = new ArrayList<Widget>();
		var owners = List.of("Platform", "Storefront", "Billing", "Growth");
		for (var i = 1; i <= 30; i++) {
			// Every 7th widget is "error" (rowClassRule target); the rest alternate active/active/active for a
			// mostly-healthy-looking table with a few flagged rows scattered through it.
			var status = i % 7 == 0 ? STATUS_ERROR : STATUS_ACTIVE;
			out.add(new Widget(
				"widget-" + i,
				status,
				owners.get(i % owners.size()),
				"2026-08-%02dT09:%02d:00Z".formatted((i % 18) + 1, i % 60),
				status.equals(STATUS_ERROR)
					? "Failed health check on 2026-08-%02d; see incident log.".formatted((i % 18) + 1)
					: "No open issues."));
		}
		return List.copyOf(out);
	}

	private static List<Widget> buildArchivedWidgets() {
		var out = new ArrayList<Widget>();
		var owners = List.of("Platform", "Storefront", "Billing");
		for (var i = 1; i <= 20; i++) {
			out.add(new Widget(
				"widget-legacy-" + i,
				VALUE_ARCHIVED,
				owners.get(i % owners.size()),
				"2025-%02d-01T00:00:00Z".formatted((i % 12) + 1),
				"Archived; superseded by widget-" + (i + 30) + "."));
		}
		return List.copyOf(out);
	}

	private static List<AuditEntry> buildAuditLog() {
		var out = new ArrayList<AuditEntry>();
		var actors = List.of("alice", "bob", "carol", "dave");
		var actions = List.of("created", "updated", VALUE_ARCHIVED, "restored", "deleted");
		for (var i = 1; i <= 40; i++) {
			out.add(new AuditEntry(
				"2026-08-%02dT%02d:00:00Z".formatted((i % 18) + 1, i % 24),
				actors.get(i % actors.size()),
				actions.get(i % actions.size()) + " widget-" + ((i % 30) + 1)));
		}
		return List.copyOf(out);
	}

	private static List<Alert> buildAlerts() {
		var out = new ArrayList<Alert>();
		var severities = List.of(SEVERITY_CRITICAL, "warning", SEVERITY_INFO);
		var assignees = List.of("alice", "bob", "carol");
		for (var i = 1; i <= 12; i++) {
			out.add(new Alert(
				"ALRT-" + i,
				severities.get(i % severities.size()),
				"Synthetic alert " + i,
				STATUS_OPEN,
				"Fired by the views-example generator; row " + i + " of the fake pager.",
				assignees.get(i % assignees.size())));
		}
		return out;
	}

	private static List<AlertEvent> buildAlertEvents() {
		var out = new ArrayList<AlertEvent>();
		var kinds = List.of("fired", "notified", STATUS_ACKNOWLEDGED, "note");
		// A handful of events per alert so the nested table has enough rows to page/sort against, and so a
		// mis-scoped request (wrong or missing alertId) would be visibly wrong - some alerts' rows leaking into
		// another's expander.
		for (var i = 1; i <= 12; i++) {
			var alertId = "ALRT-" + i;
			var count = 3 + (i % 4);
			for (var j = 0; j < count; j++)
				out.add(new AlertEvent(
					alertId,
					"2026-08-%02dT%02d:%02d:00Z".formatted((i % 18) + 1, (8 + j) % 24, (i * 7 + j * 11) % 60),
					kinds.get(j % kinds.size()),
					kinds.get(j % kinds.size()) + " event " + (j + 1) + " for " + alertId));
		}
		return List.copyOf(out);
	}
}
