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

/**
 * A functional interface representing an operation that accepts three arguments, returns no result, and may throw a checked exception.
 *
 * <p>
 * This interface extends {@link Consumer3} to allow the functional method
 * to throw checked exceptions. The default {@link #apply(Object, Object, Object)} method wraps any checked exceptions in a
 * {@link RuntimeException}, making it compatible with standard {@link Consumer3} usage while still allowing
 * the implementation to throw checked exceptions via {@link #acceptThrows(Object, Object, Object)}.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Exception support - allows checked exceptions to be thrown via {@link #acceptThrows(Object, Object, Object)}
 * 	<li>Compatible with Consumer3 - implements {@link Consumer3#apply(Object, Object, Object)} by wrapping exceptions
 * 	<li>Dual interface - can be used as both Consumer3 and ThrowingConsumer3
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Three-argument operations that may throw I/O exceptions
 * 	<li>Validation operations that may throw validation exceptions
 * 	<li>Side-effect operations that may fail with checked exceptions
 * 	<li>Operations that need to propagate checked exceptions
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda with exception</jc>
 * 	ThrowingConsumer3&lt;String,Integer,Boolean&gt; <jv>fileWriter</jv> = (<jv>path</jv>, <jv>content</jv>, <jv>append</jv>) -&gt; {
 * 		<jk>if</jk> (<jv>append</jv>) {
 * 			Files.write(Paths.get(<jv>path</jv>), <jv>content</jv>.getBytes(), StandardOpenOption.APPEND);
 * 		} <jk>else</jk> {
 * 			Files.write(Paths.get(<jv>path</jv>), <jv>content</jv>.getBytes());
 * 		}
 * 	};
 *
 * 	<jc>// Using acceptThrows to get checked exception</jc>
 * 	<jk>try</jk> {
 * 		<jv>fileWriter</jv>.acceptThrows(<js>"/tmp/output.txt"</js>, <js>"Hello World"</js>, <jk>true</jk>);
 * 	} <jk>catch</jk> (IOException <jv>e</jv>) {
 * 		<jc>// Handle checked exception</jc>
 * 	}
 *
 * 	<jc>// Using apply wraps exception in RuntimeException</jc>
 * 	<jv>fileWriter</jv>.apply(<js>"/tmp/output.txt"</js>, <js>"Hello World"</js>, <jk>true</jk>);  <jc>// May throw RuntimeException</jc>
 * </p>
 *
 * <h5 class='section'>Exception Handling:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@link #acceptThrows(Object, Object, Object)} - Throws checked exceptions directly
 * 	<li>{@link #apply(Object, Object, Object)} - Wraps checked exceptions in {@link RuntimeException}
 * 	<li>Use {@code acceptThrows} when you need to handle specific checked exceptions
 * 	<li>Use {@code apply} when you want standard Consumer3 behavior
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThrowingConsumer} - Single-argument consumer that throws exceptions
 * 	<li class='jc'>{@link ThrowingConsumer2} - Two-argument consumer that throws exceptions
 * 	<li class='jc'>{@link ThrowingConsumer4} - Four-argument consumer that throws exceptions
 * 	<li class='jc'>{@link ThrowingConsumer5} - Five-argument consumer that throws exceptions
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsFunction">juneau-commons-function</a>
 * </ul>
 *
 * @param <A> The type of the first argument to the operation.
 * @param <B> The type of the second argument to the operation.
 * @param <C> The type of the third argument to the operation.
 */
@FunctionalInterface
public interface ThrowingConsumer3<A,B,C> extends Consumer3<A,B,C> {

	/**
	 * Performs this operation on the given arguments, wrapping any checked exceptions in a {@link RuntimeException}.
	 *
	 * <p>
	 * This is the default implementation that makes this interface compatible with {@link Consumer3}.
	 * It calls {@link #acceptThrows(Object, Object, Object)} and wraps any checked exceptions.
	 *
	 * @param a The first operation argument.
	 * @param b The second operation argument.
	 * @param c The third operation argument.
	 * @throws RuntimeException if {@link #acceptThrows(Object, Object, Object)} throws a checked exception.
	 */
	@Override
	default void apply(A a, B b, C c) {
		try {
			acceptThrows(a, b, c);
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	/**
	 * Returns a composed {@link ThrowingConsumer3} that performs, in sequence, this operation followed by the {@code after} operation.
	 *
	 * <p>
	 * This method enables operation composition, allowing you to chain multiple operations together.
	 * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
	 * If performing this operation throws an exception, the {@code after} operation will not be performed.
	 *
	 * @param after The operation to perform after this operation. Must not be <jk>null</jk>.
	 * @return A composed {@link ThrowingConsumer3} that performs in sequence this operation followed by the {@code after} operation.
	 * @throws NullPointerException if {@code after} is <jk>null</jk>.
	 */
	default ThrowingConsumer3<A,B,C> andThen(ThrowingConsumer3<? super A,? super B,? super C> after) {  // NOSONAR - false positive on generics
		assertArgNotNull("after", after);
		return (A a, B b, C c) -> {
			acceptThrows(a, b, c);
			after.acceptThrows(a, b, c);
		};
	}

	/**
	 * Performs this operation on the given arguments, potentially throwing a checked exception.
	 *
	 * <p>
	 * This is the functional method that implementations must provide. It allows checked exceptions
	 * to be thrown directly, unlike the standard {@link Consumer3#apply(Object, Object, Object)} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ThrowingConsumer3&lt;String,Integer,Boolean&gt; <jv>validator</jv> = (<jv>name</jv>, <jv>age</jv>, <jv>active</jv>) -&gt; {
	 * 		<jk>if</jk> (<jv>name</jv> == <jk>null</jk> || <jv>name</jv>.isEmpty()) {
	 * 			<jk>throw new</jk> ValidationException(<js>"Name cannot be empty"</js>);
	 * 		}
	 * 		<jk>if</jk> (<jv>age</jv> &lt; 0) {
	 * 			<jk>throw new</jk> ValidationException(<js>"Age cannot be negative"</js>);
	 * 		}
	 * 	};
	 *
	 * 	<jk>try</jk> {
	 * 		<jv>validator</jv>.acceptThrows(<js>""</js>, -1, <jk>true</jk>);
	 * 	} <jk>catch</jk> (ValidationException <jv>e</jv>) {
	 * 		<jc>// Handle checked exception</jc>
	 * 	}
	 * </p>
	 *
	 * @param a The first operation argument.
	 * @param b The second operation argument.
	 * @param c The third operation argument.
	 * @throws Exception If an error occurs during operation execution.
	 */
	void acceptThrows(A a, B b, C c) throws Exception;
}

