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
 * A per-row action descriptor in the {@code VIEW_META} wire contract (design doc §6.10; the transport-agnostic
 * "row-action intent" model, Decision&nbsp;8).
 *
 * <p>
 * A {@link ViewDef} declares zero or more of these via {@link ViewDef#rowActions(RowAction...)}; the
 * {@code juneau-views.js} runtime renders each as a row-menu item and, on activation, submits the declared
 * {@link #endpoint} with the declared {@link #method} as a JSON request carrying the process's CSRF token.
 *
 * <h5 class='section'>The frozen wire schema</h5>
 * <p>
 * This is the <b>complete</b> set of wire fields; it is pinned in full now so that later waves (a future declarative
 * modal/form + typed result) can rely on it verbatim without forcing another fail-loud
 * contract bump.  Only {@link #id} is required; every other field is optional and is omitted from the serialized
 * form when unset (null-valued properties are dropped).
 * <table class='styled'>
 * 	<tr><th>Field</th><th>Type / tokens</th><th>Meaning</th></tr>
 * 	<tr><td>{@code id}</td><td>string</td><td>Stable action id (menu-item key; also the submit's logical name).</td></tr>
 * 	<tr><td>{@code label}</td><td>string</td><td>The menu-item text.</td></tr>
 * 	<tr><td>{@code icon}</td><td>string</td><td>Optional glyph name (resolved by the shared icon registry).</td></tr>
 * 	<tr><td>{@code endpoint}</td><td>string</td><td>The URL the action submits to.</td></tr>
 * 	<tr><td>{@code method}</td><td>{@code POST}|{@code PUT}|{@code PATCH}|{@code DELETE}</td>
 * 		<td>The non-safe HTTP method (see {@link Method}).</td></tr>
 * 	<tr><td>{@code confirm}</td><td>string</td><td>Optional confirmation prompt shown before the submit.</td></tr>
 * 	<tr><td>{@code form}</td><td>string</td><td>Optional form-source URL supplying the action's input fields
 * 		(a future declarative form renders).</td></tr>
 * 	<tr><td>{@code present}</td><td>{@code page}|{@code dialog}|{@code inline}</td>
 * 		<td>How the action's form/confirmation is presented (see {@link Present}).</td></tr>
 * 	<tr><td>{@code onSuccess}</td><td>{@code redraw}|{@code mergeRow}|{@code navigate}</td>
 * 		<td>What the runtime does with a successful result (see {@link OnSuccess}).</td></tr>
 * </table>
 *
 * <h5 class='section'>Why {@link #method} cannot be a safe method</h5>
 * <p>
 * {@link Method} enumerates only {@code POST}/{@code PUT}/{@code PATCH}/{@code DELETE}, so a safe (CSRF-exempt)
 * method cannot be declared at all &mdash; a build-time refusal by construction.  This mirrors the server boundary:
 * {@link org.apache.juneau.rest.server.MethodSafety} fails any {@code @Mutating} operation bound to a safe method at
 * startup, and {@link org.apache.juneau.rest.server.filter.LoopbackBoundary} applies its Origin/CSRF/JSON checks
 * only to non-safe methods.  A mutating action bound to {@code GET} would be a CSRF-able write; the type system
 * forbids expressing one.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	RowAction <jv>ack</jv> = RowAction.<jsm>create</jsm>(<js>"ack"</js>)
 * 		.label(<js>"Acknowledge"</js>)
 * 		.endpoint(<js>"servlet:/incidents/{id}/ack"</js>)
 * 		.method(RowAction.Method.<jsf>POST</jsf>)
 * 		.confirm(<js>"Acknowledge this incident?"</js>)
 * 		.present(RowAction.Present.<jsf>DIALOG</jsf>)
 * 		.onSuccess(RowAction.OnSuccess.<jsf>MERGE_ROW</jsf>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewDef}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="id,label,icon,endpoint,method,confirm,form,present,onSuccess")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class RowAction {

	/**
	 * The non-safe HTTP method a row action submits with.
	 *
	 * <p>
	 * Deliberately limited to the four non-safe methods.  A safe method (GET/HEAD/OPTIONS/TRACE) skips the
	 * Origin/CSRF/JSON checks at {@link org.apache.juneau.rest.server.filter.LoopbackBoundary}, so a mutating
	 * action bound to one would be CSRF-able; this enum makes that unexpressible.  Each constant's wire token is
	 * its uppercase name, which is exactly what the client sends as the request method.
	 */
	public enum Method {

		/** HTTP {@code POST}. */
		POST,

		/** HTTP {@code PUT}. */
		PUT,

		/** HTTP {@code PATCH}. */
		PATCH,

		/** HTTP {@code DELETE}. */
		DELETE;

		/**
		 * Returns the wire token for this method (its uppercase name).
		 *
		 * @return The wire token (e.g. {@code "POST"}).
		 */
		public String wire() {
			return name();
		}
	}

	/**
	 * How a row action's form/confirmation is presented.
	 *
	 * <p>
	 * Each constant carries the lowercase wire token emitted for the {@code present} field.  The behavior behind
	 * each token is implemented by a later wave; this type freezes the vocabulary.
	 */
	public enum Present {

		/** Navigate to / render the action's form as a full page. */
		PAGE("page"),

		/** Render the action's form/confirmation in a modal dialog. */
		DIALOG("dialog"),

		/** Render the action inline within the row. */
		INLINE("inline");

		private final String wire;

		Present(String wire) {
			this.wire = wire;
		}

		/**
		 * Returns the lowercase wire token for this presentation.
		 *
		 * @return The wire token (e.g. {@code "dialog"}).
		 */
		public String wire() {
			return wire;
		}
	}

	/**
	 * What the runtime does with a successful action result.
	 *
	 * <p>
	 * Each constant carries the wire token emitted for the {@code onSuccess} field.  The behavior behind each
	 * token is implemented by a later wave; this type freezes the vocabulary.
	 */
	public enum OnSuccess {

		/** Redraw (reload) the whole table. */
		REDRAW("redraw"),

		/** Merge the returned row into the table in place. */
		MERGE_ROW("mergeRow"),

		/** Navigate away using the result. */
		NAVIGATE("navigate");

		private final String wire;

		OnSuccess(String wire) {
			this.wire = wire;
		}

		/**
		 * Returns the wire token for this success behavior.
		 *
		 * @return The wire token (e.g. {@code "mergeRow"}).
		 */
		public String wire() {
			return wire;
		}
	}

	/** The stable action id (menu-item key; also the submit's logical name). */
	public String id;

	/** The menu-item text. */
	public String label;

	/** Optional glyph/icon name (resolved by the shared icon registry). */
	public String icon;

	/** The URL this action submits to. */
	public String endpoint;

	/** The non-safe HTTP method wire token (see {@link Method#wire()}). */
	public String method;

	/** Optional confirmation prompt shown before the submit. */
	public String confirm;

	/** Optional form-source URL supplying the action's input fields. */
	public String form;

	/** Optional presentation hint wire token (see {@link Present#wire()}). */
	public String present;

	/** Optional success-behavior hint wire token (see {@link OnSuccess#wire()}). */
	public String onSuccess;

	/**
	 * Starts a new {@link RowAction} with the specified stable action id.
	 *
	 * @param id The stable action id.  Must not be <jk>null</jk> or blank.
	 * @return A new mutable {@link RowAction} to chain builder calls on.
	 * @throws IllegalArgumentException If {@code id} is <jk>null</jk> or blank.
	 */
	public static RowAction create(String id) {
		if (id == null || id.isBlank())
			throw iaex("RowAction id must not be null or blank.");
		var a = new RowAction();
		a.id = id;
		return a;
	}

	/**
	 * Sets the menu-item text.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RowAction label(String value) {
		label = value;
		return this;
	}

	/**
	 * Sets the glyph/icon name.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RowAction icon(String value) {
		icon = value;
		return this;
	}

	/**
	 * Sets the URL this action submits to.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RowAction endpoint(String value) {
		endpoint = value;
		return this;
	}

	/**
	 * Sets the non-safe HTTP method this action submits with.
	 *
	 * <p>
	 * Only a {@link Method} (POST/PUT/PATCH/DELETE) can be passed, so a safe method can never be declared &mdash;
	 * the build-time half of the HIGH-7 safe-method refusal.
	 *
	 * @param value The method.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RowAction method(Method value) {
		method = value.wire();
		return this;
	}

	/**
	 * Sets the confirmation prompt shown before the submit.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RowAction confirm(String value) {
		confirm = value;
		return this;
	}

	/**
	 * Sets the form-source URL supplying the action's input fields.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public RowAction form(String value) {
		form = value;
		return this;
	}

	/**
	 * Sets the presentation hint.
	 *
	 * @param value The presentation.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RowAction present(Present value) {
		present = value.wire();
		return this;
	}

	/**
	 * Sets the success-behavior hint.
	 *
	 * @param value The success behavior.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RowAction onSuccess(OnSuccess value) {
		onSuccess = value.wire();
		return this;
	}
}
