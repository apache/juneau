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
package org.apache.juneau.commons.collections;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.reflect.ClassInfo;

/**
 * A generic mutable value wrapper.
 *
 * <p>
 * This class provides a simple way to wrap any object type in a mutable container, making it useful for passing
 * mutable references to lambdas, inner classes, and methods. It is similar to {@link Optional} but allows the
 * value to be changed after creation.
 *
 * <p>
 * The class supports method chaining through fluent setters and provides various convenience methods for working
 * with the wrapped value, including mapping, conditional execution, and default value handling.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Mutable value container with fluent API
 * 	<li>Optional-like methods: {@link #get()}, {@link #orElse(Object)}, {@link #ifPresent(Consumer)}, {@link #map(Function)}
 * 	<li>Atomic-like operations: {@link #getAndSet(Object)}, {@link #getAndUnset()}
 * 	<li>Listener support for value changes via {@link ValueListener}
 * 	<li>Method chaining for all setter operations
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>
 * 		This class is <b>not thread-safe</b>. For concurrent access, consider using atomic classes like
 * 		{@link java.util.concurrent.atomic.AtomicReference}.
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	Value&lt;String&gt; <jv>name</jv> = Value.<jsm>of</jsm>(<js>"John"</js>);
 * 	<jv>name</jv>.set(<js>"Jane"</js>);
 * 	String <jv>result</jv> = <jv>name</jv>.get();  <jc>// Returns "Jane"</jc>
 *
 * 	<jc>// Use in lambda (effectively final variable)</jc>
 * 	Value&lt;String&gt; <jv>result</jv> = Value.<jsm>empty</jsm>();
 * 	list.forEach(<jv>x</jv> -&gt; {
 * 		<jk>if</jk> (<jv>x</jv>.matches(criteria)) {
 * 			<jv>result</jv>.set(<jv>x</jv>);
 * 		}
 * 	});
 *
 * 	<jc>// With default values</jc>
 * 	Value&lt;String&gt; <jv>optional</jv> = Value.<jsm>empty</jsm>();
 * 	String <jv>value</jv> = <jv>optional</jv>.orElse(<js>"default"</js>);  <jc>// Returns "default"</jc>
 *
 * 	<jc>// Mapping values</jc>
 * 	Value&lt;Integer&gt; <jv>number</jv> = Value.<jsm>of</jsm>(5);
 * 	Value&lt;String&gt; <jv>text</jv> = <jv>number</jv>.map(Object::toString);  <jc>// Value of "5"</jc>
 *
 * 	<jc>// With listener for change notification</jc>
 * 	Value&lt;String&gt; <jv>monitored</jv> = Value.<jsm>of</jsm>(<js>"initial"</js>)
 * 		.listener(<jv>newValue</jv> -&gt; <jsm>log</jsm>(<js>"Value changed to: "</js> + <jv>newValue</jv>));
 * 	<jv>monitored</jv>.set(<js>"updated"</js>);  <jc>// Triggers listener</jc>
 * </p>
 *
 * <h5 class='section'>Specialized Value Classes:</h5>
 * <p>
 * For primitive types and common use cases, specialized subclasses are available with additional convenience methods:
 * <ul class='spaced-list'>
 * 	<li>{@link IntegerValue} - For mutable integers with {@code getAndIncrement()}
 * 	<li>{@link LongValue} - For mutable longs with {@code getAndIncrement()}
 * 	<li>{@link ShortValue} - For mutable shorts with {@code getAndIncrement()}
 * 	<li>{@link FloatValue} - For mutable floats
 * 	<li>{@link DoubleValue} - For mutable doubles
 * 	<li>{@link CharValue} - For mutable characters
 * 	<li>{@link BooleanValue} - For nullable booleans with {@code isTrue()}/{@code isNotTrue()}
 * 	<li>{@link Flag} - For non-nullable boolean flags (true/false only, no null state)
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsCollections">juneau-commons-collections</a>
 * 	<li class='jc'>{@link ValueListener}
 * 	<li class='jc'>{@link IntegerValue}
 * 	<li class='jc'>{@link BooleanValue}
 * 	<li class='jc'>{@link Flag}
 * </ul>
 *
 * @param <T> The value type.
 */
