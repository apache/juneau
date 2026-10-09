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

import java.io.*;
import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.rest.server.views.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@badge>} FreeMarker twin of {@link PageSpec#badge(BadgeDef)}: every scalar or enum {@link BadgeDef}
 * field is a plain attribute (the label templates as {@code labelOne}, {@code labelOther} and {@code labelMine}, the
 * scope as {@code scopeParam} and {@code scopeBy}, the state filter as {@code stateFilterColumn}).  The three
 * structured fields have no flat shape, so an optional JSON5 body supplies them under the keys
 * {@code visibleWhen} (a rule list), {@code scopeValues} (a map of nav node id to value list) and {@code params}
 * (a map).  Any other body key is rejected.
 *
 * <p>
 * The FTL scope is stricter than {@link BadgeDef#scope}: {@code scopeParam} and {@code scopeBy} must be given
 * together, and {@code scopeBy} must be spelled {@code nav} or {@code view}, where the Java form defaults a missing
 * {@code by} to {@code nav}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@badge id="health" src="/rest/health" refreshMs=30000 tone="warning"&gt;
 * 	  {"visibleWhen": [{"field": "status", "op": "eq", "value": "degraded"}]}
 * 	&lt;/@badge&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class BadgeDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "badge";

	static final Set<String> ATTRS = Set.of("id", "src", "refreshMs", "labelOne", "labelOther", "labelMine", "tooltipMax",
		"href", "refreshes", "placement", "table", "tone", "scopeParam", "scopeBy", "stateFilterColumn");

	private static final Set<String> BODY_KEYS = Set.of("visibleWhen", "scopeValues", "params");

	BadgeDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@badge> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var cap = PageCapture.get(env);
		if (cap == null || ! cap.consoleOpen)
			throw FtlAttrLists.reject("<@badge> must be nested inside <@console>.");

		var id = FtlAttrLists.scalar(p, "id");
		if (id.isEmpty())
			throw FtlAttrLists.reject("<@badge> requires id=.");
		var src = FtlAttrLists.scalar(p, "src");
		if (src.isEmpty())
			throw FtlAttrLists.reject("<@badge id='" + id + "'> requires src=.");

		var def = BadgeDef.create(id).src(src);
		try {
			var refreshMs = FtlAttrLists.scalar(p, "refreshMs");
			if (! refreshMs.isEmpty())
				def.refreshMs(Long.parseLong(refreshMs));
			var tooltipMax = FtlAttrLists.scalar(p, "tooltipMax");
			if (! tooltipMax.isEmpty())
				def.tooltipMax(Integer.parseInt(tooltipMax));
		} catch (NumberFormatException e) {
			throw FtlAttrLists.reject("<@badge id='" + id + "'> refreshMs= and tooltipMax= must be integers.");
		}
		var label = new LinkedHashMap<String,String>();
		for (var key : List.of("One", "Other", "Mine")) {
			var v = FtlAttrLists.scalar(p, "label" + key);
			if (! v.isEmpty())
				label.put(key.toLowerCase(), v);
		}
		if (! label.isEmpty())
			def.label(label);
		var href = FtlAttrLists.scalar(p, "href");
		if (! href.isEmpty())
			def.href(href);
		var refreshes = FtlAttrLists.list(p, NAME, "refreshes");
		if (! refreshes.isEmpty())
			def.refreshes(refreshes.toArray(new String[0]));
		var table = FtlAttrLists.scalar(p, "table");
		if (! table.isEmpty())
			def.table(table);
		var stateFilterColumn = FtlAttrLists.scalar(p, "stateFilterColumn");
		if (! stateFilterColumn.isEmpty())
			def.stateFilter(stateFilterColumn);
		var placement = FtlAttrLists.scalar(p, "placement");
		if (! placement.isEmpty())
			def.placement(enumOf(id, "placement", BadgeDef.Placement.values(), BadgeDef.Placement::wire, placement));
		var tone = FtlAttrLists.scalar(p, "tone");
		if (! tone.isEmpty())
			def.tone(enumOf(id, "tone", BadgeDef.Tone.values(), BadgeDef.Tone::wire, tone));

		var scopeParam = FtlAttrLists.scalar(p, "scopeParam");
		var scopeBy = FtlAttrLists.scalar(p, "scopeBy");
		if (scopeParam.isEmpty() != scopeBy.isEmpty())
			throw FtlAttrLists.reject("<@badge id='" + id + "'> scope requires both scopeParam= and scopeBy=.");

		var markup = PageCapture.render(body);
		Map<String,List<String>> scopeValues = Map.of();
		if (! markup.isBlank()) {
			JsonMap bodyMap;
			try {
				bodyMap = Json5Parser.DEFAULT.read(markup.trim(), JsonMap.class);
			} catch (org.apache.juneau.marshall.parser.ParseException e) {
				throw FtlAttrLists.reject("<@badge id='" + id + "'> body is not valid JSON5: " + e.getMessage());
			}
			for (var key : bodyMap.keySet())
				if (! BODY_KEYS.contains(key))
					throw FtlAttrLists.reject(f(
						"<@badge id='%s'> body only supports 'visibleWhen', 'scopeValues' and 'params' keys; found '%s'.", id, key));
			if (bodyMap.get("visibleWhen") != null)
				def.visibleWhen(parseVisibleWhen(id, bodyMap.get("visibleWhen")).toArray(new VisibilityRule[0]));
			if (bodyMap.get("scopeValues") != null)
				scopeValues = parseScopeValues(id, bodyMap.get("scopeValues"));
			if (bodyMap.get("params") != null) {
				if (! (bodyMap.get("params") instanceof Map<?,?> pm))
					throw FtlAttrLists.reject("<@badge id='" + id + "'> params must be a JSON5 object.");
				var out = new LinkedHashMap<String,Object>();
				pm.forEach((k, v) -> out.put(String.valueOf(k), v));
				def.params(out);
			}
		}
		if (! scopeParam.isEmpty())
			def.scope(scopeParam, enumOf(id, "scopeBy", BadgeDef.ScopeBy.values(), BadgeDef.ScopeBy::wire, scopeBy), scopeValues);
		else if (! scopeValues.isEmpty())
			throw FtlAttrLists.reject("<@badge id='" + id + "'> scopeValues requires scopeParam= and scopeBy=.");

		try {
			def.validate();
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw FtlAttrLists.reject(e.getMessage());
		}
		cap.addBadge(def);
	}

	private static <E> E enumOf(String badgeId, String attr, E[] values, java.util.function.Function<E,String> wire, String v)
			throws TemplateModelException {
		for (var e : values)
			if (wire.apply(e).equals(v))
				return e;
		var allowed = new ArrayList<String>();
		for (var e : values)
			allowed.add(wire.apply(e));
		throw FtlAttrLists.reject(f("<@badge id='%s'> %s= '%s' is not one of '%s'.", badgeId, attr, v,
			String.join(", ", allowed)));
	}

	private static Map<String,List<String>> parseScopeValues(String badgeId, Object raw) throws TemplateModelException {
		if (! (raw instanceof Map<?,?> m))
			throw FtlAttrLists.reject("<@badge id='" + badgeId + "'> scopeValues must be a JSON5 object of string lists.");
		var out = new LinkedHashMap<String,List<String>>();
		for (var e : m.entrySet()) {
			if (! (e.getValue() instanceof List<?> l))
				throw FtlAttrLists.reject("<@badge id='" + badgeId + "'> scopeValues must be a JSON5 object of string lists.");
			var vals = new ArrayList<String>();
			for (var item : l)
				vals.add(String.valueOf(item));
			out.put(String.valueOf(e.getKey()), vals);
		}
		return out;
	}

	private static List<VisibilityRule> parseVisibleWhen(String badgeId, Object raw) throws TemplateModelException {
		if (! (raw instanceof List<?> list))
			throw FtlAttrLists.reject("<@badge id='" + badgeId + "'> visibleWhen must be a JSON5 array.");
		var out = new ArrayList<VisibilityRule>();
		for (var item : list) {
			if (! (item instanceof Map<?,?> m) || m.get("field") == null)
				throw FtlAttrLists.reject("<@badge id='" + badgeId + "'> each visibleWhen entry must be an object with a field.");
			var b = VisibilityRule.when(String.valueOf(m.get("field")));
			var op = String.valueOf(m.get("op"));
			var value = m.get("value");
			out.add(switch (op) {
				case "eq" -> b.eq(value);
				case "ne" -> b.ne(value);
				case "present" -> b.present();
				case "absent" -> b.absent();
				case "in" -> {
					if (! (value instanceof List<?> l))
						throw FtlAttrLists.reject("<@badge id='" + badgeId + "'> visibleWhen op 'in' requires a list value.");
					yield b.in(l.toArray());
				}
				case "contains" -> b.contains(value);
				default -> throw FtlAttrLists.reject(f(
					"<@badge id='%s'> visibleWhen op '%s' is not one of 'eq, ne, present, absent, in, contains'.", badgeId, op));
			});
		}
		return out;
	}
}
