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

import org.apache.juneau.commons.bean.*;

/**
 * The typed result a row action's submit returns &mdash; the load-bearing half of the write-path arc (design doc
 * §6.1/§6.2; the write-path half of the row-action submit contract).
 *
 * <p>
 * A raw HTML fragment response cannot carry the distinction the consumer's non-optimistic UI rule requires: a row
 * must not show "acknowledged" because a request was <i>sent</i>, only because the remote system was read back and
 * said so.  This typed result carries exactly that distinction &mdash; an {@link Outcome outcome discriminator}, the
 * authoritative {@link #row} data to re-render from, a {@link #replay} marker, and a {@link #refusalCode} &mdash; so
 * the {@code juneau-views.js} runtime can paint what actually happened (success, failure, a <i>named</i> refusal, or
 * an honest unknown) rather than what the request asked for.
 *
 * <h5 class='section'>Its own, independent contract version</h5>
 * <p>
 * The action-result is a <b>third</b> independently-versioned wire contract, alongside {@code VIEW_META}
 * ({@link ViewDef#CONTRACT_VERSION}) and {@code PAGE_META} ({@link PageDef#CONTRACT_VERSION}).  Its
 * {@link #CONTRACT_VERSION} is deliberately its <b>own</b> constant and is <b>not</b> aliased to
 * {@link ViewDef#CONTRACT_VERSION}: aliasing a per-view contract's version onto a separate contract couples two
 * unrelated client/server locksteps (the exact mechanism behind the {@code PageDef}&rarr;{@code ViewDef} alias risk),
 * so this contract versions itself.
 *
 * <h5 class='section'>Refusals are an opaque, namespaced code &mdash; not a framework-closed enum</h5>
 * <p>
 * A refusal's {@link #refusalCode} is an <b>opaque namespaced string</b>, not a framework-closed enum and not a
 * free-text message.  Refusals span two independent sources, and the framework must be able to render a refusal it
 * has never heard of:
 * <ul class='spaced-list'>
 * 	<li><b>Framework transport codes</b> come from
 * 		{@link org.apache.juneau.rest.server.filter.LoopbackBoundary.Reason#name() LoopbackBoundary.Reason.name()}
 * 		(e.g. {@code CSRF_TOKEN_MISSING}), already on the wire as the {@code X-Loopback-Boundary} header and the
 * 		rejection envelope's {@code reason}.
 * 	<li><b>Consumer codes</b> are namespaced {@code app:} / {@code write-guard:} strings for concepts Juneau knows
 * 		nothing about ("not armed", "arming expired", "confirmation does not match this incident", "a previous
 * 		attempt's outcome is unknown").
 * </ul>
 * <p>
 * The runtime renders ANY code as a visible <i>named</i> refusal without the framework needing to know consumer
 * concepts exist.
 *
 * <h5 class='section'>Reserved async terminal outcomes</h5>
 * <p>
 * {@link Outcome#CANCELLED cancelled} and {@link Outcome#CANCELLED_AFTER_EFFECT cancelled-after-effect} are
 * <b>reserved from day one</b> (as {@code VIEW_META} reserved {@code rowActions} before it was implemented), even
 * though a synchronous write never emits them.  A future async/streaming variant can therefore emit them without a
 * second result-contract bump and a three-sided lockstep.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A success carrying the authoritative row read back from the remote system.</jc>
 * 	ActionResult <jv>ok</jv> = ActionResult.<jsm>success</jsm>(<jv>incidentReadBack</jv>);
 *
 * 	<jc>// A named refusal from the consumer's write gate.</jc>
 * 	ActionResult <jv>no</jv> = ActionResult.<jsm>refusal</jsm>(<js>"write-guard:not-armed"</js>);
 *
 * 	<jc>// A recorded prior outcome, labelled as a replay rather than re-calling.</jc>
 * 	ActionResult <jv>again</jv> = ActionResult.<jsm>success</jsm>(<jv>incidentReadBack</jv>).replay(<jk>true</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link RowAction}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.ModalDef}
 * 	<li class='jc'>{@link IdempotencyKey}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,outcome,replay,refusalCode,message,row,resultForm")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class ActionResult {

	/**
	 * The frozen action-result contract version &mdash; its <b>own</b> discriminator, deliberately NOT aliased to
	 * {@link ViewDef#CONTRACT_VERSION}.  Bumped only on a breaking wire change to this contract.
	 */
	public static final String CONTRACT_VERSION = "1";

	/**
	 * The outcome discriminator a row-action submit reports.
	 *
	 * <p>
	 * Each constant carries the lowercase wire token emitted for the {@code outcome} field.  The four synchronous
	 * outcomes ({@link #SUCCESS}, {@link #FAILURE}, {@link #REFUSAL}, {@link #UNKNOWN}) are what a synchronous write
	 * can emit; {@link #CANCELLED} and {@link #CANCELLED_AFTER_EFFECT} are reserved for a future async variant
	 * and are frozen here so that contract does not force a second result-contract bump.
	 */
	public enum Outcome {

		/** The write completed and {@link ActionResult#row} carries the authoritative row read back. */
		SUCCESS("success"),

		/** The write was attempted and failed. */
		FAILURE("failure"),

		/** The write was refused; {@link ActionResult#refusalCode} carries the named reason. */
		REFUSAL("refusal"),

		/** The write's outcome is genuinely unknown (e.g. a crash mid-write) &mdash; an honest, non-optimistic state. */
		UNKNOWN("unknown"),

		/** Reserved for a future async variant: the job was cancelled before any effect. */
		CANCELLED("cancelled"),

		/** Reserved for a future async variant: the job was cancelled after a partial/uncertain effect. */
		CANCELLED_AFTER_EFFECT("cancelled-after-effect");

		private final String wire;

		Outcome(String wire) {
			this.wire = wire;
		}

		/**
		 * Returns the lowercase wire token for this outcome.
		 *
		 * @return The wire token (e.g. {@code "success"}).
		 */
		public String wire() {
			return wire;
		}
	}

	/** The frozen contract-version discriminator (always {@value #CONTRACT_VERSION} for this contract). */
	public String contractVersion = CONTRACT_VERSION;

	/** The outcome discriminator wire token (see {@link Outcome#wire()}). */
	public String outcome;

	/**
	 * Whether this result is a recorded prior outcome replayed rather than re-executed; omitted from the wire when
	 * unset (the common, non-replay case).
	 */
	public Boolean replay;

	/**
	 * The opaque, namespaced refusal code for a {@link Outcome#REFUSAL refusal}; omitted from the wire for any other
	 * outcome.  Framework codes come from
	 * {@link org.apache.juneau.rest.server.filter.LoopbackBoundary.Reason#name()}; consumer codes are namespaced
	 * {@code app:} / {@code write-guard:} strings (see the class javadoc).
	 */
	public String refusalCode;

	/** Optional human-readable supplementary message for any outcome; omitted from the wire when unset. */
	public String message;

	/**
	 * The authoritative row payload the runtime re-renders the row from (the {@code MERGE_ROW} case); omitted from
	 * the wire when unset.  This is the row as the remote system actually reports it, not what the request asked for.
	 */
	public Object row;

	/**
	 * The follow-up <b>read-only</b> form-source URL a receipt is painted from; omitted from the wire when unset.
	 *
	 * <h5 class='section'>Honored on {@link Outcome#SUCCESS success} only</h5>
	 * <p>
	 * This is the mechanical form of <i>a receipt must never look like a receipt for a write that didn't happen</i>.
	 * On a failure, a refusal, an unknown, or a transport failure this field is <b>ignored</b>, no follow-up GET is
	 * issued, and nothing is swapped &mdash; so no result host can appear for a write that did not commit.
	 *
	 * <h5 class='section'>The served payload must not carry a form</h5>
	 * <p>
	 * The URL serves a {@code ModalDef}, and that payload <b>must not</b> declare a {@code form}.  A result host is
	 * terminal by definition: the write already committed, so there is nothing left to gate.  A payload carrying
	 * inputs is refused <i>visibly</i> rather than rendered read-only or silently stripped, because it is a consumer
	 * authoring bug and should be visible as one.
	 *
	 * <h5 class='section'>It is a swap, not a new dialog</h5>
	 * <p>
	 * The runtime paints the receipt into the dialog that is <b>already open</b>, at the same layer depth, and only
	 * when that dialog opted in with {@code ModalDef.keepOpenOnSubmit}.  Set on a result whose dialog did not opt in,
	 * the swap is dropped, the success is reported unchanged, and the runtime emits one non-alarming diagnostic
	 * rather than either opening a layer or making a successful write look failed.
	 */
	public String resultForm;

	/**
	 * Starts a {@link Outcome#SUCCESS success} result carrying the authoritative row read back from the remote system.
	 *
	 * @param row The authoritative row payload to re-render from.  Can be <jk>null</jk> (e.g. a navigate/redraw
	 * 	action that carries no row).
	 * @return A new {@link ActionResult}.
	 */
	public static ActionResult success(Object row) {
		var r = new ActionResult();
		r.outcome = Outcome.SUCCESS.wire();
		r.row = row;
		return r;
	}

	/**
	 * Starts a {@link Outcome#FAILURE failure} result.
	 *
	 * @return A new {@link ActionResult}.
	 */
	public static ActionResult failure() {
		var r = new ActionResult();
		r.outcome = Outcome.FAILURE.wire();
		return r;
	}

	/**
	 * Starts a {@link Outcome#REFUSAL refusal} result carrying the named, opaque refusal code.
	 *
	 * @param refusalCode The opaque, namespaced refusal code.  Must not be <jk>null</jk> or blank (a refusal with no
	 * 	code is exactly the free-text-message anti-pattern this contract forbids).
	 * @return A new {@link ActionResult}.
	 * @throws IllegalArgumentException If {@code refusalCode} is <jk>null</jk> or blank.
	 */
	public static ActionResult refusal(String refusalCode) {
		if (refusalCode == null || refusalCode.isBlank())
			throw iaex("ActionResult refusalCode must not be null or blank.");
		var r = new ActionResult();
		r.outcome = Outcome.REFUSAL.wire();
		r.refusalCode = refusalCode;
		return r;
	}

	/**
	 * Starts an {@link Outcome#UNKNOWN unknown} result &mdash; an honest, non-optimistic terminal state.
	 *
	 * @return A new {@link ActionResult}.
	 */
	public static ActionResult unknown() {
		var r = new ActionResult();
		r.outcome = Outcome.UNKNOWN.wire();
		return r;
	}

	/**
	 * Sets the authoritative row payload to re-render from.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public ActionResult row(Object value) {
		row = value;
		return this;
	}

	/**
	 * Marks this result as a recorded prior outcome replayed rather than re-executed.
	 *
	 * @param value <jk>true</jk> to mark this a replay; <jk>false</jk>/<jk>null</jk> to unset (omitting it from the wire).
	 * @return This object.
	 */
	public ActionResult replay(Boolean value) {
		replay = (value != null && value) ? Boolean.TRUE : null;
		return this;
	}

	/**
	 * Sets the optional human-readable supplementary message.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public ActionResult message(String value) {
		message = value;
		return this;
	}

	/**
	 * Sets the opaque, namespaced refusal code (see {@link #refusalCode}).
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public ActionResult refusalCode(String value) {
		refusalCode = value;
		return this;
	}

	/**
	 * Sets the follow-up read-only form-source URL a receipt is painted from (see {@link #resultForm}).
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public ActionResult resultForm(String value) {
		resultForm = value;
		return this;
	}
}
