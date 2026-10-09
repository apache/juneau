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
package org.apache.juneau.rest.server.console.test;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;

/**
 * Test helper that extracts the {@code #juneau-page} contract and {@code <template>} ids from a
 * rendered console page, validates them against {@link PageContractSchema}, and offers fluent
 * assertions so adopters assert on the contract instead of on rendered chrome HTML.
 *
 * <p>
 * Failures throw {@link AssertionError}, so the class works under any test framework.
 *
 * <p>
 * {@link #assertPage(String)} also fails immediately if the contract still carries the pre-rename top-level
 * <c>version</c> key (now <c>contractVersion</c>), even when {@link #isValid()} is never called.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	String <jv>html</jv> = <jv>client</jv>.get(<js>"/slo"</js>).run().getContent().asString();
 * 	PageContractAssert.<jsm>assertPage</jsm>(<jv>html</jv>)
 * 		.isValid()
 * 		.hasContractVersion(<js>"1"</js>)
 * 		.hasActiveNav(<js>"slo"</js>)
 * 		.hasNavPath(<js>"home"</js>, <js>"about"</js>)
 * 		.hasNavHref(<js>"slo"</js>, <js>"/slo"</js>)
 * 		.hasFooterText(<js>"Sandbox Support Console"</js>)
 * 		.hasHeaderSlot(<js>"banner"</js>)
 * 		.hasCard(<js>"releases"</js>, <js>"datatables"</js>)
 * 		.hasCardKey(<js>"releases"</js>, <js>"/table/dataUrl"</js>, <js>"/rest/releases/data"</js>)
 * 		.templateContains(<js>"jc-seg-1"</js>, <js>"id=\"ssc-table-slot\""</js>);
 *
 * 	PageContractAssert.<jsm>assertPage</jsm>(<jv>html</jv>)
 * 		.subscribes(<js>"tasks"</js>, <js>"selection:changes"</js>, <js>"params"</js>)
 * 		.declaresTopic(<js>"ops.jobs"</js>)
 * 		.hasBridge(<js>"ops"</js>)
 * 		.hasNoWiringErrors();
 * </p>
 *
 * @since 10.0.0
 */
public final class PageContractAssert {

	private static final Pattern ISLAND = Pattern.compile(
		"<script type=\"application/json\" id=\"juneau-page\">(.*?)</script>", Pattern.DOTALL);
	private static final Pattern TEMPLATE_OPEN = Pattern.compile("<template data-(card|slot)=\"([^\"]+)\">");

	private final String json;
	private final JsonMap contract;
	private final Map<String,String> templates;

	private PageContractAssert(String html) {
		var m = ISLAND.matcher(html);
		if (! m.find())
			throw new AssertionError("no <script type=\"application/json\" id=\"juneau-page\"> in page");
		json = m.group(1);
		try {
			contract = JsonMap.ofString(json);
		} catch (Exception e) {
			throw new AssertionError("unparseable page contract: " + e.getMessage(), e);
		}
		if (contract.containsKey("version"))
			throw new AssertionError("page contract key 'version' was renamed to 'contractVersion'; regenerate the page with a current Juneau");
		templates = scanTemplates(html);
	}

	/**
	 * Parses the contract and templates out of a rendered page.
	 *
	 * @param html The full rendered page.
	 * @return A new assertion object.
	 * @throws AssertionError If the page has no parseable contract island.
	 */
	public static PageContractAssert assertPage(String html) {
		return new PageContractAssert(html);
	}

	/**
	 * Asserts the contract passes the schema and rules R-1..R-4.
	 *
	 * @return This object.
	 */
	public PageContractAssert isValid() {
		var errors = PageContractSchema.get().validate(json, templates.keySet());
		if (! errors.isEmpty())
			throw new AssertionError("page contract is invalid:\n" + String.join("\n", errors));
		return this;
	}

	/** @param v The expected contract version. @return This object. */
	public PageContractAssert hasContractVersion(String v) {
		return eq("contractVersion", v, contract.getString("contractVersion"));
	}

	/** @param v The expected page title. @return This object. */
	public PageContractAssert hasTitle(String v) {
		return eq("title", v, contract.getString("title"));
	}

