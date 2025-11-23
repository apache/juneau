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
import org.apache.juneau.common.reflect.*;

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
	 * Returns a predicate that evaluates to true only when the value is an instance of the given type and the provided
	 * predicate also returns true for the cast value.
	 *
	 * @param <T> The target type to test and cast to.
	 * @param type The target class.
	 * @param predicate The predicate to apply to the cast value. Can be null (treated as always-true after type check).
	 * @return A predicate over Object that performs an instanceof check AND the provided predicate.
	 */
	public static <T> Predicate<Object> andType(Class<T> type, Predicate<? super T> predicate) {
		Predicate<Object> p = type::isInstance;
		if (nn(predicate)) {
			p = p.and(o -> predicate.test(type.cast(o)));
		}
		return p;
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
	 * Returns a predicate that tests whether an {@link ElementInfo} has the specified flag.
	 *
	 * <p>
	 * Useful for filtering streams of reflection elements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;ClassInfo&gt; classes = ...;
	 * 	ClassInfo publicClass = classes.stream()
	 * 		.filter(is(PUBLIC))
	 * 		.findFirst()
	 * 		.orElse(<jk>null</jk>);
	 * </p>
	 *
	 * @param flag The flag to test for.
	 * @return A predicate that returns {@code true} if the element has the specified flag.
	 */
	public static Predicate<ElementInfo> is(ElementFlag flag) {
		return ei -> ei.is(flag);
	}

	/**
	 * Returns a predicate that tests whether an {@link ElementInfo} has all of the specified flags.
	 *
	 * <p>
	 * Useful for filtering streams of reflection elements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;ClassInfo&gt; classes = ...;
	 * 	ClassInfo publicNonDeprecated = classes.stream()
	 * 		.filter(isAll(PUBLIC, NOT_DEPRECATED))
	 * 		.findFirst()
	 * 		.orElse(<jk>null</jk>);
	 * </p>
	 *
	 * @param flags The flags to test for.
	 * @return A predicate that returns {@code true} if the element has all of the specified flags.
	 */
	public static Predicate<ElementInfo> isAll(ElementFlag...flags) {
		return ei -> ei.isAll(flags);
	}

	/**
	 * Returns a predicate that tests whether an {@link ElementInfo} has any of the specified flags.
	 *
	 * <p>
	 * Useful for filtering streams of reflection elements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;ClassInfo&gt; classes = ...;
	 * 	List&lt;ClassInfo&gt; visibleClasses = classes.stream()
	 * 		.filter(isAny(PUBLIC, PROTECTED))
	 * 		.collect(Collectors.toList());
	 * </p>
	 *
	 * @param flags The flags to test for.
	 * @return A predicate that returns {@code true} if the element has any of the specified flags.
	 */
	public static Predicate<ElementInfo> isAny(ElementFlag...flags) {
		return ei -> ei.isAny(flags);
	}
}
