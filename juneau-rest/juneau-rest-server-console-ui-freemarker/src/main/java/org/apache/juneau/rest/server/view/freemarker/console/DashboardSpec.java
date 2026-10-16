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

import org.apache.juneau.rest.server.console.*;

/**
 * A dashboard: a rail of filter sections beside a grid of card panels, with a status header and optional persistence.
 *
 * <p>
 * Added to a builder page with {@link PageSpec#dashboard(DashboardSpec)}, which adds the panel and card-section cards
 * to the page and the {@code dashboard} toolkit pack.  A page has at most one dashboard.  Every structural problem is
 * E-77 when the page is built.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>page</jv>.dashboard(DashboardSpec.<jsm>create</jsm>(<js>"orders"</js>).title(<js>"Orders"</js>)
 * 		.rail(Rail.<jsm>left</jsm>().sections(
 * 			RangeSection.<jsm>quick</jsm>(<js>"7d"</js>, <js>"30d"</js>).publishes(Topics.<jsm>filter</jsm>(<js>"range"</js>))))
 * 		.grid(2)
 * 		.panel(<jv>daily</jv>).panel(<jv>byReason</jv>).panel(<js>"notes"</js>, 2)
 * 		.status(Topics.<jsm>data</jsm>(<js>"orders"</js>))
 * 		.persist(PersistSpec.<jsm>page</jsm>(<js>"app.orders"</js>).topics(Topics.<jsm>filter</jsm>(<js>"range"</js>)).shareLink()));
 * </p>
 *
 * @since 10.0.0
 */
public final class DashboardSpec {

	/** A panel: a card this dashboard adds, or the id of a card already on the page. */
	private record Panel(CardSpec card, String cardId, int span) {
		String id() {
			return card != null ? card.id() : cardId;
		}
	}

	private final String id;
	private String title;
	private Rail rail;
	private int grid = 2;
	private final List<Panel> panels = new ArrayList<>();
	private final List<String> status = new ArrayList<>();
	private PersistSpec persist;

	private DashboardSpec(String id) {
		this.id = id;
	}

	/**
	 * Creates a dashboard.
	 *
	 * @param id The dashboard id: the {@code viewId} of every section payload, and the default storage namespace
	 * 	{@code dashboard.<id>} for the rail width.
	 * @return A new dashboard.
	 */
	public static DashboardSpec create(String id) {
		return new DashboardSpec(id);
	}

	/**
	 * Sets the heading.
	 *
	 * @param value The title.
	 * @return This object.
	 */
	public DashboardSpec title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets the rail.
	 *
	 * @param value The rail.
	 * @return This object.
	 */
	public DashboardSpec rail(Rail value) {
		rail = value;
		return this;
	}

	/**
	 * Sets the number of grid columns.
	 *
	 * @param value 1 to 4.  Defaults to 2.
	 * @return This object.
	 */
	public DashboardSpec grid(int value) {
		grid = value;
		return this;
	}

	/**
	 * Appends a chart panel spanning one column.
	 *
	 * <p>
	 * The chart is snapshotted when it is added; later changes to the {@link ChartDef} are not reflected.
	 * </p>
	 *
	 * @param chart The chart.
	 * @return This object.
	 */
	public DashboardSpec panel(ChartDef chart) {
		return panel(chart, 1);
	}

	/**
	 * Appends a chart panel.
	 *
	 * <p>
	 * The chart is snapshotted when it is added; later changes to the {@link ChartDef} are not reflected.
	 * </p>
	 *
	 * @param chart The chart.
	 * @param span The columns it spans, 1 to {@link #grid(int)}.
	 * @return This object.
	 */
	public DashboardSpec panel(ChartDef chart, int span) {
		panels.add(new Panel(Objects.requireNonNull(chart, "chart must not be null").toCardSpec(), null, span));
		return this;
	}

	/**
	 * Appends a card panel spanning one column.
	 *
	 * @param card The card.  Not {@code html} or {@code datatables}; use {@link #panel(String)} for those.
	 * @return This object.
	 */
	public DashboardSpec panel(CardSpec card) {
		return panel(card, 1);
	}

	/**
	 * Appends a card panel.
	 *
	 * @param card The card.  Not {@code html} or {@code datatables}; use {@link #panel(String, int)} for those.
	 * @param span The columns it spans, 1 to {@link #grid(int)}.
	 * @return This object.
	 */
	public DashboardSpec panel(CardSpec card, int span) {
		panels.add(new Panel(card, null, span));
		return this;
	}

	/**
	 * Appends a panel for a card already added to the page (for example with {@code PageSpec.html(...)}), spanning one
	 * column.
	 *
	 * @param cardId The card id.  The card must be on the page before {@link PageSpec#dashboard(DashboardSpec)}.
	 * @return This object.
	 */
	public DashboardSpec panel(String cardId) {
		return panel(cardId, 1);
	}

	/**
	 * Appends a panel for a card already added to the page.
	 *
	 * @param cardId The card id.  See {@link #panel(String)}.
	 * @param span The columns it spans, 1 to {@link #grid(int)}.
	 * @return This object.
	 */
	public DashboardSpec panel(String cardId, int span) {
		panels.add(new Panel(null, cardId, span));
		return this;
	}

