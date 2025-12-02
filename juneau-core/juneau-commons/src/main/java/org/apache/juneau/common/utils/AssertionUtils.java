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

import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

/**
 * Utility methods for argument validation and assertion.
 *
 * <p>
 * This class provides static utility methods for validating method arguments and throwing
 * {@link IllegalArgumentException} when validation fails. These methods are designed to
 * simplify common parameter validation patterns and provide consistent error messages.
 *
 * <h5 class='section'>Features:</h5>
 * <ul>
 *   <li><b>Null checking:</b> Validate single or multiple arguments are not null
 *   <li><b>Boolean expressions:</b> Assert arbitrary boolean conditions with custom messages
 *   <li><b>String validation:</b> Check for null or blank strings
 *   <li><b>Type checking:</b> Validate class array types
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 *   <jk>import static</jk> org.apache.juneau.common.utils.AssertionUtils.*;
 *
 *   <jk>public</jk> <jk>void</jk> setFooBar(String <jv>foo</jv>, String <jv>bar</jv>) {
 *       <jc>// Validate multiple arguments at once</jc>
 *       <jsm>assertArgsNotNull</jsm>(<js>"foo"</js>, <jv>foo</jv>, <js>"bar"</js>, <jv>bar</jv>);
 *
 *       <jc>// Validate custom condition</jc>
 *       <jsm>assertArg</jsm>(<jv>foo</jv>.length() &gt; 0, <js>"Foo cannot be empty"</js>);
 *   }
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 *   <li class='link'><a class="doclink" href='../../../../../index.html#juneau-common.utils'>Overview &gt; juneau-common.utils</a>
 * </ul>
 */
public class AssertionUtils {

