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
import org.apache.juneau.marshall.marshaller.Html;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.DataTablesMixin;
import org.apache.juneau.rest.server.servlet.BasicRestServlet;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.views.ViewDef.Dir;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.Op;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Demonstrates a {@link PageDef} with a sub-tabbed tab ("Catalog" &rarr; Active/Archived) alongside sibling
 * plain tabs ("Audit Log", "Alerts"), served on an embedded Jetty server via {@link ExampleViewsServer}.
 *
 * <p>
 * This is a real caller of {@link PageDef}'s {@link Tab#subtabs(Subtab...) subtabs} outside the views module's own
 * test sources &mdash; see the class-level scope notes below for what it deliberately exercises.
 *
 * <h5 class='section'>What this dogfoods (beyond the bare sub-tab requirement):</h5>
 * <ul>
 * 	<li>The "Active" sub-tab additionally declares {@link ViewDef#poll(long) poll} and
 * 		{@link ViewDef#details(RowDetailDef) details}, plus a ribbon and a {@code rowClassRule}, so this one view
 * 		exercises most of the toolkit's declarative surface in one place.  Expand GET
 * 		{@code /data/widgets/active/{id}} projects owner/updatedAt/notes (the expander is the only place notes
 * 		appear).
 * 	<li>The "Archived" sub-tab and the "Audit Log" tab are deliberately PLAIN (no ribbon/poll/details), both to
 * 		satisfy the "at least one sibling plain tab" requirement and to keep a contrasting baseline the sub-tabbed
 * 		panel's blank-panel regression would show up against.
 * 	<li>The "Alerts" tab dogfoods {@link RowDetailDef} with two named sections, two mutating {@link ActionRef}s,
 * 		{@link SafeAction#COLLAPSE}, and expand GET {@code /data/alerts/{id}}.  Its "Context" section further
 * 		dogfoods a read-only {@link NestedTableDef}: a "related events" table with its own client-mode GET
 * 		{@code /data/alerts/events}, scoped to the parent alert by the {@code alertId} query parameter.
 * 	<li>Three distinct row types ({@link Widget}, {@link AuditEntry}, {@link Alert}) are composed into one page,
 * 		rather than one type reused everywhere.
 * 	<li>Every view uses {@link DataMode#CLIENT} for simplicity (a static in-memory row list, no
 * 		{@code ProtocolQueryable}/{@code QueryableSettings} wiring) &mdash; {@code SERVER} mode is already covered
 * 		end-to-end by {@code ViewServerWiring_Test} in the views module itself, so this example does not repeat it.
 * 	<li>Each sub-tab/tab carries enough rows (see {@link #buildActiveWidgets()}/{@link #buildArchivedWidgets()}/
 * 		{@link #buildAuditLog()}/{@link #buildAlerts()}) that a column-sizing regression from the eager-init defect
 * 		would be visibly wrong, not just theoretically present.
 * </ul>
 *
 * <p>
 * Every panel on the composed page is a {@link ViewDef} table.  A separate {@code /dashboard} endpoint dogfoods
 * the card-layout widget as a second, non-table consumer: {@link #dashboardGrid()} builds a {@link CardGrid}
 * (rendered by {@link CardGridTable}) with a static summary card and a live, auto-refreshing metrics card backed
 * by the {@code /data/cards/summary} refresh envelope.
 *
 * <p>
 * A third endpoint, {@code /overview}, dogfoods the {@link QuickStats} header strip together with both display-only
 * pill hosts: {@link #overviewView()} attaches a strip of a scalar tile, a meter and a segmented breakdown above the
 * table's toolbar, paints a display-only status pill in a column, and repeats that chip as a fill-sink pill inside the
 * row-detail expander &mdash; the inert contrast case for the Alerts tab's action-bound pill.  Its tones and the
 * pills' tones come from one closed {@link StatusTone} palette.
 *
 * @since 10.0.0
 */
@Rest(mixins=ViewsMixin.class)
public class ExampleViewsRest extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	/** The stable page id &mdash; also the first hash segment of a deep link, e.g. {@code #widgets-demo/catalog/archived}. */
	public static final String PAGE_ID = "widgets-demo";

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
	private static final String CARD_ACTIVE = SPELLING_ACTIVE;
	private static final String STATUS_ACTIVE = SPELLING_ACTIVE;
	private static final String SPELLING_ALERTS = "alerts";
	private static final String TAB_ALERTS = SPELLING_ALERTS;
	private static final String VIEW_ALERTS = SPELLING_ALERTS;
	private static final String CARD_ALERTS = SPELLING_ALERTS;
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
	// The composed page: Catalog (sub-tabbed: Active/Archived) + sibling Audit Log + Alerts tabs.
	//------------------------------------------------------------------------------------------------------------------

	static PageDef page() {
		return PageDef.create(PAGE_ID)
			.title("Juneau Views Example")
			.tabs(
				Tab.create("catalog", "Catalog").subtabs(
					Subtab.create(SUBTAB_ACTIVE, "Active").view(activeView()),
					Subtab.create(VALUE_ARCHIVED, TITLE_ARCHIVED).view(archivedView())),
				Tab.create("audit", "Audit Log").view(auditView()),
				Tab.create(TAB_ALERTS, "Alerts").view(alertsView()))
			.build();
	}

	/** The sub-tabbed tab's first (default) panel &mdash; dogfoods poll/details/ribbon/rowClassRule together. */
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
				.sections(DetailSection.create("info", "Info")
					.fields(
						DetailField.of(COL_OWNER).title(TITLE_OWNER),
						DetailField.of(COL_UPDATED_AT).title("Last updated"),
						DetailField.of("notes").title("Notes"))
					.actions(ActionBar.create().items(SafeAction.COLLAPSE))))
			.build();
	}

	/** The sub-tabbed tab's second panel &mdash; deliberately plain (the "at least one sub-tab has no bells on it" case). */
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
	 * scalar; {@link ViewTable#of(RestRequest, ViewDef)} resolves the <js>"$FV{flaggedCount}"</js> chrome at serve
	 * time into plain, serializer-encoded text.  {@code $FV} is registered by {@link #varResolver(VarResolver.Builder)}.
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

	/** The sibling PLAIN leaf tab (no sub-tabs) - the contrast case the blank-panel regression needs. */
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

	/** Fake alerts table &mdash; two detail sections, two mutating ActionRefs, expand GET. */
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
				.sections(
					DetailSection.create("overview", "Overview")
						.columns(2)
						.fields(
							DetailField.of(COL_SEVERITY).title(TITLE_SEVERITY),
							DetailField.of(COL_TITLE).title(TITLE_TITLE))
						.actions(ActionBar.create().items(ActionRef.of(ACTION_ACK), SafeAction.COLLAPSE)),
					DetailSection.create("context", "Context")
						.fields(
							DetailField.of("summary").title("Summary"),
							// A field-hosted ActionBar: the third bar host, painted in this row's VALUE column
							// beside the assignee it acts on rather than up in a toolbar.  The row carries a value
							// AND a bar at once - the same declared "esc" RowAction the section bar below offers,
							// reached from a second host, with no new action, endpoint, or contract version.  The
							// in-field buttons are quiet by construction; there is nothing to declare for that.
							DetailField.of(COL_ASSIGNEE).title("Assignee")
								.actions(ActionBar.create().items(ActionRef.of(ACTION_ESC))))
						.actions(ActionBar.create().items(ActionRef.of(ACTION_ESC)))
						// A read-only table nested in the expander: its own client-mode GET is scoped to the
						// parent alert by the "alertId" query param (no {parentId} URL template).  It runs only
						// after the alert's detail GET succeeds and the Context pane becomes visible.
						.table(NestedTableDef.create(relatedEventsView()).parentScopeParam("alertId"))))
			.build();
	}

	/** The read-only nested "related events" table dogfooded inside the Alerts expander's Context section. */
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

	//------------------------------------------------------------------------------------------------------------------
	// Card dashboard (a second, non-table consumer of the toolkit): a CardGrid rendered by CardGridTable.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * A {@link CardGrid} dashboard: one static summary card (server-rendered, works with JavaScript disabled) and
	 * one live metrics card that declares a same-origin refresh endpoint and a poll interval, so
	 * {@code juneau-cards.js} wires its built-in refresh button and an auto-refresh loop.  This dogfoods the
	 * card-layout widget as a distinct delivery shape from the {@link ViewDef} tables above &mdash; the refresh
	 * envelope's fields are keyed by {@link CardField#data}, not table columns.
	 *
	 * @return The dashboard grid.
	 */
	static CardGrid dashboardGrid() {
		return CardGrid.create("ops").title("Operations Dashboard").minCardPx(320).cards(
			Card.create("fleet", "Fleet Summary").body(
				CardFieldList.create().columns(2).fields(
					CardField.of("total", "Total widgets", Integer.toString(ACTIVE_WIDGETS.size() + ARCHIVED_WIDGETS.size())),
					CardField.of(CARD_ACTIVE, "Active", Integer.toString(ACTIVE_WIDGETS.size())),
					CardField.of(VALUE_ARCHIVED, TITLE_ARCHIVED, Integer.toString(ARCHIVED_WIDGETS.size())),
					CardField.of(CARD_ALERTS, "Total alerts", Integer.toString(ALERTS.size())))),
			Card.create("live", "Live Alert Metrics").body(
				CardFieldList.create().columns(2)
					.fields(
						CardField.of(STATUS_OPEN, "Open"),
						CardField.of(STATUS_ACKNOWLEDGED, "Acknowledged"),
						CardField.of(STATUS_ESCALATED, "Escalated"),
						CardField.of("asOf", "As of"))
					// Same-origin, non-templated path; poll well above the 5s floor so the staleness chip's advance is
					// easy to watch.  Acknowledge/escalate an alert on the main page, then refresh to see counts move.
					.refresh("/data/cards/summary")
					.pollIntervalMs(10_000)));
	}

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
	 * display-only: it has no refresh endpoint and no poll interval, unlike the {@link #dashboardGrid() live card}
	 * above.  A figure that needs to move belongs on a card or in a column, not in a quick-stat.
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
	 * The sink pill is the contrast case for the {@link #alertsView() Alerts} tab's action-bound pill: a fill sink has
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
				.sections(DetailSection.create("overview", "Overview")
					.columns(2)
					.fields(
						DetailField.of(COL_SEVERITY).title(TITLE_SEVERITY),
						DetailField.of(COL_ASSIGNEE).title("Assignee"),
						// The fill-sink pill: same chip, rendered by the sink-path renderer, unconditionally inert.
						DetailField.of(COL_STATUS).title(TITLE_STATUS).render(Render.pill().meta(META_FIELD, META_STATE)))
					.actions(ActionBar.create().items(SafeAction.COLLAPSE))))
			.build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// HTML page (hand-built, no template engine - this module takes no dependency on FreeMarker/console-ui).
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * [GET /] &mdash; the composed page: tab bar + sub-tab bar + all panels, plus the DataTables/jQuery
	 * (caller-provided, per ASF category-A discipline) and first-party toolkit asset links a real page needs.
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}
	 * 	so the head links stay correct however this example is mounted.
	 * @return The full HTML page.
	 */
	@RestGet(path="/", summary="The Catalog (Active/Archived sub-tabs) + Audit Log + Alerts demo page")
	public HttpResource index(RestRequest req) {
		var pageMarkup = Html.of(PageTable.of(req, page()));
		var deepLink = "#" + PAGE_ID + "/catalog/archived";
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
			\t.jc-tab-bar, .jc-subtab-bar { margin-bottom: 1em; }
			</style>
			</head>
			<body>
			<h1>Apache Juneau &mdash; Rich Views Example</h1>
			<p>Demonstrates a sub-tabbed <code>PageDef</code>: the <b>Catalog</b> tab holds two sub-tabs
			(<b>Active</b>/<b>Archived</b>), <b>Audit Log</b> is a sibling plain leaf tab, and <b>Alerts</b>
			dogfoods row-detail sections with an expand GET and mutating action-bar buttons. The Active sub-tab
			also declares a poll interval (watch the staleness chip) and a row-details expander (click any row).</p>
			<p><a href="%s">Deep link straight to the Archived sub-tab</a> (exercises
			<code>juneau-pages.js</code>'s hash-routing on load, not just via the tab bar's own links).</p>
			<p>See also the <a href="dashboard">card dashboard</a> and the
			<a href="overview">QuickStats overview</a> (a stats strip above a table, with display-only and
			fill-sink status pills).</p>
			%s
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			</body>
			</html>
			""".formatted(
				DataTablesMixin.DATATABLES_CSS_CDN_URL,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
				deepLink,
				pageMarkup,
				DataTablesMixin.JQUERY_CDN_URL,
				DataTablesMixin.DATATABLES_JS_CDN_URL,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.RENDERS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.ICONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.RIBBON_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_JS_PATH),
				// Must load AFTER juneau-views.js - it calls the public NS.init.initTable to lazy-init a
				// sub-tab's DataTable on first activation.
				ViewsMixin.viewAssetUrl(req, ViewsMixin.PAGES_JS_PATH));
		return HttpResourceBean.of(
			ByteArrayBody.of(html.getBytes(UTF_8), MEDIA_HTML),
			list(ContentType.of(MEDIA_HTML)));
	}

	/**
	 * [GET /dashboard] &mdash; the {@link #dashboardGrid() card dashboard}: a static summary card plus a live,
	 * auto-refreshing metrics card, with only the {@code juneau-icons.js} &rarr; {@code juneau-cards.js} assets a
	 * card page needs (no DataTables/jQuery &mdash; a card grid carries no table).
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}.
	 * @return The card-dashboard HTML page.
	 */
	@RestGet(path="/dashboard", summary="A CardGrid dashboard: a static summary card + a live, auto-refreshing metrics card")
	@SuppressWarnings("deprecation") // Deliberate: this host composes ViewsMixin alone, so it is the in-tree proof that the relocated card asset still serves from the compatibility mount.
	public HttpResource dashboard(RestRequest req) {
		var gridMarkup = Html.of(CardGridTable.of(dashboardGrid()));
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
			<p>A <code>CardGridTable</code> dashboard. The <b>Fleet Summary</b> card is static (server-rendered,
			legible with JavaScript disabled); the <b>Live Alert Metrics</b> card declares a same-origin refresh
			endpoint and a poll interval, so <code>juneau-cards.js</code> wires its built-in refresh button and an
			auto-refresh loop (watch the "As of" field and the staleness chip). Acknowledge or escalate an alert on
			the <a href="/">main page</a>, then refresh this card to see the counts move.</p>
			%s
			<script src="%s"></script>
			<script src="%s"></script>
			</body>
			</html>
			""".formatted(
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
				gridMarkup,
				// Load order: the icon registry first (the refresh button's glyph is resolved from it), then cards.
				ViewsMixin.viewAssetUrl(req, ViewsMixin.ICONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.CARDS_JS_PATH));
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
		var tableMarkup = Html.of(ViewTable.of(req, overviewView(), ALERTS));
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
			breakdown, all painted once on the server from the same alert rows the table lists. It is display-only
			&mdash; no tile is clickable and nothing refreshes, unlike the
			<a href="dashboard">live card dashboard</a>.</p>
			<p>The <b>Status</b> column is a <b>display-only</b> pill with an explicit tone, and expanding a row shows
			the same chip again as a <b>fill-sink</b> pill. Neither is keyboard-actionable; contrast them with the
			action-bound pill on the <a href="./">Alerts tab</a>, which dispatches a row action on click or
			Enter/Space. Tones on the strip and on the pills come from one palette:
			<code>info</code>, <code>success</code>, <code>warning</code>, <code>error</code>, <code>neutral</code>.</p>
			%s
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			<script src="%s"></script>
			</body>
			</html>
			""".formatted(
				DataTablesMixin.DATATABLES_CSS_CDN_URL,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
				tableMarkup,
				DataTablesMixin.JQUERY_CDN_URL,
				DataTablesMixin.DATATABLES_JS_CDN_URL,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.RENDERS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.ICONS_JS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_JS_PATH));
		return HttpResourceBean.of(
			ByteArrayBody.of(html.getBytes(UTF_8), MEDIA_HTML),
			list(ContentType.of(MEDIA_HTML)));
	}

	/**
	 * [GET /flagged] &mdash; a standalone {@link ViewTable} whose column title resolves a {@code $FV} server value.
	 *
	 * <p>
	 * Uses {@link ViewTable#of(RestRequest, ViewDef)} (the {@code ViewDef} host path) so the declared
	 * {@code $FV{flaggedCount}} chrome is resolved against a per-response sibling session; the standalone
	 * {@code ViewTable.of(view)} / {@link PageTable} paths intentionally do not resolve {@code $FV}.
	 *
	 * @param req The current request, whose var resolver knows {@code $FV}.
	 * @return The rendered standalone view.
	 */
	@RestGet(path="/flagged", swagger=@OpSwagger(ignore=true))
	public HttpResource flagged(RestRequest req) {
		var markup = Html.of(ViewTable.of(req, flaggedView(), ACTIVE_WIDGETS));
		return HttpResourceBean.of(
			ByteArrayBody.of(markup.getBytes(UTF_8), MEDIA_HTML),
			list(ContentType.of(MEDIA_HTML)));
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
	 * [GET /data/alerts] &mdash; the Alerts tab's rows.
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
	 * {@code insertDialogBarSlot} the moment the dialog opens &mdash; unlike {@link PageDef#barSlot}/
	 * {@link RowDetailDef#barSlot}, there is no server-rendered pass to ride into.
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
	 * [GET /data/cards/summary] &mdash; the Live Alert Metrics card's refresh envelope: live open/acknowledged/
	 * escalated counts (moved by the {@code ack}/{@code esc} endpoints above) plus a server timestamp.  The field
	 * keys match the {@link CardField#data} keys the {@link #dashboardGrid() dashboard} declares.
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
	// Row generation - enough rows per panel that a column-sizing regression would be visibly wrong.
	//------------------------------------------------------------------------------------------------------------------

	/** Card refresh envelope: {@link CardFieldList#CONTRACT_VERSION} + a data-only field map (no table columns). */
	private static Map<String,Object> cardEnvelope(Map<String,?> fields) {
		var out = new LinkedHashMap<String,Object>();
		out.put("contractVersion", CardFieldList.CONTRACT_VERSION);
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
