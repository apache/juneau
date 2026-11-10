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
package org.apache.juneau.console.freemarker;

import java.util.*;

import org.apache.juneau.console.*;
import org.apache.juneau.marshall.collections.*;

/**
 * The built-in {@code switcher} card type: a JSON5 body naming {@code views}, each holding one {@code card} or a list
 * of {@code sections} that each hold a card.  {@code juneau-switcher.js} renders it.  The body is validated by
 * {@link SwitcherDef#validate(String, Map)}, the same check {@link SwitcherDef} runs, and every nested card is lifted
 * through its own type's handler by the registry (see {@link CardTypeHandler#nestedCards(JsonMap)}).
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bcode'>
 * 	&lt;@card type="switcher" id="revenue-sw" title="Revenue"&gt;
 * 	{ views: [ { id: 'chart', label: 'Chart', card: { id: 'rev', type: 'chart', src: '/s', kind: 'line', x: { field: 'd' }, series: [ 'n' ] } } ] }
 * 	&lt;/@card&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class SwitcherCardType implements CardTypeHandler {

	/** Public no-arg constructor, required by {@link java.util.ServiceLoader}. */
	public SwitcherCardType() {
		// No state; instantiated by ServiceLoader.
	}

	@Override
	public String type() {
		return "switcher";
	}

	@Override
	public JsonMap toFragment(CardSource source) {
		if (! source.hasJsonBody())
			throw source.error("type='switcher' requires a JSON5 body.");
		var body = source.json();
		try {
			SwitcherDef.validate(source.id(), body);
		} catch (IllegalArgumentException e) {
			throw source.error("%s", e.getMessage());
		}
		var frag = new JsonMap();
		for (var e : body.entrySet())
			if (! "visibleWhen".equals(e.getKey()))
				frag.put(e.getKey(), copy(e.getValue()));
		return frag;
	}

	@Override
	public List<Map<String,Object>> nestedCards(JsonMap body) {
		var out = new ArrayList<Map<String,Object>>();
		if (body.get("views") instanceof List<?> views)
			for (var v : views) {
				if (! (v instanceof Map<?,?> vm))
					continue;
				add(out, vm.get("card"));
				if (vm.get("sections") instanceof List<?> secs)
					for (var s : secs)
						if (s instanceof Map<?,?> sm)
							add(out, sm.get("card"));
			}
		return out;
	}

	@SuppressWarnings({
		"unchecked" // Card maps are JSON objects, so their keys are strings.
	})
	private static void add(List<Map<String,Object>> out, Object card) {
		if (card instanceof Map<?,?> m)
			out.add((Map<String,Object>)m);
	}

	// A deep copy, so lifting a nested card never rewrites the authored body.
	private static Object copy(Object o) {
		if (o instanceof Map<?,?> m) {
			var c = new LinkedHashMap<String,Object>();
			m.forEach((k, v) -> c.put(String.valueOf(k), copy(v)));
			return c;
		}
		if (o instanceof List<?> l)
			return new ArrayList<>(l.stream().map(SwitcherCardType::copy).toList());
		return o;
	}
}
