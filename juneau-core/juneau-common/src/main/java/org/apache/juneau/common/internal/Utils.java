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
import static org.apache.juneau.common.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.text.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.common.internal.ThrowableUtils.*;
import org.apache.juneau.common.utils.*;


public class Utils {

	private static final Map<Class<?>,Function<String,?>> ENV_FUNCTIONS = new IdentityHashMap<>();

	static {
		ENV_FUNCTIONS.put(Boolean.class, Boolean::valueOf);
		ENV_FUNCTIONS.put(Charset.class, Charset::forName);
	}

	private static final ConcurrentHashMap<String,String> PROPERTY_TO_ENV = new ConcurrentHashMap<>();

	/** Constructor - This class is meant to be subclasses. */
	protected Utils() {}

	/**
	 * Creates an array of objects.
	 */
	@SafeVarargs
	public static <T> T[] a(T...x) {
		return x;
	}

	/**
	 * Traverses all elements in the specified object and accumulates them into a list.
	 *
	 * @param <T> The element type.
	 * @param o The object to traverse.
	 * @param c The consumer of the objects.
	 */
	public static <T> List<T> accumulate(Object o) {
		var l = list();
		traverse(o, l::add);
		return (List<T>) l;
	}

	/**
	 * Shortcut for creating an unmodifiable list out of an array of values.
	 */
	@SafeVarargs
	public static <T> List<T> alist(T...values) {  // NOSONAR
		return values == null ? null : Arrays.asList(values);
	}

	/**
	 * Converts the specified collection to an array.
	 *
	 * @param <E> The element type.
	 * @param value The collection to convert.
	 * @param componentType The component type of the array.
	 * @return A new array.
	 */
	public static <E> E[] array(Collection<E> value, Class<E> componentType) {
		if (value == null)
			return null;
		E[] array = (E[])Array.newInstance(componentType, value.size());
		return value.toArray(array);
	}

	/**
	 * Casts an object to a specific type if it's an instance of that type.
	 *
	 * @param <T> The type to cast to.
	 * @param c The type to cast to.
	 * @param o The object to cast to.
	 * @return The cast object, or <jk>null</jk> if the object wasn't the specified type.
	 */
	public static <T> T cast(Class<T> c, Object o) {
		return o != null && c.isInstance(o) ? c.cast(o) : null;
	}

	/**
	 * If the specified object is an instance of the specified class, casts it to that type.
	 *
	 * @param <T> The class to cast to.
	 * @param o The object to cast.
	 * @param c The class to cast to.
	 * @return The cast object, or <jk>null</jk> if the object wasn't an instance of the specified class.
	 */
	public static <T> T castOrNull(Object o, Class<T> c) {
		if (c.isInstance(o))
			return c.cast(o);
		return null;
	}

	/**
	 * Compares two objects for equality.
	 *
	 * <p>
	 * Nulls are always considered less-than unless both are null.
	 *
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return
	 * 	<c>-1</c>, <c>0</c>, or <c>1</c> if <c>o1</c> is less-than, equal, or greater-than <c>o2</c>.
	 *	<br><c>0</c> if objects are not of the same type or do not implement the {@link Comparable} interface.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int compare(Object o1, Object o2) {
		if (o1 == null) {
			if (o2 == null)
				return 0;
			return -1;
		} else if (o2 == null) {
			return 1;
		}

		if (o1.getClass() == o2.getClass() && o1 instanceof Comparable)
			return ((Comparable)o1).compareTo(o2);

		return 0;
	}

	/**
	 * Null-safe {@link String#contains(CharSequence)} operation.
	 */
	public static boolean contains(String s, char...values) {
		if (s == null || values == null || values.length == 0)
			return false;
		for (var v : values) {
			if (s.indexOf(v) >= 0)
				return true;
		}
		return false;
	}

	/**
	 * Null-safe {@link String#contains(CharSequence)} operation.
	 */
	public static boolean contains(String s, String...values) {
		if (s == null || values == null || values.length == 0)
			return false;
		for (var v : values) {
			if (s.contains(v))
				return true;
		}
		return false;
	}

	/**
	 * Returns an empty {@link Optional}.
	 *
	 * @param <T> The component type.
	 * @return An empty {@link Optional}.
	 */
	public static <T> Optional<T> empty() {
		return Optional.empty();
	}

	/**
	 * Returns the specified string, or blank if that string is null.
	 */
	public static String emptyIfNull(Object value) {
		return value == null ? "" : value.toString();
	}

	/**
	 * Looks up a system property or environment variable.
	 *
	 * <p>
	 * First looks in system properties.  Then converts the name to env-safe and looks in the system environment.
	 * Then returns the default if it can't be found.
	 *
	 * @param name The property name.
	 * @return The value if found.
	 */
	public static Optional<String> env(String name) {
		var s = System.getProperty(name);
		if (s == null)
			s = System.getenv(envName(name));
		return opt(s);
	}