	/**
	 * Binds the status header to data sources.
	 *
	 * @param topics {@code data:<sourceId>} topics, normally {@code Topics.data(id)}.  Each must be a declared source.
	 * @return This object.
	 */
	public DashboardSpec status(String...topics) {
		status.addAll(List.of(topics));
		return this;
	}

	/**
	 * Saves and restores state topics.
	 *
	 * @param value The persistence spec.
	 * @return This object.
	 */
	public DashboardSpec persist(PersistSpec value) {
		persist = value;
		return this;
	}

	/** @return The dashboard id. */
	String id() {
		return id;
	}

	/** @return The cards this dashboard adds to the page: the built panels, then the card sections, in order. */
	List<CardSpec> ownedCards() {
		var out = new ArrayList<CardSpec>();
		for (var p : panels)
			if (p.card() != null)
				out.add(p.card());
		if (rail != null)
			for (var s : rail.sections())
				out.addAll(s.cards());
		return out;
	}

	/**
	 * Checks this dashboard against no page cards and returns its JSON.  {@code PageSpec.dashboard} calls
	 * {@code toJson(Map)} instead, so by-id panels resolve.
	 *
	 * @return The {@code dashboard} contract object.
	 * @throws IllegalArgumentException E-77 on the first problem.
	 */
	public Map<String,Object> toJson() {
		return toJson(Map.of());
	}

	/**
	 * Checks this dashboard and returns its JSON.
	 *
	 * @param pageCards The cards already on the page, by id.
	 * @return The {@code dashboard} contract object.
	 * @throws IllegalArgumentException E-77 on the first problem.
	 */
	Map<String,Object> toJson(Map<String,CardSpec> pageCards) {
		if (id == null || ! Topics.ID.matcher(id).matches())
			throw Section.e77(id, "dashboard id '" + id + "' is not a valid id");
		if (grid < 1 || grid > 4)
			throw Section.e77(id, "grid " + grid + " is outside 1-4");

		var members = new LinkedHashMap<String,CardSpec>();
		var panelList = new ArrayList<Map<String,Object>>();
		for (var p : panels) {
			CardSpec card;
			if (p.card() != null) {
				card = p.card();
				Section.checkOwnedCard(id, card);
			} else {
				card = p.cardId() == null ? null : pageCards.get(p.cardId());
				if (card == null)
					throw Section.e77(id, "panel '" + p.cardId() + "' names no card on this page");
				Section.checkMemberCard(id, card);
			}
			if (p.span() < 1 || p.span() > grid)
				throw Section.e77(id, "panel '" + p.id() + "' span " + p.span() + " is outside 1-" + grid);
			if (members.put(card.id(), card) != null)
				throw Section.e77(id, "card '" + card.id() + "' is in the dashboard twice");
			var m = new LinkedHashMap<String,Object>();
			m.put("card", card.id());
			m.put("span", p.span());
			panelList.add(m);
		}

		var sectionIds = new LinkedHashSet<String>();
		if (rail != null) {
			rail.check(id);
			for (var s : rail.sections()) {
				for (var c : s.cards())
					if (members.put(c.id(), c) != null)
						throw Section.e77(id, "card '" + c.id() + "' is in the dashboard twice");
				if (s.id() != null && ! sectionIds.add(s.id()))
					throw Section.e77(id, "section id '" + s.id() + "' is used twice");
			}
			for (var sid : sectionIds)
				if (members.containsKey(sid) || pageCards.containsKey(sid))
					throw Section.e77(id, "section id '" + sid + "' duplicates card '" + sid + "'");
		}

		var statusTopics = new HashSet<String>();
		for (var t : status) {
			if (t == null || ! t.startsWith("data:") || ! Topics.isKey(t.substring(5)))
				throw Section.e77(id, "status topic '" + t + "' is not a data: topic");
			if (! statusTopics.add(t))
				throw Section.e77(id, "status lists topic '" + t + "' twice");
		}

		if (persist != null) {
			persist.check(id);
			for (var t : persist.topics()) {
				var key = t.substring(t.indexOf(':') + 1);
				if (t.startsWith("filter:")) {
					if (! sectionIds.contains(key))
						throw Section.e77(id, "persist topic '" + t + "' names no section of this dashboard");
				} else if (! publishes(members.get(key), t)) {
					throw Section.e77(id, "persist topic '" + t + "' is not published by a card of this dashboard");
				}
			}
		}

		var m = new LinkedHashMap<String,Object>();
		m.put("id", id);
		if (title != null)
			m.put("title", title);
		m.put("grid", grid);
		m.put("panels", panelList);
		if (rail != null)
			m.put("rail", rail.toMap());
		if (! status.isEmpty())
			m.put("status", List.copyOf(status));
		if (persist != null)
			m.put("persist", persist.toMap());
		return m;
	}

	/** @return <jk>true</jk> if {@code card} lists {@code topic} in its {@code publishes}. */
	private static boolean publishes(CardSpec card, String topic) {
		if (card == null || ! (card.toMap().get("publishes") instanceof List<?> list))
			return false;
		for (var o : list)
			if (o instanceof Map<?,?> p && topic.equals(p.get("topic")))
				return true;
		return false;
	}
}
