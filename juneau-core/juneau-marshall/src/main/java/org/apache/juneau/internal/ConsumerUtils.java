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
package org.apache.juneau.internal;

import java.util.function.*;

/**
 * Utilities when working with {@link Predicate Predicates} and {@link Consumer Consumers}.
 */
public final class ConsumerUtils {

	/**
	 * Returns <jk>true</jk> if the specified predicate is <jk>null</jk> or matches the specified value.

	 * @param <T> The type being tested.
	 * @param predicate The predicate.
	 * @param value The value to test.
	 * @return <jk>true</jk> if the specified predicate is <jk>null</jk> or matches the specified value.
	 */
	public static <T> boolean test(Predicate<T> predicate, T value) {
		return (predicate == null || predicate.test(value));
	}

	/**
	 * Returns <jk>true</jk> if the specified object is the specified type and the specified predicate is <jk>null</jk> or matches the specified value.
	 *
	 * @param <T> The type being tested.
	 * @param type The expected type.
	 * @param predicate The predicate.
	 * @param value The value.
	 * @return <jk>true</jk> if the specified predicate is <jk>null</jk> or matches the specified value.
	 */
	public static <T> boolean test(Class<T> type, Predicate<T> predicate, Object value) {
		return type.isInstance(value) && (predicate == null || predicate.test(type.cast(value)));
	}

	/**
	 * Consumes the specified value if the predicate is <jk>null</jk> or matches the specified value.
	 *
	 * @param <T> The type being consumed.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 * @param value The value.
	 */
	public static <T> void consume(Predicate<T> predicate, Consumer<T> consumer, T value) {
		if (test(predicate, value))
			consumer.accept(value);
	}

	/**
	 * Consumes the specified value if it's the specified type and the predicate is <jk>null</jk> or matches the specified value.
	 *
	 * @param <T> The type being consumed.
	 * @param type The expected type.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 * @param value The value.
	 */
	public static <T> void consume(Class<T> type, Predicate<T> predicate, Consumer<T> consumer, Object value) {
		if (test(type, predicate, value))
			consumer.accept(type.cast(value));
	}
}
