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

import java.util.regex.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.http.*;

/**
 * A single row in a {@link HeaderAction} or {@link AvatarChip} attached menu.
 *
 * <p>
 * A menu item is exactly one of a <b>link</b> (same-origin navigation), a <b>safe</b> (format-validated client-event
 * token, host-interpreted), or a <b>divider</b> (a separator row).  Dividers are <b>exempt</b> from the id/label/href/
 * safe rules entirely: a fail-closed validate must not reject a real menu just because it contains a separator, and
 * {@link #divider()} mints no id.
 *
 * @since 10.0.0
 */
@BeanType(properties="id,label,icon,href,safe,divider")
public class MenuItem {

	/** The client-safe token charset: lowercase letter, then lowercase / digit / hyphen, max 64 chars. */
	private static final Pattern SAFE_TOKEN = Pattern.compile("[a-z][a-z0-9-]{0,63}");

	/** Required + unique for link/safe items; <b>ignored</b> for dividers. */
	public String id;

	/** The row text, painted as {@code textContent}; ignored for dividers. */
	public String label;

	/** Optional icon-registry name, hydrated client-side from {@code data-juneau-icon}. */
	public String icon;

	/** A same-origin navigation target (link item); ignored for dividers. */
	public String href;

	/** A format-validated client-event token (safe item); ignored for dividers. */
	public String safe;

	/** When {@link Boolean#TRUE}, this is a separator row and all other fields are ignored. */
	public Boolean divider;

	/**
	 * Creates a navigation (link) menu item.
	 *
	 * @param id The stable item id.  Must not be <jk>null</jk> or blank.
	 * @param label The row text.  Must not be <jk>null</jk> or blank.
	 * @param href The same-origin navigation target.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link MenuItem}.
	 */
	public static MenuItem link(String id, String label, String href) {
		var m = new MenuItem();
		m.id = id;
		m.label = label;
		m.href = href;
		return m;
	}

	/**
	 * Creates a client-safe menu item.
	 *
	 * @param id The stable item id.  Must not be <jk>null</jk> or blank.
	 * @param label The row text.  Must not be <jk>null</jk> or blank.
	 * @param safeToken The client-event token; must match {@code ^[a-z][a-z0-9-]{0,63}$}.
	 * @return A new {@link MenuItem}.
	 */
	public static MenuItem safe(String id, String label, String safeToken) {
		var m = new MenuItem();
		m.id = id;
		m.label = label;
		m.safe = safeToken;
		return m;
	}

	/**
	 * Creates a divider (separator) row.  Needs no id &mdash; it is exempt from the unique-id rule.
	 *
	 * @return A new divider {@link MenuItem}.
	 */
	public static MenuItem divider() {
		var m = new MenuItem();
		m.divider = true;
		return m;
	}

	/**
	 * Sets the optional icon-registry name.
	 *
	 * @param value The icon name.
	 * @return This object.
	 */
	public MenuItem icon(String value) {
		icon = value;
		return this;
	}

	/**
	 * Whether this item is a divider (separator) row.
	 *
	 * @return <jk>true</jk> if this is a divider.
	 */
	public boolean isDivider() {
		return divider != null && divider;
	}

	/**
	 * Fail-closed bean validation; called by the enclosing {@link HeaderAction}/{@link AvatarChip}.
	 *
	 * <p>
	 * Dividers are exempt: a divider validates trivially regardless of its other (ignored) fields.
	 *
	 * @throws IllegalArgumentException If this menu item is not well-formed.
	 */
	public void validate() {
		if (isDivider())
			return;
		if (id == null || id.isBlank())
			throw iaex("MenuItem id must not be null or blank.");
		if (label == null || label.isBlank())
			throw iaex("MenuItem '%s' label must not be null or blank.", id);
		var hasHref = href != null && !href.isBlank();
		var hasSafe = safe != null && !safe.isBlank();
		if (hasHref == hasSafe)
			throw iaex("MenuItem '%s' must declare exactly one of link/safe.", id);
		if (hasHref && !SafePathTemplate.isSameOriginPath(href))
			throw iaex("MenuItem '%s' href must be a same-origin path (no absolute URL, '//', scheme, '..', "
				+ "'data:', or 'javascript:'): %s", id, href);
		if (hasSafe && !SAFE_TOKEN.matcher(safe).matches())
			throw iaex("MenuItem '%s' safe token must match ^[a-z][a-z0-9-]{0,63}$: %s", id, safe);
	}
}
