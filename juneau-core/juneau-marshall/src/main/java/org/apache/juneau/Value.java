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
package org.apache.juneau;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.reflect.*;

/**
 * Represents a simple settable value.
 *
 * <p>
 * Similar to an {@link Optional} but mutable.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> The value type.
 */
public class Value<T> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param <T> The value type.
	 * @param object The object being wrapped.
	 * @return A new {@link Value} object.
	 */
	public static <T> Value<T> of(T object) {
		return new Value<>(object);
	}

	/**
	 * Static creator.
	 *
	 * @param <T> The value type.
	 * @return An empty {@link Value} object.
	 */
	public static <T> Value<T> empty() {
		return new Value<>(null);
	}

	/**
	 * Returns the generic parameter type of the Value type.
	 *
	 * @param t The type to find the parameter type of.
	 * @return The parameter type of the value, or <jk>null</jk> if the type is not a subclass of <c>Value</c>.
	 */
	public static Type getParameterType(Type t) {
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			if (pt.getRawType() == Value.class) {
				Type[] ta = pt.getActualTypeArguments();
				if (ta.length > 0)
					return ta[0];
			}
		} else if (t instanceof Class) {
			Class<?> c = (Class<?>)t;
			if (Value.class.isAssignableFrom(c)) {
				return ClassInfo.of(c).getParameterType(0, Value.class);
			}
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
		Type x = getParameterType(t);
		return x != null ? x : t;
	}

	/**
	 * Convenience method for checking if the specified type is this class.
	 *
	 * @param t The type to check.
	 * @return <jk>true</jk> if the specified type is this class.
	 */
	public static boolean isType(Type t) {
		return
			(t instanceof ParameterizedType && ((ParameterizedType)t).getRawType() == Value.class)
			|| (t instanceof Class && Value.class.isAssignableFrom((Class<?>)t));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------


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

	/**
	 * Adds a listener for this value.
	 *
	 * @param listener The new listener for this value.
	 * @return This object.
	 */
	public Value<T> listener(ValueListener<T> listener) {
		this.listener = listener;
		return this;
	}

	/**
	 * Sets the value.
	 *
	 * @param t The new value.
	 * @return This object.
	 */
	public Value<T> set(T t) {
		this.t = t;
		if (listener != null)
			listener.onSet(t);
		return this;
	}

	/**
	 * Sets the value if it's not already set.
	 *
	 * @param t The new value.
	 * @return This object.
	 */
	public Value<T> setIfEmpty(T t) {
		if (isEmpty())
			set(t);
		return this;
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
	 * Returns the value and then unsets it.
	 *
	 * @return The value before it was unset.
	 */
	public T getAndUnset() {
		T t2 = t;
		t = null;
		return t2;
	}

	/**
	 * Returns <jk>true</jk> if the value is set.
	 *
	 * @return <jk>true</jk> if the value is set.
	 */
	public boolean isPresent() {
		return get() != null;
	}

	/**
	 * If a value is present, invoke the specified consumer with the value, otherwise do nothing.
	 *
	 * @param consumer Block to be executed if a value is present.
	 */
	public void ifPresent(Consumer<? super T> consumer) {
		if (t != null)
			consumer.accept(t);
	}

	/**
	 * Applies a mapping function against the contents of this value.
	 *
	 * @param <T2> The mapped value type.
	 * @param mapper The mapping function.
	 * @return The mapped value.
	 */
	public <T2> Value<T2> map(Function<? super T, T2> mapper) {
		if (t != null)
			return Value.of(mapper.apply(t));
		return Value.empty();
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
	 * Returns <jk>true</jk> if the value is empty.
	 *
	 * @return <jk>true</jk> if the value is empty.
	 */
	public boolean isEmpty() {
		return t == null;
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
		return t != null ? t : other.get();
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
		if (t != null)
			return t;
		throw exceptionSupplier.get();
	}

	@Override /* Object */
	public String toString() {
		return "Value("+t+")";
	}
}
