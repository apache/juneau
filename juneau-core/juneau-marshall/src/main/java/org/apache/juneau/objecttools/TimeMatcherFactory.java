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
package org.apache.juneau.objecttools;

import static java.time.temporal.ChronoField.*;
import static org.apache.juneau.common.utils.StateEnum.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;

/**
 * Date/time matcher factory for the {@link ObjectSearcher} class.
 *
 * <p>
 * 	The class provides searching based on the following patterns:
 * </p>
 * <ul>
 * 	<li><js>"property=2011"</js> - A single year
 * 	<li><js>"property=2011 2013 2015"</js> - Multiple years
 * 	<li><js>"property=2011-01"</js> - A single month
 * 	<li><js>"property=2011-01-01"</js> - A single day
 * 	<li><js>"property=2011-01-01T12"</js> - A single hour
 * 	<li><js>"property=2011-01-01T12:30"</js> - A single minute
 * 	<li><js>"property=2011-01-01T12:30:45"</js> - A single second
 * 	<li><js>"property=&gt;2011"</js>,<js>"property=&gt;=2011"</js>,<js>"property=&lt;2011"</js>,<js>"property=&lt;=2011"</js> - Open-ended ranges
 * 	<li><js>"property=&gt;2011"</js>,<js>"property=&gt;=2011"</js>,<js>"property=&lt;2011"</js>,<js>"property=&lt;=2011"</js> - Open-ended ranges
 * 	<li><js>"property=2011 - 2013-06-30"</js> - Closed ranges
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ObjectTools">Object Tools</a>
 * </ul>
 */
public class TimeMatcherFactory extends MatcherFactory {

	/**
	 * A construct representing a single search pattern.
	 */
	private static class TimeMatcher extends AbstractMatcher {

		// @formatter:off
		private static final AsciiSet
			DT = AsciiSet.of("0123456789-:T./"),
			WS = AsciiSet.of(" \t");
		// @formatter:on

		TimestampRange[] ranges;
		List<TimestampRange> l = new LinkedList<>();