	/**
	 * Looks up a system property or environment variable.
	 *
	 * <p>
	 * First looks in system properties.  Then converts the name to env-safe and looks in the system environment.
	 * Then returns the default if it can't be found.
	 *
	 * @param <T> The type to convert the value to.
	 * @param name The property name.
	 * @param def The default value if not found.
	 * @return The default value.
	 */
	public static <T> T env(String name, T def) {
		return env(name).map(x -> toType(x, def)).orElse(def);
	}

	private static String envName(String name) {
		return PROPERTY_TO_ENV.computeIfAbsent(name, x->x.toUpperCase().replace(".", "_"));
	}

	/**
	 * Tests two strings for equality, but gracefully handles nulls.
	 *
	 * @param caseInsensitive Use case-insensitive matching.
	 * @param s1 String 1.
	 * @param s2 String 2.
	 * @return <jk>true</jk> if the strings are equal.
	 */
	public static boolean eq(boolean caseInsensitive, String s1, String s2) {
		return caseInsensitive ? eqic(s1, s2) : eq(s1, s2);
	}

	/**
	 * Tests two objects for equality, gracefully handling nulls and arrays.
	 *
	 * @param <T> The value types.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>true</jk> if both objects are equal based on the {@link Object#equals(Object)} method.
	 */
	public static <T> boolean eq(T o1, T o2) {
		if (isArray(o1) && isArray(o2)) {
			int l1 = Array.getLength(o1), l2 = Array.getLength(o2);
			if (l1 != l2)
				return false;
			for (int i = 0; i < l1; i++)
				if (! eq(Array.get(o1, i), Array.get(o2, i)))
					return false;
			return true;
		}
		return Objects.equals(o1, o2);
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
	 * Tests two strings for case-insensitive equality, but gracefully handles nulls.
	 *
	 * @param s1 String 1.
	 * @param s2 String 2.
	 * @return <jk>true</jk> if the strings are equal.
	 */
	public static boolean eqic(String s1, String s2) {
		if (s1 == null)
			return s2 == null;
		if (s2 == null)
			return false;
		return s1.equalsIgnoreCase(s2);
	}

	/**
	 * Same as MessageFormat.format().
	 */
	public static String f(String pattern, Object...args) {
		if (args.length == 0)
			return pattern;
		return MessageFormat.format(pattern, args);
	}

	/**
	 * Returns the first non-null value in the specified array
	 *
	 * @param <T> The value types.
	 * @param t The values to check.
	 * @return The first non-null value, or <jk>null</jk> if the array is null or empty or contains only <jk>null</jk> values.
	 */
	@SafeVarargs
	public static <T> T firstNonNull(T... t) {
		if (t != null)
			for (T tt : t)
				if (tt != null)
					return tt;
		return null;
	}

	/**
	 * Simplified formatted string supplier with message arguments.
	 */
	public static Supplier<String> fs(String pattern, Object...args) {
		return ()->StringUtils.format(pattern, args);
	}

	/**
	 * Converts a string containing <js>"*"</js> meta characters with a regular expression pattern.
	 *
	 * @param s The string to create a pattern from.
	 * @return A regular expression pattern.
	 */
	public static Pattern getMatchPattern3(String s) {
		return getMatchPattern3(s, 0);
	}

	/**
	 * Converts a string containing <js>"*"</js> meta characters with a regular expression pattern.
	 *
	 * @param s The string to create a pattern from.
	 * @param flags Regular expression flags.
	 * @return A regular expression pattern.
	 */
	public static Pattern getMatchPattern3(String s, int flags) {
		if (s == null)
			return null;
		var sb = new StringBuilder();
		sb.append("\\Q");
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '*')
				sb.append("\\E").append(".*").append("\\Q");
			else if (c == '?')
				sb.append("\\E").append(".").append("\\Q");
			else
				sb.append(c);
		}
		sb.append("\\E");
		return Pattern.compile(sb.toString(), flags);
	}

	/**
	 * Shortcut for calling {@link Objects#hash(Object...)}.
	 */
	public static final int hash(Object...values) {
		return Objects.hash(values);
	}

	/**
	 * Creates an {@link IllegalArgumentException}.
	 */
	public static IllegalArgumentException illegalArg(String msg, Object...args) {
		return new IllegalArgumentException(args.length == 0 ? msg : f(msg, args));
	}

	private static boolean isArray(Object o) {
		return o != null && o.getClass().isArray();
	}

	/**
	 * Returns <jk>true</jk> if the specified object is empty.
	 *
	 * <p>
	 * Return <jk>true</jk> if the value is any of the following:
	 * <ul>
	 * 	<li><jk>null</jk>
	 * 	<li>An empty Collection
	 * 	<li>An empty Map
	 * 	<li>An empty array
	 * 	<li>An empty CharSequence
	 * 	<li>An empty String when serialized to a string using {@link Object#toString()}.
	 * </ul>
	 *
	 * @param o The object to test.
	 * @return <jk>true</jk> if the specified object is empty.
	 */
	public static boolean isEmpty(Object o) {
		if (o == null)
			return true;
		if (o instanceof Collection<?> o2)
			return  o2.isEmpty();
		if (o instanceof Map<?,?> o2)
			return o2.isEmpty();
		if (o.getClass().isArray())
			return (Array.getLength(o) == 0);
		return o.toString().isEmpty();
	}

	/**
	 * @return True if string is null or empty.
	 */
	public static boolean isEmpty(String o) {
		return o == null || o.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if specified string is <jk>null</jk> or empty or consists of only blanks.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if specified string is <jk>null</jk> or emptyor consists of only blanks.
	 */
	public static boolean isEmptyOrBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if the specified object is not <jk>null</jk> and not empty.
	 *
	 * Works on any of the following data types:  String, CharSequence, Collection, Map, array.
	 * All other types are stringified and then checked as a String.
	 *
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified object is not <jk>null</jk> and not empty.
	 */
	public static boolean isNotEmpty(Object value) {
		if (value == null) return false;
		if (value instanceof CharSequence x) return ! x.isEmpty();
		if (value instanceof Collection<?> x) return ! x.isEmpty();
		if (value instanceof Map<?,?> x) return ! x.isEmpty();
		if (value.getClass().isArray()) return Array.getLength(value) > 0;
		return isNotEmpty(s(value));
	}

	/**
	 * @return True if string is not null or empty.
	 */
	public static boolean isNotEmpty(String o) {
		return ! isEmpty(o);
	}

	/**
	 * Returns <jk>true</jk> if the specified number is not <jk>null</jk> and not <c>-1</c>.
	 *
	 * @param <T> The value types.
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified number is not <jk>null</jk> and not <c>-1</c>.
	 */
	public static <T extends Number> boolean isNotMinusOne(T value) {
		return value != null && value.intValue() != -1;
	}

	/**
	 * Returns <jk>true</jk> if the specified object is not <jk>null</jk>.
	 *
	 * @param <T> The value type.
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified object is not <jk>null</jk>.
	 */
	public static <T> boolean isNotNull(T value) {
		return value != null;
	}

	/**
	 * Returns <jk>true</jk> if the specified boolean is not <jk>null</jk> and is <jk>true</jk>.
	 *
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified boolean is not <jk>null</jk> and is <jk>true</jk>.
	 */
	public static boolean isTrue(Boolean value) {
		return value != null && value;
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Collection<?> tokens, char d) {
		if (tokens == null)
			return null;
		var sb = new StringBuilder();
		for (var iter = tokens.iterator(); iter.hasNext();) {
			sb.append(iter.next());
			if (iter.hasNext())
				sb.append(d);
		}
		return sb.toString();
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Collection<?> tokens, String d) {
		if (tokens == null)
			return null;
		return join(tokens, d, new StringBuilder()).toString();
	}

	/**
	 * Joins the specified tokens into a delimited string and writes the output to the specified string builder.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @param sb The string builder to append the response to.
	 * @return The same string builder passed in as <c>sb</c>.
	 */
	public static StringBuilder join(Collection<?> tokens, String d, StringBuilder sb) {
		if (tokens == null)
			return sb;
		for (var iter = tokens.iterator(); iter.hasNext();) {
			sb.append(iter.next());
			if (iter.hasNext())
				sb.append(d);
		}
		return sb;
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(int[] tokens, char d) {
		if (tokens == null)
			return null;
		var sb = new StringBuilder();
		for (var i = 0; i < tokens.length; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(tokens[i]);
		}
		return sb.toString();
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(List<?> tokens, char d) {
		if (tokens == null)
			return null;
		var sb = new StringBuilder();
		for (int i = 0, j = tokens.size(); i < j; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(tokens.get(i));
		}
		return sb.toString();
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(List<?> tokens, String d) {
		if (tokens == null)
			return null;
		return join(tokens, d, new StringBuilder()).toString();
	}

	/**
	 * Joins the specified tokens into a delimited string and writes the output to the specified string builder.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @param sb The string builder to append the response to.
	 * @return The same string builder passed in as <c>sb</c>.
	 */
	public static StringBuilder join(List<?> tokens, String d, StringBuilder sb) {
		if (tokens == null)
			return sb;
		for (int i = 0, j = tokens.size(); i < j; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(tokens.get(i));
		}
		return sb;
	}

	/**
	 * Joins the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Object[] tokens, char d) {
		if (tokens == null)
			return null;
		if (tokens.length == 1)
			return emptyIfNull(s(tokens[0]));
		return join(tokens, d, new StringBuilder()).toString();
	}

	/**
	 * Join the specified tokens into a delimited string and writes the output to the specified string builder.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @param sb The string builder to append the response to.
	 * @return The same string builder passed in as <c>sb</c>.
	 */
	public static StringBuilder join(Object[] tokens, char d, StringBuilder sb) {
		if (tokens == null)
			return sb;
		for (var i = 0; i < tokens.length; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(tokens[i]);
		}
		return sb;
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param separator The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Object[] tokens, String separator) {
		if (tokens == null)
			return null;
		var sb = new StringBuilder();
		for (var i = 0; i < tokens.length; i++) {
			if (i > 0)
				sb.append(separator);
			sb.append(tokens[i]);
		}
		return sb.toString();
	}

	/**
	 * Joins tokens with newlines.
	 *
	 * @param tokens The tokens to concatenate.
	 * @return A string with the specified tokens contatenated with newlines.
	 */
	public static String joinnl(Object[] tokens) {
		return join(tokens, '\n');
	}

	/**
	 * Shortcut for creating a modifiable list out of an array of values.
	 */
	@SafeVarargs
	public static <T> List<T> list(T...values) {  // NOSONAR
		return new ArrayList<>(Arrays.asList(values));
	}

	/**
	 * Convenience method for creating an {@link ArrayList} of the specified size.
	 *
	 * @param <E> The element type.
	 * @param size The initial size of the list.
	 * @return A new modifiable list.
	 */
	public static <E> ArrayList<E> listOfSize(int size) {
		return new ArrayList<>(size);
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

	/** Not equals */
	public static <T> boolean ne(T s1, T s2) {
		return ! eq(s1, s2);
	}

	/**
	 * Tests two objects for inequality, gracefully handling nulls.
	 *
	 * @param <T> Object 1 type.
	 * @param <U> Object 2 type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @param test The test to use for equality.
	 * @return <jk>false</jk> if both objects are equal based on the test.
	 */
	public static <T,U> boolean ne(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null)
			return o2 != null;
		if (o2 == null)
			return true;
		if (o1 == o2)
			return false;
		return ! test.test(o1, o2);
	}

	/**
	 * Tests two strings for non-equality ignoring case, but gracefully handles nulls.
	 *
	 * @param s1 String 1.
	 * @param s2 String 2.
	 * @return <jk>true</jk> if the strings are not equal ignoring case.
	 */
	public static boolean neic(String s1, String s2) {
		return ! eqic(s1, s2);
	}

	/**
	 * Null-safe {@link String#contains(CharSequence)} operation.
	 */
	public static boolean notContains(String s, char...values) {
		return ! contains(s, values);
	}

	/**
	 * Returns the specified string, or null if that string is null or empty.
	 */
	public static String nullIfEmpty(String value) {
		return isEmpty(value) ? null : value;
	}

	/**
	 * Returns <jk>null</jk> if the specified string is <jk>null</jk> or empty.
	 *
	 * @param s The string to check.
	 * @return <jk>null</jk> if the specified string is <jk>null</jk> or empty, or the same string if not.
	 */
	public static String nullIfEmpty3(String s) {
		if (s == null || s.isEmpty())
			return null;
		return s;
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
	 * Shortcut for calling {@link Optional#ofNullable(Object)}.
	 */
	public static final <T> Optional<T> opt(T t) {
		return Optional.ofNullable(t);
	}

	public static final <T> Optional<T> opte() {
		return Optional.empty();
	}

	/**
	 * Prints all the specified lines to System.out.
	 */
	public static final void printLines(String[] lines) {
		for (var i = 0; i < lines.length; i++)
			System.out.println(String.format("%4s:" + lines[i], i+1)); // NOSONAR - NOT DEBUG
	}

	/**
	 * Shortcut for {@link #readable(Object)}
	 */
	public static String r(Object o) {
		if (o instanceof Optional<?> o2)
			return r(o2.orElse(null));
		if (o instanceof Collection<?> o2)
			return o2.stream().map(Utils::r).collect(joining(",","[","]"));
		if (o instanceof Map<?,?> o2)
			return o2.entrySet().stream().map(Utils::r).collect(joining(",","{","}"));
		if (o instanceof Map.Entry<?,?> o2)
			return r(o2.getKey()) + '=' + r(o2.getValue());
		if (o instanceof GregorianCalendar o2)
			return o2.toZonedDateTime().format(DateTimeFormatter.ISO_INSTANT);
		if (o instanceof Date o2)
			return o2.toInstant().toString();
		if (o instanceof InputStream o2)
			return toHex(o2);
		if (o instanceof Reader o2)
			return safe(()->IOUtils.read(o2));
		if (o instanceof File o2)
			return safe(()->IOUtils.read(o2));
		if (o instanceof byte[] o2)
			return toHex(o2);
		if (o != null && o.getClass().isArray()) {
			var l = list();
			for (var i = 0; i < Array.getLength(o); i++) {
				l.add(Array.get(o, i));
			}
			return r(l);
		}
		return s(o);
	}

	/**
	 * Creates a {@link RuntimeException}.
	 */
	public static RuntimeException runtimeException(String msg, Object...args) {
		return new RuntimeException(args.length == 0 ? msg : f(msg, args));
	}

	/**
	 * Shortcut for converting an object to a string.
	 */
	public static String s(Object val) {
		return val == null ? null : val.toString();
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
	 * Shortcut for creating a modifiable set out of an array of values.
	 */
	@SafeVarargs
	public static <T> LinkedHashSet<T> set(T...values) {  // NOSONAR
		return new LinkedHashSet<>(Arrays.asList(values));
	}

	/**
	 * Splits a comma-delimited list into a list of strings.
	 */
	public static List<String> split(String s) {
		return s == null ? Collections.emptyList() : split(s, ',');
	}

	/**
	 * Splits a character-delimited string into a string array.
	 *
	 * <p>
	 * Does not split on escaped-delimiters (e.g. "\,");
	 * Resulting tokens are trimmed of whitespace.
	 *
	 * <p>
	 * <b>NOTE:</b>  This behavior is different than the Jakarta equivalent.
	 * split("a,b,c",',') -&gt; {"a","b","c"}
	 * split("a, b ,c ",',') -&gt; {"a","b","c"}
	 * split("a,,c",',') -&gt; {"a","","c"}
	 * split(",,",',') -&gt; {"","",""}
	 * split("",',') -&gt; {}
	 * split(null,',') -&gt; null
	 * split("a,b\,c,d", ',', false) -&gt; {"a","b\,c","d"}
	 * split("a,b\\,c,d", ',', false) -&gt; {"a","b\","c","d"}
	 * split("a,b\,c,d", ',', true) -&gt; {"a","b,c","d"}
	 *
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static List<String> split(String s, char c) {
		return split(s, c, Integer.MAX_VALUE);
	}

	/**
	 * Same as {@link #splita(String, char)} but limits the number of tokens returned.
	 *
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @param limit The maximum number of tokens to return.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static List<String> split(String s, char c, int limit) {

		var escapeChars = StringUtils.getEscapeSet(c);

		if (s == null)
			return null;  // NOSONAR - Intentional.
		if (isEmpty(s))
			return Collections.emptyList();
		if (s.indexOf(c) == -1)
			return Collections.singletonList(s);

		var l = new LinkedList<String>();
		var sArray = s.toCharArray();
		var x1 = 0;
		var escapeCount = 0;
		limit--;
		for (var i = 0; i < sArray.length && limit > 0; i++) {
			if (sArray[i] == '\\')
				escapeCount++;
			else if (sArray[i]==c && escapeCount % 2 == 0) {
				var s2 = new String(sArray, x1, i-x1);
				var s3 = StringUtils.unEscapeChars(s2, escapeChars);
				l.add(s3.trim());
				limit--;
				x1 = i+1;
			}
			if (sArray[i] != '\\')
				escapeCount = 0;
		}
		var s2 = new String(sArray, x1, sArray.length-x1);
		var s3 = StringUtils.unEscapeChars(s2, escapeChars);
		l.add(s3.trim());

		return l;
	}

	/**
	 * Splits a character-delimited string into a string array.
	 *
	 * <p>
	 * Does not split on escaped-delimiters (e.g. "\,");
	 * Resulting tokens are trimmed of whitespace.
	 *
	 * <p>
	 * <b>NOTE:</b>  This behavior is different than the Jakarta equivalent.
	 * split("a,b,c",',') -&gt; {"a","b","c"}
	 * split("a, b ,c ",',') -&gt; {"a","b","c"}
	 * split("a,,c",',') -&gt; {"a","","c"}
	 * split(",,",',') -&gt; {"","",""}
	 * split("",',') -&gt; {}
	 * split(null,',') -&gt; null
	 * split("a,b\,c,d", ',', false) -&gt; {"a","b\,c","d"}
	 * split("a,b\\,c,d", ',', false) -&gt; {"a","b\","c","d"}
	 * split("a,b\,c,d", ',', true) -&gt; {"a","b,c","d"}
	 *
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static String[] splita(String s, char c) {
		return splita(s, c, Integer.MAX_VALUE);
	}

	/**
	 * Same as {@link #splita(String,char)} but consumes the tokens instead of creating an array.
	 *
	 * @param s The string to split.
	 * @param c The character to split on.
	 * @param consumer The consumer of the tokens.
	 */
	public static void split(String s, char c, Consumer<String> consumer) {
		var escapeChars = StringUtils.getEscapeSet(c);

		if (isEmpty(s))
			return;
		if (s.indexOf(c) == -1) {
			consumer.accept(s);
			return;
		}

		var x1 = 0;
		var escapeCount = 0;

		for (var i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '\\')
				escapeCount++;
			else if (s.charAt(i)==c && escapeCount % 2 == 0) {
				var s2 = s.substring(x1, i);
				var s3 = StringUtils.unEscapeChars(s2, escapeChars);
				consumer.accept(s3.trim());  // NOSONAR - NPE not possible.
				x1 = i+1;
			}
			if (s.charAt(i) != '\\')
				escapeCount = 0;
		}
		var s2 = s.substring(x1);
		var s3 = StringUtils.unEscapeChars(s2, escapeChars);
		consumer.accept(s3.trim());  // NOSONAR - NPE not possible.
	}

	/**
	 * Same as {@link #splita(String, char)} but limits the number of tokens returned.
	 *
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @param limit The maximum number of tokens to return.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static String[] splita(String s, char c, int limit) {
		var l = split(s, c, limit);
		return l == null ? null : l.toArray(new String[l.size()]);
	}

	/**
	 * Same as {@link #split3(String)} but consumes the tokens instead of creating an array.
	 *
	 * @param s The string to split.
	 * @param consumer The consumer of the tokens.
	 */
	public static void split(String s, Consumer<String> consumer) {
		split(s, ',', consumer);
	}

	/**
	 * Same as {@link #splita(String, char)} except splits all strings in the input and returns a single result.
	 *
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @return The tokens, or null if the input array was null
	 */
	public static String[] splita(String[] s, char c) {
		if (s == null)
			return null;  // NOSONAR - Intentional.
		var l = new LinkedList<String>();
		for (var ss : s) {
			if (ss == null || ss.indexOf(c) == -1)
				l.add(ss);
			else
				Collections.addAll(l, splita(ss, c));
		}
		return l.toArray(new String[l.size()]);
	}

	/**
	 * Splits a comma-delimited list into an array of strings.
	 */
	public static String[] splita(String s) {
		return splita(s, ',');
	}

	/**
	 * Splits a list of key-value pairs into an ordered map.
	 *
	 * <p>
	 * Example:
	 * <p class='bjava'>
	 * 	String <jv>in</jv> = <js>"foo=1;bar=2"</js>;
	 * 	Map <jv>map</jv> = StringUtils.<jsm>splitMap</jsm>(in, <js>';'</js>, <js>'='</js>, <jk>true</jk>);
	 * </p>
	 *
	 * @param s The string to split.
	 * @param trim Trim strings after parsing.
	 * @return The parsed map, or null if the string was null.
	 */
	public static Map<String,String> splitMap(String s, boolean trim) {

		if (s == null)
			return null;  // NOSONAR - Intentional.
		if (isEmpty(s))
			return Collections.emptyMap();

		var m = new LinkedHashMap<String,String>();

		final int
			S1 = 1,  // Found start of key, looking for equals.
			S2 = 2;  // Found equals, looking for delimiter (or end).

		var state = S1;

		var sArray = s.toCharArray();
		var x1 = 0;
		var escapeCount = 0;
		String key = null;
		for (var i = 0; i < sArray.length + 1; i++) {
			var c = i == sArray.length ? ',' : sArray[i];
			if (c == '\\')
				escapeCount++;
			if (escapeCount % 2 == 0) {
				if (state == S1) {
					if (c == '=') {
						key = s.substring(x1, i);
						if (trim)
							key = StringUtils.trim(key);
						key = StringUtils.unEscapeChars(key, StringUtils.MAP_ESCAPE_SET);
						state = S2;
						x1 = i+1;
					} else if (c == ',') {
						key = s.substring(x1, i);
						if (trim)
							key = StringUtils.trim(key);
						key = StringUtils.unEscapeChars(key, StringUtils.MAP_ESCAPE_SET);
						m.put(key, "");
						state = S1;
						x1 = i+1;
					}
				} else if (state == S2) {
					if (c == ',') {  // NOSONAR - Intentional.
						var val = s.substring(x1, i);
						if (trim)
							val = StringUtils.trim(val);
						val = StringUtils.unEscapeChars(val, StringUtils.MAP_ESCAPE_SET);
						m.put(key, val);
						key = null;
						x1 = i+1;
						state = S1;
					}
				}
			}
			if (c != '\\')
				escapeCount = 0;
		}

		return m;
	}

	/**
	 * Splits the method arguments in the signature of a method.
	 *
	 * @param s The arguments to split.
	 * @return The split arguments, or null if the input string is null.
	 */
	public static String[] splitMethodArgs(String s) {

		if (s == null)
			return null;  // NOSONAR - Intentional.
		if (isEmpty(s))
			return new String[0];
		if (s.indexOf(',') == -1)
			return new String[]{s};

		var l = new LinkedList<String>();
		var sArray = s.toCharArray();
		var x1 = 0;
		var paramDepth = 0;

		for (var i = 0; i < sArray.length; i++) {
			var c = s.charAt(i);
			if (c == '>')
				paramDepth++;
			else if (c == '<')
				paramDepth--;
			else if (c == ',' && paramDepth == 0) {
				var s2 = new String(sArray, x1, i-x1);
				l.add(s2.trim());
				x1 = i+1;
			}
		}

		var s2 = new String(sArray, x1, sArray.length-x1);
		l.add(s2.trim());

		return l.toArray(new String[l.size()]);
	}

	/**
	 * Splits a comma-delimited list containing "nesting constructs".
	 *
	 * Nesting constructs are simple embedded "{...}" comma-delimted lists.
	 *
	 * Example:
	 * 	"a{b,c},d" -> ["a{b,c}","d"]
	 *
	 * Handles escapes and trims whitespace from tokens.
	 *
	 * @param s The input string.
	 * 	The results, or <jk>null</jk> if the input was <jk>null</jk>.
	 * 	<br>An empty string results in an empty array.
	 */
	public static List<String> splitNested(String s) {
		var escapeChars = StringUtils.getEscapeSet(',');

		if (s == null) return null;  // NOSONAR - Intentional.
		if (isEmpty(s)) return Collections.emptyList();
		if (s.indexOf(',') == -1) return Collections.singletonList(StringUtils.trim(s));

		var l = new LinkedList<String>();

		var x1 = 0;
		var inEscape = false;
		var depthCount = 0;

		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (inEscape) {
				if (c == '\\') {
					inEscape = false;
				}
			} else {
				if (c == '\\') {
					inEscape = true;
				} else if (c == '{') {
					depthCount++;
				} else if (c == '}') {
					depthCount--;
				} else if (c == ',' && depthCount == 0) {
					l.add(StringUtils.trim(StringUtils.unEscapeChars(s.substring(x1, i), escapeChars)));
					x1 = i+1;
				}
			}
		}
		l.add(StringUtils.trim(StringUtils.unEscapeChars(s.substring(x1, s.length()), escapeChars)));

		return l;
	}

	/**
	 * Splits a nested comma-delimited list.
	 *
	 * Nesting constructs are simple embedded "{...}" comma-delimted lists.
	 *
	 * Example:
	 * 	"a{b,c{d,e}}" -> ["b","c{d,e}"]
	 *
	 * Handles escapes and trims whitespace from tokens.
	 *
	 * @param s The input string.
	 * 	The results, or <jk>null</jk> if the input was <jk>null</jk>.
	 * 	<br>An empty string results in an empty array.
	 */
	public static List<String> splitNestedInner(String s) {
		if (s == null) throw illegalArg("String was null.");
		if (isEmpty(s)) throw illegalArg("String was empty.");

		final int
			S1 = 1,  // Looking for '{'
			S2 = 2;  // Found '{', looking for '}'

		var start = -1;
		var end = -1;
		var state = S1;
		var depth = 0;
		var inEscape = false;

		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (inEscape) {
				if (c == '\\') {
					inEscape = false;
				}
			} else {
				if (c == '\\') {
					inEscape = true;
				} else if (state == S1) {
					if (c == '{') {
						start = i+1;
						state = S2;
					}
				} else /* state == S2 */ {
					if (c == '{') {
						depth++;
					} else if (depth > 0 && c == '}') {
						depth--;
					} else if (c == '}') {
						end = i;
						break;
					}
				}
			}
		}

		if (start == -1) throw illegalArg("Start character '{' not found in string.", s);
		if (end == -1) throw illegalArg("End character '}' not found in string.", s);
		return splitNested(s.substring(start, end));
	}

	/**
	 * Splits a space-delimited string with optionally quoted arguments.
	 *
	 * <p>
	 * Examples:
	 * <ul>
	 * 	<li><js>"foo"</js> =&gt; <c>["foo"]</c>
	 * 	<li><js>" foo "</js> =&gt; <c>["foo"]</c>
	 * 	<li><js>"foo bar baz"</js> =&gt; <c>["foo","bar","baz"]</c>
	 * 	<li><js>"foo 'bar baz'"</js> =&gt; <c>["foo","bar baz"]</c>
	 * 	<li><js>"foo \"bar baz\""</js> =&gt; <c>["foo","bar baz"]</c>
	 * 	<li><js>"foo 'bar\'baz'"</js> =&gt; <c>["foo","bar'baz"]</c>
	 * </ul>
	 *
	 * @param s The input string.
	 * @return
	 * 	The results, or <jk>null</jk> if the input was <jk>null</jk>.
	 * 	<br>An empty string results in an empty array.
	 */
	public static String[] splitQuoted(String s) {
		return splitQuoted(s, false);
	}

	/**
	 * Same as {@link #splitQuoted(String)} but allows you to optionally keep the quote characters.
	 *
	 * @param s The input string.
	 * @param keepQuotes If <jk>true</jk>, quote characters are kept on the tokens.
	 * @return
	 * 	The results, or <jk>null</jk> if the input was <jk>null</jk>.
	 * 	<br>An empty string results in an empty array.
	 */
	public static String[] splitQuoted(String s, boolean keepQuotes) {

		if (s == null)
			return null;  // NOSONAR - Intentional.

		s = s.trim();

		if (isEmpty(s))
			return new String[0];

		if (! StringUtils.containsAny(s, ' ', '\t', '\'', '"'))
			return new String[]{s};

		final int
			S1 = 1,  // Looking for start of token.
			S2 = 2,  // Found ', looking for end '
			S3 = 3,  // Found ", looking for end "
			S4 = 4;  // Found non-whitespace, looking for end whitespace.

		var state = S1;

		var isInEscape = false;
		var needsUnescape = false;
		var mark = 0;

		var l = new ArrayList<String>();
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);

			if (state == S1) {
				if (c == '\'') {
					state = S2;
					mark = keepQuotes ? i : i+1;
				} else if (c == '"') {
					state = S3;
					mark = keepQuotes ? i : i+1;
				} else if (c != ' ' && c != '\t') {
					state = S4;
					mark = i;
				}
			} else if (state == S2 || state == S3) {
				if (c == '\\') {
					isInEscape = ! isInEscape;
					needsUnescape = ! keepQuotes;
				} else if (! isInEscape) {
					if (c == (state == S2 ? '\'' : '"')) {
						var s2 = s.substring(mark, keepQuotes ? i+1 : i);
						if (needsUnescape)  // NOSONAR - False positive check.
							s2 = StringUtils.unEscapeChars(s2, StringUtils.QUOTE_ESCAPE_SET);
						l.add(s2);
						state = S1;
						isInEscape = needsUnescape = false;
					}
				} else {
					isInEscape = false;
				}
			} else /* state == S4 */ {
				if (c == ' ' || c == '\t') {
					l.add(s.substring(mark, i));
					state = S1;
				}
			}
		}
		if (state == S4)
			l.add(s.substring(mark));
		else if (state == S2 || state == S3)
			throw new IllegalArgumentException("Unmatched string quotes: " + s);
		return l.toArray(new String[l.size()]);
	}

	/**
	 * Converts an array to a stream of objects.
	 * @param array The array to convert.
	 * @return A new stream.
	 */
	public static Stream<Object> toStream(Object array) {
		if (array == null || ! array.getClass().isArray()) {
			throw illegalArg("Not an array: " + array);
		}
		var length = Array.getLength(array);
		return IntStream.range(0, length).mapToObj(i -> Array.get(array, i));
	}

	@SuppressWarnings("rawtypes")
	private static <T> T toType(String s, T def) {
		if (s == null || def == null)
			return null;
		var c = (Class<T>)def.getClass();
		if (c == String.class)
			return (T)s;
		if (c.isEnum())
			return (T)Enum.valueOf((Class<? extends Enum>) c, s);
		var f = (Function<String,T>)ENV_FUNCTIONS.get(c);
		if (f == null)
			throw runtimeException("Invalid env type: {0}", c);
		return f.apply(s);
	}

	/**
	 * Traverses all elements in the specified object and executes a consumer for it.
	 *
	 * @param <T> The element type.
	 * @param o The object to traverse.
	 * @param c The consumer of the objects.
	 */
	public static <T> void traverse(Object o, Consumer<T> c) {
		if (o == null)
			return;
		if (o instanceof Iterable<?> o2)
			o2.forEach(x -> traverse(x, c));
		else if (o instanceof Stream<?> o2)
			o2.forEach(x -> traverse(x, c));
		else if (o.getClass().isArray())
			toStream(o).forEach(x -> traverse(x, c));
		else
			c.accept((T)o);
	}

	public static <T> List<T> u(List<? extends T> value) {
		return value == null ? null : Collections.unmodifiableList(value);
	}

	public static <K,V> Map<K,V> u(Map<? extends K, ? extends V> value) {
		return value == null ? null : Collections.unmodifiableMap(value);
	}

	public static <T> Set<T> u(Set<? extends T> value) {
		return value == null ? null : Collections.unmodifiableSet(value);
	}

	/**
	 * Allows you to wrap a supplier that throws an exception so that it can be used in a fluent interface.
	 *
	 * @param <T> The supplier type.
	 * @param supplier The supplier throwing an exception.
	 * @return The supplied result.
	 * @throws RuntimeException if supplier threw an exception.
	 */
	public static <T> T safeSupplier(ThrowableUtils.SupplierWithThrowable<T> supplier) {
		try {
			return supplier.get();
		} catch (RuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw ThrowableUtils.asRuntimeException(t);
		}
	}

	/**
	 * Runs a snippet of code and encapsulates any throwable inside a {@link RuntimeException}.
	 *
	 * @param snippet The snippet of code to run.
	 */
	public static void safe(Snippet snippet) {
		try {
			snippet.run();
		} catch (RuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw ThrowableUtils.asRuntimeException(t);
		}
	}
}