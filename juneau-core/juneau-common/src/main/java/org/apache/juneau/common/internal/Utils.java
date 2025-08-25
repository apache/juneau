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
import java.util.stream.*;

public class Utils {

	/** Constructor */
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
	 * @param <T> The type to convert the value to.
	 * @param name The property name.
	 * @param def The default value if not found.
	 * @return The default value.
	 */
	public static <T> T env(String name, T def) {
		return env(name).map(x -> toType(x, def)).orElse(def);
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

	private static boolean isArray(Object o) {
		return o != null && o.getClass().isArray();
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
	 * Same as MessageFormat.format().
	 */
	public static String f(String pattern, Object...args) {
		if (args.length == 0)
			return pattern;
		return MessageFormat.format(pattern, args);
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
	 * Shortcut for creating a modifiable list out of an array of values.
	 */
	@SafeVarargs
	public static <T> List<T> list(T...values) {  // NOSONAR
		return new ArrayList<>(Arrays.asList(values));
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
		return s == null ? Collections.emptyList() : StringUtils.split2(s);
	}

	/**
	 * Splits a comma-delimited list into an array of strings.
	 */
	public static String[] splita(String s) {
		return s == null ? new String[0] : StringUtils.split(s);
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

	/**
	 * Shortcut for creating an unmodifiable list out of an array of values.
	 */
	@SafeVarargs
	public static <T> List<T> alist(T...values) {  // NOSONAR
		return Arrays.asList(values);
	}

	private static final Map<Class<?>,Function<String,?>> ENV_FUNCTIONS = new IdentityHashMap<>();
	static {
		ENV_FUNCTIONS.put(Boolean.class, Boolean::valueOf);
		ENV_FUNCTIONS.put(Charset.class, Charset::forName);
	}

	private static final ConcurrentHashMap<String,String> PROPERTY_TO_ENV = new ConcurrentHashMap<>();

	private static String envName(String name) {
		return PROPERTY_TO_ENV.computeIfAbsent(name, x->x.toUpperCase().replace(".", "_"));
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
	 * Simplified formatted string supplier with message arguments.
	 */
	public static Supplier<String> fs(String pattern, Object...args) {
		return ()->StringUtils.format(pattern, args);
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
	@SuppressWarnings("rawtypes")
	public static boolean isEmpty(Object o) {
		if (o == null)
			return true;
		if (o instanceof Collection)
			return ((Collection)o).isEmpty();
		if (o instanceof Map)
			return ((Map)o).isEmpty();
		if (o.getClass().isArray())
			return (Array.getLength(o) == 0);
		return o.toString().isEmpty();
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
	 * Returns <jk>true</jk> if the specified boolean is not <jk>null</jk> and is <jk>true</jk>.
	 *
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified boolean is not <jk>null</jk> and is <jk>true</jk>.
	 */
	public static boolean isTrue(Boolean value) {
		return value != null && value;
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
		return StringUtils.isNotEmpty(s(value));
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

	public static <K,V> Map<K,V> u(Map<? extends K, ? extends V> value) {
		return value == null ? null : Collections.unmodifiableMap(value);
	}

	public static <T> List<T> u(List<? extends T> value) {
		return value == null ? null : Collections.unmodifiableList(value);
	}

	public static <T> Set<T> u(Set<? extends T> value) {
		return value == null ? null : Collections.unmodifiableSet(value);
	}
}