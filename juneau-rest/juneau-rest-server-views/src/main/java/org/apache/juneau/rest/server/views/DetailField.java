/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  The ASF licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.  See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.rest.server.widgets.*;

/**
 * One field slot in a {@link DetailSection}.
 *
 * <p>
 * Values are filled from the expand GET JSON {@code fields} map.  The default {@link Format#TEXT} paints with
 * {@code textContent} only.  {@link Format#MARKDOWN} stamps {@code data-juneau-field-format="markdown"} on the
 * slot; the runtime copies allowlisted nodes from a {@code DOMParser} document and never assigns
 * {@code innerHTML}.  The expand JSON value for a markdown field is the HTML produced by a sanitizing markdown
 * renderer (see {@code juneau-rest-server-views-markdown}); it is not the raw markdown source.
 * {@link Format#SANITIZED_HTML} paints caller-sanitized rich HTML through the same never-{@code innerHTML}
 * allowlist-copy discipline, against a wider allowlist that admits {@code <img>} and the full table set.
 *
 * <p>
 * This does not bump {@link RowDetailDef#CONTRACT_VERSION}: the expand envelope is unchanged, the format
 * attribute is additive, and a TEXT-only consumer still paints unknown attributes via {@code textContent}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class DetailField {

	/**
	 * How the expand-JSON scalar is painted into the slot.
	 *
	 * <p>
	 * Each constant carries the lowercase token emitted on {@code data-juneau-field-format}.  {@link #TEXT} is
	 * omitted from the template (the default).
	 */
	public enum Format {

		/** Paint with {@code textContent}.  The default. */
		TEXT("text"),

		/**
		 * Treat the expand-JSON value as sanitizing-markdown HTML and copy allowlisted nodes into the slot.
		 * Never {@code innerHTML}.
		 */
		MARKDOWN("markdown"),

		/**
		 * Treat the expand-JSON value as rich HTML the <b>caller has already sanitized server-side</b>, and paint it
		 * through this runtime's own second allowlist &mdash; a wider one than {@link #MARKDOWN}'s, admitting
		 * {@code <img>} and the remaining table/typographic tags a full-fidelity rich-text body needs.
		 *
		 * <p>
		 * <b>This is not a raw-HTML sink.</b>  Like {@link #MARKDOWN}, the client parses the value with
		 * {@code DOMParser} into an inert document and copies allowlisted nodes via {@code createElement}/
		 * {@code createTextNode}; it never assigns {@code innerHTML}, and it never serializes back to a string (so
		 * the mutation-XSS class that a parse&rarr;serialize&rarr;reparse sanitizer is prone to cannot arise).
		 * Attributes are copied by an explicit per-tag allowlist, so {@code on*} handlers, {@code srcdoc},
		 * {@code style} and every other unnamed attribute cannot survive <i>by construction</i> rather than by a
		 * deny-list that has to anticipate them.  {@code href} and {@code src} are scheme-checked in two layers,
		 * not one: an explicit prefix reject for {@code javascript:}, {@code data:} and {@code vbscript:}, plus a
		 * <b>colon-fallback</b> rule underneath it that rejects any value containing a colon outside the
		 * explicitly allowlisted absolute prefixes ({@code http:}/{@code https:}/{@code mailto:} for
		 * {@code href}; {@code http:}/{@code https:} for {@code src}).  <b>The colon-fallback, not the named-
		 * scheme prefix check, is the load-bearing defense</b>: it is what still rejects an obfuscated spelling
		 * of one of those three schemes (e.g. one split by a stray whitespace/control character that a browser's
		 * URL parser strips before resolving, such as {@code java\tscript:}) &mdash; the prefix check alone would
		 * miss it, but the obfuscated string still contains a colon and matches none of the allowed prefixes, so
		 * it still fails closed. A future "simplification" to a longer bare {@code javascript:}/{@code data:}/
		 * {@code vbscript:} deny-list that drops the colon-fallback as apparently redundant would silently
		 * reopen that hole.
		 *
		 * <p>
		 * <b>The contract is nonetheless caller-sanitizes-first.</b>  This runtime's allowlist is a second,
		 * independent gate &mdash; defense in depth &mdash; not a substitute for a real server-side sanitizer.  Juneau
		 * takes on no HTML-sanitizer dependency and makes no claim to sanitize hostile input on the caller's behalf:
		 * an application pointing this format at externally-authored HTML is expected to have run that HTML through
		 * a dedicated allowlist sanitizer at its own trust boundary first.  What this format guarantees is narrower
		 * and worth stating exactly: <i>if that upstream pass is wrong, a script still does not execute here.</i>
		 *
		 * <p>
		 * Fidelity is bounded by the allowlist.  Tags outside it are unwrapped (their children are kept), so an
		 * unexpected element degrades to its text rather than rendering &mdash; a body that needs an element this
		 * allowlist does not name will silently lose that element's markup, not its content.
		 *
		 * <p>
		 * Like {@link #MARKDOWN}, a field with this format spans full width and may suppress its title with an empty
		 * {@link DetailField#title}.
		 */
		SANITIZED_HTML("sanitizedHtml");

		private final String wire;

		Format(String wire) {
			this.wire = wire;
		}

		/**
		 * Returns the lowercase wire token for this format.
		 *
		 * @return The wire token (e.g. <c>"markdown"</c>).
		 */
		public String wire() {
			return wire;
		}
	}

	/** The key into the expand JSON {@code fields} map.  Unique across the whole {@link RowDetailDef}. */
	public String data;

	/** The label shown above the value slot. */
	public String title;

	/** How the slot is painted.  <jk>null</jk> means {@link Format#TEXT}. */
	public Format format;

	/**
	 * Optional named renderer.  <jk>null</jk> means no renderer (TEXT or MARKDOWN paint).  Mutually exclusive
	 * with a non-{@link Format#TEXT} {@link #format}.
	 */
	public Render render;

	/**
	 * Optional <c>{property}</c> URL template consumed by the {@code linked} renderer (and any renderer that
	 * reads {@code meta.href}).  <jk>null</jk> means no template.  Independent of {@link #render} id.
	 */
	public String href;

	/**
	 * How many of the section's grid columns this field occupies.  <jk>null</jk> means {@link FieldSpan#ONE}.
	 *
	 * <p>
	 * A maximum, not a fixed width: it clamps as the grid steps down, and {@link FieldSpan#FULL} is identical to
	 * {@link FieldSpan#ONE} at one column.  A {@link Format#MARKDOWN} or {@link Format#SANITIZED_HTML} field spans
	 * full width whether or not this is set.
	 */
	public FieldSpan span;

	/**
	 * Optional controls painted in this field's value column &mdash; the <b>third</b> {@link ActionBar} host,
	 * beside {@link RowDetailDef#headerActions} and {@link DetailSection#actions}.  <jk>null</jk> / empty omits
	 * the bar and emits byte-identical markup to a field that never declared one.
	 *
	 * <p>
	 * A field may carry a value <b>and</b> a bar at once (a linked record's id beside a quiet <c>Unlink</c>): the
	 * bar is emitted as a sibling of the value slot, not in place of it, and nothing here decides which is
	 * visible.  Show/hide is {@link ActionRef#enabledWhen(String,Op,Object,String)}'s business alone.
	 *
	 * <p>
	 * Emphasis is <b>implicit</b>: a bar in a field row is always the quiet variant, so a field row cannot be made
	 * to out-shout the panel it is subordinate to.  {@link ActionRef} ids are validated against the enclosing
	 * view's {@code rowActions} by {@link RowDetailDef#validate(java.util.List)}, and this does not bump
	 * {@link RowDetailDef#CONTRACT_VERSION} &mdash; a third host is not a schema change.
	 */
	public ActionBar actions;

	/**
	 * Creates a field bound to the specified expand-JSON key.
	 *
	 * @param data The {@code fields} map key.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link DetailField}.
	 */
	public static DetailField of(String data) {
		if (data == null || data.isBlank())
			throw iaex("DetailField data must not be null or blank.");
		var f = new DetailField();
		f.data = data;
		return f;
	}

	/**
	 * Sets the label shown above the value slot.
	 *
	 * @param value The label.  May be <jk>null</jk> (the {@link #data} key is used as a fallback at emit time).
	 * 	An empty string suppresses the label (used for a full-width markdown body under a section title).
	 * @return This object.
	 */
	public DetailField title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets how the expand-JSON scalar is painted into the slot.
	 *
	 * @param value The format.  <jk>null</jk> means {@link Format#TEXT}.
	 * @return This object.
	 */
	public DetailField format(Format value) {
		format = value;
		return this;
	}

	/**
	 * Sets the named renderer using the <c>"id:field"</c> string sugar (see {@link Render#parse(String)}).
	 *
	 * @param value The render-id string.  Must not be <jk>null</jk> or blank.
	 * @return This object.
	 */
	public DetailField render(String value) {
		render = Render.parse(value);
		return this;
	}

	/**
	 * Sets the named renderer to a pre-built {@link Render}.
	 *
	 * @param value The renderer reference.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public DetailField render(Render value) {
		render = value;
		return this;
	}

	/**
	 * Sets the declarative <c>{property}</c> URL template (same shape as {@link Column#href}).
	 *
	 * @param value The template.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public DetailField href(String value) {
		href = value;
		return this;
	}

	/**
	 * Sets how many of the section's grid columns this field occupies.
	 *
	 * @param value The span.  <jk>null</jk> means {@link FieldSpan#ONE}.
	 * @return This object.
	 */
	public DetailField span(FieldSpan value) {
		span = value;
		return this;
	}

	/**
	 * Sets the controls painted in this field's value column.
	 *
	 * @param value The bar.  Can be <jk>null</jk> to unset; a <jk>null</jk> or empty bar emits nothing.
	 * @return This object.
	 */
	public DetailField actions(ActionBar value) {
		actions = value;
		return this;
	}
}
