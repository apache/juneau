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

import static java.util.Calendar.*;
import static org.apache.juneau.internal.StateMachineState.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

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
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.ObjectTools">Overview &gt; juneau-marshall &gt; Object Tools</a>
 * </ul>
 */
public class TimeMatcherFactory extends MatcherFactory {

	/**
	 * Default reusable matcher.
	 */
	public static final TimeMatcherFactory DEFAULT = new TimeMatcherFactory();

	private final SimpleDateFormat[] formats;

	/**
	 * Constructor.
	 */
	protected TimeMatcherFactory() {
		this.formats = getTimestampFormats();
	}

	/**
	 * TODO
	 *
	 * @return TODO
	 */
	protected SimpleDateFormat[] getTimestampFormats() {
		String[] s = getTimestampFormatStrings();
		SimpleDateFormat[] a = new SimpleDateFormat[s.length];
		for (int i = 0; i < s.length; i++)
			a[i] = new SimpleDateFormat(s[i]);
		return a;
	}

	/**
	 * TODO
	 *
	 * @return TODO
	 */
	protected String[] getTimestampFormatStrings() {
		return new String[]{
			"yyyy-MM-dd'T'HH:mm:ss",
			"yyyy-MM-dd'T'HH:mm",
			"yyyy-MM-dd'T'HH",
			"yyyy-MM-dd",
			"yyyy-MM",
			"yyyy"
		};
	}

	@Override
	public boolean canMatch(ClassMeta<?> cm) {
		return cm.isDateOrCalendar();
	}

	@Override
	public AbstractMatcher create(String pattern) {
		return new TimeMatcher(formats, pattern);
	}

	/**
	 * A construct representing a single search pattern.
	 */
	private static class TimeMatcher extends AbstractMatcher {

		private static final AsciiSet
			DT = AsciiSet.create("0123456789-:T./"),
			WS = AsciiSet.create(" \t");

		TimestampRange[] ranges;
		List<TimestampRange> l = new LinkedList<>();

