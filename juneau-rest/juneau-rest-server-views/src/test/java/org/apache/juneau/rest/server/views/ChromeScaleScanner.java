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

import java.util.*;
import java.util.regex.*;

/**
 * Test-only, <b>private</b> stylesheet scanner enforcing the shared chrome scale contract on
 * {@code juneau-views.css}: the named dimensional and type steps every chrome surface spends.
 *
 * <p>
 * Not shipped and deliberately not public API, mirroring the discipline of the {@link RawContentSinkScanner}
 * test-only guard this class is modeled on and sits beside.
 *
 * <h5 class='section'>What it enforces</h5>
 * <ol>
 * 	<li><b>Declaration.</b> Every step in {@link #scale()} is declared exactly once in the file. A step declared
 * 		twice has two values in flight; a step declared zero times cannot be spent.
 * 	<li><b>No literal that duplicates a step.</b> On the properties a chrome surface actually sizes with, a
 * 		literal whose value equals a declared step is a violation &mdash; if a value equals a step, spend the
 * 		step. This is the half that turns the contract's own "never write a literal that duplicates a step" rule
 * 		from advice into a build failure.
 * </ol>
 *
 * <h5 class='section'>Why the second half is narrower than it first looks, and deliberately so</h5>
 * <p>
 * The prohibition binds <b>computed</b> values, not written ones, and an {@code em} value can only be computed
 * once you know its declaration's font-size context. That context is <i>not</i> uniform in this file &mdash; it
 * runs from a UA/system control font through 10.2px, 12px, 16px and up &mdash; so a checker that assumed one
 * context would reproduce, in test form, exactly the defect this contract exists to close. Two consequences,
 * both intentional:
 * <ul>
 * 	<li><b>{@code em} values are never evaluated here.</b> Only absolute {@code px}/{@code rem} literals are
 * 		compared. In particular a form control that sets no {@code font-size} of its own resolves its {@code em}
 * 		against a UA/system font that varies by browser and platform, so a computation against one engine's
 * 		value would report a violation on that engine and pass on every other. This scanner must never do that.
 * 	<li><b>{@code rem} is normalised to px at 16px per rem.</b> That is safe in a way {@code em} is not: nothing
 * 		in this file, and nothing in the console chrome that themes it, sets a {@code font-size} on
 * 		{@code html}/{@code body}, so the root context is the browser default in both a themeless and a themed
 * 		render.
 * </ul>
 *
 * <h5 class='section'>Provisional steps are excluded from the second half</h5>
 * <p>
 * A step whose value is still provisional has not been rendered on a views surface yet. Binding the file's
 * existing declarations to it would tie unrelated surfaces to a number the surface that owns it is going to
 * re-decide &mdash; which is the "re-value a step out from under the other surfaces" failure, arrived at from
 * the other direction. Provisional steps are still checked for {@link #scale() declaration}.
 *
 * <h5 class='section'>Why there is a recorded-exception table rather than a clean sheet</h5>
 * <p>
 * {@link #recordedLiterals()} lists every literal that stays a literal, each with a reason. It exists so the
 * check is a <b>ratchet</b>: the recorded set is what the file looks like today, and anything new is a failure.
 * An entry is not an excuse &mdash; a reader can see precisely which declarations have not been routed onto the
 * ladder and why. The table is asserted to be non-vacuous by the accompanying test: an entry matching nothing is
 * itself a failure, so the list cannot rot into a silent allow-everything.
 */
final class ChromeScaleScanner {

	private ChromeScaleScanner() {}

	/** The family a step belongs to, which decides the properties it can legitimately be spent on. */
	enum Family { SPACE, CONTROL_HEIGHT, CONTROL_PADDING_X, FONT_SIZE, LINE_HEIGHT, GLYPH }

	/**
	 * One named step.
	 *
	 * @param token The custom-property name.
	 * @param value The declared value, exactly as written in the stylesheet.
	 * @param confirmed Whether the value is settled (as opposed to provisional pending a surface that renders it).
	 * @param family The step's family.
	 */
	record Step(String token, String value, boolean confirmed, Family family) {}

	/**
	 * One literal that deliberately stays a literal.
	 *
	 * @param selector A substring of the owning rule's selector text.
	 * @param property The property name.
	 * @param value The literal value, exactly as written.
	 * @param reason Why it is not spending a step.
	 */
	record RecordedLiteral(String selector, String property, String value, String reason) {}

	/** One declaration parsed out of the stylesheet. */
	record Decl(String selector, String property, String value) {}

	/** The scan outcome. */
	record Result(List<String> violations, List<Decl> checked, Set<RecordedLiteral> matchedRecords) {}

