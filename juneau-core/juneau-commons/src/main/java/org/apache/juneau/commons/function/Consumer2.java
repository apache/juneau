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

/**
 * A functional interface representing an operation that accepts two arguments and returns no result.
 *
 * <p>
 * This interface extends the standard Java {@link java.util.function.Consumer} pattern to support
 * two-argument consumers. It's useful when you need to pass operations with two parameters to methods
 * that expect functional interfaces, such as in iteration, builders, or callback patterns.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Composition support - provides {@link #andThen(Consumer2)} for operation chaining
 * 	<li>Side-effect operations - designed for operations that perform side effects rather than return values
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Two-argument operations in iteration patterns
 * 	<li>Builder methods that accept two-parameter operations
 * 	<li>Callback patterns with two parameters
 * 	<li>Event handlers that process two values
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda expression</jc>
 * 	Consumer2&lt;String,Integer&gt; <jv>printer</jv> = (<jv>name</jv>, <jv>age</jv>) -&gt;
 * 		System.<jsf>out</jsf>.println(<jv>name</jv> + <js>" is "</js> + <jv>age</jv> + <js>" years old"</js>);
 * 	<jv>printer</jv>.apply(<js>"Alice"</js>, 30);  <jc>// Prints "Alice is 30 years old"</jc>
 *
 * 	<jc>// Method reference</jc>
 * 	Map&lt;String,Integer&gt; <jv>map</jv> = <jk>new</jk> HashMap&lt;&gt;();
 * 	Consumer2&lt;String,Integer&gt; <jv>putter</jv> = <jv>map</jv>::put;
 * 	<jv>putter</jv>.apply(<js>"key"</js>, 42);
 *
	 * 	<jc>// Operation composition</jc>
 * 	Consumer2&lt;String,Integer&gt; <jv>log</jv> = (<jv>s</jv>, <jv>i</jv>) -&gt; <jsm>logger</jsm>.info(<jv>s</jv> + <js>": "</js> + <jv>i</jv>);
 * 	Consumer2&lt;String,Integer&gt; <jv>store</jv> = (<jv>s</jv>, <jv>i</jv>) -&gt; <jv>cache</jv>.put(<jv>s</jv>, <jv>i</jv>);
 * 	Consumer2&lt;String,Integer&gt; <jv>logAndStore</jv> = <jv>log</jv>.andThen(<jv>store</jv>);
 * 	<jv>logAndStore</jv>.apply(<js>"data"</js>, 100);  <jc>// Logs and stores</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Consumer3} - Three-argument consumer
 * 	<li class='jc'>{@link Consumer4} - Four-argument consumer
 * 	<li class='jc'>{@link Consumer5} - Five-argument consumer
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonFunction">juneau-common-function</a>
 * </ul>
 *
 * @param <A> The type of the first argument to the operation.
 * @param <B> The type of the second argument to the operation.
 */
@FunctionalInterface
public interface Consumer2<A,B> {

	/**
	 * Returns a composed {@link Consumer2} that performs, in sequence, this operation followed by the {@code after} operation.
	 *
	 * <p>
	 * This method enables operation composition, allowing you to chain multiple operations together.
	 * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
	 * If performing this operation throws an exception, the {@code after} operation will not be performed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Consumer2&lt;String,Integer&gt; <jv>validate</jv> = (<jv>s</jv>, <jv>i</jv>) -&gt; {
	 * 		<jk>if</jk> (<jv>i</jv> &lt; 0) <jk>throw new</jk> IllegalArgumentException();
	 * 	};
	 * 	Consumer2&lt;String,Integer&gt; <jv>process</jv> = (<jv>s</jv>, <jv>i</jv>) -&gt; <jsm>process</jsm>(<jv>s</jv>, <jv>i</jv>);
	 * 	Consumer2&lt;String,Integer&gt; <jv>validateAndProcess</jv> = <jv>validate</jv>.andThen(<jv>process</jv>);
	 * 	<jv>validateAndProcess</jv>.apply(<js>"data"</js>, 42);  <jc>// Validates then processes</jc>
	 * </p>
	 *
	 * @param after The operation to perform after this operation. Must not be <jk>null</jk>.
	 * @return A composed {@link Consumer2} that performs in sequence this operation followed by the {@code after} operation.
	 * @throws NullPointerException if {@code after} is <jk>null</jk>.
	 */
	default Consumer2<A,B> andThen(Consumer2<? super A,? super B> after) {  // NOSONAR - false positive on generics
		assertArgNotNull("after", after);
		return (A a, B b) -> {
			apply(a, b);
			after.apply(a, b);
		};
	}

	/**
	 * Performs this operation on the given arguments.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Consumer2&lt;String,Integer&gt; <jv>handler</jv> = (<jv>key</jv>, <jv>value</jv>) -&gt; {
	 * 		<jv>map</jv>.put(<jv>key</jv>, <jv>value</jv>);
	 * 		<jv>cache</jv>.invalidate(<jv>key</jv>);
	 * 	};
	 * 	<jv>handler</jv>.apply(<js>"user"</js>, 12345);
	 * </p>
	 *
	 * @param a The first operation argument.
	 * @param b The second operation argument.
	 */
	void apply(A a, B b);
}