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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * A responsive dashboard grid of {@link Card}s &mdash; the top-level card-layout widget.
 *
 * <p>
 * A pure bean, emitted by {@code CardGridTable.of(CardGrid)} in the views module (the beans live here; the html5
 * emitter and the {@code juneau-cards.js} client runtime live in {@code juneau-rest-server-views}, served by
 * {@code ViewsMixin}).
 *
 * @since 10.0.0
 */
public class CardGrid implements Widget {

	/** The frozen contract version for this widget. */
	public static final String CONTRACT_VERSION = "1";

	/** The stable grid id (hash/anchor + sidecar key).  Required, non-blank. */
	public String id;

	/** Optional grid heading, painted as {@code textContent}. */
	public String title;

	/** The cards, in display order.  At least one is required; card ids must be unique within the grid. */
	public List<Card> cards;

	/**
	 * Optional minimum card-width hint (CSS pixels), emitted as {@code --jc-card-min}.  When set must be
	 * {@code >= 1}; when unset the emitter omits the inline style and the CSS fallback wins.
	 */
	public Integer minCardPx;

	/**
	 * Creates an empty grid with the given id.
	 *
	 * @param id The stable grid id.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link CardGrid}.
	 */
	public static CardGrid create(String id) {
		var g = new CardGrid();
		g.id = id;
		return g;
	}

	/**
	 * Sets the grid heading.
	 *
	 * @param value The heading text.
	 * @return This object.
	 */
	public CardGrid title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets the cards, in display order.
	 *
	 * @param value The cards.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public CardGrid cards(Card...value) {
		cards = l(value);
		return this;
	}

	/**
	 * Sets the minimum card-width hint.
	 *
	 * @param value The minimum card width, in CSS pixels.  Must be {@code >= 1}.
	 * @return This object.
	 */
	public CardGrid minCardPx(int value) {
		minCardPx = value;
		return this;
	}

	@Override /* Widget */
	public void validate() {
		if (id == null || id.isBlank())
			throw iaex("CardGrid id must not be null or blank.");
		if (cards == null || cards.isEmpty())
			throw iaex("CardGrid must declare at least one card.");
		if (minCardPx != null && minCardPx < 1)
			throw iaex("CardGrid minCardPx must be >= 1 when set.");
		var ids = new HashSet<String>();
		for (var c : cards) {
			if (c == null)
				throw iaex("CardGrid card must not be null.");
			c.validate();
			if (!ids.add(c.id))
				throw iaex("CardGrid duplicate card id '%s'.", c.id);
		}
	}
}
