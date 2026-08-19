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
import org.apache.juneau.marshall.marshaller.Html;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.DataTablesMixin;
import org.apache.juneau.rest.server.servlet.BasicRestServlet;
import org.apache.juneau.rest.server.views.RowClassRule.Op;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.views.ViewDef.DetailDef;
import org.apache.juneau.rest.server.views.ViewDef.Dir;
import org.apache.juneau.rest.server.views.*;

/**
 * Demonstrates a {@link PageDef} with a sub-tabbed tab ("Catalog" &rarr; Active/Archived) alongside a sibling plain
 * tab ("Audit Log"), served on an embedded Jetty server via {@link ExampleViewsServer}.
 *
 * <p>
 * This is a real caller of {@link PageDef}'s {@link Tab#subtabs(Subtab...) subtabs} outside the views module's own
 * test sources &mdash; see the class-level scope notes below for what it deliberately exercises.
 *
 * <h5 class='section'>What this dogfoods (beyond the bare sub-tab requirement):</h5>
 * <ul>
 * 	<li>The "Active" sub-tab additionally declares {@link ViewDef#poll(long) poll} and
 * 		{@link ViewDef#details(DetailDef...) details}, plus a ribbon and a {@code rowClassRule}, so this one view
 * 		exercises most of the toolkit's declarative surface in one place.
 * 	<li>The "Archived" sub-tab and the "Audit Log" tab are deliberately PLAIN (no ribbon/poll/details), both to
 * 		satisfy the "at least one sibling plain tab" requirement and to keep a contrasting baseline the sub-tabbed
 * 		panel's blank-panel regression would show up against.
 * 	<li>Two distinct row types ({@link Widget}, {@link AuditEntry}) are composed into one page, rather than one
 * 		type reused everywhere.
 * 	<li>Every view uses {@link DataMode#CLIENT} for simplicity (a static in-memory row list, no
 * 		{@code ProtocolQueryable}/{@code QueryableSettings} wiring) &mdash; {@code SERVER} mode is already covered
 * 		end-to-end by {@code ViewServerWiring_Test} in the views module itself, so this example does not repeat it.
 * 	<li>Each sub-tab/tab carries enough rows (see {@link #buildActiveWidgets()}/{@link #buildArchivedWidgets()}/
 * 		{@link #buildAuditLog()}) that a column-sizing regression from the eager-init defect would be visibly wrong,
 * 		not just theoretically present.
 * </ul>
 *
 * <p>
 * Every panel is a {@link ViewDef} table; non-table prose or form panels are outside this example's scope.
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

	private static final List<Widget> ACTIVE_WIDGETS = buildActiveWidgets();
	private static final List<Widget> ARCHIVED_WIDGETS = buildArchivedWidgets();
	private static final List<AuditEntry> AUDIT_LOG = buildAuditLog();

	//------------------------------------------------------------------------------------------------------------------
	// The composed page: Catalog (sub-tabbed: Active/Archived) + a sibling plain Audit Log tab.
	//------------------------------------------------------------------------------------------------------------------

	static PageDef page() {
		return PageDef.create(PAGE_ID)
			.title("Juneau Views Example")
			.tabs(
				Tab.create("catalog", "Catalog").subtabs(
					Subtab.create("active", "Active").view(activeView()),
					Subtab.create(VALUE_ARCHIVED, "Archived").view(archivedView())),
				Tab.create("audit", "Audit Log").view(auditView()))
			.build();
	}

	/** The sub-tabbed tab's first (default) panel &mdash; dogfoods poll/details/ribbon/rowClassRule together. */
	static ViewDef activeView() {
		return ViewDef.create("widgets-active")
			.rowType(Widget.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/widgets/active")
			.defaultOrder("name", Dir.ASC)
			.columns(
				Column.of("name").title("Name"),
				Column.of(COL_STATUS).title("Status").render("tag:status"),
				Column.of(COL_OWNER).title(TITLE_OWNER),
				Column.of(COL_UPDATED_AT).title("Updated").render("date"))
			.ribbon(
				RibbonAction.columnSearchToggle(),
				RibbonAction.refresh())
			.rowClassRule(COL_STATUS, Op.EQ, STATUS_ERROR, "row-flagged")
			// Use 10s for the per-table poll (well above the 5s floor) so the
			// staleness chip's "Xs ago" advance is easy to observe without hammering this demo endpoint.
			.poll(10_000L)
			// "notes" is intentionally not a table column; the expander is the only place it appears.
			.details(
				DetailDef.of(COL_OWNER).title(TITLE_OWNER),
				DetailDef.of(COL_UPDATED_AT).title("Last updated"),
				DetailDef.of("notes").title("Notes"))
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
				Column.of("name").title("Name"),
				Column.of(COL_STATUS).title("Status").render("tag:status"),
				Column.of(COL_OWNER).title(TITLE_OWNER),
				Column.of(COL_UPDATED_AT).title("Archived").render("date"))
			.build();
	}

	/** The sibling PLAIN leaf tab (no sub-tabs) - the contrast case the blank-panel regression needs. */
	static ViewDef auditView() {
		return ViewDef.create("audit-log")
			.rowType(AuditEntry.class)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/audit")
			.defaultOrder("timestamp", Dir.DESC)
			.columns(
				Column.of("timestamp").title("When").render("date"),
				Column.of("actor").title("Actor"),
				Column.of("action").title("Action"))
			.build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// HTML page (hand-built, no template engine - this module takes no dependency on FreeMarker/console-ui).
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * [GET /] &mdash; the composed page: tab bar + sub-tab bar + all three panels, plus the DataTables/jQuery
	 * (caller-provided, per ASF category-A discipline) and first-party toolkit asset links a real page needs.
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}
	 * 	so the head links stay correct however this example is mounted.
	 * @return The full HTML page.
	 */
	@RestGet(path="/", summary="The Catalog (Active/Archived sub-tabs) + Audit Log demo page")
	public HttpResource index(RestRequest req) {
		var pageMarkup = Html.of(PageTable.of(page()));
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
			(<b>Active</b>/<b>Archived</b>), and <b>Audit Log</b> is a sibling plain leaf tab. The Active sub-tab
			also declares a poll interval (watch the staleness chip) and a row-details expander (click any row).</p>
			<p><a href="%s">Deep link straight to the Archived sub-tab</a> (exercises
			<code>juneau-pages.js</code>'s hash-routing on load, not just via the tab bar's own links).</p>
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
			ByteArrayBody.of(html.getBytes(UTF_8), "text/html;charset=utf-8"),
			list(ContentType.of("text/html;charset=utf-8")));
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

	//------------------------------------------------------------------------------------------------------------------
	// Row generation - enough rows per panel that a column-sizing regression would be visibly wrong.
	//------------------------------------------------------------------------------------------------------------------

	private static List<Widget> buildActiveWidgets() {
		var out = new ArrayList<Widget>();
		var owners = List.of("Platform", "Storefront", "Billing", "Growth");
		for (var i = 1; i <= 30; i++) {
			// Every 7th widget is "error" (rowClassRule target); the rest alternate active/active/active for a
			// mostly-healthy-looking table with a few flagged rows scattered through it.
			var status = i % 7 == 0 ? STATUS_ERROR : "active";
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
}
