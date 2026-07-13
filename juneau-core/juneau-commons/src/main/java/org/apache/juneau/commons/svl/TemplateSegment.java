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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;

/**
 * Compiled element of a {@link VarTemplate}.
 *
 * <p>
 * Three concrete subclasses cover the three top-level token types produced by the unified
 * tokenizer in {@link VarTemplateCompiler}:
 * <ul>
 * 	<li>{@link LiteralSegment} — fixed text appended verbatim.
 * 	<li>{@link VarRefSegment} — a {@code ${...}} or {@code $X{...}} reference; resolves the
 * 		captured {@link Var} (cached at compile time) against the session.
 * 	<li>{@link ScriptSegment} — a {@code #{name(args...)}} function call. Function dispatch
 * 		is wired by {@link VarTemplate}; bare segments are constructed by the compiler.
 * </ul>
 *
 * <p>
 * Segments are immutable and threadsafe. The compile-binding contract documented on
 * {@link VarTemplate} applies to the captured {@link Var} references in {@link VarRefSegment}.
 */
abstract sealed class TemplateSegment permits LiteralSegment, VarRefSegment, ScriptSegment {

	/**
	 * Resolves this segment and appends the result to {@code out}.
	 *
	 * <p>
	 * Subclasses implement either this method or {@link #resolveTo(VarResolverSession, Writer)}
	 * — the default implementation of the other delegates here. Streamed-Var-bearing segments
	 * (only {@link VarRefSegment} for now) override the writer-based path to preserve the
	 * direct-streaming optimization that pre-existing in-place tokenization relied on.
	 *
	 * @param session The session to resolve against. Must not be {@code null}.
	 * @param out The buffer to append to. Must not be {@code null}.
	 */
	abstract void resolve(VarResolverSession session, StringBuilder out);

	/**
	 * Resolves this segment and writes the result directly to {@code w}.
	 *
	 * <p>
	 * Default implementation buffers via {@link #resolve(VarResolverSession, StringBuilder)};
	 * segments that wrap a streamed {@link Var} should override to write directly.
	 *
	 * @param session The session to resolve against.
	 * @param w The writer to append to.
	 * @throws IOException Thrown by the underlying stream.
	 */
	void resolveTo(VarResolverSession session, Writer w) throws IOException {
		var sb = new StringBuilder();
		resolve(session, sb);
		w.append(sb);
	}

	/**
	 * @return {@code true} if this segment contributes only fixed text (i.e. is a
	 *	 {@link LiteralSegment}).
	 */
	boolean isLiteral() { return false; }

	/**
	 * Helper used by {@link VarRefSegment} and {@link ScriptSegment} to wrap arbitrary
	 * checked exceptions raised during dispatch in a {@link VarResolverException} carrying
	 * the failing var/function name and the surrounding template source for diagnostics.
	 *
	 * @param e The original exception.
	 * @param sourceFragment A short representation of the failing token (e.g. {@code "$X{...}"}).
	 * @param identifier The var prefix or function name.
	 * @return The wrapped exception (never returns; always throws).
	 */
	static VarResolverException wrapDispatchFailure(Exception e, String identifier, String sourceFragment) {
		if (e instanceof VarResolverException e2)
			return e2;
		throw new VarResolverException(e, "Problem occurred resolving variable ''{0}'' in string ''{1}''", identifier, sourceFragment);
	}

	/**
	 * Convert {@link IOException} to {@link RuntimeException} for resolve paths that go
	 * through writer-based dispatch.
	 *
	 * @param e The IOException.
	 * @return The wrapped runtime exception.
	 */
	static RuntimeException wrapIo(IOException e) {
		return toRex(e);
	}
}