	/**
	 * Throws an {@link IllegalArgumentException} if the specified expression is <jk>false</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.common.utils.AssertionUtils.*;
	 *
	 *	<jk>public</jk> String setFoo(List&lt;String&gt; <jv>foo</jv>) {
	 *		<jsm>assertArg</jsm>(<jv>foo</jv> != <jk>null</jk> &amp;&amp; ! <jv>foo</jv>.isEmpty(), <js>"'foo' cannot be null or empty."</js>);
	 *		...
	 *	}
	 * </p>
	 *
	 * @param expression The boolean expression to check.
	 * @param msg The exception message.
	 * @param args The exception message args.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final void assertArg(boolean expression, String msg, Object...args) throws IllegalArgumentException {
		if (! expression)
			throw illegalArg(msg, args);
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified argument is <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.common.utils.AssertionUtils.*;
	 *
	 *	<jk>public</jk> String setFoo(String <jv>foo</jv>) {
	 *		<jsm>assertArgNotNull</jsm>(<js>"foo"</js>, <jv>foo</jv>);
	 *		...
	 *	}
	 * </p>
	 *
	 * @param <T> The argument data type.
	 * @param name The argument name.
	 * @param o The object to check.
	 * @return The same argument.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final <T> T assertArgNotNull(String name, T o) throws IllegalArgumentException {
		assertArg(o != null, "Argument ''{0}'' cannot be null.", name);
		return o;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified string is <jk>null</jk> or blank.
	 *
	 * @param name The argument name.
	 * @param o The object to check.
	 * @return The same object.
	 * @throws IllegalArgumentException Thrown if the specified string is <jk>null</jk> or blank.
	 */
	@SuppressWarnings("null")
	public static final String assertArgNotNullOrBlank(String name, String o) throws IllegalArgumentException {
		assertArg(o != null, "Argument ''{0}'' cannot be null.", name);
		assertArg(! o.isBlank(), "Argument ''{0}'' cannot be blank.", name);
		return o;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if any of the specified arguments are <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.common.utils.AssertionUtils.*;
	 *
	 *	<jk>public</jk> String setFooBar(String <jv>foo</jv>, String <jv>bar</jv>) {
	 *		<jsm>assertArgsNotNull</jsm>(<js>"foo"</js>, <jv>foo</jv>, <js>"bar"</js>, <jv>bar</jv>);
	 *		...
	 *	}
	 * </p>
	 *
	 * @param name1 The first argument name.
	 * @param o1 The first object to check.
	 * @param name2 The second argument name.
	 * @param o2 The second object to check.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final void assertArgsNotNull(String name1, Object o1, String name2, Object o2) throws IllegalArgumentException {
		assertArg(o1 != null, "Argument ''{0}'' cannot be null.", name1);
		assertArg(o2 != null, "Argument ''{0}'' cannot be null.", name2);
	}

	/**
	 * Throws an {@link IllegalArgumentException} if any of the specified arguments are <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.common.utils.AssertionUtils.*;
	 *
	 *	<jk>public</jk> String setFooBarBaz(String <jv>foo</jv>, String <jv>bar</jv>, String <jv>baz</jv>) {
	 *		<jsm>assertArgsNotNull</jsm>(<js>"foo"</js>, <jv>foo</jv>, <js>"bar"</js>, <jv>bar</jv>, <js>"baz"</js>, <jv>baz</jv>);
	 *		...
	 *	}
	 * </p>
	 *
	 * @param name1 The first argument name.
	 * @param o1 The first object to check.
	 * @param name2 The second argument name.
	 * @param o2 The second object to check.
	 * @param name3 The third argument name.
	 * @param o3 The third object to check.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final void assertArgsNotNull(String name1, Object o1, String name2, Object o2, String name3, Object o3) throws IllegalArgumentException {
		assertArg(o1 != null, "Argument ''{0}'' cannot be null.", name1);
		assertArg(o2 != null, "Argument ''{0}'' cannot be null.", name2);
		assertArg(o3 != null, "Argument ''{0}'' cannot be null.", name3);
	}

	/**
	 * Throws an {@link IllegalArgumentException} if any of the specified arguments are <jk>null</jk>.
	 *
	 * @param name1 The first argument name.
	 * @param o1 The first object to check.
	 * @param name2 The second argument name.
	 * @param o2 The second object to check.
	 * @param name3 The third argument name.
	 * @param o3 The third object to check.
	 * @param name4 The fourth argument name.
	 * @param o4 The fourth object to check.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final void assertArgsNotNull(String name1, Object o1, String name2, Object o2, String name3, Object o3, String name4, Object o4) throws IllegalArgumentException {
		assertArg(o1 != null, "Argument ''{0}'' cannot be null.", name1);
		assertArg(o2 != null, "Argument ''{0}'' cannot be null.", name2);
		assertArg(o3 != null, "Argument ''{0}'' cannot be null.", name3);
		assertArg(o4 != null, "Argument ''{0}'' cannot be null.", name4);
	}

	/**
	 * Throws an {@link IllegalArgumentException} if any of the specified arguments are <jk>null</jk>.
	 *
	 * @param name1 The first argument name.
	 * @param o1 The first object to check.
	 * @param name2 The second argument name.
	 * @param o2 The second object to check.
	 * @param name3 The third argument name.
	 * @param o3 The third object to check.
	 * @param name4 The fourth argument name.
	 * @param o4 The fourth object to check.
	 * @param name5 The fifth argument name.
	 * @param o5 The fifth object to check.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final void assertArgsNotNull(String name1, Object o1, String name2, Object o2, String name3, Object o3, String name4, Object o4, String name5, Object o5)
		throws IllegalArgumentException {
		assertArg(o1 != null, "Argument ''{0}'' cannot be null.", name1);
		assertArg(o2 != null, "Argument ''{0}'' cannot be null.", name2);
		assertArg(o3 != null, "Argument ''{0}'' cannot be null.", name3);
		assertArg(o4 != null, "Argument ''{0}'' cannot be null.", name4);
		assertArg(o5 != null, "Argument ''{0}'' cannot be null.", name5);
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified value doesn't have all subclasses of the specified type.
	 *
	 * @param <E> The element type.
	 * @param name The argument name.
	 * @param type The expected parent class.
	 * @param value The array value being checked.
	 * @return The value cast to the specified array type.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	@SuppressWarnings("unchecked")
	public static final <E> Class<E>[] assertClassArrayArgIsType(String name, Class<E> type, Class<?>[] value) throws IllegalArgumentException {
		for (var i = 0; i < value.length; i++)
			if (! type.isAssignableFrom(value[i]))
				throw illegalArg("Arg {0} did not have arg of type {1} at index {2}: {3}", name, cn(type), i, cn(value[i]));
		return (Class<E>[])value;
	}

	/**
	 * Throws an {@link AssertionError} if the specified actual value is not one of the expected values.
	 *
	 * @param <T> The value type.
	 * @param actual The actual value.
	 * @param expected The expected values.
	 * @return The actual value if it matches one of the expected values.
	 * @throws AssertionError if the value is not one of the expected values.
	 */
	@SafeVarargs
	public static final <T> T assertOneOf(T actual, T...expected) {
		for (var e : expected) {
			if (eq(actual, e))
				return actual;
		}
		throw new AssertionError("Invalid value specified: " + actual);
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified varargs array or any of its elements are <jk>null</jk>.
	 *
	 * @param <T> The element type.
	 * @param name The argument name.
	 * @param o The object to check.
	 * @return The same object.
	 * @throws IllegalArgumentException Thrown if the specified varargs array or any of its elements are <jk>null</jk>.
	 */
	@SuppressWarnings("null")
	public static final <T> T[] assertVarargsNotNull(String name, T[] o) throws IllegalArgumentException {
		assertArg(o != null, "Argument ''{0}'' cannot be null.", name);
		for (var i = 0; i < o.length; i++)
			assertArg(nn(o[i]), "Argument ''{0}'' parameter {1} cannot be null.", name, i);
		return o;
	}

}
