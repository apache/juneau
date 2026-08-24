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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Builds the server-rendered html5 delivery tree for a {@link CardGrid} &mdash; the responsive
 * {@code data-juneau-card-grid} dashboard of {@code data-juneau-card} articles the {@code juneau-cards.js} runtime
 * enhances.  The direct analogue of {@link ViewTable#of(ViewDef)}: a {@code @RestGet} returns
 * {@code CardGridTable.of(grid)} and the returned {@link Section} is serialized as the page body.
 *
 * <p>
 * The card-layout beans live in {@code juneau-rest-server-widgets}; this emitter (and the {@code juneau-cards.js}
 * client runtime, served by {@link ViewsMixin}) is the only place that turns them into markup.  It calls
 * {@link CardGrid#validate()} on entry, so a caller can never serialize an ill-formed grid.
 *
 * <h5 class='section'>Escaping contract (security-critical):</h5>
 * <p>
 * Unlike {@link ViewTable}, a card grid carries <b>no JSON sidecar</b> &mdash; a refreshable card re-fills its
 * {@code [data-juneau-card-field]} slots from a data-only GET at runtime, so there is no {@code <script>} payload to
 * neutralize here.  Every human string ({@link CardGrid#title}, {@link Card#title}, {@link CardField#label}, and the
 * server-painted initial {@link CardField#value}) is emitted as an html5 <b>text child</b>, which the serializer
 * entity-escapes.  A {@code value} of {@code <script>alert(1)</script>} therefore paints as inert text inside its
 * {@code <dd>}; it can never become a live tag.  The initial value is server-painted so a static field-list shows its
 * data with JavaScript disabled.
 *
 * <h5 class='section'>Closed v1 body dispatch (fail-closed):</h5>
 * <p>
 * The body dispatch is a <b>closed</b> set: v1 emits {@link CardFieldList} only.  Any other {@link CardBody}
 * implementation throws at emit rather than being silently dropped &mdash; a later child that adds its own body type
 * patches this dispatch and brings its own data path; it does not reuse the {@code CardFieldList} envelope.
 *
 * <h5 class='section'>Refresh wiring:</h5>
 * <p>
 * The {@code data-juneau-card-contract}, {@code data-juneau-card-refresh}, and {@code data-juneau-card-poll-ms}
 * attributes (and the header status chip + built-in refresh button) are stamped <b>per refreshable card</b> &mdash;
 * only when the card's body is a {@link CardFieldList} carrying a {@code refreshEndpoint}.  The refresh path is
 * validated same-origin and non-templated by {@link CardFieldList#validate()}; a poll interval is Java-clamped there
 * to the shared commons floor.  A static-only card carries none of these attributes, so it can never hold a dangling
 * refresh wire.  The refresh button's glyph is filled at runtime by {@code juneau-cards.js} via the
 * {@code juneau-icons.js} {@code refresh} icon (hence the {@code juneau-icons.js} &rarr; {@code juneau-cards.js} load
 * order) &mdash; the contract attribute value is {@link CardFieldList#CONTRACT_VERSION}, never a view/grid version.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link CardGrid}
 * 	<li class='jc'>{@link ViewTable}
 * </ul>
 *
 * @since 10.0.0
 */
public class CardGridTable {

	/** Marker attribute on the grid {@code <section>} the {@code juneau-cards.js} runtime scans from. */
	public static final String GRID_MARKER = "data-juneau-card-grid";

	/** Attribute carrying the {@link CardGrid#id} on the grid {@code <section>}. */
	public static final String GRID_ID_ATTR = "data-juneau-card-grid-id";

	/** Marker attribute on each card {@code <article>}. */
	public static final String CARD_MARKER = "data-juneau-card";

	/** Attribute carrying the {@link Card#id} on each card {@code <article>}. */
	public static final String CARD_ID_ATTR = "data-juneau-card-id";

	/**
	 * Attribute carrying {@link CardFieldList#CONTRACT_VERSION} &mdash; stamped only on a refreshable card (S4), so a
	 * mixed grid can hold both static and refreshable cards without a grid-wide version.
	 */
	public static final String CARD_CONTRACT_ATTR = "data-juneau-card-contract";

	/** Attribute carrying the same-origin, non-templated refresh path &mdash; refreshable cards only. */
	public static final String CARD_REFRESH_ATTR = "data-juneau-card-refresh";

	/** Attribute carrying the Java-clamped auto-refresh interval (ms) &mdash; present only when a poll is declared. */
	public static final String CARD_POLL_ATTR = "data-juneau-card-poll-ms";

	/** Marker attribute on the header staleness/error status chip &mdash; refreshable cards only. */
	public static final String CARD_STATUS_ATTR = "data-juneau-card-status";

	/** Marker attribute on the per-card contract-mismatch/error banner host ({@code <div role="alert">}). */
	public static final String CARD_BANNER_ATTR = "data-juneau-card-banner";

	/** Marker attribute on the card body wrapper. */
	public static final String CARD_BODY_ATTR = "data-juneau-card-body";

	/** Attribute carrying a {@link CardField#data} fill key on each {@code <dd>} slot. */
	public static final String CARD_FIELD_ATTR = "data-juneau-card-field";

	/** Marker attribute on the built-in refresh {@code <button>} (glyph filled at runtime from {@code juneau-icons.js}). */
	public static final String CARD_REFRESH_TRIGGER_ATTR = "data-juneau-card-refresh-trigger";

	private CardGridTable() {}

	/**
	 * Builds the html5 delivery tree for the given card grid.
	 *
	 * @param grid The built card grid.  Must not be <jk>null</jk>.
	 * @return A new {@link Section} carrying the {@code data-juneau-card-grid} dashboard.
	 * @throws IllegalArgumentException If {@code grid} is <jk>null</jk>, fails {@link CardGrid#validate()}, or carries
	 * 	a {@link CardBody} type this v1 emitter does not know how to render.
	 */
	public static Section of(CardGrid grid) {
		if (grid == null)
			throw iaex("grid must not be null.");
		grid.validate();

		var children = l();
		if (grid.title != null && ! grid.title.isBlank())
			children.add(h2(grid.title).class_("juneau-view-card-grid-title"));
		for (var card : grid.cards)
			children.add(emitCard(grid.id, card));

		var section = section(children.toArray())
			.class_("juneau-view-card-grid")
			.attr(GRID_MARKER, "1")
			.attr(GRID_ID_ATTR, grid.id);
		// Optional min-card-width hint as an inline custom property; omitted when unset so the CSS fallback wins (N3).
		if (grid.minCardPx != null)
			section.attr("style", "--jc-card-min:" + grid.minCardPx + "px");
		return section;
	}

	/** Emits one card {@code <article>}; refresh wiring is stamped only for a refreshable {@link CardFieldList} body. */
	private static Article emitCard(String gridId, Card card) {
		var titleId = gridId + "-" + card.id + "-title";
		var body = emitBody(card.body);   // closed dispatch: throws (fail-closed) on an unknown CardBody

		var fl = card.body instanceof CardFieldList x ? x : null;
		var refreshable = fl != null && fl.refreshEndpoint != null && ! fl.refreshEndpoint.isBlank();

		var headerKids = l();
		headerKids.add(span(card.title).class_("juneau-view-card-title").id(titleId));
		if (refreshable)
			headerKids.add(span().class_("juneau-view-card-status").attr(CARD_STATUS_ATTR, "1").hidden(true));
		var actionKids = l();
		if (refreshable)
			actionKids.add(button("button")
				.class_("juneau-view-card-refresh")
				.attr(CARD_REFRESH_TRIGGER_ATTR, "1")
				.attr("aria-label", "Refresh")
				.attr("title", "Refresh"));
		headerKids.add(div(actionKids.toArray()).class_("juneau-view-card-actions"));

		var header = header(headerKids.toArray()).class_("juneau-view-card-header");
		var banner = div().class_("juneau-view-card-banner").attr(CARD_BANNER_ATTR, "1").attr("role", "alert").hidden(true);
		var bodyWrap = div(body).class_("juneau-view-card-body").attr(CARD_BODY_ATTR, "1");

		var article = article(header, banner, bodyWrap)
			.class_("juneau-view-card")
			.attr(CARD_MARKER, "1")
			.attr(CARD_ID_ATTR, card.id)
			.attr("aria-labelledby", titleId);
		if (refreshable) {
			article.attr(CARD_CONTRACT_ATTR, CardFieldList.CONTRACT_VERSION);
			article.attr(CARD_REFRESH_ATTR, fl.refreshEndpoint);
			if (fl.pollIntervalMs != null)
				article.attr(CARD_POLL_ATTR, fl.pollIntervalMs.toString());
		}
		return article;
	}

	/** Closed v1 body dispatch: {@link CardFieldList} only; any other {@link CardBody} fails closed at emit (B2/S8). */
	private static HtmlElement emitBody(CardBody body) {
		if (body instanceof CardFieldList fl)
			return emitFieldList(fl);
		throw iaex("CardGridTable does not know how to emit CardBody type '%s'; v1 emits CardFieldList only.",
			body == null ? "null" : body.getClass().getName());
	}

	/**
	 * Emits a {@link CardFieldList} as a {@code <dl>} whose grid items are per-field wrapper {@code <div>}s (so each
	 * {@code dt}/{@code dd} pair stays coupled under CSS grid, N5); {@code columns} is stamped as an inline
	 * {@code grid-template-columns}.  Each initial value is a server-painted, entity-escaped text child.
	 */
	private static Dl emitFieldList(CardFieldList fl) {
		var items = l();
		for (var f : fl.fields)
			items.add(div(
				dt(f.label == null ? "" : f.label),
				dd(f.value == null ? "" : f.value).attr(CARD_FIELD_ATTR, f.data)
			).class_("juneau-view-card-field"));
		return dl(items.toArray())
			.class_("juneau-view-card-fields")
			.attr("style", "grid-template-columns:repeat(" + fl.columns + ",minmax(0,1fr))");
	}
}