	private static final List<Step> SCALE = List.of(
		new Step("--jc-space-1", "4px", true, Family.SPACE),
		new Step("--jc-space-2", "8px", true, Family.SPACE),
		new Step("--jc-space-3", "12px", true, Family.SPACE),
		new Step("--jc-space-4", "16px", true, Family.SPACE),
		new Step("--jc-space-5", "24px", true, Family.SPACE),
		new Step("--jc-space-6", "32px", true, Family.SPACE),
		new Step("--jc-chrome-control-height", "32px", true, Family.CONTROL_HEIGHT),
		new Step("--jc-chrome-control-height-compact", "28px", true, Family.CONTROL_HEIGHT),
		new Step("--jc-chrome-control-padding-x", "10px", true, Family.CONTROL_PADDING_X),
		new Step("--jc-chrome-control-padding-x-wide", "14px", true, Family.CONTROL_PADDING_X),
		new Step("--jc-chrome-font-size-1", "0.75rem", true, Family.FONT_SIZE),
		new Step("--jc-chrome-font-size-2", "0.8125rem", true, Family.FONT_SIZE),
		new Step("--jc-chrome-font-size-3", "0.875rem", true, Family.FONT_SIZE),
		new Step("--jc-chrome-line-height", "1.2", true, Family.LINE_HEIGHT),
		new Step("--jc-chrome-glyph-size", "16px", true, Family.GLYPH),
		new Step("--jc-chrome-glyph-size-small", "12px", true, Family.GLYPH)
	);

	/**
	 * Every literal that deliberately stays a literal, with the reason it is not spending a step.
	 *
	 * <p>
	 * Grouped by reason rather than by file order, so a reader can see how many distinct reasons there actually
	 * are. Three, at present.
	 */
	private static final List<RecordedLiteral> RECORDED_LITERALS = List.of(
		// (1) The table cell type context. This value is what every `em` in the DataTables child row resolves
		// against, so it is deliberately NOT a token: a theme that overrode the token would silently re-resolve
		// the whole detail-panel family, which is the exact dependency pinning it was meant to remove.
		new RecordedLiteral("table[data-juneau-view], table.dataTable", "font-size", "0.75rem",
			"anchors the table type context; a token here would be overridable and the pin would stop pinning"),
		new RecordedLiteral("> thead > tr > th", "font-size", "0.75rem",
			"pins the cell type context; a token here would be overridable and the pin would stop pinning"),
		new RecordedLiteral("> tbody > tr > th", "font-size", "0.75rem",
			"pins the cell type context; a token here would be overridable and the pin would stop pinning"),

		// (2) The card grid's gap sizes itself in `rem`. The membership sweep behind the spacing ladder evaluated
		// `em`-valued declarations only, so this was never assessed against a step and routing it now would be a
		// card-grid density decision this contract does not own. (Its two sibling font-size literals, formerly
		// recorded here for the same reason, are now tokenized onto --jc-chrome-font-size-1, so the pinning
		// rationale above no longer applies to them and they no longer need an exception.)
		new RecordedLiteral(".juneau-view-card-fields", "gap", "0.4rem 0.75rem",
			"card-grid rem sizing, never assessed by the em-valued membership sweep"),

		// (3) A third glyph site at the small glyph size, in a widget the glyph-role naming enumerated only two
		// consumers for. Routing it is pixel-neutral but adds a consumer to a named role, which is a decision
		// for whoever owns the row-detail control rather than one to take in passing.
		new RecordedLiteral(".juneau-view-detail-control svg", "width", "12px",
			"a third small-glyph consumer; enrolling it widens a named glyph role and is not this contract's call"),
		new RecordedLiteral(".juneau-view-detail-control svg", "height", "12px",
			"a third small-glyph consumer; enrolling it widens a named glyph role and is not this contract's call")
	);

	/**
	 * Properties a chrome surface sizes with, and the step families each may legitimately spend.
	 *
	 * <p>
	 * {@code padding-left}/{@code padding-right} are the longhand-only reading of {@code CONTROL_PADDING_X}
	 * (the {@code padding} shorthand is not parsed to isolate its horizontal component - a shorthand
	 * declaration hiding the value is an accepted, stated limitation).
	 */
	private static final Map<String,Set<Family>> CHECKED_PROPERTIES = Map.of(
		"height", Set.of(Family.CONTROL_HEIGHT, Family.SPACE, Family.GLYPH),
		"min-height", Set.of(Family.CONTROL_HEIGHT, Family.SPACE),
		"font-size", Set.of(Family.FONT_SIZE),
		"gap", Set.of(Family.SPACE),
		"line-height", Set.of(Family.LINE_HEIGHT),
		"width", Set.of(Family.GLYPH),
		"padding-left", Set.of(Family.CONTROL_PADDING_X),
		"padding-right", Set.of(Family.CONTROL_PADDING_X)
	);

