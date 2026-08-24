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
 * Builds the server-rendered html5 delivery tree for an {@link AppHeaderDef} &mdash; the whole
 * {@code <header class="jc-header">} page chrome the {@code juneau-chrome.js} runtime enhances (concepts #18/#19).
 *
 * <p>
 * The header beans live in {@code juneau-rest-server-widgets}; this emitter (and the {@code juneau-chrome.js} client
 * runtime, served by {@link ViewsMixin}) is the only place that turns them into markup.  It calls
 * {@link AppHeaderDef#validate()} on entry, so a caller can never serialize an ill-formed header.
 *
 * <h5 class='section'>Escaping / no-injection contract (security-critical):</h5>
 * <p>
 * Every human string (brand title, crumb, tooltip, initials, badge count) is emitted as an html5 <b>text child</b>,
 * which the serializer entity-escapes &mdash; never concatenated into an HTML string.  The icon glyph is <b>empty</b>
 * at server-emit: the action carries only a {@code data-juneau-icon} registry <i>name</i>, and {@code juneau-chrome.js}
 * hydrates the trusted first-party SVG client-side (the single allow-listed {@code innerHTML} sink).  This emitter
 * never emits {@code .jc-nav}/{@code .jc-nav-tab} (445f standing nav rule).
 *
 * <h5 class='section'>Menus wait on {@code [TODO-445h]} (M1 B):</h5>
 * <p>
 * A {@link Behavior#MENU} trigger is emitted <b>disabled</b> ({@code disabled} + {@code aria-disabled="true"}) and its
 * item list is <b>omitted</b> &mdash; no {@code <details>} disclosure, no fake {@code role="menu"} ARIA that would
 * require the (not-yet-shipped) layer manager.  Chips / {@link Behavior#LINK} / {@link Behavior#SAFE} are fully
 * functional.
 *
 * @since 10.0.0
 */
public class AppHeaderTable {

	/** Marker attribute on the {@code <header>} the {@code juneau-chrome.js} runtime scans from (carries the id). */
	public static final String HEADER_MARKER = "data-juneau-app-header";

	/** Attribute carrying a {@link HeaderAction#id} on each action element (carries the id). */
	public static final String ACTION_MARKER = "data-juneau-header-action";

	/** Attribute carrying the action {@link Behavior} (lowercase {@code link|safe|menu}). */
	public static final String BEHAVIOR_ATTR = "data-juneau-behavior";

	/** Attribute carrying a {@link Behavior#SAFE} host-dispatch token. */
	public static final String SAFE_ATTR = "data-juneau-safe";

	/** Attribute carrying the {@code juneau-icons.js} registry glyph name to hydrate client-side. */
	public static final String ICON_ATTR = "data-juneau-icon";

	/** Attribute carrying a badge's namespaced id ({@code header:<id>}), read by the refresh path. */
	public static final String BADGE_ATTR = "data-juneau-badge";

	/** Attribute carrying a badge's tone (lowercase). */
	public static final String BADGE_TONE_ATTR = "data-juneau-badge-tone";

	/**
	 * Attribute carrying a count badge's {@code max} clamp (emitted only when set), so a demand-refresh can re-clamp a
	 * fresh count client-side to {@code "<max>+"} exactly as {@link #clampCount(int, Integer)} does server-side.
	 */
	public static final String BADGE_MAX_ATTR = "data-juneau-badge-max";

	/** Marker attribute on the avatar trigger. */
	public static final String AVATAR_MARKER = "data-juneau-avatar";

	/**
	 * Attribute carrying the header's same-origin demand-refresh endpoint (emitted only when {@code refreshUrl} is set).
	 *
	 * <p>
	 * There is <b>no</b> poller: {@code juneau-chrome.js} fetches this URL only when the host explicitly calls its
	 * public {@code refresh(root)} entry, re-applying the returned counts to the namespaced {@link #BADGE_ATTR} badges
	 * via {@code textContent}.  The value is the exact same-origin path {@link AppHeaderDef#validate()} already vetted.
	 */
	public static final String REFRESH_ATTR = "data-juneau-refresh";

	/** Prefix of the header sidecar {@code <script>} element id: {@code juneau-header:<id>}. */
	public static final String SIDECAR_ID_PREFIX = "juneau-header:";

	/** The badge id namespace for header badges ({@code header:<id>}), so header and bar counts never collide. */
	public static final String BADGE_NS = "header";

	private AppHeaderTable() {}

	/**
	 * Builds the {@code <header class="jc-header">} delivery tree for the given header.
	 *
	 * @param header The built header definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Header} carrying the {@code data-juneau-app-header} chrome.
	 * @throws IllegalArgumentException If {@code header} is <jk>null</jk> or fails {@link AppHeaderDef#validate()}.
	 */
	public static Header of(AppHeaderDef header) {
		if (header == null)
			throw iaex("header must not be null.");
		header.validate();

		var children = new ArrayList<>();
		if (header.brand != null)
			children.add(emitBrand(header.brand));
		var actionKids = new ArrayList<>();
		if (header.actions != null)
			for (var a : header.actions)
				actionKids.add(emitAction(a));
		if (header.avatar != null)
			actionKids.add(emitAvatar(header.avatar));
		if (! actionKids.isEmpty())
			children.add(div(actionKids.toArray()).class_("jc-header-actions"));

		var el = header(children.toArray()).class_("jc-header").attr(HEADER_MARKER, header.id);
		if (header.refreshUrl != null && ! header.refreshUrl.isBlank())
			el.attr(REFRESH_ATTR, header.refreshUrl);
		return el;
	}

	/**
	 * Builds the header's data-only refresh sidecar, or <jk>null</jk> when the header declares no {@code refreshUrl}.
	 *
	 * @param header The built header definition.  Must not be <jk>null</jk>.
	 * @return The {@code <script type="application/json">} sidecar, or <jk>null</jk>.
	 */
	public static Script sidecar(AppHeaderDef header) {
		if (header == null || header.refreshUrl == null || header.refreshUrl.isBlank())
			return null;
		var badges = new LinkedHashMap<String,Integer>();
		if (header.actions != null)
			for (var a : header.actions)
				if (a.badge != null && a.badge.count != null)
					badges.put(BADGE_NS + ":" + a.id, a.badge.count);
		var meta = new LinkedHashMap<String,Object>();
		meta.put("contractVersion", AppHeaderDef.CONTRACT_VERSION);
		meta.put("badges", badges);
		var json = escapeForScript(Json.of(meta));
		return script().type("application/json").id(SIDECAR_ID_PREFIX + header.id).text(rawText(json));
	}

	private static Div emitBrand(Brand brand) {
		var kids = new ArrayList<>();
		if (brand.logo == null || brand.logo)
			kids.add(div().class_("jc-logo"));
		var titleKids = new ArrayList<>();
		if (brand.title != null && ! brand.title.isBlank())
			titleKids.add(span(brand.title));
		if (brand.crumbs != null)
			for (var c : brand.crumbs) {
				if (! titleKids.isEmpty())
					titleKids.add(span("/").class_("jc-brand-sep"));
				titleKids.add(span(c));
			}
		if (! titleKids.isEmpty())
			kids.add(div(titleKids.toArray()).class_("jc-brand-title"));
		return div(kids.toArray()).class_("jc-brand");
	}

	private static HtmlElement emitAction(HeaderAction a) {
		var behavior = a.behavior.name().toLowerCase(Locale.ROOT);
		var inner = new ArrayList<>();
		inner.add(span().class_("jc-icon").attr("aria-hidden", "true"));
		if (a.badge != null)
			inner.add(emitBadge(BADGE_NS, a.id, a.badge));

		if (a.behavior == Behavior.LINK) {
			var el = a(a.href, inner.toArray()).class_("jc-icon-btn")
				.attr(ACTION_MARKER, a.id)
				.attr(BEHAVIOR_ATTR, behavior)
				.attr("aria-label", a.tooltip)
				.attr("title", a.tooltip);
			if (a.icon != null)
				el.attr(ICON_ATTR, a.icon);
			return el;
		}

		var el = button("button", inner.toArray()).class_("jc-icon-btn")
			.attr(ACTION_MARKER, a.id)
			.attr(BEHAVIOR_ATTR, behavior)
			.attr("aria-label", a.tooltip)
			.attr("title", a.tooltip);
		if (a.icon != null)
			el.attr(ICON_ATTR, a.icon);
		if (a.behavior == Behavior.SAFE)
			el.attr(SAFE_ATTR, a.safe);
		if (a.behavior == Behavior.MENU) {
			// M1 B: menus wait on 445h - disable the trigger and omit the list (no fake disclosure).
			el.attr("disabled", "disabled").attr("aria-disabled", "true");
		}
		return el;
	}

	private static Span emitBadge(String ns, String id, Badge badge) {
		var s = span().class_("jc-badge").attr(BADGE_ATTR, ns + ":" + id);
		if (badge.tone != null)
			s.attr(BADGE_TONE_ATTR, badge.tone.name().toLowerCase(Locale.ROOT));
		if (badge.dot != null && badge.dot) {
			s.attr("class", "jc-badge jc-badge-dot");
		} else if (badge.count != null) {
			if (badge.max != null)
				s.attr(BADGE_MAX_ATTR, Integer.toString(badge.max));
			s.child(clampCount(badge.count, badge.max));   // plain text child - serializer entity-escapes it
		}
		if (badge.label != null && ! badge.label.isBlank())
			s.attr("aria-label", badge.label);
		return s;
	}

	/** Clamps a count above {@code max} to {@code "<max>+"} (display text; painted as an escaped text child). */
	static String clampCount(int count, Integer max) {
		if (max != null && count > max)
			return max + "+";
		return Integer.toString(count);
	}

	private static HtmlElement emitAvatar(AvatarChip avatar) {
		var cls = new StringBuilder("jc-avatar");
		if (avatar.status != null)
			cls.append(" jc-avatar-status-").append(avatar.status.name().toLowerCase(Locale.ROOT));

		var kids = new ArrayList<>();
		if (avatar.imageUrl != null && ! avatar.imageUrl.isBlank()) {
			kids.add(img(avatar.imageUrl).class_("jc-avatar-img").attr("alt", ""));
			if (avatar.initials != null && ! avatar.initials.isBlank())
				kids.add(span(avatar.initials).class_("jc-avatar-initials").hidden(true));
		} else if (avatar.initials != null && ! avatar.initials.isBlank()) {
			kids.add(avatar.initials);   // plain text child - serializer entity-escapes it
		}

		if (avatar.menu != null && ! avatar.menu.isEmpty()) {
			// M1 B: an avatar with a menu is a menu trigger - disabled until 445h, list omitted (never a dead button).
			return button("button", kids.toArray()).class_(cls.toString())
				.attr(AVATAR_MARKER, "1")
				.attr(BEHAVIOR_ATTR, "menu")
				.attr("aria-label", avatar.displayName)
				.attr("disabled", "disabled")
				.attr("aria-disabled", "true");
		}
		// No menu: a non-interactive identity chip, not a dead <button>.
		return span(kids.toArray()).class_(cls.toString())
			.attr(AVATAR_MARKER, "1")
			.attr("role", "img")
			.attr("aria-label", avatar.displayName);
	}
}
