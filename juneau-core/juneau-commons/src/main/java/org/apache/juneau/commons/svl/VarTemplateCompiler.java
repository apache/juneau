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
package org.apache.juneau.commons.svl;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;

import org.apache.juneau.commons.lang.AsciiSet;

/**
 * Single tokenizer + recursive-descent parser for SVL templates.
 *
 * <p>
 * One pass over the input string produces an array of {@link TemplateSegment} objects:
 * {@link LiteralSegment} for plain text, {@link VarRefSegment} for {@code ${...}} /
 * {@code $X{...}} markers, and {@link ScriptSegment} for {@code #{name(args...)}} markers. The
 * compiler is the only tokenizer in the codebase — both
 * {@link VarResolverSession#resolve(String)} and {@link VarResolverSession#resolveTo(String, java.io.Writer)}
 * dispatch through {@link VarResolver#compile(String)}.
 *
 * <h5 class='section'>Tokenizer behavior (ported verbatim from the legacy in-place tokenizer):</h5>
 * <ul>
 * 	<li><b>Top-level escape:</b> {@code \\}, {@code \$}, {@code \#} emit just the trailing char.
 * 		Other {@code \X} sequences emit {@code \X} verbatim (matches existing
 * 		{@link VarResolverSession#resolveTo(String, java.io.Writer)} semantics for {@code \\}/{@code \$};
 * 		{@code \#} is added new for symmetry with {@code \$}).
 * 	<li><b>{@code ${...}} / {@code $X{...}} parse:</b> after seeing {@code $}, an optional
 * 		ASCII-letter prefix is captured up to the {@code {}}; bodies are collected with brace
 * 		depth tracking; inner {@code \\} escapes inside the body are recognized for
 * 		state-machine purposes (don't close on {@code \}}) but the captured body string preserves
 * 		backslashes verbatim. Unknown prefix → fallthrough emits the original substring with
 * 		{@code \\\$\{\}} unescaped per the legacy {@code AS2} set.
 * 	<li><b>{@code #{...}} parse:</b> when {@code #} is followed immediately by {@code {}}, body
 * 		collection enters script mode (same depth/escape tracking as {@code ${...}} bodies). On
 * 		close, the body is parsed via the recursive-descent parser implemented below.
 * 	<li><b>{@code ${...}} shortcut:</b> empty prefix triggers the
 * 		{@link VarResolverSession DOLLAR_BRACE_VAR} fallback, with the first top-level {@code ':'}
 * 		in the body rewritten to {@code ','} <i>at compile time</i> on the body source string.
 * 	<li><b>End-of-input:</b> dangling {@code $} (state S2), open {@code $X{}} (state S3), or open
 * 		{@code #{}} (state S3H) preserve the legacy fallthrough — the relevant prefix + body
 * 		fragment is appended verbatim (with the legacy {@code AS1}/{@code AS2} unescaping) so a
 * 		malformed input round-trips identically through both legacy and compiled paths.
 * </ul>
 *
 * <h5 class='section'>Recursive-descent script-arg parser:</h5>
 * <p>
 * Inside a {@code #{...}} body, the parser splits {@code name(arg, arg, ...)} into the function
 * name and a list of {@link VarTemplate}-typed arguments. Each argument is one of:
 * <ul>
 * 	<li>Quoted string — {@code "..."} or {@code '...'}, with {@code \"} / {@code \'} / {@code \\}
 * 		escapes.
 * 	<li>Bare identifier — letters, digits, underscores, {@code -}, {@code .}; treated as a
 * 		literal string (e.g. {@code switch}-case labels).
 * 	<li>Numeric literal — integer or decimal; treated as a literal string.
 * 	<li>Boolean literal — {@code true} / {@code false}; treated as a literal string.
 * 	<li>Nested marker — {@code ${...}}, {@code $X{...}}, or {@code #{...}}; compiled to a
 * 		nested {@link VarTemplate}.
 * </ul>
 *
 * <p>
 * Whitespace between arg tokens (and around commas) is tolerated and discarded.
 *
 * <h5 class='section'>Function binding:</h5>
 * <p>
 * {@link VarFunction} references are resolved at compile time against the supplied
 * {@link VarResolver}'s function map. Unknown functions are left with a {@code null}
 * {@link ScriptSegment#cachedFn cachedFn}, deferring the {@code "No such function"} failure to
 * resolve time (lazy failure preserves backward compatibility for compile-once / resolve-many
 * flows where an unrelated branch references a missing function).
 *
 * <h5 class='section'>Stable-value folding:</h5>
 * <p>
 * If a captured {@link Var} opts in via {@link Var#isStable()} and its body folded to a literal
 * (no nested live markers), the var is invoked once at compile time and replaced with the
 * resolved literal. If {@link Var#canResolve(VarResolverSession)} returns {@code false} or
 * {@link Var#doResolve(VarResolverSession, String)} throws, folding is skipped and the dispatch
 * segment is emitted unchanged.
 */
