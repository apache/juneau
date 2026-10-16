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

import org.apache.juneau.commons.http.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.views.*;

/**
 * Typed builder for a {@code chart} card.
 *
 * <p>
 * Produces the same contract entry as {@code <@card type="chart">}; {@link PageSpec} runs it through
 * {@link ChartCardType} when the page renders.  The factories are {@link #line(String)}, {@link #bar(String)},
 * {@link #donut(String)}, {@link #heatmap(String)}, {@link #scatter(String)}, {@link #pie(String)}, {@link #treemap(String)} and
 * {@link #raw(String)}.  {@code line}, {@code bar} and {@code donut} emit the flat legacy form
 * ({@code kind:"line", x:..., series:[...]}) byte-for-byte; the other kinds emit {@code helper:{kind, ...}}, and
 * {@code raw} emits neither (its {@link #option(JsonMap)} is the whole chart).
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

	/** The kinds ChartCardType and the page schema accept. */
	static final Set<String> KINDS = Set.of("line", "bar", "donut", "heatmap", "scatter", "pie", "treemap");

	// The legacy kinds keep their flat card JSON byte-for-byte; the others emit helper:{kind,...}.
	static final Set<String> LEGACY_KINDS = Set.of("line", "bar", "donut");

	// Keys a helper reads; they go under helper:{} for the non-legacy kinds and stay flat for the legacy ones.
	static final Set<String> HELPER_KEYS = Set.of("x", "y", "series", "stacked", "orientation", "legend", "label",
		"value", "center", "scale", "symbolSize", "groupBy", "size", "color", "tileFields");

	private static final Set<String> DEDICATED = Set.of("id", "type", "title", "src", "kind", "data",
		"series", "x", "y", "stacked", "orientation", "legend", "height", "refreshMs", "onClick", "emptyText", "label",
		"value", "center", "visibleWhen", "source", "helper", "option", "patch", "decorators", "on", "group", "renderer",
		"theme", "largeData", "partialFrames", "a11y", "maps", "scale", "symbolSize", "groupBy", "size", "color", "tileFields",
		"topicActions", "actions");

	// An id that can be the namespace of a custom topic.
	private static final java.util.regex.Pattern CLICK_NAMESPACE = java.util.regex.Pattern.compile("[a-z][a-z0-9-]{0,31}");

	private static final Set<String> ACTION_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

	// Tones a treemap colour-key entry accepts (null is neutral): the chart tone names or a palette slot token.
	static final Set<String> TREEMAP_TONES = Set.of("ok", "warning", "danger", "--jc-chart-1", "--jc-chart-2",
		"--jc-chart-3", "--jc-chart-4", "--jc-chart-5", "--jc-chart-6", "--jc-chart-7", "--jc-chart-8");

	// A tile field is a bare row-field name, so that {meta.<field>} is always a valid template token.
	static final java.util.regex.Pattern TILE_FIELD = java.util.regex.Pattern.compile("[A-Za-z0-9_.-]+");

	// Card-level keys: CardSpec owns them (CardSpec.body rejects publishes/subscribes), so wiring and templates are set
	// on the CardSpec that toCardSpec() returns, never through set(...).
	private static final Set<String> CARD_LEVEL = Set.of("publishes", "subscribes", "template", "bare", "class");

	private final String kind, id;
	private String title, src, source;
	private List<Map<String,Object>> data;
	private final Map<String,Object> body = new LinkedHashMap<>();
	private final List<Map<String,Object>> series = new ArrayList<>();
	private VisibilityRule[] visibleWhen;
	private Object option;                          // JsonMap, or a JSON5 String parsed in toCardSpec()
	private final List<Object> patches = new ArrayList<>();
	private final List<JsonMap> decorators = new ArrayList<>();
	private final Map<String,String> on = new LinkedHashMap<>();
	private final Map<String,String> maps = new LinkedHashMap<>();
	private final Map<String,Boolean> publishes = new LinkedHashMap<>();
	private final List<Map<String,Object>> topicActions = new ArrayList<>();
	private final List<Map<String,Object>> catalog = new ArrayList<>();
	private final Set<String> actionIds = new HashSet<>();

	private ChartDef(String kind, String id) {
		if (ib(id))
			throw iaex("ChartDef requires a non-blank id.");
		this.kind = kind;
		this.id = id;
	}

	/** @param id The card id. @return A new line chart. */
	public static ChartDef line(String id) { return new ChartDef("line", id); }

	/** @param id The card id. @return A new bar chart. */
	public static ChartDef bar(String id) { return new ChartDef("bar", id); }

	/** @param id The card id. @return A new donut chart. */
	public static ChartDef donut(String id) { return new ChartDef("donut", id); }

	/** @param id The card id. @return A new heatmap (category x, category y, a value field). */
	public static ChartDef heatmap(String id) { return new ChartDef("heatmap", id); }

	/** @param id The card id. @return A new scatter chart. */
	public static ChartDef scatter(String id) { return new ChartDef("scatter", id); }

	/** @param id The card id. @return A new pie chart. */
	public static ChartDef pie(String id) { return new ChartDef("pie", id); }

	/**
	 * A treemap: rows nested into boxes by {@link #groupBy(String...)}, one tile per row, with pan and zoom.
	 *
	 * <p>
	 * A click on a tile publishes the row's {@link #tileFields(String...)} as {@code meta} (the tile's {@code row} is
	 * always <jk>null</jk>); a click on a group does nothing.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ChartDef.<jsm>treemap</jsm>(<js>"fleet"</js>).src(<js>"/api/hosts"</js>).groupBy(<js>"region"</js>, <js>"cluster"</js>).label(<js>"host"</js>)
	 * 		.colorBy(<js>"health"</js>, ChartDef.<jsm>legendItem</jsm>(<js>"ok"</js>, <js>"Healthy"</js>, <js>"ok"</js>),
	 * 			ChartDef.<jsm>legendItem</jsm>(<js>"down"</js>, <js>"Down"</js>, <js>"danger"</js>))
	 * 		.tileFields(<js>"host"</js>, <js>"health"</js>);
	 * </p>
	 *
	 * @param id The card id.
	 * @return A new treemap; set its groups with {@link #groupBy(String...)} and its tile label with {@link #label(String)}.
	 */
	public static ChartDef treemap(String id) { return new ChartDef("treemap", id); }

	/**
	 * A chart with no helper.  Anything ECharts draws works this way, for example a box plot:
	 * <p class='bjava'>
	 * 	ChartDef.<jsm>raw</jsm>(<js>"latency"</js>).src(<js>"/stats/latency"</js>)
	 * 		.option(<js>"{xAxis:{type:'category'},yAxis:{},series:[{type:'boxplot',encode:{x:'day',y:['lo','q1','med','q3','hi']}}]}"</js>);
	 * </p>
	 *
	 * @param id The card id.
	 * @return A new chart with no helper; set its ECharts option with {@link #option(JsonMap)}.
	 */
	public static ChartDef raw(String id) { return new ChartDef(null, id); }

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
		onClickExcludes("action");
		body.put("onClick", Map.of("href", template));
		return this;
	}

	/**
	 * Runs a row action when a mark is clicked.
	 *
	 * <p>
	 * A treemap tile runs it; clicking a treemap group header only zooms and runs nothing.
	 *
	 * <p>
	 * Declares {@code on("click", "<id>.click")} unless a click topic is already declared, and runs {@code action}
	 * whenever that topic fires.  Exclusive with {@link #onClickHref(String)}.  The action is refused unless it has an
	 * {@code endpoint} (and, if set, a {@code form}) that is a same-origin path, a non-safe {@code method}, and no
	 * {@code present} other than {@code dialog}, and does not navigate on success.
	 *
	 * @param action The action; its id must be unique across the card's actions.
	 * @return This object.
	 * @throws IllegalArgumentException If the action is not valid on a chart, {@link #onClickHref(String)} was set, or no click
	 * 	topic can be derived: the id must match <c>[a-z][a-z0-9-]{0,31}</c>, or call {@link #on(String, String)} with <js>"click"</js> first.
	 */
	public ChartDef onClickAction(RowAction action) {
		if (action == null)
			throw iaex("ChartDef '%s' onClickAction requires an action.", id);
		onClickExcludes("href");
		var m = toMap(action);
		checkAction("onClick action", m);
		if (body.get("onClick") instanceof Map<?,?> old && old.get("action") instanceof Map<?,?> prev)
			actionIds.remove(prev.get("id"));
		claimId(m);
		if (! on.containsKey("click")) {
			if (! CLICK_NAMESPACE.matcher(id).matches())
				throw iaex("ChartDef '%s' onClickAction cannot derive a click topic from the id; declare one with on(\"click\", topic).", id);
			on("click", id + ".click");
		}
		body.put("onClick", Map.of("action", m));
		return this;
	}

	/**
	 * Runs a row action when a topic this card publishes fires.
	 *
	 * <p>
	 * Only a click has a keyboard equivalent (Enter on the focused point synthesizes a click); an action bound to another event,
	 * such as {@code dblclick}, is reachable by pointer only.  A treemap group node publishes no events.
	 *
	 * @param topic A topic declared through {@link #on(String, String)}.
	 * @param action The action; its id must be unique across the card's actions.
	 * @return This object.
	 * @throws IllegalArgumentException If the topic is not declared, or the action is not valid on a chart.
	 */
	public ChartDef onTopicAction(String topic, RowAction action) {
		if (topic == null || ! on.containsValue(topic))
			throw iaex("ChartDef '%s' onTopicAction topic '%s' must be one this card publishes through on(...).", id, topic);
		if (action == null)
			throw iaex("ChartDef '%s' onTopicAction requires an action.", id);
		var m = toMap(action);
		checkAction("topicActions[" + topicActions.size() + "] action", m);
		claimId(m);
		var e = new LinkedHashMap<String,Object>();
		e.put("topic", topic);
		e.put("action", m);
		topicActions.add(e);
		return this;
	}

	/**
	 * Adds dialog actions that other actions can open as steps.
	 *
	 * <p>
	 * Each entry must have {@code present} set to {@code dialog}, and obeys the same rules as
	 * {@link #onClickAction(RowAction)}.  Calls accumulate.
	 *
	 * @param actions The actions; ids must be unique across the card's actions.
	 * @return This object.
	 * @throws IllegalArgumentException If an entry is <jk>null</jk>, is not a dialog action, or is not valid on a chart.
	 */
	public ChartDef actions(RowAction... actions) {
		if (actions == null)
			throw iaex("ChartDef '%s' actions entries must not be null.", id);
		for (var a : actions) {
			if (a == null)
				throw iaex("ChartDef '%s' actions entries must not be null.", id);
			var m = toMap(a);
			var where = "actions[" + catalog.size() + "]";
			checkAction(where, m);
			if (! "dialog".equals(m.get("present")))
				throw iaex("ChartDef '%s' %s '%s' must be present=dialog; a dialog button can only open a dialog action.", id, where, m.get("id"));
			claimId(m);
			catalog.add(m);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> toMap(RowAction a) {
		return (Map<String,Object>)Json.to(Json.of(a), Object.class);
	}

	private void onClickExcludes(String other) {
		if (body.get("onClick") instanceof Map<?,?> m && m.containsKey(other))
			throw iaex("ChartDef '%s' onClick takes either href or action, not both.", id);
	}

	// The rules every chart action obeys, wherever it is declared: a chart has no cell, page or servlet to hand off to.
	private void checkAction(String where, Map<String,Object> m) {
		if (! (m.get("endpoint") instanceof String endpoint) || endpoint.isBlank())
			throw iaex("ChartDef '%s' %s requires a non-blank endpoint.", id, where);
		if (! SafePathTemplate.isSameOriginPath(endpoint))
			throw iaex("ChartDef '%s' %s endpoint must be a same-origin path; got '%s'.", id, where, endpoint);
		if (m.get("form") instanceof String form && ! SafePathTemplate.isSameOriginPath(form))
			throw iaex("ChartDef '%s' %s form must be a same-origin path; got '%s'.", id, where, form);
		if (! (m.get("method") instanceof String method) || ! ACTION_METHODS.contains(method))
			throw iaex("ChartDef '%s' %s method must be POST, PUT, PATCH or DELETE; got '%s'.", id, where, m.get("method"));
		if (nn(m.get("present")) && ! "dialog".equals(m.get("present")))
			throw iaex("ChartDef '%s' %s present must be 'dialog' or absent; got '%s'.", id, where, m.get("present"));
		if ("navigate".equals(m.get("onSuccess")))
			throw iaex("ChartDef '%s' %s onSuccess must not be 'navigate'; a chart has nowhere to navigate.", id, where);
	}

	// Ids are unique across onClick.action, topicActions[] and actions[]: lookup is first-match.
	private void claimId(Map<String,Object> m) {
		if (! actionIds.add((String)m.get("id")))
			throw iaex("ChartDef '%s' action id '%s' is declared more than once.", id, m.get("id"));
	}

	/** @param v The text shown when there are no rows. @return This object. */
	public ChartDef emptyText(String v) { body.put("emptyText", v); return this; }

	/** @param fields The treemap group fields, outermost first; at least one, each non-blank. @return This object. */
	public ChartDef groupBy(String... fields) {
		only("treemap", "groupBy");
		if (fields == null || fields.length == 0 || Arrays.stream(fields).anyMatch(f -> ib(f)))
			throw iaex("ChartDef '%s' groupBy requires at least one non-blank field.", id);
		body.put("groupBy", List.of(fields));
		return this;
	}

	/** @param field The numeric treemap field that sizes each tile; without it every tile has the same size. @return This object. */
	public ChartDef size(String field) {
		only("treemap", "size");
		if (ib(field))
			throw iaex("ChartDef '%s' size requires a non-blank field.", id);
		body.put("size", field);
		return this;
	}

	/**
	 * @param field The treemap field whose value picks each tile's colour.
	 * @param legend The colour key, one {@link #legendItem(String, String, String)} per value; a value with no entry is neutral.
	 * @return This object.
	 */
	public ChartDef colorBy(String field, JsonMap... legend) {
		only("treemap", "colorBy");
		if (ib(field))
			throw iaex("ChartDef '%s' colorBy requires a non-blank field.", id);
		if (legend == null || Arrays.stream(legend).anyMatch(Objects::isNull))
			throw iaex("ChartDef '%s' colorBy legend entries must not be null.", id);
		body.put("color", JsonMap.of("field", field, "legend", List.of(legend)));
		return this;
	}

	/** @param fields The row fields a tile click publishes as {@code meta} and its tooltip shows; each matches {@code [A-Za-z0-9_.-]+}. @return This object. */
	public ChartDef tileFields(String... fields) {
		only("treemap", "tileFields");
		if (fields == null)
			throw iaex("ChartDef '%s' tileFields must not be null.", id);
		for (var f : fields)
			if (f == null || ! TILE_FIELD.matcher(f).matches())
				throw iaex("ChartDef '%s' tileFields entry '%s' must match [A-Za-z0-9_.-]+.", id, f);
		body.put("tileFields", List.of(fields));
		return this;
	}

	/**
	 * One entry of a treemap colour key.
	 *
	 * @param value The field value this entry matches.
	 * @param label The key text; defaults to the value.
	 * @param tone {@code ok}, {@code warning}, {@code danger}, {@code --jc-chart-1} to {@code --jc-chart-8}, or <jk>null</jk> for neutral.
	 * @return The entry, for {@link #colorBy(String, JsonMap...)}.
	 */
	public static JsonMap legendItem(String value, String label, String tone) {
		if (ib(value))
			throw iaex("ChartDef legendItem requires a non-blank value.");
		if (nn(tone) && ! TREEMAP_TONES.contains(tone))
			throw iaex("ChartDef legendItem tone '%s' must be 'ok', 'warning', 'danger', '--jc-chart-1'..'--jc-chart-8' or null.", tone);
		var m = JsonMap.of("value", value, "label", nn(label) ? label : value);
		if (nn(tone)) m.put("tone", tone);
		return m;
	}

	/** @param field The donut slice-label field, or the treemap tile-label field. @return This object. */
	public ChartDef label(String field) { body.put("label", field); return this; }

	/** @param field The donut slice-value field. @return This object. */
	public ChartDef value(String field) { body.put("value", field); return this; }

	/** @param v {@code total} or literal center text. @return This object. */
	public ChartDef center(String v) { body.put("center", v); return this; }

	/** @param v A raw ECharts option (JSON only; functions through {@link Fn}). @return This object. */
	public ChartDef option(JsonMap v) { option = rnn(v); return this; }

	/** @param json5 A raw ECharts option as JSON5, parsed by {@link #toCardSpec()}. @return This object. */
	public ChartDef option(String json5) { option = rnn(json5); return this; }

	/** @param d A decorator; a {@code topic} it names is added to the card's {@code publishes}. @return This object. */
	public ChartDef decorate(Decorator d) {
		var m = d.toJson();
		Fn.checkName(m.get("use") instanceof String s ? s : null, "decorator");
		decorators.add(m);
		if (m.get("topic") instanceof String t)
			addPublish(t, false);
		return this;
	}

	/** @param name A dotted app decorator name (or a built-in). @param args Its arguments (no {@code use} key). @return This object. */
	public ChartDef decorate(String name, JsonMap args) {
		Fn.checkName(name, "decorator");
		if (args != null && args.containsKey("use"))
			throw iaex("ChartDef '%s' decorator '%s' args must not set 'use'.", id, name);
		var m = JsonMap.of("use", name);
		if (args != null)
			m.putAll(args);
		decorators.add(m);
		return this;
	}

	/** @param v Deep-merged into the final option (objects by key, arrays by element {@code id}). @return This object. */
	public ChartDef patch(JsonMap v) { patches.add(rnn(v)); return this; }

	/** @param json5 A patch as JSON5, parsed by {@link #toCardSpec()}. @return This object. */
	public ChartDef patch(String json5) { patches.add(rnn(json5)); return this; }

	/**
	 * @param event An ECharts event name matching {@code ^[a-z][a-z0-9_]{0,31}$}.
	 * @param topic A custom topic; added to the card's {@code publishes}.
	 * @return This object.
	 */
	public ChartDef on(String event, String topic) {
		if (event == null || ! BusWiringValidator.EVENT_NAME.matcher(event).matches())
			throw new IllegalArgumentException("E-74: " + f(BusWiringValidator.E74, event, "event"));
		addPublish(topic, false);
		on.put(event, topic);
		return this;
	}

	/** Publishes the zoom window on {@code zoom:<id>}. @return This object. */
	public ChartDef publishZoom() { addPublish(Topics.zoom(id), true); return this; }

	/** Publishes the legend selection on {@code legend:<id>}. @return This object. */
	public ChartDef publishLegend() { addPublish(Topics.legend(id), true); return this; }

	/** @param v A connect group shared with other chart cards. @return This object. */
	public ChartDef group(String v) {
		if (ib(v))
			throw iaex("ChartDef '%s' group must be non-blank.", id);
		body.put("group", v);
		return this;
	}

	/** @param v {@code canvas} (default) or {@code svg}. @return This object. */
	public ChartDef renderer(String v) {
		if (! Set.of("canvas", "svg").contains(v))
			throw iaex("ChartDef '%s' renderer '%s' must be 'canvas' or 'svg'.", id, v);
		body.put("renderer", v);
		return this;
	}

	/** @param v An ECharts theme name registered in the page; the default is the page theme. @return This object. */
	public ChartDef theme(String v) { body.put("theme", v); return this; }

	/** @param dataTopic A {@code data:} topic ({@link Topics#data(String)}). @return This object. */
	public ChartDef source(String dataTopic) { source = Markers.dataTopicOf("ChartDef '" + id + "'", dataTopic); return this; }

	/** @param field The heatmap y (category) field. @return This object. */
	public ChartDef yField(String field) {
		only("heatmap", "yField");
		body.put("y", JsonMap.of("field", field));
		return this;
	}

	/** @param min The heatmap colour-scale minimum. @param max Its maximum. @return This object. */
	public ChartDef scale(Number min, Number max) {
		only("heatmap", "scale");
		body.put("scale", JsonMap.of("min", min, "max", max));
		return this;
	}

	/** @param field The scatter field sized by square root. @return This object. */
	public ChartDef symbolSizeBy(String field) {
		only("scatter", "symbolSizeBy");
		body.put("symbolSize", Fn.ref("symbolSizeBy", JsonMap.of("field", field)));
		return this;
	}

	/** @param threshold Rows above which the large-data defaults apply. @return This object. */
	public ChartDef largeData(int threshold) { return largeData(threshold, false); }

	/** @param threshold Rows above which the large-data defaults apply. @param progressive {@code true} sets progressive to the threshold; {@code false} leaves the JS default (5000). @return This object. */
	public ChartDef largeData(int threshold, boolean progressive) {
		if (threshold < 1)
			throw iaex("ChartDef '%s' largeData threshold '%s' must be at least 1.", id, threshold);
		var m = JsonMap.of("threshold", threshold);
		if (progressive)
			m.put("progressive", threshold);
		body.put("largeData", m);
		return this;
	}

	/** Draws partial frames during a chunked {@code source} load. @return This object. */
	public ChartDef partialFrames() { body.put("partialFrames", true); return this; }

	/** @param summary The figure caption, or <jk>null</jk> for the generated one. @param table Show the data-table toggle. @return This object. */
	public ChartDef a11y(String summary, boolean table) {
		var m = new JsonMap();
		if (nn(summary)) m.put("summary", summary);
		m.put("table", table);
		body.put("a11y", m);
		return this;
	}

	/** @param name An ECharts map name. @param path A same-origin path to its GeoJSON. @return This object. */
	public ChartDef map(String name, String path) {
		if (! SafePathTemplate.isSameOriginPath(path))
			throw iaex("ChartDef '%s' map '%s' must be a same-origin path; got '%s'.", id, name, path);
		maps.put(name, path);
		return this;
	}

	// raw(...) has no kind; Set.of(...).contains(null) throws.
	private boolean legacy() {
		return kind != null && LEGACY_KINDS.contains(kind);
	}

	private void only(String k, String method) {
		if (! eq(kind, k))
			throw iaex("ChartDef '%s' %s(...) is for kind '%s' only.", id, method, k);
	}

	// One entry per topic: the first wins, except that a later retain=true upgrades it (zoom:/legend: need it).
	private void addPublish(String topic, boolean retain) {
		TopicDecl.of(topic);
		publishes.merge(topic, retain, Boolean::logicalOr);
	}

	private JsonMap parsed(Object v, String what) {
		if (v instanceof JsonMap m)
			return m;
		try {
			return Json5Parser.DEFAULT.read(((String)v).trim(), JsonMap.class);
		} catch (org.apache.juneau.marshall.parser.ParseException e) {
			throw iaex("ChartDef '%s' %s is not valid JSON5: %s", id, what, e.getMessage());
		}
	}

	/**
	 * Deep merge: objects by key; arrays element-by-element by {@code id} (a match merges, no match
	 * appends); an array whose elements carry no {@code id} replaces the whole array.  {@code into} is changed in place.
	 */
	@SuppressWarnings("unchecked")
	static void merge(Map<String,Object> into, Map<?,?> from) {
		for (var e : from.entrySet()) {
			var k = String.valueOf(e.getKey());
			var cur = into.get(k);
			var v = e.getValue();
			if (cur instanceof Map && v instanceof Map) {
				var copy = new JsonMap((Map<String,Object>)cur);
				merge(copy, (Map<?,?>)v);
				into.put(k, copy);
			} else if (cur instanceof List<?> a && v instanceof List<?> b && hasIds(b)) {
				var out = new ArrayList<Object>(a);
				for (var item : b) {
					var at = indexOfId(out, ((Map<?,?>)item).get("id"));
					if (at < 0) {
						out.add(item);
					} else {
						var copy = new JsonMap((Map<String,Object>)out.get(at));
						merge(copy, (Map<?,?>)item);
						out.set(at, copy);
					}
				}
				into.put(k, out);
			} else {
				into.put(k, v);
			}
		}
	}

	private static boolean hasIds(List<?> l) {
		return ! l.isEmpty() && l.stream().allMatch(x -> x instanceof Map<?,?> m && m.get("id") != null);
	}

	private static int indexOfId(List<Object> l, Object id) {
		for (var i = 0; i < l.size(); i++)
			if (l.get(i) instanceof Map<?,?> m && Objects.equals(m.get("id"), id))
				return i;
		return -1;
	}

	/**
	 * Sets a body key that has no dedicated method (for example an ECharts-level card key the glue reads).
	 * For a helper kind, a helper key set here is routed under {@code helper}.
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
	 * @throws IllegalArgumentException If not exactly one of {@code src}/{@code data}/{@code source} is set, a kind's
	 * 	required keys are missing, {@code raw} has no option, or an option/patch string is not valid JSON5.
	 */
	public CardSpec toCardSpec() {
		if ((nn(src) ? 1 : 0) + (nn(data) ? 1 : 0) + (nn(source) ? 1 : 0) != 1)
			throw iaex("ChartDef '%s' requires exactly one of src(...), data(...) or source(...).", id);
		if (! nn(src) && body.containsKey("refreshMs"))
			throw iaex("ChartDef '%s' refreshMs(...) needs src(...); inline data never refreshes.", id);
		if (body.containsKey("partialFrames") && ! nn(source))
			throw iaex("ChartDef '%s' partialFrames() needs source(...).", id);
		if (kind != null && Set.of("line", "bar", "scatter").contains(kind) && (! body.containsKey("x") || series.isEmpty()))
			throw iaex("ChartDef '%s' kind '%s' requires x(...) and at least one series(...).", id, kind);
		if (kind != null && Set.of("donut", "pie").contains(kind) && (! (body.get("label") instanceof String l) || l.isBlank() || ! (body.get("value") instanceof String v) || v.isBlank()))
			throw iaex("ChartDef '%s' kind '%s' requires label(...) and value(...).", id, kind);
		if (eq(kind, "treemap") && ! body.containsKey("groupBy"))
			throw iaex("ChartDef '%s' kind 'treemap' requires groupBy(...).", id);
		if (eq(kind, "treemap") && (! (body.get("label") instanceof String tl) || tl.isBlank()))
			throw iaex("ChartDef '%s' kind 'treemap' requires label(field).", id);
		if (eq(kind, "heatmap") && (! body.containsKey("x") || ! body.containsKey("y") || ! (body.get("value") instanceof String)))
			throw iaex("ChartDef '%s' kind 'heatmap' requires x(...), yField(...) and value(...).", id);
		if (kind == null && option == null)
			throw iaex("ChartDef '%s' raw(...) requires option(...).", id);
		if (kind == null && (! series.isEmpty() || body.keySet().stream().anyMatch(HELPER_KEYS::contains)))
			throw iaex("ChartDef '%s' raw(...) takes no helper keys (x, y, series, ...); put them in option(...).", id);
		var c = CardSpec.of("chart", id);
		if (legacy())
			c.body("kind", kind);
		if (nn(title)) c.title(title);
		if (nn(src)) c.src(src);
		if (nn(source)) {
			c.body("source", source);
			c.subscribes(Subscription.to(source).as("data"));
		}
		if (legacy()) {
			body.forEach(c::body);
			if (! series.isEmpty()) c.body("series", List.copyOf(series));
		} else {
			var helper = kind == null ? null : JsonMap.of("kind", kind);
			body.forEach((k, v) -> { if (helper != null && HELPER_KEYS.contains(k)) helper.put(k, v); else if (! HELPER_KEYS.contains(k)) c.body(k, v); });
			if (helper != null && ! series.isEmpty()) helper.put("series", List.copyOf(series));
			if (helper != null) c.body("helper", helper);
		}
		if (nn(data)) c.body("data", List.copyOf(data));
		if (nn(option)) c.body("option", parsed(option, "option"));
		if (! patches.isEmpty()) {
			var p = new JsonMap();
			for (var x : patches)
				merge(p, parsed(x, "patch"));
			c.body("patch", p);
		}
		if (! decorators.isEmpty()) c.body("decorators", List.copyOf(decorators));
		if (! on.isEmpty()) c.body("on", new JsonMap(on));
		if (! topicActions.isEmpty()) c.body("topicActions", List.copyOf(topicActions));
		if (! catalog.isEmpty()) c.body("actions", List.copyOf(catalog));
		if (! maps.isEmpty()) c.body("maps", new JsonMap(maps));
		if (! publishes.isEmpty()) c.publishes(publishes.entrySet().stream().map(e -> TopicDecl.of(e.getKey()).retain(e.getValue())).toArray(TopicDecl[]::new));
		if (nn(visibleWhen)) c.visibleWhen(visibleWhen);
		return c;
	}
}