		public TimeMatcher(String s) {

			// Possible patterns:
			// >2000, <2000, >=2000, <=2000, > 2000, 2000 - 2001, '2000', >'2000', '2000'-'2001', '2000' - '2001'

			// Possible states:
			// S1 = Looking for [<]/[>]/quote/NUM ([>]=S2, [<]=S3, [']=S5, ["]=S6, NUM=S8)
			// S2 = Found [>], looking for [=]/quote/NUM ([=]=S4, [']=S5, ["]=S6, NUM=S8)
			// S3 = Found [<], looking for [=]/quote/NUM ([=]=S4, [']=S5, ["]=S6, NUM=S8)
			// S4 = Found [>=] or [<=], looking for quote/NUM ([']=S5, ["]=S6, NUM=S8)
			// S5 = Found ['], looking for ['] ([']=S1)
			// S6 = Found ["], looking for ["] (["]=S1)
			// S7 = Found [123"] or [123'], looking for WS (WS=S9)
			// S8 = Found [2], looking for WS (WS=S9)
			// S9 = Found [2000 ], looking for [-]/quote/NUM ([-]=S10, [']=S11, ["]=S12, NUM=S13)
			// S10 = Found [2000 -], looking for quote/NUM ([']=S11, ["]=S12, NUM=S13)
			// S11 = Found [2000 - '], looking for ['] ([']=S1)
			// S12 = Found [2000 - "], looking for ["] (["]=S1)
			// S13 = Found [2000 - 2], looking for WS (WS=S1)

			var state = S1;
			var mark = 0;
			var eq = Equality.NONE;
			var s1 = (String)null;
			var s2 = (String)null;

			int i;
			char c = 0;
			for (i = 0; i < s.trim().length(); i++) {
				c = s.charAt(i);
				if (state == S1) {
					// S1 = Looking for [>]/[<]/quote/NUM ([>]=S2, [<]=S3, [']=S5, ["]=S6, NUM=S8)
					if (WS.contains(c)) {
						state = S1;
					} else if (c == '>') {
						state = S2;
						eq = Equality.GT;
					} else if (c == '<') {
						state = S3;
						eq = Equality.LT;
					} else if (c == '\'') {
						state = S5;
						mark = i + 1;
					} else if (c == '"') {
						state = S6;
						mark = i + 1;
					} else if (DT.contains(c)) {
						state = S8;
						mark = i;
					} else {
						break;
					}
				} else if (state == S2) {
					// S2 = Found [>], looking for [=]/quote/NUM ([=]=S4, [']=S5, ["]=S6, NUM=S8)
					if (WS.contains(c)) {
						state = S2;
					} else if (c == '=') {
						state = S4;
						eq = Equality.GTE;
					} else if (c == '\'') {
						state = S5;
						mark = i + 1;
					} else if (c == '"') {
						state = S6;
						mark = i + 1;
					} else if (DT.contains(c)) {
						state = S8;
						mark = i;
					} else {
						break;
					}
				} else if (state == S3) {
					// S3 = Found [<], looking for [=]/quote/NUM ([=]=S4, [']=S5, ["]=S6, NUM=S8)
					if (WS.contains(c)) {
						state = S3;
					} else if (c == '=') {
						state = S4;
						eq = Equality.LTE;
					} else if (c == '\'') {
						state = S5;
						mark = i + 1;
					} else if (c == '"') {
						state = S6;
						mark = i + 1;
					} else if (DT.contains(c)) {
						state = S8;
						mark = i;
					} else {
						break;
					}
				} else if (state == S4) {
					// S4 = Found [>=] or [<=], looking for quote/NUM ([']=S5, ["]=S6, NUM=S8)
					if (WS.contains(c)) {
						state = S4;
					} else if (c == '\'') {
						state = S5;
						mark = i + 1;
					} else if (c == '"') {
						state = S6;
						mark = i + 1;
					} else if (DT.contains(c)) {
						state = S8;
						mark = i;
					} else {
						break;
					}
				} else if (state == S5) {
					// S5 = Found ['], looking for ['] ([']=S7)
					if (c == '\'') {
						state = S7;
						s1 = s.substring(mark, i);
					}
				} else if (state == S6) {
					// S6 = Found ["], looking for ["] (["]=S7)
					if (c == '"') {
						state = S7;
						s1 = s.substring(mark, i);
					}
				} else if (state == S7) {
					// S7 = Found [123"] or [123'], looking for WS (WS=S9)
					if (WS.contains(c)) {
						state = S9;
					} else if (c == '-') {
						state = S10;
					} else {
						break;
					}
				} else if (state == S8) {
					// S8 = Found [1], looking for WS (WS=S9)
					if (WS.contains(c)) {
						state = S9;
						s1 = s.substring(mark, i);
					}
				} else if (state == S9) {
					// S9 = Found [2000 ], looking for [-]/[>]/[<]/quote/NUM ([-]=S10, [>]=S2, [<]=S3, [']=S5, ["]=S6, NUM=S8)
					if (WS.contains(c)) {
						state = S9;
					} else if (c == '-') {
						state = S10;
					} else if (c == '>') {
						state = S2;
						l.add(new TimestampRange(eq, s1));
						eq = Equality.GT;
						s1 = null;
					} else if (c == '<') {
						state = S3;
						l.add(new TimestampRange(eq, s1));
						eq = Equality.LT;
						s1 = null;
					} else if (c == '\'') {
						state = S5;
						l.add(new TimestampRange(eq, s1));
						mark = i + 1;
						eq = null;
						s1 = null;
					} else if (c == '"') {
						state = S6;
						l.add(new TimestampRange(eq, s1));
						mark = i + 1;
						eq = null;
						s1 = null;
					} else if (DT.contains(c)) {
						state = S8;
						l.add(new TimestampRange(eq, s1));
						eq = null;
						s1 = null;
						mark = i;
					} else {
						break;
					}
				} else if (state == S10) {
					// S10 = Found [2000 -], looking for quote/NUM ([']=S11, ["]=S12, NUM=S13)
					if (WS.contains(c)) {
						state = S10;
					} else if (c == '\'') {
						state = S11;
						mark = i + 1;
					} else if (c == '"') {
						state = S12;
						mark = i + 1;
					} else if (DT.contains(c)) {
						state = S13;
						mark = i;
					} else {
						break;
					}
				} else if (state == S11) {
					// S11 = Found [2000 - '], looking for ['] ([']=S1)
					if (c == '\'') {
						state = S1;
						s2 = s.substring(mark, i);
						l.add(new TimestampRange(s1, s2));
						s1 = null;
						s2 = null;
					}
				} else if (state == S12) {
					// S12 = Found [2000 - "], looking for ["] (["]=S1)
					if (c == '"') {
						state = S1;
						s2 = s.substring(mark, i);
						l.add(new TimestampRange(s1, s2));
						s1 = null;
						s2 = null;
					}
				} else /* (state == S13) */ {
					// S13 = Found [2000 - 2], looking for WS (WS=S1)
					if (WS.contains(c)) {
						state = S1;
						s2 = s.substring(mark, i);
						l.add(new TimestampRange(s1, s2));
						s1 = null;
						s2 = null;
					}
				}
			}

			if (i != s.length())
				throw new PatternException("Invalid range pattern ({0}): pattern=[{1}], pos=[{2}], char=[{3}]", state, s, i, c);

			if (state == S1) {
				// No tokens found.
			} else if (state == S2 || state == S3 || state == S4 || state == S5 || state == S6 || state == S10 || state == S11 || state == S12) {
				throw new PatternException("Invalid range pattern (E{0}): {1}", state, s);
			} else if (state == S7) {
				l.add(new TimestampRange(eq, s1));
			} else if (state == S8) {
				s1 = s.substring(mark).trim();
				l.add(new TimestampRange(eq, s1));
			} else /* (state == S13) */ {
				s2 = s.substring(mark).trim();
				l.add(new TimestampRange(s1, s2));
			}

			ranges = l.toArray(new TimestampRange[l.size()]);
		}

