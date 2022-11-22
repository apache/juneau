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
package org.apache.juneau.utils;

import java.text.ParseException;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StateMachineState.*;

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

	private final Exp exp;
	private static final AsciiSet
		WS = AsciiSet.create(" \t"),
		OP = AsciiSet.create(",|&"),
		META = AsciiSet.create("*?");

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
	 * Returns <jk>true</jk> if the specified string matches this expression.
	 *
	 * @param input The input string.
	 * @return
	 * 	<jk>true</jk> if the specified string matches this expression.
	 * 	<br>Always <jk>false</jk> if the string is <jk>null</jk>.
	 */
	public boolean matches(String input) {
		return input != null && exp.matches(input);
	}

	@Override /* Object */
	public String toString() {
		return exp.toString();
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

	private Exp parse(String expression) throws ParseException {
		if (StringUtils.isEmptyOrBlank(expression))
			return new Never();

		expression = expression.trim();

		List<Exp> ors = list();
		List<Exp> ands = list();

		StateMachineState state = S01;
		int i = 0, mark = -1;
		int pDepth = 0;
		boolean error = false;

		for (i = 0; i < expression.length(); i++) {
			char c = expression.charAt(i);
			if (state == S01) {
				// S01 = Looking for start
				if (! WS.contains(c)) {
					if (c == '(') {
						state = S02;
						pDepth = 0;
						mark = i+1;
					} else if (OP.contains(c)) {
						error = true;
						break;
					} else {
						state = S03;
						mark = i;
					}
				}
			} else if (state == S02) {
				// S02 = Found [(], looking for [)].
				if (c == '(')
					pDepth++;
				if (c == ')') {
					if (pDepth > 0)
						pDepth--;
					else {
						ands.add(parse(expression.substring(mark, i)));
						mark = -1;
						state = S04;
					}
				}
			} else if (state == S03) {
				// S03 = Found [A], looking for end of A.
				if (WS.contains(c) || OP.contains(c)) {
					ands.add(parseOperand(expression.substring(mark, i)));
					mark = -1;
					if (WS.contains(c)) {
						state = S04;
					} else {
						i--;
						state = S05;
					}
				}
			} else if (state == S04) {
				// S04 = Found [A ], looking for & or | or ,.
				if (! WS.contains(c)) {
					if (OP.contains(c)) {
						i--;
						state = S05;
					} else {
						error = true;
						break;
					}
				}
			} else if (state == S05) {
				// S05 = Found & or | or ,.
				if (c == '&') {
					//ands.add(operand);
					state = S06;
				} else /* (c == '|' || c == ',') */ {
					 if (ands.size() == 1) {
						 ors.add(ands.get(0));
					 } else {
						 ors.add(new And(ands));
					 }
					 ands.clear();
					 if (c == '|') {
						 state = S07;
					 } else {
						 state = S01;
					 }
				}
			} else if (state == S06) {
				// S06 = Found &, looking for & or other
				if (! WS.contains(c)) {
					if (c != '&')
						i--;
					state = S01;
				}
			} else /* (state == S07) */ {
				// S07 = Found |, looking for | or other
				if (! WS.contains(c)) {
					if (c != '|')
						i--;
					state = S01;
				}
			}
		}

		if (error)
			throw new ParseException("Invalid character in expression '"+expression+"' at position " + i + ". state=" + state, i);

		if (state == S01)
			throw new ParseException("Could not find beginning of clause in '"+expression+"'", i);
		if (state == S02)
			throw new ParseException("Could not find matching parenthesis in expression '"+expression+"'", i);
		if (state == S05 || state == S06 || state == S07)
			throw new ParseException("Dangling clause in expression '"+expression+"'", i);

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

	private static Exp parseOperand(String operand) {
		boolean hasMeta = false;
		for (int i = 0; i < operand.length() && ! hasMeta; i++) {
			char c = operand.charAt(i);
			hasMeta |= META.contains(c);
		}
		return hasMeta ? new Match(operand) : new Eq(operand);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Expression classes
	//-----------------------------------------------------------------------------------------------------------------

	abstract static class Exp {
		abstract boolean matches(String input);
		void appendTokens(Set<String> set) {}
	}

	static class Never extends Exp {
		@Override
		boolean matches(String input) {
			return false;
		}

		@Override /* Object */
		public String toString() {
			return "(NEVER)";
		}
	}

	static class And extends Exp {
		Exp[] clauses;

		And(List<Exp> clauses) {
			this.clauses = clauses.toArray(new Exp[clauses.size()]);
		}

		@Override /* Exp */
		boolean matches(String input) {
			for (Exp e : clauses)
				if (! e.matches(input))
					return false;
			return true;
		}

		@Override /* Exp */
		void appendTokens(Set<String> set) {
			for (Exp clause : clauses)
				clause.appendTokens(set);
		}

		@Override /* Object */
		public String toString() {
			return "(& " + StringUtils.join(clauses, " ") + ')';
		}
	}

	static class Or extends Exp {
		Exp[] clauses;

		Or(List<Exp> clauses) {
			this.clauses = clauses.toArray(new Exp[clauses.size()]);
		}

		@Override
		boolean matches(String input) {
			for (Exp e : clauses)
				if (e.matches(input))
					return true;
			return false;
		}

		@Override /* Exp */
		void appendTokens(Set<String> set) {
			for (Exp clause : clauses)
				clause.appendTokens(set);
		}

		@Override /* Object */
		public String toString() {
			return "(| " + StringUtils.join(clauses, " ") + ')';
		}
	}

	static class Eq extends Exp {
		final String operand;

		Eq(String operand) {
			this.operand = operand;
		}

		@Override /* Exp */
		boolean matches(String input) {
			return operand.equals(input);
		}

		@Override /* Exp */
		void appendTokens(Set<String> set) {
			set.add(operand);
		}

		@Override /* Object */
		public String toString() {
			return "[= " + operand + "]";
		}
	}

	static class Match extends Exp {
		final Pattern p;
		final String operand;

		Match(String operand) {
			this.operand = operand;
			p = StringUtils.getMatchPattern(operand);
		}

		@Override /* Exp */
		boolean matches(String input) {
			return p.matcher(input).matches();
		}

		@Override /* Exp */
		void appendTokens(Set<String> set) {
			set.add(operand);
		}

		@Override /* Object */
		public String toString() {
			return "[* " + p.pattern().replaceAll("\\\\[QE]", "") + "]";
		}
	}
}