public class Value<T> {
	/**
	 * Creates a new empty value (with <c>null</c> as the initial value).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>empty</jsm>();
	 * 	<jsm>assertNull</jsm>(<jv>value</jv>.get());
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.isEmpty());
	 * </p>
	 *
	 * @param <T> The value type.
	 * @return A new empty {@link Value} object.
	 */
	public static <T> Value<T> empty() {
		return new Value<>(null);
	}

	/**
	 * Convenience method for checking if the specified type is this class.
	 *
	 * @param t The type to check.
	 * @return <jk>true</jk> if the specified type is this class.
	 */
	public static boolean isType(Type t) {
		return (t instanceof ParameterizedType t2 && t2.getRawType() == Value.class) || (t instanceof Class t3 && Value.class.isAssignableFrom(t3));
	}

	/**
	 * Creates a new value with the specified initial value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>of</jsm>(<js>"hello"</js>);
	 * 	<jsm>assertEquals</jsm>(<js>"hello"</js>, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param <T> The value type.
	 * @param object The object being wrapped. Can be <jk>null</jk>.
	 * @return A new {@link Value} object containing the specified value.
	 */
	public static <T> Value<T> of(T object) {
		return new Value<>(object);
	}

	/**
	 * Returns the generic parameter type of the Value type.
	 *
	 * @param t The type to find the parameter type of.
	 * @return The parameter type of the value, or <jk>null</jk> if the type is not a subclass of <c>Value</c>.
	 */
	public static Type getParameterType(Type t) {
		if (t instanceof ParameterizedType t2) {
			if (t2.getRawType() == Value.class) {
				var ta = t2.getActualTypeArguments();
				if (ta.length > 0)
					return ta[0];
			}
		} else if ((t instanceof Class<?> t3) && Value.class.isAssignableFrom(t3)) {
			return ClassInfo.of(t3).getParameterType(0, Value.class);
		}

		return null;
	}

	/**
	 * Returns the unwrapped type.
	 *
	 * @param t The type to unwrap.
	 * @return The unwrapped type, or the same type if the type isn't {@link Value}.
	 */
	public static Type unwrap(Type t) {
		var x = getParameterType(t);
		return nn(x) ? x : t;
	}

	private T t;
	private ValueListener<T> listener;

	/**
	 * Constructor.
	 */
	public Value() {}

	/**
	 * Constructor.
	 *
	 * @param t Initial value.
	 */
	public Value(T t) {
		set(t);
	}

	@Override /* Overridden from Object */
	public boolean equals(Object obj) {
		return obj instanceof Value<?> obj2 && eq(this, obj2, (x, y) -> eq(x.t, y.t));
	}

	/**
	 * If a value is present, and the value matches the given predicate, returns a {@link Value} describing the
	 * value, otherwise returns an empty {@link Value}.
	 *
	 * <p>
	 * This method is analogous to {@link java.util.Optional#filter(Predicate)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>of</jsm>(<js>"hello"</js>);
	 * 	Value&lt;String&gt; <jv>filtered</jv> = <jv>value</jv>.filter(<jv>s</jv> -&gt; <jv>s</jv>.length() &gt; 3);
	 * 	<jsm>assertTrue</jsm>(<jv>filtered</jv>.isPresent());
	 *
	 * 	Value&lt;String&gt; <jv>filtered2</jv> = <jv>value</jv>.filter(<jv>s</jv> -&gt; <jv>s</jv>.length() &gt; 10);
	 * 	<jsm>assertFalse</jsm>(<jv>filtered2</jv>.isPresent());
	 * </p>
	 *
	 * @param predicate The predicate to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return A {@link Value} describing the value if it is present and matches the predicate, otherwise an empty {@link Value}.
	 */
	public Value<T> filter(Predicate<? super T> predicate) {
		assertArgNotNull("predicate", predicate);
		if (t == null)
			return Value.empty();
		return predicate.test(t) ? this : Value.empty();
	}

