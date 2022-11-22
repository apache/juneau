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

import static org.apache.juneau.assertions.AssertionPredicate.*;
import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;

import java.text.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.cp.*;

/**
 * Predefined {@link AssertionPredicate} objects.
 *
 * <p>
 * Typically used wherever predicates are allowed for testing of {@link Assertion} objects such as...
 * <ul>
 * 	<li>{@link FluentObjectAssertion#is(Predicate)}
 * 	<li>{@link FluentArrayAssertion#is(Predicate...)}
 * 	<li>{@link FluentPrimitiveArrayAssertion#is(Predicate...)}
 * 	<li>{@link FluentListAssertion#isEach(Predicate...)}
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Asserts that a list contains te specified values.</jc>
 * 	List&lt;Object&gt; <jv>myList</jv> = AList.<jsm>of</jsm>(...);
 * 	<jsm>assertList</jsm>(<jv>myList</jv>)
 * 		.is(<jsm>eq</jsm>(<js>"foo"</js>), <jsm>any</jsm>(), <jsm>match</jsm>(<js>"bar*"</js>));
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Overview &gt; juneau-assertions &gt; Overview</a>
 * </ul>
 */
public class AssertionPredicates {

	private static Function<Object,String> TYPENAME = x -> x == null ? null : x.getClass().getName();

	private static final Messages MESSAGES = Messages.of(AssertionPredicates.class, "Messages");
	private static final String
		MSG_valueWasNull = MESSAGES.getString("valueWasNull"),
		MSG_valueWasNotNull = MESSAGES.getString("valueWasNotNull"),
		MSG_valueDidNotMatchExpected = MESSAGES.getString("valueDidNotMatchExpected"),
		MSG_valueDidNotContainExpected = MESSAGES.getString("valueDidNotContainExpected"),
		MSG_valueUnexpectedlyMatched = MESSAGES.getString("valueUnexpectedlyMatched"),
		MSG_valueWasNotExpectedType = MESSAGES.getString("valueWasNotExpectedType"),
		MSG_valueDidNotMatchPattern = MESSAGES.getString("valueDidNotMatchPattern");

