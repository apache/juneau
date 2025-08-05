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
package org.apache.juneau.common.internal;

import static java.util.stream.Collectors.*;

import java.io.*;
import java.lang.reflect.*;

import static java.util.Optional.*;

import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

public class Utils {

	/** Constructor */
	protected Utils() {}

	public static final int hash(Object...values) {
		return Objects.hash(values);
	}

	public static final void printLines(String[] lines) {
		for (var i = 0; i < lines.length; i++)
			System.out.println(String.format("%4s:" + lines[i], i+1)); // NOSONAR - NOT DEBUG
	}

	/**
	 * Converts the string to lowercase if not null.
	 */
	public static String lc(String s) {
		return s == null ? null : s.toLowerCase();
	}

	/**
	 * Converts the string to uppercase if not null.
	 */
	public static String uc(String s) {
		return s == null ? null : s.toUpperCase();
	}

	/** Equals ignore-case */
	public static boolean eqic(Object a, Object b) {
		if (a == null && b == null) { return true; }
		if (a == null || b == null) { return false; }
		return Objects.equals(a.toString().toLowerCase(), b.toString().toLowerCase());
	}

	/** Not equals */
	public static <T> boolean ne(T s1, T s2) {
		return ! eq(s1, s2);
	}

	/** Equals */
	public static <T> boolean eq(T s1, T s2) {
		return Objects.equals(s1, s2);
	}