		public TimeMatcher(SimpleDateFormat[] f, String s) {

			// Possible patterns:
			// >2000, <2000, >=2000, <=2000, > 2000, 2000 - 2001, '2000', >'2000', '2000'-'2001', '2000' - '2001'

			// Possible states:
			// S01 = Looking for [<]/[>]/quote/NUM ([>]=S02, [<]=S03, [']=S05, ["]=S06, NUM=S08)
			// S02 = Found [>], looking for [=]/quote/NUM ([=]=S04, [']=S05, ["]=S06, NUM=S08)
			// S03 = Found [<], looking for [=]/quote/NUM ([=]=S04, [']=S05, ["]=S06, NUM=S08)
			// S04 = Found [>=] or [<=], looking for quote/NUM ([']=S05, ["]=S06, NUM=S08)
			// S05 = Found ['], looking for ['] ([']=S01)
			// S06 = Found ["], looking for ["] (["]=S01)
			// S07 = Found [123"] or [123'], looking for WS (WS=S09)
			// S08 = Found [2], looking for WS (WS=S09)
			// S09 = Found [2000 ], looking for [-]/quote/NUM ([-]=S10, [']=S11, ["]=S12, NUM=S13)
			// S10 = Found [2000 -], looking for quote/NUM ([']=S11, ["]=S12, NUM=S13)
			// S11 = Found [2000 - '], looking for ['] ([']=S01)
			// S12 = Found [2000 - "], looking for ["] (["]=S01)
			// S13 = Found [2000 - 2], looking for WS (WS=S01)

			StateMachineState state = S01;
			int mark = 0;
			Equality eq = Equality.NONE;
			String s1 = null, s2 = null;

			int i;
			char c = 0;
			for (i = 0; i < s.trim().length(); i++) {
				c = s.charAt(i);
				if (state == S01) {
					// S01 = Looking for [>]/[<]/quote/NUM ([>]=S02, [<]=S03, [']=S05, ["]=S06, NUM=S08)
					if (WS.contains(c)) {
						state = S01;
					} else if (c == '>') {
						state = S02;
						eq = Equality.GT;
					} else if (c == '<') {
						state = S03;
						eq = Equality.LT;
					} else if (c == '\'') {
						state = S05;
						mark = i+1;
					} else if (c == '"') {
						state = S06;
						mark = i+1;
					} else if (DT.contains(c)) {
						state = S08;
						mark = i;
					} else {
						break;
					}
				} else if (state == S02) {
					// S02 = Found [>], looking for [=]/quote/NUM ([=]=S04, [']=S05, ["]=S06, NUM=S08)
					if (WS.contains(c)) {
						state = S02;
					} else if (c == '=') {
						state = S04;
						eq = Equality.GTE;
					} else if (c == '\'') {
						state = S05;
						mark = i+1;
					} else if (c == '"') {
						state = S06;
						mark = i+1;
					} else if (DT.contains(c)) {
						state = S08;
						mark = i;
					} else {
						break;
					}
				} else if (state == S03) {
					// S03 = Found [<], looking for [=]/quote/NUM ([=]=S04, [']=S05, ["]=S06, NUM=S08)
					if (WS.contains(c)) {
						state = S03;
					} else if (c == '=') {
						state = S04;
						eq = Equality.LTE;
					} else if (c == '\'') {
						state = S05;
						mark = i+1;
					} else if (c == '"') {
						state = S06;
						mark = i+1;
					} else if (DT.contains(c)) {
						state = S08;
						mark = i;
					} else {
						break;
					}
				} else if (state == S04) {
					// S04 = Found [>=] or [<=], looking for quote/NUM ([']=S05, ["]=S06, NUM=S08)
					if (WS.contains(c)) {
						state = S04;
					} else if (c == '\'') {
						state = S05;
						mark = i+1;
					} else if (c == '"') {
						state = S06;
						mark = i+1;
					} else if (DT.contains(c)) {
						state = S08;
						mark = i;
					} else {
						break;
					}
				} else if (state == S05) {
					// S05 = Found ['], looking for ['] ([']=S07)
					if (c == '\'') {
						state = S07;
						s1 = s.substring(mark, i);
					}
				} else if (state == S06) {
					// S06 = Found ["], looking for ["] (["]=S07)
					if (c == '"') {
						state = S07;
						s1 = s.substring(mark, i);
					}
				} else if (state == S07) {
					// S07 = Found [123"] or [123'], looking for WS (WS=S09)
					if (WS.contains(c)) {
						state = S09;
					} else if (c == '-') {
						state = S10;
					} else {
						break;
					}
				} else if (state == S08) {
					// S08 = Found [1], looking for WS (WS=S09)
					if (WS.contains(c)) {
						state = S09;
						s1 = s.substring(mark, i);
					}
				} else if (state == S09) {
					// S09 = Found [2000 ], looking for [-]/[>]/[<]/quote/NUM ([-]=S10, [>]=S02, [<]=S03, [']=S05, ["]=S06, NUM=S08)
					if (WS.contains(c)) {
						state = S09;
					} else if (c == '-') {
						state = S10;
					} else if (c == '>') {
						state = S02;
						l.add(new TimestampRange(f, eq, s1));
						eq = Equality.GT;
						s1 = null;
					} else if (c == '<') {
						state = S03;
						l.add(new TimestampRange(f, eq, s1));
						eq = Equality.LT;
						s1 = null;
					} else if (c == '\'') {
						state = S05;
						l.add(new TimestampRange(f, eq, s1));
						mark = i+1;
						eq = null;
						s1 = null;
					} else if (c == '"') {
						state = S06;
						l.add(new TimestampRange(f, eq, s1));
						mark = i+1;
						eq = null;
						s1 = null;
					} else if (DT.contains(c)) {
						state = S08;
						l.add(new TimestampRange(f, eq, s1));
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
						mark = i+1;
					} else if (c == '"') {
						state = S12;
						mark = i+1;
					} else if (DT.contains(c)) {
						state = S13;
						mark = i;
					} else {
						break;
					}
				} else if (state == S11) {
					// S11 = Found [2000 - '], looking for ['] ([']=S01)
					if (c == '\'') {
						state = S01;
						s2 = s.substring(mark, i);
						l.add(new TimestampRange(f, s1, s2));
						s1 = null;
						s2 = null;
					}
				} else if (state == S12) {
					// S12 = Found [2000 - "], looking for ["] (["]=S01)
					if (c == '"') {
						state = S01;
						s2 = s.substring(mark, i);
						l.add(new TimestampRange(f, s1, s2));
						s1 = null;
						s2 = null;
					}
				} else /* (state == S13) */ {
					// S13 = Found [2000 - 2], looking for WS (WS=S01)
					if (WS.contains(c)) {
						state = S01;
						s2 = s.substring(mark, i);
						l.add(new TimestampRange(f, s1, s2));
						s1 = null;
						s2 = null;
					}
				}
			}

			if (i != s.length())
				throw new PatternException("Invalid range pattern ({0}): pattern=[{1}], pos=[{2}], char=[{3}]", state, s, i, c);

			if (state == S01) {
				// No tokens found.
			} else if (state == S02 || state == S03 || state == S04 || state == S05 || state == S06 || state == S10 || state == S11 || state == S12) {
				throw new PatternException("Invalid range pattern (E{0}): {1}", state, s);
			} else if (state == S07) {
				l.add(new TimestampRange(f, eq, s1));
			} else if (state == S08) {
				s1 = s.substring(mark).trim();
				l.add(new TimestampRange(f, eq, s1));
			} else /* (state == S13) */ {
				s2 = s.substring(mark).trim();
				l.add(new TimestampRange(f, s1, s2));
			}

			ranges = l.toArray(new TimestampRange[l.size()]);
		}

