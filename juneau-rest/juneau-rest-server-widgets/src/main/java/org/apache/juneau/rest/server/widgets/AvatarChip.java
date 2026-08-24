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

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.http.*;

/**
 * The trailing identity chip of an {@link AppHeaderDef}: an initials fallback or a same-origin image, an optional
 * presence ring, and an optional attached single-level menu.
 *
 * <p>
 * This is a plain data bean &mdash; <b>not</b> a {@link Widget} (a child of {@link AppHeaderDef}; owns no
 * {@code CONTRACT_VERSION}).  {@link #displayName} is an <b>accessible name</b> (a display label) &mdash; never an
 * email &mdash; and {@link #imageUrl} is deliberately restricted to a same-origin path: any email-hash avatar CDN is
 * forbidden.  A load error on the image falls back to the initials node.
 *
 * @since 10.0.0
 */
@BeanType(properties="displayName,initials,imageUrl,status,menu")
public class AvatarChip {

	/** The required accessible name (a display label, not an email), emitted as {@code aria-label}. */
	public String displayName;

	/** The initials fallback (e.g. <js>"AL"</js>), painted as {@code textContent}. */
	public String initials;

	/** Optional same-origin image path (no email-hash CDN); a load error falls back to {@link #initials}. */
	public String imageUrl;

	/** Optional presence ring; <jk>null</jk> = no ring (chrome, not a directory API). */
	public Status status;

	/** Optional attached single-level menu. */
	public List<MenuItem> menu;

	/**
	 * Creates an avatar chip with the given accessible name.
	 *
	 * @param displayName The accessible name (a display label, not an email).  Must not be <jk>null</jk> or blank.
	 * @return A new {@link AvatarChip}.
	 */
	public static AvatarChip of(String displayName) {
		var a = new AvatarChip();
		a.displayName = displayName;
		return a;
	}

	/**
	 * Sets the initials fallback.
	 *
	 * @param value The initials (e.g. <js>"AL"</js>).
	 * @return This object.
	 */
	public AvatarChip initials(String value) {
		initials = value;
		return this;
	}

	/**
	 * Sets the same-origin image path.
	 *
	 * @param value The same-origin image path.
	 * @return This object.
	 */
	public AvatarChip imageUrl(String value) {
		imageUrl = value;
		return this;
	}

	/**
	 * Sets the presence ring.
	 *
	 * @param value The status, or <jk>null</jk> for no ring.
	 * @return This object.
	 */
	public AvatarChip status(Status value) {
		status = value;
		return this;
	}

	/**
	 * Sets the attached menu items.
	 *
	 * @param value The menu items, in display order.
	 * @return This object.
	 */
	public AvatarChip menu(MenuItem...value) {
		menu = l(value);
		return this;
	}

	/**
	 * Fail-closed bean validation; called by {@link AppHeaderDef#validate()}.
	 *
	 * @throws IllegalArgumentException If this chip is not well-formed.
	 */
	public void validate() {
		if (displayName == null || displayName.isBlank())
			throw iaex("AvatarChip displayName (accessible name) must not be null or blank.");
		var hasInitials = initials != null && !initials.isBlank();
		var hasImage = imageUrl != null && !imageUrl.isBlank();
		if (! (hasInitials || hasImage))
			throw iaex("AvatarChip '%s' must declare at least one of initials/imageUrl.", displayName);
		if (hasImage && !SafePathTemplate.isSameOriginPath(imageUrl))
			throw iaex("AvatarChip '%s' imageUrl must be a same-origin path (no absolute URL, '//', scheme, "
				+ "'..', 'data:', or 'javascript:'): %s", displayName, imageUrl);
		if (menu != null) {
			var ids = new HashSet<String>();
			for (var mi : menu) {
				if (mi == null)
					throw iaex("AvatarChip '%s' menu item must not be null.", displayName);
				mi.validate();
				if (!mi.isDivider() && !ids.add(mi.id))
					throw iaex("AvatarChip '%s' duplicate menu item id '%s'.", displayName, mi.id);
			}
		}
	}
}
