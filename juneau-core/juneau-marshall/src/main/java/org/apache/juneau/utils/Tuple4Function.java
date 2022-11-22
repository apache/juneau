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

import java.util.function.*;

/**
 * Allows you to perform a function against 4 objects.
 *
 * <p>
 * Similar to {@link BiFunction} except for 4 parameters.
 *
 * <p class='bjava'>
 * 	Tuple4Function&lt;A,B,C,D,R&gt; <jv>x</jv> = (<jv>a</jv>,<jv>b</jv>,<jv>c</jv>,<jv>d</jv>) -&gt; <jsm>doSomething</jsm>(<jv>a</jv>,<jv>b</jv>,<jv>c</jv>,<jv>d</jv>);
 *
 * 	R <jv>result</jv> = <jv>x</jv>.apply(<jv>xa</jv>,<jv>xb</jv>,<jv>xc</jv>,<jv>xd</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <A> Object 1 type.
 * @param <B> Object 2 type.
 * @param <C> Object 3 type.
 * @param <D> Object 4 type.
 * @param <R> Result type.
 */
@FunctionalInterface
public interface Tuple4Function<A,B,C,D,R> {

	@SuppressWarnings("javadoc")
	R apply(A a, B b, C c, D d);

	@SuppressWarnings("javadoc")
	default <V> Tuple4Function<A,B,C,D,V> andThen(Function<? super R, ? extends V> after) {
		return (A a, B b, C c, D d) -> after.apply(apply(a, b, c, d));
	}
}
