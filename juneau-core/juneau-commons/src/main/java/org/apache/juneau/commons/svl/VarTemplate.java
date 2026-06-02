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
import java.util.function.*;

/**
 * A pre-compiled SVL template.
 *
 * <p>
 * A {@link VarTemplate} is the result of compiling a template string against a {@link VarResolver}.
 * It separates the "parse" step from the "evaluate" step so that repeated resolutions of the same
 * input string skip tokenization, var-registry lookup, and (eventually) function-dispatch metadata.
 *
 * <p>
 * Templates are obtained via {@link VarResolver#compile(String)} or
 * {@link VarResolverSession#compile(String)} — there is no public constructor.
 *
 * <h5 class='section'>Compile-binding contract:</h5>
 * <p>
 * A compiled template is bound to the {@link VarResolver} instance it was compiled against. Cached
 * {@link Var} (and, in the future, {@link VarFunction}) references are resolved at compile time.
 * Passing a {@link VarTemplate} produced by one resolver to a session belonging to a different
 * resolver is undefined behavior. Rebuilding the resolver invalidates any compiled templates from
 * the previous instance — caller's responsibility to re-compile.
 *
 * <h5 class='section'>Threadsafety:</h5>
 * <p>
 * {@link VarTemplate} instances are immutable and threadsafe — the same template can be safely
 * shared across threads. Note that {@link VarResolverSession} is <b>not</b> threadsafe; the
 * session passed to {@link #resolve(VarResolverSession)} or {@link #asSupplier(VarResolverSession)}
 * inherits the standard session-threadsafety contract. The {@link #asSupplierWithFreshSessions(VarResolver)}
 * variant opens a fresh session per call and is safe to share across threads.
 *
 * <h5 class='section'>Which Supplier do I want?</h5>
 * <ul>
 * 	<li>{@link #asSupplierWithFreshSessions(VarResolver)} — opens a fresh session per
 * 		{@code .get()} call. Safe to share across threads. Use this for user-facing code,
 * 		hot-reload patterns, and any situation where the {@code Supplier} may be invoked
 * 		on threads other than the caller's. <b>Recommended default.</b>
 * 	<li>{@link #asSupplier(VarResolverSession)} — captures the existing session and reuses
 * 		it on every {@code .get()} call. <b>Inherits the session's threadsafety contract</b>
 * 		(i.e. not threadsafe). Use only in perf-sensitive paths where the caller owns the
 * 		session lifecycle.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SimpleVariableLanguageBasics">Simple Variable Language Basics</a>
 * </ul>
 */
public final class VarTemplate {

	/**
	 * The raw template string captured at compile time. Returned by {@link #getSource()}.
	 */
	private final String input;

	/**
	 * The resolver this template was compiled against. Used by
	 * {@link #asSupplierWithFreshSessions(VarResolver)} to open fresh sessions and by tests for
	 * resolver-binding diagnostics.
	 */
	private final VarResolver resolver;

	/**
	 * The compiled segments. Walked in order by {@link #resolve(VarResolverSession)} and
	 * {@link #resolveTo(VarResolverSession, Writer)}. Empty array for empty input; single
	 * {@link LiteralSegment} for inputs with no special chars; mixed sequence otherwise.
	 *
	 * <p>
	 * Compile-time stable-value folding may replace {@link VarRefSegment} entries with
	 * {@link LiteralSegment} entries when the wrapped {@link Var} opts in via
	 * {@link Var#isStable()}.
	 */
	private final TemplateSegment[] segments;

	/**
	 * Cached {@link #isLiteral()} result. Computed once at construction by inspecting
	 * {@link #segments}.
	 */
	private final boolean literal;

	/**
	 * Pre-computed result for literal-only templates. {@code null} when {@link #literal} is
	 * {@code false}.
	 *
	 * <p>
	 * <b>Why this is not just {@link #input}:</b> escape interpretation can produce a literal
	 * whose text differs from the source. For example {@code "\${literal}"} compiles to a single
	 * {@link LiteralSegment} carrying the string {@code "${literal}"} — the resolved output is
	 * <i>not</i> the raw input. Compile-time stable-value folding similarly produces
	 * literal-only templates whose resolved text is the folded value, not the source. Caching
	 * the joined literal text once at compile time avoids walking segments on every resolve.
	 */
	private final String literalText;

