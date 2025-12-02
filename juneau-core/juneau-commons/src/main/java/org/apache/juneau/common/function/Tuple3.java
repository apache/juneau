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
 * Represents a simple tuple of 3 objects.
 *
 * <p>
 * This class is useful when you need a three-value composite key for HashMap lookups that properly implements
 * {@link #equals(Object)} and {@link #hashCode()} based on content rather than identity.
 *
 * <h5 class='section'>Array Support:</h5>
 * <p>
 * Unlike using arrays directly as HashMap keys, this class properly handles arrays by using
 * content-based equality and hashing via {@link HashCode#of(Object...)} which internally uses
 * {@link java.util.Arrays#hashCode(Object[])} for arrays.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Tuple1}
 * 	<li class='jc'>{@link Tuple2}
 * 	<li class='jc'>{@link Tuple4}
 * 	<li class='jc'>{@link Tuple5}
 * </ul>
 *
 * @param <A> Object 1 type.
 * @param <B> Object 2 type.
 * @param <C> Object 3 type.
 */
public class Tuple3<A,B,C> {

	/**
	 * Static creator.
	 *
	 * @param <A> Object 1 type.
	 * @param <B> Object 2 type.
	 * @param <C> Object 3 type.
	 * @param a Object 1.
	 * @param b Object 2.
	 * @param c Object 3.
	 * @return A new tuple object.
	 */
	public static <A,B,C> Tuple3<A,B,C> of(A a, B b, C c) {
		return new Tuple3<>(a, b, c);
	}

	private final A a;
	private final B b;
	private final C c;
	private final int hashCode;

	/**
	 * Constructor.
	 *
	 * @param a Object 1.
	 * @param b Object 2.
	 * @param c Object 3.
	 */
	public Tuple3(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.hashCode = HashCode.of(a, b, c);
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return o instanceof Tuple3<?,?,?> o2 && eq(this, o2, (x, y) -> eq(x.a, y.a) && eq(x.b, y.b) && eq(x.c, y.c));
	}

	/**
	 * Returns the first object in this tuple.
	 *
	 * @return The first object in this tuple.
	 */
	public A getA() { return a; }

	/**
	 * Returns the second object in this tuple.
	 *
	 * @return The second object in this tuple.
	 */
	public B getB() { return b; }

	/**
	 * Returns the third object in this tuple.
	 *
	 * @return The third object in this tuple.
	 */
	public C getC() { return c; }

	@Override /* Overridden from Object */
	public int hashCode() {
		return hashCode;
	}
}