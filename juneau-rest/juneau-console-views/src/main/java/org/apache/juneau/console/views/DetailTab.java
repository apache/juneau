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
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.console.*;

/**
 * One tab of a tabbed {@link RowDetail}: a fields grid, a nested table, a chart or a client-rendered pane.
 *
 * <p>
 * The tab is loaded lazily: its content is fetched the first time the user opens it.  A tab id is the slug of its
 * title unless {@link #id(String)} is given.
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bjava'>
 * 	RowDetail.<jsm>inline</jsm>()
 * 		.tab(DetailTab.<jsm>fields</jsm>(<js>"Details"</js>, <js>"id"</js>, <js>"name"</js>, <js>"price"</js>).columns(3))
 * 		.tab(DetailTab.<jsm>client</jsm>(<js>"Labels"</js>, <js>"pet-labels"</js>).endpoint(<js>"/pets/{id}/labels"</js>));
 * </p>
 *
 * @since 10.0.0
 */
public final class DetailTab implements Handled<DetailTab> {

	/** The tab kinds. */
	public enum Kind {

		/** A label/value grid of row fields. */
		FIELDS("fields"),

		/** A nested table scoped to the parent row. */
		TABLE("table"),

		/** A chart fed by an endpoint. */
		CHART("chart"),

		/** A pane drawn by a client-side renderer. */
		CLIENT("client");

		private final String wire;

		Kind(String wire) {
			this.wire = wire;
		}

		/**
		 * Returns the wire token.
		 *
		 * @return The lower-case token.
		 */
		public String wire() {
			return wire;
		}
	}

	/** How a row detail caches the content of its tabs. */
	public enum Cache {

		/** A tab is fetched once per expanded row (the default). */
		PER_ROW("per-row"),

		/** A tab is fetched again each time it is activated. */
		NONE("none");

		private final String wire;

		Cache(String wire) {
			this.wire = wire;
		}

		/**
		 * Returns the wire token.
		 *
		 * @return The token.
		 */
		public String wire() {
			return wire;
		}
	}

	private static final Pattern ID = Pattern.compile("[a-z][a-z0-9-]{0,31}");
	private static final Pattern RENDERER = Pattern.compile("[A-Za-z][A-Za-z0-9._-]{0,63}");

	private record Group(String title, List<String> fields) {}

	private final Kind kind;
	private final String title;
	private String id;
	private String icon;
	private String endpoint;
	private String emptyText;
	private String scopeParam = "parentId";
	private String renderer;
	private int columns;
	private boolean refreshOnPoll;
	private boolean countFromRows;
	private boolean autoFields;
	private final List<String> fields = new ArrayList<>();
	private final List<Group> groups = new ArrayList<>();
	private TableSpec view;
	private RegionDef region;
	private Function<String,Integer> counter;
	private List<VisibilityRule> visibleWhen;
	private final ModelBlock model;

	private DetailTab(Kind kind, String title) {
		if (ib(title))
			throw iaex("DetailTab title must not be null or blank.");
		this.kind = kind;
		this.title = title;
		this.model = new ModelBlock("detailtab", title);
	}

	@Override /* Handled */
	public ModelBlock modelBlock() { return model; }

	@Override /* Handled */
	public DetailTab self() { return this; }

	/**
	 * Starts a fields-grid tab.
	 *
	 * @param title The tab title.  Must not be blank.
	 * @param fields The row field names shown, in order.  Unique within the tab.
	 * @return A new tab.
	 */
	public static DetailTab fields(String title, String...fields) {
		var t = new DetailTab(Kind.FIELDS, title);
		t.fields.addAll(Arrays.asList(fields));
		return t;
	}

	/** Starts the generated Details tab whose field list the owning table fills in. */
	static DetailTab autoFields(String title, List<String> fields) {
		var t = new DetailTab(Kind.FIELDS, title);
		t.autoFields = true;
		t.fields.addAll(fields);
		return t;
	}