		@Override
		public boolean matches(ClassMeta<?> cm, Object o) {
			if (ranges.length == 0)
				return true;

			ZonedDateTime zdt = null;
			if (cm.isCalendar()) {
				var c = (Calendar)o;
				zdt = c.toInstant().atZone(c.getTimeZone().toZoneId());
			} else {
				var date = (Date)o;
				zdt = date.toInstant().atZone(ZoneId.systemDefault());
			}
			for (var range : ranges)
				if (range.matches(zdt))
					return true;
			return false;
		}
	}

	/**
	 * A construct representing a single search range in a single search pattern.
	 * All possible forms of search patterns are boiled down to these timestamp ranges.
	 */
	private static class TimestampRange {
		ZonedDateTime start;
		ZonedDateTime end;

		public TimestampRange(Equality eq, String singleDate) {
			var singleDate1 = GranularZonedDateTime.parse(singleDate);
			if (eq == Equality.GT) {
				this.start = singleDate1.roll(1).roll(MILLI_OF_SECOND, -1).getZonedDateTime();
				this.end = Instant.ofEpochMilli(Long.MAX_VALUE).atZone(ZoneId.systemDefault());
			} else if (eq == Equality.LT) {
				this.start = Instant.ofEpochMilli(0).atZone(ZoneId.systemDefault());
				this.end = singleDate1.getZonedDateTime();
			} else if (eq == Equality.GTE) {
				this.start = singleDate1.roll(MILLI_OF_SECOND, -1).getZonedDateTime();
				this.end = Instant.ofEpochMilli(Long.MAX_VALUE).atZone(ZoneId.systemDefault());
			} else if (eq == Equality.LTE) {
				this.start = Instant.ofEpochMilli(0).atZone(ZoneId.systemDefault());
				this.end = singleDate1.roll(1).getZonedDateTime();
			} else {
				this.start = singleDate1.copy().roll(MILLI_OF_SECOND, -1).getZonedDateTime();
				this.end = singleDate1.roll(1).getZonedDateTime();
			}
		}

		public TimestampRange(String start, String end) {
			var start1 = GranularZonedDateTime.parse(start);
			var end1 = GranularZonedDateTime.parse(end);
			this.start = start1.copy().roll(MILLI_OF_SECOND, -1).getZonedDateTime();
			this.end = end1.roll(1).getZonedDateTime();
		}

		public boolean matches(ZonedDateTime zdt) {
			return zdt.isAfter(start) && zdt.isBefore(end);
		}
	}

	/**
	 * Default reusable matcher.
	 */
	public static final TimeMatcherFactory DEFAULT = new TimeMatcherFactory();

	/**
	 * Constructor.
	 */
	protected TimeMatcherFactory() {}

	@Override
	public boolean canMatch(ClassMeta<?> cm) {
		return cm.isDateOrCalendar();
	}

	@Override
	public AbstractMatcher create(String pattern) {
		return new TimeMatcher(pattern);
	}
}