	/** {@code width} is only a scale property on an SVG glyph; elsewhere it is a content measure. */
	private static final String SVG_SELECTOR_SUFFIX = "svg";

	/** Matches one {@code selector { ... }} rule block. */
	private static final Pattern RULE = Pattern.compile("([^{}]+)\\{([^{}]*)\\}");

	/** Matches a {@code var(...)} reference, including any fallback, so a fallback is not read as a bare literal. */
	private static final Pattern VAR_REF = Pattern.compile("var\\([^()]*(?:\\([^()]*\\)[^()]*)*\\)");

	/** Matches an absolute length/size token. */
	private static final Pattern ABSOLUTE = Pattern.compile("(?<![\\w.-])(\\d*\\.?\\d+)(px|rem)(?![\\w-])");

	/**
	 * Matches a bare, unitless decimal. {@code line-height}'s scale step ({@code --jc-chrome-line-height},
	 * {@code 1.2}) is the only step value in the ladder with no {@code px}/{@code rem} unit, so this pattern is
	 * used only for the {@code line-height} property (see {@link #findDuplicatedStep}) - it must never widen
	 * matching for any other property.
	 */
	private static final Pattern BARE_NUMBER = Pattern.compile("(?<![\\w.-])(\\d*\\.?\\d+)(?![\\w.%-])");

	/** The steps this contract declares. */
	static List<Step> scale() {
		return SCALE;
	}

	/** Every literal deliberately left as a literal, with its reason. */
	static List<RecordedLiteral> recordedLiterals() {
		return RECORDED_LITERALS;
	}

	/**
	 * Reports any step not declared exactly once in the stylesheet.
	 *
	 * @param css The stylesheet text.
	 * @return One message per step that is missing or declared more than once; empty when every step is sound.
	 */
	static List<String> checkDeclaredExactlyOnce(String css) {
		var code = stripComments(css);
		var out = new ArrayList<String>();
		for (var s : SCALE) {
			var m = Pattern.compile(Pattern.quote(s.token()) + "\\s*:\\s*([^;}]+)").matcher(code);
			var found = new ArrayList<String>();
			while (m.find())
				found.add(m.group(1).trim());
			if (found.size() != 1) {
				out.add(s.token() + ": expected exactly one declaration, found " + found.size() + " " + found);
			} else if (!found.get(0).equals(s.value())) {
				out.add(s.token() + ": declared as '" + found.get(0) + "', contract says '" + s.value() + "'");
			}
		}
		return out;
	}

	/**
	 * Scans for literals that duplicate a declared step on a property the scale covers.
	 *
	 * @param css The stylesheet text.
	 * @return The violations, the declarations actually examined, and which recorded exceptions were matched.
	 */
	static Result scan(String css) {
		return scan(css, SCALE);
	}

	/**
	 * Scans against an explicit step list rather than {@link #SCALE}.
	 *
	 * <p>
	 * A test-only seam: {@link #SCALE} is {@code private static final} and cannot carry a synthetic step (nor
	 * should it - {@code a01}/{@code a02} require every declared step to be real and declared exactly once in
	 * the shipped stylesheet). This overload lets a test exercise the provisional-exemption branch against a
	 * scale of its own choosing, without widening {@link #SCALE} itself.
	 *
	 * @param css The stylesheet text.
	 * @param scale The step list to check against, in place of {@link #SCALE}.
	 * @return The violations, the declarations actually examined, and which recorded exceptions were matched.
	 */
	static Result scan(String css, List<Step> scale) {
		var violations = new ArrayList<String>();
		var checked = new ArrayList<Decl>();
		var matched = new LinkedHashSet<RecordedLiteral>();
		var code = stripComments(css);

		var rules = RULE.matcher(code);
		while (rules.find()) {
			var selector = normalise(rules.group(1));
			if (selector.startsWith("@") || selector.isEmpty())
				continue;
			for (var d : declarations(selector, rules.group(2))) {
				var families = CHECKED_PROPERTIES.get(d.property());
				if (families == null)
					continue;
				if ("width".equals(d.property()) && !isSvgSelector(selector))
					continue;
				checked.add(d);
				var hit = findDuplicatedStep(d, families, scale);
				if (hit == null)
					continue;
				var record = findRecord(d);
				if (record != null) {
					matched.add(record);
					continue;
				}
				violations.add(d.selector() + " { " + d.property() + ": " + d.value() + " } duplicates the step "
					+ hit.token() + " (" + hit.value() + "). If a value equals a step, spend the step - or add a "
					+ "recorded exception with the reason it cannot.");
			}
		}
		return new Result(violations, checked, matched);
	}

