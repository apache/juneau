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
package org.apache.juneau.commons.beanquery;

import static java.util.regex.Pattern.*;

import java.time.*;
import java.util.*;
import java.util.regex.*;

/**
 * Per-call {@code $regex} state for the in-memory engine (spec D7, §5 "Regex deadline").
 *
 * <p>
 * Each session call creates one budget.  Each {@code $regex} node is compiled once per call (cached by node identity),
 * and every match runs against a {@link CharSequence} wrapper that checks the clock every {@value #CHECK_INTERVAL}
 * {@code charAt} calls.  Past the deadline it throws {@link BeanQuerySyntaxException}, which stops a catastrophic
 * backtracking pattern.  Not thread-safe; sessions are single-threaded.
 */
final class RegexBudget {

	private static final int CHECK_INTERVAL = 256;

	private final long deadline;
	private final Map<SearchExpression,Optional<Pattern>> patterns = new IdentityHashMap<>();
	private int calls;

	/**
	 * Constructor.  The deadline starts now.
	 *
	 * @param timeout How long all regex matching in this call may take.
	 */
	RegexBudget(Duration timeout) {
		deadline = System.nanoTime() + timeout.toNanos();
	}

	/**
	 * Matches a cell against a {@code $regex} node (full-string match).
	 *
	 * @param node The {@code $regex} expression node.
	 * @param value The cell text.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the whole value matches; <jk>false</jk> for an invalid pattern.
	 * @throws BeanQuerySyntaxException If the deadline passes during matching.
	 */
	boolean matches(SearchExpression node, String value) {
		var p = patterns.computeIfAbsent(node, RegexBudget::compile);
		return p.isPresent() && p.get().matcher(new Deadlined(value)).matches();
	}

	private static Optional<Pattern> compile(SearchExpression node) {
		var args = node.literalArgs();
		var flags = CASE_INSENSITIVE;  // Default when no flags= given.
		for (var i = 1; i < args.size(); i++) {
			var a = args.get(i).strip();
			if (a.startsWith("flags="))
				flags = parseFlags(a.substring(6));
		}
		try {
			return Optional.of(Pattern.compile(args.get(0), flags));
		} catch (PatternSyntaxException e) {
			return Optional.empty();
		}
	}

	private static int parseFlags(String f) {
		var flags = 0;
		for (var c : f.toCharArray()) {
			if (c == 'i')
				flags |= CASE_INSENSITIVE;
			else if (c == 'm')
				flags |= MULTILINE;
			else if (c == 's')
				flags |= DOTALL;
		}
		return flags;
	}

	/** A {@link CharSequence} view that spends the budget on every {@code charAt}. */
	private final class Deadlined implements CharSequence {
		private final CharSequence s;

		Deadlined(CharSequence s) {
			this.s = s;
		}

		private void tick() {
			if (++calls % CHECK_INTERVAL == 0 && System.nanoTime() - deadline > 0)
				throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.REGEX_TIMEOUT, "Regex search took too long.");
		}

		@Override /* CharSequence */
		public int length() {
			return s.length();
		}

		@Override /* CharSequence */
		public char charAt(int index) {
			tick();
			return s.charAt(index);
		}

		@Override /* CharSequence */
		public CharSequence subSequence(int start, int end) {
			return new Deadlined(s.subSequence(start, end));
		}

		@Override /* Object */
		public String toString() {
			return s.toString();
		}
	}
}
