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

import static org.apache.juneau.internal.StateMachineState.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

/**
 * Number matcher factory for the {@link ObjectSearcher} class.
 *
 * <p>
 * 	The class provides searching based on the following patterns:
 * </p>
 * <ul>
 * 	<li><js>"property=1"</js> - A single number
 * 	<li><js>"property=1 2"</js> - Multiple OR'ed numbers
 * 	<li><js>"property=-1 -2"</js> - Multiple OR'ed negative numbers
 * 	<li><js>"property=1-2"</js>,<js>"property=-2--1"</js>  - A range of numbers (whitespace ignored)
 * 	<li><js>"property=1-2 4-5"</js> - Multiple OR'ed ranges
 * 	<li><js>"property=&lt;1"</js>,<js>"property=&lt;=1"</js>,<js>"property=&gt;1"</js>,<js>"property=&gt;=1"</js> - Open-ended ranges
 * 	<li><js>"property=!1"</js>,<js>"property=!1-2"</js> - Negation
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.ObjectTools">Overview &gt; juneau-marshall &gt; Object Tools</a>
 * </ul>
 */
public class NumberMatcherFactory extends MatcherFactory {

	/**
	 * Default reusable matcher.
	 */
	public static final NumberMatcherFactory DEFAULT = new NumberMatcherFactory();

	@Override
	public boolean canMatch(ClassMeta<?> cm) {
		return cm.isNumber();
	}

	@Override
	public AbstractMatcher create(String pattern) {
		return new NumberMatcher(pattern);
	}

	/**
	 * A construct representing a single search pattern.
	 */
	private static class NumberMatcher extends AbstractMatcher {
		NumberRange[] numberRanges;
		String pattern;
		private static final AsciiSet
			SNUM = AsciiSet.create("-0123456789."),
			NUM = AsciiSet.create("0123456789."),
			WS = AsciiSet.create(" \t");

