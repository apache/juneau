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

import java.util.*;

import jakarta.servlet.http.*;

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
 * <h5 class='section'>Closed body dispatch (fail-closed):</h5>
 * <p>
 * The body dispatch is a <b>closed</b>, enumerated set: {@link CardFieldList} and {@link ViewCardBody}.  Any other
 * {@link CardBody} implementation throws at emit rather than being silently dropped, so a body type nobody taught
 * this emitter about can never reach the page.  In particular there is no raw-markup body: the toolkit has no
 * markup sanitizer, so a body that would pour author markup into a card is refused here rather than emitted
 * un-neutralized.  Each known body brings its <b>own</b> data path &mdash; {@link ViewCardBody} inherits the hosted
 * table's ajax/refresh and does not reuse the {@code CardFieldList} refresh envelope.
 *
 * <h5 class='section'>Request-aware emit:</h5>
 * <p>
 * {@link #of(CardGrid)} is request-free: it embeds no CSRF token and resolves no server values, and therefore
 * <b>fails closed</b> on a {@link ViewCardBody}, whose hosted table needs a request for both.  A card that hosts a
 * view is emitted by {@link #of(HttpServletRequest,Card)} instead.  There is deliberately no request-aware grid
 * entry point in the public API: a request-aware grid is assembled inside this package, walking its cards through
 * the grid-qualified card overload so each hosted table's DOM identity can be qualified by both the grid and the
 * card.
 *
 * <h5 class='section'>Per-card actions:</h5>
 * <p>
 * {@link Card#actions} are emitted into the same header action row as the built-in refresh button, using the same
 * {@code data-juneau-header-action} / {@code data-juneau-behavior} DOM vocabulary the app-header actions use (they
 * are the same {@link HeaderAction} bean), so one client enhancement contract covers both.  A
 * {@link Behavior#MENU} action's attached list is emitted as a hidden, card-scoped sibling that the card runtime
 * opens on the shared layer stack.
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
	 * Builds the html5 delivery tree for the given card grid, without a request.
	 *
	 * <p>
	 * Embeds no CSRF token and resolves no server values, so a {@link ViewCardBody} &mdash; whose hosted table needs
	 * both &mdash; <b>fails closed</b> here.  Use {@link #of(HttpServletRequest,Card)} per card when a card hosts a
	 * view.
	 *
	 * @param grid The built card grid.  Must not be <jk>null</jk>.
	 * @return A new {@link Section} carrying the {@code data-juneau-card-grid} dashboard.
	 * @throws IllegalArgumentException If {@code grid} is <jk>null</jk>, fails {@link CardGrid#validate()}, or carries
	 * 	a {@link CardBody} type this emitter does not know how to render (or one that requires a request).
	 */
	public static Section of(CardGrid grid) {
		return emitGrid(grid, null);
	}

	/**
	 * Builds the html5 delivery tree for the given card grid, handing the request down to every card so a hosted
	 * view table receives it.
	 *
	 * <p>
	 * Package-private on purpose: the public request-aware surface is the per-card
	 * {@link #of(HttpServletRequest,Card)} overload, and adding a public grid counterpart would give callers two
	 * ways to say the same thing with different id-minting behavior.  This assembler walks the grid's cards through
	 * {@link #of(HttpServletRequest,Card,String)} so it can pass the grid id it alone knows.
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @param grid The built card grid.  Must not be <jk>null</jk>.
	 * @return A new {@link Section} carrying the {@code data-juneau-card-grid} dashboard.
	 * @throws IllegalArgumentException If either argument is <jk>null</jk>, the grid fails
	 * 	{@link CardGrid#validate()}, or a card carries an unknown {@link CardBody} type.
	 */
	static Section ofGrid(HttpServletRequest req, CardGrid grid) {
		if (req == null)
			throw iaex("req must not be null; use of(CardGrid) for request-free emit.");
		return emitGrid(grid, req);
	}

	/**
	 * Builds the html5 delivery tree for a single card, handing the request down to its body.
	 *
	 * <p>
	 * The request-aware entry point: a {@link ViewCardBody}'s hosted table is emitted through
	 * {@link ViewTable#of(HttpServletRequest,ViewDef)}, so it arrives with the response's CSRF token embedded and
	 * its declared server values resolved.  An ordinary {@link CardFieldList} body renders here exactly as it does
	 * on the grid path, refresh wire and all.
	 *
	 * <p>
	 * The card's DOM identity &mdash; a hosted table's html {@code id} and sidecar ids, and a menu action's list id
	 * &mdash; is qualified by the card id, so the same authored view or action id can appear in more than one card.
	 * This overload takes no grid id and therefore omits it from that qualifier; a caller assembling a whole grid
	 * inside this package uses {@link #of(HttpServletRequest,Card,String)} to include it.
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @param card The built card.  Must not be <jk>null</jk>.
	 * @return A new {@link Article} carrying the {@code data-juneau-card} card.
	 * @throws IllegalArgumentException If either argument is <jk>null</jk>, the card fails {@link Card#validate()},
	 * 	or it carries a {@link CardBody} type this emitter does not know how to render.
	 */
	public static Article of(HttpServletRequest req, Card card) {
		return of(req, card, null);
	}

	/**
	 * Builds the html5 delivery tree for a single card whose DOM identity is qualified by its enclosing grid as
	 * well as by itself.
	 *
	 * <p>
	 * The grid-qualified counterpart of {@link #of(HttpServletRequest,Card)}, package-private because only a
	 * grid-level assembler knows the grid id.  A hosted table mints
	 * {@code <gridId>:<cardId>:<viewId>} (and its sidecars take the usual prefixes over that), and a menu action's
	 * list id is qualified the same way.
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @param card The built card.  Must not be <jk>null</jk>.
	 * @param gridId The enclosing grid id, or <jk>null</jk>/blank to qualify by the card alone.
	 * @return A new {@link Article} carrying the {@code data-juneau-card} card.
	 * @throws IllegalArgumentException If {@code req} or {@code card} is <jk>null</jk>, the card fails
	 * 	{@link Card#validate()}, or it carries a {@link CardBody} type this emitter does not know how to render.
	 */
	static Article of(HttpServletRequest req, Card card, String gridId) {
		if (req == null)
			throw iaex("req must not be null; use of(CardGrid) for request-free emit.");
		if (card == null)
			throw iaex("card must not be null.");
		card.validate();
		return emitCard(gridId, card, req);
	}

	/** The shared grid emit; {@code req} is <jk>null</jk> on the request-free path. */
	private static Section emitGrid(CardGrid grid, HttpServletRequest req) {
		if (grid == null)
			throw iaex("grid must not be null.");
		grid.validate();

		var children = l();
		if (grid.title != null && ! grid.title.isBlank())
			children.add(h2(grid.title).class_("juneau-view-card-grid-title"));
		for (var card : grid.cards)
			children.add(emitCard(grid.id, card, req));

		var section = section(children.toArray())
			.class_("juneau-view-card-grid")
			.attr(GRID_MARKER, "1")
			.attr(GRID_ID_ATTR, grid.id);
		// Optional min-card-width hint as an inline custom property; omitted when unset so the CSS fallback wins (N3).
		if (grid.minCardPx != null)
			section.attr("style", "--jc-card-min:" + grid.minCardPx + "px");
		return section;
	}

	/**
	 * The DOM-identity qualifier for everything a card mints: the grid id and the card id when the grid is known,
	 * the card id alone otherwise.  Two cards can host the same authored view, or declare the same action id,
	 * without colliding on a document-wide element id.
	 */
	private static String idScope(String gridId, String cardId) {
		return gridId == null || gridId.isBlank() ? cardId : gridId + ":" + cardId;
	}

	/**
	 * Emits one card {@code <article>}; refresh wiring is stamped only for a refreshable {@link CardFieldList} body.
	 * {@code req} is <jk>null</jk> on the request-free path, which is what makes a request-needing body fail closed.
	 */
	private static Article emitCard(String gridId, Card card, HttpServletRequest req) {
		var scope = idScope(gridId, card.id);
		var titleId = (gridId == null || gridId.isBlank() ? card.id : gridId + "-" + card.id) + "-title";
		var body = emitBody(card.body, req, scope);   // closed dispatch: throws (fail-closed) on an unknown CardBody

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
		// Declared actions follow the built-in refresh button, each MENU trigger trailed by its own hidden list.
		if (card.actions != null)
			for (var a : card.actions) {
				actionKids.add(emitAction(a, scope));
				if (a.behavior == Behavior.MENU)
					actionKids.add(emitMenu(menuId(scope, a.id), a.menu));
			}
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

	/**
	 * Closed body dispatch: {@link CardFieldList} and {@link ViewCardBody}; any other {@link CardBody} fails closed
	 * at emit.  A {@link ViewCardBody} on the request-free path also fails closed rather than emitting a hosted
	 * table stripped of the token and server values it was declared with.
	 */
	private static HtmlElement<?> emitBody(CardBody body, HttpServletRequest req, String idScope) {
		if (body instanceof CardFieldList fl)
			return emitFieldList(fl);
		if (body instanceof ViewCardBody vcb) {
			if (req == null)
				throw iaex("CardGridTable cannot emit a ViewCardBody without a request: the hosted table needs one "
					+ "for its CSRF token and server-value resolution.  Emit the card with "
					+ "of(HttpServletRequest,Card) instead of of(CardGrid).");
			return ViewTable.of(req, vcb.view, idScope);
		}
		throw iaex("CardGridTable does not know how to emit CardBody type '%s'; the known card bodies are "
			+ "CardFieldList and ViewCardBody.", body == null ? "null" : body.getClass().getName());
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

	/**
	 * Emits one {@link Card#actions} entry, in the same DOM vocabulary an {@link AppHeaderTable} action uses: the
	 * marker + behavior attributes the client enhancement selects on, the tooltip as both {@code aria-label} and
	 * {@code title} (a {@link HeaderAction} has no separate label), and the icon registry name for client
	 * hydration.  Every human string is an attribute value or text child, so the serializer escapes it.
	 */
	private static HtmlElement<?> emitAction(HeaderAction a, String scope) {
		var behavior = a.behavior.name().toLowerCase(Locale.ROOT);
		var inner = l();
		inner.add(span().class_("jc-icon").attr("aria-hidden", "true"));

		if (a.behavior == Behavior.LINK) {
			var link = a(a.href, inner.toArray()).class_("jc-icon-btn juneau-view-card-action")
				.attr(AppHeaderTable.ACTION_MARKER, a.id)
				.attr(AppHeaderTable.BEHAVIOR_ATTR, behavior)
				.attr("aria-label", a.tooltip)
				.attr("title", a.tooltip);
			if (a.icon != null)
				link.attr(AppHeaderTable.ICON_ATTR, a.icon);
			return link;
		}

		var el = button("button", inner.toArray()).class_("jc-icon-btn juneau-view-card-action")
			.attr(AppHeaderTable.ACTION_MARKER, a.id)
			.attr(AppHeaderTable.BEHAVIOR_ATTR, behavior)
			.attr("aria-label", a.tooltip)
			.attr("title", a.tooltip);
		if (a.icon != null)
			el.attr(AppHeaderTable.ICON_ATTR, a.icon);
		if (a.behavior == Behavior.SAFE)
			el.attr(AppHeaderTable.SAFE_ATTR, a.safe);
		if (a.behavior == Behavior.MENU)
			el.attr("aria-haspopup", "menu")
				.attr("aria-expanded", "false")
				.attr("aria-controls", menuId(scope, a.id));
		return el;
	}

	/**
	 * The element id of a card action's attached menu list, qualified by the card (and grid, when known) so two
	 * cards declaring the same action id do not mint the same list id.
	 */
	private static String menuId(String scope, String actionId) {
		return AppHeaderTable.MENU_ID_PREFIX + scope + ":" + actionId;
	}

	/**
	 * Emits a single-level attached menu as a hidden {@code <div class="jc-menu" role="menu">}.  The list ships
	 * {@code display:none} and is inert with JavaScript off; the card runtime portals it onto the shared layer stack
	 * when its trigger opens it.  Every label is a text child, never markup.
	 */
	private static Div emitMenu(String menuId, List<MenuItem> items) {
		var rows = l();
		for (var mi : items)
			rows.add(emitMenuItem(mi));
		return div(rows.toArray()).class_("jc-menu").id(menuId).attr("role", "menu");
	}

	/** Emits one menu row: a divider separator, a link {@code <a href>}, or a SAFE host-dispatch {@code <button>}. */
	private static HtmlElement<?> emitMenuItem(MenuItem mi) {
		if (mi.isDivider())
			return div().class_("jc-menu-divider").attr("role", "separator");

		var kids = l();
		if (mi.icon != null && ! mi.icon.isBlank())
			kids.add(span().class_("jc-icon").attr("aria-hidden", "true"));
		kids.add(mi.label);   // plain text child - serializer entity-escapes it

		if (mi.href != null && ! mi.href.isBlank()) {
			var link = a(mi.href, kids.toArray()).class_("jc-menu-item").attr("role", "menuitem");
			if (mi.icon != null && ! mi.icon.isBlank())
				link.attr(AppHeaderTable.ICON_ATTR, mi.icon);
			return link;
		}
		var el = button("button", kids.toArray()).class_("jc-menu-item")
			.attr("role", "menuitem")
			.attr(AppHeaderTable.SAFE_ATTR, mi.safe);
		if (mi.icon != null && ! mi.icon.isBlank())
			el.attr(AppHeaderTable.ICON_ATTR, mi.icon);
		return el;
	}
}
