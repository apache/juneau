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
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.function.*;

/**
 * A functional interface representing a function that accepts five arguments, produces a result, and may throw a checked exception.
 *
 * <p>
 * This interface extends {@link Function5} to allow the functional method
 * to throw checked exceptions. The default {@link #apply(Object, Object, Object, Object, Object)} method wraps any checked exceptions in a
 * {@link RuntimeException}, making it compatible with standard {@link Function5} usage while still allowing
 * the implementation to throw checked exceptions via {@link #applyThrows(Object, Object, Object, Object, Object)}.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Exception support - allows checked exceptions to be thrown via {@link #applyThrows(Object, Object, Object, Object, Object)}
 * 	<li>Compatible with Function5 - implements {@link Function5#apply(Object, Object, Object, Object, Object)} by wrapping exceptions
 * 	<li>Dual interface - can be used as both Function5 and ThrowingFunction5
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Five-argument transformations that may throw I/O exceptions
 * 	<li>Parsing operations that may throw validation exceptions
 * 	<li>Data conversion that may fail with checked exceptions
 * 	<li>Operations that need to propagate checked exceptions
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda with exception</jc>
 * 	ThrowingFunction5&lt;String,String,String,String,String,File&gt; <jv>fileParser</jv> = (<jv>dir</jv>, <jv>subdir1</jv>, <jv>subdir2</jv>, <jv>subdir3</jv>, <jv>name</jv>) -&gt; {
 * 		Path <jv>path</jv> = Paths.get(<jv>dir</jv>, <jv>subdir1</jv>, <jv>subdir2</jv>, <jv>subdir3</jv>, <jv>name</jv>);
 * 		<jk>if</jk> (! Files.exists(<jv>path</jv>)) {
 * 			<jk>throw new</jk> FileNotFoundException(<js>"File not found: "</js> + <jv>path</jv>);
 * 		}
 * 		<jk>return</jk> <jv>path</jv>.toFile();
 * 	};
 *
 * 	<jc>// Using applyThrows to get checked exception</jc>
 * 	<jk>try</jk> {
 * 		File <jv>file</jv> = <jv>fileParser</jv>.applyThrows(<js>"/tmp"</js>, <js>"data"</js>, <js>"2024"</js>, <js>"01"</js>, <js>"file.txt"</js>);
 * 	} <jk>catch</jk> (FileNotFoundException <jv>e</jv>) {
 * 		<jc>// Handle checked exception</jc>
 * 	}
 *
 * 	<jc>// Using apply wraps exception in RuntimeException</jc>
 * 	File <jv>file</jv> = <jv>fileParser</jv>.apply(<js>"/tmp"</js>, <js>"data"</js>, <js>"2024"</js>, <js>"01"</js>, <js>"file.txt"</js>);  <jc>// May throw RuntimeException</jc>
 * </p>
 *
 * <h5 class='section'>Exception Handling:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@link #applyThrows(Object, Object, Object, Object, Object)} - Throws checked exceptions directly
 * 	<li>{@link #apply(Object, Object, Object, Object, Object)} - Wraps checked exceptions in {@link RuntimeException}
 * 	<li>Use {@code applyThrows} when you need to handle specific checked exceptions
 * 	<li>Use {@code apply} when you want standard Function5 behavior
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThrowingFunction} - Single-argument function that throws exceptions
 * 	<li class='jc'>{@link ThrowingFunction2} - Two-argument function that throws exceptions
 * 	<li class='jc'>{@link ThrowingFunction3} - Three-argument function that throws exceptions
 * 	<li class='jc'>{@link ThrowingFunction4} - Four-argument function that throws exceptions
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsFunction">juneau-commons-function</a>
 * </ul>
 *
 * @param <A> The type of the first argument to the function.
 * @param <B> The type of the second argument to the function.
 * @param <C> The type of the third argument to the function.
 * @param <D> The type of the fourth argument to the function.
 * @param <E> The type of the fifth argument to the function.
 * @param <R> The type of the result of the function.
 */
