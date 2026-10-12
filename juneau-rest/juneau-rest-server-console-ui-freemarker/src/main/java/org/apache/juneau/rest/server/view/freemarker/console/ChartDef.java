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
import java.util.regex.*;

import org.apache.juneau.commons.http.*;
import org.apache.juneau.rest.server.views.*;

/**
 * Typed builder for a {@code chart} card.
 *
 * <p>
 * Produces the same contract entry as {@code <@card type="chart">}; {@link PageSpec} runs it through
 * {@link ChartCardType} when the page renders.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>page</jv>.card(ChartDef.<jsm>line</jsm>(<js>"orders-trend"</js>).title(<js>"Orders"</js>).src(<js>"/console/store/stats/daily"</js>)
 * 		.x(<js>"day"</js>).series(<js>"placed"</js>, <js>"Placed"</js>).series(<js>"delivered"</js>, <js>"Delivered"</js>).toCardSpec());
 * </p>
 *
 * @since 10.0.0
 */
public final class ChartDef {

	/** Minimum chart height in pixels. */
	public static final int MIN_HEIGHT = 120;

	// A chart kind, built-in or registered in the browser; ChartCardType and the page schema hold kinds to the same shape.
	static final Pattern KIND = Pattern.compile("^[a-z][a-z0-9-]{0,31}$");

	private static final Set<String> DEDICATED = Set.of("id", "type", "title", "src", "kind", "data",
		"series", "x", "y", "stacked", "orientation", "legend", "height", "refreshMs", "onClick", "emptyText", "label",
		"value", "center", "visibleWhen");

	// Card-level keys: CardSpec owns them (CardSpec.body rejects publishes/subscribes), so wiring and templates are set
	// on the CardSpec that toCardSpec() returns, never through set(...).
	private static final Set<String> CARD_LEVEL = Set.of("publishes", "subscribes", "template", "bare", "class");

	private final String kind, id;
	private String title, src;
	private List<Map<String,Object>> data;
	private final Map<String,Object> body = new LinkedHashMap<>();
	private final List<Map<String,Object>> series = new ArrayList<>();
	private VisibilityRule[] visibleWhen;

	private ChartDef(String kind, String id) {
		if (ib(kind))
			throw iaex("ChartDef requires a non-blank kind.");
		if (ib(id))
			throw iaex("ChartDef requires a non-blank id.");
		if (! KIND.matcher(kind).matches())
			throw iaex("ChartDef '%s' kind must match '%s'; got '%s'.", id, KIND.pattern(), kind);
		this.kind = kind;
		this.id = id;
	}

	/** @param id The card id. @return A new line chart. */
	public static ChartDef line(String id) { return new ChartDef("line", id); }

	/** @param id The card id. @return A new bar chart. */
	public static ChartDef bar(String id) { return new ChartDef("bar", id); }

	/** @param id The card id. @return A new donut chart. */
	public static ChartDef donut(String id) { return new ChartDef("donut", id); }

	/** @param kind A kind registered with {@code JuneauCharts.register} in the browser, matching {@code ^[a-z][a-z0-9-]{0,31}$}. @param id The card id. @return A new chart. */
	public static ChartDef of(String kind, String id) { return new ChartDef(kind, id); }

	/** @param v The card title. @return This object. */
	public ChartDef title(String v) { title = v; return this; }

	/** @param v A same-origin URL returning an array of rows or {@code {rows:[...]}}. @return This object. */
	public ChartDef src(String v) { src = v; return this; }

	/** @param rows Inline rows (flat maps). @return This object. */
	public ChartDef data(List<? extends Map<String,?>> rows) {
		if (rows == null)
			throw iaex("ChartDef '%s' data rows must not be null.", id);
		data = new ArrayList<>();
		for (var r : rows)
			data.add(new LinkedHashMap<>(r));
		return this;
	}

	/** @param field The x (category) field. @return This object. */
	public ChartDef x(String field) { return x(field, null, null); }

	/**
	 * @param field The x field.
	 * @param type {@code category} (default), {@code number} or {@code time}; <jk>null</jk> for the default.
	 * @param label The axis label, or <jk>null</jk>.
	 * @return This object.
	 */
	public ChartDef x(String field, String type, String label) {
		if (nn(type) && ! Set.of("category", "number", "time").contains(type))
			throw iaex("ChartDef '%s' x type '%s' must be 'category', 'number' or 'time'.", id, type);
		var m = new LinkedHashMap<String,Object>();
		m.put("field", field);
		if (nn(type)) m.put("type", type);
		if (nn(label)) m.put("label", label);
		body.put("x", m);
		return this;
	}

	/**
	 * @param label The value-axis label, or <jk>null</jk>.
	 * @param min The axis minimum, or <jk>null</jk> for 0 (or the data minimum when negative).
	 * @param format {@code integer}, {@code decimal}, {@code percent} or {@code compact}; <jk>null</jk> for the default.
	 * @return This object.
	 */
	public ChartDef y(String label, Number min, String format) {
		var m = new LinkedHashMap<String,Object>();
		if (nn(label)) m.put("label", label);
		if (nn(min)) m.put("min", min);
		if (nn(format)) m.put("format", format);
		body.put("y", m);
		return this;
	}

	/** @param field A numeric row field. @param label The legend label. @return This object. */
	public ChartDef series(String field, String label) { return addSeries(field, label, null); }

	/** @param field A numeric row field. @param label The legend label. @param tone Palette slot 1-8. @return This object. */
	public ChartDef series(String field, String label, int tone) {
		if (tone < 1 || tone > 8)
			throw iaex("ChartDef '%s' series tone '%s' must be 1-8.", id, tone);
		return addSeries(field, label, tone);
	}

