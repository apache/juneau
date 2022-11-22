//***************************************************************************************************************************
//* Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
//* distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
//* to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
//* with the License.  You may obtain a copy of the License at                                                              *
//*                                                                                                                         *
//*  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
//*                                                                                                                         *
//* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
//* specific language governing permissions and limitations under the License.                                              *
//***************************************************************************************************************************
package org.apache.juneau.internal;

import java.util.*;
import java.util.function.*;

/**
 * A function that takes in 2 arguments.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <A> The first argument.
 * @param <B> The second argument.
 * @param <R> The return type.
 */
@SuppressWarnings("javadoc")
@FunctionalInterface
public interface Function2<A,B,R> {

	R apply(A a, B b);

	default <V> Function2<A, B, V> andThen(Function<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (A a, B b) -> after.apply(apply(a, b));
	}
}