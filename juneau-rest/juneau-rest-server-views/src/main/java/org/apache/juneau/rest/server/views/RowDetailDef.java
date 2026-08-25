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
 * The row-details expander definition: named sections, an expand GET endpoint, an optional IRS-style header
 * (title template, icon, and {@link ActionBar} above section tabs), and optional per-section {@link ActionBar}s.
 *
 * <p>
 * Structure is emitted as a {@code <template data-juneau-row-detail>} sibling of the view table; field values
 * arrive via a same-origin GET.  {@link DetailField.Format#TEXT} (the default) paints with {@code textContent};
 * {@link DetailField.Format#MARKDOWN} copies allowlisted nodes from a {@code DOMParser} document and never
 * assigns {@code innerHTML}.  This type is Java-only &mdash; it is not part of the {@code VIEW_META} JSON sidecar.
 *
 * <p>
 * Two or more {@link #sections} become a tab-mode strip under the header (Details / Diagnose / &hellip;), the
 * same nested-ribbon pattern IRS Instances uses.  When {@link #title}, {@link #icon}, and/or
 * {@link #headerActions} are set, the template emits a {@code .juneau-view-detail-header} above that strip.
 * {@link #title} may contain <code>{field}</code> placeholders filled from the expand GET {@code fields} map
 * via {@code textContent}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class RowDetailDef {

	/** The frozen contract version for the expand GET envelope and the stamped {@code data-juneau-detail-contract}. */
	public static final String CONTRACT_VERSION = "1";

	/** Same-origin path template; {@code {id}} is substituted with {@code encodeURIComponent(rowId)}. */
	public String endpoint;

	/** The named sections, in display order.  At least one is required. */
	public List<DetailSection> sections;

	/**
	 * Optional expander-panel title.  May contain <code>{field}</code> placeholders filled from the expand GET
	 * {@code fields} map (plain text).  {@code null} or blank omits the title slot.
	 */
	public String title;

	/**
	 * Optional icon name resolved by the views icon registry (same names as ribbon buttons).  Painted to the
	 * left of {@link #title}.  Unknown names hide the slot at runtime.
	 */
	public String icon;

	/**
	 * Optional action bar in the detail header, above section tabs.  {@code null} / empty omits header actions.
	 * {@link ActionRef} ids are validated against the enclosing view's {@code rowActions}.
	 */
	public ActionBar headerActions;

	/**
	 * App-approved custom renderer ids allowed on {@link DetailField#render} in addition to
	 * {@link SinkRenderAllowlist#BUILTIN_IDS}.  Blank entries fail {@link #validate(List)}.
	 */
	public Set<String> allowedCustomRenderers;

	/**
	 * Author-declared server-side scalar values interpolated into <b>this panel's own</b> chrome (titles) as
	 * <js>"$FV{name}"</js> at serve time.  Java-only, like the rest of this type &mdash; lambda providers never
	 * marshal, and this does not bump {@link #CONTRACT_VERSION}.
	 *
	 * <p>
	 * Resolution happens at <b>parent paint time</b>, in the {@link ViewTable} {@code <template>} emit path, against a
	 * per-response <b>sibling</b> {@link org.apache.juneau.commons.svl.VarResolverSession} carrying its own registry.
	 * The panel's labels are painted into the server-emitted {@code <template>}, so the expand GET carries row data
	 * only and has no chrome left to resolve; a {@code $FV} template must never reach the expand envelope.  The closed
	 * allowlist is {@link #title}, {@link DetailSection#title}, and {@link DetailField#title} &mdash; nothing else.
	 * {@link #icon} is an icon-registry name, {@link ActionRef} is an id, and {@link SafeAction} is an enum, so none of
	 * them are interpolated.
	 *
	 * <h5 class='section'>There is no inheritance, and same-name collisions are legal:</h5>
	 * <p>
	 * This host resolves only its own allowlisted fields.  It neither sees nor is seen by the enclosing
	 * {@link PageDef#serverValues} or the enclosing {@link ViewDef#serverValues}; all three sessions are
	 * <b>siblings</b>.  A name declared here and a same-name value on either of those hosts resolve independently and
	 * may differ within one response &mdash; a documented semantic, not an accident.
	 */
	public ServerValues serverValues;

	/**
	 * Optional additive bar slot riding this panel's <b>detail ribbon</b> &mdash; the <i>second</i> named
	 * {@link BarSlot} attachment, distinct from {@link PageDef#barSlot}.
	 *
	 * <p>
	 * Same bean, same {@link BarSlotTable} emitter, different host and placement: the page slot trails
	 * {@code .jc-subtab-bar} once per page, while this one is painted into the row-expand {@code <template>} and is
	 * therefore <b>cloned per expanded row</b>, trailing the ribbon the runtime assembles from this panel's sections.
	 * A single-section panel has no ribbon and none is synthesized for it; the region is anchored to that lone
	 * section's title instead ({@link BarSlotTable#ANCHOR_SECTION_TITLE}).
	 *
	 * <p>
	 * Java-only, like the rest of this type &mdash; it never appears on the expand GET envelope, so it does not bump
	 * {@link #CONTRACT_VERSION}.  {@link BarSlot#id} stays the author's own id: it is what the enclosing
	 * {@link PageDef} uniqueness check compares, and the runtime mints per-row DOM identities from the parent table's
	 * id rather than from it.  {@link BarSlot#refreshUrl} powers <b>demand</b> refresh only; there is no poller.
	 *
	 * <p>
	 * Cross-host id collisions are rejected by {@link PageDef#validate()} &mdash; the only scope that sees both hosts.
	 * A top-level view served with no enclosing page has no page slot to collide with, and is a legal no-op.
	 */
	public BarSlot barSlot;

	/**
	 * Private lock object guarding the {@code $FV} chrome-resolution window this instance may be mutated under
	 * (see {@link ViewTable}).  Synchronizing on this dedicated object rather than on {@code this} keeps the
	 * monitor private to the toolkit even though callers hold a reference to the bean itself.  Not a wire field.
	 */
	final Object lock = new Object();

	/**
	 * Creates an empty row-detail definition.
	 *
	 * @return A new {@link RowDetailDef}.
	 */
	public static RowDetailDef create() {
		return new RowDetailDef();
	}

	/**
	 * Sets the expand GET path template.
	 *
	 * @param value A same-origin path template containing {@code {id}}.  Must not be <jk>null</jk> or blank.
	 * @return This object.
	 */
	public RowDetailDef endpoint(String value) {
		endpoint = value;
		return this;
	}

	/**
	 * Sets the named sections.
	 *
	 * @param value The sections, in display order.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RowDetailDef sections(DetailSection...value) {
		sections = l(value);
		return this;
	}

	/**
	 * Sets the expander-panel header title.
	 *
	 * @param value Title text, optionally with <code>{field}</code> placeholders.  Blank / {@code null} omits
	 * 	the title slot.
	 * @return This object.
	 */
	public RowDetailDef title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets the expander-panel header icon name.
	 *
	 * @param value An icon registry name.  Blank / {@code null} omits the icon slot.
	 * @return This object.
	 */
	public RowDetailDef icon(String value) {
		icon = value;
		return this;
	}

	/**
	 * Sets the header action bar (right of the title, above section tabs).
	 *
	 * @param value The action bar.  May be <jk>null</jk> (no header actions).
	 * @return This object.
	 */
	public RowDetailDef headerActions(ActionBar value) {
		headerActions = value;
		return this;
	}

	/**
	 * Opts in custom (non-built-in) renderer ids for {@link DetailField#render}.
	 *
	 * <p>
	 * Opt-in is id permission only: the custom renderer's HTML still goes through the closed
	 * {@code fillRenderSlot} copier.  Opting in a built-in id is a no-op.
	 *
	 * @param value Custom renderer ids.  Must not contain blank entries.
	 * @return This object.
	 */
	public RowDetailDef allowCustomRenderers(String...value) {
		allowedCustomRenderers = st(value);
		return this;
	}

	/**
	 * Declares the server-side scalar values interpolated into this panel's own titles as <js>"$FV{name}"</js>.
	 *
	 * <p>
	 * See {@link #serverValues} &mdash; resolved at parent paint time into the emitted {@code <template>}, never at
	 * expand-GET time, and never inherited from (or by) the enclosing page or view.
	 *
	 * @param value The server-values declaration.  Can be <jk>null</jk> (no {@code $FV} interpolation).
	 * @return This object.
	 */
	public RowDetailDef serverValues(ServerValues value) {
		serverValues = value;
		return this;
	}

	/**
	 * Declares the additive bar slot riding this panel's detail ribbon.
	 *
	 * <p>
	 * See {@link #barSlot} &mdash; a second named host for the same {@link BarSlot} bean, not a re-use of
	 * {@link PageDef#barSlot}.
	 *
	 * @param value The bar slot.  Can be <jk>null</jk> (no detail bar slot).
	 * @return This object.
	 */
	public RowDetailDef barSlot(BarSlot value) {
		barSlot = value;
		return this;
	}

	/**
	 * Fail-closed bean validation, including {@link ActionRef} existence against the enclosing view's action
	 * catalog.
	 *
	 * @param rowActions The enclosing {@link ViewDef#rowActions}, or <jk>null</jk> (any {@link ActionRef} then
	 * 	fails).
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	public void validate(List<RowAction> rowActions) {
		validate(rowActions, null);
	}

	/**
	 * Fail-closed bean validation, additionally validating nested {@link DetailSection#table}s and enforcing that a
	 * nested view id neither duplicates another nested view id nor collides with the enclosing view id.
	 *
	 * @param rowActions The enclosing {@link ViewDef#rowActions}, or <jk>null</jk> (any {@link ActionRef} then
	 * 	fails).
	 * @param enclosingViewId The enclosing {@link ViewDef#id}, or <jk>null</jk> to skip the parent-id collision check.
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	public void validate(List<RowAction> rowActions, String enclosingViewId) {
		validateEndpoint();
		if (sections == null || sections.isEmpty())
			throw iaex("RowDetailDef must declare at least one section.");
		validateAllowedCustomRenderers();
		if (serverValues != null)
			serverValues.validate();
		// Cascade into the second named bar-slot host.  Cross-host id uniqueness is NOT checkable here: this scope has
		// no enclosing page, so PageDef.validate() owns that rejection.
		if (barSlot != null)
			barSlot.validate();

		var actionIds = collectActionIds(rowActions);
		validateActionBar(headerActions, actionIds);
		validateSections(actionIds, enclosingViewId);
	}

	private void validateEndpoint() {
		if (endpoint == null || endpoint.isBlank())
			throw iaex("RowDetailDef endpoint must not be null or blank.");
		if (!endpoint.contains("{id}"))
			throw iaex("RowDetailDef endpoint must contain '{id}'.");
		if (!isSafeDetailEndpoint(endpoint))
			throw iaex("RowDetailDef endpoint must be a same-origin path template (no absolute URL, '..', or scheme): %s",
				endpoint);
	}

	private void validateAllowedCustomRenderers() {
		if (allowedCustomRenderers == null)
			return;
		for (var id : allowedCustomRenderers)
			if (id == null || id.isBlank())
				throw iaex("allowCustomRenderers entry must not be blank.");
	}

	private static Set<String> collectActionIds(List<RowAction> rowActions) {
		var actionIds = new HashSet<String>();
		if (rowActions != null)
			for (var a : rowActions)
				if (a != null && a.id != null)
					actionIds.add(a.id);
		return actionIds;
	}

	private void validateSections(Set<String> actionIds, String enclosingViewId) {
		var sectionIds = new HashSet<String>();
		var fieldKeys = new HashSet<String>();
		var nestedViewIds = new HashSet<String>();
		for (var s : sections)
			validateSection(s, actionIds, sectionIds, fieldKeys, nestedViewIds, enclosingViewId);
	}

	private void validateSection(DetailSection s, Set<String> actionIds, Set<String> sectionIds, Set<String> fieldKeys,
			Set<String> nestedViewIds, String enclosingViewId) {
		if (s == null)
			throw iaex("RowDetailDef section must not be null.");
		if (s.id == null || s.id.isBlank())
			throw iaex("DetailSection id must not be null or blank.");
		if (!sectionIds.add(s.id))
			throw iaex("RowDetailDef duplicate section id '%s'.", s.id);
		if (s.columns < 1)
			throw iaex("DetailSection '%s' columns must be >= 1.", s.id);
		validateSectionFields(s, fieldKeys);
		validateActionBar(s.actions, actionIds);
		validateNestedTable(s, nestedViewIds, enclosingViewId);
	}

	private void validateSectionFields(DetailSection s, Set<String> fieldKeys) {
		if (s.fields == null)
			return;
		for (var f : s.fields)
			validateDetailField(f, s.id, fieldKeys);
	}

	private void validateDetailField(DetailField f, String sectionId, Set<String> fieldKeys) {
		if (f == null)
			throw iaex("DetailSection '%s' field must not be null.", sectionId);
		if (f.data == null || f.data.isBlank())
			throw iaex("DetailSection '%s' field data must not be null or blank.", sectionId);
		if (!fieldKeys.add(f.data))
			throw iaex("RowDetailDef duplicate field data key '%s'.", f.data);
		if (f.render == null)
			return;
		if (f.render.id == null || f.render.id.isBlank())
			throw iaex("DetailField '%s' render id must not be null or blank.", f.data);
		if (f.format != null && f.format != DetailField.Format.TEXT)
			throw iaex("DetailField '%s' cannot set both render and a non-TEXT format.", f.data);
		SinkRenderAllowlist.assertAllowed(f.render.id, allowedCustomRenderers);
		if ("pill".equals(f.render.id))
			ViewDef.validateSinkPill(f.render, sectionId + "." + f.data);
	}

	private static void validateNestedTable(DetailSection s, Set<String> nestedViewIds, String enclosingViewId) {
		if (s.table == null)
			return;
		s.table.validate();
		var nid = s.table.view.id;
		if (enclosingViewId != null && enclosingViewId.equals(nid))
			throw iaex("DetailSection '%s' nested table view id '%s' collides with the enclosing view id.", s.id, nid);
		if (!nestedViewIds.add(nid))
			throw iaex("RowDetailDef duplicate nested table view id '%s'.", nid);
	}

	private static void validateActionBar(ActionBar bar, Set<String> actionIds) {
		if (bar == null)
			return;
		bar.validate();
		if (bar.items == null)
			return;
		for (var item : bar.items) {
			if (item instanceof ActionRef ar && !actionIds.contains(ar.id))
				throw iaex("ActionRef '%s' is not declared on the enclosing view's rowActions.", ar.id);
		}
	}

	/**
	 * Whether {@code endpoint} is a same-origin path template: no {@code ://} , no {@code //} prefix, no scheme
	 * colon-before-slash, and no {@code ..} path segments.
	 *
	 * @param endpoint The candidate template.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a same-origin path template.
	 */
	public static boolean isSafeDetailEndpoint(String endpoint) {
		if (endpoint == null || endpoint.isBlank())
			return false;
		if (endpoint.contains("://"))
			return false;
		if (endpoint.startsWith("//"))
			return false;
		var colon = endpoint.indexOf(':');
		var slash = endpoint.indexOf('/');
		if (colon >= 0 && (slash < 0 || colon < slash))
			return false;
		for (var seg : endpoint.split("/", -1)) {
			if ("..".equals(seg))
				return false;
		}
		return true;
	}
}
