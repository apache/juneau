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

import java.io.*;

/**
 * A {@link TemplateSegment} that resolves a {@code ${...}} or {@code $X{...}} reference.
 *
 * <p>
 * The captured {@link Var} reference is resolved at compile time against the bound
 * {@link VarResolver}'s var registry — one map lookup per compiled template, zero per resolve.
 * The body is itself a {@link VarTemplate}, so nested {@code ${...}} / {@code $X{...}} /
 * {@code #{...}} markers compose naturally.
 *
 * <h5 class='section'>Resolve-time behavior:</h5>
 * <ol>
 * 	<li>Verify the cached {@link Var} can resolve in the supplied session
 * 		({@link Var#canResolve(VarResolverSession)}). If not, emit the pre-computed
 * 		{@link #fallthrough} text and return.
 * 	<li>Resolve the body — recursively if {@link Var#allowNested()} and the body contains nested
 * 		markers; raw source otherwise.
 * 	<li>Dispatch to {@link Var#doResolve(VarResolverSession, String)} (or, for streamed
 * 		{@link Var}s, {@link Var#resolveTo(VarResolverSession, Writer, String)}).
 * 	<li>If the resolver allows recursion ({@link Var#allowRecurse()}) and the result contains
 * 		further variable markers, post-resolve via {@link VarResolverSession#resolve(String)}.
 * </ol>
 *
 * <p>
 * For the {@code ${...}} shortcut, the body's source string has the first top-level {@code ':'}
 * rewritten to {@code ','} <i>at compile time</i> so the body matches {@link DefaultingVar}'s
 * {@code key,default} separator. The cached var resolves to the empty-name var if registered
 * (legacy back-compat) else falls back to {@code PropertyVar} ({@code "P"}).
 */
final class VarRefSegment extends TemplateSegment {

	/**
	 * The {@link Var} resolved at compile time. {@code null} only when the prefix did not match
	 * any registered var (in which case {@link #fallthrough} carries the verbatim original
	 * text).
	 */
	private final Var cachedVar;

	/** The raw prefix from the source (e.g. {@code "X"} for {@code $X{...}}, {@code ""} for {@code ${...}}). */
	private final String prefix;

	/** The compiled body (nested markers pre-tokenized). */
	private final VarTemplate body;

	/**
	 * {@code true} if the body contained any {@code {}} during outer state-machine collection —
	 * equivalent to the legacy tokenizer's {@code hasInternalVar} flag. Drives whether
	 * {@link #resolve(VarResolverSession, StringBuilder)} re-resolves the body via
	 * {@link VarTemplate#resolve(VarResolverSession)} or passes the raw source verbatim. Match
	 * for the legacy {@code (hasInternalVar && r.allowNested() ? resolve(varVal) : varVal)}
	 * branch.
	 */
	private final boolean hasInternalVar;

	/**
	 * Pre-computed fallthrough text — the verbatim {@code $X{...}} fragment, with inner
	 * {@code \\}, {@code \$}, {@code \{}, {@code \}} escapes already unescaped. Used when the
	 * cached var is {@code null} or {@link Var#canResolve(VarResolverSession)} returns false.
	 */
	private final String fallthrough;

	VarRefSegment(Var cachedVar, String prefix, VarTemplate body, boolean hasInternalVar, String fallthrough) {
		this.cachedVar = cachedVar;
		this.prefix = prefix;
		this.body = body;
		this.hasInternalVar = hasInternalVar;
		this.fallthrough = fallthrough;
	}

	/**
	 * @return The compiled body. Useful for compile-time inspection (e.g. stable-value folding
	 *	 via stable-value folding).
	 */
	VarTemplate body() { return body; }

	/** @return The cached {@link Var}, or {@code null} if unregistered at compile time. */
	Var cachedVar() { return cachedVar; }

	@Override
	void resolve(VarResolverSession session, StringBuilder out) {
		var v = effectiveVar(session);
		if (v == null) {
			out.append(fallthrough);
			return;
		}
		try {
			var varVal = (hasInternalVar && v.allowNested()) ? body.resolve(session) : body.getSource();
			if (v.streamed) {
				var sw = new StringWriter();
				v.resolveTo(session, sw, varVal);
				out.append(sw.getBuffer());
			} else {
				var replacement = v.doResolve(session, varVal);
				if (replacement == null)
					replacement = "";
				if (replacement.indexOf('$') != -1 && v.allowRecurse())
					replacement = session.resolve(replacement);
				out.append(replacement);
			}
		} catch (Exception e) {
			throw wrapDispatchFailure(e, prefix, sourceFragment());
		}
	}

	@Override
	void resolveTo(VarResolverSession session, Writer w) throws IOException {
		var v = effectiveVar(session);
		if (v == null) {
			w.write(fallthrough);
			return;
		}
		try {
			var varVal = (hasInternalVar && v.allowNested()) ? body.resolve(session) : body.getSource();
			if (v.streamed) {
				v.resolveTo(session, w, varVal);
			} else {
				var replacement = v.doResolve(session, varVal);
				if (replacement == null)
					replacement = "";
				if (replacement.indexOf('$') != -1 && v.allowRecurse())
					replacement = session.resolve(replacement);
				w.write(replacement);
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw wrapDispatchFailure(e, prefix, sourceFragment());
		}
	}

	/**
	 * Returns the effective {@link Var} for this segment in the given session, or {@code null}
	 * if no var can resolve it.
	 *
	 * <p>
	 * Honors {@link Var#canResolve(VarResolverSession)} the same way the legacy in-place
	 * tokenizer's {@link VarResolverSession#getVar(String)} did — a var registered but unable
	 * to resolve in this session is treated as if not registered (fallthrough).
	 */
	private Var effectiveVar(VarResolverSession session) {
		var v = cachedVar;
		if (v == null)
			return null;
		return v.canResolve(session) ? v : null;
	}

	/** Approximation of the original source fragment for diagnostic messages. */
	private String sourceFragment() {
		return "$" + prefix + "{" + body.getSource() + "}";
	}
}
