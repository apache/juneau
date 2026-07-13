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
package org.apache.juneau.commons.utils;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;

/**
 * Terse alias facade for the domain utility classes.
 *
 * <p>A single wildcard static import
 * ({@code import static org.apache.juneau.commons.utils.Shorts.*;}) exposes the entire
 * short-name vocabulary. Almost every method here is a one-line delegation to a canonical,
 * fully-descriptive method on a domain class — behavior is identical to calling the canonical
 * method directly — and each such alias documents its target via an {@code @see} tag. Canonical
 * implementations live in {@link ObjectUtils}, {@link StringUtils}, {@link CollectionUtils},
 * {@link ClassUtils}, {@link ThrowableUtils}, {@link IoUtils}, {@link FileUtils},
 * {@link AssertionUtils}, {@link PredicateUtils}, {@link SystemUtils}, {@link DateUtils}, and
 * {@link org.apache.juneau.commons.reflect.ReflectionUtils}.
 *
 * <p><b>Documented exception:</b> the terse exception factories ({@code rex}/{@code brex}/
 * {@code iaex}/{@code isex}/{@code uoex}/{@code uoroex}/{@code ioex}/{@code exex}) are
 * self-contained — they construct the exception directly rather than delegating to a domain
 * class — so that the domain {@code *Utils} classes can stay {@code Shorts}-free (they use the
 * package-private {@code Exceptions} helper instead). These are the only methods in this class
 * with no {@code @see} tag.
 *
 * <p><b>Disjointness invariant:</b> no {@code Shorts} alias name is identical to the canonical
 * (full) method name of any method in any domain class. This lets a developer wildcard-import
 * {@code Shorts.*} alongside any single domain {@code *Utils.*} with zero method-name
 * collisions.
 *
 * <p>This class is the replacement for the former {@code Utils} grab-bag (now deleted): callers
 * replace {@code import static ...utils.Utils.*} with {@code import static ...utils.Shorts.*}.
 */
@SuppressWarnings({
	"java:S1118" // Utility facade with static methods only.
})
public class Shorts {

	/** Constructor — this class is meant to be subclassed. */
	protected Shorts() {}

	// ---- ObjectUtils ----

	/**
	 * Returns <jk>true</jk> if the specified object is not null.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the object is not null.
	 * @see ObjectUtils#isNotNull(Object)
	 */
	public static boolean nn(Object o) { return ObjectUtils.isNotNull(o); }

	/**
	 * Returns <jk>true</jk> if the specified value is null.
	 *
	 * @param <T> The value type.
	 * @param v The value to check.
	 * @return <jk>true</jk> if the value is null.
	 * @see ObjectUtils#isNull(Object)
	 */
	public static <T> boolean n(T v) { return ObjectUtils.isNull(v); }

	/**
	 * Returns <jk>true</jk> if the specified array is null or any of its elements are null.
	 *
	 * @param v The values to check.
	 * @return <jk>true</jk> if the array is null or contains a null element.
	 * @see ObjectUtils#isNull(Object...)
	 */
	public static boolean n(Object...v) { return ObjectUtils.isNull(v); }

	/**
	 * Returns <jk>true</jk> if the two objects are equal (null-safe).
	 *
	 * @param <T> The object type.
	 * @param a The first object.
	 * @param b The second object.
	 * @return <jk>true</jk> if the objects are equal.
	 * @see ObjectUtils#equal(Object,Object)
	 */
	public static <T> boolean eq(T a, T b) { return ObjectUtils.equal(a, b); }

	/**
	 * Returns <jk>true</jk> if the two strings are equal, optionally ignoring case.
	 *
	 * @param ci If <jk>true</jk>, comparison is case-insensitive.
	 * @param a The first string.
	 * @param b The second string.
	 * @return <jk>true</jk> if the strings are equal.
	 * @see ObjectUtils#equal(boolean,String,String)
	 */
	public static boolean eq(boolean ci, String a, String b) { return ObjectUtils.equal(ci, a, b); }

	/**
	 * Returns <jk>true</jk> if the two objects are equal using the specified equality test.
	 *
	 * @param <T> The first object type.
	 * @param <U> The second object type.
	 * @param a The first object.
	 * @param b The second object.
	 * @param t The equality predicate.
	 * @return <jk>true</jk> if the objects are equal per the predicate.
	 * @see ObjectUtils#equal(Object,Object,BiPredicate)
	 */
	public static <T,U> boolean eq(T a, U b, BiPredicate<T,U> t) { return ObjectUtils.equal(a, b, t); }

	/**
	 * Returns <jk>true</jk> if the first object equals any of the remaining objects.
	 *
	 * @param <T> The object type.
	 * @param a The object to look for.
	 * @param b The candidate values.
	 * @return <jk>true</jk> if a match is found.
	 * @see ObjectUtils#equalsAny(Object,Object...)
	 */
	@SafeVarargs
	public static <T> boolean eqa(T a, T...b) { return ObjectUtils.equalsAny(a, b); }

	/**
	 * Returns <jk>true</jk> if the two objects are equal ignoring case (via their string forms).
	 *
	 * @param a The first object.
	 * @param b The second object.
	 * @return <jk>true</jk> if the objects are equal ignoring case.
	 * @see ObjectUtils#equalIgnoreCase(Object,Object)
	 */
	public static boolean eqic(Object a, Object b) { return ObjectUtils.equalIgnoreCase(a, b); }

	/**
	 * Returns <jk>true</jk> if the two objects are not equal (null-safe).
	 *
	 * @param <T> The object type.
	 * @param a The first object.
	 * @param b The second object.
	 * @return <jk>true</jk> if the objects are not equal.
	 * @see ObjectUtils#notEqual(Object,Object)
	 */
	public static <T> boolean neq(T a, T b) { return ObjectUtils.notEqual(a, b); }

	/**
	 * Returns <jk>true</jk> if the two objects are not equal using the specified equality test.
	 *
	 * @param <T> The first object type.
	 * @param <U> The second object type.
	 * @param a The first object.
	 * @param b The second object.
	 * @param t The equality predicate.
	 * @return <jk>true</jk> if the objects are not equal per the predicate.
	 * @see ObjectUtils#notEqual(Object,Object,BiPredicate)
	 */
	public static <T,U> boolean neq(T a, U b, java.util.function.BiPredicate<T,U> t) { return ObjectUtils.notEqual(a, b, t); }

