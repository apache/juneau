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

import org.apache.juneau.commons.utils.*;

/**
 * Represents an immutable tuple containing two values.
 *
 * <p>
 * This class provides a simple wrapper for two values that properly implements
 * {@link #equals(Object)} and {@link #hashCode()} based on content rather than identity.
 * This is particularly useful for HashMap composite keys when you need to combine multiple
 * values into a single key.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Immutable - values cannot be changed after construction
 * 	<li>Content-based equality - uses {@link org.apache.juneau.commons.utils.Utils#eq(Object, Object)} for comparison
 * 	<li>Content-based hashing - uses {@link HashCode#of(Object...)} for hash code computation
 * 	<li>Array support - properly handles arrays with content-based equality and hashing
 * </ul>
 *
 * <h5 class='section'>Array Support:</h5>
 * <p>
 * Unlike using arrays directly as HashMap keys (which use identity-based equality), this class properly
 * handles arrays by using content-based equality and hashing via {@link HashCode#of(Object...)} which
 * internally uses {@link java.util.Arrays#hashCode(Object[])} for arrays. This means two Tuple2 instances
 * containing arrays with the same content will be considered equal.
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Composite keys for HashMap lookups (e.g., combining two identifiers)
 * 	<li>Using arrays in composite keys with proper content-based equality
 * 	<li>Returning multiple values from methods (alternative to creating a custom class)
 * 	<li>Grouping related values together for processing
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Using arrays in composite keys</jc>
 * 	Map&lt;Tuple2&lt;String,int[]&gt;,Result&gt; <jv>map</jv> = <jk>new</jk> HashMap&lt;&gt;();
 * 	<jv>map</jv>.put(Tuple2.<jsm>of</jsm>(<js>"key"</js>, <jk>new</jk> <jk>int</jk>[]{1,2,3}), <jv>result1</jv>);
 * 	Result <jv>r</jv> = <jv>map</jv>.get(Tuple2.<jsm>of</jsm>(<js>"key"</js>, <jk>new</jk> <jk>int</jk>[]{1,2,3}));  <jc>// Works correctly!</jc>
 *
 * 	<jc>// Simple composite key</jc>
 * 	Map&lt;Tuple2&lt;String,Integer&gt;,String&gt; <jv>cache</jv> = <jk>new</jk> HashMap&lt;&gt;();
 * 	<jv>cache</jv>.put(Tuple2.<jsm>of</jsm>(<js>"user"</js>, 12345), <js>"John Doe"</js>);
 * 	String <jv>name</jv> = <jv>cache</jv>.get(Tuple2.<jsm>of</jsm>(<js>"user"</js>, 12345));  <jc>// Returns "John Doe"</jc>
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is immutable and therefore thread-safe. Multiple threads can safely access a Tuple2 instance
 * concurrently without synchronization.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Tuple1} - Single-value tuple
 * 	<li class='jc'>{@link Tuple3} - Three-value tuple
 * 	<li class='jc'>{@link Tuple4} - Four-value tuple
 * 	<li class='jc'>{@link Tuple5} - Five-value tuple
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsFunction">juneau-commons-function</a>
 * </ul>
 *
 * @param <A> The type of the first value in this tuple.
 * @param <B> The type of the second value in this tuple.
 */
public class Tuple2<A,B> {

	/**
	 * Creates a new tuple containing the specified two values.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Tuple2&lt;String,Integer&gt; <jv>tuple</jv> = Tuple2.<jsm>of</jsm>(<js>"name"</js>, 42);
	 * 	Tuple2&lt;String[],int[]&gt; <jv>arrayTuple</jv> = Tuple2.<jsm>of</jsm>(
	 * 		<jk>new</jk> String[]{<js>"a"</js>, <js>"b"</js>},
	 * 		<jk>new</jk> <jk>int</jk>[]{1, 2}
	 * 	);
	 * </p>
	 *
	 * @param <A> The type of the first value.
	 * @param <B> The type of the second value.
	 * @param a The first value.
	 * @param b The second value.
	 * @return A new tuple containing the specified values.
	 */
	public static <A,B> Tuple2<A,B> of(A a, B b) {
		return new Tuple2<>(a, b);
	}

	private final A a;
	private final B b;
	private final int hashCode;

	/**
	 * Constructor.
	 *
	 * @param a Object 1.
	 * @param b Object 2.
	 */
	public Tuple2(A a, B b) {
		this.a = a;
		this.b = b;
		this.hashCode = HashCode.of(a, b);
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return o instanceof Tuple2<?,?> o2 && eq(this, o2, (x, y) -> eq(x.a, y.a) && eq(x.b, y.b));
	}

	/**
	 * Returns the first value in this tuple.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Tuple2&lt;String,Integer&gt; <jv>tuple</jv> = Tuple2.<jsm>of</jsm>(<js>"hello"</js>, 42);
	 * 	String <jv>first</jv> = <jv>tuple</jv>.getA();  <jc>// Returns "hello"</jc>
	 * </p>
	 *
	 * @return The first value in this tuple.
	 */
	public A getA() { return a; }

	/**
	 * Returns the second value in this tuple.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Tuple2&lt;String,Integer&gt; <jv>tuple</jv> = Tuple2.<jsm>of</jsm>(<js>"hello"</js>, 42);
	 * 	Integer <jv>second</jv> = <jv>tuple</jv>.getB();  <jc>// Returns 42</jc>
	 * </p>
	 *
	 * @return The second value in this tuple.
	 */
	public B getB() { return b; }

	@Override /* Overridden from Object */
	public int hashCode() {
		return hashCode;
	}
}