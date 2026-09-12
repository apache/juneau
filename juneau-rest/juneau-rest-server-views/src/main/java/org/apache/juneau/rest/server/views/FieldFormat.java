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

/**
 * How a field-grid catalog entry (or a remaining declarative field) is painted.
 *
 * <p>
 * Each constant carries the lowercase token emitted on {@code format} / {@code data-juneau-field-format}.
 * {@link #TEXT} is omitted from the wire (the default).
 *
 * @since 10.0.0
 */
public enum FieldFormat {

	/** Paint with {@code textContent}.  The default. */
	TEXT("text"),

	/**
	 * Treat the value as sanitizing-markdown HTML and copy allowlisted nodes into the slot.
	 * Never {@code innerHTML}.
	 */
	MARKDOWN("markdown"),

	/**
	 * Treat the value as rich HTML the <b>caller has already sanitized server-side</b>, and paint it
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
	 * Like {@link #MARKDOWN}, a field with this format spans full width.
	 */
	SANITIZED_HTML("sanitizedHtml");

	private final String wire;

	FieldFormat(String wire) {
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
