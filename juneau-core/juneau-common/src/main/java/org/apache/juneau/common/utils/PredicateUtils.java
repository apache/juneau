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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.Utils.*;

import java.util.function.*;

/**
 * Utility methods for composing {@link Predicate} instances.
 */
public final class PredicateUtils {

	private PredicateUtils() {}

	/**
	 * Returns a predicate that is the short-circuiting AND of the provided predicates.
	 *
	 * <p>
	 * {@code null} entries are ignored. If all entries are {@code null} or no predicates are provided,
	 * the returned predicate always returns {@code true}.
	 *
	 * @param <T> The input type of the predicate.
	 * @param predicates The predicates to combine.
	 * @return A composed predicate representing the logical AND.
	 */
	@SafeVarargs
	public static <T> Predicate<T> and(Predicate<T>...predicates) {
		Predicate<T> result = t -> true;
		if (nn(predicates)) {
			for (var p : predicates) {
				if (nn(p))
					result = result.and(p);
			}
		}
		return result;
	}

	/**
	 * Returns a predicate that is the short-circuiting OR of the provided predicates.
	 *
	 * <p>
	 * {@code null} entries are ignored. If all entries are {@code null} or no predicates are provided,
	 * the returned predicate always returns {@code false}.
	 *
	 * @param <T> The input type of the predicate.
	 * @param predicates The predicates to combine.
	 * @return A composed predicate representing the logical OR.
	 */
	@SafeVarargs
	public static <T> Predicate<T> or(Predicate<T>...predicates) {
		Predicate<T> result = t -> false;
		if (nn(predicates)) {
			for (var p : predicates) {
				if (nn(p))
					result = result.or(p);
			}
		}
		return result;
	}

	/**
	 * Returns a predicate that tests whether the input value is an instance of the specified type.
	 *
	 * @param type The class object representing the target type.
	 * @return A predicate that returns {@code true} if the value is an instance of {@code type}.
	 */
	public static Predicate<?> isType(Class<?> type) {
		return v -> nn(type) && type.isInstance(v);
	}

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
	 * Consumes the specified value if the predicate is <jk>null</jk> or matches the specified value.
	 *
	 * @param <T> The type being consumed.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 * @param value The value.
	 */
	public static <T> void consumeIf(Predicate<T> predicate, Consumer<T> consumer, T value) {
		if (test(predicate, value))
			consumer.accept(value);
	}
}
