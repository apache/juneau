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
 * A functional interface representing an operation that accepts four arguments and returns no result.
 *
 * <p>
 * This interface extends the standard Java {@link java.util.function.Consumer} pattern to support
 * four-argument consumers. It's useful when you need to pass operations with four parameters to methods
 * that expect functional interfaces, such as in iteration, builders, or callback patterns.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Composition support - provides {@link #andThen(Consumer4)} for operation chaining
 * 	<li>Side-effect operations - designed for operations that perform side effects rather than return values
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Four-argument operations in iteration patterns
 * 	<li>Builder methods that accept four-parameter operations
 * 	<li>Callback patterns with four parameters
 * 	<li>Event handlers that process four values
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda expression</jc>
 * 	Consumer4&lt;String,Integer,Boolean,Double&gt; <jv>printer</jv> = (<jv>name</jv>, <jv>age</jv>, <jv>active</jv>, <jv>score</jv>) -&gt;
 * 		System.<jsf>out</jsf>.println(<jv>name</jv> + <js>": age="</js> + <jv>age</jv> + <js>", active="</js> + <jv>active</jv> + <js>", score="</js> + <jv>score</jv>);
 * 	<jv>printer</jv>.apply(<js>"Alice"</js>, 30, <jk>true</jk>, 95.5);  <jc>// Prints "Alice: age=30, active=true, score=95.5"</jc>
 *
 * 	<jc>// Operation composition</jc>
 * 	Consumer4&lt;String,Integer,Boolean,Double&gt; <jv>validate</jv> = (<jv>s</jv>, <jv>i</jv>, <jv>b</jv>, <jv>d</jv>) -&gt; {
 * 		<jk>if</jk> (<jv>i</jv> &lt; 0 || <jv>d</jv> &lt; 0) <jk>throw new</jk> IllegalArgumentException();
 * 	};
 * 	Consumer4&lt;String,Integer,Boolean,Double&gt; <jv>process</jv> = (<jv>s</jv>, <jv>i</jv>, <jv>b</jv>, <jv>d</jv>) -&gt; <jsm>process</jsm>(<jv>s</jv>, <jv>i</jv>, <jv>b</jv>, <jv>d</jv>);
 * 	Consumer4&lt;String,Integer,Boolean,Double&gt; <jv>validateAndProcess</jv> = <jv>validate</jv>.andThen(<jv>process</jv>);
 * 	<jv>validateAndProcess</jv>.apply(<js>"data"</js>, 42, <jk>true</jk>, 10.5);  <jc>// Validates then processes</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Consumer2} - Two-argument consumer
 * 	<li class='jc'>{@link Consumer3} - Three-argument consumer
 * 	<li class='jc'>{@link Consumer5} - Five-argument consumer
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonFunction">juneau-common-function</a>
 * </ul>
 *
 * @param <A> The type of the first argument to the operation.
 * @param <B> The type of the second argument to the operation.
 * @param <C> The type of the third argument to the operation.
 * @param <D> The type of the fourth argument to the operation.
 */
@FunctionalInterface
public interface Consumer4<A,B,C,D> {

	/**
	 * Returns a composed {@link Consumer4} that performs, in sequence, this operation followed by the {@code after} operation.
	 *
	 * <p>
	 * This method enables operation composition, allowing you to chain multiple operations together.
	 * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
	 * If performing this operation throws an exception, the {@code after} operation will not be performed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Consumer4&lt;String,Integer,Boolean,Double&gt; <jv>log</jv> = (<jv>s</jv>, <jv>i</jv>, <jv>b</jv>, <jv>d</jv>) -&gt; <jsm>logger</jsm>.info(<jv>s</jv> + <js>": "</js> + <jv>i</jv> + <js>", "</js> + <jv>b</jv> + <js>", "</js> + <jv>d</jv>);
	 * 	Consumer4&lt;String,Integer,Boolean,Double&gt; <jv>store</jv> = (<jv>s</jv>, <jv>i</jv>, <jv>b</jv>, <jv>d</jv>) -&gt; <jv>cache</jv>.put(<jv>s</jv>, <jv>i</jv>, <jv>b</jv>, <jv>d</jv>);
	 * 	Consumer4&lt;String,Integer,Boolean,Double&gt; <jv>logAndStore</jv> = <jv>log</jv>.andThen(<jv>store</jv>);
	 * 	<jv>logAndStore</jv>.apply(<js>"data"</js>, 100, <jk>true</jk>, 50.0);  <jc>// Logs and stores</jc>
	 * </p>
	 *
	 * @param after The operation to perform after this operation. Must not be <jk>null</jk>.
	 * @return A composed {@link Consumer4} that performs in sequence this operation followed by the {@code after} operation.
	 * @throws NullPointerException if {@code after} is <jk>null</jk>.
	 */
	default Consumer4<A,B,C,D> andThen(Consumer4<? super A,? super B,? super C,? super D> after) {  // NOSONAR - false positive on generics
		assertArgNotNull("after", after);
		return (A a, B b, C c, D d) -> {
			apply(a, b, c, d);
			after.apply(a, b, c, d);
		};
	}

	/**
	 * Performs this operation on the given arguments.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Consumer4&lt;String,Integer,Boolean,Double&gt; <jv>handler</jv> = (<jv>key</jv>, <jv>value</jv>, <jv>flag</jv>, <jv>weight</jv>) -&gt; {
	 * 		<jv>map</jv>.put(<jv>key</jv>, <jv>value</jv>);
	 * 		<jk>if</jk> (<jv>flag</jv>) <jv>cache</jv>.invalidate(<jv>key</jv>);
	 * 		<jv>stats</jv>.record(<jv>weight</jv>);
	 * 	};
	 * 	<jv>handler</jv>.apply(<js>"user"</js>, 12345, <jk>true</jk>, 1.5);
	 * </p>
	 *
	 * @param a The first operation argument.
	 * @param b The second operation argument.
	 * @param c The third operation argument.
	 * @param d The fourth operation argument.
	 */
	void apply(A a, B b, C c, D d);
}