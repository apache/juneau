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
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;

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
@SuppressWarnings({
	"java:S1192", // Repeated literals are contract wire keys; named constants would obscure the contract shape.
	"java:S3776" // Validation and rendering chains many independent guards; splitting them adds no clarity.
})
public final class DatatablesCardType implements CardTypeHandler {

	private static final Logger LOG = Logger.getLogger(DatatablesCardType.class.getName());

	/** A <c>{token}</c> placeholder in an endpoint template. */
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+\\}");

	/** Inline {@code rows} count above which a {@code WARNING} steers the author to {@code dataUrl}. */
	private static final int ROWS_WARN_THRESHOLD = 1_000;

	private static final String E27 = "type='datatables' requires src= or a body with 'columns' and exactly one of 'dataUrl' or 'rows'.";

	private static final String E28 = "type='datatables' body is a pre-built SLOT_META envelope; remove 'contractVersion', 'layout' and 'view' and author the catalog form.";

	private static final Set<String> KNOWN_COLUMN_KEYS = Set.of(
		"key", "data", "label", "title", "search", "render", "href", "className",
		"orderable", "searchable", "defaultVisible", "pinned", "formats",
		"searchType", "searchOperators", "customOperators");

	/** Public no-arg constructor, required by {@link java.util.ServiceLoader}. */
	public DatatablesCardType() {
		// No state; instantiated by ServiceLoader.
	}

	/** The {@code cmd:<id>} ops the datatables JS handles, beyond the shell's {@code refresh}. */
	static final Set<String> TABLE_OPS = Set.of(
		"reload", "clear-selection", "select", "set-filter", "pause-polling", "resume-polling", "collapse-all");

	@Override
	public String type() {
		return "datatables";
	}

	/**
	 * {@code filter:} and {@code redraw:} always; {@code selection:}, {@code detail:} and {@code bulk:}
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

	/** A table applies {@code filter:}-shaped (or mapped) payloads as a {@code set-filter} patch. */
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
		checkQuickStats(source, catalog);

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
			} else if (n(col.get("render")) && nn(col.get("href"))) {
				// An href with no render implies the 'linked' renderer, as Column.toCatalogMap does.
				col.put("render", new LinkedHashMap<>(Map.of("id", "linked")));
			}
			resolveSearch(source, col, ident);
		}
		catalog.put("columns", normalized);
		projectDetailRegion(source, catalog);

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

	/**
	 * Projects the detail endpoint onto the detail region, as the builder path's {@code RowDetail.toMap()} does:
	 * {@code type} defaults to {@code row-detail} and {@code dataUrl} to the detail endpoint, so a region that
	 * declares neither joins the panel's own expand GET instead of fetching again, and {@code lazy} to its effective
	 * per-type value ({@link RegionDef#effectiveLazy()}), which the builder always emits.  Explicit values are kept,
	 * and the authored (possibly immutable) maps are copied rather than written to.
	 */
	@SuppressWarnings("unchecked")
	private static void projectDetailRegion(CardSource source, JsonMap catalog) {
		if (! (catalog.get("detail") instanceof Map<?,?> detail) || ! (detail.get("region") instanceof Map<?,?> region)
				|| ! (detail.get("endpoint") instanceof String endpoint))
			return;
		var regionCopy = new LinkedHashMap<>((Map<String,Object>)region);
		regionCopy.putIfAbsent("type", RegionDef.TYPE_ROW_DETAIL);
		regionCopy.putIfAbsent("dataUrl", endpoint);
		regionCopy.putIfAbsent("lazy", RegionDef.TYPE_TAB_BODY.equals(regionCopy.get("type")));
		if (isJobLogRegion(region))
			regionCopy.put("config", jobLogConfig(source, regionCopy));
		var detailCopy = new LinkedHashMap<>((Map<String,Object>)detail);
		detailCopy.put("region", regionCopy);
		catalog.put("detail", detailCopy);
	}

	/**
	 * Validates a job-log region's {@code config} through {@link JobLogRegion}, the same rules as the builder path,
	 * and returns it in the builder's canonical key order so the two paths emit the same sidecar.
	 */
	@SuppressWarnings("unchecked")
	private static Map<String,Object> jobLogConfig(CardSource source, Map<String,Object> region) {
		var id = region.get("id") instanceof String s ? s : "";
		var config = region.get("config");
		if (! (config instanceof Map<?,?>))
			throw source.error("type='datatables' detail.region '%s' with populate 'job-log' requires a 'config' object.", id);
		try {
			return JobLogRegion.fromMap(id, (Map<String,Object>)config).validate().toMap();
		} catch (IllegalArgumentException e) {
			throw source.error("type='datatables' detail.region: %s", e.getMessage());
		}
	}

	private static boolean isJobLogRegion(Object region) {
		return region instanceof Map<?,?> r && JobLogRegion.POPULATOR.equals(r.get("populate"));
	}

	/** A datatables card whose detail region is a job-log region needs the {@code joblog} pack's populator. */
	@Override
	public List<String> requiredPacks(JsonMap body) {
		if (body != null && body.get("table") instanceof Map<?,?> table && table.get("detail") instanceof Map<?,?> detail
				&& isJobLogRegion(detail.get("region")))
			return List.of(ToolkitPackRegistry.PACK_JOBLOG);
		return List.of();
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
		var fallback = nn(key) ? key.toString() : null;
		return nn(data) ? data.toString() : fallback;
	}

	private static final Set<String> QUICK_STAT_KINDS = Set.of("tile", "meter", "segmented");

	/** E-37: the quickStats strip is a non-empty list of uniquely-identified items, each well-formed for its kind. */
	private static void checkQuickStats(CardSource source, Map<String,Object> catalog) {
		if (! catalog.containsKey("quickStats"))
			return;
		if (! (catalog.get("quickStats") instanceof Map<?,?> qs) || ! (qs.get("items") instanceof List<?> items) || items.isEmpty())
			throw source.error("quickStats items must be a non-empty array.");
		var ids = new HashSet<String>();
		for (var o : items) {
			if (! (o instanceof Map<?,?> item))
				throw source.error("quickStats items must be objects.");
			if (! (item.get("id") instanceof String id) || id.isBlank())
				throw source.error("quickStats item ids must be non-blank strings.");
			if (! ids.add(id))
				throw source.error("quickStats item '%s': duplicate id.", id);
			checkQuickStatItem(source, id, item);
		}
	}

	private static void checkQuickStatItem(CardSource source, String id, Map<?,?> item) {
		var hasSegments = item.containsKey("segments");
		var hasMax = item.containsKey("max");
		var type = item.get("type");
		String kind;
		if (type == null) {
			if (hasSegments && hasMax)
				throw source.error("quickStats item '%s': has both segments and max; set type.", id);
			var unsegmented = hasMax ? "meter" : "tile";
			kind = hasSegments ? "segmented" : unsegmented;
		} else {
			if (! (type instanceof String t) || ! QUICK_STAT_KINDS.contains(t))
				throw source.error("quickStats item '%s': unknown type '%s'.", id, type);
			var fits = switch (t) {
				case "tile" -> ! hasSegments && ! hasMax;
				case "meter" -> hasMax && ! hasSegments;
				default -> hasSegments && ! hasMax;
			};
			if (! fits)
				throw source.error("quickStats item '%s': type '%s' does not match its shape.", id, t);
			kind = t;
		}
		if (! kind.equals("tile") && (! (item.get("label") instanceof String lbl) || lbl.isBlank()))
			throw source.error("quickStats item '%s': a %s item needs a non-blank label.", id, kind);
		switch (kind) {
			case "meter" -> {
				if (! (item.get("value") instanceof Number v) || ! Double.isFinite(v.doubleValue()) || v.doubleValue() < 0)
					throw source.error("quickStats item '%s': meter value must be a number >= 0.", id);
				if (! (item.get("max") instanceof Number m) || ! Double.isFinite(m.doubleValue()) || m.doubleValue() <= 0)
					throw source.error("quickStats item '%s': meter max must be a number > 0.", id);
				if (item.containsKey("text") && ! (item.get("text") instanceof String))
					throw source.error("quickStats item '%s': text must be a string.", id);
				checkQuickStatTone(source, id, item.get("tone"));
			}
			case "segmented" -> {
				if (item.containsKey("tone") || item.containsKey("text"))
					throw source.error("quickStats item '%s': a segmented item takes no tone or text; tone each segment.", id);
				if (! (item.get("segments") instanceof List<?> segs) || segs.isEmpty() || segs.size() > QuickStat.MAX_SEGMENTS)
					throw source.error("quickStats item '%s': segments must be an array of 1 to %s objects.", id, QuickStat.MAX_SEGMENTS);
				for (var i = 0; i < segs.size(); i++) {
					if (! (segs.get(i) instanceof Map<?,?> s) || ! (s.get("label") instanceof String l) || l.isBlank()
							|| ! (s.get("count") instanceof Number c) || ! isWholeNonNegative(c.doubleValue()))
						throw source.error("quickStats item '%s': segment %s must be an object with a non-blank label and a whole count >= 0.", id, i + 1);
					checkQuickStatTone(source, id, " segment " + (i + 1), s.get("tone"));
				}
			}
			default -> checkQuickStatTone(source, id, item.get("tone"));
		}
	}

	private static boolean isWholeNonNegative(double d) {
		return Double.isFinite(d) && d >= 0 && d == Math.rint(d);
	}

	private static void checkQuickStatTone(CardSource source, String id, Object tone) {
		checkQuickStatTone(source, id, "", tone);
	}

	private static void checkQuickStatTone(CardSource source, String id, String where, Object tone) {
		if (tone != null && ! (tone instanceof String t && StatusTone.WIRE_TOKENS.contains(t)))
			throw source.error("quickStats item '%s'%s: tone '%s' is not one of neutral, info, success, warning, error.", id, where, tone);
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
		return Arrays.stream(SearchType.values()).map(SearchType::wire).collect(Collectors.joining(", "));
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
	 * raises the identical message with no card in scope.
	 */
	@SuppressWarnings("unchecked")
	private static void checkAggregateEndpoints(JsonMap catalog) {
		if (! (catalog.get("bulk") instanceof Map<?,?> bulk))
			return;
		if (! (bulk.get("actions") instanceof List<?> actions))
			return;
		for (var raw : actions) {
			var action = (Map<String,Object>) raw;
			var mode = action.get(nn(action.get("mode")) ? "mode" : "bulkMode");
			if (! eq("aggregate", mode))
				continue;
			var endpoint = str(action.get("endpoint"));
			if (nn(endpoint) && PLACEHOLDER.matcher(endpoint).find())
				throw iaex(
					"Bulk action '%s' has mode 'aggregate' but its endpoint '%s' contains a {field} token; "
					+ "aggregate endpoints receive {ids} in the body.", action.get("id"), endpoint);
		}
	}

	private static String str(Object o) {
		return n(o) ? null : o.toString();
	}
}
