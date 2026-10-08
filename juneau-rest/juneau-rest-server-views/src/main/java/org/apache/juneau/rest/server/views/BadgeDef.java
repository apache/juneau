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

/**
 * A count badge in the page contract, fed by a JSON endpoint and polled by {@code juneau-badges.js}.
 *
 * <p>
 * The endpoint returns {@code {total, mine?, items?[]}}.  The badge is presentation only: the endpoint must
 * still authorize the caller, and a page should not declare a badge for users who cannot read it.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>badge</jv> = BadgeDef
 * 		.<jsm>create</jsm>(<js>"pending"</js>)
 * 		.src(<js>"/rest/changes/pending"</js>)
 * 		.scope(<js>"beanType"</js>, BadgeDef.ScopeBy.<jsf>NAV</jsf>, Map.<jsm>of</jsm>(<js>"suspensions"</js>, List.<jsm>of</jsm>(<js>"Suspension"</js>)))
 * 		.href(<js>"/ui/changes"</js>)
 * 		.stateFilter(<js>"beanType"</js>)
 * 		.refreshes(<js>"rules-table"</js>)
 * 		.refreshMs(60_000);
 * 	<jv>badge</jv>.validate();  <jc>// throws IllegalArgumentException with an E-6x message</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class BadgeDef {

	/** The minimum poll interval, in milliseconds (schema §4.8 {@code refreshMs.minimum}). */
	public static final long MIN_REFRESH_MS = 5000L;

	/** Default poll interval, in milliseconds, applied by {@code juneau-badges.js} when {@link #refreshMs} is unset. */
	public static final long DEFAULT_REFRESH_MS = 60_000L;

	/** Default tooltip item cap, applied by {@code juneau-badges.js} when {@link #tooltipMax} is unset. */
	public static final int DEFAULT_TOOLTIP_MAX = 10;

	/** {@code scope.by} wire tokens (schema §4.8). */
	public enum ScopeBy {
		/** Keys on the leaf of the page's {@code activeNav}. */
		NAV("nav"),
		/** Keys on the active tab of the card named by {@link BadgeDef#table}. */
		VIEW("view");

		private final String wire;
		ScopeBy(String wire) { this.wire = wire; }
		/** @return The lowercase wire token (e.g. <c>"nav"</c>). */
		public String wire() { return wire; }
	}

	/** {@code placement} wire tokens (schema §4.8). */
	public enum Placement {
		/** Renders into {@code .jc-header-badges} at the end of the header actions area. */
		HEADER("header"),
		/** Renders at the right end of {@link BadgeDef#table}'s ribbon. */
		TOOLBAR("toolbar");

		private final String wire;
		Placement(String wire) { this.wire = wire; }
		/** @return The lowercase wire token (e.g. <c>"header"</c>). */
		public String wire() { return wire; }
	}

	/** {@code tone} wire tokens (schema §4.8). */
	public enum Tone {
		/** Informational tone. */
		INFO("info"),
		/** Warning tone (the default). */
		WARNING("warning"),
		/** Danger tone. */
		DANGER("danger");

		private final String wire;
		Tone(String wire) { this.wire = wire; }
		/** @return The lowercase wire token (e.g. <c>"warning"</c>). */
		public String wire() { return wire; }
	}

	private String id;
	private String src;
	private Map<String,Object> params;
	private Long refreshMs;
	private String scopeParam;
	private ScopeBy scopeBy;
	private Map<String,List<String>> scopeValues;
	private Map<String,String> label;
	private Integer tooltipMax;
	private String href;
	private String stateFilterColumn;
	private List<String> refreshes;
	private Placement placement;
	private String table;
	private Tone tone;
	private List<VisibilityRule> visibleWhen;

	private BadgeDef() {}

	/**
	 * Creates a badge with the given id.
	 *
	 * @param id This badge's own id, unique across every Java- and FTL-declared badge on the page (C3 E-B15).
	 * 	Must not be <jk>null</jk> or blank.
	 * @return A new {@link BadgeDef}.
	 * @throws IllegalArgumentException If {@code id} is <jk>null</jk> or blank.
	 */
	public static BadgeDef create(String id) {
		if (id == null || id.isBlank())
			throw iaex("BadgeDef id must not be null or blank.");
		var b = new BadgeDef();
		b.id = id;
		return b;
	}

	/**
	 * Sets the same-origin endpoint this badge polls.
	 *
	 * @param value The endpoint path.  Checked for presence, not shape, by {@link #validate()} (E-61).
	 * @return This object.
	 */
	public BadgeDef src(String value) {
		src = value;
		return this;
	}

	/**
	 * Sets extra request parameters merged into every poll (ahead of the scope params).
	 *
	 * @param value The parameters.
	 * @return This object.
	 */
	public BadgeDef params(Map<String,Object> value) {
		params = value;
		return this;
	}

	/**
	 * Sets the poll interval.
	 *
	 * @param intervalMs The desired interval, in milliseconds.  Checked against {@link #MIN_REFRESH_MS} by
	 * 	{@link #validate()} (E-62) &mdash; unlike {@link RegionDef#refreshMs(long)}, this is rejected, not clamped.
	 * @return This object.
	 */
	public BadgeDef refreshMs(long intervalMs) {
		refreshMs = intervalMs;
		return this;
	}

	/**
	 * Sets this badge's scope (§4.3): the request param to key the poll on, how to resolve its value, and
	 * (optionally) the authoritative value map.
	 *
	 * @param param The query parameter name sent with the poll.  Must not be <jk>null</jk> or blank.
	 * @param by {@link ScopeBy#NAV} or {@link ScopeBy#VIEW}.
	 * @param values The authoritative value map, keyed by nav-leaf or view-tab id, or <jk>null</jk> for an
	 * 	unscoped badge that still carries a {@code by}.
	 * @return This object.
	 */
	public BadgeDef scope(String param, ScopeBy by, Map<String,List<String>> values) {
		scopeParam = param;
		scopeBy = by;
		scopeValues = values;
		return this;
	}

	/**
	 * Sets this badge's label templates (§4.4).
	 *
	 * @param value A map with any of the keys {@code one}, {@code other}, {@code mine}, each a {@code {total}}/
	 * 	{@code {mine}} template.  Unset keys fall back to the stock IRS-equivalent defaults in {@code juneau-badges.js}.
	 * @return This object.
	 */
	public BadgeDef label(Map<String,String> value) {
		label = value;
		return this;
	}

	/**
	 * Sets the tooltip popover's item cap (§4.5).
	 *
	 * @param value The cap, 0-50.  Not range-checked here; schema `$defs/badge.tooltipMax` enforces it for
	 * 	FTL-declared badges, and an out-of-range Java value is simply clamped by {@code juneau-badges.js} at
	 * 	render time.
	 * @return This object.
	 */
	public BadgeDef tooltipMax(int value) {
		tooltipMax = value;
		return this;
	}

	/**
	 * Sets the click-target URL (§4.6).
	 *
	 * @param value The destination.  A same-origin path is strongly recommended; the C1 URL safety check applies
	 * 	client-side regardless (an unsafe {@code href} is dropped with E-JS-9).
	 * @return This object.
	 */
	public BadgeDef href(String value) {
		href = value;
		return this;
	}

	/**
	 * Sets the column {@code href}'s destination page filters on, via {@code JuneauViews.urlState.encode(...)}
	 * (§4.6).  Requires a non-empty {@link #scope} to have any effect.
	 *
	 * @param column The destination table's filter column name.
	 * @return This object.
	 */
	public BadgeDef stateFilter(String column) {
		stateFilterColumn = column;
		return this;
	}

	/**
	 * Sets the card ids to {@code JuneauViews.tables.reload(el, {resetPaging: false})} when a previously-seen
	 * item id drains from the badge's response (§4.1 step 6).
	 *
	 * @param value The card ids, in reload order.
	 * @return This object.
	 */
	public BadgeDef refreshes(String...value) {
		refreshes = l(value);
		return this;
	}

	/**
	 * Sets where this badge renders (§4.7).
	 *
	 * @param value {@link Placement#HEADER} (default) or {@link Placement#TOOLBAR}.
	 * @return This object.
	 */
	public BadgeDef placement(Placement value) {
		placement = value;
		return this;
	}

	/**
	 * Sets the card id this badge is anchored to.  Required when {@link #placement} is {@link Placement#TOOLBAR}
	 * (E-66) and when {@link #scope}'s {@code by} is {@link ScopeBy#VIEW} (R-7, uncoded).
	 *
	 * @param value The card id.
	 * @return This object.
	 */
	public BadgeDef table(String value) {
		table = value;
		return this;
	}

	/**
	 * Sets this badge's tone (§4.11: Juneau's own tokens, not IRS's gold styling).
	 *
	 * @param value {@link Tone#INFO}, {@link Tone#WARNING} (default) or {@link Tone#DANGER}.
	 * @return This object.
	 */
	public BadgeDef tone(Tone value) {
		tone = value;
		return this;
	}

	/**
	 * Sets the dynamic-visibility rules gating this badge (§8.2); all must match (AND).
	 *
	 * @param value The rules, or none for an always-visible badge.
	 * @return This object.
	 */
	public BadgeDef visibleWhen(VisibilityRule...value) {
		visibleWhen = l(value);
		return this;
	}

	/**
	 * Validates this definition.
	 *
	 * <p>
	 * Enforces E-61 (no {@link #src}), E-62 ({@link #refreshMs} below {@link #MIN_REFRESH_MS}), E-66
	 * ({@link Placement#TOOLBAR} without {@link #table}), and R-7 (uncoded: {@link ScopeBy#VIEW} without
	 * {@link #table}).  R-6's "table/refreshes name a real card id" and R-8's "scope.values key is a real nav
	 * node id" need the rest of the page and run in {@code PageCapture} at {@code </@console>} instead (§4.10).
	 *
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	public void validate() {
		if (src == null || src.isBlank())
			throw iaex("<@badge id='%s'> requires src=.", id);
		if (refreshMs != null && refreshMs < MIN_REFRESH_MS)
			throw iaex("'%s' refreshMs='%s' is below the minimum '%s'.", id, refreshMs, MIN_REFRESH_MS);
		if (placement == Placement.TOOLBAR && (table == null || table.isBlank()))
			throw iaex("<@badge id='%s'> placement='toolbar' requires table=.", id);
		if (scopeBy == ScopeBy.VIEW && (table == null || table.isBlank()))
			throw iaex("BadgeDef '%s' scope.by='view' requires table=.", id);
	}

	/**
	 * Serializes this definition to the compact JSON the {@code header.badges[]} contract carries (§4.8), hand-built
	 * (rather than reflected) so the key set and order match the schema exactly.
	 *
	 * @return An ordered {@link Map} ready for {@code Json.of(...)}.
	 */
	public Map<String,Object> toContractMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("id", id);
		if (src != null)
			m.put("src", src);
		if (params != null && !params.isEmpty())
			m.put("params", params);
		if (refreshMs != null)
			m.put("refreshMs", refreshMs);
		if (scopeParam != null) {
			var scope = new LinkedHashMap<String,Object>();
			scope.put("param", scopeParam);
			scope.put("by", (scopeBy == null ? ScopeBy.NAV : scopeBy).wire());
			if (scopeValues != null && !scopeValues.isEmpty())
				scope.put("values", scopeValues);
			m.put("scope", scope);
		}
		if (label != null && !label.isEmpty())
			m.put("label", label);
		if (tooltipMax != null)
			m.put("tooltipMax", tooltipMax);
		if (href != null)
			m.put("href", href);
		if (stateFilterColumn != null)
			m.put("stateFilter", Map.of("column", stateFilterColumn));
		if (refreshes != null && !refreshes.isEmpty())
			m.put("refreshes", refreshes);
		if (placement != null)
			m.put("placement", placement.wire());
		if (table != null)
			m.put("table", table);
		if (tone != null)
			m.put("tone", tone.wire());
		if (visibleWhen != null && !visibleWhen.isEmpty())
			m.put("visibleWhen", VisibilityRule.toMaps(visibleWhen));
		return m;
	}
}
