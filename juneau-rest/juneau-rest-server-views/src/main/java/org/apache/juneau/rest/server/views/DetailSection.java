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
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class DetailSection {

	/** Stable section id, unique within the enclosing {@link RowDetailDef}. */
	public String id;

	/** Heading painted with {@code textContent}. */
	public String title;

	/**
	 * The <b>maximum</b> number of grid columns; default {@code 2}.  Must be {@code >= 1}.
	 *
	 * <p>
	 * The rendered count steps down with the width of the detail panel, so this is a cap rather than a fixed
	 * count: a section declaring {@code 4} renders four columns only where the panel is wide enough for them,
	 * and one column at the narrowest step.  The framework's ladder tops out at four, so a larger value renders
	 * as four.
	 */
	public int columns = 2;

	/**
	 * How this section's fields arrange their label and value.  <jk>null</jk> means {@link FieldLayout#INLINE}.
	 */
	public FieldLayout layout;

	/**
	 * Optional count rendered after this section's tab label, as a distinct element.
	 *
	 * <p>
	 * <jk>null</jk> renders no suffix; <c>0</c> renders, because "checked, none" is information rather than an
	 * empty state.  Painted with {@code textContent} into its own element by the client strip builder, so it is
	 * never folded into the section heading &mdash; a single-section detail renders no strip at all and would
	 * otherwise read {@code "Suspensions (0)"} as its heading.
	 *
	 * <p>
	 * Definition data, not payload: it never enters the expand-GET envelope.
	 */
	public Integer count;

	/** The field slots, in display order. */
	public List<DetailField> fields;

	/** Optional per-section action bar. */
	public ActionBar actions;

	/**
	 * Optional nested, read-only table.
	 *
	 * <p>
	 * When set, {@link ViewTable} appends a nested-table shell after this section's fields and the
	 * {@code juneau-views.js} runtime instantiates it (scoped to the parent row) once the detail GET succeeds and
	 * this section's pane becomes visible.  Validated by {@link RowDetailDef#validate(List, String)}.
	 */
	public NestedTableDef table;

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
	 * Sets the maximum CSS-grid column count.
	 *
	 * @param value The column cap.  Must be {@code >= 1} (enforced by {@link RowDetailDef#validate(List)}).
	 * 	Values above four render as four.
	 * @return This object.
	 */
	public DetailSection columns(int value) {
		columns = value;
		return this;
	}

	/**
	 * Sets how this section's fields arrange their label and value.
	 *
	 * @param value The arrangement.  May be <jk>null</jk> ({@link FieldLayout#INLINE}).
	 * @return This object.
	 */
	public DetailSection layout(FieldLayout value) {
		layout = value;
		return this;
	}

	/**
	 * Sets the optional count rendered after this section's tab label.
	 *
	 * @param value The count.  May be <jk>null</jk> (no suffix).  Numbers only &mdash; a general-purpose text
	 * 	badge is a separate feature.
	 * @return This object.
	 */
	public DetailSection count(Integer value) {
		count = value;
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

	/**
	 * Sets the optional nested read-only table.
	 *
	 * @param value The nested table.  May be <jk>null</jk> (no nested table).
	 * @return This object.
	 */
	public DetailSection table(NestedTableDef value) {
		table = value;
		return this;
	}
}
