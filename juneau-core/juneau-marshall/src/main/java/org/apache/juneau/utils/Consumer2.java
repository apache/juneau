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
 * Functional interface for consumers of 2-part arguments.
 *
 * @param <A> Argument 1.
 * @param <B> Argument 2.
 */
@FunctionalInterface
public interface Consumer2<A,B> {

	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param a Argument 1.
	 * @param b Argument 2.
	 */
	void apply(A a, B b);

	/**
	 * Returns a composed {@link Consumer} that performs, in sequence, this operation followed by the <c>after</c> operation.
	 *
	 * @param <V> The return type.
	 * @param after The operation to perform after this operation.
	 * @return A composed {@link Consumer} that performs in sequence this operation followed by the after operation.
	 */
	default <V> Consumer2<A,B> andThen(Consumer2<? super A,? super B> after) {
		return (A a, B b) -> {
			apply(a, b);
			after.apply(a, b);
		};
	}
}
