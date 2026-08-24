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

/**
 * A single card in a {@link CardGrid}: a stable id, a header title, and exactly one {@link CardBody} slot.
 *
 * <p>
 * The refresh wire ({@code refreshEndpoint}/{@code pollIntervalMs}) is <b>not</b> on {@code Card} &mdash; it lives on
 * the refreshable body ({@link CardFieldList}) so a non-refreshable body cannot carry a dangling refresh endpoint.
 * There is <b>no</b> per-card action catalog in v1: a refresh affordance is a built-in of a refreshable card.
 *
 * @since 10.0.0
 */
public class Card {

	/** The stable card id, unique within its grid.  Required, non-blank. */
	public String id;

	/** The card header text, painted as {@code textContent}.  Required, non-blank. */
	public String title;

	/** The single body slot.  Required. */
	public CardBody body;

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
	 * Fail-closed bean validation; fans out to {@link CardBody#validate()}.
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
	}
}