	/**
	 * Compares two objects for order (null-safe).
	 *
	 * @param a The first object.
	 * @param b The second object.
	 * @return A negative, zero, or positive integer as {@code a} is less than, equal to, or greater than {@code b}.
	 * @see ObjectUtils#compare(Object,Object)
	 */
	public static int cmp(Object a, Object b) { return ObjectUtils.compare(a, b); }

	/**
	 * Returns <jk>true</jk> if the first comparable is less than the second.
	 *
	 * @param <T> The comparable type.
	 * @param a The first value.
	 * @param b The second value.
	 * @return <jk>true</jk> if {@code a < b}.
	 * @see ObjectUtils#lessThan(Comparable,Comparable)
	 */
	public static <T extends Comparable<T>> boolean lt(T a, T b) { return ObjectUtils.lessThan(a, b); }

	/**
	 * Returns <jk>true</jk> if the first comparable is less than or equal to the second.
	 *
	 * @param <T> The comparable type.
	 * @param a The first value.
	 * @param b The second value.
	 * @return <jk>true</jk> if {@code a <= b}.
	 * @see ObjectUtils#lessThanOrEqual(Comparable,Comparable)
	 */
	public static <T extends Comparable<T>> boolean lte(T a, T b) { return ObjectUtils.lessThanOrEqual(a, b); }

	/**
	 * Returns <jk>true</jk> if the first comparable is greater than the second.
	 *
	 * @param <T> The comparable type.
	 * @param a The first value.
	 * @param b The second value.
	 * @return <jk>true</jk> if {@code a > b}.
	 * @see ObjectUtils#greaterThan(Comparable,Comparable)
	 */
	public static <T extends Comparable<T>> boolean gt(T a, T b) { return ObjectUtils.greaterThan(a, b); }

	/**
	 * Returns <jk>true</jk> if the first comparable is greater than or equal to the second.
	 *
	 * @param <T> The comparable type.
	 * @param a The first value.
	 * @param b The second value.
	 * @return <jk>true</jk> if {@code a >= b}.
	 * @see ObjectUtils#greaterThanOrEqual(Comparable,Comparable)
	 */
	public static <T extends Comparable<T>> boolean gte(T a, T b) { return ObjectUtils.greaterThanOrEqual(a, b); }

	/**
	 * Returns the first non-null argument, or null if all are null.
	 *
	 * @param <T> The value type.
	 * @param t The candidate values in priority order.
	 * @return The first non-null value, or null.
	 * @see ObjectUtils#coalesce(Object...)
	 */
	@SafeVarargs
	public static <T> T or(T...t) { return ObjectUtils.coalesce(t); }

	/**
	 * Returns <jk>true</jk> if any of the specified booleans are <jk>true</jk>.
	 *
	 * @param v The booleans to check.
	 * @return <jk>true</jk> if at least one value is <jk>true</jk>.
	 * @see ObjectUtils#anyTrue(boolean...)
	 */
	@SafeVarargs
	public static boolean or(boolean...v) { return ObjectUtils.anyTrue(v); }

	/**
	 * Returns <jk>true</jk> if all of the specified booleans are <jk>true</jk>.
	 *
	 * @param v The booleans to check.
	 * @return <jk>true</jk> if every value is <jk>true</jk>.
	 * @see ObjectUtils#allTrue(boolean...)
	 */
	@SafeVarargs
	public static boolean and(boolean...v) { return ObjectUtils.allTrue(v); }

	/**
	 * Returns the logical negation of the specified boolean.
	 *
	 * @param v The value to negate.
	 * @return {@code !v}.
	 * @see ObjectUtils#negate(boolean)
	 */
	public static boolean not(boolean v) { return ObjectUtils.negate(v); }

	/**
	 * Converts the specified object to a boolean.
	 *
	 * @param v The value to convert.
	 * @return The boolean value.
	 * @see ObjectUtils#toBoolean(Object)
	 */
	public static boolean b(Object v) { return ObjectUtils.toBoolean(v); }

	/**
	 * Converts the specified object to its string representation (null-safe).
	 *
	 * @param v The value to stringify.
	 * @return The string form, or null if the value is null.
	 * @see ObjectUtils#stringify(Object)
	 */
	public static String s(Object v) { return ObjectUtils.stringify(v); }

	/**
	 * Returns the system identity string of the specified object.
	 *
	 * @param o The object.
	 * @return The identity string (class name + identity hashcode).
	 * @see ObjectUtils#identity(Object)
	 */
	public static String id(Object o) { return ObjectUtils.identity(o); }

	/**
	 * Returns <jk>true</jk> if the specified integer is within the inclusive range.
	 *
	 * @param n The value to test.
	 * @param lo The inclusive lower bound.
	 * @param hi The inclusive upper bound.
	 * @return <jk>true</jk> if {@code lo <= n <= hi}.
	 * @see ObjectUtils#isBetween(int,int,int)
	 */
	public static boolean btw(int n, int lo, int hi) { return ObjectUtils.isBetween(n, lo, hi); }

	/**
	 * Returns <jk>true</jk> if the specified number is non-null and not equal to -1.
	 *
	 * @param <T> The number type.
	 * @param v The value to test.
	 * @return <jk>true</jk> if the value is not -1.
	 * @see ObjectUtils#isNotMinusOne(Number)
	 */
	public static <T extends Number> boolean nm1(T v) { return ObjectUtils.isNotMinusOne(v); }

	/**
	 * Computes a hash code over the specified values.
	 *
	 * @param v The values to hash.
	 * @return The combined hash code.
	 * @see ObjectUtils#hash(Object...)
	 */
	public static int h(Object...v) { return ObjectUtils.hash(v); }

	/**
	 * Returns <jk>true</jk> if the specified object is null or empty (string/collection/map/array).
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the object is null or empty.
	 * @see ObjectUtils#isEmpty(Object)
	 */
	public static boolean ie(Object o) { return ObjectUtils.isEmpty(o); }

	/**
	 * Returns <jk>true</jk> if the specified object is not null and not empty.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the object is non-empty.
	 * @see ObjectUtils#isNotEmpty(Object)
	 */
	public static boolean ine(Object o) { return ObjectUtils.isNotEmpty(o); }

