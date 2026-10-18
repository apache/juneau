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

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;

/**
 * A section of a dashboard {@link Rail}.
 *
 * <p>
 * Every section except a {@link CardSection} owns one {@code filter:<key>} topic, set with {@link #publishes(String)}.
 * The key is the section's id and its bus owner id.  The section publishes a filter-shaped payload
 * ({@code {schemaVersion:1, viewId, search?, columns?, options?}}) whenever the user changes it.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Rail.<jsm>left</jsm>().sections(
 * 		SearchSection.<jsm>create</jsm>().publishes(Topics.<jsm>filter</jsm>(<js>"search"</js>)),
 * 		Section.<jsm>custom</jsm>(<js>"app.goto"</js>, JsonMap.<jsm>of</jsm>()).publishes(Topics.<jsm>filter</jsm>(<js>"goto"</js>)));
 * </p>
 *
 * @since 10.0.0
 */
public abstract class Section {

	String topic;
	String label;

	Section() {}

	/**
	 * Sets the topic this section owns.
	 *
	 * @param topic A {@code filter:<key>} topic, normally {@code Topics.filter(key)}.  {@code <key>} becomes the section id.
	 * @return This object.
	 */
	public Section publishes(String topic) {
		this.topic = topic;
		return this;
	}

	/**
	 * Sets the section's legend text.
	 *
	 * @param label The label.
	 * @return This object.
	 */
	public Section label(String label) {
		this.label = label;
		return this;
	}

	/**
	 * Creates a section implemented in JS by a {@code JuneauDashboard.section.register(name, ...)} section.
	 *
	 * @param name The dotted section name, for example {@code app.goto}.
	 * @param args Arguments handed to the section's {@code mount} as {@code ctx.args}.
	 * @return A new section.
	 */
	public static Custom custom(String name, JsonMap args) {
		return new Custom(name, args);
	}

	/** @return The section id (the key of its topic), or <jk>null</jk> before {@link #publishes(String)} or for a card section. */
	String id() {
		return topic == null ? null : topic.substring(topic.indexOf(':') + 1);
	}

	/** @return The {@code kind} value of the section JSON. */
	abstract String kind();

	/** @return A human name for messages, for example {@code "range section"}. */
	String what() {
		return kind() + " section";
	}

	/** Adds the kind-specific fields. */
	abstract void body(Map<String,Object> m);

	/** @return The cards this section adds to the page (only a card section has one). */
	List<CardSpec> cards() {
		return List.of();
	}

	/**
	 * Checks this section, reporting E-77 under {@code dash}.
	 *
	 * @param dash The dashboard id.
	 */
	void check(String dash) {
		if (topic == null)
			throw e77(dash, "a " + what() + " must call publishes(Topics.filter(...))");
		if (! topic.startsWith("filter:"))
			throw e77(dash, "section topic '" + topic + "' is not a filter: topic");
		if (! Topics.ID.matcher(id()).matches())
			throw e77(dash, "section id '" + id() + "' is not a valid id");
	}

	/** @return The section JSON. */
	Map<String,Object> toMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("id", id());
		m.put("kind", kind());
		m.put("topic", topic);
		if (label != null)
			m.put("label", label);
		body(m);
		return m;
	}

	/**
	 * Builds an E-77 exception.
	 *
	 * @param dash The dashboard id.
	 * @param detail The problem.  A format argument, so a {@code %} in it is harmless.
	 * @return The exception, for the caller to throw.
	 */
	static IllegalArgumentException e77(String dash, String detail) {
		return new IllegalArgumentException("E-77: " + String.format(BusWiringValidator.E77, dash, detail));
	}

	/**
	 * Rejects a bare card as a dashboard member: the shell renders a bare card without a host element to place.
	 *
	 * @param dash The dashboard id.
	 * @param card The panel or card-section card.
	 */
	static void checkMemberCard(String dash, CardSpec card) {
		if (Boolean.TRUE.equals(card.toMap().get("bare")))
			throw e77(dash, "card '" + card.id() + "' is bare; a dashboard member needs a card host");
	}

	/**
	 * Checks a card the dashboard itself adds to the page: {@link #checkMemberCard}, plus the types that
	 * {@code PageSpec.card} refuses because they have dedicated methods.
	 *
	 * @param dash The dashboard id.
	 * @param card The card.
	 */
	static void checkOwnedCard(String dash, CardSpec card) {
		if ("html".equals(card.type()) || "datatables".equals(card.type()))
			throw e77(dash, "card '" + card.id() + "' has type '" + card.type() + "', which has a dedicated PageSpec method; add it with PageSpec."
				+ ("html".equals(card.type()) ? "html" : "table") + "(...) and use panel(\"" + card.id() + "\")");
		checkMemberCard(dash, card);
	}

	/** @return <jk>true</jk> if {@code name} is a dotted extension name (the E-74 shape). */
	@SuppressWarnings({
		"java:S2692" // A leading dot is deliberately invalid, so indexOf must be > 0, not >= 0.
	})
	static boolean isDottedName(String name) {
		return name != null && name.indexOf('.') > 0 && BusWiringValidator.EXTENSION_NAME.matcher(name).matches();
	}

	/**
	 * A section implemented in JS.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Section.<jsm>custom</jsm>(<js>"app.goto"</js>, JsonMap.<jsm>of</jsm>()).publishes(Topics.<jsm>filter</jsm>(<js>"goto"</js>));
	 * </p>
	 *
	 * @since 10.0.0
	 */
	public static final class Custom extends Section {

		private final String name;
		private final JsonMap args;

		Custom(String name, JsonMap args) {
			this.name = name;
			this.args = args == null ? JsonMap.of() : new JsonMap(args);
		}

		@Override /* Section */
		public Custom publishes(String topic) {
			super.publishes(topic);
			return this;
		}

		@Override /* Section */
		public Custom label(String label) {
			super.label(label);
			return this;
		}

		@Override String kind() { return "custom"; }

		@Override void check(String dash) {
			if (! isDottedName(name))
				throw e77(dash, "custom section name '" + name + "' is not a dotted extension name");
			super.check(dash);
		}

		@Override void body(Map<String,Object> m) {
			m.put("name", name);
			m.put("args", args);
		}
	}
}
