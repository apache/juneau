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
 * The declarative page-chrome header around a set of views: an optional brand cluster, a left-to-right row of
 * {@link HeaderAction}s, and an optional trailing {@link AvatarChip}.
 *
 * <p>
 * A pure data bean.  The html5 emitter lives in {@code juneau-rest-server-views} (composed by {@code PageTable} from
 * {@code PageDef.header}); the opt-in {@code juneau-chrome.js} client runtime is served by {@code ViewsMixin}.  Widgets
 * keeps no dependency on views.
 *
 * <p>
 * There is <b>no</b> role-gated visibility in v1: there is no {@code roles()} field or drop-path.  Menus depend on a
 * shared layer manager that has not shipped yet; until it lands, a {@link Behavior#MENU} trigger is disabled and its
 * list is omitted (no fake disclosure).
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,id,brand,actions,avatar")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class AppHeaderDef implements Widget {

	/** The frozen contract version for this widget.  Serialized as the JSON <b>string</b> {@code "1"}. */
	public static final String CONTRACT_VERSION = "1";

	/** The contract version; must equal {@link #CONTRACT_VERSION} at validation time. */
	public String contractVersion = CONTRACT_VERSION;

	/** The stable header id; a11y + test hook + sidecar id.  Required, non-blank. */
	public String id;

	/** Optional brand cluster (logo flag + title crumbs). */
	public Brand brand;

	/** Optional left-to-right action row. */
	public List<HeaderAction> actions;

	/** Optional trailing identity chip. */
	public AvatarChip avatar;

	/**
	 * Optional same-origin refresh endpoint owning the header sidecar's demand-refresh.  Omit = no live refresh.
	 * Present here (rather than only in HTML) so no field exists only on the wire.
	 */
	public String refreshUrl;

	/**
	 * Creates a header with the given id.
	 *
	 * @param id The stable header id.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link AppHeaderDef}.
	 */
	public static AppHeaderDef create(String id) {
		var h = new AppHeaderDef();
		h.id = id;
		return h;
	}

	/**
	 * Sets the brand cluster.
	 *
	 * @param value The brand cluster.
	 * @return This object.
	 */
	public AppHeaderDef brand(Brand value) {
		brand = value;
		return this;
	}

	/**
	 * Sets the action row.
	 *
	 * @param value The actions, in display order.
	 * @return This object.
	 */
	public AppHeaderDef actions(HeaderAction...value) {
		actions = l(value);
		return this;
	}

	/**
	 * Sets the trailing identity chip.
	 *
	 * @param value The avatar chip.
	 * @return This object.
	 */
	public AppHeaderDef avatar(AvatarChip value) {
		avatar = value;
		return this;
	}

	/**
	 * Sets the same-origin header refresh endpoint.
	 *
	 * @param value The same-origin refresh path.
	 * @return This object.
	 */
	public AppHeaderDef refreshUrl(String value) {
		refreshUrl = value;
		return this;
	}

	/**
	 * Validates this header and returns it.
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If this header is not well-formed.
	 */
	public AppHeaderDef build() {
		validate();
		return this;
	}

	@Override /* Widget */
	public void validate() {
		if (! CONTRACT_VERSION.equals(contractVersion))
			throw iaex("AppHeaderDef contractVersion must be '%s': %s", CONTRACT_VERSION, contractVersion);
		if (id == null || id.isBlank())
			throw iaex("AppHeaderDef id must not be null or blank.");
		if (actions != null) {
			var ids = new HashSet<String>();
			for (var a : actions) {
				if (a == null)
					throw iaex("AppHeaderDef '%s' action must not be null.", id);
				a.validate();
				if (!ids.add(a.id))
					throw iaex("AppHeaderDef '%s' duplicate action id '%s'.", id, a.id);
			}
		}
		if (avatar != null)
			avatar.validate();
		if (refreshUrl != null && !SafePathTemplate.isSameOriginPath(refreshUrl))
			throw iaex("AppHeaderDef '%s' refreshUrl must be a same-origin path (no absolute URL, '//', scheme, "
				+ "'..', 'data:', or 'javascript:'): %s", id, refreshUrl);
	}
}