	/** @param name The expected theme name. @return This object. */
	public PageContractAssert hasTheme(String name) {
		var t = contract.getMap("theme");
		return eq("theme.name", name, t == null ? null : t.getString("name"));
	}

	/** @param idPath The expected active path, root first. @return This object. */
	public PageContractAssert hasActiveNav(String... idPath) {
		var actual = strings(contract.getList("activeNav"));
		if (! actual.equals(List.of(idPath)))
			throw new AssertionError("activeNav: expected " + List.of(idPath) + " but was " + actual);
		return this;
	}

	/** Asserts {@code activeNav} is empty (the JS prefix fallback applies). @return This object. */
	public PageContractAssert hasNoActiveNav() {
		var actual = strings(contract.getList("activeNav"));
		if (! actual.isEmpty())
			throw new AssertionError("activeNav: expected [] but was " + actual);
		return this;
	}

	/** @param idPath A root-first id path that must exist in {@code nav}. @return This object. */
	public PageContractAssert hasNavPath(String... idPath) {
		if (idPath.length == 0)
			throw new AssertionError("hasNavPath() requires at least one path segment");
		node(List.of(idPath));
		return this;
	}

	/**
	 * @param id A node id, or a {@code /}-separated id path such as {@code "home/about"}.
	 * @param href The expected href.
	 * @return This object.
	 */
	public PageContractAssert hasNavHref(String id, String href) {
		var n = node(List.of(id.split("/")));
		if (neq(href, n.getString("href")))
			throw new AssertionError("nav '" + id + "' href: expected '" + href + "' but was '" + n.getString("href") + "'");
		return this;
	}

	/**
	 * @param idPath A {@code /}-separated id path.
	 * @param childIds The expected child ids, in order.
	 * @return This object.
	 */
	public PageContractAssert hasNavChildren(String idPath, String... childIds) {
		var n = node(List.of(idPath.split("/")));
		var kids = n.getList("children");
		var actual = kids == null ? List.<String>of() : kids.stream().map(k -> ((JsonMap)k).getString("id")).toList();
		if (! actual.equals(List.of(childIds)))
			throw new AssertionError("nav '" + idPath + "' children: expected " + List.of(childIds) + " but was " + actual);
		return this;
	}

	/** @param v The expected {@code header.title}. @return This object. */
	public PageContractAssert hasHeaderTitle(String v) {
		var h = contract.getMap("header");
		return eq("header.title", v, h == null ? null : h.getString("title"));
	}

	/** @param name A header slot name ({@code brand}, {@code actions}, {@code replace}, {@code banner}). @return This object. */
	public PageContractAssert hasHeaderSlot(String name) {
		return slot("header", name);
	}

	/** @param v The expected {@code footer.text}. @return This object. */
	public PageContractAssert hasFooterText(String v) {
		var f = contract.getMap("footer");
		return eq("footer.text", v, f == null ? null : f.getString("text"));
	}

	/** @param name A footer slot name ({@code content}). @return This object. */
	public PageContractAssert hasFooterSlot(String name) {
		return slot("footer", name);
	}

	/**
	 * @param id The card id.
	 * @param type The expected card type.
	 * @return This object.
	 */
	public PageContractAssert hasCard(String id, String type) {
		var c = card(id);
		if (neq(type, c.getString("type")))
			throw new AssertionError("card '" + id + "' type: expected '" + type + "' but was '" + c.getString("type") + "'");
		return this;
	}

	/** @param ids The expected card ids, in contract order. @return This object. */
	public PageContractAssert hasCardOrder(String... ids) {
		var actual = cardIds();
		if (! actual.equals(List.of(ids)))
			throw new AssertionError("card order: expected " + List.of(ids) + " but was " + actual);
		return this;
	}

	/** @param cardId The card id. @param topic A topic the card must subscribe to. @return This object. */
	public PageContractAssert subscribes(String cardId, String topic) {
		subscription(cardId, topic, null);
		return this;
	}

