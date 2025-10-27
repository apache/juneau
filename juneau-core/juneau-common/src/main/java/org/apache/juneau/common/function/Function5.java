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

import java.util.function.*;

/**
 * A function that takes in 5 arguments.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <A> The first argument.
 * @param <B> The second argument.
 * @param <C> The third argument.
 * @param <D> The fourth argument.
 * @param <E> The fifth argument.
 * @param <R> The return type.
 */
@SuppressWarnings("javadoc")
@FunctionalInterface
public interface Function5<A,B,C,D,E,R> {

	default <V> Function5<A,B,C,D,E,V> andThen(Function<? super R,? extends V> after) {
		assertArgNotNull("after", after);
		return (A a, B b, C c, D d, E e) -> after.apply(apply(a, b, c, d, e));
	}

	R apply(A a, B b, C c, D d, E e);
}
