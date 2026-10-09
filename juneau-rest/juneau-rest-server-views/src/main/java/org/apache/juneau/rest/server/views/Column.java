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

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.collections.*;

/**
 * One datatable column's search configuration (design §4.3) &mdash; its value {@link SearchType} and the optional
 * per-column {@link SearchOperatorSet}.
 *
 * <h5 class='section'>Operator gating (design §4.3)</h5>
 * <p>
 * A column's offerable operators are modeled by a single {@link SearchOperatorSet}, never a string allow-list or a
 * separate custom-operator map.  A custom operator is simply a {@link SearchOperator} placed inside a set via
 * {@link SearchOperatorSet#standard()}, {@link SearchOperatorSet#with(SearchOperator)}, or
 * {@link SearchOperatorSet#of(SearchOperator...)}.
 * <ul>
 * 	<li><b>Set absent</b> (default): the column offers the entire type-applicable {@link SearchOperatorSet#standard()
 * 		standard} universe (or, when resolved against a table, the table-level set &mdash; see {@link ViewDef}).
 * 	<li><b>Set present</b> ({@link #operators(SearchOperatorSet)}): the column's set entirely <b>replaces</b> the
 * 		table-level set for this column (design §4.3); the effective operators are that set's
 * 		{@link SearchOperatorSet#forType(SearchType) type-applicable} members.
 * </ul>
 * <p>
 * An operator not on the effective set is a <b>reject</b> (the popup marks it invalid), never a silent rewrite
 * &mdash; see {@link #offers(String)}.
 *
 * <h5 class='section'>Mutability</h5>
 * <p>
 * Deliberately mutable, like {@link ViewDef} and {@link RegionDef}: a {@link Column} is a server-side authoring
 * bean built up once via fluent setters while constructing a view definition (design §4.1&ndash;§4.3), not an
 * immutable value object.  Callers must not mutate one concurrently from more than one thread, and must not reuse
 * one across requests unless every request treats it as read-only after construction.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A searchable "status" column offering only $eq and $in, plus a custom "$mine" operator.</jc>
 * 	Column <jv>status</jv> = Column.<jsm>create</jsm>(<js>"status"</js>)
 * 		.label(<js>"Status"</js>)
 * 		.searchType(SearchType.<jsf>ID</jsf>)
 * 		.operators(SearchOperatorSet.<jsm>of</jsm>(
 * 			SearchOperatorSet.<jsm>standard</jsm>().get(<js>"$eq"</js>),
 * 			SearchOperatorSet.<jsm>standard</jsm>().get(<js>"$in"</js>),
 * 			SearchOperator.<jsm>create</jsm>(<js>"$mine"</js>, <js>"Assigned to me"</js>).types(SearchType.<jsf>ID</jsf>).build()));
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link SearchOperator}
 * 	<li class='jc'>{@link SearchOperatorSet}
 * 	<li class='jc'>{@link ViewDef}
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1168" // null (not an empty map) is the wire contract: a non-searchable column omits the search block; an empty map would read as searchable.
})
public final class Column {

	private final String name;
	private String label;
	private SearchType searchType;
	private SearchOperatorSet operators;  // null means "inherit the table-level set".
	private Render render;
	private String href;
	private String className;
	private boolean defaultVisible = true;

	private Column(String name) {
		this.name = name;
	}

	/**
	 * Starts a new column definition.
	 *
	 * @param name The column name (matches the row-data key).  Must not be
	 * 	<jk>null</jk> or blank.
	 * @return A new column, not searchable until a {@link #searchType(SearchType)} is set.
	 * @throws IllegalArgumentException If {@code name} is <jk>null</jk> or blank.
	 */
	public static Column create(String name) {
		req(inb(name), "Column name must not be null or blank.");
		return new Column(name);
	}

	/**
	 * Sets the human-readable column label.
	 *
	 * @param value The label.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Column label(String value) {
		label = value;
		return this;
	}

	/**
	 * Sets the column's value type, which governs both leaf semantics and the offerable built-in operators.  A
	 * column with no search type is not searchable.
	 *
	 * @param value The value type.  Can be <jk>null</jk> to mark the column non-searchable.
	 * @return This object.
	 */
	public Column searchType(SearchType value) {
		searchType = value;
		return this;
	}

	/**
	 * Sets this column's own {@link SearchOperatorSet} (design §4.3) &mdash; it entirely replaces the table-level
	 * set for this column.
	 *
	 * @param value The operator set.  A <jk>null</jk> value clears it back to <i>absent</i> (inherit the table-level
	 * 	set, or {@link SearchOperatorSet#standard()} when there is none).
	 * @return This object.
	 */
	public Column operators(SearchOperatorSet value) {
		operators = value;
		return this;
	}

	/**
	 * Sets this column's cell renderer from the compact {@code "id:field"} string sugar.
	 *
	 * @param spec The render-id string, e.g. {@code "tag:status"}.  Must not be <jk>null</jk> or blank.
	 * @return This object.
	 */
	public Column render(String spec) {
		render = Render.parse(spec);
		return this;
	}

	/**
	 * Sets this column's cell renderer.
	 *
	 * @param value The renderer.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public Column render(Render value) {
		render = value;
		return this;
	}

	/**
	 * Sets the row-data interpolation template this column's rendered cell links to.
	 *
	 * <p>
	 * Requires a {@link #render(String) render} to already be set; that is checked by {@link #toCatalogMap()}, since a
	 * column built in pieces may set {@code href} before {@code render}.
	 *
	 * @param template The href template, e.g. {@code "{incidentUrl}"}.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public Column href(String template) {
		href = template;
		return this;
	}

	/**
	 * Sets a CSS class applied to this column's cells.
	 *
	 * @param css The class name(s).  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public Column className(String css) {
		className = css;
		return this;
	}

	/**
	 * Sets whether this column starts visible.
	 *
	 * <p>
	 * Defaults to <jk>true</jk>; only {@code false} is ever emitted by {@link #toCatalogMap()}.
	 *
	 * <p>
	 * A {@code false} value only hides the column when the client's {@code columnConfig} (juneau-config.js) is on; without it
	 * the column stays visible.
	 *
	 * @param v Whether the column starts visible.
	 * @return This object.
	 */
	public Column defaultVisible(boolean v) {
		defaultVisible = v;
		return this;
	}

	/**
	 * Builds this column's catalog entry: {@code key} and {@code label} plus the display ({@code render},
	 * {@code href}, {@code className}, {@code defaultVisible}) and search ({@code searchType},
	 * {@code searchOperators}, {@code customOperators}) metadata, flattened onto one entry, in the form the
	 * {@code datatables} card type resolves.
	 *
	 * <p>
	 * {@code searchOperators} is a list of operator <i>names</i> and {@code customOperators} a list of
	 * {@code {name,help}} maps for the custom ones.  Both are emitted only when this column has its own
	 * {@link #operators(SearchOperatorSet) operator set}; a column inheriting the table-level set emits just
	 * {@code searchType}, and a column with no search type emits neither.  See
	 * {@link #toCatalogMap(SearchOperatorSet)} to resolve the table-level set into the entry.
	 *
	 * <p>
	 * {@code label} falls back to {@link #name()} when unset.
	 *
	 * @return The catalog entry.
	 * @throws IllegalArgumentException If {@code href} is set without a {@code render}.
	 */
	public JsonMap toCatalogMap() {
		return toCatalogMap(null);
	}

	/**
	 * Builds this column's catalog entry like {@link #toCatalogMap()}, but emits the column's <i>effective</i>
	 * operator set: its own set if it has one, else {@code tableDefault}.
	 *
	 * <p>
	 * This is how a table-level set reaches the {@code datatables} card type, which reads operators per column.
	 * With no column set and a <jk>null</jk> {@code tableDefault}, only {@code searchType} is emitted.
	 *
	 * @param tableDefault The table-level operator set inherited by a column with none of its own.  Can be
	 * 	<jk>null</jk>.
	 * @return The catalog entry.
	 * @throws IllegalArgumentException If {@code href} is set without a {@code render}.
	 */
	public JsonMap toCatalogMap(SearchOperatorSet tableDefault) {
		if (href != null && render == null)
			throw iaex("Column '%s' sets href '%s' without a render; use render(\"linked\").", name, href);
		var m = new JsonMap();
		m.put("key", name);
		m.put("label", label != null ? label : name);
		if (render != null)
			m.put("render", render);
		if (href != null)
			m.put("href", href);
		if (className != null)
			m.put("className", className);
		if (! defaultVisible)
			m.put("defaultVisible", false);
		if (searchType != null) {
			m.put("searchType", searchType.wire());
			var effective = operators != null ? operators : tableDefault;
			if (effective != null) {
				var names = new ArrayList<String>();
				var customs = new ArrayList<Map<String,String>>();
				for (var op : effective.forType(searchType)) {
					names.add(op.name());
					if (op.isCustom()) {
						var cm = new LinkedHashMap<String,String>();
						cm.put("name", op.name());
						cm.put("help", op.help());
						customs.add(cm);
					}
				}
				m.put("searchOperators", names);
				if (! customs.isEmpty())
					m.put("customOperators", customs);
			}
		}
		return m;
	}

	/** @return The column name. */
	public String name() {
		return name;
	}

	/** @return The column label, or <jk>null</jk>. */
	public String label() {
		return label;
	}

	/** @return The column value type, or <jk>null</jk> if the column is not searchable. */
	public SearchType searchType() {
		return searchType;
	}

	/**
	 * This column's own operator set, or <jk>null</jk> if absent (inherit the table-level set).
	 *
	 * @return The operator set, or <jk>null</jk>.
	 */
	public SearchOperatorSet operators() {
		return operators;
	}

	/** @return <jk>true</jk> if this column has a search type (and is therefore searchable). */
	public boolean searchable() {
		return searchType != null;
	}

	/**
	 * Resolves the effective operator set the header popup offers for this column (design §4.3), against the
	 * built-in {@link SearchOperatorSet#standard() standard} set as the fallback default.
	 *
	 * @return The effective operators, in the resolved set's order; empty when the column is not searchable.
	 */
	public List<SearchOperator> effectiveOperators() {
		return effectiveOperators(null);
	}

	/**
	 * Resolves the effective operator set the header popup offers for this column (design §4.3), inheriting the
	 * specified table-level set when this column has none of its own.
	 *
	 * @param tableDefault The table-level operator set to inherit when this column has none of its own.  Can be
	 * 	<jk>null</jk>, in which case {@link SearchOperatorSet#standard()} is used.
	 * @return The effective operators, in the resolved set's order; empty when the column is not searchable.
	 */
	public List<SearchOperator> effectiveOperators(SearchOperatorSet tableDefault) {
		if (searchType == null)
			return List.of();
		var inherited = tableDefault != null ? tableDefault : SearchOperatorSet.standard();
		var set = operators != null ? operators : inherited;
		return set.forType(searchType);
	}

	/**
	 * Whether the named operator is on this column's effective set &mdash; i.e. whether the popup offers it (and
	 * accepts it rather than rejecting it as invalid).
	 *
	 * @param opName The operator name, including the leading {@code $}.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the operator is offered for this column.
	 */
	public boolean offers(String opName) {
		if (opName == null)
			return false;
		for (var op : effectiveOperators())
			if (op.name().equals(opName))
				return true;
		return false;
	}

	/**
	 * Builds the per-column {@code search} block for the {@code VIEW_META} wire contract (design §4.3&ndash;§4.5)
	 * directly from author-catalog inputs, without the caller having to touch the search-engine beans.
	 *
	 * <p>
	 * This is the entry point the chrome/authoring layer uses: it takes only the wire token, the optional
	 * allowed-operator names, and the custom operators as plain {@code {name,help}} maps &mdash; so a chrome-only
	 * module that must not depend on the search engine can still emit a column's search block by calling here.
	 * Internally this builds a single {@link SearchOperatorSet} (design §4.3): an allow-list becomes
	 * {@link SearchOperatorSet#of(SearchOperator...)} with exactly the named operators (customs preferred over
	 * same-named built-ins), while an absent allow-list becomes {@link SearchOperatorSet#standard()} with each
	 * custom appended via {@link SearchOperatorSet#with(SearchOperator)}.
	 *
	 * @param name The column name (row-data key).  Must not be <jk>null</jk> or blank.
	 * @param searchTypeWire The wire {@link SearchType} token, or <jk>null</jk>.
	 * @param operatorAllowList The explicit allowed-operator names (design §4.3), or <jk>null</jk> for the default
	 * 	(the full type-applicable universe).
	 * @param customOperators Per-column custom operators (design §4.4) as {@code {name,help}} maps; may be
	 * 	<jk>null</jk> or empty.
	 * @return The {@code {type, operators:[...]}} block, or <jk>null</jk> when {@code searchTypeWire} names no known
	 * 	type (a non-searchable column emits no search block).
	 */
	@SuppressWarnings({
		"java:S3776" // Allow-list vs. default branches with custom-operator merging read best inline.
	})
	public static JsonMap searchMeta(String name, String searchTypeWire, List<String> operatorAllowList, List<Map<String,String>> customOperators) {
		var type = SearchType.fromWire(searchTypeWire);
		if (type == null)
			return null;
		var customs = new LinkedHashMap<String,SearchOperator>();
		if (customOperators != null)
			for (var cm : customOperators)
				customs.put(cm.get("name"), SearchOperator.create(cm.get("name"), cm.get("help")).types(type).build());
		SearchOperatorSet set;
		if (operatorAllowList != null) {
			var ops = new ArrayList<SearchOperator>();
			for (var n : operatorAllowList) {
				var op = customs.get(n);
				if (op == null)
					op = SearchOperatorSet.standard().get(n);
				if (op != null)
					ops.add(op);
			}
			set = SearchOperatorSet.of(ops.toArray(new SearchOperator[0]));
		} else {
			set = SearchOperatorSet.standard();
			for (var op : customs.values())
				set = set.with(op);
		}
		return create(name).searchType(type).operators(set).searchMeta();
	}

	/**
	 * Serializes this column's effective search configuration to the {@code VIEW_META} {@code search} block
	 * (design §4.3&ndash;§4.5): the wire {@link SearchType} token plus one entry per
	 * {@link #effectiveOperators() effective operator}, each carrying the metadata the header popup renders.
	 *
	 * <p>
	 * <c>help</c> is present only for custom operators; the popup supplies built-in help text from <c>juneau-search.js</c>.
	 *
	 * @return The {@code {type, operators:[{name,help,minArgs,maxArgs,combinator,custom}]}} block, or <jk>null</jk>
	 * 	if this column is not searchable.
	 */
	public JsonMap searchMeta() {
		return searchMeta(null);
	}

	/**
	 * Serializes this column's effective search configuration to the {@code VIEW_META} {@code search} block
	 * (design §4.3&ndash;§4.5), inheriting the specified table-level operator set when this column has none of its
	 * own.
	 *
	 * @param tableDefault The table-level operator set to inherit when this column has none of its own.  Can be
	 * 	<jk>null</jk>, in which case {@link SearchOperatorSet#standard()} is used.
	 * @return The {@code {type, operators:[{name,help,minArgs,maxArgs,combinator,custom}]}} block, or <jk>null</jk>
	 * 	if this column is not searchable.
	 */
	public JsonMap searchMeta(SearchOperatorSet tableDefault) {
		if (searchType == null)
			return null;
		var operatorList = new JsonList();
		for (var op : effectiveOperators(tableDefault)) {
			var o = new JsonMap();
			o.put("name", op.name());
			if (op.help() != null)
				o.put("help", op.help());
			o.put("minArgs", op.minArgs());
			o.put("maxArgs", op.maxArgs());
			o.put("combinator", op.isCombinator());
			o.put("custom", op.isCustom());
			operatorList.add(o);
		}
		var search = new JsonMap();
		search.put("type", searchType.wire());
		search.put("operators", operatorList);
		return search;
	}
}