		public NumberMatcher(String s) {

			s = s.trim();
			pattern = s;

			List<NumberRange> l = new LinkedList<>();

			// Possible patterns:
			// 123, >123, <123, >=123, <=123, >-123, >=-123, 123-456, -123--456, !123, !123-456, 123 - 456 (one token), 123 -456 (two separate tokens)

			// Possible states:
			// S01 = Looking for start (WS=S01, [!]=S01, [>]=S02, [<]=S03, SNUM=S06)
			// S02 = Found [>], looking for [=]/SNUM ([=]=S04, WS=S05, SNUM=S06)
			// S03 = Found [<], looking for [=]/SNUM ([=]=S05, WS=S05, SNUM=S06)
			// S04 = Found [=], looking for SNUN (WS=S05, SNUM=S06)
			// S05 = Found [... ], looking for SNUM (SNUM=S06)
			// S06 = Found [1], looking for [-]/WS (WS=S07, [-]=S08)
			// S07 = Found [123 ], looking for [-]/SNUM (if -, could be 123 - 456 or 123 -456) ([-]=S09, SNUM=S07)
			// S08 = Found [123-], looking for SNUM (Could be 123- 456 or 123-456) (SNUM=S11)
			// S09 = Found [123 -], looking for WS/SNUM (If WS, then it's 123 - 456, otherwise 123 -456) (WS=S10, SNUM=S06)
			// S10 = Found [123 - ], looking for SNUM (SNUM=S12)
			// S11 = Found [123 - 4], looking for WS (WS=S01)

			StateMachineState state = S01;
			int mark = 0;
			boolean isNot = false;
			Equality eq = Equality.NONE;
			Integer n1 = null, n2 = null;

			int i;
			for (i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (state == S01) {
					if (c == '!') {
						isNot = true;
					} else if (WS.contains(c)) {
						state = S01;
					} else if (c == '>') {
						state = S02;
						eq = Equality.GT;
					} else if (c == '<') {
						state = S03;
						eq = Equality.LT;
					} else if (SNUM.contains(c)) {
						state = S06;
						mark = i;
					} else {
						break;
					}
				} else if (state == S02) {
					if (c == '=') {
						state = S04;
						eq = Equality.GTE;
					} else if (WS.contains(c)) {
						state = S05;
					} else if (SNUM.contains(c)) {
						state = S06;
						mark = i;
					} else {
						break;
					}
				} else if (state == S03) {
					if (c == '=') {
						state = S04;
						eq = Equality.LTE;
					} else if (WS.contains(c)) {
						state = S05;
					} else if (SNUM.contains(c)) {
						state = S06;
						mark = i;
					} else {
						break;
					}
				} else if (state == S04) {
					if (WS.contains(c)) {
						state = S05;
					} else if (SNUM.contains(c)) {
						mark = i;
						state = S06;
					} else {
						break;
					}
				} else if (state == S05) {
					if (WS.contains(c)) {
						state = S05;
					} else if (SNUM.contains(c)) {
						state = S06;
						mark = i;
					} else {
						break;
					}
				} else if (state == S06) {
					if (NUM.contains(c)) {
						state = S06;
					} else if (WS.contains(c)) {
						state = S07;
						n1 = Integer.parseInt(s.substring(mark, i));
					} else if (c == '-') {
						state = S08;
						n1 = Integer.parseInt(s.substring(mark, i));
					} else {
						break;
					}
				} else if (state == S07) {
					if (WS.contains(c)) {
						state = S07;
					} else if (c == '-') {
						state = S09;
					} else if (SNUM.contains(c)) {
						state = S06;
						l.add(new NumberRange(eq, n1, isNot));
						eq = Equality.NONE;
						n1 = null;
						isNot = false;
						mark = i;
					} else {
						break;
					}
				} else if (state == S08) {
					if (WS.contains(c)) {
						state = S08;
					} else if (SNUM.contains(c)) {
						state = S11;
						mark = i;
					} else {
						break;
					}
				} else if (state == S09)  {
					if (WS.contains(c)) {
						state = S10;
					} else if (NUM.contains(c)) {
						state = S06;
						l.add(new NumberRange(eq, n1, isNot));
						eq = Equality.NONE;
						n1 = null;
						isNot = false;
						mark = i-1;
					} else {
						break;
					}
				} else if (state == S10) {
					if (WS.contains(c)) {
						state = S10;
					} else if (SNUM.contains(c)) {
						state = S11;
						mark = i;
					} else {
						break;
					}
				} else /* (state == S11) */ {
					if (SNUM.contains(c)) {
						state = S11;
					} else if (WS.contains(c)) {
						state = S01;
						n2 = Integer.parseInt(s.substring(mark, i));
						l.add(new NumberRange(eq, n1, n2, isNot));
						eq = Equality.NONE;
						n1 = n2 = null;
						isNot = false;
					} else {
						break;
					}
				}
			}

			if (i != s.length())
				throw new PatternException("Invalid range pattern ({0}): {1}", state, s);

			if (state == S01) {
				// No tokens found.
			} else if (state == S02 || state == S03 || state == S04 || state == S08 || state == S09) {
				throw new PatternException("Invalid range pattern (E{0}): {1}", state, s);
			} else if (state == S06) {
				n1 = Integer.parseInt(s.substring(mark).trim());
				l.add(new NumberRange(eq, n1, isNot));
			} else /* (state == S11) */ {
				n2 = Integer.parseInt(s.substring(mark).trim());
				l.add(new NumberRange(eq, n1, n2, isNot));
			}

			numberRanges = l.toArray(new NumberRange[l.size()]);
		}

		@Override /* Matcher */
		public boolean matches(ClassMeta<?> cm, Object o) {
			Number n = (Number)o;
			if (numberRanges.length == 0)
				return true;
			for (int i = 0; i < numberRanges.length; i++)
				if (numberRanges[i].matches(n))
					return true;
			return false;
		}

		@Override /* Object */
		public String toString() {
			return pattern;
		}
	}

	/**
	 * A construct representing a single search range in a single search pattern.
	 * All possible forms of search patterns are boiled down to these number ranges.
	 */
	private static class NumberRange {
		int start;
		int end;
		boolean isNot;

		public NumberRange(Equality eq, Integer num, boolean isNot) {
			this(eq, num, null, isNot);
		}

		public NumberRange(Equality eq, Integer start, Integer end, boolean isNot) {
			this.isNot = isNot;

			// 123, >123, <123, >=123, <=123, >-123, >=-123, 123-456, -123--456
			if (eq == Equality.NONE && end == null) { // 123
				this.start = start;
				this.end = this.start;
			} else if (eq == Equality.GT) {
				this.start = start+1;
				this.end = Integer.MAX_VALUE;
			} else if (eq == Equality.GTE) {
				this.start = start;
				this.end = Integer.MAX_VALUE;
			} else if (eq == Equality.LT) {
				this.start = Integer.MIN_VALUE;
				this.end = start-1;
			} else if (eq == Equality.LTE) {
				this.start = Integer.MIN_VALUE;
				this.end = start;
			} else {
				this.start = start;
				this.end = end;
			}
		}

		public boolean matches(Number n) {
			long i = n.longValue();
			boolean b = (i>=start && i<=end);
			if (isNot)
				b = !b;
			return b;
		}
	}
}
