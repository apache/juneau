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
import org.apache.juneau.rest.server.console.*;

/**
 * The built-in {@code chart} card type: a JSON5 body naming a {@code helper}, a legacy flat {@code kind}, or a raw
 * ECharts {@code option}, with data from exactly one of {@code src=}, an inline {@code data} array or a {@code source}
 * ({@code data:} topic).  {@code juneau-charts.js} renders it with ECharts.  The kinds are fixed: {@code line},
 * {@code bar}, {@code donut}, {@code heatmap}, {@code scatter}, {@code pie} and {@code treemap}.
 *
 * <p>
 * The one bus role is {@code data} (a source card's {@code data:<key>} subscription), and the ops are the eight chart
 * commands ({@code highlight}, {@code downplay}, {@code showTip}, {@code hideTip}, {@code zoomTo},
 * {@code legendSelect}, {@code export} and {@code refresh}), so Java's E-44 agrees with the client's E-JS-47.
 * The checks:
 * <ul>
 * 	<li>{@code helper} (an object with a kind) and a legacy {@code kind} are mutually exclusive, and one of
 * 		{@code helper}, {@code kind} or {@code option} is required;
 * 	<li>{@code option} and {@code patch} are JSON objects, and {@code decorators}, {@code on} and every {@code $fn}
 * 		marker name must be valid extension names (E-74);
 * 	<li>{@code renderer} is {@code canvas} or {@code svg}, {@code maps} values are same-origin paths and
 * 		{@code partialFrames} needs a {@code source};
 * 	<li>every line/bar/scatter series needs a field, a donut or pie needs string {@code label} and {@code value}
 * 		fields, a heatmap needs {@code x}, {@code y} and a string {@code value}, a treemap needs {@code groupBy} and a string
 * 		{@code label} (with an optional string {@code size}, a {@code color} key and {@code tileFields});
 * 	<li>{@code height} must be at least {@link ChartDef#MIN_HEIGHT}, and {@code onClick.href} must be a same-origin
 * 		path ({@link SafePathTemplate#isSameOriginPath(String)}), the same checks {@link ChartDef} makes;
 * 	<li>{@code onClick} holds an {@code href} or an {@code action}, never both.  The {@code action} (also each
 * 		{@code topicActions[]} entry, whose topic must be one {@code on} publishes, and each {@code actions[]} catalog
 * 		entry, which must be {@code present: "dialog"}) needs a unique id, a same-origin {@code endpoint} (and
 * 		{@code form}), a POST/PUT/PATCH/DELETE {@code method}, a string or {@code false} {@code confirm}, no
 * 		{@code present} but {@code dialog} and no {@code onSuccess: "navigate"}.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@card type="chart" id="orders-trend" title="Orders" src="/console/store/stats/daily"&gt;
 * 	{ kind: 'line', x: { field: 'day' }, series: [ { field: 'placed', label: 'Placed' } ] }
 * 	&lt;/@card&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class ChartCardType implements CardTypeHandler {

	// The non-safe methods only: a safe method skips the server's Origin and CSRF checks.
	private static final Set<String> ACTION_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
	private static final Set<String> CARTESIAN = Set.of("line", "bar", "scatter");
	private static final Set<String> OPS = Set.of("highlight", "downplay", "showTip", "hideTip", "zoomTo", "legendSelect", "export", "refresh");

	/** Public no-arg constructor, required by {@link java.util.ServiceLoader}. */
	public ChartCardType() {}

	@Override
	public String type() {
		return "chart";
	}

	// The role of a source card's data:<key> subscription (ChartDef.source adds it; the glue's roles.data reads it).
	@Override
	public Set<String> acceptedRoles() {
		return Set.of("data");
	}

	@Override
	public Set<String> acceptedOps() {
		return OPS;
	}

	// The card's own retained zoom:/legend: topics, when it publishes them (juneau-charts.js's own implicit() mirrors this; ChartWiring_JsParity_Test pins them).
	@Override
	public List<String> implicitTopics(JsonMap card) {
		var id = card.get("id");
		var out = new ArrayList<String>();
		if (! (id instanceof String cardId) || ! (card.get("publishes") instanceof List<?> pubs))
			return out;
		for (var t : List.of(Topics.zoom(cardId), Topics.legend(cardId)))
			for (var p : pubs)
				if (p instanceof Map<?,?> m && t.equals(m.get("topic")) && Boolean.TRUE.equals(m.get("retain"))) {
					out.add(t);
					break;
				}
		return out;
	}

	@Override
	public JsonMap toFragment(CardSource source) {
		if (! source.hasJsonBody())
			throw source.error("type='chart' requires a JSON5 body.");
		var body = source.json();
		var hasSrc = nn(source.src());
		var hasData = body.containsKey("data");
		var hasSource = body.containsKey("source");
		if ((hasSrc ? 1 : 0) + (hasData ? 1 : 0) + (hasSource ? 1 : 0) != 1)
			throw source.error("type='chart' requires exactly one of src=, a data body or a source.");
		if (hasSrc && ! SafePathTemplate.isSameOriginPath(source.src()))
			throw source.error("type='chart' src must be a same-origin path; got '%s'.", source.src());
		if (hasData && ! (body.get("data") instanceof List))
			throw source.error("type='chart' data must be an array of rows.");
		if (hasSource && ! (body.get("source") instanceof String s && s.startsWith("data:") && s.length() > 5))
			throw source.error("type='chart' source must be a data: topic; got '%s'.", body.get("source"));
		if (body.containsKey("helper") && body.containsKey("kind"))
			throw source.error("type='chart' takes 'helper' or a legacy 'kind', not both.");
		if (! body.containsKey("helper") && ! body.containsKey("kind") && ! body.containsKey("option"))
			throw source.error("type='chart' requires 'helper', a legacy 'kind' or an 'option'.");
		if (body.containsKey("helper") && ! (body.get("helper") instanceof Map))
			throw source.error("type='chart' helper must be a JSON object.");
		Map<?,?> helper = body.get("helper") instanceof Map<?,?> h ? h : body.containsKey("kind") ? body : null;
		if (helper != null)
			checkHelper(source, helper);
		for (var k : List.of("option", "patch"))
			if (body.containsKey(k) && ! (body.get(k) instanceof Map))
				throw source.error("type='chart' %s must be a JSON object.", k);
		if (body.containsKey("decorators")) {
			if (! (body.get("decorators") instanceof List<?> l) || ! l.stream().allMatch(d -> d instanceof Map<?,?> m && m.get("use") instanceof String))
				throw source.error("type='chart' decorators must be an array of objects with a string 'use'.");
			for (var d : (List<?>)body.get("decorators"))
				checkName(source, (String)((Map<?,?>)d).get("use"), "decorator");
		}
		if (body.containsKey("on")) {
			if (! (body.get("on") instanceof Map<?,?> on) || ! on.values().stream().allMatch(String.class::isInstance))
				throw source.error("type='chart' on must map event names to topic strings.");
			for (var ev : on.keySet())
				if (! BusWiringValidator.EVENT_NAME.matcher(String.valueOf(ev)).matches())
					throw source.error("%s", "E-74: " + f(BusWiringValidator.E74, ev, "event"));
		}
		var renderer = body.get("renderer");
		if (nn(renderer) && ! Set.of("canvas", "svg").contains(renderer))
			throw source.error("type='chart' renderer must be 'canvas' or 'svg'; got '%s'.", renderer);
		if (body.get("maps") instanceof Map<?,?> maps) {
			for (var e : maps.entrySet())
				if (! (e.getValue() instanceof String p) || ! SafePathTemplate.isSameOriginPath(p))
					throw source.error("type='chart' maps.%s must be a same-origin path; got '%s'.", e.getKey(), e.getValue());
		}
		if (body.containsKey("maps") && ! (body.get("maps") instanceof Map))
			throw source.error("type='chart' maps must be a JSON object.");
		if (body.containsKey("partialFrames") && ! (body.get("partialFrames") instanceof Boolean))
			throw source.error("type='chart' partialFrames must be a boolean.");
		for (var k : List.of("group", "theme"))
			if (body.containsKey(k) && ! (body.get(k) instanceof String))
				throw source.error("type='chart' %s must be a string.", k);
		for (var k : List.of("largeData", "a11y"))
			if (body.containsKey(k) && ! (body.get(k) instanceof Map))
				throw source.error("type='chart' %s must be a JSON object.", k);
		if (Boolean.TRUE.equals(body.get("partialFrames")) && ! hasSource)
			throw source.error("type='chart' partialFrames needs a source.");
		walkFns(source, body.get("option"));
		walkFns(source, body.get("patch"));
		walkFns(source, body.get("decorators"));
		// helper keys are mostly field names, but a helper value can be a $fn marker (ChartDef.symbolSizeBy puts Fn.ref under
		// helper.symbolSize), so the helper -- or, for a legacy flat body, its HELPER_KEYS -- is walked with the same skip set.
		if (helper != null) {
			var hm = new JsonMap();
			for (var e : helper.entrySet())
				if (helper != body || ChartDef.HELPER_KEYS.contains(e.getKey()))
					hm.put(String.valueOf(e.getKey()), e.getValue());
			walkFns(source, hm);
		}
		var refresh = body.get("refreshMs");
		if (nn(refresh)) {
			if (! (refresh instanceof Integer || refresh instanceof Long))
				throw source.error("type='chart' refreshMs must be an integer; got '%s'.", refresh);
			if (! hasSrc)
				throw source.error("type='chart' refreshMs needs src=; inline data never refreshes.");
			if (((Number)refresh).longValue() < SafePathTemplate.MIN_POLL_INTERVAL_MS)
				throw source.error("type='chart' refreshMs='%s' is below the minimum '%s'.", refresh, SafePathTemplate.MIN_POLL_INTERVAL_MS);
		}
		var height = body.get("height");
		if (nn(height)) {
			if (! (height instanceof Integer || height instanceof Long))
				throw source.error("type='chart' height must be an integer; got '%s'.", height);
			if (((Number)height).longValue() < ChartDef.MIN_HEIGHT)
				throw source.error("type='chart' height='%s' is below the minimum '%s'.", height, ChartDef.MIN_HEIGHT);
		}
		var onClick = body.get("onClick");
		var ids = new HashSet<String>();
		if (nn(onClick))
			checkOnClick(source, body, onClick, ids);
		if (nn(body.get("topicActions")))
			checkTopicActions(source, body, ids);
		if (nn(body.get("actions")))
			checkCatalog(source, body.get("actions"), ids);
		var frag = new JsonMap();
		frag.putAll(body);
		frag.remove("visibleWhen");
		return frag;
	}

	// The helper (or a legacy flat body): the kind set, plus the legacy per-kind checks.
	private static void checkHelper(CardSource source, Map<?,?> h) {
		if (! (h.get("kind") instanceof String kind) || kind.isBlank())
			throw source.error("type='chart' requires a non-blank 'kind'.");
		if (! ChartDef.KINDS.contains(kind))
			throw source.error("type='chart' kind '%s' must be one of '%s'.", kind, String.join(", ", new TreeSet<>(ChartDef.KINDS)));
		if (CARTESIAN.contains(kind) && (h.get("x") == null || ! (h.get("series") instanceof List<?> s) || s.isEmpty()))
			throw source.error("type='chart' kind='%s' requires x and at least one series.", kind);
		if (CARTESIAN.contains(kind)) {
			var series = (List<?>)h.get("series");
			for (var i = 0; i < series.size(); i++)
				if (ib(seriesField(series.get(i))))
					throw source.error("type='chart' kind='%s' series[%s] requires a non-blank 'field'.", kind, i);
		}
		if (Set.of("donut", "pie").contains(kind) && (! (h.get("label") instanceof String l) || l.isBlank() || ! (h.get("value") instanceof String v) || v.isBlank()))
			throw source.error("type='chart' kind='%s' requires string 'label' and 'value' fields.", kind);
		if (eq(kind, "heatmap") && (h.get("x") == null || h.get("y") == null || ! (h.get("value") instanceof String)))
			throw source.error("type='chart' kind='heatmap' requires x, y and a string 'value' field.");
		if (eq(kind, "treemap"))
			checkTreemap(source, h);
	}

	private static void checkTreemap(CardSource source, Map<?,?> h) {
		if (! (h.get("groupBy") instanceof List<?> g) || g.isEmpty() || ! g.stream().allMatch(x -> x instanceof String s && ! s.isBlank()))
			throw source.error("type='chart' helper kind 'treemap' requires 'groupBy' as a non-empty array of non-blank strings.");
		if (! (h.get("label") instanceof String l) || l.isBlank())
			throw source.error("type='chart' helper kind 'treemap' requires a non-blank string 'label'.");
		if (h.containsKey("size") && ! (h.get("size") instanceof String sz && ! sz.isBlank()))
			throw source.error("type='chart' helper kind 'treemap' size must be a non-blank string.");
		if (h.containsKey("tileFields") && ! (h.get("tileFields") instanceof List<?> t && t.stream().allMatch(x -> x instanceof String s && ChartDef.TILE_FIELD.matcher(s).matches())))
			throw source.error("type='chart' helper kind 'treemap' tileFields must be an array of strings matching [A-Za-z0-9_.-]+.");
		if (h.containsKey("color")) {
			if (! (h.get("color") instanceof Map<?,?> c) || ! (c.get("field") instanceof String f) || f.isBlank())
				throw source.error("type='chart' helper kind 'treemap' color must be an object with a non-blank string 'field'.");
			var legend = ((Map<?,?>)h.get("color")).get("legend");
			if (nn(legend)) {
				if (! (legend instanceof List<?> ll) || ! ll.stream().allMatch(x -> x instanceof Map<?,?> m && m.get("value") instanceof String))
					throw source.error("type='chart' helper kind 'treemap' color.legend must be an array of objects with a string 'value'.");
				for (var i = 0; i < ((List<?>)legend).size(); i++) {
					var tone = ((Map<?,?>)((List<?>)legend).get(i)).get("tone");
					if (nn(tone) && ! (tone instanceof String ts && ChartDef.TREEMAP_TONES.contains(ts)))
						throw source.error("type='chart' helper kind 'treemap' color.legend[%s].tone '%s' must be 'ok', 'warning', 'danger', '--jc-chart-1'..'--jc-chart-8' or absent.", i, tone);
				}
			}
		}
	}

	private static void checkOnClick(CardSource source, Map<?,?> body, Object onClick, Set<String> ids) {
		if (! (onClick instanceof Map<?,?> m))
			throw source.error("type='chart' onClick must be an object with a string href.");
		for (var k : m.keySet())
			if (! "href".equals(k) && ! "action".equals(k))
				throw source.error("type='chart' onClick has unknown key '%s'.", k);
		if (m.containsKey("href") && m.containsKey("action"))
			throw source.error("type='chart' onClick takes either href or action, not both.");
		if (m.containsKey("action")) {
			if (! (body.get("on") instanceof Map<?,?> on) || ! (on.get("click") instanceof String))
				throw source.error("type='chart' onClick.action needs on.click to declare the click topic.");
			checkAction(source, "onClick.action", m.get("action"), ids);
		} else if (! (m.get("href") instanceof String href)) {
			throw source.error("type='chart' onClick must be an object with a string href.");
		} else if (! SafePathTemplate.isSameOriginPath(href)) {
			throw source.error("type='chart' onClick.href must be a same-origin path; got '%s'.", href);
		}
	}

	private static void checkTopicActions(CardSource source, Map<?,?> body, Set<String> ids) {
		var shape = "type='chart' topicActions must be an array of objects with a string topic and an action object.";
		if (! (body.get("topicActions") instanceof List<?> l))
			throw source.error("%s", shape);
		var topics = body.get("on") instanceof Map<?,?> on ? on.values() : List.of();
		for (var i = 0; i < l.size(); i++) {
			if (! (l.get(i) instanceof Map<?,?> e) || ! (e.get("topic") instanceof String topic) || ! (e.get("action") instanceof Map))
				throw source.error("%s", shape);
			if (! topics.contains(topic))
				throw source.error("type='chart' topicActions[%s].topic '%s' must be one this card publishes through 'on'.", i, topic);
			checkAction(source, "topicActions[" + i + "].action", e.get("action"), ids);
		}
	}

	private static void checkCatalog(CardSource source, Object actions, Set<String> ids) {
		if (! (actions instanceof List<?> l))
			throw source.error("type='chart' actions must be an array of action objects.");
		for (var i = 0; i < l.size(); i++) {
			var where = "actions[" + i + "]";
			checkAction(source, where, l.get(i), ids);
			var a = (Map<?,?>)l.get(i);
			if (! "dialog".equals(a.get("present")))
				throw source.error("type='chart' %s '%s' must be present=dialog; a dialog button can only open a dialog action.", where, a.get("id"));
		}
	}

	// The rules every chart action obeys, wherever it is declared: a chart has no cell, page or servlet to hand off to.
	private static void checkAction(CardSource source, String where, Object action, Set<String> ids) {
		if (! (action instanceof Map<?,?> a))
			throw source.error("type='chart' %s must be an object.", where);
		if (! (a.get("id") instanceof String id) || id.isBlank())
			throw source.error("type='chart' %s requires a non-blank string id.", where);
		if (! (a.get("endpoint") instanceof String endpoint) || endpoint.isBlank())
			throw source.error("type='chart' %s requires a non-blank string endpoint.", where);
		if (! SafePathTemplate.isSameOriginPath(endpoint))
			throw source.error("type='chart' %s endpoint must be a same-origin path; got '%s'.", where, endpoint);
		if (nn(a.get("form")) && ! (a.get("form") instanceof String form && SafePathTemplate.isSameOriginPath(form)))
			throw source.error("type='chart' %s form must be a same-origin path; got '%s'.", where, a.get("form"));
		if (! (a.get("method") instanceof String method) || ! ACTION_METHODS.contains(method))
			throw source.error("type='chart' %s method must be POST, PUT, PATCH or DELETE; got '%s'.", where, a.get("method"));
		if (nn(a.get("confirm")) && ! (a.get("confirm") instanceof String || Boolean.FALSE.equals(a.get("confirm"))))
			throw source.error("type='chart' %s confirm must be a string or false.", where);
		if (nn(a.get("present")) && ! "dialog".equals(a.get("present")))
			throw source.error("type='chart' %s present must be 'dialog' or absent; got '%s'.", where, a.get("present"));
		if (nn(a.get("visibleWhen")) && ! (a.get("visibleWhen") instanceof List))
			throw source.error("type='chart' %s visibleWhen must be an array of rules; it is tested against the clicked point.", where);
		if (nn(a.get("enabledWhen")) && ! (a.get("enabledWhen") instanceof List))
			throw source.error("type='chart' %s enabledWhen must be an array of rules.", where);
		if ("navigate".equals(a.get("onSuccess")))
			throw source.error("type='chart' %s onSuccess must not be 'navigate'; a chart has nowhere to navigate.", where);
		if (! ids.add(id))
			throw source.error("type='chart' action id '%s' is declared more than once.", id);
	}

	private static void checkName(CardSource source, String name, String what) {
		if (! Fn.validName(name))
			throw source.error("%s", "E-74: " + f(BusWiringValidator.E74, name, what));
	}

	// $fn markers: an object whose only keys are $fn and, optionally, args.  Same skip set as
	// BusWiringValidator.walkFns: dataset[*].source and series[*].data are rows of data, never extension references.
	private static void walkFns(CardSource source, Object o) {
		if (o instanceof Map<?,?> m) {
			if (m.containsKey("$fn") && m.keySet().stream().allMatch(k -> "$fn".equals(k) || "args".equals(k)))
				checkName(source, m.get("$fn") instanceof String s ? s : null, "$fn");
			for (var e : m.entrySet()) {
				if ("dataset".equals(e.getKey()))
					walkSkipping(source, e.getValue(), "source");
				else if ("series".equals(e.getKey()))
					walkSkipping(source, e.getValue(), "data");
				else
					walkFns(source, e.getValue());
			}
		} else if (o instanceof List<?> l) {
			l.forEach(v -> walkFns(source, v));
		}
	}

	// Walks o (a map or a list of maps) without descending into the skip key of each map.
	private static void walkSkipping(CardSource source, Object o, String skip) {
		if (o instanceof List<?> l) {
			l.forEach(x -> walkSkipping(source, x, skip));
		} else if (o instanceof Map<?,?> m) {
			for (var e : m.entrySet())
				if (! skip.equals(e.getKey()))
					walkFns(source, e.getValue());
		}
	}

	// A series entry is a field name or an object with a field, as juneau-charts.js reads it.
	private static String seriesField(Object entry) {
		if (entry instanceof String s)
			return s;
		return entry instanceof Map<?,?> m && m.get("field") instanceof String s ? s : null;
	}
}
