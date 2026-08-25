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

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Builds the server-rendered html5 delivery tree for a {@link QuickStats} &mdash; the at-a-glance figures strip
 * {@link ViewTable} emits above a table's toolbar.
 *
 * <p>
 * The stat beans live in {@code juneau-rest-server-widgets}; this emitter is the only place that turns them into
 * markup.  It calls {@link QuickStats#validate()} on entry.  Every human string and every figure is emitted as an
 * entity-escaped html5 text child, never as markup.
 *
 * <h5 class='section'>Display-only, mechanically:</h5>
 * <p>
 * The emitted tree is inert by construction &mdash; only {@code <div>}/{@code <span>} elements, and the only
 * attributes are {@code class}, the {@code data-juneau-stat*} identity markers, {@code style} for a bar's
 * server-computed width, and {@code aria-hidden} on a bar's decorative track.  There is no {@code role},
 * {@code tabindex}, {@code href}, {@code data-juneau-action}, or {@code on*} attribute anywhere, so there is nothing
 * for a runtime to bind a click, a key, or a timer to.  There is also no sidecar: unlike {@link BarSlotTable} a
 * quick-stats strip publishes no data for a client to refresh from.
 *
 * @since 10.0.0
 */
public class QuickStatsTable {

	/** Marker attribute on the strip {@code <div>} (carries the {@link QuickStats#id}). */
	public static final String QUICKSTATS_MARKER = "data-juneau-quickstats";

	/** Attribute carrying {@link QuickStats#CONTRACT_VERSION} on the strip. */
	public static final String QUICKSTATS_CONTRACT_ATTR = "data-juneau-quickstats-contract";

	/** Attribute carrying a {@link StatItem#id()} on each item element. */
	public static final String STAT_MARKER = "data-juneau-stat";

	private QuickStatsTable() {}

	/**
	 * Builds the {@code data-juneau-quickstats} strip for the given quick stats.
	 *
	 * @param stats The built quick-stats strip.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the strip.
	 * @throws IllegalArgumentException If {@code stats} is <jk>null</jk> or fails {@link QuickStats#validate()}.
	 */
	public static Div of(QuickStats stats) {
		if (stats == null)
			throw iaex("stats must not be null.");
		stats.validate();

		var kids = new ArrayList<>();
		for (var i : stats.items)
			kids.add(emitItem(i));
		return div(kids.toArray())
			.class_("jc-quickstats")
			.attr(QUICKSTATS_MARKER, stats.id)
			.attr(QUICKSTATS_CONTRACT_ATTR, QuickStats.CONTRACT_VERSION);
	}

	private static HtmlElement<?> emitItem(StatItem i) {
		if (i instanceof StatTile i2)
			return div(
				span(i2.label).class_("jc-stat-label"),
				span(i2.value).class_(toneClass("jc-stat-value", i2.tone))
			).class_("jc-stat jc-stat-tile").attr(STAT_MARKER, i2.id);
		if (i instanceof StatBar i2)
			return div(
				span(i2.label).class_("jc-stat-label"),
				// The fill is a purely visual box with no text of its own.  It carries an empty raw-text child rather
				// than no children at all, because the html5 serializer renders a childless element as
				// `<span nil="true"/>` - which would read as "no value" on a bar that legitimately has one.
				span(span(rawText("")).class_(toneClass("jc-stat-fill", i2.tone)).attr("style", "width:" + i2.percent() + "%"))
					.class_("jc-stat-track").attr("aria-hidden", "true"),
				span(i2.value + " / " + i2.max).class_("jc-stat-value")
			).class_("jc-stat jc-stat-bar").attr(STAT_MARKER, i2.id);
		if (i instanceof SegmentedBadge i2) {
			var kids = new ArrayList<>();
			kids.add(span(i2.label).class_("jc-stat-label"));
			for (var s : i2.segments)
				kids.add(span(
					span(Long.toString(s.count)).class_("jc-stat-segment-count"),
					span(s.label).class_("jc-stat-segment-label")
				).class_(toneClass("jc-stat-segment", s.tone)));
			return div(kids.toArray()).class_("jc-stat jc-stat-segments").attr(STAT_MARKER, i2.id);
		}
		throw iaex("QuickStatsTable does not know how to emit StatItem type '%s'.", i.getClass().getName());
	}

	/**
	 * Appends the {@code is-<tone>} modifier for an explicit tone.
	 *
	 * <p>
	 * {@link StatusTone#NEUTRAL} deliberately adds nothing &mdash; "no semantic colour" is the absence of a modifier,
	 * so it renders identically to an unset tone (both inherit the surrounding text colour), exactly as a
	 * {@code neutral} pill dot does.
	 */
	private static String toneClass(String base, String tone) {
		return tone == null || tone.isBlank() || StatusTone.NEUTRAL.wire().equals(tone) ? base : base + " is-" + tone;
	}
}
