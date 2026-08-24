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
	 * Builds the bar slot's data-only sidecar carrying its initial dynamic counts.
	 *
	 * @param bar The built bar slot.  Must not be <jk>null</jk>.
	 * @return The {@code <script type="application/json">} sidecar.
	 */
	public static Script sidecar(BarSlot bar) {
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
		return script().type("application/json").id(SIDECAR_ID_PREFIX + bar.id).text(rawText(json));
	}

	private static HtmlElement emitWidget(BarWidget w) {
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
