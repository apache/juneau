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
 * The raw, unresolved shape of a {@link BeanQuery#getSearch() search} string (design §3, §4): a tree of
 * {@code column=expression} leaves combined by {@code $or}/{@code $and}/{@code $not} groups.
 *
 * <p>
 * {@link SearchParser} produces this tree from a wire string; {@link SearchItemBuilder} builds and merges it in
 * memory for {@link BeanQuery.Builder} / {@link BeanQuery.GroupBuilder}.  {@code QueryResolver}
 * walks it, resolving each leaf's column and operator, to produce the validated {@link Filter} tree.
 */
sealed interface SearchItem {

	/** One {@code column=expression} leaf.  {@code expression} is the raw, unparsed per-column {@code $}-expression. */
	record Leaf(String column, String expression) implements SearchItem {}

	/** A {@code $and(item,...)} group.  {@code items} has two or more entries; {@link SearchParser} enforces this on parse. */
	record And(List<SearchItem> items) implements SearchItem {
		public And(List<SearchItem> items) {
			this.items = List.copyOf(items);
		}
	}

	/** A {@code $or(item,...)} group.  {@code items} has two or more entries; {@link SearchParser} enforces this on parse. */
	record Or(List<SearchItem> items) implements SearchItem {
		public Or(List<SearchItem> items) {
			this.items = List.copyOf(items);
		}
	}

	/** A {@code $not(item)} group wrapping exactly one item. */
	record Not(SearchItem item) implements SearchItem {}
}
