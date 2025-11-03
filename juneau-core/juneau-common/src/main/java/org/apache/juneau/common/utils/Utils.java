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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;

import java.lang.reflect.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.function.*;

/**
 * Common utility methods.
 *
 * <p>
 * This class contains various static utility methods for working with collections, strings, objects, and other common operations.
 *
 * <h5 class='section'>Features:</h5>
 * <ul>
 *   <li><b>Collections:</b> Array and list creation, conversion, and manipulation
 *   <li><b>Strings:</b> Formatting, comparison, and null-safe operations
 *   <li><b>Objects:</b> Equality checking, casting, and null handling
 *   <li><b>Environment:</b> System property and environment variable access
 *   <li><b>Optionals:</b> Enhanced Optional operations and conversions
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 *   <li class='jc'>{@link AssertionUtils} - For argument validation and assertion methods
 *   <li class='jc'>{@link StringUtils} - For additional string manipulation utilities
 *   <li class='link'><a class="doclink" href='../../../../../index.html#juneau-common.utils'>Overview &gt; juneau-common.utils</a>
 * </ul>
 */
public class Utils {

	private static final Map<Class<?>,Function<String,?>> ENV_FUNCTIONS = new IdentityHashMap<>();

	static {
		ENV_FUNCTIONS.put(Boolean.class, Boolean::valueOf);
		ENV_FUNCTIONS.put(Charset.class, Charset::forName);
	}

	private static final ConcurrentHashMap<String,String> PROPERTY_TO_ENV = new ConcurrentHashMap<>();

	/**
	 * Casts an object to a specific type if it's an instance of that type.
	 *
	 * @param <T> The type to cast to.
	 * @param c The type to cast to.
	 * @param o The object to cast to.
	 * @return The cast object, or <jk>null</jk> if the object wasn't the specified type.
	 */
	public static <T> T cast(Class<T> c, Object o) {
		return nn(o) && c.isInstance(o) ? c.cast(o) : null;
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
	 * Creates an empty array of the specified type.
	 *
	 * @param <T> The component type of the array.
	 * @param type The component type class.
	 * @return An empty array of the specified type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] ea(Class<T> type) {
		return (T[])Array.newInstance(type, 0);
	}

	/**
	 * Returns the specified string, or blank if that string is null.
	 *
	 * @param value The value to convert to a string.
	 * @return The string representation of the value, or an empty string if <jk>null</jk>.
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
				if (ne(Array.get(o1, i), Array.get(o2, i)))
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
	 * 		return eq(this, (Role)o, (x,y)-&gt;eq(x.id,y.id) &amp;&amp; eq(x.name,y.name) &amp;&amp; eq(x.created,y.created) &amp;&amp; eq(x.createdBy,y.createdBy));
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
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return false;
		}
		if (o1 == o2) {
			return true;
		}
		return test.test(o1, o2);
	}

	/**
	 * Tests two annotations for equality using the criteria presented in the {@link java.lang.annotation.Annotation#equals(Object)} API docs.
	 *
	 * <p>
	 * This method delegates to {@link AnnotationUtils#equals(java.lang.annotation.Annotation, java.lang.annotation.Annotation)}
	 * to ensure proper annotation comparison according to the annotation equality contract.
	 *
	 * @param a1 Annotation 1.
	 * @param a2 Annotation 2.
	 * @return <jk>true</jk> if the two annotations are equal or both are <jk>null</jk>.
	 * @see AnnotationUtils#equals(java.lang.annotation.Annotation, java.lang.annotation.Annotation)
	 */
	public static boolean eq(java.lang.annotation.Annotation a1, java.lang.annotation.Annotation a2) {
		return AnnotationUtils.equals(a1, a2);
	}

