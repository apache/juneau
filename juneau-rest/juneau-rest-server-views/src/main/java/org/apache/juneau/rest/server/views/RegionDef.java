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

import java.net.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.commons.http.*;

/**
 * A page "region": a named container whose contents a client-side populator fills in, chosen by name via
 * {@link #populate} rather than declared inline.
 *
 * <p>
 * Beyond the identity/populator core, this bean also carries the <b>declarative descriptor</b> (design
 * &sect;8.2): a data source ({@link #dataUrl} / {@link #params}), an inline renderer ({@link #renderer}),
 * lazy activation ({@link #lazy}), a refresh interval ({@link #refreshMs}), and a field/title projection
 * ({@link #fields} / {@link #titleFields}). A descriptor that omits {@link #populate} resolves to the
 * Juneau-shipped declarative default ({@code JuneauViews.regions.defaultPopulate}), which reads exactly
 * this descriptor (via {@code ctx.declared}) and nothing else &mdash; see the design's &sect;8.4 (L12)
 * non-privilege proof.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class RegionDef {

	/**
	 * The frozen contract version for the region envelope.
	 *
	 * <p>
	 * Deliberately independent of {@link ViewDef#CONTRACT_VERSION} and {@link RowDetailDef#CONTRACT_VERSION}, for
	 * the same reason those two envelopes are independent of each other: a region-contract revision must never
	 * force a view- or detail-sidecar bump, or vice-versa.
	 */
	public static final String CONTRACT_VERSION = "1";

	/** Wire token for a row-detail region (design &sect;6.2's {@code ctx.type}). */
	public static final String TYPE_ROW_DETAIL = "row-detail";

	/** Wire token for a card-body region. */
	public static final String TYPE_CARD_BODY = "card-body";

	/** Wire token for a tab-body region. */
	public static final String TYPE_TAB_BODY = "tab-body";

	private static final Set<String> TYPES = st(TYPE_ROW_DETAIL, TYPE_CARD_BODY, TYPE_TAB_BODY);

	/** The reserved renderer name that requires a non-empty {@link #fields} catalog (design &sect;6.2.2 property 4). */
	private static final String RENDERER_FIELD_GRID = "field-grid";

	/**
	 * The minimum honored {@link #refreshMs}, in milliseconds.
	 *
	 * <p>
	 * A declared interval below this floor is clamped up to it rather than honored as configured &mdash; the same
	 * reasoning and the same shared floor as {@link ViewDef#MIN_POLL_INTERVAL_MS} and
	 * {@code CardFieldList.java:152-153}'s {@code pollIntervalMs} clamp.
	 */
	public static final long MIN_REFRESH_MS = SafePathTemplate.MIN_POLL_INTERVAL_MS;

	/** This region's own id, unique within its host. */
	public String id;

	/**
	 * The name of the client populator to resolve via {@code JuneauViews.regions.resolve(name)}, or <jk>null</jk>
	 * for the reserved declarative default (<js>"default"</js>, see {@link PopulatorAllowlist#BUILTIN_IDS}).
	 *
	 * <p>
	 * This is a name looked up in the client registry rather than an inline function for two reasons: an inline
	 * function cannot survive the JSON sidecar this envelope travels in, and a name &mdash; unlike an arbitrary
	 * function body &mdash; is the one shape a server-side allowlist ({@link PopulatorAllowlist}) can meaningfully
	 * constrain at all.
	 */
	public String populate;

	/**
	 * This region's type: {@link #TYPE_ROW_DETAIL}, {@link #TYPE_CARD_BODY}, or {@link #TYPE_TAB_BODY}.
	 *
	 * <p>
	 * Set by the enclosing host (a detail panel, a card, or a tab body) rather than by the region's own author in
	 * the common case; exposed here (rather than only in the sidecar) because {@link #validate()} needs it to
	 * enforce the type-scoped field-catalog rules in &sect;6.2.2 &mdash; a {@link #TYPE_ROW_DETAIL} region must
	 * carry no {@link #fields}, and a {@link #TYPE_CARD_BODY}/{@link #TYPE_TAB_BODY} region using the
	 * {@code "field-grid"} {@link #renderer} under the reserved default must carry at least one.
	 */
	public String type;

	/**
	 * A same-origin path from which the declarative default (and {@code ctx.fetchDeclared()}) fetches this
	 * region's payload. <jk>null</jk> means "no declared fetch" &mdash; legal, and the normal state for a custom
	 * populator that fetches its own way or paints with no fetch at all.
	 *
	 * <p>
	 * Validated with the same same-origin, non-cross-scheme, no-{@code ..} check as
	 * {@link RowDetailDef#isSafeDetailEndpoint(String)}, which this reuses rather than re-implements.
	 */
	public String dataUrl;

	/**
	 * Author-declared request parameters, opaque to the framework as a <b>payload</b> (nothing routes or branches
	 * on their contents) but not opaque to the query-string serializer that appends them to {@link #dataUrl}
	 * (design &sect;8.2.1). Use a {@link LinkedHashMap} (or an equivalent ordered map) when key order matters to
	 * the endpoint: the serialization walks {@link Map#entrySet()} in the map's own iteration order.
	 */
	public Map<String,Object> params;

	/**
	 * The name of the {@code JuneauViews.helpers} function the declarative default paints with (design fork F8),
	 * e.g. <js>"field-grid"</js>. <jk>null</jk> when a custom {@link #populate} does its own painting.
	 */
	public String renderer;

	/**
	 * Whether this region's first populate is deferred until it becomes visible (design &sect;8.3, fork F9).
	 *
	 * <p>
	 * <jk>null</jk> means "use the per-region-type default": <jk>false</jk> for {@link #TYPE_ROW_DETAIL} and
	 * {@link #TYPE_CARD_BODY}, <jk>true</jk> for {@link #TYPE_TAB_BODY} &mdash; see {@link #effectiveLazy()}.
	 */
	public Boolean lazy;

	/**
	 * The declared auto-refresh interval, in milliseconds, or <jk>null</jk> for no polling.
	 *
	 * <p>
	 * A value below {@link #MIN_REFRESH_MS} is silently clamped up to the floor rather than rejected, exactly as
	 * {@link ViewDef#poll(long)} and {@code CardFieldList.pollIntervalMs}'s setter already clamp.
	 */
	public Long refreshMs;

	/**
	 * The field catalog: labels, renderers, hrefs, spans and per-field action-bar ids, declared once per region
	 * rather than repeated per row/refresh (design &sect;6.2.2). Projects onto {@code ctx.declared.fields}.
	 *
	 * <p>
	 * <b>Card and tab regions only.</b> A {@link #TYPE_ROW_DETAIL} region's catalog is an author-declared JS
	 * literal handed straight to {@code fieldGrid} client-side (design SD-3); declaring {@link #fields} on a
	 * {@link #TYPE_ROW_DETAIL} region fails {@link #validate()}.
	 */
	public List<Field> fields;

	/**
	 * Chrome's declared title-key allowlist (design &sect;14.2a) &mdash; the one descriptor field whose only
	 * reader is the framework's own title painter, never the populate (see design &sect;8.4 property 4's
	 * three-way partition; the declarative default's source must not reference this field).
	 */
	public List<String> titleFields;

	/**
	 * App-approved populator names allowed on {@link #populate} in addition to
	 * {@link PopulatorAllowlist#BUILTIN_IDS}. Blank entries fail {@link #validate()}.
	 *
	 * <p>
	 * Opting a name in here is authoring discipline, not a security boundary &mdash; see
	 * {@link PopulatorAllowlist}'s class Javadoc.
	 */
	public Set<String> allowedPopulators;

	/**
	 * Creates a region with the given id.
	 *
	 * <p>
	 * SD-3 identity plus populator name for {@link RowDetailDef#region(RegionDef)} (and the table renderer's
	 * projected copy).  Page slots do not go through this factory &mdash; author HTML plus
	 * {@code JuneauViews.regions.mount({ id: populatorName })}.  Not {@code @Deprecated}.
	 * </p>
	 *
	 * @param id This region's own id, unique within its host. Must not be <jk>null</jk> or blank.
	 * @return A new {@link RegionDef}.
	 */
	public static RegionDef create(String id) {
		var r = new RegionDef();
		r.id = id;
		return r;
	}

	/**
	 * Sets the name of the client populator to resolve.
	 *
	 * @param value A populator name, or <jk>null</jk> to use the reserved declarative default.
	 * @return This object.
	 */
	public RegionDef populate(String value) {
		populate = value;
		return this;
	}

	/**
	 * Sets this region's type.
	 *
	 * @param value One of {@link #TYPE_ROW_DETAIL}, {@link #TYPE_CARD_BODY}, {@link #TYPE_TAB_BODY}.
	 * @return This object.
	 */
	public RegionDef type(String value) {
		type = value;
		return this;
	}

	/**
	 * Sets the same-origin data-fetch endpoint.
	 *
	 * @param value The path. Must be a same-origin path template (no {@code ://}, no leading {@code //}, no
	 * 	scheme colon-before-slash, no {@code ..} segment) &mdash; enforced at {@link #validate()}.
	 * @return This object.
	 */
	public RegionDef dataUrl(String value) {
		dataUrl = value;
		return this;
	}

	/**
	 * Sets the author-declared request parameters.
	 *
	 * @param value The parameters. A {@link LinkedHashMap} (or equivalent ordered map) when key order matters.
	 * @return This object.
	 */
	public RegionDef params(Map<String,Object> value) {
		params = value;
		return this;
	}

	/**
	 * Sets the author-declared request parameters from key/value pairs, preserving call order.
	 *
	 * @param keyValuePairs An even-length {@code key, value, key, value, ...} sequence.
	 * @return This object.
	 */
	public RegionDef params(Object...keyValuePairs) {
		if (keyValuePairs.length % 2 != 0)
			throw iaex("RegionDef.params(...) requires an even number of key/value arguments.");
		var m = new LinkedHashMap<String,Object>();
		for (var i = 0; i < keyValuePairs.length; i += 2)
			m.put(String.valueOf(keyValuePairs[i]), keyValuePairs[i + 1]);
		params = m;
		return this;
	}

	/**
	 * Sets the helper name the declarative default paints with.
	 *
	 * @param value A {@code JuneauViews.helpers} function name, e.g. <js>"field-grid"</js>.
	 * @return This object.
	 */
	public RegionDef renderer(String value) {
		renderer = value;
		return this;
	}

	/**
	 * Sets whether this region's first populate is deferred until it becomes visible.
	 *
	 * @param value <jk>true</jk>/<jk>false</jk>, or leave unset (<jk>null</jk>) for the per-region-type default.
	 * @return This object.
	 */
	public RegionDef lazy(boolean value) {
		lazy = value;
		return this;
	}

	/**
	 * Declares that this region should be polled (re-fetched and re-populated) on the given interval.
	 *
	 * <p>
	 * A value below {@link #MIN_REFRESH_MS} is silently clamped up to the floor rather than rejected.
	 *
	 * @param intervalMs The desired polling interval, in milliseconds. Must be positive.
	 * @return This object.
	 */
	public RegionDef refreshMs(long intervalMs) {
		if (intervalMs <= 0)
			throw iaex("RegionDef.refreshMs(...) interval must be positive.");
		refreshMs = SafePathTemplate.clampPollInterval(intervalMs);
		return this;
	}

	/**
	 * Sets the field catalog.
	 *
	 * @param value The catalog, in declaration order. Card/tab regions only &mdash; see {@link #fields}.
	 * @return This object.
	 */
	public RegionDef fields(Field...value) {
		fields = l(value);
		return this;
	}

	/**
	 * Sets chrome's declared title-key allowlist.
	 *
	 * @param value The allowlisted keys.
	 * @return This object.
	 */
	public RegionDef titleFields(String...value) {
		titleFields = l(value);
		return this;
	}

	/**
	 * Opts in app-approved populator names for {@link #populate}, in addition to
	 * {@link PopulatorAllowlist#BUILTIN_IDS}.
	 *
	 * @param value Populator names. Must not contain blank entries.
	 * @return This object.
	 */
	public RegionDef allowPopulators(String...value) {
		allowedPopulators = st(value);
		return this;
	}

	/**
	 * The effective {@link #lazy} value once the per-region-type default (fork F9) is applied.
	 *
	 * <p>
	 * <jk>false</jk> for {@link #TYPE_ROW_DETAIL} (SD-3: the one region populates on expand, not lazily) and
	 * {@link #TYPE_CARD_BODY} (a card is visible when its grid is); <jk>true</jk> for {@link #TYPE_TAB_BODY}
	 * (preserving today's init-on-activate). An explicit {@link #lazy} value always wins over the per-type
	 * default, for any {@link #type} including <jk>null</jk>.
	 *
	 * @return The effective lazy flag.
	 */
	public boolean effectiveLazy() {
		if (lazy != null)
			return lazy;
		return TYPE_TAB_BODY.equals(type);
	}

	/**
	 * Fail-closed bean validation.
	 *
	 * <p>
	 * Beyond the original id/populator checks, this now also enforces: a same-origin {@link #dataUrl}; a
	 * recognized {@link #type} (when set); no nested-map {@link #params} value and no {@link #params} key
	 * colliding with a key already present in {@link #dataUrl}'s own query string (design &sect;8.2.1); the
	 * card/tab-only field catalog and its two startup rejections (design &sect;6.2.2 property 4 / test 16d); and
	 * no blank {@link #titleFields} entry.
	 *
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	public void validate() {
		if (id == null || id.isBlank())
			throw iaex("RegionDef id must not be null or blank.");
		if (allowedPopulators != null)
			for (var name : allowedPopulators)
				if (name == null || name.isBlank())
					throw iaex("allowPopulators entry must not be blank.");
		if (populate != null)
			PopulatorAllowlist.assertAllowed(populate, allowedPopulators);
		if (type != null && !TYPES.contains(type))
			throw iaex("RegionDef '%s' type must be one of %s, was '%s'.", id, TYPES, type);
		validateDataUrlAndParams();
		validateTitleFields();
		validateFields();
	}

	private void validateDataUrlAndParams() {
		if (dataUrl != null && !dataUrl.isBlank() && !RowDetailDef.isSafeDetailEndpoint(dataUrl))
			throw iaex("RegionDef '%s' dataUrl must be a same-origin path (no scheme, no leading '//', no '..' "
				+ "segment): %s", id, dataUrl);
		if (params == null || params.isEmpty())
			return;
		for (var e : params.entrySet()) {
			if (e.getValue() instanceof Map)
				throw iaex("RegionDef '%s' params entry '%s' must not be a nested map/object; encode it into a "
					+ "string value yourself if you need structure.", id, e.getKey());
		}
		var dataUrlKeys = queryKeysOf(dataUrl);
		for (var key : params.keySet())
			if (dataUrlKeys.contains(key))
				throw iaex("RegionDef '%s' params key '%s' collides with a key already present in dataUrl's own "
					+ "query string.", id, key);
	}

	private void validateTitleFields() {
		if (titleFields == null)
			return;
		for (var f : titleFields)
			if (f == null || f.isBlank())
				throw iaex("RegionDef '%s' titleFields entry must not be blank.", id);
	}

	private void validateFields() {
		if (TYPE_ROW_DETAIL.equals(type) && fields != null && !fields.isEmpty())
			throw iaex("RegionDef '%s' is type '%s' and must not declare fields; a row-detail region's catalog "
				+ "is an author JS literal, never a projected one.", id, TYPE_ROW_DETAIL);
		if (fields != null) {
			var keys = new HashSet<String>();
			for (var f : fields) {
				if (f == null)
					throw iaex("RegionDef '%s' fields entry must not be null.", id);
				f.validate(id);
				if (!keys.add(f.data))
					throw iaex("RegionDef '%s' duplicate field data key '%s'.", id, f.data);
			}
		}
		var isDefaultPopulate = populate == null || "default".equals(populate);
		var isCardOrTab = TYPE_CARD_BODY.equals(type) || TYPE_TAB_BODY.equals(type);
		if (isDefaultPopulate && isCardOrTab && RENDERER_FIELD_GRID.equals(renderer) && (fields == null || fields.isEmpty()))
			throw iaex("RegionDef '%s' declares renderer 'field-grid' under the default populator but no fields; "
				+ "a field-grid with no catalog would silently paint an unlabeled/empty grid.", id);
	}

	/** Extracts the query-string keys already present in {@code url}'s own {@code ?...} portion, if any. */
	private static Set<String> queryKeysOf(String url) {
		if (url == null)
			return Set.of();
		var q = url.indexOf('?');
		if (q < 0 || q == url.length() - 1)
			return Set.of();
		var keys = new HashSet<String>();
		for (var pair : url.substring(q + 1).split("&")) {
			if (pair.isEmpty())
				continue;
			var eq = pair.indexOf('=');
			keys.add(eq < 0 ? pair : pair.substring(0, eq));
		}
		return keys;
	}

	/**
	 * Serializes {@code params} to a query-string fragment (no leading {@code ?} or {@code &}) under the closed
	 * rules of design &sect;8.2.1, identically to the JS client's twin ({@code juneau-regions.js}'s
	 * {@code serializeParams}, test 16a's golden). <jk>null</jk>/empty {@code params} serializes to an empty
	 * string.
	 *
	 * <p>
	 * Rules: a <jk>null</jk> value omits the key entirely; an empty string serializes as {@code k=}; a scalar
	 * (String/Number/Boolean) serializes as {@code k=<percent-encoded value>} (booleans as {@code true}/
	 * {@code false}); an array/{@link Collection} repeats the key once per element, in order ({@code k=a&k=b});
	 * a nested {@link Map} is rejected (see {@link #validate()}) rather than reaching this method under normal
	 * use. Encoding is {@code application/x-www-form-urlencoded} <b>without</b> the {@code +}-for-space
	 * substitution: a space is {@code %20}, matching the JS client's {@code encodeURIComponent}.
	 *
	 * @param params The parameters to serialize. May be <jk>null</jk>.
	 * @return The query-string fragment, e.g. {@code "scope=recent&tag=a&tag=b"}.
	 */
	public static String serializeParams(Map<String,Object> params) {
		if (params == null || params.isEmpty())
			return "";
		var sb = new StringBuilder();
		for (var e : params.entrySet()) {
			var key = e.getKey();
			var value = e.getValue();
			if (value == null)
				continue;
			if (value instanceof Map)
				throw iaex("RegionDef.serializeParams(...) params entry '%s' must not be a nested map/object.", key);
			if (value instanceof Collection<?> coll) {
				for (var el : coll)
					appendPair(sb, key, el);
			} else if (value.getClass().isArray()) {
				for (var el : (Object[]) value)
					appendPair(sb, key, el);
			} else {
				appendPair(sb, key, value);
			}
		}
		return sb.toString();
	}

	private static void appendPair(StringBuilder sb, String key, Object value) {
		if (value == null)
			return;
		if (sb.length() > 0)
			sb.append('&');
		sb.append(encode(key)).append('=').append(encode(String.valueOf(value)));
	}

	/** {@code application/x-www-form-urlencoded} encoding, minus the {@code +}-for-space substitution. */
	private static String encode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
	}

	/**
	 * Appends {@link #serializeParams(Map)}'s fragment to {@link #dataUrl}, joining with {@code ?} or {@code &}
	 * as {@code dataUrl} already having a query string requires.
	 *
	 * @return {@link #dataUrl} with {@link #params} appended, or {@link #dataUrl} verbatim when {@link #params}
	 * 	is <jk>null</jk>/empty, or <jk>null</jk> when {@link #dataUrl} itself is <jk>null</jk>.
	 */
	public String resolvedDataUrl() {
		if (dataUrl == null)
			return null;
		var q = serializeParams(params);
		if (q.isEmpty())
			return dataUrl;
		return dataUrl + (dataUrl.indexOf('?') >= 0 ? "&" : "?") + q;
	}

	/**
	 * Serializes this descriptor to the compact JSON envelope the {@code data-juneau-region-contract} sidecar
	 * attribute carries (design &sect;8.2), hand-built (rather than reflected) so the key set, order, and each
	 * nested {@link Field}'s wire shape are exactly the ones &sect;8.2/&sect;6.2.2 specify.
	 *
	 * @return An ordered {@link Map} ready for {@code Json.of(...)}.
	 */
	public Map<String,Object> toContractMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("contractVersion", CONTRACT_VERSION);
		m.put("id", id);
		if (type != null)
			m.put("type", type);
		if (populate != null)
			m.put("populate", populate);
		if (dataUrl != null)
			m.put("dataUrl", dataUrl);
		if (params != null && !params.isEmpty())
			m.put("params", params);
		if (renderer != null)
			m.put("renderer", renderer);
		m.put("lazy", effectiveLazy());
		if (refreshMs != null)
			m.put("refreshMs", refreshMs);
		if (titleFields != null && !titleFields.isEmpty())
			m.put("titleFields", titleFields);
		if (fields != null && !fields.isEmpty()) {
			var fieldMaps = new ArrayList<Map<String,Object>>();
			for (var f : fields)
				fieldMaps.add(f.toContractMap());
			m.put("fields", fieldMaps);
		}
		return m;
	}

	/**
	 * One entry in the field catalog (design &sect;6.2.2): a label, an optional named renderer and its meta, an
	 * optional href template, a paint format, a grid span, and per-field action-bar ids.
	 *
	 * <p>
	 * Card and tab regions only &mdash; see {@link RegionDef#fields}.
	 *
	 * @since 10.0.0
	 */
	@SuppressWarnings({
		"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
	})
	public static class Field {

		/** The key into {@code ctx.data}'s values map. Unique across the enclosing {@link RegionDef}. */
		public String data;

		/** The label shown above the value. <jk>null</jk> falls back to {@link #data} at paint time. */
		public String label;

		/** Optional named renderer id, resolved against the same {@code NS._renderers} registry as today. */
		public String render;

		/** Optional metadata handed to {@link #render}'s renderer function. */
		public Map<String,Object> renderMeta;

		/** Optional {@code {property}} URL template consumed by the {@code linked} renderer (and any renderer
		 * that reads it). Independent of {@link #render}. */
		public String href;

		/** How the value is painted. <jk>null</jk> means {@link FieldFormat#TEXT}. */
		public FieldFormat format;

		/** How many of the grid's columns this field occupies. <jk>null</jk> means {@link FieldSpan#ONE}. */
		public FieldSpan span;

		/**
		 * Action ids painted beside this field's value. Ids only &mdash; the bar itself is rendered by the
		 * client helper, not by this bean.
		 */
		public List<String> actions;

		/**
		 * Creates a field bound to the specified values-map key.
		 *
		 * @param data The {@code ctx.data} key. Must not be <jk>null</jk> or blank.
		 * @return A new {@link Field}.
		 */
		public static Field of(String data) {
			if (data == null || data.isBlank())
				throw iaex("RegionDef.Field data must not be null or blank.");
			var f = new Field();
			f.data = data;
			return f;
		}

		/**
		 * Sets the label.
		 *
		 * @param value The label. An empty string suppresses it (full-width markdown/sanitizedHtml body).
		 * @return This object.
		 */
		public Field label(String value) {
			label = value;
			return this;
		}

		/**
		 * Sets the named renderer id.
		 *
		 * @param value The renderer id.
		 * @return This object.
		 */
		public Field render(String value) {
			render = value;
			return this;
		}

		/**
		 * Sets the renderer's metadata.
		 *
		 * @param value The metadata.
		 * @return This object.
		 */
		public Field renderMeta(Map<String,Object> value) {
			renderMeta = value;
			return this;
		}

		/**
		 * Sets the declarative {@code {property}} URL template.
		 *
		 * @param value The template.
		 * @return This object.
		 */
		public Field href(String value) {
			href = value;
			return this;
		}

		/**
		 * Sets the paint format.
		 *
		 * @param value The format. <jk>null</jk> means {@link FieldFormat#TEXT}.
		 * @return This object.
		 */
		public Field format(FieldFormat value) {
			format = value;
			return this;
		}

		/**
		 * Sets the grid span.
		 *
		 * @param value The span. <jk>null</jk> means {@link FieldSpan#ONE}.
		 * @return This object.
		 */
		public Field span(FieldSpan value) {
			span = value;
			return this;
		}

		/**
		 * Sets the per-field action ids.
		 *
		 * @param value The action ids.
		 * @return This object.
		 */
		public Field actions(String...value) {
			actions = l(value);
			return this;
		}

		/**
		 * Fail-closed validation of this one entry: a non-blank {@link #data}.
		 *
		 * @param regionId The enclosing region's id, for the error message only.
		 */
		void validate(String regionId) {
			if (data == null || data.isBlank())
				throw iaex("RegionDef '%s' field data must not be null or blank.", regionId);
		}

		/**
		 * Serializes this entry to the compact JSON shape design &sect;6.2.2 specifies.
		 *
		 * @return An ordered {@link Map} ready for {@code Json.of(...)}.
		 */
		public Map<String,Object> toContractMap() {
			var m = new LinkedHashMap<String,Object>();
			m.put("data", data);
			if (label != null)
				m.put("label", label);
			if (render != null)
				m.put("render", render);
			if (renderMeta != null && !renderMeta.isEmpty())
				m.put("renderMeta", renderMeta);
			if (href != null)
				m.put("href", href);
			if (format != null && format != FieldFormat.TEXT)
				m.put("format", format.wire());
			if (span == FieldSpan.FULL)
				m.put("span", "full");
			if (actions != null && !actions.isEmpty())
				m.put("actions", actions);
			return m;
		}
	}
}
