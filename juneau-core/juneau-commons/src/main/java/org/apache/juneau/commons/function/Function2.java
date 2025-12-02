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
package org.apache.juneau.commons.function;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.function.*;

/**
 * A functional interface representing a function that accepts two arguments and produces a result.
 *
 * <p>
 * This interface extends the standard Java {@link java.util.function.Function} pattern to support
 * two-argument functions. It's useful when you need to pass functions with two parameters to methods
 * that expect functional interfaces, such as in stream operations, builders, or callback patterns.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Composition support - provides {@link #andThen(Function)} for function chaining
 * 	<li>Type-safe - generic type parameters ensure compile-time type safety
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Two-argument transformations in functional programming patterns
 * 	<li>Builder methods that accept two-parameter functions
 * 	<li>Stream operations requiring two-argument functions
 * 	<li>Callback patterns with two parameters
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda expression</jc>
 * 	Function2&lt;String,Integer,String&gt; <jv>concat</jv> = (<jv>s</jv>, <jv>i</jv>) -&gt; <jv>s</jv> + <js>"-"</js> + <jv>i</jv>;
 * 	String <jv>result</jv> = <jv>concat</jv>.apply(<js>"prefix"</js>, 42);  <jc>// Returns "prefix-42"</jc>
 *
 * 	<jc>// Method reference</jc>
 * 	Function2&lt;String,String,Boolean&gt; <jv>equals</jv> = String::equals;
 * 	<jk>boolean</jk> <jv>match</jv> = <jv>equals</jv>.apply(<js>"hello"</js>, <js>"hello"</js>);  <jc>// Returns true</jc>
 *
 * 	<jc>// Function composition</jc>
 * 	Function2&lt;Integer,Integer,Integer&gt; <jv>add</jv> = (<jv>a</jv>, <jv>b</jv>) -&gt; <jv>a</jv> + <jv>b</jv>;
 * 	Function2&lt;Integer,Integer,String&gt; <jv>addAndFormat</jv> = <jv>add</jv>.andThen(Object::toString);
 * 	String <jv>sum</jv> = <jv>addAndFormat</jv>.apply(5, 3);  <jc>// Returns "8"</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Function3} - Three-argument function
 * 	<li class='jc'>{@link Function4} - Four-argument function
 * 	<li class='jc'>{@link Function5} - Five-argument function
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsFunction">juneau-commons-function</a>
 * </ul>
 *
 * @param <A> The type of the first argument to the function.
 * @param <B> The type of the second argument to the function.
 * @param <R> The type of the result of the function.
 */
@FunctionalInterface
public interface Function2<A,B,R> {

	/**
	 * Returns a composed function that first applies this function to its input, and then applies
	 * the {@code after} function to the result.
	 *
	 * <p>
	 * This method enables function composition, allowing you to chain multiple transformations together.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Function2&lt;Integer,Integer,Integer&gt; <jv>multiply</jv> = (<jv>a</jv>, <jv>b</jv>) -&gt; <jv>a</jv> * <jv>b</jv>;
	 * 	Function2&lt;Integer,Integer,String&gt; <jv>multiplyAndFormat</jv> = <jv>multiply</jv>.andThen(<jv>n</jv> -&gt; <js>"Result: "</js> + <jv>n</jv>);
	 * 	String <jv>result</jv> = <jv>multiplyAndFormat</jv>.apply(6, 7);  <jc>// Returns "Result: 42"</jc>
	 * </p>
	 *
	 * @param <V> The type of output of the {@code after} function, and of the composed function.
	 * @param after The function to apply after this function is applied. Must not be <jk>null</jk>.
	 * @return A composed function that first applies this function and then applies the {@code after} function.
	 * @throws NullPointerException if {@code after} is <jk>null</jk>.
	 */
	default <V> Function2<A,B,V> andThen(Function<? super R,? extends V> after) {
		assertArgNotNull("after", after);
		return (A a, B b) -> after.apply(apply(a, b));
	}

	/**
	 * Applies this function to the given arguments.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Function2&lt;String,Integer,String&gt; <jv>repeat</jv> = (<jv>s</jv>, <jv>n</jv>) -&gt; <jv>s</jv>.repeat(<jv>n</jv>);
	 * 	String <jv>result</jv> = <jv>repeat</jv>.apply(<js>"ha"</js>, 3);  <jc>// Returns "hahaha"</jc>
	 * </p>
	 *
	 * @param a The first function argument.
	 * @param b The second function argument.
	 * @return The function result.
	 */
	R apply(A a, B b);
}