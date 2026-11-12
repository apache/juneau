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
package org.apache.juneau.commons.svl;

import java.util.*;

/**
 * A typed, structured value (list, block or table) that an SVL expression function can return.
 *
 * <p>
 * The core never emits markup.  {@link #toString()} gives a plain-text fallback; a layer that understands richer
 * output (such as the J0439 template layer) interprets {@link #kind()}, {@link #items()} / {@link #rows()} and
 * {@link #options()} itself.
 *
 * <p>
 * Instances are immutable; all inputs are defensively copied (shallow: the elements themselves are not copied).
 */
public final class Structure {

	/** The kind of structure. */
	public enum Kind {
		/** Unordered list. */ UL,
		/** Ordered list. */ OL,
		/** A single block of content. */ BLOCK,
		/** A table with column headers and rows. */ TABLE
	}

	private final Kind kind;
	private final List<?> items;
	private final List<String> columns;
	private final List<List<?>> rows;
	private final Map<String,Object> options;

	private Structure(Kind kind, List<?> items, List<String> columns, List<List<?>> rows, Map<String,Object> options) {
		this.kind = kind;
		this.items = items;
		this.columns = columns;
		this.rows = rows;
		this.options = options == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(options));
	}

	/**
	 * Creates an unordered list.
	 *
	 * @param items The items.  Must not be <jk>null</jk>.
	 * @return A new structure.
	 */
	public static Structure ul(List<?> items) { return ul(items, null); }

	/**
	 * Creates an unordered list with options.
	 *
	 * @param items The items.  Must not be <jk>null</jk>.
	 * @param options Layer-defined options.  May be <jk>null</jk>.
	 * @return A new structure.
	 */
	public static Structure ul(List<?> items, Map<String,Object> options) {
		return new Structure(Kind.UL, copy(items), null, null, options);
	}

	/**
	 * Creates an ordered list.
	 *
	 * @param items The items.  Must not be <jk>null</jk>.
	 * @return A new structure.
	 */
	public static Structure ol(List<?> items) { return ol(items, null); }

	/**
	 * Creates an ordered list with options.
	 *
	 * @param items The items.  Must not be <jk>null</jk>.
	 * @param options Layer-defined options.  May be <jk>null</jk>.
	 * @return A new structure.
	 */
	public static Structure ol(List<?> items, Map<String,Object> options) {
		return new Structure(Kind.OL, copy(items), null, null, options);
	}

	/**
	 * Creates a block holding a single content item.
	 *
	 * @param content The content.  May be <jk>null</jk>.
	 * @return A new structure.
	 */
	public static Structure block(Object content) { return block(content, null); }

	/**
	 * Creates a block with options.
	 *
	 * @param content The content.  May be <jk>null</jk>.
	 * @param options Layer-defined options.  May be <jk>null</jk>.
	 * @return A new structure.
	 */
	public static Structure block(Object content, Map<String,Object> options) {
		var l = new ArrayList<Object>(1);
		l.add(content);
		return new Structure(Kind.BLOCK, Collections.unmodifiableList(l), null, null, options);
	}

	/**
	 * Creates a table.
	 *
	 * @param columns The column headers.  Must not be <jk>null</jk>.
	 * @param rows The rows.  Must not be <jk>null</jk>.
	 * @return A new structure.
	 */
	public static Structure table(List<String> columns, List<List<?>> rows) { return table(columns, rows, null); }

	/**
	 * Creates a table with options.
	 *
	 * @param columns The column headers.  Must not be <jk>null</jk>.
	 * @param rows The rows.  Must not be <jk>null</jk>.
	 * @param options Layer-defined options.  May be <jk>null</jk>.
	 * @return A new structure.
	 */
	public static Structure table(List<String> columns, List<List<?>> rows, Map<String,Object> options) {
		Objects.requireNonNull(rows, "rows");
		var r = new ArrayList<List<?>>(rows.size());
		for (var row : rows)
			r.add(copy(Objects.requireNonNull(row, "rows must not contain a null row")));
		return new Structure(Kind.TABLE, null, Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(columns, "columns"))), Collections.unmodifiableList(r), options);
	}

	private static List<?> copy(List<?> l) {
		return Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(l, "items")));
	}

	/**
	 * Returns the kind of this structure.
	 *
	 * @return The kind.
	 */
	public Kind kind() { return kind; }

	/**
	 * Returns the items of a list or block.
	 *
	 * @return An unmodifiable list, or an empty list for a table.
	 */
	@SuppressWarnings({
		"java:S1452" // Items are heterogeneous by design; the wildcard is the public contract
	})
	public List<?> items() { return items == null ? List.of() : items; }

	/**
	 * Returns the column headers of a table.
	 *
	 * @return An unmodifiable list, or an empty list for a non-table.
	 */
	public List<String> columns() { return columns == null ? List.of() : columns; }

	/**
	 * Returns the rows of a table.
	 *
	 * @return An unmodifiable list, or an empty list for a non-table.
	 */
	public List<List<?>> rows() { return rows == null ? List.of() : rows; }

	/**
	 * Returns the layer-defined options.
	 *
	 * @return An unmodifiable map; never <jk>null</jk>.
	 */
	public Map<String,Object> options() { return options; }

	/**
	 * Plain-text fallback.
	 *
	 * <p>
	 * Lists render one item per line prefixed with <c>- </c> or <c>N. </c>; a block renders its content; a table renders
	 * the header and then each row as tab-separated values, one per line.
	 */
	@Override
	public String toString() {
		var sb = new StringBuilder();
		switch (kind) {
			case UL -> {
				for (var o : items)
					line(sb, "- " + o);
			}
			case OL -> {
				var n = 1;
				for (var o : items)
					line(sb, (n++) + ". " + o);
			}
			case BLOCK -> sb.append(items.get(0));
			case TABLE -> {
				line(sb, String.join("\t", columns));
				for (var r : rows) {
					var j = new StringJoiner("\t");
					for (var o : r)
						j.add(String.valueOf(o));
					line(sb, j.toString());
				}
			}
		}
		return sb.toString();
	}

	private static void line(StringBuilder sb, String s) {
		if (! sb.isEmpty())
			sb.append('\n');
		sb.append(s);
	}
}