	/**
	 * Predicate that always returns <jk>true</jk>.
	 *
	 * <p>
	 * Note that this typically has the same affect as a <jk>null</jk> predicate.
	 *
	 * @param <T> The object type being tested.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> any() {
		return test(x -> true, null);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value is not null.
	 *
	 * <p>
	 * Assertion error message is <js>"Value was null."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> notNull() {
		return test(x -> x != null, MSG_valueWasNull);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value is null.
	 *
	 * <p>
	 * Assertion error message is <js>"Value was not null."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> isNull() {
		return test(x -> x == null, MSG_valueWasNotNull);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value equals the specified value.
	 *
	 * <p>
	 * Uses standard Java equality for testing.
	 *
	 * <p>
	 * Assertion error message is <js>"Value did not match expected.  Expected='{0}', Actual='{1}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> eq(Object value) {
		return test(x -> Objects.equals(x, value), MSG_valueDidNotMatchExpected, value, VALUE);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string matches the specified value.
	 *
	 * <p>
	 * Assertion error message is <js>"Value did not match expected.  Expected='{0}', Actual='{1}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> eq(String value) {
		return test(x -> Objects.equals(stringify(x), value), MSG_valueDidNotMatchExpected, value, VALUE);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value does not match the specified value.
	 *
	 * <p>
	 * Assertion error message is <js>"Value unexpectedly matched.  Value='{0}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> ne(Object value) {
		return test(x -> ! Objects.equals(x, value), MSG_valueUnexpectedlyMatched, VALUE);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string does not match the specified value.
	 *
	 * <p>
	 * Assertion error message is <js>"Value unexpectedly matched.  Value='{0}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> ne(String value) {
		return test(x -> ! Objects.equals(stringify(x), value), MSG_valueUnexpectedlyMatched, VALUE);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string does not match the specified value ignoring case.
	 *
	 * <p>
	 * Assertion error message is <js>"Value did not match expected.  Expected='{0}', Actual='{1}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> eqic(String value) {
		return test(x -> StringUtils.eqic(stringify(x), value), MSG_valueDidNotMatchExpected, value, VALUE);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string contains the specified substring.
	 *
	 * <p>
	 * Assertion error message is <js>"Value did not contain expected.  Expected='{0}', Actual='{1}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> contains(String value) {
		return test(x -> StringUtils.contains(stringify(x), value), MSG_valueDidNotContainExpected, value, VALUE);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value is the specified or child type.
	 *
	 * <p>
	 * Assertion error message is <js>"Value was not expected type.  Expected='{0}', Actual='{1}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param type The specified type.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> type(Class<?> type) {
		assertArgNotNull("type", type);
		return test(x -> x != null && type.isAssignableFrom(x.getClass()), MSG_valueWasNotExpectedType, type, TYPENAME);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value is exactly specified type.
	 *
	 * <p>
	 * Assertion error message is <js>"Value was not expected type.  Expected='{0}', Actual='{1}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param type The specified type.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> exactType(Class<?> type) {
		assertArgNotNull("type", type);
		return test(x -> x != null && x.getClass().equals(type), MSG_valueWasNotExpectedType, type, TYPENAME);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string matches the specified match pattern.
	 *
	 * <p>
	 * Match pattern can contain the <js>"*"</js> meta-character.
	 *
	 * <p>
	 * Assertion error message is <js>"Value did not match pattern.  Pattern='{0}', Actual='{1}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param value The specified value.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> match(String value) {
		assertArgNotNull("value", value);
		Pattern p = StringUtils.getMatchPattern(value);
		return test(x -> x != null && p.matcher(stringify(x)).matches(), MSG_valueDidNotMatchPattern, value, VALUE);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string matches the specified regular expression.
	 *
	 * <p>
	 * Assertion error message is <js>"Value did not match pattern.  Pattern='{0}', Actual='{1}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param expression The regular expression to match.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> regex(String expression) {
		assertArgNotNull("expression", expression);
		Pattern p = Pattern.compile(expression);
		return test(x -> x != null && p.matcher(stringify(x)).matches(), MSG_valueDidNotMatchPattern, expression, VALUE);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string matches the specified regular expression.
	 *
	 * <p>
	 * Assertion error message is <js>"Value did not match pattern.  Pattern='{0}', Actual='{1}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param expression The regular expression to match.
	 * @param flags Match flags, a bit mask that may include:
	 * 	<ul>
	 * 		<li>{@link Pattern#CASE_INSENSITIVE CASE_INSENSITIVE}
	 * 		<li>{@link Pattern#MULTILINE MULTILINE}
	 * 		<li>{@link Pattern#DOTALL DOTALL}
	 * 		<li>{@link Pattern#UNICODE_CASE UNICODE_CASE}
	 * 		<li>{@link Pattern#CANON_EQ CANON_EQ}
	 * 		<li>{@link Pattern#UNIX_LINES UNIX_LINES}
	 * 		<li>{@link Pattern#LITERAL LITERAL}
	 * 		<li>{@link Pattern#UNICODE_CHARACTER_CLASS UNICODE_CHARACTER_CLASS}
	 * 		<li>{@link Pattern#COMMENTS COMMENTS}
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> regex(String expression, int flags) {
		assertArgNotNull("expression", expression);
		Pattern p = Pattern.compile(expression, flags);
		return test(x -> x != null && p.matcher(stringify(x)).matches(), MSG_valueDidNotMatchPattern, expression, VALUE);
	}

	/**
	 * Predicate that returns <jk>true</jk> if the tested value converted to a string matches the specified regular expression.
	 *
	 * <p>
	 * Assertion error message is <js>"Value did not match pattern.  Pattern='{0}', Actual='{1}'."</js>.
	 *
	 * @param <T> The object type being tested.
	 * @param value The regular expression to match.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> regex(Pattern value) {
		assertArgNotNull("value", value);
		return test(x -> x != null && value.matcher(stringify(x)).matches(), MSG_valueDidNotMatchPattern, value.pattern(), VALUE);
	}

	/**
	 * Predicate that wraps another predicate.
	 *
	 * <p>
	 * If the predicate extends from {@link AssertionPredicate}, the assertion error
	 * message is <js>"Value did not pass test."</js> followed by the inner assertion error.
	 * Otherwise the message is <js>"Value did not pass test.  Value='{0}'."</js>
	 *
	 * @param <T> The object type being tested.
	 * @param predicate The predicate to run.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> test(Predicate<T> predicate) {
		return new AssertionPredicate<>(predicate, null);
	}

	/**
	 * Predicate that wraps another predicate.
	 *
	 * <p>
	 * If the message specified is <jk>null</jk> and the predicate extends from {@link AssertionPredicate}, the assertion error
	 * message is <js>"Value did not pass test."</js> followed by the inner assertion error.
	 * Otherwise the message is <js>"Value did not pass test.  Value='{0}'."</js>
	 *
	 * @param <T> The object type being tested.
	 * @param predicate The predicate to run.
	 * @param msg
	 * 	The error message if predicate fails.
	 * 	<br>Supports {@link MessageFormat}-style arguments.
	 * @param args
	 * 	Optional message arguments.
	 * 	<br>Can contain {@code #VALUE} to specify the value itself as an argument.
	 * 	<br>Can contain {@link Function functions} to apply to the tested value.
	 * @return A new predicate.
	 */
	public static final <T> AssertionPredicate<T> test(Predicate<T> predicate, String msg, Object...args) {
		return new AssertionPredicate<>(predicate, msg, args);
	}

	/**
	 * Combines the specified predicates into a singled AND'ed predicate.
	 *
	 * <p>
	 * Assertion error message is <js>"Predicate test #x failed."</js> followed by
	 * the inner failed message if the failed predicate extends from {@link AssertionPredicate}.
	 *
	 * @param <T> The predicate type.
	 * @param predicates The predicates to combine.
	 * @return The combined predicates.
	 */
	@SafeVarargs
	public static final <T> AssertionPredicate<T> and(Predicate<T>...predicates) {
		return new AssertionPredicate.And<>(predicates);
	}

	/**
	 * Combines the specified predicates into a singled OR'ed predicate.
	 *
	 * <p>
	 * Assertion error message is <js>"No predicate tests passed."</js>.
	 *
	 * @param <T> The predicate type.
	 * @param predicates The predicates to combine.
	 * @return The combined predicates.
	 */
	@SafeVarargs
	public static final <T> AssertionPredicate<T> or(Predicate<T>...predicates) {
		return new AssertionPredicate.Or<>(predicates);
	}

	/**
	 * Negates the specified predicate.
	 *
	 * <p>
	 * Assertion error message is <js>"Predicate test unexpectedly passed."</js>.
	 *
	 * @param <T> The predicate type.
	 * @param predicate The predicate to negate.
	 * @return The combined predicates.
	 */
	public static final <T> AssertionPredicate<T> not(Predicate<T> predicate) {
		return new AssertionPredicate.Not<>(predicate);
	}
}
