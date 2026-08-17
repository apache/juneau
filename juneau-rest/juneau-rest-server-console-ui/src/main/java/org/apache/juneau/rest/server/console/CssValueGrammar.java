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
import java.util.regex.*;

/**
 * Normalizes and validates {@link Theme.Builder#token(String, String) Theme token} values against an
 * accept-known-safe allowlist grammar &mdash; the opposite of a {@code url(} blocklist.
 *
 * <p>
 * {@code url()} is deliberately not a production in any of the six allowed value shapes (hex color, functional
 * color, named/global keyword color, length/number list, font-family list, gradient function): every spelling of
 * {@code url(...)} REJECTs by construction, not by pattern-matching the threat.  Two ordered stages:
 *
 * <ol>
 * 	<li><b>Normalize (belt, runs first):</b> trim; REJECT any C0 (<code>\x00\u2013\x1F</code>) or C1/DEL
 * 		(<code>\x7F\u2013\x9F</code>) control character (this is what actually kills the {@code url\t(} /
 * 		{@code url\n(} CSS-hex reconstruction vector &mdash; a control character can never survive to be hex-escaped
 * 		by {@link CssValueEscaper} and decoded back into live whitespace); strip CSS comments
 * 		(<code>/&#42; &#42;/</code>, dotall &mdash; kills the {@code url/**&#47;(} vector); REJECT if the
 * 		comment-stripped remainder matches <code>(?i)url\s*\(</code> anywhere (belt-and-suspenders for {@code url (}
 * 		and any residual whitespace-before-paren form).
 * 	<li><b>Allowlist grammar (accept-known-safe):</b> the normalized value must match exactly one of the six shapes
 * 		below. Every production is a full-string {@code Pattern.matches(...)}-equivalent (anchored {@code ^...$}),
 * 		never {@code find(...)}.
 * </ol>
 *
 * <p>
 * The gradient production is deliberately tighter than "anything shaped like a function call": nested function
 * calls are restricted to exactly {@code rgb()}/{@code rgba()}/{@code hsl()}/{@code hsla()}/{@code calc()}
 * (case-insensitive) &mdash; there is no general {@code ident(} production, so {@code linear-gradient(url(evil))}
 * REJECTs at the grammar layer itself (a distinct, independent check from the normalization-stage {@code url(}
 * reject above), not merely because the top-level belt happened to catch it first.
 */
final class CssValueGrammar {

	private CssValueGrammar() {}

	//-----------------------------------------------------------------------------------------------------------------
	// Normalization
	//-----------------------------------------------------------------------------------------------------------------

	private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
	private static final Pattern URL_REJECT = Pattern.compile("(?i)url\\s*\\(");

