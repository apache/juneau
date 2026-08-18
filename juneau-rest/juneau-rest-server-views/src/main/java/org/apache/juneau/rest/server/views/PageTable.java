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

import static org.apache.juneau.bean.html5.HtmlBuilder.*;

import java.util.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * Builds the HTML delivery shell for a {@link PageDef} &mdash; the self-contained, class-based tab/sub-tab shell
 * (TODO-399 Phase C, design doc §"Client page runtime" + Decision 1(A)) wrapping one independent
 * {@code data-juneau-view} table per referenced {@link ViewDef}, plus the {@code <script type="application/json">}
 * PAGE_META sidecar the {@code juneau-pages.js} runtime consumes for hash routing.
 *
 * <p>
 * Mirrors the sibling {@link ViewTable} emitter pattern exactly, including its escaping contract (see that class's
 * javadoc): PAGE_META is serialized with the repo's canonical compact JSON marshaller, has every {@code <} escaped
 * to its JSON unicode escape ({@link ViewTable#escapeForScript(String)} &mdash; reused verbatim, not re-implemented,
 * so the two sidecars can never drift in how they neutralize a {@code </script>} break-out), and is inserted as
 * {@link org.apache.juneau.bean.html5.HtmlBuilder#rawText(String) raw content} so the returned bean stays
 * re-serializable.
 *
 * <h5 class='section'>No module dependency, class-based chrome only (Decision 1(A)):</h5>
 * <p>
 * The emitted shell carries only the neutral {@code .jc-*} classes ({@link #PAGE_CLASS}, {@link #TAB_CLASS},
 * {@link #SUBTAB_CLASS}, etc.) &mdash; no inline {@code style="..."} and no {@code url(...)}.  This module takes no
 * dependency on {@code juneau-rest-server-console-ui}; TODO-361's {@code chrome.css} may theme the same classes when
 * present, mirroring the already-agreed {@code .tag.<domain>.<value>} chip reconciliation.
 *
 * <h5 class='section'>Child views stay independent (no wrapping/absorption):</h5>
 * <p>
 * Each referenced {@link ViewDef} is rendered by delegating straight to {@link ViewTable#of(MarshallingContext,
 * ViewDef, Collection) ViewTable.of(ctx, view, null)} &mdash; the exact same code path a standalone
 * {@code ViewTable.of(view)} call uses &mdash; so a child view's marker table + VIEW_META sidecar is byte-for-byte
 * identical whether reached standalone or through a page.  This page emitter only wraps that untouched output in a
 * tab-bar/panel shell and controls which panel is visible; it never re-derives or mutates the child's contract.
 *
 * <h5 class='section'>PAGE_META shape:</h5>
 * <p>
 * A light-weight tab tree (id/label/subtabs only &mdash; no embedded {@code ViewDef} content, since the page
 * runtime's lazy-init binding layer locates a panel's {@code table[data-juneau-view]} elements directly via the DOM
 * rather than via PAGE_META): <c>{contractVersion, id, tabs:[{id, label, subtabs?:[{id, label}]}]}</c>.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link PageDef}
 * 	<li class='jc'>{@link ViewTable}
 * </ul>
 *
 * @since 10.0.0
 */
public class PageTable {

	/** Marker attribute the {@code juneau-pages.js} runtime looks for to auto-initialize a page shell. */
	public static final String MARKER_ATTR = "data-juneau-page";

	/** Prefix of the PAGE_META sidecar {@code <script>} element id: {@code juneau-page:<pageId>}. */
	public static final String SIDECAR_ID_PREFIX = "juneau-page:";

	/** Class on the page shell root. */
	public static final String PAGE_CLASS = "jc-page";

	/** Class on the top-level tab-bar container. */
	public static final String TAB_BAR_CLASS = "jc-tab-bar";

	/** Class on a single top-level tab-bar link. */
	public static final String TAB_CLASS = "jc-tab";

	/** Class on a sub-tab-bar container (nested inside a tab's panel). */
	public static final String SUBTAB_BAR_CLASS = "jc-subtab-bar";

	/** Class on a single sub-tab-bar link. */
	public static final String SUBTAB_CLASS = "jc-subtab";

	/** Class on a top-level tab panel. */
	public static final String PANEL_CLASS = "jc-panel";

	/** Class on a sub-tab panel (nested inside a tab's panel). */
	public static final String SUBPANEL_CLASS = "jc-subpanel";

	private PageTable() {}

	/**
	 * Builds the page shell for a built {@link PageDef}, using the default marshalling context.
	 *
	 * @param pageDef The built page definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code [data-juneau-page]} shell, tab/sub-tab bars, one panel per
	 * 	referenced view, and the PAGE_META sidecar.
	 */
	public static Div of(PageDef pageDef) {
		return of(MarshallingContext.DEFAULT, pageDef);
	}

	/**
	 * Builds the page shell using the specified marshalling context for the wrapped child views' cell reads.
	 *
	 * @param ctx The marshalling context passed through to each child {@code ViewTable.of(...)} call.  Must not be
	 * 	<jk>null</jk>.
	 * @param pageDef The built page definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code [data-juneau-page]} shell, tab/sub-tab bars, one panel per
	 * 	referenced view, and the PAGE_META sidecar.
	 */
	public static Div of(MarshallingContext ctx, PageDef pageDef) {
		var id = pageDef.id;
		var tabs = pageDef.tabs == null ? List.<Tab>of() : pageDef.tabs;

		var tabBarChildren = new ArrayList<>();
		for (var t : tabs)
			tabBarChildren.add(
				a(hashHref(id, t.id, null), t.label == null ? t.id : t.label)
					.class_(TAB_CLASS)
					.attr("data-tab-id", t.id));
		var tabBar = nav(tabBarChildren.toArray()).class_(TAB_BAR_CLASS).attr("role", "tablist");

		var panelsChildren = new ArrayList<>();
		for (var t : tabs)
			panelsChildren.add(buildTabPanel(ctx, id, t));
		var panels = div(panelsChildren.toArray()).class_("jc-panels");

		var json = ViewTable.escapeForScript(Json.of(buildMeta(pageDef)));
		var sidecar = script().type("application/json").id(SIDECAR_ID_PREFIX + id).text(rawText(json));

		return div(tabBar, panels, sidecar).id(id).attr(MARKER_ATTR, id).class_(PAGE_CLASS);
	}

	/** Builds one top-level tab's panel: either a leaf view panel, or a sub-tab bar + one sub-panel per subtab. */
	private static Div buildTabPanel(MarshallingContext ctx, String pageId, Tab t) {
		if (t.view != null) {
			var body = ViewTable.of(ctx, t.view, null);
			return div(body).class_(PANEL_CLASS).attr("data-panel-tab", t.id);
		}

		var subtabs = t.subtabs == null ? List.<Subtab>of() : t.subtabs;

		var subtabBarChildren = new ArrayList<>();
		for (var s : subtabs)
			subtabBarChildren.add(
				a(hashHref(pageId, t.id, s.id), s.label == null ? s.id : s.label)
					.class_(SUBTAB_CLASS)
					.attr("data-subtab-id", s.id)
					.attr("data-parent-tab", t.id));
		var subtabBar = nav(subtabBarChildren.toArray()).class_(SUBTAB_BAR_CLASS).attr("role", "tablist");

		var subpanelsChildren = new ArrayList<>();
		for (var s : subtabs) {
			var body = ViewTable.of(ctx, s.view, null);
			subpanelsChildren.add(
				div(body).class_(SUBPANEL_CLASS).attr("data-panel-tab", t.id).attr("data-panel-subtab", s.id));
		}
		var subpanels = div(subpanelsChildren.toArray()).class_("jc-subpanels");

		return div(subtabBar, subpanels).class_(PANEL_CLASS).attr("data-panel-tab", t.id);
	}

	/** Builds the deep-linkable hash href: {@code #pageId/tabId} or {@code #pageId/tabId/subtabId}. */
	private static String hashHref(String pageId, String tabId, String subtabId) {
		var sb = new StringBuilder("#").append(pageId).append('/').append(tabId);
		if (subtabId != null)
			sb.append('/').append(subtabId);
		return sb.toString();
	}

	/** Projects a {@link PageDef} down to the light-weight PAGE_META tab tree (no embedded ViewDef content). */
	private static PageMeta buildMeta(PageDef pageDef) {
		var meta = new PageMeta();
		meta.contractVersion = pageDef.contractVersion;
		meta.id = pageDef.id;
		var tabMetas = new ArrayList<TabMeta>();
		for (var t : (pageDef.tabs == null ? List.<Tab>of() : pageDef.tabs)) {
			var tm = new TabMeta();
			tm.id = t.id;
			tm.label = t.label;
			if (t.subtabs != null) {
				var subMetas = new ArrayList<SubtabMeta>();
				for (var s : t.subtabs) {
					var sm = new SubtabMeta();
					sm.id = s.id;
					sm.label = s.label;
					subMetas.add(sm);
				}
				tm.subtabs = subMetas;
			}
			tabMetas.add(tm);
		}
		meta.tabs = tabMetas;
		return meta;
	}

	/** The PAGE_META root: {@code {contractVersion, id, tabs}}. */
	@BeanType(properties="contractVersion,id,tabs")
	public static class PageMeta {

		/** The frozen contract-version discriminator. */
		public String contractVersion;

		/** The page id. */
		public String id;

		/** The tab tree. */
		public List<TabMeta> tabs;
	}

	/** One PAGE_META tab entry: {@code {id, label, subtabs?}}. */
	@BeanType(properties="id,label,subtabs")
	public static class TabMeta {

		/** The tab id. */
		public String id;

		/** The tab label. */
		public String label;

		/** The sub-tab entries; omitted for a leaf tab. */
		public List<SubtabMeta> subtabs;
	}

	/** One PAGE_META sub-tab entry: {@code {id, label}}. */
	@BeanType(properties="id,label")
	public static class SubtabMeta {

		/** The sub-tab id. */
		public String id;

		/** The sub-tab label. */
		public String label;
	}
}
