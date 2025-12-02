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
 * A functional interface representing an operation that accepts a single argument, returns no result, and may throw a checked exception.
 *
 * <p>
 * This interface extends the standard Java {@link java.util.function.Consumer} to allow the functional method
 * to throw checked exceptions. The default {@link #accept(Object)} method wraps any checked exceptions in a
 * {@link RuntimeException}, making it compatible with standard {@link Consumer} usage while still allowing
 * the implementation to throw checked exceptions via {@link #acceptThrows(Object)}.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Exception support - allows checked exceptions to be thrown via {@link #acceptThrows(Object)}
 * 	<li>Compatible with Consumer - implements {@link Consumer#accept(Object)} by wrapping exceptions
 * 	<li>Dual interface - can be used as both Consumer and ThrowingConsumer
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Operations that may throw I/O exceptions (file writing, network operations)
 * 	<li>Validation operations that may throw validation exceptions
 * 	<li>Side-effect operations that may fail with checked exceptions
 * 	<li>Operations that need to propagate checked exceptions
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda with exception</jc>
 * 	ThrowingConsumer&lt;String&gt; <jv>fileWriter</jv> = (<jv>content</jv>) -&gt; {
 * 		Files.write(Paths.get(<js>"output.txt"</js>), <jv>content</jv>.getBytes());
 * 	};
 *
 * 	<jc>// Using acceptThrows to get checked exception</jc>
 * 	<jk>try</jk> {
 * 		<jv>fileWriter</jv>.acceptThrows(<js>"Hello World"</js>);
 * 	} <jk>catch</jk> (IOException <jv>e</jv>) {
 * 		<jc>// Handle checked exception</jc>
 * 	}
 *
 * 	<jc>// Using accept wraps exception in RuntimeException</jc>
 * 	<jv>fileWriter</jv>.accept(<js>"Hello World"</js>);  <jc>// May throw RuntimeException</jc>
 * </p>
 *
 * <h5 class='section'>Exception Handling:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@link #acceptThrows(Object)} - Throws checked exceptions directly
 * 	<li>{@link #accept(Object)} - Wraps checked exceptions in {@link RuntimeException}
 * 	<li>Use {@code acceptThrows} when you need to handle specific checked exceptions
 * 	<li>Use {@code accept} when you want standard Consumer behavior
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThrowingSupplier} - Supplier that throws exceptions
 * 	<li class='jc'>{@link ThrowingFunction} - Function that throws exceptions
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsFunction">juneau-commons-function</a>
 * </ul>
 *
 * @param <T> The type of the input to the operation.
 */
@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

	/**
	 * Performs this operation on the given argument, wrapping any checked exceptions in a {@link RuntimeException}.
	 *
	 * <p>
	 * This is the default implementation that makes this interface compatible with {@link Consumer}.
	 * It calls {@link #acceptThrows(Object)} and wraps any checked exceptions.
	 *
	 * @param t The input argument.
	 * @throws RuntimeException if {@link #acceptThrows(Object)} throws a checked exception.
	 */
	@Override
	default void accept(T t) {
		try {
			acceptThrows(t);
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	/**
	 * Performs this operation on the given argument, potentially throwing a checked exception.
	 *
	 * <p>
	 * This is the functional method that implementations must provide. It allows checked exceptions
	 * to be thrown directly, unlike the standard {@link Consumer#accept(Object)} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ThrowingConsumer&lt;String&gt; <jv>validator</jv> = (<jv>value</jv>) -&gt; {
	 * 		<jk>if</jk> (<jv>value</jv> == <jk>null</jk> || <jv>value</jv>.isEmpty()) {
	 * 			<jk>throw new</jk> ValidationException(<js>"Value cannot be empty"</js>);
	 * 		}
	 * 	};
	 *
	 * 	<jk>try</jk> {
	 * 		<jv>validator</jv>.acceptThrows(<js>""</js>);
	 * 	} <jk>catch</jk> (ValidationException <jv>e</jv>) {
	 * 		<jc>// Handle checked exception</jc>
	 * 	}
	 * </p>
	 *
	 * @param t The input argument.
	 * @throws Exception If an error occurs during operation execution.
	 */
	void acceptThrows(T t) throws Exception;
}