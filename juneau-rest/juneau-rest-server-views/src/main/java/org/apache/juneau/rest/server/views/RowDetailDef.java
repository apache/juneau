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
 * <p>
 * These section tabs always render in tab format &mdash; the client-side strip builder unconditionally stamps
 * {@code data-juneau-strip-mode="tab"}, so there is no ribbon-format (connected-pill) option for them and no
 * per-view toggle.  The base {@code juneau-views.css} supplies the folder-tab <i>shape</i> only (a
 * {@code currentColor} floor line); the visible themed look &mdash; floor color plus the selected-tab accent
 * fill &mdash; is layered by the console-ui chrome stylesheet's {@code --jc-*} tokens, so a consumer that pulls
 * in this module without that chrome layer (or equivalent {@code --jc-*} theming) gets the correct tab structure
 * but an unthemed floor and no selected-tab fill.
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

	/**
	 * The named sections, in display order.
	 *
	 * <p>
	 * Required <b>unless</b> {@link #region} is set &mdash; the two are mutually exclusive, and exactly one of them
	 * must be present.  See {@link #region} for the coexistence rules.
	 *
	 * <p>
	 * The <b>setter</b> ({@link #sections(DetailSection...)}) is deprecated in favor of {@link #region(RegionDef)};
	 * this field deliberately is not.  Deprecating the field would warn at every one of the framework's own internal
	 * read sites &mdash; the emitter, the {@code $FV} chrome pre-scan, the validation pass &mdash; all of which
	 * <b>must</b> keep reading it for the whole deprecation window, so the warning would carry no signal and would
	 * have to be suppressed everywhere it fired.  The deprecation belongs on the authoring surface.
	 */
	public List<DetailSection> sections;

	/**
	 * The <b>one</b> region this panel's body is, or <jk>null</jk> for a {@link #sections}-declaring panel.
	 *
	 * <h5 class='section'>Exactly one of {@link #sections} / {@link #region}, and neither wins over the other:</h5>
	 * <p>
	 * A bean setting <b>both</b> is rejected by {@link #validate(List)} rather than resolved by a precedence rule.
	 * A precedence rule is a state in which one of the two declarations is <i>silently unread</i>, and a bean that
	 * says both things is a bean whose author does not know which system they are in.  The loud answer is free here
	 * &mdash; both fields are on the same bean.
	 *
	 * <h5 class='section'>What the emitter does differently:</h5>
	 * <p>
	 * A {@link #sections}-declaring panel emits today's section frames, field slots and (client-side, from two or
	 * more sections) a framework-drawn section-tab strip.  A {@code region}-declaring panel emits <b>chrome plus
	 * exactly one empty region container</b> &mdash; no section frames, no field slots, and no framework strip.  The
	 * author paints the body, and draws their own strip with {@code JuneauViews.helpers.tabStrip} if they want one.
	 * Both shapes emit, in the same build, for the whole deprecation window: a view migrates when its owner is
	 * ready, not on the day this ships.
	 *
	 * <h5 class='section'>{@link ActionRef#enabledWhen} is rejected on this path:</h5>
	 * <p>
	 * The startup cross-check {@link #validateEnabledWhenFields()} performs needs a declared
	 * {@link DetailField} catalog to check <i>against</i>, and a {@code region}-declaring panel has none &mdash; its
	 * catalog is an author JS literal.  Rather than let the rules through unchecked (which is the
	 * disabled-on-every-row-forever-and-silently failure this class refuses on the {@link #sections} path), an
	 * {@code enabledWhen} rule anywhere on a {@code region}-declaring panel <b>fails at startup</b>, naming the
	 * replacement: paint the buttons inside the region body and gate them from {@code ctx.data}'s values map in the
	 * populate itself, writing through {@code ctx.write(...)}.  This rejection is <b>per bean</b>: a
	 * {@link #sections}-declaring panel keeps {@code enabledWhen}, keeps its rules, and keeps this cross-check,
	 * unchanged, for the entire window.
	 *
	 * <p>
	 * The region's {@link RegionDef#type} is set to {@link RegionDef#TYPE_ROW_DETAIL} by {@link #region(RegionDef)}
	 * &mdash; an author never sets it &mdash; and {@link RegionDef#dataUrl} is projected from {@link #endpoint} at
	 * emit time unless the author set one explicitly, so the region shares the panel's own expand GET rather than
	 * issuing a second one.
	 */
	public RegionDef region;

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
	 * @deprecated Use {@link #region(RegionDef)} instead &mdash; one region, painted by a client populator.  Still
	 * 	fully supported and still emitting for the whole deprecation window; this marks the direction, not a removal.
	 */
	@Deprecated
	public RowDetailDef sections(DetailSection...value) {
		sections = l(value);
		return this;
	}

	/**
	 * Sets the one region this panel's body is, replacing {@link #sections(DetailSection...)}.
	 *
	 * <p>
	 * Stamps {@link RegionDef#type} to {@link RegionDef#TYPE_ROW_DETAIL} on the caller's behalf: the type is the
	 * enclosing host's fact, not the region author's, and stamping it here is what lets
	 * {@link RegionDef#validate()} enforce the row-detail-scoped catalog rule (no projected {@link RegionDef#fields}
	 * &mdash; a row-detail catalog is an author JS literal) without every caller remembering to declare it.
	 *
	 * <p>
	 * See {@link #region} for the mutual exclusion with {@link #sections}, the startup XOR rejection, and the
	 * per-bean {@code enabledWhen} rejection this path carries.
	 *
	 * @param value The region.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RowDetailDef region(RegionDef value) {
		if (value == null)
			throw iaex("RowDetailDef.region(...) must not be null.");
		value.type = RegionDef.TYPE_ROW_DETAIL;
		region = value;
		return this;
	}

	/**
	 * Whether this panel declares its body as a single {@link #region} rather than as {@link #sections}.
	 *
	 * <p>
	 * The one predicate the emitter, the {@code $FV} chrome pre-scan and the validation pass all branch on, so they
	 * can never disagree about which of the two shapes a bean is.
	 *
	 * @return <jk>true</jk> if {@link #region} is set.
	 */
	public boolean isRegionBody() {
		return region != null;
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
		validateBodyShape();
		validateAllowedCustomRenderers();
		if (serverValues != null)
			serverValues.validate();
		// Cascade into the second named bar-slot host.  Cross-host id uniqueness is NOT checkable here: this scope has
		// no enclosing page, so PageDef.validate() owns that rejection.
		if (barSlot != null)
			barSlot.validate();

		var actionIds = collectActionIds(rowActions);
		validateActionBar(headerActions, actionIds);
		if (isRegionBody()) {
			region.validate();
			// The absorb, scoped to THIS bean.  A region body has no declared field catalog to cross-check
			// enabledWhen against, so the rules are rejected rather than let through unchecked.
			rejectEnabledWhen(headerActions, "the detail header");
			return;
		}
		validateSections(actionIds, enclosingViewId);
		validateEnabledWhenFields();
	}

	/**
	 * The body-shape gate: exactly one of {@link #sections} / {@link #region}, and both directions are loud.
	 *
	 * <p>
	 * Deliberately an XOR rather than a precedence rule &mdash; see {@link #region}.  The "at least one section"
	 * requirement is conditioned on {@link #region} being unset rather than removed, which is what keeps a
	 * {@link #sections}-declaring bean validating byte for byte as it does today while letting a
	 * {@code region}-declaring one construct at all.
	 */
	private void validateBodyShape() {
		var hasSections = sections != null && !sections.isEmpty();
		if (hasSections && isRegionBody())
			throw iaex("RowDetailDef declares both sections(...) and region(...); they are mutually exclusive.  "
				+ "Declare exactly one - region(...) for a client-painted body, or the deprecated sections(...) "
				+ "for the framework-painted field slots.");
		if (!hasSections && !isRegionBody())
			throw iaex("RowDetailDef must declare at least one section, or a region.");
	}

	/**
	 * Rejects every {@link ActionRef#enabledWhen} rule on a {@link #region}-declaring panel, naming the replacement.
	 *
	 * <p>
	 * The mirror image of {@link #validateEnabledWhenFields()}: that pass rejects a rule whose field <b>no</b>
	 * declared {@link DetailField} returns, and this one rejects a rule on a bean that declares <b>no catalog at
	 * all</b>.  Both refuse the same failure &mdash; a gate that would be evaluated against a field the panel never
	 * supplies, disabling its button on every row forever and doing it silently.  Ignoring the rule instead would be
	 * that exact failure arriving from the other side.
	 */
	private static void rejectEnabledWhen(ActionBar bar, String where) {
		if (bar == null || bar.items == null)
			return;
		for (var item : bar.items) {
			if (!(item instanceof ActionRef ar) || ar.enabledWhen == null || ar.enabledWhen.isEmpty())
				continue;
			throw iaex(
				"ActionRef '%s' in %s declares enabledWhen, which a region(...) detail panel does not support: "
				+ "there is no declared DetailField catalog to cross-check the rule against, so honoring it would "
				+ "disable the button on every row forever and do it silently.  Paint the button inside the region "
				+ "body instead and gate it from ctx.data's values map in your populate, writing through "
				+ "ctx.write(...).", ar.id, where);
		}
	}

	/**
	 * Cross-checks every {@link ActionRef} row-state rule's field against the {@link DetailField} data keys this
	 * panel actually declares.
	 *
	 * <p>
	 * A rule keyed on a field the expand GET never returns would disable that button on every row forever, and
	 * would do it silently &mdash; the runtime fails closed on a field it cannot find, which is the right runtime
	 * behaviour and the wrong startup behaviour.  So the typo fails loud here instead, at the only point that can
	 * see both the rule and the field catalog.
	 *
	 * <p>
	 * Deliberately a pass of its own, run after the section walk rather than inside it: the expand GET returns
	 * <b>every</b> declared field, so a rule in the first section may legally key on a field the last one declares.
	 * Checking mid-walk would reject that against a half-built catalog.
	 */
	private void validateEnabledWhenFields() {
		var dataKeys = new HashSet<String>();
		for (var s : sections)
			if (s.fields != null)
				for (var f : s.fields)
					if (f.data != null)
						dataKeys.add(f.data);
		validateEnabledWhenFields(headerActions, dataKeys, "the detail header");
		for (var s : sections)
			validateEnabledWhenFields(s.actions, dataKeys, "section '" + s.id + "'");
		// The third bar host.  Omitting this pass would leave a field-hosted bar's rules the only ones never
		// cross-checked, and the runtime fails closed on a field it cannot find - right at runtime, silent forever
		// at startup.
		for (var s : sections)
			if (s.fields != null)
				for (var f : s.fields)
					validateEnabledWhenFields(f.actions, dataKeys, "field '" + f.data + "'");
	}

	private static void validateEnabledWhenFields(ActionBar bar, Set<String> dataKeys, String where) {
		if (bar == null || bar.items == null)
			return;
		for (var item : bar.items) {
			if (!(item instanceof ActionRef ar) || ar.enabledWhen == null)
				continue;
			for (var r : ar.enabledWhen)
				if (r != null && !dataKeys.contains(r.field))
					throw iaex(
						"ActionRef '%s' in %s declares enabledWhen on field '%s', which no DetailField of this panel returns.",
						ar.id, where, r.field);
		}
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
		if (s.count != null && s.count < 0)
			throw iaex("DetailSection '%s' count must be >= 0.", s.id);
		validateSectionFields(s, fieldKeys, actionIds);
		validateActionBar(s.actions, actionIds);
		validateNestedTable(s, nestedViewIds, enclosingViewId);
	}

	private void validateSectionFields(DetailSection s, Set<String> fieldKeys, Set<String> actionIds) {
		if (s.fields == null)
			return;
		for (var f : s.fields)
			validateDetailField(f, s.id, fieldKeys, actionIds);
	}

	private void validateDetailField(DetailField f, String sectionId, Set<String> fieldKeys, Set<String> actionIds) {
		if (f == null)
			throw iaex("DetailSection '%s' field must not be null.", sectionId);
		if (f.data == null || f.data.isBlank())
			throw iaex("DetailSection '%s' field data must not be null or blank.", sectionId);
		if (!fieldKeys.add(f.data))
			throw iaex("RowDetailDef duplicate field data key '%s'.", f.data);
		// EVERY actions check belongs above the render early-return below.  A field carrying a bar and no renderer
		// is the ordinary case, so a rule written under that return would be dead code for exactly the shape it
		// exists to reject - and would still pass a new test that happened to set a renderer too.
		validateFieldActions(f);
		validateActionBar(f.actions, actionIds);
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

	/**
	 * Rejects a field that hosts an {@link ActionBar} on a title-suppressed {@link DetailField.Format#MARKDOWN}
	 * body: an empty {@link DetailField#title} turns the block into a full-bleed prose column, and a bar in the
	 * value slot wants that same block.  Both want the whole row, so declaring both is an authoring error rather
	 * than a layout the stylesheet could arbitrate.
	 *
	 * <p>
	 * Keyed on the bar being <b>declared</b>, not on it having items: a bar attached to a full-bleed prose body
	 * is the mistake whether or not it happens to be empty on the day it is written.  Both rich-text formats
	 * ({@link DetailField.Format#MARKDOWN} and {@link DetailField.Format#SANITIZED_HTML}) are full-bleed the same
	 * way, so both are rejected the same way.
	 */
	private static void validateFieldActions(DetailField f) {
		if (f.actions == null)
			return;
		if (f.render != null || f.title == null || !f.title.isEmpty())
			return;
		if (f.format == DetailField.Format.MARKDOWN || f.format == DetailField.Format.SANITIZED_HTML)
			throw iaex("DetailField '%s' cannot host actions on a title-suppressed %s body.", f.data, f.format);
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
