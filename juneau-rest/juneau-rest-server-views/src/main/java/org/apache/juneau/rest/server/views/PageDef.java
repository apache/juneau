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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * The top-level, declarative "tabs / sub-tabs page" definition (design doc §"Bean model") &mdash; composes
 * multiple existing {@link ViewDef} views into one multi-tab / sub-tab admin page.
 *
 * <p>
 * A {@link PageDef} is an ordinary Juneau bean built via a small fluent builder ({@link #create(String)} + chained
 * setters + {@link #build()}), mirroring {@link ViewDef}'s builder ergonomics exactly.  It holds an ordered list of
 * {@link Tab}; each {@link Tab} holds either a single child {@link ViewDef} reference or an ordered list of
 * {@link Subtab}, each of which references one {@link ViewDef}.
 *
 * <p>
 * The page does <b>not</b> wrap/absorb a child view's configuration &mdash; {@link PageTable} renders each
 * referenced view exactly as {@code ViewTable.of(...)} already does (marker table + VIEW_META sidecar) and only
 * controls which panel is visible.  {@link #contractVersion} reuses {@link ViewDef#CONTRACT_VERSION} as the single
 * source of truth, mirroring {@code ViewsMixin.CONTRACT_VERSION}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	PageDef <jv>page</jv> = PageDef.<jsm>create</jsm>(<js>"admin"</js>)
 * 		.title(<js>"Admin"</js>)
 * 		.tabs(
 * 			Tab.<jsm>create</jsm>(<js>"releases"</js>, <js>"Releases"</js>).view(releasesView),
 * 			Tab.<jsm>create</jsm>(<js>"catalog"</js>, <js>"Catalog"</js>).subtabs(
 * 				Subtab.<jsm>create</jsm>(<js>"packages"</js>, <js>"Packages"</js>).view(packagesView),
 * 				Subtab.<jsm>create</jsm>(<js>"bundles"</js>, <js>"Bundles"</js>).view(bundlesView)))
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link Tab}
 * 	<li class='jc'>{@link Subtab}
 * 	<li class='jc'>{@link PageTable}
 * 	<li class='jc'>{@link ViewDef}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,id,title,tabs")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class PageDef {

	/** The frozen contract version, reusing {@link ViewDef#CONTRACT_VERSION} as the single source of truth. */
	public static final String CONTRACT_VERSION = ViewDef.CONTRACT_VERSION;

	/** The frozen contract-version discriminator (always {@value #CONTRACT_VERSION} for this contract). */
	public String contractVersion = CONTRACT_VERSION;

	/** The stable page id (the first hash segment, {@code #<id>/tabId[/subtabId]}). */
	public String id;

	/** Optional page title. */
	public String title;

	/** The ordered top-level tabs. */
	public List<Tab> tabs;

	/**
	 * Optional page-chrome header (concept #18/#19), emitted as a whole {@code <header class="jc-header">} by
	 * {@link PageTable}.  This is a <b>Java-only builder field</b> &mdash; it is omitted from {@code @BeanType} / the
	 * wire (exactly like a reserved-and-omitted field), and it does <b>not</b> bump {@link #CONTRACT_VERSION}.  The
	 * html5 emit lives in {@link AppHeaderTable}; the beans live in {@code juneau-rest-server-widgets}.
	 */
	public AppHeaderDef header;

	/**
	 * Optional bar slot (concept #9), emitted by {@link PageTable} as a <b>trailing sibling of {@code .jc-subtab-bar}</b>
	 * &mdash; never into the archived {@code .juneau-view-toolbar-*} DataTables control row.  Also a Java-only builder
	 * field, omitted from the wire and not bumping {@link #CONTRACT_VERSION}.
	 */
	public BarSlot barSlot;

	/**
	 * Author-declared server-side scalar values interpolated into <b>this page's own</b> chrome (titles/labels) as
	 * <js>"$FV{name}"</js> at serve time; a <b>Java-only builder field</b>, omitted from {@code @BeanType} / the wire
	 * (lambda providers never marshal) and not bumping {@link #CONTRACT_VERSION}.
	 *
	 * <p>
	 * {@code PageTable.of(req, pageDef)} resolves the closed page-chrome allowlist against a per-response
	 * <b>sibling</b> {@link org.apache.juneau.commons.svl.VarResolverSession} carrying its own registry, exactly as
	 * {@link ViewTable} does for {@link ViewDef#serverValues}.  The allowlist is {@link #title}, {@link Tab#label},
	 * {@link Subtab#label}, and &mdash; for the Java-only page chrome &mdash; {@link Brand#title},
	 * each {@link Brand#crumbs} element, {@link HeaderAction#tooltip}, {@link AvatarChip#displayName},
	 * {@link AvatarChip#initials}, {@link BarText#text}, and {@link BarBadge#label}.  Nothing else is interpolated.
	 *
	 * <h5 class='section'>There is no inheritance:</h5>
	 * <p>
	 * Each host resolves only its <i>own</i> allowlisted fields.  A {@link ViewDef} rendered inside this page does
	 * <b>not</b> see this declaration &mdash; a child view interpolates only what its own
	 * {@link ViewDef#serverValues} declares, and a child that declares none keeps its {@code $FV{...}} text literal.
	 * The hosts' sessions are <b>siblings</b>, not nested.
	 *
	 * <h5 class='section'>Same-name collisions across hosts are legal and independent:</h5>
	 * <p>
	 * This page and a child view may both declare a value named (say) {@code env} and they may resolve to different
	 * strings in the same response.  That is a documented semantic, not an accident: a name is scoped to the host
	 * that declared it.
	 */
	public ServerValues serverValues;

	/**
	 * Private lock object guarding the {@code $FV} chrome-resolution window this instance may be mutated under
	 * (see {@link PageTable}).  Synchronizing on this dedicated object rather than on {@code this} keeps the
	 * monitor private to the toolkit even though callers hold a reference to the bean itself.  Not a wire field.
	 */
	final Object lock = new Object();

	/**
	 * Starts a new {@link PageDef} builder with the specified stable page id.
	 *
	 * @param id The stable page id.  Must not be <jk>null</jk> or blank.
	 * @return A new mutable {@link PageDef} to chain builder calls on.
	 */
	public static PageDef create(String id) {
		if (id == null || id.isBlank())
			throw iaex("PageDef id must not be null or blank.");
		var p = new PageDef();
		p.id = id;
		return p;
	}

	/**
	 * Sets the page title.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public PageDef title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets the ordered top-level tabs.
	 *
	 * @param value The tabs, in display order.
	 * @return This object.
	 */
	public PageDef tabs(Tab...value) {
		tabs = l(value);
		return this;
	}

	/**
	 * Sets the optional page-chrome header.
	 *
	 * <p>
	 * Java-only builder field: not on the wire, and does not bump {@link #CONTRACT_VERSION}.
	 *
	 * @param value The header definition.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public PageDef header(AppHeaderDef value) {
		header = value;
		return this;
	}

	/**
	 * Sets the optional bar slot (trailing sibling of {@code .jc-subtab-bar}).
	 *
	 * <p>
	 * Java-only builder field: not on the wire, and does not bump {@link #CONTRACT_VERSION}.
	 *
	 * @param value The bar-slot definition.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public PageDef barSlot(BarSlot value) {
		barSlot = value;
		return this;
	}

	/**
	 * Declares the server-side scalar values interpolated into this page's own chrome as <js>"$FV{name}"</js>.
	 *
	 * <p>
	 * See {@link #serverValues} &mdash; a Java-only builder field, not a PAGE_META JSON key, and not inherited by any
	 * child {@link ViewDef}.
	 *
	 * @param value The server-values declaration.  Can be <jk>null</jk> (no {@code $FV} interpolation).
	 * @return This object.
	 */
	public PageDef serverValues(ServerValues value) {
		serverValues = value;
		return this;
	}

	/**
	 * Finalizes the builder, validating the composed tab tree, and returns the wire-ready {@link PageDef}.
	 *
	 * <p>
	 * Validation (design doc §"Bean model"; the panel-body matrix documented on {@link Tab}/{@link Subtab} widens
	 * this from a view/subtabs exclusive-or): at least one tab must be declared;
	 * every {@link Tab} must satisfy its own panel-body matrix and every declared {@link Subtab} its own
	 * (delegated to {@link Tab#validate()}, which also checks sub-tab id uniqueness <i>within</i> that tab); every
	 * {@link Tab#id} must be unique across the page; and every referenced {@link ViewDef#id} (from a leaf tab's
	 * view or any subtab's view &mdash; a tab or subtab carrying {@link Tab#content}/{@link Subtab#content}
	 * instead references no {@link ViewDef} and contributes nothing here) must be unique across the whole page, so
	 * hash routing and sidecar lookup stay unambiguous.
	 *
	 * <p>
	 * This is also where <b>cross-host</b> bar-slot id uniqueness is enforced: a {@link RowDetailDef#barSlot} in this
	 * page's view tree may not reuse this page's own {@link #barSlot} id, since a page can now hold both hosts live at
	 * once.  {@link RowDetailDef#validate(java.util.List)} cannot make that check &mdash; it has no enclosing page
	 * &mdash; and a top-level view served with no page has no page slot to collide with at all.
	 *
	 * @return This object.
	 * @throws IllegalArgumentException On any validation rule violation.
	 */
	public PageDef build() {
		validate();
		return this;
	}

	void validate() {
		if (tabs == null || tabs.isEmpty())
			throw iaex("PageDef '%s' must declare at least one tab.", id);
		validateTabs();
		validateChrome();
		validateBarSlotUniqueness();
	}

	/** Validates every declared tab/subtab and collects/checks tab-id and referenced-view-id uniqueness. */
	private void validateTabs() {
		var tabIds = new HashSet<String>();
		var viewIds = new HashSet<String>();
		for (var t : tabs) {
			t.validate();
			if (!tabIds.add(t.id))
				throw iaex("PageDef '%s': duplicate tab id '%s'.", id, t.id);
			if (t.view != null) {
				addViewId(viewIds, t.view.id);
				t.view.validate();
			}
			if (t.subtabs != null)
				for (var s : t.subtabs)
					if (s.view != null) {
						addViewId(viewIds, s.view.id);
						s.view.validate();
					}
		}
	}

	/** Validates the Java-only page-chrome fields (m1/m2): validated on the serving path, absent from the wire. */
	private void validateChrome() {
		if (header != null)
			header.validate();
		if (barSlot != null)
			barSlot.validate();
		if (serverValues != null)
			serverValues.validate();
	}

	/**
	 * Enforces cross-host bar-slot id uniqueness.  This is the ONLY scope that sees both hosts:
	 * {@link RowDetailDef#validate(java.util.List)} has no enclosing page, so it cannot make this check.  Runs
	 * last, so both slots have already passed {@link BarSlot#validate()} and their ids are known non-blank.
	 */
	private void validateBarSlotUniqueness() {
		if (barSlot == null)
			return;
		for (var t : tabs) {
			checkDetailBarSlotIds(t.view, 0);
			if (t.subtabs != null)
				for (var s : t.subtabs)
					checkDetailBarSlotIds(s.view, 0);
		}
	}

	/**
	 * Rejects a {@link RowDetailDef#barSlot} whose id equals this page's own {@link #barSlot} id, anywhere in the view
	 * tree this page references &mdash; including a detail section's nested table view, which carries its own details.
	 *
	 * <p>
	 * A page with no {@link #barSlot}, and a top-level {@link ViewDef} served with no enclosing page at all, have no
	 * page slot to collide with: those are legal no-ops rather than errors.
	 *
	 * @param view The view to walk.  May be <jk>null</jk> (a content-only tab references none).
	 * @param depth The nesting depth, bounded by {@link NestedTableDef#MAX_DEPTH}.
	 */
	private void checkDetailBarSlotIds(ViewDef view, int depth) {
		if (view == null || view.details == null || depth > NestedTableDef.MAX_DEPTH)
			return;
		var detailSlot = view.details.barSlot;
		if (detailSlot != null && barSlot.id.equals(detailSlot.id))
			throw iaex("PageDef '%s': RowDetailDef bar slot id '%s' duplicates the page bar slot id.", id, barSlot.id);
		if (view.details.sections != null)
			for (var s : view.details.sections)
				if (s != null && s.table != null)
					checkDetailBarSlotIds(s.table.view, depth + 1);
	}

	private void addViewId(Set<String> viewIds, String viewId) {
		if (!viewIds.add(viewId))
			throw iaex("PageDef '%s': duplicate referenced ViewDef id '%s'.", id, viewId);
	}
}
