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
 * A read-only DataTables view nested inside a {@link DetailSection} of a row-detail panel.
 *
 * <p>
 * The wrapped {@link #view} is an ordinary {@link ViewDef} that the {@code juneau-views.js} runtime instantiates
 * after the enclosing row's detail GET succeeds and the section pane becomes visible.  The nested table runs its own
 * same-origin data GET, scoped to the parent row by merging a single query parameter (named {@link #parentScopeParam},
 * default {@code "parentId"}) carrying the parent row id &mdash; there is no {@code {parentId}} URL template.
 *
 * <p>
 * This slice is deliberately narrow (fold {@code g4}): a nested view is <b>read-only</b>.  It may declare columns,
 * paging, sort, and search, but must not declare {@link ViewDef#rowActions} or {@link ViewDef#columnConfig}, and must
 * be depth-1 (it may not itself declare {@link ViewDef#details}).  Selection and bulk mutation cannot be expressed on
 * a bare {@link ViewDef} and so need no explicit forbid here.
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
	public static final String CONTRACT_VERSION = "1";

	/** DataTables server-side request parameters that a scope parameter must not shadow. */
	private static final Set<String> RESERVED_SCOPE_PARAMS =
		Set.of("draw", "start", "length", "search", "columns", "order", "_");

	/** The nested, read-only view rendered inside the detail section. */
	public ViewDef view;

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
	 * @param view The nested, read-only {@link ViewDef}.  May be <jk>null</jk> (then fails {@link #validate()}).
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
	 * Fail-closed bean validation.
	 *
	 * <p>
	 * Enforces: a non-null nested view; a legal, non-reserved {@link #parentScopeParam}; a non-blank same-origin
	 * relative-or-{@code servlet:} {@link ViewDef#dataUrl}; depth-1 (no nested {@link ViewDef#details}); and the
	 * read-only forbids (no {@link ViewDef#rowActions} / {@link ViewDef#columnConfig}).  Then delegates to
	 * {@link ViewDef#validate()}.
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

		if (view.details != null)
			throw iaex("NestedTableDef view must be depth-1: a nested view may not itself declare row details.");

		if (view.rowActions != null)
			throw iaex("NestedTableDef view is read-only: rowActions are not permitted on a nested view.");
		if (view.columnConfig != null)
			throw iaex("NestedTableDef view is read-only: columnConfig is not permitted on a nested view.");

		view.validate();
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