	/**
	 * Wraps the specified value in an {@link Optional}.
	 *
	 * @param <T> The value type.
	 * @param t The value (may be null).
	 * @return An optional containing the value, or empty if null.
	 * @see ObjectUtils#optional(Object)
	 */
	public static <T> Optional<T> o(T t) { return ObjectUtils.optional(t); }

	/**
	 * Returns the element at the specified index of a list as an {@link Optional}.
	 *
	 * @param <T> The element type.
	 * @param l The list.
	 * @param i The index.
	 * @return An optional containing the element, or empty if out of bounds.
	 * @see CollectionUtils#optionalAt(List,int)
	 */
	public static <T> Optional<T> o(List<T> l, int i) { return CollectionUtils.optionalAt(l, i); }

	/**
	 * Returns an empty {@link Optional}.
	 *
	 * @param <T> The value type.
	 * @return An empty optional.
	 * @see ObjectUtils#emptyOptional()
	 */
	public static <T> Optional<T> oe() { return ObjectUtils.emptyOptional(); }

	/**
	 * Returns the value of the optional, or the specified default if empty.
	 *
	 * @param <T> The value type.
	 * @param o The optional.
	 * @param d The default value.
	 * @return The optional's value, or {@code d} if empty.
	 * @see ObjectUtils#orElse(Optional,Object)
	 */
	public static <T> T oo(Optional<T> o, T d) { return ObjectUtils.orElse(o, d); }

	/**
	 * Returns the specified value, throwing {@link NullPointerException} if it is null.
	 *
	 * @param <T> The value type.
	 * @param v The value.
	 * @return The non-null value.
	 * @see ObjectUtils#requireNonNull(Object)
	 */
	public static <T> T rnn(T v) { return ObjectUtils.requireNonNull(v); }

	/**
	 * Returns the size of the specified object (string/collection/map/array).
	 *
	 * @param o The object.
	 * @return The size, or 0 if null.
	 * @see ObjectUtils#size(Object)
	 */
	public static int sz(Object o) { return ObjectUtils.size(o); }

	// ---- StringUtils ----

	/**
	 * Returns <jk>true</jk> if the specified character sequence is null or empty.
	 *
	 * @param s The sequence to check.
	 * @return <jk>true</jk> if null or zero-length.
	 * @see StringUtils#isEmpty(CharSequence)
	 */
	public static boolean ie(CharSequence s) { return StringUtils.isEmpty(s); }

	/**
	 * Returns <jk>true</jk> if the specified character sequence is not null and not empty.
	 *
	 * @param s The sequence to check.
	 * @return <jk>true</jk> if non-empty.
	 * @see StringUtils#isNotEmpty(CharSequence)
	 */
	public static boolean ine(CharSequence s) { return StringUtils.isNotEmpty(s); }

	/**
	 * Formats a message using {@link java.text.MessageFormat}-style {0} placeholders.
	 *
	 * @param p The pattern.
	 * @param a The arguments.
	 * @return The formatted string.
	 * @see StringUtils#format(String,Object...)
	 */
	public static String f(String p, Object...a) { return StringUtils.format(p, a); }

	/**
	 * Returns a supplier that lazily formats a message (deferred until {@link Supplier#get()}).
	 *
	 * @param p The pattern.
	 * @param a The arguments.
	 * @return A supplier of the formatted string.
	 * @see StringUtils#format(String,Object...)
	 */
	public static Supplier<String> fs(String p, Object...a) { return () -> StringUtils.format(p, a); }

	/**
	 * Returns a human-readable string representation of the specified object.
	 *
	 * @param o The object.
	 * @return The readable representation.
	 * @see StringUtils#readable(Object)
	 */
	public static String r(Object o) { return StringUtils.readable(o); }

	/**
	 * Returns <jk>true</jk> if the specified string is null, empty, or whitespace-only.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if blank.
	 * @see StringUtils#isBlank(CharSequence)
	 */
	public static boolean ib(String s) { return StringUtils.isBlank(s); }

	/**
	 * Returns <jk>true</jk> if the specified string contains non-whitespace content.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if not blank.
	 * @see StringUtils#isNotBlank(CharSequence)
	 */
	public static boolean inb(String s) { return StringUtils.isNotBlank(s); }

	/**
	 * Converts the specified string to lower case (null-safe).
	 *
	 * @param s The string.
	 * @return The lower-cased string, or null if null.
	 * @see StringUtils#lowerCase(String)
	 */
	public static String lc(String s) { return StringUtils.lowerCase(s); }

	/**
	 * Converts the specified string to upper case (null-safe).
	 *
	 * @param s The string.
	 * @return The upper-cased string, or null if null.
	 * @see StringUtils#upperCase(String)
	 */
	public static String uc(String s) { return StringUtils.upperCase(s); }

	/**
	 * Trims leading/trailing whitespace from the specified string (null-safe).
	 *
	 * @param s The string.
	 * @return The trimmed string, or null if null.
	 * @see StringUtils#trim(String)
	 */
	public static String tr(String s) { return StringUtils.trim(s); }

	/**
	 * Returns <jk>true</jk> if the string starts with the specified prefix (null-safe).
	 *
	 * @param s The string.
	 * @param p The prefix.
	 * @return <jk>true</jk> if it starts with the prefix.
	 * @see StringUtils#startsWith(String,String)
	 */
	public static boolean sw(String s, String p) { return StringUtils.startsWith(s, p); }

	/**
	 * Returns <jk>true</jk> if the string ends with the specified suffix (null-safe).
	 *
	 * @param s The string.
	 * @param x The suffix.
	 * @return <jk>true</jk> if it ends with the suffix.
	 * @see StringUtils#endsWith(String,String)
	 */
	public static boolean ew(String s, String x) { return StringUtils.endsWith(s, x); }

	/**
	 * Returns <jk>true</jk> if the string contains the specified substring (null-safe).
	 *
	 * @param s The string.
	 * @param x The substring.
	 * @return <jk>true</jk> if it contains the substring.
	 * @see StringUtils#contains(String,String)
	 */
	public static boolean co(String s, String x) { return StringUtils.contains(s, x); }

	/**
	 * Returns <jk>true</jk> if the two strings are equal ignoring case (null-safe).
	 *
	 * @param a The first string.
	 * @param b The second string.
	 * @return <jk>true</jk> if equal ignoring case.
	 * @see StringUtils#equalsIgnoreCase(String,String)
	 */
	public static boolean eqic(String a, String b) { return StringUtils.equalsIgnoreCase(a, b); }