	/**
	 * Starts a nested-table tab.
	 *
	 * @param title The tab title.  Must not be blank.
	 * @param view The nested table; it must declare a rows data URL.
	 * @return A new tab.
	 */
	public static DetailTab table(String title, TableSpec view) {
		var t = new DetailTab(Kind.TABLE, title);
		t.view = view;
		return t;
	}

	/**
	 * Starts a chart tab.
	 *
	 * @param title The tab title.  Must not be blank.
	 * @param region The chart region.
	 * @return A new tab.
	 */
	public static DetailTab chart(String title, RegionDef region) {
		var t = new DetailTab(Kind.CHART, title);
		t.region = region;
		return t;
	}

	/**
	 * Starts a client-rendered tab.
	 *
	 * @param title The tab title.  Must not be blank.
	 * @param renderer The name of a renderer registered in the browser; letters, digits, {@code . _ -}, at most 64.
	 * @return A new tab.
	 */
	public static DetailTab client(String title, String renderer) {
		var t = new DetailTab(Kind.CLIENT, title);
		t.renderer = renderer;
		return t;
	}

	/**
	 * Sets the tab id (default: the slug of the title).
	 *
	 * @param value The id; lower-case letter first, then lower-case letters, digits and {@code -}, at most 32.
	 * @return This object.
	 */
	public DetailTab id(String value) {
		id = value;
		return this;
	}

	/**
	 * Sets the tab icon.
	 *
	 * @param value An icon name.
	 * @return This object.
	 */
	public DetailTab icon(String value) {
		icon = value;
		return this;
	}

	/**
	 * Sets the per-tab GET endpoint.
	 *
	 * @param value A same-origin path template; {@code {id}} is replaced by the row id.
	 * @return This object.
	 */
	public DetailTab endpoint(String value) {
		endpoint = value;
		return this;
	}

	/**
	 * Sets the text shown when the tab has nothing to show.
	 *
	 * @param value The text.
	 * @return This object.
	 */
	public DetailTab emptyText(String value) {
		emptyText = value;
		return this;
	}

	/**
	 * Sets the grid column count of a fields tab.
	 *
	 * @param n The count, 1 to 4; 0 inherits the detail's count.
	 * @return This object.
	 */
	public DetailTab columns(int n) {
		columns = n;
		return this;
	}

	/**
	 * Groups some of a fields tab's fields under a heading.  Fields in no group follow, under no heading.
	 *
	 * @param groupTitle The heading.
	 * @param groupFields The fields; each must be in the tab's field list and in no other group.
	 * @return This object.
	 */
	public DetailTab group(String groupTitle, String...groupFields) {
		groups.add(new Group(groupTitle, List.of(groupFields)));
		return this;
	}

	/**
	 * Sets the query parameter carrying the parent row id to a nested table.
	 *
	 * @param value The parameter name (default {@code parentId}).
	 * @return This object.
	 */
	public DetailTab scopeParam(String value) {
		scopeParam = value;
		return this;
	}

	/**
	 * Shows the nested table's row count in the tab label once the tab has loaded.  Table tabs only.
	 *
	 * @return This object.
	 */
	public DetailTab countFromRows() {
		countFromRows = true;
		return this;
	}

	/**
	 * Sets a server-side counter shown in the tab label.  The value travels in the detail's {@code counts}.
	 *
	 * @param f Maps a row id to a count.
	 * @return This object.
	 */
	public DetailTab count(Function<String,Integer> f) {
		counter = f;
		return this;
	}

	/**
	 * Sets whether the tab reloads when the table polls.
	 *
	 * @param value The flag (default <jk>false</jk>).
	 * @return This object.
	 */
	public DetailTab refreshOnPoll(boolean value) {
		refreshOnPoll = value;
		return this;
	}

	/**
	 * Hides the tab unless every rule matches.  Presentation only; it never authorises.
	 *
	 * @param rules The rules.
	 * @return This object.
	 */
	public DetailTab visibleWhen(VisibilityRule...rules) {
		visibleWhen = l(rules);
		return this;
	}

	String resolvedId() {
		return id != null ? id : slug(title);
	}

	static String slug(String t) {
		return t.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
	}

	Kind kind() {
		return kind;
	}

	boolean hasCounter() {
		return counter != null;
	}

