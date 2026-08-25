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
import static org.apache.juneau.commons.utils.StringUtils.escapeForScript;

import java.util.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Builds the server-rendered html5 delivery tree for a {@link BarSlot} &mdash; the additive
 * {@code data-juneau-bar-slot} region {@link PageTable} emits as a <b>trailing sibling of {@code .jc-subtab-bar}</b>
 * (concept #9), plus a data-only sidecar for its dynamic counts.
 *
 * <h5 class='section'>Two named hosts, one emitter:</h5>
 * <p>
 * {@link PageDef#barSlot} is the page host: {@link #of(BarSlot)} + {@link #sidecar(BarSlot)}, emitted once per page
 * with a document-unique sidecar {@code id}.  {@link RowDetailDef#barSlot} is the row-detail host:
 * {@link #detailRegion(BarSlot, String)} + {@link #detailSidecar(BarSlot)}, emitted into the row-expand
 * {@code <template>} and therefore cloned per expanded row &mdash; so its sidecar ships {@code id}-less and the
 * runtime mints a row-qualified identity after cloning.  Sharing the bean and the emitter is the point; the hosts and
 * their placements stay distinct.
 *
 * <p>
 * The bar beans live in {@code juneau-rest-server-widgets}; this emitter is the only place that turns them into markup.
 * It calls {@link BarSlot#validate()} on entry.  Every human string is emitted as an entity-escaped html5 text child.
 * The region is <b>never</b> emitted into the archived {@code .juneau-view-toolbar-*} DataTables control row.
 *
 * @since 10.0.0
 */
public class BarSlotTable {

	/** Marker attribute on the bar-slot region {@code <div>} the runtime scans from (carries the id). */
	public static final String BAR_SLOT_MARKER = "data-juneau-bar-slot";

	/** Attribute carrying a {@link BarWidget} id on each widget element. */
	public static final String BAR_WIDGET_MARKER = "data-juneau-bar-widget";

	/** Prefix of the bar sidecar {@code <script>} element id: {@code juneau-bar:<id>}. */
	public static final String SIDECAR_ID_PREFIX = "juneau-bar:";

	/** The badge id namespace for bar badges ({@code bar:<id>}), so header and bar counts never collide. */
	public static final String BADGE_NS = "bar";

	/**
	 * Attribute naming the markup anchor a <b>detail-hosted</b> region was emitted against, so the runtime never has to
	 * guess from position: {@link #ANCHOR_RIBBON} or {@link #ANCHOR_SECTION_TITLE}.  Absent on a page-level region.
	 */
	public static final String BAR_SLOT_ANCHOR_ATTR = "data-juneau-bar-slot-anchor";

	/**
	 * Anchor for a detail with <b>two or more</b> sections: the region is the detail {@code <template>}'s last direct
	 * child, and the runtime moves it to the trailing position of the ribbon it builds from those sections.
	 */
	public static final String ANCHOR_RIBBON = "ribbon";

	/**
	 * Anchor for a <b>single-section</b> detail: no ribbon exists and none is synthesized, so the region is emitted
	 * inside that lone section as the immediate next sibling of its
	 * {@code h2.juneau-view-detail-section-title}, and the runtime leaves it there.
	 */
	public static final String ANCHOR_SECTION_TITLE = "section-title";

	/** Class added to a detail-hosted region, so CSS and the runtime address it by name rather than by position. */
	public static final String DETAIL_SLOT_CLASS = "juneau-view-detail-bar-slot";

	/**
	 * Attribute finding a detail-hosted region's {@code id}-less sidecar inside a cloned panel, exactly as
	 * {@link ViewTable#NESTED_META_ATTR} finds a nested table's: a {@code <template>} clone cannot carry a
	 * document-unique {@code id}, so the runtime mints one per expanded row.
	 */
	public static final String BAR_META_ATTR = "data-juneau-bar-meta";

	private BarSlotTable() {}

	/**
	 * Builds the {@code data-juneau-bar-slot} region for the given bar slot.
	 *
	 * @param bar The built bar slot.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code data-juneau-bar-slot} region.
	 * @throws IllegalArgumentException If {@code bar} is <jk>null</jk> or fails {@link BarSlot#validate()}.
	 */
	public static Div of(BarSlot bar) {
		if (bar == null)
			throw iaex("bar must not be null.");
		bar.validate();

		var kids = new ArrayList<>();
		for (var w : bar.widgets)
			kids.add(emitWidget(w));
		var region = div(kids.toArray()).class_("jc-bar-slot").attr(BAR_SLOT_MARKER, bar.id);
		if (bar.refreshUrl != null && ! bar.refreshUrl.isBlank())
			region.attr(AppHeaderTable.REFRESH_ATTR, bar.refreshUrl);
		return region;
	}

	/**
	 * Builds the {@code data-juneau-bar-slot} region for a bar slot hosted on the <b>row-detail ribbon</b>.
	 *
	 * <p>
	 * Same region as {@link #of(BarSlot)}, plus {@link #DETAIL_SLOT_CLASS} and the declared
	 * {@link #BAR_SLOT_ANCHOR_ATTR}.  The marker still carries the <b>author</b> {@link BarSlot#id}; the runtime
	 * rewrites it to a row-qualified suffix once the enclosing {@code <template>} has been cloned, since one document
	 * can hold one such region per expanded row.
	 *
	 * @param bar The built bar slot.  Must not be <jk>null</jk>.
	 * @param anchor {@link #ANCHOR_RIBBON} or {@link #ANCHOR_SECTION_TITLE}.
	 * @return A new {@link Div} carrying the detail-hosted region.
	 * @throws IllegalArgumentException If {@code bar} is <jk>null</jk> or fails {@link BarSlot#validate()}.
	 */
	public static Div detailRegion(BarSlot bar, String anchor) {
		var region = of(bar);
		region.attr("class", "jc-bar-slot " + DETAIL_SLOT_CLASS);
		region.attr(BAR_SLOT_ANCHOR_ATTR, anchor);
		return region;
	}

	/**
	 * Builds the bar slot's data-only sidecar carrying its initial dynamic counts.
	 *
	 * @param bar The built bar slot.  Must not be <jk>null</jk>.
	 * @return The {@code <script type="application/json">} sidecar.
	 */
	public static Script sidecar(BarSlot bar) {
		return sidecar(bar, true);
	}

	/**
	 * Builds the same data-only sidecar {@code id}-less, found by {@link #BAR_META_ATTR} instead.
	 *
	 * <p>
	 * For a detail-hosted slot only: the sidecar rides the row-expand {@code <template>}, so a baked-in
	 * {@code id="juneau-bar:<authorId>"} would collide across simultaneously-expanded rows.  The runtime mints the
	 * per-row {@code id} after cloning, matching the nested-table VIEW_META sidecar's contract.
	 *
	 * @param bar The built bar slot.  Must not be <jk>null</jk>.
	 * @return The {@code id}-less {@code <script type="application/json">} sidecar.
	 */
	public static Script detailSidecar(BarSlot bar) {
		return sidecar(bar, false);
	}

	private static Script sidecar(BarSlot bar, boolean withId) {
		if (bar == null)
			throw iaex("bar must not be null.");
		var badges = new LinkedHashMap<String,Integer>();
		if (bar.widgets != null)
			for (var w : bar.widgets)
				if (w instanceof BarBadge b && b.badge != null && b.badge.count != null)
					badges.put(BADGE_NS + ":" + b.id, b.badge.count);
		var meta = new LinkedHashMap<String,Object>();
		meta.put("contractVersion", BarSlot.CONTRACT_VERSION);
		meta.put("badges", badges);
		var json = escapeForScript(Json.of(meta));
		var s = script().type("application/json").text(rawText(json));
		if (withId)
			s.id(SIDECAR_ID_PREFIX + bar.id);
		else
			s.attr(BAR_META_ATTR, bar.id);
		return s;
	}

	private static HtmlElement<?> emitWidget(BarWidget w) {
		if (w instanceof BarText t)
			return span(t.text).class_("jc-bar-text").attr(BAR_WIDGET_MARKER, t.id);
		if (w instanceof BarBadge b) {
			var kids = new ArrayList<>();
			if (b.label != null && ! b.label.isBlank())
				kids.add(span(b.label).class_("jc-bar-label"));
			if (b.badge != null)
				kids.add(emitBadge(b.id, b.badge));
			return span(kids.toArray()).class_("jc-bar-badge").attr(BAR_WIDGET_MARKER, b.id);
		}
		throw iaex("BarSlotTable does not know how to emit BarWidget type '%s'.", w.getClass().getName());
	}

	private static Span emitBadge(String id, Badge badge) {
		var s = span().class_("jc-badge").attr(AppHeaderTable.BADGE_ATTR, BADGE_NS + ":" + id);
		if (badge.tone != null)
			s.attr(AppHeaderTable.BADGE_TONE_ATTR, badge.tone.name().toLowerCase(Locale.ROOT));
		if (badge.dot != null && badge.dot) {
			s.attr("class", "jc-badge jc-badge-dot");
		} else if (badge.count != null) {
			if (badge.max != null)
				s.attr(AppHeaderTable.BADGE_MAX_ATTR, Integer.toString(badge.max));
			s.child(AppHeaderTable.clampCount(badge.count, badge.max));
		}
		if (badge.label != null && ! badge.label.isBlank())
			s.attr("aria-label", badge.label);
		return s;
	}
}
