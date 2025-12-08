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
 * Represents a simple tuple of 5 objects.
 *
 * <p>
 * This class is useful when you need a five-value composite key for HashMap lookups that properly implements
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
 * 	<li class='jc'>{@link Tuple3}
 * 	<li class='jc'>{@link Tuple4}
 * </ul>
 *
 * @param <A> Object 1 type.
 * @param <B> Object 2 type.
 * @param <C> Object 3 type.
 * @param <D> Object 4 type.
 * @param <E> Object 5 type.
 */
public class Tuple5<A,B,C,D,E> {

	/**
	 * Static creator.
	 *
	 * @param <A> Object 1 type.
	 * @param <B> Object 2 type.
	 * @param <C> Object 3 type.
	 * @param <D> Object 4 type.
	 * @param <E> Object 5 type.
	 * @param a Object 1.
	 * @param b Object 2.
	 * @param c Object 3.
	 * @param d Object 4.
	 * @param e Object 5.
	 * @return A new tuple object.
	 */
	public static <A,B,C,D,E> Tuple5<A,B,C,D,E> of(A a, B b, C c, D d, E e) {
		return new Tuple5<>(a, b, c, d, e);
	}

	private final A a;
	private final B b;
	private final C c;
	private final D d;
	private final E e;
	private final int hashCode;

	/**
	 * Constructor.
	 *
	 * @param a Object 1.
	 * @param b Object 2.
	 * @param c Object 3.
	 * @param d Object 4.
	 * @param e Object 5.
	 */
	public Tuple5(A a, B b, C c, D d, E e) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.hashCode = HashCode.of(a, b, c, d, e);
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return o instanceof Tuple5<?,?,?,?,?> o2 && eq(this, o2, (x, y) -> eq(x.a, y.a) && eq(x.b, y.b) && eq(x.c, y.c) && eq(x.d, y.d) && eq(x.e, y.e));
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

	/**
	 * Returns the fourth object in this tuple.
	 *
	 * @return The fourth object in this tuple.
	 */
	public D getD() { return d; }

	/**
	 * Returns the fifth object in this tuple.
	 *
	 * @return The fifth object in this tuple.
	 */
	public E getE() { return e; }

	/**
	 * Returns the first object in this tuple wrapped in an {@link Optional}.
	 *
	 * @return The first object wrapped in an Optional, or Optional.empty() if the value is null.
	 */
	public Optional<A> optA() {
		return Optional.ofNullable(a);
	}

	/**
	 * Returns the second object in this tuple wrapped in an {@link Optional}.
	 *
	 * @return The second object wrapped in an Optional, or Optional.empty() if the value is null.
	 */
	public Optional<B> optB() {
		return Optional.ofNullable(b);
	}

	/**
	 * Returns the third object in this tuple wrapped in an {@link Optional}.
	 *
	 * @return The third object wrapped in an Optional, or Optional.empty() if the value is null.
	 */
	public Optional<C> optC() {
		return Optional.ofNullable(c);
	}

	/**
	 * Returns the fourth object in this tuple wrapped in an {@link Optional}.
	 *
	 * @return The fourth object wrapped in an Optional, or Optional.empty() if the value is null.
	 */
	public Optional<D> optD() {
		return Optional.ofNullable(d);
	}

	/**
	 * Returns the fifth object in this tuple wrapped in an {@link Optional}.
	 *
	 * @return The fifth object wrapped in an Optional, or Optional.empty() if the value is null.
	 */
	public Optional<E> optE() {
		return Optional.ofNullable(e);
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return hashCode;
	}
}