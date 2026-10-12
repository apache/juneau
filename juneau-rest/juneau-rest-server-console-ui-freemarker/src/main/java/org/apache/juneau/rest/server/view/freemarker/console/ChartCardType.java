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
 * The built-in {@code chart} card type: a JSON5 body naming a {@code kind} plus that kind's keys, with data from
 * {@code src=} or an inline {@code data} array (exactly one).  {@code juneau-charts.js} renders it; the built-in kinds
 * are {@code line}, {@code bar} and {@code donut}, and apps register more in the browser, so only the built-in kinds'
 * keys are checked here.
 *
 * <p>
 * The card takes no bus roles beyond the shell's {@code params} and {@code refresh}, and no ops: the browser handler
 * has no {@code roles}, so {@link #acceptedRoles()} is empty and Java's E-44 agrees with the client's E-JS-47.
 * {@code kind} must match {@code ^[a-z][a-z0-9-]{0,31}$}, every line/bar series needs a field, a donut needs string
 * {@code label} and {@code value} fields, {@code height} must be at least {@link ChartDef#MIN_HEIGHT}, and
 * {@code onClick.href} must be a same-origin path ({@link SafePathTemplate#isSameOriginPath(String)}), the same checks
 * {@link ChartDef} makes.
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

	private static final Set<String> CARTESIAN = Set.of("line", "bar");

	/** Public no-arg constructor, required by {@link java.util.ServiceLoader}. */
	public ChartCardType() {}

	@Override
	public String type() {
		return "chart";
	}

	@Override
	public Set<String> acceptedRoles() {
		return Set.of();
	}

	@Override
	public Set<String> acceptedOps() {
		return Set.of();
	}

	@Override
	public JsonMap toFragment(CardSource source) {
		if (! source.hasJsonBody())
			throw source.error("type='chart' requires a JSON5 body with a 'kind'.");
		var body = source.json();
		if (! (body.get("kind") instanceof String kind) || kind.isBlank())
			throw source.error("type='chart' requires a non-blank 'kind'.");
		if (! ChartDef.KIND.matcher(kind).matches())
			throw source.error("type='chart' kind must match '%s'; got '%s'.", ChartDef.KIND.pattern(), kind);
		var hasSrc = nn(source.src());
		var hasData = body.containsKey("data");
		if (hasSrc == hasData)
			throw source.error("type='chart' requires exactly one of src= or a data body.");
		if (hasData && ! (body.get("data") instanceof List))
			throw source.error("type='chart' data must be an array of rows.");
		if (CARTESIAN.contains(kind)
				&& (body.get("x") == null || ! (body.get("series") instanceof List<?> s) || s.isEmpty()))
			throw source.error("type='chart' kind='%s' requires x and at least one series.", kind);
		if (CARTESIAN.contains(kind)) {
			var series = (List<?>)body.get("series");
			for (var i = 0; i < series.size(); i++)
				if (ib(seriesField(series.get(i))))
					throw source.error("type='chart' kind='%s' series[%s] requires a non-blank 'field'.", kind, i);
		}
		if (eq(kind, "donut") && (! (body.get("label") instanceof String l) || l.isBlank() || ! (body.get("value") instanceof String v) || v.isBlank()))
			throw source.error("type='chart' kind='donut' requires string 'label' and 'value' fields.");
		var refresh = body.get("refreshMs");
		if (nn(refresh)) {
			if (! (refresh instanceof Integer || refresh instanceof Long))
				throw source.error("type='chart' refreshMs must be an integer; got '%s'.", refresh);
			if (hasData)
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
		if (nn(onClick)) {
			if (! (onClick instanceof Map<?,?> m) || ! (m.get("href") instanceof String href))
				throw source.error("type='chart' onClick must be an object with a string href.");
			if (! SafePathTemplate.isSameOriginPath(href))
				throw source.error("type='chart' onClick.href must be a same-origin path; got '%s'.", href);
		}
		var frag = new JsonMap();
		frag.putAll(body);
		frag.remove("visibleWhen");
		return frag;
	}

	// A series entry is a field name or an object with a field, as juneau-charts.js reads it.
	private static String seriesField(Object entry) {
		if (entry instanceof String s)
			return s;
		return entry instanceof Map<?,?> m && m.get("field") instanceof String s ? s : null;
	}
}
