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
package org.apache.juneau.rest.server.mcp;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

/**
 * Hand-rolled, dependency-free compiler and reverse matcher for the narrow, reliably-invertible subset of
 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc6570">RFC 6570</a> URI templates used by MCP
 * resource-template reads.
 *
 * <p>
 * A template is {@link #compile(String) compiled} once into an immutable matcher that exposes:
 * <ul>
 * 	<li>{@link #isReverseMatchable()} - whether the template is one of the settled reversible forms (simple,
 * 		reserved, fragment, path, label, or query scalar expressions, with no explode/prefix modifier and no
 * 		ambiguous adjacency to another expression);
 * 	<li>{@link #literalOctetCount()} - the number of normalized literal octets outside <c>{...}</c> expressions,
 * 		for registry specificity ranking;
 * 	<li>{@link #variableCount()} and {@link #variableNames()} - the declared template variables, in declaration
 * 		order; and
 * 	<li>{@link #match(String)} - reverse-matches a concrete URI against a reverse-matchable template, returning
 * 		an immutable, insertion-ordered, percent-decoded variable map, or <jk>null</jk> if the URI does not match.
 * </ul>
 *
 * <h5 class='section'>Reverse-matchable forms:</h5>
 * <p>
 * Non-explode, non-prefix simple scalar <c>{var}</c>, reserved scalar <c>{+var}</c>, fragment scalar
 * <c>{#var}</c>, path segments <c>{/var}</c> / <c>{/x,y}</c>, label segments <c>{.var}</c> / <c>{.x,y}</c>, query
 * start <c>{?x}</c> / <c>{?x,y}</c>, and query continuation <c>{&amp;x}</c> / <c>{&amp;x,y}</c>. All other legal
 * RFC 6570 forms - exploded (<c>*</c>), prefixed (<c>:n</c>), matrix (<c>;</c>), multi-variable simple/reserved/
 * fragment expressions, and expressions with ambiguous adjacency to a neighboring expression - parse
 * successfully (so they remain listable and completable) but are never selected for a template-backed read;
 * {@link #match(String)} always returns <jk>null</jk> for them.
 *
 * <h5 class='section'>Percent-encoding normalization:</h5>
 * <p>
 * Matching and literal-specificity counting follow RFC 3986 percent-normalization implemented fresh by this
 * class: valid <c>%HH</c> triplets are hex-uppercased; triplets that decode to an unreserved octet
 * (<c>ALPHA</c> / <c>DIGIT</c> / <c>-</c> / <c>.</c> / <c>_</c> / <c>~</c>) normalize to their literal
 * character for comparison; triplets that decode to a reserved octet stay encoded while match boundaries are
 * computed, so an encoded <js>"%2F"</js> is data inside a <c>{var}</c> segment rather than a path separator;
 * captured values are then UTF-8 percent-decoded before being placed in the result map; and <c>+</c> is always
 * a literal plus (this class never applies {@code application/x-www-form-urlencoded} plus-as-space semantics).
 * A concrete URI with a malformed percent escape, or a capture whose decoded bytes are not valid UTF-8, never
 * matches. This class deliberately does <b>not</b> reuse
 * {@code org.apache.juneau.commons.utils.StringUtils} <c>urlEncode</c>/<c>urlDecode</c>/<c>fixUrl</c>/
 * <c>urlEncodePath</c> or the SVL <c>EncodingFunctions</c>: those implement form-urlencoded semantics
 * (<c>+</c> as space) and lack hex-case normalization, reserved-octet preservation, and malformed-escape
 * rejection, all of which contradict the rules above.
 *
 * <h5 class='section'>Ambiguous-capture resolution:</h5>
 * <p>
 * A reversible template compiles to an alternating sequence of literal and capture pieces; because an
 * expression with ambiguous adjacency to another expression is rejected as non-reverse-matchable (see above),
 * a capture piece in a reverse-matchable template is always immediately followed by either a literal piece or
 * the end of the template, never by another capture. When a reserved/fragment (unbounded) or path/label/simple
 * (bounded-by-<c>/</c>) capture is immediately followed by a literal, {@link #match(String)} resolves the
 * capture's length via a single longest-to-shortest search for the position at which that immediately-following
 * literal text occurs, using {@link String#lastIndexOf(String, int)}. This deterministically implements "the
 * longest capture that still permits the (immediately following) literal to match" without whole-suffix
 * backtracking across multiple capture/literal boundaries, which keeps matching a bounded, linear-in-input scan
 * per capture with no regex-style catastrophic backtracking risk, at the cost of not re-trying an earlier
 * capture's length choice if a later, unrelated piece of the template fails to match. That scope narrowing is
 * intentional and consistent with this matcher covering reliable inversion of a narrow subset, not a complete
 * RFC 6570 engine.
 *
 * <h5 class='section'>Scope:</h5>
 * <p>
 * This matcher is intentionally MCP-local. It is not registered as a general-purpose RFC 3986/6570 utility;
 * if a second real consumer outside MCP resource templates needs equivalent percent-normalization or
 * URI-template parsing, that would justify extracting a shared helper into {@code juneau-commons} at that time,
 * not before.
 */
