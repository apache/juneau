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
package org.apache.juneau.utils;

import static org.apache.juneau.common.utils.StateEnum.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.text.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.common.utils.*;

/**
 * Utility class for matching strings against string expressions.
 *
 * <p>
 * Supports the following string constructs:
 * <ul>
 * 	<li><js>"foo"</js> - Single arguments.
 * 	<li><js>"foo,bar,baz"</js> - Multiple OR'ed arguments.
 * 	<li><js>"foo | bar | bqz"</js> - Multiple OR'ed arguments, pipe syntax.
 * 	<li><js>"foo || bar || bqz"</js> - Multiple OR'ed arguments, Java-OR syntax.
 * 	<li><js>"fo*"</js> - Patterns including <js>'*'</js> and <js>'?'</js>.
 * 	<li><js>"fo* &amp; *oo"</js> - Multiple AND'ed arguments, ampersand syntax.
 * 	<li><js>"fo* &amp;&amp; *oo"</js> - Multiple AND'ed arguments, Java-AND syntax.
 * 	<li><js>"fo* || (*oo || bar)"</js> - Parenthesis.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>AND operations take precedence over OR operations (as expected).
 * 	<li class='note'>Whitespace is ignored.
 * 	<li class='note'><jk>null</jk> or empty expressions always match as <jk>false</jk>.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class StringExpressionMatcher {

	static class And extends Exp {
		Exp[] clauses;

		And(List<Exp> clauses) {
			this.clauses = clauses.toArray(new Exp[clauses.size()]);
		}

		@Override /* Overridden from Object */
		public String toString() {
			return "(& " + StringUtils.join(clauses, " ") + ')';
		}

		@Override /* Overridden from Exp */
		void appendTokens(Set<String> set) {
			for (Exp clause : clauses)
				clause.appendTokens(set);
		}

		@Override /* Overridden from Exp */
		boolean matches(String input) {
			for (Exp e : clauses)
				if (! e.matches(input))
					return false;
			return true;
		}
	}

	static class Eq extends Exp {
		final String operand;

		Eq(String operand) {
			this.operand = operand;
		}

		@Override /* Overridden from Object */
		public String toString() {
			return "[= " + operand + "]";
		}

		@Override /* Overridden from Exp */
		void appendTokens(Set<String> set) {
			set.add(operand);
		}

		@Override /* Overridden from Exp */
		boolean matches(String input) {
			return operand.equals(input);
		}
	}

	abstract static class Exp {
		void appendTokens(Set<String> set) {}

		abstract boolean matches(String input);
	}

	static class Match extends Exp {
		final Pattern p;
		final String operand;

		Match(String operand) {
			this.operand = operand;
			p = StringUtils.getMatchPattern(operand);
		}

		@Override /* Overridden from Object */
		public String toString() {
			return "[* " + p.pattern().replaceAll("\\\\[QE]", "") + "]";
		}

		@Override /* Overridden from Exp */
		void appendTokens(Set<String> set) {
			set.add(operand);
		}

		@Override /* Overridden from Exp */
		boolean matches(String input) {
			return p.matcher(input).matches();
		}
	}

	static class Never extends Exp {
		@Override /* Overridden from Object */
		public String toString() {
			return "(NEVER)";
		}

		@Override
		boolean matches(String input) {
			return false;
		}
	}

	static class Or extends Exp {
		Exp[] clauses;

		Or(List<Exp> clauses) {
			this.clauses = clauses.toArray(new Exp[clauses.size()]);
		}

		@Override /* Overridden from Object */
		public String toString() {
			return "(| " + StringUtils.join(clauses, " ") + ')';
		}

		@Override /* Overridden from Exp */
		void appendTokens(Set<String> set) {
			for (Exp clause : clauses)
				clause.appendTokens(set);
		}

		@Override
		boolean matches(String input) {
			for (Exp e : clauses)
				if (e.matches(input))
					return true;
			return false;
		}
	}

	// @formatter:off
	private static final AsciiSet
		WS = AsciiSet.of(" \t"),
		OP = AsciiSet.of(",|&"),
		META = AsciiSet.of("*?");
	// @formatter:on

	private static Exp parseOperand(String operand) {
		boolean hasMeta = false;
		for (int i = 0; i < operand.length() && ! hasMeta; i++) {
			char c = operand.charAt(i);
			hasMeta |= META.contains(c);
		}
		return hasMeta ? new Match(operand) : new Eq(operand);
	}

	private final Exp exp;

	/**
	 * Constructor.
	 *
	 * @param expression The string expression.
	 * @throws ParseException Malformed input encountered.
	 */
	public StringExpressionMatcher(String expression) throws ParseException {
		this.exp = parse(expression);
	}

	/**
	 * Returns all the tokens used in this expression.
	 *
	 * @return All the tokens used in this expression.
	 */
	public Set<String> getTokens() {
		Set<String> set = new TreeSet<>();
		exp.appendTokens(set);
		return set;
	}

	/**
	 * Returns <jk>true</jk> if the specified string matches this expression.
	 *
	 * @param input The input string.
	 * @return
	 * 	<jk>true</jk> if the specified string matches this expression.
	 * 	<br>Always <jk>false</jk> if the string is <jk>null</jk>.
	 */
	public boolean matches(String input) {
		return nn(input) && exp.matches(input);
	}

	@Override /* Overridden from Object */
	public String toString() {
		return exp.toString();
	}

	private Exp parse(String expression) throws ParseException {
		if (isBlank(expression))
			return new Never();

		expression = expression.trim();

		List<Exp> ors = list();
		List<Exp> ands = list();

		StateEnum state = S1;
		int i = 0, mark = -1;
		int pDepth = 0;
		boolean error = false;

		for (i = 0; i < expression.length(); i++) {
			char c = expression.charAt(i);
			if (state == S1) {
				// S1 = Looking for start
				if (! WS.contains(c)) {
					if (c == '(') {
						state = S2;
						pDepth = 0;
						mark = i + 1;
					} else if (OP.contains(c)) {
						error = true;
						break;
					} else {
						state = S3;
						mark = i;
					}
				}
			} else if (state == S2) {
				// S2 = Found [(], looking for [)].
				if (c == '(')
					pDepth++;
				if (c == ')') {
					if (pDepth > 0)
						pDepth--;
					else {
						ands.add(parse(expression.substring(mark, i)));
						mark = -1;
						state = S4;
					}
				}
			} else if (state == S3) {
				// S3 = Found [A], looking for end of A.
				if (WS.contains(c) || OP.contains(c)) {
					ands.add(parseOperand(expression.substring(mark, i)));
					mark = -1;
					if (WS.contains(c)) {
						state = S4;
					} else {
						i--;
						state = S5;
					}
				}
			} else if (state == S4) {
				// S4 = Found [A ], looking for & or | or ,.
				if (! WS.contains(c)) {
					if (OP.contains(c)) {
						i--;
						state = S5;
					} else {
						error = true;
						break;
					}
				}
			} else if (state == S5) {
				// S5 = Found & or | or ,.
				if (c == '&') {
					//ands.add(operand);
					state = S6;
				} else /* (c == '|' || c == ',') */ {
					if (ands.size() == 1) {
						ors.add(ands.get(0));
					} else {
						ors.add(new And(ands));
					}
					ands.clear();
					if (c == '|') {
						state = S7;
					} else {
						state = S1;
					}
				}
			} else if (state == S6) {
				// S6 = Found &, looking for & or other
				if (! WS.contains(c)) {
					if (c != '&')
						i--;
					state = S1;
				}
			} else /* (state == S7) */ {
				// S7 = Found |, looking for | or other
				if (! WS.contains(c)) {
					if (c != '|')
						i--;
					state = S1;
				}
			}
		}

		if (error)
			throw new ParseException("Invalid character in expression '" + expression + "' at position " + i + ". state=" + state, i);

		if (state == S1)
			throw new ParseException("Could not find beginning of clause in '" + expression + "'", i);
		if (state == S2)
			throw new ParseException("Could not find matching parenthesis in expression '" + expression + "'", i);
		if (state == S5 || state == S6 || state == S7)
			throw new ParseException("Dangling clause in expression '" + expression + "'", i);

		if (mark != -1)
			ands.add(parseOperand(expression.substring(mark, expression.length())));
		if (ands.size() == 1)
			ors.add(ands.get(0));
		else
			ors.add(new And(ands));

		if (ors.size() == 1)
			return ors.get(0);
		return new Or(ors);
	}
}