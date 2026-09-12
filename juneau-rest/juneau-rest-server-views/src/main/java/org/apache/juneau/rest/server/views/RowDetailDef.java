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
 * The row-details expander definition: one {@link #region} body, an expand GET endpoint, and optional header
 * chrome (title template and icon).
 *
 * <p>
 * Structure is emitted as a {@code <template data-juneau-row-detail>} sibling of the view table.  The panel body
 * is exactly one empty region container; the author paints it with a named populator
 * ({@code JuneauViews.regions.register}) using helpers such as {@code fieldGrid} / {@code tabStrip}.
 * {@link FieldFormat#TEXT} (the default catalog format) paints with {@code textContent};
 * {@link FieldFormat#MARKDOWN} copies allowlisted nodes from a {@code DOMParser} document and never
 * assigns {@code innerHTML}.  This type is Java-only &mdash; it is not part of the {@code VIEW_META} JSON sidecar.
 *
 * <p>
 * When {@link #title} and/or {@link #icon} are set, the template emits a
 * {@code .juneau-view-detail-header} above the region.  {@link #title} may contain <code>{field}</code>
 * placeholders filled from the expand GET {@code fields} map via {@code textContent}, but only for keys the
 * region's {@link RegionDef#titleFields} allowlist names.
 *
 * <p>
 * Nested-table seeding inside a row-detail panel is deferred (F24); {@link #CONTRACT_VERSION} stays {@code "1"}
 * until that consumer of the expand envelope has a replacement.
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
	 * The <b>one</b> region this panel's body is.  Required.
	 *
	 * <p>
	 * The emitter paints chrome plus exactly one empty region container &mdash; no section frames, no field slots,
	 * and no framework strip.  The author paints the body, and draws their own strip with
	 * {@code JuneauViews.helpers.tabStrip} if they want one.  Panel-header action bars are not a framework
	 * surface: authors paint buttons inside the region body and gate them from {@code ctx.data}'s values map
	 * in the populate itself, writing through {@code ctx.write(...)}.
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
	 * {@code fields} map (plain text), restricted to {@link RegionDef#titleFields}.  {@code null} or blank omits
	 * the title slot.
	 */
	public String title;

	/**
	 * Optional icon name resolved by the views icon registry (same names as ribbon buttons).  Painted to the
	 * left of {@link #title}.  Unknown names hide the slot at runtime.
	 */
	public String icon;

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
	 * allowlist is {@link #title} &mdash; nothing else.  {@link #icon} is an icon-registry name, so it is not
	 * interpolated.
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
	 * Optional additive bar slot riding this panel's region body &mdash; the <i>second</i> named
	 * {@link BarSlot} attachment, distinct from {@link PageDef#barSlot}.
	 *
	 * <p>
	 * Same bean, same {@link BarSlotTable} emitter, different host and placement: the page slot trails
	 * {@code .jc-subtab-bar} once per page, while this one is painted into the row-expand {@code <template>} and is
	 * therefore <b>cloned per expanded row</b>, anchored with {@link BarSlotTable#ANCHOR_SECTION_TITLE} because a
	 * region panel has no framework ribbon.
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
	 * Sets the one region this panel's body is.
	 *
	 * <p>
	 * Stamps {@link RegionDef#type} to {@link RegionDef#TYPE_ROW_DETAIL} on the caller's behalf: the type is the
	 * enclosing host's fact, not the region author's, and stamping it here is what lets
	 * {@link RegionDef#validate()} enforce the row-detail-scoped catalog rule (no projected {@link RegionDef#fields}
	 * &mdash; a row-detail catalog is an author JS literal) without every caller remembering to declare it.
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
	 * Whether this panel declares its body as a {@link #region}.
	 *
	 * <p>
	 * Always <jk>true</jk> for a well-formed bean after {@link #validate(List)}; kept so the emitter, the
	 * {@code $FV} chrome pre-scan and tests share one predicate.
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
	 * Declares the additive bar slot riding this panel's region body.
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
	 * Fail-closed bean validation.
	 *
	 * @param rowActions The enclosing {@link ViewDef#rowActions}.  Unused after {@code headerActions} retired;
	 * 	kept so existing {@link ViewDef} call sites do not change signature.
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	@SuppressWarnings({
		"unused" // rowActions kept so ViewDef call sites do not change signature after headerActions retired.
	})
	public void validate(List<RowAction> rowActions) {
		validate(rowActions, null);
	}

	/**
	 * Fail-closed bean validation.
	 *
	 * @param rowActions The enclosing {@link ViewDef#rowActions}.  Unused after {@code headerActions} retired;
	 * 	kept so existing {@link ViewDef} call sites do not change signature.
	 * @param enclosingViewId The enclosing {@link ViewDef#id}.  Unused after nested-in-section emit retired (F24);
	 * 	kept so existing {@link ViewDef} call sites do not change signature.
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	@SuppressWarnings({
		"unused" // rowActions / enclosingViewId kept so ViewDef call sites do not change signature.
	})
	public void validate(List<RowAction> rowActions, String enclosingViewId) {
		validateEndpoint();
		if (region == null)
			throw iaex("RowDetailDef must declare a region.");
		if (serverValues != null)
			serverValues.validate();
		// Cascade into the second named bar-slot host.  Cross-host id uniqueness is NOT checkable here: this scope has
		// no enclosing page, so PageDef.validate() owns that rejection.
		if (barSlot != null)
			barSlot.validate();
		region.validate();
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
