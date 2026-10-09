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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.logging.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.views.*;

/**
 * The built-in {@code datatables} card type, registered through {@code META-INF/services}: validates the author
 * catalog and resolves per-column search metadata, with no shape lift — the catalog stays in author shape
 * ({@code key}/{@code label}, not VIEW_META's {@code data}/{@code title}); {@code NS.card.liftCatalog}
 * (juneau-views.js) does the lift at mount time.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   CardSource <jv>s</jv> = CardSource.<jsm>create</jsm>(<js>"datatables"</js>, <js>"releases"</js>)
 *     .body(<js>"{dataUrl:'/rest/releases/data', columns:[{key:'version', label:'Version', searchType:'text'}]}"</js>)
 *     .build();
 *   JsonMap <jv>frag</jv> = <jk>new</jk> DatatablesCardType().toFragment(<jv>s</jv>);  <jc>// {"table": {...}}</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class DatatablesCardType implements CardTypeHandler {

	private static final Logger LOG = Logger.getLogger(DatatablesCardType.class.getName());

	/** Inline {@code rows} count above which a {@code WARNING} steers the author to {@code dataUrl}. */
	private static final int ROWS_WARN_THRESHOLD = 1_000;

	private static final String E27 = "type='datatables' requires src= or a body with 'columns' and exactly one of 'dataUrl' or 'rows'.";

	private static final String E28 = "type='datatables' body is a pre-built SLOT_META envelope; remove 'contractVersion', 'layout' and 'view' and author the catalog form.";

	private static final Set<String> KNOWN_COLUMN_KEYS = Set.of(
		"key", "data", "label", "title", "search", "render", "href", "className",
		"orderable", "searchable", "defaultVisible", "pinned", "formats",
		"searchType", "searchOperators", "customOperators");

	/** Public no-arg constructor, required by {@link java.util.ServiceLoader}. */
	public DatatablesCardType() {}

	/** The {@code cmd:<id>} ops the datatables JS handles (spec §4.5), beyond the shell's {@code refresh}. */
	static final Set<String> TABLE_OPS = Set.of(
		"reload", "clear-selection", "select", "set-filter", "pause-polling", "resume-polling", "collapse-all");

	@Override
	public String type() {
		return "datatables";
	}

	/**
	 * Spec §5.4: {@code filter:} and {@code redraw:} always; {@code selection:}, {@code detail:} and {@code bulk:}
	 * only when the catalog sets that key.  A {@code src}-only card's catalog arrives at runtime, so it is credited
	 * with all five.  The JS side applies this same rule and does not re-check once the catalog is known: a topic the
	 * catalog turns out not to need stays claimed and is simply never published.
	 */
	@Override
	public List<String> implicitTopics(JsonMap card) {
		var id = card.getString("id");
		var view = card.get("table") instanceof Map<?,?> t ? t : card;
		var all = view.get("dataUrl") == null;
		var out = new ArrayList<String>();
		if (all || view.get("selection") != null)
			out.add("selection:" + id);
		out.add("filter:" + id);
		out.add("redraw:" + id);
		if (all || view.get("detail") != null)
			out.add("detail:" + id);
		if (all || view.get("bulk") != null)
			out.add("bulk:" + id);
		return out;
	}

	/** Spec §5.3: a table applies {@code filter:}-shaped (or mapped) payloads as a {@code set-filter} patch. */
	@Override
	public Set<String> acceptedRoles() {
		return Set.of("filter");
	}

	@Override
	public Set<String> acceptedOps() {
		return TABLE_OPS;
	}

	@Override
	@SuppressWarnings("unchecked")
	public JsonMap toFragment(CardSource source) {
		var hasSrc = nn(source.src()) && ! source.src().isEmpty();
		var hasBody = source.hasJsonBody() || (nn(source.body()) && ! source.body().isEmpty());
		if (hasSrc && hasBody)
			throw source.error("type='datatables' takes src= or a body, not both.");
		if (hasSrc)
			return new JsonMap();
		if (! hasBody)
			throw source.error(E27);

		var catalog = source.json(); // E-25 for a non-JSON5 body (the dropped bare-URL form), E-24 for bad JSON5.

		if (catalog.get("view") instanceof Map)
			throw source.error(E28);

		var columns = catalog.getList("columns");
		var hasDataUrl = nn(catalog.get("dataUrl"));
		var hasRows = catalog.containsKey("rows");
		if (n(columns) || hasDataUrl == hasRows)
			throw source.error(E27);
		if (hasRows)
			validateRows(source, catalog);

		checkNoContractVersion(source, catalog, "table");

		// The authored body (and its lists) may be immutable, so the normalized columns go into a fresh list of copies.
		var normalized = new ArrayList<Object>(columns.size());
		var seen = new HashSet<String>();
		for (var i = 0; i < columns.size(); i++) {
			if (! (columns.get(i) instanceof Map<?,?> raw))
				throw source.error("columns['%s'] requires 'key'.", i);
			var col = new LinkedHashMap<>((Map<String,Object>) raw);
			normalized.add(col);
			var ident = columnIdent(col);
			if (n(ident))
				throw source.error("columns['%s'] requires 'key'.", i);
			for (var key : col.keySet())
				if (! KNOWN_COLUMN_KEYS.contains(key))
					throw source.error("column '%s' has unknown key '%s'; known: '%s'.",
						ident, key, String.join(", ", new TreeSet<>(KNOWN_COLUMN_KEYS)));
			if (! seen.add(ident))
				throw source.error("declares column '%s' twice.", ident);
			if (col.get("render") instanceof String r) {
				Render parsed;
				try {
					parsed = Render.parse(r);
				} catch (IllegalArgumentException e) {
					throw source.error("column '%s' render: %s", ident, e.getMessage());
				}
				var rm = new LinkedHashMap<String,Object>();
				rm.put("id", parsed.id);
				if (parsed.meta != null)
					rm.put("meta", parsed.meta);
				col.put("render", rm);
			}
			resolveSearch(source, col, ident);
		}
		catalog.put("columns", normalized);

		if (catalog.get("ribbon") instanceof List<?> ribbon)
			for (var item : ribbon) {
				if (! (item instanceof Map<?,?> im))
					throw source.error("ribbon each item must be an object.");
				try {
					RibbonItem.validate(im);
				} catch (IllegalArgumentException e) {
					throw source.error("%s", e.getMessage());
				}
			}

		if (nn(catalog.get("bulk")) && n(catalog.get("selection")))
			throw source.error("sets bulk without selection.");
		checkAggregateEndpoints(catalog);

		var frag = new JsonMap();
		var page = catalog.remove("page");
		if (nn(page))
			frag.put("page", page);
		// toCard lifts a body visibleWhen to the card top level; leaving it in the catalog would emit it twice.
		catalog.remove("visibleWhen");
		frag.put("table", catalog);
		return frag;
	}

	private static void validateRows(CardSource source, JsonMap catalog) {
		var rows = catalog.get("rows");
		if (! (rows instanceof List<?> list))
			throw source.error("type='datatables' 'rows' must be an array of objects.");
		for (var r : list)
			if (! (r instanceof Map<?, ?>))
				throw source.error("type='datatables' each 'rows' element must be an object.");
		if (eq("server", catalog.getString("dataMode")))
			throw source.error("type='datatables' inline 'rows' cannot be combined with dataMode:'server'; use 'dataUrl'.");
		if (nn(catalog.get("pollIntervalMs")))
			throw source.error("type='datatables' inline 'rows' cannot be combined with 'pollIntervalMs' (nothing to poll); use 'dataUrl'.");
		if (list.size() > ROWS_WARN_THRESHOLD)
			LOG.log(Level.WARNING, "<@card type=\"datatables\"> inline 'rows' has {0} entries; prefer 'dataUrl' for large data sets.", list.size());
	}

	private static String columnIdent(Map<String,Object> col) {
		var data = col.get("data");
		var key = col.get("key");
		return nn(data) ? data.toString() : (nn(key) ? key.toString() : null);
	}

	/** E-31: a {@code contractVersion} key anywhere in the catalog (table, detail, quickStats, bulk) — JS stamps versions. */
	private static void checkNoContractVersion(CardSource source, Object node, String where) {
		if (node instanceof Map<?,?> m) {
			if (m.containsKey("contractVersion"))
				throw source.error("'%s' sets contractVersion; remove it, the console stamps contract versions.", where);
			for (var e : m.entrySet())
				checkNoContractVersion(source, e.getValue(), String.valueOf(e.getKey()));
		} else if (node instanceof List<?> l) {
			for (var v : l)
				checkNoContractVersion(source, v, where);
		}
	}

	@SuppressWarnings("unchecked")
	private static void resolveSearch(CardSource source, Map<String,Object> col, String ident) {
		var searchType = str(col.get("searchType"));
		if (ib(searchType))
			return; // Non-searchable column: no search block.
		if (n(SearchType.fromWire(searchType)))
			throw source.error("column '%s' searchType '%s' is unknown; known: '%s'.", ident, searchType, knownSearchTypes());

		var customs = new LinkedHashMap<String,String>(); // name -> help
		if (col.get("customOperators") instanceof List<?> cs)
			for (var raw : cs) {
				if (! (raw instanceof Map<?,?>))
					throw source.error("column '%s' each customOperator must be an object.", ident);
				var cm = (Map<String,Object>) raw;
				customs.put(str(cm.get("name")), str(cm.get("help")));
			}

		List<String> allow = null;
		if (col.get("searchOperators") instanceof List<?> a) {
			allow = new ArrayList<>();
			for (var n : a) {
				var name = str(n);
				if (! customs.containsKey(name) && n(SearchOperatorSet.standard().get(name)))
					throw source.error("column '%s' searchOperators names unknown operator '%s'; known: '%s'.",
						ident, name, knownOperators(customs.keySet()));
				allow.add(name);
			}
		}

		var customOps = new ArrayList<Map<String,String>>();
		for (var e : customs.entrySet()) {
			var m = new LinkedHashMap<String,String>();
			m.put("name", e.getKey());
			m.put("help", e.getValue());
			customOps.add(m);
		}
		var search = Column.searchMeta(ident, searchType, allow, customOps);
		col.put("search", search);
		col.remove("searchType");
		col.remove("searchOperators");
		col.remove("customOperators");
	}

	private static String knownSearchTypes() {
		var names = new ArrayList<String>();
		for (var t : SearchType.values())
			names.add(t.wire());
		return String.join(", ", names);
	}

	private static String knownOperators(Set<String> customNames) {
		var names = new TreeSet<String>(customNames);
		for (var op : SearchOperatorSet.standard().operators())
			names.add(op.name());
		return String.join(", ", names);
	}

	/**
	 * E-35: an {@code aggregate} bulk action's endpoint must not contain a {@code {field}} token (aggregate
	 * endpoints receive {@code {ids}} in the body). No {@code <@card id='%s'>} prefix — {@code BulkMutateDef}
	 * (Task 7) raises the identical message with no card in scope.
	 */
	@SuppressWarnings("unchecked")
	private static void checkAggregateEndpoints(JsonMap catalog) {
		if (! (catalog.get("bulk") instanceof Map<?,?> bulk))
			return;
		if (! (bulk.get("actions") instanceof List<?> actions))
			return;
		for (var raw : actions) {
			var action = (Map<String,Object>) raw;
			var mode = nn(action.get("mode")) ? action.get("mode") : action.get("bulkMode");
			if (! eq("aggregate", mode))
				continue;
			var endpoint = str(action.get("endpoint"));
			if (nn(endpoint) && endpoint.matches(".*\\{[^}]+\\}.*"))
				throw iaex(
					"Bulk action '%s' has mode 'aggregate' but its endpoint '%s' contains a {field} token; "
					+ "aggregate endpoints receive {ids} in the body.", action.get("id"), endpoint);
		}
	}

	private static String str(Object o) {
		return n(o) ? null : o.toString();
	}
}
