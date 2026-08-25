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

/**
 * A DataTables view nested inside a {@link DetailSection} of a row-detail panel.
 *
 * <p>
 * The wrapped {@link #view} is an ordinary {@link ViewDef} that the {@code juneau-views.js} runtime instantiates
 * after the enclosing row's detail GET succeeds and the section pane becomes visible.  The nested table runs its own
 * same-origin data GET, scoped to the parent row by merging a single query parameter (named {@link #parentScopeParam},
 * default {@code "parentId"}) carrying the parent row id &mdash; there is no {@code {parentId}} URL template.
 *
 * <h5 class='section'>Depth</h5>
 * <p>
 * Table nesting is capped at {@link #MAX_DEPTH}: the enclosing (root) table is depth 1 and a nested table is
 * depth 2.  The depth is the contract, not an author knob &mdash; there is no {@code maxDepth} field and no way to
 * clamp it downward.  A nested view may declare its own {@link ViewDef#details} sections, but none of those sections
 * may declare a further {@link NestedTableDef} (that table would be depth 3).  {@link #validate()} walks the graph
 * with a <b>path-scoped</b> identity set (pushed on descent, popped on unwind), so a self-referencing or
 * mutually-referencing author graph fails closed while a sibling DAG that reuses one {@link ViewDef} instance under
 * two different parents stays legal.
 *
 * <h5 class='section'>What a nested view may declare</h5>
 * <p>
 * A nested view may declare columns, paging, sort, search, {@link ViewDef#rowActions}, detail sections, and (via
 * {@link #selection}) row selection.  {@link ViewDef#columnConfig} is <b>not</b> permitted: the column chooser and
 * its saved-views identity live on the enclosing table only, as does bulk mutation &mdash; which is why there is no
 * {@link BulkMutateDef} field here.  A nested mutating action rides the enclosing response's CSRF token; once that
 * token has rotated the action fails closed through the ordinary 403 path (recovery is a page reload, never a
 * nested token refresh).
 *
 * <p>
 * The {@link #CONTRACT_VERSION} handshake is independent of the enclosing {@link ViewDef#CONTRACT_VERSION} and
 * {@link RowDetailDef#CONTRACT_VERSION}: the nested runtime fails loud on a mismatch of its own version alone.  This
 * type is Java-only &mdash; it is not part of the {@code VIEW_META} JSON sidecar.
 *
 * @since 10.0.0
 */
public class NestedTableDef {

	/** The frozen contract version for the nested-table shell, independent of the enclosing view/detail contracts. */
	public static final String CONTRACT_VERSION = "2";

	/**
	 * The maximum table nesting depth: the enclosing (root) table is depth 1, a nested table is depth 2.
	 *
	 * <p>
	 * This is the topology, not a configurable ceiling.  A {@link NestedTableDef} reached from a nested view would
	 * be at depth 3 and fails {@link #validate()}.
	 */
	public static final int MAX_DEPTH = 2;

	/** The depth {@link #validate()} anchors its descent at: a nested table is always depth 2. */
	private static final int NESTED_DEPTH = 2;

	/** DataTables server-side request parameters that a scope parameter must not shadow. */
	private static final Set<String> RESERVED_SCOPE_PARAMS =
		Set.of("draw", "start", "length", "search", "columns", "order", "_");

	/** The nested view rendered inside the detail section. */
	public ViewDef view;

	/**
	 * The optional row-selection opt-in for this nested table.
	 *
	 * <p>
	 * Adds per-row checkboxes (and, per {@link SelectionDef#selectAll()}, a select-all header checkbox) to the
	 * nested table alone.  Bulk mutation is deliberately not expressible here: the bulk toolbar and its sidecar
	 * stay bound to the enclosing table's id, so two expanded rows share one parent bulk affordance rather than
	 * minting one per nested table.
	 */
	public SelectionDef selection;

	/**
	 * The query-parameter name the nested data GET carries the parent row id under.
	 *
	 * <p>
	 * Merged into both the client-side ({@code data.dataSrc}) and server-side ({@code data} callback) ajax paths.
	 * Must match {@code [A-Za-z][A-Za-z0-9_]*} and must not shadow a reserved DataTables parameter.
	 */
	public String parentScopeParam = "parentId";

	/**
	 * Wraps a nested view.
	 *
	 * @param view The nested {@link ViewDef}.  May be <jk>null</jk> (then fails {@link #validate()}).
	 * @return A new {@link NestedTableDef}.
	 */
	public static NestedTableDef create(ViewDef view) {
		var n = new NestedTableDef();
		n.view = view;
		return n;
	}

	/**
	 * Sets the parent-scope query-parameter name.
	 *
	 * @param value The parameter name.  Must match {@code [A-Za-z][A-Za-z0-9_]*} and not be a reserved DataTables key.
	 * @return This object.
	 */
	public NestedTableDef parentScopeParam(String value) {
		parentScopeParam = value;
		return this;
	}

