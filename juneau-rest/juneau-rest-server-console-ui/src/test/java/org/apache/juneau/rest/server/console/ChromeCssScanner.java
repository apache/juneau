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

import java.util.*;
import java.util.regex.*;

/**
 * Test-only golden-file scanner for {@code chrome.css} (Phase 2 gate, S1/S3).
 *
 * <p>
 * Not shipped &mdash; this is a build-time regression check on the static classpath resource, not a runtime
 * feature. Fails a CSS source if:
 * <ul class='spaced-list'>
 * 	<li>{@code --jc-logo} appears anywhere (the logo is a hard-coded static rule, never a token);
 * 	<li>{@code var(--jc-} appears nested inside any function call other than the {@code var(} call itself (e.g.
 * 		nested in {@code linear-gradient(...)}/{@code rgb(...)}/{@code url(...)});
 * 	<li>a {@code var(--jc-*)} is the value of a url-capable property, UNLESS the property is
 * 		{@code background-image} and the token is one of the two whitelisted gradient tokens
 * 		({@code --jc-page-bg}, {@code --jc-avatar-bg}).
 * </ul>
 *
 * <p>
 * Does NOT fail on the static {@code url("data:image/svg+xml;...")} literal in {@code .jc-logo} &mdash; there is no
 * {@code var(--jc-} there.
 */
final class ChromeCssScanner {

	private ChromeCssScanner() {}

	/** The url-capable CSS properties denylist (S1's extended list). */
	private static final Set<String> URL_CAPABLE_PROPERTIES = Set.of(
		"background", "background-image", "background-size", "border", "cursor", "list-style", "content",
		"border-image", "border-image-source", "list-style-image", "mask-image", "filter", "clip-path");

	/** The only (property, token) pairs allowed to put a var(--jc-*) on a url-capable property. */
	private static final Set<String> GRADIENT_TOKEN_ALLOWLIST = Set.of("--jc-page-bg", "--jc-avatar-bg");

	private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
	private static final Pattern WHOLE_VALUE_VAR_JC =
		Pattern.compile("([a-zA-Z-]++)\\s*+:\\s*+var\\((--jc-[a-z0-9-]++)\\)\\s*+;");

	/**
	 * Scans the specified CSS source and returns a list of human-readable violation descriptions (empty if the CSS
	 * is clean).
	 *
	 * @param css The CSS source to scan.
	 * @return A list of violations, empty if none.
	 */
	static List<String> scan(String css) {
		var violations = new ArrayList<String>();
		var stripped = COMMENT.matcher(css).replaceAll("");

		if (stripped.contains("--jc-logo"))
			violations.add("'--jc-logo' custom property must not exist - the logo is a hard-coded static rule.");

		scanNestedVarJc(stripped, violations);
		scanUrlCapableSinks(stripped, violations);

		return violations;
	}

	/** Fails if {@code var(--jc-} is found nested inside any function call other than the {@code var(} call itself. */
	private static void scanNestedVarJc(String css, List<String> violations) {
		var i = 0;
		while (true) {
			var idx = css.indexOf('(', i);
			if (idx < 0)
				break;
			var j = idx - 1;
			while (j >= 0 && (Character.isLetterOrDigit(css.charAt(j)) || css.charAt(j) == '-'))
				j--;
			var name = css.substring(j + 1, idx).toLowerCase(Locale.ROOT);

			var depth = 1;
			var k = idx + 1;
			while (k < css.length() && depth > 0) {
				var c = css.charAt(k);
				if (c == '(')
					depth++;
				else if (c == ')')
					depth--;
				k++;
			}
			var inner = css.substring(idx + 1, Math.max(idx + 1, k - 1));

			if (! name.equals("var") && inner.contains("var(--jc-"))
				violations.add("'var(--jc-' nested inside '" + name + "(...)': " + name + "(" + inner + ")");

			i = idx + 1;
		}
	}

	/** Fails if a var(--jc-*) is the WHOLE value of a url-capable property, unless it's an allowlisted gradient token on background-image. */
	private static void scanUrlCapableSinks(String css, List<String> violations) {
		var m = WHOLE_VALUE_VAR_JC.matcher(css);
		while (m.find()) {
			var property = m.group(1).toLowerCase(Locale.ROOT);
			var token = m.group(2);
			if (! URL_CAPABLE_PROPERTIES.contains(property))
				continue;
			var allowed = property.equals("background-image") && GRADIENT_TOKEN_ALLOWLIST.contains(token);
			if (! allowed)
				violations.add("'" + token + "' sinks into url-capable property '" + property + "'.");
		}
	}
}