	/**
	 * Tests two objects for equality, gracefully handling nulls.
	 *
	 * Allows you to simplify object comparison without sacrificing efficiency.
	 *
	 * Example:
	 * <code>
	 * 	public boolean equals(Object o)
	 * 		return eq(this, (Role)o, (x,y)->eq(x.id,y.id) && eq(x.name,y.name) && eq(x.created,y.created) && eq(x.createdBy,y.createdBy));
	 * 	}
	 * </code>
	 *
	 * @param <T> Object 1 type.
	 * @param <U> Object 2 type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @param test The test to use for equality.
	 * @return <jk>true</jk> if both objects are equal based on the test.
	 */
	public static <T,U> boolean eq(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null) { return o2 == null; }
		if (o2 == null) { return false; }
		if (o1 == o2) { return true; }
		return test.test(o1, o2);
	}

	/**
	 * @return True if string is null or empty.
	 */
	public static boolean isEmpty(String o) {
		return o == null || o.isEmpty();
	}

	/**
	 * @return True if string is not null or empty.
	 */
	public static boolean isNotEmpty(String o) {
		return ! isEmpty(o);
	}

	/**
	 * Splits a comma-delimited list.
	 */
	public static String[] split(String s) {
		return s == null ? new String[0] : StringUtils.split(s);
	}

	/**
	 * Splits a comma-delimited list to a stream.
	 */
	public static Stream<String> splits(String s) {
		return Stream.of(isEmpty(s) ? new String[0] : split(s)).map(String::trim);
	}

	/**
	 * Splits a delimited list to a stream using the specified character as the delimiter.
	 */
	public static Stream<String> splits(String s, char delim) {
		return Stream.of(isEmpty(s) ? new String[0] : split(s, delim)).map(String::trim);
	}

	/**
	 * Splits a comma-delimited list.
	 */
	public static String[] split(String s, char delim) {
		return s == null ? new String[0] : StringUtils.split(s, delim);
	}

	/**
	 * Converts a comma-delimited string to a list.
	 * @return A new modifiable list.  Never null.
	 */
	public static List<String> cdlToList(String s) {
		return Stream.of(isEmpty(s) ? new String[0] : split(s)).map(String::trim).collect(toList());  // NOSONAR
	}

	/**
	 * Converts a comma-delimited string to a set.
	 * @return A new {@link LinkedHashSet}.  Never null.
	 */
	public static LinkedHashSet<String> cdlToSet(String s) {  // NOSONAR
		return Stream.of(isEmpty(s) ? new String[0] : split(s)).map(String::trim).collect(toCollection(LinkedHashSet::new));
	}

	/**
	 * @return A new {@link TreeSet} copy of the specified set, or null if the set was null.
	 */
	public static <T> TreeSet<T> treeSet(Set<T> copyFrom) {  // NOSONAR
		return copyFrom == null ? null : new TreeSet<>(copyFrom);
	}

	/**
	 * @return A new {@link TreeSet} of the specified values, never null.
	 */
	@SafeVarargs
	public static <T> TreeSet<T> treeSet(T...values) {  // NOSONAR
		return new TreeSet<>(Arrays.asList(values));
	}

	/**
	 * Create Calendar from...
	 *  - ISO date (2000-01-01T12:34:56Z)
	 *  - Duration (P1D)
	 *  - Year (2000).
	 *  - Short format (20000101).
	 */
	public static Calendar calendar(String isoDateOrDuration) throws IllegalArgumentException {
		try {
			var x = isoDateOrDuration.charAt(0);
			if (x == 'P' || x == '-') {
				var c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				var duration = Duration.parse(isoDateOrDuration).toMillis() / 1000;
				c.add(Calendar.SECOND, (int)duration);
				return c;
			}
			if (notContains(isoDateOrDuration, '-')) {
				if (isoDateOrDuration.length() == 4) isoDateOrDuration += "0101";
				Calendar c = new GregorianCalendar(TimeZone.getTimeZone("Z"));
				c.setTime(SIMPLIFIED_DATE.get().parse(isoDateOrDuration));
				return c;
			}
			if (notContains(isoDateOrDuration, 'T')) {
				isoDateOrDuration += "T00:00:00Z";
			}
			var zdt = ZonedDateTime.ofInstant(Instant.parse(isoDateOrDuration), ZoneId.of("Z"));
			return GregorianCalendar.from(zdt);
		} catch (DateTimeParseException | ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static final ThreadLocal<SimpleDateFormat> SIMPLIFIED_DATE = ThreadLocal.withInitial(Utils::newSimplifiedDate);  // NOSONAR
	private static SimpleDateFormat newSimplifiedDate() {
		var df = new SimpleDateFormat("yyyyMMdd");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		return df;
	}

	/**
	 * Add or Subtracts number of days from Calendar given as input
	 * @param c Date time
	 * @param days to add or subtract
	 * @return A cloned calendar with updated date as per days.
	 */
	public static Calendar addSubtractDays(Calendar c, int days) {
		return ofNullable(c)
			.map(x -> (Calendar)x.clone())
			.map(x -> add(x, Calendar.DATE, days))
			.orElse(null);
	}

	/**
	 * Same as {@link Integer#parseInt(String)} but removes any underscore characters.
	 */
	public static int parseInt(String value) {
		return Integer.parseInt(removeUnderscores(value));
	}

	/**
	 * Same as {@link Long#parseLong(String)} but removes any underscore characters.
	 */
	public static long parseLong(String value) {
		return Long.parseLong(removeUnderscores(value));
	}

	/**
	 * Same as {@link Float#parseFloat(String)} but removes any underscore characters.
	 */
	public static float parseFloat(String value) {
		return Float.parseFloat(removeUnderscores(value));
	}

	private static String removeUnderscores(String value) {
		if (value == null)
			throw new NullPointerException("Trying to parse null string.");
		return (notContains(value, '_') ? value : value.replace("_", ""));
	}

	/**
	 * Same as {@link Optional#ofNullable(Object)} but treats -1 as null.
	 */
	public static <T extends Number> Optional<T> optional(T value) {
		return Optional.ofNullable(value).filter(x -> x.intValue() >= 0);
	}

	/**
	 * Shortcut for creating a modifiable list out of an array of values.
	 */
	@SafeVarargs
	public static <T> ArrayList<T> list(T...values) {  // NOSONAR
		return new ArrayList<>(Arrays.asList(values));
	}

	/**
	 * Shortcut for creating a modifiable set out of an array of values.
	 */
	@SafeVarargs
	public static <T> LinkedHashSet<T> set(T...values) {  // NOSONAR
		return new LinkedHashSet<>(Arrays.asList(values));
	}

	/**
	 * Shortcut for appending values to a set.
	 */
	@SafeVarargs
	public static <T> Set<T> appendSet(Set<T> existing, T...values) {
		var existing2 = ofNullable(existing).orElse(new LinkedHashSet<>());
		Arrays.stream(values).forEach(existing2::add);
		return existing2;
	}

	/**
	 * Shortcut for creating a modifiable set out of an array of values.
	 */
	@SafeVarargs
	public static <K,V> LinkedHashMap<K,V> map(Object...values) {  // NOSONAR
		var m = new LinkedHashMap<K,V>();
		for (var i = 0; i < values.length; i+=2) {
			m.put((K)values[i], (V)values[i+1]);
		}
		return m;
	}

	/**
	 * Shortcut for adding a bunch of elements to a collection.
	 */
	@SafeVarargs
	public static <E, C extends Collection<E>> C addAll(C collection, E...elements) {
		Collections.addAll(collection, elements);
		return collection;
	}

	/**
	 * Shortcut for adding a bunch of elements to a set.
	 * If set is null, one will be created.
	 */
	@SafeVarargs
	public static <E> Set<E> append(Set<E> set, E...values) {
		return set == null ? set(values) : addAll(set, values);
	}

	/**
	 * Adds to a field of a calendar.
	 */
	public static Calendar add(Calendar c, int field, int amount) {
		c.add(field, amount);
		return c;
	}

	/**
	 * Converts a calendar to an ISO8601 string.
	 */
	public static Optional<ZonedDateTime> toZonedDateTime(Calendar c) {
		return ofNullable(c).map(GregorianCalendar.class::cast).map(GregorianCalendar::toZonedDateTime);
	}

	/**
	 * Combines values into a simple comma-delimited list.
	 */
	public static String join(String...values) {
		return StringUtils.join(values, ',');
	}

	/**
	 * Combines values into a simple comma-delimited list.
	 */
	public static String join(Collection<?> values) {
		return StringUtils.joine(new ArrayList<>(values), ',');
	}

	/**
	 * Calls {@link #toString()} on the specified object if it's not null.
	 *
	 * @param o The object to convert to a string.
	 * @return The object converted to a string, or <jk>null</jk> if the object was null.
	 */
	public static String stringify(Object o) {
		if (o instanceof Collection)
			return (String) Collection.class.cast(o).stream().map(Utils::stringify).collect(joining(",","[","]"));
		if (o instanceof Map)
			return (String) Map.class.cast(o).entrySet().stream().map(Utils::stringify).collect(joining(",","{","}"));
		if (o instanceof Map.Entry) {
			var e = Map.Entry.class.cast(o);
			return stringify(e.getKey()) + '=' + stringify(e.getValue());
		}
		if (o instanceof GregorianCalendar) {
			return GregorianCalendar.class.cast(o).toZonedDateTime().format(DateTimeFormatter.ISO_INSTANT);
		}
		if (o != null && o.getClass().isArray()) {
			List<Object> l = list();
			for (var i = 0; i < Array.getLength(o); i++) {
				l.add(Array.get(o, i));
			}
			return stringify(l);
		}
		return StringUtils.stringify(o);
	}

	/**
	 * Abbreviates a string if it's longer than the specified length.
	 *
	 * @param value The input value.  Can be null.
	 * @param length The max length.
	 * @return An abbreviated string.
	 */
	public static String abbreviate(String value, int length) {
		return StringUtils.abbreviate(value, length);
	}

	/**
	 * Searches through the cause chain of an exception to find the exception of the nested type.
	 * @param <T> The cause type.
	 * @param e The exception to search.
	 * @param cause The cause type.
	 * @return The cause of the specified type if it was found.
	 */
	public static <T extends Throwable> Optional<T> findCause(Throwable e, Class<T> cause) {
		while (e != null) {
			if (cause.isInstance(e)) { return Optional.of(cause.cast(e)); }
			e = e.getCause();
		}
		return Optional.empty();
	}

	/**
	 * Calulates a hash against a throwable stacktrace.
	 */
	public static int hash(Throwable t, String stopClass) {
		var i = 0;
		while (t != null) {
			for (StackTraceElement e : t.getStackTrace()) {
				if (e.getClassName().equals(stopClass))
					break;
				if (notContains(e.getClassName(), '$'))
					i = 31*i+e.hashCode();
			}
			t = t.getCause();
		}
		return i;
	}

	/**
	 * Returns true if this string can be parsed by StringUtils.parseNumber(String, Class).
	 */
	public static boolean isNumeric(String val) {
		return StringUtils.isNumeric(val);
	}

	/**
	 * Returns the first string in the array that's not null and not empty.
	 * Returns null if all values were empty/null.
	 */
	public static String coalesce(String...vals) {
		for (String v : vals) {
			if (isNotEmpty(v)) {
				return v;
			}
		}
		return null;
	}

	/**
	 * Returns the first object that's not null.
	 */
	@SafeVarargs
	public static <T> T coalesce(T...vals) {
		for (T v : vals) {
			if (v != null) {
				return v;
			}
		}
		return null;
	}

	/**
	 * Returns the specified string, or blank if that string is null.
	 */
	public static String emptyIfNull(String value) {
		return value == null ? "" : value;
	}

	/**
	 * Returns the specified string, or null if that string is null or empty.
	 */
	public static String nullIfEmpty(String value) {
		return isEmpty(value) ? null : value;
	}

	/**
	 * Splits a quoted comma-delimited list.
	 *
	 * Example:  "foo","bar","baz" => ['foo','bar','baz']
	 * Example:  "foo","bar,baz","qux" => ['foo','bar,baz','baz']
	 *
	 * Handles double-quoted or unquoted entries.
	 * Handles escaped characters.
	 *
	 * @param s The input string to split.
	 * @param result Where to place the split tokens.
	 * @return The same array as result.
	 */
	public static ArrayList<String> splitQcd(String s, ArrayList<String> result) {  // NOSONAR
		result.clear();
		if (s == null)
			return result;

		if (s.length() > 1 && s.charAt(0) == '\uFEFF') s = s.substring(1);  // Remove BOM if present.

		s = s.trim();

		int
			s1 = 1,  // Looking for start of token.
			s2 = 2,  // Found ", looking for end "
			s3 = 3,  // Found end ", looking for comma
			s4 = 4;  // Found non-whitespace, looking for comma.

		var state = s1;

		boolean isInEscape = false, needsUnescape = false;
		var mark = 0;

		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);

			if (state == s1) {
				if (c == '"') {
					state = s2;
					mark = i+1;
				} else if (c == ',') {
					result.add("");
				} else if (c != ' ' && c != '\t') {
					state = s4;
					mark = i;
				}
			} else if (state == s2) {
				if (c == '\\') {
					isInEscape = ! isInEscape;
					needsUnescape = true;
				} else if (! isInEscape) {
					if (c == '"') {
						var x = s.substring(mark, i);
						if (needsUnescape)  // NOSONAR
							x = StringUtils.unEscapeChars(x, QUOTE_ESCAPE_SET);
						result.add(x);
						state = s3;
						isInEscape = needsUnescape = false;
					}
				} else {
					isInEscape = false;
				}
			} else if (state == s3) {
				if (c == ',') {
					state = s1;
				}
			} else /* (state == S4) */ {
				if (c == '\\') {
					isInEscape = ! isInEscape;
					needsUnescape = true;
				} else if (! isInEscape) {
					if (c == ',') {
						var x = s.substring(mark, i);
						if (needsUnescape)  // NOSONAR
							x = StringUtils.unEscapeChars(x, COMMA_ESCAPE_SET);
						result.add(x.trim());
						state = s1;
						isInEscape = needsUnescape = false;
					}
				} else {
					isInEscape = false;
				}
			}
		}

		if (state == s1) {
			result.add("");
		} else if (state == s2) {
			throw new RuntimeException("Unmatched string quotes: " + s);
		} else if (state == s4) {
			var x = s.substring(mark);
			if (needsUnescape)
				x = StringUtils.unEscapeChars(x, COMMA_ESCAPE_SET);
			result.add(x.trim());
		}

		return result;
	}

	private static final AsciiSet QUOTE_ESCAPE_SET = AsciiSet.of("\"'\\");
	private static final AsciiSet COMMA_ESCAPE_SET = AsciiSet.of(",");


	/**
	 * Takes a supplier of anything and turns it into a Supplier<String>.
	 * Useful when passing arguments to the logger.
	 */
	public static Supplier<String> stringSupplier(Supplier<?> s) {
		return () -> Utils.stringify(s.get());
	}

	/**
	 * Throws an {@link AssertionError} if the specified actual value is not one of the expected values.
	 */
	@SafeVarargs
	public static final <T> T assertOneOf(T actual, T...expected) {
		for (T e : expected) {
			if (eq(actual,e)) return actual;
		}
		throw new AssertionError("Invalid value specified: " + actual);
	}

	/**
	 * Creates a {@link TreeSet} collector using the specified comparator.
	 */
	public static <T> Collector<T,?,TreeSet<T>> toTreeSet(Comparator<T> comparator) {
		return Collectors.toCollection(() -> new TreeSet<>(comparator));
	}

	/**
	 * Converts a string/object to a boolean.
	 */
	public static boolean b(Object val) {
		return ofNullable(val).map(Object::toString).map(Boolean::valueOf).orElse(false);
	}

	/**
	 * Used to wrap code that returns a value but throws an exception.
	 * Useful in cases where you're trying to execute code in a fluent method call
	 * or are trying to eliminate untestable catch blocks in code.
	 */
	public static <T> T safe(ThrowingSupplier<T> s) {
		try {
			return s.get();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Used to wrap code that returns a value but throws an exception.
	 * Useful in cases where you're trying to execute code in a fluent method call
	 * or are trying to eliminate untestable catch blocks in code.
	 */
	public static <T> T safe(ThrowingSupplier<T> s, ThrowableLogger logger) {
		try {
			return s.get();
		} catch (Exception e) {
			logger.log(e);
		}
		return null;
	}

	/**
	 * Returns true if the specified number is inclusively between the two values.
	 */
	public static boolean isBetween(int n, int lower, int higher) {
		return n >= lower && n <= higher;
	}

	/**
	 * Same as MessageFormat.format().
	 */
	public static String format(String pattern, Object...args) {
		if (notContains(pattern, "{"))
			return pattern;
		return MessageFormat.format(pattern, args);
	}

	/**
	 * Shortcut for converting an object to a string.
	 */
	public static String s(Object val) {
		return val == null ? null : val.toString();
	}

	private static final AsciiSet ESCAPE_SET = AsciiSet.of(",=");

	/**
	 * Shortcut for converting an object to a string.
	 */
	public static String readable(Object o) {
		if (o instanceof Collection)
			return (String) Collection.class.cast(o).stream().map(Utils::readable).collect(joining(",","[","]"));
		if (o instanceof Map)
			return (String) Map.class.cast(o).entrySet().stream().map(Utils::readable).collect(joining(",","{","}"));
		if (o instanceof Map.Entry) {
			var e = Map.Entry.class.cast(o);
			return readable(e.getKey()) + '=' + readable(e.getValue());
		}
		if (o instanceof GregorianCalendar) {
			return GregorianCalendar.class.cast(o).toZonedDateTime().format(DateTimeFormatter.ISO_INSTANT);
		}
		if (o instanceof Date) {
			return Date.class.cast(o).toInstant().toString();
		}
		if (o != null && o.getClass().isArray()) {
			List<Object> l = list();
			for (var i = 0; i < Array.getLength(o); i++) {
				l.add(Array.get(o, i));
			}
			return readable(l);
		}
		return StringUtils.stringify(o);
	}

	/**
	 * Shortcut for {@link #readable(Object)}
	 */
	public static String r(Object o) {
		return readable(o);
	}

	/**
	 * Prepends '\' to the beginning of ',' and '='.
	 */
	public static String escapeChars(String val) {
		return StringUtils.escapeChars(val, ESCAPE_SET);
	}

	/**
	 * Returns true if the specified string ends with any of the specified characters.
	 */
	public static boolean endsWith(String s, char...chars) {
		return StringUtils.endsWith(s, chars);
	}

	/**
	 * Returns the index of the first character in the list of characters.
	 */
	public static int indexOf(String s, char...chars) {
		return StringUtils.indexOf(s, chars);
	}

	/**
	 * Adds 'a' or 'an' to the beginning of a string.
	 */
	public static String articlized(String subject) {
		var p = Pattern.compile("^[AEIOUaeiou].*");
		return (p.matcher(subject).matches() ? "an " : "a ") + subject;
	}

	/**
	 * Returns true if the specified string is null or not numeric.
	 */
	public static boolean isNotNumeric(String s) {
		return ! isNumeric(s);
	}

	/**
	 * Null-safe {@link String#contains(CharSequence)} operation.
	 */
	public static boolean contains(String s, String...values) {
		if (s == null || values == null || values.length == 0)
			return false;
		for (String v : values) {
			if (s.contains(v))
				return true;
		}
		return false;
	}

	/**
	 * Null-safe {@link String#contains(CharSequence)} operation.
	 */
	public static boolean contains(String s, char...values) {
		if (s == null || values == null || values.length == 0)
			return false;
		for (char v : values) {
			if (s.indexOf(v) >= 0)
				return true;
		}
		return false;
	}
	/**
	 * Null-safe {@link String#contains(CharSequence)} operation.
	 */
	public static boolean notContains(String s, String...values) {
		return ! contains(s, values);
	}

	/**
	 * Null-safe {@link String#contains(CharSequence)} operation.
	 */
	public static boolean notContains(String s, char...values) {
		return ! contains(s, values);
	}

	/**
	 * Null-safe object comparison operation.
	 */
	public static int compare(Object o1, Object o2) {
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		if (o1 instanceof Comparable o1c && o2 instanceof Comparable o2c) {  // NOSONAR
			return o1c.compareTo(o2c);
		}
		return o1.toString().compareTo(o2.toString());
	}

	/**
	 * Convenience method for getting a stack trace as a string.
	 *
	 * @param t The throwable to get the stack trace from.
	 * @return The same content that would normally be rendered via <c>t.printStackTrace()</c>
	 */
	public static String getStackTrace(Throwable t) {
		var sw = new StringWriter();
		try (var pw = new PrintWriter(sw)) {
			t.printStackTrace(pw);
		}
		return sw.toString();
	}

	/**
	 * Returns an obfuscated version of the specified string.
	 */
	public static String obfuscate(String s) {
		if (s == null || s.length() < 2)
			return "*";
		return s.substring(0, 1) + s.substring(1).replaceAll(".", "*");  // NOSONAR
	}

	/**
	 * Returns a stream of strings from a comma-delimited string.
	 */
	public static List<String> cdl(String value) {
		return Arrays.asList(StringUtils.split(value));
	}

	/**
	 * Creates an {@link IllegalArgumentException}.
	 */
	public static IllegalArgumentException illegalArg(String msg, Object...args) {
		return new IllegalArgumentException(args.length == 0 ? msg : format(msg, args));
	}

	/**
	 * Creates a {@link RuntimeException}.
	 */
	public static RuntimeException runtimeException(String msg, Object...args) {
		return new RuntimeException(args.length == 0 ? msg : format(msg, args));
	}
}