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
 * Builds the standard table ribbon.
 *
 * <p>
 * {@code [search] [create] [filters] [export: copy/csv/link + extras] [view: collapse / columns / reset] [refresh]}
 *
 * <p>
 * Each slot has a canonical group, so the ribbon sorts the same way everywhere:
 *
 * <table class='styled'>
 * 	<caption>Ribbon slots</caption>
 * 	<tr><th>Slot</th><th>Group</th><th>Item ids</th></tr>
 * 	<tr><td>search</td><td>{@code search}</td><td>{@code search}</td></tr>
 * 	<tr><td>create</td><td>{@code entity}</td><td>your {@link Standard#create(RibbonItem...)} items</td></tr>
 * 	<tr><td>filters</td><td>{@code filter}</td><td>your {@link Standard#filter(RibbonItem...)} items</td></tr>
 * 	<tr><td>export</td><td>{@code export}</td><td>{@code export} (buttons {@code copy}, {@code csv} plus extras), {@code link}</td></tr>
 * 	<tr><td>view</td><td>{@code viewing}</td><td>{@code collapse}, {@code columns}, {@code reset}</td></tr>
 * 	<tr><td>refresh</td><td>{@code page}</td><td>{@code refresh}</td></tr>
 * </table>
 *
 * <p>
 * Every item carries an explicit id, so the output describes itself.  The ids {@code collapse} and {@code reset}
 * are explicit overrides of the stock defaults {@code collapseAll} and {@code resetDefaults}.  {@code build()}
 * replaces the group of every item you pass to {@code create} or {@code filter} with the slot's group.
 *
 * <p>
 * The {@code copy} and {@code csv} buttons need the DataTables Buttons extension, so a page using the preset must
 * list the {@code datatables-buttons} toolkit pack.  Without it the export cluster is omitted at runtime and the link
 * button still renders.  The collapse button is hidden at runtime on a table with no row detail, and the columns and
 * reset buttons on a table without {@code columnConfig}.
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bjava'>
 * 	TableSpec.<jsm>create</jsm>(<js>"pets"</js>)
 * 		.ribbon(Ribbons.<jsm>standard</jsm>()
 * 			.create(RibbonItem.<jsm>dialog</jsm>(<js>"add-pet"</js>).title(<js>"Add pet"</js>))
 * 			.export(<js>"excel"</js>, <js>"pdf"</js>)
 * 			.build());
 * </p>
 */
@SuppressWarnings({
	"java:S1192" // Ribbon item ids read more clearly inline than as constants.
})
public final class Ribbons {

	private static final List<String> VALID_IDS = List.of(
		"search", "export", "copy", "csv", "excel", "pdf", "link", "collapse", "columns", "reset", "refresh");

	private Ribbons() {}

	/**
	 * Starts the standard preset.
	 *
	 * @return A new builder.
	 */
	public static Standard standard() {
		return new Standard();
	}

	/** Builder for the standard table ribbon. */
	public static final class Standard {

		private final List<RibbonItem> create = new ArrayList<>();
		private final List<RibbonItem> filter = new ArrayList<>();
		private final List<String> extras = new ArrayList<>();
		private final Set<String> without = new LinkedHashSet<>();
		private String refreshId = "refresh";

		private Standard() {}

		/**
		 * Adds items to the create slot (group {@code entity}); repeatable.
		 *
		 * <p>
		 * Ids of items added here are not checked for duplicates by the builder; build-time id validation applies
		 * to the stock items only, so keep the ids you pass unique among themselves and against the stock ids.
		 *
		 * @param items The items.
		 * @return This object.
		 */
		public Standard create(RibbonItem...items) {
			create.addAll(List.of(items));
			return this;
		}

		/**
		 * Adds items to the filter slot (group {@code filter}); repeatable.
		 *
		 * @param items The items.
		 * @return This object.
		 */
		public Standard filter(RibbonItem...items) {
			filter.addAll(List.of(items));
			return this;
		}

		/**
		 * Adds optional export buttons (for example {@code "excel"}, {@code "pdf"}) after copy and csv.
		 *
		 * <p>
		 * An optional button is omitted at runtime when its library is missing.
		 *
		 * @param buttons The buttons, each {@code excel} or {@code pdf}.
		 * @return This object.
		 */
		public Standard export(String...buttons) {
			extras.addAll(List.of(buttons));
			return this;
		}

		/**
		 * Drops a preset item or export button by id.
		 *
		 * <p>
		 * Valid ids are {@code search}, {@code export}, {@code copy}, {@code csv}, {@code excel}, {@code pdf},
		 * {@code link}, {@code collapse}, {@code columns}, {@code reset}, {@code refresh}, and the id given to
		 * {@link #refreshId(String)}.  An unknown id is rejected by {@link #build()}.
		 *
		 * @param id The id.
		 * @return This object.
		 */
		public Standard without(String id) {
			without.add(id);
			return this;
		}

		/**
		 * Renames the refresh item (default {@code "refresh"}).
		 *
		 * @param id The id.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Standard refreshId(String id) {
			if (ib(id))
				throw new IllegalArgumentException("Ribbons refresh id must not be null or blank.");
			refreshId = id;
			return this;
		}

		/**
		 * Builds the items in slot order, each with its canonical group.
		 *
		 * @return An immutable list, for {@link TableSpec#ribbon(List)}.
		 * @throws IllegalArgumentException If {@link #without(String)} named an id that is not a preset id.
		 */
		@SuppressWarnings({
			"java:S3776" // Branching is inherent to build(), which assembles the ribbon from many optional items.
		})
		public List<RibbonItem> build() {
			for (var w : without)
				if (! VALID_IDS.contains(w) && ! w.equals(refreshId))
					throw new IllegalArgumentException("Unknown ribbon preset id '" + w + "'. Valid ids: " + VALID_IDS + " and the refresh id '" + refreshId + "'.");
			var out = new ArrayList<RibbonItem>();
			if (! without.contains("search"))
				out.add(RibbonItem.search().id("search").group("search"));
			create.forEach(i -> out.add(i.group("entity")));
			filter.forEach(i -> out.add(i.group("filter")));
			if (! without.contains("export")) {
				var buttons = new ArrayList<String>();
				if (! without.contains("copy"))
					buttons.add("copy");
				if (! without.contains("csv"))
					buttons.add("csv");
				var optional = extras.stream().filter(x -> ! without.contains(x)).toList();
				if (! buttons.isEmpty() || ! optional.isEmpty()) {
					var e = RibbonItem.export(buttons.toArray(String[]::new)).id("export").group("export");
					if (! optional.isEmpty())
						e.optional(optional.toArray(String[]::new));
					out.add(e);
				}
			}
			if (! without.contains("link"))
				out.add(RibbonItem.linkButton().id("link").group("export"));
			if (! without.contains("collapse"))
				out.add(RibbonItem.collapseAll().id("collapse").group("viewing"));
			if (! without.contains("columns"))
				out.add(RibbonItem.columnSettings().id("columns").group("viewing"));
			if (! without.contains("reset"))
				out.add(RibbonItem.resetDefaults().id("reset").group("viewing"));
			if (! without.contains(refreshId))
				out.add(RibbonItem.refresh().id(refreshId).group("page"));
			return List.copyOf(out);
		}
	}
}