	/**
	 * The confirmed step of an eligible family whose value the declaration writes out as a literal.
	 *
	 * <p>
	 * Role-named families are searched before the spacing ladder, so a 32px {@code height} is reported against
	 * {@code --jc-chrome-control-height} rather than against the numerically-identical {@code --jc-space-6}. Both
	 * are violations either way; naming the apter one is what makes the message actionable.
	 */
	private static Step findDuplicatedStep(Decl d, Set<Family> families, List<Step> scale) {
		// line-height's step value is unitless, so it needs bare-number matching instead of the general
		// px/rem literal matcher - scoped to this one property so no other property's matching widens (LD-2).
		var lineHeight = "line-height".equals(d.property());
		var literals = lineHeight ? bareNumberValues(d.value()) : absoluteValues(d.value());
		if (literals.isEmpty())
			return null;
		for (var roleNamed : List.of(true, false)) {
			for (var s : scale) {
				if (!s.confirmed() || !families.contains(s.family()) || (s.family() == Family.SPACE) == roleNamed)
					continue;
				var stepValue = lineHeight ? bareValue(s.value()) : toPx(s.value());
				if (stepValue != null && literals.contains(stepValue))
					return s;
			}
		}
		return null;
	}

	private static RecordedLiteral findRecord(Decl d) {
		for (var r : RECORDED_LITERALS)
			if (d.selector().contains(r.selector()) && r.property().equals(d.property()) && r.value().equals(d.value()))
				return r;
		return null;
	}

	/** True if any comma-separated selector in the group targets an {@code svg} element. */
	private static boolean isSvgSelector(String selector) {
		for (var s : selector.split(","))
			if (s.trim().endsWith(SVG_SELECTOR_SUFFIX))
				return true;
		return false;
	}

	/**
	 * Every absolute value in a declaration, normalised to px. {@code var(...)} expressions are removed first, so
	 * a token's own fallback (e.g. {@code var(--jc-card-gap, 1rem)}) is not mistaken for a bare literal - the
	 * declaration is already spending a token, which is the thing the rule asks for.
	 */
	private static Set<Double> absoluteValues(String value) {
		var out = new LinkedHashSet<Double>();
		var m = ABSOLUTE.matcher(VAR_REF.matcher(value).replaceAll(" "));
		while (m.find())
			out.add(px(Double.parseDouble(m.group(1)), m.group(2)));
		return out;
	}

	private static Double toPx(String value) {
		var m = ABSOLUTE.matcher(value);
		return m.find() ? px(Double.parseDouble(m.group(1)), m.group(2)) : null;
	}

	/**
	 * Every bare unitless number in a declaration's value, {@code var(...)} fallbacks excluded. Only ever called
	 * for the {@code line-height} property (see {@link #findDuplicatedStep}).
	 */
	private static Set<Double> bareNumberValues(String value) {
		var out = new LinkedHashSet<Double>();
		var m = BARE_NUMBER.matcher(VAR_REF.matcher(value).replaceAll(" "));
		while (m.find())
			out.add(Double.parseDouble(m.group(1)));
		return out;
	}

	/** The step-value counterpart to {@link #bareNumberValues}: a step's own value read as a bare number. */
	private static Double bareValue(String value) {
		var m = BARE_NUMBER.matcher(value);
		return m.find() ? Double.parseDouble(m.group(1)) : null;
	}

	/** Normalises to px. {@code rem} resolves at the browser default because nothing here sets a root font-size. */
	private static double px(double n, String unit) {
		return "rem".equals(unit) ? n * 16.0 : n;
	}

	private static List<Decl> declarations(String selector, String body) {
		var out = new ArrayList<Decl>();
		for (var raw : body.split(";")) {
			var colon = raw.indexOf(':');
			if (colon < 0)
				continue;
			var property = raw.substring(0, colon).trim().toLowerCase(Locale.ROOT);
			var value = normalise(raw.substring(colon + 1));
			if (property.isEmpty() || property.startsWith("--") || value.isEmpty())
				continue;
			out.add(new Decl(selector, property, value));
		}
		return out;
	}

	private static String normalise(String s) {
		return s.replaceAll("\\s+", " ").trim();
	}

	/** Blanks out comment content so a value mentioned in a comment is not read as a declaration. */
	private static String stripComments(String s) {
		var out = new StringBuilder(s.length());
		var i = 0;
		while (i < s.length()) {
			if (s.charAt(i) == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') {
				i += 2;
				while (i + 1 < s.length() && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
					if (s.charAt(i) == '\n')
						out.append('\n');
					i++;
				}
				i = Math.min(s.length(), i + 2);
				continue;
			}
			out.append(s.charAt(i));
			i++;
		}
		return out.toString();
	}
}
