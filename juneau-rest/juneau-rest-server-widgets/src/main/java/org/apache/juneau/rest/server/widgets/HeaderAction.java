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
import java.util.regex.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.http.*;

/**
 * One icon action in an {@link AppHeaderDef} action row: an icon, an accessible tooltip, a {@link Behavior}, and an
 * optional overlay {@link Badge}.
 *
 * <p>
 * This is a plain data bean &mdash; <b>not</b> a {@link Widget} (it is a child of {@link AppHeaderDef} and owns no
 * {@code CONTRACT_VERSION}).  There is <b>no</b> role-gated visibility in v1: there is no {@code roles()} field, no
 * hint, and no emitter drop-path.
 *
 * @since 10.0.0
 */
@BeanType(properties="id,icon,tooltip,behavior,href,safe,badge,menu")
public class HeaderAction {

	/** The client-safe token charset: lowercase letter, then lowercase / digit / hyphen, max 64 chars. */
	private static final Pattern SAFE_TOKEN = Pattern.compile("[a-z][a-z0-9-]{0,63}");

	/** The stable action id; a11y + test hook; unique within the action row.  Required, non-blank. */
	public String id;

	/** The {@code juneau-icons.js} registry name, emitted as {@code data-juneau-icon} and hydrated client-side. */
	public String icon;

	/** The accessible name, emitted as {@code aria-label} + {@code title}.  Required, non-blank. */
	public String tooltip;

	/** How the action behaves.  Required. */
	public Behavior behavior;

	/** {@link Behavior#LINK} only: a same-origin navigation path. */
	public String href;

	/** {@link Behavior#SAFE} only: a format-validated host-dispatch token. */
	public String safe;

	/** Optional overlay badge (a bell count, for example). */
	public Badge badge;

	/** {@link Behavior#MENU} only: a single-level attached menu. */
	public List<MenuItem> menu;

	/**
	 * Creates a {@link Behavior#LINK} action.
	 *
	 * @param id The stable action id.  Must not be <jk>null</jk> or blank.
	 * @param icon The icon-registry name.
	 * @param tooltip The accessible name.  Must not be <jk>null</jk> or blank.
	 * @param href The same-origin navigation path.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link HeaderAction}.
	 */
	public static HeaderAction link(String id, String icon, String tooltip, String href) {
		var a = new HeaderAction();
		a.id = id;
		a.icon = icon;
		a.tooltip = tooltip;
		a.behavior = Behavior.LINK;
		a.href = href;
		return a;
	}

	/**
	 * Creates a {@link Behavior#SAFE} action.
	 *
	 * @param id The stable action id.  Must not be <jk>null</jk> or blank.
	 * @param icon The icon-registry name.
	 * @param tooltip The accessible name.  Must not be <jk>null</jk> or blank.
	 * @param safeToken The host-dispatch token; must match {@code ^[a-z][a-z0-9-]{0,63}$}.
	 * @return A new {@link HeaderAction}.
	 */
	public static HeaderAction safe(String id, String icon, String tooltip, String safeToken) {
		var a = new HeaderAction();
		a.id = id;
		a.icon = icon;
		a.tooltip = tooltip;
		a.behavior = Behavior.SAFE;
		a.safe = safeToken;
		return a;
	}

	/**
	 * Creates a {@link Behavior#MENU} action.  The menu items are attached via {@link #menu(MenuItem...)}.
	 *
	 * @param id The stable action id.  Must not be <jk>null</jk> or blank.
	 * @param icon The icon-registry name.
	 * @param tooltip The accessible name.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link HeaderAction}.
	 */
	public static HeaderAction menu(String id, String icon, String tooltip) {
		var a = new HeaderAction();
		a.id = id;
		a.icon = icon;
		a.tooltip = tooltip;
		a.behavior = Behavior.MENU;
		return a;
	}

	/**
	 * Sets the overlay badge.
	 *
	 * @param value The badge.
	 * @return This object.
	 */
	public HeaderAction badge(Badge value) {
		badge = value;
		return this;
	}

	/**
	 * Sets the attached menu items ({@link Behavior#MENU} only).
	 *
	 * @param value The menu items, in display order.
	 * @return This object.
	 */
	public HeaderAction menu(MenuItem...value) {
		menu = l(value);
		return this;
	}

	/**
	 * Fail-closed bean validation; called by {@link AppHeaderDef#validate()} (not a {@link Widget} override).
	 *
	 * @throws IllegalArgumentException If this action is not well-formed.
	 */
	public void validate() {
		if (id == null || id.isBlank())
			throw iaex("HeaderAction id must not be null or blank.");
		if (tooltip == null || tooltip.isBlank())
			throw iaex("HeaderAction '%s' tooltip (accessible name) must not be null or blank.", id);
		if (behavior == null)
			throw iaex("HeaderAction '%s' behavior must not be null.", id);
		switch (behavior) {
			case LINK -> {
				if (href == null || href.isBlank())
					throw iaex("HeaderAction '%s' LINK requires a same-origin href.", id);
				if (!SafePathTemplate.isSameOriginPath(href))
					throw iaex("HeaderAction '%s' href must be a same-origin path (no absolute URL, '//', scheme, "
						+ "'..', 'data:', or 'javascript:'): %s", id, href);
				if (safe != null)
					throw iaex("HeaderAction '%s' LINK must not declare a safe token.", id);
				if (menu != null)
					throw iaex("HeaderAction '%s' LINK must not declare a menu.", id);
			}
			case SAFE -> {
				if (safe == null || !SAFE_TOKEN.matcher(safe).matches())
					throw iaex("HeaderAction '%s' SAFE token must match ^[a-z][a-z0-9-]{0,63}$: %s", id, safe);
				if (href != null)
					throw iaex("HeaderAction '%s' SAFE must not declare an href.", id);
				if (menu != null)
					throw iaex("HeaderAction '%s' SAFE must not declare a menu.", id);
			}
			case MENU -> {
				if (menu == null || menu.isEmpty())
					throw iaex("HeaderAction '%s' MENU requires a non-empty menu.", id);
				if (href != null)
					throw iaex("HeaderAction '%s' MENU must not declare an href.", id);
				if (safe != null)
					throw iaex("HeaderAction '%s' MENU must not declare a safe token.", id);
				var ids = new HashSet<String>();
				for (var mi : menu) {
					if (mi == null)
						throw iaex("HeaderAction '%s' menu item must not be null.", id);
					mi.validate();
					if (!mi.isDivider() && !ids.add(mi.id))
						throw iaex("HeaderAction '%s' duplicate menu item id '%s'.", id, mi.id);
				}
			}
		}
		if (badge != null)
			badge.validate();
	}
}