@FunctionalInterface
public interface ThrowingFunction5<A,B,C,D,E,R> extends Function5<A,B,C,D,E,R> {

	/**
	 * Applies this function to the given arguments, wrapping any checked exceptions in a {@link RuntimeException}.
	 *
	 * <p>
	 * This is the default implementation that makes this interface compatible with {@link Function5}.
	 * It calls {@link #applyThrows(Object, Object, Object, Object, Object)} and wraps any checked exceptions.
	 *
	 * @param a The first function argument.
	 * @param b The second function argument.
	 * @param c The third function argument.
	 * @param d The fourth function argument.
	 * @param e The fifth function argument.
	 * @return The function result.
	 * @throws RuntimeException if {@link #applyThrows(Object, Object, Object, Object, Object)} throws a checked exception.
	 */
	@Override
	default R apply(A a, B b, C c, D d, E e) {
		try {
			return applyThrows(a, b, c, d, e);
		} catch (Exception ex) {
			throw toRex(ex);
		}
	}

	/**
	 * Returns a composed function that first applies this function to its input, and then applies
	 * the {@code after} function to the result.
	 *
	 * <p>
	 * This method enables function composition, allowing you to chain multiple transformations together.
	 * The composed function will throw exceptions from this function, but the {@code after} function
	 * is a standard {@link Function} that does not throw checked exceptions.
	 *
	 * @param <V> The type of output of the {@code after} function, and of the composed function.
	 * @param after The function to apply after this function is applied. Must not be <jk>null</jk>.
	 * @return A composed {@link ThrowingFunction5} that first applies this function and then applies the {@code after} function.
	 * @throws NullPointerException if {@code after} is <jk>null</jk>.
	 */
	default <V> ThrowingFunction5<A,B,C,D,E,V> andThen(Function<? super R,? extends V> after) {
		assertArgNotNull("after", after);
		return (A a, B b, C c, D d, E e) -> after.apply(applyThrows(a, b, c, d, e));
	}

	/**
	 * Applies this function to the given arguments, potentially throwing a checked exception.
	 *
	 * <p>
	 * This is the functional method that implementations must provide. It allows checked exceptions
	 * to be thrown directly, unlike the standard {@link Function5#apply(Object, Object, Object, Object, Object)} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ThrowingFunction5&lt;String,Integer,Boolean,Double,Long,Integer&gt; <jv>parser</jv> = (<jv>s</jv>, <jv>base</jv>, <jv>signed</jv>, <jv>multiplier</jv>, <jv>offset</jv>) -&gt; {
	 * 		<jk>try</jk> {
	 * 			<jk>int</jk> <jv>value</jv> = Integer.parseInt(<jv>s</jv>, <jv>base</jv>);
	 * 			<jv>value</jv> = <jv>signed</jv> ? <jv>value</jv> : Math.abs(<jv>value</jv>);
	 * 			<jk>return</jk> (<jk>int</jk>)(<jv>value</jv> * <jv>multiplier</jv> + <jv>offset</jv>);
	 * 		} <jk>catch</jk> (NumberFormatException <jv>e</jv>) {
	 * 			<jk>throw new</jk> ParseException(<js>"Invalid number: "</js> + <jv>s</jv> + <js>" in base "</js> + <jv>base</jv>, 0);
	 * 		}
	 * 	};
	 *
	 * 	<jk>try</jk> {
	 * 		<jk>int</jk> <jv>value</jv> = <jv>parser</jv>.applyThrows(<js>"123"</js>, 10, <jk>true</jk>, 2.0, 100L);
	 * 	} <jk>catch</jk> (ParseException <jv>e</jv>) {
	 * 		<jc>// Handle checked exception</jc>
	 * 	}
	 * </p>
	 *
	 * @param a The first function argument.
	 * @param b The second function argument.
	 * @param c The third function argument.
	 * @param d The fourth function argument.
	 * @param e The fifth function argument.
	 * @return The function result.
	 * @throws Exception If an error occurs during function execution.
	 */
	R applyThrows(A a, B b, C c, D d, E e) throws Exception;
}