	/**
	 * Normalizes and validates a token value, returning the normalized (comment-stripped, trimmed) value if it is
	 * accepted.
	 *
	 * @param value The raw candidate value.
	 * @return The normalized value.
	 * @throws IllegalArgumentException If the value is <jk>null</jk>, contains a control character, contains a
	 * 	{@code url(} production in any spelling, or does not match one of the allowed CSS value shapes.
	 */
	static String normalizeAndValidate(String value) {
		if (value == null)
			throw iaex("Theme token value must not be null.");

		// Scan the RAW (pre-trim) value for control characters first - String.trim() strips any character
		// <= U+0020 (which covers all of C0) from the boundaries, so trimming first would silently swallow a
		// leading/trailing C0 control character before this check ever saw it.
		for (var i = 0; i < value.length(); i++) {
			var c = value.charAt(i);
			if ((c >= 0x00 && c <= 0x1F) || (c >= 0x7F && c <= 0x9F))
				throw iaex("Theme token value contains an illegal control character at index %d.", i);
		}

		var trimmed = value.trim();
		var stripped = COMMENT.matcher(trimmed).replaceAll("");

		if (URL_REJECT.matcher(stripped).find())
			throw iaex("Theme token value must not contain a url() production.");

		if (isAllowedShape(stripped))
			return stripped;

		throw iaex("Theme token value is not one of the allowed CSS value shapes: '%s'.", value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Allowlist grammar
	//-----------------------------------------------------------------------------------------------------------------

	// 3/4/6/8-digit hex color.  (Not 5 or 7 - those are not legal CSS hex-color lengths.)
	private static final Pattern HEX_COLOR =
		Pattern.compile("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");

	// rgb()/rgba()/hsl()/hsla() with a numeric/%/,//whitespace/- argument charset only.
	private static final Pattern FUNCTIONAL_COLOR =
		Pattern.compile("^(?i:rgb|rgba|hsl|hsla)\\([0-9.,%\\s-]+\\)$");

	// <number> + allowlisted unit, one or more space-separated (unit optional, e.g. bare "0").
	private static final String UNIT = "px|em|rem|%|vh|vw|vmin|vmax|pt|ch|fr|deg|turn|rad|s|ms";
	private static final Pattern LENGTH_LIST = Pattern.compile(
		"^-?\\d+(?:\\.\\d+)?(?:" + UNIT + ")?(?:\\s+-?\\d+(?:\\.\\d+)?(?:" + UNIT + ")?)*$");

	// Comma-separated font-family list: each item a quoted string (no control/quote breakout) or a bare identifier.
	private static final Pattern FONT_FAMILY_ITEM_SQ = Pattern.compile("^'[^'\\\\]*'$");
	private static final Pattern FONT_FAMILY_ITEM_DQ = Pattern.compile("^\"[^\"\\\\]*\"$");
	private static final Pattern FONT_FAMILY_ITEM_BARE = Pattern.compile("^[a-zA-Z][a-zA-Z0-9 -]*$");

	// Gradient function: name, then a nested-function-call allowlist scan, then a strict argument charset.
	private static final Pattern GRADIENT_OUTER =
		Pattern.compile("^(?i:repeating-)?(?i:linear|radial|conic)-gradient\\(.*\\)$", Pattern.DOTALL);
	private static final Pattern FUNCTION_CALL_TOKEN = Pattern.compile("([a-zA-Z][a-zA-Z-]*)\\(");
	private static final Set<String> GRADIENT_ALLOWED_NESTED_FUNCTIONS = Set.of("rgb", "rgba", "hsl", "hsla", "calc");
	private static final Pattern GRADIENT_ARG_CHARSET = Pattern.compile("^[0-9a-zA-Z%#.,\\s()-]*$");

	private static final Set<String> NAMED_COLORS_AND_KEYWORDS = namedColorsAndKeywords();

	private static boolean isAllowedShape(String v) {
		return HEX_COLOR.matcher(v).matches()
			|| FUNCTIONAL_COLOR.matcher(v).matches()
			|| NAMED_COLORS_AND_KEYWORDS.contains(v.toLowerCase(Locale.ROOT))
			|| LENGTH_LIST.matcher(v).matches()
			|| isFontFamilyList(v)
			|| isGradient(v);
	}

	private static boolean isFontFamilyList(String v) {
		var items = v.split(",", -1);
		if (items.length == 0)
			return false;
		for (var item : items) {
			var t = item.trim();
			if (t.isEmpty())
				return false;
			if (! (FONT_FAMILY_ITEM_SQ.matcher(t).matches() || FONT_FAMILY_ITEM_DQ.matcher(t).matches() || FONT_FAMILY_ITEM_BARE.matcher(t).matches()))
				return false;
		}
		return true;
	}

	private static boolean isGradient(String v) {
		if (! GRADIENT_OUTER.matcher(v).matches())
			return false;
		var inner = v.substring(v.indexOf('(') + 1, v.length() - 1);
		var m = FUNCTION_CALL_TOKEN.matcher(inner);
		while (m.find()) {
			var fn = m.group(1).toLowerCase(Locale.ROOT);
			if (! GRADIENT_ALLOWED_NESTED_FUNCTIONS.contains(fn))
				return false;
		}
		return GRADIENT_ARG_CHARSET.matcher(inner).matches();
	}

	/** CSS Color Module named colors + CSS-wide/global keywords, lowercased. */
	private static Set<String> namedColorsAndKeywords() {
		var s = new HashSet<>(Set.of(
			"aliceblue", "antiquewhite", "aqua", "aquamarine", "azure", "beige", "bisque", "black", "blanchedalmond",
			"blue", "blueviolet", "brown", "burlywood", "cadetblue", "chartreuse", "chocolate", "coral",
			"cornflowerblue", "cornsilk", "crimson", "cyan", "darkblue", "darkcyan", "darkgoldenrod", "darkgray",
			"darkgreen", "darkgrey", "darkkhaki", "darkmagenta", "darkolivegreen", "darkorange", "darkorchid",
			"darkred", "darksalmon", "darkseagreen", "darkslateblue", "darkslategray", "darkslategrey",
			"darkturquoise", "darkviolet", "deeppink", "deepskyblue", "dimgray", "dimgrey", "dodgerblue",
			"firebrick", "floralwhite", "forestgreen", "fuchsia", "gainsboro", "ghostwhite", "gold", "goldenrod",
			"gray", "green", "greenyellow", "grey", "honeydew", "hotpink", "indianred", "indigo", "ivory", "khaki",
			"lavender", "lavenderblush", "lawngreen", "lemonchiffon", "lightblue", "lightcoral", "lightcyan",
			"lightgoldenrodyellow", "lightgray", "lightgreen", "lightgrey", "lightpink", "lightsalmon",
			"lightseagreen", "lightskyblue", "lightslategray", "lightslategrey", "lightsteelblue", "lightyellow",
			"lime", "limegreen", "linen", "magenta", "maroon", "mediumaquamarine", "mediumblue", "mediumorchid",
			"mediumpurple", "mediumseagreen", "mediumslateblue", "mediumspringgreen", "mediumturquoise",
			"mediumvioletred", "midnightblue", "mintcream", "mistyrose", "moccasin", "navajowhite", "navy",
			"oldlace", "olive", "olivedrab", "orange", "orangered", "orchid", "palegoldenrod", "palegreen",
			"paleturquoise", "palevioletred", "papayawhip", "peachpuff", "peru", "pink", "plum", "powderblue",
			"purple", "rebeccapurple", "red", "rosybrown", "royalblue", "saddlebrown", "salmon", "sandybrown",
			"seagreen", "seashell", "sienna", "silver", "skyblue", "slateblue", "slategray", "slategrey", "snow",
			"springgreen", "steelblue", "tan", "teal", "thistle", "tomato", "turquoise", "violet", "wheat", "white",
			"whitesmoke", "yellow", "yellowgreen"));
		s.addAll(Set.of("transparent", "currentcolor", "inherit", "initial", "unset"));
		return Set.copyOf(s);
	}
}