public final class McpUriTemplateMatcher {

	//-----------------------------------------------------------------------------------------------------------------
	// Compilation entry point
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Parses and compiles a single RFC 6570 URI template.
	 *
	 * <p>
	 * Registration syntax is validated independently of reverse-matchability: a syntactically legal template
	 * outside the reversible subset compiles successfully with {@link #isReverseMatchable()} returning
	 * <jk>false</jk>, so it remains listable and completable even though {@link #match(String)} always returns
	 * <jk>null</jk> for it.
	 *
	 * @param uriTemplate The URI template to compile. Must not be <jk>null</jk> or blank.
	 * @return A new compiled matcher.
	 * @throws IllegalArgumentException If the template is <jk>null</jk>/blank, has malformed braces, has a
	 * 	malformed percent escape, has invalid operator/varspec grammar, has a literal segment containing an
	 * 	invalid UTF-8 percent-escape sequence, or declares the same variable name more than once.
	 */
	@SuppressWarnings({
		"java:S3776" // Intentional: hand-rolled RFC 6570 reverse matcher; control-flow verified by McpUriTemplateMatcher_Test, refactor would risk matching correctness.
	})
	public static McpUriTemplateMatcher compile(String uriTemplate) {
		if (uriTemplate == null || uriTemplate.isBlank())
			throw iaex("Malformed URI template ''%s'': template must not be null or blank", uriTemplate);

		var tokens = tokenize(uriTemplate);
		var declaredVars = new LinkedHashSet<String>();
		var pieces = new ArrayList<Piece>();
		var matchable = true;
		var literalOctetCount = 0;
		RawToken previous = null;

		for (var token : tokens) {
			if (token instanceof RawLiteral lit) {
				var normalized = normalizeLiteralStrict(lit.text(), uriTemplate);
				literalOctetCount += countOctets(normalized);
				pieces.add(new LiteralPiece(normalized));
			} else if (token instanceof RawExpr expr) {
				if (previous instanceof RawExpr)
					matchable = false;
				var parsed = parseExpr(expr.body(), uriTemplate, expr.index());
				for (var v : parsed.vars()) {
					if (! declaredVars.add(v.name()))
						throw iaex("Malformed URI template ''%s'': duplicate variable ''%s''", uriTemplate, v.name());
				}
				if (isExprReverseMatchable(parsed))
					pieces.addAll(desugar(parsed));
				else
					matchable = false;
			}
			previous = token;
		}

		return new McpUriTemplateMatcher(uriTemplate, matchable, literalOctetCount, List.copyOf(declaredVars),
			List.copyOf(pieces));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance state and accessors
	//-----------------------------------------------------------------------------------------------------------------

	private final String template;
	private final boolean reverseMatchable;
	private final int literalOctetCount;
	private final List<String> variableNames;
	private final List<Piece> pieces;

	private McpUriTemplateMatcher(String template, boolean reverseMatchable, int literalOctetCount,
			List<String> variableNames, List<Piece> pieces) {
		this.template = template;
		this.reverseMatchable = reverseMatchable;
		this.literalOctetCount = literalOctetCount;
		this.variableNames = variableNames;
		this.pieces = pieces;
	}

	/**
	 * The original source template this matcher was compiled from.
	 *
	 * @return The original template string. Never <jk>null</jk>.
	 */
	public String template() {
		return template;
	}

	/**
	 * Whether this template is one of C4's reversible forms and therefore eligible for template-backed reads.
	 *
	 * @return <jk>true</jk> if {@link #match(String)} can ever return a non-<jk>null</jk> result.
	 */
	public boolean isReverseMatchable() {
		return reverseMatchable;
	}

	/**
	 * The count of normalized literal URI octets outside <c>{...}</c> expressions.
	 *
	 * <p>
	 * A normalized percent triplet counts as one literal octet, not three source characters. This is exposed
	 * purely as ranking metadata; this class applies no ranking itself. Registry code performing final ranking
	 * must not invent additional tie-breakers (operator type, capture width, or value length) beyond this count,
	 * {@link #variableCount()}, and registration order.
	 *
	 * @return The literal octet count.
	 */
	public int literalOctetCount() {
		return literalOctetCount;
	}

	/**
	 * The number of distinct variables declared by this template.
	 *
	 * @return The declared variable count.
	 */
	public int variableCount() {
		return variableNames.size();
	}

	/**
	 * The variables declared by this template, in declaration order.
	 *
	 * <p>
	 * Populated regardless of {@link #isReverseMatchable()}, so variables of a valid-but-not-reverse-matchable
	 * template remain available for exact-template-string completion lookups.
	 *
	 * @return An immutable, insertion-ordered list of declared variable names. Never <jk>null</jk>.
	 */
	public List<String> variableNames() {
		return variableNames;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Matching
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Reverse-matches a concrete URI against this template.
	 *
	 * @param uri The concrete URI to match. Can be <jk>null</jk>, in which case this method returns <jk>null</jk>.
	 * @return An immutable, insertion-ordered, UTF-8-decoded map of variable name to decoded value, in
	 * 	declaration order, or <jk>null</jk> if this template is not {@link #isReverseMatchable() reverse-matchable},
	 * 	the URI does not match, the URI contains a malformed percent escape, or a captured value's decoded bytes
	 * 	are not valid UTF-8.
	 */
	@SuppressWarnings({
		"java:S1168" // null means "template does not match"; an empty map would mean "matched with zero variables" - collapsing the two would turn non-matches into false matches.
	})
	public Map<String,String> match(String uri) {
		if (! reverseMatchable || uri == null)
			return null;
		var normalized = normalizeForMatch(uri);
		if (normalized == null)
			return null;
		return matchPieces(normalized);
	}

	/**
	 * Left-to-right single-pass match against the compiled, alternating literal/capture piece list.
	 *
	 * <p>
	 * See the class Javadoc "Ambiguous-capture resolution" section for the boundary-resolution algorithm and its
	 * complexity guarantee.
	 */
	@SuppressWarnings({
		"java:S1168", // null means "template does not match"; an empty map would mean "matched with zero variables" - collapsing the two would turn non-matches into false matches.
		"java:S3776", // Intentional: hand-rolled RFC 6570 reverse matcher; control-flow verified by McpUriTemplateMatcher_Test, refactor would risk matching correctness.
		"java:S127", // The i++ skip of the paired literal piece after consuming a capture is intentional and documented by the class-level "Ambiguous-capture resolution" section.
		"java:S135" // Single continue plus the loop-counter skip above are the simplest expression of this piece-by-piece scan; splitting it out would risk matching correctness.
	})
	private Map<String,String> matchPieces(String normalized) {
		var captures = new LinkedHashMap<String,String>();
		var pos = 0;
		for (var i = 0; i < pieces.size(); i++) {
			var piece = pieces.get(i);
			if (piece instanceof LiteralPiece lit) {
				if (! normalized.startsWith(lit.text(), pos))
					return null;
				pos += lit.text().length();
				continue;
			}
			var capture = (CapturePiece) piece;
			var maxEnd = scanMaxEnd(normalized, pos, capture.stopChars());
			// Invariant established at compile time: a capture piece is always followed by either a literal
			// piece or the end of the template, never by another capture (see class Javadoc).
			var nextLiteral = (i + 1 < pieces.size() && pieces.get(i + 1) instanceof LiteralPiece lp) ? lp : null;
			String rawValue;
			if (nextLiteral == null) {
				rawValue = normalized.substring(pos, maxEnd);
				pos = maxEnd;
			} else {
				var literalText = nextLiteral.text();
				var searchFrom = Math.min(maxEnd, normalized.length() - literalText.length());
				var foundAt = searchFrom < pos ? -1 : normalized.lastIndexOf(literalText, searchFrom);
				if (foundAt < pos)
					return null;
				rawValue = normalized.substring(pos, foundAt);
				pos = foundAt + literalText.length();
				i++;
			}
			var decoded = decodeCapture(rawValue);
			if (decoded == null)
				return null;
			captures.put(capture.name(), decoded);
		}
		if (pos != normalized.length())
			return null;
		return Collections.unmodifiableMap(captures);
	}

	/**
	 * Scans forward from {@code pos} while the current character is not one of {@code stopChars}, returning the
	 * bound at which a capture governed by that stop set must end. An empty stop set (reserved/fragment captures)
	 * scans unconditionally to the end of the string.
	 *
	 * <p>
	 * Because {@code stopChars} only ever contains reserved delimiter characters (<c>/</c>, <c>.</c>, <c>&amp;</c>,
	 * <c>#</c>) - none of which are <c>%</c> or a hex digit - a normalized <c>%XX</c> triplet can never be
	 * mistaken for one of these characters, so a plain character scan over the already-normalized string
	 * correctly treats an encoded delimiter as data rather than a boundary.
	 */
	private static int scanMaxEnd(String normalized, int pos, Set<Character> stopChars) {
		if (stopChars.isEmpty())
			return normalized.length();
		var i = pos;
		while (i < normalized.length() && ! stopChars.contains(normalized.charAt(i)))
			i++;
		return i;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Tokenizing: split the raw template into literal and expression segments
	//-----------------------------------------------------------------------------------------------------------------

	private sealed interface RawToken permits RawLiteral, RawExpr {}

	private record RawLiteral(String text) implements RawToken {}

	private record RawExpr(String body, int index) implements RawToken {}

	@SuppressWarnings({
		"java:S3776" // Intentional: hand-rolled RFC 6570 reverse matcher; control-flow verified by McpUriTemplateMatcher_Test, refactor would risk matching correctness.
	})
	private static List<RawToken> tokenize(String template) {
		var out = new ArrayList<RawToken>();
		var n = template.length();
		var i = 0;
		var lit = new StringBuilder();
		while (i < n) {
			var c = template.charAt(i);
			if (c == '{') {
				if (! lit.isEmpty()) {
					out.add(new RawLiteral(lit.toString()));
					lit.setLength(0);
				}
				var close = template.indexOf('}', i + 1);
				if (close < 0)
					throw iaex("Malformed URI template ''%s'': unterminated '{' at index %s", template, i);
				var body = template.substring(i + 1, close);
				if (body.indexOf('{') >= 0)
					throw iaex("Malformed URI template ''%s'': nested '{' inside expression at index %s", template, i);
				if (body.isEmpty())
					throw iaex("Malformed URI template ''%s'': empty expression '{}' at index %s", template, i);
				out.add(new RawExpr(body, i));
				i = close + 1;
			} else if (c == '}') {
				throw iaex("Malformed URI template ''%s'': unmatched '}' at index %s", template, i);
			} else {
				lit.append(c);
				i++;
			}
		}
		if (! lit.isEmpty())
			out.add(new RawLiteral(lit.toString()));
		return out;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Expression grammar: operator + comma-separated varspec list
	//-----------------------------------------------------------------------------------------------------------------

	private enum Modifier { NONE, PREFIX, EXPLODE }

	private record VarSpec(String name, Modifier modifier) {}

	private record ParsedExpr(char operator, List<VarSpec> vars) {}

	private static final Set<Character> OPERATORS = Set.of('+', '#', '.', '/', ';', '?', '&');
	private static final Set<Character> RESERVED_FOR_FUTURE_OPERATORS = Set.of('!', '=', ',', '|', '@');

	@SuppressWarnings({
		"java:S5998" // Bounded by RFC 6570 varname grammar (dot-separated [A-Za-z0-9_]+ segments); real inputs are short template strings, not attacker-controlled bulk text.
	})
	private static final Pattern VARNAME = Pattern.compile("\\w+(?:\\.\\w+)*");

	private static ParsedExpr parseExpr(String body, String template, int index) {
		var first = body.charAt(0);
		char operator;
		String rest;
		if (OPERATORS.contains(first)) {
			operator = first;
			rest = body.substring(1);
		} else if (RESERVED_FOR_FUTURE_OPERATORS.contains(first)) {
			throw iaex("Malformed URI template ''%s'': unsupported operator ''%s'' in expression at index %s",
				template, first, index);
		} else {
			operator = '\0';
			rest = body;
		}
		if (rest.isEmpty())
			throw iaex("Malformed URI template ''%s'': empty variable list in expression at index %s", template, index);
		var vars = new ArrayList<VarSpec>();
		for (var part : rest.split(",", -1))
			vars.add(parseVarSpec(part, template, index));
		return new ParsedExpr(operator, vars);
	}

	private static VarSpec parseVarSpec(String spec, String template, int index) {
		if (spec.isEmpty())
			throw iaex("Malformed URI template ''%s'': empty variable name in expression at index %s", template, index);
		if (spec.endsWith("*")) {
			var name = spec.substring(0, spec.length() - 1);
			validateVarName(name, template, index);
			return new VarSpec(name, Modifier.EXPLODE);
		}
		var colon = spec.indexOf(':');
		if (colon >= 0) {
			var name = spec.substring(0, colon);
			var digits = spec.substring(colon + 1);
			validateVarName(name, template, index);
			if (digits.isEmpty() || digits.length() > 4 || ! digits.chars().allMatch(Character::isDigit))
				throw iaex("Malformed URI template ''%s'': invalid prefix-length modifier ''%s'' in expression at index %s",
					template, spec, index);
			return new VarSpec(name, Modifier.PREFIX);
		}
		validateVarName(spec, template, index);
		return new VarSpec(spec, Modifier.NONE);
	}

	private static void validateVarName(String name, String template, int index) {
		if (! VARNAME.matcher(name).matches())
			throw iaex("Malformed URI template ''%s'': invalid variable name ''%s'' in expression at index %s",
				template, name, index);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Matchability classification and desugaring into literal/capture pieces
	//-----------------------------------------------------------------------------------------------------------------

	private sealed interface Piece permits LiteralPiece, CapturePiece {}

	private record LiteralPiece(String text) implements Piece {}

	private record CapturePiece(String name, Set<Character> stopChars) implements Piece {}

	private static final Set<Character> STOP_SLASH = Set.of('/');
	private static final Set<Character> STOP_SLASH_DOT = Set.of('/', '.');
	private static final Set<Character> STOP_QUERY = Set.of('&', '#');
	private static final Set<Character> STOP_NONE = Set.of();

	/**
	 * Determines whether a single parsed expression is one of the settled reversible forms, independent of its
	 * neighbors (adjacency ambiguity is a whole-template concern handled by the caller).
	 */
	private static boolean isExprReverseMatchable(ParsedExpr expr) {
		if (expr.vars().stream().anyMatch(v -> v.modifier() != Modifier.NONE))
			return false;
		return switch (expr.operator()) {
			case '\0', '+', '#' -> expr.vars().size() == 1;
			case '/', '.', '?', '&' -> true;
			default -> false;
		};
	}

	/**
	 * Expands a reverse-matchable expression into its literal/capture piece sequence. Every branch here ends
	 * with a {@link CapturePiece}, which is what guarantees the "capture is always followed by a literal or the
	 * end of the template" invariant documented on the class and relied on by {@link #matchPieces(String)}.
	 */
	private static List<Piece> desugar(ParsedExpr expr) {
		var out = new ArrayList<Piece>();
		switch (expr.operator()) {
			case '\0' -> out.add(new CapturePiece(expr.vars().get(0).name(), STOP_SLASH));
			case '+' -> out.add(new CapturePiece(expr.vars().get(0).name(), STOP_NONE));
			case '#' -> {
				out.add(new LiteralPiece("#"));
				out.add(new CapturePiece(expr.vars().get(0).name(), STOP_NONE));
			}
			case '/' -> {
				for (var v : expr.vars()) {
					out.add(new LiteralPiece("/"));
					out.add(new CapturePiece(v.name(), STOP_SLASH));
				}
			}
			case '.' -> {
				for (var v : expr.vars()) {
					out.add(new LiteralPiece("."));
					out.add(new CapturePiece(v.name(), STOP_SLASH_DOT));
				}
			}
			case '?' -> {
				var first = true;
				for (var v : expr.vars()) {
					out.add(new LiteralPiece((first ? "?" : "&") + v.name() + "="));
					out.add(new CapturePiece(v.name(), STOP_QUERY));
					first = false;
				}
			}
			case '&' -> {
				for (var v : expr.vars()) {
					out.add(new LiteralPiece("&" + v.name() + "="));
					out.add(new CapturePiece(v.name(), STOP_QUERY));
				}
			}
			default -> { /* matrix and any other non-matchable operator: never reached, never used for matching */ }
		}
		return out;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RFC 3986 percent-encoding normalization (implemented fresh; see class Javadoc)
	//-----------------------------------------------------------------------------------------------------------------

	private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

	private static boolean isUnreserved(int octet) {
		return (octet >= 'A' && octet <= 'Z') || (octet >= 'a' && octet <= 'z') || (octet >= '0' && octet <= '9')
			|| octet == '-' || octet == '.' || octet == '_' || octet == '~';
	}

	private static int hexValue(char c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;
		return -1;
	}

	/**
	 * Normalizes a template literal segment at compile time: uppercases valid percent triplets, decodes
	 * percent-encoded unreserved octets to their literal character, and leaves percent-encoded reserved/other
	 * octets encoded. Also validates that the segment's percent-escaped bytes, combined with its raw characters,
	 * form valid UTF-8. Throws on any malformed escape or invalid UTF-8, naming the offending template.
	 */
	private static String normalizeLiteralStrict(String raw, String template) {
		var sb = new StringBuilder();
		var bytes = new ByteArrayOutputStream();
		var n = raw.length();
		var i = 0;
		while (i < n) {
			var c = raw.charAt(i);
			if (c == '%') {
				if (i + 2 >= n || hexValue(raw.charAt(i + 1)) < 0 || hexValue(raw.charAt(i + 2)) < 0)
					throw iaex("Malformed URI template ''%s'': malformed percent-escape at index %s", template, i);
				var octet = (hexValue(raw.charAt(i + 1)) << 4) | hexValue(raw.charAt(i + 2));
				bytes.write(octet);
				appendNormalizedOctet(sb, octet);
				i += 3;
			} else {
				bytes.writeBytes(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
				sb.append(c);
				i++;
			}
		}
		if (! isValidUtf8(bytes.toByteArray()))
			throw iaex("Malformed URI template ''%s'': invalid UTF-8 percent-escape sequence in literal text", template);
		return sb.toString();
	}

	/**
	 * Normalizes an entire concrete URI at match time, using the same rules as
	 * {@link #normalizeLiteralStrict(String, String)} but returning <jk>null</jk> instead of throwing when a
	 * percent escape is malformed, per the rule that a concrete URI with a malformed escape simply does not
	 * template-match.
	 */
	private static String normalizeForMatch(String raw) {
		var sb = new StringBuilder();
		var n = raw.length();
		var i = 0;
		while (i < n) {
			var c = raw.charAt(i);
			if (c == '%') {
				if (i + 2 >= n)
					return null;
				var h1 = hexValue(raw.charAt(i + 1));
				var h2 = hexValue(raw.charAt(i + 2));
				if (h1 < 0 || h2 < 0)
					return null;
				appendNormalizedOctet(sb, (h1 << 4) | h2);
				i += 3;
			} else {
				sb.append(c);
				i++;
			}
		}
		return sb.toString();
	}

	private static void appendNormalizedOctet(StringBuilder sb, int octet) {
		if (isUnreserved(octet))
			sb.append((char) octet);
		else
			sb.append('%').append(HEX_DIGITS[(octet >> 4) & 0xF]).append(HEX_DIGITS[octet & 0xF]);
	}

	/**
	 * Counts normalized literal octets in an already-normalized string: each <c>%XX</c> triplet counts as one
	 * octet, and every other character counts as one octet.
	 */
	private static int countOctets(String normalized) {
		var count = 0;
		var i = 0;
		var n = normalized.length();
		while (i < n) {
			i += normalized.charAt(i) == '%' ? 3 : 1;
			count++;
		}
		return count;
	}

	/**
	 * Percent-decodes an already-normalized capture substring to its final UTF-8 value for placement in the
	 * result map, rejecting (returning <jk>null</jk> for) captures whose decoded bytes are not valid UTF-8.
	 */
	private static String decodeCapture(String normalizedSubstring) {
		var bytes = new ByteArrayOutputStream();
		var n = normalizedSubstring.length();
		var i = 0;
		while (i < n) {
			var c = normalizedSubstring.charAt(i);
			if (c == '%') {
				bytes.write((hexValue(normalizedSubstring.charAt(i + 1)) << 4) | hexValue(normalizedSubstring.charAt(i + 2)));
				i += 3;
			} else {
				bytes.writeBytes(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
				i++;
			}
		}
		var data = bytes.toByteArray();
		if (! isValidUtf8(data))
			return null;
		return new String(data, StandardCharsets.UTF_8);
	}

	private static boolean isValidUtf8(byte[] data) {
		try {
			StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.decode(ByteBuffer.wrap(data));
			return true;
		} catch (CharacterCodingException e) {
			return false;
		}
	}
}
