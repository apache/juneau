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
package org.apache.juneau.commons.concurrent;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * A thread-safe reference that combines {@link AtomicReference} with {@link Optional}-like functionality.
 *
 * <p>
 * This class extends {@link AtomicReference} to provide both atomic reference operations and Optional-like
 * convenience methods for working with potentially null values. It is thread-safe and can be used in
 * concurrent environments where you need to atomically update a reference while also having Optional-like
 * operations.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Extends AtomicReference - inherits all atomic operations (get, set, compareAndSet, etc.)
 * 	<li>Optional-like API - provides isPresent(), isEmpty(), map(), orElse(), etc.
 * 	<li>Thread-safe - all operations are atomic
 * 	<li>Null-safe - handles null values gracefully (null represents "empty")
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an empty reference</jc>
 * 	OptionalReference&lt;String&gt; <jv>ref</jv> = OptionalReference.<jsm>empty</jsm>();
 *
 * 	<jc>// Set a value atomically</jc>
 * 	<jv>ref</jv>.set(<js>"value"</js>);
 *
 * 	<jc>// Check if value is present</jc>
 * 	<jk>if</jk> (<jv>ref</jv>.isPresent()) {
 * 		String <jv>value</jv> = <jv>ref</jv>.get();
 * 	}
 *
 * 	<jc>// Use Optional-like methods</jc>
 * 	String <jv>result</jv> = <jv>ref</jv>.map(String::toUpperCase).orElse(<js>"default"</js>);
 *
 * 	<jc>// Atomic update</jc>
 * 	<jv>ref</jv>.compareAndSet(<js>"value"</js>, <js>"newValue"</js>);
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe. All operations inherit thread-safety from {@link AtomicReference}.
 * Multiple threads can safely call methods like {@link #get()}, {@link #set(Object)}, {@link #compareAndSet(Object, Object)},
 * and the Optional-like methods concurrently.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AtomicReference} - Base class providing atomic operations
 * 	<li class='jc'>{@link Optional} - Java's Optional class
 * 	<li class='jc'>{@link org.apache.juneau.commons.function.OptionalSupplier} - Similar functionality for suppliers
 * </ul>
 *
 * @param <V> The type of value held by this reference.
 */