	Integer count(String rowId) {
		return counter == null ? null : counter.apply(rowId);
	}

	List<String> fieldNames() {
		return List.copyOf(fields);
	}

	private void validate() {
		var rid = resolvedId();
		if (! ID.matcher(rid).matches())
			throw iaex("DetailTab '%s' id '%s' must match %s.", title, rid, ID.pattern());
		if (endpoint != null && ! RegionDef.isSafeDetailEndpoint(endpoint))
			throw iaex("DetailTab '%s' endpoint '%s' is not a same-origin path.", rid, endpoint);
		if (countFromRows && kind != Kind.TABLE)
			throw iaex("DetailTab '%s' countFromRows applies to table tabs only.", rid);
		switch (kind) {
			case FIELDS -> validateFields(rid);
			case TABLE -> {
				if (view == null)
					throw iaex("DetailTab '%s' needs a nested table.", rid);
				if (ib(scopeParam))
					throw iaex("DetailTab '%s' scopeParam must not be blank.", rid);
			}
			case CHART -> {
				if (region == null)
					throw iaex("DetailTab '%s' needs a chart region.", rid);
			}
			case CLIENT -> {
				if (renderer == null || ! RENDERER.matcher(renderer).matches())
					throw iaex("DetailTab '%s' renderer '%s' must match %s.", rid, renderer, RENDERER.pattern());
			}
		}
	}

	private void validateFields(String rid) {
		if (columns != 0 && (columns < 1 || columns > 4))
			throw iaex("DetailTab '%s' columns must be 1 to 4, was %s.", rid, columns);
		if (fields.isEmpty() && ! autoFields)
			throw iaex("DetailTab '%s' needs at least one field.", rid);
		var seen = new HashSet<String>();
		for (var f : fields)
			if (ib(f) || ! seen.add(f))
				throw iaex("DetailTab '%s' field '%s' is blank or repeated.", rid, f);
		var grouped = new HashSet<String>();
		for (var g : groups)
			for (var f : g.fields()) {
				if (! seen.contains(f))
					throw iaex("DetailTab '%s' group '%s' names field '%s' which is not in the tab.", rid, g.title(), f);
				if (! grouped.add(f))
					throw iaex("DetailTab '%s' field '%s' is in more than one group.", rid, f);
			}
	}

	/**
	 * Serializes this tab to its wire shape.
	 *
	 * @return The tab map.
	 * @throws IllegalArgumentException If the tab is invalid.
	 */
	public Map<String,Object> toMap() {
		validate();
		var m = new LinkedHashMap<String,Object>();
		m.put("id", resolvedId());
		m.put("kind", kind.wire());
		m.put("title", title);
		if (icon != null)
			m.put("icon", icon);
		if (kind == Kind.FIELDS) {
			if (columns != 0)
				m.put("columns", columns);
			m.put("fields", List.copyOf(fields));
			if (! groups.isEmpty()) {
				var gl = new ArrayList<Map<String,Object>>();
				for (var g : groups) {
					var gm = new LinkedHashMap<String,Object>();
					gm.put("title", g.title());
					gm.put("fields", g.fields());
					gl.add(gm);
				}
				m.put("groups", gl);
			}
		}
		if (kind == Kind.TABLE)
			m.put("nested", NestedSlot.of(view, scopeParam));
		if (countFromRows)
			m.put("count", "rows");
		if (emptyText != null)
			m.put("emptyText", emptyText);
		if (endpoint != null)
			m.put("endpoint", endpoint);
		if (renderer != null && kind == Kind.CLIENT)
			m.put("renderer", renderer);
		if (refreshOnPoll)
			m.put("refreshOnPoll", true);
		if (kind == Kind.CHART) {
			var r = new LinkedHashMap<>(region.toContractMap());
			r.remove("contractVersion");
			m.put("region", r);
		}
		if (visibleWhen != null && ! visibleWhen.isEmpty())
			m.put("visibleWhen", VisibilityRule.toMaps(visibleWhen));
		model.validate();
		var mm = model.toMap();
		if (mm != null)
			m.put("model", mm);
		return m;
	}
}
