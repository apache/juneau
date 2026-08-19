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
package org.apache.juneau.rest.server.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.bean.html5.*;

/**
 * Builds the pill/badge {@code <span class="tag &lt;domain&gt; &lt;value&gt;">} markup consumed by
 * {@code chrome.css}'s {@code .tag.<domain>.<value>} rules.
 *
 * <p>
 * Returns an {@link HtmlElement} (a {@link Span}), <b>never a {@code String}</b> &mdash; per the design's
 * Trusted-HTML insertion contract, the only supported v1 insertion point is the FreeMarker {@code <@tag>} macro
 * (see {@code console-ui-freemarker}'s {@code ConsoleFreemarkerMixin}/base template), which serializes this
 * element and marks the result trusted via {@code HTMLOutputFormat.fromMarkup(...)}. A bare
 * {@code ${Tag.of(...)}} written directly in a template does <b>not</b> produce that markup (FreeMarker's
 * {@code DefaultObjectWrapper} would expose it as a bean, not call {@code toString()}) &mdash; this is a documented
 * footgun, not a bug.
 *
 * <h5 class='section'>Security:</h5>
 * <p>
 * Both {@code domain} and {@code value} are lowercased first, then REJECT (fail-closed, {@code IllegalArgumentException})
 * unless they full-string-match {@code ^[a-z0-9_-]+$}; the empty string REJECTs. Lowercasing happens
 * <i>before</i> the REJECT check, so e.g. {@code "Status"} is accepted (as {@code "status"}) while
 * {@code "<script>"} still REJECTs after lowercasing.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link TagDomain}
 * 	<li class='jc'>{@link TagHtmlRender}
 * </ul>
 *
 * @since 10.0.0
 */
public final class Tag {

	/** Anchored (full-string) guard applied to the lowercased domain/value. */
	private static final String PATTERN = "^[a-z0-9_-]+$";

	private Tag() {}

	/**
	 * Builds a {@code <span class="tag &lt;domain&gt; &lt;value&gt;">} pill/badge element.
	 *
	 * @param domain The pill domain (e.g. {@code "status"}). Lowercased, then must match {@code ^[a-z0-9_-]+$}.
	 * @param value The pill value (e.g. {@code "released"}). Lowercased, then must match {@code ^[a-z0-9_-]+$}.
	 * @return A new {@link Span}.
	 * @throws IllegalArgumentException If either argument is <jk>null</jk>, empty, or (after lowercasing) not in
	 * 	the legal shape.
	 */
	@SuppressWarnings({
		"java:S1452" // Intended public API: the wildcard return keeps this factory's contract at the HtmlElement abstraction (it currently returns a Span) without committing callers to the concrete type; narrowing it to Span would be a public-signature change.
	})
	public static HtmlElement<?> of(String domain, String value) {
		var d = normalize("domain", domain);
		var v = normalize("value", value);
		return new Span().class_("tag " + d + " " + v);
	}

	private static String normalize(String argName, String raw) {
		if (raw == null)
			throw iaex("Tag %s must not be null.", argName);
		var lower = raw.toLowerCase(Locale.ROOT);
		if (! lower.matches(PATTERN))
			throw iaex("Invalid tag %s: '%s'.  Must match %s (after lowercasing).", argName, raw, PATTERN);
		return lower;
	}
}
