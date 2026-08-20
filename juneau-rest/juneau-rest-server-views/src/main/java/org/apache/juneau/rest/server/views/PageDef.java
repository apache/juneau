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
	 * Finalizes the builder, validating the composed tab tree, and returns the wire-ready {@link PageDef}.
	 *
	 * <p>
	 * Validation (design doc §"Bean model"; TODO-420 widens the panel-body shape from a view/subtabs
	 * exclusive-or to the matrix documented on {@link Tab}/{@link Subtab}): at least one tab must be declared;
	 * every {@link Tab} must satisfy its own panel-body matrix and every declared {@link Subtab} its own
	 * (delegated to {@link Tab#validate()}, which also checks sub-tab id uniqueness <i>within</i> that tab); every
	 * {@link Tab#id} must be unique across the page; and every referenced {@link ViewDef#id} (from a leaf tab's
	 * view or any subtab's view &mdash; a tab or subtab carrying {@link Tab#content}/{@link Subtab#content}
	 * instead references no {@link ViewDef} and contributes nothing here) must be unique across the whole page, so
	 * hash routing and sidecar lookup stay unambiguous.
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

	private void addViewId(Set<String> viewIds, String viewId) {
		if (!viewIds.add(viewId))
			throw iaex("PageDef '%s': duplicate referenced ViewDef id '%s'.", id, viewId);
	}
}