	/**
	 * Tests two annotations for inequality using the criteria presented in the {@link java.lang.annotation.Annotation#equals(Object)} API docs.
	 *
	 * <p>
	 * This method is the negation of {@link #eq(java.lang.annotation.Annotation, java.lang.annotation.Annotation)},
	 * which delegates to {@link AnnotationUtils#equals(java.lang.annotation.Annotation, java.lang.annotation.Annotation)}.
	 *
	 * @param a1 Annotation 1.
	 * @param a2 Annotation 2.
	 * @return <jk>true</jk> if the two annotations are not equal.
	 * @see #eq(java.lang.annotation.Annotation, java.lang.annotation.Annotation)
	 * @see AnnotationUtils#equals(java.lang.annotation.Annotation, java.lang.annotation.Annotation)
	 */
	public static boolean ne(java.lang.annotation.Annotation a1, java.lang.annotation.Annotation a2) {
		return !eq(a1, a2);
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
	 *
	 * @param pattern The message pattern.
	 * @param args The arguments to substitute into the pattern.
	 * @return The formatted string.
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
	public static <T> T firstNonNull(T...t) {
		if (nn(t))
			for (var tt : t)
				if (nn(tt))
					return tt;
		return null;
	}

	/**
	 * Converts a string containing glob-style wildcard characters to a regular expression {@link java.util.regex.Pattern}.
	 *
	 * <p>This method converts glob-style patterns to regular expressions with the following mappings:
	 * <ul>
	 *   <li>{@code *} matches any sequence of characters (including none)</li>
	 *   <li>{@code ?} matches exactly one character</li>
	 *   <li>All other characters are treated literally</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>var</jk> <jv>pattern</jv> = <jsm>getGlobMatchPattern</jsm>(<js>"user_*_temp"</js>);
	 *   <jk>boolean</jk> <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"user_alice_temp"</js>).matches();  <jc>// true</jc>
	 *   <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"user_bob_temp"</js>).matches();    <jc>// true</jc>
	 *   <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"admin_alice_temp"</js>).matches(); <jc>// false</jc>
	 * </p>
	 *
	 * @param s The glob-style wildcard pattern string.
	 * @return A compiled {@link java.util.regex.Pattern} object, or <jk>null</jk> if the input string is <jk>null</jk>.
	 */
	public static java.util.regex.Pattern getGlobMatchPattern(String s) {
		return getGlobMatchPattern(s, 0);
	}

	/**
	 * Converts a string containing glob-style wildcard characters to a regular expression {@link java.util.regex.Pattern} with flags.
	 *
	 * <p>This method converts glob-style patterns to regular expressions with the following mappings:
	 * <ul>
	 *   <li>{@code *} matches any sequence of characters (including none)</li>
	 *   <li>{@code ?} matches exactly one character</li>
	 *   <li>All other characters are treated literally</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jc>// Case-insensitive matching</jc>
	 *   <jk>var</jk> <jv>pattern</jv> = <jsm>getGlobMatchPattern</jsm>(<js>"USER_*"</js>, Pattern.<jsf>CASE_INSENSITIVE</jsf>);
	 *   <jk>boolean</jk> <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"user_alice"</js>).matches();  <jc>// true</jc>
	 * </p>
	 *
	 * @param s The glob-style wildcard pattern string.
	 * @param flags Regular expression flags (see {@link java.util.regex.Pattern} constants).
	 * @return A compiled {@link java.util.regex.Pattern} object, or <jk>null</jk> if the input string is <jk>null</jk>.
	 */
	public static java.util.regex.Pattern getGlobMatchPattern(String s, int flags) {
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
		return java.util.regex.Pattern.compile(sb.toString(), flags);
	}

	/**
	 * Creates a formatted string supplier with message arguments for lazy evaluation.
	 *
	 * <p>This method returns a {@link Supplier} that formats the string pattern with the provided arguments
	 * only when the supplier's {@code get()} method is called. This is useful for expensive string formatting
	 * operations that may not always be needed, such as error messages in assertions.</p>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Lazy evaluation - string is only formatted if assertion fails</jc>
	 * 	assertTrue(condition, fs(<js>"Expected {0} but got {1}"</js>, expected, actual));
	 *
	 * 	<jc>// Can be used anywhere a Supplier&lt;String&gt; is expected</jc>
	 * 	Supplier&lt;String&gt; <jv>messageSupplier</jv> = fs(<js>"Processing item {0} of {1}"</js>, i, total);
	 * </p>
	 *
	 * @param pattern The message pattern using <js>{0}</js>, <js>{1}</js>, etc. placeholders.
	 * @param args The arguments to substitute into the pattern placeholders.
	 * @return A {@link Supplier} that will format the string when {@code get()} is called.
	 * @see StringUtils#format(String, Object...)
	 */
	public static Supplier<String> fs(String pattern, Object...args) {
		return () -> format(pattern, args);
	}

	/**
	 * Calculates a hash code for the specified values.
	 *
	 * <p>
	 * This method handles annotations specially by delegating to {@link AnnotationUtils#hash(Annotation)}
	 * to ensure consistent hashing according to the {@link java.lang.annotation.Annotation#hashCode()} contract.
	 * For non-annotation values, it uses {@link Objects#hashCode(Object)}.
	 *
	 * @param values The values to hash.
	 * @return A hash code value for the given values.
	 * @see AnnotationUtils#hash(Annotation)
	 */
	public static final int hash(Object...values) {
		if (values == null)
			return 0;
		var result = 1;
		for (var value : values) {
			result = 31 * result + (value instanceof java.lang.annotation.Annotation ? AnnotationUtils.hash((java.lang.annotation.Annotation)value) : Objects.hashCode(value));
		}
		return result;
	}

	/**
	 * Checks if the specified object is an array.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the object is not <jk>null</jk> and is an array.
	 */
	public static boolean isArray(Object o) {
		return nn(o) && o.getClass().isArray();
	}

	/**
	 * Returns <jk>true</jk> if the specified object can be converted to a list.
	 *
	 * <p>
	 * The following types are considered convertible:
	 * <ul>
	 * 	<li>Collections
	 * 	<li>Iterables
	 * 	<li>Iterators
	 * 	<li>Enumerations
	 * 	<li>Streams
	 * 	<li>Maps
	 * 	<li>Optional
	 * 	<li>Arrays
	 * </ul>
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the object can be converted to a list.
	 */
	public static boolean isConvertibleToList(Object o) {
		return nn(o) && (o instanceof Collection || o instanceof Iterable || o instanceof Iterator || o instanceof Enumeration || o instanceof Stream || o instanceof Map || o instanceof Optional
			|| isArray(o));
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
			return o2.isEmpty();
		if (o instanceof Map<?,?> o2)
			return o2.isEmpty();
		if (isArray(o))
			return (Array.getLength(o) == 0);
		return o.toString().isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if the specified collection is <jk>null</jk> or empty.
	 *
	 * @param o The collection to check.
	 * @return <jk>true</jk> if the specified collection is <jk>null</jk> or empty.
	 */
	public static boolean isEmpty(Collection<?> o) {
		if (o == null)
			return true;
		return o.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if the specified map is <jk>null</jk> or empty.
	 *
	 * @param o The map to check.
	 * @return <jk>true</jk> if the specified map is <jk>null</jk> or empty.
	 */
	public static boolean isEmpty(Map<?,?> o) {
		if (o == null)
			return true;
		return o.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if string is <jk>null</jk> or empty.
	 *
	 * @param o The string to check.
	 * @return <jk>true</jk> if string is <jk>null</jk> or empty.
	 */
	public static boolean isEmpty(CharSequence o) {
		return StringUtils.isEmpty(o);
	}

	/**
	 * Returns <jk>true</jk> if specified string is <jk>null</jk> or empty or consists of only blanks.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if specified string is <jk>null</jk> or empty or consists of only blanks.
	 */
	public static boolean isBlank(CharSequence s) {
		return isBlank(s);
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
		if (value == null)
			return false;
		if (value instanceof CharSequence x)
			return ! x.isEmpty();
		if (value instanceof Collection<?> x)
			return ! x.isEmpty();
		if (value instanceof Map<?,?> x)
			return ! x.isEmpty();
		if (isArray(value))
			return Array.getLength(value) > 0;
		return isNotEmpty(s(value));
	}

	/**
	 * Returns <jk>true</jk> if the specified collection is not <jk>null</jk> and not empty.
	 *
	 * @param value The collection to check.
	 * @return <jk>true</jk> if the specified collection is not <jk>null</jk> and not empty.
	 */
	public static boolean isNotEmpty(Collection<?> value) {
		return ! isEmpty(value);
	}

	/**
	 * Returns <jk>true</jk> if the specified map is not <jk>null</jk> and not empty.
	 *
	 * @param value The map to check.
	 * @return <jk>true</jk> if the specified map is not <jk>null</jk> and not empty.
	 */
	public static boolean isNotEmpty(Map<?,?> value) {
		return ! isEmpty(value);
	}

	/**
	 * Returns <jk>true</jk> if string is not <jk>null</jk> and not empty.
	 *
	 * @param o The string to check.
	 * @return <jk>true</jk> if string is not <jk>null</jk> and not empty.
	 */
	public static boolean isNotEmpty(CharSequence o) {
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
		return nn(value) && value.intValue() != -1;
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
		return nn(value) && value;
	}

	/**
	 * Returns <jk>null</jk> for the specified type.
	 *
	 * @param <T> The type.
	 * @param type The type class.
	 * @return <jk>null</jk>.
	 */
	public static <T> T n(Class<T> type) {
		return null;
	}

	/**
	 * Null-safe not-equals check.
	 *
	 * @param <T> The object type.
	 * @param s1 Object 1.
	 * @param s2 Object 2.
	 * @return <jk>true</jk> if the objects are not equal.
	 */
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
			return nn(o2);
		if (o2 == null)
			return true;
		if (o1 == o2)
			return false;
		return ! test.test(o1, o2);
	}

	/**
	 * Returns <jk>true</jk> if the specified object is not <jk>null</jk>.
	 *
	 * <p>
	 * Equivalent to <c><jv>o</jv> != <jk>null</jk></c>, but with a more readable method name.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.common.utils.Utils.*;
	 *
	 * 	<jk>if</jk> (<jsm>nn</jsm>(<jv>myObject</jv>)) {
	 * 		<jc>// Do something with non-null object</jc>
	 * 	}
	 * </p>
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is not <jk>null</jk>.
	 */
	public static boolean nn(Object o) {
		return isNotNull(o);
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
	 * Shortcut for calling {@link Optional#ofNullable(Object)}.
	 *
	 * @param <T> The object type.
	 * @param t The object to wrap in an Optional.
	 * @return An Optional containing the specified object, or empty if <jk>null</jk>.
	 */
	public static final <T> Optional<T> opt(T t) {
		return Optional.ofNullable(t);
	}

	/**
	 * Returns an empty Optional.
	 *
	 * @param <T> The object type.
	 * @return An empty Optional.
	 */
	public static final <T> Optional<T> opte() {
		return Optional.empty();
	}

	/**
	 * Prints all the specified lines to System.out.
	 *
	 * @param lines The lines to print.
	 */
	public static final void printLines(String[] lines) {
		for (var i = 0; i < lines.length; i++)
			System.out.println(String.format("%4s:" + lines[i], i + 1)); // NOSONAR - NOT DEBUG
	}

	/**
	 * Shortcut for calling {@link StringUtils#readable(Object)}.
	 *
	 * <p>Converts an arbitrary object to a readable string format suitable for debugging and testing.
	 *
	 * @param o The object to convert to readable format. Can be <jk>null</jk>.
	 * @return A readable string representation of the object, or <jk>null</jk> if the input was <jk>null</jk>.
	 * @see StringUtils#readable(Object)
	 */
	public static String r(Object o) {
		return readable(o);
	}

	/**
	 * Shortcut for converting an object to a string.
	 *
	 * @param val The object to convert.
	 * @return The string representation of the object, or <jk>null</jk> if the object is <jk>null</jk>.
	 */
	public static String s(Object val) {
		return val == null ? null : val.toString();
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
			throw ThrowableUtils.toRuntimeException(t);
		}
	}

	/**
	 * Used to wrap code that returns a value but throws an exception.
	 * Useful in cases where you're trying to execute code in a fluent method call
	 * or are trying to eliminate untestable catch blocks in code.
	 *
	 * @param <T> The return type.
	 * @param s The supplier that may throw an exception.
	 * @return The result of the supplier execution.
	 */
	public static <T> T safe(ThrowingSupplier<T> s) {
		try {
			return s.get();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw runtimeException(e);
		}
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
			throw toRuntimeException(t);
		}
	}

	/**
	 * Safely converts an object to its string representation, handling any exceptions that may occur.
	 *
	 * <p>This method provides a fail-safe way to call {@code toString()} on any object, ensuring that
	 * exceptions thrown by problematic {@code toString()} implementations don't propagate up the call stack.
	 * Instead, it returns a descriptive error message containing the exception type and message.</p>
	 *
	 * <h5 class='section'>Exception Handling:</h5>
	 * <p>If the object's {@code toString()} method throws any {@code Throwable}, this method catches it
	 * and returns a formatted string in the form: {@code "<ExceptionType>: <exception message>"}.</p>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Normal case - returns object's toString() result</jc>
	 *    String <jv>result</jv> = <jsm>safeToString</jsm>(<js>"Hello"</js>);
	 *    <jc>// result = "Hello"</jc>
	 *
	 *    <jc>// Exception case - returns formatted error message</jc>
	 *    Object <jv>problematic</jv> = <jk>new</jk> Object() {
	 *       <ja>@Override</ja>
	 *       <jk>public</jk> String toString() {
	 *          <jk>throw new</jk> RuntimeException(<js>"Cannot convert"</js>);
	 *       }
	 *    };
	 *    String <jv>result</jv> = <jsm>safeToString</jsm>(<jv>problematic</jv>);
	 *    <jc>// result = "RuntimeException: Cannot convert"</jc>
	 * </p>
	 *
	 * <h5 class='section'>Use Cases:</h5>
	 * <ul>
	 *    <li><b>Object stringification in converters:</b> Safe conversion of arbitrary objects to strings</li>
	 *    <li><b>Debugging and logging:</b> Ensures log statements never fail due to toString() exceptions</li>
	 *    <li><b>Error handling:</b> Graceful degradation when objects have problematic string representations</li>
	 *    <li><b>Third-party object integration:</b> Safe handling of objects from external libraries</li>
	 * </ul>
	 *
	 * @param o The object to convert to a string. May be any object including <jk>null</jk>.
	 * @return The string representation of the object, or a formatted error message if toString() throws an exception.
	 *    Returns <js>"null"</js> if the object is <jk>null</jk>.
	 */
	public static String safeToString(Object o) {
		try {
			return o.toString();
		} catch (Throwable t) { // NOSONAR
			return scn(t) + ": " + t.getMessage();
		}
	}

	/**
	 * Helper method for creating StringBuilder objects.
	 *
	 * @param value The string value to wrap in a StringBuilder.
	 * @return A new StringBuilder containing the specified value.
	 */
	public static StringBuilder sb(String value) {
		return new StringBuilder(value);
	}

	/**
	 * Converts a property name to an environment variable name.
	 *
	 * @param name The property name to convert.
	 * @return The environment variable name (uppercase with dots replaced by underscores).
	 */
	private static String envName(String name) {
		return PROPERTY_TO_ENV.computeIfAbsent(name, x -> x.toUpperCase().replace(".", "_"));
	}

	/**
	 * Converts a string to the specified type using registered conversion functions.
	 *
	 * @param <T> The target type.
	 * @param s The string to convert.
	 * @param def The default value (used to determine the target type).
	 * @return The converted value, or <jk>null</jk> if the string or default is <jk>null</jk>.
	 * @throws RuntimeException If the type is not supported for conversion.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> T toType(String s, T def) {
		if (s == null || def == null)
			return null;
		var c = (Class<T>)def.getClass();
		if (c == String.class)
			return (T)s;
		if (c.isEnum())
			return (T)Enum.valueOf((Class<? extends Enum>)c, s);
		var f = (Function<String,T>)ENV_FUNCTIONS.get(c);
		if (f == null)
			throw runtimeException("Invalid env type: {0}", c);
		return f.apply(s);
	}

	/** Constructor - This class is meant to be subclasses. */
	protected Utils() {}

	/**
	 * If the specified object is a {@link Supplier} or {@link Value}, returns the inner value, otherwise the same value.
	 *
	 * @param o The object to unwrap.
	 * @return The unwrapped object.
	 */
	public static Object unwrap(Object o) {
		if (o instanceof Supplier)
			o = unwrap(((Supplier<?>)o).get());
		if (o instanceof Value)
			o = unwrap(((Value<?>)o).get());
		if (o instanceof Optional)
			o = unwrap(((Optional<?>)o).orElse(null));
		return o;
	}

	// TODO: Look for cases of getClass().getName() and getClass().getSimpleName() in the code and replace with cn() and scn().
	// These utility methods provide cleaner, more concise syntax and null-safe handling.
	// Search patterns:
	//   - Replace: getClass().getName() -> cn(this) or cn(object)
	//   - Replace: getClass().getSimpleName() -> scn(this) or scn(object)
	//   - Replace: obj.getClass().getName() -> cn(obj)
	//   - Replace: obj.getClass().getSimpleName() -> scn(obj)

	/**
	 * Shortcut for calling {@link ClassUtils#className(Object)}.
	 *
	 * <p>
	 * Returns the fully-qualified class name including the full package path.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Regular classes</jc>
	 * 	cn(String.<jk>class</jk>);                  <jc>// "java.lang.String"</jc>
	 * 	cn(<jk>new</jk> HashMap&lt;&gt;());                <jc>// "java.util.HashMap"</jc>
	 *
	 * 	<jc>// Inner classes</jc>
	 * 	cn(Map.Entry.<jk>class</jk>);               <jc>// "java.util.Map$Entry"</jc>
	 *
	 * 	<jc>// Primitives</jc>
	 * 	cn(<jk>int</jk>.<jk>class</jk>);                      <jc>// "int"</jc>
	 * 	cn(<jk>boolean</jk>.<jk>class</jk>);                  <jc>// "boolean"</jc>
	 *
	 * 	<jc>// Arrays</jc>
	 * 	cn(String[].<jk>class</jk>);                <jc>// "[Ljava.lang.String;"</jc>
	 * 	cn(<jk>int</jk>[].<jk>class</jk>);                    <jc>// "[I"</jc>
	 * 	cn(String[][].<jk>class</jk>);              <jc>// "[[Ljava.lang.String;"</jc>
	 *
	 * 	<jc>// Null</jc>
	 * 	cn(<jk>null</jk>);                          <jc>// null</jc>
	 * </p>
	 *
	 * @param value The object to get the class name for.
	 * @return The name of the class or <jk>null</jk> if the value was null.
	 */
	public static String cn(Object value) {
		return ClassUtils.className(value);
	}

	/**
	 * Shortcut for calling {@link ClassUtils#simpleClassName(Object)}.
	 *
	 * <p>
	 * Returns only the simple class name without any package or outer class information.
	 * For inner classes, only the innermost class name is returned.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Regular classes</jc>
	 * 	scn(String.<jk>class</jk>);            <jc>// "String"</jc>
	 * 	scn(<jk>new</jk> HashMap&lt;&gt;());          <jc>// "HashMap"</jc>
	 *
	 * 	<jc>// Inner classes</jc>
	 * 	scn(Map.Entry.<jk>class</jk>);         <jc>// "Entry"</jc>
	 *
	 * 	<jc>// Primitives</jc>
	 * 	scn(<jk>int</jk>.<jk>class</jk>);                <jc>// "int"</jc>
	 * 	scn(<jk>boolean</jk>.<jk>class</jk>);            <jc>// "boolean"</jc>
	 *
	 * 	<jc>// Arrays</jc>
	 * 	scn(String[].<jk>class</jk>);          <jc>// "String[]"</jc>
	 * 	scn(<jk>int</jk>[].<jk>class</jk>);              <jc>// "int[]"</jc>
	 * 	scn(String[][].<jk>class</jk>);        <jc>// "String[][]"</jc>
	 *
	 * 	<jc>// Null</jc>
	 * 	scn(<jk>null</jk>);                    <jc>// null</jc>
	 * </p>
	 *
	 * @param value The object to get the simple class name for.
	 * @return The simple name of the class or <jk>null</jk> if the value was null.
	 */
	public static String scn(Object value) {
		return ClassUtils.simpleClassName(value);
	}

	/**
	 * Shortcut for calling {@link ClassUtils#simpleQualifiedClassName(Object)}.
	 *
	 * <p>
	 * Returns the simple class name including outer class names, but without the package.
	 * Inner class separators ($) are replaced with dots (.).
	 * Array types are properly formatted with brackets.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Regular classes</jc>
	 * 	sqcn(String.<jk>class</jk>);                     <jc>// "String"</jc>
	 * 	sqcn(<jk>new</jk> HashMap&lt;&gt;());                   <jc>// "HashMap"</jc>
	 *
	 * 	<jc>// Inner classes</jc>
	 * 	sqcn(Map.Entry.<jk>class</jk>);                  <jc>// "Map.Entry"</jc>
	 * 	sqcn(Outer.Inner.Deep.<jk>class</jk>);           <jc>// "Outer.Inner.Deep"</jc>
	 *
	 * 	<jc>// Primitives</jc>
	 * 	sqcn(<jk>int</jk>.<jk>class</jk>);                         <jc>// "int"</jc>
	 * 	sqcn(<jk>boolean</jk>.<jk>class</jk>);                     <jc>// "boolean"</jc>
	 *
	 * 	<jc>// Object arrays</jc>
	 * 	sqcn(String[].<jk>class</jk>);                   <jc>// "String[]"</jc>
	 * 	sqcn(Map.Entry[].<jk>class</jk>);                <jc>// "Map.Entry[]"</jc>
	 * 	sqcn(String[][].<jk>class</jk>);                 <jc>// "String[][]"</jc>
	 *
	 * 	<jc>// Primitive arrays</jc>
	 * 	sqcn(<jk>int</jk>[].<jk>class</jk>);                       <jc>// "int[]"</jc>
	 * 	sqcn(<jk>boolean</jk>[][].<jk>class</jk>);                 <jc>// "boolean[][]"</jc>
	 *
	 * 	<jc>// Null</jc>
	 * 	sqcn(<jk>null</jk>);                             <jc>// null</jc>
	 * </p>
	 *
	 * @param value The object to get the simple qualified class name for.
	 * @return The simple qualified name of the class or <jk>null</jk> if the value was null.
	 */
	public static String sqcn(Object value) {
		return ClassUtils.simpleQualifiedClassName(value);
	}

	/**
	 * Converts an object to a boolean.
	 *
	 * @param val The object to convert.
	 * @return The boolean value, or <jk>false</jk> if the value was <jk>null</jk>.
	 */
	public static boolean b(Object val) {
		return opt(val).map(Object::toString).map(Boolean::valueOf).orElse(false);
	}

	/**
	 * Returns <jk>true</jk> if the specified number is inclusively between the two values.
	 *
	 * @param n The number to check.
	 * @param lower The lower bound (inclusive).
	 * @param higher The upper bound (inclusive).
	 * @return <jk>true</jk> if the number is between the bounds.
	 */
	public static boolean isBetween(int n, int lower, int higher) {
		return n >= lower && n <= higher;
	}

	/**
	 * Same as {@link Integer#parseInt(String)} but removes any underscore characters first.
	 *
	 * <p>Allows for better readability of numeric literals (e.g., <js>"1_000_000"</js>).
	 *
	 * @param value The string to parse.
	 * @return The parsed integer value.
	 * @throws NumberFormatException If the string cannot be parsed.
	 * @throws NullPointerException If the string is <jk>null</jk>.
	 */
	public static int parseInt(String value) {
		return Integer.parseInt(removeUnderscores(value));
	}

	/**
	 * Same as {@link Long#parseLong(String)} but removes any underscore characters first.
	 *
	 * <p>Allows for better readability of numeric literals (e.g., <js>"1_000_000"</js>).
	 *
	 * @param value The string to parse.
	 * @return The parsed long value.
	 * @throws NumberFormatException If the string cannot be parsed.
	 * @throws NullPointerException If the string is <jk>null</jk>.
	 */
	public static long parseLong(String value) {
		return Long.parseLong(removeUnderscores(value));
	}

	/**
	 * Same as {@link Float#parseFloat(String)} but removes any underscore characters first.
	 *
	 * <p>Allows for better readability of numeric literals (e.g., <js>"1_000.5"</js>).
	 *
	 * @param value The string to parse.
	 * @return The parsed float value.
	 * @throws NumberFormatException If the string cannot be parsed.
	 * @throws NullPointerException If the string is <jk>null</jk>.
	 */
	public static float parseFloat(String value) {
		return Float.parseFloat(removeUnderscores(value));
	}

	private static String removeUnderscores(String value) {
		if (value == null)
			throw new NullPointerException("Trying to parse null string.");
		return notContains(value, '_') ? value : value.replace("_", "");
	}

	/**
	 * Shortcut for calling {@link StringUtils#stringSupplier(Supplier)}.
	 *
	 * @param s The supplier.
	 * @return A string supplier that calls {@link #r(Object)} on the supplied value.
	 */
	public static Supplier<String> ss(Supplier<?> s) {
		return stringSupplier(s);
	}

	/**
	 * Creates a thread-safe memoizing supplier that computes a value once and caches it.
	 *
	 * <p>
	 * The returned supplier is thread-safe and guarantees that the underlying supplier's {@link Supplier#get() get()}
	 * method is called at most once, even under concurrent access. The computed value is cached and returned
	 * on all subsequent calls.
	 *
	 * <p>
	 * This is useful for lazy initialization of expensive-to-compute values that should only be calculated once.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a memoizing supplier</jc>
	 * 	Supplier&lt;ExpensiveObject&gt; <jv>supplier</jv> = memoize(() -&gt; <jk>new</jk> ExpensiveObject());
	 *
	 * 	<jc>// First call computes and caches the value</jc>
	 * 	ExpensiveObject <jv>obj1</jv> = <jv>supplier</jv>.get();
	 *
	 * 	<jc>// Subsequent calls return the cached value (no recomputation)</jc>
	 * 	ExpensiveObject <jv>obj2</jv> = <jv>supplier</jv>.get();  <jc>// Same instance as obj1</jc>
	 * </p>
	 *
	 * <h5 class='section'>Thread Safety:</h5>
	 * <p>
	 * The implementation uses {@link java.util.concurrent.atomic.AtomicReference} with double-checked locking
	 * to ensure thread-safe lazy initialization. Under high contention, multiple threads may compute the value,
	 * but only one result is stored and returned to all callers.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>The supplier may be called multiple times if threads race, but only one result is cached.
	 * 	<li>The cached value can be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 * 	<li>Once cached, the value never changes (immutable after first computation).
	 * 	<li>The returned supplier does not support {@link #toString()}, {@link #equals(Object)}, or {@link #hashCode()}.
	 * </ul>
	 *
	 * @param <T> The type of value supplied.
	 * @param supplier The supplier to memoize. Must not be <jk>null</jk>.
	 * @return A thread-safe memoizing wrapper around the supplier.
	 * @throws NullPointerException if supplier is <jk>null</jk>.
	 */
	public static <T> Supplier<T> memoize(Supplier<T> supplier) {
		assertArgNotNull("supplier", supplier);

		var cache = new AtomicReference<Optional<T>>();

		return () -> {
			Optional<T> h = cache.get();
			if (h == null) {
				h = Optional.ofNullable(supplier.get());
				if (!cache.compareAndSet(null, h)) {
					// Another thread beat us, use their value
					h = cache.get();
				}
			}
			return h.orElse(null);
		};
	}

	/**
	 * Converts the specified object into an identifiable string of the form "Class[identityHashCode]"
	 * @param o The object to convert to a string.
	 * @return An identity string.
	 */
	public static String identity(Object o) {
		if (o instanceof Optional)
			o = ((Optional<?>)o).orElse(null);
		if (o == null)
			return null;
		return sqcn(o) + "@" + System.identityHashCode(o);
	}
}