	/**
	 * Returns <jk>true</jk> if the two strings are not equal ignoring case (null-safe).
	 *
	 * @param a The first string.
	 * @param b The second string.
	 * @return <jk>true</jk> if not equal ignoring case.
	 * @see StringUtils#notEqualsIgnoreCase(String,String)
	 */
	public static boolean neqic(String a, String b) { return StringUtils.notEqualsIgnoreCase(a, b); }

	/**
	 * Returns an empty string if the specified string is null, else the string.
	 *
	 * @param s The string.
	 * @return The string, or an empty string if null.
	 * @see StringUtils#emptyIfNull(String)
	 */
	public static String ein(String s) { return StringUtils.emptyIfNull(s); }

	/**
	 * Returns an empty string if the specified object is null, else its string form.
	 *
	 * @param o The object.
	 * @return The string form, or an empty string if null.
	 * @see StringUtils#emptyIfNull(Object)
	 */
	public static String ein(Object o) { return StringUtils.emptyIfNull(o); }

	/**
	 * Returns a blank (single-space) string if the specified string is null, else the string.
	 *
	 * @param s The string.
	 * @return The string, or a blank string if null.
	 * @see StringUtils#blankIfNull(String)
	 */
	public static String bin(String s) { return StringUtils.blankIfNull(s); }

	/**
	 * Returns null if the specified string is empty, else the string.
	 *
	 * @param s The string.
	 * @return The string, or null if empty.
	 * @see StringUtils#nullIfEmpty(String)
	 */
	public static String nie(String s) { return StringUtils.nullIfEmpty(s); }

	/**
	 * Creates a new {@link StringBuilder} initialized with the stringified form of the specified value.
	 *
	 * @param v The initial value. Can be <jk>null</jk> (renders as {@code "null"}).
	 * @return A new string builder.
	 * @see StringUtils#newStringBuilder(Object)
	 */
	public static StringBuilder sb(Object v) { return StringUtils.newStringBuilder(v); }

	/**
	 * Creates a new {@link StringBuilder} with the stringified form of each specified value appended in order.
	 *
	 * @param v The values to append in order (empty builder for no args).
	 * @return A new string builder.
	 * @see StringUtils#newStringBuilder(Object...)
	 */
	public static StringBuilder sb(Object...v) { return StringUtils.newStringBuilder(v); }

	/**
	 * Returns the fully-qualified class name of the specified object (null-safe).
	 *
	 * @param o The object.
	 * @return The class name, or null if the object is null.
	 * @see ClassUtils#className(Object)
	 */
	public static String cn(Object o) { return ClassUtils.className(o); }

	/**
	 * Returns the simple class name of the specified object (null-safe).
	 *
	 * @param o The object.
	 * @return The simple class name, or null if the object is null.
	 * @see ClassUtils#classNameSimple(Object)
	 */
	public static String cns(Object o) { return ClassUtils.classNameSimple(o); }

	/**
	 * Returns the qualified simple class name of the specified object (includes enclosing classes).
	 *
	 * @param o The object.
	 * @return The qualified simple class name.
	 * @see ClassUtils#classNameSimpleQualified(Object)
	 */
	public static String cnsq(Object o) { return ClassUtils.classNameSimpleQualified(o); }

	/**
	 * Returns the first non-empty string from the specified values.
	 *
	 * @param v The candidate strings.
	 * @return The first non-empty string, or null if all are empty.
	 * @see StringUtils#firstNonEmpty(String...)
	 */
	@SafeVarargs
	public static String fne(String...v) { return StringUtils.firstNonEmpty(v); }

	/**
	 * Returns the first non-blank string from the specified values.
	 *
	 * @param v The candidate strings.
	 * @return The first non-blank string, or null if all are blank.
	 * @see StringUtils#firstNonBlank(String...)
	 */
	@SafeVarargs
	public static String fnb(String...v) { return StringUtils.firstNonBlank(v); }

	/**
	 * Concatenates the specified strings.
	 *
	 * @param v The strings to join.
	 * @return The concatenated string.
	 * @see StringUtils#join(String...)
	 */
	@SafeVarargs
	public static String jn(String...v) { return StringUtils.join(v); }

	// ---- CollectionUtils ----

	/**
	 * Creates an array from the specified elements.
	 *
	 * @param <T> The element type.
	 * @param x The elements.
	 * @return A new array.
	 * @see CollectionUtils#array(Object...)
	 */
	@SafeVarargs
	public static <T> T[] a(T...x) { return CollectionUtils.array(x); }

	/**
	 * Creates a modifiable {@link java.util.ArrayList} from the specified elements.
	 *
	 * @param <T> The element type.
	 * @param x The elements.
	 * @return A new modifiable list.
	 * @see CollectionUtils#list(Object...)
	 */
	@SafeVarargs
	public static <T> List<T> l(T...x) { return CollectionUtils.list(x); }

	/**
	 * Creates an empty modifiable map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return An empty modifiable map.
	 * @see CollectionUtils#map()
	 */
	public static <K,V> Map<K,V> m() { return CollectionUtils.map(); }

	/**
	 * Creates a modifiable map with 1 entry.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 The key.
	 * @param v1 The value.
	 * @return A modifiable map.
	 * @see CollectionUtils#map(Object,Object)
	 */
	public static <K,V> Map<K,V> m(K k1, V v1) { return CollectionUtils.map(k1, v1); }