	/** @param field A numeric row field. @param label The legend label. @param tone {@code danger}, {@code ok} or {@code warning}. @return This object. */
	public ChartDef series(String field, String label, String tone) {
		if (! Set.of("danger", "ok", "warning").contains(tone))
			throw iaex("ChartDef '%s' series tone '%s' must be 'danger', 'ok' or 'warning'.", id, tone);
		return addSeries(field, label, tone);
	}

	private ChartDef addSeries(String field, String label, Object tone) {
		if (ib(field))
			throw iaex("ChartDef '%s' series requires a non-blank field.", id);
		var m = new LinkedHashMap<String,Object>();
		m.put("field", field);
		if (nn(label)) m.put("label", label);
		if (nn(tone)) m.put("tone", tone);
		series.add(m);
		return this;
	}

	/** @param v Whether bar series stack. @return This object. */
	public ChartDef stacked(boolean v) { body.put("stacked", v); return this; }

	/** Draws bars horizontally. @return This object. */
	public ChartDef horizontal() { body.put("orientation", "horizontal"); return this; }

	/** @param v {@code bottom} (default), {@code right} or {@code none}. @return This object. */
	public ChartDef legend(String v) {
		if (! Set.of("bottom", "right", "none").contains(v))
			throw iaex("ChartDef '%s' legend '%s' must be 'bottom', 'right' or 'none'.", id, v);
		body.put("legend", v);
		return this;
	}

	/** @param px The plot height, at least {@link #MIN_HEIGHT}. @return This object. */
	public ChartDef height(int px) {
		if (px < MIN_HEIGHT)
			throw iaex("ChartDef '%s' height '%s' is below the minimum '%s'.", id, px, MIN_HEIGHT);
		body.put("height", px);
		return this;
	}

	/** @param ms The re-fetch interval, at least {@link SafePathTemplate#MIN_POLL_INTERVAL_MS}. @return This object. */
	public ChartDef refreshMs(long ms) {
		if (ms < SafePathTemplate.MIN_POLL_INTERVAL_MS)
			throw iaex("ChartDef '%s' refreshMs '%s' is below the minimum '%s'.", id, ms, SafePathTemplate.MIN_POLL_INTERVAL_MS);
		body.put("refreshMs", ms);
		return this;
	}

	/**
	 * @param template A same-origin path template ({@link SafePathTemplate#isSameOriginPath(String)}); {@code {field}}
	 * 	is replaced with the clicked row's value.
	 * @return This object.
	 */
	public ChartDef onClickHref(String template) {
		if (! SafePathTemplate.isSameOriginPath(template))
			throw iaex("ChartDef '%s' onClick href must be a same-origin path; got '%s'.", id, template);
		body.put("onClick", Map.of("href", template));
		return this;
	}

	/** @param v The text shown when there are no rows. @return This object. */
	public ChartDef emptyText(String v) { body.put("emptyText", v); return this; }

	/** @param field The donut slice-label field. @return This object. */
	public ChartDef label(String field) { body.put("label", field); return this; }

	/** @param field The donut slice-value field. @return This object. */
	public ChartDef value(String field) { body.put("value", field); return this; }

	/** @param v {@code total} or literal center text. @return This object. */
	public ChartDef center(String v) { body.put("center", v); return this; }

	/**
	 * Sets a key for a kind registered in the browser.
	 *
	 * @param key A key with no dedicated method.
	 * @param value Its value.
	 * @return This object.
	 */
	public ChartDef set(String key, Object value) {
		if (CARD_LEVEL.contains(key))
			throw iaex("ChartDef '%s' key '%s' is card-level; set it on toCardSpec().", id, key);
		if (DEDICATED.contains(key))
			throw iaex("ChartDef '%s' key '%s' has a dedicated method.", id, key);
		body.put(key, value);
		return this;
	}

	/** @param rules Card-level visibility rules, ANDed. @return This object. */
	public ChartDef visibleWhen(VisibilityRule... rules) { visibleWhen = rules; return this; }

	/**
	 * @return The {@code chart} card.
	 * @throws IllegalArgumentException If neither or both of {@code src}/{@code data} are set, a line/bar chart
	 * 	lacks {@code x} or a series, or a donut chart lacks {@code label} or {@code value}.
	 */
	public CardSpec toCardSpec() {
		if (nn(src) == nn(data))
			throw iaex("ChartDef '%s' requires exactly one of src(...) or data(...).", id);
		if (nn(data) && body.containsKey("refreshMs"))
			throw iaex("ChartDef '%s' refreshMs(...) needs src(...); inline data never refreshes.", id);
		if (Set.of("line", "bar").contains(kind) && (! body.containsKey("x") || series.isEmpty()))
			throw iaex("ChartDef '%s' kind '%s' requires x(...) and at least one series(...).", id, kind);
		if (eq(kind, "donut") && (! (body.get("label") instanceof String l) || l.isBlank() || ! (body.get("value") instanceof String v) || v.isBlank()))
			throw iaex("ChartDef '%s' kind 'donut' requires label(...) and value(...).", id);
		var c = CardSpec.of("chart", id).body("kind", kind);
		if (nn(title)) c.title(title);
		if (nn(src)) c.src(src);
		body.forEach(c::body);
		if (! series.isEmpty()) c.body("series", List.copyOf(series));
		if (nn(data)) c.body("data", List.copyOf(data));
		if (nn(visibleWhen)) c.visibleWhen(visibleWhen);
		return c;
	}
}