	/**
	 * Package-private constructor — instances are created exclusively via
	 * {@link VarTemplateCompiler#compile(VarResolver, String)} (which is invoked by
	 * {@link VarResolver#compile(String)} and {@link VarResolverSession#compile(String)}).
	 *
	 * @param resolver The resolver this template is bound to.
	 * @param input The raw template string.
	 * @param segments The compiled segments.
	 */
	VarTemplate(VarResolver resolver, String input, TemplateSegment[] segments) {
		this.resolver = resolver;
		this.input = input;
		this.segments = segments;
		this.literal = computeIsLiteral(segments);
		// For literal-only templates, pre-compute the resolved text. For null source we keep
		// literalText null so resolve() returns null (matching VarResolverSession.resolve(null)).
		this.literalText = literal ? (input == null ? null : joinLiteralSegments(segments)) : null;
	}

	/**
	 * Resolves this template against the given session.
	 *
	 * <p>
	 * Walks the cached segment array in order, accumulating the result in a {@link StringBuilder}.
	 * Literal-only templates fast-path to a single allocation skip and return the source string
	 * directly.
	 *
	 * @param session The session to resolve against. Must not be {@code null}. The session is
	 * 	not required to belong to the same {@link VarResolver} this template was compiled
	 * 	against, but cached {@link Var} references will only honor that resolver's registry —
	 * 	see the "compile-binding contract" note in the class javadoc.
	 * @return The resolved string. Never {@code null}.
	 */
	public String resolve(VarResolverSession session) {
		if (literal)
			return literalText;
		var sb = new StringBuilder(input.length() * 2);
		for (var seg : segments)
			seg.resolve(session, sb);
		return sb.toString();
	}

	/**
	 * Resolves this template against the given session and writes the result directly to
	 * {@code out}.
	 *
	 * <p>
	 * Streamed-Var-bearing segments write through to {@code out} without materializing an
	 * intermediate {@code String}. Used by {@link VarResolver#resolveTo(String, Writer)} so
	 * downstream serializers can stream large content efficiently.
	 *
	 * @param session The session to resolve against.
	 * @param out The writer to append to.
	 * @return {@code out}, for fluent-API chaining.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public Writer resolveTo(VarResolverSession session, Writer out) throws IOException {
		if (literal) {
			out.write(literalText);
			return out;
		}
		for (var seg : segments)
			seg.resolveTo(session, out);
		return out;
	}

	/**
	 * Returns a {@link Supplier} that resolves this template against the given session each time
	 * its {@link Supplier#get()} method is called.
	 *
	 * <p>
	 * The captured session is reused across {@code .get()} calls; this Supplier is <b>not</b>
	 * threadsafe (it inherits the session's threadsafety contract). For a threadsafe variant
	 * use {@link #asSupplierWithFreshSessions(VarResolver)}.
	 *
	 * @param session The session to capture. Must not be {@code null}.
	 * @return A {@code Supplier<String>} bound to {@code session}.
	 */
	public Supplier<String> asSupplier(VarResolverSession session) {
		return () -> resolve(session);
	}

	/**
	 * Returns a threadsafe {@link Supplier} that opens a fresh {@link VarResolverSession} on
	 * every {@link Supplier#get()} call.
	 *
	 * <p>
	 * Use this variant when the returned {@code Supplier} may be invoked from threads other
	 * than the caller's — e.g. when stashed on a long-lived bean field or shared across
	 * worker threads. The fresh-session-per-{@code .get()} pattern is the recommended default
	 * for user-facing code.
	 *
	 * @param vr The resolver to open sessions against. Must not be {@code null}. Typically
	 * 	the same resolver this template was compiled against.
	 * @return A threadsafe {@code Supplier<String>}.
	 */
	public Supplier<String> asSupplierWithFreshSessions(VarResolver vr) {
		return () -> resolve(vr.createSession());
	}

