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

import static org.apache.juneau.rest.server.views.ConsoleBrowserFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The dark color scheme in a real browser: the pre-paint script settles the scheme before the first frame, switching
 * fetches no stylesheet, and every console surface meets WCAG contrast under dark (stock {@code open} palette) and under
 * light (each of the five stock palettes), links and focus rings meet it under dark in each stock palette, and chart
 * colours meet it.  The measured exceptions are pinned at their ratios in {@code DARK_ALLOWED} and
 * {@code LIGHT_ALLOWED}.
 */
@SuppressWarnings({
	"unchecked" // The probe report is parsed into raw Maps and Lists.
})
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-rest-server-views -am test` from the repo root")
class DarkScheme_BrowserTest extends TestBase {

	private static final String CONFIG_CSS = "/views/juneau-config.css";
	private static final String CALENDAR_CSS = "/views/juneau-calendar.css";
	private static final String CHARTS = "/juneau-console/juneau-charts.js";
	private static final String ECHARTS = "/webjars/echarts/6.1.0/dist/echarts.min.js";
	private static final String ECHARTS_RESOURCE = "/META-INF/resources" + ECHARTS;
	/** The DataTables vendor stylesheet the views toolkit links, as a WebJar file path. */
	private static final String DT_CSS_FILE = WebJarResolver.resolvePath(WebJarResolver.asset("org.webjars.npm", "datatables.net-dt", "datatables.net-dt/{version}/css/dataTables.dataTables.min.css"));
	private static final String DT_CSS = "/webjars/" + DT_CSS_FILE;
	private static final List<String> PALETTES = List.of("open", "gray", "light-brown", "light-red", "red");

	private static final String TOGGLE = "data-jc-scheme-toggle";
	private static final String PIN_DARK = "data-jc-scheme=\"dark\" data-jc-scheme-pin=\"dark\"";
	private static final String PIN_LIGHT = "data-jc-scheme=\"light\" data-jc-scheme-pin=\"light\"";

	/** The dark chrome background, which is the html and body background. */
	private static final String DARK_CHROME_BG = "rgb(31, 34, 40)";

	/** The light chrome background of the stock {@code open} palette ({@code --jc-chrome-bg} #f3f2f2). */
	private static final String LIGHT_CHROME_BG = "rgb(243, 242, 242)";

	/** The light control border, {@code --jc-border-2} in the open palette. */
	private static final String LIGHT_CONTROL_BORDER = "#888c90";

	private static final String SIMPLE = """
		{"contractVersion":"1","title":"S","header":{"title":"T","links":[{"id":"l","label":"Docs","href":"/docs"}]},"cards":[]}
		""";

	/** Publishes the root scheme lazily, so the probe reads it after the steps. */
	private static final String SWITCH_PROBE = """
		<script>
		window.__probe = { toJSON: function () { return { scheme: document.documentElement.getAttribute('data-jc-scheme') }; } };
		</script>
		""";

	private static final String CLICK_DARK = """
		document.querySelector('.jc-scheme-toggle [data-scheme=dark]').click();
		""";

	/**
	 * Colours the probe cannot parse, as a background, a gradient stop and a foreground: each must be reported as an
	 * error, never measured as if it were transparent.
	 */
	private static final String UNPARSEABLE = """
		<!DOCTYPE html>
		<html lang="en"><head><meta charset="utf-8"><link rel="icon" href="data:,"></head><body>
		<div style="background-color: color(display-p3 1 1 1)"><span class="probe-p3-bg" style="color: #000">On a wide-gamut background</span></div>
		<div style="background-image: linear-gradient(lab(90 0 0), lab(95 0 0)); background-color: transparent"><span class="probe-lab-stop" style="color: #000">On a lab gradient</span></div>
		<span class="probe-p3-fg" style="color: color(display-p3 0 0 0)">Wide-gamut ink</span>
		</body></html>
		""";

	/** The table card's rows: enough for a second page, so the paging pill has a disabled control. */
	private static String rows() {
		var sb = new StringBuilder("[");
		for (var i = 1; i <= 30; i++)
			sb.append(i == 1 ? "" : ",").append("{\"id\":\"").append(i).append("\",\"pod\":\"pod-").append(i).append("\",\"cause\":\"disk\"}");
		return sb.append(']').toString();
	}

	/** The surface page's contract: a header with links, a View Settings table card, the surface kit card, and optionally a chart. */
	private static String contract(boolean chart) {
		var table = TableSpec.create("tbl").dataUrl("/rest/tbl/data").columns(Column.create("pod").label("Pod"), Column.create("cause").label("Cause")).columnConfig(true);
		var cards = new ArrayList<Map<String,Object>>();
		cards.add(new LinkedHashMap<>(Map.of("id", "tbl", "type", "datatables", "title", "Pods", "table", table.toCardBody())));
		cards.add(new LinkedHashMap<>(Map.of("id", "kit", "type", "html", "template", "kit", "title", "Kit")));
		if (chart)
			cards.add((Map<String,Object>)Json.to(CHART_CARD, Map.class));
		var c = new LinkedHashMap<String,Object>();
		c.put("contractVersion", "1");
		c.put("title", "Surfaces");
		c.put("header", Map.of("title", "Console", "links", List.of(Map.of("id", "docs", "label", "Docs", "href", "/docs"))));
		c.put("cards", cards);
		return Json.of(c);
	}

	private static final String CHART_CARD = """
		{"id":"ch","type":"chart","title":"Trend",
		 "helper":{"kind":"line","x":"k","series":[{"field":"s1"},{"field":"s2"},{"field":"s3"},{"field":"s4"},{"field":"s5"},{"field":"s6"},{"field":"s7"},{"field":"s8"}]},
		 "data":[{"k":"a","s1":1,"s2":2,"s3":3,"s4":4,"s5":5,"s6":6,"s7":7,"s8":8},{"k":"b","s1":2,"s2":3,"s3":4,"s4":5,"s5":6,"s6":7,"s7":8,"s8":9}]}
		""";

	/**
	 * Publishes the chart's resolved colours lazily: the option's palette and per-series colours, the axis label,
	 * legend and tooltip colours, and the computed {@code --jc-surface} they sit on.
	 */
	private static final String CHART_PROBE = """
		<script>
		window.__probe = { toJSON: function () {
			var api = window.JuneauConsole.cardApi('ch'), c = api && api.chart();
			if (!c) return { chart: false };
			var o = c.getOption(), first = function (v) { return Array.isArray(v) ? v[0] : v; };
			var tip = first(o.tooltip) || {}, legend = first(o.legend) || {};
			return {
				chart: true,
				surface: getComputedStyle(document.documentElement).getPropertyValue('--jc-surface').trim(),
				palette: o.color,
				series: o.series.map(function (s) { return (s.itemStyle && s.itemStyle.color) || (s.lineStyle && s.lineStyle.color) || null; }),
				xAxisLabel: first(o.xAxis).axisLabel.color,
				yAxisLabel: first(o.yAxis).axisLabel.color,
				legendText: legend.textStyle ? legend.textStyle.color : null,
				tooltipText: tip.textStyle ? tip.textStyle.color : null,
				tooltipBg: tip.backgroundColor || null
			};
		} };
		</script>
		""";

	/**
	 * One of each static surface, styled only by the shipped stylesheets: card status and field labels, a link,
	 * status pills and tags, probes, header badges, the floating overlays the views pack positions itself (tip,
	 * timestamp popup, cell popover, bulk toast), and calendar event chips with the overflow popover.  The overlays
	 * are made visible inline; their colours come from the stylesheets.
	 */
	private static final String KIT = """
		<template data-card="kit">
		<div class="probe-kit">
		<span class="juneau-view-card-status">Updated 2m ago</span>
		<span class="juneau-view-card-status" data-state="error">Failed</span>
		<dl class="juneau-view-card-fields"><div class="juneau-view-card-field"><dt>Owner</dt><dd>Pat</dd></div></dl>
		<p class="probe-muted" style="color: var(--jc-text-muted)">Muted helper text</p>
		<p class="probe-soft" style="color: var(--jc-text-soft)">Soft text</p>
		<a class="probe-link" href="#x">A card link</a>
		<div class="jc-prose"><pre><a class="probe-chrome-bg-link" href="#p">A link on the chrome background</a></pre><pre><code>Code on the chrome background</code></pre></div>
		<span class="jc-pill tag status released"><span class="jc-pill-dot"></span>Released</span>
		<span class="jc-pill tag status rc"><span class="jc-pill-dot"></span>RC</span>
		<span class="jc-pill tag status pending"><span class="jc-pill-dot"></span>Pending</span>
		<span class="jc-pill tag status unknown"><span class="jc-pill-dot"></span>Unknown</span>
		<span class="jc-pill tag status failed"><span class="jc-pill-dot"></span>Failed</span>
		<span class="jc-badge" data-juneau-badge-tone="info">Info</span>
		<span class="jc-badge" data-juneau-badge-tone="success">Ok</span>
		<span class="jc-badge" data-juneau-badge-tone="warning">Warn</span>
		<span class="jc-badge" data-juneau-badge-tone="error">Err</span>
		<span class="jc-badge" data-juneau-badge-tone="neutral">Idle</span>
		<div class="jc-probe-group" role="radiogroup" aria-label="Status">
		<span class="jc-probe jc-probe-success" role="radio" aria-checked="true" tabindex="0"><span class="jc-probe-dot"></span>Up</span>
		<span class="jc-probe jc-probe-error" role="radio" aria-checked="false" tabindex="-1"><span class="jc-probe-dot"></span>Down</span>
		<span class="jc-probe jc-probe-warning" role="radio" aria-checked="false" tabindex="-1"><span class="jc-probe-dot"></span>Slow</span>
		<span class="jc-probe jc-probe-neutral" role="radio" aria-checked="false" tabindex="-1"><span class="jc-probe-dot"></span>Off</span>
		</div>
		<div class="jc-cal">
		<a class="jc-cal-event jc-cal-cat--blue" href="#e1">Standup</a>
		<a class="jc-cal-event jc-cal-cat--green" href="#e2">Release</a>
		<a class="jc-cal-event jc-cal-cat--amber" href="#e3">Freeze</a>
		<a class="jc-cal-event jc-cal-cat--red" href="#e4">Outage</a>
		<a class="jc-cal-event jc-cal-cat--neutral" href="#e5">Holiday</a>
		<div class="jc-cal-popover" style="position: static"><a class="jc-cal-event jc-cal-cat--blue" href="#e6">Review</a></div>
		</div>
		</div>
		</template>
		""";

	/**
	 * Overlays the views pack appends to the body itself; visible here so their computed colours apply.  The dialog
	 * Close button only appears once a submitted action returns a receipt, so it stands in here as static markup.
	 */
	private static final String OVERLAYS = """
		<div class="jc-tip" style="display: block; left: 10px; top: 10px">Cursor tip</div>
		<div class="juneau-ts-popup" style="display: block; left: 10px; top: 60px">2026-10-13 10:00 local</div>
		<div class="jc-cell-popover" style="display: block; left: 10px; top: 110px">
		<div class="jc-cell-popover-title">Pod</div>
		<div class="jc-cell-popover-row"><span class="jc-cell-popover-label">Cause</span><span class="jc-cell-popover-value">disk</span></div>
		</div>
		<div class="juneau-view-bulk-toast juneau-view-bulk-toast-status" role="status"><span class="probe-bulk-text">3 rows updated</span>
		<button type="button" class="juneau-view-bulk-toast-close">Dismiss</button></div>
		<div class="juneau-view-dialog-actions"><button type="button" class="juneau-view-dialog-close">Close</button></div>
		<a class="probe-chrome-link" href="#c">A link on the page background</a>
		""";

	/** Opens the modal confirm and the error toast once the table has painted. */
	private static final String OPEN_OVERLAYS = """
		window.JuneauViews.dialogs.confirm({ title: 'Delete pod?', body: 'This cannot be undone.' });
		window.JuneauViews.helpers.toast('Save failed', { tone: 'error', timeoutMs: 0 });
		""";

	private static Map<String,Map<String,Object>> report;

	private static final double TEXT = 4.5;
	private static final double NON_TEXT = 3;

	/** An app's own dark accent block with a deliberately low-contrast link: reported, never failed. */
	private static final String APP_LOW_LINK = "<style>html:root[data-jc-scheme=\"dark\"] { --jc-link: #4a5a7a; }</style>\n";

	/** The petstore demo's dark link colour as an app override: reported, never failed. */
	private static final String APP_PETSTORE_LINK = "<style>html:root[data-jc-scheme=\"dark\"] { --jc-link: #d9a65c; }</style>\n";

	/** Text surfaces: {name, kind, selector}, each held to 4.5:1. */
	private static final String[][] TEXT_SURFACES = {
		{"brandTitle", "text", ".jc-brand-title"},
		{"headerLink", "text", ".jc-header-link"},
		{"cardTitle", "text", ".jc-card-title"},
		{"tableHead", "text", "#tbl-body thead .dt-column-title"},
		{"tableRowOdd", "text", "#tbl-body tbody tr:nth-child(odd) td"},
		{"tableRowEven", "text", "#tbl-body tbody tr:nth-child(even) td"},
		{"filterText", "text", "#tbl-body .dt-search input"},
		{"pagingInfo", "text", ".juneau-view-pagingpill-info"},
		{"cardStatus", "text", ".juneau-view-card-status:not([data-state])"},
		{"cardStatusError", "text", ".juneau-view-card-status[data-state=\"error\"]"},
		{"fieldLabel", "text", ".juneau-view-card-field dt"},
		{"fieldValue", "text", ".juneau-view-card-field dd"},
		{"mutedText", "text", ".probe-muted"},
		{"softText", "text", ".probe-soft"},
		{"cardLink", "text", ".probe-link"},
		{"pageLink", "text", ".probe-chrome-link"},
		{"chromeBgLink", "text", ".probe-chrome-bg-link"},
		{"preCode", "text", ".jc-prose pre code"},
		{"pillReleased", "text", ".jc-pill.status.released"},
		{"pillRc", "text", ".jc-pill.status.rc"},
		{"pillPending", "text", ".jc-pill.status.pending"},
		{"pillUnknown", "text", ".jc-pill.status.unknown"},
		{"pillFailed", "text", ".jc-pill.status.failed"},
		{"badgeInfo", "text", ".jc-badge[data-juneau-badge-tone=\"info\"]"},
		{"badgeSuccess", "text", ".jc-badge[data-juneau-badge-tone=\"success\"]"},
		{"badgeWarning", "text", ".jc-badge[data-juneau-badge-tone=\"warning\"]"},
		{"badgeError", "text", ".jc-badge[data-juneau-badge-tone=\"error\"]"},
		{"badgeNeutral", "text", ".jc-badge[data-juneau-badge-tone=\"neutral\"]"},
		{"probeSuccess", "text", ".jc-probe-success"},
		{"probeError", "text", ".jc-probe-error"},
		{"probeWarning", "text", ".jc-probe-warning"},
		{"probeNeutral", "text", ".jc-probe-neutral"},
		{"cursorTip", "text", ".jc-tip"},
		{"tsPopup", "text", ".juneau-ts-popup"},
		{"popoverTitle", "text", ".jc-cell-popover-title"},
		{"popoverLabel", "text", ".jc-cell-popover-label"},
		{"popoverValue", "text", ".jc-cell-popover-value"},
		{"bulkToast", "text", ".probe-bulk-text"},
		{"bulkToastClose", "text", ".juneau-view-bulk-toast-close"},
		{"dialogTitle", "text", ".juneau-view-dialog .juneau-view-dialog-title"},
		{"dialogBody", "text", ".juneau-view-dialog > p"},
		{"dialogCancel", "text", ".juneau-view-dialog-cancel"},
		{"dialogConfirm", "text", ".juneau-view-dialog-confirm"},
		{"dialogClose", "text", ".juneau-view-dialog-close"},
		{"configTitle", "text", ".juneau-config-title"},
		{"configTab", "text", ".juneau-config-tab"},
		{"configColName", "text", ".juneau-config-col-name"},
		{"configButton", "text", ".juneau-config-apply, .juneau-config-reset"},
		{"configSelect", "text", ".juneau-config-view-select"},
		{"configInput", "text", ".juneau-config-col-label"},
		{"errorToast", "text", ".jc-toast.is-error"},
		{"toggleChecked", "text", ".jc-scheme-toggle button[aria-checked=\"true\"]"},
		{"toggleUnchecked", "text", ".jc-scheme-toggle button[aria-checked=\"false\"]"},
		{"calBlue", "text", ".jc-cal > .jc-cal-event.jc-cal-cat--blue"},
		{"calGreen", "text", ".jc-cal > .jc-cal-event.jc-cal-cat--green"},
		{"calAmber", "text", ".jc-cal > .jc-cal-event.jc-cal-cat--amber"},
		{"calRed", "text", ".jc-cal > .jc-cal-event.jc-cal-cat--red"},
		{"calNeutral", "text", ".jc-cal > .jc-cal-event.jc-cal-cat--neutral"},
		{"calPopover", "text", ".jc-cal-popover .jc-cal-event"},
	};

	/** Non-text surfaces (control borders, icon glyphs, placeholders, focus rings), each held to 3:1. */
	private static final String[][] NON_TEXT_SURFACES = {
		{"filterBorder", "border", "#tbl-body .dt-search input"},
		{"filterPlaceholder", "placeholder", "#tbl-body .dt-search input"},
		{"configInputBorder", "border", ".juneau-config-col-label"},
		{"configSelectBorder", "border", ".juneau-config-view-select"},
		{"configButtonBorder", "border", ".juneau-config-apply, .juneau-config-reset"},
		{"ribbonIcon", "text", ".juneau-view-ribbon-btn"},
		{"pagingMenuIcon", "text", ".juneau-view-pagingpill-menubtn"},
		{"pagingButtonIcon", "text", ".juneau-view-pagingpill-btn:not([disabled])"},
		{"focusCardLink", "outline", ".probe-link"},
		{"focusHeaderLink", "outline", ".jc-header-link"},
		{"focusPageLink", "outline", ".probe-chrome-link"},
		{"focusRibbon", "outline", ".juneau-view-ribbon-btn"},
		{"focusToggle", "outline", ".jc-scheme-toggle button[aria-checked=\"true\"]"},
		{"focusFilter", "focus-border", "#tbl-body .dt-search input"},
	};

	/** The open column-search popover's input: its focused border must be a visible focus indicator. */
	private static final String[][] COL_SEARCH_SURFACES = {
		{"focusColSearch", "focus-border", ".juneau-view-col-search-popover-input"},
	};

	/** The unparseable-colour page's surfaces. */
	private static final String[][] UNPARSEABLE_SURFACES = {
		{"p3Background", "text", ".probe-p3-bg"},
		{"labStop", "text", ".probe-lab-stop"},
		{"p3Foreground", "text", ".probe-p3-fg"},
	};

	/** Disabled controls: exempt, measured and reported only. */
	private static final String[][] DISABLED_SURFACES = {
		{"pagingButtonDisabled", "text", ".juneau-view-pagingpill-btn[disabled]"},
		{"configMoveDisabled", "text", ".juneau-config-col-move[disabled]"},
	};

	/**
	 * Links on the card, in the header, on the page background (the gradient over the chrome) and on the plain
	 * {@code --jc-chrome-bg} (a prose code block), and focus rings, for each stock palette.
	 */
	private static final String[][] ACCENT_TEXT = {
		{"headerLink", "text", ".jc-header-link"},
		{"cardLink", "text", ".probe-link"},
		{"pageLink", "text", ".probe-chrome-link"},
		{"chromeBgLink", "text", ".probe-chrome-bg-link"},
		{"preCode", "text", ".jc-prose pre code"},
	};
	private static final String[][] ACCENT_FOCUS = {
		{"focusCardLink", "outline", ".probe-link"},
		{"focusHeaderLink", "outline", ".jc-header-link"},
		{"focusPageLink", "outline", ".probe-chrome-link"},
		{"focusToggle", "outline", ".jc-scheme-toggle button[aria-checked=\"true\"]"},
	};

	/**
	 * Measured dark failures, by surface name, each with the ratio it measured.  A listed surface must keep measuring
	 * that ratio (so a change, better or worse, is noticed); every other surface must meet its threshold.  Empty: every
	 * dark surface meets its threshold.
	 */
	private static final Map<String,Double> DARK_ALLOWED = Map.of();

	/**
	 * Measured light exceptions, by surface name: only {@code filterPlaceholder}, the toolbar search placeholder
	 * {@code --jc-placeholder} #b4b5b6 on white, 2.05.
	 */
	private static final Map<String,Double> LIGHT_ALLOWED = Map.of(
		"filterPlaceholder", 2.05
	);

	/**
	 * The surface page under {@code htmlAttrs}, with an optional inline style block and an optional chart card.  The
	 * DataTables vendor stylesheet sits where the toolkit emits it: after the theme, before the views stylesheet.
	 */
	private static String surfacePage(String htmlAttrs, String style, boolean chart) throws Exception {
		var head = "<link rel=\"stylesheet\" href=\"" + CONFIG_CSS + "\">\n<link rel=\"stylesheet\" href=\"" + CALENDAR_CSS + "\">\n" + style;
		var before = chart ? "<script src=\"" + ECHARTS + "\"></script>\n" + CHART_PROBE : "";
		var after = viewsPack() + (chart ? "<script src=\"" + CHARTS + "\"></script>\n" : "") + OVERLAYS;
		var views = "<link rel=\"stylesheet\" href=\"" + VIEWS_CSS + "\">";
		var page = contractPage(contract(chart), KIT, head + before, after, htmlAttrs, true);
		assertTrue(page.contains(views), "the views stylesheet is linked");
		return page.replace(views, "<link rel=\"stylesheet\" href=\"" + DT_CSS + "\">\n" + views);
	}

	@BeforeAll
	static void run() throws Exception {
		var f = ConsoleBrowserFixture.create("dark-scheme")
			.asset(CONFIG_CSS, resource(ViewsMixin.CONFIG_CSS_RESOURCE), "text/css")
			.asset(CALENDAR_CSS, resource(ViewsMixin.CALENDAR_CSS_RESOURCE), "text/css")
			.asset(DT_CSS, resource("/" + WebJarResolver.WEBJARS_ROOT + DT_CSS_FILE), "text/css")
			.asset(CHARTS, resource(ConsoleChromeMixin.CHARTS_JS_RESOURCE), "text/javascript")
			.asset(ECHARTS, resource(ECHARTS_RESOURCE), "text/javascript")
			.asset("/rest/tbl/data", rows(), "application/json");
		for (var p : PALETTES)
			f.asset(theme(p), resource("/org/apache/juneau/console/juneau-theme-" + p + ".css"), "text/css");

		f.page("n01", ORIGIN + "/n01", contractPage(SIMPLE, "", "", "", "", true)).colorScheme("dark").firstFrameProbe();
		f.page("n02", ORIGIN + "/n02", contractPage(SIMPLE, "", "", "", TOGGLE, true)).colorScheme("light")
			.storage(Map.of("juneau-console:scheme", "dark")).firstFrameProbe();
		f.page("n03", ORIGIN + "/n03", contractPage(SIMPLE, "", "", "", PIN_LIGHT, true)).colorScheme("dark").firstFrameProbe();
		f.page("n04", ORIGIN + "/n04", contractPage(SIMPLE, "", SWITCH_PROBE, "", TOGGLE, true)).colorScheme("light")
			.waitFor(".jc-scheme-toggle").watchRequests().eval(CLICK_DARK);
		f.page("unparseable", ORIGIN + "/unparseable", UNPARSEABLE);
		probes(f, UNPARSEABLE_SURFACES);

		for (var scheme : List.of("dark", "light")) {
			surface(f, scheme, surfacePage((scheme.equals("dark") ? PIN_DARK : PIN_LIGHT) + " " + TOGGLE, "", false));
			f.click(".juneau-config-chooser-btn").eval(OPEN_OVERLAYS);
			probes(f, TEXT_SURFACES, NON_TEXT_SURFACES, DISABLED_SURFACES);
		}
		for (var scheme : List.of("dark", "light")) {
			surface(f, "colsearch-" + scheme, surfacePage((scheme.equals("dark") ? PIN_DARK : PIN_LIGHT) + " " + TOGGLE, "", false));
			f.click(".juneau-view-col-search-icon");
			probes(f, COL_SEARCH_SURFACES);
		}
		for (var p : PALETTES) {
			if (! p.equals("open")) {
				surface(f, lightCase(p), surfacePage(PIN_LIGHT + " " + TOGGLE, "", false).replace(THEME_CSS, theme(p)));
				f.click(".juneau-config-chooser-btn").eval(OPEN_OVERLAYS);
				probes(f, TEXT_SURFACES, NON_TEXT_SURFACES, DISABLED_SURFACES);
			}
			surface(f, "palette-" + p, surfacePage(PIN_DARK + " " + TOGGLE, "", false).replace(THEME_CSS, theme(p)));
			probes(f, ACCENT_TEXT, ACCENT_FOCUS);
		}
		surface(f, "app-low", surfacePage(PIN_DARK + " " + TOGGLE, APP_LOW_LINK, false));
		probes(f, ACCENT_TEXT);
		surface(f, "app-petstore", surfacePage(PIN_DARK + " " + TOGGLE, APP_PETSTORE_LINK, false));
		probes(f, ACCENT_TEXT);
		f.page("chart", ORIGIN + "/chart", surfacePage(PIN_DARK + " " + TOGGLE, "", true)).waitFor("#ch canvas");
		report = f.run();
	}

	/** The light-scheme case for {@code palette}; the stock {@code open} palette is the {@code light} case. */
	private static String lightCase(String palette) {
		return palette.equals("open") ? "light" : "light-palette-" + palette;
	}

	private static String theme(String palette) {
		return "/juneau-console/juneau-theme-" + palette + ".css";
	}

	/** Adds a surface case that waits for the table card to paint. */
	private static void surface(ConsoleBrowserFixture f, String name, String html) {
		f.page(name, ORIGIN + "/" + name, html).waitFor("#tbl-body tbody tr");
	}

	private static void probes(ConsoleBrowserFixture f, String[][]... sets) {
		for (var set : sets)
			for (var e : set)
				f.probeContrast(e[0], e[1], e[2]);
	}

	/** The worst measurement of {@code name} in {@code caseName}, or null when nothing rendered matched. */
	private static Map<String,Object> worst(String caseName, String name) {
		var c = contrast(report.get(caseName));
		assertNotNull(c, () -> caseName + ": no contrast report: " + report.get(caseName));
		var e = c.get(name);
		return e == null ? null : (Map<String,Object>)e.get("worst");
	}

	/** The colours the probe could not parse for {@code name} in {@code caseName}; empty when there were none. */
	private static List<String> errors(String caseName, String name) {
		var e = contrast(report.get(caseName)).get(name);
		var l = e == null ? null : (List<String>)e.get("errors");
		return l == null ? List.of() : l;
	}

	/**
	 * Every surface in {@code caseName} rendered, painted a colour, and met {@code min}; an allowlisted surface instead
	 * measured exactly its recorded ratio.
	 */
	private static void assertContrast(String caseName, String[][] surfaces, double min, Map<String,Double> allowed) {
		var bad = new ArrayList<String>();
		for (var s : surfaces) {
			var errors = errors(caseName, s[0]);
			if (! errors.isEmpty()) {
				bad.add(s[0] + " (" + s[2] + "): " + errors);
				continue;
			}
			var w = worst(caseName, s[0]);
			if (w == null) {
				bad.add(s[0] + " (" + s[2] + "): nothing rendered");
				continue;
			}
			var r = (Number)w.get("ratio");
			if (r == null) {
				bad.add(s[0] + " (" + s[2] + "): no " + s[1] + " colour painted: " + w);
				continue;
			}
			var a = allowed.get(s[0]);
			if (a != null) {
				if (Math.abs(r.doubleValue() - a) > 0.005)
					bad.add(s[0] + " (" + s[2] + "): allowlisted at " + a + " but measured " + r + ": " + w);
			} else if (r.doubleValue() < min) {
				bad.add(s[0] + " (" + s[2] + "): " + r + " < " + min + ": " + w);
			}
		}
		assertEquals(List.of(), bad, caseName);
	}

	/** Prints each surface's measurement and returns the lines; never fails on a ratio. */
	private static List<String> reportOnly(String caseName, String[][] surfaces) {
		var lines = new ArrayList<String>();
		for (var s : surfaces) {
			var w = worst(caseName, s[0]);
			var errors = errors(caseName, s[0]);
			lines.add(caseName + " " + s[0] + " (" + s[2] + "): " + (! errors.isEmpty() ? "errors " + errors : w == null ? "nothing rendered" : w.get("fg") + " on " + w.get("bg") + " = " + w.get("ratio")));
		}
		lines.forEach(System.out::println);
		return lines;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No-flash
	//-----------------------------------------------------------------------------------------------------------------

	@Test void n01_osDarkUnpinned() {
		var ff = firstFrame(report.get("n01"));
		assertEquals("dark", ff.get("scheme"), ff::toString);
		assertEquals(DARK_CHROME_BG, ff.get("bodyBg"), ff::toString);
	}

	@Test void n02_storedDarkToggleOsLight() {
		var ff = firstFrame(report.get("n02"));
		assertEquals("dark", ff.get("scheme"), ff::toString);
		assertEquals(DARK_CHROME_BG, ff.get("bodyBg"), ff::toString);
	}

	@Test void n03_pinnedLightOsDark() {
		var ff = firstFrame(report.get("n03"));
		assertEquals("light", ff.get("scheme"), ff::toString);
		assertEquals(LIGHT_CHROME_BG, ff.get("bodyBg"), ff::toString);
	}

	@Test void n04_switchNoFetch() {
		var r = report.get("n04");
		assertNoShellErrors(r);
		var p = probe(r);
		assertEquals("dark", p.get("scheme"), p::toString);
		var log = requestLog(r);
		assertTrue(log.stream().anyMatch(e -> "load".equals(e.get("phase")) && DARK_CSS.equals(e.get("path"))), () -> "dark layer linked up front: " + log);
		var css = log.stream().filter(e -> ! "load".equals(e.get("phase")) && ((String)e.get("path")).endsWith(".css")).toList();
		assertEquals(List.of(), css, "switching to dark requests no stylesheet");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Contrast
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_darkTextAtLeast4_5() {
		assertNoShellErrors(report.get("dark"));
		assertContrast("dark", TEXT_SURFACES, TEXT, DARK_ALLOWED);
	}

	@Test void c02_darkNonTextAtLeast3() {
		assertContrast("dark", NON_TEXT_SURFACES, NON_TEXT, DARK_ALLOWED);
	}

	@Test void c03_disabledReported() {
		var lines = reportOnly("dark", DISABLED_SURFACES);
		assertTrue(lines.stream().noneMatch(l -> l.endsWith("nothing rendered") || l.contains("): errors ")), lines::toString);
	}

	/** Every light stock palette meets the text and non-text targets; every measured pair is logged first, pass or fail. */
	@Test void c04_lightBaseline() throws Exception {
		var failures = new ArrayList<String>();
		var log = new ArrayList<String>();
		for (var p : PALETTES) {
			var c = lightCase(p);
			assertNoShellErrors(report.get(c));
			logContrast(log, p, c, TEXT_SURFACES, TEXT);
			logContrast(log, p, c, NON_TEXT_SURFACES, NON_TEXT);
			for (var set : List.of(TEXT_SURFACES, NON_TEXT_SURFACES)) {
				try {
					assertContrast(c, set, set == TEXT_SURFACES ? TEXT : NON_TEXT, LIGHT_ALLOWED);
				} catch (AssertionError e) {
					failures.add(p + ": " + e.getMessage());
				}
			}
		}
		log.forEach(System.out::println);
		var out = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "light-contrast-table.txt");
		java.nio.file.Files.write(out, log);
		assertEquals(List.of(), failures);
	}

	/** Appends one line per surface: palette, surface, selector, ink on background, ratio and target. */
	private static void logContrast(List<String> log, String palette, String caseName, String[][] surfaces, double min) {
		for (var s : surfaces) {
			var w = worst(caseName, s[0]);
			log.add(palette + " | " + s[0] + " | " + s[2] + " | " + (w == null ? "nothing rendered" : w.get("fg") + " on " + w.get("bg") + " = " + w.get("ratio")) + " | target " + min);
		}
	}

	@Test void c05_stockAccentsAllThemes() {
		for (var p : PALETTES) {
			assertNoShellErrors(report.get("palette-" + p));
			assertContrast("palette-" + p, ACCENT_TEXT, TEXT, Map.of());
			assertContrast("palette-" + p, ACCENT_FOCUS, NON_TEXT, Map.of());
		}
	}

	@Test void c06_chartColours() {
		var r = report.get("chart");
		assertNoShellErrors(r);
		var p = probe(r);
		assertEquals(true, p.get("chart"), p::toString);
		var surface = Wcag.parse((String)p.get("surface"));
		var bad = new ArrayList<String>();
		var palette = (List<String>)p.get("palette");
		assertTrue(palette.size() >= 8, p::toString);
		for (var i = 0; i < 8; i++)
			check(bad, "palette[" + i + "]", palette.get(i), surface, NON_TEXT);
		var series = (List<String>)p.get("series");
		for (var i = 0; i < series.size(); i++)
			check(bad, "series[" + i + "]", series.get(i) == null ? palette.get(i % palette.size()) : series.get(i), surface, NON_TEXT);
		for (var k : List.of("xAxisLabel", "yAxisLabel", "legendText"))
			check(bad, k, (String)p.get(k), surface, TEXT);
		check(bad, "tooltipText", (String)p.get("tooltipText"), Wcag.parse((String)p.get("tooltipBg")), TEXT);
		System.out.println("chart " + p);
		assertEquals(List.of(), bad);
	}

	@Test void c07_appAccentsReportedOnly() {
		var lines = reportOnly("app-low", ACCENT_TEXT);
		assertTrue(lines.stream().noneMatch(l -> l.endsWith("nothing rendered") || l.contains("): errors ")), lines::toString);
		assertEquals("#4a5a7a", worst("app-low", "cardLink").get("fg"), "the app's own link colour is the one measured");
	}

	/** The petstore demo's dark link colour against the card surface and the chrome: reported only. */
	@Test void c08_petstoreLinkReportedOnly() {
		var lines = reportOnly("app-petstore", ACCENT_TEXT);
		assertTrue(lines.stream().noneMatch(l -> l.endsWith("nothing rendered") || l.contains("): errors ")), lines::toString);
		assertEquals("#d9a65c", worst("app-petstore", "cardLink").get("fg"));
		assertEquals("#d9a65c", worst("app-petstore", "chromeBgLink").get("fg"));
	}

	/** A colour the probe cannot parse is an error entry, never a transparent background or a missing ink. */
	@Test void c09_unparseableColourFailsClosed() {
		var c = contrast(report.get("unparseable"));
		assertNotNull(c, () -> "no contrast report: " + report.get("unparseable"));
		for (var s : UNPARSEABLE_SURFACES) {
			var errors = (List<String>)c.get(s[0]).get("errors");
			assertNotNull(errors, () -> s[0] + ": no error reported: " + c.get(s[0]));
			assertEquals(1, errors.size(), () -> s[0] + ": " + errors);
		}
		assertTrue(((List<String>)c.get("p3Background").get("errors")).get(0).contains("background"), c::toString);
		assertTrue(((List<String>)c.get("labStop").get("errors")).get(0).contains("background"), c::toString);
		assertTrue(((List<String>)c.get("p3Foreground").get("errors")).get(0).contains("foreground"), c::toString);
	}

	/** The View Settings dialog's buttons, selects and inputs paint the console's control tokens in both schemes. */
	@Test void c10_configControlsUseConsoleTokens() {
		var bad = new ArrayList<String>();
		for (var e : Map.of("light", new String[] {"#080707", "#ffffff", LIGHT_CONTROL_BORDER}, "dark", new String[] {"#e6e8eb", "#2a2e36", "#7a818c"}).entrySet()) {
			var scheme = e.getKey();
			var t = e.getValue();
			for (var name : List.of("configButton", "configSelect", "configInput"))
				paints(bad, scheme, name, t[0], t[1]);
			for (var name : List.of("configButtonBorder", "configSelectBorder", "configInputBorder"))
				paints(bad, scheme, name, t[2], t[1]);
		}
		assertEquals(List.of(), bad);
	}

	/** The toolbar search's focus border is the focus colour in dark and stays the accent in light. */
	@Test void c11_searchFocusBorder() {
		assertEquals("#4aa3ff", worst("dark", "focusFilter").get("fg"), () -> "dark: " + worst("dark", "focusFilter"));
		assertEquals("#1589ee", worst("light", "focusFilter").get("fg"), () -> "light: " + worst("light", "focusFilter"));
	}

	/**
	 * The column-search popover input's focus border is the focus colour in dark and the accent in light, and
	 * meets 3:1 against the popover background in both.
	 */
	@Test void c12_columnSearchPopoverFocusBorder() {
		assertEquals("#4aa3ff", worst("colsearch-dark", "focusColSearch").get("fg"), () -> "dark: " + worst("colsearch-dark", "focusColSearch"));
		assertEquals("#1589ee", worst("colsearch-light", "focusColSearch").get("fg"), () -> "light: " + worst("colsearch-light", "focusColSearch"));
		assertContrast("colsearch-dark", COL_SEARCH_SURFACES, NON_TEXT, Map.of());
		assertContrast("colsearch-light", COL_SEARCH_SURFACES, NON_TEXT, Map.of());
	}

	private static void paints(List<String> bad, String scheme, String name, String fg, String bg) {
		var w = worst(scheme, name);
		if (w == null || ! fg.equals(w.get("fg")) || ! bg.equals(w.get("bg")))
			bad.add(scheme + " " + name + ": expected " + fg + " on " + bg + ", measured " + w);
	}

	private static void check(List<String> bad, String name, String colour, double[] bg, double min) {
		if (colour == null || bg == null) {
			bad.add(name + ": no colour (" + colour + ")");
			return;
		}
		var fg = Wcag.parse(colour);
		if (fg == null) {
			bad.add(name + ": unparseable colour " + colour);
			return;
		}
		var rt = Wcag.ratio(Wcag.over(fg, bg), bg);
		System.out.println("chart " + name + ": " + colour + " = " + rt);
		if (rt < min)
			bad.add(name + ": " + colour + " = " + rt + " < " + min);
	}

	/** WCAG 2 contrast over colours parsed from {@code #rgb}, {@code #rrggbb} or {@code rgb[a](...)}. */
	private static final class Wcag {

		/** {r, g, b, a} with channels 0-255 and alpha 0-1, or null. */
		static double[] parse(String s) {
			if (s == null)
				return null;
			s = s.trim();
			if (s.startsWith("#")) {
				var h = s.substring(1);
				if (h.length() == 3)
					h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
				if (h.length() != 6)
					return null;
				var v = Integer.parseInt(h, 16);
				return new double[] {v >> 16 & 0xff, v >> 8 & 0xff, v & 0xff, 1};
			}
			var m = java.util.regex.Pattern.compile("rgba?\\(\\s*([\\d.]+)[,\\s]+([\\d.]+)[,\\s]+([\\d.]+)(?:\\s*[,/]\\s*([\\d.]+))?\\s*\\)").matcher(s);
			if (! m.find())
				return null;
			return new double[] {Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)), m.group(4) == null ? 1 : Double.parseDouble(m.group(4))};
		}

		/** {@code top} composited over the opaque {@code under}. */
		static double[] over(double[] top, double[] under) {
			var a = top[3];
			return new double[] {top[0] * a + under[0] * (1 - a), top[1] * a + under[1] * (1 - a), top[2] * a + under[2] * (1 - a), 1};
		}

		static double ratio(double[] a, double[] b) {
			var x = lum(a);
			var y = lum(b);
			return Math.round((Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05) * 100) / 100.0;
		}

		private static double lum(double[] c) {
			return 0.2126 * channel(c[0]) + 0.7152 * channel(c[1]) + 0.0722 * channel(c[2]);
		}

		private static double channel(double v) {
			v /= 255;
			return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
		}
	}
}
