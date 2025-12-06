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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

/**
 * Combines the features of {@link Supplier} and {@link Optional} into a single class.
 *
 * <p>
 * This interface extends {@link Supplier} and provides convenience methods for working with potentially null values,
 * similar to {@link Optional}. The key difference is that this interface is lazy - the value is only computed when
 * {@link #get()} is called, and the Optional-like methods operate on that computed value.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Extends Supplier - can be used anywhere a Supplier is expected
 * 	<li>Optional-like API - provides isPresent(), isEmpty(), map(), orElse(), etc.
 * 	<li>Lazy evaluation - value is only computed when get() is called
 * 	<li>Null-safe - handles null values gracefully
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create from a supplier</jc>
 * 	OptionalSupplier&lt;String&gt; <jv>supplier</jv> = OptionalSupplier.<jsm>of</jsm>(() -&gt; <js>"value"</js>);
 *
 * 	<jc>// Check if value is present</jc>
 * 	<jk>if</jk> (<jv>supplier</jv>.isPresent()) {
 * 		String <jv>value</jv> = <jv>supplier</jv>.get();
 * 	}
 *
 * 	<jc>// Map the value</jc>
 * 	OptionalSupplier&lt;Integer&gt; <jv>length</jv> = <jv>supplier</jv>.map(String::length);
 *
 * 	<jc>// Get value or default</jc>
 * 	String <jv>result</jv> = <jv>supplier</jv>.orElse(<js>"default"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Supplier} - Base functional interface
 * 	<li class='jc'>{@link Optional} - Java's Optional class
 * </ul>
 *
 * @param <T> The type of value supplied by this supplier.
 */
@FunctionalInterface
public interface OptionalSupplier<T> extends Supplier<T> {

	/**
	 * Creates an OptionalSupplier from a Supplier.
	 *
	 * @param <T> The value type.
	 * @param supplier The supplier. Must not be <jk>null</jk>.
	 * @return A new OptionalSupplier instance.
	 */
	public static <T> OptionalSupplier<T> of(Supplier<T> supplier) {
		assertArgNotNull("supplier", supplier);
		return supplier::get;
	}

	/**
	 * Creates an OptionalSupplier that always returns the specified value.
	 *
	 * @param <T> The value type.
	 * @param value The value to return. Can be <jk>null</jk>.
	 * @return A new OptionalSupplier instance.
	 */
	public static <T> OptionalSupplier<T> ofNullable(T value) {
		return () -> value;
	}

	/**
	 * Creates an empty OptionalSupplier that always returns <jk>null</jk>.
	 *
	 * @param <T> The value type.
	 * @return A new OptionalSupplier instance that always returns <jk>null</jk>.
	 */
	public static <T> OptionalSupplier<T> empty() {
		return () -> null;
	}

	/**
	 * Returns <jk>true</jk> if the supplied value is not <jk>null</jk>.
	 *
	 * @return <jk>true</jk> if the supplied value is not <jk>null</jk>.
	 */
	default boolean isPresent() {
		return nn(get());
	}

	/**
	 * Returns <jk>true</jk> if the supplied value is <jk>null</jk>.
	 *
	 * @return <jk>true</jk> if the supplied value is <jk>null</jk>.
	 */
	default boolean isEmpty() {
		return ! isPresent();
	}

	/**
	 * If a value is present, applies the provided mapping function to it and returns an OptionalSupplier describing the result.
	 *
	 * @param <U> The type of the result of the mapping function.
	 * @param mapper A mapping function to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return An OptionalSupplier describing the result of applying a mapping function to the value of this OptionalSupplier, if a value is present, otherwise an empty OptionalSupplier.
	 */
	default <U> OptionalSupplier<U> map(Function<? super T, ? extends U> mapper) {
		assertArgNotNull("mapper", mapper);
		return () -> {
			T value = get();
			return nn(value) ? mapper.apply(value) : null;
		};
	}

	/**
	 * If a value is present, returns the result of applying the given OptionalSupplier-bearing mapping function to the value, otherwise returns an empty OptionalSupplier.
	 *
	 * @param <U> The type parameter to the OptionalSupplier returned by the mapping function.
	 * @param mapper A mapping function to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return The result of applying an OptionalSupplier-bearing mapping function to the value of this OptionalSupplier, if a value is present, otherwise an empty OptionalSupplier.
	 */
	default <U> OptionalSupplier<U> flatMap(Function<? super T, ? extends OptionalSupplier<? extends U>> mapper) {
		assertArgNotNull("mapper", mapper);
		return () -> {
			T value = get();
			if (nn(value)) {
				OptionalSupplier<? extends U> result = mapper.apply(value);
				return result != null ? result.get() : null;
			}
			return null;
		};
	}

	/**
	 * If a value is present, and the value matches the given predicate, returns an OptionalSupplier describing the value, otherwise returns an empty OptionalSupplier.
	 *
	 * @param predicate A predicate to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return An OptionalSupplier describing the value of this OptionalSupplier if a value is present and the value matches the given predicate, otherwise an empty OptionalSupplier.
	 */
	default OptionalSupplier<T> filter(Predicate<? super T> predicate) {
		assertArgNotNull("predicate", predicate);
		return () -> {
			T value = get();
			return (nn(value) && predicate.test(value)) ? value : null;
		};
	}

	/**
	 * If a value is present, returns the value, otherwise returns <jk>other</jk>.
	 *
	 * @param other The value to be returned if there is no value present. Can be <jk>null</jk>.
	 * @return The value, if present, otherwise <jk>other</jk>.
	 */
	default T orElse(T other) {
		T value = get();
		return nn(value) ? value : other;
	}

	/**
	 * If a value is present, returns the value, otherwise returns the result produced by the supplying function.
	 *
	 * @param other A {@link Supplier} whose result is returned if no value is present. Must not be <jk>null</jk>.
	 * @return The value, if present, otherwise the result of <jk>other.get()</jk>.
	 */
	default T orElseGet(Supplier<? extends T> other) {
		assertArgNotNull("other", other);
		T value = get();
		return nn(value) ? value : other.get();
	}

	/**
	 * If a value is present, returns the value, otherwise throws an exception produced by the exception supplying function.
	 *
	 * @param <X> Type of the exception to be thrown.
	 * @param exceptionSupplier The supplying function that produces an exception to be thrown. Must not be <jk>null</jk>.
	 * @return The value, if present.
	 * @throws X If no value is present.
	 */
	default <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		assertArgNotNull("exceptionSupplier", exceptionSupplier);
		T value = get();
		if (nn(value))
			return value;
		throw exceptionSupplier.get();
	}

	/**
	 * If a value is present, performs the given action with the value, otherwise does nothing.
	 *
	 * @param action The action to be performed, if a value is present. Must not be <jk>null</jk>.
	 */
	default void ifPresent(Consumer<? super T> action) {
		assertArgNotNull("action", action);
		T value = get();
		if (nn(value))
			action.accept(value);
	}

	/**
	 * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
	 *
	 * @param action The action to be performed, if a value is present. Must not be <jk>null</jk>.
	 * @param emptyAction The empty-based action to be performed, if no value is present. Must not be <jk>null</jk>.
	 */
	default void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
		assertArgNotNull("action", action);
		assertArgNotNull("emptyAction", emptyAction);
		T value = get();
		if (nn(value))
			action.accept(value);
		else
			emptyAction.run();
	}

	/**
	 * Converts this OptionalSupplier to an {@link Optional}.
	 *
	 * @return An Optional containing the value if present, otherwise an empty Optional.
	 */
	default Optional<T> toOptional() {
		return opt(get());
	}
}