@SuppressWarnings({
	"java:S125", // State-machine narration in comments
	"java:S2583", // Condition always true/false; state persists across iterations
	"java:S6541", // Brain-method threshold; intentional state-machine consolidation
	"java:S3776", // Cognitive complexity acceptable for this state-machine + parser
	"java:S110", // Inheritance depth: the segment hierarchy is intentionally shallow
})
final class VarTemplateCompiler {

	private static final AsciiSet AS1 = AsciiSet.of("\\{");
	private static final AsciiSet AS2 = AsciiSet.of("\\${}");

	/** Var name targeted by the {@code ${xxx}} shortcut. Mirrors {@code VarResolverSession.DOLLAR_BRACE_VAR}. */
	private static final String DOLLAR_BRACE_VAR = "P";

	private static final int S1 = 1;  // Outside any var/script
	private static final int S2 = 2;  // Saw '$', looking for '{'
	private static final int S3 = 3;  // Inside ${...} / $X{...} body, looking for matching '}'
	private static final int S3H = 4; // Inside #{...} body, looking for matching '}'

	private final VarResolver resolver;

	private VarTemplateCompiler(VarResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * Compiles the given input string against the supplied resolver.
	 *
	 * @param resolver The resolver whose var registry is used for compile-time {@link Var}
	 *	 binding. Must not be {@code null}.
	 * @param input The template source. May be {@code null}.
	 * @return The compiled template. Never {@code null}.
	 */
	static VarTemplate compile(VarResolver resolver, String input) {
		if (input == null)
			return new VarTemplate(resolver, null, new TemplateSegment[0]);
		if (input.isEmpty())
			return new VarTemplate(resolver, input, new TemplateSegment[0]);
		// Fast path: no special chars at all.
		if (input.indexOf('$') == -1 && input.indexOf('\\') == -1 && input.indexOf('#') == -1)
			return new VarTemplate(resolver, input, new TemplateSegment[]{new LiteralSegment(input)});
		return new VarTemplateCompiler(resolver).compileImpl(input);
	}

	/**
	 * Compiles {@code input} into segments. Mirrors the legacy
	 * {@link VarResolverSession#resolveTo(String, java.io.Writer)} state machine, but writes
	 * to a segment list instead of a {@link java.io.Writer}.
	 */
	private VarTemplate compileImpl(String input) {
		var segments = new ArrayList<TemplateSegment>();
		var literal = new StringBuilder();

		var state = S1;
		var isInEscape = false;
		var hasInternalVar = false;
		var hasInnerEscapes = false;
		String varType = null;
		var x = 0;        // index of opening '{' of current body (state S3/S3H)
		var x2 = 0;       // index of opening '$' or '#' of current var/script
		var depth = 0;
		var isDollarBraceShortcut = false;
		var length = input.length();

		for (var i = 0; i < length; i++) {
			var c = input.charAt(i);
			if (state == S1) {
				if (isInEscape) {
					if (c == '\\' || c == '$' || c == '#') {
						literal.append(c);
					} else {
						literal.append('\\').append(c);
					}
					isInEscape = false;
				} else if (c == '\\') {
					isInEscape = true;
				} else if (c == '$') {
					x = i;
					x2 = i;
					state = S2;
				} else if (c == '#' && i + 1 < length && input.charAt(i + 1) == '{') {
					// Script trigger: '#{' begins a script segment. Flush pending literal,
					// advance past the '{', and enter S3H to collect the body.
					flushLiteral(segments, literal);
					x2 = i;
					i++;            // consume '{'
					x = i;          // body start (the '{' position; body is substring(x+1, i))
					depth = 0;
					state = S3H;
				} else {
					literal.append(c);
				}
			} else if (state == S2) {
				if (isInEscape) {
					isInEscape = false;
				} else if (c == '\\') {
					hasInnerEscapes = true;
					isInEscape = true;
				} else if (c == '{') {
					varType = input.substring(x + 1, i);
					if (varType.isEmpty())
						isDollarBraceShortcut = true;
					x = i;
					depth = 0;
					state = S3;
				} else if (c < 'A' || c > 'z' || (c > 'Z' && c < 'a')) {  // False trigger "$X "
					if (hasInnerEscapes)
						literal.append(unescapeChars(input.substring(x, i + 1), AS1));
					else
						literal.append(input, x, i + 1);
					state = S1;
					hasInnerEscapes = false;
				}
			} else if (state == S3) {
				if (isInEscape) {
					isInEscape = false;
				} else if (c == '\\') {
					isInEscape = true;
					hasInnerEscapes = true;
				} else if (c == '{') {
					depth++;
					hasInternalVar = true;
				} else if (c == '}') {
					if (depth > 0) {
						depth--;
					} else {
						// Body collected. Flush pending literal first, then build the var ref.
						flushLiteral(segments, literal);
						var bodySource = input.substring(x + 1, i);
						if (isDollarBraceShortcut)
							bodySource = translateDollarBraceDefault(bodySource);

						// Look up the cached Var: prefer explicit empty-name reg if any, else
						// fall back to PropertyVar for the shortcut.
						var v = resolver.getVarMap().get(varType);
						if (v == null && isDollarBraceShortcut)
							v = resolver.getVarMap().get(DOLLAR_BRACE_VAR);

						// Pre-compute fallthrough text per legacy AS2 unescaping rules.
						String fallthrough;
						if (hasInnerEscapes)
							fallthrough = unescapeChars(input.substring(x2, i + 1), AS2);
						else
							fallthrough = input.substring(x2, i + 1);

						// Recursively compile the body so nested markers compose naturally.
						var body = compile(resolver, bodySource);

						// Stable-value folding: if the captured Var opted in via Var.isStable()
						// AND the body itself folded to a literal (no nested live markers), we
						// can resolve the var once at compile time and replace the dispatch
						// segment with the resolved literal text. Match the runtime "var must
						// canResolve" behavior — fold only if the var canResolve a no-bean
						// session (the compiler has no session of its own), and if doResolve
						// throws, treat as "cannot fold" and fall back to the dispatch segment.
						var folded = tryFoldStableVar(v, body, hasInternalVar);
						if (folded != null) {
							segments.add(new LiteralSegment(folded));
						} else {
							segments.add(new VarRefSegment(v, varType, body, hasInternalVar, fallthrough));
						}

						state = S1;
						hasInnerEscapes = false;
						hasInternalVar = false;
						isDollarBraceShortcut = false;
					}
				}
			} else if (state == S3H) {
				if (isInEscape) {
					isInEscape = false;
				} else if (c == '\\') {
					isInEscape = true;
					hasInnerEscapes = true;
				} else if (c == '{') {
					depth++;
				} else if (c == '}') {
					if (depth > 0) {
						depth--;
					} else {
						// Script body collected; parse via recursive-descent.
						var bodySource = input.substring(x + 1, i);
						var seg = parseScriptBody(input, bodySource, x2, i);
						segments.add(seg);
						state = S1;
						hasInnerEscapes = false;
					}
				}
			}
		}

		// End-of-input handling — preserve legacy fallthrough exactly.
		if (isInEscape) {
			literal.append('\\');
		} else if (state == S2) {
			literal.append('$').append(unescapeChars(input.substring(x + 1), AS1));
		} else if (state == S3) {
			literal.append('$').append(varType).append('{').append(unescapeChars(input.substring(x + 1), AS2));
		} else if (state == S3H) {
			literal.append("#{").append(unescapeChars(input.substring(x + 1), AS2));
		}
		flushLiteral(segments, literal);

		return new VarTemplate(resolver, input, segments.toArray(new TemplateSegment[0]));
	}

	private static void flushLiteral(List<TemplateSegment> out, StringBuilder buf) {
		if (buf.length() > 0) {
			out.add(new LiteralSegment(buf.toString()));
			buf.setLength(0);
		}
	}

	/**
	 * Translates the first top-level {@code ':'} in a {@code ${...}}-shortcut body to {@code ','}
	 * so that the body matches {@link DefaultingVar}'s {@code key,default} separator.
	 *
	 * <p>
	 * Mirrors {@code VarResolverSession.translateDollarBraceDefault(...)} — applied on the body
	 * source string at compile time so any {@code ':'} produced by an inner var resolution at
	 * runtime is NOT re-interpreted as a default separator.
	 */
	private static String translateDollarBraceDefault(String body) {
		var depth = 0;
		var length = body.length();
		for (var i = 0; i < length; i++) {
			var c = body.charAt(i);
			if (c == '{')
				depth++;
			else if (c == '}' && depth > 0)
				depth--;
			else if (c == ':' && depth == 0)
				return body.substring(0, i) + ',' + body.substring(i + 1);
		}
		return body;
	}

	// ============================================================================
	// Recursive-descent parser for #{...} script bodies
	// ============================================================================

	/**
	 * Parses a captured script body string into a {@link ScriptSegment}.
	 *
	 * @param fullInput The full template input (for diagnostic context).
	 * @param body The captured body — everything between {@code #{}} and the matching {@code }}.
	 * @param x2 Index of the opening {@code #} in {@code fullInput}.
	 * @param closeIndex Index of the matching {@code }} in {@code fullInput}.
	 */
	private ScriptSegment parseScriptBody(String fullInput, String body, int x2, int closeIndex) {
		var p = new ScriptParser(body, fullInput, x2, closeIndex);
		var name = p.parseFunctionName();
		p.expect('(');
		var args = p.parseArgList();
		p.expect(')');
		p.expectEnd();
		// Bind the function at compile time. Unknown functions stay null and fail
		// lazily at resolve time.
		var fn = resolver.getFunctionMap().get(name);
		return new ScriptSegment(fn, name, args.toArray(new VarTemplate[0]));
	}

	/**
	 * Attempt compile-time stable-value folding for a {@code ${...}} / {@code $X{...}} dispatch.
	 *
	 * <p>
	 * Folding is only valid when:
	 * <ul>
	 * 	<li>{@code v} is non-{@code null}.
	 * 	<li>{@code v.isStable()} returns {@code true}.
	 * 	<li>The compiled body is a pure literal — no nested live markers, including the legacy
	 * 		{@code hasInternalVar} flag set by the state machine on inner {@code {}} characters.
	 * 	<li>The var is a {@link SimpleVar} (streamed vars are excluded — folding a stream loses
	 * 		the writer-direct optimization for no compile-time gain).
	 * 	<li>{@link Var#canResolve(VarResolverSession)} on a fresh no-bean session returns
	 * 		{@code true}, and {@link Var#doResolve(VarResolverSession, String)} returns a
	 * 		non-{@code null} value without throwing.
	 * </ul>
	 *
	 * <p>
	 * Returns the folded literal text, or {@code null} if folding is skipped for any reason.
	 */
	@SuppressWarnings({
		"java:S1166" // Exception swallowed intentionally — fold-or-skip contract.
	})
	private String tryFoldStableVar(Var v, VarTemplate body, boolean hasInternalVar) {
		if (v == null || !v.isStable() || hasInternalVar || !(v instanceof SimpleVar))
			return null;
		if (!body.isLiteral())
			return null;
		try {
			var session = resolver.createSession();
			if (!v.canResolve(session))
				return null;
			// body.isLiteral() guaranteed above; resolve(session) short-circuits to literalText
			// and never touches session beans.
			var resolved = v.doResolve(session, body.resolve(session));
			if (resolved == null)
				return null;
			// Skip folding if the resolved value would itself need recursive resolution. The
			// runtime VarRefSegment.resolve(...) path post-resolves dispatch results that
			// contain ${...} / $X{...} markers when v.allowRecurse() returns true; folding
			// would silently drop that recursion. (Compare: VarRefSegment.resolve, which
			// re-invokes session.resolve(replacement) when the replacement contains '$' and
			// the var allows recursion.)
			if (v.allowRecurse() && resolved.indexOf('$') != -1)
				return null;
			return resolved;
		} catch (@SuppressWarnings("unused") Exception e) {
			return null;
		}
	}

	/**
	 * Hand-written recursive-descent parser for {@code name(arg, arg, ...)} script bodies.
	 *
	 * <p>
	 * The parser operates on the body string only — anything outside the {@code #{...}}
	 * delimiters has already been handled by the outer state machine. Each parsed argument is
	 * compiled into a {@link VarTemplate} via {@link VarTemplateCompiler#compile(VarResolver, String)}
	 * so nested markers and stable-value folding behave consistently with top-level template
	 * compilation.
	 */
	private final class ScriptParser {

		private final String body;
		private final String fullInput;
		private final int x2;          // for error messages
		private final int closeIndex;  // for error messages
		private int pos;

		ScriptParser(String body, String fullInput, int x2, int closeIndex) {
			this.body = body;
			this.fullInput = fullInput;
			this.x2 = x2;
			this.closeIndex = closeIndex;
		}

		String parseFunctionName() {
			skipWs();
			var start = pos;
			while (pos < body.length()) {
				var c = body.charAt(pos);
				if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
					pos++;
				} else {
					break;
				}
			}
			if (pos == start)
				throw parseError("Expected function name");
			var name = body.substring(start, pos);
			if (!Character.isLetter(name.charAt(0)))
				throw parseError("Function name must start with a letter (got ''{0}'')", name);
			skipWs();
			return name;
		}

		List<VarTemplate> parseArgList() {
			var args = new ArrayList<VarTemplate>();
			skipWs();
			if (peek() == ')')
				return args;
			args.add(parseArg());
			skipWs();
			while (peek() == ',') {
				pos++;
				skipWs();
				args.add(parseArg());
				skipWs();
			}
			return args;
		}

		/**
		 * Parse a single argument. Returns a {@link VarTemplate} compiled from the captured
		 * argument source.
		 */
		VarTemplate parseArg() {
			skipWs();
			if (pos >= body.length())
				throw parseError("Unexpected end of script body");
			var c = body.charAt(pos);
			if (c == '"' || c == '\'')
				return parseQuotedString(c);
			if (c == '$' || c == '#')
				return parseNestedMarker();
			// Bare token: identifier / number / boolean — captured verbatim until comma /
			// close-paren / whitespace. Whitespace inside a bare token is invalid per the
			// recursive-descent grammar; the parser stops at the first whitespace and the next
			// expected token handles the remainder.
			return parseBareToken();
		}

		/**
		 * Parse a quoted string, returning a {@link VarTemplate} whose source is the unquoted,
		 * escape-interpreted contents. The result is a literal-only template.
		 */
		VarTemplate parseQuotedString(char quote) {
			pos++; // skip opening quote
			var sb = new StringBuilder();
			while (pos < body.length()) {
				var c = body.charAt(pos);
				if (c == '\\' && pos + 1 < body.length()) {
					var next = body.charAt(pos + 1);
					if (next == quote || next == '\\') {
						sb.append(next);
						pos += 2;
						continue;
					}
					sb.append(c);
					pos++;
				} else if (c == quote) {
					pos++; // consume closing quote
					return compile(resolver, sb.toString());
				} else {
					sb.append(c);
					pos++;
				}
			}
			throw parseError("Unterminated quoted string in script body");
		}

		/**
		 * Parse a nested {@code ${...}}, {@code $X{...}}, or {@code #{...}} marker by capturing
		 * the substring up to the matching close brace and recursively compiling it.
		 */
		VarTemplate parseNestedMarker() {
			var start = pos;
			var first = body.charAt(pos);
			pos++;
			// Consume the prefix (zero or more letters for $X{, none for #{).
			if (first == '$') {
				while (pos < body.length()) {
					var c = body.charAt(pos);
					if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
						pos++;
					} else {
						break;
					}
				}
			}
			if (pos >= body.length() || body.charAt(pos) != '{')
				throw parseError("Expected ''{'' after ''{0}'' in nested marker", first);
			pos++; // consume '{'
			var depth = 1;
			var inEscape = false;
			while (pos < body.length()) {
				var c = body.charAt(pos);
				if (inEscape) {
					inEscape = false;
				} else if (c == '\\') {
					inEscape = true;
				} else if (c == '{') {
					depth++;
				} else if (c == '}') {
					depth--;
					if (depth == 0) {
						pos++;
						return compile(resolver, body.substring(start, pos));
					}
				}
				pos++;
			}
			throw parseError("Unterminated nested marker starting at index {0}", start);
		}

