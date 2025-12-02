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
package org.apache.juneau.common.function;

import static org.apache.juneau.common.utils.AssertionUtils.*;

/**
 * A functional interface representing an operation that accepts three arguments and returns no result.
 *
 * <p>
 * This interface extends the standard Java {@link java.util.function.Consumer} pattern to support
 * three-argument consumers. It's useful when you need to pass operations with three parameters to methods
 * that expect functional interfaces, such as in iteration, builders, or callback patterns.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Composition support - provides {@link #andThen(Consumer3)} for operation chaining
 * 	<li>Side-effect operations - designed for operations that perform side effects rather than return values
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Three-argument operations in iteration patterns
 * 	<li>Builder methods that accept three-parameter operations
 * 	<li>Callback patterns with three parameters
 * 	<li>Event handlers that process three values
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda expression</jc>
 * 	Consumer3&lt;String,Integer,Boolean&gt; <jv>printer</jv> = (<jv>name</jv>, <jv>age</jv>, <jv>active</jv>) -&gt;
 * 		System.<jsf>out</jsf>.println(<jv>name</jv> + <js>" is "</js> + <jv>age</jv> + <js>" ("</js> + (<jv>active</jv> ? <js>"active"</js> : <js>"inactive"</js>) + <js>")"</js>);
 * 	<jv>printer</jv>.apply(<js>"Alice"</js>, 30, <jk>true</jk>);  <jc>// Prints "Alice is 30 (active)"</jc>
 *
 * 	<jc>// Operation composition</jc>
 * 	Consumer3&lt;String,Integer,Boolean&gt; <jv>validate</jv> = (<jv>s</jv>, <jv>i</jv>, <jv>b</jv>) -&gt; {
 * 		<jk>if</jk> (<jv>i</jv> &lt; 0) <jk>throw new</jk> IllegalArgumentException();
 * 	};
 * 	Consumer3&lt;String,Integer,Boolean&gt; <jv>process</jv> = (<jv>s</jv>, <jv>i</jv>, <jv>b</jv>) -&gt; <jsm>process</jsm>(<jv>s</jv>, <jv>i</jv>, <jv>b</jv>);
 * 	Consumer3&lt;String,Integer,Boolean&gt; <jv>validateAndProcess</jv> = <jv>validate</jv>.andThen(<jv>process</jv>);
 * 	<jv>validateAndProcess</jv>.apply(<js>"data"</js>, 42, <jk>true</jk>);  <jc>// Validates then processes</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Consumer2} - Two-argument consumer
 * 	<li class='jc'>{@link Consumer4} - Four-argument consumer
 * 	<li class='jc'>{@link Consumer5} - Five-argument consumer
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonFunction">juneau-common-function</a>
 * </ul>
 *
 * @param <A> The type of the first argument to the operation.
 * @param <B> The type of the second argument to the operation.
 * @param <C> The type of the third argument to the operation.
 */
@FunctionalInterface
public interface Consumer3<A,B,C> {

	/**
	 * Returns a composed {@link Consumer3} that performs, in sequence, this operation followed by the {@code after} operation.
	 *
	 * <p>
	 * This method enables operation composition, allowing you to chain multiple operations together.
	 * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
	 * If performing this operation throws an exception, the {@code after} operation will not be performed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Consumer3&lt;String,Integer,Boolean&gt; <jv>log</jv> = (<jv>s</jv>, <jv>i</jv>, <jv>b</jv>) -&gt; <jsm>logger</jsm>.info(<jv>s</jv> + <js>": "</js> + <jv>i</jv> + <js>", "</js> + <jv>b</jv>);
	 * 	Consumer3&lt;String,Integer,Boolean&gt; <jv>store</jv> = (<jv>s</jv>, <jv>i</jv>, <jv>b</jv>) -&gt; <jv>cache</jv>.put(<jv>s</jv>, <jv>i</jv>, <jv>b</jv>);
	 * 	Consumer3&lt;String,Integer,Boolean&gt; <jv>logAndStore</jv> = <jv>log</jv>.andThen(<jv>store</jv>);
	 * 	<jv>logAndStore</jv>.apply(<js>"data"</js>, 100, <jk>true</jk>);  <jc>// Logs and stores</jc>
	 * </p>
	 *
	 * @param after The operation to perform after this operation. Must not be <jk>null</jk>.
	 * @return A composed {@link Consumer3} that performs in sequence this operation followed by the {@code after} operation.
	 * @throws NullPointerException if {@code after} is <jk>null</jk>.
	 */
	default Consumer3<A,B,C> andThen(Consumer3<? super A,? super B,? super C> after) {  // NOSONAR - false positive on generics
		assertArgNotNull("after", after);
		return (A a, B b, C c) -> {
			apply(a, b, c);
			after.apply(a, b, c);
		};
	}

	/**
	 * Performs this operation on the given arguments.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Consumer3&lt;String,Integer,Boolean&gt; <jv>handler</jv> = (<jv>key</jv>, <jv>value</jv>, <jv>flag</jv>) -&gt; {
	 * 		<jv>map</jv>.put(<jv>key</jv>, <jv>value</jv>);
	 * 		<jk>if</jk> (<jv>flag</jv>) <jv>cache</jv>.invalidate(<jv>key</jv>);
	 * 	};
	 * 	<jv>handler</jv>.apply(<js>"user"</js>, 12345, <jk>true</jk>);
	 * </p>
	 *
	 * @param a The first operation argument.
	 * @param b The second operation argument.
	 * @param c The third operation argument.
	 */
	void apply(A a, B b, C c);
}