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
package org.apache.juneau.rest.server.console;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.marshall.collections.*;

/**
 * Static check of a page contract's message-bus wiring (spec §6.1, §11.2): rule R-10 (every
 * subscription has a publisher), rule R-11 (every {@code publisher:"server"} topic rides a bridge downstream), and the
 * E-40..E-54 shape rules on {@code topics}, {@code bridges}, card {@code publishes} / {@code subscribes} and ribbon
 * {@code target} / {@code publish} items.
 *
 * <p>
 * The same rule runs in the browser as {@code JuneauViews.bus.wiring.validate}, and both read the shared corpus
 * {@code bus-wiring-corpus.json}.  The JS side reports the subset that only the running page can act on (E-JS-41,
 * E-JS-47, E-JS-52); this class reports everything.
 *
 * <p>
 * Every problem is collected; nothing is thrown.  Callers decide how loud to be (spec §6.1): the page render, the
 * template validator and C3's {@code PageSpec.build()} each turn the list into their own failure.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   JsonMap <jv>contract</jv> = JsonMap.<jsm>ofString</jsm>(<jv>pageJson</jv>);
 *   <jk>for</jk> (<jk>var</jk> <jv>p</jv> : BusWiringValidator.<jsm>validate</jsm>(<jv>contract</jv>, CardTypeRegistry.<jsm>standard</jsm>()))
 *     System.<jsf>err</jsf>.println(<jv>p</jv>.code() + <js>" "</js> + <jv>p</jv>.message());
 *   <jc>// E-41 card 'tasks' subscribes to 'selection:changes' but nothing publishes it; card 'changes' does not enable selection</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class BusWiringValidator {

	/**
	 * One wiring problem.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>var</jk> <jv>p</jv> = <jk>new</jk> BusWiringValidator.Problem(<js>"E-52"</js>,
	 *     <js>"topic 'ops.jobs' is declared publisher=server but no bridge carries it downstream"</js>);
	 *   <jv>p</jv>.toString();   <jc>// "E-52 topic 'ops.jobs' is declared ..."</jc>
	 * </p>
	 *
	 * @param code The Java code, {@code E-40}..{@code E-54}.
	 * @param message The spec §6.2 message, without the code.
	 */
	public record Problem(String code, String message) {
		@Override
		public String toString() {
			return code + " " + message;
		}
	}

	static final String E40 = "invalid topic '%s': expected family[:key] (see the topic syntax)";
	static final String E41 = "card '%s' subscribes to '%s' but nothing publishes it%s";
	static final String E41_SUGGEST = "; did you mean '%s'?";
	static final String E41_NOT_ENABLED = "; card '%s' does not enable %s";
	static final String E42 = "topic '%s' names card '%s', which is not on this page";
	static final String E43_PUBLISH = "card '%s' (type '%s') does not publish '%s'";
	static final String E43_OP = "card '%s' (type '%s') does not handle op '%s'";
	static final String E44 = "card '%s' (type '%s') has no role '%s'; accepted: %s";
	static final String E45_FRAMEWORK = "'%s' is a framework family; framework topics are implicit and may not be declared";
	static final String E45_NAMESPACE = "custom topic family '%s' must be namespaced (e.g. 'app.%s')";
	static final String E46 = "topic '%s' is declared with retain=%s here and retain=%s at %s";
	static final String E47_TOKEN = "card '%s': token '{%s}' in %s is not mapped by any params subscription";
	static final String E47_MAP = "card '%s': params subscription needs map";
	static final String E48 = "ribbon item %s on card '%s': %s";
	static final String E49 = "card '%s' subscribes to its own topic '%s'; use ctx.subscribe with {echo:true} in JS if this is intended";
	static final String E51 = "bridge '%s': %s";
	static final String E52 = "topic '%s' is declared publisher=server but no bridge carries it downstream";
	static final String E53 = "bridge '%s' sends '%s' upstream, but no topics entry or card publishes declares it";
	static final String E54 = "bridge '%s' may not carry framework state topic '%s' (only job:*, and cmd:<cardId> downstream)";

	// Topic grammar (schema §5.7): one copy, owned by Topics.
	static final Set<String> FRAMEWORK_FAMILIES = Topics.FRAMEWORK_FAMILIES;
	static final Pattern TOPIC = Topics.TOPIC;
	static final Pattern CUSTOM_DECL = Topics.PUBLICATION;
	static final Pattern UNNAMESPACED = Pattern.compile("^([a-z][a-z0-9-]{0,31})(:([A-Za-z0-9_.-]{1,128}|\\*))?$");
	static final Pattern BRIDGE_ID = Topics.ID;
	static final Pattern SAME_ORIGIN_PATH = Pattern.compile("^/(?![/\\\\])\\S*$");
	static final Pattern URL_TOKEN = Pattern.compile("\\{([A-Za-z0-9_-]+)\\}");

	/** Families whose key is a card id (spec §4). */
	static final Set<String> CARD_FAMILIES = Set.of("card", "selection", "filter", "redraw", "detail", "bulk", "cmd");
	/** Roles the shell implements for every card type (spec §5.3). */
	static final Set<String> SHELL_ROLES = Set.of("refresh", "params");
	/** The {@code cmd:} op each targetable ribbon item sends (spec §5.5). */
	static final Map<String,String> RIBBON_OPS = Map.of(
		"refresh", "reload", "option", "set-filter", "optionGroup", "set-filter",
		"pausePolling", "pause-polling", "collapseAll", "collapse-all");
	/** Datatables families that need a catalog key to be published (spec §5.4). */
	static final Set<String> DATATABLES_OPT_IN = Set.of("selection", "detail", "bulk");
	/** Largest edit distance a "did you mean" suggestion may have. */
	static final int SUGGEST_MAX_DISTANCE = 3;

	private BusWiringValidator() {}

	/**
	 * Checks a page contract's bus wiring.
	 *
	 * @param contract The page contract (the {@code #juneau-page} JSON).
	 * @param registry Card types, for implicit topics, accepted roles and ops.  Unregistered types are skipped.
	 * @return Every problem, in contract order: {@code topics}, then each card, then {@code bridges}, then R-11.
	 */
	public static List<Problem> validate(JsonMap contract, CardTypeRegistry registry) {
		return new Run(contract, registry).run();
	}

	/** {@code pattern} equals {@code topic}, or is {@code family:*} and {@code topic} is a concrete {@code family:key}. */
	static boolean topicMatches(String pattern, String topic) {
		if (pattern.equals(topic))
			return true;
		if (! pattern.endsWith(":*"))
			return false;
		var prefix = pattern.substring(0, pattern.length() - 1);
		return topic.length() > prefix.length() && topic.startsWith(prefix);
	}

	/** Levenshtein distance. */
	static int distance(String a, String b) {
		var prev = new int[b.length() + 1];
		var cur = new int[b.length() + 1];
		for (var j = 0; j <= b.length(); j++)
			prev[j] = j;
		for (var i = 1; i <= a.length(); i++) {
			cur[0] = i;
			for (var j = 1; j <= b.length(); j++) {
				var cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
			}
			var t = prev;
			prev = cur;
			cur = t;
		}
		return prev[b.length()];
	}

	private static final class Run {
		final CardTypeRegistry registry;
		final List<Problem> problems = new ArrayList<>();
		final List<Map<?,?>> topics;
		final List<Map<?,?>> bridges;
		final Map<String,Map<?,?>> cards = new LinkedHashMap<>();
		/** Every topic or pattern something on the page publishes (R-10 rules 2-6), sorted for the suggestion. */
		final TreeSet<String> published = new TreeSet<>();
		/** topic -> [retain, where] of its first declaration (E-46). */
		final Map<String,Object[]> retains = new HashMap<>();

		Run(JsonMap contract, CardTypeRegistry registry) {
			this.registry = registry;
			topics = maps(contract.get("topics"));
			bridges = maps(contract.get("bridges"));
			for (var c : maps(contract.get("cards")))
				if (str(c.get("id")) != null)
					cards.putIfAbsent(str(c.get("id")), c);
		}

		List<Problem> run() {
			collectPublishers();
			for (var i = 0; i < topics.size(); i++)
				checkTopicDecl(i, topics.get(i));
			for (var c : cards.values())
				checkCard(c);
			checkBridges();
			checkServerTopics();
			return List.copyOf(problems);
		}

		void add(String code, String format, Object... args) {
			problems.add(new Problem(code, String.format(format, args)));
		}

		// ---- R-10 publishers --------------------------------------------------------------------------------------

		void collectPublishers() {
			for (var c : cards.values()) {
				var id = str(c.get("id"));
				published.add("card:" + id);
				var type = str(c.get("type"));
				if (type != null && registry.isRegistered(type))
					published.addAll(registry.handler(type).implicitTopics(json(c)));
				for (var p : maps(c.get("publishes")))
					addCustom(str(p.get("topic")));
				for (var item : ribbon(c))
					if ("publish".equals(str(item.get("type"))))
						addCustom(str(item.get("topic")));
			}
			for (var t : topics) {
				var topic = str(t.get("topic"));
				if ("job:*".equals(topic))
					published.add(topic);
				else
					addCustom(topic);
			}
			for (var b : bridges) {
				var id = str(b.get("id"));
				if (id != null)
					published.add("bridge:" + id);
				for (var d : strings(b.get("downstream")))
					if (d.startsWith("job:"))
						published.add(d);
			}
		}

		void addCustom(String topic) {
			if (topic != null && CUSTOM_DECL.matcher(topic).matches())
				published.add(topic);
		}

		boolean isPublished(String topic) {
			for (var p : published)
				if (topicMatches(p, topic))
					return true;
			return false;
		}

		String suggest(String topic) {
			String best = null;
			var bestDistance = SUGGEST_MAX_DISTANCE + 1;
			for (var p : published) {
				var d = distance(topic, p);
				if (d < bestDistance) {
					best = p;
					bestDistance = d;
				}
			}
			return best == null ? "" : String.format(E41_SUGGEST, best);
		}

		// ---- declarations (E-40, E-45, E-46) ----------------------------------------------------------------------

		void checkTopicDecl(int index, Map<?,?> decl) {
			var topic = str(decl.get("topic"));
			if ("job:*".equals(topic)) {
				if (! "server".equals(str(decl.get("publisher"))))
					add("E-45", E45_FRAMEWORK, "job");
				return;
			}
			checkDeclared(topic, decl.get("retain"), "topics[" + index + "]");
		}

		void checkDeclared(String topic, Object retain, String where) {
			if (topic != null && CUSTOM_DECL.matcher(topic).matches()) {
				var first = retains.putIfAbsent(topic, new Object[] { retain, where });
				if (first != null && ! Objects.equals(first[0], retain))
					add("E-46", E46, topic, retain, first[0], first[1]);
				return;
			}
			var family = topic == null ? null : family(topic);
			if (family != null && FRAMEWORK_FAMILIES.contains(family))
				add("E-45", E45_FRAMEWORK, family);
			else if (topic != null && UNNAMESPACED.matcher(topic).matches())
				add("E-45", E45_NAMESPACE, family, family);
			else
				add("E-40", E40, topic);
		}

		// ---- cards ------------------------------------------------------------------------------------------------

		void checkCard(Map<?,?> card) {
			var id = str(card.get("id"));
			for (var p : maps(card.get("publishes")))
				checkDeclared(str(p.get("topic")), p.get("retain"), "card '" + id + "'");
			var params = new LinkedHashSet<String>();
			var hasParams = false;
			for (var s : maps(card.get("subscribes"))) {
				checkSubscription(card, s);
				if ("params".equals(str(s.get("as")))) {
					hasParams = true;
					if (s.get("map") instanceof Map<?,?> m)
						m.keySet().forEach(k -> params.add(String.valueOf(k)));
					else
						add("E-47", E47_MAP, id);
				}
			}
			var items = ribbon(card);
			for (var i = 0; i < items.size(); i++)
				checkRibbonItem(card, i, items.get(i));
			if (hasParams) {
				checkTokens(id, "src", str(card.get("src")), params);
				checkTokens(id, "dataUrl", str(view(card).get("dataUrl")), params);
			}
		}

		void checkTokens(String id, String field, String url, Set<String> mapped) {
			if (url == null)
				return;
			var seen = new HashSet<String>();
			var m = URL_TOKEN.matcher(url);
			while (m.find())
				if (! mapped.contains(m.group(1)) && seen.add(m.group(1)))
					add("E-47", E47_TOKEN, id, m.group(1), field);
		}

		void checkSubscription(Map<?,?> card, Map<?,?> sub) {
			var id = str(card.get("id"));
			var topic = str(sub.get("topic"));
			if (topic == null || ! TOPIC.matcher(topic).matches())
				add("E-40", E40, topic);
			else
				checkPublisher(id, topic);
			checkRole(card, str(sub.get("as")));
		}

		void checkPublisher(String id, String topic) {
			var family = family(topic);
			var key = key(topic);
			if (FRAMEWORK_FAMILIES.contains(family) && key != null) {
				if (CARD_FAMILIES.contains(family)) {
					if (key.equals(id)) {
						add("E-49", E49, id, topic);
						return;
					}
					var target = cards.get(key);
					if (target == null) {
						add("E-42", E42, topic, key);
						return;
					}
					if (family.equals("card") || family.equals("cmd"))
						return;
					var type = str(target.get("type"));
					if (type == null || ! registry.isRegistered(type) || published.contains(topic))
						return;
					if ("datatables".equals(type) && DATATABLES_OPT_IN.contains(family))
						add("E-41", E41, id, topic, String.format(E41_NOT_ENABLED, key, family));
					else
						add("E-43", E43_PUBLISH, key, type, topic);
					return;
				}
				// probe: and badge: are owned by toolkit runtimes the contract does not describe; JS checks them.
				if (family.equals("probe") || family.equals("badge"))
					return;
			}
			if (! isPublished(topic))
				add("E-41", E41, id, topic, suggest(topic));
		}

		void checkRole(Map<?,?> card, String as) {
			if (as == null || SHELL_ROLES.contains(as))
				return;
			var type = str(card.get("type"));
			if (type == null || ! registry.isRegistered(type))
				return;
			var roles = registry.handler(type).acceptedRoles();
			if (roles == null || roles.contains(as))
				return;
			var accepted = new TreeSet<>(SHELL_ROLES);
			accepted.addAll(roles);
			add("E-44", E44, str(card.get("id")), type, as, String.join(", ", accepted));
		}

		// ---- ribbon items (E-42, E-43, E-48) ----------------------------------------------------------------------

		void checkRibbonItem(Map<?,?> card, int index, Map<?,?> item) {
			var id = str(card.get("id"));
			var type = str(item.get("type"));
			var label = "#" + index + " (" + type + ")";
			if ("publish".equals(type)) {
				checkPublishItem(id, label, item);
				return;
			}
			var target = item.get("target");
			if (target == null)
				return;
			if (type == null || ! RIBBON_OPS.containsKey(type)) {
				add("E-48", E48, label, id, "'target' is not allowed on a '" + type + "' item");
				return;
			}
			var tid = str(target);
			var tc = tid == null ? null : cards.get(tid);
			if (tc == null) {
				add("E-42", E42, "cmd:" + tid, tid);
				return;
			}
			var op = RIBBON_OPS.get(type);
			var ttype = str(tc.get("type"));
			if (ttype != null && registry.isRegistered(ttype)) {
				var ops = registry.handler(ttype).acceptedOps();
				if (ops != null && ! ops.contains(op)) {
					add("E-43", E43_OP, tid, ttype, op);
					return;
				}
			}
			if (op.equals("set-filter") && ! tid.equals(id)) {
				var option = str(item.get("id"));
				if (! declaredOptions(tc).contains(option))
					add("E-48", E48, label, id, "option '" + option + "' is not declared on target card '" + tid + "'");
			}
		}

		void checkPublishItem(String id, String label, Map<?,?> item) {
			if (str(item.get("title")) == null)
				add("E-48", E48, label, id, "a publish item needs a title");
			var topic = str(item.get("topic"));
			if (topic == null || ! TOPIC.matcher(topic).matches()) {
				add("E-40", E40, topic);
				return;
			}
			var family = family(topic);
			if (FRAMEWORK_FAMILIES.contains(family)) {
				var key = key(topic);
				if (family.equals("cmd") && key != null) {
					if (! cards.containsKey(key))
						add("E-42", E42, topic, key);
					return;
				}
				add("E-48", E48, label, id, "'" + topic + "' is a framework topic; a publish item may only publish custom topics or cmd:<cardId>");
				return;
			}
			if (! declaredCustom(topic, Set.of("script", "server", "ribbon"), true))
				add("E-48", E48, label, id, "topic '" + topic + "' is not declared in topics or by any card's publishes");
		}

		/**
		 * The option ids a card declares on its own ribbon: its untargeted (or self-targeted) {@code option} and
		 * {@code optionGroup} items.  C2's {@code tableCatalog} has no {@code filterOptions} key, so the ribbon is the
		 * only place an option id can be declared.
		 */
		Set<String> declaredOptions(Map<?,?> card) {
			var out = new HashSet<String>();
			var cid = str(card.get("id"));
			for (var item : ribbon(card)) {
				var type = str(item.get("type"));
				var target = str(item.get("target"));
				if (("option".equals(type) || "optionGroup".equals(type)) && (target == null || target.equals(cid)) && str(item.get("id")) != null)
					out.add(str(item.get("id")));
			}
			return out;
		}

		// ---- bridges (E-51, E-53, E-54) and R-11 (E-52) -----------------------------------------------------------

		void checkBridges() {
			var seen = new HashSet<String>();
			for (var b : bridges) {
				var id = str(b.get("id"));
				var name = id == null ? "?" : id;
				if (id == null || ! BRIDGE_ID.matcher(id).matches())
					add("E-51", E51, name, "id must match " + BRIDGE_ID.pattern());
				else if (! seen.add(id))
					add("E-51", E51, name, "duplicate id");
				var transport = str(b.get("transport"));
				if (! "sse".equals(transport) && ! "websocket".equals(transport))
					add("E-51", E51, name, "unknown transport '" + transport + "'");
				var session = str(b.get("session"));
				if (session == null || ! SAME_ORIGIN_PATH.matcher(session).matches())
					add("E-51", E51, name, "session must be a same-origin path, got '" + session + "'");
				var max = b.get("maxAttempts");
				if (max != null && ! ((max instanceof Integer || max instanceof Long) && ((Number)max).longValue() >= 1))
					add("E-51", E51, name, "maxAttempts must be an integer of at least 1");
				var down = strings(b.get("downstream"));
				var up = strings(b.get("upstream"));
				if (! up.isEmpty() && ! "websocket".equals(transport))
					add("E-51", E51, name, "upstream is only allowed on a websocket bridge");
				for (var d : down)
					checkDownstream(name, d);
				for (var u : up)
					checkUpstream(name, u);
				for (var d : down)
					if (up.contains(d))
						add("E-51", E51, name, "topic '" + d + "' is both downstream and upstream");
			}
		}

		void checkDownstream(String bridge, String topic) {
			if (! "job:*".equals(topic) && ! TOPIC.matcher(topic).matches() && ! CUSTOM_DECL.matcher(topic).matches()) {
				add("E-51", E51, bridge, "invalid topic '" + topic + "'");
				return;
			}
			var family = family(topic);
			if (FRAMEWORK_FAMILIES.contains(family)) {
				var key = key(topic);
				if (family.equals("job"))
					return;
				if (family.equals("cmd") && key != null) {
					if (! cards.containsKey(key))
						add("E-42", E42, topic, key);
					return;
				}
				add("E-54", E54, bridge, topic);
				return;
			}
			if (! declaredCustom(topic, Set.of("server"), false))
				add("E-51", E51, bridge, "downstream '" + topic + "' is not declared in topics with publisher=server");
		}

		void checkUpstream(String bridge, String topic) {
			if (! TOPIC.matcher(topic).matches() && ! CUSTOM_DECL.matcher(topic).matches()) {
				add("E-51", E51, bridge, "invalid topic '" + topic + "'");
				return;
			}
			if (FRAMEWORK_FAMILIES.contains(family(topic))) {
				add("E-54", E54, bridge, topic);
				return;
			}
			if (! declaredCustom(topic, Set.of("script", "ribbon"), true))
				add("E-53", E53, bridge, topic);
		}

		/** A top-level topics entry with one of {@code publishers}, or (when {@code cardPublishes}) a card's publishes, matches. */
		boolean declaredCustom(String topic, Set<String> publishers, boolean cardPublishes) {
			for (var t : topics) {
				var decl = str(t.get("topic"));
				if (decl != null && str(t.get("publisher")) != null && publishers.contains(str(t.get("publisher"))) && topicMatches(decl, topic))
					return true;
			}
			if (cardPublishes)
				for (var c : cards.values())
					for (var p : maps(c.get("publishes"))) {
						var decl = str(p.get("topic"));
						if (decl != null && topicMatches(decl, topic))
							return true;
					}
			return false;
		}

		void checkServerTopics() {
			for (var t : topics) {
				var topic = str(t.get("topic"));
				if (topic == null || ! "server".equals(str(t.get("publisher"))))
					continue;
				var covered = false;
				for (var b : bridges)
					for (var d : strings(b.get("downstream")))
						covered |= topicMatches(d, topic);
				if (! covered)
					add("E-52", E52, topic);
			}
		}
	}

	// ---- JSON helpers -----------------------------------------------------------------------------------------------

	static String family(String topic) {
		var i = topic.indexOf(':');
		return i < 0 ? topic : topic.substring(0, i);
	}

	static String key(String topic) {
		var i = topic.indexOf(':');
		return i < 0 ? null : topic.substring(i + 1);
	}

	static String str(Object o) {
		return o instanceof String s ? s : null;
	}

	static List<Map<?,?>> maps(Object o) {
		var out = new ArrayList<Map<?,?>>();
		if (o instanceof List<?> l)
			for (var e : l)
				if (e instanceof Map<?,?> m)
					out.add(m);
		return out;
	}

	static List<String> strings(Object o) {
		var out = new ArrayList<String>();
		if (o instanceof List<?> l)
			for (var e : l)
				if (e instanceof String s)
					out.add(s);
		return out;
	}

	/** A datatables card's catalog (its {@code table}), or the card itself for every other shape. */
	static Map<?,?> view(Map<?,?> card) {
		return card.get("table") instanceof Map<?,?> t ? t : card;
	}

	static List<Map<?,?>> ribbon(Map<?,?> card) {
		return maps(view(card).get("ribbon"));
	}

	@SuppressWarnings("unchecked")
	static JsonMap json(Map<?,?> m) {
		if (m instanceof JsonMap j)
			return j;
		var j = new JsonMap();
		j.putAll((Map<String,Object>)m);
		return j;
	}
}
