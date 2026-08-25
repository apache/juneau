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
 * The bulk-mutate opt-in for a {@link ViewTable} &mdash; the second, INDEPENDENT half of the row-selection /
 * bulk-mutate separability guarantee (design doc §9.3/§6.2; HIGH-5).
 *
 * <h5 class='section'>Two required parameters make it structurally, not conventionally, safe</h5>
 * <p>
 * There is no zero-argument (or permit-less) way to obtain a {@link BulkMutateDef}: {@link #create(WritePermit,
 * SelectionDef)} takes both a {@link WritePermit} <b>and</b> the {@link SelectionDef} it operates against as
 * required positional parameters. Omitting either is a <b>compile</b> failure, not a missed javadoc sentence
 * &mdash; the same discipline the console's own {@code WriteGuard} uses for its {@code WritePermit} (source
 * design §5.4). This closes both halves of HIGH-5 by construction:
 * <ul class='spaced-list'>
 * 	<li>The {@link WritePermit} proves the caller's own write-gate was consulted before this affordance is even
 * 		configured &mdash; a bulk-mutate control cannot be reached by a caller who forgot to check.
 * 	<li>The {@link SelectionDef} proves a stable-row-id selection mechanism already exists for this table to
 * 		target &mdash; a {@link BulkMutateDef} can never float free of the selection it acts on, so the reverse
 * 		question ("which rows does this bulk action see?") is never ambiguous. This is the only direction of
 * 		coupling between the two opt-ins: bulk-mutate requires selection, but selection never requires (or even
 * 		references) bulk-mutate &mdash; so enabling selection alone can never surface a bulk-mutate control.
 * </ul>
 *
 * <h5 class='section'>Reuses {@link RowAction} and {@link ActionResult} verbatim &mdash; N per-row writes, per-target results</h5>
 * <p>
 * A bulk action is declared as an ordinary {@link RowAction} (the {@link RowAction} wire schema is completely
 * untouched by this type); {@code juneau-views.js} executes a bulk action as N independent, per-row invocations
 * of the SAME single-action submit path ({@code submitRowAction}) it already uses for the per-row action menu
 * &mdash; one fail-closed CSRF POST per selected row, each with its OWN {@code data-juneau-inflight} marker (the
 * per-row in-flight model, driven per target) and its OWN typed {@link ActionResult}, rendered
 * independently into that row's own outcome banner. There is deliberately <b>no</b> aggregate "bulk result": a
 * partial failure among N targets can never be hidden behind an aggregate success, and each target's
 * {@code data-juneau-inflight} marker clears on ITS OWN terminal outcome (success/failure/refusal/unknown) so a
 * stuck marker on one target can never halt the whole table's polling (MED-4).
 *
 * <h5 class='section'>Its own, independent contract version</h5>
 * <p>
 * Unlike {@link SelectionDef} (which is pure DOM attributes, never wire data), a bulk action LIST is itself wire
 * data the client must parse &mdash; so it travels as its own JSON sidecar (distinct from the {@code VIEW_META}
 * one), with its own {@link #CONTRACT_VERSION}, exactly mirroring how {@link ActionResult} versions itself
 * independently of {@link ViewDef#CONTRACT_VERSION} rather than aliasing it. A version bump here can therefore
 * NEVER force a {@link ViewDef#CONTRACT_VERSION} bump (or vice versa): the two contracts are unrelated, so this
 * opt-in cannot back the framework into the very lockstep the opt-in separability guard exists to avoid. The
 * {@link WritePermit} carried by this builder is a server-side-only construction guard and is <b>never</b>
 * serialized &mdash; only {@link #contractVersion} and {@link #actions} reach the wire.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	SelectionDef <jv>selection</jv> = SelectionDef.<jsm>create</jsm>(<js>"id"</js>);
 * 	<jc>// Requires a permit the CALLER's own write-gate has already minted, and the selection it targets.</jc>
 * 	BulkMutateDef <jv>bulk</jv> = BulkMutateDef.<jsm>create</jsm>(WritePermit.<jsm>forCapability</jsm>(<js>"incident:bulk-ack"</js>), <jv>selection</jv>)
 * 		.actions(RowAction.<jsm>create</jsm>(<js>"ack"</js>).label(<js>"Acknowledge"</js>)
 * 			.endpoint(<js>"servlet:/incidents/ack"</js>).method(RowAction.Method.<jsf>POST</jsf>));
 * 	Div <jv>markup</jv> = ViewTable.<jsm>of</jsm>(<jv>req</jv>, <jv>viewDef</jv>, <jv>rows</jv>, <jv>bulk</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link SelectionDef}
 * 	<li class='jc'>{@link WritePermit}
 * 	<li class='jc'>{@link RowAction}
 * 	<li class='jc'>{@link ActionResult}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,actions")
public final class BulkMutateDef {

	/**
	 * The frozen bulk-actions contract version &mdash; its <b>own</b> discriminator, deliberately NOT aliased to
	 * {@link ViewDef#CONTRACT_VERSION} (see the class javadoc). Bumped only on a breaking wire change to this
	 * contract.
	 */
	public static final String CONTRACT_VERSION = "1";

	/** The frozen contract-version discriminator (always {@value #CONTRACT_VERSION} for this contract). */
	public String contractVersion = CONTRACT_VERSION;

	/** The declared bulk actions, in menu order. Each is an ordinary, unmodified {@link RowAction}. */
	public List<RowAction> actions;

	/**
	 * The construction-time proof that the caller's own write-gate was consulted. Deliberately private and NOT
	 * listed in {@code @BeanType(properties=...)}: it must never reach the wire.
	 */
	private final WritePermit permit;

	/** The selection this bulk config operates against. Also excluded from the wire (see {@link #permit}). */
	private final SelectionDef selection;

	private BulkMutateDef(WritePermit permit, SelectionDef selection) {
		this.permit = permit;
		this.selection = selection;
	}

	/**
	 * Starts a new {@link BulkMutateDef}, requiring proof that the caller's write-gate was consulted and the
	 * selection this bulk config will operate against.
	 *
	 * @param permit A permit the caller's OWN write-gate minted after its own checks passed. Must not be
	 * 	<jk>null</jk>.
	 * @param selection The {@link SelectionDef} this bulk config operates against. Must not be <jk>null</jk>.
	 * @return A new {@link BulkMutateDef} with no actions yet declared.
	 * @throws IllegalArgumentException If {@code permit} or {@code selection} is <jk>null</jk>.
	 */
	public static BulkMutateDef create(WritePermit permit, SelectionDef selection) {
		if (permit == null)
			throw iaex("BulkMutateDef requires a non-null WritePermit (the caller's own write-gate must mint one first).");
		if (selection == null)
			throw iaex("BulkMutateDef requires a non-null SelectionDef (the selection this bulk config operates against).");
		return new BulkMutateDef(permit, selection);
	}

	/**
	 * Declares the bulk actions.
	 *
	 * @param value The actions, in menu order. Each declares a mutating request executed once per selected row
	 * 	(see the class javadoc's N-per-row-writes section). Must not be <jk>null</jk> or empty.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or empty.
	 */
	@SuppressWarnings({
		"java:S1845" // Fluent-builder setter intentionally mirrors the wire field name (Juneau DSL convention); part of the public fluent API.
	})
	public BulkMutateDef actions(RowAction...value) {
		if (value == null || value.length == 0)
			throw iaex("BulkMutateDef requires at least one action.");
		actions = l(value);
		return this;
	}

	/**
	 * The permit proving the caller's write-gate was consulted before this config was built.
	 *
	 * @return The permit. Never <jk>null</jk>.
	 */
	public WritePermit permit() {
		return permit;
	}

	/**
	 * The selection this bulk config operates against.
	 *
	 * @return The selection. Never <jk>null</jk>.
	 */
	public SelectionDef selection() {
		return selection;
	}
}
