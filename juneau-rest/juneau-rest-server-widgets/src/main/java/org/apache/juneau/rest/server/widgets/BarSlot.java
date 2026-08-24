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
 * An additive trailing region beside the subtab ribbon: a small row of {@link BarWidget}s (concept #9).
 *
 * <p>
 * A pure data bean.  The views emitter renders a {@code data-juneau-bar-slot} region as a <b>trailing sibling of
 * {@code .jc-subtab-bar}</b> (never into the archived {@code .juneau-view-toolbar-*} DataTables control row) and, only
 * when the slot is non-empty, a tiny JSON sidecar for any dynamic counts.  The accessor ({@code PageDef.barSlot(...)})
 * is a Java-only builder field in the views module &mdash; omitted from the wire, and it does not bump any
 * {@code CONTRACT_VERSION}.
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,id,widgets")
public class BarSlot implements Widget {

	/** The frozen contract version for this widget.  Serialized as the JSON <b>string</b> {@code "1"}. */
	public static final String CONTRACT_VERSION = "1";

	/** The contract version; must equal {@link #CONTRACT_VERSION} at validation time. */
	public String contractVersion = CONTRACT_VERSION;

	/** The stable bar-slot id; sidecar id ({@code id="juneau-bar:<id>"}).  Required, non-blank. */
	public String id;

	/** The bar widgets, in display order.  At least one is required; ids must be unique within the slot. */
	public List<BarWidget> widgets;

	/** Optional same-origin refresh endpoint owning the bar sidecar's demand-refresh.  Omit = no live refresh. */
	public String refreshUrl;

	/**
	 * Creates an empty bar slot with the given id.
	 *
	 * @param id The stable bar-slot id.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link BarSlot}.
	 */
	public static BarSlot create(String id) {
		var b = new BarSlot();
		b.id = id;
		return b;
	}

	/**
	 * Sets the bar widgets, in display order.
	 *
	 * @param value The widgets.
	 * @return This object.
	 */
	public BarSlot widgets(BarWidget...value) {
		widgets = l(value);
		return this;
	}

	/**
	 * Sets the same-origin bar refresh endpoint.
	 *
	 * @param value The same-origin refresh path.
	 * @return This object.
	 */
	public BarSlot refreshUrl(String value) {
		refreshUrl = value;
		return this;
	}

	@Override /* Widget */
	public void validate() {
		if (! CONTRACT_VERSION.equals(contractVersion))
			throw iaex("BarSlot contractVersion must be '%s': %s", CONTRACT_VERSION, contractVersion);
		if (id == null || id.isBlank())
			throw iaex("BarSlot id must not be null or blank.");
		if (widgets == null || widgets.isEmpty())
			throw iaex("BarSlot '%s' must declare at least one widget.", id);
		var ids = new HashSet<String>();
		for (var w : widgets) {
			if (w == null)
				throw iaex("BarSlot '%s' widget must not be null.", id);
			var wid = validateWidget(w);
			if (!ids.add(wid))
				throw iaex("BarSlot '%s' duplicate widget id '%s'.", id, wid);
		}
		if (refreshUrl != null && !SafePathTemplate.isSameOriginPath(refreshUrl))
			throw iaex("BarSlot '%s' refreshUrl must be a same-origin path (no absolute URL, '//', scheme, "
				+ "'..', 'data:', or 'javascript:'): %s", id, refreshUrl);
	}

	/**
	 * Fail-closed per-widget validation via total {@code instanceof} dispatch over the sealed {@link BarWidget}.
	 *
	 * @param w The widget.
	 * @return The widget's id (for uniqueness checking).
	 */
	private String validateWidget(BarWidget w) {
		if (w instanceof BarBadge b) {
			if (b.id == null || b.id.isBlank())
				throw iaex("BarSlot '%s' BarBadge id must not be null or blank.", id);
			if (b.badge != null)
				b.badge.validate();
			return b.id;
		}
		if (w instanceof BarText t) {
			if (t.id == null || t.id.isBlank())
				throw iaex("BarSlot '%s' BarText id must not be null or blank.", id);
			if (t.text == null || t.text.isBlank())
				throw iaex("BarSlot '%s' BarText '%s' text must not be null or blank.", id, t.id);
			return t.id;
		}
		throw iaex("BarSlot '%s' unknown widget type: %s", id, w.getClass().getName());
	}
}
