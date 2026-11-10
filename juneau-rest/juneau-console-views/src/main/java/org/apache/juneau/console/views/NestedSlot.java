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
package org.apache.juneau.console.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * Serializes a {@link TableSpec} into the nested-table slot a tabbed {@link RowDetail} carries.
 *
 * <p>
 * The slot holds the table shell ({@code view}), the sidecar metadata the client builds the nested table from
 * ({@code meta}) and, when declared, the nested table's selection and row detail.  A nested table sits at depth two;
 * a table that itself declares a nested-table tab would sit at depth three and is rejected.
 */
@SuppressWarnings({
	"java:S1192" // Map keys read more clearly inline than as constants.
})
final class NestedSlot {

	/** The deepest table nesting: the page table is depth one, a table inside its row detail is depth two. */
	static final int MAX_NESTED_DEPTH = 2;

	private NestedSlot() {}

	/**
	 * Builds the nested slot.
	 *
	 * @param view The nested table.  Must declare a rows data URL and at least one column.
	 * @param scopeParam The query parameter carrying the parent row id.
	 * @return The slot map, in fixed key order.
	 * @throws IllegalArgumentException If the table is unusable or would exceed {@link #MAX_NESTED_DEPTH}.
	 */
	@SuppressWarnings({
		"java:S3776" // Branching is inherent to of(), which validates and reshapes the nested table body.
	})
	static Map<String,Object> of(TableSpec view, String scopeParam) {
		if (view == null)
			throw iaex("A nested table is required.");
		var body = view.toCardBody();
		if (ib((String)body.get("dataUrl")))
			throw iaex("Nested table '%s' needs a rows data URL.", view.id());
		view.validate();
		var detail = body.get("detail");
		if (detail instanceof Map<?,?> dm && dm.get("tabs") instanceof List<?> tl)
			for (var t : tl)
				if (t instanceof Map<?,?> tm && "table".equals(tm.get("kind")))
					throw iaex("Nested table '%s' declares a nested-table tab; tables nest at most %s deep (MAX_NESTED_DEPTH).",
						view.id(), MAX_NESTED_DEPTH);

		var cols = new ArrayList<Map<String,Object>>();
		for (var c : (List<?>)body.get("columns")) {
			var cm = new LinkedHashMap<String,Object>();
			for (var e : ((Map<?,?>)c).entrySet()) {
				var k = (String)e.getKey();
				var renamed = k;
				if ("key".equals(k))
					renamed = "data";
				else if ("label".equals(k))
					renamed = "title";
				cm.put(renamed, e.getValue());
			}
			cols.add(cm);
		}

		var shell = new LinkedHashMap<String,Object>();
		shell.put("id", view.id());
		shell.put("columns", cols);

		var meta = new LinkedHashMap<String,Object>();
		meta.put("contractVersion", ViewsMixin.CONTRACT_VERSION);
		meta.put("id", view.id());
		for (var e : body.entrySet()) {
			var k = e.getKey();
			if (! NOT_META.contains(k) && ! "columns".equals(k))
				meta.put(k, e.getValue());
		}
		meta.put("columns", cols);

		var n = new LinkedHashMap<String,Object>();
		n.put("contractVersion", "2");
		n.put("parentScopeParam", scopeParam);
		n.put("view", shell);
		n.put("meta", meta);
		if (body.get("selection") != null)
			n.put("selection", body.get("selection"));
		if (detail != null)
			n.put("detail", detail);
		return n;
	}

	private static final Set<String> NOT_META = Set.of("selection", "detail", "bulk", "model", "quickStats", "columnConfig",
		"pollIntervalMs");
}
