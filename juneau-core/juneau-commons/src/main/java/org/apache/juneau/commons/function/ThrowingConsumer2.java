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
 * A functional interface representing an operation that accepts two arguments, returns no result, and may throw a checked exception.
 *
 * <p>
 * This interface extends {@link Consumer2} to allow the functional method
 * to throw checked exceptions. The default {@link #apply(Object, Object)} method wraps any checked exceptions in a
 * {@link RuntimeException}, making it compatible with standard {@link Consumer2} usage while still allowing
 * the implementation to throw checked exceptions via {@link #acceptThrows(Object, Object)}.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Exception support - allows checked exceptions to be thrown via {@link #acceptThrows(Object, Object)}
 * 	<li>Compatible with Consumer2 - implements {@link Consumer2#apply(Object, Object)} by wrapping exceptions
 * 	<li>Dual interface - can be used as both Consumer2 and ThrowingConsumer2
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Two-argument operations that may throw I/O exceptions
 * 	<li>Validation operations that may throw validation exceptions
 * 	<li>Side-effect operations that may fail with checked exceptions
 * 	<li>Operations that need to propagate checked exceptions
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda with exception</jc>
 * 	ThrowingConsumer2&lt;String,Integer&gt; <jv>fileWriter</jv> = (<jv>path</jv>, <jv>content</jv>) -&gt; {
 * 		Files.write(Paths.get(<jv>path</jv>), <jv>content</jv>.getBytes());
 * 	};
 *
 * 	<jc>// Using acceptThrows to get checked exception</jc>
 * 	<jk>try</jk> {
 * 		<jv>fileWriter</jv>.acceptThrows(<js>"/tmp/output.txt"</js>, <js>"Hello World"</js>);
 * 	} <jk>catch</jk> (IOException <jv>e</jv>) {
 * 		<jc>// Handle checked exception</jc>
 * 	}
 *
 * 	<jc>// Using apply wraps exception in RuntimeException</jc>
 * 	<jv>fileWriter</jv>.apply(<js>"/tmp/output.txt"</js>, <js>"Hello World"</js>);  <jc>// May throw RuntimeException</jc>
 * </p>
 *
 * <h5 class='section'>Exception Handling:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@link #acceptThrows(Object, Object)} - Throws checked exceptions directly
 * 	<li>{@link #apply(Object, Object)} - Wraps checked exceptions in {@link RuntimeException}
 * 	<li>Use {@code acceptThrows} when you need to handle specific checked exceptions
 * 	<li>Use {@code apply} when you want standard Consumer2 behavior
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThrowingConsumer} - Single-argument consumer that throws exceptions
 * 	<li class='jc'>{@link ThrowingConsumer3} - Three-argument consumer that throws exceptions
 * 	<li class='jc'>{@link ThrowingConsumer4} - Four-argument consumer that throws exceptions
 * 	<li class='jc'>{@link ThrowingConsumer5} - Five-argument consumer that throws exceptions
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsFunction">juneau-commons-function</a>
 * </ul>
 *
 * @param <A> The type of the first argument to the operation.
 * @param <B> The type of the second argument to the operation.
 */
@FunctionalInterface
public interface ThrowingConsumer2<A,B> extends Consumer2<A,B> {

	/**
	 * Performs this operation on the given arguments, wrapping any checked exceptions in a {@link RuntimeException}.
	 *
	 * <p>
	 * This is the default implementation that makes this interface compatible with {@link Consumer2}.
	 * It calls {@link #acceptThrows(Object, Object)} and wraps any checked exceptions.
	 *
	 * @param a The first operation argument.
	 * @param b The second operation argument.
	 * @throws RuntimeException if {@link #acceptThrows(Object, Object)} throws a checked exception.
	 */
	@Override
	default void apply(A a, B b) {
		try {
			acceptThrows(a, b);
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	/**
	 * Returns a composed {@link ThrowingConsumer2} that performs, in sequence, this operation followed by the {@code after} operation.
	 *
	 * <p>
	 * This method enables operation composition, allowing you to chain multiple operations together.
	 * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
	 * If performing this operation throws an exception, the {@code after} operation will not be performed.
	 *
	 * @param after The operation to perform after this operation. Must not be <jk>null</jk>.
	 * @return A composed {@link ThrowingConsumer2} that performs in sequence this operation followed by the {@code after} operation.
	 * @throws NullPointerException if {@code after} is <jk>null</jk>.
	 */
	default ThrowingConsumer2<A,B> andThen(ThrowingConsumer2<? super A,? super B> after) {  // NOSONAR - false positive on generics
		assertArgNotNull("after", after);
		return (A a, B b) -> {
			acceptThrows(a, b);
			after.acceptThrows(a, b);
		};
	}

	/**
	 * Performs this operation on the given arguments, potentially throwing a checked exception.
	 *
	 * <p>
	 * This is the functional method that implementations must provide. It allows checked exceptions
	 * to be thrown directly, unlike the standard {@link Consumer2#apply(Object, Object)} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ThrowingConsumer2&lt;String,Integer&gt; <jv>validator</jv> = (<jv>name</jv>, <jv>age</jv>) -&gt; {
	 * 		<jk>if</jk> (<jv>name</jv> == <jk>null</jk> || <jv>name</jv>.isEmpty()) {
	 * 			<jk>throw new</jk> ValidationException(<js>"Name cannot be empty"</js>);
	 * 		}
	 * 		<jk>if</jk> (<jv>age</jv> &lt; 0) {
	 * 			<jk>throw new</jk> ValidationException(<js>"Age cannot be negative"</js>);
	 * 		}
	 * 	};
	 *
	 * 	<jk>try</jk> {
	 * 		<jv>validator</jv>.acceptThrows(<js>""</js>, -1);
	 * 	} <jk>catch</jk> (ValidationException <jv>e</jv>) {
	 * 		<jc>// Handle checked exception</jc>
	 * 	}
	 * </p>
	 *
	 * @param a The first operation argument.
	 * @param b The second operation argument.
	 * @throws Exception If an error occurs during operation execution.
	 */
	void acceptThrows(A a, B b) throws Exception;
}