		/**
		 * Parse a bare identifier / numeric / boolean token. Captured verbatim until comma,
		 * close-paren, or whitespace. The captured substring is wrapped in a literal-only
		 * {@link VarTemplate}.
		 */
		VarTemplate parseBareToken() {
			var start = pos;
			while (pos < body.length()) {
				var c = body.charAt(pos);
				if (c == ',' || c == ')' || c == ' ' || c == '\t' || c == '\n' || c == '\r')
					break;
				pos++;
			}
			if (pos == start)
				throw parseError("Expected argument");
			return compile(resolver, body.substring(start, pos));
		}

		void expect(char c) {
			skipWs();
			if (pos >= body.length() || body.charAt(pos) != c)
				throw parseError("Expected ''{0}''", c);
			pos++;
		}

		void expectEnd() {
			skipWs();
			if (pos < body.length())
				throw parseError("Unexpected trailing content ''{0}''", body.substring(pos));
		}

		char peek() {
			skipWs();
			return pos < body.length() ? body.charAt(pos) : '\0';
		}

		void skipWs() {
			while (pos < body.length()) {
				var c = body.charAt(pos);
				if (c == ' ' || c == '\t' || c == '\n' || c == '\r')
					pos++;
				else
					break;
			}
		}

		IllegalArgumentException parseError(String fmt, Object... args) {
			var message = "Invalid script in template: " + fmt + " at script body offset {" + pos
				+ "}; full script: \"" + sourceFragment() + "\"";
			return illegalArg(message, args);
		}

		String sourceFragment() {
			var safeEnd = Math.min(closeIndex + 1, fullInput.length());
			return fullInput.substring(x2, safeEnd);
		}
	}
}
