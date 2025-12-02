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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.function.*;

/**
 * A functional interface representing a function that accepts one argument, produces a result, and may throw a checked exception.
 *
 * <p>
 * This interface extends the standard Java {@link java.util.function.Function} to allow the functional method
 * to throw checked exceptions. The default {@link #apply(Object)} method wraps any checked exceptions in a
 * {@link RuntimeException}, making it compatible with standard {@link Function} usage while still allowing
 * the implementation to throw checked exceptions via {@link #applyThrows(Object)}.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Exception support - allows checked exceptions to be thrown via {@link #applyThrows(Object)}
 * 	<li>Compatible with Function - implements {@link Function#apply(Object)} by wrapping exceptions
 * 	<li>Dual interface - can be used as both Function and ThrowingFunction
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Transformations that may throw I/O exceptions
 * 	<li>Parsing operations that may throw validation exceptions
 * 	<li>Data conversion that may fail with checked exceptions
 * 	<li>Operations that need to propagate checked exceptions
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda with exception</jc>
 * 	ThrowingFunction&lt;String,File&gt; <jv>fileParser</jv> = (<jv>path</jv>) -&gt; {
 * 		<jk>if</jk> (! Files.exists(Paths.get(<jv>path</jv>))) {
 * 			<jk>throw new</jk> FileNotFoundException(<js>"File not found: "</js> + <jv>path</jv>);
 * 		}
 * 		<jk>return new</jk> File(<jv>path</jv>);
 * 	};
 *
 * 	<jc>// Using applyThrows to get checked exception</jc>
 * 	<jk>try</jk> {
 * 		File <jv>file</jv> = <jv>fileParser</jv>.applyThrows(<js>"/path/to/file"</js>);
 * 	} <jk>catch</jk> (FileNotFoundException <jv>e</jv>) {
 * 		<jc>// Handle checked exception</jc>
 * 	}
 *
 * 	<jc>// Using apply wraps exception in RuntimeException</jc>
 * 	File <jv>file</jv> = <jv>fileParser</jv>.apply(<js>"/path/to/file"</js>);  <jc>// May throw RuntimeException</jc>
 * </p>
 *
 * <h5 class='section'>Exception Handling:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@link #applyThrows(Object)} - Throws checked exceptions directly
 * 	<li>{@link #apply(Object)} - Wraps checked exceptions in {@link RuntimeException}
 * 	<li>Use {@code applyThrows} when you need to handle specific checked exceptions
 * 	<li>Use {@code apply} when you want standard Function behavior
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThrowingSupplier} - Supplier that throws exceptions
 * 	<li class='jc'>{@link ThrowingConsumer} - Consumer that throws exceptions
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsFunction">juneau-commons-function</a>
 * </ul>
 *
 * @param <T> The type of the input to the function.
 * @param <R> The type of the result of the function.
 */
@FunctionalInterface
public interface ThrowingFunction<T,R> extends Function<T,R> {

	/**
	 * Applies this function to the given argument, wrapping any checked exceptions in a {@link RuntimeException}.
	 *
	 * <p>
	 * This is the default implementation that makes this interface compatible with {@link Function}.
	 * It calls {@link #applyThrows(Object)} and wraps any checked exceptions.
	 *
	 * @param t The function argument.
	 * @return The function result.
	 * @throws RuntimeException if {@link #applyThrows(Object)} throws a checked exception.
	 */
	@Override
	default R apply(T t) {
		try {
			return applyThrows(t);
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	/**
	 * Applies this function to the given argument, potentially throwing a checked exception.
	 *
	 * <p>
	 * This is the functional method that implementations must provide. It allows checked exceptions
	 * to be thrown directly, unlike the standard {@link Function#apply(Object)} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ThrowingFunction&lt;String,Integer&gt; <jv>parser</jv> = (<jv>s</jv>) -&gt; {
	 * 		<jk>try</jk> {
	 * 			<jk>return</jk> Integer.parseInt(<jv>s</jv>);
	 * 		} <jk>catch</jk> (NumberFormatException <jv>e</jv>) {
	 * 			<jk>throw new</jk> ParseException(<js>"Invalid number: "</js> + <jv>s</jv>, 0);
	 * 		}
	 * 	};
	 *
	 * 	<jk>try</jk> {
	 * 		<jk>int</jk> <jv>value</jv> = <jv>parser</jv>.applyThrows(<js>"123"</js>);
	 * 	} <jk>catch</jk> (ParseException <jv>e</jv>) {
	 * 		<jc>// Handle checked exception</jc>
	 * 	}
	 * </p>
	 *
	 * @param t The function argument.
	 * @return The function result.
	 * @throws Exception If an error occurs during function execution.
	 */
	R applyThrows(T t) throws Exception;
}