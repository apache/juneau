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
 * A functional interface representing a function that accepts four arguments and produces a result.
 *
 * <p>
 * This interface extends the standard Java {@link java.util.function.Function} pattern to support
 * four-argument functions. It's useful when you need to pass functions with four parameters to methods
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
 * 	<li>Four-argument transformations in functional programming patterns
 * 	<li>Builder methods that accept four-parameter functions
 * 	<li>Stream operations requiring four-argument functions
 * 	<li>Callback patterns with four parameters
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda expression</jc>
 * 	Function4&lt;String,Integer,Boolean,Double,String&gt; <jv>format</jv> = (<jv>s</jv>, <jv>i</jv>, <jv>b</jv>, <jv>d</jv>) -&gt;
 * 		<jv>s</jv> + <js>"-"</js> + <jv>i</jv> + <js>"-"</js> + (<jv>b</jv> ? <js>"Y"</js> : <js>"N"</js>) + <js>"-"</js> + <jv>d</jv>;
 * 	String <jv>result</jv> = <jv>format</jv>.apply(<js>"prefix"</js>, 42, <jk>true</jk>, 95.5);  <jc>// Returns "prefix-42-Y-95.5"</jc>
 *
 * 	<jc>// Function composition</jc>
 * 	Function4&lt;Integer,Integer,Integer,Integer,Integer&gt; <jv>add</jv> = (<jv>a</jv>, <jv>b</jv>, <jv>c</jv>, <jv>d</jv>) -&gt; <jv>a</jv> + <jv>b</jv> + <jv>c</jv> + <jv>d</jv>;
 * 	Function4&lt;Integer,Integer,Integer,Integer,String&gt; <jv>addAndFormat</jv> = <jv>add</jv>.andThen(Object::toString);
 * 	String <jv>sum</jv> = <jv>addAndFormat</jv>.apply(5, 3, 2, 1);  <jc>// Returns "11"</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Function2} - Two-argument function
 * 	<li class='jc'>{@link Function3} - Three-argument function
 * 	<li class='jc'>{@link Function5} - Five-argument function
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonFunction">juneau-common-function</a>
 * </ul>
 *
 * @param <A> The type of the first argument to the function.
 * @param <B> The type of the second argument to the function.
 * @param <C> The type of the third argument to the function.
 * @param <D> The type of the fourth argument to the function.
 * @param <R> The type of the result of the function.
 */
@FunctionalInterface
public interface Function4<A,B,C,D,R> {

	/**
	 * Returns a composed function that first applies this function to its input, and then applies
	 * the {@code after} function to the result.
	 *
	 * <p>
	 * This method enables function composition, allowing you to chain multiple transformations together.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Function4&lt;Integer,Integer,Integer,Integer,Integer&gt; <jv>multiply</jv> = (<jv>a</jv>, <jv>b</jv>, <jv>c</jv>, <jv>d</jv>) -&gt; <jv>a</jv> * <jv>b</jv> * <jv>c</jv> * <jv>d</jv>;
	 * 	Function4&lt;Integer,Integer,Integer,Integer,String&gt; <jv>multiplyAndFormat</jv> = <jv>multiply</jv>.andThen(<jv>n</jv> -&gt; <js>"Result: "</js> + <jv>n</jv>);
	 * 	String <jv>result</jv> = <jv>multiplyAndFormat</jv>.apply(2, 3, 2, 2);  <jc>// Returns "Result: 24"</jc>
	 * </p>
	 *
	 * @param <V> The type of output of the {@code after} function, and of the composed function.
	 * @param after The function to apply after this function is applied. Must not be <jk>null</jk>.
	 * @return A composed function that first applies this function and then applies the {@code after} function.
	 * @throws NullPointerException if {@code after} is <jk>null</jk>.
	 */
	default <V> Function4<A,B,C,D,V> andThen(Function<? super R,? extends V> after) {
		assertArgNotNull("after", after);
		return (A a, B b, C c, D d) -> after.apply(apply(a, b, c, d));
	}

	/**
	 * Applies this function to the given arguments.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Function4&lt;String,Integer,Boolean,Double,String&gt; <jv>format</jv> = (<jv>s</jv>, <jv>n</jv>, <jv>upper</jv>, <jv>mult</jv>) -&gt; {
	 * 		String <jv>result</jv> = <jv>s</jv>.repeat(<jv>n</jv>);
	 * 		<jv>result</jv> = <jv>upper</jv> ? <jv>result</jv>.toUpperCase() : <jv>result</jv>;
	 * 		<jk>return</jk> <jv>result</jv> + <js>" x"</js> + <jv>mult</jv>;
	 * 	};
	 * 	String <jv>result</jv> = <jv>format</jv>.apply(<js>"ha"</js>, 2, <jk>true</jk>, 1.5);  <jc>// Returns "HAHA x1.5"</jc>
	 * </p>
	 *
	 * @param a The first function argument.
	 * @param b The second function argument.
	 * @param c The third function argument.
	 * @param d The fourth function argument.
	 * @return The function result.
	 */
	R apply(A a, B b, C c, D d);
}