@SuppressWarnings("java:S115")
public class OptionalReference<V> extends AtomicReference<V> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_action = "action";
	private static final String ARG_emptyAction = "emptyAction";
	private static final String ARG_exceptionSupplier = "exceptionSupplier";
	private static final String ARG_mapper = "mapper";
	private static final String ARG_other = "other";
	private static final String ARG_predicate = "predicate";

	private static final long serialVersionUID = 1L;

	/**
	 * Creates an empty OptionalReference with an initial value of <jk>null</jk>.
	 *
	 * @param <V> The value type.
	 * @return A new empty OptionalReference.
	 */
	public static <V> OptionalReference<V> empty() {
		return new OptionalReference<>(null);
	}

	/**
	 * Creates an OptionalReference with the specified initial value.
	 *
	 * @param <V> The value type.
	 * @param initialValue The initial value. Can be <jk>null</jk>.
	 * @return A new OptionalReference with the specified value.
	 */
	public static <V> OptionalReference<V> of(V initialValue) {
		return new OptionalReference<>(initialValue);
	}

	/**
	 * Creates an OptionalReference with the specified initial value (alias for {@link #of(Object)}).
	 *
	 * @param <V> The value type.
	 * @param initialValue The initial value. Can be <jk>null</jk>.
	 * @return A new OptionalReference with the specified value.
	 */
	public static <V> OptionalReference<V> ofNullable(V initialValue) {
		return new OptionalReference<>(initialValue);
	}

	/**
	 * Constructor with initial value.
	 *
	 * @param initialValue The initial value. Can be <jk>null</jk>.
	 */
	public OptionalReference(V initialValue) {
		super(initialValue);
	}

	/**
	 * Default constructor (initializes to <jk>null</jk>).
	 */
	public OptionalReference() {
		super();
	}

	/**
	 * Returns <jk>true</jk> if the current value is not <jk>null</jk>.
	 *
	 * @return <jk>true</jk> if the current value is not <jk>null</jk>.
	 */
	public boolean isPresent() {
		return nn(get());
	}

	/**
	 * Returns <jk>true</jk> if the current value is <jk>null</jk>.
	 *
	 * @return <jk>true</jk> if the current value is <jk>null</jk>.
	 */
	public boolean isEmpty() {
		return !isPresent();
	}

	/**
	 * If a value is present, applies the provided mapping function to it and returns an OptionalReference describing the result.
	 *
	 * @param <U> The type of the result of the mapping function.
	 * @param mapper A mapping function to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return An OptionalReference describing the result of applying a mapping function to the value, if a value is present, otherwise an empty OptionalReference.
	 */
	public <U> OptionalReference<U> map(Function<? super V, ? extends U> mapper) {
		assertArgNotNull(ARG_mapper, mapper);
		V value = get();
		return nn(value) ? OptionalReference.of(mapper.apply(value)) : OptionalReference.empty();
	}

	/**
	 * If a value is present, returns the result of applying the given OptionalReference-bearing mapping function to the value, otherwise returns an empty OptionalReference.
	 *
	 * @param <U> The type parameter to the OptionalReference returned by the mapping function.
	 * @param mapper A mapping function to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return The result of applying an OptionalReference-bearing mapping function to the value, if a value is present, otherwise an empty OptionalReference.
	 */
	public <U> OptionalReference<U> flatMap(Function<? super V, ? extends OptionalReference<? extends U>> mapper) {
		assertArgNotNull(ARG_mapper, mapper);
		V value = get();
		if (nn(value)) {
			OptionalReference<? extends U> result = mapper.apply(value);
			return result != null ? OptionalReference.ofNullable(result.get()) : OptionalReference.empty();
		}
		return OptionalReference.empty();
	}

	/**
	 * If a value is present, and the value matches the given predicate, returns an OptionalReference describing the value, otherwise returns an empty OptionalReference.
	 *
	 * @param predicate A predicate to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return An OptionalReference describing the value if a value is present and the value matches the given predicate, otherwise an empty OptionalReference.
	 */
	public OptionalReference<V> filter(Predicate<? super V> predicate) {
		assertArgNotNull(ARG_predicate, predicate);
		V value = get();
		return (nn(value) && predicate.test(value)) ? OptionalReference.of(value) : OptionalReference.empty();
	}

	/**
	 * If a value is present, returns the value, otherwise returns <jk>other</jk>.
	 *
	 * @param other The value to be returned if there is no value present. Can be <jk>null</jk>.
	 * @return The value, if present, otherwise <jk>other</jk>.
	 */
	public V orElse(V other) {
		V value = get();
		return nn(value) ? value : other;
	}

	/**
	 * If a value is present, returns the value, otherwise returns the result produced by the supplying function.
	 *
	 * @param other A {@link Supplier} whose result is returned if no value is present. Must not be <jk>null</jk>.
	 * @return The value, if present, otherwise the result of <jk>other.get()</jk>.
	 */
	public V orElseGet(Supplier<? extends V> other) {
		assertArgNotNull(ARG_other, other);
		V value = get();
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
	public <X extends Throwable> V orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		assertArgNotNull(ARG_exceptionSupplier, exceptionSupplier);
		V value = get();
		if (nn(value))
			return value;
		throw exceptionSupplier.get();
	}

	/**
	 * If a value is present, performs the given action with the value, otherwise does nothing.
	 *
	 * @param action The action to be performed, if a value is present. Must not be <jk>null</jk>.
	 */
	public void ifPresent(Consumer<? super V> action) {
		assertArgNotNull(ARG_action, action);
		V value = get();
		if (nn(value))
			action.accept(value);
	}

	/**
	 * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
	 *
	 * @param action The action to be performed, if a value is present. Must not be <jk>null</jk>.
	 * @param emptyAction The empty-based action to be performed, if no value is present. Must not be <jk>null</jk>.
	 */
	public void ifPresentOrElse(Consumer<? super V> action, Runnable emptyAction) {
		assertArgNotNull(ARG_action, action);
		assertArgNotNull(ARG_emptyAction, emptyAction);
		V value = get();
		if (nn(value))
			action.accept(value);
		else
			emptyAction.run();
	}

	/**
	 * Converts this OptionalReference to an {@link Optional}.
	 *
	 * @return An Optional containing the value if present, otherwise an empty Optional.
	 */
	public Optional<V> toOptional() {
		return opt(get());
	}
}
