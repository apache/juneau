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

/**
 * The declarative modal (dialog) a {@code present=dialog} row action opens &mdash; the payload the
 * <b>modal-open confirmation fetch</b> returns (design doc §6.2).
 *
 * <h5 class='section'>A widget contract driven by a view-module runtime</h5>
 * <p>
 * This bean is a general widget contract and lives in this module, but the browser runtime that paints it ships with
 * the rich-view module (it is part of that module's {@code juneau-views.js} monolith, which is not split apart here).
 * So a page that opens one of these dialogs loads the view runtime; nothing in this module reaches back to it.
 *
 * <h5 class='section'>Safe-by-construction confirmation body</h5>
 * <p>
 * The confirmation body is <b>typed structured fields</b> ({@link #fields}: incident #, title, service, current
 * status &mdash; the source design §6.2 field set), server-rendered against the current, authoritative record and
 * bound to the target identifier.  The {@code juneau-views.js} runtime paints each field's value with
 * {@code textContent} &mdash; NEVER {@code innerHTML}, never raw markup, never the non-table panel-content sink.
 * Live/remote data is attacker-influenceable (anyone who can title an incident or post in a watched thread) and this
 * origin holds the CSRF token, so an HTML sink here would be stored-XSS&nbsp;&rarr;&nbsp;token-theft&nbsp;&rarr;
 * arbitrary-write; typed fields painted with {@code textContent} close that off by construction.
 *
 * <h5 class='section'>The idempotency key rides here</h5>
 * <p>
 * {@link #idempotencyKey} is a server-minted key value, minted at modal-open and bound to
 * {@code (action, targetId)}.  The runtime carries it verbatim on the submit so a double-click / re-submit / browser
 * retry all collapse to one effect; the server checks that binding on submit and answers a mismatch with a named
 * refusal, never a replayed success.  The minting/binding helper itself is a table-and-row-action concern and stays
 * in the rich-view module, which is why this field is a plain {@code String} rather than that helper's type.
 *
 * <h5 class='section'>{@link #selfTargeted} &mdash; the opt-in for a key that targets itself</h5>
 * <p>
 * A dialog opened where there is <b>no row</b> (a ribbon-hosted dialog whose submit creates the thing it is about)
 * has no artifact id to bind its key to at modal-open.  The rich-view module's key helper can mint a key whose
 * bound target <i>is its own value</i>; {@link #selfTargeted} is the signal that tells the runtime to send that
 * value as the submitted target instead of the row's id.
 * <p>
 * The signal has to ride on this bean, because it cannot be inferred from the wire: {@link #idempotencyKey} is an
 * opaque string either way, indistinguishable in shape from an artifact-bound key's value.  It is therefore an
 * explicit, additive, per-dialog opt-in &mdash; <b>not</b> a blanket runtime precedence rule.  A dialog that leaves
 * this unset behaves exactly as it does today, <i>including</i> one that sets {@link #idempotencyKey} for an
 * artifact-bound reason: it keeps sending the row's real id, which is what its own mint call bound.
 * <p>
 * Set it <b>only</b> on a modal whose key came from the self-targeted mint helper.  Setting it on a modal carrying
 * an artifact-bound key silently discards the real target and makes the server's binding check vacuous.
 *
 * <h5 class='section'>{@link #keepOpenOnSubmit} &mdash; the opt-in for an in-dialog result receipt</h5>
 * <p>
 * By default the runtime closes a dialog at confirm-click time and then submits, so every dialog is gone before its
 * result exists.  {@link #keepOpenOnSubmit} suppresses that close, and the runtime instead paints the write's
 * <b>result</b> into the dialog that is already open &mdash; a <i>receipt</i>.
 * <p>
 * A receipt host is <b>non-submittable by construction</b>, not by convention: the runtime paints it with a distinct
 * function that creates no confirm control at all, and it refuses to paint one whose payload carries a
 * {@link #form}.  It is reached <b>only</b> after a committed write (an
 * {@code ActionResult} whose outcome is {@code success}), so a result host can never appear for a write that did not
 * happen.  A follow-up read-only form source is named by that result's {@code resultForm}.
 * <p>
 * The opt-in has to live here rather than on the action bean, because the confirm handler must decide whether to
 * close <i>before</i> any response exists.  A dialog synthesized from a {@code confirm} string alone (no form-source
 * URL) therefore cannot opt in &mdash; a receipt-bearing action must serve a real {@link ModalDef}.
 * <p>
 * Additive, and named degradation, accepted: an older runtime ignoring this flag closes the dialog and drops the
 * receipt.  The write still happens; the receipt is simply not shown.
 *
 * <h5 class='section'>{@link #childActions} &mdash; the dialog-scoped child-action catalog</h5>
 * <p>
 * A {@code type="action"} form input names an action id, and the runtime resolves it against the enclosing view's
 * row-action catalog.  A <b>stacked</b> step reached from inside an already-open dialog (a Review step in a wizard)
 * has nothing legal to resolve against there: it is not a row action, and publishing it as one would surface it in
 * every row's action menu.  {@link #childActions} is that missing catalog, scoped to <b>one open dialog</b>.
 * <p>
 * Scoping is <b>structural</b>, not a flag some menu builder has to remember to filter on: this catalog rides on a
 * per-open payload, so the row-action menu and the ribbon resolver never see it and there is no exclusion logic to
 * rot.  Resolution precedence is likewise fail-safe: the existing row-action check runs <b>first</b> and unchanged,
 * so a served payload can never shadow &mdash; and thereby bypass the gating of &mdash; a declared row action.
 *
 * <h5 class='section'>Not a new row-action wire field</h5>
 * <p>
 * This is the response a row action's form-source URL returns, <b>not</b> a new field on the frozen row-action wire
 * schema: the modal's declaration lives in this separately-served payload so the row-action contract stays frozen.
 * The same reasoning is why {@link #keepOpenOnSubmit} and {@link #childActions} land here rather than on
 * {@code RowAction} / {@code RibbonAction}, which stay frozen and untouched.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ModalDef <jv>modal</jv> = ModalDef.<jsm>create</jsm>(<js>"Acknowledge this incident?"</js>)
 * 		.field(<js>"Incident"</js>, <jv>incidentNumber</jv>)
 * 		.field(<js>"Title"</js>, <jv>incidentTitle</jv>)
 * 		.field(<js>"Service"</js>, <jv>serviceName</jv>)
 * 		.field(<js>"Current status"</js>, <jv>currentStatus</jv>)
 * 		.form(FormDef.<jsm>ofTemplate</jsm>(<js>"servlet:/incidents/ack-form.ftl"</js>))
 * 		.idempotencyKey(<jv>keyValue</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link FormDef}
 * 	<li class='jc'>{@link Widget}
 * </ul>
 *
 * <h5 class='section'>The third named bar-slot host</h5>
 * <p>
 * {@link #barSlot} is the <i>third</i> named {@link BarSlot} attachment &mdash; alongside the rich-view module's page
 * and row-detail hosts (that module never depends on this one, so they are not linked here).  Unlike those two
 * &mdash; Java-only fields a server-side emitter paints directly, omitted from their wire &mdash; this modal itself
 * <b>is</b> the wire payload the modal-open confirmation fetch returns, with no separate server-rendered pass the
 * dialog could ride into.  So this field <b>is</b> carried on the wire (additive-only: {@code null} when unset, and
 * its presence does not bump {@link #CONTRACT_VERSION}), and the {@code juneau-views.js} runtime paints the region
 * and its dynamic-count sidecar client-side from the JSON, the same way it already paints typed confirmation
 * {@link #fields} and {@link #form} controls &mdash; {@code textContent} only, never {@code innerHTML}.  The dialog
 * anchors its own region immediately after the title (the rich-view module's {@code
 * BarSlotTable.ANCHOR_DIALOG_TITLE}), owning that placement itself rather than leaning on the shared strip builder,
 * exactly as the row-detail host owns its own ribbon-trailing relocation.
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,title,fields,form,idempotencyKey,selfTargeted,barSlot,keepOpenOnSubmit,childActions")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class ModalDef implements Widget {

	/**
	 * The frozen modal contract version.  Bumped only on a breaking wire change to this modal contract.
	 *
	 * <p>
	 * Moves in <b>lockstep</b> with {@link FormDef#CONTRACT_VERSION} and the runtime's baked-in literal: the runtime
	 * compares one literal against both this version and the nested form's, so bumping any two of the three makes
	 * every form-bearing dialog refuse to open.
	 */
	public static final String CONTRACT_VERSION = "2";

	/** The {@link Field#kind} token for an ordinary text value &mdash; the default when the field declares none. */
	public static final String FIELD_KIND_TEXT = "text";

	/** The {@link Field#kind} token for a monospaced, copyable value (an id, a URL, a generated token). */
	public static final String FIELD_KIND_CODE = "code";

	/**
	 * A single typed confirmation field: a label and a value, painted client-side with {@code textContent} (never
	 * {@code innerHTML}).
	 *
	 * @since 10.0.0
	 */
	@BeanType(properties="label,value,kind")
	public static class Field {

		/** The field label shown to the user. */
		public String label;

		/** The field value, read back from the current authoritative record. */
		public String value;

		/**
		 * How this field's value is <i>displayed</i>; omitted from the wire when unset (which reads as
		 * {@link ModalDef#FIELD_KIND_TEXT text}).
		 *
		 * <p>
		 * The allowlist is exactly {@link ModalDef#FIELD_KIND_TEXT text} and {@link ModalDef#FIELD_KIND_CODE code};
		 * {@link ModalDef#validate()} rejects anything else server-side, and the runtime falls back to
		 * {@code text} for an unrecognized token rather than trusting it.
		 *
		 * <p>
		 * This is a <b>display</b> kind on a confirmation field, deliberately <b>not</b> a new
		 * {@code FormDef.Input} type: a {@code code} field carries no submit value, so expressing it as an input
		 * would mean teaching the collection, validation, prefill and submit paths about a control that is not one.
		 */
		public String kind;

		/**
		 * Creates a typed confirmation field.
		 *
		 * @param label The field label.  Must not be <jk>null</jk> or blank.
		 * @param value The field value.  Can be <jk>null</jk> (rendered as an empty value).
		 * @return A new {@link Field}.
		 * @throws IllegalArgumentException If {@code label} is <jk>null</jk> or blank.
		 */
		public static Field of(String label, String value) {
			if (label == null || label.isBlank())
				throw iaex("ModalDef.Field label must not be null or blank.");
			var f = new Field();
			f.label = label;
			f.value = value;
			return f;
		}

		/**
		 * Creates a typed confirmation field displayed as monospaced, copyable {@link ModalDef#FIELD_KIND_CODE code}.
		 *
		 * <p>
		 * The runtime paints the value into a {@code <pre>} with {@code textContent} (never {@code innerHTML}) and
		 * adds a copy button that is feature-detected and never throws; the {@code <pre>} stays selectable, so
		 * manual copy always works.
		 *
		 * @param label The field label.  Must not be <jk>null</jk> or blank.
		 * @param value The field value.  Can be <jk>null</jk> (rendered as an empty value).
		 * @return A new {@link Field}.
		 * @throws IllegalArgumentException If {@code label} is <jk>null</jk> or blank.
		 */
		public static Field code(String label, String value) {
			var f = of(label, value);
			f.kind = FIELD_KIND_CODE;
			return f;
		}
	}

	/**
	 * One entry in a dialog's {@link ModalDef#childActions} catalog &mdash; a stacked step a {@code type="action"}
	 * input inside <i>this</i> dialog's form may open.
	 *
	 * <h5 class='section'>Dialog-presenting by construction</h5>
	 * <p>
	 * A child action carries no {@code present} field: opening a stacked dialog is the only thing it does.  A child
	 * with no {@link #form} is a legal confirm-only child dialog.
	 *
	 * <h5 class='section'>No gating vocabulary, on purpose</h5>
	 * <p>
	 * There is deliberately no {@code enabledWhen} here, for three independent reasons.  The rule type lives in the
	 * rich-view module, which this module must not reference.  A dialog opened where there is no row has no row data
	 * to evaluate against, so a field-keyed rule would fail closed forever with no signal to its author.  And the
	 * subject of a child step is the <i>draft in the open dialog</i>, not the parent row &mdash; so evaluating the
	 * parent row's data would answer a question nobody asked.  Gating a child step is the <b>server's</b> job at
	 * child-form-GET time; it already owns that GET and can refuse it.
	 *
	 * @since 10.0.0
	 */
	@BeanType(properties="id,label,form,endpoint,method,onSuccess,carryDrafts")
	@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
	public static class ChildAction {

		/** The action id a {@code type="action"} input in this dialog's form names.  Required, non-blank. */
		public String id;

		/** The label painted on the button that opens this child.  Required, non-blank. */
		public String label;

		/**
		 * The child dialog's read-only form-source URL; omitted from the wire when unset (a confirm-only child).
		 *
		 * <p>
		 * Required if this child declares {@link #carryDrafts} &mdash; with no URL there is nothing for the draft
		 * query to ride on, so the drafts would vanish silently.
		 */
		public String form;

		/** The child's submit endpoint; omitted from the wire when unset.  See {@link #endpoint(String)}. */
		public String endpoint;

		/** The child's submit method; omitted from the wire when unset.  See {@link #method(String)}. */
		public String method;

		/** The child's on-success behaviour token; omitted from the wire when unset. */
		public String onSuccess;

		/**
		 * Whether opening this child carries the <b>parent dialog's current draft values</b> to its form-source GET
		 * as one query parameter; omitted from the wire when unset or <jk>false</jk>.
		 *
		 * <p>
		 * Without it a Review step opens <b>empty</b> and loses everything the operator typed.  With it, the
		 * runtime serializes the parent's collected form values, URL-encodes them, and appends them as a single
		 * {@code juneauDrafts} parameter, refusing visibly (and not opening the child) above a client-side byte cap
		 * measured on the <i>encoded</i> value.
		 *
		 * <p>
		 * <b>Drafts land in access logs and browser history.</b>  No secret may ride this channel.
		 */
		public Boolean carryDrafts;

		/**
		 * Creates a child action.
		 *
		 * @param id The action id a {@code type="action"} input names.  Must not be <jk>null</jk> or blank.
		 * @param label The button label.  Must not be <jk>null</jk> or blank.
		 * @return A new {@link ChildAction}.
		 * @throws IllegalArgumentException If {@code id} or {@code label} is <jk>null</jk> or blank.
		 */
		public static ChildAction of(String id, String label) {
			if (id == null || id.isBlank())
				throw iaex("ModalDef.ChildAction id must not be null or blank.");
			if (label == null || label.isBlank())
				throw iaex("ModalDef.ChildAction label must not be null or blank.");
			var c = new ChildAction();
			c.id = id;
			c.label = label;
			return c;
		}

		/**
		 * Sets the child dialog's read-only form-source URL.
		 *
		 * @param value The URL.  Can be <jk>null</jk> to unset (a confirm-only child).
		 * @return This object.
		 */
		public ChildAction form(String value) {
			form = value;
			return this;
		}

		/**
		 * Sets the child's submit endpoint.
		 *
		 * <p>
		 * Optional individually; a child action whose dialog is meant to be <b>submittable</b> must declare both a
		 * non-safe {@link #method} and an {@code endpoint}, or its Confirm visibly refuses with {@code safe-method}
		 * and sends nothing.  A display-only child that never submits needs neither, which is why this is not a
		 * {@link ModalDef#validate()} rule &mdash; this bean cannot know which of its children are meant to submit.
		 *
		 * @param value The endpoint.  Can be <jk>null</jk> to unset.
		 * @return This object.
		 */
		public ChildAction endpoint(String value) {
			endpoint = value;
			return this;
		}

		/**
		 * Sets the child's submit method.
		 *
		 * <p>
		 * Optional individually; see {@link #endpoint(String)} &mdash; a submittable child needs both, and a
		 * missing or safe method is a visible client-side refusal before any request is sent.
		 *
		 * @param value The method.  Can be <jk>null</jk> to unset.
		 * @return This object.
		 */
		public ChildAction method(String value) {
			method = value;
			return this;
		}

		/**
		 * Sets the child's on-success behaviour token.
		 *
		 * @param value The token.  Can be <jk>null</jk> to unset.
		 * @return This object.
		 */
		public ChildAction onSuccess(String value) {
			onSuccess = value;
			return this;
		}

		/**
		 * Declares that opening this child carries the parent dialog's drafts (see {@link #carryDrafts}).
		 *
		 * <p>
		 * Passing <jk>false</jk> <b>clears</b> the flag rather than serializing an explicit {@code false}, so an
		 * un-opted-in child's payload is byte-identical to one that never mentioned it.
		 *
		 * @param value <jk>true</jk> to carry the parent's drafts; <jk>false</jk> (or unset) to open empty.
		 * @return This object.
		 */
		public ChildAction carryDrafts(boolean value) {
			carryDrafts = value ? Boolean.TRUE : null;
			return this;
		}
	}

	/**
	 * The frozen modal contract version discriminator.
	 *
	 * <p>
	 * <b>Null</b> on a confirm-only modal (no {@link #form}) &mdash; confirm-only stays unversioned and is not
	 * fail-loud on a missing version; omitted from the wire while null.  Set to {@link #CONTRACT_VERSION} by
	 * {@link #checked()} when a form is present.
	 */
	public String contractVersion;

	/** The modal title / confirmation prompt. */
	public String title;

	/** The typed confirmation fields, in display order; omitted from the wire when none are declared. */
	public List<Field> fields;

	/** The optional input form; omitted from the wire when unset (a confirm-only modal). */
	public FormDef form;

	/**
	 * The server-minted idempotency key value the runtime carries on the submit; omitted from the wire when unset.
	 * The value is minted (and its {@code (action, targetId)} binding re-checked on submit) by the rich-view
	 * module's idempotency-key helper.
	 */
	public String idempotencyKey;

	/**
	 * Whether {@link #idempotencyKey} is a <b>self-targeted</b> key whose own value the runtime must send as the
	 * submitted target (see the class Javadoc); omitted from the wire when unset or <jk>false</jk>.
	 *
	 * <p>
	 * Additive-only: like {@link #barSlot}, its presence does not bump {@link #CONTRACT_VERSION}.
	 */
	public Boolean selfTargeted;

	/**
	 * Optional additive bar slot anchored to this dialog's title &mdash; the <i>third</i> named {@link BarSlot}
	 * attachment (see the class Javadoc for how this host differs from the other two).  Omitted from the wire when
	 * unset; painted by the runtime immediately after {@link #title}, ahead of {@link #fields} and {@link #form}.
	 */
	public BarSlot barSlot;

	/**
	 * Whether the runtime must <b>not</b> close this dialog at confirm-click time, so the write's result can be
	 * painted into it as a receipt (see the class Javadoc); omitted from the wire when unset or <jk>false</jk>.
	 *
	 * <p>
	 * Additive-only: like {@link #barSlot} and {@link #selfTargeted}, its presence does not bump
	 * {@link #CONTRACT_VERSION}.
	 */
	public Boolean keepOpenOnSubmit;

	/**
	 * The dialog-scoped catalog of stacked child actions a {@code type="action"} input in {@link #form} may open
	 * (see the class Javadoc); omitted from the wire when unset or empty.
	 */
	public List<ChildAction> childActions;

	/**
	 * Starts a new {@link ModalDef} with the specified title / confirmation prompt.
	 *
	 * @param title The modal title / confirmation prompt.  Must not be <jk>null</jk> or blank.
	 * @return A new mutable {@link ModalDef} to chain builder calls on.
	 * @throws IllegalArgumentException If {@code title} is <jk>null</jk> or blank.
	 */
	public static ModalDef create(String title) {
		if (title == null || title.isBlank())
			throw iaex("ModalDef title must not be null or blank.");
		var m = new ModalDef();
		m.title = title;
		return m;
	}

	/**
	 * Adds one typed confirmation field.
	 *
	 * @param label The field label.  Must not be <jk>null</jk> or blank.
	 * @param value The field value (read back from the current record).  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public ModalDef field(String label, String value) {
		if (fields == null)
			fields = l();
		fields.add(Field.of(label, value));
		return this;
	}

	/**
	 * Adds one typed confirmation field displayed as monospaced, copyable {@link #FIELD_KIND_CODE code}.
	 *
	 * @param label The field label.  Must not be <jk>null</jk> or blank.
	 * @param value The field value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public ModalDef codeField(String label, String value) {
		if (fields == null)
			fields = l();
		fields.add(Field.code(label, value));
		return this;
	}

	/**
	 * Adds one entry to this dialog's child-action catalog.
	 *
	 * @param value The child action.  Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public ModalDef childAction(ChildAction value) {
		if (value == null)
			throw iaex("ModalDef.ChildAction must not be null.");
		if (childActions == null)
			childActions = l();
		childActions.add(value);
		return this;
	}

	/**
	 * Declares that the runtime must keep this dialog open across its submit so the result can be painted into it
	 * as a receipt (see the class Javadoc).
	 *
	 * <p>
	 * Passing <jk>false</jk> <b>clears</b> the flag rather than serializing an explicit {@code false}: the wire
	 * carries this opt-in only when it is on, so an un-opted-in dialog's payload is byte-identical to today's.
	 *
	 * @param value <jk>true</jk> to opt in; <jk>false</jk> (or unset) keeps today's close-then-submit behaviour.
	 * @return This object.
	 */
	public ModalDef keepOpenOnSubmit(boolean value) {
		keepOpenOnSubmit = value ? Boolean.TRUE : null;
		return this;
	}

	/**
	 * Sets the optional input form.
	 *
	 * @param value The form.  Can be <jk>null</jk> to unset (a confirm-only modal).
	 * @return This object.
	 */
	public ModalDef form(FormDef value) {
		form = value;
		return this;
	}

	/**
	 * Sets the server-minted idempotency key value carried on the submit.
	 *
	 * @param value The key value.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public ModalDef idempotencyKey(String value) {
		idempotencyKey = value;
		return this;
	}

	/**
	 * Declares that {@link #idempotencyKey} is a self-targeted key &mdash; the runtime sends the key's own value as
	 * the submitted target rather than a row id.
	 *
	 * <p>
	 * Set this <b>only</b> alongside a key minted by the rich-view module's self-targeted mint helper; pairing it
	 * with an artifact-bound key discards that key's real target.  See the class Javadoc.
	 *
	 * <p>
	 * Passing <jk>false</jk> <b>clears</b> the flag rather than serializing an explicit {@code false}: the wire
	 * carries this opt-in only when it is on, so an un-opted-in dialog's payload is byte-identical to today's.
	 *
	 * @param value <jk>true</jk> to opt in; <jk>false</jk> (or unset) leaves today's row-id behaviour untouched.
	 * @return This object.
	 */
	public ModalDef selfTargeted(boolean value) {
		selfTargeted = value ? Boolean.TRUE : null;
		return this;
	}

	/**
	 * Declares the additive bar slot anchored to this dialog's title.
	 *
	 * <p>
	 * See {@link #barSlot} &mdash; a third named host for the same {@link BarSlot} bean, not a re-use of the
	 * rich-view module's page or row-detail hosts.
	 *
	 * @param value The bar slot.  Can be <jk>null</jk> (no dialog bar slot).
	 * @return This object.
	 */
	public ModalDef barSlot(BarSlot value) {
		barSlot = value;
		return this;
	}

	/**
	 * Fail-closed bean validation.
	 *
	 * <p>
	 * Requires a non-blank {@link #title} and a non-blank label on each {@link Field}; when a {@link #form} is present
	 * it delegates to {@link FormDef#validate()}.  Does <b>not</b> require {@link #contractVersion} to already be set
	 * (a raw-built form-bearing modal validated directly must not false-refuse on a null version &mdash;
	 * {@link #checked()} stamps the version first, then validates).
	 *
	 * <p>
	 * Also rejects, fail-closed at serve time rather than silently on the wire: a {@link Field#kind} outside the
	 * {@link #FIELD_KIND_TEXT text}/{@link #FIELD_KIND_CODE code} allowlist; a blank, duplicate or unlabelled
	 * {@link ChildAction} id; {@link ChildAction#carryDrafts} on a modal that declares no {@link #form} (nothing to
	 * collect <i>from</i>); and {@link ChildAction#carryDrafts} on a child that declares no
	 * {@link ChildAction#form} (nowhere to put them &mdash; the runtime would take its confirm-only branch, issue
	 * no GET, and the drafts would vanish silently).
	 *
	 * @throws IllegalArgumentException If this modal is not well-formed.
	 */
	@Override
	public void validate() {
		if (title == null || title.isBlank())
			throw iaex("ModalDef title must not be null or blank.");
		if (fields != null)
			for (var f : fields) {
				if (f == null)
					throw iaex("ModalDef field must not be null.");
				if (f.label == null || f.label.isBlank())
					throw iaex("ModalDef.Field label must not be null or blank.");
				if (f.kind != null && ! (FIELD_KIND_TEXT.equals(f.kind) || FIELD_KIND_CODE.equals(f.kind)))
					throw iaex("ModalDef.Field kind must be '" + FIELD_KIND_TEXT + "' or '" + FIELD_KIND_CODE
						+ "', not '" + f.kind + "'.");
			}
		validateChildActions();
		if (form != null)
			form.validate();
		if (barSlot != null)
			barSlot.validate();
	}

	/**
	 * The {@link #childActions} half of {@link #validate()}.
	 *
	 * <p>
	 * {@link ChildAction#of(String,String)} already argument-checks {@code id} and {@code label}; they are
	 * re-checked here as the wire-level backstop for a bean-deserialized instance that never went through the
	 * factory.
	 */
	private void validateChildActions() {
		if (childActions == null)
			return;
		var seen = new HashSet<String>();
		for (var c : childActions) {
			if (c == null)
				throw iaex("ModalDef.ChildAction must not be null.");
			if (c.id == null || c.id.isBlank())
				throw iaex("ModalDef.ChildAction id must not be null or blank.");
			if (c.label == null || c.label.isBlank())
				throw iaex("ModalDef.ChildAction label must not be null or blank.");
			if (! seen.add(c.id))
				throw iaex("ModalDef.ChildAction id '" + c.id + "' is declared more than once on this modal.");
			if (Boolean.TRUE.equals(c.carryDrafts)) {
				if (form == null)
					throw iaex("ModalDef.ChildAction '" + c.id + "' declares carryDrafts, but this modal declares "
						+ "no form - there is nothing to collect drafts from.");
				if (c.form == null || c.form.isBlank())
					throw iaex("ModalDef.ChildAction '" + c.id + "' declares carryDrafts, but declares no form URL "
						+ "- a confirm-only child issues no form GET, so the drafts would be dropped silently.");
			}
		}
	}

	/**
	 * The serving-path hook every app {@code @RestGet} that returns a {@link ModalDef} must invoke.
	 *
	 * <p>
	 * When a {@link #form} is present it stamps {@link #CONTRACT_VERSION} on this modal and its form (the fail-loud
	 * handshake baseline); a confirm-only modal is left <b>unversioned</b> ({@code contractVersion} null).  Then it
	 * {@link #validate() validates}, so a malformed modal/form fails at serve time rather than silently on the wire.
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If this modal (or its form) is not well-formed.
	 */
	public ModalDef checked() {
		if (form != null) {
			contractVersion = CONTRACT_VERSION;
			form.checked();
		} else {
			contractVersion = null;
		}
		validate();
		return this;
	}
}
