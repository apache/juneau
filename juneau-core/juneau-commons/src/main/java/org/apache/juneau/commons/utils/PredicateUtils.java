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
package org.apache.juneau.commons.utils;

import java.util.function.*;

/**
 * Utility methods for composing {@link Predicate} instances.
 */
public final class PredicateUtils {

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

	/**
	 * Returns a function that prints the input value to stderr and returns it unchanged.
	 *
	 * <p>
	 * Useful for debugging streams by inserting into a stream pipeline with {@code .map(peek())}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	list.stream()
	 * 		.map(peek())
	 * 		.filter(x -&gt; x != <jk>null</jk>)
	 * 		.collect(Collectors.toList());
	 * </p>
	 *
	 * @param <T> The type of value.
	 * @return A function that prints and returns the value.
	 */
	public static <T> Function<T,T> peek() {
		return v -> {
			System.err.println(v);
			return v;
		};
	}

	/**
	 * Returns a function that prints the input value to stderr using a custom formatter and returns it unchanged.
	 *
	 * <p>
	 * Useful for debugging streams by inserting into a stream pipeline with {@code .map(peek(...))}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	list.stream()
	 * 		.map(peek(<js>"Processing: {0}"</js>, x -&gt; x.getName()))
	 * 		.filter(x -&gt; x != <jk>null</jk>)
	 * 		.collect(Collectors.toList());
	 * </p>
	 *
	 * @param <T> The type of value.
	 * @param message A format string using {@code {0}} as placeholder for the formatted value.
	 * @param formatter A function to extract/format the value for display.
	 * @return A function that prints and returns the value.
	 */
	public static <T> Function<T,T> peek(String message, Function<T,?> formatter) {
		return v -> {
			System.err.println(message.replace("{0}", String.valueOf(formatter.apply(v))));
			return v;
		};
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

	private PredicateUtils() {}
}
