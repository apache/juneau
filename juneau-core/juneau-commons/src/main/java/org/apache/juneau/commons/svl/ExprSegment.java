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

import java.util.function.*;

/**
 * A {@link TemplateSegment} that evaluates a parsed {@code #{expression}}.
 *
 * <p>
 * The AST is parsed once at compile time (the caller's job, via {@link ExprParser#parse(String, int)}).  Each render
 * builds a <em>fresh</em> {@link ExprContext} so the collection budget (which lives on the root context) is
 * independent per render.
 *
 * <p>
 * <b>Root scope:</b> identifiers resolve against the session's render scope
 * ({@link VarResolverSession#scope(Function)}); when none is set every {@link Expr.Ident} other than {@code this}
 * yields <jk>null</jk>.  Values can also be reached through {@code ${...}} var references.
 *
 * <p>
 * <b>Output is never re-parsed.</b>  The result is stringified and appended; it does not go through
 * {@code resolveRecursive} and {@code allowRecurse} does not apply.  Stringification is
 * {@link VarResolverSession#formatValue(Object)} (the value formatter).
 *
 * <p>
 * <b>Fall-through:</b> in lenient mode an unknown/denied function (an {@link ExprEvaluator.FallThrough}) emits the
 * verbatim {@code #{source}} text; in strict mode (and for every other failure) an {@link ExprException} propagates.
 */
final class ExprSegment extends TemplateSegment {

	/** Render scope used for identifier lookups when the session sets none: always absent. */
	static final Function<String,Object> EMPTY_SCOPE = n -> null;

	private final Expr ast;
	private final String source;

	/**
	 * Constructor.
	 *
	 * @param ast The parsed expression.
	 * @param source The expression text between {@code #{} and {@code }} (used for diagnostics and fall-through).
	 */
	ExprSegment(Expr ast, String source) {
		this.ast = ast;
		this.source = source;
	}

	@Override
	void resolve(VarResolverSession session, StringBuilder out) {
		out.append(render(session));
	}

	/**
	 * Resolves with a value encoder.
	 *
	 * <p>
	 * The encoder runs on the formatted result.  A {@link Structure} result is not encoded here: it is formatted by the
	 * session formatter, which owns the encoding of its own items.  Fall-through text is author text and stays unencoded.
	 */
	@Override
	void resolve(VarResolverSession session, StringBuilder out, UnaryOperator<String> encoder) {
		try {
			var v = evaluate(session);
			var text = format(session, v);
			out.append(v instanceof Structure ? text : encoder.apply(text));
		} catch (@SuppressWarnings("unused") ExprEvaluator.FallThrough e) {
			out.append("#{").append(source).append('}');
		}
	}

	private String render(VarResolverSession session) {
		try {
			return format(session, evaluate(session));
		} catch (@SuppressWarnings("unused") ExprEvaluator.FallThrough e) {
			return "#{" + source + "}";
		}
	}

	/** Evaluates with a fresh context; may throw {@link ExprEvaluator.FallThrough} or {@link ExprException}. */
	private Object evaluate(VarResolverSession session) {
		return ExprEvaluator.eval(ast, new ExprContext(session, session.scope(), null, source));
	}

	/**
	 * Attempts compile-time folding.
	 *
	 * <p>
	 * An expression is stable when it contains only literals, operators, paths/indexes, and {@code ${...}} references
	 * to stable vars (see {@link VarTemplateCompiler#isStable(Var, boolean)}).  Identifiers, plain function calls,
	 * select/project are never folded.  Evaluation failures or fall-through skip folding.
	 *
	 * @param resolver The resolver (for var lookup and a throwaway session).
	 * @return The folded literal, or <jk>null</jk> if the expression is not stable or fails to evaluate.
	 */
	@SuppressWarnings({
		"java:S1166" // Exception swallowed intentionally — fold-or-skip contract.
	})
	LiteralSegment fold(VarResolver resolver) {
		if (! stable(ast, resolver))
			return null;
		try {
			// Fall-through and errors at fold time must never be baked in: skip folding so they resurface at render.
			return foldText(resolver);
		} catch (@SuppressWarnings("unused") RuntimeException e) {
			return null;
		}
	}

	/** Wraps a formatter failure (including strict-mode limit errors) in an {@link ExprException} positioned at the expression. */
	private String format(VarResolverSession session, Object value) {
		try {
			return session.formatValue(value);
		} catch (ExprException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ExprException("Value formatter failed: " + e.getMessage(), source, ast.pos(), e);
		}
	}

	/**
	 * Folds only formatter-invariant scalars (String, Number, Boolean); anything else (collections, Structure, beans)
	 * is formatted at render time so a per-render session formatter applies.  Note that a session formatter
	 * that rewrites scalars does not affect an already-folded stable expression.
	 */
	private LiteralSegment foldText(VarResolver resolver) {
		var s = resolver.createSession();
		var v = evaluate(s);
		if (v != null && ! (v instanceof CharSequence || v instanceof Number || v instanceof Boolean))
			return null;
		// An expression that reads a var carries data, so its folded text is a value; a pure-literal fold is author text.
		return new LiteralSegment(format(s, v), hasVarRef(ast));
	}

	private static boolean hasVarRef(Expr e) {
		if (e instanceof Expr.VarRef) return true;
		if (e instanceof Expr.Unary x) return hasVarRef(x.e());
		if (e instanceof Expr.Binary x) return hasVarRef(x.l()) || hasVarRef(x.r());
		if (e instanceof Expr.Ternary x) return hasVarRef(x.c()) || hasVarRef(x.a()) || hasVarRef(x.b());
		if (e instanceof Expr.Elvis x) return hasVarRef(x.l()) || hasVarRef(x.r());
		if (e instanceof Expr.Path x) return hasVarRef(x.target());
		if (e instanceof Expr.Index x) return hasVarRef(x.target()) || hasVarRef(x.idx());
		return false;
	}

	private static boolean stable(Expr e, VarResolver r) {
		if (e instanceof Expr.Literal) return true;
		if (e instanceof Expr.Unary x) return stable(x.e(), r);
		if (e instanceof Expr.Binary x) return stable(x.l(), r) && stable(x.r(), r);
		if (e instanceof Expr.Ternary x) return stable(x.c(), r) && stable(x.a(), r) && stable(x.b(), r);
		if (e instanceof Expr.Elvis x) return stable(x.l(), r) && stable(x.r(), r);
		if (e instanceof Expr.Path x) return stable(x.target(), r);
		if (e instanceof Expr.Index x) return stable(x.target(), r) && stable(x.idx(), r);
		if (e instanceof Expr.VarRef x) return stableVarRef(x, r);
		return false;  // Ident, Call, Select, Project
	}

	private static boolean stableVarRef(Expr.VarRef x, VarResolver r) {
		var raw = x.raw();
		var call = VarTemplateCompiler.translate(r.getVarMap()::get, raw.substring(2, raw.length() - 1));
		if (call.error() != null)
			return false;
		var v = call.varDef();
		var body = call.body();
		if (! VarTemplateCompiler.isStable(v, body.indexOf('$') != -1))
			return false;
		// Same guards as the template compiler: canResolve on a bean-less session, non-null value.
		return VarTemplateCompiler.foldStableVarText(r, v, s -> body) != null;
	}
}