	/**
	 * If a value is present, returns the result of applying the given {@link Value}-bearing mapping function to
	 * the value, otherwise returns an empty {@link Value}.
	 *
	 * <p>
	 * This method is similar to {@link #map(Function)}, but the mapping function returns a {@link Value} rather
	 * than a simple value. This is analogous to {@link java.util.Optional#flatMap(Function)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>of</jsm>(<js>"hello"</js>);
	 * 	Value&lt;Integer&gt; <jv>length</jv> = <jv>value</jv>.flatMap(<jv>s</jv> -&gt; Value.<jsm>of</jsm>(<jv>s</jv>.length()));
	 * 	<jsm>assertEquals</jsm>(5, <jv>length</jv>.get());
	 *
	 * 	<jc>// Returns empty if mapper returns empty</jc>
	 * 	Value&lt;String&gt; <jv>empty</jv> = <jv>value</jv>.flatMap(<jv>s</jv> -&gt; Value.<jsm>empty</jsm>());
	 * 	<jsm>assertFalse</jsm>(<jv>empty</jv>.isPresent());
	 * </p>
	 *
	 * @param <T2> The type of value in the {@link Value} returned by the mapping function.
	 * @param mapper The mapping function to apply to the value, if present. Must not be <jk>null</jk>.
	 * @return The result of applying the {@link Value}-bearing mapping function to the value if present,
	 *         otherwise an empty {@link Value}.
	 */
	public <T2> Value<T2> flatMap(Function<? super T,? extends Value<? extends T2>> mapper) {
		assertArgNotNull("mapper", mapper);
		if (t == null)
			return Value.empty();
		var result = mapper.apply(t);
		@SuppressWarnings("unchecked")
		var cast = (Value<T2>)result;
		return cast;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value, or <jk>null</jk> if it is not set.
	 */
	public T get() {
		return t;
	}

	/**
	 * Returns the current value and then sets it to the specified value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>of</jsm>(<js>"old"</js>);
	 * 	String <jv>oldValue</jv> = <jv>value</jv>.getAndSet(<js>"new"</js>);  <jc>// Returns "old"</jc>
	 * 	String <jv>newValue</jv> = <jv>value</jv>.get();                   <jc>// Returns "new"</jc>
	 * </p>
	 *
	 * @param t The new value.
	 * @return The value before it was set.
	 */
	public T getAndSet(T t) {
		var t2 = this.t;
		set(t);
		return t2;
	}

	/**
	 * Returns the current value and then unsets it (sets to <jk>null</jk>).
	 *
	 * <p>
	 * This is useful for "consuming" a value, ensuring it can only be retrieved once.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>of</jsm>(<js>"data"</js>);
	 * 	String <jv>result</jv> = <jv>value</jv>.getAndUnset();  <jc>// Returns "data"</jc>
	 * 	<jsm>assertNull</jsm>(<jv>value</jv>.get());           <jc>// Value is now null</jc>
	 * </p>
	 *
	 * @return The value before it was unset, or <jk>null</jk> if it was already <jk>null</jk>.
	 */
	public T getAndUnset() {
		var t2 = t;
		t = null;
		return t2;
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return t == null ? 0 : t.hashCode();
	}

	/**
	 * If a value is present (not <jk>null</jk>), invokes the specified consumer with the value, otherwise does nothing.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>of</jsm>(<js>"hello"</js>);
	 * 	<jv>value</jv>.ifPresent(<jsm>System.out</jsm>::<jv>println</jv>);  <jc>// Prints "hello"</jc>
	 *
	 * 	Value&lt;String&gt; <jv>empty</jv> = Value.<jsm>empty</jsm>();
	 * 	<jv>empty</jv>.ifPresent(<jsm>System.out</jsm>::<jv>println</jv>);  <jc>// Does nothing</jc>
	 * </p>
	 *
	 * @param consumer Block to be executed if a value is present. Must not be <jk>null</jk>.
	 */
	public void ifPresent(Consumer<? super T> consumer) {
		if (nn(t))
			consumer.accept(t);
	}

	/**
	 * Checks if the current value equals the specified value using {@link org.apache.juneau.commons.utils.Utils#eq(Object, Object)}.
	 *
	 * <p>
	 * This method uses {@code eq()} for equality comparison, which handles <jk>null</jk> values safely
	 * and performs deep equality checks for arrays and collections.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>of</jsm>(<js>"hello"</js>);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.is(<js>"hello"</js>));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.is(<js>"world"</js>));
	 *
	 * 	<jc>// Handles null values safely</jc>
	 * 	Value&lt;String&gt; <jv>empty</jv> = Value.<jsm>empty</jsm>();
	 * 	<jsm>assertTrue</jsm>(<jv>empty</jv>.is(<jk>null</jk>));
	 * 	<jsm>assertFalse</jsm>(<jv>empty</jv>.is(<js>"test"</js>));
	 *
	 * 	<jc>// Works with any type</jc>
	 * 	Value&lt;Integer&gt; <jv>number</jv> = Value.<jsm>of</jsm>(42);
	 * 	<jsm>assertTrue</jsm>(<jv>number</jv>.is(42));
	 * 	<jsm>assertFalse</jsm>(<jv>number</jv>.is(43));
	 * </p>
	 *
	 * @param other The value to compare with. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the values are equal according to {@link org.apache.juneau.commons.utils.Utils#eq(Object, Object)}.
	 */
	public boolean is(T other) {
		return eq(t, other);
	}

	/**
	 * Returns <jk>true</jk> if the value is empty.
	 *
	 * @return <jk>true</jk> if the value is empty.
	 */
	public boolean isEmpty() { return t == null; }

	/**
	 * Returns <jk>true</jk> if the value is set.
	 *
	 * @return <jk>true</jk> if the value is set.
	 */
	public boolean isPresent() { return nn(get()); }

	/**
	 * Registers a listener that will be called whenever the value is changed via {@link #set(Object)}.
	 *
	 * <p>
	 * Only one listener can be registered at a time. Calling this method again will replace the previous listener.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>of</jsm>(<js>"initial"</js>);
	 * 	<jv>value</jv>.listener(<jv>newValue</jv> -&gt; <jsm>log</jsm>(<js>"Changed to: "</js> + <jv>newValue</jv>));
	 *
	 * 	<jv>value</jv>.set(<js>"updated"</js>);  <jc>// Triggers listener, logs "Changed to: updated"</jc>
	 * </p>
	 *
	 * @param listener The listener to be called on value changes. Can be <jk>null</jk> to remove the listener.
	 * @return This object for method chaining.
	 * @see ValueListener
	 */
	public Value<T> listener(ValueListener<T> listener) {
		this.listener = listener;
		return this;
	}

	/**
	 * Applies a mapping function to the value if present, returning a new {@link Value} with the result.
	 *
	 * <p>
	 * If this value is empty (<jk>null</jk>), returns an empty value without applying the mapper.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>name</jv> = Value.<jsm>of</jsm>(<js>"john"</js>);
	 * 	Value&lt;String&gt; <jv>upper</jv> = <jv>name</jv>.map(String::toUpperCase);
	 * 	<jsm>assertEquals</jsm>(<js>"JOHN"</js>, <jv>upper</jv>.get());
	 *
	 * 	Value&lt;Integer&gt; <jv>length</jv> = <jv>name</jv>.map(String::length);
	 * 	<jsm>assertEquals</jsm>(4, <jv>length</jv>.get());
	 *
	 * 	Value&lt;String&gt; <jv>empty</jv> = Value.<jsm>empty</jsm>();
	 * 	Value&lt;Integer&gt; <jv>result</jv> = <jv>empty</jv>.map(String::length);  <jc>// Returns empty Value</jc>
	 * </p>
	 *
	 * @param <T2> The mapped value type.
	 * @param mapper The mapping function to apply. Must not be <jk>null</jk>.
	 * @return A new {@link Value} containing the mapped result, or an empty value if this value is empty.
	 */
	public <T2> Value<T2> map(Function<? super T,T2> mapper) {
		assertArgNotNull("mapper", mapper);
		if (nn(t))
			return of(mapper.apply(t));
		return empty();
	}

	/**
	 * Returns the contents of this value or the default value if <jk>null</jk>.
	 *
	 * @param def The default value.
	 * @return The contents of this value or the default value if <jk>null</jk>.
	 */
	public T orElse(T def) {
		return t == null ? def : t;
	}

	/**
	 * Return the value if present, otherwise invoke {@code other} and return
	 * the result of that invocation.
	 *
	 * @param other a {@code Supplier} whose result is returned if no value
	 * is present
	 * @return the value if present otherwise the result of {@code other.get()}
	 * @throws NullPointerException if value is not present and {@code other} is
	 * null
	 */
	public T orElseGet(Supplier<? extends T> other) {
		return nn(t) ? t : other.get();
	}

	/**
	 * Return the contained value, if present, otherwise throw an exception
	 * to be created by the provided supplier.
	 *
	 * @param <X> The exception type.
	 * @param exceptionSupplier The supplier which will return the exception to
	 * be thrown
	 * @return the present value
	 * @throws X if there is no value present
	 * @throws NullPointerException if no value is present and
	 * {@code exceptionSupplier} is null
	 */
	public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		if (nn(t))
			return t;
		throw exceptionSupplier.get();
	}

