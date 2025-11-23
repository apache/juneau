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

import static org.apache.juneau.common.utils.Utils.*;

import org.apache.juneau.common.utils.*;

/**
 * Represents a simple tuple of 1 object.
 *
 * <p>
 * This class is useful when you need a single-value wrapper that properly implements
 * {@link #equals(Object)} and {@link #hashCode()} based on content rather than identity.
 * This is particularly useful for HashMap keys when you want to use an array or need
 * a wrapper that uses content-based equality.
 *
 * <h5 class='section'>Array Support:</h5>
 * <p>
 * Unlike using arrays directly as HashMap keys, this class properly handles arrays by using
 * content-based equality and hashing via {@link HashCode#of(Object...)} which internally uses
 * {@link java.util.Arrays#hashCode(Object[])} for arrays.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Using arrays as keys via Tuple1</jc>
 * 	Map&lt;Tuple1&lt;String[]&gt;,Integer&gt; <jv>map</jv> = <jk>new</jk> HashMap&lt;&gt;();
 * 	<jv>map</jv>.put(Tuple1.<jsm>of</jsm>(<jk>new</jk> String[]{<js>"a"</js>,<js>"b"</js>}), 1);
 * 	<jv>map</jv>.put(Tuple1.<jsm>of</jsm>(<jk>new</jk> String[]{<js>"a"</js>,<js>"b"</js>}), 2);  <jc>// Replaces first entry</jc>
 * 	System.<jsf>out</jsf>.println(<jv>map</jv>.size());  <jc>// Prints "1"</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Tuple2}
 * 	<li class='jc'>{@link Tuple3}
 * 	<li class='jc'>{@link Tuple4}
 * 	<li class='jc'>{@link Tuple5}
 * </ul>
 *
 * @param <A> Object type.
 */
public class Tuple1<A> {

	/**
	 * Static creator.
	 *
	 * @param <A> Object type.
	 * @param a Object value.
	 * @return A new tuple object.
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
	 * Returns the object in this tuple.
	 *
	 * @return The object in this tuple.
	 */
	public A getA() { return a; }

	@Override /* Overridden from Object */
	public int hashCode() {
		return hashCode;
	}
}
