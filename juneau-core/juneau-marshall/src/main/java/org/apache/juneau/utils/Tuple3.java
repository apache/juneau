// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.utils;

import static org.apache.juneau.internal.ObjectUtils.*;

import org.apache.juneau.internal.*;

/**
 * Represents a simple tuple of 3 objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
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
		return new Tuple3<>(a,b,c);
	}

	private final A a;
	private final B b;
	private final C c;

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
	}

	/**
	 * Returns the first object in this tuple.
	 *
	 * @return The first object in this tuple.
	 */
	public A getA() {
		return a;
	}

	/**
	 * Returns the second object in this tuple.
	 *
	 * @return The second object in this tuple.
	 */
	public B getB() {
		return b;
	}

	/**
	 * Returns the third object in this tuple.
	 *
	 * @return The third object in this tuple.
	 */
	public C getC() {
		return c;
	}

	@Override /* Object */
	public int hashCode() {
		return HashCode.of(a,b,c);
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return o instanceof Tuple3 && eq(this, (Tuple3<?,?,?>)o, (x,y)->eq(x.a,y.a) && eq(x.b,y.b) && eq(x.c,y.c));
	}
}
