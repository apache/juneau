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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * A single top-level tab within a {@link PageDef}, referencing either one child {@link ViewDef} directly or an
 * ordered list of {@link Subtab} (TODO-399 Phase C, design doc §"Bean model").
 *
 * <p>
 * A {@link Tab} carries a stable {@code id} (the second hash segment, {@code #pageId/<tabId>/...}) and {@code label}
 * (the tab-bar button text), plus <b>exactly one</b> of {@link #view} or {@link #subtabs} &mdash; enforced by
 * {@link PageDef#build()}, not by the individual setters, so builder call order stays unconstrained.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link PageDef}
 * 	<li class='jc'>{@link Subtab}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="id,label,view,subtabs")
public class Tab {

	/** The stable tab id (the second hash segment), unique across the page. */
	public String id;

	/** The tab-bar button text. */
	public String label;

	/** The referenced child view (mutually exclusive with {@link #subtabs}). */
	public ViewDef view;

	/** The ordered sub-tabs (mutually exclusive with {@link #view}). */
	public List<Subtab> subtabs;

	/**
	 * Starts a new {@link Tab} builder with the specified stable id and display label.
	 *
	 * @param id The stable tab id.  Must not be <jk>null</jk> or blank.
	 * @param label The tab-bar button text.
	 * @return A new mutable {@link Tab} to chain builder calls on.
	 */
	public static Tab create(String id, String label) {
		if (id == null || id.isBlank())
			throw iaex("Tab id must not be null or blank.");
		var t = new Tab();
		t.id = id;
		t.label = label;
		return t;
	}

	/**
	 * Sets the referenced child view (a leaf tab with no sub-tabs).
	 *
	 * @param value The built child view.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Tab view(ViewDef value) {
		view = value;
		return this;
	}

	/**
	 * Sets the ordered sub-tabs.
	 *
	 * @param value The sub-tabs, in display order.
	 * @return This object.
	 */
	public Tab subtabs(Subtab...value) {
		subtabs = new ArrayList<>(Arrays.asList(value));
		return this;
	}

	/**
	 * Validates this tab in isolation (design doc §"Bean model" validation rules): exactly one of {@link #view}/
	 * {@link #subtabs} must be set, and sub-tab ids must be unique <b>within this tab</b> (a sub-tab id may safely
	 * recur under a different tab &mdash; the hash's {@code tabId} segment already disambiguates the parent before
	 * {@code subtabId} is ever resolved).
	 *
	 * @throws IllegalArgumentException On any rule violation.
	 */
	void validate() {
		var hasView = view != null;
		var hasSubtabs = subtabs != null && !subtabs.isEmpty();
		if (hasView == hasSubtabs)
			throw iaex("Tab '%s' must declare exactly one of view or subtabs.", id);
		if (hasSubtabs) {
			var ids = new HashSet<String>();
			for (var s : subtabs) {
				if (s.view == null)
					throw iaex("Subtab '%s' in tab '%s' must declare a view.", s.id, id);
				if (!ids.add(s.id))
					throw iaex("Tab '%s': duplicate subtab id '%s'.", id, s.id);
			}
		}
	}
}