	/**
	 * Returns {@code true} if this template contains no variable references and resolves to a
	 * fixed string.
	 *
	 * <p>
	 * Computed at compile time by inspecting the segment array: {@code true} iff the array is
	 * empty or contains only {@link LiteralSegment} entries. Stable-value folding may flip a
	 * template with stable {@link Var}s to literal by replacing the relevant {@link VarRefSegment}
	 * entries with {@link LiteralSegment} entries at compile time.
	 *
	 * <p>
	 * Callers can use this hint to fast-path purely-literal templates — for example, by skipping
	 * the {@code Supplier} wrapping overhead for a {@code @Value("plain string")} field:
	 *
	 * <p class='bjava'>
	 * 	<jk>var</jk> <jv>tpl</jv> = <jv>vr</jv>.compile(<jv>raw</jv>);
	 * 	<jk>if</jk> (<jv>tpl</jv>.isLiteral())
	 * 		<jc>// No need to wrap in a Supplier; the value never changes.</jc>
	 * 		<jk>return</jk> <jv>tpl</jv>.resolve(<jv>session</jv>);
	 * </p>
	 *
	 * @return {@code true} if the template resolves to a fixed string.
	 */
	public boolean isLiteral() {
		return literal;
	}

	/**
	 * Returns the raw input string this template was compiled from.
	 *
	 * <p>
	 * Useful for diagnostics, error messages, and debugging. The string is preserved verbatim
	 * including any escape sequences.
	 *
	 * @return The raw input string.
	 */
	public String getSource() {
		return input;
	}

	/**
	 * The resolver this template is bound to.
	 *
	 * <p>
	 * Package-private accessor for use by {@link VarResolver}/{@link VarResolverSession} and
	 * test code. Callers that need fresh-session resolution should use
	 * {@link #asSupplierWithFreshSessions(VarResolver)} instead.
	 *
	 * @return The bound resolver.
	 */
	VarResolver getResolver() {
		return resolver;
	}

	/**
	 * Package-private accessor for the compiled segments. Used by tests and by Phase 2's
	 * stable-value folding pass.
	 *
	 * @return The compiled segments. The array is shared (not copied); callers must not mutate.
	 */
	TemplateSegment[] segments() {
		return segments;
	}

	/**
	 * @return Number of compiled segments — useful for tests asserting structural shape of the
	 *	 compiled output.
	 */
	int segmentCount() {
		return segments.length;
	}

	/**
	 * Inspects the segment array to determine whether this template is purely literal.
	 *
	 * <p>
	 * Implementation detail: empty array → literal; all-{@link LiteralSegment} → literal;
	 * anything else → not literal.
	 */
	private static boolean computeIsLiteral(TemplateSegment[] segments) {
		if (segments == null || segments.length == 0)
			return true;
		for (var seg : segments)
			if (!seg.isLiteral())
				return false;
		return true;
	}

	/**
	 * Concatenates all {@link LiteralSegment} text into a single string. Caller must have
	 * verified {@link #computeIsLiteral(TemplateSegment[])} is {@code true} before calling.
	 *
	 * <p>
	 * Used to pre-compute {@link #literalText} at construction time so the literal-only
	 * fast-path in {@link #resolve(VarResolverSession)} avoids walking segments per call.
	 */
	private static String joinLiteralSegments(TemplateSegment[] segments) {
		if (segments.length == 0)
			return "";
		if (segments.length == 1)
			return ((LiteralSegment) segments[0]).text;
		var sb = new StringBuilder();
		for (var seg : segments)
			sb.append(((LiteralSegment) seg).text);
		return sb.toString();
	}

	/**
	 * Convenience helper for callers that need to {@link #resolveTo(VarResolverSession, Writer)}
	 * but don't want to handle {@link IOException}. Wraps any thrown {@link IOException} in a
	 * {@link RuntimeException} via the standard {@code ThrowableUtils.toRex} helper.
	 *
	 * @param session The session to resolve against.
	 * @param out The writer to append to.
	 * @return {@code out}.
	 */
	@SuppressWarnings("resource")  // 'out' and 'session' are caller-owned; this method must not close them.
	public Writer resolveToUnchecked(VarResolverSession session, Writer out) {
		try {
			return resolveTo(session, out);
		} catch (IOException e) {
			throw toRex(e);
		}
	}

	@Override /* Overridden from Object */
	public String toString() {
		return "VarTemplate[" + input + "]";
	}
}