	/**
	 * @param cardId The card id.
	 * @param topic A topic the card must subscribe to.
	 * @param role The role it must be wired to.
	 * @return This object.
	 */
	public PageContractAssert subscribes(String cardId, String topic, String role) {
		subscription(cardId, topic, role);
		return this;
	}

	/** @param cardId The card id. @param topic A custom topic the card must declare in {@code publishes}. @return This object. */
	public PageContractAssert publishes(String cardId, String topic) {
		var topics = topicsOf(card(cardId).getList("publishes"));
		if (! topics.contains(topic))
			throw new AssertionError("card '" + cardId + "' does not publish '" + topic + "'; publishes: " + topics);
		return this;
	}

	/** @param topic A top-level {@code topics} entry that must exist. @return This object. */
	public PageContractAssert declaresTopic(String topic) {
		var topics = topicsOf(contract.getList("topics"));
		if (! topics.contains(topic))
			throw new AssertionError("no topics entry '" + topic + "'; topics: " + topics);
		return this;
	}

	/** @param id A top-level {@code bridges} id that must exist. @return This object. */
	public PageContractAssert hasBridge(String id) {
		var ids = new ArrayList<String>();
		var l = contract.getList("bridges");
		if (l != null)
			for (var o : l)
				ids.add(((JsonMap)o).getString("id"));
		if (! ids.contains(id))
			throw new AssertionError("no bridge '" + id + "'; bridges: " + ids);
		return this;
	}

	/**
	 * Runs R-10 and R-11 against the standard card-type registry. Pair it with {@code BusPolicy.check(contract())}
	 * (juneau-rest-server-bus) for the server-grant side (E-55). A page with custom card types needs the
	 * {@link #hasNoWiringErrors(CardTypeRegistry)} overload with the registry that knows them.
	 *
	 * @return This object.
	 */
	public PageContractAssert hasNoWiringErrors() {
		return hasNoWiringErrors(CardTypeRegistry.standard());
	}

	/** @param registry The registry that knows the page's card types. @return This object. */
	public PageContractAssert hasNoWiringErrors(CardTypeRegistry registry) {
		var problems = BusWiringValidator.validate(contract, registry);
		if (! problems.isEmpty())
			throw new AssertionError("page wiring has errors:\n"
				+ problems.stream().map(p -> p.code() + " " + p.message()).collect(Collectors.joining("\n")));
		return this;
	}

	private JsonMap subscription(String cardId, String topic, String role) {
		var l = card(cardId).getList("subscribes");
		var roles = new ArrayList<String>();
		if (l != null)
			for (var o : l) {
				var m = (JsonMap)o;
				if (! topic.equals(m.getString("topic")))
					continue;
				if (role == null || role.equals(m.getString("as")))
					return m;
				roles.add(m.getString("as"));
			}
		if (! roles.isEmpty())
			throw new AssertionError("card '" + cardId + "' subscribes to '" + topic + "' as " + roles + ", not '" + role + "'");
		throw new AssertionError("card '" + cardId + "' does not subscribe to '" + topic + "'; subscribes: " + topicsOf(l));
	}

	private static List<String> topicsOf(List<?> l) {
		var out = new ArrayList<String>();
		if (l != null)
			for (var o : l)
				out.add(((JsonMap)o).getString("topic"));
		return out;
	}

	/**
	 * @param id The card id.
	 * @param src The expected {@code src} value.
	 * @return This object.
	 */
	public PageContractAssert hasCardSrc(String id, String src) {
		return eq("card '" + id + "' src", src, card(id).getString("src"));
	}

	/**
	 * @param id The card id.
	 * @param jsonPointer A {@code /}-separated path into the card's JSON, for example {@code "/table/dataUrl"}.
	 * @param expected The expected value at that path.
	 * @return This object.
	 */
	public PageContractAssert hasCardKey(String id, String jsonPointer, Object expected) {
		var actual = resolvePointer(card(id), jsonPointer);
		if (neq(expected, actual))
			throw new AssertionError("card '" + id + "' " + jsonPointer + ": expected '" + expected + "' but was '" + actual + "'");
		return this;
	}