	/**
	 * Creates a modifiable map with 2 entries.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 The first key.
	 * @param v1 The first value.
	 * @param k2 The second key.
	 * @param v2 The second value.
	 * @return A modifiable map.
	 * @see CollectionUtils#map(Object,Object,Object,Object)
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2) { return CollectionUtils.map(k1, v1, k2, v2); }

	/**
	 * Creates a modifiable map with 3 entries.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 The first key.
	 * @param v1 The first value.
	 * @param k2 The second key.
	 * @param v2 The second value.
	 * @param k3 The third key.
	 * @param v3 The third value.
	 * @return A modifiable map.
	 * @see CollectionUtils#map(Object,Object,Object,Object,Object,Object)
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3) { return CollectionUtils.map(k1, v1, k2, v2, k3, v3); }

	/**
	 * Creates a modifiable map with 4 entries (keys/values in alternating order).
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1. @param v1 Value 1. @param k2 Key 2. @param v2 Value 2.
	 * @param k3 Key 3. @param v3 Value 3. @param k4 Key 4. @param v4 Value 4.
	 * @return A modifiable map.
	 * @see CollectionUtils#map(Object,Object,Object,Object,Object,Object,Object,Object)
	 */
	@SuppressWarnings({
		"java:S107" // Fixed-arity terse map-factory overload; the many parameters are intentional alternating key/value pairs mirroring map entries.
	})
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) { return CollectionUtils.map(k1, v1, k2, v2, k3, v3, k4, v4); }

	/**
	 * Creates a modifiable map with 5 entries (keys/values in alternating order).
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1. @param v1 Value 1. @param k2 Key 2. @param v2 Value 2. @param k3 Key 3. @param v3 Value 3.
	 * @param k4 Key 4. @param v4 Value 4. @param k5 Key 5. @param v5 Value 5.
	 * @return A modifiable map.
	 * @see CollectionUtils#map(Object,Object,Object,Object,Object,Object,Object,Object,Object,Object)
	 */
	@SuppressWarnings({
		"java:S107" // Fixed-arity terse map-factory overload; the many parameters are intentional alternating key/value pairs mirroring map entries.
	})
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) { return CollectionUtils.map(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5); }

	/**
	 * Creates a modifiable map with 6 entries (keys/values in alternating order).
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1. @param v1 Value 1. @param k2 Key 2. @param v2 Value 2. @param k3 Key 3. @param v3 Value 3.
	 * @param k4 Key 4. @param v4 Value 4. @param k5 Key 5. @param v5 Value 5. @param k6 Key 6. @param v6 Value 6.
	 * @return A modifiable map.
	 * @see CollectionUtils#map(Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object)
	 */
	@SuppressWarnings({
		"java:S107" // Fixed-arity terse map-factory overload; the many parameters are intentional alternating key/value pairs mirroring map entries.
	})
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) { return CollectionUtils.map(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6); }

	/**
	 * Creates a modifiable map with 7 entries (keys/values in alternating order).
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1. @param v1 Value 1. @param k2 Key 2. @param v2 Value 2. @param k3 Key 3. @param v3 Value 3. @param k4 Key 4. @param v4 Value 4.
	 * @param k5 Key 5. @param v5 Value 5. @param k6 Key 6. @param v6 Value 6. @param k7 Key 7. @param v7 Value 7.
	 * @return A modifiable map.
	 * @see CollectionUtils#map(Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object)
	 */
	@SuppressWarnings({
		"java:S107" // Fixed-arity terse map-factory overload; the many parameters are intentional alternating key/value pairs mirroring map entries.
	})
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) { return CollectionUtils.map(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7); }

	/**
	 * Creates a modifiable map with 8 entries (keys/values in alternating order).
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1. @param v1 Value 1. @param k2 Key 2. @param v2 Value 2. @param k3 Key 3. @param v3 Value 3. @param k4 Key 4. @param v4 Value 4.
	 * @param k5 Key 5. @param v5 Value 5. @param k6 Key 6. @param v6 Value 6. @param k7 Key 7. @param v7 Value 7. @param k8 Key 8. @param v8 Value 8.
	 * @return A modifiable map.
	 * @see CollectionUtils#map(Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object)
	 */
	@SuppressWarnings({
		"java:S107" // Fixed-arity terse map-factory overload; the many parameters are intentional alternating key/value pairs mirroring map entries.
	})
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) { return CollectionUtils.map(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8); }

	/**
	 * Creates a modifiable map with 9 entries (keys/values in alternating order).
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1. @param v1 Value 1. @param k2 Key 2. @param v2 Value 2. @param k3 Key 3. @param v3 Value 3. @param k4 Key 4. @param v4 Value 4. @param k5 Key 5. @param v5 Value 5.
	 * @param k6 Key 6. @param v6 Value 6. @param k7 Key 7. @param v7 Value 7. @param k8 Key 8. @param v8 Value 8. @param k9 Key 9. @param v9 Value 9.
	 * @return A modifiable map.
	 * @see CollectionUtils#map(Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object)
	 */
	@SuppressWarnings({
		"java:S107" // Fixed-arity terse map-factory overload; the many parameters are intentional alternating key/value pairs mirroring map entries.
	})
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) { return CollectionUtils.map(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9); }

	/**
	 * Creates a modifiable map with 10 entries (keys/values in alternating order).
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1. @param v1 Value 1. @param k2 Key 2. @param v2 Value 2. @param k3 Key 3. @param v3 Value 3. @param k4 Key 4. @param v4 Value 4. @param k5 Key 5. @param v5 Value 5.
	 * @param k6 Key 6. @param v6 Value 6. @param k7 Key 7. @param v7 Value 7. @param k8 Key 8. @param v8 Value 8. @param k9 Key 9. @param v9 Value 9. @param k10 Key 10. @param v10 Value 10.
	 * @return A modifiable map.
	 * @see CollectionUtils#map(Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object,Object)
	 */
	@SuppressWarnings({
		"java:S107" // Fixed-arity terse map-factory overload; the many parameters are intentional alternating key/value pairs mirroring map entries.
	})
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) { return CollectionUtils.map(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10); }

	/**
	 * Returns the element at the specified index of a list (null-safe).
	 *
	 * @param <E> The element type.
	 * @param l The list.
	 * @param i The index.
	 * @return The element, or null if out of bounds.
	 * @see CollectionUtils#elementAt(List,int)
	 */
	public static <E> E at(List<E> l, int i) { return CollectionUtils.elementAt(l, i); }

	/**
	 * Creates an empty (zero-length) array of the specified component type.
	 *
	 * @param <T> The component type.
	 * @param t The component class.
	 * @return An empty typed array.
	 * @see CollectionUtils#nullArray(Class)
	 */
	public static <T> T[] na(Class<T> t) { return CollectionUtils.nullArray(t); }

	/**
	 * Returns null if the specified map is empty, else the map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param v The map.
	 * @return The map, or null if empty.
	 * @see CollectionUtils#nullIfEmpty(Map)
	 */
	public static <K,V> Map<K,V> nie(Map<K,V> v) { return CollectionUtils.nullIfEmpty(v); }

	/**
	 * Returns null if the specified list is empty, else the list.
	 *
	 * @param <E> The element type.
	 * @param v The list.
	 * @return The list, or null if empty.
	 * @see CollectionUtils#nullIfEmpty(List)
	 */
	public static <E> List<E> nie(List<E> v) { return CollectionUtils.nullIfEmpty(v); }

	/**
	 * Returns null if the specified set is empty, else the set.
	 *
	 * @param <E> The element type.
	 * @param v The set.
	 * @return The set, or null if empty.
	 * @see CollectionUtils#nullIfEmpty(Set)
	 */
	public static <E> Set<E> nie(Set<E> v) { return CollectionUtils.nullIfEmpty(v); }

	/**
	 * Returns <jk>true</jk> if the specified collection is null or empty.
	 *
	 * @param c The collection.
	 * @return <jk>true</jk> if null or empty.
	 * @see CollectionUtils#isEmpty(Collection)
	 */
	public static boolean ie(Collection<?> c) { return CollectionUtils.isEmpty(c); }

	/**
	 * Returns <jk>true</jk> if the specified map is null or empty.
	 *
	 * @param m The map.
	 * @return <jk>true</jk> if null or empty.
	 * @see CollectionUtils#isEmpty(Map)
	 */
	public static boolean ie(Map<?,?> m) { return CollectionUtils.isEmpty(m); }

	/**
	 * Returns <jk>true</jk> if the specified collection is not null and not empty.
	 *
	 * @param c The collection.
	 * @return <jk>true</jk> if non-empty.
	 * @see CollectionUtils#isNotEmpty(Collection)
	 */
	public static boolean ine(Collection<?> c) { return CollectionUtils.isNotEmpty(c); }

	/**
	 * Returns <jk>true</jk> if the specified map is not null and not empty.
	 *
	 * @param m The map.
	 * @return <jk>true</jk> if non-empty.
	 * @see CollectionUtils#isNotEmpty(Map)
	 */
	public static boolean ine(Map<?,?> m) { return CollectionUtils.isNotEmpty(m); }

	/**
	 * Creates a 2-dimensional array from the specified rows.
	 *
	 * @param <E> The element type.
	 * @param v The rows.
	 * @return A 2-dimensional array.
	 * @see CollectionUtils#array2d(Object[][])
	 */
	@SafeVarargs
	public static <E> E[][] a2(E[]...v) { return CollectionUtils.array2d(v); }

	/**
	 * Creates an {@code Object[]} from the specified elements.
	 *
	 * @param v The elements.
	 * @return A new object array.
	 * @see CollectionUtils#objectArray(Object...)
	 */
	public static Object[] ao(Object...v) { return CollectionUtils.objectArray(v); }

	/**
	 * Creates a mutable {@link LinkedList} from the specified elements.
	 *
	 * @param <T> The element type.
	 * @param v The elements.
	 * @return A new linked list.
	 * @see CollectionUtils#linkedList(Object...)
	 */
	@SafeVarargs
	public static <T> List<T> ll(T...v) { return CollectionUtils.linkedList(v); }

	/**
	 * Creates a mutable {@link HashSet} from the specified elements.
	 *
	 * @param <T> The element type.
	 * @param v The elements.
	 * @return A new hash set.
	 * @see CollectionUtils#hashSet(Object...)
	 */
	@SafeVarargs
	public static <T> Set<T> hs(T...v) { return CollectionUtils.hashSet(v); }

	/**
	 * Creates a modifiable, insertion-ordered {@link java.util.LinkedHashSet} from the specified elements.
	 *
	 * @param <T> The element type.
	 * @param v The elements.
	 * @return A new modifiable set.
	 * @see CollectionUtils#set(Object...)
	 */
	@SafeVarargs
	public static <T> Set<T> st(T...v) { return CollectionUtils.set(v); }

	/**
	 * Creates a sorted {@link java.util.TreeSet} from the specified elements (null values filtered out).
	 *
	 * @param <E> The element type.
	 * @param v The elements.
	 * @return A new sorted set.
	 * @see CollectionUtils#sortedSet(Object...)
	 */
	@SafeVarargs
	public static <E> SortedSet<E> ss(E...v) { return CollectionUtils.sortedSet(v); }

	/**
	 * Returns an unmodifiable view of the specified list.
	 *
	 * @param <T> The element type.
	 * @param v The list.
	 * @return An unmodifiable list.
	 * @see CollectionUtils#unmodifiable(List)
	 */
	public static <T> List<T> u(List<? extends T> v) { return CollectionUtils.unmodifiable(v); }

	/**
	 * Returns an unmodifiable view of the specified map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param v The map.
	 * @return An unmodifiable map.
	 * @see CollectionUtils#unmodifiable(Map)
	 */
	public static <K,V> Map<K,V> u(Map<? extends K,? extends V> v) { return CollectionUtils.unmodifiable(v); }

	/**
	 * Returns an unmodifiable view of the specified set.
	 *
	 * @param <T> The element type.
	 * @param v The set.
	 * @return An unmodifiable set.
	 * @see CollectionUtils#unmodifiable(Set)
	 */
	public static <T> Set<T> u(Set<? extends T> v) { return CollectionUtils.unmodifiable(v); }

	/**
	 * Returns an unmodifiable view of the specified sorted set.
	 *
	 * @param <T> The element type.
	 * @param v The sorted set.
	 * @return An unmodifiable sorted set.
	 * @see CollectionUtils#unmodifiable(SortedSet)
	 */
	public static <T> SortedSet<T> u(SortedSet<T> v) { return CollectionUtils.unmodifiable(v); }

	/**
	 * Returns the length/size of the specified object (array/collection/map/string).
	 *
	 * @param v The object.
	 * @return The length, or 0 if null.
	 * @see CollectionUtils#length(Object)
	 */
	public static int len(Object v) { return CollectionUtils.length(v); }

	/**
	 * Returns a mutable copy of the specified list.
	 *
	 * @param <E> The element type.
	 * @param v The list to copy.
	 * @return A new list containing the same elements.
	 * @see CollectionUtils#copyOf(List)
	 */
	public static <E> List<E> cp(List<E> v) { return CollectionUtils.copyOf(v); }

	/**
	 * Adds the specified elements to the given list and returns it.
	 *
	 * @param <E> The element type.
	 * @param v The list to add to.
	 * @param e The elements to add.
	 * @return The same list.
	 * @see CollectionUtils#addAll(List,Object...)
	 */
	@SafeVarargs
	public static <E> List<E> aa(List<E> v, E...e) { return CollectionUtils.addAll(v, e); }

	/**
	 * Creates an {@code int[]} from the specified values.
	 *
	 * @param v The values.
	 * @return A new int array.
	 * @see CollectionUtils#intArray(int...)
	 */
	public static int[] ints(int...v) { return CollectionUtils.intArray(v); }

	/**
	 * Creates a {@code long[]} from the specified values.
	 *
	 * @param v The values.
	 * @return A new long array.
	 * @see CollectionUtils#longArray(long...)
	 */
	public static long[] longs(long...v) { return CollectionUtils.longArray(v); }

	/**
	 * Creates a {@code byte[]} from the specified int values.
	 *
	 * @param v The values.
	 * @return A new byte array.
	 * @see CollectionUtils#byteArray(int...)
	 */
	public static byte[] bytes(int...v) { return CollectionUtils.byteArray(v); }

	/**
	 * Creates a {@code char[]} from the specified values.
	 *
	 * @param v The values.
	 * @return A new char array.
	 * @see CollectionUtils#charArray(char...)
	 */
	public static char[] chars(char...v) { return CollectionUtils.charArray(v); }

	/**
	 * Creates a {@code float[]} from the specified values.
	 *
	 * @param v The values.
	 * @return A new float array.
	 * @see CollectionUtils#floatArray(float...)
	 */
	public static float[] floats(float...v) { return CollectionUtils.floatArray(v); }

	/**
	 * Creates a {@code double[]} from the specified values.
	 *
	 * @param v The values.
	 * @return A new double array.
	 * @see CollectionUtils#doubleArray(double...)
	 */
	public static double[] doubles(double...v) { return CollectionUtils.doubleArray(v); }

	/**
	 * Creates a {@code boolean[]} from the specified values.
	 *
	 * @param v The values.
	 * @return A new boolean array.
	 * @see CollectionUtils#booleanArray(boolean...)
	 */
	public static boolean[] booleans(boolean...v) { return CollectionUtils.booleanArray(v); }

	/**
	 * Creates a {@code short[]} from the specified int values.
	 *
	 * @param v The values.
	 * @return A new short array.
	 * @see CollectionUtils#shortArray(int...)
	 */
	public static short[] shorts(int...v) { return CollectionUtils.shortArray(v); }

	/**
	 * Creates a new {@link Lists} builder for the specified element type.
	 *
	 * @param <E> The element type.
	 * @param type The element type.
	 * @return A new list builder.
	 * @see CollectionUtils#listBuilder(Class)
	 */
	public static <E> Lists<E> lb(Class<E> type) { return CollectionUtils.listBuilder(type); }

	/**
	 * Creates a new {@link Maps} builder.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new map builder.
	 * @see CollectionUtils#mapBuilder()
	 */
	public static <K,V> Maps<K,V> mb() { return CollectionUtils.mapBuilder(); }

	/**
	 * Creates a new {@link Maps} builder for the specified key and value types.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param keyType The key type.
	 * @param valueType The value type.
	 * @return A new map builder.
	 * @see CollectionUtils#mapBuilder(Class,Class)
	 */
	public static <K,V> Maps<K,V> mb(Class<K> keyType, Class<V> valueType) { return CollectionUtils.mapBuilder(keyType, valueType); }

	/**
	 * Creates a new {@link Sets} builder for the specified element type.
	 *
	 * @param <E> The element type.
	 * @param type The element type.
	 * @return A new set builder.
	 * @see CollectionUtils#setBuilder(Class)
	 */
	public static <E> Sets<E> stb(Class<E> type) { return CollectionUtils.setBuilder(type); }

	// ---- ClassUtils ----

	/**
	 * Casts the specified object to the specified type (null-safe).
	 *
	 * @param <T> The target type.
	 * @param c The target class.
	 * @param o The object to cast.
	 * @return The cast object, or null if the object is null.
	 * @see ClassUtils#castTo(Class,Object)
	 */
	public static <T> T cast(Class<T> c, Object o) { return ClassUtils.castTo(c, o); }

	/**
	 * Makes the specified method accessible.
	 *
	 * @param m The method.
	 * @return <jk>true</jk> if the method was made accessible.
	 * @see ClassUtils#setAccessible(Method)
	 */
	public static boolean sa(Method m) { return ClassUtils.setAccessible(m); }

	/**
	 * Makes the specified field accessible.
	 *
	 * @param f The field.
	 * @return <jk>true</jk> if the field was made accessible.
	 * @see ClassUtils#setAccessible(Field)
	 */
	public static boolean sa(Field f) { return ClassUtils.setAccessible(f); }

	/**
	 * Makes the specified constructor accessible.
	 *
	 * @param c The constructor.
	 * @return <jk>true</jk> if the constructor was made accessible.
	 * @see ClassUtils#setAccessible(Constructor)
	 */
	public static boolean sa(Constructor<?> c) { return ClassUtils.setAccessible(c); }

	// ---- ThrowableUtils ----

	// Self-contained exception factories (Principle A — no delegation, no log side-effect).

	/** Creates a {@link RuntimeException} with a formatted message. */
	public static RuntimeException rex(String m, Object...a) { return new RuntimeException(StringUtils.format(m, a)); }

	/** Creates a {@link RuntimeException} wrapping a cause. */
	public static RuntimeException rex(Throwable t) { return new RuntimeException(t); }

	/** Creates a {@link RuntimeException} with a cause and formatted message. */
	public static RuntimeException rex(Throwable t, String m, Object...a) { return new RuntimeException(StringUtils.format(m, a), t); }

	/** Creates an {@link IllegalArgumentException} with a formatted message. */
	public static IllegalArgumentException iaex(String m, Object...a) { return new IllegalArgumentException(StringUtils.format(m, a)); }

	/** Creates an {@link IllegalArgumentException} wrapping a cause. */
	public static IllegalArgumentException iaex(Throwable t) { return new IllegalArgumentException(t); }

	/** Creates an {@link IllegalArgumentException} with a cause and formatted message. */
	public static IllegalArgumentException iaex(Throwable t, String m, Object...a) { return new IllegalArgumentException(StringUtils.format(m, a), t); }

	/** Creates a {@link BeanRuntimeException} with a formatted message. */
	public static BeanRuntimeException brex(String m, Object...a) { return new BeanRuntimeException(m, a); }

	/** Creates a {@link BeanRuntimeException} wrapping a cause. */
	public static BeanRuntimeException brex(Throwable t) { return new BeanRuntimeException(t); }

	/** Creates a {@link BeanRuntimeException} with a cause and formatted message. */
	public static BeanRuntimeException brex(Throwable t, String m, Object...a) { return new BeanRuntimeException(StringUtils.format(m, a), t); }

	/** Creates a {@link BeanRuntimeException} with an associated class and formatted message. */
	public static BeanRuntimeException brex(Class<?> c, String m, Object...a) { return new BeanRuntimeException(c, m, a); }

	/** Creates a {@link BeanRuntimeException} with an associated class and formatted message. */
	public static BeanRuntimeException brex(ClassInfo c, String m, Object...a) { return new BeanRuntimeException(c.inner(), m, a); }

	/** Creates a {@link BeanRuntimeException} with a cause, associated class, and formatted message. */
	public static BeanRuntimeException brex(Throwable t, Class<?> c, String m, Object...a) { return new BeanRuntimeException(t, c, m, a); }

	/** Creates a {@link BeanRuntimeException} with a cause, associated class, and formatted message. */
	public static BeanRuntimeException brex(Throwable t, ClassInfo c, String m, Object...a) { return new BeanRuntimeException(t, c.inner(), m, a); }

	/** Creates an {@link IOException} with a formatted message. */
	public static IOException ioex(String m, Object...a) { return new IOException(StringUtils.format(m, a)); }

	/** Creates an {@link IOException} wrapping a cause. */
	public static IOException ioex(Throwable t) { return new IOException(t); }

	/** Creates an {@link IOException} with a cause and formatted message. */
	public static IOException ioex(Throwable t, String m, Object...a) { return new IOException(StringUtils.format(m, a), t); }

	/** Creates an {@link ExecutableException} with a formatted message. */
	public static ExecutableException exex(String m, Object...a) { return new ExecutableException(m, a); }

	/** Creates an {@link ExecutableException} wrapping a cause. */
	public static ExecutableException exex(Throwable t) { return new ExecutableException(t); }

	/** Creates an {@link ExecutableException} with a cause and formatted message. */
	public static ExecutableException exex(Throwable t, String m, Object...a) { return new ExecutableException(t, m, a); }

	/** Creates an {@link IllegalStateException} with a formatted message. */
	public static IllegalStateException isex(String m, Object...a) { return new IllegalStateException(StringUtils.format(m, a)); }

	/** Creates an {@link UnsupportedOperationException} with the message "Not supported." */
	public static UnsupportedOperationException uoex() { return new UnsupportedOperationException("Not supported."); }

	/** Creates an {@link UnsupportedOperationException} with a formatted message. */
	public static UnsupportedOperationException uoex(String m, Object...a) { return new UnsupportedOperationException(StringUtils.format(m, a)); }

	/** Creates an {@link UnsupportedOperationException} wrapping a cause. */
	public static UnsupportedOperationException uoex(Throwable t) { return new UnsupportedOperationException(t); }

	/** Creates an {@link UnsupportedOperationException} with a cause and formatted message. */
	public static UnsupportedOperationException uoex(Throwable t, String m, Object...a) { return new UnsupportedOperationException(StringUtils.format(m, a), t); }

	/** Creates an {@link UnsupportedOperationException} with the message "Object is read only." */
	public static UnsupportedOperationException uoroex() { return new UnsupportedOperationException("Object is read only."); }

	/**
	 * Returns the stack trace of the specified throwable as a string.
	 *
	 * @param t The throwable.
	 * @return The stack trace string.
	 * @see ThrowableUtils#getStackTrace(Throwable)
	 */
	public static String gst(Throwable t) { return ThrowableUtils.getStackTrace(t); }

	/**
	 * Casts the specified throwable to the specified type, wrapping if necessary.
	 *
	 * @param <T> The target type.
	 * @param c The target class.
	 * @param t The throwable.
	 * @return The cast (or wrapped) throwable.
	 * @see ThrowableUtils#castException(Class,Throwable)
	 */
	public static <T> T cex(Class<T> c, Throwable t) { return ThrowableUtils.castException(c, t); }

	/**
	 * Searches the causal chain of the specified throwable for a cause of the specified type.
	 *
	 * @param <T> The cause type.
	 * @param e The throwable to search.
	 * @param c The cause class to look for.
	 * @return An optional containing the matching cause, if found.
	 * @see ThrowableUtils#findCause(Throwable,Class)
	 */
	public static <T extends Throwable> Optional<T> fc(Throwable e, Class<T> c) { return ThrowableUtils.findCause(e, c); }

	/**
	 * Returns the first cause of the specified type from the throwable's causal chain.
	 *
	 * @param <T> The cause type.
	 * @param c The cause class to look for.
	 * @param t The throwable to search.
	 * @return The matching cause, or null if not found.
	 * @see ThrowableUtils#getThrowableCause(Class,Throwable)
	 */
	public static <T extends Throwable> T gtc(Class<T> c, Throwable t) { return ThrowableUtils.getThrowableCause(c, t); }

	/**
	 * Returns the localized message of the specified throwable (null-safe).
	 *
	 * @param t The throwable.
	 * @return The localized message, or null if the throwable is null.
	 * @see ThrowableUtils#localizedMessage(Throwable)
	 */
	public static String lm(Throwable t) { return ThrowableUtils.localizedMessage(t); }

	/**
	 * Runs the specified snippet, silently swallowing any thrown exception.
	 *
	 * @param s The snippet to run.
	 * @see ThrowableUtils#runQuietly(Snippet)
	 */
	public static void quiet(Snippet s) { ThrowableUtils.runQuietly(s); }

	// ---- PredicateUtils ----

	/**
	 * Tests the specified value against the given predicate (null-safe).
	 *
	 * @param <T> The value type.
	 * @param p The predicate.
	 * @param v The value to test.
	 * @return The result of the predicate, or <jk>false</jk> if the predicate is null.
	 * @see PredicateUtils#test(Predicate,Object)
	 */
	public static <T> boolean t(Predicate<T> p, T v) { return PredicateUtils.test(p, v); }

}
