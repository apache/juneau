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

import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.console.Subscription.*;

/** Lowers {@code <@card subscribes/publishes>} (attribute or JSON5 body) through the {@link Subscription} and {@link TopicDecl} builders. */
final class CardBusAttrs {

	private static final Set<String> SUB_KEYS = new LinkedHashSet<>(List.of("topic", "as", "map", "whenEmpty", "emptyText"));
	private static final Set<String> PUB_KEYS = new LinkedHashSet<>(List.of("topic", "retain"));

	private CardBusAttrs() {}

	/**
	 * {@code "a,b"} (each wired as {@code refresh}), or a sequence of strings / hashes.
	 *
	 * @param cardId The card id, for messages.
	 * @param v The authored value.
	 * @return {@code $defs/subscription} maps, in order.
	 */
	static List<Map<String,Object>> subscriptions(String cardId, Object v) {
		var out = new ArrayList<Map<String,Object>>();
		for (var o : items(v)) {
			if (o instanceof Map<?,?> m) {
				checkKeys(cardId, "subscribes", m, SUB_KEYS);
				var s = Subscription.to(topic(cardId, "subscribes", m));
				if (m.get("as") != null)
					s.as(str(m.get("as")));
				if (m.get("map") instanceof Map<?,?> mm)
					mm.forEach((k, p) -> s.map(str(k), str(p)));
				if (m.get("whenEmpty") != null)
					s.whenEmpty(whenEmpty(cardId, str(m.get("whenEmpty"))));
				if (m.get("emptyText") != null)
					s.emptyText(str(m.get("emptyText")));
				out.add(s.toMap());
			} else {
				out.add(Subscription.to(str(o)).as("refresh").toMap());
			}
		}
		return out;
	}

	/**
	 * {@code "t,u!retain"} ({@code !retain} means {@code retain=true}; otherwise {@code false}), or a sequence of
	 * strings / {@code {topic, retain}} hashes.
	 *
	 * @param cardId The card id, for messages.
	 * @param v The authored value.
	 * @return {@code $defs/publication} maps, in order.
	 */
	static List<Map<String,Object>> publications(String cardId, Object v) {
		var out = new ArrayList<Map<String,Object>>();
		for (var o : items(v)) {
			if (o instanceof Map<?,?> m) {
				checkKeys(cardId, "publishes", m, PUB_KEYS);
				var d = TopicDecl.of(topic(cardId, "publishes", m));
				var retain = m.get("retain");
				if (retain != null) {
					if (! (retain instanceof Boolean r))
						throw iaex(
							"<@card id='%s'> publishes entry retain must be a boolean (true or false); got '%s'", cardId, retain);
					d.retain(r);
				}
				out.add(d.toPublicationMap());
			} else {
				var s = str(o);
				var retain = ew(s, "!retain");
				out.add(TopicDecl.of(retain ? s.substring(0, s.length() - 7) : s).retain(retain).toPublicationMap());
			}
		}
		return out;
	}

	/** Blank entries in a comma list are ignored, so {@code subscribes=""} declares nothing. */
	private static List<?> items(Object v) {
		if (v == null)
			return List.of();
		if (v instanceof String s) {
			var l = new ArrayList<String>();
			for (var part : s.split(","))
				if (! part.isBlank())
					l.add(part.trim());
			return l;
		}
		if (v instanceof Collection<?> c)
			return List.copyOf(c);
		return List.of(v);
	}

	private static String topic(String cardId, String attr, Map<?,?> m) {
		var t = str(m.get("topic"));
		if (ie(t))
			throw iaex("<@card id='%s'> %s entry requires a topic", cardId, attr);
		return t;
	}

	private static void checkKeys(String cardId, String attr, Map<?,?> m, Set<String> allowed) {
		for (var k : m.keySet())
			if (! allowed.contains(String.valueOf(k)))
				throw iaex("<@card id='%s'> %s entry has unknown key '%s'; allowed: %s",
					cardId, attr, k, String.join(", ", allowed));
	}

	private static WhenEmpty whenEmpty(String cardId, String v) {
		return switch (v) {
			case "clear" -> WhenEmpty.CLEAR;
			case "keep" -> WhenEmpty.KEEP;
			default -> throw iaex("<@card id='%s'> whenEmpty '%s' must be clear or keep", cardId, v);
		};
	}

	private static String str(Object o) {
		return o == null ? null : String.valueOf(o).trim();
	}
}
