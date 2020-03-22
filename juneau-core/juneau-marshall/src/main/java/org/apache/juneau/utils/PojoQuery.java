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

import static java.util.Calendar.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Designed to provide search/view/sort/paging filtering on tabular in-memory POJO models.
 *
 * <p>
 * It can also perform just view filtering on beans/maps.
 *
 * <p>
 * Examples of tabular POJO models:
 * <ul>
 * 	<li><tt>Collection{@code <Map>}</tt>
 * 	<li><tt>Collection{@code <Bean>}</tt>
 * 	<li><tt>Map[]</tt>
 * 	<li><tt>Bean[]</tt>
 * </ul>
 *
 * <p>
 * Tabular POJO models can be thought of as tables of data.  For example, a list of the following beans...
 * <p class='bcode w800'>
 * 	<jk>public</jk> MyBean {
 * 		<jk>public int</jk> myInt;
 * 		<jk>public</jk> String myString;
 * 		<jk>public</jk> Date myDate;
 * 	}
 * <p>
 * 	... can be thought of a table containing the following columns...
 * <p>
 * <table class='styled code'>
 * 	<tr><th>myInt</th><th>myString</th><th>myDate</th></tr>
 * 	<tr><td>123</td><td>'foobar'</td><td>yyyy/MM/dd HH:mm:ss</td></tr>
 * 	<tr><td colspan=3>...</td></tr>
 * </table>
 *
 * <p>
 * From this table, you can perform the following functions:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Search - Return only rows where a search pattern matches.
 * 	<li>
 * 		View - Return only the specified subset of columns in the specified order.
 * 	<li>
 * 		Sort - Sort the table by one or more columns.
 * 	<li>
 * 		Position/limit - Only return a subset of rows.
 * </ul>
 *
 * <h5 class='topic'>Search</h5>
 *
 * The search capabilities allow you to filter based on query patterns against strings, dates, and numbers.
 * Queries take the form of a Map with column names as keys, and search patterns as values.
 * <br>Multiple search patterns are ANDed (i.e. all patterns must match for the row to be returned).
 *
 * <h5 class='section'>Example:</h5>
 * <ul class='spaced-list'>
 * 	<li>
 * 		<tt>{myInt:'123'}</tt> - Return only rows where the <tt>myInt</tt> column is 123.
 * 	<li>
 * 		<tt>{myString:'foobar'}</tt> - Return only rows where the <tt>myString</tt> column is 'foobar'.
 * 	<li>
 * 		<tt>{myDate:'2001'}</tt> - Return only rows where the <tt>myDate</tt> column have dates in the year 2001.
 * </ul>
 *
 * <h5 class='topic'>String Patterns</h5>
 *
 * Any objects can be queried against using string patterns.
 * If the objects being searched are not strings, then the patterns are matched against whatever is return by the
 * {@code Object#toString()} method.
 *
 * <h5 class='topic'>Example string query patterns:</h5>
 * <ul>
 * 	<li><tt>foo</tt> - The string 'foo'
 * 	<li><tt>foo bar</tt> - The string 'foo' or the string 'bar'
 * 	<li><tt>'foo bar'</tt> - The phrase 'foo bar'
 * 	<li><tt>"foo bar"</tt> - The phrase 'foo bar'
 * 	<li><tt>foo*</tt> - <tt>*</tt> matches zero-or-more characters.
 * 	<li><tt>foo?</tt> - <tt>?</tt> matches exactly one character
 * </ul>
 *
 * <ul class='notes'>
 * 	<li>
 * 		Whitespace is ignored around search patterns.
 * 	<li>
 * 		Prepend <tt>+</tt> to tokens that must match.  (e.g. <tt>+foo* +*bar</tt>)
 * 	<li>
 * 		Prepend <tt>-</tt> to tokens that must not match.  (e.g. <tt>+foo* -*bar</tt>)
 * </ul>
 *
 * <h5 class='topic'>Numeric Patterns</h5>
 *
 * Any object of type {@link Number} (or numeric primitives) can be searched using numeric patterns.
 *
 * <h5 class='topic'>Example numeric query patterns:</h5>
 * <ul>
 * 	<li><tt>123</tt> - The single number 123
 * 	<li><tt>1 2 3</tt>	- 1, 2, or 3
 * 	<li><tt>1-100</tt> - Between 1 and 100
 * 	<li><tt>1 - 100</tt> - Between 1 and 100
 * 	<li><tt>1 - 100 200-300</tt> - Between 1 and 100 or between 200 and 300
 * 	<li><tt>&gt; 100</tt> - Greater than 100
 * 	<li><tt>&gt;= 100</tt> - Greater than or equal to 100
 * 	<li><tt>!123</tt> - Not 123
 * </ul>
 *
 * <ul class='notes'>
 * 	<li>
 * 		Whitespace is ignored in search patterns.
 * 	<li>
 * 		Negative numbers are supported.
 * </ul>
 *
 * <h5 class='topic'>Date Patterns</h5>
 *
 * Any object of type {@link Date} or {@link Calendar} can be searched using date patterns.
 *
 * <p>
 * The default valid input timestamp formats (which can be overridden via the {@link #setValidTimestampFormats(String...)}
 * method are...
 *
 * <ul>
 * 	<li><tt>yyyy.MM.dd.HH.mm.ss</tt>
 * 	<li><tt>yyyy.MM.dd.HH.mm</tt>
 * 	<li><tt>yyyy.MM.dd.HH</tt>
 * 	<li><tt>yyyy.MM.dd</tt>
 * 	<li><tt>yyyy.MM</tt>
 * 	<li><tt>yyyy</tt>
 * </ul>
 *
 * <h5 class='topic'>Example date query patterns:</h5>
 * <ul>
 * 	<li><tt>2001</tt> - A specific year.
 * 	<li><tt>2001.01.01.10.50</tt> - A specific time.
 * 	<li><tt>&gt;2001</tt>	- After a specific year.
 * 	<li><tt>&gt;=2001</tt> - During or after a specific year.
 * 	<li><tt>2001 - 2003.06.30</tt>	- A date range.
 * 	<li><tt>2001 2003 2005</tt>	- Multiple date patterns are ORed.
 * </ul>
 *
 * <ul class='notes'>
 * 	<li>
 * 		Whitespace is ignored in search patterns.
 * </ul>
 *
 * <h5 class='topic'>View</h5>
 *
 * The view capability allows you to return only the specified subset of columns in the specified order.
 * <br>The view parameter is a list of either <tt>Strings</tt> or <tt>Maps</tt>.
 *
 * <h5 class='topic'>Example view parameters:</h5>
 * <ul>
 * 	<li><tt>column1</tt> - Return only column 'column1'.
 * 	<li><tt>column2, column1</tt> - Return only columns 'column2' and 'column1' in that order.
 * </ul>
 *
 * <h5 class='topic'>Sort</h5>
 *
 * The sort capability allows you to sort values by the specified rows.
 * <br>The sort parameter is a list of strings with an optional <js>'+'</js> or <js>'-'</js> suffix representing
 * ascending and descending order accordingly.
 *
 * <h5 class='topic'>Example sort parameters:</h5>
 * <ul>
 * 	<li><tt>column1</tt> - Sort rows by column 'column1' ascending.
 * 	<li><tt>column1+</tt> - Sort rows by column 'column1' ascending.
 * 	<li><tt>column1-</tt> - Sort rows by column 'column1' descending.
 * 	<li><tt>column1, column2-</tt> - Sort rows by column 'column1' ascending, then 'column2' descending.
 * </ul>
 *
 * <h5 class='topic'>Paging</h5>
 *
 * Use the <tt>position</tt> and <tt>limit</tt> parameters to specify a subset of rows to return.
 */
@SuppressWarnings({"unchecked","rawtypes"})
public final class PojoQuery {

	private Object input;
	private ClassMeta type;
	private BeanSession session;

	/**
	 * Constructor.
	 *
	 * @param input The POJO we're going to be filtering.
	 * @param session The bean session to use to create bean maps for beans.
	 */
	public PojoQuery(Object input, BeanSession session) {
		this.input = input;
		this.type = session.getClassMetaForObject(input);
		this.session = session;
	}

	/**
	 * Filters the input object as a collection of maps.
	 *
	 * @param args The search arguments.
	 * @return The filtered collection.
	 * Returns the unaltered input if the input is not a collection or array of objects.
	 */
	public List filter(SearchArgs args) {

		if (input == null)
			return null;

		if (! type.isCollectionOrArray())
			throw new FormattedRuntimeException("Cannot call filterCollection() on class type ''{0}''", type);

		// Create a new ObjectList
		ObjectList l = (ObjectList)replaceWithMutables(input);

		// Do the search
		CollectionFilter filter = new CollectionFilter(args.getSearch(), args.isIgnoreCase());
		filter.doQuery(l);

		// If sort or view isn't empty, then we need to make sure that all entries in the
		// list are maps.
		Map<String,Boolean> sort = args.getSort();
		List<String> view = args.getView();

		if ((! sort.isEmpty()) || (! view.isEmpty())) {
			if (! sort.isEmpty())
				doSort(l, sort);
			if (! view.isEmpty())
				doView(l, view);
		}

		// Do the paging.
		int pos = args.getPosition();
		int limit = args.getLimit();
		if (pos != 0 || limit != 0) {
			int end = (limit == 0 || limit+pos >= l.size()) ? l.size() : limit + pos;
			pos = Math.min(pos, l.size());
			ObjectList l2 = new DelegateList(((DelegateList)l).getClassMeta());
			l2.addAll(l.subList(pos, end));
			l = l2;
		}

		return l;
	}

	/*
	 * If there are any non-Maps in the specified list, replaces them with BeanMaps.
	 */
	private Object replaceWithMutables(Object o) {
		if (o == null)
			return null;
		ClassMeta cm = session.getClassMetaForObject(o);
		if (cm.isCollection()) {
			ObjectList l = new DelegateList(session.getClassMetaForObject(o));
			for (Object o2 : (Collection)o)
				l.add(replaceWithMutables(o2));
			return l;
		}
		if (cm.isMap() && o instanceof BeanMap) {
			BeanMap bm = (BeanMap)o;
			DelegateBeanMap dbm = new DelegateBeanMap(bm.getBean(), session);
			for (Object key : bm.keySet())
				dbm.addKey(key.toString());
			return dbm;
		}
		if (cm.isBean()) {
			BeanMap bm = session.toBeanMap(o);
			DelegateBeanMap dbm = new DelegateBeanMap(bm.getBean(), session);
			for (Object key : bm.keySet())
				dbm.addKey(key.toString());
			return dbm;
		}
		if (cm.isMap()) {
			Map m = (Map)o;
			DelegateMap dm = new DelegateMap(m, session);
			for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
				dm.put(e.getKey().toString(), e.getValue());
			return dm;
		}
		if (cm.isArray()) {
			return replaceWithMutables(Arrays.asList((Object[])o));
		}
		return o;
	}

	/*
	 * Sorts the specified list by the sort list.
	 */
	private static void doSort(List list, Map<String,Boolean> sortList) {

		// We reverse the list and sort last to first.
		List<String> columns = new ArrayList<>(sortList.keySet());
		Collections.reverse(columns);

		for (final String c : columns) {
			final boolean isDesc = sortList.get(c);

			Comparator comp = new Comparator<Map>() {
				@Override /* Comparator */
				public int compare(Map m1, Map m2) {
					Comparable v1 = toComparable(m1.get(c)), v2 = toComparable(m2.get(c));
					if (v1 == null && v2 == null)
						return 0;
					if (v1 == null)
						return (isDesc ? -1 : 1);
					if (v2 == null)
						return (isDesc ? 1 : -1);
					return (isDesc ? v2.compareTo(v1) : v1.compareTo(v2));
				}
			};
			Collections.sort(list, comp);
		}
	}

	static final Comparable toComparable(Object o) {
		if (o == null)
			return null;
		if (o instanceof Comparable)
			return (Comparable)o;
		if (o instanceof Map)
			return ((Map)o).size();
		if (o.getClass().isArray())
			return Array.getLength(o);
		return o.toString();
	}

	/*
	 * Filters all but the specified view columns on all entries in the specified list.
	 */
	private static void doView(List list, List<String> view) {
		for (ListIterator i = list.listIterator(); i.hasNext();) {
			Object o = i.next();
			Map m = (Map)o;
			doView(m, view);
		}
	}

	/*
	 * Creates a new Map with only the entries specified in the view list.
	 */
	private static Map doView(Map m, List<String> view) {
		if (m instanceof DelegateMap)
			((DelegateMap)m).filterKeys(view);
		else
			((DelegateBeanMap)m).filterKeys(view);
		return m;
	}


	//====================================================================================================
	// CollectionFilter
	//====================================================================================================
	private class CollectionFilter {
		IMatcher entryMatcher;

		public CollectionFilter(Map query, boolean ignoreCase) {
			if (query != null && ! query.isEmpty())
				entryMatcher = new MapMatcher(query, ignoreCase);
		}

		public void doQuery(List in) {
			if (in == null || entryMatcher == null)
				return;
			for (Iterator i = in.iterator(); i.hasNext();) {
				Object o = i.next();
				if (! entryMatcher.matches(o))
					i.remove();
			}
		}
	}

	//====================================================================================================
	// IMatcher
	//====================================================================================================
	private interface IMatcher<E> {
		public boolean matches(E o);
	}

	//====================================================================================================
	// MapMatcher
	//====================================================================================================
	/*
	 * Matches on a Map only if all specified entry matchers match.
	 */
	private class MapMatcher implements IMatcher<Map> {

		Map<String,IMatcher> entryMatchers = new HashMap<>();

		public MapMatcher(Map query, boolean ignoreCase) {
			for (Map.Entry e : (Set<Map.Entry>)query.entrySet())
				if (e.getKey() != null && e.getValue() != null)
					entryMatchers.put(e.getKey().toString(), new ObjectMatcher(e.getValue().toString(), ignoreCase));
		}

		@Override /* IMatcher */
		public boolean matches(Map m) {
			if (m == null)
				return false;
			for (Map.Entry<String,IMatcher> e : entryMatchers.entrySet()) {
				String key = e.getKey();
				Object val = null;
				if (m instanceof BeanMap) {
					val = ((BeanMap)m).getRaw(key);
				} else {
					val = m.get(key);
				}
				if (! e.getValue().matches(val))
					return false;
			}
			return true;
		}
	}

	//====================================================================================================
	// ObjectMatcher
	//====================================================================================================
	/*
	 * Matcher that uses the correct matcher based on object type.
	 * Used for objects when we can't determine the object type beforehand.
	 */
	private class ObjectMatcher implements IMatcher<Object> {

		String searchPattern;
		boolean ignoreCase;
		DateMatcher dateMatcher;
		NumberMatcher numberMatcher;
		StringMatcher stringMatcher;

		ObjectMatcher(String searchPattern, boolean ignoreCase) {
			this.searchPattern = searchPattern;
			this.ignoreCase = ignoreCase;
		}

		@Override /* IMatcher */
		public boolean matches(Object o) {
			if (o instanceof Collection) {
				for (Object o2 : (Collection)o)
					if (matches(o2))
						return true;
				return false;
			}
			if (o != null && o.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(o); i++)
					if (matches(Array.get(o, i)))
						return true;
				return false;
			}
			if (o instanceof Map) {
				for (Object o2 : ((Map)o).values())
					if (matches(o2))
						return true;
				return false;
			}
			if (o instanceof Number)
				return getNumberMatcher().matches(o);
			if (o instanceof Date || o instanceof Calendar)
				return getDateMatcher().matches(o);
			return getStringMatcher().matches(o);
		}

		private IMatcher getNumberMatcher() {
			if (numberMatcher == null)
				numberMatcher = new NumberMatcher(searchPattern);
			return numberMatcher;
		}

		private IMatcher getStringMatcher() {
			if (stringMatcher == null)
				stringMatcher = new StringMatcher(searchPattern, ignoreCase);
			return stringMatcher;
		}

		private IMatcher getDateMatcher() {
			if (dateMatcher == null)
				dateMatcher = new DateMatcher(searchPattern);
			return dateMatcher;
		}
	}

	//====================================================================================================
	// NumberMatcher
	//====================================================================================================
	private static class NumberMatcher implements IMatcher<Number> {

		private NumberPattern[] numberPatterns;

		/**
		 * Construct a number matcher for the given search pattern.
		 *
		 * @param searchPattern A date search paattern.  See class usage for a description.
		 */
		public NumberMatcher(String searchPattern) {
			numberPatterns = new NumberPattern[1];
			numberPatterns[0] = new NumberPattern(searchPattern);

		}

		/**
		 * Returns 'true' if this integer matches the pattern(s).
		 */
		@Override /* IMatcher */
		public boolean matches(Number in) {
			for (int i = 0; i < numberPatterns.length; i++) {
				if (! numberPatterns[i].matches(in))
					return false;
			}
			return true;
		}

	}

	/**
	 * A construct representing a single search pattern.
	 */
	private static class NumberPattern {
		NumberRange[] numberRanges;

		public NumberPattern(String searchPattern) {

			List<NumberRange> l = new LinkedList<>();

			for (String s : breakUpTokens(searchPattern)) {
				boolean isNot = (s.charAt(0) == '!');
				String token = s.substring(1);
				Pattern p = Pattern.compile("(([<>]=?)?)(-?\\d+)(-?(-?\\d+)?)");

				// Possible patterns:
				// 123, >123, <123, >=123, <=123, >-123, >=-123, 123-456, -123--456
				// Regular expression used:  (([<>]=?)?)(-?\d+)(-??(-?\d+))
				Matcher m = p.matcher(token);

				// If a non-numeric value was passed in for a numeric value, just set the value to '0'.
				// (I think this might resolve a workaround in custom queries).
				if (! m.matches())
					throw new FormattedRuntimeException("Numeric value didn't match pattern:  ''{0}''", token);
					//m = numericPattern.matcher("0");

				String arg1 = m.group(1);
				String start = m.group(3);
				String end = m.group(5);

				l.add(new NumberRange(arg1, start, end, isNot));
			}

			numberRanges = l.toArray(new NumberRange[l.size()]);
		}

		private static List<String> breakUpTokens(String s) {
			// Get rid of whitespace in "123 - 456"
			s = s.replaceAll("(-?\\d+)\\s*-\\s*(-?\\d+)", "$1-$2");
			// Get rid of whitespace in ">= 123"
			s = s.replaceAll("([<>]=?)\\s+(-?\\d+)", "$1$2");
			// Get rid of whitespace in "! 123"
			s = s.replaceAll("(!)\\s+(-?\\d+)", "$1$2");

			// Replace all commas with whitespace
			// Allows for alternate notation of: 123,456...
			s = s.replaceAll(",", " ");

			String[] s2 = s.split("\\s+");

			// Make all tokens 'ORed'.  There is no way to AND numeric tokens.
			for (int i = 0; i < s2.length; i++)
				if (! startsWith(s2[i], '!'))
					s2[i] = "^"+s2[i];

			return AList.of(s2);
		}

		public boolean matches(Number number) {
			if (numberRanges.length == 0) return true;
			for (int i = 0; i < numberRanges.length; i++)
				if (numberRanges[i].matches(number))
					return true;
			return false;
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

		public NumberRange(String arg, String start, String end, boolean isNot) {

			this.isNot = isNot;

			// 123, >123, <123, >=123, <=123, >-123, >=-123, 123-456, -123--456
			if (arg.equals("") && end == null) { // 123
				this.start = Integer.parseInt(start);
				this.end = this.start;
			} else if (arg.equals(">")) {
				this.start = Integer.parseInt(start)+1;
				this.end = Integer.MAX_VALUE;
			} else if (arg.equals(">=")) {
				this.start = Integer.parseInt(start);
				this.end = Integer.MAX_VALUE;
			} else if (arg.equals("<")) {
				this.start = Integer.MIN_VALUE;
				this.end = Integer.parseInt(start)-1;
			} else if (arg.equals("<=")) {
				this.start = Integer.MIN_VALUE;
				this.end = Integer.parseInt(start);
			} else {
				this.start = Integer.parseInt(start);
				this.end = Integer.parseInt(end);
			}
		}

		public boolean matches(Number n) {
			long i = n.longValue();
			boolean b = (i>=start && i<=end);
			if (isNot) b = !b;
			return b;
		}
	}

	//====================================================================================================
	// DateMatcher
	//====================================================================================================
	/** The list of all valid timestamp formats */
	private SimpleDateFormat[] validTimestampFormats = new SimpleDateFormat[0];
	{
		setValidTimestampFormats("yyyy.MM.dd.HH.mm.ss","yyyy.MM.dd.HH.mm","yyyy.MM.dd.HH","yyyy.MM.dd","yyyy.MM","yyyy");
	}

	/**
	 * Use this method to override the allowed search patterns when used in locales where time formats are different.
	 *
	 * @param s A comma-delimited list of valid time formats.
	 */
	public void setValidTimestampFormats(String...s) {
		validTimestampFormats = new SimpleDateFormat[s.length];
		for (int i = 0; i < s.length; i++)
			validTimestampFormats[i] = new SimpleDateFormat(s[i]);
	}

	private class DateMatcher implements IMatcher<Object> {

		private TimestampPattern[] patterns;

		/**
		 * Construct a timestamp matcher for the given search pattern.
		 *
		 * @param searchPattern The search pattern.
		 */
		DateMatcher(String searchPattern) {
			patterns = new TimestampPattern[1];
			patterns[0] = new TimestampPattern(searchPattern);

		}

		/**
		 * Returns <jk>true</jk> if the specified date matches the pattern passed in through the constructor.
		 *
		 * <p>
		 * <br>The Object can be of type {@link Date} or {@link Calendar}.
		 * <br>Always returns <jk>false</jk> on <jk>null</jk> input.
		 */
		@Override /* IMatcher */
		public boolean matches(Object in) {
			if (in == null) return false;

			Calendar c = null;
			if (in instanceof Calendar)
				c = (Calendar)in;
			else if (in instanceof Date) {
				c = Calendar.getInstance();
				c.setTime((Date)in);
			} else {
				return false;
			}
			for (int i = 0; i < patterns.length; i++) {
				if (! patterns[i].matches(c))
					return false;
			}
			return true;
		}
	}

	/**
	 * A construct representing a single search pattern.
	 */
	private class TimestampPattern {
		TimestampRange[] ranges;
		List<TimestampRange> l = new LinkedList<>();

		public TimestampPattern(String s) {

			// Handle special case where timestamp is enclosed in quotes.
			// This can occur on hyperlinks created by group-by queries.
			// e.g. '2007/01/29 04:17:43 PM'
			if (s.charAt(0) == '\'' && s.charAt(s.length()-1) == '\'')
				s = s.substring(1, s.length()-1);

			// Pattern for finding <,>,<=,>=
			Pattern p1 = Pattern.compile("^\\s*([<>](?:=)?)\\s*(\\S+.*)$");
			// Pattern for finding range dash (e.g. xxx - yyy)
			Pattern p2 = Pattern.compile("^(\\s*-\\s*)(\\S+.*)$");

			// States are...
			// 1 - Looking for <,>,<=,>=
			// 2 - Looking for single date.
			// 3 - Looking for start date.
			// 4 - Looking for -
			// 5 - Looking for end date.
			int state = 1;

			String op = null;
			CalendarP startDate = null;

			ParsePosition pp = new ParsePosition(0);
			Matcher m = null;
			String seg = s;

			while (! seg.equals("") || state != 1) {
				if (state == 1) {
					m = p1.matcher(seg);
					if (m.matches()) {
						op = m.group(1);
						seg = m.group(2);
						state = 2;
					} else {
						state = 3;
					}
				} else if (state == 2) {
					l.add(new TimestampRange(op, parseDate(seg, pp)));
					//tokens.add("^"+op + parseTimestamp(seg, pp));
					seg = seg.substring(pp.getIndex()).trim();
					pp.setIndex(0);
					state = 1;
				} else if (state == 3) {
					startDate = parseDate(seg, pp);
					seg = seg.substring(pp.getIndex()).trim();
					pp.setIndex(0);
					state = 4;
				} else if (state == 4) {
					// Look for '-'
					m = p2.matcher(seg);
					if (m.matches()) {
						state = 5;
						seg = m.group(2);
					} else {
						// This is a single date (e.g. 2002/01/01)
						l.add(new TimestampRange(startDate));
						state = 1;
					}
				} else if (state == 5) {
					l.add(new TimestampRange(startDate, parseDate(seg, pp)));
					seg = seg.substring(pp.getIndex()).trim();
					pp.setIndex(0);
					state = 1;
				}
			}

			ranges = l.toArray(new TimestampRange[l.size()]);
		}

		public boolean matches(Calendar c) {
			if (ranges.length == 0) return true;
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

		public TimestampRange(CalendarP start, CalendarP end) {
			this.start = start.copy().roll(MILLISECOND, -1).getCalendar();
			this.end = end.roll(1).getCalendar();
		}

		public TimestampRange(CalendarP singleDate) {
			this.start = singleDate.copy().roll(MILLISECOND, -1).getCalendar();
			this.end = singleDate.roll(1).getCalendar();
		}

		public TimestampRange(String op, CalendarP singleDate) {
			if (op.equals(">")) {
				this.start = singleDate.roll(1).roll(MILLISECOND, -1).getCalendar();
				this.end = new CalendarP(new Date(Long.MAX_VALUE), 0).getCalendar();
			} else if (op.equals("<")) {
				this.start = new CalendarP(new Date(0), 0).getCalendar();
				this.end = singleDate.getCalendar();
			} else if (op.equals(">=")) {
				this.start = singleDate.roll(MILLISECOND, -1).getCalendar();
				this.end = new CalendarP(new Date(Long.MAX_VALUE), 0).getCalendar();
			} else if (op.equals("<=")) {
				this.start = new CalendarP(new Date(0), 0).getCalendar();
				this.end = singleDate.roll(1).getCalendar();
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
	CalendarP parseDate(String seg, ParsePosition pp) {

		CalendarP cal = null;

		for (int i = 0; i < validTimestampFormats.length && cal == null; i++) {
			pp.setIndex(0);
			SimpleDateFormat f = validTimestampFormats[i];
			Date d = f.parse(seg, pp);
			int idx = pp.getIndex();
			if (idx != 0) {
				// it only counts if the next character is '-', 'space', or end-of-string.
				char c = (seg.length() == idx ? 0 : seg.charAt(idx));
				if (c == 0 || c == '-' || Character.isWhitespace(c))
					cal = new CalendarP(d, getPrecisionField(f.toPattern()));
			}
		}

		if (cal == null)
			throw new FormattedRuntimeException("Invalid date encountered:  ''{0}''", seg);

		return cal;
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

	//====================================================================================================
	// StringMatcher
	//====================================================================================================
	private static class StringMatcher implements IMatcher<Object> {

		private SearchPattern[] searchPatterns;

		/**
		 * Construct a string matcher for the given search pattern.
		 *
		 * @param searchPattern The search pattern.  See class usage for details.
		 * @param ignoreCase If <jk>true</jk>, use case-insensitive matching.
		 */
		public StringMatcher(String searchPattern, boolean ignoreCase) {
			this.searchPatterns = new SearchPattern[1];
			this.searchPatterns[0] = new SearchPattern(searchPattern, ignoreCase);
		}

		/**
		 * Returns 'true' if this string matches the pattern(s).
		 * Always returns false on null input.
		 */
		@Override /* IMatcher */
		public boolean matches(Object in) {
			if (in == null) return false;
			for (int i = 0; i < searchPatterns.length; i++) {
				if (! searchPatterns[i].matches(in.toString()))
					return false;
			}
			return true;
		}

	}
	/**
	 * A construct representing a single search pattern.
	 */
	private static class SearchPattern {
		Pattern[] orPatterns, andPatterns, notPatterns;

		public SearchPattern(String searchPattern, boolean ignoreCase) {

			List<Pattern> ors = new LinkedList<>();
			List<Pattern> ands = new LinkedList<>();
			List<Pattern> nots = new LinkedList<>();

			for (String arg : breakUpTokens(searchPattern)) {
				char prefix = arg.charAt(0);
				String token = arg.substring(1);

				token = token.replaceAll("([\\?\\*\\+\\\\\\[\\]\\{\\}\\(\\)\\^\\$\\.])", "\\\\$1");
				token = token.replace("\u9997", ".*");
				token = token.replace("\u9996", ".?");

				if (! token.startsWith(".*"))
					token = "^" + token;
				if (! token.endsWith(".*"))
					token = token + "$";

				int flags = Pattern.DOTALL;
				if (ignoreCase)
					flags |= Pattern.CASE_INSENSITIVE;

				Pattern p = Pattern.compile(token, flags);

				if (prefix == '^')
					ors.add(p);
				else if (prefix == '+')
					ands.add(p);
				else if (prefix == '-')
					nots.add(p);
			}
			orPatterns = ors.toArray(new Pattern[ors.size()]);
			andPatterns = ands.toArray(new Pattern[ands.size()]);
			notPatterns = nots.toArray(new Pattern[nots.size()]);
		}

		/**
		 * Break up search pattern into separate tokens.
		 */
		private static List<String> breakUpTokens(String s) {

			// If the string is null or all whitespace, return an empty vector.
			if (s == null || s.trim().length() == 0)
				return Collections.emptyList();

			// Pad with spaces.
			s = " " + s + " ";

			// Replace instances of [+] and [-] inside single and double quotes with
			// \u2001 and \u2002 for later replacement.
			int escapeCount = 0;
			boolean inSingleQuote = false;
			boolean inDoubleQuote = false;
			char[] ca = s.toCharArray();
			for (int i = 0; i < ca.length; i++) {
				if (ca[i] == '\\') escapeCount++;
				else if (escapeCount % 2 == 0) {
					if (ca[i] == '\'') inSingleQuote = ! inSingleQuote;
					else if (ca[i] == '"') inDoubleQuote = ! inDoubleQuote;
					else if (ca[i] == '+' && (inSingleQuote || inDoubleQuote)) ca[i] = '\u9999';
					else if (ca[i] == '-' && (inSingleQuote || inDoubleQuote)) ca[i] = '\u9998';
				}
				if (ca[i] != '\\') escapeCount = 0;
			}
			s = new String(ca);

			// Remove spaces between '+' or '-' and the keyword.
			//s = perl5Util.substitute("s/([\\+\\-])\\s+/$1/g", s);
			s = s.replaceAll("([\\+\\-])\\s+", "$1");

			// Replace:  [*]->[\u3001] as placeholder for '%', ignore escaped.
			s = replace(s, '*', '\u9997', true);
			// Replace:  [?]->[\u3002] as placeholder for '_', ignore escaped.
			s = replace(s, '?', '\u9996', true);
			// Replace:  [\*]->[*], [\?]->[?]
			s = unEscapeChars(s, new char[]{'*','?'});

			// Remove spaces
			s = s.trim();

			// Re-replace the [+] and [-] characters inside quotes.
			s = s.replace('\u9999', '+');
			s = s.replace('\u9998', '-');

			String[] sa = splitQuoted(s, ' ');
			List<String> l = new ArrayList<>(sa.length);
			int numOrs = 0;
			for (int i = 0; i < sa.length; i++) {
				String token = sa[i];
				int len = token.length();
				if (len > 0) {
					char c = token.charAt(0);
					String s2 = null;
					if ((c == '+' || c == '-') && len > 1)
						s2 = token.substring(1);
					else {
						s2 = token;
						c = '^';
						numOrs++;
					}
					// Trim off leading and trailing single and double quotes.
					if (s2.matches("\".*\"") || s2.matches("'.*'"))
						s2 = s2.substring(1, s2.length()-1);

					// Replace:  [\"]->["]
					s2 = unEscapeChars(s2, new char[]{'"','\''});

					// Un-escape remaining escaped backslashes.
					s2 = unEscapeChars(s2, new char[]{'\\'});

					l.add(c + s2);
				}
			}

			// If there's a single OR clause, turn it into an AND clause (makes the SQL cleaner).
			if (numOrs == 1) {
				int ii = l.size();
				for (int i = 0; i < ii; i++) {
					String x = l.get(i);
					if (x.charAt(0) == '^')
						l.set(i, '+'+x.substring(1));
				}
			}
			return l;
		}

		public boolean matches(String input) {
			if (input == null) return false;
			for (int i = 0; i < andPatterns.length; i++)
				if (! andPatterns[i].matcher(input).matches())
					return false;
			for (int i = 0; i < notPatterns.length; i++)
				if (notPatterns[i].matcher(input).matches())
					return false;
			for (int i = 0; i < orPatterns.length; i++)
				if (orPatterns[i].matcher(input).matches())
					return true;
			return orPatterns.length == 0;
		}

	}

	/*
	 * Same as split(String, char), but does not split on characters inside
	 * single quotes.
	 * Does not split on escaped delimiters, and escaped quotes are also ignored.
	 * Example:
	 * split("a,b,c",',') -> {"a","b","c"}
	 * split("a,'b,b,b',c",',') -> {"a","'b,b,b'","c"}
	 */
	static final String[] splitQuoted(String s, char c) {

		if (s == null || s.matches("\\s*"))
			return new String[0];

		List<String> l = new LinkedList<>();
		char[] sArray = s.toCharArray();
		int x1 = 0;
		int escapeCount = 0;
		boolean inSingleQuote = false;
		boolean inDoubleQuote = false;
		for (int i = 0; i < sArray.length; i++) {
			if (sArray[i] == '\\') escapeCount++;
			else if (escapeCount % 2 == 0) {
				if (sArray[i] == '\'' && ! inDoubleQuote) inSingleQuote = ! inSingleQuote;
				else if (sArray[i] == '"' && ! inSingleQuote) inDoubleQuote = ! inDoubleQuote;
				else if (sArray[i] == c && ! inSingleQuote && ! inDoubleQuote) {
					String s2 = new String(sArray, x1, i-x1).trim();
					l.add(s2);
					x1 = i+1;
				}
			}
			if (sArray[i] != '\\') escapeCount = 0;
		}
		String s2 = new String(sArray, x1, sArray.length-x1).trim();
		l.add(s2);

		return l.toArray(new String[l.size()]);
	}

	/**
	 * Replaces tokens in a string with a different token.
	 *
	 * <p>
	 * replace("A and B and C", "and", "or") -> "A or B or C"
	 * replace("andandand", "and", "or") -> "ororor"
	 * replace(null, "and", "or") -> null
	 * replace("andandand", null, "or") -> "andandand"
	 * replace("andandand", "", "or") -> "andandand"
	 * replace("A and B and C", "and", null) -> "A  B  C"
	 * @param ignoreEscapedChars Specify 'true' if escaped 'from' characters should be ignored.
	 */
	static String replace(String s, char from, char to, boolean ignoreEscapedChars) {
		if (s == null) return null;

		char[] sArray = s.toCharArray();

		int escapeCount = 0;
		int singleQuoteCount = 0;
		int doubleQuoteCount = 0;
		for (int i = 0; i < sArray.length; i++) {
			char c = sArray[i];
			if (c == '\\' && ignoreEscapedChars)
				escapeCount++;
			else if (escapeCount % 2 == 0) {
				if (c == from && singleQuoteCount % 2 == 0 && doubleQuoteCount % 2 == 0)
				sArray[i] = to;
			}
			if (sArray[i] != '\\') escapeCount = 0;
		}
		return new String(sArray);
	}

	/**
	 * Removes escape characters (specified by escapeChar) from the specified characters.
	 */
	static String unEscapeChars(String s, char[] toEscape) {
		char escapeChar = '\\';
		if (s == null) return null;
		if (s.length() == 0) return s;
		StringBuffer sb = new StringBuffer(s.length());
		char[] sArray = s.toCharArray();
		for (int i = 0; i < sArray.length; i++) {
			char c = sArray[i];

			if (c == escapeChar) {
				if (i+1 != sArray.length) {
					char c2 = sArray[i+1];
					boolean isOneOf = false;
					for (int j = 0; j < toEscape.length && ! isOneOf; j++)
						isOneOf = (c2 == toEscape[j]);
					if (isOneOf) {
						i++;
					} else if (c2 == escapeChar) {
						sb.append(escapeChar);
						i++;
					}
				}
			}
			sb.append(sArray[i]);
		}
		return sb.toString();
	}
}


