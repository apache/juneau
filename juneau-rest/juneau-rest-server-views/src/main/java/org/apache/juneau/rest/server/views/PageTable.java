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
import static org.apache.juneau.commons.utils.StringUtils.escapeForScript;

import java.util.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.*;

/**
 * Builds the HTML delivery shell for a {@link PageDef} &mdash; the self-contained, class-based tab/sub-tab shell
 * (design doc §"Client page runtime" + Decision 1(A)) wrapping one independent {@code data-juneau-view} table per
 * referenced {@link ViewDef}, plus the {@code <script type="application/json">} PAGE_META sidecar the
 * {@code juneau-pages.js} runtime consumes for hash routing.
 *
 * <p>
 * Mirrors the sibling {@link ViewTable} emitter pattern exactly, including its escaping contract (see that class's
 * javadoc): PAGE_META is serialized with the repo's canonical compact JSON marshaller, is passed through
 * {@link StringUtils#escapeForScript(String)} (the same shared escaper {@link ViewTable} uses, so the two sidecars
 * can never drift in how they neutralize a {@code </script>} break-out), and is inserted as
 * {@link org.apache.juneau.bean.html5.HtmlBuilder#rawText(String) raw content} so the returned bean stays
 * re-serializable.
 *
 * <h5 class='section'>Panel markup contract (shared with {@code juneau-pages.js} &mdash; both sides MUST agree):</h5>
 * <p>
 * Panel visibility is a two-attribute, hierarchically-narrowing contract:
 * <ul class='spaced-list'>
 * 	<li>{@link #PANEL_TAB_ATTR} scopes a panel to one top-level tab and is emitted on <b>every</b> panel.
 * 	<li>{@link #PANEL_SUBTAB_ATTR} is <b>optional</b> and only <i>narrows</i> a panel further, to one specific
 * 		sub-tab.  Omitting it means the panel is sub-tab-<b>agnostic</b>: it is shown whenever its tab is active,
 * 		whichever sub-tab that tab resolved to.
 * </ul>
 * <p>
 * A leaf tab (one declaring {@link Tab#view}) therefore emits a single sub-tab-agnostic {@link #PANEL_CLASS} panel.
 * A tab declaring {@link Tab#subtabs} emits <b>two nested levels</b>: an outer {@link #PANEL_CLASS} panel carrying
 * only {@link #PANEL_TAB_ATTR} (it wraps the sub-tab bar and must stay visible for <i>all</i> of its sub-tabs, so it
 * deliberately carries no {@link #PANEL_SUBTAB_ATTR} &mdash; a static attribute could only ever name one of them),
 * and inside it one {@link #SUBPANEL_CLASS} per sub-tab carrying <b>both</b> attributes.  Because
 * {@code juneau-views.css} hides both panel classes until the runtime adds {@code .jc-active}, the runtime's
 * matching rule must treat a missing {@link #PANEL_SUBTAB_ATTR} as "any sub-tab"; requiring an exact match instead
 * leaves the outer panel {@code display:none} and blanks the whole tab, sub-tab bar and active sub-panel included.
 *
 * <h5 class='section'>No module dependency, class-based chrome only (Decision 1(A)):</h5>
 * <p>
 * The emitted shell carries only the neutral {@code .jc-*} classes ({@link #PAGE_CLASS}, {@link #TAB_CLASS},
 * {@link #SUBTAB_CLASS}, etc.) &mdash; no inline {@code style="..."} and no {@code url(...)}.  This module takes no
 * dependency on {@code juneau-rest-server-console-ui}; that module's {@code chrome.css} may theme the same classes
 * when present, mirroring the already-agreed {@code .tag.<domain>.<value>} chip reconciliation.
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
 * rather than via PAGE_META): <c>{contractVersion, id, tabs:[{id, label, subtabs?:[{id, label}]}]}</c>. This
 * projection is built by {@link #buildMeta(PageDef)}, which copies only the fields above from each {@link Tab}/
 * {@link Subtab} &mdash; {@link Tab#content}/{@link Subtab#content} (TODO-420) are never read by it and so
 * structurally cannot reach PAGE_META, regardless of what {@code @BeanType} pins on {@link Tab}/{@link Subtab};
 * this is a stronger guarantee than reserve-and-omit (there is nothing to omit because there is no code path that
 * copies it), and it is why the field is also simply absent from {@link Tab}/{@link Subtab}'s own
 * {@code @BeanType} property lists, so the framework's wire contract (and {@link PageDef#CONTRACT_VERSION}) needs
 * no bump to add it.
 *
 * <h5 class='section'>Panel-body content (TODO-420): template engine, trusted / first-party content only</h5>
 * <p>
 * {@link Tab#content}/{@link Subtab#content} let a panel hold raw markup instead of (or, for {@link Tab#content},
 * above) a {@code ViewDef}-backed table &mdash; see the panel-body matrices on {@link Tab}/{@link Subtab}. This
 * emitter writes that markup <b>verbatim</b>, via {@code rawText(...)}, exactly like the PAGE_META/VIEW_META JSON
 * sidecars use {@code rawText(...)} for their own (separately escaped) payloads. Unlike the sidecars, {@code
 * content} passes through with <b>no</b> escaping of any kind &mdash; the framework is a template engine on this
 * path: the caller pre-sanitizes, this method emits verbatim. {@code content} is for trusted, first-party prose
 * only (the FG-2 docs-page use case) and MUST NEVER carry live/remote/attacker-influenceable data; a write-path
 * confirmation/detail body must use a typed/escaped path instead. A build-gating scanner
 * ({@code RawContentSinkScanner}, this module's test tree) enforces that this framework's own sources never pour a
 * non-literal (i.e. plausibly live-data-derived) value into {@link Tab#content}/{@link Subtab#content}.
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

	/**
	 * Attribute scoping a panel to one top-level tab; emitted on <b>every</b> panel.
	 *
	 * <p>
	 * Published alongside the {@code .jc-*} class constants because it is the load-bearing half of the panel markup
	 * contract documented in this class's javadoc: {@code juneau-pages.js}'s {@code panelMatches} reads this exact
	 * attribute name and cannot import a Java constant, so renaming it here without mirroring it there silently
	 * blanks every panel.  {@code PageTable_SubtabPanelContract_Test} asserts the two spellings still agree.
	 */
	public static final String PANEL_TAB_ATTR = "data-panel-tab";

	/**
	 * Attribute narrowing a panel to one specific sub-tab; <b>optional</b>, and its <i>absence</i> is meaningful.
	 *
	 * <p>
	 * A panel omitting it is sub-tab-agnostic (shown for whichever sub-tab its tab resolved to) &mdash; the rule a
	 * sub-tabbed tab's outer panel depends on to render at all.  Same cross-artifact caveat as
	 * {@link #PANEL_TAB_ATTR}.
	 */
	public static final String PANEL_SUBTAB_ATTR = "data-panel-subtab";

	/**
	 * Attribute carrying a top-level tab-bar link's tab id, compared by the runtime to decide which tab reads as
	 * selected.  Same cross-artifact caveat as {@link #PANEL_TAB_ATTR}.
	 */
	public static final String TAB_ID_ATTR = "data-tab-id";

	/**
	 * Attribute carrying a sub-tab-bar link's sub-tab id.  Same cross-artifact caveat as {@link #PANEL_TAB_ATTR}.
	 */
	public static final String SUBTAB_ID_ATTR = "data-subtab-id";

	/**
	 * Attribute carrying the id of the tab a sub-tab link belongs to.
	 *
	 * <p>
	 * Sub-tab ids are only required to be unique <i>within</i> their tab, so the runtime pairs this with
	 * {@link #SUBTAB_ID_ATTR} before marking a sub-tab selected; matching on the sub-tab id alone would light up a
	 * same-named sub-tab under a different tab.  Same cross-artifact caveat as {@link #PANEL_TAB_ATTR}.
	 */
	public static final String PARENT_TAB_ATTR = "data-parent-tab";

	/**
	 * Attribute the resolved saved-views REST base is stamped onto on this page shell so every nested
	 * {@code table[data-juneau-view]} can find it via {@code closest('[data-juneau-saved-views]')}.
	 *
	 * <p>
	 * Same spelling as {@link ViewTable#SAVED_VIEWS_ATTR} (the shared contract with {@code juneau-config.js}).
	 */
	public static final String SAVED_VIEWS_ATTR = ViewTable.SAVED_VIEWS_ATTR;

	private PageTable() {}

	/**
	 * Builds the page shell for a built {@link PageDef}, using the default marshalling context.
	 *
	 * @param pageDef The built page definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code [data-juneau-page]} shell, tab/sub-tab bars, one panel per
	 * 	referenced view, and the PAGE_META sidecar.
	 */
	public static Div of(PageDef pageDef) {
		return of(MarshallingContext.DEFAULT, pageDef, null);
	}

	/**
	 * Builds the page shell, stamping the request's resolved saved-views REST base onto the page shell so
	 * nested view tables can discover it via {@code closest(...)}.
	 *
	 * @param req The current request, supplying the URI resolver.  Must not be <jk>null</jk>.
	 * @param pageDef The built page definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code [data-juneau-page]} shell and {@link #SAVED_VIEWS_ATTR}.
	 */
	public static Div of(RestRequest req, PageDef pageDef) {
		return of(MarshallingContext.DEFAULT, pageDef, SavedViewsMixin.resolvedBaseUrl(req));
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
		return of(ctx, pageDef, null);
	}

	/**
	 * Builds the page shell, optionally stamping a pre-resolved saved-views REST base onto the page shell.
	 *
	 * @param ctx The marshalling context passed through to each child {@code ViewTable.of(...)} call.  Must not be
	 * 	<jk>null</jk>.
	 * @param pageDef The built page definition.  Must not be <jk>null</jk>.
	 * @param savedViewsBase The already-resolved saved-views REST base, or <jk>null</jk>/blank to stamp none.
	 * @return A new {@link Div} carrying the {@code [data-juneau-page]} shell and optional {@link #SAVED_VIEWS_ATTR}.
	 */
	public static Div of(MarshallingContext ctx, PageDef pageDef, String savedViewsBase) {
		pageDef.validate();
		var id = pageDef.id;
		var tabs = pageDef.tabs == null ? List.<Tab>of() : pageDef.tabs;

		var tabBarChildren = new ArrayList<>();
		for (var t : tabs)
			tabBarChildren.add(
				a(hashHref(id, t.id, null), t.label == null ? t.id : t.label)
					.class_(TAB_CLASS)
					.attr(TAB_ID_ATTR, t.id));
		var tabBar = nav(tabBarChildren.toArray()).class_(TAB_BAR_CLASS).attr("role", "tablist");

		var panelsChildren = new ArrayList<>();
		for (var t : tabs)
			panelsChildren.add(buildTabPanel(ctx, id, t));
		var panels = div(panelsChildren.toArray()).class_("jc-panels");

		var json = escapeForScript(Json.of(buildMeta(pageDef)));
		var sidecar = script().type("application/json").id(SIDECAR_ID_PREFIX + id).text(rawText(json));

		var shell = div(tabBar, panels, sidecar).id(id).attr(MARKER_ATTR, id).class_(PAGE_CLASS);
		if (savedViewsBase != null && ! savedViewsBase.isBlank())
			shell.attr(SAVED_VIEWS_ATTR, savedViewsBase);
		return shell;
	}

	/**
	 * Builds one top-level tab's panel: a leaf view panel, a leaf raw-content panel, or a (optionally
	 * content-prefaced) sub-tab bar + one sub-panel per subtab &mdash; the {@code Tab} panel-body matrix
	 * ({@code {view} | {subtabs} | {content} | {content+subtabs}}, TODO-420) mirrored from {@link Tab#validate()}.
	 */
	private static Div buildTabPanel(MarshallingContext ctx, String pageId, Tab t) {
		if (t.view != null) {
			var body = ViewTable.of(ctx, t.view, null);
			return div(body).class_(PANEL_CLASS).attr(PANEL_TAB_ATTR, t.id);
		}

		var subtabs = t.subtabs == null ? List.<Subtab>of() : t.subtabs;

		if (subtabs.isEmpty())
			// Leaf content-only tab (Tab = {content}): the panel body IS the raw content, emitted verbatim (see
			// Tab#content's ownership contract - caller sanitizes, this emits as-is).
			return div(rawText(t.content)).class_(PANEL_CLASS).attr(PANEL_TAB_ATTR, t.id);

		var subtabBarChildren = new ArrayList<>();
		for (var s : subtabs)
			subtabBarChildren.add(
				a(hashHref(pageId, t.id, s.id), s.label == null ? s.id : s.label)
					.class_(SUBTAB_CLASS)
					.attr(SUBTAB_ID_ATTR, s.id)
					.attr(PARENT_TAB_ATTR, t.id));
		var subtabBar = nav(subtabBarChildren.toArray()).class_(SUBTAB_BAR_CLASS).attr("role", "tablist");

		var subpanelsChildren = new ArrayList<>();
		for (var s : subtabs) {
			// Subtab = {view} | {content} (Subtab#validate()): exactly one of these is non-null here.
			Object body = s.view != null ? ViewTable.of(ctx, s.view, null) : rawText(s.content);
			subpanelsChildren.add(
				div(body).class_(SUBPANEL_CLASS).attr(PANEL_TAB_ATTR, t.id).attr(PANEL_SUBTAB_ATTR, s.id));
		}
		var subpanels = div(subpanelsChildren.toArray()).class_("jc-subpanels");

		// Tab-scoped only, on purpose: this panel wraps the sub-tab bar and must be visible for EVERY sub-tab, so it
		// stays sub-tab-agnostic (see the panel markup contract in this class's javadoc).  Do not add
		// data-panel-subtab here - it would pin the whole tab to a single sub-tab and blank it for the others.
		//
		// The optional Tab#content preamble (Tab = {content+subtabs}) is emitted first, inside this SAME outer
		// panel - so it renders above the sub-tab bar and stays visible for every sub-tab exactly like the bar
		// itself, with no separate routing attribute (design doc open question 3).
		var outerChildren = new ArrayList<>();
		if (t.content != null)
			outerChildren.add(rawText(t.content));
		outerChildren.add(subtabBar);
		outerChildren.add(subpanels);
		return div(outerChildren.toArray()).class_(PANEL_CLASS).attr(PANEL_TAB_ATTR, t.id);
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
