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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.console.*;

/**
 * Builder for a {@code switcher} card: one card slot that shows one of several named views, each holding one card or a
 * strip of sections that each hold a card.
 *
 * <p>
 * A view with a single section that has the default id and no model block serializes as a plain {@code card}; any
 * other view serializes as {@code sections}.  A switcher cannot hold another switcher.  Validation runs in
 * {@link #toCardSpec()} and is shared with {@link SwitcherCardType} through {@link #validate(String, Map)}, so both
 * paths report the same messages.
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bjava'>
 * 	CardSpec <jv>c</jv> = SwitcherDef.<jsm>create</jsm>(<js>"revenue-sw"</js>).title(<js>"Revenue"</js>)
 * 		.view(<js>"chart"</js>, <js>"Chart"</js>, <jv>v</jv> -&gt; <jv>v</jv>.section(ChartDef.<jsm>line</jsm>(<js>"revenue-chart"</js>).src(<js>"/console/store/stats/revenue"</js>).toCardSpec()))
 * 		.view(<js>"data"</js>, <js>"Data"</js>, <jv>v</jv> -&gt; <jv>v</jv>.section(<js>"all"</js>, <js>"All"</js>, CardSpec.<jsm>of</jsm>(<js>"html"</js>, <js>"d1"</js>)).section(<js>"open"</js>, <js>"Open"</js>, CardSpec.<jsm>of</jsm>(<js>"html"</js>, <js>"d2"</js>)))
 * 		.initialView(<js>"chart"</js>)
 * 		.toCardSpec();
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Map keys read more clearly inline than as constants.
})
public final class SwitcherDef {

	/** The grammar of a view or section id. */
	static final Pattern ID_GRAMMAR = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,63}$");

	private static final String DEFAULT_SECTION = "main";

	private final String id;
	private String title;
	private String initialView;
	private final List<View> views = new ArrayList<>();

	private SwitcherDef(String id) {
		if (ib(id))
			throw iaex("SwitcherDef requires a non-blank id.");
		this.id = id;
	}

	/** @param id The card id. @return A new switcher. */
	public static SwitcherDef create(String id) { return new SwitcherDef(id); }

	/** @param v The card title. @return This object. */
	public SwitcherDef title(String v) { title = v; return this; }

	/** @param viewId The id of the view shown first when nothing else decides. @return This object. */
	public SwitcherDef initialView(String viewId) { initialView = viewId; return this; }

	/**
	 * Adds a view.
	 *
	 * @param viewId The view id, matching {@code [A-Za-z][A-Za-z0-9_-]{0,63}}.
	 * @param label The tab label.
	 * @param config Fills in the view's sections and model block.
	 * @return This object.
	 */
	public SwitcherDef view(String viewId, String label, Consumer<View> config) {
		var v = new View(viewId, label);
		config.accept(v);
		views.add(v);
		return this;
	}

	/**
	 * One view of a switcher.
	 *
	 * <h2 class='section'>Example:</h2>
	 * <p class='bjava'>
	 * 	<jv>v</jv>.section(<js>"all"</js>, <js>"All"</js>, <jv>card</jv>).initialSection(<js>"all"</js>).on(<js>"activated"</js>, <js>"orders.shown"</js>);
	 * </p>
	 */
	public static final class View implements Handled<View> {
		private final String id;
		private final String label;
		private String initialSection;
		private final List<Section> sections = new ArrayList<>();
		private final ModelBlock model;

		private View(String id, String label) {
			this.id = id;
			this.label = label;
			this.model = new ModelBlock("view", id);
		}

		@Override /* Handled */
		public ModelBlock modelBlock() { return model; }

		@Override /* Handled */
		public View self() { return this; }

		/** @param card The only card of the view, in the default section. @return This object. */
		public View section(CardSpec card) {
			sections.add(new Section(DEFAULT_SECTION, label, card, true));
			return this;
		}

		/** @param sectionId The section id. @param sectionLabel The section tab label. @param card The section's card. @return This object. */
		public View section(String sectionId, String sectionLabel, CardSpec card) {
			sections.add(new Section(sectionId, sectionLabel, card, false));
			return this;
		}

		/** @param sectionId The section id. @param sectionLabel The section tab label. @param card The section's card. @param config Fills in the section's model block. @return This object. */
		public View section(String sectionId, String sectionLabel, CardSpec card, Consumer<Section> config) {
			var s = new Section(sectionId, sectionLabel, card, false);
			config.accept(s);
			sections.add(s);
			return this;
		}

		/** @param sectionId The section shown first when nothing else decides. @return This object. */
		public View initialSection(String sectionId) { initialSection = sectionId; return this; }

		private Map<String,Object> toMap() {
			var m = new LinkedHashMap<String,Object>();
			m.put("id", id);
			m.put("label", label);
			if (nn(initialSection)) m.put("initialSection", initialSection);
			if (sections.size() == 1 && sections.get(0).implicit && sections.get(0).model.isEmpty()) {
				m.put("card", sections.get(0).card.toMap());
			} else if (! sections.isEmpty()) {
				m.put("sections", sections.stream().map(Section::toMap).toList());
			}
			var mm = model.toMap();
			if (mm != null) m.put("model", mm);
			return m;
		}
	}

	/**
	 * One section of a switcher view.
	 *
	 * <h2 class='section'>Example:</h2>
	 * <p class='bjava'>
	 * 	<jv>v</jv>.section(<js>"all"</js>, <js>"All"</js>, <jv>card</jv>, <jv>s</jv> -&gt; <jv>s</jv>.on(<js>"activated"</js>, <js>"orders.sectionShown"</js>));
	 * </p>
	 */
	public static final class Section implements Handled<Section> {
		private final String id;
		private final String label;
		private final CardSpec card;
		private final boolean implicit;
		private final ModelBlock model;

		private Section(String id, String label, CardSpec card, boolean implicit) {
			this.id = id;
			this.label = label;
			this.card = card;
			this.implicit = implicit;
			this.model = new ModelBlock("detailtab", id);
		}

		@Override /* Handled */
		public ModelBlock modelBlock() { return model; }

		@Override /* Handled */
		public Section self() { return this; }

		private Map<String,Object> toMap() {
			var m = new LinkedHashMap<String,Object>();
			m.put("id", id);
			m.put("label", label);
			m.put("card", card.toMap());
			var mm = model.toMap();
			if (mm != null) m.put("model", mm);
			return m;
		}
	}

	/**
	 * Builds the {@code switcher} card entry.
	 *
	 * @return The card.
	 * @throws IllegalArgumentException If the switcher is invalid.
	 */
	public CardSpec toCardSpec() {
		var vs = views.stream().map(View::toMap).toList();
		var body = new LinkedHashMap<String,Object>();
		if (nn(initialView)) body.put("initialView", initialView);
		body.put("views", vs);
		validate(id, body);
		var c = CardSpec.of("switcher", id);
		if (nn(title)) c.title(title);
		if (nn(initialView)) c.body("initialView", initialView);
		c.body("views", vs);
		return c;
	}

	/**
	 * Validates a switcher body: the shape the builder emits and the page-contract JSON carries.
	 *
	 * @param id The switcher id, used in messages.
	 * @param body The body holding {@code views} and optionally {@code initialView}.
	 * @throws IllegalArgumentException On the first problem found.
	 */
	@SuppressWarnings({
		"java:S3776" // Branching is inherent to validate(), which checks many view/section combinations.
	})
	public static void validate(String id, Map<String,?> body) {
		var who = "SwitcherDef '" + id + "'";
		if (! (body.get("views") instanceof List<?> views) || views.isEmpty())
			throw iaex("%s requires at least one view.", who);
		var viewIds = new HashSet<String>();
		var cardIds = new HashSet<String>();
		for (var o : views) {
			if (! (o instanceof Map<?,?> v))
				throw iaex("%s views must be objects.", who);
			var vid = String.valueOf(v.get("id"));
			if (! (v.get("id") instanceof String) || ! ID_GRAMMAR.matcher(vid).matches())
				throw iaex("%s view id '%s' must match %s.", who, vid, ID_GRAMMAR.pattern());
			if (! viewIds.add(vid))
				throw iaex("%s view id '%s' is declared more than once.", who, vid);
			if (! (v.get("label") instanceof String))
				throw iaex("%s view '%s' requires a string label.", who, vid);
			var hasCard = v.get("card") != null;
			var hasSections = v.get("sections") != null;
			if (hasCard && hasSections)
				throw iaex("%s view '%s' takes either a card or sections, not both.", who, vid);
			if (! hasCard && (! (v.get("sections") instanceof List<?> l) || l.isEmpty()))
				throw iaex("%s view '%s' requires a card or at least one section.", who, vid);
			var sectionIds = new HashSet<String>();
			if (hasCard) {
				checkCard(who, vid, v.get("card"), cardIds);
			} else {
				for (var so : (List<?>)v.get("sections")) {
					if (! (so instanceof Map<?,?> s))
						throw iaex("%s view '%s' sections must be objects.", who, vid);
					var sid = String.valueOf(s.get("id"));
					if (! (s.get("id") instanceof String) || ! ID_GRAMMAR.matcher(sid).matches())
						throw iaex("%s view '%s' section id '%s' must match %s.", who, vid, sid, ID_GRAMMAR.pattern());
					if (! sectionIds.add(sid))
						throw iaex("%s view '%s' section id '%s' is declared more than once.", who, vid, sid);
					if (! (s.get("label") instanceof String))
						throw iaex("%s view '%s' section '%s' requires a string label.", who, vid, sid);
					checkCard(who, vid, s.get("card"), cardIds);
				}
			}
			var is = v.get("initialSection");
			if (is != null && ! sectionIds.contains(is))
				throw iaex("%s view '%s' initialSection '%s' names no section.", who, vid, is);
		}
		var iv = body.get("initialView");
		if (iv != null && ! viewIds.contains(iv))
			throw iaex("%s initialView '%s' names no view.", who, iv);
	}

	private static void checkCard(String who, String vid, Object card, Set<String> cardIds) {
		if (! (card instanceof Map<?,?> c) || ! (c.get("id") instanceof String cid) || ! (c.get("type") instanceof String type))
			throw iaex("%s view '%s' cards must be objects with a string id and type.", who, vid);
		if ("switcher".equals(type))
			throw iaex("%s view '%s' cannot hold a switcher card ('%s').", who, vid, cid);
		if (! cardIds.add(cid))
			throw iaex("%s card id '%s' is declared more than once.", who, cid);
	}
}
