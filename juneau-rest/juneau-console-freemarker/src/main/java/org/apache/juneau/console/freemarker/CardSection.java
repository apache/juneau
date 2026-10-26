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

/**
 * A rail section holding one ordinary card.
 *
 * <p>
 * The card publishes what it publishes; the section owns no topic.  The card is added to the page by
 * {@code PageSpec.dashboard(...)}.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	CardSection.<jsm>of</jsm>(ChartDef.<jsm>donut</jsm>(<js>"share"</js>).src(<js>"/stats/share"</js>).label(<js>"reason"</js>).value(<js>"count"</js>));
 * </p>
 *
 * @since 10.0.0
 */
public final class CardSection extends Section {

	private final CardSpec card;
	private boolean published;

	private CardSection(CardSpec card) {
		this.card = card;
	}

	/**
	 * Creates a card section from a chart.
	 *
	 * @param chart The chart.
	 * @return A new section.
	 */
	public static CardSection of(ChartDef chart) {
		return new CardSection(Objects.requireNonNull(chart, "chart must not be null").toCardSpec());
	}

	/**
	 * Creates a card section from any card.
	 *
	 * @param card The card.  It must not be bare.
	 * @return A new section.
	 */
	public static CardSection of(CardSpec card) {
		return new CardSection(Objects.requireNonNull(card, "card must not be null"));
	}

	/**
	 * Not allowed: a card section publishes through its card.  Calling this makes the dashboard fail with E-77.
	 *
	 * @param topic Ignored.
	 * @return This object.
	 */
	@Override /* Section */
	public CardSection publishes(String topic) {
		published = true;
		return this;
	}

	@Override /* Section */
	public CardSection label(String label) {
		super.label(label);
		return this;
	}

	@Override String kind() { return "card"; }

	@Override String id() {
		return null;
	}

	@Override List<CardSpec> cards() {
		return List.of(card);
	}

	@Override void check(String dash) {
		if (published)
			throw e77(dash, "card section '" + card.id() + "' publishes through its card; remove publishes(...)");
		checkOwnedCard(dash, card);
	}

	@Override Map<String,Object> toMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("kind", "card");
		m.put("card", card.id());
		if (label != null)
			m.put("label", label);
		return m;
	}

	@Override void body(Map<String,Object> m) {
		// Unused: toMap is overridden.
	}
}
