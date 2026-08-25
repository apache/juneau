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
 * A single card in a {@link CardGrid}: a stable id, a header title, exactly one {@link CardBody} slot, and an
 * optional per-card action catalog.
 *
 * <p>
 * The refresh wire ({@code refreshEndpoint}/{@code pollIntervalMs}) is <b>not</b> on {@code Card} &mdash; it lives on
 * the refreshable body ({@link CardFieldList}) so a non-refreshable body cannot carry a dangling refresh endpoint.
 * The built-in refresh affordance of a refreshable card is likewise not an entry in {@link #actions}; it is emitted
 * by the card emitter and the two coexist in the same header action row.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class Card {

	/** The stable card id, unique within its grid.  Required, non-blank. */
	public String id;

	/** The card header text, painted as {@code textContent}.  Required, non-blank. */
	public String title;

	/** The single body slot.  Required. */
	public CardBody body;

	/**
	 * Optional per-card actions, in display order; action ids must be unique within the card.
	 *
	 * <p>
	 * Typed as the same {@link HeaderAction} vocabulary the app header uses, because that is the vocabulary that
	 * already carries a {@link Behavior}, an icon, an accessible tooltip, an href, a badge and an attached menu
	 * &mdash; everything a card action needs and nothing it does not.  Note this is deliberately <b>not</b> the
	 * view's {@link ActionBar} row-action vocabulary: an {@link ActionRef} resolves against a view's
	 * {@code rowActions} catalog, which a card does not have, and {@link SafeAction#COLLAPSE} has no meaning on a
	 * card.
	 *
	 * <p>
	 * A {@link Behavior#MENU} action's attached list is opened on the shared client layer stack, so a card menu
	 * shares one popup owner with every other menu and dialog on the page.
	 */
	public List<HeaderAction> actions;

	/**
	 * Creates a card with the given id and title.
	 *
	 * @param id The stable card id.  Must not be <jk>null</jk> or blank.
	 * @param title The card header text.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link Card}.
	 */
	public static Card create(String id, String title) {
		var c = new Card();
		c.id = id;
		c.title = title;
		return c;
	}

	/**
	 * Sets the body slot.
	 *
	 * @param value The card body.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Card body(CardBody value) {
		body = value;
		return this;
	}

	/**
	 * Sets the per-card actions, in display order.
	 *
	 * @param value The actions.  Passing none leaves the card with an empty catalog, which is not an error.
	 * @return This object.
	 */
	public Card actions(HeaderAction...value) {
		actions = l(value);
		return this;
	}

	/**
	 * Fail-closed bean validation; fans out to {@link CardBody#validate()} and to each
	 * {@link HeaderAction#validate()}.
	 *
	 * @throws IllegalArgumentException If this card is not well-formed.
	 */
	public void validate() {
		if (id == null || id.isBlank())
			throw iaex("Card id must not be null or blank.");
		if (title == null || title.isBlank())
			throw iaex("Card '%s' title must not be null or blank.", id);
		if (body == null)
			throw iaex("Card '%s' must declare a body.", id);
		body.validate();
		if (actions != null) {
			var ids = new HashSet<String>();
			for (var a : actions) {
				if (a == null)
					throw iaex("Card '%s' action must not be null.", id);
				a.validate();
				if (!ids.add(a.id))
					throw iaex("Card '%s' duplicate action id '%s'.", id, a.id);
			}
		}
	}
}
