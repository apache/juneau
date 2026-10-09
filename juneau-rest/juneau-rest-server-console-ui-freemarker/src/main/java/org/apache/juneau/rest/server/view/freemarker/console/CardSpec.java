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
import org.apache.juneau.rest.server.views.*;

/**
 * Type-agnostic card entry in the page contract: {@code id}, {@code type}, optional {@code src},
 * {@code title}, {@code template}, plus a type-specific body map. C2 extends the card grammar.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	CardSpec <jv>c</jv> = CardSpec.<jsm>html</jsm>(<js>"intro"</js>).title(<js>"Welcome"</js>).template(<js>"intro"</js>);
 * 	CardSpec <jv>t</jv> = CardSpec.<jsm>of</jsm>(<js>"datatables"</js>, <js>"releases"</js>)
 * 		.body(<js>"table"</js>, <js>"/rest/releases/data"</js>);
 * 	PageCapture.<jsm>of</jsm>(<jv>env</jv>).addCard(<jv>c</jv>, <js>"&lt;p&gt;Hello&lt;/p&gt;"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class CardSpec {

	private final String type;
	private final String id;
	private String title;
	private String src;
	private String template;
	private boolean bare;
	private String cssClass;
	private final Map<String,Object> body = new LinkedHashMap<>();
	private final List<Map<String,Object>> publishes = new ArrayList<>(), subscribes = new ArrayList<>();
	private List<VisibilityRule> visibleWhen = List.of();

	private CardSpec(String type, String id) {
		this.type = type;
		this.id = id;
	}

	/** @param id The card id. @return A new {@code html} card. */
	public static CardSpec html(String id) {
		return new CardSpec("html", id);
	}

	/** @param type The card type. @param id The card id. @return A new card. */
	public static CardSpec of(String type, String id) {
		return new CardSpec(type, id);
	}

	/** @param v The card title. @return This object. */
	public CardSpec title(String v) { title = v; return this; }

	/** @param v A same-origin URL the card loads. @return This object. */
	public CardSpec src(String v) { src = v; return this; }

	/** @param templateId The id of a {@code <template data-card>} element. @return This object. */
	public CardSpec template(String templateId) { template = templateId; return this; }

	/** @param v Whether an html card is inserted without the {@code .jc-card} wrapper. @return This object. */
	public CardSpec bare(boolean v) { bare = v; return this; }

	/** @param v Space-separated CSS class names stamped on the card wrapper element. @return This object. */
	public CardSpec cssClass(String v) { cssClass = v; return this; }

	/** @param key A type-specific field. @param value Its value. @return This object. */
	public CardSpec body(String key, Object value) {
		if ("publishes".equals(key) || "subscribes".equals(key))
			throw iaex("card '%s': '%s' is a base key; use %s(...)", id, key, key);
		body.put(key, value);
		return this;
	}

	/**
	 * Declares custom topics this card's JS publishes; repeatable, appends.
	 *
	 * @param v The declarations; each needs {@code retain} and must not set {@code publisher}.
	 * @return This object.
	 */
	public CardSpec publishes(TopicDecl... v) {
		for (var d : v)
			publishes.add(d.toPublicationMap());
		return this;
	}

	/**
	 * Sets card-level visibility rules.  The client evaluates them against the page's facts; this only serializes them.
	 *
	 * @param rules The rules, ANDed together.  None clears them.
	 * @return This object.
	 */
	public CardSpec visibleWhen(VisibilityRule... rules) {
		if (rules == null || Arrays.stream(rules).anyMatch(Objects::isNull))
			throw iaex("CardSpec visibleWhen rules must not be null.");
		visibleWhen = List.of(rules);
		return this;
	}

	/**
	 * Wires topics to roles this card's type implements; repeatable, appends.
	 *
	 * @param v The subscriptions.
	 * @return This object.
	 */
	public CardSpec subscribes(Subscription... v) {
		for (var s : v)
			subscribes.add(s.toMap());
		return this;
	}

	/** @return The id. */
	public String id() { return id; }

	/** @return The type. */
	public String type() { return type; }

	/** @return An independent copy of this card, safe to mutate without affecting this one. */
	CardSpec copy() {
		var c = new CardSpec(type, id);
		c.title = title;
		c.src = src;
		c.template = template;
		c.bare = bare;
		c.cssClass = cssClass;
		c.body.putAll(body);
		return c.wiringFrom(this);
	}

	/** Wiring already lowered to contract maps (FTL attributes or a JSON5 body). */
	CardSpec wiring(List<?> publishesMaps, List<?> subscribesMaps) {
		if (publishesMaps != null) for (var o : publishesMaps) publishes.add(asMap(o));
		if (subscribesMaps != null) for (var o : subscribesMaps) subscribes.add(asMap(o));
		return this;
	}

	/** @param other The card whose {@code publishes} / {@code subscribes} are appended to this one's, and whose {@code visibleWhen} replaces (does not merge with) this one's when it has any. @return This object. */
	CardSpec wiringFrom(CardSpec other) {
		publishes.addAll(other.publishes);
		subscribes.addAll(other.subscribes);
		if (! other.visibleWhen.isEmpty())
			visibleWhen = other.visibleWhen;
		return this;
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> asMap(Object o) {
		if (! (o instanceof Map))
			throw new IllegalArgumentException("card wiring entries must be objects; got " + (o == null ? "null" : o.getClass().getSimpleName()) + ".");
		return (Map<String,Object>)o;
	}

	/** @return The template id, or <jk>null</jk>. */
	String template() { return template; }

	/** @return The title, or <jk>null</jk>. */
	String title() { return title; }

	/** @return The {@code src} URL, or <jk>null</jk>. */
	String src() { return src; }

	/** @return The wrapper CSS classes, or <jk>null</jk>. */
	String cssClass() { return cssClass; }

	/** @return The contract entry, common fields first. */
	public Map<String,Object> toMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("id", id);
		m.put("type", type);
		if (nn(title)) m.put("title", title);
		if (nn(src)) m.put("src", src);
		if (nn(template)) m.put("template", template);
		if (bare) m.put("bare", true);
		if (nn(cssClass)) m.put("class", cssClass);
		if (! visibleWhen.isEmpty()) m.put("visibleWhen", visibleWhen.stream().map(VisibilityRule::toMap).toList());
		if (! publishes.isEmpty()) m.put("publishes", List.copyOf(publishes));
		if (! subscribes.isEmpty()) m.put("subscribes", List.copyOf(subscribes));
		m.putAll(body);
		return m;
	}
}
