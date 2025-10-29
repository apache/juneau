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
 * Represents a simple tuple of 2 objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <A> Object 1 type.
 * @param <B> Object 2 type.
 */
public class Tuple2<A,B> {

	/**
	 * Static creator.
	 *
	 * @param <A> Object 1 type.
	 * @param <B> Object 2 type.
	 * @param a Object 1.
	 * @param b Object 2.
	 * @return A new tuple object.
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

	@Override /* Overridden from Object */
	public int hashCode() {
		return hashCode;
	}
}