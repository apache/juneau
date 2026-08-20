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

import org.apache.juneau.rest.server.widgets.*;

/**
 * A named subsection of a {@link RowDetailDef} expander panel.
 *
 * @since 10.0.0
 */
public class DetailSection {

	/** Stable section id, unique within the enclosing {@link RowDetailDef}. */
	public String id;

	/** Heading painted with {@code textContent}. */
	public String title;

	/** CSS-grid column hint; default {@code 2}.  Must be {@code >= 1}. */
	public int columns = 2;

	/** The field slots, in display order. */
	public List<DetailField> fields;

	/** Optional per-section action bar. */
	public ActionBar actions;

	/**
	 * Creates a section with the specified id and title.
	 *
	 * @param id The section id.  Must not be <jk>null</jk> or blank.
	 * @param title The heading.  May be <jk>null</jk> (the id is used as a fallback at emit time).
	 * @return A new {@link DetailSection}.
	 */
	public static DetailSection create(String id, String title) {
		if (id == null || id.isBlank())
			throw iaex("DetailSection id must not be null or blank.");
		var s = new DetailSection();
		s.id = id;
		s.title = title;
		return s;
	}

	/**
	 * Sets the CSS-grid column count.
	 *
	 * @param value The column count.  Must be {@code >= 1} (enforced by {@link RowDetailDef#validate(List)}).
	 * @return This object.
	 */
	public DetailSection columns(int value) {
		columns = value;
		return this;
	}

	/**
	 * Sets the field slots.
	 *
	 * @param value The fields, in display order.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public DetailSection fields(DetailField...value) {
		fields = l(value);
		return this;
	}

	/**
	 * Sets the optional action bar.
	 *
	 * @param value The action bar.  May be <jk>null</jk> (no bar).
	 * @return This object.
	 */
	public DetailSection actions(ActionBar value) {
		actions = value;
		return this;
	}
}