	/**
	 * Enables row selection on this nested table.
	 *
	 * @param value The selection opt-in.  May be <jk>null</jk> (no selection).
	 * @return This object.
	 */
	public NestedTableDef selection(SelectionDef value) {
		selection = value;
		return this;
	}

	/**
	 * Fail-closed bean validation.
	 *
	 * <p>
	 * Enforces: a non-null nested view; a legal, non-reserved {@link #parentScopeParam}; a non-blank same-origin
	 * relative-or-{@code servlet:} {@link ViewDef#dataUrl}; the parent-only {@link ViewDef#columnConfig} forbid; and
	 * the {@link #MAX_DEPTH} cap with path-scoped cycle detection.  Then delegates to {@link ViewDef#validate()}.
	 *
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	public void validate() {
		if (view == null)
			throw iaex("NestedTableDef view must not be null.");

		if (parentScopeParam == null || parentScopeParam.isBlank())
			throw iaex("NestedTableDef parentScopeParam must not be null or blank.");
		if (RESERVED_SCOPE_PARAMS.contains(parentScopeParam))
			throw iaex("NestedTableDef parentScopeParam '%s' is a reserved DataTables request parameter.", parentScopeParam);
		if (!parentScopeParam.matches("[A-Za-z][A-Za-z0-9_]*"))
			throw iaex("NestedTableDef parentScopeParam '%s' must match [A-Za-z][A-Za-z0-9_]*.", parentScopeParam);

		if (view.dataUrl == null || view.dataUrl.isBlank())
			throw iaex("NestedTableDef view dataUrl must not be null or blank.");
		if (!isSafeNestedDataUrl(view.dataUrl))
			throw iaex("NestedTableDef view dataUrl must be a same-origin relative or 'servlet:' path (no absolute URL, '//', '..', or foreign scheme): %s",
				view.dataUrl);

		if (view.columnConfig != null)
			throw iaex("NestedTableDef view must not declare columnConfig: the column chooser stays on the enclosing table.");

		// The depth/cycle walk runs BEFORE view.validate(), which would otherwise descend into an over-deep nested
		// table and re-anchor it at depth 2.
		assertWithinDepth(view, NESTED_DEPTH, Collections.newSetFromMap(new IdentityHashMap<ViewDef,Boolean>()));

		view.validate();
	}

	/**
	 * Descends {@code view}'s nested-table graph, failing closed on a cycle or on a table deeper than
	 * {@link #MAX_DEPTH}.
	 *
	 * <p>
	 * {@code path} holds the views on the <b>current</b> descent path by identity and is unwound on the way back
	 * out, so reaching the same {@link ViewDef} instance from two sibling branches is a legal DAG while reaching it
	 * again on the same path is a cycle.
	 *
	 * @param view The view at this level.  Must not be <jk>null</jk>.
	 * @param depth This view's table depth (the enclosing root table is 1).
	 * @param path The views on the current descent path, by identity.
	 */
	private static void assertWithinDepth(ViewDef view, int depth, Set<ViewDef> path) {
		if (!path.add(view))
			throw iaex("NestedTableDef nesting is cyclic: view '%s' appears twice on one nesting path.", view.id);
		try {
			if (depth > MAX_DEPTH)
				throw iaex("NestedTableDef nesting exceeds the maximum depth of %s: view '%s' would be at depth %s "
					+ "(a nested view may not itself declare a nested table).", MAX_DEPTH, view.id, depth);
			if (view.details == null || view.details.sections == null)
				return;
			for (var s : view.details.sections) {
				if (s != null && s.table != null && s.table.view != null)
					assertWithinDepth(s.table.view, depth + 1, path);
			}
		} finally {
			path.remove(view);
		}
	}

	/**
	 * Whether {@code dataUrl} is a legal nested data endpoint: a same-origin relative path, or a {@code servlet:}
	 * path.  Rejects absolute URLs ({@code ://}), protocol-relative ({@code //}) prefixes, foreign schemes
	 * ({@code javascript:} / {@code data:} / ...), and {@code ..} path segments.
	 *
	 * @param dataUrl The candidate url.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a legal nested data endpoint.
	 */
	public static boolean isSafeNestedDataUrl(String dataUrl) {
		if (dataUrl == null || dataUrl.isBlank())
			return false;
		if (dataUrl.contains("://"))
			return false;
		if (dataUrl.startsWith("//"))
			return false;
		var colon = dataUrl.indexOf(':');
		var slash = dataUrl.indexOf('/');
		if (colon >= 0 && (slash < 0 || colon < slash)) {
			// A scheme is present (colon before any slash).  Only 'servlet:' is honored.
			if (!"servlet".equals(dataUrl.substring(0, colon)))
				return false;
		}
		for (var seg : dataUrl.split("/", -1)) {
			if ("..".equals(seg))
				return false;
		}
		return true;
	}
}
