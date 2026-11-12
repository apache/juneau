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

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.lang.*;

/**
 * Single-pass tokenizer for SVL templates.
 *
 * <p>
 * One pass over the input string produces an array of {@link TemplateSegment} objects:
 * {@link LiteralSegment} for plain text, {@link VarRefSegment} for {@code ${...}} markers, and
 * {@link ExprSegment} for {@code #{expr}} markers. The compiler is the only tokenizer in the codebase — both
 * {@link VarResolverSession#resolve(String)} and {@link VarResolverSession#resolveTo(String, java.io.Writer)}
 * dispatch through {@link VarResolver#compile(String)}.
 *
 * <h5 class='section'>Grammar:</h5>
 * <ul>
 * 	<li><b>Top-level escape:</b> {@code \\}, {@code \$}, {@code \#} emit just the trailing char.
 * 		Other {@code \X} sequences emit {@code \X} verbatim.
 * 	<li><b>Marker ends:</b> where a {@code ${...}} or {@code #{...}} ends, and where its top-level {@code :} separators
 * 		are, is decided by {@link VarSplit} alone (the expression lexer uses it too).  In short: a backslash escapes the
 * 		next char; inside {@code ${...}} a quote opens a quoted run only at the start of a part (so {@code it's} and
 * 		{@code don't} are plain text); inside {@code #{...}} quotes are SpEL strings ({@code ''} doubling, no backslash
 * 		escape).
 * 	<li><b>{@code ${Type:rest}}:</b> the body runs to the matching {@code }}.
 * 		If the text before the first top-level {@code :} (see {@link VarSplit}) is {@code [A-Za-z]+} and a var with
 * 		that name is registered, it is a lookup of that var. Otherwise the whole body is the empty-prefix
 * 		{@code P} ({@code PropertyVar}) shortcut, so {@code ${name}} / {@code ${name:default}} keep their meaning.
 * 	<li><b>Colon split per var class</b> ({@link #translate(Function, String)}): the body is translated to the
 * 		internal comma form the var classes already understand. {@link DefaultingVar}: the first top-level {@code :}
 * 		splits key from default, the rest is the default verbatim. {@link MultipartResolvingVar} /
 * 		{@link MultipartVar}: each top-level {@code :} separates one part. Any other var: the body is whole.
 * 		In a key or part, {@code \:} is a literal colon and a fully quoted {@code '...'} / {@code "..."} part is
 * 		unquoted. A key or part that contains a literal {@code ,} cannot be represented in the internal form:
 * 		strict mode throws, lenient mode keeps the marker as literal text.
 * 	<li><b>{@code #{expr}}:</b> the body runs to the matching {@code }} (the braces of an embedded {@code ${...}} or a
 * 		quoted {@code '}'} do not end it) and is parsed with
 * 		{@link ExprParser#parse(String, int)} using the resolver's {@code maxExprDepth}. Constant expressions fold
 * 		to a literal via {@link ExprSegment#fold(VarResolver)}. A parse error (including the empty expression)
 * 		throws in strict mode and keeps the marker as literal text in lenient mode.
 * 	<li><b>Legacy dollar-brace variable syntax</b> (removed in 10.0): strict mode throws a positioned
 * 		{@link VarResolverException}; lenient mode keeps the whole marker as literal text.
 * 	<li><b>Unterminated</b> {@code ${} / {@code #{}: strict mode throws a positioned error; lenient mode appends
 * 		the rest of the input as literal text.
 * 	<li><b>Nesting depth:</b> {@code ${...}} bodies nest at most {@code maxExprDepth} deep.  A deeper marker throws a
 * 		positioned error in strict mode and is kept as literal text in lenient mode.  ({@code ${...}} inside
 * 		{@code #{...}} is resolved lazily; {@link ExprEvaluator} caps that chain with the same limit.)
 * 	<li><b>Lenient literal text</b> (unterminated, parse error, unrepresentable key, legacy syntax, too deep) is the
 * 		marker source with {@code \\}, {@code \$}, {@code \{} and {@code \}} escapes removed, as the old compiler did.
 * </ul>
 *
 * <p>
 * Compile errors are {@link ExprException}s whose source is the template and whose position is the offset of the
 * offending marker (or, for an expression parse error, of the offending token).  Errors inside a nested
 * {@code ${...}} body are mapped back to the template exactly only when translating the body to its comma form kept
 * its length (only {@code :} became {@code ,}).  When it did not (an unquoted key, a removed prefix, a {@code \:}
 * escape), the error points at the start of the enclosing {@code ${} marker instead.
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
	"java:S127", // Scanners skip escaped characters by advancing the loop counter in the body
	"java:S3776" // Cognitive complexity acceptable for this tokenizer
})
final class VarTemplateCompiler {

	private static final AsciiSet AS2 = AsciiSet.of("\\${}");

	/** Var name targeted by the {@code ${xxx}} shortcut. Mirrors {@code VarResolverSession.DOLLAR_BRACE_VAR}. */
	private static final String DOLLAR_BRACE_VAR = "P";

	private static final String LEGACY_SYNTAX_MSG = "Legacy variable syntax $X{...} is no longer supported; use ${X:...}"; // svl-migrate:skip (deliberate legacy text)

	private final VarResolver resolver;

	/** Number of {@code ${...}} bodies enclosing the text being compiled. */
	private final int depth;

	/** References found so far, in source order. */
	private final List<TemplateReference> refs = new ArrayList<>();

	private VarTemplateCompiler(VarResolver resolver, int depth) {
		this.resolver = resolver;
		this.depth = depth;
	}

	/**
	 * Compiles the given input string against the supplied resolver.
	 *
	 * @param resolver The resolver whose var registry is used for compile-time {@link Var}
	 *	 binding. Must not be {@code null}.
	 * @param input The template source. May be {@code null}.
	 * @return The compiled template. Never {@code null}.
	 * @throws ExprException In strict mode, on a malformed marker or legacy syntax.
	 */
	static VarTemplate compile(VarResolver resolver, String input) {
		return compile(resolver, input, 0);
	}

	private static VarTemplate compile(VarResolver resolver, String input, int depth) {
		if (input == null)
			return new VarTemplate(resolver, null, new TemplateSegment[0]);
		if (input.isEmpty())
			return new VarTemplate(resolver, input, new TemplateSegment[0]);
		// Fast path: no special chars at all.
		if (input.indexOf('$') == -1 && input.indexOf('\\') == -1 && input.indexOf('#') == -1)
			return new VarTemplate(resolver, input, new TemplateSegment[]{new LiteralSegment(input)});
		return new VarTemplateCompiler(resolver, depth).compileImpl(input);
	}

	private VarTemplate compileImpl(String input) {
		var segments = new ArrayList<TemplateSegment>();
		var literal = new StringBuilder();
		var n = input.length();
		var i = 0;
		while (i < n) {
			var c = input.charAt(i);
			if (c == '\\') {
				if (i + 1 < n) {
					var d = input.charAt(i + 1);
					if (d != '\\' && d != '$' && d != '#')
						literal.append('\\');
					literal.append(d);
					i += 2;
				} else {
					literal.append('\\');
					i++;
				}
			} else if (c == '$') {
				i = dollar(input, i, segments, literal);
			} else if (c == '#' && i + 1 < n && input.charAt(i + 1) == '{') {
				i = hash(input, i, segments, literal);
			} else {
				literal.append(c);
				i++;
			}
		}
		flushLiteral(segments, literal);
		return new VarTemplate(resolver, input, segments.toArray(new TemplateSegment[0]), List.copyOf(refs));
	}

	/** Handles a {@code $} at {@code start}; returns the index to continue scanning from. */
	private int dollar(String input, int start, List<TemplateSegment> segments, StringBuilder literal) {
		var n = input.length();
		var j = start + 1;
		while (j < n && isAsciiLetter(input.charAt(j)))
			j++;
		if (j >= n || input.charAt(j) != '{') {
			// False trigger ("$", "$X ", "$1"): literal text, rescan from the non-letter char.
			literal.append(input, start, j);
			return j;
		}
		var close = VarSplit.closeOfBody(input, j + 1);
		if (j > start + 1) {
			// Legacy dollar-brace variables.
			if (resolver.strict)
				throw new ExprException(LEGACY_SYNTAX_MSG, input, start);
			var end = close < 0 ? n : close + 1;
			syntax(LEGACY_SYNTAX_MSG, start, end);
			literal.append(unescapeChars(input.substring(start, end), AS2));
			return end;
		}
		if (close < 0) {
			if (resolver.strict)
				throw new ExprException("Unterminated ${", input, start);
			syntax("Unterminated ${", start, n);
			literal.append("${").append(unescapeChars(input.substring(j + 1), AS2));
			return n;
		}
		var inner = input.substring(j + 1, close);
		var fallthrough = unescapeChars(input.substring(start, close + 1), AS2);
		if (depth >= resolver.maxExprDepth) {
			if (resolver.strict)
				throw new ExprException("Variable nested too deeply (max " + resolver.maxExprDepth + ")", input, start);
			syntax("Variable nested too deeply (max " + resolver.maxExprDepth + ")", start, close + 1);
			literal.append(fallthrough);
			return close + 1;
		}
		var call = translate(resolver.getVarMap()::get, inner);
		if (call.error() != null) {
			if (resolver.strict)
				throw new ExprException(call.error(), input, start);
			syntax(call.error(), start, close + 1);
			literal.append(fallthrough);
			return close + 1;
		}
		var body = compileNested(call, inner, input, j + 1, start);
		var hasInternalVar = call.body().indexOf('{') != -1;
		var folded = tryFoldStableVar(call.varDef(), body, hasInternalVar);
		if (folded != null) {
			// A folded var is a value, not author text: keep it a separate segment so a value encoder still sees it.
			flushLiteral(segments, literal);
			segments.add(new LiteralSegment(folded, true));
		} else {
			refs.add(new TemplateReference(TemplateReference.Kind.VAR, call.prefix().isEmpty() ? DOLLAR_BRACE_VAR : call.prefix(), call.varDef() != null, null, start, close + 1));
			addKeyRef(call, inner, start + 2);
			flushLiteral(segments, literal);
			segments.add(new VarRefSegment(call.varDef(), call.prefix(), body, hasInternalVar, fallthrough));
		}
		return close + 1;
	}

	/** Handles a {@code #{} at {@code start}; returns the index to continue scanning from. */
	private int hash(String input, int start, List<TemplateSegment> segments, StringBuilder literal) {
		var n = input.length();
		var close = VarSplit.closeOfExpr(input, start + 2);
		if (close < 0) {
			if (resolver.strict)
				throw new ExprException("Unterminated #{", input, start);
			syntax("Unterminated #{", start, n);
			literal.append("#{").append(unescapeChars(input.substring(start + 2), AS2));
			return n;
		}
		var src = input.substring(start + 2, close);
		Expr ast;
		Expr unknown;
		ExprSegment seg;
		LiteralSegment folded;
		try {
			ast = ExprParser.parse(src, resolver.maxExprDepth);
			unknown = findUnknownCall(ast);
			seg = new ExprSegment(ast, src);
			folded = unknown == null ? seg.fold(resolver) : null;
		} catch (StackOverflowError e) {
			if (resolver.strict)
				throw new ExprException("expression too complex", input, start + 2, e);
			syntax("expression too complex", start, close + 1);
			literal.append(unescapeChars(input.substring(start, close + 1), AS2));
			return close + 1;
		} catch (ExprException e) {
			if (resolver.strict)
				throw new ExprException(e.getBaseMessage(), input, start + 2 + e.getPos(), e);
			syntax(e.getBaseMessage(), start, close + 1);
			literal.append(unescapeChars(input.substring(start, close + 1), AS2));
			return close + 1;
		}
		if (unknown != null) {
			if (resolver.strict)
				throw new ExprException(unavailableMessage(unknown), input, start + 2 + unknown.pos());
			collectRefs(ast, start + 2);
			literal.append(unescapeChars(input.substring(start, close + 1), AS2));
			return close + 1;
		}
		if (folded != null) {
			if (folded.value) {
				flushLiteral(segments, literal);
				segments.add(folded);
			} else
				literal.append(folded.text);
		} else {
			collectRefs(ast, start + 2);
			flushLiteral(segments, literal);
			segments.add(seg);
		}
		return close + 1;
	}

	/**
	 * Records the lookup key of a {@code P} reference ({@code ${name}}, {@code ${name:default}}) as an identifier, since
	 * the shortcut reads a named value.  The default is not a reference.  A key containing {@code ${} is reported as
	 * written.
	 */
	private void addKeyRef(VarCall call, String inner, int innerStart) {
		if (! (call.varDef() instanceof DefaultingVar) || ! DOLLAR_BRACE_VAR.equals(call.prefix()))
			return;
		var rest = inner.substring(call.restStart());
		var j = VarSplit.firstTopLevel(rest, ':');
		var len = j < 0 ? rest.length() : j;
		var body = call.body();
		var comma = body.indexOf(',');
		var key = comma < 0 ? body : body.substring(0, comma);
		var from = innerStart + call.restStart();
		refs.add(new TemplateReference(TemplateReference.Kind.IDENT, key, true, null, from, from + len));
	}

	private void syntax(String message, int start, int end) {
		refs.add(new TemplateReference(TemplateReference.Kind.SYNTAX, message, false, null, start, end));
	}

	/** Walks an expression, recording vars, calls and identifiers.  {@code base} is the source offset of the expression text. */
	private void collectRefs(Expr root, int base) {
		walk(root, base, ROOT);
	}

	/** Identifier scope: top level, a row of a named list, or a body whose target cannot be named. */
	@SuppressWarnings({
		"java:S2129" // The String constructor guarantees a unique identity that no interned literal can equal
	})
	private static final String ROOT = new String("<root>");

	@SuppressWarnings({
		"java:S2129" // See ROOT: a unique identity marker
	})
	private static final String OPAQUE = new String("<opaque>");

	@SuppressWarnings({
		"java:S4973" // Scope markers are compared by identity on purpose, so they stay distinct from any equal name
	})
	private void walk(Expr e, int base, String scope) {
		if (e instanceof Expr.Ident x) {
			if (scope != OPAQUE)
				refs.add(new TemplateReference(TemplateReference.Kind.IDENT, x.name(), true, scope == ROOT ? null : scope, base + x.pos(), base + x.pos() + x.name().length()));
		} else if (e instanceof Expr.VarRef x) {
			var raw = x.raw();
			var call = translate(resolver.getVarMap()::get, raw.substring(2, raw.length() - 1));
			String name;
			if (call.error() != null)
				name = raw;
			else
				name = call.prefix().isEmpty() ? DOLLAR_BRACE_VAR : call.prefix();
			refs.add(new TemplateReference(TemplateReference.Kind.VAR, name, call.error() == null && call.varDef() != null, null, base + x.pos(), base + x.pos() + raw.length()));
			if (call.error() == null)
				addKeyRef(call, raw.substring(2, raw.length() - 1), base + x.pos() + 2);
		} else if (e instanceof Expr.Call x) {
			var bound = ! isDenied(x.fn()) && (resolver.getFunctionMap().containsKey(x.fn()) || x.fn().startsWith("$op."));
			refs.add(new TemplateReference(TemplateReference.Kind.FUNCTION, x.fn(), bound, null, base + x.pos(), base + x.pos() + x.fn().length()));
			x.args().forEach(a -> walk(a, base, scope));
		} else {
			var op = opName(e);
			if (op != null && ! resolver.isAllowed(op))
				refs.add(new TemplateReference(TemplateReference.Kind.FUNCTION, op, false, null, base + e.pos(), base + e.pos() + 1));
			if (e instanceof Expr.Unary x) walk(x.e(), base, scope);
			else if (e instanceof Expr.Binary x) { walk(x.l(), base, scope); walk(x.r(), base, scope); }
			else if (e instanceof Expr.Ternary x) { walk(x.c(), base, scope); walk(x.a(), base, scope); walk(x.b(), base, scope); }
			else if (e instanceof Expr.Elvis x) { walk(x.l(), base, scope); walk(x.r(), base, scope); }
			else if (e instanceof Expr.Path x) walk(x.target(), base, scope);
			else if (e instanceof Expr.Index x) { walk(x.target(), base, scope); walk(x.idx(), base, scope); }
			else if (e instanceof Expr.Select x) { walk(x.target(), base, scope); walk(x.pred(), base, bodyScope(x.target(), scope)); }
			else if (e instanceof Expr.Project x) { walk(x.target(), base, scope); walk(x.map(), base, bodyScope(x.target(), scope)); }
		}
	}

	/** The identifier scope of a select/project body: the list name when the target is a top-level identifier. */
	@SuppressWarnings({
		"java:S4973" // Scope markers are compared by identity on purpose
	})
	private static String bodyScope(Expr target, String scope) {
		return target instanceof Expr.Ident x && scope == ROOT ? x.name() : OPAQUE;
	}

	private String unavailableMessage(Expr e) {
		if (e instanceof Expr.Call c)
			return isDenied(c.fn()) ? "function '" + c.fn() + "' is not permitted" : "unknown function '" + c.fn() + "'";
		return "operator '" + opName(e) + "' is not permitted";
	}

	private boolean isDenied(String fn) {
		return resolver.isDenied(fn) || (fn.startsWith("$op.") && ! resolver.isAllowed(fn));
	}

	/** The reserved <c>$op.*</c> function an operator node dispatches to, or <jk>null</jk> if the node is not an operator. */
	private static String opName(Expr e) {
		String n = null;
		if (e instanceof Expr.Unary x) n = ExprEvaluator.unaryName(x.op());
		else if (e instanceof Expr.Binary x) n = ExprEvaluator.binaryName(x.op());
		else if (e instanceof Expr.Ternary) n = "ternary";
		else if (e instanceof Expr.Elvis) n = "elvis";
		else if (e instanceof Expr.Select) n = "select";
		else if (e instanceof Expr.Project) n = "project";
		return n == null ? null : "$op." + n;
	}

	/**
	 * Finds the first node (in source order) that cannot be evaluated: an {@link Expr.Call} whose function is not
	 * registered or is denied by the allowlist, or an operator whose <c>$op.*</c> function is denied by the allowlist.
	 *
	 * <p>
	 * An operator whose function is merely unregistered (and not denied) is not reported here; it fails at evaluation.
	 * Because the whole AST is walked, an unavailable node on a branch that would never be taken (e.g. the unused
	 * side of a ternary) is reported at compile time, not only when that branch is evaluated.
	 *
	 * <p>
	 * A {@code ${...}} reference is opaque in the AST, so a function call inside a var body nested in the expression
	 * is not checked here; it is checked lazily, when the reference is evaluated.
	 *
	 * @param e The expression.
	 * @return The first unavailable node, or <jk>null</jk> if every call and operator resolves.
	 */
	private Expr findUnknownCall(Expr root) {
		// Iterative pre-order walk (explicit stack) so deep left-associative chains cannot overflow the call stack.
		var stack = new ArrayDeque<Expr>();
		stack.push(root);
		while (! stack.isEmpty()) {
			var e = stack.pop();
			var op = opName(e);
			if (op != null && ! resolver.isAllowed(op))
				return e;
			if (e instanceof Expr.Call c) {
				if (isDenied(c.fn()) || (resolver.findFunction(c.fn()) == null && ! c.fn().startsWith("$op.")))
					return c;
				var args = c.args();
				for (var i = args.size() - 1; i >= 0; i--)
					stack.push(args.get(i));
			} else if (e instanceof Expr.Unary x) {
				stack.push(x.e());
			} else if (e instanceof Expr.Binary x) {
				stack.push(x.r()); stack.push(x.l());
			} else if (e instanceof Expr.Ternary x) {
				stack.push(x.b()); stack.push(x.a()); stack.push(x.c());
			} else if (e instanceof Expr.Elvis x) {
				stack.push(x.r()); stack.push(x.l());
			} else if (e instanceof Expr.Path x) {
				stack.push(x.target());
			} else if (e instanceof Expr.Index x) {
				stack.push(x.idx()); stack.push(x.target());
			} else if (e instanceof Expr.Select x) {
				stack.push(x.pred()); stack.push(x.target());
			} else if (e instanceof Expr.Project x) {
				stack.push(x.map()); stack.push(x.target());
			}
			// Literal, VarRef, Ident: leaf
		}
		return null;
	}

	/**
	 * Compiles a var body, re-positioning strict-mode errors against the outer template.
	 *
	 * <p>
	 * Positions map exactly when the translation kept the length of the text after the prefix (only {@code :} to
	 * {@code ,} rewrites); otherwise the error is positioned at the outer marker.
	 */
	private VarTemplate compileNested(VarCall call, String inner, String input, int bodyStart, int markerStart) {
		var body = call.body();
		try {
			return compile(resolver, body, depth + 1);
		} catch (ExprException e) {
			var exact = body.length() == inner.length() - call.restStart();
			var pos = exact ? bodyStart + call.restStart() + e.getPos() : markerStart;
			throw new ExprException(e.getBaseMessage(), input, pos, e);
		}
	}

	private static boolean isAsciiLetter(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
	}

	private static void flushLiteral(List<TemplateSegment> out, StringBuilder buf) {
		if (!buf.isEmpty()) {
			out.add(new LiteralSegment(buf.toString()));
			buf.setLength(0);
		}
	}

	// ============================================================================
	// ${...} body translation (shared with the expression evaluator)
	// ============================================================================

	/**
	 * A translated {@code ${...}} reference.
	 *
	 * @param prefix The var name ({@code "P"} for the empty-prefix shortcut).
	 * @param varDef The registered var, or <jk>null</jk> if none (only possible when {@code P} is not registered).
	 * @param body The body in the var's internal comma form, or <jk>null</jk> if {@link #error()} is set.
	 * @param error Why the body cannot be represented, or <jk>null</jk>.
	 * @param restStart Offset in the reference body where the text after the prefix starts (<c>0</c> if no prefix
	 * 	was stripped).
	 */
	record VarCall(String prefix, Var varDef, String body, String error, int restStart) {}

	/**
	 * Translates the text between {@code ${} and {@code }} into a var and its internal comma-form body.
	 *
	 * <p>
	 * Used by the template compiler and by the expression evaluator for {@code ${...}} inside {@code #{...}}, so both
	 * read a reference the same way (see the class Javadoc for the rules).
	 *
	 * @param lookup Finds a registered var by name.
	 * @param inner The reference body.
	 * @return The translation. Never <jk>null</jk>.
	 */
	static VarCall translate(Function<String,Var> lookup, String inner) {
		var i = VarSplit.firstTopLevel(inner, ':');
		String prefix = null;
		Var v = null;
		var rest = inner;
		var restStart = 0;
		if (i > 0 && isAsciiLetters(inner, i)) {
			v = lookup.apply(inner.substring(0, i));
			if (v != null) {
				prefix = inner.substring(0, i);
				rest = inner.substring(i + 1);
				restStart = i + 1;
			}
		}
		if (v == null) {
			prefix = DOLLAR_BRACE_VAR;
			v = lookup.apply(DOLLAR_BRACE_VAR);
		}
		if (v instanceof MultipartResolvingVar || v instanceof MultipartVar) {
			var sb = new StringBuilder();
			for (var part : VarSplit.splitTopLevel(rest, ':')) {
				if (hasTopLevelComma(part) && ! isQuoted(part))
					return commaError(prefix, part, restStart);
				var p = keyPart(part);
				if (! sb.isEmpty())
					sb.append(',');
				sb.append(p);
			}
			return new VarCall(prefix, v, sb.toString(), null, restStart);
		}
		if (v == null || v instanceof DefaultingVar) {
			var j = VarSplit.firstTopLevel(rest, ':');
			var rawKey = j < 0 ? rest : rest.substring(0, j);
			if (hasTopLevelComma(rawKey) && ! isQuoted(rawKey))
				return commaError(prefix, rawKey, restStart);
			var key = keyPart(rawKey);
			return new VarCall(prefix, v, j < 0 ? key : key + ',' + unescape(rest.substring(j + 1)), null, restStart);
		}
		return new VarCall(prefix, v, VarSplit.restoreCommas(unescape(rest)), null, restStart);
	}

	private static VarCall commaError(String prefix, String key, int restStart) {
		return new VarCall(prefix, null, null, "Variable '" + prefix + "' key '" + key + "' contains ',' which is not supported in a key", restStart);
	}

	private static boolean isQuoted(String s) {
		var n = s.length();
		return n >= 2 && (s.charAt(0) == '\'' || s.charAt(0) == '"') && s.charAt(n - 1) == s.charAt(0);
	}

	private static boolean isAsciiLetters(String s, int end) {
		for (var i = 0; i < end; i++)
			if (! isAsciiLetter(s.charAt(i)))
				return false;
		return true;
	}

	/** Unquotes a fully quoted key part, else removes the grammar escapes (see {@link #unescape(String)}). */
	private static String keyPart(String s) {
		var n = s.length();
		if (n >= 2) {
			var q = s.charAt(0);
			if ((q == '\'' || q == '"') && s.charAt(n - 1) == q)
				return s.substring(1, n - 1).replace(',', VarSplit.COMMA);
		}
		return unescape(s);
	}

	/**
	 * Removes the escape backslash before {@code :}, {@code '}, {@code "}, {@code ,} and {@code }} outside nested braces.
	 *
	 * <p>
	 * An escaped comma becomes {@link VarSplit#COMMA}, an internal-only character the var classes turn back into
	 * {@code ,} after they split the internal comma form, so a literal comma is never re-split.  an escaped backslash becomes {@link VarSplit#BACKSLASH} (restored the same way); every
	 * other escape is kept for the nested template compile.
	 */
	private static String unescape(String s) {
		if (s.indexOf('\\') == -1)
			return s;
		var sb = new StringBuilder(s.length());
		var depth = 0;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '\\' && i + 1 < s.length()) {
				i++;
				var d = s.charAt(i);
				if (depth == 0 && (d == ':' || d == '\'' || d == '"' || d == '}')) {
					sb.append(d);
				} else if (depth == 0 && d == ',') {
					sb.append(VarSplit.COMMA);
				} else if (depth == 0 && d == '\\') {
					sb.append(VarSplit.BACKSLASH);
				} else {
					sb.append(c).append(d);
				}
			} else {
				if (c == '{')
					depth++;
				else if (c == '}' && depth > 0)
					depth--;
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/** Whether {@code s} has an unescaped {@code ,} outside nested braces. */
	private static boolean hasTopLevelComma(String s) {
		var depth = 0;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '\\')
				i++;
			else if (c == '{')
				depth++;
			else if (c == '}' && depth > 0)
				depth--;
			else if (c == ',' && depth == 0)
				return true;
		}
		return false;
	}

	/**
	 * Returns whether a var reference is eligible for compile-time folding: the var opted in via {@link Var#isStable()},
	 * is a {@link SimpleVar}, and its body has no nested live markers.
	 *
	 * @param v The var (may be <jk>null</jk>).
	 * @param hasInternalVar Whether the body contains nested live markers.
	 * @return <jk>true</jk> if stable.
	 */
	static boolean isStable(Var v, boolean hasInternalVar) {
		return v != null && v.isStable() && ! hasInternalVar && v instanceof SimpleVar;
	}

	/**
	 * Attempt compile-time stable-value folding for a {@code ${...}} dispatch.
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
	private String tryFoldStableVar(Var v, VarTemplate body, boolean hasInternalVar) {
		if (! isStable(v, hasInternalVar) || ! body.isLiteral())
			return null;
		// body.isLiteral() guaranteed above; resolve(session) short-circuits to literalText
		// and never touches session beans.
		return foldStableVarText(resolver, v, body::resolve);
	}

	/**
	 * Shared fold guard: resolves a stable var on a fresh no-bean session.
	 *
	 * <p>
	 * Returns {@code null} (do not fold) if {@link Var#canResolve(VarResolverSession)} is false, the resolved value is
	 * {@code null}, it would need recursive resolution ({@code $} present and {@link Var#allowRecurse()}), or anything throws.
	 *
	 * @param resolver The resolver.
	 * @param v The var (already known to be {@link #isStable(Var, boolean) stable}).
	 * @param body Produces the var body text from the fresh session.
	 * @return The resolved text, or {@code null} if folding must be skipped.
	 */
	@SuppressWarnings({
		"java:S1166" // Exception swallowed intentionally — fold-or-skip contract.
	})
	static String foldStableVarText(VarResolver resolver, Var v, Function<VarResolverSession,String> body) {
		try {
			var session = resolver.createSession();
			if (!v.canResolve(session))
				return null;
			var resolved = v.doResolve(session, body.apply(session));
			if (resolved == null)
				return null;
			// Skip folding if the resolved value would itself need recursive resolution. The
			// runtime VarRefSegment.resolve(...) path post-resolves dispatch results that
			// contain ${...} / ${X:...} markers when v.allowRecurse() returns true; folding
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
}