	/**
	 * Sets the value.
	 *
	 * <p>
	 * If a {@link ValueListener} is registered, it will be notified of the change.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>empty</jsm>();
	 * 	<jv>value</jv>.set(<js>"hello"</js>).set(<js>"world"</js>);  <jc>// Method chaining</jc>
	 * 	<jsm>assertEquals</jsm>(<js>"world"</js>, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param t The new value. Can be <jk>null</jk>.
	 * @return This object for method chaining.
	 */
	public Value<T> set(T t) {
		this.t = t;
		if (nn(listener))
			listener.onSet(t);
		return this;
	}

	/**
	 * Sets the value only if the specified condition is <jk>true</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>of</jsm>(<js>"old"</js>);
	 * 	<jv>value</jv>.setIf(<jk>true</jk>, <js>"new"</js>);   <jc>// Sets to "new"</jc>
	 * 	<jv>value</jv>.setIf(<jk>false</jk>, <js>"newer"</js>);  <jc>// Does nothing</jc>
	 * 	<jsm>assertEquals</jsm>(<js>"new"</js>, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param condition The condition to check.
	 * @param t The value to set if condition is <jk>true</jk>.
	 * @return This object.
	 */
	public Value<T> setIf(boolean condition, T t) {
		if (condition)
			set(t);
		return this;
	}

