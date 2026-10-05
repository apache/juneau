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
package org.apache.juneau.commons.beanquery;

import java.util.*;

/**
 * The same-column {@code $and} merging logic (design D2) shared by {@link BeanQuery.Builder} and
 * {@link BeanQuery.GroupBuilder}: an ordered list of {@link SearchItem}s at one nesting level (top level, or inside
 * one group), where adding a leaf on a column already present at that level replaces it with both conditions
 * combined by {@code $and(...)} &mdash; appending into an existing {@code $and} rather than nesting a new one
 * inside it &mdash; while a group is always appended as a new, distinct sibling (groups never merge with each
 * other or with a leaf).
 */
final class SearchItemBuilder {

	private final List<SearchItem> items = new ArrayList<>();
	private final Map<String,Integer> leafIndex = new HashMap<>();

	/** Adds a leaf condition on {@code column}, merging via {@code $and(...)} if that column is already present. */
	void addLeaf(String column, String expression) {
		var idx = leafIndex.get(column);
		if (idx == null) {
			leafIndex.put(column, items.size());
			items.add(new SearchItem.Leaf(column, expression));
		} else {
			var existing = (SearchItem.Leaf) items.get(idx);
			items.set(idx, new SearchItem.Leaf(column, mergeExpression(existing.expression(), expression)));
		}
	}

	/**
	 * Appends a group as a new sibling.  A <jk>null</jk> group (an empty or fully-blank lambda) adds nothing.
	 *
	 * <p>
	 * Groups are never registered in {@link #leafIndex}: a column named inside a nested group is invisible to this
	 * level's same-column merge, exactly like {@link SearchParser}'s own duplicate-column check only looking at
	 * top-level leaves.
	 */
	void addGroup(SearchItem group) {
		if (group != null)
			items.add(group);
	}

	/** Seeds this builder's state from an already-parsed top-level item list (used by {@code BeanQuery.copy()}). */
	void seed(List<SearchItem> parsed) {
		for (var item : parsed) {
			if (item instanceof SearchItem.Leaf leaf)
				addLeaf(leaf.column(), leaf.expression());
			else
				addGroup(item);
		}
	}

	/** The accumulated items at this level, in insertion order. */
	List<SearchItem> items() {
		return List.copyOf(items);
	}

	// Splices raw expression text, not a parsed tree: a Leaf's expression is the opaque, unparsed per-column
	// $-expression (see SearchItem.Leaf / SearchParser), so string-level $and(...) detection is the only option here.
	private static String mergeExpression(String existing, String next) {
		if (existing.startsWith("$and(") && existing.endsWith(")"))
			return existing.substring(0, existing.length() - 1) + "," + next + ")";
		return "$and(" + existing + "," + next + ")";
	}
}
