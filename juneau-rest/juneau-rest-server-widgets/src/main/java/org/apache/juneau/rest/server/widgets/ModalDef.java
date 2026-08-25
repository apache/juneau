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
 * <h5 class='section'>Not a new row-action wire field</h5>
 * <p>
 * This is the response a row action's form-source URL returns, <b>not</b> a new field on the frozen row-action wire
 * schema: the modal's declaration lives in this separately-served payload so the row-action contract stays frozen.
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
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,title,fields,form,idempotencyKey")
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

	/**
	 * A single typed confirmation field: a label and a value, painted client-side with {@code textContent} (never
	 * {@code innerHTML}).
	 *
	 * @since 10.0.0
	 */
	@BeanType(properties="label,value")
	public static class Field {

		/** The field label shown to the user. */
		public String label;

		/** The field value, read back from the current authoritative record. */
		public String value;

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
	 * Fail-closed bean validation.
	 *
	 * <p>
	 * Requires a non-blank {@link #title} and a non-blank label on each {@link Field}; when a {@link #form} is present
	 * it delegates to {@link FormDef#validate()}.  Does <b>not</b> require {@link #contractVersion} to already be set
	 * (a raw-built form-bearing modal validated directly must not false-refuse on a null version &mdash;
	 * {@link #checked()} stamps the version first, then validates).
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
			}
		if (form != null)
			form.validate();
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
