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

/**
 * The row-selection opt-in for a {@link ViewTable} &mdash; the first of {@code TODO-428}'s two INDEPENDENT
 * opt-ins (design doc §9.3/§6.2; HIGH-5).
 *
 * <h5 class='section'>Independent from bulk mutation, by construction</h5>
 * <p>
 * Declaring a {@link SelectionDef} adds a per-row checkbox column (and, when {@link #selectAll()} is set, a
 * select-all header checkbox) to a view table &mdash; nothing else. It does <b>not</b>, by any default or
 * convenience, surface a bulk-mutate control: {@link ViewTable}'s selection-only overloads
 * ({@link ViewTable#of(ViewDef, java.util.Collection, SelectionDef)}, and the request-bearing sibling) have no code
 * path that can render one. A view can therefore declare "selectable, for export" while categorically excluding
 * bulk mutation &mdash; e.g. a PagerDuty-style incident table that wants selection for a client-side export/copy
 * action but must never expose a multi-row mutate. A {@link BulkMutateDef} is a separate, independent type that
 * <b>consumes</b> a {@link SelectionDef} (it requires one to be constructed at all) but a {@link SelectionDef}
 * never requires, implies, or references one back.
 *
 * <h5 class='section'>Non-wire: never touches {@code VIEW_META} or {@link ViewDef#CONTRACT_VERSION}</h5>
 * <p>
 * A {@link SelectionDef} is deliberately <b>not</b> a bean field on {@link ViewDef} and never reaches the
 * {@code VIEW_META} JSON sidecar or its {@link ViewDef#CONTRACT_VERSION}. {@link ViewTable} instead stamps it as
 * plain HTML attributes directly on the emitted {@code <table>} &mdash; {@link ViewTable#SELECT_ATTR},
 * {@link ViewTable#ROW_ID_FIELD_ATTR}, {@link ViewTable#SELECT_ALL_ATTR} &mdash; mirroring the existing
 * {@link ViewTable#CSRF_ATTR} auto-embed pattern. The {@code juneau-views.js} runtime reads them straight off the
 * DOM, so a table with no {@link SelectionDef} carries none of these attributes and renders no selection UI at
 * all; there is no shared, independently-versioned wire contract to keep in lockstep (contrast
 * {@link BulkMutateDef}, whose actual bulk-action list <i>is</i> wire data and therefore carries its own,
 * separate {@link BulkMutateDef#CONTRACT_VERSION}).
 *
 * <h5 class='section'>Selection identity is a stable row id, never a DOM/table index (MED-11)</h5>
 * <p>
 * {@link #rowIdField()} names the row-data key {@code juneau-views.js} reads to stamp a stable
 * {@code data-juneau-row-id} attribute on each rendered {@code <tr>} (see {@code createdRow} in that runtime).
 * Selection state is keyed by that value, never by a row's position in the DOM or in the DataTables draw &mdash;
 * so a poll ({@code TODO-426}), a sort, or a page change can never retarget a selection (or a bulk mutate driven
 * from one) onto whichever rows now occupy those same positions. The runtime's persistence rule drops any
 * previously-selected id that is no longer present in the newly drawn row set, so a selection whose rows have
 * scrolled off screen or off the current page is never kept as a live, actionable target.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Selectable for export, with NO bulk-mutate capability - satisfies the separability condition by construction.</jc>
 * 	SelectionDef <jv>selection</jv> = SelectionDef.<jsm>create</jsm>(<js>"id"</js>);
 * 	Div <jv>markup</jv> = ViewTable.<jsm>of</jsm>(<jv>req</jv>, <jv>viewDef</jv>, <jv>rows</jv>, <jv>selection</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link BulkMutateDef}
 * 	<li class='jc'>{@link ViewTable}
 * </ul>
 *
 * @since 10.0.0
 */
public final class SelectionDef {

	private final String rowIdField;
	private boolean selectAll = true;

	private SelectionDef(String rowIdField) {
		this.rowIdField = rowIdField;
	}

	/**
	 * Starts a new {@link SelectionDef} bound to the specified stable row-id field.
	 *
	 * @param rowIdField The row-data key whose value is this row's stable identity (e.g. {@code "id"} or
	 * 	{@code "incidentId"}). Must not be <jk>null</jk> or blank.
	 * @return A new {@link SelectionDef}, with {@link #selectAll()} defaulting to <jk>true</jk>.
	 * @throws IllegalArgumentException If {@code rowIdField} is <jk>null</jk> or blank.
	 */
	public static SelectionDef create(String rowIdField) {
		if (rowIdField == null || rowIdField.isBlank())
			throw iaex("SelectionDef rowIdField must not be null or blank.");
		return new SelectionDef(rowIdField);
	}

	/**
	 * Sets whether a select-all header checkbox is rendered alongside the per-row checkboxes.
	 *
	 * <p>
	 * Select-all always applies only to the rows currently on screen (the current page's draw) &mdash; it can
	 * never reach into rows on another page, which would be indistinguishable from the off-screen-retarget risk
	 * this whole opt-in exists to close.
	 *
	 * @param value The new value. Defaults to <jk>true</jk>.
	 * @return This object.
	 */
	public SelectionDef selectAll(boolean value) {
		selectAll = value;
		return this;
	}

	/**
	 * The row-data key used as this table's stable row identity.
	 *
	 * @return The row-id field name.
	 */
	public String rowIdField() {
		return rowIdField;
	}

	/**
	 * Whether a select-all header checkbox is rendered.
	 *
	 * @return <jk>true</jk> if select-all is enabled.
	 */
	public boolean selectAll() {
		return selectAll;
	}
}
