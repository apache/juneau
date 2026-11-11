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

import org.apache.juneau.console.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.Json;

/**
 * Row-detail panel for a table: the per-row GET endpoint, title template, icon and the row-detail region.
 *
 * <p>
 * {@link #CONTRACT_VERSION} and {@link #response(Object)} describe the JSON that endpoint returns.
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bjava'>
 * 	<jv>table</jv>.detail(RowDetail.<jsm>create</jsm>(<js>"/rest/work/data/{id}"</js>)
 * 		.title(<js>"{workId}"</js>).icon(<js>"search"</js>)
 * 		.region(RegionDef.<jsm>create</jsm>(<js>"work-detail"</js>).populate(<js>"work-detail"</js>)
 * 			.allowPopulators(<js>"work-detail"</js>)));
 *
 * 	<jc>// The matching endpoint:</jc>
 * 	<ja>@RestGet</ja>(path=<js>"/data/{id}"</js>)
 * 	<jk>public</jk> JsonMap detail(<ja>@Path</ja> String <jv>id</jv>) {
 * 		<jk>return</jk> RowDetail.<jsm>response</jsm>(<jv>store</jv>.fields(<jv>id</jv>));
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192", // Map keys read more clearly inline than as constants.
	"java:S3776" // Branching is inherent to toMap() and tabModelMap(), which serialize many optional settings.
})
public final class RowDetail implements Handled<RowDetail> {

	/** The contract version for the detail GET response (the card fragment itself carries no version). */
	public static final String CONTRACT_VERSION = "1";

	private final String endpoint;
	private final String mode;
	private final Class<?> beanType;
	private final Function<String,?> lookup;
	private String key;
	private String basePath = "";
	private String title;
	private String icon;
	private RegionDef region;
	private boolean tabModel;
	private int columns;
	private DetailTab.Cache cache = DetailTab.Cache.PER_ROW;
	private String countsEndpoint;
	private final List<RowAction> actions = new ArrayList<>();
	private final List<DetailTab> tabs = new ArrayList<>();
	private final List<String> narrowedFields = new ArrayList<>();
	private final Map<String,String> labels = new LinkedHashMap<>();
	private final ModelBlock model;
	/** Default field sets and labels resolved against each owning table, keyed by table id; shared by every copy. */
	private final Map<String,Resolved> resolved;

	private static final class Resolved {
		final List<String> fields = new ArrayList<>();
		final Map<String,String> labels = new LinkedHashMap<>();
	}

	private RowDetail(String endpoint, String mode, Class<?> beanType, Function<String,?> lookup) {
		this(endpoint, mode, beanType, lookup, new java.util.concurrent.ConcurrentHashMap<>());
	}

	private RowDetail(String endpoint, String mode, Class<?> beanType, Function<String,?> lookup, Map<String,Resolved> resolved) {
		this.endpoint = endpoint;
		this.mode = mode;
		this.beanType = beanType;
		this.lookup = lookup;
		this.tabModel = mode != null;
		this.resolved = resolved;
		this.model = new ModelBlock("rowdetail", modelId(endpoint, beanType));
	}

	private static String modelId(String endpoint, Class<?> beanType) {
		if (endpoint != null)
			return endpoint;
		return beanType != null ? beanType.getSimpleName() : "inline";
	}

	@Override /* Handled */
	public ModelBlock modelBlock() { return model; }

	@Override /* Handled */
	public RowDetail self() { return this; }

	/**
	 * Starts a row-detail definition for the given per-row GET endpoint.
	 *
	 * @param endpoint A same-origin path template carrying the literal {@code {id}} row-id placeholder (the client substitutes only that), e.g. {@code "/rest/work/data/{id}"}.
	 * 	Must not be <jk>null</jk> or blank.
	 * @return A new {@link RowDetail}.
	 * @throws IllegalArgumentException If {@code endpoint} is not a same-origin path with the literal {@code {id}} placeholder.
	 */
	public static RowDetail create(String endpoint) {
		if (! RegionDef.isSafeDetailEndpoint(endpoint) || ! endpoint.contains("{id}"))
			throw iaex("RowDetail endpoint '%s' is not a safe detail endpoint (same-origin path with an {id} placeholder).", endpoint);
		return new RowDetail(endpoint, null, null, null);
	}

	/**
	 * Starts a tabbed detail fed from the clicked row's own data, with no server round trip.
	 *
	 * <p>
	 * The row data includes columns hidden by default.  A column that must not reach the browser must not be in the
	 * rows endpoint.
	 *
	 * @return A new {@link RowDetail}.
	 */
	public static RowDetail inline() {
		return new RowDetail(null, "inline", null, null);
	}

	/**
	 * Starts a tabbed detail served by {@link RowDetailMixin} from a bean.
	 *
	 * @param <T> The bean type.
	 * @param type The bean class; only its readable, non-ignored properties can be served.
	 * @param lookup Finds the bean for a row id, or returns <jk>null</jk> when there is none.
	 * @return A new {@link RowDetail}.
	 */
	public static <T> RowDetail fromBean(Class<T> type, Function<String,T> lookup) {
		if (type == null || lookup == null)
			throw iaex("RowDetail.fromBean needs a type and a lookup.");
		var d = new RowDetail(null, "bean", type, lookup);
		d.key = decapitalise(type.getSimpleName());
		return d;
	}

	private static String decapitalise(String s) {
		return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}

	/**
	 * Returns an independent copy, so a shared instance can be attached to several tables.
	 *
	 * @return The copy.
	 */
	public RowDetail copy() {
		var c = new RowDetail(endpoint, mode, beanType, lookup, resolved);
		c.key = key;
		c.basePath = basePath;
		c.title = title;
		c.icon = icon;
		c.region = region;
		c.tabModel = tabModel;
		c.columns = columns;
		c.cache = cache;
		c.countsEndpoint = countsEndpoint;
		c.actions.addAll(actions);
		c.tabs.addAll(tabs);
		c.narrowedFields.addAll(narrowedFields);
		c.labels.putAll(labels);
		c.model.addAll(model);
		return c;
	}

	/**
	 * Adds a header action, shown right-aligned in the detail header.
	 *
	 * @param a The action.
	 * @return This object.
	 */
	public RowDetail action(RowAction a) {
		useTabModel();
		actions.add(a);
		return this;
	}

	/**
	 * Adds a tab.
	 *
	 * @param t The tab.  Its id must be unique in this detail.
	 * @return This object.
	 */
	public RowDetail tab(DetailTab t) {
		useTabModel();
		tabs.add(t);
		return this;
	}

	/**
	 * Adds a fields tab.
	 *
	 * @param tabTitle The tab title.
	 * @param fields The row fields shown.
	 * @return This object.
	 */
	public RowDetail section(String tabTitle, String...fields) {
		return tab(DetailTab.fields(tabTitle, fields));
	}

	/**
	 * Narrows the fields the generated Details tab shows.
	 *
	 * @param names The field names.
	 * @return This object.
	 */
	public RowDetail fields(String...names) {
		useTabModel();
		narrowedFields.addAll(Arrays.asList(names));
		return this;
	}

	/**
	 * Sets the default grid column count of the fields tabs.
	 *
	 * @param n The count, 1 to 4 (default 2).
	 * @return This object.
	 */
	public RowDetail columns(int n) {
		if (n < 1 || n > 4)
			throw iaex("RowDetail columns must be 1 to 4, was %s.", n);
		useTabModel();
		columns = n;
		return this;
	}

	/**
	 * Sets an explicit label for a field.  It wins over the column label and the bean metadata.
	 *
	 * @param field The field name.
	 * @param text The label.
	 * @return This object.
	 */
	public RowDetail label(String field, String text) {
		useTabModel();
		labels.put(field, text);
		return this;
	}

	/**
	 * Sets a dedicated endpoint for the tab counts.  Without it the counts ride the detail response.
	 *
	 * @param value A same-origin path template carrying {@code {id}}.
	 * @return This object.
	 */
	public RowDetail counts(String value) {
		if (! RegionDef.isSafeDetailEndpoint(value) || ! value.contains("{id}"))
			throw iaex("RowDetail counts endpoint '%s' is not a safe detail endpoint (same-origin path with an {id} placeholder).", value);
		useTabModel();
		countsEndpoint = value;
		return this;
	}

	/**
	 * Sets how tab content is cached.
	 *
	 * @param value The policy (default {@link DetailTab.Cache#PER_ROW}).
	 * @return This object.
	 */
	public RowDetail cache(DetailTab.Cache value) {
		useTabModel();
		cache = value;
		return this;
	}

	/**
	 * Sets the URL key of a bean-backed detail (default: the decapitalised simple class name).
	 *
	 * @param value The key.
	 * @return This object.
	 */
	public RowDetail key(String value) {
		key = value;
		return this;
	}

	/**
	 * Sets the path of the resource that serves a bean-backed detail through {@link RowDetailMixin}.
	 *
	 * <p>
	 * The endpoint becomes <c>{basePath}/row-detail/{key}/{id}</c>.  Without it the endpoint is relative to the site root.
	 *
	 * @param value A same-origin path with no placeholders, e.g. {@code "/console/pets"}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is not a safe same-origin path.
	 */
	public RowDetail basePath(String value) {
		if (value == null || value.contains("{") || ! RegionDef.isSafeDetailEndpoint(value))
			throw iaex("RowDetail basePath '%s' is not a safe same-origin path.", value);
		basePath = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
		return this;
	}

	/** @return The bean class of a bean-backed detail, or <jk>null</jk>. */
	Class<?> beanType() { return beanType; }

	/** @return The bean lookup of a bean-backed detail, or <jk>null</jk>. */
	@SuppressWarnings({
		"java:S1452" // The lookup result type is the caller's bean type, unknown to this class.
	})
	Function<String,?> lookup() { return lookup; }

	/** @return The tabs declared on this detail. */
	List<DetailTab> tabList() { return List.copyOf(tabs); }

	/** @return The title template, or <jk>null</jk>. */
	String titleTemplate() { return title; }

	/** @return The field set resolved against the owning tables, in order and without repeats (empty until a table has been built). */
	List<String> resolvedFields() {
		var out = new LinkedHashSet<String>();
		for (var r : resolved.values())
			out.addAll(r.fields);
		return List.copyOf(out);
	}

	/** @return The labels resolved against the owning table. */
	Map<String,String> resolvedLabels() {
		var m = new LinkedHashMap<String,String>();
		for (var r : resolved.values())
			r.labels.forEach(m::putIfAbsent);
		labels.forEach(m::put);
		return m;
	}

	/** @return The bean property names that may be served: readable and not ignored. */
	Set<String> servableProperties() {
		return eligibleBeanProperties();
	}

	/** @return The URL key of a bean-backed detail, or <jk>null</jk>. */
	String key() {
		return key;
	}

	private void useTabModel() {
		if (region != null)
			throw isex("RowDetail with a region cannot use tabs or header actions.");
		tabModel = true;
	}

	/**
	 * Resolves a field label: explicit, then column, then bean metadata, then the humanised name.
	 *
	 * @param field The field name.
	 * @param explicit The label set with {@link #label(String,String)}, or <jk>null</jk>.
	 * @param columnLabel The table column's label, or <jk>null</jk>.
	 * @param beanLabel The bean metadata label, or <jk>null</jk>.
	 * @return The label.
	 */
	static String resolveLabel(String field, String explicit, String columnLabel, String beanLabel) {
		if (explicit != null)
			return explicit;
		if (columnLabel != null)
			return columnLabel;
		if (beanLabel != null)
			return beanLabel;
		return humanise(field);
	}

	/** Turns {@code birthDate} or {@code birth_date} into {@code Birth date}. */
	static String humanise(String field) {
		var s = field.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replaceAll("[_-]+", " ").trim().toLowerCase(Locale.ROOT);
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/**
	 * Sets the detail panel's title template.
	 *
	 * @param template A row-data interpolation template, e.g. {@code "{workId}"}.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RowDetail title(String template) {
		title = template;
		return this;
	}

	/**
	 * Sets the detail panel's icon.
	 *
	 * @param name A glyph name resolved by the shared icon registry.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public RowDetail icon(String name) {
		icon = name;
		return this;
	}

	/**
	 * Sets the detail panel's region.
	 *
	 * @param value The region.  Can be <jk>null</jk> to clear it (a header-only panel).
	 * 	Its {@link RegionDef#type} defaults to {@link RegionDef#TYPE_ROW_DETAIL} and its
	 * 	{@link RegionDef#dataUrl} defaults to this detail's own endpoint, applied at {@link #toMap()} time onto a
	 * 	copy; this method never mutates {@code value} itself.
	 * @return This object.
	 */
	public RowDetail region(RegionDef value) {
		if (tabModel)
			throw isex("RowDetail with tabs or header actions cannot have a region.");
		region = value;
		return this;
	}

	/**
	 * Builds the detail GET endpoint's own JSON response (not the card fragment).
	 *
	 * @param fields The row's detail fields, a map (or bean) in whatever shape the detail region's populator/renderer expects.
	 * @return {@code {contractVersion, fields}}.
	 */
	public static JsonMap response(Object fields) {
		var m = new JsonMap();
		m.put("contractVersion", CONTRACT_VERSION);
		m.put("fields", fields);
		return m;
	}

	/**
	 * Builds this detail's card-body entry: {@code {endpoint, title?, icon?, region?}}.
	 *
	 * <p>
	 * The region is optional: a detail with none is a header-only panel (the client fills the {@code title}/{@code icon}
	 * header from the endpoint's {@code fields}) and the entry carries no {@code region} key.
	 *
	 * <p>
	 * When a region is set, builds from a copy: reads {@link RegionDef#toContractMap()} off the caller's {@link #region(RegionDef) region}
	 * into a fresh map, applies the {@code type}/{@code dataUrl} defaults onto that copy, and strips the copy's
	 * {@code contractVersion} key (a card fragment never nests a {@code contractVersion}, since the client stamps it
	 * on mount).  The caller's {@link RegionDef} instance is never written to.
	 *
	 * @return The detail entry.
	 */
	public JsonMap toMap() {
		return tabModel ? tabModelMap() : plainMap();
	}

	/**
	 * Builds the detail entry resolved against the owning table: the default field set, labels and row id.
	 *
	 * @param tableId The table id, for error messages.
	 * @param cols The table's columns.
	 * @param rowIdField The table's row-id field (explicit, else the selection's), or <jk>null</jk>.
	 * @return The detail entry.
	 * @throws IllegalStateException If a tabbed detail has no resolvable row id field.
	 */
	JsonMap toMap(String tableId, List<Column> cols, String rowIdField) {
		if (! tabModel)
			return plainMap();
		var names = cols.stream().map(Column::name).toList();
		var idField = rowIdField;
		if (idField == null && names.contains("id"))
			idField = "id";
		if (idField == null && names.contains("name"))
			idField = "name";
		if (idField == null)
			throw isex("table '%s' has a detail but no resolvable row id field.", tableId);
		var c = copy();
		if (c.narrowedFields.isEmpty()) {
			var eligible = eligibleBeanProperties();
			for (var col : cols)
				if (beanType == null || eligible.contains(col.name()))
					c.narrowedFields.add(col.name());
		}
		for (var col : cols)
			if (col.label() != null && ! c.labels.containsKey(col.name()))
				c.labels.put(col.name(), col.label());
		var r = new Resolved();
		r.fields.addAll(c.narrowedFields);
		r.labels.putAll(c.labels);
		resolved.put(tableId, r);
		return c.tabModelMap();
	}

	private Set<String> eligibleBeanProperties() {
		if (beanType == null)
			return Set.of();
		var bm = org.apache.juneau.marshall.MarshallingContext.DEFAULT.getBeanMeta(beanType);
		var out = new LinkedHashSet<String>();
		if (bm != null)
			for (var pm : bm.getProperties().values())
				if (pm.canRead())
					out.add(pm.getName());
		return out;
	}

	private JsonMap tabModelMap() {
		var m = new JsonMap();
		if (mode != null)
			m.put("mode", mode);
		var ep = endpoint;
		if (ep == null && "bean".equals(mode))
			ep = basePath + "/row-detail/" + key + "/{id}";
		if (ep != null)
			m.put("endpoint", ep);
		if (title != null)
			m.put("title", title);
		if (icon != null)
			m.put("icon", icon);
		if (columns != 0)
			m.put("columns", columns);
		if (cache != DetailTab.Cache.PER_ROW)
			m.put("cache", cache.wire());
		if (countsEndpoint != null)
			m.put("countsEndpoint", countsEndpoint);
		if (! labels.isEmpty())
			m.put("labels", new LinkedHashMap<>(labels));
		var all = new ArrayList<Map<String,Object>>();
		for (var a : actions) {
			a.validateLink();
			if (a.href == null && ib(a.label) || ib(a.label) && ! ib(a.icon))
				throw iaex("RowDetail action '%s' needs a label (an icon-only action has no accessible name).", a.id);
			var am = new LinkedHashMap<String,Object>(Json.to(Json.of(a), Map.class));
			am.put("type", a.href != null ? "link" : "row-action");
			all.add(am);
		}
		var mm = model.toMap();
		if (mm != null && mm.get("actions") instanceof List<?> hl)
			for (var h : hl)
				all.add(castMap(h));
		if (! all.isEmpty())
			m.put("actions", all);
		var tl = new ArrayList<DetailTab>(tabs);
		if (tl.isEmpty())
			tl.add(DetailTab.autoFields("Details", narrowedFields).id("details"));
		var ids = new HashSet<String>();
		var tms = new ArrayList<Map<String,Object>>();
		var viewIds = new HashSet<String>();
		for (var t : tl) {
			var tm = t.toMap();
			if (tm.get("nested") instanceof Map<?,?> nm && ! viewIds.add((String)((Map<?,?>)nm.get("view")).get("id")))
				throw iaex("RowDetail has two nested tables with view id '%s'.", ((Map<?,?>)nm.get("view")).get("id"));
			if (! ids.add((String)tm.get("id")))
				throw iaex("RowDetail has two tabs with id '%s'.", tm.get("id"));
			tms.add(tm);
		}
		m.put("tabs", tms);
		if (mm != null) {
			var rest = new LinkedHashMap<>(mm);
			rest.remove("actions");
			if (! rest.isEmpty())
				m.put("model", rest);
		}
		return m;
	}

	@SuppressWarnings("unchecked") // Handler action maps are string-keyed by construction.
	private static Map<String,Object> castMap(Object o) {
		return (Map<String,Object>)o;
	}

	private JsonMap plainMap() {
		var m = new JsonMap();
		m.put("endpoint", endpoint);
		if (title != null)
			m.put("title", title);
		if (icon != null)
			m.put("icon", icon);
		if (region != null) {
			var regionMap = new LinkedHashMap<>(region.toContractMap());
			regionMap.remove("contractVersion");
			regionMap.putIfAbsent("type", RegionDef.TYPE_ROW_DETAIL);
			regionMap.putIfAbsent("dataUrl", endpoint);
			m.put("region", regionMap);
		}
		var mm = model.toMap();
		if (mm != null) {
			mm = new LinkedHashMap<>(mm);
			var modelActions = mm.remove("actions");
			if (modelActions != null)
				m.put("actions", modelActions);
			if (! mm.isEmpty())
				m.put("model", mm);
		}
		return m;
	}
}