		@Override
		public boolean matches(ClassMeta<?> cm, Object o) {
			if (ranges.length == 0)
				return true;

			Calendar c = null;
			if (cm.isCalendar())
				c = (Calendar)o;
			else {
				c = Calendar.getInstance();
				c.setTime((Date)o);
			}
			for (int i = 0; i < ranges.length; i++)
				if (ranges[i].matches(c))
					return true;
			return false;
		}
	}

	/**
	 * A construct representing a single search range in a single search pattern.
	 * All possible forms of search patterns are boiled down to these timestamp ranges.
	 */
	private static class TimestampRange {
		Calendar start;
		Calendar end;

		public TimestampRange(SimpleDateFormat[] formats, String start, String end) {
			CalendarP start1 = parseDate(formats, start);
			CalendarP end1 = parseDate(formats, end);
			this.start = start1.copy().roll(MILLISECOND, -1).getCalendar();
			this.end = end1.roll(1).getCalendar();
		}

		public TimestampRange(SimpleDateFormat[] formats, Equality eq, String singleDate) {
			CalendarP singleDate1 = parseDate(formats, singleDate);
			if (eq == Equality.GT) {
				this.start = singleDate1.roll(1).roll(MILLISECOND, -1).getCalendar();
				this.end = new CalendarP(new Date(Long.MAX_VALUE), 0).getCalendar();
			} else if (eq == Equality.LT) {
				this.start = new CalendarP(new Date(0), 0).getCalendar();
				this.end = singleDate1.getCalendar();
			} else if (eq == Equality.GTE) {
				this.start = singleDate1.roll(MILLISECOND, -1).getCalendar();
				this.end = new CalendarP(new Date(Long.MAX_VALUE), 0).getCalendar();
			} else if (eq == Equality.LTE) {
				this.start = new CalendarP(new Date(0), 0).getCalendar();
				this.end = singleDate1.roll(1).getCalendar();
			} else {
				this.start = singleDate1.copy().roll(MILLISECOND, -1).getCalendar();
				this.end = singleDate1.roll(1).getCalendar();
			}
		}

		public boolean matches(Calendar c) {
			boolean b = (c.after(start) && c.before(end));
			return b;
		}
	}

	private static int getPrecisionField(String pattern) {
		if (pattern.indexOf('s') != -1)
			return SECOND;
		if (pattern.indexOf('m') != -1)
			return MINUTE;
		if (pattern.indexOf('H') != -1)
			return HOUR_OF_DAY;
		if (pattern.indexOf('d') != -1)
			return DAY_OF_MONTH;
		if (pattern.indexOf('M') != -1)
			return MONTH;
		if (pattern.indexOf('y') != -1)
			return YEAR;
		return Calendar.MILLISECOND;
	}

	/**
	 * Parses a timestamp string off the beginning of the string segment 'seg'.
	 * Goes through each possible valid timestamp format until it finds a match.
	 * The position where the parsing left off is stored in pp.
	 *
	 * @param seg The string segment being parsed.
	 * @param pp Where parsing last left off.
	 * @return An object representing a timestamp.
	 */
	static CalendarP parseDate(SimpleDateFormat[] formats, String seg) {
		ParsePosition pp = new ParsePosition(0);
		for (int i = 0; i < formats.length; i++) {
			SimpleDateFormat f = formats[i];
			Date d = f.parse(seg, pp);
			int idx = pp.getIndex();
			if (idx != 0) {
				// it only counts if the next character is '-', 'space', or end-of-string.
				char c = (seg.length() == idx ? 0 : seg.charAt(idx));
				if (c == 0 || c == '-' || Character.isWhitespace(c))
					return new CalendarP(d, getPrecisionField(f.toPattern()));
			}
		}

		throw new BasicRuntimeException("Invalid date encountered:  ''{0}''", seg);
	}

	/**
	 * Combines a Calendar with a precision identifier.
	 */
	private static class CalendarP {
		public Calendar c;
		public int precision;

		public CalendarP(Date date, int precision) {
			c = Calendar.getInstance();
			c.setTime(date);
			this.precision = precision;
		}

		public CalendarP copy() {
			return new CalendarP(c.getTime(), precision);
		}

		public CalendarP roll(int field, int amount) {
			c.add(field, amount);
			return this;
		}

		public CalendarP roll(int amount) {
			return roll(precision, amount);
		}

		public Calendar getCalendar() {
			return c;
		}
	}
}
