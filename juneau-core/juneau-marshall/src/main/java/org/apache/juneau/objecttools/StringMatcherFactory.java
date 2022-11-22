// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.objecttools;

import static org.apache.juneau.common.internal.StringUtils.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;

/**
 * String matcher factory for the {@link ObjectSearcher} class.
 *
 * <p>
 * 	The class provides searching based on the following patterns:
 * </p>
 * <ul>
 * 	<li><js>"property=foo"</js> - Simple full word match
 * 	<li><js>"property=fo*"</js>, <js>"property=?ar"</js> - Meta-character matching
 * 	<li><js>"property=foo bar"</js>(implicit), <js>"property=^foo ^bar"</js>(explicit) - Multiple OR'ed patterns
 * 	<li><js>"property=+fo* +*ar"</js> - Multiple AND'ed patterns
 * 	<li><js>"property=fo* -bar"</js> - Negative patterns
 * 	<li><js>"property='foo bar'"</js> - Patterns with whitespace
 * 	<li><js>"property=foo\\'bar"</js> - Patterns with single-quotes
 * 	<li><js>"property=/foo\\s+bar"</js> - Regular expression match
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.ObjectTools">Overview &gt; juneau-marshall &gt; Object Tools</a>
 * </ul>
 */
public class StringMatcherFactory extends MatcherFactory {

	/**
	 * Default reusable matcher.
	 */
	public static final StringMatcherFactory DEFAULT = new StringMatcherFactory();

	@Override
	public boolean canMatch(ClassMeta<?> cm) {
		return true;
	}

	@Override
	public AbstractMatcher create(String pattern) {
		return new StringMatcher(pattern);
	}

	/**
	 * A construct representing a single search pattern.
	 */
	private static class StringMatcher extends AbstractMatcher {
		private String pattern;
		private static final AsciiSet
			META_CHARS = AsciiSet.create("*?'\""),
			SQ_CHAR = AsciiSet.create("'"),
			DQ_CHAR = AsciiSet.create("\""),
			REGEX_CHARS = AsciiSet.create("+\\[]{}()^$.");

		Pattern[] orPatterns, andPatterns, notPatterns;

		public StringMatcher(String searchPattern) {

			this.pattern = searchPattern.trim();
			List<Pattern> ors = new LinkedList<>();
			List<Pattern> ands = new LinkedList<>();
			List<Pattern> nots = new LinkedList<>();

			for (String s : splitQuoted(pattern, true)) {
				char c0 = s.charAt(0), c9 = s.charAt(s.length()-1);

				if (c0 == '/' && c9 == '/' && s.length() > 1) {
					ands.add(Pattern.compile(strip(s)));
				} else {
					char prefix = '^';
					boolean ignoreCase = false;
					if (s.length() > 1 && (c0 == '^' || c0 == '+' || c0 == '-')) {
						prefix = c0;
						s = s.substring(1);
						c0 = s.charAt(0);
					}

					if (c0 == '\'') {
						s = unEscapeChars(strip(s), SQ_CHAR);
						ignoreCase = true;
					} else if (c0 == '"') {
						s = unEscapeChars(strip(s), DQ_CHAR);
					}

					if (REGEX_CHARS.contains(s) || META_CHARS.contains(s)) {
						StringBuilder sb = new StringBuilder();
						boolean isInEscape = false;
						for (int i = 0; i < s.length(); i++) {
							char c = s.charAt(i);
							if (isInEscape) {
								if (c == '?' || c == '*' || c == '\\')
									sb.append('\\').append(c);
								else
									sb.append(c);
								isInEscape = false;
							} else {
								if (c == '\\')
									isInEscape = true;
								else if (c == '?')
									sb.append(".?");
								else if (c == '*')
									sb.append(".*");
								else if (REGEX_CHARS.contains(c))
									sb.append("\\").append(c);
								else
									sb.append(c);
							}
						}
						s = sb.toString();
					}


					int flags = Pattern.DOTALL;
					if (ignoreCase)
						flags |= Pattern.CASE_INSENSITIVE;

					Pattern p = Pattern.compile(s, flags);

					if (prefix == '-')
						nots.add(p);
					else if (prefix == '+')
						ands.add(p);
					else
						ors.add(p);
				}
			}
			orPatterns = ors.toArray(new Pattern[ors.size()]);
			andPatterns = ands.toArray(new Pattern[ands.size()]);
			notPatterns = nots.toArray(new Pattern[nots.size()]);
		}

		@Override
		public boolean matches(ClassMeta<?> cm, Object o) {
			String s = (String)o;
			for (int i = 0; i < andPatterns.length; i++)
				if (! andPatterns[i].matcher(s).matches())
					return false;
			for (int i = 0; i < notPatterns.length; i++)
				if (notPatterns[i].matcher(s).matches())
					return false;
			for (int i = 0; i < orPatterns.length; i++)
				if (orPatterns[i].matcher(s).matches())
					return true;
			return orPatterns.length == 0;
		}

		@Override
		public String toString() {
			return pattern;
		}
	}
}
