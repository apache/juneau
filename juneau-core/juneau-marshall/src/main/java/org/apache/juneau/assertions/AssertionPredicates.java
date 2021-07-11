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
package org.apache.juneau.assertions;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.internal.*;

/**
 * Generic predicates that can be run.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Asserts that a list contains te specified values.</jc>
 * 	List&lt;Object&gt; <jv>myList</jv> = AList.<jsm>of</jsm>(...);
 * 	<jsm>assertList</jsm>(<jv>myList</jv>)
 * 		.passes(<jsm>eq</jsm>(<js>"foo"</js>), <jsm>any</jsm>(), <jsm>match</jsm>(<js>"bar*"</js>));
 * </p>
 */
public class AssertionPredicates {

	/**
	 * Predicate that always returns <jk>true</jk>.
	 *
	 * <p>
	 * Note that this typically has the same affect as a <jk>null</jk> predicate.
	 *
	 * @param <T> The object type being tested.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> any() {
		return new AssertionPredicate<>(x -> true, null);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value is not null.
	 *
	 * @param <T> The object type being tested.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> notNull() {
		return new AssertionPredicate<>(x -> x != null, "Value was null.");
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value is null.
	 *
	 * @param <T> The object type being tested.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> isNull() {
		return new AssertionPredicate<>(x -> x == null, "Value was not null.");
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value equals the specified value.
	 *
	 * <p>
	 * Uses standard Java equality for testing.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> eq(Object value) {
		return new AssertionPredicate<>(x -> Objects.equals(x, value), "Value did not match expected.  Expected=''{0}'', Actual='{VALUE}'.", value);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string matches the specified value.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> eq(String value) {
		return new AssertionPredicate<>(x -> Objects.equals(stringify(x), value), "Value did not match expected.  Expected=''{0}'', Actual='{VALUE}'.", value);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value does not match the specified value.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> ne(Object value) {
		return new AssertionPredicate<>(x -> ! Objects.equals(x, value), "Value unexpectedly matched.  Value='{VALUE}'.");
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string does not match the specified value.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> ne(String value) {
		return new AssertionPredicate<>(x -> ! Objects.equals(stringify(x), value), "Value unexpectedly matched.  Value='{VALUE}'.");
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string does not match the specified value ignoring case.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> eqic(String value) {
		return new AssertionPredicate<>(x -> StringUtils.eqic(stringify(x), value), "Value did not match expected.  Expected=''{0}'', Actual='{VALUE}'.", value);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value is the specified type.
	 *
	 * @param <T> The object type being tested.
	 * @param type The specified type.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> type(Class<?> type) {
		return new AssertionPredicate<>(x -> x.getClass().isAssignableFrom(type), "Value was not expected type  Expected=''{0}'', Actual='{VALUE}'.", type);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string matches the specified match pattern.
	 *
	 * <p>
	 * Match pattern can contain the <js>"*"</js> meta-character.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> match(String value) {
		return regex(StringUtils.getMatchPattern(value));
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string matches the specified regular expression.
	 *
	 * @param <T> The object type being tested.
	 * @param expression The regular expression to match.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> regex(String expression) {
		return regex(Pattern.compile(expression));
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string matches the specified regular expression.
	 *
	 * @param <T> The object type being tested.
	 * @param value The regular expression to match.
	 * @return A new predicate.
	 */
	public static final <T> Predicate<T> regex(Pattern value) {
		return new AssertionPredicate<>(x -> x != null && value.matcher(stringify(x)).matches(), "Value did not match pattern.  Pattern=''{0}'', Actual='{VALUE}'", value.pattern());
	}

	/**
	 * Combines the specified predicates into a singled AND'ed predicate.
	 *
	 * @param predicates The predicates to combine.
	 * @return The combined predicates.
	 */
	@SafeVarargs
	public static final <T> Predicate<T> and(Predicate<T>...predicates) {
		return new AssertionPredicate.And<>(predicates);
	}

	/**
	 * Combines the specified predicates into a singled OR'ed predicate.
	 *
	 * @param predicates The predicates to combine.
	 * @return The combined predicates.
	 */
	@SafeVarargs
	public static final <T> Predicate<T> or(Predicate<T>...predicates) {
		return new AssertionPredicate.Or<>(predicates);
	}

	/**
	 * Negates the specified predicate.
	 *
	 * @param predicate The predicate to negate.
	 * @return The combined predicates.
	 */
	public static final <T> Predicate<T> not(Predicate<T> predicate) {
		return new AssertionPredicate.Not<>(predicate);
	}
}