	/**
	 * Sets the value only if it is currently empty (<jk>null</jk>).
	 *
	 * <p>
	 * If the value is already set, this method does nothing.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>empty</jsm>();
	 * 	<jv>value</jv>.setIfEmpty(<js>"first"</js>);   <jc>// Sets value to "first"</jc>
	 * 	<jv>value</jv>.setIfEmpty(<js>"second"</js>);  <jc>// Does nothing, value remains "first"</jc>
	 * 	<jsm>assertEquals</jsm>(<js>"first"</js>, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param t The new value. Can be <jk>null</jk>.
	 * @return This object for method chaining.
	 */
	public Value<T> setIfEmpty(T t) {
		if (isEmpty())
			set(t);
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return "Value(" + t + ")";
	}

	/**
	 * Updates the value in-place using the specified function.
	 *
	 * <p>
	 * If the current value is <jk>null</jk>, this is a no-op.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Value&lt;String&gt; <jv>value</jv> = Value.<jsm>of</jsm>(<js>"hello"</js>);
	 * 	<jv>value</jv>.update(String::toUpperCase);
	 * 	<jsm>assertEquals</jsm>(<js>"HELLO"</js>, <jv>value</jv>.get());
	 *
	 * 	<jc>// No-op when null</jc>
	 * 	Value&lt;String&gt; <jv>empty</jv> = Value.<jsm>empty</jsm>();
	 * 	<jv>empty</jv>.update(String::toUpperCase);  <jc>// Does nothing</jc>
	 * 	<jsm>assertNull</jsm>(<jv>empty</jv>.get());
	 * </p>
	 *
	 * @param updater The function to apply to the current value. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Value<T> update(Function<T,T> updater) {
		if (nn(t))
			set(updater.apply(t));
		return this;
	}
}