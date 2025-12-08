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

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.Optional;

import org.apache.juneau.commons.utils.*;

/**
 * Represents an immutable tuple containing a single value.
 *
 * <p>
 * This class provides a simple wrapper for a single value that properly implements
 * {@link #equals(Object)} and {@link #hashCode()} based on content rather than identity.
 * This is particularly useful for HashMap keys when you want to use an array or need
 * a wrapper that uses content-based equality.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Immutable - value cannot be changed after construction
 * 	<li>Content-based equality - uses {@link org.apache.juneau.commons.utils.Utils#eq(Object, Object)} for comparison
 * 	<li>Content-based hashing - uses {@link HashCode#of(Object...)} for hash code computation
 * 	<li>Array support - properly handles arrays with content-based equality and hashing
 * </ul>
 *
 * <h5 class='section'>Array Support:</h5>
 * <p>
 * Unlike using arrays directly as HashMap keys (which use identity-based equality), this class properly
 * handles arrays by using content-based equality and hashing via {@link HashCode#of(Object...)} which
 * internally uses {@link java.util.Arrays#hashCode(Object[])} for arrays. This means two Tuple1 instances
 * containing arrays with the same content will be considered equal.
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Using arrays as HashMap keys with proper content-based equality
 * 	<li>Wrapping values that need content-based equality for collections
 * 	<li>Creating composite keys from single values
 * 	<li>Passing values through APIs that require object identity preservation
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Using arrays as keys via Tuple1</jc>
 * 	Map&lt;Tuple1&lt;String[]&gt;,Integer&gt; <jv>map</jv> = <jk>new</jk> HashMap&lt;&gt;();
 * 	<jv>map</jv>.put(Tuple1.<jsm>of</jsm>(<jk>new</jk> String[]{<js>"a"</js>,<js>"b"</js>}), 1);
 * 	<jv>map</jv>.put(Tuple1.<jsm>of</jsm>(<jk>new</jk> String[]{<js>"a"</js>,<js>"b"</js>}), 2);  <jc>// Replaces first entry</jc>
 * 	System.<jsf>out</jsf>.println(<jv>map</jv>.size());  <jc>// Prints "1"</jc>
 *
 * 	<jc>// Using with other types</jc>
 * 	Map&lt;Tuple1&lt;String&gt;,Integer&gt; <jv>stringMap</jv> = <jk>new</jk> HashMap&lt;&gt;();
 * 	<jv>stringMap</jv>.put(Tuple1.<jsm>of</jsm>(<js>"key"</js>), 42);
 * 	Integer <jv>value</jv> = <jv>stringMap</jv>.get(Tuple1.<jsm>of</jsm>(<js>"key"</js>));  <jc>// Returns 42</jc>
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is immutable and therefore thread-safe. Multiple threads can safely access a Tuple1 instance
 * concurrently without synchronization.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Tuple2} - Two-value tuple
 * 	<li class='jc'>{@link Tuple3} - Three-value tuple
 * 	<li class='jc'>{@link Tuple4} - Four-value tuple
 * 	<li class='jc'>{@link Tuple5} - Five-value tuple
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsFunction">juneau-commons-function</a>
 * </ul>
 *
 * @param <A> The type of the value in this tuple.
 */
public class Tuple1<A> {

	/**
	 * Creates a new tuple containing the specified value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Tuple1&lt;String&gt; <jv>tuple</jv> = Tuple1.<jsm>of</jsm>(<js>"value"</js>);
	 * 	Tuple1&lt;String[]&gt; <jv>arrayTuple</jv> = Tuple1.<jsm>of</jsm>(<jk>new</jk> String[]{<js>"a"</js>, <js>"b"</js>});
	 * </p>
	 *
	 * @param <A> The value type.
	 * @param a The value to wrap in the tuple.
	 * @return A new tuple containing the specified value.
	 */
	public static <A> Tuple1<A> of(A a) {
		return new Tuple1<>(a);
	}

	private final A a;
	private final int hashCode;

	/**
	 * Constructor.
	 *
	 * @param a Object value.
	 */
	public Tuple1(A a) {
		this.a = a;
		this.hashCode = HashCode.of(a);
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return o instanceof Tuple1<?> o2 && eq(this, o2, (x, y) -> eq(x.a, y.a));
	}

	/**
	 * Returns the value contained in this tuple.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Tuple1&lt;String&gt; <jv>tuple</jv> = Tuple1.<jsm>of</jsm>(<js>"hello"</js>);
	 * 	String <jv>value</jv> = <jv>tuple</jv>.getA();  <jc>// Returns "hello"</jc>
	 * </p>
	 *
	 * @return The value contained in this tuple.
	 */
	public A getA() { return a; }

	/**
	 * Returns the value contained in this tuple wrapped in an {@link Optional}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Tuple1&lt;String&gt; <jv>tuple</jv> = Tuple1.<jsm>of</jsm>(<js>"hello"</js>);
	 * 	Optional&lt;String&gt; <jv>value</jv> = <jv>tuple</jv>.optA();  <jc>// Returns Optional.of("hello")</jc>
	 *
	 * 	Tuple1&lt;String&gt; <jv>tuple2</jv> = Tuple1.<jsm>of</jsm>(<jk>null</jk>);
	 * 	Optional&lt;String&gt; <jv>value2</jv> = <jv>tuple2</jv>.optA();  <jc>// Returns Optional.empty()</jc>
	 * </p>
	 *
	 * @return The value wrapped in an Optional, or Optional.empty() if the value is null.
	 */
	public Optional<A> optA() {
		return Optional.ofNullable(a);
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return hashCode;
	}
}