	/**
	 * @param templateId A {@code data-card} or {@code data-slot} template id.
	 * @param fragment Markup that must appear in the template.
	 * @return This object.
	 */
	public PageContractAssert templateContains(String templateId, String fragment) {
		if (! template(templateId).contains(fragment))
			throw new AssertionError("template '" + templateId + "' does not contain '" + fragment + "'");
		return this;
	}

	/**
	 * Escape hatch for asserts this class does not offer.
	 *
	 * @return The parsed contract.
	 */
	public JsonMap contract() {
		return contract;
	}

	/**
	 * @return Every {@code <template data-card|data-slot>} id mapped to its inner markup, in document order.
	 */
	public Map<String,String> templates() {
		return u(templates);
	}

	/**
	 * @param templateId A {@code data-card} or {@code data-slot} template id.
	 * @return The template's inner markup.
	 */
	public String template(String templateId) {
		var t = templates.get(templateId);
		if (t == null)
			throw new AssertionError("no <template data-card|data-slot=\"" + templateId + "\">; templates: " + templates.keySet());
		return t;
	}

	private PageContractAssert eq(String what, String expected, String actual) {
		if (neq(expected, actual))
			throw new AssertionError(what + ": expected '" + expected + "' but was '" + actual + "'");
		return this;
	}

	private PageContractAssert slot(String scope, String name) {
		var sec = contract.getMap(scope);
		var slots = sec == null ? null : sec.getMap("slots");
		if (slots == null || ! slots.containsKey(name))
			throw new AssertionError(scope + ".slots has no '" + name + "'");
		return this;
	}

	private JsonMap node(List<String> path) {
		var level = contract.getList("nav");
		JsonMap found = null;
		for (var id : path) {
			found = null;
			if (level != null)
				for (var o : level)
					if (id.equals(((JsonMap)o).getString("id")))
						found = (JsonMap)o;
			if (found == null)
				throw new AssertionError("nav path " + path + " not found (failed at '" + id + "')");
			level = found.getList("children");
		}
		return found;
	}

	/**
	 * @param id The card id.
	 * @return The card's parsed JSON.
	 */
	public JsonMap card(String id) {
		for (var o : contract.getList("cards"))
			if (id.equals(((JsonMap)o).getString("id")))
				return (JsonMap)o;
		throw new AssertionError("no card '" + id + "'; cards: " + cardIds());
	}

	private List<String> cardIds() {
		return contract.getList("cards").stream().map(o -> ((JsonMap)o).getString("id")).toList();
	}

	private static Object resolvePointer(Object node, String pointer) {
		if (pointer.isEmpty())
			return node;
		if (! pointer.startsWith("/"))
			throw new AssertionError("json pointer '" + pointer + "' must start with '/'");
		var cur = node;
		for (var raw : pointer.substring(1).split("/", -1)) {
			var seg = raw.replace("~1", "/").replace("~0", "~");
			if (cur instanceof Map<?,?> m) {
				cur = m.get(seg);
			} else if (cur instanceof List<?> l) {
				var idx = Integer.parseInt(seg);
				cur = idx >= 0 && idx < l.size() ? l.get(idx) : null;
			} else {
				throw new AssertionError("json pointer '" + pointer + "' hit a leaf before '" + seg + "'; node was: " + cur);
			}
		}
		return cur;
	}

	private static List<String> strings(List<?> l) {
		return l == null ? List.of() : l.stream().map(String::valueOf).toList();
	}

	// Depth-aware: page markup may itself contain <template> elements.
	private static Map<String,String> scanTemplates(String html) {
		var out = new LinkedHashMap<String,String>();
		var m = TEMPLATE_OPEN.matcher(html);
		var from = 0;
		while (m.find(from)) {
			var start = m.end();
			var depth = 1;
			var i = start;
			while (depth > 0) {
				var open = html.indexOf("<template", i);
				var close = html.indexOf("</template>", i);
				if (close < 0)
					throw new AssertionError("unterminated <template data-" + m.group(1) + "=\"" + m.group(2) + "\">");
				if (open >= 0 && open < close) {
					depth++;
					i = open + 9;
				} else {
					depth--;
					i = close + 11;
				}
			}
			out.put(m.group(2), html.substring(start, i - 11));
			from = i;
		}
		return out;
	}
}
